@echo off
REM Quick development startup script for Windows - runs without Docker

echo.
echo ================================
echo Location Sync Gateway - Dev Mode
echo ================================
echo.
echo Prerequisites Check:
echo   [OK] JDK 17+ installed
echo   [OK] No Docker required
echo   [OK] Uses H2 in-memory database
echo.

REM Build project
echo Building project...
call gradlew clean build -x test

echo.
echo Starting application...
echo   Server: http://localhost:8080
echo   Swagger UI: http://localhost:8080/swagger-ui
echo   Health Check: http://localhost:8080/health
echo.

REM Run with development profile
set MICRONAUT_ENVIRONMENTS=dev
call gradlew run
