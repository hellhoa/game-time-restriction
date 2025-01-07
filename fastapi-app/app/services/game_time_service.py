import redis
import json
from datetime import date
from sqlalchemy.orm import Session
from app.core.config import settings
from app.models.game_time import UserDailyGameTime
from app.schemas.game_time_schema import GameRestrictionCheck

# Redis Client
redis_client = redis.Redis(
    host=settings.REDIS_HOST, 
    port=settings.REDIS_PORT, 
    decode_responses=True
)

class GameTimeService:
    @staticmethod
    def get_user_game_time(db: Session, user_id: str, game_id: str = None):
        """
        Retrieve user game time with Redis caching
        """
        today = date.today()
        
        # If specific game is requested
        if game_id:
            # Check Redis cache
            cache_key = f"user_game_time:{user_id}:{game_id}:{today}"
            cached_result = redis_client.get(cache_key)
            
            if cached_result:
                return json.loads(cached_result)
            
            # Fallback to database
            game_time = db.query(UserDailyGameTime).filter(
                UserDailyGameTime.user_id == user_id,
                UserDailyGameTime.game_id == game_id,
                UserDailyGameTime.play_date == today
            ).first()
            
            if game_time:
                result = {
                    "user_id": game_time.user_id,
                    "game_id": game_time.game_id,
                    "distinct_minutes": game_time.distinct_minutes
                }
                
                # Cache result
                redis_client.setex(
                    cache_key, 
                    86400,  # 24-hour cache
                    json.dumps(result)
                )
                
                return result
        
        # If no specific game, retrieve all games
        game_times = db.query(UserDailyGameTime).filter(
            UserDailyGameTime.user_id == user_id,
            UserDailyGameTime.play_date == today
        ).all()
        
        results = [
            {
                "user_id": gt.user_id,
                "game_id": gt.game_id,
                "distinct_minutes": gt.distinct_minutes
            } for gt in game_times
        ]
        
        # Cache individual game times
        for result in results:
            cache_key = f"user_game_time:{result['user_id']}:{result['game_id']}:{today}"
            redis_client.setex(
                cache_key, 
                86400,  # 24-hour cache
                json.dumps(result)
            )
        
        return results

    @staticmethod
    def check_game_restriction(db: Session, user_id: str, game_id: str) -> GameRestrictionCheck:
        """
        Check game time restrictions
        """
        today = date.today()
        
        # Get user's game times
        game_times = db.query(UserDailyGameTime).filter(
            UserDailyGameTime.user_id == user_id,
            UserDailyGameTime.play_date == today
        ).all()
        
        # Calculate total and game-specific play time
        total_play_time = sum(gt.distinct_minutes for gt in game_times)
        game_specific_time = next(
            (gt.distinct_minutes for gt in game_times if gt.game_id == game_id), 
            0
        )
        
        # Check restrictions
        can_play = (
            game_specific_time < 60 and  # Max 60 minutes per game
            total_play_time < 120  # Max 120 minutes total
        )
        
        return GameRestrictionCheck(
            user_id=user_id,
            can_play=can_play,
            current_game_time=game_specific_time,
            total_play_time=total_play_time
        )