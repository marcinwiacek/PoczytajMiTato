package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.os.HandlerCompat;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.mwiacek.poczytaj.mi.tato.FragmentConfig;

/* Synchronization for lists in tab in background */
public class UploadWorker extends ListenableWorker {
    private final Handler mainThreadHandler = HandlerCompat.createAsync(Looper.getMainLooper());

    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        SettableFuture<Result> future = SettableFuture.create();
        future.set(Result.success());

        FragmentConfig config = FragmentConfig.readFromInternalStorage(getApplicationContext(),
                getInputData().getInt("TabNum", -1));
        if (config == null) {
            future.set(Result.failure());
            return future;
        }

        ConnectivityManager mgr = (ConnectivityManager)
                getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        if (!((config.canDownloadWithGSM &&
                mgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting()) ||
                (config.canDownloadWithWifi &&
                        mgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting()) ||
                (config.canDownloadWithOtherNetwork && mgr.getActiveNetworkInfo() != null &&
                        mgr.getActiveNetworkInfo().isConnectedOrConnecting()))) {
            future.set(Result.failure());
            return future;
        }

        new Thread(() -> {
            for (Page.PageTyp t : config.readInfoForReadFragment) {
                //fixme - we should update GUI too
                Page.getReadInfo(t).getList(getApplicationContext(),
                        mainThreadHandler, new DBHelper(getApplicationContext()), t, "",
                        1, 3, null);
            }
        }).start();
        return future;
    }
}
