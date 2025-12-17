package com.example.soukify.ui.chat;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.example.soukify.R;
import com.example.soukify.data.models.Message;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MessagesAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_SENT = 1;
    private static final int TYPE_RECEIVED = 2;

    private final List<Message> messagesList;
    private final String currentUserId;

    public MessagesAdapter(List<Message> messagesList) {
        this.messagesList = messagesList;
        String uid = FirebaseAuth.getInstance().getUid();
        this.currentUserId = uid != null ? uid : "";
    }

    @Override
    public int getItemViewType(int position) {
        Message message = messagesList.get(position);
        if (message.getSenderId() != null && message.getSenderId().equals(currentUserId)) {
            return TYPE_SENT;
        } else {
            return TYPE_RECEIVED;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_SENT) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_sent, parent, false);
            return new SentViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_received, parent, false);
            return new ReceivedViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Message message = messagesList.get(position);

        long timestamp = message.getTimestamp() > 0 ? message.getTimestamp() : System.currentTimeMillis();
        String time = new SimpleDateFormat("HH:mm", Locale.getDefault())
                .format(new Date(timestamp));

        String text = message.getText() != null ? message.getText() : "";
        String senderName = message.getSenderName() != null ? message.getSenderName() : "Utilisateur";

        if (holder instanceof SentViewHolder) {
            SentViewHolder sentHolder = (SentViewHolder) holder;
            sentHolder.tvMessage.setText(text);
            sentHolder.tvTime.setText(time);
        } else if (holder instanceof ReceivedViewHolder) {
            ReceivedViewHolder receivedHolder = (ReceivedViewHolder) holder;
            receivedHolder.tvMessage.setText(text);
            receivedHolder.tvTime.setText(time);
            receivedHolder.tvSenderName.setText(senderName);
        }
    }

    @Override
    public int getItemCount() {
        return messagesList.size();
    }

    static class SentViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime;

        public SentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
        }
    }

    static class ReceivedViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage, tvTime, tvSenderName;

        public ReceivedViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvSenderName = itemView.findViewById(R.id.tvSenderName);
        }
    }
}
