-- Create extension
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- Create user_daily_game_time table
CREATE TABLE user_daily_game_time (
    user_id TEXT NOT NULL,
    game_id TEXT NOT NULL,
    play_date DATE NOT NULL,
    distinct_minutes INTEGER NOT NULL,
    window_start TIMESTAMPTZ NOT NULL,
    window_end TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (user_id, game_id, play_date)
);

-- Create TimescaleDB Hypertable
SELECT create_hypertable('user_daily_game_time', 'play_date');

-- Create indexes for performance
CREATE INDEX idx_user_daily_game_time_user_id ON user_daily_game_time(user_id);
CREATE INDEX idx_user_daily_game_time_game_id ON user_daily_game_time(game_id);
CREATE INDEX idx_user_daily_game_time_play_date ON user_daily_game_time(play_date);