import React from 'react';
import { Bell, Search, User, ChevronDown } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { useNavigate } from 'react-router-dom';

interface HeaderProps {
  sidebarCollapsed: boolean;
}

const Header: React.FC<HeaderProps> = ({ sidebarCollapsed }) => {
  const { user, logout } = useAuthStore();
  const navigate = useNavigate();
  const [showUserMenu, setShowUserMenu] = React.useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header
      className={`fixed top-0 right-0 h-20 bg-[#0D0D0D] border-b border-[#1E1E1E] flex items-center justify-between px-8 z-40 transition-all duration-300 ${
        sidebarCollapsed ? 'left-20' : 'left-64'
      }`}
    >
      {/* Search */}
      <div className="relative flex-1 max-w-md">
        <Search size={18} strokeWidth={1.5} className="absolute left-4 top-1/2 -translate-y-1/2 text-[#5C5C5C]" />
        <input
          type="text"
          placeholder="Buscar..."
          className="w-full pl-12 pr-4 py-3 bg-[#1A1A1A] border border-[#2D2D2D] rounded-xl text-white placeholder-[#5C5C5C] focus:outline-none focus:border-[#C9A962] transition-colors"
        />
      </div>

      {/* Right side */}
      <div className="flex items-center gap-6">
        {/* Notifications */}
        <button className="relative p-3 rounded-xl hover:bg-[#1A1A1A] transition-colors">
          <Bell size={20} strokeWidth={1.5} className="text-[#8C8C8C]" />
          <span className="absolute top-2 right-2 w-2 h-2 bg-[#E53935] rounded-full" />
        </button>

        {/* User Menu */}
        <div className="relative">
          <button
            onClick={() => setShowUserMenu(!showUserMenu)}
            className="flex items-center gap-4 pl-6 border-l border-[#2D2D2D] hover:bg-[#1A1A1A] rounded-xl py-2 px-4 transition-colors"
          >
            <div className="text-right hidden sm:block">
              <p className="text-white text-sm font-medium">{user?.name || 'Admin ZendPag'}</p>
              <p className="text-[#5C5C5C] text-xs">{user?.email || 'admin@zendpag.com'}</p>
            </div>
            <div className="w-11 h-11 bg-gradient-to-br from-[#C9A962] to-[#8B6914] rounded-xl flex items-center justify-center">
              <User size={20} className="text-black" />
            </div>
            <ChevronDown size={16} strokeWidth={1.5} className="text-[#5C5C5C]" />
          </button>

          {showUserMenu && (
            <div className="absolute right-0 top-full mt-2 w-56 bg-[#1A1A1A] border border-[#2D2D2D] rounded-xl shadow-xl py-2 z-50">
              <div className="px-4 py-3 border-b border-[#2D2D2D]">
                <p className="text-white text-sm font-medium">{user?.name || 'Admin'}</p>
                <p className="text-[#5C5C5C] text-xs">{user?.email || 'admin@zendpag.com'}</p>
              </div>
              <div className="py-1">
                <button
                  onClick={() => {
                    navigate('/profile');
                    setShowUserMenu(false);
                  }}
                  className="w-full text-left px-4 py-3 text-sm text-[#8C8C8C] hover:bg-[#2D2D2D] hover:text-white transition-colors"
                >
                  Meu Perfil
                </button>
                <button
                  onClick={() => {
                    navigate('/settings');
                    setShowUserMenu(false);
                  }}
                  className="w-full text-left px-4 py-3 text-sm text-[#8C8C8C] hover:bg-[#2D2D2D] hover:text-white transition-colors"
                >
                  Configurações
                </button>
              </div>
              <div className="border-t border-[#2D2D2D] py-1">
                <button
                  onClick={handleLogout}
                  className="w-full text-left px-4 py-3 text-sm text-[#E53935] hover:bg-[#E53935]/10 transition-colors"
                >
                  Sair
                </button>
              </div>
            </div>
          )}
        </div>
      </div>

      {/* Click outside to close */}
      {showUserMenu && (
        <div
          className="fixed inset-0 z-40"
          onClick={() => setShowUserMenu(false)}
        />
      )}
    </header>
  );
};

export default Header;
