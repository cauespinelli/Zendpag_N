# Secrets Setup Guide

Este guia detalha como configurar todos os secrets necessários para o pipeline CI/CD do Zendapag.

## 🔐 Secrets Overview

### Categorias de Secrets

- **🏗️ Infrastructure**: AWS, Kubernetes, Docker
- **🗄️ Database**: Conexões de banco de dados
- **🔒 Security**: JWT, PIX certificates, tokens
- **🔧 Tools**: SonarCloud, Semgrep, monitoring
- **📢 Notifications**: Slack webhooks

## ⚙️ Configuração no GitHub

### 1. Acessar Secrets Settings

```bash
# No repositório GitHub
Settings → Secrets and variables → Actions
```

### 2. Repository Secrets

#### Infrastructure Secrets

```yaml
AWS_ACCESS_KEY_ID:
  description: "AWS Access Key para EKS e ECR"
  value: "AKIA..."
  usage: "Deploy workflows"

AWS_SECRET_ACCESS_KEY:
  description: "AWS Secret Access Key"
  value: "..."
  usage: "Deploy workflows"
  sensitive: true

DOCKER_REGISTRY_URL:
  description: "URL do Docker Registry (ECR)"
  value: "123456789012.dkr.ecr.us-west-2.amazonaws.com"
  usage: "Build e deploy"
```

#### Database Secrets

```yaml
STAGING_DATABASE_URL:
  description: "URL completa do banco staging"
  value: "jdbc:postgresql://staging-db.internal:5432/zendapag"
  usage: "CD Staging"

STAGING_DATABASE_USERNAME:
  description: "Username do banco staging"
  value: "zendapag_staging"
  usage: "CD Staging"

STAGING_DATABASE_PASSWORD:
  description: "Password do banco staging"
  value: "..."
  usage: "CD Staging"
  sensitive: true

PROD_DATABASE_URL:
  description: "URL completa do banco produção"
  value: "jdbc:postgresql://prod-db.internal:5432/zendapag"
  usage: "CD Production"

PROD_DATABASE_USERNAME:
  description: "Username do banco produção"
  value: "zendapag_prod"
  usage: "CD Production"

PROD_DATABASE_PASSWORD:
  description: "Password do banco produção"
  value: "..."
  usage: "CD Production"
  sensitive: true
```

#### Security Secrets

```yaml
JWT_SECRET:
  description: "Secret para assinatura de JWT tokens"
  value: "super-secret-jwt-key-production-change-this"
  usage: "Aplicação"
  sensitive: true
  rotation: "Trimestral"

PIX_CERT_PRIVATE_KEY:
  description: "Chave privada do certificado PIX"
  value: "-----BEGIN PRIVATE KEY-----\n...\n-----END PRIVATE KEY-----"
  usage: "PIX Integration"
  sensitive: true
  format: "PEM"

PIX_CERT_CERTIFICATE:
  description: "Certificado público PIX"
  value: "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----"
  usage: "PIX Integration"
  format: "PEM"

PIX_CERT_CA:
  description: "Certificado da CA PIX"
  value: "-----BEGIN CERTIFICATE-----\n...\n-----END CERTIFICATE-----"
  usage: "PIX Integration"
  format: "PEM"
```

#### Tool Secrets

```yaml
SONAR_TOKEN:
  description: "Token do SonarCloud para análise de código"
  value: "squ_..."
  usage: "CI Pipeline"
  how_to_get: "SonarCloud → My Account → Security"

SEMGREP_APP_TOKEN:
  description: "Token do Semgrep para SAST"
  value: "..."
  usage: "Security Pipeline"
  how_to_get: "Semgrep → Settings → Tokens"
  optional: true

GITLEAKS_LICENSE:
  description: "Licença do GitLeaks (opcional para features extras)"
  value: "..."
  usage: "Security Pipeline"
  optional: true
```

#### Notification Secrets

```yaml
SLACK_WEBHOOK:
  description: "Webhook do Slack para notificações gerais"
  value: "https://hooks.slack.com/services/..."
  usage: "Todos os workflows"
  channel: "#ci-cd, #deployments, #releases"

SECURITY_SLACK_WEBHOOK:
  description: "Webhook do Slack para alertas de segurança"
  value: "https://hooks.slack.com/services/..."
  usage: "Security Pipeline"
  channel: "#security-alerts"
```

## 🏗️ Environment-Specific Secrets

### Environment: staging

```yaml
name: staging
secrets:
  - STAGING_DATABASE_URL
  - STAGING_DATABASE_USERNAME
  - STAGING_DATABASE_PASSWORD
  - JWT_SECRET
  - PIX_CERT_PRIVATE_KEY
  - PIX_CERT_CERTIFICATE
  - PIX_CERT_CA

protection_rules:
  - required_reviewers: 1
  - wait_timer: 0
```

### Environment: production

```yaml
name: production
secrets:
  - PROD_DATABASE_URL
  - PROD_DATABASE_USERNAME
  - PROD_DATABASE_PASSWORD
  - JWT_SECRET
  - PIX_CERT_PRIVATE_KEY
  - PIX_CERT_CERTIFICATE
  - PIX_CERT_CA

protection_rules:
  - required_reviewers: 2
  - wait_timer: 5  # 5 minutos
  - required_teams: ["security-team"]
```

### Environment: production-approval

```yaml
name: production-approval
protection_rules:
  - required_reviewers: 1
  - required_teams: ["release-team"]
  - environment_url: "https://github.com/${{ github.repository }}/actions/runs/${{ github.run_id }}"
```

## 🔄 Secret Generation

### JWT Secret

```bash
# Gerar JWT secret seguro
openssl rand -base64 64
# Ou usando Node.js
node -e "console.log(require('crypto').randomBytes(64).toString('base64'))"
```

### Database Passwords

```bash
# Gerar password forte
openssl rand -base64 32 | tr -d "=+/" | cut -c1-25
```

### PIX Certificates

```bash
# Para ambiente de desenvolvimento/staging (mock)
openssl req -x509 -newkey rsa:4096 -keyout pix-key.pem -out pix-cert.pem -days 365 -nodes

# Para produção: usar certificados fornecidos pelo Banco Central
```

## 🔧 AWS Setup

### 1. Criar IAM User para CI/CD

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "eks:DescribeCluster",
        "eks:DescribeNodegroup",
        "eks:ListClusters"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage",
        "ecr:PutImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload"
      ],
      "Resource": "*"
    }
  ]
}
```

### 2. Configurar ECR Repository

```bash
# Criar repositórios ECR
aws ecr create-repository --repository-name zendapag-api --region us-west-2
aws ecr create-repository --repository-name zendapag-worker --region us-west-2

# Configurar lifecycle policy
aws ecr put-lifecycle-policy \
  --repository-name zendapag-api \
  --lifecycle-policy-text file://ecr-lifecycle-policy.json
```

### 3. EKS Cluster Access

```bash
# Adicionar role de CI/CD ao ConfigMap
kubectl edit configmap aws-auth -n kube-system

# Adicionar:
mapUsers: |
  - userarn: arn:aws:iam::ACCOUNT:user/zendapag-cicd
    username: zendapag-cicd
    groups:
    - system:masters
```

## 📊 SonarCloud Setup

### 1. Criar Organização

1. Acessar [SonarCloud](https://sonarcloud.io)
2. Importar repositório GitHub
3. Configurar Quality Gate

### 2. Gerar Token

```bash
# SonarCloud → My Account → Security → Generate Tokens
# Nome: "zendapag-github-actions"
# Expiration: No expiration
```

### 3. Configurar Quality Gate

```yaml
quality_gate:
  conditions:
    - metric: new_coverage
      operator: LT
      threshold: 80
    - metric: new_duplicated_lines_density
      operator: GT
      threshold: 3
    - metric: new_maintainability_rating
      operator: GT
      threshold: 1
```

## 📢 Slack Setup

### 1. Criar Slack App

1. Acesse [Slack API](https://api.slack.com/apps)
2. Create New App → From scratch
3. Nome: "Zendapag CI/CD"

### 2. Configurar Webhooks

```bash
# Features → Incoming Webhooks → Activate
# Add New Webhook to Workspace
# Channels: #ci-cd, #deployments, #releases, #security-alerts
```

### 3. Configurar Permissões

```yaml
scopes:
  - chat:write
  - chat:write.public
  - channels:read
permissions:
  channels:
    - "#ci-cd"
    - "#deployments"
    - "#releases"
    - "#security-alerts"
```

## 🔄 Secret Rotation

### Cronograma de Rotação

```yaml
jwt_secret:
  frequency: "Trimestral"
  impact: "Requires application restart"
  procedure: "Generate new secret → Update GitHub → Deploy"

database_passwords:
  frequency: "Semestral"
  impact: "Database connection reset"
  procedure: "Update DB → Update secrets → Deploy"

pix_certificates:
  frequency: "Annual"
  impact: "PIX integration disruption"
  procedure: "Renew certs → Update secrets → Deploy → Test"

aws_keys:
  frequency: "Annual"
  impact: "Deploy pipeline disruption"
  procedure: "Create new user → Update secrets → Test → Deactivate old"
```

### Rotation Script

```bash
#!/bin/bash
# rotate-secrets.sh

SECRET_NAME=$1
NEW_VALUE=$2

echo "Rotating secret: $SECRET_NAME"

# Update in GitHub
gh secret set $SECRET_NAME --body "$NEW_VALUE"

# Update in staging first
kubectl patch secret $SECRET_NAME --type='json' \
  -p='[{"op": "replace", "path": "/data/value", "value": "'$(echo -n "$NEW_VALUE" | base64)'"}]' \
  -n zendapag-staging

# Test staging deployment
kubectl rollout restart deployment/zendapag-api -n zendapag-staging
kubectl rollout status deployment/zendapag-api -n zendapag-staging

# If successful, update production
kubectl patch secret $SECRET_NAME --type='json' \
  -p='[{"op": "replace", "path": "/data/value", "value": "'$(echo -n "$NEW_VALUE" | base64)'"}]' \
  -n zendapag-production

echo "Secret rotation completed: $SECRET_NAME"
```

## 🔍 Validation

### Secret Validation Script

```bash
#!/bin/bash
# validate-secrets.sh

echo "Validating GitHub Secrets..."

# Check required secrets
REQUIRED_SECRETS=(
  "AWS_ACCESS_KEY_ID"
  "AWS_SECRET_ACCESS_KEY"
  "DOCKER_REGISTRY_URL"
  "JWT_SECRET"
  "SLACK_WEBHOOK"
)

for secret in "${REQUIRED_SECRETS[@]}"; do
  if gh secret list | grep -q "$secret"; then
    echo "✅ $secret: Found"
  else
    echo "❌ $secret: Missing"
  fi
done

echo "Validation completed."
```

## 📋 Checklist

### Initial Setup

- [ ] AWS IAM user created with correct permissions
- [ ] ECR repositories created
- [ ] EKS cluster access configured
- [ ] Database secrets generated
- [ ] JWT secret generated
- [ ] PIX certificates obtained
- [ ] SonarCloud project configured
- [ ] Slack webhooks created
- [ ] All secrets added to GitHub
- [ ] Environment protection rules configured
- [ ] Validation script run successfully

### Regular Maintenance

- [ ] Secret expiration dates monitored
- [ ] Rotation schedule followed
- [ ] Access logs reviewed
- [ ] Unused secrets removed
- [ ] Security audit completed

## 🆘 Troubleshooting

### Common Issues

1. **AWS Access Denied**
   - Verificar IAM permissions
   - Confirmar região correta
   - Validar access keys

2. **Database Connection Failed**
   - Verificar URL format
   - Confirmar credentials
   - Testar conectividade

3. **SonarCloud Quality Gate Failed**
   - Verificar token validity
   - Confirmar project key
   - Revisar quality gate settings

4. **Slack Notifications Not Working**
   - Validar webhook URL
   - Confirmar channel permissions
   - Testar webhook manualmente