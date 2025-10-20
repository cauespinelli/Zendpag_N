// @ts-nocheck
import React, { useEffect } from 'react';
import { Navigate } from 'react-router-dom';
import { Spin } from 'antd';
import { useAuthStore } from '@/store/authStore';
import LoadingScreen from './LoadingScreen';

interface ProtectedRouteProps {
  children: React.ReactNode;
  requiredPermissions?: string[];
  requiredRoles?: string[];
  redirectTo?: string;
}

const ProtectedRoute: React.FC<ProtectedRouteProps> = ({
  children,
  requiredPermissions = [],
  requiredRoles = [],
  redirectTo = '/login'
}) => {
  const {
    isAuthenticated,
    user,
    isLoading,
    getCurrentUser,
    token
  } = useAuthStore();

  // Try to get current user if authenticated but no user data
  useEffect(() => {
    if (isAuthenticated && !user && !isLoading && token) {
      getCurrentUser().catch(() => {
        // Error handled in auth store
      });
    }
  }, [isAuthenticated, user, isLoading, token, getCurrentUser]);

  // Show loading while checking authentication or fetching user
  if (isLoading || (isAuthenticated && !user && token)) {
    return <LoadingScreen tip="Verificando autenticação..." />;
  }

  // Redirect to login if not authenticated
  if (!isAuthenticated || !user) {
    return <Navigate to={redirectTo} replace />;
  }

  // Check required permissions
  if (requiredPermissions.length > 0) {
    const hasPermissions = requiredPermissions.every(permission =>
      user.permissions.includes(permission) || user.roles.includes('ADMIN')
    );

    if (!hasPermissions) {
      return (
        <Navigate
          to="/dashboard"
          replace
          state={{ error: 'Você não tem permissão para acessar esta página.' }}
        />
      );
    }
  }

  // Check required roles
  if (requiredRoles.length > 0) {
    const hasRoles = requiredRoles.some(role => user.roles.includes(role));

    if (!hasRoles) {
      return (
        <Navigate
          to="/dashboard"
          replace
          state={{ error: 'Você não tem o nível de acesso necessário para esta página.' }}
        />
      );
    }
  }

  return <>{children}</>;
};

// HOC for component-level protection
export const withAuth = <P extends object>(
  Component: React.ComponentType<P>,
  options?: {
    requiredPermissions?: string[];
    requiredRoles?: string[];
    redirectTo?: string;
  }
) => {
  const AuthenticatedComponent = (props: P) => (
    <ProtectedRoute {...options}>
      <Component {...props} />
    </ProtectedRoute>
  );

  AuthenticatedComponent.displayName = `withAuth(${Component.displayName || Component.name})`;

  return AuthenticatedComponent;
};

// Hook for conditional rendering based on permissions
export const usePermissions = () => {
  const user = useAuthStore(state => state.user);

  const hasPermission = (permission: string): boolean => {
    if (!user) return false;
    return user.permissions.includes(permission) || user.roles.includes('ADMIN');
  };

  const hasRole = (role: string): boolean => {
    if (!user) return false;
    return user.roles.includes(role);
  };

  const hasAnyPermission = (permissions: string[]): boolean => {
    if (!user) return false;
    return permissions.some(permission => hasPermission(permission));
  };

  const hasAnyRole = (roles: string[]): boolean => {
    if (!user) return false;
    return roles.some(role => hasRole(role));
  };

  const hasAllPermissions = (permissions: string[]): boolean => {
    if (!user) return false;
    return permissions.every(permission => hasPermission(permission));
  };

  const hasAllRoles = (roles: string[]): boolean => {
    if (!user) return false;
    return roles.every(role => hasRole(role));
  };

  return {
    user,
    hasPermission,
    hasRole,
    hasAnyPermission,
    hasAnyRole,
    hasAllPermissions,
    hasAllRoles,
    isAdmin: hasRole('ADMIN'),
    isManager: hasRole('MANAGER'),
    isUser: hasRole('USER'),
  };
};

export default ProtectedRoute;