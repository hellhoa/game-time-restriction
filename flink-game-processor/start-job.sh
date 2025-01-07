#!/bin/bash

# Function to check if JobManager is ready
wait_for_jobmanager() {
    echo "Waiting for JobManager..."
    while ! nc -z jobmanager 8081; do
        sleep 3
    done
    echo "JobManager is ready!"
}

# Wait for JobManager
wait_for_jobmanager

# Submit the Flink job
flink run -d /opt/flink/usrlib/flink-game-processor.jar