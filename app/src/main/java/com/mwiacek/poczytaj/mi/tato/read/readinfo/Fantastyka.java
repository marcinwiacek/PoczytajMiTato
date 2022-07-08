package com.mwiacek.poczytaj.mi.tato.read.readinfo;

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
import java.util.concurrent.ThreadPoolExecutor;

public class Fantastyka extends ReadInfo {

    private static void processOneEntry(String s, DBHelper mydb, Page.PagesTyp typ) {
        int indeks = Utils.Szukaj(s, "<div class=\"autor\">", 0);
        indeks = Utils.Szukaj(s, ">", indeks);
        int indeks2 = s.indexOf(":</a>", indeks);
        String author = s.substring(indeks, indeks2);

        indeks = Utils.Szukaj(s, "<div class=\"teksty\"><a href=\"", indeks2);
        indeks2 = s.indexOf("\"", indeks);
        String url = "https://www.fantastyka.pl" + s.substring(indeks, indeks2);

        indeks = Utils.Szukaj(s, "class=\"tytul\">", indeks);
        indeks2 = s.indexOf("</a>", indeks);
        String tytul = s.substring(indeks, indeks2).replace("#", "&#35;");
        indeks = indeks2;
        String comments = "";
        indeks = Utils.Szukaj(s, "<div>", indeks);
        if (Utils.Szukaj(s, "<a class=\"konkurs\" href=\"", indeks) != -1) {
            indeks = Utils.Szukaj(s, "<a class=\"konkurs\" href=\"", indeks);
            indeks = s.indexOf(">", indeks);
            indeks2 = s.indexOf("</a>", indeks);
            comments += s.substring(indeks + 1, indeks2);
            indeks = indeks2;
        }
        indeks = Utils.Szukaj(s, "   ", indeks);
        if (indeks != -1) {
            indeks2 = s.indexOf(" | ", indeks);
            if (!comments.isEmpty()) comments += ", ";
            comments += s.substring(indeks, indeks2).trim();
        }

        int i = 0;
        while (s.indexOf(" | ", i + 1) != -1) {
            i = Utils.Szukaj(s, " | ", i + 1);
        }
        indeks = i;
        indeks2 = s.indexOf("  ", indeks);
        String dt = s.substring(indeks, indeks2);
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy', g. 'HH:mm");
        Date date = new Date(0);
        try {
            date = format.parse(dt);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        Log.i("MY2", "'" + tytul + "'" + author + "'" + comments + "'" + url + "'" + date);
        mydb.insertPage(typ, tytul, author, comments, url, date);
    }

    public String getOpkoFromSinglePage(String result, File f) {
        int index = result.indexOf("<section class=\"opko no-headline\">");
        int index2 = result.indexOf("<span class=\"koniec\">Koniec</span>", index);
        Utils.writeFile(f, result.substring(index, index2));
        return result.substring(index, index2);
    }

    public void getList(
            final Utils.RepositoryCallback<StringBuilder> callback,
            final Utils.RepositoryCallback<StringBuilder> callbackOnTheEnd,
            final Handler resultHandler,
            final DBHelper mydb, final Context context, Page.PagesTyp typ) {

        try {
            String url = "";
            int index = 1;
            while (true) {
                Notifications.notificationManager(context).notify(1,
                        Notifications.setupNotification(Notifications.Channels.CZYTANIE, context,
                                "Czytanie listy - strona " + index).build());

                if (typ == Page.PagesTyp.FANTASTYKA_POCZEKALNIA) {
                    url = "https://www.fantastyka.pl/opowiadania/wszystkie" +
                            (index == 1 ? "" : "/w/w/w/0/d/" + index);
                } else if (typ == Page.PagesTyp.FANTASTYKA_BIBLIOTEKA) {
                    url = "https://www.fantastyka.pl/opowiadania/biblioteka" +
                            (index == 1 ? "" : "/w/w/w/0/d/" + index);
                } else if (typ == Page.PagesTyp.FANTASTYKA_ARCHIWUM) {
                    url = "https://www.fantastyka.pl/opowiadania/archiwum/d" +
                            (index == 1 ? "" : "/" + index);
                }

                String result = Utils.getPageContent(url).toString();
                int indeks = result.indexOf("<article style=\"margin-top: 4px;\">");
                while (true) {
                    indeks = result.indexOf("<div class=\"lista\">", indeks);
                    int indeks2 = result.indexOf("<div class=\"clear linia\"></div>", indeks);
                    if (indeks == -1 || indeks2 == -1) break;
                    processOneEntry(result.substring(indeks, indeks2), mydb, typ);
                    indeks = indeks2;
                }
                if (callback != null)
                    Utils.notifyResult(new StringBuilder(), callback, resultHandler);
                index++;
                if (index == 3) break;
            }
            Notifications.notificationManager(context).cancel(1);
            if (callbackOnTheEnd != null)
                Utils.notifyResult(new StringBuilder(), callbackOnTheEnd, resultHandler);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getList(
            final Utils.RepositoryCallback<StringBuilder> callback,
            final Utils.RepositoryCallback<StringBuilder> callbackOnTheEnd,
            final Handler resultHandler,
            final ThreadPoolExecutor executor,
            final DBHelper mydb, final Context context, Page.PagesTyp typ) {
        executor.execute(() -> {
            getList(callback, callbackOnTheEnd, resultHandler, mydb, context, typ);
        });
    }
}
