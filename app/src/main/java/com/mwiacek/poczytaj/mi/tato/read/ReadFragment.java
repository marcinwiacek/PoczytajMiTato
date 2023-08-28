package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.ViewSwitcher;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.os.HandlerCompat;
import androidx.core.view.MenuProvider;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.webkit.WebSettingsCompat;
import androidx.webkit.WebViewClientCompat;
import androidx.webkit.WebViewFeature;
import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;

import com.google.android.material.snackbar.Snackbar;
import com.mwiacek.poczytaj.mi.tato.FragmentConfig;
import com.mwiacek.poczytaj.mi.tato.MainActivity;
import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.ViewPagerAdapter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class ReadFragment extends Fragment {
    private final static String MIME_TYPE = "text/html; charset=UTF-8";
    private final static String ENCODING = "UTF-8";
    private final static String URL_PREFIX = "https://mwiacek.com/ffiles/img/";
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private final ViewPagerAdapter topPagerAdapter = MainActivity.getViewPagerAdapter();
    private FragmentConfig config = null;
    private PageListRecyclerViewAdapter pageListAdapter;
    private RecyclerView pageList;
    private SwipeRefreshLayout refresh;
    private SwipeRefreshLayout refresh2;
    private ViewSwitcher viewSwitcher;
    private WebView webView;
    private SearchView searchView;
    private Toolbar toolbar;
    private NestedScrollView nestedScrollView;
    private ActivityResultLauncher<String> mCreateEPUB;
    private ActivityResultLauncher<String[]> mImportEPUB;
    private int positionInPageList;
    private String webViewLoadingString = "";
    private boolean loadingMorePages = false;

    public ReadFragment() {
    }

    public int getTabNum() {
        return config.fileNameTabNum;
    }

    /* Called, when page with concrete url needs to be updated in the list */
    public void onPageUpdate(String url) {
        pageListAdapter.onPageUpdate(url, MainActivity.getDB());
    }

    /* Called, when pages with concrete PageTyp need to be updated in the list */
    public void onPageUpdate(Page.PageTyp typ) {
        if (config.readInfoForReadFragment.contains(typ)) {
            pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                    config.readInfoForReadFragment, config.authorFilter,
                    config.tagFilter);
        }
    }

    public void onInvalidateMenu() {
        toolbar.invalidateMenu();
    }

    public void onBackPressed() {
        if (!webViewLoadingString.isEmpty()) return;
        if (pageList.isShown()) System.exit(0);
        MainActivity.getDB().setPageTop(pageListAdapter.getItem(positionInPageList).url, nestedScrollView.getScrollY());
        viewSwitcher.showPrevious();
        //webView.loadDataWithBaseURL(null, "", MIME_TYPE, ENCODING, null);
    }

    /* Called, when somebody deleted or added tab and we change mode from one to multi-tab mode
       or vice versa
     */
    public void onUpdateLayout(boolean withMargin) {
        int actionBarSize = (int) MainActivity.getContext().getTheme().obtainStyledAttributes(
                new int[]{android.R.attr.actionBarSize}).getDimension(0, 0);
        if (pageList != null) pageList.setPadding(0, 0, 0, withMargin ? actionBarSize : 0);
        //   if (frameLayout!=null) frameLayout.setPadding(0, 0, 0, withMargin ? actionBarSize : 0);
    }

    private void setupRefresh() {
        WorkManager.getInstance(MainActivity.getContext())
                .cancelAllWorkByTag("poczytajmitato" + config.fileNameTabNum);
        if (config.howOftenRefreshTabInHours == -1) return;
        Constraints constraints = new Constraints.Builder()
                .setRequiresBatteryNotLow(!config.canDownloadWithLowBattery)
                .setRequiresCharging(!config.canDownloadWithoutCharger)
                .setRequiresStorageNotLow(!config.canDownloadWithLowStorage)
                .setRequiredNetworkType(!config.canDownloadOnRoaming ? NetworkType.NOT_ROAMING :
                        (config.canDownloadInNetworkWithLimit ? NetworkType.METERED : NetworkType.UNMETERED))
                .build();
        WorkRequest.Builder<PeriodicWorkRequest.Builder, PeriodicWorkRequest> request = new PeriodicWorkRequest.Builder(UploadWorker.class,
                //   15,TimeUnit.MINUTES)
                config.howOftenRefreshTabInHours, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInputData(new Data.Builder()
                        .putInt("TabNum", config.fileNameTabNum).build())
                .addTag("poczytajmitato" + config.fileNameTabNum);
        if (config.howOftenTryToRefreshTabAfterErrorInMinutes != -1) {
            request.setBackoffCriteria(BackoffPolicy.LINEAR,
                    config.howOftenTryToRefreshTabAfterErrorInMinutes,
                    TimeUnit.MINUTES);
        }
        WorkManager.getInstance(MainActivity.getContext()).enqueue(request.build());
    }

    private void readTextFromInternet(Page p, WebView webView, boolean deleteBefore) {
        boolean isRefreshing = refresh2.isRefreshing();
        refresh2.setRefreshing(false);
        if (webView != null) {
            webViewLoadingString = "<div style='word-break: break-all;'><h1>" + p.name + "</h1><p>" +
                    "Postęp 0 bajtów, proszę czekać<p>Czytanie pliku " + p.url;
            webView.loadDataWithBaseURL(null, webViewLoadingString + "</div>", MIME_TYPE,
                    ENCODING, null);
        }
        new Thread(() -> Page.getReadInfo(p.typ).processTextFromSinglePage(MainActivity.getContext(),
                p, mainThreadHandler, threadPoolExecutor,
                updateIndicator -> {
                    if (updateIndicator == 0) {
                        if (deleteBefore) {
                            File f0 = p.getCacheFile(MainActivity.getContext());
                            String fileContent = Utils.readTextFile(f0);
                            for (String s : Utils.findImagesUrlInHTML(fileContent)) {
                                File f = new File(Page.getCacheDirectory(MainActivity.getContext()) + File.separator + s);
                                f.delete();
                            }
                            f0.delete();
                        }
                    }
                    if (webView != null) {
                        webViewLoadingString = webViewLoadingString.replaceAll("Postęp [0-9]* bajtów, proszę czekać<p>",
                                "Postęp " + updateIndicator + " bajtów, proszę czekać<p>");
                        webView.loadDataWithBaseURL(null,
                                webViewLoadingString + "</div>", MIME_TYPE, ENCODING, null);
                    }
                },
                error -> {
                    Snackbar.make(getView(), R.string.NO_INTERNET, Snackbar.LENGTH_SHORT).show();

                    webViewLoadingString = "";
                    if (isRefreshing) {
                        File f = p.getCacheFile(MainActivity.getContext());
                        if (webView != null) {
                            webView.loadDataWithBaseURL(null, renderPage(Utils.readTextFile(f)),
                                    MIME_TYPE, ENCODING, null);
                        }
                    } else {
                        viewSwitcher.showPrevious();
                        if (webView != null) {
                            webView.loadDataWithBaseURL(null, "", MIME_TYPE, ENCODING, null);
                        }
                    }
                },
                imageURLListOnBeginning -> {
                    if (webView != null) {
                        webViewLoadingString += "<br>OK";
                        for (String s : imageURLListOnBeginning) {
                            webViewLoadingString += "<p>Czytanie pliku " + s;
                        }
                        webView.loadDataWithBaseURL(null,
                                webViewLoadingString + "</div>", MIME_TYPE, ENCODING, null);
                    }
                }, imageUrlInAfterReadingTheMiddle -> {
                    if (webView != null) {
                        webViewLoadingString = webViewLoadingString.replace(imageUrlInAfterReadingTheMiddle,
                                imageUrlInAfterReadingTheMiddle + "<br>OK");
                        webView.loadDataWithBaseURL(null,
                                webViewLoadingString + "</div>", MIME_TYPE, ENCODING, null);
                    }
                }, mainPageContentOnTheEnd -> {
                    if (webView != null) {
                        webViewLoadingString = "";
                    }
                    refresh2.setRefreshing(false);
                    MainActivity.getDB().disableUpdatedOnServer(p.url);
                    for (Fragment ff : getParentFragmentManager().getFragments()) {
                        if (ff instanceof ReadFragment) {
                            ((ReadFragment) ff).onPageUpdate(p.url);
                        }
                    }
                    if (webView != null) {
                        webView.loadDataWithBaseURL(null, renderPage(mainPageContentOnTheEnd),
                                MIME_TYPE, ENCODING, null
                        );
                    }
                }
        )).start();
    }

    private void setSearchHintColor() {
        ((EditText) searchView.findViewById(androidx.appcompat.R.id.search_src_text))
                .setHintTextColor(getResources().getColor(
                        config.showHiddenTexts == FragmentConfig.HiddenTexts.NONE ?
                                android.R.color.darker_gray :
                                (config.showHiddenTexts == FragmentConfig.HiddenTexts.RED ?
                                        android.R.color.holo_red_dark :
                                        (config.showHiddenTexts == FragmentConfig.HiddenTexts.GREEN ?
                                                android.R.color.holo_green_dark :
                                                android.R.color.holo_orange_dark))
                ));
    }

    private String renderPage(String s) {
        String page = s;
        if (!searchView.getQuery().toString().trim().isEmpty()) {
            String[] toSearch = searchView.getQuery().toString().trim().split(" ");
            for (String ts : toSearch) {
                page = Utils.findText(page, ts.trim());
            }
        }
        return "<html><head><body style='text-align:justify; font-size: " +
                MainActivity.getSharedPref().getString(MainActivity.PREF_TEXT_SIZE, "medium") +
                ";'>" + page.replaceAll("src=\"", "src=\"" + URL_PREFIX) +
                "</body></html>";
    }

    public void informAllReadTabsAboutUpdate(Page.PageTyp typ) {
        for (Fragment ff : getFragmentManager().getFragments()) {
            if (ff instanceof ReadFragment) {
                ((ReadFragment) ff).onPageUpdate(typ);
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        this.config = ViewPagerAdapter.configs.get(this.getArguments().getInt("configNum"));
        View view = inflater.inflate(R.layout.read_fragment, container, false);

        viewSwitcher = view.findViewById(R.id.viewSwitcher2);

        /* Page with webview */

        nestedScrollView = view.findViewById(R.id.nestedscroll);
        nestedScrollView.setOnScrollChangeListener((NestedScrollView.OnScrollChangeListener)
                (v, scrollX, scrollY, oldScrollX, oldScrollY) -> {
                    int percent = (int) ((float) scrollY
                            / (float) (v.getChildAt(0).getMeasuredHeight() - v.getMeasuredHeight())
                            * 100);
                    if (percent != 100) {
                        Snackbar.make(getView(), percent + "%", Snackbar.LENGTH_SHORT).show();
                    }
                    if (config.addToOrange && percent == 100) {
                        MainActivity.getDB().setPageHidden(pageListAdapter.getItem(positionInPageList).url,
                                FragmentConfig.HiddenTexts.AMBER);
                        informAllReadTabsAboutUpdate(pageListAdapter.getItem(positionInPageList).typ);
                    }
                });

        webView = view.findViewById(R.id.webview);
        if (((MainActivity.getContext().getResources().getConfiguration().uiMode &
                Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES)) {
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
                WebSettingsCompat.setForceDark(webView.getSettings(), WebSettingsCompat.FORCE_DARK_ON);
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK_STRATEGY)) {
                WebSettingsCompat.setForceDarkStrategy(webView.getSettings(),
                        WebSettingsCompat.DARK_STRATEGY_USER_AGENT_DARKENING_ONLY);
            }
            if (WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(webView.getSettings(), true);
            }
        }
        /* 1. We could load images locally using relative URLs with setAllowFileAccess(true)
              and it works, but is not very recommended.
           2. We could also use WebViewAssetLoader.InternalStoragePathHandler and this should
              help with making redirections - in image url we give full url with https://.../path/
              and thx to handler paths with /path/ will be redirected to local files, but...
              it doesn't work. For investigating why.
           3. Currently I implement some kind of own handler below.
        */
        webView.getSettings().setAllowFileAccess(false);
        webView.getSettings().setAllowFileAccessFromFileURLs(false);
        webView.getSettings().setAllowUniversalAccessFromFileURLs(false);
        webView.getSettings().setAllowContentAccess(false);
        /* final WebViewAssetLoader imageLoader = new WebViewAssetLoader.Builder()
                .addPathHandler("/ffiles/", new WebViewAssetLoader.InternalStoragePathHandler(
                        MainActivity.getContext()()(), new File(MainActivity.getContext()()().getCacheDir(),"ffiles")))
                .setHttpAllowed(false).setDomain("mwiacek.com").build();
        */
        // webView.getSettings().setUseWideViewPort(false);
        // webView.getSettings().setLoadWithOverviewMode(false);
        //webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        //webView.setVerticalScrollBarEnabled(true);
        //webView.setHorizontalScrollBarEnabled(true);
        //webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
        //webView.setScrollbarFadingEnabled(false);
        webView.setNetworkAvailable(false);
        webView.setWebViewClient(new WebViewClientCompat() {
            private WebResourceResponse shouldIntercept(String url) {
                for (String extension : Page.SUPPORTED_IMAGE_EXTENSIONS) {
                    if (url.startsWith(URL_PREFIX) && url.endsWith("." + extension)) {
                        try {
                            return new WebResourceResponse(MimeTypeMap.getSingleton()
                                    .getMimeTypeFromExtension("." + extension), null,
                                    new FileInputStream(Page.getCacheDirectory(MainActivity.getContext()) +
                                            File.separator + url.replace(URL_PREFIX, "")));
                        } catch (FileNotFoundException ignore) {
                        }
                    }
                }
                return null;
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                int top = MainActivity.getDB().getPageTop(pageListAdapter.getItem(positionInPageList).url);
                nestedScrollView.post(() -> nestedScrollView.setScrollY(top));
                // nestedScrollView.setScrollY(top);
                //   nestedScrollView.dispatchNestedScroll(0, 0, 0,
                //         top, null);
                // nestedScrollView.scrollTo(0, top);
            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return shouldIntercept(request.getUrl().toString());
                // return imageLoader.shouldInterceptRequest(request.getUrl());
            }

            @Override
            @SuppressWarnings("deprecation")
            public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
                return shouldIntercept(Uri.parse(url).toString());
                // return imageLoader.shouldInterceptRequest(Uri.parse(url));
            }
        });

        refresh2 = view.findViewById(R.id.swiperefresh2);
        refresh2.setOnRefreshListener(() -> {
            Page p = pageListAdapter.getItem(positionInPageList);
            MainActivity.getDB().setPageTop(p.url, 0);
            readTextFromInternet(p, webView, true);
        });

        /* Page with list */
        refresh = view.findViewById(R.id.swiperefresh);
        refresh.setOnRefreshListener(() -> {
            if (config.showHiddenTexts != FragmentConfig.HiddenTexts.NONE
                    || !searchView.getQuery().toString().isEmpty()) {
                refresh.setRefreshing(false);
                return;
            }
            refresh.setRefreshing(true);
            Page.getList(MainActivity.getContext(), mainThreadHandler, threadPoolExecutor, MainActivity.getDB(),
                    config.readInfoForReadFragment, config.tabName, config.fileNameTabNum,
                    false, true, result -> Snackbar.make(getView(),
                            R.string.NO_INTERNET, Snackbar.LENGTH_SHORT).show(),
                    this::informAllReadTabsAboutUpdate,
                    result -> refresh.setRefreshing(false));
        });

        toolbar = view.findViewById(R.id.toolbar);
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

                menu.add(0, R.string.MENU_SHOW_GREEN, mainIndex++, R.string.MENU_SHOW_GREEN)
                        .setCheckable(true)
                        .setChecked(config.showHiddenTexts == FragmentConfig.HiddenTexts.GREEN);
                menu.add(0, R.string.MENU_SHOW_RED, mainIndex++, R.string.MENU_SHOW_RED)
                        .setCheckable(true)
                        .setChecked(config.showHiddenTexts == FragmentConfig.HiddenTexts.RED);
                menu.add(0, R.string.MENU_SHOW_AMBER, mainIndex++, R.string.MENU_SHOW_AMBER)
                        .setCheckable(true)
                        .setChecked(config.showHiddenTexts == FragmentConfig.HiddenTexts.AMBER);
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
                        .setCheckable(true).setChecked(ViewPagerAdapter.isSearchTabAvailable());
                //   menu.add(3, R.string.MENU_USE_TOR, mainIndex++, R.string.MENU_USE_TOR)
                //           .setCheckable(true).setChecked(config.useTOR);
                //menu.add(3, R.string.MENU_GET_TEXTS_WITH_INDEX, mainIndex++, R.string.MENU_GET_TEXTS_WITH_INDEX)
                //        .setCheckable(true).setChecked(config.getTextsWhenRefreshingIndex);
                for (String s : hm.keySet()) {
                    menu.add(3, i++, mainIndex++, s).setCheckable(true)
                            .setChecked(config.readInfoForReadFragment.contains(hm.get(s)));
                }
                menu.add(3, R.string.MENU_LOCAL_AUTHOR_FILTER, mainIndex++, R.string.MENU_LOCAL_AUTHOR_FILTER)
                        .setCheckable(true).setChecked(!config.authorFilter.isEmpty());
                menu.add(3, R.string.MENU_LOCAL_TAG_FILTER, mainIndex++, R.string.MENU_LOCAL_TAG_FILTER)
                        .setCheckable(true).setChecked(!config.tagFilter.isEmpty());
                menu.add(3, R.string.MENU_READ_TO_ORANGE, mainIndex, R.string.MENU_READ_TO_ORANGE)
                        .setCheckable(true).setChecked(config.addToOrange);
                menu.add(4, i++, mainIndex++,
                                "Pobierz co " + (config.howOftenRefreshTabInHours == -1 ?
                                        "x" : config.howOftenRefreshTabInHours) + " godzin")
                        .setCheckable(true).setChecked(config.howOftenRefreshTabInHours != -1);
                menu.add(4, i, mainIndex++,
                                "Przy błędzie co " +
                                        (config.howOftenTryToRefreshTabAfterErrorInMinutes == -1 ?
                                                "x" : config.howOftenTryToRefreshTabAfterErrorInMinutes) +
                                        " minut").setCheckable(true)
                        .setChecked(config.howOftenTryToRefreshTabAfterErrorInMinutes != -1)
                        .setEnabled(config.howOftenRefreshTabInHours != -1);
                menu.add(4, R.string.MENU_DOWNLOAD_ON_WIFI, mainIndex++, R.string.MENU_DOWNLOAD_ON_WIFI)
                        .setCheckable(true).setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.canDownloadWithWifi);
                menu.add(4, R.string.MENU_DOWNLOAD_ON_GSM, mainIndex++, R.string.MENU_DOWNLOAD_ON_GSM)
                        .setCheckable(true).setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.canDownloadWithGSM);
                menu.add(4, R.string.MENU_DOWNLOAD_ON_OTHER_NET, mainIndex++, R.string.MENU_DOWNLOAD_ON_OTHER_NET)
                        .setCheckable(true).setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.canDownloadWithOtherNetwork);
                menu.add(4, R.string.MENU_DOWNLOAD_WITHOUT_CHARGER, mainIndex++, R.string.MENU_DOWNLOAD_WITHOUT_CHARGER)
                        .setCheckable(true).setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.canDownloadWithoutCharger);
                menu.add(4, R.string.MENU_DOWNLOAD_WITH_LOW_BATTERY, mainIndex++, R.string.MENU_DOWNLOAD_WITH_LOW_BATTERY)
                        .setCheckable(true).setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.canDownloadWithLowBattery);
                menu.add(4, R.string.MENU_DOWNLOAD_WITH_LOW_STORAGE, mainIndex++, R.string.MENU_DOWNLOAD_WITH_LOW_STORAGE)
                        .setCheckable(true).setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.canDownloadWithLowStorage);
                menu.add(4, R.string.MENU_NETWORK_WITH_LIMIT, mainIndex++, R.string.MENU_NETWORK_WITH_LIMIT)
                        .setCheckable(true).setEnabled(config.howOftenRefreshTabInHours != -1)
                        .setChecked(config.canDownloadInNetworkWithLimit);
                menu.add(5, R.string.MENU_DONT_BLOCK_SCREEN, mainIndex, R.string.MENU_DONT_BLOCK_SCREEN)
                        .setCheckable(true)
                        .setChecked(MainActivity.getSharedPref().getBoolean(MainActivity.PREF_DONT_BLOCK_SCREEN, false));
                menu.add(5, R.string.MENU_HIDE_ELEMENTS, mainIndex, R.string.MENU_HIDE_ELEMENTS)
                        .setCheckable(true)
                        .setChecked(MainActivity.getSharedPref().getBoolean(MainActivity.PREF_HIDE_NAVIGATION, false));
                menu.add(5, R.string.MENU_TEXT_SIZE, mainIndex, getResources().getString(R.string.MENU_TEXT_SIZE) + ": " +
                        MainActivity.getSharedPref().getString(MainActivity.PREF_TEXT_SIZE, "medium")
                );
                menu.add(5, R.string.MENU_WRITE_MAIL_TO_AUTHOR, mainIndex, R.string.MENU_WRITE_MAIL_TO_AUTHOR);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    menu.setGroupDividerEnabled(true);
                }
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.string.MENU_LOCAL_AUTHOR_FILTER) {
                    EditText input = new EditText(MainActivity.getContext());
                    input.setText(config.authorFilter);
                    Utils.dialog(MainActivity.getContext(),
                            "Podaj autorów (oddzielonych przecinkiem). " +
                                    "\"not \" na początku autora oznacza zaprzeczenie.", input,
                            (dialog, which) -> {
                                menuItem.setChecked(!input.getText().toString().isEmpty());
                                if (!config.authorFilter.equals(input.getText().toString().trim())) {
                                    config.authorFilter = input.getText().toString().trim();
                                    pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                                            config.readInfoForReadFragment,
                                            config.authorFilter, config.tagFilter);
                                }
                                config.saveToInternalStorage(MainActivity.getContext());
                            }, (dialog, which) -> {
                                dialog.dismiss();
                            });
                    return false;
                } else if (menuItem.getItemId() == R.string.MENU_LOCAL_TAG_FILTER) {
                    EditText input = new EditText(MainActivity.getContext());
                    input.setText(config.tagFilter);
                    Utils.dialog(MainActivity.getContext(),
                            "Podaj tagi (oddzielone przecinkiem). " +
                                    "\"not \" na początku taga oznacza zaprzeczenie.", input,
                            (dialog, which) -> {
                                menuItem.setChecked(!input.getText().toString().isEmpty());
                                if (!config.tagFilter.equals(input.getText().toString().trim())) {
                                    config.tagFilter = input.getText().toString().trim();
                                    pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                                            config.readInfoForReadFragment,
                                            config.authorFilter, config.tagFilter);
                                }
                                config.saveToInternalStorage(MainActivity.getContext());
                            }, (dialog, which) -> {
                                dialog.dismiss();
                            });
                    return false;
                } else if (menuItem.getItemId() == R.string.MENU_HIDE_ELEMENTS) {
                    SharedPreferences.Editor editor = MainActivity.getSharedPref().edit();
                    editor.putBoolean(MainActivity.PREF_HIDE_NAVIGATION,
                            !menuItem.isChecked());
                    editor.commit();
                    (new AlertDialog.Builder(MainActivity.getContext())
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setMessage("Wymaga restartu apki. Będzie ona teraz zamknięta i musisz ją wystartować.")
                            .setPositiveButton("OK", (dialog, which) -> System.exit(0)))
                            .create().show();

                } else if (menuItem.getItemId() == R.string.MENU_DONT_BLOCK_SCREEN) {
                    SharedPreferences.Editor editor = MainActivity.getSharedPref().edit();
                    editor.putBoolean(MainActivity.PREF_DONT_BLOCK_SCREEN,
                            !menuItem.isChecked());
                    editor.commit();
                    (new AlertDialog.Builder(MainActivity.getContext())
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setMessage("Wymaga restartu apki. Będzie ona teraz zamknięta i musisz ją wystartować.")
                            .setPositiveButton("OK", (dialog, which) -> System.exit(0)))
                            .create().show();
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
                    pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                            config.readInfoForReadFragment, config.authorFilter,
                            config.tagFilter);
                    break;
                }
                if (menuItem.getItemId() == R.string.MENU_SHOW_GREEN) {
                    config.showHiddenTexts = menuItem.isChecked() ?
                            FragmentConfig.HiddenTexts.GREEN : FragmentConfig.HiddenTexts.NONE;
                    setSearchHintColor();
                    pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                            config.readInfoForReadFragment, config.authorFilter,
                            config.tagFilter);
                    config.saveToInternalStorage(MainActivity.getContext());
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            onInvalidateMenu();
                        }
                    }.sendEmptyMessage(1);
                } else if (menuItem.getItemId() == R.string.MENU_SHOW_RED) {
                    config.showHiddenTexts = menuItem.isChecked() ?
                            FragmentConfig.HiddenTexts.RED : FragmentConfig.HiddenTexts.NONE;
                    setSearchHintColor();
                    pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                            config.readInfoForReadFragment, config.authorFilter,
                            config.tagFilter);
                    config.saveToInternalStorage(MainActivity.getContext());
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            onInvalidateMenu();
                        }
                    }.sendEmptyMessage(1);
                } else if (menuItem.getItemId() == R.string.MENU_SHOW_AMBER) {
                    config.showHiddenTexts = menuItem.isChecked() ?
                            FragmentConfig.HiddenTexts.AMBER : FragmentConfig.HiddenTexts.NONE;
                    setSearchHintColor();
                    pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                            config.readInfoForReadFragment, config.authorFilter,
                            config.tagFilter);
                    config.saveToInternalStorage(MainActivity.getContext());
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            onInvalidateMenu();
                        }
                    }.sendEmptyMessage(1);
                } else if (menuItem.getItemId() == R.string.MENU_SHOW_SEARCH_TAB) {
                    new Handler(Looper.getMainLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                            if (menuItem.isChecked()) {
                                topPagerAdapter.addSearchTab();
                            } else {
                                topPagerAdapter.deleteSearchTab();
                            }
                        }
                    }.sendEmptyMessage(1);
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_ON_WIFI) {
                    config.canDownloadWithWifi = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_ON_GSM) {
                    config.canDownloadWithGSM = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_ON_OTHER_NET) {
                    config.canDownloadWithOtherNetwork = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_WITHOUT_CHARGER) {
                    config.canDownloadWithoutCharger = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_WITH_LOW_BATTERY) {
                    config.canDownloadWithLowBattery = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_DOWNLOAD_WITH_LOW_STORAGE) {
                    config.canDownloadWithLowStorage = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_NETWORK_WITH_LIMIT) {
                    config.canDownloadInNetworkWithLimit = menuItem.isChecked();
                } else if (menuItem.getItemId() == R.string.MENU_READ_TO_ORANGE) {
                    config.addToOrange = menuItem.isChecked();
                } else if (menuItem.getTitle().toString().startsWith("Pobierz co")) {
                    if (menuItem.isChecked()) {
                        EditText input = new EditText(MainActivity.getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        Utils.dialog(MainActivity.getContext(),
                                "Co ile godzin zakładka ma być odświeżana (24 = 1 dzień, " +
                                        "48 = 2 dni, 168 = tydzień, etc.)", input,
                                (dialog, which) -> {
                                    config.howOftenRefreshTabInHours =
                                            Integer.parseInt(String.valueOf(input.getText()));
                                    setupRefresh();
                                    config.saveToInternalStorage(MainActivity.getContext());
                                }, (dialog, which) -> {
                                    dialog.dismiss();
                                    config.howOftenRefreshTabInHours = -1;
                                    setupRefresh();
                                    config.saveToInternalStorage(MainActivity.getContext());
                                });
                    } else {
                        config.howOftenRefreshTabInHours = -1;
                        setupRefresh();
                    }
                } else if (menuItem.getTitle().toString().startsWith("Przy błędzie co ")) {
                    if (menuItem.isChecked()) {
                        EditText input = new EditText(MainActivity.getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        Utils.dialog(MainActivity.getContext(),
                                "Co ile minut próbować pobrać indeks po pierwszym niepowodzeniu",
                                input, (dialog, which) -> {
                                    config.howOftenTryToRefreshTabAfterErrorInMinutes =
                                            Integer.parseInt(String.valueOf(input.getText()));
                                    config.saveToInternalStorage(MainActivity.getContext());
                                }, (dialog, which) -> {
                                    dialog.dismiss();
                                    config.howOftenTryToRefreshTabAfterErrorInMinutes = -1;
                                    config.saveToInternalStorage(MainActivity.getContext());
                                });
                    } else {
                        config.howOftenTryToRefreshTabAfterErrorInMinutes = -1;
                    }
                }
                if (menuItem.isCheckable()) {
                    config.saveToInternalStorage(MainActivity.getContext());
                    return true;
                }
                if (menuItem.getItemId() == R.string.MENU_CLONE_TAB) {
                    EditText input = new EditText(MainActivity.getContext());
                    Utils.dialog(MainActivity.getContext(), "Nazwa nowej zakładki", input,
                            (dialog, which) -> {
                                try {
                                    topPagerAdapter.addTab(config, input.getText().toString());
                                } catch (CloneNotSupportedException e) {
                                    e.printStackTrace();
                                }
                            }, (dialog, which) -> dialog.dismiss());
                } else if (menuItem.getItemId() == R.string.MENU_CHANGE_TAB_NAME) {
                    EditText input = new EditText(MainActivity.getContext());
                    input.setText(config.tabName);
                    Utils.dialog(MainActivity.getContext(), "Nowa nazwa zakładki " +
                                    config.tabName, input,
                            (dialog, which) -> {
                                if (!input.getText().toString().equals(config.tabName)) {
                                    config.tabName = input.getText().toString();
                                    config.saveToInternalStorage(MainActivity.getContext());
                                    topPagerAdapter.notifyDataSetChanged();
                                }
                            }, (dialog, which) -> dialog.dismiss());
                } else if (menuItem.getItemId() == R.string.MENU_DELETE_TAB) {
                    Utils.dialog(MainActivity.getContext(), "Czy chcesz usunąć zakładkę " +
                                    config.tabName + "?",
                            null, (dialog, which) -> topPagerAdapter.deleteTab(config),
                            (dialog, which) -> dialog.dismiss());
                } else if (menuItem.getItemId() == R.string.MENU_WRITE_MAIL_TO_AUTHOR) {
                    Utils.contactMe(MainActivity.getContext());
                } else if (menuItem.getItemId() == R.string.MENU_GET_UNREAD_TEXTS) {
                    for (Page p : pageListAdapter.getAllItems()) {
                        File f = p.getCacheFile(MainActivity.getContext());
                        if (f.exists()) {
                            continue;
                        }
                        readTextFromInternet(p, webView, false);
                    }
                } else if (menuItem.getItemId() == R.string.MENU_GET_ALL_TEXTS) {
                    for (Page p : pageListAdapter.getAllItems()) {
                        readTextFromInternet(p, webView, true);
                    }
                } else if (menuItem.getItemId() == R.string.MENU_GET_ALL_INDEX_PAGES) {
                    refresh.setRefreshing(true);
                    Page.getList(MainActivity.getContext(), mainThreadHandler, threadPoolExecutor, MainActivity.getDB(),
                            config.readInfoForReadFragment, config.tabName, config.fileNameTabNum,
                            true, false,
                            result -> Snackbar.make(getView(),
                                    R.string.NO_INTERNET, Snackbar.LENGTH_SHORT).show(),
                            p -> informAllReadTabsAboutUpdate(p), result -> refresh.setRefreshing(false));
                } else if (menuItem.getItemId() == R.string.MENU_EXPORT_EPUB) {
                    mCreateEPUB.launch(config.tabName);
                } else if (menuItem.getItemId() == R.string.MENU_IMPORT_EPUB) {
                    mImportEPUB.launch(new String[]{Utils.EPUB_MIME_TYPE});
                } else if (menuItem.getItemId() == R.string.MENU_TEXT_SIZE) {
                    AtomicReference<String> selectedSize = new AtomicReference<>(MainActivity.getSharedPref().getString(MainActivity.PREF_TEXT_SIZE, "medium"));
                    (new AlertDialog.Builder(MainActivity.getContext())
                            .setIcon(android.R.drawable.ic_dialog_info)
                            .setTitle("Wielkość tekstu podczas czytania")
                            .setSingleChoiceItems(
                                    MainActivity.getContext().getResources().getStringArray(R.array.text_size),
                                    new ArrayList<>(Arrays.asList(MainActivity.getContext().getResources().getStringArray(R.array.text_size)))
                                            .indexOf(MainActivity.getSharedPref().getString(MainActivity.PREF_TEXT_SIZE, "medium")),
                                    (dialog, which) ->
                                            selectedSize.set(
                                                    MainActivity.getContext().getResources().getStringArray(R.array.text_size)[which]))
                            .setPositiveButton("OK", (dialog, which) -> {
                                SharedPreferences.Editor editor = MainActivity.getSharedPref().edit();
                                editor.putString(MainActivity.PREF_TEXT_SIZE, String.valueOf(selectedSize));
                                editor.commit();
                                toolbar.invalidateMenu();
                                dialog.dismiss();
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss()))
                            .create().show();
                }

                return false;
            }
        });

        pageListAdapter = new PageListRecyclerViewAdapter(MainActivity.getContext());
        pageListAdapter.setOnClick(position -> {
            Page p = pageListAdapter.getItem(position);
            File f = p.getCacheFile(MainActivity.getContext());
            positionInPageList = position;
            if (f.exists()) {
                if (p.updatedOnServer) {
                    Utils.dialog(MainActivity.getContext(),
                            "Na serwerze może być nowa wersja tekstu. Czy chcesz odświeżyć?",
                            null,
                            (dialog, which) -> {
                                readTextFromInternet(p, webView, false);
                                dialog.dismiss();
                            }, (dialog, which) -> {
                                webView.loadDataWithBaseURL(null,
                                        renderPage(Utils.readTextFile(f)), MIME_TYPE, ENCODING, null);
                                dialog.dismiss();
                            });
                } else {
                    webView.loadDataWithBaseURL(null, renderPage(Utils.readTextFile(f)),
                            MIME_TYPE, ENCODING, null);
                }
            } else {
                readTextFromInternet(p, webView, false);
            }
            viewSwitcher.showNext();
        });

        pageList = view.findViewById(R.id.pagesRecyclerView);
        pageList.setAdapter(pageListAdapter);
        pageList.addItemDecoration(new DividerItemDecoration(MainActivity.getContext(),
                DividerItemDecoration.VERTICAL));
        pageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                if (loadingMorePages || config.showHiddenTexts != FragmentConfig.HiddenTexts.NONE ||
                        pageListAdapter.getItemCount() == 1 || !searchView.getQuery().toString().isEmpty())
                    return;
                LinearLayoutManager linearLayoutManager = (LinearLayoutManager)
                        recyclerView.getLayoutManager();
                if (linearLayoutManager != null &&
                        linearLayoutManager.findLastCompletelyVisibleItemPosition() >
                                pageListAdapter.getItemCount() - 7) {
                    loadingMorePages = true;
                    new Handler().postDelayed(() ->
                            Page.getList(MainActivity.getContext(), mainThreadHandler, threadPoolExecutor, MainActivity.getDB(),
                                    config.readInfoForReadFragment, config.tabName,
                                    config.fileNameTabNum, false, false,
                                    result -> Snackbar.make(getView(),
                                            R.string.NO_INTERNET, Snackbar.LENGTH_SHORT).show(),
                                    pt -> informAllReadTabsAboutUpdate(pt),
                                    result -> loadingMorePages = false), 0);
                }
            }
        });

        pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts, config.readInfoForReadFragment,
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
                Page p = pageListAdapter.getItem(viewHolder.getAbsoluteAdapterPosition());
                MainActivity.getDB().setPageHidden(p.url, config.showHiddenTexts == FragmentConfig.HiddenTexts.NONE ?
                        (direction == ItemTouchHelper.RIGHT ? FragmentConfig.HiddenTexts.GREEN :
                                FragmentConfig.HiddenTexts.RED) :
                        (direction == ItemTouchHelper.RIGHT ? FragmentConfig.HiddenTexts.NONE :
                                (config.showHiddenTexts == FragmentConfig.HiddenTexts.RED ?
                                        FragmentConfig.HiddenTexts.GREEN : FragmentConfig.HiddenTexts.RED)));
                informAllReadTabsAboutUpdate(p.typ);
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
                if (dX > 0) {
                    p.setColor(config.showHiddenTexts == FragmentConfig.HiddenTexts.NONE ?
                            Color.GREEN : Color.GRAY);
                    c.drawRect(viewHolder.itemView.getLeft(), viewHolder.itemView.getTop(), dX,
                            viewHolder.itemView.getBottom(), p);
                } else {
                    p.setColor(config.showHiddenTexts == FragmentConfig.HiddenTexts.NONE ?
                            Color.RED : (config.showHiddenTexts == FragmentConfig.HiddenTexts.RED ?
                            Color.GREEN : Color.RED));
                    c.drawRect(viewHolder.itemView.getRight() + dX, viewHolder.itemView.getTop(),
                            viewHolder.itemView.getRight(), viewHolder.itemView.getBottom(), p);
                }
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        }).attachToRecyclerView(pageList);

        searchView = view.findViewById(R.id.mySearch);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                ArrayList<Page> list = new ArrayList<>();
                String[] toSearch = query.trim().split(" ");
                for (Page p : MainActivity.getDB().getAllPages(config.showHiddenTexts,
                        config.readInfoForReadFragment, config.authorFilter,
                        config.tagFilter)) {
                    File f = p.getCacheFile(MainActivity.getContext());
                    for (String ts : toSearch) {
                        if (Utils.containsText(p.name, ts.trim()) ||
                                Utils.containsText(p.tags, ts.trim()) ||
                                Utils.containsText(p.author, ts.trim())) {
                            list.add(p);
                            f = null;
                            break;
                        }
                    }
                    if (f == null) continue;
                    if (!f.exists()) continue;
                    String s = Utils.readTextFile(f);
                    for (String ts : toSearch) {
                        if (Utils.containsText(s, ts.trim())) {
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
        searchView.findViewById(androidx.appcompat.R.id.search_close_btn).setOnClickListener(v -> {
            searchView.setQuery("", true);
            searchView.clearFocus();
            pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                    config.readInfoForReadFragment,
                    config.authorFilter, config.tagFilter);
        });
        setSearchHintColor();

        mCreateEPUB = registerForActivityResult(
                new ActivityResultContracts.CreateDocument(Utils.EPUB_MIME_TYPE),
                uri -> Utils.createEPUB(MainActivity.getContext(), uri, pageListAdapter.getAllItems(),
                        config.readInfoForReadFragment));

        mImportEPUB = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {
                    Utils.importEPUB(MainActivity.getContext(), uri, MainActivity.getDB());
                    pageListAdapter.update(MainActivity.getDB(), config.showHiddenTexts,
                            config.readInfoForReadFragment,
                            config.authorFilter, config.tagFilter);
                });

        return view;
    }
}
