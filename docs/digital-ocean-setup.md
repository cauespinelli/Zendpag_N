# Digital Ocean Setup Guide

Este guia detalha como configurar a infraestrutura no Digital Ocean e habilitar o deploy automático via GitHub Actions.

## 📋 Índice

1. [Pré-requisitos](#pré-requisitos)
2. [Criar Droplet](#criar-droplet)
3. [Configurar Container Registry](#configurar-container-registry)
4. [Setup do Droplet](#setup-do-droplet)
5. [Configurar GitHub Secrets](#configurar-github-secrets)
6. [Testar Deploy](#testar-deploy)
7. [Troubleshooting](#troubleshooting)

---

## 🎯 Pré-requisitos

- Conta no Digital Ocean
- `doctl` CLI instalado localmente
- GitHub CLI (`gh`) instalado
- Acesso ao repositório GitHub

### Instalar doctl

```bash
# Windows (via Chocolatey)
choco install doctl

# macOS
brew install doctl

# Linux
cd ~
wget https://github.com/digitalocean/doctl/releases/download/v1.98.0/doctl-1.98.0-linux-amd64.tar.gz
tar xf doctl-1.98.0-linux-amd64.tar.gz
sudo mv doctl /usr/local/bin

# Autenticar
doctl auth init
```

---

## 🖥️ Criar Droplet

### Opção 1: Via Web Console

1. Acesse https://cloud.digitalocean.com/droplets
2. Click "Create" → "Droplets"
3. Configure:
   - **Image**: Ubuntu 22.04 LTS
   - **Plan**: Basic
   - **CPU Options**: Regular (4 GB / 2 vCPUs - $24/month)
   - **Region**: New York 1 (ou mais próximo de você)
   - **Authentication**: SSH Key (recomendado)
   - **Hostname**: zendapag-prod

### Opção 2: Via doctl CLI

```bash
# Listar suas SSH keys
doctl compute ssh-key list

# Criar Droplet
doctl compute droplet create zendapag-prod \
  --image ubuntu-22-04-x64 \
  --size s-2vcpu-4gb \
  --region nyc1 \
  --ssh-keys YOUR_SSH_KEY_ID \
  --tag-names zendapag,production \
  --enable-monitoring \
  --enable-ipv6 \
  --wait

# Obter IP do Droplet
doctl compute droplet list | grep zendapag-prod
```

### Especificações Recomendadas

| Ambiente | Size | vCPUs | RAM | Disco | Custo/mês |
|----------|------|-------|-----|-------|-----------|
| Staging | s-1vcpu-2gb | 1 | 2 GB | 50 GB | $12 |
| Production | s-2vcpu-4gb | 2 | 4 GB | 80 GB | $24 |
| Production (Alta demanda) | s-4vcpu-8gb | 4 | 8 GB | 160 GB | $48 |

---

## 📦 Configurar Container Registry

### 1. Criar Container Registry

```bash
# Via doctl
doctl registry create zendapag --subscription-tier basic

# Verificar
doctl registry get
```

**Subscription Tiers:**
- **Basic**: $5/mês, 500MB storage, 500GB transfer
- **Professional**: $20/mês, 10GB storage, 1TB transfer

### 2. Gerar Token de Acesso

```bash
# Gerar token read/write
doctl registry kubernetes-manifest | grep docker-registry

# Ou via web console:
# https://cloud.digitalocean.com/account/api/tokens
```

1. Acesse: API → Tokens/Keys
2. Click "Generate New Token"
3. Nome: "zendapag-github-actions"
4. Scopes: Read + Write
5. Copie o token (só aparece uma vez!)

### 3. Testar Registry Login

```bash
# Login no registry
doctl registry login

# Testar push de imagem
docker tag hello-world registry.digitalocean.com/zendapag/test:latest
docker push registry.digitalocean.com/zendapag/test:latest

# Listar imagens
doctl registry repository list-v2
```

---

## 🔧 Setup do Droplet

### 1. Conectar ao Droplet

```bash
# Obter IP do droplet
export DROPLET_IP=$(doctl compute droplet list --format PublicIPv4 --no-header | grep zendapag)

# Conectar via SSH
ssh root@$DROPLET_IP
```

### 2. Instalar Dependências

```bash
# Atualizar sistema
apt update && apt upgrade -y

# Instalar Docker
curl -fsSL https://get.docker.com | sh

# Instalar Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Verificar instalação
docker --version
docker-compose --version
```

### 3. Configurar Firewall (UFW)

```bash
# Habilitar UFW
ufw allow OpenSSH
ufw allow 80/tcp      # HTTP
ufw allow 443/tcp     # HTTPS
ufw allow 8093/tcp    # Zendapag API
ufw allow 8094/tcp    # Zendapag Worker
ufw allow 3005/tcp    # Dashboard
ufw --force enable

# Verificar status
ufw status
```

### 4. Criar Estrutura de Diretórios

```bash
# Criar diretório da aplicação
mkdir -p /opt/zendapag
cd /opt/zendapag

# Criar diretórios de logs
mkdir -p logs/{api,worker}

# Criar diretório de dados
mkdir -p data/{postgres,redis,kafka,zookeeper}

# Permissões
chown -R root:root /opt/zendapag
chmod -R 755 /opt/zendapag
```

### 5. Configurar Variáveis de Ambiente

```bash
# Criar arquivo .env
cat > /opt/zendapag/.env << 'EOF'
# Database
POSTGRES_DB=zendapag
POSTGRES_USER=zendapag
POSTGRES_PASSWORD=CHANGE_THIS_PASSWORD

# Redis
REDIS_PASSWORD=CHANGE_THIS_PASSWORD

# Application
SPRING_PROFILES_ACTIVE=prod
JWT_SECRET=CHANGE_THIS_JWT_SECRET

# API Ports
API_PORT=8093
WORKER_PORT=8094
DASHBOARD_PORT=3005

# Database Ports
POSTGRES_PORT=5435
REDIS_PORT=6381

# Registry
REGISTRY_URL=registry.digitalocean.com/zendapag
EOF

# Gerar senhas seguras
export DB_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
export REDIS_PASSWORD=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-25)
export JWT_SECRET=$(openssl rand -base64 64)

# Atualizar .env com senhas geradas
sed -i "s/POSTGRES_PASSWORD=.*/POSTGRES_PASSWORD=$DB_PASSWORD/" .env
sed -i "s/REDIS_PASSWORD=.*/REDIS_PASSWORD=$REDIS_PASSWORD/" .env
sed -i "s/JWT_SECRET=.*/JWT_SECRET=$JWT_SECRET/" .env

# Proteger arquivo
chmod 600 .env

echo "✅ Variáveis de ambiente configuradas"
```

### 6. Login no Digital Ocean Registry

```bash
# Login com token
echo "YOUR_DO_REGISTRY_TOKEN" | docker login registry.digitalocean.com -u YOUR_DO_REGISTRY_TOKEN --password-stdin

# Ou via doctl
doctl registry login

echo "✅ Registry login configurado"
```

### 7. Configurar Docker Compose Override

```bash
# Criar docker-compose.override.yml
cat > /opt/zendapag/docker-compose.override.yml << 'EOF'
version: '3.8'

services:
  zendapag-api:
    image: registry.digitalocean.com/zendapag/zendapag-api:latest
    container_name: zendapag-api-prod
    restart: unless-stopped
    ports:
      - "8093:8080"
    env_file:
      - .env

  zendapag-worker:
    image: registry.digitalocean.com/zendapag/zendapag-worker:latest
    container_name: zendapag-worker-prod
    restart: unless-stopped
    ports:
      - "8094:8081"
    env_file:
      - .env

  zendapag-dashboard:
    image: registry.digitalocean.com/zendapag/zendapag-dashboard:latest
    container_name: zendapag-dashboard-prod
    restart: unless-stopped
    ports:
      - "3005:80"
    environment:
      - REACT_APP_API_URL=http://${DROPLET_IP}:8093
EOF
```

### 8. Configurar Monitoramento (Opcional)

```bash
# Instalar Node Exporter para Prometheus
docker run -d \
  --name node-exporter \
  --restart unless-stopped \
  -p 9100:9100 \
  prom/node-exporter

# Verificar
curl http://localhost:9100/metrics
```

---

## 🔐 Configurar GitHub Secrets

### Secrets Necessários

Configure os seguintes secrets no GitHub:

```bash
# Via GitHub CLI
cd /c/Projetos/zendapag

# 1. Token do Digital Ocean
gh secret set DO_TOKEN --body "dop_v1_your_token_here"

# 2. IP do Droplet
export DROPLET_IP=$(doctl compute droplet list --format PublicIPv4 --no-header | head -1)
gh secret set DO_DROPLET_IP --body "$DROPLET_IP"

# 3. SSH Private Key
gh secret set DO_SSH_KEY < ~/.ssh/id_rsa

# 4. Registry Token (mesmo do DO_TOKEN)
gh secret set DO_REGISTRY_TOKEN --body "dop_v1_your_token_here"

# 5. Slack Webhook (opcional)
gh secret set SLACK_WEBHOOK --body "https://hooks.slack.com/services/YOUR/WEBHOOK/URL"
```

### Lista Completa de Secrets

| Secret Name | Descrição | Como Obter | Obrigatório |
|-------------|-----------|------------|-------------|
| `DO_TOKEN` | Digital Ocean API Token | [DO Account → API → Tokens](https://cloud.digitalocean.com/account/api/tokens) | ✅ Sim |
| `DO_DROPLET_IP` | IP público do Droplet | `doctl compute droplet list` | ✅ Sim |
| `DO_SSH_KEY` | Chave SSH privada | `cat ~/.ssh/id_rsa` | ✅ Sim |
| `DO_REGISTRY_TOKEN` | Token do Container Registry | Mesmo que `DO_TOKEN` | ✅ Sim |
| `SLACK_WEBHOOK` | Webhook para notificações | [Slack Apps](https://api.slack.com/apps) | ❌ Opcional |

### Verificar Secrets Configurados

```bash
# Listar secrets
gh secret list

# Output esperado:
# DO_TOKEN               Updated 2025-01-20
# DO_DROPLET_IP          Updated 2025-01-20
# DO_SSH_KEY             Updated 2025-01-20
# DO_REGISTRY_TOKEN      Updated 2025-01-20
# SLACK_WEBHOOK          Updated 2025-01-20
```

---

## 🚀 Testar Deploy

### 1. Deploy Manual (Teste)

```bash
# No Droplet
ssh root@$DROPLET_IP

cd /opt/zendapag

# Pull das imagens (após build no GitHub)
docker pull registry.digitalocean.com/zendapag/zendapag-api:latest
docker pull registry.digitalocean.com/zendapag/zendapag-worker:latest

# Iniciar serviços de infraestrutura
docker-compose up -d postgres redis

# Aguardar inicialização
sleep 10

# Iniciar aplicação
docker-compose up -d zendapag-api zendapag-worker

# Verificar status
docker-compose ps

# Verificar logs
docker-compose logs -f zendapag-api
```

### 2. Trigger Deploy via GitHub Actions

```bash
# Fazer uma mudança e push
cd /c/Projetos/zendapag

echo "# Test deployment" >> README.md
git add README.md
git commit -m "test: Trigger Digital Ocean deployment"
git push origin main

# Acompanhar workflow
gh run watch
```

### 3. Deploy Manual via Workflow Dispatch

```bash
# Trigger manual
gh workflow run cd-digitalocean.yml \
  -f environment=production

# Ver logs
gh run list --workflow=cd-digitalocean.yml --limit 1
gh run view --log
```

### 4. Verificar Health Checks

```bash
# API Health
curl http://$DROPLET_IP:8093/actuator/health

# Worker Health
curl http://$DROPLET_IP:8094/actuator/health

# API Info
curl http://$DROPLET_IP:8093/actuator/info

# Métricas
curl http://$DROPLET_IP:8093/actuator/prometheus
```

---

## 🔍 Troubleshooting

### Problema: SSH Connection Failed

**Sintoma:** GitHub Actions não consegue conectar ao Droplet

**Soluções:**
```bash
# 1. Verificar se SSH key está correta
ssh -i ~/.ssh/id_rsa root@$DROPLET_IP

# 2. Verificar formato da chave no GitHub Secret
cat ~/.ssh/id_rsa | gh secret set DO_SSH_KEY

# 3. Verificar firewall
ssh root@$DROPLET_IP
ufw status
ufw allow OpenSSH
```

### Problema: Registry Login Failed

**Sintoma:** `Error: Cannot perform an interactive login from a non TTY device`

**Soluções:**
```bash
# 1. Regenerar token no Digital Ocean
# https://cloud.digitalocean.com/account/api/tokens

# 2. Atualizar secret
gh secret set DO_REGISTRY_TOKEN --body "dop_v1_NEW_TOKEN"

# 3. Testar login no Droplet
echo "$TOKEN" | docker login registry.digitalocean.com -u $TOKEN --password-stdin
```

### Problema: Health Check Failed

**Sintoma:** Health checks falhando após deploy

**Soluções:**
```bash
# 1. Verificar logs da aplicação
ssh root@$DROPLET_IP
cd /opt/zendapag
docker-compose logs zendapag-api --tail 100

# 2. Verificar se portas estão corretas
docker-compose ps
netstat -tlnp | grep -E "(8093|8094)"

# 3. Verificar variáveis de ambiente
docker exec zendapag-api-prod env | grep SPRING

# 4. Verificar conectividade do banco
docker exec zendapag-api-prod curl postgres:5432
```

### Problema: Out of Memory

**Sintoma:** Containers reiniciando frequentemente

**Soluções:**
```bash
# 1. Verificar uso de memória
docker stats

# 2. Limitar memória dos containers
# Editar docker-compose.yml:
services:
  zendapag-api:
    mem_limit: 1g
    mem_reservation: 512m

# 3. Aumentar tamanho do Droplet
doctl compute droplet resize DROPLET_ID --size s-4vcpu-8gb
```

### Problema: Disk Space Full

**Sintoma:** Sem espaço em disco

**Soluções:**
```bash
# 1. Verificar uso de disco
df -h

# 2. Limpar imagens antigas
docker system prune -a --volumes

# 3. Limpar logs antigos
journalctl --vacuum-time=7d

# 4. Limpar cache do APT
apt clean
```

---

## 📊 Monitoramento

### Comandos Úteis

```bash
# Status dos containers
docker-compose ps

# Logs em tempo real
docker-compose logs -f

# Uso de recursos
docker stats

# Disco
df -h

# Memória
free -h

# Processos
htop
```

### Metrics Endpoints

- **API Metrics**: http://$DROPLET_IP:8093/actuator/prometheus
- **Worker Metrics**: http://$DROPLET_IP:8094/actuator/prometheus
- **Node Exporter**: http://$DROPLET_IP:9100/metrics

---

## 🔄 Operações Comuns

### Rollback Manual

```bash
ssh root@$DROPLET_IP
cd /opt/zendapag

# Pull imagem stable
docker pull registry.digitalocean.com/zendapag/zendapag-api:stable

# Restart com stable
docker-compose down
docker-compose up -d
```

### Atualizar Variáveis de Ambiente

```bash
ssh root@$DROPLET_IP
cd /opt/zendapag

# Editar .env
nano .env

# Restart serviços
docker-compose down
docker-compose up -d
```

### Backup do Banco de Dados

```bash
# Criar backup
docker exec zendapag-postgres-prod pg_dump -U zendapag zendapag > backup_$(date +%Y%m%d).sql

# Restaurar backup
cat backup_20250120.sql | docker exec -i zendapag-postgres-prod psql -U zendapag -d zendapag
```

---

## 📝 Checklist de Configuração

- [ ] Droplet criado e acessível via SSH
- [ ] Docker e Docker Compose instalados
- [ ] Container Registry criado no Digital Ocean
- [ ] Registry token gerado
- [ ] Firewall configurado (UFW)
- [ ] Diretórios criados em `/opt/zendapag`
- [ ] Arquivo `.env` configurado
- [ ] Registry login configurado
- [ ] Secrets configurados no GitHub
- [ ] Workflow `cd-digitalocean.yml` commitado
- [ ] Deploy manual testado
- [ ] Health checks funcionando
- [ ] Monitoramento configurado (opcional)

---

## 🆘 Suporte

- **Digital Ocean Docs**: https://docs.digitalocean.com/
- **doctl Reference**: https://docs.digitalocean.com/reference/doctl/
- **GitHub Actions**: https://docs.github.com/actions
- **Zendapag Issues**: https://github.com/klebergobbi/zendapag/issues
