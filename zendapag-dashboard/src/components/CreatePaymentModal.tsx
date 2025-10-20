// @ts-nocheck
// @ts-nocheck
import React, { useState, useEffect } from 'react';
import {
  Modal,
  Form,
  Input,
  InputNumber,
  Select,
  DatePicker,
  Switch,
  Typography,
  Space,
  Alert,
  Row,
  Col,
  Card,
  Divider,
  message,
} from 'antd';
import {
  DollarOutlined,
  UserOutlined,
  MailOutlined,
  IdcardOutlined,
  KeyOutlined,
  ClockCircleOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';

import { useCreatePixPayment } from '@/hooks/useQuery';
import { useFormDraft } from '@/hooks/useLocalStorage';
import { formatCurrency, isValidEmail, isValidCPF, isValidCNPJ } from '@/utils/helpers';
import { VALIDATION_RULES } from '@/utils/constants';
import type { PaymentFormData } from '@/types';

const { Text, Title } = Typography;
const { Option } = Select;
const { TextArea } = Input;

interface CreatePaymentModalProps {
  open: boolean;
  onClose: () => void;
  onSuccess?: (paymentId: string) => void;
}

interface FormValues extends PaymentFormData {
  pixKey?: string;
  pixKeyType?: string;
  hasExpiration: boolean;
}

const PIX_KEY_TYPES = [
  { value: 'CPF', label: 'CPF', pattern: VALIDATION_RULES.PIX_KEY.CPF },
  { value: 'CNPJ', label: 'CNPJ', pattern: VALIDATION_RULES.PIX_KEY.CNPJ },
  { value: 'EMAIL', label: 'E-mail', pattern: VALIDATION_RULES.PIX_KEY.EMAIL },
  { value: 'PHONE', label: 'Telefone', pattern: VALIDATION_RULES.PIX_KEY.PHONE },
  { value: 'EVP', label: 'Chave Aleatória', pattern: VALIDATION_RULES.PIX_KEY.EVP },
];

const CreatePaymentModal: React.FC<CreatePaymentModalProps> = ({
  open,
  onClose,
  onSuccess,
}) => {
  const [form] = Form.useForm<FormValues>();
  const [previewAmount, setPreviewAmount] = useState<number>(0);
  const [pixKeyType, setPixKeyType] = useState<string>('');

  // Form draft management
  const { draft, saveDraft, clearDraft } = useFormDraft<FormValues>('create-payment');

  // Mutations
  const createPaymentMutation = useCreatePixPayment();

  // Load draft on mount
  useEffect(() => {
    if (draft && open) {
      form.setFieldsValue(draft);
      setPreviewAmount(draft.amount || 0);
      setPixKeyType(draft.pixKeyType || '');
    }
  }, [draft, form, open]);

  // Save draft when form values change
  const handleFormChange = (changedValues: any, allValues: FormValues) => {
    saveDraft(allValues);

    if (changedValues.amount) {
      setPreviewAmount(changedValues.amount || 0);
    }

    if (changedValues.pixKeyType) {
      setPixKeyType(changedValues.pixKeyType);
      // Clear PIX key when type changes
      if (changedValues.pixKeyType !== pixKeyType) {
        form.setFieldValue('pixKey', '');
      }
    }
  };

  const validatePixKey = (pixKey: string, type: string): boolean => {
    const keyType = PIX_KEY_TYPES.find(t => t.value === type);
    if (!keyType) return false;

    return keyType.pattern.test(pixKey);
  };

  const handleSubmit = async (values: FormValues) => {
    try {
      const paymentData: PaymentFormData = {
        amount: values.amount,
        description: values.description,
        customerName: values.customerName,
        customerEmail: values.customerEmail,
        customerDocument: values.customerDocument,
        expirationMinutes: values.hasExpiration ? values.expirationMinutes : undefined,
      };

      // Add PIX key if provided
      if (values.pixKey && values.pixKeyType) {
        (paymentData as any).pixKey = values.pixKey;
        (paymentData as any).pixKeyType = values.pixKeyType;
      }

      const response = await createPaymentMutation.mutateAsync(paymentData);

      message.success('Pagamento PIX criado com sucesso!');
      clearDraft();
      form.resetFields();
      onSuccess?.(response.id);
      onClose();

    } catch (error) {
      // Error handled in mutation
    }
  };

  const handleClose = () => {
    const values = form.getFieldsValue();
    if (Object.values(values).some(v => v !== undefined && v !== '')) {
      // Save draft if form has data
      saveDraft(values);
    }
    onClose();
  };

  const getPixKeyPlaceholder = (type: string): string => {
    switch (type) {
      case 'CPF':
        return '12345678901';
      case 'CNPJ':
        return '12345678000123';
      case 'EMAIL':
        return 'usuario@exemplo.com';
      case 'PHONE':
        return '+5511999999999';
      case 'EVP':
        return '12345678-1234-1234-1234-123456789012';
      default:
        return 'Selecione o tipo primeiro';
    }
  };

  const getPixKeyValidationMessage = (type: string): string => {
    switch (type) {
      case 'CPF':
        return 'Digite um CPF válido (apenas números)';
      case 'CNPJ':
        return 'Digite um CNPJ válido (apenas números)';
      case 'EMAIL':
        return 'Digite um e-mail válido';
      case 'PHONE':
        return 'Digite um telefone válido (+5511999999999)';
      case 'EVP':
        return 'Digite uma chave aleatória válida (formato UUID)';
      default:
        return 'Chave PIX inválida';
    }
  };

  return (
    <Modal
      title={
        <Space>
          <DollarOutlined />
          <span>Criar Novo Pagamento PIX</span>
        </Space>
      }
      open={open}
      onCancel={handleClose}
      onOk={() => form.submit()}
      confirmLoading={createPaymentMutation.isPending}
      width={700}
      okText="Criar Pagamento"
      cancelText="Cancelar"
    >
      <Form
        form={form}
        layout="vertical"
        onFinish={handleSubmit}
        onValuesChange={handleFormChange}
        initialValues={{
          hasExpiration: false,
          expirationMinutes: 30,
        }}
      >
        {/* Payment Amount Preview */}
        {previewAmount > 0 && (
          <Alert
            message={
              <div style={{ textAlign: 'center' }}>
                <Text type="secondary">Valor do Pagamento</Text>
                <br />
                <Title level={3} style={{ margin: 0, color: 'var(--primary-color)' }}>
                  {formatCurrency(previewAmount)}
                </Title>
              </div>
            }
            type="info"
            style={{ marginBottom: 24, textAlign: 'center' }}
          />
        )}

        <Row gutter={[16, 0]}>
          <Col xs={24} md={12}>
            {/* Payment Information */}
            <Card size="small" title="Informações do Pagamento" style={{ marginBottom: 16 }}>
              <Form.Item
                name="amount"
                label="Valor"
                rules={[
                  { required: true, message: 'Digite o valor do pagamento' },
                  { type: 'number', min: 0.01, message: 'O valor deve ser maior que R$ 0,01' },
                  { type: 'number', max: 100000, message: 'O valor deve ser menor que R$ 100.000,00' },
                ]}
              >
                <InputNumber
                  style={{ width: '100%' }}
                  placeholder="0,00"
                  prefix="R$"
                  step={0.01}
                  precision={2}
                  formatter={(value) =>
                    value ? `${value}`.replace(/\B(?=(\d{3})+(?!\d))/g, '.') : ''
                  }
                  parser={(value) => value?.replace(/\$\s?|(\.\d{2})/g, '') as any}
                />
              </Form.Item>

              <Form.Item
                name="description"
                label="Descrição"
                rules={[
                  { max: 200, message: 'A descrição deve ter no máximo 200 caracteres' },
                ]}
              >
                <TextArea
                  rows={3}
                  placeholder="Descrição do pagamento (opcional)"
                  maxLength={200}
                  showCount
                />
              </Form.Item>
            </Card>

            {/* PIX Key Information */}
            <Card size="small" title="Chave PIX (Opcional)" style={{ marginBottom: 16 }}>
              <Form.Item
                name="pixKeyType"
                label="Tipo da Chave PIX"
              >
                <Select placeholder="Selecione o tipo da chave">
                  {PIX_KEY_TYPES.map(type => (
                    <Option key={type.value} value={type.value}>
                      {type.label}
                    </Option>
                  ))}
                </Select>
              </Form.Item>

              <Form.Item
                name="pixKey"
                label="Chave PIX"
                rules={[
                  {
                    validator: async (_, value) => {
                      if (!value) return; // Optional field

                      const keyType = form.getFieldValue('pixKeyType');
                      if (!keyType) {
                        throw new Error('Selecione o tipo da chave primeiro');
                      }

                      if (!validatePixKey(value, keyType)) {
                        throw new Error(getPixKeyValidationMessage(keyType));
                      }
                    },
                  },
                ]}
              >
                <Input
                  placeholder={getPixKeyPlaceholder(pixKeyType)}
                  prefix={<KeyOutlined />}
                  disabled={!pixKeyType}
                />
              </Form.Item>

              <Text type="secondary" style={{ fontSize: 12 }}>
                Se não informado, será usado a chave padrão do merchant
              </Text>
            </Card>
          </Col>

          <Col xs={24} md={12}>
            {/* Customer Information */}
            <Card size="small" title="Informações do Cliente (Opcional)" style={{ marginBottom: 16 }}>
              <Form.Item
                name="customerName"
                label="Nome do Cliente"
                rules={[
                  { max: 100, message: 'O nome deve ter no máximo 100 caracteres' },
                ]}
              >
                <Input
                  placeholder="Nome completo do cliente"
                  prefix={<UserOutlined />}
                />
              </Form.Item>

              <Form.Item
                name="customerEmail"
                label="E-mail do Cliente"
                rules={[
                  { type: 'email', message: 'Digite um e-mail válido' },
                ]}
              >
                <Input
                  placeholder="email@exemplo.com"
                  prefix={<MailOutlined />}
                />
              </Form.Item>

              <Form.Item
                name="customerDocument"
                label="Documento do Cliente"
                rules={[
                  {
                    validator: async (_, value) => {
                      if (!value) return; // Optional field

                      const cleanValue = value.replace(/\D/g, '');

                      if (cleanValue.length === 11) {
                        if (!isValidCPF(cleanValue)) {
                          throw new Error('CPF inválido');
                        }
                      } else if (cleanValue.length === 14) {
                        if (!isValidCNPJ(cleanValue)) {
                          throw new Error('CNPJ inválido');
                        }
                      } else {
                        throw new Error('Digite um CPF ou CNPJ válido');
                      }
                    },
                  },
                ]}
              >
                <Input
                  placeholder="CPF ou CNPJ (apenas números)"
                  prefix={<IdcardOutlined />}
                />
              </Form.Item>
            </Card>

            {/* Expiration Configuration */}
            <Card size="small" title="Configurações de Expiração" style={{ marginBottom: 16 }}>
              <Form.Item
                name="hasExpiration"
                valuePropName="checked"
                style={{ marginBottom: 12 }}
              >
                <Switch
                  checkedChildren="Com expiração"
                  unCheckedChildren="Sem expiração"
                />
              </Form.Item>

              <Form.Item
                noStyle
                shouldUpdate={(prevValues, curValues) =>
                  prevValues.hasExpiration !== curValues.hasExpiration
                }
              >
                {({ getFieldValue }) =>
                  getFieldValue('hasExpiration') && (
                    <Form.Item
                      name="expirationMinutes"
                      label="Tempo de Expiração (minutos)"
                      rules={[
                        { required: true, message: 'Digite o tempo de expiração' },
                        { type: 'number', min: 5, message: 'Mínimo de 5 minutos' },
                        { type: 'number', max: 10080, message: 'Máximo de 7 dias (10080 minutos)' },
                      ]}
                    >
                      <InputNumber
                        style={{ width: '100%' }}
                        placeholder="30"
                        min={5}
                        max={10080}
                        prefix={<ClockCircleOutlined />}
                        formatter={(value) => `${value} min`}
                        parser={(value) => value?.replace(' min', '') as any}
                      />
                    </Form.Item>
                  )
                }
              </Form.Item>

              <Text type="secondary" style={{ fontSize: 12 }}>
                Pagamentos sem expiração ficam ativos até serem pagos ou cancelados
              </Text>
            </Card>
          </Col>
        </Row>

        <Divider />

        <Alert
          message="Informações Importantes"
          description={
            <ul style={{ marginBottom: 0, paddingLeft: 16 }}>
              <li>O pagamento será criado com status "ATIVO" e ficará aguardando o pagamento</li>
              <li>Um QR Code PIX será gerado automaticamente para o pagamento</li>
              <li>Você será notificado quando o pagamento for concluído</li>
              <li>O valor mínimo é R$ 0,01 e o máximo é R$ 100.000,00</li>
            </ul>
          }
          type="info"
          showIcon
          style={{ marginTop: 16 }}
        />
      </Form>
    </Modal>
  );
};

export default CreatePaymentModal;