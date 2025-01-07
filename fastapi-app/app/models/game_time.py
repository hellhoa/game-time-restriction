from sqlalchemy import Column, String, Integer, Date, DateTime
from app.core.database import Base

class UserDailyGameTime(Base):
    __tablename__ = "user_daily_game_time"
    
    user_id = Column(String, index=True, primary_key=True)
    game_id = Column(String, index=True, primary_key=True)
    play_date = Column(Date, index=True, primary_key=True)
    distinct_minutes = Column(Integer)
    window_start = Column(DateTime)
    window_end = Column(DateTime)