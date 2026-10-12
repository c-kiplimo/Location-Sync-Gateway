# Setup Guide - Location Sync Gateway

## Quick Start (Recommended for Development)

### Option 1: One-Command Startup (No Docker)

**Linux/Mac:**
```bash
./run-dev.sh
```

**Windows:**
```bash
run-dev.bat
```

This script will:
1. Build the project
2. Start the application with H2 in-memory database
3. Open endpoints at:
   - Application: http://localhost:8080
   - Swagger UI: http://localhost:8080/swagger-ui
   - Health Check: http://localhost:8080/health

**No Docker, PostgreSQL, Kafka, or Redis required!**

---

## Option 2: Manual Development Setup

### Prerequisites
- JDK 17 or higher
- Git

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/c-kiplimo/Location-Sync-Gateway.git
   cd Location-Sync-Gateway
   ```

2. **Build the project**
   ```bash
   ./gradlew clean build -x test
   ```

3. **Run with development profile**

   **Linux/Mac:**
   ```bash
   MICRONAUT_ENVIRONMENTS=dev ./gradlew run
   ```

   **Windows CMD:**
   ```bash
   set MICRONAUT_ENVIRONMENTS=dev && gradlew run
   ```

   **Windows PowerShell:**
   ```powershell
   $env:MICRONAUT_ENVIRONMENTS="dev"; .\gradlew run
   ```

4. **Verify application is running**
   ```bash
   curl http://localhost:8080/health
   ```

---

## Option 3: Production Setup (with Docker)

### Prerequisites
- Docker Desktop installed and running
- Docker Compose

### Infrastructure Services

The application requires:
- **PostgreSQL** - Main database
- **Kafka** - Event streaming
- **Redis** - Caching
- **Prometheus** - Metrics
- **Grafana** - Visualization
- **Jaeger** - Distributed tracing

### Start Infrastructure

1. **Start all services**
   ```bash
   docker-compose up -d
   ```

2. **Verify services are running**
   ```bash
   docker-compose ps
   ```

3. **Check service health**
   ```bash
   # PostgreSQL
   docker-compose exec postgres pg_isready -U lsg

   # Kafka
   docker-compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092

   # Redis
   docker-compose exec redis redis-cli --raw incr ping
   ```

4. **Run the application**
   ```bash
   ./gradlew run
   ```

### Access Services

| Service | URL | Credentials |
|---------|-----|-------------|
| Application | http://localhost:8080 | - |
| Swagger UI | http://localhost:8080/swagger-ui | - |
| Kafka UI | http://localhost:8090 | - |
| Prometheus | http://localhost:9090 | - |
| Grafana | http://localhost:3000 | admin / admin |
| Jaeger | http://localhost:16686 | - |
| pgAdmin | http://localhost:5050 | admin@locationsync.com / admin |
| Redis Commander | http://localhost:8081 | - |
| Mailhog | http://localhost:8025 | - |

### Stop Infrastructure

```bash
docker-compose down

# To remove volumes (database data)
docker-compose down -v
```

---

## Testing the Application

### 1. Health Check

```bash
curl http://localhost:8080/health
```

Expected response:
```json
{
  "status": "UP"
}
```

### 2. Generate Simulated Data

The application includes a data simulation API for testing and ML training:

```bash
# Generate 100 location snapshots with varying quality
curl -X POST "http://localhost:8080/api/v1/simulation/generate?count=100&source=GOOGLE_MY_BUSINESS"

# Generate conflict scenarios (baseline + conflicting data)
curl -X POST "http://localhost:8080/api/v1/simulation/generate-conflicts?count=50"

# Generate temporary closure snapshots
curl -X POST "http://localhost:8080/api/v1/simulation/generate-closures?count=20"

# Stress test with high volume (1000 snapshots)
curl -X POST "http://localhost:8080/api/v1/simulation/stress-test?count=1000"
```

### 3. View Swagger UI

Open http://localhost:8080/swagger-ui in your browser to:
- Explore all API endpoints
- Test endpoints interactively
- View request/response schemas

---

## Troubleshooting

### Issue: Application fails to start

**Error:** `Failed to initialize pool: FATAL: password authentication failed`

**Solution:** Make sure Docker services are running:
```bash
docker-compose up -d postgres redis kafka
```

Or use development profile to run without Docker:
```bash
MICRONAUT_ENVIRONMENTS=dev ./gradlew run
```

---

### Issue: Port 8080 already in use

**Solution:** Stop the conflicting process or change the port:

**Linux/Mac:**
```bash
lsof -i :8080
kill -9 <PID>
```

**Windows:**
```powershell
netstat -ano | findstr :8080
taskkill /PID <PID> /F
```

Or run on different port:
```bash
MICRONAUT_SERVER_PORT=8081 ./gradlew run
```

---

### Issue: Docker services won't start

**Solution:** Ensure Docker Desktop is running and has sufficient resources

**Check Docker status:**
```bash
docker info
```

**Restart Docker Desktop:**
- Windows/Mac: Right-click Docker Desktop icon → Restart
- Linux: `sudo systemctl restart docker`

**Check resource allocation:**
- Open Docker Desktop → Settings → Resources
- Recommended: 4+ CPUs, 8+ GB RAM

---

### Issue: Build fails with "OutOfMemoryError"

**Solution:** Increase Gradle heap size

Create or edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
```

---

## Development Workflow

### 1. Make Code Changes

Edit source files in `src/main/kotlin/com/locationsync/gateway/`

### 2. Run Tests

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "com.locationsync.gateway.service.ConflictDetectionServiceTest"

# Run tests continuously (watch mode)
./gradlew test --continuous
```

### 3. Check Code Quality

```bash
# Kotlin linting
./gradlew ktlintCheck

# Auto-fix formatting issues
./gradlew ktlintFormat
```

### 4. Build JAR

```bash
./gradlew build

# JAR location
ls build/libs/
```

### 5. Run JAR

```bash
java -jar build/libs/location-sync-gateway-0.1-all.jar
```

---

## Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `MICRONAUT_ENVIRONMENTS` | Active profile | - |
| `DB_HOST` | PostgreSQL host | localhost |
| `DB_PORT` | PostgreSQL port | 5432 |
| `DB_NAME` | Database name | location_sync_gateway |
| `DB_USER` | Database user | lsg |
| `DB_PASSWORD` | Database password | lsg_password |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | localhost:9092 |
| `REDIS_HOST` | Redis host | localhost |
| `REDIS_PORT` | Redis port | 6379 |
| `REDIS_PASSWORD` | Redis password | redis_password |
| `GOOGLE_OAUTH_CLIENT_ID` | Google OAuth client ID | - |
| `GOOGLE_OAUTH_CLIENT_SECRET` | Google OAuth secret | - |
| `UBERALL_PRIVATE_KEY` | Uberall API key | - |
| `SLACK_WEBHOOK_URL` | Slack webhook for notifications | - |
| `OPENAI_API_KEY` | OpenAI API key (for LLM features) | - |
| `LOG_LEVEL` | Logging level | INFO |

---

## Next Steps

1. **Explore the API** - Visit http://localhost:8080/swagger-ui
2. **Generate test data** - Use simulation endpoints
3. **View conflict detection** - Check how the engine works
4. **Read architecture docs** - See `docs/` folder
5. **Check AI integration** - Review quality scoring and anomaly detection

---

## Support

- Report bugs: https://github.com/c-kiplimo/Location-Sync-Gateway/issues
- Feature requests: https://github.com/c-kiplimo/Location-Sync-Gateway/issues
- Documentation: See `README.md` and `docs/` folder

---

**Happy Coding! 🚀**
