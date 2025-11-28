// @ts-nocheck
import React, { Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp } from 'antd';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ReactQueryDevtools } from '@tanstack/react-query-devtools';
import { ErrorBoundary } from 'react-error-boundary';
import ptBR from 'antd/locale/pt_BR';
import enUS from 'antd/locale/en_US';

// Store imports
import { useAuthStore } from '@/store/authStore';
import { themeSelectors } from '@/store/themeStore';

// Component imports
import ErrorFallback from '@/components/ErrorFallback';
import LoadingScreen from '@/components/LoadingScreen';
import ProtectedRoute from '@/components/ProtectedRoute';
import DashboardLayout from '@/components/DashboardLayout';

// Route configuration
import { publicRoutes, protectedRoutes } from '@/config/routes';

// React Query client configuration
const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      refetchOnWindowFocus: false,
      refetchOnReconnect: true,
      retry: (failureCount, error: any) => {
        // Don't retry on 4xx errors (except 429)
        if (error?.response?.status >= 400 && error?.response?.status < 500 && error?.response?.status !== 429) {
          return false;
        }
        return failureCount < 3;
      },
      staleTime: 5 * 60 * 1000, // 5 minutes
      gcTime: 10 * 60 * 1000, // 10 minutes
    },
    mutations: {
      retry: 1,
    },
  },
});

const App: React.FC = () => {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);
  const getCurrentUser = useAuthStore((state) => state.getCurrentUser);
  const themeConfig = themeSelectors.useThemeConfig();
  const language = themeSelectors.useLanguage();
  const cssVariables = themeSelectors.useCSSVariables();

  // Initialize auth on app start
  useEffect(() => {
    if (isAuthenticated) {
      getCurrentUser().catch(() => {
        // Error is handled in auth store
      });
    }
  }, [isAuthenticated, getCurrentUser]);

  // Apply CSS variables to document
  useEffect(() => {
    const root = document.documentElement;
    Object.entries(cssVariables).forEach(([key, value]) => {
      root.style.setProperty(key, value);
    });
  }, [cssVariables]);

  // Remove loading screen on app mount
  useEffect(() => {
    const loadingElement = document.getElementById('loading');
    if (loadingElement) {
      loadingElement.style.display = 'none';
    }
  }, []);

  const locale = language === 'pt-BR' ? ptBR : enUS;

  return (
    <ErrorBoundary FallbackComponent={ErrorFallback}>
      <QueryClientProvider client={queryClient}>
        <ConfigProvider theme={themeConfig} locale={locale}>
          <AntApp>
            <BrowserRouter>
              <Suspense fallback={<LoadingScreen />}>
                <Routes>
                  {/* Public routes */}
                  {publicRoutes.map((route) => {
                    const Component = route.element;

                    // Special handling for login route
                    if (route.path === '/login') {
                      return (
                        <Route
                          key={route.path}
                          path={route.path}
                          element={
                            isAuthenticated ? (
                              <Navigate to="/dashboard" replace />
                            ) : (
                              <Component />
                            )
                          }
                        />
                      );
                    }

                    // Special handling for checkout (public but may need session)
                    if (route.path === '/checkout') {
                      return (
                        <Route
                          key={route.path}
                          path={route.path}
                          element={<Component />}
                        />
                      );
                    }

                    return null; // 404 route handled separately
                  })}

                  {/* Protected routes */}
                  <Route
                    path="/"
                    element={
                      <ProtectedRoute>
                        <DashboardLayout />
                      </ProtectedRoute>
                    }
                  >
                    {/* Redirect root to dashboard */}
                    <Route index element={<Navigate to="/dashboard" replace />} />

                    {/* Dynamic protected routes */}
                    {protectedRoutes.map((route) => {
                      const Component = route.element;
                      return (
                        <Route
                          key={route.path}
                          path={route.path}
                          element={
                            <ProtectedRoute
                              requiredPermissions={route.requiredPermissions}
                              requiredRoles={route.requiredRoles}
                            >
                              <Component />
                            </ProtectedRoute>
                          }
                        />
                      );
                    })}
                  </Route>

                  {/* 404 page */}
                  <Route path="*" element={<Suspense fallback={<LoadingScreen />}><NotFoundPage /></Suspense>} />
                </Routes>
              </Suspense>
            </BrowserRouter>

            {/* React Query DevTools in development */}
            {process.env.NODE_ENV === 'development' && (
              <ReactQueryDevtools initialIsOpen={false} />
            )}
          </AntApp>
        </ConfigProvider>
      </QueryClientProvider>
    </ErrorBoundary>
  );
};

// Import NotFoundPage for 404 route
const NotFoundPage = React.lazy(() => import('@/pages/NotFoundPage'));

export default App;
