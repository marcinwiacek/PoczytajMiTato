package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.Book;
import com.mwiacek.poczytaj.mi.tato.search.Books;
import com.mwiacek.poczytaj.mi.tato.search.VolumeInfo;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class WolneLektury extends StoreInfo {
    public String[] getSearchUrl(String name, int pageNumber) {
        return new String[]{"https://wolnelektury.pl/szukaj/?q=" + name};
    }

    public boolean doesItMatch(String name, String url, StringBuilder pageContent, ArrayList<Books> books, ReentrantLock lock) {
        String formattedName = name.toLowerCase().replaceAll("\\s", "-");
        formattedName = Normalizer.normalize(formattedName, Normalizer.Form.NFD);
        formattedName = formattedName.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");

        int startSearchPosition, fromPosition, toPosition, sortOrder = 1;
        Book book;
        String s;

        fromPosition = pageContent.indexOf("<div class=\"book-box-inner\"><p>Znalezione w treści</p></div>");
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

            book = new Book();

            book.offerExpiryDate = null;

            book.price = 0;
            book.volumeInfo = new VolumeInfo();
            book.volumeInfo.title =
                    Utils.stripHtml(Utils.findBetween(s, "<div class=\"title\">", "</div>", 0))
                            .trim();
            book.volumeInfo.authors = new String[1];
            book.volumeInfo.authors[0] = Utils.findBetween(s, "/\">", "</a>",
                    s.indexOf("<div class=\"author\">"));
            book.volumeInfo.smallThumbnail = "https://wolnelektury.pl" +
                    Utils.findBetween(s, "<img src=\"", "\" alt=\"Cover\"", 0);
            book.downloadUrl = "https://wolnelektury.pl/media/book/epub/" +
                    formattedName + ".epub";

            if (book.downloadUrl.isEmpty() || book.volumeInfo.smallThumbnail.isEmpty() ||
                    book.volumeInfo.title.isEmpty()) {
                break;
            }
            addBook(book, books, sortOrder, lock);
            sortOrder++;
        }
        return false;
    }
}