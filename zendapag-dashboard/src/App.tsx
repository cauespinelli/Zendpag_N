// @ts-nocheck
import React, { Suspense, useEffect } from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider, App as AntApp, Spin } from 'antd';
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

// Lazy load pages for code splitting
const LoginPage = React.lazy(() => import('@/pages/LoginPage'));
const DashboardLayout = React.lazy(() => import('@/components/DashboardLayout'));
const DashboardPage = React.lazy(() => import('@/pages/DashboardPage'));
const PaymentsPage = React.lazy(() => import('@/pages/PaymentsPage'));
const PaymentDetailsPage = React.lazy(() => import('@/pages/PaymentDetailsPage'));
const AnalyticsPage = React.lazy(() => import('@/pages/AnalyticsPage'));
const WebhooksPage = React.lazy(() => import('@/pages/WebhooksPage'));
const ReportsPage = React.lazy(() => import('@/pages/ReportsPage'));
const ProfilePage = React.lazy(() => import('@/pages/ProfilePage'));
const SettingsPage = React.lazy(() => import('@/pages/SettingsPage'));
const NotFoundPage = React.lazy(() => import('@/pages/NotFoundPage'));

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
                  <Route
                    path="/login"
                    element={
                      isAuthenticated ? (
                        <Navigate to="/dashboard" replace />
                      ) : (
                        <LoginPage />
                      )
                    }
                  />

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

                    {/* Main pages */}
                    <Route path="dashboard" element={<DashboardPage />} />
                    <Route path="payments" element={<PaymentsPage />} />
                    <Route path="payments/:id" element={<PaymentDetailsPage />} />
                    <Route path="analytics" element={<AnalyticsPage />} />
                    <Route path="webhooks" element={<WebhooksPage />} />
                    <Route path="reports" element={<ReportsPage />} />
                    <Route path="profile" element={<ProfilePage />} />
                    <Route path="settings" element={<SettingsPage />} />
                  </Route>

                  {/* 404 page */}
                  <Route path="*" element={<NotFoundPage />} />
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

export default App;