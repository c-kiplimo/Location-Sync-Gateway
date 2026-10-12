# Testing Workflow - Visual Guide

**Step-by-step visual workflow for testing the Location Sync Platform**

---

## 🎯 Testing Flow Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    START TESTING                            │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 0: STARTUP                                           │
│  ✓ Start Docker Compose                                     │
│  ✓ Wait for health checks (30-60 seconds)                   │
│  ✓ Verify all containers running                            │
│                                                              │
│  Command: docker-compose -f docker-compose-microservices.yml up -d
│  Expected: 7 containers running (healthy)                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 1: HEALTH CHECKS (5 minutes)                         │
│  ✓ AI Service (8081) - curl localhost:8081/health          │
│  ✓ Gateway (8080) - curl localhost:8080/health             │
│  ✓ PostgreSQL - docker exec pg_isready                      │
│  ✓ Kafka - docker exec kafka-topics --list                  │
│  ✓ Redis - docker exec redis-cli PING                       │
│                                                              │
│  Success: All services respond "healthy"                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 2: PYTHON AI SERVICE TESTING (10 minutes)            │
│                                                              │
│  TEST 1: Quality Scoring - High Quality                     │
│  ├─ POST /api/v1/quality/score                              │
│  ├─ Payload: Complete location data                         │
│  └─ Expected: Score 85-100, severity "LOW"                  │
│                                                              │
│  TEST 2: Quality Scoring - Low Quality (Null Island)        │
│  ├─ POST /api/v1/quality/score                              │
│  ├─ Payload: Coordinates (0, 0)                             │
│  └─ Expected: Score < 60, "CRITICAL", null island detected  │
│                                                              │
│  TEST 3: Anomaly Detection - Normal                         │
│  ├─ POST /api/v1/anomaly/detect                             │
│  ├─ Payload: Valid Nairobi coordinates                      │
│  └─ Expected: isAnomalous = false, score < 0.5              │
│                                                              │
│  TEST 4: Anomaly Detection - Suspicious                     │
│  ├─ POST /api/v1/anomaly/detect                             │
│  ├─ Payload: Coordinates (0, 0)                             │
│  └─ Expected: isAnomalous = true, score > 0.7               │
│                                                              │
│  TEST 5: Resolution Prediction                              │
│  ├─ POST /api/v1/resolution/predict                         │
│  ├─ Payload: Low quality + high anomaly conflict            │
│  └─ Expected: "FLAG_FOR_REVIEW", riskLevel "HIGH"           │
│                                                              │
│  Success: All 5 tests return expected responses             │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 3: KOTLIN GATEWAY TESTING (10 minutes)               │
│                                                              │
│  TEST 1: Generate Simulated Data                            │
│  ├─ POST /api/v1/simulation/generate?count=10               │
│  └─ Expected: 10 events published to Kafka                  │
│                                                              │
│  TEST 2: Generate Conflicts                                 │
│  ├─ POST /api/v1/simulation/generate-conflicts?count=5      │
│  └─ Expected: 5 conflict pairs created                      │
│                                                              │
│  TEST 3: Generate Closures                                  │
│  ├─ POST /api/v1/simulation/generate-closures?count=3       │
│  └─ Expected: 3 closure events created                      │
│                                                              │
│  TEST 4: Verify Kafka Messages                              │
│  ├─ docker exec kafka-console-consumer                      │
│  └─ Expected: See JSON messages in location-snapshots topic │
│                                                              │
│  TEST 5: Verify Database Persistence                        │
│  ├─ docker exec psql SELECT COUNT(*) FROM conflicts         │
│  └─ Expected: Rows present in database                      │
│                                                              │
│  Success: Data flows through Kafka to database              │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 4: INTEGRATION TESTING (5 minutes)                   │
│                                                              │
│  TEST 1: Gateway → AI Service Communication                 │
│  ├─ Check AI_SERVICE_URL env variable                       │
│  ├─ Generate data in gateway                                │
│  └─ Monitor logs for "Calling AI Service"                   │
│                                                              │
│  TEST 2: End-to-End Conflict Flow                           │
│  ├─ Generate baseline snapshot (Uberall)                    │
│  ├─ Generate conflicting snapshot (Google)                  │
│  ├─ Watch logs for complete flow:                           │
│  │   • Quality scoring (2x)                                 │
│  │   • Conflict detection                                   │
│  │   • Resolution prediction                                │
│  │   • Decision execution                                   │
│  │   • Database persistence                                 │
│  └─ Expected: All steps logged successfully                 │
│                                                              │
│  Success: Services communicate correctly                    │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 5: MONITORING & OBSERVABILITY (5 minutes)            │
│                                                              │
│  PROMETHEUS (localhost:9090)                                │
│  ├─ Query: rate(http_requests_total[5m])                    │
│  ├─ Query: conflicts_detected_total                         │
│  └─ Expected: Metrics visible and updating                  │
│                                                              │
│  GRAFANA (localhost:3000)                                   │
│  ├─ Login: admin/admin                                      │
│  ├─ View: Service Overview dashboard                        │
│  ├─ View: AI Performance dashboard                          │
│  └─ Expected: Live data and charts                          │
│                                                              │
│  API DOCUMENTATION                                           │
│  ├─ AI Service: localhost:8081/docs (Swagger)               │
│  ├─ Gateway: localhost:8080/swagger-ui                      │
│  └─ Expected: Interactive API documentation                 │
│                                                              │
│  Success: All monitoring tools accessible                   │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│  PHASE 6: STRESS TESTING (Optional, 5 minutes)              │
│                                                              │
│  TEST: High Volume Data Generation                          │
│  ├─ POST /api/v1/simulation/stress-test?count=1000          │
│  ├─ Monitor: docker stats (CPU/memory)                      │
│  ├─ Monitor: Kafka lag                                      │
│  └─ Expected: All messages processed, no errors             │
│                                                              │
│  Success: System handles high load                          │
└────────────────────┬────────────────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────────────────┐
│                    TESTING COMPLETE ✅                       │
│                                                              │
│  Summary:                                                    │
│  • All services healthy                                     │
│  • AI service endpoints working                             │
│  • Gateway simulation working                               │
│  • Services communicating                                   │
│  • Data persisting correctly                                │
│  • Monitoring operational                                   │
│                                                              │
│  🎉 System is production-ready!                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 🔍 Detailed Test Scenarios

### Scenario 1: High Quality Location Data

**Purpose:** Verify AI service correctly scores complete, accurate data

```
INPUT:
├─ Name: "Perfect Restaurant & Cafe"
├─ Coordinates: Valid Nairobi location (-1.286389, 36.817223)
├─ Address: Complete (street, city, state, postal, country)
├─ Phone: Valid Kenyan format (+254712345678)
├─ Opening Hours: Full week schedule
├─ Categories: Multiple relevant categories
└─ Website: Valid URL

PROCESSING:
├─ POST /api/v1/quality/score
├─ AI Service validates all fields
└─ Calculates 5-component score

OUTPUT:
├─ overallScore: 95 (out of 100)
├─ breakdown:
│   ├─ coordinates: 20/20
│   ├─ address: 20/20
│   ├─ openingHours: 20/20
│   ├─ keywords: 20/20
│   └─ consistency: 15/20
├─ issues: [] (empty)
└─ severity: "LOW"

✅ PASS CRITERIA: Score 85-100, severity "LOW", minimal issues
```

---

### Scenario 2: Low Quality Data (Null Island)

**Purpose:** Verify AI service detects suspicious and incomplete data

```
INPUT:
├─ Name: "Bad Location"
├─ Coordinates: (0, 0) - "Null Island"
├─ Address: Missing
├─ Phone: Missing
├─ Opening Hours: Missing
└─ Categories: Missing

PROCESSING:
├─ POST /api/v1/quality/score
├─ AI Service detects null island
└─ Flags multiple missing fields

OUTPUT:
├─ overallScore: 25 (out of 100)
├─ breakdown:
│   ├─ coordinates: 0/20 (null island!)
│   ├─ address: 0/20 (missing)
│   ├─ openingHours: 0/20 (missing)
│   ├─ keywords: 10/20 (name only)
│   └─ consistency: 15/20
├─ issues: [
│   ├─ {field: "coordinates", type: "SUSPICIOUS_LOCATION", severity: "CRITICAL"}
│   ├─ {field: "address", type: "MISSING_REQUIRED_FIELD", severity: "HIGH"}
│   └─ {field: "openingHours", type: "MISSING_REQUIRED_FIELD", severity: "MEDIUM"}
│ ]
└─ severity: "CRITICAL"

✅ PASS CRITERIA: Score < 60, severity "CRITICAL", null island detected
```

---

### Scenario 3: Anomaly Detection

**Purpose:** Verify anomaly detector identifies unusual patterns

```
TEST 1: NORMAL LOCATION
INPUT: Valid Nairobi coordinates, complete data
OUTPUT:
├─ anomalyScore: 0.15
├─ isAnomalous: false
├─ anomalyReasons: []
└─ confidence: 0.85
✅ PASS: isAnomalous = false

TEST 2: SUSPICIOUS LOCATION
INPUT: Coordinates (0, 0)
OUTPUT:
├─ anomalyScore: 0.95
├─ isAnomalous: true
├─ anomalyReasons: ["SUSPICIOUS_NULL_ISLAND: Coordinates are (0,0)"]
└─ confidence: 0.90
✅ PASS: isAnomalous = true, null island detected
```

---

### Scenario 4: Resolution Prediction

**Purpose:** Verify AI service predicts correct conflict resolution

```
INPUT CONFLICT:
├─ locationId: 3001
├─ fieldType: "PHONE"
├─ source: "GOOGLE_MY_BUSINESS"
├─ currentValue: "+254700000000"
├─ incomingValue: "+254711111111"
├─ qualityScore: 45 (LOW!)
└─ anomalyScore: 0.85 (HIGH!)

PROCESSING:
├─ POST /api/v1/resolution/predict
├─ AI Service analyzes quality + anomaly
├─ Rule 1: Quality < 60 → Revert consideration
└─ Rule 2: Anomaly > 0.7 → Flag for review

OUTPUT:
├─ predictedOutcome: "FLAG_FOR_REVIEW"
├─ confidence: 0.85
├─ reasoning: [
│   ├─ "Quality score below threshold (45 < 60): Suggests data quality issues"
│   └─ "High anomaly score (0.85 > 0.70): Anomalous data pattern detected"
│ ]
├─ suggestedAction: "Manual review required due to quality and anomaly concerns"
└─ riskLevel: "HIGH"

✅ PASS CRITERIA: predictedOutcome = "FLAG_FOR_REVIEW", riskLevel = "HIGH"
```

---

### Scenario 5: End-to-End Conflict Flow

**Purpose:** Verify complete workflow from event ingestion to resolution

```
STEP 1: BASELINE SNAPSHOT
├─ Source: WEBHOOK (Uberall)
├─ LocationId: 50000
├─ Phone: "+254700000000"
└─ Quality Score: 92 (HIGH)

   ↓ Gateway receives snapshot
   ↓ Calls AI Service for quality scoring
   ↓ Stores in PostgreSQL

STEP 2: CONFLICTING SNAPSHOT
├─ Source: GOOGLE_MY_BUSINESS
├─ LocationId: 50000
├─ Phone: "+254711111111" (DIFFERENT!)
└─ Quality Score: 45 (LOW)

   ↓ Gateway receives snapshot
   ↓ Calls AI Service for quality scoring
   ↓ Detects conflict (phone field differs)
   ↓ Calls AI Service for anomaly detection
   ↓ Calls AI Service for resolution prediction
   ↓ Executes resolution (FLAG_FOR_REVIEW)
   ↓ Stores conflict in PostgreSQL
   ↓ (Optional) Sends notification

EXPECTED LOG SEQUENCE:
[gateway]    INFO: Received LocationSnapshot from WEBHOOK for location 50000
[gateway]    INFO: Calling AI Service for quality scoring
[ai-service] INFO: POST /api/v1/quality/score - 200 OK
[gateway]    INFO: Quality score: 92
[gateway]    INFO: Storing snapshot in database

[gateway]    INFO: Received LocationSnapshot from GOOGLE_MY_BUSINESS for location 50000
[gateway]    INFO: Calling AI Service for quality scoring
[ai-service] INFO: POST /api/v1/quality/score - 200 OK
[gateway]    INFO: Quality score: 45
[gateway]    INFO: Conflict detected! Field: PHONE
[gateway]    INFO: Calling AI Service for resolution prediction
[ai-service] INFO: POST /api/v1/resolution/predict - 200 OK
[gateway]    INFO: Predicted outcome: FLAG_FOR_REVIEW
[gateway]    INFO: Resolution executed: Manual review flagged
[gateway]    INFO: Conflict stored in database

VERIFICATION:
├─ Check PostgreSQL:
│   SELECT * FROM conflicts WHERE location_id = 50000;
│   Expected: 1 row with PHONE conflict
│
├─ Check Kafka:
│   Expected: 2 messages in location-snapshots topic
│
└─ Check Prometheus:
│   Expected: conflicts_detected_total increased by 1

✅ PASS CRITERIA: Complete flow logged, data persisted, metrics updated
```

---

## 🎨 Visual Service Communication

```
┌────────────────────┐
│  External Source   │  (Google My Business, POS, ERP)
└─────────┬──────────┘
          │ HTTP/Webhook
          ▼
┌────────────────────────────────────────────┐
│        KAFKA (location-snapshots)          │  Event Stream
└─────────┬──────────────────────────────────┘
          │ Consume
          ▼
┌────────────────────────────────────────────┐
│       KOTLIN GATEWAY SERVICE               │
│       (Port 8080)                          │
│                                            │
│  ┌──────────────────────────────────┐     │
│  │ Kafka Consumer                   │     │
│  └──────────┬───────────────────────┘     │
│             │                              │
│             ▼                              │
│  ┌──────────────────────────────────┐     │
│  │ Conflict Detection Engine        │     │
│  └──────────┬───────────────────────┘     │
│             │ HTTP POST                    │
│             ▼                              │
└─────────────┼──────────────────────────────┘
              │
              │ HTTP REST
              ▼
┌────────────────────────────────────────────┐
│      PYTHON AI SERVICE                     │
│      (Port 8081)                           │
│                                            │
│  ┌──────────────────────────────────┐     │
│  │ Quality Scorer                   │     │
│  │ • Validates 5 components         │     │
│  │ • Returns 0-100 score            │     │
│  └──────────────────────────────────┘     │
│                                            │
│  ┌──────────────────────────────────┐     │
│  │ Anomaly Detector                 │     │
│  │ • Isolation Forest algorithm     │     │
│  │ • Detects null island            │     │
│  └──────────────────────────────────┘     │
│                                            │
│  ┌──────────────────────────────────┐     │
│  │ Resolution Predictor             │     │
│  │ • Rule-based prediction          │     │
│  │ • Returns outcome + confidence   │     │
│  └──────────────────────────────────┘     │
│                                            │
└─────────────┬──────────────────────────────┘
              │ JSON Response
              ▼
┌────────────────────────────────────────────┐
│       KOTLIN GATEWAY SERVICE               │
│                                            │
│  ┌──────────────────────────────────┐     │
│  │ Resolution Engine                │     │
│  │ • Applies rules                  │     │
│  │ • Executes decision              │     │
│  └──────────┬───────────────────────┘     │
│             │                              │
│             ▼                              │
│  ┌──────────────────────────────────┐     │
│  │ Persistence Layer                │     │
│  │ • Stores conflict                │     │
│  │ • Stores decision                │     │
│  └──────────────────────────────────┘     │
└─────────────┬──────────────────────────────┘
              │
              ▼
┌────────────────────────────────────────────┐
│         POSTGRESQL DATABASE                │
│  • Conflicts table                         │
│  • Decisions table                         │
│  • Snapshots table                         │
└────────────────────────────────────────────┘
```

---

## 📊 Expected Metrics

### After Running Full Test Suite

**Prometheus Metrics:**
```
# HTTP requests to AI Service
http_requests_total{service="ai-service", endpoint="/api/v1/quality/score"} = 8+
http_requests_total{service="ai-service", endpoint="/api/v1/anomaly/detect"} = 4+
http_requests_total{service="ai-service", endpoint="/api/v1/resolution/predict"} = 2+

# Quality score distribution
quality_score_calculations_total{bucket="90-100"} = 3
quality_score_calculations_total{bucket="60-89"} = 2
quality_score_calculations_total{bucket="0-59"} = 3

# Anomaly detection
anomaly_detections_total{result="normal"} = 4
anomaly_detections_total{result="anomalous"} = 4

# Conflicts detected (from gateway)
conflicts_detected_total{field="PHONE"} = 2+
conflicts_detected_total{field="COORDINATES"} = 1+

# Resolution outcomes
decisions_executed_total{outcome="FLAG_FOR_REVIEW"} = 3+
decisions_executed_total{outcome="AUTO_APPLY"} = 1+
```

**Database State:**
```sql
-- Check conflicts table
SELECT COUNT(*) FROM conflicts;
-- Expected: 5-10 rows

-- Check recent conflicts
SELECT
    location_id,
    field_type,
    source,
    detected_at
FROM conflicts
ORDER BY detected_at DESC
LIMIT 5;
-- Expected: Recent timestamps, various field types
```

**Kafka Topics:**
```bash
# Check message count
docker exec lsg-kafka kafka-run-class.sh kafka.tools.GetOffsetShell \
  --broker-list localhost:9092 \
  --topic location-snapshots
# Expected: 20+ messages
```

---

## 🚨 Common Issues & Solutions

### Issue 1: AI Service Not Responding

**Symptoms:**
```
curl http://localhost:8081/health
# Connection refused
```

**Solution:**
```bash
# Check container status
docker ps | grep ai-service

# View logs
docker logs lsg-ai-service

# Restart
docker-compose -f docker-compose-microservices.yml restart ai-service

# Wait 30 seconds, retry
```

---

### Issue 2: Gateway Can't Reach AI Service

**Symptoms:**
```
[gateway] ERROR: Failed to call AI Service
[gateway] java.net.ConnectException: Connection refused
```

**Solution:**
```bash
# Verify network
docker network inspect lsg-network

# Check AI service from gateway container
docker exec lsg-gateway curl http://ai-service:8081/health

# Check environment variable
docker exec lsg-gateway env | grep AI_SERVICE_URL
# Expected: AI_SERVICE_URL=http://ai-service:8081
```

---

### Issue 3: Kafka Messages Not Consuming

**Symptoms:**
```
# Generate data but nothing happens
curl -X POST "http://localhost:8080/api/v1/simulation/generate?count=10"
# No logs in gateway
```

**Solution:**
```bash
# Check Kafka is running
docker logs lsg-kafka | grep -i "started"

# Check topics exist
docker exec lsg-kafka kafka-topics.sh --list --bootstrap-server localhost:9092
# Expected: location-snapshots

# Check consumer group
docker exec lsg-kafka kafka-consumer-groups.sh \
  --bootstrap-server localhost:9092 \
  --describe --all-groups
```

---

## ✅ Final Checklist

Before marking testing complete:

- [ ] All 7 containers running (docker-compose ps)
- [ ] AI Service health check passes
- [ ] Gateway health check passes
- [ ] PostgreSQL, Kafka, Redis responding
- [ ] Quality scoring returns expected scores
- [ ] Anomaly detection detects null island
- [ ] Resolution prediction returns valid outcomes
- [ ] Data simulation creates Kafka events
- [ ] Conflicts stored in database
- [ ] Prometheus shows metrics
- [ ] Grafana dashboards load
- [ ] Swagger UI accessible for both services
- [ ] End-to-end conflict flow completes
- [ ] No errors in logs

---

**For detailed commands, see: [QUICK_TEST_COMMANDS.md](QUICK_TEST_COMMANDS.md)**

**For comprehensive guide, see: [TESTING_GUIDE.md](TESTING_GUIDE.md)**
