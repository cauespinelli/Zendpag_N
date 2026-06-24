// @ts-nocheck
/**
 * Índice de MED (Mecanismo Especial de Devolução) por estabelecimento.
 *
 * HOJE: mock — deriva dos dados de exemplo.
 * DEPOIS: o MED de cada estabelecimento deve vir da API de disputa dele.
 *   Basta trocar o corpo de getMedIndices pela versão "REAL (futuro)" abaixo,
 *   que consome o disputeService.getSummary({ establishmentId }). O restante
 *   do painel (tabela de Estabelecimentos) não muda — só consome este service.
 */
import { estabelecimentos } from '@/mock/admin';
// import { disputeService } from './disputeService';

export interface MedIndice {
  medPct: number;
  disputaPct: number;
}

export const medService = {
  /**
   * Retorna o índice de MED/disputa por estabelecimento (mapa id -> índice).
   */
  async getMedIndices(ids: string[]): Promise<Record<string, MedIndice>> {
    // ───────────────────────── MOCK (atual) ─────────────────────────
    const todos = estabelecimentos.reduce((acc, e) => {
      acc[e.id] = { medPct: e.medPct, disputaPct: e.disputaPct };
      return acc;
    }, {} as Record<string, MedIndice>);

    return ids.reduce((acc, id) => {
      if (todos[id]) acc[id] = todos[id];
      return acc;
    }, {} as Record<string, MedIndice>);

    // ──────────────────────── REAL (futuro) ─────────────────────────
    // const entries = await Promise.all(
    //   ids.map(async (id) => {
    //     const resumo = await disputeService.getSummary({ establishmentId: id });
    //     return [id, { medPct: resumo.medPct, disputaPct: resumo.disputaPct }] as const;
    //   })
    // );
    // return Object.fromEntries(entries);
  },
};

export default medService;
