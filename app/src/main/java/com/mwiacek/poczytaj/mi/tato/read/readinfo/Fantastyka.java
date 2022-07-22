package com.mwiacek.poczytaj.mi.tato.read.readinfo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Handler;

import com.mwiacek.poczytaj.mi.tato.Notifications;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.ThreadPoolExecutor;

public class Fantastyka extends ReadInfo {

    private static boolean processOneListEntry(String s, DBHelper mydb, Page.PageTyp typ) {
        int indeks = Utils.Szukaj(s, "<div class=\"autor\">", 0);
        indeks = Utils.Szukaj(s, ">", indeks);
        int indeks2 = s.indexOf(":</a>", indeks);
        String author = s.substring(indeks, indeks2);

        String comments = "";
        indeks = Utils.Szukaj(s, "<div class=\"teksty\"><a href=\"", indeks2);
        if (indeks == -1) {
            indeks = Utils.Szukaj(s, "<div class=\"teksty teksty-piorko\"><a href=\"", indeks2);
            if (indeks != -1) {
                if (Utils.Szukaj(s, "<img src=\"/images/braz.png\" class=\"piorko\" />",
                        indeks) != -1) {
                    comments += "brązowe piórko";
                } else if (Utils.Szukaj(s, "<img src=\"/images/srebro.png\" class=\"piorko\" />",
                        indeks) != -1) {
                    comments += "srebrne piórko";
                } else if (Utils.Szukaj(s, "<img src=\"/images/zloto.png\" class=\"piorko\" />",
                        indeks) != -1) {
                    comments += "złote piórko";
                }
            }
        }
        indeks2 = s.indexOf("\"", indeks);

        String url = "https://www.fantastyka.pl" + s.substring(indeks, indeks2);

        indeks = Utils.Szukaj(s, "class=\"tytul\">", indeks);
        indeks2 = s.indexOf("</a>", indeks);
        String tytul = s.substring(indeks, indeks2).replace("#", "&#35;");
        indeks = indeks2;
        indeks = Utils.Szukaj(s, "<div>", indeks);
        if (Utils.Szukaj(s, "<a class=\"konkurs\" href=\"", indeks) != -1) {
            indeks = Utils.Szukaj(s, "<a class=\"konkurs\" href=\"", indeks);
            indeks = s.indexOf(">", indeks);
            indeks2 = s.indexOf("</a>", indeks);
            if (!comments.isEmpty()) comments += ", ";
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
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format =
                new SimpleDateFormat("dd.MM.yy', g. 'HH:mm");
        Date date = new Date(0);
        try {
            date = format.parse(dt);
        } catch (ParseException e) {
            e.printStackTrace();
        }

        //  Log.i("MY2", "'" + tytul + "'" + author + "'" + comments + "'" + url + "'" + date);
        assert date != null;
        return mydb.insertOrUpdatePage(typ, tytul, author, comments, url, date);
    }

    private static String getTextFromSinglePage(String result, File f) {
        int index = result.indexOf("<section class=\"opko no-headline\">");
        index = result.indexOf("<article>", index) + 9;
        int index2 = result.indexOf("<span class=\"koniec\">Koniec</span>", index);
        return result.substring(index, index2)
                .replaceAll("<br>", "<br />")
                .replaceAll("<hr>", "<hr />")
                .replaceAll("</p>", "")
                .replaceAll("<p />&nbsp; <p />", "<p />")
                .replaceAll("<p /><p />", "<p />")
                .replaceAll("<p /><p />", "<p />")
                .replaceAll("<br /><br />", "<br />")
                .replaceAll("<hr /><p />", "<hr />")
                .replaceAll("&nbsp;", " ")
                .replaceAll("\t\t", "\t")
                .replaceAll("  ", " ")
                .replaceAll(" <p ", "<p ")
                .replaceAll("<p /> <hr />", "<hr />")
                .replaceAll("<p>", "<p />")
                .replaceAll("&oacute;", "ó");
    }

    public void processTextFromSinglePage(
            Context context, Page p,
            final Handler resultHandler,
            final ThreadPoolExecutor executor,
            final Utils.RepositoryCallback<String[]> callbackAfterMainFileWithResourceList,
            final Utils.RepositoryCallback<String> callbackAfterEveryImage,
            final Utils.RepositoryCallback<String> completeCallback) {
        Utils.getTextPage(p.url, result -> {
            String mainPageText = getTextFromSinglePage(result.toString(), p.getCacheFile(context))
                    .replaceAll("<img ", "<img width=\"100%\" ")
                    .replaceAll("http://", "https://");
            ArrayList<String> pictures = Utils.findImagesUrlInHTML(mainPageText);
            for (int i = 0; i < pictures.size(); i++) {
                mainPageText = mainPageText.replace(pictures.get(i),
                        Page.getShortCacheFileName(pictures.get(i)));
            }
            Utils.writeTextFile(p.getCacheFile(context), mainPageText);
            if (callbackAfterMainFileWithResourceList != null) {
                resultHandler.post(() -> callbackAfterMainFileWithResourceList.onComplete(
                        pictures.isEmpty() ?
                                new String[]{} : pictures.toArray(new String[0])));
            }
            if (pictures.isEmpty()) {
                if (completeCallback != null) {
                    String finalMainPageText = mainPageText;
                    resultHandler.post(() -> completeCallback.onComplete(finalMainPageText));
                }
            } else {
                String finalMainPageText1 = mainPageText;
                Utils.getBinaryPages(context, pictures, result2 -> {
                    if (callbackAfterEveryImage != null) {
                        resultHandler.post(() -> callbackAfterEveryImage.onComplete(result2));
                    }
                }, result3 -> {
                    if (completeCallback != null) {
                        resultHandler.post(() -> completeCallback.onComplete(finalMainPageText1));
                    }
                }, resultHandler, executor);
            }
        }, resultHandler, executor);
    }

    public void getList(final Context context,
                        final Handler resultHandler,
                        final DBHelper mydb, Page.PageTyp typ,
                        int pageStart, int pageStop,
                        final Utils.RepositoryCallback<Page.PageTyp> callbackOnUpdatedPage) {
        try {
            String url = "";
            int index = pageStart;
            boolean haveNewEntry = false;
            while (true) {
                Objects.requireNonNull(Notifications.notificationManager(context)).notify(
                        typ.ordinal(), Notifications.setupNotification(context,
                                Notifications.Channels.CZYTANIE_Z_INTERNETU,
                                "Czytanie " + typ.name() + " - strona " + index).build());

                if (typ == Page.PageTyp.FANTASTYKA_POCZEKALNIA) {
                    url = "/opowiadania/wszystkie" +
                            (index == 1 ? "" : "/w/w/w/0/d/" + index);
                } else if (typ == Page.PageTyp.FANTASTYKA_BIBLIOTEKA) {
                    url = "/opowiadania/biblioteka" +
                            (index == 1 ? "" : "/w/w/w/0/d/" + index);
                } else if (typ == Page.PageTyp.FANTASTYKA_ARCHIWUM) {
                    url = "/opowiadania/archiwum/d" +
                            (index == 1 ? "" : "/" + index);
                }

                String result = Utils.getTextPageContent("https://www.fantastyka.pl"
                        + url).toString();
                int indeks = result.indexOf("<article style=\"margin-top: 4px;\">");
                boolean haveNewEntryOnThisPage = false;
                while (true) {
                    indeks = result.indexOf("<div class=\"lista\">", indeks);
                    int indeks2 = result.indexOf("<div class=\"clear linia\"></div>", indeks);
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
                if (result.contains("<a href=\"" + url + "\" title=\"koniec\">")) {
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
                                typ.name() + " - nowe strony lub wersje stron").build());
            } else {
                Objects.requireNonNull(Notifications.notificationManager(context)).cancel(typ.ordinal());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
