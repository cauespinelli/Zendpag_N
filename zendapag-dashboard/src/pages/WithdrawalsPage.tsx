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
  Alert,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  DownloadOutlined,
  FilterOutlined,
  EyeOutlined,
  CopyOutlined,
  FileTextOutlined,
  StopOutlined,
  RedoOutlined,
  BankOutlined,
  DollarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  SyncOutlined,
  PlusOutlined,
  WalletOutlined,
} from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import dayjs from 'dayjs';

import { ActionMenu, CopyButton } from '@/components/shared';
import {
  WITHDRAWAL_STATUS_COLORS,
  WITHDRAWAL_STATUS_LABELS,
  WALLET_TYPE_LABELS,
  WITHDRAWAL_TYPE_LABELS,
  PIX_KEY_TYPE_LABELS,
  PAGINATION_DEFAULTS,
} from '@/utils/constants';
import { formatCurrency, formatDateTime, formatCNPJ, maskPixKey } from '@/utils/helpers';
import type {
  Withdrawal,
  WithdrawalStatus,
  WithdrawalType,
  WalletType,
  PixKeyType,
  WithdrawalSummary,
  WithdrawalFilters,
} from '@/types';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

// Mock data for development
const mockWithdrawals: Withdrawal[] = [
  {
    id: 'WD-001',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    amount: 5000.00,
    fee: 5.00,
    netAmount: 4995.00,
    pixKey: { value: '12345678901', type: 'CPF' },
    type: 'MANUAL',
    wallet: 'PIX',
    status: 'PROCESSED',
    acquirer: { name: 'Pagnet', code: 'PAGNET' },
    endToEnd: 'E12345678202411121234567890',
    createdAt: '2024-11-12T10:30:00Z',
    processedAt: '2024-11-12T10:35:00Z',
  },
  {
    id: 'WD-002',
    establishment: { id: '2', name: 'Tech Solutions ME', cnpj: '98765432000110' },
    amount: 15000.00,
    fee: 15.00,
    netAmount: 14985.00,
    pixKey: { value: 'financeiro@techsolutions.com', type: 'EMAIL' },
    type: 'AUTOMATIC',
    wallet: 'PIX',
    status: 'PROCESSING',
    acquirer: { name: 'Pagnet', code: 'PAGNET' },
    createdAt: '2024-11-12T09:00:00Z',
  },
  {
    id: 'WD-003',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    amount: 2500.00,
    fee: 2.50,
    netAmount: 2497.50,
    pixKey: { value: '+5511999998888', type: 'PHONE' },
    type: 'MANUAL',
    wallet: 'PIX',
    status: 'PENDING',
    acquirer: { name: 'Pagnet', code: 'PAGNET' },
    createdAt: '2024-11-12T08:45:00Z',
  },
  {
    id: 'WD-004',
    establishment: { id: '3', name: 'Digital Store EIRELI', cnpj: '55566677000188' },
    amount: 8000.00,
    fee: 8.00,
    netAmount: 7992.00,
    pixKey: { value: '55566677000188', type: 'CNPJ' },
    type: 'MANUAL',
    wallet: 'CARD',
    status: 'FAILED',
    acquirer: { name: 'Cartwave', code: 'CARTWAVE' },
    createdAt: '2024-11-11T16:20:00Z',
    failedReason: 'Chave PIX inválida ou não encontrada',
  },
  {
    id: 'WD-005',
    establishment: { id: '2', name: 'Tech Solutions ME', cnpj: '98765432000110' },
    amount: 3200.00,
    fee: 3.20,
    netAmount: 3196.80,
    pixKey: { value: 'abc123de-4567-890f-ghij-klmnopqrstuv', type: 'RANDOM' },
    type: 'AUTOMATIC',
    wallet: 'PIX',
    status: 'PROCESSED',
    acquirer: { name: 'Pagnet 2', code: 'PAGNET_2' },
    endToEnd: 'E98765432202411101400000001',
    createdAt: '2024-11-10T14:00:00Z',
    processedAt: '2024-11-10T14:05:00Z',
  },
  {
    id: 'WD-006',
    establishment: { id: '4', name: 'Comercio Rapido SA', cnpj: '11223344000156' },
    amount: 1800.00,
    fee: 1.80,
    netAmount: 1798.20,
    pixKey: { value: '11223344000156', type: 'CNPJ' },
    type: 'MANUAL',
    wallet: 'BOLETO',
    status: 'CANCELLED',
    acquirer: { name: 'Pagarme', code: 'PAGARME' },
    createdAt: '2024-11-09T11:30:00Z',
  },
  {
    id: 'WD-007',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    amount: 25000.00,
    fee: 25.00,
    netAmount: 24975.00,
    pixKey: { value: '12345678901', type: 'CPF' },
    type: 'AUTOMATIC',
    wallet: 'PIX',
    status: 'PROCESSED',
    acquirer: { name: 'Pagnet', code: 'PAGNET' },
    endToEnd: 'E12345678202411081000000001',
    createdAt: '2024-11-08T10:00:00Z',
    processedAt: '2024-11-08T10:02:00Z',
  },
];

const mockSummary: WithdrawalSummary = {
  totalWithdrawals: 847,
  totalAmount: 1847320.50,
  totalFees: 1847.32,
  totalNetAmount: 1845473.18,
  byStatus: {
    processed: 720,
    pending: 85,
    processing: 25,
    failed: 12,
    cancelled: 5,
  },
  byWallet: {
    pix: 680,
    card: 120,
    boleto: 47,
  },
  byType: {
    manual: 350,
    automatic: 497,
  },
};

const WithdrawalsPage: React.FC = () => {
  // State
  const [loading, setLoading] = useState(false);
  const [withdrawals, setWithdrawals] = useState<Withdrawal[]>(mockWithdrawals);
  const [summary, setSummary] = useState<WithdrawalSummary>(mockSummary);
  const [selectedWithdrawal, setSelectedWithdrawal] = useState<Withdrawal | null>(null);
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [cancelModalVisible, setCancelModalVisible] = useState(false);
  const [newWithdrawalVisible, setNewWithdrawalVisible] = useState(false);

  // Filters
  const [filters, setFilters] = useState<WithdrawalFilters>({});
  const [searchText, setSearchText] = useState('');
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  // Pagination
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: PAGINATION_DEFAULTS.PAGE_SIZE,
    total: mockWithdrawals.length,
  });

  // Forms
  const [cancelForm] = Form.useForm();
  const [newWithdrawalForm] = Form.useForm();

  // Handlers
  const handleSearch = useCallback((value: string) => {
    setSearchText(value);
    setFilters(prev => ({ ...prev, search: value }));
  }, []);

  const handleFilterChange = useCallback((key: keyof WithdrawalFilters, value: any) => {
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

  const handleViewDetails = useCallback((record: Withdrawal) => {
    setSelectedWithdrawal(record);
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

  const handleDownloadReceipt = useCallback((record: Withdrawal) => {
    message.success('Comprovante baixado!');
  }, []);

  const handleCancel = useCallback((record: Withdrawal) => {
    setSelectedWithdrawal(record);
    setCancelModalVisible(true);
  }, []);

  const handleCancelSubmit = useCallback(async () => {
    try {
      await cancelForm.validateFields();
      message.loading({ content: 'Cancelando saque...', key: 'cancel' });
      setTimeout(() => {
        message.success({ content: 'Saque cancelado com sucesso!', key: 'cancel' });
        setCancelModalVisible(false);
        cancelForm.resetFields();
        // Update local state
        setWithdrawals(prev =>
          prev.map(w =>
            w.id === selectedWithdrawal?.id ? { ...w, status: 'CANCELLED' as WithdrawalStatus } : w
          )
        );
      }, 1500);
    } catch (error) {
      // Validation failed
    }
  }, [cancelForm, selectedWithdrawal]);

  const handleRetry = useCallback((record: Withdrawal) => {
    Modal.confirm({
      title: 'Reprocessar Saque',
      content: `Deseja reprocessar o saque ${record.id}?`,
      okText: 'Sim, reprocessar',
      cancelText: 'Não',
      onOk: () => {
        message.loading({ content: 'Reprocessando...', key: 'retry' });
        setTimeout(() => {
          message.success({ content: 'Saque enviado para reprocessamento!', key: 'retry' });
          setWithdrawals(prev =>
            prev.map(w =>
              w.id === record.id ? { ...w, status: 'PROCESSING' as WithdrawalStatus } : w
            )
          );
        }, 1000);
      },
    });
  }, []);

  const handleNewWithdrawal = useCallback(() => {
    setNewWithdrawalVisible(true);
  }, []);

  const handleNewWithdrawalSubmit = useCallback(async () => {
    try {
      await newWithdrawalForm.validateFields();
      message.loading({ content: 'Criando saque...', key: 'new' });
      setTimeout(() => {
        message.success({ content: 'Saque criado com sucesso!', key: 'new' });
        setNewWithdrawalVisible(false);
        newWithdrawalForm.resetFields();
      }, 1500);
    } catch (error) {
      // Validation failed
    }
  }, [newWithdrawalForm]);

  // Filtered data
  const filteredWithdrawals = useMemo(() => {
    return withdrawals.filter(w => {
      if (filters.search) {
        const search = filters.search.toLowerCase();
        if (
          !w.id.toLowerCase().includes(search) &&
          !w.establishment.name.toLowerCase().includes(search) &&
          !w.pixKey.value.toLowerCase().includes(search) &&
          !(w.endToEnd?.toLowerCase().includes(search))
        ) {
          return false;
        }
      }
      if (filters.status && w.status !== filters.status) return false;
      if (filters.wallet && w.wallet !== filters.wallet) return false;
      if (filters.type && w.type !== filters.type) return false;
      if (filters.minAmount && w.amount < filters.minAmount) return false;
      if (filters.maxAmount && w.amount > filters.maxAmount) return false;
      return true;
    });
  }, [withdrawals, filters]);

  // Status icon
  const getStatusIcon = (status: WithdrawalStatus) => {
    const icons = {
      PROCESSED: <CheckCircleOutlined />,
      PENDING: <ClockCircleOutlined />,
      PROCESSING: <SyncOutlined spin />,
      FAILED: <CloseCircleOutlined />,
      CANCELLED: <StopOutlined />,
    };
    return icons[status] || <ClockCircleOutlined />;
  };

  // Table columns
  const columns: ColumnsType<Withdrawal> = [
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
      title: 'Valor',
      dataIndex: 'amount',
      key: 'amount',
      width: 130,
      align: 'right',
      sorter: (a, b) => a.amount - b.amount,
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
      title: 'Líquido',
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
      title: 'Chave PIX',
      key: 'pixKey',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Tag color="blue">{PIX_KEY_TYPE_LABELS[record.pixKey.type] || record.pixKey.type}</Tag>
          <Tooltip title={record.pixKey.value}>
            <Text style={{ fontSize: 12 }}>
              {maskPixKey(record.pixKey.value, record.pixKey.type)}
            </Text>
          </Tooltip>
        </Space>
      ),
    },
    {
      title: 'Carteira',
      key: 'wallet',
      width: 120,
      filters: [
        { text: 'PIX', value: 'PIX' },
        { text: 'Cartão', value: 'CARD' },
        { text: 'Boleto', value: 'BOLETO' },
      ],
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Tag color={record.wallet === 'PIX' ? 'green' : record.wallet === 'CARD' ? 'blue' : 'orange'}>
            {WALLET_TYPE_LABELS[record.wallet]}
          </Tag>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {WITHDRAWAL_TYPE_LABELS[record.type]}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      filters: [
        { text: 'Processado', value: 'PROCESSED' },
        { text: 'Pendente', value: 'PENDING' },
        { text: 'Processando', value: 'PROCESSING' },
        { text: 'Falhou', value: 'FAILED' },
        { text: 'Cancelado', value: 'CANCELLED' },
      ],
      render: (status: WithdrawalStatus) => (
        <Tag
          color={WITHDRAWAL_STATUS_COLORS[status]}
          icon={getStatusIcon(status)}
        >
          {WITHDRAWAL_STATUS_LABELS[status]}
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
          {record.processedAt && (
            <Text type="secondary" style={{ fontSize: 11 }}>
              Proc: {formatDateTime(record.processedAt, 'HH:mm:ss')}
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
          ...(record.status === 'PROCESSED' ? [{
            key: 'receipt',
            label: 'Baixar comprovante',
            icon: <FileTextOutlined />,
            onClick: () => handleDownloadReceipt(record),
          }] : []),
          ...(record.status === 'FAILED' ? [
            { key: 'divider1', divider: true },
            {
              key: 'retry',
              label: 'Reprocessar',
              icon: <RedoOutlined />,
              onClick: () => handleRetry(record),
            },
          ] : []),
          ...(['PENDING', 'PROCESSING'].includes(record.status) ? [
            { key: 'divider2', divider: true },
            {
              key: 'cancel',
              label: 'Cancelar',
              icon: <StopOutlined />,
              danger: true,
              onClick: () => handleCancel(record),
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
      total: filteredWithdrawals.length,
    });
  };

  return (
    <div style={{ padding: 0 }}>
      {/* Header */}
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
        <div>
          <Title level={4} style={{ marginBottom: 8 }}>Saques</Title>
          <Text type="secondary">
            Gerencie e acompanhe todos os saques da plataforma
          </Text>
        </div>
        <Button type="primary" icon={<PlusOutlined />} onClick={handleNewWithdrawal}>
          Novo Saque
        </Button>
      </div>

      {/* Summary Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Total de Saques"
              value={summary.totalWithdrawals}
              prefix={<BankOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Volume Total"
              value={summary.totalAmount}
              prefix={<DollarOutlined />}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
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
      </Row>

      {/* Status Summary */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center' }}>
            <CheckCircleOutlined style={{ fontSize: 20, color: '#52c41a' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong>{summary.byStatus.processed}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Processados</Text>
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
            <SyncOutlined style={{ fontSize: 20, color: '#1890ff' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong>{summary.byStatus.processing}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Processando</Text>
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
          <Card size="small" style={{ textAlign: 'center', background: '#f0fdf4' }}>
            <WalletOutlined style={{ fontSize: 16, color: '#10b981' }} />
            <div style={{ marginTop: 4 }}>
              <Text strong style={{ color: '#10b981' }}>{summary.byWallet.pix}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>PIX</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#eff6ff' }}>
            <WalletOutlined style={{ fontSize: 16, color: '#3b82f6' }} />
            <div style={{ marginTop: 4 }}>
              <Text strong style={{ color: '#3b82f6' }}>{summary.byWallet.card}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>Cartão</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#fef3c7' }}>
            <div style={{ marginTop: 4 }}>
              <Text strong style={{ color: '#d97706' }}>{summary.byType.manual}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>Manuais</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#f3e8ff' }}>
            <div style={{ marginTop: 4 }}>
              <Text strong style={{ color: '#7c3aed' }}>{summary.byType.automatic}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>Automáticos</Text>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card size="small" style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space wrap>
              <Input.Search
                placeholder="Buscar por ID, estabelecimento, chave PIX..."
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
                style={{ width: 150 }}
                value={filters.status}
                onChange={(value) => handleFilterChange('status', value)}
                options={[
                  { label: 'Processado', value: 'PROCESSED' },
                  { label: 'Pendente', value: 'PENDING' },
                  { label: 'Processando', value: 'PROCESSING' },
                  { label: 'Falhou', value: 'FAILED' },
                  { label: 'Cancelado', value: 'CANCELLED' },
                ]}
              />
              <Select
                placeholder="Carteira"
                allowClear
                style={{ width: 130 }}
                value={filters.wallet}
                onChange={(value) => handleFilterChange('wallet', value)}
                options={[
                  { label: 'PIX', value: 'PIX' },
                  { label: 'Cartão', value: 'CARD' },
                  { label: 'Boleto', value: 'BOLETO' },
                ]}
              />
              <Select
                placeholder="Tipo"
                allowClear
                style={{ width: 130 }}
                value={filters.type}
                onChange={(value) => handleFilterChange('type', value)}
                options={[
                  { label: 'Manual', value: 'MANUAL' },
                  { label: 'Automático', value: 'AUTOMATIC' },
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
                {showAdvancedFilters ? 'Menos' : 'Mais'}
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
          dataSource={filteredWithdrawals}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} de ${total} saques`,
            pageSizeOptions: PAGINATION_DEFAULTS.PAGE_SIZE_OPTIONS,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1500 }}
          size="small"
        />
      </Card>

      {/* Details Drawer */}
      <Drawer
        title="Detalhes do Saque"
        placement="right"
        width={500}
        onClose={() => setDetailsVisible(false)}
        open={detailsVisible}
      >
        {selectedWithdrawal && (
          <div>
            {selectedWithdrawal.status === 'FAILED' && selectedWithdrawal.failedReason && (
              <Alert
                message="Motivo da Falha"
                description={selectedWithdrawal.failedReason}
                type="error"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}

            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="ID">
                <Text copyable style={{ fontFamily: 'monospace' }}>
                  {selectedWithdrawal.id}
                </Text>
              </Descriptions.Item>
              {selectedWithdrawal.endToEnd && (
                <Descriptions.Item label="End-to-End">
                  <Text copyable style={{ fontFamily: 'monospace', fontSize: 11 }}>
                    {selectedWithdrawal.endToEnd}
                  </Text>
                </Descriptions.Item>
              )}
              <Descriptions.Item label="Status">
                <Tag
                  color={WITHDRAWAL_STATUS_COLORS[selectedWithdrawal.status]}
                  icon={getStatusIcon(selectedWithdrawal.status)}
                >
                  {WITHDRAWAL_STATUS_LABELS[selectedWithdrawal.status]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Valor">
                <Text strong>{formatCurrency(selectedWithdrawal.amount)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Taxa">
                <Text type="danger">-{formatCurrency(selectedWithdrawal.fee)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Valor Líquido">
                <Text style={{ color: '#52c41a', fontWeight: 'bold' }}>
                  {formatCurrency(selectedWithdrawal.netAmount)}
                </Text>
              </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Estabelecimento
            </Title>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Nome">
                {selectedWithdrawal.establishment.name}
              </Descriptions.Item>
              <Descriptions.Item label="CNPJ">
                {formatCNPJ(selectedWithdrawal.establishment.cnpj)}
              </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Destino
            </Title>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Tipo Chave">
                <Tag color="blue">
                  {PIX_KEY_TYPE_LABELS[selectedWithdrawal.pixKey.type] || selectedWithdrawal.pixKey.type}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Chave PIX">
                <Text copyable>{selectedWithdrawal.pixKey.value}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Carteira">
                {WALLET_TYPE_LABELS[selectedWithdrawal.wallet]}
              </Descriptions.Item>
              <Descriptions.Item label="Tipo">
                {WITHDRAWAL_TYPE_LABELS[selectedWithdrawal.type]}
              </Descriptions.Item>
              <Descriptions.Item label="Adquirente">
                {selectedWithdrawal.acquirer.name}
              </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Datas
            </Title>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Criado em">
                {formatDateTime(selectedWithdrawal.createdAt)}
              </Descriptions.Item>
              {selectedWithdrawal.processedAt && (
                <Descriptions.Item label="Processado em">
                  {formatDateTime(selectedWithdrawal.processedAt)}
                </Descriptions.Item>
              )}
            </Descriptions>

            <div style={{ marginTop: 24 }}>
              <Space>
                {selectedWithdrawal.status === 'PROCESSED' && (
                  <Button icon={<FileTextOutlined />} onClick={() => handleDownloadReceipt(selectedWithdrawal)}>
                    Baixar Comprovante
                  </Button>
                )}
                {selectedWithdrawal.status === 'FAILED' && (
                  <Button icon={<RedoOutlined />} onClick={() => handleRetry(selectedWithdrawal)}>
                    Reprocessar
                  </Button>
                )}
                {['PENDING', 'PROCESSING'].includes(selectedWithdrawal.status) && (
                  <Button danger icon={<StopOutlined />} onClick={() => handleCancel(selectedWithdrawal)}>
                    Cancelar
                  </Button>
                )}
              </Space>
            </div>
          </div>
        )}
      </Drawer>

      {/* Cancel Modal */}
      <Modal
        title="Cancelar Saque"
        open={cancelModalVisible}
        onCancel={() => {
          setCancelModalVisible(false);
          cancelForm.resetFields();
        }}
        onOk={handleCancelSubmit}
        okText="Confirmar Cancelamento"
        okButtonProps={{ danger: true }}
      >
        <Form form={cancelForm} layout="vertical">
          <Form.Item label="Saque">
            <Text strong>{selectedWithdrawal?.id}</Text>
            <br />
            <Text type="secondary">
              {selectedWithdrawal && formatCurrency(selectedWithdrawal.amount)}
            </Text>
          </Form.Item>
          <Form.Item name="reason" label="Motivo do cancelamento">
            <Input.TextArea rows={3} placeholder="Descreva o motivo do cancelamento..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* New Withdrawal Modal */}
      <Modal
        title="Novo Saque"
        open={newWithdrawalVisible}
        onCancel={() => {
          setNewWithdrawalVisible(false);
          newWithdrawalForm.resetFields();
        }}
        onOk={handleNewWithdrawalSubmit}
        okText="Criar Saque"
        width={600}
      >
        <Form form={newWithdrawalForm} layout="vertical">
          <Form.Item
            name="establishmentId"
            label="Estabelecimento"
            rules={[{ required: true, message: 'Selecione o estabelecimento' }]}
          >
            <Select
              placeholder="Selecione..."
              showSearch
              optionFilterProp="label"
              options={[
                { label: 'Loja ABC Ltda', value: '1' },
                { label: 'Tech Solutions ME', value: '2' },
                { label: 'Digital Store EIRELI', value: '3' },
                { label: 'Comercio Rapido SA', value: '4' },
              ]}
            />
          </Form.Item>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="wallet"
                label="Carteira"
                rules={[{ required: true, message: 'Selecione a carteira' }]}
              >
                <Select
                  placeholder="Selecione..."
                  options={[
                    { label: 'Carteira PIX', value: 'PIX' },
                    { label: 'Carteira Cartão', value: 'CARD' },
                    { label: 'Carteira Boleto', value: 'BOLETO' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="amount"
                label="Valor"
                rules={[{ required: true, message: 'Informe o valor' }]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={0.01}
                  precision={2}
                  formatter={(value) => `R$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                  parser={(value) => value?.replace(/R\$\s?|(\.)/g, '').replace(',', '.') as any}
                />
              </Form.Item>
            </Col>
          </Row>
          <Row gutter={16}>
            <Col span={12}>
              <Form.Item
                name="pixKeyType"
                label="Tipo de Chave PIX"
                rules={[{ required: true, message: 'Selecione o tipo' }]}
              >
                <Select
                  placeholder="Selecione..."
                  options={[
                    { label: 'CPF', value: 'CPF' },
                    { label: 'CNPJ', value: 'CNPJ' },
                    { label: 'E-mail', value: 'EMAIL' },
                    { label: 'Telefone', value: 'PHONE' },
                    { label: 'Chave Aleatória', value: 'RANDOM' },
                  ]}
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="pixKeyValue"
                label="Chave PIX"
                rules={[{ required: true, message: 'Informe a chave' }]}
              >
                <Input placeholder="Digite a chave PIX" />
              </Form.Item>
            </Col>
          </Row>
        </Form>
      </Modal>
    </div>
  );
};

export default WithdrawalsPage;
