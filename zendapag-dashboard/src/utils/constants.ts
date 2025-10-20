// @ts-nocheck
export const API_ENDPOINTS = {
  AUTH: {
    LOGIN: '/auth/login',
    REFRESH: '/auth/refresh',
    LOGOUT: '/auth/logout',
    ME: '/auth/me',
  },
  MERCHANTS: {
    BASE: '/merchants',
    ME: '/merchants/me',
    BALANCE: '/merchants/me/balance',
    API_KEY: '/merchants/me/api-key',
  },
  PAYMENTS: {
    BASE: '/payments',
    PIX: '/payments/pix',
    CANCEL: (id: string) => `/payments/${id}/cancel`,
    QR_CODE: (id: string) => `/payments/${id}/qr-code`,
  },
  WEBHOOKS: {
    BASE: '/webhooks',
    BY_ID: (id: string) => `/webhooks/${id}`,
    TEST: (id: string) => `/webhooks/${id}/test`,
  },
  ANALYTICS: {
    DASHBOARD: '/analytics/dashboard',
    TRANSACTIONS: '/analytics/transactions',
    REVENUE: '/analytics/revenue',
  },
  REPORTS: {
    BASE: '/reports',
    TRANSACTIONS: '/reports/transactions',
    DOWNLOAD: (id: string) => `/reports/${id}/download`,
  },
} as const;

export const STORAGE_KEYS = {
  AUTH_TOKEN: 'zendapag_auth_token',
  REFRESH_TOKEN: 'zendapag_refresh_token',
  USER_DATA: 'zendapag_user_data',
  THEME_CONFIG: 'zendapag_theme_config',
  SIDEBAR_COLLAPSED: 'zendapag_sidebar_collapsed',
} as const;

export const PAYMENT_STATUS_COLORS = {
  PENDING: '#faad14',
  ACTIVE: '#1890ff',
  COMPLETED: '#52c41a',
  FAILED: '#ff4d4f',
  CANCELLED: '#d9d9d9',
  EXPIRED: '#8c8c8c',
} as const;

export const PAYMENT_STATUS_LABELS = {
  PENDING: 'Pendente',
  ACTIVE: 'Ativo',
  COMPLETED: 'Concluído',
  FAILED: 'Falhou',
  CANCELLED: 'Cancelado',
  EXPIRED: 'Expirado',
} as const;

export const PIX_KEY_TYPES = {
  CPF: 'CPF',
  CNPJ: 'CNPJ',
  EMAIL: 'E-mail',
  PHONE: 'Telefone',
  EVP: 'Chave Aleatória',
} as const;

export const WEBHOOK_EVENTS = {
  'payment.created': 'Pagamento Criado',
  'payment.completed': 'Pagamento Concluído',
  'payment.failed': 'Pagamento Falhou',
  'payment.cancelled': 'Pagamento Cancelado',
  'payment.expired': 'Pagamento Expirado',
} as const;

export const DATE_FORMATS = {
  DISPLAY: 'DD/MM/YYYY HH:mm:ss',
  DISPLAY_DATE: 'DD/MM/YYYY',
  DISPLAY_TIME: 'HH:mm:ss',
  ISO: 'YYYY-MM-DDTHH:mm:ss',
  API: 'YYYY-MM-DD',
} as const;

export const CURRENCY_CONFIG = {
  BRL: {
    symbol: 'R$',
    code: 'BRL',
    decimals: 2,
    locale: 'pt-BR',
  },
} as const;

export const PAGINATION_DEFAULTS = {
  PAGE_SIZE: 20,
  PAGE_SIZE_OPTIONS: ['10', '20', '50', '100'],
} as const;

export const QUERY_KEYS = {
  USER: ['user'],
  MERCHANT: ['merchant'],
  PAYMENTS: ['payments'],
  PAYMENT: (id: string) => ['payment', id],
  WEBHOOKS: ['webhooks'],
  WEBHOOK: (id: string) => ['webhook', id],
  ANALYTICS: ['analytics'],
  DASHBOARD_STATS: ['dashboard-stats'],
  REPORTS: ['reports'],
} as const;

export const ROUTES = {
  HOME: '/',
  LOGIN: '/login',
  DASHBOARD: '/dashboard',
  PAYMENTS: '/payments',
  PAYMENT_DETAILS: (id: string) => `/payments/${id}`,
  ANALYTICS: '/analytics',
  WEBHOOKS: '/webhooks',
  REPORTS: '/reports',
  PROFILE: '/profile',
  SETTINGS: '/settings',
} as const;

export const THEME_CONFIG = {
  PRIMARY_COLOR: '#1890ff',
  SUCCESS_COLOR: '#52c41a',
  WARNING_COLOR: '#faad14',
  ERROR_COLOR: '#ff4d4f',
  INFO_COLOR: '#1890ff',
  BORDER_RADIUS: 6,
  BOX_SHADOW: '0 2px 8px rgba(0, 0, 0, 0.1)',
} as const;

export const CHART_COLORS = {
  PRIMARY: '#1890ff',
  SUCCESS: '#52c41a',
  WARNING: '#faad14',
  ERROR: '#ff4d4f',
  INFO: '#13c2c2',
  SECONDARY: '#722ed1',
  GRADIENT: [
    '#1890ff',
    '#52c41a',
    '#faad14',
    '#ff4d4f',
    '#13c2c2',
    '#722ed1',
    '#eb2f96',
    '#fa8c16',
  ],
} as const;

export const NOTIFICATION_DURATION = {
  SUCCESS: 3000,
  ERROR: 5000,
  WARNING: 4000,
  INFO: 3000,
} as const;

export const FILE_UPLOAD = {
  MAX_SIZE: 5 * 1024 * 1024, // 5MB
  ACCEPTED_TYPES: [
    'image/jpeg',
    'image/png',
    'image/gif',
    'application/pdf',
    'text/csv',
    'application/json',
  ],
} as const;

export const VALIDATION_RULES = {
  EMAIL: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
  CPF: /^\d{11}$/,
  CNPJ: /^\d{14}$/,
  PHONE: /^\+?55\d{10,11}$/,
  PIX_KEY: {
    CPF: /^\d{11}$/,
    CNPJ: /^\d{14}$/,
    EMAIL: /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i,
    PHONE: /^\+55\d{10,11}$/,
    EVP: /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i,
  },
} as const;

export const ERROR_MESSAGES = {
  NETWORK_ERROR: 'Erro de conexão. Verifique sua internet.',
  UNAUTHORIZED: 'Sessão expirada. Faça login novamente.',
  FORBIDDEN: 'Você não tem permissão para realizar esta ação.',
  NOT_FOUND: 'Recurso não encontrado.',
  SERVER_ERROR: 'Erro interno do servidor. Tente novamente mais tarde.',
  VALIDATION_ERROR: 'Dados inválidos. Verifique os campos.',
  TIMEOUT: 'Operação demorou muito. Tente novamente.',
} as const;