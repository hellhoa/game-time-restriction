package com.game;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.game.models.GameEvent;
import com.game.utils.TimeUtils;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.streams.*;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.WindowStore;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.streams.KeyValue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.Map;
import java.time.ZoneId;
import java.sql.Types;
import java.sql.Date;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

public class GameTimeProcessor {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final JedisPool jedisPool;

    static {
        // Initialize Redis connection pool
        JedisPoolConfig poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(128);
        poolConfig.setMaxIdle(128);
        poolConfig.setMinIdle(16);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestOnReturn(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleTimeMillis(Duration.ofSeconds(60).toMillis());
        poolConfig.setTimeBetweenEvictionRunsMillis(Duration.ofSeconds(30).toMillis());
        poolConfig.setNumTestsPerEvictionRun(3);
        poolConfig.setBlockWhenExhausted(true);

        String redisHost = System.getenv().getOrDefault("REDIS_HOST", "redis");
        int redisPort = Integer.parseInt(System.getenv().getOrDefault("REDIS_PORT", "6379"));
        jedisPool = new JedisPool(poolConfig, redisHost, redisPort);
    }

    // Custom Serializer for Set<Integer>
    static class SetSerializer implements Serializer<Set<Integer>> {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {}

        @Override
        public byte[] serialize(String topic, Set<Integer> data) {
            try {
                return mapper.writeValueAsBytes(data);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {}
    }

    // Custom Deserializer for Set<Integer>
    static class SetDeserializer implements Deserializer<Set<Integer>> {
        private final ObjectMapper mapper = new ObjectMapper();

        @Override
        public void configure(Map<String, ?> configs, boolean isKey) {}

        @Override
        public Set<Integer> deserialize(String topic, byte[] data) {
            try {
                if (data == null) return null;
                return mapper.readValue(data, mapper.getTypeFactory().constructCollectionType(Set.class, Integer.class));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public void close() {}
    }

    private static void updateRedisCache(String userId, String gameId, LocalDateTime playDate, int distinctMinutes) {
        String cacheKey = String.format("game_time:%s:%s:%s", userId, gameId, playDate.toLocalDate());
        try (Jedis jedis = jedisPool.getResource()) {
            // Store with 24-hour expiration
            jedis.setex(cacheKey, 86400, String.valueOf(distinctMinutes));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        Properties props = new Properties();
        props.put(StreamsConfig.APPLICATION_ID_CONFIG, "game-time-processor");
        props.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, System.getenv("KAFKA_BOOTSTRAP_SERVERS"));
        props.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass());
        props.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, Serdes.String().getClass());

        StreamsBuilder builder = new StreamsBuilder();

        // Create Serde for Set<Integer>
        final Serde<Set<Integer>> setIntegerSerde = Serdes.serdeFrom(
            new SetSerializer(),
            new SetDeserializer()
        );

        // Process game events
        builder.<String, String>stream("user_game_events")
                .map((key, value) -> {
                    try {
                        GameEvent event = MAPPER.readValue(value, GameEvent.class);
                        // Create composite key: userId-gameId
                        return KeyValue.pair(event.getUserId() + "-" + event.getGameId(), value);
                    } catch (Exception e) {
                        e.printStackTrace();
                        return KeyValue.pair("", value);
                    }
                })
                .groupByKey()
                .windowedBy(TimeWindows.ofSizeWithNoGrace(Duration.ofDays(1)))
                .<Set<Integer>>aggregate(
                        () -> new HashSet<>(),
                        (key, value, aggregate) -> {
                            try {
                                GameEvent event = MAPPER.readValue(value, GameEvent.class);
                                LocalDateTime eventTime = TimeUtils.parseWithTimezone(event.getEventTime());
                                aggregate.add(TimeUtils.getMinuteOfDay(eventTime));
                                return aggregate;
                            } catch (Exception e) {
                                e.printStackTrace();
                                return aggregate;
                            }
                        },
                        Materialized.<String, Set<Integer>, WindowStore<Bytes, byte[]>>as("time-store")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(setIntegerSerde)
                )
                .toStream()
                .foreach((key, value) -> {
                    String[] keyParts = key.key().split("-");
                    String userId = keyParts[0];
                    String gameId = keyParts[1];
                    LocalDateTime playDateTime = LocalDateTime.ofInstant(
                        key.window().startTime(), 
                        ZoneId.of("UTC")
                    );
                    LocalDateTime windowStart = LocalDateTime.ofInstant(
                        key.window().startTime(), 
                        ZoneId.of("UTC")
                    );
                    LocalDateTime windowEnd = LocalDateTime.ofInstant(
                        key.window().endTime(), 
                        ZoneId.of("UTC")
                    );
                    
                    try (Connection conn = DriverManager.getConnection(
                            System.getenv("TIMESCALEDB_URL"),
                            System.getenv("DB_USERNAME"),
                            System.getenv("DB_PASSWORD"))) {
                        
                        PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO user_daily_game_time (user_id, game_id, play_date, distinct_minutes, window_start, window_end) " +
                            "VALUES (?, ?, ?, ?, ?, ?) " +
                            "ON CONFLICT (user_id, game_id, play_date) " +
                            "DO UPDATE SET distinct_minutes = EXCLUDED.distinct_minutes"
                        );
                        
                        stmt.setString(1, userId);
                        stmt.setString(2, gameId);
                        stmt.setObject(3, Date.valueOf(playDateTime.toLocalDate()), Types.DATE);
                        stmt.setInt(4, value.size());
                        stmt.setObject(5, windowStart, Types.TIMESTAMP);
                        stmt.setObject(6, windowEnd, Types.TIMESTAMP);
                        
                        stmt.executeUpdate();

                        updateRedisCache(userId, gameId, playDateTime, value.size());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });

        KafkaStreams streams = new KafkaStreams(builder.build(), props);
        streams.start();

        // Add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(streams::close));
    }
}