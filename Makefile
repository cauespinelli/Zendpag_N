# Zendapag PIX Payment Platform Makefile - Enhanced with Kafka Support

.PHONY: help build test clean docker up down logs setup lint format kafka-start kafka-stop monitoring-up

# Default target
help:
	@echo "Zendapag Development Commands:"
	@echo ""
	@echo "Setup & Build:"
	@echo "  make setup        - Initial project setup"
	@echo "  make build        - Build all modules"
	@echo "  make test         - Run all tests"
	@echo "  make build-jar    - Build JAR files"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make lint         - Run code quality checks"
	@echo "  make format       - Format code"
	@echo ""
	@echo "Development:"
	@echo "  make dev-api      - Run API locally (dev profile)"
	@echo "  make dev-worker   - Run Worker locally (dev profile)"
	@echo "  make dev-dashboard - Run React dashboard locally"
	@echo "  make quick-start  - Start complete development environment"
	@echo ""
	@echo "Infrastructure:"
	@echo "  make up           - Start infrastructure (postgres, redis)"
	@echo "  make up-dev       - Start with Kafka UI"
	@echo "  make up-full      - Start full stack with applications"
	@echo "  make down         - Stop all containers"
	@echo ""
	@echo "Kafka:"
	@echo "  make kafka-start  - Start Kafka cluster (3 brokers + zookeeper)"
	@echo "  make kafka-stop   - Stop Kafka cluster"
	@echo "  make kafka-topics - List/Create Kafka topics"
	@echo "  make kafka-reset  - Reset Kafka data (DANGEROUS)"
	@echo ""
	@echo "Monitoring:"
	@echo "  make monitoring-up   - Start monitoring stack"
	@echo "  make monitoring-down - Stop monitoring stack"
	@echo "  make logs         - View all logs"
	@echo "  make logs-kafka   - View Kafka logs"
	@echo ""
	@echo "Security:"
	@echo "  make generate-certs - Generate Kafka SSL certificates"

# Setup
setup:
	@echo "Setting up Zendapag development environment..."
	@mkdir -p logs/api logs/worker
	@chmod +x mvnw

# Build
build:
	@echo "Building all modules..."
	./mvnw clean compile

build-jar:
	@echo "Building JAR files..."
	./mvnw clean package -DskipTests

# Test
test:
	@echo "Running tests..."
	./mvnw test

test-integration:
	@echo "Running integration tests..."
	./mvnw verify -P integration-tests

# Clean
clean:
	@echo "Cleaning build artifacts..."
	./mvnw clean
	@docker system prune -f

# Code Quality
lint:
	@echo "Running code quality checks..."
	./mvnw spotbugs:check checkstyle:check

format:
	@echo "Formatting code..."
	./mvnw spotless:apply

# Docker
docker-build:
	@echo "Building Docker images..."
	docker build -t zendapag-api:latest -f zendapag-api/Dockerfile .
	docker build -t zendapag-worker:latest -f zendapag-worker/Dockerfile .

# Infrastructure only
up:
	@echo "Starting infrastructure services..."
	docker-compose up -d postgres redis
	make kafka-start

# Full stack
up-full: docker-build
	@echo "Starting full stack..."
	docker-compose up -d
	make kafka-start
	make monitoring-up

# Development with Kafka UI
up-dev:
	@echo "Starting development environment..."
	docker-compose up -d postgres redis
	make kafka-start

# Kafka specific commands
kafka-start:
	@echo "Starting Kafka cluster..."
	docker-compose -f docker-compose.kafka.yml up -d zookeeper1 zookeeper2 zookeeper3
	@echo "Waiting for Zookeeper cluster..."
	sleep 20
	docker-compose -f docker-compose.kafka.yml up -d kafka1 kafka2 kafka3
	@echo "Waiting for Kafka brokers..."
	sleep 30
	docker-compose -f docker-compose.kafka.yml up -d schema-registry kafka-connect kafka-ui kafka-topic-manager

kafka-stop:
	@echo "Stopping Kafka cluster..."
	docker-compose -f docker-compose.kafka.yml down

kafka-reset:
	@echo "WARNING: This will delete all Kafka data!"
	@read -p "Are you sure? (y/N) " confirm && [ "$$confirm" = "y" ] || exit 1
	make kafka-stop
	docker volume rm $$(docker volume ls -q | grep kafka) || true
	docker volume rm $$(docker volume ls -q | grep zookeeper) || true
	make kafka-start

# Monitoring
monitoring-up:
	@echo "Starting monitoring stack..."
	docker-compose -f docker-compose.monitoring.yml up -d
	@echo "Monitoring started!"
	@echo "Grafana: http://localhost:3001 (admin/zendapag123)"
	@echo "Prometheus: http://localhost:9090"

monitoring-down:
	@echo "Stopping monitoring stack..."
	docker-compose -f docker-compose.monitoring.yml down

# Stop containers
down:
	@echo "Stopping containers..."
	docker-compose down

# Logs
logs:
	docker-compose logs -f

logs-api:
	docker-compose logs -f zendapag-api

logs-worker:
	docker-compose logs -f zendapag-worker

logs-kafka:
	docker-compose logs -f kafka

# Development
dev-api:
	@echo "Running API in development mode..."
	./mvnw spring-boot:run -pl zendapag-api -Dspring-boot.run.profiles=dev

dev-worker:
	@echo "Running Worker in development mode..."
	./mvnw spring-boot:run -pl zendapag-worker -Dspring-boot.run.profiles=dev

dev-dashboard:
	@echo "Running React dashboard in development mode..."
	cd zendapag-dashboard && npm start

# Database
db-migrate:
	@echo "Running database migrations..."
	./mvnw flyway:migrate -pl zendapag-core

db-reset:
	@echo "Resetting database..."
	docker-compose exec postgres psql -U zendapag -d zendapag -c "DROP SCHEMA public CASCADE; CREATE SCHEMA public;"

# Kafka topics and utilities
kafka-topics:
	@echo "Listing Kafka topics..."
	docker exec zendapag-kafka1 kafka-topics --bootstrap-server localhost:9092 --list

kafka-consumer-groups:
	@echo "Listing consumer groups..."
	docker exec zendapag-kafka1 kafka-consumer-groups --bootstrap-server localhost:9092 --list

kafka-consumer-lag:
	@echo "Checking consumer lag..."
	docker exec zendapag-kafka1 kafka-consumer-groups --bootstrap-server localhost:9092 --describe --all-groups

# Security
generate-certs:
	@echo "Generating Kafka SSL certificates..."
	cd kafka/scripts && ./generate-certificates.sh

# Monitoring
prometheus:
	@echo "Opening Prometheus..."
	@open http://localhost:9090 || xdg-open http://localhost:9090 || echo "Open http://localhost:9090 in your browser"

grafana:
	@echo "Opening Grafana (admin/admin123)..."
	@open http://localhost:3000 || xdg-open http://localhost:3000 || echo "Open http://localhost:3000 in your browser"

kafka-ui:
	@echo "Opening Kafka UI..."
	@open http://localhost:8080 || xdg-open http://localhost:8080 || echo "Open http://localhost:8080 in your browser"

# Health checks
health:
	@echo "Checking API health..."
	@curl -f http://localhost:8091/actuator/health || echo "API not healthy"
	@echo "Checking Worker health..."
	@curl -f http://localhost:8092/actuator/health || echo "Worker not healthy"

# Quick start
quick-start: setup up kafka-topics
	@echo "Zendapag development environment is ready!"
	@echo "Dashboard: http://localhost:3005"
	@echo "API: http://localhost:8091"
	@echo "Worker: http://localhost:8092"
	@echo "Kafka UI: http://localhost:8080"
	@echo "PostgreSQL: localhost:5432 (zendapag/zendapag123)"
	@echo "Redis: localhost:6379"

# Deploy
deploy-staging:
	@echo "Deploying to staging..."
	# Add your staging deployment commands here

deploy-prod:
	@echo "Deploying to production..."
	# Add your production deployment commands here

# Utilities
tail-api-logs:
	tail -f logs/api/application.log

tail-worker-logs:
	tail -f logs/worker/application.log

backup-db:
	@echo "Creating database backup..."
	docker-compose exec postgres pg_dump -U zendapag zendapag > backup_$(shell date +%Y%m%d_%H%M%S).sql

restore-db:
	@echo "Restoring database from backup (specify BACKUP_FILE)..."
	docker-compose exec -T postgres psql -U zendapag -d zendapag < $(BACKUP_FILE)