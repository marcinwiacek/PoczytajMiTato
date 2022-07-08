package com.mwiacek.poczytaj.mi.tato.search;

import android.os.Build;
import android.os.Bundle;
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

import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;

import com.mwiacek.poczytaj.mi.tato.FragmentConfig;
import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.File;
import java.util.LinkedHashMap;

public class SearchFragment extends Fragment {
    public final FragmentConfig config;

    private ArrayAdapter adapter;
    private AutoCompleteTextView mSearchTextView;
    private BooksListListViewAdapter customAdapter;
    private BookListListViewAdapter customAdapter2;
    private Button mSearchButton;
    private ViewSwitcher mViewSwitcher;

    public SearchFragment(FragmentConfig config) {
        this.config = config;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);

        ImageCacheForListView mImageCache = new ImageCacheForListView(view.getContext());

        Button mBackButton = view.findViewById(R.id.backButton);
        ListView mBooksListListView = view.findViewById(R.id.booksListListView);
        ListView mBooksList2ListView = view.findViewById(R.id.bookListListView);
        ProgressBar mProgressBar = view.findViewById(R.id.progressBar);
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        mSearchButton = view.findViewById(R.id.searchButton);
        mSearchTextView = view.findViewById(R.id.searchTextView);
        mViewSwitcher = view.findViewById(R.id.viewSwitcher);

        customAdapter = new BooksListListViewAdapter(mImageCache, mSearchButton,
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

            config.searchHistory.add(mSearchTextView.getText().toString());
            adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, config.searchHistory);
            mSearchTextView.setAdapter(adapter);

            customAdapter.BooksListListViewAdapterSearch(
                    mSearchTextView.getText().toString(), getContext(), config);
        });

        mBooksList2ListView.setAdapter(customAdapter2);
        mBooksList2ListView.setOnItemClickListener((parent, view13, position, id) -> {
            Book book = (Book) parent.getAdapter().getItem(position);

            Utils.downloadFileWithDownloadManager(book.downloadUrl,
                    book.volumeInfo.title, getContext());
        });

        mBackButton.setOnClickListener(v -> mViewSwitcher.showPrevious());

        adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, config.searchHistory);
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

        Button menu = view.findViewById(R.id.menuButton);
        menu.setOnClickListener(view1 -> {
            LinkedHashMap<String, StoreInfo.StoreInfoTyp> hm = new LinkedHashMap<String, StoreInfo.StoreInfoTyp>() {{
                put("artrage.pl/bookrage", StoreInfo.StoreInfoTyp.BOOKRAGE);
                put("ebooki.swiatczytnikow.pl", StoreInfo.StoreInfoTyp.EBOOKI_SWIAT_CZYTNIKOW);
                put("ibook.pl", StoreInfo.StoreInfoTyp.IBUK);
                put("upolujebooka.pl", StoreInfo.StoreInfoTyp.UPOLUJ_EBOOKA);
                put("wolnelektury.pl", StoreInfo.StoreInfoTyp.WOLNE_LEKTURY);
            }};
            int i = 0;
            int mainIndex = 0;
            PopupMenu popupMenu = new PopupMenu(getContext(), menu);
            popupMenu.getMenu().add(2, i++, mainIndex++, "Używaj TOR")
                    .setActionView(R.layout.checkbox_layout).setCheckable(true).setChecked(config.useTOR);
            for (String s : hm.keySet()) {
                popupMenu.getMenu().add(2, i++, mainIndex++, s).
                        setActionView(R.layout.checkbox_layout).setCheckable(true).setChecked(
                                config.storeInfoForSearchFragment.contains(hm.get(s)));
            }
            popupMenu.getMenu().add(3, i++, mainIndex++, "Klonuj zakładkę");
            popupMenu.getMenu().add(3, i++, mainIndex++, "Usuń zakładkę");
            popupMenu.getMenu().add(5, i++, mainIndex++, "Napisz maila do autora");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                popupMenu.getMenu().setGroupDividerEnabled(true);
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                for (String s : hm.keySet()) {
                    if (!item.getTitle().equals(s)) continue;
                    item.setChecked(!item.isChecked());
                    if (item.isChecked()) {
                        config.storeInfoForSearchFragment.add(hm.get(s));
                    } else {
                        config.storeInfoForSearchFragment.remove(hm.get(s));
                    }
                    config.saveToInternalStorage(getContext());
                    return true;
                }
                if (item.isCheckable()) {
                    item.setChecked(!item.isChecked());
                    config.saveToInternalStorage(getContext());
                    return true;
                }
                if (item.getTitle().equals("Napisz maila do autora")) {
                    Utils.contactMe(getContext());
                }
                return false;
            });
            popupMenu.show();
        });

        return view;
    }


}
