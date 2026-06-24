// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Afiliados (Gestão).
 * Visão da plataforma sobre o programa de afiliados: abas Afiliados /
 * Comissões / Ranking, filtros, e ações de aprovar/bloquear afiliado e
 * pagar/reter comissão. Dados mock, estado local.
 */
import React, { useMemo, useState } from 'react';
import {
  Users,
  HandCoins,
  Clock,
  TrendingUp,
  Search,
  Eye,
  Check,
  Ban,
  ShieldCheck,
  Trophy,
  Store,
} from 'lucide-react';
import {
  brl,
  pct,
  afiliados as afiliadosSeed,
  afiliadosKpis as kpis,
  comissoes as comissoesSeed,
  rankingAfiliados,
} from '@/mock/admin';
import { StatCard, AdminCard, StatusBadge, PageHeader, MiniBar } from '@/components/admin/ui';

const afiliadoStatusMeta: Record<string, { tone: any; label: string }> = {
  ativo: { tone: 'success', label: 'Ativo' },
  pendente: { tone: 'warning', label: 'Pendente' },
  bloqueado: { tone: 'danger', label: 'Bloqueado' },
};

const comissaoStatusMeta: Record<string, { tone: any; label: string }> = {
  paga: { tone: 'success', label: 'Paga' },
  pendente: { tone: 'warning', label: 'Pendente' },
  retida: { tone: 'danger', label: 'Retida' },
};

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const AdminAffiliates: React.FC = () => {
  const [aba, setAba] = useState<'afiliados' | 'comissoes' | 'ranking'>('afiliados');
  const [afiliados, setAfiliados] = useState(afiliadosSeed);
  const [comissoes, setComissoes] = useState(comissoesSeed);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('todos');
  const [statusCom, setStatusCom] = useState('todos');

  const pendentesQtd = useMemo(() => afiliados.filter((a) => a.status === 'pendente').length, [afiliados]);
  const maxComissao = rankingAfiliados[0]?.comissao || 1;

  const afiliadosFiltrados = useMemo(
    () =>
      afiliados.filter((a) => {
        const q = query.toLowerCase();
        if (q && !a.nome.toLowerCase().includes(q) && !a.documento.toLowerCase().includes(q) && !a.email.toLowerCase().includes(q))
          return false;
        if (status !== 'todos' && a.status !== status) return false;
        return true;
      }),
    [afiliados, query, status]
  );

  const comissoesFiltradas = useMemo(
    () => comissoes.filter((c) => (statusCom === 'todos' ? true : c.status === statusCom)),
    [comissoes, statusCom]
  );

  const setAfiliadoStatus = (id: string, novo: string) =>
    setAfiliados((prev) => prev.map((a) => (a.id === id ? { ...a, status: novo } : a)));

  const setComissaoStatus = (id: string, novo: string) =>
    setComissoes((prev) => prev.map((c) => (c.id === id ? { ...c, status: novo } : c)));

  return (
    <div>
      <PageHeader title="Afiliados" subtitle="Programa de afiliados de toda a plataforma" />

      {/* Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Afiliados ativos" value={kpis.ativos.toLocaleString('pt-BR')} accent="blue" icon={<Users size={20} />} hint={`${pendentesQtd} aguardando aprovação`} />
        <StatCard label="Comissões pagas (mês)" value={brl(kpis.comissoesPagasMes)} accent="emerald" icon={<HandCoins size={20} />} />
        <StatCard label="Comissões a pagar" value={brl(kpis.comissoesPendentes)} accent="amber" icon={<Clock size={20} />} />
        <StatCard label="Vendas via afiliados" value={brl(kpis.vendasViaAfiliados)} accent="violet" icon={<TrendingUp size={20} />} hint={`${kpis.vendasQtd.toLocaleString('pt-BR')} vendas`} />
      </div>

      <AdminCard>
        {/* Abas */}
        <div className="flex flex-wrap gap-1 px-4 pt-3 border-b border-slate-100">
          {[
            { key: 'afiliados', label: 'Afiliados', badge: pendentesQtd },
            { key: 'comissoes', label: 'Comissões', badge: 0 },
            { key: 'ranking', label: 'Ranking', badge: 0 },
          ].map((t) => (
            <button
              key={t.key}
              onClick={() => setAba(t.key as any)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors flex items-center gap-2 ${
                aba === t.key ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              {t.label}
              {t.badge > 0 && (
                <span className="text-[11px] font-semibold px-1.5 py-0.5 rounded-full bg-amber-100 text-amber-700">{t.badge}</span>
              )}
            </button>
          ))}
        </div>

        {/* ───────── Aba: Afiliados ───────── */}
        {aba === 'afiliados' && (
          <>
            <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
              <div className="relative flex-1 min-w-[200px]">
                <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                <input
                  value={query}
                  onChange={(e) => setQuery(e.target.value)}
                  placeholder="Buscar por nome, documento ou e-mail..."
                  className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
                />
              </div>
              <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectCls}>
                <option value="todos">Todos os status</option>
                <option value="ativo">Ativo</option>
                <option value="pendente">Pendente</option>
                <option value="bloqueado">Bloqueado</option>
              </select>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                    <th className="px-5 py-3 font-medium">Afiliado</th>
                    <th className="px-5 py-3 font-medium">Promove</th>
                    <th className="px-5 py-3 font-medium text-right">Vendas</th>
                    <th className="px-5 py-3 font-medium text-right">Comissão acum.</th>
                    <th className="px-5 py-3 font-medium text-right">Taxa média</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                    <th className="px-5 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {afiliadosFiltrados.map((a) => (
                    <tr key={a.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                      <td className="px-5 py-3.5">
                        <p className="font-medium text-slate-700">{a.nome}</p>
                        <p className="text-xs text-slate-400">{a.email}</p>
                        <p className="text-[11px] text-slate-300">{a.id} · {a.documento}</p>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-1.5 text-slate-600">
                          <Store size={14} className="text-slate-400" />
                          <span className="text-[13px]">{a.promove.length} {a.promove.length === 1 ? 'loja' : 'lojas'}</span>
                        </div>
                        <p className="text-[11px] text-slate-400 truncate max-w-[180px]">{a.promove.join(', ') || '—'}</p>
                      </td>
                      <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{a.vendas.toLocaleString('pt-BR')}</td>
                      <td className="px-5 py-3.5 text-right tabular-nums font-semibold text-slate-800">{brl(a.comissaoTotal)}</td>
                      <td className="px-5 py-3.5 text-right tabular-nums text-slate-500">{pct(a.taxaMedia)}</td>
                      <td className="px-5 py-3.5">
                        <StatusBadge tone={afiliadoStatusMeta[a.status].tone}>{afiliadoStatusMeta[a.status].label}</StatusBadge>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center justify-end gap-1">
                          {a.status === 'pendente' && (
                            <button onClick={() => setAfiliadoStatus(a.id, 'ativo')} className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-emerald-700 bg-emerald-50 hover:bg-emerald-100" title="Aprovar afiliado">
                              <Check size={14} /> Aprovar
                            </button>
                          )}
                          {a.status === 'ativo' && (
                            <button onClick={() => setAfiliadoStatus(a.id, 'bloqueado')} className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-rose-700 bg-rose-50 hover:bg-rose-100" title="Bloquear afiliado">
                              <Ban size={14} /> Bloquear
                            </button>
                          )}
                          {a.status === 'bloqueado' && (
                            <button onClick={() => setAfiliadoStatus(a.id, 'ativo')} className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-emerald-700 bg-emerald-50 hover:bg-emerald-100" title="Desbloquear afiliado">
                              <ShieldCheck size={14} /> Desbloquear
                            </button>
                          )}
                          <button className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Ver detalhes">
                            <Eye size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {afiliadosFiltrados.length === 0 && (
                    <tr>
                      <td colSpan={7} className="px-5 py-12 text-center text-slate-400 text-sm">Nenhum afiliado encontrado com os filtros atuais.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* ───────── Aba: Comissões ───────── */}
        {aba === 'comissoes' && (
          <>
            <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
              <select value={statusCom} onChange={(e) => setStatusCom(e.target.value)} className={selectCls}>
                <option value="todos">Todos os status</option>
                <option value="pendente">Pendente</option>
                <option value="paga">Paga</option>
                <option value="retida">Retida</option>
              </select>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                    <th className="px-5 py-3 font-medium">Comissão</th>
                    <th className="px-5 py-3 font-medium">Afiliado</th>
                    <th className="px-5 py-3 font-medium">Estabelecimento</th>
                    <th className="px-5 py-3 font-medium text-right">Venda</th>
                    <th className="px-5 py-3 font-medium text-right">%</th>
                    <th className="px-5 py-3 font-medium text-right">Comissão</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                    <th className="px-5 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {comissoesFiltradas.map((c) => (
                    <tr key={c.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                      <td className="px-5 py-3.5">
                        <p className="font-medium text-slate-700">{c.id}</p>
                        <p className="text-[11px] text-slate-300 tabular-nums">{c.data} · {c.txId}</p>
                      </td>
                      <td className="px-5 py-3.5 text-slate-700">{c.afiliado}</td>
                      <td className="px-5 py-3.5 text-slate-600">{c.estabelecimento}</td>
                      <td className="px-5 py-3.5 text-right tabular-nums text-slate-600">{brl(c.valorVenda)}</td>
                      <td className="px-5 py-3.5 text-right tabular-nums text-slate-400">{pct(c.percentual)}</td>
                      <td className="px-5 py-3.5 text-right tabular-nums font-semibold text-slate-800">{brl(c.valorComissao)}</td>
                      <td className="px-5 py-3.5">
                        <StatusBadge tone={comissaoStatusMeta[c.status].tone}>{comissaoStatusMeta[c.status].label}</StatusBadge>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center justify-end gap-1">
                          {c.status === 'pendente' && (
                            <>
                              <button onClick={() => setComissaoStatus(c.id, 'paga')} className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-emerald-700 bg-emerald-50 hover:bg-emerald-100" title="Pagar comissão">
                                <Check size={14} /> Pagar
                              </button>
                              <button onClick={() => setComissaoStatus(c.id, 'retida')} className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-rose-700 bg-rose-50 hover:bg-rose-100" title="Reter comissão">
                                <Ban size={14} /> Reter
                              </button>
                            </>
                          )}
                          {c.status === 'retida' && (
                            <button onClick={() => setComissaoStatus(c.id, 'paga')} className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-emerald-700 bg-emerald-50 hover:bg-emerald-100" title="Liberar e pagar">
                              <ShieldCheck size={14} /> Liberar
                            </button>
                          )}
                          {c.status === 'paga' && (
                            <span className="inline-flex items-center gap-1 text-xs text-slate-400 px-2.5 py-1.5"><Check size={14} /> Quitada</span>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                  {comissoesFiltradas.length === 0 && (
                    <tr>
                      <td colSpan={8} className="px-5 py-12 text-center text-slate-400 text-sm">Nenhuma comissão com esse status.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* ───────── Aba: Ranking ───────── */}
        {aba === 'ranking' && (
          <div className="p-5 space-y-3">
            {rankingAfiliados.map((r, i) => (
              <div key={r.nome} className="flex items-center gap-4 p-3 rounded-xl hover:bg-slate-50 transition-colors">
                <div className={`w-8 h-8 rounded-lg flex items-center justify-center font-bold text-sm shrink-0 ${
                  i === 0 ? 'bg-amber-100 text-amber-700' : i === 1 ? 'bg-slate-200 text-slate-600' : i === 2 ? 'bg-orange-100 text-orange-700' : 'bg-slate-100 text-slate-400'
                }`}>
                  {i < 3 ? <Trophy size={15} /> : i + 1}
                </div>
                <div className="flex-1 min-w-0">
                  <div className="flex items-center justify-between gap-3 mb-1.5">
                    <p className="font-medium text-slate-700 truncate">{r.nome}</p>
                    <p className="font-semibold text-slate-800 tabular-nums shrink-0">{brl(r.comissao)}</p>
                  </div>
                  <MiniBar value={(r.comissao / maxComissao) * 100} tone={i === 0 ? 'emerald' : 'blue'} />
                  <p className="text-[11px] text-slate-400 mt-1 tabular-nums">{r.vendas.toLocaleString('pt-BR')} vendas geradas</p>
                </div>
              </div>
            ))}
          </div>
        )}
      </AdminCard>
    </div>
  );
};

export default AdminAffiliates;
