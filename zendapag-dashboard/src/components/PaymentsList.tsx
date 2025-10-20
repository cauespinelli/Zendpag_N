// @ts-nocheck
// @ts-nocheck
import React, { useState, useCallback, useMemo } from 'react';
import {
  Card,
  Table,
  Space,
  Tag,
  Button,
  Input,
  Select,
  DatePicker,
  Dropdown,
  Tooltip,
  Typography,
  Row,
  Col,
  Badge,
  Divider,
  message,
} from 'antd';
import {
  SearchOutlined,
  FilterOutlined,
  DownloadOutlined,
  ReloadOutlined,
  EyeOutlined,
  StopOutlined,
  ExportOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import { Link } from 'react-router-dom';
import type { ColumnsType, TableProps } from 'antd/es/table';
import dayjs from 'dayjs';

import { usePayments, useCancelPayment } from '@/hooks/useQuery';
import { usePaymentUpdates } from '@/hooks/useWebSocket';
import { useTableState } from '@/hooks/useLocalStorage';
import { formatCurrency, formatDate, getPaymentStatusColor, getPaymentStatusText } from '@/utils/helpers';
import { PAYMENT_STATUS_COLORS, PAGINATION_DEFAULTS } from '@/utils/constants';
import type { Payment, PaymentStatus, PaginatedResponse } from '@/types';

const { Text, Title } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

interface PaymentFilters {
  status?: PaymentStatus;
  startDate?: string;
  endDate?: string;
  search?: string;
  pixKeyType?: string;
  amountMin?: number;
  amountMax?: number;
}

interface PaymentsListProps {
  embedded?: boolean;
  showHeader?: boolean;
  maxHeight?: number;
  showFilters?: boolean;
  onPaymentSelect?: (payment: Payment) => void;
}

const PaymentStatusTag: React.FC<{ status: PaymentStatus }> = ({ status }) => (
  <Tag color={getPaymentStatusColor(status)} style={{ borderRadius: 12, fontWeight: 500 }}>
    {getPaymentStatusText(status)}
  </Tag>
);

const PaymentsList: React.FC<PaymentsListProps> = ({
  embedded = false,
  showHeader = true,
  maxHeight,
  showFilters = true,
  onPaymentSelect,
}) => {
  const [filters, setFilters] = useState<PaymentFilters>({});
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([]);
  const [bulkActionLoading, setBulkActionLoading] = useState(false);

  // Table state management
  const {
    tableState,
    updateFilters,
    updateSorter,
    updatePagination,
  } = useTableState('payments-list');

  // Data fetching
  const {
    data: paymentsData,
    isLoading,
    error,
    refetch,
  } = usePayments({
    page: tableState.pagination.current || 1,
    size: tableState.pagination.pageSize || PAGINATION_DEFAULTS.PAGE_SIZE,
    ...filters,
  });

  // Real-time updates
  const { recentUpdates } = usePaymentUpdates();

  // Mutations
  const cancelPaymentMutation = useCancelPayment();

  const handleTableChange: TableProps<Payment>['onChange'] = useCallback(
    (pagination, tableFilters, sorter) => {
      updatePagination(pagination);
      updateFilters(tableFilters);
      updateSorter(sorter);
    },
    [updatePagination, updateFilters, updateSorter]
  );

  const handleSearch = useCallback((value: string) => {
    setFilters(prev => ({ ...prev, search: value }));
    updatePagination({ current: 1, pageSize: tableState.pagination.pageSize });
  }, [updatePagination, tableState.pagination.pageSize]);

  const handleStatusFilter = useCallback((status: PaymentStatus | undefined) => {
    setFilters(prev => ({ ...prev, status }));
    updatePagination({ current: 1, pageSize: tableState.pagination.pageSize });
  }, [updatePagination, tableState.pagination.pageSize]);

  const handleDateRangeFilter = useCallback((dates: [dayjs.Dayjs, dayjs.Dayjs] | null) => {
    if (dates) {
      setFilters(prev => ({
        ...prev,
        startDate: dates[0].format('YYYY-MM-DD'),
        endDate: dates[1].format('YYYY-MM-DD'),
      }));
    } else {
      setFilters(prev => {
        const { startDate, endDate, ...rest } = prev;
        return rest;
      });
    }
    updatePagination({ current: 1, pageSize: tableState.pagination.pageSize });
  }, [updatePagination, tableState.pagination.pageSize]);

  const handleCancelPayment = useCallback(async (paymentId: string, reason?: string) => {
    try {
      await cancelPaymentMutation.mutateAsync({ id: paymentId, reason });
      refetch();
    } catch (error) {
      // Error handled in mutation
    }
  }, [cancelPaymentMutation, refetch]);

  const handleBulkCancel = useCallback(async () => {
    if (selectedRowKeys.length === 0) return;

    setBulkActionLoading(true);
    try {
      await Promise.all(
        selectedRowKeys.map(id =>
          cancelPaymentMutation.mutateAsync({ id, reason: 'Cancelamento em lote' })
        )
      );
      setSelectedRowKeys([]);
      refetch();
      message.success(`${selectedRowKeys.length} pagamentos cancelados`);
    } catch (error) {
      message.error('Erro ao cancelar pagamentos em lote');
    } finally {
      setBulkActionLoading(false);
    }
  }, [selectedRowKeys, cancelPaymentMutation, refetch]);

  const handleExport = useCallback(async (format: 'csv' | 'pdf' = 'csv') => {
    try {
      // This would typically call an export API
      message.success(`Exportação ${format.toUpperCase()} iniciada`);
    } catch (error) {
      message.error('Erro ao exportar dados');
    }
  }, []);

  const columns: ColumnsType<Payment> = useMemo(() => [
    {
      title: 'ID',
      dataIndex: 'referenceId',
      key: 'referenceId',
      width: 120,
      render: (referenceId: string, record: Payment) => (
        <Space direction="vertical" size={0}>
          <Link
            to={`/payments/${record.id}`}
            onClick={() => onPaymentSelect?.(record)}
          >
            <Text strong style={{ color: 'var(--primary-color)' }}>
              {referenceId.slice(-8)}
            </Text>
          </Link>
          {recentUpdates.some(u => u.id === record.id) && (
            <Badge status="processing" text="Atualizado" />
          )}
        </Space>
      ),
    },
    {
      title: 'Valor',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      align: 'right',
      render: (amount: number) => (
        <Text strong style={{ fontSize: '14px' }}>
          {formatCurrency(amount)}
        </Text>
      ),
      sorter: true,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (status: PaymentStatus) => <PaymentStatusTag status={status} />,
      filters: [
        { text: 'Ativo', value: 'ACTIVE' },
        { text: 'Concluído', value: 'COMPLETED' },
        { text: 'Pendente', value: 'PENDING' },
        { text: 'Falhou', value: 'FAILED' },
        { text: 'Cancelado', value: 'CANCELLED' },
        { text: 'Expirado', value: 'EXPIRED' },
      ],
    },
    {
      title: 'Cliente',
      dataIndex: 'customerName',
      key: 'customerName',
      width: 160,
      render: (customerName: string, record: Payment) => (
        <Space direction="vertical" size={0}>
          <Text>{customerName || 'N/A'}</Text>
          {record.customerEmail && (
            <Text type="secondary" style={{ fontSize: '12px' }}>
              {record.customerEmail}
            </Text>
          )}
        </Space>
      ),
      ellipsis: true,
    },
    {
      title: 'Chave PIX',
      dataIndex: 'pixKey',
      key: 'pixKey',
      width: 140,
      render: (pixKey: string, record: Payment) => (
        <Space direction="vertical" size={0}>
          {pixKey ? (
            <>
              <Text code style={{ fontSize: '11px' }}>
                {pixKey.length > 20 ? `${pixKey.slice(0, 20)}...` : pixKey}
              </Text>
              <Tag size="small" color="blue">
                {record.pixKeyType}
              </Tag>
            </>
          ) : (
            <Text type="secondary">N/A</Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Data/Hora',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 140,
      render: (createdAt: string) => (
        <Space direction="vertical" size={0}>
          <Text>{formatDate(createdAt, 'DD/MM/YY')}</Text>
          <Text type="secondary" style={{ fontSize: '12px' }}>
            {formatDate(createdAt, 'HH:mm:ss')}
          </Text>
        </Space>
      ),
      sorter: true,
    },
    {
      title: 'Ações',
      key: 'actions',
      width: 80,
      render: (_, record: Payment) => (
        <Dropdown
          menu={{
            items: [
              {
                key: 'view',
                label: 'Ver detalhes',
                icon: <EyeOutlined />,
                onClick: () => onPaymentSelect?.(record),
              },
              {
                key: 'cancel',
                label: 'Cancelar',
                icon: <StopOutlined />,
                disabled: !['PENDING', 'ACTIVE'].includes(record.status),
                onClick: () => handleCancelPayment(record.id),
              },
            ],
          }}
          trigger={['click']}
        >
          <Button type="text" icon={<MoreOutlined />} size="small" />
        </Dropdown>
      ),
    },
  ], [recentUpdates, onPaymentSelect, handleCancelPayment]);

  const rowSelection = useMemo(() => ({
    selectedRowKeys,
    onChange: setSelectedRowKeys,
    getCheckboxProps: (record: Payment) => ({
      disabled: !['PENDING', 'ACTIVE'].includes(record.status),
    }),
  }), [selectedRowKeys]);

  const filterSection = showFilters && (
    <Card size="small" style={{ marginBottom: 16 }}>
      <Row gutter={[16, 16]} align="middle">
        <Col xs={24} sm={12} md={6}>
          <Input
            placeholder="Buscar por ID ou cliente"
            prefix={<SearchOutlined />}
            allowClear
            onChange={(e) => handleSearch(e.target.value)}
            style={{ width: '100%' }}
          />
        </Col>
        <Col xs={24} sm={12} md={5}>
          <Select
            placeholder="Status"
            allowClear
            style={{ width: '100%' }}
            onChange={handleStatusFilter}
          >
            <Option value="ACTIVE">Ativo</Option>
            <Option value="COMPLETED">Concluído</Option>
            <Option value="PENDING">Pendente</Option>
            <Option value="FAILED">Falhou</Option>
            <Option value="CANCELLED">Cancelado</Option>
            <Option value="EXPIRED">Expirado</Option>
          </Select>
        </Col>
        <Col xs={24} sm={12} md={6}>
          <RangePicker
            style={{ width: '100%' }}
            onChange={handleDateRangeFilter}
            placeholder={['Data inicial', 'Data final']}
          />
        </Col>
        <Col xs={24} sm={12} md={7}>
          <Space wrap>
            <Button icon={<ReloadOutlined />} onClick={refetch}>
              Atualizar
            </Button>
            <Dropdown
              menu={{
                items: [
                  {
                    key: 'csv',
                    label: 'Exportar CSV',
                    onClick: () => handleExport('csv'),
                  },
                  {
                    key: 'pdf',
                    label: 'Exportar PDF',
                    onClick: () => handleExport('pdf'),
                  },
                ],
              }}
            >
              <Button icon={<ExportOutlined />}>Exportar</Button>
            </Dropdown>
          </Space>
        </Col>
      </Row>
    </Card>
  );

  const headerSection = showHeader && (
    <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <div>
        <Title level={4} style={{ margin: 0 }}>
          Pagamentos
        </Title>
        <Text type="secondary">
          {paymentsData?.totalElements || 0} pagamentos encontrados
        </Text>
      </div>
      {selectedRowKeys.length > 0 && (
        <Space>
          <Text>{selectedRowKeys.length} selecionados</Text>
          <Divider type="vertical" />
          <Button
            type="primary"
            danger
            size="small"
            loading={bulkActionLoading}
            onClick={handleBulkCancel}
          >
            Cancelar Selecionados
          </Button>
        </Space>
      )}
    </div>
  );

  if (error) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Text type="danger">Erro ao carregar pagamentos</Text>
          <br />
          <Button type="link" onClick={refetch}>
            Tentar novamente
          </Button>
        </div>
      </Card>
    );
  }

  return (
    <div className="payments-list">
      {headerSection}
      {filterSection}

      <Card
        bodyStyle={{ padding: embedded ? 0 : 24 }}
        style={{ overflow: 'hidden' }}
      >
        <Table<Payment>
          columns={columns}
          dataSource={paymentsData?.content || []}
          rowKey="id"
          loading={isLoading}
          rowSelection={embedded ? undefined : rowSelection}
          onChange={handleTableChange}
          pagination={
            embedded
              ? false
              : {
                  current: paymentsData?.page || 1,
                  total: paymentsData?.totalElements || 0,
                  pageSize: paymentsData?.size || PAGINATION_DEFAULTS.PAGE_SIZE,
                  showSizeChanger: true,
                  showQuickJumper: true,
                  showTotal: (total, range) =>
                    `${range[0]}-${range[1]} de ${total} pagamentos`,
                  pageSizeOptions: PAGINATION_DEFAULTS.PAGE_SIZE_OPTIONS,
                }
          }
          scroll={{
            x: 1000,
            y: maxHeight,
          }}
          size="small"
          className="payments-table"
        />
      </Card>
    </div>
  );
};

export default PaymentsList;