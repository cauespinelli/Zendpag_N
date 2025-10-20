// @ts-nocheck
import React, { useEffect, useState } from 'react';
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
  Drawer,
  Descriptions,
  message,
  Modal,
  Form,
  InputNumber,
  Row,
  Col,
  Statistic,
} from 'antd';
import {
  SearchOutlined,
  PlusOutlined,
  EyeOutlined,
  QrcodeOutlined,
  CopyOutlined,
  CloseCircleOutlined,
  ReloadOutlined,
  FilterOutlined,
} from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';
import apiService from '@/services/api';
import { API_ENDPOINTS, PAYMENT_STATUS_COLORS, PAYMENT_STATUS_LABELS } from '@/utils/constants';
import { formatCurrency, formatDate } from '@/utils/helpers';
import type { Payment, PaginatedResponse } from '@/types';

const { Title, Text } = Typography;
const { RangePicker } = DatePicker;
const { Option } = Select;

const PaymentsPage: React.FC = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [payments, setPayments] = useState<Payment[]>([]);
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 20,
    total: 0,
  });
  const [filters, setFilters] = useState({
    search: '',
    status: undefined,
    dateRange: undefined,
  });
  const [selectedPayment, setSelectedPayment] = useState<Payment | null>(null);
  const [drawerVisible, setDrawerVisible] = useState(false);
  const [createModalVisible, setCreateModalVisible] = useState(false);
  const [form] = Form.useForm();
  const [stats, setStats] = useState({
    total: 0,
    completed: 0,
    pending: 0,
    failed: 0,
  });

  useEffect(() => {
    fetchPayments();
  }, [pagination.current, pagination.pageSize, filters]);

  const fetchPayments = async () => {
    try {
      setLoading(true);
      const params = {
        page: pagination.current - 1,
        size: pagination.pageSize,
        search: filters.search,
        status: filters.status,
        startDate: filters.dateRange?.[0]?.format('YYYY-MM-DD'),
        endDate: filters.dateRange?.[1]?.format('YYYY-MM-DD'),
      };

      const response = await apiService.getPaginated<Payment>(
        API_ENDPOINTS.PAYMENTS.BASE,
        params
      );

      setPayments(response.content);
      setPagination({
        ...pagination,
        total: response.totalElements,
      });

      // Calculate stats
      const completed = response.content.filter(p => p.status === 'COMPLETED').length;
      const pending = response.content.filter(p => p.status === 'PENDING' || p.status === 'ACTIVE').length;
      const failed = response.content.filter(p => p.status === 'FAILED' || p.status === 'CANCELLED').length;

      setStats({
        total: response.totalElements,
        completed,
        pending,
        failed,
      });
    } catch (error: any) {
      message.error('Erro ao carregar pagamentos');
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = (payment: Payment) => {
    setSelectedPayment(payment);
    setDrawerVisible(true);
  };

  const handleCreatePayment = async (values: any) => {
    try {
      await apiService.post(API_ENDPOINTS.PAYMENTS.PIX, values);
      message.success('Pagamento PIX criado com sucesso!');
      setCreateModalVisible(false);
      form.resetFields();
      fetchPayments();
    } catch (error: any) {
      message.error(error.message || 'Erro ao criar pagamento');
    }
  };

  const handleCancelPayment = async (paymentId: string) => {
    Modal.confirm({
      title: 'Confirmar cancelamento',
      content: 'Tem certeza que deseja cancelar este pagamento?',
      okText: 'Sim, cancelar',
      okType: 'danger',
      cancelText: 'Não',
      onOk: async () => {
        try {
          await apiService.post(API_ENDPOINTS.PAYMENTS.CANCEL(paymentId));
          message.success('Pagamento cancelado com sucesso');
          fetchPayments();
          setDrawerVisible(false);
        } catch (error: any) {
          message.error('Erro ao cancelar pagamento');
        }
      },
    });
  };

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text);
    message.success('Copiado para a área de transferência!');
  };

  const columns = [
    {
      title: 'ID',
      dataIndex: 'txid',
      key: 'txid',
      width: 120,
      fixed: 'left',
      render: (txid: string) => (
        <Text copyable ellipsis={{ tooltip: txid }} style={{ maxWidth: 100 }}>
          {txid}
        </Text>
      ),
    },
    {
      title: 'Cliente',
      key: 'customer',
      width: 180,
      render: (record: Payment) => (
        <div>
          <div>{record.payerName || record.customerName || '-'}</div>
          <Text type="secondary" style={{ fontSize: 12 }}>
            {record.payerDocument || record.customerDocument || '-'}
          </Text>
        </div>
      ),
    },
    {
      title: 'Valor',
      dataIndex: 'amount',
      key: 'amount',
      width: 120,
      render: (amount: number) => <Text strong>{formatCurrency(amount)}</Text>,
      sorter: (a: Payment, b: Payment) => a.amount - b.amount,
    },
    {
      title: 'Status',
      dataIndex: 'status',
      key: 'status',
      width: 120,
      render: (status: string) => (
        <Tag color={PAYMENT_STATUS_COLORS[status]}>
          {PAYMENT_STATUS_LABELS[status]}
        </Tag>
      ),
      filters: [
        { text: 'Pendente', value: 'PENDING' },
        { text: 'Ativo', value: 'ACTIVE' },
        { text: 'Concluído', value: 'COMPLETED' },
        { text: 'Falhou', value: 'FAILED' },
        { text: 'Cancelado', value: 'CANCELLED' },
        { text: 'Expirado', value: 'EXPIRED' },
      ],
    },
    {
      title: 'Descrição',
      dataIndex: 'description',
      key: 'description',
      width: 200,
      ellipsis: true,
      render: (desc: string) => desc || '-',
    },
    {
      title: 'Data de Criação',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (date: string) => formatDate(date),
      sorter: (a: Payment, b: Payment) => new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime(),
    },
    {
      title: 'Ações',
      key: 'actions',
      width: 100,
      fixed: 'right',
      render: (record: Payment) => (
        <Space>
          <Button
            type="link"
            icon={<EyeOutlined />}
            onClick={() => handleViewDetails(record)}
          >
            Ver
          </Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={2} style={{ margin: 0 }}>Pagamentos</Title>
          <Text type="secondary">Gerencie seus pagamentos PIX</Text>
        </Col>
        <Col>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => setCreateModalVisible(true)}
            size="large"
          >
            Novo Pagamento PIX
          </Button>
        </Col>
      </Row>

      {/* Stats Cards */}
      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Total"
              value={stats.total}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Concluídos"
              value={stats.completed}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Pendentes"
              value={stats.pending}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <Card>
            <Statistic
              title="Falhas"
              value={stats.failed}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Filters */}
      <Card style={{ marginBottom: 16 }}>
        <Space wrap style={{ width: '100%' }}>
          <Input
            placeholder="Buscar por ID, cliente ou documento..."
            prefix={<SearchOutlined />}
            value={filters.search}
            onChange={(e) => setFilters({ ...filters, search: e.target.value })}
            style={{ width: 300 }}
            allowClear
          />
          <Select
            placeholder="Filtrar por status"
            value={filters.status}
            onChange={(value) => setFilters({ ...filters, status: value })}
            style={{ width: 150 }}
            allowClear
          >
            <Option value="PENDING">Pendente</Option>
            <Option value="ACTIVE">Ativo</Option>
            <Option value="COMPLETED">Concluído</Option>
            <Option value="FAILED">Falhou</Option>
            <Option value="CANCELLED">Cancelado</Option>
            <Option value="EXPIRED">Expirado</Option>
          </Select>
          <RangePicker
            value={filters.dateRange}
            onChange={(dates) => setFilters({ ...filters, dateRange: dates })}
            format="DD/MM/YYYY"
          />
          <Button icon={<ReloadOutlined />} onClick={fetchPayments}>
            Atualizar
          </Button>
        </Space>
      </Card>

      {/* Table */}
      <Card>
        <Table
          columns={columns}
          dataSource={payments}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showTotal: (total) => `Total de ${total} pagamentos`,
          }}
          onChange={(newPagination) => {
            setPagination({
              current: newPagination.current || 1,
              pageSize: newPagination.pageSize || 20,
              total: pagination.total,
            });
          }}
          scroll={{ x: 1200 }}
        />
      </Card>

      {/* Payment Details Drawer */}
      <Drawer
        title="Detalhes do Pagamento"
        placement="right"
        width={600}
        onClose={() => setDrawerVisible(false)}
        open={drawerVisible}
      >
        {selectedPayment && (
          <div>
            <Descriptions column={1} bordered>
              <Descriptions.Item label="ID da Transação">
                <Text copyable>{selectedPayment.txid || selectedPayment.id}</Text>
              </Descriptions.Item>
              <Descriptions.Item label="Referência">
                {selectedPayment.referenceId || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Valor">
                <Text strong style={{ fontSize: 18 }}>
                  {formatCurrency(selectedPayment.amount)}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="Status">
                <Tag color={PAYMENT_STATUS_COLORS[selectedPayment.status]} style={{ fontSize: 14 }}>
                  {PAYMENT_STATUS_LABELS[selectedPayment.status]}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="Descrição">
                {selectedPayment.description || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Cliente">
                {selectedPayment.customerName || selectedPayment.payerName || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Documento do Cliente">
                {selectedPayment.customerDocument || selectedPayment.payerDocument || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Email do Cliente">
                {selectedPayment.customerEmail || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Chave PIX">
                {selectedPayment.pixKey ? (
                  <Space>
                    <Text>{selectedPayment.pixKey}</Text>
                    <Button
                      type="link"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => copyToClipboard(selectedPayment.pixKey!)}
                    />
                  </Space>
                ) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Tipo de Chave">
                {selectedPayment.pixKeyType || '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Data de Criação">
                {formatDate(selectedPayment.createdAt)}
              </Descriptions.Item>
              <Descriptions.Item label="Data de Pagamento">
                {selectedPayment.paidAt ? formatDate(selectedPayment.paidAt) : '-'}
              </Descriptions.Item>
              <Descriptions.Item label="Data de Expiração">
                {selectedPayment.expiresAt ? formatDate(selectedPayment.expiresAt) : '-'}
              </Descriptions.Item>
            </Descriptions>

            {selectedPayment.qrCodeText && (
              <div style={{ marginTop: 24 }}>
                <Title level={5}>QR Code PIX</Title>
                <Input.TextArea
                  value={selectedPayment.qrCodeText}
                  rows={4}
                  readOnly
                />
                <Button
                  type="primary"
                  icon={<CopyOutlined />}
                  onClick={() => copyToClipboard(selectedPayment.qrCodeText!)}
                  style={{ marginTop: 8 }}
                  block
                >
                  Copiar Código PIX
                </Button>
              </div>
            )}

            {(selectedPayment.status === 'PENDING' || selectedPayment.status === 'ACTIVE') && (
              <Button
                danger
                icon={<CloseCircleOutlined />}
                onClick={() => handleCancelPayment(selectedPayment.id)}
                style={{ marginTop: 24 }}
                block
              >
                Cancelar Pagamento
              </Button>
            )}
          </div>
        )}
      </Drawer>

      {/* Create Payment Modal */}
      <Modal
        title="Criar Novo Pagamento PIX"
        open={createModalVisible}
        onCancel={() => {
          setCreateModalVisible(false);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form
          form={form}
          layout="vertical"
          onFinish={handleCreatePayment}
        >
          <Form.Item
            name="amount"
            label="Valor"
            rules={[
              { required: true, message: 'Por favor, insira o valor' },
              { type: 'number', min: 0.01, message: 'Valor deve ser maior que R$ 0,00' },
            ]}
          >
            <InputNumber
              style={{ width: '100%' }}
              prefix="R$"
              precision={2}
              placeholder="0,00"
            />
          </Form.Item>

          <Form.Item
            name="description"
            label="Descrição"
            rules={[{ required: true, message: 'Por favor, insira uma descrição' }]}
          >
            <Input.TextArea rows={3} placeholder="Descrição do pagamento" />
          </Form.Item>

          <Form.Item
            name="customerName"
            label="Nome do Cliente"
          >
            <Input placeholder="Nome completo" />
          </Form.Item>

          <Form.Item
            name="customerEmail"
            label="Email do Cliente"
            rules={[{ type: 'email', message: 'Email inválido' }]}
          >
            <Input placeholder="email@exemplo.com" />
          </Form.Item>

          <Form.Item
            name="customerDocument"
            label="CPF/CNPJ do Cliente"
          >
            <Input placeholder="00000000000" />
          </Form.Item>

          <Form.Item
            name="expirationMinutes"
            label="Validade (minutos)"
            initialValue={30}
          >
            <InputNumber
              style={{ width: '100%' }}
              min={5}
              max={1440}
              placeholder="30"
            />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default PaymentsPage;
