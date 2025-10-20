// @ts-nocheck
import React from 'react';
import { Card, Descriptions, Avatar, Typography, Button, Space, Tag } from 'antd';
import { UserOutlined, MailOutlined } from '@ant-design/icons';
import { useAuthStore } from '@/store/authStore';
import { formatDate } from '@/utils/helpers';

const { Title } = Typography;

const ProfilePage: React.FC = () => {
  const user = useAuthStore((state) => state.user);

  return (
    <div>
      <Title level={2}>Perfil do Usuário</Title>

      <Card style={{ marginTop: 24 }}>
        <Space direction="vertical" size="large" style={{ width: '100%' }}>
          <div style={{ textAlign: 'center' }}>
            <Avatar size={100} icon={<UserOutlined />} />
            <Title level={3} style={{ marginTop: 16 }}>
              {user?.name}
            </Title>
            <Space>
              {user?.roles.map((role) => (
                <Tag key={role} color="blue">
                  {role}
                </Tag>
              ))}
            </Space>
          </div>

          <Descriptions bordered column={1}>
            <Descriptions.Item label="Email">
              <Space>
                <MailOutlined />
                {user?.email}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="ID do Usuário">{user?.id}</Descriptions.Item>
            <Descriptions.Item label="ID do Merchant">{user?.merchantId}</Descriptions.Item>
            <Descriptions.Item label="Criado em">
              {user?.createdAt ? formatDate(user.createdAt) : '-'}
            </Descriptions.Item>
            <Descriptions.Item label="Permissões">
              <Space wrap>
                {user?.permissions.map((perm) => (
                  <Tag key={perm}>{perm}</Tag>
                ))}
              </Space>
            </Descriptions.Item>
          </Descriptions>

          <div style={{ textAlign: 'center' }}>
            <Button type="primary" size="large">
              Editar Perfil
            </Button>
          </div>
        </Space>
      </Card>
    </div>
  );
};

export default ProfilePage;
