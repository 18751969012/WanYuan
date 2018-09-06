package com.njust.major.dao.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.njust.major.bean.Foodstuff;
import com.njust.major.dao.FoodDao;

import java.util.ArrayList;
import java.util.List;


public class FoodDaoImpl implements FoodDao {
    private Context context;

    public FoodDaoImpl(Context context) {
        super();
        this.context = context;
        SQLiteDatabase db = SQLiteDatabase.openDatabase("data/data/com.njust/databases/Major.db", null,
                SQLiteDatabase.OPEN_READWRITE);
    }

    @Override
    public void add(Foodstuff foodstuff) {
        Uri uri = Uri.parse("content://com.njust/Finsert");
        ContentValues values = new ContentValues();
        values.put("foodID", foodstuff.getFoodID());
        values.put("positionID", foodstuff.getPositionID());
        values.put("stock", foodstuff.getStock());
        values.put("counter", foodstuff.getCounter());
        values.put("state", foodstuff.getState());
        values.put("price", foodstuff.getPrice());
        Uri insert = context.getContentResolver().insert(uri, values);
    }

    @Override
    public void delete(int counter, int positionID) {
        Uri uri = Uri.parse("content://com.njust/Fdelete");
        int delete = context.getContentResolver().delete(uri, "positionID=? and counter=?", new String[]{"" + positionID, "" + counter});
    }

    @Override
    public void update(Foodstuff foodstuff) {
        Uri uri = Uri.parse("content://com.njust/Fupdate");
        ContentValues values = new ContentValues();
        values.put("foodID", foodstuff.getFoodID());
        values.put("stock", foodstuff.getStock());
        values.put("state", foodstuff.getState());
        values.put("price", foodstuff.getPrice());
        int update = context.getContentResolver().update(uri, values,
                "positionID=? and counter=?",
                new String[]{"" + foodstuff.getPositionID(),
                        "" + foodstuff.getCounter()});
    }

    @Override
    public void updateState(int counter, int positionID, int state) {
        Uri uri = Uri.parse("content://com.njust/Fupdate");
        ContentValues values = new ContentValues();
        values.put("state", state);
        int update = context.getContentResolver().update(uri, values,
                "positionID=? and counter=?",
                new String[]{"" + positionID, "" + counter});
    }

    @Override
    public void updateAllState(int counter, int state) {
        Uri uri = Uri.parse("content://com.njust/Fupdate");
        ContentValues values = new ContentValues();
        values.put("state", state);
        int update = context.getContentResolver().update(uri, values,
                "state!=1 and state!=2 and counter=?", new String[]{"" + counter});
    }

    @Override
    public void updatePositionIDStock(int counter, int positionID) {
        Foodstuff bean = query(counter, positionID);
        int stock = bean.getStock();
        int state = 0;
        if (stock <= 1) {
            state = 1;
        }
        Uri uri = Uri.parse("content://com.njust/Fupdate");
        ContentValues values = new ContentValues();
        values.put("stock", stock - 1);
        values.put("state", state);
        int update = context.getContentResolver().update(uri, values,
                "positionID=? and counter=?",
                new String[]{"" + positionID, "" + counter});
    }

    @Override
    public Foodstuff query(int counter, int positionID) {
        Foodstuff bean = null;
        Uri uri = Uri.parse("content://com.njust/Fquery");
        String[] Args = new String[]{"" + positionID, "" + counter};
        Cursor cursor = context.getContentResolver().query(uri, null,
                "positionID=? and counter=?", Args, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToNext();
            int _id = cursor.getInt(0);
            int foodID = cursor.getInt(1);
            int stock = cursor.getInt(3);
            int state = cursor.getInt(5);
            int price = cursor.getInt(6);
            bean = new Foodstuff(_id, foodID, positionID, stock, counter, state, price);
            cursor.close();
        }
        return bean;
    }

    @Override
    public List<Foodstuff> queryAll() {
        List<Foodstuff> list = new ArrayList<Foodstuff>();
        Uri uri = Uri.parse("content://com.njust/Fquery");
        Cursor cursor = context.getContentResolver().query(uri, null,
                "counter=? or counter=?", new String[]{"" + 1, "" + 2}, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int _id = cursor.getInt(0);
                int foodID = cursor.getInt(1);
                int positionID = cursor.getInt(2);
                int stock = cursor.getInt(3);
                int counter = cursor.getInt(4);
                int state = cursor.getInt(5);
                int price = cursor.getInt(6);
                Foodstuff bean = new Foodstuff(_id, foodID, positionID, stock, counter, state, price);
                list.add(bean);
            }
            cursor.close();
        }
        return list;
    }

    @Override
    public List<Foodstuff> queryByFoodID(int foodID) {
        List<Foodstuff> list = new ArrayList<Foodstuff>();
        Foodstuff bean = null;
        Uri uri = Uri.parse("content://com.njust/Fquery");
        String[] Args = new String[]{"" + foodID};
        Cursor cursor = context.getContentResolver().query(uri, null,
                "foodID=?", Args, null);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int _id = cursor.getInt(0);
                int positionID = cursor.getInt(2);
                int stock = cursor.getInt(3);
                int counter = cursor.getInt(4);
                int state = cursor.getInt(5);
                int price = cursor.getInt(6);
                bean = new Foodstuff(_id, foodID, positionID, stock, counter, state, price);
                list.add(bean);
            }
            cursor.close();
        }
        return list;
    }
}
