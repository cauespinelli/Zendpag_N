// @ts-nocheck
import React, { useState } from 'react';
import {
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  Typography,
  Space,
  Alert,
  Row,
  Col,
  Card,
  Divider,
  message,
  Statistic,
} from 'antd';
import {
  DollarOutlined,
  KeyOutlined,
  BankOutlined,
  IdcardOutlined,
  UserOutlined,
  WarningOutlined,
} from '@ant-design/icons';

import { formatCurrency } from '@/utils/helpers';

const { Text, Title } = Typography;
const { Option } = Select;
const { TextArea } = Input;

interface CreateWithdrawalModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: (withdrawalId: string) => void;
  accountBalance?: number;
  accountId?: string;
  merchantId?: string;
}

interface FormValues {
  amount: number;
  pixKey: string;
  pixKeyType: string;
  description?: string;
  recipientName?: string;
  recipientDocument?: string;
}

const PIX_KEY_TYPES = [
  { value: 'CPF', label: 'CPF', placeholder: '000.000.000-00' },
  { value: 'CNPJ', label: 'CNPJ', placeholder: '00.000.000/0000-00' },
  { value: 'EMAIL', label: 'E-mail', placeholder: 'email@exemplo.com' },
  { value: 'PHONE', label: 'Telefone', placeholder: '+55 11 99999-9999' },
  { value: 'RANDOM', label: 'Chave Aleatória', placeholder: 'chave-aleatoria-uuid' },
];

const CreateWithdrawalModal: React.FC<CreateWithdrawalModalProps> = ({
  open,
  onClose,
  onSuccess,
  accountBalance = 0,
  accountId,
  merchantId,
}) => {
  const [form] = Form.useForm<FormValues>();
  const [loading, setLoading] = useState(false);
  const [pixKeyType, setPixKeyType] = useState<string>('CPF');
  const [withdrawalAmount, setWithdrawalAmount] = useState<number>(0);

  const handlePixKeyTypeChange = (value: string) => {
    setPixKeyType(value);
    form.setFieldValue('pixKey', '');
  };

  const handleAmountChange = (value: number | null) => {
    setWithdrawalAmount(value || 0);
  };

  const handleSubmit = async (values: FormValues) => {
    if (!accountId || !merchantId) {
      message.error('Account ID and Merchant ID are required');
      return;
    }

    setLoading(true);

    try {
      // Aqui você faria a chamada para a API
      const response = await fetch(`/api/v1/withdrawals?accountId=${accountId}&merchantId=${merchantId}`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${localStorage.getItem('token')}`,
        },
        body: JSON.stringify(values),
      });

      if (!response.ok) {
        const error = await response.json();
        throw new Error(error.message || 'Failed to create withdrawal');
      }

      const data = await response.json();

      message.success('Saque solicitado com sucesso!');
      form.resetFields();

      if (onSuccess && data?.data?.id) {
        onSuccess(data.data.id);
      }

      onClose();

    } catch (error: any) {
      message.error(error.message || 'Erro ao solicitar saque');
      console.error('Withdrawal creation error:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleCancel = () => {
    form.resetFields();
    setWithdrawalAmount(0);
    onClose();
  };

  const fee = withdrawalAmount * 0.0; // 0% de taxa - ajustar conforme necessário
  const netAmount = withdrawalAmount - fee;
  const remainingBalance = accountBalance - withdrawalAmount;

  return (
    <Modal
      title={
        <Space>
          <BankOutlined style={{ fontSize: 24, color: '#1890ff' }} />
          <Title level={4} style={{ margin: 0 }}>
            Solicitar Saque PIX
          </Title>
        </Space>
      }
      open={open}
      onCancel={handleCancel}
      onOk={() => form.submit()}
      okText="Solicitar Saque"
      cancelText="Cancelar"
      width={700}
      confirmLoading={loading}
      destroyOnClose
    >
      <Divider />

      <Alert
        message="Informações Importantes"
        description={
          <ul style={{ margin: 0, paddingLeft: 20 }}>
            <li>O saque será processado em até 1 hora</li>
            <li>Verifique se a chave PIX está correta antes de confirmar</li>
            <li>O valor mínimo para saque é R$ 0,01</li>
            <li>O valor máximo para saque é R$ 50.000,00</li>
          </ul>
        }
        type="info"
        showIcon
        style={{ marginBottom: 24 }}
      />

      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        initialValues={{
          pixKeyType: 'CPF',
        }}
      >
        <Row gutter={16}>
          <Col span={24}>
            <Card size="small" style={{ marginBottom: 16, background: '#f5f5f5' }}>
              <Row gutter={16}>
                <Col span={8}>
                  <Statistic
                    title="Saldo Disponível"
                    value={accountBalance}
                    precision={2}
                    prefix="R$"
                    valueStyle={{ color: '#3f8600', fontSize: 20 }}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="Valor do Saque"
                    value={withdrawalAmount}
                    precision={2}
                    prefix="R$"
                    valueStyle={{ fontSize: 20 }}
                  />
                </Col>
                <Col span={8}>
                  <Statistic
                    title="Saldo Após Saque"
                    value={remainingBalance}
                    precision={2}
                    prefix="R$"
                    valueStyle={{
                      color: remainingBalance < 0 ? '#cf1322' : '#3f8600',
                      fontSize: 20,
                    }}
                  />
                </Col>
              </Row>
            </Card>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={24}>
            <Form.Item
              label={
                <Space>
                  <DollarOutlined />
                  <Text>Valor do Saque</Text>
                </Space>
              }
              name="amount"
              rules={[
                { required: true, message: 'Por favor, informe o valor do saque' },
                {
                  type: 'number',
                  min: 0.01,
                  message: 'O valor mínimo é R$ 0,01',
                },
                {
                  type: 'number',
                  max: 50000.0,
                  message: 'O valor máximo é R$ 50.000,00',
                },
                {
                  validator: (_, value) => {
                    if (value > accountBalance) {
                      return Promise.reject('Saldo insuficiente');
                    }
                    return Promise.resolve();
                  },
                },
              ]}
            >
              <InputNumber
                style={{ width: '100%' }}
                size="large"
                min={0.01}
                max={accountBalance}
                step={0.01}
                precision={2}
                prefix="R$"
                placeholder="0,00"
                onChange={handleAmountChange}
              />
            </Form.Item>
          </Col>
        </Row>

        <Divider orientation="left">Dados da Chave PIX</Divider>

        <Row gutter={16}>
          <Col span={8}>
            <Form.Item
              label={
                <Space>
                  <KeyOutlined />
                  <Text>Tipo de Chave</Text>
                </Space>
              }
              name="pixKeyType"
              rules={[{ required: true, message: 'Selecione o tipo de chave' }]}
            >
              <Select
                size="large"
                onChange={handlePixKeyTypeChange}
                placeholder="Selecione"
              >
                {PIX_KEY_TYPES.map((type) => (
                  <Option key={type.value} value={type.value}>
                    {type.label}
                  </Option>
                ))}
              </Select>
            </Form.Item>
          </Col>

          <Col span={16}>
            <Form.Item
              label={
                <Space>
                  <KeyOutlined />
                  <Text>Chave PIX</Text>
                </Space>
              }
              name="pixKey"
              rules={[
                { required: true, message: 'Por favor, informe a chave PIX' },
                { max: 255, message: 'Chave PIX muito longa' },
              ]}
            >
              <Input
                size="large"
                placeholder={
                  PIX_KEY_TYPES.find((t) => t.value === pixKeyType)?.placeholder ||
                  'Digite a chave PIX'
                }
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={12}>
            <Form.Item
              label={
                <Space>
                  <UserOutlined />
                  <Text>Nome do Favorecido (Opcional)</Text>
                </Space>
              }
              name="recipientName"
            >
              <Input
                size="large"
                placeholder="Nome do destinatário"
                maxLength={255}
              />
            </Form.Item>
          </Col>

          <Col span={12}>
            <Form.Item
              label={
                <Space>
                  <IdcardOutlined />
                  <Text>CPF/CNPJ do Favorecido (Opcional)</Text>
                </Space>
              }
              name="recipientDocument"
            >
              <Input
                size="large"
                placeholder="000.000.000-00"
                maxLength={20}
              />
            </Form.Item>
          </Col>
        </Row>

        <Row gutter={16}>
          <Col span={24}>
            <Form.Item
              label={<Text>Descrição (Opcional)</Text>}
              name="description"
            >
              <TextArea
                rows={3}
                placeholder="Adicione uma descrição para este saque"
                maxLength={500}
                showCount
              />
            </Form.Item>
          </Col>
        </Row>

        {withdrawalAmount > 0 && (
          <Card size="small" style={{ background: '#e6f7ff', borderColor: '#91d5ff' }}>
            <Row gutter={16}>
              <Col span={12}>
                <Text strong>Valor do Saque:</Text>
                <br />
                <Text style={{ fontSize: 16 }}>{formatCurrency(withdrawalAmount)}</Text>
              </Col>
              <Col span={12}>
                <Text strong>Taxa:</Text>
                <br />
                <Text style={{ fontSize: 16 }}>{formatCurrency(fee)}</Text>
              </Col>
              <Col span={24} style={{ marginTop: 8 }}>
                <Divider style={{ margin: '8px 0' }} />
                <Text strong style={{ fontSize: 16 }}>Valor Líquido:</Text>
                <br />
                <Text strong style={{ fontSize: 18, color: '#1890ff' }}>
                  {formatCurrency(netAmount)}
                </Text>
              </Col>
            </Row>
          </Card>
        )}

        {remainingBalance < 0 && (
          <Alert
            message="Saldo Insuficiente"
            description="O valor do saque é maior que o saldo disponível em sua conta."
            type="error"
            showIcon
            icon={<WarningOutlined />}
            style={{ marginTop: 16 }}
          />
        )}
      </Form>
    </Modal>
  );
};

export default CreateWithdrawalModal;
