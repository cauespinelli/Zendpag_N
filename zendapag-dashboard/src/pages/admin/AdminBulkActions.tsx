// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Ações em Massa (Gestão).
 * Seleção múltipla de estabelecimentos + barra de ação fixa + modal que
 * se adapta à ação escolhida (bloquear, liberar, compliance, retenção,
 * adquirente, comunicado, exportar) e histórico de lotes. Dados mock.
 */
import React, { useMemo, useState } from 'react';
import {
  Layers,
  Lock,
  Unlock,
  ShieldCheck,
  Percent,
  Repeat,
  Megaphone,
  Download,
  Search,
  X,
  CheckCircle2,
  History,
} from 'lucide-react';
import {
  brl,
  pct,
  estabelecimentos,
  adquirentesDisponiveis,
  lotesExecutados as lotesSeed,
} from '@/mock/admin';
import { AdminCard, StatusBadge, PageHeader } from '@/components/admin/ui';

const statusEstabMeta: Record<string, { tone: any; label: string }> = {
  ativo: { tone: 'success', label: 'Ativo' },
  bloqueado: { tone: 'danger', label: 'Bloqueado' },
  analise: { tone: 'warning', label: 'Em análise' },
  restrito: { tone: 'warning', label: 'Restrito' },
};

const acoes = [
  { key: 'bloquear', label: 'Aplicar bloqueio cautelar', icon: Lock, needs: 'motivo', destrutiva: true },
  { key: 'liberar', label: 'Liberar bloqueio', icon: Unlock, needs: null, destrutiva: false },
  { key: 'compliance', label: 'Aprovar compliance', icon: ShieldCheck, needs: null, destrutiva: false },
  { key: 'retencao', label: 'Ajustar retenção', icon: Percent, needs: 'percent', destrutiva: false },
  { key: 'adquirente', label: 'Trocar adquirente', icon: Repeat, needs: 'adquirente', destrutiva: false },
  { key: 'comunicado', label: 'Enviar comunicado', icon: Megaphone, needs: 'texto', destrutiva: false },
  { key: 'exportar', label: 'Exportar selecionados', icon: Download, needs: null, destrutiva: false },
];

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const loteStatusMeta: Record<string, { tone: any; label: string }> = {
  concluido: { tone: 'success', label: 'Concluído' },
  parcial: { tone: 'warning', label: 'Parcial' },
  erro: { tone: 'danger', label: 'Com erro' },
};

const AdminBulkActions: React.FC = () => {
  const [query, setQuery] = useState('');
  const [statusFiltro, setStatusFiltro] = useState('todos');
  const [selecionados, setSelecionados] = useState<Set<string>>(new Set());
  const [acaoKey, setAcaoKey] = useState('');
  const [lotes, setLotes] = useState(lotesSeed);

  // estado do modal de confirmação
  const [confirmando, setConfirmando] = useState(false);
  const [valor, setValor] = useState('');
  const [feito, setFeito] = useState<string | null>(null);

  const filtered = useMemo(
    () =>
      estabelecimentos.filter((e) => {
        const q = query.toLowerCase();
        if (q && !e.nome.toLowerCase().includes(q) && !e.documento.toLowerCase().includes(q)) return false;
        if (statusFiltro !== 'todos' && e.status !== statusFiltro) return false;
        return true;
      }),
    [query, statusFiltro]
  );

  const allSelected = filtered.length > 0 && filtered.every((e) => selecionados.has(e.id));
  const acao = acoes.find((a) => a.key === acaoKey);

  const toggle = (id: string) => {
    setSelecionados((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const toggleAll = () => {
    setSelecionados((prev) => {
      const next = new Set(prev);
      if (allSelected) filtered.forEach((e) => next.delete(e.id));
      else filtered.forEach((e) => next.add(e.id));
      return next;
    });
  };

  const limpar = () => setSelecionados(new Set());

  const executar = () => {
    const detalhe =
      acao.needs === 'percent' ? `Retenção definida em ${valor || 0}%`
      : acao.needs === 'adquirente' ? `Roteamento migrado para ${valor || '—'}`
      : acao.needs === 'texto' ? valor || 'Comunicado enviado'
      : acao.needs === 'motivo' ? valor || 'Bloqueio cautelar em lote'
      : '—';
    setLotes((prev) => [
      {
        id: `LOTE-${2209 + prev.length}`,
        acao: acao.label,
        detalhe,
        qtd: selecionados.size,
        executadoPor: 'Admin Master',
        executadoEm: '2026-06-23 14:55',
        status: 'concluido',
      },
      ...prev,
    ]);
    setFeito(`${acao.label}: ${selecionados.size} ${selecionados.size === 1 ? 'estabelecimento' : 'estabelecimentos'}.`);
    setConfirmando(false);
    setSelecionados(new Set());
    setAcaoKey('');
    setValor('');
  };

  return (
    <div className="pb-24">
      <PageHeader title="Ações em Massa" subtitle="Selecione estabelecimentos e aplique uma ação em lote" />

      {/* Toast de sucesso */}
      {feito && (
        <div className="mb-4 flex items-center gap-2 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-700">
          <CheckCircle2 size={18} className="text-emerald-500" />
          <span className="flex-1">Lote executado — {feito}</span>
          <button onClick={() => setFeito(null)} className="text-emerald-500 hover:text-emerald-700"><X size={16} /></button>
        </div>
      )}

      <AdminCard>
        {/* Filtros */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar estabelecimento ou documento..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <select value={statusFiltro} onChange={(e) => setStatusFiltro(e.target.value)} className={selectCls}>
            <option value="todos">Todos os status</option>
            <option value="ativo">Ativo</option>
            <option value="analise">Em análise</option>
            <option value="restrito">Restrito</option>
            <option value="bloqueado">Bloqueado</option>
          </select>
          <span className="text-xs text-slate-400">{filtered.length} estabelecimentos</span>
        </div>

        {/* Tabela selecionável */}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                <th className="px-5 py-3 w-10">
                  <input type="checkbox" checked={allSelected} onChange={toggleAll} className="rounded border-slate-300 text-blue-600 focus:ring-blue-200 cursor-pointer" />
                </th>
                <th className="px-5 py-3 font-medium">Estabelecimento</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 font-medium text-right">Volume/mês</th>
                <th className="px-5 py-3 font-medium text-right">MED</th>
                <th className="px-5 py-3 font-medium text-right">Retenção</th>
                <th className="px-5 py-3 font-medium">Adquirentes</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((e) => {
                const checked = selecionados.has(e.id);
                return (
                  <tr
                    key={e.id}
                    onClick={() => toggle(e.id)}
                    className={`border-b border-slate-50 cursor-pointer transition-colors ${checked ? 'bg-blue-50/50' : 'hover:bg-slate-50/60'}`}
                  >
                    <td className="px-5 py-3.5" onClick={(ev) => ev.stopPropagation()}>
                      <input type="checkbox" checked={checked} onChange={() => toggle(e.id)} className="rounded border-slate-300 text-blue-600 focus:ring-blue-200 cursor-pointer" />
                    </td>
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-slate-700">{e.nome}</p>
                      <p className="text-xs text-slate-400">{e.documento} · {e.tipo}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <StatusBadge tone={statusEstabMeta[e.status].tone}>{statusEstabMeta[e.status].label}</StatusBadge>
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{brl(e.volumeMes)}</td>
                    <td className={`px-5 py-3.5 text-right tabular-nums ${e.medPct > 3 ? 'text-rose-600 font-medium' : 'text-slate-500'}`}>{pct(e.medPct)}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-500">{e.retencaoPct ? pct(e.retencaoPct) : '—'}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex flex-wrap gap-1">
                        {e.adquirentes.length ? e.adquirentes.map((a) => (
                          <span key={a} className="text-[11px] px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">{a}</span>
                        )) : <span className="text-xs text-slate-300">—</span>}
                      </div>
                    </td>
                  </tr>
                );
              })}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400 text-sm">Nenhum estabelecimento encontrado.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </AdminCard>

      {/* Histórico de lotes */}
      <AdminCard className="mt-6">
        <div className="flex items-center gap-2 px-5 pt-5 pb-3">
          <History size={18} className="text-slate-400" />
          <h3 className="text-[15px] font-semibold text-slate-800">Histórico de lotes</h3>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                <th className="px-5 py-3 font-medium">Lote</th>
                <th className="px-5 py-3 font-medium">Ação</th>
                <th className="px-5 py-3 font-medium text-right">Alvos</th>
                <th className="px-5 py-3 font-medium">Executado por</th>
                <th className="px-5 py-3 font-medium">Status</th>
              </tr>
            </thead>
            <tbody>
              {lotes.map((l) => (
                <tr key={l.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                  <td className="px-5 py-3.5">
                    <p className="font-medium text-slate-700">{l.id}</p>
                    <p className="text-[11px] text-slate-300 tabular-nums">{l.executadoEm}</p>
                  </td>
                  <td className="px-5 py-3.5">
                    <p className="text-slate-700">{l.acao}</p>
                    <p className="text-xs text-slate-400">{l.detalhe}</p>
                  </td>
                  <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{l.qtd.toLocaleString('pt-BR')}</td>
                  <td className="px-5 py-3.5 text-slate-600">{l.executadoPor}</td>
                  <td className="px-5 py-3.5">
                    <StatusBadge tone={loteStatusMeta[l.status].tone}>{loteStatusMeta[l.status].label}</StatusBadge>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </AdminCard>

      {/* Barra de ação fixa */}
      {selecionados.size > 0 && (
        <div className="fixed bottom-0 left-0 right-0 z-40 pl-64 transition-all">
          <div className="m-4 ml-4 rounded-2xl bg-white border border-slate-200 shadow-2xl px-5 py-3 flex flex-wrap items-center gap-3">
            <div className="flex items-center gap-2">
              <span className="w-7 h-7 rounded-lg bg-gradient-to-br from-blue-600 to-emerald-500 text-white text-xs font-bold flex items-center justify-center">
                {selecionados.size}
              </span>
              <span className="text-sm text-slate-600">selecionados</span>
              <button onClick={limpar} className="text-xs text-slate-400 hover:text-slate-600 underline ml-1">limpar</button>
            </div>
            <div className="flex-1 min-w-[180px]">
              <select value={acaoKey} onChange={(e) => setAcaoKey(e.target.value)} className={`${selectCls} w-full`}>
                <option value="">Escolha uma ação...</option>
                {acoes.map((a) => (
                  <option key={a.key} value={a.key}>{a.label}</option>
                ))}
              </select>
            </div>
            <button
              onClick={() => { setConfirmando(true); setValor(''); }}
              disabled={!acaoKey}
              className="inline-flex items-center gap-2 text-sm font-semibold text-white px-4 py-2.5 rounded-xl bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-700 hover:to-emerald-600 shadow-sm disabled:opacity-40 disabled:cursor-not-allowed"
            >
              <Layers size={16} /> Executar
            </button>
          </div>
        </div>
      )}

      {/* Modal de confirmação (adapta ao tipo de ação) */}
      {confirmando && acao && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <acao.icon size={18} className={acao.destrutiva ? 'text-rose-500' : 'text-blue-500'} /> {acao.label}
              </h3>
              <button onClick={() => setConfirmando(false)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>

            <div className="p-6 space-y-4">
              <p className="text-sm text-slate-600">
                Aplicar <strong className="text-slate-800">{acao.label.toLowerCase()}</strong> a{' '}
                <strong className="text-slate-800">{selecionados.size}</strong>{' '}
                {selecionados.size === 1 ? 'estabelecimento' : 'estabelecimentos'} selecionados.
              </p>

              {acao.needs === 'percent' && (
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1.5">Percentual de retenção</label>
                  <div className="relative">
                    <input type="number" min={0} max={100} value={valor} onChange={(e) => setValor(e.target.value)} placeholder="0"
                      className="w-full px-3 py-2 pr-9 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 tabular-nums" />
                    <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">%</span>
                  </div>
                </div>
              )}
              {acao.needs === 'adquirente' && (
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1.5">Novo adquirente</label>
                  <select value={valor} onChange={(e) => setValor(e.target.value)} className={`${selectCls} w-full`}>
                    <option value="">Selecione...</option>
                    {adquirentesDisponiveis.map((a) => <option key={a} value={a}>{a}</option>)}
                  </select>
                </div>
              )}
              {(acao.needs === 'texto' || acao.needs === 'motivo') && (
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1.5">
                    {acao.needs === 'texto' ? 'Mensagem do comunicado' : 'Motivo do bloqueio'}
                  </label>
                  <textarea value={valor} onChange={(e) => setValor(e.target.value)} rows={3}
                    placeholder={acao.needs === 'texto' ? 'Texto enviado a todos os selecionados...' : 'Justificativa da retenção cautelar...'}
                    className="w-full px-3 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 resize-none" />
                </div>
              )}

              {acao.destrutiva && (
                <div className="rounded-xl bg-rose-50 border border-rose-100 p-3 text-xs text-rose-700">
                  Ação sensível: retém saldo e suspende saques de todos os selecionados. Revise antes de confirmar.
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => setConfirmando(false)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">
                Cancelar
              </button>
              <button
                onClick={executar}
                className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl ${acao.destrutiva ? 'bg-rose-600 hover:bg-rose-700' : 'bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-700 hover:to-emerald-600'}`}
              >
                <acao.icon size={16} /> Confirmar e executar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminBulkActions;
