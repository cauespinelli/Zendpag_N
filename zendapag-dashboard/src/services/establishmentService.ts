// @ts-nocheck
import apiService from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type {
  Establishment,
  EstablishmentSummary,
  EstablishmentFilters,
  PaginatedResponse,
  EstablishmentFees,
  EstablishmentAcquirers,
  WithdrawalConfig,
} from '@/types';

export interface EstablishmentListParams extends EstablishmentFilters {
  page?: number;
  size?: number;
  sortBy?: string;
  sortDir?: 'asc' | 'desc';
}

export interface UpdateEstablishmentRequest {
  companyName?: string;
  tradeName?: string;
  email?: string;
  phone?: string;
  address?: {
    street?: string;
    number?: string;
    complement?: string;
    neighborhood?: string;
    city?: string;
    state?: string;
    zipCode?: string;
  };
  status?: string;
}

export interface EditBalanceRequest {
  amount: number;
  type: 'ADD' | 'SUBTRACT';
  wallet: 'PIX' | 'CARD' | 'BOLETO';
  reason: string;
}

export interface LinkAffiliateRequest {
  affiliateId: string;
}

export interface ConfigureRetentionRequest {
  retentionDays: number;
  retentionPercentage: number;
}

class EstablishmentService {
  /**
   * Get paginated list of establishments
   */
  async getEstablishments(params: EstablishmentListParams = {}): Promise<PaginatedResponse<Establishment>> {
    const queryParams = {
      page: params.page ?? 0,
      size: params.size ?? 20,
      search: params.search,
      status: params.status,
      documentStatus: params.documentStatus,
      startDate: params.startDate,
      endDate: params.endDate,
      cnpj: params.cnpj,
      sortBy: params.sortBy,
      sortDir: params.sortDir,
    };

    // Remove undefined values
    Object.keys(queryParams).forEach(key => {
      if (queryParams[key] === undefined) {
        delete queryParams[key];
      }
    });

    return apiService.getPaginated<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.BASE, queryParams);
  }

  /**
   * Get establishment summary (totals)
   */
  async getSummary(): Promise<EstablishmentSummary> {
    return apiService.get<EstablishmentSummary>(API_ENDPOINTS.ESTABLISHMENTS.SUMMARY);
  }

  /**
   * Get establishment by ID
   */
  async getById(id: string): Promise<Establishment> {
    return apiService.get<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.BY_ID(id));
  }

  /**
   * Update establishment basic info
   */
  async update(id: string, data: UpdateEstablishmentRequest): Promise<Establishment> {
    return apiService.put<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.BY_ID(id), data);
  }

  /**
   * Update establishment fees
   */
  async updateFees(id: string, fees: EstablishmentFees): Promise<Establishment> {
    return apiService.put<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.FEES(id), fees);
  }

  /**
   * Update establishment acquirers
   */
  async updateAcquirers(id: string, acquirers: EstablishmentAcquirers): Promise<Establishment> {
    return apiService.put<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.ACQUIRERS(id), acquirers);
  }

  /**
   * Update withdrawal config
   */
  async updateWithdrawalConfig(id: string, config: WithdrawalConfig): Promise<Establishment> {
    return apiService.put<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.BY_ID(id), { withdrawal: config });
  }

  /**
   * Get establishment documents
   */
  async getDocuments(id: string): Promise<any[]> {
    return apiService.get(API_ENDPOINTS.ESTABLISHMENTS.DOCUMENTS(id));
  }

  /**
   * Get establishment timeline
   */
  async getTimeline(id: string): Promise<any[]> {
    return apiService.get(API_ENDPOINTS.ESTABLISHMENTS.TIMELINE(id));
  }

  /**
   * Activate establishment
   */
  async activate(id: string): Promise<Establishment> {
    return apiService.post<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.ACTIVATE(id));
  }

  /**
   * Block establishment
   */
  async block(id: string, reason?: string): Promise<Establishment> {
    return apiService.post<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.BLOCK(id), { reason });
  }

  /**
   * Inactivate establishment
   */
  async inactivate(id: string, reason?: string): Promise<Establishment> {
    return apiService.post<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.INACTIVATE(id), { reason });
  }

  /**
   * Resend activation email
   */
  async resendActivation(id: string): Promise<void> {
    return apiService.post(API_ENDPOINTS.ESTABLISHMENTS.RESEND_ACTIVATION(id));
  }

  /**
   * Make establishment an affiliate
   */
  async makeAffiliate(id: string, commissionRate: number): Promise<any> {
    return apiService.post(API_ENDPOINTS.ESTABLISHMENTS.MAKE_AFFILIATE(id), { commissionRate });
  }

  /**
   * Link establishment to affiliate
   */
  async linkToAffiliate(id: string, affiliateId: string): Promise<Establishment> {
    return apiService.post<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.LINK_AFFILIATE(id), { affiliateId });
  }

  /**
   * Edit establishment balance
   */
  async editBalance(id: string, data: EditBalanceRequest): Promise<Establishment> {
    return apiService.post<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.EDIT_BALANCE(id), data);
  }

  /**
   * Edit MED percentage
   */
  async editMedPercentage(id: string, percentage: number): Promise<Establishment> {
    return apiService.put<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.EDIT_MED(id), { medPercentage: percentage });
  }

  /**
   * Edit dispute percentage
   */
  async editDisputePercentage(id: string, percentage: number): Promise<Establishment> {
    return apiService.put<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.EDIT_DISPUTE(id), { disputePercentage: percentage });
  }

  /**
   * Configure retention
   */
  async configureRetention(id: string, data: ConfigureRetentionRequest): Promise<Establishment> {
    return apiService.put<Establishment>(API_ENDPOINTS.ESTABLISHMENTS.CONFIGURE_RETENTION(id), data);
  }

  /**
   * Export establishments to CSV
   */
  async exportToCsv(filters: EstablishmentFilters = {}): Promise<void> {
    return apiService.downloadFile(API_ENDPOINTS.ESTABLISHMENTS.EXPORT, 'estabelecimentos.csv', {
      params: filters,
    });
  }
}

export const establishmentService = new EstablishmentService();
export default establishmentService;
