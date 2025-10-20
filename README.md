# Zendapag - PIX Payment Platform

Uma plataforma moderna de pagamentos PIX desenvolvida em Spring Boot 3.2+ com arquitetura multi-módulos.

## 🏗️ Arquitetura

```
zendapag/
├── zendapag-common/    # Utilitários, configurações e security
├── zendapag-core/      # Entidades, repositórios e serviços de domínio
├── zendapag-api/       # Controllers REST e configurações web
├── zendapag-worker/    # Processamento assíncrono com Kafka
├── docker/             # Configurações Docker
├── k8s/               # Manifestos Kubernetes
├── scripts/           # Scripts de setup e utilitários
└── docs/              # Documentação adicional
```

## 🚀 Tecnologias

- **Java 17** + **Spring Boot 3.2.2**
- **Spring Security 6+** com autenticação JWT
- **Spring Data JPA** + **PostgreSQL**
- **Redis** para cache e sessões
- **Apache Kafka** para messaging
- **Micrometer** + **Prometheus** para métricas
- **Docker** + **Docker Compose**
- **Maven** multi-módulo

## 🔧 Pré-requisitos

- Java 17+
- Docker e Docker Compose
- Make (opcional, mas recomendado)

## 🎯 Quick Start

```bash
# Clonar e setup inicial
git clone <repository-url>
cd zendapag
make setup

# Iniciar ambiente de desenvolvimento
make quick-start

# Ou manualmente:
docker-compose --profile dev up -d
make kafka-topics
```

## 🔨 Comandos Principais

### Build e Testes
```bash
make build          # Compilar todos os módulos
make test           # Executar testes
make build-jar      # Gerar JARs
make clean          # Limpar artefatos
```

### Docker
```bash
make up             # Infra (postgres, redis, kafka)
make up-full        # Stack completo com apps
make up-dev         # Com Kafka UI para desenvolvimento
make up-monitoring  # Com Prometheus + Grafana
make down           # Parar containers
make logs           # Ver logs
```

### Desenvolvimento
```bash
make dev-api        # Executar API localmente
make dev-worker     # Executar Worker localmente
make kafka-topics   # Criar tópicos Kafka
```

## 🌍 Profiles

### Development (`dev`)
- H2 in-memory database
- Mock integrations
- Debug logging
- Kafka UI habilitado

### Staging (`staging`)
- PostgreSQL database
- Redis cache
- Mock PIX integrations
- Structured logging

### Production (`prod`)
- PostgreSQL com pool otimizado
- Redis com configurações de produção
- Integrações PIX reais
- Logging estruturado JSON
- Métricas completas

## 🔗 Endpoints

### API (Port 8081)
- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`
- **API Docs**: `GET /swagger-ui.html`
- **Authentication**: `POST /api/v1/auth/login`

### Worker (Port 8082)
- **Health Check**: `GET /actuator/health`
- **Metrics**: `GET /actuator/metrics`

### Monitoring
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin123)
- **Kafka UI**: http://localhost:8080

## 🗄️ Database

### Desenvolvimento
```bash
# H2 Console (dev profile)
http://localhost:8081/h2-console
```

### PostgreSQL
```bash
# Conexão
Host: localhost
Port: 5432
Database: zendapag
User: zendapag
Password: zendapag123
```

## 🔄 Kafka Topics

- `transaction-events-dev` - Eventos de transação
- `pix-webhook-dev` - Webhooks PIX

## 📊 Monitoramento

### Métricas Disponíveis
- JVM metrics
- Spring Boot metrics
- Business metrics customizadas
- Database connection pool metrics
- Kafka consumer/producer metrics

### Health Checks
- Database connectivity
- Redis connectivity
- Kafka connectivity

## 🧪 Testes

```bash
# Todos os testes
make test

# Testes de integração
make test-integration

# Testes específicos de um módulo
./mvnw test -pl zendapag-api
```

## 🚀 Deploy

### Staging
```bash
make deploy-staging
```

### Production
```bash
make deploy-prod
```

## 🔒 Segurança

- Autenticação JWT com refresh tokens
- Senhas criptografadas com BCrypt
- CORS configurado adequadamente
- Rate limiting implementado
- Input validation em todas as APIs

## 📝 Logs

### Desenvolvimento
```bash
make tail-api-logs
make tail-worker-logs
```

### Formato de Log
- **Dev**: Plain text para facilitar leitura
- **Prod**: JSON estruturado para agregação

## 🤝 Contribuindo

1. Fork o projeto
2. Crie uma branch: `git checkout -b feature/nova-funcionalidade`
3. Commit: `git commit -m 'Adicionar nova funcionalidade'`
4. Push: `git push origin feature/nova-funcionalidade`
5. Abra um Pull Request

## 📚 Documentação Adicional

- [API Documentation](docs/api.md)
- [Architecture Guide](docs/architecture.md)
- [Deployment Guide](docs/deployment.md)
- [Development Guide](docs/development.md)

## 📄 Licença

Este projeto está sob a licença MIT. Veja o arquivo [LICENSE](LICENSE) para detalhes.

## 🆘 Suporte

Para questões e suporte:
- Abra uma [issue](../../issues)
- Consulte a [documentação](docs/)
- Entre em contato com a equipe de desenvolvimento