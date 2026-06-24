// @ts-nocheck
/**
 * Service de saques para o Painel Admin Master.
 *
 * Usa uma instância axios DEDICADA (não a global) de propósito: a global
 * redireciona pra /login em qualquer 401, o que sequestraria o painel admin
 * (que ainda não tem login próprio). Aqui os erros sobem pra tela tratar
 * como estado de erro, sem mexer na navegação.
 *
 * Endpoints reais (zendapag-api / PixWithdrawalController):
 *   GET  /withdrawals/status/{STATUS}   (ADMIN)  — listagem paginada por status
 *   POST /withdrawals/{id}/approve      (ADMIN)  — aprova saque pendente
 *   POST /withdrawals/{id}/cancel?reason=…       — recusa/cancela saque
 */
import axios from 'axios';
import { STORAGE_KEYS } from '@/utils/constants';
import { storage } from '@/utils/helpers';

const http = axios.create({
  baseURL: process.env.REACT_APP_API_URL,
  timeout: 20000,
  headers: { 'Content-Type': 'application/json' },
});

// Anexa o token (se houver) sem nenhum tratamento global de erro.
http.interceptors.request.use((config) => {
  const token = storage.get<string>(STORAGE_KEYS.AUTH_TOKEN);
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

// Todos os status do enum WithdrawalStatus (backend).
export const WITHDRAWAL_STATUSES = [
  'PENDING', 'PROCESSING', 'APPROVED', 'COMPLETED',
  'REJECTED', 'CANCELLED', 'FAILED', 'REVERSED',
] as const;

// Desembrulha ApiResponse<Page<T>> -> T[] (content da página).
const unwrapPageContent = (res: any): any[] => {
  const envelope = res?.data;          // ApiResponse
  const page = envelope?.data ?? envelope; // Page<T>
  return page?.content ?? [];
};

export const adminWithdrawalService = {
  /**
   * Busca saques de TODOS os status em paralelo e junta numa lista única,
   * ordenada por data (mais recentes primeiro). Como não há endpoint
   * "listar todos", consolidamos as consultas por status.
   *
   * Se TODAS as consultas falharem (ex.: 401/403/sem backend), propaga o
   * erro pra tela exibir estado de erro; se ao menos uma responder, retorna
   * o que veio (lista pode ser vazia = estado vazio legítimo).
   */
  async listAll(): Promise<any[]> {
    const settled = await Promise.allSettled(
      WITHDRAWAL_STATUSES.map((s) =>
        http.get(`/withdrawals/status/${s}`, { params: { page: 0, size: 100 } })
      )
    );

    const fulfilled = settled.filter((r) => r.status === 'fulfilled');
    if (fulfilled.length === 0) {
      throw (settled[0] as PromiseRejectedResult).reason;
    }

    const items = fulfilled.flatMap((r: any) => unwrapPageContent(r.value));
    return items.sort((a, b) => {
      const da = a.requestedAt || a.createdAt || '';
      const db = b.requestedAt || b.createdAt || '';
      return db.localeCompare(da);
    });
  },

  /** Aprova um saque pendente (PENDING -> PROCESSING). */
  async approve(id: string): Promise<any> {
    const res = await http.post(`/withdrawals/${id}/approve`);
    return res?.data?.data ?? res?.data;
  },

  /** Recusa/cancela um saque, com motivo (enviado como query param). */
  async reject(id: string, reason: string): Promise<any> {
    const res = await http.post(`/withdrawals/${id}/cancel`, null, {
      params: { reason },
    });
    return res?.data?.data ?? res?.data;
  },
};

export default adminWithdrawalService;
