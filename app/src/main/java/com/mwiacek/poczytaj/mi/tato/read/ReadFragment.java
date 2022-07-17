package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
import android.content.res.Configuration;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ViewSwitcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.os.HandlerCompat;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewFeature;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.mwiacek.poczytaj.mi.tato.FragmentConfig;
import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.ViewPagerAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ReadFragment extends Fragment {
    private final static String MIME_TYPE = "text/html; charset=UTF-8";
    private final static String ENCODING = "UTF-8";

    private final FragmentConfig config;
    private final DBHelper db;
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    private final ViewPagerAdapter topPagerAdapter;
    private RecyclerView pageList;
    private PageListRecyclerViewAdapter pageListAdapter;
    private SwipeRefreshLayout refresh;
    private ViewSwitcher viewSwitcher;
    private WebView webView;
    private FrameLayout frameLayout;
    private ActivityResultLauncher<String> mCreateEPUB;

    private int positionInPageList;

    public ReadFragment(FragmentConfig config, ViewPagerAdapter topPagerAdapter, DBHelper mydb) {
        this.topPagerAdapter = topPagerAdapter;
        this.config = config;
        this.db = mydb;
    }

    public int getTabNum() {
        return config.fileNameTabNum;
    }

    public void notifyAboutUpdates(String url) {
        pageListAdapter.notifyAboutUpdates(url);
    }

    public void notifyAboutUpdates(Page.PageTyp typ) {
        if (config.readInfoForReadFragment.contains(typ)) {
            pageListAdapter.update(db, config.showHiddenTexts,
                    config.readInfoForReadFragment.iterator(), config.authorFilter,
                    config.tagFilter);
        }
    }

    public void setupRefresh() {
        WorkManager.getInstance(requireContext())
                .cancelAllWorkByTag("poczytajmitato" + config.fileNameTabNum);
        if (config.howOftenRefreshTabInHours == -1) return;
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(config.doNotDownloadWithLowBattery)
                .setRequiresCharging(config.downloadDuringChargingOnly)
                //      .setRequiredNetworkType()
                //        .setRequiredNetworkType(NetworkType.UNMETERED)
                .build();
        WorkRequest request = new PeriodicWorkRequest.Builder(UploadWorker.class,
                config.howOftenRefreshTabInHours, TimeUnit.HOURS)
                .setBackoffCriteria(BackoffPolicy.LINEAR,
                        config.howOftenTryToRefreshTabAfterErrorInMinutes,
                        TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putInt("TabNum", config.fileNameTabNum).build())
                .addTag("poczytajmitato" + config.fileNameTabNum)
                .build();

        WorkManager.getInstance(requireContext()).enqueue(request);
    }

    public void updateLayout(boolean withMargin) {
        final TypedArray styledAttributes = requireContext().getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize});
        int mActionBarSize = (int) styledAttributes.getDimension(0, 0);
        pageList.setPadding(0, 0, 0, withMargin ? mActionBarSize : 0);
        frameLayout.setPadding(0, 0, 0, withMargin ? mActionBarSize : 0);
    }

    public void onBackPressed() {
        if (pageList.isShown()) System.exit(0);
        db.setPageTop(pageListAdapter.getItem(positionInPageList).url, webView.getScrollY());
        viewSwitcher.showPrevious();
        webView.loadDataWithBaseURL(null, "",
                MIME_TYPE, ENCODING, null);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.read_fragment, container, false);
        viewSwitcher = view.findViewById(R.id.viewSwitcher2);

        /* Page with webview */
        SwipeRefreshLayout refresh2 = view.findViewById(R.id.swiperefresh2);
        frameLayout = view.findViewById(R.id.frameLayout);
        webView = view.findViewById(R.id.webview);

        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                webView.scrollTo(0, db.getPageTop(pageListAdapter.getItem(positionInPageList).url));
            }
        });
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK) &&
                ((requireContext().getResources().getConfiguration().uiMode &
                        Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)) {
            WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
        }

        refresh2.setOnRefreshListener(() -> {
            Page p = pageListAdapter.getItem(positionInPageList);
            File f = p.getCacheFileName(getContext());
            Utils.getPage(p.url,
                    result -> {
                        f.delete();
                        webView.loadDataWithBaseURL(null,
                                Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f),
                                MIME_TYPE, ENCODING, null);
                    }, mainThreadHandler, threadPoolExecutor);

            refresh2.setRefreshing(false);
        });

        /* Page with list */
        SearchView searchView = view.findViewById(R.id.mySearch);
        refresh = view.findViewById(R.id.swiperefresh);
        pageList = view.findViewById(R.id.pagesRecyclerView);
        Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.addMenuProvider(new MenuProvider() {
            private final LinkedHashMap<String, Page.PageTyp> hm = new LinkedHashMap<String, Page.PageTyp>() {{
                put("fantastyka.pl, archiwum", Page.PageTyp.FANTASTYKA_ARCHIWUM);
                put("fantastyka.pl, biblioteka", Page.PageTyp.FANTASTYKA_BIBLIOTEKA);
                put("fantastyka.pl, poczekalnia", Page.PageTyp.FANTASTYKA_POCZEKALNIA);
                put("opowi.pl, fantastyka", Page.PageTyp.OPOWI_FANTASTYKA);
            }};

            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                int i = 0;
                int mainIndex = 0;

                menu.add(0, R.string.MENU_SHOW_HIDDEN, mainIndex++, R.string.MENU_SHOW_HIDDEN)
                        .setCheckable(true).setChecked(config.showHiddenTexts == FragmentConfig.HiddenTexts.HIDDEN);
                menu.add(1, R.string.MENU_IMPORT_EPUB, mainIndex++, R.string.MENU_IMPORT_EPUB);
                menu.add(1, R.string.MENU_EXPORT_EPUB, mainIndex++, R.string.MENU_EXPORT_EPUB);
                menu.add(1, R.string.MENU_GET_UNREAD_TEXTS, mainIndex++, R.string.MENU_GET_UNREAD_TEXTS);
                menu.add(1, R.string.MENU_GET_ALL_TEXTS, mainIndex++, R.string.MENU_GET_ALL_TEXTS);
                menu.add(1, R.string.MENU_GET_ALL_INDEX_PAGES, mainIndex++, R.string.MENU_GET_ALL_INDEX_PAGES);
                menu.add(2, R.string.MENU_CLONE_TAB, mainIndex++, R.string.MENU_CLONE_TAB);
                menu.add(2, R.string.MENU_DELETE_TAB, mainIndex++, R.string.MENU_DELETE_TAB)
                        .setEnabled(ViewPagerAdapter.areMultipleReadTabsAvailable());
                menu.add(2, R.string.MENU_CHANGE_TAB_NAME, mainIndex++, R.string.MENU_CHANGE_TAB_NAME);
                menu.add(2, R.string.MENU_SHOW_SEARCH_TAB, mainIndex++, R.string.MENU_SHOW_SEARCH_TAB)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setChecked(ViewPagerAdapter.isSearchTabAvailable());
                menu.add(3, R.string.MENU_USE_TOR, mainIndex++, R.string.MENU_USE_TOR)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setChecked(config.useTOR);
                menu.add(3, R.string.MENU_GET_TEXTS_WITH_INDEX, mainIndex++, R.string.MENU_GET_TEXTS_WITH_INDEX)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setChecked(config.getTextsWhenRefreshingIndex);
                for (String s : hm.keySet()) {
                    menu.add(3, i++, mainIndex++, s).
                            setActionView(R.layout.checkbox_menu_item).setCheckable(true).setChecked(
                                    config.readInfoForReadFragment.contains(hm.get(s)));
                }
                menu.add(3, R.string.MENU_LOCAL_AUTHOR_FILTER, mainIndex++, R.string.MENU_LOCAL_AUTHOR_FILTER)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setChecked(!config.authorFilter.isEmpty());
                menu.add(3, R.string.MENU_LOCAL_TAG_FILTER, mainIndex++, R.string.MENU_LOCAL_TAG_FILTER)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setChecked(!config.tagFilter.isEmpty());
                menu.add(4, i++, mainIndex++,
                                "Pobierz co " + (config.howOftenRefreshTabInHours == -1 ?
                                        "x" : config.howOftenRefreshTabInHours) + " godzin")
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setChecked(config.howOftenRefreshTabInHours != -1);
                menu.add(4, i++, mainIndex++,
                                "Przy błędzie co " +
                                        (config.howOftenTryToRefreshTabAfterErrorInMinutes == -1 ?
                                                "x" : config.howOftenTryToRefreshTabAfterErrorInMinutes) +
                                        " minut").setActionView(R.layout.checkbox_menu_item)
                        .setCheckable(true).setChecked(config.howOftenTryToRefreshTabAfterErrorInMinutes != -1)
                        .setEnabled(config.howOftenRefreshTabInHours != -1);
                menu.add(4, R.string.MENU_DOWNLOAD_ON_WIFI, mainIndex++, R.string.MENU_DOWNLOAD_ON_WIFI)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.downloadWithWifi);
                menu.add(4, R.string.MENU_DOWNLOAD_ON_GSM, mainIndex++, R.string.MENU_DOWNLOAD_ON_GSM)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.downloadWithGSM);
                menu.add(4, R.string.MENU_DOWNLOAD_ON_OTHER_NET, mainIndex++, R.string.MENU_DOWNLOAD_ON_OTHER_NET)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.downloadWithOtherNet);
                menu.add(4, R.string.MENU_DOWNLOAD_ONLY_ON_CHARGING, mainIndex++, R.string.MENU_DOWNLOAD_ONLY_ON_CHARGING)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.downloadDuringChargingOnly);
                menu.add(4, R.string.MENU_DONT_DOWNLOAD_WITH_LOW_BATTERY, mainIndex++, R.string.MENU_DONT_DOWNLOAD_WITH_LOW_BATTERY)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.doNotDownloadWithLowBattery);
                menu.add(4, R.string.MENU_NETWORK_WITHOUT_LIMIT, mainIndex++, R.string.MENU_NETWORK_WITHOUT_LIMIT)
                        .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                        .setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.networkWithoutLimit);
                menu.add(5, R.string.MENU_WRITE_MAIL_TO_AUTHOR, mainIndex, R.string.MENU_WRITE_MAIL_TO_AUTHOR);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    menu.setGroupDividerEnabled(true);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.string.MENU_LOCAL_AUTHOR_FILTER) {
                    EditText input = new EditText(getContext());
                    input.setText(config.authorFilter);
                    Utils.dialog(requireContext(),
                            "Podaj autorów (oddzielonych przecinkiem). " +
                                    "\"not \" na początku autora oznacza zaprzeczenie.", input,
                            (dialog, which) -> {
                                menuItem.setChecked(input.getText().toString().isEmpty());
                                if (!config.authorFilter.equals(input.getText().toString().trim())) {
                                    config.authorFilter = input.getText().toString().trim();
                                    pageListAdapter.update(db, config.showHiddenTexts,
                                            config.readInfoForReadFragment.iterator(),
                                            config.authorFilter, config.tagFilter);
                                    config.saveToInternalStorage(getContext());
                                }
                            }, (dialog, which) -> {
                                dialog.dismiss();
                                if (!config.tagFilter.equals(input.getText().toString().trim())) {
                                    config.tagFilter = input.getText().toString().trim();
                                    pageListAdapter.update(db, config.showHiddenTexts,
                                            config.readInfoForReadFragment.iterator(),
                                            config.authorFilter, config.tagFilter);
                                    config.saveToInternalStorage(getContext());
                                }
                                config.saveToInternalStorage(getContext());
                            });
                    return false;
                } else if (menuItem.getItemId() == R.string.MENU_LOCAL_TAG_FILTER) {
                    EditText input = new EditText(getContext());
                    input.setText(config.tagFilter);
                    Utils.dialog(requireContext(),
                            "Podaj tagi (oddzielone przecinkiem). " +
                                    "\"not \" na początku taga oznacza zaprzeczenie.", input,
                            (dialog, which) -> {
                                menuItem.setChecked(input.getText().toString().isEmpty());
                                if (!config.tagFilter.equals(input.getText().toString().trim())) {
                                    config.tagFilter = input.getText().toString().trim();
                                    pageListAdapter.update(db, config.showHiddenTexts,
                                            config.readInfoForReadFragment.iterator(),
                                            config.authorFilter, config.tagFilter);
                                    config.saveToInternalStorage(getContext());
                                }
                            }, (dialog, which) -> {
                                dialog.dismiss();
                                menuItem.setChecked(false);
                                if (!config.tagFilter.equals(input.getText().toString().trim())) {
                                    config.tagFilter = input.getText().toString().trim();
                                    pageListAdapter.update(db, config.showHiddenTexts,
                                            config.readInfoForReadFragment.iterator(),
                                            config.authorFilter, config.tagFilter);
                                    config.saveToInternalStorage(getContext());
                                }
                                config.saveToInternalStorage(getContext());
                            });
                    return false;
                }
                if (menuItem.isCheckable()) {
                    menuItem.setChecked(!menuItem.isChecked());
                }
                for (String s : hm.keySet()) {
                    if (!menuItem.getTitle().equals(s)) continue;
                    if (menuItem.isChecked()) {
                        config.readInfoForReadFragment.add(hm.get(s));
                    } else {
                        config.readInfoForReadFragment.remove(hm.get(s));
                    }
                    pageListAdapter.update(db, config.showHiddenTexts,
                            config.readInfoForReadFragment.iterator(), config.authorFilter,
                            config.tagFilter);
                    break;
                }
                if (menuItem.getItemId() == R.string.MENU_SHOW_HIDDEN) {
                    config.showHiddenTexts = menuItem.isChecked() ?
                            FragmentConfig.HiddenTexts.HIDDEN : FragmentConfig.HiddenTexts.NONE;
                    pageListAdapter.update(db, config.showHiddenTexts,
                            config.readInfoForReadFragment.iterator(), config.authorFilter,
                            config.tagFilter);
                } else if (menuItem.getItemId() == R.string.MENU_SHOW_SEARCH_TAB) {
                    if (menuItem.isChecked()) {
                        topPagerAdapter.addSearchTab();
                    } else {
                        topPagerAdapter.deleteSearchTab();
                    }
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_ON_WIFI) {
                    config.downloadWithWifi = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_ON_GSM) {
                    config.downloadWithGSM = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_ON_OTHER_NET) {
                    config.downloadWithOtherNet = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_ONLY_ON_CHARGING) {
                    config.downloadDuringChargingOnly = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DONT_DOWNLOAD_WITH_LOW_BATTERY) {
                    config.doNotDownloadWithLowBattery = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_NETWORK_WITHOUT_LIMIT) {
                    config.networkWithoutLimit = menuItem.isChecked();
                } else if (menuItem.getTitle().toString().startsWith("Pobierz co")) {
                    if (menuItem.isChecked()) {
                        EditText input = new EditText(getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        Utils.dialog(requireContext(),
                                "Co ile godzin zakładka ma być odświeżana (24 = 1 dzień, " +
                                        "48 = 2 dni, 168 = tydzień, etc.)", input,
                                (dialog, which) -> {
                                    config.howOftenRefreshTabInHours =
                                            Integer.parseInt(String.valueOf(input.getText()));
                                    setupRefresh();
                                    config.saveToInternalStorage(getContext());
                                }, (dialog, which) -> {
                                    dialog.dismiss();
                                    config.howOftenRefreshTabInHours = -1;
                                    setupRefresh();
                                    config.saveToInternalStorage(getContext());
                                });
                    } else {
                        config.howOftenRefreshTabInHours = -1;
                        setupRefresh();
                    }
                } else if (menuItem.getTitle().toString().startsWith("Przy błędzie co ")) {
                    if (menuItem.isChecked()) {
                        EditText input = new EditText(getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        Utils.dialog(requireContext(),
                                "Co ile minut próbować pobrać indeks po pierwszym niepowodzeniu",
                                input, (dialog, which) -> {
                                    config.howOftenTryToRefreshTabAfterErrorInMinutes =
                                            Integer.parseInt(String.valueOf(input.getText()));
                                    config.saveToInternalStorage(getContext());
                                }, (dialog, which) -> {
                                    dialog.dismiss();
                                    config.howOftenTryToRefreshTabAfterErrorInMinutes = -1;
                                    config.saveToInternalStorage(getContext());
                                });
                    } else {
                        config.howOftenTryToRefreshTabAfterErrorInMinutes = -1;
                    }
                }
                if (menuItem.isCheckable()) {
                    config.saveToInternalStorage(getContext());
                    return true;
                }
                if (menuItem.getItemId() == R.string.MENU_CLONE_TAB) {
                    EditText input = new EditText(getContext());
                    Utils.dialog(requireContext(), "Nazwa nowej zakładki", input,
                            (dialog, which) -> {
                                try {
                                    topPagerAdapter.addTab(config, input.getText().toString());
                                } catch (CloneNotSupportedException e) {
                                    e.printStackTrace();
                                }
                            }, (dialog, which) -> dialog.dismiss());
                } else if (menuItem.getItemId() == R.string.MENU_CHANGE_TAB_NAME) {
                    EditText input = new EditText(getContext());
                    input.setText(config.tabName);
                    Utils.dialog(requireContext(), "Nowa nazwa zakładki " +
                                    config.tabName, input,
                            (dialog, which) -> {
                                if (!input.getText().toString().equals(config.tabName)) {
                                    config.tabName = input.getText().toString();
                                    config.saveToInternalStorage(getContext());
                                    topPagerAdapter.notifyDataSetChanged();
                                }
                            }, (dialog, which) -> dialog.dismiss());
                } else if (menuItem.getItemId() == R.string.MENU_DELETE_TAB) {
                    Utils.dialog(requireContext(), "Czy chcesz usunąć zakładkę " +
                                    config.tabName + "?",
                            null, (dialog, which) -> topPagerAdapter.deleteTab(config),
                            (dialog, which) -> dialog.dismiss());
                } else if (menuItem.getItemId() == R.string.MENU_WRITE_MAIL_TO_AUTHOR) {
                    Utils.contactMe(getContext());
                } else if (menuItem.getItemId() == R.string.MENU_GET_UNREAD_TEXTS) {
                    for (Page p : pageListAdapter.getAllItems()) {
                        File f = p.getCacheFileName(getContext());
                        if (f.exists()) continue;
                        Utils.getPage(p.url, result -> {
                                    Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f);
                                    pageListAdapter.update(db, config.showHiddenTexts,
                                            config.readInfoForReadFragment.iterator(),
                                            config.authorFilter, config.tagFilter);
                                },
                                mainThreadHandler, threadPoolExecutor);
                    }
                } else if (menuItem.getItemId() == R.string.MENU_GET_ALL_TEXTS) {
                    for (Page p : pageListAdapter.getAllItems()) {
                        File f = p.getCacheFileName(getContext());
                        Utils.getPage(p.url, result -> {
                                    Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f);
                                    pageListAdapter.update(db, config.showHiddenTexts,
                                            config.readInfoForReadFragment.iterator(),
                                            config.authorFilter, config.tagFilter);
                                },
                                mainThreadHandler, threadPoolExecutor);
                    }
                } else if (menuItem.getItemId() == R.string.MENU_GET_ALL_INDEX_PAGES) {
                    for (Page.PageTyp pt : config.readInfoForReadFragment) {
                        refresh.setRefreshing(true);
                        Page.getReadInfo(pt).getList(
                                result -> pageListAdapter.update(db, config.showHiddenTexts,
                                        config.readInfoForReadFragment.iterator(),
                                        config.authorFilter, config.tagFilter),
                                result -> refresh.setRefreshing(false),
                                mainThreadHandler, threadPoolExecutor, db, getContext(), pt,
                                -1);
                    }
                } else if (menuItem.getItemId() == R.string.MENU_EXPORT_EPUB) {
                    mCreateEPUB.launch(config.tabName);
                }
                return false;
            }
        });

        pageListAdapter = new PageListRecyclerViewAdapter(requireContext());
        pageListAdapter.setOnClick(position -> {
            Page p = pageListAdapter.getItem(position);
            File f = p.getCacheFileName(requireContext());
            positionInPageList = position;
            if (f.exists()) {
                String s = Utils.readFile(f);
                if (!searchView.getQuery().toString().trim().isEmpty()) {
                    String[] toSearch = searchView.getQuery().toString().trim().split(" ");
                    for (String ts : toSearch) {
                        s = Utils.findText(s, ts.trim());
                    }
                }
                webView.loadDataWithBaseURL(null, s, MIME_TYPE, ENCODING, null);
            } else {
                webView.loadDataWithBaseURL(null, "Czytanie pliku " + p.url,
                        MIME_TYPE, ENCODING, null);
                Utils.getPage(p.url, result -> {
                            for (Fragment ff : getParentFragmentManager().getFragments()) {
                                if (ff instanceof ReadFragment) {
                                    ((ReadFragment) ff).notifyAboutUpdates(p.url);
                                }
                            }
                            String s = Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f);
                            if (!searchView.getQuery().toString().trim().isEmpty()) {
                                String[] toSearch = searchView.getQuery().toString().trim().split(" ");
                                for (String ts : toSearch) {
                                    s = Utils.findText(s, ts.trim());
                                }
                            }
                            webView.loadDataWithBaseURL(null, s, MIME_TYPE, ENCODING, null);
                        },
                        mainThreadHandler, threadPoolExecutor);
            }
            viewSwitcher.showNext();
        });
        pageList.setAdapter(pageListAdapter);
        pageList.addItemDecoration(new DividerItemDecoration(requireContext(),
                DividerItemDecoration.VERTICAL));
        pageListAdapter.update(db, config.showHiddenTexts, config.readInfoForReadFragment.iterator(),
                config.authorFilter, config.tagFilter);

        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                Page p = pageListAdapter
                        .getItem(viewHolder.getAbsoluteAdapterPosition());
                db.setPageHidden(p.url, config.showHiddenTexts == FragmentConfig.HiddenTexts.HIDDEN ?
                        FragmentConfig.HiddenTexts.NONE : FragmentConfig.HiddenTexts.HIDDEN);
                for (Fragment ff : getParentFragmentManager().getFragments()) {
                    if (ff instanceof ReadFragment) {
                        ((ReadFragment) ff).notifyAboutUpdates(p.typ);
                    }
                }
            }

            /**
             * Create color background during swipe
             */
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView,
                                    @NonNull RecyclerView.ViewHolder viewHolder,
                                    float dX, float dY, int actionState,
                                    boolean isCurrentlyActive) {
                if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    return;
                }
                Paint p = new Paint();
                p.setColor(Color.GREEN);
                if (dX > 0) {
                    c.drawRect(viewHolder.itemView.getLeft(), viewHolder.itemView.getTop(), dX,
                            viewHolder.itemView.getBottom(), p);
                } else {
                    c.drawRect(viewHolder.itemView.getRight() + dX, viewHolder.itemView.getTop(),
                            viewHolder.itemView.getRight(), viewHolder.itemView.getBottom(), p);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(pageList);

        refresh.setOnRefreshListener(() -> {
            for (Page.PageTyp pt : config.readInfoForReadFragment) {
                refresh.setRefreshing(true);
                Page.getReadInfo(pt).getList(
                        result -> {
                            for (Fragment ff : getParentFragmentManager().getFragments()) {
                                if (ff instanceof ReadFragment) {
                                    ((ReadFragment) ff).notifyAboutUpdates(pt);
                                }
                            }
                        },
                        result -> refresh.setRefreshing(false),
                        mainThreadHandler, threadPoolExecutor, db, getContext(), pt,
                        new DBHelper(getContext()).checkIfTypIsCompletelyRead(pt) ? 3 : -1);
            }
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ArrayList<Page> list = new ArrayList<>();
                String[] toSearch = query.trim().split(" ");
                for (Page p : db.getAllPages(config.showHiddenTexts,
                        config.readInfoForReadFragment.iterator(), config.authorFilter, config.tagFilter)) {
                    File f = p.getCacheFileName(getContext());
                    for (String ts : toSearch) {
                        if (Utils.contaisText(p.name, ts.trim()) || Utils.contaisText(p.tags, ts.trim()) ||
                                Utils.contaisText(p.author, ts.trim())) {
                            list.add(p);
                            f = null;
                            break;
                        }
                    }
                    if (f == null) continue;
                    if (!f.exists()) continue;
                    String s = Utils.readFile(f);
                    for (String ts : toSearch) {
                        if (Utils.contaisText(s, ts.trim())) {
                            list.add(p);
                            break;
                        }
                    }
                }
                pageListAdapter.update(list, toSearch);
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
            pageListAdapter.update(db, config.showHiddenTexts,
                    config.readInfoForReadFragment.iterator(),
                    config.authorFilter, config.tagFilter);
        });

        mCreateEPUB = registerForActivityResult(
                new ActivityResultContracts.CreateDocument("application/zip"), //epub+zip
                uri -> Utils.createEPUB(getContext(), uri, config.tabName,
                        pageListAdapter.getAllItems(),
                        config.readInfoForReadFragment));


        return view;
    }
}
