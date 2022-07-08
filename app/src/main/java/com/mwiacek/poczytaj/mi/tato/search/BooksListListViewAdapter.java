package com.mwiacek.poczytaj.mi.tato.search;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

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
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantLock;

public class BooksListListViewAdapter extends BaseAdapter {
    private final ImageCacheForListView mImageCache;
    private final ArrayList<Books> mData = new ArrayList<>();
    private final ArrayList<StoreInfo> mList = new ArrayList<>();
    private final Button mSearchButton;
    private final AutoCompleteTextView mSearchTextView;
    private final ProgressBar mProgressBar;
    private final ReentrantLock lock = new ReentrantLock();
    private String mSearchText;
    private int pageNumber;
    private int sitesWithContent;
    private int sitesProcessed;

    public BooksListListViewAdapter(ImageCacheForListView imageCache,
                                    Button searchButton, ProgressBar progressBar,
                                    AutoCompleteTextView searchTextView) {
        mImageCache = imageCache;
        mSearchButton = searchButton;
        mProgressBar = progressBar;
        mSearchTextView = searchTextView;
    }

    /**
     * Method for starting books search
     */
    public void BooksListListViewAdapterSearch(String searchText, Context context,
                                               FragmentConfig config) {
        ConnectivityManager cm = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null &&
                activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(context, "Mistrzu, lepiej działa z siecią!", Toast.LENGTH_SHORT)
                    .show();
            mSearchButton.setEnabled(true);
            mSearchTextView.setEnabled(true);
            return;
        }

        if (mData.size() != 0) {
            mData.clear();
            notifyDataSetInvalidated();
        }

        mList.clear();
        if (config.storeInfoForSearchFragment.contains(StoreInfo.StoreInfoTyp.WOLNE_LEKTURY)) {
            mList.add(new WolneLektury());
        }
        if (config.storeInfoForSearchFragment.contains(StoreInfo.StoreInfoTyp.EBOOKI_SWIAT_CZYTNIKOW)) {
            mList.add(new EbookiSwiatCzytnikow());
        }
        if (config.storeInfoForSearchFragment.contains(StoreInfo.StoreInfoTyp.UPOLUJ_EBOOKA)) {
            mList.add(new UpolujEbooka());
        }
        if (config.storeInfoForSearchFragment.contains(StoreInfo.StoreInfoTyp.BOOKRAGE)) {
            mList.add(new BookRage());
        }
        if (config.storeInfoForSearchFragment.contains(StoreInfo.StoreInfoTyp.IBUK)) {
            mList.add(new IBUK());
        }
        if (mList.size() == 0) {
            Toast.makeText(context, "Sorry Gregory, nie wybrano żadnej wyszukiwarki w opcjach!", Toast.LENGTH_SHORT)
                    .show();
            mSearchButton.setEnabled(true);
            mSearchTextView.setEnabled(true);
            return;
        }

        mSearchText = searchText;

        pageNumber = 1;
        sitesWithContent = 0;
        sitesProcessed = 0;
        Iterator<StoreInfo> itr = mList.iterator();
        StoreInfo ele;
        while (itr.hasNext()) {
            ele = itr.next();
            new DownloadBooksInfoTask(this, mSearchButton, mProgressBar)
                    .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, searchText, ele, mData, 100 / mList.size());
        }
    }

    @Override
    public int getCount() {
        return mData.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public Object getItem(int i) {
        return mData.get(i);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        BookListListViewAdapter.ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.search_book_list_item, parent, false);

            holder = new BookListListViewAdapter.ViewHolder();
            holder.titleText = convertView.findViewById(R.id.BookName);
            holder.authorText = convertView.findViewById(R.id.BookAuthor);
            holder.priceText = convertView.findViewById(R.id.BookPrice);
            holder.thumbnailPicture = convertView.findViewById(R.id.BookImage);

            convertView.setTag(holder);
        } else {
            holder = (BookListListViewAdapter.ViewHolder) convertView.getTag();
        }

        // Read next page if we have last entry and something to read
        if (position == getCount() - 1 && pageNumber != -1 && mSearchButton.isEnabled()) {

            //TODO: reading list of engines from settings

            pageNumber++;
            sitesWithContent = 0;
            sitesProcessed = 0;

            mSearchButton.setEnabled(false);
            mSearchTextView.setEnabled(false);

            Iterator<StoreInfo> itr = mList.iterator();
            StoreInfo ele;
            while (itr.hasNext()) {
                ele = itr.next();
                new DownloadBooksInfoTask(this, mSearchButton, mProgressBar)
                        .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,
                                mSearchText,
                                ele,
                                mData,
                                100 / mList.size());
            }
        }

        Book b = ((Books) getItem(position)).items.get(((Books) getItem(position)).positionInMainList);

        holder.position = position;
        holder.titleText.setText(b.volumeInfo.title);
        String s = "";
        if (b.downloadUrl.contains(".epub") && ((Books) getItem(position)).items.size() == 1) {
            s = "Pobierz";
        } else {
            if (b.price != 0.0) {
                s = ((Books) getItem(position)).items.size() != 1 ?
                        "Od " + b.price + " PLN" : b.price + " PLN";
            } else {
                s = ((Books) getItem(position)).items.size() != 1 ?
                        "Od 0.00 PLN" : "Darmowa";
            }
        }
        if (b.offerExpiryDate != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("d.M");
            s = s + " do " + dateFormat.format(b.offerExpiryDate);
        }
        holder.priceText.setText(s);
        if (b.volumeInfo.authors != null) {
            holder.authorText.setText(b.volumeInfo.authors[0]);
        }

        mImageCache.getImageFromCache(convertView.getContext(),
                b.volumeInfo.smallThumbnail, holder, position);

        return convertView;
    }


    private class DownloadBooksInfoTask extends AsyncTask<Object, Object, ArrayList<Books>> {
        private final BaseAdapter mAdapter;
        private final Button mSearchButton;
        private final ProgressBar mProgressBar;

        DownloadBooksInfoTask(BaseAdapter adapter, Button searchButton, ProgressBar progressBar) {
            mAdapter = adapter;
            mSearchButton = searchButton;
            mProgressBar = progressBar;

            mProgressBar.setProgress(0);
        }

        @Override
        protected ArrayList<Books> doInBackground(Object... params) {
            String nameToSearch = (String) params[0];
            StoreInfo ele = (StoreInfo) params[1];
            ArrayList<Books> allBooks = (ArrayList<Books>) params[2];
            Boolean siteWithContent = false;
            String[] urls = ele.getSearchUrl(nameToSearch, pageNumber);

            for (String singleURL : urls) {
                try {
                    StringBuilder content = Utils.getPageContent(singleURL);
                    if (!content.toString().isEmpty()) {
                        if (ele.doesItMatch(nameToSearch, singleURL, content, allBooks, lock)) {
                            siteWithContent = true;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }

                mProgressBar.setProgress(mProgressBar.getProgress() +
                        ((int) (params[3]) / urls.length));
            }
            if (siteWithContent) {
                sitesWithContent++;
            }
            sitesProcessed++;

            return allBooks;
        }

        protected void onPostExecute(ArrayList<Books> books) {
            if (sitesProcessed == mList.size()) {
                if (sitesWithContent == 0) {
                    pageNumber = -1;
                }
                mProgressBar.setProgress(0);
                mSearchButton.setEnabled(true);
                mSearchTextView.setEnabled(true);
                mAdapter.notifyDataSetChanged();
            }
        }
    }

}
