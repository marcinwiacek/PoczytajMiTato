package com.mwiacek.poczytaj.mi.tato;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.text.Html;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mwiacek.poczytaj.mi.tato.read.Page;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class Utils {
    public static void contactMe(Context context) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        String subject = "";
        try {
            subject = "Poczytaj mi tato " +
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName +
                    " / Android " +
                    Build.VERSION.RELEASE;
        } catch (Exception ignored) {
        }
        String[] extra = new String[]{"marcin@mwiacek.com"};
        emailIntent.putExtra(Intent.EXTRA_EMAIL, extra);
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.setType("message/rfc822");
        try {
            context.startActivity(emailIntent);
        } catch (Exception e) {
            AlertDialog alertDialog;
            alertDialog = new AlertDialog.Builder(context).create();
            alertDialog.setTitle("Informacja");
            alertDialog.setMessage("Błąd stworzenia maila");
            alertDialog.show();
        }
    }

    public static void writeFile(File f, String s) {
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

    public static String readFile(File f) {
        StringBuilder r = new StringBuilder();
        try {
            FileInputStream fIn = new FileInputStream(f);
            BufferedReader myReader = new BufferedReader(new InputStreamReader(fIn));
            String aDataRow = "";
            while ((aDataRow = myReader.readLine()) != null) {
                r.append(aDataRow);
            }
            myReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r.toString();
    }

    public static String getDiskCacheFolder(Context context) {
        return context.getExternalCacheDir() == null ?
                context.getCacheDir().getPath() : context.getExternalCacheDir().getPath();
    }

    public static void notifyResult(final StringBuilder result,
                                    final RepositoryCallback<StringBuilder> callback,
                                    final Handler resultHandler) {
        resultHandler.post(() -> callback.onComplete(result));
    }

    public static StringBuilder getPageContent(String address) throws Exception {
        StringBuilder content = new StringBuilder();
        if (address.isEmpty()) {
            throw new Exception("Empty URL");
        }
        URL url = new URL(address);
        HttpURLConnection connection = url.getProtocol().equals("https") ?
                (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
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

    public static void getPage(
            final String URL,
            final RepositoryCallback<StringBuilder> callback,
            final Handler resultHandler,
            final ThreadPoolExecutor executor) {
        executor.execute(() -> {
            try {
                StringBuilder result = Utils.getPageContent(URL);
                notifyResult(result, callback, resultHandler);
            } catch (Exception e) {
//                    notifyResult(new StringBuilder(), callback, resultHandler);
            }
        });
    }

    public static void downloadFileWithDownloadManager(String url, String title, Context context) {
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

    public static void addZipFile(String name, ZipOutputStream out, File f) throws IOException {
        byte[] data = new byte[1000];

        FileInputStream fi = new FileInputStream(f);
        BufferedInputStream origin = new BufferedInputStream(fi, 1000);
        ZipEntry entry = new ZipEntry(name);
        out.putNextEntry(entry);
        int count;
        while ((count = origin.read(data, 0, 1000)) != -1) {
            out.write(data, 0, count);
        }
        origin.close();
    }

    public static void addZipFile(String name, ZipOutputStream out, String content) throws IOException {
        ZipEntry entry = new ZipEntry(name);
        out.putNextEntry(entry);
        out.write(content.getBytes());
    }

    public static void createEPUB(Context context, String tabName, List<Page> list) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                ActivityCompat.requestPermissions((Activity) context,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
            if (ContextCompat.checkSelfPermission(context,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_DENIED) {
                return;
            }
        }
        NotificationCompat.Builder builder =
                Notifications.setupNotification(Notifications.Channels.ZAPIS_W_URZADZENIU, context,
                        "Tworzenie pliku EPUB");
        Objects.requireNonNull(Notifications.notificationManager(context)).notify(2, builder.build());
        try {
            int z = 0;
            String longFileName;
            String shortFileName;

            while (true) {
                longFileName = Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator +
                        tabName.replaceAll("[^A-Za-z0-9]", "") +
                        (z == 0 ? "" : z) + ".zip";
                File f = new File(longFileName);
                if (!f.exists()) break;
                z++;
            }

            shortFileName = tabName.replaceAll("[^A-Za-z0-9]", "") +
                    (z == 0 ? "" : z) + ".zip";
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(longFileName)));

            for (int j = 0; j < list.size(); j++) {
                File f = list.get(j).getCacheFileName(context);
                if (f.exists()) {
                    Utils.addZipFile("OEBPS\\" + j + ".html", out, f);
                }
            }

            Utils.addZipFile("mimetype", out, "application/epub+zip");
            Utils.addZipFile("META-INF\\container.xml",
                    out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
                            "<rootfiles>\n" +
                            "<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
                            "</rootfiles>\n" +
                            "</container>");
            Utils.addZipFile("OEBPS\\style.css", out, "body {text-align:justify}");

            Utils.addZipFile("OEBPS\\toc.ncx", out,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\"\n" +
                            "xmlns:py=\"http://genshi.edgewall.org/\"\n" +
                            "version=\"2005-1\"\n" +
                            "xml:lang=\"pl\">\n" +
                            "<head>\n" +
                            "<meta name=\"cover\" content=\"cover\"/>\n" +
                            "<meta name=\"dtb:uid\" content=\"urn:uuid:e5953946-ea06-4599-9a53-f5c652b89f5c\"/>\n" +
                            "<meta name=\"dtb:depth\" content=\"1\"/>\n" +
                            "<meta name=\"dtb:totalPageCount\" content=\"0\"/>\n" +
                            "<meta name=\"dtb:maxPageNumber\" content=\"0\"/>\n" +
                            "</head>\n" +
                            "<docTitle>\n" +
                            "<text>tytul</text>\n" +
                            "</docTitle>\n" +
                            "<navMap>\n" +
                            //$tocTocNCX.
                            "</navMap>\n" +
                            "</ncx>\n");

            Utils.addZipFile("OEBPS\\toc.xhtml", out,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
                            "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n" +
                            "xmlns:epub=\"http://www.idpf.org/2007/ops\"\n" +
                            "xml:lang=\"pl\" lang=\"pl\">\n" +
                            "<head>\n" +
                            "<title>title</title>\n" +
                            "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />\n" +
                            "</head>\n" +
                            "<body xml:lang=\"pl\" lang=\"pl\">\n" +
                            "<header>\n" +
                            "<h2>Spis treści</h2>\n" +
                            "</header>\n" +
                            "<nav epub:type=\"toc\">\n" +
                            "<ol>\n" +
                            //$tocTocXHTML.
                            "</ol>\n" +
                            "</nav>\n" +
                            "</body>\n" +
                            "</html>");

            Utils.addZipFile("OEBPS\\content.opf", out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<package xmlns=\"http://www.idpf.org/2007/opf\"\n" +
                    "xmlns:opf=\"http://www.idpf.org/2007/opf\"\n" +
                    "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                    "unique-identifier=\"bookid\"\n" +
                    "version=\"3.0\"\n" +
                    "xml:lang=\"pl\">\n" +
                    "<metadata>\n" +
                    "<dc:identifier id=\"bookid\">urn:uuid:e5953946-ea06-4599-9a53-f5c652b89f5c</dc:identifier>\n" +
                    "<dc:language>pl-PL</dc:language>\n" +
                    "<meta name=\"generator\" content=\"Skrypt z mwiacek.com\"/>\n" +
                    "<dc:title>title</dc:title>\n" +
                    "<dc:description>\n" +
                    "title\n" +
                    "</dc:description>\n" +
                    "<dc:creator id=\"creator-0\">A.zbiorowy+skrypt z mwiacek.com</dc:creator>\n" +
                    "<meta refines=\"#creator-0\" property=\"role\" scheme=\"marc:relators\">aut</meta>\n" +
                    "<meta refines=\"#creator-0\" property=\"file-as\">A.zbiorowy+skrypt z mwiacek.com</meta>\n" +
                    "<meta name=\"cover\" content=\"cover\"></meta>\n" +
                    "<meta property=\"dcterms:modified\"></meta>\n" +
                    "</metadata>\n" +
                    "<manifest>\n" +
                    "<item id=\"style_css\" media-type=\"text/css\" href=\"style.css\" />\n" +
                    "<item id=\"cover\" media-type=\"image/jpeg\" href=\"cover$set.jpg\" properties=\"cover-image\" />\n" +
                    "<item id=\"cover-page_xhtml\" media-type=\"application/xhtml+xml\" href=\"cover-page.xhtml\" />\n" +
                    "<item id=\"toc_xhtml\" media-type=\"application/xhtml+xml\" href=\"toc.xhtml\" properties=\"nav\" />\n" +
                    //        $tocContentOpf1.
                    "<item id=\"ncxtoc\" media-type=\"application/x-dtbncx+xml\" href=\"toc.ncx\" />\n" +
                    "</manifest>\n" +
                    "<spine toc=\"ncxtoc\">\n" +
                    "<itemref idref=\"cover-page_xhtml\" linear=\"no\"/>\n" +
                    "<itemref idref=\"toc_xhtml\"/>\n" +
                    //$tocContentOpf2.
                    "</spine>\n" +
                    "<guide>\n" +
                    "<reference href=\"cover-page.xhtml\" type=\"cover\" title=\"Strona okładki\"/>\n" +
                    "<reference href=\"toc.xhtml\" type=\"toc\" title=\"Spis treści\"/>\n" +
                    //$tocContentOpf3.
                    "</guide>\n" +
                    "</package>");

            Utils.addZipFile("OEBPS\\cover-page.xhtml", out,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<!DOCTYPE html>\n" +
                            "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n" +
                            "xmlns:epub=\"http://www.idpf.org/2007/ops\"\n" +
                            "xml:lang=\"pl\" lang=\"pl\">\n" +
                            "<head>\n" +
                            "<title>title</title>\n" +
                            "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />\n" +
                            "</head>\n" +
                            "<body xml:lang=\"pl\" lang=\"pl\">\n" +
                            "<div>\n" +
                            "<img src=\"cover$set.jpg\"/>\n" +
                            "</div>\n" +
                            "</body>\n" +
                            "</html>");

            out.close();

            Intent intent = new Intent();
            intent.setDataAndType(Uri.fromFile(new File(longFileName)),
                    MimeTypeMap.getSingleton().getMimeTypeFromExtension(".ZIP"));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            PendingIntent pendingIntent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                pendingIntent = PendingIntent.getActivity(context,
                        0, intent, PendingIntent.FLAG_IMMUTABLE);
            }

            builder.setContentText("Zapisano plik EPUB " + shortFileName).setContentIntent(pendingIntent);
            Objects.requireNonNull(Notifications.notificationManager(context)).notify(2, builder.build());
        } catch (Exception e) {
            builder.setContentText("Błąd zapisu pliku EPUB");
            Objects.requireNonNull(Notifications.notificationManager(context)).notify(2, builder.build());

            e.printStackTrace();
        }
    }

    @SuppressWarnings("deprecation")
    public static String stripHtml(String html) {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ?
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString() :
                Html.fromHtml(html).toString();
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

    public static int Szukaj(String s, String szukaj, int startIndeks) {
        int i = s.indexOf(szukaj, startIndeks);
        if (i != -1) i += szukaj.length();
        return i;
    }

    public interface RepositoryCallback<T> {
        void onComplete(T result);
    }

    public interface OnItemClicked {
        void onItemClick(int position);
    }

    public static class ItemViewHolder extends RecyclerView.ViewHolder {
        public TextView titleText;
        public TextView authorText;
        public ImageView thumbnailPicture;
        public TextView priceText;

        public ItemViewHolder(@NonNull View itemView, Utils.OnItemClicked onClick) {
            super(itemView);
            titleText = itemView.findViewById(R.id.BookName);
            authorText = itemView.findViewById(R.id.BookAuthor);
            priceText = itemView.findViewById(R.id.BookPrice);
            thumbnailPicture = itemView.findViewById(R.id.BookImage);

            itemView.setOnClickListener(v -> onClick.onItemClick(getAbsoluteAdapterPosition()));
        }
    }
}
