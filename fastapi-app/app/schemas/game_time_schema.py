from pydantic import BaseModel
from datetime import date, datetime
from typing import Optional, List

class GameTimeBase(BaseModel):
    user_id: str
    game_id: str
    distinct_minutes: int
    play_date: Optional[date] = None

class GameTimeCreate(GameTimeBase):
    window_start: Optional[datetime] = None
    window_end: Optional[datetime] = None

class GameTimeResponse(GameTimeBase):
    class Config:
        orm_mode = True

class GameRestrictionCheck(BaseModel):
    user_id: str
    can_play: bool
    current_game_time: int
    total_play_time: int