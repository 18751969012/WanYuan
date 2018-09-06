package com.njust.major.dao.impl;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.njust.major.bean.MachineState;
import com.njust.major.dao.MachineStateDao;

public class MachineStateDaoImpl implements MachineStateDao {

    private SQLiteDatabase db;
    private Context context;

    public MachineStateDaoImpl(Context context){
        super();
        this.context = context;
        db = SQLiteDatabase.openDatabase("data/data/com.njust/databases/Major.db", null,
                SQLiteDatabase.OPEN_READWRITE);
    }

    @Override
    public void updateSetting(int counter, int flootNo, int outPosition, String flootPositions) {
        if (counter == 1){
            String sql1 = "update MachineInfo set leftFlootNo=?,leftOutPosition=?,leftFlootPosition=? where _id =1";
            Object[] bindArgs1 = new Object[] { "" + flootNo, "" + outPosition, "" + flootPositions};
            db.execSQL(sql1, bindArgs1);
        }else {
            String sql1 = "update MachineInfo set rightFlootNo=?,rightOutPosition=?,rightFlootPosition=? where _id =1";
            Object[] bindArgs1 = new Object[] { "" + flootNo, "" + outPosition, "" + flootPositions};
            db.execSQL(sql1, bindArgs1);
        }
    }
    @Override
    public void updateTemperature(byte leftTempState, byte leftSetTemp, byte rightTempState, byte rightSetTemp) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("leftTempState", leftTempState);
        values.put("leftSetTemp", leftSetTemp);
        values.put("rightTempState", rightTempState);
        values.put("rightSetTemp", rightSetTemp);
        int update = context.getContentResolver().update(uri, values, "_id=?",
                new String[] { "1" });
    }

    @Override
    public void updateLight(int leftLight, int rightLight, int midLight) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("leftLight", leftLight);
        values.put("rightLight", rightLight);
        values.put("rightLight", midLight);
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }


    @Override
    public void updateVersion(String version) {
        String sql1 = "update MachineInfo set version=? where _id =1";
        Object[] bindArgs1 = new Object[] { "" + version };
        db.execSQL(sql1, bindArgs1);
    }

    @Override
    public void updateState(int state) {
        String sql1 = "update MachineInfo set vmState=? where _id =1";
        Object[] bindArgs1 = new Object[] { "" + state };
        db.execSQL(sql1, bindArgs1);
        String sql2 = "update MachineInfo set leftState=?, rightState=? where _id =1";
        Object[] bindArgs2 = new Object[] { "" + state, "" + state };
        db.execSQL(sql2, bindArgs2);
    }

    @Override
    public void updateCounterState(int leftState, int rightState) {
        String sql1 = "update MachineInfo set leftState=?, rightState=? where _id =1";
        Object[] bindArgs1 = new Object[] { "" + leftState, "" + rightState };
        db.execSQL(sql1, bindArgs1);
    }

    @Override
    public void updateDoorState(int leftDoor, int rightDoor, int midDoorLock) {
        String sql1 = "update MachineInfo set leftDoor=?， rightDoor=? ,midDoorLock=? where _id =1";
        Object[] bindArgs1 = new Object[] { "" + leftDoor + "" + rightDoor + "" + midDoorLock };
        db.execSQL(sql1, bindArgs1);
    }

    @Override
    public void updateCounterDoorState(int leftDoorheat, int rightDoorheat) {
        String sql1 = "update MachineInfo set leftDoorheat=?, rightDoorheat=? where _id =1";
        Object[] bindArgs1 = new Object[] { "" + leftDoorheat, "" + rightDoorheat };
        db.execSQL(sql1, bindArgs1);
    }

    @Override
    public void updateMachineState(MachineState machineState) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("machineID", machineState.getMachineID());
        values.put("version", machineState.getVersion());
        values.put("vmState", machineState.getVmState());
        values.put("leftState", machineState.getLeftState());
        values.put("rightState", machineState.getRightState());
        values.put("leftTempState", machineState.getLeftTempState());
        values.put("leftSetTemp", machineState.getLeftSetTemp());
        values.put("leftCabinetTemp", machineState.getLeftCabinetTemp());
        values.put("leftCabinetTopTemp", machineState.getLeftCabinetTopTemp());
        values.put("leftCompressorTemp", machineState.getLeftCompressorTemp());
        values.put("leftCompressorDCfanState", machineState.getLeftCompressorDCfanState());
        values.put("leftCabinetDCfanState", machineState.getLeftCabinetDCfanState());
        values.put("leftDoor", machineState.getLeftDoor());
        values.put("leftDoorheat", machineState.getLeftDoorheat());
        values.put("leftHumidity", machineState.getLeftHumidity());
        values.put("leftLight", machineState.getLeftLight());
        values.put("leftPushGoodsRaster", machineState.getLeftPushGoodsRaster());
        values.put("leftOutGoodsRaster", machineState.getLeftOutGoodsRaster());
        values.put("rightTempState", machineState.getRightTempState());
        values.put("rightSetTemp", machineState.getRightSetTemp());
        values.put("rightCabinetTemp", machineState.getRightCabinetTemp());
        values.put("rightCabinetTopTemp", machineState.getRightCabinetTopTemp());
        values.put("rightCompressorTemp", machineState.getRightCompressorTemp());
        values.put("rightCompressorDCfanState", machineState.getRightCompressorDCfanState());
        values.put("rightCabinetDCfanState", machineState.getRightCabinetDCfanState());
        values.put("rightDoor", machineState.getRightDoor());
        values.put("rightDoorheat", machineState.getRightDoorheat());
        values.put("rightHumidity", machineState.getRightHumidity());
        values.put("rightLight", machineState.getRightLight());
        values.put("rightPushGoodsRaster", machineState.getRightPushGoodsRaster());
        values.put("rightOutGoodsRaster", machineState.getRightOutGoodsRaster());
        values.put("midLight", machineState.getMidLight());
        values.put("midDoorLock", machineState.getMidDoorLock());
        values.put("midGetGoodsRaster", machineState.getMidGetGoodsRaster());
        values.put("midDropGoodsRaster", machineState.getMidDropGoodsRaster());
        values.put("midAntiPinchHandRaster", machineState.getMidAntiPinchHandRaster());
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }

    @Override
    public MachineState queryMachineState() {
        MachineState machineState = new MachineState();
        Uri uri = Uri.parse("content://com.njust/Mquery");
        Cursor cursor = context.getContentResolver().query(uri, null,
                "_id=?", new String[] { "1" }, null);
        if (cursor != null && cursor.getCount() > 0) {
            cursor.moveToNext();
            machineState.set_id(cursor.getInt(0));
            machineState.setMachineID(cursor.getString(1));
            machineState.setVersion(cursor.getString(2));
            machineState.setVmState(cursor.getInt(3));
            machineState.setLeftState(cursor.getInt(4));
            machineState.setRightState(cursor.getInt(5));
            machineState.setLeftTempState((byte) cursor.getInt(6));
            machineState.setLeftSetTemp((byte)cursor.getInt(7));
            machineState.setLeftCabinetTemp((byte)cursor.getInt(8));
            machineState.setLeftCabinetTopTemp((byte)cursor.getInt(9));
            machineState.setLeftCompressorTemp((byte)cursor.getInt(10));
            machineState.setLeftCompressorDCfanState((byte)cursor.getInt(11));
            machineState.setLeftCabinetDCfanState((byte)cursor.getInt(12));
            machineState.setLeftDoor(cursor.getInt(13));
            machineState.setLeftDoorheat(cursor.getInt(14));
            machineState.setLeftHumidity(cursor.getInt(15));
            machineState.setLeftLight(cursor.getInt(16));
            machineState.setLeftPushGoodsRaster(cursor.getInt(17));
            machineState.setLeftOutGoodsRaster(cursor.getInt(18));
            machineState.setRightTempState((byte) cursor.getInt(19));
            machineState.setRightSetTemp((byte)cursor.getInt(20));
            machineState.setRightCabinetTemp((byte)cursor.getInt(21));
            machineState.setRightCabinetTopTemp((byte)cursor.getInt(22));
            machineState.setRightCompressorTemp((byte)cursor.getInt(23));
            machineState.setRightCompressorDCfanState((byte)cursor.getInt(24));
            machineState.setRightCabinetDCfanState((byte)cursor.getInt(25));
            machineState.setRightDoor(cursor.getInt(26));
            machineState.setRightDoorheat(cursor.getInt(27));
            machineState.setRightHumidity(cursor.getInt(28));
            machineState.setRightLight(cursor.getInt(29));
            machineState.setRightPushGoodsRaster(cursor.getInt(30));
            machineState.setRightOutGoodsRaster(cursor.getInt(31));
            machineState.setMidLight(cursor.getInt(32));
            machineState.setMidDoorLock(cursor.getInt(33));
            machineState.setMidGetGoodsRaster(cursor.getInt(34));
            machineState.setMidDropGoodsRaster(cursor.getInt(35));
            machineState.setMidAntiPinchHandRaster(cursor.getInt(36));
            machineState.setLeftOutPosition(cursor.getInt(37));
            machineState.setLeftFlootPosition(cursor.getString(38));
            machineState.setLeftFlootNo(cursor.getInt(39));
            machineState.setRightOutPosition(cursor.getInt(40));
            machineState.setRightFlootPosition(cursor.getString(41));
            machineState.setRightFlootNo(cursor.getInt(42));
            cursor.close();
        }
        return machineState;
    }
}
