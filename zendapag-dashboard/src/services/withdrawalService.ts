// @ts-nocheck
import api from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type {
  Withdrawal,
  WithdrawalSummary,
  WithdrawalListParams,
  PaginatedResponse,
  ApiResponse,
} from '@/types';

export interface CreateWithdrawalRequest {
  establishmentId: string;
  amount: number;
  pixKey: {
    value: string;
    type: string;
  };
  wallet: string;
  type?: string;
}

class WithdrawalService {
  async getWithdrawals(
    params: WithdrawalListParams = {}
  ): Promise<PaginatedResponse<Withdrawal>> {
    const response = await api.get<PaginatedResponse<Withdrawal>>(
      API_ENDPOINTS.WITHDRAWALS.BASE,
      { params }
    );
    return response.data;
  }

  async getWithdrawalById(id: string): Promise<Withdrawal> {
    const response = await api.get<ApiResponse<Withdrawal>>(
      API_ENDPOINTS.WITHDRAWALS.BY_ID(id)
    );
    return response.data.data!;
  }

  async getSummary(params?: {
    startDate?: string;
    endDate?: string;
    establishmentId?: string;
  }): Promise<WithdrawalSummary> {
    const response = await api.get<ApiResponse<WithdrawalSummary>>(
      `${API_ENDPOINTS.WITHDRAWALS.BASE}/summary`,
      { params }
    );
    return response.data.data!;
  }

  async create(data: CreateWithdrawalRequest): Promise<Withdrawal> {
    const response = await api.post<ApiResponse<Withdrawal>>(
      API_ENDPOINTS.WITHDRAWALS.BASE,
      data
    );
    return response.data.data!;
  }

  async cancel(id: string, reason?: string): Promise<Withdrawal> {
    const response = await api.post<ApiResponse<Withdrawal>>(
      API_ENDPOINTS.WITHDRAWALS.CANCEL(id),
      { reason }
    );
    return response.data.data!;
  }

  async retry(id: string): Promise<Withdrawal> {
    const response = await api.post<ApiResponse<Withdrawal>>(
      `${API_ENDPOINTS.WITHDRAWALS.BY_ID(id)}/retry`
    );
    return response.data.data!;
  }

  async export(
    params: WithdrawalListParams,
    format: 'csv' | 'xlsx' = 'csv'
  ): Promise<Blob> {
    const response = await api.get(API_ENDPOINTS.WITHDRAWALS.EXPORT, {
      params: { ...params, format },
      responseType: 'blob',
    });
    return response.data;
  }

  async getReceipt(id: string): Promise<Blob> {
    const response = await api.get(
      `${API_ENDPOINTS.WITHDRAWALS.BY_ID(id)}/receipt`,
      { responseType: 'blob' }
    );
    return response.data;
  }
}

export const withdrawalService = new WithdrawalService();
export default withdrawalService;
