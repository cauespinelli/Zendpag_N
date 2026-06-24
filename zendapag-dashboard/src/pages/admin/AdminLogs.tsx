// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Logs (Gestão).
 * Trilha de auditoria: KPIs, filtros (severidade/módulo/busca), feed de
 * eventos com ator, ação, alvo e severidade, e modal de detalhe com
 * IP/dispositivo. Dados mock.
 */
import React, { useMemo, useState } from 'react';
import {
  ScrollText,
  ShieldAlert,
  Users,
  ShieldX,
  Search,
  Download,
  X,
  Monitor,
  Globe,
  Clock,
  Bot,
} from 'lucide-react';
import { logs as logsSeed, logsKpis as kpis } from '@/mock/admin';
import { StatCard, AdminCard, PageHeader } from '@/components/admin/ui';

const sevMeta: Record<string, { label: string; dot: string; chip: string }> = {
  info: { label: 'Info', dot: 'bg-slate-400', chip: 'text-slate-600 bg-slate-100' },
  aviso: { label: 'Aviso', dot: 'bg-amber-500', chip: 'text-amber-700 bg-amber-50' },
  critico: { label: 'Crítico', dot: 'bg-rose-500', chip: 'text-rose-700 bg-rose-50' },
};

const atorMeta: Record<string, { cls: string }> = {
  admin: { cls: 'from-blue-600 to-emerald-500' },
  gerente: { cls: 'from-violet-500 to-violet-600' },
  sistema: { cls: 'from-slate-400 to-slate-500' },
};

const modulos = ['Saques', 'Risco', 'Estabelecimentos', 'Ações em Massa', 'Afiliados', 'Gerentes', 'Acesso', 'Autenticação', 'Financeiro'];

const selectCls =
  'px-3 py-2 text-sm rounded-lg bg-white border border-slate-200 focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 text-slate-600';

const iniciais = (nome: string) =>
  nome === 'desconhecido' ? '?' : nome.split(' ').map((n) => n[0]).slice(0, 2).join('').toUpperCase();

const AdminLogs: React.FC = () => {
  const [query, setQuery] = useState('');
  const [sev, setSev] = useState('todas');
  const [modulo, setModulo] = useState('todos');
  const [detalhe, setDetalhe] = useState<any>(null);

  const filtered = useMemo(
    () =>
      logsSeed.filter((l) => {
        const q = query.toLowerCase();
        if (q && !l.ator.toLowerCase().includes(q) && !l.acao.toLowerCase().includes(q) && !l.alvo.toLowerCase().includes(q) && !l.id.toLowerCase().includes(q))
          return false;
        if (sev !== 'todas' && l.severidade !== sev) return false;
        if (modulo !== 'todos' && l.modulo !== modulo) return false;
        return true;
      }),
    [query, sev, modulo]
  );

  return (
    <div>
      <PageHeader
        title="Logs"
        subtitle="Trilha de auditoria de ações do painel e do sistema"
        action={
          <button className="inline-flex items-center gap-2 text-sm font-medium text-slate-600 px-4 py-2.5 rounded-xl border border-slate-200 bg-white hover:bg-slate-50 transition-colors">
            <Download size={16} /> Exportar logs
          </button>
        }
      />

      {/* KPIs */}
      <div className="grid grid-cols-1 sm:grid-cols-2 xl:grid-cols-4 gap-4 mb-6">
        <StatCard label="Eventos hoje" value={kpis.eventosHoje.toLocaleString('pt-BR')} accent="blue" icon={<ScrollText size={20} />} />
        <StatCard label="Ações sensíveis" value={String(kpis.acoesSensiveis)} accent="rose" icon={<ShieldAlert size={20} />} hint="Severidade crítica" />
        <StatCard label="Usuários ativos" value={String(kpis.usuariosAtivos)} accent="emerald" icon={<Users size={20} />} />
        <StatCard label="Falhas de acesso" value={String(kpis.falhasAcesso)} accent="amber" icon={<ShieldX size={20} />} hint="Últimas 24h" />
      </div>

      <AdminCard>
        {/* Filtros */}
        <div className="flex flex-wrap items-center gap-3 p-4 border-b border-slate-100">
          <div className="relative flex-1 min-w-[200px]">
            <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Buscar por ator, ação, alvo ou ID..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-lg bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100"
            />
          </div>
          <select value={sev} onChange={(e) => setSev(e.target.value)} className={selectCls}>
            <option value="todas">Todas as severidades</option>
            <option value="info">Info</option>
            <option value="aviso">Aviso</option>
            <option value="critico">Crítico</option>
          </select>
          <select value={modulo} onChange={(e) => setModulo(e.target.value)} className={selectCls}>
            <option value="todos">Todos os módulos</option>
            {modulos.map((m) => <option key={m} value={m}>{m}</option>)}
          </select>
        </div>

        {/* Feed de eventos */}
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="text-left text-xs text-slate-400 border-b border-slate-100">
                <th className="px-5 py-3 font-medium w-2"></th>
                <th className="px-5 py-3 font-medium">Evento</th>
                <th className="px-5 py-3 font-medium">Ator</th>
                <th className="px-5 py-3 font-medium">Módulo</th>
                <th className="px-5 py-3 font-medium">Severidade</th>
                <th className="px-5 py-3 font-medium text-right">Quando</th>
              </tr>
            </thead>
            <tbody>
              {filtered.map((l) => {
                const sm = sevMeta[l.severidade];
                const am = atorMeta[l.atorTipo];
                return (
                  <tr
                    key={l.id}
                    onClick={() => setDetalhe(l)}
                    className="border-b border-slate-50 hover:bg-slate-50/60 cursor-pointer"
                  >
                    <td className="pl-5 pr-0 py-3.5">
                      <span className={`block w-2 h-2 rounded-full ${sm.dot}`} />
                    </td>
                    <td className="px-5 py-3.5">
                      <p className="font-medium text-slate-700">{l.acao}</p>
                      <p className="text-xs text-slate-400">{l.alvo}</p>
                      <p className="text-[11px] text-slate-300">{l.id}</p>
                    </td>
                    <td className="px-5 py-3.5">
                      <div className="flex items-center gap-2">
                        <div className={`w-7 h-7 rounded-lg bg-gradient-to-br ${am.cls} text-white text-[10px] font-semibold flex items-center justify-center shrink-0`}>
                          {l.atorTipo === 'sistema' ? <Bot size={13} /> : iniciais(l.ator)}
                        </div>
                        <span className="text-slate-600">{l.ator}</span>
                      </div>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className="text-xs px-2 py-0.5 rounded-md bg-slate-100 text-slate-600">{l.modulo}</span>
                    </td>
                    <td className="px-5 py-3.5">
                      <span className={`inline-flex items-center gap-1.5 text-xs font-medium px-2 py-1 rounded-md ${sm.chip}`}>
                        <span className={`w-1.5 h-1.5 rounded-full ${sm.dot}`} /> {sm.label}
                      </span>
                    </td>
                    <td className="px-5 py-3.5 text-right text-xs text-slate-400 tabular-nums">{l.timestamp}</td>
                  </tr>
                );
              })}
              {filtered.length === 0 && (
                <tr>
                  <td colSpan={6} className="px-5 py-12 text-center text-slate-400 text-sm">Nenhum evento encontrado com os filtros atuais.</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </AdminCard>

      {/* Modal: detalhe do evento */}
      {detalhe && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/40 backdrop-blur-sm">
          <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md">
            <div className="flex items-center justify-between px-6 py-4 border-b border-slate-100">
              <h3 className="font-semibold text-slate-800 flex items-center gap-2">
                <span className={`w-2.5 h-2.5 rounded-full ${sevMeta[detalhe.severidade].dot}`} /> {detalhe.acao}
              </h3>
              <button onClick={() => setDetalhe(null)} className="w-9 h-9 rounded-lg hover:bg-slate-100 flex items-center justify-center text-slate-500">
                <X size={18} />
              </button>
            </div>
            <div className="p-6 space-y-4">
              <div className="rounded-xl bg-slate-50 border border-slate-100 p-4">
                <p className="text-sm text-slate-700">{detalhe.detalhe}</p>
              </div>
              <div className="grid grid-cols-2 gap-3 text-sm">
                <div>
                  <p className="text-xs text-slate-400">Evento</p>
                  <p className="font-medium text-slate-700">{detalhe.id}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Alvo</p>
                  <p className="font-medium text-slate-700">{detalhe.alvo}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Ator</p>
                  <p className="font-medium text-slate-700">{detalhe.ator}</p>
                </div>
                <div>
                  <p className="text-xs text-slate-400">Módulo</p>
                  <p className="font-medium text-slate-700">{detalhe.modulo}</p>
                </div>
                <div className="flex items-start gap-2">
                  <Globe size={15} className="text-slate-400 mt-0.5" />
                  <div>
                    <p className="text-xs text-slate-400">Origem (IP)</p>
                    <p className="font-medium text-slate-700 tabular-nums">{detalhe.ip}</p>
                  </div>
                </div>
                <div className="flex items-start gap-2">
                  <Monitor size={15} className="text-slate-400 mt-0.5" />
                  <div>
                    <p className="text-xs text-slate-400">Dispositivo</p>
                    <p className="font-medium text-slate-700">{detalhe.userAgent}</p>
                  </div>
                </div>
                <div className="col-span-2 flex items-start gap-2">
                  <Clock size={15} className="text-slate-400 mt-0.5" />
                  <div>
                    <p className="text-xs text-slate-400">Quando</p>
                    <p className="font-medium text-slate-700 tabular-nums">{detalhe.timestamp}</p>
                  </div>
                </div>
              </div>
            </div>
            <div className="flex justify-end px-6 py-4 border-t border-slate-100">
              <button onClick={() => setDetalhe(null)} className="px-4 py-2.5 text-sm font-medium text-slate-600 rounded-xl hover:bg-slate-100">Fechar</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminLogs;
