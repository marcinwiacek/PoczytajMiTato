package com.mwiacek.poczytaj.mi.tato;

import android.annotation.SuppressLint;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.Html;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HttpsURLConnection;

public class Utils {
    public final static String BEFORE_HIGHLIGHT = "<ins style='background-color:green'>";
    public final static String AFTER_HIGHLIGHT = "</ins>";
    public final static String EPUB_MIME_TYPE = "application/epub+zip";

    public static void dialog(Context context, String message, View view,
                              android.content.DialogInterface.OnClickListener OKListener,
                              android.content.DialogInterface.OnClickListener CancelListener) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("Poczytaj mi tato")
                .setMessage(message)
                .setPositiveButton("OK", OKListener);
        if (CancelListener != null) builder.setNegativeButton("Cancel", CancelListener);
        if (view != null) builder.setView(view);
        builder.show();
    }

    public static void contactMe(Context context) {
        Intent emailIntent = new Intent(Intent.ACTION_SEND);
        String subject = "";
        try {
            subject = "Poczytaj mi tato " +
                    context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName +
                    " / Android " + Build.VERSION.RELEASE;
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

    public static void writeTextFile(File f, String s) {
        try {
            if (f.createNewFile()) {
                FileOutputStream outputStream = new FileOutputStream(f, false);
                outputStream.write(s.getBytes());
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String readTextFile(File f) {
        StringBuilder r = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
            String line = "";
            while ((line = reader.readLine()) != null) {
                r.append(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r.toString();
    }

    public static String getDiskCacheFolder(Context context) {
        return context.getCacheDir().getPath();
    }

    public static StringBuilder getTextPageContent(
            String address, final RepositoryCallback<Void> errorCallback,
            final RepositoryCallback<Integer> updatecallback,
            final Handler resultHandler) throws Exception {
        StringBuilder content = new StringBuilder();
        if (address.isEmpty()) {
            throw new Exception("Empty URL");
        }
        URL url = new URL(address);
        HttpURLConnection connection = url.getProtocol().equals("https") ?
                (HttpsURLConnection) url.openConnection() : (HttpURLConnection) url.openConnection();
        connection.setReadTimeout(15000); // 15 seconds
        try {
            connection.connect();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                if (errorCallback != null) {
                    resultHandler.post(() -> errorCallback.onComplete(null));
                }
                return content;
            }
            InputStreamReader isr = new InputStreamReader(connection.getInputStream());
            BufferedReader in = new BufferedReader(isr);
            int byteReadNum;
            char[] buffer = new char[1000];
            if (updatecallback != null) {
                resultHandler.post(() -> updatecallback.onComplete(0));
            }
            while ((byteReadNum = in.read(buffer)) != -1) {
                content.append(buffer, 0, byteReadNum);
                if (updatecallback != null) {
                    resultHandler.post(() -> updatecallback.onComplete(content.length()));
                }
            }
            isr.close();
        } catch (IOException e) {
            if (errorCallback != null) {
                resultHandler.post(() -> errorCallback.onComplete(null));
            }
            return content;
        } finally {
            connection.disconnect();
        }
        return content;
    }

    public static void getTextPage(
            final String url,
            final RepositoryCallback<Void> errorCallback,
            final RepositoryCallback<Integer> updatecallback,
            final RepositoryCallback<StringBuilder> callback,
            final Handler resultHandler,
            final ThreadPoolExecutor executor) {
        executor.execute(() -> {
            try {
                StringBuilder result = Utils.getTextPageContent(url, errorCallback,
                        updatecallback, resultHandler);
                if (result.length() != 0 && callback != null) {
                    resultHandler.post(() -> callback.onComplete(result));
                }
            } catch (Exception ignore) {
            }
        });
    }

    public static void getBinaryPages(
            Context context,
            final ArrayList<String> urls,
            final RepositoryCallback<String> errorCallback,
            final RepositoryCallback<Integer> fileUpdateCallback,
            final RepositoryCallback<String> fileCompleteCallback,
            final RepositoryCallback<String> callbackAfterAllFiles,
            final Handler resultHandler,
            final ThreadPoolExecutor executor) {
        executor.execute(() -> {
            for (String url : urls) {
                int len = 0;
                HttpURLConnection connection = null;
                try {
                    URL url2 = new URL(url.replace(" ", "%20"));
                    connection = url2.getProtocol().equals("https") ?
                            (HttpsURLConnection) url2.openConnection() : (HttpURLConnection) url2.openConnection();
                    // connection.setConnectTimeout(5000);
                    connection.setReadTimeout(5000); // 5 seconds
                    connection.connect();
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                        return;
                    }
                    OutputStream outputStream =
                            new FileOutputStream(Page.getLongCacheFileName(context, url));
                    int byteReadNum;
                    byte[] buffer = new byte[5000];
                    while ((byteReadNum = connection.getInputStream().read(buffer)) != -1) {
                        outputStream.write(buffer, 0, byteReadNum);
                        len += byteReadNum;
                        if (fileUpdateCallback != null && len != 0) {
                            int finalLen = len;
                            resultHandler.post(() -> fileUpdateCallback.onComplete(finalLen));
                        }
                    }

                    outputStream.close();
                    if (fileCompleteCallback != null) {
                        resultHandler.post(() -> fileCompleteCallback.onComplete(url));
                    }
                } catch (IOException e) {
                    new File(Page.getLongCacheFileName(context, url)).delete();
                    if (errorCallback != null) {
                        resultHandler.post(() -> errorCallback.onComplete(url));
                    }
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
            if (callbackAfterAllFiles != null) {
                resultHandler.post(() -> callbackAfterAllFiles.onComplete(null));
            }
        });
    }

    public static void downloadFileWithDownloadManagerAfterGrantingPermission(String url, String title, Context context) {
        Uri uri = Uri.parse(url);
        File f = new File("" + uri);
        ((DownloadManager) context.getSystemService(android.content.Context.DOWNLOAD_SERVICE))
                .enqueue(new DownloadManager.Request(uri)
                        .setTitle(f.getName())
                        .setDescription(title)
                        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                        .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, f.getName()));
    }

    public static void addFileToZipFile(String name, ZipOutputStream out, InputStream f) throws IOException {
        byte[] data = new byte[1000];

        BufferedInputStream origin = new BufferedInputStream(f, 1000);
        ZipEntry entry = new ZipEntry(name);
        out.putNextEntry(entry);
        int count;
        while ((count = origin.read(data, 0, 1000)) != -1) {
            out.write(data, 0, count);
        }
        origin.close();
    }

    public static void addFileToZipFile(String name, ZipOutputStream out, String content) throws IOException {
        out.putNextEntry(new ZipEntry(name));
        out.write(content.getBytes());
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static void createEPUB(Context context, Uri file, List<Page> list,
                                  ArrayList<Page.PageTyp> types) {
        if (file == null) return;
        NotificationCompat.Builder builder = Notifications.setupNotification(context,
                Notifications.Channels.ZAPIS_W_URZADZENIU, "Tworzenie pliku EPUB", -1);
        Objects.requireNonNull(Notifications.notificationManager(context)).notify(2, builder.build());

        String tytul = "";
        Cursor fileCursor = context.getContentResolver().query(file,
                new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
        if (fileCursor != null && fileCursor.moveToFirst()) {
            int index = fileCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            if (index != -1) {
                tytul = fileCursor.getString(index);
            }
        }
        if (fileCursor != null) fileCursor.close();

        @SuppressLint("SimpleDateFormat") String d0 = (new SimpleDateFormat("ddMMyyyy HH:mm:ss"))
                .format(new Date());

        try {
            ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(
                    context.getContentResolver().openOutputStream(file)));

            String coverName = "cover5.jpg";
            Object[] arr = types.toArray();
            if (arr.length == 1) {
                if (arr[0] == Page.PageTyp.FANTASTYKA_BIBLIOTEKA) coverName = "cover1.jpg";
                if (arr[0] == Page.PageTyp.FANTASTYKA_POCZEKALNIA) coverName = "cover2.jpg";
                if (arr[0] == Page.PageTyp.FANTASTYKA_ARCHIWUM) coverName = "cover3.jpg";
            }
            Utils.addFileToZipFile("OEBPS/" + coverName, out, context.getAssets().open(coverName));

            StringBuilder tocTocNCX = new StringBuilder();
            StringBuilder tocTocXHTML = new StringBuilder();
            StringBuilder tocContentOpf1 = new StringBuilder();
            StringBuilder tocContentOpf2 = new StringBuilder();
            String tocContentOpf3 = "";
            for (int j = 0; j < list.size(); j++) {
                Page p = list.get(j);
                File f = p.getCacheFile(context);
                Date date = new Date();
                if (f.exists()) {
                    date.setTime(f.lastModified());
                    @SuppressLint("SimpleDateFormat") String d = (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")).format(date);

                    @SuppressLint("SimpleDateFormat") String d2 =
                            (new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")).format(p.dt);

                    String fileContent = readTextFile(f);

                    addFileToZipFile("OEBPS/" + j + ".xhtml", out,
                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
                                    "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n" +
                                    "xmlns:epub=\"http://www.idpf.org/2007/ops\"\n" +
                                    "xml:lang=\"pl\" lang=\"pl\">\n" +
                                    "<head>\n" +
                                    "<!-- typ " + p.typ.name() + " -->\n" +
                                    "<title>" + p.name + "</title>\n" +
                                    "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />\n" +
                                    "</head>\n" +
                                    "<body xml:lang=\"pl\" lang=\"pl\">\n" +
                                    "Autor: " + p.author + "<br/>\n" +
                                    "Info: " + p.tags + "<br/>\n" +
                                    "Czas: " + d2 + "<br/>\n" +
                                    "Pobrano: <a href=\"" + p.url + "\">" + d + "</a><br/>\n" +
                                    "<hr/>\n" +
                                    fileContent +
                                    "</body>\n</html>");
                    tocTocNCX.append("<navPoint id=\"index_").append(j).append("\" playOrder=\"")
                            .append(j).append("\">\n")
                            .append("<navLabel>\n")
                            .append("<text>").append(p.name).append("</text>\n")
                            .append("</navLabel>\n")
                            .append("<content src=\"").append(j).append(".xhtml\"/>\n")
                            .append("</navPoint>\n");

                    tocTocXHTML.append("<li>\n" + "<a href=\"").append(j).append(".xhtml\">")
                            .append(p.name).append("</a>\n").append("</li>\n");

                    tocContentOpf1.append("<item id=\"").append(j)
                            .append("_xhtml\" media-type=\"application/xhtml+xml\" href=\"")
                            .append(j).append(".xhtml\" />\n");

                    tocContentOpf2.append("<itemref idref=\"").append(j).append("_xhtml\"/>\n");

                    if (tocContentOpf3.isEmpty()) {
                        tocContentOpf3 = "<reference href=\"" + j + ".xhtml\" type=\"text\" title=\"Tekst\"/>\n";
                    }

                    for (String s : findImagesUrlInHTML(fileContent)) {
                        for (String extension : Page.SUPPORTED_IMAGE_EXTENSIONS) {
                            if (s.endsWith("." + extension)) {
                                Utils.addFileToZipFile("OEBPS/" + s, out,
                                        new FileInputStream(Page.getCacheDirectory(context) +
                                                File.separator + s));
                                tocContentOpf1.append("<item id=\"").append(s).append("\" media-type=\"")
                                        .append(MimeTypeMap.getSingleton().getMimeTypeFromExtension(
                                                "." + extension)).append("\" href=\"")
                                        .append(s).append("\" properties=\"image\" />\n");
                                break;
                            }
                        }
                    }
                }
            }

            addFileToZipFile("mimetype", out, EPUB_MIME_TYPE);

            addFileToZipFile("META-INF/container.xml",
                    out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
                            "<rootfiles>\n" +
                            "<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
                            "</rootfiles>\n" +
                            "</container>");

            addFileToZipFile("OEBPS/style.css", out, "body {text-align:justify}");

            addFileToZipFile("OEBPS/toc.ncx", out,
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
                            "<text>" + tytul + " (" + d0 + ")</text>\n" +
                            "</docTitle>\n" +
                            "<navMap>\n" +
                            tocTocNCX +
                            "</navMap>\n" +
                            "</ncx>\n");

            addFileToZipFile("OEBPS/toc.xhtml", out,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
                            "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n" +
                            "xmlns:epub=\"http://www.idpf.org/2007/ops\"\n" +
                            "xml:lang=\"pl\" lang=\"pl\">\n" +
                            "<head>\n" +
                            "<title>\" + tytul + \" (\" + d0 + \")</title>\n" +
                            "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />\n" +
                            "</head>\n" +
                            "<body xml:lang=\"pl\" lang=\"pl\">\n" +
                            "<header>\n" +
                            "<h2>Spis treści</h2>\n" +
                            "</header>\n" +
                            "<nav epub:type=\"toc\">\n" +
                            "<ol>\n" +
                            tocTocXHTML +
                            "</ol>\n" +
                            "</nav>\n" +
                            "</body>\n" +
                            "</html>");

            addFileToZipFile("OEBPS/content.opf", out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                    "<package xmlns=\"http://www.idpf.org/2007/opf\"\n" +
                    "xmlns:opf=\"http://www.idpf.org/2007/opf\"\n" +
                    "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                    "unique-identifier=\"bookid\"\n" +
                    "version=\"3.0\"\n" +
                    "xml:lang=\"pl\">\n" +
                    "<metadata>\n" +
                    "<dc:identifier id=\"bookid\">urn:uuid:e5953946-ea06-4599-9a53-f5c652b89f5c</dc:identifier>\n" +
                    "<dc:language>pl-PL</dc:language>\n" +
                    "<meta name=\"generator\" content=\"PoczytajMiTato z Google Play\"/>\n" +
                    "<dc:title>" + tytul + " (" + d0 + ")</dc:title>\n" +
                    "<dc:description>\n" +
                    tytul + " (" + d0 + ")" +
                    "</dc:description>\n" +
                    "<dc:creator id=\"creator-0\">A.zbiorowy+PoczytajMiTato z Google Play</dc:creator>\n" +
                    "<meta refines=\"#creator-0\" property=\"role\" scheme=\"marc:relators\">aut</meta>\n" +
                    "<meta refines=\"#creator-0\" property=\"file-as\">A.zbiorowy+Poczytaj Mi Tato z Google Play</meta>\n" +
                    "<meta name=\"cover\" content=\"cover\"></meta>\n" +
                    "<meta property=\"dcterms:modified\"></meta>\n" +
                    "</metadata>\n" +
                    "<manifest>\n" +
                    "<item id=\"style_css\" media-type=\"text/css\" href=\"style.css\" />\n" +
                    "<item id=\"cover\" media-type=\"image/jpeg\" href=\"" + coverName + "\" properties=\"cover-image\" />\n" +
                    "<item id=\"cover-page_xhtml\" media-type=\"application/xhtml+xml\" href=\"cover-page.xhtml\" />\n" +
                    "<item id=\"toc_xhtml\" media-type=\"application/xhtml+xml\" href=\"toc.xhtml\" properties=\"nav\" />\n" +
                    tocContentOpf1 +
                    "<item id=\"ncxtoc\" media-type=\"application/x-dtbncx+xml\" href=\"toc.ncx\" />\n" +
                    "</manifest>\n" +
                    "<spine toc=\"ncxtoc\">\n" +
                    "<itemref idref=\"cover-page_xhtml\" linear=\"no\"/>\n" +
                    "<itemref idref=\"toc_xhtml\"/>\n" +
                    tocContentOpf2 +
                    "</spine>\n" +
                    "<guide>\n" +
                    "<reference href=\"cover-page.xhtml\" type=\"cover\" title=\"Strona okładki\"/>\n" +
                    "<reference href=\"toc.xhtml\" type=\"toc\" title=\"Spis treści\"/>\n" +
                    tocContentOpf3 +
                    "</guide>\n" +
                    "</package>");

            addFileToZipFile("OEBPS/cover-page.xhtml", out,
                    "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                            "<!DOCTYPE html>\n" +
                            "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n" +
                            "xmlns:epub=\"http://www.idpf.org/2007/ops\"\n" +
                            "xml:lang=\"pl\" lang=\"pl\">\n" +
                            "<head>\n" +
                            "<title>" + tytul + " (" + d0 + ")</title>\n" +
                            "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />\n" +
                            "</head>\n" +
                            "<body xml:lang=\"pl\" lang=\"pl\">\n" +
                            "<div>\n" +
                            "<img src=\"" + coverName + "\"/>\n" +
                            "</div>\n" +
                            "</body>\n" +
                            "</html>");

            out.close();

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(file, EPUB_MIME_TYPE);
            intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (intent.resolveActivity(context.getPackageManager()) == null) {
                intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://search?q=" + EPUB_MIME_TYPE));
            }
            if (intent.resolveActivity(context.getPackageManager()) != null &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                builder.setContentText("Zapisano plik " + tytul)
                        .setContentIntent(PendingIntent.getActivity(context, 0, intent,
                                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ?
                                        PendingIntent.FLAG_MUTABLE : 0))
                        .setSubText("Kliknij, żeby otworzyć");
                Objects.requireNonNull(Notifications.notificationManager(context)).notify(2, builder.build());
            } else {
                Notifications.notificationManager(context).cancel(2);
                (new AlertDialog.Builder(context)
                        .setIcon(android.R.drawable.ic_dialog_info)
                        .setMessage("Zapisano plik " + tytul)
                        .setPositiveButton("OK", null))
                        .create().show();
            }
        } catch (Exception e) {
            Notifications.notificationManager(context).cancel(2);
            (new AlertDialog.Builder(context)
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setMessage("Błąd zapisu pliku EPUB")
                    .setPositiveButton("OK", null))
                    .create().show();
        }
    }

    /* Function is making few passes to avoid extreme memory usage.
       We try to be quite conservative and parse possible files - errors are returned only in
       some "big" problems
     */
    public static void importEPUB(Context context, Uri uri, DBHelper myDB) {
        if (uri == null) return;
        try {
            HashSet<String> imageNames = new HashSet<>();
            HashSet<String> imagesToRead = new HashSet<>();
            ZipInputStream zipfile = new ZipInputStream(context.getContentResolver().openInputStream(
                    Uri.parse(uri.toString())));
            ZipEntry entry;
            // pass 1 - get all image names in file and search for mimetype
            String mimetype = "";
            while ((entry = zipfile.getNextEntry()) != null) {
                if (entry.getName().equals("mimetype")) {
                    StringBuilder sss = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipfile));
                    String str;
                    while ((str = reader.readLine()) != null) {
                        sss.append(str);
                    }
                    mimetype = sss.toString();
                    if (!sss.toString().equals(EPUB_MIME_TYPE)) {
                        break;
                    }
                }
                for (String extension : Page.SUPPORTED_IMAGE_EXTENSIONS) {
                    if (entry.getName().startsWith("OEBPS/") &&
                            entry.getName().endsWith("." + extension)) {
                        if (imageNames.contains(
                                entry.getName().replace("OEBPS/", ""))) {
                            dialog(context, "Plik EPUB ma zły format (podwójne pliki)", null,
                                    (dialog, which) -> {
                                    }, null);
                            zipfile.close();
                            return;
                        }
                        imageNames.add(entry.getName().replace("OEBPS/", ""));
                    }
                }
            }
            zipfile.close();
            if (!mimetype.equals(EPUB_MIME_TYPE)) {
                dialog(context, "Plik EPUB ma zły format (błąd mimetype)", null,
                        (dialog, which) -> {
                        }, null);
                return;
            }
            int recognizedButIgnored = 0;
            int recognizedAndImported = 0;
            // pass 2 - parse text files
            zipfile = new ZipInputStream(context.getContentResolver().openInputStream(
                    Uri.parse(uri.toString())));
            while ((entry = zipfile.getNextEntry()) != null) {
                /* These two xhtml files are allowed */
                if (entry.getName().equals("OEBPS/cover-page.xhtml") ||
                        entry.getName().equals("OEBPS/toc.xhtml")) {
                    continue;
                }
                if (entry.getName().startsWith("OEBPS/") && entry.getName().endsWith(".xhtml")) {
                    StringBuilder sss = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(zipfile));
                    String str;
                    while ((str = reader.readLine()) != null) {
                        sss.append(str);
                    }
                    Page.PageTyp typ = null;
                    for (Page.PageTyp pt : Page.PageTyp.values()) {
                        if (sss.toString().contains("<!-- typ " + pt.name() + " -->")) {
                            typ = pt;
                            break;
                        }
                    }
                    if (typ == null) continue;
                    String tytul = findBetween(sss.toString(), "<title>", "</title>", 0);
                    String author = findBetween(sss.toString(), "Autor: ", "<br/>", 0);
                    String tags = findBetween(sss.toString(), "Info: ", "<br/>", 0);
                    String url = findBetween(sss.toString(), "Pobrano: <a href=\"", "\"", 0);
                    //System.out.println("'" + tytul + "'" + author + "'" + tags + "'" + url + "'" + typ.ordinal());
                    if (tytul.isEmpty() || author.isEmpty() || url.isEmpty()) continue;
                    String created = findBetween(sss.toString(), "Czas: ", "<br/>", 0);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat format =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    Date date;
                    try {
                        date = format.parse(created);
                    } catch (ParseException e) {
                        date = null;
                    }
                    if (date == null) continue;
                    String modified = findBetween(sss.toString(), url + "\">", "</a>", 0);
                    @SuppressLint("SimpleDateFormat") SimpleDateFormat format2 =
                            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                    Date date2;
                    try {
                        date2 = format2.parse(modified);
                    } catch (ParseException e) {
                        modified = "";
                    }
                    if (modified.isEmpty()) continue;
                    boolean correctImage = true;
                    for (String s : findImagesUrlInHTML(sss.toString())) {
                        correctImage = false;
                        for (String extension : Page.SUPPORTED_IMAGE_EXTENSIONS) {
                            if (s.endsWith("." + extension) &&
                                    imageNames.contains(s) && !imagesToRead.contains(s)) {
                                correctImage = true;
                                break;
                            }
                        }
                        if (!correctImage) break;
                    }
                    if (!correctImage) continue;
                    Page p = myDB.getPage(url);
                    boolean doimport = false;
                    if (p == null) {
                        doimport = true;
                        recognizedAndImported++;
                    } else {
                        File f = p.getCacheFile(context);
                        Date date3 = new Date();
                        date3.setTime(f.lastModified());
                        if (!date3.after(date)) {
                            doimport = true;
                            recognizedAndImported++;
                        } else {
                            recognizedButIgnored++;
                        }
                    }
                    if (doimport) {
                        if (p != null) {
                            File f = p.getCacheFile(context);
                            f.delete();
                            String fileContent = Utils.readTextFile(f);
                            for (String s : findImagesUrlInHTML(fileContent)) {
                                f = new File(Page.getCacheDirectory(context) +
                                        File.separator + s);
                                f.delete();
                            }
                        }
                        myDB.insertOrUpdatePage(typ, tytul, author, tags, url, date);
                        p = myDB.getPage(url);
                        writeTextFile(p.getCacheFile(context),
                                findBetween(sss.toString(), "<hr/>", "</body>", 0));
                        imagesToRead.addAll(findImagesUrlInHTML(sss.toString()));
                    }
                }
            }
            zipfile.close();
            // pass 3 - save binary files
            zipfile = new ZipInputStream(context.getContentResolver().openInputStream(
                    Uri.parse(uri.toString())));
            while ((entry = zipfile.getNextEntry()) != null) {
                if (!imagesToRead.contains(entry.getName().replace("OEBPS/", ""))) {
                    continue;
                }
                OutputStream outputStream = new FileOutputStream(Page.getLongCacheFileName(context,
                        entry.getName().replace("OEBPS/", "")));
                int byteRead;
                while ((byteRead = zipfile.read()) != -1) {
                    outputStream.write(byteRead);
                }
                outputStream.close();
            }
            zipfile.close();
            dialog(context, recognizedButIgnored +
                            " tekstów rozpoznano i zignorowano (w pliku nie ma nowych wersji)\n\n"
                            + recognizedAndImported + " tekstów zaimportowano", null,
                    (dialog, which) -> {
                    }, null);
        } catch (IOException e) {
            dialog(context, "Problem z czytaniem pliku EPUB", null,
                    (dialog, which) -> {
                    }, null);
        }
    }

    @SuppressWarnings("deprecation")
    public static String stripHtml(String html) {
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N ?
                Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString() :
                Html.fromHtml(html).toString();
    }

    public static ArrayList<String> findImagesUrlInHTML(String s) {
        ArrayList<String> urls = new ArrayList<>();
        int index = 0;
        while (true) {
            index = Utils.Szukaj(s, "<img", index);
            if (index == -1) break;
            index = Utils.Szukaj(s, "src=\"", index);
            if (index == -1) break;
            int index2 = Utils.Szukaj(s, "\"", index);
            urls.add(s.substring(index, index2 - 1));
            index = index2;
        }
        return urls;
    }

    public static String findBetween(String s, String start, String stop, int startIndex) {
        if (startIndex == -1) return "";
        int fromPosition = s.indexOf(start, startIndex);
        if (fromPosition == -1) return "";
        int toPosition = s.indexOf(stop, fromPosition + start.length());
        if (toPosition == -1) return "";
        return s.substring(fromPosition + start.length(), toPosition);
    }

    public static int Szukaj(String s, String szukaj, int startIndeks) {
        int i = s.indexOf(szukaj, startIndeks);
        if (i != -1) i += szukaj.length();
        return i;
    }

    public static String findText(String s, String s2) {
        return s.replaceAll("(?![^<]+>)((?i:\\Q" + s2
                        .replace("\\E", "\\E\\\\E\\Q")
                        .replace("a", "\\E[aąĄ]\\Q")
                        .replace("c", "\\E[cćĆ]\\Q")
                        .replace("e", "\\E[eęĘ]\\Q")
                        .replace("l", "\\E[lłŁ]\\Q")
                        .replace("n", "\\E[nńŃ]\\Q")
                        .replace("o", "\\E[oóÓ]\\Q")
                        .replace("s", "\\E[sśŚ]\\Q")
                        .replace("z", "\\E[zźżŻŹ]\\Q")
                        + "\\E))",
                BEFORE_HIGHLIGHT + "$1" + AFTER_HIGHLIGHT);
    }

    public static boolean containsText(String s, String s2) {
        return (findText(s, s2).length() != s.length());
    }

    public interface RepositoryCallback<T> {
        void onComplete(T result);
    }

    public interface OnItemClicked {
        void onItemClick(int position);
    }

    public interface OnLongItemClicked {
        boolean onLongItemClick(int position);
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

    public static class InfoTaskListRecyclerViewHolder extends RecyclerView.ViewHolder {
        public TextView description;

        public InfoTaskListRecyclerViewHolder(View view) {
            super(view);

            description = view.findViewById(R.id.Description);
        }
    }

}
