#!/bin/sh
set -e

echo "Waiting for PostgreSQL to accept connections..."
for i in $(seq 1 30); do
  if /app/bin/server init-db /app/config.yml 2>/dev/null; then
    echo "Database initialized successfully."
    break
  fi
  echo "  Attempt $i/30 — retrying in 2s..."
  sleep 2
done

echo "Starting Motif server..."
exec /app/bin/server server /app/config.yml
