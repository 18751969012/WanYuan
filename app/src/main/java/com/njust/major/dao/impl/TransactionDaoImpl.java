package com.njust.major.dao.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.njust.major.bean.Transaction;
import com.njust.major.dao.TransactionDao;

import java.util.ArrayList;
import java.util.List;


public class TransactionDaoImpl implements TransactionDao {

    private Context context;

    public TransactionDaoImpl(Context context) {
        this.context = context;
    }

    @Override
    public void addTransaction(Transaction transaction) {
        Uri uri = Uri.parse("content://com.njust/t/Tinsert");
        ContentValues values = new ContentValues();
        values.put("orderNO", transaction.getOrderNO());
        values.put("complete", transaction.getComplete());
        values.put("type", transaction.getType());
        values.put("beginTime", transaction.getBeginTime());
        values.put("endTime", transaction.getEndTime());
        values.put("positionIDs", transaction.getPositionIDs());
        values.put("error", transaction.getError());

        Uri insert = context.getContentResolver().insert(uri, values);
    }

    @Override
    public void deleteTransaction() {
        Uri uri = Uri.parse("content://com.njust/t/Tdelete");
        int delete = context.getContentResolver().delete(uri, null, null);
        //Util.WriteFile("清空了 " + delete + " 交易记录");
    }

    @Override
    public void updateTransaction(Transaction transaction) {
        Uri uri = Uri.parse("content://com.njust/t/Tupdate");
        ContentValues values = new ContentValues();
        values.put("orderNO", transaction.getOrderNO());
        values.put("complete", transaction.getComplete());
        values.put("type", transaction.getType());
        values.put("endTime", transaction.getEndTime());
        values.put("positionIDs", transaction.getPositionIDs());
        values.put("error", transaction.getError());

        int update = context.getContentResolver().update(uri, values,
                "beginTime=?", new String[] { transaction.getBeginTime() });
    }

    @Override
    public Transaction queryLastedTransaction() {
        Uri uri = Uri.parse("content://com.njust/t/Tquery");
        Cursor cursor = context.getContentResolver().query(uri, null, null,
                null, "_id desc");
        Transaction transaction = null;
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToNext();
            int _id = cursor.getInt(0);
            String orderNO = cursor.getString(1);
            int complete = cursor.getInt(2);
            int type = cursor.getInt(3);
            String beginTime = cursor.getString(4);
            String endTime = cursor.getString(5);
            String positionIDs = cursor.getString(6);
            int error = cursor.getInt(7);
            transaction = new Transaction(_id, orderNO, complete, type, beginTime,
                    endTime, positionIDs, error);
            cursor.close();
        }
        return transaction;
    }

    @Override
    public List<Transaction> getTransaction(String bTime, String eTime) {
        List<Transaction> list = new ArrayList<Transaction>();
        Uri uri = Uri.parse("content://com.njust/t/Tquery");
        String[] args = new String[]{bTime, eTime};
        Cursor cursor = context.getContentResolver().query(uri, null,
                "beginTime>=? and endTime<=?", args, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int _id = cursor.getInt(0);
                String orderNO = cursor.getString(1);
                int complete = cursor.getInt(2);
                int type = cursor.getInt(3);
                String beginTime = cursor.getString(4);
                String endTime = cursor.getString(5);
                String positionIDs = cursor.getString(6);
                int error = cursor.getInt(7);
                Transaction bean = new Transaction(_id, orderNO, complete,type, beginTime, endTime,positionIDs,error);
                list.add(bean);
            }
            cursor.close();
        }
        return list;
    }
}