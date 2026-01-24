#!/bin/bash

# CrisisConnect - Demo Mode Start Script (Linux/Mac)
# Uses H2 in-memory database for quick testing

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
LOG_FILE="$SCRIPT_DIR/demo.log"
PID_FILE="$SCRIPT_DIR/demo.pid"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}CrisisConnect DEMO Mode${NC}"
echo -e "${BLUE}========================================${NC}"
echo -e "${YELLOW}Using H2 in-memory database${NC}"
echo ""

# Check prerequisites
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    exit 1
fi

if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed${NC}"
    exit 1
fi

# Check if demo is already running
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p "$OLD_PID" > /dev/null 2>&1; then
        echo -e "${YELLOW}Demo is already running (PID: $OLD_PID)${NC}"
        exit 1
    else
        rm -f "$PID_FILE"
    fi
fi

# Navigate to backend
cd "$BACKEND_DIR"

echo ""
echo -e "${YELLOW}Starting demo server with H2 in-memory database...${NC}"
echo "Log file: $LOG_FILE"
echo ""
echo -e "${BLUE}NOTE: Demo startup may take 10-15 seconds...${NC}"
echo ""

# Start using mvn spring-boot:run with demo configuration (more reliable)
DEMO_ARGS="--spring.profiles.active=demo"
DEMO_ARGS="$DEMO_ARGS --spring.datasource.url=jdbc:h2:mem:crisisconnect_demo"
DEMO_ARGS="$DEMO_ARGS --spring.datasource.driver-class-name=org.h2.Driver"
DEMO_ARGS="$DEMO_ARGS --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
DEMO_ARGS="$DEMO_ARGS --spring.h2.console.enabled=true"
DEMO_ARGS="$DEMO_ARGS --spring.jpa.hibernate.ddl-auto=create-drop"
DEMO_ARGS="$DEMO_ARGS --admin.bootstrap.enabled=true"
DEMO_ARGS="$DEMO_ARGS --admin.bootstrap.email=admin@crisisconnect.org"
DEMO_ARGS="$DEMO_ARGS --admin.bootstrap.password=Admin2026!Secure"
DEMO_ARGS="$DEMO_ARGS --admin.bootstrap.name=System Administrator"
DEMO_ARGS="$DEMO_ARGS --jwt.secret=demo-secret-for-testing-only-32-characters-minimum"
DEMO_ARGS="$DEMO_ARGS --encryption.secret=0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef"

nohup mvn spring-boot:run -Dspring-boot.run.arguments="$DEMO_ARGS" > "$LOG_FILE" 2>&1 &

DEMO_PID=$!
echo $DEMO_PID > "$PID_FILE"

# Wait for demo backend to start (increased from 8s to 12s)
echo "Waiting for demo backend to start..."
sleep 12

# Check if process is still running AND port is listening
DEMO_STARTED=false
if ps -p $DEMO_PID > /dev/null 2>&1; then
    # Check if port 8080 is listening (if lsof is available)
    if command -v lsof &> /dev/null; then
        if lsof -ti:8080 > /dev/null 2>&1; then
            DEMO_STARTED=true
        fi
    else
        # Fallback: just check if process is running
        DEMO_STARTED=true
    fi
fi

if [ "$DEMO_STARTED" = true ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Demo Backend started successfully!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "PID: $DEMO_PID"
    echo ""
    echo -e "${BLUE}Backend API:${NC}    http://localhost:8080/api"
    echo -e "${BLUE}H2 Console:${NC}     http://localhost:8080/h2-console"
    echo -e "${BLUE}Swagger UI:${NC}     http://localhost:8080/swagger-ui.html"
    echo ""
    echo -e "${YELLOW}H2 Console Login:${NC}"
    echo "  JDBC URL: jdbc:h2:mem:crisisconnect_demo"
    echo "  Username: sa"
    echo "  Password: (leave empty)"
    echo ""
    echo -e "${YELLOW}========================================${NC}"
    echo -e "${YELLOW}DEMO USERS (Auto-created on startup)${NC}"
    echo -e "${YELLOW}========================================${NC}"
    echo ""
    echo -e "${GREEN}Admin Account (NIST-COMPLIANT):${NC}"
    echo "  Email:    admin@crisisconnect.org"
    echo "  Password: Admin2026!Secure"
    echo "  Role:     ADMIN"
    echo "  Access:   Full system access, manage users & organizations"
    echo "  Note:     12+ chars, mixed case, numbers, special chars"
    echo ""
    echo -e "${BLUE}Note:${NC} This demo uses H2 in-memory database."
    echo "      All data will be lost when the application stops."
    echo ""
    echo -e "${YELLOW}========================================${NC}"
    echo ""
    echo -e "${BLUE}To start the frontend:${NC}"
    echo "  ./start-frontend.sh"
    echo "  Then visit: http://localhost:3000"
    echo ""
    echo -e "${BLUE}Or start both together:${NC}"
    echo "  ./start-all.sh --demo"
    echo ""
    echo -e "${RED}WARNING: This is DEMO mode only!${NC}"
    echo -e "${RED}Data is stored in memory and will be lost on restart.${NC}"
    echo -e "${RED}DO NOT use for production!${NC}"
    echo ""
    echo "To view logs:"
    echo "  tail -f $LOG_FILE"
    echo ""
    echo "To stop demo:"
    echo "  ./stop-demo.sh"
else
    echo -e "${RED}Demo failed to start. Port 8080 is not listening.${NC}"
    echo "Check logs for details:"
    echo "  tail -30 $LOG_FILE"
    echo ""
    echo "Last 30 lines of log:"
    tail -30 "$LOG_FILE" 2>/dev/null || echo "Log file not found"
    rm -f "$PID_FILE"
    exit 1
fi
