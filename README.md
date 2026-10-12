# Location Sync Gateway

<div align="center">

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9-blue.svg)](https://kotlinlang.org/)
[![Micronaut](https://img.shields.io/badge/Micronaut-4.2-brightgreen.svg)](https://micronaut.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-15-336791.svg)](https://www.postgresql.org/)
[![Kafka](https://img.shields.io/badge/Kafka-3.6-black.svg)](https://kafka.apache.org/)

**Data conflict resolution engine for multi-location brands**

[Quick Start](#quick-start) • [Architecture](#architecture) • [API](#api) • [Use Cases](#use-cases)

</div>

---

## Overview

Detects and resolves location data conflicts between external sources (Google My Business, POS systems, ERP) and Uberall's platform. When franchisees edit Google Business Profile directly, POS systems signal closures, or ERP data conflicts with live listings — this gateway catches it before customers are affected.

**Core Capabilities**
- Multi-source data ingestion via pluggable connectors (Google, POS, ERP, Franchise APIs)
- Field-level conflict detection comparing external sources against Uberall as system of record
- **⭐ AI-powered data quality validation** - Scores every external snapshot (0-100), detects anomalies, prevents bad data
- **⭐ Intelligent conflict resolution** - ML model learns from historical patterns, predicts best outcome
- **⭐ Natural language explanations** - LLM-generated summaries explain decisions in plain language
- Configurable priority-based resolution engine (YAML rules, version-controlled)
- Automated execution with Uberall API integration for corrections
- Event-driven architecture with Kafka for async processing
- Human-in-the-loop workflow with Slack/Email notifications for ambiguous cases
- Full audit trail for every decision (compliance-ready)
- OAuth 2.0 integration for secure external API access

## The Problem

**Scene 1: The Data War**
Franchisee edits Google Business Profile directly → Uberall doesn't know → Next sync overwrites the edit → Franchisee edits again → Endless conflict. Neither wins. Customer sees inconsistent data.

**Scene 2: The Silent Closure**
Branch closes for renovation → POS shows zero transactions for 5 days → Nobody updates Uberall → Google Maps shows "open" → Customers drive there, find it closed → Angry 1-star reviews.

**Scene 3: The Authority Conflict**
Head office sets standard hours 9-6 → Local franchisee knows their market needs 8-8 → Edits Google directly → Uberall reverts → Franchisee re-edits → Data war continues.

**Root Cause:** Uberall doesn't know what it doesn't know. UB-I optimizes based on what's inside the platform — this gateway feeds it better data from outside the platform boundary.

## Architecture

```mermaid
graph TB
    subgraph EXTERNAL["🌐 EXTERNAL SOURCES"]
        GMB["Google My Business<br/>(OAuth 2.0)"]
        POS["POS System"]
        ERP["ERP System"]
    end

    subgraph GATEWAY["Location Sync Gateway Service"]
        subgraph CONNECTOR_LAYER["🔌 Connector Layer"]
            CONNECTORS["GoogleConnector<br/>PosConnector<br/>ErpConnector<br/>WebhookConnector"]
        end

        subgraph INGESTION_LAYER["⚙️ Ingestion Layer"]
            SCHEDULER["@Scheduled Jobs<br/>Pull • Validate • Deduplicate"]
            KAFKA_PROD["Kafka Producer"]
        end

        subgraph EVENT_LAYER["📨 Event Streaming"]
            KAFKA["Kafka Topic<br/>location.snapshots"]
        end

        subgraph PROCESSING_LAYER["🔍 Processing Layer"]
            CONSUMER["Kafka Consumer"]
            DETECTOR["Conflict Detector"]
            ENGINE["Resolution Engine<br/>(YAML Rules)"]
        end

        subgraph STORAGE_LAYER["💾 Storage Layer"]
            REDIS["Redis Cache<br/>Tokens • Data"]
            POSTGRES[(PostgreSQL<br/>Conflicts • Decisions<br/>Audit Log)]
        end

        subgraph API_LAYER["🌐 REST API"]
            REST["Endpoints<br/>/sync • /conflicts<br/>/decisions • /rules"]
        end
    end

    subgraph OUTPUTS["🎯 OUTPUTS"]
        UBERALL["Uberall API<br/>PATCH locations"]
        NOTIFY["Notifications<br/>Slack • Email"]
        CLIENTS["Client Apps<br/>Web • Mobile<br/>Dashboards"]
    end

    subgraph TECH["Tech Stack"]
        STACK["• Kotlin + Micronaut<br/>• PostgreSQL + R2DBC<br/>• Kafka + Redis<br/>• OAuth 2.0<br/>• Prometheus"]
    end

    EXTERNAL -->|Pull Data| CONNECTORS
    CONNECTORS --> SCHEDULER
    SCHEDULER --> KAFKA_PROD
    KAFKA_PROD --> KAFKA
    KAFKA --> CONSUMER
    CONSUMER --> DETECTOR
    DETECTOR --> ENGINE
    ENGINE -->|AUTO_ACCEPT| UBERALL
    ENGINE -->|AUTO_REVERT| UBERALL
    ENGINE -->|FLAG_REVIEW| NOTIFY
    ENGINE --> POSTGRES
    POSTGRES --> REDIS
    REDIS --> REST
    REST --> CLIENTS

    style EXTERNAL fill:#E3F2FD,stroke:#1976D2,stroke-width:3px,color:#000
    style GATEWAY fill:#E8F5E9,stroke:#388E3C,stroke-width:4px,color:#000
    style OUTPUTS fill:#FFF3E0,stroke:#F57C00,stroke-width:3px,color:#000
    style TECH fill:#F3E5F5,stroke:#7B1FA2,stroke-width:3px,color:#000

    style CONNECTOR_LAYER fill:#C8E6C9,stroke:#388E3C,stroke-width:2px,color:#000
    style INGESTION_LAYER fill:#FFCCBC,stroke:#E64A19,stroke-width:2px,color:#000
    style EVENT_LAYER fill:#FFF9C4,stroke:#FBC02D,stroke-width:2px,color:#000
    style PROCESSING_LAYER fill:#D1C4E9,stroke:#673AB7,stroke-width:2px,color:#000
    style STORAGE_LAYER fill:#B2DFDB,stroke:#00897B,stroke-width:2px,color:#000
    style API_LAYER fill:#BBDEFB,stroke:#1976D2,stroke-width:2px,color:#000

    style GMB fill:#ffffff,stroke:#1976D2,stroke-width:2px,color:#000
    style POS fill:#ffffff,stroke:#1976D2,stroke-width:2px,color:#000
    style ERP fill:#ffffff,stroke:#1976D2,stroke-width:2px,color:#000
    style CONNECTORS fill:#ffffff,stroke:#388E3C,stroke-width:2px,color:#000
    style SCHEDULER fill:#ffffff,stroke:#E64A19,stroke-width:2px,color:#000
    style KAFKA_PROD fill:#ffffff,stroke:#FBC02D,stroke-width:2px,color:#000
    style KAFKA fill:#ffffff,stroke:#000,stroke-width:2px,color:#000
    style CONSUMER fill:#ffffff,stroke:#673AB7,stroke-width:2px,color:#000
    style DETECTOR fill:#ffffff,stroke:#673AB7,stroke-width:2px,color:#000
    style ENGINE fill:#ffffff,stroke:#673AB7,stroke-width:2px,color:#000
    style REDIS fill:#ffffff,stroke:#00897B,stroke-width:2px,color:#000
    style POSTGRES fill:#ffffff,stroke:#00897B,stroke-width:2px,color:#000
    style REST fill:#ffffff,stroke:#1976D2,stroke-width:2px,color:#000
    style UBERALL fill:#ffffff,stroke:#F57C00,stroke-width:2px,color:#000
    style NOTIFY fill:#ffffff,stroke:#F57C00,stroke-width:2px,color:#000
    style CLIENTS fill:#ffffff,stroke:#F57C00,stroke-width:2px,color:#000
    style STACK fill:#ffffff,stroke:#7B1FA2,stroke-width:2px,color:#000
```

<p align="center">
  <em>Event-driven architecture with pluggable connectors - Horizontally scalable via Kafka consumers</em>
</p>

**Architecture Pattern:** Connector-Based Event-Driven Pipeline with Strategy-Based Conflict Resolution

**Stack**: Kotlin • Micronaut • PostgreSQL • Kafka • Redis • Google OAuth 2.0 • Flyway • Prometheus

## How It Works

### 1. Data Ingestion
- **@Scheduled** job runs every 5 minutes
- Each connector pulls location data from its source
- Snapshots published to Kafka topic `location.snapshots`
- Async processing decouples ingestion from resolution

### 2. Conflict Detection
- Kafka consumer receives snapshot
- Fetches current Uberall data for comparison
- Field-by-field diff identifies conflicts
- Conflict stored in PostgreSQL with full context

### 3. Resolution Engine
- Loads YAML rules from version-controlled config
- Matches rule by field type + source + location pattern
- Applies confidence threshold checks
- Produces one of three outcomes:

**AUTO_ACCEPT** → External value is more accurate
**AUTO_REVERT** → Uberall value is authoritative
**FLAG_FOR_REVIEW** → Ambiguous, needs human decision

### 4. Execution
- AUTO_ACCEPT → PATCH Uberall API with new value
- AUTO_REVERT → Trigger Uberall sync to revert external edit
- FLAG_FOR_REVIEW → Send Slack notification to ops team
- All actions logged to audit table with full context

### 5. Audit & Compliance
- Every conflict detected → PostgreSQL row
- Every resolution decision → Audit log entry
- Every Uberall API call → Request/response logged
- Idempotency keys prevent duplicate updates

---

## ⭐ New: AI-Powered Quality & Intelligence Layer

### Quality Validation
Every external snapshot is scored (0-100) before conflict resolution:

```kotlin
data class QualityScore(
    overallScore: Int,              // 0-100
    breakdown: {
        coordinates: 20,            // Missing/invalid coords
        address: 20,                // Incomplete address
        openingHours: 20,           // Invalid hours
        keywords: 20,               // Missing keywords
        consistency: 20             // Degradation detected
    },
    issues: [...],                  // MISSING_COORDINATES, etc.
    severity: CRITICAL | HIGH | MEDIUM | LOW
)
```

**Quality-Aware Resolution:**
- **Critical quality (< 40)** → AUTO_REVERT (reject bad data)
- **Quality degradation (> 20 point drop)** → FLAG_FOR_REVIEW
- **High severity** → Increase confidence threshold to 0.95

### AI Features

**1. Anomaly Detection** (Isolation Forest)
- Detects suspicious coordinate moves (> 100km)
- Catches gradual quality degradation
- Identifies bulk edit patterns

**2. Resolution Prediction** (XGBoost)
- Learns from historical decisions
- Predicts AUTO_ACCEPT vs AUTO_REVERT vs FLAG_FOR_REVIEW
- Adapts to franchisee behavior over time

**3. Natural Language Explanations** (GPT-4/Claude)
```
"The franchisee in Mombasa extended their opening hours from 9am-6pm
to 8am-8pm. This change looks legitimate because:

1. The data quality is high (92/100) with no red flags
2. This franchisee has made 15 similar changes in the past year,
   and 14 were correct
3. Local market hours in Mombasa typically run 8am-8pm

Recommendation: Accept this change and update Uberall to match."
```

**Documentation:**
- [Quality Integration Details](docs/04-quality-integration.md)
- [AI Strategy & Implementation](docs/05-ai-integration-strategy.md)
- [Uberall Challenge Integration](docs/UBERALL-CHALLENGE-INTEGRATION.md)

---

## Quick Start

### 🚀 Fastest Way to Run (Development)

**No Docker Required!** Run with in-memory database for quick testing:

```bash
# Clone repository
git clone https://github.com/c-kiplimo/Location-Sync-Gateway.git
cd Location-Sync-Gateway

# Build project
./gradlew clean build -x test

# Run with development profile (H2 in-memory database)
MICRONAUT_ENVIRONMENTS=dev ./gradlew run

# On Windows
set MICRONAUT_ENVIRONMENTS=dev && gradlew run
```

Application starts at **http://localhost:8080**

Access **Swagger UI** at http://localhost:8080/swagger-ui

#### Test Simulation Endpoints

```bash
# Generate 100 simulated location snapshots
curl -X POST "http://localhost:8080/api/v1/simulation/generate?count=100"

# Generate conflict scenarios
curl -X POST "http://localhost:8080/api/v1/simulation/generate-conflicts?count=50"

# Generate temporary closure snapshots
curl -X POST "http://localhost:8080/api/v1/simulation/generate-closures?count=20"

# Stress test with high volume
curl -X POST "http://localhost:8080/api/v1/simulation/stress-test?count=1000"
```

### Current Implementation Status

**✅ Currently Implemented:**
- Core conflict detection and resolution logic
- Mock Google My Business connector with test data
- In-memory storage (no database setup required)
- REST API for manual sync triggering
- Resolution engine with hardcoded rules (phone, opening hours, categories)
- Unit tests for all core components

**⏳ Planned Features:**
- PostgreSQL persistence layer
- Kafka event streaming for async processing
- Real external API integrations (Google OAuth, POS, ERP)
- Scheduled automated execution
- Notification system (Slack, Email)
- YAML-based configuration for resolution rules
- Management dashboard UI
- Production monitoring and observability

### Prerequisites
- JDK 17+
- Gradle 8+ (or use wrapper - `./gradlew`)

### Run Locally

#### Option 1: Standalone (Development)

```bash
# Clone repository
git clone https://github.com/c-kiplimo/Location-Sync-Gateway.git
cd Location-Sync-Gateway

# Run application (no Docker needed for current version)
./gradlew run

# Or on Windows
gradlew.bat run
```

**Server starts at `http://localhost:8080`**

#### Option 2: Docker Compose (Full Stack)

```bash
# Start all services (PostgreSQL, Kafka, Redis, Prometheus, Grafana)
make dev-up

# Or directly
docker-compose up -d

# Run database migrations
make db-migrate

# Create Kafka topics
make kafka-create-topics

# Run the application
./gradlew run

# Access services:
# - Application: http://localhost:8080
# - Kafka UI: http://localhost:8081
# - Grafana: http://localhost:3000
# - Prometheus: http://localhost:9090
# - Jaeger: http://localhost:16686
```

#### Option 3: Kubernetes (Production-Ready)

For container orchestration, scaling, and production deployment:

```bash
# Quick start with Minikube
minikube start --cpus=4 --memory=8192
minikube addons enable ingress metrics-server

# Deploy to Kubernetes
kubectl apply -f k8s/

# Or use Kustomize
kubectl apply -k k8s/

# Access the application
kubectl port-forward -n location-sync-gateway svc/location-sync-gateway 8080:80
```

**See [k8s/README.md](k8s/README.md) for comprehensive Kubernetes deployment guide including:**
- Production-ready manifests (Deployment, StatefulSets, Services, Ingress)
- Auto-scaling with HPA (3-12 replicas based on CPU/memory/Kafka lag)
- High availability with pod anti-affinity and disruption budgets
- Security with RBAC, Network Policies, and pod security contexts
- Monitoring with Prometheus and Grafana
- AWS EKS deployment architecture

### Test the API

```bash
# Trigger sync for test locations (1001-1004 have mock data)
curl -X POST "http://localhost:8080/api/v1/sync/trigger?locationIds=1001,1002,1003,1004"

# Expected output:
# {
#   "locationsProcessed": 4,
#   "conflictsDetected": 3,
#   "autoAccepted": 1,
#   "autoReverted": 1,
#   "flaggedForReview": 1
# }

# View all detected conflicts
curl http://localhost:8080/api/v1/conflicts

# View resolution decisions
curl http://localhost:8080/api/v1/decisions

# View conflicts flagged for human review
curl http://localhost:8080/api/v1/conflicts/flagged

# Health check
curl http://localhost:8080/api/v1/health
```

### Test Scenarios

| Location ID | Conflict | Expected Resolution |
|-------------|----------|---------------------|
| **1001** | Phone differs from Uberall | AUTO_REVERT (HQ controls phone) |
| **1002** | Opening hours differ from Uberall | AUTO_ACCEPT (franchisees control hours) |
| **1003** | Category differs from Uberall | FLAG_FOR_REVIEW (no rule configured) |
| **1004** | All fields match | No conflicts detected |

## API

### Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/sync/trigger` | Trigger manual sync for location IDs |
| GET | `/api/v1/conflicts` | Get all detected conflicts |
| GET | `/api/v1/conflicts/location/{id}` | Get conflicts for specific location |
| GET | `/api/v1/conflicts/flagged` | Get conflicts needing human review |
| GET | `/api/v1/decisions` | Get all resolution decisions |
| GET | `/api/v1/decisions/conflict/{id}` | Get decision for specific conflict |
| GET | `/api/v1/audit/location/{id}` | Get audit log for location |
| GET | `/api/v1/rules` | List all resolution rules |
| POST | `/api/v1/rules` | Create new resolution rule |
| PUT | `/api/v1/rules/{id}` | Update resolution rule |
| DELETE | `/api/v1/rules/{id}` | Delete resolution rule |
| GET | `/api/v1/connectors/health` | Check health of all connectors |
| GET | `/api/v1/health` | Application health check |

### Response Format

All responses use standardized wrapper:

```json
{
  "meta": {
    "requestId": "550e8400-e29b-41d4-a716-446655440000",
    "status": "success",
    "statusDesc": "Operation completed",
    "respondedAt": "2026-06-15T10:30:00Z"
  },
  "payload": {
    "locationsProcessed": 4,
    "conflictsDetected": 3,
    "autoAccepted": 1,
    "autoReverted": 1,
    "flaggedForReview": 1
  }
}
```

### Sample Responses

**Conflict Response:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "locationId": 1001,
  "fieldType": "PHONE",
  "source": "GOOGLE_MY_BUSINESS",
  "externalValue": "+254799999999",
  "uberallValue": "+254712345678",
  "confidence": 1.0,
  "detectedAt": "2026-06-15T10:30:00Z"
}
```

**Resolution Decision Response:**
```json
{
  "id": "660e8400-e29b-41d4-a716-446655440001",
  "conflictId": "550e8400-e29b-41d4-a716-446655440000",
  "locationId": 1001,
  "fieldType": "PHONE",
  "source": "GOOGLE_MY_BUSINESS",
  "externalValue": "+254799999999",
  "uberallValue": "+254712345678",
  "outcome": "AUTO_REVERT",
  "reason": "Phone numbers are centrally managed by head office",
  "ruleApplied": "google-phone-hq-override",
  "actionTaken": "UBERALL_SYNC_TRIGGERED",
  "decidedAt": "2026-06-15T10:30:01Z",
  "executedAt": "2026-06-15T10:30:02Z"
}
```

## Resolution Rules Configuration

Rules defined in YAML, checked into version control:

```yaml
# resolution-rules.yml
rules:
  - name: google-opening-hours-franchisee
    fieldType: OPENING_HOURS
    source: GOOGLE_MY_BUSINESS
    locationPattern: "franchise.*"       # Regex pattern
    defaultOutcome: AUTO_ACCEPT          # Franchisees control hours
    minimumConfidence: 0.9
    conditions:
      - type: VALUE_WITHIN_RANGE
        field: openingHourSpan
        min: 6                           # Min 6 hours per day
        max: 16                          # Max 16 hours per day

  - name: google-phone-hq-override
    fieldType: PHONE
    source: GOOGLE_MY_BUSINESS
    defaultOutcome: AUTO_REVERT          # HQ controls phone numbers
    minimumConfidence: 1.0
    reason: "Phone numbers are centrally managed"

  - name: google-category-brand-standard
    fieldType: CATEGORIES
    source: GOOGLE_MY_BUSINESS
    defaultOutcome: AUTO_REVERT          # Brand category non-negotiable
    minimumConfidence: 1.0

  - name: pos-temporary-closure
    fieldType: TEMPORARY_CLOSURE
    source: POS_SYSTEM
    defaultOutcome: FLAG_FOR_REVIEW      # Always confirm before closing
    minimumConfidence: 0.85
    notifyChannel: slack
    notifyTarget: "#location-ops-alerts"

  - name: erp-planned-closure
    fieldType: TEMPORARY_CLOSURE
    source: ERP_SYSTEM
    defaultOutcome: AUTO_ACCEPT          # ERP work orders are authoritative
    minimumConfidence: 1.0
```

### Resolution Outcomes

| Outcome | Behavior | Use Case |
|---------|----------|----------|
| **AUTO_ACCEPT** | Update Uberall with external value | Franchisee sets local hours |
| **AUTO_REVERT** | Trigger Uberall sync to revert external edit | Unauthorized category change |
| **FLAG_FOR_REVIEW** | Send Slack notification, wait for human | POS signals possible closure |

## Use Cases

### 1. Franchisee Edits Opening Hours
**Scenario:** Franchisee in Mombasa extends hours to 8am-8pm on Google Business Profile because of local market demand.

**Detection:** Google connector pulls snapshot, detects hours differ from Uberall's 9am-6pm.

**Resolution:** Rule `google-opening-hours-franchisee` applies → AUTO_ACCEPT (franchisees allowed to set hours within 6-16 hour range).

**Action:** PATCH Uberall API to update opening hours to 8am-8pm.

**Result:** Both systems aligned. No data war. Customers see correct hours.

---

### 2. POS System Signals Closure
**Scenario:** POS system shows zero transactions for 3 consecutive trading days. Branch might be closed for renovation.

**Detection:** POS connector infers closure signal with confidence 0.80.

**Resolution:** Rule `pos-temporary-closure` applies → FLAG_FOR_REVIEW (confidence below 0.85 threshold).

**Action:** Slack message to `#location-ops-alerts` with full context.

**Result:** Ops team confirms closure, manually updates Uberall before customers affected.

---

### 3. Unauthorized Category Change
**Scenario:** Someone changes Google Business Profile category from "Restaurant" to "Bar".

**Detection:** Google connector pulls category, detects conflict.

**Resolution:** Rule `google-category-brand-standard` applies → AUTO_REVERT (brand categories are centrally controlled).

**Action:** Trigger Uberall sync to push correct category back to Google.

**Result:** Brand consistency maintained across all locations.

---

### 4. ERP Signals Planned Renovation
**Scenario:** ERP system has active work order for branch renovation, start date tomorrow.

**Detection:** ERP connector pulls work order, creates TEMPORARY_CLOSURE snapshot with confidence 1.0.

**Resolution:** Rule `erp-planned-closure` applies → AUTO_ACCEPT (ERP is authoritative for planned closures).

**Action:** Update Uberall with temporary closure dates.

**Result:** Google Maps shows "Temporarily Closed" before customers try to visit.

## Configuration

```yaml
# src/main/resources/application.yml
micronaut:
  application:
    name: location-sync-gateway
  server:
    port: 8080

datasources:
  default:
    url: jdbc:postgresql://localhost:5432/location_sync_gateway
    username: lsg
    password: ${DB_PASSWORD}
    driverClassName: org.postgresql.Driver

flyway:
  datasources:
    default:
      enabled: true

kafka:
  bootstrap:
    servers: localhost:9092
  producers:
    default:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: io.micronaut.kafka.serdes.JsonSerializer
  consumers:
    default:
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: io.micronaut.kafka.serdes.JsonDeserializer

redis:
  uri: redis://localhost:6379
  caches:
    location-data:
      expire-after-write: 5m
    oauth-tokens:
      expire-after-write: 55m

google:
  oauth:
    clientId: ${GOOGLE_OAUTH_CLIENT_ID}
    clientSecret: ${GOOGLE_OAUTH_CLIENT_SECRET}

uberall:
  api:
    baseUrl: https://uberall.com/api
    privateKey: ${UBERALL_PRIVATE_KEY}

scheduler:
  ingestion:
    enabled: true
    fixedDelay: 5m
    initialDelay: 30s

notifications:
  slack:
    webhookUrl: ${SLACK_WEBHOOK_URL}
    channel: "#location-ops-alerts"
```

## Development

### Build
```bash
./gradlew clean build
```

### Run Tests
```bash
./gradlew test
```

### Database Migrations
Auto-applied via Flyway on startup. Files in `src/main/resources/db/migration/`

---

## Implementation Roadmap

### Phase 1: Core Proof of Concept ✅ (COMPLETE)

**Goal:** Demonstrate end-to-end conflict detection and resolution flow with minimal infrastructure.

**Implementation Steps:**
1. Define core domain models (LocationSnapshot, Conflict, ResolutionDecision, ResolutionOutcome enum)
2. Create LocationConnector interface for pluggable external data sources
3. Implement MockGoogleMyBusinessConnector with hardcoded test data for 4 location scenarios
4. Implement MockUberallDataService with hardcoded baseline data for comparison
5. Build ConflictDetector service to compare snapshot fields against Uberall data
6. Build ResolutionEngine service with hardcoded if/else rules for field+source combinations
7. Create InMemoryStorage service using ConcurrentHashMap for conflicts and decisions
8. Implement SyncService to orchestrate full pipeline: pull → detect → resolve → store
9. Build REST API controller with endpoints for manual triggering and viewing results
10. Write unit tests for ConflictDetector, ResolutionEngine, and InMemoryStorage
11. Test end-to-end flow with 4 test locations via REST API

**Success Criteria:**
- Manual sync trigger detects conflicts correctly
- Resolution rules apply as expected (AUTO_ACCEPT, AUTO_REVERT, FLAG_FOR_REVIEW)
- All conflicts and decisions retrievable via REST API
- Unit tests pass with 100% coverage of core logic

---

### Phase 2: Persistence & Automation ⏳ (PLANNED)

**Goal:** Make system production-ready with real persistence and automated execution.

**Implementation Steps:**

#### Database Layer
1. Add PostgreSQL and Flyway dependencies to build.gradle.kts
2. Create Flyway migration: V1__create_tables.sql for conflicts, decisions, rules, mappings tables
3. Create Flyway migration: V2__create_indexes.sql for performance optimization
4. Create Flyway migration: V3__seed_rules.sql to pre-populate resolution rules
5. Implement JPA/JDBC repository interfaces for each table (ConflictRepository, DecisionRepository, etc.)
6. Replace InMemoryStorage with PostgreSQL repositories in all services
7. Add database connection pooling configuration (HikariCP)
8. Write integration tests using Testcontainers for PostgreSQL

#### YAML Configuration
9. Create resolution-rules.yml file with all resolution rules in structured format
10. Implement RulesRepository to load YAML rules on startup and cache in memory
11. Update ResolutionEngine to query rules from repository instead of hardcoded logic
12. Add rule validation on startup to catch configuration errors early
13. Implement rules reload endpoint for runtime updates without restart

#### Scheduled Execution
14. Annotate SyncService orchestration method with @Scheduled(fixedDelay = "5m")
15. Add configuration properties for schedule timing (fixedDelay, initialDelay)
16. Implement application.yml with scheduler enable/disable flag
17. Add scheduler state monitoring endpoint for observability

#### Uberall API Integration
18. Create UberallApiClient with HTTP client configuration for external calls
19. Implement UberallApiExecutor service to handle PATCH requests for AUTO_ACCEPT outcomes
20. Implement sync triggering for AUTO_REVERT outcomes
21. Add idempotency logic using Redis or database keys to prevent duplicate updates
22. Implement retry logic with exponential backoff for failed API calls
23. Add circuit breaker pattern for graceful degradation when Uberall API is down
24. Use WireMock in tests to stub Uberall API responses

#### Audit & Compliance
25. Implement AuditLogger service to record every conflict detection event
26. Log every resolution decision with full context (rule applied, reason, timestamp)
27. Log every Uberall API call with request payload and response status
28. Add audit query endpoints for compliance reporting

**Success Criteria:**
- System runs automatically every 5 minutes
- All data persists across restarts
- Rules loaded from YAML and applied correctly
- Uberall API called with proper payloads (verified via WireMock)
- Idempotency prevents duplicate API calls
- Full audit trail queryable via API
- Integration tests pass with real database and mocked external APIs

---

### Phase 3: Multi-Source & Event Streaming ⏳ (PLANNED)

**Goal:** Add multiple data sources and asynchronous processing with Kafka.

**Implementation Steps:**

#### Kafka Infrastructure
1. Add Kafka and Micronaut Kafka dependencies to build.gradle.kts
2. Create docker-compose.yml with Kafka, Zookeeper, PostgreSQL, Redis services
3. Configure Kafka producers in application.yml (serializers, topic names)
4. Configure Kafka consumers in application.yml (deserializers, consumer groups)
5. Create Kafka topic: location.snapshots with appropriate partitions and replication

#### Event-Driven Architecture
6. Implement SnapshotProducer service to publish LocationSnapshot events to Kafka
7. Refactor IngestionScheduler to publish to Kafka instead of direct processing
8. Implement SnapshotConsumer service to consume from location.snapshots topic
9. Move conflict detection and resolution logic into Kafka consumer
10. Add dead letter queue (DLQ) topic for failed message processing
11. Implement Kafka consumer error handling and retry logic
12. Add consumer offset management for exactly-once semantics

#### POS System Connector
13. Define PosSystemClient interface for external POS system integration
14. Implement PosSystemConnector to query transaction counts for lookback period
15. Implement zero-transaction detection logic with configurable lookback days
16. Calculate closure confidence score based on consecutive zero-transaction days
17. Create TEMPORARY_CLOSURE field snapshots when confidence exceeds threshold
18. Add POS connector health check endpoint
19. Write tests using WireMock to stub POS system API responses

#### ERP System Connector
20. Define ErpSystemClient interface for external ERP system integration
21. Implement ErpSystemConnector to query active work orders for locations
22. Filter work orders by type (renovation, maintenance) and status (active)
23. Extract closure date ranges from work order start/end dates
24. Create TEMPORARY_CLOSURE snapshots with confidence 1.0 for ERP signals
25. Add ERP connector health check endpoint
26. Write tests using WireMock to stub ERP system API responses

#### Notification Engine
27. Implement SlackClient for Slack webhook integration
28. Implement EmailClient for SMTP email sending
29. Create NotificationEngine service to handle FLAG_FOR_REVIEW outcomes
30. Build Slack message templates with conflict details and action buttons
31. Build email templates with HTML formatting and embedded context
32. Add notification channel routing based on rule configuration
33. Implement notification delivery retry logic with exponential backoff
34. Add notification delivery audit logging

**Success Criteria:**
- Kafka consumers process snapshots asynchronously and independently
- POS connector detects closure signals from transaction data
- ERP connector detects planned closures from work orders
- Slack notifications sent for FLAG_FOR_REVIEW conflicts
- System scales horizontally by adding more Kafka consumer instances
- All connectors tested with WireMock for deterministic behavior
- Dead letter queue captures failed messages for manual review

---

### Phase 4: Production & Enterprise Features ⏳ (PLANNED)

**Goal:** Real OAuth integration, advanced rules, and management UI.

**Implementation Steps:**

#### Google OAuth 2.0
1. Register application in Google Cloud Console to obtain OAuth credentials
2. Configure OAuth client ID and client secret in application.yml
3. Implement OAuthTokenStore repository to persist access and refresh tokens
4. Implement GoogleOAuthManager to handle token refresh before expiry
5. Update GoogleMyBusinessConnector to use real OAuth tokens
6. Implement OAuth callback endpoint to receive authorization codes
7. Add token expiration monitoring and proactive refresh logic
8. Implement token revocation handling when user disconnects
9. Add rate limiting compliance for Google My Business API calls
10. Write integration tests with OAuth token mocking

#### Advanced Rule Conditions
11. Extend ResolutionRule model to support complex condition types
12. Implement VALUE_WITHIN_RANGE condition for numeric field validation
13. Implement CHANGE_MAGNITUDE condition to detect suspicious large changes
14. Implement LOCATION_PATTERN condition for regex-based location filtering
15. Implement rule condition evaluator with short-circuit logic
16. Add rule testing endpoint to validate rules against sample conflicts
17. Implement rule versioning to track changes over time

#### Rules Management API
18. Implement RulesController with full CRUD endpoints for rule management
19. Add rule validation logic to prevent invalid rules from being saved
20. Implement rule activation/deactivation without deletion
21. Add rule priority ordering when multiple rules match same conflict
22. Implement rule conflict detection (overlapping rules with different outcomes)
23. Add rule usage analytics to show which rules are firing most often
24. Create audit log for all rule changes (who, when, what changed)

#### Management Dashboard (Basic)
25. Set up basic frontend project (React/Vue or Thymeleaf templates)
26. Create conflicts list view with filtering and sorting
27. Create conflict detail view with timeline and full context
28. Create decisions audit log view with date range filtering
29. Create rules management interface for CRUD operations
30. Create connector health status dashboard
31. Add location-specific conflict history view
32. Implement manual conflict resolution workflow UI

#### Metrics & Observability
33. Add Micrometer and Prometheus dependencies
34. Implement metrics collectors for conflicts detected by source and field
35. Implement metrics collectors for decisions executed by outcome
36. Implement Uberall API latency histograms
37. Implement connector pull duration histograms
38. Create custom Grafana dashboards for visualization
39. Add application health check aggregator for all dependencies
40. Implement structured logging with correlation IDs for request tracing

#### Production Readiness
41. Implement circuit breaker pattern for all external API calls
42. Add retry logic with exponential backoff for transient failures
43. Implement graceful shutdown to complete in-flight processing
44. Add request rate limiting to prevent abuse of public endpoints
45. Implement API authentication and authorization (JWT tokens)
46. Add comprehensive error handling with user-friendly messages
47. Implement backup and restore procedures for PostgreSQL
48. Create runbook documentation for common operational scenarios
49. Set up staging environment mimicking production configuration
50. Perform load testing with realistic traffic patterns

**Success Criteria:**
- Real Google My Business API connection works with OAuth flow
- OAuth tokens refresh automatically before expiration
- Advanced rule conditions evaluated correctly for complex scenarios
- Dashboard allows manual conflict resolution without API calls
- All metrics exported and visible in Grafana dashboards
- Health checks report comprehensive system status
- Circuit breakers prevent cascade failures
- System handles 1000+ locations with sub-second response times
- Production deployment successful with zero downtime

---

### Phase 5: AI & Intelligence Layer ⏳ (PLANNED)

**Goal:** Add AI-powered quality validation, anomaly detection, and intelligent resolution prediction.

**Prerequisites:**
- Phase 2 complete (need historical conflict/decision data in PostgreSQL)
- Minimum 3-6 months of production data for ML training
- External AI API access (OpenAI/Anthropic for LLM features)

**Implementation Steps:**

#### Data Quality Scoring Engine
1. Design QualityScore model with breakdown by field category (coordinates, address, hours, etc.)
2. Implement rule-based quality validators for each field type:
   - CoordinatesValidator: Check for missing/invalid lat/lng, detect implausible locations
   - AddressValidator: Validate completeness (street, city, postal code), format consistency
   - OpeningHoursValidator: Check for invalid time ranges, impossible schedules
   - KeywordsValidator: Detect missing required fields (name, category)
3. Implement ConsistencyValidator to compare current snapshot against historical baseline
4. Create QualityScorer service to aggregate individual field scores (weighted average)
5. Define severity levels (CRITICAL < 40, HIGH < 60, MEDIUM < 80, LOW >= 80)
6. Integrate quality scoring into ConflictDetector pipeline (score before resolution)
7. Add quality-based rejection rules:
   - CRITICAL quality → AUTO_REVERT (reject bad data)
   - Quality degradation > 20 points → FLAG_FOR_REVIEW
   - HIGH severity → Increase confidence threshold to 0.95
8. Add quality score to Conflict model and database schema
9. Create quality trends API endpoint for monitoring degradation over time
10. Write unit tests for each validator with edge cases

#### Anomaly Detection (Isolation Forest)
11. Add scikit-learn or similar ML library for anomaly detection
12. Design feature engineering pipeline to extract numerical features from snapshots:
    - Coordinate distance from baseline (haversine formula)
    - Quality score delta from moving average
    - Number of fields changed in single snapshot
    - Time since last change for each field
13. Implement HistoricalDataExtractor to query PostgreSQL for training data
14. Implement AnomalyDetector service using Isolation Forest algorithm
15. Train initial model on historical snapshots (mark known good vs problematic)
16. Configure anomaly score threshold (e.g., > 0.7 = anomalous)
17. Integrate anomaly detection into quality scoring pipeline
18. Add anomaly flags to QualityScore model (SUSPICIOUS_COORDINATE_MOVE, BULK_EDIT_PATTERN)
19. Implement model retraining scheduler (weekly or monthly)
20. Create model versioning and A/B testing framework
21. Add anomaly detection metrics to Prometheus (anomalies_detected_total)
22. Write integration tests with known anomalous patterns

#### Resolution Prediction (ML Model)
23. Design ML feature set for resolution prediction:
    - Field type, source, location pattern
    - Quality score, anomaly score
    - Historical decision patterns for this location/franchisee
    - Confidence score from conflict detector
    - Time-based features (day of week, hour, seasonality)
24. Implement FeatureExtractor to transform Conflict into ML feature vector
25. Extract labeled training data from decisions table (outcome as label)
26. Train XGBoost classifier to predict AUTO_ACCEPT vs AUTO_REVERT vs FLAG_FOR_REVIEW
27. Evaluate model performance (precision/recall for each class, confusion matrix)
28. Implement ResolutionPredictor service to invoke trained model
29. Add ML prediction as input to ResolutionEngine (suggest outcome before rules)
30. Implement confidence scoring for ML predictions
31. Add fallback logic: if ML confidence < 0.8, use rule-based resolution
32. Create A/B testing framework to compare ML vs rule-based outcomes
33. Implement model monitoring for prediction drift detection
34. Add prediction explanation features (SHAP values for interpretability)
35. Create model retraining pipeline triggered by performance degradation
36. Store ML prediction alongside rule-based decision for comparison
37. Add ML metrics to dashboard (prediction accuracy, confidence distribution)

#### Natural Language Explanations (LLM Integration)
38. Add OpenAI or Anthropic SDK dependencies to project
39. Configure API keys and rate limits in application.yml
40. Design LLM prompt templates for conflict explanation:
    - Conflict context (field, old value, new value, source)
    - Quality score and issues
    - Historical pattern analysis
    - Resolution recommendation with reasoning
41. Implement ExplanationGenerator service with LLM client
42. Create structured prompt with conflict details, quality score, ML prediction
43. Parse LLM response into structured format (summary, reasoning points, recommendation)
44. Add caching layer (Redis) for repeated similar conflicts
45. Implement rate limiting to stay within LLM API quotas
46. Add explanation generation for FLAG_FOR_REVIEW conflicts (notify ops team with context)
47. Create optional explanation endpoint for manual conflict review
48. Implement fallback template-based explanations when LLM unavailable
49. Add LLM response time metrics to Prometheus
50. Write tests with mocked LLM responses

#### Integration & Workflow
51. Update ConflictDetector to include quality scoring step
52. Update ResolutionEngine to consider:
    - Quality score (reject if critical)
    - Anomaly detection (flag if suspicious)
    - ML prediction (suggest outcome)
    - Rule-based resolution (final decision)
53. Add decision confidence aggregation (combine rule + ML + quality signals)
54. Implement "confidence cascade" logic:
    - If quality CRITICAL → AUTO_REVERT (skip ML/rules)
    - Else if anomaly detected → FLAG_FOR_REVIEW
    - Else if ML confidence > 0.9 → Use ML prediction
    - Else → Use rule-based resolution
55. Add LLM explanations to FLAG_FOR_REVIEW notifications
56. Create AI insights dashboard showing:
    - Quality score trends over time
    - Anomaly detection hit rate
    - ML prediction accuracy vs rule-based
    - LLM explanation samples
57. Implement feature flags to enable/disable each AI component independently
58. Add AI component health checks to observability
59. Create AI configuration management (model versions, thresholds, prompt templates)
60. Document AI decision-making process in audit logs

#### Model Operations (MLOps)
61. Set up model registry (MLflow or similar) for version control
62. Implement model deployment pipeline (train → validate → deploy)
63. Create model performance monitoring dashboard
64. Set up automated retraining triggers based on:
    - Performance degradation (accuracy drops below threshold)
    - Data drift detection (input distribution changes)
    - Time-based schedule (monthly retraining)
65. Implement model rollback capability if new version underperforms
66. Add model explainability reports for compliance
67. Create model testing suite with validation datasets
68. Document model architectures and hyperparameters

**Success Criteria:**
- Quality scoring blocks bad data before it enters resolution pipeline
- Anomaly detection catches suspicious coordinate moves and bulk edits
- ML model achieves > 85% accuracy on held-out test set
- ML predictions align with rule-based decisions 90%+ of cases (validate rules)
- LLM generates coherent explanations that ops team finds useful
- Quality score trends show improvement over time (external sources send better data)
- AI components degrade gracefully when unavailable (fallback to rules)
- Model retraining pipeline runs automatically without manual intervention
- AI insights dashboard helps identify patterns human rules missed
- Full audit trail includes AI scores, predictions, and explanations

**Cost Considerations:**
- LLM API costs: Estimate $0.01-0.05 per conflict explanation
- ML infrastructure: Training compute (GPU/TPU) vs inference (CPU)
- Consider running LLM only for FLAG_FOR_REVIEW cases (10-20% of conflicts)
- Implement aggressive caching for repeated patterns
- Monitor API usage and set budget alerts

**Data Requirements:**
- Minimum 10,000 historical conflicts for ML training (preferably 50,000+)
- Balanced dataset across outcomes (AUTO_ACCEPT, AUTO_REVERT, FLAG_FOR_REVIEW)
- Minimum 6 months of temporal coverage for seasonality patterns
- Representative data across all location types and sources

---

## Project Structure

```
src/main/kotlin/com/locationsync/gateway/
├── Application.kt                    # Main entry point
├── connector/
│   ├── LocationConnector.kt          # Connector interface
│   ├── GoogleMyBusinessConnector.kt  # Google API integration
│   ├── PosSystemConnector.kt         # POS system integration
│   ├── ErpSystemConnector.kt         # ERP system integration
│   └── oauth/
│       ├── OAuthTokenStore.kt        # Token management
│       └── GoogleOAuthManager.kt     # Token refresh logic
├── controller/
│   ├── SyncController.kt             # Sync API endpoints
│   ├── ConflictsController.kt        # Conflicts API
│   ├── RulesController.kt            # Rules management API
│   └── AuditController.kt            # Audit log API
├── model/
│   ├── Conflict.kt
│   ├── LocationSnapshot.kt
│   ├── ResolutionDecision.kt
│   ├── ResolutionRule.kt
│   └── AuditEntry.kt
├── service/
│   ├── IngestionScheduler.kt         # @Scheduled ingestion
│   ├── ConflictDetector.kt           # Conflict detection logic
│   ├── ResolutionEngine.kt           # Resolution rules engine
│   ├── UberallApiExecutor.kt         # Uberall API client
│   ├── NotificationEngine.kt         # Slack/Email alerts
│   ├── IdempotencyService.kt         # Duplicate prevention
│   └── ai/                           # AI & ML services (Phase 5)
│       ├── QualityScorer.kt          # Data quality validation
│       ├── AnomalyDetector.kt        # Isolation Forest anomaly detection
│       ├── ResolutionPredictor.kt    # XGBoost resolution prediction
│       ├── ExplanationGenerator.kt   # LLM-powered explanations
│       ├── FeatureExtractor.kt       # ML feature engineering
│       └── ModelRegistry.kt          # MLOps model versioning
├── repository/
│   ├── ConflictRepository.kt         # PostgreSQL DAO
│   ├── DecisionRepository.kt
│   ├── RulesRepository.kt
│   └── AuditRepository.kt
├── kafka/
│   ├── SnapshotProducer.kt           # Publish to Kafka
│   └── SnapshotConsumer.kt           # Consume from Kafka
└── config/
    ├── KafkaConfig.kt
    ├── RedisConfig.kt
    └── UberallClientConfig.kt

src/main/resources/
├── application.yml                   # Main config
├── resolution-rules.yml              # Conflict resolution rules
└── db/migration/                     # Flyway migrations
    ├── V1__create_tables.sql
    ├── V2__create_indexes.sql
    └── V3__seed_rules.sql
```

## External Sources Supported

| Source | What It Detects | Resolution Strategy |
|--------|-----------------|---------------------|
| **Google My Business API** | Direct edits by franchisees or customers | Accept hours/address, revert categories/phone |
| **POS System connector** | Zero transactions = closure signal | Flag for review (confidence-based) |
| **ERP System connector** | Renovation work orders = planned closure | Auto-accept (authoritative) |
| **Franchise data API** | Franchisee-managed location data | Reconcile per HQ policy |
| **Custom webhook** | Any external system push | Process via resolution engine |

## Why This Matters

### For Uberall
- **Makes UB-I Better:** Feeds UB-I accurate data by catching external edits before they cause conflicts
- **Prevents Data Wars:** Automated resolution stops endless back-and-forth between franchisees and HQ
- **Reduces Support Tickets:** Fewer customer complaints about wrong listing information
- **Competitive Differentiator:** No other location platform solves the "external edit" problem

### For Multi-Location Brands
- **Single Source of Truth:** Uberall becomes authoritative with confidence
- **Automated Compliance:** Business rules enforced automatically (phone numbers, categories)
- **Operational Efficiency:** Reduces manual reconciliation work by ops teams
- **Customer Experience:** Accurate listing information = fewer angry customers

### Revenue Impact
- **Lost Foot Traffic:** Wrong hours on Google = 5-10% customer deflection per location
- **Negative Reviews:** "Drove 20 minutes, found you closed" reviews damage ranking
- **Brand Reputation:** Inconsistent data across platforms erodes trust

## Technical Highlights

- **Kotlin Coroutines** - Async connector pulls without thread overhead
- **Event-Driven Architecture** - Kafka decouples ingestion from processing
- **Pluggable Connectors** - Add new sources by implementing one interface
- **Strategy Pattern** - Resolution rules as configurable strategies
- **Idempotent Operations** - Safe to reprocess same data (Kafka consumer offsets + Redis keys)
- **Circuit Breakers** - Graceful degradation when external APIs fail
- **OAuth Token Management** - Automatic token refresh for Google API
- **Audit-First Design** - Every decision logged immutably for compliance
- **Horizontal Scaling** - Multiple Kafka consumers process in parallel
- **Testable Architecture** - All external APIs stubbed with WireMock in tests

## Observability

### Metrics (Prometheus)
- `conflicts_detected_total{source, field_type}` - Counter
- `decisions_executed_total{outcome}` - Counter
- `uberall_api_latency_seconds` - Histogram
- `connector_pull_duration_seconds{connector_type}` - Histogram

### Health Checks
```bash
curl http://localhost:8080/api/v1/health
```

```json
{
  "status": "UP",
  "checks": [
    {"name": "postgres", "status": "UP"},
    {"name": "kafka", "status": "UP"},
    {"name": "redis", "status": "UP"},
    {"name": "uberall-api", "status": "UP", "latency": "45ms"},
    {"name": "google-connector", "status": "UP"},
    {"name": "pos-connector", "status": "UP"}
  ]
}
```

## 📚 Testing & Documentation

### Complete Testing Guide

See **[docs/guides/e2e-testing.md](docs/guides/e2e-testing.md)** for comprehensive end-to-end testing procedures including:
- ✅ Health checks for all services
- ✅ Individual service testing (Python AI + Kotlin Gateway)
- ✅ Integration testing (service-to-service communication)
- ✅ End-to-end workflow testing
- ✅ Monitoring verification (Prometheus, Grafana)
- ✅ Expected results for every test
- ✅ Troubleshooting guide

### Quick Test Commands

See **[docs/guides/QUICK_TEST_COMMANDS.md](docs/guides/QUICK_TEST_COMMANDS.md)** for copy-paste commands:
- Health check one-liners
- AI service endpoint tests
- Gateway simulation tests
- Monitoring queries
- Debugging commands

### Architecture Documentation

| Document | Description |
|----------|-------------|
| [MICROSERVICES_ARCHITECTURE.md](docs/MICROSERVICES_ARCHITECTURE.md) | Detailed microservices design |
| [MICROSERVICES_IMPLEMENTATION.md](MICROSERVICES_IMPLEMENTATION.md) | Implementation guide with code examples |
| [MICROSERVICES_COMPLETE.md](../MICROSERVICES_COMPLETE.md) | Project completion summary |
| [SOLUTION_ARCHITECTURE.md](docs/SOLUTION_ARCHITECTURE.md) | Overall solution architecture |
| [SETUP.md](docs/guides/SETUP.md) | Detailed setup instructions |
| [PROJECT_STATUS.md](PROJECT_STATUS.md) | Current project status |

### Project Review Procedure

**For recruiters, portfolio reviews, or demonstrations:**

1. **Read the overview** - [MICROSERVICES_COMPLETE.md](../MICROSERVICES_COMPLETE.md) (5 minutes)
2. **Review architecture** - [MICROSERVICES_ARCHITECTURE.md](docs/MICROSERVICES_ARCHITECTURE.md) (10 minutes)
3. **Start the system** - Follow [docs/guides/e2e-testing.md](docs/guides/e2e-testing.md) Phase 0 (5 minutes)
4. **Test the system** - Run tests from [docs/guides/QUICK_TEST_COMMANDS.md](docs/guides/QUICK_TEST_COMMANDS.md) (10 minutes)
5. **View the code** - Explore key files mentioned in documentation (20 minutes)
6. **Check monitoring** - Access Prometheus and Grafana dashboards (5 minutes)

**Total review time: ~1 hour for comprehensive understanding**

### API Documentation

**Python AI Service (Port 8081):**
- Interactive Swagger: http://localhost:8081/docs
- Endpoints: Quality Scoring, Anomaly Detection, Resolution Prediction

**Kotlin Gateway (Port 8080):**
- Interactive Swagger: http://localhost:8080/swagger-ui
- Endpoints: Data Simulation, Conflict Detection, Health Checks

## Contributing

1. Fork the repository
2. Create feature branch (`git checkout -b feature/amazing-feature`)
3. Commit changes (`git commit -m 'Add feature'`)
4. Push to branch (`git push origin feature/amazing-feature`)
5. Open Pull Request

**Guidelines**: Follow Kotlin conventions, use coroutines for async, write tests for all business logic, maintain idempotency.

## License

MIT License - see [LICENSE](LICENSE)

---

<div align="center">

**Built for Uberall Engineering**

*Solving the data gap outside the platform boundary*

[Report Bug](https://github.com/c-kiplimo/Location-Sync-Gateway/issues) • [Request Feature](https://github.com/c-kiplimo/Location-Sync-Gateway/issues)

</div>