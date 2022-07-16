package com.mwiacek.poczytaj.mi.tato.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mwiacek.poczytaj.mi.tato.FragmentConfig;
import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.ViewPagerAdapter;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.File;
import java.util.LinkedHashMap;

public class SearchFragment extends Fragment {
    private final FragmentConfig config;
    private final ImageCache imageCache;
    private final ViewPagerAdapter topPagerAdapter;
    private ManyBooksRecyclerViewAdapter manyBooksRecyclerViewAdapter;
    private SingleBookRecyclerViewAdapter singleBookRecyclerViewAdapter;
    private ArrayAdapter searchTextAdapter;
    private ViewSwitcher viewSwitcher;
    private boolean isLoadingMoreBooks = false;

    public SearchFragment(FragmentConfig config, ImageCache imageCache, ViewPagerAdapter topPagerAdapter) {
        this.config = config;
        this.imageCache = imageCache;
        this.topPagerAdapter = topPagerAdapter;
    }

    public void onBackPressed() {
        System.exit(0);
    }

    public int getTabNum() {
        return config.fileNameTabNum;
    }

    private void loadMoreBooks(Context context) {
        NetworkInfo activeNetwork = ((ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        boolean isConnected = activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        if (!isConnected) {
            Toast.makeText(context, "Potrzebny internet!", Toast.LENGTH_SHORT).show();
            return;
        }
        // rowsArrayList.add(null);
        // manyBooksRecyclerView.post(() -> manyBooksRecyclerViewAdapter.notifyItemInserted(rowsArrayList.size() - 1));

        new Handler().postDelayed(() -> {
            //    rowsArrayList.remove(rowsArrayList.size() - 1);
            //     int scrollPosition = rowsArrayList.size();
            //     manyBooksRecyclerViewAdapter.notifyItemRemoved(scrollPosition);
            manyBooksRecyclerViewAdapter.makeSearch(true);
            //  manyBooksRecyclerViewAdapter.notifyDataSetChanged();
            isLoadingMoreBooks = false;
        }, 1000);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.search_fragment, container, false);
        viewSwitcher = view.findViewById(R.id.viewSwitcher);

        /* Page with single book */
        TextView titleTextView = view.findViewById(R.id.titleTextView);
        RecyclerView singleBookRecyclerView = view.findViewById(R.id.bookListListView);
        Button mBackButton = view.findViewById(R.id.backButton);

        singleBookRecyclerViewAdapter = new SingleBookRecyclerViewAdapter(imageCache, getContext(),
                position -> {
                    SingleBook singleBook = singleBookRecyclerViewAdapter.getItem(position);
                    Utils.downloadFileWithDownloadManager(singleBook.downloadUrl,
                            singleBook.title, getContext());
                });
        singleBookRecyclerView.setAdapter(singleBookRecyclerViewAdapter);
        singleBookRecyclerView.setItemAnimator(null);

        mBackButton.setOnClickListener(v -> viewSwitcher.showPrevious());

        /* Page with many books */
        Button searchButton = view.findViewById(R.id.searchButton);
        AutoCompleteTextView searchTextView = view.findViewById(R.id.searchTextView);
        ProgressBar mProgressBar = view.findViewById(R.id.progressBar);
        RecyclerView manyBooksRecyclerView = view.findViewById(R.id.booksListListView);
        Button menu = view.findViewById(R.id.menuButton);

        manyBooksRecyclerViewAdapter = new ManyBooksRecyclerViewAdapter(config, searchButton,
                searchTextView, mProgressBar, imageCache, position -> {
            ManyBooks books = manyBooksRecyclerViewAdapter.getItem(position);
            SingleBook book = books.items.get(books.itemsPositionForManyBooksList);
            if (books.items.size() == 1) {
                Utils.downloadFileWithDownloadManager(book.downloadUrl,
                        book.title, getContext());
                return;
            }
            singleBookRecyclerViewAdapter.refreshData(books.items);
            titleTextView.setText(book.title);
            viewSwitcher.showNext();
        });
        manyBooksRecyclerView.setAdapter(manyBooksRecyclerViewAdapter);
        manyBooksRecyclerView.setItemAnimator(null);
        manyBooksRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (isLoadingMoreBooks) return;
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (linearLayoutManager != null &&
                        linearLayoutManager.findLastCompletelyVisibleItemPosition() >
                                manyBooksRecyclerViewAdapter.getItemCount() - 7) {
                    isLoadingMoreBooks = true;
                    loadMoreBooks(requireContext());
                }
            }
        });

        searchButton.setOnClickListener(v -> {
//          InputMethodManager imm = (InputMethodManager) view.getSystemService(Context.INPUT_METHOD_SERVICE);
//          imm.hideSoftInputFromWindow(mSearchTextView.getWindowToken(), 0);

            searchButton.setEnabled(false);
            searchTextView.setEnabled(false);
            config.searchHistory.add(searchTextView.getText().toString());
            searchTextAdapter = new ArrayAdapter<>(getActivity(),
                    android.R.layout.simple_dropdown_item_1line, config.searchHistory);
            searchTextView.setAdapter(searchTextAdapter);
            manyBooksRecyclerViewAdapter.makeSearch(false);
        });

        // mSearchTextView.setText("warszawo naprzód");
        // mSearchButton.callOnClick();

        searchTextAdapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_dropdown_item_1line, config.searchHistory);
        searchTextView.setAdapter(searchTextAdapter);
        searchTextView.setThreshold(0);
        searchTextView.setOnTouchListener((paramView, paramMotionEvent) -> {
            searchTextAdapter.getFilter().filter(null);
            searchTextView.showDropDown();
            return false;
        });
        searchTextView.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN &&
                    keyCode == KeyEvent.KEYCODE_ENTER) {
                searchTextView.dismissDropDown();
                searchButton.callOnClick();
                return true;
            }
            return false;
        });
        searchTextView.setOnItemClickListener(
                (parent, view1, position, id) -> searchButton.callOnClick());
        searchTextView.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                searchTextAdapter.getFilter().filter(null);
                if (searchTextView.isShown()) searchTextView.showDropDown();
                searchButton.setEnabled(searchTextView.length() != 0);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        menu.setOnClickListener(view1 -> {
            LinkedHashMap<String, StoreInfo.StoreInfoTyp> hm = new LinkedHashMap<String,
                    StoreInfo.StoreInfoTyp>() {{
                put("artrage.pl/bookrage", StoreInfo.StoreInfoTyp.BOOKRAGE);
                put("ebooki.swiatczytnikow.pl", StoreInfo.StoreInfoTyp.EBOOKI_SWIAT_CZYTNIKOW);
                put("ibook.pl", StoreInfo.StoreInfoTyp.IBUK);
                put("upolujebooka.pl", StoreInfo.StoreInfoTyp.UPOLUJ_EBOOKA);
                put("wolnelektury.pl", StoreInfo.StoreInfoTyp.WOLNE_LEKTURY);
            }};
            int i = 0;
            int mainIndex = 0;
            PopupMenu popupMenu = new PopupMenu(requireContext(), menu);
            popupMenu.getMenu().add(2, i++, mainIndex++, R.string.MENU_USE_TOR)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setChecked(config.useTOR);
            for (String s : hm.keySet()) {
                popupMenu.getMenu().add(2, i++, mainIndex++, s).
                        setActionView(R.layout.checkbox_menu_item).setCheckable(true).setChecked(
                                config.storeInfoForSearchFragment.contains(hm.get(s)));
            }
            popupMenu.getMenu().add(3, i++, mainIndex++, R.string.MENU_SHOW_SEARCH_TAB)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setChecked(ViewPagerAdapter.isSearchTabAvailable());
            popupMenu.getMenu().add(4, i, mainIndex, R.string.MENU_WRITE_MAIL_TO_AUTHOR);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                popupMenu.getMenu().setGroupDividerEnabled(true);
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.isCheckable()) {
                    item.setChecked(!item.isChecked());
                }
                for (String s : hm.keySet()) {
                    if (!item.getTitle().equals(s)) continue;
                    if (item.isChecked()) {
                        config.storeInfoForSearchFragment.add(hm.get(s));
                    } else {
                        config.storeInfoForSearchFragment.remove(hm.get(s));
                    }
                    break;
                }
                if (item.getTitle().equals(getString(R.string.MENU_SHOW_SEARCH_TAB))) {
                    topPagerAdapter.deleteSearchTab();
                    return true;
                }
                if (item.isCheckable()) {
                    config.saveToInternalStorage(getContext());
                    return true;
                }
                if (item.getTitle().equals(getString(R.string.MENU_WRITE_MAIL_TO_AUTHOR))) {
                    Utils.contactMe(getContext());
                }
                return false;
            });
            popupMenu.show();
        });

        /* Delete files if we have more than 500 */
        (new Thread(() -> {
            while (true) {
                File[] files = new File(Utils.getDiskCacheFolder(requireContext())).listFiles();
                if (files == null || files.length < 500) break;
                long lastModified = Long.MAX_VALUE;
                File f = null;
                for (File file : files) {
                    if (file.lastModified() < lastModified) {
                        lastModified = file.lastModified();
                        f = file;
                    }
                }
                if (lastModified != Long.MAX_VALUE) f.delete();
            }
        })).start();

        return view;
    }


}
