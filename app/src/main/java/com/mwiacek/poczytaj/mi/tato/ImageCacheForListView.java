package com.mwiacek.poczytaj.mi.tato;

import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.LruCache;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

class ImageCacheForListView {
    private static LruCache<String, Bitmap> mMemoryCache;

    ImageCacheForListView(Context context) {
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        int memClassBytes = am.getMemoryClass() * 1024 * 1024;
        int cacheSize = memClassBytes / 8;
        mMemoryCache = new LruCache<>(cacheSize);
    }

    public static String getDiskCacheFolder(Context context) {
        return context.getExternalCacheDir() == null ?
                context.getCacheDir().getPath() : context.getExternalCacheDir().getPath();
    }

    /**
     * Generates cache file name for thumbnail
     */
    private static String getDiskCacheFileName(Context context, String uniqueName) {
        return getDiskCacheFolder(context) + File.separator + uniqueName;
    }


    void getImageFromCache(Context context, String name, ViewHolder holder,
                           int position) {
        String cacheName = name.replaceAll("[:/\\[\\]]+", "");

        // Check if we have bitmap in memory cache
        if (mMemoryCache.get(cacheName) != null) {
            holder.thumbnailPicture.setImageBitmap(
                    mMemoryCache.get(cacheName));
            return;
        }

        // Check if we have bitmap in disk cache
        File file = new File(getDiskCacheFileName(context, cacheName));
        if (file.exists()) {
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
                context);
    }

    public static class ViewHolder {
        public int position;

        public TextView titleText;
        public TextView authorText;
        public ImageView thumbnailPicture;
        public TextView priceText;
    }

    /**
     * Task for downloading book thumbnail
     */
    private class DownloadThumbnailTask extends AsyncTask<Object, Void, String> {
        final int mPosition;
        final ViewHolder mViewHolder;

        DownloadThumbnailTask(int position, ViewHolder viewHolder) {
            mViewHolder = viewHolder;
            mPosition = position;
        }

        @Override
        protected String doInBackground(Object... params) {
            try {
                URL url = new URL((String) params[0]);

                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(5000); // 5 seconds
                connection.connect();
                try {
                    Bitmap bitmap = BitmapFactory.decodeStream(connection.getInputStream());

                    mMemoryCache.put((String) params[1], bitmap);

                    try {
                        String s = getDiskCacheFileName((Context) params[2], (String) params[1]);

                        FileOutputStream fos = new FileOutputStream(s);
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
            if (id == null) {
                return;
            }
            if (mViewHolder.position == mPosition) {
                mViewHolder.thumbnailPicture.setImageBitmap(mMemoryCache.get(id));
            }
        }
    }
}
