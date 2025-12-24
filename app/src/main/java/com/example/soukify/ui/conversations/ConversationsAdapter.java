package com.example.soukify.ui.conversations;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.soukify.R;
import com.example.soukify.data.models.Conversation;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;

public class ConversationsAdapter
        extends RecyclerView.Adapter<ConversationsAdapter.ConversationViewHolder> {

    private static final String TAG = "ConversationsAdapter";

    private final Context context;
    private List<Conversation> conversations;
    private final OnConversationClickListener listener;

    // 🔑 Utilisateur connecté
    private final String currentUserId;

    public ConversationsAdapter(Context context,
                                List<Conversation> conversations,
                                OnConversationClickListener listener) {
        this.context = context;
        this.conversations = conversations != null ? conversations : new ArrayList<>();
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getUid();

        Log.d(TAG, "✅ Adapter créé avec currentUserId: " + currentUserId);
    }

    @NonNull
    @Override
    public ConversationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context)
                .inflate(R.layout.item_conversation, parent, false);
        return new ConversationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ConversationViewHolder holder, int position) {

        Conversation conversation = conversations.get(position);
        if (conversation == null) return;

        // ==========================
        // 🔥 AFFICHER LE BON NOM
        // ==========================
        String displayName;

        if (currentUserId != null && currentUserId.equals(conversation.getSellerId())) {
            // 🔥 JE SUIS LE VENDEUR → Afficher le nom de l'ACHETEUR
            displayName = conversation.getBuyerName() != null
                    ? conversation.getBuyerName()
                    : "Client";

            Log.d(TAG, "👨‍💼 Mode VENDEUR - Affichage client: " + displayName);

        } else {
            // 👤 JE SUIS L'ACHETEUR → Afficher le nom de la BOUTIQUE
            displayName = conversation.getShopName() != null
                    ? conversation.getShopName()
                    : "Boutique";

            Log.d(TAG, "🛍️ Mode ACHETEUR - Affichage boutique: " + displayName);
        }

        holder.tvName.setText(displayName);

        // ==========================
        // Dernier message
        // ==========================
        holder.tvLastMessage.setText(
                conversation.getLastMessage() != null
                        && !conversation.getLastMessage().isEmpty()
                        ? conversation.getLastMessage()
                        : "Aucun message"
        );

        // ==========================
        // Timestamp
        // ==========================
        Long timestamp = conversation.getLastMessageTimestamp();
        holder.tvTimestamp.setText(
                timestamp != null ? getTimeAgo(timestamp) : ""
        );

        // ==========================
        // ✅ Badge non-lus (CLIENT / VENDEUR)
        // ==========================
        int unreadCount = 0;

        if (currentUserId != null) {
            if (currentUserId.equals(conversation.getSellerId())) {
                // Je suis le vendeur
                unreadCount = conversation.getUnreadCountSeller();
            } else if (currentUserId.equals(conversation.getBuyerId())) {
                // Je suis l'acheteur
                unreadCount = conversation.getUnreadCountBuyer();
            }
        }

        if (unreadCount > 0) {
            holder.tvUnreadBadge.setVisibility(View.VISIBLE);
            holder.tvUnreadBadge.setText(String.valueOf(unreadCount));
        } else {
            holder.tvUnreadBadge.setVisibility(View.GONE);
        }

        // ==========================
        // Image boutique
        // ==========================
        if (conversation.getShopImage() != null
                && !conversation.getShopImage().isEmpty()) {

            Glide.with(context)
                    .load(conversation.getShopImage())
                    .placeholder(R.drawable.ic_launcher_background)
                    .circleCrop()
                    .into(holder.ivShopImage);

        } else {
            holder.ivShopImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // ==========================
        // Clic
        // ==========================
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onConversationClick(conversation);
            }
        });
    }

    @Override
    public int getItemCount() {
        return conversations != null ? conversations.size() : 0;
    }

    // ==========================
    // Update liste
    // ==========================
    public void updateConversations(List<Conversation> newConversations) {
        this.conversations = newConversations != null
                ? newConversations
                : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ==========================
    // ViewHolder
    // ==========================
    static class ConversationViewHolder extends RecyclerView.ViewHolder {

        ImageView ivShopImage;
        TextView tvName, tvLastMessage, tvTimestamp, tvUnreadBadge;

        public ConversationViewHolder(@NonNull View itemView) {
            super(itemView);
            ivShopImage = itemView.findViewById(R.id.ivShopImage);
            tvName = itemView.findViewById(R.id.tvName);
            tvLastMessage = itemView.findViewById(R.id.tvLastMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvUnreadBadge = itemView.findViewById(R.id.tvUnreadBadge);
        }
    }

    // ==========================
    // TimeAgo
    // ==========================
    private String getTimeAgo(long timestamp) {
        long diff = System.currentTimeMillis() - timestamp;

        long minutes = diff / (1000 * 60);
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) return days + "j";
        if (hours > 0) return hours + "h";
        if (minutes > 0) return minutes + " min";
        return "maintenant";
    }

    // ==========================
    // Interface clic
    // ==========================
    public interface OnConversationClickListener {
        void onConversationClick(Conversation conversation);
    }
}