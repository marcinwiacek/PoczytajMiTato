package com.mwiacek.poczytaj.mi.tato.read.readinfo;

import android.content.Context;
import android.os.Handler;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;

import java.util.concurrent.ThreadPoolExecutor;

public abstract class ReadInfo {

    public abstract void processTextFromSinglePage(
            Context context, Page p, final Handler resultHandler,
            final ThreadPoolExecutor executor,
            final Utils.RepositoryCallback<Integer> readingCallback,
            final Utils.RepositoryCallback<Void> errorCallback,
            final Utils.RepositoryCallback<String[]> callbackAfterMainFile,
            final Utils.RepositoryCallback<String> callbackAfterEveryImage,
            final Utils.RepositoryCallback<String> completeCallback);

    public abstract void getList(final Context context,
                                 final Handler resultHandler,
                                 final DBHelper mydb, Page.PageTyp typ,
                                 String tabName, int tabNum, int pageStart, int pageStop,
                                 final Utils.RepositoryCallback<Page.PageTyp> callback);
}