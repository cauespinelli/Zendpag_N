// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Estabelecimentos (Operação).
 * Cards de compliance/saldo, tabela, modal de edição com abas
 * (Básico / Empresa / Financeiro / Adquirentes) e menu de ações
 * (% MED, % disputa, retenção, bloquear, ativar). Dados mock.
 */
import React, { useCallback, useMemo, useState, useRef, useEffect } from 'react';
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
  RefreshCw,
  Loader,
  Inbox,
  WifiOff,
} from 'lucide-react';
import {
  brl,
  pct,
  pixInAdquirentes,
  pixOutAdquirentes,
} from '@/mock/admin';
import {
  StatCard,
  AdminCard,
  StatusBadge,
  PageHeader,
  GradientButton,
} from '@/components/admin/ui';
import { medService } from '@/services/medService';
import { adminMerchantService } from '@/services/adminMerchantService';
import { adminHttpError } from '@/services/adminHttp';

// formata dinheiro, ou "—" quando o backend não fornece o valor
const money = (v: any) => (v != null ? brl(v) : '—');

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

/* Campo somente-leitura (visualização de dados) */
const ReadField: React.FC<{ label: string; value: React.ReactNode; className?: string }> = ({
  label,
  value,
  className,
}) => (
  <div className={className}>
    <label className="block text-xs font-medium text-slate-500 mb-1.5">{label}</label>
    <div className="w-full px-3 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 text-slate-800 min-h-[38px] break-words">
      {value ?? '—'}
    </div>
  </div>
);

/* Toggle de adquirente (habilitar/desabilitar) */
const AcqToggle: React.FC<{ name: string; on: boolean; onToggle: () => void }> = ({ name, on, onToggle }) => (
  <button
    onClick={onToggle}
    className={`flex items-center justify-between px-4 py-3 rounded-xl border text-sm font-medium transition-all ${
      on ? 'border-blue-300 bg-blue-50 text-blue-700' : 'border-slate-200 text-slate-600 hover:border-slate-300'
    }`}
  >
    {name}
    <span className={`w-9 h-5 rounded-full p-0.5 transition-colors ${on ? 'bg-blue-600' : 'bg-slate-300'}`}>
      <span className={`block w-4 h-4 rounded-full bg-white transition-transform ${on ? 'translate-x-4' : ''}`} />
    </span>
  </button>
);

const tipoLabel = (t: string) => (t === 'PJ' ? 'Pessoa Jurídica' : 'Pessoa Física');

const EditModal: React.FC<{ estab: any; onClose: () => void; onSave: (id: string, acqIn: string[], acqOut: string[]) => void }> = ({ estab, onClose, onSave }) => {
  const [tab, setTab] = useState<(typeof tabs)[number]>('Básico');
  // PIX IN: pré-marca os adquirentes salvos no estabelecimento
  const [acqIn, setAcqIn] = useState<string[]>(
    (estab.adquirentes || []).filter((a: string) => pixInAdquirentes.includes(a))
  );
  // PIX OUT: pré-marca os provedores salvos (campo próprio, persiste ao reabrir)
  const [acqOut, setAcqOut] = useState<string[]>(estab.adquirentesOut || []);

  const salvar = () => {
    onSave(estab.id, acqIn, acqOut);
    onClose();
  };

  const toggleIn = (name: string) =>
    setAcqIn((cur) => (cur.includes(name) ? cur.filter((a) => a !== name) : [...cur, name]));
  const toggleOut = (name: string) =>
    setAcqOut((cur) => (cur.includes(name) ? cur.filter((a) => a !== name) : [...cur, name]));

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
              <ReadField label="Razão social" value={estab.razaoSocial} className="col-span-2" />
              <ReadField label="Nome fantasia" value={estab.nome} />
              <ReadField label="E-mail" value={estab.email} />
              <ReadField label="Telefone" value={estab.telefone} />
              <ReadField label="Tipo" value={tipoLabel(estab.tipo)} />
              <ReadField label="Status" value={statusMeta[estab.status]?.label} />
              <ReadField label="Compliance" value={complianceMeta[estab.compliance]?.label} />
            </div>
          )}

          {tab === 'Empresa' && (
            <div className="grid grid-cols-2 gap-4">
              <ReadField label="Razão social" value={estab.razaoSocial} className="col-span-2" />
              <ReadField label="CNPJ / CPF" value={estab.documento} />
              <ReadField label="Tipo" value={tipoLabel(estab.tipo)} />
              <ReadField label="Data de cadastro" value={estab.criadoEm} />
              <ReadField label="Volume no mês" value={<span className="tabular-nums">{money(estab.volumeMes)}</span>} />
              <ReadField label="Adquirentes ativos" value={estab.adquirentes.length ? estab.adquirentes.join(', ') : 'Nenhum'} className="col-span-2" />
            </div>
          )}

          {tab === 'Financeiro' && (
            <div className="space-y-5">
              <p className="text-sm text-slate-500">Saldos do estabelecimento.</p>
              <div className="grid grid-cols-3 gap-4">
                <div className="rounded-xl bg-emerald-50 border border-emerald-100 p-4">
                  <p className="text-xs text-emerald-700">Saldo disponível</p>
                  <p className="text-lg font-bold text-emerald-800 tabular-nums mt-1">{money(estab.saldoDisponivel)}</p>
                </div>
                <div className="rounded-xl bg-amber-50 border border-amber-100 p-4">
                  <p className="text-xs text-amber-700">Saldo pendente</p>
                  <p className="text-lg font-bold text-amber-800 tabular-nums mt-1">{money(estab.saldoPendente)}</p>
                </div>
                <div className="rounded-xl bg-slate-50 border border-slate-200 p-4">
                  <p className="text-xs text-slate-500">Saldo retido</p>
                  <p className="text-lg font-bold text-slate-700 tabular-nums mt-1">{money(estab.saldoRetido)}</p>
                </div>
              </div>
            </div>
          )}

          {tab === 'Adquirentes' && (
            <div className="space-y-6">
              {/* PIX IN */}
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold px-2 py-1 rounded-md bg-emerald-50 text-emerald-700 border border-emerald-200">PIX IN</span>
                  <p className="text-sm text-slate-500">Adquirentes para recebimento (cash-in).</p>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  {pixInAdquirentes.map((name) => (
                    <AcqToggle key={name} name={name} on={acqIn.includes(name)} onToggle={() => toggleIn(name)} />
                  ))}
                </div>
              </div>
              {/* PIX OUT */}
              <div className="space-y-3">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-semibold px-2 py-1 rounded-md bg-blue-50 text-blue-700 border border-blue-200">PIX OUT</span>
                  <p className="text-sm text-slate-500">Provedores para pagamento/saque (cash-out).</p>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  {pixOutAdquirentes.map((name) => (
                    <AcqToggle key={name} name={name} on={acqOut.includes(name)} onToggle={() => toggleOut(name)} />
                  ))}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-end gap-3 px-6 py-4 border-t border-slate-100">
          <button onClick={onClose} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">
            Cancelar
          </button>
          <GradientButton onClick={salvar}>Salvar alterações</GradientButton>
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

  // Estabelecimentos vindos do backend (GET /merchants, ADMIN)
  const [estabs, setEstabs] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true); setErro(null);
    try { setEstabs(await adminMerchantService.listAll()); }
    catch (e: any) { setErro(adminHttpError(e)); setEstabs([]); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { carregar(); }, [carregar]);

  // Persistência local da seleção de adquirentes (backend ainda não tem endpoint)
  const handleSaveAcq = (id: string, acqIn: string[], acqOut: string[]) => {
    setEstabs((prev) => prev.map((e) => (e.id === id ? { ...e, adquirentes: acqIn, adquirentesOut: acqOut } : e)));
    setToast('Adquirentes salvos (apenas nesta sessão — sem endpoint no backend ainda)');
    setTimeout(() => setToast(null), 3000);
  };

  // MED por estabelecimento — via medService (mock/API de disputa). Para ids reais
  // do backend ainda não há índice, então fica "—".
  const [medMap, setMedMap] = useState<Record<string, { medPct: number; disputaPct: number }>>({});
  useEffect(() => {
    if (!estabs.length) return;
    medService.getMedIndices(estabs.map((e) => e.id)).then(setMedMap).catch(() => {});
  }, [estabs]);

  // KPIs calculados a partir dos dados carregados
  const kpis = useMemo(() => {
    const by = (s: string) => estabs.filter((e) => e.status === s).length;
    return { total: estabs.length, ativos: by('ativo'), emAnalise: by('analise'), bloqueados: by('bloqueado'), restritos: by('restrito') };
  }, [estabs]);

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
      estabs.filter((e) => {
        const q = query.toLowerCase();
        const matchQuery =
          !q ||
          e.nome.toLowerCase().includes(q) ||
          e.documento.includes(q) ||
          e.id.toLowerCase().includes(q);
        const matchStatus = statusFilter === 'todos' || e.status === statusFilter;
        return matchQuery && matchStatus;
      }),
    [estabs, query, statusFilter]
  );

  return (
    <div>
      <PageHeader
        title="Estabelecimentos"
        subtitle={`${kpis.total.toLocaleString('pt-BR')} cadastrados`}
        action={
          <button onClick={carregar} disabled={loading} className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 transition-colors disabled:opacity-50">
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Atualizar
          </button>
        }
      />

      {/* Cards (calculados dos dados carregados) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Total" value={kpis.total.toLocaleString('pt-BR')} accent="blue" icon={<Building2 size={20} />} />
        <StatCard label="Ativos" value={kpis.ativos.toLocaleString('pt-BR')} accent="emerald" icon={<CheckCircle2 size={20} />} />
        <StatCard label="Em análise" value={kpis.emAnalise.toLocaleString('pt-BR')} accent="amber" icon={<Clock size={20} />} hint="Aguardando compliance" />
        <StatCard label="Bloqueados" value={kpis.bloqueados.toLocaleString('pt-BR')} accent="rose" icon={<Ban size={20} />} hint={`${kpis.restritos} restritos`} />
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

        {loading ? (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <Loader size={26} className="text-blue-500 animate-spin mb-3" />
            <p className="text-sm text-slate-500">Carregando estabelecimentos do backend...</p>
          </div>
        ) : erro ? (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-xl bg-rose-50 flex items-center justify-center text-rose-500 mb-3"><WifiOff size={22} /></div>
            <p className="text-sm font-medium text-slate-700">Não foi possível carregar os estabelecimentos</p>
            <p className="text-xs text-slate-400 mt-1 max-w-md">{erro}</p>
            <button onClick={carregar} className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-blue-700 bg-blue-50 hover:bg-blue-100 px-4 py-2 rounded-lg"><RefreshCw size={15} /> Tentar novamente</button>
          </div>
        ) : (
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
                  <td className="px-5 py-3.5 text-right tabular-nums font-medium text-slate-700">{e.saldoDisponivel != null ? brl(e.saldoDisponivel) : <span className="text-slate-300">—</span>}</td>
                  <td className="px-5 py-3.5 text-right tabular-nums">
                    {(() => {
                      const med = medMap[e.id]?.medPct ?? e.medPct;
                      const disp = medMap[e.id]?.disputaPct ?? e.disputaPct;
                      if (med == null && disp == null) return <span className="text-slate-300">—</span>;
                      return (
                        <span>
                          <span className={med > 3 ? 'text-rose-600 font-medium' : 'text-slate-600'}>{med != null ? pct(med) : '—'}</span>
                          <span className="text-slate-300"> / </span>
                          <span className="text-slate-500">{disp != null ? pct(disp) : '—'}</span>
                        </span>
                      );
                    })()}
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
                  <td colSpan={7} className="px-5 py-16 text-center">
                    <div className="flex flex-col items-center justify-center text-slate-400">
                      <Inbox size={26} className="mb-2 text-slate-300" />
                      <p className="text-sm">{estabs.length === 0 ? 'Nenhum estabelecimento no backend.' : 'Nenhum estabelecimento com os filtros atuais.'}</p>
                    </div>
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
        )}
      </AdminCard>

      {editing && <EditModal estab={editing} onClose={() => setEditing(null)} onSave={handleSaveAcq} />}

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
