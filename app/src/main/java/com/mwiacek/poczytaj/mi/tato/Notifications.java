package com.mwiacek.poczytaj.mi.tato;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
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

    public static void setupNotifications(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(Channels.CZYTANIE_Z_INTERNETU.name(),
                    Channels.CZYTANIE_Z_INTERNETU.name(),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Czytanie plików z Internetu");
            Objects.requireNonNull(notificationManager(context)).createNotificationChannel(channel);

            channel = new NotificationChannel(Channels.ZAPIS_W_URZADZENIU.name(),
                    Channels.ZAPIS_W_URZADZENIU.name(),
                    NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Zapis plików w urządzeniu");
            Objects.requireNonNull(notificationManager(context)).createNotificationChannel(channel);
        }
    }

    public static NotificationCompat.Builder setupNotification(Channels channel, Context context, String text) {
        return new NotificationCompat.Builder(context, channel.name())
                .setSmallIcon(R.drawable.ic_launcher)
                .setContentTitle("Poczytaj Mi Tato")
                .setContentText(text);
    }

    public enum Channels {
        CZYTANIE_Z_INTERNETU,
        ZAPIS_W_URZADZENIU
    }
}
