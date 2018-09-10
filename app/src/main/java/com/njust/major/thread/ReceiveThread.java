package com.njust.major.thread;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.njust.SerialPort;
import com.njust.major.SCM.MotorControl;
import com.njust.major.bean.Malfunction;
import com.njust.major.bean.Transaction;
import com.njust.major.dao.FoodDao;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.MalfunctionDao;
import com.njust.major.dao.TransactionDao;
import com.njust.major.dao.impl.FoodDaoImpl;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.dao.impl.MalfunctionDaoImpl;
import com.njust.major.dao.impl.TransactionDaoImpl;
import com.njust.major.error.errorHandling;
import com.njust.major.util.Util;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import static com.njust.VMApplication.ReceiveThreadFlag;
import static com.njust.VMApplication.SendThreadFlag;
import static com.njust.VMApplication.leftGoodsRecZhenNumber;
import static com.njust.VMApplication.leftRimRecZhenNumber;
import static com.njust.VMApplication.leftZhenNumber;
import static com.njust.VMApplication.midRecZhenNumber;
import static com.njust.VMApplication.rightGoodsRecZhenNumber;
import static com.njust.VMApplication.rightRimRecZhenNumber;
import static com.njust.VMApplication.rightZhenNumber;
import static com.njust.VMApplication.midZhenNumber;



public class ReceiveThread extends Thread {
    private Context context;
    private ReceiveThreadAssist mR2;

    SerialPort serialPort485;
    MotorControl mMotorControl;
    //发送相关标志位
    boolean rimConfirmOrderFlag1;
    boolean goodsConfirmOrderFlag1;
    boolean midConfirmOrderFlag;
    boolean moveFloorFlag1;
    boolean moveFloorFlag2;
    boolean pushGoodsFlag1;
    boolean pushGoodsFlag2;
    boolean outGoodsFlag1;
    boolean outGoodsFlag2;
    boolean homingFlag1;
    boolean homingFlag2;
    boolean getGoodsFlag;
    boolean closeDoorFlag;
    boolean dropGoodsFlag;
    boolean leftFinishFlag;
    boolean allFinishFlag;

    int currentOutCount1 = 0;//存放左右柜当前出货的序号
    int currentPackageCount1 = 0;//存放左右柜当前出货的包号
    int[][] outGoods1;
    //接收指令判断
    private static final int ReceiveMoveFloor1 = 1;
    private static final int ReceiveMoveFloor2 = 2;
    private static final int ReceivePushGoods1 = 3;
    private static final int ReceivePushGoods2 = 4;
    private static final int ReceiveOutGoods1 = 5;
    private static final int ReceiveOutGoods2 = 6;
    private static final int ReceiveHoming1 = 7;
    private static final int ReceiveHoming2 = 8;
    private static final int ReceiveGetGoods = 9;
    private static final int ReceiveCloseDoor = 10;
    private static final int ReceiveDropGoods = 11;
    private int ReceiveDropGoodsTimes = 0;
    //重发机制相关标志位
    Timer mTimer = new Timer();
    int leftPhases = 0;
    int midPhases = 0;
    int leftTimeNumber = 0;
    int midTimeNumber = 0;
    boolean leftOneSecFlag = false;
    boolean midOneSecFlag = false;
    boolean leftTimerErrorFlag = false;
    boolean midTimerErrorFlag = false;
    private int leftReMissionTime = 0;
    private int midReMissionTime = 0;
    private byte[] rec;

    public ReceiveThread(Context context, ReceiveThreadAssist receiveThreadAssist){
        super();
        this.mR2 = receiveThreadAssist;
        this.context = context;
        serialPort485 = new SerialPort(1, 38400, 8, 'n', 1);
        mMotorControl = new MotorControl(serialPort485,context);
    }

    public void initReceiveThread() {
        moveFloorFlag1 = true;
        moveFloorFlag2 = true;
        pushGoodsFlag1 = false;
        pushGoodsFlag2 = false;
        outGoodsFlag1 = false;
        outGoodsFlag2 = false;
        homingFlag1 = false;
        homingFlag2 = false;
        getGoodsFlag = false;
        closeDoorFlag = false;
        dropGoodsFlag = false;
        leftFinishFlag = false;
        allFinishFlag = false;

        rimConfirmOrderFlag1 = false;
        goodsConfirmOrderFlag1 = false;
        midConfirmOrderFlag = false;

        timeStart();
        Log.w("happy", "接收线程初始化，各标志位置位，开启重发定时器");
        Util.WriteFile("接收线程初始化，各标志位置位，开启重发定时器");
    }

    @Override
    public void run() {
        super.run();
        while(ReceiveThreadFlag){
            int onceRecNumber = 1;
            byte[] rec_original = serialPort485.receiveData();
            if(rec_original != null && rec_original.length >= 5) {
                StringBuilder str2 = new StringBuilder();
                for (byte aRec1 : rec_original) {
                    str2.append(Integer.toHexString(aRec1&0xFF)).append(" ");
                }
                Log.w("happy", "485收到原始串口："+ str2);
                /*如果指令前有乱码，掐头。如果指令后有乱码，去尾*/
                if (rec_original[0] != (byte) 0xE2 || rec_original[rec_original.length - 2] != (byte) 0xF1) {
                    boolean head = true;
                    boolean trail = true;
                    int start = 0;
                    int end = 0;
                    for (int y = 0; y < rec_original.length; y++) {
                        if (head) {
                            if (rec_original[y] == 0xE2) {
                                head = false;
                                start = y;
                            }
                        }
                        if (trail) {
                            if (rec_original[rec_original.length - 1 - y] == 0xF1) {
                                trail = false;
                                end = y - 1;
                            }
                        }
                    }
                    rec = new byte[rec_original.length - end - start];
                    System.arraycopy(rec_original, start, rec, 0, rec_original.length - end - start);
                }else{
                    rec = rec_original;
                }
            }
            if(rec != null && rec.length >= 4 && (rec[0] != (byte)0xE2 || rec[rec.length - 2] != (byte) 0xF1)){
                rec = null;
            }
            if(rec != null && rec.length >= 5){
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "485收到串口："+ str1);
                /*如果有两个串口指令前后衔接，合并接收到就拆分后运行两遍*/
                byte[][] recReal = new byte[3][];
                if(rec.length > (rec[1]&0xFF)){
                    onceRecNumber = 2;
                    recReal[0] = new byte[rec[1]&0xFF];
                    recReal[1] = new byte[rec.length - rec[1]&0xFF];
                    System.arraycopy(rec, 0, recReal[0], 0, rec[1]&0xFF);
                    System.arraycopy(rec, rec[1]&0xFF, recReal[1], 0, rec.length - rec[1]&0xFF);
                }else{
                    onceRecNumber = 1;
                    recReal[0] = new byte[rec[1]&0xFF];
                    System.arraycopy(rec, 0, recReal[0], 0, rec[1]&0xFF);
                }
                rec = null;
//                Log.w("happy", ""+ Arrays.deepToString(recReal));
                for(int i = 0; i < onceRecNumber; i++){
                    switch (analyticReceive(recReal[i])){
                        case ReceiveMoveFloor1:{
                                if(recReal[i][7] == (byte)0x59 && recReal[i][9] == (byte)0x01){
                                    Log.w("happy", "收到左柜移层上行——Y轴");Util.WriteFile("收到左柜移层上行——Y轴");
                                    rimConfirmOrderFlag1 = true;
                                    try {
                                        Thread.sleep(150);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    pushGoodsFlag1 = true;
                                }else if(recReal[i][7] == (byte)0x59 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                    rimConfirmOrderFlag1 = true;
                                    //报错，停止
                                    byte[] errorRec = new byte[9];
                                    System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                    new errorHandling(context,1,(byte)0x59,errorRec);
                                }
                                if(recReal[i][7] == (byte)0x58 && recReal[i][9] == (byte)0x01){
                                    Log.w("happy", "收到左柜移层上行——X轴");Util.WriteFile("收到左柜移层上行——X轴");
                                    rimConfirmOrderFlag1 = true;
                                }else if(recReal[i][7] == (byte)0x58 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                    rimConfirmOrderFlag1 = true;
                                    //报错，停止
                                    byte[] errorRec = new byte[9];
                                    System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                    new errorHandling(context,1,(byte)0x58,errorRec);
                                }
                            break;
                        }
                        case ReceivePushGoods1:{
                            if(recReal[i][7] == (byte)0x50 && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到左柜推货上行");Util.WriteFile("收到左柜推货上行");
                                goodsConfirmOrderFlag1 = true;
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(currentOutCount1 == 0){
                                    outGoodsFlag1 = true;
                                }else{
                                    moveFloorFlag1 = true;
                                }

                            }else if(recReal[i][7] == (byte)0x50 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                goodsConfirmOrderFlag1 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,1,(byte)0x50,errorRec);
                            }
                            break;
                        }
                        case ReceiveOutGoods1:{
                            if(recReal[i][7] == (byte)0x59 && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到左柜出货上行——Y轴");Util.WriteFile("收到左柜出货上行——Y轴");
                                rimConfirmOrderFlag1 = true;
                            }else if(recReal[i][7] == (byte)0x59 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag1 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,1,(byte)0x59,errorRec);
                            }
                            if(recReal[i][7] == (byte)0x58 && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到左柜出货上行——X轴");Util.WriteFile("收到左柜出货上行——X轴");
                                rimConfirmOrderFlag1 = true;
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                homingFlag1 = true;
                            }else if(recReal[i][7] == (byte)0x58 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag1 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,1,(byte)0x58,errorRec);
                            }
                            if(recReal[i][7] == (byte)0x5A && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到左柜出货上行——出货门开");Util.WriteFile("收到左柜出货上行——出货门开");
                                rimConfirmOrderFlag1 = true;
                            }else if(recReal[i][7] == (byte)0x5A && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag1 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,1,(byte)0x5A,errorRec);
                            }
                            break;
                        }
                        case ReceiveHoming1:{
                            if(recReal[i][7] == (byte)0x5A && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到左柜归位上行——出货门关");Util.WriteFile("收到左柜归位上行——出货门关");
                                rimConfirmOrderFlag1 = true;
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(leftFinishFlag){
                                    getGoodsFlag = true;
                                }
                            }else if(recReal[i][7] == (byte)0x5A && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag1 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,1,(byte)0x5A,errorRec);
                            }
                            if(recReal[i][7] == (byte)0x59 && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到左柜归位上行——Y轴");Util.WriteFile("收到左柜归位上行——Y轴");
                                rimConfirmOrderFlag1 = true;
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(!leftFinishFlag){
                                    moveFloorFlag1 = true;
                                }
                            }else if(recReal[i][7] == (byte)0x59 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag1 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,1,(byte)0x59,errorRec);
                            }
                            break;
                        }
                        case ReceiveGetGoods:{
                            if(recReal[i][7] == (byte)0x4D && /*recReal[i][8] == (byte)0x00 &&*/ (recReal[i][9]&0x01) == (byte)0x01){
                                Log.w("happy", "收到中柜取货上行");Util.WriteFile("收到中柜取货上行");
                                midConfirmOrderFlag = true;
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                closeDoorFlag = true;
                            }else/* if(recReal[i][7] == (byte)0x4D && (recReal[i][8] != (byte)0x00 || (recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00)))*/{
                                midConfirmOrderFlag = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,0,(byte)0x4D,errorRec);
                            }
                            break;
                        }
                        case ReceiveCloseDoor:{
                            if(recReal[i][7] == (byte)0x4D && /*recReal[i][8] == (byte)0x00 &&*/ (recReal[i][9]&0x01) == (byte)0x01){
                                Log.w("happy", "收到中柜关门上行");Util.WriteFile("收到中柜关门上行");
                                midConfirmOrderFlag = true;
                                try {
                                    Thread.sleep(150);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                dropGoodsFlag = true;
                            }else /*if(recReal[i][7] == (byte)0x4D && (recReal[i][8] != (byte)0x00 || (recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00)))*/{
                                midConfirmOrderFlag = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,0,(byte)0x4D,errorRec);
                            }
                            break;
                        }
                        case ReceiveDropGoods:{
                            if(recReal[i][7] == (byte)0x46 && /*recReal[i][8] == (byte)0x00 &&*/ (recReal[i][9]&0x01) == (byte)0x01){
                                ReceiveDropGoodsTimes++;
                                if(ReceiveDropGoodsTimes >= 2){
                                    Log.w("happy", "收到中柜掉货上行——关门");Util.WriteFile("收到中柜掉货上行——关门");
                                    //广播告诉上位机出货完毕
                                    allFinishFlag = true;
                                    //一整个交易流程结束，关闭接收线程
                                }else{
                                    Log.w("happy", "收到中柜掉货上行——开门");Util.WriteFile("收到中柜掉货上行——开门");
                                }
                                midConfirmOrderFlag = true;
                            }else /*if(recReal[i][7] == (byte)0x4D && (recReal[i][8] != (byte)0x00 || (recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00)))*/{
                                midConfirmOrderFlag = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,0,(byte)0x46,errorRec);
                            }
                        }
                        default:break;
                    }
                }
            }
            if(leftZhenNumber >= 256){leftZhenNumber = 0;}
            if(rightZhenNumber >= 256){rightZhenNumber = 0;}
            if(midZhenNumber >= 256){midZhenNumber = 0;}
        }
    }


    private int analyticReceive(byte[] rec){
        int length = rec.length;
        int recid = 0;
        if(length < 9) return recid;
        if(rec[0] == (byte)0xE2 && rec[1] == length && rec[2] == 0x00 && rec[4] == (byte)0x60 && rec[length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
            if(rec[6] == (byte)0x6C && rec[3] == (byte)0xC0 && rec[5] != leftRimRecZhenNumber) {leftRimRecZhenNumber = rec[5];recid = ReceiveMoveFloor1;}
            if(rec[6] == (byte)0x6C && rec[3] == (byte)0xC1 && rec[5] != rightRimRecZhenNumber) {rightRimRecZhenNumber = rec[5];recid = ReceiveMoveFloor2;}
            if(rec[6] == (byte)0x70 && rec[3] == (byte)0x80 && rec[5] != leftGoodsRecZhenNumber) {leftGoodsRecZhenNumber = rec[5];recid = ReceivePushGoods1;}
            if(rec[6] == (byte)0x70 && rec[3] == (byte)0x81 && rec[5] != rightGoodsRecZhenNumber) {rightGoodsRecZhenNumber = rec[5];recid = ReceivePushGoods2;}
            if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC0 && rec[5] != leftRimRecZhenNumber) {leftRimRecZhenNumber = rec[5];recid = ReceiveOutGoods1;}
            if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC1 && rec[5] != rightRimRecZhenNumber) {rightRimRecZhenNumber = rec[5];recid = ReceiveOutGoods2;}
            if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC0 && rec[5] != leftRimRecZhenNumber) {leftRimRecZhenNumber = rec[5];recid = ReceiveHoming1;}
            if(rec[6] == (byte)0x72 && rec[3] == (byte)0xC1 && rec[5] != rightRimRecZhenNumber) {rightRimRecZhenNumber = rec[5];recid = ReceiveHoming2;}
            if(rec[6] == (byte)0x74 && rec[3] == (byte)0xE0 && rec[5] != midRecZhenNumber) {midRecZhenNumber = rec[5];recid = ReceiveGetGoods;}
            if(rec[6] == (byte)0x63 && rec[3] == (byte)0xE0 && rec[5] != midRecZhenNumber) {midRecZhenNumber = rec[5];recid = ReceiveCloseDoor;}
            if(rec[6] == (byte)0x66 && rec[3] == (byte)0xE0 && rec[5] != midRecZhenNumber) {midRecZhenNumber = rec[5];recid = ReceiveDropGoods;}
        }
        if(rec[0] == (byte)0xE2 && rec[1] == length && rec[2] == 0x00 && rec[4] == (byte)0x80 && rec[length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
            if(/*rec[8] == 0x01 &&*/ (rec[3] == (byte)0xC0 || rec[3] == (byte)0x80) && (rec[6] == (byte)0x01 || rec[6] == (byte)0x02 || rec[6] == (byte)0x03 || rec[6] == (byte)0x05)){
                leftOneSecFlag = false;leftReMissionTime = 0; leftZhenNumber++;leftPhases = 0;
                Log.w("happy", "左柜收到确认指令");
                Util.WriteFile("左柜收到确认指令");
            }
            if(/*rec[8] == 0x01 &&*/ rec[3] == (byte)0xE0 && (rec[6] == (byte)0x04 || rec[6] == (byte)0x06 || rec[6] == (byte)0x07)){
                midOneSecFlag = false;midReMissionTime = 0; midZhenNumber++;midPhases = 0;
                Log.w("happy", "中柜收到确认指令");
                Util.WriteFile("中柜收到确认指令");
            }
        }
        return recid;
    }


    private void timeStart(){
        TimerTask timerTaskLeft = new TimerTask() {
            @Override
            public void run() {
                leftTimeNumber++;
                if (leftTimeNumber == 5 && leftOneSecFlag) {
                    if (leftReMissionTime < 5) {
                        leftReMissionTime++;
                        Log.w("happy", "定时器到时重发开启");
                        switch (leftPhases) {
                            case 1:
                                moveFloorFlag1 = true;
                                break;
                            case 2:
                                currentOutCount1 = currentOutCount1 - 1;
                                if(currentOutCount1 < 0 ){
                                    if(outGoods1[currentPackageCount1][6] == 0){
                                        if(outGoods1[currentPackageCount1][3] == 0){
                                            currentOutCount1 = 0;
                                        }else {
                                            currentOutCount1 = 1;
                                        }
                                    }
                                    else if(outGoods1[currentPackageCount1][6] != 0){
                                        currentOutCount1 = 2;
                                    }
                                }
                                pushGoodsFlag1 = true;
                                break;
                            case 3:
                                currentPackageCount1 = currentPackageCount1 - 1;
                                if(currentPackageCount1 < 0 ){
                                    currentPackageCount1 = SendThread.packageCount1 - 1;
                                }
                                outGoodsFlag1 = true;
                                break;
                            case 4:
                                homingFlag1 = true;
                                break;
                            default:break;
                        }
                    } else {
                        leftTimerErrorFlag = true;
                        leftFinishFlag = true;
                        Log.w("happy", "左柜通信故障");
                        Util.WriteFile("左柜通信故障");
                        //报错，停止左柜交易，
                    }
                }
                if (leftTimeNumber >= 10000) {
                    leftTimeNumber = 0;
                }
            }
        };
            mTimer.schedule(timerTaskLeft, 0, 200);

        TimerTask timerTaskMid = new TimerTask() {
            @Override
            public void run() {
                midTimeNumber++;
                if (midTimeNumber == 5 && midOneSecFlag) {
                    if (midReMissionTime < 5) {
                        midReMissionTime++;
                        switch (midPhases) {
                            case 1:
                                getGoodsFlag = true;
                                mR2.getGoodsFlag = true;
                                break;
                            case 2:
                                closeDoorFlag = true;
                                break;
                            case 3:
                                dropGoodsFlag = true;
                                break;
                            default:break;
                        }
                    } else {
                        midTimerErrorFlag = true;
                        Log.w("happy", "中柜通信故障");
                        Util.WriteFile("中柜通信故障");
                    }
                }
                if (midTimeNumber >= 10000) {
                    midTimeNumber = 0;
                }
            }
        };
            mTimer.schedule(timerTaskMid, 0, 200);

    }

    /**
     * 校验和验证
     * 说明：验证接收到的数据是否正确
     * @param rec 接收到的串口数据
     * */
    private static boolean isVerify(byte[] rec){
        int sum = 0;
        for (int i = 0; i < rec.length - 1; i++) {
            sum = sum + rec[i];
        }
        return sum == rec[rec.length-1];
    }

    /**
     * 将byte转换为一个长度为8的byte数组，数组每个值代表bit
     * @param b 1个字节byte数据
     * */
    public static byte[] byteTo8Byte(byte b) {
        byte[] array = new byte[8];
        for (int i = 0; i <= 7; i++) {
            array[i] = (byte)(b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }
}
