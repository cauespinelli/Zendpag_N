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
  Timeline,
  Badge,
  Divider,
} from 'antd';
import {
  SearchOutlined,
  ReloadOutlined,
  DownloadOutlined,
  FilterOutlined,
  EyeOutlined,
  CopyOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  ClockCircleOutlined,
  FileSearchOutlined,
  MessageOutlined,
  FlagOutlined,
  WarningOutlined,
  FireOutlined,
  UserOutlined,
} from '@ant-design/icons';
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table';
import dayjs from 'dayjs';

import { ActionMenu, CopyButton } from '@/components/shared';
import {
  DISPUTE_STATUS_COLORS,
  DISPUTE_STATUS_LABELS,
  DISPUTE_PRIORITY_COLORS,
  DISPUTE_PRIORITY_LABELS,
  PAGINATION_DEFAULTS,
} from '@/utils/constants';
import { formatCurrency, formatDateTime, formatCNPJ } from '@/utils/helpers';
import { DISPUTE_REASON_LABELS } from '@/types';
import type {
  Dispute,
  DisputeStatus,
  DisputeReason,
  DisputeSummary,
  DisputeFilters,
} from '@/types';

const { Title, Text, Paragraph } = Typography;
const { RangePicker } = DatePicker;
const { TextArea } = Input;

// Mock data for development
const mockDisputes: Dispute[] = [
  {
    id: 'DSP-001',
    transactionId: 'TRX-001',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    customer: { name: 'João Silva', email: 'joao@email.com', document: '12345678901' },
    amount: 1500.00,
    reason: 'PRODUCT_NOT_DELIVERED',
    status: 'OPEN',
    priority: 'HIGH',
    description: 'Cliente afirma que não recebeu o produto após 30 dias da compra.',
    createdAt: '2024-11-12T10:30:00Z',
  },
  {
    id: 'DSP-002',
    transactionId: 'TRX-015',
    establishment: { id: '2', name: 'Tech Solutions ME', cnpj: '98765432000110' },
    customer: { name: 'Maria Santos', email: 'maria@email.com', document: '98765432100' },
    amount: 2999.90,
    reason: 'DEFECTIVE_PRODUCT',
    status: 'ANALYZING',
    priority: 'CRITICAL',
    description: 'Produto chegou com defeito de fabricação, não funciona.',
    assignedTo: 'Carlos Analista',
    createdAt: '2024-11-11T14:20:00Z',
    updatedAt: '2024-11-12T09:00:00Z',
  },
  {
    id: 'DSP-003',
    transactionId: 'TRX-022',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    customer: { name: 'Pedro Costa', email: 'pedro@email.com' },
    amount: 450.00,
    reason: 'DUPLICATE_CHARGE',
    status: 'RESOLVED',
    priority: 'MEDIUM',
    description: 'Cobrança duplicada no cartão de crédito.',
    resolution: 'Estorno realizado integralmente.',
    createdAt: '2024-11-08T16:45:00Z',
    resolvedAt: '2024-11-10T11:30:00Z',
  },
  {
    id: 'DSP-004',
    transactionId: 'TRX-031',
    establishment: { id: '3', name: 'Digital Store EIRELI', cnpj: '55566677000188' },
    customer: { name: 'Ana Oliveira', email: 'ana@email.com', document: '55566677788' },
    amount: 890.00,
    reason: 'WRONG_PRODUCT',
    status: 'REJECTED',
    priority: 'LOW',
    description: 'Cliente alega produto diferente do pedido.',
    resolution: 'Após análise, verificamos que o produto está correto conforme descrição.',
    createdAt: '2024-11-05T09:15:00Z',
    resolvedAt: '2024-11-07T14:00:00Z',
  },
  {
    id: 'DSP-005',
    transactionId: 'TRX-045',
    establishment: { id: '2', name: 'Tech Solutions ME', cnpj: '98765432000110' },
    customer: { name: 'Carlos Mendes', email: 'carlos@email.com', document: '11122233344' },
    amount: 3200.00,
    reason: 'SERVICE_NOT_PROVIDED',
    status: 'OPEN',
    priority: 'HIGH',
    description: 'Serviço de instalação não foi realizado após pagamento.',
    createdAt: '2024-11-12T08:00:00Z',
  },
  {
    id: 'DSP-006',
    transactionId: 'TRX-052',
    establishment: { id: '4', name: 'Comercio Rapido SA', cnpj: '11223344000156' },
    customer: { name: 'Beatriz Lima', email: 'beatriz@email.com' },
    amount: 180.00,
    reason: 'DELAYED_DELIVERY',
    status: 'ANALYZING',
    priority: 'MEDIUM',
    description: 'Entrega atrasada em mais de 15 dias.',
    assignedTo: 'Ana Analista',
    createdAt: '2024-11-10T11:30:00Z',
    updatedAt: '2024-11-11T16:00:00Z',
  },
  {
    id: 'DSP-007',
    transactionId: 'TRX-067',
    establishment: { id: '1', name: 'Loja ABC Ltda', cnpj: '12345678000190' },
    customer: { name: 'Fernando Alves', email: 'fernando@email.com', document: '99988877766' },
    amount: 5500.00,
    reason: 'REFUND_NOT_PROCESSED',
    status: 'OPEN',
    priority: 'CRITICAL',
    description: 'Estorno prometido há 30 dias não foi processado.',
    createdAt: '2024-11-11T15:45:00Z',
  },
];

const mockSummary: DisputeSummary = {
  totalDisputes: 156,
  totalAmount: 487520.50,
  byStatus: {
    open: 45,
    analyzing: 32,
    resolved: 67,
    rejected: 12,
  },
  byPriority: {
    low: 28,
    medium: 52,
    high: 48,
    critical: 28,
  },
  avgResolutionTime: 72,
  resolvedThisMonth: 23,
};

const DisputesPage: React.FC = () => {
  // State
  const [loading, setLoading] = useState(false);
  const [disputes, setDisputes] = useState<Dispute[]>(mockDisputes);
  const [summary, setSummary] = useState<DisputeSummary>(mockSummary);
  const [selectedDispute, setSelectedDispute] = useState<Dispute | null>(null);
  const [detailsVisible, setDetailsVisible] = useState(false);
  const [resolveModalVisible, setResolveModalVisible] = useState(false);
  const [analyzeModalVisible, setAnalyzeModalVisible] = useState(false);
  const [noteModalVisible, setNoteModalVisible] = useState(false);

  // Filters
  const [filters, setFilters] = useState<DisputeFilters>({});
  const [searchText, setSearchText] = useState('');
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);

  // Pagination
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: PAGINATION_DEFAULTS.PAGE_SIZE,
    total: mockDisputes.length,
  });

  // Forms
  const [resolveForm] = Form.useForm();
  const [analyzeForm] = Form.useForm();
  const [noteForm] = Form.useForm();

  // Handlers
  const handleSearch = useCallback((value: string) => {
    setSearchText(value);
    setFilters(prev => ({ ...prev, search: value }));
  }, []);

  const handleFilterChange = useCallback((key: keyof DisputeFilters, value: any) => {
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

  const handleViewDetails = useCallback((record: Dispute) => {
    setSelectedDispute(record);
    setDetailsVisible(true);
  }, []);

  const handleCopyId = useCallback((id: string) => {
    navigator.clipboard.writeText(id);
    message.success('ID copiado!');
  }, []);

  const handleStartAnalysis = useCallback((record: Dispute) => {
    setSelectedDispute(record);
    setAnalyzeModalVisible(true);
  }, []);

  const handleAnalyzeSubmit = useCallback(async () => {
    try {
      const values = await analyzeForm.validateFields();
      message.loading({ content: 'Iniciando análise...', key: 'analyze' });
      setTimeout(() => {
        message.success({ content: 'Análise iniciada!', key: 'analyze' });
        setAnalyzeModalVisible(false);
        analyzeForm.resetFields();
        setDisputes(prev =>
          prev.map(d =>
            d.id === selectedDispute?.id
              ? { ...d, status: 'ANALYZING' as DisputeStatus, assignedTo: values.assignedTo }
              : d
          )
        );
      }, 1000);
    } catch (error) {
      // Validation failed
    }
  }, [analyzeForm, selectedDispute]);

  const handleResolve = useCallback((record: Dispute) => {
    setSelectedDispute(record);
    resolveForm.setFieldsValue({ refundAmount: record.amount });
    setResolveModalVisible(true);
  }, [resolveForm]);

  const handleResolveSubmit = useCallback(async () => {
    try {
      const values = await resolveForm.validateFields();
      message.loading({ content: 'Resolvendo disputa...', key: 'resolve' });
      setTimeout(() => {
        const newStatus = values.resolution === 'REJECTED' ? 'REJECTED' : 'RESOLVED';
        message.success({ content: 'Disputa resolvida!', key: 'resolve' });
        setResolveModalVisible(false);
        resolveForm.resetFields();
        setDisputes(prev =>
          prev.map(d =>
            d.id === selectedDispute?.id
              ? {
                  ...d,
                  status: newStatus as DisputeStatus,
                  resolution: values.notes,
                  resolvedAt: new Date().toISOString(),
                }
              : d
          )
        );
      }, 1500);
    } catch (error) {
      // Validation failed
    }
  }, [resolveForm, selectedDispute]);

  const handleAddNote = useCallback((record: Dispute) => {
    setSelectedDispute(record);
    setNoteModalVisible(true);
  }, []);

  const handleNoteSubmit = useCallback(async () => {
    try {
      await noteForm.validateFields();
      message.loading({ content: 'Adicionando nota...', key: 'note' });
      setTimeout(() => {
        message.success({ content: 'Nota adicionada!', key: 'note' });
        setNoteModalVisible(false);
        noteForm.resetFields();
      }, 1000);
    } catch (error) {
      // Validation failed
    }
  }, [noteForm]);

  const handleChangePriority = useCallback((record: Dispute, priority: string) => {
    message.loading({ content: 'Alterando prioridade...', key: 'priority' });
    setTimeout(() => {
      message.success({ content: 'Prioridade alterada!', key: 'priority' });
      setDisputes(prev =>
        prev.map(d =>
          d.id === record.id ? { ...d, priority: priority as any } : d
        )
      );
    }, 500);
  }, []);

  // Filtered data
  const filteredDisputes = useMemo(() => {
    return disputes.filter(d => {
      if (filters.search) {
        const search = filters.search.toLowerCase();
        if (
          !d.id.toLowerCase().includes(search) &&
          !d.transactionId.toLowerCase().includes(search) &&
          !d.establishment.name.toLowerCase().includes(search) &&
          !d.customer.name.toLowerCase().includes(search) &&
          !d.customer.email.toLowerCase().includes(search)
        ) {
          return false;
        }
      }
      if (filters.status && d.status !== filters.status) return false;
      if (filters.reason && d.reason !== filters.reason) return false;
      if (filters.priority && d.priority !== filters.priority) return false;
      if (filters.minAmount && d.amount < filters.minAmount) return false;
      if (filters.maxAmount && d.amount > filters.maxAmount) return false;
      return true;
    });
  }, [disputes, filters]);

  // Status icon
  const getStatusIcon = (status: DisputeStatus) => {
    const icons = {
      OPEN: <ExclamationCircleOutlined />,
      ANALYZING: <FileSearchOutlined />,
      RESOLVED: <CheckCircleOutlined />,
      REJECTED: <CloseCircleOutlined />,
    };
    return icons[status] || <ExclamationCircleOutlined />;
  };

  // Priority icon
  const getPriorityIcon = (priority?: string) => {
    const icons = {
      LOW: <FlagOutlined style={{ color: DISPUTE_PRIORITY_COLORS.LOW }} />,
      MEDIUM: <FlagOutlined style={{ color: DISPUTE_PRIORITY_COLORS.MEDIUM }} />,
      HIGH: <WarningOutlined style={{ color: DISPUTE_PRIORITY_COLORS.HIGH }} />,
      CRITICAL: <FireOutlined style={{ color: DISPUTE_PRIORITY_COLORS.CRITICAL }} />,
    };
    return priority ? icons[priority] || null : null;
  };

  // Table columns
  const columns: ColumnsType<Dispute> = [
    {
      title: 'ID / Transação',
      key: 'id',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Space size={4}>
            <Text strong style={{ fontFamily: 'monospace', fontSize: 12 }}>
              {record.id}
            </Text>
            <CopyButton text={record.id} size="small" />
          </Space>
          <Space size={4}>
            <Text type="secondary" style={{ fontFamily: 'monospace', fontSize: 11 }}>
              {record.transactionId}
            </Text>
            <CopyButton text={record.transactionId} size="small" tooltipText="Copiar ID Transação" />
          </Space>
        </Space>
      ),
    },
    {
      title: 'Estabelecimento',
      key: 'establishment',
      width: 180,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text strong>{record.establishment.name}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {formatCNPJ(record.establishment.cnpj)}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Cliente',
      key: 'customer',
      width: 160,
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text>{record.customer.name}</Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {record.customer.email}
          </Text>
        </Space>
      ),
    },
    {
      title: 'Valor',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      align: 'right',
      sorter: (a, b) => a.amount - b.amount,
      render: (value) => (
        <Text strong style={{ fontFamily: 'monospace', color: '#ff4d4f' }}>
          {formatCurrency(value)}
        </Text>
      ),
    },
    {
      title: 'Motivo',
      dataIndex: 'reason',
      key: 'reason',
      width: 200,
      ellipsis: true,
      filters: [
        { text: 'Produto não entregue', value: 'PRODUCT_NOT_DELIVERED' },
        { text: 'Cobrança duplicada', value: 'DUPLICATE_CHARGE' },
        { text: 'Produto com defeito', value: 'DEFECTIVE_PRODUCT' },
        { text: 'Serviço não prestado', value: 'SERVICE_NOT_PROVIDED' },
        { text: 'Produto errado', value: 'WRONG_PRODUCT' },
      ],
      render: (reason: DisputeReason) => (
        <Tooltip title={DISPUTE_REASON_LABELS[reason]}>
          <Text style={{ fontSize: 12 }}>
            {DISPUTE_REASON_LABELS[reason]?.substring(0, 40)}...
          </Text>
        </Tooltip>
      ),
    },
    {
      title: 'Prioridade',
      dataIndex: 'priority',
      key: 'priority',
      width: 110,
      filters: [
        { text: 'Baixa', value: 'LOW' },
        { text: 'Média', value: 'MEDIUM' },
        { text: 'Alta', value: 'HIGH' },
        { text: 'Crítica', value: 'CRITICAL' },
      ],
      render: (priority: string) => priority && (
        <Tag
          color={DISPUTE_PRIORITY_COLORS[priority]}
          icon={getPriorityIcon(priority)}
        >
          {DISPUTE_PRIORITY_LABELS[priority]}
        </Tag>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 130,
      filters: [
        { text: 'Aberta', value: 'OPEN' },
        { text: 'Em Análise', value: 'ANALYZING' },
        { text: 'Resolvida', value: 'RESOLVED' },
        { text: 'Rejeitada', value: 'REJECTED' },
      ],
      render: (status: DisputeStatus) => (
        <Tag
          color={DISPUTE_STATUS_COLORS[status]}
          icon={getStatusIcon(status)}
        >
          {DISPUTE_STATUS_LABELS[status]}
        </Tag>
      ),
    },
    {
      title: 'Criada em',
      key: 'createdAt',
      width: 140,
      sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
      defaultSortOrder: 'descend',
      render: (_, record) => (
        <Space direction="vertical" size={0}>
          <Text style={{ fontSize: 12 }}>
            {formatDateTime(record.createdAt, 'DD/MM/YYYY')}
          </Text>
          <Text type="secondary" style={{ fontSize: 11 }}>
            {formatDateTime(record.createdAt, 'HH:mm')}
          </Text>
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
          {
            key: 'note',
            label: 'Adicionar nota',
            icon: <MessageOutlined />,
            onClick: () => handleAddNote(record),
          },
          { key: 'divider1', divider: true },
          ...(record.status === 'OPEN' ? [
            {
              key: 'analyze',
              label: 'Iniciar análise',
              icon: <FileSearchOutlined />,
              onClick: () => handleStartAnalysis(record),
            },
          ] : []),
          ...(['OPEN', 'ANALYZING'].includes(record.status) ? [
            {
              key: 'resolve',
              label: 'Resolver disputa',
              icon: <CheckCircleOutlined />,
              onClick: () => handleResolve(record),
            },
          ] : []),
          { key: 'divider2', divider: true },
          {
            key: 'priority-low',
            label: 'Prioridade: Baixa',
            icon: <FlagOutlined style={{ color: DISPUTE_PRIORITY_COLORS.LOW }} />,
            onClick: () => handleChangePriority(record, 'LOW'),
            disabled: record.priority === 'LOW',
          },
          {
            key: 'priority-medium',
            label: 'Prioridade: Média',
            icon: <FlagOutlined style={{ color: DISPUTE_PRIORITY_COLORS.MEDIUM }} />,
            onClick: () => handleChangePriority(record, 'MEDIUM'),
            disabled: record.priority === 'MEDIUM',
          },
          {
            key: 'priority-high',
            label: 'Prioridade: Alta',
            icon: <WarningOutlined style={{ color: DISPUTE_PRIORITY_COLORS.HIGH }} />,
            onClick: () => handleChangePriority(record, 'HIGH'),
            disabled: record.priority === 'HIGH',
          },
          {
            key: 'priority-critical',
            label: 'Prioridade: Crítica',
            icon: <FireOutlined style={{ color: DISPUTE_PRIORITY_COLORS.CRITICAL }} />,
            onClick: () => handleChangePriority(record, 'CRITICAL'),
            disabled: record.priority === 'CRITICAL',
          },
        ];

        return <ActionMenu items={items} />;
      },
    },
  ];

  const handleTableChange = (paginationConfig: TablePaginationConfig) => {
    setPagination({
      current: paginationConfig.current || 1,
      pageSize: paginationConfig.pageSize || PAGINATION_DEFAULTS.PAGE_SIZE,
      total: filteredDisputes.length,
    });
  };

  return (
    <div style={{ padding: 0 }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ marginBottom: 8 }}>Disputas</Title>
        <Text type="secondary">
          Gerencie e resolva disputas de clientes
        </Text>
      </div>

      {/* Summary Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Total de Disputas"
              value={summary.totalDisputes}
              prefix={<ExclamationCircleOutlined />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Valor em Disputa"
              value={summary.totalAmount}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
              valueStyle={{ color: '#fa8c16' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Tempo Médio Resolução"
              value={summary.avgResolutionTime}
              suffix="horas"
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card size="small">
            <Statistic
              title="Resolvidas (mês)"
              value={summary.resolvedThisMonth}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Status Summary */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', borderLeft: '3px solid #faad14' }}>
            <ExclamationCircleOutlined style={{ fontSize: 20, color: '#faad14' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong style={{ fontSize: 18 }}>{summary.byStatus.open}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Abertas</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', borderLeft: '3px solid #1890ff' }}>
            <FileSearchOutlined style={{ fontSize: 20, color: '#1890ff' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong style={{ fontSize: 18 }}>{summary.byStatus.analyzing}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Em Análise</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', borderLeft: '3px solid #52c41a' }}>
            <CheckCircleOutlined style={{ fontSize: 20, color: '#52c41a' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong style={{ fontSize: 18 }}>{summary.byStatus.resolved}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Resolvidas</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', borderLeft: '3px solid #ff4d4f' }}>
            <CloseCircleOutlined style={{ fontSize: 20, color: '#ff4d4f' }} />
            <div style={{ marginTop: 8 }}>
              <Text strong style={{ fontSize: 18 }}>{summary.byStatus.rejected}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 12 }}>Rejeitadas</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#fafafa' }}>
            <Badge color={DISPUTE_PRIORITY_COLORS.LOW} />
            <div style={{ marginTop: 4 }}>
              <Text strong>{summary.byPriority.low}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>Baixa</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#fffbe6' }}>
            <Badge color={DISPUTE_PRIORITY_COLORS.MEDIUM} />
            <div style={{ marginTop: 4 }}>
              <Text strong>{summary.byPriority.medium}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>Média</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#fff7e6' }}>
            <Badge color={DISPUTE_PRIORITY_COLORS.HIGH} />
            <div style={{ marginTop: 4 }}>
              <Text strong>{summary.byPriority.high}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>Alta</Text>
            </div>
          </Card>
        </Col>
        <Col xs={12} sm={6} lg={3}>
          <Card size="small" style={{ textAlign: 'center', background: '#fff1f0' }}>
            <Badge color={DISPUTE_PRIORITY_COLORS.CRITICAL} />
            <div style={{ marginTop: 4 }}>
              <Text strong>{summary.byPriority.critical}</Text>
              <br />
              <Text type="secondary" style={{ fontSize: 11 }}>Crítica</Text>
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
                placeholder="Buscar por ID, transação, estabelecimento..."
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
                  { label: 'Aberta', value: 'OPEN' },
                  { label: 'Em Análise', value: 'ANALYZING' },
                  { label: 'Resolvida', value: 'RESOLVED' },
                  { label: 'Rejeitada', value: 'REJECTED' },
                ]}
              />
              <Select
                placeholder="Prioridade"
                allowClear
                style={{ width: 130 }}
                value={filters.priority}
                onChange={(value) => handleFilterChange('priority', value)}
                options={[
                  { label: 'Baixa', value: 'LOW' },
                  { label: 'Média', value: 'MEDIUM' },
                  { label: 'Alta', value: 'HIGH' },
                  { label: 'Crítica', value: 'CRITICAL' },
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
                Motivo
              </Text>
              <Select
                style={{ width: '100%' }}
                placeholder="Selecione..."
                allowClear
                value={filters.reason}
                onChange={(value) => handleFilterChange('reason', value)}
                options={[
                  { label: 'Produto não entregue', value: 'PRODUCT_NOT_DELIVERED' },
                  { label: 'Cobrança duplicada', value: 'DUPLICATE_CHARGE' },
                  { label: 'Produto com defeito', value: 'DEFECTIVE_PRODUCT' },
                  { label: 'Serviço não prestado', value: 'SERVICE_NOT_PROVIDED' },
                  { label: 'Produto errado', value: 'WRONG_PRODUCT' },
                  { label: 'Entrega atrasada', value: 'DELAYED_DELIVERY' },
                  { label: 'Estorno não processado', value: 'REFUND_NOT_PROCESSED' },
                ]}
              />
            </Col>
            <Col xs={24} sm={12} lg={6}>
              <Text type="secondary" style={{ fontSize: 12, display: 'block', marginBottom: 4 }}>
                Valor mínimo
              </Text>
              <InputNumber
                style={{ width: '100%' }}
                placeholder="R$ 0,00"
                min={0}
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
          dataSource={filteredDisputes}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `${range[0]}-${range[1]} de ${total} disputas`,
            pageSizeOptions: PAGINATION_DEFAULTS.PAGE_SIZE_OPTIONS,
          }}
          onChange={handleTableChange}
          scroll={{ x: 1400 }}
          size="small"
        />
      </Card>

      {/* Details Drawer */}
      <Drawer
        title="Detalhes da Disputa"
        placement="right"
        width={550}
        onClose={() => setDetailsVisible(false)}
        open={detailsVisible}
      >
        {selectedDispute && (
          <div>
            {/* Status Alert */}
            {selectedDispute.status === 'OPEN' && (
              <Alert
                message="Disputa Aberta"
                description="Esta disputa aguarda início da análise."
                type="warning"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}
            {selectedDispute.status === 'ANALYZING' && (
              <Alert
                message="Em Análise"
                description={`Responsável: ${selectedDispute.assignedTo || 'Não atribuído'}`}
                type="info"
                showIcon
                style={{ marginBottom: 16 }}
              />
            )}

            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="ID">
                <Text copyable style={{ fontFamily: 'monospace' }}>
                  {selectedDispute.id}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="Transação">
                <Text copyable style={{ fontFamily: 'monospace' }}>
                  {selectedDispute.transactionId}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag
                  color={DISPUTE_STATUS_COLORS[selectedDispute.status]}
                  icon={getStatusIcon(selectedDispute.status)}
                >
                  {DISPUTE_STATUS_LABELS[selectedDispute.status]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Prioridade">
                {selectedDispute.priority && (
                  <Tag
                    color={DISPUTE_PRIORITY_COLORS[selectedDispute.priority]}
                    icon={getPriorityIcon(selectedDispute.priority)}
                  >
                    {DISPUTE_PRIORITY_LABELS[selectedDispute.priority]}
                  </Tag>
                )}
              </Descriptions.Item>
              <Descriptions.Item label="Valor">
                <Text strong style={{ color: '#ff4d4f' }}>
                  {formatCurrency(selectedDispute.amount)}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="Motivo">
                <Text>{DISPUTE_REASON_LABELS[selectedDispute.reason]}</Text>
              </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Descrição
            </Title>
            <Paragraph style={{ background: '#fafafa', padding: 12, borderRadius: 4 }}>
              {selectedDispute.description || 'Sem descrição'}
            </Paragraph>

            {selectedDispute.resolution && (
              <>
                <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
                  Resolução
                </Title>
                <Paragraph style={{ background: '#f6ffed', padding: 12, borderRadius: 4, border: '1px solid #b7eb8f' }}>
                  {selectedDispute.resolution}
                </Paragraph>
              </>
            )}

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Estabelecimento
            </Title>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Nome">
                {selectedDispute.establishment.name}
              </Descriptions.Item>
              <Descriptions.Item label="CNPJ">
                {formatCNPJ(selectedDispute.establishment.cnpj)}
              </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Cliente
            </Title>
            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="Nome">
                {selectedDispute.customer.name}
              </Descriptions.Item>
              <Descriptions.Item label="E-mail">
                {selectedDispute.customer.email}
              </Descriptions.Item>
              {selectedDispute.customer.document && (
                <Descriptions.Item label="Documento">
                  {selectedDispute.customer.document}
                </Descriptions.Item>
              )}
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
              Timeline
            </Title>
            <Timeline
              items={[
                {
                  color: 'blue',
                  children: (
                    <>
                      <Text strong>Disputa aberta</Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {formatDateTime(selectedDispute.createdAt)}
                      </Text>
                    </>
                  ),
                },
                ...(selectedDispute.updatedAt ? [{
                  color: 'blue',
                  children: (
                    <>
                      <Text strong>Análise iniciada</Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {formatDateTime(selectedDispute.updatedAt)}
                      </Text>
                    </>
                  ),
                }] : []),
                ...(selectedDispute.resolvedAt ? [{
                  color: selectedDispute.status === 'RESOLVED' ? 'green' : 'red',
                  children: (
                    <>
                      <Text strong>
                        {selectedDispute.status === 'RESOLVED' ? 'Disputa resolvida' : 'Disputa rejeitada'}
                      </Text>
                      <br />
                      <Text type="secondary" style={{ fontSize: 12 }}>
                        {formatDateTime(selectedDispute.resolvedAt)}
                      </Text>
                    </>
                  ),
                }] : []),
              ]}
            />

            <Divider />

            <div style={{ marginTop: 16 }}>
              <Space wrap>
                {selectedDispute.status === 'OPEN' && (
                  <Button
                    type="primary"
                    icon={<FileSearchOutlined />}
                    onClick={() => {
                      setDetailsVisible(false);
                      handleStartAnalysis(selectedDispute);
                    }}
                  >
                    Iniciar Análise
                  </Button>
                )}
                {['OPEN', 'ANALYZING'].includes(selectedDispute.status) && (
                  <Button
                    icon={<CheckCircleOutlined />}
                    onClick={() => {
                      setDetailsVisible(false);
                      handleResolve(selectedDispute);
                    }}
                  >
                    Resolver
                  </Button>
                )}
                <Button
                  icon={<MessageOutlined />}
                  onClick={() => {
                    setDetailsVisible(false);
                    handleAddNote(selectedDispute);
                  }}
                >
                  Adicionar Nota
                </Button>
              </Space>
            </div>
          </div>
        )}
      </Drawer>

      {/* Analyze Modal */}
      <Modal
        title="Iniciar Análise"
        open={analyzeModalVisible}
        onCancel={() => {
          setAnalyzeModalVisible(false);
          analyzeForm.resetFields();
        }}
        onOk={handleAnalyzeSubmit}
        okText="Iniciar"
      >
        <Form form={analyzeForm} layout="vertical">
          <Form.Item label="Disputa">
            <Text strong>{selectedDispute?.id}</Text>
            <br />
            <Text type="secondary">
              {selectedDispute && formatCurrency(selectedDispute.amount)}
            </Text>
          </Form.Item>
          <Form.Item
            name="assignedTo"
            label="Atribuir para"
            rules={[{ required: true, message: 'Selecione o responsável' }]}
          >
            <Select
              placeholder="Selecione o analista..."
              options={[
                { label: 'Carlos Analista', value: 'Carlos Analista' },
                { label: 'Ana Analista', value: 'Ana Analista' },
                { label: 'Pedro Analista', value: 'Pedro Analista' },
              ]}
            />
          </Form.Item>
          <Form.Item name="notes" label="Observações iniciais">
            <TextArea rows={3} placeholder="Observações para a análise..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Resolve Modal */}
      <Modal
        title="Resolver Disputa"
        open={resolveModalVisible}
        onCancel={() => {
          setResolveModalVisible(false);
          resolveForm.resetFields();
        }}
        onOk={handleResolveSubmit}
        okText="Confirmar Resolução"
        width={600}
      >
        <Form form={resolveForm} layout="vertical">
          <Form.Item label="Disputa">
            <Text strong>{selectedDispute?.id}</Text>
            <br />
            <Text type="secondary">
              Cliente: {selectedDispute?.customer.name}
            </Text>
            <br />
            <Text type="secondary">
              Valor: {selectedDispute && formatCurrency(selectedDispute.amount)}
            </Text>
          </Form.Item>
          <Form.Item
            name="resolution"
            label="Decisão"
            rules={[{ required: true, message: 'Selecione a decisão' }]}
          >
            <Select
              placeholder="Selecione..."
              options={[
                { label: 'Aprovar - Estorno total ao cliente', value: 'APPROVED' },
                { label: 'Aprovar parcial - Estorno parcial', value: 'PARTIAL' },
                { label: 'Rejeitar - Disputa improcedente', value: 'REJECTED' },
              ]}
            />
          </Form.Item>
          <Form.Item
            noStyle
            shouldUpdate={(prevValues, currentValues) => prevValues.resolution !== currentValues.resolution}
          >
            {({ getFieldValue }) =>
              getFieldValue('resolution') === 'PARTIAL' && (
                <Form.Item
                  name="refundAmount"
                  label="Valor do estorno parcial"
                  rules={[{ required: true, message: 'Informe o valor' }]}
                >
                  <InputNumber
                    style={{ width: '100%' }}
                    min={0.01}
                    max={selectedDispute?.amount}
                    precision={2}
                    formatter={(value) => `R$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
                    parser={(value) => value?.replace(/R\$\s?|(\.)/g, '').replace(',', '.') as any}
                  />
                </Form.Item>
              )
            }
          </Form.Item>
          <Form.Item
            name="notes"
            label="Justificativa da decisão"
            rules={[{ required: true, message: 'Informe a justificativa' }]}
          >
            <TextArea rows={4} placeholder="Descreva a justificativa da decisão..." />
          </Form.Item>
        </Form>
      </Modal>

      {/* Note Modal */}
      <Modal
        title="Adicionar Nota"
        open={noteModalVisible}
        onCancel={() => {
          setNoteModalVisible(false);
          noteForm.resetFields();
        }}
        onOk={handleNoteSubmit}
        okText="Adicionar"
      >
        <Form form={noteForm} layout="vertical">
          <Form.Item label="Disputa">
            <Text strong>{selectedDispute?.id}</Text>
          </Form.Item>
          <Form.Item
            name="note"
            label="Nota"
            rules={[{ required: true, message: 'Digite a nota' }]}
          >
            <TextArea rows={4} placeholder="Digite sua nota..." />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default DisputesPage;
