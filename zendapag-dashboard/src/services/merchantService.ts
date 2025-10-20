// @ts-nocheck
import apiService from './api';
import { API_ENDPOINTS } from '@/utils/constants';
import type { Merchant } from '@/types';

export interface MerchantUpdateRequest {
  name?: string;
  tradingName?: string;
  email?: string;
  phoneNumber?: string;
  address?: {
    street: string;
    number: string;
    complement?: string;
    neighborhood: string;
    city: string;
    state: string;
    zipCode: string;
    country: string;
  };
  webhookUrl?: string;
}

export interface BalanceResponse {
  balance: number;
  pendingBalance: number;
  availableBalance: number;
  lastUpdated: string;
}

export interface ApiKeyResponse {
  apiKey: string;
  expiresAt?: string;
  createdAt: string;
}

export interface MerchantStats {
  totalTransactions: number;
  totalRevenue: number;
  successRate: number;
  averageTicket: number;
  monthlyGrowth: {
    transactions: number;
    revenue: number;
  };
  topPaymentMethods: Array<{
    method: string;
    count: number;
    percentage: number;
  }>;
}

class MerchantService {
  async getMerchant(): Promise<Merchant> {
    return apiService.get<Merchant>(API_ENDPOINTS.MERCHANTS.ME);
  }

  async updateMerchant(data: MerchantUpdateRequest): Promise<Merchant> {
    return apiService.put<Merchant>(API_ENDPOINTS.MERCHANTS.ME, data);
  }

  async getBalance(): Promise<BalanceResponse> {
    return apiService.get<BalanceResponse>(API_ENDPOINTS.MERCHANTS.BALANCE);
  }

  async generateApiKey(): Promise<ApiKeyResponse> {
    return apiService.post<ApiKeyResponse>(API_ENDPOINTS.MERCHANTS.API_KEY);
  }

  async revokeApiKey(): Promise<void> {
    return apiService.delete(API_ENDPOINTS.MERCHANTS.API_KEY);
  }

  async getMerchantStats(period: string = '30d'): Promise<MerchantStats> {
    return apiService.get<MerchantStats>(`${API_ENDPOINTS.MERCHANTS.ME}/stats`, {
      params: { period },
    });
  }

  async updateWebhookUrl(url: string): Promise<void> {
    return apiService.put(`${API_ENDPOINTS.MERCHANTS.ME}/webhook`, { webhookUrl: url });
  }

  async testWebhook(): Promise<{ success: boolean; message: string }> {
    return apiService.post<any>(`${API_ENDPOINTS.MERCHANTS.ME}/webhook/test`);
  }

  async getTransactionHistory(
    page: number = 1,
    size: number = 20,
    startDate?: string,
    endDate?: string
  ): Promise<{
    transactions: Array<{
      id: string;
      type: 'CREDIT' | 'DEBIT' | 'FEE';
      amount: number;
      description: string;
      createdAt: string;
      paymentId?: string;
      balance: number;
    }>;
    total: number;
    page: number;
    size: number;
  }> {
    return apiService.get<any>(`${API_ENDPOINTS.MERCHANTS.ME}/transactions`, {
      params: { page, size, startDate, endDate },
    });
  }

  async requestWithdrawal(amount: number, bankAccount: {
    bank: string;
    agency: string;
    account: string;
    accountType: 'CHECKING' | 'SAVINGS';
    ownerName: string;
    ownerDocument: string;
  }): Promise<{
    id: string;
    amount: number;
    status: string;
    requestedAt: string;
    estimatedProcessingTime: string;
  }> {
    return apiService.post<any>(`${API_ENDPOINTS.MERCHANTS.ME}/withdrawals`, {
      amount,
      bankAccount,
    });
  }

  async getWithdrawals(): Promise<Array<{
    id: string;
    amount: number;
    status: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED';
    requestedAt: string;
    processedAt?: string;
    failureReason?: string;
    bankAccount: {
      bank: string;
      agency: string;
      account: string;
      accountType: string;
    };
  }>> {
    return apiService.get<any>(`${API_ENDPOINTS.MERCHANTS.ME}/withdrawals`);
  }

  async updateProfile(profileData: {
    name?: string;
    email?: string;
    phoneNumber?: string;
    avatar?: File;
  }): Promise<Merchant> {
    const formData = new FormData();

    Object.entries(profileData).forEach(([key, value]) => {
      if (value !== undefined && value !== null) {
        if (key === 'avatar' && value instanceof File) {
          formData.append(key, value);
        } else if (typeof value === 'string') {
          formData.append(key, value);
        }
      }
    });

    return apiService.uploadFile<Merchant>(
      `${API_ENDPOINTS.MERCHANTS.ME}/profile`,
      profileData.avatar as File,
      'avatar',
      Object.fromEntries(
        Object.entries(profileData).filter(([key]) => key !== 'avatar')
      )
    );
  }

  async uploadDocument(documentType: 'IDENTITY' | 'ADDRESS_PROOF' | 'BUSINESS_LICENSE', file: File): Promise<{
    id: string;
    type: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    uploadedAt: string;
  }> {
    return apiService.uploadFile<any>(
      `${API_ENDPOINTS.MERCHANTS.ME}/documents`,
      file,
      'document',
      { type: documentType }
    );
  }

  async getDocuments(): Promise<Array<{
    id: string;
    type: string;
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    uploadedAt: string;
    reviewedAt?: string;
    reviewComment?: string;
  }>> {
    return apiService.get<any>(`${API_ENDPOINTS.MERCHANTS.ME}/documents`);
  }

  async deleteDocument(documentId: string): Promise<void> {
    return apiService.delete(`${API_ENDPOINTS.MERCHANTS.ME}/documents/${documentId}`);
  }

  async getNotificationSettings(): Promise<{
    email: {
      paymentCompleted: boolean;
      paymentFailed: boolean;
      lowBalance: boolean;
      monthlyReport: boolean;
    };
    sms: {
      paymentCompleted: boolean;
      paymentFailed: boolean;
      lowBalance: boolean;
    };
    webhook: {
      enabled: boolean;
      url: string;
      events: string[];
    };
  }> {
    return apiService.get<any>(`${API_ENDPOINTS.MERCHANTS.ME}/notifications`);
  }

  async updateNotificationSettings(settings: {
    email?: {
      paymentCompleted?: boolean;
      paymentFailed?: boolean;
      lowBalance?: boolean;
      monthlyReport?: boolean;
    };
    sms?: {
      paymentCompleted?: boolean;
      paymentFailed?: boolean;
      lowBalance?: boolean;
    };
    webhook?: {
      enabled?: boolean;
      url?: string;
      events?: string[];
    };
  }): Promise<void> {
    return apiService.put(`${API_ENDPOINTS.MERCHANTS.ME}/notifications`, settings);
  }

  async exportData(type: 'PAYMENTS' | 'TRANSACTIONS' | 'ALL', format: 'CSV' | 'JSON' | 'PDF' = 'CSV'): Promise<{
    exportId: string;
    status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
    downloadUrl?: string;
    requestedAt: string;
  }> {
    return apiService.post<any>(`${API_ENDPOINTS.MERCHANTS.ME}/export`, {
      type,
      format,
    });
  }

  async getExportStatus(exportId: string): Promise<{
    id: string;
    status: 'PROCESSING' | 'COMPLETED' | 'FAILED';
    downloadUrl?: string;
    requestedAt: string;
    completedAt?: string;
    errorMessage?: string;
  }> {
    return apiService.get<any>(`${API_ENDPOINTS.MERCHANTS.ME}/export/${exportId}`);
  }

  async deleteAccount(confirmationPassword: string): Promise<void> {
    return apiService.post(`${API_ENDPOINTS.MERCHANTS.ME}/delete`, {
      password: confirmationPassword,
    });
  }
}

export const merchantService = new MerchantService();
export default merchantService;