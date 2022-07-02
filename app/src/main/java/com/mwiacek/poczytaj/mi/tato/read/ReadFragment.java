package com.mwiacek.poczytaj.mi.tato.read;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.activity.OnBackPressedCallback;
import androidx.core.os.HandlerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.mwiacek.poczytaj.mi.tato.R;
import com.mwiacek.poczytaj.mi.tato.Utils;

import java.io.File;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ReadFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.read_fragment, container, false);
        ViewSwitcher mViewSwitcher = view.findViewById(R.id.viewSwitcher2);
        RecyclerView list = view.findViewById(R.id.pagesRecyclerView);
        SwipeRefreshLayout refresh = view.findViewById(R.id.swiperefresh);
        SwipeRefreshLayout refresh2 = view.findViewById(R.id.swiperefresh2);
        WebView vw = view.findViewById(R.id.webview);
        AtomicInteger pos = new AtomicInteger();

        DBHelper mydb = new DBHelper(this.getContext());

        vw.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                view.scrollTo(0, 0);
                Log.i("MW", "finish");
            }
        });

        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors(),
                1, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>()
        );
        Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

        requireActivity().getOnBackPressedDispatcher().addCallback(
                getViewLifecycleOwner(), new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        if (list.isShown()) System.exit(0);
                        mViewSwitcher.showPrevious();
                    }
                });

        PageListListViewAdapter mTaskListAdapter = new PageListListViewAdapter();
        mTaskListAdapter.setOnClick(position -> {
            Page p = ((Page) mTaskListAdapter.getItem(position));
            File f = p.getCacheFileName(this.getContext());
            if (f.exists()) {
                vw.loadDataWithBaseURL(null, Utils.readFile(f),
                        "text/html; charset=UTF-8", "UFT-8", null);
            } else {
                vw.loadDataWithBaseURL(null, "Loading file "
                                + p.url,
                        "text/html; charset=UTF-8",
                        "UFT-8", null);
                Utils.getPage(p.url,
                        result -> {
                            mTaskListAdapter.update(mydb);
                            vw.loadDataWithBaseURL(null,
                                    Fantastyka.getOpko(result.toString(), f),
                                    "text/html; charset=UTF-8", "UFT-8",
                                    null);
                        },
                        mainThreadHandler, threadPoolExecutor);
            }
            mViewSwitcher.showNext();
        });
        list.setAdapter(mTaskListAdapter);
        mTaskListAdapter.update(mydb);

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(0,
                        ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
                    @Override
                    public boolean onMove(RecyclerView recyclerView,
                                          RecyclerView.ViewHolder viewHolder,
                                          RecyclerView.ViewHolder target) {
                        return false;
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder,
                                         int direction) {
                        Page p = ((Page) ((PageListListViewAdapter) list.getAdapter())
                                .getItem(viewHolder.getAdapterPosition()));
//                                direction == ItemTouchHelper.RIGHT) {
//                        } else if (direction == ItemTouchHelper.LEFT) {
                    }

                    /**
                     * Create color background during swipe
                     */
                    @Override
                    public void onChildDraw(Canvas c, RecyclerView recyclerView,
                                            RecyclerView.ViewHolder viewHolder,
                                            float dX, float dY, int actionState,
                                            boolean isCurrentlyActive) {
                        if (actionState != ItemTouchHelper.ACTION_STATE_SWIPE) {
                            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                            return;
                        }
                        //TODO: Showing startTaskImageView and snoozeTaskImageView from layout
                        Paint p = new Paint();
                        p.setColor(Color.GREEN);
                        if (dX > 0) {
                            c.drawRect((float) viewHolder.itemView.getLeft(),
                                    (float) viewHolder.itemView.getTop(), dX,
                                    (float) viewHolder.itemView.getBottom(), p);
                        } else {
                            /*if (mTaskListBottomNavigationView.getSelectedItemId()
                                    == R.id.navigation_pending) {
                                p.setColor(Color.YELLOW);
                            }*/
                            c.drawRect((float) viewHolder.itemView.getRight() + dX,
                                    (float) viewHolder.itemView.getTop(),
                                    (float) viewHolder.itemView.getRight(),
                                    (float) viewHolder.itemView.getBottom(), p);
                        }
                        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                    }
                };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(list);

        refresh.setOnRefreshListener(() -> Fantastyka.getList(
                result -> mTaskListAdapter.update(mydb),
                result -> refresh.setRefreshing(false),
                mainThreadHandler, threadPoolExecutor, mydb));

        refresh2.setOnRefreshListener(() -> {
            Page p = ((Page) mTaskListAdapter.getItem(pos.get()));
            Toast.makeText(getContext(), "ala" + pos.get(), Toast.LENGTH_LONG);
            Log.i("MW", "ala" + pos.get());
            if (p != null) {
                Log.i("MW", "reading");
                Toast.makeText(getContext(), "reading", Toast.LENGTH_LONG);
                File f = p.getCacheFileName(getContext());
                Utils.getPage(p.url,
                        result -> vw.loadDataWithBaseURL(null,
                                Fantastyka.getOpko(result.toString(), f),
                                "text/html; charset=UTF-8", "UFT-8",
                                null),
                        mainThreadHandler, threadPoolExecutor);
            }
            refresh2.setRefreshing(false);
        });

        return view;
    }

}
