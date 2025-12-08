// @ts-nocheck
import React, { useState, useEffect } from 'react';
import { Navigate, useLocation, useNavigate } from 'react-router-dom';
import { Mail, Lock, Eye, EyeOff } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';

interface LocationState {
  from?: Location;
  error?: string;
}

const LoginPage: React.FC = () => {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [rememberMe, setRememberMe] = useState(false);
  const [loginError, setLoginError] = useState<string | null>(null);

  const location = useLocation();
  const navigate = useNavigate();
  const state = location.state as LocationState;

  const {
    login,
    isAuthenticated,
    isLoading,
    error: authError,
    clearError
  } = useAuthStore();

  // Clear errors when component mounts or form values change
  useEffect(() => {
    clearError();
    setLoginError(null);
  }, [clearError]);

  useEffect(() => {
    if (authError) {
      setLoginError(authError);
    }
  }, [authError]);

  // Show error from state (e.g., session expired)
  useEffect(() => {
    if (state?.error) {
      setLoginError(state.error);
    }
  }, [state]);

  // Redirect if already authenticated
  if (isAuthenticated) {
    const redirectTo = (state?.from as any)?.pathname || '/dashboard';
    return <Navigate to={redirectTo} replace />;
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoginError(null);
    clearError();

    try {
      await login({ email, password, rememberMe });
      navigate('/dashboard');
    } catch (error) {
      console.error('Login error:', error);
    }
  };

  const handleInputChange = () => {
    if (loginError) {
      setLoginError(null);
    }
    if (authError) {
      clearError();
    }
  };

  return (
    <div className="min-h-screen bg-[#0D0D0D] flex items-center justify-center p-4">
      <div className="w-full max-w-md">
        {/* Logo */}
        <div className="text-center mb-10">
          <div className="inline-flex items-center justify-center w-16 h-16 bg-gradient-to-br from-[#C9A962] to-[#8B6914] rounded-2xl mb-4">
            <span className="text-black font-bold text-2xl">Z</span>
          </div>
          <h1 className="text-white text-2xl font-semibold">ZendPag</h1>
          <p className="text-[#5C5C5C] text-sm mt-1">Acesse sua conta</p>
        </div>

        {/* Error Alert */}
        {loginError && (
          <div className="mb-6 p-4 bg-[#E53935]/10 border border-[#E53935]/20 rounded-xl">
            <p className="text-[#E53935] text-sm">{loginError}</p>
          </div>
        )}

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-6">
          {/* Email */}
          <div className="space-y-2">
            <label className="text-sm text-[#8C8C8C]">Email</label>
            <div className="relative">
              <Mail size={18} strokeWidth={1.5} className="absolute left-4 top-1/2 -translate-y-1/2 text-[#5C5C5C]" />
              <input
                type="email"
                value={email}
                onChange={(e) => {
                  setEmail(e.target.value);
                  handleInputChange();
                }}
                placeholder="seu@email.com"
                className="w-full pl-12 pr-4 py-4 bg-[#1A1A1A] border border-[#2D2D2D] rounded-xl text-white placeholder-[#5C5C5C] focus:outline-none focus:border-[#C9A962] transition-colors"
                required
              />
            </div>
          </div>

          {/* Password */}
          <div className="space-y-2">
            <label className="text-sm text-[#8C8C8C]">Senha</label>
            <div className="relative">
              <Lock size={18} strokeWidth={1.5} className="absolute left-4 top-1/2 -translate-y-1/2 text-[#5C5C5C]" />
              <input
                type={showPassword ? 'text' : 'password'}
                value={password}
                onChange={(e) => {
                  setPassword(e.target.value);
                  handleInputChange();
                }}
                placeholder="••••••••"
                className="w-full pl-12 pr-12 py-4 bg-[#1A1A1A] border border-[#2D2D2D] rounded-xl text-white placeholder-[#5C5C5C] focus:outline-none focus:border-[#C9A962] transition-colors"
                required
                minLength={6}
              />
              <button
                type="button"
                onClick={() => setShowPassword(!showPassword)}
                className="absolute right-4 top-1/2 -translate-y-1/2 text-[#5C5C5C] hover:text-white transition-colors"
              >
                {showPassword ? <EyeOff size={18} strokeWidth={1.5} /> : <Eye size={18} strokeWidth={1.5} />}
              </button>
            </div>
          </div>

          {/* Remember Me */}
          <div className="flex items-center justify-between">
            <label className="flex items-center gap-2 cursor-pointer">
              <input
                type="checkbox"
                checked={rememberMe}
                onChange={(e) => setRememberMe(e.target.checked)}
                className="w-4 h-4 rounded border-[#2D2D2D] bg-[#1A1A1A] text-[#C9A962] focus:ring-[#C9A962] focus:ring-offset-0"
              />
              <span className="text-sm text-[#8C8C8C]">Lembrar de mim</span>
            </label>
            <a href="#" className="text-sm text-[#C9A962] hover:underline">
              Esqueceu a senha?
            </a>
          </div>

          {/* Submit */}
          <button
            type="submit"
            disabled={isLoading}
            className="w-full py-4 bg-gradient-to-r from-[#C9A962] to-[#8B6914] text-black font-semibold rounded-xl hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? (
              <span className="flex items-center justify-center gap-2">
                <svg className="animate-spin h-5 w-5" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z" />
                </svg>
                Entrando...
              </span>
            ) : (
              'Entrar'
            )}
          </button>
        </form>

        {/* Footer */}
        <p className="text-center text-[#5C5C5C] text-sm mt-8">
          Não tem uma conta?{' '}
          <a href="#" className="text-[#C9A962] hover:underline">
            Fale conosco
          </a>
        </p>

        {/* Development Helper */}
        {process.env.NODE_ENV === 'development' && (
          <div className="mt-6 p-4 bg-[#1A1A1A] border border-[#2D2D2D] rounded-xl">
            <p className="text-[#5C5C5C] text-xs mb-3">Demo Login (Dev Only)</p>
            <div className="space-y-2">
              <button
                type="button"
                onClick={() => {
                  setEmail('admin@zendapag.com');
                  setPassword('admin123');
                  setRememberMe(true);
                }}
                className="w-full py-2 text-sm text-[#C9A962] bg-[#2D2D2D] rounded-lg hover:bg-[#3D3D3D] transition-colors"
              >
                Preencher como Admin
              </button>
              <button
                type="button"
                onClick={() => {
                  setEmail('merchant@example.com');
                  setPassword('merchant123');
                  setRememberMe(false);
                }}
                className="w-full py-2 text-sm text-[#8C8C8C] bg-[#2D2D2D] rounded-lg hover:bg-[#3D3D3D] transition-colors"
              >
                Preencher como Merchant
              </button>
            </div>
          </div>
        )}

        {/* Copyright */}
        <div className="text-center mt-8">
          <p className="text-[#5C5C5C] text-xs">
            © 2024 ZendPag. Todos os direitos reservados.
          </p>
        </div>
      </div>
    </div>
  );
};

export default LoginPage;
