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
  message,
  Tooltip,
  Progress,
  Timeline,
  Empty,
} from 'antd';
import {
  SearchOutlined,
  FilterOutlined,
  ReloadOutlined,
  ExportOutlined,
  DownloadOutlined,
  EyeOutlined,
  ArrowUpOutlined,
  ArrowDownOutlined,
  SwapOutlined,
  BankOutlined,
  RollbackOutlined,
  PercentageOutlined,
  TeamOutlined,
  EditOutlined,
  ExclamationCircleOutlined,
  LockOutlined,
  UnlockOutlined,
  WalletOutlined,
  CalendarOutlined,
  FileTextOutlined,
  DollarOutlined,
  RiseOutlined,
  FallOutlined,
  LineChartOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import ActionMenu from '@/components/ActionMenu';
import CopyButton from '@/components/CopyButton';
import type {
  StatementEntry,
  StatementSummary,
  StatementEntryType,
  StatementEntryCategory,
  WalletType,
} from '@/types';
import {
  STATEMENT_ENTRY_TYPE_COLORS,
  STATEMENT_ENTRY_TYPE_LABELS,
  STATEMENT_CATEGORY_COLORS,
  STATEMENT_CATEGORY_LABELS,
  WALLET_TYPE_LABELS,
} from '@/utils/constants';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;

// Icon mapping for entry types
const EntryTypeIcon: React.FC<{ type: StatementEntryType }> = ({ type }) => {
  const icons: Record<StatementEntryType, React.ReactNode> = {
    CREDIT: <ArrowDownOutlined style={{ color: '#52c41a' }} />,
    DEBIT: <ArrowUpOutlined style={{ color: '#ff4d4f' }} />,
    TRANSACTION: <SwapOutlined style={{ color: '#52c41a' }} />,
    WITHDRAWAL: <BankOutlined style={{ color: '#ff4d4f' }} />,
    REFUND: <RollbackOutlined style={{ color: '#fa8c16' }} />,
    FEE: <PercentageOutlined style={{ color: '#8c8c8c' }} />,
    COMMISSION: <TeamOutlined style={{ color: '#722ed1' }} />,
    ADJUSTMENT: <EditOutlined style={{ color: '#1890ff' }} />,
    CHARGEBACK: <ExclamationCircleOutlined style={{ color: '#ff4d4f' }} />,
    RETENTION: <LockOutlined style={{ color: '#faad14' }} />,
    RELEASE: <UnlockOutlined style={{ color: '#13c2c2' }} />,
  };
  return <>{icons[type]}</>;
};

// Mock summary data
const mockSummary: StatementSummary = {
  period: {
    startDate: '2024-01-01',
    endDate: '2024-01-26',
  },
  openingBalance: 125678.90,
  closingBalance: 189456.78,
  totalCredits: 456789.12,
  totalDebits: 392011.24,
  netChange: 63777.88,
  byType: {
    transactions: { count: 1234, amount: 456789.12 },
    withdrawals: { count: 45, amount: 234567.89 },
    refunds: { count: 23, amount: 45678.90 },
    fees: { count: 1234, amount: 12345.67 },
    commissions: { count: 89, amount: 34567.89 },
    adjustments: { count: 5, amount: 2345.67 },
    chargebacks: { count: 3, amount: 12506.22 },
  },
  byWallet: {
    pix: { credits: 345678.90, debits: 234567.89, balance: 111111.01 },
    card: { credits: 98765.43, debits: 145678.90, balance: 56789.53 },
    boleto: { credits: 12344.79, debits: 11764.45, balance: 21556.24 },
  },
  dailyBalance: [
    { date: '2024-01-20', credits: 45678.90, debits: 34567.89, balance: 156789.01 },
    { date: '2024-01-21', credits: 56789.01, debits: 45678.90, balance: 167899.12 },
    { date: '2024-01-22', credits: 34567.89, debits: 23456.78, balance: 179010.23 },
    { date: '2024-01-23', credits: 67890.12, debits: 56789.01, balance: 190111.34 },
    { date: '2024-01-24', credits: 45678.90, debits: 34567.89, balance: 201222.35 },
    { date: '2024-01-25', credits: 23456.78, debits: 45678.90, balance: 178999.23 },
    { date: '2024-01-26', credits: 34567.89, debits: 24110.34, balance: 189456.78 },
  ],
};

// Mock entries data
const mockEntries: StatementEntry[] = [
  {
    id: 'STM001',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90' },
    type: 'TRANSACTION',
    category: 'INCOME',
    description: 'Pagamento PIX recebido',
    amount: 1500.00,
    balanceBefore: 187956.78,
    balanceAfter: 189456.78,
    wallet: 'PIX',
    metadata: {
      transactionId: 'TXN-2024-001',
      endToEnd: 'E0000000020240126153045789012345',
      customerName: 'João Silva',
      fee: 3.00,
    },
    createdAt: '2024-01-26T15:30:45',
  },
  {
    id: 'STM002',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90' },
    type: 'FEE',
    category: 'EXPENSE',
    description: 'Taxa de transação PIX',
    amount: -3.00,
    balanceBefore: 187959.78,
    balanceAfter: 187956.78,
    wallet: 'PIX',
    metadata: {
      transactionId: 'TXN-2024-001',
    },
    createdAt: '2024-01-26T15:30:45',
  },
  {
    id: 'STM003',
    establishment: { id: '2', name: 'E-commerce XYZ', cnpj: '98.765.432/0001-10' },
    type: 'WITHDRAWAL',
    category: 'EXPENSE',
    description: 'Saque automático PIX',
    amount: -5000.00,
    balanceBefore: 192959.78,
    balanceAfter: 187959.78,
    wallet: 'PIX',
    metadata: {
      withdrawalId: 'WTH-2024-001',
      endToEnd: 'E0000000020240126140030456789012',
    },
    createdAt: '2024-01-26T14:00:30',
  },
  {
    id: 'STM004',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90' },
    type: 'TRANSACTION',
    category: 'INCOME',
    description: 'Pagamento Cartão - 3x',
    amount: 2500.00,
    balanceBefore: 190459.78,
    balanceAfter: 192959.78,
    wallet: 'CARD',
    metadata: {
      transactionId: 'TXN-2024-002',
      customerName: 'Maria Santos',
      fee: 147.50,
    },
    createdAt: '2024-01-26T12:45:00',
  },
  {
    id: 'STM005',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90' },
    type: 'FEE',
    category: 'EXPENSE',
    description: 'Taxa cartão 3x (5.90%)',
    amount: -147.50,
    balanceBefore: 190607.28,
    balanceAfter: 190459.78,
    wallet: 'CARD',
    metadata: {
      transactionId: 'TXN-2024-002',
    },
    createdAt: '2024-01-26T12:45:00',
  },
  {
    id: 'STM006',
    establishment: { id: '3', name: 'Marketplace Plus', cnpj: '45.678.901/0001-23' },
    type: 'REFUND',
    category: 'EXPENSE',
    description: 'Estorno de transação',
    amount: -890.00,
    balanceBefore: 191497.28,
    balanceAfter: 190607.28,
    wallet: 'PIX',
    metadata: {
      transactionId: 'TXN-2024-003',
      customerName: 'Pedro Oliveira',
    },
    createdAt: '2024-01-26T11:20:00',
  },
  {
    id: 'STM007',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90' },
    type: 'COMMISSION',
    category: 'EXPENSE',
    description: 'Comissão afiliado - João Silva',
    amount: -150.00,
    balanceBefore: 191647.28,
    balanceAfter: 191497.28,
    wallet: 'PIX',
    metadata: {
      affiliateId: 'AFF001',
      transactionId: 'TXN-2024-004',
    },
    createdAt: '2024-01-26T10:15:00',
  },
  {
    id: 'STM008',
    establishment: { id: '2', name: 'E-commerce XYZ', cnpj: '98.765.432/0001-10' },
    type: 'CHARGEBACK',
    category: 'EXPENSE',
    description: 'Chargeback - Disputa #DSP001',
    amount: -2500.00,
    balanceBefore: 194147.28,
    balanceAfter: 191647.28,
    wallet: 'CARD',
    metadata: {
      disputeId: 'DSP001',
      transactionId: 'TXN-2024-005',
    },
    createdAt: '2024-01-26T09:00:00',
  },
  {
    id: 'STM009',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90' },
    type: 'RETENTION',
    category: 'ADJUSTMENT',
    description: 'Retenção preventiva',
    amount: -1000.00,
    balanceBefore: 195147.28,
    balanceAfter: 194147.28,
    wallet: 'PIX',
    createdAt: '2024-01-25T18:30:00',
  },
  {
    id: 'STM010',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90' },
    type: 'ADJUSTMENT',
    category: 'ADJUSTMENT',
    description: 'Ajuste manual - Correção de saldo',
    amount: 500.00,
    balanceBefore: 194647.28,
    balanceAfter: 195147.28,
    wallet: 'PIX',
    createdAt: '2024-01-25T16:00:00',
  },
  {
    id: 'STM011',
    establishment: { id: '4', name: 'Digital Store', cnpj: '56.789.012/0001-34' },
    type: 'RELEASE',
    category: 'ADJUSTMENT',
    description: 'Liberação de retenção',
    amount: 2000.00,
    balanceBefore: 192647.28,
    balanceAfter: 194647.28,
    wallet: 'CARD',
    createdAt: '2024-01-25T14:30:00',
  },
  {
    id: 'STM012',
    establishment: { id: '1', name: 'Loja Virtual ABC', cnpj: '12.345.678/0001-90' },
    type: 'TRANSACTION',
    category: 'INCOME',
    description: 'Pagamento Boleto recebido',
    amount: 3456.78,
    balanceBefore: 189190.50,
    balanceAfter: 192647.28,
    wallet: 'BOLETO',
    metadata: {
      transactionId: 'TXN-2024-006',
      customerName: 'Empresa ABC Ltda',
      fee: 3.50,
    },
    createdAt: '2024-01-25T11:00:00',
  },
];

const StatementsPage: React.FC = () => {
  const [entries] = useState<StatementEntry[]>(mockEntries);
  const [summary] = useState<StatementSummary>(mockSummary);
  const [loading, setLoading] = useState(false);
  const [selectedEntry, setSelectedEntry] = useState<StatementEntry | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);

  // Filters
  const [searchText, setSearchText] = useState('');
  const [typeFilter, setTypeFilter] = useState<StatementEntryType | undefined>();
  const [categoryFilter, setCategoryFilter] = useState<StatementEntryCategory | undefined>();
  const [walletFilter, setWalletFilter] = useState<WalletType | undefined>();
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>([
    dayjs().subtract(30, 'day'),
    dayjs(),
  ]);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [establishmentFilter, setEstablishmentFilter] = useState<string | undefined>();
  const [minAmount, setMinAmount] = useState<number | undefined>();
  const [maxAmount, setMaxAmount] = useState<number | undefined>();

  // Filtered entries
  const filteredEntries = useMemo(() => {
    return entries.filter((entry) => {
      if (searchText) {
        const search = searchText.toLowerCase();
        const matchId = entry.id.toLowerCase().includes(search);
        const matchDescription = entry.description.toLowerCase().includes(search);
        const matchEstablishment = entry.establishment.name.toLowerCase().includes(search);
        const matchCnpj = entry.establishment.cnpj.includes(search);
        const matchTransaction = entry.metadata?.transactionId?.toLowerCase().includes(search);
        if (!matchId && !matchDescription && !matchEstablishment && !matchCnpj && !matchTransaction) {
          return false;
        }
      }
      if (typeFilter && entry.type !== typeFilter) return false;
      if (categoryFilter && entry.category !== categoryFilter) return false;
      if (walletFilter && entry.wallet !== walletFilter) return false;
      if (establishmentFilter && entry.establishment.id !== establishmentFilter) return false;
      if (minAmount && Math.abs(entry.amount) < minAmount) return false;
      if (maxAmount && Math.abs(entry.amount) > maxAmount) return false;
      if (dateRange) {
        const entryDate = dayjs(entry.createdAt);
        if (entryDate.isBefore(dateRange[0], 'day') || entryDate.isAfter(dateRange[1], 'day')) {
          return false;
        }
      }
      return true;
    });
  }, [entries, searchText, typeFilter, categoryFilter, walletFilter, establishmentFilter, minAmount, maxAmount, dateRange]);

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
    setTypeFilter(undefined);
    setCategoryFilter(undefined);
    setWalletFilter(undefined);
    setEstablishmentFilter(undefined);
    setDateRange([dayjs().subtract(30, 'day'), dayjs()]);
    setMinAmount(undefined);
    setMaxAmount(undefined);
  };

  const handleViewEntry = (entry: StatementEntry) => {
    setSelectedEntry(entry);
    setDrawerOpen(true);
  };

  const handleExport = (format: 'csv' | 'xlsx' | 'pdf') => {
    message.success(`Exportação ${format.toUpperCase()} iniciada. O arquivo será baixado em instantes.`);
  };

  // Establishment options for filter
  const establishmentOptions = useMemo(() => {
    const unique = new Map();
    entries.forEach((e) => {
      if (!unique.has(e.establishment.id)) {
        unique.set(e.establishment.id, e.establishment);
      }
    });
    return Array.from(unique.values()).map((est) => ({
      label: est.name,
      value: est.id,
    }));
  }, [entries]);

  const columns: ColumnsType<StatementEntry> = [
    {
      title: 'Data',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (date: string) => (
        <Text style={{ fontSize: 13 }}>{formatDate(date)}</Text>
      ),
      sorter: (a, b) => dayjs(a.createdAt).unix() - dayjs(b.createdAt).unix(),
      defaultSortOrder: 'descend',
    },
    {
      title: 'Tipo',
      key: 'type',
      width: 140,
      render: (_: any, record: StatementEntry) => (
        <Space>
          <EntryTypeIcon type={record.type} />
          <Tag color={STATEMENT_ENTRY_TYPE_COLORS[record.type]}>
            {STATEMENT_ENTRY_TYPE_LABELS[record.type]}
          </Tag>
        </Space>
      ),
      filters: [
        { text: 'Transação', value: 'TRANSACTION' },
        { text: 'Saque', value: 'WITHDRAWAL' },
        { text: 'Estorno', value: 'REFUND' },
        { text: 'Taxa', value: 'FEE' },
        { text: 'Comissão', value: 'COMMISSION' },
        { text: 'Ajuste', value: 'ADJUSTMENT' },
        { text: 'Chargeback', value: 'CHARGEBACK' },
        { text: 'Retenção', value: 'RETENTION' },
        { text: 'Liberação', value: 'RELEASE' },
      ],
      onFilter: (value, record) => record.type === value,
    },
    {
      title: 'Descrição',
      dataIndex: 'description',
      key: 'description',
      width: 280,
      ellipsis: true,
      render: (description: string, record: StatementEntry) => (
        <Tooltip title={description}>
          <div>
            <Text ellipsis style={{ maxWidth: 260 }}>{description}</Text>
            {record.metadata?.customerName && (
              <div>
                <Text type="secondary" style={{ fontSize: 11 }}>
                  Cliente: {record.metadata.customerName}
                </Text>
              </div>
            )}
          </div>
        </Tooltip>
      ),
    },
    {
      title: 'Estabelecimento',
      key: 'establishment',
      width: 180,
      ellipsis: true,
      render: (_: any, record: StatementEntry) => (
        <Tooltip title={record.establishment.cnpj}>
          <Text ellipsis style={{ maxWidth: 160 }}>{record.establishment.name}</Text>
        </Tooltip>
      ),
    },
    {
      title: 'Carteira',
      dataIndex: 'wallet',
      key: 'wallet',
      width: 100,
      render: (wallet: WalletType) => (
        <Tag>{WALLET_TYPE_LABELS[wallet]?.replace('Carteira ', '') || wallet}</Tag>
      ),
      filters: [
        { text: 'PIX', value: 'PIX' },
        { text: 'Cartão', value: 'CARD' },
        { text: 'Boleto', value: 'BOLETO' },
      ],
      onFilter: (value, record) => record.wallet === value,
    },
    {
      title: 'Valor',
      dataIndex: 'amount',
      key: 'amount',
      width: 140,
      align: 'right',
      render: (amount: number) => (
        <Text
          strong
          style={{
            color: amount >= 0 ? '#52c41a' : '#ff4d4f',
            fontSize: 14,
          }}
        >
          {amount >= 0 ? '+' : ''}{formatCurrency(amount)}
        </Text>
      ),
      sorter: (a, b) => a.amount - b.amount,
    },
    {
      title: 'Saldo',
      dataIndex: 'balanceAfter',
      key: 'balanceAfter',
      width: 140,
      align: 'right',
      render: (balance: number) => (
        <Text style={{ color: '#1890ff' }}>
          {formatCurrency(balance)}
        </Text>
      ),
    },
    {
      title: 'Ações',
      key: 'actions',
      width: 70,
      fixed: 'right',
      render: (_: any, record: StatementEntry) => (
        <Tooltip title="Ver detalhes">
          <Button
            type="text"
            icon={<EyeOutlined />}
            onClick={() => handleViewEntry(record)}
          />
        </Tooltip>
      ),
    },
  ];

  // Calculate totals for filtered entries
  const filteredTotals = useMemo(() => {
    const credits = filteredEntries.filter(e => e.amount > 0).reduce((sum, e) => sum + e.amount, 0);
    const debits = filteredEntries.filter(e => e.amount < 0).reduce((sum, e) => sum + Math.abs(e.amount), 0);
    return { credits, debits, net: credits - debits };
  }, [filteredEntries]);

  return (
    <div style={{ padding: '0 0 24px 0' }}>
      {/* Header */}
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>
            <FileTextOutlined style={{ marginRight: 8 }} />
            Extrato
          </Title>
          <Text type="secondary">
            Movimentações financeiras detalhadas
          </Text>
        </div>
        <Space>
          <Select
            placeholder="Exportar como..."
            style={{ width: 150 }}
            onChange={handleExport}
            value={undefined}
            options={[
              { label: 'Exportar CSV', value: 'csv' },
              { label: 'Exportar Excel', value: 'xlsx' },
              { label: 'Exportar PDF', value: 'pdf' },
            ]}
          />
        </Space>
      </div>

      {/* Summary Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Saldo Atual"
              value={summary.closingBalance}
              prefix={<WalletOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                Saldo inicial: {formatCurrency(summary.openingBalance)}
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Total de Entradas"
              value={summary.totalCredits}
              prefix={<ArrowDownOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                {summary.byType.transactions.count} transações
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Total de Saídas"
              value={summary.totalDebits}
              prefix={<ArrowUpOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                {summary.byType.withdrawals.count} saques
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Variação do Período"
              value={summary.netChange}
              prefix={summary.netChange >= 0 ? <RiseOutlined style={{ color: '#52c41a' }} /> : <FallOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: summary.netChange >= 0 ? '#52c41a' : '#ff4d4f' }}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                {dayjs(summary.period.startDate).format('DD/MM')} - {dayjs(summary.period.endDate).format('DD/MM/YYYY')}
              </Text>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Wallet Balances and Daily Trend */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="Saldo por Carteira" bordered={false} size="small">
            <Row gutter={[8, 8]}>
              <Col span={8}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#f6ffed' }}>
                  <div style={{ marginBottom: 4 }}>
                    <Text type="secondary">PIX</Text>
                  </div>
                  <Statistic
                    value={summary.byWallet.pix.balance}
                    valueStyle={{ fontSize: 18, color: '#52c41a' }}
                    precision={2}
                    prefix="R$"
                  />
                  <div style={{ marginTop: 4, fontSize: 11 }}>
                    <Text type="secondary">
                      +{formatCurrency(summary.byWallet.pix.credits)} | -{formatCurrency(summary.byWallet.pix.debits)}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col span={8}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#e6f7ff' }}>
                  <div style={{ marginBottom: 4 }}>
                    <Text type="secondary">Cartão</Text>
                  </div>
                  <Statistic
                    value={summary.byWallet.card.balance}
                    valueStyle={{ fontSize: 18, color: '#1890ff' }}
                    precision={2}
                    prefix="R$"
                  />
                  <div style={{ marginTop: 4, fontSize: 11 }}>
                    <Text type="secondary">
                      +{formatCurrency(summary.byWallet.card.credits)} | -{formatCurrency(summary.byWallet.card.debits)}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col span={8}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#fffbe6' }}>
                  <div style={{ marginBottom: 4 }}>
                    <Text type="secondary">Boleto</Text>
                  </div>
                  <Statistic
                    value={summary.byWallet.boleto.balance}
                    valueStyle={{ fontSize: 18, color: '#faad14' }}
                    precision={2}
                    prefix="R$"
                  />
                  <div style={{ marginTop: 4, fontSize: 11 }}>
                    <Text type="secondary">
                      +{formatCurrency(summary.byWallet.boleto.credits)} | -{formatCurrency(summary.byWallet.boleto.debits)}
                    </Text>
                  </div>
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card
            title={
              <Space>
                <LineChartOutlined />
                <span>Evolução do Saldo (Últimos 7 dias)</span>
              </Space>
            }
            bordered={false}
            size="small"
          >
            <div style={{ padding: '8px 0' }}>
              {summary.dailyBalance.map((day, index) => (
                <div key={day.date} style={{ marginBottom: 8 }}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 2 }}>
                    <Text style={{ fontSize: 12 }}>{dayjs(day.date).format('DD/MM')}</Text>
                    <Text strong style={{ fontSize: 12, color: '#1890ff' }}>
                      {formatCurrency(day.balance)}
                    </Text>
                  </div>
                  <Progress
                    percent={Math.min(100, (day.balance / 250000) * 100)}
                    strokeColor={{
                      '0%': '#1890ff',
                      '100%': '#52c41a',
                    }}
                    showInfo={false}
                    size="small"
                  />
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>

      {/* Type Breakdown */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col span={24}>
          <Card title="Resumo por Tipo de Movimentação" bordered={false} size="small">
            <Row gutter={[8, 8]}>
              <Col xs={12} sm={8} md={6} lg={3}>
                <Card bordered size="small" style={{ textAlign: 'center' }}>
                  <SwapOutlined style={{ fontSize: 20, color: '#52c41a' }} />
                  <div style={{ marginTop: 4 }}>
                    <Text strong style={{ fontSize: 14 }}>{summary.byType.transactions.count}</Text>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 11 }}>Transações</Text>
                  </div>
                  <div>
                    <Text style={{ fontSize: 11, color: '#52c41a' }}>
                      {formatCurrency(summary.byType.transactions.amount)}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col xs={12} sm={8} md={6} lg={3}>
                <Card bordered size="small" style={{ textAlign: 'center' }}>
                  <BankOutlined style={{ fontSize: 20, color: '#ff4d4f' }} />
                  <div style={{ marginTop: 4 }}>
                    <Text strong style={{ fontSize: 14 }}>{summary.byType.withdrawals.count}</Text>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 11 }}>Saques</Text>
                  </div>
                  <div>
                    <Text style={{ fontSize: 11, color: '#ff4d4f' }}>
                      -{formatCurrency(summary.byType.withdrawals.amount)}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col xs={12} sm={8} md={6} lg={3}>
                <Card bordered size="small" style={{ textAlign: 'center' }}>
                  <RollbackOutlined style={{ fontSize: 20, color: '#fa8c16' }} />
                  <div style={{ marginTop: 4 }}>
                    <Text strong style={{ fontSize: 14 }}>{summary.byType.refunds.count}</Text>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 11 }}>Estornos</Text>
                  </div>
                  <div>
                    <Text style={{ fontSize: 11, color: '#fa8c16' }}>
                      -{formatCurrency(summary.byType.refunds.amount)}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col xs={12} sm={8} md={6} lg={3}>
                <Card bordered size="small" style={{ textAlign: 'center' }}>
                  <PercentageOutlined style={{ fontSize: 20, color: '#8c8c8c' }} />
                  <div style={{ marginTop: 4 }}>
                    <Text strong style={{ fontSize: 14 }}>{summary.byType.fees.count}</Text>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 11 }}>Taxas</Text>
                  </div>
                  <div>
                    <Text style={{ fontSize: 11, color: '#8c8c8c' }}>
                      -{formatCurrency(summary.byType.fees.amount)}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col xs={12} sm={8} md={6} lg={3}>
                <Card bordered size="small" style={{ textAlign: 'center' }}>
                  <TeamOutlined style={{ fontSize: 20, color: '#722ed1' }} />
                  <div style={{ marginTop: 4 }}>
                    <Text strong style={{ fontSize: 14 }}>{summary.byType.commissions.count}</Text>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 11 }}>Comissões</Text>
                  </div>
                  <div>
                    <Text style={{ fontSize: 11, color: '#722ed1' }}>
                      -{formatCurrency(summary.byType.commissions.amount)}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col xs={12} sm={8} md={6} lg={3}>
                <Card bordered size="small" style={{ textAlign: 'center' }}>
                  <EditOutlined style={{ fontSize: 20, color: '#1890ff' }} />
                  <div style={{ marginTop: 4 }}>
                    <Text strong style={{ fontSize: 14 }}>{summary.byType.adjustments.count}</Text>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 11 }}>Ajustes</Text>
                  </div>
                  <div>
                    <Text style={{ fontSize: 11, color: '#1890ff' }}>
                      {formatCurrency(summary.byType.adjustments.amount)}
                    </Text>
                  </div>
                </Card>
              </Col>
              <Col xs={12} sm={8} md={6} lg={3}>
                <Card bordered size="small" style={{ textAlign: 'center' }}>
                  <ExclamationCircleOutlined style={{ fontSize: 20, color: '#ff4d4f' }} />
                  <div style={{ marginTop: 4 }}>
                    <Text strong style={{ fontSize: 14 }}>{summary.byType.chargebacks.count}</Text>
                  </div>
                  <div>
                    <Text type="secondary" style={{ fontSize: 11 }}>Chargebacks</Text>
                  </div>
                  <div>
                    <Text style={{ fontSize: 11, color: '#ff4d4f' }}>
                      -{formatCurrency(summary.byType.chargebacks.amount)}
                    </Text>
                  </div>
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card bordered={false} style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space wrap size="middle">
              <Input
                placeholder="Buscar por descrição, estabelecimento, ID..."
                prefix={<SearchOutlined />}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                style={{ width: 320 }}
                allowClear
              />
              <Select
                placeholder="Tipo"
                value={typeFilter}
                onChange={setTypeFilter}
                style={{ width: 140 }}
                allowClear
                options={[
                  { label: 'Transação', value: 'TRANSACTION' },
                  { label: 'Saque', value: 'WITHDRAWAL' },
                  { label: 'Estorno', value: 'REFUND' },
                  { label: 'Taxa', value: 'FEE' },
                  { label: 'Comissão', value: 'COMMISSION' },
                  { label: 'Ajuste', value: 'ADJUSTMENT' },
                  { label: 'Chargeback', value: 'CHARGEBACK' },
                  { label: 'Retenção', value: 'RETENTION' },
                  { label: 'Liberação', value: 'RELEASE' },
                ]}
              />
              <Select
                placeholder="Categoria"
                value={categoryFilter}
                onChange={setCategoryFilter}
                style={{ width: 120 }}
                allowClear
                options={[
                  { label: 'Entrada', value: 'INCOME' },
                  { label: 'Saída', value: 'EXPENSE' },
                  { label: 'Ajuste', value: 'ADJUSTMENT' },
                ]}
              />
              <Select
                placeholder="Carteira"
                value={walletFilter}
                onChange={setWalletFilter}
                style={{ width: 120 }}
                allowClear
                options={[
                  { label: 'PIX', value: 'PIX' },
                  { label: 'Cartão', value: 'CARD' },
                  { label: 'Boleto', value: 'BOLETO' },
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
              {(searchText || typeFilter || categoryFilter || walletFilter || establishmentFilter || minAmount || maxAmount) && (
                <Button onClick={handleClearFilters}>Limpar filtros</Button>
              )}
            </Space>
          </Col>
          <Col>
            <Tooltip title="Atualizar">
              <Button icon={<ReloadOutlined />} onClick={handleRefresh} loading={loading} />
            </Tooltip>
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
                  value={minAmount}
                  onChange={(e) => setMinAmount(e.target.value ? Number(e.target.value) : undefined)}
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
                  value={maxAmount}
                  onChange={(e) => setMaxAmount(e.target.value ? Number(e.target.value) : undefined)}
                  prefix="R$"
                />
              </Col>
            </Row>
          </>
        )}
      </Card>

      {/* Results Info */}
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Text type="secondary">
          Exibindo {filteredEntries.length} de {entries.length} movimentações
        </Text>
        <Space size="large">
          <Text>
            Entradas: <Text strong style={{ color: '#52c41a' }}>{formatCurrency(filteredTotals.credits)}</Text>
          </Text>
          <Text>
            Saídas: <Text strong style={{ color: '#ff4d4f' }}>{formatCurrency(filteredTotals.debits)}</Text>
          </Text>
          <Text>
            Líquido: <Text strong style={{ color: filteredTotals.net >= 0 ? '#52c41a' : '#ff4d4f' }}>
              {formatCurrency(filteredTotals.net)}
            </Text>
          </Text>
        </Space>
      </div>

      {/* Table */}
      <Card bordered={false}>
        <Table
          columns={columns}
          dataSource={filteredEntries}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1300 }}
          pagination={{
            total: filteredEntries.length,
            pageSize: 20,
            showSizeChanger: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} de ${total} movimentações`,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
          rowClassName={(record) => record.amount >= 0 ? 'row-income' : 'row-expense'}
        />
      </Card>

      {/* Detail Drawer */}
      <Drawer
        title={
          <Space>
            <FileTextOutlined />
            <span>Detalhes da Movimentação</span>
          </Space>
        }
        placement="right"
        width={550}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedEntry(null);
        }}
        extra={
          selectedEntry && (
            <Tag color={STATEMENT_ENTRY_TYPE_COLORS[selectedEntry.type]}>
              {STATEMENT_ENTRY_TYPE_LABELS[selectedEntry.type]}
            </Tag>
          )
        }
      >
        {selectedEntry && (
          <div>
            {/* Amount Display */}
            <div style={{ textAlign: 'center', marginBottom: 24 }}>
              <EntryTypeIcon type={selectedEntry.type} />
              <Title
                level={2}
                style={{
                  margin: '8px 0 0 0',
                  color: selectedEntry.amount >= 0 ? '#52c41a' : '#ff4d4f',
                }}
              >
                {selectedEntry.amount >= 0 ? '+' : ''}{formatCurrency(selectedEntry.amount)}
              </Title>
              <Text type="secondary">{selectedEntry.description}</Text>
            </div>

            <Descriptions column={1} bordered size="small">
              <Descriptions.Item label="ID">
                <Space>
                  <Text code>{selectedEntry.id}</Text>
                  <CopyButton text={selectedEntry.id} />
                </Space>
              </Descriptions.Item>
              <Descriptions.Item label="Tipo">
                <Tag color={STATEMENT_ENTRY_TYPE_COLORS[selectedEntry.type]}>
                  {STATEMENT_ENTRY_TYPE_LABELS[selectedEntry.type]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Categoria">
                <Tag color={STATEMENT_CATEGORY_COLORS[selectedEntry.category]}>
                  {STATEMENT_CATEGORY_LABELS[selectedEntry.category]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Carteira">
                <Tag>{WALLET_TYPE_LABELS[selectedEntry.wallet]?.replace('Carteira ', '') || selectedEntry.wallet}</Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Data/Hora">
                {formatDate(selectedEntry.createdAt)}
              </Descriptions.Item>
            </Descriptions>

            <Divider />

            <Descriptions title="Estabelecimento" column={1} bordered size="small">
              <Descriptions.Item label="Nome">
                {selectedEntry.establishment.name}
              </Descriptions.Item>
              <Descriptions.Item label="CNPJ">
                <Space>
                  <Text>{selectedEntry.establishment.cnpj}</Text>
                  <CopyButton text={selectedEntry.establishment.cnpj} />
                </Space>
              </Descriptions.Item>
            </Descriptions>

            <Divider />

            <Descriptions title="Saldos" column={2} bordered size="small">
              <Descriptions.Item label="Antes">
                {formatCurrency(selectedEntry.balanceBefore)}
              </Descriptions.Item>
              <Descriptions.Item label="Depois">
                <Text strong style={{ color: '#1890ff' }}>
                  {formatCurrency(selectedEntry.balanceAfter)}
                </Text>
              </Descriptions.Item>
            </Descriptions>

            {selectedEntry.metadata && Object.keys(selectedEntry.metadata).length > 0 && (
              <>
                <Divider />
                <Descriptions title="Informações Adicionais" column={1} bordered size="small">
                  {selectedEntry.metadata.transactionId && (
                    <Descriptions.Item label="ID Transação">
                      <Space>
                        <Text code>{selectedEntry.metadata.transactionId}</Text>
                        <CopyButton text={selectedEntry.metadata.transactionId} />
                      </Space>
                    </Descriptions.Item>
                  )}
                  {selectedEntry.metadata.withdrawalId && (
                    <Descriptions.Item label="ID Saque">
                      <Space>
                        <Text code>{selectedEntry.metadata.withdrawalId}</Text>
                        <CopyButton text={selectedEntry.metadata.withdrawalId} />
                      </Space>
                    </Descriptions.Item>
                  )}
                  {selectedEntry.metadata.disputeId && (
                    <Descriptions.Item label="ID Disputa">
                      <Space>
                        <Text code>{selectedEntry.metadata.disputeId}</Text>
                        <CopyButton text={selectedEntry.metadata.disputeId} />
                      </Space>
                    </Descriptions.Item>
                  )}
                  {selectedEntry.metadata.affiliateId && (
                    <Descriptions.Item label="ID Afiliado">
                      <Space>
                        <Text code>{selectedEntry.metadata.affiliateId}</Text>
                        <CopyButton text={selectedEntry.metadata.affiliateId} />
                      </Space>
                    </Descriptions.Item>
                  )}
                  {selectedEntry.metadata.endToEnd && (
                    <Descriptions.Item label="End-to-End">
                      <Space>
                        <Text code style={{ fontSize: 10 }}>{selectedEntry.metadata.endToEnd}</Text>
                        <CopyButton text={selectedEntry.metadata.endToEnd} />
                      </Space>
                    </Descriptions.Item>
                  )}
                  {selectedEntry.metadata.customerName && (
                    <Descriptions.Item label="Cliente">
                      {selectedEntry.metadata.customerName}
                    </Descriptions.Item>
                  )}
                  {selectedEntry.metadata.fee !== undefined && (
                    <Descriptions.Item label="Taxa">
                      <Text type="danger">{formatCurrency(selectedEntry.metadata.fee)}</Text>
                    </Descriptions.Item>
                  )}
                </Descriptions>
              </>
            )}
          </div>
        )}
      </Drawer>
    </div>
  );
};

export default StatementsPage;
