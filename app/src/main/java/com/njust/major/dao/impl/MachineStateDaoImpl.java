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
    public void updateDCfan(int leftCompressorDCfanState, int leftCabinetDCfanState, int rightCompressorDCfanState, int rightCabinetDCfanState) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("leftCompressorDCfanState", leftCompressorDCfanState);
        values.put("leftCabinetDCfanState", leftCabinetDCfanState);
        values.put("rightCompressorDCfanState", rightCompressorDCfanState);
        values.put("rightCabinetDCfanState", rightCabinetDCfanState);
        int update = context.getContentResolver().update(uri, values, "_id=?",
                new String[] { "1" });
    }

    @Override
    public void updateTemperature(int leftTempState, int leftSetTemp, int rightTempState, int rightSetTemp) {
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
    public void updateMeasureTemp(int leftCompressorTemp, int leftCabinetTemp, int leftCabinetTopTemp, int leftOutCabinetTemp, int leftHumidity,
                                  int rightCompressorTemp, int rightCabinetTemp,int rightCabinetTopTemp, int rightOutCabinetTemp, int rightHumidity) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("leftCompressorTemp", leftCompressorTemp);
        values.put("leftCabinetTemp", leftCabinetTemp);
        values.put("leftCabinetTopTemp", leftCabinetTopTemp);
        values.put("leftOutCabinetTemp", leftOutCabinetTemp);
        values.put("leftHumidity", leftHumidity);
        values.put("rightCompressorTemp", rightCompressorTemp);
        values.put("rightCabinetTemp", rightCabinetTemp);
        values.put("rightCabinetTopTemp", rightCabinetTopTemp);
        values.put("rightOutCabinetTemp", rightOutCabinetTemp);
        values.put("rightHumidity", rightHumidity);
        int update = context.getContentResolver().update(uri, values, "_id=?",
                new String[] { "1" });
    }


    @Override
    public void updateLight(int leftLight, int rightLight, int midLight) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("leftLight", leftLight);
        values.put("rightLight", rightLight);
        values.put("midLight", midLight);
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
    public void updateDoorState(int midDoor, int midDoorLock) {
        String sql1 = "update MachineInfo set midDoor=? , midDoorLock=? where _id =1";
        Object[] bindArgs1 = new Object[] { "" + midDoor +  "" + midDoorLock };
        db.execSQL(sql1, bindArgs1);
    }

    @Override
    public void updateCounterDoorState(int leftDoorheat, int rightDoorheat) {
        String sql1 = "update MachineInfo set leftDoorheat=?, rightDoorheat=? where _id =1";
        Object[] bindArgs1 = new Object[] { "" + leftDoorheat, "" + rightDoorheat };
        db.execSQL(sql1, bindArgs1);
    }

    @Override
    public void updateRasterState(int leftPushGoodsRaster, int leftOutGoodsRaster, int rightPushGoodsRaster, int rightOutGoodsRaster, int midGetGoodsRaster, int midDropGoodsRaster, int midAntiPinchHandRaster){
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("leftPushGoodsRaster", leftPushGoodsRaster);
        values.put("leftOutGoodsRaster", leftOutGoodsRaster);
        values.put("rightPushGoodsRaster", rightPushGoodsRaster);
        values.put("rightOutGoodsRaster", rightOutGoodsRaster);
        values.put("midGetGoodsRaster", midGetGoodsRaster);
        values.put("midDropGoodsRaster", midDropGoodsRaster);
        values.put("midAntiPinchHandRaster", midAntiPinchHandRaster);
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }

    @Override
    public void updateOtherDoorState(int leftOutGoodsDoor, int rightOutGoodsDoor, int midGetDoor, int midDropDoor){
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("leftOutGoodsDoor", leftOutGoodsDoor);
        values.put("rightOutGoodsDoor", rightOutGoodsDoor);
        values.put("midGetDoor", midGetDoor);
        values.put("midDropDoor", midDropDoor);
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }

    @Override
    public void updateFan(int leftTempControlAlternatPower, int leftRefrigerationCompressorState, int leftCompressorFanState, int leftHeatingWireState, int leftRecirculatAirFanState,
                          int rightTempControlAlternatPower, int rightRefrigerationCompressorState, int rightCompressorFanState, int rightHeatingWireState, int rightRecirculatAirFanState) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("leftTempControlAlternatPower", leftTempControlAlternatPower);
        values.put("leftRefrigerationCompressorState", leftRefrigerationCompressorState);
        values.put("leftCompressorFanState", leftCompressorFanState);
        values.put("leftHeatingWireState", leftHeatingWireState);
        values.put("leftRecirculatAirFanState", leftRecirculatAirFanState);
        values.put("rightTempControlAlternatPower", rightTempControlAlternatPower);
        values.put("rightRefrigerationCompressorState", rightRefrigerationCompressorState);
        values.put("rightCompressorFanState", rightCompressorFanState);
        values.put("rightHeatingWireState", rightHeatingWireState);
        values.put("rightRecirculatAirFanState", rightRecirculatAirFanState);
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }
    @Override
    public void updateYState( int counter, int liftPlatformDownSwitch, int liftPlatformUpSwitch) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        if(counter == 1){
            values.put("leftLiftPlatformDownSwitch", liftPlatformDownSwitch);
            values.put("leftLiftPlatformUpSwitch", liftPlatformUpSwitch);
        }else{
            values.put("rightLiftPlatformDownSwitch", liftPlatformDownSwitch);
            values.put("rightLiftPlatformUpSwitch", liftPlatformUpSwitch);
        }
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }
    @Override
    public void updatePushGoodsRaster(int counter, int pushGoodsRaster) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        if(counter == 1){
            values.put("leftPushGoodsRasterImmediately", pushGoodsRaster);
        }else{
            values.put("rightPushGoodsRasterImmediately", pushGoodsRaster);
        }
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }
    @Override
    public void updateOutGoodsRaster(int counter, int outGoodsRaster) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        if(counter == 1){
            values.put("leftOutGoodsRasterImmediately", outGoodsRaster);
        }else{
            values.put("rightOutGoodsRasterImmediately", outGoodsRaster);
        }
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }
    @Override
    public void updateOutGoodsDoorSwitch(int counter, int outGoodsDoorDownSwitch, int outGoodsDoorUpSwitch) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        if(counter == 1){
            values.put("leftOutGoodsDoorDownSwitch", outGoodsDoorDownSwitch);
            values.put("leftOutGoodsDoorUpSwitch", outGoodsDoorUpSwitch);
        }else{
            values.put("rightOutGoodsDoorDownSwitch", outGoodsDoorDownSwitch);
            values.put("rightOutGoodsDoorUpSwitch", outGoodsDoorUpSwitch);
        }
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }
    @Override
    public void updateGetGoodsDoorSwitch(int getGoodsDoorDownSwitch, int getGoodsDoorUpSwitch) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("midGetDoorDownSwitch", getGoodsDoorDownSwitch);
        values.put("midGetDoorUpSwitch", getGoodsDoorUpSwitch);
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
    }
    @Override
    public void updateElectricQuantity(int electricQuantity) {
        Uri uri = Uri.parse("content://com.njust/Mupdate");
        ContentValues values = new ContentValues();
        values.put("electricQuantity", electricQuantity);
        int update = context.getContentResolver().update(uri, values, "_id = ?", new String[]{"1"});
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
        values.put("leftOutCabinetTemp", machineState.getLeftOutCabinetTemp());
        values.put("leftDoorheat", machineState.getLeftDoorheat());
        values.put("leftHumidity", machineState.getLeftHumidity());
        values.put("leftLight", machineState.getLeftLight());
        values.put("leftPushGoodsRaster", machineState.getLeftPushGoodsRaster());
        values.put("leftOutGoodsRaster", machineState.getLeftOutGoodsRaster());
        values.put("leftOutGoodsDoor", machineState.getLeftOutGoodsDoor());

        values.put("rightTempState", machineState.getRightTempState());
        values.put("rightSetTemp", machineState.getRightSetTemp());
        values.put("rightCabinetTemp", machineState.getRightCabinetTemp());
        values.put("rightCabinetTopTemp", machineState.getRightCabinetTopTemp());
        values.put("rightCompressorTemp", machineState.getRightCompressorTemp());
        values.put("rightCompressorDCfanState", machineState.getRightCompressorDCfanState());
        values.put("rightCabinetDCfanState", machineState.getRightCabinetDCfanState());
        values.put("rightOutCabinetTemp", machineState.getRightOutCabinetTemp());
        values.put("rightDoorheat", machineState.getRightDoorheat());
        values.put("rightHumidity", machineState.getRightHumidity());
        values.put("rightLight", machineState.getRightLight());
        values.put("rightPushGoodsRaster", machineState.getRightPushGoodsRaster());
        values.put("rightOutGoodsRaster", machineState.getRightOutGoodsRaster());
        values.put("rightOutGoodsDoor", machineState.getRightOutGoodsDoor());

        values.put("midLight", machineState.getMidLight());
        values.put("midDoorLock", machineState.getMidDoorLock());
        values.put("midDoor", machineState.getMidDoor());
        values.put("midGetGoodsRaster", machineState.getMidGetGoodsRaster());
        values.put("midDropGoodsRaster", machineState.getMidDropGoodsRaster());
        values.put("midAntiPinchHandRaster", machineState.getMidAntiPinchHandRaster());
        values.put("midGetDoor", machineState.getMidGetDoor());
        values.put("midDropDoor", machineState.getMidDropDoor());
        
        //更新单片机通讯协议后新加的字段
        values.put("leftTempControlAlternatPower", machineState.getLeftTempControlAlternatPower());
        values.put("leftRefrigerationCompressorState", machineState.getLeftRefrigerationCompressorState());
        values.put("leftCompressorFanState", machineState.getLeftCompressorFanState());
        values.put("leftHeatingWireState", machineState.getLeftHeatingWireState());
        values.put("leftRecirculatAirFanState", machineState.getLeftRecirculatAirFanState());
        
        values.put("leftLiftPlatformDownSwitch", machineState.getLeftLiftPlatformDownSwitch());
        values.put("leftLiftPlatformUpSwitch", machineState.getLeftLiftPlatformUpSwitch());
        values.put("leftLiftPlatformOutGoodsSwitch", machineState.getLeftLiftPlatformOutGoodsSwitch());
        values.put("leftOutGoodsRasterImmediately", machineState.getLeftOutGoodsRasterImmediately());
        values.put("leftPushGoodsRasterImmediately", machineState.getLeftPushGoodsRasterImmediately());
        values.put("leftMotorFeedbackState1", machineState.getLeftMotorFeedbackState1());
        values.put("leftMotorFeedbackState2", machineState.getLeftMotorFeedbackState2());
        values.put("leftOutGoodsDoorDownSwitch", machineState.getLeftOutGoodsDoorDownSwitch());
        values.put("leftOutGoodsDoorUpSwitch", machineState.getLeftOutGoodsDoorUpSwitch());

        values.put("rightTempControlAlternatPower", machineState.getRightTempControlAlternatPower());
        values.put("rightRefrigerationCompressorState", machineState.getRightRefrigerationCompressorState());
        values.put("rightCompressorFanState", machineState.getRightCompressorFanState());
        values.put("rightHeatingWireState", machineState.getRightHeatingWireState());
        values.put("rightRecirculatAirFanState", machineState.getRightRecirculatAirFanState());

        values.put("rightLiftPlatformDownSwitch", machineState.getRightLiftPlatformDownSwitch());
        values.put("rightLiftPlatformUpSwitch", machineState.getRightLiftPlatformUpSwitch());
        values.put("rightLiftPlatformOutGoodsSwitch", machineState.getRightLiftPlatformOutGoodsSwitch());
        values.put("rightOutGoodsRasterImmediately", machineState.getRightOutGoodsRasterImmediately());
        values.put("rightPushGoodsRasterImmediately", machineState.getRightPushGoodsRasterImmediately());
        values.put("rightMotorFeedbackState1", machineState.getRightMotorFeedbackState1());
        values.put("rightMotorFeedbackState2", machineState.getRightMotorFeedbackState2());
        values.put("rightOutGoodsDoorDownSwitch", machineState.getRightOutGoodsDoorDownSwitch());
        values.put("rightOutGoodsDoorUpSwitch", machineState.getRightOutGoodsDoorUpSwitch());

        values.put("midGetDoorWaitClose", machineState.getMidGetDoorWaitClose());
        values.put("midGetDoorDownSwitch", machineState.getMidGetDoorDownSwitch());
        values.put("midGetDoorUpSwitch", machineState.getMidGetDoorUpSwitch());
        values.put("midDropDoorDownSwitch", machineState.getMidDropDoorDownSwitch());
        values.put("midDropDoorUpSwitch", machineState.getMidDropDoorUpSwitch());
        values.put("electricQuantity", machineState.getElectricQuantity());
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

            machineState.setLeftTempState(cursor.getInt(6));
            machineState.setLeftSetTemp(cursor.getInt(7));
            machineState.setLeftCabinetTemp(cursor.getInt(8));
            machineState.setLeftCabinetTopTemp(cursor.getInt(9));
            machineState.setLeftCompressorTemp(cursor.getInt(10));
            machineState.setLeftCompressorDCfanState(cursor.getInt(11));
            machineState.setLeftCabinetDCfanState(cursor.getInt(12));
            machineState.setLeftOutCabinetTemp(cursor.getInt(13));
            machineState.setLeftDoorheat(cursor.getInt(14));
            machineState.setLeftHumidity(cursor.getInt(15));
            machineState.setLeftLight(cursor.getInt(16));
            machineState.setLeftPushGoodsRaster(cursor.getInt(17));
            machineState.setLeftOutGoodsRaster(cursor.getInt(18));
            machineState.setLeftOutGoodsDoor(cursor.getInt(19));

            machineState.setRightTempState(cursor.getInt(20));
            machineState.setRightSetTemp(cursor.getInt(21));
            machineState.setRightCabinetTemp(cursor.getInt(22));
            machineState.setRightCabinetTopTemp(cursor.getInt(23));
            machineState.setRightCompressorTemp(cursor.getInt(24));
            machineState.setRightCompressorDCfanState(cursor.getInt(25));
            machineState.setRightCabinetDCfanState(cursor.getInt(26));
            machineState.setRightOutCabinetTemp(cursor.getInt(27));
            machineState.setRightDoorheat(cursor.getInt(28));
            machineState.setRightHumidity(cursor.getInt(29));
            machineState.setRightLight(cursor.getInt(30));
            machineState.setRightPushGoodsRaster(cursor.getInt(31));
            machineState.setRightOutGoodsRaster(cursor.getInt(32));
            machineState.setRightOutGoodsDoor(cursor.getInt(33));

            machineState.setMidLight(cursor.getInt(34));
            machineState.setMidDoorLock(cursor.getInt(35));
            machineState.setMidDoor(cursor.getInt(36));
            machineState.setMidGetGoodsRaster(cursor.getInt(37));
            machineState.setMidDropGoodsRaster(cursor.getInt(38));
            machineState.setMidAntiPinchHandRaster(cursor.getInt(39));
            machineState.setMidGetDoor(cursor.getInt(40));
            machineState.setMidDropDoor(cursor.getInt(41));

            machineState.setLeftOutPosition(cursor.getInt(42));
            machineState.setLeftFlootPosition(cursor.getString(43));
            machineState.setLeftFlootNo(cursor.getInt(44));
            machineState.setRightOutPosition(cursor.getInt(45));
            machineState.setRightFlootPosition(cursor.getString(46));
            machineState.setRightFlootNo(cursor.getInt(47));

            //更新单片机通讯协议后新加的字段
            machineState.setLeftTempControlAlternatPower(cursor.getInt(48));
            machineState.setLeftRefrigerationCompressorState(cursor.getInt(49));
            machineState.setLeftCompressorFanState(cursor.getInt(50));
            machineState.setLeftHeatingWireState(cursor.getInt(51));
            machineState.setLeftRecirculatAirFanState(cursor.getInt(52));

            machineState.setLeftLiftPlatformDownSwitch(cursor.getInt(53));
            machineState.setLeftLiftPlatformUpSwitch(cursor.getInt(54));
            machineState.setLeftLiftPlatformOutGoodsSwitch(cursor.getInt(55));
            machineState.setLeftOutGoodsRasterImmediately(cursor.getInt(56));
            machineState.setLeftPushGoodsRasterImmediately(cursor.getInt(57));
            machineState.setLeftMotorFeedbackState1(cursor.getInt(58));
            machineState.setLeftMotorFeedbackState2(cursor.getInt(59));
            machineState.setLeftOutGoodsDoorDownSwitch(cursor.getInt(60));
            machineState.setLeftOutGoodsDoorUpSwitch(cursor.getInt(61));

            machineState.setRightTempControlAlternatPower(cursor.getInt(62));
            machineState.setRightRefrigerationCompressorState(cursor.getInt(63));
            machineState.setRightCompressorFanState(cursor.getInt(64));
            machineState.setRightHeatingWireState(cursor.getInt(65));
            machineState.setRightRecirculatAirFanState(cursor.getInt(66));

            machineState.setRightLiftPlatformDownSwitch(cursor.getInt(67));
            machineState.setRightLiftPlatformUpSwitch(cursor.getInt(68));
            machineState.setRightLiftPlatformOutGoodsSwitch(cursor.getInt(69));
            machineState.setRightOutGoodsRasterImmediately(cursor.getInt(70));
            machineState.setRightPushGoodsRasterImmediately(cursor.getInt(71));
            machineState.setRightMotorFeedbackState1(cursor.getInt(72));
            machineState.setRightMotorFeedbackState2(cursor.getInt(73));
            machineState.setRightOutGoodsDoorDownSwitch(cursor.getInt(74));
            machineState.setRightOutGoodsDoorUpSwitch(cursor.getInt(75));

            machineState.setMidGetDoorWaitClose(cursor.getInt(76));
            machineState.setMidGetDoorDownSwitch(cursor.getInt(77));
            machineState.setMidGetDoorUpSwitch(cursor.getInt(78));
            machineState.setMidDropDoorDownSwitch(cursor.getInt(79));
            machineState.setMidDropDoorUpSwitch(cursor.getInt(80));
            machineState.setElectricQuantity(cursor.getInt(81));
            cursor.close();
        }
        return machineState;
    }
}
