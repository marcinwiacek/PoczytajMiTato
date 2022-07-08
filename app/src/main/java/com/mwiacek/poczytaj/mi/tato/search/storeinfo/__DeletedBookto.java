package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.Book;
import com.mwiacek.poczytaj.mi.tato.search.Books;
import com.mwiacek.poczytaj.mi.tato.search.VolumeInfo;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class __DeletedBookto extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        return new String[]{"http://bookto.pl/szukaj/" + name + "/t-e0/p-c0/" + pageNumber};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books, ReentrantLock lock) {
        int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
        String s, s2;
        Book book;
        boolean added = false;
        while (true) {
            startSearchPosition = toPosition;
            fromPosition = pageContent.indexOf("<div class=\"cover\">", startSearchPosition);
            if (fromPosition == -1) {
                break;
            }
            toPosition = pageContent.indexOf("</div></div></div></div></div>", fromPosition);
            s = pageContent.substring(fromPosition, toPosition);

            book = new Book();
            book.offerExpiryDate = null;

            book.volumeInfo = new VolumeInfo();
            book.volumeInfo.smallThumbnail =
                    Utils.findBetween(s, "<img src=\"", "\"", 0);

            book.volumeInfo.title = Utils.findBetween(s, "<span itemprop=\"name\">", "</span>", 0);

            book.downloadUrl =
                    Utils.findBetween(s, "<h3 class=\"title\"><a href=\"", "\"", 0) + ".htm";

            book.volumeInfo.authors = new String[1];
            book.volumeInfo.authors[0] =
                    Utils.stripHtml(
                                    Utils.findBetween(s, "<span itemprop=\"name\">", "</span>",
                                            s.indexOf("<span itemprop=\"author\"")))
                            .trim();
            s2 = Utils.findBetween(s, "<meta itemprop=\"price\" content=\"", "\"", 0);
            if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                    book.volumeInfo.title.isEmpty() || s2.isEmpty()) {
                break;
            }

            book.price = Float.parseFloat(s2);

            if (addBook(book, books, sortOrder, lock)) {
                added = true;
            }
            sortOrder++;
        }
        return (pageContent.indexOf("<i class=\"fa fa-angle-right\"></i>") != -1) &&
                added;
    }
}