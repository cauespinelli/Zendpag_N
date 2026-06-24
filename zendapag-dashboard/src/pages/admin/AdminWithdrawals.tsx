// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Saques (Operação).
 * Fila de aprovação de saques de todos os estabelecimentos: cards,
 * abas (Aguardando aprovação / Todos), filtros (método/status/busca),
 * tabela (bruto/taxa/líquido, chave de destino, status) e ações de
 * aprovar / recusar com confirmação. Dados mock, interatividade local.
 */
import React, { useMemo, useState } from 'react';
import {
  Banknote,
  Clock,
  Loader,
  Wallet,
  QrCode,
  Landmark,
  Bitcoin,
  Eye,
  Check,
  X,
  Search,
  ShieldAlert,
  AlertTriangle,
  Copy,
} from 'lucide-react';
import {
  brl,
  saques as saquesSeed,
  saquesKpis as kpis,
  chaveTipoLabel,
} from '@/mock/admin';
import { StatCard, AdminCard, StatusBadge, PageHeader } from '@/components/admin/ui';

const statusMeta: Record<string, { tone: any; label: string }> = {
  pendente: { tone: 'warning', label: 'Pendente' },
  aprovado: { tone: 'info', label: 'Aprovado' },
  processando: { tone: 'info', label: 'Processando' },
  concluido: { tone: 'success', label: 'Concluído' },
  recusado: { tone: 'danger', label: 'Recusado' },
};

const metodoMeta: Record<string, { icon: any; label: string }> = {
  pix: { icon: QrCode, label: 'Pix' },
  ted: { icon: Landmark, label: 'TED' },
  cripto: { icon: Bitcoin, label: 'Cripto' },
};

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const AdminWithdrawals: React.FC = () => {
  const [rows, setRows] = useState(saquesSeed);
  const [aba, setAba] = useState<'aprovacao' | 'todos'>('aprovacao');
  const [query, setQuery] = useState('');
  const [metodo, setMetodo] = useState('todos');
  const [status, setStatus] = useState('todos');
  const [acao, setAcao] = useState<{ tipo: 'aprovar' | 'recusar'; saque: any } | null>(null);
  const [motivo, setMotivo] = useState('');

  const pendentesQtd = useMemo(() => rows.filter((s) => s.status === 'pendente').length, [rows]);

  const filtered = useMemo(
    () =>
      rows.filter((s) => {
        if (aba === 'aprovacao' && s.status !== 'pendente') return false;
        const q = query.toLowerCase();
        if (q && !s.id.toLowerCase().includes(q) && !s.estabelecimento.toLowerCase().includes(q) && !s.documento.toLowerCase().includes(q))
          return false;
        if (metodo !== 'todos' && s.metodo !== metodo) return false;
        if (aba === 'todos' && status !== 'todos' && s.status !== status) return false;
        return true;
      }),
    [rows, aba, query, metodo, status]
  );

  const confirmar = () => {
    if (!acao) return;
    setRows((prev) =>
      prev.map((s) =>
        s.id === acao.saque.id
          ? {
              ...s,
              status: acao.tipo === 'aprovar' ? 'processando' : 'recusado',
              motivoRecusa: acao.tipo === 'recusar' ? motivo || 'Recusado pelo Admin Master.' : null,
            }
          : s
      )
    );
    setAcao(null);
    setMotivo('');
  };

  return (
    <div>
      <PageHeader
        title="Saques"
        subtitle="Fila de aprovação e acompanhamento de saques dos estabelecimentos"
      />

      {/* Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Solicitado hoje" value={brl(kpis.solicitadoHoje)} accent="blue" icon={<Banknote size={20} />} />
        <StatCard
          label="Aguardando aprovação"
          value={brl(kpis.pendentesValor)}
          accent="amber"
          icon={<Clock size={20} />}
          hint={`${pendentesQtd} ${pendentesQtd === 1 ? 'solicitação' : 'solicitações'}`}
        />
        <StatCard
          label="Em processamento"
          value={brl(kpis.processandoValor)}
          accent="violet"
          icon={<Loader size={20} />}
          hint={`${kpis.processandoQtd} em trânsito`}
        />
        <StatCard
          label="Concluído no mês"
          value={brl(kpis.concluidoMes)}
          accent="emerald"
          icon={<Wallet size={20} />}
          hint={`Taxas: ${brl(kpis.taxasArrecadadas)}`}
        />
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
                <span className="text-[11px] font-semibold px-1.5 py-0.5 rounded-full bg-amber-100 text-amber-700">
                  {t.badge}
                </span>
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
              placeholder="Buscar por ID, estabelecimento ou documento..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <select value={metodo} onChange={(e) => setMetodo(e.target.value)} className={selectCls}>
            <option value="todos">Todos os métodos</option>
            <option value="pix">Pix</option>
            <option value="ted">TED</option>
            <option value="cripto">Cripto</option>
          </select>
          <select
            value={status}
            onChange={(e) => setStatus(e.target.value)}
            disabled={aba === 'aprovacao'}
            className={`${selectCls} disabled:opacity-40 disabled:cursor-not-allowed`}
          >
            <option value="todos">Todos os status</option>
            <option value="pendente">Pendente</option>
            <option value="processando">Processando</option>
            <option value="concluido">Concluído</option>
            <option value="recusado">Recusado</option>
          </select>
        </div>

        {/* Tabela */}
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
              {filtered.map((s) => {
                const MetIcon = metodoMeta[s.metodo].icon;
                return (
                  <tr key={s.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-slate-700">{s.id}</p>
                      <p className="text-xs text-slate-400">{s.estabelecimento}</p>
                      <p className="text-[11px] text-slate-300 tabular-nums">{s.solicitadoEm} · {s.prazo}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="inline-flex items-center gap-1.5 text-slate-600">
                        <MetIcon size={16} className="text-slate-400" />
                        {metodoMeta[s.metodo].label}
                      </span>
                    </td>
                    <td className="px-5 py-3.5">
                      <p className="text-xs text-slate-400">{chaveTipoLabel(s.chaveTipo)}</p>
                      <p className="text-slate-600 truncate max-w-[200px]" title={s.chave}>{s.chave}</p>
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-700">{brl(s.bruto)}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums text-slate-400">{s.taxa ? `- ${brl(s.taxa)}` : 'Grátis'}</td>
                    <td className="px-5 py-3.5 text-right tabular-nums font-semibold text-slate-800">{brl(s.liquido)}</td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-1.5">
                        <StatusBadge tone={statusMeta[s.status].tone}>{statusMeta[s.status].label}</StatusBadge>
                        {s.motivoRecusa && (
                          <span title={s.motivoRecusa} className="text-rose-500">
                            <AlertTriangle size={15} />
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1">
                        {s.status === 'pendente' ? (
                          <>
                            <button
                              onClick={() => setAcao({ tipo: 'aprovar', saque: s })}
                              className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-emerald-700 bg-emerald-50 hover:bg-emerald-100 transition-colors"
                              title="Aprovar saque"
                            >
                              <Check size={14} /> Aprovar
                            </button>
                            <button
                              onClick={() => setAcao({ tipo: 'recusar', saque: s })}
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
                );
              })}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={8} className="px-5 py-12 text-center text-slate-400 text-sm">
                    {aba === 'aprovacao'
                      ? 'Nenhum saque aguardando aprovação. Tudo em dia. ✓'
                      : 'Nenhum saque encontrado com os filtros atuais.'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
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
              <button onClick={() => { setAcao(null); setMotivo(''); }} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>

            <div className="p-6 space-y-4">
              {/* Resumo */}
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-xs text-slate-400">Saque</p>
                  <p className="font-medium text-slate-700">{acao.saque.id}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Estabelecimento</p>
                  <p className="font-medium text-slate-700">{acao.saque.estabelecimento}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Método · prazo</p>
                  <p className="font-medium text-slate-700">{metodoMeta[acao.saque.metodo].label} · {acao.saque.prazo}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Valor líquido</p>
                  <p className="font-semibold text-slate-800 tabular-nums">{brl(acao.saque.liquido)}</p>
                </div>
              </div>

              {/* Chave de destino */}
              <div className="rounded-xl bg-slate-50 border border-slate-100 p-3">
                <p className="text-xs text-slate-400 mb-0.5">{chaveTipoLabel(acao.saque.chaveTipo)} de destino</p>
                <div className="flex items-center justify-between gap-2">
                  <p className="text-sm font-medium text-slate-700 truncate">{acao.saque.chave}</p>
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
            </div>

            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => { setAcao(null); setMotivo(''); }} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">
                Cancelar
              </button>
              <button
                onClick={confirmar}
                className={`inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl transition-colors ${
                  acao.tipo === 'aprovar' ? 'bg-emerald-600 hover:bg-emerald-700' : 'bg-rose-600 hover:bg-rose-700'
                }`}
              >
                {acao.tipo === 'aprovar' ? <><Check size={16} /> Confirmar aprovação</> : <><X size={16} /> Confirmar recusa</>}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminWithdrawals;
