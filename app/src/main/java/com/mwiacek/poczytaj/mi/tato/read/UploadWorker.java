package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.mwiacek.poczytaj.mi.tato.FragmentConfig;

/* Synchronization for lists in tab in background */
public class UploadWorker extends ListenableWorker {
    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        SettableFuture<Result> future = SettableFuture.create();
        future.set(Result.success());

       /* final ConnectivityManager connMgr = (ConnectivityManager)
                this.getSystemService(Context.CONNECTIVITY_SERVICE);
        final android.net.NetworkInfo wifi = connMgr.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        final android.net.NetworkInfo mobile = connMgr.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);
        if (wifi.isConnectedOrConnecting ()) {
        } else if (mobile.isConnectedOrConnecting ()) {
        } else {
        }*/
        if (getInputData().getInt("TabNum", -1) == -1) return future;
        new Thread(() -> {
            FragmentConfig config = FragmentConfig.readFromInternalStorage(getApplicationContext(),
                    getInputData().getInt("TabNum", -1));
            if (config != null) {
                for (Page.PageTyp typ : config.readInfoForReadFragment) {
                    Page.getReadInfo(typ).getList(null, null, null,
                            new DBHelper(getApplicationContext()), getApplicationContext(), typ,
                            new DBHelper(getApplicationContext()).checkIfTypIsCompletelyRead(typ) ? 3 : -1);
                }
            }
        }).start();
        return future;
    }
}
