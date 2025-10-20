# Zendapag Kubernetes Infrastructure

Esta documentação descreve a infraestrutura Kubernetes completa para a Zendapag, incluindo configurações AWS, Helm Charts e scripts de deployment.

## 📋 Visão Geral

A infraestrutura Kubernetes da Zendapag está organizada da seguinte forma:

```
k8s/
├── aws-infrastructure/           # Infraestrutura AWS (Terraform)
├── manifests/                   # Manifestos base do Kubernetes
├── helm/                       # Helm Charts
└── README.md                   # Esta documentação
```

## 🏗️ Componentes da Infraestrutura

### AWS Infrastructure (Terraform)

- **EKS Cluster**: Kubernetes gerenciado com node groups dedicados
- **RDS PostgreSQL**: Banco de dados principal com Multi-AZ para produção
- **ElastiCache Redis**: Cache distribuído com clustering
- **MSK Kafka**: Message broker para processamento assíncrono
- **Application Load Balancer**: Balanceamento de carga para APIs
- **S3 Buckets**: Armazenamento para logs, assets e backups
- **Route53**: DNS management com certificados SSL/TLS
- **Security Groups**: Políticas de rede granulares
- **IAM Roles**: Permissões específicas para cada componente

### Helm Charts

O Helm Chart principal (`helm/zendapag/`) contém:

- **Deployments**: API e Worker com configurações específicas
- **Services**: Exposição de serviços internos e externos
- **Ingress**: Roteamento HTTP/HTTPS
- **HPA**: Auto-scaling baseado em métricas
- **ServiceMonitor**: Integração com Prometheus
- **NetworkPolicies**: Segurança de rede no cluster
- **PodDisruptionBudgets**: Garantia de disponibilidade

### Manifests Base

Configurações Kubernetes organizadas por ambiente:
- **base/**: Recursos compartilhados
- **overlays/**: Customizações por ambiente (staging/production)

## 🚀 Getting Started

### Pré-requisitos

1. **Ferramentas necessárias:**
   ```bash
   # AWS CLI
   curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
   unzip awscliv2.zip && sudo ./aws/install

   # Terraform
   wget -O- https://apt.releases.hashicorp.com/gpg | gpg --dearmor | sudo tee /usr/share/keyrings/hashicorp-archive-keyring.gpg
   echo "deb [signed-by=/usr/share/keyrings/hashicorp-archive-keyring.gpg] https://apt.releases.hashicorp.com $(lsb_release -cs) main" | sudo tee /etc/apt/sources.list.d/hashicorp.list
   sudo apt update && sudo apt install terraform

   # kubectl
   curl -LO "https://dl.k8s.io/release/$(curl -L -s https://dl.k8s.io/release/stable.txt)/bin/linux/amd64/kubectl"
   sudo install -o root -g root -m 0755 kubectl /usr/local/bin/kubectl

   # Helm
   curl https://get.helm.sh/helm-v3.12.0-linux-amd64.tar.gz | tar -xzO linux-amd64/helm > helm
   sudo install helm /usr/local/bin/
   ```

2. **Configurar credenciais AWS:**
   ```bash
   aws configure --profile zendapag-dev
   aws configure --profile zendapag-staging
   aws configure --profile zendapag-prod
   ```

### Deploy da Infraestrutura AWS

1. **Setup inicial (executar uma vez por conta AWS):**
   ```bash
   cd k8s/aws-infrastructure/scripts
   ./deploy.sh setup-backend
   ```

2. **Deploy por ambiente:**
   ```bash
   # Staging
   ./deploy.sh staging apply

   # Production
   ./deploy.sh prod apply
   ```

3. **Configurar kubeconfig:**
   ```bash
   ./kubeconfig.sh staging
   ./kubeconfig.sh prod
   ```

### Deploy da Aplicação

1. **Usando Helm (recomendado):**
   ```bash
   # Staging
   helm upgrade --install zendapag ./k8s/helm/zendapag \
     --namespace zendapag-staging \
     --create-namespace \
     --values ./k8s/helm/zendapag/values-staging.yaml

   # Production
   helm upgrade --install zendapag ./k8s/helm/zendapag \
     --namespace zendapag-production \
     --create-namespace \
     --values ./k8s/helm/zendapag/values-production.yaml
   ```

2. **Usando Kustomize:**
   ```bash
   # Staging
   kubectl apply -k k8s/manifests/overlays/staging

   # Production
   kubectl apply -k k8s/manifests/overlays/production
   ```

## ⚙️ Configuração

### Variáveis de Ambiente

Principais variáveis configuráveis no Terraform:

```hcl
# terraform.tfvars
environment = "staging"  # ou "prod"
aws_region = "us-west-2"
domain_name = "yourdomain.com"  # opcional

# VPC
vpc_cidr = "10.0.0.0/16"

# Database
rds_instance_class = "db.r6g.large"
rds_allocated_storage = 100

# Cache
redis_node_type = "cache.r6g.medium"
redis_num_cache_clusters = 2

# Kafka
kafka_instance_type = "kafka.m5.large"
kafka_number_of_broker_nodes = 3
```

### Valores do Helm Chart

Principais configurações no `values.yaml`:

```yaml
# Replicas por ambiente
replicaCount:
  api: 3
  worker: 2

# Recursos
resources:
  api:
    requests:
      memory: "512Mi"
      cpu: "500m"
    limits:
      memory: "1Gi"
      cpu: "1000m"

# Auto-scaling
autoscaling:
  enabled: true
  api:
    minReplicas: 3
    maxReplicas: 10
    targetCPUUtilizationPercentage: 70

# Monitoring
monitoring:
  enabled: true
  serviceMonitor:
    enabled: true
```

## 🔒 Segurança

### Network Policies

- **API Pods**: Acesso restrito a PostgreSQL, Redis, Kafka e egress HTTPS
- **Worker Pods**: Acesso a bancos de dados e Kafka, sem ingress público
- **Deny All**: Política padrão de bloqueio (quando habilitada)

### Security Groups AWS

- **EKS Nodes**: Comunicação entre nodes e cluster
- **RDS**: Acesso apenas dos nodes EKS na porta 5432
- **Redis**: Acesso apenas dos nodes EKS na porta 6379
- **MSK**: Acesso apenas dos nodes EKS nas portas Kafka

### IAM Roles

- **EKS Service Role**: Permissões para gerenciar o cluster
- **EKS Node Group Role**: Permissões para nodes (ECR, EBS CSI, CNI)
- **ZendapagAdminRole**: Acesso administrativo ao cluster

## 📊 Monitoramento

### Métricas Principais

1. **API Metrics:**
   - Taxa de erro HTTP
   - Latência P95
   - Throughput (requests/segundo)
   - Status dos pods

2. **Worker Metrics:**
   - Consumer lag do Kafka
   - Taxa de erro de processamento
   - Mensagens processadas/segundo

3. **Infrastructure Metrics:**
   - Uso de CPU/Memória
   - Status dos nodes
   - Utilização de PVCs

### Alertas (Prometheus Rules)

- **ZendapagAPIHighErrorRate**: Taxa de erro > 5% por 2 minutos
- **ZendapagAPIHighLatency**: P95 > 1s por 5 minutos
- **ZendapagWorkerKafkaConsumerLag**: Lag > 1000 mensagens
- **ZendapagHighMemoryUsage**: Uso de memória > 80%
- **ZendapagPodCrashLooping**: Pods reiniciando constantemente

## 🔄 Operações

### Comandos Úteis

```bash
# Verificar status do cluster
kubectl get nodes
kubectl get pods -n zendapag-staging

# Logs em tempo real
kubectl logs -l app.kubernetes.io/component=api -n zendapag-staging -f
kubectl logs -l app.kubernetes.io/component=worker -n zendapag-staging -f

# Port forward para testes locais
kubectl port-forward svc/zendapag-api 8080:8080 -n zendapag-staging

# Scaling manual
kubectl scale deployment/zendapag-api --replicas=5 -n zendapag-staging

# Verificar recursos
kubectl top pods -n zendapag-staging
kubectl describe hpa zendapag-api-hpa -n zendapag-staging
```

### Troubleshooting

1. **Pods não iniciam:**
   ```bash
   kubectl describe pod <pod-name> -n <namespace>
   kubectl logs <pod-name> -n <namespace> --previous
   ```

2. **Problemas de rede:**
   ```bash
   kubectl get networkpolicies -n <namespace>
   kubectl describe networkpolicy <policy-name> -n <namespace>
   ```

3. **Issues de autoscaling:**
   ```bash
   kubectl describe hpa -n <namespace>
   kubectl get --raw /apis/metrics.k8s.io/v1beta1/pods
   ```

### Backup e Recovery

1. **Database Backups:**
   - RDS: Snapshots automáticos configurados
   - Retenção: 30 dias (prod), 7 dias (staging)

2. **Application Backups:**
   - Configurações: Versionadas no Git
   - Secrets: Gerenciados pelo AWS Secrets Manager

3. **Disaster Recovery:**
   - Multi-AZ deployment para produção
   - Cross-region backups disponíveis

## 📈 Escalabilidade

### Horizontal Pod Autoscaler (HPA)

- **API**: 3-10 replicas baseado em CPU e memória
- **Worker**: 2-8 replicas baseado em Kafka consumer lag

### Cluster Autoscaler

- **Application Nodes**: 3-15 nodes conforme demanda
- **System Nodes**: 2-4 nodes para componentes do sistema

### Database Scaling

- **RDS**: Read replicas para scaling de leitura
- **Redis**: Cluster mode para distribuição de carga

## 🔧 Manutenção

### Updates de Segurança

1. **Node Groups**: Atualização rolling automática
2. **EKS Control Plane**: Atualização gerenciada pela AWS
3. **Container Images**: Pipeline CI/CD automatizado

### Monitoramento de Custos

- **AWS Cost Explorer**: Análise de custos por serviço
- **Resource Tags**: Rastreamento por ambiente e projeto
- **Spot Instances**: Para cargas de trabalho não-críticas

## 📚 Referências

- [EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Helm Documentation](https://helm.sh/docs/)
- [Terraform AWS Provider](https://registry.terraform.io/providers/hashicorp/aws/latest/docs)
- [Prometheus Operator](https://github.com/prometheus-operator/prometheus-operator)

## 🤝 Contribuindo

1. Faça fork do repositório
2. Crie uma branch para sua feature (`git checkout -b feature/nova-feature`)
3. Commit suas mudanças (`git commit -am 'Add nova feature'`)
4. Push para a branch (`git push origin feature/nova-feature`)
5. Abra um Pull Request

## 📞 Suporte

Para suporte técnico ou dúvidas sobre a infraestrutura:

- **Slack**: #zendapag-infra
- **Email**: infra@zendapag.com
- **Runbook**: `/docs/runbooks/`

---

**Zendapag Infrastructure Team** | Versão 1.0 | Atualizado em $(date +%Y-%m-%d)