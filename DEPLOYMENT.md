# Deployment Guide - Voice Assistant Backend

This guide covers production deployment of the FastAPI backend using Docker, docker-compose, and CI/CD pipelines.

## Table of Contents
- [Quick Start](#quick-start)
- [Docker Deployment](#docker-deployment)
- [Environment Configuration](#environment-configuration)
- [Database Setup](#database-setup)
- [Security Considerations](#security-considerations)
- [Monitoring & Logging](#monitoring--logging)
- [CI/CD Pipeline](#cicd-pipeline)
- [Production Checklist](#production-checklist)
- [Troubleshooting](#troubleshooting)

## Quick Start

### Development Environment
```bash
# Clone repository
git clone <repository-url>
cd voice-assistant

# Start development environment with SQLite
make dev-up

# View logs
make logs

# Stop environment
make dev-down
```

### Production Environment
```bash
# Configure environment
cp .env.production .env
# Edit .env with your production settings

# Build and start production stack (PostgreSQL + Backend)
make build
make up

# Check status
docker compose ps

# View logs
make logs
```

## Docker Deployment

### Architecture

The deployment consists of two services:

1. **Backend (FastAPI)**: Python application with Gunicorn/Uvicorn workers
2. **Database (PostgreSQL)**: Persistent data storage

```
┌─────────────────────────────────────────────┐
│                Load Balancer                 │
│            (nginx/traefik/etc)               │
└───────────────────┬─────────────────────────┘
                    │
        ┌───────────▼────────────┐
        │   Backend Container    │
        │                        │
        │   Gunicorn (master)    │
        │   ├── Worker 1         │
        │   ├── Worker 2         │
        │   ├── Worker 3         │
        │   └── Worker 4         │
        │                        │
        │   Port: 8000           │
        └───────────┬────────────┘
                    │
        ┌───────────▼────────────┐
        │  PostgreSQL Container  │
        │                        │
        │  Database: voiceassistant
        │  Port: 5432            │
        └────────────────────────┘
```

### Building the Image

```bash
# Build production image
docker build -t voice-assistant-backend:latest .

# Build with specific tag
docker build -t voice-assistant-backend:v1.0.0 .

# Build using make
make build
```

### Running with Docker Compose

#### Production (PostgreSQL)
```bash
# Start services
docker compose up -d

# View logs
docker compose logs -f backend

# Scale workers (if needed)
docker compose up -d --scale backend=3

# Stop services
docker compose down

# Stop and remove volumes (WARNING: deletes data)
docker compose down -v
```

#### Development (SQLite)
```bash
# Start development environment
docker compose -f compose.dev.yaml up -d

# Hot reload is not enabled by default
# For development with hot reload, use local Python installation
```

### Dockerfile Overview

The Dockerfile uses a multi-stage build:

1. **Base Stage**: Installs Python dependencies
2. **Production Stage**: Copies application code and sets up non-root user

Key features:
- Non-root user (appuser) for security
- Health checks every 30 seconds
- Persistent volumes for data and logs
- Optimized layer caching

## Environment Configuration

### Required Environment Variables

Create a `.env` file in the project root:

```bash
# Copy production template
cp .env.production .env

# Edit with your settings
nano .env
```

### Critical Variables

#### OpenAI API Key (REQUIRED)
```env
OPENAI_API_KEY=sk-proj-xxxxxxxxxxxxxxxxxxxxx
```

Get your API key from: https://platform.openai.com/api-keys

⚠️ **Security Warning**: Never commit API keys to version control!

#### Database Configuration (Production)
```env
DATABASE_URL=postgresql+asyncpg://postgres:SECURE_PASSWORD@db:5432/voiceassistant
POSTGRES_USER=postgres
POSTGRES_PASSWORD=SECURE_PASSWORD_HERE
POSTGRES_DB=voiceassistant
```

#### Application Settings
```env
APP_NAME=Voice Assistant Backend
DEBUG=false
LOG_LEVEL=INFO
STRUCTURED_LOGGING=true
```

#### Worker Configuration
```env
# Number of Gunicorn workers
# Formula: (2 x CPU cores) + 1
GUNICORN_WORKERS=4
```

#### CORS Configuration
```env
# Restrict to your frontend domains in production
CORS_ORIGINS=["https://yourdomain.com","https://app.yourdomain.com"]
CORS_CREDENTIALS=true
CORS_METHODS=["GET","POST","PUT","DELETE","OPTIONS"]
CORS_HEADERS=["*"]
```

### Environment Variables Priority

1. Docker Compose `environment` section
2. `.env` file in project root
3. Default values in `app/config.py`

## Database Setup

### PostgreSQL (Production)

PostgreSQL is automatically started by docker-compose:

```bash
# Start database
docker compose up -d db

# Check database status
docker compose exec db pg_isready -U postgres

# Access database shell
make db-shell
# or
docker compose exec db psql -U postgres -d voiceassistant
```

### Database Migrations

Migrations run automatically on container startup via `scripts/entrypoint.sh`.

Manual migration:
```bash
# Run migrations
make migrate

# or directly
docker compose exec backend python -c "
import asyncio
from app.database import init_db
asyncio.run(init_db())
"
```

### Future Migration Tools

For production, consider using Alembic for database migrations:

```bash
# Install Alembic
pip install alembic

# Initialize
alembic init alembic

# Generate migration
alembic revision --autogenerate -m "description"

# Apply migrations
alembic upgrade head
```

### Backup & Restore

#### Backup
```bash
# Backup PostgreSQL database
docker compose exec db pg_dump -U postgres voiceassistant > backup_$(date +%Y%m%d_%H%M%S).sql

# Backup Docker volume
docker run --rm -v voice-assistant_postgres-data:/data -v $(pwd):/backup alpine tar czf /backup/postgres-backup.tar.gz -C /data .
```

#### Restore
```bash
# Restore from SQL dump
cat backup_20240115_120000.sql | docker compose exec -T db psql -U postgres voiceassistant

# Restore from volume backup
docker run --rm -v voice-assistant_postgres-data:/data -v $(pwd):/backup alpine tar xzf /backup/postgres-backup.tar.gz -C /data
```

## Security Considerations

### API Key Management

1. **Never commit secrets to Git**
   - Use `.env` files (included in `.gitignore`)
   - Use secret management tools (Vault, AWS Secrets Manager)

2. **Rotate API keys regularly**
   - OpenAI allows multiple API keys
   - Rotate without downtime

3. **Use environment-specific keys**
   - Development keys
   - Staging keys
   - Production keys

### Container Security

1. **Non-root user**: Application runs as `appuser` (UID 1000)

2. **Minimal base image**: Using `python:3.11-slim`

3. **Security updates**: Regularly rebuild images
   ```bash
   docker compose pull
   docker compose up -d --build
   ```

4. **Network isolation**: Services communicate on isolated bridge network

5. **Volume permissions**: Data directories owned by `appuser`

### Database Security

1. **Strong passwords**: Use 32+ character random passwords
   ```bash
   # Generate secure password
   openssl rand -base64 32
   ```

2. **Network isolation**: Database not exposed to public internet
   - Remove `ports` section in `compose.yaml` for production
   - Access only via backend container

3. **Connection encryption**: Use SSL/TLS for database connections
   ```env
   DATABASE_URL=postgresql+asyncpg://user:pass@host:5432/db?ssl=require
   ```

### CORS Configuration

Restrict CORS origins in production:

```env
# Development (permissive)
CORS_ORIGINS=["*"]

# Production (restrictive)
CORS_ORIGINS=["https://yourdomain.com","https://app.yourdomain.com"]
```

### HTTPS/SSL

For production, use a reverse proxy (nginx, traefik) with SSL:

```nginx
server {
    listen 443 ssl http2;
    server_name api.yourdomain.com;
    
    ssl_certificate /etc/ssl/certs/cert.pem;
    ssl_certificate_key /etc/ssl/private/key.pem;
    
    location / {
        proxy_pass http://backend:8000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;
    }
}
```

## Monitoring & Logging

### Structured Logging

Enable structured JSON logging for production:

```env
STRUCTURED_LOGGING=true
LOG_LEVEL=INFO
```

Log format:
```json
{
  "timestamp": "2024-01-15T10:30:00+0000",
  "level": "INFO",
  "logger": "app.services",
  "message": "Conversation processed for user 1 in 1250.50ms"
}
```

### Log Collection

#### View logs
```bash
# Follow backend logs
docker compose logs -f backend

# All services
docker compose logs -f

# Last 100 lines
docker compose logs --tail=100 backend
```

#### Export logs
```bash
# Export to file
docker compose logs backend > logs/backend_$(date +%Y%m%d).log
```

#### Log aggregation tools
- **ELK Stack**: Elasticsearch, Logstash, Kibana
- **Loki**: Grafana Loki with Promtail
- **CloudWatch**: AWS CloudWatch Logs
- **Datadog**: Datadog APM

Example Promtail config:
```yaml
scrape_configs:
  - job_name: voice-assistant-backend
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        regex: 'voice-assistant-backend'
        action: keep
```

### Health Checks

#### Built-in health endpoint
```bash
# Check health
curl http://localhost:8000/health

# Expected response
{
  "status": "healthy",
  "timestamp": "2024-01-15T10:30:00",
  "version": "0.1.0"
}
```

#### Docker health checks
```bash
# Check container health
docker compose ps

# Detailed health status
docker inspect --format='{{json .State.Health}}' voice-assistant-backend | jq
```

### Performance Monitoring

#### Response time monitoring

Backend logs include processing time:
```
INFO: Conversation processed for user 1 in 1250.50ms
WARNING: Slow processing detected: 2500.00ms for user 2
```

#### Prometheus metrics (future)

Add prometheus client:
```python
# app/metrics.py
from prometheus_client import Counter, Histogram

request_count = Counter('requests_total', 'Total requests')
request_duration = Histogram('request_duration_seconds', 'Request duration')
```

#### Application Performance Monitoring (APM)

Consider integrating:
- **Sentry**: Error tracking
- **New Relic**: Full stack monitoring
- **Datadog**: Infrastructure and APM
- **OpenTelemetry**: Distributed tracing

## CI/CD Pipeline

### GitHub Actions

The project includes a comprehensive CI pipeline in `.github/workflows/backend-ci.yaml`.

#### Pipeline Stages

1. **Lint & Format Check**: Black, isort, flake8
2. **Type Check**: mypy type validation
3. **Tests**: pytest across Python 3.9, 3.10, 3.11
4. **Docker Build**: Build and test Docker image
5. **Security Scan**: Safety (vulnerabilities) and Bandit (security)

#### Triggering CI

CI runs on:
- Push to `main`, `develop`, `deploy/**` branches
- Pull requests to `main`, `develop`
- Changes to backend code or Docker files

#### Environment Secrets

Configure in GitHub Settings > Secrets and variables > Actions:

```
OPENAI_API_KEY=sk-test-key-for-ci  # Test key or mock
```

#### Manual workflow trigger

```bash
# Push to trigger CI
git push origin main

# Or push to deploy branch
git checkout -b deploy/feature-name
git push origin deploy/feature-name
```

### CD Pipeline (Deployment)

Example GitHub Actions deploy job:

```yaml
deploy:
  name: Deploy to Production
  runs-on: ubuntu-latest
  needs: [lint, test, docker-build]
  if: github.ref == 'refs/heads/main'
  steps:
    - name: Deploy to server
      uses: appleboy/ssh-action@master
      with:
        host: ${{ secrets.DEPLOY_HOST }}
        username: ${{ secrets.DEPLOY_USER }}
        key: ${{ secrets.DEPLOY_KEY }}
        script: |
          cd /opt/voice-assistant
          git pull origin main
          docker compose pull
          docker compose up -d --build
```

### Continuous Deployment Options

1. **Direct SSH**: Deploy via SSH (shown above)
2. **Docker Registry**: Push to Docker Hub/ECR, pull on server
3. **Kubernetes**: Deploy to K8s cluster
4. **Cloud Platforms**: 
   - AWS ECS/Fargate
   - Google Cloud Run
   - Azure Container Instances
   - DigitalOcean App Platform

## Production Checklist

### Pre-deployment

- [ ] Set `DEBUG=false`
- [ ] Set strong `POSTGRES_PASSWORD`
- [ ] Configure valid `OPENAI_API_KEY`
- [ ] Restrict `CORS_ORIGINS` to your domains
- [ ] Enable `STRUCTURED_LOGGING=true`
- [ ] Set appropriate `GUNICORN_WORKERS`
- [ ] Configure SSL certificates
- [ ] Set up log aggregation
- [ ] Configure monitoring/alerting
- [ ] Test backup and restore procedures

### Deployment

- [ ] Build Docker image
- [ ] Run database migrations
- [ ] Start containers
- [ ] Verify health endpoint
- [ ] Test API endpoints
- [ ] Check logs for errors
- [ ] Monitor performance metrics

### Post-deployment

- [ ] Set up automated backups
- [ ] Configure monitoring alerts
- [ ] Document deployment process
- [ ] Create runbook for incidents
- [ ] Schedule regular security updates

## Troubleshooting

### Container won't start

```bash
# Check logs
docker compose logs backend

# Common issues:
# 1. Port already in use
docker compose down
lsof -i :8000  # Find process using port
kill -9 <PID>

# 2. Database connection failed
docker compose logs db
docker compose exec db pg_isready -U postgres

# 3. Environment variables missing
docker compose config  # View resolved configuration
```

### Database connection errors

```bash
# Check database is running
docker compose ps db

# Check connection from backend
docker compose exec backend pg_isready -h db -p 5432 -U postgres

# Check database logs
docker compose logs db

# Restart database
docker compose restart db
```

### OpenAI API errors

```bash
# Check logs for API errors
docker compose logs backend | grep -i "openai"

# Verify API key
docker compose exec backend env | grep OPENAI_API_KEY

# Test API key
curl https://api.openai.com/v1/models \
  -H "Authorization: Bearer $OPENAI_API_KEY"
```

### Performance issues

```bash
# Check resource usage
docker stats

# Scale workers
GUNICORN_WORKERS=8 docker compose up -d

# Check slow queries
docker compose logs backend | grep "WARNING.*Slow"
```

### Data persistence

```bash
# List volumes
docker volume ls

# Inspect volume
docker volume inspect voice-assistant_postgres-data

# Backup volume before troubleshooting
docker run --rm -v voice-assistant_postgres-data:/data -v $(pwd):/backup alpine tar czf /backup/emergency-backup.tar.gz -C /data .
```

## Support & Resources

- **FastAPI Docs**: https://fastapi.tiangolo.com/
- **Docker Docs**: https://docs.docker.com/
- **PostgreSQL Docs**: https://www.postgresql.org/docs/
- **Gunicorn Docs**: https://docs.gunicorn.org/

## Next Steps

1. Set up reverse proxy (nginx/traefik)
2. Implement Alembic migrations
3. Add Prometheus metrics
4. Set up ELK or Loki for log aggregation
5. Configure automated backups
6. Implement blue-green deployment
7. Add rate limiting
8. Implement caching (Redis)
