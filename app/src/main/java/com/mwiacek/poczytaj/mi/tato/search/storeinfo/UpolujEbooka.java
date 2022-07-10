package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooks;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooksRecyclerViewAdapter;
import com.mwiacek.poczytaj.mi.tato.search.SingleBook;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class UpolujEbooka extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        //najpopularniejsze
        return new String[]{"https://upolujebooka.pl/szukaj," +
                pageNumber + "," + name + ".html?order_by=1"};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent,
                               ArrayList<ManyBooks> books, ReentrantLock lock, ManyBooksRecyclerViewAdapter adapter) {
        int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
        String s, s2;
        SingleBook singleBook;
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

            singleBook = new SingleBook();
            singleBook.offerExpiryDate = null;
            singleBook.smallThumbnailUrl = "https://upolujebooka.pl/" +
                    Utils.findBetween(s, "itemprop=\"image\" src=\"", "\"", 0);
            singleBook.title = Utils.findBetween(s, "<h2 itemprop=\"name\">", "</h2>", 0);
            singleBook.downloadUrl = "https://upolujebooka.pl/" +
                    Utils.findBetween(s, "<meta itemprop=\"url\" content=\"", "\"", 0);
            singleBook.authors = new String[1];
            singleBook.authors[0] = Utils.stripHtml(Utils.findBetween(s,
                    "itemprop=\"author\"  >", "</a>", 0)).trim();

            s2 = Utils.findBetween(s, "<span itemprop=\"price\">", "</span>", 0);
            if (singleBook.downloadUrl.isEmpty() || singleBook.smallThumbnailUrl.isEmpty() ||
                    singleBook.title.isEmpty() || s2.isEmpty()) {
                break;
            }
            singleBook.price = Float.parseFloat(s2);

            if (addBook(singleBook, books, sortOrder, lock, adapter)) {
                added = true;
            }
            sortOrder++;
        }
        return (pageContent.indexOf("rel=\"next\">następna</a></li>") != -1) && added;
    }
}