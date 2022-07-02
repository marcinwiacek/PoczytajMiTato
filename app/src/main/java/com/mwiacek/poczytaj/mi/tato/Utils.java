package com.mwiacek.poczytaj.mi.tato;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ThreadPoolExecutor;

import javax.net.ssl.HttpsURLConnection;

public class Utils {
    static public int Szukaj(String s, String szukaj, int startIndeks) {
        int i = s.indexOf(szukaj, startIndeks);
        if (i != -1) i += szukaj.length();
        return i;
    }

    static public void writeFile(File f, String s) {
        FileOutputStream outputStream;
        try {
            f.createNewFile();
            outputStream = new FileOutputStream(f, false);
            outputStream.write(s.getBytes());
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static public String readFile(File f) {
        String r = "";
        try {
            FileInputStream fIn = new FileInputStream(f);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            String aDataRow = "";
            while ((aDataRow = myReader.readLine()) != null) {
                r += aDataRow;
            }
            myReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    static public void notifyResult(final StringBuilder result,
                                    final RepositoryCallback<StringBuilder> callback,
                                    final Handler resultHandler) {
        resultHandler.post(() -> callback.onComplete(result));
    }

    public static String getDiskCacheFolder(Context context) {
        return context.getExternalCacheDir() == null ?
                context.getCacheDir().getPath() : context.getExternalCacheDir().getPath();
    }

    static public void downloadFileWithDownloadManager(String url, String title, Context context) {
        if (url.length() == 0) {
            return;
        }

        if (url.contains(".htm") || url.contains("artrage.pl")) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse(url));
            context.startActivity(i);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
            if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                return;
            }
        }
        File path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS);

        DownloadManager downloadmanager = (DownloadManager) context.getSystemService(android.content.Context.DOWNLOAD_SERVICE);
        Uri uri = Uri.parse(url);

        File f = new File("" + uri);

        DownloadManager.Request request = new DownloadManager.Request(uri);
        request.setTitle(f.getName());
        request.setDescription(title);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationUri(Uri.parse("file://" + path + "/" + f.getName()));
        downloadmanager.enqueue(request);
    }

    static public void getPage(
            final String URL,
            final RepositoryCallback<StringBuilder> callback,
            final Handler resultHandler,
            final ThreadPoolExecutor executor
    ) {
        executor.execute(() -> {
            try {
                StringBuilder result = Utils.getPageContent(URL);
                notifyResult(result, callback, resultHandler);
            } catch (Exception e) {
//                    notifyResult(new StringBuilder(), callback, resultHandler);
            }
        });
    }

    @SuppressWarnings("deprecation")
    public static String stripHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(html).toString();
        }
    }

    public static String findBetween(String s, String start, String stop, int startIndex) {
        if (startIndex == -1) {
            return "";
        }
        int fromPosition = s.indexOf(start, startIndex);
        if (fromPosition == -1) {
            return "";
        }
        int toPosition = s.indexOf(stop, fromPosition + start.length());
        if (toPosition == -1) {
            return "";
        }
        return s.substring(fromPosition + start.length(), toPosition);
    }

    static public StringBuilder getPageContent(String address) throws Exception {
        StringBuilder content = new StringBuilder();
        if (address.isEmpty()) {
            throw new Exception("Empty URL");
        }
        URL url = new URL(address);

        HttpURLConnection connection = url.getProtocol().equals("https") ?
                (HttpsURLConnection) url.openConnection() :
                (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(5000); // 5 seconds
        connection.connect();
        if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
            return content;
        }
        try {
            InputStreamReader isr = new InputStreamReader(connection.getInputStream());
            BufferedReader in = new BufferedReader(isr);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                content.append(inputLine);
            }
        } finally {
            connection.disconnect();
        }
        return content;
    }

    public interface RepositoryCallback<T> {
        void onComplete(T result);
    }
}
