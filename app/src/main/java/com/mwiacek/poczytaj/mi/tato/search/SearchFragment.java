package com.mwiacek.poczytaj.mi.tato.search;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mwiacek.poczytaj.mi.tato.FragmentConfig;
import com.mwiacek.poczytaj.mi.tato.MainActivity;
import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.ViewPagerAdapter;
import com.mwiacek.poczytaj.mi.tato.search.storeinfo.StoreInfo;

import java.io.File;
import java.util.LinkedHashMap;

public class SearchFragment extends Fragment {
    private final ImageCache imageCache = new ImageCache(MainActivity.getContext());
    private final ViewPagerAdapter topPagerAdapter = MainActivity.getViewPagerAdapter();
    private FragmentConfig config = null;
    private ViewSwitcher viewSwitcher;
    private ManyBooksRecyclerViewAdapter manyBooksRecyclerViewAdapter;
    private SingleBookRecyclerViewAdapter singleBookRecyclerViewAdapter;
    private SingleBook lastClickedBook;
    private RecyclerView manyBooksRecyclerView;

    public SearchFragment() {
    }

    public void onBackPressed() {
        if (manyBooksRecyclerView.isShown()) System.exit(0);
        viewSwitcher.showPrevious();
    }

    public int getTabNum() {
        return config.fileNameTabNum;
    }

    private void download(ActivityResultLauncher<String> requestPermissionLauncher) {
        if (lastClickedBook.downloadUrl.length() == 0) {
            return;
        }
        if (lastClickedBook.downloadUrl.contains(".htm") || lastClickedBook.downloadUrl.contains("artrage.pl")) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lastClickedBook.downloadUrl)));
            return;
        }
        if (ActivityCompat.shouldShowRequestPermissionRationale(requireActivity(),
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            new AlertDialog.Builder(requireContext())
                    .setIcon(android.R.drawable.ic_dialog_info)
                    .setTitle("Poczytaj mi tato")
                    .setMessage("Potrzebne uprawnienie WRITE_EXTERNAL_STORAGE. Bez niego aplikacja " +
                            "musi otworzyć link w przeglądarce.")
                    .setPositiveButton("PRZYZNAJ", (dialog, which) -> {
                        requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
                        dialog.dismiss();
                    })
                    .setNegativeButton("W PRZEGLĄDARCE", (dialog, which) -> {
                        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(lastClickedBook.downloadUrl)));
                        dialog.dismiss();
                    }).show();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.config = ViewPagerAdapter.configs.get(this.getArguments().getInt("configNum"));

        View view = inflater.inflate(R.layout.search_fragment, container, false);
        viewSwitcher = view.findViewById(R.id.viewSwitcher);

        ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                result -> {
                    if (result) Utils.downloadFileWithDownloadManagerAfterGrantingPermission(
                            lastClickedBook.downloadUrl, lastClickedBook.title, requireContext());
                });

        /* Page with single book */
        RecyclerView singleBookRecyclerView = view.findViewById(R.id.bookListListView);
        singleBookRecyclerViewAdapter = new SingleBookRecyclerViewAdapter(imageCache, getContext(),
                position -> {
                    lastClickedBook = singleBookRecyclerViewAdapter.getItem(position);
                    download(requestPermissionLauncher);
                });
        singleBookRecyclerView.setAdapter(singleBookRecyclerViewAdapter);
        singleBookRecyclerView.setItemAnimator(null);
        singleBookRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext(),
                DividerItemDecoration.VERTICAL));

        /* Page with many books */
        SearchView searchView = view.findViewById(R.id.mySearch3);
        ProgressBar mProgressBar = view.findViewById(R.id.progressBar3);
        manyBooksRecyclerView = view.findViewById(R.id.booksListListView);
        Toolbar toolbar = view.findViewById(R.id.toolbar3);

        manyBooksRecyclerViewAdapter = new ManyBooksRecyclerViewAdapter(config, mProgressBar,
                imageCache, position -> {
            ManyBooks books = manyBooksRecyclerViewAdapter.getItem(position);
            SingleBook book = books.items.get(books.itemsPositionForManyBooksList);
            if (books.items.size() == 1) {
                lastClickedBook = book;
                download(requestPermissionLauncher);
                return;
            }
            singleBookRecyclerViewAdapter.refreshData(books.items);
            viewSwitcher.showNext();
        });
        manyBooksRecyclerView.setAdapter(manyBooksRecyclerViewAdapter);
        manyBooksRecyclerView.setItemAnimator(null);
        manyBooksRecyclerView.addItemDecoration(new DividerItemDecoration(requireContext(),
                DividerItemDecoration.VERTICAL));
        manyBooksRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (mProgressBar.getMax() != 0) return;
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (linearLayoutManager != null && linearLayoutManager.findLastCompletelyVisibleItemPosition() >
                        manyBooksRecyclerViewAdapter.getItemCount() - 5) {
                    NetworkInfo activeNetwork = ((ConnectivityManager) requireContext()
                            .getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
                    if (!(activeNetwork != null && activeNetwork.isConnectedOrConnecting())) {
                        Toast.makeText(requireContext(), "Potrzebny internet!", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    manyBooksRecyclerViewAdapter.makeSearch(false,
                            searchView.getQuery().toString());
                }
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                manyBooksRecyclerViewAdapter.clear();
                manyBooksRecyclerViewAdapter.makeSearch(true, query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        View closeButton = searchView.findViewById(androidx.appcompat.R.id.search_close_btn);
        closeButton.setOnClickListener(v -> {
            searchView.setQuery("", true);
            searchView.clearFocus();
        });

        toolbar.addMenuProvider(new MenuProvider() {
            private final LinkedHashMap<String, StoreInfo.StoreInfoTyp> hm = new LinkedHashMap<String,
                    StoreInfo.StoreInfoTyp>() {{
                put("artrage.pl/bookrage", StoreInfo.StoreInfoTyp.BOOKRAGE);
                put("ebooki.swiatczytnikow.pl", StoreInfo.StoreInfoTyp.EBOOKI_SWIAT_CZYTNIKOW);
                put("ibook.pl", StoreInfo.StoreInfoTyp.IBUK);
                put("upolujebooka.pl", StoreInfo.StoreInfoTyp.UPOLUJ_EBOOKA);
                put("wolnelektury.pl", StoreInfo.StoreInfoTyp.WOLNE_LEKTURY);
            }};

            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                int i = 0;
                int mainIndex = 0;
                //  menu.add(2, R.string.MENU_USE_TOR, mainIndex++, R.string.MENU_USE_TOR)
                //          .setCheckable(true).setChecked(config.useTOR);
                for (String s : hm.keySet()) {
                    menu.add(2, i++, mainIndex++, s).setCheckable(true).setChecked(
                            config.storeInfoForSearchFragment.contains(hm.get(s)));
                }
                menu.add(3, R.string.MENU_SHOW_SEARCH_TAB, mainIndex++, R.string.MENU_SHOW_SEARCH_TAB)
                        .setCheckable(true).setChecked(ViewPagerAdapter.isSearchTabAvailable());
                menu.add(4, R.string.MENU_WRITE_MAIL_TO_AUTHOR, mainIndex, R.string.MENU_WRITE_MAIL_TO_AUTHOR);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    menu.setGroupDividerEnabled(true);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.isCheckable()) {
                    menuItem.setChecked(!menuItem.isChecked());
                }
                for (String s : hm.keySet()) {
                    if (!menuItem.getTitle().equals(s)) continue;
                    if (menuItem.isChecked()) {
                        config.storeInfoForSearchFragment.add(hm.get(s));
                    } else {
                        config.storeInfoForSearchFragment.remove(hm.get(s));
                    }
                    break;
                }
                if (menuItem.getItemId() == R.string.MENU_SHOW_SEARCH_TAB) {
                    topPagerAdapter.deleteSearchTab();
                    return true;
                }
                if (menuItem.isCheckable()) {
                    config.saveToInternalStorage(getContext());
                    return true;
                }
                if (menuItem.getItemId() == R.string.MENU_WRITE_MAIL_TO_AUTHOR) {
                    Utils.contactMe(getContext());
                }
                return false;
            }
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
