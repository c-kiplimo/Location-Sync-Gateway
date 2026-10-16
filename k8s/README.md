# Kubernetes Deployment Guide

This directory contains production-grade Kubernetes manifests for deploying the Location Sync Gateway application and its dependencies.

## 📋 Table of Contents

- [Overview](#overview)
- [Prerequisites](#prerequisites)
- [Architecture](#architecture)
- [Quick Start](#quick-start)
- [Manifest Files](#manifest-files)
- [Deployment Steps](#deployment-steps)
- [Configuration](#configuration)
- [Monitoring](#monitoring)
- [Scaling](#scaling)
- [Security](#security)
- [Troubleshooting](#troubleshooting)
- [Production Deployment](#production-deployment)

## 🎯 Overview

The Kubernetes deployment includes:

- **Application**: Location Sync Gateway (3+ replicas with HPA)
- **Database**: PostgreSQL 15 StatefulSet
- **Message Broker**: Kafka 3-node cluster with ZooKeeper
- **Cache**: Redis with Sentinel for HA
- **Networking**: Ingress with TLS, Network Policies
- **Monitoring**: Prometheus metrics, Grafana dashboards, alerts
- **Security**: RBAC, Pod Security Policies, Network Policies

## ✅ Prerequisites

### Required Tools

```bash
# kubectl (v1.27+)
kubectl version --client

# kustomize (v5.0+)
kustomize version

# helm (v3.12+) - optional
helm version

# k9s (optional, for cluster management)
k9s version
```

### Kubernetes Cluster

**Development/Testing:**
- Minikube, Kind, or Docker Desktop Kubernetes
- Minimum: 4 CPU cores, 8GB RAM

**Production:**
- Amazon EKS (recommended)
- Google GKE
- Azure AKS
- Self-managed Kubernetes 1.27+

**Required Add-ons:**
- Ingress controller (NGINX or AWS ALB)
- Metrics Server (for HPA)
- Prometheus Operator (for monitoring)
- CSI driver (for persistent volumes)

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Ingress Controller                       │
│              (api.locationsync.example.com)                  │
└──────────────────────┬──────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│              Location Sync Gateway Service                   │
│                  (ClusterIP: 80 → 8080)                      │
└──────────────────────┬──────────────────────────────────────┘
                       │
       ┌───────────────┼───────────────┐
       ▼               ▼               ▼
┌──────────┐    ┌──────────┐    ┌──────────┐
│  Gateway │    │  Gateway │    │  Gateway │
│  Pod 1   │    │  Pod 2   │    │  Pod 3   │
└─────┬────┘    └─────┬────┘    └─────┬────┘
      │               │               │
      └───────┬───────┴───────┬───────┘
              │               │
      ┌───────▼───────┐ ┌────▼──────┐ ┌──────────┐
      │   PostgreSQL  │ │   Kafka   │ │  Redis   │
      │  StatefulSet  │ │  Cluster  │ │  +       │
      │  (1 primary)  │ │ (3 nodes) │ │ Sentinel │
      └───────────────┘ └───────────┘ └──────────┘
```

## 🚀 Quick Start

### 1. Deploy to Minikube (Development)

```bash
# Start Minikube with sufficient resources
minikube start --cpus=4 --memory=8192 --kubernetes-version=v1.27.0

# Enable required addons
minikube addons enable ingress
minikube addons enable metrics-server

# Create namespace
kubectl apply -f namespace.yaml

# Create secrets (update values first!)
kubectl apply -f secret.yaml

# Deploy all resources
kubectl apply -f .

# Wait for pods to be ready
kubectl wait --for=condition=ready pod -l app=location-sync-gateway -n location-sync-gateway --timeout=300s

# Port forward to access locally
kubectl port-forward -n location-sync-gateway svc/location-sync-gateway 8080:80

# Test the application
curl http://localhost:8080/health
```

### 2. Deploy with Kustomize

```bash
# Deploy all resources
kubectl apply -k .

# Verify deployment
kubectl get all -n location-sync-gateway

# Check pod status
kubectl get pods -n location-sync-gateway -w
```

## 📁 Manifest Files

| File | Description |
|------|-------------|
| `namespace.yaml` | Namespace with ResourceQuota and LimitRange |
| `deployment.yaml` | Application deployment with 3 replicas, health checks, anti-affinity |
| `service.yaml` | ClusterIP and headless services |
| `configmap.yaml` | Application configuration, DB/Kafka/Redis settings, resolution rules |
| `secret.yaml` | Template for sensitive credentials (DO NOT commit real values) |
| `ingress.yaml` | Ingress with TLS for external access (ALB + NGINX examples) |
| `hpa.yaml` | HorizontalPodAutoscaler (3-12 replicas based on CPU/memory/Kafka lag) |
| `rbac.yaml` | ServiceAccount, Role, RoleBinding with AWS IRSA support |
| `postgres-statefulset.yaml` | PostgreSQL 15 with persistent storage, metrics exporter |
| `kafka-statefulset.yaml` | Kafka 3-node cluster with ZooKeeper, JMX metrics |
| `redis-deployment.yaml` | Redis with Sentinel for HA, persistence enabled |
| `network-policy.yaml` | Network policies for zero-trust security |
| `poddisruptionbudget.yaml` | PDBs to ensure availability during updates |
| `servicemonitor.yaml` | Prometheus ServiceMonitors and alerting rules |
| `kustomization.yaml` | Kustomize configuration for multi-environment deployment |

## 📦 Deployment Steps

### Step 1: Prepare Secrets

```bash
# Copy secret template
cp secret.yaml secret-values.yaml

# Edit secret-values.yaml and replace placeholders:
# - REPLACE_WITH_ACTUAL_PASSWORD
# - REPLACE_WITH_ACTUAL_CLIENT_ID
# - REPLACE_WITH_ACTUAL_API_KEY
# etc.

# Apply secrets
kubectl apply -f secret-values.yaml

# IMPORTANT: DO NOT commit secret-values.yaml to Git!
# Add to .gitignore
echo "k8s/secret-values.yaml" >> .gitignore
```

### Step 2: Update ConfigMap

```bash
# Edit configmap.yaml if needed
# Update database host, Kafka servers, Redis host for your environment
kubectl apply -f configmap.yaml
```

### Step 3: Deploy Dependencies

```bash
# Deploy PostgreSQL
kubectl apply -f postgres-statefulset.yaml

# Wait for PostgreSQL to be ready
kubectl wait --for=condition=ready pod -l app=postgres -n location-sync-gateway --timeout=300s

# Deploy Kafka and ZooKeeper
kubectl apply -f kafka-statefulset.yaml

# Wait for Kafka to be ready
kubectl wait --for=condition=ready pod -l app=kafka -n location-sync-gateway --timeout=600s

# Deploy Redis
kubectl apply -f redis-deployment.yaml

# Wait for Redis to be ready
kubectl wait --for=condition=ready pod -l app=redis -n location-sync-gateway --timeout=180s
```

### Step 4: Deploy Application

```bash
# Deploy RBAC
kubectl apply -f rbac.yaml

# Deploy application
kubectl apply -f deployment.yaml
kubectl apply -f service.yaml

# Deploy HPA
kubectl apply -f hpa.yaml

# Deploy PodDisruptionBudgets
kubectl apply -f poddisruptionbudget.yaml
```

### Step 5: Deploy Networking

```bash
# Deploy network policies
kubectl apply -f network-policy.yaml

# Deploy ingress (update hostnames first!)
kubectl apply -f ingress.yaml
```

### Step 6: Deploy Monitoring

```bash
# Deploy ServiceMonitors (requires Prometheus Operator)
kubectl apply -f servicemonitor.yaml

# Verify metrics are being scraped
kubectl get servicemonitor -n location-sync-gateway
```

## ⚙️ Configuration

### Environment Variables

Set via ConfigMap or Deployment environment variables:

```yaml
env:
- name: ENVIRONMENT
  value: "production"
- name: LOG_LEVEL
  value: "INFO"
- name: DB_HOST
  valueFrom:
    configMapKeyRef:
      name: location-sync-gateway-config
      key: db.host
- name: DB_PASSWORD
  valueFrom:
    secretKeyRef:
      name: location-sync-gateway-secrets
      key: db-password
```

### Resource Requests/Limits

Default values (adjust based on load testing):

```yaml
resources:
  requests:
    memory: "512Mi"
    cpu: "500m"
  limits:
    memory: "1Gi"
    cpu: "1000m"
```

### Persistent Storage

**Storage Classes:**
- Development: `standard` (hostPath)
- AWS EKS: `gp3` (EBS GP3)
- GKE: `pd-ssd` (Persistent Disk SSD)
- AKS: `managed-premium` (Premium SSD)

**Volume Sizes:**
- PostgreSQL: 20Gi (adjust based on data growth)
- Kafka: 50Gi per broker
- Redis: 10Gi
- ZooKeeper: 10Gi data + 5Gi logs

## 📊 Monitoring

### Prometheus Metrics

Application exposes metrics at `/prometheus`:

```bash
# Port forward to view metrics
kubectl port-forward -n location-sync-gateway svc/location-sync-gateway 8080:80

curl http://localhost:8080/prometheus
```

**Key Metrics:**
- `http_server_requests_seconds_count` - Request count
- `http_server_requests_seconds_sum` - Total request duration
- `conflicts_detected_total` - Total conflicts detected
- `conflicts_auto_resolved_total` - Auto-resolved conflicts
- `quality_score` - Quality score distribution
- `kafka_consumer_lag` - Consumer lag per topic/partition

### Grafana Dashboards

Import the dashboard from `servicemonitor.yaml`:

```bash
# Get Grafana URL
kubectl get svc -n monitoring grafana

# Port forward
kubectl port-forward -n monitoring svc/grafana 3000:80

# Open http://localhost:3000
# Import dashboard from ConfigMap
```

### Alerts

Prometheus alerts are configured in `servicemonitor.yaml`:

- **Critical**: Application down, database down, Kafka down
- **Warning**: High error rate, high latency, high consumer lag
- **Info**: Low conflict detection rate, quality score degradation

Configure AlertManager for notifications:

```bash
# Edit AlertManager ConfigMap
kubectl edit configmap -n monitoring alertmanager

# Add receivers (Slack, PagerDuty, email)
receivers:
- name: 'slack'
  slack_configs:
  - api_url: 'https://hooks.slack.com/services/YOUR/WEBHOOK/URL'
    channel: '#alerts'
```

## 📈 Scaling

### Horizontal Pod Autoscaler

HPA automatically scales based on:
- **CPU**: 70% utilization
- **Memory**: 80% utilization
- **Kafka lag**: 5000 messages

```bash
# View HPA status
kubectl get hpa -n location-sync-gateway

# Manually scale (overrides HPA temporarily)
kubectl scale deployment location-sync-gateway --replicas=5 -n location-sync-gateway
```

### Vertical Scaling

Update resource requests/limits:

```bash
# Edit deployment
kubectl edit deployment location-sync-gateway -n location-sync-gateway

# Or patch
kubectl patch deployment location-sync-gateway -n location-sync-gateway \
  -p '{"spec":{"template":{"spec":{"containers":[{"name":"application","resources":{"requests":{"memory":"1Gi","cpu":"1000m"},"limits":{"memory":"2Gi","cpu":"2000m"}}}]}}}}'
```

### Database Scaling

**PostgreSQL:**
- Vertical: Increase StatefulSet resources
- Read replicas: Add read-only replicas (requires configuration)
- **Production**: Use Amazon RDS with Multi-AZ

**Kafka:**
- Add brokers: Increase StatefulSet replicas
- Rebalance partitions: Use Kafka tools
- **Production**: Use Amazon MSK

**Redis:**
- Vertical: Increase deployment resources
- Clustering: Deploy Redis Cluster mode
- **Production**: Use Amazon ElastiCache

## 🔒 Security

### RBAC

ServiceAccount has minimal permissions:
- Read ConfigMaps and Secrets
- Read Pods and Services (for discovery)

For AWS EKS with IRSA:

```yaml
annotations:
  eks.amazonaws.com/role-arn: arn:aws:iam::ACCOUNT_ID:role/location-sync-gateway-pod-role
```

### Network Policies

Default deny-all ingress, explicit allow rules:

```bash
# Verify network policies
kubectl get networkpolicy -n location-sync-gateway

# Test connectivity
kubectl run test-pod --rm -it --image=busybox -n location-sync-gateway -- sh
# Try to connect to services
```

### Pod Security

Security contexts enforce:
- Run as non-root user (UID 1001)
- Read-only root filesystem (where possible)
- Drop all capabilities
- No privilege escalation

### Secrets Management

**Development:**
```bash
kubectl create secret generic location-sync-gateway-secrets \
  --from-literal=db-password=YOUR_PASSWORD \
  --from-literal=redis-password=YOUR_PASSWORD \
  -n location-sync-gateway
```

**Production Options:**
1. **AWS Secrets Manager** + External Secrets Operator
2. **HashiCorp Vault** + Vault Agent Injector
3. **Sealed Secrets** (GitOps-friendly)

## 🔧 Troubleshooting

### Check Pod Status

```bash
# List all pods
kubectl get pods -n location-sync-gateway

# Describe pod
kubectl describe pod <pod-name> -n location-sync-gateway

# View logs
kubectl logs <pod-name> -n location-sync-gateway

# Follow logs
kubectl logs -f <pod-name> -n location-sync-gateway

# Previous logs (if pod crashed)
kubectl logs <pod-name> --previous -n location-sync-gateway
```

### Debug Application Issues

```bash
# Execute shell in pod
kubectl exec -it <pod-name> -n location-sync-gateway -- /bin/sh

# Check environment variables
kubectl exec <pod-name> -n location-sync-gateway -- env

# Check DNS resolution
kubectl exec <pod-name> -n location-sync-gateway -- nslookup postgres-service

# Check connectivity
kubectl exec <pod-name> -n location-sync-gateway -- nc -zv postgres-service 5432
```

### Database Issues

```bash
# Connect to PostgreSQL
kubectl exec -it postgres-0 -n location-sync-gateway -- psql -U lsg_user -d location_sync_gateway

# Check PostgreSQL logs
kubectl logs postgres-0 -n location-sync-gateway

# Check persistence
kubectl get pvc -n location-sync-gateway
```

### Kafka Issues

```bash
# List topics
kubectl exec -it kafka-0 -n location-sync-gateway -- kafka-topics --list --bootstrap-server localhost:9092

# Check consumer groups
kubectl exec -it kafka-0 -n location-sync-gateway -- kafka-consumer-groups --list --bootstrap-server localhost:9092

# Check consumer lag
kubectl exec -it kafka-0 -n location-sync-gateway -- kafka-consumer-groups \
  --describe --group location-sync-gateway-consumer-group --bootstrap-server localhost:9092
```

### Redis Issues

```bash
# Connect to Redis
kubectl exec -it redis-<pod-id> -n location-sync-gateway -- redis-cli

# Authenticate
AUTH <password>

# Check keys
KEYS *

# Monitor commands
MONITOR
```

### Common Issues

**1. Pods pending**
```bash
# Check events
kubectl get events -n location-sync-gateway --sort-by='.lastTimestamp'

# Common causes:
# - Insufficient cluster resources
# - PVC not bound (storage issue)
# - Image pull errors
```

**2. CrashLoopBackOff**
```bash
# View logs
kubectl logs <pod-name> --previous -n location-sync-gateway

# Common causes:
# - Application startup failure
# - Missing environment variables
# - Database connection failure
```

**3. ImagePullBackOff**
```bash
# Check image name and tag
kubectl describe pod <pod-name> -n location-sync-gateway

# Common causes:
# - Wrong image name/tag
# - Private registry without imagePullSecrets
# - Network issues
```

## 🏭 Production Deployment

### Managed Services (Recommended)

Instead of running stateful services in Kubernetes, use managed services:

**AWS:**
```yaml
# Use these instead of StatefulSets
- PostgreSQL: Amazon RDS for PostgreSQL
- Kafka: Amazon MSK (Managed Streaming for Kafka)
- Redis: Amazon ElastiCache for Redis
- Secrets: AWS Secrets Manager
- Ingress: AWS ALB Ingress Controller
- Monitoring: Amazon Managed Service for Prometheus
```

**GCP:**
```yaml
- PostgreSQL: Cloud SQL for PostgreSQL
- Kafka: Confluent Cloud or self-managed
- Redis: Cloud Memorystore
- Secrets: Secret Manager
- Ingress: GKE Ingress Controller
- Monitoring: Cloud Monitoring
```

### Infrastructure as Code

**Terraform example:**

```hcl
# eks-cluster.tf
module "eks" {
  source          = "terraform-aws-modules/eks/aws"
  cluster_name    = "location-sync-gateway"
  cluster_version = "1.27"

  vpc_id          = module.vpc.vpc_id
  subnet_ids      = module.vpc.private_subnets

  eks_managed_node_groups = {
    application = {
      desired_size = 3
      min_size     = 3
      max_size     = 12
      instance_types = ["t3.large"]
    }
  }
}

# rds.tf
resource "aws_db_instance" "postgres" {
  identifier     = "location-sync-gateway"
  engine         = "postgres"
  engine_version = "15.4"
  instance_class = "db.r6g.xlarge"

  multi_az               = true
  storage_encrypted      = true
  backup_retention_period = 7
}

# msk.tf
resource "aws_msk_cluster" "kafka" {
  cluster_name   = "location-sync-gateway"
  kafka_version  = "3.5.1"
  number_of_broker_nodes = 3

  broker_node_group_info {
    instance_type = "kafka.m5.xlarge"
    client_subnets = var.private_subnet_ids
  }
}
```

### CI/CD Pipeline

**GitHub Actions example:**

```yaml
name: Deploy to EKS

on:
  push:
    branches: [main]

jobs:
  deploy:
    runs-on: ubuntu-latest
    steps:
    - uses: actions/checkout@v3

    - name: Configure AWS credentials
      uses: aws-actions/configure-aws-credentials@v2
      with:
        role-to-assume: ${{ secrets.AWS_ROLE_ARN }}
        aws-region: us-east-1

    - name: Build and push Docker image
      run: |
        docker build -t $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG .
        docker push $ECR_REGISTRY/$ECR_REPOSITORY:$IMAGE_TAG

    - name: Deploy to EKS
      run: |
        aws eks update-kubeconfig --name location-sync-gateway
        kubectl apply -k k8s/overlays/production
        kubectl rollout status deployment/location-sync-gateway -n location-sync-gateway
```

### Health Checks

Ensure endpoints are implemented:

```kotlin
// Health check endpoint
@Get("/health")
fun health(): HttpResponse<Map<String, Any>> {
    return HttpResponse.ok(mapOf(
        "status" to "UP",
        "database" to checkDatabase(),
        "kafka" to checkKafka(),
        "redis" to checkRedis()
    ))
}

// Readiness check
@Get("/health/ready")
fun ready(): HttpResponse<String> {
    return if (isApplicationReady()) {
        HttpResponse.ok("READY")
    } else {
        HttpResponse.status(HttpStatus.SERVICE_UNAVAILABLE, "NOT_READY")
    }
}

// Liveness check
@Get("/health/live")
fun live(): HttpResponse<String> {
    return HttpResponse.ok("ALIVE")
}
```

### Deployment Checklist

- [ ] Secrets stored in AWS Secrets Manager (not in Git)
- [ ] TLS certificates configured (cert-manager or ACM)
- [ ] Resource requests/limits tuned based on load testing
- [ ] HPA tested and validated
- [ ] Network policies tested
- [ ] Monitoring and alerting configured
- [ ] Log aggregation configured (CloudWatch, ELK)
- [ ] Backup and disaster recovery tested
- [ ] Security scanning (Trivy, Snyk)
- [ ] Compliance checks (OPA, Kyverno)
- [ ] Documentation updated

## 📚 Additional Resources

- [Kubernetes Documentation](https://kubernetes.io/docs/)
- [Micronaut Kubernetes Guide](https://guides.micronaut.io/latest/micronaut-kubernetes.html)
- [AWS EKS Best Practices](https://aws.github.io/aws-eks-best-practices/)
- [Prometheus Operator](https://prometheus-operator.dev/)
- [Kustomize Documentation](https://kustomize.io/)

---

**Note:** This Kubernetes configuration is production-ready but should be customized for your specific requirements. Always test thoroughly in a non-production environment first.
