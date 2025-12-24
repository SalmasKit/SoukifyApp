package com.example.soukify.ui.conversations;

import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.soukify.data.models.Conversation;
import com.example.soukify.data.repositories.ChatRepository;
import com.google.firebase.auth.FirebaseAuth;

import java.util.List;

public class ConversationsViewModel extends ViewModel {

    private static final String TAG = "ConversationsViewModel";

    private final ChatRepository repository;

    private final MediatorLiveData<List<Conversation>> conversationsLiveData =
            new MediatorLiveData<>();

    private final MutableLiveData<Boolean> isLoadingLiveData =
            new MutableLiveData<>(false);

    private final MutableLiveData<String> errorLiveData =
            new MutableLiveData<>();

    private LiveData<List<Conversation>> conversationsSource;

    public ConversationsViewModel() {
        repository = new ChatRepository();
        Log.d(TAG, "✅ ConversationsViewModel créé");
    }

    // ==========================
    // Getters
    // ==========================
    public LiveData<List<Conversation>> getConversations() {
        return conversationsLiveData;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoadingLiveData;
    }

    public LiveData<String> getError() {
        return errorLiveData;
    }

    // ==========================
    // Vendeur
    // ==========================
    public void loadSellerConversations() {

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Log.e(TAG, "❌ Vendeur non connecté");
            errorLiveData.postValue("Utilisateur non connecté");
            return;
        }

        Log.d(TAG, "🔄 loadSellerConversations pour userId=" + userId);
        isLoadingLiveData.postValue(true);

        if (conversationsSource != null) {
            conversationsLiveData.removeSource(conversationsSource);
            Log.d(TAG, "🗑️ Ancienne source retirée");
        }

        conversationsSource = repository.getSellerConversationsRealtime();

        conversationsLiveData.addSource(conversationsSource, conversations -> {
            Log.d(TAG, "📥 Conversations vendeur reçues: "
                    + (conversations != null ? conversations.size() : "null"));

            isLoadingLiveData.postValue(false);

            if (conversations != null) {
                conversationsLiveData.postValue(conversations);

                for (Conversation c : conversations) {
                    Log.d(TAG, "   ✔ " + c.getShopName()
                            + " | id=" + c.getId()
                            + " | unread=" + c.getUnreadCountSeller());
                }
            } else {
                errorLiveData.postValue("Erreur chargement conversations vendeur");
            }
        });
    }

    // ==========================
    // Client
    // ==========================
    public void loadClientConversations() {

        String userId = FirebaseAuth.getInstance().getUid();
        if (userId == null) {
            Log.e(TAG, "❌ Client non connecté");
            errorLiveData.postValue("Utilisateur non connecté");
            return;
        }

        Log.d(TAG, "🔄 loadClientConversations pour userId=" + userId);
        isLoadingLiveData.postValue(true);

        if (conversationsSource != null) {
            conversationsLiveData.removeSource(conversationsSource);
            Log.d(TAG, "🗑️ Ancienne source retirée");
        }

        conversationsSource = repository.getClientConversationsRealtime();

        conversationsLiveData.addSource(conversationsSource, conversations -> {
            Log.d(TAG, "📥 Conversations client reçues: "
                    + (conversations != null ? conversations.size() : "null"));

            isLoadingLiveData.postValue(false);

            if (conversations != null) {
                conversationsLiveData.postValue(conversations);

                for (Conversation c : conversations) {
                    Log.d(TAG, "   ✔ " + c.getShopName()
                            + " | id=" + c.getId()
                            + " | unread=" + c.getUnreadCountBuyer());
                }
            } else {
                errorLiveData.postValue("Erreur chargement conversations client");
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (conversationsSource != null) {
            conversationsLiveData.removeSource(conversationsSource);
            Log.d(TAG, "🧹 ViewModel détruit, source retirée");
        }
    }
}