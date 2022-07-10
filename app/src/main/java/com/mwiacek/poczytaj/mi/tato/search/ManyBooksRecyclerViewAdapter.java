package com.mwiacek.poczytaj.mi.tato.search;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
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
import java.util.concurrent.locks.ReentrantLock;

public class ManyBooksRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
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
        for (StoreInfo.StoreInfoTyp info : config.storeInfoForSearchFragment) {
            StoreInfo st = null;
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
            }
            new DownloadBooksInfoTask(this, mSearchButton, mProgressBar,
                    mSearchTextView, config)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                            mSearchTextView.getText().toString(),
                            st,
                            mData,
                            100 / config.storeInfoForSearchFragment.size(),
                            this);
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

    private class DownloadBooksInfoTask extends AsyncTask<Object, Object, ArrayList<ManyBooks>> {
        public final FragmentConfig config;
        private final ManyBooksRecyclerViewAdapter mAdapter;
        private final Button mSearchButton;
        private final ProgressBar mProgressBar;
        private final AutoCompleteTextView mSearchTextView;

        DownloadBooksInfoTask(ManyBooksRecyclerViewAdapter adapter, Button searchButton,
                              ProgressBar progressBar, AutoCompleteTextView mSearchTextView,
                              FragmentConfig config) {
            mAdapter = adapter;
            mSearchButton = searchButton;
            mProgressBar = progressBar;
            this.mSearchTextView = mSearchTextView;
            this.config = config;
            mProgressBar.setProgress(0);
        }

        @Override
        protected ArrayList<ManyBooks> doInBackground(Object... params) {
            String nameToSearch = (String) params[0];
            StoreInfo ele = (StoreInfo) params[1];
            ArrayList<ManyBooks> allBooks = (ArrayList<ManyBooks>) params[2];
            String[] urls = ele.getSearchUrl(nameToSearch, pageNumber);
            for (String singleURL : urls) {
                try {
                    StringBuilder content = Utils.getPageContent(singleURL);
                    if (!content.toString().isEmpty()) {
                        ele.doesItMatch(nameToSearch, singleURL, content, allBooks, lock,
                                mAdapter);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                mProgressBar.setProgress(mProgressBar.getProgress() + ((int) (params[3]) / urls.length));
            }
            sitesProcessed++;

            return allBooks;
        }

        protected void onPostExecute(ArrayList<ManyBooks> books) {
            if (sitesProcessed == config.storeInfoForSearchFragment.size()) {
                mProgressBar.setProgress(0);
                mSearchButton.setEnabled(true);
                mSearchTextView.setEnabled(true);
                //   mAdapter.notifyDataSetChanged();
            }
        }
    }

}

