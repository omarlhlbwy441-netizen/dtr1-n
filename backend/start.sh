#!/bin/bash
# Rafeeq Backend Start Script for Render

echo "🐺 Starting Rafeeq Backend..."
echo "Provider: $AI_PROVIDER"
echo "Environment: $ENVIRONMENT"

# Run migrations if needed
# alembic upgrade head

# Start server
exec uvicorn app.main:app --host 0.0.0.0 --port $PORT --workers 1
