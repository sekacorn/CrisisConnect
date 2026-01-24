@echo off
REM CrisisConnect - Backend Start Script (Windows)
REM Builds and starts the Spring Boot backend with PostgreSQL

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set BACKEND_DIR=%SCRIPT_DIR%backend
set LOG_FILE=%SCRIPT_DIR%backend.log
set PID_FILE=%SCRIPT_DIR%backend.pid

echo ========================================
echo CrisisConnect Backend Startup
echo ========================================

REM Check if Java is installed
where java >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Java is not installed
    echo Please install Java 17 or higher
    exit /b 1
)

REM Check if Maven is installed
where mvn >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Maven is not installed
    echo Please install Maven 3.6 or higher
    exit /b 1
)

REM Parse arguments
set CLEAN_BUILD=false
set SKIP_TESTS=false
set USE_H2=false

:parse_args
if "%~1"=="" goto :done_parsing
if /i "%~1"=="-c" set CLEAN_BUILD=true
if /i "%~1"=="--clean" set CLEAN_BUILD=true
if /i "%~1"=="-s" set SKIP_TESTS=true
if /i "%~1"=="--skip-tests" set SKIP_TESTS=true
if /i "%~1"=="--h2" set USE_H2=true
if /i "%~1"=="-h" goto :show_help
if /i "%~1"=="--help" goto :show_help
shift
goto :parse_args

:show_help
echo Usage: start-backend.bat [OPTIONS]
echo.
echo Options:
echo   -c, --clean       Clean build (mvn clean)
echo   -s, --skip-tests  Skip running tests
echo   --h2              Use H2 database instead of PostgreSQL
echo   -h, --help        Show this help message
exit /b 0

:done_parsing

REM Check if backend is already running
if exist "%PID_FILE%" (
    set /p OLD_PID=<"%PID_FILE%"
    tasklist /FI "PID eq !OLD_PID!" 2>nul | find "java.exe" >nul
    if !errorlevel! equ 0 (
        echo Backend is already running (PID: !OLD_PID!)
        echo Stop it first with: taskkill /PID !OLD_PID!
        exit /b 1
    ) else (
        echo Removing stale PID file
        del "%PID_FILE%"
    )
)

REM Check database configuration
if "%USE_H2%"=="true" (
    echo Using H2 in-memory database
    echo H2 Console will be available at: http://localhost:8080/h2-console
) else (
    REM Check if .env file exists
    if not exist "%SCRIPT_DIR%.env" (
        echo Warning: .env file not found
        if exist "%SCRIPT_DIR%.env.example" (
            echo Creating .env from .env.example...
            copy "%SCRIPT_DIR%.env.example" "%SCRIPT_DIR%.env"
            echo Please edit .env and set your configuration
            echo DO NOT use default secrets in production!
        ) else (
            echo Error: .env.example not found
            exit /b 1
        )
    )
)

REM Navigate to backend directory
cd /d "%BACKEND_DIR%"

REM Build only if requested (skip by default for faster startup)
if "%CLEAN_BUILD%"=="true" (
    echo Performing clean build...
    if "%SKIP_TESTS%"=="true" (
        call mvn clean install -DskipTests
    ) else (
        call mvn clean install
    )
    if %errorlevel% neq 0 (
        echo Build failed!
        exit /b 1
    )
    echo Build successful!
)

echo Starting backend server...
echo Log file: %LOG_FILE%
echo.

REM Build Spring Boot arguments
set SPRING_ARGS=

if "%USE_H2%"=="true" (
    echo Using H2 in-memory database
    set SPRING_ARGS=--spring.datasource.url=jdbc:h2:mem:crisisconnect
    set SPRING_ARGS=!SPRING_ARGS! --spring.datasource.driver-class-name=org.h2.Driver
    set SPRING_ARGS=!SPRING_ARGS! --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect
    set SPRING_ARGS=!SPRING_ARGS! --spring.h2.console.enabled=true
    set SPRING_ARGS=!SPRING_ARGS! --spring.jpa.hibernate.ddl-auto=create-drop
    set SPRING_ARGS=!SPRING_ARGS! --admin.bootstrap.enabled=true
    set SPRING_ARGS=!SPRING_ARGS! --admin.bootstrap.email=admin@crisisconnect.org
    set SPRING_ARGS=!SPRING_ARGS! --admin.bootstrap.password=Admin2026!Secure
    set SPRING_ARGS=!SPRING_ARGS! --admin.bootstrap.name=System Administrator
)

REM Start using mvn spring-boot:run (more reliable than JAR approach)
if "%USE_H2%"=="true" (
    start /min cmd /c "mvn spring-boot:run -Dspring-boot.run.arguments=\"!SPRING_ARGS!\" > \"%LOG_FILE%\" 2>&1"
) else (
    start /min cmd /c "mvn spring-boot:run > \"%LOG_FILE%\" 2>&1"
)

REM Wait for backend to start (increased from 5s to 10s)
echo Waiting for backend to start...
timeout /t 10 /nobreak >nul

REM Check if port 8080 is listening (more reliable than PID tracking)
set BACKEND_STARTED=false
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    set BACKEND_PID=%%a
    set BACKEND_STARTED=true
    goto :backend_found
)

:backend_found
if "%BACKEND_STARTED%"=="true" (
    REM Save PID for stopping later
    echo !BACKEND_PID! > "%PID_FILE%"

    echo ========================================
    echo Backend started successfully!
    echo ========================================
    echo PID: !BACKEND_PID!
    echo Port: 8080
    echo API: http://localhost:8080/api

    if "%USE_H2%"=="true" (
        echo H2 Console: http://localhost:8080/h2-console
        echo.
        echo H2 Console Login:
        echo   JDBC URL: jdbc:h2:mem:crisisconnect
        echo   Username: sa
        echo   Password: (leave empty)
        echo.
        echo Bootstrap Admin Account (NIST-COMPLIANT):
        echo   Email: admin@crisisconnect.org
        echo   Password: Admin2026!Secure
        echo   Note: 12+ chars, mixed case, numbers, special chars
        echo.
        echo Note: Using H2 in-memory database - data will be lost on restart
    )

    echo.
    echo To view logs:
    echo   type %LOG_FILE%
    echo   or tail -f equivalent: powershell Get-Content %LOG_FILE% -Wait
    echo.
    echo To stop backend:
    echo   stop-backend.bat
) else (
    echo Backend failed to start. Port 8080 is not listening.
    echo Check logs for details:
    echo   type %LOG_FILE%
    echo.
    echo Last 20 lines of log:
    powershell -Command "Get-Content '%LOG_FILE%' -Tail 20 -ErrorAction SilentlyContinue"
    del "%PID_FILE%" 2>nul
    exit /b 1
)

endlocal
