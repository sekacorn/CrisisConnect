#!/bin/bash

# CrisisConnect - Frontend Start Script (Linux/Mac)
# Starts the React frontend development server

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
FRONTEND_DIR="$SCRIPT_DIR/frontend"
LOG_FILE="$SCRIPT_DIR/frontend.log"
PID_FILE="$SCRIPT_DIR/frontend.pid"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}CrisisConnect Frontend Startup${NC}"
echo -e "${BLUE}========================================${NC}"

# Check if Node.js is installed
if ! command -v node &> /dev/null; then
    echo -e "${RED}Error: Node.js is not installed${NC}"
    echo "Please install Node.js 18 or higher"
    exit 1
fi

# Check if npm is installed
if ! command -v npm &> /dev/null; then
    echo -e "${RED}Error: npm is not installed${NC}"
    exit 1
fi

# Check if frontend is already running
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p "$OLD_PID" > /dev/null 2>&1; then
        echo -e "${YELLOW}Frontend is already running (PID: $OLD_PID)${NC}"
        echo "Stop it first with: ./stop-frontend.sh"
        exit 1
    else
        echo -e "${YELLOW}Removing stale PID file${NC}"
        rm -f "$PID_FILE"
    fi
fi

# Navigate to frontend directory
cd "$FRONTEND_DIR"

# Install dependencies if needed
if [ ! -d "node_modules" ]; then
    echo -e "${YELLOW}Installing frontend dependencies...${NC}"
    npm install
    if [ $? -ne 0 ]; then
        echo -e "${RED}Failed to install dependencies${NC}"
        exit 1
    fi
else
    echo "Dependencies already installed. Use 'npm install' to update if needed."
fi

echo ""
echo -e "${YELLOW}Starting frontend development server...${NC}"
echo "Log file: $LOG_FILE"
echo ""
echo -e "${BLUE}NOTE: Frontend compilation may take 20-30 seconds...${NC}"
echo ""

# Start frontend in background
nohup npm start > "$LOG_FILE" 2>&1 &
FRONTEND_PID=$!

# Save PID
echo $FRONTEND_PID > "$PID_FILE"

# Wait for frontend to compile and start (increased from 10s to 30s)
echo "Waiting for frontend to compile and start..."
sleep 15
echo "Still compiling... please wait..."
sleep 15

# Check if process is still running AND port is listening
FRONTEND_STARTED=false
if ps -p $FRONTEND_PID > /dev/null 2>&1; then
    # Check if port 3000 is listening (if lsof is available)
    if command -v lsof &> /dev/null; then
        if lsof -ti:3000 > /dev/null 2>&1; then
            FRONTEND_STARTED=true
        fi
    else
        # Fallback: just check if process is running
        FRONTEND_STARTED=true
    fi
fi

if [ "$FRONTEND_STARTED" = true ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Frontend started successfully!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "PID: $FRONTEND_PID"
    echo "URL: http://localhost:3000"
    echo ""
    echo "The browser should open automatically."
    echo "If not, manually navigate to: http://localhost:3000"
    echo ""
    echo "To view logs:"
    echo "  tail -f $LOG_FILE"
    echo ""
    echo "To stop frontend:"
    echo "  ./stop-frontend.sh"
else
    echo -e "${RED}Frontend failed to start. Port 3000 is not listening.${NC}"
    echo "This could mean:"
    echo "  1. Compilation is still in progress (wait longer)"
    echo "  2. There are compilation errors"
    echo "  3. Port 3000 is already in use"
    echo ""
    echo "Check logs for details:"
    echo "  tail -30 $LOG_FILE"
    echo ""
    echo "Last 30 lines of log:"
    tail -30 "$LOG_FILE" 2>/dev/null || echo "Log file not found"
    rm -f "$PID_FILE"
    exit 1
fi
