#!/bin/bash
# Start CodeOps infrastructure

set -e

echo "Starting CodeOps infrastructure..."

cd "$(dirname "$0")/.."

# Start services
docker-compose up -d postgres redis zookeeper kafka

# Wait for services to be healthy
echo "Waiting for services to be ready..."
sleep 10

# Initialize Kafka topics
docker-compose up kafka-init

echo ""
echo "Infrastructure ready!"
echo ""
echo "Services:"
echo "  - PostgreSQL: localhost:5432 (user: codeops, pass: codeops, db: codeops)"
echo "  - Redis:      localhost:6379"
echo "  - Kafka:      localhost:9092"
echo ""
echo "To view logs:  docker-compose logs -f"
echo "To stop:       docker-compose down"
