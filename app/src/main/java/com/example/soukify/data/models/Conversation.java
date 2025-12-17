package com.example.soukify.data.models;

import com.google.firebase.firestore.ServerTimestamp;
import java.util.Date;

public class Conversation {
    private String id;
    private String buyerId;
    private String sellerId;
    private String shopId;
    private String shopName;
    private String shopImage;
    private String lastMessage;
    private long lastMessageTimestamp;
    private int unreadCountBuyer;
    private int unreadCountSeller;
    @ServerTimestamp
    private Date createdAt;

    // Constructeur vide requis pour Firestore
    public Conversation() {
    }

    public Conversation(String id, String buyerId, String sellerId, String shopId,
                        String shopName, String shopImage) {
        this.id = id;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.shopId = shopId;
        this.shopName = shopName;
        this.shopImage = shopImage;
        this.lastMessage = "";
        this.lastMessageTimestamp = System.currentTimeMillis();
        this.unreadCountBuyer = 0;
        this.unreadCountSeller = 0;
    }

    // Getters et Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getBuyerId() { return buyerId; }
    public void setBuyerId(String buyerId) { this.buyerId = buyerId; }

    public String getSellerId() { return sellerId; }
    public void setSellerId(String sellerId) { this.sellerId = sellerId; }

    public String getShopId() { return shopId; }
    public void setShopId(String shopId) { this.shopId = shopId; }

    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }

    public String getShopImage() { return shopImage; }
    public void setShopImage(String shopImage) { this.shopImage = shopImage; }

    public String getLastMessage() { return lastMessage; }
    public void setLastMessage(String lastMessage) { this.lastMessage = lastMessage; }

    public long getLastMessageTimestamp() { return lastMessageTimestamp; }
    public void setLastMessageTimestamp(long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public int getUnreadCountBuyer() { return unreadCountBuyer; }
    public void setUnreadCountBuyer(int unreadCountBuyer) {
        this.unreadCountBuyer = unreadCountBuyer;
    }

    public int getUnreadCountSeller() { return unreadCountSeller; }
    public void setUnreadCountSeller(int unreadCountSeller) {
        this.unreadCountSeller = unreadCountSeller;
    }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
}
