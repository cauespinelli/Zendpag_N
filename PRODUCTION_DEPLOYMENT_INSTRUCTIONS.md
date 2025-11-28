# 📋 Instruções de Deploy em Produção - Zendapag

## 🎯 Status do Deploy

**Data**: 29 de Outubro de 2025
**Servidor**: 159.89.80.179 (Digital Ocean)
**Diretório**: /opt/zendapag
**Status**: ✅ Em Execução

---

## 📦 O que foi deployado

### 1. ✅ Banco de Dados (Completo)
- Tabela `pix_withdrawals` criada com 48 colunas
- 14 índices otimizados
- 2 triggers automáticos
- Backup criado em: `/opt/backups/zendapag/zendapag_backup_20251029_030040.sql`

### 2. 🔄 Aplicações (Em Progresso)
- **zendapag-api**: Porta 8091
- **zendapag-worker**: Porta 8092
- **zendapag-dashboard**: Porta 3005

### 3. 🔄 Infraestrutura (Em Progresso)
- **Kafka**: Porta 9092
- **Zookeeper**: Porta 2181
- **Redis**: Porta 6381

---

## 🚀 Como verificar o deploy

### Conectar ao servidor

```bash
ssh root@159.89.80.179
cd /opt/zendapag
```

### Verificar status dos containers

```bash
docker-compose -f docker-compose.prod.yml ps
```

### Ver logs em tempo real

```bash
# API
docker-compose -f docker-compose.prod.yml logs -f zendapag-api

# Worker
docker-compose -f docker-compose.prod.yml logs -f zendapag-worker

# Dashboard
docker-compose -f docker-compose.prod.yml logs -f zendapag-dashboard

# Kafka
docker-compose -f docker-compose.prod.yml logs -f kafka

# Todos
docker-compose -f docker-compose.prod.yml logs -f
```

### Verificar health checks

```bash
# API Health
curl http://localhost:8091/actuator/health

# Worker Health
curl http://localhost:8092/actuator/health

# Dashboard Health
curl http://localhost:3005/health

# Kafka Topics
docker exec zendapag-kafka kafka-topics --bootstrap-server localhost:9092 --list
```

---

## 🔧 Comandos úteis

### Reiniciar um serviço

```bash
# Reiniciar API
docker-compose -f docker-compose.prod.yml restart zendapag-api

# Reiniciar Worker
docker-compose -f docker-compose.prod.yml restart zendapag-worker

# Reiniciar Dashboard
docker-compose -f docker-compose.prod.yml restart zendapag-dashboard
```

### Parar todos os serviços

```bash
docker-compose -f docker-compose.prod.yml down
```

### Iniciar todos os serviços

```bash
docker-compose -f docker-compose.prod.yml up -d
```

### Reconstruir uma imagem

```bash
# Rebuild API
docker-compose -f docker-compose.prod.yml build zendapag-api
docker-compose -f docker-compose.prod.yml up -d zendapag-api
```

### Ver uso de recursos

```bash
# CPU e Memória dos containers
docker stats

# Espaço em disco
df -h
```

---

## 🌐 URLs de Acesso

### APIs

- **API REST**: http://159.89.80.179:8091
- **API Health**: http://159.89.80.179:8091/actuator/health
- **Swagger UI**: http://159.89.80.179:8091/swagger-ui.html
- **Actuator**: http://159.89.80.179:8091/actuator

### Worker

- **Worker Health**: http://159.89.80.179:8092/actuator/health
- **Worker Actuator**: http://159.89.80.179:8092/actuator

### Dashboard

- **Dashboard**: http://159.89.80.179:3005
- **Página de Saques**: http://159.89.80.179:3005/withdrawals

---

## 🧪 Testando a API

### 1. Criar um saque

```bash
curl -X POST "http://159.89.80.179:8091/api/v1/withdrawals?accountId=UUID&merchantId=UUID" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "amount": 100.00,
    "pixKey": "123.456.789-00",
    "pixKeyType": "CPF",
    "description": "Teste de saque em produção"
  }'
```

### 2. Listar saques de uma conta

```bash
curl "http://159.89.80.179:8091/api/v1/withdrawals/account/UUID" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 3. Buscar saque por ID

```bash
curl "http://159.89.80.179:8091/api/v1/withdrawals/WITHDRAWAL_ID" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

### 4. Cancelar um saque

```bash
curl -X POST "http://159.89.80.179:8091/api/v1/withdrawals/WITHDRAWAL_ID/cancel" \
  -H "Authorization: Bearer YOUR_TOKEN"
```

---

## 📊 Monitoramento

### Verificar Kafka

```bash
# Listar topics
docker exec zendapag-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Descrever um topic
docker exec zendapag-kafka kafka-topics --bootstrap-server localhost:9092 --describe --topic withdrawal-events-prod

# Ver mensagens (últimas 10)
docker exec zendapag-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic withdrawal-events-prod \
  --from-beginning \
  --max-messages 10
```

### Verificar Redis

```bash
# Conectar ao Redis
docker exec -it zendapag-redis redis-cli -a zendapag123

# Dentro do Redis:
# INFO
# KEYS *
# GET key_name
```

### Verificar PostgreSQL

```bash
# Conectar ao banco
docker exec -it voalive-postgres-1 psql -U reservasegura_user -d zendapag

# Dentro do PostgreSQL:
# \dt pix_withdrawals
# SELECT COUNT(*) FROM pix_withdrawals;
# SELECT * FROM pix_withdrawals LIMIT 10;
```

---

## ⚠️ Troubleshooting

### Problema: Container não inicia

```bash
# Ver logs do container
docker-compose -f docker-compose.prod.yml logs [service-name]

# Verificar se há containers conflitantes
docker ps -a

# Remover container problemático e recriar
docker-compose -f docker-compose.prod.yml rm -f [service-name]
docker-compose -f docker-compose.prod.yml up -d [service-name]
```

### Problema: Kafka não conecta

```bash
# Verificar se Kafka está rodando
docker-compose -f docker-compose.prod.yml ps kafka

# Reiniciar Kafka e Zookeeper
docker-compose -f docker-compose.prod.yml restart zookeeper kafka

# Recriar topics
bash scripts/create-kafka-topics-prod.sh
```

### Problema: API retorna erro de conexão com banco

```bash
# Verificar se PostgreSQL está acessível
docker exec zendapag-api ping postgres -c 3

# Verificar rede Docker
docker network ls
docker network inspect voalive_default

# Verificar variáveis de ambiente
docker exec zendapag-api env | grep SPRING_DATASOURCE
```

### Problema: Falta de memória

```bash
# Ver uso de memória
free -h

# Limpar containers parados
docker container prune -f

# Limpar images não utilizadas
docker image prune -a -f

# Limpar volumes não utilizados
docker volume prune -f
```

---

## 🔄 Rollback

### Rollback da Aplicação

```bash
# Parar containers
docker-compose -f docker-compose.prod.yml down

# Remover images
docker rmi zendapag-api:latest zendapag-worker:latest zendapag-dashboard:latest

# Fazer checkout de versão anterior do código
# git checkout [previous-commit]

# Rebuild
bash deploy/build-and-deploy.sh
```

### Rollback do Banco de Dados

```bash
# Conectar ao PostgreSQL
docker exec -it voalive-postgres-1 psql -U reservasegura_user -d zendapag

# Restaurar backup
# \i /opt/backups/zendapag/zendapag_backup_20251029_030040.sql

# Ou remover tabela completamente
# DROP TABLE IF EXISTS pix_withdrawals CASCADE;
```

---

## 📈 Próximos Passos

### 1. Configurar Nginx Reverse Proxy

Adicionar configuração para expor as APIs via subdomínios:

- api.zendapag.com → 159.89.80.179:8091
- dashboard.zendapag.com → 159.89.80.179:3005

### 2. Configurar SSL/TLS

```bash
# Instalar Certbot
apt-get update
apt-get install -y certbot python3-certbot-nginx

# Obter certificado
certbot --nginx -d api.zendapag.com -d dashboard.zendapag.com
```

### 3. Configurar Monitoramento

- Prometheus para métricas
- Grafana para dashboards
- Alertas via email/Slack

### 4. Configurar Backups Automáticos

```bash
# Adicionar ao crontab
0 3 * * * docker exec voalive-postgres-1 pg_dump -U reservasegura_user zendapag > /opt/backups/zendapag/backup_$(date +\%Y\%m\%d).sql
```

### 5. Adicionar Foreign Keys

Quando as tabelas `accounts` e `merchants` existirem:

```sql
ALTER TABLE pix_withdrawals
  ADD CONSTRAINT fk_withdrawal_account
  FOREIGN KEY (account_id) REFERENCES accounts(id) ON DELETE RESTRICT;

ALTER TABLE pix_withdrawals
  ADD CONSTRAINT fk_withdrawal_merchant
  FOREIGN KEY (merchant_id) REFERENCES merchants(id) ON DELETE RESTRICT;
```

---

## 📞 Suporte

- **Email**: dev@zendapag.com
- **Documentação**: /opt/zendapag/PIX_WITHDRAWAL_MODULE.md
- **Logs**: /opt/zendapag/logs/

---

## ✅ Checklist de Deploy

- [x] Banco de dados criado
- [x] Migration V013 executada
- [x] Backup criado
- [x] Código transferido para servidor
- [ ] Docker images buildadas
- [ ] Kafka topics criados
- [ ] Aplicações iniciadas
- [ ] Health checks passando
- [ ] Teste de criação de saque realizado
- [ ] Documentação atualizada
- [ ] Nginx configurado (próximo passo)
- [ ] SSL configurado (próximo passo)
- [ ] Monitoramento configurado (próximo passo)

---

**Última atualização**: 29 de Outubro de 2025, 03:40 UTC

*Gerado por Claude Code - Anthropic*
