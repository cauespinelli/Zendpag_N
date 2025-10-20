// @ts-nocheck
import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';
import { persist } from 'zustand/middleware';
import apiService from '@/services/api';
import { storage } from '@/utils/helpers';
import { STORAGE_KEYS, API_ENDPOINTS } from '@/utils/constants';
import type { User, LoginRequest, LoginResponse } from '@/types';

interface AuthState {
  user: User | null;
  token: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
}

interface AuthActions {
  login: (credentials: LoginRequest) => Promise<void>;
  logout: () => Promise<void>;
  refreshAuth: () => Promise<void>;
  getCurrentUser: () => Promise<void>;
  updateUser: (userData: Partial<User>) => void;
  clearError: () => void;
  setLoading: (loading: boolean) => void;
}

type AuthStore = AuthState & AuthActions;

const initialState: AuthState = {
  user: null,
  token: null,
  refreshToken: null,
  isAuthenticated: false,
  isLoading: false,
  error: null,
};

export const useAuthStore = create<AuthStore>()(
  persist(
    immer((set, get) => ({
      ...initialState,

      login: async (credentials: LoginRequest) => {
        set((state) => {
          state.isLoading = true;
          state.error = null;
        });

        try {
          const response = await apiService.post<LoginResponse>(
            API_ENDPOINTS.AUTH.LOGIN,
            credentials
          );

          const { token, refreshToken, user } = response;

          // Store tokens
          storage.set(STORAGE_KEYS.AUTH_TOKEN, token);
          storage.set(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
          storage.set(STORAGE_KEYS.USER_DATA, user);

          set((state) => {
            state.user = user;
            state.token = token;
            state.refreshToken = refreshToken;
            state.isAuthenticated = true;
            state.isLoading = false;
            state.error = null;
          });
        } catch (error: any) {
          const errorMessage = error.response?.data?.message || 'Erro ao fazer login';

          set((state) => {
            state.isLoading = false;
            state.error = errorMessage;
            state.isAuthenticated = false;
          });

          throw new Error(errorMessage);
        }
      },

      logout: async () => {
        set((state) => {
          state.isLoading = true;
        });

        try {
          // Call logout endpoint to invalidate tokens on server
          await apiService.post(API_ENDPOINTS.AUTH.LOGOUT);
        } catch (error) {
          // Continue with logout even if server request fails
          console.warn('Logout request failed:', error);
        } finally {
          // Clear all stored data
          storage.remove(STORAGE_KEYS.AUTH_TOKEN);
          storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
          storage.remove(STORAGE_KEYS.USER_DATA);

          set((state) => {
            state.user = null;
            state.token = null;
            state.refreshToken = null;
            state.isAuthenticated = false;
            state.isLoading = false;
            state.error = null;
          });

          // Redirect to login
          window.location.href = '/login';
        }
      },

      refreshAuth: async () => {
        const currentRefreshToken = get().refreshToken;

        if (!currentRefreshToken) {
          throw new Error('No refresh token available');
        }

        try {
          const response = await apiService.post<LoginResponse>(
            API_ENDPOINTS.AUTH.REFRESH,
            { refreshToken: currentRefreshToken }
          );

          const { token, refreshToken, user } = response;

          // Update stored tokens
          storage.set(STORAGE_KEYS.AUTH_TOKEN, token);
          storage.set(STORAGE_KEYS.REFRESH_TOKEN, refreshToken);
          storage.set(STORAGE_KEYS.USER_DATA, user);

          set((state) => {
            state.user = user;
            state.token = token;
            state.refreshToken = refreshToken;
            state.isAuthenticated = true;
            state.error = null;
          });
        } catch (error: any) {
          // Refresh failed, logout user
          get().logout();
          throw error;
        }
      },

      getCurrentUser: async () => {
        set((state) => {
          state.isLoading = true;
        });

        try {
          const user = await apiService.get<User>(API_ENDPOINTS.AUTH.ME);

          // Update stored user data
          storage.set(STORAGE_KEYS.USER_DATA, user);

          set((state) => {
            state.user = user;
            state.isLoading = false;
            state.error = null;
          });
        } catch (error: any) {
          const errorMessage = error.response?.data?.message || 'Erro ao carregar dados do usuário';

          set((state) => {
            state.isLoading = false;
            state.error = errorMessage;
          });

          // If unauthorized, logout user
          if (error.response?.status === 401) {
            get().logout();
          }

          throw new Error(errorMessage);
        }
      },

      updateUser: (userData: Partial<User>) => {
        set((state) => {
          if (state.user) {
            state.user = { ...state.user, ...userData };

            // Update stored user data
            storage.set(STORAGE_KEYS.USER_DATA, state.user);
          }
        });
      },

      clearError: () => {
        set((state) => {
          state.error = null;
        });
      },

      setLoading: (loading: boolean) => {
        set((state) => {
          state.isLoading = loading;
        });
      },
    })),
    {
      name: 'auth-store',
      partialize: (state) => ({
        user: state.user,
        token: state.token,
        refreshToken: state.refreshToken,
        isAuthenticated: state.isAuthenticated,
      }),
      onRehydrateStorage: () => (state) => {
        // Validate stored auth data on rehydration
        if (state && state.token) {
          try {
            // Check if token is expired (basic check)
            const tokenData = JSON.parse(atob(state.token.split('.')[1]));
            const now = Date.now() / 1000;

            if (tokenData.exp && tokenData.exp < now) {
              // Token expired, clear auth state
              state.user = null;
              state.token = null;
              state.refreshToken = null;
              state.isAuthenticated = false;

              // Clear storage
              storage.remove(STORAGE_KEYS.AUTH_TOKEN);
              storage.remove(STORAGE_KEYS.REFRESH_TOKEN);
              storage.remove(STORAGE_KEYS.USER_DATA);
            }
          } catch (error) {
            // Invalid token format, clear auth state
            console.warn('Invalid token format:', error);
            state.user = null;
            state.token = null;
            state.refreshToken = null;
            state.isAuthenticated = false;
          }
        }
      },
    }
  )
);

// Auth utility functions
export const authUtils = {
  isTokenExpired: (token: string): boolean => {
    try {
      const tokenData = JSON.parse(atob(token.split('.')[1]));
      const now = Date.now() / 1000;
      return tokenData.exp && tokenData.exp < now;
    } catch {
      return true;
    }
  },

  hasPermission: (user: User | null, permission: string): boolean => {
    if (!user) return false;
    return user.permissions.includes(permission) || user.roles.includes('ADMIN');
  },

  hasRole: (user: User | null, role: string): boolean => {
    if (!user) return false;
    return user.roles.includes(role);
  },

  getTokenExpirationTime: (token: string): Date | null => {
    try {
      const tokenData = JSON.parse(atob(token.split('.')[1]));
      return tokenData.exp ? new Date(tokenData.exp * 1000) : null;
    } catch {
      return null;
    }
  },
};

// Selectors
export const authSelectors = {
  useUser: () => useAuthStore((state) => state.user),
  useIsAuthenticated: () => useAuthStore((state) => state.isAuthenticated),
  useIsLoading: () => useAuthStore((state) => state.isLoading),
  useError: () => useAuthStore((state) => state.error),
  useToken: () => useAuthStore((state) => state.token),

  useHasPermission: (permission: string) =>
    useAuthStore((state) => authUtils.hasPermission(state.user, permission)),

  useHasRole: (role: string) =>
    useAuthStore((state) => authUtils.hasRole(state.user, role)),
};

export default useAuthStore;