// @ts-nocheck
import React, { useState, useEffect } from 'react';
import { Form, Input, Button, Card, Typography, Checkbox, Alert, Space } from 'antd';
import { UserOutlined, LockOutlined, EyeInvisibleOutlined, EyeTwoTone } from '@ant-design/icons';
import { Navigate, useLocation } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';
import { themeSelectors } from '@/store/themeStore';
import type { LoginRequest } from '@/types';

const { Title, Text, Link } = Typography;

interface LocationState {
  from?: Location;
  error?: string;
}

const LoginPage: React.FC = () => {
  const [form] = Form.useForm<LoginRequest>();
  const location = useLocation();
  const state = location.state as LocationState;

  const {
    login,
    isAuthenticated,
    isLoading,
    error: authError,
    clearError
  } = useAuthStore();

  const primaryColor = themeSelectors.usePrimaryColor();
  const [loginError, setLoginError] = useState<string | null>(null);

  // Clear errors when component mounts or form values change
  useEffect(() => {
    clearError();
    setLoginError(null);
  }, [clearError]);

  useEffect(() => {
    if (authError) {
      setLoginError(authError);
    }
  }, [authError]);

  // Show error from state (e.g., session expired)
  useEffect(() => {
    if (state?.error) {
      setLoginError(state.error);
    }
  }, [state]);

  // Redirect if already authenticated
  if (isAuthenticated) {
    const redirectTo = (state?.from as any)?.pathname || '/dashboard';
    return <Navigate to={redirectTo} replace />;
  }

  const handleSubmit = async (values: LoginRequest) => {
    setLoginError(null);
    clearError();

    try {
      await login(values);
      // Navigation will happen automatically via auth state change
    } catch (error) {
      // Error is already handled in the store and will show via authError
      console.error('Login error:', error);
    }
  };

  const handleFormChange = () => {
    if (loginError) {
      setLoginError(null);
    }
    if (authError) {
      clearError();
    }
  };

  return (
    <div
      className="login-page"
      style={{
        minHeight: '100vh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        background: `linear-gradient(135deg, ${primaryColor}15 0%, ${primaryColor}05 100%)`,
        padding: '20px'
      }}
    >
      <div style={{ width: '100%', maxWidth: 400 }}>
        {/* Logo and branding */}
        <div style={{ textAlign: 'center', marginBottom: 40 }}>
          <div style={{ marginBottom: 16 }}>
            <svg
              width="80"
              height="80"
              viewBox="0 0 80 80"
              fill="none"
              xmlns="http://www.w3.org/2000/svg"
            >
              <circle
                cx="40"
                cy="40"
                r="36"
                fill={primaryColor}
                opacity="0.1"
              />
              <path
                d="M25 30L40 45L55 30"
                stroke={primaryColor}
                strokeWidth="4"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
              <path
                d="M25 50L40 35L55 50"
                stroke={primaryColor}
                strokeWidth="4"
                strokeLinecap="round"
                strokeLinejoin="round"
              />
            </svg>
          </div>

          <Title level={2} style={{ margin: 0, color: primaryColor }}>
            ZendaPag
          </Title>
          <Text type="secondary" style={{ fontSize: 16 }}>
            Plataforma de Pagamentos PIX
          </Text>
        </div>

        <Card
          className="login-card"
          style={{
            boxShadow: '0 8px 32px rgba(0, 0, 0, 0.1)',
            borderRadius: 12,
            border: '1px solid var(--border-color)'
          }}
        >
          <div style={{ textAlign: 'center', marginBottom: 24 }}>
            <Title level={3} style={{ marginBottom: 8 }}>
              Faça seu login
            </Title>
            <Text type="secondary">
              Acesse sua conta para continuar
            </Text>
          </div>

          {loginError && (
            <Alert
              message={loginError}
              type="error"
              showIcon
              closable
              onClose={() => setLoginError(null)}
              style={{ marginBottom: 24 }}
            />
          )}

          <Form
            form={form}
            name="login"
            onFinish={handleSubmit}
            onValuesChange={handleFormChange}
            layout="vertical"
            size="large"
            autoComplete="off"
          >
            <Form.Item
              name="email"
              label="E-mail"
              rules={[
                { required: true, message: 'Digite seu e-mail' },
                { type: 'email', message: 'Digite um e-mail válido' }
              ]}
            >
              <Input
                prefix={<UserOutlined />}
                placeholder="seu@email.com"
                autoComplete="email"
              />
            </Form.Item>

            <Form.Item
              name="password"
              label="Senha"
              rules={[
                { required: true, message: 'Digite sua senha' },
                { min: 6, message: 'A senha deve ter pelo menos 6 caracteres' }
              ]}
            >
              <Input.Password
                prefix={<LockOutlined />}
                placeholder="Digite sua senha"
                autoComplete="current-password"
                iconRender={(visible) => (visible ? <EyeTwoTone /> : <EyeInvisibleOutlined />)}
              />
            </Form.Item>

            <Form.Item name="rememberMe" valuePropName="checked">
              <Checkbox>Lembrar de mim</Checkbox>
            </Form.Item>

            <Form.Item style={{ marginBottom: 16 }}>
              <Button
                type="primary"
                htmlType="submit"
                loading={isLoading}
                block
                style={{
                  height: 44,
                  fontSize: 16,
                  fontWeight: 500
                }}
              >
                {isLoading ? 'Entrando...' : 'Entrar'}
              </Button>
            </Form.Item>

            <div style={{ textAlign: 'center' }}>
              <Space split={<span style={{ color: 'var(--text-tertiary)' }}>•</span>}>
                <Link href="#" onClick={(e) => e.preventDefault()}>
                  Esqueci minha senha
                </Link>
                <Link href="#" onClick={(e) => e.preventDefault()}>
                  Criar conta
                </Link>
              </Space>
            </div>
          </Form>
        </Card>

        {/* Footer */}
        <div style={{ textAlign: 'center', marginTop: 24 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            © 2024 ZendaPag. Todos os direitos reservados.
          </Text>
        </div>

        {/* Development helper */}
        {process.env.NODE_ENV === 'development' && (
          <Card
            size="small"
            title="Demo Login"
            style={{
              marginTop: 16,
              background: '#f8f9fa',
              border: '1px dashed #dee2e6'
            }}
          >
            <Space direction="vertical" size="small" style={{ width: '100%' }}>
              <Text type="secondary" style={{ fontSize: 12 }}>
                Para desenvolvimento:
              </Text>
              <Button
                size="small"
                type="link"
                onClick={() => {
                  form.setFieldsValue({
                    email: 'admin@zendapag.com',
                    password: 'admin123',
                    rememberMe: true
                  });
                }}
                style={{ padding: 0, height: 'auto' }}
              >
                Preencher como Admin
              </Button>
              <Button
                size="small"
                type="link"
                onClick={() => {
                  form.setFieldsValue({
                    email: 'merchant@example.com',
                    password: 'merchant123',
                    rememberMe: false
                  });
                }}
                style={{ padding: 0, height: 'auto' }}
              >
                Preencher como Merchant
              </Button>
            </Space>
          </Card>
        )}
      </div>
    </div>
  );
};

export default LoginPage;