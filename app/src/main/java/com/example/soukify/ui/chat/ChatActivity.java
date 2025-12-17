package com.example.soukify.ui.chat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.data.models.Message;

import java.util.ArrayList;

public class ChatActivity extends AppCompatActivity {

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private ProgressBar progressBar;
    private TextView tvEmptyMessages;
    private Toolbar toolbar;

    private ChatViewModel viewModel;
    private MessagesAdapter adapter;
    private ArrayList<Message> messagesList;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        // Récupérer les extras (id de la boutique et vendeur)
        String shopId = getIntent().getStringExtra("shopId");
        String shopName = getIntent().getStringExtra("shopName");
        String shopImage = getIntent().getStringExtra("shopImage");
        String sellerId = getIntent().getStringExtra("sellerId");

        // Initialiser les vues
        toolbar = findViewById(R.id.toolbar);
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        progressBar = findViewById(R.id.progressBar);
        tvEmptyMessages = findViewById(R.id.tvEmptyMessages);

        // Configurer Toolbar
        toolbar.setTitle(shopName != null ? shopName : "Chat");
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());

        // Initialiser la liste et l'adapter
        messagesList = new ArrayList<>();
        adapter = new MessagesAdapter(messagesList);
        rvMessages.setLayoutManager(new LinearLayoutManager(this));
        rvMessages.setAdapter(adapter);

        // Initialiser ViewModel
        viewModel = new ViewModelProvider(this).get(ChatViewModel.class);

        // Initialiser la conversation
        viewModel.initializeConversation(shopId, shopName, shopImage, sellerId);

        // Observer l'état de chargement
        viewModel.getIsLoading().observe(this, isLoading ->
                progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE)
        );

        // Observer les messages
        viewModel.getMessages().observe(this, messages -> {
            messagesList.clear();
            if (messages != null && !messages.isEmpty()) {
                messagesList.addAll(messages);
                tvEmptyMessages.setVisibility(View.GONE);

                // Scroll vers le dernier message
                rvMessages.scrollToPosition(messagesList.size() - 1);
            } else {
                tvEmptyMessages.setVisibility(View.VISIBLE);
            }
            adapter.notifyDataSetChanged();
        });

        // Observer les erreurs
        viewModel.getError().observe(this, error -> {
            if (error != null && !error.isEmpty()) {
                tvEmptyMessages.setText(error);
                tvEmptyMessages.setVisibility(View.VISIBLE);
            }
        });

        // Envoyer message
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void sendMessage() {
        String text = etMessage.getText().toString().trim();
        if (!TextUtils.isEmpty(text)) {
            // Nom de l'utilisateur "Vous" ou récupérer depuis profil
            viewModel.sendMessage(text, "Vous");
            etMessage.setText("");

            // Scroll vers le bas après envoi
            rvMessages.scrollToPosition(messagesList.size() - 1);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Marquer les messages comme lus à chaque ouverture
        viewModel.markMessagesAsRead();
    }
}

