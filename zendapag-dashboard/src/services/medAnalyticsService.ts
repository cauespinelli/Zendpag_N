// @ts-nocheck
import api from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type {
  MedRecord,
  MedDashboardSummary,
  MedListParams,
  MedFilters,
  PaginatedResponse,
  ApiResponse,
} from '@/types';

class MedAnalyticsService {
  async getSummary(params?: {
    startDate?: string;
    endDate?: string;
    establishmentId?: string;
  }): Promise<MedDashboardSummary> {
    const response = await api.get<ApiResponse<MedDashboardSummary>>(
      API_ENDPOINTS.MED_ANALYTICS.SUMMARY,
      { params }
    );
    return response.data.data!;
  }

  async getHistory(
    params: MedListParams = {}
  ): Promise<PaginatedResponse<MedRecord>> {
    const response = await api.get<PaginatedResponse<MedRecord>>(
      API_ENDPOINTS.MED_ANALYTICS.HISTORY,
      { params }
    );
    return response.data;
  }

  async getRealtime(): Promise<{
    pendingMeds: number;
    pendingDisputes: number;
    executingNow: number;
    lastUpdate: string;
  }> {
    const response = await api.get<ApiResponse<{
      pendingMeds: number;
      pendingDisputes: number;
      executingNow: number;
      lastUpdate: string;
    }>>(API_ENDPOINTS.MED_ANALYTICS.REALTIME);
    return response.data.data!;
  }

  async getRecordById(id: string): Promise<MedRecord> {
    const response = await api.get<ApiResponse<MedRecord>>(
      `${API_ENDPOINTS.MED_ANALYTICS.HISTORY}/${id}`
    );
    return response.data.data!;
  }

  async cancelMed(id: string, reason: string): Promise<MedRecord> {
    const response = await api.post<ApiResponse<MedRecord>>(
      `${API_ENDPOINTS.MED_ANALYTICS.HISTORY}/${id}/cancel`,
      { reason }
    );
    return response.data.data!;
  }

  async retryMed(id: string): Promise<MedRecord> {
    const response = await api.post<ApiResponse<MedRecord>>(
      `${API_ENDPOINTS.MED_ANALYTICS.HISTORY}/${id}/retry`
    );
    return response.data.data!;
  }

  async export(
    params: MedFilters,
    format: 'csv' | 'xlsx' = 'csv'
  ): Promise<Blob> {
    const response = await api.get(API_ENDPOINTS.MED_ANALYTICS.EXPORT, {
      params: { ...params, format },
      responseType: 'blob',
    });
    return response.data;
  }

  async getDailyReport(date: string): Promise<{
    date: string;
    meds: MedRecord[];
    disputes: MedRecord[];
    totalMedsValue: number;
    totalDisputesValue: number;
    executedCount: number;
    pendingCount: number;
  }> {
    const response = await api.get<ApiResponse<{
      date: string;
      meds: MedRecord[];
      disputes: MedRecord[];
      totalMedsValue: number;
      totalDisputesValue: number;
      executedCount: number;
      pendingCount: number;
    }>>(`${API_ENDPOINTS.MED_ANALYTICS.SUMMARY}/daily`, {
      params: { date }
    });
    return response.data.data!;
  }

  async getEstablishmentMedHistory(
    establishmentId: string,
    params: MedListParams = {}
  ): Promise<PaginatedResponse<MedRecord>> {
    const response = await api.get<PaginatedResponse<MedRecord>>(
      `${API_ENDPOINTS.MED_ANALYTICS.HISTORY}/establishment/${establishmentId}`,
      { params }
    );
    return response.data;
  }
}

export const medAnalyticsService = new MedAnalyticsService();
export default medAnalyticsService;
