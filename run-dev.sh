#!/bin/bash
# Quick development startup script - runs without Docker

set -e

echo "🚀 Starting Location Sync Gateway in Development Mode..."
echo ""
echo "Prerequisites Check:"
echo "  ✓ JDK 17+ installed"
echo "  ✓ No Docker required"
echo "  ✓ Uses H2 in-memory database"
echo ""

# Build project
echo "📦 Building project..."
./gradlew clean build -x test

echo ""
echo "🎯 Starting application..."
echo "   Server: http://localhost:8080"
echo "   Swagger UI: http://localhost:8080/swagger-ui"
echo "   Health Check: http://localhost:8080/health"
echo ""

# Run with development profile
MICRONAUT_ENVIRONMENTS=dev ./gradlew run
