package com.example.soukify.ui.chat;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.soukify.data.models.Message;
import com.example.soukify.data.repositories.ChatRepository;

import java.util.List;

public class ChatViewModel extends ViewModel {

    private final ChatRepository repository;

    private final MutableLiveData<String> conversationIdLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoadingLiveData = new MutableLiveData<>();
    private final MutableLiveData<String> errorLiveData = new MutableLiveData<>();

    private final MediatorLiveData<List<Message>> messagesLiveData = new MediatorLiveData<>();
    private LiveData<List<Message>> messagesSource;

    public ChatViewModel() {
        repository = new ChatRepository();
    }

    // ==========================
    // Getters
    // ==========================
    public LiveData<List<Message>> getMessages() { return messagesLiveData; }
    public LiveData<String> getConversationId() { return conversationIdLiveData; }
    public LiveData<Boolean> getIsLoading() { return isLoadingLiveData; }
    public LiveData<String> getError() { return errorLiveData; }

    // ==========================
    // Initialiser la conversation
    // ==========================
    public void initializeConversation(String shopId, String shopName,
                                       String shopImage, String sellerId) {
        isLoadingLiveData.setValue(true);

        repository.getOrCreateConversation(shopId, shopName, shopImage, sellerId,
                new ChatRepository.ConversationCallback() {
                    @Override
                    public void onSuccess(String conversationId) {
                        conversationIdLiveData.setValue(conversationId);
                        listenToMessages(conversationId);
                        repository.markMessagesAsRead(conversationId);
                        isLoadingLiveData.setValue(false);
                    }

                    @Override
                    public void onError(String error) {
                        errorLiveData.setValue(error);
                        isLoadingLiveData.setValue(false);
                    }
                });
    }

    // ==========================
    // Écoute des messages en temps réel
    // ==========================
    private void listenToMessages(String conversationId) {
        messagesSource = repository.getMessagesRealtime(conversationId);
        messagesLiveData.addSource(messagesSource, messages -> messagesLiveData.setValue(messages));
    }

    // ==========================
    // Envoyer un message
    // ==========================
    public void sendMessage(String text, String senderName) {
        String conversationId = conversationIdLiveData.getValue();
        if (conversationId == null) {
            errorLiveData.setValue("Conversation non initialisée");
            return;
        }
        if (text == null || text.trim().isEmpty()) {
            errorLiveData.setValue("Le message ne peut pas être vide");
            return;
        }

        repository.sendMessage(conversationId, text.trim(), senderName,
                new ChatRepository.SendMessageCallback() {
                    @Override
                    public void onSuccess() { /* Message envoyé avec succès */ }

                    @Override
                    public void onError(String error) {
                        errorLiveData.setValue("Erreur d'envoi: " + error);
                    }
                });
    }

    // ==========================
    // Marquer les messages comme lus
    // ==========================
    public void markMessagesAsRead() {
        String conversationId = conversationIdLiveData.getValue();
        if (conversationId != null) {
            repository.markMessagesAsRead(conversationId);
        }
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Supprimer la source pour éviter fuite mémoire
        if (messagesSource != null) {
            messagesLiveData.removeSource(messagesSource);
        }
    }
}
