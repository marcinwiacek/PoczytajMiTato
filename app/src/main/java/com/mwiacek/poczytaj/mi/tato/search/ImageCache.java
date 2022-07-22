package com.mwiacek.poczytaj.mi.tato.search;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.LruCache;

import com.mwiacek.poczytaj.mi.tato.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class ImageCache {
    private final static Executor executor = Executors.newSingleThreadExecutor();
    private static LruCache<String, Bitmap> mMemoryCache;

    public ImageCache(Context context) {
        mMemoryCache = new LruCache<>(((ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryClass() * 1024 * 1024 / 8);
    }

    public void getImageFromCache(Context context, String name, Utils.ItemViewHolder holder,
                                  int position) {
        if (name == null || name.isEmpty()) return;

        String cacheName = name.replaceAll("[:/\\[\\]]+", "");
        if (mMemoryCache.get(cacheName) != null) {
            holder.thumbnailPicture.setImageBitmap(mMemoryCache.get(cacheName));
            return;
        }

        String cacheNameOnDisk = Utils.getDiskCacheFolder(context) + File.separator + name;
        if ((new File(cacheNameOnDisk).exists())) {
            Bitmap bitmap = BitmapFactory.decodeFile(cacheNameOnDisk);
            holder.thumbnailPicture.setImageBitmap(bitmap);
            mMemoryCache.put(cacheName, bitmap);
            return;
        }

        holder.thumbnailPicture.setImageBitmap(null);
        if (holder.getAbsoluteAdapterPosition() != position) return;
        executor.execute(() -> {
            try {
                URL url = new URL(name);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(5000); // 5 seconds
                connection.connect();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                    if (bitmap == null) return;
                    mMemoryCache.put(cacheName, bitmap);
                    try {
                        FileOutputStream fos = new FileOutputStream(cacheNameOnDisk);
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.close();
                    } catch (IOException ignored) {
                    }
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            holder.thumbnailPicture.setImageBitmap(bitmap);
                        }
                    }.sendEmptyMessage(1);
                } finally {
                    connection.disconnect();
                }
            } catch (IOException ignore) {
            }
        });
    }
}
