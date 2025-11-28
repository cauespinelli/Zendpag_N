// @ts-nocheck
import React from 'react';
import {
  Home,
  CreditCard,
  TrendingUp,
  BarChart3,
  Settings,
  Code,
  Webhook,
  FileText,
  User,
  Receipt,
  Wallet
} from 'lucide-react';

// Lazy load pages
const DashboardPage = React.lazy(() => import('@/pages/DashboardPage'));
const PaymentsPage = React.lazy(() => import('@/pages/PaymentsPage'));
const PaymentDetailsPage = React.lazy(() => import('@/pages/PaymentDetailsPage'));
const TransactionsPage = React.lazy(() => import('@/pages/TransactionsPage'));
const WithdrawalsPage = React.lazy(() => import('@/pages/WithdrawalsPage'));

// Old Analytics and Settings pages
const AnalyticsPageOld = React.lazy(() => import('@/pages/AnalyticsPage'));
const SettingsPageOld = React.lazy(() => import('@/pages/SettingsPage'));

// New Dashboard Pages
const Analytics = React.lazy(() => import('@/pages/dashboard/Analytics'));
const APIPage = React.lazy(() => import('@/pages/dashboard/API'));
const SettingsNew = React.lazy(() => import('@/pages/dashboard/Settings'));

// Other pages
const WebhooksPage = React.lazy(() => import('@/pages/WebhooksPage'));
const ReportsPage = React.lazy(() => import('@/pages/ReportsPage'));
const ProfilePage = React.lazy(() => import('@/pages/ProfilePage'));

// Public pages
const LoginPage = React.lazy(() => import('@/pages/LoginPage'));
const CheckoutPage = React.lazy(() => import('@/pages/CheckoutPage'));
const NotFoundPage = React.lazy(() => import('@/pages/NotFoundPage'));

/**
 * Route configuration type
 */
export interface RouteConfig {
  path: string;
  element: React.LazyExoticComponent<React.ComponentType<any>>;
  name: string;
  icon?: React.ComponentType<any>;
  showInNav?: boolean;
  requiredPermissions?: string[];
  requiredRoles?: string[];
  children?: RouteConfig[];
}

/**
 * Public routes (no authentication required)
 */
export const publicRoutes: RouteConfig[] = [
  {
    path: '/login',
    element: LoginPage,
    name: 'Login',
    showInNav: false,
  },
  {
    path: '/checkout',
    element: CheckoutPage,
    name: 'Checkout',
    showInNav: false,
  },
  {
    path: '*',
    element: NotFoundPage,
    name: '404',
    showInNav: false,
  },
];

/**
 * Protected routes (authentication required)
 */
export const protectedRoutes: RouteConfig[] = [
  {
    path: 'dashboard',
    element: DashboardPage,
    name: 'Dashboard',
    icon: Home,
    showInNav: true,
  },
  {
    path: 'payments',
    element: PaymentsPage,
    name: 'Pagamentos',
    icon: CreditCard,
    showInNav: true,
  },
  {
    path: 'payments/:id',
    element: PaymentDetailsPage,
    name: 'Detalhes do Pagamento',
    showInNav: false,
  },
  {
    path: 'transactions',
    element: TransactionsPage,
    name: 'Transações',
    icon: Receipt,
    showInNav: true,
  },
  {
    path: 'withdrawals',
    element: WithdrawalsPage,
    name: 'Saques',
    icon: Wallet,
    showInNav: true,
  },
  {
    path: 'analytics',
    element: Analytics,
    name: 'Analytics',
    icon: TrendingUp,
    showInNav: true,
  },
  {
    path: 'api',
    element: APIPage,
    name: 'API',
    icon: Code,
    showInNav: true,
  },
  {
    path: 'webhooks',
    element: WebhooksPage,
    name: 'Webhooks',
    icon: Webhook,
    showInNav: true,
  },
  {
    path: 'reports',
    element: ReportsPage,
    name: 'Relatórios',
    icon: FileText,
    showInNav: true,
  },
  {
    path: 'profile',
    element: ProfilePage,
    name: 'Perfil',
    icon: User,
    showInNav: false,
  },
  {
    path: 'settings',
    element: SettingsNew,
    name: 'Configurações',
    icon: Settings,
    showInNav: true,
  },
];

/**
 * Navigation menu structure
 */
export const navigationMenu = [
  {
    label: 'Principal',
    items: [
      {
        path: '/dashboard',
        name: 'Dashboard',
        icon: Home,
      },
      {
        path: '/analytics',
        name: 'Analytics',
        icon: TrendingUp,
      },
    ],
  },
  {
    label: 'Financeiro',
    items: [
      {
        path: '/payments',
        name: 'Pagamentos',
        icon: CreditCard,
      },
      {
        path: '/transactions',
        name: 'Transações',
        icon: Receipt,
      },
      {
        path: '/withdrawals',
        name: 'Saques',
        icon: Wallet,
      },
    ],
  },
  {
    label: 'Desenvolvimento',
    items: [
      {
        path: '/api',
        name: 'API',
        icon: Code,
      },
      {
        path: '/webhooks',
        name: 'Webhooks',
        icon: Webhook,
      },
    ],
  },
  {
    label: 'Gestão',
    items: [
      {
        path: '/reports',
        name: 'Relatórios',
        icon: FileText,
      },
      {
        path: '/settings',
        name: 'Configurações',
        icon: Settings,
      },
    ],
  },
];

/**
 * Get navigation items for sidebar
 */
export const getNavigationItems = () => {
  return protectedRoutes
    .filter(route => route.showInNav)
    .map(route => ({
      path: `/${route.path}`,
      name: route.name,
      icon: route.icon,
    }));
};

/**
 * Get route by path
 */
export const getRouteByPath = (path: string): RouteConfig | undefined => {
  const allRoutes = [...publicRoutes, ...protectedRoutes];
  return allRoutes.find(route => route.path === path || `/${route.path}` === path);
};

/**
 * Check if user has permission to access route
 */
export const canAccessRoute = (
  route: RouteConfig,
  userPermissions: string[] = [],
  userRoles: string[] = []
): boolean => {
  // No restrictions, allow access
  if (!route.requiredPermissions && !route.requiredRoles) {
    return true;
  }

  // Check permissions
  if (route.requiredPermissions) {
    const hasPermissions = route.requiredPermissions.every(
      permission => userPermissions.includes(permission) || userRoles.includes('ADMIN')
    );
    if (!hasPermissions) return false;
  }

  // Check roles
  if (route.requiredRoles) {
    const hasRoles = route.requiredRoles.some(role => userRoles.includes(role));
    if (!hasRoles) return false;
  }

  return true;
};

export default {
  publicRoutes,
  protectedRoutes,
  navigationMenu,
  getNavigationItems,
  getRouteByPath,
  canAccessRoute,
};
