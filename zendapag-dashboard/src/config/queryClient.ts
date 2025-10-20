// @ts-nocheck
// @ts-nocheck
import { QueryClient, MutationCache, QueryCache } from '@tanstack/react-query';
import { useAppStore } from '@/store/appStore';

// Error handler for queries and mutations
const handleError = (error: unknown) => {
  const addNotification = useAppStore.getState().addNotification;
  const setOfflineMode = useAppStore.getState().setOfflineMode;

  if (error instanceof Error) {
    // Handle network errors
    if (error.message.includes('Network Error') || error.message.includes('ERR_NETWORK')) {
      setOfflineMode(true);
      addNotification({
        type: 'warning',
        title: 'Conexão perdida',
        message: 'Modo offline ativado. Suas ações serão sincronizadas quando a conexão for restabelecida.',
        duration: 5000,
      });
      return;
    }

    // Handle API errors
    if ('status' in error) {
      const status = (error as any).status;

      switch (status) {
        case 401:
          addNotification({
            type: 'error',
            title: 'Não autorizado',
            message: 'Sua sessão expirou. Faça login novamente.',
          });
          useAppStore.getState().logout();
          break;

        case 403:
          addNotification({
            type: 'error',
            title: 'Acesso negado',
            message: 'Você não tem permissão para realizar esta ação.',
          });
          break;

        case 429:
          addNotification({
            type: 'warning',
            title: 'Limite de requisições',
            message: 'Muitas requisições. Tente novamente em alguns minutos.',
          });
          break;

        case 500:
        case 502:
        case 503:
          addNotification({
            type: 'error',
            title: 'Erro no servidor',
            message: 'Ocorreu um erro interno. Tente novamente em alguns minutos.',
          });
          break;

        default:
          addNotification({
            type: 'error',
            title: 'Erro inesperado',
            message: error.message || 'Ocorreu um erro inesperado.',
          });
      }
    }
  }
};

// Query cache configuration
const queryCache = new QueryCache({
  onError: handleError,
  onSuccess: (data, query) => {
    // Update last sync time for successful queries
    const now = new Date();
    useAppStore.getState().setLastSync(now);

    // Reset offline mode if we have successful network requests
    const offlineMode = useAppStore.getState().offlineMode;
    if (offlineMode) {
      useAppStore.getState().setOfflineMode(false);
      useAppStore.getState().addNotification({
        type: 'success',
        title: 'Conexão restabelecida',
        message: 'Modo online ativado.',
        duration: 3000,
      });
    }
  },
});

// Mutation cache configuration
const mutationCache = new MutationCache({
  onError: handleError,
  onSuccess: (data, variables, context, mutation) => {
    const now = new Date();
    useAppStore.getState().setLastSync(now);

    // Show success notification for mutations
    useAppStore.getState().addNotification({
      type: 'success',
      title: 'Ação realizada',
      message: 'Operação concluída com sucesso.',
      duration: 3000,
    });
  },
});

// Query client configuration
export const queryClient = new QueryClient({
  queryCache,
  mutationCache,
  defaultOptions: {
    queries: {
      // Stale time - how long until data is considered stale
      staleTime: 5 * 60 * 1000, // 5 minutes

      // Cache time - how long to keep data in cache
      gcTime: 10 * 60 * 1000, // 10 minutes

      // Retry configuration
      retry: (failureCount, error: any) => {
        // Don't retry on 4xx errors (client errors)
        if (error?.status >= 400 && error?.status < 500) {
          return false;
        }

        // Retry up to 3 times for other errors
        return failureCount < 3;
      },

      // Retry delay with exponential backoff
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 30000),

      // Background refetch configuration
      refetchOnMount: true,
      refetchOnWindowFocus: true,
      refetchOnReconnect: true,

      // Refetch intervals
      refetchInterval: false, // Disabled by default
      refetchIntervalInBackground: false,

      // Network mode
      networkMode: 'online',

      // Meta information for queries
      meta: {
        persist: true, // Enable persistence for offline support
      },
    },
    mutations: {
      // Retry configuration for mutations
      retry: (failureCount, error: any) => {
        // Only retry on network errors or 5xx server errors
        if (error?.message?.includes('Network Error') ||
            (error?.status >= 500 && error?.status < 600)) {
          return failureCount < 2; // Retry up to 2 times
        }
        return false;
      },

      // Retry delay
      retryDelay: (attemptIndex) => Math.min(1000 * 2 ** attemptIndex, 10000),

      // Network mode
      networkMode: 'online',

      // Meta information
      meta: {
        persist: false, // Don't persist mutations by default
      },
    },
  },
});

// Query keys factory
export const queryKeys = {
  // Auth
  auth: {
    user: ['auth', 'user'] as const,
  },

  // Payments
  payments: {
    all: ['payments'] as const,
    lists: () => [...queryKeys.payments.all, 'list'] as const,
    list: (filters: Record<string, any>) =>
      [...queryKeys.payments.lists(), filters] as const,
    details: () => [...queryKeys.payments.all, 'detail'] as const,
    detail: (id: string) => [...queryKeys.payments.details(), id] as const,
    stats: () => [...queryKeys.payments.all, 'stats'] as const,
    export: (filters: Record<string, any>) =>
      [...queryKeys.payments.all, 'export', filters] as const,
  },

  // Merchant
  merchant: {
    all: ['merchant'] as const,
    profile: () => [...queryKeys.merchant.all, 'profile'] as const,
    balance: () => [...queryKeys.merchant.all, 'balance'] as const,
    stats: (period: string) =>
      [...queryKeys.merchant.all, 'stats', period] as const,
    transactions: (params: Record<string, any>) =>
      [...queryKeys.merchant.all, 'transactions', params] as const,
    withdrawals: () => [...queryKeys.merchant.all, 'withdrawals'] as const,
    documents: () => [...queryKeys.merchant.all, 'documents'] as const,
    notifications: () => [...queryKeys.merchant.all, 'notifications'] as const,
  },

  // Webhooks
  webhooks: {
    all: ['webhooks'] as const,
    lists: () => [...queryKeys.webhooks.all, 'list'] as const,
    detail: (id: string) => [...queryKeys.webhooks.all, 'detail', id] as const,
    deliveries: (webhookId: string, params: Record<string, any>) =>
      [...queryKeys.webhooks.all, 'deliveries', webhookId, params] as const,
    stats: (webhookId: string, period: string) =>
      [...queryKeys.webhooks.all, 'stats', webhookId, period] as const,
    metrics: (webhookId: string, period: string) =>
      [...queryKeys.webhooks.all, 'metrics', webhookId, period] as const,
    logs: (params: Record<string, any>) =>
      [...queryKeys.webhooks.all, 'logs', params] as const,
    events: () => [...queryKeys.webhooks.all, 'events'] as const,
    config: () => [...queryKeys.webhooks.all, 'config'] as const,
  },

  // Analytics
  analytics: {
    all: ['analytics'] as const,
    dashboard: (period: string) =>
      [...queryKeys.analytics.all, 'dashboard', period] as const,
    transactions: (filters: Record<string, any>) =>
      [...queryKeys.analytics.all, 'transactions', filters] as const,
    revenue: (filters: Record<string, any>) =>
      [...queryKeys.analytics.all, 'revenue', filters] as const,
    metrics: {
      revenue: (period: string) =>
        [...queryKeys.analytics.all, 'metrics', 'revenue', period] as const,
      customers: (period: string) =>
        [...queryKeys.analytics.all, 'metrics', 'customers', period] as const,
      performance: (period: string) =>
        [...queryKeys.analytics.all, 'metrics', 'performance', period] as const,
      fraud: (period: string) =>
        [...queryKeys.analytics.all, 'metrics', 'fraud', period] as const,
    },
    paymentMethods: (period: string) =>
      [...queryKeys.analytics.all, 'payment-methods', period] as const,
    geographic: (period: string) =>
      [...queryKeys.analytics.all, 'geographic', period] as const,
    cohorts: (type: string, periods: number) =>
      [...queryKeys.analytics.all, 'cohorts', type, periods] as const,
    funnel: (period: string) =>
      [...queryKeys.analytics.all, 'funnel', period] as const,
    abTests: (testId?: string) =>
      [...queryKeys.analytics.all, 'ab-tests', testId] as const,
    realtime: () => [...queryKeys.analytics.all, 'realtime'] as const,
    predictions: (metric: string, horizon: number) =>
      [...queryKeys.analytics.all, 'predictions', metric, horizon] as const,
    customReport: (params: Record<string, any>) =>
      [...queryKeys.analytics.all, 'custom-report', params] as const,
    scheduledReports: () =>
      [...queryKeys.analytics.all, 'scheduled-reports'] as const,
  },
};

// Query configuration presets
export const queryConfig = {
  // Real-time data (short cache, frequent refetch)
  realtime: {
    staleTime: 30 * 1000, // 30 seconds
    gcTime: 2 * 60 * 1000, // 2 minutes
    refetchInterval: 30 * 1000, // 30 seconds
    refetchIntervalInBackground: true,
  },

  // Static data (long cache, rare refetch)
  static: {
    staleTime: 60 * 60 * 1000, // 1 hour
    gcTime: 24 * 60 * 60 * 1000, // 24 hours
    refetchOnMount: false,
    refetchOnWindowFocus: false,
  },

  // User-specific data (medium cache)
  user: {
    staleTime: 5 * 60 * 1000, // 5 minutes
    gcTime: 30 * 60 * 1000, // 30 minutes
  },

  // List data with pagination
  list: {
    staleTime: 2 * 60 * 1000, // 2 minutes
    gcTime: 10 * 60 * 1000, // 10 minutes
    keepPreviousData: true, // Keep previous page data while loading new page
  },
};

// Utility functions
export const invalidateQueries = {
  payments: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.payments.all });
  },

  merchant: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.merchant.all });
  },

  webhooks: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.webhooks.all });
  },

  analytics: () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.analytics.all });
  },

  all: () => {
    queryClient.invalidateQueries();
  },
};

export const prefetchQueries = {
  dashboardData: async () => {
    await Promise.all([
      queryClient.prefetchQuery({
        queryKey: queryKeys.analytics.dashboard('7d'),
        queryFn: () => import('@/services/analyticsService').then(s => s.default.getDashboardStats('7d')),
        ...queryConfig.user,
      }),
      queryClient.prefetchQuery({
        queryKey: queryKeys.payments.list({}),
        queryFn: () => import('@/services/paymentService').then(s => s.default.getPayments({})),
        ...queryConfig.list,
      }),
      queryClient.prefetchQuery({
        queryKey: queryKeys.analytics.realtime(),
        queryFn: () => import('@/services/analyticsService').then(s => s.default.getRealtimeMetrics()),
        ...queryConfig.realtime,
      }),
    ]);
  },
};

// Offline support utilities
export const syncPendingMutations = async () => {
  const pendingActions = useAppStore.getState().pendingActions;
  const removePendingAction = useAppStore.getState().removePendingAction;

  for (const action of pendingActions) {
    try {
      // Execute pending mutation based on type
      switch (action.type) {
        case 'CREATE_PAYMENT':
          await queryClient.executeMutation({
            mutationFn: () => import('@/services/paymentService').then(s =>
              s.default.createPixPayment(action.payload)
            ),
          });
          break;

        case 'CANCEL_PAYMENT':
          await queryClient.executeMutation({
            mutationFn: () => import('@/services/paymentService').then(s =>
              s.default.cancelPayment(action.payload.id, action.payload.reason)
            ),
          });
          break;

        case 'UPDATE_WEBHOOK':
          await queryClient.executeMutation({
            mutationFn: () => import('@/services/webhookService').then(s =>
              s.default.updateWebhook(action.payload.id, action.payload.data)
            ),
          });
          break;

        // Add more mutation types as needed
        default:
          console.warn(`Unknown pending action type: ${action.type}`);
      }

      // Remove from pending list if successful
      removePendingAction(action.id);
    } catch (error) {
      console.error(`Failed to sync pending action ${action.id}:`, error);
    }
  }
};