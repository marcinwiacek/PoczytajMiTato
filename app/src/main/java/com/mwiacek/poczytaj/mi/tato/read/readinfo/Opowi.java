package com.mwiacek.poczytaj.mi.tato.read.readinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;

import com.mwiacek.poczytaj.mi.tato.Notifications;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

public class Opowi extends ReadInfo {
    private static boolean processOneListEntry(String s, DBHelper mydb, Page.PageTyp typ) {

        String author = Utils.findBetween(s, "<div class=\"list-box-author\">", "</div>", 0);

        int indeks = Utils.Szukaj(s, "<div class=\"list-box-title\"><a href=\"", 0);
        String title = Utils.findBetween(s, "\">", "</a>", indeks);

        String url = "https://www.opowi.pl" +
                Utils.findBetween(s, "<div class=\"list-box-title\"><a href=\"", "\">", 0);

        String dt = Utils.findBetween(s, "<div class=\"list-box-date\">", "</div>", 0);
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
        Date date = new Date(0);
        try {
            date = format.parse(dt);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        // Log.i("MY2", "'" + title + "'" + author + "'" + url + "'" + date);
        assert date != null;
        return mydb.insertOrUpdatePage(typ, title, author, "", url, date);
    }

    public void processTextFromSinglePage(
            Context context, Page p,
            final Handler resultHandler,
            final ThreadPoolExecutor executor,
            final Utils.RepositoryCallback<String[]> callbackAfterMainFileWithResourceList,
            final Utils.RepositoryCallback<String> callbackAfterEveryImage,
            final Utils.RepositoryCallback<String> completeCallback) {
        Utils.getTextPage(p.url, result -> {
            int index = result.indexOf("<div id=\"content\" class=\"novel  \">");
            int index2 = result.indexOf("<div class=\"clear\"></div></div>", index);
            String mainPageText = result.substring(index, index2);
            Utils.writeTextFile(p.getCacheFile(context), mainPageText);
            if (completeCallback != null) {
                resultHandler.post(() -> completeCallback.onComplete(mainPageText));
            }
        }, resultHandler, executor);
    }

    public void getList(final Context context,
                        final Handler resultHandler,
                        final DBHelper mydb, Page.PageTyp typ,
                        String tabName, int tabNum, int pageStart, int pageStop,
                        final Utils.RepositoryCallback<Page.PageTyp> callbackOnUpdatedPage) {
        try {
            String url = "";
            int index = pageStart;
            boolean haveNewEntry = false;
            while (true) {
                Objects.requireNonNull(Notifications.notificationManager(context)).notify(typ.ordinal(),
                        Notifications.setupNotification(context, Notifications.Channels.CZYTANIE_Z_INTERNETU,
                                "Czytanie w zakładce " + tabName + " - strona " + index, tabNum).build());
                url = "https://www.opowi.pl/opowiadania-fantastyka/" +
                        (index == 1 ? "" : "?str=" + index);
                String result = Utils.getTextPageContent(url).toString();
                int indeks = result.indexOf("<h2>Opowiadania z kategorii: Fantastyka</h2>");
                boolean haveNewEntryOnThisPage = false;
                while (true) {
                    indeks = result.indexOf("<div class=\"list-box-title\">", indeks);
                    int indeks2 = Utils.Szukaj(result, "</div></div>", indeks);
                    if (indeks == -1 || indeks2 == -1) break;
                    if (processOneListEntry(result.substring(indeks, indeks2), mydb, typ)) {
                        haveNewEntry = true;
                        haveNewEntryOnThisPage = true;
                    }
                    indeks = indeks2;
                }
                if (callbackOnUpdatedPage != null && haveNewEntryOnThisPage) {
                    resultHandler.post(() -> callbackOnUpdatedPage.onComplete(typ));
                }
                if (!result.contains("rel=\"next\">Dalej &raquo;</a>")) {
                    mydb.setLastIndexPageRead(typ, -1);
                    break;
                }
                index++;
                if (index == pageStop) {
                    mydb.setLastIndexPageRead(typ, pageStop);
                    break;
                }
            }
            if (haveNewEntry && pageStart == 1) {
                Objects.requireNonNull(Notifications.notificationManager(context)).notify(
                        typ.ordinal(), Notifications.setupNotification(context,
                                Notifications.Channels.CZYTANIE_Z_INTERNETU,
                                "Nowości w zakładce " + tabName, tabNum).build());
            } else {
                Objects.requireNonNull(Notifications.notificationManager(context)).cancel(typ.ordinal());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
