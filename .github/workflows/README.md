# GitHub Actions Workflows

Este diretório contém todos os workflows de CI/CD do projeto Zendapag.

## 📋 Workflows Disponíveis

### 1. Continuous Integration (`ci.yml`)

**Trigger:** Pull Requests e Push para `main`/`develop`

**Jobs:**
- ✅ Detecta mudanças em código Java/Docker
- ✅ Executa testes unitários e de integração
- ✅ Verifica code coverage (>80%)
- ✅ Análise SonarCloud
- ✅ Build de artefatos Maven
- ✅ Build e teste de imagens Docker
- ✅ Quality gates

**Duração estimada:** 8-12 minutos

---

### 2. Security Scans (`security.yml`)

**Trigger:** Diário às 2h UTC, Push/PR, Manual

**Jobs:**
- 🔒 OWASP Dependency Check
- 🔍 CodeQL SAST Analysis
- 🛡️ Semgrep Security Scan
- 🐳 Trivy Container Scanning
- 🔑 GitLeaks Secret Detection
- 📄 License Compliance Check

**Duração estimada:** 15-20 minutos

---

### 3. Deploy to Digital Ocean (`cd-digitalocean.yml`) ⭐ **NOVO**

**Trigger:** Push para `main`, Manual dispatch

**Jobs:**

#### Build & Push
- 📦 Compila código Java com Maven
- 🐳 Build de imagens Docker:
  - `zendapag-api`
  - `zendapag-worker`
  - `zendapag-dashboard`
- 📤 Push para Digital Ocean Container Registry
- 🏷️ Tags: `latest` e `version-sha-timestamp`

#### Deploy to Droplet
- 📥 Copia arquivos de deployment para Droplet
- 🔐 Login no DO Registry
- 📥 Pull de imagens mais recentes
- 🛑 Para containers antigos
- 🔧 Inicia infraestrutura (PostgreSQL, Redis, Kafka)
- 🚀 Inicia aplicações (API, Worker)
- 🏥 Health checks
- ✅ Verificação de deployment

#### Notifications
- 📢 Notificações Slack (sucesso/falha)
- 📊 Métricas de deployment

#### Rollback (em caso de falha)
- 🔄 Restaura versão estável automaticamente
- 🚨 Alerta equipe via Slack

**Duração estimada:** 10-15 minutos

**Ambientes:**
- Production (padrão)
- Staging (via manual dispatch)

---

### 4. Deploy to Staging (`cd-staging.yml`)

**Trigger:** Push para `develop`

**Target:** AWS EKS Staging

**Jobs:**
- Build e push para ECR
- Deploy via Helm
- Health checks
- Rollback automático

**Status:** ⚠️ Configurado para AWS (não Digital Ocean)

---

### 5. Deploy to Production (`cd-production.yml`)

**Trigger:** Release publicado, Manual com aprovação

**Target:** AWS EKS Production

**Jobs:**
- Aprovação manual obrigatória
- Pre-deployment tests
- Build com assinatura de imagens (Cosign)
- Blue-Green deployment
- Traffic switching
- Monitoramento pós-deploy (5 min)
- Rollback em caso de falha

**Status:** ⚠️ Configurado para AWS (não Digital Ocean)

---

### 6. Release (`release.yml`)

**Trigger:** Tags `v*.*.*`, Manual

**Jobs:**
- Validação de versão
- Testes completos
- Build de artefatos
- Imagens Docker multi-arch
- Criação de GitHub Release
- Geração de changelog
- Helm chart packaging
- Notificações

**Duração estimada:** 20-25 minutos

---

## 🔐 Secrets Necessários

### Digital Ocean Deploy

| Secret | Descrição | Obrigatório |
|--------|-----------|-------------|
| `DO_TOKEN` | Digital Ocean API Token | ✅ |
| `DO_DROPLET_IP` | IP do Droplet de produção | ✅ |
| `DO_SSH_KEY` | Chave SSH privada | ✅ |
| `DO_REGISTRY_TOKEN` | Token do Container Registry | ✅ |
| `SLACK_WEBHOOK` | Webhook para notificações | ❌ |

### CI/CD Geral

| Secret | Descrição | Obrigatório |
|--------|-----------|-------------|
| `SONAR_TOKEN` | Token do SonarCloud | ❌ |
| `SEMGREP_APP_TOKEN` | Token do Semgrep | ❌ |
| `CODECOV_TOKEN` | Token do Codecov | ❌ |

### AWS (se usar workflows AWS)

| Secret | Descrição | Obrigatório |
|--------|-----------|-------------|
| `AWS_ACCESS_KEY_ID` | AWS Access Key | ✅ |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Key | ✅ |
| `DOCKER_REGISTRY_URL` | URL do ECR | ✅ |

---

## 🚀 Como Usar

### Deploy Automático para Digital Ocean

1. Faça commit e push para `main`:
```bash
git add .
git commit -m "feat: nova funcionalidade"
git push origin main
```

2. O workflow `cd-digitalocean.yml` será executado automaticamente

3. Acompanhe o progresso:
```bash
gh run watch
```

### Deploy Manual

```bash
# Trigger manual do workflow
gh workflow run cd-digitalocean.yml \
  -f environment=production

# Ou via interface web
# https://github.com/klebergobbi/zendapag/actions/workflows/cd-digitalocean.yml
```

### Monitorar Workflows

```bash
# Listar últimas execuções
gh run list --limit 10

# Ver logs de uma execução
gh run view RUN_ID --log

# Ver logs em tempo real
gh run watch RUN_ID

# Cancelar execução
gh run cancel RUN_ID
```

---

## 📊 Status Badges

Adicione ao README.md:

```markdown
[![CI](https://github.com/klebergobbi/zendapag/actions/workflows/ci.yml/badge.svg)](https://github.com/klebergobbi/zendapag/actions/workflows/ci.yml)
[![Security](https://github.com/klebergobbi/zendapag/actions/workflows/security.yml/badge.svg)](https://github.com/klebergobbi/zendapag/actions/workflows/security.yml)
[![Deploy DO](https://github.com/klebergobbi/zendapag/actions/workflows/cd-digitalocean.yml/badge.svg)](https://github.com/klebergobbi/zendapag/actions/workflows/cd-digitalocean.yml)
```

---

## 🔧 Customização

### Adicionar Novo Ambiente

1. Edite `cd-digitalocean.yml`:
```yaml
workflow_dispatch:
  inputs:
    environment:
      options:
        - production
        - staging
        - development  # Novo ambiente
```

2. Configure secrets específicos do ambiente

3. Ajuste lógica de deploy conforme necessário

### Modificar Health Checks

Edite o job `deploy-to-droplet` em `cd-digitalocean.yml`:

```yaml
- name: Health check API
  run: |
    # Customize timeout, retries, endpoints
    for i in {1..20}; do  # Aumentar tentativas
      if curl -f http://${{ secrets.DO_DROPLET_IP }}:8093/custom/endpoint; then
        echo "✅ Custom health check passed"
        break
      fi
      sleep 5
    done
```

---

## 🐛 Troubleshooting

### Workflow Falhando

1. **Verificar secrets:**
```bash
gh secret list
```

2. **Ver logs detalhados:**
```bash
gh run view --log-failed
```

3. **Testar localmente (act):**
```bash
# Instalar act
choco install act

# Executar workflow localmente
act -j build-and-push --secret-file .secrets
```

### Build Falhando

- Verificar se Maven build funciona localmente
- Checar dependências no `pom.xml`
- Validar versão do Java (17)

### Deploy Falhando

- Verificar conectividade SSH
- Confirmar que Droplet está acessível
- Verificar logs no Droplet:
```bash
ssh root@$DROPLET_IP
cd /opt/zendapag
docker-compose logs
```

---

## 📚 Documentação Adicional

- [Digital Ocean Setup Guide](../../docs/digital-ocean-setup.md)
- [CI/CD Documentation](../../docs/ci-cd.md)
- [Secrets Setup Guide](../../docs/secrets-setup.md)
- [GitHub Actions Docs](https://docs.github.com/actions)

---

## 🔄 Manutenção

### Atualizar Workflows

1. Edite o workflow desejado
2. Teste localmente (se possível)
3. Commit e push
4. Monitore primeira execução

### Rotação de Secrets

```bash
# Exemplo: Atualizar DO_TOKEN
gh secret set DO_TOKEN --body "novo_token"

# Trigger redeploy para aplicar
gh workflow run cd-digitalocean.yml
```

---

## 📈 Métricas

Monitore performance dos workflows:

- **Build time**: Target < 10 minutos
- **Deploy time**: Target < 15 minutos
- **Success rate**: Target > 95%
- **MTTR**: Target < 30 minutos

Dashboard: https://github.com/klebergobbi/zendapag/actions

---

## 🆘 Suporte

- **Issues**: https://github.com/klebergobbi/zendapag/issues
- **Discussions**: https://github.com/klebergobbi/zendapag/discussions
- **GitHub Actions Docs**: https://docs.github.com/actions
