// @ts-nocheck
import { useQuery as useReactQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { message } from 'antd';
import apiService from '@/services/api';
import { API_ENDPOINTS, QUERY_KEYS } from '@/utils/constants';
import { getErrorMessage } from '@/utils/helpers';
import type {
  User,
  Merchant,
  Payment,
  Webhook,
  DashboardStats,
  AnalyticsData,
  TransactionReport,
  PaginatedResponse,
} from '@/types';

// Generic query hook with error handling
export const useQuery = <T = any>(
  key: any[],
  queryFn: () => Promise<T>,
  options: any = {}
) => {
  return useReactQuery({
    queryKey: key,
    queryFn,
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
    retry: (failureCount, error: any) => {
      // Don't retry on 4xx errors (except 429)
      if (error?.response?.status >= 400 && error?.response?.status < 500 && error?.response?.status !== 429) {
        return false;
      }
      return failureCount < 3;
    },
    onError: (error: any) => {
      if (!options.suppressErrorNotification) {
        const errorMessage = getErrorMessage(error);
        message.error(errorMessage);
      }
    },
    ...options,
  });
};

// Auth queries
export const useCurrentUser = () => {
  return useQuery(
    QUERY_KEYS.USER,
    () => apiService.get<User>(API_ENDPOINTS.AUTH.ME),
    {
      staleTime: 10 * 60 * 1000, // 10 minutes
      suppressErrorNotification: true, // Handle auth errors in auth store
    }
  );
};

// Merchant queries
export const useMerchant = () => {
  return useQuery(
    QUERY_KEYS.MERCHANT,
    () => apiService.get<Merchant>(API_ENDPOINTS.MERCHANTS.ME),
    {
      staleTime: 15 * 60 * 1000, // 15 minutes
    }
  );
};

export const useMerchantBalance = () => {
  return useQuery(
    [...QUERY_KEYS.MERCHANT, 'balance'],
    () => apiService.get<{ balance: number }>(API_ENDPOINTS.MERCHANTS.BALANCE),
    {
      staleTime: 2 * 60 * 1000, // 2 minutes
      refetchInterval: 5 * 60 * 1000, // Auto-refresh every 5 minutes
    }
  );
};

// Payment queries
export const usePayments = (params?: {
  page?: number;
  size?: number;
  status?: string;
  startDate?: string;
  endDate?: string;
  search?: string;
}) => {
  return useQuery(
    [...QUERY_KEYS.PAYMENTS, params],
    () => apiService.getPaginated<Payment>(API_ENDPOINTS.PAYMENTS.BASE, params)
  );
};

export const usePayment = (id: string) => {
  return useQuery(
    QUERY_KEYS.PAYMENT(id),
    () => apiService.get<Payment>(`${API_ENDPOINTS.PAYMENTS.BASE}/${id}`),
    {
      enabled: !!id,
    }
  );
};

export const usePaymentQrCode = (id: string) => {
  return useQuery(
    [...QUERY_KEYS.PAYMENT(id), 'qr-code'],
    () => apiService.get<{ qrCodeText: string; qrCodeImage: string }>(
      API_ENDPOINTS.PAYMENTS.QR_CODE(id)
    ),
    {
      enabled: !!id,
      staleTime: 30 * 60 * 1000, // 30 minutes
    }
  );
};

// Webhook queries
export const useWebhooks = () => {
  return useQuery(
    QUERY_KEYS.WEBHOOKS,
    () => apiService.get<Webhook[]>(API_ENDPOINTS.WEBHOOKS.BASE)
  );
};

export const useWebhook = (id: string) => {
  return useQuery(
    QUERY_KEYS.WEBHOOK(id),
    () => apiService.get<Webhook>(API_ENDPOINTS.WEBHOOKS.BY_ID(id)),
    {
      enabled: !!id,
    }
  );
};

// Dashboard queries
export const useDashboardStats = (period?: string) => {
  return useQuery(
    [...QUERY_KEYS.DASHBOARD_STATS, period],
    () => apiService.get<DashboardStats>(API_ENDPOINTS.ANALYTICS.DASHBOARD, {
      params: period ? { period } : undefined,
    }),
    {
      staleTime: 2 * 60 * 1000, // 2 minutes
      refetchInterval: 5 * 60 * 1000, // Auto-refresh every 5 minutes
    }
  );
};

// Analytics queries
export const useTransactionAnalytics = (params: {
  period: string;
  groupBy?: 'day' | 'hour' | 'month';
  startDate?: string;
  endDate?: string;
}) => {
  return useQuery(
    [...QUERY_KEYS.ANALYTICS, 'transactions', params],
    () => apiService.get<AnalyticsData>(API_ENDPOINTS.ANALYTICS.TRANSACTIONS, {
      params,
    })
  );
};

export const useRevenueAnalytics = (params: {
  period: string;
  currency?: string;
  startDate?: string;
  endDate?: string;
}) => {
  return useQuery(
    [...QUERY_KEYS.ANALYTICS, 'revenue', params],
    () => apiService.get<AnalyticsData>(API_ENDPOINTS.ANALYTICS.REVENUE, {
      params,
    })
  );
};

// Reports queries
export const useReports = () => {
  return useQuery(
    QUERY_KEYS.REPORTS,
    () => apiService.get<TransactionReport[]>(API_ENDPOINTS.REPORTS.BASE)
  );
};

// Mutations
export const useCreatePixPayment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: any) =>
      apiService.post<Payment>(API_ENDPOINTS.PAYMENTS.PIX, data),
    onSuccess: (data) => {
      // Invalidate and refetch payments list
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.PAYMENTS });

      // Update dashboard stats
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.DASHBOARD_STATS });

      message.success('Pagamento PIX criado com sucesso!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao criar pagamento: ${errorMessage}`);
    },
  });
};

export const useCancelPayment = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, reason }: { id: string; reason?: string }) =>
      apiService.post(API_ENDPOINTS.PAYMENTS.CANCEL(id), { reason }),
    onSuccess: (_, variables) => {
      // Update specific payment
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.PAYMENT(variables.id) });

      // Update payments list
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.PAYMENTS });

      // Update dashboard stats
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.DASHBOARD_STATS });

      message.success('Pagamento cancelado com sucesso!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao cancelar pagamento: ${errorMessage}`);
    },
  });
};

export const useCreateWebhook = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: any) =>
      apiService.post<Webhook>(API_ENDPOINTS.WEBHOOKS.BASE, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.WEBHOOKS });
      message.success('Webhook criado com sucesso!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao criar webhook: ${errorMessage}`);
    },
  });
};

export const useUpdateWebhook = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, data }: { id: string; data: any }) =>
      apiService.put<Webhook>(API_ENDPOINTS.WEBHOOKS.BY_ID(id), data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.WEBHOOK(variables.id) });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.WEBHOOKS });
      message.success('Webhook atualizado com sucesso!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao atualizar webhook: ${errorMessage}`);
    },
  });
};

export const useDeleteWebhook = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id: string) =>
      apiService.delete(API_ENDPOINTS.WEBHOOKS.BY_ID(id)),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.WEBHOOKS });
      message.success('Webhook deletado com sucesso!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao deletar webhook: ${errorMessage}`);
    },
  });
};

export const useTestWebhook = () => {
  return useMutation({
    mutationFn: (id: string) =>
      apiService.post(API_ENDPOINTS.WEBHOOKS.TEST(id)),
    onSuccess: () => {
      message.success('Webhook testado com sucesso!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao testar webhook: ${errorMessage}`);
    },
  });
};

export const useGenerateReport = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (params: {
      type: string;
      startDate: string;
      endDate: string;
      format: string;
    }) => apiService.post<TransactionReport>(API_ENDPOINTS.REPORTS.TRANSACTIONS, params),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.REPORTS });
      message.success('Relatório será gerado em breve!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao gerar relatório: ${errorMessage}`);
    },
  });
};

export const useDownloadReport = () => {
  return useMutation({
    mutationFn: (id: string) =>
      apiService.downloadFile(API_ENDPOINTS.REPORTS.DOWNLOAD(id)),
    onSuccess: () => {
      message.success('Download iniciado!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao baixar relatório: ${errorMessage}`);
    },
  });
};

export const useRegenerateApiKey = () => {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => apiService.post<{ apiKey: string }>(API_ENDPOINTS.MERCHANTS.API_KEY),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.MERCHANT });
      message.success('Nova chave API gerada com sucesso!');
    },
    onError: (error: any) => {
      const errorMessage = getErrorMessage(error);
      message.error(`Erro ao gerar nova chave API: ${errorMessage}`);
    },
  });
};

// Custom hooks for common patterns
export const useInfinitePayments = (filters?: any) => {
  const queryClient = useQueryClient();

  return useReactQuery({
    queryKey: [...QUERY_KEYS.PAYMENTS, 'infinite', filters],
    queryFn: ({ pageParam = 1 }) =>
      apiService.getPaginated<Payment>(API_ENDPOINTS.PAYMENTS.BASE, {
        ...filters,
        page: pageParam,
        size: 20,
      }),
    getNextPageParam: (lastPage: PaginatedResponse<Payment>) => {
      return !lastPage.last ? lastPage.page + 1 : undefined;
    },
    staleTime: 5 * 60 * 1000,
  });
};

export const useRealtimeUpdates = (enabled = true) => {
  const queryClient = useQueryClient();

  // This would connect to WebSocket or SSE for real-time updates
  // For now, we'll implement polling
  return useReactQuery({
    queryKey: ['realtime-updates'],
    queryFn: async () => {
      // Check for updates
      const stats = await apiService.get<DashboardStats>(API_ENDPOINTS.ANALYTICS.DASHBOARD);

      // Invalidate relevant queries if there are new updates
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.PAYMENTS });
      queryClient.invalidateQueries({ queryKey: QUERY_KEYS.DASHBOARD_STATS });

      return stats;
    },
    enabled,
    refetchInterval: 30 * 1000, // Poll every 30 seconds
    refetchIntervalInBackground: false,
    staleTime: 0,
    gcTime: 0,
  });
};