package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import android.annotation.SuppressLint;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooks;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooksRecyclerViewAdapter;
import com.mwiacek.poczytaj.mi.tato.search.SingleBook;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

public class BookRage extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        return new String[]{"https://artrage.pl/bookrage", "https://artrage.pl/bookrage/quick"};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent,
                               ArrayList<ManyBooks> books, ReentrantLock lock,
                               ManyBooksRecyclerViewAdapter adapter) {
        int startSearchPosition, fromPosition, fromPosition2, toPosition = 0;
        String s;
        SingleBook singleBook;
        boolean added = false;
        Date expiryDate;
        float price = (float) 0;

        s = Utils.findBetween(pageContent.toString(), "<p data-ends-at=\"", "\"",
                0);
        if (s.equals("")) return false;

        @SuppressLint("SimpleDateFormat") SimpleDateFormat format =
                new SimpleDateFormat("y/M/d H:m");
        try {
            expiryDate = format.parse(s);
        } catch (ParseException e) {
            return false;
        }

        while (true) {
            startSearchPosition = toPosition;

            fromPosition = pageContent.indexOf("<article class=\"book\"", startSearchPosition);
            if (fromPosition == -1) {
                break;
            }
            fromPosition2 = pageContent.indexOf("<h5", startSearchPosition);

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

            singleBook = new SingleBook();
            singleBook.price = price;
            singleBook.offerExpiryDate = expiryDate;
            singleBook.smallThumbnailUrl = "https://artrage.pl" +
                    Utils.findBetween(s, "<img src=\"", "\"", 0);
            singleBook.title = Utils.findBetween(s, "<h4>", "</h4>", 0);
            singleBook.downloadUrl = url;
            singleBook.authors = new String[1];
            singleBook.authors[0] =
                    Utils.findBetween(s, "<p class=\"author\">", "</p>", 0);

            if (!singleBook.authors[0].toLowerCase().contains(name.toLowerCase()) &&
                    !singleBook.title.toLowerCase().contains(name.toLowerCase())) {
                continue;
            }
            if (addBook(singleBook, books, lock, adapter)) {
                added = true;
            }
        }
        return added;
    }
}
