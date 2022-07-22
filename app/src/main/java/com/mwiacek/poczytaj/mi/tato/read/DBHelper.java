package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mwiacek.poczytaj.mi.tato.FragmentConfig;

import java.util.ArrayList;
import java.util.Date;

public class DBHelper extends SQLiteOpenHelper {

    private final static String COMPLETED_TABLE_NAME = "completed";
    private final static String PAGES_TABLE_NAME = "pages";
    private final static String COLUMN_NAME = "name";
    private final static String COLUMN_AUTHOR = "author";
    private final static String COLUMN_COMMENTS = "comments";
    private final static String COLUMN_URL = "url";
    private final static String COLUMN_TOP = "top";
    private final static String COLUMN_DATETIME = "dt";
    private final static String COLUMN_TYP = "typ";
    private final static String COLUMN_HIDDEN = "hidden";
    private final static String COLUMN_PAGE_NUMBER = "page_number";

    public DBHelper(Context context) {
        super(context, "pages.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + PAGES_TABLE_NAME + " " +
                "(" + COLUMN_TYP + " integer, " + COLUMN_NAME + " text," +
                COLUMN_AUTHOR + " text, " + COLUMN_DATETIME + " real, " +
                COLUMN_HIDDEN + " integer, " + COLUMN_COMMENTS + " text, " +
                COLUMN_URL + " text, " + COLUMN_TOP + " integer, " +
                "UNIQUE(" + COLUMN_URL + "))"
        );
        db.execSQL("create table " + COMPLETED_TABLE_NAME + " " +
                "(" + COLUMN_TYP + " integer, " + COLUMN_PAGE_NUMBER + " integer, " +
                "UNIQUE(" + COLUMN_TYP + "))"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    @SuppressLint("Range")
    public int getLastIndexPageRead(Page.PageTyp typ) {
        Cursor res = this.getReadableDatabase().rawQuery(
                "select " + COLUMN_PAGE_NUMBER + " from " + COMPLETED_TABLE_NAME +
                        " where " + COLUMN_TYP + "=" + typ.ordinal(), null);
        if (!res.moveToFirst()) return 0;
        return res.getInt(res.getColumnIndex(COLUMN_PAGE_NUMBER));
    }

    public void setLastIndexPageRead(Page.PageTyp typ, int number) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TYP, typ.ordinal());
        contentValues.put(COLUMN_PAGE_NUMBER, number);
        try {
            this.getWritableDatabase().insertOrThrow(COMPLETED_TABLE_NAME,
                    null, contentValues);
        } catch (SQLException ignore) {
            this.getWritableDatabase().update(COMPLETED_TABLE_NAME,
                    contentValues, COLUMN_TYP + "=" + typ.ordinal(), null);
        }
    }

    public void setPageTop(String url, int top) {
        this.getWritableDatabase().execSQL("update " + PAGES_TABLE_NAME +
                " set " + COLUMN_TOP + "=" + top +
                " where " + COLUMN_URL + "='" + url + "'");
    }

    @SuppressLint("Range")
    public int getPageTop(String url) {
        Cursor res = this.getReadableDatabase().rawQuery(
                "select " + COLUMN_TOP + " from " + PAGES_TABLE_NAME +
                        " where " + COLUMN_URL + "='" + url + "'", null);
        res.moveToFirst();
        return res.getInt(res.getColumnIndex(COLUMN_TOP));
    }

    public void setPageHidden(String url, FragmentConfig.HiddenTexts hidden) {
        this.getWritableDatabase().execSQL("update " + PAGES_TABLE_NAME +
                " set " + COLUMN_HIDDEN + "=" + hidden.ordinal() +
                " where " + COLUMN_URL + "='" + url + "'");
    }

    public boolean insertOrUpdatePage(Page.PageTyp typ, String name, String author, String comments, String url, Date d) {
        ContentValues contentValues = new ContentValues();
        contentValues.put(COLUMN_TYP, typ.ordinal());
        contentValues.put(COLUMN_NAME, name);
        contentValues.put(COLUMN_AUTHOR, author);
        contentValues.put(COLUMN_COMMENTS, comments);
        contentValues.put(COLUMN_URL, url);
        contentValues.put(COLUMN_TOP, 0);
        contentValues.put(COLUMN_DATETIME, d.getTime());
        contentValues.put(COLUMN_HIDDEN, 0);
        try {
            this.getWritableDatabase().insertOrThrow(PAGES_TABLE_NAME, null, contentValues);
            return true;
        } catch (SQLException ignore) {
            this.getWritableDatabase().update(PAGES_TABLE_NAME, contentValues,
                    COLUMN_URL + "='" + url + "'", null);
            return false;
        }
    }

    @SuppressLint("Range")
    public Page getPage(String url) {
        Cursor res = this.getReadableDatabase().rawQuery(
                "select * from " + PAGES_TABLE_NAME +
                        " where " + COLUMN_URL + "='" + url + "'", null);
        res.moveToFirst();

        if (!res.isAfterLast()) {
            return new Page(
                    res.getString(res.getColumnIndex(COLUMN_NAME)),
                    res.getString(res.getColumnIndex(COLUMN_AUTHOR)),
                    res.getString(res.getColumnIndex(COLUMN_COMMENTS)),
                    res.getString(res.getColumnIndex(COLUMN_URL)),
                    new Date(res.getLong(res.getColumnIndex(COLUMN_DATETIME))),
                    Page.PageTyp.values()[res.getInt(res.getColumnIndex(COLUMN_TYP))]);
        }
        return null;
    }

    @SuppressLint("Range")
    public ArrayList<Page> getAllPages(FragmentConfig.HiddenTexts hidden, ArrayList<Page.PageTyp> typ,
                                       String authorFilter, String tagFilter) {
        ArrayList<Page> array_list = new ArrayList<>();

        StringBuilder types = new StringBuilder();
        for (Page.PageTyp t : typ) {
            if (types.length() > 0) types.append(",");
            types.append("").append(t.ordinal());
        }

        String[] authors = null;
        if (!authorFilter.isEmpty()) authors = authorFilter.toLowerCase().split(",");

        String[] tags = null;
        if (!tagFilter.isEmpty()) tags = tagFilter.toLowerCase().split(",");

        Cursor res = this.getReadableDatabase().rawQuery(
                "select * from " + PAGES_TABLE_NAME +
                        " where " + COLUMN_HIDDEN + "=" + hidden.ordinal() +
                        " and " + COLUMN_TYP + " IN (" + types + ") " +
                        " order by " + COLUMN_DATETIME + " desc", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            if (authors != null) {
                boolean ok = true;
                for (String s : authors) {
                    //TODO: "not "
                    if (s.trim().startsWith("not ") ==
                            res.getString(res.getColumnIndex(COLUMN_AUTHOR)).toLowerCase()
                                    .trim().equals(s.trim().replace("not ", ""))) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) {
                    res.moveToNext();
                    continue;
                }
            }
            if (tags != null && !res.getString(res.getColumnIndex(COLUMN_COMMENTS)).isEmpty()) {
                boolean ok = true;
                String[] abc = res.getString(res.getColumnIndex(COLUMN_COMMENTS)).toLowerCase().split(",");
                for (String s : tags) {
                    //TODO: "not "
                    for (String ab : abc) {
                        if (ab.trim().startsWith("not ") == ab.trim().equals(s.trim()
                                .replace("not ", ""))) {
                            ok = false;
                            break;
                        }
                    }
                    if (!ok) break;
                }
                if (!ok) {
                    res.moveToNext();
                    continue;
                }
            }
            array_list.add(new Page(
                    res.getString(res.getColumnIndex(COLUMN_NAME)),
                    res.getString(res.getColumnIndex(COLUMN_AUTHOR)),
                    res.getString(res.getColumnIndex(COLUMN_COMMENTS)),
                    res.getString(res.getColumnIndex(COLUMN_URL)),
                    new Date(res.getLong(res.getColumnIndex(COLUMN_DATETIME))),
                    Page.PageTyp.values()[res.getInt(res.getColumnIndex(COLUMN_TYP))]));
            res.moveToNext();
        }
        return array_list;
    }
}