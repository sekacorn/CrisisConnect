#!/bin/bash

# CrisisConnect - Backend Start Script (Linux/Mac)
# Builds and starts the Spring Boot backend with PostgreSQL

set -e  # Exit on error

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BACKEND_DIR="$SCRIPT_DIR/backend"
LOG_FILE="$SCRIPT_DIR/backend.log"
PID_FILE="$SCRIPT_DIR/backend.pid"

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}========================================${NC}"
echo -e "${GREEN}CrisisConnect Backend Startup${NC}"
echo -e "${GREEN}========================================${NC}"

# Check if Java is installed
if ! command -v java &> /dev/null; then
    echo -e "${RED}Error: Java is not installed${NC}"
    echo "Please install Java 17 or higher"
    exit 1
fi

# Check Java version
JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}' | awk -F'.' '{print $1}')
if [ "$JAVA_VERSION" -lt 17 ]; then
    echo -e "${RED}Error: Java 17 or higher is required${NC}"
    echo "Current version: $(java -version 2>&1 | head -n 1)"
    exit 1
fi

# Check if Maven is installed
if ! command -v mvn &> /dev/null; then
    echo -e "${RED}Error: Maven is not installed${NC}"
    echo "Please install Maven 3.6 or higher"
    exit 1
fi

# Parse arguments
CLEAN_BUILD=false
SKIP_TESTS=false
USE_H2=false

while [[ $# -gt 0 ]]; do
    case $1 in
        -c|--clean)
            CLEAN_BUILD=true
            shift
            ;;
        -s|--skip-tests)
            SKIP_TESTS=true
            shift
            ;;
        --h2)
            USE_H2=true
            shift
            ;;
        -h|--help)
            echo "Usage: ./start-backend.sh [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -c, --clean       Clean build (mvn clean)"
            echo "  -s, --skip-tests  Skip running tests"
            echo "  --h2              Use H2 database instead of PostgreSQL"
            echo "  -h, --help        Show this help message"
            exit 0
            ;;
        *)
            echo -e "${RED}Unknown option: $1${NC}"
            echo "Use -h or --help for usage information"
            exit 1
            ;;
    esac
done

# Check if backend is already running
if [ -f "$PID_FILE" ]; then
    OLD_PID=$(cat "$PID_FILE")
    if ps -p "$OLD_PID" > /dev/null 2>&1; then
        echo -e "${YELLOW}Backend is already running (PID: $OLD_PID)${NC}"
        echo "Stop it first with: kill $OLD_PID"
        exit 1
    else
        echo -e "${YELLOW}Removing stale PID file${NC}"
        rm -f "$PID_FILE"
    fi
fi

# Check database configuration
if [ "$USE_H2" = true ]; then
    echo -e "${YELLOW}Using H2 in-memory database${NC}"
    echo -e "${BLUE}H2 Console will be available at: http://localhost:8080/h2-console${NC}"
else
    # Check if .env file exists (but don't block H2 mode)
    if [ ! -f "$SCRIPT_DIR/.env" ]; then
        echo -e "${YELLOW}Warning: .env file not found${NC}"
        if [ -f "$SCRIPT_DIR/.env.example" ]; then
            echo "Creating .env from .env.example..."
            cp "$SCRIPT_DIR/.env.example" "$SCRIPT_DIR/.env"
            echo -e "${YELLOW}Please edit .env and set your configuration${NC}"
            echo -e "${RED}DO NOT use default secrets in production!${NC}"
        else
            echo -e "${YELLOW}Note: .env.example not found${NC}"
            echo -e "${YELLOW}Tip: Use --h2 flag to start with H2 database instead${NC}"
        fi
    fi

    # Load environment variables if .env exists
    if [ -f "$SCRIPT_DIR/.env" ]; then
        set -a
        source "$SCRIPT_DIR/.env"
        set +a
    fi

    # Check if PostgreSQL is accessible (but don't block startup)
    echo -e "${YELLOW}Checking PostgreSQL connection...${NC}"
    if command -v psql &> /dev/null; then
        if psql -h "${DB_HOST:-localhost}" -p "${DB_PORT:-5432}" -U "${DB_USERNAME:-crisisconnect}" -d "${DB_NAME:-crisisconnect}" -c "SELECT 1" &> /dev/null 2>&1; then
            echo -e "${GREEN}PostgreSQL connection OK${NC}"
        else
            echo -e "${YELLOW}Warning: Cannot connect to PostgreSQL${NC}"
            echo "Host: ${DB_HOST:-localhost}:${DB_PORT:-5432}"
            echo "Database: ${DB_NAME:-crisisconnect}"
            echo -e "${YELLOW}Proceeding anyway - connection may succeed during startup${NC}"
        fi
    else
        echo -e "${YELLOW}psql not found, skipping database connection check${NC}"
    fi
fi

# Navigate to backend directory
cd "$BACKEND_DIR"

# Build only if requested (skip by default for faster startup)
if [ "$CLEAN_BUILD" = true ]; then
    echo -e "${YELLOW}Building backend...${NC}"
    if [ "$SKIP_TESTS" = true ]; then
        mvn clean install -DskipTests
    else
        mvn clean install
    fi

    if [ $? -ne 0 ]; then
        echo -e "${RED}Build failed!${NC}"
        exit 1
    fi
    echo -e "${GREEN}Build successful!${NC}"
fi

echo -e "${YELLOW}Starting backend server...${NC}"
echo "Log file: $LOG_FILE"
echo ""

# Build Spring Boot arguments
SPRING_ARGS=""

if [ "$USE_H2" = true ]; then
    echo "Using H2 in-memory database"
    SPRING_ARGS="--spring.datasource.url=jdbc:h2:mem:crisisconnect"
    SPRING_ARGS="$SPRING_ARGS --spring.datasource.driver-class-name=org.h2.Driver"
    SPRING_ARGS="$SPRING_ARGS --spring.jpa.database-platform=org.hibernate.dialect.H2Dialect"
    SPRING_ARGS="$SPRING_ARGS --spring.h2.console.enabled=true"
    SPRING_ARGS="$SPRING_ARGS --spring.jpa.hibernate.ddl-auto=create-drop"
    SPRING_ARGS="$SPRING_ARGS --admin.bootstrap.enabled=true"
    SPRING_ARGS="$SPRING_ARGS --admin.bootstrap.email=admin@crisisconnect.org"
    SPRING_ARGS="$SPRING_ARGS --admin.bootstrap.password=Admin2026!Secure"
    SPRING_ARGS="$SPRING_ARGS --admin.bootstrap.name=System Administrator"
fi

# Start using mvn spring-boot:run (more reliable than JAR approach)
if [ "$USE_H2" = true ]; then
    nohup mvn spring-boot:run -Dspring-boot.run.arguments="$SPRING_ARGS" > "$LOG_FILE" 2>&1 &
else
    nohup mvn spring-boot:run > "$LOG_FILE" 2>&1 &
fi

BACKEND_PID=$!

# Save PID
echo $BACKEND_PID > "$PID_FILE"

# Wait for backend to start (increased from 5s to 10s)
echo "Waiting for backend to start..."
sleep 10

# Check if process is still running AND port is listening
BACKEND_STARTED=false
if ps -p $BACKEND_PID > /dev/null 2>&1; then
    # Check if port 8080 is listening (if lsof is available)
    if command -v lsof &> /dev/null; then
        if lsof -ti:8080 > /dev/null 2>&1; then
            BACKEND_STARTED=true
        fi
    else
        # Fallback: just check if process is running
        BACKEND_STARTED=true
    fi
fi

if [ "$BACKEND_STARTED" = true ]; then
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}Backend started successfully!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo "PID: $BACKEND_PID"
    echo "Port: 8080"
    echo "API: http://localhost:8080/api"

    if [ "$USE_H2" = true ]; then
        echo "H2 Console: http://localhost:8080/h2-console"
        echo ""
        echo -e "${YELLOW}H2 Console Login:${NC}"
        echo "  JDBC URL: jdbc:h2:mem:crisisconnect"
        echo "  Username: sa"
        echo "  Password: (leave empty)"
        echo ""
        echo -e "${YELLOW}Bootstrap Admin Account (NIST-COMPLIANT):${NC}"
        echo "  Email: admin@crisisconnect.org"
        echo "  Password: Admin2026!Secure"
        echo "  Note: 12+ chars, mixed case, numbers, special chars"
        echo ""
        echo -e "${RED}Note: Using H2 in-memory database - data will be lost on restart${NC}"
    fi

    echo ""
    echo "To view logs:"
    echo "  tail -f $LOG_FILE"
    echo ""
    echo "To stop backend:"
    echo "  ./stop-backend.sh"
else
    echo -e "${RED}Backend failed to start. Check logs:${NC}"
    echo "  tail -20 $LOG_FILE"
    echo ""
    echo "Last 20 lines of log:"
    tail -20 "$LOG_FILE" 2>/dev/null || echo "Log file not found"
    rm -f "$PID_FILE"
    exit 1
fi
