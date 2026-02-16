#!/bin/bash
# Reset CodeOps infrastructure (WARNING: destroys all data)

set -e

echo "WARNING: This will destroy all data!"
read -p "Are you sure? (y/N) " -n 1 -r
echo

if [[ $REPLY =~ ^[Yy]$ ]]; then
    cd "$(dirname "$0")/.."

    echo "Removing containers and volumes..."
    docker-compose down -v

    echo "Starting fresh infrastructure..."
    ./scripts/start-infra.sh

    echo "Infrastructure reset complete"
else
    echo "Cancelled"
fi
