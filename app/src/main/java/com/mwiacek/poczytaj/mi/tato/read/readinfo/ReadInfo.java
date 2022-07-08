package com.mwiacek.poczytaj.mi.tato.read.readinfo;

import android.content.Context;
import android.os.Handler;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.read.DBHelper;
import com.mwiacek.poczytaj.mi.tato.read.Page;

import java.io.File;
import java.util.concurrent.ThreadPoolExecutor;

public abstract class ReadInfo {
    public abstract String getOpkoFromSinglePage(String result, File f);

    public abstract void getList(final Utils.RepositoryCallback<StringBuilder> callback,
                                 final Utils.RepositoryCallback<StringBuilder> callbackOnTheEnd,
                                 final Handler resultHandler,
                                 final ThreadPoolExecutor executor,
                                 final DBHelper mydb, final Context context, Page.PagesTyp typ);

    public abstract void getList(final Utils.RepositoryCallback<StringBuilder> callback,
                                 final Utils.RepositoryCallback<StringBuilder> callbackOnTheEnd,
                                 final Handler resultHandler,
                                 final DBHelper mydb, final Context context, Page.PagesTyp typ);

}