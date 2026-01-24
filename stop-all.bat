@echo off
REM CrisisConnect - Stop All Services (Windows)

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set STOPPED_SOMETHING=false

echo ========================================
echo CrisisConnect - Stopping All Services
echo ========================================
echo.

REM Stop frontend
if exist "%SCRIPT_DIR%frontend.pid" (
    echo Stopping frontend...
    set /p PID=<"%SCRIPT_DIR%frontend.pid"
    taskkill /PID !PID! /F /T >nul 2>&1
    del "%SCRIPT_DIR%frontend.pid" >nul 2>&1
    set STOPPED_SOMETHING=true
)

REM Also try to kill any node process on port 3000
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING 2^>nul') do (
    echo Stopping process on port 3000 (PID: %%a)...
    taskkill /PID %%a /F /T >nul 2>&1
    set STOPPED_SOMETHING=true
)

REM Stop backend
if exist "%SCRIPT_DIR%backend.pid" (
    echo Stopping backend...
    set /p PID=<"%SCRIPT_DIR%backend.pid"
    taskkill /PID !PID! /F /T >nul 2>&1
    del "%SCRIPT_DIR%backend.pid" >nul 2>&1
    set STOPPED_SOMETHING=true
)

REM Also try to kill any java process on port 8080
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :8080 ^| findstr LISTENING 2^>nul') do (
    echo Stopping process on port 8080 (PID: %%a)...
    taskkill /PID %%a /F /T >nul 2>&1
    set STOPPED_SOMETHING=true
)

REM Stop demo
if exist "%SCRIPT_DIR%demo.pid" (
    echo Stopping demo...
    set /p PID=<"%SCRIPT_DIR%demo.pid"
    taskkill /PID !PID! /F /T >nul 2>&1
    del "%SCRIPT_DIR%demo.pid" >nul 2>&1
    set STOPPED_SOMETHING=true
)

REM Clean up any stale PID files
if exist "%SCRIPT_DIR%backend.pid" del "%SCRIPT_DIR%backend.pid" >nul 2>&1
if exist "%SCRIPT_DIR%frontend.pid" del "%SCRIPT_DIR%frontend.pid" >nul 2>&1
if exist "%SCRIPT_DIR%demo.pid" del "%SCRIPT_DIR%demo.pid" >nul 2>&1

echo.
if "%STOPPED_SOMETHING%"=="true" (
    echo All services stopped successfully
) else (
    echo No services were running
)
echo.

endlocal
