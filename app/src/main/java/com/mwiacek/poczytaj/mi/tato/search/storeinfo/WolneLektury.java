package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooks;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooksRecyclerViewAdapter;
import com.mwiacek.poczytaj.mi.tato.search.SingleBook;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class WolneLektury extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        return new String[]{"https://wolnelektury.pl/szukaj/?q=" + name};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent,
                               ArrayList<ManyBooks> books, ReentrantLock lock,
                               ManyBooksRecyclerViewAdapter adapter) {
        int startSearchPosition, fromPosition, toPosition;
        SingleBook singleBook;
        String s;

        String formattedName = name.toLowerCase().replaceAll("\\s", "-");
        formattedName = Normalizer.normalize(formattedName, Normalizer.Form.NFD);
        formattedName = formattedName.replaceAll("\\p{InCombiningDiacriticalMarks}+",
                "");

        fromPosition = pageContent
                .indexOf("<div class=\"book-box-inner\"><p>Znalezione w treści</p></div>");
        if (fromPosition != -1) {
            toPosition = pageContent.length();
            pageContent.delete(fromPosition, toPosition);
        }
        toPosition = 0;
        while (true) {
            startSearchPosition = toPosition;
            fromPosition = pageContent.indexOf("<div class=\"book-box-inner\">",
                    startSearchPosition);
            if (fromPosition == -1) {
                break;
            }
            toPosition = pageContent.indexOf("</div></li>", fromPosition);
            s = pageContent.substring(fromPosition, toPosition);

            singleBook = new SingleBook();
            singleBook.offerExpiryDate = null;
            singleBook.price = 0;
            singleBook.title = Utils.stripHtml(Utils.findBetween(s,
                    "<div class=\"title\">", "</div>", 0)).trim();
            singleBook.authors = new String[1];
            singleBook.authors[0] = Utils.findBetween(s, "/\">", "</a>",
                    s.indexOf("<div class=\"author\">"));
            singleBook.smallThumbnailUrl =
                    Utils.findBetween(s, "<img src=\"", "\" alt=\"Cover\"", 0);
            if (singleBook.smallThumbnailUrl.isEmpty()) {
                singleBook.smallThumbnailUrl =
                        Utils.findBetween(s, "<img class=\"cover\" src=\"", "\"",
                                0);
            }
            if (!singleBook.smallThumbnailUrl.isEmpty()) {
                singleBook.smallThumbnailUrl = "https://wolnelektury.pl" +
                        singleBook.smallThumbnailUrl;
            }

            singleBook.downloadUrl = "https://wolnelektury.pl/media/book/epub/" +
                    formattedName + ".epub";

            if (singleBook.smallThumbnailUrl.isEmpty() || singleBook.title.isEmpty()) {
                break;
            }
            addBook(singleBook, books, lock, adapter);
        }
        return false;
    }
}