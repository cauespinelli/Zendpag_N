// @ts-nocheck
/**
 * Regras de liquidação / saque automático + saldos por método (Painel Admin).
 * Endpoints reais (ADMIN):
 *   GET/PUT    /admin/payout-rules                       — regras globais
 *   GET/PUT    /admin/payout-rules/merchant/{id}         — efetivas + override
 *   DELETE     /admin/payout-rules/merchant/{id}/{method}— remove override
 *   GET        /admin/merchants/{id}/balances           — saldos por método
 */
import { adminHttp } from './adminHttp';

const unwrap = (res: any) => res?.data?.data ?? res?.data;

// rótulo amigável por método
export const methodLabel: Record<string, string> = {
  PIX: 'Pix',
  CREDIT_CARD: 'Cartão de crédito',
  DEBIT_CARD: 'Cartão de débito',
  BANK_SLIP: 'Boleto',
  BANK_TRANSFER: 'Transferência',
  WALLET: 'Carteira',
};

export const adminPayoutService = {
  async getGlobalRules(): Promise<any[]> {
    const res = await adminHttp.get('/admin/payout-rules');
    return unwrap(res) || [];
  },

  async saveGlobalRule(rule: any): Promise<any> {
    const res = await adminHttp.put('/admin/payout-rules', rule);
    return unwrap(res);
  },

  async getMerchantRules(merchantId: string): Promise<any[]> {
    const res = await adminHttp.get(`/admin/payout-rules/merchant/${merchantId}`);
    return unwrap(res) || [];
  },

  async saveMerchantRule(merchantId: string, rule: any): Promise<any> {
    const res = await adminHttp.put(`/admin/payout-rules/merchant/${merchantId}`, rule);
    return unwrap(res);
  },

  async deleteMerchantOverride(merchantId: string, method: string): Promise<any> {
    const res = await adminHttp.delete(`/admin/payout-rules/merchant/${merchantId}/${method}`);
    return unwrap(res);
  },

  async getBalances(merchantId: string): Promise<any> {
    const res = await adminHttp.get(`/admin/merchants/${merchantId}/balances`);
    return unwrap(res);
  },
};

export default adminPayoutService;
