package com.mwiacek.poczytaj.mi.tato.read;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.Date;

public class DBHelper extends SQLiteOpenHelper {

    private final static String PAGES_TABLE_NAME = "pages";
    private final static String COLUMN_NAME = "name";
    private final static String COLUMN_AUTHOR = "author";
    private final static String COLUMN_COMMENTS = "comments";
    private final static String COLUMN_URL = "url";
    private final static String COLUMN_TOP = "top";
    private final static String COLUMN_DATETIME = "dt";
    private final static String COLUMN_TYP = "typ";
    private final static String COLUMN_HIDDEN = "hidden";

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
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    public void updateTop(String url, int top) {
        this.getWritableDatabase().execSQL("update " + PAGES_TABLE_NAME +
                " set " + COLUMN_TOP + "=" + top +
                " where " + COLUMN_URL + "='" + url + "'");
    }

    public void setPageVisible(String url, boolean hidden) {
        this.getWritableDatabase().execSQL("update " + PAGES_TABLE_NAME +
                " set " + COLUMN_HIDDEN + "=" + (hidden ? "1" : "0") +
                " where " + COLUMN_URL + "='" + url + "'");
    }

    public void insertPage(Page.PageTyp typ, String name, String author, String comments, String url, Date d) {
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
        } catch (SQLException ignore) {
        }
    }

    @SuppressLint("Range")
    public ArrayList<Page> getAllPages(boolean hidden, Page.PageTyp[] typ) {
        ArrayList<Page> array_list = new ArrayList<>();

        StringBuilder types = new StringBuilder();
        for (Page.PageTyp t : typ) {
            if (types.length() > 0) types.append(",");
            types.append("").append(t.ordinal());
        }

        Cursor res = this.getReadableDatabase().rawQuery("select * from " + PAGES_TABLE_NAME +
                " where " + COLUMN_HIDDEN + "=" + (hidden ? "1" : "0") +
                " and " + COLUMN_TYP + " IN (" + types + ") " +
                " order by " + COLUMN_DATETIME + " desc", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(new Page(
                    res.getString(res.getColumnIndex(COLUMN_NAME)),
                    res.getString(res.getColumnIndex(COLUMN_AUTHOR)),
                    res.getString(res.getColumnIndex(COLUMN_COMMENTS)),
                    res.getString(res.getColumnIndex(COLUMN_URL)),
                    res.getInt(res.getColumnIndex(COLUMN_TOP)),
                    new Date(res.getLong(res.getColumnIndex(COLUMN_DATETIME))),
                    Page.PageTyp.values()[res.getInt(res.getColumnIndex(COLUMN_TYP))]));
            res.moveToNext();
        }
        return array_list;
    }
}