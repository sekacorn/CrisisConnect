#!/bin/bash

# CrisisConnect - Test Runner Script
# Runs all tests: backend unit tests, frontend unit tests, and E2E tests

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo -e "${GREEN}=== CrisisConnect Test Suite ===${NC}"

# Run backend tests
echo -e "${YELLOW}Running backend tests...${NC}"
cd backend
mvn test
if [ $? -eq 0 ]; then
  echo -e "${GREEN}Backend tests passed${NC}"
else
  echo -e "${RED}Backend tests failed${NC}"
  exit 1
fi
cd ..

# Run frontend tests
echo -e "${YELLOW}Running frontend tests...${NC}"
cd frontend
npm test -- --coverage --watchAll=false
if [ $? -eq 0 ]; then
  echo -e "${GREEN}Frontend tests passed${NC}"
else
  echo -e "${RED}Frontend tests failed${NC}"
  exit 1
fi
cd ..

# Run E2E tests (if Cypress is installed)
echo -e "${YELLOW}Running E2E tests...${NC}"
cd e2e
if [ -d "node_modules" ]; then
  npm test
  if [ $? -eq 0 ]; then
    echo -e "${GREEN}E2E tests passed${NC}"
  else
    echo -e "${RED}E2E tests failed${NC}"
    exit 1
  fi
else
  echo -e "${YELLOW}E2E dependencies not installed. Run 'npm install' in e2e directory.${NC}"
fi
cd ..

echo ""
echo -e "${GREEN}=== All tests passed ===${NC}"
