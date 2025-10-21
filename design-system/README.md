# Zendapag Design System

> Sistema de design enterprise premium para a plataforma Zendapag PIX

## 🎨 Visão Geral

Este design system foi criado para elevar o Zendapag ao nível visual de plataformas enterprise como Stripe, Plaid e Witetec, transmitindo **confiança**, **inovação** e **profissionalismo**.

---

## 🎯 Princípios de Design

### 1. Confiança e Segurança
- Uso estratégico de azuis profundos
- Elementos de segurança visíveis
- Hierarquia clara de informações

### 2. Clareza e Simplicidade
- White space generoso
- Tipografia legível
- Componentes intuitivos

### 3. Modernidade e Inovação
- Gradientes sutis
- Animações fluidas
- Design contemporâneo

### 4. Profissionalismo Enterprise
- Consistência rigorosa
- Atenção aos detalhes
- Qualidade premium

---

## 🎨 Paleta de Cores

### Cores Primárias

```css
/* Primary - Azul Tecnológico (Confiança & Inovação) */
--zp-primary-50: #EEF2FF;
--zp-primary-100: #E0E7FF;
--zp-primary-200: #C7D2FE;
--zp-primary-300: #A5B4FC;
--zp-primary-400: #818CF8;
--zp-primary-500: #6366F1;  /* BRAND COLOR */
--zp-primary-600: #4F46E5;
--zp-primary-700: #4338CA;
--zp-primary-800: #3730A3;
--zp-primary-900: #312E81;
--zp-primary-950: #1E1B4B;

/* Success - Verde Crescimento */
--zp-success-50: #ECFDF5;
--zp-success-100: #D1FAE5;
--zp-success-200: #A7F3D0;
--zp-success-300: #6EE7B7;
--zp-success-400: #34D399;
--zp-success-500: #10B981;  /* PRIMARY SUCCESS */
--zp-success-600: #059669;
--zp-success-700: #047857;
--zp-success-800: #065F46;
--zp-success-900: #064E3B;

/* Accent - Âmbar Energia */
--zp-accent-50: #FFFBEB;
--zp-accent-100: #FEF3C7;
--zp-accent-200: #FDE68A;
--zp-accent-300: #FCD34D;
--zp-accent-400: #FBBF24;
--zp-accent-500: #F59E0B;  /* PRIMARY ACCENT */
--zp-accent-600: #D97706;
--zp-accent-700: #B45309;
--zp-accent-800: #92400E;
--zp-accent-900: #78350F;
```

### Cores Neutras

```css
/* Neutros - Slate (Profissionalismo) */
--zp-neutral-50: #F8FAFC;   /* Background Light */
--zp-neutral-100: #F1F5F9;  /* Background Subtle */
--zp-neutral-200: #E2E8F0;  /* Border Light */
--zp-neutral-300: #CBD5E1;  /* Border Default */
--zp-neutral-400: #94A3B8;  /* Text Disabled */
--zp-neutral-500: #64748B;  /* Text Secondary */
--zp-neutral-600: #475569;  /* Text Body */
--zp-neutral-700: #334155;  /* Text Default */
--zp-neutral-800: #1E293B;  /* Text Heading */
--zp-neutral-900: #0F172A;  /* Text Strong */
--zp-neutral-950: #020617;  /* Background Dark */
```

### Cores Funcionais

```css
/* Success */
--zp-color-success: #10B981;
--zp-color-success-light: #D1FAE5;
--zp-color-success-dark: #047857;

/* Warning */
--zp-color-warning: #F59E0B;
--zp-color-warning-light: #FEF3C7;
--zp-color-warning-dark: #B45309;

/* Error */
--zp-color-error: #EF4444;
--zp-color-error-light: #FEE2E2;
--zp-color-error-dark: #B91C1C;

/* Info */
--zp-color-info: #3B82F6;
--zp-color-info-light: #DBEAFE;
--zp-color-info-dark: #1D4ED8;
```

---

## ✍️ Tipografia

### Fonte Primária: Inter

**Família**: [Inter](https://fonts.google.com/specimen/Inter)
**Uso**: Interface, body text, UI elements
**Weights**: 400 (Regular), 500 (Medium), 600 (SemiBold), 700 (Bold), 800 (ExtraBold)

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');

--zp-font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
```

### Fonte Display: Space Grotesk

**Família**: [Space Grotesk](https://fonts.google.com/specimen/Space+Grotesk)
**Uso**: Headlines, títulos de seção, hero text
**Weights**: 500 (Medium), 600 (SemiBold), 700 (Bold)

```css
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&display=swap');

--zp-font-display: 'Space Grotesk', 'Inter', sans-serif;
```

### Scale Tipográfica

```css
/* Display - Headlines principais */
--zp-text-display-2xl: 4.5rem;    /* 72px - Hero principal */
--zp-text-display-xl: 3.75rem;    /* 60px - Hero secundário */
--zp-text-display-lg: 3rem;       /* 48px - Títulos de página */

/* Headings */
--zp-text-h1: 2.25rem;  /* 36px */
--zp-text-h2: 1.875rem; /* 30px */
--zp-text-h3: 1.5rem;   /* 24px */
--zp-text-h4: 1.25rem;  /* 20px */
--zp-text-h5: 1.125rem; /* 18px */
--zp-text-h6: 1rem;     /* 16px */

/* Body */
--zp-text-body-lg: 1.125rem;  /* 18px */
--zp-text-body: 1rem;         /* 16px */
--zp-text-body-sm: 0.875rem;  /* 14px */
--zp-text-body-xs: 0.75rem;   /* 12px */

/* Line Heights */
--zp-leading-tight: 1.2;
--zp-leading-snug: 1.375;
--zp-leading-normal: 1.5;
--zp-leading-relaxed: 1.625;
--zp-leading-loose: 1.75;

/* Letter Spacing */
--zp-tracking-tighter: -0.05em;
--zp-tracking-tight: -0.025em;
--zp-tracking-normal: 0;
--zp-tracking-wide: 0.025em;
--zp-tracking-wider: 0.05em;

/* Font Weights */
--zp-font-regular: 400;
--zp-font-medium: 500;
--zp-font-semibold: 600;
--zp-font-bold: 700;
--zp-font-extrabold: 800;
```

---

## 📐 Espaçamento

### Sistema de 8pt Grid

```css
--zp-space-0: 0;
--zp-space-1: 0.25rem;   /* 4px */
--zp-space-2: 0.5rem;    /* 8px */
--zp-space-3: 0.75rem;   /* 12px */
--zp-space-4: 1rem;      /* 16px */
--zp-space-5: 1.25rem;   /* 20px */
--zp-space-6: 1.5rem;    /* 24px */
--zp-space-8: 2rem;      /* 32px */
--zp-space-10: 2.5rem;   /* 40px */
--zp-space-12: 3rem;     /* 48px */
--zp-space-16: 4rem;     /* 64px */
--zp-space-20: 5rem;     /* 80px */
--zp-space-24: 6rem;     /* 96px */
--zp-space-32: 8rem;     /* 128px */
```

---

## 🎭 Elevação & Sombras

```css
/* Subtle - Cards, inputs */
--zp-shadow-xs: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
--zp-shadow-sm: 0 1px 3px 0 rgba(0, 0, 0, 0.1),
                0 1px 2px -1px rgba(0, 0, 0, 0.1);

/* Medium - Dropdowns, popovers */
--zp-shadow-md: 0 4px 6px -1px rgba(0, 0, 0, 0.1),
                0 2px 4px -2px rgba(0, 0, 0, 0.1);

/* Large - Modals */
--zp-shadow-lg: 0 10px 15px -3px rgba(0, 0, 0, 0.1),
                0 4px 6px -4px rgba(0, 0, 0, 0.1);

/* XL - Overlays */
--zp-shadow-xl: 0 20px 25px -5px rgba(0, 0, 0, 0.1),
                0 8px 10px -6px rgba(0, 0, 0, 0.1);

/* 2XL - Elevated cards */
--zp-shadow-2xl: 0 25px 50px -12px rgba(0, 0, 0, 0.25);

/* Colored Shadows - Para CTAs */
--zp-shadow-primary: 0 10px 25px -5px rgba(99, 102, 241, 0.3);
--zp-shadow-success: 0 10px 25px -5px rgba(16, 185, 129, 0.3);
--zp-shadow-accent: 0 10px 25px -5px rgba(245, 158, 11, 0.3);
```

---

## 🔲 Border Radius

```css
--zp-radius-none: 0;
--zp-radius-sm: 0.25rem;    /* 4px - Badges, tags */
--zp-radius-md: 0.375rem;   /* 6px - Inputs, buttons */
--zp-radius-lg: 0.5rem;     /* 8px - Cards */
--zp-radius-xl: 0.75rem;    /* 12px - Modals */
--zp-radius-2xl: 1rem;      /* 16px - Featured cards */
--zp-radius-3xl: 1.5rem;    /* 24px - Hero elements */
--zp-radius-full: 9999px;   /* Pills, avatars */
```

---

## 🎬 Animações & Transições

```css
/* Durations */
--zp-duration-fast: 150ms;
--zp-duration-normal: 250ms;
--zp-duration-slow: 350ms;
--zp-duration-slower: 500ms;

/* Easings */
--zp-ease-linear: linear;
--zp-ease-in: cubic-bezier(0.4, 0, 1, 1);
--zp-ease-out: cubic-bezier(0, 0, 0.2, 1);
--zp-ease-in-out: cubic-bezier(0.4, 0, 0.2, 1);
--zp-ease-bounce: cubic-bezier(0.68, -0.55, 0.265, 1.55);

/* Standard Transition */
--zp-transition: all var(--zp-duration-normal) var(--zp-ease-out);
```

---

## 📱 Breakpoints Responsivos

```css
/* Mobile first approach */
--zp-screen-sm: 640px;   /* Small tablets */
--zp-screen-md: 768px;   /* Tablets */
--zp-screen-lg: 1024px;  /* Small laptops */
--zp-screen-xl: 1280px;  /* Desktops */
--zp-screen-2xl: 1536px; /* Large screens */
```

---

## 🎨 Gradientes

### Gradientes de Marca

```css
/* Primary Gradient - Hero, CTAs principais */
--zp-gradient-primary: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);

/* Success Gradient - Confirmações */
--zp-gradient-success: linear-gradient(135deg, #10B981 0%, #059669 100%);

/* Premium Gradient - Features destacadas */
--zp-gradient-premium: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

/* Subtle Background */
--zp-gradient-subtle: linear-gradient(180deg, #F8FAFC 0%, #F1F5F9 100%);
```

---

## 🎯 Z-Index Scale

```css
--zp-z-base: 0;
--zp-z-dropdown: 1000;
--zp-z-sticky: 1100;
--zp-z-fixed: 1200;
--zp-z-modal-backdrop: 1300;
--zp-z-modal: 1400;
--zp-z-popover: 1500;
--zp-z-tooltip: 1600;
--zp-z-notification: 1700;
```

---

## 📦 Componentes Base

### Buttons

```css
/* Primary Button */
.zp-btn-primary {
  background: var(--zp-primary-600);
  color: white;
  padding: 0.75rem 1.5rem;
  border-radius: var(--zp-radius-lg);
  font-weight: var(--zp-font-semibold);
  font-size: var(--zp-text-body);
  transition: var(--zp-transition);
  box-shadow: var(--zp-shadow-sm);
}

.zp-btn-primary:hover {
  background: var(--zp-primary-700);
  box-shadow: var(--zp-shadow-primary);
  transform: translateY(-2px);
}

/* Secondary Button */
.zp-btn-secondary {
  background: white;
  color: var(--zp-primary-600);
  border: 2px solid var(--zp-primary-600);
  padding: 0.75rem 1.5rem;
  border-radius: var(--zp-radius-lg);
  font-weight: var(--zp-font-semibold);
  transition: var(--zp-transition);
}

.zp-btn-secondary:hover {
  background: var(--zp-primary-50);
  transform: translateY(-2px);
}
```

### Cards

```css
.zp-card {
  background: white;
  border-radius: var(--zp-radius-xl);
  padding: var(--zp-space-6);
  box-shadow: var(--zp-shadow-md);
  border: 1px solid var(--zp-neutral-200);
  transition: var(--zp-transition);
}

.zp-card:hover {
  box-shadow: var(--zp-shadow-lg);
  transform: translateY(-4px);
}

.zp-card-premium {
  background: linear-gradient(white, white) padding-box,
              var(--zp-gradient-primary) border-box;
  border: 2px solid transparent;
}
```

---

## 🎨 Aplicação de Cores

### Text Colors

- **Headings**: `--zp-neutral-900`
- **Body**: `--zp-neutral-700`
- **Secondary**: `--zp-neutral-500`
- **Disabled**: `--zp-neutral-400`

### Background Colors

- **Page**: `--zp-neutral-50`
- **Cards**: `white` ou `--zp-neutral-50`
- **Sections alternadas**: `white` e `--zp-neutral-50`

### Interactive Elements

- **Primary Actions**: `--zp-primary-600`
- **Success**: `--zp-success-500`
- **Warning**: `--zp-accent-500`
- **Danger**: `--zp-color-error`

---

## 📏 Guidelines de Uso

### Do's ✅

- Use white space generosamente
- Mantenha hierarquia visual clara
- Use sombras sutis para profundidade
- Animações suaves e purpose-driven
- Consistência em todos os componentes

### Don'ts ❌

- Não use cores saturadas em excesso
- Evite animações desnecessárias
- Não misture estilos de border-radius
- Evite sombras muito pesadas
- Não quebre a hierarquia tipográfica

---

## 📚 Recursos

- **Figma**: [Link do projeto]
- **Fonts**: Google Fonts (Inter, Space Grotesk)
- **Icons**: Lucide Icons (consistente, moderno)
- **Illustrations**: unDraw ou Storyset (customizadas com paleta de marca)

---

## 📝 Changelog

- **v1.0.0** (2025-01-20) - Sistema inicial criado
