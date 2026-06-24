// @ts-nocheck
/**
 * ZENDPAG ADMIN — Kit de UI compartilhado (tema claro, azul→verde).
 * Componentes pequenos e reutilizáveis entre as telas do painel admin.
 */
import React from 'react';
import { cn } from '@/utils/cn';

/* Card branco padrão com borda e sombra suave */
export const AdminCard: React.FC<{ className?: string; children: React.ReactNode }> = ({
  className,
  children,
}) => (
  <div className={cn('bg-white rounded-2xl border border-slate-200/80 shadow-sm', className)}>
    {children}
  </div>
);

/* Cabeçalho de seção/card */
export const SectionHeader: React.FC<{
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
}> = ({ title, subtitle, action }) => (
  <div className="flex items-start justify-between gap-4 px-5 pt-5 pb-4">
    <div>
      <h3 className="text-[15px] font-semibold text-slate-800">{title}</h3>
      {subtitle && <p className="text-xs text-slate-400 mt-0.5">{subtitle}</p>}
    </div>
    {action}
  </div>
);

/* KPI / Stat card com ícone em gradiente */
export const StatCard: React.FC<{
  label: string;
  value: string;
  icon: React.ReactNode;
  trend?: number;
  accent?: 'blue' | 'emerald' | 'amber' | 'violet' | 'rose';
  hint?: string;
}> = ({ label, value, icon, trend, accent = 'blue', hint }) => {
  const accents: Record<string, string> = {
    blue: 'from-blue-500 to-blue-600',
    emerald: 'from-emerald-500 to-emerald-600',
    amber: 'from-amber-400 to-amber-500',
    violet: 'from-violet-500 to-violet-600',
    rose: 'from-rose-500 to-rose-600',
  };
  return (
    <AdminCard className="p-5">
      <div className="flex items-start justify-between">
        <div
          className={cn(
            'w-11 h-11 rounded-xl flex items-center justify-center text-white bg-gradient-to-br shadow-sm',
            accents[accent]
          )}
        >
          {icon}
        </div>
        {typeof trend === 'number' && (
          <span
            className={cn(
              'text-xs font-semibold px-2 py-1 rounded-lg',
              trend >= 0 ? 'text-emerald-600 bg-emerald-50' : 'text-rose-600 bg-rose-50'
            )}
          >
            {trend >= 0 ? '▲' : '▼'} {Math.abs(trend).toLocaleString('pt-BR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })}%
          </span>
        )}
      </div>
      <p className="text-sm text-slate-500 mt-4">{label}</p>
      <p className="text-2xl font-bold text-slate-800 mt-1 tabular-nums tracking-tight">{value}</p>
      {hint && <p className="text-xs text-slate-400 mt-1">{hint}</p>}
    </AdminCard>
  );
};

/* Badge de status com cores semânticas */
export const StatusBadge: React.FC<{
  tone: 'success' | 'danger' | 'warning' | 'neutral' | 'info';
  children: React.ReactNode;
}> = ({ tone, children }) => {
  const tones: Record<string, string> = {
    success: 'text-emerald-700 bg-emerald-50 border-emerald-200',
    danger: 'text-rose-700 bg-rose-50 border-rose-200',
    warning: 'text-amber-700 bg-amber-50 border-amber-200',
    info: 'text-blue-700 bg-blue-50 border-blue-200',
    neutral: 'text-slate-600 bg-slate-50 border-slate-200',
  };
  return (
    <span className={cn('inline-flex items-center gap-1 text-xs font-medium px-2.5 py-1 rounded-full border', tones[tone])}>
      {children}
    </span>
  );
};

/* Página: título + ação */
export const PageHeader: React.FC<{
  title: string;
  subtitle?: string;
  action?: React.ReactNode;
}> = ({ title, subtitle, action }) => (
  <div className="flex flex-wrap items-end justify-between gap-4 mb-6">
    <div>
      <h1 className="text-2xl font-bold text-slate-800 tracking-tight">{title}</h1>
      {subtitle && <p className="text-sm text-slate-500 mt-1">{subtitle}</p>}
    </div>
    {action}
  </div>
);

/* Botão primário (gradiente azul→verde) */
export const GradientButton: React.FC<
  React.ButtonHTMLAttributes<HTMLButtonElement> & { children: React.ReactNode }
> = ({ children, className, ...props }) => (
  <button
    className={cn(
      'inline-flex items-center gap-2 text-sm font-semibold text-white px-4 py-2.5 rounded-xl',
      'bg-gradient-to-r from-blue-600 to-emerald-500 hover:from-blue-700 hover:to-emerald-600',
      'shadow-sm transition-all disabled:opacity-50',
      className
    )}
    {...props}
  >
    {children}
  </button>
);

/* Mini barra de progresso (ex.: % aprovação) */
export const MiniBar: React.FC<{ value: number; tone?: 'emerald' | 'blue' | 'rose' }> = ({
  value,
  tone = 'emerald',
}) => {
  const tones = { emerald: 'bg-emerald-500', blue: 'bg-blue-500', rose: 'bg-rose-500' };
  return (
    <div className="h-1.5 w-full rounded-full bg-slate-100 overflow-hidden">
      <div className={cn('h-full rounded-full', tones[tone])} style={{ width: `${Math.min(value, 100)}%` }} />
    </div>
  );
};
