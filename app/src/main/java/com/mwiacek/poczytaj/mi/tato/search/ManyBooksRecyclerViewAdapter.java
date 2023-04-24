package com.mwiacek.poczytaj.mi.tato.search;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mwiacek.poczytaj.mi.tato.FragmentConfig;
import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.BookRage;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.EbookiSwiatCzytnikow;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.IBUK;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.UpolujEbooka;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.WolneLektury;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

public class ManyBooksRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final static int REGULAR_VIEW_TYPE_ITEM = 1;

    private final static Executor executor = Executors.newSingleThreadExecutor();
    private final FragmentConfig config;
    private final ArrayList<ManyBooks> mData = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final ProgressBar mProgressBar;
    private final ImageCache mImageCache;
    private final Utils.OnItemClicked mOnClick;
    private int pageNumber = 0;
    private int sitesProcessed = 0;
    private boolean foundSomething = false;

    public ManyBooksRecyclerViewAdapter(FragmentConfig config,
                                        ProgressBar mProgressBar, ImageCache imageCache,
                                        Utils.OnItemClicked mOnClick) {
        this.config = config;
        this.mProgressBar = mProgressBar;
        this.mImageCache = imageCache;
        this.mOnClick = mOnClick;
        this.mProgressBar.setMax(0);
    }

    public void clear() {
        mData.clear();
        notifyDataSetChanged();
        pageNumber = 0;
    }

    ManyBooks getItem(int position) {
        if (mProgressBar.getMax() != 0 && position == mData.size()) return null;
        return mData.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == REGULAR_VIEW_TYPE_ITEM) {
            return new Utils.ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                    R.layout.search_list_item, parent, false), mOnClick);
        }
        return new Utils.InfoTaskListRecyclerViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.info_list_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        if (viewHolder instanceof Utils.InfoTaskListRecyclerViewHolder) {
            ((Utils.InfoTaskListRecyclerViewHolder) viewHolder).description.setText("Szukanie");
            return;
        }
        Utils.ItemViewHolder holder = (Utils.ItemViewHolder) viewHolder;
        List<SingleBook> books = mData.get(position).items;
        SingleBook book = books.get(mData.get(position).itemsPositionForManyBooksList);
        holder.titleText.setText(book.title);
        String s = "";
        if (book.downloadUrl.contains(".epub") && books.size() == 1) {
            s = "Pobierz";
        } else if (book.price != 0.0) {
            s = books.size() != 1 ? "Od " + book.price + " PLN" : book.price + " PLN";
        } else {
            s = books.size() != 1 ? "Od 0.00 PLN" : "Darmowa";
        }
        if (book.offerExpiryDate != null) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat =
                    new SimpleDateFormat("d.M");
            s = s + " do " + dateFormat.format(book.offerExpiryDate);
        }
        holder.priceText.setText(s);
        if (book.authors != null) holder.authorText.setText(book.authors[0]);
        mImageCache.getImageFromCache(this.mProgressBar.getContext(),
                book.smallThumbnailUrl, holder, position);
    }

    @Override
    public int getItemCount() {
        return mData.size() + (mProgressBar.getMax() != 0 ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        return getItem(position) == null ? REGULAR_VIEW_TYPE_ITEM + 1 : REGULAR_VIEW_TYPE_ITEM;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void makeSearch(boolean newSearch, String stringToSearch) {
        if (pageNumber == -1) return;
        pageNumber = newSearch ? 0 : pageNumber + 1;
        mProgressBar.setMax(config.storeInfoForSearchFragment.size());
        mProgressBar.setProgress(0);
        mProgressBar.setVisibility(View.VISIBLE);
        new Handler(Looper.getMainLooper()) {
            @Override
            public void handleMessage(Message msg) {
                notifyItemInserted(mData.size());
            }
        }.sendEmptyMessage(1);
        sitesProcessed = 0;
        foundSomething = false;
        for (StoreInfo.StoreInfoTyp info : config.storeInfoForSearchFragment) {
            StoreInfo st;
            if (info == StoreInfo.StoreInfoTyp.IBUK) {
                st = new IBUK();
            } else if (info == StoreInfo.StoreInfoTyp.BOOKRAGE) {
                st = new BookRage();
            } else if (info == StoreInfo.StoreInfoTyp.UPOLUJ_EBOOKA) {
                st = new UpolujEbooka();
            } else if (info == StoreInfo.StoreInfoTyp.WOLNE_LEKTURY) {
                st = new WolneLektury();
            } else if (info == StoreInfo.StoreInfoTyp.EBOOKI_SWIAT_CZYTNIKOW) {
                st = new EbookiSwiatCzytnikow();
            } else {
                continue;
            }
            executor.execute(() -> {
                String[] urls = st.getSearchUrl(stringToSearch, pageNumber);
                mProgressBar.setMax(mProgressBar.getMax() + urls.length - 1);
                for (String singleURL : urls) {
                    try {
                        StringBuilder content = Utils.getTextPageContent(singleURL,null,null);
                        if (!content.toString().isEmpty()) {
                            if (st.doesItMatch(stringToSearch, singleURL, content, mData, lock,
                                    this)) {
                                foundSomething = true;
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mProgressBar.setProgress(mProgressBar.getProgress() + 1);
                }
                sitesProcessed++;
                new Handler(Looper.getMainLooper()) {
                    @Override
                    public void handleMessage(Message msg) {
                        if (sitesProcessed == config.storeInfoForSearchFragment.size()) {
                            mProgressBar.setMax(0);
                            mProgressBar.setVisibility(View.GONE);
                            notifyItemRemoved(mData.size() + 1);
                            if (!foundSomething) {
                                pageNumber = -1;
                            }
                        }
                    }
                }.sendEmptyMessage(1);
            });
        }
    }
}

