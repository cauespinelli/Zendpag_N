# 🚀 Router Setup - ZendaPag Dashboard

## 📋 Visão Geral

Este documento descreve a arquitetura completa de roteamento do ZendaPag Dashboard, incluindo rotas públicas, protegidas, navegação e configuração.

## 🗂️ Estrutura de Arquivos

```
src/
├── config/
│   └── routes.tsx              # Configuração centralizada de rotas
├── components/
│   ├── DashboardLayout.tsx     # Layout principal com sidebar
│   ├── DashboardLayout.v2.tsx  # Nova versão com navegação dinâmica
│   └── ProtectedRoute.tsx      # HOC para rotas protegidas
├── pages/
│   ├── dashboard/              # Páginas do dashboard
│   │   ├── Analytics.tsx
│   │   ├── API.tsx
│   │   └── Settings.tsx
│   ├── DashboardPage.tsx
│   ├── PaymentsPage.tsx
│   ├── TransactionsPage.tsx
│   ├── WithdrawalsPage.tsx
│   ├── WebhooksPage.tsx
│   ├── ReportsPage.tsx
│   ├── ProfilePage.tsx
│   ├── LoginPage.tsx
│   ├── CheckoutPage.tsx
│   └── NotFoundPage.tsx
├── App.tsx                     # Configuração original
└── App.v2.tsx                  # Nova versão com rotas dinâmicas
```

## 🛣️ Rotas Disponíveis

### Rotas Públicas

| Rota | Componente | Descrição |
|------|-----------|-----------|
| `/login` | `LoginPage` | Página de autenticação |
| `/checkout` | `CheckoutPage` | Página de checkout (pública) |
| `*` | `NotFoundPage` | Página 404 |

### Rotas Protegidas

| Rota | Componente | Ícone | Menu |
|------|-----------|-------|------|
| `/dashboard` | `DashboardPage` | Home | ✅ |
| `/payments` | `PaymentsPage` | CreditCard | ✅ |
| `/payments/:id` | `PaymentDetailsPage` | - | ❌ |
| `/transactions` | `TransactionsPage` | Receipt | ✅ |
| `/withdrawals` | `WithdrawalsPage` | Wallet | ✅ |
| `/analytics` | `Analytics` | TrendingUp | ✅ |
| `/api` | `APIPage` | Code | ✅ |
| `/webhooks` | `WebhooksPage` | Webhook | ✅ |
| `/reports` | `ReportsPage` | FileText | ✅ |
| `/profile` | `ProfilePage` | User | ❌ |
| `/settings` | `Settings` | Settings | ✅ |

## 📱 Estrutura do Menu de Navegação

```typescript
// src/config/routes.tsx

export const navigationMenu = [
  {
    label: 'Principal',
    items: [
      { path: '/dashboard', name: 'Dashboard', icon: Home },
      { path: '/analytics', name: 'Analytics', icon: TrendingUp },
    ],
  },
  {
    label: 'Financeiro',
    items: [
      { path: '/payments', name: 'Pagamentos', icon: CreditCard },
      { path: '/transactions', name: 'Transações', icon: Receipt },
      { path: '/withdrawals', name: 'Saques', icon: Wallet },
    ],
  },
  {
    label: 'Desenvolvimento',
    items: [
      { path: '/api', name: 'API', icon: Code },
      { path: '/webhooks', name: 'Webhooks', icon: Webhook },
    ],
  },
  {
    label: 'Gestão',
    items: [
      { path: '/reports', name: 'Relatórios', icon: FileText },
      { path: '/settings', name: 'Configurações', icon: Settings },
    ],
  },
];
```

## 🔒 Proteção de Rotas

### ProtectedRoute Component

O componente `ProtectedRoute` fornece proteção baseada em autenticação e permissões:

```typescript
<ProtectedRoute
  requiredPermissions={['payment.read']}
  requiredRoles={['ADMIN', 'MANAGER']}
  redirectTo="/login"
>
  <YourComponent />
</ProtectedRoute>
```

### Funcionalidades

- ✅ Verificação de autenticação
- ✅ Verificação de permissões
- ✅ Verificação de roles
- ✅ Redirecionamento automático
- ✅ Loading state durante verificação

### Hooks Disponíveis

```typescript
import { usePermissions } from '@/components/ProtectedRoute';

const MyComponent = () => {
  const {
    hasPermission,
    hasRole,
    hasAnyPermission,
    hasAnyRole,
    isAdmin,
    isManager,
  } = usePermissions();

  if (!hasPermission('payment.read')) {
    return <div>Sem permissão</div>;
  }

  return <div>Conteúdo protegido</div>;
};
```

## 🎨 Configuração de Rotas

### Como Adicionar uma Nova Rota

1. **Criar a página:**
```typescript
// src/pages/MyNewPage.tsx
export default function MyNewPage() {
  return <div>Minha Nova Página</div>;
}
```

2. **Adicionar na configuração:**
```typescript
// src/config/routes.tsx
const MyNewPage = React.lazy(() => import('@/pages/MyNewPage'));

export const protectedRoutes: RouteConfig[] = [
  // ... outras rotas
  {
    path: 'my-new-page',
    element: MyNewPage,
    name: 'Minha Nova Página',
    icon: MyIcon,
    showInNav: true,
    requiredPermissions: ['my-permission'], // opcional
    requiredRoles: ['ADMIN'], // opcional
  },
];
```

3. **Adicionar ao menu (opcional):**
```typescript
// src/config/routes.tsx
export const navigationMenu = [
  // ...
  {
    label: 'Minha Seção',
    items: [
      { path: '/my-new-page', name: 'Minha Nova Página', icon: MyIcon },
    ],
  },
];
```

## 🔄 Migração para Nova Versão

### Passo 1: Backup dos Arquivos Atuais
```bash
cd /c/projetos/zendapag/zendapag-dashboard/src
cp App.tsx App.backup.tsx
cp components/DashboardLayout.tsx components/DashboardLayout.backup.tsx
```

### Passo 2: Substituir pelos Novos Arquivos
```bash
# Substituir App.tsx
cp App.v2.tsx App.tsx

# Substituir DashboardLayout.tsx
cp components/DashboardLayout.v2.tsx components/DashboardLayout.tsx
```

### Passo 3: Testar a Aplicação
```bash
npm run dev
```

### Passo 4: Verificar Navegação
- [ ] Login funciona corretamente
- [ ] Redirecionamento após login
- [ ] Todas as rotas são acessíveis
- [ ] Menu lateral está correto
- [ ] Proteção de rotas funciona
- [ ] Permissões estão corretas

## 📚 Exemplos de Uso

### Navegação Programática

```typescript
import { useNavigate } from 'react-router-dom';

const MyComponent = () => {
  const navigate = useNavigate();

  const handleClick = () => {
    navigate('/payments');
  };

  return <button onClick={handleClick}>Ver Pagamentos</button>;
};
```

### Links de Navegação

```typescript
import { Link } from 'react-router-dom';

const MyComponent = () => {
  return (
    <Link to="/analytics">
      Ver Analytics
    </Link>
  );
};
```

### Verificação de Rota Atual

```typescript
import { useLocation } from 'react-router-dom';

const MyComponent = () => {
  const location = useLocation();
  const isActive = location.pathname === '/dashboard';

  return <div className={isActive ? 'active' : ''}>Dashboard</div>;
};
```

## 🎯 Boas Práticas

### ✅ Faça

- Use lazy loading para todas as páginas
- Centralize a configuração de rotas em `routes.tsx`
- Use o componente `ProtectedRoute` para rotas protegidas
- Defina permissões e roles quando necessário
- Mantenha o menu de navegação sincronizado com as rotas

### ❌ Evite

- Hardcode de rotas em múltiplos lugares
- Duplicação de configuração
- Rotas públicas sem proteção adequada
- Navegação sem validação de permissões

## 🔧 Configurações Avançadas

### Lazy Loading com Preload

```typescript
const PreloadedPage = React.lazy(() => {
  const promise = import('@/pages/MyPage');
  // Preload após 2 segundos
  setTimeout(() => promise, 2000);
  return promise;
});
```

### Rotas Aninhadas

```typescript
{
  path: 'settings',
  element: SettingsPage,
  children: [
    { path: 'profile', element: ProfileSettings },
    { path: 'security', element: SecuritySettings },
  ],
}
```

### Redirect Condicional

```typescript
const ConditionalRedirect = () => {
  const { hasPermission } = usePermissions();

  if (!hasPermission('admin.access')) {
    return <Navigate to="/dashboard" replace />;
  }

  return <AdminPage />;
};
```

## 📊 Performance

### Code Splitting

Todas as páginas usam `React.lazy()` para code splitting automático:

```typescript
const Analytics = React.lazy(() => import('@/pages/dashboard/Analytics'));
```

### Suspense Boundaries

```typescript
<Suspense fallback={<LoadingScreen />}>
  <Routes>
    {/* rotas */}
  </Routes>
</Suspense>
```

## 🐛 Troubleshooting

### Problema: Rota não encontrada (404)
**Solução:** Verifique se a rota está registrada em `routes.tsx`

### Problema: Redirecionamento infinito
**Solução:** Verifique as condições de autenticação e permissões

### Problema: Menu não atualiza
**Solução:** Certifique-se de que `showInNav: true` está definido

### Problema: Ícones não aparecem
**Solução:** Verifique se os ícones do lucide-react estão importados corretamente

## 📝 Changelog

### v2.0 (Nova Versão)
- ✨ Configuração centralizada de rotas
- ✨ Navegação dinâmica baseada em configuração
- ✨ Suporte a permissões e roles
- ✨ Menu agrupado por seções
- ✨ Lazy loading de todas as páginas
- ✨ Hooks utilitários de permissões

### v1.0 (Versão Original)
- ✅ Rotas básicas
- ✅ Proteção de autenticação
- ✅ Menu lateral estático

## 🤝 Contribuindo

Para adicionar novas rotas ou melhorar a navegação:

1. Edite `src/config/routes.tsx`
2. Adicione a página correspondente
3. Atualize esta documentação
4. Teste a navegação completa

## 📞 Suporte

Para dúvidas ou problemas:
- Verifique esta documentação
- Revise o código em `src/config/routes.tsx`
- Consulte a equipe de desenvolvimento

---

**Última atualização:** 2024-11-13
**Versão:** 2.0
**Autor:** ZendaPag Team
