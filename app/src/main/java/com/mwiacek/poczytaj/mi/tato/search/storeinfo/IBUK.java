package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooks;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooksRecyclerViewAdapter;
import com.mwiacek.poczytaj.mi.tato.search.SingleBook;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class IBUK extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        return new String[]{"https://www.ibuk.pl/szukaj/ala.html?pid=4&co=" + name +
                "&od=" + ((pageNumber - 1) * 15) + "&limit=15" +
                "&typ_publikacji=epub,mobi,pdf"};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent,
                               ArrayList<ManyBooks> books, ReentrantLock lock, ManyBooksRecyclerViewAdapter adapter) {
        int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
        String s, s2;
        SingleBook singleBook;
        boolean added = false;
        while (true) {
            startSearchPosition = toPosition;
            fromPosition = pageContent.indexOf("<div class=\"bookitem\">", startSearchPosition);
            if (fromPosition == -1) break;
            toPosition = pageContent.indexOf("<div style=\"clear: both; width: 100%;\"></div>", fromPosition);
            s = pageContent.substring(fromPosition, toPosition);

            singleBook = new SingleBook();
            singleBook.offerExpiryDate = null;

            singleBook.smallThumbnailUrl = "https://www.ibuk.pl" + Utils.findBetween(Utils.findBetween(s,
                            "<div class=\"tablecell okladaka\">", "</div>", 0),
                    "<img src=\"", "\"", 0);
            singleBook.title = Utils.findBetween(Utils.findBetween(s,
                            "<h2>", "</h2>", 0),
                    "\" >", "<br />", 0);
            singleBook.downloadUrl = "https://www.ibuk.pl" +
                    Utils.findBetween(s, "<h2><a href=\"", "\" >", 0);
            singleBook.authors = new String[1];
            singleBook.authors[0] = Utils.findBetween(Utils.findBetween(s,
                            "<span><a href=\"https://www.ibuk.pl/szukaj/", "/a>", 0),
                    "\">", "<", 0);

            s2 = Utils.findBetween(s, "Kup teraz za <b style=\"color: black;\"> ",
                    " zł", 0);
            if (singleBook.downloadUrl.isEmpty() || singleBook.smallThumbnailUrl.isEmpty() ||
                    singleBook.title.isEmpty() || s2.isEmpty()) {
                break;
            }
            singleBook.price = Float.parseFloat(s2.replace(",", "."));

            if (addBook(singleBook, books, sortOrder, lock, adapter)) {
                added = true;
            }
            sortOrder++;
        }
        return (pageContent.indexOf("<div class=\"pagination font-size14\">") != -1) &&
                added;
    }
}
