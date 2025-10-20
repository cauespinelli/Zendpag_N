// @ts-nocheck
import apiService from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type {
  Payment,
  PaymentFormData,
  PaginatedResponse,
} from '@/types';

export interface PaymentFilters {
  page?: number;
  size?: number;
  status?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
  pixKeyType?: string;
  amountMin?: number;
  amountMax?: number;
  customerId?: string;
  merchantId?: string;
}

export interface CreatePixPaymentRequest extends PaymentFormData {
  pixKey?: string;
  pixKeyType?: string;
}

export interface PaymentResponse {
  id: string;
  referenceId: string;
  pixTxId?: string;
  amount: number;
  status: string;
  qrCodeText?: string;
  qrCodeImage?: string;
  createdAt: string;
  expiresAt?: string;
}

class PaymentService {
  async getPayments(filters: PaymentFilters = {}): Promise<PaginatedResponse<Payment>> {
    const queryParams = this.buildQueryParams(filters);
    return apiService.getPaginated<Payment>(`${API_ENDPOINTS.PAYMENTS.BASE}${queryParams}`);
  }

  async getPayment(id: string): Promise<Payment> {
    return apiService.get<Payment>(`${API_ENDPOINTS.PAYMENTS.BASE}/${id}`);
  }

  async createPixPayment(request: CreatePixPaymentRequest): Promise<PaymentResponse> {
    const payload = this.buildCreatePaymentPayload(request);
    return apiService.post<PaymentResponse>(API_ENDPOINTS.PAYMENTS.PIX, payload);
  }

  async cancelPayment(id: string, reason?: string): Promise<void> {
    return apiService.post(API_ENDPOINTS.PAYMENTS.CANCEL(id), { reason });
  }

  async getPaymentQrCode(id: string): Promise<{ qrCodeText: string; qrCodeImage: string }> {
    return apiService.get<{ qrCodeText: string; qrCodeImage: string }>(
      API_ENDPOINTS.PAYMENTS.QR_CODE(id)
    );
  }

  async getPaymentsByDateRange(startDate: string, endDate: string, filters: Partial<PaymentFilters> = {}): Promise<PaginatedResponse<Payment>> {
    return this.getPayments({
      ...filters,
      startDate,
      endDate,
    });
  }

  async searchPayments(query: string, filters: Partial<PaymentFilters> = {}): Promise<PaginatedResponse<Payment>> {
    return this.getPayments({
      ...filters,
      search: query,
    });
  }

  async getPaymentsByStatus(status: string, filters: Partial<PaymentFilters> = {}): Promise<PaginatedResponse<Payment>> {
    return this.getPayments({
      ...filters,
      status,
    });
  }

  async exportPayments(filters: PaymentFilters = {}, format: 'csv' | 'pdf' = 'csv'): Promise<void> {
    const queryParams = this.buildQueryParams({ ...filters, format });
    return apiService.downloadFile(
      `${API_ENDPOINTS.PAYMENTS.BASE}/export${queryParams}`,
      `payments_${new Date().toISOString().split('T')[0]}.${format}`
    );
  }

  async bulkCancelPayments(paymentIds: string[], reason?: string): Promise<void> {
    return apiService.post(`${API_ENDPOINTS.PAYMENTS.BASE}/bulk-cancel`, {
      paymentIds,
      reason,
    });
  }

  async getPaymentStats(period: string = '30d'): Promise<{
    total: number;
    completed: number;
    pending: number;
    failed: number;
    cancelled: number;
    totalAmount: number;
    averageAmount: number;
  }> {
    return apiService.get<any>(`${API_ENDPOINTS.PAYMENTS.BASE}/stats`, {
      params: { period },
    });
  }

  async validatePixKey(pixKey: string, pixKeyType: string): Promise<{
    valid: boolean;
    message?: string;
    ownerName?: string;
    ownerDocument?: string;
  }> {
    return apiService.post<any>('/pix/keys/validate', {
      pixKey,
      pixKeyType,
    });
  }

  private buildQueryParams(filters: PaymentFilters): string {
    const params = new URLSearchParams();

    Object.entries(filters).forEach(([key, value]) => {
      if (value !== undefined && value !== null && value !== '') {
        params.append(key, String(value));
      }
    });

    const queryString = params.toString();
    return queryString ? `?${queryString}` : '';
  }

  private buildCreatePaymentPayload(request: CreatePixPaymentRequest): any {
    const payload: any = {
      amount: request.amount,
      currency: 'BRL',
      description: request.description,
      customerName: request.customerName,
      customerEmail: request.customerEmail,
      customerDocument: request.customerDocument,
    };

    // Add PIX specific fields
    if (request.pixKey && request.pixKeyType) {
      payload.pixKey = request.pixKey;
      payload.pixKeyType = request.pixKeyType;
    }

    // Add expiration
    if (request.expirationMinutes) {
      const expiresAt = new Date();
      expiresAt.setMinutes(expiresAt.getMinutes() + request.expirationMinutes);
      payload.expiresAt = expiresAt.toISOString();
    }

    return payload;
  }
}

export const paymentService = new PaymentService();
export default paymentService;