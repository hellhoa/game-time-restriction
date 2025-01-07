# Use official Python runtime as base image
FROM python:3.9-slim-buster

# Set working directory
WORKDIR /app

# Install system dependencies
RUN apt-get update && apt-get install -y \
    librdkafka-dev \
    gcc \
    && rm -rf /var/lib/apt/lists/*

# Copy requirements file
COPY requirements.txt .

# Install Python dependencies
RUN pip install --no-cache-dir -r requirements.txt

# Copy the producer script
COPY kafka_producer.py .

# Set environment variables
ENV PYTHONUNBUFFERED=1

# Command to run the producer
CMD ["python", "kafka_producer.py"]