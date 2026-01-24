@echo off
REM CrisisConnect - Demo Mode Start Script (Windows)
REM Uses H2 in-memory database for quick testing

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set BACKEND_DIR=%SCRIPT_DIR%backend
set LOG_FILE=%SCRIPT_DIR%demo.log
set PID_FILE=%SCRIPT_DIR%demo.pid

echo ========================================
echo CrisisConnect DEMO Mode
echo ========================================
echo Using H2 in-memory database
echo.

REM Check prerequisites
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed
    exit /b 1
)

where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Maven is not installed
    exit /b 1
)

REM Check if demo is already running
if exist "%PID_FILE%" (
    set /p OLD_PID=<"%PID_FILE%"
    tasklist /FI "PID eq !OLD_PID!" 2>nul | find "java.exe" >nul
    if !errorlevel! equ 0 (
        echo Demo is already running (PID: !OLD_PID!)
        exit /b 1
    ) else (
        del "%PID_FILE%"
    )
)

REM Navigate to backend
cd /d "%BACKEND_DIR%"

echo.
echo Starting demo server with H2 in-memory database...
echo Log file: %LOG_FILE%
echo.
echo NOTE: Demo startup may take 10-15 seconds...
echo.

REM Start using mvn spring-boot:run with demo configuration (more reliable)
set DEMO_ARGS=--spring.profiles.active=demo
set DEMO_ARGS=!DEMO_ARGS! --spring.datasource.url=jdbc:h2:mem:crisisconnect_demo
set DEMO_ARGS=!DEMO_ARGS! --spring.datasource.driver-class-name=org.h2.Driver
set DEMO_ARGS=!DEMO_ARGS! --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
set DEMO_ARGS=!DEMO_ARGS! --spring.h2.console.enabled=true
set DEMO_ARGS=!DEMO_ARGS! --spring.jpa.hibernate.ddl-auto=create-drop
set DEMO_ARGS=!DEMO_ARGS! --admin.bootstrap.enabled=true
set DEMO_ARGS=!DEMO_ARGS! --admin.bootstrap.email=admin@crisisconnect.org
set DEMO_ARGS=!DEMO_ARGS! --admin.bootstrap.password=Admin2026!Secure
set DEMO_ARGS=!DEMO_ARGS! --admin.bootstrap.name=System Administrator
set DEMO_ARGS=!DEMO_ARGS! --jwt.secret=demo-secret-for-testing-only-32-characters-minimum
set DEMO_ARGS=!DEMO_ARGS! --encryption.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef

start /min cmd /c "mvn spring-boot:run -Dspring-boot.run.arguments=\"!DEMO_ARGS!\" > \"%LOG_FILE%\" 2>&1"

REM Wait for demo backend to start (increased from 8s to 12s)
echo Waiting for demo backend to start...
timeout /t 12 /nobreak >nul

REM Check if port 8080 is listening (more reliable than PID tracking)
set DEMO_STARTED=false
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    set DEMO_PID=%%a
    set DEMO_STARTED=true
    goto :demo_found
)

:demo_found
if "%DEMO_STARTED%"=="true" (
    REM Save PID for stopping later
    echo !DEMO_PID! > "%PID_FILE%"

    echo ========================================
    echo Demo Backend started successfully!
    echo ========================================
    echo PID: !DEMO_PID!
    echo.
    echo Backend API:    http://localhost:8080/api
    echo H2 Console:     http://localhost:8080/h2-console
    echo Swagger UI:     http://localhost:8080/swagger-ui.html
    echo.
    echo H2 Console Login:
    echo   JDBC URL: jdbc:h2:mem:crisisconnect_demo
    echo   Username: sa
    echo   Password: (leave empty)
    echo.
    echo ========================================
    echo DEMO USERS (Auto-created on startup)
    echo ========================================
    echo.
    echo Admin Account (NIST-COMPLIANT):
    echo   Email:    admin@crisisconnect.org
    echo   Password: Admin2026!Secure
    echo   Role:     ADMIN
    echo   Access:   Full system access, manage users and organizations
    echo   Note:     12+ chars, mixed case, numbers, special chars
    echo.
    echo Note: This demo uses H2 in-memory database.
    echo       All data will be lost when the application stops.
    echo.
    echo ========================================
    echo.
    echo To start the frontend:
    echo   start-frontend.bat
    echo   Then visit: http://localhost:3000
    echo.
    echo Or start both together:
    echo   start-all.bat --demo
    echo.
    echo WARNING: This is DEMO mode only!
    echo Data is stored in memory and will be lost on restart.
    echo DO NOT use for production!
    echo.
    echo To view logs:
    echo   type %LOG_FILE%
    echo   or tail -f equivalent: powershell Get-Content %LOG_FILE% -Wait
    echo.
    echo To stop demo:
    echo   stop-demo.bat
) else (
    echo Demo failed to start. Port 8080 is not listening.
    echo Check logs for details:
    echo   type %LOG_FILE%
    echo.
    echo Last 30 lines of log:
    powershell -Command "Get-Content '%LOG_FILE%' -Tail 30 -ErrorAction SilentlyContinue"
    del "%PID_FILE%" 2>nul
    exit /b 1
)

endlocal
