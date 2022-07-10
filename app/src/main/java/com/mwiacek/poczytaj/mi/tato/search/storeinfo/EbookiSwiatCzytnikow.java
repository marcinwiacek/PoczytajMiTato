package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooks;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooksRecyclerViewAdapter;
import com.mwiacek.poczytaj.mi.tato.search.SingleBook;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class EbookiSwiatCzytnikow extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        //popularne
        return new String[]{"https://ebooki.swiatczytnikow.pl/szukaj/" +
                name + "?strona=" + pageNumber};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent,
                               ArrayList<ManyBooks> books, ReentrantLock lock, ManyBooksRecyclerViewAdapter adapter) {
        int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
        String s, s2;
        SingleBook singleBook;
        boolean added = false;

        while (true) {
            startSearchPosition = toPosition;
            fromPosition = pageContent.indexOf("<li class=\"resultbox\">",
                    startSearchPosition);
            if (fromPosition == -1) {
                break;
            }
            toPosition = pageContent.indexOf("</li>", fromPosition);
            s = pageContent.substring(fromPosition, toPosition);

            if (s.contains("Brak ofert")) {
                continue;
            }

            singleBook = new SingleBook();

            singleBook.offerExpiryDate = null;
            singleBook.smallThumbnailUrl = "https://" +
                    Utils.findBetween(s, "<img src=\"", "\"", 0);

            s2 = Utils.findBetween(s, "<div class=\"title\">", "</div>", 0);
            singleBook.title = Utils.findBetween(s2, "\">", "</a>", s2.indexOf("<a href=\""));
            singleBook.downloadUrl = "https://ebooki.swiatczytnikow.pl" +
                    Utils.findBetween(s2, "<a href=\"", "\">", 0);

            singleBook.authors = new String[1];
            singleBook.authors[0] =
                    Utils.stripHtml(Utils.findBetween(s, "<div class=\"author\">", "</div>", 0))
                            .trim();

            if (singleBook.downloadUrl.isEmpty() || singleBook.smallThumbnailUrl.isEmpty() ||
                    singleBook.title.isEmpty()) {
                break;
            }

            s2 = Utils.findBetween(s, "<strong>", "</strong>", 0);
            if (s2.equals("")) {
                s2 = Utils.findBetween(s, "<strong id=\"\">", "</strong>", 0);
            }
            singleBook.price = s2.equals("") ?
                    (float) 0.0 : Float.parseFloat(s2.replace(",", "."));

            if (addBook(singleBook, books, sortOrder, lock, adapter)) {
                added = true;
            }
            sortOrder++;
        }
        return (pageContent.indexOf("\">następna »</a></div>") != -1 &&
                added);
    }
}
