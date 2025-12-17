package com.example.soukify.data.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Message {
    private String id;
    private String conversationId;
    private String senderId;
    private String senderName;
    private String text;
    private long timestamp;
    private boolean isRead;
    @ServerTimestamp
    private Date createdAt;

    // Constructeur vide requis pour Firestore
    public Message() {
    }

    public Message(String conversationId, String senderId, String senderName, String text) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) {
        this.conversationId = conversationId;
    }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
