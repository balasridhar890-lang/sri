#!/bin/bash

# Quick Start Script for Voice Assistant Backend
# This script helps you get started quickly with the backend

set -e

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

echo "=================================="
echo "Voice Assistant Backend Quick Start"
echo "=================================="
echo ""

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}Error: Docker is not installed${NC}"
    echo "Please install Docker from: https://docs.docker.com/get-docker/"
    exit 1
fi

# Check if Docker Compose is installed
if ! command -v docker compose &> /dev/null; then
    echo -e "${RED}Error: Docker Compose is not installed${NC}"
    echo "Please install Docker Compose from: https://docs.docker.com/compose/install/"
    exit 1
fi

echo -e "${GREEN}✓ Docker and Docker Compose are installed${NC}"
echo ""

# Check if .env file exists
if [ ! -f .env ]; then
    echo -e "${YELLOW}⚠ No .env file found${NC}"
    echo "Would you like to:"
    echo "  1) Start development environment (SQLite, no API key required)"
    echo "  2) Create production .env file (PostgreSQL, requires OpenAI API key)"
    echo "  3) Exit and create .env manually"
    echo ""
    read -p "Choose option [1-3]: " option
    
    case $option in
        1)
            echo ""
            echo "Starting development environment..."
            echo "This will use SQLite and mock OpenAI responses for testing"
            docker compose -f compose.dev.yaml up -d
            echo ""
            echo -e "${GREEN}✓ Development environment started!${NC}"
            echo ""
            echo "Access the API at: http://localhost:8000"
            echo "API Documentation: http://localhost:8000/docs"
            echo ""
            echo "To view logs: docker compose -f compose.dev.yaml logs -f"
            echo "To stop: docker compose -f compose.dev.yaml down"
            exit 0
            ;;
        2)
            echo ""
            read -p "Enter your OpenAI API Key: " api_key
            if [ -z "$api_key" ]; then
                echo -e "${RED}Error: API key cannot be empty${NC}"
                exit 1
            fi
            
            # Create .env from template
            cp .env.production .env
            
            # Replace API key
            sed -i.bak "s/sk-your-production-api-key-here/$api_key/" .env
            rm .env.bak
            
            # Generate secure database password
            db_password=$(openssl rand -base64 32 | tr -d "=+/" | cut -c1-32)
            sed -i.bak "s/CHANGE_ME_PASSWORD/$db_password/g" .env
            rm .env.bak
            
            echo -e "${GREEN}✓ .env file created with secure credentials${NC}"
            echo ""
            ;;
        3)
            echo ""
            echo "Please create a .env file based on .env.production"
            echo "Then run this script again or use: make up"
            exit 0
            ;;
        *)
            echo -e "${RED}Invalid option${NC}"
            exit 1
            ;;
    esac
fi

# Start production environment
echo "Building and starting production environment..."
echo ""

# Build the image
echo "Building Docker image..."
docker compose build

# Start services
echo "Starting services..."
docker compose up -d

# Wait for services to be healthy
echo ""
echo "Waiting for services to be ready..."
sleep 5

# Check health
max_attempts=30
attempt=0
while [ $attempt -lt $max_attempts ]; do
    if curl -f -s http://localhost:8000/health > /dev/null 2>&1; then
        echo -e "${GREEN}✓ Backend is healthy!${NC}"
        break
    fi
    attempt=$((attempt + 1))
    echo "Attempt $attempt/$max_attempts..."
    sleep 2
done

if [ $attempt -eq $max_attempts ]; then
    echo -e "${RED}✗ Backend failed to become healthy${NC}"
    echo "Check logs with: docker compose logs backend"
    exit 1
fi

echo ""
echo "=================================="
echo -e "${GREEN}✓ Voice Assistant Backend is running!${NC}"
echo "=================================="
echo ""
echo "API URL: http://localhost:8000"
echo "API Documentation: http://localhost:8000/docs"
echo "Health Check: http://localhost:8000/health"
echo ""
echo "Useful commands:"
echo "  make logs       - View application logs"
echo "  make shell      - Open shell in backend container"
echo "  make db-shell   - Open PostgreSQL shell"
echo "  make down       - Stop services"
echo "  make clean      - Stop and remove all data"
echo ""
echo "See DEPLOYMENT.md for more information"
echo ""
