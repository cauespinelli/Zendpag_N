// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Extrato (Gestão).
 * Razão consolidado do gateway: KPIs, gráfico entradas x saídas, resumo
 * por categoria e lançamentos com saldo corrido. Estilo extrato bancário
 * (números tabulares). Dados mock.
 */
import React, { useMemo, useState } from 'react';
import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import {
  Landmark,
  ArrowDownLeft,
  ArrowUpRight,
  TrendingUp,
  Search,
  Download,
  Percent,
  Banknote,
  Users,
  UserCog,
  RotateCcw,
  ShieldAlert,
  Building2,
} from 'lucide-react';
import {
  brl,
  extratoKpis as kpis,
  extratoSerie,
  extratoResumo,
  extratoLancamentos as lancamentos,
} from '@/mock/admin';
import { StatCard, AdminCard, SectionHeader, PageHeader } from '@/components/admin/ui';

const compact = (v: number) => (v >= 1000 ? `${(v / 1000).toFixed(0)}k` : `${v}`);

const categoriaMeta: Record<string, { label: string; icon: any; cls: string }> = {
  taxa: { label: 'Taxas (MDR)', icon: Percent, cls: 'text-emerald-600 bg-emerald-50' },
  comissao_plataforma: { label: 'Tarifa de plataforma', icon: Landmark, cls: 'text-emerald-600 bg-emerald-50' },
  saque: { label: 'Saques', icon: Banknote, cls: 'text-blue-600 bg-blue-50' },
  comissao_afiliado: { label: 'Comissão de afiliado', icon: Users, cls: 'text-violet-600 bg-violet-50' },
  comissao_gerente: { label: 'Comissão de gerente', icon: UserCog, cls: 'text-violet-600 bg-violet-50' },
  estorno: { label: 'Estornos', icon: RotateCcw, cls: 'text-amber-600 bg-amber-50' },
  med: { label: 'Devolução de MED', icon: ShieldAlert, cls: 'text-rose-600 bg-rose-50' },
};

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const AdminStatement: React.FC = () => {
  const [query, setQuery] = useState('');
  const [tipo, setTipo] = useState('todos');
  const [categoria, setCategoria] = useState('todas');

  const filtered = useMemo(
    () =>
      lancamentos.filter((l) => {
        const q = query.toLowerCase();
        if (q && !l.id.toLowerCase().includes(q) && !l.descricao.toLowerCase().includes(q) && !l.contraparte.toLowerCase().includes(q))
          return false;
        if (tipo !== 'todos' && l.tipo !== tipo) return false;
        if (categoria !== 'todas' && l.categoria !== categoria) return false;
        return true;
      }),
    [query, tipo, categoria]
  );

  const maxResumo = Math.max(...extratoResumo.map((r) => r.valor));

  return (
    <div>
      <PageHeader
        title="Extrato"
        subtitle="Razão financeiro consolidado do gateway"
        action={
          <button className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 transition-colors">
            <Download size={16} /> Baixar extrato
          </button>
        }
      />

      {/* KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Saldo consolidado" value={brl(kpis.saldoConsolidado)} accent="blue" icon={<Landmark size={20} />} />
        <StatCard label="Entradas (mês)" value={brl(kpis.entradasMes)} accent="emerald" icon={<ArrowDownLeft size={20} />} />
        <StatCard label="Saídas (mês)" value={brl(kpis.saidasMes)} accent="rose" icon={<ArrowUpRight size={20} />} />
        <StatCard label="Resultado líquido" value={brl(kpis.resultadoLiquido)} accent="emerald" icon={<TrendingUp size={20} />} hint="Entradas − saídas no mês" />
      </div>

      {/* Gráfico + resumo por categoria */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        <AdminCard className="lg:col-span-2">
          <SectionHeader title="Entradas × saídas" subtitle="Movimentação diária — últimos 7 dias" />
          <div className="px-3 pb-5">
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={extratoSerie} barGap={4}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
                <XAxis dataKey="dia" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                <YAxis tickFormatter={(v) => `R$${compact(v)}`} tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} width={56} />
                <Tooltip formatter={(v: number) => brl(v)} cursor={{ fill: '#f8fafc' }} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                <Bar name="Entradas" dataKey="entradas" fill="#10B981" radius={[4, 4, 0, 0]} maxBarSize={22} />
                <Bar name="Saídas" dataKey="saidas" fill="#F43F5E" radius={[4, 4, 0, 0]} maxBarSize={22} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </AdminCard>

        <AdminCard>
          <SectionHeader title="Por categoria" subtitle="Composição do mês" />
          <div className="px-5 pb-5 space-y-3">
            {extratoResumo.map((r) => {
              const meta = categoriaMeta[r.categoria];
              const Icon = meta.icon;
              return (
                <div key={r.categoria}>
                  <div className="flex items-center justify-between text-sm mb-1">
                    <span className="flex items-center gap-2 text-slate-600">
                      <span className={`w-6 h-6 rounded-md flex items-center justify-center ${meta.cls}`}><Icon size={13} /></span>
                      {meta.label}
                    </span>
                    <span className={`font-semibold tabular-nums ${r.tipo === 'credito' ? 'text-emerald-600' : 'text-slate-700'}`}>
                      {r.tipo === 'credito' ? '+' : '−'} {brl(r.valor)}
                    </span>
                  </div>
                  <div className="h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
                    <div className={`h-full rounded-full ${r.tipo === 'credito' ? 'bg-emerald-500' : 'bg-slate-400'}`} style={{ width: `${(r.valor / maxResumo) * 100}%` }} />
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
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar por ID, descrição ou contraparte..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <select value={tipo} onChange={(e) => setTipo(e.target.value)} className={selectCls}>
            <option value="todos">Crédito e débito</option>
            <option value="credito">Apenas créditos</option>
            <option value="debito">Apenas débitos</option>
          </select>
          <select value={categoria} onChange={(e) => setCategoria(e.target.value)} className={selectCls}>
            <option value="todas">Todas as categorias</option>
            {Object.entries(categoriaMeta).map(([k, m]) => (
              <option key={k} value={k}>{m.label}</option>
            ))}
          </select>
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                <th className="px-5 py-3 font-medium">Lançamento</th>
                <th className="px-5 py-3 font-medium">Categoria</th>
                <th className="px-5 py-3 font-medium">Contraparte</th>
                <th className="px-5 py-3 font-medium text-right">Valor</th>
                <th className="px-5 py-3 font-medium text-right">Saldo após</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((l) => {
                const meta = categoriaMeta[l.categoria];
                const Icon = meta.icon;
                const credito = l.tipo === 'credito';
                return (
                  <tr key={l.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-slate-700">{l.descricao}</p>
                      <p className="text-[11px] text-slate-300 tabular-nums">{l.id} · {l.data}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-2 py-1 rounded-md ${meta.cls}`}>
                        <Icon size={13} /> {meta.label}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-slate-600">
                      <span className="inline-flex items-center gap-1.5">
                        <Building2 size={14} className="text-slate-400" /> {l.contraparte}
                      </span>
                    </td>
                    <td className={`px-5 py-3.5 text-right tabular-nums font-semibold ${credito ? 'text-emerald-600' : 'text-rose-600'}`}>
                      {credito ? '+' : '−'} {brl(l.valor)}
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{brl(l.saldoApos)}</td>
                  </tr>
                );
              })}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={5} className="px-5 py-12 text-center text-slate-400 text-sm">Nenhum lançamento encontrado com os filtros atuais.</td>
                </tr>
              )}
            </tbody>
            {filtered.length > 0 && (
              <tfoot>
                <tr className="border-t border-slate-200">
                  <td colSpan={3} className="px-5 py-3 text-xs text-slate-400">{filtered.length} lançamentos exibidos</td>
                  <td className="px-5 py-3 text-right text-xs text-slate-400">Saldo atual</td>
                  <td className="px-5 py-3 text-right tabular-nums font-bold text-slate-800">{brl(kpis.saldoConsolidado)}</td>
                </tr>
              </tfoot>
            )}
          </table>
        </div>
      </AdminCard>
    </div>
  );
};

export default AdminStatement;
