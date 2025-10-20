# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Zendapag is a modern PIX payment platform built with Spring Boot 3.2+ using a multi-module Maven architecture. It's designed for processing PIX transactions with high scalability and reliability.

## Architecture

- **zendapag-common**: Shared utilities, security configurations, DTOs, and exception handling
- **zendapag-core**: Domain entities, repositories, and business services
- **zendapag-api**: REST controllers and web layer (Port 8091 in containers)
- **zendapag-worker**: Asynchronous processing with Kafka consumers (Port 8092 in containers)
- **zendapag-dashboard**: React frontend dashboard (Port 3005 in development)

## Key Technologies

- Java 17, Spring Boot 3.2.2, Spring Security 6+
- PostgreSQL (prod/staging) / H2 (dev)
- Redis for caching and sessions
- Apache Kafka for async messaging
- JWT authentication
- Micrometer + Prometheus for metrics
- Docker multi-stage builds

## Development Commands

```bash
# Initial setup
make setup

# Build and test
make build              # Compile all modules
make test              # Run tests
make build-jar         # Build JARs
make clean             # Clean artifacts

# Development environment
make quick-start       # Full dev environment setup
make up                # Infrastructure only (postgres, redis, kafka)
make up-dev            # With Kafka UI
make up-full           # Full stack with applications

# Local development
make dev-api           # Run API locally with 'dev' profile
make dev-worker        # Run Worker locally with 'dev' profile

# Docker
make docker-build      # Build container images
make logs              # View all logs
make logs-api          # API logs only
make logs-worker       # Worker logs only
```

## Profiles

- **dev**: H2 database, mock integrations, debug logging
- **staging**: PostgreSQL, Redis, mock PIX, structured logging
- **prod**: Full production setup with real PIX integrations

## Database

- **Dev**: H2 in-memory database (auto-created)
- **Staging/Prod**: PostgreSQL with connection pooling
- JPA entities auto-create tables in dev mode
- Production uses `ddl-auto: validate`

## Security

- JWT tokens with configurable expiration
- BCrypt password encoding
- Spring Security 6+ with method-level security
- CORS configured for API endpoints

## Key Endpoints

- **Frontend Dashboard**: `http://localhost:3005`
- API Health: `http://localhost:8091/actuator/health`
- API Docs: `http://localhost:8091/swagger-ui.html`
- Worker Health: `http://localhost:8092/actuator/health`
- H2 Console (dev): `http://localhost:8091/h2-console`

## Kafka Integration

- Topics: `transaction-events-dev`, `pix-webhook-dev` (dev profile)
- Consumer groups: `zendapag-api-dev`, `zendapag-worker-dev`
- JSON serialization with trusted packages configuration

## Testing

Tests are organized by module. Use:
- `make test` for all tests
- `./mvnw test -pl zendapag-api` for specific module
- TestContainers used for integration tests

## Common Development Tasks

1. **Adding new entities**: Create in `zendapag-core/entity`, add repositories and services
2. **New API endpoints**: Add controllers in `zendapag-api`, DTOs for request/response
3. **Async processing**: Add consumers in `zendapag-worker` for Kafka message handling
4. **Configuration**: Profile-specific configs in `application.yml` files

## Monitoring

- Metrics exposed on `/actuator/prometheus`
- Health checks available
- Structured JSON logging in production
- Grafana dashboards available with `make up-monitoring`

## Deployment

The project includes Docker multi-stage builds and docker-compose configurations for different environments. Use `make deploy-staging` or `make deploy-prod` for deployments.