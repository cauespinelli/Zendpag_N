// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Gerentes (Gestão).
 * Gerentes de conta da plataforma: carteira de estabelecimentos, volume e
 * comissão de override. Cards, filtros, tabela, ver carteira, ativar/
 * desativar e cadastrar novo gerente. Dados mock, estado local.
 */
import React, { useMemo, useState } from 'react';
import {
  UserCog,
  Building2,
  Wallet,
  HandCoins,
  Search,
  Eye,
  Power,
  UserPlus,
  X,
  Mail,
  Phone,
  Store,
} from 'lucide-react';
import {
  brl,
  pct,
  gerentes as gerentesSeed,
  gerentesKpis as kpis,
} from '@/mock/admin';
import { StatCard, AdminCard, StatusBadge, PageHeader, GradientButton } from '@/components/admin/ui';

const statusMeta: Record<string, { tone: any; label: string }> = {
  ativo: { tone: 'success', label: 'Ativo' },
  ferias: { tone: 'info', label: 'Em férias' },
  inativo: { tone: 'neutral', label: 'Inativo' },
};

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';
const inputCls =
  'w-full px-3 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100';

const novoVazio = { nome: '', email: '', telefone: '', comissaoPct: '0.5' };

const AdminManagers: React.FC = () => {
  const [gerentes, setGerentes] = useState(gerentesSeed);
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('todos');
  const [carteira, setCarteira] = useState<any>(null);
  const [novoOpen, setNovoOpen] = useState(false);
  const [novo, setNovo] = useState(novoVazio);

  const filtered = useMemo(
    () =>
      gerentes.filter((g) => {
        const q = query.toLowerCase();
        if (q && !g.nome.toLowerCase().includes(q) && !g.email.toLowerCase().includes(q) && !g.id.toLowerCase().includes(q))
          return false;
        if (status !== 'todos' && g.status !== status) return false;
        return true;
      }),
    [gerentes, query, status]
  );

  const toggleStatus = (id: string) =>
    setGerentes((prev) => prev.map((g) => (g.id === id ? { ...g, status: g.status === 'inativo' ? 'ativo' : 'inativo' } : g)));

  const salvarNovo = () => {
    setGerentes((prev) => [
      {
        id: `GER-${119 + prev.length}`,
        nome: novo.nome || 'Novo gerente',
        email: novo.email,
        telefone: novo.telefone,
        carteira: [],
        volumeCarteira: 0,
        comissaoPct: parseFloat(novo.comissaoPct) || 0,
        comissaoAcum: 0,
        status: 'ativo',
        desde: '2026-06-23',
        ultimaAtividade: '2026-06-23 14:55',
      },
      ...prev,
    ]);
    setNovoOpen(false);
    setNovo(novoVazio);
  };

  return (
    <div>
      <PageHeader
        title="Gerentes"
        subtitle="Gerentes de conta e suas carteiras de estabelecimentos"
        action={
          <GradientButton onClick={() => { setNovo(novoVazio); setNovoOpen(true); }}>
            <UserPlus size={16} /> Novo gerente
          </GradientButton>
        }
      />

      {/* Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Gerentes ativos" value={kpis.ativos.toLocaleString('pt-BR')} accent="blue" icon={<UserCog size={20} />} />
        <StatCard label="Estabelecimentos geridos" value={kpis.estabelecimentosGeridos.toLocaleString('pt-BR')} accent="violet" icon={<Building2 size={20} />} />
        <StatCard label="Volume da carteira" value={brl(kpis.volumeCarteira)} accent="emerald" icon={<Wallet size={20} />} />
        <StatCard label="Comissão de gerentes (mês)" value={brl(kpis.comissaoMes)} accent="amber" icon={<HandCoins size={20} />} />
      </div>

      <AdminCard>
        {/* Filtros */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar por nome, e-mail ou ID..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectCls}>
            <option value="todos">Todos os status</option>
            <option value="ativo">Ativo</option>
            <option value="ferias">Em férias</option>
            <option value="inativo">Inativo</option>
          </select>
        </div>

        {/* Tabela */}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                <th className="px-5 py-3 font-medium">Gerente</th>
                <th className="px-5 py-3 font-medium text-right">Carteira</th>
                <th className="px-5 py-3 font-medium text-right">Volume</th>
                <th className="px-5 py-3 font-medium text-right">Override</th>
                <th className="px-5 py-3 font-medium text-right">Comissão acum.</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 font-medium text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((g) => (
                <tr key={g.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                  <td className="px-5 py-3.5">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-emerald-500 text-white text-xs font-semibold flex items-center justify-center shrink-0">
                        {g.nome.split(' ').map((n) => n[0]).slice(0, 2).join('')}
                      </div>
                      <div>
                        <p className="font-medium text-slate-700">{g.nome}</p>
                        <p className="text-xs text-slate-400">{g.email}</p>
                        <p className="text-[11px] text-slate-300">{g.id} · desde {g.desde}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-5 py-3.5 text-right">
                    <button
                      onClick={() => setCarteira(g)}
                      disabled={!g.carteira.length}
                      className="inline-flex items-center gap-1 text-sm tabular-nums text-slate-700 hover:text-blue-700 disabled:text-slate-300 disabled:cursor-default"
                    >
                      <Store size={14} /> {g.carteira.length}
                    </button>
                  </td>
                  <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{brl(g.volumeCarteira)}</td>
                  <td className="px-5 py-3.5 text-right tabular-nums text-slate-500">{pct(g.comissaoPct)}</td>
                  <td className="px-5 py-3.5 text-right tabular-nums font-semibold text-slate-800">{brl(g.comissaoAcum)}</td>
                  <td className="px-5 py-3.5">
                    <StatusBadge tone={statusMeta[g.status].tone}>{statusMeta[g.status].label}</StatusBadge>
                  </td>
                  <td className="px-5 py-3.5">
                    <div className="flex items-center justify-end gap-1">
                      <button onClick={() => setCarteira(g)} className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Ver carteira">
                        <Eye size={16} />
                      </button>
                      <button
                        onClick={() => toggleStatus(g.id)}
                        className={`w-8 h-8 rounded-lg flex items-center justify-center transition-colors ${
                          g.status === 'inativo' ? 'text-emerald-600 hover:bg-emerald-50' : 'text-slate-500 hover:bg-rose-50 hover:text-rose-600'
                        }`}
                        title={g.status === 'inativo' ? 'Reativar gerente' : 'Desativar gerente'}
                      >
                        <Power size={16} />
                      </button>
                    </div>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400 text-sm">Nenhum gerente encontrado com os filtros atuais.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </AdminCard>

      {/* Modal: ver carteira */}
      {carteira && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <Store size={18} className="text-blue-500" /> Carteira de {carteira.nome}
              </h3>
              <button onClick={() => setCarteira(null)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-xs text-slate-400">Volume da carteira</p>
                  <p className="font-semibold text-slate-800 tabular-nums">{brl(carteira.volumeCarteira)}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Comissão acumulada</p>
                  <p className="font-semibold text-slate-800 tabular-nums">{brl(carteira.comissaoAcum)}</p>
                </div>
              </div>
              <div>
                <p className="text-xs font-medium text-slate-500 mb-2">{carteira.carteira.length} estabelecimentos</p>
                <div className="space-y-1.5 max-h-60 overflow-y-auto">
                  {carteira.carteira.length ? carteira.carteira.map((e: string) => (
                    <div key={e} className="flex items-center gap-2 px-3 py-2 rounded-lg bg-slate-50 border border-slate-100">
                      <Building2 size={15} className="text-slate-400" />
                      <span className="text-sm text-slate-700">{e}</span>
                    </div>
                  )) : <p className="text-sm text-slate-400 py-4 text-center">Carteira vazia.</p>}
                </div>
              </div>
            </div>
            <div className="flex justify-end px-6 py-4 border-t border-slate-100">
              <button onClick={() => setCarteira(null)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">Fechar</button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: novo gerente */}
      {novoOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <UserPlus size={18} className="text-blue-500" /> Novo gerente
              </h3>
              <button onClick={() => setNovoOpen(false)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Nome completo</label>
                <input value={novo.nome} onChange={(e) => setNovo({ ...novo, nome: e.target.value })} placeholder="Nome do gerente" className={inputCls} />
              </div>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1.5">E-mail</label>
                  <div className="relative">
                    <Mail size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input value={novo.email} onChange={(e) => setNovo({ ...novo, email: e.target.value })} placeholder="nome@zendpag.com" className={`${inputCls} pl-9`} />
                  </div>
                </div>
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1.5">Telefone</label>
                  <div className="relative">
                    <Phone size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                    <input value={novo.telefone} onChange={(e) => setNovo({ ...novo, telefone: e.target.value })} placeholder="(11) 99999-9999" className={`${inputCls} pl-9`} />
                  </div>
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Override sobre o volume</label>
                <div className="relative max-w-[140px]">
                  <input type="number" step="0.1" min={0} value={novo.comissaoPct} onChange={(e) => setNovo({ ...novo, comissaoPct: e.target.value })} className={`${inputCls} pr-9 tabular-nums`} />
                  <span className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400 text-sm">%</span>
                </div>
              </div>
              <p className="text-xs text-slate-400">A carteira de estabelecimentos pode ser atribuída depois, no detalhe do gerente.</p>
            </div>
            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => setNovoOpen(false)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">Cancelar</button>
              <button
                onClick={salvarNovo}
                disabled={!novo.nome || !novo.email}
                className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-700 hover:to-emerald-600 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <UserPlus size={16} /> Cadastrar gerente
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminManagers;
