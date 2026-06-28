// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Sidebar em 3 seções.
 * Tema claro, marca em gradiente azul→verde.
 */
import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  ArrowLeftRight,
  Banknote,
  ShieldAlert,
  Layers,
  Users,
  UserCog,
  FileText,
  ScrollText,
  KeyRound,
  SlidersHorizontal,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { cn } from '@/utils/cn';

interface NavItem {
  path: string;
  label: string;
  icon: React.ComponentType<any>;
}

interface NavSection {
  title: string;
  items: NavItem[];
}

const sections: NavSection[] = [
  {
    title: 'Operação',
    items: [
      { path: '/admin/dashboard', label: 'Dashboard', icon: LayoutDashboard },
      { path: '/admin/establishments', label: 'Estabelecimentos', icon: Building2 },
      { path: '/admin/transactions', label: 'Transações', icon: ArrowLeftRight },
      { path: '/admin/withdrawals', label: 'Saques', icon: Banknote },
      { path: '/admin/payout-rules', label: 'Regras de Liquidação', icon: SlidersHorizontal },
    ],
  },
  {
    title: 'Gestão',
    items: [
      { path: '/admin/management', label: 'Painel Unificado', icon: ShieldAlert },
      { path: '/admin/bulk-actions', label: 'Ações em Massa', icon: Layers },
      { path: '/admin/affiliates', label: 'Afiliados', icon: Users },
      { path: '/admin/managers', label: 'Gerentes', icon: UserCog },
      { path: '/admin/statement', label: 'Extrato', icon: FileText },
      { path: '/admin/logs', label: 'Logs', icon: ScrollText },
    ],
  },
  {
    title: 'Controle de Acesso',
    items: [
      { path: '/admin/access', label: 'Usuários e Permissões', icon: KeyRound },
    ],
  },
];

interface Props {
  collapsed: boolean;
  onToggle: () => void;
}

const AdminSidebar: React.FC<Props> = ({ collapsed, onToggle }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const isActive = (path: string) => location.pathname === path;

  return (
    <aside
      className={cn(
        'fixed left-0 top-0 h-screen bg-white border-r border-slate-200 z-50 flex flex-col transition-all duration-300',
        collapsed ? 'w-20' : 'w-64'
      )}
    >
      {/* Marca */}
      <div className={cn('h-16 flex items-center border-b border-slate-100', collapsed ? 'justify-center px-2' : 'px-5')}>
        <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-emerald-500 flex items-center justify-center shadow-sm shrink-0">
          <span className="text-white font-bold text-base">Z</span>
        </div>
        {!collapsed && (
          <div className="ml-3 leading-tight">
            <h1 className="text-slate-800 font-bold text-[15px] tracking-tight">Zendpag</h1>
            <p className="text-[11px] text-slate-400">Admin Master</p>
          </div>
        )}
      </div>

      {/* Navegação */}
      <nav className="flex-1 overflow-y-auto py-4">
        {sections.map((section) => (
          <div key={section.title} className="mb-5">
            {!collapsed && (
              <p className="px-5 mb-2 text-[10px] font-semibold uppercase tracking-wider text-slate-400">
                {section.title}
              </p>
            )}
            <div className="space-y-0.5 px-3">
              {section.items.map((item) => {
                const Icon = item.icon;
                const active = isActive(item.path);
                return (
                  <button
                    key={item.path}
                    onClick={() => navigate(item.path)}
                    title={collapsed ? item.label : undefined}
                    className={cn(
                      'group relative w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm transition-all',
                      active
                        ? 'bg-gradient-to-r from-blue-50 to-emerald-50 text-blue-700 font-semibold'
                        : 'text-slate-500 hover:text-slate-800 hover:bg-slate-50',
                      collapsed && 'justify-center'
                    )}
                  >
                    {active && (
                      <span className="absolute left-0 top-1/2 -translate-y-1/2 h-6 w-1 rounded-r-full bg-gradient-to-b from-blue-600 to-emerald-500" />
                    )}
                    <Icon size={19} strokeWidth={1.75} className={active ? 'text-blue-600' : ''} />
                    {!collapsed && <span className="truncate">{item.label}</span>}
                  </button>
                );
              })}
            </div>
          </div>
        ))}
      </nav>

      {/* Recolher */}
      <div className="border-t border-slate-100 p-3">
        <button
          onClick={onToggle}
          className={cn(
            'flex items-center gap-3 px-3 py-2.5 w-full rounded-xl text-slate-400 hover:text-slate-700 hover:bg-slate-50 transition-all text-sm',
            collapsed && 'justify-center'
          )}
        >
          {collapsed ? <ChevronRight size={19} /> : <ChevronLeft size={19} />}
          {!collapsed && <span>Recolher menu</span>}
        </button>
      </div>
    </aside>
  );
};

export default AdminSidebar;
