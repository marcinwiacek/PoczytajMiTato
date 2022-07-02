package com.mwiacek.poczytaj.mi.tato.read;

import android.os.Handler;
import android.util.Log;

import com.mwiacek.poczytaj.mi.tato.Utils;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

public class Fantastyka {
    static public String getOpko(String result, File f) {
        int index = result.indexOf("<section class=\"opko no-headline\">");
        int index2 = result.indexOf("<span class=\"koniec\">Koniec</span>", index);
        Utils.writeFile(f, result.substring(index, index2));
        return result.substring(index, index2);
    }

    static public void getList(
            final Utils.RepositoryCallback<StringBuilder> callback,
            final Utils.RepositoryCallback<StringBuilder> callbackOnTheEnd,
            final Handler resultHandler,
            final ThreadPoolExecutor executor,
            final DBHelper mydb) {
        executor.execute(() -> {
            try {
                String url = "";
                int index = 1;
                while (true) {
                    url = "https://www.fantastyka.pl/opowiadania/wszystkie" +
                            (index == 1 ? "" : "/w/w/w/0/d/" + index);
                    String result = Utils.getPageContent(url).toString();
                    int indeks = result.indexOf("<article style=\"margin-top: 4px;\">");
                    while (true) {
                        indeks = result.indexOf("<div class=\"lista\">", indeks);
                        if (indeks == -1) break;
                        indeks = Utils.Szukaj(result, "<div class=\"autor\">", indeks);
                        indeks = Utils.Szukaj(result, ">", indeks);
                        int indeks2 = result.indexOf("</a>", indeks);
                        String author = result.substring(indeks, indeks2);
                        indeks = indeks2;
                        indeks = Utils.Szukaj(result, "<div class=\"teksty\"><a href=\"", indeks);
                        indeks2 = result.indexOf("\"", indeks);
                        String url2 = "https://www.fantastyka.pl" +
                                result.substring(indeks, indeks2);
                        indeks = indeks2;
                        indeks = Utils.Szukaj(result, "class=\"tytul\">", indeks);
                        indeks2 = result.indexOf("</a>", indeks);
                        String tytul = result.substring(indeks, indeks2).replace("#", "&#35;");
                        indeks = Utils.Szukaj(result, "class=\"tytul\">", indeks);
                        Log.i("MY", "'" + tytul + "'");
                        indeks = indeks2;
                        indeks = Utils.Szukaj(result, "  | ", indeks);
                        indeks = Utils.Szukaj(result, " | ", indeks);
                        indeks2 = result.indexOf("  ", indeks);
                        Log.i("MY", "'" + indeks + "'" + indeks2);
                        Log.i("MY", result.substring(indeks, indeks + 100));
                        String dt = result.substring(indeks, indeks2);
                        Log.i("MY", "'" + dt + "'");
                        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yy', g. 'HH:mm");
                        Date date = new Date(0);
                        try {
                            date = format.parse(dt);
                        } catch (ParseException e) {
                            e.printStackTrace();
                        }
                        indeks = indeks2;
                        mydb.insertPage(tytul, author, "", url2, date);
                    }
                    Utils.notifyResult(new StringBuilder(), callback, resultHandler);
                    index++;
                    if (index == 3) break;
                }
                Utils.notifyResult(new StringBuilder(), callbackOnTheEnd, resultHandler);
            } catch (Exception e) {
                Log.i("MY", "Error " + e.getMessage());
            }
        });
    }
}
