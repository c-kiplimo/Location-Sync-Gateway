# Deployment Workflow - Complete Guide

**Step-by-step deployment workflow for Location Sync Platform on your own server**

---

## 📋 Table of Contents

1. [Deployment Overview](#deployment-overview)
2. [Pre-Deployment Checklist](#pre-deployment-checklist)
3. [Deployment Workflow - Docker](#deployment-workflow---docker)
4. [Deployment Workflow - Kubernetes](#deployment-workflow---kubernetes)
5. [Post-Deployment Verification](#post-deployment-verification)
6. [Rollback Procedures](#rollback-procedures)
7. [Scaling Guide](#scaling-guide)
8. [Maintenance Procedures](#maintenance-procedures)

---

## Deployment Overview

### Architecture Deployment Layers

```
┌────────────────────────────────────────────────────────────────┐
│                      DEPLOYMENT LAYERS                          │
└────────────────────────────────────────────────────────────────┘

Layer 1: Infrastructure (Your Server)
├─ Physical/Virtual Server
├─ Operating System (Ubuntu/Debian)
├─ Docker Engine
└─ Kubernetes (k3s/microk8s/kubeadm)

Layer 2: Cluster Services
├─ Container Runtime
├─ Network Plugin (CNI)
├─ Storage Plugin (CSI)
└─ Ingress Controller

Layer 3: Data Layer
├─ PostgreSQL (Stateful)
├─ Kafka + Zookeeper (Stateful)
├─ Redis (Stateful)
└─ Persistent Volumes

Layer 4: Application Layer
├─ Python AI Service (Stateless)
├─ Kotlin Gateway Service (Stateless)
└─ Nginx Reverse Proxy

Layer 5: Monitoring Layer
├─ Prometheus (Metrics)
├─ Grafana (Dashboards)
└─ Logging (Optional)

Layer 6: Ingress Layer
├─ SSL/TLS Termination
├─ Load Balancing
└─ Rate Limiting
```

---

## Pre-Deployment Checklist

### ✅ Before You Deploy

**Server Preparation:**
- [ ] Server has minimum requirements (4 CPU, 8GB RAM, 50GB disk)
- [ ] Operating system is updated (`sudo apt update && sudo apt upgrade`)
- [ ] Firewall is configured (ports 22, 80, 443, 8080, 8081 open)
- [ ] Docker is installed and running (`docker --version`)
- [ ] Kubernetes is installed (if using K8s) (`kubectl version`)
- [ ] Domain name is configured (DNS A records point to server IP)

**Code Preparation:**
- [ ] Code is cloned from GitHub
- [ ] All configuration files are reviewed
- [ ] Environment variables are set
- [ ] Secrets are generated (database passwords, API keys)
- [ ] SSL certificates are obtained (Let's Encrypt or self-signed)

**Backup Preparation:**
- [ ] Backup strategy is defined
- [ ] Backup directory is created
- [ ] Database backup script is tested
- [ ] Recovery procedure is documented

---

## Deployment Workflow - Docker

### Complete Step-by-Step Docker Deployment

```
┌─────────────────────────────────────────────────────────────────┐
│                  DOCKER DEPLOYMENT WORKFLOW                      │
└─────────────────────────────────────────────────────────────────┘

Step 1: Server Preparation
     ↓
Step 2: Clone Repository
     ↓
Step 3: Configure Environment
     ↓
Step 4: Generate Secrets
     ↓
Step 5: Build Docker Images
     ↓
Step 6: Start Infrastructure Services
     ↓
Step 7: Run Database Migrations
     ↓
Step 8: Start Application Services
     ↓
Step 9: Configure Reverse Proxy
     ↓
Step 10: SSL Setup
     ↓
Step 11: Start Monitoring
     ↓
Step 12: Verification Tests
     ↓
Step 13: Production Go-Live
```

---

### Step 1: Server Preparation (15 minutes)

**Purpose:** Prepare your server with all necessary software

```bash
# 1.1 Update system
sudo apt update && sudo apt upgrade -y

# 1.2 Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh
sudo systemctl enable docker
sudo systemctl start docker

# 1.3 Install Docker Compose
sudo curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" \
  -o /usr/local/bin/docker-compose
sudo chmod +x /usr/local/bin/docker-compose

# 1.4 Verify installations
docker --version
docker-compose --version

# 1.5 Create deployment user
sudo useradd -m -s /bin/bash deploy
sudo usermod -aG docker deploy
sudo usermod -aG sudo deploy

# 1.6 Configure firewall
sudo ufw allow 22/tcp    # SSH
sudo ufw allow 80/tcp    # HTTP
sudo ufw allow 443/tcp   # HTTPS
sudo ufw enable
```

**Expected Result:** Docker and Docker Compose installed, firewall configured

---

### Step 2: Clone Repository (5 minutes)

**Purpose:** Get the application code on your server

```bash
# 2.1 Switch to deployment user
sudo su - deploy

# 2.2 Clone repository to /opt
cd /opt
sudo git clone https://github.com/c-kiplimo/Location-Sync-Gateway.git
sudo chown -R deploy:deploy Location-Sync-Gateway

# 2.3 Navigate to project
cd Location-Sync-Gateway/location-sync-gateway

# 2.4 Verify structure
ls -la
```

**Expected Result:** Repository cloned, permissions set correctly

---

### Step 3: Configure Environment (10 minutes)

**Purpose:** Set all environment variables and configuration

```bash
# 3.1 Create production environment file
cat > .env.production << 'EOF'
# ============================================================
# PRODUCTION ENVIRONMENT CONFIGURATION
# Location Sync Platform
# ============================================================

# DATABASE CONFIGURATION
POSTGRES_DB=location_sync_gateway
POSTGRES_USER=lsg_prod
POSTGRES_PASSWORD=CHANGE_THIS_STRONG_PASSWORD_12345!@#
POSTGRES_HOST=postgres
POSTGRES_PORT=5432

# KAFKA CONFIGURATION
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_TOPIC_SNAPSHOTS=location-snapshots
KAFKA_REPLICATION_FACTOR=1

# REDIS CONFIGURATION
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=CHANGE_THIS_REDIS_PASSWORD_67890!@#

# AI SERVICE CONFIGURATION
AI_SERVICE_URL=http://ai-service:8081
AI_SERVICE_TIMEOUT=30000

# UBERALL API CONFIGURATION (if integrating)
UBERALL_API_URL=https://uberall.com/api/v1
UBERALL_API_KEY=YOUR_UBERALL_API_KEY_HERE

# APPLICATION CONFIGURATION
MICRONAUT_ENVIRONMENTS=prod
LOG_LEVEL=INFO
JAVA_OPTS=-Xmx2G -Xms1G

# MONITORING CONFIGURATION
PROMETHEUS_ENABLED=true
METRICS_EXPORT_ENABLED=true
GRAFANA_PASSWORD=CHANGE_THIS_GRAFANA_PASSWORD!@#

# DOMAIN CONFIGURATION
DOMAIN=your-domain.com
API_DOMAIN=api.your-domain.com
AI_DOMAIN=ai.your-domain.com
GRAFANA_DOMAIN=grafana.your-domain.com

# SECURITY
ALLOWED_ORIGINS=https://your-domain.com,https://api.your-domain.com
ENABLE_CORS=true
EOF

# 3.2 Secure the file (important!)
chmod 600 .env.production

# 3.3 Edit the file with your actual values
nano .env.production
# Change ALL passwords, API keys, and domain names

# 3.4 Verify configuration
cat .env.production | grep -v PASSWORD | grep -v KEY
```

**Expected Result:** `.env.production` file created with your settings

**Important:** Replace ALL placeholder values with your actual:
- Database passwords (use strong passwords!)
- Redis password
- Grafana password
- API keys
- Domain names

---

### Step 4: Generate Secrets (5 minutes)

**Purpose:** Generate secure passwords and API keys

```bash
# 4.1 Generate strong passwords
generate_password() {
  openssl rand -base64 32
}

# 4.2 Create secrets file
cat > secrets.txt << EOF
PostgreSQL Password: $(generate_password)
Redis Password: $(generate_password)
Grafana Password: $(generate_password)
JWT Secret: $(generate_password)
EOF

# 4.3 Display (save these securely!)
cat secrets.txt

# 4.4 Update .env.production with generated passwords
# Use a text editor or sed commands

# 4.5 Secure secrets file
chmod 400 secrets.txt
```

**Expected Result:** Secure passwords generated, saved to secrets.txt

**Important:** Save secrets.txt to a password manager or secure location!

---

### Step 5: Build Docker Images (10 minutes)

**Purpose:** Build custom Docker images for both services

```bash
# 5.1 Build Kotlin Gateway image
docker build -t location-sync-gateway:latest .

# 5.2 Build Python AI Service image
cd ../location-sync-ai-service
docker build -t location-sync-ai-service:latest .

# 5.3 Verify images
docker images | grep location-sync

# 5.4 Tag images for versioning
docker tag location-sync-gateway:latest location-sync-gateway:v1.0.0
docker tag location-sync-ai-service:latest location-sync-ai-service:v1.0.0

# 5.5 Return to gateway directory
cd ../location-sync-gateway
```

**Expected Result:** Two Docker images built successfully

---

### Step 6: Start Infrastructure Services (5 minutes)

**Purpose:** Start database, Kafka, Redis (data layer)

```bash
# 6.1 Start infrastructure only
docker-compose -f docker-compose.production.yml --env-file .env.production \
  up -d postgres redis zookeeper kafka

# 6.2 Wait for services to be healthy (60 seconds)
echo "Waiting for infrastructure to start..."
sleep 60

# 6.3 Check status
docker-compose -f docker-compose.production.yml ps

# 6.4 Verify health
docker exec lsg-postgres-prod pg_isready -U lsg_prod
docker exec lsg-redis-prod redis-cli ping
docker exec lsg-kafka-prod kafka-broker-api-versions --bootstrap-server localhost:9092 | head -1
```

**Expected Result:** PostgreSQL, Redis, Kafka all healthy

---

### Step 7: Run Database Migrations (5 minutes)

**Purpose:** Create database schema

```bash
# 7.1 Run Flyway migrations
docker-compose -f docker-compose.production.yml \
  run --rm gateway ./gradlew flywayMigrate

# 7.2 Verify tables created
docker exec -it lsg-postgres-prod psql -U lsg_prod -d location_sync_gateway -c "\dt"

# 7.3 Check migration history
docker exec -it lsg-postgres-prod psql -U lsg_prod -d location_sync_gateway \
  -c "SELECT * FROM flyway_schema_history;"
```

**Expected Result:** Database tables created successfully

---

### Step 8: Start Application Services (5 minutes)

**Purpose:** Start AI Service and Gateway

```bash
# 8.1 Start AI service first
docker-compose -f docker-compose.production.yml --env-file .env.production \
  up -d ai-service

# 8.2 Wait for AI service health check (30 seconds)
echo "Waiting for AI service..."
sleep 30

# 8.3 Start Gateway service
docker-compose -f docker-compose.production.yml --env-file .env.production \
  up -d gateway

# 8.4 Wait for Gateway health check (30 seconds)
echo "Waiting for Gateway..."
sleep 30

# 8.5 Check service health
curl http://localhost:8081/health
curl http://localhost:8080/health
```

**Expected Result:** Both services healthy and responding

---

### Step 9: Configure Reverse Proxy (10 minutes)

**Purpose:** Set up Nginx for SSL termination and routing

```bash
# 9.1 Create nginx directory
mkdir -p nginx/ssl

# 9.2 Create nginx configuration (see DEPLOYMENT_GUIDE.md for full config)
cat > nginx/nginx.conf << 'EOF'
# Full nginx configuration here (from DEPLOYMENT_GUIDE.md)
EOF

# 9.3 Start nginx
docker-compose -f docker-compose.production.yml --env-file .env.production \
  up -d nginx

# 9.4 Check nginx logs
docker logs lsg-nginx-prod
```

**Expected Result:** Nginx running, routing to services

---

### Step 10: SSL Setup (15 minutes)

**Purpose:** Configure HTTPS with Let's Encrypt

**Option A: Let's Encrypt (Recommended for production)**
```bash
# 10.1 Install certbot
sudo apt install -y certbot

# 10.2 Stop nginx temporarily
docker-compose -f docker-compose.production.yml stop nginx

# 10.3 Generate certificates
sudo certbot certonly --standalone \
  -d your-domain.com \
  -d api.your-domain.com \
  -d ai.your-domain.com \
  -d grafana.your-domain.com \
  --email your-email@example.com \
  --agree-tos \
  --no-eff-email

# 10.4 Copy certificates to nginx directory
sudo cp /etc/letsencrypt/live/your-domain.com/fullchain.pem nginx/ssl/
sudo cp /etc/letsencrypt/live/your-domain.com/privkey.pem nginx/ssl/
sudo chown deploy:deploy nginx/ssl/*

# 10.5 Restart nginx
docker-compose -f docker-compose.production.yml start nginx

# 10.6 Setup auto-renewal
echo "0 0 * * * certbot renew --quiet --deploy-hook 'docker-compose -f /opt/Location-Sync-Gateway/location-sync-gateway/docker-compose.production.yml restart nginx'" | sudo crontab -
```

**Option B: Self-Signed (Development/Testing)**
```bash
# 10.1 Generate self-signed certificate
sudo openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
  -keyout nginx/ssl/privkey.pem \
  -out nginx/ssl/fullchain.pem \
  -subj "/C=US/ST=State/L=City/O=Organization/CN=your-domain.com"

# 10.2 Restart nginx
docker-compose -f docker-compose.production.yml restart nginx
```

**Expected Result:** HTTPS working on all domains

---

### Step 11: Start Monitoring (5 minutes)

**Purpose:** Enable Prometheus and Grafana

```bash
# 11.1 Start monitoring services
docker-compose -f docker-compose.production.yml --env-file .env.production \
  up -d prometheus grafana

# 11.2 Wait for startup
sleep 30

# 11.3 Access Grafana
# Open browser: https://grafana.your-domain.com
# Login: admin / [GRAFANA_PASSWORD from .env]

# 11.4 Verify Prometheus
# Open browser: https://prometheus.your-domain.com
# Run query: up
```

**Expected Result:** Grafana and Prometheus accessible, showing metrics

---

### Step 12: Verification Tests (15 minutes)

**Purpose:** Verify entire system is working

```bash
# 12.1 Health checks
curl https://api.your-domain.com/health
curl https://ai.your-domain.com/health

# 12.2 Test AI Service
curl -X POST https://ai.your-domain.com/api/v1/quality/score \
  -H "Content-Type: application/json" \
  -d '{
    "snapshot": {
      "locationId": 1001,
      "name": "Test Restaurant",
      "coordinates": {"latitude": -1.286389, "longitude": 36.817223}
    }
  }'

# 12.3 Test Gateway
curl -X POST https://api.your-domain.com/api/v1/simulation/generate?count=5

# 12.4 Check database
docker exec -it lsg-postgres-prod psql -U lsg_prod -d location_sync_gateway \
  -c "SELECT COUNT(*) FROM conflicts;"

# 12.5 Check Kafka messages
docker exec lsg-kafka-prod kafka-console-consumer.sh \
  --bootstrap-server localhost:9092 \
  --topic location-snapshots \
  --from-beginning \
  --max-messages 5

# 12.6 Check logs (no errors)
docker-compose -f docker-compose.production.yml logs --tail=100 | grep -i error

# 12.7 Monitor resource usage
docker stats --no-stream
```

**Expected Result:** All tests pass, no errors in logs

---

### Step 13: Production Go-Live (5 minutes)

**Purpose:** Final checklist before declaring system live

**Final Checklist:**
```bash
# ✅ All services healthy
docker-compose -f docker-compose.production.yml ps

# ✅ HTTPS working
curl -I https://api.your-domain.com

# ✅ Monitoring operational
# Visit https://grafana.your-domain.com

# ✅ Backups configured
ls -la backups/

# ✅ Logs rotating properly
docker-compose -f docker-compose.production.yml logs | wc -l

# ✅ Resource usage acceptable
docker stats --no-stream

# ✅ Security hardening applied
# - Passwords changed from defaults
# - Firewall enabled
# - SSL certificates installed
# - Unnecessary ports closed
```

**When all checks pass:** System is production-ready! 🎉

---

## Deployment Workflow - Kubernetes

### Complete Step-by-Step Kubernetes Deployment

```
┌─────────────────────────────────────────────────────────────────┐
│               KUBERNETES DEPLOYMENT WORKFLOW                     │
└─────────────────────────────────────────────────────────────────┘

Step 1: Install Kubernetes (k3s/microk8s)
     ↓
Step 2: Create Namespace
     ↓
Step 3: Create Secrets
     ↓
Step 4: Create ConfigMaps
     ↓
Step 5: Deploy Persistent Volumes
     ↓
Step 6: Deploy StatefulSets (DB, Kafka, Redis)
     ↓
Step 7: Deploy Applications (AI Service, Gateway)
     ↓
Step 8: Deploy Services (ClusterIP, LoadBalancer)
     ↓
Step 9: Deploy Ingress Controller
     ↓
Step 10: Configure Ingress Rules
     ↓
Step 11: Deploy Monitoring (Prometheus, Grafana)
     ↓
Step 12: Configure Auto-Scaling
     ↓
Step 13: Verification Tests
     ↓
Step 14: Production Go-Live
```

---

### Step 1: Install Kubernetes (30 minutes)

**Purpose:** Set up Kubernetes cluster on your server

**Option A: k3s (Recommended - Lightweight)**
```bash
# 1.1 Install k3s
curl -sfL https://get.k3s.io | sh -

# 1.2 Verify installation
sudo k3s kubectl get nodes

# 1.3 Configure kubectl for current user
mkdir -p ~/.kube
sudo cp /etc/rancher/k3s/k3s.yaml ~/.kube/config
sudo chown $USER:$USER ~/.kube/config
export KUBECONFIG=~/.kube/config
echo "export KUBECONFIG=~/.kube/config" >> ~/.bashrc

# 1.4 Verify
kubectl get nodes
kubectl cluster-info
```

**Expected Result:** Kubernetes cluster running, kubectl configured

---

### Step 2: Create Namespace (2 minutes)

**Purpose:** Isolate resources in dedicated namespace

```bash
# 2.1 Create namespace
kubectl create namespace location-sync

# 2.2 Set as default
kubectl config set-context --current --namespace=location-sync

# 2.3 Verify
kubectl get namespaces
kubectl config get-contexts
```

**Expected Result:** Namespace created and set as default

---

### Step 3: Create Secrets (5 minutes)

**Purpose:** Store sensitive data securely

```bash
# 3.1 Create database secret
kubectl create secret generic postgres-secret \
  --from-literal=POSTGRES_USER=lsg_prod \
  --from-literal=POSTGRES_PASSWORD='YOUR_STRONG_PASSWORD_HERE' \
  --from-literal=POSTGRES_DB=location_sync_gateway

# 3.2 Create Redis secret
kubectl create secret generic redis-secret \
  --from-literal=REDIS_PASSWORD='YOUR_REDIS_PASSWORD_HERE'

# 3.3 Create API keys secret
kubectl create secret generic api-keys \
  --from-literal=UBERALL_API_KEY='YOUR_API_KEY_HERE'

# 3.4 Create Grafana secret
kubectl create secret generic grafana-secret \
  --from-literal=GF_SECURITY_ADMIN_PASSWORD='YOUR_GRAFANA_PASSWORD_HERE'

# 3.5 Verify secrets created
kubectl get secrets
kubectl describe secret postgres-secret
```

**Expected Result:** All secrets created securely

---

### Step 4: Apply Kubernetes Manifests (10 minutes)

**Purpose:** Deploy all Kubernetes resources

```bash
# 4.1 Navigate to k8s directory
cd k8s/base

# 4.2 Apply in order (data layer first)
kubectl apply -f namespace.yaml
kubectl apply -f configmaps.yaml
kubectl apply -f persistent-volumes.yaml
kubectl apply -f statefulsets/  # PostgreSQL, Kafka, Redis

# 4.3 Wait for stateful sets to be ready (may take 2-3 minutes)
kubectl wait --for=condition=ready pod -l app=postgres --timeout=300s
kubectl wait --for=condition=ready pod -l app=kafka --timeout=300s
kubectl wait --for=condition=ready pod -l app=redis --timeout=300s

# 4.4 Deploy application layer
kubectl apply -f deployments/  # AI Service, Gateway

# 4.5 Wait for deployments
kubectl wait --for=condition=available deployment/ai-service --timeout=300s
kubectl wait --for=condition=available deployment/gateway --timeout=300s

# 4.6 Deploy services
kubectl apply -f services/

# 4.7 Deploy ingress
kubectl apply -f ingress.yaml

# 4.8 Deploy monitoring
kubectl apply -f monitoring/

# 4.9 Verify all resources
kubectl get all
```

**Expected Result:** All pods running, services created

---

### (Continuing in next steps...)

**Total Deployment Time:**
- Docker: ~1.5 hours (first time)
- Kubernetes: ~2 hours (first time)
- Subsequent deployments: ~15 minutes

---

## Post-Deployment Verification

### Comprehensive System Check

```bash
# 1. Service Health
curl https://api.your-domain.com/health
curl https://ai.your-domain.com/health

# 2. API Functionality
curl -X POST https://ai.your-domain.com/api/v1/quality/score \
  -H "Content-Type: application/json" \
  -d '{"snapshot":{"locationId":1001,"coordinates":{"latitude":-1.286389,"longitude":36.817223}}}'

# 3. Database Connectivity
# Docker: docker exec -it lsg-postgres-prod psql -U lsg_prod -c "SELECT version();"
# K8s: kubectl exec -it postgres-0 -- psql -U lsg_prod -c "SELECT version();"

# 4. Kafka Connectivity
# Docker: docker exec lsg-kafka-prod kafka-topics.sh --list --bootstrap-server localhost:9092
# K8s: kubectl exec -it kafka-0 -- kafka-topics.sh --list --bootstrap-server localhost:9092

# 5. Monitoring
# Visit: https://grafana.your-domain.com
# Check dashboards are loading with data

# 6. SSL Certificates
curl -vI https://api.your-domain.com 2>&1 | grep -A 5 "SSL certificate"

# 7. Resource Usage
# Docker: docker stats
# K8s: kubectl top nodes && kubectl top pods

# 8. Logs (check for errors)
# Docker: docker-compose logs --tail=100
# K8s: kubectl logs -l app=gateway --tail=100
```

---

## Rollback Procedures

### Docker Rollback

```bash
# If deployment fails, rollback to previous version

# 1. Stop new services
docker-compose -f docker-compose.production.yml down

# 2. Restore previous database backup
./scripts/restore-backup.sh backup-YYYY-MM-DD-HH-MM.sql

# 3. Revert to previous image tag
docker-compose -f docker-compose.production.yml pull gateway:v0.9.0
docker-compose -f docker-compose.production.yml pull ai-service:v0.9.0

# 4. Start with previous version
docker-compose -f docker-compose.production.yml up -d

# 5. Verify
curl https://api.your-domain.com/health
```

### Kubernetes Rollback

```bash
# View deployment history
kubectl rollout history deployment/gateway

# Rollback to previous version
kubectl rollout undo deployment/gateway
kubectl rollout undo deployment/ai-service

# Rollback to specific revision
kubectl rollout undo deployment/gateway --to-revision=2

# Check rollback status
kubectl rollout status deployment/gateway
```

---

## Scaling Guide

### Docker Scaling

```bash
# Scale AI service (3 replicas)
docker-compose -f docker-compose.production.yml up -d --scale ai-service=3

# Scale Gateway (5 replicas)
docker-compose -f docker-compose.production.yml up -d --scale gateway=5
```

### Kubernetes Scaling

```bash
# Manual scaling
kubectl scale deployment ai-service --replicas=3
kubectl scale deployment gateway --replicas=5

# Auto-scaling (HPA)
kubectl autoscale deployment ai-service --min=2 --max=10 --cpu-percent=70
kubectl autoscale deployment gateway --min=3 --max=15 --cpu-percent=70

# Check auto-scaling status
kubectl get hpa
```

---

## Maintenance Procedures

### Regular Maintenance Tasks

**Daily:**
- [ ] Check service health
- [ ] Review error logs
- [ ] Monitor resource usage

**Weekly:**
- [ ] Database backup
- [ ] Review Grafana dashboards
- [ ] Check disk space
- [ ] Review security alerts

**Monthly:**
- [ ] Update dependencies
- [ ] Rotate SSL certificates (if needed)
- [ ] Review and optimize database queries
- [ ] Capacity planning review

---

**For detailed Kubernetes manifests and file purposes, see:** [K8S_FILES_EXPLAINED.md](K8S_FILES_EXPLAINED.md)

**For complete deployment guide, see:** [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md)
