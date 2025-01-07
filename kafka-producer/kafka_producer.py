import json
import uuid
from datetime import datetime
import random
import os
import time
from confluent_kafka import Producer

class GameEventProducer:
    def __init__(self, bootstrap_servers):
        """
        Initialize Kafka Producer with configuration
        
        :param bootstrap_servers: Comma-separated list of Kafka broker addresses
        """
        self.producer_config = {
            'bootstrap.servers': bootstrap_servers,
            'client.id': 'game-event-producer',
            'acks': 'all',
            'compression.type': 'snappy',
            'retries': 3,
        }
        
        self.producer = Producer(self.producer_config)
        self.topic = 'user_game_events'

    def delivery_report(self, err, msg):
        """
        Callback for message delivery confirmation
        """
        if err is not None:
            print(f'Message delivery failed: {err}')
        else:
            print(f'Message delivered to {msg.topic()} [{msg.partition()}]')

    def generate_game_event(self):
        """
        Generate synthetic game event data matching the new structure
        
        :return: Dictionary representing a game event
        """
        users = ['user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8', 'user9', 'user10']
        games = ['game1', 'game2', 'game3', 'game4', 'game5', 'game6', 'game7', 'game8', 'game9', 'game10']
        
        return {
            "user_id": random.choice(users),
            "role_id": f"role_{str(uuid.uuid4())[:8]}",
            "game_id": random.choice(games),
            "event_time": datetime.utcnow().isoformat()
        }

    def produce_event(self):
        """
        Produce a single game event to Kafka
        """
        try:
            event = self.generate_game_event()
            
            # Serialize event to JSON
            event_json = json.dumps(event).encode('utf-8')
            
            # Produce message to Kafka
            self.producer.produce(
                topic=self.topic, 
                value=event_json,
                key=event['user_id'].encode('utf-8'),
                callback=self.delivery_report
            )
            
            # Trigger message delivery
            self.producer.poll(0)
        
        except Exception as e:
            print(f"Error producing event: {e}")

    def produce_batch_events(self, num_events=100):
        """
        Produce a batch of game events
        
        :param num_events: Number of events to produce
        """
        for _ in range(num_events):
            self.produce_event()
            time.sleep(0.1)  # Small delay between events
        
        # Flush any remaining messages
        self.producer.flush()

def main():
    # Use environment variable for Kafka bootstrap servers
    bootstrap_servers = os.getenv('KAFKA_BOOTSTRAP_SERVERS', 'localhost:9092')
    
    # Initialize producer
    event_producer = GameEventProducer(bootstrap_servers)
    
    # Continuous event production
    while True:
        event_producer.produce_batch_events(num_events=10)
        time.sleep(5)  # Wait 5 seconds between batches

if __name__ == '__main__':
    main()