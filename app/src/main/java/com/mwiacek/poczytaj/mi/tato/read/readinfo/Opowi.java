package com.mwiacek.poczytaj.mi.tato.read.readinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.mwiacek.poczytaj.mi.tato.Notifications;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

public class Opowi extends ReadInfo {

    private static void processOneEntry(String s, DBHelper mydb, Page.PageTyp typ) {
        if (!s.contains("<div class=\"list-box-cat\">Fantastyka</div>")) return;

        int indeks = Utils.Szukaj(s, "<div class=\"list-box-author\">", 0);
        int indeks2 = s.indexOf("</div>", indeks);
        String author = s.substring(indeks, indeks2);

        indeks = Utils.Szukaj(s, "<div class=\"list-box-title\"><a href=\"", 0);
        indeks = Utils.Szukaj(s, "\">", indeks);
        indeks2 = s.indexOf("</a>", indeks);
        String title = s.substring(indeks, indeks2);

        indeks = Utils.Szukaj(s, "<div class=\"list-box-title\"><a href=\"", 0);
        indeks2 = s.indexOf("\">", indeks);
        String url = "https://www.opowi.pl" + s.substring(indeks, indeks2);

        indeks = Utils.Szukaj(s, "<div class=\"list-box-date\">", 0);
        indeks2 = s.indexOf("</div>", indeks);
        String dt = s.substring(indeks, indeks2);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date(0);
        try {
            date = format.parse(dt);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Log.i("MY2", "'" + title + "'" + author + "'" + url + "'" + date);
        assert date != null;
        mydb.insertPage(typ, title, author, "", url, date);
    }

    public String getOpkoFromSinglePage(String result, File f) {
        int index = result.indexOf("<div id=\"content\" class=\"novel  \">");
        int index2 = result.indexOf("<div class=\"clear\"></div></div>", index);
        Utils.writeFile(f, result.substring(index, index2));
        return result.substring(index, index2);
    }

    public void getList(
            final Utils.RepositoryCallback<StringBuilder> callback,
            final Utils.RepositoryCallback<StringBuilder> callbackOnTheEnd,
            final Handler resultHandler,
            final DBHelper mydb, final Context context, Page.PageTyp typ,
            int pagesInPartReading) {
        try {
            String url = "";
            int index = 1;
            while (true) {
                Objects.requireNonNull(Notifications.notificationManager(context)).notify(1,
                        Notifications.setupNotification(Notifications.Channels.CZYTANIE_Z_INTERNETU, context,
                                "Czytanie listy - strona " + index).build());
                url = "https://www.opowi.pl/spis" +
                        (index == 1 ? "" : "?str=" + index);
                String result = Utils.getPageContent(url).toString();
                int indeks = result.indexOf("<h2>Wszystkie opowiadania</h2>");
                while (true) {
                    indeks = result.indexOf("<div class=\"list-box-title\">", indeks);
                    int indeks2 = Utils.Szukaj(result, "</div></div>", indeks);
                    if (indeks == -1 || indeks2 == -1) break;
                    processOneEntry(result.substring(indeks, indeks2), mydb, typ);
                    indeks = indeks2;
                }
                resultHandler.post(() -> callback.onComplete(new StringBuilder()));
                index++;
                if (index == 3) break;
            }
            Objects.requireNonNull(Notifications.notificationManager(context)).cancel(1);
            resultHandler.post(() -> callbackOnTheEnd.onComplete(new StringBuilder()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getList(
            final Utils.RepositoryCallback<StringBuilder> callback,
            final Utils.RepositoryCallback<StringBuilder> callbackOnTheEnd,
            final Handler resultHandler,
            final ThreadPoolExecutor executor,
            final DBHelper mydb, final Context context, Page.PageTyp typ,
            int pagesInPartReading) {
        executor.execute(() -> {
            getList(callback, callbackOnTheEnd, resultHandler, mydb, context, typ,
                    pagesInPartReading);
        });
    }
}
