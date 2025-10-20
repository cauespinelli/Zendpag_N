// @ts-nocheck
import React from 'react';
import { Card, Form, Input, Button, Switch, Typography, Space, Divider, message } from 'antd';
import { SaveOutlined, KeyOutlined } from '@ant-design/icons';

const { Title, Text } = Typography;

const SettingsPage: React.FC = () => {
  const [form] = Form.useForm();

  const handleSave = (values: any) => {
    message.success('Configurações salvas com sucesso!');
  };

  return (
    <div>
      <Title level={2}>Configurações</Title>
      <Text type="secondary">Gerencie as configurações do sistema</Text>

      <Card title="Configurações Gerais" style={{ marginTop: 24 }}>
        <Form form={form} layout="vertical" onFinish={handleSave}>
          <Form.Item name="companyName" label="Nome da Empresa">
            <Input placeholder="Nome da sua empresa" />
          </Form.Item>

          <Form.Item name="email" label="Email para Notificações">
            <Input placeholder="email@exemplo.com" />
          </Form.Item>

          <Form.Item name="phone" label="Telefone">
            <Input placeholder="(00) 00000-0000" />
          </Form.Item>

          <Divider />

          <Form.Item name="notifications" label="Notificações" valuePropName="checked">
            <Switch checkedChildren="Ativado" unCheckedChildren="Desativado" />
          </Form.Item>

          <Form.Item name="emailAlerts" label="Alertas por Email" valuePropName="checked">
            <Switch checkedChildren="Ativado" unCheckedChildren="Desativado" />
          </Form.Item>

          <Form.Item name="twoFactor" label="Autenticação de Dois Fatores" valuePropName="checked">
            <Switch checkedChildren="Ativado" unCheckedChildren="Desativado" />
          </Form.Item>

          <Form.Item>
            <Button type="primary" htmlType="submit" icon={<SaveOutlined />} size="large">
              Salvar Configurações
            </Button>
          </Form.Item>
        </Form>
      </Card>

      <Card title="Chave API" style={{ marginTop: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Text>Use esta chave para integrar com a API do ZendaPag</Text>
          <Input.Password
            readOnly
            value="sk_live_xxxxxxxxxxxxxxxxxxxxxxxxxx"
            addonAfter={<KeyOutlined />}
          />
          <Button type="primary" danger>
            Gerar Nova Chave
          </Button>
        </Space>
      </Card>

      <Card title="Segurança" style={{ marginTop: 16 }}>
        <Space direction="vertical" style={{ width: '100%' }}>
          <Button type="primary">Alterar Senha</Button>
          <Button danger>Revogar Todas as Sessões</Button>
        </Space>
      </Card>
    </div>
  );
};

export default SettingsPage;
