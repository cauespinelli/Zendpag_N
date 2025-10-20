// @ts-nocheck
import React, { useEffect, useMemo } from 'react';
import { notification, message } from 'antd';
import { useNotifications } from '@/store/appStore';
import type { NotificationState } from '@/store/appStore';

// Configure global notification settings
notification.config({
  placement: 'topRight',
  duration: 4.5,
  maxCount: 5,
  rtl: false,
});

message.config({
  duration: 3,
  maxCount: 3,
});

interface NotificationProviderProps {
  children: React.ReactNode;
}

export const NotificationProvider: React.FC<NotificationProviderProps> = ({ children }) => {
  const { notifications, removeNotification } = useNotifications();

  // Notification API instance
  const [notificationApi, notificationContextHolder] = notification.useNotification({
    placement: 'topRight',
    maxCount: 5,
  });

  const [messageApi, messageContextHolder] = message.useMessage();

  // Process notifications queue
  useEffect(() => {
    notifications.forEach((notification: NotificationState) => {
      const { id, type, title, message: msg, duration, action } = notification;

      // Configure notification options
      const config = {
        key: id,
        message: title,
        description: msg,
        duration: duration ? duration / 1000 : undefined, // Convert to seconds
        onClose: () => removeNotification(id),
        btn: action ? (
          <button
            className="ant-btn ant-btn-primary ant-btn-sm"
            onClick={() => {
              action.onClick();
              removeNotification(id);
            }}
          >
            {action.label}
          </button>
        ) : undefined,
      };

      // Show notification based on type
      switch (type) {
        case 'success':
          notificationApi.success(config);
          break;
        case 'error':
          notificationApi.error(config);
          break;
        case 'warning':
          notificationApi.warning(config);
          break;
        case 'info':
          notificationApi.info(config);
          break;
        default:
          notificationApi.open({
            ...config,
            type: 'info',
          });
      }

      // Remove from store after showing
      removeNotification(id);
    });
  }, [notifications, notificationApi, removeNotification]);

  // Global message and notification utilities
  const messageUtils = useMemo(() => ({
    success: (content: string, duration?: number) => {
      messageApi.success(content, duration);
    },
    error: (content: string, duration?: number) => {
      messageApi.error(content, duration);
    },
    warning: (content: string, duration?: number) => {
      messageApi.warning(content, duration);
    },
    info: (content: string, duration?: number) => {
      messageApi.info(content, duration);
    },
    loading: (content: string, duration?: number) => {
      return messageApi.loading(content, duration);
    },
  }), [messageApi]);

  const notificationUtils = useMemo(() => ({
    success: (title: string, description?: string, options?: Partial<NotificationState>) => {
      notificationApi.success({
        message: title,
        description,
        ...options,
      });
    },
    error: (title: string, description?: string, options?: Partial<NotificationState>) => {
      notificationApi.error({
        message: title,
        description,
        ...options,
      });
    },
    warning: (title: string, description?: string, options?: Partial<NotificationState>) => {
      notificationApi.warning({
        message: title,
        description,
        ...options,
      });
    },
    info: (title: string, description?: string, options?: Partial<NotificationState>) => {
      notificationApi.info({
        message: title,
        description,
        ...options,
      });
    },
  }), [notificationApi]);

  // Attach utilities to window for global access (development)
  useEffect(() => {
    if (process.env.NODE_ENV === 'development') {
      (window as any).__zendapagNotifications = {
        message: messageUtils,
        notification: notificationUtils,
      };
    }
  }, [messageUtils, notificationUtils]);

  return (
    <>
      {notificationContextHolder}
      {messageContextHolder}
      {children}
    </>
  );
};

// Hook for using global notifications
export const useGlobalNotifications = () => {
  const [notificationApi, notificationContextHolder] = notification.useNotification();
  const [messageApi, messageContextHolder] = message.useMessage();

  const showSuccess = (title: string, description?: string) => {
    notificationApi.success({
      message: title,
      description,
    });
  };

  const showError = (title: string, description?: string) => {
    notificationApi.error({
      message: title,
      description,
    });
  };

  const showWarning = (title: string, description?: string) => {
    notificationApi.warning({
      message: title,
      description,
    });
  };

  const showInfo = (title: string, description?: string) => {
    notificationApi.info({
      message: title,
      description,
    });
  };

  const showMessage = {
    success: (content: string) => messageApi.success(content),
    error: (content: string) => messageApi.error(content),
    warning: (content: string) => messageApi.warning(content),
    info: (content: string) => messageApi.info(content),
    loading: (content: string) => messageApi.loading(content),
  };

  return {
    showSuccess,
    showError,
    showWarning,
    showInfo,
    showMessage,
    notificationContextHolder,
    messageContextHolder,
  };
};

// Utility functions for common notification patterns
export const notificationPatterns = {
  // API Success
  apiSuccess: (action: string, resource?: string) => {
    message.success(`${action} ${resource || ''} realizado com sucesso!`);
  },

  // API Error
  apiError: (action: string, error?: any, resource?: string) => {
    const title = `Erro ao ${action.toLowerCase()} ${resource || ''}`;
    const description = error?.response?.data?.message || error?.message || 'Ocorreu um erro inesperado';

    notification.error({
      message: title,
      description,
      duration: 0, // Don't auto-close error notifications
    });
  },

  // Network Error
  networkError: () => {
    notification.error({
      message: 'Erro de Conexão',
      description: 'Não foi possível conectar ao servidor. Verifique sua conexão de internet.',
      duration: 0,
    });
  },

  // Validation Error
  validationError: (errors: string[]) => {
    notification.warning({
      message: 'Dados inválidos',
      description: (
        <ul style={{ margin: 0, paddingLeft: '20px' }}>
          {errors.map((error, index) => (
            <li key={index}>{error}</li>
          ))}
        </ul>
      ),
    });
  },

  // Permission Error
  permissionError: () => {
    notification.error({
      message: 'Acesso Negado',
      description: 'Você não tem permissão para realizar esta ação.',
    });
  },

  // Session Expired
  sessionExpired: () => {
    notification.error({
      message: 'Sessão Expirada',
      description: 'Sua sessão expirou. Faça login novamente.',
      duration: 0,
      btn: (
        <button
          className="ant-btn ant-btn-primary"
          onClick={() => window.location.href = '/login'}
        >
          Fazer Login
        </button>
      ),
    });
  },

  // Offline Mode
  offlineMode: () => {
    notification.warning({
      message: 'Modo Offline',
      description: 'Você está offline. Suas ações serão sincronizadas quando a conexão for restabelecida.',
      duration: 0,
    });
  },

  // Online Mode
  onlineMode: () => {
    message.success('Conexão restabelecida!');
    notification.destroy(); // Clear offline notification
  },

  // Feature Coming Soon
  comingSoon: (feature: string) => {
    message.info(`${feature} estará disponível em breve!`);
  },

  // Copy to Clipboard
  copySuccess: (content?: string) => {
    message.success(`${content || 'Conteúdo'} copiado!`);
  },

  // Export Success
  exportSuccess: (format: string) => {
    message.success(`Arquivo ${format} exportado com sucesso!`);
  },

  // Import Success
  importSuccess: (count: number, resource: string) => {
    message.success(`${count} ${resource} importados com sucesso!`);
  },

  // Bulk Operation Success
  bulkSuccess: (action: string, count: number, resource: string) => {
    message.success(`${action} realizado em ${count} ${resource}!`);
  },

  // Confirmation Required
  confirmationRequired: (title: string, description: string, onConfirm: () => void) => {
    notification.warning({
      message: title,
      description,
      btn: (
        <button
          className="ant-btn ant-btn-primary"
          onClick={() => {
            onConfirm();
            notification.destroy();
          }}
        >
          Confirmar
        </button>
      ),
      duration: 0,
    });
  },
};