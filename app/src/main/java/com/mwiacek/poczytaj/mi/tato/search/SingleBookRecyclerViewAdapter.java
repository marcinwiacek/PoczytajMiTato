package com.mwiacek.poczytaj.mi.tato.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;

public class SingleBookRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final ImageCache mImageCache;
    private final Utils.OnItemClicked mOnClick;
    private final Context context;
    private ArrayList<SingleBook> mData = new ArrayList<>();

    public SingleBookRecyclerViewAdapter(ImageCache imageCache, Context context,
                                         Utils.OnItemClicked mOnClick) {
        this.mImageCache = imageCache;
        this.context = context;
        this.mOnClick = mOnClick;
    }

    SingleBook getItem(int position) {
        return mData.get(position);
    }

    @SuppressLint("NotifyDataSetChanged")
    public void refreshData(ArrayList<SingleBook> list) {
        this.mData = list;
        Collections.sort(mData, (obj1, obj2) -> {
            if (obj1.downloadUrl.contains(".epub")) return -1;
            return (int) (obj1.price * 100 - obj2.price * 100);
        });
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Utils.ItemViewHolder(LayoutInflater.from(parent.getContext()).inflate(
                R.layout.search_book_list_item, parent, false), mOnClick);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
        //if (viewHolder instanceof Utils.ItemViewHolder) {
        populateItemRows((Utils.ItemViewHolder) viewHolder, position);
        //}
    }

    @Override
    public int getItemCount() {
        return mData.size();
    }

    @Override
    public int getItemViewType(int position) {
        return 0;
    }

    private void populateItemRows(Utils.ItemViewHolder viewHolder, int position) {
        SingleBook book = mData.get(position);
        viewHolder.titleText.setText(book.title);
        String s = book.downloadUrl.contains(".epub") ? "Pobierz" :
                (book.price != 0.0 ? book.price + " PLN" : "Darmowa");
        if (book.offerExpiryDate != null) {
            @SuppressLint("SimpleDateFormat") SimpleDateFormat dateFormat =
                    new SimpleDateFormat("d.M");
            s = s + " do " + dateFormat.format(book.offerExpiryDate);
        }
        viewHolder.priceText.setText(s);
        if (book.authors != null) viewHolder.authorText.setText(book.authors[0]);
        mImageCache.getImageFromCache(context, book.smallThumbnailUrl, viewHolder, position);
    }

}

