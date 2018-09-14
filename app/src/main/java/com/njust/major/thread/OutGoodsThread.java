package com.njust.major.thread;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.njust.SerialPort;
import com.njust.major.SCM.MotorControl;
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
import static com.njust.VMApplication.current_transaction_order_number;
import static com.njust.VMApplication.rimZNum1;
import static com.njust.VMApplication.rimZNum2;
import static com.njust.VMApplication.aisleZNum1;
import static com.njust.VMApplication.aisleZNum2;
import static com.njust.VMApplication.midZNum;



public class OutGoodsThread extends Thread {
    private Context context;

    private SerialPort serialPort485;
    private MotorControl mMotorControl;

    private PositionDao pDao;
    private TransactionDao tDao;
    private Transaction queryLastedTransaction;
    private Timer mTimer = new Timer();

    private static int packageCount1 = 0;//存放左右柜分别打包的个数,每三个凑成一个包，完成一次轮询出货
    private static int packageCount2 = 0;
    private int moveTime = 1200;
    private int moveTimeOut = 3600;
    private int delay = 150;
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
    private boolean finshDone1 = false;
    private boolean finshDone2 = false;


    public OutGoodsThread(Context context){
        super();
        this.context = context;
        serialPort485 = new SerialPort(1, 38400, 8, 'n', 1);
        mMotorControl = new MotorControl(serialPort485,context);
        pDao = new PositionDaoImpl(context);
        tDao = new TransactionDaoImpl(context);
    }

    public void init() {
        queryLastedTransaction = tDao.queryLastedTransaction();
        Log.w("happy", queryLastedTransaction.toString());
        String [] str = queryLastedTransaction.getPositionIDs().split(" ");
        int[] positionIDs = new int[str.length];
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
        }
    }

    private int[][] calculateOrientationTime(int counter, int[] set ,int packageCount){
        int[][] outGoods = new int[packageCount][9];
        if(packageCount > 0) {
            /*循环每个包*/
            for (int i = 1; i <= packageCount; i++) {
                /*打包只有一件货*/
                if ((set.length - (i - 1) * 3) == 1) {
                    outGoods[i-1][0] = set[(i-1)*3];
                    for(int j= 1; j<=8 ; j++){
                        outGoods[i-1][j] = 0;
                    }
                }
                /*打包两件货*/
                else if((set.length - (i - 1) * 3) == 2){
                    if(isWhere(counter,set[(i-1)*3]) == isWhere(counter,set[(i-1)*3+1]) && isWhere(counter,set[(i-1)*3]) <= 3){
                        outGoods[i-1][0] = set[(i-1)*3];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = set[(i-1)*3+1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else if(isWhere(counter,set[(i-1)*3]) == isWhere(counter,set[(i-1)*3+1]) && isWhere(counter,set[(i-1)*3]) > 3){
                        outGoods[i-1][0] = set[(i-1)*3];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = set[(i-1)*3+1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        for(int j= 6; j<=8 ; j++){
                            outGoods[i-1][j] = 0;
                        }
                    }else{
                        outGoods[i-1][0] = set[(i-1)*3];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = set[(i-1)*3+1];
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
                    /*1、2、3位置三个重叠*/
                    if(isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == isWhere(counter,sort[2]) && isWhere(counter,sort[0]) <= 3){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }
                    /*4、5位置三个重叠*/
                    else if(isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == isWhere(counter,sort[2]) && isWhere(counter,sort[0]) > 3){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 1;
                        outGoods[i-1][8] = moveTime;
                    }
                    /*1、1、2 或2、2、3*/
                    else if((isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == 1 && isWhere(counter,sort[2]) == 2)
                            ||(isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == 2 && isWhere(counter,sort[2]) == 3)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 0;
                        outGoods[i-1][8] = 0;
                    }
                    /*1、1、3或1、1、4或1、1、5或2、2、4或2、2、5*/
                    else if((isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == 1 && isWhere(counter,sort[2]) > 2)
                            ||(isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == 2 && isWhere(counter,sort[2]) > 3)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 0;
                        outGoods[i-1][8] = 0;
                    }
                    /*1、2、2*/
                    else if(isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 2 && isWhere(counter,sort[0]) == 1){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }
                    /*1、3、3*/
                    else if(isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 3 && isWhere(counter,sort[0]) == 1){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 0;
                        outGoods[i-1][5] = 0;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 2;
                        outGoods[i-1][8] = moveTime;
                    }
                    /*1、4、4或1、5、5*/
                    else if((isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 4 && isWhere(counter,sort[0]) == 1)
                            || (isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 5 && isWhere(counter,sort[0]) == 1)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 1;
                        outGoods[i-1][8] = moveTime;
                    }
                    /*2、4、4或2、5、5*/
                    else if((isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 4 && isWhere(counter,sort[0]) == 2)
                            || (isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 5 && isWhere(counter,sort[0]) == 2)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 1;
                        outGoods[i-1][8] = moveTime;
                    }
                    /*2、3、3*/
                    else if((isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 3 && isWhere(counter,sort[0]) == 2)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 2;
                        outGoods[i-1][5] = moveTime*2;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 0;
                        outGoods[i-1][8] = 0;
                    }
                    /*3、3、4或3、3、5*/
                    else if((isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == 3 && isWhere(counter,sort[2]) == 4)
                            ||(isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == 3 && isWhere(counter,sort[2]) == 5)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 0;
                        outGoods[i-1][8] = 0;
                    }
                    /*3、4、4或3、5、5*/
                    else if((isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 4 && isWhere(counter,sort[0]) == 3)
                            ||(isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 5 && isWhere(counter,sort[0]) == 3)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 0;
                        outGoods[i-1][5] = 0;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 1;
                        outGoods[i-1][8] = moveTime;
                    }
                    /*4、4、5或4、5、5*/
                    else if((isWhere(counter,sort[0]) == isWhere(counter,sort[1]) && isWhere(counter,sort[0]) == 4 && isWhere(counter,sort[2]) == 5)
                            ||(isWhere(counter,sort[1]) == isWhere(counter,sort[2]) && isWhere(counter,sort[1]) == 5 && isWhere(counter,sort[0]) == 4)){
                        outGoods[i-1][0] = sort[0];
                        outGoods[i-1][1] = 0;
                        outGoods[i-1][2] = 0;
                        outGoods[i-1][3] = sort[1];
                        outGoods[i-1][4] = 1;
                        outGoods[i-1][5] = moveTime;
                        outGoods[i-1][6] = sort[2];
                        outGoods[i-1][7] = 1;
                        outGoods[i-1][8] = moveTime;
                    }
                    else{
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
                where = row1;
                break;
            case 3:
                where = row1 % 2 == 0 ? row1 / 2 : (row1 / 2 + 1);
                break;
            case 2:
                if (counter == 1) {
                    where = row1 / 2;
                } else {
                    where = row1 / 2 + 1;
                }
                break;
            case 1:
                if (counter == 1) {
                    if (row1 == 1 || row1 == 2) {
                        where = 1;
                    } else if (row1 == 3 || row1 == 4 || row1 == 5) {
                        where = 2;
                    } else if (row1 == 6 || row1 == 7) {
                        where = 3;
                    } else if (row1 == 8) {
                        where = 4;
                    }
                } else {
                    if (row2 == 9 || row2 == 10) {
                        where = 5;
                    } else if (row2 == 6 || row2 == 7 || row2 == 8) {
                        where = 4;
                    } else if (row2 == 4 || row2 == 5) {
                        where = 3;
                    } else if (row2 == 3) {
                        where = 2;
                    }
                }
                break;
        }
        return  where;//1-5
    }

    /**
     * 接受指令预处理
     * 说明：假如指令前后存在乱码则处理掉,暂时不用，预防特殊情况
     * @param rec 接收到的串口数据
     * */
    private byte[] preprocessRec(byte[] rec){
        byte[] rec1;
        if(rec != null && rec.length >= 5) {
            StringBuilder str = new StringBuilder();
            for (byte aRec1 : rec) {
                str.append(Integer.toHexString(aRec1 & 0xFF)).append(" ");
            }
            Log.w("happy", "收到原始串口："+ str);
        }
        if(rec != null && rec.length >= 5) {
            if (rec[0] != (byte) 0xE2 || rec[rec.length - 2] != (byte) 0xF1) {
                boolean head = true;
                boolean trail = true;
                int start = 0;
                int end = 0;
                for (int y = 0; y < rec.length; y++) {
                    if (head) {
                        if (rec[y] == 0xE2) {
                            head = false;
                            start = y;
                        }
                    }
                    if (trail) {
                        if (rec[rec.length - 1 - y] == 0xF1) {
                            trail = false;
                            end = y - 1;
                        }
                    }
                }
                rec1 = new byte[rec.length - end - start];
                System.arraycopy(rec, start, rec, 0, rec.length - end - start);
            }else{
                rec1 = new byte[rec.length];
                rec1 = rec;
            }
            return rec1;
        }
        return rec;
    }

    /**
     * 处理通信故障
     * 说明：判定通信故障后调用
     * */
    private void handleCommunicationError(){
        queryLastedTransaction.setComplete(1);
        queryLastedTransaction.setError(1);
        tDao.updateTransaction(queryLastedTransaction);
        OutGoodsThreadFlag = false;
        SystemClock.sleep(20);
        Intent intent = new Intent();
        intent.setAction("njust_outgoods_complete");
        intent.putExtra("transaction_order_number", current_transaction_order_number);
        intent.putExtra("outgoods_status", "fail");
        context.sendBroadcast(intent);
    }

    /**
     * 校验和验证
     * 说明：验证接收到的数据是否正确
     * @param rec 接收到的串口数据或者处理过的串口数据
     * */
    private static boolean isVerify(byte[] rec){
        int sum = 0;
        for (int i = 0; i < rec.length - 1; i++) {
            sum = sum + rec[i];
        }
        return sum == rec[rec.length-1];
    }

    private void timeStart(){
        TimerTask timerTask = new TimerTask() {
            @Override
            public void run() {
                midBoard = Constant.closeGetGoodsDoor;
                if (mTimer != null) {
                    mTimer.cancel();
                    mTimer = null;
                }
            }
        };
        mTimer.schedule(timerTask,1000);
    }


    private void moveFloor1(){
        boolean flag = true;
        int times = 0;
        while (flag){
            mMotorControl.moveFloor(1,rimZNum1,goods1[currentPackageCount1][currentOutCount1*3]);
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "左边柜板通信故障");
                Util.WriteFile("左边柜板通信故障");
            }
        }
    }

    private void queryMoveFloor1(){
        mMotorControl.query((byte)0x01,(byte)0xC0,rimZNum1);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        rimBoard1 = Constant.wait;
                        aisleBoard1 = Constant.pushGoods;
                        rimZNum1++;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(1,(byte)0x59,errorRec);
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
            mMotorControl.moveFloor(2,rimZNum2,goods2[currentPackageCount2][currentOutCount2*3]);
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                            flag = false;
                            rimBoard2 = Constant.queryMoveFloor;
                            rimZNum2++;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                handleCommunicationError();
                Log.w("happy", "右边柜板通信故障");
                Util.WriteFile("右边柜板通信故障");
            }
        }
    }

    private void queryMoveFloor2(){
        mMotorControl.query((byte)0x01,(byte)0xC1,rimZNum2);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        rimBoard2 = Constant.wait;
                        aisleBoard2 = Constant.pushGoods;
                        rimZNum2++;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(2,(byte)0x59,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x70 && rec[3] == (byte)0x80 && rec[7] == (byte)0x50){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            aisleBoard1 = Constant.queryPushGoods;
                            aisleZNum1++;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                handleCommunicationError();
                Log.w("happy", "左货道板通信故障");
                Util.WriteFile("左货道板通信故障");
            }
        }
    }

    private void queryPushGoods1(){
        mMotorControl.query((byte)0x03,(byte)0x80,aisleZNum1);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x70 && rec[3] == (byte)0x80 && rec[7] == (byte)0x50){
                    if(rec[16] == (byte)0x02){
                        aisleBoard1 = Constant.wait;
                        aisleZNum1++;
                        currentOutCount1 = currentOutCount1 + 1;
                        if(currentOutCount1 == 3 || goods1[currentPackageCount1][currentOutCount1*3] == 0){
                            currentOutCount1 = 0;
                            rimBoard1 = Constant.moveFloorOut;
                        }else{
                            if(goods1[currentPackageCount1][currentOutCount1*3+1] == 0){
                                if(((goods1[currentPackageCount1][(currentOutCount1-1)*3]-1)/10) == ((goods1[currentPackageCount1][currentOutCount1*3]-1)/10)){
                                    aisleBoard1 = Constant.pushGoods;
                                }else{
                                    rimBoard1 = Constant.moveFloor;
                                }
                            }else{
                                rimBoard1 = Constant.moveHorizontal;
                            }
                        }
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(1,(byte)0x50,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x70 && rec[3] == (byte)0x81 && rec[7] == (byte)0x50){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            aisleBoard2 = Constant.queryPushGoods;
                            aisleZNum2++;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                handleCommunicationError();
                Log.w("happy", "右货道板通信故障");
                Util.WriteFile("右货道板通信故障");
            }
        }
    }

    private void queryPushGoods2(){
        mMotorControl.query((byte)0x03,(byte)0x81,aisleZNum2);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x70 && rec[3] == (byte)0x81 && rec[7] == (byte)0x50){
                    if(rec[16] == (byte)0x02){
                        aisleBoard2 = Constant.wait;
                        aisleZNum2++;
                        currentOutCount2 = currentOutCount2 + 1;
                        if(currentOutCount2 == 3 || goods2[currentPackageCount2][currentOutCount2*3] == 0){
                            currentOutCount2 = 0;
                            rimBoard2 = Constant.moveFloorOut;
                        }else{
                            if(goods2[currentPackageCount2][currentOutCount2*3+1] == 0){
                                if(((goods2[currentPackageCount2][(currentOutCount2-1)*3]-1)/10) == ((goods2[currentPackageCount2][currentOutCount2*3]-1)/10)){
                                    aisleBoard2 = Constant.pushGoods;
                                }else{
                                    rimBoard2 = Constant.moveFloor;
                                }
                            }else{
                                rimBoard2 = Constant.moveHorizontal;
                            }
                        }
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(2,(byte)0x50,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "左边柜板通信故障");
                Util.WriteFile("左边柜板通信故障");
            }
        }
    }

    private void queryMoveHorizontal1(){
        mMotorControl.query((byte)0x02,(byte)0xC0,rimZNum1);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                    if(rec[16] == (byte)0x02){
                        rimBoard1 = Constant.wait;
                        rimZNum1++;
                        if(((goods1[currentPackageCount1][(currentOutCount1-1)*3]-1)/10) == ((goods1[currentPackageCount1][currentOutCount1*3]-1)/10)){
                            aisleBoard1 = Constant.pushGoods;
                        }else{
                            rimBoard1 = Constant.moveFloor;
                        }
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(1,(byte)0x58,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "右边柜板通信故障");
                Util.WriteFile("右边柜板通信故障");
            }
        }
    }

    private void queryMoveHorizontal2(){
        mMotorControl.query((byte)0x02,(byte)0xC1,rimZNum2);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                    if(rec[16] == (byte)0x02){
                        rimBoard2 = Constant.wait;
                        rimZNum2++;
                        if(((goods2[currentPackageCount2][(currentOutCount2-1)*3]-1)/10) == ((goods2[currentPackageCount2][currentOutCount2*3]-1)/10)){
                            aisleBoard2 = Constant.pushGoods;
                        }else{
                            rimBoard2 = Constant.moveFloor;
                        }
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(2,(byte)0x58,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "左边柜板通信故障");
                Util.WriteFile("左边柜板通信故障");
            }
        }
    }

    private void queryMoveFloorOut1(){
        mMotorControl.query((byte)0x01,(byte)0xC0,rimZNum1);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        rimZNum1++;
                        if(closeGetGoodsDoor){
                            rimBoard1 = Constant.openOutGoodsDoor;
                        }
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(1,(byte)0x59,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "右边柜板通信故障");
                Util.WriteFile("右边柜板通信故障");
            }
        }
    }

    private void queryMoveFloorOut2(){
        mMotorControl.query((byte)0x01,(byte)0xC1,rimZNum2);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x79 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        rimZNum2++;
                        if(closeGetGoodsDoor){
                            rimBoard2 = Constant.openOutGoodsDoor;
                        }
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(2,(byte)0x59,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "左边柜板通信故障");
                Util.WriteFile("左边柜板通信故障");
            }
        }
    }

    private void queryOpenOutGoodsDoor1(){
        mMotorControl.query((byte)0x04,(byte)0xC0,rimZNum1);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                    if(rec[16] == (byte)0x02){
                        closeOutGoodsDoor1 = false;
                        rimBoard1 = Constant.moveHorizontalOut;
                        rimZNum1++;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(1,(byte)0x5A,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "右边柜板通信故障");
                Util.WriteFile("右边柜板通信故障");
            }
        }
    }

    private void queryOpenOutGoodsDoor2(){
        mMotorControl.query((byte)0x04,(byte)0xC1,rimZNum2);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                    if(rec[16] == (byte)0x02){
                        closeOutGoodsDoor2 = false;
                        rimBoard2 = Constant.moveHorizontalOut;
                        rimZNum2++;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(2,(byte)0x5A,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "左边柜板通信故障");
                Util.WriteFile("左边柜板通信故障");
            }
        }
    }

    private void queryMoveHorizontalOut1(){
        mMotorControl.query((byte)0x02,(byte)0xC0,rimZNum1);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                    if(rec[16] == (byte)0x02){
                        rimBoard1 = Constant.closeOutGoodsDoor;
                        rimZNum1++;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(1,(byte)0x58,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "右边柜板通信故障");
                Util.WriteFile("右边柜板通信故障");
            }
        }
    }

    private void queryMoveHorizontalOut2(){
        mMotorControl.query((byte)0x02,(byte)0xC1,rimZNum2);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                    if(rec[16] == (byte)0x02){
                        rimBoard2 = Constant.closeOutGoodsDoor;
                        rimZNum2++;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(2,(byte)0x58,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "左边柜板通信故障");
                Util.WriteFile("左边柜板通信故障");
            }
        }
    }

    private void queryCloseOutGoodsDoor1(){
        mMotorControl.query((byte)0x05,(byte)0xC0,rimZNum1);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                    if(rec[16] == (byte)0x02){
                        rimZNum1++;
                        closeOutGoodsDoor1 = true;
                        currentPackageCount1 = currentPackageCount1 + 1;
                        if(currentPackageCount1 >= packageCount1){
                            finshDone1 = true;
                        }
                        if(finshDone1 && finshDone2){
                            midBoard = Constant.openGetGoodsDoor;
                        }
                        rimBoard1 = Constant.homing;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(1,(byte)0x5A,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "右边柜板通信故障");
                Util.WriteFile("右边柜板通信故障");
            }
        }
    }

    private void queryCloseOutGoodsDoor2(){
        mMotorControl.query((byte)0x05,(byte)0xC1,rimZNum2);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                    if(rec[16] == (byte)0x02){
                        rimZNum2++;
                        closeOutGoodsDoor2 = true;
                        currentPackageCount2 = currentPackageCount2 + 1;
                        if(currentPackageCount2 >= packageCount2){
                            finshDone2 = true;
                        }
                        if(finshDone1 && finshDone2){
                            midBoard = Constant.openGetGoodsDoor;
                        }
                        rimBoard2 = Constant.homing;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(2,(byte)0x5A,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "左边柜板通信故障");
                Util.WriteFile("左边柜板通信故障");
            }
        }
    }

    private void queryHoming1(){
        mMotorControl.query((byte)0x06,(byte)0xC0,rimZNum1);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        rimZNum1++;
                        if(!finshDone1){
                            rimBoard1 = Constant.moveFloor;
                        }else{
                            rimBoard1 = Constant.wait;
                        }
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(1,(byte)0x59,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                        if(rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "右边柜板通信故障");
                Util.WriteFile("右边柜板通信故障");
            }
        }
    }

    private void queryHoming2(){
        mMotorControl.query((byte)0x06,(byte)0xC1,rimZNum2);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x59){
                    if(rec[18] == (byte)0x02){
                        rimZNum2++;
                        if(!finshDone2){
                            rimBoard2 = Constant.moveFloor;
                        }else{
                            rimBoard2 = Constant.wait;
                        }
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(2,(byte)0x59,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
            }
        }
    }

    private void queryOpenGetGoodsDoor(){
        mMotorControl.query((byte)0x07,(byte)0xE0,midZNum);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                    if(rec[16] == (byte)0x02){
                        midZNum++;
                        midBoard = Constant.wait;
                        timeStart();
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(0,(byte)0x4D,errorRec);
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
            SystemClock.sleep(delay);
            byte[] rec = serialPort485.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
                handleCommunicationError();
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
            }
        }
    }

    private void queryCloseGetGoodsDoor(){
        mMotorControl.query((byte)0x08,(byte)0xE0,midZNum);
        SystemClock.sleep(delay);
        byte[] rec = serialPort485.receiveData();
        if (rec != null && rec.length >= 5) {
            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                if(rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                    if(rec[16] == (byte)0x02){
                        midZNum++;
                        closeGetGoodsDoor = true;
                        midBoard = Constant.wait;
                        if((rec[9]&0x01) != (byte)0x01){
                            byte[] errorRec = new byte[9];
                            System.arraycopy(rec, 7, errorRec, 0, 9);
                            errorHandling(0,(byte)0x4D,errorRec);
                        }
                        OutGoodsThreadFlag = false;
                        serialPort485.close();
                        queryLastedTransaction.setComplete(1);
                        queryLastedTransaction.setError(0);
                        tDao.updateTransaction(queryLastedTransaction);
                        SystemClock.sleep(20);
                        Intent intent = new Intent();
                        intent.setAction("njust_outgoods_complete");
                        intent.putExtra("transaction_order_number", current_transaction_order_number);
                        intent.putExtra("outgoods_status", "success");
                        context.sendBroadcast(intent);
                        Log.w("happy", "本次交易完毕");Util.WriteFile("本次交易完毕");
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
//                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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
//                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
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

    private void errorHandling(int counter, byte module, byte[] rec) {
        MachineStateDao machineStateDao = new MachineStateDaoImpl(context);
        MalfunctionDao malfunctionDao = new MalfunctionDaoImpl(context);
        TransactionDao transactionDao = new TransactionDaoImpl(context);
        Malfunction malfunction = new Malfunction();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Transaction transaction = transactionDao.queryLastedTransaction();
        byte error1[];
        byte error2[];
        String Counter;
        if(counter == 0){
            Counter = "中柜";
            machineStateDao.updateState(1);
        }else{
            Counter = counter == 1? "左柜":"右柜";
            if(counter == 1){
                machineStateDao.updateCounterState(1,0);
            }else {
                machineStateDao.updateCounterState(0,1);
            }
        }
        switch (module) {
            /*字符“Y”，表示驱动Y轴电机的应答*/
            case 0x59: {
                String log = Counter + "检测到错误：";
                error1 = byteTo8Byte(rec[2]);
                if (error1[0] != (byte)0x00) {
                    log += "已执行动作，";
                } else {
                    log += "未执行动作，";
                }
                if(error1[1] == (byte)0x01){
                    log += "Y轴电机过流，";
                }
                if(error1[2] == (byte)0x01){
                    log += "Y轴电机断路，";
                }
                if(error1[3] == (byte)0x01){
                    log += "Y轴上止点开关故障，";
                }
                if(error1[4] == (byte)0x01){
                    log += "Y轴下止点开关故障，";
                }
                if(error1[5] == (byte)0x01){
                    log += "Y轴电机超时，";
                }
                if(error1[6] == (byte)0x01){
                    log += "Y轴码盘故障，";
                }
                if(error1[7] == (byte)0x01){
                    log += "Y轴出货门定位开关故障，";
                }
                log += "停止出货。";
                malfunction.setTransactionID(current_transaction_order_number);
                String time = sdf.format(Calendar.getInstance().getTime());
                malfunction.setErrorTime(time);
                malfunction.setCounter(counter);
                malfunction.setErrorModule(Counter+"Y轴电机");
                malfunction.setErrorDescription(log);
                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
                malfunctionDao.addMalfunction(malfunction);
                log += " Y轴电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
                        +" Y轴电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
                        +" Y轴电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
                Log.w("happy", ""+log);
                Util.WriteFile(log);
                break;
            }
            /*字符“P”，表示驱动货道电机的应答*/
            case 0x50: {
                String log = Counter + "检测到错误：";
                error1 = byteTo8Byte(rec[2]);
                if (error1[0] != (byte)0x00) {
                    log += "已执行动作，";
                } else {
                    log += "未执行动作，";
                }
                if(error1[1] == (byte)0x01){
                    log += "货道电机过流，";
                }
                if(error1[2] == (byte)0x01){
                    log += "货道电机断路，";
                }
                if(error1[3] == (byte)0x01){
                    log += "货道超时（无货物输出的超时），";
                }
                if(error1[4] == (byte)0x01){
                    log += "商品超时（货物遮挡光栅超时），";
                }
                if(error1[5] == (byte)0x01){
                    log += "弹簧电机1反馈开关故障，";
                }
                if(error1[6] == (byte)0x01){
                    log += "弹簧电机2反馈开关故障，";
                }
                if(error1[7] == (byte)0x01){
                    log += "货道下货光栅故障，";
                }
                log += "停止出货。";
                malfunction.setTransactionID(current_transaction_order_number);
                String time = sdf.format(Calendar.getInstance().getTime());
                malfunction.setErrorTime(time);
                malfunction.setCounter(counter);
                malfunction.setErrorModule(Counter+"货道电机");
                malfunction.setErrorDescription(log);
                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
                malfunctionDao.addMalfunction(malfunction);
                log += " 货道电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
                        +" 货道电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
                        +" 货道电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
                Log.w("happy", ""+log);
                Util.WriteFile(log);
                break;
            }
            /*字符“X”，表示驱动X轴电机的应答*/
            case 0x58: {
                String log = Counter + "检测到错误：";
                error1 = byteTo8Byte(rec[2]);
                if (error1[0] != (byte)0x00) {
                    log += "已执行动作，";
                } else {
                    log += "未执行动作，";
                }
                if(error1[1] == (byte)0x01){
                    log += "X轴电机过流，";
                }
                if(error1[2] == (byte)0x01){
                    log += "X轴电机断路，";
                }
                if(error1[3] == (byte)0x01){
                    log += "X轴出货光栅未检测到货物，";
                }
                if(error1[4] == (byte)0x01){
                    log += "X轴出货光栅货物遮挡超时，";
                }
                if(error1[5] == (byte)0x01){
                    log += "X轴出货光栅故障，";
                }
                if(error1[6] == (byte)0x01){
                    log += "X轴电机超时，";
                }
                log += "停止出货。";
                malfunction.setTransactionID(current_transaction_order_number);
                String time = sdf.format(Calendar.getInstance().getTime());
                malfunction.setErrorTime(time);
                malfunction.setCounter(counter);
                malfunction.setErrorModule(Counter+"X轴电机");
                malfunction.setErrorDescription(log);
                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
                malfunctionDao.addMalfunction(malfunction);
                log += " 货道电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
                        +" 货道电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
                        +" 货道电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
                Log.w("happy", ""+log);
                Util.WriteFile(log);
                break;
            }
            /*字符“Z”，表示驱动出货门电机的应答*/
            case 0x5A: {
                String log = Counter + "检测到错误：";
                error1 = byteTo8Byte(rec[2]);
                if (error1[0] != (byte)0x00) {
                    log += "已执行动作，";
                } else {
                    log += "未执行动作，";
                }
                if(error1[1] == (byte)0x01){
                    log += "出货门电机过流，";
                }
                if(error1[2] == (byte)0x01){
                    log += "出货门电机断路，";
                }
                if(error1[3] == (byte)0x01){
                    log += "出货门前止点开关故障，";
                }
                if(error1[4] == (byte)0x01){
                    log += "出货门后止点开关故障，";
                }
                if(error1[5] == (byte)0x01){
                    log += "开、关出货门超时，";
                }
                if(error1[6] == (byte)0x01){
                    log += "出货门半开、半关，";
                }
                log += "停止出货。";
                malfunction.setTransactionID(current_transaction_order_number);
                String time = sdf.format(Calendar.getInstance().getTime());
                malfunction.setErrorTime(time);
                malfunction.setCounter(counter);
                malfunction.setErrorModule(Counter+"出货门电机");
                malfunction.setErrorDescription(log);
                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
                malfunctionDao.addMalfunction(malfunction);
                log += " 出货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
                        +" 出货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
                        +" 出货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
                Log.w("happy", ""+log);
                Util.WriteFile(log);
                break;
            }
            /*字符“M”，表示驱动取货门电机的应答*/
            case 0x4D: {
                String log = Counter + "检测到错误：";
                error1 = byteTo8Byte(rec[2]);
                error2 = byteTo8Byte(rec[1]);
                if (error1[0] != (byte)0x00) {
                    log += "已执行动作，";
                } else {
                    log += "未执行动作，";
                }
                if(error1[1] == (byte)0x01){
                    log += "取货门电机过流，";
                }
                if(error1[2] == (byte)0x01){
                    log += "取货门电机断路，";
                }
                if(error1[3] == (byte)0x01){
                    log += "取货门上止点开关故障，";
                }
                if(error1[4] == (byte)0x01){
                    log += "取货门下止点开关故障，";
                }
                if(error1[5] == (byte)0x01){
                    log += "开、关取货门超时，";
                }
                if(error1[6] == (byte)0x01){
                    log += "取货门半开、半关，";
                }
                if(error1[7] == (byte)0x01){
                    log += "取货仓光栅检测无货物，";
                }
                if(error2[0] == (byte)0x01){
                    log += "取货仓光栅检测有货物，";
                }
                if(error2[1] == (byte)0x01){
                    log += "取货篮光栅故障，";
                }
                if(error2[2] == (byte)0x01){
                    log += "防夹手光栅检测到遮挡物，";
                }
                if(error2[3] == (byte)0x01){
                    log += "防夹手光栅故障，";
                }
                log += "停止出货。";
                malfunction.setTransactionID(current_transaction_order_number);
                String time = sdf.format(Calendar.getInstance().getTime());
                malfunction.setErrorTime(time);
                malfunction.setCounter(counter);
                malfunction.setErrorModule(Counter+"取货门电机");
                malfunction.setErrorDescription(log);
                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
                malfunctionDao.addMalfunction(malfunction);
                log += " 取货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
                        +" 取货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
                        +" 取货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
                Log.w("happy", ""+log);
                Util.WriteFile(log);
                break;
            }
            /*字符“F”，表示驱动落货门电机的应答*/
            case 0x46: {
                String log = Counter + "检测到错误：";
                error1 = byteTo8Byte(rec[2]);
                error2 = byteTo8Byte(rec[1]);
                if (error1[0] != (byte)0x00) {
                    log += "已执行动作，";
                } else {
                    log += "未执行动作，";
                }
                if(error1[1] == (byte)0x01){
                    log += "落货门电机过流，";
                }
                if(error1[2] == (byte)0x01){
                    log += "落货门电机断路，";
                }
                if(error1[3] == (byte)0x01){
                    log += "落货门上止点开关故障，";
                }
                if(error1[4] == (byte)0x01){
                    log += "落货门下止点开关故障，";
                }
                if(error1[5] == (byte)0x01){
                    log += "开、关落货门超时，";
                }
                if(error1[6] == (byte)0x01){
                    log += "落货门半开、半关，";
                }
                if(error1[7] == (byte)0x01){
                    log += "落货仓光栅检测无货物，";
                }
                if(error2[0] == (byte)0x01){
                    log += "落货仓光栅检测有货物，";
                }
                if(error2[1] == (byte)0x01){
                    log += "落货篮光栅故障，";
                }
                log += "停止出货。";
                malfunction.setTransactionID(current_transaction_order_number);
                String time = sdf.format(Calendar.getInstance().getTime());
                malfunction.setErrorTime(time);
                malfunction.setCounter(counter);
                malfunction.setErrorModule(Counter+"落货门电机");
                malfunction.setErrorDescription(log);
                malfunction.setMotorRealActionTime((rec[3]&0xff) * 256 + (rec[4]&0xff));
                malfunction.setMotorMaxElectricity((rec[5]&0xff) * 256 + (rec[6]&0xff));
                malfunction.setMotorAverageElectricity((rec[7]&0xff) * 256 + (rec[8]&0xff));
                malfunctionDao.addMalfunction(malfunction);
                log += " 落货门电机实际动作时间（毫秒）:"+ ((rec[3]&0xff) * 256 + (rec[4]&0xff))
                        +" 落货门电机最大电流（毫安）:"+ ((rec[5]&0xff) * 256 + (rec[6]&0xff))
                        +" 落货门电机平均电流（毫安）:"+ ((rec[7]&0xff) * 256 + (rec[8]&0xff));
                Log.w("happy", ""+log);
                Util.WriteFile(log);
                break;
            }
            default:break;
        }
        transaction.setComplete(1);
        transaction.setError(1);
        transactionDao.updateTransaction(transaction);
        OutGoodsThreadFlag = false;
        SystemClock.sleep(20);
        Intent intent = new Intent();
        intent.setAction("njust_outgoods_complete");
        intent.putExtra("transaction_order_number", current_transaction_order_number);
        intent.putExtra("outgoods_status", "fail");
        context.sendBroadcast(intent);
    }
    /**
     * 将byte转换为一个长度为8的byte数组，数组每个值代表bit
     * @param b 1个字节byte数据
     * */
    private static byte[] byteTo8Byte(byte b) {
        byte[] array = new byte[8];
        for (int i = 0; i <= 7; i++) {
            array[i] = (byte)(b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }
}
