// @ts-nocheck
import api from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type {
  Dispute,
  DisputeSummary,
  DisputeListParams,
  ResolveDisputeRequest,
  PaginatedResponse,
  ApiResponse,
} from '@/types';

class DisputeService {
  async getDisputes(
    params: DisputeListParams = {}
  ): Promise<PaginatedResponse<Dispute>> {
    const response = await api.get<PaginatedResponse<Dispute>>(
      API_ENDPOINTS.DISPUTES.BASE,
      { params }
    );
    return response.data;
  }

  async getDisputeById(id: string): Promise<Dispute> {
    const response = await api.get<ApiResponse<Dispute>>(
      API_ENDPOINTS.DISPUTES.BY_ID(id)
    );
    return response.data.data!;
  }

  async getSummary(params?: {
    startDate?: string;
    endDate?: string;
    establishmentId?: string;
  }): Promise<DisputeSummary> {
    const response = await api.get<ApiResponse<DisputeSummary>>(
      `${API_ENDPOINTS.DISPUTES.BASE}/summary`,
      { params }
    );
    return response.data.data!;
  }

  async resolve(id: string, data: ResolveDisputeRequest): Promise<Dispute> {
    const response = await api.post<ApiResponse<Dispute>>(
      API_ENDPOINTS.DISPUTES.RESOLVE(id),
      data
    );
    return response.data.data!;
  }

  async reject(id: string, reason: string): Promise<Dispute> {
    const response = await api.post<ApiResponse<Dispute>>(
      API_ENDPOINTS.DISPUTES.REJECT(id),
      { reason }
    );
    return response.data.data!;
  }

  async startAnalysis(id: string, assignedTo?: string): Promise<Dispute> {
    const response = await api.post<ApiResponse<Dispute>>(
      `${API_ENDPOINTS.DISPUTES.BY_ID(id)}/analyze`,
      { assignedTo }
    );
    return response.data.data!;
  }

  async updatePriority(id: string, priority: string): Promise<Dispute> {
    const response = await api.patch<ApiResponse<Dispute>>(
      `${API_ENDPOINTS.DISPUTES.BY_ID(id)}/priority`,
      { priority }
    );
    return response.data.data!;
  }

  async addNote(id: string, note: string): Promise<Dispute> {
    const response = await api.post<ApiResponse<Dispute>>(
      `${API_ENDPOINTS.DISPUTES.BY_ID(id)}/notes`,
      { note }
    );
    return response.data.data!;
  }

  async export(
    params: DisputeListParams,
    format: 'csv' | 'xlsx' = 'csv'
  ): Promise<Blob> {
    const response = await api.get(API_ENDPOINTS.DISPUTES.EXPORT, {
      params: { ...params, format },
      responseType: 'blob',
    });
    return response.data;
  }
}

export const disputeService = new DisputeService();
export default disputeService;
