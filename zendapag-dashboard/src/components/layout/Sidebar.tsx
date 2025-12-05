import React from 'react';
import { useNavigate, useLocation } from 'react-router-dom';
import {
  LayoutDashboard,
  Building2,
  ArrowLeftRight,
  Wallet,
  AlertTriangle,
  Activity,
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
  onClick?: () => void;
}

const Sidebar: React.FC<SidebarProps> = ({ collapsed, onToggle }) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { logout } = useAuthStore();

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  const menuItems: MenuItem[] = [
    {
      key: '/dashboard',
      icon: <LayoutDashboard size={20} />,
      label: 'Dashboard',
    },
    {
      key: '/establishments',
      icon: <Building2 size={20} />,
      label: 'Estabelecimentos',
    },
    {
      key: '/transactions',
      icon: <ArrowLeftRight size={20} />,
      label: 'Transações',
    },
    {
      key: '/withdrawals',
      icon: <Wallet size={20} />,
      label: 'Saques',
    },
    {
      key: '/disputes',
      icon: <AlertTriangle size={20} />,
      label: 'Disputas',
    },
    {
      key: '/med-analytics',
      icon: <Activity size={20} />,
      label: 'Med Analytics',
    },
    {
      key: '/affiliates',
      icon: <Users size={20} />,
      label: 'Afiliados',
    },
    {
      key: '/statements',
      icon: <FileText size={20} />,
      label: 'Extrato',
    },
  ];

  const isActive = (path: string) => location.pathname === path;

  return (
    <aside
      className={`fixed left-0 top-0 h-screen bg-[#0a0a0a] border-r border-gray-800 transition-all duration-300 z-50 flex flex-col ${
        collapsed ? 'w-[70px]' : 'w-[240px]'
      }`}
    >
      {/* Logo */}
      <div className="h-16 flex items-center justify-center border-b border-gray-800 px-4">
        {collapsed ? (
          <span className="text-2xl font-bold text-blue-500">Z</span>
        ) : (
          <div className="flex items-center gap-2">
            <span className="text-2xl font-bold text-blue-500">Zend</span>
            <span className="text-2xl font-bold text-white">Pag</span>
          </div>
        )}
      </div>

      {/* Menu Items */}
      <nav className="flex-1 py-4 overflow-y-auto">
        <ul className="space-y-1 px-3">
          {menuItems.map((item) => (
            <li key={item.key}>
              <button
                onClick={() => navigate(item.key)}
                className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200 ${
                  isActive(item.key)
                    ? 'bg-blue-600 text-white'
                    : 'text-gray-400 hover:bg-gray-800 hover:text-white'
                }`}
                title={collapsed ? item.label : undefined}
              >
                <span className="flex-shrink-0">{item.icon}</span>
                {!collapsed && (
                  <span className="text-sm font-medium truncate">{item.label}</span>
                )}
              </button>
            </li>
          ))}
        </ul>
      </nav>

      {/* Logout Button */}
      <div className="border-t border-gray-800 p-3">
        <button
          onClick={handleLogout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-all duration-200"
          title={collapsed ? 'Sair' : undefined}
        >
          <LogOut size={20} />
          {!collapsed && <span className="text-sm font-medium">Sair</span>}
        </button>
      </div>

      {/* Toggle Button */}
      <button
        onClick={onToggle}
        className="absolute -right-3 top-20 w-6 h-6 bg-gray-800 border border-gray-700 rounded-full flex items-center justify-center text-gray-400 hover:text-white hover:bg-gray-700 transition-colors"
      >
        {collapsed ? <ChevronRight size={14} /> : <ChevronLeft size={14} />}
      </button>
    </aside>
  );
};

export default Sidebar;
