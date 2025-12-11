.PHONY: help install install-dev test lint format type-check build up down logs clean dev-up dev-down shell db-shell migrate

# Default target
help:
	@echo "Voice Assistant Backend - Available Commands"
	@echo "============================================="
	@echo "Development:"
	@echo "  make install        - Install production dependencies"
	@echo "  make install-dev    - Install development dependencies"
	@echo "  make test           - Run tests with coverage"
	@echo "  make lint           - Run linting (flake8)"
	@echo "  make format         - Format code (black, isort)"
	@echo "  make type-check     - Run type checking (mypy)"
	@echo "  make dev-up         - Start development environment"
	@echo "  make dev-down       - Stop development environment"
	@echo ""
	@echo "Docker (Production):"
	@echo "  make build          - Build Docker image"
	@echo "  make up             - Start production containers"
	@echo "  make down           - Stop production containers"
	@echo "  make logs           - View container logs"
	@echo "  make shell          - Open shell in backend container"
	@echo "  make db-shell       - Open PostgreSQL shell"
	@echo ""
	@echo "Utilities:"
	@echo "  make clean          - Clean up containers, volumes, and cache"
	@echo "  make migrate        - Run database migrations"

# Development commands
install:
	pip install -e .

install-dev:
	pip install -e ".[dev]"

test:
	pytest --cov=app --cov-report=term-missing --cov-report=html

lint:
	flake8 app tests

format:
	black app tests
	isort app tests

type-check:
	mypy app

# Docker production commands
build:
	docker compose build

up:
	docker compose up -d
	@echo "Backend is starting... Check status with: make logs"
	@echo "API will be available at: http://localhost:8000"
	@echo "API docs at: http://localhost:8000/docs"

down:
	docker compose down

logs:
	docker compose logs -f backend

shell:
	docker compose exec backend /bin/bash

db-shell:
	docker compose exec db psql -U postgres -d voiceassistant

restart:
	docker compose restart backend

# Development environment
dev-up:
	docker compose -f compose.dev.yaml up -d
	@echo "Development backend is starting..."
	@echo "API will be available at: http://localhost:8000"

dev-down:
	docker compose -f compose.dev.yaml down

# Database migrations
migrate:
	docker compose exec backend python -c "import asyncio; from app.database import init_db; asyncio.run(init_db())"

# Cleanup
clean:
	docker compose down -v
	docker compose -f compose.dev.yaml down -v
	find . -type d -name "__pycache__" -exec rm -rf {} + 2>/dev/null || true
	find . -type f -name "*.pyc" -delete
	find . -type d -name "*.egg-info" -exec rm -rf {} + 2>/dev/null || true
	rm -rf .pytest_cache .mypy_cache htmlcov .coverage
	@echo "Cleanup complete!"

# CI/CD testing
ci-test: install-dev lint type-check test

# Health check
health:
	@curl -f http://localhost:8000/health || echo "Backend is not running"

# View environment
env:
	docker compose config
