@echo off
REM CrisisConnect - Test Runner Script (Windows)
REM Runs all tests: backend unit tests, frontend unit tests, and E2E tests

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set BACKEND_DIR=%SCRIPT_DIR%backend
set FRONTEND_DIR=%SCRIPT_DIR%frontend
set E2E_DIR=%SCRIPT_DIR%e2e

echo ========================================
echo CrisisConnect Test Suite
echo ========================================
echo.

REM Run backend tests
echo Running backend tests...
cd /d "%BACKEND_DIR%"
call mvn test

if %errorlevel% neq 0 (
    echo [ERROR] Backend tests failed
    cd /d "%SCRIPT_DIR%"
    exit /b 1
)

echo [OK] Backend tests passed
cd /d "%SCRIPT_DIR%"

REM Run frontend tests
echo.
echo Running frontend tests...
cd /d "%FRONTEND_DIR%"
call npm test -- --coverage --watchAll=false

if %errorlevel% neq 0 (
    echo [ERROR] Frontend tests failed
    cd /d "%SCRIPT_DIR%"
    exit /b 1
)

echo [OK] Frontend tests passed
cd /d "%SCRIPT_DIR%"

REM Run E2E tests (if Cypress is installed)
echo.
echo Running E2E tests...
cd /d "%E2E_DIR%"

if exist "node_modules" (
    call npm test
    if !errorlevel! neq 0 (
        echo [ERROR] E2E tests failed
        cd /d "%SCRIPT_DIR%"
        exit /b 1
    )
    echo [OK] E2E tests passed
) else (
    echo [WARN] E2E dependencies not installed. Run 'npm install' in e2e directory.
)

cd /d "%SCRIPT_DIR%"

echo.
echo ========================================
echo All tests passed
echo ========================================

endlocal
