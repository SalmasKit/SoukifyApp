package com.example.soukify.data.repositories;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.soukify.data.models.Conversation;
import com.example.soukify.data.models.Message;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatRepository {

    private static final String TAG = "ChatRepository";

    private final FirebaseFirestore db;
    private final FirebaseAuth auth;

    public ChatRepository() {
        this.db = FirebaseFirestore.getInstance();
        this.auth = FirebaseAuth.getInstance();
    }

    private String getCurrentUserId() {
        return auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : "";
    }

    // ==========================
    // Conversation
    // ==========================
    public void getOrCreateConversation(String shopId, String shopName,
                                        String shopImage, String sellerId,
                                        ConversationCallback callback) {
        String buyerId = getCurrentUserId();
        if (buyerId.isEmpty()) {
            callback.onError("Utilisateur non connecté");
            return;
        }

        db.collection("conversations")
                .whereEqualTo("buyerId", buyerId)
                .whereEqualTo("shopId", shopId)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!querySnapshot.isEmpty()) {
                        String conversationId = querySnapshot.getDocuments().get(0).getId();
                        callback.onSuccess(conversationId);
                    } else {
                        // Créer nouvelle conversation
                        Map<String, Object> conversation = new HashMap<>();
                        conversation.put("buyerId", buyerId);
                        conversation.put("sellerId", sellerId);
                        conversation.put("shopId", shopId);
                        conversation.put("shopName", shopName);
                        conversation.put("shopImage", shopImage);
                        conversation.put("lastMessage", "");
                        conversation.put("lastMessageTimestamp", System.currentTimeMillis());
                        conversation.put("unreadCountBuyer", 0);
                        conversation.put("unreadCountSeller", 0);
                        conversation.put("createdAt", FieldValue.serverTimestamp());

                        db.collection("conversations")
                                .add(conversation)
                                .addOnSuccessListener(documentReference ->
                                        callback.onSuccess(documentReference.getId()))
                                .addOnFailureListener(e ->
                                        callback.onError("Erreur création conversation: " + e.getMessage()));
                    }
                })
                .addOnFailureListener(e ->
                        callback.onError("Erreur récupération conversation: " + e.getMessage()));
    }

    // ==========================
    // Envoyer message
    // ==========================
    public void sendMessage(String conversationId, String text, String senderName,
                            SendMessageCallback callback) {
        String senderId = getCurrentUserId();
        if (senderId.isEmpty() || text.trim().isEmpty()) {
            callback.onError("Données invalides");
            return;
        }

        Map<String, Object> message = new HashMap<>();
        message.put("conversationId", conversationId);
        message.put("senderId", senderId);
        message.put("senderName", senderName);
        message.put("text", text.trim());
        message.put("timestamp", System.currentTimeMillis());
        message.put("isRead", false);
        message.put("createdAt", FieldValue.serverTimestamp());

        // CHEMIN CORRECT: messages/{conversationId}/messages/{messageId}
        db.collection("messages")
                .document(conversationId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(docRef -> {
                    updateConversationAfterMessage(conversationId, text.trim(), senderId);
                    callback.onSuccess();
                })
                .addOnFailureListener(e -> callback.onError("Erreur d'envoi: " + e.getMessage()));
    }

    private void updateConversationAfterMessage(String conversationId, String lastMessage,
                                                String senderId) {
        db.collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String buyerId = doc.getString("buyerId");
                    boolean isSenderBuyer = senderId.equals(buyerId);

                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", lastMessage);
                    updates.put("lastMessageTimestamp", System.currentTimeMillis());
                    if (isSenderBuyer) updates.put("unreadCountSeller", FieldValue.increment(1));
                    else updates.put("unreadCountBuyer", FieldValue.increment(1));

                    db.collection("conversations")
                            .document(conversationId)
                            .update(updates);
                });
    }

    // ==========================
    // Écoute messages temps réel
    // ==========================
    public LiveData<List<Message>> getMessagesRealtime(String conversationId) {
        MutableLiveData<List<Message>> messagesLiveData = new MutableLiveData<>();

        db.collection("messages")
                .document(conversationId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Erreur écoute messages", error);
                        return;
                    }
                    if (value != null) {
                        List<Message> messages = new ArrayList<>();
                        for (QueryDocumentSnapshot doc : value) {
                            Message message = doc.toObject(Message.class);
                            message.setId(doc.getId());
                            messages.add(message);
                        }
                        messagesLiveData.setValue(messages);
                    }
                });

        return messagesLiveData;
    }

    // ==========================
    // Marquer messages lus
    // ==========================
    public void markMessagesAsRead(String conversationId) {
        String currentUserId = getCurrentUserId();

        db.collection("conversations")
                .document(conversationId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    String buyerId = doc.getString("buyerId");
                    boolean isBuyer = currentUserId.equals(buyerId);
                    String field = isBuyer ? "unreadCountBuyer" : "unreadCountSeller";

                    db.collection("conversations")
                            .document(conversationId)
                            .update(field, 0);

                    db.collection("messages")
                            .document(conversationId)
                            .collection("messages")
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
    // Callbacks
    // ==========================
    public interface ConversationCallback {
        void onSuccess(String conversationId);
        void onError(String error);
    }

    public interface SendMessageCallback {
        void onSuccess();
        void onError(String error);
    }
}


