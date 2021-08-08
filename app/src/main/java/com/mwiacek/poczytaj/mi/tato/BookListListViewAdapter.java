package com.mwiacek.poczytaj.mi.tato;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

class BookListListViewAdapter extends BaseAdapter {
    private ArrayList<Book> mData = new ArrayList<>();
    private final ImageCacheForListView mImageCache;

    BookListListViewAdapter(ImageCacheForListView imageCache) {
        mImageCache = imageCache;
    }

    public void BookListListViewAdapterDisplay(ArrayList<Book> data) {
        mData = data;
        Collections.sort(mData, new Comparator<Book>() {
            public int compare(Book obj1, Book obj2) {
                if (obj1.downloadUrl.contains(".epub")) {
                    return -1;
                }
                return (int) (obj1.price * 100 - obj2.price * 100);
            }
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
        ImageCacheForListView.ViewHolder holder;

        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            convertView = inflater.inflate(R.layout.book_list_item, parent, false);

            holder = new ImageCacheForListView.ViewHolder();
            holder.titleText = convertView.findViewById(R.id.BookName);
            holder.authorText = convertView.findViewById(R.id.BookAuthor);
            holder.priceText = convertView.findViewById(R.id.BookPrice);
            holder.thumbnailPicture = convertView.findViewById(R.id.BookImage);

            convertView.setTag(holder);
        } else {
            holder = (ImageCacheForListView.ViewHolder) convertView.getTag();
        }

        Book b = ((Book) getItem(position));

        holder.position = position;
        holder.titleText.setText(b.volumeInfo.title);
        if (b.downloadUrl.contains(".epub")) {
            holder.priceText.setText("Pobierz");
        } else {
            holder.priceText.setText(b.price != 0.0 ?
                    String.valueOf(b.price) + " PLN" : "Darmowa");
        }
        if (b.volumeInfo.authors != null) {
            holder.authorText.setText(b.volumeInfo.authors[0]);
        }

        mImageCache.getImageFromCache(convertView.getContext(),
                b.volumeInfo.smallThumbnail, holder, position);

        return convertView;
    }
}
