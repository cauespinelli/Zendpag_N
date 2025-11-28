// @ts-nocheck
/**
 * Utility functions to test and validate routes
 */

import { publicRoutes, protectedRoutes, canAccessRoute } from '@/config/routes';

/**
 * Test all routes configuration
 */
export const testRoutesConfig = () => {
  console.group('🧪 Testing Routes Configuration');

  // Test public routes
  console.group('📖 Public Routes');
  publicRoutes.forEach((route) => {
    console.log(`✓ ${route.path} - ${route.name}`);
  });
  console.groupEnd();

  // Test protected routes
  console.group('🔒 Protected Routes');
  protectedRoutes.forEach((route) => {
    const permissions = route.requiredPermissions ? `[${route.requiredPermissions.join(', ')}]` : 'none';
    const roles = route.requiredRoles ? `[${route.requiredRoles.join(', ')}]` : 'none';
    console.log(`✓ /${route.path} - ${route.name} | Permissions: ${permissions} | Roles: ${roles}`);
  });
  console.groupEnd();

  console.groupEnd();
};

/**
 * Get all available routes
 */
export const getAllRoutes = () => {
  return {
    public: publicRoutes.map(r => ({ path: r.path, name: r.name })),
    protected: protectedRoutes.map(r => ({
      path: `/${r.path}`,
      name: r.name,
      showInNav: r.showInNav
    })),
  };
};

/**
 * Check if path exists in routes config
 */
export const routeExists = (path: string): boolean => {
  const allRoutes = [...publicRoutes, ...protectedRoutes];
  return allRoutes.some(route =>
    route.path === path ||
    `/${route.path}` === path ||
    route.path === path.replace('/', '')
  );
};

/**
 * Get route information
 */
export const getRouteInfo = (path: string) => {
  const allRoutes = [...publicRoutes, ...protectedRoutes];
  const route = allRoutes.find(r =>
    r.path === path ||
    `/${r.path}` === path ||
    r.path === path.replace('/', '')
  );

  if (!route) return null;

  return {
    path: route.path,
    name: route.name,
    showInNav: route.showInNav,
    hasPermissions: !!route.requiredPermissions,
    hasRoles: !!route.requiredRoles,
    permissions: route.requiredPermissions || [],
    roles: route.requiredRoles || [],
  };
};

/**
 * Validate user access to all routes
 */
export const validateUserAccess = (
  userPermissions: string[] = [],
  userRoles: string[] = []
) => {
  console.group('👤 User Access Validation');
  console.log('User Permissions:', userPermissions);
  console.log('User Roles:', userRoles);

  const accessible: string[] = [];
  const restricted: string[] = [];

  protectedRoutes.forEach((route) => {
    const canAccess = canAccessRoute(route, userPermissions, userRoles);
    if (canAccess) {
      accessible.push(`/${route.path}`);
    } else {
      restricted.push(`/${route.path}`);
    }
  });

  console.log('✅ Accessible Routes:', accessible);
  console.log('❌ Restricted Routes:', restricted);
  console.groupEnd();

  return { accessible, restricted };
};

/**
 * Get navigation tree
 */
export const getNavigationTree = () => {
  return protectedRoutes
    .filter(route => route.showInNav)
    .map(route => ({
      path: `/${route.path}`,
      name: route.name,
      icon: route.icon?.name || 'Unknown',
    }));
};

/**
 * Print route map for debugging
 */
export const printRouteMap = () => {
  console.group('🗺️ Complete Route Map');

  console.group('Public Routes');
  publicRoutes.forEach(route => {
    console.log(`${route.path.padEnd(20)} → ${route.name}`);
  });
  console.groupEnd();

  console.group('Protected Routes');
  protectedRoutes.forEach(route => {
    const nav = route.showInNav ? '📍' : '  ';
    console.log(`${nav} /${route.path.padEnd(20)} → ${route.name}`);
  });
  console.groupEnd();

  console.groupEnd();
};

/**
 * Test route permissions
 */
export const testRoutePermissions = () => {
  console.group('🔐 Route Permissions Test');

  // Test cases
  const testCases = [
    {
      name: 'Admin User',
      permissions: ['payment.read', 'payment.write', 'user.read'],
      roles: ['ADMIN'],
    },
    {
      name: 'Manager User',
      permissions: ['payment.read', 'user.read'],
      roles: ['MANAGER'],
    },
    {
      name: 'Regular User',
      permissions: ['payment.read'],
      roles: ['USER'],
    },
    {
      name: 'No Permissions',
      permissions: [],
      roles: [],
    },
  ];

  testCases.forEach(testCase => {
    console.group(`Testing: ${testCase.name}`);
    validateUserAccess(testCase.permissions, testCase.roles);
    console.groupEnd();
  });

  console.groupEnd();
};

/**
 * Route statistics
 */
export const getRouteStats = () => {
  const stats = {
    total: publicRoutes.length + protectedRoutes.length,
    public: publicRoutes.length,
    protected: protectedRoutes.length,
    withNavigation: protectedRoutes.filter(r => r.showInNav).length,
    withPermissions: protectedRoutes.filter(r => r.requiredPermissions).length,
    withRoles: protectedRoutes.filter(r => r.requiredRoles).length,
    lazyLoaded: publicRoutes.length + protectedRoutes.length, // All are lazy loaded
  };

  console.table(stats);
  return stats;
};

/**
 * Find routes by permission
 */
export const findRoutesByPermission = (permission: string) => {
  return protectedRoutes.filter(route =>
    route.requiredPermissions?.includes(permission)
  ).map(route => ({
    path: `/${route.path}`,
    name: route.name,
  }));
};

/**
 * Find routes by role
 */
export const findRoutesByRole = (role: string) => {
  return protectedRoutes.filter(route =>
    route.requiredRoles?.includes(role)
  ).map(route => ({
    path: `/${route.path}`,
    name: route.name,
  }));
};

// Development helper - auto-run tests in development
if (process.env.NODE_ENV === 'development') {
  // Expose to window for console access
  if (typeof window !== 'undefined') {
    (window as any).testRoutes = {
      testConfig: testRoutesConfig,
      getAllRoutes,
      routeExists,
      getRouteInfo,
      validateUserAccess,
      getNavigationTree,
      printRouteMap,
      testPermissions: testRoutePermissions,
      getStats: getRouteStats,
      findByPermission: findRoutesByPermission,
      findByRole: findRoutesByRole,
    };

    console.log(
      '%c🚀 Route Testing Utilities Loaded',
      'background: #0066FF; color: white; padding: 4px 8px; border-radius: 4px; font-weight: bold;'
    );
    console.log(
      '%cUse window.testRoutes.* to access route testing functions',
      'color: #6B7280; font-style: italic;'
    );
    console.log('%cExample: window.testRoutes.getStats()', 'color: #10B981;');
  }
}

export default {
  testRoutesConfig,
  getAllRoutes,
  routeExists,
  getRouteInfo,
  validateUserAccess,
  getNavigationTree,
  printRouteMap,
  testRoutePermissions,
  getRouteStats,
  findRoutesByPermission,
  findRoutesByRole,
};
