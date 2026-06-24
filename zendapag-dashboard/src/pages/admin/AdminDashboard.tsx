// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Dashboard (Operação).
 * KPIs, rosca de conversão geral, conversão por adquirente,
 * faturamento por método e ranking de clientes. Dados mock.
 */
import React from 'react';
import {
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  Legend,
} from 'recharts';
import { QrCode, CreditCard, Barcode, FileWarning, TrendingUp } from 'lucide-react';
import {
  brl,
  pct,
  dashboardKpis,
  conversaoGeral,
  conversaoPorAdquirente,
  faturamentoPorMetodo,
  rankingClientes,
} from '@/mock/admin';
import { StatCard, AdminCard, SectionHeader, MiniBar, PageHeader } from '@/components/admin/ui';

const compact = (v: number) =>
  v >= 1000 ? `${(v / 1000).toFixed(0)}k` : `${v}`;

const AdminDashboard: React.FC = () => {
  const k = dashboardKpis;
  const totalConv = conversaoGeral.reduce((s, c) => s + c.valor, 0);
  const aprovadas = conversaoGeral.find((c) => c.nome === 'Aprovadas')?.valor ?? 0;
  const taxaAprovacao = ((aprovadas / totalConv) * 100).toFixed(1);

  return (
    <div>
      <PageHeader
        title="Dashboard"
        subtitle="Visão geral da operação — atualizado há 2 min"
      />

      {/* KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-5 gap-4 mb-6">
        <StatCard label="Pix processado" value={brl(k.pixProcessado)} trend={k.pixVariacao} accent="emerald" icon={<QrCode size={20} />} />
        <StatCard label="Cartão processado" value={brl(k.cartaoProcessado)} trend={k.cartaoVariacao} accent="blue" icon={<CreditCard size={20} />} />
        <StatCard label="Boleto processado" value={brl(k.boletoProcessado)} trend={k.boletoVariacao} accent="violet" icon={<Barcode size={20} />} />
        <StatCard label="Documentos pendentes" value={String(k.documentosPendentes)} accent="amber" icon={<FileWarning size={20} />} hint="Aguardando análise KYC/KYB" />
        <StatCard label="Lucro total" value={brl(k.lucroTotal)} trend={k.lucroVariacao} accent="emerald" icon={<TrendingUp size={20} />} />
      </div>

      {/* Rosca + Faturamento por método */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-4 mb-6">
        {/* Rosca de conversão geral */}
        <AdminCard>
          <SectionHeader title="Conversão geral" subtitle="Últimos 30 dias" />
          <div className="px-5 pb-5">
            <div className="relative">
              <ResponsiveContainer width="100%" height={220}>
                <PieChart>
                  <Pie
                    data={conversaoGeral}
                    dataKey="valor"
                    nameKey="nome"
                    innerRadius={62}
                    outerRadius={90}
                    paddingAngle={2}
                    stroke="none"
                  >
                    {conversaoGeral.map((c) => (
                      <Cell key={c.nome} fill={c.cor} />
                    ))}
                  </Pie>
                  <Tooltip formatter={(v: number) => v.toLocaleString('pt-BR')} />
                </PieChart>
              </ResponsiveContainer>
              <div className="absolute inset-0 flex flex-col items-center justify-center pointer-events-none">
                <span className="text-2xl font-bold text-slate-800 tabular-nums">{taxaAprovacao}%</span>
                <span className="text-xs text-slate-400">aprovação</span>
              </div>
            </div>
            <div className="mt-4 space-y-2">
              {conversaoGeral.map((c) => (
                <div key={c.nome} className="flex items-center justify-between text-sm">
                  <span className="flex items-center gap-2 text-slate-600">
                    <span className="w-2.5 h-2.5 rounded-full" style={{ background: c.cor }} />
                    {c.nome}
                  </span>
                  <span className="font-semibold text-slate-800 tabular-nums">{c.valor.toLocaleString('pt-BR')}</span>
                </div>
              ))}
            </div>
          </div>
        </AdminCard>

        {/* Faturamento por método */}
        <AdminCard className="lg:col-span-2">
          <SectionHeader title="Faturamento por método" subtitle="Pix · Cartão · Boleto — últimos 6 meses" />
          <div className="px-3 pb-5">
            <ResponsiveContainer width="100%" height={260}>
              <BarChart data={faturamentoPorMetodo} barGap={4}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
                <XAxis dataKey="mes" tick={{ fontSize: 12, fill: '#94a3b8' }} axisLine={false} tickLine={false} />
                <YAxis tickFormatter={(v) => `R$${compact(v)}`} tick={{ fontSize: 11, fill: '#94a3b8' }} axisLine={false} tickLine={false} width={56} />
                <Tooltip formatter={(v: number) => brl(v)} cursor={{ fill: '#f8fafc' }} />
                <Legend wrapperStyle={{ fontSize: 12 }} />
                <Bar name="Pix" dataKey="pix" fill="#10B981" radius={[4, 4, 0, 0]} maxBarSize={26} />
                <Bar name="Cartão" dataKey="cartao" fill="#2563EB" radius={[4, 4, 0, 0]} maxBarSize={26} />
                <Bar name="Boleto" dataKey="boleto" fill="#8B5CF6" radius={[4, 4, 0, 0]} maxBarSize={26} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </AdminCard>
      </div>

      {/* Conversão por adquirente + Ranking de clientes */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {/* Conversão por adquirente */}
        <AdminCard>
          <SectionHeader title="Conversão por adquirente" subtitle="Volume e taxa de aprovação por parceiro" />
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-slate-400 border-y border-slate-100">
                  <th className="px-5 py-2.5 font-medium">Parceiro</th>
                  <th className="px-5 py-2.5 font-medium text-right">Volume</th>
                  <th className="px-5 py-2.5 font-medium">Aprovação</th>
                  <th className="px-5 py-2.5 font-medium text-right">Falha</th>
                </tr>
              </thead>
              <tbody>
                {conversaoPorAdquirente.map((a) => (
                  <tr key={a.parceiro} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3 font-medium text-slate-700">{a.parceiro}</td>
                    <td className="px-5 py-3 text-right tabular-nums text-slate-600">{brl(a.volume)}</td>
                    <td className="px-5 py-3">
                      <div className="flex items-center gap-2">
                        <MiniBar value={a.aprovacao} tone="emerald" />
                        <span className="tabular-nums text-slate-700 font-medium w-12 text-right">{pct(a.aprovacao)}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3 text-right tabular-nums text-rose-600">{pct(a.falha)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </AdminCard>

        {/* Ranking de clientes */}
        <AdminCard>
          <SectionHeader title="Ranking de clientes" subtitle="Top estabelecimentos por volume" />
          <div className="px-5 pb-5 space-y-3">
            {rankingClientes.map((c, i) => (
              <div key={c.documento} className="flex items-center gap-3">
                <span
                  className={`w-7 h-7 rounded-lg flex items-center justify-center text-xs font-bold shrink-0 ${
                    i < 3 ? 'text-white bg-gradient-to-br from-blue-600 to-emerald-500' : 'text-slate-500 bg-slate-100'
                  }`}
                >
                  {i + 1}
                </span>
                <div className="min-w-0 flex-1">
                  <p className="text-sm font-medium text-slate-700 truncate">{c.nome}</p>
                  <p className="text-xs text-slate-400 tabular-nums">{c.documento} · {c.transacoes.toLocaleString('pt-BR')} transações</p>
                </div>
                <div className="text-right shrink-0">
                  <p className="text-sm font-semibold text-slate-800 tabular-nums">{brl(c.volume)}</p>
                  <p className="text-xs text-emerald-600 tabular-nums">{pct(c.conversao)} aprov.</p>
                </div>
              </div>
            ))}
          </div>
        </AdminCard>
      </div>
    </div>
  );
};

export default AdminDashboard;
