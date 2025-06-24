#!/bin/bash

# Kafka container name (the same as in docker-compose.yml)
KAFKA_CONTAINER="kafka"

# Bootstrap server (inside Docker network)
BOOTSTRAP_SERVER="localhost:9092"

# Topics list
TOPICS=(
  "cities-cdc"
  "cities-enriched"
  "cities-aggregated"
  "cities-validated"
  "cities-transformed"
  "cities-dlq"
  "cities-retry"
)

# Create topics
for TOPIC in "${TOPICS[@]}"; do
  echo "🛠 Creating topic: $TOPIC"
  docker exec -i $KAFKA_CONTAINER \
    kafka-topics --create --if-not-exists \
    --bootstrap-server $BOOTSTRAP_SERVER \
    --replication-factor 1 \
    --partitions 1 \
    --topic $TOPIC
done

echo "✅ All topics created successfully!"
