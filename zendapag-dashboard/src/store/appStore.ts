// @ts-nocheck
import { create } from 'zustand';
import { devtools, subscribeWithSelector } from 'zustand/middleware';
import { immer } from 'zustand/middleware/immer';
import type {
  User,
  Payment,
  Merchant,
  Webhook,
  AnalyticsData,
  DashboardStats,
  WebhookDelivery,
  PaginatedResponse
} from '@/types';

export interface NotificationState {
  id: string;
  type: 'success' | 'error' | 'info' | 'warning';
  title: string;
  message?: string;
  duration?: number;
  action?: {
    label: string;
    onClick: () => void;
  };
}

export interface LoadingState {
  payments: boolean;
  merchant: boolean;
  analytics: boolean;
  webhooks: boolean;
  global: boolean;
}

export interface FilterState {
  payments: {
    status?: string;
    startDate?: string;
    endDate?: string;
    search?: string;
    amountMin?: number;
    amountMax?: number;
  };
  webhooks: {
    status?: string;
    eventType?: string;
  };
  analytics: {
    period: string;
    groupBy: 'hour' | 'day' | 'week' | 'month';
  };
}

export interface WebSocketState {
  connected: boolean;
  reconnectAttempts: number;
  lastHeartbeat?: Date;
  subscriptions: Set<string>;
}

export interface AppState {
  // User & Auth
  user: User | null;
  isAuthenticated: boolean;

  // Data
  payments: PaginatedResponse<Payment> | null;
  selectedPayment: Payment | null;
  merchant: Merchant | null;
  webhooks: Webhook[];
  webhookDeliveries: Record<string, WebhookDelivery[]>;
  analytics: AnalyticsData | null;
  dashboardStats: DashboardStats | null;

  // UI State
  loading: LoadingState;
  filters: FilterState;
  notifications: NotificationState[];
  sidebarCollapsed: boolean;
  theme: 'light' | 'dark' | 'system';

  // WebSocket
  websocket: WebSocketState;

  // Cache & Offline
  lastSync: Date | null;
  offlineMode: boolean;
  pendingActions: Array<{
    id: string;
    type: string;
    payload: any;
    timestamp: Date;
  }>;

  // Actions - Auth
  setUser: (user: User | null) => void;
  login: (user: User, token: string) => void;
  logout: () => void;

  // Actions - Payments
  setPayments: (payments: PaginatedResponse<Payment>) => void;
  addPayment: (payment: Payment) => void;
  updatePayment: (id: string, payment: Partial<Payment>) => void;
  setSelectedPayment: (payment: Payment | null) => void;

  // Actions - Merchant
  setMerchant: (merchant: Merchant) => void;
  updateMerchant: (updates: Partial<Merchant>) => void;

  // Actions - Webhooks
  setWebhooks: (webhooks: Webhook[]) => void;
  addWebhook: (webhook: Webhook) => void;
  updateWebhook: (id: string, webhook: Partial<Webhook>) => void;
  removeWebhook: (id: string) => void;
  setWebhookDeliveries: (webhookId: string, deliveries: WebhookDelivery[]) => void;

  // Actions - Analytics
  setAnalytics: (analytics: AnalyticsData) => void;
  setDashboardStats: (stats: DashboardStats) => void;

  // Actions - Loading
  setLoading: (key: keyof LoadingState, loading: boolean) => void;
  setGlobalLoading: (loading: boolean) => void;

  // Actions - Filters
  setPaymentFilters: (filters: Partial<FilterState['payments']>) => void;
  setWebhookFilters: (filters: Partial<FilterState['webhooks']>) => void;
  setAnalyticsFilters: (filters: Partial<FilterState['analytics']>) => void;
  clearFilters: () => void;

  // Actions - Notifications
  addNotification: (notification: Omit<NotificationState, 'id'>) => void;
  removeNotification: (id: string) => void;
  clearNotifications: () => void;

  // Actions - UI
  toggleSidebar: () => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  setTheme: (theme: 'light' | 'dark' | 'system') => void;

  // Actions - WebSocket
  setWebSocketConnected: (connected: boolean) => void;
  incrementReconnectAttempts: () => void;
  resetReconnectAttempts: () => void;
  updateHeartbeat: () => void;
  addSubscription: (channel: string) => void;
  removeSubscription: (channel: string) => void;

  // Actions - Cache & Offline
  setLastSync: (date: Date) => void;
  setOfflineMode: (offline: boolean) => void;
  addPendingAction: (action: { type: string; payload: any }) => void;
  removePendingAction: (id: string) => void;
  clearPendingActions: () => void;

  // Real-time updates
  handleRealtimeUpdate: (event: {
    type: 'payment_updated' | 'webhook_delivery' | 'analytics_update';
    data: any;
  }) => void;
}

const initialLoadingState: LoadingState = {
  payments: false,
  merchant: false,
  analytics: false,
  webhooks: false,
  global: false,
};

const initialFilterState: FilterState = {
  payments: {},
  webhooks: {},
  analytics: {
    period: '30d',
    groupBy: 'day',
  },
};

const initialWebSocketState: WebSocketState = {
  connected: false,
  reconnectAttempts: 0,
  subscriptions: new Set(),
};

export const useAppStore = create<AppState>()(
  devtools(
    subscribeWithSelector(
      immer((set, get) => ({
        // Initial State
        user: null,
        isAuthenticated: false,
        payments: null,
        selectedPayment: null,
        merchant: null,
        webhooks: [],
        webhookDeliveries: {},
        analytics: null,
        dashboardStats: null,
        loading: initialLoadingState,
        filters: initialFilterState,
        notifications: [],
        sidebarCollapsed: false,
        theme: 'system',
        websocket: initialWebSocketState,
        lastSync: null,
        offlineMode: false,
        pendingActions: [],

        // Auth Actions
        setUser: (user) =>
          set((state) => {
            state.user = user;
            state.isAuthenticated = !!user;
          }),

        login: (user, token) =>
          set((state) => {
            state.user = user;
            state.isAuthenticated = true;
            localStorage.setItem('auth_token', token);
            localStorage.setItem('user', JSON.stringify(user));
          }),

        logout: () =>
          set((state) => {
            state.user = null;
            state.isAuthenticated = false;
            state.payments = null;
            state.merchant = null;
            state.webhooks = [];
            state.analytics = null;
            state.dashboardStats = null;
            localStorage.removeItem('auth_token');
            localStorage.removeItem('user');
          }),

        // Payment Actions
        setPayments: (payments) =>
          set((state) => {
            state.payments = payments;
          }),

        addPayment: (payment) =>
          set((state) => {
            if (state.payments) {
              state.payments.data.unshift(payment);
              state.payments.total += 1;
            }
          }),

        updatePayment: (id, updates) =>
          set((state) => {
            if (state.payments) {
              const index = state.payments.data.findIndex((p) => p.id === id);
              if (index !== -1) {
                Object.assign(state.payments.data[index], updates);
              }
            }
            if (state.selectedPayment?.id === id) {
              Object.assign(state.selectedPayment, updates);
            }
          }),

        setSelectedPayment: (payment) =>
          set((state) => {
            state.selectedPayment = payment;
          }),

        // Merchant Actions
        setMerchant: (merchant) =>
          set((state) => {
            state.merchant = merchant;
          }),

        updateMerchant: (updates) =>
          set((state) => {
            if (state.merchant) {
              Object.assign(state.merchant, updates);
            }
          }),

        // Webhook Actions
        setWebhooks: (webhooks) =>
          set((state) => {
            state.webhooks = webhooks;
          }),

        addWebhook: (webhook) =>
          set((state) => {
            state.webhooks.push(webhook);
          }),

        updateWebhook: (id, updates) =>
          set((state) => {
            const index = state.webhooks.findIndex((w) => w.id === id);
            if (index !== -1) {
              Object.assign(state.webhooks[index], updates);
            }
          }),

        removeWebhook: (id) =>
          set((state) => {
            state.webhooks = state.webhooks.filter((w) => w.id !== id);
            delete state.webhookDeliveries[id];
          }),

        setWebhookDeliveries: (webhookId, deliveries) =>
          set((state) => {
            state.webhookDeliveries[webhookId] = deliveries;
          }),

        // Analytics Actions
        setAnalytics: (analytics) =>
          set((state) => {
            state.analytics = analytics;
          }),

        setDashboardStats: (stats) =>
          set((state) => {
            state.dashboardStats = stats;
          }),

        // Loading Actions
        setLoading: (key, loading) =>
          set((state) => {
            state.loading[key] = loading;
          }),

        setGlobalLoading: (loading) =>
          set((state) => {
            state.loading.global = loading;
          }),

        // Filter Actions
        setPaymentFilters: (filters) =>
          set((state) => {
            Object.assign(state.filters.payments, filters);
          }),

        setWebhookFilters: (filters) =>
          set((state) => {
            Object.assign(state.filters.webhooks, filters);
          }),

        setAnalyticsFilters: (filters) =>
          set((state) => {
            Object.assign(state.filters.analytics, filters);
          }),

        clearFilters: () =>
          set((state) => {
            state.filters = initialFilterState;
          }),

        // Notification Actions
        addNotification: (notification) =>
          set((state) => {
            const id = Date.now().toString();
            state.notifications.push({ ...notification, id });
          }),

        removeNotification: (id) =>
          set((state) => {
            state.notifications = state.notifications.filter((n) => n.id !== id);
          }),

        clearNotifications: () =>
          set((state) => {
            state.notifications = [];
          }),

        // UI Actions
        toggleSidebar: () =>
          set((state) => {
            state.sidebarCollapsed = !state.sidebarCollapsed;
          }),

        setSidebarCollapsed: (collapsed) =>
          set((state) => {
            state.sidebarCollapsed = collapsed;
          }),

        setTheme: (theme) =>
          set((state) => {
            state.theme = theme;
            localStorage.setItem('theme', theme);
          }),

        // WebSocket Actions
        setWebSocketConnected: (connected) =>
          set((state) => {
            state.websocket.connected = connected;
            if (connected) {
              state.websocket.reconnectAttempts = 0;
            }
          }),

        incrementReconnectAttempts: () =>
          set((state) => {
            state.websocket.reconnectAttempts += 1;
          }),

        resetReconnectAttempts: () =>
          set((state) => {
            state.websocket.reconnectAttempts = 0;
          }),

        updateHeartbeat: () =>
          set((state) => {
            state.websocket.lastHeartbeat = new Date();
          }),

        addSubscription: (channel) =>
          set((state) => {
            state.websocket.subscriptions.add(channel);
          }),

        removeSubscription: (channel) =>
          set((state) => {
            state.websocket.subscriptions.delete(channel);
          }),

        // Cache & Offline Actions
        setLastSync: (date) =>
          set((state) => {
            state.lastSync = date;
          }),

        setOfflineMode: (offline) =>
          set((state) => {
            state.offlineMode = offline;
          }),

        addPendingAction: (action) =>
          set((state) => {
            const id = Date.now().toString();
            state.pendingActions.push({
              ...action,
              id,
              timestamp: new Date(),
            });
          }),

        removePendingAction: (id) =>
          set((state) => {
            state.pendingActions = state.pendingActions.filter((a) => a.id !== id);
          }),

        clearPendingActions: () =>
          set((state) => {
            state.pendingActions = [];
          }),

        // Real-time Update Handler
        handleRealtimeUpdate: (event) =>
          set((state) => {
            switch (event.type) {
              case 'payment_updated':
                if (state.payments) {
                  const index = state.payments.data.findIndex(
                    (p) => p.id === event.data.id
                  );
                  if (index !== -1) {
                    Object.assign(state.payments.data[index], event.data);
                  }
                }
                if (state.selectedPayment?.id === event.data.id) {
                  Object.assign(state.selectedPayment, event.data);
                }
                break;

              case 'webhook_delivery':
                const { webhookId, delivery } = event.data;
                if (!state.webhookDeliveries[webhookId]) {
                  state.webhookDeliveries[webhookId] = [];
                }
                state.webhookDeliveries[webhookId].unshift(delivery);
                break;

              case 'analytics_update':
                if (state.dashboardStats) {
                  Object.assign(state.dashboardStats, event.data);
                }
                break;
            }
          }),
      }))
    ),
    { name: 'zendapag-app-store' }
  )
);

// Selectors
export const useAuth = () => useAppStore((state) => ({
  user: state.user,
  isAuthenticated: state.isAuthenticated,
  login: state.login,
  logout: state.logout,
}));

export const usePayments = () => useAppStore((state) => ({
  payments: state.payments,
  selectedPayment: state.selectedPayment,
  loading: state.loading.payments,
  filters: state.filters.payments,
  setPayments: state.setPayments,
  addPayment: state.addPayment,
  updatePayment: state.updatePayment,
  setSelectedPayment: state.setSelectedPayment,
  setLoading: (loading: boolean) => state.setLoading('payments', loading),
  setFilters: state.setPaymentFilters,
}));

export const useMerchant = () => useAppStore((state) => ({
  merchant: state.merchant,
  loading: state.loading.merchant,
  setMerchant: state.setMerchant,
  updateMerchant: state.updateMerchant,
  setLoading: (loading: boolean) => state.setLoading('merchant', loading),
}));

export const useWebhooks = () => useAppStore((state) => ({
  webhooks: state.webhooks,
  deliveries: state.webhookDeliveries,
  loading: state.loading.webhooks,
  filters: state.filters.webhooks,
  setWebhooks: state.setWebhooks,
  addWebhook: state.addWebhook,
  updateWebhook: state.updateWebhook,
  removeWebhook: state.removeWebhook,
  setDeliveries: state.setWebhookDeliveries,
  setLoading: (loading: boolean) => state.setLoading('webhooks', loading),
  setFilters: state.setWebhookFilters,
}));

export const useAnalytics = () => useAppStore((state) => ({
  analytics: state.analytics,
  dashboardStats: state.dashboardStats,
  loading: state.loading.analytics,
  filters: state.filters.analytics,
  setAnalytics: state.setAnalytics,
  setDashboardStats: state.setDashboardStats,
  setLoading: (loading: boolean) => state.setLoading('analytics', loading),
  setFilters: state.setAnalyticsFilters,
}));

export const useNotifications = () => useAppStore((state) => ({
  notifications: state.notifications,
  addNotification: state.addNotification,
  removeNotification: state.removeNotification,
  clearNotifications: state.clearNotifications,
}));

export const useWebSocketStore = () => useAppStore((state) => ({
  connected: state.websocket.connected,
  reconnectAttempts: state.websocket.reconnectAttempts,
  lastHeartbeat: state.websocket.lastHeartbeat,
  subscriptions: state.websocket.subscriptions,
  setConnected: state.setWebSocketConnected,
  incrementReconnectAttempts: state.incrementReconnectAttempts,
  resetReconnectAttempts: state.resetReconnectAttempts,
  updateHeartbeat: state.updateHeartbeat,
  addSubscription: state.addSubscription,
  removeSubscription: state.removeSubscription,
}));

export const useOffline = () => useAppStore((state) => ({
  offlineMode: state.offlineMode,
  lastSync: state.lastSync,
  pendingActions: state.pendingActions,
  setOfflineMode: state.setOfflineMode,
  setLastSync: state.setLastSync,
  addPendingAction: state.addPendingAction,
  removePendingAction: state.removePendingAction,
  clearPendingActions: state.clearPendingActions,
}));