// @ts-nocheck
import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse } from 'axios';
import { message } from 'antd';
import { STORAGE_KEYS, ERROR_MESSAGES } from '@/utils/constants';
import { storage } from '@/utils/helpers';
import type { ApiResponse, PaginatedResponse } from '@/types';

class ApiService {
  private api: AxiosInstance;

  constructor() {
    this.api = axios.create({
      baseURL: process.env.REACT_APP_API_URL,
      timeout: 30000,
      headers: {
        'Content-Type': 'application/json',
      },
    });

    this.setupInterceptors();
  }

  private setupInterceptors(): void {
    // Request interceptor - Add auth token
    this.api.interceptors.request.use(
      (config) => {
        const token = storage.get<string>(STORAGE_KEYS.AUTH_TOKEN);

        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }

        // Add request ID for tracing
        config.headers['X-Request-ID'] = this.generateRequestId();

        // Add environment info in development
        if (process.env.NODE_ENV === 'development') {
          config.headers['X-Environment'] = 'development';
          config.headers['X-Version'] = process.env.REACT_APP_VERSION || '1.0.0';
        }

        return config;
      },
      (error) => {
        return Promise.reject(error);
      }
    );

    // Response interceptor - Handle common errors
    this.api.interceptors.response.use(
      (response: AxiosResponse) => {
        return response;
      },
      async (error) => {
        const { response } = error;

        if (!response) {
          // Network error
          message.error(ERROR_MESSAGES.NETWORK_ERROR);
          return Promise.reject(error);
        }

        const { status, data } = response;

        switch (status) {
          case 401:
            // Unauthorized - try to refresh token
            if (!error.config._retry) {
              error.config._retry = true;

              try {
                await this.refreshToken();
                // Retry original request
                return this.api(error.config);
              } catch (refreshError) {
                this.handleAuthError();
                return Promise.reject(error);
              }
            }
            this.handleAuthError();
            break;

          case 403:
            message.error(data?.message || ERROR_MESSAGES.FORBIDDEN);
            break;

          case 404:
            // Don't show error for 404 on specific endpoints
            if (!this.isSilent404(error.config.url)) {
              message.error(data?.message || ERROR_MESSAGES.NOT_FOUND);
            }
            break;

          case 422:
            // Validation errors - handled by forms
            break;

          case 429:
            message.warning('Muitas requisições. Tente novamente em alguns instantes.');
            break;

          case 500:
          case 502:
          case 503:
          case 504:
            message.error(data?.message || ERROR_MESSAGES.SERVER_ERROR);
            break;

          default:
            if (status >= 400) {
              message.error(data?.message || 'Erro na requisição');
            }
        }

        return Promise.reject(error);
      }
    );
  }

  private generateRequestId(): string {
    return `req_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
  }

  private isSilent404(url?: string): boolean {
    const silentUrls = ['/auth/me', '/merchants/me'];
    return silentUrls.some(silentUrl => url?.includes(silentUrl));
  }

  private async refreshToken(): Promise<void> {
    const refreshToken = storage.get<string>(STORAGE_KEYS.REFRESH_TOKEN);

    if (!refreshToken) {
      throw new Error('No refresh token available');
    }

    try {
      const response = await axios.post(
        `${process.env.REACT_APP_API_URL}/auth/refresh`,
        { refreshToken }
      );

      const { token, refreshToken: newRefreshToken } = response.data;

      storage.set(STORAGE_KEYS.AUTH_TOKEN, token);
      storage.set(STORAGE_KEYS.REFRESH_TOKEN, newRefreshToken);
    } catch (error) {
      storage.remove(STORAGE_KEYS.AUTH_TOKEN);
      storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
      storage.remove(STORAGE_KEYS.USER_DATA);
      throw error;
    }
  }

  private handleAuthError(): void {
    // Clear auth data
    storage.remove(STORAGE_KEYS.AUTH_TOKEN);
    storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
    storage.remove(STORAGE_KEYS.USER_DATA);

    // Show error message
    message.error(ERROR_MESSAGES.UNAUTHORIZED);

    // Redirect to login after a short delay
    setTimeout(() => {
      window.location.href = '/login';
    }, 1500);
  }

  // Generic HTTP methods
  async get<T = any>(
    url: string,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.api.get<ApiResponse<T>>(url, config);
    return response.data.data || response.data;
  }

  async post<T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.api.post<ApiResponse<T>>(url, data, config);
    return response.data.data || response.data;
  }

  async put<T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.api.put<ApiResponse<T>>(url, data, config);
    return response.data.data || response.data;
  }

  async patch<T = any>(
    url: string,
    data?: any,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.api.patch<ApiResponse<T>>(url, data, config);
    return response.data.data || response.data;
  }

  async delete<T = any>(
    url: string,
    config?: AxiosRequestConfig
  ): Promise<T> {
    const response = await this.api.delete<ApiResponse<T>>(url, config);
    return response.data.data || response.data;
  }

  // Paginated GET request
  async getPaginated<T = any>(
    url: string,
    params?: Record<string, any>,
    config?: AxiosRequestConfig
  ): Promise<PaginatedResponse<T>> {
    const response = await this.api.get<ApiResponse<PaginatedResponse<T>>>(url, {
      ...config,
      params,
    });

    return response.data.data || response.data;
  }

  // File download
  async downloadFile(
    url: string,
    filename?: string,
    config?: AxiosRequestConfig
  ): Promise<void> {
    const response = await this.api.get(url, {
      ...config,
      responseType: 'blob',
    });

    // Extract filename from Content-Disposition header if not provided
    let downloadFilename = filename;
    if (!downloadFilename) {
      const contentDisposition = response.headers['content-disposition'];
      if (contentDisposition) {
        const filenameMatch = contentDisposition.match(/filename[^;=\n]*=((['"]).*?\2|[^;\n]*)/);
        if (filenameMatch) {
          downloadFilename = filenameMatch[1].replace(/['"]/g, '');
        }
      }
    }

    // Create download link
    const blob = new Blob([response.data]);
    const downloadUrl = URL.createObjectURL(blob);
    const link = document.createElement('a');

    link.href = downloadUrl;
    link.download = downloadFilename || 'download';
    document.body.appendChild(link);
    link.click();

    // Cleanup
    document.body.removeChild(link);
    URL.revokeObjectURL(downloadUrl);
  }

  // File upload
  async uploadFile<T = any>(
    url: string,
    file: File,
    fieldName = 'file',
    additionalData?: Record<string, any>,
    onProgress?: (progressEvent: any) => void
  ): Promise<T> {
    const formData = new FormData();
    formData.append(fieldName, file);

    // Add additional data if provided
    if (additionalData) {
      Object.entries(additionalData).forEach(([key, value]) => {
        formData.append(key, String(value));
      });
    }

    const response = await this.api.post<ApiResponse<T>>(url, formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      onUploadProgress: onProgress,
    });

    return response.data.data || response.data;
  }

  // Health check
  async healthCheck(): Promise<{ status: string; timestamp: string }> {
    return this.get('/health');
  }

  // Get raw axios instance for advanced usage
  getAxiosInstance(): AxiosInstance {
    return this.api;
  }

  // Cancel all pending requests
  cancelAllRequests(message = 'Requests cancelled'): void {
    // This would require implementing a request tracking system
    // For now, we can provide a basic implementation
    console.warn('Cancel all requests:', message);
  }
}

// Create singleton instance
export const apiService = new ApiService();

// Export axios instance for direct usage when needed
export const api = apiService.getAxiosInstance();

export default apiService;