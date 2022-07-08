package com.mwiacek.poczytaj.mi.tato;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

public class Notifications {
    public static NotificationManager notificationManager(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return context.getSystemService(NotificationManager.class);
        }
        return null;
    }

    public static void setupNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Channels.CZYTANIE.name(),
                    Channels.CZYTANIE.name(), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Czytanie plików z Internetu");
            notificationManager(context).createNotificationChannel(channel);

            channel = new NotificationChannel(Channels.ZAPIS.name(),
                    Channels.ZAPIS.name(), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Zapis plików w urządzeniu");
            notificationManager(context).createNotificationChannel(channel);
        }
    }

    public static NotificationCompat.Builder setupNotification(Channels channel, Context context, String text) {
        return new NotificationCompat.Builder(context, channel.name())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Poczytaj Mi Tato")
                .setContentText(text);
    }

    public enum Channels {
        CZYTANIE,
        ZAPIS
    }
}
