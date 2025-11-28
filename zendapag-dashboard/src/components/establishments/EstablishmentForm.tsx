// @ts-nocheck
import React, { useEffect, useState } from 'react';
import {
  Modal,
  Form,
  Input,
  Select,
  Tabs,
  Row,
  Col,
  InputNumber,
  Radio,
  Divider,
  Typography,
  Tag,
  Space,
  Card,
  Statistic,
  message,
  Spin,
} from 'antd';
import {
  UserOutlined,
  ShopOutlined,
  DollarOutlined,
  ApiOutlined,
} from '@ant-design/icons';
import {
  ESTABLISHMENT_STATUS_LABELS,
  ESTABLISHMENT_STATUS_COLORS,
  DOCUMENT_STATUS_LABELS,
  DOCUMENT_STATUS_COLORS,
  FEE_TYPE_LABELS,
  WITHDRAWAL_TYPE_LABELS,
  ACQUIRER_PROVIDERS,
  BRAZILIAN_STATES,
} from '@/utils/constants';
import { formatCurrency, formatDate } from '@/utils/helpers';
import type {
  Establishment,
  EstablishmentStatus,
  DocumentStatus,
  FeeType,
  WithdrawalType,
  DEFAULT_CARD_FEES,
} from '@/types';
import establishmentService from '@/services/establishmentService';

const { Title, Text } = Typography;
const { Option } = Select;
const { TabPane } = Tabs;

interface EstablishmentFormProps {
  open: boolean;
  establishment: Establishment | null;
  onClose: () => void;
  onSuccess: () => void;
}

const EstablishmentForm: React.FC<EstablishmentFormProps> = ({
  open,
  establishment,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [activeTab, setActiveTab] = useState('basic');
  const [pixFeeType, setPixFeeType] = useState<FeeType>('FIXED');
  const [boletoFeeType, setBoletoFeeType] = useState<FeeType>('FIXED');
  const [withdrawalType, setWithdrawalType] = useState<WithdrawalType>('MANUAL');

  useEffect(() => {
    if (open && establishment) {
      // Populate form with establishment data
      form.setFieldsValue({
        // Basic tab
        companyName: establishment.companyName,
        tradeName: establishment.tradeName,
        email: establishment.email,
        phone: establishment.phone,
        street: establishment.address?.street,
        city: establishment.address?.city,
        state: establishment.address?.state,
        zipCode: establishment.address?.zipCode,

        // Company tab
        cnpj: establishment.cnpj,
        status: establishment.status,
        documentStatus: establishment.documentStatus,

        // Financial tab
        pixFeeType: establishment.fees?.pix?.type || 'FIXED',
        pixFeeValue: establishment.fees?.pix?.value || 0,
        boletoFeeType: establishment.fees?.boleto?.type || 'FIXED',
        boletoFeeValue: establishment.fees?.boleto?.value || 0,
        ...Object.fromEntries(
          Array.from({ length: 12 }, (_, i) => [
            `cardFee${i + 1}`,
            establishment.fees?.card?.installments?.[i + 1] || 0,
          ])
        ),
        withdrawalType: establishment.withdrawal?.type || 'MANUAL',
        automaticFee: establishment.withdrawal?.automaticFee || 0,
        withdrawalLimit: establishment.withdrawal?.limit || 0,

        // Acquirers tab
        pixTransactionProvider: establishment.acquirers?.pix?.transactions?.provider,
        pixTransactionDisplayName: establishment.acquirers?.pix?.transactions?.displayName,
        pixWithdrawalProvider: establishment.acquirers?.pix?.withdrawals?.provider,
        pixWithdrawalDisplayName: establishment.acquirers?.pix?.withdrawals?.displayName,
        cardTransactionProvider: establishment.acquirers?.card?.transactions?.provider,
        cardTransactionDisplayName: establishment.acquirers?.card?.transactions?.displayName,
        boletoTransactionProvider: establishment.acquirers?.boleto?.transactions?.provider,
        boletoTransactionDisplayName: establishment.acquirers?.boleto?.transactions?.displayName,
      });

      setPixFeeType(establishment.fees?.pix?.type || 'FIXED');
      setBoletoFeeType(establishment.fees?.boleto?.type || 'FIXED');
      setWithdrawalType(establishment.withdrawal?.type || 'MANUAL');
    }
  }, [open, establishment, form]);

  const handleClose = () => {
    form.resetFields();
    setActiveTab('basic');
    onClose();
  };

  const handleSave = async () => {
    try {
      const values = await form.validateFields();
      setSaving(true);

      if (establishment) {
        // Update basic info
        await establishmentService.update(establishment.id, {
          companyName: values.companyName,
          tradeName: values.tradeName,
          email: values.email,
          phone: values.phone,
          address: {
            street: values.street,
            city: values.city,
            state: values.state,
            zipCode: values.zipCode,
          },
          status: values.status,
        });

        // Update fees
        await establishmentService.updateFees(establishment.id, {
          pix: {
            type: values.pixFeeType,
            value: values.pixFeeValue,
          },
          boleto: {
            type: values.boletoFeeType,
            value: values.boletoFeeValue,
          },
          card: {
            type: 'PERCENTAGE',
            installments: Object.fromEntries(
              Array.from({ length: 12 }, (_, i) => [i + 1, values[`cardFee${i + 1}`] || 0])
            ),
          },
        });

        // Update withdrawal config
        await establishmentService.updateWithdrawalConfig(establishment.id, {
          type: values.withdrawalType,
          automaticFee: values.automaticFee || 0,
          limit: values.withdrawalLimit || 0,
        });

        // Update acquirers
        await establishmentService.updateAcquirers(establishment.id, {
          pix: {
            transactions: {
              provider: values.pixTransactionProvider,
              displayName: values.pixTransactionDisplayName,
            },
            withdrawals: {
              provider: values.pixWithdrawalProvider,
              displayName: values.pixWithdrawalDisplayName,
            },
          },
          card: {
            transactions: {
              provider: values.cardTransactionProvider,
              displayName: values.cardTransactionDisplayName,
            },
          },
          boleto: {
            transactions: {
              provider: values.boletoTransactionProvider,
              displayName: values.boletoTransactionDisplayName,
            },
          },
        });

        message.success('Estabelecimento atualizado com sucesso!');
        onSuccess();
        handleClose();
      }
    } catch (error: any) {
      console.error('Error saving establishment:', error);
      message.error(error.message || 'Erro ao salvar estabelecimento');
    } finally {
      setSaving(false);
    }
  };

  const renderBasicTab = () => (
    <div style={{ padding: '16px 0' }}>
      <Row gutter={16}>
        <Col span={12}>
          <Form.Item
            name="companyName"
            label="Razão Social"
            rules={[{ required: true, message: 'Informe a razão social' }]}
          >
            <Input placeholder="Razão Social da Empresa" />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item
            name="tradeName"
            label="Nome Fantasia"
            rules={[{ required: true, message: 'Informe o nome fantasia' }]}
          >
            <Input placeholder="Nome Fantasia" />
          </Form.Item>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Form.Item
            name="email"
            label="Email"
            rules={[
              { required: true, message: 'Informe o email' },
              { type: 'email', message: 'Email inválido' },
            ]}
          >
            <Input placeholder="email@empresa.com" />
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
      </Row>

      <Divider orientation="left">Endereço</Divider>

      <Row gutter={16}>
        <Col span={16}>
          <Form.Item name="street" label="Endereço">
            <Input placeholder="Rua, número, complemento" />
          </Form.Item>
        </Col>
        <Col span={8}>
          <Form.Item name="zipCode" label="CEP">
            <Input placeholder="00000-000" />
          </Form.Item>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Form.Item name="city" label="Cidade">
            <Input placeholder="Cidade" />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item name="state" label="Estado">
            <Select placeholder="Selecione o estado">
              {BRAZILIAN_STATES.map((state) => (
                <Option key={state.value} value={state.value}>
                  {state.label}
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Col>
      </Row>
    </div>
  );

  const renderCompanyTab = () => (
    <div style={{ padding: '16px 0' }}>
      <Row gutter={16}>
        <Col span={12}>
          <Form.Item name="cnpj" label="CNPJ">
            <Input disabled placeholder="00.000.000/0000-00" />
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item
            name="status"
            label="Status"
            rules={[{ required: true, message: 'Selecione o status' }]}
          >
            <Select placeholder="Selecione o status">
              {Object.entries(ESTABLISHMENT_STATUS_LABELS).map(([key, label]) => (
                <Option key={key} value={key}>
                  <Tag color={ESTABLISHMENT_STATUS_COLORS[key as EstablishmentStatus]}>
                    {label}
                  </Tag>
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Col>
      </Row>

      <Row gutter={16}>
        <Col span={12}>
          <Form.Item name="documentStatus" label="Status dos Documentos">
            <Select disabled placeholder="Status">
              {Object.entries(DOCUMENT_STATUS_LABELS).map(([key, label]) => (
                <Option key={key} value={key}>
                  <Tag color={DOCUMENT_STATUS_COLORS[key as DocumentStatus]}>
                    {label}
                  </Tag>
                </Option>
              ))}
            </Select>
          </Form.Item>
        </Col>
        <Col span={12}>
          <Form.Item label="Data de Criação">
            <Input
              disabled
              value={establishment ? formatDate(establishment.createdAt) : '-'}
            />
          </Form.Item>
        </Col>
      </Row>

      {establishment && (
        <>
          <Divider orientation="left">Saldos</Divider>
          <Row gutter={16}>
            <Col span={8}>
              <Card size="small">
                <Statistic
                  title="Saldo Disponível"
                  value={establishment.balances?.available || 0}
                  precision={2}
                  prefix="R$"
                  valueStyle={{ color: '#3f8600', fontSize: 18 }}
                />
              </Card>
            </Col>
            <Col span={8}>
              <Card size="small">
                <Statistic
                  title="Saldo Bloqueado"
                  value={establishment.balances?.blocked || 0}
                  precision={2}
                  prefix="R$"
                  valueStyle={{ color: '#cf1322', fontSize: 18 }}
                />
              </Card>
            </Col>
            <Col span={8}>
              <Card size="small">
                <Statistic
                  title="Saldo Total"
                  value={establishment.balances?.total || 0}
                  precision={2}
                  prefix="R$"
                  valueStyle={{ color: '#1890ff', fontSize: 18 }}
                />
              </Card>
            </Col>
          </Row>
        </>
      )}
    </div>
  );

  const renderFinancialTab = () => (
    <div style={{ padding: '16px 0' }}>
      {/* PIX Fees */}
      <Card size="small" title="PIX" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="pixFeeType" label="Tipo de Taxa">
              <Radio.Group onChange={(e) => setPixFeeType(e.target.value)}>
                <Radio.Button value="FIXED">Fixo</Radio.Button>
                <Radio.Button value="PERCENTAGE">Percentual</Radio.Button>
              </Radio.Group>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="pixFeeValue"
              label={pixFeeType === 'FIXED' ? 'Valor da Taxa (R$)' : 'Percentual (%)'}
            >
              <InputNumber
                style={{ width: '100%' }}
                min={0}
                step={pixFeeType === 'FIXED' ? 0.01 : 0.1}
                precision={2}
                prefix={pixFeeType === 'FIXED' ? 'R$' : ''}
                suffix={pixFeeType === 'PERCENTAGE' ? '%' : ''}
              />
            </Form.Item>
          </Col>
        </Row>
      </Card>

      {/* Boleto Fees */}
      <Card size="small" title="Boleto" style={{ marginBottom: 16 }}>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="boletoFeeType" label="Tipo de Taxa">
              <Radio.Group onChange={(e) => setBoletoFeeType(e.target.value)}>
                <Radio.Button value="FIXED">Fixo</Radio.Button>
                <Radio.Button value="PERCENTAGE">Percentual</Radio.Button>
              </Radio.Group>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item
              name="boletoFeeValue"
              label={boletoFeeType === 'FIXED' ? 'Valor da Taxa (R$)' : 'Percentual (%)'}
            >
              <InputNumber
                style={{ width: '100%' }}
                min={0}
                step={boletoFeeType === 'FIXED' ? 0.01 : 0.1}
                precision={2}
                prefix={boletoFeeType === 'FIXED' ? 'R$' : ''}
                suffix={boletoFeeType === 'PERCENTAGE' ? '%' : ''}
              />
            </Form.Item>
          </Col>
        </Row>
      </Card>

      {/* Card Fees */}
      <Card size="small" title="Cartão de Crédito - Taxas por Parcela (%)" style={{ marginBottom: 16 }}>
        <Row gutter={[8, 8]}>
          {Array.from({ length: 12 }, (_, i) => (
            <Col span={6} key={i + 1}>
              <Form.Item
                name={`cardFee${i + 1}`}
                label={`${i + 1}x`}
                style={{ marginBottom: 8 }}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  min={0}
                  max={100}
                  step={0.01}
                  precision={2}
                  suffix="%"
                  size="small"
                />
              </Form.Item>
            </Col>
          ))}
        </Row>
      </Card>

      {/* Withdrawal Config */}
      <Card size="small" title="Configurações de Saque">
        <Row gutter={16}>
          <Col span={8}>
            <Form.Item name="withdrawalType" label="Tipo de Saque">
              <Radio.Group onChange={(e) => setWithdrawalType(e.target.value)}>
                <Radio.Button value="MANUAL">Manual</Radio.Button>
                <Radio.Button value="AUTOMATIC">Automático</Radio.Button>
              </Radio.Group>
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="automaticFee" label="Taxa Saque Automático (R$)">
              <InputNumber
                style={{ width: '100%' }}
                min={0}
                step={0.01}
                precision={2}
                prefix="R$"
                disabled={withdrawalType === 'MANUAL'}
              />
            </Form.Item>
          </Col>
          <Col span={8}>
            <Form.Item name="withdrawalLimit" label="Limite de Saque (R$)">
              <InputNumber
                style={{ width: '100%' }}
                min={0}
                step={100}
                precision={2}
                prefix="R$"
              />
            </Form.Item>
          </Col>
        </Row>
      </Card>
    </div>
  );

  const renderAcquirersTab = () => (
    <div style={{ padding: '16px 0' }}>
      {/* PIX Acquirers */}
      <Card size="small" title="PIX" style={{ marginBottom: 16 }}>
        <Title level={5} style={{ marginBottom: 16 }}>Transações</Title>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="pixTransactionProvider" label="Provider">
              <Select placeholder="Selecione o provider">
                {Object.entries(ACQUIRER_PROVIDERS).map(([key, provider]) => (
                  <Option key={key} value={provider.code}>
                    {provider.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="pixTransactionDisplayName" label="Nome de Exibição">
              <Input placeholder="Nome para exibição" />
            </Form.Item>
          </Col>
        </Row>

        <Divider dashed />

        <Title level={5} style={{ marginBottom: 16 }}>Saques</Title>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="pixWithdrawalProvider" label="Provider">
              <Select placeholder="Selecione o provider">
                {Object.entries(ACQUIRER_PROVIDERS).map(([key, provider]) => (
                  <Option key={key} value={provider.code}>
                    {provider.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="pixWithdrawalDisplayName" label="Nome de Exibição">
              <Input placeholder="Nome para exibição" />
            </Form.Item>
          </Col>
        </Row>
      </Card>

      {/* Card Acquirers */}
      <Card size="small" title="Cartão de Crédito" style={{ marginBottom: 16 }}>
        <Title level={5} style={{ marginBottom: 16 }}>Transações</Title>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="cardTransactionProvider" label="Provider">
              <Select placeholder="Selecione o provider">
                {Object.entries(ACQUIRER_PROVIDERS).map(([key, provider]) => (
                  <Option key={key} value={provider.code}>
                    {provider.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="cardTransactionDisplayName" label="Nome de Exibição">
              <Input placeholder="Nome para exibição" />
            </Form.Item>
          </Col>
        </Row>
      </Card>

      {/* Boleto Acquirers */}
      <Card size="small" title="Boleto">
        <Title level={5} style={{ marginBottom: 16 }}>Transações</Title>
        <Row gutter={16}>
          <Col span={12}>
            <Form.Item name="boletoTransactionProvider" label="Provider">
              <Select placeholder="Selecione o provider">
                {Object.entries(ACQUIRER_PROVIDERS).map(([key, provider]) => (
                  <Option key={key} value={provider.code}>
                    {provider.name}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>
          <Col span={12}>
            <Form.Item name="boletoTransactionDisplayName" label="Nome de Exibição">
              <Input placeholder="Nome para exibição" />
            </Form.Item>
          </Col>
        </Row>
      </Card>
    </div>
  );

  const tabItems = [
    {
      key: 'basic',
      label: (
        <span>
          <UserOutlined />
          Básico
        </span>
      ),
      children: renderBasicTab(),
    },
    {
      key: 'company',
      label: (
        <span>
          <ShopOutlined />
          Empresa
        </span>
      ),
      children: renderCompanyTab(),
    },
    {
      key: 'financial',
      label: (
        <span>
          <DollarOutlined />
          Financeiro
        </span>
      ),
      children: renderFinancialTab(),
    },
    {
      key: 'acquirers',
      label: (
        <span>
          <ApiOutlined />
          Adquirentes
        </span>
      ),
      children: renderAcquirersTab(),
    },
  ];

  return (
    <Modal
      title={
        <Space>
          <ShopOutlined />
          {establishment ? 'Editar Estabelecimento' : 'Novo Estabelecimento'}
        </Space>
      }
      open={open}
      onCancel={handleClose}
      onOk={handleSave}
      okText="Salvar"
      cancelText="Cancelar"
      width={900}
      confirmLoading={saving}
      destroyOnClose
    >
      <Spin spinning={loading}>
        <Form
          form={form}
          layout="vertical"
          requiredMark="optional"
        >
          <Tabs
            activeKey={activeTab}
            onChange={setActiveTab}
            items={tabItems}
          />
        </Form>
      </Spin>
    </Modal>
  );
};

export default EstablishmentForm;
