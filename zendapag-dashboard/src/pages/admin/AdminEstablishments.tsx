// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Estabelecimentos (Operação).
 * Cards de compliance/saldo, tabela, modal de edição com abas
 * (Básico / Empresa / Financeiro / Adquirentes) e menu de ações
 * (% MED, % disputa, retenção, bloquear, ativar). Dados mock.
 */
import React, { useMemo, useState, useRef, useEffect } from 'react';
import {
  Building2,
  CheckCircle2,
  Clock,
  Ban,
  Wallet,
  Lock,
  Search,
  MoreVertical,
  Pencil,
  Percent,
  ShieldAlert,
  PauseCircle,
  PlayCircle,
  X,
} from 'lucide-react';
import {
  brl,
  pct,
  estabelecimentos as estabSeed,
  estabelecimentosKpis as kpis,
  adquirentesDisponiveis,
} from '@/mock/admin';
import {
  StatCard,
  AdminCard,
  StatusBadge,
  PageHeader,
  GradientButton,
} from '@/components/admin/ui';

const statusMeta: Record<string, { tone: any; label: string }> = {
  ativo: { tone: 'success', label: 'Ativo' },
  bloqueado: { tone: 'danger', label: 'Bloqueado' },
  analise: { tone: 'warning', label: 'Em análise' },
  restrito: { tone: 'info', label: 'Restrito' },
};

const complianceMeta: Record<string, { tone: any; label: string }> = {
  aprovado: { tone: 'success', label: 'Aprovado' },
  pendente: { tone: 'warning', label: 'Pendente' },
  restrito: { tone: 'info', label: 'Restrito' },
  reprovado: { tone: 'danger', label: 'Reprovado' },
};

/* ───────────────── Menu de ações (dropdown) ───────────────── */
const ActionMenu: React.FC<{
  estab: any;
  onEdit: () => void;
  onAction: (tipo: string, estab: any) => void;
}> = ({ estab, onEdit, onAction }) => {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const close = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setOpen(false);
    };
    document.addEventListener('mousedown', close);
    return () => document.removeEventListener('mousedown', close);
  }, []);

  const ativo = estab.status === 'ativo';

  const items = [
    { key: 'edit', label: 'Editar', icon: Pencil, onClick: onEdit },
    { key: 'med', label: 'Ajustar % MED', icon: Percent, onClick: () => onAction('med', estab) },
    { key: 'disputa', label: 'Ajustar % disputa', icon: ShieldAlert, onClick: () => onAction('disputa', estab) },
    { key: 'retencao', label: 'Definir retenção', icon: Wallet, onClick: () => onAction('retencao', estab) },
    ativo
      ? { key: 'block', label: 'Bloquear', icon: PauseCircle, danger: true, onClick: () => onAction('bloquear', estab) }
      : { key: 'activate', label: 'Ativar', icon: PlayCircle, onClick: () => onAction('ativar', estab) },
  ];

  return (
    <div className="relative" ref={ref}>
      <button
        onClick={() => setOpen((o) => !o)}
        className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500"
      >
        <MoreVertical size={17} />
      </button>
      {open && (
        <div className="absolute right-0 mt-1 w-52 bg-white rounded-xl border border-slate-200 shadow-lg py-1.5 z-30">
          {items.map((it) => {
            const Icon = it.icon;
            return (
              <button
                key={it.key}
                onClick={() => {
                  setOpen(false);
                  it.onClick();
                }}
                className={`w-full flex items-center gap-2.5 px-3.5 py-2 text-sm hover:bg-slate-50 ${
                  it.danger ? 'text-rose-600' : 'text-slate-600'
                }`}
              >
                <Icon size={16} />
                {it.label}
              </button>
            );
          })}
        </div>
      )}
    </div>
  );
};

/* ───────────────── Modal de edição (abas) ───────────────── */
const tabs = ['Básico', 'Empresa', 'Financeiro', 'Adquirentes'] as const;

const Field: React.FC<{ label: string; children: React.ReactNode; className?: string }> = ({
  label,
  children,
  className,
}) => (
  <div className={className}>
    <label className="block text-xs font-medium text-slate-500 mb-1.5">{label}</label>
    {children}
  </div>
);

const inputCls =
  'w-full px-3 py-2 text-sm rounded-lg border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 transition-all';

const EditModal: React.FC<{ estab: any; onClose: () => void }> = ({ estab, onClose }) => {
  const [tab, setTab] = useState<(typeof tabs)[number]>('Básico');
  const [acq, setAcq] = useState<string[]>(estab.adquirentes);

  const toggleAcq = (name: string) =>
    setAcq((cur) => (cur.includes(name) ? cur.filter((a) => a !== name) : [...cur, name]));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
      <div className="bg-white rounded-2xl shadow-2xl w-full max-w-2xl max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-600 to-emerald-500 flex items-center justify-center text-white font-semibold">
              {estab.nome.charAt(0)}
            </div>
            <div>
              <h3 className="font-semibold text-slate-800">{estab.nome}</h3>
              <p className="text-xs text-slate-400">{estab.id} · {estab.documento}</p>
            </div>
          </div>
          <button onClick={onClose} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
            <X size={18} />
          </button>
        </div>

        {/* Abas */}
        <div className="flex gap-1 px-6 pt-4 border-b border-slate-100">
          {tabs.map((t) => (
            <button
              key={t}
              onClick={() => setTab(t)}
              className={`px-4 py-2.5 text-sm font-medium border-b-2 -mb-px transition-colors ${
                tab === t
                  ? 'border-blue-600 text-blue-700'
                  : 'border-transparent text-slate-500 hover:text-slate-700'
              }`}
            >
              {t}
            </button>
          ))}
        </div>

        {/* Conteúdo */}
        <div className="p-6 overflow-y-auto">
          {tab === 'Básico' && (
            <div className="grid grid-cols-2 gap-4">
              <Field label="Nome fantasia"><input className={inputCls} defaultValue={estab.nome} /></Field>
              <Field label="E-mail"><input className={inputCls} defaultValue={estab.email} /></Field>
              <Field label="Telefone"><input className={inputCls} defaultValue={estab.telefone} /></Field>
              <Field label="Tipo">
                <select className={inputCls} defaultValue={estab.tipo}>
                  <option value="PJ">Pessoa Jurídica</option>
                  <option value="PF">Pessoa Física</option>
                </select>
              </Field>
              <Field label="Status">
                <select className={inputCls} defaultValue={estab.status}>
                  <option value="ativo">Ativo</option>
                  <option value="analise">Em análise</option>
                  <option value="restrito">Restrito</option>
                  <option value="bloqueado">Bloqueado</option>
                </select>
              </Field>
              <Field label="Compliance">
                <select className={inputCls} defaultValue={estab.compliance}>
                  <option value="aprovado">Aprovado</option>
                  <option value="pendente">Pendente</option>
                  <option value="restrito">Restrito</option>
                  <option value="reprovado">Reprovado</option>
                </select>
              </Field>
            </div>
          )}

          {tab === 'Empresa' && (
            <div className="grid grid-cols-2 gap-4">
              <Field label="Razão social" className="col-span-2"><input className={inputCls} defaultValue={estab.razaoSocial} /></Field>
              <Field label="CNPJ / CPF"><input className={inputCls} defaultValue={estab.documento} /></Field>
              <Field label="Data de cadastro"><input className={inputCls} defaultValue={estab.criadoEm} /></Field>
              <Field label="CEP"><input className={inputCls} placeholder="00000-000" /></Field>
              <Field label="Cidade / UF"><input className={inputCls} placeholder="São Paulo / SP" /></Field>
              <Field label="Endereço" className="col-span-2"><input className={inputCls} placeholder="Logradouro, número, complemento" /></Field>
            </div>
          )}

          {tab === 'Financeiro' && (
            <div className="space-y-5">
              <div className="grid grid-cols-3 gap-4">
                <div className="rounded-xl bg-emerald-50 border border-emerald-100 p-4">
                  <p className="text-xs text-emerald-700">Saldo disponível</p>
                  <p className="text-lg font-bold text-emerald-800 tabular-nums mt-1">{brl(estab.saldoDisponivel)}</p>
                </div>
                <div className="rounded-xl bg-amber-50 border border-amber-100 p-4">
                  <p className="text-xs text-amber-700">Saldo pendente</p>
                  <p className="text-lg font-bold text-amber-800 tabular-nums mt-1">{brl(estab.saldoPendente)}</p>
                </div>
                <div className="rounded-xl bg-slate-50 border border-slate-200 p-4">
                  <p className="text-xs text-slate-500">Saldo retido</p>
                  <p className="text-lg font-bold text-slate-700 tabular-nums mt-1">{brl(estab.saldoRetido)}</p>
                </div>
              </div>
              <div className="grid grid-cols-3 gap-4">
                <Field label="% MED">
                  <div className="relative">
                    <input className={inputCls} defaultValue={estab.medPct} />
                    <Percent size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  </div>
                </Field>
                <Field label="% Disputa">
                  <div className="relative">
                    <input className={inputCls} defaultValue={estab.disputaPct} />
                    <Percent size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  </div>
                </Field>
                <Field label="% Retenção">
                  <div className="relative">
                    <input className={inputCls} defaultValue={estab.retencaoPct} />
                    <Percent size={14} className="absolute right-3 top-1/2 -translate-y-1/2 text-slate-400" />
                  </div>
                </Field>
              </div>
            </div>
          )}

          {tab === 'Adquirentes' && (
            <div className="space-y-3">
              <p className="text-sm text-slate-500">Selecione os adquirentes habilitados para roteamento das transações.</p>
              <div className="grid grid-cols-2 gap-3">
                {adquirentesDisponiveis.map((name) => {
                  const on = acq.includes(name);
                  return (
                    <button
                      key={name}
                      onClick={() => toggleAcq(name)}
                      className={`flex items-center justify-between px-4 py-3 rounded-xl border text-sm font-medium transition-all ${
                        on
                          ? 'border-blue-300 bg-blue-50 text-blue-700'
                          : 'border-slate-200 text-slate-600 hover:border-slate-300'
                      }`}
                    >
                      {name}
                      <span
                        className={`w-9 h-5 rounded-full p-0.5 transition-colors ${on ? 'bg-blue-600' : 'bg-slate-300'}`}
                      >
                        <span className={`block w-4 h-4 rounded-full bg-white transition-transform ${on ? 'translate-x-4' : ''}`} />
                      </span>
                    </button>
                  );
                })}
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-100">
          <button onClick={onClose} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">
            Cancelar
          </button>
          <GradientButton onClick={onClose}>Salvar alterações</GradientButton>
        </div>
      </div>
    </div>
  );
};

/* ───────────────── Página ───────────────── */
const AdminEstablishments: React.FC = () => {
  const [query, setQuery] = useState('');
  const [statusFilter, setStatusFilter] = useState('todos');
  const [editing, setEditing] = useState<any>(null);
  const [toast, setToast] = useState<string | null>(null);

  const handleAction = (tipo: string, estab: any) => {
    const labels: Record<string, string> = {
      med: `Ajuste de % MED solicitado para ${estab.nome}`,
      disputa: `Ajuste de % disputa solicitado para ${estab.nome}`,
      retencao: `Definição de retenção para ${estab.nome}`,
      bloquear: `${estab.nome} bloqueado`,
      ativar: `${estab.nome} ativado`,
    };
    setToast(labels[tipo] || 'Ação executada');
    setTimeout(() => setToast(null), 2500);
  };

  const filtered = useMemo(
    () =>
      estabSeed.filter((e) => {
        const q = query.toLowerCase();
        const matchQuery =
          !q ||
          e.nome.toLowerCase().includes(q) ||
          e.documento.includes(q) ||
          e.id.toLowerCase().includes(q);
        const matchStatus = statusFilter === 'todos' || e.status === statusFilter;
        return matchQuery && matchStatus;
      }),
    [query, statusFilter]
  );

  return (
    <div>
      <PageHeader
        title="Estabelecimentos"
        subtitle={`${kpis.total.toLocaleString('pt-BR')} contas cadastradas`}
        action={<GradientButton><Building2 size={16} /> Novo estabelecimento</GradientButton>}
      />

      {/* Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Ativos" value={kpis.ativos.toLocaleString('pt-BR')} accent="emerald" icon={<CheckCircle2 size={20} />} hint={`${kpis.total.toLocaleString('pt-BR')} no total`} />
        <StatCard label="Em análise (compliance)" value={kpis.emAnalise.toLocaleString('pt-BR')} accent="amber" icon={<Clock size={20} />} hint={`${kpis.documentosPendentes} docs pendentes`} />
        <StatCard label="Bloqueados" value={kpis.bloqueados.toLocaleString('pt-BR')} accent="rose" icon={<Ban size={20} />} />
        <StatCard label="Saldo retido total" value={brl(kpis.saldoRetido)} accent="violet" icon={<Lock size={20} />} hint={`Saldo total: ${brl(kpis.saldoTotal)}`} />
      </div>

      {/* Tabela */}
      <AdminCard>
        {/* Filtros */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
          <div className="relative flex-1 min-w-[220px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar por nome, documento ou ID..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          {['todos', 'ativo', 'analise', 'restrito', 'bloqueado'].map((s) => (
            <button
              key={s}
              onClick={() => setStatusFilter(s)}
              className={`px-3 py-2 text-sm rounded-lg font-medium transition-colors ${
                statusFilter === s
                  ? 'bg-blue-600 text-white'
                  : 'text-slate-500 hover:bg-slate-100'
              }`}
            >
              {s === 'todos' ? 'Todos' : statusMeta[s].label}
            </button>
          ))}
        </div>

        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                <th className="px-5 py-3 font-medium">Estabelecimento</th>
                <th className="px-5 py-3 font-medium">Compliance</th>
                <th className="px-5 py-3 font-medium">Status</th>
                <th className="px-5 py-3 font-medium text-right">Saldo disponível</th>
                <th className="px-5 py-3 font-medium text-right">MED / Disputa</th>
                <th className="px-5 py-3 font-medium">Adquirentes</th>
                <th className="px-5 py-3 font-medium text-right">Ações</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((e) => (
                <tr key={e.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                  <td className="px-5 py-3.5">
                    <div className="flex items-center gap-3">
                      <div className="w-9 h-9 rounded-lg bg-slate-100 flex items-center justify-center text-slate-500 font-semibold text-sm shrink-0">
                        {e.nome.charAt(0)}
                      </div>
                      <div className="min-w-0">
                        <p className="font-medium text-slate-700 truncate">{e.nome}</p>
                        <p className="text-xs text-slate-400 tabular-nums">{e.documento} · {e.tipo}</p>
                      </div>
                    </div>
                  </td>
                  <td className="px-5 py-3.5">
                    <StatusBadge tone={complianceMeta[e.compliance].tone}>{complianceMeta[e.compliance].label}</StatusBadge>
                  </td>
                  <td className="px-5 py-3.5">
                    <StatusBadge tone={statusMeta[e.status].tone}>{statusMeta[e.status].label}</StatusBadge>
                  </td>
                  <td className="px-5 py-3.5 text-right tabular-nums font-medium text-slate-700">{brl(e.saldoDisponivel)}</td>
                  <td className="px-5 py-3.5 text-right tabular-nums text-slate-500">
                    {pct(e.medPct)} / {pct(e.disputaPct)}
                  </td>
                  <td className="px-5 py-3.5">
                    {e.adquirentes.length ? (
                      <div className="flex flex-wrap gap-1">
                        {e.adquirentes.map((a) => (
                          <span key={a} className="text-xs px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">{a}</span>
                        ))}
                      </div>
                    ) : (
                      <span className="text-xs text-slate-300">—</span>
                    )}
                  </td>
                  <td className="px-5 py-3.5">
                    <div className="flex justify-end">
                      <ActionMenu estab={e} onEdit={() => setEditing(e)} onAction={handleAction} />
                    </div>
                  </td>
                </tr>
              ))}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={7} className="px-5 py-12 text-center text-slate-400 text-sm">
                    Nenhum estabelecimento encontrado.
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </AdminCard>

      {editing && <EditModal estab={editing} onClose={() => setEditing(null)} />}

      {/* Toast */}
      {toast && (
        <div className="fixed bottom-6 right-6 z-50 bg-slate-800 text-white text-sm px-4 py-3 rounded-xl shadow-lg flex items-center gap-2">
          <CheckCircle2 size={16} className="text-emerald-400" />
          {toast}
        </div>
      )}
    </div>
  );
};

export default AdminEstablishments;
