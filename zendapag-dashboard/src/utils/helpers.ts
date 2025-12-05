// @ts-nocheck
import dayjs from 'dayjs';
import 'dayjs/locale/pt-br';
import relativeTime from 'dayjs/plugin/relativeTime';
import duration from 'dayjs/plugin/duration';
import utc from 'dayjs/plugin/utc';
import timezone from 'dayjs/plugin/timezone';

import { CURRENCY_CONFIG, DATE_FORMATS } from './constants';
import type { PaymentStatus, ApiError } from '@/types';

// Configure dayjs
dayjs.extend(relativeTime);
dayjs.extend(duration);
dayjs.extend(utc);
dayjs.extend(timezone);
dayjs.locale('pt-br');

// Currency formatting
export const formatCurrency = (
  amount: number,
  currency: keyof typeof CURRENCY_CONFIG = 'BRL'
): string => {
  const config = CURRENCY_CONFIG[currency];

  return new Intl.NumberFormat(config.locale, {
    style: 'currency',
    currency: config.code,
    minimumFractionDigits: config.decimals,
    maximumFractionDigits: config.decimals,
  }).format(amount);
};

// Number formatting
export const formatNumber = (
  value: number,
  options: Intl.NumberFormatOptions = {}
): string => {
  return new Intl.NumberFormat('pt-BR', options).format(value);
};

export const formatPercentage = (value: number, decimals = 1): string => {
  return `${(value * 100).toFixed(decimals)}%`;
};

// Date formatting
export const formatDate = (
  date: string | Date,
  format = DATE_FORMATS.DISPLAY
): string => {
  return dayjs(date).format(format);
};

export const formatDateRelative = (date: string | Date): string => {
  return dayjs(date).fromNow();
};

export const formatDuration = (milliseconds: number): string => {
  const duration = dayjs.duration(milliseconds);

  if (duration.asHours() >= 1) {
    return `${Math.floor(duration.asHours())}h ${duration.minutes()}m`;
  }

  if (duration.asMinutes() >= 1) {
    return `${Math.floor(duration.asMinutes())}m ${duration.seconds()}s`;
  }

  return `${Math.floor(duration.asSeconds())}s`;
};

export const isToday = (date: string | Date): boolean => {
  return dayjs(date).isSame(dayjs(), 'day');
};

export const isYesterday = (date: string | Date): boolean => {
  return dayjs(date).isSame(dayjs().subtract(1, 'day'), 'day');
};

// String utilities
export const truncateText = (text: string, maxLength: number): string => {
  if (text.length <= maxLength) return text;
  return `${text.substring(0, maxLength)}...`;
};

export const capitalizeFirst = (text: string): string => {
  return text.charAt(0).toUpperCase() + text.slice(1).toLowerCase();
};

export const slugify = (text: string): string => {
  return text
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .replace(/[^a-z0-9]+/g, '-')
    .replace(/^-+|-+$/g, '');
};

// Validation utilities
export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$/i;
  return emailRegex.test(email);
};

export const isValidCPF = (cpf: string): boolean => {
  const cleanCPF = cpf.replace(/\D/g, '');

  if (cleanCPF.length !== 11 || /^(\d)\1{10}$/.test(cleanCPF)) {
    return false;
  }

  let sum = 0;
  for (let i = 0; i < 9; i++) {
    sum += parseInt(cleanCPF.charAt(i)) * (10 - i);
  }

  let remainder = (sum * 10) % 11;
  if (remainder === 10 || remainder === 11) remainder = 0;
  if (remainder !== parseInt(cleanCPF.charAt(9))) return false;

  sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += parseInt(cleanCPF.charAt(i)) * (11 - i);
  }

  remainder = (sum * 10) % 11;
  if (remainder === 10 || remainder === 11) remainder = 0;

  return remainder === parseInt(cleanCPF.charAt(10));
};

export const isValidCNPJ = (cnpj: string): boolean => {
  const cleanCNPJ = cnpj.replace(/\D/g, '');

  if (cleanCNPJ.length !== 14 || /^(\d)\1{13}$/.test(cleanCNPJ)) {
    return false;
  }

  let sum = 0;
  let weight = 2;

  for (let i = 11; i >= 0; i--) {
    sum += parseInt(cleanCNPJ.charAt(i)) * weight;
    weight = weight === 9 ? 2 : weight + 1;
  }

  let remainder = sum % 11;
  const digit1 = remainder < 2 ? 0 : 11 - remainder;

  if (digit1 !== parseInt(cleanCNPJ.charAt(12))) return false;

  sum = 0;
  weight = 2;

  for (let i = 12; i >= 0; i--) {
    sum += parseInt(cleanCNPJ.charAt(i)) * weight;
    weight = weight === 9 ? 2 : weight + 1;
  }

  remainder = sum % 11;
  const digit2 = remainder < 2 ? 0 : 11 - remainder;

  return digit2 === parseInt(cleanCNPJ.charAt(13));
};

// Mask utilities
export const maskCPF = (cpf: string): string => {
  const cleaned = cpf.replace(/\D/g, '');
  return cleaned.replace(/(\d{3})(\d{3})(\d{3})(\d{2})/, '$1.$2.$3-$4');
};

export const maskCNPJ = (cnpj: string): string => {
  const cleaned = cnpj.replace(/\D/g, '');
  return cleaned.replace(/(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})/, '$1.$2.$3/$4-$5');
};

export const maskPhone = (phone: string): string => {
  const cleaned = phone.replace(/\D/g, '');

  if (cleaned.length === 11) {
    return cleaned.replace(/(\d{2})(\d{5})(\d{4})/, '($1) $2-$3');
  }

  if (cleaned.length === 10) {
    return cleaned.replace(/(\d{2})(\d{4})(\d{4})/, '($1) $2-$3');
  }

  return phone;
};

export const maskPixKey = (pixKey: string, type?: string): string => {
  if (!pixKey) return '';

  if (type === 'CPF' && pixKey.length === 11) {
    return maskCPF(pixKey);
  }

  if (type === 'CNPJ' && pixKey.length === 14) {
    return maskCNPJ(pixKey);
  }

  if (type === 'PHONE') {
    return maskPhone(pixKey);
  }

  // For email and EVP, mask middle part
  if (pixKey.includes('@')) {
    const [local, domain] = pixKey.split('@');
    const maskedLocal = local.length > 2
      ? `${local.charAt(0)}***${local.slice(-1)}`
      : local;
    return `${maskedLocal}@${domain}`;
  }

  // For other keys, mask middle part
  if (pixKey.length > 4) {
    return `${pixKey.substring(0, 2)}***${pixKey.slice(-2)}`;
  }

  return pixKey;
};

// Payment utilities
export const getPaymentStatusColor = (status: PaymentStatus): string => {
  const colors = {
    PENDING: '#faad14',
    ACTIVE: '#1890ff',
    COMPLETED: '#52c41a',
    FAILED: '#ff4d4f',
    CANCELLED: '#d9d9d9',
    EXPIRED: '#8c8c8c',
  };

  return colors[status] || '#d9d9d9';
};

export const getPaymentStatusText = (status: PaymentStatus): string => {
  const labels = {
    PENDING: 'Pendente',
    ACTIVE: 'Ativo',
    COMPLETED: 'Concluído',
    FAILED: 'Falhou',
    CANCELLED: 'Cancelado',
    EXPIRED: 'Expirado',
  };

  return labels[status] || status;
};

// File utilities
export const downloadFile = (data: Blob | string, filename: string, type?: string): void => {
  let blob: Blob;

  if (typeof data === 'string') {
    blob = new Blob([data], { type: type || 'text/plain' });
  } else {
    blob = data;
  }

  const url = URL.createObjectURL(blob);
  const link = document.createElement('a');

  link.href = url;
  link.download = filename;
  document.body.appendChild(link);
  link.click();

  document.body.removeChild(link);
  URL.revokeObjectURL(url);
};

export const formatFileSize = (bytes: number): string => {
  if (bytes === 0) return '0 B';

  const k = 1024;
  const sizes = ['B', 'KB', 'MB', 'GB'];
  const i = Math.floor(Math.log(bytes) / Math.log(k));

  return `${parseFloat((bytes / Math.pow(k, i)).toFixed(2))} ${sizes[i]}`;
};

// Error handling
export const getErrorMessage = (error: unknown): string => {
  if (typeof error === 'string') {
    return error;
  }

  if (error && typeof error === 'object') {
    const apiError = error as ApiError;

    if (apiError.message) {
      return apiError.message;
    }

    if ('response' in error) {
      const response = (error as any).response;

      if (response?.data?.message) {
        return response.data.message;
      }

      if (response?.data?.error) {
        return response.data.error;
      }

      if (response?.statusText) {
        return response.statusText;
      }
    }

    if ('message' in error) {
      return (error as Error).message;
    }
  }

  return 'Erro desconhecido';
};

// URL utilities
export const buildQueryParams = (params: Record<string, any>): string => {
  const searchParams = new URLSearchParams();

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      if (Array.isArray(value)) {
        value.forEach(v => searchParams.append(key, String(v)));
      } else {
        searchParams.set(key, String(value));
      }
    }
  });

  const queryString = searchParams.toString();
  return queryString ? `?${queryString}` : '';
};

// Array utilities
export const groupBy = <T>(
  array: T[],
  keyFn: (item: T) => string | number
): Record<string, T[]> => {
  return array.reduce((groups, item) => {
    const key = String(keyFn(item));
    groups[key] = groups[key] || [];
    groups[key].push(item);
    return groups;
  }, {} as Record<string, T[]>);
};

export const sortBy = <T>(
  array: T[],
  keyFn: (item: T) => string | number,
  order: 'asc' | 'desc' = 'asc'
): T[] => {
  return [...array].sort((a, b) => {
    const aValue = keyFn(a);
    const bValue = keyFn(b);

    if (order === 'desc') {
      return aValue < bValue ? 1 : aValue > bValue ? -1 : 0;
    }

    return aValue > bValue ? 1 : aValue < bValue ? -1 : 0;
  });
};

// Debounce utility
export const debounce = <T extends (...args: any[]) => any>(
  func: T,
  wait: number
): ((...args: Parameters<T>) => void) => {
  let timeout: NodeJS.Timeout;

  return (...args: Parameters<T>) => {
    clearTimeout(timeout);
    timeout = setTimeout(() => func.apply(null, args), wait);
  };
};

// Local storage utilities
export const storage = {
  get: <T>(key: string, defaultValue?: T): T | null => {
    try {
      const item = localStorage.getItem(key);
      return item ? JSON.parse(item) : defaultValue || null;
    } catch {
      return defaultValue || null;
    }
  },

  set: (key: string, value: any): void => {
    try {
      localStorage.setItem(key, JSON.stringify(value));
    } catch (error) {
      console.warn('Failed to save to localStorage:', error);
    }
  },

  remove: (key: string): void => {
    localStorage.removeItem(key);
  },

  clear: (): void => {
    localStorage.clear();
  },
};
// ============================================
// HELPERS ADICIONAIS PARA NOVOS MÓDULOS
// ============================================

// Formatar CNPJ
export const formatCNPJ = (cnpj: string): string => {
  if (!cnpj) return '';
  const cleaned = cnpj.replace(/\D/g, '');
  if (cleaned.length !== 14) return cnpj;
  return cleaned.replace(
    /^(\d{2})(\d{3})(\d{3})(\d{4})(\d{2})$/,
    '$1.$2.$3/$4-$5'
  );
};

// Formatar DateTime
export const formatDateTime = (date: string | Date): string => {
  if (!date) return '';
  const d = typeof date === 'string' ? new Date(date) : date;
  return d.toLocaleString('pt-BR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });
};
