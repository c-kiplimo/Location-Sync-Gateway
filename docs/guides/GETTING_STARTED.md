# Getting Started Guide
## Location Sync Gateway

This guide will help you set up and run the Location Sync Gateway locally for development and testing.

---

## Prerequisites

### Required Software

- **JDK 17+** (OpenJDK or Oracle JDK)
- **Docker Desktop** (for running infrastructure services)
- **Git** (for cloning the repository)
- **Make** (optional, for convenience commands)

### Verify Installation

```bash
# Check Java version
java -version  # Should show 17 or higher

# Check Docker
docker --version
docker-compose --version

# Check Git
git --version
```

---

## Quick Start (5 Minutes)

### 1. Clone the Repository

```bash
git clone https://github.com/c-kiplimo/Location-Sync-Gateway.git
cd Location-Sync-Gateway
```

### 2. Start Infrastructure Services

```bash
# Using Make (recommended)
make setup

# OR manually with Docker Compose
docker-compose up -d
./scripts/wait-for-services.sh  # Wait for services to be ready
docker exec lsg-kafka kafka-topics --create --if-not-exists \
  --bootstrap-server localhost:9092 \
  --replication-factor 1 --partitions 12 --topic location.snapshots
# ... create other topics
```

### 3. Run Database Migrations

```bash
# Using Make
make db-migrate

# OR manually
./gradlew flywayMigrate
```

### 4. Start the Application

```bash
# Using Make
make run

# OR manually
./gradlew run
```

### 5. Verify Installation

```bash
# Check application health
curl http://localhost:8080/health

# Expected response:
# {"status":"UP"}
```

**🎉 You're all set!** The application is now running at `http://localhost:8080`

---

## Access Points

Once the infrastructure is running, you can access:

| Service | URL | Credentials |
|---------|-----|-------------|
| **Application** | http://localhost:8080 | - |
| **Kafka UI** | http://localhost:8090 | - |
| **Redis Commander** | http://localhost:8081 | - |
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | - |
| **Jaeger (Tracing)** | http://localhost:16686 | - |
| **pgAdmin** | http://localhost:5050 | admin@locationsync.com / admin |
| **Mailhog (Email)** | http://localhost:8025 | - |

---

## Testing the Application

### 1. Trigger Manual Sync

The application comes with 4 pre-seeded test locations (1001-1004).

```bash
# Trigger sync for all test locations
curl -X POST "http://localhost:8080/api/v1/sync/trigger?locationIds=1001,1002,1003,1004"

# Expected output:
{
  "meta": {
    "status": "success",
    "respondedAt": "2026-09-22T10:30:00Z"
  },
  "payload": {
    "locationsProcessed": 4,
    "conflictsDetected": 4,
    "autoAccepted": 1,
    "autoReverted": 2,
    "flaggedForReview": 1
  }
}
```

### 2. View Detected Conflicts

```bash
# Get all conflicts
curl http://localhost:8080/api/v1/conflicts

# Get conflicts for specific location
curl http://localhost:8080/api/v1/conflicts/location/1001
```

### 3. View Resolution Decisions

```bash
# Get all decisions
curl http://localhost:8080/api/v1/decisions

# Get decision for specific conflict
curl http://localhost:8080/api/v1/decisions/conflict/{conflictId}
```

### 4. View Test Scenarios

| Location ID | Conflict Type | Expected Resolution | Reason |
|-------------|---------------|---------------------|--------|
| **1001** | PHONE | AUTO_REVERT | HQ controls phone numbers |
| **1002** | OPENING_HOURS | AUTO_ACCEPT | Franchisees control hours |
| **1003** | CATEGORIES | AUTO_REVERT | Brand categories are fixed |
| **1004** | TEMPORARY_CLOSURE | FLAG_FOR_REVIEW | POS signal needs confirmation |

---

## Development Workflow

### Running Tests

```bash
# Run unit tests
make test
# OR: ./gradlew test

# Run integration tests (requires Docker)
make test-integration
# OR: ./gradlew integrationTest

# Run all tests
make test-all
# OR: ./gradlew clean test integrationTest

# Generate coverage report
make test-coverage
# OR: ./gradlew test jacocoTestReport
# Report: build/reports/jacoco/test/html/index.html
```

### Database Management

```bash
# View migration status
make db-info

# Run migrations
make db-migrate

# Open PostgreSQL shell
make db-shell

# Clean database (WARNING: Deletes all data)
make db-clean
```

### Kafka Management

```bash
# List topics
make kafka-topics

# Create required topics
make kafka-create-topics

# Start console consumer
make kafka-console-consumer
```

### Monitoring & Debugging

```bash
# View all logs
make dev-logs

# View application logs only
docker-compose logs -f application  # (when running in Docker)

# Check service health
make health

# Open Grafana dashboard
make dashboard

# Open Prometheus metrics
make metrics

# Open Jaeger tracing
make traces
```

### Hot Reload Development

```bash
# Run with continuous build (hot reload)
make run-dev
# OR: ./gradlew run --continuous
```

---

## Configuration

### Environment Variables

Create a `.env` file in the project root:

```env
# Database
DB_HOST=localhost
DB_PORT=5432
DB_NAME=location_sync_gateway
DB_USER=lsg
DB_PASSWORD=lsg_password

# Kafka
KAFKA_BOOTSTRAP_SERVERS=localhost:9092

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379
REDIS_PASSWORD=redis_password

# External APIs (optional for testing)
GOOGLE_OAUTH_CLIENT_ID=your-client-id
GOOGLE_OAUTH_CLIENT_SECRET=your-client-secret
UBERALL_PRIVATE_KEY=your-private-key

# AI/ML (optional)
OPENAI_API_KEY=your-openai-key
ANTHROPIC_API_KEY=your-anthropic-key

# Scheduler
SCHEDULER_ENABLED=true
INGESTION_DELAY=5m

# Logging
LOG_LEVEL=INFO
```

### Application Configuration

Edit `src/main/resources/application.yml` for more advanced configuration.

### Resolution Rules

Edit `src/main/resources/resolution-rules.yml` to customize conflict resolution logic.

---

## Project Structure

```
location-sync-gateway/
├── src/
│   ├── main/
│   │   ├── kotlin/com/locationsync/gateway/
│   │   │   ├── connector/          # External API connectors
│   │   │   ├── controller/         # REST API controllers
│   │   │   ├── model/              # Domain models
│   │   │   ├── service/            # Business logic
│   │   │   │   └── ai/             # AI/ML services
│   │   │   ├── repository/         # Data persistence
│   │   │   ├── kafka/              # Kafka producers/consumers
│   │   │   └── config/             # Configuration classes
│   │   └── resources/
│   │       ├── application.yml     # Main configuration
│   │       ├── resolution-rules.yml # Conflict resolution rules
│   │       └── db/migration/       # Flyway migrations
│   └── test/
│       └── kotlin/                 # Test files
├── docs/                           # Documentation
│   ├── SOLUTION_ARCHITECTURE.md    # Architecture document
│   └── TEST_GUIDE.md               # Testing guide
├── k8s/                            # Kubernetes manifests
├── monitoring/                     # Prometheus/Grafana configs
├── scripts/                        # Utility scripts
├── docker-compose.yml              # Development environment
├── Dockerfile                      # Application container
├── Makefile                        # Convenience commands
└── README.md                       # Project overview
```

---

## Common Tasks

### Add a New Resolution Rule

1. Edit `src/main/resources/resolution-rules.yml`
2. Add your rule with appropriate priority
3. Restart the application
4. Test with matching conflict data

Example:
```yaml
- name: custom-rule
  fieldType: PHONE
  source: GOOGLE_MY_BUSINESS
  defaultOutcome: AUTO_ACCEPT
  minimumConfidence: 0.9
  priority: 100
  enabled: true
  reason: "Custom reason"
```

### Simulate Conflict Data

```bash
# Insert test conflict via SQL
docker exec -it lsg-postgres psql -U lsg -d location_sync_gateway -c "
INSERT INTO conflicts (location_id, field_type, source, external_value, uberall_value, confidence, detected_at)
VALUES (2001, 'PHONE', 'GOOGLE_MY_BUSINESS', '\"+254700000000\"', '\"+254711111111\"', 0.95, NOW());
"
```

### View Metrics

```bash
# Prometheus metrics endpoint
curl http://localhost:8080/prometheus

# Application health
curl http://localhost:8080/health
```

---

## Troubleshooting

### Services Not Starting

```bash
# Check Docker resources
docker stats

# Restart services
make dev-restart

# View logs for specific service
docker-compose logs postgres
docker-compose logs kafka
```

### Port Conflicts

```bash
# Kill process using port 8080
# Linux/Mac:
lsof -ti:8080 | xargs kill -9

# Windows:
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

### Database Connection Issues

```bash
# Check PostgreSQL is running
docker exec lsg-postgres pg_isready -U lsg

# Verify connection
docker exec -it lsg-postgres psql -U lsg -d location_sync_gateway -c "\dt"
```

### Kafka Issues

```bash
# Check Kafka broker is healthy
docker exec lsg-kafka kafka-broker-api-versions --bootstrap-server localhost:9092

# Recreate topics
docker exec lsg-kafka kafka-topics --delete --bootstrap-server localhost:9092 --topic location.snapshots
make kafka-create-topics
```

### Clean Start

```bash
# Stop and remove all data
make clean-all

# Start fresh
make setup
```

---

## Next Steps

- **Read the [Architecture Document](docs/SOLUTION_ARCHITECTURE.md)** to understand system design
- **Read the [Test Guide](docs/TEST_GUIDE.md)** for testing best practices
- **Explore the API** using Postman or cURL
- **Monitor metrics** in Grafana dashboards
- **Add custom connectors** for your data sources
- **Configure resolution rules** for your use case

---

## Resources

- **Documentation**: `/docs` folder
- **API Reference**: http://localhost:8080/swagger-ui (coming soon)
- **Grafana Dashboards**: http://localhost:3000
- **Issue Tracker**: https://github.com/c-kiplimo/Location-Sync-Gateway/issues

---

## Need Help?

- Check the [README](README.md) for project overview
- Review [Architecture Document](docs/SOLUTION_ARCHITECTURE.md)
- Open an issue on GitHub
- Contact the maintainers

---

**Happy coding!** 🚀