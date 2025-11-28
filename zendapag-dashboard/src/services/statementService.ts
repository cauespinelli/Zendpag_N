// @ts-nocheck
import api from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type {
  StatementEntry,
  StatementSummary,
  StatementListParams,
  StatementFilters,
  PaginatedResponse,
  ApiResponse,
} from '@/types';

class StatementService {
  async getEntries(
    params: StatementListParams = {}
  ): Promise<PaginatedResponse<StatementEntry>> {
    const response = await api.get<PaginatedResponse<StatementEntry>>(
      API_ENDPOINTS.STATEMENTS.BASE,
      { params }
    );
    return response.data;
  }

  async getEntryById(id: string): Promise<StatementEntry> {
    const response = await api.get<ApiResponse<StatementEntry>>(
      `${API_ENDPOINTS.STATEMENTS.BASE}/${id}`
    );
    return response.data.data!;
  }

  async getSummary(params: {
    startDate: string;
    endDate: string;
    establishmentId?: string;
    wallet?: string;
  }): Promise<StatementSummary> {
    const response = await api.get<ApiResponse<StatementSummary>>(
      `${API_ENDPOINTS.STATEMENTS.BASE}/summary`,
      { params }
    );
    return response.data.data!;
  }

  async getBalance(establishmentId?: string): Promise<{
    available: number;
    pending: number;
    blocked: number;
    total: number;
    byWallet: {
      pix: number;
      card: number;
      boleto: number;
    };
  }> {
    const response = await api.get<ApiResponse<{
      available: number;
      pending: number;
      blocked: number;
      total: number;
      byWallet: {
        pix: number;
        card: number;
        boleto: number;
      };
    }>>(`${API_ENDPOINTS.STATEMENTS.BASE}/balance`, {
      params: { establishmentId }
    });
    return response.data.data!;
  }

  async getDailyBalances(params: {
    startDate: string;
    endDate: string;
    establishmentId?: string;
  }): Promise<Array<{
    date: string;
    openingBalance: number;
    closingBalance: number;
    credits: number;
    debits: number;
  }>> {
    const response = await api.get<ApiResponse<Array<{
      date: string;
      openingBalance: number;
      closingBalance: number;
      credits: number;
      debits: number;
    }>>>(`${API_ENDPOINTS.STATEMENTS.BASE}/daily-balances`, {
      params
    });
    return response.data.data!;
  }

  async export(
    params: StatementFilters & { startDate: string; endDate: string },
    format: 'csv' | 'xlsx' | 'pdf' = 'csv'
  ): Promise<Blob> {
    const response = await api.get(API_ENDPOINTS.STATEMENTS.EXPORT, {
      params: { ...params, format },
      responseType: 'blob',
    });
    return response.data;
  }

  async getReconciliation(params: {
    startDate: string;
    endDate: string;
    establishmentId?: string;
  }): Promise<{
    period: { startDate: string; endDate: string };
    expectedBalance: number;
    actualBalance: number;
    difference: number;
    status: 'MATCHED' | 'MISMATCH';
    details: Array<{
      date: string;
      expected: number;
      actual: number;
      difference: number;
    }>;
  }> {
    const response = await api.get<ApiResponse<{
      period: { startDate: string; endDate: string };
      expectedBalance: number;
      actualBalance: number;
      difference: number;
      status: 'MATCHED' | 'MISMATCH';
      details: Array<{
        date: string;
        expected: number;
        actual: number;
        difference: number;
      }>;
    }>>(`${API_ENDPOINTS.STATEMENTS.BASE}/reconciliation`, {
      params
    });
    return response.data.data!;
  }
}

export const statementService = new StatementService();
export default statementService;
