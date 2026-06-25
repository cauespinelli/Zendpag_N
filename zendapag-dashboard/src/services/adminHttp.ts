// @ts-nocheck
/**
 * Cliente axios DEDICADO ao Painel Admin Master.
 * Não usa a instância global de propósito: a global redireciona pra /login em
 * qualquer 401, o que sequestraria o painel. Aqui os erros sobem pra tela.
 */
import axios from 'axios';
import { STORAGE_KEYS } from '@/utils/constants';
import { storage } from '@/utils/helpers';

export const adminHttp = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
});

adminHttp.interceptors.request.use((config) => {
  const token = storage.get<string>(STORAGE_KEYS.AUTH_TOKEN);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

/** Desembrulha ApiResponse<Page<T>> -> T[] (content da página). */
export const unwrapPageContent = (res: any): any[] => {
  const envelope = res?.data;            // ApiResponse
  const page = envelope?.data ?? envelope; // Page<T>
  return page?.content ?? [];
};

/** Traduz um erro axios numa mensagem amigável pra tela. */
export const adminHttpError = (e: any): string => {
  const sc = e?.response?.status;
  const backendMsg = e?.response?.data?.message;
  if (sc === 401) return 'Sessão não autorizada (401). É necessário login com perfil ADMIN.';
  if (sc === 403) return 'Acesso negado (403). O usuário atual não tem perfil ADMIN.';
  if (!e?.response) return 'Não foi possível alcançar a API. Verifique se o backend está no ar (porta 8093).';
  // Mensagem de negócio vinda do backend (ex.: "Pagamento retido por risco alto...")
  const generic = ['An unexpected error occurred', 'Internal server error'];
  if (backendMsg && !generic.includes(backendMsg)) return backendMsg;
  return `Erro (HTTP ${sc}).`;
};
