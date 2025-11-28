// @ts-nocheck
import React, { useState, useMemo, useCallback } from 'react';
import {
  Card,
  Table,
  Tag,
  Button,
  Input,
  Select,
  DatePicker,
  Space,
  Typography,
  Row,
  Col,
  Statistic,
  Tooltip,
  message,
  Drawer,
  Descriptions,
  InputNumber,
  Modal,
  Form,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  DownloadOutlined,
  FilterOutlined,
  EyeOutlined,
  CopyOutlined,
  FileTextOutlined,
  ExclamationCircleOutlined,
  SendOutlined,
  RollbackOutlined,
  DollarOutlined,
  SwapOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import dayjs from 'dayjs';

import { ActionMenu, CopyButton } from '@/components/shared';
import {
  TRANSACTION_STATUS_COLORS,
  TRANSACTION_STATUS_LABELS,
  TRANSACTION_METHOD_COLORS,
  TRANSACTION_METHOD_LABELS,
  PAGINATION_DEFAULTS,
} from '@/utils/constants';
import { formatCurrency, formatDateTime, formatCNPJ } from '@/utils/helpers';
import type {
  Transaction,
  TransactionStatus,
  TransactionMethod,
  TransactionSummary,
  TransactionFilters,
} from '@/types';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

// Mock data for development
const mockTransactions: Transaction[] = [
  {
    id: 'TRX-001',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    customer: { name: 'João Silva', email: 'joao@email.com', document: '12345678901' },
    grossAmount: 1500.00,
    fee: 22.50,
    netAmount: 1477.50,
    status: 'PAID',
    method: 'PIX',
    endToEnd: 'E12345678202411121234567890',
    createdAt: '2024-11-12T10:30:00Z',
    paidAt: '2024-11-12T10:30:45Z',
  },
  {
    id: 'TRX-002',
    establishment: { id: '2', name: 'Tech Solutions ME', cnpj: '98765432000110' },
    customer: { name: 'Maria Santos', email: 'maria@email.com', document: '98765432100' },
    grossAmount: 2999.90,
    fee: 105.00,
    netAmount: 2894.90,
    status: 'PAID',
    method: 'CARD',
    createdAt: '2024-11-12T09:15:00Z',
    paidAt: '2024-11-12T09:15:30Z',
    installments: 3,
    cardBrand: 'Visa',
    cardLastDigits: '4242',
  },
  {
    id: 'TRX-003',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    customer: { name: 'Pedro Costa', email: 'pedro@email.com' },
    grossAmount: 850.00,
    fee: 12.75,
    netAmount: 837.25,
    status: 'PENDING',
    method: 'PIX',
    createdAt: '2024-11-12T08:45:00Z',
  },
  {
    id: 'TRX-004',
    establishment: { id: '3', name: 'Digital Store EIRELI', cnpj: '55566677000188' },
    customer: { name: 'Ana Oliveira', email: 'ana@email.com', document: '55566677788' },
    grossAmount: 4500.00,
    fee: 67.50,
    netAmount: 4432.50,
    status: 'FAILED',
    method: 'PIX',
    createdAt: '2024-11-11T16:20:00Z',
  },
  {
    id: 'TRX-005',
    establishment: { id: '2', name: 'Tech Solutions ME', cnpj: '98765432000110' },
    customer: { name: 'Carlos Mendes', email: 'carlos@email.com', document: '11122233344' },
    grossAmount: 1200.00,
    fee: 36.00,
    netAmount: 1164.00,
    status: 'REFUNDED',
    method: 'CARD',
    createdAt: '2024-11-10T14:00:00Z',
    paidAt: '2024-11-10T14:00:25Z',
    refundedAt: '2024-11-11T10:30:00Z',
    installments: 1,
    cardBrand: 'Mastercard',
    cardLastDigits: '5555',
  },
  {
    id: 'TRX-006',
    establishment: { id: '4', name: 'Comercio Rapido SA', cnpj: '11223344000156' },
    customer: { name: 'Beatriz Lima', email: 'beatriz@email.com' },
    grossAmount: 350.00,
    fee: 10.50,
    netAmount: 339.50,
    status: 'PENDING',
    method: 'BOLETO',
    createdAt: '2024-11-12T07:30:00Z',
    boletoDueDate: '2024-11-15',
    boletoBarcode: '23793.38128 60000.000003 00000.000409 1 84340000035000',
  },
  {
    id: 'TRX-007',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    customer: { name: 'Fernando Alves', email: 'fernando@email.com', document: '99988877766' },
    grossAmount: 6800.00,
    fee: 238.00,
    netAmount: 6562.00,
    status: 'PAID',
    method: 'CARD',
    createdAt: '2024-11-11T11:45:00Z',
    paidAt: '2024-11-11T11:45:20Z',
    installments: 12,
    cardBrand: 'Visa',
    cardLastDigits: '1234',
  },
  {
    id: 'TRX-008',
    establishment: { id: '3', name: 'Digital Store EIRELI', cnpj: '55566677000188' },
    customer: { name: 'Camila Martins', email: 'camila@email.com' },
    grossAmount: 189.90,
    fee: 2.85,
    netAmount: 187.05,
    status: 'PAID',
    method: 'PIX',
    endToEnd: 'E55566677202411111145000001',
    createdAt: '2024-11-11T09:00:00Z',
    paidAt: '2024-11-11T09:00:15Z',
  },
];

const mockSummary: TransactionSummary = {
  totalTransactions: 1847,
  totalGrossAmount: 2847320.50,
  totalNetAmount: 2762845.25,
  totalFees: 84475.25,
  byStatus: {
    paid: 1650,
    pending: 145,
    failed: 32,
    refunded: 20,
  },
  byMethod: {
    pix: 1200,
    card: 520,
    boleto: 127,
  },
};

const TransactionsPage: React.FC = () => {
  // State
  const [loading, setLoading] = useState(false);
  const [transactions, setTransactions] = useState<Transaction[]>(mockTransactions);
  const [summary, setSummary] = useState<TransactionSummary>(mockSummary);
  const [selectedTransaction, setSelectedTransaction] = useState<Transaction | null>(null);
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [refundModalVisible, setRefundModalVisible] = useState(false);
  const [disputeModalVisible, setDisputeModalVisible] = useState(false);

  // Filters
  const [filters, setFilters] = useState<TransactionFilters>({});
  const [searchText, setSearchText] = useState('');
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  // Pagination
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: PAGINATION_DEFAULTS.PAGE_SIZE,
    total: mockTransactions.length,
  });

  // Forms
  const [refundForm] = Form.useForm();
  const [disputeForm] = Form.useForm();

  // Handlers
  const handleSearch = useCallback((value: string) => {
    setSearchText(value);
    setFilters(prev => ({ ...prev, search: value }));
  }, []);

  const handleFilterChange = useCallback((key: keyof TransactionFilters, value: any) => {
    setFilters(prev => ({ ...prev, [key]: value }));
  }, []);

  const handleDateRangeChange = useCallback((dates: any) => {
    if (dates) {
      setFilters(prev => ({
        ...prev,
        startDate: dates[0]?.format('YYYY-MM-DD'),
        endDate: dates[1]?.format('YYYY-MM-DD'),
      }));
    } else {
      setFilters(prev => {
        const { startDate, endDate, ...rest } = prev;
        return rest;
      });
    }
  }, []);

  const handleClearFilters = useCallback(() => {
    setFilters({});
    setSearchText('');
  }, []);

  const handleRefresh = useCallback(() => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      message.success('Dados atualizados');
    }, 500);
  }, []);

  const handleExport = useCallback(async () => {
    message.loading({ content: 'Gerando exportação...', key: 'export' });
    setTimeout(() => {
      message.success({ content: 'Exportação gerada com sucesso!', key: 'export' });
    }, 1500);
  }, []);

  const handleViewDetails = useCallback((record: Transaction) => {
    setSelectedTransaction(record);
    setDetailsVisible(true);
  }, []);

  const handleCopyId = useCallback((id: string) => {
    navigator.clipboard.writeText(id);
    message.success('ID copiado!');
  }, []);

  const handleCopyEndToEnd = useCallback((endToEnd: string) => {
    navigator.clipboard.writeText(endToEnd);
    message.success('End-to-End copiado!');
  }, []);

  const handleDownloadReceipt = useCallback((record: Transaction) => {
    message.success('Comprovante baixado!');
  }, []);

  const handleRefund = useCallback((record: Transaction) => {
    setSelectedTransaction(record);
    refundForm.setFieldsValue({ amount: record.grossAmount });
    setRefundModalVisible(true);
  }, [refundForm]);

  const handleRefundSubmit = useCallback(async () => {
    try {
      await refundForm.validateFields();
      message.loading({ content: 'Processando estorno...', key: 'refund' });
      setTimeout(() => {
        message.success({ content: 'Estorno realizado com sucesso!', key: 'refund' });
        setRefundModalVisible(false);
        refundForm.resetFields();
      }, 1500);
    } catch (error) {
      // Validation failed
    }
  }, [refundForm]);

  const handleOpenDispute = useCallback((record: Transaction) => {
    setSelectedTransaction(record);
    setDisputeModalVisible(true);
  }, []);

  const handleDisputeSubmit = useCallback(async () => {
    try {
      await disputeForm.validateFields();
      message.loading({ content: 'Abrindo disputa...', key: 'dispute' });
      setTimeout(() => {
        message.success({ content: 'Disputa aberta com sucesso!', key: 'dispute' });
        setDisputeModalVisible(false);
        disputeForm.resetFields();
      }, 1500);
    } catch (error) {
      // Validation failed
    }
  }, [disputeForm]);

  const handleResendWebhook = useCallback((record: Transaction) => {
    message.loading({ content: 'Reenviando webhook...', key: 'webhook' });
    setTimeout(() => {
      message.success({ content: 'Webhook reenviado!', key: 'webhook' });
    }, 1000);
  }, []);

  // Filtered data
  const filteredTransactions = useMemo(() => {
    return transactions.filter(t => {
      if (filters.search) {
        const search = filters.search.toLowerCase();
        if (
          !t.id.toLowerCase().includes(search) &&
          !t.establishment.name.toLowerCase().includes(search) &&
          !t.customer.name.toLowerCase().includes(search) &&
          !t.customer.email.toLowerCase().includes(search) &&
          !(t.endToEnd?.toLowerCase().includes(search))
        ) {
          return false;
        }
      }
      if (filters.status && t.status !== filters.status) return false;
      if (filters.method && t.method !== filters.method) return false;
      if (filters.minAmount && t.grossAmount < filters.minAmount) return false;
      if (filters.maxAmount && t.grossAmount > filters.maxAmount) return false;
      return true;
    });
  }, [transactions, filters]);

  // Table columns
  const columns: ColumnsType<Transaction> = [
    {
      title: 'ID / End-to-End',
      key: 'id',
      width: 200,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Space size={4}>
            <Text strong style={{ fontFamily: 'monospace', fontSize: 12 }}>
              {record.id}
            </Text>
            <CopyButton text={record.id} size="small" />
          </Space>
          {record.endToEnd && (
            <Space size={4}>
              <Text type="secondary" style={{ fontFamily: 'monospace', fontSize: 10 }}>
                {record.endToEnd.substring(0, 20)}...
              </Text>
              <CopyButton text={record.endToEnd} size="small" tooltipText="Copiar End-to-End" />
            </Space>
          )}
        </Space>
      ),
    },
    {
      title: 'Estabelecimento',
      key: 'establishment',
      width: 200,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text strong>{record.establishment.name}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {formatCNPJ(record.establishment.cnpj)}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Cliente',
      key: 'customer',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>{record.customer.name}</Text>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.customer.email}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Valor Bruto',
      dataIndex: 'grossAmount',
      key: 'grossAmount',
      width: 130,
      align: 'right',
      sorter: (a, b) => a.grossAmount - b.grossAmount,
      render: (value) => (
        <Text strong style={{ fontFamily: 'monospace' }}>
          {formatCurrency(value)}
        </Text>
      ),
    },
    {
      title: 'Taxa',
      dataIndex: 'fee',
      key: 'fee',
      width: 100,
      align: 'right',
      render: (value) => (
        <Text type="secondary" style={{ fontFamily: 'monospace', fontSize: 12 }}>
          -{formatCurrency(value)}
        </Text>
      ),
    },
    {
      title: 'Valor Líquido',
      dataIndex: 'netAmount',
      key: 'netAmount',
      width: 130,
      align: 'right',
      render: (value) => (
        <Text style={{ fontFamily: 'monospace', color: '#52c41a' }}>
          {formatCurrency(value)}
        </Text>
      ),
    },
    {
      title: 'Método',
      dataIndex: 'method',
      key: 'method',
      width: 120,
      filters: [
        { text: 'PIX', value: 'PIX' },
        { text: 'Cartão', value: 'CARD' },
        { text: 'Boleto', value: 'BOLETO' },
      ],
      render: (method: TransactionMethod, record) => (
        <Space direction="vertical" size={0}>
          <Tag color={TRANSACTION_METHOD_COLORS[method]}>
            {TRANSACTION_METHOD_LABELS[method]}
          </Tag>
          {record.installments && record.installments > 1 && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              {record.installments}x {record.cardBrand} ****{record.cardLastDigits}
            </Text>
          )}
          {record.cardBrand && !record.installments && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              {record.cardBrand} ****{record.cardLastDigits}
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      filters: [
        { text: 'Pago', value: 'PAID' },
        { text: 'Pendente', value: 'PENDING' },
        { text: 'Falhou', value: 'FAILED' },
        { text: 'Estornado', value: 'REFUNDED' },
      ],
      render: (status: TransactionStatus) => (
        <Tag color={TRANSACTION_STATUS_COLORS[status]}>
          {TRANSACTION_STATUS_LABELS[status]}
        </Tag>
      ),
    },
    {
      title: 'Data',
      key: 'date',
      width: 160,
      sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
      defaultSortOrder: 'descend',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text style={{ fontSize: 12 }}>
            {formatDateTime(record.createdAt)}
          </Text>
          {record.paidAt && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              Pago: {formatDateTime(record.paidAt, 'HH:mm:ss')}
            </Text>
          )}
        </Space>
      ),
    },
    {
      title: '',
      key: 'actions',
      width: 50,
      fixed: 'right',
      render: (_, record) => {
        const items = [
          {
            key: 'view',
            label: 'Ver detalhes',
            icon: <EyeOutlined />,
            onClick: () => handleViewDetails(record),
          },
          {
            key: 'copy-id',
            label: 'Copiar ID',
            icon: <CopyOutlined />,
            onClick: () => handleCopyId(record.id),
          },
          ...(record.endToEnd ? [{
            key: 'copy-e2e',
            label: 'Copiar End-to-End',
            icon: <CopyOutlined />,
            onClick: () => handleCopyEndToEnd(record.endToEnd!),
          }] : []),
          {
            key: 'receipt',
            label: 'Baixar comprovante',
            icon: <FileTextOutlined />,
            onClick: () => handleDownloadReceipt(record),
          },
          { key: 'divider1', divider: true },
          {
            key: 'webhook',
            label: 'Reenviar Webhook',
            icon: <SendOutlined />,
            onClick: () => handleResendWebhook(record),
          },
          ...(record.status === 'PAID' ? [
            { key: 'divider2', divider: true },
            {
              key: 'refund',
              label: 'Estornar',
              icon: <RollbackOutlined />,
              danger: true,
              onClick: () => handleRefund(record),
            },
            {
              key: 'dispute',
              label: 'Abrir disputa',
              icon: <ExclamationCircleOutlined />,
              danger: true,
              onClick: () => handleOpenDispute(record),
            },
          ] : []),
        ];

        return <ActionMenu items={items} />;
      },
    },
  ];

  const handleTableChange = (paginationConfig: TablePaginationConfig) => {
    setPagination({
      current: paginationConfig.current || 1,
      pageSize: paginationConfig.pageSize || PAGINATION_DEFAULTS.PAGE_SIZE,
      total: filteredTransactions.length,
    });
  };

  return (
    <div style={{ padding: 0 }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ marginBottom: 8 }}>Transações</Title>
        <Text type="secondary">
          Gerencie e acompanhe todas as transações da plataforma
        </Text>
      </div>

      {/* Summary Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Total de Transações"
              value={summary.totalTransactions}
              prefix={<SwapOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Volume Bruto"
              value={summary.totalGrossAmount}
              prefix={<DollarOutlined />}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Volume Líquido"
              value={summary.totalNetAmount}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Total em Taxas"
              value={summary.totalFees}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Status Summary */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <CheckCircleOutlined style={{ fontSize: 20, color: '#52c41a' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong>{summary.byStatus.paid}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Pagas</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <ClockCircleOutlined style={{ fontSize: 20, color: '#faad14' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong>{summary.byStatus.pending}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Pendentes</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <CloseCircleOutlined style={{ fontSize: 20, color: '#ff4d4f' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong>{summary.byStatus.failed}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Falharam</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <RollbackOutlined style={{ fontSize: 20, color: '#8c8c8c' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong>{summary.byStatus.refunded}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Estornadas</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#f0fdf4' }}>
            <div style={{ color: '#10b981', fontWeight: 'bold', fontSize: 18 }}>
              {summary.byMethod.pix}
            </div>
            <Text type="secondary" style={{ fontSize: 12 }}>PIX</Text>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#eff6ff' }}>
            <div style={{ color: '#3b82f6', fontWeight: 'bold', fontSize: 18 }}>
              {summary.byMethod.card}
            </div>
            <Text type="secondary" style={{ fontSize: 12 }}>Cartão</Text>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#fffbeb' }}>
            <div style={{ color: '#f59e0b', fontWeight: 'bold', fontSize: 18 }}>
              {summary.byMethod.boleto}
            </div>
            <Text type="secondary" style={{ fontSize: 12 }}>Boleto</Text>
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space wrap>
              <Input.Search
                placeholder="Buscar por ID, estabelecimento, cliente..."
                allowClear
                style={{ width: 320 }}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                onSearch={handleSearch}
                prefix={<SearchOutlined />}
              />
              <Select
                placeholder="Status"
                allowClear
                style={{ width: 140 }}
                value={filters.status}
                onChange={(value) => handleFilterChange('status', value)}
                options={[
                  { label: 'Pago', value: 'PAID' },
                  { label: 'Pendente', value: 'PENDING' },
                  { label: 'Falhou', value: 'FAILED' },
                  { label: 'Estornado', value: 'REFUNDED' },
                ]}
              />
              <Select
                placeholder="Método"
                allowClear
                style={{ width: 130 }}
                value={filters.method}
                onChange={(value) => handleFilterChange('method', value)}
                options={[
                  { label: 'PIX', value: 'PIX' },
                  { label: 'Cartão', value: 'CARD' },
                  { label: 'Boleto', value: 'BOLETO' },
                ]}
              />
              <RangePicker
                placeholder={['Data início', 'Data fim']}
                onChange={handleDateRangeChange}
                format="DD/MM/YYYY"
              />
              <Button
                icon={<FilterOutlined />}
                onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
              >
                {showAdvancedFilters ? 'Menos filtros' : 'Mais filtros'}
              </Button>
            </Space>
          </Col>
          <Col>
            <Space>
              <Button onClick={handleClearFilters}>Limpar</Button>
              <Button icon={<ReloadOutlined />} onClick={handleRefresh}>
                Atualizar
              </Button>
              <Button type="primary" icon={<DownloadOutlined />} onClick={handleExport}>
                Exportar
              </Button>
            </Space>
          </Col>
        </Row>

        {/* Advanced Filters */}
        {showAdvancedFilters && (
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} sm={12} lg={6}>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                Valor mínimo
              </Text>
              <InputNumber
                style={{ width: '100%' }}
                placeholder="R$ 0,00"
                min={0}
                step={0.01}
                precision={2}
                formatter={(value) => `R$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                parser={(value) => value?.replace(/R\$\s?|(\.)/g, '').replace(',', '.') as any}
                value={filters.minAmount}
                onChange={(value) => handleFilterChange('minAmount', value)}
              />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                Valor máximo
              </Text>
              <InputNumber
                style={{ width: '100%' }}
                placeholder="R$ 0,00"
                min={0}
                step={0.01}
                precision={2}
                formatter={(value) => `R$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                parser={(value) => value?.replace(/R\$\s?|(\.)/g, '').replace(',', '.') as any}
                value={filters.maxAmount}
                onChange={(value) => handleFilterChange('maxAmount', value)}
              />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                Estabelecimento
              </Text>
              <Select
                style={{ width: '100%' }}
                placeholder="Selecione..."
                allowClear
                showSearch
                optionFilterProp="label"
                value={filters.establishmentId}
                onChange={(value) => handleFilterChange('establishmentId', value)}
                options={[
                  { label: 'Loja ABC Ltda', value: '1' },
                  { label: 'Tech Solutions ME', value: '2' },
                  { label: 'Digital Store EIRELI', value: '3' },
                  { label: 'Comercio Rapido SA', value: '4' },
                ]}
              />
            </Col>
          </Row>
        )}
      </Card>

      {/* Table */}
      <Card size="small">
        <Table
          columns={columns}
          dataSource={filteredTransactions}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} de ${total} transações`,
            pageSizeOptions: PAGINATION_DEFAULTS.PAGE_SIZE_OPTIONS,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1400 }}
          size="small"
        />
      </Card>

      {/* Details Drawer */}
      <Drawer
        title="Detalhes da Transação"
        placement="right"
        width={500}
        onClose={() => setDetailsVisible(false)}
        open={detailsVisible}
      >
        {selectedTransaction && (
          <div>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="ID">
                <Space>
                  <Text copyable style={{ fontFamily: 'monospace' }}>
                    {selectedTransaction.id}
                  </Text>
                </Space>
              </Descriptions.Item>
              {selectedTransaction.endToEnd && (
                <Descriptions.Item label="End-to-End">
                  <Text copyable style={{ fontFamily: 'monospace', fontSize: 11 }}>
                    {selectedTransaction.endToEnd}
                  </Text>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="Status">
                <Tag color={TRANSACTION_STATUS_COLORS[selectedTransaction.status]}>
                  {TRANSACTION_STATUS_LABELS[selectedTransaction.status]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Método">
                <Tag color={TRANSACTION_METHOD_COLORS[selectedTransaction.method]}>
                  {TRANSACTION_METHOD_LABELS[selectedTransaction.method]}
                </Tag>
                {selectedTransaction.installments && (
                  <Text style={{ marginLeft: 8 }}>
                    {selectedTransaction.installments}x {selectedTransaction.cardBrand}
                  </Text>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Valor Bruto">
                <Text strong>{formatCurrency(selectedTransaction.grossAmount)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Taxa">
                <Text type="danger">-{formatCurrency(selectedTransaction.fee)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Valor Líquido">
                <Text style={{ color: '#52c41a', fontWeight: 'bold' }}>
                  {formatCurrency(selectedTransaction.netAmount)}
                </Text>
              </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Estabelecimento
            </Title>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Nome">
                {selectedTransaction.establishment.name}
              </Descriptions.Item>
              <Descriptions.Item label="CNPJ">
                {formatCNPJ(selectedTransaction.establishment.cnpj)}
              </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Cliente
            </Title>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Nome">
                {selectedTransaction.customer.name}
              </Descriptions.Item>
              <Descriptions.Item label="E-mail">
                {selectedTransaction.customer.email}
              </Descriptions.Item>
              {selectedTransaction.customer.document && (
                <Descriptions.Item label="Documento">
                  {selectedTransaction.customer.document}
                </Descriptions.Item>
              )}
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Datas
            </Title>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Criada em">
                {formatDateTime(selectedTransaction.createdAt)}
              </Descriptions.Item>
              {selectedTransaction.paidAt && (
                <Descriptions.Item label="Paga em">
                  {formatDateTime(selectedTransaction.paidAt)}
                </Descriptions.Item>
              )}
              {selectedTransaction.refundedAt && (
                <Descriptions.Item label="Estornada em">
                  {formatDateTime(selectedTransaction.refundedAt)}
                </Descriptions.Item>
              )}
            </Descriptions>

            <div style={{ marginTop: 24 }}>
              <Space>
                <Button icon={<FileTextOutlined />} onClick={() => handleDownloadReceipt(selectedTransaction)}>
                  Baixar Comprovante
                </Button>
                <Button icon={<SendOutlined />} onClick={() => handleResendWebhook(selectedTransaction)}>
                  Reenviar Webhook
                </Button>
              </Space>
            </div>
          </div>
        )}
      </Drawer>

      {/* Refund Modal */}
      <Modal
        title="Estornar Transação"
        open={refundModalVisible}
        onCancel={() => {
          setRefundModalVisible(false);
          refundForm.resetFields();
        }}
        onOk={handleRefundSubmit}
        okText="Confirmar Estorno"
        okButtonProps={{ danger: true }}
      >
        <Form form={refundForm} layout="vertical">
          <Form.Item label="Transação">
            <Text strong>{selectedTransaction?.id}</Text>
          </Form.Item>
          <Form.Item
            name="amount"
            label="Valor do Estorno"
            rules={[{ required: true, message: 'Informe o valor' }]}
          >
            <InputNumber
              style={{ width: '100%' }}
              min={0.01}
              max={selectedTransaction?.grossAmount}
              precision={2}
              formatter={(value) => `R$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
              parser={(value) => value?.replace(/R\$\s?|(\.)/g, '').replace(',', '.') as any}
            />
          </Form.Item>
          <Form.Item name="reason" label="Motivo">
            <Input.TextArea rows={3} placeholder="Descreva o motivo do estorno..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Dispute Modal */}
      <Modal
        title="Abrir Disputa"
        open={disputeModalVisible}
        onCancel={() => {
          setDisputeModalVisible(false);
          disputeForm.resetFields();
        }}
        onOk={handleDisputeSubmit}
        okText="Abrir Disputa"
        okButtonProps={{ danger: true }}
      >
        <Form form={disputeForm} layout="vertical">
          <Form.Item label="Transação">
            <Text strong>{selectedTransaction?.id}</Text>
            <br />
            <Text type="secondary">
              {selectedTransaction && formatCurrency(selectedTransaction.grossAmount)}
            </Text>
          </Form.Item>
          <Form.Item
            name="reason"
            label="Motivo da Disputa"
            rules={[{ required: true, message: 'Selecione o motivo' }]}
          >
            <Select
              placeholder="Selecione..."
              options={[
                { label: 'Produto não entregue', value: 'PRODUCT_NOT_DELIVERED' },
                { label: 'Cobrança em duplicidade', value: 'DUPLICATE_CHARGE' },
                { label: 'Produto com defeito', value: 'DEFECTIVE_PRODUCT' },
                { label: 'Serviço não prestado', value: 'SERVICE_NOT_PROVIDED' },
                { label: 'Produto diferente do anunciado', value: 'WRONG_PRODUCT' },
              ]}
            />
          </Form.Item>
          <Form.Item name="description" label="Descrição">
            <Input.TextArea rows={4} placeholder="Descreva detalhes da disputa..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default TransactionsPage;
