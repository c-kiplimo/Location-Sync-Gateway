@echo off
REM Run application with local profile (Kafka disabled temporarily)

echo.
echo ====================================
echo Location Sync Gateway - Local Mode
echo ====================================
echo.
echo Services:
echo   PostgreSQL: localhost:5432
echo   Redis:      localhost:6379
echo   Kafka:      DISABLED (debugging)
echo.

set MICRONAUT_ENVIRONMENTS=local
call gradlew run
