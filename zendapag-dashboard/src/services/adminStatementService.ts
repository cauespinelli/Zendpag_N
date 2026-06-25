// @ts-nocheck
/**
 * Extrato (razão financeiro) para o Painel Admin Master.
 * Endpoint real: GET /transactions/all (ADMIN) — lançamentos do razão.
 *
 * Mapeia cada Transaction para um lançamento da tela. Sinal (entrada/saída) é
 * do ponto de vista de caixa da plataforma.
 */
import { adminHttp, unwrapPageContent } from './adminHttp';

// tipo do backend -> entrada (crédito) ou saída (débito)
const CREDITO = new Set(['PAYMENT', 'FEE', 'CREDIT', 'ADJUSTMENT']);

// rótulo amigável por tipo
const tipoLabel: Record<string, string> = {
  PAYMENT: 'Pagamento',
  FEE: 'Taxa (MDR)',
  REFUND: 'Estorno',
  CHARGEBACK: 'Chargeback',
  SETTLEMENT: 'Repasse',
  WITHDRAWAL: 'Saque',
  REVERSAL: 'Reversão',
  ADJUSTMENT: 'Ajuste',
  CREDIT: 'Crédito',
  DEBIT: 'Débito',
  TRANSFER: 'Transferência',
};

const num = (v: any) => (v == null ? 0 : Number(v));

const mapTransaction = (t: any) => ({
  id: t.referenceId || t.id,
  tipoRaw: t.type,
  categoria: tipoLabel[t.type] || t.type || '—',
  contraparte: t.merchantName || '—',
  fluxo: CREDITO.has(t.type) ? 'credito' : 'debito', // entrada/saída (caixa)
  valor: num(t.amount),
  descricao: t.description || tipoLabel[t.type] || t.type,
  data: t.createdAt ? String(t.createdAt).replace('T', ' ').slice(0, 16) : '—',
  dia: t.createdAt ? String(t.createdAt).slice(0, 10) : '—',
});

export const adminStatementService = {
  async listAll(): Promise<any[]> {
    const res = await adminHttp.get('/transactions/all', { params: { page: 0, size: 300 } });
    return unwrapPageContent(res).map(mapTransaction);
  },
};

export default adminStatementService;
