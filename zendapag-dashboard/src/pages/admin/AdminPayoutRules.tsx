// @ts-nocheck
/**
 * Regras de Liquidação (Painel Admin Master).
 *
 * Configura, por método de pagamento, a retenção (D+N) e o saque automático —
 * globalmente e por estabelecimento (override em cascata: merchant -> global).
 * Mostra também os saldos por método do estabelecimento selecionado.
 *
 * Backend real: /admin/payout-rules (global), /admin/payout-rules/merchant/{id}
 * (efetivas + override) e /admin/merchants/{id}/balances.
 */
import React, { useEffect, useMemo, useState } from 'react';
import {
  PageHeader,
  AdminCard,
  SectionHeader,
  GradientButton,
  StatusBadge,
} from '@/components/admin/ui';
import { adminPayoutService, methodLabel } from '@/services/adminPayoutService';
import adminMerchantService from '@/services/adminMerchantService';
import { Save, Zap, Clock, RefreshCw, Inbox, WifiOff, Check } from 'lucide-react';

const brl = (v: any) =>
  (Number(v) || 0).toLocaleString('pt-BR', { style: 'currency', currency: 'BRL' });

/* Toggle simples (branco sólido, sem dependências) */
const Toggle = ({ checked, onChange, label }: any) => (
  <button
    type="button"
    onClick={() => onChange(!checked)}
    className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors ${
      checked ? 'bg-emerald-500' : 'bg-slate-300'
    }`}
    title={label}
  >
    <span
      className={`inline-block h-5 w-5 transform rounded-full bg-white shadow transition-transform ${
        checked ? 'translate-x-5' : 'translate-x-1'
      }`}
    />
  </button>
);

/* Uma linha de regra editável (usada tanto na global quanto no override) */
const RuleRow = ({ rule, onChange, extra }: any) => (
  <tr className="border-b border-slate-50">
    <td className="px-4 py-3">
      <p className="font-medium text-slate-700">{methodLabel[rule.method] || rule.method}</p>
      {extra}
    </td>
    <td className="px-4 py-3 text-center">
      <Toggle
        checked={rule.retentionEnabled}
        onChange={(v: boolean) => onChange({ ...rule, retentionEnabled: v })}
        label="Retenção"
      />
    </td>
    <td className="px-4 py-3 text-center">
      <input
        type="number"
        min={0}
        value={rule.holdingDays}
        disabled={!rule.retentionEnabled}
        onChange={(e) => onChange({ ...rule, holdingDays: Math.max(0, Number(e.target.value) || 0) })}
        className="w-20 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-center text-sm text-slate-700 tabular-nums disabled:bg-slate-50 disabled:text-slate-300"
      />
    </td>
    <td className="px-4 py-3 text-center">
      <Toggle
        checked={rule.autoPayoutEnabled}
        onChange={(v: boolean) => onChange({ ...rule, autoPayoutEnabled: v })}
        label="Saque automático"
      />
    </td>
  </tr>
);

export default function AdminPayoutRules() {
  const [aba, setAba] = useState<'global' | 'merchant'>('global');

  // Global
  const [globalRules, setGlobalRules] = useState<any[]>([]);
  const [loadingGlobal, setLoadingGlobal] = useState(true);
  const [errGlobal, setErrGlobal] = useState<string | null>(null);
  const [savingMethod, setSavingMethod] = useState<string | null>(null);
  const [toast, setToast] = useState<string | null>(null);

  // Merchant
  const [merchants, setMerchants] = useState<any[]>([]);
  const [merchantId, setMerchantId] = useState<string>('');
  const [merchantRules, setMerchantRules] = useState<any[]>([]);
  const [balances, setBalances] = useState<any>(null);
  const [loadingMerchant, setLoadingMerchant] = useState(false);

  const loadGlobal = async () => {
    setLoadingGlobal(true);
    setErrGlobal(null);
    try {
      setGlobalRules(await adminPayoutService.getGlobalRules());
    } catch (e: any) {
      setErrGlobal(e?.response?.status === 401 || e?.response?.status === 403 ? 'sem-acesso' : 'erro');
    } finally {
      setLoadingGlobal(false);
    }
  };

  const loadMerchants = async () => {
    try {
      const list = await adminMerchantService.listAll();
      setMerchants(list);
      if (list.length && !merchantId) setMerchantId(list[0].id);
    } catch {
      /* silencioso: a aba global ainda funciona */
    }
  };

  const loadMerchantData = async (id: string) => {
    if (!id) return;
    setLoadingMerchant(true);
    try {
      const [rules, bal] = await Promise.all([
        adminPayoutService.getMerchantRules(id),
        adminPayoutService.getBalances(id),
      ]);
      setMerchantRules(rules);
      setBalances(bal);
    } catch {
      setMerchantRules([]);
      setBalances(null);
    } finally {
      setLoadingMerchant(false);
    }
  };

  useEffect(() => {
    loadGlobal();
    loadMerchants();
  }, []);

  useEffect(() => {
    if (aba === 'merchant' && merchantId) loadMerchantData(merchantId);
  }, [aba, merchantId]);

  const flash = (msg: string) => {
    setToast(msg);
    setTimeout(() => setToast(null), 2500);
  };

  const saveGlobal = async (rule: any) => {
    setSavingMethod(rule.method);
    try {
      await adminPayoutService.saveGlobalRule({
        method: rule.method,
        retentionEnabled: rule.retentionEnabled,
        holdingDays: rule.holdingDays,
        autoPayoutEnabled: rule.autoPayoutEnabled,
      });
      flash(`Regra global de ${methodLabel[rule.method] || rule.method} salva.`);
    } catch {
      flash('Falha ao salvar a regra global.');
    } finally {
      setSavingMethod(null);
    }
  };

  const saveMerchant = async (rule: any) => {
    setSavingMethod(rule.method);
    try {
      await adminPayoutService.saveMerchantRule(merchantId, {
        method: rule.method,
        retentionEnabled: rule.retentionEnabled,
        holdingDays: rule.holdingDays,
        autoPayoutEnabled: rule.autoPayoutEnabled,
      });
      flash(`Override de ${methodLabel[rule.method] || rule.method} salvo.`);
      loadMerchantData(merchantId);
    } catch {
      flash('Falha ao salvar o override.');
    } finally {
      setSavingMethod(null);
    }
  };

  const removeOverride = async (method: string) => {
    try {
      await adminPayoutService.deleteMerchantOverride(merchantId, method);
      flash(`Override de ${methodLabel[method] || method} removido (herda a global).`);
      loadMerchantData(merchantId);
    } catch {
      flash('Falha ao remover o override.');
    }
  };

  const setGlobalRule = (next: any) =>
    setGlobalRules((prev) => prev.map((r) => (r.method === next.method ? next : r)));
  const setMerchantRule = (next: any) =>
    setMerchantRules((prev) => prev.map((r) => (r.method === next.method ? next : r)));

  return (
    <div>
      <PageHeader
        title="Regras de Liquidação"
        subtitle="Retenção (D+N) e saque automático por método — global e por estabelecimento."
        action={
          <GradientButton onClick={() => (aba === 'global' ? loadGlobal() : loadMerchantData(merchantId))}>
            <RefreshCw size={16} /> Atualizar
          </GradientButton>
        }
      />

      {/* Abas */}
      <div className="flex gap-1 mb-5 bg-slate-100 p-1 rounded-xl w-fit">
        {[
          { k: 'global', label: 'Global' },
          { k: 'merchant', label: 'Por estabelecimento' },
        ].map((t) => (
          <button
            key={t.k}
            onClick={() => setAba(t.k as any)}
            className={`px-4 py-2 rounded-lg text-sm font-semibold transition-colors ${
              aba === t.k ? 'bg-white text-slate-800 shadow-sm' : 'text-slate-500 hover:text-slate-700'
            }`}
          >
            {t.label}
          </button>
        ))}
      </div>

      {toast && (
        <div className="mb-4 inline-flex items-center gap-2 rounded-xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm font-medium text-emerald-700">
          <Check size={16} /> {toast}
        </div>
      )}

      {aba === 'global' && (
        <AdminCard>
          <SectionHeader
            title="Padrão da plataforma"
            subtitle="Aplicado a todos os estabelecimentos, salvo override."
          />
          {loadingGlobal ? (
            <div className="px-5 py-16 text-center text-slate-400">Carregando regras…</div>
          ) : errGlobal ? (
            <div className="px-5 py-16 text-center text-slate-400">
              <WifiOff className="mx-auto mb-3 text-slate-300" size={28} />
              {errGlobal === 'sem-acesso' ? 'Sem acesso (faça login como ADMIN).' : 'Falha ao carregar as regras.'}
            </div>
          ) : (
            <table className="w-full text-sm">
              <thead>
                <tr className="text-left text-xs uppercase tracking-wide text-slate-400 border-b border-slate-100">
                  <th className="px-4 py-3 font-medium">Método</th>
                  <th className="px-4 py-3 font-medium text-center">Retenção</th>
                  <th className="px-4 py-3 font-medium text-center">Dias (D+N)</th>
                  <th className="px-4 py-3 font-medium text-center">Saque automático</th>
                  <th className="px-4 py-3 font-medium text-right">Ação</th>
                </tr>
              </thead>
              <tbody>
                {globalRules.map((rule) => (
                  <RuleRow key={rule.method} rule={rule} onChange={setGlobalRule}>
                    {null}
                  </RuleRow>
                ))}
              </tbody>
            </table>
          )}
          {!loadingGlobal && !errGlobal && (
            <div className="flex flex-wrap gap-2 px-4 py-4 border-t border-slate-100">
              {globalRules.map((rule) => (
                <button
                  key={rule.method}
                  onClick={() => saveGlobal(rule)}
                  disabled={savingMethod === rule.method}
                  className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                >
                  <Save size={13} /> Salvar {methodLabel[rule.method] || rule.method}
                </button>
              ))}
            </div>
          )}
        </AdminCard>
      )}

      {aba === 'merchant' && (
        <div className="space-y-5">
          {/* Seletor de estabelecimento */}
          <AdminCard className="p-5">
            <div className="flex flex-wrap items-center gap-3">
              <span className="text-sm font-medium text-slate-600">Estabelecimento:</span>
              <select
                value={merchantId}
                onChange={(e) => setMerchantId(e.target.value)}
                className="rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-700 min-w-[240px]"
              >
                {merchants.length === 0 && <option value="">—</option>}
                {merchants.map((m) => (
                  <option key={m.id} value={m.id}>
                    {m.nome || m.razaoSocial}
                  </option>
                ))}
              </select>
            </div>
          </AdminCard>

          {/* Saldos por método */}
          <AdminCard>
            <SectionHeader title="Saldos por método" subtitle="Disponível e pendente, derivados do razão." />
            {!balances ? (
              <div className="px-5 pb-6 text-sm text-slate-400">
                {loadingMerchant ? 'Carregando…' : 'Sem dados de saldo.'}
              </div>
            ) : (
              <div className="px-5 pb-5">
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 mb-4">
                  <div className="rounded-xl border border-emerald-100 bg-emerald-50/60 px-4 py-3">
                    <p className="text-xs text-emerald-700">Disponível (total)</p>
                    <p className="text-lg font-bold text-emerald-800 tabular-nums mt-1">{brl(balances.availableTotal)}</p>
                  </div>
                  <div className="rounded-xl border border-amber-100 bg-amber-50/60 px-4 py-3">
                    <p className="text-xs text-amber-700">Pendente (total)</p>
                    <p className="text-lg font-bold text-amber-800 tabular-nums mt-1">{brl(balances.pendingTotal)}</p>
                  </div>
                </div>
                {balances.byMethod?.length ? (
                  <table className="w-full text-sm">
                    <thead>
                      <tr className="text-left text-xs uppercase tracking-wide text-slate-400 border-b border-slate-100">
                        <th className="px-3 py-2 font-medium">Método</th>
                        <th className="px-3 py-2 font-medium text-right">Pendente</th>
                        <th className="px-3 py-2 font-medium text-right">Recebido (liberado)</th>
                      </tr>
                    </thead>
                    <tbody>
                      {balances.byMethod.map((b: any) => (
                        <tr key={b.method} className="border-b border-slate-50">
                          <td className="px-3 py-2 text-slate-700">{methodLabel[b.method] || b.method}</td>
                          <td className="px-3 py-2 text-right tabular-nums text-amber-700">{brl(b.pending)}</td>
                          <td className="px-3 py-2 text-right tabular-nums text-slate-600">{brl(b.received)}</td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                ) : (
                  <p className="text-sm text-slate-400">Nenhum lançamento ainda.</p>
                )}
              </div>
            )}
          </AdminCard>

          {/* Regras efetivas + override */}
          <AdminCard>
            <SectionHeader
              title="Regras efetivas"
              subtitle="Editar salva um override; remover volta a herdar a global."
            />
            {loadingMerchant ? (
              <div className="px-5 py-16 text-center text-slate-400">Carregando regras…</div>
            ) : (
              <table className="w-full text-sm">
                <thead>
                  <tr className="text-left text-xs uppercase tracking-wide text-slate-400 border-b border-slate-100">
                    <th className="px-4 py-3 font-medium">Método</th>
                    <th className="px-4 py-3 font-medium text-center">Retenção</th>
                    <th className="px-4 py-3 font-medium text-center">Dias (D+N)</th>
                    <th className="px-4 py-3 font-medium text-center">Saque automático</th>
                    <th className="px-4 py-3 font-medium text-right">Ações</th>
                  </tr>
                </thead>
                <tbody>
                  {merchantRules.map((rule) => (
                    <tr key={rule.method} className="border-b border-slate-50">
                      <td className="px-4 py-3">
                        <p className="font-medium text-slate-700 flex items-center gap-2">
                          {methodLabel[rule.method] || rule.method}
                          {rule.overridden ? (
                            <StatusBadge tone="info">Próprio</StatusBadge>
                          ) : (
                            <StatusBadge tone="neutral">Herdado</StatusBadge>
                          )}
                        </p>
                      </td>
                      <td className="px-4 py-3 text-center">
                        <Toggle
                          checked={rule.retentionEnabled}
                          onChange={(v: boolean) => setMerchantRule({ ...rule, retentionEnabled: v })}
                        />
                      </td>
                      <td className="px-4 py-3 text-center">
                        <input
                          type="number"
                          min={0}
                          value={rule.holdingDays}
                          disabled={!rule.retentionEnabled}
                          onChange={(e) =>
                            setMerchantRule({ ...rule, holdingDays: Math.max(0, Number(e.target.value) || 0) })
                          }
                          className="w-20 rounded-lg border border-slate-200 bg-white px-2 py-1.5 text-center text-sm text-slate-700 tabular-nums disabled:bg-slate-50 disabled:text-slate-300"
                        />
                      </td>
                      <td className="px-4 py-3 text-center">
                        <Toggle
                          checked={rule.autoPayoutEnabled}
                          onChange={(v: boolean) => setMerchantRule({ ...rule, autoPayoutEnabled: v })}
                        />
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex items-center justify-end gap-2">
                          <button
                            onClick={() => saveMerchant(rule)}
                            disabled={savingMethod === rule.method}
                            className="inline-flex items-center gap-1.5 rounded-lg border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 hover:bg-slate-50 disabled:opacity-50"
                          >
                            <Save size={13} /> Salvar
                          </button>
                          {rule.overridden && (
                            <button
                              onClick={() => removeOverride(rule.method)}
                              className="inline-flex items-center gap-1.5 rounded-lg border border-rose-200 bg-white px-3 py-1.5 text-xs font-semibold text-rose-600 hover:bg-rose-50"
                            >
                              Remover
                            </button>
                          )}
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </AdminCard>

          <p className="text-xs text-slate-400 flex items-center gap-1.5">
            <Zap size={12} /> Saque automático envia o líquido disponível à chave PIX do estabelecimento.
            <Clock size={12} className="ml-2" /> Retenção segura o valor como pendente por D+N antes de liberar.
          </p>
        </div>
      )}
    </div>
  );
}
