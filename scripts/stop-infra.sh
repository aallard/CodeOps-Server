#!/bin/bash
# Stop CodeOps infrastructure

set -e

echo "Stopping CodeOps infrastructure..."

cd "$(dirname "$0")/.."

docker-compose down

echo "Infrastructure stopped"
