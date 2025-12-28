package com.example.soukify.data.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.models.Conversation;
import com.example.soukify.data.models.Message;
import com.example.soukify.services.NotificationSenderService;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {

    private static final String TAG = "ChatRepository";
    private static final String COLLECTION_CONVERSATIONS = "Conversation";
    private static final String COLLECTION_MESSAGES = "messages";
    private static final String COLLECTION_USERS = "users";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;
    private final FirebaseStorage storage;
    private final NotificationSenderService notificationSenderService;

    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
        this.storage = FirebaseStorage.getInstance();
        this.notificationSenderService = new NotificationSenderService();
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    // ==========================
    // INTERFACES CALLBACK
    // ==========================
    public interface OnConversationLoadedListener {
        void onSuccess(Conversation conversation);
        void onFailure(String error);
    }

    public interface SendMessageCallback {
        void onSuccess();
        void onError(String error);
    }

    // ==========================
    // 🔥 RÉCUPÉRER LE fullName D'UN UTILISATEUR
    // ==========================
    private void getUserInfo(String userId, OnUserInfoLoadedListener listener) {
        if (userId == null || userId.isEmpty()) {
            listener.onLoaded("Utilisateur", "");
            return;
        }

        Log.d(TAG, "🔍 Récupération info pour userId: " + userId);

        db.collection(COLLECTION_USERS)
                .document(userId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String fullName = doc.getString("fullName");
                        String profileImage = doc.getString("profileImage");

                        Log.d(TAG, "✅ Info récupérées: " + fullName + " | image: " + profileImage);

                        if (fullName == null || fullName.isEmpty()) {
                            fullName = doc.getString("name");
                            if (fullName == null || fullName.isEmpty()) {
                                fullName = "Utilisateur";
                            }
                        }
                        listener.onLoaded(fullName, profileImage != null ? profileImage : "");
                    } else {
                        Log.w(TAG, "⚠️ Document utilisateur inexistant");
                        listener.onLoaded("Utilisateur", "");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "❌ Erreur récupération info", e);
                    listener.onLoaded("Utilisateur", "");
                });
    }

    private interface OnUserInfoLoadedListener {
        void onLoaded(String name, String image);
    }

    // ==========================
    // ✅ CRÉER/RÉCUPÉRER CONVERSATION
    // ==========================
    public void getOrCreateConversation(String buyerId,
                                        String sellerId,
                                        String shopId,
                                        String shopName,
                                        String shopImage,
                                        OnConversationLoadedListener listener) {

        if (buyerId == null || sellerId == null || shopId == null) {
            Log.e(TAG, "❌ Données manquantes");
            listener.onFailure("Données manquantes");
            return;
        }

        String conversationId = "conv_" + buyerId + "_" + shopId;

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "🔍 CRÉATION/RÉCUPÉRATION CONVERSATION");
        Log.e(TAG, "   conversationId: " + conversationId);
        Log.e(TAG, "   buyerId: " + buyerId);
        Log.e(TAG, "   sellerId: " + sellerId);
        Log.e(TAG, "   shopId: " + shopId);
        Log.e(TAG, "════════════════════════════════════════");

        DocumentReference conversationRef =
                db.collection(COLLECTION_CONVERSATIONS).document(conversationId);

        conversationRef.get().addOnSuccessListener(snapshot -> {

            if (snapshot.exists()) {
                Log.e(TAG, "✅ Conversation existante trouvée");
                Conversation conversation = snapshot.toObject(Conversation.class);
                if (conversation != null) {
                    conversation.setId(conversationId);
                    
                    // 🔥 REPAIR: Si l'photo de l'acheteur est manquante, on la récupère et on MAJ
                    if (conversation.getBuyerImage() == null || conversation.getBuyerImage().isEmpty()) {
                        getUserInfo(buyerId, (name, image) -> {
                            if (image != null && !image.isEmpty()) {
                                conversationRef.update("buyerImage", image);
                                conversation.setBuyerImage(image);
                            }
                            listener.onSuccess(conversation);
                        });
                    } else {
                        listener.onSuccess(conversation);
                    }
                }
                return;
            }

            // 🆕 CRÉATION NOUVELLE CONVERSATION
            Log.e(TAG, "🆕 Création nouvelle conversation");

            // 🔥 RÉCUPÉRER LES INFOS DE L'ACHETEUR
            getUserInfo(buyerId, (buyerFullName, buyerProfileImage) -> {
                Log.e(TAG, "✅ Infos acheteur récupérées: " + buyerFullName + " | " + buyerProfileImage);

                Map<String, Object> data = new HashMap<>();
                data.put("id", conversationId);
                data.put("buyerId", buyerId);
                data.put("buyerName", buyerFullName);
                data.put("buyerImage", buyerProfileImage); // 🔥 Photo de profil de l'acheteur
                data.put("sellerId", sellerId);
                data.put("shopId", shopId);
                data.put("shopName", shopName);
                data.put("shopImage", shopImage != null ? shopImage : "");
                data.put("lastMessage", "");
                data.put("lastMessageTimestamp", System.currentTimeMillis());
                data.put("unreadCountBuyer", 0);
                data.put("unreadCountSeller", 0);
                data.put("createdAt", FieldValue.serverTimestamp());

                conversationRef.set(data)
                        .addOnSuccessListener(v -> {
                            Log.e(TAG, "✅ Conversation créée avec buyerName: " + buyerFullName);
                            Conversation c = new Conversation();
                            c.setId(conversationId);
                            c.setBuyerId(buyerId);
                            c.setBuyerName(buyerFullName);
                            c.setBuyerImage(buyerProfileImage);
                            c.setSellerId(sellerId);
                            c.setShopId(shopId);
                            c.setShopName(shopName);
                            c.setShopImage(shopImage);
                            listener.onSuccess(c);
                        })
                        .addOnFailureListener(e -> {
                            Log.e(TAG, "❌ Erreur création", e);
                            listener.onFailure(e.getMessage());
                        });
            });

        }).addOnFailureListener(e -> {
            Log.e(TAG, "❌ Erreur recherche", e);
            listener.onFailure(e.getMessage());
        });
    }

    // ==========================
    // 🔥 ENVOYER MESSAGE (AVEC fullName AUTO)
    // ==========================
    public void sendMessage(String conversationId, String text, SendMessageCallback callback) {
        String senderId = getCurrentUserId();
        if (senderId.isEmpty() || text.trim().isEmpty()) {
            callback.onError("Données invalides");
            return;
        }

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "📤 ENVOI MESSAGE");
        Log.e(TAG, "   ConversationId: " + conversationId);
        Log.e(TAG, "   SenderId: " + senderId);
        Log.e(TAG, "   Text: " + text.trim());
        Log.e(TAG, "════════════════════════════════════════");

        // 🔥 RÉCUPÉRER LES INFOS DE L'EXPÉDITEUR AUTOMATIQUEMENT
        getUserInfo(senderId, (senderFullName, senderImage) -> {
            Log.e(TAG, "✅ Info expéditeur récupérées: " + senderFullName);

            Map<String, Object> message = new HashMap<>();
            message.put("conversationId", conversationId);
            message.put("senderId", senderId);
            message.put("senderName", senderFullName);
            message.put("text", text.trim());
            message.put("timestamp", System.currentTimeMillis());
            message.put("isRead", false);
            message.put("createdAt", FieldValue.serverTimestamp());

            db.collection(COLLECTION_CONVERSATIONS)
                    .document(conversationId)
                    .collection(COLLECTION_MESSAGES)
                    .add(message)
                    .addOnSuccessListener(docRef -> {
                        Log.e(TAG, "✅ Message envoyé avec senderName: " + senderFullName);
                        updateConversationAfterMessage(conversationId, text.trim(), senderId);
                        
                        // 🔔 Send notification to recipient
                        sendMessageNotification(conversationId, senderId, senderFullName, text.trim());
                        
                        callback.onSuccess();
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "❌ Erreur envoi", e);
                        callback.onError(e.getMessage());
                    });
        });
    }
    
    // ==========================
    // 🔔 SEND MESSAGE NOTIFICATION
    // ==========================
    private void sendMessageNotification(String conversationId, String senderId, String senderName, String messageText) {
        db.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;
                    
                    String buyerId = doc.getString("buyerId");
                    String recipientId = senderId.equals(buyerId) ? doc.getString("sellerId") : buyerId;
                    
                    if (recipientId != null) {
                        // Delegate to centralized NotificationSenderService which now handles OneSignal
                        notificationSenderService.sendMessageNotification(
                            recipientId, 
                            senderName, 
                            messageText, 
                            conversationId
                        );
                    }
                });
    }

    // ==========================
    // MAJ CONVERSATION APRÈS MESSAGE
    // ==========================
    private void updateConversationAfterMessage(String conversationId, String lastMessage,
                                                String senderId) {
        db.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String buyerId = doc.getString("buyerId");
                    boolean isSenderBuyer = senderId.equals(buyerId);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", lastMessage);
                    updates.put("lastMessageTimestamp", System.currentTimeMillis());

                    if (isSenderBuyer) {
                        updates.put("unreadCountSeller", FieldValue.increment(1));
                    } else {
                        updates.put("unreadCountBuyer", FieldValue.increment(1));
                    }

                    db.collection(COLLECTION_CONVERSATIONS)
                            .document(conversationId)
                            .update(updates);
                });
    }

    // ==========================
    // ✅ ÉCOUTE MESSAGES TEMPS RÉEL
    // ==========================
    public LiveData<List<Message>> getMessagesRealtime(String conversationId) {
        MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "👂 DÉMARRAGE ÉCOUTE MESSAGES");
        Log.e(TAG, "   ConversationId: " + conversationId);
        Log.e(TAG, "════════════════════════════════════════");

        db.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .collection(COLLECTION_MESSAGES)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {

                    if (error != null) {
                        Log.e(TAG, "❌ ERREUR: " + error.getMessage());
                        messagesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (value == null) {
                        messagesLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    Log.e(TAG, "📨 Messages reçus: " + value.size());

                    List<Message> messages = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Message message = doc.toObject(Message.class);
                        message.setId(doc.getId());

                        // 🔥 LOG pour debug senderName
                        Log.d(TAG, "   Message: " + message.getText() + " | senderName: " + message.getSenderName());

                        messages.add(message);
                    }

                    messagesLiveData.setValue(messages);
                });

        return messagesLiveData;
    }

    // ==========================
    // MARQUER MESSAGES LUS
    // ==========================
    public void markMessagesAsRead(String conversationId) {
        String currentUserId = getCurrentUserId();

        db.collection(COLLECTION_CONVERSATIONS)
                .document(conversationId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String buyerId = doc.getString("buyerId");
                    boolean isBuyer = currentUserId.equals(buyerId);
                    String field = isBuyer ? "unreadCountBuyer" : "unreadCountSeller";

                    db.collection(COLLECTION_CONVERSATIONS)
                            .document(conversationId)
                            .update(field, 0);

                    db.collection(COLLECTION_CONVERSATIONS)
                            .document(conversationId)
                            .collection(COLLECTION_MESSAGES)
                            .whereEqualTo("isRead", false)
                            .get()
                            .addOnSuccessListener(querySnapshot -> {
                                for (QueryDocumentSnapshot docMsg : querySnapshot) {
                                    String senderId = docMsg.getString("senderId");
                                    if (!currentUserId.equals(senderId)) {
                                        docMsg.getReference().update("isRead", true);
                                    }
                                }
                            });
                });
    }

    // ==========================
    // CONVERSATIONS VENDEUR
    // ==========================
    public LiveData<List<Conversation>> getSellerConversationsRealtime() {
        MutableLiveData<List<Conversation>> conversationsLiveData = new MutableLiveData<>();
        String currentUserId = getCurrentUserId();

        if (currentUserId.isEmpty()) {
            conversationsLiveData.setValue(new ArrayList<>());
            return conversationsLiveData;
        }

        Log.d(TAG, "🔍 Recherche conversations pour vendeur: " + currentUserId);

        db.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("sellerId", currentUserId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Erreur écoute conversations vendeur", error);
                        conversationsLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (value == null) {
                        conversationsLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Conversation> conversations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Conversation conv = doc.toObject(Conversation.class);
                        conv.setId(doc.getId());
                        conversations.add(conv);
                    }
                    Log.d(TAG, "✅ Conversations vendeur trouvées: " + conversations.size());
                    conversationsLiveData.setValue(conversations);
                });

        return conversationsLiveData;
    }

    // ==========================
    // CONVERSATIONS ACHETEUR (BUYER)
    // ==========================
    public LiveData<List<Conversation>> getBuyerConversationsRealtime() {
        MutableLiveData<List<Conversation>> conversationsLiveData = new MutableLiveData<>();
        String currentUserId = getCurrentUserId();

        if (currentUserId.isEmpty()) {
            conversationsLiveData.setValue(new ArrayList<>());
            return conversationsLiveData;
        }

        Log.d(TAG, "🔍 Recherche conversations pour acheteur: " + currentUserId);

        db.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("buyerId", currentUserId)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "❌ Erreur écoute conversations acheteur", error);
                        conversationsLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    if (value == null) {
                        conversationsLiveData.setValue(new ArrayList<>());
                        return;
                    }

                    List<Conversation> conversations = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : value) {
                        Conversation conv = doc.toObject(Conversation.class);
                        conv.setId(doc.getId());
                        conversations.add(conv);
                    }
                    Log.d(TAG, "✅ Conversations acheteur trouvées: " + conversations.size());
                    conversationsLiveData.setValue(conversations);
                });

        return conversationsLiveData;
    }

    // ==========================
    // ALIAS POUR COMPATIBILITÉ
    // ==========================
    public LiveData<List<Conversation>> getClientConversationsRealtime() {
        return getBuyerConversationsRealtime();
    }

    // ==========================
    // COMPTEUR NON LUS VENDEUR
    // ==========================
    public LiveData<Integer> getSellerUnreadCount() {
        MutableLiveData<Integer> unreadCountLiveData = new MutableLiveData<>();
        String currentUserId = getCurrentUserId();

        if (currentUserId.isEmpty()) {
            unreadCountLiveData.setValue(0);
            return unreadCountLiveData;
        }

        db.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("sellerId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        unreadCountLiveData.setValue(0);
                        return;
                    }

                    int totalUnread = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        Long unread = doc.getLong("unreadCountSeller");
                        if (unread != null) {
                            totalUnread += unread.intValue();
                        }
                    }
                    unreadCountLiveData.setValue(totalUnread);
                });

        return unreadCountLiveData;
    }

    // ==========================
    // COMPTEUR NON LUS ACHETEUR
    // ==========================
    public LiveData<Integer> getBuyerUnreadCount() {
        MutableLiveData<Integer> unreadCountLiveData = new MutableLiveData<>();
        String currentUserId = getCurrentUserId();

        if (currentUserId.isEmpty()) {
            unreadCountLiveData.setValue(0);
            return unreadCountLiveData;
        }

        db.collection(COLLECTION_CONVERSATIONS)
                .whereEqualTo("buyerId", currentUserId)
                .addSnapshotListener((value, error) -> {
                    if (error != null || value == null) {
                        unreadCountLiveData.setValue(0);
                        return;
                    }

                    int totalUnread = 0;
                    for (QueryDocumentSnapshot doc : value) {
                        Long unread = doc.getLong("unreadCountBuyer");
                        if (unread != null) {
                            totalUnread += unread.intValue();
                        }
                    }
                    unreadCountLiveData.setValue(totalUnread);
                });

        return unreadCountLiveData;
    }

    // ==========================
    // ALIAS POUR COMPATIBILITÉ
    // ==========================
    public LiveData<Integer> getClientUnreadCount() {
        return getBuyerUnreadCount();
    }
}