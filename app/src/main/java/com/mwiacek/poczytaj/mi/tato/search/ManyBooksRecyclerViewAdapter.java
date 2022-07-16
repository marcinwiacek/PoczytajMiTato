package com.mwiacek.poczytaj.mi.tato.search;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

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
    private final static Executor executor = Executors.newSingleThreadExecutor();
    private final static int VIEW_TYPE_ITEM = 0;
    private final FragmentConfig config;
    private final ArrayList<ManyBooks> mData = new ArrayList<>();
    private final ReentrantLock lock = new ReentrantLock();
    private final Button mSearchButton;
    private final AutoCompleteTextView mSearchTextView;
    private final ProgressBar mProgressBar;
    private final ImageCache mImageCache;
    private final Utils.OnItemClicked mOnClick;
    private int pageNumber = 0;
    private int sitesProcessed;

    public ManyBooksRecyclerViewAdapter(FragmentConfig config, Button mSearchButton,
                                        AutoCompleteTextView mSearchTextView,
                                        ProgressBar mProgressBar, ImageCache imageCache,
                                        Utils.OnItemClicked mOnClick) {
        this.config = config;
        this.mSearchButton = mSearchButton;
        this.mSearchTextView = mSearchTextView;
        this.mProgressBar = mProgressBar;
        this.mImageCache = imageCache;
        this.mOnClick = mOnClick;
    }

    ManyBooks getItem(int position) {
        return mData.get(position);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Utils.ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                viewType == VIEW_TYPE_ITEM ?
                        R.layout.search_book_list_item : R.layout.item_loading_list_item,
                parent, false), mOnClick);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {

        if (viewHolder instanceof Utils.ItemViewHolder) {
            populateItemRows((Utils.ItemViewHolder) viewHolder, position);
        }
        //} else if (viewHolder instanceof LoadingViewHolder) {
//            showLoadingView((LoadingViewHolder) viewHolder, position);
//        }

    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return mData.get(position) == null ? (VIEW_TYPE_ITEM + 1) : VIEW_TYPE_ITEM;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void makeSearch(boolean pageNumberAdd) {
        if (!pageNumberAdd) {
            mData.clear();
            notifyDataSetChanged();
        }
        pageNumber = pageNumberAdd ? pageNumber + 1 : 0;
        sitesProcessed = 0;
        StoreInfo st;
        Toast.makeText(mSearchButton.getContext(),
                "szukanie", Toast.LENGTH_LONG);
        for (StoreInfo.StoreInfoTyp info : config.storeInfoForSearchFragment) {
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
            StoreInfo finalSt = st;
            executor.execute(() -> {
                mProgressBar.setProgress(0);
                String[] urls = finalSt.getSearchUrl(mSearchTextView.getText().toString(), pageNumber);
                for (String singleURL : urls) {
                    try {
                        StringBuilder content = Utils.getPageContent(singleURL);
                        if (!content.toString().isEmpty()) {
                            System.out.println(
                                    "Wyniki z serwisu " + info.name());
                            finalSt.doesItMatch(mSearchTextView.getText().toString(),
                                    singleURL, content, mData, lock,
                                    this);
                        } else {
                            System.out.println(
                                    "Problem z wynikami z serwisu " + info.name());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mProgressBar.setProgress(mProgressBar.getProgress() +
                            (100 / config.storeInfoForSearchFragment.size() / urls.length));
                }
                sitesProcessed++;
                if (sitesProcessed == config.storeInfoForSearchFragment.size()) {
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            mProgressBar.setProgress(0);
                            mSearchButton.setEnabled(true);
                            mSearchTextView.setEnabled(true);
                            //   mAdapter.notifyDataSetChanged();
                            /*Collections.sort(mData, new Comparator<CustomData>() {
                                @Override
                                public int compare(CustomData lhs, CustomData rhs) {
                                    // -1 - less than, 1 - greater than, 0 - equal, all inversed for descending
                                    return lhs.getId() > rhs.getId() ? -1 : (lhs.customInt < rhs.customInt ) ? 1 : 0;
                                }
                            });*/
                        }
                    }.sendEmptyMessage(1);
                }
            });
        }
    }

//    private void showLoadingView(LoadingViewHolder viewHolder, int position) {
    //  }

    private void populateItemRows(Utils.ItemViewHolder viewHolder, int position) {
        if (mData.get(position) == null) return;
        List<SingleBook> books = mData.get(position).items;
        SingleBook book = books.get(mData.get(position).itemsPositionForManyBooksList);
        viewHolder.titleText.setText(book.title);
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
        viewHolder.priceText.setText(s);
        if (book.authors != null) viewHolder.authorText.setText(book.authors[0]);
        mImageCache.getImageFromCache(this.mProgressBar.getContext(),
                book.smallThumbnailUrl, viewHolder, position);
    }

    /*
    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        public LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
     */
}

