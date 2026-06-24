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
  Lock,
  Plus,
  Save,
  SlidersHorizontal,
  CheckCircle2,
} from 'lucide-react';
import {
  acessoKpis as kpis,
  usuariosAdmin as usuariosSeed,
  perfisAcesso as perfisSeed,
  permissoesCatalogo,
  permissoesPorPerfil,
  todasPermissoes,
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
const perfilVazio = { nome: '', descricao: '', base: 'Suporte' };

const corMap: Record<string, { chip: string; dot: string }> = {
  blue: { chip: 'text-blue-700 bg-blue-50 border-blue-200', dot: 'bg-blue-500' },
  rose: { chip: 'text-rose-700 bg-rose-50 border-rose-200', dot: 'bg-rose-500' },
  emerald: { chip: 'text-emerald-700 bg-emerald-50 border-emerald-200', dot: 'bg-emerald-500' },
  violet: { chip: 'text-violet-700 bg-violet-50 border-violet-200', dot: 'bg-violet-500' },
  amber: { chip: 'text-amber-700 bg-amber-50 border-amber-200', dot: 'bg-amber-500' },
};

// matriz inicial: role -> Set de chaves concedidas
const matrizInicial = () => {
  const m: Record<string, Set<string>> = {};
  Object.entries(permissoesPorPerfil).forEach(([role, keys]) => { m[role] = new Set(keys); });
  return m;
};

const AdminAccess: React.FC = () => {
  const [aba, setAba] = useState<'usuarios' | 'perfis' | 'permissoes'>('usuarios');
  const [usuarios, setUsuarios] = useState(usuariosSeed);
  const [perfis, setPerfis] = useState(perfisSeed);
  const [query, setQuery] = useState('');
  const [perfil, setPerfil] = useState('todos');
  const [status, setStatus] = useState('todos');
  const [conviteOpen, setConviteOpen] = useState(false);
  const [convite, setConvite] = useState(conviteVazio);

  // Perfis
  const [novoPerfilOpen, setNovoPerfilOpen] = useState(false);
  const [novoPerfil, setNovoPerfil] = useState(perfilVazio);

  // Permissões
  const [matriz, setMatriz] = useState(matrizInicial);
  const [dirty, setDirty] = useState(false);
  const [salvo, setSalvo] = useState(false);

  const togglePerm = (role: string, key: string) => {
    if (role === 'Admin Master') return; // travado
    setMatriz((prev) => {
      const next = { ...prev, [role]: new Set(prev[role]) };
      next[role].has(key) ? next[role].delete(key) : next[role].add(key);
      return next;
    });
    setDirty(true);
    setSalvo(false);
  };

  const salvarMatriz = () => { setDirty(false); setSalvo(true); };

  const criarPerfil = () => {
    setPerfis((prev) => [
      ...prev,
      {
        id: `PF-${prev.length + 1}`,
        nome: novoPerfil.nome || 'Novo perfil',
        descricao: novoPerfil.descricao || 'Perfil personalizado',
        usuarios: 0,
        cor: 'emerald',
      },
    ]);
    setMatriz((prev) => ({ ...prev, [novoPerfil.nome || 'Novo perfil']: new Set(permissoesPorPerfil[novoPerfil.base] || []) }));
    setNovoPerfilOpen(false);
    setNovoPerfil(perfilVazio);
  };

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
          ) : aba === 'perfis' ? (
            <GradientButton onClick={() => { setNovoPerfil(perfilVazio); setNovoPerfilOpen(true); }}>
              <Plus size={16} /> Novo perfil
            </GradientButton>
          ) : dirty ? (
            <GradientButton onClick={salvarMatriz}>
              <Save size={16} /> Salvar alterações
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
                {perfis.map((p) => <option key={p.id} value={p.nome}>{p.nome}</option>)}
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

        {/* ───────── Aba: Perfis ───────── */}
        {aba === 'perfis' && (
          <div className="p-5 grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-3 gap-4">
            {perfis.map((p) => {
              const cor = corMap[p.cor] || corMap.emerald;
              const qtdPerms = matriz[p.nome]?.size ?? 0;
              const locked = p.nome === 'Admin Master';
              return (
                <div key={p.id} className="rounded-xl border border-slate-200 p-4 hover:shadow-sm transition-shadow">
                  <div className="flex items-start justify-between gap-2">
                    <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-2.5 py-1 rounded-full border ${cor.chip}`}>
                      <span className={`w-1.5 h-1.5 rounded-full ${cor.dot}`} /> {p.nome}
                    </span>
                    {locked && <Lock size={14} className="text-slate-300 mt-1" title="Perfil do sistema" />}
                  </div>
                  <p className="text-xs text-slate-500 mt-3 min-h-[32px]">{p.descricao}</p>
                  <div className="flex items-center justify-between mt-4 pt-3 border-t border-slate-100 text-sm">
                    <span className="flex items-center gap-1.5 text-slate-600"><Users size={14} className="text-slate-400" /> {p.usuarios}</span>
                    <span className="flex items-center gap-1.5 text-slate-600"><KeyRound size={14} className="text-slate-400" /> {qtdPerms} permissões</span>
                  </div>
                  <button
                    onClick={() => setAba('permissoes')}
                    className="mt-3 w-full inline-flex items-center justify-center gap-1.5 text-xs font-semibold text-blue-700 bg-blue-50 hover:bg-blue-100 py-2 rounded-lg transition-colors"
                  >
                    <SlidersHorizontal size={14} /> Ver permissões
                  </button>
                </div>
              );
            })}
          </div>
        )}

        {/* ───────── Aba: Permissões (matriz) ───────── */}
        {aba === 'permissoes' && (
          <>
            {salvo && (
              <div className="m-4 mb-0 flex items-center gap-2 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-2.5 text-sm text-emerald-700">
                <CheckCircle2 size={16} className="text-emerald-500" /> Permissões salvas com sucesso.
              </div>
            )}
            <div className="p-4 text-xs text-slate-400 border-b border-slate-100">
              Marque as permissões concedidas a cada perfil. A coluna <strong className="text-slate-500">Admin Master</strong> é travada (acesso total).
            </div>
            <div className="overflow-x-auto">
              <table className="w-full text-sm">
                <thead>
                  <tr className="border-b border-slate-100">
                    <th className="px-5 py-3 text-left text-xs font-medium text-slate-400 sticky left-0 bg-white min-w-[240px]">Permissão</th>
                    {perfis.map((p) => (
                      <th key={p.id} className="px-3 py-3 text-center text-xs font-medium text-slate-500 min-w-[110px]">{p.nome}</th>
                    ))}
                  </tr>
                </thead>
                <tbody>
                  {permissoesCatalogo.map((g) => (
                    <React.Fragment key={g.grupo}>
                      <tr className="bg-slate-50/70">
                        <td colSpan={perfis.length + 1} className="px-5 py-2 text-[11px] font-semibold uppercase tracking-wider text-slate-400">
                          {g.grupo}
                        </td>
                      </tr>
                      {g.permissoes.map((perm) => (
                        <tr key={perm.key} className="border-b border-slate-50 hover:bg-slate-50/40">
                          <td className="px-5 py-3 text-slate-700 sticky left-0 bg-white">{perm.label}</td>
                          {perfis.map((p) => {
                            const locked = p.nome === 'Admin Master';
                            const checked = locked ? true : matriz[p.nome]?.has(perm.key) ?? false;
                            return (
                              <td key={p.id} className="px-3 py-3 text-center">
                                <input
                                  type="checkbox"
                                  checked={checked}
                                  disabled={locked}
                                  onChange={() => togglePerm(p.nome, perm.key)}
                                  className="rounded border-slate-300 text-blue-600 focus:ring-blue-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                                />
                              </td>
                            );
                          })}
                        </tr>
                      ))}
                    </React.Fragment>
                  ))}
                </tbody>
              </table>
            </div>
          </>
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
                  {perfis.filter((p) => p.nome !== 'Admin Master').map((p) => (
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

      {/* Modal: novo perfil */}
      {novoPerfilOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <ShieldCheck size={18} className="text-blue-500" /> Novo perfil de acesso
              </h3>
              <button onClick={() => setNovoPerfilOpen(false)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Nome do perfil</label>
                <input value={novoPerfil.nome} onChange={(e) => setNovoPerfil({ ...novoPerfil, nome: e.target.value })} placeholder="Ex.: Analista de prevenção" className={inputCls} />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Descrição</label>
                <input value={novoPerfil.descricao} onChange={(e) => setNovoPerfil({ ...novoPerfil, descricao: e.target.value })} placeholder="O que este perfil pode fazer" className={inputCls} />
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Herdar permissões de</label>
                <select value={novoPerfil.base} onChange={(e) => setNovoPerfil({ ...novoPerfil, base: e.target.value })} className={`${selectCls} w-full`}>
                  {perfis.filter((p) => p.nome !== 'Admin Master').map((p) => (
                    <option key={p.id} value={p.nome}>{p.nome}</option>
                  ))}
                </select>
                <p className="text-[11px] text-slate-400 mt-1.5">As permissões podem ser ajustadas depois na matriz.</p>
              </div>
            </div>
            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => setNovoPerfilOpen(false)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">Cancelar</button>
              <button
                onClick={criarPerfil}
                disabled={!novoPerfil.nome}
                className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-700 hover:to-emerald-600 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <Plus size={16} /> Criar perfil
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminAccess;
