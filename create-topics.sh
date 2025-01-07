#!/bin/bash

# Wait for Kafka to be ready
sleep 30

# Create Kafka topics
kafka-topics --create --topic user_game_events \
    --bootstrap-server kafka-broker:9093 \
    --partitions 3 \
    --replication-factor 1