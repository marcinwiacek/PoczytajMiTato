package com.mwiacek.poczytaj.mi.tato.read;

import android.Manifest;
import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.MimeTypeMap;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.PopupMenu;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
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
import com.mwiacek.poczytaj.mi.tato.Notifications;
import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.ViewPagerAdapter;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.LinkedHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipOutputStream;

public class ReadFragment extends Fragment {
    public final FragmentConfig config;
    private final ViewPagerAdapter topPagrAdapter;
    private PageListListViewAdapter mTaskListAdapter;
    private SwipeRefreshLayout refresh;
    private ThreadPoolExecutor threadPoolExecutor;
    private Handler mainThreadHandler;
    private RecyclerView list;
    private ViewSwitcher mViewSwitcher;
    private WebView vw;
    private DBHelper mydb;
    private String url;
    private int top;

    public ReadFragment(FragmentConfig config, ViewPagerAdapter topPagrAdapter) {
        this.topPagrAdapter = topPagrAdapter;
        this.config = config;
    }

    public void setupRefresh() {
        WorkManager.getInstance(this.getContext()).cancelAllWorkByTag("poczytajmitato" + config.tabNum);
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
                        .putInt("TabNum", config.tabNum)
                        .build())
                .addTag("poczytajmitato" + config.tabNum)
                .build();

        WorkManager.getInstance(this.getContext()).enqueue(request);
    }

    public void onBackPressed() {
        if (list.isShown()) System.exit(0);
        mydb.updateTop(url, vw.getScrollY());
        ((PageListListViewAdapter) list.getAdapter()).update(mydb, config.showHiddenTexts,
                config.readInfoForReadFragment.toArray(
                        new Page.PagesTyp[config.readInfoForReadFragment.size()])
        );
        mViewSwitcher.showPrevious();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.read_fragment, container, false);
        mViewSwitcher = view.findViewById(R.id.viewSwitcher2);
        refresh = view.findViewById(R.id.swiperefresh);
        SwipeRefreshLayout refresh2 = view.findViewById(R.id.swiperefresh2);
        list = view.findViewById(R.id.pagesRecyclerView);
        vw = view.findViewById(R.id.webview);
        AtomicInteger pos = new AtomicInteger();
        Button menu = view.findViewById(R.id.menuButton2);

        mydb = new DBHelper(this.getContext());

        vw.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                vw.scrollTo(0, top);
            }
        });
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(vw.getSettings(),
                    WebSettingsCompat.FORCE_DARK_ON);
        }

        threadPoolExecutor = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                1, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
        mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

        mTaskListAdapter = new PageListListViewAdapter();
        mTaskListAdapter.setOnClick(position -> {
            Page p = ((Page) mTaskListAdapter.getItem(position));
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
                                            new Page.PagesTyp[config.readInfoForReadFragment.size()])
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
                toArray(new Page.PagesTyp[config.readInfoForReadFragment.size()]));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView,
                                  RecyclerView.ViewHolder viewHolder,
                                  RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                mydb.setPageVisible(((Page) ((PageListListViewAdapter) list.getAdapter())
                        .getItem(viewHolder.getAbsoluteAdapterPosition())).url, !config.showHiddenTexts);
                mTaskListAdapter.update(mydb, config.showHiddenTexts,
                        config.readInfoForReadFragment.toArray(
                                new Page.PagesTyp[config.readInfoForReadFragment.size()]));
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
            for (Page.PagesTyp pt : config.readInfoForReadFragment) {
                refresh.setRefreshing(true);
                Page.getReadInfo(pt).getList(
                        result -> mTaskListAdapter.update(mydb, config.showHiddenTexts, config.readInfoForReadFragment
                                .toArray(new Page.PagesTyp[config.readInfoForReadFragment.size()])),
                        result -> refresh.setRefreshing(false),
                        mainThreadHandler, threadPoolExecutor, mydb, this.getContext(), pt);
            }
        });

        /*
        workManager.getWorkInfoByIdLiveData(uploadRequest.id)
            .observe(this, Observer {
                textView.text = it.state.name
                if(it.state.isFinished){
                 val data = it.outputData
                 val message = data.getString(UploadWorker.KEY_WORKER)
                    Toast.makeText(applicationContext,message,Toast.LENGTH_LONG).show()
                }
            })
         */

        refresh2.setOnRefreshListener(() -> {
            Page p = ((Page) mTaskListAdapter.getItem(pos.get()));
            File f = p.getCacheFileName(getContext());
            Utils.getPage(p.url,
                    result -> vw.loadDataWithBaseURL(null,
                            Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f),
                            "text/html; charset=UTF-8", "UFT-8",
                            null),
                    mainThreadHandler, threadPoolExecutor);
            refresh2.setRefreshing(false);
        });

        menu.setOnClickListener(view1 -> {
            LinkedHashMap<String, Page.PagesTyp> hm = new LinkedHashMap<String, Page.PagesTyp>() {{
                put("fantastyka.pl, archiwum", Page.PagesTyp.FANTASTYKA_ARCHIWUM);
                put("fantastyka.pl, biblioteka", Page.PagesTyp.FANTASTYKA_BIBLIOTEKA);
                put("fantastyka.pl, poczekalnia", Page.PagesTyp.FANTASTYKA_POCZEKALNIA);
                put("opowi.pl, autorzy", Page.PagesTyp.OPOWI_AUTORZY);
                put("opowi.pl, fantastyka", Page.PagesTyp.OPOWI_FANTASTYKA);
            }};
            int i = 0;
            int mainIndex = 0;
            PopupMenu popupMenu = new PopupMenu(getContext(), menu);
            popupMenu.getMenu().add(1, i++, mainIndex++, "Pokaż ukryte").setCheckable(true)
                    .setChecked(config.showHiddenTexts);
            popupMenu.getMenu().add(2, i++, mainIndex++, "Używaj TOR")
                    .setActionView(R.layout.checkbox_layout).setCheckable(true).setChecked(config.useTOR);
            popupMenu.getMenu().add(2, i++, mainIndex++, "Zawsze pobieraj teksty")
                    .setActionView(R.layout.checkbox_layout).setCheckable(true).setChecked(config.getTextsWhenRefreshingIndex);
            for (String s : hm.keySet()) {
                popupMenu.getMenu().add(2, i++, mainIndex++, s).
                        setActionView(R.layout.checkbox_layout).setCheckable(true).setChecked(
                                config.readInfoForReadFragment.contains(hm.get(s)));
            }
            popupMenu.getMenu().add(2, i++, mainIndex++, "Lokalny filtr na autorów").setActionView(R.layout.checkbox_layout).setCheckable(true);
            popupMenu.getMenu().add(2, i++, mainIndex++, "Lokalny filtr na tagi").setActionView(R.layout.checkbox_layout).setCheckable(true);
            popupMenu.getMenu().add(3, i++, mainIndex++, "Pobierz niepobrane teksty");
            popupMenu.getMenu().add(3, i++, mainIndex++, "EPUB z pokazanych i pobranych");
            popupMenu.getMenu().add(3, i++, mainIndex++, "Klonuj zakładkę");
            popupMenu.getMenu().add(3, i++, mainIndex++, "Usuń zakładkę");
            popupMenu.getMenu().add(4, i++, mainIndex++,
                            "Pobierz co " + (config.howOftenRefreshTabInHours == -1 ? "x" : config.howOftenRefreshTabInHours) + " godzin").setActionView(R.layout.checkbox_layout).setCheckable(true)
                    .setChecked(config.howOftenRefreshTabInHours != -1);
            popupMenu.getMenu().add(4, i++, mainIndex++,
                            "Przy błędzie co " + (config.howOftenTryToRefreshTabAfterErrorInMinutes == -1 ? "x" : config.howOftenTryToRefreshTabAfterErrorInMinutes) + " minut").setActionView(R.layout.checkbox_layout)
                    .setCheckable(true).setChecked(config.howOftenTryToRefreshTabAfterErrorInMinutes != -1).setEnabled(config.howOftenRefreshTabInHours != -1);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Pobierz na Wifi").setActionView(R.layout.checkbox_layout).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.pobierzPrzyWifi);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Pobierz na GSM").setActionView(R.layout.checkbox_layout).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.pobierzPrzyGSM);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Pobierz na innej sieci").setActionView(R.layout.checkbox_layout).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.pobierzPrzyInnejSieci);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Pobierz tylko przy ładowaniu").setActionView(R.layout.checkbox_layout).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.pobierzTylkoPrzyLadowaniu);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Nie pobieraj przy niskiej baterii").setActionView(R.layout.checkbox_layout).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.niePobierajPrzyNiskiejBaterii);
            popupMenu.getMenu().add(4, i++, mainIndex++, "Sieć musi być bez limitu").setActionView(R.layout.checkbox_layout).setCheckable(true)
                    .setEnabled(config.howOftenRefreshTabInHours != -1).setChecked(config.networkWithoutLimit);
            popupMenu.getMenu().add(5, i++, mainIndex++, "Napisz maila do autora");
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
                            .toArray(new Page.PagesTyp[config.readInfoForReadFragment.size()]));
                    return true;
                }
                if (item.getTitle().toString().startsWith("Pobierz co")) {
                    item.setChecked(!item.isChecked());
                    if (item.isChecked()) {
                        EditText input = new EditText(this.getContext());
                        input.setRawInputType(InputType.TYPE_CLASS_NUMBER);
                        new AlertDialog.Builder(this.getContext())
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
                        new AlertDialog.Builder(this.getContext())
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
                    new AlertDialog.Builder(this.getContext())
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
                    new AlertDialog.Builder(this.getContext())
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
                            .toArray(new Page.PagesTyp[config.readInfoForReadFragment.size()]));
                    config.saveToInternalStorage(getContext());
                }
                if (item.isCheckable()) {
                    item.setChecked(!item.isChecked());
                }
                if (item.getTitle().equals("Pobierz na Wifi")) {
                    config.pobierzPrzyWifi = item.isChecked();
                }
                if (item.getTitle().equals("Pobierz na GSM")) {
                    config.pobierzPrzyGSM = item.isChecked();
                }
                if (item.getTitle().equals("Pobierz na innej sieci")) {
                    config.pobierzPrzyInnejSieci = item.isChecked();
                }
                if (item.getTitle().equals("Pobierz tylko przy ładowaniu")) {
                    config.pobierzTylkoPrzyLadowaniu = item.isChecked();
                }
                if (item.getTitle().equals("Nie pobieraj przy niskiej baterii")) {
                    config.niePobierajPrzyNiskiejBaterii = item.isChecked();
                }
                if (item.getTitle().equals("Sieć musi być bez limitu")) {
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
                        Page p = ((Page) mTaskListAdapter.getItem(j));
                        File f = p.getCacheFileName(this.getContext());
                        if (f.exists()) continue;
                        Utils.getPage(p.url, result -> {
                                    Page.getReadInfo(p.typ).getOpkoFromSinglePage(result.toString(), f);
                                    mTaskListAdapter.update(mydb, config.showHiddenTexts,
                                            config.readInfoForReadFragment
                                                    .toArray(new Page.PagesTyp[config.readInfoForReadFragment.size()]));
                                },
                                mainThreadHandler, threadPoolExecutor);
                    }
                }
                if (item.getTitle().equals("EPUB z pokazanych i pobranych")) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (ContextCompat.checkSelfPermission(getContext(),
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_DENIED) {
                            ActivityCompat.requestPermissions((Activity) getContext(),
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        }
                        if (ContextCompat.checkSelfPermission(getContext(),
                                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                == PackageManager.PERMISSION_DENIED) {
                            return true;
                        }
                    }
                    NotificationCompat.Builder builder =
                            Notifications.setupNotification(Notifications.Channels.ZAPIS, getContext(),
                                    "Tworzenie pliku EPUB");
                    Notifications.notificationManager(getContext()).notify(2, builder.build());
                    try {
                        int z = 0;
                        String longFileName;
                        String shortFileName;

                        while (true) {
                            longFileName = Environment.getExternalStoragePublicDirectory(
                                    Environment.DIRECTORY_DOWNLOADS).getPath() + File.separator +
                                    config.tabName.replaceAll("[^A-Za-z0-9]", "") +
                                    (z == 0 ? "" : z) + ".zip";
                            File f = new File(longFileName);
                            if (!f.exists()) break;
                            z++;
                        }

                        shortFileName = config.tabName.replaceAll("[^A-Za-z0-9]", "") +
                                (z == 0 ? "" : z) + ".zip";
                        FileOutputStream dest = new FileOutputStream(longFileName);
                        ZipOutputStream out = new ZipOutputStream(new BufferedOutputStream(dest));

                        for (int j = 0; j < mTaskListAdapter.getItemCount(); j++) {
                            File f = (((Page) mTaskListAdapter.getItem(j)).getCacheFileName(this.getContext()));
                            if (f.exists()) {
                                Utils.addZipFile("OEBPS\\" + j + ".html", out, f);
                            }
                        }

                        Utils.addZipFile("mimetype", out, "application/epub+zip");
                        Utils.addZipFile("META-INF\\container.xml",
                                out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<container version=\"1.0\" xmlns=\"urn:oasis:names:tc:opendocument:xmlns:container\">\n" +
                                        "<rootfiles>\n" +
                                        "<rootfile full-path=\"OEBPS/content.opf\" media-type=\"application/oebps-package+xml\"/>\n" +
                                        "</rootfiles>\n" +
                                        "</container>");
                        Utils.addZipFile("OEBPS\\style.css", out, "body {text-align:justify}");

                        Utils.addZipFile("OEBPS\\toc.ncx", out,
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<ncx xmlns=\"http://www.daisy.org/z3986/2005/ncx/\"\n" +
                                        "xmlns:py=\"http://genshi.edgewall.org/\"\n" +
                                        "version=\"2005-1\"\n" +
                                        "xml:lang=\"pl\">\n" +
                                        "<head>\n" +
                                        "<meta name=\"cover\" content=\"cover\"/>\n" +
                                        "<meta name=\"dtb:uid\" content=\"urn:uuid:e5953946-ea06-4599-9a53-f5c652b89f5c\"/>\n" +
                                        "<meta name=\"dtb:depth\" content=\"1\"/>\n" +
                                        "<meta name=\"dtb:totalPageCount\" content=\"0\"/>\n" +
                                        "<meta name=\"dtb:maxPageNumber\" content=\"0\"/>\n" +
                                        "</head>\n" +
                                        "<docTitle>\n" +
                                        "<text>tytul</text>\n" +
                                        "</docTitle>\n" +
                                        "<navMap>\n" +
                                        //$tocTocNCX.
                                        "</navMap>\n" +
                                        "</ncx>\n");

                        Utils.addZipFile("OEBPS\\toc.xhtml", out,
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.1//EN\" \"http://www.w3.org/TR/xhtml11/DTD/xhtml11.dtd\">\n" +
                                        "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n" +
                                        "xmlns:epub=\"http://www.idpf.org/2007/ops\"\n" +
                                        "xml:lang=\"pl\" lang=\"pl\">\n" +
                                        "<head>\n" +
                                        "<title>title</title>\n" +
                                        "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />\n" +
                                        "</head>\n" +
                                        "<body xml:lang=\"pl\" lang=\"pl\">\n" +
                                        "<header>\n" +
                                        "<h2>Spis treści</h2>\n" +
                                        "</header>\n" +
                                        "<nav epub:type=\"toc\">\n" +
                                        "<ol>\n" +
                                        //$tocTocXHTML.
                                        "</ol>\n" +
                                        "</nav>\n" +
                                        "</body>\n" +
                                        "</html>");

                        Utils.addZipFile("OEBPS\\content.opf", out, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                "<package xmlns=\"http://www.idpf.org/2007/opf\"\n" +
                                "xmlns:opf=\"http://www.idpf.org/2007/opf\"\n" +
                                "xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n" +
                                "unique-identifier=\"bookid\"\n" +
                                "version=\"3.0\"\n" +
                                "xml:lang=\"pl\">\n" +
                                "<metadata>\n" +
                                "<dc:identifier id=\"bookid\">urn:uuid:e5953946-ea06-4599-9a53-f5c652b89f5c</dc:identifier>\n" +
                                "<dc:language>pl-PL</dc:language>\n" +
                                "<meta name=\"generator\" content=\"Skrypt z mwiacek.com\"/>\n" +
                                "<dc:title>title</dc:title>\n" +
                                "<dc:description>\n" +
                                "title\n" +
                                "</dc:description>\n" +
                                "<dc:creator id=\"creator-0\">A.zbiorowy+skrypt z mwiacek.com</dc:creator>\n" +
                                "<meta refines=\"#creator-0\" property=\"role\" scheme=\"marc:relators\">aut</meta>\n" +
                                "<meta refines=\"#creator-0\" property=\"file-as\">A.zbiorowy+skrypt z mwiacek.com</meta>\n" +
                                "<meta name=\"cover\" content=\"cover\"></meta>\n" +
                                "<meta property=\"dcterms:modified\"></meta>\n" +
                                "</metadata>\n" +
                                "<manifest>\n" +
                                "<item id=\"style_css\" media-type=\"text/css\" href=\"style.css\" />\n" +
                                "<item id=\"cover\" media-type=\"image/jpeg\" href=\"cover$set.jpg\" properties=\"cover-image\" />\n" +
                                "<item id=\"cover-page_xhtml\" media-type=\"application/xhtml+xml\" href=\"cover-page.xhtml\" />\n" +
                                "<item id=\"toc_xhtml\" media-type=\"application/xhtml+xml\" href=\"toc.xhtml\" properties=\"nav\" />\n" +
                                //        $tocContentOpf1.
                                "<item id=\"ncxtoc\" media-type=\"application/x-dtbncx+xml\" href=\"toc.ncx\" />\n" +
                                "</manifest>\n" +
                                "<spine toc=\"ncxtoc\">\n" +
                                "<itemref idref=\"cover-page_xhtml\" linear=\"no\"/>\n" +
                                "<itemref idref=\"toc_xhtml\"/>\n" +
                                //$tocContentOpf2.
                                "</spine>\n" +
                                "<guide>\n" +
                                "<reference href=\"cover-page.xhtml\" type=\"cover\" title=\"Strona okładki\"/>\n" +
                                "<reference href=\"toc.xhtml\" type=\"toc\" title=\"Spis treści\"/>\n" +
                                //$tocContentOpf3.
                                "</guide>\n" +
                                "</package>");

                        Utils.addZipFile("OEBPS\\cover-page.xhtml", out,
                                "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                                        "<!DOCTYPE html>\n" +
                                        "<html xmlns=\"http://www.w3.org/1999/xhtml\"\n" +
                                        "xmlns:epub=\"http://www.idpf.org/2007/ops\"\n" +
                                        "xml:lang=\"pl\" lang=\"pl\">\n" +
                                        "<head>\n" +
                                        "<title>title</title>\n" +
                                        "<link rel=\"stylesheet\" href=\"style.css\" type=\"text/css\" />\n" +
                                        "</head>\n" +
                                        "<body xml:lang=\"pl\" lang=\"pl\">\n" +
                                        "<div>\n" +
                                        "<img src=\"cover$set.jpg\"/>\n" +
                                        "</div>\n" +
                                        "</body>\n" +
                                        "</html>");

                        out.close();

                        Intent intent = new Intent();
                        intent.setDataAndType(Uri.fromFile(new File(longFileName)),
                                MimeTypeMap.getSingleton().getMimeTypeFromExtension(".ZIP"));
                        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        PendingIntent pendingIntent = null;
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                            pendingIntent = PendingIntent.getActivity(getContext(),
                                    0, intent, PendingIntent.FLAG_IMMUTABLE);
                        }

                        builder.setContentText("Zapisano plik EPUB " + shortFileName).setContentIntent(pendingIntent);
                        Notifications.notificationManager(getContext()).notify(2, builder.build());
                    } catch (Exception e) {
                        builder.setProgress(0, 0, false);
                        Notifications.notificationManager(getContext()).notify(2, builder.build());

                        builder.setContentText("Błąd zapisu pliku EPUB");
                        Notifications.notificationManager(getContext()).notify(2, builder.build());

                        e.printStackTrace();
                    }
                }
                return false;
            });
            popupMenu.show();
        });
        return view;
    }
}
