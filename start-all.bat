@echo off
REM CrisisConnect - Start All Services (Windows)
REM Starts both backend and frontend

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set USE_DEMO=false
set USE_H2=false
set SKIP_BUILD=false
set CLEAN_BUILD=false

REM Parse arguments
:parse_args
if "%~1"=="" goto :done_parsing
if /i "%~1"=="--demo" set USE_DEMO=true
if /i "%~1"=="--h2" set USE_H2=true
if /i "%~1"=="--skip-build" set SKIP_BUILD=true
if /i "%~1"=="-c" set CLEAN_BUILD=true
if /i "%~1"=="--clean" set CLEAN_BUILD=true
if /i "%~1"=="-h" goto :show_help
if /i "%~1"=="--help" goto :show_help
shift
goto :parse_args

:show_help
echo Usage: start-all.bat [OPTIONS]
echo.
echo Options:
echo   --demo         Use demo mode with H2 (special demo setup)
echo   --h2           Use H2 database for development (persistent data)
echo   --skip-build   Skip building, use existing JARs
echo   -c, --clean    Clean build before starting
echo   -h, --help     Show this help message
echo.
echo Database Modes:
echo   Default:       PostgreSQL (requires database setup)
echo   --h2:          H2 in-memory (easy development, no PostgreSQL needed)
echo   --demo:        H2 with demo bootstrap (quick testing)
exit /b 0

:done_parsing

echo ========================================
echo CrisisConnect - Starting All Services
echo ========================================
echo.

REM Check if already running
if exist "%SCRIPT_DIR%backend.pid" (
    echo Backend appears to be running. Stop it first with:
    echo   stop-all.bat
    exit /b 1
)

if exist "%SCRIPT_DIR%demo.pid" (
    echo Demo appears to be running. Stop it first with:
    echo   stop-all.bat
    exit /b 1
)

if exist "%SCRIPT_DIR%frontend.pid" (
    echo Frontend appears to be running. Stop it first with:
    echo   stop-all.bat
    exit /b 1
)

REM Start backend
if "%USE_DEMO%"=="true" (
    echo Starting backend in DEMO mode...
    call "%SCRIPT_DIR%start-demo.bat"
) else if "%USE_H2%"=="true" (
    echo Starting backend with H2 database...

    REM Build backend args
    set BACKEND_ARGS=--h2
    if "%SKIP_BUILD%"=="true" set BACKEND_ARGS=!BACKEND_ARGS! -s
    if "%CLEAN_BUILD%"=="true" set BACKEND_ARGS=!BACKEND_ARGS! -c

    call "%SCRIPT_DIR%start-backend.bat" !BACKEND_ARGS!
) else (
    echo Starting backend with PostgreSQL...

    REM Build backend args
    set BACKEND_ARGS=
    if "%SKIP_BUILD%"=="true" set BACKEND_ARGS=!BACKEND_ARGS! -s
    if "%CLEAN_BUILD%"=="true" set BACKEND_ARGS=!BACKEND_ARGS! -c

    call "%SCRIPT_DIR%start-backend.bat" !BACKEND_ARGS!
)

if %errorlevel% neq 0 (
    echo Failed to start backend!
    exit /b 1
)

echo.
echo Waiting for backend to initialize...
echo (This may take 10-15 seconds)
timeout /t 3 /nobreak >nul

REM Start frontend
echo.
echo Starting frontend...
echo.
call "%SCRIPT_DIR%start-frontend.bat"

if %errorlevel% neq 0 (
    echo.
    echo [ERROR] Failed to start frontend!
    echo Stopping backend...
    call "%SCRIPT_DIR%stop-all.bat"
    exit /b 1
)

echo.
echo ========================================
echo All services started!
echo ========================================
echo.
echo Frontend:  http://localhost:3000
echo Backend:   http://localhost:8080/api

if "%USE_DEMO%"=="true" (
    echo H2 Console: http://localhost:8080/h2-console
    echo.
    echo Demo Users:
    echo   Admin: admin@crisisconnect.org / Admin2026!Secure
) else if "%USE_H2%"=="true" (
    echo H2 Console: http://localhost:8080/h2-console
    echo.
    echo Bootstrap Admin Account (NIST-COMPLIANT):
    echo   Email: admin@crisisconnect.org
    echo   Password: Admin2026!Secure
    echo   Note: 12+ chars, mixed case, numbers, special chars
    echo.
    echo Note: Using H2 in-memory database - data will be lost on restart
)

echo.
echo To stop all services:
echo   stop-all.bat
echo.
echo To view logs:
if "%USE_DEMO%"=="true" (
    echo   type demo.log
) else (
    echo   type backend.log
)
echo   type frontend.log

endlocal
