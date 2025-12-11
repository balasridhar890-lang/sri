# Scripts Directory

This directory contains utility scripts for deployment, testing, and management of the Voice Assistant Backend.

## Scripts Overview

### entrypoint.sh

**Purpose**: Docker container entrypoint script that handles initialization tasks before starting the application.

**What it does**:
1. Waits for PostgreSQL database to be ready (if using PostgreSQL)
2. Runs database migrations
3. Starts the application

**Usage**:
```bash
# Automatically called by Docker
# You don't need to run this manually
```

**Logs**:
```
=== Voice Assistant Backend Entrypoint ===
Starting initialization...
Waiting for database to be ready...
Database is ready!
Running database migrations...
Migrations completed successfully
Starting application...
```

### quick-start.sh

**Purpose**: Interactive script to quickly get started with the backend.

**What it does**:
1. Checks for Docker and Docker Compose
2. Guides you through environment setup
3. Starts the appropriate environment (dev or production)
4. Verifies the application is healthy

**Usage**:
```bash
./scripts/quick-start.sh
```

**Interactive Prompts**:
- Development environment (no API key needed)
- Production environment (requires OpenAI API key)
- Manual configuration

**Example**:
```bash
$ ./scripts/quick-start.sh
==================================
Voice Assistant Backend Quick Start
==================================

✓ Docker and Docker Compose are installed

⚠ No .env file found
Would you like to:
  1) Start development environment (SQLite, no API key required)
  2) Create production .env file (PostgreSQL, requires OpenAI API key)
  3) Exit and create .env manually

Choose option [1-3]: 1

Starting development environment...
✓ Development environment started!

Access the API at: http://localhost:8000
API Documentation: http://localhost:8000/docs
```

### test-docker.sh

**Purpose**: Test Docker build and basic functionality without external dependencies.

**What it does**:
1. Builds the Docker image
2. Verifies application imports
3. Checks entrypoint script
4. Validates Gunicorn configuration
5. Verifies non-root user
6. Tests health endpoint

**Usage**:
```bash
./scripts/test-docker.sh
```

**Example Output**:
```
Testing Docker Build and Setup
==============================

1. Building Docker image...
✓ Docker build successful

2. Testing image can import the application...
✓ Application imports successfully

3. Testing entrypoint script exists and is executable...
✓ Entrypoint script is executable

4. Checking gunicorn configuration...
✓ Gunicorn config is valid

5. Verifying non-root user...
✓ Container runs as non-root user (appuser)

6. Checking health endpoint availability...
✓ Health endpoint responds correctly

==============================
All Docker tests passed! ✓
==============================
```

## When to Use Each Script

### Quick Start (quick-start.sh)
Use when:
- First time setting up the project
- Need guided setup
- Want to quickly start the backend

### Test Docker (test-docker.sh)
Use when:
- Verifying Docker build after code changes
- Testing before deployment
- CI/CD pipeline validation
- Debugging Docker issues

### Entrypoint (entrypoint.sh)
Use when:
- Automatically used by Docker
- Customizing initialization steps
- Debugging startup issues

## Script Maintenance

### Adding New Scripts

When adding new scripts to this directory:

1. Make them executable:
   ```bash
   chmod +x scripts/your-script.sh
   ```

2. Add proper error handling:
   ```bash
   set -e  # Exit on error
   set -u  # Exit on undefined variable
   ```

3. Add descriptive output:
   ```bash
   echo "Step 1: Doing something..."
   echo "✓ Step completed successfully"
   ```

4. Document in this README

### Testing Scripts

Always test scripts in a clean environment:

```bash
# Create test environment
docker run -it --rm -v $(pwd):/app ubuntu:22.04 bash

# In container
cd /app
./scripts/your-script.sh
```

## Common Issues

### Permission Denied

If you get "Permission denied" errors:

```bash
chmod +x scripts/*.sh
```

### Line Endings (Windows)

If scripts fail with strange errors on Linux, check line endings:

```bash
# Convert CRLF to LF
dos2unix scripts/*.sh

# Or with sed
sed -i 's/\r$//' scripts/*.sh
```

### Script Not Found

Make sure you're running from the project root:

```bash
cd /path/to/voice-assistant
./scripts/quick-start.sh
```

## Integration with Makefile

Scripts can be called from the Makefile:

```makefile
quickstart:
	./scripts/quick-start.sh

test-docker:
	./scripts/test-docker.sh
```

Usage:
```bash
make quickstart
make test-docker
```

## Environment Variables

Scripts respect these environment variables:

- `DATABASE_URL`: Database connection string
- `OPENAI_API_KEY`: OpenAI API key
- `DEBUG`: Enable debug mode
- `LOG_LEVEL`: Set logging level

Example:
```bash
DATABASE_URL=sqlite:///./test.db ./scripts/test-docker.sh
```

## Security Notes

- Never commit `.env` files with secrets
- Scripts should not log sensitive information
- Use secure passwords for production databases
- Validate user input in interactive scripts

## Contributing

When contributing scripts:

1. Follow existing patterns
2. Add comprehensive error handling
3. Provide clear user feedback
4. Document in this README
5. Test on multiple platforms (Linux, macOS, Windows WSL)
