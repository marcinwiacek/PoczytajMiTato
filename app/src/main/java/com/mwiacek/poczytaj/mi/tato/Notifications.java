package com.mwiacek.poczytaj.mi.tato;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import java.util.Objects;

public class Notifications {
    public static NotificationManager notificationManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getSystemService(NotificationManager.class);
        }
        return null;
    }

    private static void setupOneChannel(Context context, Channels channelName, String description) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelName.name(),
                    description, NotificationManager.IMPORTANCE_LOW);
            channel.setDescription(description);
            Objects.requireNonNull(notificationManager(context)).createNotificationChannel(channel);
        }
    }

    public static void setupNotifications(Context context) {
        setupOneChannel(context, Channels.CZYTANIE_Z_INTERNETU, "Czytanie plików z internetu");
        setupOneChannel(context, Channels.ZAPIS_W_URZADZENIU, "Zapis plików w urządzeniu");
    }

    public static NotificationCompat.Builder setupNotification(Context context, Channels channel, String text, int tabNum) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.putExtra("tabNum", tabNum);
        /*Intent notificationIntent = new Intent(context, MainActivity.class)
                .setAction("com.app.action.notification")
                .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);*/
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(context, channel.name())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Poczytaj Mi Tato")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setContentText(text);
    }

    public enum Channels {
        CZYTANIE_Z_INTERNETU,
        ZAPIS_W_URZADZENIU
    }
}
