package com.example.soukify.ui.chat;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.soukify.R;
import com.example.soukify.data.models.Message;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final String TAG = "MessagesAdapter";
    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private List<Message> messages;
    private final String currentUserId;

    public MessagesAdapter(String currentUserId) {
        this.currentUserId = currentUserId;
        this.messages = new ArrayList<>();

        Log.e(TAG, "════════════════════════════════════════");
        Log.e(TAG, "🔧 MessagesAdapter créé");
        Log.e(TAG, "   CurrentUserId: " + currentUserId);
        Log.e(TAG, "════════════════════════════════════════");
    }

    // ==========================
    // ✅ Mettre à jour la liste
    // ==========================
    public void submitList(List<Message> newMessages) {
        if (newMessages != null) {
            Log.e(TAG, "════════════════════════════════════════");
            Log.e(TAG, "📝 submitList appelé");
            Log.e(TAG, "   Ancien nombre: " + this.messages.size());
            Log.e(TAG, "   Nouveau nombre: " + newMessages.size());

            this.messages = new ArrayList<>(newMessages);
            notifyDataSetChanged();

            Log.e(TAG, "   ✅ Adapter mis à jour");
            Log.e(TAG, "════════════════════════════════════════");
        } else {
            Log.e(TAG, "⚠️ submitList reçu NULL");
        }
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messages.get(position);

        if (message == null) {
            Log.e(TAG, "⚠️ Message NULL à position " + position);
            return VIEW_TYPE_RECEIVED;
        }

        String senderId = message.getSenderId();

        // ✅ LOG DÉTAILLÉ
        boolean isSent = senderId != null && senderId.equals(currentUserId);

        Log.d(TAG, "getItemViewType pos=" + position +
                " | senderId=" + senderId +
                " | currentUserId=" + currentUserId +
                " | isSent=" + isSent);

        return isSent ? VIEW_TYPE_SENT : VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        Log.d(TAG, "onCreateViewHolder: viewType=" +
                (viewType == VIEW_TYPE_SENT ? "SENT" : "RECEIVED"));

        if (viewType == VIEW_TYPE_SENT) {
            View view = inflater.inflate(R.layout.item_message_sent, parent, false);
            return new SentMessageViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_received, parent, false);
            return new ReceivedMessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messages.get(position);

        if (message == null) {
            Log.e(TAG, "⚠️ Message NULL au bind position " + position);
            return;
        }

        Log.d(TAG, "onBindViewHolder pos=" + position +
                " | text=" + message.getText() +
                " | senderId=" + message.getSenderId());

        if (holder instanceof SentMessageViewHolder) {
            ((SentMessageViewHolder) holder).bind(message);
        } else if (holder instanceof ReceivedMessageViewHolder) {
            ((ReceivedMessageViewHolder) holder).bind(message);
        }
    }

    @Override
    public int getItemCount() {
        int count = messages.size();
        Log.d(TAG, "getItemCount: " + count);
        return count;
    }

    // ==========================
    // ✅ ViewHolder messages envoyés
    // ==========================
    static class SentMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTime;

        public SentMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);

            if (tvMessage == null) {
                Log.e(TAG, "❌ tvMessage NULL dans item_message_sent.xml");
            }
            if (tvTime == null) {
                Log.e(TAG, "❌ tvTime NULL dans item_message_sent.xml");
            }
        }

        public void bind(Message message) {
            if (message == null) {
                Log.e(TAG, "⚠️ bind() reçu message NULL (SENT)");
                return;
            }

            String text = message.getText() != null ? message.getText() : "";
            tvMessage.setText(text);

            if (tvTime != null && message.getTimestamp() > 0) {
                tvTime.setText(formatTime(message.getTimestamp()));
            }

            Log.d(TAG, "✅ Message SENT bindé: " + text);
        }
    }

    // ==========================
    // ✅ ViewHolder messages reçus
    // ==========================
    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMessage;
        private final TextView tvTime;
        private final TextView tvSenderName;

        public ReceivedMessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);

            if (tvMessage == null) {
                Log.e(TAG, "❌ tvMessage NULL dans item_message_received.xml");
            }
            if (tvTime == null) {
                Log.e(TAG, "❌ tvTime NULL dans item_message_received.xml");
            }
            if (tvSenderName == null) {
                Log.e(TAG, "❌ tvSenderName NULL dans item_message_received.xml");
            }
        }

        public void bind(Message message) {
            if (message == null) {
                Log.e(TAG, "⚠️ bind() reçu message NULL (RECEIVED)");
                return;
            }

            String text = message.getText() != null ? message.getText() : "";
            tvMessage.setText(text);

            if (tvSenderName != null) {
                String senderName = message.getSenderName();
                if (senderName != null && !senderName.isEmpty()) {
                    tvSenderName.setText(senderName);
                    tvSenderName.setVisibility(View.VISIBLE);
                } else {
                    tvSenderName.setVisibility(View.GONE);
                }
            }

            if (tvTime != null && message.getTimestamp() > 0) {
                tvTime.setText(formatTime(message.getTimestamp()));
            }

            Log.d(TAG, "✅ Message RECEIVED bindé: " + text);
        }
    }

    // ==========================
    // ✅ Formater timestamp
    // ==========================
    private static String formatTime(Long timestamp) {
        if (timestamp == null || timestamp == 0) {
            Log.w(TAG, "⚠️ Timestamp NULL ou 0");
            return "";
        }

        try {
            Date date = new Date(timestamp);
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
            return sdf.format(date);
        } catch (Exception e) {
            Log.e(TAG, "❌ Erreur formatTime: " + e.getMessage());
            return "";
        }
    }
}