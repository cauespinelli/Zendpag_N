// @ts-nocheck
import React from 'react';
import { Result, Button, Typography, Collapse, Space } from 'antd';
import { BugOutlined, ReloadOutlined, HomeOutlined } from '@ant-design/icons';
import { useNavigate } from 'react-router-dom';

const { Text, Paragraph } = Typography;
const { Panel } = Collapse;

interface ErrorFallbackProps {
  error: Error;
  resetErrorBoundary: () => void;
}

const ErrorFallback: React.FC<ErrorFallbackProps> = ({ error, resetErrorBoundary }) => {
  const navigate = useNavigate();

  const handleGoHome = () => {
    navigate('/dashboard');
    resetErrorBoundary();
  };

  const handleReload = () => {
    window.location.reload();
  };

  const errorDetails = {
    message: error.message,
    stack: error.stack,
    timestamp: new Date().toISOString(),
    userAgent: navigator.userAgent,
    url: window.location.href,
  };

  const copyErrorDetails = () => {
    const errorText = JSON.stringify(errorDetails, null, 2);
    navigator.clipboard.writeText(errorText).then(() => {
      console.log('Error details copied to clipboard');
    });
  };

  return (
    <div className="error-fallback-container" style={{
      minHeight: '100vh',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      background: 'var(--background-color)',
      padding: '20px'
    }}>
      <div style={{ maxWidth: 600, width: '100%' }}>
        <Result
          status="500"
          title="Oops! Algo deu errado"
          subTitle="Ocorreu um erro inesperado. Nossa equipe foi notificada e está investigando o problema."
          icon={<BugOutlined style={{ color: '#ff4d4f' }} />}
          extra={
            <Space direction="vertical" size="middle" style={{ width: '100%' }}>
              <Space wrap>
                <Button
                  type="primary"
                  icon={<ReloadOutlined />}
                  onClick={resetErrorBoundary}
                >
                  Tentar Novamente
                </Button>
                <Button
                  icon={<HomeOutlined />}
                  onClick={handleGoHome}
                >
                  Ir para Dashboard
                </Button>
                <Button onClick={handleReload}>
                  Recarregar Página
                </Button>
              </Space>

              {process.env.NODE_ENV === 'development' && (
                <Collapse ghost style={{ width: '100%' }}>
                  <Panel
                    header={
                      <Text type="secondary">
                        <BugOutlined /> Detalhes do Erro (Desenvolvimento)
                      </Text>
                    }
                    key="error-details"
                  >
                    <Space direction="vertical" size="small" style={{ width: '100%' }}>
                      <div>
                        <Text strong>Mensagem:</Text>
                        <Paragraph copyable style={{ marginBottom: 8 }}>
                          <Text code>{error.message}</Text>
                        </Paragraph>
                      </div>

                      <div>
                        <Text strong>Stack Trace:</Text>
                        <Paragraph
                          copyable
                          style={{
                            maxHeight: 200,
                            overflow: 'auto',
                            background: '#f5f5f5',
                            padding: 8,
                            borderRadius: 4,
                            fontSize: 12,
                            fontFamily: 'monospace',
                            marginBottom: 8
                          }}
                        >
                          <pre style={{ margin: 0, whiteSpace: 'pre-wrap' }}>
                            {error.stack}
                          </pre>
                        </Paragraph>
                      </div>

                      <div>
                        <Text strong>Informações do Sistema:</Text>
                        <ul style={{ marginTop: 8 }}>
                          <li><Text type="secondary">URL: {errorDetails.url}</Text></li>
                          <li><Text type="secondary">Timestamp: {errorDetails.timestamp}</Text></li>
                          <li><Text type="secondary">User Agent: {errorDetails.userAgent}</Text></li>
                        </ul>
                      </div>

                      <Button
                        size="small"
                        onClick={copyErrorDetails}
                        style={{ alignSelf: 'flex-start' }}
                      >
                        Copiar Detalhes Completos
                      </Button>
                    </Space>
                  </Panel>
                </Collapse>
              )}
            </Space>
          }
        />

        <div style={{ textAlign: 'center', marginTop: 24 }}>
          <Text type="secondary" style={{ fontSize: 12 }}>
            Se o problema persistir, entre em contato com o suporte técnico.
          </Text>
        </div>
      </div>
    </div>
  );
};

export default ErrorFallback;