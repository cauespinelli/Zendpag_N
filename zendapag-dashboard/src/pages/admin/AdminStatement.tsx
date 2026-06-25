// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Extrato (Gestão) — CONECTADO AO BACKEND.
 * Razão real de transações (GET /transactions/all). KPIs e gráfico calculados
 * dos lançamentos. Estados loading / vazio / erro.
 */
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ResponsiveContainer, BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend,
} from 'recharts';
import {
  Landmark, Banknote, Percent, RotateCcw, ArrowUpRight, Building2, TrendingUp, Receipt,
  Search, RefreshCw, Loader, Inbox, WifiOff,
} from 'lucide-react';
import { adminStatementService } from '@/services/adminStatementService';
import { adminHttpError } from '@/services/adminHttp';
import { StatCard, AdminCard, SectionHeader, PageHeader } from '@/components/admin/ui';

const brl = (v: number): string =>
  (Number(v) || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });
const compact = (v: number) => (v >= 1000 ? `${(v / 1000).toFixed(0)}k` : `${v}`);

const categoriaMeta: Record<string, { icon: any; cls: string }> = {
  PAYMENT: { icon: Banknote, cls: 'text-emerald-600 bg-emerald-50' },
  FEE: { icon: Percent, cls: 'text-emerald-600 bg-emerald-50' },
  REFUND: { icon: RotateCcw, cls: 'text-amber-600 bg-amber-50' },
  SETTLEMENT: { icon: Landmark, cls: 'text-blue-600 bg-blue-50' },
  WITHDRAWAL: { icon: ArrowUpRight, cls: 'text-blue-600 bg-blue-50' },
};
const metaFor = (tipo: string) => categoriaMeta[tipo] || { icon: Building2, cls: 'text-slate-600 bg-slate-100' };

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const AdminStatement: React.FC = () => {
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);
  const [query, setQuery] = useState('');
  const [categoria, setCategoria] = useState('todas');

  const carregar = useCallback(async () => {
    setLoading(true); setErro(null);
    try { setRows(await adminStatementService.listAll()); }
    catch (e: any) { setErro(adminHttpError(e)); setRows([]); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { carregar(); }, [carregar]);

  const sumBy = (pred: (r: any) => boolean) => rows.filter(pred).reduce((a, r) => a + (r.valor || 0), 0);

  const kpis = useMemo(() => ({
    volume: sumBy((r) => r.tipoRaw === 'PAYMENT'),
    receita: sumBy((r) => r.tipoRaw === 'FEE'),
    estornos: sumBy((r) => r.tipoRaw === 'REFUND'),
    total: rows.length,
  }), [rows]);

  // série diária: entradas = taxas (receita); saídas = estornos + saques + repasses
  const serie = useMemo(() => {
    const byDay: Record<string, { dia: string; entradas: number; saidas: number }> = {};
    for (const r of rows) {
      const d = (byDay[r.dia] ||= { dia: r.dia, entradas: 0, saidas: 0 });
      if (r.tipoRaw === 'FEE') d.entradas += r.valor;
      else if (['REFUND', 'WITHDRAWAL', 'SETTLEMENT', 'CHARGEBACK'].includes(r.tipoRaw)) d.saidas += r.valor;
    }
    return Object.values(byDay).sort((a, b) => a.dia.localeCompare(b.dia)).map((d) => ({ ...d, dia: d.dia.slice(5) }));
  }, [rows]);

  const resumo = useMemo(() => {
    const by: Record<string, { tipo: string; total: number; count: number }> = {};
    for (const r of rows) {
      const e = (by[r.tipoRaw] ||= { tipo: r.tipoRaw, total: 0, count: 0 });
      e.total += r.valor; e.count++;
    }
    return Object.values(by).sort((a, b) => b.total - a.total);
  }, [rows]);
  const maxResumo = Math.max(1, ...resumo.map((r) => r.total));

  const categorias = useMemo(() => Array.from(new Set(rows.map((r) => r.tipoRaw))), [rows]);

  const filtered = useMemo(() => rows.filter((r) => {
    const q = query.toLowerCase();
    if (q && !`${r.id} ${r.descricao} ${r.contraparte}`.toLowerCase().includes(q)) return false;
    if (categoria !== 'todas' && r.tipoRaw !== categoria) return false;
    return true;
  }), [rows, query, categoria]);

  return (
    <div>
      <PageHeader
        title="Extrato"
        subtitle="Razão de transações da plataforma"
        action={
          <button onClick={carregar} disabled={loading} className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 transition-colors disabled:opacity-50">
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Atualizar
          </button>
        }
      />

      {/* KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Volume processado" value={brl(kpis.volume)} accent="blue" icon={<Banknote size={20} />} />
        <StatCard label="Receita (taxas)" value={brl(kpis.receita)} accent="emerald" icon={<Percent size={20} />} />
        <StatCard label="Estornos" value={brl(kpis.estornos)} accent="amber" icon={<RotateCcw size={20} />} />
        <StatCard label="Lançamentos" value={kpis.total.toLocaleString('pt-BR')} accent="violet" icon={<Receipt size={20} />} />
      </div>

      {/* Gráfico + resumo */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        <AdminCard className="lg:col-span-2">
          <SectionHeader title="Receita × saídas" subtitle="Por dia (taxas vs estornos/saques/repasses)" />
          <div className="px-3 pb-5">
            {serie.length === 0 ? (
              <div className="py-12 text-center text-sm text-slate-400">Sem dados ainda.</div>
            ) : (
              <ResponsiveContainer width="100%" height={260}>
                <BarChart data={serie} barGap={4}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
                  <XAxis dataKey="dia" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                  <YAxis tickFormatter={(v) => `R$${compact(v)}`} tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} width={56} />
                  <Tooltip formatter={(v: number) => brl(v)} cursor={{ fill: '#f8fafc' }} />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar name="Receita" dataKey="entradas" fill="#10B981" radius={[4, 4, 0, 0]} maxBarSize={22} />
                  <Bar name="Saídas" dataKey="saidas" fill="#F43F5E" radius={[4, 4, 0, 0]} maxBarSize={22} />
                </BarChart>
              </ResponsiveContainer>
            )}
          </div>
        </AdminCard>

        <AdminCard>
          <SectionHeader title="Por categoria" subtitle="Total por tipo de lançamento" />
          <div className="px-5 pb-5 space-y-3">
            {resumo.length === 0 ? (
              <p className="text-sm text-slate-400 py-6 text-center">Sem lançamentos.</p>
            ) : resumo.map((r) => {
              const meta = metaFor(r.tipo);
              const Icon = meta.icon;
              return (
                <div key={r.tipo}>
                  <div className="flex items-center justify-between text-sm mb-1">
                    <span className="flex items-center gap-2 text-slate-600">
                      <span className={`w-6 h-6 rounded-md flex items-center justify-center ${meta.cls}`}><Icon size={13} /></span>
                      {r.tipo} <span className="text-xs text-slate-400">({r.count})</span>
                    </span>
                    <span className="font-semibold tabular-nums text-slate-700">{brl(r.total)}</span>
                  </div>
                  <div className="h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
                    <div className="h-full rounded-full bg-slate-400" style={{ width: `${(r.total / maxResumo) * 100}%` }} />
                  </div>
                </div>
              );
            })}
          </div>
        </AdminCard>
      </div>

      {/* Lançamentos */}
      <AdminCard>
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Buscar por ID, descrição ou estabelecimento..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100" />
          </div>
          <select value={categoria} onChange={(e) => setCategoria(e.target.value)} className={selectCls}>
            <option value="todas">Todas as categorias</option>
            {categorias.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>

        {loading ? (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <Loader size={26} className="text-blue-500 animate-spin mb-3" />
            <p className="text-sm text-slate-500">Carregando o razão do backend...</p>
          </div>
        ) : erro ? (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-xl bg-rose-50 flex items-center justify-center text-rose-500 mb-3"><WifiOff size={22} /></div>
            <p className="text-sm font-medium text-slate-700">Não foi possível carregar o extrato</p>
            <p className="text-xs text-slate-400 mt-1 max-w-md">{erro}</p>
            <button onClick={carregar} className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-blue-700 bg-blue-50 hover:bg-blue-100 px-4 py-2 rounded-lg"><RefreshCw size={15} /> Tentar novamente</button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                  <th className="px-5 py-3 font-medium">Lançamento</th>
                  <th className="px-5 py-3 font-medium">Categoria</th>
                  <th className="px-5 py-3 font-medium">Estabelecimento</th>
                  <th className="px-5 py-3 font-medium text-right">Valor</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((r) => {
                  const meta = metaFor(r.tipoRaw);
                  const Icon = meta.icon;
                  const credito = r.fluxo === 'credito';
                  return (
                    <tr key={r.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                      <td className="px-5 py-3.5">
                        <p className="font-medium text-slate-700">{r.descricao}</p>
                        <p className="text-[11px] text-slate-300 tabular-nums">{r.id} · {r.data}</p>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-2 py-1 rounded-md ${meta.cls}`}>
                          <Icon size={13} /> {r.categoria}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 text-slate-600">
                        <span className="inline-flex items-center gap-1.5"><Building2 size={14} className="text-slate-400" /> {r.contraparte}</span>
                      </td>
                      <td className={`px-5 py-3.5 text-right tabular-nums font-semibold ${credito ? 'text-emerald-600' : 'text-rose-600'}`}>
                        {credito ? '+' : '−'} {brl(r.valor)}
                      </td>
                    </tr>
                  );
                })}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={4} className="px-5 py-16 text-center">
                      <div className="flex flex-col items-center justify-center text-slate-400">
                        <Inbox size={26} className="mb-2 text-slate-300" />
                        <p className="text-sm">{rows.length === 0 ? 'Nenhum lançamento ainda — aprove pagamentos para gerar o razão.' : 'Nenhum lançamento com os filtros atuais.'}</p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </AdminCard>
    </div>
  );
};

export default AdminStatement;
