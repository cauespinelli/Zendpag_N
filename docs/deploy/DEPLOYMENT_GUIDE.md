# Guia de Deploy - Módulo de Saque PIX

Este guia descreve os passos para realizar o deploy do módulo de Saque PIX no ambiente Zendapag.

## 📋 Pré-requisitos

- Java 17+
- Maven 3.8+
- Node.js 18+
- Docker e Docker Compose
- PostgreSQL 14+ (ou Docker)
- Redis 7+ (ou Docker)
- Apache Kafka 3+ (ou Docker)
- Acesso ao repositório Git

## 🚀 Passo a Passo do Deploy

### 1. Atualizar o Código do Repositório

```bash
cd /c/Projetos/zendapag

# Atualizar branch principal
git pull origin main

# Ou fazer checkout de uma branch específica
# git checkout feature/pix-withdrawal-module
# git pull origin feature/pix-withdrawal-module
```

### 2. Executar Migrações do Banco de Dados

#### Opção A: Via Flyway (Recomendado)

```bash
# Ambiente de desenvolvimento
./mvnw flyway:migrate -Dflyway.configFiles=flyway-dev.conf

# Ambiente de staging
./mvnw flyway:migrate -Dflyway.configFiles=flyway-staging.conf

# Ambiente de produção
./mvnw flyway:migrate -Dflyway.configFiles=flyway-prod.conf
```

#### Opção B: Manualmente via psql

```bash
# Conectar ao banco de dados
psql -h localhost -U zendapag -d zendapag_db

# Executar a migration
\i zendapag-core/src/main/resources/db/migration/V013__create_pix_withdrawals.sql

# Verificar que a tabela foi criada
\dt pix_withdrawals
```

### 3. Criar Tópicos do Kafka

```bash
cd /c/Projetos/zendapag/kafka

# Ambiente de desenvolvimento
./create-withdrawal-topics.sh dev

# Ambiente de staging
./create-withdrawal-topics.sh staging

# Ambiente de produção
./create-withdrawal-topics.sh prod
```

**Verificar os tópicos criados:**

```bash
# Listar tópicos
kafka-topics.sh --list --bootstrap-server localhost:9092 | grep withdrawal

# Descrever um tópico específico
kafka-topics.sh --describe --bootstrap-server localhost:9092 --topic withdrawal-events-dev
```

### 4. Configurar Variáveis de Ambiente

#### Desenvolvimento (application.yml)

As configurações já estão no arquivo `application.yml` para o perfil `dev`.

#### Staging/Produção (Variáveis de Ambiente)

```bash
# Banco de Dados
export DB_HOST=postgres.example.com
export DB_PORT=5432
export DB_NAME=zendapag_prod
export DB_USERNAME=zendapag_user
export DB_PASSWORD=super_secret_password

# Redis
export REDIS_HOST=redis.example.com
export REDIS_PORT=6379
export REDIS_PASSWORD=redis_password

# Kafka
export KAFKA_BOOTSTRAP_SERVERS=kafka-1.example.com:9092,kafka-2.example.com:9092

# JWT
export JWT_SECRET=your-very-secret-jwt-key-min-256-bits
export JWT_EXPIRATION=86400000

# Withdrawal Configuration
export WITHDRAWAL_MAX_AMOUNT=50000.00
export WITHDRAWAL_MIN_AMOUNT=0.01
export WITHDRAWAL_DAILY_LIMIT=100000.00
export WITHDRAWAL_FEE_PERCENTAGE=0.00
export WITHDRAWAL_FIXED_FEE=0.00
export WITHDRAWAL_MAX_PENDING=5
```

### 5. Build do Projeto

#### Backend (Java/Spring Boot)

```bash
cd /c/Projetos/zendapag

# Limpar e compilar
./mvnw clean install -DskipTests

# Com testes
./mvnw clean install

# Build dos módulos específicos
./mvnw clean package -pl zendapag-core,zendapag-api,zendapag-worker
```

#### Frontend (React/TypeScript)

```bash
cd /c/Projetos/zendapag/zendapag-dashboard

# Instalar dependências
npm install

# Build de produção
npm run build

# O build estará em: zendapag-dashboard/dist
```

### 6. Build Docker Images

```bash
cd /c/Projetos/zendapag

# Build de todas as imagens
docker-compose build

# Build de imagens específicas
docker-compose build zendapag-api
docker-compose build zendapag-worker
docker-compose build zendapag-dashboard
```

### 7. Deploy da Infraestrutura

#### Opção A: Docker Compose (Desenvolvimento/Staging)

```bash
cd /c/Projetos/zendapag

# Iniciar toda a stack
docker-compose up -d

# Ou iniciar serviços específicos
docker-compose up -d postgres redis kafka zookeeper

# Verificar logs
docker-compose logs -f zendapag-api
docker-compose logs -f zendapag-worker
```

#### Opção B: Kubernetes (Produção)

```bash
cd /c/Projetos/zendapag/k8s

# Aplicar configurações
kubectl apply -f namespace.yaml
kubectl apply -f configmap.yaml
kubectl apply -f secrets.yaml
kubectl apply -f deployment-api.yaml
kubectl apply -f deployment-worker.yaml
kubectl apply -f deployment-dashboard.yaml
kubectl apply -f service.yaml
kubectl apply -f ingress.yaml

# Verificar pods
kubectl get pods -n zendapag

# Verificar logs
kubectl logs -f deployment/zendapag-api -n zendapag
kubectl logs -f deployment/zendapag-worker -n zendapag
```

### 8. Deploy das Aplicações

#### API

```bash
# Via Docker Compose
docker-compose up -d zendapag-api

# Via Java direto (desenvolvimento)
cd /c/Projetos/zendapag/zendapag-api
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Worker

```bash
# Via Docker Compose
docker-compose up -d zendapag-worker

# Via Java direto (desenvolvimento)
cd /c/Projetos/zendapag/zendapag-worker
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

#### Dashboard

```bash
# Via Docker Compose
docker-compose up -d zendapag-dashboard

# Via servidor web estático (Nginx/Apache)
cp -r /c/Projetos/zendapag/zendapag-dashboard/dist/* /var/www/html/

# Via servidor Node.js simples (desenvolvimento)
cd /c/Projetos/zendapag/zendapag-dashboard
npm run preview
```

### 9. Verificações Pós-Deploy

#### Health Checks

```bash
# API Health
curl http://localhost:8091/actuator/health

# Worker Health
curl http://localhost:8092/actuator/health

# Dashboard
curl http://localhost:3005
```

#### Verificar Banco de Dados

```bash
psql -h localhost -U zendapag -d zendapag_db -c "SELECT COUNT(*) FROM pix_withdrawals;"
```

#### Verificar Kafka

```bash
# Listar consumer groups
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --list | grep withdrawal

# Ver lag dos consumers
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group withdrawal-processor
```

#### Verificar Logs

```bash
# Via Docker
docker-compose logs -f --tail=100 zendapag-api
docker-compose logs -f --tail=100 zendapag-worker

# Via arquivos de log
tail -f /var/log/zendapag/api.log
tail -f /var/log/zendapag/worker.log
```

### 10. Testes Funcionais

#### Criar um Saque

```bash
# Obter token JWT
TOKEN=$(curl -X POST http://localhost:8091/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"merchant@example.com","password":"password"}' \
  | jq -r '.data.token')

# Criar saque
curl -X POST "http://localhost:8091/api/v1/withdrawals?accountId=ACCOUNT_UUID&merchantId=MERCHANT_UUID" \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "pixKey": "123.456.789-00",
    "pixKeyType": "CPF",
    "description": "Teste de saque"
  }'
```

#### Verificar Processamento

```bash
# Listar saques
curl "http://localhost:8091/api/v1/withdrawals/account/ACCOUNT_UUID" \
  -H "Authorization: Bearer $TOKEN"

# Verificar mensagens no Kafka
kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic withdrawal-events-dev \
  --from-beginning \
  --max-messages 10
```

## 🔧 Troubleshooting

### Problema: Tabela pix_withdrawals não existe

**Solução:**
```bash
# Verificar versão do Flyway
./mvnw flyway:info

# Executar migration manualmente
./mvnw flyway:migrate
```

### Problema: Kafka topics não existem

**Solução:**
```bash
# Recriar topics
cd /c/Projetos/zendapag/kafka
./create-withdrawal-topics.sh dev
```

### Problema: Erro ao conectar no Redis

**Solução:**
```bash
# Verificar se Redis está rodando
redis-cli ping

# Reiniciar Redis
docker-compose restart redis
```

### Problema: Worker não consome mensagens

**Solução:**
```bash
# Verificar consumer group
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --describe --group withdrawal-processor

# Resetar offset
kafka-consumer-groups.sh --bootstrap-server localhost:9092 --group withdrawal-processor --reset-offsets --to-earliest --execute --topic withdrawal-events-dev
```

## 📊 Monitoramento

### Métricas Prometheus

```bash
# Métricas da API
curl http://localhost:8091/actuator/prometheus | grep withdrawal

# Métricas do Worker
curl http://localhost:8092/actuator/prometheus | grep withdrawal
```

### Dashboards Grafana

Importe os dashboards em `monitoring/grafana/dashboards/`:
- `withdrawal-overview.json`
- `withdrawal-processing.json`

## 🔄 Rollback

### Rollback da Aplicação

```bash
# Via Docker Compose
docker-compose down
docker-compose up -d --force-recreate zendapag-api zendapag-worker

# Via Kubernetes
kubectl rollout undo deployment/zendapag-api -n zendapag
kubectl rollout undo deployment/zendapag-worker -n zendapag
```

### Rollback do Banco de Dados

```bash
# Flyway não suporta rollback automático
# Criar migration de reversa manualmente se necessário
psql -h localhost -U zendapag -d zendapag_db < rollback_v013.sql
```

## 📝 Checklist de Deploy

- [ ] Código atualizado do repositório
- [ ] Migration V013 executada com sucesso
- [ ] Kafka topics criados (withdrawal-events, withdrawal-processing, withdrawal-events-dlq)
- [ ] Variáveis de ambiente configuradas
- [ ] Backend compilado sem erros
- [ ] Frontend compilado e bundle gerado
- [ ] Docker images buildadas
- [ ] Infraestrutura iniciada (PostgreSQL, Redis, Kafka)
- [ ] API iniciada e respondendo no health check
- [ ] Worker iniciado e respondendo no health check
- [ ] Dashboard acessível e carregando
- [ ] Teste funcional de criação de saque realizado
- [ ] Verificação de logs sem erros críticos
- [ ] Métricas sendo coletadas corretamente
- [ ] Alertas configurados no Grafana/Prometheus

## 🎯 Próximos Passos Após Deploy

1. **Monitorar logs** nas primeiras horas para detectar erros
2. **Acompanhar métricas** de processamento de saques
3. **Validar integração PIX** em ambiente de homologação
4. **Realizar testes de carga** para validar performance
5. **Documentar** quaisquer ajustes necessários
6. **Treinar equipe** no uso da nova funcionalidade
7. **Preparar comunicação** para usuários finais

## 📞 Suporte

Em caso de problemas durante o deploy:
- Email: devops@zendapag.com
- Slack: #deploy-support
- Documentação: https://docs.zendapag.com/deploy
- Issues: https://github.com/zendapag/issues
