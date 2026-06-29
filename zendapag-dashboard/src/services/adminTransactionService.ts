// @ts-nocheck
/**
 * Transações (payments) para o Painel Admin Master.
 * Endpoint real: GET /payments/all (ADMIN) — listagem paginada de todos os
 * pagamentos. Mapeia PaymentResponse -> forma da tela.
 *
 * Observações de impedância: o PaymentResponse não traz o adquirente e os
 * pagamentos aqui são Pix (vêm como "—"/Pix). O nome do estabelecimento
 * (merchantName) já é retornado pelo backend.
 */
import { adminHttp, unwrapPageContent } from './adminHttp';

// PaymentStatus (backend) -> status da tela
const statusMap: Record<string, string> = {
  APPROVED: 'aprovada',
  PENDING: 'pendente',
  PROCESSING: 'pendente',
  REJECTED: 'recusada',
  FAILED: 'recusada',
  CANCELLED: 'estornada',
  REFUNDED: 'estornada',
  PARTIALLY_REFUNDED: 'estornada',
  CHARGEBACK: 'disputa',
  EXPIRED: 'recusada',
};

const num = (v: any) => (v == null ? 0 : Number(v));

const isCard = (p: any) =>
  p.paymentMethodType === 'CREDIT_CARD' || p.paymentMethodType === 'DEBIT_CARD' || !!p.cardLast4;

const mapPayment = (p: any) => {
  const bruto = num(p.grossAmount ?? p.amount);
  const taxa = num(p.feeAmount);
  const liquido = p.netAmount != null ? num(p.netAmount) : bruto - taxa;
  const card = isCard(p);
  return {
    id: p.referenceId || p.id,
    uuid: p.id, // id real (UUID) para ações como aprovar
    tipo: 'geral',
    cliente: p.customerName || '—',
    documento: p.customerDocument || '—',
    estabelecimento: p.merchantName || '—',
    metodo: card ? 'cartao' : 'pix',
    status: statusMap[p.status] || 'pendente',
    statusRaw: p.status,
    bruto,
    taxa,
    liquido: p.status === 'REFUNDED' ? -bruto : liquido,
    adquirente: '—',
    // Dados de cartão (PCI-compliant: só máscara/bandeira/validade/parcelas — nunca PAN/CVV)
    cartao: card
      ? {
          bandeira: p.cardBrand || '—',
          mascara: p.cardMaskedNumber || (p.cardLast4 ? `•••• ${p.cardLast4}` : '—'),
          last4: p.cardLast4 || null,
          validade: p.cardExpiry || '—',
          parcelas: p.installments || 1,
          tresDs: p.threeDsStatus || null,
        }
      : null,
    motivoErro: (p.status === 'REJECTED' || p.status === 'FAILED') ? 'Pagamento recusado/falhou' : null,
    criadoEm: p.createdAt ? String(p.createdAt).replace('T', ' ').slice(0, 16) : '—',
  };
};

export const adminTransactionService = {
  async listAll(): Promise<any[]> {
    const res = await adminHttp.get('/payments/all', { params: { page: 0, size: 200 } });
    return unwrapPageContent(res).map(mapPayment);
  },

  /** Aprova um pagamento PENDING (sandbox: simula a confirmação do PSP). */
  async approve(uuid: string): Promise<any> {
    const res = await adminHttp.post(`/payments/${uuid}/approve`);
    return res?.data?.data ?? res?.data;
  },

  /** Recusa um pagamento PENDING (dispara PAYMENT_FAILED). */
  async reject(uuid: string, reason = 'Recusado pelo Admin Master'): Promise<any> {
    const res = await adminHttp.post(`/payments/${uuid}/reject`, null, { params: { reason } });
    return res?.data?.data ?? res?.data;
  },

  /** Estorna um pagamento APROVADO (reverte o saldo, dispara PAYMENT_REFUNDED). */
  async refund(uuid: string, reason = 'Estorno pelo Admin Master'): Promise<any> {
    const res = await adminHttp.post(`/payments/${uuid}/reverse`, null, { params: { reason } });
    return res?.data?.data ?? res?.data;
  },
};

export default adminTransactionService;
