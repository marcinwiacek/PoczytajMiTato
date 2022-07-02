package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.Book;
import com.mwiacek.poczytaj.mi.tato.search.Books;
import com.mwiacek.poczytaj.mi.tato.search.VolumeInfo;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class BookRage extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        return new String[]{"https://artrage.pl/bookrage", "https://artrage.pl/bookrage/quick"};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books, ReentrantLock lock) {
        int startSearchPosition, fromPosition, fromPosition2, toPosition = 0, sortOrder = 1;
        String s;
        Book book;
        boolean added = false;
        Date expiryDate;
        float price = (float) 0;

        s = Utils.findBetween(pageContent.toString(), "<p data-ends-at=\"", "\"", 0);
        if (s.equals("")) return false;

        SimpleDateFormat format = new SimpleDateFormat("y/M/d H:m");
        try {
            expiryDate = format.parse(s);
        } catch (ParseException e) {
            return false;
        }

        while (true) {
            startSearchPosition = toPosition;

            fromPosition = pageContent.indexOf("<article class=\"book\"",
                    startSearchPosition);
            if (fromPosition == -1) {
                break;
            }
            fromPosition2 = pageContent.indexOf("<h5",
                    startSearchPosition);

            if (fromPosition2 != -1 && fromPosition2 < fromPosition) {
                toPosition = pageContent.indexOf("</h5>", fromPosition2);
                s = Utils.findBetween(
                        pageContent.substring(fromPosition2, toPosition),
                        "<strong>", " ", 0);

                if (s.equals("")) {
                    s = Utils.findBetween(
                            pageContent.substring(fromPosition2, toPosition),
                            "<strong>", "PLN", 0);
                }
                if (s.equals("")) {
                    break;
                }

                price = Float.parseFloat(s.replace(",", "."));
            }
            if (price == 0.00) {
                break;
            }

            toPosition = pageContent.indexOf("</article>", fromPosition);
            s = pageContent.substring(fromPosition, toPosition);


            book = new Book();

            book.price = price;

            book.offerExpiryDate = expiryDate;
            book.volumeInfo = new VolumeInfo();
            book.volumeInfo.smallThumbnail = "https://artrage.pl" +
                    Utils.findBetween(s, "<img src=\"", "\"", 0);

            book.volumeInfo.title = Utils.findBetween(s, "<h4>", "</h4>", 0);

            book.downloadUrl = url;

            book.volumeInfo.authors = new String[1];
            book.volumeInfo.authors[0] =
                    Utils.findBetween(s, "<p class=\"author\">", "</p>", 0);

            if (!book.volumeInfo.authors[0].toLowerCase().contains(name.toLowerCase()) &&
                    !book.volumeInfo.title.toLowerCase().contains(name.toLowerCase())) {
                continue;
            }
            if (addBook(book, books, sortOrder, lock)) {
                added = true;
            }
            sortOrder++;
        }
        return added;
    }
}
