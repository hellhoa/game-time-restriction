from fastapi import FastAPI, Depends, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.database import get_db
from app.services.game_time_service import GameTimeService
from app.schemas.game_time_schema import GameRestrictionCheck

app = FastAPI(
    title=settings.PROJECT_NAME,
    description="Game Time Restriction API",
    version="1.0.0"
)

# CORS Middleware
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

@app.get(f"{settings.API_V1_STR}/user-game-time/{{user_id}}")
def get_user_game_time(
    user_id: str, 
    game_id: str = None, 
    db: Session = Depends(get_db)
):
    """
    Retrieve user's game time for today
    """
    try:
        return GameTimeService.get_user_game_time(db, user_id, game_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

@app.get(
    f"{settings.API_V1_STR}/check-game-restriction/{{user_id}}", 
    response_model=GameRestrictionCheck
)
def check_game_restriction(
    user_id: str, 
    game_id: str, 
    db: Session = Depends(get_db)
):
    """
    Check if user can play a specific game
    """
    try:
        return GameTimeService.check_game_restriction(db, user_id, game_id)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))

# Optional: Health Check Endpoint
@app.get("/health")
def health_check():
    return {"status": "healthy"}