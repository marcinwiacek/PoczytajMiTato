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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
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
import java.util.concurrent.atomic.AtomicInteger;

public class ReadFragment extends Fragment {
    private final FragmentConfig config;
    private final ViewPagerAdapter topPagrAdapter;
    private final DBHelper mydb;
    private final AtomicInteger pos = new AtomicInteger();
    private final ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors(),
            1, TimeUnit.SECONDS,
            new LinkedBlockingQueue<>()
    );
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());
    private PageListRecyclerViewAdapter mTaskListAdapter;
    private SwipeRefreshLayout refresh;
    private RecyclerView list;
    private ViewSwitcher mViewSwitcher;
    private WebView vw;
    private String url;
    private int top;

    public ReadFragment(FragmentConfig config, ViewPagerAdapter topPagrAdapter, DBHelper mydb) {
        this.topPagrAdapter = topPagrAdapter;
        this.config = config;
        this.mydb = mydb;
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
                        .putInt("TabNum", config.tabNumForFileForSerialization)
                        .build())
                .addTag("poczytajmitato" + config.tabNumForFileForSerialization)
                .build();

        WorkManager.getInstance(this.requireContext()).enqueue(request);
    }

    public void onBackPressed() {
        if (list.isShown()) System.exit(0);
        mydb.updateTop(url, vw.getScrollY());
        mTaskListAdapter.update(mydb, config.showHiddenTexts, config.readInfoForReadFragment.toArray(
                new Page.PageTyp[config.readInfoForReadFragment.size()])
        );
        mViewSwitcher.showPrevious();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.read_fragment, container, false);
        mViewSwitcher = view.findViewById(R.id.viewSwitcher2);

        /* Page with webview */
        SwipeRefreshLayout refresh2 = view.findViewById(R.id.swiperefresh2);
        vw = view.findViewById(R.id.webview);

        vw.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                vw.scrollTo(0, top);
            }
        });
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(vw.getSettings(),
                    WebSettingsCompat.FORCE_DARK_ON);
        }

        refresh2.setOnRefreshListener(() -> {
            Page p = mTaskListAdapter.getItem(pos.get());
            File f = p.getCacheFileName(getContext());
            Utils.getPage(p.url,
                    result -> vw.loadDataWithBaseURL(null,
                            Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f),
                            "text/html; charset=UTF-8", "UFT-8",
                            null),
                    mainThreadHandler, threadPoolExecutor);
            refresh2.setRefreshing(false);
        });

        /* Page with list */
        Button menu = view.findViewById(R.id.menuButton2);
        refresh = view.findViewById(R.id.swiperefresh);
        list = view.findViewById(R.id.pagesRecyclerView);

        mTaskListAdapter = new PageListRecyclerViewAdapter();
        mTaskListAdapter.setOnClick(position -> {
            Page p = mTaskListAdapter.getItem(position);
            File f = p.getCacheFileName(this.getContext());
            top = p.top;
            url = p.url;
            if (f.exists()) {
                vw.loadDataWithBaseURL(null, Utils.readFile(f),
                        "text/html; charset=UTF-8", "UFT-8", null);
            } else {
                vw.loadDataWithBaseURL(null, "Loading file " + p.url,
                        "text/html; charset=UTF-8",
                        "UFT-8", null);
                Utils.getPage(p.url, result -> {
                            mTaskListAdapter.update(mydb, config.showHiddenTexts,
                                    config.readInfoForReadFragment.toArray(
                                            new Page.PageTyp[config.readInfoForReadFragment.size()])
                            );
                            vw.loadDataWithBaseURL(null,
                                    Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f),
                                    "text/html; charset=UTF-8", "UFT-8",
                                    null);
                        },
                        mainThreadHandler, threadPoolExecutor);
            }
            mViewSwitcher.showNext();
        });
        list.setAdapter(mTaskListAdapter);
        mTaskListAdapter.update(mydb, config.showHiddenTexts, config.readInfoForReadFragment.
                toArray(new Page.PageTyp[config.readInfoForReadFragment.size()]));

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
                mydb.setPageVisible(mTaskListAdapter
                        .getItem(viewHolder.getAbsoluteAdapterPosition()).url, !config.showHiddenTexts);
                mTaskListAdapter.update(mydb, config.showHiddenTexts,
                        config.readInfoForReadFragment.toArray(
                                new Page.PageTyp[config.readInfoForReadFragment.size()]));
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
        itemTouchHelper.attachToRecyclerView(list);

        refresh.setOnRefreshListener(() -> {
            for (Page.PageTyp pt : config.readInfoForReadFragment) {
                refresh.setRefreshing(true);
                Page.getReadInfo(pt).getList(
                        result -> mTaskListAdapter.update(mydb, config.showHiddenTexts, config.readInfoForReadFragment
                                .toArray(new Page.PageTyp[config.readInfoForReadFragment.size()])),
                        result -> refresh.setRefreshing(false),
                        mainThreadHandler, threadPoolExecutor, mydb, this.getContext(), pt);
            }
        });

        menu.setOnClickListener(view1 -> {
            LinkedHashMap<String, Page.PageTyp> hm = new LinkedHashMap<String, Page.PageTyp>() {{
                put("fantastyka.pl, archiwum", Page.PageTyp.FANTASTYKA_ARCHIWUM);
                put("fantastyka.pl, biblioteka", Page.PageTyp.FANTASTYKA_BIBLIOTEKA);
                put("fantastyka.pl, poczekalnia", Page.PageTyp.FANTASTYKA_POCZEKALNIA);
                put("opowi.pl, autorzy", Page.PageTyp.OPOWI_AUTORZY);
                put("opowi.pl, fantastyka", Page.PageTyp.OPOWI_FANTASTYKA);
            }};
            int i = 0;
            int mainIndex = 0;
            PopupMenu popupMenu = new PopupMenu(requireContext(), menu);
            popupMenu.getMenu().add(1, i++, mainIndex++, "Pokaż ukryte").setCheckable(true)
                    .setChecked(config.showHiddenTexts);
            popupMenu.getMenu().add(2, i++, mainIndex++, "Używaj TOR")
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true).setChecked(config.useTOR);
            popupMenu.getMenu().add(2, i++, mainIndex++, "Zawsze pobieraj teksty")
                    .setActionView(R.layout.checkbox_menu_item).setCheckable(true).setChecked(config.getTextsWhenRefreshingIndex);
            for (String s : hm.keySet()) {
                popupMenu.getMenu().add(2, i++, mainIndex++, s).
                        setActionView(R.layout.checkbox_menu_item).setCheckable(true).setChecked(
                                config.readInfoForReadFragment.contains(hm.get(s)));
            }
            popupMenu.getMenu().add(2, i++, mainIndex++, "Lokalny filtr na autorów").setActionView(R.layout.checkbox_menu_item).setCheckable(true);
            popupMenu.getMenu().add(2, i++, mainIndex++, "Lokalny filtr na tagi").setActionView(R.layout.checkbox_menu_item).setCheckable(true);
            popupMenu.getMenu().add(3, i++, mainIndex++, "Pobierz niepobrane teksty");
            popupMenu.getMenu().add(3, i++, mainIndex++, "EPUB z pokazanych i pobranych");
            popupMenu.getMenu().add(3, i++, mainIndex++, "Klonuj zakładkę");
            popupMenu.getMenu().add(3, i++, mainIndex++, "Usuń zakładkę");
            popupMenu.getMenu().add(4, i++, mainIndex++,
                            "Pobierz co " + (config.howOftenRefreshTabInHours == -1 ? "x" : config.howOftenRefreshTabInHours) + " godzin").setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setChecked(config.howOftenRefreshTabInHours != -1);
            popupMenu.getMenu().add(4, i++, mainIndex++,
                            "Przy błędzie co " + (config.howOftenTryToRefreshTabAfterErrorInMinutes == -1 ? "x" : config.howOftenTryToRefreshTabAfterErrorInMinutes) + " minut").setActionView(R.layout.checkbox_menu_item)
                    .setCheckable(true).setChecked(config.howOftenTryToRefreshTabAfterErrorInMinutes != -1).setEnabled(config.howOftenRefreshTabInHours != -1);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Pobierz na Wifi").setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.pobierzPrzyWifi);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Pobierz na GSM").setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.pobierzPrzyGSM);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Pobierz na innej sieci").setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.pobierzPrzyInnejSieci);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Pobierz tylko przy ładowaniu").setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.pobierzTylkoPrzyLadowaniu);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Nie pobieraj przy niskiej baterii").setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.niePobierajPrzyNiskiejBaterii);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Sieć musi być bez limitu").setActionView(R.layout.checkbox_menu_item).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.networkWithoutLimit);
            popupMenu.getMenu().add(5, i, mainIndex, "Napisz maila do autora");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                popupMenu.getMenu().setGroupDividerEnabled(true);
            }

            popupMenu.setOnMenuItemClickListener(item -> {
                for (String s : hm.keySet()) {
                    if (!item.getTitle().equals(s)) continue;
                    item.setChecked(!item.isChecked());
                    if (item.isChecked()) {
                        config.readInfoForReadFragment.add(hm.get(s));
                    } else {
                        config.readInfoForReadFragment.remove(hm.get(s));
                    }
                    config.saveToInternalStorage(getContext());
                    mTaskListAdapter.update(mydb, config.showHiddenTexts, config.readInfoForReadFragment
                            .toArray(new Page.PageTyp[config.readInfoForReadFragment.size()]));
                    return true;
                }
                if (item.getTitle().toString().startsWith("Pobierz co")) {
                    item.setChecked(!item.isChecked());
                    if (item.isChecked()) {
                        EditText input = new EditText(this.getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        new AlertDialog.Builder(this.requireContext())
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Poczytaj mi tato")
                                .setMessage("Co ile godzin zakładka ma być odświeżana")
                                .setView(input)
                                .setPositiveButton("OK", (dialog, which) -> {
                                    config.howOftenRefreshTabInHours = Integer.parseInt(String.valueOf(input.getText()));
                                    setupRefresh();
                                    config.saveToInternalStorage(getContext());
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> {
                                    dialog.dismiss();
                                    config.howOftenRefreshTabInHours = -1;
                                    setupRefresh();
                                    config.saveToInternalStorage(getContext());
                                })
                                .show();
                    } else {
                        config.howOftenRefreshTabInHours = -1;
                        setupRefresh();
                        config.saveToInternalStorage(getContext());
                    }
                    return true;
                }
                if (item.getTitle().toString().startsWith("Przy błędzie co ")) {
                    item.setChecked(!item.isChecked());
                    if (item.isChecked()) {
                        EditText input = new EditText(this.getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        new AlertDialog.Builder(this.requireContext())
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setTitle("Poczytaj mi tato")
                                .setMessage("Co ile minut próbować pobrać indeks po pierwszym niepowodzeniu")
                                .setView(input)
                                .setPositiveButton("OK", (dialog, which) -> {
                                    config.howOftenTryToRefreshTabAfterErrorInMinutes = Integer.parseInt(String.valueOf(input.getText()));
                                    config.saveToInternalStorage(getContext());
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> {
                                    dialog.dismiss();
                                    config.howOftenTryToRefreshTabAfterErrorInMinutes = -1;
                                    config.saveToInternalStorage(getContext());
                                })
                                .show();
                    } else {
                        config.howOftenTryToRefreshTabAfterErrorInMinutes = -1;
                        config.saveToInternalStorage(getContext());
                    }
                    return true;
                }
                if (item.getTitle().equals("Klonuj zakładkę")) {
                    EditText input = new EditText(this.getContext());
                    new AlertDialog.Builder(this.requireContext())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Poczytaj mi tato")
                            .setMessage("Nazwa nowej zakładki")
                            .setView(input)
                            .setPositiveButton("OK", (dialog, which) -> {
                                try {
                                    topPagrAdapter.addTab(config, input.getText().toString());
                                } catch (CloneNotSupportedException e) {
                                    e.printStackTrace();
                                }
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .show();
                    return true;
                }
                if (item.getTitle().equals("Usuń zakładkę")) {
                    new AlertDialog.Builder(this.requireContext())
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setTitle("Poczytaj mi tato")
                            .setMessage("Czy chcesz usunąć zakładkę?")
                            .setPositiveButton("OK", (dialog, which) -> {
                                topPagrAdapter.deleteTab(config);
                            })
                            .setNegativeButton("Cancel", (dialog, which) -> {
                                dialog.dismiss();
                            })
                            .show();
                    return true;
                }
                if (item.getTitle().equals("Pokaż ukryte")) {
                    config.showHiddenTexts = !config.showHiddenTexts;
                    mTaskListAdapter.update(mydb, config.showHiddenTexts, config.readInfoForReadFragment
                            .toArray(new Page.PageTyp[config.readInfoForReadFragment.size()]));
                    config.saveToInternalStorage(getContext());
                }
                if (item.isCheckable()) {
                    item.setChecked(!item.isChecked());
                }
                if (item.getTitle().equals("Pobierz na Wifi")) {
                    config.pobierzPrzyWifi = item.isChecked();
                } else if (item.getTitle().equals("Pobierz na GSM")) {
                    config.pobierzPrzyGSM = item.isChecked();
                } else if (item.getTitle().equals("Pobierz na innej sieci")) {
                    config.pobierzPrzyInnejSieci = item.isChecked();
                } else if (item.getTitle().equals("Pobierz tylko przy ładowaniu")) {
                    config.pobierzTylkoPrzyLadowaniu = item.isChecked();
                } else if (item.getTitle().equals("Nie pobieraj przy niskiej baterii")) {
                    config.niePobierajPrzyNiskiejBaterii = item.isChecked();
                } else if (item.getTitle().equals("Sieć musi być bez limitu")) {
                    config.networkWithoutLimit = item.isChecked();
                }
                if (item.isCheckable()) {
                    config.saveToInternalStorage(getContext());
                    return true;
                }
                if (item.getTitle().equals("Napisz maila do autora")) {
                    Utils.contactMe(getContext());
                }
                if (item.getTitle().equals("Pobierz niepobrane")) {
                    for (int j = 0; j < mTaskListAdapter.getItemCount(); j++) {
                        Page p = mTaskListAdapter.getItem(j);
                        File f = p.getCacheFileName(this.getContext());
                        if (f.exists()) continue;
                        Utils.getPage(p.url, result -> {
                                    Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f);
                                    mTaskListAdapter.update(mydb, config.showHiddenTexts,
                                            config.readInfoForReadFragment
                                                    .toArray(new Page.PageTyp[config.readInfoForReadFragment.size()]));
                                },
                                mainThreadHandler, threadPoolExecutor);
                    }
                }
                if (item.getTitle().equals("EPUB z pokazanych i pobranych")) {
                    Utils.createEPUB(getContext(), config.tabName, mTaskListAdapter.getAllItems());
                }
                return false;
            });
            popupMenu.show();
        });
        return view;
    }
}
