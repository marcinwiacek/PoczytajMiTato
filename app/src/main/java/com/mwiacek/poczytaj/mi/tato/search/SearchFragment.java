package com.mwiacek.poczytaj.mi.tato.search;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import androidx.fragment.app.Fragment;

import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;

import java.io.File;
import java.util.ArrayList;

public class SearchFragment extends Fragment {

    private ArrayAdapter<String> adapter;
    private AutoCompleteTextView mSearchTextView;
    private BooksListListViewAdapter customAdapter;
    private BookListListViewAdapter customAdapter2;
    private Button mSearchButton;
    private ViewSwitcher mViewSwitcher;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);

        ImageCacheForListView mImageCache = new ImageCacheForListView(view.getContext());

        Button mBackButton = view.findViewById(R.id.backButton);
        ListView mBooksListListView = view.findViewById(R.id.booksListListView);
        ListView mBooksList2ListView = view.findViewById(R.id.bookListListView);
        ProgressBar mProgressBar = view.findViewById(R.id.progressBar);
        mSearchButton = view.findViewById(R.id.searchButton);
        mSearchTextView = view.findViewById(R.id.searchTextView);
        mViewSwitcher = view.findViewById(R.id.viewSwitcher);
        final TextView titleTextView = view.findViewById(R.id.titleTextView);

        customAdapter = new BooksListListViewAdapter(mImageCache,
                mSearchButton,
                mProgressBar, mSearchTextView);
        customAdapter2 = new BookListListViewAdapter(mImageCache);

        mBooksListListView.setAdapter(customAdapter);
        mBooksListListView.setOnItemClickListener((parent, view12, position, id) -> {
            Books books = (Books) parent.getAdapter().getItem(position);
            Book book = books.items.get(books.positionInMainList);
            if (books.items.size() != 1) {
                customAdapter2.BookListListViewAdapterDisplay(books.items);
                titleTextView.setText(book.volumeInfo.title);
                mViewSwitcher.showNext();
                return;
            }
            Utils.downloadFileWithDownloadManager(book.downloadUrl,
                    book.volumeInfo.title, getContext());
        });

        mSearchButton.setOnClickListener(v -> {
//                InputMethodManager imm = (InputMethodManager) view.getSystemService(Context.INPUT_METHOD_SERVICE);
            //              imm.hideSoftInputFromWindow(mSearchTextView.getWindowToken(), 0);

            mSearchButton.setEnabled(false);
            mSearchTextView.setEnabled(false);

            SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(view.getContext());
            ArrayList<String> l = new ArrayList<>();
            String s;
            l.add(mSearchTextView.getText().toString());
            for (int i = 0; i < 10; i++) {
                s = sharedPref.getString("history" + i, "");
                if (!s.isEmpty() && !s.equals(mSearchTextView.getText().toString())) {
                    l.add(s);
                }
            }
            if (l.size() > 10) {
                l.remove(0);
            }
            SharedPreferences.Editor editor = sharedPref.edit();
            for (int i = 0; i < l.size(); i++) {
                editor.putString("history" + i, l.get(i));
            }
            editor.commit();
            adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, l);
            mSearchTextView.setAdapter(adapter);

            customAdapter.BooksListListViewAdapterSearch(
                    mSearchTextView.getText().toString(),
                    getContext(),
                    sharedPref);
        });

        mBooksList2ListView.setAdapter(customAdapter2);
        mBooksList2ListView.setOnItemClickListener((parent, view13, position, id) -> {
            Book book = (Book) parent.getAdapter().getItem(position);

            Utils.downloadFileWithDownloadManager(book.downloadUrl,
                    book.volumeInfo.title, getContext());
        });

        mBackButton.setOnClickListener(v -> mViewSwitcher.showPrevious());

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getContext());
        ArrayList<String> l = new ArrayList<>();
        String s;
        for (int i = 0; i < 10; i++) {
            s = sharedPref.getString("history" + i, "");
            if (!s.isEmpty()) {
                l.add(s);
            }
        }
        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, l);
        mSearchTextView.setAdapter(adapter);
        mSearchTextView.setThreshold(0);

        // mSearchTextView.setText("warszawo naprzód");
        // mSearchButton.callOnClick();

        mSearchTextView.setOnTouchListener((paramView, paramMotionEvent) -> {
            adapter.getFilter().filter(null);
            mSearchTextView.showDropDown();
            return false;
        });

        mSearchTextView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_ENTER) {
                mSearchTextView.dismissDropDown();
                mSearchButton.callOnClick();
                return true;
            }
            return false;
        });

        mSearchTextView.setOnItemClickListener(
                (parent, view1, position, id) -> mSearchButton.callOnClick());

        mSearchTextView.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                adapter.getFilter().filter(null);
                if (mSearchTextView.isShown()) {
                    mSearchTextView.showDropDown();
                }
                mSearchButton.setEnabled(mSearchTextView.length() != 0);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        Runnable r = () -> {
            while (true) {
                File dirList = new File(Utils.getDiskCacheFolder(getContext()));
                File[] files = dirList.listFiles();
                if (files.length < 500) {
                    break;
                }
                long lmod = Long.MAX_VALUE;
                File f = null;
                for (File file : files) {
                    if (file.lastModified() < lmod) {
                        lmod = file.lastModified();
                        f = file;
                    }
                }
                if (lmod != Long.MAX_VALUE) {
                    f.delete();
                }
            }
        };

        (new Thread(r)).start();

        return view;
    }


}
