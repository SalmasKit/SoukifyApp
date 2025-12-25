package com.example.soukify.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import androidx.core.app.NotificationCompat;
import com.example.soukify.MainActivity;
import com.example.soukify.R;

public class NotificationHelper {

    public static final String CHANNEL_ID_DEFAULT = "default_channel_id";
    public static final String CHANNEL_NAME_DEFAULT = "General Notifications";
    public static final String CHANNEL_ID_PROMOTIONS = "promotions_channel_id";
    public static final String CHANNEL_NAME_PROMOTIONS = "Promotions";
    public static final String CHANNEL_ID_MESSAGES = "messages_channel_id";
    public static final String CHANNEL_NAME_MESSAGES = "Messages";

    private final Context context;

    public NotificationHelper(Context context) {
        this.context = context;
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            if (notificationManager == null) return;

            // Default Channel
            NotificationChannel defaultChannel = new NotificationChannel(
                    CHANNEL_ID_DEFAULT,
                    CHANNEL_NAME_DEFAULT,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            defaultChannel.setDescription("General notifications for Soukify");
            notificationManager.createNotificationChannel(defaultChannel);

            // Promotions Channel
            NotificationChannel promotionsChannel = new NotificationChannel(
                    CHANNEL_ID_PROMOTIONS,
                    CHANNEL_NAME_PROMOTIONS,
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            promotionsChannel.setDescription("Notifications about sales and promotions");
            notificationManager.createNotificationChannel(promotionsChannel);

            // Messages Channel
            NotificationChannel messagesChannel = new NotificationChannel(
                    CHANNEL_ID_MESSAGES,
                    CHANNEL_NAME_MESSAGES,
                    NotificationManager.IMPORTANCE_HIGH
            );
            messagesChannel.setDescription("Notifications for new messages");
            messagesChannel.setShowBadge(true);
            notificationManager.createNotificationChannel(messagesChannel);
        }
    }

    public void showNotification(String title, String body, String type) {
        String channelId = CHANNEL_ID_DEFAULT;
        if ("promotion".equalsIgnoreCase(type)) {
            channelId = CHANNEL_ID_PROMOTIONS;
        } else if ("message".equalsIgnoreCase(type)) {
            channelId = CHANNEL_ID_MESSAGES;
        }

        Intent intent = new Intent(context, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE
        );

        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(context, channelId)
                        .setSmallIcon(R.mipmap.ic_launcher) // Ensure this resource exists or use default
                        .setContentTitle(title)
                        .setContentText(body)
                        .setAutoCancel(true)
                        .setSound(defaultSoundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            // ID can be unique to not overwrite. Using text hash for minimal collision on similar content
            int notificationId = (title + body).hashCode();
            notificationManager.notify(notificationId, notificationBuilder.build());
        }
    }
}
