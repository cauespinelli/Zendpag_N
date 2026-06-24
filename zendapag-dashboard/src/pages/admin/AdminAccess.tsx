// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Usuários, Perfis e Permissões (Controle de Acesso).
 * Abas: Usuários (implementada) / Perfis / Permissões (próximo passo).
 * Cards, filtros, tabela de usuários, convite e ativar/desativar. Mock.
 */
import React, { useMemo, useState } from 'react';
import {
  Users,
  UserCheck,
  ShieldCheck,
  KeyRound,
  Search,
  UserPlus,
  Power,
  Eye,
  X,
  Mail,
  ShieldOff,
  Construction,
} from 'lucide-react';
import {
  acessoKpis as kpis,
  usuariosAdmin as usuariosSeed,
  perfisAcesso,
} from '@/mock/admin';
import { StatCard, AdminCard, StatusBadge, PageHeader, GradientButton } from '@/components/admin/ui';

const statusMeta: Record<string, { tone: any; label: string }> = {
  ativo: { tone: 'success', label: 'Ativo' },
  convidado: { tone: 'info', label: 'Convidado' },
  inativo: { tone: 'neutral', label: 'Inativo' },
};

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';
const inputCls =
  'w-full px-3 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100';

const iniciais = (nome: string) => nome.split(' ').map((n) => n[0]).slice(0, 2).join('').toUpperCase();
const conviteVazio = { nome: '', email: '', perfil: 'Suporte' };

const AdminAccess: React.FC = () => {
  const [aba, setAba] = useState<'usuarios' | 'perfis' | 'permissoes'>('usuarios');
  const [usuarios, setUsuarios] = useState(usuariosSeed);
  const [query, setQuery] = useState('');
  const [perfil, setPerfil] = useState('todos');
  const [status, setStatus] = useState('todos');
  const [conviteOpen, setConviteOpen] = useState(false);
  const [convite, setConvite] = useState(conviteVazio);

  const filtered = useMemo(
    () =>
      usuarios.filter((u) => {
        const q = query.toLowerCase();
        if (q && !u.nome.toLowerCase().includes(q) && !u.email.toLowerCase().includes(q) && !u.id.toLowerCase().includes(q))
          return false;
        if (perfil !== 'todos' && u.perfil !== perfil) return false;
        if (status !== 'todos' && u.status !== status) return false;
        return true;
      }),
    [usuarios, query, perfil, status]
  );

  const toggleStatus = (id: string) =>
    setUsuarios((prev) => prev.map((u) => (u.id === id ? { ...u, status: u.status === 'inativo' ? 'ativo' : 'inativo' } : u)));

  const enviarConvite = () => {
    setUsuarios((prev) => [
      {
        id: `USR-${310 + prev.length}`,
        nome: convite.nome || 'Convidado',
        email: convite.email,
        perfil: convite.perfil,
        status: 'convidado',
        twoFA: false,
        ultimoAcesso: '—',
        criadoEm: '2026-06-23',
      },
      ...prev,
    ]);
    setConviteOpen(false);
    setConvite(conviteVazio);
  };

  return (
    <div>
      <PageHeader
        title="Usuários e Permissões"
        subtitle="Equipe do gateway, perfis de acesso e matriz de permissões"
        action={
          aba === 'usuarios' ? (
            <GradientButton onClick={() => { setConvite(conviteVazio); setConviteOpen(true); }}>
              <UserPlus size={16} /> Convidar usuário
            </GradientButton>
          ) : null
        }
      />

      {/* KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Usuários" value={String(kpis.usuarios)} accent="blue" icon={<Users size={20} />} />
        <StatCard label="Ativos" value={String(kpis.ativos)} accent="emerald" icon={<UserCheck size={20} />} />
        <StatCard label="Perfis de acesso" value={String(kpis.perfis)} accent="violet" icon={<ShieldCheck size={20} />} />
        <StatCard label="Com 2FA" value={`${kpis.com2fa}/${kpis.usuarios}`} accent="amber" icon={<KeyRound size={20} />} hint="Autenticação em dois fatores" />
      </div>

      <AdminCard>
        {/* Abas */}
        <div className="flex flex-wrap gap-1 px-4 pt-3 border-b border-slate-100">
          {[
            { key: 'usuarios', label: 'Usuários' },
            { key: 'perfis', label: 'Perfis' },
            { key: 'permissoes', label: 'Permissões' },
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

        {/* ───────── Aba: Usuários ───────── */}
        {aba === 'usuarios' && (
          <>
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
              <select value={perfil} onChange={(e) => setPerfil(e.target.value)} className={selectCls}>
                <option value="todos">Todos os perfis</option>
                {perfisAcesso.map((p) => <option key={p.id} value={p.nome}>{p.nome}</option>)}
              </select>
              <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectCls}>
                <option value="todos">Todos os status</option>
                <option value="ativo">Ativo</option>
                <option value="convidado">Convidado</option>
                <option value="inativo">Inativo</option>
              </select>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                    <th className="px-5 py-3 font-medium">Usuário</th>
                    <th className="px-5 py-3 font-medium">Perfil</th>
                    <th className="px-5 py-3 font-medium">2FA</th>
                    <th className="px-5 py-3 font-medium">Status</th>
                    <th className="px-5 py-3 font-medium text-right">Último acesso</th>
                    <th className="px-5 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {filtered.map((u) => (
                    <tr key={u.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                      <td className="px-5 py-3.5">
                        <div className="flex items-center gap-3">
                          <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-emerald-500 text-white text-xs font-semibold flex items-center justify-center shrink-0">
                            {iniciais(u.nome)}
                          </div>
                          <div>
                            <p className="font-medium text-slate-700">{u.nome}</p>
                            <p className="text-xs text-slate-400">{u.email}</p>
                            <p className="text-[11px] text-slate-300">{u.id}</p>
                          </div>
                        </div>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className="text-xs px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">{u.perfil}</span>
                      </td>
                      <td className="px-5 py-3.5">
                        {u.twoFA ? (
                          <span className="inline-flex items-center gap-1 text-xs text-emerald-600"><ShieldCheck size={14} /> Ativo</span>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs text-slate-400"><ShieldOff size={14} /> Inativo</span>
                        )}
                      </td>
                      <td className="px-5 py-3.5">
                        <StatusBadge tone={statusMeta[u.status].tone}>{statusMeta[u.status].label}</StatusBadge>
                      </td>
                      <td className="px-5 py-3.5 text-right text-xs text-slate-500 tabular-nums">{u.ultimoAcesso}</td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center justify-end gap-1">
                          <button className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Ver usuário">
                            <Eye size={16} />
                          </button>
                          <button
                            onClick={() => toggleStatus(u.id)}
                            disabled={u.perfil === 'Admin Master'}
                            className={`w-8 h-8 rounded-lg flex items-center justify-center transition-colors disabled:opacity-30 disabled:cursor-not-allowed ${
                              u.status === 'inativo' ? 'text-emerald-600 hover:bg-emerald-50' : 'text-slate-500 hover:bg-rose-50 hover:text-rose-600'
                            }`}
                            title={u.perfil === 'Admin Master' ? 'Admin Master não pode ser desativado' : u.status === 'inativo' ? 'Reativar usuário' : 'Desativar usuário'}
                          >
                            <Power size={16} />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                  {filtered.length === 0 && (
                    <tr>
                      <td colSpan={6} className="px-5 py-12 text-center text-slate-400 text-sm">Nenhum usuário encontrado com os filtros atuais.</td>
                    </tr>
                  )}
                </tbody>
              </table>
            </div>
          </>
        )}

        {/* ───────── Abas: Perfis / Permissões (próximo passo) ───────── */}
        {aba !== 'usuarios' && (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-xl bg-slate-100 flex items-center justify-center text-slate-400 mb-3">
              <Construction size={22} />
            </div>
            <p className="text-sm font-medium text-slate-600">Aba "{aba === 'perfis' ? 'Perfis' : 'Permissões'}" — próximo passo</p>
            <p className="text-xs text-slate-400 mt-1 max-w-sm">
              {aba === 'perfis'
                ? 'Lista de perfis de acesso (Admin Master, Gerente de risco, Financeiro...) já estruturada no mock; a UI vem na próxima tela.'
                : 'Matriz de permissões por perfil (toggles granulares) vem na próxima tela.'}
            </p>
          </div>
        )}
      </AdminCard>

      {/* Modal: convidar usuário */}
      {conviteOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <UserPlus size={18} className="text-blue-500" /> Convidar usuário
              </h3>
              <button onClick={() => setConviteOpen(false)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Nome completo</label>
                <input value={convite.nome} onChange={(e) => setConvite({ ...convite, nome: e.target.value })} placeholder="Nome do usuário" className={inputCls} />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">E-mail corporativo</label>
                <div className="relative">
                  <Mail size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  <input value={convite.email} onChange={(e) => setConvite({ ...convite, email: e.target.value })} placeholder="nome@zendpag.com" className={`${inputCls} pl-9`} />
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Perfil de acesso</label>
                <select value={convite.perfil} onChange={(e) => setConvite({ ...convite, perfil: e.target.value })} className={`${selectCls} w-full`}>
                  {perfisAcesso.filter((p) => p.nome !== 'Admin Master').map((p) => (
                    <option key={p.id} value={p.nome}>{p.nome} — {p.descricao}</option>
                  ))}
                </select>
              </div>
              <div className="rounded-xl bg-blue-50 border border-blue-100 p-3 text-xs text-blue-700">
                O usuário recebe um e-mail com link de ativação e será obrigado a configurar 2FA no primeiro acesso.
              </div>
            </div>
            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => setConviteOpen(false)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">Cancelar</button>
              <button
                onClick={enviarConvite}
                disabled={!convite.nome || !convite.email}
                className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-700 hover:to-emerald-600 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <Mail size={16} /> Enviar convite
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminAccess;
