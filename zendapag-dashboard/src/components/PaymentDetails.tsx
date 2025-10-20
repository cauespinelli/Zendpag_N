// @ts-nocheck
// @ts-nocheck
import React, { useState } from 'react';
import {
  Card,
  Descriptions,
  Typography,
  Space,
  Tag,
  Button,
  Modal,
  QRCode,
  Divider,
  Alert,
  Tooltip,
  Row,
  Col,
  Timeline,
  Image,
  message,
  Spin,
} from 'antd';
import {
  CopyOutlined,
  QrcodeOutlined,
  StopOutlined,
  ReloadOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  EyeOutlined,
  EyeInvisibleOutlined,
} from '@ant-design/icons';
import dayjs from 'dayjs';

import { usePayment, usePaymentQrCode, useCancelPayment } from '@/hooks/useQuery';
import { formatCurrency, formatDate, getPaymentStatusColor, getPaymentStatusText, maskPixKey } from '@/utils/helpers';
import type { Payment } from '@/types';

const { Title, Text, Paragraph } = Typography;

interface PaymentDetailsProps {
  paymentId: string;
  onClose?: () => void;
  embedded?: boolean;
}

const PaymentStatusTag: React.FC<{ status: Payment['status'] }> = ({ status }) => {
  const getStatusIcon = () => {
    switch (status) {
      case 'COMPLETED':
        return <CheckCircleOutlined />;
      case 'PENDING':
      case 'ACTIVE':
        return <ClockCircleOutlined />;
      case 'FAILED':
      case 'CANCELLED':
        return <CloseCircleOutlined />;
      case 'EXPIRED':
        return <ExclamationCircleOutlined />;
      default:
        return null;
    }
  };

  return (
    <Tag
      color={getPaymentStatusColor(status)}
      icon={getStatusIcon()}
      style={{ borderRadius: 12, fontWeight: 500, padding: '4px 12px' }}
    >
      {getPaymentStatusText(status)}
    </Tag>
  );
};

const PaymentDetails: React.FC<PaymentDetailsProps> = ({
  paymentId,
  onClose,
  embedded = false,
}) => {
  const [qrCodeModalVisible, setQrCodeModalVisible] = useState(false);
  const [showPixKey, setShowPixKey] = useState(false);
  const [cancelModalVisible, setCancelModalVisible] = useState(false);

  // Data fetching
  const {
    data: payment,
    isLoading: paymentLoading,
    refetch: refetchPayment,
  } = usePayment(paymentId);

  const {
    data: qrCodeData,
    isLoading: qrCodeLoading,
  } = usePaymentQrCode(paymentId);

  // Mutations
  const cancelMutation = useCancelPayment();

  const handleCopyToClipboard = async (text: string, label: string) => {
    try {
      await navigator.clipboard.writeText(text);
      message.success(`${label} copiado para a área de transferência`);
    } catch (error) {
      message.error('Erro ao copiar para a área de transferência');
    }
  };

  const handleCancelPayment = async () => {
    if (!payment) return;

    try {
      await cancelMutation.mutateAsync({
        id: payment.id,
        reason: 'Cancelado pelo usuário',
      });
      setCancelModalVisible(false);
      refetchPayment();
      message.success('Pagamento cancelado com sucesso');
    } catch (error) {
      // Error handled in mutation
    }
  };

  const getPaymentTimeline = () => {
    if (!payment) return [];

    const events = [
      {
        color: 'blue',
        dot: <ClockCircleOutlined />,
        children: (
          <div>
            <Text strong>Pagamento Criado</Text>
            <br />
            <Text type="secondary">{formatDate(payment.createdAt)}</Text>
          </div>
        ),
      },
    ];

    if (payment.processedAt) {
      events.push({
        color: 'orange',
        dot: <ClockCircleOutlined />,
        children: (
          <div>
            <Text strong>Processamento Iniciado</Text>
            <br />
            <Text type="secondary">{formatDate(payment.processedAt)}</Text>
          </div>
        ),
      });
    }

    if (payment.paidAt) {
      events.push({
        color: 'green',
        dot: <CheckCircleOutlined />,
        children: (
          <div>
            <Text strong>Pagamento Concluído</Text>
            <br />
            <Text type="secondary">{formatDate(payment.paidAt)}</Text>
            {payment.payerName && (
              <>
                <br />
                <Text type="secondary">Por: {payment.payerName}</Text>
              </>
            )}
          </div>
        ),
      });
    }

    if (payment.status === 'FAILED' && payment.failureReason) {
      events.push({
        color: 'red',
        dot: <CloseCircleOutlined />,
        children: (
          <div>
            <Text strong>Pagamento Falhou</Text>
            <br />
            <Text type="secondary">{payment.failureReason}</Text>
          </div>
        ),
      });
    }

    if (payment.status === 'CANCELLED' && payment.cancellationReason) {
      events.push({
        color: 'gray',
        dot: <CloseCircleOutlined />,
        children: (
          <div>
            <Text strong>Pagamento Cancelado</Text>
            <br />
            <Text type="secondary">{payment.cancellationReason}</Text>
          </div>
        ),
      });
    }

    return events;
  };

  if (paymentLoading) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Spin size="large" />
          <br />
          <br />
          <Text>Carregando detalhes do pagamento...</Text>
        </div>
      </Card>
    );
  }

  if (!payment) {
    return (
      <Card>
        <div style={{ textAlign: 'center', padding: '40px 0' }}>
          <Text type="secondary">Pagamento não encontrado</Text>
        </div>
      </Card>
    );
  }

  const canCancel = ['PENDING', 'ACTIVE'].includes(payment.status);
  const isExpired = payment.expiresAt && dayjs(payment.expiresAt).isBefore(dayjs());

  return (
    <div className="payment-details">
      {/* Header */}
      <Card style={{ marginBottom: 16 }}>
        <Row justify="space-between" align="middle">
          <Col>
            <Space direction="vertical" size={0}>
              <Title level={4} style={{ margin: 0 }}>
                Pagamento #{payment.referenceId}
              </Title>
              <Text type="secondary">ID: {payment.id}</Text>
            </Space>
          </Col>
          <Col>
            <Space>
              <PaymentStatusTag status={payment.status} />
              <Button icon={<ReloadOutlined />} onClick={refetchPayment}>
                Atualizar
              </Button>
              {canCancel && (
                <Button
                  danger
                  icon={<StopOutlined />}
                  onClick={() => setCancelModalVisible(true)}
                  loading={cancelMutation.isPending}
                >
                  Cancelar
                </Button>
              )}
            </Space>
          </Col>
        </Row>
      </Card>

      {/* Alerts */}
      {isExpired && payment.status === 'ACTIVE' && (
        <Alert
          message="Pagamento Expirado"
          description="Este pagamento expirou e não pode mais ser processado"
          type="warning"
          style={{ marginBottom: 16 }}
          showIcon
        />
      )}

      <Row gutter={[16, 16]}>
        <Col xs={24} lg={16}>
          {/* Payment Information */}
          <Card title="Informações do Pagamento" style={{ marginBottom: 16 }}>
            <Descriptions column={1} size="small">
              <Descriptions.Item label="Valor">
                <Text strong style={{ fontSize: 18, color: 'var(--primary-color)' }}>
                  {formatCurrency(payment.amount)}
                </Text>
              </Descriptions.Item>
              <Descriptions.Item label="Moeda">
                {payment.currency}
              </Descriptions.Item>
              <Descriptions.Item label="Descrição">
                {payment.description || 'N/A'}
              </Descriptions.Item>
              <Descriptions.Item label="Data de Criação">
                {formatDate(payment.createdAt)}
              </Descriptions.Item>
              {payment.expiresAt && (
                <Descriptions.Item label="Data de Expiração">
                  <Space>
                    {formatDate(payment.expiresAt)}
                    {isExpired && <Tag color="red">Expirado</Tag>}
                  </Space>
                </Descriptions.Item>
              )}
              {payment.paidAt && (
                <Descriptions.Item label="Data do Pagamento">
                  {formatDate(payment.paidAt)}
                </Descriptions.Item>
              )}
            </Descriptions>
          </Card>

          {/* PIX Information */}
          <Card title="Informações PIX" style={{ marginBottom: 16 }}>
            <Descriptions column={1} size="small">
              {payment.pixTxId && (
                <Descriptions.Item label="Transaction ID">
                  <Space>
                    <Text code>{payment.pixTxId}</Text>
                    <Button
                      type="text"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => handleCopyToClipboard(payment.pixTxId!, 'Transaction ID')}
                    />
                  </Space>
                </Descriptions.Item>
              )}
              {payment.pixKey && (
                <Descriptions.Item label="Chave PIX">
                  <Space>
                    <Text code>
                      {showPixKey
                        ? payment.pixKey
                        : maskPixKey(payment.pixKey, payment.pixKeyType)
                      }
                    </Text>
                    <Tag size="small" color="blue">
                      {payment.pixKeyType}
                    </Tag>
                    <Button
                      type="text"
                      size="small"
                      icon={showPixKey ? <EyeInvisibleOutlined /> : <EyeOutlined />}
                      onClick={() => setShowPixKey(!showPixKey)}
                    />
                    <Button
                      type="text"
                      size="small"
                      icon={<CopyOutlined />}
                      onClick={() => handleCopyToClipboard(payment.pixKey!, 'Chave PIX')}
                    />
                  </Space>
                </Descriptions.Item>
              )}
            </Descriptions>

            {(['PENDING', 'ACTIVE'].includes(payment.status) && qrCodeData) && (
              <div style={{ marginTop: 16 }}>
                <Space>
                  <Button
                    type="primary"
                    icon={<QrcodeOutlined />}
                    onClick={() => setQrCodeModalVisible(true)}
                  >
                    Ver QR Code
                  </Button>
                  <Button
                    icon={<CopyOutlined />}
                    onClick={() => handleCopyToClipboard(qrCodeData.qrCodeText, 'Código PIX')}
                  >
                    Copiar Código
                  </Button>
                </Space>
              </div>
            )}
          </Card>

          {/* Customer Information */}
          {(payment.customerName || payment.customerEmail || payment.customerDocument) && (
            <Card title="Informações do Cliente" style={{ marginBottom: 16 }}>
              <Descriptions column={1} size="small">
                {payment.customerName && (
                  <Descriptions.Item label="Nome">
                    {payment.customerName}
                  </Descriptions.Item>
                )}
                {payment.customerEmail && (
                  <Descriptions.Item label="E-mail">
                    {payment.customerEmail}
                  </Descriptions.Item>
                )}
                {payment.customerDocument && (
                  <Descriptions.Item label="Documento">
                    {payment.customerDocument}
                  </Descriptions.Item>
                )}
              </Descriptions>
            </Card>
          )}

          {/* Payer Information */}
          {(payment.payerName || payment.payerDocument) && (
            <Card title="Informações do Pagador" style={{ marginBottom: 16 }}>
              <Descriptions column={1} size="small">
                {payment.payerName && (
                  <Descriptions.Item label="Nome">
                    {payment.payerName}
                  </Descriptions.Item>
                )}
                {payment.payerDocument && (
                  <Descriptions.Item label="Documento">
                    {payment.payerDocument}
                  </Descriptions.Item>
                )}
                {payment.payerBank && (
                  <Descriptions.Item label="Banco">
                    {payment.payerBank}
                  </Descriptions.Item>
                )}
              </Descriptions>
            </Card>
          )}
        </Col>

        <Col xs={24} lg={8}>
          {/* Timeline */}
          <Card title="Histórico" style={{ marginBottom: 16 }}>
            <Timeline items={getPaymentTimeline()} />
          </Card>
        </Col>
      </Row>

      {/* QR Code Modal */}
      <Modal
        title="QR Code PIX"
        open={qrCodeModalVisible}
        onCancel={() => setQrCodeModalVisible(false)}
        footer={[
          <Button key="close" onClick={() => setQrCodeModalVisible(false)}>
            Fechar
          </Button>,
          <Button
            key="copy"
            type="primary"
            icon={<CopyOutlined />}
            onClick={() =>
              qrCodeData &&
              handleCopyToClipboard(qrCodeData.qrCodeText, 'Código PIX')
            }
          >
            Copiar Código
          </Button>,
        ]}
        width={400}
      >
        {qrCodeLoading ? (
          <div style={{ textAlign: 'center', padding: '40px 0' }}>
            <Spin />
          </div>
        ) : qrCodeData ? (
          <div style={{ textAlign: 'center' }}>
            <QRCode
              value={qrCodeData.qrCodeText}
              size={256}
              style={{ marginBottom: 16 }}
            />
            <Paragraph
              copyable={{
                text: qrCodeData.qrCodeText,
                tooltips: ['Copiar', 'Copiado!'],
              }}
              style={{
                fontSize: 12,
                fontFamily: 'monospace',
                wordBreak: 'break-all',
                background: '#f5f5f5',
                padding: 8,
                borderRadius: 4,
                maxHeight: 100,
                overflow: 'auto',
              }}
            >
              {qrCodeData.qrCodeText}
            </Paragraph>
            <Text type="secondary" style={{ fontSize: 12 }}>
              Escaneie o QR Code ou copie o código PIX para realizar o pagamento
            </Text>
          </div>
        ) : (
          <div style={{ textAlign: 'center' }}>
            <Text type="secondary">QR Code não disponível</Text>
          </div>
        )}
      </Modal>

      {/* Cancel Modal */}
      <Modal
        title="Cancelar Pagamento"
        open={cancelModalVisible}
        onCancel={() => setCancelModalVisible(false)}
        onOk={handleCancelPayment}
        confirmLoading={cancelMutation.isPending}
        okText="Sim, Cancelar"
        cancelText="Não"
        okButtonProps={{ danger: true }}
      >
        <p>
          Tem certeza que deseja cancelar este pagamento?
        </p>
        <p>
          <Text strong>Valor:</Text> {formatCurrency(payment.amount)}
          <br />
          <Text strong>ID:</Text> {payment.referenceId}
        </p>
        <Alert
          message="Esta ação não pode ser desfeita"
          type="warning"
          style={{ marginTop: 16 }}
        />
      </Modal>
    </div>
  );
};

export default PaymentDetails;