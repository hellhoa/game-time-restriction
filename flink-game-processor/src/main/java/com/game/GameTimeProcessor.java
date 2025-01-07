package com.game;

import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;

public class GameTimeProcessor {
    public static void main(String[] args) throws Exception {
        // Create Table Environment
        EnvironmentSettings settings = EnvironmentSettings.newInstance().inStreamingMode().build();
        TableEnvironment tEnv = TableEnvironment.create(settings);

        // Create Kafka Source Table
        tEnv.executeSql(
            "CREATE TABLE game_events (" +
            "    user_id STRING," +
            "    game_id STRING," +
            "    event_time TIMESTAMP(3)," +
            "    WATERMARK FOR event_time AS event_time - INTERVAL '5' SECONDS" +
            ") WITH (" +
            "    'connector' = 'kafka'," +
            "    'topic' = 'user_game_events'," +
            "    'properties.bootstrap.servers' = '" + System.getenv("KAFKA_BOOTSTRAP_SERVERS") + "'," +
            "    'properties.group.id' = 'game-time-processor'," +
            "    'scan.startup.mode' = 'earliest-offset'," +
            "    'format' = 'json'," +
            "    'json.fail-on-missing-field' = 'false'," +
            "    'json.ignore-parse-errors' = 'true'," +
            "    'json.timestamp-format.standard' = 'ISO-8601'" +
            ")"
        );

        // Create JDBC Sink Table
        tEnv.executeSql(
            "CREATE TABLE user_daily_game_time (" +
            "    user_id STRING," +
            "    game_id STRING," +
            "    play_date DATE," +
            "    distinct_minutes BIGINT," +
            "    window_start TIMESTAMP(3)," +
            "    window_end TIMESTAMP(3)," +
            "    PRIMARY KEY (user_id, game_id, play_date) NOT ENFORCED" +
            ") WITH (" +
            "    'connector' = 'jdbc'," +
            "    'url' = '" + System.getenv("TIMESCALEDB_URL") + "'," +
            "    'table-name' = 'user_daily_game_time'," +
            "    'driver' = 'org.postgresql.Driver'," +
            "    'username' = '" + System.getenv("DB_USERNAME") + "'," +
            "    'password' = '" + System.getenv("DB_PASSWORD") + "'" +
            ")"
        );

        // Create temporary view for minute extraction
        tEnv.executeSql(
            "CREATE TEMPORARY VIEW events_with_minutes AS " +
            "SELECT " +
            "    user_id, " +
            "    game_id, " +
            "    event_time, " +
            "    EXTRACT(HOUR FROM event_time + INTERVAL '7' HOUR) * 60 + " +
            "    EXTRACT(MINUTE FROM event_time + INTERVAL '7' HOUR) as minute_of_day " +
            "FROM game_events"
        );

        // Create temporary view for aggregated results
        tEnv.executeSql(
            "CREATE TEMPORARY VIEW daily_stats AS " +
            "SELECT " +
            "    user_id, " +
            "    game_id, " +
            "    CAST(window_start + INTERVAL '7' HOUR AS DATE) as play_date, " +
            "    COUNT(DISTINCT minute_of_day) as distinct_minutes, " +
            "    window_start + INTERVAL '7' HOUR as window_start, " +
            "    window_end + INTERVAL '7' HOUR as window_end " +
            "FROM TABLE(" +
            "    TUMBLE(TABLE events_with_minutes, DESCRIPTOR(event_time), INTERVAL '1' DAY)" +
            ") " +
            "GROUP BY " +
            "    user_id, " +
            "    game_id, " +
            "    window_start, " +
            "    window_end"
        );

        // Insert results into the JDBC sink
        tEnv.executeSql(
            "INSERT INTO user_daily_game_time " +
            "SELECT user_id, game_id, play_date, distinct_minutes, window_start, window_end " +
            "FROM daily_stats"
        );
    }
}