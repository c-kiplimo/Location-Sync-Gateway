# Quick Test Commands - Location Sync Platform

**Copy-paste commands for rapid testing**

---

## 🚀 Startup

```bash
# Full stack
cd C:\Users\HomePC\Desktop\opensource\Backend\location-sync-gateway
docker-compose -f docker-compose-microservices.yml up -d

# Check status
docker-compose -f docker-compose-microservices.yml ps

# Watch logs
docker-compose -f docker-compose-microservices.yml logs -f
```

---

## ✅ Health Checks

```bash
# AI Service
curl http://localhost:8081/health

# Gateway
curl http://localhost:8080/health

# Infrastructure
docker exec lsg-postgres pg_isready -U lsg
docker exec lsg-redis redis-cli PING
docker exec lsg-kafka kafka-topics.sh --list --bootstrap-server localhost:9092
```

---

## 🧪 Python AI Service Tests

### Quality Scoring - High Quality
```bash
curl -X POST "http://localhost:8081/api/v1/quality/score" \
  -H "Content-Type: application/json" \
  -d '{
    "snapshot": {
      "locationId": 1001,
      "name": "Perfect Restaurant & Cafe",
      "coordinates": {"latitude": -1.286389, "longitude": 36.817223},
      "address": {
        "street": "123 Kimathi Street",
        "city": "Nairobi",
        "state": "Nairobi County",
        "postalCode": "00100",
        "country": "Kenya"
      },
      "phone": "+254712345678",
      "openingHours": {
        "Monday": "09:00-18:00",
        "Tuesday": "09:00-18:00",
        "Wednesday": "09:00-18:00"
      },
      "categories": ["Restaurant", "Cafe"],
      "website": "https://perfect-restaurant.com",
      "description": "Excellent service and great food"
    }
  }'
```

**Expected:** Score 85-100, severity "LOW"

---

### Quality Scoring - Low Quality (Null Island)
```bash
curl -X POST "http://localhost:8081/api/v1/quality/score" \
  -H "Content-Type: application/json" \
  -d '{
    "snapshot": {
      "locationId": 1002,
      "name": "Bad Location",
      "coordinates": {"latitude": 0, "longitude": 0}
    }
  }'
```

**Expected:** Score < 60, severity "CRITICAL", null island detected

---

### Anomaly Detection - Normal
```bash
curl -X POST "http://localhost:8081/api/v1/anomaly/detect" \
  -H "Content-Type: application/json" \
  -d '{
    "snapshot": {
      "locationId": 2001,
      "name": "Normal Restaurant",
      "coordinates": {"latitude": -1.286389, "longitude": 36.817223},
      "address": {"street": "123 Test St", "city": "Nairobi", "country": "Kenya"},
      "phone": "+254712345678"
    }
  }'
```

**Expected:** isAnomalous = false, anomalyScore < 0.5

---

### Anomaly Detection - Suspicious (Null Island)
```bash
curl -X POST "http://localhost:8081/api/v1/anomaly/detect" \
  -H "Content-Type: application/json" \
  -d '{
    "snapshot": {
      "locationId": 2002,
      "name": "Suspicious Location",
      "coordinates": {"latitude": 0, "longitude": 0}
    }
  }'
```

**Expected:** isAnomalous = true, anomalyScore > 0.7, reason includes "SUSPICIOUS_NULL_ISLAND"

---

### Resolution Prediction
```bash
curl -X POST "http://localhost:8081/api/v1/resolution/predict" \
  -H "Content-Type: application/json" \
  -d '{
    "conflict": {
      "locationId": 3001,
      "fieldType": "PHONE",
      "source": "GOOGLE_MY_BUSINESS",
      "currentValue": "+254700000000",
      "incomingValue": "+254711111111",
      "qualityScore": 45,
      "anomalyScore": 0.85
    }
  }'
```

**Expected:** predictedOutcome = "FLAG_FOR_REVIEW", riskLevel = "HIGH"

---

## 🔷 Kotlin Gateway Tests

### Generate Simulated Data
```bash
curl -X POST "http://localhost:8080/api/v1/simulation/generate?count=10"
```

**Expected:** 10 events published to Kafka

---

### Generate Conflicts
```bash
curl -X POST "http://localhost:8080/api/v1/simulation/generate-conflicts?count=5"
```

**Expected:** 5 conflict pairs created

---

### Generate Closures
```bash
curl -X POST "http://localhost:8080/api/v1/simulation/generate-closures?count=3"
```

**Expected:** 3 closure events created

---

### Stress Test
```bash
curl -X POST "http://localhost:8080/api/v1/simulation/stress-test?count=100"
```

**Expected:** 100 messages processed, throughput reported

---

## 📊 Monitoring

### Prometheus
```
http://localhost:9090
```

**Key Queries:**
```promql
# Request rate
rate(http_requests_total[5m])

# Conflicts detected
conflicts_detected_total

# Error rate
rate(http_requests_total{status=~"5.."}[5m])
```

---

### Grafana
```
http://localhost:3000
Username: admin
Password: admin
```

---

### API Documentation

**AI Service Swagger:**
```
http://localhost:8081/docs
```

**Gateway Swagger:**
```
http://localhost:8080/swagger-ui
```

---

## 🔍 Debugging

### View Logs
```bash
# All services
docker-compose -f docker-compose-microservices.yml logs -f

# Specific service
docker-compose -f docker-compose-microservices.yml logs -f gateway
docker-compose -f docker-compose-microservices.yml logs -f ai-service

# Search logs
docker-compose -f docker-compose-microservices.yml logs | grep -i "error\|exception"
```

---

### Check Kafka Messages
```bash
# List topics
docker exec lsg-kafka kafka-topics.sh --list --bootstrap-server localhost:9092

# Consume messages
docker exec lsg-kafka kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic location-snapshots \
  --from-beginning \
  --max-messages 10
```

---

### Database Queries
```bash
# Connect to PostgreSQL
docker exec -it lsg-postgres psql -U lsg -d location_sync_gateway

# Inside psql:
SELECT COUNT(*) FROM conflicts;
SELECT * FROM conflicts ORDER BY detected_at DESC LIMIT 5;
\dt  -- List tables
\q   -- Quit
```

---

### Redis Inspection
```bash
# Check keys
docker exec lsg-redis redis-cli KEYS "*"

# Get cache stats
docker exec lsg-redis redis-cli INFO stats

# Monitor commands
docker exec lsg-redis redis-cli MONITOR
```

---

## 🛑 Shutdown

```bash
# Stop all services
docker-compose -f docker-compose-microservices.yml down

# Stop and remove volumes (clean slate)
docker-compose -f docker-compose-microservices.yml down -v

# View running containers
docker ps
```

---

## 🔄 Restart

```bash
# Restart specific service
docker-compose -f docker-compose-microservices.yml restart gateway
docker-compose -f docker-compose-microservices.yml restart ai-service

# Rebuild and restart
docker-compose -f docker-compose-microservices.yml up -d --build ai-service

# Full restart
docker-compose -f docker-compose-microservices.yml restart
```

---

## 📱 Windows Quick Test (PowerShell)

```powershell
# Test AI Service
Invoke-RestMethod -Uri "http://localhost:8081/health" -Method Get

# Test Gateway
Invoke-RestMethod -Uri "http://localhost:8080/health" -Method Get

# Generate data
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/simulation/generate?count=5" -Method Post
```

---

## 🎯 One-Command Full Test

**Linux/Mac:**
```bash
#!/bin/bash
echo "Testing AI Service..." && \
curl -s http://localhost:8081/health | grep -q "healthy" && echo "✅ AI Service UP" && \
echo "Testing Gateway..." && \
curl -s http://localhost:8080/health | grep -q "UP" && echo "✅ Gateway UP" && \
echo "Testing Quality Scoring..." && \
curl -s -X POST http://localhost:8081/api/v1/quality/score -H "Content-Type: application/json" -d '{"snapshot":{"locationId":1,"coordinates":{"latitude":-1.286389,"longitude":36.817223}}}' | grep -q "overallScore" && echo "✅ Quality Scoring WORKS" && \
echo "Generating Test Data..." && \
curl -s -X POST "http://localhost:8080/api/v1/simulation/generate?count=5" | grep -q "generated" && echo "✅ Data Generation WORKS" && \
echo -e "\n🎉 All tests passed!"
```

**Windows (Command Prompt):**
```batch
curl http://localhost:8081/health && curl http://localhost:8080/health && curl -X POST "http://localhost:8080/api/v1/simulation/generate?count=5"
```

---

## 📋 Quick Checklist

- [ ] All containers running (`docker-compose ps`)
- [ ] AI Service health check passes
- [ ] Gateway health check passes
- [ ] Quality scoring returns 0-100 score
- [ ] Anomaly detection detects null island
- [ ] Data simulation creates Kafka events
- [ ] Prometheus shows metrics
- [ ] Swagger docs accessible
- [ ] No errors in logs

---

**For detailed explanations, see:** [TESTING_GUIDE.md](TESTING_GUIDE.md)
