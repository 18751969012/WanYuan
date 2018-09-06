package com.njust.major.dao.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.njust.major.bean.Malfunction;
import com.njust.major.dao.MalfunctionDao;

import java.util.ArrayList;
import java.util.List;


public class MalfunctionDaoImpl implements MalfunctionDao {


    private Context context;

    public MalfunctionDaoImpl(Context context){
        super();
        this.context = context;
        SQLiteDatabase db = SQLiteDatabase.openDatabase("data/data/com.njust/databases/Major.db", null,
                SQLiteDatabase.OPEN_READWRITE);
    }
    @Override
    public void addMalfunction(Malfunction Malfunction) {
        Uri uri = Uri.parse("content://com.njust/Einsert");
        ContentValues values = new ContentValues();
        values.put("transactionID", Malfunction.getTransactionID());
        values.put("errorTime", Malfunction.getErrorTime());
        values.put("counter", Malfunction.getCounter());
        values.put("errorModule",Malfunction.getErrorModule());
        values.put("errorDescription",Malfunction.getErrorDescription());
        values.put("motorRealActionTime",Malfunction.getMotorRealActionTime());
        values.put("motorMaxElectricity", Malfunction.getMotorMaxElectricity());
        values.put("motorAverageElectricity", Malfunction.getMotorAverageElectricity());

        Uri insert = context.getContentResolver().insert(uri, values);
    }

    @Override
    public void deleteMalfunction(String transactionID) {
        Uri uri = Uri.parse("content://com.njust/Edelete");
        int delete = context.getContentResolver().delete(uri, "transactionID=?", new String[]{transactionID});
    }

    @Override
    public void deleteAllMalfunction() {
        Uri uri = Uri.parse("content://com.njust/Edelete");
        int delete = context.getContentResolver().delete(uri, null, null);
    }

    @Override
    public void updateMalfunction(Malfunction Malfunction) {
        Uri uri = Uri.parse("content://com.njust/Eupdate");
        ContentValues values = new ContentValues();
        values.put("transactionID", Malfunction.getTransactionID());
        values.put("errorTime", Malfunction.getErrorTime());
        values.put("counter", Malfunction.getCounter());
        values.put("errorModule",Malfunction.getErrorModule());
        values.put("errorDescription",Malfunction.getErrorDescription());
        values.put("motorRealActionTime",Malfunction.getMotorRealActionTime());
        values.put("motorMaxElectricity", Malfunction.getMotorMaxElectricity());
        values.put("motorAverageElectricity", Malfunction.getMotorAverageElectricity());
        int update = context.getContentResolver().update(uri, values, "transactionID=?", new String[]{Malfunction.getTransactionID()});
    }

    @Override
    public List<Malfunction> queryMalfunction() {
        List<Malfunction> list = new ArrayList<Malfunction>();
        Uri uri = Uri.parse("content://com.njust/Equery");
        Cursor cursor = context.getContentResolver().query(uri, null,
                null, null, "transactionID ASC");
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int _id = cursor.getInt(0);
                String transactionID = cursor.getString(1);
                String errorTime = cursor.getString(2);
                int counter = cursor.getInt(3);
                String errorModule = cursor.getString(4);
                String errorDescription = cursor.getString(5);
                int motorRealActionTime = cursor.getInt(6);
                int motorMaxElectricity = cursor.getInt(7);
                int motorAverageElectricity = cursor.getInt(8);
                Malfunction bean = new Malfunction(_id, transactionID, errorTime,counter, errorModule, errorDescription, motorRealActionTime,motorMaxElectricity, motorAverageElectricity);
                list.add(bean);
            }
            cursor.close();
        }
        return list;
    }

    @Override
    public Malfunction queryMalfunction(String TransactionID) {
        Malfunction bean = null;
        Uri uri = Uri.parse("content://com.njust/Equery");
        Cursor cursor = context.getContentResolver().query(uri, null,
                "transactionID=?", new String[]{TransactionID}, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToNext();
            int _id = cursor.getInt(0);
            String transactionID = cursor.getString(1);
            String errorTime = cursor.getString(2);
            int counter = cursor.getInt(3);
            String errorModule = cursor.getString(4);
            String errorDescription = cursor.getString(5);
            int motorRealActionTime = cursor.getInt(6);
            int motorMaxElectricity = cursor.getInt(7);
            int motorAverageElectricity = cursor.getInt(8);
                bean = new Malfunction(_id, transactionID,errorTime, counter, errorModule, errorDescription, motorRealActionTime,motorMaxElectricity, motorAverageElectricity);
            cursor.close();
        }
        return bean;
    }
}
