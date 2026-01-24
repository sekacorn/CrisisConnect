@echo off
REM CrisisConnect - Frontend Start Script (Windows)
REM Starts the React frontend development server

setlocal enabledelayedexpansion

set SCRIPT_DIR=%~dp0
set FRONTEND_DIR=%SCRIPT_DIR%frontend
set LOG_FILE=%SCRIPT_DIR%frontend.log
set PID_FILE=%SCRIPT_DIR%frontend.pid

echo ========================================
echo CrisisConnect Frontend Startup
echo ========================================

REM Check if Node.js is installed
where node >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: Node.js is not installed
    echo Please install Node.js 18 or higher
    exit /b 1
)

REM Check if npm is installed
where npm >nul 2>&1
if %errorlevel% neq 0 (
    echo Error: npm is not installed
    exit /b 1
)

REM Check if frontend is already running
if exist "%PID_FILE%" (
    set /p OLD_PID=<"%PID_FILE%"
    tasklist /FI "PID eq !OLD_PID!" 2>nul | find "node.exe" >nul
    if !errorlevel! equ 0 (
        echo Frontend is already running (PID: !OLD_PID!)
        echo Stop it first with: stop-frontend.bat
        exit /b 1
    ) else (
        echo Removing stale PID file
        del "%PID_FILE%"
    )
)

REM Navigate to frontend directory
cd /d "%FRONTEND_DIR%"

REM Install dependencies if needed
if not exist "node_modules" (
    echo Installing frontend dependencies...
    echo This may take a few minutes...
    call npm install
    if !errorlevel! neq 0 (
        echo Failed to install dependencies
        exit /b 1
    )
    echo Dependencies installed successfully!
) else (
    echo Dependencies already installed. Use 'npm install' to update if needed.
)

echo.
echo Starting frontend development server...
echo Log file: %LOG_FILE%
echo.
echo NOTE: Frontend compilation may take 20-30 seconds...
echo The development server will open in a new window.
echo.

REM Start frontend using npm start in a new window (more visible and reliable)
REM Window stays open so you can see compilation output and any errors
start "CrisisConnect Frontend" cmd /c "cd /d \"%FRONTEND_DIR%\" && npm start > \"%LOG_FILE%\" 2>&1"

REM Wait for frontend to compile and start (increased timeout for reliability)
echo Waiting for frontend to compile and start...
timeout /t 15 /nobreak >nul
echo Still compiling... please wait...
timeout /t 15 /nobreak >nul

REM Check if port 3000 is listening (more reliable than PID tracking)
set FRONTEND_STARTED=false
for /f "tokens=5" %%a in ('netstat -ano ^| findstr :3000 ^| findstr LISTENING 2^>nul') do (
    set FRONTEND_PID=%%a
    set FRONTEND_STARTED=true
    goto :frontend_found
)

:frontend_found
if "%FRONTEND_STARTED%"=="true" (
    REM Save PID for stopping later
    echo !FRONTEND_PID! > "%PID_FILE%"

    echo ========================================
    echo Frontend started successfully!
    echo ========================================
    echo PID: !FRONTEND_PID!
    echo URL: http://localhost:3000
    echo.
    echo The browser should open automatically.
    echo If not, manually navigate to: http://localhost:3000
    echo.
    echo To view logs:
    echo   type %LOG_FILE%
    echo   or tail -f equivalent: powershell Get-Content %LOG_FILE% -Wait
    echo.
    echo To stop frontend:
    echo   stop-frontend.bat
) else (
    echo Frontend failed to start. Port 3000 is not listening.
    echo This could mean:
    echo   1. Compilation is still in progress (wait longer)
    echo   2. There are compilation errors
    echo   3. Port 3000 is already in use
    echo.
    echo Check logs for details:
    echo   type %LOG_FILE%
    echo.
    echo Last 30 lines of log:
    powershell -Command "Get-Content '%LOG_FILE%' -Tail 30 -ErrorAction SilentlyContinue"
    del "%PID_FILE%" 2>nul
    exit /b 1
)

endlocal
