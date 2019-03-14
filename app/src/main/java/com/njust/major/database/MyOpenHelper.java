package com.njust.major.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyOpenHelper extends SQLiteOpenHelper {

    private Context context;


    public MyOpenHelper(Context context){
        super(context, "Major.db", null, 3);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String MachineInfo_table = "create table MachineInfo(" +
                "_id integer primary key autoincrement," +
                "machineID nvarchar(100)," +
                "version nvarchar(100)," +
                "vmState integer," +
                "leftState integer," +
                "rightState integer," +

                "leftTempState integer," +
                "leftSetTemp integer," +
                "leftCabinetTemp integer," +
                "leftCabinetTopTemp integer," +
                "leftCompressorTemp integer,"+
                "leftCompressorDCfanState integer,"+
                "leftCabinetDCfanState integer,"+
                "leftOutCabinetTemp integer,"+
                "leftDoorheat integer," +
                "leftHumidity integer," +
                "leftLight integer,"+
                "leftPushGoodsRaster integer,"+
                "leftOutGoodsRaster integer,"+
                "leftOutGoodsDoor integer,"+

                "rightTempState integer," +
                "rightSetTemp integer," +
                "rightCabinetTemp integer," +
                "rightCabinetTopTemp integer," +
                "rightCompressorTemp integer,"+
                "rightCompressorDCfanState integer,"+
                "rightCabinetDCfanState integer,"+
                "rightOutCabinetTemp integer,"+
                "rightDoorheat integer," +
                "rightHumidity integer," +
                "rightLight integer,"+
                "rightPushGoodsRaster integer,"+
                "rightOutGoodsRaster integer,"+
                "rightOutGoodsDoor integer,"+

                "midLight integer,"+
                "midDoorLock integer,"+
                "midDoor integer,"+
                "midGetGoodsRaster integer," +
                "midDropGoodsRaster integer," +
                "midAntiPinchHandRaster integer," +
                "midGetDoor integer," +
                "midDropDoor integer," +


                "leftOutPosition integer,"+
                "leftFlootPosition nvarchar(40),"+
                "leftFlootNo integer,"+
                "rightOutPosition integer,"+
                "rightFlootPosition nvarchar(40),"+
                "rightFlootNo integer,"+
                
                "leftTempControlAlternatPower integer,"+
                "leftRefrigerationCompressorState integer,"+
                "leftCompressorFanState integer,"+
                "leftHeatingWireState integer,"+
                "leftRecirculatAirFanState integer,"+

                "leftLiftPlatformDownSwitch integer,"+
                "leftLiftPlatformUpSwitch integer,"+
                "leftLiftPlatformOutGoodsSwitch integer,"+
                "leftOutGoodsRasterImmediately integer,"+
                "leftPushGoodsRasterImmediately integer,"+
                "leftMotorFeedbackState1 integer,"+
                "leftMotorFeedbackState2 integer,"+
                "leftOutGoodsDoorDownSwitch integer,"+
                "leftOutGoodsDoorUpSwitch integer,"+

                "rightTempControlAlternatPower integer,"+
                "rightRefrigerationCompressorState integer,"+
                "rightCompressorFanState integer,"+
                "rightHeatingWireState integer,"+
                "rightRecirculatAirFanState integer,"+

                "rightLiftPlatformDownSwitch integer,"+
                "rightLiftPlatformUpSwitch integer,"+
                "rightLiftPlatformOutGoodsSwitch integer,"+
                "rightOutGoodsRasterImmediately integer,"+
                "rightPushGoodsRasterImmediately integer,"+
                "rightMotorFeedbackState1 integer,"+
                "rightMotorFeedbackState2 integer,"+
                "rightOutGoodsDoorDownSwitch integer,"+
                "rightOutGoodsDoorUpSwitch integer,"+

                "midGetDoorWaitClose integer,"+
                "midGetDoorDownSwitch integer,"+
                "midGetDoorUpSwitch integer,"+
                "midDropDoorDownSwitch integer,"+
                "midDropDoorUpSwitch integer,"+
                "electricQuantity integer)";
        sqLiteDatabase.execSQL(MachineInfo_table);

        String Transaction_table = "create table TransactionInfo(" +
                "_id integer primary key autoincrement," +
                "orderNO nvarchar(100)," +
                "complete integer," +
                "type integer," +
                "beginTime datetime default CURRENT_TIMESTAMP," +
                "endTime datetime," +
                "positionIDs nvarchar(100),"+
                "error integer)";
        sqLiteDatabase.execSQL(Transaction_table);


        String Position_table = "create table PositionInfo(" +
                "_id integer primary key autoincrement," +
                "positionID integer," +
                "counter integer," +
                "state integer," +
                "motorType integer," +
                "position1 integer," +
                "position2 integer)";
        sqLiteDatabase.execSQL(Position_table);

        String Malfunction_table = "create table MalfunctionInfo(" +
                "_id integer primary key autoincrement," +
                "transactionID integer," +
                "errorTime datetime default CURRENT_TIMESTAMP," +
                "counter integer," +
                "errorModule nvarchar(100)," +
                "errorDescription nvarchar(150)," +
                "motorRealActionTime integer," +
                "motorMaxElectricity integer," +
                "motorAverageElectricity integer)";
        sqLiteDatabase.execSQL(Malfunction_table);

        initData(sqLiteDatabase);
    }

    private static final String DROP_MachineInfo = "drop table MachineInfo";
    private static final String CREATE_MachineInfo = "create table MachineInfo(" +
            "_id integer primary key autoincrement," +
            "machineID nvarchar(100)," +
            "version nvarchar(100)," +
            "vmState integer," +
            "leftState integer," +
            "rightState integer," +

            "leftTempState integer," +
            "leftSetTemp integer," +
            "leftCabinetTemp integer," +
            "leftCabinetTopTemp integer," +
            "leftCompressorTemp integer,"+
            "leftCompressorDCfanState integer,"+
            "leftCabinetDCfanState integer,"+
            "leftOutCabinetTemp integer,"+
            "leftDoorheat integer," +
            "leftHumidity integer," +
            "leftLight integer,"+
            "leftPushGoodsRaster integer,"+
            "leftOutGoodsRaster integer,"+
            "leftOutGoodsDoor integer,"+

            "rightTempState integer," +
            "rightSetTemp integer," +
            "rightCabinetTemp integer," +
            "rightCabinetTopTemp integer," +
            "rightCompressorTemp integer,"+
            "rightCompressorDCfanState integer,"+
            "rightCabinetDCfanState integer,"+
            "rightOutCabinetTemp integer,"+
            "rightDoorheat integer," +
            "rightHumidity integer," +
            "rightLight integer,"+
            "rightPushGoodsRaster integer,"+
            "rightOutGoodsRaster integer,"+
            "rightOutGoodsDoor integer,"+

            "midLight integer,"+
            "midDoorLock integer,"+
            "midDoor integer,"+
            "midGetGoodsRaster integer," +
            "midDropGoodsRaster integer," +
            "midAntiPinchHandRaster integer," +
            "midGetDoor integer," +
            "midDropDoor integer," +


            "leftOutPosition integer,"+
            "leftFlootPosition nvarchar(40),"+
            "leftFlootNo integer,"+
            "rightOutPosition integer,"+
            "rightFlootPosition nvarchar(40),"+
            "rightFlootNo integer,"+

            "leftTempControlAlternatPower integer,"+
            "leftRefrigerationCompressorState integer,"+
            "leftCompressorFanState integer,"+
            "leftHeatingWireState integer,"+
            "leftRecirculatAirFanState integer,"+

            "leftLiftPlatformDownSwitch integer,"+
            "leftLiftPlatformUpSwitch integer,"+
            "leftLiftPlatformOutGoodsSwitch integer,"+
            "leftOutGoodsRasterImmediately integer,"+
            "leftPushGoodsRasterImmediately integer,"+
            "leftMotorFeedbackState1 integer,"+
            "leftMotorFeedbackState2 integer,"+
            "leftOutGoodsDoorDownSwitch integer,"+
            "leftOutGoodsDoorUpSwitch integer,"+

            "rightTempControlAlternatPower integer,"+
            "rightRefrigerationCompressorState integer,"+
            "rightCompressorFanState integer,"+
            "rightHeatingWireState integer,"+
            "rightRecirculatAirFanState integer,"+

            "rightLiftPlatformDownSwitch integer,"+
            "rightLiftPlatformUpSwitch integer,"+
            "rightLiftPlatformOutGoodsSwitch integer,"+
            "rightOutGoodsRasterImmediately integer,"+
            "rightPushGoodsRasterImmediately integer,"+
            "rightMotorFeedbackState1 integer,"+
            "rightMotorFeedbackState2 integer,"+
            "rightOutGoodsDoorDownSwitch integer,"+
            "rightOutGoodsDoorUpSwitch integer,"+

            "midGetDoorWaitClose integer,"+
            "midGetDoorDownSwitch integer,"+
            "midGetDoorUpSwitch integer,"+
            "midDropDoorDownSwitch integer,"+
            "midDropDoorUpSwitch integer,"+
            "electricQuantity integer)";

    private static final String INIT_MachineInfo = "insert into MachineInfo( machineID, version, vmstate," +
            "leftState, rightState," +
            "leftTempState, leftSetTemp,leftCabinetTemp," +
            "leftCabinetTopTemp, leftCompressorTemp,leftCompressorDCfanState," +
            "leftCabinetDCfanState, leftOutCabinetTemp, leftDoorheat," +
            "leftHumidity, leftLight, leftPushGoodsRaster, leftOutGoodsRaster,leftOutGoodsDoor," +

            "rightTempState, rightSetTemp,rightCabinetTemp," +
            "rightCabinetTopTemp, rightCompressorTemp,rightCompressorDCfanState," +
            "rightCabinetDCfanState, rightOutCabinetTemp, rightDoorheat," +
            "rightHumidity, rightLight, rightPushGoodsRaster, rightOutGoodsRaster,rightOutGoodsDoor," +

            "midLight, midDoorLock, midDoor,midGetGoodsRaster," +
            "midDropGoodsRaster, midAntiPinchHandRaster,midGetDoor,midDropDoor," +

            "leftOutPosition, leftFlootPosition," +
            "leftFlootNo," +
            "rightOutPosition, rightFlootPosition," +
            "rightFlootNo," +

            "leftTempControlAlternatPower, leftRefrigerationCompressorState, leftCompressorFanState,leftHeatingWireState,leftRecirculatAirFanState," +
            "leftLiftPlatformDownSwitch, leftLiftPlatformUpSwitch, leftLiftPlatformOutGoodsSwitch,leftOutGoodsRasterImmediately,leftPushGoodsRasterImmediately," +
            "leftMotorFeedbackState1, leftMotorFeedbackState2, leftOutGoodsDoorDownSwitch,leftOutGoodsDoorUpSwitch," +
            "rightTempControlAlternatPower, rightRefrigerationCompressorState, rightCompressorFanState,rightHeatingWireState,rightRecirculatAirFanState," +
            "rightLiftPlatformDownSwitch, rightLiftPlatformUpSwitch, rightLiftPlatformOutGoodsSwitch,rightOutGoodsRasterImmediately,rightPushGoodsRasterImmediately," +
            "rightMotorFeedbackState1, rightMotorFeedbackState2, rightOutGoodsDoorDownSwitch,rightOutGoodsDoorUpSwitch," +
            "midGetDoorWaitClose, midGetDoorDownSwitch, midGetDoorUpSwitch,midDropDoorDownSwitch,midDropDoorUpSwitch,electricQuantity" +
            ") values (" +
            "'00000000','ADH816ASV3.0.0.07',0,0,0,0,10,0,0,0,0,0,10,0,10,0,0,0,0,0,10,0,0,0,0,0,10,0,10,0,0,0,0,0,0,1,0,0,0,0,0,0,'0 0 0 0 0 0',0,0,'0 0 0 0 0 0',0," +
            "0,0,0,0,0,1,0,1,0,0,0,0,1,0," +
            "0,0,0,0,0,1,0,1,0,0,0,0,1,0," +
            "0,0,1,0,0,0)";
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch(newVersion){
            case 3:
//                db.execSQL(CHANGE_OLD_TABLE_TRANSANCTIONINFO); //第一步将旧表改为临时表
//
//                db.execSQL(CREATE_NEW_TABLE); //第二步创建新表(新添加的字段或去掉 的字段)
//
//                db.execSQL(INSERT_DATA); //第三步将旧表中的原始数据保存到新表中以防遗失
//
//                db.execSQL(DROP_TABLE); //第四步删除临时备份表

                db.execSQL(DROP_MachineInfo);//第一步删除

                db.execSQL(CREATE_MachineInfo); //第二步创建新表

                db.execSQL(INIT_MachineInfo); //第二步初始化信息
                break;
        }
    }

    public void initData(SQLiteDatabase db) {
        String MachineInfo = "insert into MachineInfo( machineID, version, vmstate," +
                "leftState, rightState," +
                "leftTempState, leftSetTemp,leftCabinetTemp," +
                "leftCabinetTopTemp, leftCompressorTemp,leftCompressorDCfanState," +
                "leftCabinetDCfanState, leftOutCabinetTemp, leftDoorheat," +
                "leftHumidity, leftLight, leftPushGoodsRaster, leftOutGoodsRaster,leftOutGoodsDoor," +

                "rightTempState, rightSetTemp,rightCabinetTemp," +
                "rightCabinetTopTemp, rightCompressorTemp,rightCompressorDCfanState," +
                "rightCabinetDCfanState, rightOutCabinetTemp, rightDoorheat," +
                "rightHumidity, rightLight, rightPushGoodsRaster, rightOutGoodsRaster,rightOutGoodsDoor," +

                "midLight, midDoorLock, midDoor,midGetGoodsRaster," +
                "midDropGoodsRaster, midAntiPinchHandRaster,midGetDoor,midDropDoor," +

                "leftOutPosition, leftFlootPosition," +
                "leftFlootNo," +
                "rightOutPosition, rightFlootPosition," +
                "rightFlootNo," +
                
                "leftTempControlAlternatPower, leftRefrigerationCompressorState, leftCompressorFanState,leftHeatingWireState,leftRecirculatAirFanState," +
                "leftLiftPlatformDownSwitch, leftLiftPlatformUpSwitch, leftLiftPlatformOutGoodsSwitch,leftOutGoodsRasterImmediately,leftPushGoodsRasterImmediately," +
                "leftMotorFeedbackState1, leftMotorFeedbackState2, leftOutGoodsDoorDownSwitch,leftOutGoodsDoorUpSwitch," +
                "rightTempControlAlternatPower, rightRefrigerationCompressorState, rightCompressorFanState,rightHeatingWireState,rightRecirculatAirFanState," +
                "rightLiftPlatformDownSwitch, rightLiftPlatformUpSwitch, rightLiftPlatformOutGoodsSwitch,rightOutGoodsRasterImmediately,rightPushGoodsRasterImmediately," +
                "rightMotorFeedbackState1, rightMotorFeedbackState2, rightOutGoodsDoorDownSwitch,rightOutGoodsDoorUpSwitch," +
                "midGetDoorWaitClose, midGetDoorDownSwitch, midGetDoorUpSwitch,midDropDoorDownSwitch,midDropDoorUpSwitch,electricQuantity" +
                ") values (" +
                "'00000000','ADH816ASV3.0.0.07',0,0,0,0,10,0,0,0,0,0,10,0,10,0,0,0,0,0,10,0,0,0,0,0,10,0,10,0,0,0,0,0,0,1,0,0,0,0,0,0,'0 0 0 0 0 0',0,0,'0 0 0 0 0 0',0," +
                "0,0,0,0,0,1,0,1,0,0,0,0,1,0," +
                "0,0,0,0,0,1,0,1,0,0,0,0,1,0," +
                "0,0,1,0,0,0)";
        db.execSQL(MachineInfo);


        for (int i = 1; i <= 2; i++ ){
            for(int j = 1; j<= 10; j++ ){
                for(int k = 1; k<= 10; k++ ){
                    int T = 0;
                    if(i == 1){
                        if(j == 1){
                            T = 1;
                        }else if(j == 2 || j == 3 || j == 4 || j == 5 || j == 6){
                            T = 3;
                        }
                        if((j == 1) && (k == 9 || k == 10)){
                            T = 2;
                        }
                    }
                    if(i == 2){
                        if(j == 2){
                            T = 3;
                        }else if(j == 5 || j == 6){
                            T = 4;
                        }else if(j == 1 || j == 3 || j == 4){
                            T = 1;
                        }
                        if((j == 1 || j == 3 || j == 4) && (k == 1 || k == 2)){
                            T = 2;
                        }
                    }
                    String sql = "insert into PositionInfo(positionID,counter,state,motorType,position1,position2) values ("
                            +((i-1)*100+(j-1)*10+k) +","+i+","+1+","+T+","+(j*16+k)+","+(j*16+k)+")";
                    db.execSQL(sql);
                }
            }
        }

    }
}
