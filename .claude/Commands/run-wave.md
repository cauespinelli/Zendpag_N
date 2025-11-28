# 🎯 Comandos Rápidos de Execução

## Executar TUDO de uma vez
```bash
claude-code "
INSTRUÇÕES CRÍTICAS:
1. Leia completamente /Commands/exec-prompt.md - essas são suas diretrizes fundamentais
2. Leia completamente /Commands/execution-plan.md - esse é seu plano detalhado
3. Siga RIGOROSAMENTE ambos os documentos
4. Execute TODAS as ondas sequencialmente
5. NUNCA encerre sem completar totalmente cada agente
6. Atualize IMPLEMENTATION_TRACKER.md após cada agente
7. Pense passo a passo, raciocínio minucioso

COMEÇAR AGORA pela ONDA 1, AGENTE 1.1"
```

## Executar por Onda

### Onda 1
```bash
claude-code "Leia /Commands/exec-prompt.md e /Commands/execution-plan.md. Execute ONDA 1 completa seguindo todas as diretrizes."
```

### Onda 2
```bash
claude-code "Leia /Commands/exec-prompt.md e /Commands/execution-plan.md. Execute ONDA 2 completa seguindo todas as diretrizes."
```

### Onda 3
```bash
claude-code "Leia /Commands/exec-prompt.md e /Commands/execution-plan.md. Execute ONDA 3 completa seguindo todas as diretrizes."
```

### Onda 4
```bash
claude-code "Leia /Commands/exec-prompt.md e /Commands/execution-plan.md. Execute ONDA 4 completa seguindo todas as diretrizes."
```

## Executar Agente Específico
```bash
# Exemplo: apenas Pagamentos
claude-code "Leia /Commands/exec-prompt.md e /Commands/execution-plan.md. Execute apenas AGENTE 1.1 (Pagamentos) seguindo todas as diretrizes."
```
```

---

## ✅ Resultado Final

Agora você tem uma estrutura completamente integrada:
```
/Commands/
  ├── exec-prompt.md          ← Suas instruções fundamentais (já existe)
  ├── execution-plan.md       ← Plano detalhado de ondas (criar)
  └── run-wave.md            ← Comandos facilitadores (criar)

/IMPLEMENTATION_TRACKER.md     ← Status tracker (já existe)