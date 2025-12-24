package com.example.soukify.ui.chat;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.soukify.data.models.Conversation;
import com.example.soukify.data.models.Message;
import com.example.soukify.data.repositories.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private static final String TAG = "ChatViewModel";

    private final ChatRepository repository;

    // 🔒 MutableLiveData internes
    private final MutableLiveData<String> conversationIdLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private final MediatorLiveData<List<Message>> messagesLiveData = new MediatorLiveData<>();
    private LiveData<List<Message>> messagesSource;

    // ✅ Pour tracker quelle conversation on écoute
    private String currentListeningConversationId = null;

    public ChatViewModel() {
        repository = new ChatRepository();
    }

    // ==========================
    // GETTERS (LiveData exposés)
    // ==========================
    public LiveData<List<Message>> getMessages() {
        return messagesLiveData;
    }

    public LiveData<String> getConversationId() {
        return conversationIdLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    // ==========================
    // ✅ SETTER - Démarre l'écoute automatiquement
    // ==========================
    public void setConversationId(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            Log.e(TAG, "❌ setConversationId: conversationId est null ou vide");
            return;
        }

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "🆔 setConversationId appelé");
        Log.e(TAG, "   Nouveau ID: " + conversationId);
        Log.e(TAG, "   Ancien ID écouté: " + currentListeningConversationId);
        Log.e(TAG, "════════════════════════════════════════");

        // ✅ Mettre à jour l'ID
        conversationIdLiveData.setValue(conversationId);

        // ✅ Démarrer l'écoute (même si déjà en cours sur autre conversation)
        startListeningToMessages(conversationId);
        repository.markMessagesAsRead(conversationId);
    }

    // ==========================
    // ✅ CHARGER UNE CONVERSATION EXISTANTE
    // ==========================
    public void loadMessages(String conversationId) {
        if (conversationId == null || conversationId.isEmpty()) {
            Log.e(TAG, "❌ loadMessages: conversationId invalide");
            errorLiveData.setValue("ID de conversation invalide");
            return;
        }

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "🔄 loadMessages appelé");
        Log.e(TAG, "   ConversationId: " + conversationId);
        Log.e(TAG, "════════════════════════════════════════");

        // ✅ Démarrer l'écoute
        startListeningToMessages(conversationId);
        repository.markMessagesAsRead(conversationId);
    }

    // ==========================
    // ✅ INITIALISER CONVERSATION (CLIENT → VENDEUR)
    // ==========================
    public void initializeConversation(String shopId,
                                       String shopName,
                                       String shopImage,
                                       String sellerId) {

        isLoadingLiveData.setValue(true);

        String buyerId = FirebaseAuth.getInstance().getUid();
        if (buyerId == null || buyerId.isEmpty()) {
            errorLiveData.setValue("Utilisateur non connecté");
            isLoadingLiveData.setValue(false);
            return;
        }

        if (shopId == null || sellerId == null) {
            errorLiveData.setValue("Données manquantes (shopId / sellerId)");
            isLoadingLiveData.setValue(false);
            return;
        }

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "🆕 Initialisation conversation");
        Log.e(TAG, "   buyerId: " + buyerId);
        Log.e(TAG, "   sellerId: " + sellerId);
        Log.e(TAG, "   shopId: " + shopId);
        Log.e(TAG, "════════════════════════════════════════");

        repository.getOrCreateConversation(
                buyerId,
                sellerId,
                shopId,
                shopName,
                shopImage,
                new ChatRepository.OnConversationLoadedListener() {
                    @Override
                    public void onSuccess(Conversation conversation) {
                        Log.e(TAG, "✅ Conversation prête: " + conversation.getId());

                        // ✅ Mettre à jour l'ID
                        conversationIdLiveData.setValue(conversation.getId());

                        // ✅ Démarrer l'écoute
                        startListeningToMessages(conversation.getId());
                        repository.markMessagesAsRead(conversation.getId());

                        isLoadingLiveData.setValue(false);
                    }

                    @Override
                    public void onFailure(String error) {
                        Log.e(TAG, "❌ Erreur conversation: " + error);
                        errorLiveData.setValue(error);
                        isLoadingLiveData.setValue(false);
                    }
                }
        );
    }

    // ==========================
    // ✅ DÉMARRAGE ÉCOUTE MESSAGES
    // ==========================
    private void startListeningToMessages(String conversationId) {
        // ✅ Si on écoute déjà cette conversation, ne rien faire
        if (conversationId.equals(currentListeningConversationId)) {
            Log.w(TAG, "⚠️ Déjà en écoute sur cette conversation, ignoré");
            return;
        }

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "📡 DÉMARRAGE NOUVELLE ÉCOUTE");
        Log.e(TAG, "   Conversation: " + conversationId);
        Log.e(TAG, "   Arrêt ancienne écoute: " + currentListeningConversationId);
        Log.e(TAG, "════════════════════════════════════════");

        // ✅ Retirer l'ancienne source
        if (messagesSource != null) {
            messagesLiveData.removeSource(messagesSource);
            Log.d(TAG, "🗑️ Ancienne source supprimée");
        }

        // ✅ Mettre à jour la conversation écoutée
        currentListeningConversationId = conversationId;

        // ✅ Créer la nouvelle source
        messagesSource = repository.getMessagesRealtime(conversationId);

        messagesLiveData.addSource(messagesSource, messages -> {
            if (messages != null) {
                Log.e(TAG, "════════════════════════════════════════");
                Log.e(TAG, "📨 MESSAGES REÇUS DANS LE VIEWMODEL");
                Log.e(TAG, "   Nombre: " + messages.size());
                Log.e(TAG, "   Conversation: " + currentListeningConversationId);

                for (int i = 0; i < messages.size(); i++) {
                    Message m = messages.get(i);
                    Log.e(TAG, "   Message " + (i+1) + ": " + m.getText());
                    Log.e(TAG, "      SenderId: " + m.getSenderId());
                    Log.e(TAG, "      SenderName: " + m.getSenderName());
                }
                Log.e(TAG, "════════════════════════════════════════");

                messagesLiveData.setValue(messages);
            } else {
                Log.w(TAG, "⚠️ Messages null reçus");
            }
        });
    }

    // ==========================
    // ✅ ENVOYER MESSAGE (CORRIGÉE)
    // ==========================
    public void sendMessage(String text) {
        String conversationId = conversationIdLiveData.getValue();

        if (conversationId == null || conversationId.isEmpty()) {
            errorLiveData.setValue("Conversation non prête");
            return;
        }

        if (text == null || text.trim().isEmpty()) {
            errorLiveData.setValue("Message vide");
            return;
        }

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "📤 ENVOI MESSAGE");
        Log.e(TAG, "   Text: " + text.trim());
        Log.e(TAG, "   ConversationId: " + conversationId);
        Log.e(TAG, "════════════════════════════════════════");

        // 🔥 APPEL CORRIGÉ : seulement conversationId et text
        repository.sendMessage(
                conversationId,
                text.trim(),
                new ChatRepository.SendMessageCallback() {
                    @Override
                    public void onSuccess() {
                        Log.e(TAG, "✅ Message envoyé avec succès");
                        // Le message apparaîtra automatiquement via le listener temps réel
                    }

                    @Override
                    public void onError(String error) {
                        Log.e(TAG, "❌ Envoi échoué: " + error);
                        errorLiveData.setValue(error);
                    }
                }
        );
    }

    // ==========================
    // MARQUER COMME LUS
    // ==========================
    public void markMessagesAsRead() {
        String conversationId = conversationIdLiveData.getValue();
        if (conversationId != null) {
            repository.markMessagesAsRead(conversationId);
        }
    }

    // ==========================
    // NETTOYAGE
    // ==========================
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.e(TAG, "🧹 ViewModel détruit");

        currentListeningConversationId = null;

        if (messagesSource != null) {
            messagesLiveData.removeSource(messagesSource);
        }
    }
}