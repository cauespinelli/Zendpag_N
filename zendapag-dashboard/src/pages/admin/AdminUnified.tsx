// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Painel Unificado (Gestão de risco).
 * Disputas (chargeback) + MEDs (devolução Pix) numa fila única, com
 * defesa/contestação, e o registro de bloqueios cautelares sobre
 * estabelecimentos de risco (aplicar/liberar). Dados mock, estado local.
 */
import React, { useMemo, useState } from 'react';
import {
  ShieldAlert,
  AlertOctagon,
  Scale,
  Lock,
  QrCode,
  CreditCard,
  Eye,
  ShieldCheck,
  Gavel,
  Search,
  X,
  Clock,
  Unlock,
  Ban,
} from 'lucide-react';
import {
  brl,
  pct,
  riscoItens as riscoSeed,
  painelKpis as kpis,
  bloqueiosCautelares as bloqueiosSeed,
  estabelecimentos,
} from '@/mock/admin';
import { StatCard, AdminCard, StatusBadge, PageHeader, GradientButton } from '@/components/admin/ui';

const statusMeta: Record<string, { tone: any; label: string }> = {
  aberto: { tone: 'warning', label: 'Aberto' },
  em_analise: { tone: 'info', label: 'Em análise' },
  contestado: { tone: 'info', label: 'Contestado' },
  devolvido: { tone: 'danger', label: 'Devolvido' },
  ganha: { tone: 'success', label: 'Ganha' },
  perdida: { tone: 'danger', label: 'Perdida' },
  encerrado: { tone: 'neutral', label: 'Encerrado' },
};

const tipoMeta: Record<string, { icon: any; label: string; cls: string }> = {
  med: { icon: AlertOctagon, label: 'MED', cls: 'text-rose-600 bg-rose-50 border-rose-200' },
  disputa: { icon: Scale, label: 'Disputa', cls: 'text-violet-600 bg-violet-50 border-violet-200' },
};

const metodoIcon: Record<string, any> = { pix: QrCode, cartao: CreditCard };
const abertos = (s: string) => s === 'aberto' || s === 'em_analise' || s === 'contestado';

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const AdminUnified: React.FC = () => {
  const [itens, setItens] = useState(riscoSeed);
  const [bloqueios, setBloqueios] = useState(bloqueiosSeed);
  const [aba, setAba] = useState<'fila' | 'med' | 'disputa' | 'bloqueios'>('fila');
  const [query, setQuery] = useState('');
  const [status, setStatus] = useState('todos');

  const [defesa, setDefesa] = useState<any>(null);
  const [texto, setTexto] = useState('');
  const [bloquear, setBloquear] = useState<{ estabelecimento: string; documento: string } | null>(null);
  const [motivoBloqueio, setMotivoBloqueio] = useState('');
  const [liberar, setLiberar] = useState<any>(null);

  const filaQtd = useMemo(() => itens.filter((i) => abertos(i.status)).length, [itens]);
  const bloqueiosAtivos = useMemo(() => bloqueios.filter((b) => b.status === 'ativo').length, [bloqueios]);

  const filtered = useMemo(
    () =>
      itens.filter((i) => {
        if (aba === 'fila' && !abertos(i.status)) return false;
        if (aba === 'med' && i.tipo !== 'med') return false;
        if (aba === 'disputa' && i.tipo !== 'disputa') return false;
        const q = query.toLowerCase();
        if (q && !i.id.toLowerCase().includes(q) && !i.estabelecimento.toLowerCase().includes(q) && !i.cliente.toLowerCase().includes(q))
          return false;
        if (aba !== 'fila' && status !== 'todos' && i.status !== status) return false;
        return true;
      }),
    [itens, aba, query, status]
  );

  const prazoUrgente = (prazo: string) => prazo <= '2026-06-25';

  const enviarDefesa = () => {
    setItens((prev) => prev.map((i) => (i.id === defesa.id ? { ...i, status: 'contestado' } : i)));
    setDefesa(null);
    setTexto('');
  };

  const aplicarBloqueio = () => {
    if (!bloquear) return;
    const est = estabelecimentos.find((e) => e.nome === bloquear.estabelecimento);
    setBloqueios((prev) => [
      {
        id: `BLQ-${3013 + prev.length}`,
        estabelecimento: bloquear.estabelecimento,
        documento: bloquear.documento,
        motivo: motivoBloqueio || 'Bloqueio cautelar aplicado pelo Admin Master.',
        saldoRetido: est?.saldoDisponivel ?? 0,
        medPct: est?.medPct ?? 0,
        aplicadoEm: '2026-06-23 14:50',
        aplicadoPor: 'Admin Master',
        status: 'ativo',
      },
      ...prev,
    ]);
    setBloquear(null);
    setMotivoBloqueio('');
    setAba('bloqueios');
  };

  const liberarBloqueio = () => {
    setBloqueios((prev) => prev.map((b) => (b.id === liberar.id ? { ...b, status: 'liberado', saldoRetido: 0 } : b)));
    setLiberar(null);
  };

  return (
    <div>
      <PageHeader
        title="Painel Unificado"
        subtitle="Disputas, MEDs e bloqueios cautelares num só lugar"
        action={
          <GradientButton onClick={() => setBloquear({ estabelecimento: '', documento: '' })}>
            <Lock size={16} /> Aplicar bloqueio cautelar
          </GradientButton>
        }
      />

      {/* Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="MEDs abertos" value={brl(kpis.medsValor)} accent="rose" icon={<AlertOctagon size={20} />} hint={`${kpis.medsAbertos} em aberto`} />
        <StatCard label="Disputas abertas" value={brl(kpis.disputasValor)} accent="violet" icon={<Scale size={20} />} hint={`${kpis.disputasAbertas} em aberto`} />
        <StatCard label="Índice de MED" value={pct(kpis.taxaMedPlataforma)} accent="amber" icon={<ShieldAlert size={20} />} hint="Limite saudável: até 3%" />
        <StatCard label="Bloqueios ativos" value={brl(kpis.saldoBloqueado)} accent="blue" icon={<Lock size={20} />} hint={`${bloqueiosAtivos} estabelecimentos retidos`} />
      </div>

      <AdminCard>
        {/* Abas */}
        <div className="flex flex-wrap gap-1 px-4 pt-3 border-b border-slate-100">
          {[
            { key: 'fila', label: 'Fila de risco', badge: filaQtd },
            { key: 'med', label: 'MEDs', badge: 0 },
            { key: 'disputa', label: 'Disputas', badge: 0 },
            { key: 'bloqueios', label: 'Bloqueios cautelares', badge: bloqueiosAtivos },
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
                <span className="text-[11px] font-semibold px-1.5 py-0.5 rounded-full bg-rose-100 text-rose-700">{t.badge}</span>
              )}
            </button>
          ))}
        </div>

        {/* Filtros (ocultos na aba de bloqueios) */}
        {aba !== 'bloqueios' && (
          <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
            <div className="relative flex-1 min-w-[200px]">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
              <input
                value={query}
                onChange={(e) => setQuery(e.target.value)}
                placeholder="Buscar por ID, estabelecimento ou cliente..."
                className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
              />
            </div>
            <select
              value={status}
              onChange={(e) => setStatus(e.target.value)}
              disabled={aba === 'fila'}
              className={`${selectCls} disabled:opacity-40 disabled:cursor-not-allowed`}
            >
              <option value="todos">Todos os status</option>
              <option value="aberto">Aberto</option>
              <option value="em_analise">Em análise</option>
              <option value="contestado">Contestado</option>
              <option value="devolvido">Devolvido</option>
              <option value="ganha">Ganha</option>
              <option value="perdida">Perdida</option>
            </select>
          </div>
        )}

        {/* Tabela: risco (MEDs + disputas) */}
        {aba !== 'bloqueios' ? (
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                  <th className="px-5 py-3 font-medium">Caso</th>
                  <th className="px-5 py-3 font-medium">Tipo</th>
                  <th className="px-5 py-3 font-medium">Motivo</th>
                  <th className="px-5 py-3 font-medium text-right">Valor</th>
                  <th className="px-5 py-3 font-medium">Prazo</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                {filtered.map((i) => {
                  const TipoIcon = tipoMeta[i.tipo].icon;
                  const MetIcon = metodoIcon[i.metodo];
                  return (
                    <tr key={i.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                      <td className="px-5 py-3.5">
                        <p className="font-medium text-slate-700">{i.id}</p>
                        <p className="text-xs text-slate-400">{i.estabelecimento}</p>
                        <p className="text-[11px] text-slate-300">{i.cliente} · {i.txId}</p>
                      </td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex items-center gap-1 text-xs font-medium px-2 py-1 rounded-full border ${tipoMeta[i.tipo].cls}`}>
                          <TipoIcon size={13} /> {tipoMeta[i.tipo].label}
                        </span>
                        <span className="mt-1 flex items-center gap-1 text-[11px] text-slate-400">
                          {MetIcon && <MetIcon size={12} />} {i.adquirente}
                        </span>
                      </td>
                      <td className="px-5 py-3.5 max-w-[220px]">
                        <p className="text-slate-600 text-[13px]">{i.motivo}</p>
                      </td>
                      <td className="px-5 py-3.5 text-right tabular-nums font-semibold text-slate-800">{brl(i.valor)}</td>
                      <td className="px-5 py-3.5">
                        <span className={`inline-flex items-center gap-1 text-xs tabular-nums ${prazoUrgente(i.prazoResposta) && abertos(i.status) ? 'text-rose-600 font-medium' : 'text-slate-500'}`}>
                          <Clock size={13} /> {i.prazoResposta}
                        </span>
                      </td>
                      <td className="px-5 py-3.5">
                        <StatusBadge tone={statusMeta[i.status].tone}>{statusMeta[i.status].label}</StatusBadge>
                      </td>
                      <td className="px-5 py-3.5">
                        <div className="flex items-center justify-end gap-1">
                          {abertos(i.status) ? (
                            <>
                              <button
                                onClick={() => setDefesa(i)}
                                className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-blue-700 bg-blue-50 hover:bg-blue-100 transition-colors"
                                title="Apresentar defesa"
                              >
                                <Gavel size={14} /> Defender
                              </button>
                              <button
                                onClick={() => setBloquear({ estabelecimento: i.estabelecimento, documento: i.documento })}
                                className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-rose-700 bg-rose-50 hover:bg-rose-100 transition-colors"
                                title="Bloquear estabelecimento"
                              >
                                <Lock size={14} /> Bloquear
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
                    <td colSpan={7} className="px-5 py-12 text-center text-slate-400 text-sm">
                      {aba === 'fila' ? 'Nenhum caso aberto na fila de risco. ✓' : 'Nenhum caso encontrado com os filtros atuais.'}
                    </td>
                  </tr>
                )}
              </tbody>
            </table>
          </div>
        ) : (
          /* Tabela: bloqueios cautelares */
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                  <th className="px-5 py-3 font-medium">Bloqueio</th>
                  <th className="px-5 py-3 font-medium">Motivo</th>
                  <th className="px-5 py-3 font-medium text-right">Saldo retido</th>
                  <th className="px-5 py-3 font-medium text-right">MED</th>
                  <th className="px-5 py-3 font-medium">Aplicado</th>
                  <th className="px-5 py-3 font-medium">Status</th>
                  <th className="px-5 py-3 font-medium text-right">Ações</th>
                </tr>
              </thead>
              <tbody>
                {bloqueios.map((b) => (
                  <tr key={b.id} className="border-b border-slate-50 hover:bg-slate-50/60">
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-slate-700">{b.estabelecimento}</p>
                      <p className="text-xs text-slate-400">{b.id} · {b.documento}</p>
                    </td>
                    <td className="px-5 py-3.5 max-w-[280px]">
                      <p className="text-slate-600 text-[13px]">{b.motivo}</p>
                    </td>
                    <td className="px-5 py-3.5 text-right tabular-nums font-semibold text-slate-800">{b.saldoRetido ? brl(b.saldoRetido) : '—'}</td>
                    <td className={`px-5 py-3.5 text-right tabular-nums ${b.medPct > 3 ? 'text-rose-600 font-medium' : 'text-slate-500'}`}>{pct(b.medPct)}</td>
                    <td className="px-5 py-3.5">
                      <p className="text-slate-600 text-[13px] tabular-nums">{b.aplicadoEm}</p>
                      <p className="text-[11px] text-slate-400">por {b.aplicadoPor}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <StatusBadge tone={b.status === 'ativo' ? 'danger' : 'neutral'}>
                        {b.status === 'ativo' ? 'Ativo' : 'Liberado'}
                      </StatusBadge>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center justify-end gap-1">
                        {b.status === 'ativo' ? (
                          <button
                            onClick={() => setLiberar(b)}
                            className="inline-flex items-center gap-1 text-xs font-semibold px-2.5 py-1.5 rounded-lg text-emerald-700 bg-emerald-50 hover:bg-emerald-100 transition-colors"
                            title="Liberar bloqueio"
                          >
                            <Unlock size={14} /> Liberar
                          </button>
                        ) : (
                          <span className="inline-flex items-center gap-1 text-xs text-slate-400 px-2.5 py-1.5">
                            <ShieldCheck size={14} /> Encerrado
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </AdminCard>

      {/* Modal: defesa/contestação */}
      {defesa && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <Gavel size={18} className="text-blue-500" /> Apresentar defesa — {defesa.id}
              </h3>
              <button onClick={() => { setDefesa(null); setTexto(''); }} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-xs text-slate-400">Estabelecimento</p>
                  <p className="font-medium text-slate-700">{defesa.estabelecimento}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Valor em risco</p>
                  <p className="font-semibold text-slate-800 tabular-nums">{brl(defesa.valor)}</p>
                </div>
                <div className="col-span-2">
                  <p className="text-xs text-slate-400">Motivo alegado</p>
                  <p className="font-medium text-slate-700">{defesa.motivo}</p>
                </div>
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Fundamentação da defesa</label>
                <textarea
                  value={texto}
                  onChange={(e) => setTexto(e.target.value)}
                  rows={3}
                  placeholder="Descreva as evidências (comprovante de entrega, logs, contato com o cliente)..."
                  className="w-full px-3 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 resize-none"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => { setDefesa(null); setTexto(''); }} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">
                Cancelar
              </button>
              <button onClick={enviarDefesa} className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-blue-600 hover:bg-blue-700">
                <Gavel size={16} /> Enviar defesa
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: aplicar bloqueio cautelar */}
      {bloquear && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <Lock size={18} className="text-rose-500" /> Aplicar bloqueio cautelar
              </h3>
              <button onClick={() => { setBloquear(null); setMotivoBloqueio(''); }} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Estabelecimento</label>
                {bloquear.documento ? (
                  <div className="rounded-lg bg-slate-50 border border-slate-100 px-3 py-2">
                    <p className="text-sm font-medium text-slate-700">{bloquear.estabelecimento}</p>
                    <p className="text-xs text-slate-400">{bloquear.documento}</p>
                  </div>
                ) : (
                  <select
                    value={bloquear.estabelecimento}
                    onChange={(e) => {
                      const est = estabelecimentos.find((x) => x.nome === e.target.value);
                      setBloquear({ estabelecimento: e.target.value, documento: est?.documento ?? '' });
                    }}
                    className={`${selectCls} w-full`}
                  >
                    <option value="">Selecione um estabelecimento...</option>
                    {estabelecimentos.map((e) => (
                      <option key={e.id} value={e.nome}>{e.nome} · {e.documento}</option>
                    ))}
                  </select>
                )}
              </div>
              <div>
                <label className="block text-xs font-medium text-slate-500 mb-1.5">Motivo do bloqueio</label>
                <textarea
                  value={motivoBloqueio}
                  onChange={(e) => setMotivoBloqueio(e.target.value)}
                  rows={3}
                  placeholder="Ex.: índice de MED acima do limite, suspeita de fraude em série..."
                  className="w-full px-3 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 resize-none"
                />
              </div>
              <div className="rounded-xl bg-rose-50 border border-rose-100 p-4 flex gap-3">
                <Ban size={18} className="text-rose-500 shrink-0 mt-0.5" />
                <p className="text-xs text-rose-700">
                  O bloqueio cautelar <strong>retém o saldo disponível</strong> e suspende saques do estabelecimento durante a apuração. Ele continua transacionando, mas não recebe.
                </p>
              </div>
            </div>
            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => { setBloquear(null); setMotivoBloqueio(''); }} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">
                Cancelar
              </button>
              <button
                onClick={aplicarBloqueio}
                disabled={!bloquear.estabelecimento}
                className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-rose-600 hover:bg-rose-700 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                <Lock size={16} /> Aplicar bloqueio
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: liberar bloqueio */}
      {liberar && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <Unlock size={18} className="text-emerald-500" /> Liberar bloqueio
              </h3>
              <button onClick={() => setLiberar(null)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <p className="text-sm text-slate-600">
                Liberar o bloqueio de <strong className="text-slate-800">{liberar.estabelecimento}</strong> devolve o saldo retido
                {liberar.saldoRetido ? <> de <span className="font-semibold tabular-nums">{brl(liberar.saldoRetido)}</span></> : null} e reabilita os saques.
              </p>
              <div className="rounded-xl bg-emerald-50 border border-emerald-100 p-4 flex gap-3">
                <ShieldCheck size={18} className="text-emerald-500 shrink-0 mt-0.5" />
                <p className="text-xs text-emerald-700">Confirme apenas se a apuração foi concluída sem irregularidade pendente.</p>
              </div>
            </div>
            <div className="flex justify-end gap-2 px-6 py-4 border-t border-slate-100">
              <button onClick={() => setLiberar(null)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">
                Cancelar
              </button>
              <button onClick={liberarBloqueio} className="inline-flex items-center gap-2 px-4 py-2.5 text-sm font-semibold text-white rounded-xl bg-emerald-600 hover:bg-emerald-700">
                <Unlock size={16} /> Confirmar liberação
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminUnified;
