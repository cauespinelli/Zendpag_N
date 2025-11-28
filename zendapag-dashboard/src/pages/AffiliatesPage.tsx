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
  InputNumber,
  Avatar,
  List,
} from 'antd';
import {
  SearchOutlined,
  FilterOutlined,
  ReloadOutlined,
  ExportOutlined,
  PlusOutlined,
  EyeOutlined,
  EditOutlined,
  StopOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  UserOutlined,
  TeamOutlined,
  DollarOutlined,
  ShopOutlined,
  LinkOutlined,
  PercentageOutlined,
  WalletOutlined,
  HistoryOutlined,
  BankOutlined,
  CopyOutlined,
  ShareAltOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import ActionMenu from '@/components/ActionMenu';
import CopyButton from '@/components/CopyButton';
import type {
  Affiliate,
  AffiliateSummary,
  AffiliateStatus,
  AffiliateCommission,
  AffiliateEstablishment,
  PixKeyType,
} from '@/types';
import {
  AFFILIATE_STATUS_COLORS,
  AFFILIATE_STATUS_LABELS,
  COMMISSION_STATUS_COLORS,
  COMMISSION_STATUS_LABELS,
  PIX_KEY_TYPE_LABELS,
} from '@/utils/constants';

const { Title, Text, Paragraph } = Typography;
const { RangePicker } = DatePicker;
const { TextArea } = Input;

// Mock summary data
const mockSummary: AffiliateSummary = {
  totalAffiliates: 156,
  activeAffiliates: 134,
  totalCommissionsPaid: 2345678.90,
  totalCommissionsPending: 156789.45,
  totalLinkedEstablishments: 892,
  totalVolumeGenerated: 45678901.23,
  byStatus: {
    active: 134,
    inactive: 12,
    blocked: 5,
    pending: 5,
  },
  topAffiliates: [
    { id: '1', name: 'João Silva', establishments: 45, volume: 5678901.23, commissions: 567890.12 },
    { id: '2', name: 'Maria Santos', establishments: 38, volume: 4567890.12, commissions: 456789.01 },
    { id: '3', name: 'Pedro Oliveira', establishments: 32, volume: 3456789.01, commissions: 345678.90 },
    { id: '4', name: 'Ana Costa', establishments: 28, volume: 2345678.90, commissions: 234567.89 },
    { id: '5', name: 'Carlos Lima', establishments: 25, volume: 1234567.89, commissions: 123456.78 },
  ],
};

// Mock affiliates data
const mockAffiliates: Affiliate[] = [
  {
    id: 'AFF001',
    referenceCode: 'JOAOSILVA2024',
    name: 'João Silva',
    email: 'joao.silva@email.com',
    phone: '(11) 99999-1234',
    document: '123.456.789-00',
    pixKey: { value: 'joao.silva@email.com', type: 'EMAIL' as PixKeyType },
    status: 'ACTIVE',
    commissionRate: 10,
    balance: {
      available: 15678.90,
      pending: 3456.78,
      blocked: 0,
      totalEarned: 567890.12,
      totalWithdrawn: 548754.44,
    },
    linkedEstablishments: 45,
    totalTransactions: 12567,
    totalVolume: 5678901.23,
    createdAt: '2023-06-15T10:30:00',
    updatedAt: '2024-01-26T14:20:00',
    lastWithdrawalAt: '2024-01-20T16:45:00',
  },
  {
    id: 'AFF002',
    referenceCode: 'MARIASANTOS24',
    name: 'Maria Santos',
    email: 'maria.santos@email.com',
    phone: '(21) 98888-5678',
    document: '987.654.321-00',
    pixKey: { value: '11988885678', type: 'PHONE' as PixKeyType },
    status: 'ACTIVE',
    commissionRate: 8,
    balance: {
      available: 8765.43,
      pending: 2345.67,
      blocked: 0,
      totalEarned: 456789.01,
      totalWithdrawn: 445677.91,
    },
    linkedEstablishments: 38,
    totalTransactions: 9876,
    totalVolume: 4567890.12,
    createdAt: '2023-08-20T14:15:00',
    updatedAt: '2024-01-25T09:30:00',
    lastWithdrawalAt: '2024-01-18T11:20:00',
  },
  {
    id: 'AFF003',
    referenceCode: 'PEDROOLIVEIRA',
    name: 'Pedro Oliveira',
    email: 'pedro.oliveira@empresa.com',
    phone: '(31) 97777-9012',
    document: '456.789.012-00',
    pixKey: { value: '45678901200', type: 'CPF' as PixKeyType },
    status: 'PENDING',
    commissionRate: 12,
    balance: {
      available: 0,
      pending: 1234.56,
      blocked: 0,
      totalEarned: 1234.56,
      totalWithdrawn: 0,
    },
    linkedEstablishments: 5,
    totalTransactions: 234,
    totalVolume: 123456.78,
    createdAt: '2024-01-10T08:00:00',
  },
  {
    id: 'AFF004',
    referenceCode: 'ANACOSTA2024',
    name: 'Ana Costa',
    email: 'ana.costa@parceiro.com',
    phone: '(41) 96666-3456',
    document: '321.654.987-00',
    pixKey: { value: 'ana.costa@parceiro.com', type: 'EMAIL' as PixKeyType },
    status: 'ACTIVE',
    commissionRate: 15,
    balance: {
      available: 12345.67,
      pending: 4567.89,
      blocked: 0,
      totalEarned: 234567.89,
      totalWithdrawn: 217654.33,
    },
    linkedEstablishments: 28,
    totalTransactions: 5678,
    totalVolume: 2345678.90,
    createdAt: '2023-09-05T11:45:00',
    updatedAt: '2024-01-24T16:10:00',
    lastWithdrawalAt: '2024-01-15T14:30:00',
  },
  {
    id: 'AFF005',
    referenceCode: 'CARLOSLIMA99',
    name: 'Carlos Lima',
    email: 'carlos.lima@parceiros.com',
    phone: '(51) 95555-7890',
    status: 'BLOCKED',
    commissionRate: 10,
    balance: {
      available: 0,
      pending: 0,
      blocked: 5678.90,
      totalEarned: 123456.78,
      totalWithdrawn: 117777.88,
    },
    linkedEstablishments: 0,
    totalTransactions: 3456,
    totalVolume: 1234567.89,
    createdAt: '2023-04-12T09:20:00',
    updatedAt: '2024-01-20T10:00:00',
  },
  {
    id: 'AFF006',
    referenceCode: 'FERNANDABR',
    name: 'Fernanda Braga',
    email: 'fernanda.braga@email.com',
    phone: '(61) 94444-1234',
    document: '654.321.098-00',
    pixKey: { value: 'b2c3d4e5-f6g7-h8i9-j0k1-l2m3n4o5p6q7', type: 'RANDOM' as PixKeyType },
    status: 'INACTIVE',
    commissionRate: 8,
    balance: {
      available: 234.56,
      pending: 0,
      blocked: 0,
      totalEarned: 34567.89,
      totalWithdrawn: 34333.33,
    },
    linkedEstablishments: 3,
    totalTransactions: 890,
    totalVolume: 345678.90,
    createdAt: '2023-11-08T15:30:00',
    updatedAt: '2024-01-10T08:45:00',
  },
];

// Mock commissions data
const mockCommissions: AffiliateCommission[] = [
  {
    id: 'COM001',
    affiliateId: 'AFF001',
    establishmentId: 'EST001',
    establishmentName: 'Loja Virtual ABC',
    transactionId: 'TXN-2024-001',
    transactionAmount: 1500.00,
    commissionRate: 10,
    commissionValue: 150.00,
    status: 'PAID',
    createdAt: '2024-01-25T14:30:00',
    paidAt: '2024-01-26T10:00:00',
  },
  {
    id: 'COM002',
    affiliateId: 'AFF001',
    establishmentId: 'EST002',
    establishmentName: 'E-commerce XYZ',
    transactionId: 'TXN-2024-002',
    transactionAmount: 2500.00,
    commissionRate: 10,
    commissionValue: 250.00,
    status: 'PENDING',
    createdAt: '2024-01-26T09:15:00',
  },
  {
    id: 'COM003',
    affiliateId: 'AFF001',
    establishmentId: 'EST003',
    establishmentName: 'Digital Store',
    transactionId: 'TXN-2024-003',
    transactionAmount: 890.00,
    commissionRate: 10,
    commissionValue: 89.00,
    status: 'PENDING',
    createdAt: '2024-01-26T11:45:00',
  },
];

// Mock establishments data
const mockEstablishments: AffiliateEstablishment[] = [
  {
    id: 'EST001',
    name: 'Loja Virtual ABC',
    cnpj: '12.345.678/0001-90',
    status: 'ACTIVE',
    linkedAt: '2023-07-15T10:30:00',
    totalTransactions: 3456,
    totalVolume: 1234567.89,
    totalCommissions: 123456.78,
  },
  {
    id: 'EST002',
    name: 'E-commerce XYZ',
    cnpj: '98.765.432/0001-10',
    status: 'ACTIVE',
    linkedAt: '2023-08-20T14:15:00',
    totalTransactions: 2345,
    totalVolume: 987654.32,
    totalCommissions: 98765.43,
  },
  {
    id: 'EST003',
    name: 'Digital Store',
    cnpj: '45.678.901/0001-23',
    status: 'ACTIVE',
    linkedAt: '2023-09-10T09:00:00',
    totalTransactions: 1234,
    totalVolume: 567890.12,
    totalCommissions: 56789.01,
  },
];

const AffiliatesPage: React.FC = () => {
  const [affiliates] = useState<Affiliate[]>(mockAffiliates);
  const [summary] = useState<AffiliateSummary>(mockSummary);
  const [loading, setLoading] = useState(false);
  const [selectedAffiliate, setSelectedAffiliate] = useState<Affiliate | null>(null);
  const [drawerOpen, setDrawerOpen] = useState(false);
  const [drawerTab, setDrawerTab] = useState('details');

  // Modals
  const [createModalOpen, setCreateModalOpen] = useState(false);
  const [editModalOpen, setEditModalOpen] = useState(false);
  const [blockModalOpen, setBlockModalOpen] = useState(false);
  const [commissionModalOpen, setCommissionModalOpen] = useState(false);
  const [withdrawModalOpen, setWithdrawModalOpen] = useState(false);

  // Forms
  const [createForm] = Form.useForm();
  const [editForm] = Form.useForm();
  const [blockForm] = Form.useForm();
  const [commissionForm] = Form.useForm();
  const [withdrawForm] = Form.useForm();

  // Filters
  const [searchText, setSearchText] = useState('');
  const [statusFilter, setStatusFilter] = useState<AffiliateStatus | undefined>();
  const [dateRange, setDateRange] = useState<[dayjs.Dayjs, dayjs.Dayjs] | null>(null);
  const [showAdvancedFilters, setShowAdvancedFilters] = useState(false);
  const [minCommissions, setMinCommissions] = useState<number | undefined>();
  const [maxCommissions, setMaxCommissions] = useState<number | undefined>();

  // Filtered affiliates
  const filteredAffiliates = useMemo(() => {
    return affiliates.filter((affiliate) => {
      if (searchText) {
        const search = searchText.toLowerCase();
        const matchId = affiliate.id.toLowerCase().includes(search);
        const matchName = affiliate.name.toLowerCase().includes(search);
        const matchEmail = affiliate.email.toLowerCase().includes(search);
        const matchCode = affiliate.referenceCode.toLowerCase().includes(search);
        const matchDocument = affiliate.document?.includes(search);
        if (!matchId && !matchName && !matchEmail && !matchCode && !matchDocument) {
          return false;
        }
      }
      if (statusFilter && affiliate.status !== statusFilter) return false;
      if (minCommissions && affiliate.balance.totalEarned < minCommissions) return false;
      if (maxCommissions && affiliate.balance.totalEarned > maxCommissions) return false;
      if (dateRange) {
        const affiliateDate = dayjs(affiliate.createdAt);
        if (affiliateDate.isBefore(dateRange[0], 'day') || affiliateDate.isAfter(dateRange[1], 'day')) {
          return false;
        }
      }
      return true;
    });
  }, [affiliates, searchText, statusFilter, minCommissions, maxCommissions, dateRange]);

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
    setDateRange(null);
    setMinCommissions(undefined);
    setMaxCommissions(undefined);
  };

  const handleViewAffiliate = (affiliate: Affiliate) => {
    setSelectedAffiliate(affiliate);
    setDrawerTab('details');
    setDrawerOpen(true);
  };

  const handleEditAffiliate = (affiliate: Affiliate) => {
    setSelectedAffiliate(affiliate);
    editForm.setFieldsValue({
      name: affiliate.name,
      email: affiliate.email,
      phone: affiliate.phone,
      commissionRate: affiliate.commissionRate,
      pixKeyType: affiliate.pixKey?.type,
      pixKeyValue: affiliate.pixKey?.value,
    });
    setEditModalOpen(true);
  };

  const handleBlockAffiliate = (affiliate: Affiliate) => {
    setSelectedAffiliate(affiliate);
    setBlockModalOpen(true);
  };

  const handleActivateAffiliate = (affiliate: Affiliate) => {
    Modal.confirm({
      title: 'Ativar Afiliado',
      content: `Deseja ativar o afiliado "${affiliate.name}"?`,
      okText: 'Sim, ativar',
      cancelText: 'Cancelar',
      onOk: () => {
        message.success('Afiliado ativado com sucesso');
      },
    });
  };

  const handleDeactivateAffiliate = (affiliate: Affiliate) => {
    Modal.confirm({
      title: 'Desativar Afiliado',
      content: `Deseja desativar o afiliado "${affiliate.name}"?`,
      okText: 'Sim, desativar',
      cancelText: 'Cancelar',
      onOk: () => {
        message.success('Afiliado desativado com sucesso');
      },
    });
  };

  const handleUnblockAffiliate = (affiliate: Affiliate) => {
    Modal.confirm({
      title: 'Desbloquear Afiliado',
      content: `Deseja desbloquear o afiliado "${affiliate.name}"?`,
      okText: 'Sim, desbloquear',
      cancelText: 'Cancelar',
      onOk: () => {
        message.success('Afiliado desbloqueado com sucesso');
      },
    });
  };

  const handleChangeCommission = (affiliate: Affiliate) => {
    setSelectedAffiliate(affiliate);
    commissionForm.setFieldsValue({
      commissionRate: affiliate.commissionRate,
    });
    setCommissionModalOpen(true);
  };

  const handleWithdraw = (affiliate: Affiliate) => {
    setSelectedAffiliate(affiliate);
    withdrawForm.setFieldsValue({
      amount: affiliate.balance.available,
    });
    setWithdrawModalOpen(true);
  };

  const handleCopyReferralLink = (affiliate: Affiliate) => {
    const link = `https://zendapag.com/ref/${affiliate.referenceCode}`;
    navigator.clipboard.writeText(link);
    message.success('Link de indicação copiado!');
  };

  const handleExport = () => {
    message.success('Exportação iniciada. O arquivo será baixado em instantes.');
  };

  const onCreateSubmit = async (values: any) => {
    message.success('Afiliado criado com sucesso');
    setCreateModalOpen(false);
    createForm.resetFields();
  };

  const onEditSubmit = async (values: any) => {
    message.success('Afiliado atualizado com sucesso');
    setEditModalOpen(false);
    editForm.resetFields();
    setSelectedAffiliate(null);
  };

  const onBlockSubmit = async (values: { reason: string }) => {
    message.success('Afiliado bloqueado com sucesso');
    setBlockModalOpen(false);
    blockForm.resetFields();
    setSelectedAffiliate(null);
  };

  const onCommissionSubmit = async (values: { commissionRate: number }) => {
    message.success('Taxa de comissão atualizada com sucesso');
    setCommissionModalOpen(false);
    commissionForm.resetFields();
    setSelectedAffiliate(null);
  };

  const onWithdrawSubmit = async (values: { amount: number }) => {
    message.success('Saque solicitado com sucesso');
    setWithdrawModalOpen(false);
    withdrawForm.resetFields();
    setSelectedAffiliate(null);
  };

  const getActionMenuItems = (affiliate: Affiliate) => {
    const items: any[] = [
      {
        key: 'view',
        icon: <EyeOutlined />,
        label: 'Ver detalhes',
        onClick: () => handleViewAffiliate(affiliate),
      },
      {
        key: 'edit',
        icon: <EditOutlined />,
        label: 'Editar',
        onClick: () => handleEditAffiliate(affiliate),
      },
      {
        key: 'commission',
        icon: <PercentageOutlined />,
        label: 'Alterar comissão',
        onClick: () => handleChangeCommission(affiliate),
      },
      {
        key: 'copy-link',
        icon: <LinkOutlined />,
        label: 'Copiar link de indicação',
        onClick: () => handleCopyReferralLink(affiliate),
      },
    ];

    if (affiliate.status === 'ACTIVE') {
      if (affiliate.balance.available > 0) {
        items.push({
          key: 'withdraw',
          icon: <WalletOutlined />,
          label: 'Solicitar saque',
          onClick: () => handleWithdraw(affiliate),
        });
      }
      items.push({
        key: 'deactivate',
        icon: <StopOutlined />,
        label: 'Desativar',
        onClick: () => handleDeactivateAffiliate(affiliate),
      });
      items.push({
        key: 'block',
        icon: <CloseCircleOutlined />,
        label: 'Bloquear',
        onClick: () => handleBlockAffiliate(affiliate),
        danger: true,
      });
    }

    if (affiliate.status === 'INACTIVE') {
      items.push({
        key: 'activate',
        icon: <CheckCircleOutlined />,
        label: 'Ativar',
        onClick: () => handleActivateAffiliate(affiliate),
      });
    }

    if (affiliate.status === 'PENDING') {
      items.push({
        key: 'approve',
        icon: <CheckCircleOutlined />,
        label: 'Aprovar',
        onClick: () => handleActivateAffiliate(affiliate),
      });
    }

    if (affiliate.status === 'BLOCKED') {
      items.push({
        key: 'unblock',
        icon: <CheckCircleOutlined />,
        label: 'Desbloquear',
        onClick: () => handleUnblockAffiliate(affiliate),
      });
    }

    return items;
  };

  const columns: ColumnsType<Affiliate> = [
    {
      title: 'Afiliado',
      key: 'affiliate',
      width: 280,
      render: (_: any, record: Affiliate) => (
        <Space>
          <Avatar icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }}>
            {record.name.charAt(0)}
          </Avatar>
          <div>
            <Text strong>{record.name}</Text>
            <div>
              <Text type="secondary" style={{ fontSize: 12 }}>{record.email}</Text>
            </div>
          </div>
        </Space>
      ),
    },
    {
      title: 'Código',
      dataIndex: 'referenceCode',
      key: 'referenceCode',
      width: 150,
      render: (code: string) => (
        <Space>
          <Text code style={{ fontSize: 11 }}>{code}</Text>
          <CopyButton text={code} />
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 110,
      render: (status: AffiliateStatus) => (
        <Tag color={AFFILIATE_STATUS_COLORS[status]}>
          {AFFILIATE_STATUS_LABELS[status]}
        </Tag>
      ),
      filters: [
        { text: 'Ativo', value: 'ACTIVE' },
        { text: 'Inativo', value: 'INACTIVE' },
        { text: 'Bloqueado', value: 'BLOCKED' },
        { text: 'Pendente', value: 'PENDING' },
      ],
      onFilter: (value, record) => record.status === value,
    },
    {
      title: 'Comissão',
      dataIndex: 'commissionRate',
      key: 'commissionRate',
      width: 100,
      align: 'center',
      render: (rate: number) => (
        <Tag color="blue">{rate}%</Tag>
      ),
      sorter: (a, b) => a.commissionRate - b.commissionRate,
    },
    {
      title: 'Estabelecimentos',
      dataIndex: 'linkedEstablishments',
      key: 'linkedEstablishments',
      width: 130,
      align: 'center',
      render: (count: number) => (
        <Space>
          <ShopOutlined />
          <Text>{count}</Text>
        </Space>
      ),
      sorter: (a, b) => a.linkedEstablishments - b.linkedEstablishments,
    },
    {
      title: 'Saldo Disponível',
      key: 'available',
      width: 150,
      align: 'right',
      render: (_: any, record: Affiliate) => (
        <Text strong style={{ color: '#52c41a' }}>
          {formatCurrency(record.balance.available)}
        </Text>
      ),
      sorter: (a, b) => a.balance.available - b.balance.available,
    },
    {
      title: 'Total Ganho',
      key: 'totalEarned',
      width: 150,
      align: 'right',
      render: (_: any, record: Affiliate) => (
        <Text strong style={{ color: '#1890ff' }}>
          {formatCurrency(record.balance.totalEarned)}
        </Text>
      ),
      sorter: (a, b) => a.balance.totalEarned - b.balance.totalEarned,
    },
    {
      title: 'Cadastro',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (date: string) => dayjs(date).format('DD/MM/YYYY'),
      sorter: (a, b) => dayjs(a.createdAt).unix() - dayjs(b.createdAt).unix(),
    },
    {
      title: 'Ações',
      key: 'actions',
      width: 80,
      fixed: 'right',
      render: (_: any, record: Affiliate) => (
        <ActionMenu items={getActionMenuItems(record)} />
      ),
    },
  ];

  // Commission columns for drawer
  const commissionColumns: ColumnsType<AffiliateCommission> = [
    {
      title: 'Estabelecimento',
      dataIndex: 'establishmentName',
      key: 'establishmentName',
      ellipsis: true,
    },
    {
      title: 'Valor Transação',
      dataIndex: 'transactionAmount',
      key: 'transactionAmount',
      align: 'right',
      render: (value: number) => formatCurrency(value),
    },
    {
      title: 'Comissão',
      dataIndex: 'commissionValue',
      key: 'commissionValue',
      align: 'right',
      render: (value: number) => (
        <Text strong style={{ color: '#52c41a' }}>{formatCurrency(value)}</Text>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      render: (status: 'PENDING' | 'PAID' | 'CANCELLED') => (
        <Tag color={COMMISSION_STATUS_COLORS[status]}>
          {COMMISSION_STATUS_LABELS[status]}
        </Tag>
      ),
    },
    {
      title: 'Data',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (date: string) => dayjs(date).format('DD/MM/YY HH:mm'),
    },
  ];

  // Establishment columns for drawer
  const establishmentColumns: ColumnsType<AffiliateEstablishment> = [
    {
      title: 'Estabelecimento',
      dataIndex: 'name',
      key: 'name',
      render: (name: string, record: AffiliateEstablishment) => (
        <div>
          <Text strong>{name}</Text>
          <div>
            <Text type="secondary" style={{ fontSize: 11 }}>{record.cnpj}</Text>
          </div>
        </div>
      ),
    },
    {
      title: 'Volume',
      dataIndex: 'totalVolume',
      key: 'totalVolume',
      align: 'right',
      render: (value: number) => formatCurrency(value),
    },
    {
      title: 'Comissões',
      dataIndex: 'totalCommissions',
      key: 'totalCommissions',
      align: 'right',
      render: (value: number) => (
        <Text strong style={{ color: '#52c41a' }}>{formatCurrency(value)}</Text>
      ),
    },
    {
      title: 'Vinculado em',
      dataIndex: 'linkedAt',
      key: 'linkedAt',
      render: (date: string) => dayjs(date).format('DD/MM/YYYY'),
    },
  ];

  return (
    <div style={{ padding: '0 0 24px 0' }}>
      {/* Header */}
      <div style={{ marginBottom: 24, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <div>
          <Title level={4} style={{ margin: 0 }}>
            <TeamOutlined style={{ marginRight: 8 }} />
            Afiliados
          </Title>
          <Text type="secondary">
            Gestão de afiliados e comissões
          </Text>
        </div>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => setCreateModalOpen(true)}
        >
          Novo Afiliado
        </Button>
      </div>

      {/* Summary Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Total de Afiliados"
              value={summary.totalAffiliates}
              prefix={<TeamOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                {summary.activeAffiliates} ativos
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Comissões Pagas"
              value={summary.totalCommissionsPaid}
              prefix={<DollarOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                Pendentes: {formatCurrency(summary.totalCommissionsPending)}
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Estabelecimentos Vinculados"
              value={summary.totalLinkedEstablishments}
              prefix={<ShopOutlined style={{ color: '#722ed1' }} />}
              valueStyle={{ color: '#722ed1' }}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                Via programa de afiliados
              </Text>
            </div>
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card bordered={false} style={{ height: '100%' }}>
            <Statistic
              title="Volume Gerado"
              value={summary.totalVolumeGenerated}
              prefix={<BankOutlined style={{ color: '#13c2c2' }} />}
              valueStyle={{ color: '#13c2c2' }}
              precision={2}
              formatter={(value) => formatCurrency(Number(value))}
            />
            <div style={{ marginTop: 8 }}>
              <Text type="secondary">
                Por indicações
              </Text>
            </div>
          </Card>
        </Col>
      </Row>

      {/* Status Cards */}
      <Row gutter={[16, 16]} style={{ marginBottom: 24 }}>
        <Col xs={24} lg={16}>
          <Card title="Distribuição por Status" bordered={false} size="small">
            <Row gutter={[8, 8]}>
              <Col span={6}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#f6ffed' }}>
                  <Statistic
                    title="Ativos"
                    value={summary.byStatus.active}
                    valueStyle={{ fontSize: 20, color: '#52c41a' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#f5f5f5' }}>
                  <Statistic
                    title="Inativos"
                    value={summary.byStatus.inactive}
                    valueStyle={{ fontSize: 20, color: '#8c8c8c' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#fff2f0' }}>
                  <Statistic
                    title="Bloqueados"
                    value={summary.byStatus.blocked}
                    valueStyle={{ fontSize: 20, color: '#ff4d4f' }}
                  />
                </Card>
              </Col>
              <Col span={6}>
                <Card bordered size="small" style={{ textAlign: 'center', background: '#fffbe6' }}>
                  <Statistic
                    title="Pendentes"
                    value={summary.byStatus.pending}
                    valueStyle={{ fontSize: 20, color: '#faad14' }}
                  />
                </Card>
              </Col>
            </Row>
          </Card>
        </Col>
        <Col xs={24} lg={8}>
          <Card title="Top 5 Afiliados" bordered={false} size="small" style={{ height: '100%' }}>
            <div style={{ maxHeight: 150, overflow: 'auto' }}>
              {summary.topAffiliates.slice(0, 5).map((aff, index) => (
                <div
                  key={aff.id}
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    padding: '6px 0',
                    borderBottom: index < 4 ? '1px solid #f0f0f0' : 'none',
                  }}
                >
                  <Space size={8}>
                    <Badge
                      count={index + 1}
                      style={{
                        backgroundColor: index === 0 ? '#faad14' : index === 1 ? '#8c8c8c' : index === 2 ? '#cd7f32' : '#d9d9d9',
                      }}
                    />
                    <Text ellipsis style={{ maxWidth: 100 }}>{aff.name}</Text>
                  </Space>
                  <Text strong style={{ color: '#52c41a', fontSize: 12 }}>
                    {formatCurrency(aff.commissions)}
                  </Text>
                </div>
              ))}
            </div>
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card bordered={false} style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]} align="middle">
          <Col flex="auto">
            <Space wrap size="middle">
              <Input
                placeholder="Buscar por nome, email, código, CPF..."
                prefix={<SearchOutlined />}
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                style={{ width: 350 }}
                allowClear
              />
              <Select
                placeholder="Status"
                value={statusFilter}
                onChange={setStatusFilter}
                style={{ width: 140 }}
                allowClear
                options={[
                  { label: 'Ativo', value: 'ACTIVE' },
                  { label: 'Inativo', value: 'INACTIVE' },
                  { label: 'Bloqueado', value: 'BLOCKED' },
                  { label: 'Pendente', value: 'PENDING' },
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
              {(searchText || statusFilter || dateRange || minCommissions || maxCommissions) && (
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
              <Col xs={12} sm={6} md={4}>
                <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                  Comissões mín.
                </Text>
                <Input
                  type="number"
                  placeholder="R$ 0,00"
                  value={minCommissions}
                  onChange={(e) => setMinCommissions(e.target.value ? Number(e.target.value) : undefined)}
                  prefix="R$"
                />
              </Col>
              <Col xs={12} sm={6} md={4}>
                <Text type="secondary" style={{ display: 'block', marginBottom: 4 }}>
                  Comissões máx.
                </Text>
                <Input
                  type="number"
                  placeholder="R$ 999.999"
                  value={maxCommissions}
                  onChange={(e) => setMaxCommissions(e.target.value ? Number(e.target.value) : undefined)}
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
          Exibindo {filteredAffiliates.length} de {affiliates.length} afiliados
        </Text>
      </div>

      {/* Table */}
      <Card bordered={false}>
        <Table
          columns={columns}
          dataSource={filteredAffiliates}
          rowKey="id"
          loading={loading}
          scroll={{ x: 1400 }}
          pagination={{
            total: filteredAffiliates.length,
            pageSize: 20,
            showSizeChanger: true,
            showTotal: (total, range) => `${range[0]}-${range[1]} de ${total} afiliados`,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
        />
      </Card>

      {/* Detail Drawer */}
      <Drawer
        title={
          <Space>
            <UserOutlined />
            <span>Detalhes do Afiliado</span>
            {selectedAffiliate && (
              <Tag color={AFFILIATE_STATUS_COLORS[selectedAffiliate.status]}>
                {AFFILIATE_STATUS_LABELS[selectedAffiliate.status]}
              </Tag>
            )}
          </Space>
        }
        placement="right"
        width={700}
        open={drawerOpen}
        onClose={() => {
          setDrawerOpen(false);
          setSelectedAffiliate(null);
        }}
      >
        {selectedAffiliate && (
          <Tabs
            activeKey={drawerTab}
            onChange={setDrawerTab}
            items={[
              {
                key: 'details',
                label: 'Dados',
                children: (
                  <div>
                    <div style={{ textAlign: 'center', marginBottom: 24 }}>
                      <Avatar size={80} icon={<UserOutlined />} style={{ backgroundColor: '#1890ff' }}>
                        {selectedAffiliate.name.charAt(0)}
                      </Avatar>
                      <div style={{ marginTop: 12 }}>
                        <Title level={5} style={{ margin: 0 }}>{selectedAffiliate.name}</Title>
                        <Text type="secondary">{selectedAffiliate.email}</Text>
                      </div>
                    </div>

                    <Descriptions column={2} bordered size="small">
                      <Descriptions.Item label="ID">
                        <Space>
                          <Text code>{selectedAffiliate.id}</Text>
                          <CopyButton text={selectedAffiliate.id} />
                        </Space>
                      </Descriptions.Item>
                      <Descriptions.Item label="Código de Referência">
                        <Space>
                          <Text code>{selectedAffiliate.referenceCode}</Text>
                          <CopyButton text={selectedAffiliate.referenceCode} />
                        </Space>
                      </Descriptions.Item>
                      <Descriptions.Item label="Telefone">
                        {selectedAffiliate.phone || '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="CPF/CNPJ">
                        {selectedAffiliate.document || '-'}
                      </Descriptions.Item>
                      <Descriptions.Item label="Taxa de Comissão">
                        <Tag color="blue">{selectedAffiliate.commissionRate}%</Tag>
                      </Descriptions.Item>
                      <Descriptions.Item label="Estabelecimentos">
                        {selectedAffiliate.linkedEstablishments}
                      </Descriptions.Item>
                      {selectedAffiliate.pixKey && (
                        <>
                          <Descriptions.Item label="Tipo Chave PIX">
                            {PIX_KEY_TYPE_LABELS[selectedAffiliate.pixKey.type]}
                          </Descriptions.Item>
                          <Descriptions.Item label="Chave PIX">
                            <Space>
                              <Text ellipsis style={{ maxWidth: 150 }}>{selectedAffiliate.pixKey.value}</Text>
                              <CopyButton text={selectedAffiliate.pixKey.value} />
                            </Space>
                          </Descriptions.Item>
                        </>
                      )}
                    </Descriptions>

                    <Divider />

                    <Title level={5}>Saldos</Title>
                    <Row gutter={[16, 16]}>
                      <Col span={8}>
                        <Card size="small" style={{ textAlign: 'center', background: '#f6ffed' }}>
                          <Statistic
                            title="Disponível"
                            value={selectedAffiliate.balance.available}
                            valueStyle={{ color: '#52c41a', fontSize: 18 }}
                            precision={2}
                            prefix="R$"
                          />
                        </Card>
                      </Col>
                      <Col span={8}>
                        <Card size="small" style={{ textAlign: 'center', background: '#fffbe6' }}>
                          <Statistic
                            title="Pendente"
                            value={selectedAffiliate.balance.pending}
                            valueStyle={{ color: '#faad14', fontSize: 18 }}
                            precision={2}
                            prefix="R$"
                          />
                        </Card>
                      </Col>
                      <Col span={8}>
                        <Card size="small" style={{ textAlign: 'center', background: '#fff2f0' }}>
                          <Statistic
                            title="Bloqueado"
                            value={selectedAffiliate.balance.blocked}
                            valueStyle={{ color: '#ff4d4f', fontSize: 18 }}
                            precision={2}
                            prefix="R$"
                          />
                        </Card>
                      </Col>
                    </Row>
                    <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
                      <Col span={12}>
                        <Card size="small" style={{ textAlign: 'center' }}>
                          <Statistic
                            title="Total Ganho"
                            value={selectedAffiliate.balance.totalEarned}
                            valueStyle={{ color: '#1890ff', fontSize: 18 }}
                            precision={2}
                            prefix="R$"
                          />
                        </Card>
                      </Col>
                      <Col span={12}>
                        <Card size="small" style={{ textAlign: 'center' }}>
                          <Statistic
                            title="Total Sacado"
                            value={selectedAffiliate.balance.totalWithdrawn}
                            valueStyle={{ fontSize: 18 }}
                            precision={2}
                            prefix="R$"
                          />
                        </Card>
                      </Col>
                    </Row>

                    <Divider />

                    <Descriptions title="Datas" column={1} bordered size="small">
                      <Descriptions.Item label="Cadastro">
                        {formatDate(selectedAffiliate.createdAt)}
                      </Descriptions.Item>
                      {selectedAffiliate.updatedAt && (
                        <Descriptions.Item label="Última atualização">
                          {formatDate(selectedAffiliate.updatedAt)}
                        </Descriptions.Item>
                      )}
                      {selectedAffiliate.lastWithdrawalAt && (
                        <Descriptions.Item label="Último saque">
                          {formatDate(selectedAffiliate.lastWithdrawalAt)}
                        </Descriptions.Item>
                      )}
                    </Descriptions>

                    <Divider />

                    <Space wrap>
                      <Button
                        icon={<LinkOutlined />}
                        onClick={() => handleCopyReferralLink(selectedAffiliate)}
                      >
                        Copiar Link
                      </Button>
                      <Button
                        icon={<EditOutlined />}
                        onClick={() => {
                          setDrawerOpen(false);
                          handleEditAffiliate(selectedAffiliate);
                        }}
                      >
                        Editar
                      </Button>
                      {selectedAffiliate.status === 'ACTIVE' && selectedAffiliate.balance.available > 0 && (
                        <Button
                          type="primary"
                          icon={<WalletOutlined />}
                          onClick={() => {
                            setDrawerOpen(false);
                            handleWithdraw(selectedAffiliate);
                          }}
                        >
                          Solicitar Saque
                        </Button>
                      )}
                    </Space>
                  </div>
                ),
              },
              {
                key: 'commissions',
                label: 'Comissões',
                children: (
                  <Table
                    columns={commissionColumns}
                    dataSource={mockCommissions}
                    rowKey="id"
                    size="small"
                    pagination={{ pageSize: 10 }}
                  />
                ),
              },
              {
                key: 'establishments',
                label: 'Estabelecimentos',
                children: (
                  <Table
                    columns={establishmentColumns}
                    dataSource={mockEstablishments}
                    rowKey="id"
                    size="small"
                    pagination={{ pageSize: 10 }}
                  />
                ),
              },
            ]}
          />
        )}
      </Drawer>

      {/* Create Modal */}
      <Modal
        title={
          <Space>
            <PlusOutlined />
            <span>Novo Afiliado</span>
          </Space>
        }
        open={createModalOpen}
        onCancel={() => {
          setCreateModalOpen(false);
          createForm.resetFields();
        }}
        footer={null}
        width={600}
      >
        <Form form={createForm} layout="vertical" onFinish={onCreateSubmit}>
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                name="name"
                label="Nome completo"
                rules={[{ required: true, message: 'Informe o nome' }]}
              >
                <Input placeholder="Nome do afiliado" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="email"
                label="E-mail"
                rules={[
                  { required: true, message: 'Informe o e-mail' },
                  { type: 'email', message: 'E-mail inválido' },
                ]}
              >
                <Input placeholder="email@exemplo.com" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="phone"
                label="Telefone"
              >
                <Input placeholder="(00) 00000-0000" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="document"
                label="CPF/CNPJ"
              >
                <Input placeholder="000.000.000-00" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="commissionRate"
                label="Taxa de comissão (%)"
                rules={[{ required: true, message: 'Informe a taxa' }]}
                initialValue={10}
              >
                <InputNumber
                  min={0}
                  max={100}
                  style={{ width: '100%' }}
                  addonAfter="%"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="pixKeyType"
                label="Tipo de Chave PIX"
              >
                <Select
                  placeholder="Selecione"
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
              >
                <Input placeholder="Chave PIX" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setCreateModalOpen(false)}>
                Cancelar
              </Button>
              <Button type="primary" htmlType="submit">
                Criar Afiliado
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Modal */}
      <Modal
        title={
          <Space>
            <EditOutlined />
            <span>Editar Afiliado</span>
          </Space>
        }
        open={editModalOpen}
        onCancel={() => {
          setEditModalOpen(false);
          editForm.resetFields();
          setSelectedAffiliate(null);
        }}
        footer={null}
        width={600}
      >
        <Form form={editForm} layout="vertical" onFinish={onEditSubmit}>
          <Row gutter={16}>
            <Col span={24}>
              <Form.Item
                name="name"
                label="Nome completo"
                rules={[{ required: true, message: 'Informe o nome' }]}
              >
                <Input placeholder="Nome do afiliado" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="email"
                label="E-mail"
                rules={[
                  { required: true, message: 'Informe o e-mail' },
                  { type: 'email', message: 'E-mail inválido' },
                ]}
              >
                <Input placeholder="email@exemplo.com" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="phone"
                label="Telefone"
              >
                <Input placeholder="(00) 00000-0000" />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="commissionRate"
                label="Taxa de comissão (%)"
                rules={[{ required: true, message: 'Informe a taxa' }]}
              >
                <InputNumber
                  min={0}
                  max={100}
                  style={{ width: '100%' }}
                  addonAfter="%"
                />
              </Form.Item>
            </Col>
            <Col span={12}>
              <Form.Item
                name="pixKeyType"
                label="Tipo de Chave PIX"
              >
                <Select
                  placeholder="Selecione"
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
              >
                <Input placeholder="Chave PIX" />
              </Form.Item>
            </Col>
          </Row>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setEditModalOpen(false)}>
                Cancelar
              </Button>
              <Button type="primary" htmlType="submit">
                Salvar Alterações
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Block Modal */}
      <Modal
        title={
          <Space>
            <CloseCircleOutlined style={{ color: '#ff4d4f' }} />
            <span>Bloquear Afiliado</span>
          </Space>
        }
        open={blockModalOpen}
        onCancel={() => {
          setBlockModalOpen(false);
          blockForm.resetFields();
          setSelectedAffiliate(null);
        }}
        footer={null}
      >
        <Form form={blockForm} layout="vertical" onFinish={onBlockSubmit}>
          {selectedAffiliate && (
            <div style={{ marginBottom: 16 }}>
              <Card size="small" style={{ background: '#fafafa' }}>
                <Space>
                  <Avatar icon={<UserOutlined />} />
                  <div>
                    <Text strong>{selectedAffiliate.name}</Text>
                    <div>
                      <Text type="secondary" style={{ fontSize: 12 }}>{selectedAffiliate.email}</Text>
                    </div>
                  </div>
                </Space>
              </Card>
            </div>
          )}

          <Form.Item
            name="reason"
            label="Motivo do bloqueio"
            rules={[{ required: true, message: 'Informe o motivo do bloqueio' }]}
          >
            <TextArea
              rows={4}
              placeholder="Descreva o motivo do bloqueio..."
              maxLength={500}
              showCount
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setBlockModalOpen(false)}>
                Cancelar
              </Button>
              <Button type="primary" danger htmlType="submit">
                Bloquear Afiliado
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Commission Rate Modal */}
      <Modal
        title={
          <Space>
            <PercentageOutlined />
            <span>Alterar Taxa de Comissão</span>
          </Space>
        }
        open={commissionModalOpen}
        onCancel={() => {
          setCommissionModalOpen(false);
          commissionForm.resetFields();
          setSelectedAffiliate(null);
        }}
        footer={null}
      >
        <Form form={commissionForm} layout="vertical" onFinish={onCommissionSubmit}>
          {selectedAffiliate && (
            <div style={{ marginBottom: 16 }}>
              <Card size="small" style={{ background: '#fafafa' }}>
                <Row gutter={16}>
                  <Col span={12}>
                    <Text type="secondary">Afiliado:</Text>
                    <div><Text strong>{selectedAffiliate.name}</Text></div>
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">Taxa atual:</Text>
                    <div>
                      <Tag color="blue">{selectedAffiliate.commissionRate}%</Tag>
                    </div>
                  </Col>
                </Row>
              </Card>
            </div>
          )}

          <Form.Item
            name="commissionRate"
            label="Nova taxa de comissão (%)"
            rules={[{ required: true, message: 'Informe a nova taxa' }]}
          >
            <InputNumber
              min={0}
              max={100}
              style={{ width: '100%' }}
              addonAfter="%"
              placeholder="Ex: 10"
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setCommissionModalOpen(false)}>
                Cancelar
              </Button>
              <Button type="primary" htmlType="submit">
                Atualizar Taxa
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>

      {/* Withdraw Modal */}
      <Modal
        title={
          <Space>
            <WalletOutlined />
            <span>Solicitar Saque</span>
          </Space>
        }
        open={withdrawModalOpen}
        onCancel={() => {
          setWithdrawModalOpen(false);
          withdrawForm.resetFields();
          setSelectedAffiliate(null);
        }}
        footer={null}
      >
        <Form form={withdrawForm} layout="vertical" onFinish={onWithdrawSubmit}>
          {selectedAffiliate && (
            <div style={{ marginBottom: 16 }}>
              <Card size="small" style={{ background: '#f6ffed' }}>
                <Row gutter={16}>
                  <Col span={12}>
                    <Text type="secondary">Afiliado:</Text>
                    <div><Text strong>{selectedAffiliate.name}</Text></div>
                  </Col>
                  <Col span={12}>
                    <Text type="secondary">Saldo disponível:</Text>
                    <div>
                      <Text strong style={{ color: '#52c41a', fontSize: 18 }}>
                        {formatCurrency(selectedAffiliate.balance.available)}
                      </Text>
                    </div>
                  </Col>
                </Row>
                {selectedAffiliate.pixKey && (
                  <div style={{ marginTop: 8 }}>
                    <Text type="secondary">Chave PIX: </Text>
                    <Text>{selectedAffiliate.pixKey.value}</Text>
                  </div>
                )}
              </Card>
            </div>
          )}

          <Form.Item
            name="amount"
            label="Valor do saque"
            rules={[
              { required: true, message: 'Informe o valor' },
              {
                validator: (_, value) => {
                  if (selectedAffiliate && value > selectedAffiliate.balance.available) {
                    return Promise.reject('Valor maior que o saldo disponível');
                  }
                  return Promise.resolve();
                },
              },
            ]}
          >
            <InputNumber
              min={0.01}
              max={selectedAffiliate?.balance.available}
              style={{ width: '100%' }}
              precision={2}
              formatter={(value) => `R$ ${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.')}
              parser={(value) => value?.replace(/R\$\s?|(\.)/g, '').replace(',', '.') as any}
            />
          </Form.Item>

          <Form.Item style={{ marginBottom: 0, textAlign: 'right' }}>
            <Space>
              <Button onClick={() => setWithdrawModalOpen(false)}>
                Cancelar
              </Button>
              <Button type="primary" htmlType="submit">
                Confirmar Saque
              </Button>
            </Space>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default AffiliatesPage;
