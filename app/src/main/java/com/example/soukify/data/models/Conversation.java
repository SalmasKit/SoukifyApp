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
    private Long lastMessageTimestamp;
    private int unreadCountBuyer;
    private int unreadCountSeller;
    private String buyerImage; // 🔥 Photo de profil de l'acheteur

    // ✅ NOUVEAU : Champ createdAt pour Firebase
    @ServerTimestamp
    private Date createdAt;

    // ✅ Constructeur vide OBLIGATOIRE pour Firebase
    public Conversation() {
        // Firebase utilise ce constructeur pour désérialiser
    }

    // ✅ Constructeur complet
    public Conversation(String buyerId, String sellerId, String shopId,
                        String shopName, String shopImage, String buyerName, String buyerImage) {
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.shopId = shopId;
        this.shopName = shopName;
        this.shopImage = shopImage;
        this.buyerName = buyerName;
        this.buyerImage = buyerImage;
        this.lastMessage = "";
        this.lastMessageTimestamp = System.currentTimeMillis();
        this.unreadCountBuyer = 0;
        this.unreadCountSeller = 0;
    }

    // ════════════════════════════════════════
    // GETTERS
    // ════════════════════════════════════════

    public String getId() {
        return id;
    }

    public String getBuyerId() {
        return buyerId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getShopId() {
        return shopId;
    }

    public String getShopName() {
        return shopName;
    }

    public String getShopImage() {
        return shopImage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public Long getLastMessageTimestamp() {
        return lastMessageTimestamp;
    }

    public int getUnreadCountBuyer() {
        return unreadCountBuyer;
    }

    public int getUnreadCountSeller() {
        return unreadCountSeller;
    }

    // ✅ NOUVEAU : Getter pour createdAt
    public Date getCreatedAt() {
        return createdAt;
    }

    // ════════════════════════════════════════
    // SETTERS
    // ════════════════════════════════════════

    public void setId(String id) {
        this.id = id;
    }

    public void setBuyerId(String buyerId) {
        this.buyerId = buyerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public void setShopId(String shopId) {
        this.shopId = shopId;
    }

    public void setShopName(String shopName) {
        this.shopName = shopName;
    }

    public void setShopImage(String shopImage) {
        this.shopImage = shopImage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public void setLastMessageTimestamp(Long lastMessageTimestamp) {
        this.lastMessageTimestamp = lastMessageTimestamp;
    }

    public void setUnreadCountBuyer(int unreadCountBuyer) {
        this.unreadCountBuyer = unreadCountBuyer;
    }

    public void setUnreadCountSeller(int unreadCountSeller) {
        this.unreadCountSeller = unreadCountSeller;
    }

    // ✅ NOUVEAU : Setter pour createdAt
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }
    private String buyerName; // nom de l'acheteur

    public String getBuyerName() {
        return buyerName;
    }

    public void setBuyerName(String buyerName) {
        this.buyerName = buyerName;
    }

    public String getBuyerImage() {
        return buyerImage;
    }

    public void setBuyerImage(String buyerImage) {
        this.buyerImage = buyerImage;
    }

}