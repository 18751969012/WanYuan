package com.njust.major.dao.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import com.njust.major.bean.Position;
import com.njust.major.dao.PositionDao;
import java.util.ArrayList;
import java.util.List;


public class PositionDaoImpl implements PositionDao {


    private Context context;

    public PositionDaoImpl(Context context){
        super();
        this.context = context;
        SQLiteDatabase db = SQLiteDatabase.openDatabase("data/data/com.njust/databases/Major.db", null,
                SQLiteDatabase.OPEN_READWRITE);
    }
    @Override
    public void addPosition(Position position) {
        Uri uri = Uri.parse("content://com.njust/Pinsert");
        ContentValues values = new ContentValues();
        values.put("positionID", position.getPositionID());
        values.put("counter",position.getCounter());
        values.put("state",position.getState());
        values.put("motorType",position.getMotorType());
        values.put("position1", (int)position.getPosition1());
        values.put("position2", (int)position.getPosition2());

        Uri insert = context.getContentResolver().insert(uri, values);
    }

    @Override
    public void deletePosition(int positionID ,int counter) {
        Uri uri = Uri.parse("content://com.njust/Pdelete");
        int delete = context.getContentResolver().delete(uri, "positionID=? and counter=?", new String[]{"" + positionID, "" + counter });
    }

    @Override
    public void deleteAllPosition() {
        Uri uri = Uri.parse("content://com.njust/Pdelete");
        int delete = context.getContentResolver().delete(uri, null, null);
    }

    @Override
    public void updatePosition(Position position) {
        Uri uri = Uri.parse("content://com.njust/Pupdate");
        ContentValues values = new ContentValues();
        values.put("positionID", position.getPositionID());
        values.put("counter",position.getCounter());
        values.put("state",position.getState());
        values.put("motorType",position.getMotorType());
        values.put("position1", (int)position.getPosition1());
        values.put("position2", (int)position.getPosition2());
        int update = context.getContentResolver().update(uri, values, "positionID=? and counter=?", new String[]{"" + position.getPositionID(), "" + position.getCounter() });
    }

    @Override
    public void updatePosition(int counter, int positionID, int state) {
        Uri uri = Uri.parse("content://com.njust/Pupdate");
        ContentValues values = new ContentValues();
        values.put("state", state);
        int update = context.getContentResolver().update(uri, values,
                "positionID=? and counter=?",
                new String[]{"" + positionID, "" + counter});
    }
    @Override
    public List<Position> queryPosition() {
        List<Position> list = new ArrayList<Position>();
        Uri uri = Uri.parse("content://com.njust/Pquery");
        Cursor cursor = context.getContentResolver().query(uri, null,
                null, null, "positionID ASC");
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                int _id = cursor.getInt(0);
                int positionID = cursor.getInt(1);
                int counter = cursor.getInt(2);
                int state = cursor.getInt(3);
                int motorType = cursor.getInt(4);
                int position1 = cursor.getInt(5);
                int position2 = cursor.getInt(6);
                Position bean = new Position(positionID, counter, state, motorType, (byte) position1, (byte)position2);
                list.add(bean);
            }
            cursor.close();
        }
        return list;
    }

    @Override
    public Position queryPosition(int positionID ,int counter) {
        Position bean = null;
        Uri uri = Uri.parse("content://com.njust/Pquery");
        Cursor cursor = context.getContentResolver().query(uri, null,
                "positionID=? and counter=?", new String[]{ "" + positionID, "" + counter }, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToNext();
                int _id = cursor.getInt(0);
                int PositionID = cursor.getInt(1);
                int Counter = cursor.getInt(2);
                int state = cursor.getInt(3);
                int motorType = cursor.getInt(4);
                int position1 = cursor.getInt(5);
                int position2 = cursor.getInt(6);
                bean = new Position(PositionID, Counter, state, motorType, (byte) position1, (byte)position2);
            cursor.close();
        }
        return bean;
    }
}
