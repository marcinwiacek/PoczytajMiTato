package com.mwiacek.poczytaj.mi.tato.search;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.mwiacek.poczytaj.mi.tato.R;

import java.util.ArrayList;
import java.util.Collections;

public class BookListListViewAdapter extends BaseAdapter {
    private final ImageCacheForListView mImageCache;
    private ArrayList<Book> mData = new ArrayList<>();

    public BookListListViewAdapter(ImageCacheForListView imageCache) {
        mImageCache = imageCache;
    }

    public void BookListListViewAdapterDisplay(ArrayList<Book> data) {
        mData = data;
        Collections.sort(mData, (obj1, obj2) -> {
            if (obj1.downloadUrl.contains(".epub")) return -1;
            return (int) (obj1.price * 100 - obj2.price * 100);
        });
        notifyDataSetChanged();
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
        ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.search_book_list_item, parent, false);

            holder = new ViewHolder();
            holder.titleText = convertView.findViewById(R.id.BookName);
            holder.authorText = convertView.findViewById(R.id.BookAuthor);
            holder.priceText = convertView.findViewById(R.id.BookPrice);
            holder.thumbnailPicture = convertView.findViewById(R.id.BookImage);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        Book b = ((Book) getItem(position));

        holder.position = position;
        holder.titleText.setText(b.volumeInfo.title);
        if (b.downloadUrl.contains(".epub")) {
            holder.priceText.setText("Pobierz");
        } else {
            holder.priceText.setText(b.price != 0.0 ?
                    b.price + " PLN" : "Darmowa");
        }
        if (b.volumeInfo.authors != null) {
            holder.authorText.setText(b.volumeInfo.authors[0]);
        }

        mImageCache.getImageFromCache(convertView.getContext(),
                b.volumeInfo.smallThumbnail, holder, position);

        return convertView;
    }

    public static class ViewHolder {
        public int position;
        public TextView titleText;
        public TextView authorText;
        public ImageView thumbnailPicture;
        public TextView priceText;
    }
}
