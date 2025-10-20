// @ts-nocheck
import React, { useEffect, useState } from 'react';
import {
  Table,
  Card,
  Button,
  Modal,
  Form,
  Input,
  Select,
  Switch,
  Tag,
  Space,
  Typography,
  message,
  Row,
  Col,
  Statistic,
} from 'antd';
import {
  PlusOutlined,
  EditOutlined,
  DeleteOutlined,
  ThunderboltOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
} from '@ant-design/icons';
import apiService from '@/services/api';
import { API_ENDPOINTS, WEBHOOK_EVENTS } from '@/utils/constants';
import { formatDate } from '@/utils/helpers';
import type { Webhook } from '@/types';

const { Title, Text } = Typography;
const { Option } = Select;

const WebhooksPage: React.FC = () => {
  const [loading, setLoading] = useState(false);
  const [webhooks, setWebhooks] = useState<Webhook[]>([]);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingWebhook, setEditingWebhook] = useState<Webhook | null>(null);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchWebhooks();
  }, []);

  const fetchWebhooks = async () => {
    try {
      setLoading(true);
      const response = await apiService.getPaginated<Webhook>(API_ENDPOINTS.WEBHOOKS.BASE);
      setWebhooks(response.content);
    } catch (error: any) {
      message.error('Erro ao carregar webhooks');
    } finally {
      setLoading(false);
    }
  };

  const handleSave = async (values: any) => {
    try {
      if (editingWebhook) {
        await apiService.put(API_ENDPOINTS.WEBHOOKS.BY_ID(editingWebhook.id), values);
        message.success('Webhook atualizado com sucesso!');
      } else {
        await apiService.post(API_ENDPOINTS.WEBHOOKS.BASE, values);
        message.success('Webhook criado com sucesso!');
      }
      setModalVisible(false);
      setEditingWebhook(null);
      form.resetFields();
      fetchWebhooks();
    } catch (error: any) {
      message.error(error.message || 'Erro ao salvar webhook');
    }
  };

  const handleEdit = (webhook: Webhook) => {
    setEditingWebhook(webhook);
    form.setFieldsValue(webhook);
    setModalVisible(true);
  };

  const handleDelete = (webhookId: string) => {
    Modal.confirm({
      title: 'Confirmar exclusão',
      content: 'Tem certeza que deseja excluir este webhook?',
      okText: 'Sim, excluir',
      okType: 'danger',
      cancelText: 'Não',
      onOk: async () => {
        try {
          await apiService.delete(API_ENDPOINTS.WEBHOOKS.BY_ID(webhookId));
          message.success('Webhook excluído com sucesso');
          fetchWebhooks();
        } catch (error: any) {
          message.error('Erro ao excluir webhook');
        }
      },
    });
  };

  const handleTest = async (webhookId: string) => {
    try {
      await apiService.post(API_ENDPOINTS.WEBHOOKS.TEST(webhookId));
      message.success('Webhook testado com sucesso! Verifique sua URL.');
    } catch (error: any) {
      message.error('Erro ao testar webhook');
    }
  };

  const columns = [
    {
      title: 'URL',
      dataIndex: 'url',
      key: 'url',
      width: 300,
      ellipsis: true,
      render: (url: string) => <Text copyable>{url}</Text>,
    },
    {
      title: 'Eventos',
      dataIndex: 'events',
      key: 'events',
      render: (events: string[]) => (
        <Space wrap>
          {events.map((event) => (
            <Tag key={event} color="blue">
              {WEBHOOK_EVENTS[event] || event}
            </Tag>
          ))}
        </Space>
      ),
    },
    {
      title: 'Status',
      dataIndex: 'active',
      key: 'active',
      width: 100,
      render: (active: boolean) =>
        active ? (
          <Tag icon={<CheckCircleOutlined />} color="success">
            Ativo
          </Tag>
        ) : (
          <Tag icon={<CloseCircleOutlined />} color="default">
            Inativo
          </Tag>
        ),
    },
    {
      title: 'Sucesso / Falhas',
      key: 'stats',
      width: 150,
      render: (record: Webhook) => (
        <Space>
          <Text type="success">{record.successCount || 0}</Text>
          <Text>/</Text>
          <Text type="danger">{record.failureCount || 0}</Text>
        </Space>
      ),
    },
    {
      title: 'Criado em',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
      render: (date: string) => formatDate(date),
    },
    {
      title: 'Ações',
      key: 'actions',
      width: 200,
      render: (record: Webhook) => (
        <Space>
          <Button
            size="small"
            icon={<ThunderboltOutlined />}
            onClick={() => handleTest(record.id)}
          >
            Testar
          </Button>
          <Button
            size="small"
            icon={<EditOutlined />}
            onClick={() => handleEdit(record)}
          >
            Editar
          </Button>
          <Button
            size="small"
            danger
            icon={<DeleteOutlined />}
            onClick={() => handleDelete(record.id)}
          >
            Excluir
          </Button>
        </Space>
      ),
    },
  ];

  const totalSuccess = webhooks.reduce((sum, w) => sum + (w.successCount || 0), 0);
  const totalFailures = webhooks.reduce((sum, w) => sum + (w.failureCount || 0), 0);
  const activeCount = webhooks.filter((w) => w.active).length;

  return (
    <div>
      <Row justify="space-between" align="middle" style={{ marginBottom: 24 }}>
        <Col>
          <Title level={2} style={{ margin: 0 }}>
            Webhooks
          </Title>
          <Text type="secondary">Configure notificações automáticas para eventos PIX</Text>
        </Col>
        <Col>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditingWebhook(null);
              form.resetFields();
              setModalVisible(true);
            }}
            size="large"
          >
            Novo Webhook
          </Button>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="Total de Webhooks" value={webhooks.length} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic title="Webhooks Ativos" value={activeCount} valueStyle={{ color: '#52c41a' }} />
          </Card>
        </Col>
        <Col xs={24} sm={8}>
          <Card>
            <Statistic
              title="Taxa de Sucesso"
              value={totalSuccess + totalFailures > 0 ? ((totalSuccess / (totalSuccess + totalFailures)) * 100).toFixed(1) : 0}
              suffix="%"
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
      </Row>

      <Card>
        <Table
          columns={columns}
          dataSource={webhooks}
          rowKey="id"
          loading={loading}
          pagination={false}
          scroll={{ x: 1000 }}
        />
      </Card>

      <Modal
        title={editingWebhook ? 'Editar Webhook' : 'Novo Webhook'}
        open={modalVisible}
        onCancel={() => {
          setModalVisible(false);
          setEditingWebhook(null);
          form.resetFields();
        }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSave} initialValues={{ active: true, events: [] }}>
          <Form.Item
            name="url"
            label="URL do Webhook"
            rules={[
              { required: true, message: 'Por favor, insira a URL' },
              { type: 'url', message: 'URL inválida' },
            ]}
          >
            <Input placeholder="https://api.seusite.com/webhook" />
          </Form.Item>

          <Form.Item
            name="events"
            label="Eventos"
            rules={[{ required: true, message: 'Selecione pelo menos um evento' }]}
          >
            <Select mode="multiple" placeholder="Selecione os eventos">
              {Object.entries(WEBHOOK_EVENTS).map(([key, label]) => (
                <Option key={key} value={key}>
                  {label}
                </Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item name="secret" label="Secret (opcional)">
            <Input.Password placeholder="Chave secreta para validação" />
          </Form.Item>

          <Form.Item name="active" label="Status" valuePropName="checked">
            <Switch checkedChildren="Ativo" unCheckedChildren="Inativo" />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default WebhooksPage;
