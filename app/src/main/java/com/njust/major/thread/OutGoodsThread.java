package com.njust.major.thread;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.njust.SerialPort;
import com.njust.major.SCM.MotorControl;
import com.njust.major.bean.MachineState;
import com.njust.major.bean.Malfunction;
import com.njust.major.bean.Position;
import com.njust.major.bean.Transaction;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.MalfunctionDao;
import com.njust.major.dao.PositionDao;
import com.njust.major.dao.TransactionDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.dao.impl.MalfunctionDaoImpl;
import com.njust.major.dao.impl.PositionDaoImpl;
import com.njust.major.dao.impl.TransactionDaoImpl;
import com.njust.major.util.Util;
import com.njust.major.config.Constant;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import static com.njust.VMApplication.OutGoodsThreadFlag;
import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.current_transaction_order_number;
import static com.njust.VMApplication.mQuery0Flag;
import static com.njust.VMApplication.mQuery1Flag;
import static com.njust.VMApplication.mQuery2Flag;
import static com.njust.VMApplication.mUpdataDatabaseFlag;
import static com.njust.VMApplication.rimZNum1;
import static com.njust.VMApplication.rimZNum2;
import static com.njust.VMApplication.aisleZNum1;
import static com.njust.VMApplication.aisleZNum2;
import static com.njust.VMApplication.midZNum;
import static com.njust.VMApplication.delayGetDoor;
import static com.njust.VMApplication.wuOrsihuodaoxian;


public class OutGoodsThread extends Thread {
    private Context context;

    private SerialPort serialPort485;
    private MotorControl mMotorControl;

    private PositionDao pDao;
    private TransactionDao tDao;
    private MachineStateDao mDao;
    private Transaction queryLastedTransaction;
    private Timer mTimer = new Timer();
    private TimerTask timerTask;//首次取货门开门所用延时，和货道板推货启动后定时启动x轴移动
    private TimerTask timerTaskPush1;
    private TimerTask timerTaskPush2;
    private Timer mTimerOff = new Timer();//遮挡取货门所用定时器
    private TimerTask timerTaskOff;
    private int closeGetGoodsDoorTimeNo = 25;//首次取货门开门所用延时计数，防止被后台kill，同时根据购买数量确定延时时间



    private static int packageCount1 = 0;//存放左右柜分别打包的个数,每三个凑成一个包，完成一次轮询出货
    private static int packageCount2 = 0;
    private int moveTime = 700;
    private int moveTimeOut = 5500;
    private int delay = 220;
    private int delayGetDoor1 = delayGetDoor;
    private int[][] goods1;
    private int[][] goods2;



    private int currentOutCount1 = 0;//存放左右柜当前出货的序号
    private int currentOutCount2 = 0;//存放左右柜当前出货的序号
    private int currentPackageCount1 = 0;//存放左右柜当前出货的包号
    private int currentPackageCount2 = 0;//存放左右柜当前出货的包号

    //逻辑相关，五块板子的当前状态
    private int rimBoard1 = Constant.wait;
    private int rimBoard2 = Constant.wait;
    private int aisleBoard1 = Constant.wait;
    private int aisleBoard2 = Constant.wait;
    private int midBoard = Constant.wait;

    //标志位
    private boolean closeOutGoodsDoor1 = true;
    private boolean closeOutGoodsDoor2 = true;
    private boolean closeGetGoodsDoor = true;
    private boolean finshDone1 = false;//把货物推送到中柜并关上出货门的标志位
    private boolean finshDone2 = false;
    private boolean pushGoods1 = false;//用来判断是否推货完毕，来决定x轴是否再次平移用以解决货物遮挡光栅的情况
    private boolean pushGoods2 = false;
    private boolean complete = false;//用来确定只给上位机发送一次出货成功的广播
    private boolean pushGoodsQuery1 = false;//从货道推货查询到运动结束后，会存在货物卡住掉下来过慢，错误认为货物超时的情况，延时一段时间再次查询，次标志位用来限制再次查询一次
    private boolean pushGoodsQuery2 = false;


    private boolean Error1 = false;//单柜停机故障标志位，用来确认单柜故障，判断是否继续运行
    private boolean Error2 = false;

    private String error_ID_closeAisle = "";//记录出货完的上报信息
    private String error_ID_justRecord = "";

    private int outGoodsDoorErrorCount1 = 0;//开关出货门存在超时或者半开半关，重新尝试的次数
    private int outGoodsDoorErrorCount2 = 0;
    private int getGoodsDoorErrorCount = 0;//开关取货门存在超时或者半开半关，重新尝试的次数
    private int needMoveHorizontalOut1Again = 0;//如果x轴出货有货物一直卡在出货光栅或者未检测到货物，则再次尝试x轴出货（两次或者一次）
    private int needMoveHorizontalOut2Again = 0;

    private int none = 0;//用来放在run里，防止长时间没有动作被系统回收资源
    private int moveFloorOutRevise = 50;//用来表示云台移动到出货口下方的距离，通过折返动作校正移动误差（码盘格位数）


    public OutGoodsThread(Context context){
        super();
        this.context = context;
        serialPort485 = new SerialPort(1, 38400, 8, 'n', 1);
        mMotorControl = new MotorControl(serialPort485,context);
        pDao = new PositionDaoImpl(context);
        tDao = new TransactionDaoImpl(context);
        mDao = new MachineStateDaoImpl(context);
    }

    public void init() {
        queryLastedTransaction = tDao.queryLastedTransaction();
        Log.w("happy", queryLastedTransaction.toString());
        Util.WriteFile(queryLastedTransaction.toString());
        String [] str = queryLastedTransaction.getPositionIDs().split(" ");
        int[] positionIDs = new int[str.length];
        closeGetGoodsDoorTimeNo = 20 + str.length*5;//根据本次购买的数量确定取货门开门后的延时时长
        List<Integer> str1 = new ArrayList<>();
        List<Integer> str2 = new ArrayList<>();
        for (int i = 0; i < str.length; i++){
            positionIDs[i] = Integer.parseInt(str[i]);
            if(positionIDs[i] <= 100){
                str1.add(positionIDs[i]);
            }else if(positionIDs[i] > 100){
                str2.add(positionIDs[i]);
            }
        }
        int[] positionIDs1 = new int[str1.size()];
        int[] positionIDs2 = new int[str2.size()];
        int tmp = 0;
        for (Integer i : str1){
            positionIDs1[tmp++] = i;
        }
        tmp = 0;
        for (Integer i : str2){
            positionIDs2[tmp++] = i;
        }
        Arrays.sort(positionIDs1);
        Arrays.sort(positionIDs2);
        //每三件一个包
        packageCount1 = (str1.size() % 3 == 0 ? (str1.size() / 3): (str1.size() / 3 + 1));
        packageCount2 = (str2.size() % 3 == 0 ? (str2.size() / 3): (str2.size() / 3 + 1));
        goods1 = calculateOrientationTime(1, positionIDs1,packageCount1);
        goods2 = calculateOrientationTime(2, positionIDs2,packageCount2);

        Log.w("happy", ""+ Arrays.deepToString(goods1));
        Log.w("happy", ""+ Arrays.deepToString(goods2));
        Util.WriteFile("outGoods1："+Arrays.deepToString(goods1));
        Util.WriteFile("outGoods2："+Arrays.deepToString(goods2));
        Log.w("happy", "出货主线程初始化，计算出货数组完毕");
        Util.WriteFile("出货主线程初始化，计算出货数组完毕");
        if(packageCount1 > 0){
            rimBoard1 = Constant.moveFloor;
        }else{
            rimBoard1 = Constant.wait;
            finshDone1 = true;
        }
        if(packageCount2 > 0){
            rimBoard2 = Constant.moveFloor;
        }else{
            rimBoard2 = Constant.wait;
            finshDone2 = true;
        }
    }

    @Override
    public void run() {
        super.run();
        while(OutGoodsThreadFlag){
            /*发送运动指令逻辑*/
            switch (rimBoard1){
                case 0:{/*等待*/
                    break;
                }
                case 1:{/*移层_货道位置*/
                    moveFloor1();
                    break;
                }
                case 2:{/*移层_出货口位置*/
                    moveFloorOut1();
                    break;
                }
                case 3:{/*水平移位_错位*/
                    moveHorizontal1();
                    break;
                }
                case 4:{/*水平移位_推入出货口*/
                    moveHorizontalOut1();
                    break;
                }
                case 5:{/*开出货门*/
                    openOutGoodsDoor1();
                    break;
                }
                case 6:{/*关出货门*/
                    closeOutGoodsDoor1();
                    break;
                }
                case 7:{/*云台归位*/
                    homing1();
                    break;
                }
                case 8:{/*查询移层_货道位置*/
                    queryMoveFloor1();
                    break;
                }
                case 9:{/*查询移层_出货口位置*/
                    queryMoveFloorOut1();
                    break;
                }
                case 10:{/*查询水平移位_错位*/
                    queryMoveHorizontal1();
                    break;
                }
                case 11:{/*查询水平移位_推入出货口*/
                    queryMoveHorizontalOut1();
                    break;
                }
                case 12:{/*查询开出货门*/
                    queryOpenOutGoodsDoor1();
                    break;
                }
                case 13:{/*查询关出货门*/
                    queryCloseOutGoodsDoor1();
                    break;
                }
                case 14:{/*查询云台归位*/
                    queryHoming1();
                    break;
                }
                case 15:{/*移层_出货口位置（折返动作校正传送带导致的Y轴误差）*/
                    moveFloorOutRevise1();
                    break;
                }
                case 16:{/*查询移层_出货口位置（折返动作校正传送带导致的Y轴误差）*/
                    queryMoveFloorOutRevise1();
                    break;
                }
            }
            switch (aisleBoard1){
                case 0:{/*等待*/
                    break;
                }
                case 1:{/*推送货*/
                    pushGoods1();
                    break;
                }
                case 2:{/*查询推送货*/
                    queryPushGoods1();
                    break;
                }
            }
            switch (rimBoard2){
                case 0:{/*等待*/
                    break;
                }
                case 1:{/*移层_货道位置*/
                    moveFloor2();
                    break;
                }
                case 2:{/*移层_出货口位置*/
                    moveFloorOut2();
                    break;
                }
                case 3:{/*水平移位_错位*/
                    moveHorizontal2();
                    break;
                }
                case 4:{/*水平移位_推入出货口*/
                    moveHorizontalOut2();
                    break;
                }
                case 5:{/*开出货门*/
                    openOutGoodsDoor2();
                    break;
                }
                case 6:{/*关出货门*/
                    closeOutGoodsDoor2();
                    break;
                }
                case 7:{/*云台归位*/
                    homing2();
                    break;
                }
                case 8:{/*查询移层_货道位置*/
                    queryMoveFloor2();
                    break;
                }
                case 9:{/*查询移层_出货口位置*/
                    queryMoveFloorOut2();
                    break;
                }
                case 10:{/*查询水平移位_错位*/
                    queryMoveHorizontal2();
                    break;
                }
                case 11:{/*查询水平移位_推入出货口*/
                    queryMoveHorizontalOut2();
                    break;
                }
                case 12:{/*查询开出货门*/
                    queryOpenOutGoodsDoor2();
                    break;
                }
                case 13:{/*查询关出货门*/
                    queryCloseOutGoodsDoor2();
                    break;
                }
                case 14:{/*查询云台归位*/
                    queryHoming2();
                    break;
                }
                case 15:{/*移层_出货口位置（折返动作校正传送带导致的Y轴误差）*/
                    moveFloorOutRevise2();
                    break;
                }
                case 16:{/*查询移层_出货口位置（折返动作校正传送带导致的Y轴误差）*/
                    queryMoveFloorOutRevise2();
                    break;
                }
            }
            switch (aisleBoard2){
                case 0:{/*等待*/
                    break;
                }
                case 1:{/*推送货*/
                    pushGoods2();
                    break;
                }
                case 2:{/*查询推送货*/
                    queryPushGoods2();
                    break;
                }
            }
            switch (midBoard){
                case 0:{/*等待*/
                    break;
                }
                case 1:{/*开取货门*/
                    openGetGoodsDoor();
                    break;
                }
                case 2:{/*关取货门*/
                    closeGetGoodsDoor();
                    break;
                }
                case 3:{/*开落货门*/
//                    openDropGoodsDoor();
                    break;
                }
                case 4:{/*关落货门*/
//                    closeDropGoodsDoor();
                    break;
                }
                case 5:{/*查询开取货门*/
                    queryOpenGetGoodsDoor();
                    break;
                }
                case 6:{/*查询关取货门*/
                    queryCloseGetGoodsDoor();
                    break;
                }
                case 7:{/*查询开落货门*/
//                    queryOpenDropGoodsDoor();
                    break;
                }
                case 8:{/*查询关落货门*/
//                    queryCloseDropGoodsDoor();
                    break;
                }
            }
            none++;
            if(none == 10){
                none = 0;
            }
            if(rimBoard1 == Constant.wait &&
                    rimBoard2 == Constant.wait &&
                    aisleBoard1 == Constant.wait &&
                    aisleBoard2 == Constant.wait &&
                    midBoard == Constant.wait){
                mMotorControl.query((byte)0x07,(byte)0xE0,midZNum);
                SystemClock.sleep(delayGetDoor1);
                byte[] rec = serialPort485.receiveData();
            }
        }
    }

    private int[][] calculateOrientationTime(int counter, int[] set ,int packageCount){
        int[][] outGoods = new int[packageCount][9];
        if(packageCount > 0) {
            /*循环每个包*/
            for (int i = 1; i <= packageCount; i++) {
                /*打包只有一件货*/
                if ((set.length - (i - 1) * 3) == 1) {
                    int w = isWhere(counter,set[(i-1)*3]);
                    if(w <= 3){
                        outGoods[i-1][0] = set[(i-1)*3];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        for(int j= 3; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else{
                        outGoods[i-1][0] = set[(i-1)*3];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        for(int j= 3; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }
                }
                /*打包两件货*/
                else if((set.length - (i - 1) * 3) == 2){
                    /*先排序*/
                    int sort[] = new int[2];
                    int where[] = new int[2];
                    where[0]=isWhere(counter,set[(i-1)*3]);
                    where[1]=isWhere(counter,set[(i-1)*3+1]);
                    Map<Integer, Integer> map = new TreeMap<>();
                    map.put(0, where[0]);
                    map.put(1, where[1]);
                    List<Map.Entry<Integer,Integer>> list = new ArrayList<>(map.entrySet());
                    //然后通过比较器来实现排序
                    Collections.sort(list,new Comparator<Map.Entry<Integer,Integer>>() {
                        public int compare(Map.Entry<Integer, Integer> o1,
                                           Map.Entry<Integer, Integer> o2) {
                            return o1.getValue().compareTo(o2.getValue());
                        }
                    });
                    int no = 0;
                    for(Map.Entry<Integer,Integer> mapping:list){
                        sort[no++] = set[(i-1)*3 + mapping.getKey()];
                    }
                    int w1 = isWhere(counter,sort[0]);
                    int w2 = isWhere(counter,sort[1]);
                    if((w1==1&&w2==1) || (w1==1&&w2==3) || (w1==2&&w2==2)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else if((w1==4&&w2==4) || (w1==4&&w2==5) || (w1==5&&w2==5)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else if((w1==2&&w2==3) || (w1==2&&w2==5)|| (w1==3&&w2==3) || (w1==3&&w2==4)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*2;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else if((w1==1&&w2==5) || (w1==1&&w2==4)){
                        outGoods[i-1][0] = sort[1];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime*4;
                        outGoods[i-1][3] = sort[0];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else if((w1==1&&w2==2)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*2;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else if((w1==3&&w2==5) || (w1==2&&w2==4) ){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else{
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 0;
                        outGoods[i-1][5] = 0;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }
                }
                /*打包三件货*/
                else{
                    /*先排序*/
                    int sort[] = new int[3];
                    int where[] = new int[3];
                    where[0]=isWhere(counter,set[(i-1)*3]);
                    where[1]=isWhere(counter,set[(i-1)*3+1]);
                    where[2]=isWhere(counter,set[(i-1)*3+2]);
                    Map<Integer, Integer> map = new TreeMap<>();
                    map.put(0, where[0]);
                    map.put(1, where[1]);
                    map.put(2, where[2]);
                    List<Map.Entry<Integer,Integer>> list = new ArrayList<>(map.entrySet());
                    //然后通过比较器来实现排序
                    Collections.sort(list,new Comparator<Map.Entry<Integer,Integer>>() {
                        public int compare(Map.Entry<Integer, Integer> o1,
                                           Map.Entry<Integer, Integer> o2) {
                            return o1.getValue().compareTo(o2.getValue());
                        }
                    });
                    int no = 0;
                    for(Map.Entry<Integer,Integer> mapping:list){
                        sort[no++] = set[(i-1)*3 + mapping.getKey()];
                    }
                    int w1 = isWhere(counter,sort[0]);
                    int w2 = isWhere(counter,sort[1]);
                    int w3 = isWhere(counter,sort[2]);
                    if((w1==1&&w2==1&&w3==1) || (w1==1&&w2==1&&w3==4) || (w1==2&&w2==1&&w3==2)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==1&&w3==2)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime*3;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==1&&w3==3) || (w1==1&&w2==2&&w3==2)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*2;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==1&&w3==5)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime*4;
                        outGoods[i-1][3] = sort[0];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[1];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==2&&w3==3)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[0];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==2&&w3==4) || (w1==2&&w2==3&&w3==4)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*2;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==2&&w3==5)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime*4;
                        outGoods[i-1][3] = sort[0];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[1];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==2&&w2==2&&w3==4) || (w1==2&&w2==3&&w3==3)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[0];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==3&&w3==3)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[0];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime*3/2;
                        outGoods[i-1][6] = sort[1];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==3&&w3==4)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*3;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==3&&w3==5) || (w1==1&&w2==4&&w3==4)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[0];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==2&&w2==2&&w3==5)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime*2;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[0];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==3&&w2==3&&w3==3)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime/2;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==5&&w3==5)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[0];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==2&&w2==2&&w3==3)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime/2;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==2&&w2==4&&w3==4) || (w1==3&&w2==3&&w3==4)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==2&&w2==5&&w3==5) || (w1==3&&w2==4&&w3==5)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[0];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==3&&w2==3&&w3==5)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 1;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==3&&w2==4&&w3==4)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*2;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==3&&w2==5&&w3==5) || (w1==4&&w2==4&&w3==5)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 1;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==4&&w2==4&&w3==4)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==5&&w2==5&&w3==5)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 1;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==2&&w2==4&&w3==5)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*2;
                        outGoods[i-1][3] = sort[2];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[1];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==1&&w2==4&&w3==5)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*3;
                        outGoods[i-1][3] = sort[2];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[1];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==2&&w2==3&&w3==5)){
                        outGoods[i-1][0] = sort[1];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[2];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[0];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==4&&w2==5&&w3==5)){
                        outGoods[i-1][0] = sort[2];
                        outGoods[i-1][1] = 1;
                        outGoods[i-1][2] = moveTime;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[0];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }else if((w1==2&&w2==2&&w3==2)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 2;
                        outGoods[i-1][2] = moveTime*2;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime*2;
                    }else{
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 0;
                        outGoods[i-1][5] = 0;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 0;
                        outGoods[i-1][8] = 0;
                    }
                }
            }
        }else if(packageCount == 0){
            outGoods = null;
        }
        return outGoods;
    }

    private int isWhere(int counter, int positionID) {
        int where = 0;
        Position p = pDao.queryPosition(positionID ,counter);
        int row1 = p.getPosition1() % 16;
        int row2 = p.getPosition2() % 16;
        switch (p.getMotorType()) {
            case 4:
                if(wuOrsihuodaoxian.equals("13579_1357")){
                    if(pDao.queryPositionNo(positionID ,counter) <= 4){
                        if(row1 == 1){
                            where = 1;
                        }else if(row1==3){
                            where = 2;
                        }else if(row1==5){
                            where = 4;
                        }else if(row1 == 7){
                            where = 5;
                        }else{
                            where = 5;
                        }
                    }else {
                        if(row1 == 1){
                            where = 1;
                        }else if(row1==3){
                            where = 2;
                        }else if(row1==5){
                            where = 3;
                        }else if(row1 == 7){
                            where = 4;
                        }else{
                            where = 5;
                        }
                    }
                }else{
                    if(pDao.queryPositionNo(positionID ,counter) <= 4){
                        if(row1 == 1 || row1 == 2){
                            where = 1;
                        }else if(row1==3){
                            where = 2;
                        }else if(row1==4){
                            where = 4;
                        }else if(row1 == 5){
                            where = 5;
                        }else{
                            where = 5;
                        }
                    }else {
                        if(row1 == 1){
                            where = 1;
                        }else if(row1==2){
                            where = 2;
                        }else if(row1==3){
                            where = 3;
                        }else if(row1 == 4){
                            where = 4;
                        }else{
                            where = 5;
                        }
                    }
                }
                break;
            case 3:
                where = row1 % 2 == 0 ? row1 / 2 : (row1 / 2 + 1);
                break;
            case 2:
                if(pDao.queryPositionNo(positionID ,counter) == 6 && pDao.queryPosition((positionID-1)/10 * 10+1 ,counter).getMotorType() == 2 && pDao.queryPosition((positionID-1)/10 * 10+6 ,counter).getMotorType() == 2){
                    if(row1<=3){
                        where = row1;
                    }else if(row1>=4){
                        where = row1-1;
                    }
                }else if(pDao.queryPositionNo(positionID ,counter) == 10){
                    if (counter == 1) {
                        where = row1 / 2;
                    } else {
                        where = row1 / 2 + 1;
                    }
                }else if(pDao.queryPositionNo(positionID ,counter) == 8){
                    where = row1 / 2 + 1;
                }else{
                    if (counter == 1) {
                        where = row1 / 2;
                    } else {
                        where = row1 / 2 + 1;
                    }
                }
                break;
            case 1:
                if(pDao.queryPositionNo(positionID ,counter) == 8){
                    if (counter == 1) {
                        if (row1 == 1 || row1 == 2) {
                            where = 1;
                        } else if (row1 == 3 || row1 == 4) {
                            where = 2;
                        } else if (row1 == 5 || row1 == 6) {
                            where = 3;
                        }else{
                            where = 4;
                        }
                    } else {
                        if (row1 == 3 || row1 == 4) {
                            where = 3;
                        } else if (row1 == 5 || row1 == 6) {
                            where = 4;
                        } else if (row1 == 7 || row1 == 8) {
                            where = 5;
                        }else{
                            where = 2;
                        }
                    }
                }else{
                    if (counter == 1) {
                        if (row1 == 1 || row1 == 2) {
                            where = 1;
                        } else if (row1 == 3 || row1 == 4) {
                            where = 2;
                        } else if (row1 == 5 || row1 == 6) {
                            where = 3;
                        } else if (row1 == 7 || row1 == 8) {
                            where = 4;
                        }else{
                            where = 5;
                        }
                    } else {
                        if (row1 == 3 || row1 == 4 || row1 == 5) {
                            where = 3;
                        } else if (row1 == 6 || row1 == 7 || row1 == 8) {
                            where = 4;
                        } else if (row1 == 9 || row1 == 10) {
                            where = 5;
                        }else{
                            where = 1;
                        }
                    }
                }
                break;
        }
        return  where;//1-5
    }

//    /**
//     * 接受指令预处理
//     * 说明：假如指令前后存在乱码则处理掉,暂时不用，预防特殊情况
//     * @param rec 接收到的串口数据
//     * */
//    private byte[] preprocessRec(byte[] rec){
//        byte[] rec1;
//        if(rec != null && rec.length >= 5) {
//            StringBuilder str = new StringBuilder();
//            for (byte aRec1 : rec) {
//                str.append(Integer.toHexString(aRec1 & 0xFF)).append(" ");
//            }
//            Log.w("happy", "收到原始串口："+ str);
//        }
//        if(rec != null && rec.length >= 5) {
//            if (rec[0] != (byte) 0xE2 || rec[rec.length - 2] != (byte) 0xF1) {
//                boolean head = true;
//                boolean trail = true;
//                int start = 0;
//                int end = 0;
//                for (int y = 0; y < rec.length; y++) {
//                    if (head) {
//                        if (rec[y] == 0xE2) {
//                            head = false;
//                            start = y;
//                        }
//                    }
//                    if (trail) {
//                        if (rec[rec.length - 1 - y] == 0xF1) {
//                            trail = false;
//                            end = y - 1;
//                        }
//                    }
//                }
//                rec1 = new byte[rec.length - end - start];
//                System.arraycopy(rec, start, rec, 0, rec.length - end - start);
//            }else{
//                rec1 = new byte[rec.length];
//                rec1 = rec;
//            }
//            return rec1;
//        }
//        return rec;
//    }
//
//    /**
//     * 处理通信故障
//     * 说明：判定通信故障后调用
//     * */
//    private void handleCommunicationError(){
//        queryLastedTransaction.setComplete(1);
//        queryLastedTransaction.setError(1);
//        tDao.updateTransaction(queryLastedTransaction);
//        OutGoodsThreadFlag = false;
//        VMMainThreadFlag = true;
//        mQuery1Flag = true;
//        mQuery2Flag = true;
//        mQuery0Flag = true;
//        mUpdataDatabaseFlag = true;
//        SystemClock.sleep(20);
//        Intent intent = new Intent();
//        intent.setAction("njust_outgoods_complete");
//        intent.putExtra("transaction_order_number", current_transaction_order_number);
//        intent.putExtra("outgoods_status", "fail");
//        context.sendBroadcast(intent);
//    }
//
//    /**
//     * 校验和验证
//     * 说明：验证接收到的数据是否正确
//     * @param rec 接收到的串口数据或者处理过的串口数据
//     * */
//    private static boolean isVerify(byte[] rec){
//        int sum = 0;
//        for (int i = 0; i < rec.length - 1; i++) {
//            sum = sum + rec[i];
//        }
//        return sum == rec[rec.length-1];
//    }


    private void timeStartPush(int counter){
        if(counter == 1){
            if(timerTaskPush1 != null){
                timerTaskPush1.cancel();
                timerTaskPush1 = null;
            }
            timerTaskPush1 = new TimerTask() {
                @Override
                public void run() {
                    rimBoard1 = Constant.moveHorizontal;
                }
            };
            mTimer.schedule(timerTaskPush1,1100);
        }else{
            if(timerTaskPush2 != null){
                timerTaskPush2.cancel();
                timerTaskPush2 = null;
            }
            timerTaskPush2 = new TimerTask() {
                @Override
                public void run() {
                    rimBoard2 = Constant.moveHorizontal;
                }
            };
            mTimer.schedule(timerTaskPush2,1100);
        }
    }
    private void timeStart(){
        if(timerTask != null){
            timerTask.cancel();
            timerTask = null;
        }
        final int[] t = {0};//总是被系统kill，延时放短
        timerTask = new TimerTask() {
            @Override
            public void run() {
                t[0]++;
                if(t[0] == closeGetGoodsDoorTimeNo){
                    midBoard = Constant.closeGetGoodsDoor;
                    Util.WriteFile("取货等待定时器到时间，关闭取货门");
                    t[0] = 0;
                    timerTask.cancel();
                    timerTask = null;
                }
            }
        };
        mTimer.schedule(timerTask,200,200);
        Util.WriteFile("开启取货等待定时器，"+closeGetGoodsDoorTimeNo*200+"ms后关闭取货门");
    }
    private void timeStartOff(){
        if(timerTaskOff != null){
            timerTaskOff.cancel();
            timerTaskOff = null;
        }
        final int[] t = {0};//总是被系统kill，延时放短
        timerTaskOff = new TimerTask() {
            @Override
            public void run() {
                t[0]++;
                if(t[0] == 15){//3秒
                    midBoard = Constant.closeGetGoodsDoor;
                    Util.WriteFile("防夹手到时间再次关闭取货门");
                    t[0] = 0;
                    timerTaskOff.cancel();
                    timerTaskOff = null;
                }
            }
        };
        mTimerOff.schedule(timerTaskOff,200,200);
        Util.WriteFile("开启遮挡取货门防夹手定时器，定时3s后再次关闭");
    }


    private void moveFloor1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveFloor(1,rimZNum1,goods1[currentPackageCount1][currentOutCount1*3]);
            Log.w("happy", "发送左柜移层指令");Util.WriteFile("发送左柜移层指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜移层反馈："+ str1);Util.WriteFile("左柜移层反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimBoard1 = Constant.queryMoveFloor;
                            rimZNum1++;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if(packageCount2 <= 0 || Error2){
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板移层指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("左边柜板移层指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                }else{
                    rimBoard1 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    rimZNum1++;
                    finshDone1 = true;
                    if(finshDone2){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error1 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板移层指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("左边柜板移层指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveFloor1(){
        mMotorControl.query((byte)0x01,(byte)0xC0,rimZNum1);
        Log.w("happy", "发送左柜移层查询");Util.WriteFile("发送左柜移层查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜移层查询反馈："+ str1);Util.WriteFile("左柜移层查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        if((rec[9]&0x7E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位,移层不检查Bit7=1，Y轴出货门定位开关故障，未使用定位开关，新加升降台上止点开关即时状态（0=开，1=合）不能触发上止点，触发为合
                            String error_ID_Y = "";
                            String log = "左柜移层检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "Y轴电机过流，";
                                error_ID_Y += "1,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "Y轴电机断路，";
                                error_ID_Y += "3,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "Y轴上止点开关故障，";
                                error_ID_Y += "5,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "Y轴下止点开关故障，";
                                error_ID_Y += "7,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "Y轴电机超时，";
                                error_ID_Y += "9,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "Y轴码盘故障，";
                                error_ID_Y += "11,";
                            }
                            if((rec[9]&0x80) != (byte)0x00){
                                log += "Y轴出货门定位开关故障，";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "Y轴触发上止点开关，";
                                error_ID_Y += "26,";
                            }
                            error_ID_Y = error_ID_Y.substring(0,error_ID_Y.length()-1);
                            log += "停止出货。";
                            log += "  Y轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" Y轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" Y轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff))
                                    +" Y轴电机当前实际位置（自零点起齿位数）:"+ ((rec[16]&0xff) * 256 + (rec[17]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount2 <= 0 || Error2){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左边柜板移层查询反馈Y轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("左边柜板移层查询反馈Y轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard1 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                rimZNum1++;
                                finshDone1 = true;
                                if(finshDone2){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error1 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左边柜板移层查询反馈Y轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("左边柜板移层查询反馈Y轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimBoard1 = Constant.wait;
                            aisleBoard1 = Constant.pushGoods;
                            rimZNum1++;
                        }
                    }
                }
            }
        }
    }

    private void moveFloor2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            Log.w("happy", "发送右柜移层指令");Util.WriteFile("发送右柜移层指令");
            mMotorControl.moveFloor(2,rimZNum2,goods2[currentPackageCount2][currentOutCount2*3]);
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜移层反馈："+ str1);Util.WriteFile("右柜移层反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimBoard2 = Constant.queryMoveFloor;
                            rimZNum2++;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5) {
                flag = false;
                if (packageCount1 <= 0 || Error1) {
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板移层指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("右边柜板移层指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                } else{
                    rimBoard2 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    rimZNum2++;
                    finshDone2 = true;
                    if(finshDone1){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error2 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板移层指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("右边柜板移层指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveFloor2(){
        mMotorControl.query((byte)0x01,(byte)0xC1,rimZNum2);
        Log.w("happy", "发送右柜移层查询");Util.WriteFile("发送右柜移层查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜移层查询反馈："+ str1);Util.WriteFile("右柜移层查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        if((rec[9]&0x7E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位,移层不检查Bit7=1，Y轴出货门定位开关故障，未使用定位开关，新加升降台上止点开关即时状态（0=开，1=合）不能触发上止点，触发为合
                            String error_ID_Y = "";
                            String log = "右柜移层检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "Y轴电机过流，";
                                error_ID_Y += "2,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "Y轴电机断路，";
                                error_ID_Y += "4,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "Y轴上止点开关故障，";
                                error_ID_Y += "6,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "Y轴下止点开关故障，";
                                error_ID_Y += "8,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "Y轴电机超时，";
                                error_ID_Y += "10,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "Y轴码盘故障，";
                                error_ID_Y += "12,";
                            }
                            if((rec[9]&0x80) != (byte)0x00){
                                log += "Y轴出货门定位开关故障，";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "Y轴触发上止点开关，";
                                error_ID_Y += "27,";
                            }
                            error_ID_Y = error_ID_Y.substring(0,error_ID_Y.length()-1);
                            log += "停止出货。";
                            log += "  Y轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" Y轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" Y轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff))
                                    +" Y轴电机当前实际位置（自零点起齿位数）:"+ ((rec[16]&0xff) * 256 + (rec[17]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount1 <= 0 || Error1){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右边柜板移层查询反馈Y轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("右边柜板移层查询反馈Y轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard2 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                rimZNum2++;
                                finshDone2 = true;
                                if(finshDone1){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error2 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右边柜板移层查询反馈Y轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("右边柜板移层查询反馈Y轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimBoard2 = Constant.wait;
                            aisleBoard2 = Constant.pushGoods;
                            rimZNum2++;
                        }
                    }
                }
            }
        }
    }

    private void pushGoods1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.pushGoods(1,aisleZNum1,goods1[currentPackageCount1][currentOutCount1*3]);
            Log.w("happy", "发送左柜推货"+goods1[currentPackageCount1][currentOutCount1*3]);Util.WriteFile("发送左柜推货"+goods1[currentPackageCount1][currentOutCount1*3]);
            SystemClock.sleep(delay+10);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜推货反馈："+ str1);Util.WriteFile("左柜推货反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x70 && rec[3] == (byte)0x80 && rec[7] == (byte)0x50){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            pushGoods1 = false;
                            pushGoodsQuery1 = false;
                            timeStartPush(1);
                            aisleBoard1 = Constant.queryPushGoods;
                            aisleZNum1++;
                            SystemClock.sleep(50);
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if(packageCount2 <= 0 || Error2){
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "23");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左货道板推货指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("左货道板推货指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                }else{
                    rimBoard1 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    rimZNum1++;
                    finshDone1 = true;
                    if(finshDone2){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error1 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "23");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左货道板推货指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("左货道板推货指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryPushGoods1(){
        mMotorControl.query((byte)0x03,(byte)0x80,aisleZNum1);
        Log.w("happy", "发送左柜推货查询");Util.WriteFile("发送左柜推货查询");
        SystemClock.sleep(delay+10);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜推货查询反馈："+ str1);Util.WriteFile("左柜推货查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x70 && rec[3] == (byte)0x80 && rec[7] == (byte)0x50){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x80) != (byte)0x00){//货道下货光栅故障
                            if(packageCount2 <= 0 || Error2){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", "19");
                                context.sendBroadcast(intent);
                                Log.w("happy", "左货道板下货光栅故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("左货道板下货光栅故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard1 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                rimZNum1++;
                                finshDone1 = true;
                                if(finshDone2){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error1 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", "19");
                                context.sendBroadcast(intent);
                                Log.w("happy", "左货道板下货光栅故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("左货道板下货光栅故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                            }
                        }else{//不存在下货光栅故障
                            if(!pushGoodsQuery1 && (rec[9]&0x18) != (byte)0x00){//如果存在货道超时（无货物输出的超时）、商品超时（货物遮挡光栅超时）
                                aisleZNum1++;
                                pushGoodsQuery1 = true;
                                SystemClock.sleep(500);
                                Log.w("happy", "存在超时，延时500ms后再次发送左柜推货查询");Util.WriteFile("存在超时，延时500ms后再次发送左柜推货查询");
                            }else{
                                if(((rec[8]&0x02) == (byte)0x00 && (rec[9]&0x7E) != (byte)0x00) || ((rec[8]&0x02) != (byte)0x00 && (rec[9]&0x66) != (byte)0x00)){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位
                                    String log = "左柜货道"+goods1[currentPackageCount1][currentOutCount1*3]+"检测到错误：";
                                    error_ID_closeAisle = error_ID_closeAisle + goods1[currentPackageCount1][currentOutCount1*3]+":";
                                    if ((rec[9]&0x01) != (byte)0x00) {
                                        log += "已执行动作，";
                                    } else {
                                        log += "未执行动作，";
                                    }
                                    if((rec[9]&0x02) != (byte)0x00){
                                        log += "货道电机过流，";
                                        error_ID_closeAisle += "1,";
                                    }
                                    if((rec[9]&0x04) != (byte)0x00){
                                        log += "货道电机断路，";
                                        error_ID_closeAisle += "3,";
                                    }
                                    if((rec[8]&0x02) == (byte)0x00 && (rec[9]&0x08) != (byte)0x00){
                                        log += "货道超时（无货物输出的超时），";
                                        error_ID_closeAisle += "9,";
                                    }
                                    if((rec[8]&0x02) == (byte)0x00 && (rec[9]&0x10) != (byte)0x00){
                                        log += "商品超时（货物遮挡光栅超时），";
                                        error_ID_closeAisle += "11,";
                                    }
                                    if((rec[9]&0x20) != (byte)0x00){
                                        log += "弹簧电机1反馈开关故障，";
                                        error_ID_closeAisle += "5,";
                                    }
                                    if((rec[9]&0x40) != (byte)0x00){
                                        log += "弹簧电机2反馈开关故障，";
                                        error_ID_closeAisle += "7,";
                                    }
                                    error_ID_closeAisle = error_ID_closeAisle.substring(0,error_ID_closeAisle.length()-1);
                                    error_ID_closeAisle += ";";
                                    log = log.substring(0,log.length()-1);
                                    log += "  货道电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                            +" 货道电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                            +" 货道电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                                    Log.w("happy", ""+log);
                                    Util.WriteFile(log);
                                }
                                aisleBoard1 = Constant.wait;
                                pushGoods1 = true;
                                aisleZNum1++;
                            }
                        }
                    }
                }
            }
        }
    }

    private void pushGoods2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.pushGoods(2,aisleZNum2,goods2[currentPackageCount2][currentOutCount2*3]);
            Log.w("happy", "发送右柜推货"+goods2[currentPackageCount2][currentOutCount2*3]);Util.WriteFile("发送右柜推货"+goods2[currentPackageCount2][currentOutCount2*3]);
            SystemClock.sleep(delay+10);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜推货反馈："+ str1);Util.WriteFile("右柜推货反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x70 && rec[3] == (byte)0x81 && rec[7] == (byte)0x50){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            pushGoods2 = false;
                            pushGoodsQuery2 = false;
                            timeStartPush(2);
                            aisleBoard2 = Constant.queryPushGoods;
                            aisleZNum2++;
                            SystemClock.sleep(50);
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if (packageCount1 <= 0 || Error1) {
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "24");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右货道板推货指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("右货道板推货指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                } else{
                    rimBoard2 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    rimZNum2++;
                    finshDone2 = true;
                    if(finshDone1){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error2 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "24");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右货道板推货指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("右货道板推货指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryPushGoods2(){
        mMotorControl.query((byte)0x03,(byte)0x81,aisleZNum2);
        Log.w("happy", "发送右柜推货查询");Util.WriteFile("发送右柜推货查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜推货查询反馈："+ str1);Util.WriteFile("右柜推货查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x70 && rec[3] == (byte)0x81 && rec[7] == (byte)0x50){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x80) != (byte)0x00){//货道下货光栅故障
                            if(packageCount1 <= 0 || Error1){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", "20");
                                context.sendBroadcast(intent);
                                Log.w("happy", "右货道板下货光栅故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("右货道板下货光栅故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard2 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                rimZNum2++;
                                finshDone2 = true;
                                if(finshDone1){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error2 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", "20");
                                context.sendBroadcast(intent);
                                Log.w("happy", "右货道板下货光栅故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("右货道板下货光栅故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                            }
                        }else{//不存在下货光栅故障
                            if(!pushGoodsQuery2 && (rec[9]&0x18) != (byte)0x00){//如果存在货道超时（无货物输出的超时）、商品超时（货物遮挡光栅超时）
                                aisleZNum2++;
                                pushGoodsQuery2 = true;
                                SystemClock.sleep(500);
                                Log.w("happy", "存在超时，延时500ms后再次发送右柜推货查询");Util.WriteFile("存在超时，延时500ms后再次发送右柜推货查询");
                            }else {
                                if(((rec[8]&0x02) == (byte)0x00 && (rec[9]&0x7E) != (byte)0x00) || ((rec[8]&0x02) != (byte)0x00 && (rec[9]&0x66) != (byte)0x00)){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位
                                    String log = "右柜货道" + goods2[currentPackageCount2][currentOutCount2 * 3] + "检测到错误：";
                                    error_ID_closeAisle = error_ID_closeAisle + goods2[currentPackageCount2][currentOutCount2 * 3] + ":";
                                    if ((rec[9] & 0x01) != (byte) 0x00) {
                                        log += "已执行动作，";
                                    } else {
                                        log += "未执行动作，";
                                    }
                                    if ((rec[9] & 0x02) != (byte) 0x00) {
                                        log += "货道电机过流，";
                                        error_ID_closeAisle += "2,";
                                    }
                                    if ((rec[9] & 0x04) != (byte) 0x00) {
                                        log += "货道电机断路，";
                                        error_ID_closeAisle += "4,";
                                    }
                                    if ((rec[8]&0x02) == (byte)0x00 && (rec[9] & 0x08) != (byte) 0x00) {
                                        log += "货道超时（无货物输出的超时），";
                                        error_ID_closeAisle += "10,";
                                    }
                                    if ((rec[8]&0x02) == (byte)0x00 && (rec[9] & 0x10) != (byte) 0x00) {
                                        log += "商品超时（货物遮挡光栅超时），";
                                        error_ID_closeAisle += "12,";
                                    }
                                    if ((rec[9] & 0x20) != (byte) 0x00) {
                                        log += "弹簧电机1反馈开关故障，";
                                        error_ID_closeAisle += "6,";
                                    }
                                    if ((rec[9] & 0x40) != (byte) 0x00) {
                                        log += "弹簧电机2反馈开关故障，";
                                        error_ID_closeAisle += "8,";
                                    }
                                    error_ID_closeAisle = error_ID_closeAisle.substring(0, error_ID_closeAisle.length() - 1);
                                    error_ID_closeAisle += ";";
                                    log = log.substring(0, log.length() - 1);
                                    log += "  货道电机实际动作时间（毫秒）:" + ((rec[10] & 0xff) * 256 + (rec[11] & 0xff))
                                            + " 货道电机最大电流（毫安）:" + ((rec[12] & 0xff) * 256 + (rec[13] & 0xff))
                                            + " 货道电机平均电流（毫安）:" + ((rec[14] & 0xff) * 256 + (rec[15] & 0xff));
                                    Log.w("happy", "" + log);
                                    Util.WriteFile(log);
                                }
                                aisleBoard2 = Constant.wait;
                                pushGoods2 = true;
                                aisleZNum2++;
                            }
                        }
                    }
                }
            }
        }
    }

    private void moveHorizontal1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveHorizontal(1,rimZNum1,goods1[currentPackageCount1][currentOutCount1*3+1],goods1[currentPackageCount1][currentOutCount1*3+2]);
            Log.w("happy", "发送左柜水平移位指令");Util.WriteFile("发送左柜水平移位指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜水平移位反馈："+ str1);Util.WriteFile("左柜水平移位反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            rimBoard1 = Constant.queryMoveHorizontal;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if(packageCount2 <= 0 || Error2){
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板水平移位指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("左边柜板水平移位指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                }else{
                    rimBoard1 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    rimZNum1++;
                    finshDone1 = true;
                    if(finshDone2){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error1 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板水平移位指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("左边柜板水平移位指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveHorizontal1(){
        mMotorControl.query((byte)0x02,(byte)0xC0,rimZNum1);
        Log.w("happy", "发送左柜水平移位查询");Util.WriteFile("发送左柜水平移位查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜水平移位查询反馈："+ str1);Util.WriteFile("左柜水平移位查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x46) != (byte)0x00){//水平移动错位只判断停机故障字
                            String error_ID_X = "";
                            String log = "左柜水平移位检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "X轴电机过流，";
                                error_ID_X += "13,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "X轴电机断路，";
                                error_ID_X += "15,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "X轴电机超时，";
                                error_ID_X += "17,";
                            }
                            error_ID_X = error_ID_X.substring(0,error_ID_X.length()-1);
                            log += "停止出货。";
                            log += "  X轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" X轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" X轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount2 <= 0 || Error2){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_X);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左边柜板水平移位查询反馈X轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("左边柜板水平移位查询反馈X轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard1 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                rimZNum1++;
                                finshDone1 = true;
                                if(finshDone2){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error1 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_X);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左边柜板水平移位查询反馈X轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("左边柜板水平移位查询反馈X轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimBoard1 = Constant.wait;
                            rimZNum1++;
                            if(pushGoods1){
                                currentOutCount1 = currentOutCount1 + 1;
                                if(currentOutCount1 == 3 || goods1[currentPackageCount1][currentOutCount1*3] == 0){
                                    MachineState queryMachineState = mDao.queryMachineState();
                                    Position p1 = pDao.queryPosition(goods1[currentPackageCount1][(currentOutCount1-1)*3],1);
                                    if(queryMachineState.getLeftFlootPosition((p1.getPosition1()/16 - 1)) > queryMachineState.getLeftOutPosition()){
                                        rimBoard1 = Constant.moveFloorOutRevise;
                                    }else{
                                        rimBoard1 = Constant.moveFloorOut;
                                    }
                                    currentOutCount1 = 0;
                                }else{
                                    if(((goods1[currentPackageCount1][(currentOutCount1-1)*3]-1)/10) == ((goods1[currentPackageCount1][currentOutCount1*3]-1)/10)){
                                        aisleBoard1 = Constant.pushGoods;
                                    }else{
                                        rimBoard1 = Constant.moveFloor;
                                    }
                                }
                            }else{
                                rimBoard1 = Constant.moveHorizontal;
                            }
                        }
                    }
                }
            }
        }
    }

    private void moveHorizontal2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveHorizontal(2,rimZNum2,goods2[currentPackageCount2][currentOutCount2*3+1],goods2[currentPackageCount2][currentOutCount2*3+2]);
            Log.w("happy", "发送右柜水平移位指令");Util.WriteFile("发送右柜水平移位指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜水平移位反馈："+ str1);Util.WriteFile("右柜水平移位反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum2++;
                            rimBoard2 = Constant.queryMoveHorizontal;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if (packageCount1 <= 0 || Error1) {
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板水平移位指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("右边柜板水平移位指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                } else{
                    rimBoard2 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    rimZNum2++;
                    finshDone2 = true;
                    if(finshDone1){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error2 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板水平移位指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("右边柜板水平移位指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveHorizontal2(){
        mMotorControl.query((byte)0x02,(byte)0xC1,rimZNum2);
        Log.w("happy", "发送右柜水平移位查询");Util.WriteFile("发送右柜水平移位查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜水平移位查询反馈："+ str1);Util.WriteFile("右柜水平移位查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x46) != (byte)0x00){//水平移动错位只判断停机故障字
                            String error_ID_X = "";
                            String log = "右柜水平移位检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "X轴电机过流，";
                                error_ID_X += "14,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "X轴电机断路，";
                                error_ID_X += "16,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "X轴电机超时，";
                                error_ID_X += "18,";
                            }
                            error_ID_X = error_ID_X.substring(0,error_ID_X.length()-1);
                            log += "停止出货。";
                            log += "  X轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" X轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" X轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount1 <= 0 || Error1){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_X);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右边柜板水平移位查询反馈X轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("右边柜板水平移位查询反馈X轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard2 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                rimZNum2++;
                                finshDone2 = true;
                                if(finshDone1){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error2 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_X);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右边柜板水平移位查询反馈X轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("右边柜板水平移位查询反馈X轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimBoard2 = Constant.wait;
                            rimZNum2++;
                            if(pushGoods2){
                                currentOutCount2 = currentOutCount2 + 1;
                                if(currentOutCount2 == 3 || goods2[currentPackageCount2][currentOutCount2*3] == 0){
                                    MachineState queryMachineState = mDao.queryMachineState();
                                    Position p2 = pDao.queryPosition(goods2[currentPackageCount2][(currentOutCount2-1)*3],2);
                                    if(queryMachineState.getRightFlootPosition((p2.getPosition1()/16 - 1)) > queryMachineState.getRightOutPosition()){
                                        rimBoard2 = Constant.moveFloorOutRevise;
                                    }else{
                                        rimBoard2 = Constant.moveFloorOut;
                                    }
                                    currentOutCount2 = 0;
                                }else{
                                    if(((goods2[currentPackageCount2][(currentOutCount2-1)*3]-1)/10) == ((goods2[currentPackageCount2][currentOutCount2*3]-1)/10)){
                                        aisleBoard2 = Constant.pushGoods;
                                    }else{
                                        rimBoard2 = Constant.moveFloor;
                                    }
                                }
                            }else{
                                rimBoard2 = Constant.moveHorizontal;
                            }
                        }
                    }
                }
            }
        }
    }

    private void moveFloorOutRevise1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveFloorOutRevise(1,rimZNum1,moveFloorOutRevise);
            Log.w("happy", "发送左柜移层到出货口(校正)指令");Util.WriteFile("发送左柜移层到出货口(校正)指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜移层到出货口(校正)反馈："+ str1);Util.WriteFile("左柜移层到出货口(校正)反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            rimBoard1 = Constant.queryMoveFloorOutRevise;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if(packageCount2 <= 0 || Error2){
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板移层到出货口(校正)指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("左边柜板移层到出货口(校正)指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                }else{
                    rimBoard1 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    rimZNum1++;
                    finshDone1 = true;
                    if(finshDone2){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error1 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板移层到出货口(校正)指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("左边柜板移层到出货口(校正)指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveFloorOutRevise1(){
        mMotorControl.query((byte)0x01,(byte)0xC0,rimZNum1);
        Log.w("happy", "发送左柜移层到出货口(校正)查询");Util.WriteFile("发送左柜移层到出货口(校正)查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜移层到出货口(校正)查询反馈："+ str1);Util.WriteFile("左柜移层到出货口(校正)查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        if((rec[9]&0x7E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位,移层不检查Bit7=1，Y轴出货门定位开关故障，未使用定位开关，新加升降台上止点开关即时状态（0=开，1=合）不能触发上止点，触发为合
                            String error_ID_Y = "";
                            String log = "左柜移层到出货口(校正)检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "Y轴电机过流，";
                                error_ID_Y += "1,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "Y轴电机断路，";
                                error_ID_Y += "3,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "Y轴上止点开关故障，";
                                error_ID_Y += "5,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "Y轴下止点开关故障，";
                                error_ID_Y += "7,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "Y轴电机超时，";
                                error_ID_Y += "9,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "Y轴码盘故障，";
                                error_ID_Y += "11,";
                            }
                            if((rec[9]&0x80) != (byte)0x00){
                                log += "Y轴出货门定位开关故障，";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "Y轴触发上止点开关，";
                                error_ID_Y += "26,";
                            }
                            error_ID_Y = error_ID_Y.substring(0,error_ID_Y.length()-1);
                            log += "停止出货。";
                            log += "  Y轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" Y轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" Y轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff))
                                    +" Y轴电机当前实际位置（自零点起齿位数）:"+ ((rec[16]&0xff) * 256 + (rec[17]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount2 <= 0 || Error2){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左边移层到出货口(校正)查询反馈Y轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("左边移层到出货口(校正)查询反馈Y轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard1 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                rimZNum1++;
                                finshDone1 = true;
                                if(finshDone2){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error1 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左边移层到出货口(校正)查询反馈Y轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("左边移层到出货口(校正)查询反馈Y轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimZNum1++;
                            rimBoard1 = Constant.moveFloorOut;
                        }
                    }
                }
            }
        }
    }

    private void moveFloorOutRevise2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveFloorOutRevise(2,rimZNum2,moveFloorOutRevise);
            Log.w("happy", "发送右柜移层到出货口(校正)指令");Util.WriteFile("发送右柜移层到出货口(校正)指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜移层到出货口(校正)反馈："+ str1);Util.WriteFile("右柜移层到出货口(校正)反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimZNum2++;
                            rimBoard2 = Constant.queryMoveFloorOutRevise;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if (packageCount1 <= 0 || Error1) {
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板移层到出货口(校正)指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("右边柜板移层到出货口(校正)指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                } else{
                    rimBoard2 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    rimZNum2++;
                    finshDone2 = true;
                    if(finshDone1){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error2 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板移层到出货口(校正)指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("右边柜板移层到出货口(校正)指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveFloorOutRevise2(){
        mMotorControl.query((byte)0x01,(byte)0xC1,rimZNum2);
        Log.w("happy", "发送右柜移层到出货口(校正)查询");Util.WriteFile("发送右柜移层到出货口(校正)查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜移层到出货口(校正)查询反馈："+ str1);Util.WriteFile("右柜移层到出货口(校正)查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        if((rec[9]&0x7E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位,移层不检查Bit7=1，Y轴出货门定位开关故障，未使用定位开关，新加升降台上止点开关即时状态（0=开，1=合）不能触发上止点，触发为合
                            String error_ID_Y = "";
                            String log = "右柜移层到出货口(校正)检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "Y轴电机过流，";
                                error_ID_Y += "2,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "Y轴电机断路，";
                                error_ID_Y += "4,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "Y轴上止点开关故障，";
                                error_ID_Y += "6,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "Y轴下止点开关故障，";
                                error_ID_Y += "8,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "Y轴电机超时，";
                                error_ID_Y += "10,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "Y轴码盘故障，";
                                error_ID_Y += "12,";
                            }
                            if((rec[9]&0x80) != (byte)0x00){
                                log += "Y轴出货门定位开关故障，";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "Y轴触发上止点开关，";
                                error_ID_Y += "27,";
                            }
                            error_ID_Y = error_ID_Y.substring(0,error_ID_Y.length()-1);
                            log += "停止出货。";
                            log += "  Y轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" Y轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" Y轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff))
                                    +" Y轴电机当前实际位置（自零点起齿位数）:"+ ((rec[16]&0xff) * 256 + (rec[17]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount1 <= 0 || Error1){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右边柜移层到出货口(校正)查询反馈Y轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("右边柜移层到出货口(校正)查询反馈Y轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard2 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                rimZNum2++;
                                finshDone2 = true;
                                if(finshDone1){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error2 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右边柜移层到出货口(校正)查询反馈Y轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("右边柜移层到出货口(校正)查询反馈Y轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimZNum2++;
                            rimBoard2 = Constant.moveFloorOut;
                        }
                    }
                }
            }
        }
    }

    private void moveFloorOut1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveFloorOut(1,rimZNum1);
            Log.w("happy", "发送左柜移层到出货口指令");Util.WriteFile("发送左柜移层到出货口指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜移层到出货口反馈："+ str1);Util.WriteFile("左柜移层到出货口反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            rimBoard1 = Constant.queryMoveFloorOut;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if(packageCount2 <= 0 || Error2){
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板移层到出货口指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("左边柜板移层到出货口指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                }else{
                    rimBoard1 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    rimZNum1++;
                    finshDone1 = true;
                    if(finshDone2){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error1 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板移层到出货口指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("左边柜板移层到出货口指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveFloorOut1(){
        mMotorControl.query((byte)0x01,(byte)0xC0,rimZNum1);
        Log.w("happy", "发送左柜移层到出货口查询");Util.WriteFile("发送左柜移层到出货口查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜移层到出货口查询反馈："+ str1);Util.WriteFile("左柜移层到出货口查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        if((rec[9]&0x7E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位,移层不检查Bit7=1，Y轴出货门定位开关故障，未使用定位开关，新加升降台上止点开关即时状态（0=开，1=合）不能触发上止点，触发为合
                            String error_ID_Y = "";
                            String log = "左柜移层到出货口检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "Y轴电机过流，";
                                error_ID_Y += "1,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "Y轴电机断路，";
                                error_ID_Y += "3,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "Y轴上止点开关故障，";
                                error_ID_Y += "5,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "Y轴下止点开关故障，";
                                error_ID_Y += "7,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "Y轴电机超时，";
                                error_ID_Y += "9,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "Y轴码盘故障，";
                                error_ID_Y += "11,";
                            }
                            if((rec[9]&0x80) != (byte)0x00){
                                log += "Y轴出货门定位开关故障，";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "Y轴触发上止点开关，";
                                error_ID_Y += "26,";
                            }
                            error_ID_Y = error_ID_Y.substring(0,error_ID_Y.length()-1);
                            log += "停止出货。";
                            log += "  Y轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" Y轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" Y轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff))
                                    +" Y轴电机当前实际位置（自零点起齿位数）:"+ ((rec[16]&0xff) * 256 + (rec[17]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount2 <= 0 || Error2){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左边移层到出货口查询反馈Y轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("左边移层到出货口查询反馈Y轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard1 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                rimZNum1++;
                                finshDone1 = true;
                                if(finshDone2){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error1 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左边移层到出货口查询反馈Y轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("左边移层到出货口查询反馈Y轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimZNum1++;
                            if(closeGetGoodsDoor){
                                rimBoard1 = Constant.openOutGoodsDoor;
                            }
                        }
                    }
                }
            }
        }
    }

    private void moveFloorOut2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveFloorOut(2,rimZNum2);
            Log.w("happy", "发送右柜移层到出货口指令");Util.WriteFile("发送右柜移层到出货口指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜移层到出货口反馈："+ str1);Util.WriteFile("右柜移层到出货口反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimZNum2++;
                            rimBoard2 = Constant.queryMoveFloorOut;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if (packageCount1 <= 0 || Error1) {
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板移层到出货口指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("右边柜板移层到出货口指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                } else{
                    rimBoard2 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    rimZNum2++;
                    finshDone2 = true;
                    if(finshDone1){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error2 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板移层到出货口指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("右边柜板移层到出货口指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveFloorOut2(){
        mMotorControl.query((byte)0x01,(byte)0xC1,rimZNum2);
        Log.w("happy", "发送右柜移层到出货口查询");Util.WriteFile("发送右柜移层到出货口查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜移层到出货口查询反馈："+ str1);Util.WriteFile("右柜移层到出货口查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        if((rec[9]&0x7E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位,移层不检查Bit7=1，Y轴出货门定位开关故障，未使用定位开关，新加升降台上止点开关即时状态（0=开，1=合）不能触发上止点，触发为合
                            String error_ID_Y = "";
                            String log = "右柜移层到出货口检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "Y轴电机过流，";
                                error_ID_Y += "2,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "Y轴电机断路，";
                                error_ID_Y += "4,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "Y轴上止点开关故障，";
                                error_ID_Y += "6,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "Y轴下止点开关故障，";
                                error_ID_Y += "8,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "Y轴电机超时，";
                                error_ID_Y += "10,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "Y轴码盘故障，";
                                error_ID_Y += "12,";
                            }
                            if((rec[9]&0x80) != (byte)0x00){
                                log += "Y轴出货门定位开关故障，";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "Y轴触发上止点开关，";
                                error_ID_Y += "27,";
                            }
                            error_ID_Y = error_ID_Y.substring(0,error_ID_Y.length()-1);
                            log += "停止出货。";
                            log += "  Y轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" Y轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" Y轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff))
                                    +" Y轴电机当前实际位置（自零点起齿位数）:"+ ((rec[16]&0xff) * 256 + (rec[17]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount1 <= 0 || Error1){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右边柜移层到出货口查询反馈Y轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("右边柜移层到出货口查询反馈Y轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard2 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                rimZNum2++;
                                finshDone2 = true;
                                if(finshDone1){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error2 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右边柜移层到出货口查询反馈Y轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("右边柜移层到出货口查询反馈Y轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimZNum2++;
                            if(closeGetGoodsDoor){
                                rimBoard2 = Constant.openOutGoodsDoor;
                            }
                        }
                    }
                }
            }
        }
    }

    private void openOutGoodsDoor1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.openOutGoodsDoor(1,rimZNum1);
            Log.w("happy", "发送左柜开出货门指令");Util.WriteFile("发送左柜开出货门指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜开出货门反馈："+ str1);Util.WriteFile("左柜开出货门反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            closeOutGoodsDoor1 = false;
                            rimBoard1 = Constant.queryOpenOutGoodsDoor;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if(packageCount2 <= 0 || Error2){
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板开出货门指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("左边柜板开出货门指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                }else{
                    rimBoard1 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    rimZNum1++;
                    finshDone1 = true;
                    if(finshDone2){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error1 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板开出货门指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("左边柜板开出货门指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryOpenOutGoodsDoor1(){
        mMotorControl.query((byte)0x04,(byte)0xC0,rimZNum1);
        Log.w("happy", "发送左柜开出货门查询");Util.WriteFile("发送左柜开出货门查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜开出货门查询反馈："+ str1);Util.WriteFile("左柜开出货门查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x3E) != (byte)0x00){//检查停机故障字
                            String error_ID_E = "";
                            String log = "左柜开出货门检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "出货门电机过流，";
                                error_ID_E += "1,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "出货门电机断路，";
                                error_ID_E += "3,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "出货门前止点开关故障，";
                                error_ID_E += "5,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "出货门后止点开关故障，";
                                error_ID_E += "7,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "开出货门超时，";
                                error_ID_E += "9,";
                            }
                            error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
                            log += "停止出货。";
                            log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            queryLastedTransaction.setComplete(1);
                            queryLastedTransaction.setError(1);
                            tDao.updateTransaction(queryLastedTransaction);
                            OutGoodsThreadFlag = false;
                            rimBoard1 = Constant.wait;
                            rimBoard2 = Constant.wait;
                            aisleBoard1 = Constant.wait;
                            aisleBoard2 = Constant.wait;
                            midBoard = Constant.wait;
                            mTimer.cancel();
                            mTimerOff.cancel();
                            VMMainThreadFlag = true;
                            mQuery1Flag = true;
                            mQuery2Flag = true;
                            mQuery0Flag = true;
                            mUpdataDatabaseFlag = true;
                            SystemClock.sleep(5);
                            Intent intent = new Intent();
                            intent.setAction("njust_outgoods_complete");
                            intent.putExtra("transaction_order_number", current_transaction_order_number);
                            intent.putExtra("outgoods_status", "fail");
                            intent.putExtra("error_type", "stopAllCounter");
                            intent.putExtra("error_ID", error_ID_E);
                            context.sendBroadcast(intent);
                            Log.w("happy", "左柜开出货门查询反馈故障，停止出货，广播上报");
                            Util.WriteFile("左柜开出货门查询反馈故障，停止出货，广播上报");
//                        }else if((rec[9]&0x60) != (byte)0x00){
//                            outGoodsDoorErrorCount1++;
//                            if(outGoodsDoorErrorCount1 >= 3){
//                                outGoodsDoorErrorCount1 = 0;
//                                String log = "左柜开出货门3次尝试失败：";
//                                String error_ID_E = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    log += "开出货门超时，";
//                                    error_ID_E += "9,";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    log += "出货门半开，";
//                                    error_ID_E += "11,";
//                                }
//                                error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
//                                log += "停止出货。";
//                                log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
//                                        +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
//                                        +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
//                                Log.w("happy", ""+log);
//                                Util.WriteFile(log);
//                                queryLastedTransaction.setComplete(1);
//                                queryLastedTransaction.setError(1);
//                                tDao.updateTransaction(queryLastedTransaction);
//                                OutGoodsThreadFlag = false;
//                                rimBoard1 = Constant.wait;
//                                rimBoard2 = Constant.wait;
//                                aisleBoard1 = Constant.wait;
//                                aisleBoard2 = Constant.wait;
//                                midBoard = Constant.wait;
//                                mTimer.cancel();
//                                mTimerOff.cancel();
//                                VMMainThreadFlag = true;
//                                mQuery1Flag = true;
//                                mQuery2Flag = true;
//                                mQuery0Flag = true;
//                                mUpdataDatabaseFlag = true;
//                                SystemClock.sleep(5);
//                                Intent intent = new Intent();
//                                intent.setAction("njust_outgoods_complete");
//                                intent.putExtra("transaction_order_number", current_transaction_order_number);
//                                intent.putExtra("outgoods_status", "fail");
//                                intent.putExtra("error_type", "stopAllCounter");
//                                intent.putExtra("error_ID", error_ID_E);
//                                context.sendBroadcast(intent);
//                            }else{
//                                String a = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    a += "开出货门超时，";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    a += "出货门半开，";
//                                }
//                                a = a.substring(0,a.length()-1);
//                                Util.WriteFile("左柜开出货门出现故障："+a+"  尝试再次打开出货门");
//                                SystemClock.sleep(delay);
//                                rimBoard1 = Constant.openOutGoodsDoor;
//                                rimZNum1++;
//                            }
                        }else{
                            outGoodsDoorErrorCount1 = 0;
                            closeOutGoodsDoor1 = false;
                            rimBoard1 = Constant.moveHorizontalOut;
                            rimZNum1++;
                        }
                    }
                }
            }
        }
    }

    private void openOutGoodsDoor2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.openOutGoodsDoor(2,rimZNum2);
            Log.w("happy", "发送右柜开出货门指令");Util.WriteFile("发送右柜开出货门指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜开出货门反馈："+ str1);Util.WriteFile("右柜开出货门反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum2++;
                            closeOutGoodsDoor2 = false;
                            rimBoard2 = Constant.queryOpenOutGoodsDoor;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if (packageCount1 <= 0 || Error1) {
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板开出货门指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("右边柜板开出货门指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                } else{
                    rimBoard2 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    rimZNum2++;
                    finshDone2 = true;
                    if(finshDone1){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error2 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板开出货门指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("右边柜板开出货门指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryOpenOutGoodsDoor2(){
        mMotorControl.query((byte)0x04,(byte)0xC1,rimZNum2);
        Log.w("happy", "发送右柜开出货门查询");Util.WriteFile("发送右柜开出货门查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜开出货门查询反馈："+ str1);Util.WriteFile("右柜开出货门查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x3E) != (byte)0x00){//检查停机故障字
                            String error_ID_E = "";
                            String log = "右柜开出货门检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "出货门电机过流，";
                                error_ID_E += "2,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "出货门电机断路，";
                                error_ID_E += "4,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "出货门前止点开关故障，";
                                error_ID_E += "6,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "出货门后止点开关故障，";
                                error_ID_E += "8,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "开出货门超时，";
                                error_ID_E += "10,";
                            }
                            error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
                            log += "停止出货。";
                            log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            queryLastedTransaction.setComplete(1);
                            queryLastedTransaction.setError(1);
                            tDao.updateTransaction(queryLastedTransaction);
                            OutGoodsThreadFlag = false;
                            rimBoard1 = Constant.wait;
                            rimBoard2 = Constant.wait;
                            aisleBoard1 = Constant.wait;
                            aisleBoard2 = Constant.wait;
                            midBoard = Constant.wait;
                            mTimer.cancel();
                            mTimerOff.cancel();
                            VMMainThreadFlag = true;
                            mQuery1Flag = true;
                            mQuery2Flag = true;
                            mQuery0Flag = true;
                            mUpdataDatabaseFlag = true;
                            SystemClock.sleep(5);
                            Intent intent = new Intent();
                            intent.setAction("njust_outgoods_complete");
                            intent.putExtra("transaction_order_number", current_transaction_order_number);
                            intent.putExtra("outgoods_status", "fail");
                            intent.putExtra("error_type", "stopAllCounter");
                            intent.putExtra("error_ID", error_ID_E);
                            context.sendBroadcast(intent);
                            Log.w("happy", "右柜开出货门查询反馈故障，停止出货，广播上报");
                            Util.WriteFile("右柜开出货门查询反馈故障，停止出货，广播上报");
//                        }else if((rec[9]&0x60) != (byte)0x00){
//                            outGoodsDoorErrorCount2++;
//                            if(outGoodsDoorErrorCount2 >= 3){
//                                outGoodsDoorErrorCount2 = 0;
//                                String log = "右柜开出货门3次尝试失败：";
//                                String error_ID_E = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    log += "开出货门超时，";
//                                    error_ID_E += "10,";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    log += "出货门半开，";
//                                    error_ID_E += "12,";
//                                }
//                                error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
//                                log += "停止出货。";
//                                log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
//                                        +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
//                                        +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
//                                Log.w("happy", ""+log);
//                                Util.WriteFile(log);
//                                queryLastedTransaction.setComplete(1);
//                                queryLastedTransaction.setError(1);
//                                tDao.updateTransaction(queryLastedTransaction);
//                                OutGoodsThreadFlag = false;
//                                rimBoard1 = Constant.wait;
//                                rimBoard2 = Constant.wait;
//                                aisleBoard1 = Constant.wait;
//                                aisleBoard2 = Constant.wait;
//                                midBoard = Constant.wait;
//                                mTimer.cancel();
//                                mTimerOff.cancel();
//                                VMMainThreadFlag = true;
//                                mQuery1Flag = true;
//                                mQuery2Flag = true;
//                                mQuery0Flag = true;
//                                mUpdataDatabaseFlag = true;
//                                SystemClock.sleep(5);
//                                Intent intent = new Intent();
//                                intent.setAction("njust_outgoods_complete");
//                                intent.putExtra("transaction_order_number", current_transaction_order_number);
//                                intent.putExtra("outgoods_status", "fail");
//                                intent.putExtra("error_type", "stopAllCounter");
//                                intent.putExtra("error_ID", error_ID_E);
//                                context.sendBroadcast(intent);
//                            }else{
//                                String a = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    a += "开出货门超时，";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    a += "出货门半开，";
//                                }
//                                a = a.substring(0,a.length()-1);
//                                Util.WriteFile("右柜开出货门出现故障："+a+"  尝试再次打开出货门");
//                                SystemClock.sleep(delay);
//                                rimBoard2 = Constant.openOutGoodsDoor;
//                                rimZNum2++;
//                            }
                        }else{
                            outGoodsDoorErrorCount2 = 0;
                            closeOutGoodsDoor2 = false;
                            rimBoard2 = Constant.moveHorizontalOut;
                            rimZNum2++;
                        }
                    }
                }
            }
        }
    }

    private void moveHorizontalOut1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveHorizontalOut(1,rimZNum1,1,moveTimeOut);
            Log.w("happy", "发送左柜X轴出货指令");Util.WriteFile("发送左柜X轴出货指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜X轴出货反馈："+ str1);Util.WriteFile("左柜X轴出货反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            rimBoard1 = Constant.queryMoveHorizontalOut;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if(packageCount2 <= 0 || Error2){
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    boolean flagE = true;
                    int timesE = 0;
                    while (flagE){//如果此刻故障停机需要先关闭出货门
                        mMotorControl.closeOutGoodsDoor(1,rimZNum1);
                        SystemClock.sleep(delay);
                        byte[] recE = serialPort485.receiveData();
                        if (recE != null && recE.length >= 5) {
                            if(recE[0] == (byte)0xE2 && recE[1] == recE.length && recE[2] == 0x00 && recE[4] == (byte)0x0F && recE[recE.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(recE[6] == (byte)0x63 && recE[3] == (byte)0xC0 && recE[7] == (byte)0x5A){
                                    if(recE[16] == (byte)0x00 || recE[16] == (byte)0x01 || recE[16] == (byte)0x03){
                                        flagE = false;
                                        rimZNum1++;
                                    }
                                }
                            }
                        }
                        timesE = timesE + 1;
                        if(timesE == 6){
                            flagE = false;
                        }
                    }
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板移动x轴推货到中柜指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("左边柜板移动x轴推货到中柜指令通信故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                }else{
                    boolean flagE = true;
                    int timesE = 0;
                    while (flagE){//如果此刻故障停机需要先关闭出货门
                        mMotorControl.closeOutGoodsDoor(1,rimZNum1);
                        SystemClock.sleep(delay);
                        byte[] recE = serialPort485.receiveData();
                        if (recE != null && recE.length >= 5) {
                            if(recE[0] == (byte)0xE2 && recE[1] == recE.length && recE[2] == 0x00 && recE[4] == (byte)0x0F && recE[recE.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(recE[6] == (byte)0x63 && recE[3] == (byte)0xC0 && recE[7] == (byte)0x5A){
                                    if(recE[16] == (byte)0x00 || recE[16] == (byte)0x01 || recE[16] == (byte)0x03){
                                        flagE = false;
                                        rimZNum1++;
                                    }
                                }
                            }
                        }
                        timesE = timesE + 1;
                        if(timesE == 6){
                            flagE = false;
                        }
                    }
                    rimBoard1 =Constant.wait;
                    aisleBoard1 = Constant.wait;
                    rimZNum1++;
                    finshDone1 = true;
                    if(finshDone2){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error1 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "left");
                    intent.putExtra("error_ID", "21");
                    context.sendBroadcast(intent);
                    Log.w("happy", "左边柜板移动x轴推货到中柜指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("左边柜板移动x轴推货到中柜指令通信故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveHorizontalOut1(){
        mMotorControl.query((byte)0x02,(byte)0xC0,rimZNum1);
        Log.w("happy", "发送左柜X轴出货查询");Util.WriteFile("发送左柜X轴出货查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜X轴出货查询反馈："+ str1);Util.WriteFile("左柜X轴出货查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x38) != (byte)0x00){//X轴出货故障点只做记录
                            String log = "左柜X轴出货记录：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "X轴出货光栅未检测到货物，";
                                error_ID_justRecord += "1,";
                                needMoveHorizontalOut1Again++;
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "X轴出货光栅货物遮挡超时，";
                                error_ID_justRecord += "3,";
                                needMoveHorizontalOut1Again++;
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "X轴出货光栅故障，";
                                error_ID_justRecord += "5,";
                            }
                            log = log.substring(0,log.length()-1);
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                        }
                        if(needMoveHorizontalOut1Again == 1 || needMoveHorizontalOut1Again == 2){
                            rimBoard1 = Constant.moveHorizontalOut;
                            needMoveHorizontalOut1Again = 3;
                            rimZNum1++;
                        }else{
                            rimBoard1 = Constant.closeOutGoodsDoor;
                            rimZNum1++;
                        }
                    }
                }
            }
        }
    }

    private void moveHorizontalOut2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveHorizontalOut(2,rimZNum2,1,moveTimeOut);
            Log.w("happy", "发送右柜X轴出货指令");Util.WriteFile("发送右柜X轴出货指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜X轴出货反馈："+ str1);Util.WriteFile("右柜X轴出货反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum2++;
                            rimBoard2 = Constant.queryMoveHorizontalOut;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                if (packageCount1 <= 0 || Error1) {
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(1);
                    tDao.updateTransaction(queryLastedTransaction);
                    OutGoodsThreadFlag = false;
                    boolean flagE = true;
                    int timesE = 0;
                    while (flagE){//如果此刻故障停机需要先关闭出货门
                        mMotorControl.closeOutGoodsDoor(2,rimZNum2);
                        SystemClock.sleep(delay);
                        byte[] recE = serialPort485.receiveData();
                        if (recE != null && recE.length >= 5) {
                            if(recE[0] == (byte)0xE2 && recE[1] == recE.length && recE[2] == 0x00 && recE[4] == (byte)0x0F && recE[recE.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(recE[6] == (byte)0x63 && recE[3] == (byte)0xC1 && recE[7] == (byte)0x5A){
                                    if(recE[16] == (byte)0x00 || recE[16] == (byte)0x01 || recE[16] == (byte)0x03){
                                        flagE = false;
                                        rimZNum2++;
                                    }
                                }
                            }
                        }
                        timesE = timesE + 1;
                        if(timesE == 6){
                            flagE = false;
                        }
                    }
                    rimBoard1 = Constant.wait;
                    rimBoard2 = Constant.wait;
                    aisleBoard1 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    midBoard = Constant.wait;
                    mTimer.cancel();
                    mTimerOff.cancel();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    SystemClock.sleep(5);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板移动x轴推货到中柜指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                    Util.WriteFile("右边柜板移动x轴推货到中柜指令通信故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                } else{
                    boolean flagE = true;
                    int timesE = 0;
                    while (flagE){//如果此刻故障停机需要先关闭出货门
                        mMotorControl.closeOutGoodsDoor(2,rimZNum2);
                        SystemClock.sleep(delay);
                        byte[] recE = serialPort485.receiveData();
                        if (recE != null && recE.length >= 5) {
                            if(recE[0] == (byte)0xE2 && recE[1] == recE.length && recE[2] == 0x00 && recE[4] == (byte)0x0F && recE[recE.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(recE[6] == (byte)0x63 && recE[3] == (byte)0xC1 && recE[7] == (byte)0x5A){
                                    if(recE[16] == (byte)0x00 || recE[16] == (byte)0x01 || recE[16] == (byte)0x03){
                                        flagE = false;
                                        rimZNum2++;
                                    }
                                }
                            }
                        }
                        timesE = timesE + 1;
                        if(timesE == 6){
                            flagE = false;
                        }
                    }
                    rimBoard2 = Constant.wait;
                    aisleBoard2 = Constant.wait;
                    rimZNum2++;
                    finshDone2 = true;
                    if(finshDone1){
                        midBoard = Constant.openGetGoodsDoor;
                        complete = true;
                    }
                    Error2 = true;
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "fail");
                    intent.putExtra("error_type", "stopOneCounter");
                    intent.putExtra("error_counter", "right");
                    intent.putExtra("error_ID", "22");
                    context.sendBroadcast(intent);
                    Log.w("happy", "右边柜板移动x轴推货到中柜指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                    Util.WriteFile("右边柜板移动x轴推货到中柜指令通信故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                }
            }
        }
    }

    private void queryMoveHorizontalOut2(){
        mMotorControl.query((byte)0x02,(byte)0xC1,rimZNum2);
        Log.w("happy", "发送右柜X轴出货查询");Util.WriteFile("发送右柜X轴出货查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜X轴出货查询反馈："+ str1);Util.WriteFile("右柜X轴出货查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x38) != (byte)0x00){//X轴出货故障点只做记录
                            String log = "右柜X轴出货记录：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "X轴出货光栅未检测到货物，";
                                error_ID_justRecord += "2,";
                                needMoveHorizontalOut2Again++;
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "X轴出货光栅货物遮挡超时，";
                                error_ID_justRecord += "4,";
                                needMoveHorizontalOut2Again++;
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "X轴出货光栅故障，";
                                error_ID_justRecord += "6,";
                            }
                            log = log.substring(0,log.length()-1);
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                        }
                        if(needMoveHorizontalOut2Again == 1 || needMoveHorizontalOut2Again == 2){
                            rimBoard2 = Constant.moveHorizontalOut;
                            needMoveHorizontalOut2Again = 3;
                            rimZNum2++;
                        }else{
                            rimBoard2 = Constant.closeOutGoodsDoor;
                            rimZNum2++;
                        }
                    }
                }
            }
        }
    }

    private void closeOutGoodsDoor1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.closeOutGoodsDoor(1,rimZNum1);
            Log.w("happy", "发送左柜关出货门指令");Util.WriteFile("发送左柜关出货门指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜关出货门反馈："+ str1);Util.WriteFile("左柜关出货门反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            rimBoard1 = Constant.queryCloseOutGoodsDoor;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                queryLastedTransaction.setComplete(1);
                queryLastedTransaction.setError(1);
                tDao.updateTransaction(queryLastedTransaction);
                OutGoodsThreadFlag = false;
                rimBoard1 = Constant.wait;
                rimBoard2 = Constant.wait;
                aisleBoard1 = Constant.wait;
                aisleBoard2 = Constant.wait;
                midBoard = Constant.wait;
                mTimer.cancel();
                mTimerOff.cancel();
                VMMainThreadFlag = true;
                mQuery1Flag = true;
                mQuery2Flag = true;
                mQuery0Flag = true;
                mUpdataDatabaseFlag = true;
                SystemClock.sleep(5);
                Intent intent = new Intent();
                intent.setAction("njust_outgoods_complete");
                intent.putExtra("transaction_order_number", current_transaction_order_number);
                intent.putExtra("outgoods_status", "fail");
                intent.putExtra("error_type", "stopOneCounter");
                intent.putExtra("error_counter", "all");
                intent.putExtra("error_ID", "21");
                context.sendBroadcast(intent);
                Log.w("happy", "左边柜板关闭出货门指令通信故障，停止当次出货，广播上报");
                Util.WriteFile("左边柜板关闭出货门指令通信故障，停止当次出货，广播上报");
            }
        }
    }

    private void queryCloseOutGoodsDoor1(){
        mMotorControl.query((byte)0x05,(byte)0xC0,rimZNum1);
        Log.w("happy", "发送左柜关出货门查询");Util.WriteFile("发送左柜关出货门查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜关出货门查询反馈："+ str1);Util.WriteFile("左柜关出货门查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x3E) != (byte)0x00){//检查停机故障字
                            String error_ID_E = "";
                            String log = "左柜关出货门检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "出货门电机过流，";
                                error_ID_E += "1,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "出货门电机断路，";
                                error_ID_E += "3,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "出货门前止点开关故障，";
                                error_ID_E += "5,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "出货门后止点开关故障，";
                                error_ID_E += "7,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "关出货门超时，";
                                error_ID_E += "9,";
                            }
                            error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
                            log += "停止出货。";
                            log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            queryLastedTransaction.setComplete(1);
                            queryLastedTransaction.setError(1);
                            tDao.updateTransaction(queryLastedTransaction);
                            OutGoodsThreadFlag = false;
                            rimBoard1 = Constant.wait;
                            rimBoard2 = Constant.wait;
                            aisleBoard1 = Constant.wait;
                            aisleBoard2 = Constant.wait;
                            midBoard = Constant.wait;
                            mTimer.cancel();
                            mTimerOff.cancel();
                            VMMainThreadFlag = true;
                            mQuery1Flag = true;
                            mQuery2Flag = true;
                            mQuery0Flag = true;
                            mUpdataDatabaseFlag = true;
                            SystemClock.sleep(5);
                            Intent intent = new Intent();
                            intent.setAction("njust_outgoods_complete");
                            intent.putExtra("transaction_order_number", current_transaction_order_number);
                            intent.putExtra("outgoods_status", "fail");
                            intent.putExtra("error_type", "stopAllCounter");
                            intent.putExtra("error_ID", error_ID_E);
                            context.sendBroadcast(intent);
                            Log.w("happy", "左柜关出货门查询反馈故障，停止出货，广播上报");
                            Util.WriteFile("左柜关出货门查询反馈故障，停止出货，广播上报");
//                        }else if((rec[9]&0x60) != (byte)0x00){
//                            outGoodsDoorErrorCount1++;
//                            if(outGoodsDoorErrorCount1 >= 3){
//                                outGoodsDoorErrorCount1 = 0;
//                                String log = "左柜关出货门3次尝试失败：";
//                                String error_ID_E = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    log += "关出货门超时，";
//                                    error_ID_E += "9,";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    log += "出货门半关，";
//                                    error_ID_E += "11,";
//                                }
//                                error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
//                                log += "停止出货。";
//                                log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
//                                        +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
//                                        +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
//                                Log.w("happy", ""+log);
//                                Util.WriteFile(log);
//                                queryLastedTransaction.setComplete(1);
//                                queryLastedTransaction.setError(1);
//                                tDao.updateTransaction(queryLastedTransaction);
//                                OutGoodsThreadFlag = false;
//                                rimBoard1 = Constant.wait;
//                                rimBoard2 = Constant.wait;
//                                aisleBoard1 = Constant.wait;
//                                aisleBoard2 = Constant.wait;
//                                midBoard = Constant.wait;
//                                mTimer.cancel();
//                                mTimerOff.cancel();
//                                VMMainThreadFlag = true;
//                                mQuery1Flag = true;
//                                mQuery2Flag = true;
//                                mQuery0Flag = true;
//                                mUpdataDatabaseFlag = true;
//                                SystemClock.sleep(5);
//                                Intent intent = new Intent();
//                                intent.setAction("njust_outgoods_complete");
//                                intent.putExtra("transaction_order_number", current_transaction_order_number);
//                                intent.putExtra("outgoods_status", "fail");
//                                intent.putExtra("error_type", "stopAllCounter");
//                                intent.putExtra("error_ID", error_ID_E);
//                                context.sendBroadcast(intent);
//                            }else{
//                                String a = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    a += "关出货门超时，";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    a += "出货门半关，";
//                                }
//                                a = a.substring(0,a.length()-1);
//                                Util.WriteFile("左柜关出货门出现故障："+a+"  尝试再次关闭出货门");
//                                SystemClock.sleep(delay);
//                                rimBoard1 = Constant.closeOutGoodsDoor;
//                                rimZNum1++;
//                            }
                        }else{
                            outGoodsDoorErrorCount1 = 0;
                            rimZNum1++;
                            closeOutGoodsDoor1 = true;
                            currentPackageCount1 = currentPackageCount1 + 1;
                            if(currentPackageCount1 >= packageCount1){
                                finshDone1 = true;
                            }
                            if(finshDone1 && finshDone2){
                                midBoard = Constant.openGetGoodsDoor;
                                complete = true;
                            }
                            rimBoard1 = Constant.homing;
                        }
                    }
                }
            }
        }
    }

    private void closeOutGoodsDoor2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.closeOutGoodsDoor(2,rimZNum2);
            Log.w("happy", "发送右柜关出货门指令");Util.WriteFile("发送右柜关出货门指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜关出货门反馈："+ str1);Util.WriteFile("右柜关出货门反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum2++;
                            rimBoard2 = Constant.queryCloseOutGoodsDoor;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                queryLastedTransaction.setComplete(1);
                queryLastedTransaction.setError(1);
                tDao.updateTransaction(queryLastedTransaction);
                OutGoodsThreadFlag = false;
                rimBoard1 = Constant.wait;
                rimBoard2 = Constant.wait;
                aisleBoard1 = Constant.wait;
                aisleBoard2 = Constant.wait;
                midBoard = Constant.wait;
                mTimer.cancel();
                mTimerOff.cancel();
                VMMainThreadFlag = true;
                mQuery1Flag = true;
                mQuery2Flag = true;
                mQuery0Flag = true;
                mUpdataDatabaseFlag = true;
                SystemClock.sleep(5);
                Intent intent = new Intent();
                intent.setAction("njust_outgoods_complete");
                intent.putExtra("transaction_order_number", current_transaction_order_number);
                intent.putExtra("outgoods_status", "fail");
                intent.putExtra("error_type", "stopOneCounter");
                intent.putExtra("error_counter", "all");
                intent.putExtra("error_ID", "22");
                context.sendBroadcast(intent);
                Log.w("happy", "右边柜板关闭出货门指令通信故障，停止当次出货，广播上报");
                Util.WriteFile("右边柜板关闭出货门指令通信故障，停止当次出货，广播上报");
            }
        }
    }

    private void queryCloseOutGoodsDoor2(){
        mMotorControl.query((byte)0x05,(byte)0xC1,rimZNum2);
        Log.w("happy", "发送右柜关出货门查询");Util.WriteFile("发送右柜关出货门查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜关出货门查询反馈："+ str1);Util.WriteFile("右柜关出货门查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                    if(rec[16] == (byte)0x02){
                        if((rec[9]&0x3E) != (byte)0x00){//检查停机故障字
                            String error_ID_E = "";
                            String log = "右柜关出货门检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "出货门电机过流，";
                                error_ID_E += "2,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "出货门电机断路，";
                                error_ID_E += "4,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "出货门前止点开关故障，";
                                error_ID_E += "6,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "出货门后止点开关故障，";
                                error_ID_E += "8,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "关出货门超时，";
                                error_ID_E += "10,";
                            }
                            error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
                            log += "停止出货。";
                            log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            queryLastedTransaction.setComplete(1);
                            queryLastedTransaction.setError(1);
                            tDao.updateTransaction(queryLastedTransaction);
                            OutGoodsThreadFlag = false;
                            rimBoard1 = Constant.wait;
                            rimBoard2 = Constant.wait;
                            aisleBoard1 = Constant.wait;
                            aisleBoard2 = Constant.wait;
                            midBoard = Constant.wait;
                            mTimer.cancel();
                            mTimerOff.cancel();
                            VMMainThreadFlag = true;
                            mQuery1Flag = true;
                            mQuery2Flag = true;
                            mQuery0Flag = true;
                            mUpdataDatabaseFlag = true;
                            SystemClock.sleep(5);
                            Intent intent = new Intent();
                            intent.setAction("njust_outgoods_complete");
                            intent.putExtra("transaction_order_number", current_transaction_order_number);
                            intent.putExtra("outgoods_status", "fail");
                            intent.putExtra("error_type", "stopAllCounter");
                            intent.putExtra("error_ID", error_ID_E);
                            context.sendBroadcast(intent);
                            Log.w("happy", "右柜关出货门查询反馈故障，停止出货，广播上报");
                            Util.WriteFile("右柜关出货门查询反馈故障，停止出货，广播上报");
//                        }else if((rec[9]&0x60) != (byte)0x00){
//                            outGoodsDoorErrorCount2++;
//                            if(outGoodsDoorErrorCount2 >= 3){
//                                outGoodsDoorErrorCount2 = 0;
//                                String log = "右柜关出货门3次尝试失败：";
//                                String error_ID_E = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    log += "关出货门超时，";
//                                    error_ID_E += "10,";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    log += "出货门半关，";
//                                    error_ID_E += "12,";
//                                }
//                                error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
//                                log += "停止出货。";
//                                log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
//                                        +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
//                                        +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
//                                Log.w("happy", ""+log);
//                                Util.WriteFile(log);
//                                queryLastedTransaction.setComplete(1);
//                                queryLastedTransaction.setError(1);
//                                tDao.updateTransaction(queryLastedTransaction);
//                                OutGoodsThreadFlag = false;
//                                rimBoard1 = Constant.wait;
//                                rimBoard2 = Constant.wait;
//                                aisleBoard1 = Constant.wait;
//                                aisleBoard2 = Constant.wait;
//                                midBoard = Constant.wait;
//                                mTimer.cancel();
//                                mTimerOff.cancel();
//                                VMMainThreadFlag = true;
//                                mQuery1Flag = true;
//                                mQuery2Flag = true;
//                                mQuery0Flag = true;
//                                mUpdataDatabaseFlag = true;
//                                SystemClock.sleep(5);
//                                Intent intent = new Intent();
//                                intent.setAction("njust_outgoods_complete");
//                                intent.putExtra("transaction_order_number", current_transaction_order_number);
//                                intent.putExtra("outgoods_status", "fail");
//                                intent.putExtra("error_type", "stopAllCounter");
//                                intent.putExtra("error_ID", error_ID_E);
//                                context.sendBroadcast(intent);
//                            }else{
//                                String a = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    a += "关出货门超时，";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    a += "出货门半关，";
//                                }
//                                a = a.substring(0,a.length()-1);
//                                Util.WriteFile("右柜关出货门出现故障："+a+"  尝试再次关闭出货门");
//                                SystemClock.sleep(delay);
//                                rimBoard2 = Constant.closeOutGoodsDoor;
//                                rimZNum2++;
//                            }
                        }else{
                            outGoodsDoorErrorCount2 = 0;
                            rimZNum2++;
                            closeOutGoodsDoor2 = true;
                            currentPackageCount2 = currentPackageCount2 + 1;
                            if(currentPackageCount2 >= packageCount2){
                                finshDone2 = true;
                            }
                            if(finshDone1 && finshDone2){
                                midBoard = Constant.openGetGoodsDoor;
                                complete = true;
                            }
                            rimBoard2 = Constant.homing;
                        }
                    }
                }
            }
        }
    }

    private void homing1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.homing(1,rimZNum1);
            Log.w("happy", "发送左柜归位指令");Util.WriteFile("发送左柜归位指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "左柜归位反馈："+ str1);Util.WriteFile("左柜归位反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            rimBoard1 = Constant.queryHoming;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                rimZNum1++;
                rimBoard1 = Constant.queryHoming;
                Log.w("happy", "左边柜板归位指令通信故障，直接进行归位查询");
                Util.WriteFile("左边柜板归位指令通信故障，直接进行归位查询");
            }
        }
    }

    private void queryHoming1(){
        mMotorControl.query((byte)0x06,(byte)0xC0,rimZNum1);
        Log.w("happy", "发送左柜归位查询");Util.WriteFile("发送左柜归位查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "左柜归位查询反馈："+ str1);Util.WriteFile("左柜归位查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        if((rec[9]&0x7E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位,移层不检查Bit7=1，Y轴出货门定位开关故障，未使用定位开关，新加升降台上止点开关即时状态（0=开，1=合）不能触发上止点，触发为合
                            String error_ID_Y = "";
                            String log = "左柜归位检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "Y轴电机过流，";
                                error_ID_Y += "1,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "Y轴电机断路，";
                                error_ID_Y += "3,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "Y轴上止点开关故障，";
                                error_ID_Y += "5,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "Y轴下止点开关故障，";
                                error_ID_Y += "7,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "Y轴电机超时，";
                                error_ID_Y += "9,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "Y轴码盘故障，";
                                error_ID_Y += "11,";
                            }
                            if((rec[9]&0x80) != (byte)0x00){
                                log += "Y轴出货门定位开关故障，";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "Y轴触发上止点开关，";
                                error_ID_Y += "26,";
                            }
                            error_ID_Y = error_ID_Y.substring(0,error_ID_Y.length()-1);
                            log += "停止出货。";
                            log += "  Y轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" Y轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" Y轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff))
                                    +" Y轴电机当前实际位置（自零点起齿位数）:"+ ((rec[16]&0xff) * 256 + (rec[17]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount2 <= 0 || Error2){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左柜归位查询反馈Y轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("左柜归位查询反馈Y轴电机组件故障，本次交易右柜未购物商品，或者右柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard1 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                rimZNum1++;
                                finshDone1 = true;
                                if(finshDone2){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error1 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "left");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "左柜归位查询反馈Y轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("左柜归位查询反馈Y轴电机组件故障，本次交易右柜购物了商品，且右柜无停机故障，停止左柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimZNum1++;
                            if(!finshDone1){
                                rimBoard1 = Constant.moveFloor;
                            }else{
                                rimBoard1 = Constant.wait;
                            }
                        }
                    }
                }
            }
        }
    }

    private void homing2(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.homing(2,rimZNum2);
            Log.w("happy", "发送右柜归位指令");Util.WriteFile("发送右柜归位指令");
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "右柜归位反馈："+ str1);Util.WriteFile("右柜归位反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimZNum2++;
                            rimBoard2 = Constant.queryHoming;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                rimZNum2++;
                rimBoard2 = Constant.queryHoming;
                Log.w("happy", "右边柜板归位指令通信故障，直接进行归位查询");
                Util.WriteFile("右边柜板归位指令通信故障，直接进行归位查询");
            }
        }
    }

    private void queryHoming2(){
        mMotorControl.query((byte)0x06,(byte)0xC1,rimZNum2);
        Log.w("happy", "发送右柜归位查询");Util.WriteFile("发送右柜归位查询");
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "右柜归位查询反馈："+ str1);Util.WriteFile("右柜归位查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        if((rec[9]&0x7E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//暂时不检查（Bit0=0，未执行动作；Bit0=1，已执行动作）位,移层不检查Bit7=1，Y轴出货门定位开关故障，未使用定位开关，新加升降台上止点开关即时状态（0=开，1=合）不能触发上止点，触发为合
                            String error_ID_Y = "";
                            String log = "右柜归位检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "Y轴电机过流，";
                                error_ID_Y += "2,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "Y轴电机断路，";
                                error_ID_Y += "4,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "Y轴上止点开关故障，";
                                error_ID_Y += "6,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "Y轴下止点开关故障，";
                                error_ID_Y += "8,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "Y轴电机超时，";
                                error_ID_Y += "10,";
                            }
                            if((rec[9]&0x40) != (byte)0x00){
                                log += "Y轴码盘故障，";
                                error_ID_Y += "12,";
                            }
                            if((rec[9]&0x80) != (byte)0x00){
                                log += "Y轴出货门定位开关故障，";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "Y轴触发上止点开关，";
                                error_ID_Y += "27,";
                            }
                            error_ID_Y = error_ID_Y.substring(0,error_ID_Y.length()-1);
                            log += "停止出货。";
                            log += "  Y轴电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" Y轴电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" Y轴电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff))
                                    +" Y轴电机当前实际位置（自零点起齿位数）:"+ ((rec[16]&0xff) * 256 + (rec[17]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            if(packageCount1 <= 0 || Error1){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右柜归位查询反馈Y轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                                Util.WriteFile("右柜归位查询反馈Y轴电机组件故障，本次交易左柜未购物商品，或者左柜已经故障停机，停止出货，广播上报");
                            }else{
                                rimBoard2 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                rimZNum2++;
                                finshDone2 = true;
                                if(finshDone1){
                                    midBoard = Constant.openGetGoodsDoor;
                                    complete = true;
                                }
                                Error2 = true;
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopOneCounter");
                                intent.putExtra("error_counter", "right");
                                intent.putExtra("error_ID", error_ID_Y);
                                context.sendBroadcast(intent);
                                Log.w("happy", "右柜归位查询反馈Y轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                                Util.WriteFile("右柜归位查询反馈Y轴电机组件故障，本次交易左柜购物了商品，且左柜无停机故障，停止右柜出货，等待取完货物再次广播上报");
                            }
                        }else{
                            rimZNum2++;
                            if(!finshDone2){
                                rimBoard2 = Constant.moveFloor;
                            }else{
                                rimBoard2 = Constant.wait;
                            }
                        }
                    }
                }
            }
        }
    }

    private void openGetGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.openGetGoodsDoor(midZNum);
            Log.w("happy", "发送开取货门指令");Util.WriteFile("发送开取货门指令");
            SystemClock.sleep(delayGetDoor1);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "开取货门反馈："+ str1);Util.WriteFile("开取货门反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            closeGetGoodsDoor = false;
                            midBoard = Constant.queryOpenGetGoodsDoor;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                queryLastedTransaction.setComplete(1);
                queryLastedTransaction.setError(1);
                tDao.updateTransaction(queryLastedTransaction);
                OutGoodsThreadFlag = false;
                rimBoard1 = Constant.wait;
                rimBoard2 = Constant.wait;
                aisleBoard1 = Constant.wait;
                aisleBoard2 = Constant.wait;
                midBoard = Constant.wait;
                mTimer.cancel();
                mTimerOff.cancel();
                VMMainThreadFlag = true;
                mQuery1Flag = true;
                mQuery2Flag = true;
                mQuery0Flag = true;
                mUpdataDatabaseFlag = true;
                SystemClock.sleep(5);
                Intent intent = new Intent();
                intent.setAction("njust_outgoods_complete");
                intent.putExtra("transaction_order_number", current_transaction_order_number);
                intent.putExtra("outgoods_status", "fail");
                intent.putExtra("error_type", "stopOneCounter");
                intent.putExtra("error_counter", "all");
                intent.putExtra("error_ID", "25");
                context.sendBroadcast(intent);
                Log.w("happy", "中柜开取货门通信故障，停止当次交易，广播上报");
                Util.WriteFile("中柜开取货门通信故障，停止当次交易，广播上报");
            }
        }
    }

    private void queryOpenGetGoodsDoor(){
        mMotorControl.query((byte)0x07,(byte)0xE0,midZNum);
        Log.w("happy", "发送开取货门查询");Util.WriteFile("发送开取货门查询");
        SystemClock.sleep(delayGetDoor1);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "开取货门查询反馈："+ str1);Util.WriteFile("开取货门查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                    if(rec[16] == (byte)0x02){
                        if((rec[8]&0x02) != (byte)0x00){//只需要记录下来的故障
                            error_ID_justRecord += "7,";
                        }
                        if((rec[9]&0x3E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//检查停机故障字
                            String error_ID_E = "";
                            String log = "开取货门检测到错误：";
                            if ((rec[9]&0x01) != (byte)0x00) {
                                log += "已执行动作，";
                            } else {
                                log += "未执行动作，";
                            }
                            if((rec[9]&0x02) != (byte)0x00){
                                log += "取货门电机过流，";
                                error_ID_E += "13,";
                            }
                            if((rec[9]&0x04) != (byte)0x00){
                                log += "取货门电机断路，";
                                error_ID_E += "14,";
                            }
                            if((rec[9]&0x08) != (byte)0x00){
                                log += "取货门上止点开关故障，";
                                error_ID_E += "15,";
                            }
                            if((rec[9]&0x10) != (byte)0x00){
                                log += "取货门下止点开关故障，";
                                error_ID_E += "16,";
                            }
                            if((rec[9]&0x20) != (byte)0x00){
                                log += "开取货门超时，";
                                error_ID_E += "17,";
                            }
                            if((rec[8]&0x08) != (byte)0x00){
                                log += "防夹手光栅故障，";
                                error_ID_E += "19,";
                            }

                            error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
                            log += "停止出货。";
                            log += "  取货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                    +" 取货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                    +" 取货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                            Log.w("happy", ""+log);
                            Util.WriteFile(log);
                            queryLastedTransaction.setComplete(1);
                            queryLastedTransaction.setError(1);
                            tDao.updateTransaction(queryLastedTransaction);
                            OutGoodsThreadFlag = false;
                            rimBoard1 = Constant.wait;
                            rimBoard2 = Constant.wait;
                            aisleBoard1 = Constant.wait;
                            aisleBoard2 = Constant.wait;
                            midBoard = Constant.wait;
                            mTimer.cancel();
                            mTimerOff.cancel();
                            VMMainThreadFlag = true;
                            mQuery1Flag = true;
                            mQuery2Flag = true;
                            mQuery0Flag = true;
                            mUpdataDatabaseFlag = true;
                            SystemClock.sleep(5);
                            Intent intent = new Intent();
                            intent.setAction("njust_outgoods_complete");
                            intent.putExtra("transaction_order_number", current_transaction_order_number);
                            intent.putExtra("outgoods_status", "fail");
                            intent.putExtra("error_type", "stopAllCounter");
                            intent.putExtra("error_ID", error_ID_E);
                            context.sendBroadcast(intent);
                            Log.w("happy", "开取货门查询反馈故障，停止出货，广播上报");
                            Util.WriteFile("开取货门查询反馈故障，停止出货，广播上报");
//                        }else if((rec[9]&0x60) != (byte)0x00){
//                            getGoodsDoorErrorCount++;
//                            if(getGoodsDoorErrorCount >= 3){
//                                getGoodsDoorErrorCount = 0;
//                                String log = "开取货门3次尝试失败：";
//                                String error_ID_E = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    log += "开取货门超时，";
//                                    error_ID_E += "17,";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    log += "取货门半开，";
//                                    error_ID_E += "18,";
//                                }
//                                error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
//                                log += "停止出货。";
//                                log += "  出货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
//                                        +" 出货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
//                                        +" 出货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
//                                Log.w("happy", ""+log);
//                                Util.WriteFile(log);
//                                queryLastedTransaction.setComplete(1);
//                                queryLastedTransaction.setError(1);
//                                tDao.updateTransaction(queryLastedTransaction);
//                                OutGoodsThreadFlag = false;
//                                rimBoard1 = Constant.wait;
//                                rimBoard2 = Constant.wait;
//                                aisleBoard1 = Constant.wait;
//                                aisleBoard2 = Constant.wait;
//                                midBoard = Constant.wait;
//                                mTimer.cancel();
//                                mTimerOff.cancel();
//                                VMMainThreadFlag = true;
//                                mQuery1Flag = true;
//                                mQuery2Flag = true;
//                                mQuery0Flag = true;
//                                mUpdataDatabaseFlag = true;
//                                SystemClock.sleep(5);
//                                Intent intent = new Intent();
//                                intent.setAction("njust_outgoods_complete");
//                                intent.putExtra("transaction_order_number", current_transaction_order_number);
//                                intent.putExtra("outgoods_status", "fail");
//                                intent.putExtra("error_type", "stopAllCounter");
//                                intent.putExtra("error_ID", error_ID_E);
//                                context.sendBroadcast(intent);
//                            }else{
//                                String a = "";
//                                if((rec[9]&0x20) != (byte)0x00){
//                                    a += "开取货门超时，";
//                                }
//                                if((rec[9]&0x40) != (byte)0x00){
//                                    a += "取货门半开，";
//                                }
//                                a = a.substring(0,a.length()-1);
//                                Util.WriteFile("开取货门出现故障："+a+"  尝试再次打开取货门");
//                                SystemClock.sleep(delay);
//                                midBoard = Constant.openGetGoodsDoor;
//                                midZNum++;
//                            }
                        }else{
                            getGoodsDoorErrorCount = 0;
                            midZNum++;
                            if(complete) {
                                timeStart();
                            }else{
                                timeStartOff();
                            }
                            midBoard = Constant.wait;
                            if(complete){
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(0);
                                tDao.updateTransaction(queryLastedTransaction);
                                SystemClock.sleep(20);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "success");
                                intent.putExtra("error_type", "justRecord&closeAisle");
                                if(!"".equals(error_ID_justRecord)){
                                    error_ID_justRecord = error_ID_justRecord.substring(0,error_ID_justRecord.length()-1);
                                }
                                intent.putExtra("error_ID_justRecord", error_ID_justRecord);
                                intent.putExtra("error_ID_closeAisle", error_ID_closeAisle);
                                context.sendBroadcast(intent);
                                complete = false;
                                Util.WriteFile("成功开启取货门，发送成功广播");
                            }
                        }
                    }
                }
            }
        }
    }

    private void closeGetGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.closeGetGoodsDoor(midZNum);
            Log.w("happy", "发送关取货门指令");Util.WriteFile("发送关取货门指令");
            SystemClock.sleep(delayGetDoor1);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "关取货门反馈："+ str1);Util.WriteFile("关取货门反馈："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            midBoard = Constant.queryCloseGetGoodsDoor;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                queryLastedTransaction.setComplete(1);
                queryLastedTransaction.setError(1);
                tDao.updateTransaction(queryLastedTransaction);
                OutGoodsThreadFlag = false;
                rimBoard1 = Constant.wait;
                rimBoard2 = Constant.wait;
                aisleBoard1 = Constant.wait;
                aisleBoard2 = Constant.wait;
                midBoard = Constant.wait;
                mTimer.cancel();
                mTimerOff.cancel();
                VMMainThreadFlag = true;
                mQuery1Flag = true;
                mQuery2Flag = true;
                mQuery0Flag = true;
                mUpdataDatabaseFlag = true;
                SystemClock.sleep(5);
                Intent intent = new Intent();
                intent.setAction("njust_outgoods_complete");
                intent.putExtra("transaction_order_number", current_transaction_order_number);
                intent.putExtra("outgoods_status", "fail");
                intent.putExtra("error_type", "stopOneCounter");
                intent.putExtra("error_counter", "all");
                intent.putExtra("error_ID", "25");
                context.sendBroadcast(intent);
                Log.w("happy", "中柜关取货门通信故障，停止当次交易，广播上报");
                Util.WriteFile("中柜关取货门通信故障，停止当次交易，广播上报");
            }
        }
    }

    private void queryCloseGetGoodsDoor(){
        mMotorControl.query((byte)0x08,(byte)0xE0,midZNum);
        Log.w("happy", "发送关取货门查询");Util.WriteFile("发送关取货门查询");
        SystemClock.sleep(delayGetDoor1);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            StringBuilder str1 = new StringBuilder();
            for (byte aRec : rec) {
                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
            }
            Log.w("happy", "关取货门查询反馈："+ str1);Util.WriteFile("关取货门查询反馈："+ str1);
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                    if(rec[16] == (byte)0x02){
                        if((rec[8]&0x04) == (byte)0x04){
                            midZNum++;
                            midBoard = Constant.openGetGoodsDoor;
                        }else if((rec[8]&0x20) == (byte)0x20){

                        }
                        else{
                            if((rec[9]&0x3E) != (byte)0x00 || (rec[8]&0x08) != (byte)0x00){//检查停机故障字
                                String error_ID_E = "";
                                String log = "关取货门检测到错误：";
                                if ((rec[9]&0x01) != (byte)0x00) {
                                    log += "已执行动作，";
                                } else {
                                    log += "未执行动作，";
                                }
                                if((rec[9]&0x02) != (byte)0x00){
                                    log += "取货门电机过流，";
                                    error_ID_E += "13,";
                                }
                                if((rec[9]&0x04) != (byte)0x00){
                                    log += "取货门电机断路，";
                                    error_ID_E += "14,";
                                }
                                if((rec[9]&0x08) != (byte)0x00){
                                    log += "取货门上止点开关故障，";
                                    error_ID_E += "15,";
                                }
                                if((rec[9]&0x10) != (byte)0x00){
                                    log += "取货门下止点开关故障，";
                                    error_ID_E += "16,";
                                }
                                if((rec[9]&0x20) != (byte)0x00){
                                    log += "关取货门超时，";
                                    error_ID_E += "17,";
                                }
                                if((rec[8]&0x08) != (byte)0x00){
                                    log += "防夹手光栅故障，";
                                    error_ID_E += "19,";
                                }
                                error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
                                log += "停止出货。";
                                log += "  取货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
                                        +" 取货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
                                        +" 取货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
                                Log.w("happy", ""+log);
                                Util.WriteFile(log);
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(1);
                                tDao.updateTransaction(queryLastedTransaction);
                                OutGoodsThreadFlag = false;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                                SystemClock.sleep(5);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "fail");
                                intent.putExtra("error_type", "stopAllCounter");
                                intent.putExtra("error_ID", error_ID_E);
                                context.sendBroadcast(intent);
                                Log.w("happy", "关取货门查询反馈故障，广播上报");
                                Util.WriteFile("关取货门查询反馈故障，广播上报");
//                            }else if((rec[9]&0x60) != (byte)0x00){
//                                getGoodsDoorErrorCount++;
//                                if(getGoodsDoorErrorCount >= 3){
//                                    getGoodsDoorErrorCount = 0;
//                                    String log = "关取货门3次尝试失败：";
//                                    String error_ID_E = "";
//                                    if((rec[9]&0x20) != (byte)0x00){
//                                        log += "关取货门超时，";
//                                        error_ID_E += "17,";
//                                    }
//                                    if((rec[9]&0x40) != (byte)0x00){
//                                        log += "取货门半关，";
//                                        error_ID_E += "18,";
//                                    }
//                                    error_ID_E = error_ID_E.substring(0,error_ID_E.length()-1);
//                                    log += "停止出货。";
//                                    log += "  取货门电机实际动作时间（毫秒）:"+ ((rec[10]&0xff) * 256 + (rec[11]&0xff))
//                                            +" 取货门电机最大电流（毫安）:"+ ((rec[12]&0xff) * 256 + (rec[13]&0xff))
//                                            +" 取货门电机平均电流（毫安）:"+ ((rec[14]&0xff) * 256 + (rec[15]&0xff));
//                                    Log.w("happy", ""+log);
//                                    Util.WriteFile(log);
//                                    queryLastedTransaction.setComplete(1);
//                                    queryLastedTransaction.setError(1);
//                                    tDao.updateTransaction(queryLastedTransaction);
//                                    OutGoodsThreadFlag = false;
//                                    rimBoard1 = Constant.wait;
//                                    rimBoard2 = Constant.wait;
//                                    aisleBoard1 = Constant.wait;
//                                    aisleBoard2 = Constant.wait;
//                                    midBoard = Constant.wait;
//                                    mTimer.cancel();
//                                    mTimerOff.cancel();
//                                    VMMainThreadFlag = true;
//                                    mQuery1Flag = true;
//                                    mQuery2Flag = true;
//                                    mQuery0Flag = true;
//                                    mUpdataDatabaseFlag = true;
//                                    SystemClock.sleep(5);
//                                    Intent intent = new Intent();
//                                    intent.setAction("njust_outgoods_complete");
//                                    intent.putExtra("transaction_order_number", current_transaction_order_number);
//                                    intent.putExtra("outgoods_status", "fail");
//                                    intent.putExtra("error_type", "stopAllCounter");
//                                    intent.putExtra("error_ID", error_ID_E);
//                                    context.sendBroadcast(intent);
//                                }else{
//                                    String a = "";
//                                    if((rec[9]&0x20) != (byte)0x00){
//                                        a += "关取货门超时，";
//                                    }
//                                    if((rec[9]&0x40) != (byte)0x00){
//                                        a += "取货门半关，";
//                                    }
//                                    a = a.substring(0,a.length()-1);
//                                    Util.WriteFile("关闭取货门出现故障："+a+"  尝试再次关闭取货门");
//                                    SystemClock.sleep(delay);
//                                    midBoard = Constant.closeGetGoodsDoor;
//                                    midZNum++;
//                                }
                            }else{
                                getGoodsDoorErrorCount = 0;
                                midZNum++;
                                closeGetGoodsDoor = true;
                                rimBoard1 = Constant.wait;
                                rimBoard2 = Constant.wait;
                                aisleBoard1 = Constant.wait;
                                aisleBoard2 = Constant.wait;
                                midBoard = Constant.wait;
                                OutGoodsThreadFlag = false;
                                mTimer.cancel();
                                mTimerOff.cancel();
                                queryLastedTransaction.setComplete(1);
                                queryLastedTransaction.setError(0);
                                tDao.updateTransaction(queryLastedTransaction);
                                SystemClock.sleep(20);
                                Intent intent = new Intent();
                                intent.setAction("njust_outgoods_complete");
                                intent.putExtra("transaction_order_number", current_transaction_order_number);
                                intent.putExtra("outgoods_status", "closeMidDoorSuccess");
                                context.sendBroadcast(intent);
                                Log.w("happy", "本次交易完毕");Util.WriteFile("本次交易完毕");
                                VMMainThreadFlag = true;
                                mQuery1Flag = true;
                                mQuery2Flag = true;
                                mQuery0Flag = true;
                                mUpdataDatabaseFlag = true;
                            }
                        }
                    }
                }
            }
        }
    }

//    private void openDropGoodsDoor(){
//        boolean flag = true;
//        int times = 0;
//        while (flag){
//            mMotorControl.openDropGoodsDoor(midZNum);
//            SystemClock.sleep(delay);
//            byte[] rec = serialPort485.receiveData();
//            if (rec != null && rec.length >= 5) {
//                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
//                    if(rec[6] == (byte)0x66 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
//                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
//                            flag = false;
//                            midZNum++;
//                            midBoard = Constant.queryOpenDropGoodsDoor;
//                        }
//                    }
//                }
//            }
//            times = times + 1;
//            if(times == 5){
//                flag = false;
//                handleCommunicationError();
//                Log.w("happy", "中柜板通信故障");
//                Util.WriteFile("中柜板通信故障");
//            }
//        }
//    }
//
//    private void queryOpenDropGoodsDoor(){
//        mMotorControl.query((byte)0x09,(byte)0xE0,midZNum);
//        SystemClock.sleep(delay);
//        byte[] rec = serialPort485.receiveData();
//        if (rec != null && rec.length >= 5) {
//            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
//                if(rec[6] == (byte)0x66 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
//                    if(rec[16] == (byte)0x02){
//                        midZNum++;
//                        midBoard = Constant.closeDropGoodsDoor;
//                        if((rec[9]&0x01) != (byte)0x01){
//                            byte[] errorRec = new byte[9];
//                            System.arraycopy(rec, 7, errorRec, 0, 9);
//                            errorHandling(0,(byte)0x46,errorRec);
//                        }
//                    }
//                }
//            }
//        }
//    }
//
//    private void closeDropGoodsDoor(){
//        boolean flag = true;
//        int times = 0;
//        while (flag){
//            mMotorControl.closeDropGoodsDoor(midZNum);
//            SystemClock.sleep(delay);
//            byte[] rec = serialPort485.receiveData();
//            if (rec != null && rec.length >= 5) {
//                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
//                    if(rec[6] == (byte)0x6C && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
//                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
//                            flag = false;
//                            midZNum++;
//                            midBoard = Constant.queryCloseDropGoodsDoor;
//                        }
//                    }
//                }
//            }
//            times = times + 1;
//            if(times == 5){
//                flag = false;
//                handleCommunicationError();
//                Log.w("happy", "中柜板通信故障");
//                Util.WriteFile("中柜板通信故障");
//            }
//        }
//    }
//
//    private void queryCloseDropGoodsDoor(){
//        mMotorControl.query((byte)0x0A,(byte)0xE0,midZNum);
//        SystemClock.sleep(delay);
//        byte[] rec = serialPort485.receiveData();
//        if (rec != null && rec.length >= 5) {
//            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
//                if(rec[6] == (byte)0x6C && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
//                    if(rec[16] == (byte)0x02){
//                        if((rec[9]&0x01) != (byte)0x01){
//                            byte[] errorRec = new byte[9];
//                            System.arraycopy(rec, 7, errorRec, 0, 9);
//                            errorHandling(0,(byte)0x46,errorRec);
//                        }
//                        midZNum++;
//                    }
//                }
//            }
//        }
//    }

//    private void errorHandling(int counter, byte module, byte[] rec) {
//        MachineStateDao machineStateDao = new MachineStateDaoImpl(context);
//        MalfunctionDao malfunctionDao = new MalfunctionDaoImpl(context);
//        TransactionDao transactionDao = new TransactionDaoImpl(context);
//        Malfunction malfunction = new Malfunction();
//        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
//        Transaction transaction = transactionDao.queryLastedTransaction();
//        byte error1[];
//        byte error2[];
//        String Counter;
//        if(counter == 0){
//            Counter = "中柜";
//            machineStateDao.updateState(1);
//        }else{
//            Counter = counter == 1? "左柜":"右柜";
//            if(counter == 1){
//                machineStateDao.updateCounterState(1,0);
//            }else {
//                machineStateDao.updateCounterState(0,1);
//            }
//        }
//        switch (module) {
//            /*字符“Y”，表示驱动Y轴电机的应答*/
//            case 0x59: {
//                String log = Counter + "检测到错误：";
//                error1 = byteTo8Byte(rec[2]);
//                if (error1[0] != (byte)0x00) {
//                    log += "已执行动作，";
//                } else {
//                    log += "未执行动作，";
//                }
//                if(error1[1] == (byte)0x01){
//                    log += "Y轴电机过流，";
//                }
//                if(error1[2] == (byte)0x01){
//                    log += "Y轴电机断路，";
//                }
//                if(error1[3] == (byte)0x01){
//                    log += "Y轴上止点开关故障，";
//                }
//                if(error1[4] == (byte)0x01){
//                    log += "Y轴下止点开关故障，";
//                }
//                if(error1[5] == (byte)0x01){
//                    log += "Y轴电机超时，";
//                }
//                if(error1[6] == (byte)0x01){
//                    log += "Y轴码盘故障，";
//                }
//                if(error1[7] == (byte)0x01){
//                    log += "Y轴出货门定位开关故障，";
//                }
//                log += "停止出货。";
//                malfunction.setTransactionID(current_transaction_order_number);
//                String time = sdf.format(Calendar.getInstance().getTime());
//                malfunction.setErrorTime(time);
//                malfunction.setCounter(counter);
//                malfunction.setErrorModule(Counter+"Y轴电机");
//                malfunction.setErrorDescription(log);
//                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
//                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
//                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                malfunctionDao.addMalfunction(malfunction);
//                log += " Y轴电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
//                        +" Y轴电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
//                        +" Y轴电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                Log.w("happy", ""+log);
//                Util.WriteFile(log);
//                break;
//            }
//            /*字符“P”，表示驱动货道电机的应答*/
//            case 0x50: {
//                String log = Counter + "检测到错误：";
//                error1 = byteTo8Byte(rec[2]);
//                if (error1[0] != (byte)0x00) {
//                    log += "已执行动作，";
//                } else {
//                    log += "未执行动作，";
//                }
//                if(error1[1] == (byte)0x01){
//                    log += "货道电机过流，";
//                }
//                if(error1[2] == (byte)0x01){
//                    log += "货道电机断路，";
//                }
//                if(error1[3] == (byte)0x01){
//                    log += "货道超时（无货物输出的超时），";
//                }
//                if(error1[4] == (byte)0x01){
//                    log += "商品超时（货物遮挡光栅超时），";
//                }
//                if(error1[5] == (byte)0x01){
//                    log += "弹簧电机1反馈开关故障，";
//                }
//                if(error1[6] == (byte)0x01){
//                    log += "弹簧电机2反馈开关故障，";
//                }
//                if(error1[7] == (byte)0x01){
//                    log += "货道下货光栅故障，";
//                }
//                log += "停止出货。";
//                malfunction.setTransactionID(current_transaction_order_number);
//                String time = sdf.format(Calendar.getInstance().getTime());
//                malfunction.setErrorTime(time);
//                malfunction.setCounter(counter);
//                malfunction.setErrorModule(Counter+"货道电机");
//                malfunction.setErrorDescription(log);
//                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
//                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
//                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                malfunctionDao.addMalfunction(malfunction);
//                log += " 货道电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
//                        +" 货道电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
//                        +" 货道电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                Log.w("happy", ""+log);
//                Util.WriteFile(log);
//                break;
//            }
//            /*字符“X”，表示驱动X轴电机的应答*/
//            case 0x58: {
//                String log = Counter + "检测到错误：";
//                error1 = byteTo8Byte(rec[2]);
//                if (error1[0] != (byte)0x00) {
//                    log += "已执行动作，";
//                } else {
//                    log += "未执行动作，";
//                }
//                if(error1[1] == (byte)0x01){
//                    log += "X轴电机过流，";
//                }
//                if(error1[2] == (byte)0x01){
//                    log += "X轴电机断路，";
//                }
//                if(error1[3] == (byte)0x01){
//                    log += "X轴出货光栅未检测到货物，";
//                }
//                if(error1[4] == (byte)0x01){
//                    log += "X轴出货光栅货物遮挡超时，";
//                }
//                if(error1[5] == (byte)0x01){
//                    log += "X轴出货光栅故障，";
//                }
//                if(error1[6] == (byte)0x01){
//                    log += "X轴电机超时，";
//                }
//                log += "停止出货。";
//                malfunction.setTransactionID(current_transaction_order_number);
//                String time = sdf.format(Calendar.getInstance().getTime());
//                malfunction.setErrorTime(time);
//                malfunction.setCounter(counter);
//                malfunction.setErrorModule(Counter+"X轴电机");
//                malfunction.setErrorDescription(log);
//                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
//                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
//                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                malfunctionDao.addMalfunction(malfunction);
//                log += " 货道电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
//                        +" 货道电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
//                        +" 货道电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                Log.w("happy", ""+log);
//                Util.WriteFile(log);
//                break;
//            }
//            /*字符“Z”，表示驱动出货门电机的应答*/
//            case 0x5A: {
//                String log = Counter + "检测到错误：";
//                error1 = byteTo8Byte(rec[2]);
//                if (error1[0] != (byte)0x00) {
//                    log += "已执行动作，";
//                } else {
//                    log += "未执行动作，";
//                }
//                if(error1[1] == (byte)0x01){
//                    log += "出货门电机过流，";
//                }
//                if(error1[2] == (byte)0x01){
//                    log += "出货门电机断路，";
//                }
//                if(error1[3] == (byte)0x01){
//                    log += "出货门前止点开关故障，";
//                }
//                if(error1[4] == (byte)0x01){
//                    log += "出货门后止点开关故障，";
//                }
//                if(error1[5] == (byte)0x01){
//                    log += "开、关出货门超时，";
//                }
//                if(error1[6] == (byte)0x01){
//                    log += "出货门半开、半关，";
//                }
//                log += "停止出货。";
//                malfunction.setTransactionID(current_transaction_order_number);
//                String time = sdf.format(Calendar.getInstance().getTime());
//                malfunction.setErrorTime(time);
//                malfunction.setCounter(counter);
//                malfunction.setErrorModule(Counter+"出货门电机");
//                malfunction.setErrorDescription(log);
//                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
//                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
//                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                malfunctionDao.addMalfunction(malfunction);
//                log += " 出货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
//                        +" 出货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
//                        +" 出货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                Log.w("happy", ""+log);
//                Util.WriteFile(log);
//                break;
//            }
//            /*字符“M”，表示驱动取货门电机的应答*/
//            case 0x4D: {
//                String log = Counter + "检测到错误：";
//                error1 = byteTo8Byte(rec[2]);
//                error2 = byteTo8Byte(rec[1]);
//                if (error1[0] != (byte)0x00) {
//                    log += "已执行动作，";
//                } else {
//                    log += "未执行动作，";
//                }
//                if(error1[1] == (byte)0x01){
//                    log += "取货门电机过流，";
//                }
//                if(error1[2] == (byte)0x01){
//                    log += "取货门电机断路，";
//                }
//                if(error1[3] == (byte)0x01){
//                    log += "取货门上止点开关故障，";
//                }
//                if(error1[4] == (byte)0x01){
//                    log += "取货门下止点开关故障，";
//                }
//                if(error1[5] == (byte)0x01){
//                    log += "开、关取货门超时，";
//                }
//                if(error1[6] == (byte)0x01){
//                    log += "取货门半开、半关，";
//                }
//                if(error1[7] == (byte)0x01){
//                    log += "取货仓光栅检测无货物，";
//                }
//                if(error2[0] == (byte)0x01){
//                    log += "取货仓光栅检测有货物，";
//                }
//                if(error2[1] == (byte)0x01){
//                    log += "取货篮光栅故障，";
//                }
//                if(error2[2] == (byte)0x01){
//                    log += "防夹手光栅检测到遮挡物，";
//                }
//                if(error2[3] == (byte)0x01){
//                    log += "防夹手光栅故障，";
//                }
//                log += "停止出货。";
//                malfunction.setTransactionID(current_transaction_order_number);
//                String time = sdf.format(Calendar.getInstance().getTime());
//                malfunction.setErrorTime(time);
//                malfunction.setCounter(counter);
//                malfunction.setErrorModule(Counter+"取货门电机");
//                malfunction.setErrorDescription(log);
//                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
//                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
//                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                malfunctionDao.addMalfunction(malfunction);
//                log += " 取货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
//                        +" 取货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
//                        +" 取货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                Log.w("happy", ""+log);
//                Util.WriteFile(log);
//                break;
//            }
//            /*字符“F”，表示驱动落货门电机的应答*/
//            case 0x46: {
//                String log = Counter + "检测到错误：";
//                error1 = byteTo8Byte(rec[2]);
//                error2 = byteTo8Byte(rec[1]);
//                if (error1[0] != (byte)0x00) {
//                    log += "已执行动作，";
//                } else {
//                    log += "未执行动作，";
//                }
//                if(error1[1] == (byte)0x01){
//                    log += "落货门电机过流，";
//                }
//                if(error1[2] == (byte)0x01){
//                    log += "落货门电机断路，";
//                }
//                if(error1[3] == (byte)0x01){
//                    log += "落货门上止点开关故障，";
//                }
//                if(error1[4] == (byte)0x01){
//                    log += "落货门下止点开关故障，";
//                }
//                if(error1[5] == (byte)0x01){
//                    log += "开、关落货门超时，";
//                }
//                if(error1[6] == (byte)0x01){
//                    log += "落货门半开、半关，";
//                }
//                if(error1[7] == (byte)0x01){
//                    log += "落货仓光栅检测无货物，";
//                }
//                if(error2[0] == (byte)0x01){
//                    log += "落货仓光栅检测有货物，";
//                }
//                if(error2[1] == (byte)0x01){
//                    log += "落货篮光栅故障，";
//                }
//                log += "停止出货。";
//                malfunction.setTransactionID(current_transaction_order_number);
//                String time = sdf.format(Calendar.getInstance().getTime());
//                malfunction.setErrorTime(time);
//                malfunction.setCounter(counter);
//                malfunction.setErrorModule(Counter+"落货门电机");
//                malfunction.setErrorDescription(log);
//                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
//                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
//                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                malfunctionDao.addMalfunction(malfunction);
//                log += " 落货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
//                        +" 落货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
//                        +" 落货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
//                Log.w("happy", ""+log);
//                Util.WriteFile(log);
//                break;
//            }
//            default:break;
//        }
//        transaction.setComplete(1);
//        transaction.setError(1);
//        transactionDao.updateTransaction(transaction);
//        OutGoodsThreadFlag = false;
//        SystemClock.sleep(20);
//        Intent intent = new Intent();
//        intent.setAction("njust_outgoods_complete");
//        intent.putExtra("transaction_order_number", current_transaction_order_number);
//        intent.putExtra("outgoods_status", "fail");
//        context.sendBroadcast(intent);
//        VMMainThreadFlag = true;
//        mQuery1Flag = true;
//        mQuery2Flag = true;
//        mQuery0Flag = true;
//        mUpdataDatabaseFlag = true;
//    }
//    /**
//     * 将byte转换为一个长度为8的byte数组，数组每个值代表bit
//     * @param b 1个字节byte数据
//     * */
//    private static byte[] byteTo8Byte(byte b) {
//        byte[] array = new byte[8];
//        for (int i = 0; i <= 7; i++) {
//            array[i] = (byte)(b & 1);
//            b = (byte) (b >> 1);
//        }
//        return array;
//    }
}
