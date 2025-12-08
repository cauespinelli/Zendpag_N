import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  ArrowLeftRight,
  Wallet,
  AlertCircle,
  BarChart2,
  Users,
  FileText,
  LogOut,
  ChevronLeft,
  ChevronRight,
} from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

interface SidebarProps {
  collapsed: boolean;
  onToggle: () => void;
}

interface MenuItem {
  key: string;
  icon: React.ReactNode;
  label: string;
}

const menuItems: MenuItem[] = [
  { key: '/dashboard', icon: <LayoutDashboard size={20} strokeWidth={1.5} />, label: 'Dashboard' },
  { key: '/establishments', icon: <Building2 size={20} strokeWidth={1.5} />, label: 'Estabelecimentos' },
  { key: '/transactions', icon: <ArrowLeftRight size={20} strokeWidth={1.5} />, label: 'Transações' },
  { key: '/withdrawals', icon: <Wallet size={20} strokeWidth={1.5} />, label: 'Saques' },
  { key: '/disputes', icon: <AlertCircle size={20} strokeWidth={1.5} />, label: 'Disputas' },
  { key: '/med-analytics', icon: <BarChart2 size={20} strokeWidth={1.5} />, label: 'Med Analytics' },
  { key: '/affiliates', icon: <Users size={20} strokeWidth={1.5} />, label: 'Afiliados' },
  { key: '/statements', icon: <FileText size={20} strokeWidth={1.5} />, label: 'Extrato' },
];

const Sidebar: React.FC<SidebarProps> = ({ collapsed, onToggle }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const isActive = (path: string) => location.pathname === path;

  return (
    <aside
      className={`fixed left-0 top-0 h-screen bg-[#0D0D0D] transition-all duration-300 z-50 flex flex-col ${
        collapsed ? 'w-20' : 'w-64'
      }`}
    >
      {/* Logo */}
      <div className={`h-20 flex items-center border-b border-[#1E1E1E] ${collapsed ? 'justify-center px-2' : 'px-6'}`}>
        {!collapsed ? (
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 bg-gradient-to-br from-[#C9A962] to-[#8B6914] rounded-xl flex items-center justify-center">
              <span className="text-black font-bold text-lg">Z</span>
            </div>
            <div>
              <h1 className="text-white font-semibold text-xl tracking-tight">ZendPag</h1>
              <p className="text-[#5C5C5C] text-xs">Payment Gateway</p>
            </div>
          </div>
        ) : (
          <div className="w-10 h-10 bg-gradient-to-br from-[#C9A962] to-[#8B6914] rounded-xl flex items-center justify-center">
            <span className="text-black font-bold text-lg">Z</span>
          </div>
        )}
      </div>

      {/* Navigation */}
      <nav className="flex-1 py-6 overflow-y-auto">
        <div className="space-y-1 px-3">
          {menuItems.map((item) => {
            const active = isActive(item.key);
            return (
              <button
                key={item.key}
                onClick={() => navigate(item.key)}
                className={`group w-full flex items-center gap-4 px-4 py-3 rounded-xl transition-all duration-200 ${
                  active
                    ? 'bg-[#1E1E1E] text-white'
                    : 'text-[#8C8C8C] hover:text-white hover:bg-[#1A1A1A]'
                } ${collapsed ? 'justify-center' : ''}`}
                title={collapsed ? item.label : undefined}
              >
                <span className={active ? 'text-[#C9A962]' : 'group-hover:text-white'}>
                  {item.icon}
                </span>
                {!collapsed && (
                  <span className="text-sm font-medium">{item.label}</span>
                )}
                {active && !collapsed && (
                  <div className="ml-auto w-1.5 h-1.5 bg-[#C9A962] rounded-full" />
                )}
              </button>
            );
          })}
        </div>
      </nav>

      {/* Footer */}
      <div className="border-t border-[#1E1E1E] p-3">
        <button
          onClick={handleLogout}
          className={`flex items-center gap-4 px-4 py-3 w-full rounded-xl text-[#8C8C8C] hover:text-[#E53935] hover:bg-[#1A1A1A] transition-all duration-200 ${
            collapsed ? 'justify-center' : ''
          }`}
          title={collapsed ? 'Sair' : undefined}
        >
          <LogOut size={20} strokeWidth={1.5} />
          {!collapsed && <span className="text-sm font-medium">Sair</span>}
        </button>

        <button
          onClick={onToggle}
          className={`flex items-center gap-4 px-4 py-3 w-full rounded-xl text-[#5C5C5C] hover:text-white hover:bg-[#1A1A1A] transition-all duration-200 mt-2 ${
            collapsed ? 'justify-center' : ''
          }`}
          title={collapsed ? 'Expandir menu' : 'Recolher menu'}
        >
          {collapsed ? <ChevronRight size={20} strokeWidth={1.5} /> : <ChevronLeft size={20} strokeWidth={1.5} />}
          {!collapsed && <span className="text-sm">Recolher menu</span>}
        </button>
      </div>
    </aside>
  );
};

export default Sidebar;
