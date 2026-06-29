// @ts-nocheck
/**
 * Estabelecimentos (merchants) para o Painel Admin Master.
 * Endpoint real: GET /merchants (ADMIN) — listagem paginada.
 *
 * Mapeia o MerchantResponse do backend para a forma usada pela tela. Campos
 * que o backend ainda NÃO expõe (saldo, MED, adquirentes, volume) vêm como
 * null/0 e a tela mostra "—".
 */
import { adminHttp, unwrapPageContent } from './adminHttp';

// MerchantStatus (backend) -> status da tela
const statusMap: Record<string, string> = {
  ACTIVE: 'ativo',
  PENDING_APPROVAL: 'analise',
  SUSPENDED: 'restrito',
  BLOCKED: 'bloqueado',
  INACTIVE: 'bloqueado',
};

const mapMerchant = (m: any) => {
  const doc = m.document || '';
  const tipo = doc.replace(/\D/g, '').length === 11 ? 'PF' : 'PJ';
  return {
    id: m.id,
    nome: m.tradingName || m.name,
    razaoSocial: m.name,
    documento: doc,
    tipo,
    email: m.email,
    telefone: m.phoneNumber || '—',
    status: statusMap[m.status] || 'analise',
    origem: m.source || 'DIRETO',
    origemExternalId: m.sourceExternalId || null,
    compliance: m.kycVerified ? 'aprovado' : 'pendente',
    // ainda não disponíveis no backend:
    saldoDisponivel: null,
    saldoPendente: null,
    saldoRetido: null,
    volumeMes: null,
    medPct: null,
    disputaPct: null,
    retencaoPct: null,
    adquirentes: [],
    riskScore: m.riskScore,
    criadoEm: m.createdAt ? String(m.createdAt).slice(0, 10) : '—',
  };
};

export const adminMerchantService = {
  async listAll(): Promise<any[]> {
    const res = await adminHttp.get('/merchants', { params: { page: 0, size: 200 } });
    return unwrapPageContent(res).map(mapMerchant);
  },
};

export default adminMerchantService;
