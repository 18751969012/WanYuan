package com.njust.major.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyOpenHelper extends SQLiteOpenHelper {

    private Context context;


    public MyOpenHelper(Context context){
        super(context, "Major.db", null, 1);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        String MachineInfo_table = "create table MachineInfo(" +
                "_id integer primary key autoincrement," +
                "machineID nvarchar(20)," +
                "version nvarchar(20)," +
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
                "leftDoor integer,"+
                "leftDoorheat integer," +
                "leftHumidity integer," +
                "leftLight integer,"+
                "leftPushGoodsRaster integer,"+
                "leftOutGoodsRaster integer,"+

                "rightTempState integer," +
                "rightSetTemp integer," +
                "rightCabinetTemp integer," +
                "rightCabinetTopTemp integer," +
                "rightCompressorTemp integer,"+
                "rightCompressorDCfanState integer,"+
                "rightCabinetDCfanState integer,"+
                "rightDoor integer,"+
                "rightDoorheat integer," +
                "rightHumidity integer," +
                "rightLight integer,"+
                "rightPushGoodsRaster integer,"+
                "rightOutGoodsRaster integer,"+

                "midLight integer,"+
                "midDoorLock integer,"+
                "midGetGoodsRaster integer," +
                "midDropGoodsRaster integer," +
                "midAntiPinchHandRaster integer," +


                "leftOutPosition integer,"+
                "leftFlootPosition nvarchar(40),"+
                "leftFlootNo integer,"+
                "rightOutPosition integer,"+
                "rightFlootPosition nvarchar(40),"+
                "rightFlootNo integer)";

        sqLiteDatabase.execSQL(MachineInfo_table);

        String FoodInfo_table = "create table FoodInfo( _id integer primary key autoincrement, " +
                "foodID integer," +
                "positionID integer," +
                "stock integer," +
                "counter integer," +
                "state integer,"+
                "price integer)";
        sqLiteDatabase.execSQL(FoodInfo_table);

        String Transaction_table = "create table TransactionInfo(" +
                "_id integer primary key autoincrement," +
                "orderNO nvarchar(100)," +
                "complete integer," +
                "type integer," +
                "beginTime datetime default CURRENT_TIMESTAMP," +
                "endTime datetime," +
                "foodIDs nvarchar(100),"+
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

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int i, int i1) {

    }

    public void initData(SQLiteDatabase db) {
        String MachineInfo = "insert into MachineInfo( machineID, version, vmstate," +
                "leftState, rightState," +
                "leftTempState, leftSetTemp,leftCabinetTemp," +
                "leftCabinetTopTemp, leftCompressorTemp,leftCompressorDCfanState," +
                "leftCabinetDCfanState, leftDoor, leftDoorheat," +
                "leftHumidity, leftLight, leftPushGoodsRaster, leftOutGoodsRaster," +

                "rightTempState, rightSetTemp,rightCabinetTemp," +
                "rightCabinetTopTemp, rightCompressorTemp,rightCompressorDCfanState," +
                "rightCabinetDCfanState, rightDoor, rightDoorheat," +
                "rightHumidity, rightLight, rightPushGoodsRaster, rightOutGoodsRaster," +

                "midLight, midDoorLock, midGetGoodsRaster," +
                "midDropGoodsRaster, midAntiPinchHandRaster," +

                "leftOutPosition, leftFlootPosition," +
                "leftFlootNo," +
                "rightOutPosition, rightFlootPosition," +
                "rightFlootNo) values (" +
                "'00000000','ADH816ASV3.0.0.07',0,0,0,0,10,0,0,0,0,0,0,0,0,0,0,0,0,10,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,'',0,0,'',0)";
        db.execSQL(MachineInfo);



        for (int i = 1; i <= 60; i++ ){
            String sql = "insert into FoodInfo(foodID,positionID,stock,counter,state,price) values ("
                    +i +","+i+","+"100,1,1,1)";
            db.execSQL(sql);
            String sql1 = "insert into FoodInfo(foodID,positionID,stock,counter,state,price) values ("
                    +(i+60) +","+i+","+"100,2,1,1)";
            db.execSQL(sql1);
        }


        for (int i = 1; i <= 2; i++ ){
            for(int j = 1; j<= 6; j++ ){
                for(int k = 1; k<= 10; k++ ){
                    int T = 0;
                    if(i == 1){
                        if(j == 1){
                            T = 3;
                        }else if(j == 2){
                            T = 4;
                        }else if(j == 3 || j == 4 || j == 5 || j == 6){
                            T = 1;
                        }
                        if((j == 3 || j == 4 || j == 5 || j == 6) && (k == 9 || k == 10)){
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
                            +((j-1)*10+k) +","+i+","+1+","+T+","+(j*16+k)+","+(j*16+k)+")";
                    db.execSQL(sql);
                }
            }
        }

    }
}
