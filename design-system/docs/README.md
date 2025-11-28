# Zendapag Design System

Design system completo e profissional para a plataforma Zendapag de pagamentos PIX.

## 🎨 Visão Geral

O Zendapag Design System foi criado para proporcionar uma experiência visual consistente, moderna e profissional em toda a plataforma. Inspirado nas melhores práticas de fintechs enterprise como Stripe, Linear e Witetec.

## 📦 Instalação

### Opção 1: CSS Direto

```html
<!-- No <head> do seu HTML -->
<link rel="stylesheet" href="/design-system/css/main.css">
```

### Opção 2: Import em CSS/React

```css
/* No seu arquivo CSS principal */
@import './design-system/css/main.css';
```

```javascript
// No seu arquivo React/TypeScript
import './design-system/css/main.css';
```

## 🎨 Identidade Visual

### Logo

Disponível em múltiplos formatos:

- **Logo Completo**: `/design-system/logos/logo-full-color.svg`
- **Logo Branco**: `/design-system/logos/logo-white.svg` (para fundos escuros)
- **Ícone**: `/design-system/logos/logo-icon-only.svg`
- **Favicon**: `/design-system/logos/favicon.svg`

**Conceito**: O logo representa "ZEN" (paz, simplicidade) + "PAG" (pagamentos), simbolizando pagamentos sem fricção.

### Paleta de Cores

#### Cores Primárias
- **Primary (Indigo)**: `#4F46E5` (var(--zend-primary))
- **Secondary (Green)**: `#10B981` (var(--zend-secondary))
- **Accent (Orange)**: `#F59E0B` (var(--zend-accent))

#### Cores Funcionais
- **Success**: `#10B981` (var(--zend-success))
- **Warning**: `#F59E0B` (var(--zend-warning))
- **Error**: `#EF4444` (var(--zend-error))
- **Info**: `#3B82F6` (var(--zend-info))

#### Grayscale
- 50, 100, 200, 300, 400, 500, 600, 700, 800, 900
- Formato: `var(--zend-gray-{número})`

### Tipografia

**Font Family**:
- Primary: Inter (400, 500, 600, 700, 800, 900)
- Display: Space Grotesk (500, 600, 700)

**Tamanhos**:
- xs: 12px
- sm: 14px
- base: 16px
- lg: 18px
- xl: 20px
- 2xl: 24px
- 3xl: 30px
- 4xl: 36px
- 5xl: 48px
- 6xl: 60px

**Uso**:
```css
font-size: var(--zend-text-xl);
font-weight: var(--zend-weight-semibold);
```

## 🧩 Componentes

### Botões

```html
<!-- Primary Button -->
<button class="btn btn-primary">Criar Conta</button>

<!-- Secondary Button -->
<button class="btn btn-secondary">Cancelar</button>

<!-- Ghost Button -->
<button class="btn btn-ghost">Ver mais</button>

<!-- Danger Button -->
<button class="btn btn-danger">Excluir</button>

<!-- Tamanhos -->
<button class="btn btn-primary btn-sm">Pequeno</button>
<button class="btn btn-primary">Normal</button>
<button class="btn btn-primary btn-lg">Grande</button>
<button class="btn btn-primary btn-xl">Extra Grande</button>

<!-- Full Width -->
<button class="btn btn-primary btn-block">Full Width</button>

<!-- Com Ícone -->
<button class="btn btn-primary">
  Salvar
  <svg width="16" height="16"><!-- ícone --></svg>
</button>
```

### Formulários

```html
<div class="form-group">
  <label class="form-label required" for="email">Email</label>
  <input
    type="email"
    id="email"
    class="form-input"
    placeholder="seu@email.com"
  />
  <span class="form-helper">Nunca compartilharemos seu email</span>
</div>

<!-- Input com erro -->
<input class="form-input is-invalid" />
<span class="form-helper is-error">Email inválido</span>

<!-- Input válido -->
<input class="form-input is-valid" />
<span class="form-helper is-success">Email disponível</span>

<!-- Select -->
<select class="form-input form-select">
  <option>Opção 1</option>
  <option>Opção 2</option>
</select>

<!-- Textarea -->
<textarea class="form-input form-textarea"></textarea>
```

### Cards

```html
<!-- Card Básico -->
<div class="card">
  <div class="card-header">
    <h3 class="card-title">Título do Card</h3>
    <p class="card-subtitle">Subtítulo opcional</p>
  </div>
  <div class="card-body">
    Conteúdo do card aqui.
  </div>
  <div class="card-footer">
    <button class="btn btn-primary">Ação</button>
  </div>
</div>

<!-- Card de Métrica -->
<div class="card metric-card">
  <div class="metric-label">Receita Total</div>
  <div class="metric-value">R$ 45.230</div>
  <div class="metric-change is-positive">
    ↑ 12.5%
  </div>
</div>

<!-- Card Interativo -->
<div class="card card-interactive">
  Clicável
</div>

<!-- Card Elevado -->
<div class="card card-elevated">
  Sombra maior
</div>
```

### Badges

```html
<!-- Badge Básico -->
<span class="badge badge-primary">Novo</span>
<span class="badge badge-success">Ativo</span>
<span class="badge badge-warning">Pendente</span>
<span class="badge badge-error">Erro</span>
<span class="badge badge-info">Info</span>
<span class="badge badge-gray">Neutro</span>

<!-- Tamanhos -->
<span class="badge badge-primary badge-sm">Pequeno</span>
<span class="badge badge-primary">Normal</span>
<span class="badge badge-primary badge-lg">Grande</span>

<!-- Com Dot -->
<span class="badge badge-success badge-dot">Online</span>
```

### Alerts

```html
<div class="alert alert-success">
  <svg class="alert-icon"><!-- ícone --></svg>
  <div class="alert-content">
    <div class="alert-title">Sucesso!</div>
    <div class="alert-message">Suas alterações foram salvas.</div>
  </div>
  <button class="alert-close">×</button>
</div>

<!-- Variantes -->
<div class="alert alert-error">...</div>
<div class="alert alert-warning">...</div>
<div class="alert alert-info">...</div>
```

### Tabelas

```html
<div class="table-container">
  <table class="table">
    <thead>
      <tr>
        <th>Data</th>
        <th>Descrição</th>
        <th class="text-right">Valor</th>
        <th>Status</th>
      </tr>
    </thead>
    <tbody>
      <tr>
        <td>21/10/2025</td>
        <td>Pagamento #1234</td>
        <td class="text-right">R$ 1.250,00</td>
        <td><span class="badge badge-success">Aprovado</span></td>
      </tr>
    </tbody>
  </table>
</div>
```

### Modais

```html
<div class="modal-backdrop">
  <div class="modal">
    <div class="modal-header">
      <h3 class="modal-title">Título do Modal</h3>
      <button class="modal-close">×</button>
    </div>

    <div class="modal-body">
      Conteúdo do modal aqui.
    </div>

    <div class="modal-footer">
      <button class="btn btn-secondary">Cancelar</button>
      <button class="btn btn-primary">Confirmar</button>
    </div>
  </div>
</div>

<!-- Tamanhos -->
<div class="modal modal-sm">...</div>
<div class="modal modal-lg">...</div>
<div class="modal modal-xl">...</div>
```

### Loading States

```html
<!-- Spinner -->
<div class="spinner"></div>
<div class="spinner spinner-lg"></div>
<div class="spinner spinner-sm"></div>

<!-- Skeleton -->
<div class="skeleton skeleton-heading"></div>
<div class="skeleton skeleton-text"></div>
<div class="skeleton skeleton-text short"></div>
<div class="skeleton skeleton-circle"></div>
<div class="skeleton skeleton-button"></div>
```

## 🎭 Dark Mode

### Ativação

```html
<!-- Adicione este script no <head> -->
<script>
const savedTheme = localStorage.getItem('theme') || 'light';
document.documentElement.setAttribute('data-theme', savedTheme);
</script>
```

### Toggle Dark Mode

```javascript
function toggleTheme() {
  const html = document.documentElement;
  const currentTheme = html.getAttribute('data-theme');
  const newTheme = currentTheme === 'dark' ? 'light' : 'dark';

  html.setAttribute('data-theme', newTheme);
  localStorage.setItem('theme', newTheme);
}
```

## 📐 Sistema de Espaçamento

Baseado em múltiplos de 4px:

```css
var(--zend-space-1)  /* 4px */
var(--zend-space-2)  /* 8px */
var(--zend-space-3)  /* 12px */
var(--zend-space-4)  /* 16px */
var(--zend-space-6)  /* 24px */
var(--zend-space-8)  /* 32px */
var(--zend-space-12) /* 48px */
var(--zend-space-16) /* 64px */
```

## 🎨 Sombras

```css
var(--zend-shadow-sm)      /* Pequena */
var(--zend-shadow-base)    /* Base */
var(--zend-shadow-md)      /* Média */
var(--zend-shadow-lg)      /* Grande */
var(--zend-shadow-xl)      /* Extra Grande */
var(--zend-shadow-2xl)     /* 2X Grande */
var(--zend-shadow-primary) /* Sombra Primary */
```

## 🔲 Border Radius

```css
var(--zend-radius-sm)   /* 4px */
var(--zend-radius-base) /* 8px */
var(--zend-radius-md)   /* 12px */
var(--zend-radius-lg)   /* 16px */
var(--zend-radius-xl)   /* 24px */
var(--zend-radius-full) /* Circular */
```

## 🚀 Classes Utilitárias

### Container

```html
<div class="container">
  <!-- Conteúdo centralizado com max-width: 1280px -->
</div>
```

### Flexbox

```html
<div class="flex items-center justify-between gap-4">
  <!-- Flex com alinhamento e gap -->
</div>
```

### Text Alignment

```html
<p class="text-center">Centralizado</p>
<p class="text-left">Esquerda</p>
<p class="text-right">Direita</p>
```

## 📱 Responsividade

O design system é mobile-first com breakpoints em:

- **sm**: 640px
- **md**: 768px
- **lg**: 1024px
- **xl**: 1280px
- **2xl**: 1536px

## ♿ Acessibilidade

- Contraste de cores WCAG AA compliant
- Navegação por teclado em todos componentes
- Focus visible em elementos interativos
- Suporte a screen readers
- Modo de movimento reduzido (`prefers-reduced-motion`)

## 🎯 Boas Práticas

1. **Sempre use variáveis CSS** ao invés de valores hardcoded
2. **Mantenha consistência** usando classes do design system
3. **Mobile First**: Desenvolva primeiro para mobile
4. **Acessibilidade**: Sempre teste com keyboard navigation
5. **Dark Mode**: Teste sempre em ambos os modos
6. **Performance**: Use apenas as classes necessárias

## 📚 Exemplos de Uso

Veja exemplos completos na pasta `/design-system/examples/`.

## 🆘 Suporte

Para dúvidas ou problemas:
- Abra uma issue no GitHub
- Consulte a documentação completa
- Entre em contato com a equipe de desenvolvimento

---

**Versão**: 2.0.0
**Última Atualização**: Outubro 2025
**Licença**: MIT
