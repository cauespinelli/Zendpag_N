// @ts-nocheck
/**
 * ZENDPAG ADMIN MASTER — Layout (sidebar + topbar + área de conteúdo).
 */
import React, { useState } from 'react';
import { Outlet } from 'react-router-dom';
import { Search, Bell, LogOut } from 'lucide-react';
import { cn } from '@/utils/cn';
import { useAuthStore } from '@/store/authStore';
import AdminSidebar from './AdminSidebar';

const iniciais = (nome?: string) =>
  (nome || 'Admin Master').split(' ').map((n) => n[0]).slice(0, 2).join('').toUpperCase();

const AdminLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const user = useAuthStore((s) => s.user);
  const logout = useAuthStore((s) => s.logout);

  const nome = user?.name || 'Admin Master';
  const papel = user?.roles?.includes('ADMIN') ? 'Acesso total' : (user?.roles?.join(', ') || '—');

  return (
    <div className="min-h-screen bg-slate-50">
      <AdminSidebar collapsed={collapsed} onToggle={() => setCollapsed(!collapsed)} />

      <div className={cn('transition-all duration-300', collapsed ? 'ml-20' : 'ml-64')}>
        {/* Topbar */}
        <header className="sticky top-0 z-40 h-16 bg-white/90 backdrop-blur border-b border-slate-200 flex items-center justify-between px-6">
          <div className="relative w-full max-w-md">
            <Search size={17} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-400" />
            <input
              type="text"
              placeholder="Buscar estabelecimento, transação, documento..."
              className="w-full pl-9 pr-4 py-2 text-sm rounded-xl bg-slate-50 border border-slate-200 focus:bg-white focus:border-blue-400 focus:outline-none focus:ring-2 focus:ring-blue-100 transition-all"
            />
          </div>

          <div className="flex items-center gap-4">
            <button className="relative w-10 h-10 rounded-xl hover:bg-slate-50 flex items-center justify-center text-slate-500">
              <Bell size={19} strokeWidth={1.75} />
              <span className="absolute top-2 right-2 w-2 h-2 rounded-full bg-rose-500" />
            </button>
            <div className="flex items-center gap-3 pl-3 border-l border-slate-200">
              <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-blue-600 to-emerald-500 flex items-center justify-center text-white font-semibold text-sm">
                {iniciais(nome)}
              </div>
              <div className="leading-tight hidden sm:block">
                <p className="text-sm font-semibold text-slate-800">{nome}</p>
                <p className="text-[11px] text-slate-400">{papel}</p>
              </div>
              <button
                onClick={() => logout()}
                title="Sair"
                className="ml-1 w-10 h-10 rounded-xl hover:bg-rose-50 flex items-center justify-center text-slate-400 hover:text-rose-600 transition-colors"
              >
                <LogOut size={18} strokeWidth={1.75} />
              </button>
            </div>
          </div>
        </header>

        {/* Conteúdo */}
        <main className="p-6">
          <Outlet />
        </main>
      </div>
    </div>
  );
};

export default AdminLayout;
