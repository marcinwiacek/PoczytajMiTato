package com.mwiacek.poczytaj.mi.tato.search;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;

import com.mwiacek.poczytaj.mi.tato.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageCache {
    private static LruCache<String, Bitmap> mMemoryCache;

    public ImageCache(Context context) {
        mMemoryCache = new LruCache<>(((ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE))
                .getMemoryClass() * 1024 * 1024 / 8);
    }

    /**
     * Generates cache file name for thumbnail
     */
    private static String getDiskCacheFileName(Context context, String uniqueName) {
        return Utils.getDiskCacheFolder(context) + File.separator + uniqueName;
    }

    public void getImageFromCache(Context context, String name,
                                  Utils.ItemViewHolder holder,
                                  int position) {
        if (name == null || name.isEmpty()) return;
        String cacheName = name.replaceAll("[:/\\[\\]]+", "");

        if (mMemoryCache.get(cacheName) != null) {
            holder.thumbnailPicture.setImageBitmap(mMemoryCache.get(cacheName));
            return;
        }

        if ((new File(getDiskCacheFileName(context, cacheName)).exists())) {
            Bitmap bitmap = BitmapFactory.decodeFile(getDiskCacheFileName(
                    context,
                    cacheName));
            holder.thumbnailPicture.setImageBitmap(bitmap);
            mMemoryCache.put(cacheName, bitmap);
            return;
        }

        holder.thumbnailPicture.setImageBitmap(null);
        new DownloadThumbnailTask(position, holder).executeOnExecutor(
                AsyncTask.THREAD_POOL_EXECUTOR,
                name,
                cacheName,
                context,
                holder.titleText.getText().toString());
    }

    /**
     * Task for downloading book thumbnail
     */
    private static class DownloadThumbnailTask extends AsyncTask<Object, Void, String> {
        final int mPosition;
        final Utils.ItemViewHolder mViewHolder;

        DownloadThumbnailTask(int position, Utils.ItemViewHolder viewHolder) {
            mViewHolder = viewHolder;
            mPosition = position;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                URL url = new URL((String) params[0]);
                System.out.println(params[0] + " " + params[1] + " " + params[3]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(5000); // 5 seconds
                connection.connect();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());
                    if (bitmap == null) return null;
                    mMemoryCache.put((String) params[1], bitmap);
                    try {
                        FileOutputStream fos = new FileOutputStream(
                                getDiskCacheFileName((Context) params[2], (String) params[1])
                        );
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                        fos.close();
                    } catch (IOException ignored) {
                    }
                } finally {
                    connection.disconnect();
                }
                return (String) params[1];
            } catch (IOException ignore) {
            }
            return null;
        }

        protected void onPostExecute(String id) {
            if (id == null || mViewHolder.getAbsoluteAdapterPosition() != mPosition) return;
            mViewHolder.thumbnailPicture.setImageBitmap(mMemoryCache.get(id));
        }
    }
}
