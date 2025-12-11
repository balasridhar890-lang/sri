#!/bin/bash
set -e

echo "=== Voice Assistant Backend Entrypoint ==="
echo "Starting initialization..."

# Function to wait for database
wait_for_db() {
    echo "Waiting for database to be ready..."
    
    if [[ $DATABASE_URL == postgresql* ]]; then
        # Extract database connection details
        DB_HOST=$(echo $DATABASE_URL | sed -n 's/.*@\([^:]*\).*/\1/p')
        DB_PORT=$(echo $DATABASE_URL | sed -n 's/.*:\([0-9]*\)\/.*/\1/p')
        
        echo "Checking PostgreSQL at $DB_HOST:$DB_PORT"
        
        max_attempts=30
        attempt=0
        
        until pg_isready -h "$DB_HOST" -p "$DB_PORT" -U postgres > /dev/null 2>&1 || [ $attempt -eq $max_attempts ]; do
            attempt=$((attempt + 1))
            echo "Attempt $attempt/$max_attempts: Database not ready yet..."
            sleep 2
        done
        
        if [ $attempt -eq $max_attempts ]; then
            echo "ERROR: Database failed to become ready in time"
            exit 1
        fi
        
        echo "Database is ready!"
    else
        echo "Using SQLite - no wait needed"
    fi
}

# Function to run database migrations
run_migrations() {
    echo "Running database migrations..."
    
    # For now, we use SQLModel's create_all which is called on app startup
    # In the future, you can integrate Alembic for proper migrations
    # Example: alembic upgrade head
    
    python -c "
import asyncio
from app.database import init_db

async def main():
    await init_db()
    print('Database tables created/verified successfully')

asyncio.run(main())
    "
    
    if [ $? -eq 0 ]; then
        echo "Migrations completed successfully"
    else
        echo "ERROR: Migrations failed"
        exit 1
    fi
}

# Main execution
echo "Environment: ${DEBUG:-false}"
echo "Log Level: ${LOG_LEVEL:-INFO}"

# Wait for database if using PostgreSQL
wait_for_db

# Run migrations
run_migrations

echo "Initialization complete. Starting application..."
echo "============================================"

# Execute the main command (CMD from Dockerfile)
exec "$@"
