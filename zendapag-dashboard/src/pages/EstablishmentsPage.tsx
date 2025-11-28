// @ts-nocheck
import React, { useEffect, useState, useCallback } from 'react';
import {
  Table,
  Card,
  Tag,
  Space,
  Button,
  Input,
  Select,
  DatePicker,
  Typography,
  message,
  Modal,
  Row,
  Col,
  Statistic,
  Tooltip,
  InputNumber,
  Form,
} from 'antd';
import {
  SearchOutlined,
  ShopOutlined,
  ExportOutlined,
  ReloadOutlined,
  FilterOutlined,
  ClearOutlined,
  EyeOutlined,
  EditOutlined,
  MailOutlined,
  UserAddOutlined,
  LinkOutlined,
  DollarOutlined,
  PercentageOutlined,
  ClockCircleOutlined,
  HistoryOutlined,
  CheckCircleOutlined,
  StopOutlined,
  CloseCircleOutlined,
  MoreOutlined,
} from '@ant-design/icons';
import type { ColumnsType } from 'antd/es/table';
import { ActionMenu, CopyButton } from '@/components/shared';
import { EstablishmentForm } from '@/components/establishments';
import establishmentService from '@/services/establishmentService';
import {
  ESTABLISHMENT_STATUS_COLORS,
  ESTABLISHMENT_STATUS_LABELS,
  DOCUMENT_STATUS_COLORS,
  DOCUMENT_STATUS_LABELS,
} from '@/utils/constants';
import { formatCurrency, formatDate, formatCNPJ } from '@/utils/helpers';
import type {
  Establishment,
  EstablishmentStatus,
  DocumentStatus,
  EstablishmentSummary,
  PaginatedResponse,
} from '@/types';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

// Mock data for development
const MOCK_ESTABLISHMENTS: Establishment[] = [
  {
    id: '1',
    companyName: 'Empresa Exemplo LTDA',
    tradeName: 'Empresa Exemplo',
    cnpj: '12345678000190',
    email: 'contato@empresa.com',
    phone: '11999999999',
    status: 'ACTIVE',
    documentStatus: 'APPROVED',
    createdAt: '2024-01-15T10:30:00Z',
    updatedAt: '2024-01-15T10:30:00Z',
    balances: {
      available: 15000.00,
      blocked: 2500.00,
      retained: 500.00,
      pending: 1000.00,
      pix: { balance: 10000, retained: 300 },
      card: { balance: 4000, retained: 150, pending: 800 },
      boleto: { balance: 1000, pending: 200 },
      total: 19000.00,
    },
    fees: {
      pix: { type: 'FIXED', value: 1.99 },
      boleto: { type: 'FIXED', value: 2.99 },
      card: {
        type: 'PERCENTAGE',
        installments: { 1: 3.51, 2: 4.99, 3: 5.88, 4: 6.78, 5: 7.68, 6: 8.58, 7: 9.82, 8: 10.71, 9: 11.61, 10: 12.50, 11: 13.39, 12: 14.29 },
      },
    },
    withdrawal: { type: 'MANUAL', automaticFee: 0, limit: 50000 },
    acquirers: {
      pix: { transactions: { provider: 'PAGNET', displayName: 'Pagnet' }, withdrawals: { provider: 'PAGNET_2', displayName: 'Pagnet 2' } },
      card: { transactions: { provider: 'PAGARME', displayName: 'Pagarme' } },
      boleto: { transactions: { provider: 'PAGNET', displayName: 'Pagnet' } },
    },
  },
  {
    id: '2',
    companyName: 'Loja Virtual S.A.',
    tradeName: 'Loja Virtual',
    cnpj: '98765432000155',
    email: 'financeiro@lojavirtual.com',
    phone: '21988888888',
    status: 'PENDING',
    documentStatus: 'PENDING',
    createdAt: '2024-02-20T14:15:00Z',
    updatedAt: '2024-02-20T14:15:00Z',
    balances: {
      available: 0,
      blocked: 0,
      retained: 0,
      pending: 0,
      pix: { balance: 0, retained: 0 },
      card: { balance: 0, retained: 0, pending: 0 },
      boleto: { balance: 0, pending: 0 },
      total: 0,
    },
    fees: {
      pix: { type: 'FIXED', value: 1.99 },
      boleto: { type: 'FIXED', value: 2.99 },
      card: {
        type: 'PERCENTAGE',
        installments: { 1: 3.51, 2: 4.99, 3: 5.88, 4: 6.78, 5: 7.68, 6: 8.58, 7: 9.82, 8: 10.71, 9: 11.61, 10: 12.50, 11: 13.39, 12: 14.29 },
      },
    },
    withdrawal: { type: 'MANUAL', automaticFee: 0, limit: 50000 },
    acquirers: {
      pix: { transactions: { provider: 'PAGNET', displayName: 'Pagnet' }, withdrawals: { provider: 'PAGNET', displayName: 'Pagnet' } },
      card: { transactions: { provider: 'PAGARME', displayName: 'Pagarme' } },
      boleto: { transactions: { provider: 'PAGNET', displayName: 'Pagnet' } },
    },
  },
  {
    id: '3',
    companyName: 'Tech Solutions ME',
    tradeName: 'Tech Solutions',
    cnpj: '11223344000166',
    email: 'tech@solutions.com',
    status: 'BLOCKED',
    documentStatus: 'REJECTED',
    createdAt: '2024-03-10T09:00:00Z',
    updatedAt: '2024-03-15T16:30:00Z',
    balances: {
      available: 5000.00,
      blocked: 5000.00,
      retained: 0,
      pending: 0,
      pix: { balance: 5000, retained: 0 },
      card: { balance: 0, retained: 0, pending: 0 },
      boleto: { balance: 0, pending: 0 },
      total: 10000.00,
    },
    fees: {
      pix: { type: 'PERCENTAGE', value: 1.5 },
      boleto: { type: 'FIXED', value: 3.99 },
      card: {
        type: 'PERCENTAGE',
        installments: { 1: 4.0, 2: 5.5, 3: 6.5, 4: 7.5, 5: 8.5, 6: 9.5, 7: 10.5, 8: 11.5, 9: 12.5, 10: 13.5, 11: 14.5, 12: 15.5 },
      },
    },
    withdrawal: { type: 'AUTOMATIC', automaticFee: 2.99, limit: 100000 },
    acquirers: {
      pix: { transactions: { provider: 'PAGNET_2', displayName: 'Pagnet 2' }, withdrawals: { provider: 'PAGNET_2', displayName: 'Pagnet 2' } },
      card: { transactions: { provider: 'PAGARME', displayName: 'Pagarme' } },
      boleto: { transactions: { provider: 'PAGNET', displayName: 'Pagnet' } },
    },
  },
];

const MOCK_SUMMARY: EstablishmentSummary = {
  totalEstablishments: 156,
  totalAvailableBalance: 1250000.00,
  totalBlockedBalance: 45000.00,
  totalBalance: 1295000.00,
};

const EstablishmentsPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [establishments, setEstablishments] = useState<Establishment[]>(MOCK_ESTABLISHMENTS);
  const [summary, setSummary] = useState<EstablishmentSummary>(MOCK_SUMMARY);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: MOCK_ESTABLISHMENTS.length,
  });
  const [filters, setFilters] = useState({
    search: '',
    status: undefined as EstablishmentStatus | undefined,
    documentStatus: undefined as DocumentStatus | undefined,
    cnpj: '',
    dateRange: undefined as any,
  });
  const [showMoreFilters, setShowMoreFilters] = useState(false);
  const [selectedEstablishment, setSelectedEstablishment] = useState<Establishment | null>(null);
  const [formModalOpen, setFormModalOpen] = useState(false);
  const [editBalanceModalOpen, setEditBalanceModalOpen] = useState(false);
  const [editMedModalOpen, setEditMedModalOpen] = useState(false);
  const [editDisputeModalOpen, setEditDisputeModalOpen] = useState(false);
  const [retentionModalOpen, setRetentionModalOpen] = useState(false);
  const [affiliateLinkModalOpen, setAffiliateLinkModalOpen] = useState(false);

  const [editBalanceForm] = Form.useForm();
  const [editMedForm] = Form.useForm();
  const [editDisputeForm] = Form.useForm();
  const [retentionForm] = Form.useForm();
  const [affiliateLinkForm] = Form.useForm();

  const fetchEstablishments = useCallback(async () => {
    try {
      setLoading(true);
      // In production, use the service
      // const response = await establishmentService.getEstablishments({
      //   page: pagination.current - 1,
      //   size: pagination.pageSize,
      //   ...filters,
      //   startDate: filters.dateRange?.[0]?.format('YYYY-MM-DD'),
      //   endDate: filters.dateRange?.[1]?.format('YYYY-MM-DD'),
      // });
      // setEstablishments(response.content);
      // setPagination({ ...pagination, total: response.totalElements });

      // For now, use mock data with filtering
      let filtered = [...MOCK_ESTABLISHMENTS];

      if (filters.search) {
        const search = filters.search.toLowerCase();
        filtered = filtered.filter(
          (e) =>
            e.companyName.toLowerCase().includes(search) ||
            e.tradeName.toLowerCase().includes(search) ||
            e.cnpj.includes(search)
        );
      }

      if (filters.status) {
        filtered = filtered.filter((e) => e.status === filters.status);
      }

      if (filters.documentStatus) {
        filtered = filtered.filter((e) => e.documentStatus === filters.documentStatus);
      }

      setEstablishments(filtered);
      setPagination({ ...pagination, total: filtered.length });
    } catch (error: any) {
      message.error('Erro ao carregar estabelecimentos');
    } finally {
      setLoading(false);
    }
  }, [filters, pagination.current, pagination.pageSize]);

  const fetchSummary = useCallback(async () => {
    try {
      // In production, use the service
      // const data = await establishmentService.getSummary();
      // setSummary(data);

      // For now, calculate from mock data
      const available = establishments.reduce((sum, e) => sum + (e.balances?.available || 0), 0);
      const blocked = establishments.reduce((sum, e) => sum + (e.balances?.blocked || 0), 0);
      setSummary({
        totalEstablishments: establishments.length,
        totalAvailableBalance: available,
        totalBlockedBalance: blocked,
        totalBalance: available + blocked,
      });
    } catch (error) {
      console.error('Error fetching summary:', error);
    }
  }, [establishments]);

  useEffect(() => {
    fetchEstablishments();
  }, [filters]);

  useEffect(() => {
    fetchSummary();
  }, [establishments]);

  const handleClearFilters = () => {
    setFilters({
      search: '',
      status: undefined,
      documentStatus: undefined,
      cnpj: '',
      dateRange: undefined,
    });
  };

  const handleExport = async () => {
    try {
      message.loading('Exportando...', 0);
      // await establishmentService.exportToCsv(filters);
      message.destroy();
      message.success('Exportação iniciada!');
    } catch (error) {
      message.destroy();
      message.error('Erro ao exportar');
    }
  };

  const handleEdit = (establishment: Establishment) => {
    setSelectedEstablishment(establishment);
    setFormModalOpen(true);
  };

  const handleViewDocuments = (establishment: Establishment) => {
    message.info('Abrindo documentos...');
    // TODO: Implement documents modal
  };

  const handleViewTimeline = (establishment: Establishment) => {
    message.info('Abrindo timeline...');
    // TODO: Implement timeline modal
  };

  const handleResendActivation = async (establishment: Establishment) => {
    try {
      // await establishmentService.resendActivation(establishment.id);
      message.success('Email de ativação reenviado!');
    } catch (error) {
      message.error('Erro ao reenviar email');
    }
  };

  const handleMakeAffiliate = (establishment: Establishment) => {
    Modal.confirm({
      title: 'Tornar Afiliado',
      content: `Deseja tornar "${establishment.tradeName}" um afiliado?`,
      okText: 'Sim',
      cancelText: 'Não',
      onOk: async () => {
        try {
          // await establishmentService.makeAffiliate(establishment.id, 10);
          message.success('Estabelecimento agora é um afiliado!');
          fetchEstablishments();
        } catch (error) {
          message.error('Erro ao criar afiliado');
        }
      },
    });
  };

  const handleLinkAffiliate = (establishment: Establishment) => {
    setSelectedEstablishment(establishment);
    setAffiliateLinkModalOpen(true);
  };

  const handleEditBalance = (establishment: Establishment) => {
    setSelectedEstablishment(establishment);
    editBalanceForm.resetFields();
    setEditBalanceModalOpen(true);
  };

  const handleEditMed = (establishment: Establishment) => {
    setSelectedEstablishment(establishment);
    editMedForm.setFieldsValue({ medPercentage: establishment.medPercentage || 0 });
    setEditMedModalOpen(true);
  };

  const handleEditDispute = (establishment: Establishment) => {
    setSelectedEstablishment(establishment);
    editDisputeForm.setFieldsValue({ disputePercentage: establishment.disputePercentage || 0 });
    setEditDisputeModalOpen(true);
  };

  const handleConfigureRetention = (establishment: Establishment) => {
    setSelectedEstablishment(establishment);
    retentionForm.setFieldsValue({ retentionDays: establishment.retentionDays || 0 });
    setRetentionModalOpen(true);
  };

  const handleActivate = async (establishment: Establishment) => {
    Modal.confirm({
      title: 'Ativar Estabelecimento',
      content: `Deseja ativar "${establishment.tradeName}"?`,
      okText: 'Ativar',
      okType: 'primary',
      cancelText: 'Cancelar',
      onOk: async () => {
        try {
          // await establishmentService.activate(establishment.id);
          message.success('Estabelecimento ativado!');
          fetchEstablishments();
        } catch (error) {
          message.error('Erro ao ativar');
        }
      },
    });
  };

  const handleBlock = async (establishment: Establishment) => {
    Modal.confirm({
      title: 'Bloquear Estabelecimento',
      content: `Tem certeza que deseja bloquear "${establishment.tradeName}"? Esta ação pode afetar as operações do estabelecimento.`,
      okText: 'Bloquear',
      okType: 'danger',
      cancelText: 'Cancelar',
      onOk: async () => {
        try {
          // await establishmentService.block(establishment.id);
          message.success('Estabelecimento bloqueado!');
          fetchEstablishments();
        } catch (error) {
          message.error('Erro ao bloquear');
        }
      },
    });
  };

  const handleInactivate = async (establishment: Establishment) => {
    Modal.confirm({
      title: 'Inativar Estabelecimento',
      content: `Deseja inativar "${establishment.tradeName}"?`,
      okText: 'Inativar',
      okType: 'danger',
      cancelText: 'Cancelar',
      onOk: async () => {
        try {
          // await establishmentService.inactivate(establishment.id);
          message.success('Estabelecimento inativado!');
          fetchEstablishments();
        } catch (error) {
          message.error('Erro ao inativar');
        }
      },
    });
  };

  const getActionMenuItems = (record: Establishment) => [
    {
      key: 'documents',
      label: 'Ver documentos',
      icon: <EyeOutlined />,
      onClick: () => handleViewDocuments(record),
    },
    {
      key: 'edit',
      label: 'Editar',
      icon: <EditOutlined />,
      onClick: () => handleEdit(record),
    },
    {
      key: 'resend',
      label: 'Reenviar ativação',
      icon: <MailOutlined />,
      onClick: () => handleResendActivation(record),
    },
    { key: 'divider1', divider: true, label: '' },
    {
      key: 'makeAffiliate',
      label: 'Tornar afiliado',
      icon: <UserAddOutlined />,
      onClick: () => handleMakeAffiliate(record),
    },
    {
      key: 'linkAffiliate',
      label: 'Vincular a afiliado',
      icon: <LinkOutlined />,
      onClick: () => handleLinkAffiliate(record),
    },
    { key: 'divider2', divider: true, label: '' },
    {
      key: 'editBalance',
      label: 'Editar saldo',
      icon: <DollarOutlined />,
      onClick: () => handleEditBalance(record),
    },
    {
      key: 'editMed',
      label: 'Editar % MED',
      icon: <PercentageOutlined />,
      onClick: () => handleEditMed(record),
    },
    {
      key: 'editDispute',
      label: 'Editar % Disputa',
      icon: <PercentageOutlined />,
      onClick: () => handleEditDispute(record),
    },
    {
      key: 'retention',
      label: 'Configurar Retenção',
      icon: <ClockCircleOutlined />,
      onClick: () => handleConfigureRetention(record),
    },
    { key: 'divider3', divider: true, label: '' },
    {
      key: 'timeline',
      label: 'Timeline',
      icon: <HistoryOutlined />,
      onClick: () => handleViewTimeline(record),
    },
    { key: 'divider4', divider: true, label: '' },
    ...(record.status !== 'ACTIVE'
      ? [
          {
            key: 'activate',
            label: 'Ativar',
            icon: <CheckCircleOutlined />,
            onClick: () => handleActivate(record),
          },
        ]
      : []),
    ...(record.status !== 'BLOCKED'
      ? [
          {
            key: 'block',
            label: 'Bloquear',
            icon: <StopOutlined />,
            danger: true,
            onClick: () => handleBlock(record),
          },
        ]
      : []),
    ...(record.status !== 'INACTIVE'
      ? [
          {
            key: 'inactivate',
            label: 'Inativar',
            icon: <CloseCircleOutlined />,
            onClick: () => handleInactivate(record),
          },
        ]
      : []),
  ];

  const columns: ColumnsType<Establishment> = [
    {
      title: 'Estabelecimento',
      key: 'establishment',
      width: 250,
      fixed: 'left',
      render: (_, record) => (
        <div>
          <Text strong>{record.tradeName}</Text>
          <br />
          <Space size={4}>
            <Text type="secondary" style={{ fontSize: 12 }}>
              {formatCNPJ(record.cnpj)}
            </Text>
            <CopyButton text={record.cnpj} tooltipText="Copiar CNPJ" />
          </Space>
        </div>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: EstablishmentStatus) => (
        <Tag color={ESTABLISHMENT_STATUS_COLORS[status]}>
          {ESTABLISHMENT_STATUS_LABELS[status]}
        </Tag>
      ),
      filters: Object.entries(ESTABLISHMENT_STATUS_LABELS).map(([key, label]) => ({
        text: label,
        value: key,
      })),
    },
    {
      title: 'Documentos',
      dataIndex: 'documentStatus',
      key: 'documentStatus',
      width: 120,
      render: (status: DocumentStatus) => (
        <Tag color={DOCUMENT_STATUS_COLORS[status]}>
          {DOCUMENT_STATUS_LABELS[status]}
        </Tag>
      ),
      filters: Object.entries(DOCUMENT_STATUS_LABELS).map(([key, label]) => ({
        text: label,
        value: key,
      })),
    },
    {
      title: 'Saldo Disponível',
      key: 'available',
      width: 140,
      align: 'right',
      render: (_, record) => (
        <Text style={{ color: '#52c41a' }}>
          {formatCurrency(record.balances?.available || 0)}
        </Text>
      ),
      sorter: (a, b) => (a.balances?.available || 0) - (b.balances?.available || 0),
    },
    {
      title: 'Saldo Bloqueado',
      key: 'blocked',
      width: 140,
      align: 'right',
      render: (_, record) => (
        <Text style={{ color: '#ff4d4f' }}>
          {formatCurrency(record.balances?.blocked || 0)}
        </Text>
      ),
      sorter: (a, b) => (a.balances?.blocked || 0) - (b.balances?.blocked || 0),
    },
    {
      title: 'Saldo Total',
      key: 'total',
      width: 140,
      align: 'right',
      render: (_, record) => (
        <Text strong style={{ color: '#1890ff' }}>
          {formatCurrency(record.balances?.total || 0)}
        </Text>
      ),
      sorter: (a, b) => (a.balances?.total || 0) - (b.balances?.total || 0),
    },
    {
      title: 'Data Criação',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (date: string) => formatDate(date, 'DD/MM/YYYY'),
      sorter: (a, b) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    },
    {
      title: 'Ações',
      key: 'actions',
      width: 80,
      fixed: 'right',
      render: (_, record) => (
        <ActionMenu items={getActionMenuItems(record)} />
      ),
    },
  ];

  return (
    <div>
      {/* Header */}
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={2} style={{ margin: 0 }}>
            <ShopOutlined style={{ marginRight: 12 }} />
            Estabelecimentos
          </Title>
        </Col>
        <Col>
          <Space>
            <Button icon={<ReloadOutlined />} onClick={fetchEstablishments}>
              Atualizar
            </Button>
            <Button icon={<ExportOutlined />} onClick={handleExport}>
              Exportar
            </Button>
          </Space>
        </Col>
      </Row>

      {/* Summary Cards */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total de Estabelecimentos"
              value={summary.totalEstablishments}
              valueStyle={{ color: '#1890ff' }}
              prefix={<ShopOutlined />}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Saldo Disponível Total"
              value={summary.totalAvailableBalance}
              precision={2}
              prefix="R$"
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Saldo Bloqueado Total"
              value={summary.totalBlockedBalance}
              precision={2}
              prefix="R$"
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Saldo Total Geral"
              value={summary.totalBalance}
              precision={2}
              prefix="R$"
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card style={{ marginBottom: 16 }}>
        <Row gutter={[16, 16]}>
          <Col xs={24} md={8}>
            <Input
              placeholder="Buscar por nome ou CNPJ..."
              prefix={<SearchOutlined />}
              value={filters.search}
              onChange={(e) => setFilters({ ...filters, search: e.target.value })}
              allowClear
            />
          </Col>
          <Col xs={24} md={4}>
            <Select
              placeholder="Status"
              value={filters.status}
              onChange={(value) => setFilters({ ...filters, status: value })}
              style={{ width: '100%' }}
              allowClear
            >
              {Object.entries(ESTABLISHMENT_STATUS_LABELS).map(([key, label]) => (
                <Option key={key} value={key}>
                  {label}
                </Option>
              ))}
            </Select>
          </Col>
          <Col xs={24} md={4}>
            <Select
              placeholder="Documentos"
              value={filters.documentStatus}
              onChange={(value) => setFilters({ ...filters, documentStatus: value })}
              style={{ width: '100%' }}
              allowClear
            >
              {Object.entries(DOCUMENT_STATUS_LABELS).map(([key, label]) => (
                <Option key={key} value={key}>
                  {label}
                </Option>
              ))}
            </Select>
          </Col>
          <Col xs={24} md={6}>
            <RangePicker
              value={filters.dateRange}
              onChange={(dates) => setFilters({ ...filters, dateRange: dates })}
              format="DD/MM/YYYY"
              style={{ width: '100%' }}
              placeholder={['Data Início', 'Data Fim']}
            />
          </Col>
          <Col xs={24} md={2}>
            <Space>
              <Tooltip title="Limpar filtros">
                <Button icon={<ClearOutlined />} onClick={handleClearFilters} />
              </Tooltip>
            </Space>
          </Col>
        </Row>

        {showMoreFilters && (
          <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
            <Col xs={24} md={8}>
              <Input
                placeholder="CNPJ do Estabelecimento"
                value={filters.cnpj}
                onChange={(e) => setFilters({ ...filters, cnpj: e.target.value })}
                allowClear
              />
            </Col>
          </Row>
        )}

        <Button
          type="link"
          onClick={() => setShowMoreFilters(!showMoreFilters)}
          style={{ padding: 0, marginTop: 8 }}
        >
          <FilterOutlined /> {showMoreFilters ? 'Menos filtros' : 'Mais filtros'}
        </Button>
      </Card>

      {/* Table */}
      <Card>
        <Table
          columns={columns}
          dataSource={establishments}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `Total de ${total} estabelecimentos`,
            pageSizeOptions: ['10', '20', '50', '100'],
          }}
          onChange={(newPagination, tableFilters) => {
            setPagination({
              current: newPagination.current || 1,
              pageSize: newPagination.pageSize || 20,
              total: pagination.total,
            });

            // Apply table column filters
            if (tableFilters.status?.length) {
              setFilters({ ...filters, status: tableFilters.status[0] as EstablishmentStatus });
            }
            if (tableFilters.documentStatus?.length) {
              setFilters({ ...filters, documentStatus: tableFilters.documentStatus[0] as DocumentStatus });
            }
          }}
          scroll={{ x: 1400 }}
        />
      </Card>

      {/* Edit Form Modal */}
      <EstablishmentForm
        open={formModalOpen}
        establishment={selectedEstablishment}
        onClose={() => {
          setFormModalOpen(false);
          setSelectedEstablishment(null);
        }}
        onSuccess={() => {
          fetchEstablishments();
          setFormModalOpen(false);
          setSelectedEstablishment(null);
        }}
      />

      {/* Edit Balance Modal */}
      <Modal
        title="Editar Saldo"
        open={editBalanceModalOpen}
        onCancel={() => setEditBalanceModalOpen(false)}
        onOk={async () => {
          try {
            const values = await editBalanceForm.validateFields();
            // await establishmentService.editBalance(selectedEstablishment!.id, values);
            message.success('Saldo editado com sucesso!');
            setEditBalanceModalOpen(false);
            fetchEstablishments();
          } catch (error) {
            message.error('Erro ao editar saldo');
          }
        }}
        okText="Salvar"
      >
        <Form form={editBalanceForm} layout="vertical">
          <Form.Item name="type" label="Tipo de Operação" rules={[{ required: true }]}>
            <Select>
              <Option value="ADD">Adicionar</Option>
              <Option value="SUBTRACT">Subtrair</Option>
            </Select>
          </Form.Item>
          <Form.Item name="wallet" label="Carteira" rules={[{ required: true }]}>
            <Select>
              <Option value="PIX">Carteira PIX</Option>
              <Option value="CARD">Carteira Cartão</Option>
              <Option value="BOLETO">Carteira Boleto</Option>
            </Select>
          </Form.Item>
          <Form.Item name="amount" label="Valor" rules={[{ required: true }]}>
            <InputNumber prefix="R$" style={{ width: '100%' }} min={0} precision={2} />
          </Form.Item>
          <Form.Item name="reason" label="Motivo" rules={[{ required: true }]}>
            <Input.TextArea rows={3} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit MED Modal */}
      <Modal
        title="Editar % MED"
        open={editMedModalOpen}
        onCancel={() => setEditMedModalOpen(false)}
        onOk={async () => {
          try {
            const values = await editMedForm.validateFields();
            // await establishmentService.editMedPercentage(selectedEstablishment!.id, values.medPercentage);
            message.success('% MED editado com sucesso!');
            setEditMedModalOpen(false);
            fetchEstablishments();
          } catch (error) {
            message.error('Erro ao editar % MED');
          }
        }}
        okText="Salvar"
      >
        <Form form={editMedForm} layout="vertical">
          <Form.Item name="medPercentage" label="Percentual MED (%)" rules={[{ required: true }]}>
            <InputNumber suffix="%" style={{ width: '100%' }} min={0} max={100} precision={2} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Edit Dispute Modal */}
      <Modal
        title="Editar % Disputa"
        open={editDisputeModalOpen}
        onCancel={() => setEditDisputeModalOpen(false)}
        onOk={async () => {
          try {
            const values = await editDisputeForm.validateFields();
            // await establishmentService.editDisputePercentage(selectedEstablishment!.id, values.disputePercentage);
            message.success('% Disputa editado com sucesso!');
            setEditDisputeModalOpen(false);
            fetchEstablishments();
          } catch (error) {
            message.error('Erro ao editar % Disputa');
          }
        }}
        okText="Salvar"
      >
        <Form form={editDisputeForm} layout="vertical">
          <Form.Item name="disputePercentage" label="Percentual Disputa (%)" rules={[{ required: true }]}>
            <InputNumber suffix="%" style={{ width: '100%' }} min={0} max={100} precision={2} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Configure Retention Modal */}
      <Modal
        title="Configurar Retenção"
        open={retentionModalOpen}
        onCancel={() => setRetentionModalOpen(false)}
        onOk={async () => {
          try {
            const values = await retentionForm.validateFields();
            // await establishmentService.configureRetention(selectedEstablishment!.id, values);
            message.success('Retenção configurada com sucesso!');
            setRetentionModalOpen(false);
            fetchEstablishments();
          } catch (error) {
            message.error('Erro ao configurar retenção');
          }
        }}
        okText="Salvar"
      >
        <Form form={retentionForm} layout="vertical">
          <Form.Item name="retentionDays" label="Dias de Retenção" rules={[{ required: true }]}>
            <InputNumber suffix="dias" style={{ width: '100%' }} min={0} max={365} />
          </Form.Item>
          <Form.Item name="retentionPercentage" label="Percentual de Retenção (%)" rules={[{ required: true }]}>
            <InputNumber suffix="%" style={{ width: '100%' }} min={0} max={100} precision={2} />
          </Form.Item>
        </Form>
      </Modal>

      {/* Link Affiliate Modal */}
      <Modal
        title="Vincular a Afiliado"
        open={affiliateLinkModalOpen}
        onCancel={() => setAffiliateLinkModalOpen(false)}
        onOk={async () => {
          try {
            const values = await affiliateLinkForm.validateFields();
            // await establishmentService.linkToAffiliate(selectedEstablishment!.id, values.affiliateId);
            message.success('Vinculado ao afiliado com sucesso!');
            setAffiliateLinkModalOpen(false);
            fetchEstablishments();
          } catch (error) {
            message.error('Erro ao vincular afiliado');
          }
        }}
        okText="Vincular"
      >
        <Form form={affiliateLinkForm} layout="vertical">
          <Form.Item name="affiliateId" label="ID do Afiliado" rules={[{ required: true }]}>
            <Input placeholder="Digite o ID do afiliado" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default EstablishmentsPage;
