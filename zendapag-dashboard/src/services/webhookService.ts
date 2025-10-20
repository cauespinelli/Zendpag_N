// @ts-nocheck
import apiService from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type { Webhook, WebhookEvent } from '@/types';

export interface WebhookCreateRequest {
  url: string;
  events: WebhookEvent[];
  secret?: string;
  active?: boolean;
  description?: string;
}

export interface WebhookUpdateRequest extends Partial<WebhookCreateRequest> {
  id: string;
}

export interface WebhookDelivery {
  id: string;
  webhookId: string;
  eventType: string;
  eventId: string;
  payload: any;
  status: 'PENDING' | 'DELIVERED' | 'FAILED' | 'RETRYING';
  attempts: number;
  maxAttempts: number;
  lastAttemptAt?: string;
  nextAttemptAt?: string;
  responseStatus?: number;
  responseBody?: string;
  errorMessage?: string;
  createdAt: string;
  deliveredAt?: string;
}

export interface WebhookStats {
  totalDeliveries: number;
  successfulDeliveries: number;
  failedDeliveries: number;
  successRate: number;
  averageResponseTime: number;
  lastDeliveryAt?: string;
  recentDeliveries: WebhookDelivery[];
}

class WebhookService {
  async getWebhooks(): Promise<Webhook[]> {
    return apiService.get<Webhook[]>(API_ENDPOINTS.WEBHOOKS.BASE);
  }

  async getWebhook(id: string): Promise<Webhook> {
    return apiService.get<Webhook>(API_ENDPOINTS.WEBHOOKS.BY_ID(id));
  }

  async createWebhook(data: WebhookCreateRequest): Promise<Webhook> {
    return apiService.post<Webhook>(API_ENDPOINTS.WEBHOOKS.BASE, data);
  }

  async updateWebhook(id: string, data: Partial<WebhookCreateRequest>): Promise<Webhook> {
    return apiService.put<Webhook>(API_ENDPOINTS.WEBHOOKS.BY_ID(id), data);
  }

  async deleteWebhook(id: string): Promise<void> {
    return apiService.delete(API_ENDPOINTS.WEBHOOKS.BY_ID(id));
  }

  async testWebhook(id: string, eventType?: WebhookEvent): Promise<{
    success: boolean;
    message: string;
    responseStatus?: number;
    responseTime?: number;
    errorDetails?: string;
  }> {
    return apiService.post<any>(API_ENDPOINTS.WEBHOOKS.TEST(id), {
      eventType: eventType || 'payment.created',
    });
  }

  async toggleWebhookStatus(id: string, active: boolean): Promise<Webhook> {
    return apiService.patch<Webhook>(API_ENDPOINTS.WEBHOOKS.BY_ID(id), { active });
  }

  async getWebhookDeliveries(
    webhookId: string,
    page: number = 1,
    size: number = 20,
    status?: 'PENDING' | 'DELIVERED' | 'FAILED' | 'RETRYING'
  ): Promise<{
    deliveries: WebhookDelivery[];
    total: number;
    page: number;
    size: number;
  }> {
    return apiService.get<any>(`${API_ENDPOINTS.WEBHOOKS.BY_ID(webhookId)}/deliveries`, {
      params: { page, size, status },
    });
  }

  async getWebhookStats(webhookId: string, period: string = '30d'): Promise<WebhookStats> {
    return apiService.get<WebhookStats>(`${API_ENDPOINTS.WEBHOOKS.BY_ID(webhookId)}/stats`, {
      params: { period },
    });
  }

  async retryWebhookDelivery(deliveryId: string): Promise<{
    success: boolean;
    message: string;
    newAttemptId?: string;
  }> {
    return apiService.post<any>(`/webhook-deliveries/${deliveryId}/retry`);
  }

  async bulkRetryFailedDeliveries(webhookId: string, maxAge?: number): Promise<{
    retriedCount: number;
    message: string;
  }> {
    return apiService.post<any>(`${API_ENDPOINTS.WEBHOOKS.BY_ID(webhookId)}/retry-failed`, {
      maxAgeHours: maxAge,
    });
  }

  async validateWebhookUrl(url: string): Promise<{
    valid: boolean;
    message: string;
    reachable: boolean;
    responseTime?: number;
    supportedMethods?: string[];
  }> {
    return apiService.post<any>('/webhooks/validate-url', { url });
  }

  async generateWebhookSecret(): Promise<{ secret: string }> {
    return apiService.post<any>('/webhooks/generate-secret');
  }

  async getWebhookEvents(): Promise<{
    events: Array<{
      name: string;
      description: string;
      example: any;
      category: string;
    }>;
  }> {
    return apiService.get<any>('/webhooks/events');
  }

  async getWebhookLogs(
    webhookId?: string,
    eventType?: string,
    startDate?: string,
    endDate?: string,
    page: number = 1,
    size: number = 20
  ): Promise<{
    logs: Array<{
      id: string;
      webhookId: string;
      eventType: string;
      eventId: string;
      status: string;
      attempts: number;
      createdAt: string;
      lastAttemptAt?: string;
      responseStatus?: number;
      responseTime?: number;
      errorMessage?: string;
    }>;
    total: number;
    page: number;
    size: number;
  }> {
    return apiService.get<any>('/webhook-logs', {
      params: {
        webhookId,
        eventType,
        startDate,
        endDate,
        page,
        size,
      },
    });
  }

  async exportWebhookLogs(
    webhookId?: string,
    startDate?: string,
    endDate?: string,
    format: 'CSV' | 'JSON' = 'CSV'
  ): Promise<void> {
    const params = new URLSearchParams();
    if (webhookId) params.append('webhookId', webhookId);
    if (startDate) params.append('startDate', startDate);
    if (endDate) params.append('endDate', endDate);
    params.append('format', format);

    return apiService.downloadFile(
      `/webhook-logs/export?${params.toString()}`,
      `webhook-logs-${new Date().toISOString().split('T')[0]}.${format.toLowerCase()}`
    );
  }

  async getWebhookConfig(): Promise<{
    maxRetryAttempts: number;
    retryIntervals: number[];
    timeoutSeconds: number;
    supportedEvents: WebhookEvent[];
    rateLimits: {
      requestsPerMinute: number;
      burstLimit: number;
    };
  }> {
    return apiService.get<any>('/webhooks/config');
  }

  async simulateEvent(eventType: WebhookEvent, customPayload?: any): Promise<{
    eventId: string;
    payload: any;
    deliveredWebhooks: number;
    message: string;
  }> {
    return apiService.post<any>('/webhooks/simulate', {
      eventType,
      customPayload,
    });
  }

  async getDeliveryDetails(deliveryId: string): Promise<{
    id: string;
    webhook: {
      id: string;
      url: string;
      events: WebhookEvent[];
    };
    event: {
      type: string;
      id: string;
      createdAt: string;
    };
    payload: any;
    attempts: Array<{
      attemptNumber: number;
      attemptedAt: string;
      responseStatus?: number;
      responseHeaders?: Record<string, string>;
      responseBody?: string;
      responseTime: number;
      errorMessage?: string;
    }>;
    status: string;
    nextRetryAt?: string;
  }> {
    return apiService.get<any>(`/webhook-deliveries/${deliveryId}`);
  }

  async pauseWebhook(id: string, reason?: string): Promise<void> {
    return apiService.post(`${API_ENDPOINTS.WEBHOOKS.BY_ID(id)}/pause`, { reason });
  }

  async resumeWebhook(id: string): Promise<void> {
    return apiService.post(`${API_ENDPOINTS.WEBHOOKS.BY_ID(id)}/resume`);
  }

  async getWebhookMetrics(webhookId: string, period: string = '7d'): Promise<{
    totalRequests: number;
    successfulRequests: number;
    failedRequests: number;
    averageResponseTime: number;
    p95ResponseTime: number;
    errorRate: number;
    dailyStats: Array<{
      date: string;
      requests: number;
      successes: number;
      failures: number;
      averageResponseTime: number;
    }>;
    statusCodeDistribution: Record<string, number>;
    errorDistribution: Array<{
      error: string;
      count: number;
      percentage: number;
    }>;
  }> {
    return apiService.get<any>(`${API_ENDPOINTS.WEBHOOKS.BY_ID(webhookId)}/metrics`, {
      params: { period },
    });
  }
}

export const webhookService = new WebhookService();
export default webhookService;