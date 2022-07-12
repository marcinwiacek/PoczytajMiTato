package com.mwiacek.poczytaj.mi.tato.read;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.os.HandlerCompat;
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

    private int positionInPageList;

    public ReadFragment(FragmentConfig config, ViewPagerAdapter topPagrAdapter, DBHelper mydb) {
        this.topPagerAdapter = topPagrAdapter;
        this.config = config;
        this.db = mydb;
    }

    public int getTabNum() {
        return config.tabNumForFileForSerialization;
    }

    public void setupRefresh() {
        WorkManager.getInstance(this.requireContext())
                .cancelAllWorkByTag("poczytajmitato" + config.tabNumForFileForSerialization);
        if (config.howOftenRefreshTabInHours == -1) return;
        Constraints constraints = new Constraints.Builder()
                //        .setRequiredNetworkType(NetworkType.UNMETERED)
//                .setRequiresBatteryNotLow(true)
                .build();
        WorkRequest request = new PeriodicWorkRequest.Builder(UploadWorker.class,
                16, TimeUnit.MINUTES)
                .setBackoffCriteria(BackoffPolicy.LINEAR, 5L, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putInt("TabNum", config.tabNumForFileForSerialization).build())
                .addTag("poczytajmitato" + config.tabNumForFileForSerialization)
                .build();

        WorkManager.getInstance(this.requireContext()).enqueue(request);
    }

    public void onBackPressed() {
        if (pageList.isShown()) System.exit(0);
        db.setPageTop(pageListAdapter.getItem(positionInPageList).url, webView.getScrollY());
        pageListAdapter.update(db, config.showHiddenTexts,
                config.readInfoForReadFragment.iterator());
        viewSwitcher.showPrevious();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.read_fragment, container, false);
        viewSwitcher = view.findViewById(R.id.viewSwitcher2);

        /* Page with webview */
        SwipeRefreshLayout refresh2 = view.findViewById(R.id.swiperefresh2);
        webView = view.findViewById(R.id.webview);

        webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                webView.scrollTo(0, pageListAdapter.getItem(positionInPageList).top);
            }
        });
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(webView.getSettings(),
                    WebSettingsCompat.FORCE_DARK_ON);
        }

        refresh2.setOnRefreshListener(() -> {
            Page p = pageListAdapter.getItem(positionInPageList);
            File f = p.getCacheFileName(getContext());
            Utils.getPage(p.url,
                    result -> webView.loadDataWithBaseURL(null,
                            Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f),
                            MIME_TYPE, ENCODING, null),
                    mainThreadHandler, threadPoolExecutor);
            refresh2.setRefreshing(false);
        });

        /* Page with list */
        Button menu = view.findViewById(R.id.menuButton2);
        refresh = view.findViewById(R.id.swiperefresh);
        pageList = view.findViewById(R.id.pagesRecyclerView);

        pageListAdapter = new PageListRecyclerViewAdapter(requireContext());
        pageListAdapter.setOnClick(position -> {
            Page p = pageListAdapter.getItem(position);
            File f = p.getCacheFileName(requireContext());
            positionInPageList = position;
            if (f.exists()) {
                webView.loadDataWithBaseURL(null, Utils.readFile(f),
                        MIME_TYPE, ENCODING, null);
            } else {
                webView.loadDataWithBaseURL(null, "Czytanie pliku " + p.url,
                        MIME_TYPE, ENCODING, null);
                Utils.getPage(p.url, result -> {
                            pageListAdapter.update(db, config.showHiddenTexts,
                                    config.readInfoForReadFragment.iterator());
                            webView.loadDataWithBaseURL(null,
                                    Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f),
                                    MIME_TYPE, ENCODING, null);
                        },
                        mainThreadHandler, threadPoolExecutor);
            }
            viewSwitcher.showNext();
        });
        pageList.setAdapter(pageListAdapter);
        pageList.addItemDecoration(new DividerItemDecoration(requireContext(),
                DividerItemDecoration.VERTICAL));
        pageListAdapter.update(db, config.showHiddenTexts, config.readInfoForReadFragment.iterator());

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                db.setPageVisible(pageListAdapter
                        .getItem(viewHolder.getAbsoluteAdapterPosition()).url, !config.showHiddenTexts);
                pageListAdapter.update(db, config.showHiddenTexts, config.readInfoForReadFragment.iterator());
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
        });
        itemTouchHelper.attachToRecyclerView(pageList);

        refresh.setOnRefreshListener(() -> {
            for (Page.PageTyp pt : config.readInfoForReadFragment) {
                refresh.setRefreshing(true);
                Page.getReadInfo(pt).getList(
                        result -> pageListAdapter.update(db, config.showHiddenTexts,
                                config.readInfoForReadFragment.iterator()),
                        result -> refresh.setRefreshing(false),
                        mainThreadHandler, threadPoolExecutor, db, this.getContext(), pt,
                        3);
            }
        });

        menu.setOnClickListener(view1 -> {
            LinkedHashMap<String, Page.PageTyp> hm = new LinkedHashMap<String, Page.PageTyp>() {{
                put("fantastyka.pl, archiwum", Page.PageTyp.FANTASTYKA_ARCHIWUM);
                put("fantastyka.pl, biblioteka", Page.PageTyp.FANTASTYKA_BIBLIOTEKA);
                put("fantastyka.pl, poczekalnia", Page.PageTyp.FANTASTYKA_POCZEKALNIA);
                put("opowi.pl, fantastyka", Page.PageTyp.OPOWI_FANTASTYKA);
            }};
            int i = 0;
            int mainIndex = 0;
            PopupMenu popupMenu = new PopupMenu(requireContext(), menu);
            popupMenu.getMenu().add(1, i++, mainIndex++, R.string.MENU_SHOW_HIDDEN)
                    .setCheckable(true).setChecked(config.showHiddenTexts);
            popupMenu.getMenu().add(1, i++, mainIndex++, R.string.MENU_IMPORT_EPUB);
            popupMenu.getMenu().add(1, i++, mainIndex++, R.string.MENU_EXPORT_EPUB);
            popupMenu.getMenu().add(1, i++, mainIndex++, R.string.MENU_GET_UNREAD_TEXTS);
            popupMenu.getMenu().add(1, i++, mainIndex++, R.string.MENU_GET_ALL_TEXTS);
            popupMenu.getMenu().add(1, i++, mainIndex++, R.string.MENU_GET_ALL_INDEX_PAGES);
            popupMenu.getMenu().add(2, i++, mainIndex++, R.string.MENU_CLONE_TAB);
            popupMenu.getMenu().add(2, i++, mainIndex++, R.string.MENU_DELETE_TAB);
            popupMenu.getMenu().add(3, i++, mainIndex++, R.string.MENU_USE_TOR)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setChecked(config.useTOR);
            popupMenu.getMenu().add(3, i++, mainIndex++, R.string.MENU_GET_TEXTS_WITH_INDEX)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setChecked(config.getTextsWhenRefreshingIndex);
            for (String s : hm.keySet()) {
                popupMenu.getMenu().add(3, i++, mainIndex++, s).
                        setActionView(R.layout.checkbox_menu_item).setCheckable(true).setChecked(
                                config.readInfoForReadFragment.contains(hm.get(s)));
            }
            popupMenu.getMenu().add(3, i++, mainIndex++, R.string.MENU_LOCAL_AUTHOR_FILTER)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true);
            popupMenu.getMenu().add(3, i++, mainIndex++, R.string.MENU_LOCAL_TAG_FILTER)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true);
            popupMenu.getMenu().add(4, i++, mainIndex++,
                            "Pobierz co " + (config.howOftenRefreshTabInHours == -1 ?
                                    "x" : config.howOftenRefreshTabInHours) + " godzin")
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setChecked(config.howOftenRefreshTabInHours != -1);
            popupMenu.getMenu().add(4, i++, mainIndex++,
                            "Przy błędzie co " +
                                    (config.howOftenTryToRefreshTabAfterErrorInMinutes == -1 ?
                                            "x" : config.howOftenTryToRefreshTabAfterErrorInMinutes) +
                                    " minut").setActionView(R.layout.checkbox_menu_item)
                    .setCheckable(true).setChecked(config.howOftenTryToRefreshTabAfterErrorInMinutes != -1)
                    .setEnabled(config.howOftenRefreshTabInHours != -1);
            popupMenu.getMenu().add(4, i++, mainIndex++, R.string.MENU_DOWNLOAD_ON_WIFI)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1)
                    .setChecked(config.downloadWithWifi);
            popupMenu.getMenu().add(4, i++, mainIndex++, R.string.MENU_DOWNLOAD_ON_GSM)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1)
                    .setChecked(config.downloadWithGSM);
            popupMenu.getMenu().add(4, i++, mainIndex++, R.string.MENU_DOWNLOAD_ON_OTHER_NET)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1)
                    .setChecked(config.downloadWithOtherNet);
            popupMenu.getMenu().add(4, i++, mainIndex++, R.string.MENU_DOWNLOAD_ONLY_ON_CHARGING)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1)
                    .setChecked(config.downloadDuringChargingOnly);
            popupMenu.getMenu().add(4, i++, mainIndex++, R.string.MENU_DONT_DOWNLOAD_WITH_LOW_BATTERY)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1)
                    .setChecked(config.doNotDownloadWithLowBattery);
            popupMenu.getMenu().add(4, i++, mainIndex++, R.string.MENU_NETWORK_WITHOUT_LIMIT)
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1)
                    .setChecked(config.networkWithoutLimit);
            popupMenu.getMenu().add(5, i, mainIndex, R.string.MENU_WRITE_MAIL_TO_AUTHOR);
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
                        config.readInfoForReadFragment.add(hm.get(s));
                    } else {
                        config.readInfoForReadFragment.remove(hm.get(s));
                    }
                    pageListAdapter.update(db, config.showHiddenTexts,
                            config.readInfoForReadFragment.iterator());
                    break;
                }
                if (item.getTitle().equals(getString(R.string.MENU_SHOW_HIDDEN))) {
                    config.showHiddenTexts = item.isChecked();
                    pageListAdapter.update(db, config.showHiddenTexts,
                            config.readInfoForReadFragment.iterator());
                } else if (item.getTitle().equals(getString(R.string.MENU_DOWNLOAD_ON_WIFI))) {
                    config.downloadWithWifi = item.isChecked();
                } else if (item.getTitle().equals(getString(R.string.MENU_DOWNLOAD_ON_GSM))) {
                    config.downloadWithGSM = item.isChecked();
                } else if (item.getTitle().equals(getString(R.string.MENU_DOWNLOAD_ON_OTHER_NET))) {
                    config.downloadWithOtherNet = item.isChecked();
                } else if (item.getTitle().equals(getString(R.string.MENU_DOWNLOAD_ONLY_ON_CHARGING))) {
                    config.downloadDuringChargingOnly = item.isChecked();
                } else if (item.getTitle().equals(getString(R.string.MENU_DONT_DOWNLOAD_WITH_LOW_BATTERY))) {
                    config.doNotDownloadWithLowBattery = item.isChecked();
                } else if (item.getTitle().equals(getString(R.string.MENU_NETWORK_WITHOUT_LIMIT))) {
                    config.networkWithoutLimit = item.isChecked();
                } else if (item.getTitle().toString().startsWith("Pobierz co")) {
                    if (item.isChecked()) {
                        EditText input = new EditText(this.getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        Utils.dialog(this.requireContext(),
                                "Co ile godzin zakładka ma być odświeżana", input,
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
                } else if (item.getTitle().toString().startsWith("Przy błędzie co ")) {
                    if (item.isChecked()) {
                        EditText input = new EditText(this.getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        Utils.dialog(this.requireContext(),
                                "Co ile minut próbować pobrać indeks po pierwszym niepowodzeniu",
                                input, (dialog, which) -> {
                                    config.howOftenTryToRefreshTabAfterErrorInMinutes =
                                            Integer.parseInt(String.valueOf(input.getText()));
                                    config.saveToInternalStorage(ReadFragment.this.getContext());
                                }, (dialog, which) -> {
                                    dialog.dismiss();
                                    config.howOftenTryToRefreshTabAfterErrorInMinutes = -1;
                                    config.saveToInternalStorage(getContext());
                                });
                    } else {
                        config.howOftenTryToRefreshTabAfterErrorInMinutes = -1;
                    }
                }
                if (item.isCheckable()) {
                    config.saveToInternalStorage(getContext());
                    return true;
                }
                if (item.getTitle().equals(getString(R.string.MENU_CLONE_TAB))) {
                    EditText input = new EditText(this.getContext());
                    Utils.dialog(this.requireContext(), "Nazwa nowej zakładki", input,
                            (dialog, which) -> {
                                try {
                                    topPagerAdapter.addTab(config, input.getText().toString());
                                } catch (CloneNotSupportedException e) {
                                    e.printStackTrace();
                                }
                            }, (dialog, which) -> dialog.dismiss());
                } else if (item.getTitle().equals(getString(R.string.MENU_DELETE_TAB))) {
                    Utils.dialog(this.requireContext(), "Czy chcesz usunąć zakładkę?",
                            null, (dialog, which) -> topPagerAdapter.deleteTab(config),
                            (dialog, which) -> dialog.dismiss());
                } else if (item.getTitle().equals(getString(R.string.MENU_WRITE_MAIL_TO_AUTHOR))) {
                    Utils.contactMe(getContext());
                } else if (item.getTitle().equals(getString(R.string.MENU_GET_UNREAD_TEXTS))) {
                    for (Page p : pageListAdapter.getAllItems()) {
                        File f = p.getCacheFileName(this.getContext());
                        if (f.exists()) continue;
                        Utils.getPage(p.url, result -> {
                                    Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f);
                                    pageListAdapter.update(db, config.showHiddenTexts,
                                            config.readInfoForReadFragment.iterator());
                                },
                                mainThreadHandler, threadPoolExecutor);
                    }
                } else if (item.getTitle().equals(getString(R.string.MENU_GET_ALL_TEXTS))) {
                    for (Page p : pageListAdapter.getAllItems()) {
                        File f = p.getCacheFileName(this.getContext());
                        Utils.getPage(p.url, result -> {
                                    Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f);
                                    pageListAdapter.update(db, config.showHiddenTexts,
                                            config.readInfoForReadFragment.iterator());
                                },
                                mainThreadHandler, threadPoolExecutor);
                    }
                } else if (item.getTitle().equals(getString(R.string.MENU_GET_ALL_INDEX_PAGES))) {
                    for (Page.PageTyp pt : config.readInfoForReadFragment) {
                        refresh.setRefreshing(true);
                        Page.getReadInfo(pt).getList(
                                result -> pageListAdapter.update(db, config.showHiddenTexts,
                                        config.readInfoForReadFragment.iterator()),
                                result -> refresh.setRefreshing(false),
                                mainThreadHandler, threadPoolExecutor, db, this.getContext(), pt,
                                -1);
                    }
                } else if (item.getTitle().equals(getString(R.string.MENU_EXPORT_EPUB))) {
                    Utils.createEPUB(getContext(), config.tabName, pageListAdapter.getAllItems());
                }
                return false;
            });
            popupMenu.show();
        });
        return view;
    }
}
