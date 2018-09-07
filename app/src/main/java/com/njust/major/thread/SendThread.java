package com.njust.major.thread;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.njust.major.SCM.MotorControl;
import com.njust.major.bean.Foodstuff;
import com.njust.major.bean.MachineState;
import com.njust.major.bean.Position;
import com.njust.major.bean.Transaction;
import com.njust.major.dao.FoodDao;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.PositionDao;
import com.njust.major.dao.TransactionDao;
import com.njust.major.dao.impl.FoodDaoImpl;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.dao.impl.PositionDaoImpl;
import com.njust.major.dao.impl.TransactionDaoImpl;
import com.njust.major.error.errorHandling;
import com.njust.major.util.Util;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import static com.njust.VMApplication.ReceiveThreadFlag;
import static com.njust.VMApplication.SendThreadFlag;
import static com.njust.VMApplication.leftZhenNumber;
import static com.njust.VMApplication.rightZhenNumber;
import static com.njust.VMApplication.midZhenNumber;
import static com.njust.VMApplication.current_transaction_order_number;


public class SendThread extends Thread {
    private Context context;
    private FoodDao fDao;
    private PositionDao pDao;
    private TransactionDao tDao;
    private MachineStateDao mDao;
    private Transaction queryLastedTransaction;
    private MotorControl mMotorControl;
    private MotorControl mMotorControl2;
    private ReceiveThread mR;
    private ReceiveThreadAssist mR2;
    static int packageCount1 = 0;//存放左右柜分别打包的个数,每三个凑成一个包，完成一次轮询出货
    static int packageCount2 = 0;
    private int moveTime = 1200;






    public SendThread(Context context, ReceiveThread receiveThread, ReceiveThreadAssist receiveThreadAssist) {
        super();
        this.context = context;
        this.mR = receiveThread;
        this.mR2 = receiveThreadAssist;
        mMotorControl = receiveThread.mMotorControl;
        mMotorControl2 = receiveThreadAssist.mMotorControl;
        fDao = new FoodDaoImpl(context);
        pDao = new PositionDaoImpl(context);
        tDao = new TransactionDaoImpl(context);
        mDao = new MachineStateDaoImpl(context);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }



    public void initSendThread() {
        queryLastedTransaction = tDao.queryLastedTransaction();
        Log.w("happy", queryLastedTransaction.toString());
        String [] str = queryLastedTransaction.getFoodIDs().split(" ");
        Log.w("happy", String.valueOf(str));
        int[] foodIDs = new int[str.length];
        List<Integer> counterSet1 = new ArrayList<Integer>();//存储counter1要取的货的positionID
        List<Integer> counterSet2 = new ArrayList<Integer>();//存储counter2要取的货的positionID
        for (int i = 0; i < str.length; i++){
            foodIDs[i] = Integer.parseInt(str[i]);
        }
        Arrays.sort(foodIDs);
        /*根据购物清单，随机查找还有库存并且可以交易的商品，分成左右柜两个列表存储起来*/
        for (int foodID : foodIDs){
            List<Foodstuff> foodstuffList = fDao.queryByFoodID(foodID);
            for (Foodstuff foodstuff : foodstuffList){
                if (foodstuff.getStock() != 0 && foodstuff.getState() == 1){
                    if (foodstuff.getCounter() == 1){
                        counterSet1.add(foodstuff.getPositionID());
                        fDao.updatePositionIDStock(1,foodstuff.getPositionID());
                        break;
                    } else if (foodstuff.getCounter() == 2){
                        counterSet2.add(foodstuff.getPositionID());
                        fDao.updatePositionIDStock(2,foodstuff.getPositionID());
                        break;
                    }
                }
            }
        }

        int[] counter1 = new int[counterSet1.size()];
        int[] counter2 = new int[counterSet2.size()];
        int tmp = 0;
        for (Integer i : counterSet1){
            counter1[tmp++] = i;
        }
        tmp = 0;
        for (Integer i : counterSet2){
            counter2[tmp++] = i;
        }
        Arrays.sort(counter1);
        Arrays.sort(counter2);

        //每三件一个包
        packageCount1 = (counterSet1.size() % 3 == 0 ? (counterSet1.size() / 3): (counterSet1.size() / 3 + 1));
        packageCount2 = (counterSet2.size() % 3 == 0 ? (counterSet2.size() / 3): (counterSet2.size() / 3 + 1));

        mR.outGoods1 = calculateOrientationTime(1, counter1,packageCount1);
        mR2.outGoods2 = calculateOrientationTime(2, counter2,packageCount2);


        Log.w("happy", ""+ Arrays.deepToString(mR.outGoods1));
        Log.w("happy", ""+ Arrays.deepToString(mR2.outGoods2));
        Util.WriteFile("outGoods1："+Arrays.deepToString(mR.outGoods1));
        Util.WriteFile("outGoods2："+Arrays.deepToString(mR2.outGoods2));
        Log.w("happy", "发送线程初始化，计算出货数组完毕");
        Util.WriteFile("发送线程初始化，计算出货数组完毕");
    }

    @Override
    public void run() {
        super.run();
        while (SendThreadFlag){
            if(mR.rimConfirmOrderFlag1) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mR.rimConfirmOrderFlag1 = false;mMotorControl.confirmOrder(1, (byte) 0xC0, leftZhenNumber);Log.w("happy", "给左边柜板发送确认指令");Util.WriteFile("给左边柜板发送确认指令");
                leftZhenNumber++;
            }
            if(mR2.rimConfirmOrderFlag2) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mR2.rimConfirmOrderFlag2 = false;mMotorControl2.confirmOrder(1, (byte) 0xC1, rightZhenNumber);Log.w("happy", "给右边柜板发送确认指令");Util.WriteFile("给右边柜板发送确认指令");
                rightZhenNumber++;
            }
            if(mR.goodsConfirmOrderFlag1) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mR.goodsConfirmOrderFlag1 = false;mMotorControl.confirmOrder(1, (byte) 0x80, leftZhenNumber);Log.w("happy", "给左货道板发送确认指令");Util.WriteFile("给左货道板发送确认指令");
                leftZhenNumber++;
            }
            if(mR2.goodsConfirmOrderFlag2) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mR2.goodsConfirmOrderFlag2 = false;mMotorControl2.confirmOrder(1, (byte) 0x81, rightZhenNumber);Log.w("happy", "给右货道板发送确认指令");Util.WriteFile("给右货道板发送确认指令");
                rightZhenNumber++;
            }
            if(mR.midConfirmOrderFlag) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mR.midConfirmOrderFlag = false;
                mMotorControl.confirmOrder(1, (byte) 0xE0, midZhenNumber);
                midZhenNumber++;
                Log.w("happy", "给中柜板发送确认指令");Util.WriteFile("给中柜板发送确认指令");
                if(mR.allFinishFlag){
                    mR.mTimer.cancel();
                    mR2.mTimer.cancel();
                    ReceiveThreadFlag = false;
                    SendThreadFlag = false;
                    mR.serialPort485.close();
                    mR2.serialPort232.close();
                    queryLastedTransaction.setComplete(1);
                    queryLastedTransaction.setError(0);
                    tDao.updateTransaction(queryLastedTransaction);
                    Intent intent = new Intent();
                    intent.setAction("njust_outgoods_complete");
                    intent.putExtra("transaction_order_number", current_transaction_order_number);
                    intent.putExtra("outgoods_status", "success");
                    context.sendBroadcast(intent);
                    Log.w("happy", "本次交易完毕");Util.WriteFile("本次交易完毕");
                }
            }
            if(packageCount1 > 0){
                if(mR.moveFloorFlag1){
                    Log.w("happy", "左柜 "+(mR.currentPackageCount1+1)+"-"+(mR.currentOutCount1+1)+" 移层");
                    Util.WriteFile("左柜 "+(mR.currentPackageCount1+1)+"-"+(mR.currentOutCount1+1)+" 移层");
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mMotorControl.moveFloor(1,leftZhenNumber,mR.outGoods1[mR.currentPackageCount1][mR.currentOutCount1 * 3],mR.outGoods1[mR.currentPackageCount1][mR.currentOutCount1 * 3+1],mR.outGoods1[mR.currentPackageCount1][mR.currentOutCount1 * 3+2]);
                    mR.moveFloorFlag1 = false;
                    mR.leftPhases = 1;mR.leftOneSecFlag = true;mR.leftTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零
                }
                if(mR.pushGoodsFlag1){
                    Log.w("happy", "左柜 "+(mR.currentPackageCount1+1)+"-"+(mR.currentOutCount1+1)+" 推货");
                    Util.WriteFile("左柜 "+(mR.currentPackageCount1+1)+"-"+(mR.currentOutCount1+1)+" 推货");
                    mMotorControl.pushGoods(1,leftZhenNumber,mR.outGoods1[mR.currentPackageCount1][mR.currentOutCount1 * 3]);
                    mR.currentOutCount1 = mR.currentOutCount1 + 1;
                    if(mR.currentOutCount1 == 3 || mR.outGoods1[mR.currentPackageCount1][mR.currentOutCount1*3] == 0){
                        mR.currentOutCount1 = 0;
                    }
                    mR.pushGoodsFlag1 = false;
                    mR.leftPhases = 2;mR.leftOneSecFlag = true;mR.leftTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零

                }
                if(mR.outGoodsFlag1){
                    Log.w("happy", "左柜 "+(mR.currentPackageCount1+1)+"包 出货");
                    Util.WriteFile("左柜 "+(mR.currentPackageCount1+1)+"包 出货");
                    mMotorControl.outGoods(1,leftZhenNumber);
                    mR.currentPackageCount1 = mR.currentPackageCount1 + 1;
                    if(mR.currentPackageCount1 >= packageCount1){
                        mR.currentPackageCount1 = 0;
                        mR.leftFinishFlag = true;
                    }
                    mR.outGoodsFlag1 = false;
                    mR.leftPhases = 3;mR.leftOneSecFlag = true;mR.leftTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零

                }
                if(mR.homingFlag1){
                    Log.w("happy", "左柜 归位");
                    Util.WriteFile("左柜 归位");
                    mMotorControl.homing(1,leftZhenNumber);
                    mR.homingFlag1 = false;
                    mR.leftPhases = 4;mR.leftOneSecFlag = true;mR.leftTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零
                }
            }else if(packageCount1 == 0){
                mR.leftFinishFlag = true;
                mR.getGoodsFlag = true;
            }
            if(packageCount2 > 0){
                if(mR2.moveFloorFlag2){
                    Log.w("happy", "右柜 "+(mR2.currentPackageCount2+1)+"-"+(mR2.currentOutCount2+1)+" 移层");
                    Util.WriteFile("右柜 "+(mR2.currentPackageCount2+1)+"-"+(mR2.currentOutCount2+1)+" 移层");
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    mMotorControl2.moveFloor(2,rightZhenNumber,mR2.outGoods2[mR2.currentPackageCount2][mR2.currentOutCount2 * 3],mR2.outGoods2[mR2.currentPackageCount2][mR2.currentOutCount2 * 3+1],mR2.outGoods2[mR2.currentPackageCount2][mR2.currentOutCount2 * 3+2]);
                    mR2.moveFloorFlag2 = false;
                    mR2.rightPhases = 1;mR2.rightOneSecFlag = true;mR2.rightTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零

                }
                if(mR2.pushGoodsFlag2){
                    Log.w("happy", "右柜 "+(mR2.currentPackageCount2+1)+"-"+(mR2.currentOutCount2+1)+" 推货");
                    Util.WriteFile("右柜 "+(mR2.currentPackageCount2+1)+"-"+(mR2.currentOutCount2+1)+" 推货");
                    mMotorControl2.pushGoods(2,rightZhenNumber,mR2.outGoods2[mR2.currentPackageCount2][mR2.currentOutCount2 * 3]);
                    mR2.currentOutCount2 = mR2.currentOutCount2 + 1;
                    if(mR2.currentOutCount2 == 3 || mR2.outGoods2[mR2.currentPackageCount2][mR2.currentOutCount2 * 3] == 0){
                        mR2.currentOutCount2 = 0;
                    }
                    mR2.pushGoodsFlag2 = false;
                    mR2.rightPhases = 2;mR2.rightOneSecFlag = true;mR2.rightTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零

                }
                if(mR2.outGoodsFlag2){
                    Log.w("happy", "右柜 "+(mR2.currentPackageCount2+1)+"包 出货");
                    Util.WriteFile("右柜 "+(mR2.currentPackageCount2+1)+"包 出货");
                    mMotorControl2.outGoods(2,rightZhenNumber);
                    mR2.currentPackageCount2 = mR2.currentPackageCount2 + 1;
                    if(mR2.currentPackageCount2 >= packageCount2){
                        mR2.currentPackageCount2 = 0;
                        mR2.rightFinishFlag = true;
                    }
                    mR2.outGoodsFlag2 = false;
                    mR2.rightPhases = 3;mR2.rightOneSecFlag = true;mR2.rightTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零

                }
                if(mR2.homingFlag2){
                    Log.w("happy", "右柜 归位");
                    Util.WriteFile("右柜 归位");
                    mMotorControl2.homing(2,rightZhenNumber);
                    mR2.homingFlag2 = false;
                    mR2.rightPhases = 4;mR2.rightOneSecFlag = true;mR2.rightTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零

                }
            }else if(packageCount2 == 0){
                mR2.rightFinishFlag = true;
                mR2.getGoodsFlag = true;
            }
            if(mR.getGoodsFlag && mR2.getGoodsFlag){
                    Log.w("happy", "中柜 取货");
                    Util.WriteFile("中柜 取货");
                    mMotorControl.getGoods(midZhenNumber);
                    mR.getGoodsFlag = false;
                    mR2.getGoodsFlag = false;
                    mR.midPhases = 1;mR.midOneSecFlag = true;mR.midTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零
            }
            if(mR.closeDoorFlag){
                Log.w("happy", "中柜 关门");
                Util.WriteFile("中柜 关门");
                try {
                    Thread.currentThread();
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mMotorControl.closeDoor(midZhenNumber);
                mR.closeDoorFlag = false;
                mR.midPhases = 2;mR.midOneSecFlag = true;mR.midTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零

            }
            if(mR.dropGoodsFlag){
                Log.w("happy", "中柜 落货");
                Util.WriteFile("中柜 落货");
                mMotorControl.dropGoods(midZhenNumber);
                mR.dropGoodsFlag = false;
                mR.midPhases = 3;mR.midOneSecFlag = true;mR.midTimeNumber = 0;//告诉定时器运行阶段，开启定时重发的标志位，定时器计数置零

            }
            if(leftZhenNumber >= 256){leftZhenNumber = 0;}
            if(rightZhenNumber >= 256){rightZhenNumber = 0;}
            if(midZhenNumber >= 256){midZhenNumber = 0;}

        }
    }

    private int[][] calculateOrientationTime(int counter, int[] set ,int packageCount){
        MachineState queryMachineState = mDao.queryMachineState();
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
                    Map<Integer, Integer> map = new TreeMap<Integer, Integer>();
                    map.put(0, where[0]);
                    map.put(1, where[1]);
                    map.put(2, where[2]);
                    List<Map.Entry<Integer,Integer>> list = new ArrayList<Map.Entry<Integer,Integer>>(map.entrySet());
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
}
