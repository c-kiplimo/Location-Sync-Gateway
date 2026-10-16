@echo off
echo Starting all services (PostgreSQL, Redis, Kafka, Jaeger)...
docker-compose up -d

echo.
echo Waiting for services to be healthy...
timeout /t 10 /nobreak >nul

echo.
echo Services status:
docker-compose ps

echo.
echo ============================================
echo Services are starting up!
echo ============================================
echo PostgreSQL: localhost:5432
echo Redis:      localhost:6379
echo Kafka:      localhost:9092
echo Jaeger UI:  http://localhost:16686
echo ============================================
echo.
echo To view logs: docker-compose logs -f
echo To stop: docker-compose down
echo ============================================
