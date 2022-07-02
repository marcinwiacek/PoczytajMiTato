package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.Book;
import com.mwiacek.poczytaj.mi.tato.search.Books;
import com.mwiacek.poczytaj.mi.tato.search.VolumeInfo;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class EbookiSwiatCzytnikow extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        //popularne
        return new String[]{"https://ebooki.swiatczytnikow.pl/szukaj/" +
                name + "?strona=" + pageNumber};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books, ReentrantLock lock) {
        int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
        String s, s2;
        Book book;
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

            book = new Book();

            book.offerExpiryDate = null;

            book.volumeInfo = new VolumeInfo();
            book.volumeInfo.smallThumbnail = "https://" +
                    Utils.findBetween(s, "<img src=\"", "\"", 0);

            s2 = Utils.findBetween(s, "<div class=\"title\">", "</div>", 0);
            book.volumeInfo.title = Utils.findBetween(s2, "\">", "</a>", s2.indexOf("<a href=\""));
            book.downloadUrl = "https://ebooki.swiatczytnikow.pl/" +
                    Utils.findBetween(s2, "<a href=\"", "\">", 0);

            book.volumeInfo.authors = new String[1];
            book.volumeInfo.authors[0] =
                    Utils.stripHtml(Utils.findBetween(s, "<div class=\"author\">", "</div>", 0))
                            .trim();

            if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                    book.volumeInfo.title.isEmpty()) {
                break;
            }

            s2 = Utils.findBetween(s, "<strong>", "</strong>", 0);
            if (s2.equals("")) {
                s2 = Utils.findBetween(s, "<strong id=\"\">", "</strong>", 0);
            }
            if (s2.equals("")) {
                book.price = (float) 0.0;
            } else {
                book.price = Float.parseFloat(s2.replace(",", "."));
            }

            if (addBook(book, books, sortOrder, lock)) {
                added = true;
            }
            sortOrder++;
        }
        return (pageContent.indexOf("\">następna »</a></div>") != -1 &&
                added);
    }
}
