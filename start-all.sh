#!/bin/bash

# CrisisConnect - Start All Services (Linux/Mac)
# Starts both backend and frontend

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}CrisisConnect - Starting All Services${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# Parse arguments
USE_DEMO=false
USE_H2=false
SKIP_BUILD=false
CLEAN_BUILD=false

while [[ $# -gt 0 ]]; do
    case $1 in
        --demo)
            USE_DEMO=true
            shift
            ;;
        --h2)
            USE_H2=true
            shift
            ;;
        --skip-build)
            SKIP_BUILD=true
            shift
            ;;
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -h|--help)
            echo "Usage: ./start-all.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  --demo         Use demo mode with H2 (special demo setup)"
            echo "  --h2           Use H2 database for development (persistent data)"
            echo "  --skip-build   Skip building, use existing JARs"
            echo "  -c, --clean    Clean build before starting"
            echo "  -h, --help     Show this help message"
            echo ""
            echo "Database Modes:"
            echo "  Default:       PostgreSQL (requires database setup)"
            echo "  --h2:          H2 in-memory (easy development, no PostgreSQL needed)"
            echo "  --demo:        H2 with demo bootstrap (quick testing)"
            exit 0
            ;;
        *)
            echo -e "${YELLOW}Warning: Unknown option $1${NC}"
            shift
            ;;
    esac
done

# Check if already running
if [ -f "$SCRIPT_DIR/backend.pid" ] || [ -f "$SCRIPT_DIR/demo.pid" ]; then
    echo -e "${YELLOW}Backend appears to be running. Stop it first with:${NC}"
    echo "  ./stop-all.sh"
    exit 1
fi

if [ -f "$SCRIPT_DIR/frontend.pid" ]; then
    echo -e "${YELLOW}Frontend appears to be running. Stop it first with:${NC}"
    echo "  ./stop-all.sh"
    exit 1
fi

# Start backend
if [ "$USE_DEMO" = true ]; then
    echo -e "${YELLOW}Starting backend in DEMO mode...${NC}"
    "$SCRIPT_DIR/start-demo.sh"
elif [ "$USE_H2" = true ]; then
    echo -e "${YELLOW}Starting backend with H2 database...${NC}"

    # Build backend args
    BACKEND_ARGS="--h2"
    if [ "$SKIP_BUILD" = true ]; then
        BACKEND_ARGS="$BACKEND_ARGS -s"
    fi
    if [ "$CLEAN_BUILD" = true ]; then
        BACKEND_ARGS="$BACKEND_ARGS -c"
    fi

    "$SCRIPT_DIR/start-backend.sh" $BACKEND_ARGS
else
    echo -e "${YELLOW}Starting backend with PostgreSQL...${NC}"

    # Build backend args
    BACKEND_ARGS=""
    if [ "$SKIP_BUILD" = true ]; then
        BACKEND_ARGS="$BACKEND_ARGS -s"
    fi
    if [ "$CLEAN_BUILD" = true ]; then
        BACKEND_ARGS="$BACKEND_ARGS -c"
    fi

    "$SCRIPT_DIR/start-backend.sh" $BACKEND_ARGS
fi

if [ $? -ne 0 ]; then
    echo -e "${RED}Failed to start backend!${NC}"
    exit 1
fi

echo ""
echo -e "${YELLOW}Waiting for backend to initialize...${NC}"
echo "(This may take 10-15 seconds)"
sleep 3

# Start frontend
echo ""
echo -e "${YELLOW}Starting frontend...${NC}"
echo ""
"$SCRIPT_DIR/start-frontend.sh"

if [ $? -ne 0 ]; then
    echo ""
    echo -e "${RED}[ERROR] Failed to start frontend!${NC}"
    echo -e "${YELLOW}Stopping backend...${NC}"
    "$SCRIPT_DIR/stop-all.sh"
    exit 1
fi

echo ""
echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}All services started!${NC}"
echo -e "${GREEN}========================================${NC}"
echo ""
echo -e "${BLUE}Frontend:${NC}  http://localhost:3000"
echo -e "${BLUE}Backend:${NC}   http://localhost:8080/api"

if [ "$USE_DEMO" = true ]; then
    echo -e "${BLUE}H2 Console:${NC} http://localhost:8080/h2-console"
    echo ""
    echo -e "${YELLOW}Demo Users:${NC}"
    echo "  Admin: admin@crisisconnect.org / Admin2026!Secure"
elif [ "$USE_H2" = true ]; then
    echo -e "${BLUE}H2 Console:${NC} http://localhost:8080/h2-console"
    echo ""
    echo -e "${YELLOW}Bootstrap Admin Account (NIST-COMPLIANT):${NC}"
    echo "  Email: admin@crisisconnect.org"
    echo "  Password: Admin2026!Secure"
    echo "  Note: 12+ chars, mixed case, numbers, special chars"
    echo ""
    echo -e "${RED}Note: Using H2 in-memory database - data will be lost on restart${NC}"
fi

echo ""
echo "To stop all services:"
echo "  ./stop-all.sh"
echo ""
echo "To view logs:"
if [ "$USE_DEMO" = true ]; then
    echo "  tail -f demo.log"
else
    echo "  tail -f backend.log"
fi
echo "  tail -f frontend.log"
