// @ts-nocheck
import api from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type {
  Affiliate,
  AffiliateSummary,
  AffiliateListParams,
  AffiliateFilters,
  AffiliateCommission,
  AffiliateEstablishment,
  CreateAffiliateRequest,
  UpdateAffiliateRequest,
  PaginatedResponse,
  ApiResponse,
} from '@/types';

class AffiliateService {
  async getAffiliates(
    params: AffiliateListParams = {}
  ): Promise<PaginatedResponse<Affiliate>> {
    const response = await api.get<PaginatedResponse<Affiliate>>(
      API_ENDPOINTS.AFFILIATES.BASE,
      { params }
    );
    return response.data;
  }

  async getAffiliateById(id: string): Promise<Affiliate> {
    const response = await api.get<ApiResponse<Affiliate>>(
      API_ENDPOINTS.AFFILIATES.BY_ID(id)
    );
    return response.data.data!;
  }

  async getSummary(params?: {
    startDate?: string;
    endDate?: string;
  }): Promise<AffiliateSummary> {
    const response = await api.get<ApiResponse<AffiliateSummary>>(
      `${API_ENDPOINTS.AFFILIATES.BASE}/summary`,
      { params }
    );
    return response.data.data!;
  }

  async create(data: CreateAffiliateRequest): Promise<Affiliate> {
    const response = await api.post<ApiResponse<Affiliate>>(
      API_ENDPOINTS.AFFILIATES.BASE,
      data
    );
    return response.data.data!;
  }

  async update(id: string, data: UpdateAffiliateRequest): Promise<Affiliate> {
    const response = await api.patch<ApiResponse<Affiliate>>(
      API_ENDPOINTS.AFFILIATES.BY_ID(id),
      data
    );
    return response.data.data!;
  }

  async activate(id: string): Promise<Affiliate> {
    const response = await api.post<ApiResponse<Affiliate>>(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(id)}/activate`
    );
    return response.data.data!;
  }

  async deactivate(id: string): Promise<Affiliate> {
    const response = await api.post<ApiResponse<Affiliate>>(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(id)}/deactivate`
    );
    return response.data.data!;
  }

  async block(id: string, reason: string): Promise<Affiliate> {
    const response = await api.post<ApiResponse<Affiliate>>(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(id)}/block`,
      { reason }
    );
    return response.data.data!;
  }

  async unblock(id: string): Promise<Affiliate> {
    const response = await api.post<ApiResponse<Affiliate>>(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(id)}/unblock`
    );
    return response.data.data!;
  }

  async updateCommissionRate(id: string, commissionRate: number): Promise<Affiliate> {
    const response = await api.patch<ApiResponse<Affiliate>>(
      API_ENDPOINTS.AFFILIATES.COMMISSION(id),
      { commissionRate }
    );
    return response.data.data!;
  }

  async getCommissions(
    id: string,
    params?: {
      page?: number;
      size?: number;
      status?: 'PENDING' | 'PAID' | 'CANCELLED';
      startDate?: string;
      endDate?: string;
    }
  ): Promise<PaginatedResponse<AffiliateCommission>> {
    const response = await api.get<PaginatedResponse<AffiliateCommission>>(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(id)}/commissions`,
      { params }
    );
    return response.data;
  }

  async getEstablishments(
    id: string,
    params?: {
      page?: number;
      size?: number;
    }
  ): Promise<PaginatedResponse<AffiliateEstablishment>> {
    const response = await api.get<PaginatedResponse<AffiliateEstablishment>>(
      API_ENDPOINTS.AFFILIATES.ESTABLISHMENTS(id),
      { params }
    );
    return response.data;
  }

  async linkEstablishment(affiliateId: string, establishmentId: string): Promise<void> {
    await api.post(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(affiliateId)}/link-establishment`,
      { establishmentId }
    );
  }

  async unlinkEstablishment(affiliateId: string, establishmentId: string): Promise<void> {
    await api.post(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(affiliateId)}/unlink-establishment`,
      { establishmentId }
    );
  }

  async requestWithdrawal(id: string, amount: number): Promise<{
    withdrawalId: string;
    amount: number;
    status: string;
  }> {
    const response = await api.post<ApiResponse<{
      withdrawalId: string;
      amount: number;
      status: string;
    }>>(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(id)}/withdraw`,
      { amount }
    );
    return response.data.data!;
  }

  async export(
    params: AffiliateFilters,
    format: 'csv' | 'xlsx' = 'csv'
  ): Promise<Blob> {
    const response = await api.get(`${API_ENDPOINTS.AFFILIATES.BASE}/export`, {
      params: { ...params, format },
      responseType: 'blob',
    });
    return response.data;
  }

  async generateReferralLink(id: string): Promise<{ link: string; code: string }> {
    const response = await api.post<ApiResponse<{ link: string; code: string }>>(
      `${API_ENDPOINTS.AFFILIATES.BY_ID(id)}/generate-link`
    );
    return response.data.data!;
  }
}

export const affiliateService = new AffiliateService();
export default affiliateService;
