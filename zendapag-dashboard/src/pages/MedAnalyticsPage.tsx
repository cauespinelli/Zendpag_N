// @ts-nocheck
import React, { useState, useMemo } from 'react';
import {
  Card,
  Row,
  Col,
  Table,
  Tag,
  Space,
  Button,
  Input,
  Select,
  DatePicker,
  Typography,
  Statistic,
  Drawer,
  Descriptions,
  Divider,
  Form,
  Modal,
  message,
  Tooltip,
  Progress,
  Tabs,
  Badge,
} from 'antd';
import {
  SearchOutlined,
  FilterOutlined,
  ReloadOutlined,
  ExportOutlined,
  DownloadOutlined,
  EyeOutlined,
  CloseCircleOutlined,
  RetweetOutlined,
  DollarOutlined,
  ClockCircleOutlined,
  RiseOutlined,
  FallOutlined,
  ExclamationCircleOutlined,
  CheckCircleOutlined,
  SyncOutlined,
  WarningOutlined,
  BarChartOutlined,
  LineChartOutlined,
  PieChartOutlined,
  BankOutlined,
  FileTextOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import ActionMenu from '@/components/ActionMenu';
import CopyButton from '@/components/CopyButton';
import type {
  MedRecord,
  MedDashboardSummary,
  MedRecordType,
  MedRecordStatus,
} from '@/types';
import { DISPUTE_REASON_LABELS } from '@/types';

const { Title, Text, Paragraph } = Typography;
const { RangePicker } = DatePicker;
const { TextArea } = Input;

// Status colors and labels
const MED_STATUS_COLORS: Record<MedRecordStatus, string> = {
  PENDING: '#faad14',
  EXECUTED: '#52c41a',
  CANCELLED: '#8c8c8c',
  EXPIRED: '#ff4d4f',
};

const MED_STATUS_LABELS: Record<MedRecordStatus, string> = {
  PENDING: 'Pendente',
  EXECUTED: 'Executado',
  CANCELLED: 'Cancelado',
  EXPIRED: 'Expirado',
};

const MED_TYPE_COLORS: Record<MedRecordType, string> = {
  MED: '#1890ff',
  DISPUTE: '#ff4d4f',
};

const MED_TYPE_LABELS: Record<MedRecordType, string> = {
  MED: 'MED',
  DISPUTE: 'Disputa',
};

// Mock summary data
const mockSummary: MedDashboardSummary = {
  totalMeds: 1245,
  totalDisputes: 89,
  totalMedsValue: 2456780.50,
  totalDisputesValue: 125890.30,
  pendingMeds: 156,
  pendingDisputes: 23,
  executedToday: 45,
  avgExecutionTime: 4.2,
  topEstablishments: [
    { id: '1', name: 'Loja Virtual ABC', medsCount: 156, disputesCount: 12, totalValue: 345678.90 },
    { id: '2', name: 'E-commerce XYZ', medsCount: 134, disputesCount: 8, totalValue: 298765.40 },
    { id: '3', name: 'Marketplace Plus', medsCount: 98, disputesCount: 15, totalValue: 256890.20 },
    { id: '4', name: 'Digital Store', medsCount: 87, disputesCount: 6, totalValue: 198765.30 },
    { id: '5', name: 'Tech Solutions', medsCount: 76, disputesCount: 9, totalValue: 167890.50 },
  ],
  dailyTrend: [
    { date: '2024-01-20', meds: 42, disputes: 3, medsValue: 89560.30, disputesValue: 4560.20 },
    { date: '2024-01-21', meds: 38, disputes: 5, medsValue: 76890.40, disputesValue: 6780.30 },
    { date: '2024-01-22', meds: 51, disputes: 2, medsValue: 98760.50, disputesValue: 3450.10 },
    { date: '2024-01-23', meds: 45, disputes: 4, medsValue: 87650.20, disputesValue: 5670.40 },
    { date: '2024-01-24', meds: 55, disputes: 6, medsValue: 102340.60, disputesValue: 8900.50 },
    { date: '2024-01-25', meds: 48, disputes: 3, medsValue: 91230.30, disputesValue: 4560.20 },
    { date: '2024-01-26', meds: 45, disputes: 4, medsValue: 85670.40, disputesValue: 5890.30 },
  ],
  byReason: [
    { reason: 'PRODUCT_NOT_DELIVERED', count: 25, value: 45678.90, percentage: 28.1 },
    { reason: 'DUPLICATE_CHARGE', count: 18, value: 32456.30, percentage: 20.2 },
    { reason: 'DEFECTIVE_PRODUCT', count: 15, value: 27890.40, percentage: 16.9 },
    { reason: 'REFUND_NOT_PROCESSED', count: 12, value: 19870.50, percentage: 13.5 },
    { reason: 'CONTRACT_NOT_FULFILLED', count: 10, value: 18765.20, percentage: 11.2 },
    { reason: 'OTHER', count: 9, value: 12890.30, percentage: 10.1 },
  ],
};

// Mock records data
const mockRecords: MedRecord[] = [
  {
    id: 'MED001',
    type: 'MED',
    status: 'EXECUTED',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90', email: 'contato@lojaabc.com' },
    transactionId: 'TXN-2024-001',
    endToEnd: 'E0000000020240126153045789012345',
    reason: 'DUPLICATE_CHARGE',
    quantity: 1,
    unitValue: 1250.00,
    totalValue: 1250.00,
    fee: 2.50,
    netValue: 1247.50,
    scheduledDate: '2024-01-26',
    executionDate: '2024-01-26T15:30:45',
    createdAt: '2024-01-26T10:15:00',
    updatedAt: '2024-01-26T15:30:45',
  },
  {
    id: 'MED002',
    type: 'DISPUTE',
    status: 'PENDING',
    establishment: { id: '2', name: 'E-commerce XYZ', cnpj: '98.765.432/0001-10', email: 'financeiro@ecommxyz.com' },
    transactionId: 'TXN-2024-002',
    endToEnd: 'E0000000020240126142030456789012',
    reason: 'PRODUCT_NOT_DELIVERED',
    quantity: 1,
    unitValue: 3456.78,
    totalValue: 3456.78,
    fee: 6.91,
    netValue: 3449.87,
    scheduledDate: '2024-01-27',
    createdAt: '2024-01-26T12:45:00',
  },
  {
    id: 'MED003',
    type: 'MED',
    status: 'PENDING',
    establishment: { id: '3', name: 'Marketplace Plus', cnpj: '45.678.901/0001-23', email: 'admin@marketplace.com' },
    transactionId: 'TXN-2024-003',
    reason: 'REFUND_NOT_PROCESSED',
    quantity: 2,
    unitValue: 589.90,
    totalValue: 1179.80,
    fee: 2.36,
    netValue: 1177.44,
    scheduledDate: '2024-01-28',
    createdAt: '2024-01-26T09:30:00',
  },
  {
    id: 'MED004',
    type: 'DISPUTE',
    status: 'EXECUTED',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90', email: 'contato@lojaabc.com' },
    transactionId: 'TXN-2024-004',
    endToEnd: 'E0000000020240125183020789456123',
    reason: 'DEFECTIVE_PRODUCT',
    quantity: 1,
    unitValue: 2890.00,
    totalValue: 2890.00,
    fee: 5.78,
    netValue: 2884.22,
    scheduledDate: '2024-01-25',
    executionDate: '2024-01-25T18:30:20',
    createdAt: '2024-01-25T14:20:00',
    updatedAt: '2024-01-25T18:30:20',
  },
  {
    id: 'MED005',
    type: 'MED',
    status: 'CANCELLED',
    establishment: { id: '4', name: 'Digital Store', cnpj: '56.789.012/0001-34', email: 'suporte@digitalstore.com' },
    transactionId: 'TXN-2024-005',
    reason: 'CONTRACT_NOT_FULFILLED',
    quantity: 1,
    unitValue: 4567.89,
    totalValue: 4567.89,
    fee: 9.14,
    netValue: 4558.75,
    scheduledDate: '2024-01-24',
    createdAt: '2024-01-24T08:00:00',
    updatedAt: '2024-01-24T16:45:00',
  },
  {
    id: 'MED006',
    type: 'MED',
    status: 'EXPIRED',
    establishment: { id: '5', name: 'Tech Solutions', cnpj: '67.890.123/0001-45', email: 'fin@techsolutions.com' },
    transactionId: 'TXN-2024-006',
    reason: 'DELAYED_DELIVERY',
    quantity: 3,
    unitValue: 199.90,
    totalValue: 599.70,
    fee: 1.20,
    netValue: 598.50,
    scheduledDate: '2024-01-20',
    createdAt: '2024-01-18T11:30:00',
    updatedAt: '2024-01-20T23:59:59',
  },
  {
    id: 'MED007',
    type: 'DISPUTE',
    status: 'PENDING',
    establishment: { id: '2', name: 'E-commerce XYZ', cnpj: '98.765.432/0001-10', email: 'financeiro@ecommxyz.com' },
    transactionId: 'TXN-2024-007',
    reason: 'SERVICE_NOT_PROVIDED',
    quantity: 1,
    unitValue: 1890.00,
    totalValue: 1890.00,
    fee: 3.78,
    netValue: 1886.22,
    scheduledDate: '2024-01-29',
    createdAt: '2024-01-26T14:00:00',
  },
];

const MedAnalyticsPage: React.FC = () => {
  const [records] = useState<MedRecord[]>(mockRecords);
  const [summary] = useState<MedDashboardSummary>(mockSummary);
  const [loading, setLoading] = useState(false);
  const [selectedRecord, setSelectedRecord] = useState<MedRecord | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [cancelModalOpen, setCancelModalOpen] = useState(false);
  const [cancelForm] = Form.useForm();
  const [activeTab, setActiveTab] = useState('overview');

  // Filters
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<MedRecordStatus | undefined>();
  const [typeFilter, setTypeFilter] = useState<MedRecordType | undefined>();
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [establishmentFilter, setEstablishmentFilter] = useState<string | undefined>();
  const [minValue, setMinValue] = useState<number | undefined>();
  const [maxValue, setMaxValue] = useState<number | undefined>();

  // Filtered records
  const filteredRecords = useMemo(() => {
    return records.filter((record) => {
      if (searchText) {
        const search = searchText.toLowerCase();
        const matchId = record.id.toLowerCase().includes(search);
        const matchEstablishment = record.establishment.name.toLowerCase().includes(search);
        const matchCnpj = record.establishment.cnpj.includes(search);
        const matchTransaction = record.transactionId?.toLowerCase().includes(search);
        const matchEndToEnd = record.endToEnd?.toLowerCase().includes(search);
        if (!matchId && !matchEstablishment && !matchCnpj && !matchTransaction && !matchEndToEnd) {
          return false;
        }
      }
      if (statusFilter && record.status !== statusFilter) return false;
      if (typeFilter && record.type !== typeFilter) return false;
      if (establishmentFilter && record.establishment.id !== establishmentFilter) return false;
      if (minValue && record.totalValue < minValue) return false;
      if (maxValue && record.totalValue > maxValue) return false;
      if (dateRange) {
        const recordDate = dayjs(record.createdAt);
        if (recordDate.isBefore(dateRange[0], 'day') || recordDate.isAfter(dateRange[1], 'day')) {
          return false;
        }
      }
      return true;
    });
  }, [records, searchText, statusFilter, typeFilter, establishmentFilter, minValue, maxValue, dateRange]);

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat('pt-BR', {
      style: 'currency',
      currency: 'BRL',
    }).format(value);
  };

  const formatDate = (date: string) => {
    return dayjs(date).format('DD/MM/YYYY HH:mm');
  };

  const handleRefresh = () => {
    setLoading(true);
    setTimeout(() => {
      setLoading(false);
      message.success('Dados atualizados');
    }, 1000);
  };

  const handleClearFilters = () => {
    setSearchText('');
    setStatusFilter(undefined);
    setTypeFilter(undefined);
    setEstablishmentFilter(undefined);
    setDateRange(null);
    setMinValue(undefined);
    setMaxValue(undefined);
  };

  const handleViewRecord = (record: MedRecord) => {
    setSelectedRecord(record);
    setDrawerOpen(true);
  };

  const handleCancelMed = (record: MedRecord) => {
    setSelectedRecord(record);
    setCancelModalOpen(true);
  };

  const handleRetryMed = (record: MedRecord) => {
    Modal.confirm({
      title: 'Reprocessar MED/Disputa',
      icon: <ExclamationCircleOutlined />,
      content: `Deseja reprocessar o registro ${record.id}?`,
      okText: 'Sim, reprocessar',
      cancelText: 'Cancelar',
      onOk: () => {
        message.success('MED/Disputa enviado para reprocessamento');
      },
    });
  };

  const handleExport = () => {
    message.success('Exportação iniciada. O arquivo será baixado em instantes.');
  };

  const onCancelSubmit = async (values: { reason: string }) => {
    message.success('MED/Disputa cancelado com sucesso');
    setCancelModalOpen(false);
    cancelForm.resetFields();
    setSelectedRecord(null);
  };

  const getActionMenuItems = (record: MedRecord) => {
    const items = [
      {
        key: 'view',
        icon: <EyeOutlined />,
        label: 'Ver detalhes',
        onClick: () => handleViewRecord(record),
      },
    ];

    if (record.status === 'PENDING') {
      items.push({
        key: 'cancel',
        icon: <CloseCircleOutlined />,
        label: 'Cancelar',
        onClick: () => handleCancelMed(record),
        danger: true,
      });
    }

    if (record.status === 'EXPIRED' || record.status === 'CANCELLED') {
      items.push({
        key: 'retry',
        icon: <RetweetOutlined />,
        label: 'Reprocessar',
        onClick: () => handleRetryMed(record),
      });
    }

    return items;
  };

  const columns: ColumnsType<MedRecord> = [
    {
      title: 'ID',
      dataIndex: 'id',
      key: 'id',
      width: 120,
      render: (id: string) => (
        <Space>
          <Text code style={{ fontSize: 12 }}>{id}</Text>
          <CopyButton text={id} />
        </Space>
      ),
    },
    {
      title: 'Tipo',
      dataIndex: 'type',
      key: 'type',
      width: 100,
      render: (type: MedRecordType) => (
        <Tag color={MED_TYPE_COLORS[type]} style={{ fontWeight: 600 }}>
          {MED_TYPE_LABELS[type]}
        </Tag>
      ),
      filters: [
        { text: 'MED', value: 'MED' },
        { text: 'Disputa', value: 'DISPUTE' },
      ],
      onFilter: (value, record) => record.type === value,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: MedRecordStatus) => (
        <Tag color={MED_STATUS_COLORS[status]}>
          {MED_STATUS_LABELS[status]}
        </Tag>
      ),
      filters: [
        { text: 'Pendente', value: 'PENDING' },
        { text: 'Executado', value: 'EXECUTED' },
        { text: 'Cancelado', value: 'CANCELLED' },
        { text: 'Expirado', value: 'EXPIRED' },
      ],
      onFilter: (value, record) => record.status === value,
    },
    {
      title: 'Estabelecimento',
      dataIndex: ['establishment', 'name'],
      key: 'establishment',
      width: 200,
      ellipsis: true,
      render: (_: any, record: MedRecord) => (
        <Tooltip title={record.establishment.cnpj}>
          <Text>{record.establishment.name}</Text>
        </Tooltip>
      ),
    },
    {
      title: 'Motivo',
      dataIndex: 'reason',
      key: 'reason',
      width: 200,
      ellipsis: true,
      render: (reason: string) => (
        <Tooltip title={DISPUTE_REASON_LABELS[reason as keyof typeof DISPUTE_REASON_LABELS] || reason}>
          <Text ellipsis style={{ maxWidth: 180 }}>
            {DISPUTE_REASON_LABELS[reason as keyof typeof DISPUTE_REASON_LABELS] || reason || '-'}
          </Text>
        </Tooltip>
      ),
    },
    {
      title: 'Valor',
      dataIndex: 'totalValue',
      key: 'totalValue',
      width: 140,
      align: 'right',
      render: (value: number) => (
        <Text strong style={{ color: '#1890ff' }}>
          {formatCurrency(value)}
        </Text>
      ),
      sorter: (a, b) => a.totalValue - b.totalValue,
    },
    {
      title: 'Agendado',
      dataIndex: 'scheduledDate',
      key: 'scheduledDate',
      width: 120,
      render: (date: string) => date ? dayjs(date).format('DD/MM/YYYY') : '-',
      sorter: (a, b) => dayjs(a.scheduledDate).unix() - dayjs(b.scheduledDate).unix(),
    },
    {
      title: 'Criado em',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (date: string) => formatDate(date),
      sorter: (a, b) => dayjs(a.createdAt).unix() - dayjs(b.createdAt).unix(),
      defaultSortOrder: 'descend',
    },
    {
      title: 'Ações',
      key: 'actions',
      width: 80,
      fixed: 'right',
      render: (_: any, record: MedRecord) => (
        <ActionMenu items={getActionMenuItems(record)} />
      ),
    },
  ];

  // Establishment options for filter
  const establishmentOptions = useMemo(() => {
    const unique = new Map();
    records.forEach((r) => {
      if (!unique.has(r.establishment.id)) {
        unique.set(r.establishment.id, r.establishment);
      }
    });
    return Array.from(unique.values()).map((e) => ({
      label: e.name,
      value: e.id,
    }));
  }, [records]);

  // Calculate status counts for cards
  const statusCounts = useMemo(() => {
    const counts = { PENDING: 0, EXECUTED: 0, CANCELLED: 0, EXPIRED: 0 };
    records.forEach((r) => {
      counts[r.status]++;
    });
    return counts;
  }, [records]);

  // Calculate type counts
  const typeCounts = useMemo(() => {
    const counts = { MED: 0, DISPUTE: 0 };
    records.forEach((r) => {
      counts[r.type]++;
    });
    return counts;
  }, [records]);

  // Render overview tab
  const renderOverviewTab = () => (
    <>
      {/* Summary Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Total MEDs"
              value={summary.totalMeds}
              prefix={<FileTextOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                Valor total: {formatCurrency(summary.totalMedsValue)}
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Total Disputas"
              value={summary.totalDisputes}
              prefix={<ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                Valor total: {formatCurrency(summary.totalDisputesValue)}
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Pendentes"
              value={summary.pendingMeds + summary.pendingDisputes}
              prefix={<ClockCircleOutlined style={{ color: '#faad14' }} />}
              valueStyle={{ color: '#faad14' }}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                {summary.pendingMeds} MEDs | {summary.pendingDisputes} Disputas
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Executados Hoje"
              value={summary.executedToday}
              prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                Tempo médio: {summary.avgExecutionTime}h
              </Text>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Status and Type Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={16}>
          <Card title="Distribuição por Status" bordered={false} size="small">
            <Row gutter={[8, 8]}>
              <Col span={6}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#fffbe6' }}>
                  <Statistic
                    title="Pendentes"
                    value={statusCounts.PENDING}
                    valueStyle={{ fontSize: 20, color: '#faad14' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#f6ffed' }}>
                  <Statistic
                    title="Executados"
                    value={statusCounts.EXECUTED}
                    valueStyle={{ fontSize: 20, color: '#52c41a' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#f5f5f5' }}>
                  <Statistic
                    title="Cancelados"
                    value={statusCounts.CANCELLED}
                    valueStyle={{ fontSize: 20, color: '#8c8c8c' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#fff2f0' }}>
                  <Statistic
                    title="Expirados"
                    value={statusCounts.EXPIRED}
                    valueStyle={{ fontSize: 20, color: '#ff4d4f' }}
                  />
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Distribuição por Tipo" bordered={false} size="small">
            <Row gutter={[8, 8]}>
              <Col span={12}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#e6f7ff' }}>
                  <Statistic
                    title="MEDs"
                    value={typeCounts.MED}
                    valueStyle={{ fontSize: 20, color: '#1890ff' }}
                  />
                </Card>
              </Col>
              <Col span={12}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#fff2f0' }}>
                  <Statistic
                    title="Disputas"
                    value={typeCounts.DISPUTE}
                    valueStyle={{ fontSize: 20, color: '#ff4d4f' }}
                  />
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* Trend Chart Placeholder and Top Establishments */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={14}>
          <Card
            title={
              <Space>
                <LineChartOutlined />
                <span>Tendência Diária (Últimos 7 dias)</span>
              </Space>
            }
            bordered={false}
          >
            <div style={{ padding: '20px 0' }}>
              {summary.dailyTrend.map((day, index) => (
                <div key={day.date} style={{ marginBottom: 16 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                    <Text>{dayjs(day.date).format('DD/MM')}</Text>
                    <Text type="secondary">
                      {day.meds} MEDs | {day.disputes} Disputas
                    </Text>
                  </div>
                  <Progress
                    percent={Math.round((day.meds / 60) * 100)}
                    strokeColor="#1890ff"
                    trailColor="#fff2f0"
                    showInfo={false}
                    size="small"
                  />
                  <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
                    <Text type="secondary" style={{ fontSize: 12 }}>
                      {formatCurrency(day.medsValue + day.disputesValue)}
                    </Text>
                  </div>
                </div>
              ))}
            </div>
          </Card>
        </Col>
        <Col xs={24} lg={10}>
          <Card
            title={
              <Space>
                <BankOutlined />
                <span>Top 5 Estabelecimentos</span>
              </Space>
            }
            bordered={false}
          >
            <div>
              {summary.topEstablishments.map((est, index) => (
                <div
                  key={est.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '12px 0',
                    borderBottom: index < 4 ? '1px solid #f0f0f0' : 'none',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
                    <Badge
                      count={index + 1}
                      style={{
                        backgroundColor: index === 0 ? '#faad14' : index === 1 ? '#8c8c8c' : index === 2 ? '#cd7f32' : '#d9d9d9',
                      }}
                    />
                    <div>
                      <Text strong ellipsis style={{ maxWidth: 150 }}>{est.name}</Text>
                      <div>
                        <Text type="secondary" style={{ fontSize: 12 }}>
                          {est.medsCount} MEDs | {est.disputesCount} Disputas
                        </Text>
                      </div>
                    </div>
                  </div>
                  <Text strong style={{ color: '#1890ff' }}>
                    {formatCurrency(est.totalValue)}
                  </Text>
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>

      {/* By Reason */}
      <Row gutter={[16, 16]}>
        <Col span={24}>
          <Card
            title={
              <Space>
                <PieChartOutlined />
                <span>Distribuição por Motivo</span>
              </Space>
            }
            bordered={false}
          >
            <Row gutter={[16, 16]}>
              {summary.byReason.map((item) => (
                <Col xs={24} sm={12} md={8} lg={4} key={item.reason}>
                  <Card bordered size="small" style={{ textAlign: 'center' }}>
                    <Progress
                      type="circle"
                      percent={item.percentage}
                      size={60}
                      strokeColor="#1890ff"
                      format={() => item.count}
                    />
                    <div style={{ marginTop: 8 }}>
                      <Text ellipsis style={{ fontSize: 12 }}>
                        {DISPUTE_REASON_LABELS[item.reason as keyof typeof DISPUTE_REASON_LABELS] || item.reason}
                      </Text>
                    </div>
                    <div>
                      <Text type="secondary" style={{ fontSize: 11 }}>
                        {formatCurrency(item.value)}
                      </Text>
                    </div>
                  </Card>
                </Col>
              ))}
            </Row>
          </Card>
        </Col>
      </Row>
    </>
  );

  // Render history tab
  const renderHistoryTab = () => (
    <>
      {/* Filters */}
      <Card bordered={false} style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space wrap size="middle">
              <Input
                placeholder="Buscar por ID, estabelecimento, CNPJ, transação..."
                prefix={<SearchOutlined />}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                style={{ width: 350 }}
                allowClear
              />
              <Select
                placeholder="Tipo"
                value={typeFilter}
                onChange={setTypeFilter}
                style={{ width: 120 }}
                allowClear
                options={[
                  { label: 'MED', value: 'MED' },
                  { label: 'Disputa', value: 'DISPUTE' },
                ]}
              />
              <Select
                placeholder="Status"
                value={statusFilter}
                onChange={setStatusFilter}
                style={{ width: 140 }}
                allowClear
                options={[
                  { label: 'Pendente', value: 'PENDING' },
                  { label: 'Executado', value: 'EXECUTED' },
                  { label: 'Cancelado', value: 'CANCELLED' },
                  { label: 'Expirado', value: 'EXPIRED' },
                ]}
              />
              <RangePicker
                value={dateRange}
                onChange={(dates) => setDateRange(dates as [dayjs.Dayjs, dayjs.Dayjs] | null)}
                format="DD/MM/YYYY"
                placeholder={['Data inicial', 'Data final']}
              />
              <Button
                icon={<FilterOutlined />}
                onClick={() => setShowAdvancedFilters(!showAdvancedFilters)}
                type={showAdvancedFilters ? 'primary' : 'default'}
              >
                Filtros
              </Button>
              {(searchText || statusFilter || typeFilter || dateRange || establishmentFilter || minValue || maxValue) && (
                <Button onClick={handleClearFilters}>Limpar filtros</Button>
              )}
            </Space>
          </Col>
          <Col>
            <Space>
              <Tooltip title="Atualizar">
                <Button icon={<ReloadOutlined />} onClick={handleRefresh} loading={loading} />
              </Tooltip>
              <Tooltip title="Exportar">
                <Button icon={<ExportOutlined />} onClick={handleExport}>
                  Exportar
                </Button>
              </Tooltip>
            </Space>
          </Col>
        </Row>

        {/* Advanced Filters */}
        {showAdvancedFilters && (
          <>
            <Divider style={{ margin: '16px 0' }} />
            <Row gutter={[16, 16]}>
              <Col xs={24} sm={12} md={8}>
                <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                  Estabelecimento
                </Text>
                <Select
                  placeholder="Selecione o estabelecimento"
                  value={establishmentFilter}
                  onChange={setEstablishmentFilter}
                  style={{ width: '100%' }}
                  allowClear
                  showSearch
                  optionFilterProp="label"
                  options={establishmentOptions}
                />
              </Col>
              <Col xs={12} sm={6} md={4}>
                <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                  Valor mínimo
                </Text>
                <Input
                  type="number"
                  placeholder="R$ 0,00"
                  value={minValue}
                  onChange={(e) => setMinValue(e.target.value ? Number(e.target.value) : undefined)}
                  prefix="R$"
                />
              </Col>
              <Col xs={12} sm={6} md={4}>
                <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                  Valor máximo
                </Text>
                <Input
                  type="number"
                  placeholder="R$ 999.999"
                  value={maxValue}
                  onChange={(e) => setMaxValue(e.target.value ? Number(e.target.value) : undefined)}
                  prefix="R$"
                />
              </Col>
            </Row>
          </>
        )}
      </Card>

      {/* Results Info */}
      <div style={{ marginBottom: 16 }}>
        <Text type="secondary">
          Exibindo {filteredRecords.length} de {records.length} registros
        </Text>
      </div>

      {/* Table */}
      <Card bordered={false}>
        <Table
          columns={columns}
          dataSource={filteredRecords}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1400 }}
          pagination={{
            total: filteredRecords.length,
            pageSize: 20,
            showSizeChanger: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} de ${total} registros`,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
        />
      </Card>
    </>
  );

  return (
    <div style={{ padding: '0 0 24px 0' }}>
      {/* Header */}
      <div style={{ marginBottom: 24 }}>
        <Title level={4} style={{ margin: 0 }}>
          <BarChartOutlined style={{ marginRight: 8 }} />
          Med Analytics
        </Title>
        <Text type="secondary">
          Dashboard analítico de MEDs e Disputas
        </Text>
      </div>

      {/* Tabs */}
      <Tabs
        activeKey={activeTab}
        onChange={setActiveTab}
        items={[
          {
            key: 'overview',
            label: (
              <span>
                <BarChartOutlined />
                Visão Geral
              </span>
            ),
            children: renderOverviewTab(),
          },
          {
            key: 'history',
            label: (
              <span>
                <FileTextOutlined />
                Histórico
              </span>
            ),
            children: renderHistoryTab(),
          },
        ]}
      />

      {/* Detail Drawer */}
      <Drawer
        title={
          <Space>
            <FileTextOutlined />
            <span>Detalhes do Registro</span>
            {selectedRecord && (
              <Tag color={MED_TYPE_COLORS[selectedRecord.type]}>
                {MED_TYPE_LABELS[selectedRecord.type]}
              </Tag>
            )}
          </Space>
        }
        placement="right"
        width={600}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedRecord(null);
        }}
        extra={
          selectedRecord && (
            <Tag color={MED_STATUS_COLORS[selectedRecord.status]}>
              {MED_STATUS_LABELS[selectedRecord.status]}
            </Tag>
          )
        }
      >
        {selectedRecord && (
          <div>
            <Descriptions title="Informações Gerais" column={1} bordered size="small">
              <Descriptions.Item label="ID">
                <Space>
                  <Text code>{selectedRecord.id}</Text>
                  <CopyButton text={selectedRecord.id} />
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Tipo">
                <Tag color={MED_TYPE_COLORS[selectedRecord.type]}>
                  {MED_TYPE_LABELS[selectedRecord.type]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={MED_STATUS_COLORS[selectedRecord.status]}>
                  {MED_STATUS_LABELS[selectedRecord.status]}
                </Tag>
              </Descriptions.Item>
              {selectedRecord.transactionId && (
                <Descriptions.Item label="ID Transação">
                  <Space>
                    <Text code>{selectedRecord.transactionId}</Text>
                    <CopyButton text={selectedRecord.transactionId} />
                  </Space>
                </Descriptions.Item>
              )}
              {selectedRecord.endToEnd && (
                <Descriptions.Item label="End-to-End">
                  <Space>
                    <Text code style={{ fontSize: 11 }}>{selectedRecord.endToEnd}</Text>
                    <CopyButton text={selectedRecord.endToEnd} />
                  </Space>
                </Descriptions.Item>
              )}
            </Descriptions>

            <Divider />

            <Descriptions title="Estabelecimento" column={1} bordered size="small">
              <Descriptions.Item label="Nome">
                {selectedRecord.establishment.name}
              </Descriptions.Item>
              <Descriptions.Item label="CNPJ">
                <Space>
                  <Text>{selectedRecord.establishment.cnpj}</Text>
                  <CopyButton text={selectedRecord.establishment.cnpj} />
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="E-mail">
                {selectedRecord.establishment.email}
              </Descriptions.Item>
            </Descriptions>

            <Divider />

            <Descriptions title="Valores" column={2} bordered size="small">
              <Descriptions.Item label="Quantidade">
                {selectedRecord.quantity}
              </Descriptions.Item>
              <Descriptions.Item label="Valor Unitário">
                {formatCurrency(selectedRecord.unitValue)}
              </Descriptions.Item>
              <Descriptions.Item label="Valor Total">
                <Text strong style={{ color: '#1890ff', fontSize: 16 }}>
                  {formatCurrency(selectedRecord.totalValue)}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="Taxa">
                <Text type="danger">{formatCurrency(selectedRecord.fee)}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Valor Líquido" span={2}>
                <Text strong style={{ color: '#52c41a', fontSize: 16 }}>
                  {formatCurrency(selectedRecord.netValue)}
                </Text>
              </Descriptions.Item>
            </Descriptions>

            {selectedRecord.reason && (
              <>
                <Divider />
                <Descriptions title="Motivo" column={1} bordered size="small">
                  <Descriptions.Item label="Código">
                    <Text code>{selectedRecord.reason}</Text>
                  </Descriptions.Item>
                  <Descriptions.Item label="Descrição">
                    {DISPUTE_REASON_LABELS[selectedRecord.reason as keyof typeof DISPUTE_REASON_LABELS] || selectedRecord.reason}
                  </Descriptions.Item>
                </Descriptions>
              </>
            )}

            <Divider />

            <Descriptions title="Datas" column={1} bordered size="small">
              <Descriptions.Item label="Criado em">
                {formatDate(selectedRecord.createdAt)}
              </Descriptions.Item>
              {selectedRecord.scheduledDate && (
                <Descriptions.Item label="Agendado para">
                  {dayjs(selectedRecord.scheduledDate).format('DD/MM/YYYY')}
                </Descriptions.Item>
              )}
              {selectedRecord.executionDate && (
                <Descriptions.Item label="Executado em">
                  {formatDate(selectedRecord.executionDate)}
                </Descriptions.Item>
              )}
              {selectedRecord.updatedAt && (
                <Descriptions.Item label="Atualizado em">
                  {formatDate(selectedRecord.updatedAt)}
                </Descriptions.Item>
              )}
            </Descriptions>

            {/* Actions */}
            {selectedRecord.status === 'PENDING' && (
              <>
                <Divider />
                <Space style={{ width: '100%' }}>
                  <Button
                    danger
                    icon={<CloseCircleOutlined />}
                    onClick={() => {
                      setDrawerOpen(false);
                      handleCancelMed(selectedRecord);
                    }}
                  >
                    Cancelar
                  </Button>
                </Space>
              </>
            )}

            {(selectedRecord.status === 'EXPIRED' || selectedRecord.status === 'CANCELLED') && (
              <>
                <Divider />
                <Button
                  icon={<RetweetOutlined />}
                  onClick={() => {
                    setDrawerOpen(false);
                    handleRetryMed(selectedRecord);
                  }}
                >
                  Reprocessar
                </Button>
              </>
            )}
          </div>
        )}
      </Drawer>

      {/* Cancel Modal */}
      <Modal
        title={
          <Space>
            <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
            <span>Cancelar MED/Disputa</span>
          </Space>
        }
        open={cancelModalOpen}
        onCancel={() => {
          setCancelModalOpen(false);
          cancelForm.resetFields();
        }}
        footer={null}
      >
        <Form form={cancelForm} layout="vertical" onFinish={onCancelSubmit}>
          {selectedRecord && (
            <div style={{ marginBottom: 16 }}>
              <Card size="small" style={{ background: '#fafafa' }}>
                <Row gutter={16}>
                  <Col span={12}>
                    <Text type="secondary">ID:</Text>
                    <div><Text strong>{selectedRecord.id}</Text></div>
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">Valor:</Text>
                    <div>
                      <Text strong style={{ color: '#1890ff' }}>
                        {formatCurrency(selectedRecord.totalValue)}
                      </Text>
                    </div>
                  </Col>
                </Row>
              </Card>
            </div>
          )}

          <Form.Item
            name="reason"
            label="Motivo do cancelamento"
            rules={[{ required: true, message: 'Informe o motivo do cancelamento' }]}
          >
            <TextArea
              rows={4}
              placeholder="Descreva o motivo do cancelamento..."
              maxLength={500}
              showCount
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setCancelModalOpen(false)}>
                Voltar
              </Button>
              <Button type="primary" danger htmlType="submit">
                Confirmar Cancelamento
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default MedAnalyticsPage;
