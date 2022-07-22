package com.mwiacek.poczytaj.mi.tato.search.storeinfo;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;

import com.mwiacek.poczytaj.mi.tato.search.ManyBooks;
import com.mwiacek.poczytaj.mi.tato.search.ManyBooksRecyclerViewAdapter;
import com.mwiacek.poczytaj.mi.tato.search.SingleBook;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public abstract class StoreInfo {
    public abstract boolean doesItMatch(
            String name, String url, StringBuilder pageContent,
            ArrayList<ManyBooks> books, ReentrantLock lock,
            ManyBooksRecyclerViewAdapter adapter);

    public abstract String[] getSearchUrl(String name, int pageNumber);

    public boolean addBook(SingleBook singleBook, ArrayList<ManyBooks> allBooks,
                           ReentrantLock lock, ManyBooksRecyclerViewAdapter adapter) {
        SingleBook b2;
        lock.lock();
        try {
            for (int i = 0; i < allBooks.size(); i++) {
                ManyBooks manyBooks = allBooks.get(i);
                for (SingleBook b : manyBooks.items) {
                    if (singleBook.title.equalsIgnoreCase(b.title) &&
                            singleBook.authors[0].equalsIgnoreCase(b.authors[0]) &&
                            singleBook.downloadUrl.equals(b.downloadUrl)) {
                        return false;
                    }
                }
                b2 = manyBooks.items.get(manyBooks.itemsPositionForManyBooksList);
                if (singleBook.title.equalsIgnoreCase(b2.title) &&
                        singleBook.authors[0].equalsIgnoreCase(b2.authors[0])) {
                    manyBooks.items.add(singleBook);
                    if (singleBook.price < b2.price || singleBook.downloadUrl.contains(".epub")) {
                        manyBooks.itemsPositionForManyBooksList = manyBooks.items.size() - 1;
                        int finalI = i;
                        new Handler(Looper.getMainLooper()) {
                            @Override
                            public void handleMessage(Message msg) {
                                adapter.notifyItemChanged(finalI);
                            }
                        }.sendEmptyMessage(1);
                    }
                    return true;
                }
            }
            ManyBooks nb = new ManyBooks();
            nb.items = new ArrayList<>();
            nb.items.add(singleBook);
            nb.itemsPositionForManyBooksList = 0;
            allBooks.add(nb);
            new Handler(Looper.getMainLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    adapter.notifyItemRangeInserted(allBooks.size() - 2, 1);
                }
            }.sendEmptyMessage(1);
            return true;
        } finally {
            lock.unlock();
        }
    }

    public enum StoreInfoTyp {
        BOOKRAGE,
        EBOOKI_SWIAT_CZYTNIKOW,
        IBUK,
        UPOLUJ_EBOOKA,
        WOLNE_LEKTURY
    }
}