// @ts-nocheck
// User and Authentication
export interface User {
  id: string;
  name: string;
  email: string;
  roles: string[];
  merchantId: string;
  merchantName: string;
  avatar?: string;
  lastLogin?: string;
  permissions: string[];
}

export interface LoginRequest {
  email: string;
  password: string;
  rememberMe?: boolean;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: User;
  expiresIn: number;
}

// Merchant
export interface Merchant {
  id: string;
  name: string;
  tradingName?: string;
  document: string;
  email: string;
  phoneNumber?: string;
  status: 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION';
  createdAt: string;
  updatedAt: string;
  address?: Address;
  balance: number;
  apiKey?: string;
  webhookUrl?: string;
}

export interface Address {
  street: string;
  number: string;
  complement?: string;
  neighborhood: string;
  city: string;
  state: string;
  zipCode: string;
  country: string;
}

// Payment
export interface Payment {
  id: string;
  referenceId: string;
  pixTxId?: string;
  amount: number;
  currency: string;
  status: PaymentStatus;
  description?: string;
  pixKey?: string;
  pixKeyType?: 'CPF' | 'CNPJ' | 'EMAIL' | 'PHONE' | 'EVP';
  qrCodeText?: string;
  qrCodeImage?: string;
  customerName?: string;
  customerEmail?: string;
  customerDocument?: string;
  payerName?: string;
  payerDocument?: string;
  payerBank?: string;
  merchantId: string;
  createdAt: string;
  updatedAt: string;
  expiresAt?: string;
  paidAt?: string;
  processedAt?: string;
  failureReason?: string;
  cancellationReason?: string;
}

export type PaymentStatus =
  | 'PENDING'
  | 'ACTIVE'
  | 'COMPLETED'
  | 'FAILED'
  | 'CANCELLED'
  | 'EXPIRED';

// Webhook
export interface Webhook {
  id: string;
  merchantId: string;
  url: string;
  events: WebhookEvent[];
  secret?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  lastTriggeredAt?: string;
  successCount: number;
  failureCount: number;
}

export type WebhookEvent =
  | 'payment.created'
  | 'payment.completed'
  | 'payment.failed'
  | 'payment.cancelled'
  | 'payment.expired';

// Analytics
export interface AnalyticsData {
  period: string;
  totalTransactions: number;
  totalAmount: number;
  successRate: number;
  completedTransactions: number;
  failedTransactions: number;
  cancelledTransactions: number;
  averageAmount: number;
  averageProcessingTime: number;
  topPixKeyTypes: Array<{
    type: string;
    count: number;
    percentage: number;
  }>;
  dailyStats: Array<{
    date: string;
    transactions: number;
    amount: number;
    successRate: number;
  }>;
  hourlyStats: Array<{
    hour: number;
    transactions: number;
    amount: number;
  }>;
}

// Reports
export interface TransactionReport {
  id: string;
  merchantId: string;
  reportType: 'TRANSACTIONS' | 'FINANCIAL' | 'RECONCILIATION';
  period: {
    startDate: string;
    endDate: string;
  };
  status: 'GENERATING' | 'COMPLETED' | 'FAILED';
  format: 'CSV' | 'JSON' | 'PDF';
  downloadUrl?: string;
  createdAt: string;
  completedAt?: string;
  recordCount?: number;
  fileSize?: number;
}

// API Response
export interface ApiResponse<T = any> {
  success: boolean;
  data?: T;
  message?: string;
  error?: string;
  timestamp: string;
}

export interface PaginatedResponse<T = any> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  first: boolean;
  last: boolean;
}

// Dashboard
export interface DashboardStats {
  totalTransactions: number;
  totalAmount: number;
  successRate: number;
  averageTicket: number;
  transactionsToday: number;
  amountToday: number;
  pendingTransactions: number;
  activeTransactions: number;
  recentTransactions: Payment[];
  monthlyGrowth: {
    transactions: number;
    amount: number;
  };
}

// Theme
export interface ThemeConfig {
  primaryColor: string;
  darkMode: boolean;
  sidebarCollapsed: boolean;
  language: 'pt-BR' | 'en-US';
}

// Form types
export interface PaymentFormData {
  amount: number;
  description?: string;
  customerName?: string;
  customerEmail?: string;
  customerDocument?: string;
  expirationMinutes?: number;
}

export interface WebhookFormData {
  url: string;
  events: WebhookEvent[];
  secret?: string;
  active: boolean;
}

// Error types
export interface ApiError {
  code: string;
  message: string;
  details?: Record<string, any>;
  timestamp: string;
}

// Utility types
export type LoadingState = 'idle' | 'loading' | 'succeeded' | 'failed';

export interface AsyncState<T = any> {
  data?: T;
  loading: boolean;
  error?: string;
}

// Chart data types
export interface ChartData {
  labels: string[];
  datasets: Array<{
    label: string;
    data: number[];
    backgroundColor?: string | string[];
    borderColor?: string | string[];
    borderWidth?: number;
  }>;
}

export interface TimeSeriesData {
  timestamp: string;
  value: number;
  label?: string;
}

// Notification types
export interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  title: string;
  message: string;
  timestamp: string;
  read: boolean;
  actions?: Array<{
    label: string;
    action: () => void;
  }>;
}
// ============================================
// TIPOS E LABELS PARA NOVOS MÓDULOS
// ============================================

// Dispute Reason Labels
export const DISPUTE_REASON_LABELS: Record<string, string> = {
  FRAUD: 'Fraude',
  UNAUTHORIZED: 'Não Autorizado',
  DUPLICATE: 'Duplicado',
  NOT_RECEIVED: 'Não Recebido',
  DEFECTIVE: 'Produto Defeituoso',
  NOT_AS_DESCRIBED: 'Produto Diferente',
  CANCELLED: 'Cancelamento',
  REFUND_NOT_PROCESSED: 'Estorno Não Processado',
  OTHER: 'Outro',
};
