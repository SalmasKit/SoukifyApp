package com.example.soukify.ui.chat;

import android.os.Bundle;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.auth.FirebaseAuth;

public class ChatActivity extends AppCompatActivity {

    private static final String TAG = "ChatActivity";

    public static final String EXTRA_CONVERSATION_ID = "conversation_id";
    public static final String EXTRA_SHOP_NAME = "shop_name";
    public static final String EXTRA_SHOP_ID = "shop_id";
    public static final String EXTRA_SELLER_ID = "seller_id";
    public static final String EXTRA_SHOP_IMAGE = "shop_image";
    public static final String EXTRA_OTHER_USER_NAME = "extra_other_user_name";
    public static final String EXTRA_IS_SELLER_VIEW = "extra_is_seller_view";

    private ChatViewModel viewModel;
    private MessagesAdapter adapter;

    private EditText etMessage;
    private ImageButton btnSend;
    private RecyclerView rvMessages;
    private MaterialToolbar toolbar;
    private TextView tvTitle;

    private String conversationId;
    private String currentUserId;
    private boolean isSellerView;
    private String contactName; // nom de l'autre utilisateur (shop ou client)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        currentUserId = FirebaseAuth.getInstance().getUid();
        if (currentUserId == null || currentUserId.isEmpty()) {
            Toast.makeText(this, "❌ Vous devez être connecté", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // ==========================
        // SETUP VIEWS
        // ==========================
        toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        tvTitle = findViewById(R.id.tvTitle);
        ImageView ivBack = findViewById(R.id.ivBack);
        ivBack.setOnClickListener(v -> finish());

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);

        // ==========================
        // SETUP VIEWMODEL & ADAPTER
        // ==========================
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);
        setupRecyclerView();
        observeViewModel();
        setupSendButton();

        // ==========================
        // 🔥 RÉCUPÉRATION DES EXTRAS
        // ==========================
        conversationId = getIntent().getStringExtra(EXTRA_CONVERSATION_ID);
        String shopName = getIntent().getStringExtra(EXTRA_SHOP_NAME);
        String otherUserName = getIntent().getStringExtra(EXTRA_OTHER_USER_NAME); // 🔥 Nom passé depuis ConversationsListActivity
        isSellerView = getIntent().getBooleanExtra(EXTRA_IS_SELLER_VIEW, false);

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "🚀 ChatActivity créée");
        Log.e(TAG, "   conversationId: " + conversationId);
        Log.e(TAG, "   shopName: " + shopName);
        Log.e(TAG, "   otherUserName: " + otherUserName);
        Log.e(TAG, "   isSellerView: " + isSellerView);
        Log.e(TAG, "════════════════════════════════════════");

        // 🔥 DÉTERMINER LE NOM À AFFICHER DANS LA TOOLBAR
        if (otherUserName != null && !otherUserName.isEmpty()) {
            // Nom passé explicitement par ConversationsListActivity
            contactName = otherUserName;
        } else if (isSellerView) {
            // Fallback : si c'est le vendeur, afficher "Client"
            contactName = "Client";
        } else {
            // Fallback : si c'est l'acheteur, afficher le nom du shop
            contactName = shopName != null ? shopName : "💬 Chat";
        }

        // 🔥 PROTECTION CONTRE NULL
        if (tvTitle != null) {
            tvTitle.setText(contactName);
        } else {
            Log.e(TAG, "❌ tvTitle est NULL - vérifiez activity_chat.xml");
        }

        // ==========================
        // CHARGER LA CONVERSATION
        // ==========================
        if (conversationId != null && !conversationId.isEmpty()) {
            // Conversation existante
            Log.d(TAG, "✅ Chargement conversation existante: " + conversationId);
            viewModel.setConversationId(conversationId);
        } else {
            // Nouvelle conversation
            String shopId = getIntent().getStringExtra(EXTRA_SHOP_ID);
            String sellerId = getIntent().getStringExtra(EXTRA_SELLER_ID);
            String shopImage = getIntent().getStringExtra(EXTRA_SHOP_IMAGE);

            if (shopId == null || sellerId == null) {
                Toast.makeText(this, "❌ Données manquantes", Toast.LENGTH_LONG).show();
                finish();
                return;
            }

            Log.d(TAG, "🆕 Initialisation nouvelle conversation");
            viewModel.initializeConversation(shopId, shopName, shopImage, sellerId);
        }
    }

    // ==========================
    // SETUP RECYCLERVIEW
    // ==========================
    private void setupRecyclerView() {
        adapter = new MessagesAdapter(currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);
    }

    // ==========================
    // OBSERVER VIEWMODEL
    // ==========================
    private void observeViewModel() {
        viewModel.getMessages().observe(this, messages -> {
            if (messages != null) {
                Log.d(TAG, "📨 Messages reçus: " + messages.size());
                adapter.submitList(messages);
                if (!messages.isEmpty()) {
                    rvMessages.scrollToPosition(messages.size() - 1);
                }
            }
        });

        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                Log.e(TAG, "❌ Erreur: " + error);
                Toast.makeText(this, "❌ " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ==========================
    // 🔥 SETUP BOUTON ENVOI (CORRIGÉ)
    // ==========================
    private void setupSendButton() {
        btnSend.setOnClickListener(v -> {
            String text = etMessage.getText().toString().trim();

            if (text.isEmpty()) {
                Toast.makeText(this, "⚠️ Message vide", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "📤 Envoi message: " + text);

            // 🔥 APPEL CORRIGÉ : senderName est récupéré automatiquement par le repository
            viewModel.sendMessage(text);

            etMessage.setText("");
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        viewModel.markMessagesAsRead();
    }
}