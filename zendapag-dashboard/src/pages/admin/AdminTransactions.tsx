// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Transações (Operação) — CONECTADO AO BACKEND.
 * Lista pagamentos via GET /payments/all (ADMIN), com KPIs calculados e
 * estados loading/vazio/erro. Ações de detalhe/disputa/reenviar webhook
 * abrem modais (disputa/webhook são mock — sem endpoint admin ainda).
 */
import React, { useCallback, useEffect, useMemo, useState } from 'react';
import {
  ArrowDownUp,
  Receipt,
  TrendingDown,
  Wallet,
  QrCode,
  Eye,
  ShieldAlert,
  Webhook,
  AlertTriangle,
  Search,
  X,
  Send,
  CheckCircle2,
  Check,
  Ban,
  RotateCcw,
  RefreshCw,
  Loader,
  Inbox,
  WifiOff,
} from 'lucide-react';
import { adminTransactionService } from '@/services/adminTransactionService';
import { adminHttpError } from '@/services/adminHttp';
import { StatCard, AdminCard, StatusBadge, PageHeader } from '@/components/admin/ui';

const brl = (v: number): string =>
  (Number(v) || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

const statusMeta: Record<string, { tone: any; label: string }> = {
  aprovada: { tone: 'success', label: 'Aprovada' },
  recusada: { tone: 'danger', label: 'Recusada' },
  pendente: { tone: 'warning', label: 'Pendente' },
  estornada: { tone: 'neutral', label: 'Estornada' },
  disputa: { tone: 'info', label: 'Em disputa' },
};

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const AdminTransactions: React.FC = () => {
  const [rows, setRows] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [erro, setErro] = useState<string | null>(null);

  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('todos');

  const [detalhe, setDetalhe] = useState<any>(null);
  const [webhookTx, setWebhookTx] = useState<any>(null);
  const [disputaTx, setDisputaTx] = useState<any>(null);
  const [disputaMotivo, setDisputaMotivo] = useState('');
  const [toast, setToast] = useState<string | null>(null);
  const notify = (m: string) => { setToast(m); setTimeout(() => setToast(null), 3000); };
  const [aprovando, setAprovando] = useState<string | null>(null);

  const aprovar = async (t: any) => {
    setAprovando(t.uuid);
    try {
      await adminTransactionService.approve(t.uuid);
      notify(`Pagamento ${t.id} aprovado — taxa descontada e saldo creditado.`);
      await carregar();
    } catch (e: any) {
      notify('Falha ao aprovar: ' + adminHttpError(e));
    } finally {
      setAprovando(null);
    }
  };

  const recusar = async (t: any) => {
    setAprovando(t.uuid);
    try {
      await adminTransactionService.reject(t.uuid);
      notify(`Pagamento ${t.id} recusado — webhook PAYMENT_FAILED disparado.`);
      await carregar();
    } catch (e: any) {
      notify('Falha ao recusar: ' + adminHttpError(e));
    } finally {
      setAprovando(null);
    }
  };

  const estornar = async (t: any) => {
    setAprovando(t.uuid);
    try {
      await adminTransactionService.refund(t.uuid);
      notify(`Pagamento ${t.id} estornado — saldo revertido e webhook PAYMENT_REFUNDED.`);
      await carregar();
    } catch (e: any) {
      notify('Falha ao estornar: ' + adminHttpError(e));
    } finally {
      setAprovando(null);
    }
  };

  const carregar = useCallback(async () => {
    setLoading(true);
    setErro(null);
    try {
      setRows(await adminTransactionService.listAll());
    } catch (e: any) {
      setErro(adminHttpError(e));
      setRows([]);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { carregar(); }, [carregar]);

  const kpis = useMemo(() => {
    const aprov = rows.filter((r) => r.status === 'aprovada');
    return {
      volumeBruto: rows.reduce((a, r) => a + (r.bruto || 0), 0),
      taxaTotal: rows.reduce((a, r) => a + (r.taxa || 0), 0),
      volumeLiquido: aprov.reduce((a, r) => a + (r.liquido || 0), 0),
      aprovadas: aprov.length,
      ticketMedio: aprov.length ? aprov.reduce((a, r) => a + (r.bruto || 0), 0) / aprov.length : 0,
    };
  }, [rows]);

  const filtered = useMemo(
    () =>
      rows.filter((t) => {
        const q = query.toLowerCase();
        const alvo = `${t.id || ''} ${t.cliente || ''} ${t.documento || ''}`.toLowerCase();
        if (q && !alvo.includes(q)) return false;
        if (status !== 'todos' && t.status !== status) return false;
        return true;
      }),
    [rows, query, status]
  );

  return (
    <div>
      <PageHeader
        title="Transações"
        subtitle="Pagamentos processados pela plataforma"
        action={
          <button onClick={carregar} disabled={loading} className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 transition-colors disabled:opacity-50">
            <RefreshCw size={16} className={loading ? 'animate-spin' : ''} /> Atualizar
          </button>
        }
      />

      {toast && (
        <div className="mb-4 flex items-center gap-2 rounded-xl bg-emerald-50 border border-emerald-200 px-4 py-3 text-sm text-emerald-700">
          <CheckCircle2 size={18} className="text-emerald-500" /><span className="flex-1">{toast}</span>
          <button onClick={() => setToast(null)} className="text-emerald-500 hover:text-emerald-700"><X size={16} /></button>
        </div>
      )}

      {/* Cards (calculados dos dados carregados) */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Volume bruto" value={brl(kpis.volumeBruto)} accent="blue" icon={<ArrowDownUp size={20} />} />
        <StatCard label="Taxas retidas" value={brl(kpis.taxaTotal)} accent="violet" icon={<TrendingDown size={20} />} />
        <StatCard label="Volume líquido (aprovado)" value={brl(kpis.volumeLiquido)} accent="emerald" icon={<Wallet size={20} />} />
        <StatCard label="Transações aprovadas" value={kpis.aprovadas.toLocaleString('pt-BR')} accent="emerald" icon={<Receipt size={20} />} hint={`Ticket médio ${brl(kpis.ticketMedio)}`} />
      </div>

      <AdminCard>
        {/* Filtros */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar por referência, cliente ou CPF..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <select value={status} onChange={(e) => setStatus(e.target.value)} className={selectCls}>
            <option value="todos">Todos os status</option>
            <option value="aprovada">Aprovada</option>
            <option value="recusada">Recusada</option>
            <option value="pendente">Pendente</option>
            <option value="estornada">Estornada</option>
            <option value="disputa">Em disputa</option>
          </select>
        </div>

        {loading ? (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <Loader size={26} className="text-blue-500 animate-spin mb-3" />
            <p className="text-sm text-slate-500">Carregando transações do backend...</p>
          </div>
        ) : erro ? (
          <div className="px-5 py-16 flex flex-col items-center justify-center text-center">
            <div className="w-12 h-12 rounded-xl bg-rose-50 flex items-center justify-center text-rose-500 mb-3"><WifiOff size={22} /></div>
            <p className="text-sm font-medium text-slate-700">Não foi possível carregar as transações</p>
            <p className="text-xs text-slate-400 mt-1 max-w-md">{erro}</p>
            <button onClick={carregar} className="mt-4 inline-flex items-center gap-2 text-sm font-semibold text-blue-700 bg-blue-50 hover:bg-blue-100 px-4 py-2 rounded-lg"><RefreshCw size={15} /> Tentar novamente</button>
          </div>
        ) : (
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
                  <th className="px-5 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((t) => (
                  <tr key={t.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-slate-700">{t.id}</p>
                      <p className="text-xs text-slate-400">{t.cliente} · {t.estabelecimento}</p>
                      <p className="text-[11px] text-slate-300 tabular-nums">{t.criadoEm}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="inline-flex items-center gap-1.5 text-slate-600"><QrCode size={16} className="text-slate-400" /> Pix</span>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-1.5">
                        <StatusBadge tone={statusMeta[t.status]?.tone || 'neutral'}>{statusMeta[t.status]?.label || t.status}</StatusBadge>
                        {t.motivoErro && (
                          <button onClick={() => setDetalhe(t)} className="text-rose-500 hover:text-rose-600" title="Ver motivo"><AlertTriangle size={15} /></button>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{brl(t.bruto)}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-400">{t.taxa ? `- ${brl(t.taxa)}` : '—'}</td>
                    <td className={`px-5 py-3.5 text-right tabular-nums font-semibold ${t.liquido < 0 ? 'text-rose-600' : 'text-slate-800'}`}>{t.liquido ? brl(t.liquido) : '—'}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1">
                        {t.status === 'pendente' && (
                          <>
                            <button
                              onClick={() => aprovar(t)}
                              disabled={aprovando === t.uuid}
                              className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-emerald-700 bg-emerald-50 hover:bg-emerald-100 transition-colors disabled:opacity-50"
                              title="Aprovar pagamento (sandbox)"
                            >
                              {aprovando === t.uuid ? <Loader size={14} className="animate-spin" /> : <Check size={14} />} Aprovar
                            </button>
                            <button
                              onClick={() => recusar(t)}
                              disabled={aprovando === t.uuid}
                              className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-rose-700 bg-rose-50 hover:bg-rose-100 transition-colors disabled:opacity-50"
                              title="Recusar pagamento (dispara PAYMENT_FAILED)"
                            >
                              <Ban size={14} /> Recusar
                            </button>
                          </>
                        )}
                        {t.status === 'aprovada' && (
                          <button
                            onClick={() => estornar(t)}
                            disabled={aprovando === t.uuid}
                            className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-amber-700 bg-amber-50 hover:bg-amber-100 transition-colors disabled:opacity-50"
                            title="Estornar pagamento (reverte saldo, dispara PAYMENT_REFUNDED)"
                          >
                            {aprovando === t.uuid ? <Loader size={14} className="animate-spin" /> : <RotateCcw size={14} />} Estornar
                          </button>
                        )}
                        <button onClick={() => setDetalhe(t)} className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Ver detalhes"><Eye size={16} /></button>
                        <button onClick={() => { setDisputaTx(t); setDisputaMotivo(''); }} className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Abrir disputa"><ShieldAlert size={16} /></button>
                        <button onClick={() => setWebhookTx(t)} className="w-8 h-8 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500" title="Reenviar webhook"><Webhook size={16} /></button>
                      </div>
                    </td>
                  </tr>
                ))}
                {filtered.length === 0 && (
                  <tr>
                    <td colSpan={7} className="px-5 py-16 text-center">
                      <div className="flex flex-col items-center justify-center text-slate-400">
                        <Inbox size={26} className="mb-2 text-slate-300" />
                        <p className="text-sm">{rows.length === 0 ? 'Nenhuma transação no backend.' : 'Nenhuma transação com os filtros atuais.'}</p>
                      </div>
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        )}
      </AdminCard>

      {/* Modal: detalhes */}
      {detalhe && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-lg max-h-[90vh] flex flex-col">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2"><Eye size={18} className="text-blue-500" /> Detalhes da transação</h3>
              <button onClick={() => setDetalhe(null)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500"><X size={18} /></button>
            </div>
            <div className="p-6 overflow-y-auto space-y-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="font-semibold text-slate-800">{detalhe.id}</p>
                  <p className="text-xs text-slate-400 tabular-nums">{detalhe.criadoEm}</p>
                </div>
                <StatusBadge tone={statusMeta[detalhe.status]?.tone || 'neutral'}>{statusMeta[detalhe.status]?.label || detalhe.status}</StatusBadge>
              </div>
              <div className="grid grid-cols-2 gap-x-4 gap-y-3 text-sm">
                <div><p className="text-xs text-slate-400">Pagador (nome)</p><p className="font-medium text-slate-700">{detalhe.cliente}</p></div>
                <div><p className="text-xs text-slate-400">CPF</p><p className="font-medium text-slate-700 tabular-nums">{detalhe.documento}</p></div>
                <div><p className="text-xs text-slate-400">Método</p><p className="font-medium text-slate-700 inline-flex items-center gap-1.5"><QrCode size={15} className="text-slate-400" /> Pix</p></div>
                <div><p className="text-xs text-slate-400">Status</p><div className="mt-0.5"><StatusBadge tone={statusMeta[detalhe.status]?.tone || 'neutral'}>{statusMeta[detalhe.status]?.label || detalhe.status}</StatusBadge></div></div>
                <div><p className="text-xs text-slate-400">Data</p><p className="font-medium text-slate-700 tabular-nums">{detalhe.criadoEm}</p></div>
                <div><p className="text-xs text-slate-400">Estabelecimento</p><p className="font-medium text-slate-700">{detalhe.estabelecimento}</p></div>
              </div>
              <div className="grid grid-cols-3 gap-3 pt-1">
                <div className="rounded-xl bg-slate-50 border border-slate-100 p-3"><p className="text-xs text-slate-400">Bruto</p><p className="font-semibold text-slate-800 tabular-nums mt-0.5">{brl(detalhe.bruto)}</p></div>
                <div className="rounded-xl bg-slate-50 border border-slate-100 p-3"><p className="text-xs text-slate-400">Taxa</p><p className="font-semibold text-slate-500 tabular-nums mt-0.5">{detalhe.taxa ? `- ${brl(detalhe.taxa)}` : '—'}</p></div>
                <div className="rounded-xl bg-slate-50 border border-slate-100 p-3"><p className="text-xs text-slate-400">Líquido</p><p className={`font-semibold tabular-nums mt-0.5 ${detalhe.liquido < 0 ? 'text-rose-600' : 'text-slate-800'}`}>{detalhe.liquido ? brl(detalhe.liquido) : '—'}</p></div>
              </div>
              {detalhe.motivoErro && (
                <div className="rounded-xl bg-rose-50 border border-rose-100 p-3 flex gap-2">
                  <AlertTriangle size={16} className="text-rose-500 shrink-0 mt-0.5" />
                  <p className="text-sm font-medium text-rose-700">{detalhe.motivoErro}</p>
                </div>
              )}
            </div>
            <div className="flex items-center justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => { setWebhookTx(detalhe); setDetalhe(null); }} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100 inline-flex items-center gap-2"><Webhook size={15} /> Reenviar webhook</button>
              <button onClick={() => setDetalhe(null)} className="px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-700 hover:to-emerald-600">Fechar</button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: abrir disputa (mock) */}
      {disputaTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2"><ShieldAlert size={18} className="text-violet-500" /> Abrir disputa</h3>
              <button onClick={() => setDisputaTx(null)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500"><X size={18} /></button>
            </div>
            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div><p className="text-xs text-slate-400">Transação</p><p className="font-medium text-slate-700">{disputaTx.id}</p></div>
                <div><p className="text-xs text-slate-400">Valor</p><p className="font-semibold text-slate-800 tabular-nums">{brl(disputaTx.bruto)}</p></div>
                <div><p className="text-xs text-slate-400">Cliente</p><p className="font-medium text-slate-700">{disputaTx.cliente}</p></div>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Motivo da disputa</label>
                <textarea value={disputaMotivo} onChange={(e) => setDisputaMotivo(e.target.value)} rows={3} placeholder="Descreva o motivo..." className="w-full px-3 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 resize-none" />
              </div>
            </div>
            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => setDisputaTx(null)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">Cancelar</button>
              <button onClick={() => { const id = disputaTx.id; setDisputaTx(null); notify(`Disputa aberta para ${id}.`); }} className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-violet-600 hover:bg-violet-700"><ShieldAlert size={16} /> Abrir disputa</button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: reenviar webhook (mock) */}
      {webhookTx && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2"><Webhook size={18} className="text-blue-500" /> Reenviar webhook</h3>
              <button onClick={() => setWebhookTx(null)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500"><X size={18} /></button>
            </div>
            <div className="p-6 space-y-4">
              <p className="text-sm text-slate-600">Payload do evento que será reenviado:</p>
              <pre className="text-xs bg-slate-900 text-slate-100 rounded-xl p-4 overflow-x-auto leading-relaxed">{JSON.stringify({
                event: 'payment.updated', id: webhookTx.id, status: webhookTx.statusRaw || webhookTx.status,
                amount: webhookTx.bruto, fee: webhookTx.taxa, net: webhookTx.liquido, method: 'pix', created_at: webhookTx.criadoEm,
              }, null, 2)}</pre>
            </div>
            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => setWebhookTx(null)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">Cancelar</button>
              <button onClick={() => { const id = webhookTx.id; setWebhookTx(null); notify(`Webhook reenviado para ${id}.`); }} className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-700 hover:to-emerald-600"><Send size={16} /> Reenviar agora</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminTransactions;
