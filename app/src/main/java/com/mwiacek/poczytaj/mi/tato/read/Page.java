package com.mwiacek.poczytaj.mi.tato.read;

import android.content.Context;

import com.mwiacek.poczytaj.mi.tato.Utils;
import com.mwiacek.poczytaj.mi.tato.read.readinfo.Fantastyka;
import com.mwiacek.poczytaj.mi.tato.read.readinfo.Opowi;
import com.mwiacek.poczytaj.mi.tato.read.readinfo.ReadInfo;

import java.io.File;
import java.util.Date;

public class Page {
    String name;
    String author;
    String tags;
    String url;
    int top;
    Date dt;
    PageTyp typ;

    Page(String name, String author, String comments, String url, int top, Date dt, PageTyp typ) {
        this.name = name;
        this.author = author;
        this.tags = comments;
        this.url = url;
        this.top = top;
        this.dt = dt;
        this.typ = typ;
    }

    public static ReadInfo getReadInfo(PageTyp typ) {
        return typ.name().toLowerCase().startsWith("fantastyka_") ? new Fantastyka() : new Opowi();
    }

    public File getCacheFileName(Context context) {
        return new File(Utils.getDiskCacheFolder(context) + File.separator +
                url.replaceAll("[^A-Za-z0-9]", ""));
    }

    public enum PageTyp {
        FANTASTYKA_ARCHIWUM,
        FANTASTYKA_BIBLIOTEKA,
        FANTASTYKA_POCZEKALNIA,
        OPOWI_FANTASTYKA,
        OPOWI_AUTORZY
    }
}