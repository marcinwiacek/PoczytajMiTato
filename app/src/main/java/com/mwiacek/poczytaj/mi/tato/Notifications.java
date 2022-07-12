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

    private static void setupOneChannel(Channels channelName, String description, Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(channelName.name(),
                    channelName.name(), NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription(description);
            Objects.requireNonNull(notificationManager(context)).createNotificationChannel(channel);
        }
    }

    public static void setupNotifications(Context context) {
        setupOneChannel(Channels.CZYTANIE_Z_INTERNETU, "Czytanie plików z Internetu", context);
        setupOneChannel(Channels.ZAPIS_W_URZADZENIU, "Zapis plików w urządzeniu", context);
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
