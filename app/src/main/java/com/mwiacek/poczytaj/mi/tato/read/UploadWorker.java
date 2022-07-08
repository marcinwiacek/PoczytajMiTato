package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;
import com.mwiacek.poczytaj.mi.tato.read.readinfo.Fantastyka;

public class UploadWorker extends ListenableWorker {
    public UploadWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @SuppressLint("RestrictedApi")
    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        SettableFuture<Result> future = SettableFuture.create();
        new Thread(() -> {
            try {
                new Fantastyka().getList(null, null, null,
                        new DBHelper(getApplicationContext()), getApplicationContext(),
                        Page.PagesTyp.FANTASTYKA_BIBLIOTEKA);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
        future.set(Result.success());
        return future;
    }
}
