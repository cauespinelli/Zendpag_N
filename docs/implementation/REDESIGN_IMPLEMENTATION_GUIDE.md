# 🎨 Zendapag Design System - Guia de Implementação

## 📋 Sumário Executivo

O **Zendapag Design System v2.0** foi criado seguindo as melhores práticas de design enterprise, inspirado em Stripe, Linear e Witetec. Este guia fornece tudo que você precisa para implementar o novo visual na plataforma.

## ✅ O Que Foi Criado

### 1. Identidade Visual

- ✅ **Logo Profissional** em 4 variações (SVG)
  - `design-system/logos/logo-full-color.svg`
  - `design-system/logos/logo-white.svg`
  - `design-system/logos/logo-icon-only.svg`
  - `design-system/logos/favicon.svg`

- ✅ **Paleta de Cores Completa**
  - Primary: Indigo (#4F46E5)
  - Secondary: Green (#10B981)
  - Accent: Orange (#F59E0B)
  - Grayscale: 10 variações (50-900)
  - Cores funcionais: Success, Warning, Error, Info

- ✅ **Sistema Tipográfico**
  - Fonte Primary: Inter (400-900)
  - Fonte Display: Space Grotesk (500-700)
  - 10 tamanhos (xs a 6xl)
  - 6 pesos (regular a black)

### 2. Design System CSS

#### Arquivos Criados:

```
design-system/
├── css/
│   ├── variables.css      # 300+ CSS Variables
│   ├── reset.css          # Reset & Base Styles
│   ├── components.css     # Todos os componentes
│   └── main.css           # Entry point
├── logos/
│   ├── logo-full-color.svg
│   ├── logo-white.svg
│   ├── logo-icon-only.svg
│   └── favicon.svg
└── docs/
    └── README.md          # Documentação completa
```

#### Componentes Implementados:

- ✅ Buttons (6 variantes + 4 tamanhos)
- ✅ Forms (inputs, selects, textarea, checkbox, radio)
- ✅ Cards (4 variantes + metric cards)
- ✅ Badges (6 cores + 3 tamanhos)
- ✅ Alerts (4 variantes)
- ✅ Tables (com hover e responsive)
- ✅ Modals (3 tamanhos)
- ✅ Loading States (spinners + skeleton)
- ✅ Navigation
- ✅ Utility Classes

### 3. Aplicações Atualizadas

- ✅ **Dashboard** (React + Ant Design)
  - Arquivo: `zendapag-dashboard/src/styles/design-system-v2.css`
  - Overrides completos do Ant Design
  - Componentes customizados

- ✅ **Landing Page** (React)
  - Arquivo: `zendapag-landing/src/styles/design-system.css`
  - Hero section redesenhado
  - Features grid
  - CTA section
  - Footer profissional

## 🚀 Como Implementar

### Passo 1: Dashboard (Ant Design)

1. **Abra o arquivo**: `zendapag-dashboard/src/index.tsx`

2. **Adicione o import**:
```typescript
import './styles/design-system-v2.css';
```

3. **Pronto!** O Ant Design agora usa o design system Zendapag.

### Passo 2: Landing Page

1. **Abra o arquivo**: `zendapag-landing/src/index.tsx`

2. **Verifique o import**:
```typescript
import './styles/design-system.css';
```

3. **Atualize os componentes** para usar as classes do design system.

### Passo 3: Outros Projetos

Para usar em qualquer projeto:

```html
<!-- No <head> -->
<link rel="stylesheet" href="/design-system/css/main.css">
```

Ou em React/Vue:

```javascript
import './design-system/css/main.css';
```

## 🎨 Exemplos de Uso

### Botões

```html
<!-- Primary -->
<button class="btn btn-primary">Criar Conta</button>

<!-- Secondary -->
<button class="btn btn-secondary">Cancelar</button>

<!-- Com tamanho -->
<button class="btn btn-primary btn-lg">Grande</button>

<!-- Full width -->
<button class="btn btn-primary btn-block">Full Width</button>
```

### Cards

```html
<div class="card">
  <div class="card-header">
    <h3 class="card-title">Título</h3>
  </div>
  <div class="card-body">
    Conteúdo aqui
  </div>
</div>
```

### Metric Card

```html
<div class="card metric-card">
  <div class="metric-label">Receita Total</div>
  <div class="metric-value">R$ 45.230</div>
  <div class="metric-change is-positive">↑ 12.5%</div>
</div>
```

### Forms

```html
<div class="form-group">
  <label class="form-label required" for="email">Email</label>
  <input type="email" id="email" class="form-input" />
  <span class="form-helper">Digite seu email</span>
</div>
```

### Badges

```html
<span class="badge badge-success">Ativo</span>
<span class="badge badge-warning">Pendente</span>
<span class="badge badge-error">Erro</span>
```

## 🌙 Dark Mode

### Ativar Dark Mode

```javascript
// No <head> do HTML
<script>
const theme = localStorage.getItem('theme') || 'light';
document.documentElement.setAttribute('data-theme', theme);
</script>
```

### Toggle Dark Mode

```javascript
function toggleTheme() {
  const html = document.documentElement;
  const current = html.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';

  html.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
}

// Usar em botão
<button onclick="toggleTheme()">Toggle Dark Mode</button>
```

## 📐 CSS Variables

### Cores

```css
/* Primary */
var(--zend-primary)          /* #4F46E5 */
var(--zend-blue-50 a -900)   /* Variações */

/* Secondary */
var(--zend-secondary)        /* #10B981 */
var(--zend-green-50 a -900)  /* Variações */

/* Functional */
var(--zend-success)          /* #10B981 */
var(--zend-warning)          /* #F59E0B */
var(--zend-error)            /* #EF4444 */
var(--zend-info)             /* #3B82F6 */

/* Grayscale */
var(--zend-gray-50)          /* #F8FAFC */
var(--zend-gray-900)         /* #0F172A */

/* Text */
var(--zend-text-primary)     /* Texto principal */
var(--zend-text-secondary)   /* Texto secundário */
var(--zend-text-tertiary)    /* Texto terciário */
```

### Espaçamento

```css
var(--zend-space-1)   /* 4px */
var(--zend-space-2)   /* 8px */
var(--zend-space-4)   /* 16px */
var(--zend-space-6)   /* 24px */
var(--zend-space-8)   /* 32px */
var(--zend-space-12)  /* 48px */
```

### Tipografia

```css
/* Tamanhos */
var(--zend-text-xs)    /* 12px */
var(--zend-text-sm)    /* 14px */
var(--zend-text-base)  /* 16px */
var(--zend-text-lg)    /* 18px */
var(--zend-text-xl)    /* 20px */
var(--zend-text-2xl)   /* 24px */
var(--zend-text-4xl)   /* 36px */

/* Pesos */
var(--zend-weight-regular)    /* 400 */
var(--zend-weight-medium)     /* 500 */
var(--zend-weight-semibold)   /* 600 */
var(--zend-weight-bold)       /* 700 */
var(--zend-weight-extrabold)  /* 800 */
```

### Outros

```css
/* Border Radius */
var(--zend-radius-sm)    /* 4px */
var(--zend-radius-base)  /* 8px */
var(--zend-radius-md)    /* 12px */
var(--zend-radius-lg)    /* 16px */
var(--zend-radius-full)  /* 9999px */

/* Shadows */
var(--zend-shadow-sm)
var(--zend-shadow-md)
var(--zend-shadow-lg)
var(--zend-shadow-xl)
var(--zend-shadow-primary)

/* Transitions */
var(--zend-transition-fast)   /* 150ms */
var(--zend-transition-base)   /* 200ms */
var(--zend-transition-slow)   /* 300ms */
```

## 📱 Responsividade

### Breakpoints

```css
/* Mobile */
@media (max-width: 640px) { }

/* Tablet */
@media (max-width: 768px) { }

/* Desktop */
@media (max-width: 1024px) { }

/* Large Desktop */
@media (max-width: 1280px) { }
```

### Classes Utilitárias

```html
<!-- Container -->
<div class="container">
  <!-- Max-width: 1280px, centralizado -->
</div>

<!-- Flexbox -->
<div class="flex items-center justify-between gap-4">
  <!-- Display flex com alinhamento -->
</div>

<!-- Text Alignment -->
<p class="text-center">Centralizado</p>
<p class="text-left">Esquerda</p>
<p class="text-right">Direita</p>
```

## 🎯 Checklist de Implementação

### Dashboard

- [ ] Importar `design-system-v2.css` no `index.tsx`
- [ ] Testar todos os componentes Ant Design
- [ ] Verificar cores e espaçamentos
- [ ] Testar dark mode
- [ ] Testar responsividade (mobile, tablet, desktop)
- [ ] Verificar performance (Lighthouse)

### Landing Page

- [ ] Atualizar logo no header
- [ ] Verificar hero section
- [ ] Testar CTA buttons
- [ ] Verificar features grid
- [ ] Testar footer
- [ ] Verificar SEO (meta tags)
- [ ] Testar mobile

### Geral

- [ ] Adicionar favicon (`design-system/logos/favicon.svg`)
- [ ] Configurar meta tags (Open Graph, Twitter Card)
- [ ] Testar em múltiplos navegadores (Chrome, Firefox, Safari, Edge)
- [ ] Validar acessibilidade (contraste, keyboard navigation)
- [ ] Otimizar performance (minificar CSS, lazy load)
- [ ] Documentar componentes customizados

## 🐛 Troubleshooting

### CSS não está aplicando

1. Verificar ordem dos imports (design system deve vir primeiro)
2. Limpar cache do navegador (Ctrl + Shift + R)
3. Verificar DevTools > Network (arquivo carregando?)
4. Verificar console (erros de import?)

### Cores aparecem diferentes

1. Verificar se variáveis CSS estão carregadas (DevTools > Elements > Computed)
2. Usar valores exatos das variáveis
3. Testar em modo anônimo (extensões podem interferir)

### Layout quebrado no mobile

1. Verificar viewport meta tag:
```html
<meta name="viewport" content="width=device-width, initial-scale=1">
```
2. Testar em dispositivo real, não só emulador
3. Verificar media queries
4. Usar `max-width: 100%` em imagens

### Dark mode não funciona

1. Verificar atributo `data-theme` no `<html>`
2. Verificar localStorage
3. Testar hard-coded:
```javascript
document.documentElement.setAttribute('data-theme', 'dark');
```

## 📚 Recursos

### Documentação

- **Design System Docs**: `/design-system/docs/README.md`
- **Componentes**: `/design-system/css/components.css`
- **Variables**: `/design-system/css/variables.css`

### Ferramentas

- [Figma](https://figma.com) - Design visual
- [Coolors](https://coolors.co) - Paletas de cores
- [Google Fonts](https://fonts.google.com) - Fontes
- [Lucide Icons](https://lucide.dev) - Ícones

### Referências

- [Stripe Design System](https://stripe.com)
- [Linear Design](https://linear.app)
- [Tailwind CSS](https://tailwindcss.com)

## 🎉 Próximos Passos

1. **Semana 1**: Implementar no dashboard
2. **Semana 2**: Implementar na landing page
3. **Semana 3**: Criar componentes React customizados
4. **Semana 4**: Testes, ajustes e lançamento

## 🆘 Suporte

Para dúvidas ou problemas:
- Abra uma issue no GitHub
- Consulte a documentação em `/design-system/docs/`
- Entre em contato com a equipe de desenvolvimento

---

**Versão**: 2.0.0
**Data**: Outubro 2025
**Status**: ✅ Pronto para Implementação

**Desenvolvido com ❤️ para Zendapag**
