# Zendapag Logo & Brand Assets

## 🎨 Conceito do Logo

O logo Zendapag foi projetado para transmitir:
- **Confiança**: Formas geométricas sólidas
- **Tecnologia**: Elementos modernos e digitais
- **Movimento**: Fluidez do dinheiro (PIX)
- **Segurança**: Estabilidade visual

---

## 📐 Construção do Logo

### Símbolo (Icon)
O símbolo representa:
- **Movimento circular**: Fluxo contínuo de transações
- **Seta/Raio**: Velocidade (PIX instantâneo)
- **Forma geométrica**: Solidez e confiabilidade

### Logotipo (Wordmark)
- **Fonte**: Space Grotesk Bold
- **Peso**: 700
- **Case**: Lowercase (zendapag) - Moderno e acessível
- **Lettering**: Ligeiramente espaçado para legibilidade

---

## 🎨 Versões do Logo

### 1. Full Logo (Horizontal)
```
[Símbolo] zendapag
```
**Uso**: Cabeçalhos, website, materiais de marketing
**Tamanho mínimo**: 120px de largura
**Arquivo**: `zendapag-logo-full.svg`

### 2. Logo Vertical (Stacked)
```
[Símbolo]
zendapag
```
**Uso**: Aplicativos mobile, avatares quadrados
**Tamanho mínimo**: 80px de largura
**Arquivo**: `zendapag-logo-vertical.svg`

### 3. Símbolo Isolado (Icon Only)
```
[Símbolo]
```
**Uso**: Favicon, app icons, redes sociais
**Tamanho mínimo**: 32px x 32px
**Arquivo**: `zendapag-icon.svg`

### 4. Wordmark (Texto apenas)
```
zendapag
```
**Uso**: Casos onde o símbolo não é apropriado
**Arquivo**: `zendapag-wordmark.svg`

---

## 🎨 Variações de Cor

### Primary (Full Color)
- **Fundo**: Branco ou neutro claro
- **Símbolo**: Gradient (#6366F1 → #4F46E5)
- **Texto**: #1E293B (Neutral 800)

### Dark (Reversed)
- **Fundo**: Escuro (#1E293B ou mais escuro)
- **Símbolo**: Branco com gradient sutil
- **Texto**: Branco (#FFFFFF)

### Monochrome Dark
- **Fundo**: Branco
- **Logo completo**: #1E293B (Neutral 800)

### Monochrome Light
- **Fundo**: Escuro
- **Logo completo**: Branco (#FFFFFF)

---

## 📏 Área de Proteção

**Regra de ouro**: Manter espaço mínimo equivalente a **2x a altura do "z"** em todas as direções.

```
    [2x altura z]
    ↓
    ───────────────────
   |                   |
   |  [Logo Zendapag]  |
   |                   |
    ───────────────────
```

**Nunca**:
- Colocar elementos gráficos dentro da área de proteção
- Reduzir abaixo do tamanho mínimo
- Distorcer proporções
- Rotacionar o logo
- Aplicar efeitos não aprovados

---

## 📐 Tamanhos Mínimos

### Digital
- **Full Logo**: 120px largura
- **Logo Vertical**: 80px largura
- **Símbolo**: 32px x 32px

### Impressão
- **Full Logo**: 30mm largura
- **Logo Vertical**: 20mm largura
- **Símbolo**: 8mm x 8mm

---

## 🎨 Cores Oficiais do Logo

### Gradiente Principal
```css
background: linear-gradient(135deg, #6366F1 0%, #4F46E5 100%);
```

### Cores Alternativas
- **Primary Solid**: #6366F1 (Indigo 500)
- **Dark**: #1E293B (Slate 800)
- **Light**: #FFFFFF

---

## ❌ Uso Incorreto

**NÃO FAZER**:
- ❌ Mudar as cores do logo
- ❌ Distorcer ou esticar
- ❌ Rotacionar
- ❌ Adicionar sombras ou efeitos
- ❌ Usar em fundos de baixo contraste
- ❌ Recriar ou modificar o símbolo
- ❌ Usar fonte diferente no wordmark
- ❌ Colocar em caixa ou bordas não aprovadas

**FAZER**:
- ✅ Usar arquivos oficiais fornecidos
- ✅ Manter proporções originais
- ✅ Respeitar área de proteção
- ✅ Escolher versão apropriada para o contexto
- ✅ Garantir contraste adequado
- ✅ Usar em tamanhos acima do mínimo

---

## 📦 Arquivos Disponíveis

```
logo/
├── svg/
│   ├── zendapag-logo-full.svg          # Logo horizontal completo
│   ├── zendapag-logo-full-white.svg    # Versão branca
│   ├── zendapag-logo-vertical.svg      # Logo vertical
│   ├── zendapag-icon.svg               # Símbolo apenas
│   ├── zendapag-icon-white.svg         # Símbolo branco
│   └── zendapag-wordmark.svg           # Texto apenas
│
├── png/
│   ├── zendapag-logo-full@1x.png       # 300px
│   ├── zendapag-logo-full@2x.png       # 600px
│   ├── zendapag-logo-full@3x.png       # 900px
│   ├── zendapag-icon-32.png            # Favicon
│   ├── zendapag-icon-64.png
│   ├── zendapag-icon-128.png
│   ├── zendapag-icon-256.png
│   └── zendapag-icon-512.png
│
└── README.md                            # Este arquivo
```

---

## 💡 Exemplos de Uso

### Website Header
```html
<img src="logo/svg/zendapag-logo-full.svg"
     alt="Zendapag"
     width="180"
     height="40" />
```

### Favicon
```html
<link rel="icon" type="image/png"
      href="logo/png/zendapag-icon-32.png"
      sizes="32x32" />
```

### Open Graph / Social Media
```html
<meta property="og:image"
      content="logo/png/zendapag-logo-full@3x.png" />
```

### Footer (Dark Background)
```html
<img src="logo/svg/zendapag-logo-full-white.svg"
     alt="Zendapag" />
```

---

## 🎨 SVG Logo Code (Inline)

### Logo Principal (Simplificado para implementação)

```svg
<svg width="180" height="40" viewBox="0 0 180 40" fill="none" xmlns="http://www.w3.org/2000/svg">
  <!-- Símbolo com Gradient -->
  <defs>
    <linearGradient id="logo-gradient" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#6366F1" />
      <stop offset="100%" style="stop-color:#4F46E5" />
    </linearGradient>
  </defs>

  <!-- Icon: Círculo com raio/seta estilizado -->
  <circle cx="20" cy="20" r="18" fill="url(#logo-gradient)" />
  <path d="M20 10 L20 30 M14 20 L26 20 M23 17 L26 20 L23 23"
        stroke="white"
        stroke-width="2.5"
        stroke-linecap="round"
        stroke-linejoin="round" />

  <!-- Wordmark: "zendapag" -->
  <text x="50" y="28"
        font-family="'Space Grotesk', sans-serif"
        font-weight="700"
        font-size="24"
        fill="#1E293B">
    zendapag
  </text>
</svg>
```

---

## 📝 Notas de Implementação

1. **SVG Preferencial**: Sempre use SVG quando possível (escalável, menor tamanho)
2. **PNG para Raster**: Use PNG apenas quando SVG não for suportado
3. **Lazy Loading**: Implemente lazy loading para logos em imagens
4. **Alt Text**: Sempre inclua `alt="Zendapag"` para acessibilidade
5. **Dark Mode**: Troque automaticamente para versão branca em dark mode

---

## 🔄 Versão

**Versão atual**: 1.0.0
**Data**: Janeiro 2025
**Designer**: Zendapag Design Team
