# Build stage
FROM maven:3.8.4-openjdk-11-slim AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package

# Run stage
FROM apache/flink:1.19.0-scala_2.12-java11
WORKDIR /opt/flink/usrlib

# Copy the built JAR file
RUN apt-get update && apt-get install -y netcat
COPY --from=build /app/target/flink-game-processor-1.0-SNAPSHOT.jar /opt/flink/usrlib/flink-game-processor.jar

# Set environment variables
ENV KAFKA_BOOTSTRAP_SERVERS=kafka-broker:9093
ENV TIMESCALEDB_URL=jdbc:postgresql://timescaledb:5432/gamedb
ENV DB_USERNAME=gameuser
ENV DB_PASSWORD=gamepassword
COPY start-job.sh /start-job.sh
RUN chmod +x /start-job.sh

# Command to run the Flink job
CMD ["/start-job.sh"]