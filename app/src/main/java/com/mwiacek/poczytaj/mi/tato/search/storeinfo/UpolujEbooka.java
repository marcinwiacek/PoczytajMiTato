package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.Book;
import com.mwiacek.poczytaj.mi.tato.search.Books;
import com.mwiacek.poczytaj.mi.tato.search.VolumeInfo;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class UpolujEbooka extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        //najpopularniejsze
        return new String[]{"https://upolujebooka.pl/szukaj," +
                pageNumber + "," + name + ".html?order_by=1"};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books, ReentrantLock lock) {
        int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
        String s, s2;
        Book book;
        boolean added = false;
        while (true) {
            startSearchPosition = toPosition;
            fromPosition = pageContent.indexOf("<div class=\"item\">",
                    startSearchPosition);
            if (fromPosition == -1) {
                break;
            }
            toPosition = pageContent.indexOf("<span class=\"priceMax\">", fromPosition);
            s = pageContent.substring(fromPosition, toPosition);

            book = new Book();
            book.offerExpiryDate = null;

            book.volumeInfo = new VolumeInfo();
            book.volumeInfo.smallThumbnail = "https://upolujebooka.pl/" +
                    Utils.findBetween(s, "itemprop=\"image\" src=\"", "\"", 0);

            book.volumeInfo.title = Utils.findBetween(s, "<h2 itemprop=\"name\">", "</h2>", 0);
            book.downloadUrl = "https://upolujebooka.pl/" +
                    Utils.findBetween(s, "<meta itemprop=\"url\" content=\"", "\"", 0);

            book.volumeInfo.authors = new String[1];
            book.volumeInfo.authors[0] =
                    Utils.stripHtml(Utils.findBetween(s, "itemprop=\"author\"  >", "</a>", 0))
                            .trim();

            s2 = Utils.findBetween(s, "<span itemprop=\"price\">", "</span>", 0);
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
        return (pageContent.indexOf("rel=\"next\">następna</a></li>") != -1) &&
                added;
    }
}