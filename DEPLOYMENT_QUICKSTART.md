# Deployment Quick Start

This guide gets you up and running in minutes. For complete documentation, see [DEPLOYMENT.md](DEPLOYMENT.md).

## Prerequisites

- Docker and Docker Compose installed
- OpenAI API key (get from https://platform.openai.com/api-keys)

## Option 1: Interactive Quick Start (Recommended)

```bash
./scripts/quick-start.sh
```

This script will:
1. Check your environment
2. Guide you through setup
3. Start the backend
4. Verify it's running

## Option 2: Manual Setup

### Development (SQLite, No API Key Required)

```bash
make dev-up
```

Access at: http://localhost:8000

### Production (PostgreSQL, Requires API Key)

1. **Create environment file:**
   ```bash
   cp .env.production .env
   ```

2. **Edit `.env` and set:**
   - `OPENAI_API_KEY=sk-your-actual-key-here`
   - `POSTGRES_PASSWORD=secure-password-here`

3. **Start services:**
   ```bash
   make build
   make up
   ```

4. **Verify health:**
   ```bash
   curl http://localhost:8000/health
   ```

## Available Endpoints

- **API Docs**: http://localhost:8000/docs
- **Health Check**: http://localhost:8000/health
- **Root**: http://localhost:8000/

## Common Commands

```bash
# View logs
make logs

# Stop services
make down

# Access database
make db-shell

# Run tests
make test

# Clean everything
make clean
```

## Troubleshooting

### Container won't start
```bash
make logs  # Check what went wrong
```

### Port already in use
```bash
make down  # Stop existing containers
```

### Database connection error
```bash
docker compose ps  # Check if db is running
docker compose restart db
```

## Next Steps

- Read [DEPLOYMENT.md](DEPLOYMENT.md) for production deployment
- See [README.md](README.md) for API documentation
- Check [ANDROID_INTEGRATION.md](ANDROID_INTEGRATION.md) for mobile app setup

## Security Checklist

Before deploying to production:

- [ ] Set `DEBUG=false`
- [ ] Use strong `POSTGRES_PASSWORD` (32+ characters)
- [ ] Configure real `OPENAI_API_KEY`
- [ ] Restrict `CORS_ORIGINS` to your domains
- [ ] Enable HTTPS with reverse proxy (nginx/traefik)
- [ ] Set up monitoring and alerts
- [ ] Configure automated backups

## Support

For issues or questions:
1. Check logs: `make logs`
2. See [DEPLOYMENT.md](DEPLOYMENT.md) troubleshooting section
3. Review GitHub workflow status if using CI/CD
