package com.example.rahatassistant.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class NotifUtils {

    public static final String CHANNEL_ID = "rahat_assistant_channel";
    public static final int NOTIF_ID = 1001;

    public static void ensureChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID, "Rahat Assistant", NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }

    public static Notification build(Context ctx, String text) {
        return new NotificationCompat.Builder(ctx, CHANNEL_ID)
                .setContentTitle("Rahat Assistant")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_btn_speak_now)
                .setOngoing(true)
                .build();
    }

    public static void notify(Context ctx, String text) {
        NotificationManager nm = (NotificationManager) ctx.getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, build(ctx, text));
    }
}
