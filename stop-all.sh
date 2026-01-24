#!/bin/bash

# CrisisConnect - Stop All Services (Linux/Mac)

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}CrisisConnect - Stopping All Services${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

STOPPED_SOMETHING=false

# Stop frontend
if [ -f "$SCRIPT_DIR/frontend.pid" ]; then
    echo -e "${YELLOW}Stopping frontend...${NC}"
    PID=$(cat "$SCRIPT_DIR/frontend.pid")
    if ps -p $PID > /dev/null 2>&1; then
        kill $PID 2>/dev/null || kill -9 $PID 2>/dev/null || true
        STOPPED_SOMETHING=true
    fi
    rm -f "$SCRIPT_DIR/frontend.pid"
fi

# Also try to kill any process on port 3000
if command -v lsof &> /dev/null; then
    PORT_PID=$(lsof -ti:3000 2>/dev/null)
    if [ ! -z "$PORT_PID" ]; then
        echo -e "${YELLOW}Stopping process on port 3000 (PID: $PORT_PID)...${NC}"
        kill -9 $PORT_PID 2>/dev/null || true
        STOPPED_SOMETHING=true
    fi
fi

# Stop backend
if [ -f "$SCRIPT_DIR/backend.pid" ]; then
    echo -e "${YELLOW}Stopping backend...${NC}"
    PID=$(cat "$SCRIPT_DIR/backend.pid")
    if ps -p $PID > /dev/null 2>&1; then
        kill $PID 2>/dev/null || kill -9 $PID 2>/dev/null || true
        STOPPED_SOMETHING=true
    fi
    rm -f "$SCRIPT_DIR/backend.pid"
fi

# Also try to kill any process on port 8080
if command -v lsof &> /dev/null; then
    PORT_PID=$(lsof -ti:8080 2>/dev/null)
    if [ ! -z "$PORT_PID" ]; then
        echo -e "${YELLOW}Stopping process on port 8080 (PID: $PORT_PID)...${NC}"
        kill -9 $PORT_PID 2>/dev/null || true
        STOPPED_SOMETHING=true
    fi
fi

# Stop demo
if [ -f "$SCRIPT_DIR/demo.pid" ]; then
    echo -e "${YELLOW}Stopping demo...${NC}"
    PID=$(cat "$SCRIPT_DIR/demo.pid")
    if ps -p $PID > /dev/null 2>&1; then
        kill $PID 2>/dev/null || kill -9 $PID 2>/dev/null || true
        STOPPED_SOMETHING=true
    fi
    rm -f "$SCRIPT_DIR/demo.pid"
fi

# Clean up any stale PID files
rm -f "$SCRIPT_DIR/backend.pid" 2>/dev/null
rm -f "$SCRIPT_DIR/frontend.pid" 2>/dev/null
rm -f "$SCRIPT_DIR/demo.pid" 2>/dev/null

echo ""
if [ "$STOPPED_SOMETHING" = true ]; then
    echo -e "${GREEN}All services stopped successfully${NC}"
else
    echo -e "${YELLOW}No services were running${NC}"
fi
echo ""
