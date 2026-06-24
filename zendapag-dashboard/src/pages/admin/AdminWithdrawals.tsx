// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Saques (Operação) — CONECTADO AO BACKEND REAL.
 * Lista saques via GET /withdrawals/status/{status} (consolidando os status),
 * aprova via POST /{id}/approve e recusa via POST /{id}/cancel. KPIs são
 * calculados a partir dos dados carregados. Estados de loading / vazio / erro.
 */
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  Banknote,
  Clock,
  Loader,
  Wallet,
  QrCode,
  Eye,
  Check,
  X,
  Search,
  ShieldAlert,
  AlertTriangle,
  Copy,
  RefreshCw,
  Inbox,
  WifiOff,
} from 'lucide-react';
import { adminWithdrawalService } from '@/services/adminWithdrawalService';
import { StatCard, AdminCard, StatusBadge, PageHeader } from '@/components/admin/ui';

// ── helpers locais (tela desacoplada do mock) ──
const brl = (v: number): string =>
  (Number(v) || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

const fmt = (s?: string): string => {
  if (!s) return '—';
  const d = new Date(s);
  if (isNaN(d.getTime())) return String(s);
  const p = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}`;
};

const chaveLabel = (t?: string): string =>
  ({ CPF: 'CPF', CNPJ: 'CNPJ', EMAIL: 'E-mail', PHONE: 'Telefone', RANDOM: 'Chave aleatória', EVP: 'Chave aleatória' }[t] || t || 'Chave Pix');

// status do backend (WithdrawalStatus) -> apresentação
const statusMeta: Record<string, { tone: any; label: string }> = {
  PENDING: { tone: 'warning', label: 'Pendente' },
  PROCESSING: { tone: 'info', label: 'Processando' },
  APPROVED: { tone: 'info', label: 'Aprovado' },
  COMPLETED: { tone: 'success', label: 'Concluído' },
  REJECTED: { tone: 'danger', label: 'Recusado' },
  CANCELLED: { tone: 'neutral', label: 'Cancelado' },
  FAILED: { tone: 'danger', label: 'Falhou' },
  REVERSED: { tone: 'neutral', label: 'Estornado' },
};
const isPendente = (s: string) => s === 'PENDING';

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const AdminWithdrawals: React.FC = () => {
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const [aba, setAba] = useState<'aprovacao' | 'todos'>('aprovacao');
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('todos');

  const [acao, setAcao] = useState<{ tipo: 'aprovar' | 'recusar'; saque: any } | null>(null);
  const [motivo, setMotivo] = useState('');
  const [enviando, setEnviando] = useState(false);
  const [acaoErro, setAcaoErro] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro(null);
    try {
      const data = await adminWithdrawalService.listAll();
      setRows(data);
    } catch (e: any) {
      const sc = e?.response?.status;
      if (sc === 401) setErro('Sessão não autorizada (401). O painel admin ainda não tem login próprio — é necessário um token com perfil ADMIN.');
      else if (sc === 403) setErro('Acesso negado (403). O usuário atual não tem perfil ADMIN.');
      else if (!e?.response) setErro('Não foi possível alcançar a API. Verifique se o backend está no ar (porta 8093).');
      else setErro(`Erro ao carregar saques (HTTP ${sc}).`);
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  const pendentesQtd = useMemo(() => rows.filter((s) => isPendente(s.status)).length, [rows]);

  const kpis = useMemo(() => {
    const sum = (arr: any[]) => arr.reduce((acc, r) => acc + (Number(r.amount) || 0), 0);
    const hojeStr = new Date().toISOString().slice(0, 10);
    const pend = rows.filter((r) => r.status === 'PENDING');
    const proc = rows.filter((r) => r.status === 'PROCESSING');
    const conc = rows.filter((r) => r.status === 'COMPLETED');
    const hoje = rows.filter((r) => (r.requestedAt || r.createdAt || '').slice(0, 10) === hojeStr);
    return {
      solicitadoHoje: sum(hoje),
      pendentesValor: sum(pend),
      processandoQtd: proc.length,
      processandoValor: sum(proc),
      concluidoValor: sum(conc),
    };
  }, [rows]);

  const filtered = useMemo(
    () =>
      rows.filter((s) => {
        if (aba === 'aprovacao' && !isPendente(s.status)) return false;
        const q = query.toLowerCase();
        const alvo = `${s.referenceId || ''} ${s.recipientName || ''} ${s.recipientDocument || ''} ${s.id || ''}`.toLowerCase();
        if (q && !alvo.includes(q)) return false;
        if (aba === 'todos' && status !== 'todos' && s.status !== status) return false;
        return true;
      }),
    [rows, aba, query, status]
  );

  const confirmar = async () => {
    if (!acao) return;
    setEnviando(true);
    setAcaoErro(null);
    try {
      const resp =
        acao.tipo === 'aprovar'
          ? await adminWithdrawalService.approve(acao.saque.id)
          : await adminWithdrawalService.reject(acao.saque.id, motivo || 'Recusado pelo Admin Master.');
      // atualiza a linha com a resposta do backend (status/motivo novos)
      setRows((prev) => prev.map((s) => (s.id === acao.saque.id ? { ...s, ...resp } : s)));
      setToast(acao.tipo === 'aprovar' ? `Saque ${acao.saque.referenceId || ''} aprovado.` : `Saque ${acao.saque.referenceId || ''} recusado.`);
      setAcao(null);
      setMotivo('');
    } catch (e: any) {
      const sc = e?.response?.status;
      const msg = e?.response?.data?.message;
      setAcaoErro(msg || (sc ? `Falha na operação (HTTP ${sc}).` : 'Não foi possível alcançar a API.'));
    } finally {
      setEnviando(false);
    }
  };

  return (
    <div>
      <PageHeader
        title="Saques"
        subtitle="Fila de aprovação e acompanhamento de saques dos estabelecimentos"
        action={
          <button
            onClick={carregar}
            disabled={loading}
            className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 transition-colors disabled:opacity-50"
          >
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Atualizar
          </button>
        }
      />

      {/* Toast de sucesso */}
      {toast && (
        <div className="mb-4 flex items-center gap-2 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-700">
          <Check size={18} className="text-emerald-500" />
          <span className="flex-1">{toast}</span>
          <button onClick={() => setToast(null)} className="text-emerald-500 hover:text-emerald-700"><X size={16} /></button>
        </div>
      )}

      {/* Cards (calculados dos dados carregados) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Solicitado hoje" value={brl(kpis.solicitadoHoje)} accent="blue" icon={<Banknote size={20} />} />
        <StatCard label="Aguardando aprovação" value={brl(kpis.pendentesValor)} accent="amber" icon={<Clock size={20} />} hint={`${pendentesQtd} ${pendentesQtd === 1 ? 'solicitação' : 'solicitações'}`} />
        <StatCard label="Em processamento" value={brl(kpis.processandoValor)} accent="violet" icon={<Loader size={20} />} hint={`${kpis.processandoQtd} em trânsito`} />
        <StatCard label="Concluído (carregado)" value={brl(kpis.concluidoValor)} accent="emerald" icon={<Wallet size={20} />} />
      </div>

      <AdminCard>
        {/* Abas */}
        <div className="flex gap-1 px-4 pt-3 border-b border-slate-100">
          {[
            { key: 'aprovacao', label: 'Aguardando aprovação', badge: pendentesQtd },
            { key: 'todos', label: 'Todos', badge: 0 },
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

        {/* Filtros */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar por referência, destinatário ou documento..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            disabled={aba === 'aprovacao'}
            className={`${selectCls} disabled:opacity-40 disabled:cursor-not-allowed`}
          >
            <option value="todos">Todos os status</option>
            <option value="PENDING">Pendente</option>
            <option value="PROCESSING">Processando</option>
            <option value="APPROVED">Aprovado</option>
            <option value="COMPLETED">Concluído</option>
            <option value="REJECTED">Recusado</option>
            <option value="CANCELLED">Cancelado</option>
            <option value="FAILED">Falhou</option>
            <option value="REVERSED">Estornado</option>
          </select>
        </div>

        {/* Conteúdo: loading / erro / vazio / tabela */}
        {loading ? (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <Loader size={26} className="text-blue-500 animate-spin mb-3" />
            <p className="text-sm text-slate-500">Carregando saques do backend...</p>
          </div>
        ) : erro ? (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-xl bg-rose-50 flex items-center justify-center text-rose-500 mb-3">
              <WifiOff size={22} />
            </div>
            <p className="text-sm font-medium text-slate-700">Não foi possível carregar os saques</p>
            <p className="text-xs text-slate-400 mt-1 max-w-md">{erro}</p>
            <button onClick={carregar} className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-blue-700 bg-blue-50 hover:bg-blue-100 px-4 py-2 rounded-lg">
              <RefreshCw size={15} /> Tentar novamente
            </button>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                  <th className="px-5 py-3 font-medium">Saque</th>
                  <th className="px-5 py-3 font-medium">Método</th>
                  <th className="px-5 py-3 font-medium">Destino</th>
                  <th className="px-5 py-3 font-medium text-right">Bruto</th>
                  <th className="px-5 py-3 font-medium text-right">Taxa</th>
                  <th className="px-5 py-3 font-medium text-right">Líquido</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((s) => (
                  <tr key={s.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-slate-700">{s.referenceId || s.id}</p>
                      <p className="text-xs text-slate-400">{s.recipientName || '—'}</p>
                      <p className="text-[11px] text-slate-300 tabular-nums">{fmt(s.requestedAt || s.createdAt)}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="inline-flex items-center gap-1.5 text-slate-600">
                        <QrCode size={16} className="text-slate-400" /> Pix
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <p className="text-xs text-slate-400">{chaveLabel(s.pixKeyType)}</p>
                      <p className="text-slate-600 truncate max-w-[200px]" title={s.pixKey}>{s.pixKey || '—'}</p>
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{brl(s.amount)}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-400">{Number(s.feeAmount) ? `- ${brl(s.feeAmount)}` : 'Grátis'}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums font-semibold text-slate-800">{brl(s.netAmount ?? s.amount)}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-1.5">
                        <StatusBadge tone={statusMeta[s.status]?.tone || 'neutral'}>{statusMeta[s.status]?.label || s.status}</StatusBadge>
                        {s.rejectionReason && (
                          <span title={s.rejectionReason} className="text-rose-500"><AlertTriangle size={15} /></span>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1">
                        {isPendente(s.status) ? (
                          <>
                            <button
                              onClick={() => { setAcao({ tipo: 'aprovar', saque: s }); setAcaoErro(null); }}
                              className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-emerald-700 bg-emerald-50 hover:bg-emerald-100 transition-colors"
                              title="Aprovar saque"
                            >
                              <Check size={14} /> Aprovar
                            </button>
                            <button
                              onClick={() => { setAcao({ tipo: 'recusar', saque: s }); setMotivo(''); setAcaoErro(null); }}
                              className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-rose-700 bg-rose-50 hover:bg-rose-100 transition-colors"
                              title="Recusar saque"
                            >
                              <X size={14} /> Recusar
                            </button>
                          </>
                        ) : (
                          <button className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Ver detalhes">
                            <Eye size={16} />
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={8} className="px-5 py-16 text-center">
                      <div className="flex flex-col items-center justify-center text-slate-400">
                        <Inbox size={26} className="mb-2 text-slate-300" />
                        <p className="text-sm">
                          {aba === 'aprovacao'
                            ? 'Nenhum saque aguardando aprovação.'
                            : rows.length === 0
                              ? 'Nenhum saque encontrado no backend.'
                              : 'Nenhum saque encontrado com os filtros atuais.'}
                        </p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </AdminCard>

      {/* Modal: aprovar / recusar */}
      {acao && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                {acao.tipo === 'aprovar' ? (
                  <><Check size={18} className="text-emerald-500" /> Aprovar saque</>
                ) : (
                  <><ShieldAlert size={18} className="text-rose-500" /> Recusar saque</>
                )}
              </h3>
              <button onClick={() => { setAcao(null); setMotivo(''); }} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" disabled={enviando}>
                <X size={18} />
              </button>
            </div>

            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-xs text-slate-400">Saque</p>
                  <p className="font-medium text-slate-700">{acao.saque.referenceId || acao.saque.id}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Destinatário</p>
                  <p className="font-medium text-slate-700">{acao.saque.recipientName || '—'}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Método</p>
                  <p className="font-medium text-slate-700">Pix</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Valor líquido</p>
                  <p className="font-semibold text-slate-800 tabular-nums">{brl(acao.saque.netAmount ?? acao.saque.amount)}</p>
                </div>
              </div>

              <div className="rounded-xl bg-slate-50 border border-slate-100 p-3">
                <p className="text-xs text-slate-400 mb-0.5">{chaveLabel(acao.saque.pixKeyType)} de destino</p>
                <div className="flex items-center justify-between gap-2">
                  <p className="text-sm font-medium text-slate-700 truncate">{acao.saque.pixKey || '—'}</p>
                  <Copy size={15} className="text-slate-400 shrink-0" />
                </div>
              </div>

              {acao.tipo === 'aprovar' ? (
                <div className="rounded-xl bg-amber-50 border border-amber-100 p-4 flex gap-3">
                  <AlertTriangle size={18} className="text-amber-500 shrink-0 mt-0.5" />
                  <p className="text-xs text-amber-700">
                    Ao aprovar, o saque vai para processamento e é <strong>irreversível</strong>. Confira a chave de destino antes de confirmar.
                  </p>
                </div>
              ) : (
                <div>
                  <label className="block text-xs font-medium text-slate-500 mb-1.5">Motivo da recusa</label>
                  <textarea
                    value={motivo}
                    onChange={(e) => setMotivo(e.target.value)}
                    rows={3}
                    placeholder="Descreva o motivo (visível para o estabelecimento)..."
                    className="w-full px-3 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 resize-none"
                  />
                </div>
              )}

              {acaoErro && (
                <div className="rounded-xl bg-rose-50 border border-rose-200 p-3 text-xs text-rose-700 flex gap-2">
                  <AlertTriangle size={15} className="shrink-0 mt-0.5" /> {acaoErro}
                </div>
              )}
            </div>

            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => { setAcao(null); setMotivo(''); }} disabled={enviando} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100 disabled:opacity-50">
                Cancelar
              </button>
              <button
                onClick={confirmar}
                disabled={enviando}
                className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl transition-colors disabled:opacity-60 ${
                  acao.tipo === 'aprovar' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'
                }`}
              >
                {enviando ? <Loader size={16} className="animate-spin" /> : acao.tipo === 'aprovar' ? <Check size={16} /> : <X size={16} />}
                {enviando ? 'Enviando...' : acao.tipo === 'aprovar' ? 'Confirmar aprovação' : 'Confirmar recusa'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminWithdrawals;
