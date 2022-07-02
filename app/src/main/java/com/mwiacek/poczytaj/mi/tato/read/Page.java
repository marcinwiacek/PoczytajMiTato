package com.mwiacek.poczytaj.mi.tato.read;

import android.content.Context;

import com.mwiacek.poczytaj.mi.tato.Utils;

import java.io.File;
import java.util.Date;

class Page {
    String name;
    String author;
    String comments;
    String url;
    int top;
    Date dt;

    Page(String name, String author, String comments, String url, int top, Date dt) {
        this.name = name;
        this.author = author;
        this.comments = comments;
        this.url = url;
        this.top = top;
        this.dt = dt;
    }

    public File getCacheFileName(Context context) {
        return new File(Utils.getDiskCacheFolder(context) + File.separator +
                url.replaceAll("[^A-Za-z0-9]", ""));
    }
}