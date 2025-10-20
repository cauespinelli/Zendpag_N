// @ts-nocheck
import React, { Component, ErrorInfo, ReactNode } from 'react';
import { Result, Button, Typography, Card, Space } from 'antd';
import { BugOutlined, ReloadOutlined, HomeOutlined } from '@ant-design/icons';

const { Paragraph, Text } = Typography;

interface Props {
  children?: ReactNode;
  fallback?: ReactNode;
  onError?: (error: Error, errorInfo: ErrorInfo) => void;
}

interface State {
  hasError: boolean;
  error?: Error;
  errorInfo?: ErrorInfo;
}

export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props);
    this.state = { hasError: false };
  }

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    this.setState({ error, errorInfo });

    // Log error to console and external services
    console.error('Error Boundary caught an error:', error, errorInfo);

    // Send error to monitoring service (e.g., Sentry)
    if (process.env.NODE_ENV === 'production') {
      // window.Sentry?.captureException(error, { extra: errorInfo });
    }

    // Call custom error handler
    this.props.onError?.(error, errorInfo);
  }

  private handleReload = () => {
    window.location.reload();
  };

  private handleGoHome = () => {
    window.location.href = '/';
  };

  private handleRetry = () => {
    this.setState({ hasError: false, error: undefined, errorInfo: undefined });
  };

  public render() {
    if (this.state.hasError) {
      // Custom fallback UI
      if (this.props.fallback) {
        return this.props.fallback;
      }

      return (
        <div
          style={{
            minHeight: '100vh',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            padding: '20px',
            background: '#f5f5f5'
          }}
        >
          <Card style={{ maxWidth: 600, width: '100%' }}>
            <Result
              status="error"
              icon={<BugOutlined style={{ color: '#ff4d4f' }} />}
              title="Oops! Algo deu errado"
              subTitle="Ocorreu um erro inesperado na aplicação. Nossa equipe foi notificada."
              extra={
                <Space direction="vertical" size="middle">
                  <Space wrap>
                    <Button
                      type="primary"
                      icon={<ReloadOutlined />}
                      onClick={this.handleRetry}
                    >
                      Tentar Novamente
                    </Button>
                    <Button
                      icon={<ReloadOutlined />}
                      onClick={this.handleReload}
                    >
                      Recarregar Página
                    </Button>
                    <Button
                      icon={<HomeOutlined />}
                      onClick={this.handleGoHome}
                    >
                      Ir para Início
                    </Button>
                  </Space>

                  {process.env.NODE_ENV === 'development' && this.state.error && (
                    <Card
                      size="small"
                      title="Detalhes do Erro (Desenvolvimento)"
                      style={{ textAlign: 'left', marginTop: 16 }}
                    >
                      <Paragraph>
                        <Text strong>Erro:</Text>
                        <br />
                        <Text code copyable>
                          {this.state.error.name}: {this.state.error.message}
                        </Text>
                      </Paragraph>

                      {this.state.error.stack && (
                        <Paragraph>
                          <Text strong>Stack Trace:</Text>
                          <br />
                          <Text code copyable style={{ fontSize: '11px' }}>
                            {this.state.error.stack}
                          </Text>
                        </Paragraph>
                      )}

                      {this.state.errorInfo?.componentStack && (
                        <Paragraph>
                          <Text strong>Component Stack:</Text>
                          <br />
                          <Text code copyable style={{ fontSize: '11px' }}>
                            {this.state.errorInfo.componentStack}
                          </Text>
                        </Paragraph>
                      )}
                    </Card>
                  )}
                </Space>
              }
            />
          </Card>
        </div>
      );
    }

    return this.props.children;
  }
}

// Higher-order component for wrapping components with error boundary
export const withErrorBoundary = <P extends object>(
  Component: React.ComponentType<P>,
  fallback?: ReactNode
) => {
  const WrappedComponent = (props: P) => (
    <ErrorBoundary fallback={fallback}>
      <Component {...props} />
    </ErrorBoundary>
  );

  WrappedComponent.displayName = `withErrorBoundary(${Component.displayName || Component.name})`;

  return WrappedComponent;
};

// Hook for handling errors in functional components
export const useErrorHandler = () => {
  return React.useCallback((error: Error, errorInfo?: any) => {
    console.error('Error caught by useErrorHandler:', error, errorInfo);

    // Log to external service in production
    if (process.env.NODE_ENV === 'production') {
      // window.Sentry?.captureException(error, { extra: errorInfo });
    }

    // You could also trigger a global error state here
    throw error; // Re-throw to trigger error boundary
  }, []);
};