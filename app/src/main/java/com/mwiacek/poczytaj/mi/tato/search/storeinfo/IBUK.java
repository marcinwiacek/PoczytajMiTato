package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.Book;
import com.mwiacek.poczytaj.mi.tato.search.Books;
import com.mwiacek.poczytaj.mi.tato.search.VolumeInfo;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class IBUK extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        return new String[]{"https://www.ibuk.pl/szukaj/ala.html?pid=4&co=" + name +
                "&od=" + ((pageNumber - 1) * 15) + "&limit=15" + "&typ_publikacji=epub,mobi,pdf"};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books, ReentrantLock lock) {
        int startSearchPosition, fromPosition, toPosition = 0, sortOrder = 1;
        String s, s2;
        Book book;
        boolean added = false;
        while (true) {
            startSearchPosition = toPosition;
            fromPosition = pageContent.indexOf("<div class=\"bookitem\">",
                    startSearchPosition);
            if (fromPosition == -1) {
                break;
            }
            toPosition = pageContent.indexOf("<div style=\"clear: both; width: 100%;\"></div>", fromPosition);
            s = pageContent.substring(fromPosition, toPosition);

            book = new Book();
            book.offerExpiryDate = null;

            book.volumeInfo = new VolumeInfo();
            book.volumeInfo.smallThumbnail =
                    "https://www.ibuk.pl" +
                            Utils.findBetween(
                                    Utils.findBetween(s,
                                            "<div class=\"tablecell okladaka\">", "</div>", 0),
                                    "<img src=\"", "\"", 0);

            book.volumeInfo.title =
                    Utils.findBetween(
                            Utils.findBetween(s, "<h2>", "</h2>", 0),
                            "\" >", "<br />", 0);

            book.downloadUrl =
                    "https://www.ibuk.pl" +
                            Utils.findBetween(s, "<h2><a href=\"", "\" >", 0);

            book.volumeInfo.authors = new String[1];
            book.volumeInfo.authors[0] =
                    Utils.findBetween(
                            Utils.findBetween(s, "<span><a href=\"https://www.ibuk.pl/szukaj/", "/a>",
                                    0),
                            "\">", "<", 0);

            s2 = Utils.findBetween(s, "Kup teraz za <b style=\"color: black;\"> ",
                    " zł", 0);

            if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                    book.volumeInfo.title.isEmpty() || s2.isEmpty()) {
                break;
            }

            book.price = Float.parseFloat(s2.replace(",", "."));

            if (addBook(book, books, sortOrder, lock)) {
                added = true;
            }
            sortOrder++;
        }
        return (pageContent.indexOf("<div class=\"pagination font-size14\">") != -1) &&
                added;
    }
}
