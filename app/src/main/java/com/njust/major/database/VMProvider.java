package com.njust.major.database;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;



public class VMProvider extends ContentProvider {

    private static final UriMatcher sURIMatcher = new UriMatcher(UriMatcher.NO_MATCH);

    private static final int MUPDATESUCESS = 0;
    private static final int MQUERYSUCESS = 1;
    private static final int TINSERTSUCESS = 2;
    private static final int TDELETESUCESS = 3;
    private static final int TUPDATESUCESS = 4;
    private static final int TQUERYSUCESS = 5;
    private static final int PINSERTSUCESS = 6;
    private static final int PDELETESUCESS = 7;
    private static final int PUPDATESUCESS = 8;
    private static final int PQUERYSUCESS = 9;
    private static final int EINSERTSUCESS = 10;
    private static final int EDELETESUCESS = 11;
    private static final int EUPDATESUCESS = 12;
    private static final int EQUERYSUCESS = 13;


    private MyOpenHelper mMyOpenHelper;
    private SQLiteDatabase sqldb;
    static {
        sURIMatcher.addURI("com.njust", "Mupdate", MUPDATESUCESS);
        sURIMatcher.addURI("com.njust", "Mquery", MQUERYSUCESS);
        sURIMatcher.addURI("com.njust", "t/Tinsert", TINSERTSUCESS);
        sURIMatcher.addURI("com.njust", "t/Tdelete", TDELETESUCESS);
        sURIMatcher.addURI("com.njust", "t/Tupdate", TUPDATESUCESS);
        sURIMatcher.addURI("com.njust", "t/Tquery", TQUERYSUCESS);
        sURIMatcher.addURI("com.njust", "Pinsert", PINSERTSUCESS);
        sURIMatcher.addURI("com.njust", "Pdelete", PDELETESUCESS);
        sURIMatcher.addURI("com.njust", "Pupdate", PUPDATESUCESS);
        sURIMatcher.addURI("com.njust", "Pquery", PQUERYSUCESS);
        sURIMatcher.addURI("com.njust", "Einsert", EINSERTSUCESS);
        sURIMatcher.addURI("com.njust", "Edelete", EDELETESUCESS);
        sURIMatcher.addURI("com.njust", "Eupdate", EUPDATESUCESS);
        sURIMatcher.addURI("com.njust", "Equery", EQUERYSUCESS);
    }




    @Override
    public boolean onCreate() {
        mMyOpenHelper = new MyOpenHelper(getContext());
        sqldb = mMyOpenHelper.getWritableDatabase();
        return false;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        int code = sURIMatcher.match(uri);
        if (code == TQUERYSUCESS){
            SQLiteDatabase db = mMyOpenHelper.getReadableDatabase();
            Cursor cursor = db.query("TransactionInfo", projection, selection, selectionArgs, null, null, sortOrder);
            return cursor;
        }else if (code == MQUERYSUCESS){
            SQLiteDatabase db = mMyOpenHelper.getReadableDatabase();
            Cursor cursor = db.query("MachineInfo", projection, selection, selectionArgs, null, null, sortOrder);
            return cursor;
        }else if (code == PQUERYSUCESS){
            SQLiteDatabase db = mMyOpenHelper.getReadableDatabase();
            Cursor cursor = db.query("PositionInfo", projection, selection, selectionArgs, null, null, sortOrder);
            return cursor;
        }else if (code == EQUERYSUCESS){
            SQLiteDatabase db = mMyOpenHelper.getReadableDatabase();
            Cursor cursor = db.query("MalfunctionInfo", projection, selection, selectionArgs, null, null, sortOrder);
            return cursor;
        }else {
            throw new IllegalArgumentException("路径不匹配");
        }

    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        int code = sURIMatcher.match(uri);
        if (code == TINSERTSUCESS) {
            long insert = sqldb.insert("TransactionInfo", null, values);
            getContext().getContentResolver().notifyChange(uri, null);
            Uri uri2 = Uri.parse("insert/" + insert);
            return uri2;
        }else if (code == PINSERTSUCESS){
            long insert = sqldb.insert("PositionInfo", null, values);
            Uri uri2 = Uri.parse("insert/" + insert);
            return uri2;
        }else if (code == EINSERTSUCESS){
            long insert = sqldb.insert("MalfunctionInfo", null, values);
            Uri uri2 = Uri.parse("insert/" + insert);
            return uri2;
        } else {
            throw new IllegalArgumentException("·路径不匹配");
        }


    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        int code = sURIMatcher.match(uri);
        if (code == TDELETESUCESS) {
            int delete = sqldb.delete("TransactionInfo", selection,
                    selectionArgs);
            return delete;
        }else if (code == PDELETESUCESS){
            int delete = sqldb
                    .delete("PositionInfo", selection, selectionArgs);
            return delete;
        }else if (code == EDELETESUCESS){
            int delete = sqldb
                    .delete("MalfunctionInfo", selection, selectionArgs);
            return delete;
        }
        else {
            throw new IllegalArgumentException("·路径不匹配");
        }
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection, @Nullable String[] selectionArgs) {
        int code = sURIMatcher.match(uri);
        if (code == TUPDATESUCESS) {
            int update = sqldb.update("TransactionInfo", values, selection,
                    selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return update;
        }else if (code == MUPDATESUCESS) {
            int update = sqldb.update("MachineInfo", values, selection,
                    selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return update;
        }else if (code == PUPDATESUCESS){
            int update = sqldb.update("PositionInfo", values, selection,
                    selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return update;
        }else if (code == EUPDATESUCESS){
            int update = sqldb.update("MalfunctionInfo", values, selection,
                    selectionArgs);
            getContext().getContentResolver().notifyChange(uri, null);
            return update;
        } else {
            throw new IllegalArgumentException("路径不匹配");
        }
    }
}
