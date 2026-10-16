.PHONY: help
.DEFAULT_GOAL := help

# Colors for terminal output
CYAN := \033[0;36m
GREEN := \033[0;32m
YELLOW := \033[0;33m
RED := \033[0;31m
NC := \033[0m # No Color

help: ## Show this help message
	@echo "${CYAN}Location Sync Gateway - Makefile Commands${NC}"
	@echo ""
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | awk 'BEGIN {FS = ":.*?## "}; {printf "${GREEN}%-20s${NC} %s\n", $$1, $$2}'

# Development
dev-up: ## Start development environment (Docker Compose)
	@echo "${CYAN}Starting development environment...${NC}"
	docker-compose up -d
	@echo "${GREEN}Environment started. Access points:${NC}"
	@echo "  Application: http://localhost:8080"
	@echo "  Kafka UI: http://localhost:8090"
	@echo "  Redis Commander: http://localhost:8081"
	@echo "  Grafana: http://localhost:3000 (admin/admin)"
	@echo "  Prometheus: http://localhost:9090"
	@echo "  Jaeger: http://localhost:16686"
	@echo "  pgAdmin: http://localhost:5050 (admin@locationsync.com/admin)"
	@echo "  Mailhog: http://localhost:8025"

dev-down: ## Stop development environment
	@echo "${YELLOW}Stopping development environment...${NC}"
	docker-compose down

dev-clean: ## Stop and remove all containers, networks, and volumes
	@echo "${RED}Cleaning development environment (removing all data)...${NC}"
	docker-compose down -v --remove-orphans

dev-logs: ## View logs from all services
	docker-compose logs -f

dev-logs-app: ## View application logs only
	docker-compose logs -f application

dev-restart: ## Restart development environment
	@echo "${YELLOW}Restarting development environment...${NC}"
	docker-compose restart

dev-status: ## Show status of all services
	docker-compose ps

# Build
build: ## Build the application
	@echo "${CYAN}Building application...${NC}"
	./gradlew clean build

build-docker: ## Build Docker image for application
	@echo "${CYAN}Building Docker image...${NC}"
	docker build -t location-sync-gateway:latest .

# Testing
test: ## Run unit tests
	@echo "${CYAN}Running unit tests...${NC}"
	./gradlew test

test-integration: ## Run integration tests
	@echo "${CYAN}Running integration tests...${NC}"
	./gradlew integrationTest

test-all: ## Run all tests
	@echo "${CYAN}Running all tests...${NC}"
	./gradlew clean test integrationTest

test-coverage: ## Generate test coverage report
	@echo "${CYAN}Generating coverage report...${NC}"
	./gradlew test jacocoTestReport
	@echo "${GREEN}Coverage report: build/reports/jacoco/test/html/index.html${NC}"

# Database
db-migrate: ## Run database migrations
	@echo "${CYAN}Running database migrations...${NC}"
	./gradlew flywayMigrate

db-clean: ## Clean database (drop all tables)
	@echo "${RED}Cleaning database...${NC}"
	./gradlew flywayClean

db-info: ## Show migration info
	./gradlew flywayInfo

db-seed: ## Seed database with test data
	@echo "${CYAN}Seeding database...${NC}"
	./gradlew seedDatabase

db-shell: ## Open PostgreSQL shell
	docker exec -it lsg-postgres psql -U lsg -d location_sync_gateway

# Kafka
kafka-topics: ## List Kafka topics
	docker exec lsg-kafka kafka-topics --bootstrap-server localhost:9092 --list

kafka-create-topics: ## Create required Kafka topics
	@echo "${CYAN}Creating Kafka topics...${NC}"
	docker exec lsg-kafka kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --replication-factor 1 --partitions 12 --topic location.snapshots
	docker exec lsg-kafka kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --replication-factor 1 --partitions 12 --topic location.conflicts
	docker exec lsg-kafka kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --replication-factor 1 --partitions 12 --topic location.decisions
	docker exec lsg-kafka kafka-topics --create --if-not-exists --bootstrap-server localhost:9092 --replication-factor 1 --partitions 12 --topic location.dlq
	@echo "${GREEN}Topics created successfully${NC}"

kafka-console-consumer: ## Start Kafka console consumer for snapshots topic
	docker exec -it lsg-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic location.snapshots --from-beginning

# Redis
redis-cli: ## Open Redis CLI
	docker exec -it lsg-redis redis-cli -a redis_password

redis-flush: ## Flush all Redis data
	@echo "${RED}Flushing Redis cache...${NC}"
	docker exec lsg-redis redis-cli -a redis_password FLUSHALL

# Application
run: ## Run the application locally
	@echo "${CYAN}Starting application...${NC}"
	./gradlew run

run-dev: ## Run application in dev mode with hot reload
	./gradlew run --continuous

stop: ## Stop the application
	@echo "${YELLOW}Stopping application...${NC}"
	pkill -f location-sync-gateway || true

# Kubernetes (local)
k8s-up: ## Start local Kubernetes cluster (minikube)
	@echo "${CYAN}Starting Minikube...${NC}"
	minikube start --driver=docker --memory=4096 --cpus=2

k8s-down: ## Stop local Kubernetes cluster
	@echo "${YELLOW}Stopping Minikube...${NC}"
	minikube stop

k8s-deploy: ## Deploy to Kubernetes
	@echo "${CYAN}Deploying to Kubernetes...${NC}"
	kubectl apply -f k8s/

k8s-delete: ## Delete Kubernetes deployment
	@echo "${RED}Deleting Kubernetes deployment...${NC}"
	kubectl delete -f k8s/

k8s-logs: ## View Kubernetes pod logs
	kubectl logs -f -l app=location-sync-gateway

# Monitoring
metrics: ## Open Prometheus UI
	@echo "Opening Prometheus: http://localhost:9090"
	@open http://localhost:9090 2>/dev/null || xdg-open http://localhost:9090 2>/dev/null || start http://localhost:9090

dashboard: ## Open Grafana dashboard
	@echo "Opening Grafana: http://localhost:3000"
	@open http://localhost:3000 2>/dev/null || xdg-open http://localhost:3000 2>/dev/null || start http://localhost:3000

traces: ## Open Jaeger UI
	@echo "Opening Jaeger: http://localhost:16686"
	@open http://localhost:16686 2>/dev/null || xdg-open http://localhost:16686 2>/dev/null || start http://localhost:16686

# Cleanup
clean: ## Clean build artifacts
	@echo "${YELLOW}Cleaning build artifacts...${NC}"
	./gradlew clean

clean-all: dev-clean clean ## Clean everything (build + docker)

# Documentation
docs: ## Generate API documentation
	@echo "${CYAN}Generating documentation...${NC}"
	./gradlew dokkaHtml
	@echo "${GREEN}Documentation: build/dokka/html/index.html${NC}"

# Health checks
health: ## Check health of all services
	@echo "${CYAN}Checking service health...${NC}"
	@echo "PostgreSQL:" && docker exec lsg-postgres pg_isready -U lsg && echo "${GREEN}✓ Healthy${NC}" || echo "${RED}✗ Unhealthy${NC}"
	@echo "Kafka:" && docker exec lsg-kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1 && echo "${GREEN}✓ Healthy${NC}" || echo "${RED}✗ Unhealthy${NC}"
	@echo "Redis:" && docker exec lsg-redis redis-cli -a redis_password ping > /dev/null 2>&1 && echo "${GREEN}✓ Healthy${NC}" || echo "${RED}✗ Unhealthy${NC}"

# Quick start
setup: dev-up kafka-create-topics db-migrate ## Complete setup (start services, create topics, run migrations)
	@echo "${GREEN}Setup complete! Ready for development.${NC}"

# Performance testing
perf-test: ## Run performance tests with K6
	@echo "${CYAN}Running performance tests...${NC}"
	k6 run tests/performance/load-test.js