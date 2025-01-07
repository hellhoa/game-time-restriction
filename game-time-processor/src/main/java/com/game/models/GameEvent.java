package com.game.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

public class GameEvent {
    @JsonProperty("user_id")
    private String userId;

    @JsonProperty("role_id")
    private String roleId;
    
    @JsonProperty("game_id")
    private String gameId;
    
    @JsonProperty("event_time")
    private String eventTime;

    // Getters and setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getRoleId() { return roleId; }
    public void setRoleId(String roleId) { this.roleId = roleId; }
    public String getGameId() { return gameId; }
    public void setGameId(String gameId) { this.gameId = gameId; }
    public String getEventTime() { return eventTime; }
    public void setEventTime(String eventTime) { this.eventTime = eventTime; }
}