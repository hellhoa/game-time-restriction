from pydantic import BaseSettings

class Settings(BaseSettings):
    # Database Configuration
    DATABASE_URL: str = "postgresql://gameuser:gamepassword@timescaledb:5432/gamedb"
    
    # Redis Configuration
    REDIS_HOST: str = "redis"
    REDIS_PORT: int = 6379
    
    # API Settings
    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "Game Time Restriction API"
    
    # Logging and Monitoring
    LOG_LEVEL: str = "INFO"
    
    class Config:
        env_file = ".env"
        env_file_encoding = "utf-8"

settings = Settings()