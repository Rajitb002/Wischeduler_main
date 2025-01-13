package com.example.wischeduler;

public class ChatMessage {
    private final String message;
    private final boolean isBot;
    private final String timestamp;

    public ChatMessage(String message, boolean isBot, String timestamp) {
        this.message = message;
        this.isBot = isBot;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public boolean isBot() {
        return isBot;
    }

    public String getTimestamp() {
        return timestamp;
    }
}
