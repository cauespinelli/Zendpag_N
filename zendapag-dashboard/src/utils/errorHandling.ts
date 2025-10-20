// @ts-nocheck
import React from 'react';
import { AxiosError } from 'axios';
import { notification, message, Button } from 'antd';
import { useAppStore } from '@/store/appStore';

// Error types
export interface AppError extends Error {
  code?: string;
  statusCode?: number;
  details?: any;
  context?: Record<string, any>;
}

export interface ValidationError extends AppError {
  field?: string;
  value?: any;
}

export interface NetworkError extends AppError {
  isNetworkError: true;
  originalError: Error;
}

export interface APIError extends AppError {
  endpoint?: string;
  method?: string;
  statusCode: number;
  response?: any;
}

// Error classification
export const ErrorTypes = {
  NETWORK: 'NETWORK_ERROR',
  API: 'API_ERROR',
  VALIDATION: 'VALIDATION_ERROR',
  AUTH: 'AUTH_ERROR',
  PERMISSION: 'PERMISSION_ERROR',
  NOT_FOUND: 'NOT_FOUND_ERROR',
  SERVER: 'SERVER_ERROR',
  UNKNOWN: 'UNKNOWN_ERROR',
} as const;

export type ErrorType = typeof ErrorTypes[keyof typeof ErrorTypes];

// Error classifier
export const classifyError = (error: any): { type: ErrorType; error: AppError } => {
  // Network errors
  if (error.message?.includes('Network Error') || error.code === 'ERR_NETWORK') {
    return {
      type: ErrorTypes.NETWORK,
      error: {
        ...error,
        name: 'NetworkError',
        message: 'Erro de conexão com o servidor',
        isNetworkError: true,
        originalError: error,
      } as NetworkError,
    };
  }

  // Axios errors (API errors)
  if (error.isAxiosError || error.response) {
    const axiosError = error as AxiosError;
    const statusCode = axiosError.response?.status || 0;
    const responseData = axiosError.response?.data as any;

    let type: ErrorType;
    let message: string;

    switch (statusCode) {
      case 400:
        type = ErrorTypes.VALIDATION;
        message = responseData?.message || 'Dados inválidos';
        break;
      case 401:
        type = ErrorTypes.AUTH;
        message = 'Não autorizado. Faça login novamente.';
        break;
      case 403:
        type = ErrorTypes.PERMISSION;
        message = 'Acesso negado. Você não tem permissão para esta ação.';
        break;
      case 404:
        type = ErrorTypes.NOT_FOUND;
        message = 'Recurso não encontrado.';
        break;
      case 422:
        type = ErrorTypes.VALIDATION;
        message = 'Dados de entrada inválidos.';
        break;
      case 429:
        type = ErrorTypes.API;
        message = 'Muitas requisições. Tente novamente em alguns minutos.';
        break;
      case 500:
      case 502:
      case 503:
      case 504:
        type = ErrorTypes.SERVER;
        message = 'Erro interno do servidor. Tente novamente mais tarde.';
        break;
      default:
        type = ErrorTypes.API;
        message = responseData?.message || axiosError.message || 'Erro na API';
    }

    return {
      type,
      error: {
        name: 'APIError',
        message,
        statusCode,
        endpoint: axiosError.config?.url,
        method: axiosError.config?.method?.toUpperCase(),
        response: responseData,
        code: responseData?.code,
        details: responseData?.details,
      } as APIError,
    };
  }

  // Validation errors
  if (error.name === 'ValidationError' || error.type === 'validation') {
    return {
      type: ErrorTypes.VALIDATION,
      error: {
        ...error,
        name: 'ValidationError',
        message: error.message || 'Erro de validação',
      } as ValidationError,
    };
  }

  // Default to unknown error
  return {
    type: ErrorTypes.UNKNOWN,
    error: {
      name: error.name || 'UnknownError',
      message: error.message || 'Erro desconhecido',
      code: error.code,
      details: error.details,
    } as AppError,
  };
};

// Global error handler
export class ErrorHandler {
  private static instance: ErrorHandler;

  public static getInstance(): ErrorHandler {
    if (!ErrorHandler.instance) {
      ErrorHandler.instance = new ErrorHandler();
    }
    return ErrorHandler.instance;
  }

  public handle(error: any, context?: Record<string, any>): void {
    const { type, error: classifiedError } = classifyError(error);

    // Add context to error
    if (context) {
      classifiedError.context = context;
    }

    // Log error
    this.logError(type, classifiedError);

    // Show user notification
    this.notifyUser(type, classifiedError);

    // Handle specific error types
    this.handleSpecificError(type, classifiedError);

    // Send to monitoring service
    this.reportError(type, classifiedError);
  }

  private logError(type: ErrorType, error: AppError): void {
    const logLevel = this.getLogLevel(type);
    const logMethod = console[logLevel] || console.error;

    logMethod(`[${type}] ${error.name}:`, {
      message: error.message,
      code: error.code,
      statusCode: error.statusCode,
      context: error.context,
      stack: error.stack,
      details: error.details,
    });
  }

  private getLogLevel(type: ErrorType): 'error' | 'warn' | 'info' {
    switch (type) {
      case ErrorTypes.NETWORK:
      case ErrorTypes.SERVER:
      case ErrorTypes.UNKNOWN:
        return 'error';
      case ErrorTypes.AUTH:
      case ErrorTypes.PERMISSION:
        return 'warn';
      case ErrorTypes.VALIDATION:
      case ErrorTypes.NOT_FOUND:
        return 'info';
      default:
        return 'error';
    }
  }

  private notifyUser(type: ErrorType, error: AppError): void {
    switch (type) {
      case ErrorTypes.NETWORK:
        notification.error({
          message: 'Erro de Conexão',
          description: 'Não foi possível conectar ao servidor. Verifique sua conexão.',
          duration: 0,
        });
        break;

      case ErrorTypes.AUTH:
        notification.error({
          message: 'Sessão Expirada',
          description: error.message,
          duration: 0,
          btn: React.createElement(Button, {
            type: 'primary',
            onClick: () => {
              useAppStore.getState().logout();
              window.location.href = '/login';
            }
          }, 'Fazer Login'),
        });
        break;

      case ErrorTypes.PERMISSION:
        message.error(error.message);
        break;

      case ErrorTypes.VALIDATION:
        if (error.details && Array.isArray(error.details)) {
          notification.warning({
            message: 'Dados Inválidos',
            description: React.createElement('ul', { style: { margin: 0, paddingLeft: '20px' } },
              error.details.map((detail: any, index: number) =>
                React.createElement('li', { key: index }, detail.message || detail)
              )
            ),
          });
        } else {
          message.warning(error.message);
        }
        break;

      case ErrorTypes.NOT_FOUND:
        message.info(error.message);
        break;

      case ErrorTypes.SERVER:
        notification.error({
          message: 'Erro no Servidor',
          description: error.message,
          duration: 5,
        });
        break;

      case ErrorTypes.API:
        if (error.statusCode === 429) {
          notification.warning({
            message: 'Limite Excedido',
            description: error.message,
          });
        } else {
          notification.error({
            message: 'Erro na API',
            description: error.message,
          });
        }
        break;

      default:
        notification.error({
          message: 'Erro Inesperado',
          description: error.message,
        });
    }
  }

  private handleSpecificError(type: ErrorType, error: AppError): void {
    switch (type) {
      case ErrorTypes.NETWORK:
        // Enable offline mode
        useAppStore.getState().setOfflineMode(true);
        break;

      case ErrorTypes.AUTH:
        // Logout user
        useAppStore.getState().logout();
        break;

      case ErrorTypes.PERMISSION:
        // Could redirect to access denied page
        break;

      case ErrorTypes.SERVER:
        // Could trigger retry mechanism
        break;
    }
  }

  private reportError(type: ErrorType, error: AppError): void {
    // Only report in production
    if (process.env.NODE_ENV !== 'production') {
      return;
    }

    // Report to monitoring service (e.g., Sentry)
    try {
      // window.Sentry?.captureException(error, {
      //   tags: { type },
      //   extra: {
      //     context: error.context,
      //     statusCode: error.statusCode,
      //     endpoint: (error as APIError).endpoint,
      //     method: (error as APIError).method,
      //   },
      // });
    } catch (reportingError) {
      console.error('Failed to report error:', reportingError);
    }
  }
}

// Global error handler instance
export const globalErrorHandler = ErrorHandler.getInstance();

// React error boundary error handler
export const handleReactError = (error: Error, errorInfo: any) => {
  globalErrorHandler.handle(error, {
    type: 'REACT_ERROR',
    componentStack: errorInfo.componentStack,
  });
};

// Promise rejection handler
export const setupGlobalErrorHandlers = () => {
  // Handle unhandled promise rejections
  window.addEventListener('unhandledrejection', (event) => {
    globalErrorHandler.handle(event.reason, {
      type: 'UNHANDLED_PROMISE_REJECTION',
      promise: event.promise,
    });
    event.preventDefault(); // Prevent default browser handling
  });

  // Handle uncaught errors
  window.addEventListener('error', (event) => {
    globalErrorHandler.handle(event.error, {
      type: 'UNCAUGHT_ERROR',
      filename: event.filename,
      lineno: event.lineno,
      colno: event.colno,
    });
  });
};

// Utility functions
export const handleAsyncError = async <T>(
  promise: Promise<T>,
  context?: Record<string, any>
): Promise<[T | null, AppError | null]> => {
  try {
    const result = await promise;
    return [result, null];
  } catch (error) {
    const { error: classifiedError } = classifyError(error);
    if (context) {
      classifiedError.context = context;
    }
    return [null, classifiedError];
  }
};

// Retry wrapper with exponential backoff
export const withRetry = async <T>(
  fn: () => Promise<T>,
  options: {
    maxAttempts?: number;
    delay?: number;
    backoffFactor?: number;
    retryCondition?: (error: any) => boolean;
  } = {}
): Promise<T> => {
  const {
    maxAttempts = 3,
    delay = 1000,
    backoffFactor = 2,
    retryCondition = (error) => error.isAxiosError && error.response?.status >= 500,
  } = options;

  let lastError: any;

  for (let attempt = 1; attempt <= maxAttempts; attempt++) {
    try {
      return await fn();
    } catch (error) {
      lastError = error;

      if (attempt === maxAttempts || !retryCondition(error)) {
        throw error;
      }

      const waitTime = delay * Math.pow(backoffFactor, attempt - 1);
      await new Promise(resolve => setTimeout(resolve, waitTime));
    }
  }

  throw lastError;
};

// Error context provider
export const withErrorContext = <T extends any[], R>(
  fn: (...args: T) => Promise<R>,
  context: Record<string, any>
) => {
  return async (...args: T): Promise<R> => {
    try {
      return await fn(...args);
    } catch (error) {
      globalErrorHandler.handle(error, context);
      throw error;
    }
  };
};