// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Transações (Operação).
 * Cards, abas Geral/Internas, filtros (método/status/adquirente),
 * tabela (bruto/taxa/líquido, adquirente, ações ver/disputa/webhook),
 * e "ver motivo do erro" para recusas. Dados mock.
 */
import React, { useMemo, useState } from 'react';
import {
  ArrowDownUp,
  Receipt,
  TrendingDown,
  Wallet,
  QrCode,
  CreditCard,
  Barcode,
  Eye,
  ShieldAlert,
  Webhook,
  AlertTriangle,
  Search,
  X,
} from 'lucide-react';
import {
  brl,
  pct,
  transacoes,
  transacoesKpis as kpis,
  adquirentesDisponiveis,
} from '@/mock/admin';
import { StatCard, AdminCard, StatusBadge, PageHeader } from '@/components/admin/ui';

const statusMeta: Record<string, { tone: any; label: string }> = {
  aprovada: { tone: 'success', label: 'Aprovada' },
  recusada: { tone: 'danger', label: 'Recusada' },
  pendente: { tone: 'warning', label: 'Pendente' },
  estornada: { tone: 'neutral', label: 'Estornada' },
  disputa: { tone: 'info', label: 'Em disputa' },
};

const metodoMeta: Record<string, { icon: any; label: string }> = {
  pix: { icon: QrCode, label: 'Pix' },
  cartao: { icon: CreditCard, label: 'Cartão' },
  boleto: { icon: Barcode, label: 'Boleto' },
};

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const AdminTransactions: React.FC = () => {
  const [aba, setAba] = useState<'geral' | 'interna'>('geral');
  const [query, setQuery] = useState('');
  const [metodo, setMetodo] = useState('todos');
  const [status, setStatus] = useState('todos');
  const [adquirente, setAdquirente] = useState('todos');
  const [erroDetalhe, setErroDetalhe] = useState<any>(null);

  const filtered = useMemo(
    () =>
      transacoes.filter((t) => {
        if (t.tipo !== aba) return false;
        const q = query.toLowerCase();
        if (q && !t.id.toLowerCase().includes(q) && !t.cliente.toLowerCase().includes(q) && !t.estabelecimento.toLowerCase().includes(q))
          return false;
        if (metodo !== 'todos' && t.metodo !== metodo) return false;
        if (status !== 'todos' && t.status !== status) return false;
        if (adquirente !== 'todos' && t.adquirente !== adquirente) return false;
        return true;
      }),
    [aba, query, metodo, status, adquirente]
  );

  return (
    <div>
      <PageHeader title="Transações" subtitle="Movimentações processadas pela plataforma" />

      {/* Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Volume bruto" value={brl(kpis.volumeBruto)} accent="blue" icon={<ArrowDownUp size={20} />} />
        <StatCard label="Taxas retidas" value={brl(kpis.taxaTotal)} accent="violet" icon={<TrendingDown size={20} />} />
        <StatCard label="Volume líquido" value={brl(kpis.volumeLiquido)} accent="emerald" icon={<Wallet size={20} />} />
        <StatCard label="Transações aprovadas" value={kpis.aprovadas.toLocaleString('pt-BR')} accent="emerald" icon={<Receipt size={20} />} hint={`Ticket médio ${brl(kpis.ticketMedio)}`} />
      </div>

      <AdminCard>
        {/* Abas */}
        <div className="flex gap-1 px-4 pt-3 border-b border-slate-100">
          {[
            { key: 'geral', label: 'Geral' },
            { key: 'interna', label: 'Internas' },
          ].map((t) => (
            <button
              key={t.key}
              onClick={() => setAba(t.key as any)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
                aba === t.key ? 'border-blue-600 text-blue-700' : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              {t.label}
            </button>
          ))}
        </div>

        {/* Filtros */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar por ID, cliente ou estabelecimento..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <select value={metodo} onChange={(e) => setMetodo(e.target.value)} className={selectCls}>
            <option value="todos">Todos os métodos</option>
            <option value="pix">Pix</option>
            <option value="cartao">Cartão</option>
            <option value="boleto">Boleto</option>
          </select>
          <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectCls}>
            <option value="todos">Todos os status</option>
            <option value="aprovada">Aprovada</option>
            <option value="recusada">Recusada</option>
            <option value="pendente">Pendente</option>
            <option value="estornada">Estornada</option>
            <option value="disputa">Em disputa</option>
          </select>
          <select value={adquirente} onChange={(e) => setAdquirente(e.target.value)} className={selectCls}>
            <option value="todos">Todos os adquirentes</option>
            {adquirentesDisponiveis.map((a) => (
              <option key={a} value={a}>{a}</option>
            ))}
            {aba === 'interna' && <option value="Interno">Interno</option>}
          </select>
        </div>

        {/* Tabela */}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                <th className="px-5 py-3 font-medium">Transação</th>
                <th className="px-5 py-3 font-medium">Método</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 font-medium text-right">Bruto</th>
                <th className="px-5 py-3 font-medium text-right">Taxa</th>
                <th className="px-5 py-3 font-medium text-right">Líquido</th>
                <th className="px-5 py-3 font-medium">Adquirente</th>
                <th className="px-5 py-3 font-medium text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((t) => {
                const MetIcon = metodoMeta[t.metodo].icon;
                return (
                  <tr key={t.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-slate-700">{t.id}</p>
                      <p className="text-xs text-slate-400">{t.cliente} · {t.estabelecimento}</p>
                      <p className="text-[11px] text-slate-300 tabular-nums">{t.criadoEm}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="inline-flex items-center gap-1.5 text-slate-600">
                        <MetIcon size={16} className="text-slate-400" />
                        {metodoMeta[t.metodo].label}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-1.5">
                        <StatusBadge tone={statusMeta[t.status].tone}>{statusMeta[t.status].label}</StatusBadge>
                        {t.motivoErro && (
                          <button
                            onClick={() => setErroDetalhe(t)}
                            className="text-rose-500 hover:text-rose-600"
                            title="Ver motivo do erro"
                          >
                            <AlertTriangle size={15} />
                          </button>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{brl(t.bruto)}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-400">{t.taxa ? `- ${brl(t.taxa)}` : '—'}</td>
                    <td className={`px-5 py-3.5 text-right tabular-nums font-semibold ${t.liquido < 0 ? 'text-rose-600' : 'text-slate-800'}`}>
                      {t.liquido ? brl(t.liquido) : '—'}
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="text-xs px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">{t.adquirente}</span>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1">
                        <button className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Ver detalhes">
                          <Eye size={16} />
                        </button>
                        <button className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Abrir disputa">
                          <ShieldAlert size={16} />
                        </button>
                        <button className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Reenviar webhook">
                          <Webhook size={16} />
                        </button>
                      </div>
                    </td>
                  </tr>
                );
              })}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-5 py-12 text-center text-slate-400 text-sm">
                    Nenhuma transação encontrada com os filtros atuais.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </AdminCard>

      {/* Modal: motivo do erro */}
      {erroDetalhe && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <AlertTriangle size={18} className="text-rose-500" /> Motivo da recusa
              </h3>
              <button onClick={() => setErroDetalhe(null)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div className="rounded-xl bg-rose-50 border border-rose-100 p-4">
                <p className="text-sm font-semibold text-rose-700">{erroDetalhe.motivoErro}</p>
                <p className="text-xs text-rose-500 mt-1">Código retornado pelo adquirente {erroDetalhe.adquirente}</p>
              </div>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-xs text-slate-400">Transação</p>
                  <p className="font-medium text-slate-700">{erroDetalhe.id}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Valor</p>
                  <p className="font-medium text-slate-700 tabular-nums">{brl(erroDetalhe.bruto)}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Cliente</p>
                  <p className="font-medium text-slate-700">{erroDetalhe.cliente}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Estabelecimento</p>
                  <p className="font-medium text-slate-700">{erroDetalhe.estabelecimento}</p>
                </div>
              </div>
            </div>
            <div className="flex justify-end px-6 py-4 border-t border-slate-100">
              <button onClick={() => setErroDetalhe(null)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">
                Fechar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminTransactions;
