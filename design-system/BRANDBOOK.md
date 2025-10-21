# Zendapag Brandbook

> **Guia Oficial de Identidade Visual Enterprise**

---

## 📖 Índice

1. [Introdução](#introdução)
2. [Essência da Marca](#essência-da-marca)
3. [Logo e Aplicações](#logo-e-aplicações)
4. [Paleta de Cores](#paleta-de-cores)
5. [Tipografia](#tipografia)
6. [Sistema de Design](#sistema-de-design)
7. [Voz e Tom](#voz-e-tom)
8. [Aplicações](#aplicações)
9. [Checklist de Qualidade](#checklist-de-qualidade)

---

## 🎯 Introdução

### Sobre Este Documento

Este brandbook é o guia definitivo para garantir consistência visual e comunicacional em todos os pontos de contato do Zendapag. Ele foi criado para elevar a percepção de qualidade da marca ao nível de plataformas enterprise líderes como Stripe, Plaid e Witetec.

### Objetivo do Redesign

**Transformar** a identidade visual do Zendapag de "funcional" para "premium enterprise", transmitindo:
- ✅ Confiança e segurança
- ✅ Inovação tecnológica
- ✅ Profissionalismo enterprise
- ✅ Modernidade e clareza

---

## 💡 Essência da Marca

### Posicionamento

**Zendapag** é a plataforma de pagamentos PIX para empresas que buscam crescimento, combinando tecnologia de ponta com experiência empresarial intuitiva.

### Valores da Marca

1. **Confiança**: Segurança em cada transação
2. **Simplicidade**: Tecnologia complexa, experiência simples
3. **Velocidade**: Pagamentos instantâneos, suporte ágil
4. **Transparência**: Preços claros, sem surpresas
5. **Inovação**: Sempre um passo à frente

### Personalidade da Marca

- 🎯 **Profissional**, mas **acessível**
- 💡 **Tecnológica**, mas **humana**
- 🚀 **Inovadora**, mas **confiável**
- 📊 **Data-driven**, mas **intuitiva**

### Promessa da Marca

> "Transformamos a complexidade dos pagamentos PIX em uma experiência simples, segura e escalável."

---

## 🎨 Logo e Aplicações

### Anatomia do Logo

#### Símbolo (Icon)
- **Forma**: Quadrado com bordas arredondadas (12px radius)
- **Elementos**: Letra "Z" estilizada + pontos de fluxo
- **Significado**:
  - Z = Zendapag
  - Pontos = Fluxo contínuo de transações
  - Gradiente = Inovação e tecnologia

#### Wordmark
- **Tipografia**: Space Grotesk Bold
- **Case**: Lowercase (zendapag)
- **Cor**: Slate 800 (#1E293B) ou Branco

### Versões Disponíveis

| Versão | Uso Recomendado | Arquivo |
|--------|----------------|---------|
| **Full Logo Horizontal** | Headers, website, materiais | `zendapag-logo-full.svg` |
| **Icon Isolado** | Favicon, app icons, avatar | `zendapag-icon.svg` |
| **Logo Branco** | Fundos escuros, footer | `zendapag-logo-full-white.svg` |

### Variações de Cor

#### 1. Primary (Preferencial)
```css
/* Símbolo */
background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);

/* Wordmark */
color: #1E293B;
```

#### 2. White (Dark Backgrounds)
```css
/* Símbolo */
background: rgba(255, 255, 255, 0.15);
border: 2px solid white;

/* Wordmark */
color: #FFFFFF;
```

### Área de Proteção

**Regra**: Manter espaço equivalente a **2x a altura do "z"** em todas as direções.

```
    [  2x  ]
    ────────────────────
   |                    |
   |  [Logo Zendapag]   |
   |                    |
    ────────────────────
```

### Tamanhos Mínimos

- **Digital**: 120px de largura (full logo)
- **Print**: 30mm de largura (full logo)
- **Favicon**: 32x32px (icon apenas)

### ❌ Usos Proibidos

**NUNCA**:
- ❌ Alterar cores do gradiente
- ❌ Distorcer ou esticar proporções
- ❌ Rotacionar o logo
- ❌ Adicionar sombras ou efeitos não aprovados
- ❌ Usar em fundos de baixo contraste
- ❌ Colocar sobre imagens complexas
- ❌ Usar bordas ou caixas não aprovadas
- ❌ Recriar ou modificar o símbolo

---

## 🎨 Paleta de Cores

### Sistema de Cores

Nossa paleta foi cientificamente escolhida para transmitir confiança, inovação e profissionalismo.

### Cores Primárias

#### Primary - Indigo (Confiança & Tecnologia)

```css
--zp-primary-50:  #EEF2FF;  /* Backgrounds sutis */
--zp-primary-100: #E0E7FF;  /* Hover states */
--zp-primary-200: #C7D2FE;  /* Borders */
--zp-primary-300: #A5B4FC;
--zp-primary-400: #818CF8;
--zp-primary-500: #6366F1;  /* ⭐ BRAND COLOR */
--zp-primary-600: #4F46E5;  /* Buttons, CTAs */
--zp-primary-700: #4338CA;  /* Hover */
--zp-primary-800: #3730A3;
--zp-primary-900: #312E81;
```

**Uso**: Botões primários, links, elementos interativos, gradientes

#### Success - Emerald (Crescimento & Confirmação)

```css
--zp-success-500: #10B981;  /* ⭐ PRIMARY SUCCESS */
--zp-success-600: #059669;  /* Hover */
--zp-success-100: #D1FAE5;  /* Backgrounds */
```

**Uso**: Mensagens de sucesso, status positivos, indicadores de crescimento

#### Accent - Amber (Energia & Atenção)

```css
--zp-accent-500: #F59E0B;  /* ⭐ PRIMARY ACCENT */
--zp-accent-600: #D97706;  /* Hover */
--zp-accent-100: #FEF3C7;  /* Backgrounds */
```

**Uso**: Warnings, badges de atenção, elementos de destaque

### Cores Neutras (Slate)

```css
--zp-neutral-50:  #F8FAFC;  /* Page background */
--zp-neutral-100: #F1F5F9;  /* Cards background */
--zp-neutral-200: #E2E8F0;  /* Borders */
--zp-neutral-300: #CBD5E1;  /* Dividers */
--zp-neutral-400: #94A3B8;  /* Disabled text */
--zp-neutral-500: #64748B;  /* Secondary text */
--zp-neutral-600: #475569;  /* Body text */
--zp-neutral-700: #334155;  /* Headings */
--zp-neutral-800: #1E293B;  /* Strong headings */
--zp-neutral-900: #0F172A;  /* Dark backgrounds */
```

### Aplicação de Cores

| Elemento | Cor | Código |
|----------|-----|--------|
| **Headings** | Neutral 900 | `#0F172A` |
| **Body Text** | Neutral 700 | `#334155` |
| **Secondary Text** | Neutral 500 | `#64748B` |
| **Page Background** | Neutral 50 | `#F8FAFC` |
| **Card Background** | White | `#FFFFFF` |
| **Primary Button** | Primary 600 | `#4F46E5` |
| **Success State** | Success 500 | `#10B981` |
| **Warning State** | Accent 500 | `#F59E0B` |
| **Error State** | Error | `#EF4444` |

### Gradientes

```css
/* Primary Gradient - Hero, CTAs */
background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);

/* Premium Gradient - Features destacadas */
background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);

/* Subtle Background */
background: linear-gradient(180deg, #F8FAFC 0%, #F1F5F9 100%);
```

---

## ✍️ Tipografia

### Sistema Tipográfico

Nossa tipografia combina **Inter** (interface/body) com **Space Grotesk** (display/headlines) para criar hierarquia clara e moderna.

### Fontes

#### Inter (Interface & Body)

```css
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap');

font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
```

**Uso**: UI elements, body text, navegação, formulários

**Weights disponíveis**:
- 400 (Regular) - Body text
- 500 (Medium) - UI elements
- 600 (SemiBold) - Buttons, labels
- 700 (Bold) - Enfatização
- 800 (ExtraBold) - Números, stats

#### Space Grotesk (Display & Headlines)

```css
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@500;600;700&display=swap');

font-family: 'Space Grotesk', 'Inter', sans-serif;
```

**Uso**: Títulos de seção, hero text, headings principais

**Weights disponíveis**:
- 500 (Medium) - Subtítulos
- 600 (SemiBold) - Headings
- 700 (Bold) - Hero text, display

### Escala Tipográfica

| Elemento | Tamanho | Line Height | Weight | Família |
|----------|---------|-------------|--------|---------|
| **Display 2XL** | 72px (4.5rem) | 1.2 | 800 | Space Grotesk |
| **Display XL** | 60px (3.75rem) | 1.2 | 800 | Space Grotesk |
| **Display LG** | 48px (3rem) | 1.2 | 700 | Space Grotesk |
| **H1** | 36px (2.25rem) | 1.2 | 700 | Space Grotesk |
| **H2** | 30px (1.875rem) | 1.2 | 700 | Space Grotesk |
| **H3** | 24px (1.5rem) | 1.375 | 600 | Space Grotesk |
| **H4** | 20px (1.25rem) | 1.375 | 600 | Inter |
| **Body Large** | 18px (1.125rem) | 1.625 | 400 | Inter |
| **Body** | 16px (1rem) | 1.5 | 400 | Inter |
| **Body Small** | 14px (0.875rem) | 1.5 | 400 | Inter |
| **Caption** | 12px (0.75rem) | 1.5 | 500 | Inter |

### Exemplos de Uso

```css
/* Hero Title */
h1.hero {
  font-family: 'Space Grotesk';
  font-size: 3.75rem;
  font-weight: 800;
  line-height: 1.2;
  letter-spacing: -0.025em;
  color: #0F172A;
}

/* Section Title */
h2.section-title {
  font-family: 'Space Grotesk';
  font-size: 3rem;
  font-weight: 700;
  line-height: 1.2;
  color: #0F172A;
}

/* Body Text */
p {
  font-family: 'Inter';
  font-size: 1rem;
  line-height: 1.625;
  color: #334155;
}

/* Button Text */
button {
  font-family: 'Inter';
  font-size: 1rem;
  font-weight: 600;
  letter-spacing: 0;
}
```

---

## 🎨 Sistema de Design

### Espaçamento (8pt Grid)

```css
--zp-space-1:  4px    /* Tight spacing */
--zp-space-2:  8px    /* Minimal spacing */
--zp-space-3:  12px   /* Small spacing */
--zp-space-4:  16px   /* Base spacing */
--zp-space-5:  20px
--zp-space-6:  24px   /* Section spacing */
--zp-space-8:  32px   /* Large spacing */
--zp-space-10: 40px
--zp-space-12: 48px   /* XL spacing */
--zp-space-16: 64px   /* Section padding */
--zp-space-20: 80px
--zp-space-24: 96px   /* Page sections */
```

### Elevação (Shadows)

```css
/* Subtle - Cards, inputs */
box-shadow: 0 1px 3px 0 rgba(0, 0, 0, 0.1);

/* Medium - Dropdowns */
box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);

/* Large - Modals */
box-shadow: 0 10px 15px -3px rgba(0, 0, 0, 0.1);

/* XL - Hero cards */
box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.1);

/* Colored - Primary CTAs */
box-shadow: 0 10px 25px -5px rgba(99, 102, 241, 0.3);
```

### Border Radius

```css
--zp-radius-sm:  4px   /* Badges, tags */
--zp-radius-md:  6px   /* Inputs, small buttons */
--zp-radius-lg:  8px   /* Buttons, cards */
--zp-radius-xl:  12px  /* Large cards */
--zp-radius-2xl: 16px  /* Feature cards */
--zp-radius-3xl: 24px  /* Hero sections */
--zp-radius-full: 9999px  /* Pills, avatars */
```

### Transições

```css
/* Fast - Micro interactions */
transition: all 150ms cubic-bezier(0, 0, 0.2, 1);

/* Normal - Standard interactions */
transition: all 250ms cubic-bezier(0, 0, 0.2, 1);

/* Slow - Complex animations */
transition: all 350ms cubic-bezier(0, 0, 0.2, 1);
```

---

## 💬 Voz e Tom

### Princípios de Comunicação

1. **Claro e Direto**: Evite jargões desnecessários
2. **Profissional mas Acessível**: Enterprise sem ser frio
3. **Confiante mas Humilde**: Mostramos valor sem arrogância
4. **Técnico quando Necessário**: Profundidade para desenvolvedores

### Tom por Contexto

| Contexto | Tom | Exemplo |
|----------|-----|---------|
| **Marketing** | Inspirador e confiante | "Transforme pagamentos em crescimento" |
| **Documentação** | Claro e preciso | "Envie uma requisição POST para /api/v1/payments" |
| **Suporte** | Empático e solucionador | "Vamos resolver isso juntos" |
| **Erro** | Claro e orientador | "Não encontramos esse pagamento. Verifique o ID" |

### Palavras-Chave

**Usar** ✅:
- Simplificado, Escalável, Seguro, Instantâneo
- Transparente, Confiável, Moderno, Eficiente
- Crescimento, Inovação, Enterprise

**Evitar** ❌:
- Complicado, Difícil, Experimental
- Barato, Básico, Simples demais
- Revolucionário, Disruptivo (clichês)

---

## 📱 Aplicações

### Website

- **Hero Section**: Gradient background, título com gradient text, CTAs primários
- **Features**: Grid 3 colunas, ícones coloridos, cards com hover
- **Footer**: Background escuro (Neutral 900), logo branco

### Dashboard

- **Header**: Background branco, logo colorido, navegação limpa
- **Sidebar**: Background Neutral 50, ícones Primary 600
- **Cards**: Background branco, shadow-md, radius-xl
- **Buttons**: Primary 600, hover com shadow e transform

### Documentação

- **Code Blocks**: Background Neutral 900, syntax highlighting
- **Alerts**: Borders coloridos (Primary/Success/Warning/Error)
- **Navigation**: Sticky sidebar, active state Primary 600

---

## ✅ Checklist de Qualidade Enterprise

Use este checklist para garantir que cada elemento atende aos padrões premium:

### Visual

- [ ] Logo usa versão correta (cor/branco) para o fundo
- [ ] Área de proteção do logo é respeitada (2x altura do z)
- [ ] Cores seguem paleta definida (sem cores aleatórias)
- [ ] Tipografia usa apenas Inter e Space Grotesk
- [ ] Hierarquia tipográfica é clara
- [ ] Espaçamentos seguem grid de 8pt
- [ ] Sombras são sutis e consistentes
- [ ] Border radius é consistente

### Interação

- [ ] Hover states estão definidos em todos os links/botões
- [ ] Transições são suaves (250ms padrão)
- [ ] Focus states são visíveis para acessibilidade
- [ ] Animações têm propósito (não são decorativas)
- [ ] Loading states são claros

### Conteúdo

- [ ] Tom está apropriado para o contexto
- [ ] Linguagem é clara e direta
- [ ] Não há jargões desnecessários
- [ ] CTAs são claros e orientados a ação
- [ ] Mensagens de erro são úteis

### Técnico

- [ ] Código usa variáveis CSS do design system
- [ ] Classes seguem convenção (zp-*)
- [ ] Responsividade está implementada
- [ ] Acessibilidade (ARIA, alt text) está correta
- [ ] Performance (lazy loading, SVG) está otimizada

---

## 📚 Recursos

- **Figma**: [Link para projeto Figma]
- **Fonts**: [Google Fonts - Inter](https://fonts.google.com/specimen/Inter) | [Space Grotesk](https://fonts.google.com/specimen/Space+Grotesk)
- **Icons**: [Heroicons](https://heroicons.com) ou [Lucide](https://lucide.dev)
- **Illustrations**: [unDraw](https://undraw.co) customizadas com Primary 600

---

## 📝 Changelog

- **v1.0.0** (Janeiro 2025) - Brandbook inicial criado
  - Sistema de design completo
  - Logo profissional
  - Paleta enterprise
  - Tipografia definida
  - Guidelines de uso

---

## 📞 Contato

Para dúvidas sobre uso da marca ou solicitações especiais:
- **Email**: design@zendapag.com
- **Slack**: #design-system

---

**© 2025 Zendapag. Todos os direitos reservados.**
