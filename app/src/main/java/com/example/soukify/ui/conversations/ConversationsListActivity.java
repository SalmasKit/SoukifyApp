package com.example.soukify.ui.conversations;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.data.models.Conversation;
import com.example.soukify.ui.chat.ChatActivity;

import java.util.ArrayList;

public class ConversationsListActivity extends AppCompatActivity
        implements ConversationsAdapter.OnConversationClickListener {

    private static final String TAG = "ConversationsList";

    public static final String EXTRA_IS_SELLER_VIEW = "isSellerView";

    private RecyclerView rvConversations;
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private Toolbar toolbar;

    private ConversationsViewModel viewModel;
    private ConversationsAdapter adapter;
    private final ArrayList<Conversation> conversationsList = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversations_list);

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "🚀 ConversationsListActivity créée");
        Log.e(TAG, "════════════════════════════════════════");

        // ==========================
        // Views
        // ==========================
        toolbar = findViewById(R.id.toolbar);
        rvConversations = findViewById(R.id.rvConversations);
        progressBar = findViewById(R.id.progressBar);
        tvEmpty = findViewById(R.id.tvEmpty);

        // TextView titre dans la toolbar
        TextView tvTitle = findViewById(R.id.tvTitle);

        // ImageView flèche dans la toolbar
        ImageView ivBack = findViewById(R.id.ivBack);
        if (ivBack != null) {
            ivBack.setOnClickListener(v -> finish()); // fermeture de l'activité
        }

        // Supprimer la flèche par défaut et le titre de la toolbar
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowHomeEnabled(false);
            getSupportActionBar().setDisplayHomeAsUpEnabled(false);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        // ==========================
        // RecyclerView
        // ==========================
        adapter = new ConversationsAdapter(this, conversationsList, this);
        rvConversations.setLayoutManager(new LinearLayoutManager(this));
        rvConversations.setAdapter(adapter);

        // ==========================
        // ViewModel
        // ==========================
        viewModel = new ViewModelProvider(this).get(ConversationsViewModel.class);

        viewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            Log.d(TAG, "⏳ Loading = " + isLoading);
        });

        viewModel.getConversations().observe(this, conversations -> {
            Log.e(TAG, "════════════════════════════════════════");
            Log.e(TAG, "📩 Conversations reçues: " + (conversations != null ? conversations.size() : 0));
            Log.e(TAG, "════════════════════════════════════════");

            conversationsList.clear();

            if (conversations != null && !conversations.isEmpty()) {
                conversationsList.addAll(conversations);
                tvEmpty.setVisibility(View.GONE);
                rvConversations.setVisibility(View.VISIBLE);
            } else {
                rvConversations.setVisibility(View.GONE);
                tvEmpty.setVisibility(View.VISIBLE);
                tvEmpty.setText("📭\n\nAucune conversation\nVos messages apparaîtront ici");
            }

            adapter.notifyDataSetChanged();
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "❌ Erreur: " + error);
                tvEmpty.setText("❌ " + error);
                tvEmpty.setVisibility(View.VISIBLE);
            }
        });

        // ==========================
        // Charger selon rôle et mettre le texte dans tvTitle
        // ==========================
        boolean isSellerView = getIntent().getBooleanExtra(EXTRA_IS_SELLER_VIEW, false);
        Log.e(TAG, "👤 isSellerView: " + isSellerView);

        if (tvTitle != null) {
            if (isSellerView) {
                tvTitle.setText("💬 Messages clients");
                viewModel.loadSellerConversations();
            } else {
                tvTitle.setText("💬 Mes messages");
                viewModel.loadClientConversations();
            }
        }
    }

    // ==========================
    // ✅ Clic sur conversation
    // ==========================
    @Override
    public void onConversationClick(Conversation conversation) {
        if (conversation == null || conversation.getId() == null) {
            Log.e(TAG, "❌ Conversation invalide");
            return;
        }

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "🔥 CLIC SUR CONVERSATION");
        Log.e(TAG, "   ID: " + conversation.getId());
        Log.e(TAG, "   ShopName: " + conversation.getShopName());
        Log.e(TAG, "   ShopId: " + conversation.getShopId());
        Log.e(TAG, "   SellerId: " + conversation.getSellerId());
        Log.e(TAG, "   BuyerId: " + conversation.getBuyerId());
        Log.e(TAG, "════════════════════════════════════════");

        Intent intent = new Intent(this, ChatActivity.class);

        // ✅ PASSER TOUTES LES INFOS NÉCESSAIRES
        intent.putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversation.getId());
        intent.putExtra(ChatActivity.EXTRA_SHOP_ID, conversation.getShopId());
        intent.putExtra(ChatActivity.EXTRA_SELLER_ID, conversation.getSellerId());
        intent.putExtra(ChatActivity.EXTRA_SHOP_IMAGE, conversation.getShopImage());

        // 🔹 PASSER LE NOM À AFFICHER DANS CHAT
        boolean isSellerView = getIntent().getBooleanExtra(EXTRA_IS_SELLER_VIEW, false);
        if (isSellerView) {
            // côté vendeur → afficher le nom de l'acheteur
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, conversation.getBuyerName());
        } else {
            // côté client → afficher le nom du shop
            intent.putExtra(ChatActivity.EXTRA_OTHER_USER_NAME, conversation.getShopName());
        }

        // Indiquer le rôle pour ChatActivity si besoin
        intent.putExtra(EXTRA_IS_SELLER_VIEW, isSellerView);

        startActivity(intent);
    }
}