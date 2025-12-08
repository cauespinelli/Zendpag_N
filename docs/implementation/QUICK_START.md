# ⚡ Zendapag Design System - Quick Start

## 🚀 Implementação em 5 Minutos

### Passo 1: Ver o Showcase (RECOMENDADO)

Abra este arquivo no navegador para ver TODOS os componentes:

```
C:/Projetos/zendapag/design-system/examples/components-showcase.html
```

**OU** execute:
```bash
cd C:/Projetos/zendapag/design-system/examples
npx serve .
# Abra http://localhost:3000/components-showcase.html
```

---

### Passo 2: Dashboard (React + Ant Design)

**Arquivo:** `zendapag-dashboard/src/index.tsx`

**Adicione esta linha:**
```typescript
import './styles/design-system-v2.css';
```

**Execute:**
```bash
cd zendapag-dashboard
npm start
```

✅ **Pronto!** O dashboard agora usa o design system Zendapag.

---

### Passo 3: Landing Page (React)

**Arquivo:** `zendapag-landing/src/index.tsx`

**Verifique se existe:**
```typescript
import './styles/design-system.css';
```

**Execute:**
```bash
cd zendapag-landing
npm start
```

✅ **Pronto!** A landing page está atualizada.

---

## 📁 Arquivos Principais

### Design System Core
```
design-system/css/main.css          ⭐ Import este arquivo
```

### Logos
```
design-system/logos/logo-full-color.svg  ⭐ Logo principal
design-system/logos/favicon.svg          ⭐ Favicon
```

### Documentação
```
design-system/docs/README.md             ⭐ Referência completa
REDESIGN_IMPLEMENTATION_GUIDE.md         ⭐ Guia detalhado
REDESIGN_COMPLETE.md                     ⭐ Resumo executivo
```

---

## 🎨 Uso Básico

### Botão
```html
<button class="btn btn-primary">Clique aqui</button>
```

### Card
```html
<div class="card">
  <h3 class="card-title">Título</h3>
  <p class="card-body">Conteúdo</p>
</div>
```

### Badge
```html
<span class="badge badge-success">Ativo</span>
```

### Form
```html
<div class="form-group">
  <label class="form-label" for="email">Email</label>
  <input type="email" class="form-input" id="email" />
</div>
```

---

## 🌙 Dark Mode

### Ativar
```html
<script>
const theme = localStorage.getItem('theme') || 'light';
document.documentElement.setAttribute('data-theme', theme);
</script>
```

### Toggle
```javascript
function toggleTheme() {
  const html = document.documentElement;
  const current = html.getAttribute('data-theme');
  const next = current === 'dark' ? 'light' : 'dark';
  html.setAttribute('data-theme', next);
  localStorage.setItem('theme', next);
}
```

---

## 💡 CSS Variables

### Cores
```css
var(--zend-primary)        /* #4F46E5 Indigo */
var(--zend-secondary)      /* #10B981 Green */
var(--zend-success)        /* #10B981 */
var(--zend-error)          /* #EF4444 */
```

### Espaçamento
```css
var(--zend-space-2)   /* 8px */
var(--zend-space-4)   /* 16px */
var(--zend-space-6)   /* 24px */
```

### Tipografia
```css
var(--zend-text-sm)   /* 14px */
var(--zend-text-base) /* 16px */
var(--zend-text-xl)   /* 20px */
```

---

## ✅ Checklist

- [ ] Ver showcase: `components-showcase.html`
- [ ] Implementar no dashboard
- [ ] Implementar na landing page
- [ ] Testar dark mode
- [ ] Testar mobile
- [ ] Deploy

---

## 📚 Mais Informações

- **Documentação Completa**: `design-system/docs/README.md`
- **Guia de Implementação**: `REDESIGN_IMPLEMENTATION_GUIDE.md`
- **Resumo Executivo**: `REDESIGN_COMPLETE.md`

---

**🎨 Zendapag Design System v2.0**

Pronto para usar! 🚀
