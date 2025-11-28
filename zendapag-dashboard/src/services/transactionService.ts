// @ts-nocheck
import api from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type {
  Transaction,
  TransactionSummary,
  TransactionListParams,
  PaginatedResponse,
  ApiResponse,
} from '@/types';

class TransactionService {
  async getTransactions(
    params: TransactionListParams = {}
  ): Promise<PaginatedResponse<Transaction>> {
    const response = await api.get<PaginatedResponse<Transaction>>(
      API_ENDPOINTS.TRANSACTIONS.BASE,
      { params }
    );
    return response.data;
  }

  async getTransactionById(id: string): Promise<Transaction> {
    const response = await api.get<ApiResponse<Transaction>>(
      API_ENDPOINTS.TRANSACTIONS.BY_ID(id)
    );
    return response.data.data!;
  }

  async getSummary(params?: {
    startDate?: string;
    endDate?: string;
    establishmentId?: string;
  }): Promise<TransactionSummary> {
    const response = await api.get<ApiResponse<TransactionSummary>>(
      `${API_ENDPOINTS.TRANSACTIONS.BASE}/summary`,
      { params }
    );
    return response.data.data!;
  }

  async refund(id: string, reason?: string): Promise<Transaction> {
    const response = await api.post<ApiResponse<Transaction>>(
      `${API_ENDPOINTS.TRANSACTIONS.BY_ID(id)}/refund`,
      { reason }
    );
    return response.data.data!;
  }

  async openDispute(
    id: string,
    data: { reason: string; description?: string }
  ): Promise<void> {
    await api.post(API_ENDPOINTS.TRANSACTIONS.DISPUTE(id), data);
  }

  async resendWebhook(id: string): Promise<void> {
    await api.post(API_ENDPOINTS.TRANSACTIONS.WEBHOOK(id));
  }

  async export(
    params: TransactionListParams,
    format: 'csv' | 'xlsx' = 'csv'
  ): Promise<Blob> {
    const response = await api.get(API_ENDPOINTS.TRANSACTIONS.EXPORT, {
      params: { ...params, format },
      responseType: 'blob',
    });
    return response.data;
  }

  async getReceipt(id: string): Promise<Blob> {
    const response = await api.get(
      `${API_ENDPOINTS.TRANSACTIONS.BY_ID(id)}/receipt`,
      { responseType: 'blob' }
    );
    return response.data;
  }
}

export const transactionService = new TransactionService();
export default transactionService;
