# game-time-restriction


### First, clone the repository and start the Docker Compose project by the following commands:

```
git clone https://github.com/hellhoa/game-time-restriction.git
cd game-time-restriction
docker compose up -d
```
### Verify the setup by accessing these services:

- KafkaUI: http://localhost:8080
- FastAPI docs: http://localhost:8000/docs

### View sample messages in KafkaUI:

1. Navigate to Topics > user_game_events > Messages
2. Find messages with IDs `user1`-`user10`

### To create test messages:

1. Go to Topics > user_game_events > Produce Message
2. Set user_id as the message key
3. Use this sample message format:
```
{
    "user_id": "user_test",
    "role_id": "role_abcxyz",
    "game_id": "game1",
    "event_time": "2025-01-07 07:27:11+00"
}
```

### You can verify the computed data by accessing the TimescaleDB container as follows:
1. Access to the TimescaleDB:
```
docker exec -it timescaledb /bin/bash
```
2. Query the data by `user_id`
```
psql -U gameuser gamedb -c "select * from user_daily_game_time where user_id='user_test' order by game_id asc"
```

### Next, access FastAPI docs and test the API endpoint we have.

1. Access to the link: `http://localhost:8000/docs#/`. You will see two APIs `/api/v1/user-game-time/{user_id}` and `/api/v1/check-game-restriction/{user_id}`

2. Navigate the APIs and try it out by entering user_id as `user_test` and game_id as `game1`


3. The expected results would be:
```
{
  "user_id": "user_test",
  "game_id": "game1",
  "distinct_minutes": 1
}
```

and

```
{
  "user_id": "user_test",
  "can_play": true,
  "current_game_time": 1,
  "total_play_time": 1
}
```
### Now you can produce additional messages and try other scenarios as you wish.

### Additionally, I have another compose project that runs game-time-processor using Apache Flink, you can try it out by running the following command:
```
docker compose -f docker-compose-flink.yml up -d --build
```
### However, this Flink job configuration requires a lot of resources, so you should only run it if you have more than 24GB of RAM.