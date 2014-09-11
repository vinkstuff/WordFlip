package com.filipvinkovic.wordflip.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

/**
 * Created by Filip on 29.8.2014..
 */
public class Database extends SQLiteAssetHelper {

    private static final String DATABASE_NAME = "words_db.db";
    private static final int DATABASE_VERSION = 1;

    public Database(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public String getRandomWord() {
        SQLiteDatabase db = getReadableDatabase();
        String query = "SELECT word FROM words ORDER BY RANDOM() LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        String word = cursor.getString(0);

        cursor.close();
        db.close();

        return word;
    }
}
