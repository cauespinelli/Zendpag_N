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
// ============================================
// CONSTANTES ADICIONAIS PARA NOVOS MÓDULOS
// ============================================

// Estados Brasileiros
export const BRAZILIAN_STATES = [
  { value: 'AC', label: 'Acre' },
  { value: 'AL', label: 'Alagoas' },
  { value: 'AP', label: 'Amapá' },
  { value: 'AM', label: 'Amazonas' },
  { value: 'BA', label: 'Bahia' },
  { value: 'CE', label: 'Ceará' },
  { value: 'DF', label: 'Distrito Federal' },
  { value: 'ES', label: 'Espírito Santo' },
  { value: 'GO', label: 'Goiás' },
  { value: 'MA', label: 'Maranhão' },
  { value: 'MT', label: 'Mato Grosso' },
  { value: 'MS', label: 'Mato Grosso do Sul' },
  { value: 'MG', label: 'Minas Gerais' },
  { value: 'PA', label: 'Pará' },
  { value: 'PB', label: 'Paraíba' },
  { value: 'PR', label: 'Paraná' },
  { value: 'PE', label: 'Pernambuco' },
  { value: 'PI', label: 'Piauí' },
  { value: 'RJ', label: 'Rio de Janeiro' },
  { value: 'RN', label: 'Rio Grande do Norte' },
  { value: 'RS', label: 'Rio Grande do Sul' },
  { value: 'RO', label: 'Rondônia' },
  { value: 'RR', label: 'Roraima' },
  { value: 'SC', label: 'Santa Catarina' },
  { value: 'SP', label: 'São Paulo' },
  { value: 'SE', label: 'Sergipe' },
  { value: 'TO', label: 'Tocantins' },
] as const;

// Establishment Status
export const ESTABLISHMENT_STATUS_COLORS = {
  ACTIVE: '#52c41a',
  INACTIVE: '#8c8c8c',
  PENDING: '#faad14',
  SUSPENDED: '#ff4d4f',
  BLOCKED: '#ff4d4f',
} as const;

export const ESTABLISHMENT_STATUS_LABELS = {
  ACTIVE: 'Ativo',
  INACTIVE: 'Inativo',
  PENDING: 'Pendente',
  SUSPENDED: 'Suspenso',
  BLOCKED: 'Bloqueado',
} as const;

// Document Status
export const DOCUMENT_STATUS_COLORS = {
  PENDING: '#faad14',
  APPROVED: '#52c41a',
  REJECTED: '#ff4d4f',
  EXPIRED: '#8c8c8c',
} as const;

export const DOCUMENT_STATUS_LABELS = {
  PENDING: 'Pendente',
  APPROVED: 'Aprovado',
  REJECTED: 'Rejeitado',
  EXPIRED: 'Expirado',
} as const;

// Acquirer Providers
export const ACQUIRER_PROVIDERS = [
  { value: 'CIELO', label: 'Cielo' },
  { value: 'REDE', label: 'Rede' },
  { value: 'STONE', label: 'Stone' },
  { value: 'PAGSEGURO', label: 'PagSeguro' },
  { value: 'GETNET', label: 'Getnet' },
  { value: 'SAFRA', label: 'Safra' },
  { value: 'MERCADOPAGO', label: 'Mercado Pago' },
  { value: 'PAGARME', label: 'Pagar.me' },
] as const;

// Transaction Status
export const TRANSACTION_STATUS_COLORS = {
  PENDING: '#faad14',
  PROCESSING: '#1890ff',
  COMPLETED: '#52c41a',
  FAILED: '#ff4d4f',
  CANCELLED: '#8c8c8c',
  REFUNDED: '#722ed1',
} as const;

export const TRANSACTION_STATUS_LABELS = {
  PENDING: 'Pendente',
  PROCESSING: 'Processando',
  COMPLETED: 'Concluído',
  FAILED: 'Falhou',
  CANCELLED: 'Cancelado',
  REFUNDED: 'Estornado',
} as const;

// Transaction Method
export const TRANSACTION_METHOD_COLORS = {
  PIX: '#32BCAD',
  CREDIT_CARD: '#1890ff',
  DEBIT_CARD: '#13c2c2',
  BOLETO: '#faad14',
  TED: '#722ed1',
  DOC: '#eb2f96',
} as const;

export const TRANSACTION_METHOD_LABELS = {
  PIX: 'PIX',
  CREDIT_CARD: 'Cartão de Crédito',
  DEBIT_CARD: 'Cartão de Débito',
  BOLETO: 'Boleto',
  TED: 'TED',
  DOC: 'DOC',
} as const;

// Withdrawal Status
export const WITHDRAWAL_STATUS_COLORS = {
  PENDING: '#faad14',
  PROCESSING: '#1890ff',
  COMPLETED: '#52c41a',
  FAILED: '#ff4d4f',
  CANCELLED: '#8c8c8c',
} as const;

export const WITHDRAWAL_STATUS_LABELS = {
  PENDING: 'Pendente',
  PROCESSING: 'Processando',
  COMPLETED: 'Concluído',
  FAILED: 'Falhou',
  CANCELLED: 'Cancelado',
} as const;

// Withdrawal Type
export const WITHDRAWAL_TYPE_LABELS = {
  MANUAL: 'Manual',
  AUTOMATIC: 'Automático',
  SCHEDULED: 'Agendado',
} as const;

// PIX Key Type Labels
export const PIX_KEY_TYPE_LABELS = {
  CPF: 'CPF',
  CNPJ: 'CNPJ',
  EMAIL: 'E-mail',
  PHONE: 'Telefone',
  EVP: 'Chave Aleatória',
} as const;

// Wallet Type
export const WALLET_TYPE_LABELS = {
  MAIN: 'Principal',
  RESERVE: 'Reserva',
  BLOCKED: 'Bloqueado',
} as const;

// Dispute Status
export const DISPUTE_STATUS_COLORS = {
  OPEN: '#faad14',
  IN_ANALYSIS: '#1890ff',
  WAITING_DOCUMENTS: '#722ed1',
  RESOLVED: '#52c41a',
  CLOSED: '#8c8c8c',
  WON: '#52c41a',
  LOST: '#ff4d4f',
} as const;

export const DISPUTE_STATUS_LABELS = {
  OPEN: 'Aberta',
  IN_ANALYSIS: 'Em Análise',
  WAITING_DOCUMENTS: 'Aguardando Documentos',
  RESOLVED: 'Resolvida',
  CLOSED: 'Fechada',
  WON: 'Ganha',
  LOST: 'Perdida',
} as const;

// Dispute Priority
export const DISPUTE_PRIORITY_COLORS = {
  LOW: '#52c41a',
  MEDIUM: '#faad14',
  HIGH: '#ff7a45',
  CRITICAL: '#ff4d4f',
} as const;

export const DISPUTE_PRIORITY_LABELS = {
  LOW: 'Baixa',
  MEDIUM: 'Média',
  HIGH: 'Alta',
  CRITICAL: 'Crítica',
} as const;

// Affiliate Status
export const AFFILIATE_STATUS_COLORS = {
  ACTIVE: '#52c41a',
  INACTIVE: '#8c8c8c',
  PENDING: '#faad14',
  SUSPENDED: '#ff4d4f',
} as const;

export const AFFILIATE_STATUS_LABELS = {
  ACTIVE: 'Ativo',
  INACTIVE: 'Inativo',
  PENDING: 'Pendente',
  SUSPENDED: 'Suspenso',
} as const;

// Commission Status
export const COMMISSION_STATUS_COLORS = {
  PENDING: '#faad14',
  APPROVED: '#52c41a',
  PAID: '#1890ff',
  CANCELLED: '#8c8c8c',
} as const;

export const COMMISSION_STATUS_LABELS = {
  PENDING: 'Pendente',
  APPROVED: 'Aprovada',
  PAID: 'Paga',
  CANCELLED: 'Cancelada',
} as const;

// Statement Entry Type
export const STATEMENT_ENTRY_TYPE_COLORS = {
  CREDIT: '#52c41a',
  DEBIT: '#ff4d4f',
  FEE: '#faad14',
  REFUND: '#722ed1',
  ADJUSTMENT: '#1890ff',
} as const;

export const STATEMENT_ENTRY_TYPE_LABELS = {
  CREDIT: 'Crédito',
  DEBIT: 'Débito',
  FEE: 'Taxa',
  REFUND: 'Estorno',
  ADJUSTMENT: 'Ajuste',
} as const;

// Statement Category
export const STATEMENT_CATEGORY_COLORS = {
  PAYMENT: '#52c41a',
  WITHDRAWAL: '#1890ff',
  FEE: '#faad14',
  REFUND: '#722ed1',
  CHARGEBACK: '#ff4d4f',
  TRANSFER: '#13c2c2',
} as const;

export const STATEMENT_CATEGORY_LABELS = {
  PAYMENT: 'Pagamento',
  WITHDRAWAL: 'Saque',
  FEE: 'Taxa',
  REFUND: 'Estorno',
  CHARGEBACK: 'Chargeback',
  TRANSFER: 'Transferência',
} as const;
