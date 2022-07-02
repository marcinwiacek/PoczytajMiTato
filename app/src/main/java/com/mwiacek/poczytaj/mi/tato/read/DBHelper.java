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

    public static final String DATABASE_NAME = "pages.db";
    public static final String CONTACTS_COLUMN_NAME = "name";
    public static final String CONTACTS_COLUMN_AUTHOR = "author";
    public static final String CONTACTS_COLUMN_COMMENTS = "comments";
    public static final String CONTACTS_COLUMN_URL = "url";
    public static final String CONTACTS_COLUMN_TOP = "top";
    public static final String CONTACTS_COLUMN_DATETIME = "dt";

    public DBHelper(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(
                "create table pages " +
                        "(tabnr integer, typ integer,name text,author text, dt real, " +
                        "comments text, url text, top integer, UNIQUE(url))"
        );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS pages");
        onCreate(db);
    }

    public void insertPage(String name, String author, String comments, String url, Date d) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("tabnr", 1);
        contentValues.put("typ", 1);
        contentValues.put("name", name);
        contentValues.put("author", author);
        contentValues.put("comments", comments);
        contentValues.put("url", url);
        contentValues.put("top", 0);
        contentValues.put("dt", d.getTime());
        try {
            db.insertOrThrow("pages", null, contentValues);
        } catch (SQLException ignore) {
        }
    }

    @SuppressLint("Range")
    public ArrayList<Page> getAllPages() {
        ArrayList<Page> array_list = new ArrayList<>();

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor res = db.rawQuery("select * from pages", null);
        res.moveToFirst();

        while (!res.isAfterLast()) {
            array_list.add(new Page(
                    res.getString(res.getColumnIndex(CONTACTS_COLUMN_NAME)),
                    res.getString(res.getColumnIndex(CONTACTS_COLUMN_AUTHOR)),
                    res.getString(res.getColumnIndex(CONTACTS_COLUMN_COMMENTS)),
                    res.getString(res.getColumnIndex(CONTACTS_COLUMN_URL)),
                    res.getInt(res.getColumnIndex(CONTACTS_COLUMN_TOP)),
                    new Date(res.getLong(res.getColumnIndex(CONTACTS_COLUMN_DATETIME)))));
            res.moveToNext();
        }
        return array_list;
    }
}