package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import com.mwiacek.poczytaj.mi.tato.search.Book;
import com.mwiacek.poczytaj.mi.tato.search.Books;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public abstract class StoreInfo {
    public abstract boolean doesItMatch(String name, String url,
                                        StringBuilder pageContent, ArrayList<Books> books, ReentrantLock lock);

    public abstract String[] getSearchUrl(String name, int pageNumber);

    public boolean addBook(Book book, ArrayList<Books> allBooks, int sortOrder, ReentrantLock lock) {
        Book b2;
        lock.lock();
        try {
            for (Books books : allBooks) {
                for (Book b : books.items) {
                    if (book.volumeInfo.title.equalsIgnoreCase(b.volumeInfo.title) &&
                            book.volumeInfo.authors[0].equalsIgnoreCase(b.volumeInfo.authors[0]) &&
                            book.downloadUrl.equals(b.downloadUrl)) {
                        return false;
                    }
                }
                b2 = books.items.get(books.positionInMainList);
                if (book.volumeInfo.title.equalsIgnoreCase(b2.volumeInfo.title) &&
                        book.volumeInfo.authors[0].equalsIgnoreCase(b2.volumeInfo.authors[0])) {
                    books.items.add(book);
                    if (book.price < b2.price || book.downloadUrl.contains(".epub")) {
                        books.positionInMainList = books.items.size() - 1;
                        //books.sortOrderInMainList = sortOrder;
                    }

                    return true;
                }

            }
            Books nb = new Books();
            nb.items = new ArrayList<>();
            nb.items.add(book);
            nb.positionInMainList = 0;
            //nb.sortOrderInMainList = sortOrder;
            allBooks.add(nb);
            return true;
        } finally {
            lock.unlock();
        }
    }
}