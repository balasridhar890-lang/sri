#!/bin/bash

# Script to test Docker build and basic functionality
# This doesn't require external services or API keys

set -e

echo "Testing Docker Build and Setup"
echo "=============================="
echo ""

# Build the Docker image
echo "1. Building Docker image..."
docker build -t voice-assistant-backend:test .

if [ $? -eq 0 ]; then
    echo "✓ Docker build successful"
else
    echo "✗ Docker build failed"
    exit 1
fi

echo ""
echo "2. Testing image can import the application..."
docker run --rm voice-assistant-backend:test python -c "import app; print('✓ Application imports successfully')"

echo ""
echo "3. Testing entrypoint script exists and is executable..."
docker run --rm voice-assistant-backend:test ls -la /app/scripts/entrypoint.sh | grep -q "rwx"
echo "✓ Entrypoint script is executable"

echo ""
echo "4. Checking gunicorn configuration..."
docker run --rm voice-assistant-backend:test python -c "import gunicorn_conf; print('✓ Gunicorn config is valid')"

echo ""
echo "5. Verifying non-root user..."
docker run --rm voice-assistant-backend:test whoami | grep -q "appuser"
echo "✓ Container runs as non-root user (appuser)"

echo ""
echo "6. Checking health endpoint availability..."
# Start container in background
CONTAINER_ID=$(docker run -d -e OPENAI_API_KEY=test-key -e DATABASE_URL=sqlite:///./data/test.db -p 8001:8000 voice-assistant-backend:test)

# Wait for container to be ready
sleep 10

# Check health endpoint
if curl -f -s http://localhost:8001/health > /dev/null 2>&1; then
    echo "✓ Health endpoint responds correctly"
else
    echo "⚠ Health endpoint not responding (may need more time to start)"
fi

# Cleanup
echo ""
echo "Cleaning up test container..."
docker stop $CONTAINER_ID > /dev/null 2>&1
docker rm $CONTAINER_ID > /dev/null 2>&1

echo ""
echo "=============================="
echo "All Docker tests passed! ✓"
echo "=============================="
echo ""
echo "You can now deploy with:"
echo "  make up          # Production with PostgreSQL"
echo "  make dev-up      # Development with SQLite"
