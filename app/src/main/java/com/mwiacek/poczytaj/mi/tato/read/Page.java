package com.mwiacek.poczytaj.mi.tato.read;

import android.content.Context;
import android.os.Handler;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.read.readinfo.Fantastyka;
import com.mwiacek.poczytaj.mi.tato.read.readinfo.Opowi;
import com.mwiacek.poczytaj.mi.tato.read.readinfo.ReadInfo;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ThreadPoolExecutor;

public class Page {
    public final static String[] SUPPORTED_IMAGE_EXTENSIONS = {"png", "gif", "jpg", "jpeg"};
    public final static String CACHE_SUB_DIRECTORY = "pages";

    public String name;
    public String author;
    public String tags;
    public String url;
    public Date dt;
    public PageTyp typ;
    public boolean updatedOnServer;

    Page(String name, String author, String comments, String url, Date dt, PageTyp typ, boolean updatedOnServer) {
        this.name = name;
        this.author = author;
        this.tags = comments;
        this.url = url;
        this.dt = dt;
        this.typ = typ;
        this.updatedOnServer = updatedOnServer;
    }

    public static ReadInfo getReadInfo(Page.PageTyp t) {
        return t.name().toLowerCase().startsWith("fantastyka_") ? new Fantastyka() : new Opowi();
    }

    public static void getList(Context context,
                               final Handler resultHandler,
                               final ThreadPoolExecutor executor,
                               final DBHelper mydb, final ArrayList<PageTyp> typ,
                               String tabName, int tabNum, boolean allPages, boolean firstPages,
                               final Utils.RepositoryCallback<Void> errorCallback,
                               final Utils.RepositoryCallback<Page.PageTyp> callbackOnUpdatedPage,
                               final Utils.RepositoryCallback<String> callbackOnTheEnd) {
        executor.execute(() -> {
            for (Page.PageTyp t : typ) {
                int i = firstPages ? 1 : mydb.getLastIndexPageRead(t);
                if (!allPages && i == -1) continue;
                getReadInfo(t).getList(context, resultHandler, mydb, t, tabName, tabNum,
                        allPages ? 1 : i, allPages ? -1 : i + 2, errorCallback, callbackOnUpdatedPage);
            }
            if (callbackOnTheEnd != null)
                resultHandler.post(() -> callbackOnTheEnd.onComplete(""));
        });
    }

    public static String getShortCacheFileName(String thisUrl) {
        String s = thisUrl;
        for (String extension : SUPPORTED_IMAGE_EXTENSIONS) {
            if (s.toLowerCase().endsWith(extension)) {
                s = s.substring(0, s.length() - 1 - extension.length());
                break;
            }
        }
        s = s.replaceAll("[^A-Za-z0-9]", "");
        for (String extension : SUPPORTED_IMAGE_EXTENSIONS) {
            if (thisUrl.toLowerCase().endsWith(extension)) {
                s += "." + extension;
                break;
            }
        }
        return s;
    }

    public static String getCacheDirectory(Context context) {
        // after updating please update mkdir in MainActivity.onCreate
        return Utils.getDiskCacheFolder(context) + File.separator + CACHE_SUB_DIRECTORY;
    }

    public static String getLongCacheFileName(Context context, String thisUrl) {
        return getCacheDirectory(context) + File.separator + getShortCacheFileName(thisUrl);
    }

    public File getCacheFile(Context context) {
        return new File(getLongCacheFileName(context, url));
    }

    public enum PageTyp {
        FANTASTYKA_ARCHIWUM,
        FANTASTYKA_BIBLIOTEKA,
        FANTASTYKA_POCZEKALNIA,
        OPOWI_FANTASTYKA
    }
}