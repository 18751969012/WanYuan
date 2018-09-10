package com.njust.major.thread;


import android.content.Context;
import android.util.Log;
import com.njust.SerialPort;
import com.njust.major.SCM.MotorControl;
import com.njust.major.util.Util;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;
import com.njust.major.error.errorHandling;


import static com.njust.VMApplication.ReceiveThreadFlag;
import static com.njust.VMApplication.leftGoodsRecZhenNumber;
import static com.njust.VMApplication.leftRimRecZhenNumber;
import static com.njust.VMApplication.leftZhenNumber;
import static com.njust.VMApplication.midRecZhenNumber;
import static com.njust.VMApplication.midZhenNumber;
import static com.njust.VMApplication.rightGoodsRecZhenNumber;
import static com.njust.VMApplication.rightRimRecZhenNumber;
import static com.njust.VMApplication.rightZhenNumber;


public class ReceiveThreadAssist extends Thread {
    private Context context;
    SerialPort serialPort232;
    MotorControl mMotorControl;
    //发送相关标志位
    boolean rimConfirmOrderFlag2;
    boolean goodsConfirmOrderFlag2;
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
    boolean rightFinishFlag;

    int currentOutCount2 = 0;//存放左右柜当前出货的序号
    int currentPackageCount2 = 0;//存放左右柜当前出货的包号
    int[][] outGoods2;
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
    //重发机制相关标志位
    Timer mTimer = new Timer();
    int rightPhases = 0;
    int rightTimeNumber = 0;
    boolean rightOneSecFlag = false;
    boolean rightTimerErrorFlag = false;
    private int rightReMissionTime = 0;
    private byte[] rec;



    public ReceiveThreadAssist(Context context){
        super();
        this.context = context;
        serialPort232 = new SerialPort(3, 19200, 8, 'n', 1);
        mMotorControl = new MotorControl(serialPort232,context);
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
        rightFinishFlag = false;

        rimConfirmOrderFlag2 = false;
        goodsConfirmOrderFlag2 = false;

        timeStart();
        Log.w("happy", "接收线程初始化，各标志位置位，开启重发定时器");
        Util.WriteFile("接收线程初始化，各标志位置位，开启重发定时器");
    }

    @Override
    public void run() {
        super.run();
        while(ReceiveThreadFlag){
            int onceRecNumber = 1;
            byte[] rec_original = serialPort232.receiveData();
            if(rec_original != null && rec_original.length >= 5) {
                StringBuilder str2 = new StringBuilder();
                for (byte aRec1 : rec_original) {
                    str2.append(Integer.toHexString(aRec1&0xFF)).append(" ");
                }
                Log.w("happy", "232收到原始串口："+ str2);
                /*如果指令前有乱码，掐头。如果指令后有乱码，去尾*/
                if (rec_original[0] != (byte) 0xE2 || rec_original[rec_original.length - 2] != (byte) 0xF1) {
                    boolean head = true;
                    boolean trail = true;
                    int start = 0;
                    int end = 0;
                    for (int y = 0; y < rec_original.length; y++) {
                        if (head) {
                            if (rec_original[y] == (byte)0xE2) {
                                head = false;
                                start = y;
                            }
                        }
                        if (trail) {
                            if (rec_original[rec_original.length - 1 - y] == (byte)0xF1) {
                                trail = false;
                                end = y - 1;
                            }
                        }
                    }
                    rec = new byte[rec_original.length - end - start];
                    System.arraycopy(rec_original, start, rec, 0, rec_original.length - end - start);
                    Log.w("happy", "232原始串口转换完毕："+ Arrays.toString(rec));
                }else{
                    rec = rec_original;
                    Log.w("happy", "232原始直接赋值rec："+ Arrays.toString(rec));
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
                Log.w("happy", "232收到串口："+ str1);
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
                        case ReceiveMoveFloor2:{
                                if(recReal[i][7] == (byte)0x59 && recReal[i][9] == (byte)0x01){
                                    Log.w("happy", "收到右柜移层上行——Y轴");Util.WriteFile("收到右柜移层上行——Y轴");
                                    rimConfirmOrderFlag2 = true;
                                    try {
                                        Thread.sleep(200);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    pushGoodsFlag2 = true;
                                }else if(recReal[i][7] == (byte)0x59 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                    rimConfirmOrderFlag2 = true;
                                    //报错，停止
                                    byte[] errorRec = new byte[9];
                                    System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                    new errorHandling(context,2,(byte)0x59,errorRec);
                                }
                                if(recReal[i][7] == (byte)0x58 && recReal[i][9] == (byte)0x01){
                                    Log.w("happy", "收到右柜移层上行——X轴");Util.WriteFile("收到右柜移层上行——X轴");
                                    rimConfirmOrderFlag2 = true;
                                }else if(recReal[i][7] == (byte)0x58 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                    rimConfirmOrderFlag2 = true;
                                    //报错，停止
                                    byte[] errorRec = new byte[9];
                                    System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                    new errorHandling(context,2,(byte)0x58,errorRec);
                                }
                            break;
                        }
                        case ReceivePushGoods2:{
                            if(recReal[i][7] == (byte)0x50 && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到右柜推货上行");Util.WriteFile("收到右柜推货上行");
                                goodsConfirmOrderFlag2 = true;
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(currentOutCount2 == 0){
                                    outGoodsFlag2 = true;
                                }else{
                                    moveFloorFlag2 = true;
                                }
                            }else if(recReal[i][7] == (byte)0x50 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                goodsConfirmOrderFlag2 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,2,(byte)0x50,errorRec);
                            }
                            break;
                        }
                        case ReceiveOutGoods2:{
                            if(recReal[i][7] == (byte)0x59 && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到右柜出货上行——Y轴");Util.WriteFile("收到右柜出货上行——Y轴");
                                rimConfirmOrderFlag2 = true;
                            }else if(recReal[i][7] == (byte)0x59 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag2 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,2,(byte)0x59,errorRec);
                            }
                            if(recReal[i][7] == (byte)0x58 && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到右柜出货上行——X轴");Util.WriteFile("收到右柜出货上行——X轴");
                                rimConfirmOrderFlag2 = true;
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                homingFlag2 = true;
                            }else if(recReal[i][7] == (byte)0x58 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag2 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,2,(byte)0x58,errorRec);
                            }
                            if(recReal[i][7] == (byte)0x5A && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到右柜出货上行——出货门开");Util.WriteFile("收到右柜出货上行——出货门开");
                                rimConfirmOrderFlag2 = true;
                            }else if(recReal[i][7] == (byte)0x5A && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag2 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,2,(byte)0x5A,errorRec);
                            }
                            break;
                        }
                        case ReceiveHoming2:{
                            if(recReal[i][7] == (byte)0x5A && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到右柜归位上行——出货门关");Util.WriteFile("收到右柜归位上行——出货门关");
                                rimConfirmOrderFlag2 = true;
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(rightFinishFlag){
                                    getGoodsFlag = true;
                                }
                            }else if(recReal[i][7] == (byte)0x5A && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag2 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,2,(byte)0x5A,errorRec);
                            }
                            if(recReal[i][7] == (byte)0x59 && recReal[i][9] == (byte)0x01){
                                Log.w("happy", "收到右柜归位上行——Y轴");Util.WriteFile("收到右柜归位上行——Y轴");
                                rimConfirmOrderFlag2 = true;
                                try {
                                    Thread.sleep(200);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                                if(!rightFinishFlag){
                                    moveFloorFlag2 = true;
                                }
                            }else if(recReal[i][7] == (byte)0x59 && recReal[i][9] != (byte)0x01 && recReal[i][9] != (byte)0x00){
                                rimConfirmOrderFlag2 = true;
                                //报错，停止
                                byte[] errorRec = new byte[9];
                                System.arraycopy(recReal[i], 7, errorRec, 0, 9);
                                new errorHandling(context,2,(byte)0x59,errorRec);
                            }
                            break;
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
            if(/*rec[8] == 0x01 &&*/ (rec[3] == (byte)0xC1 || rec[3] == (byte)0x81) && (rec[6] == (byte)0x01 || rec[6] == (byte)0x02 || rec[6] == (byte)0x03 || rec[6] == (byte)0x05)){
                rightOneSecFlag = false;rightReMissionTime = 0; rightZhenNumber++;rightPhases = 0;
                Log.w("happy", "右柜收到确认指令");
                Util.WriteFile("右柜收到确认指令");
            }
        }
        return recid;
    }
    /**
     * 启动定时器
     * 说明：定时重发机制
     * */
    private void timeStart(){
        TimerTask timerTaskRight = new TimerTask() {
            @Override
            public void run() {
                rightTimeNumber++;
                if (rightTimeNumber == 5 && rightOneSecFlag) {
                    if (rightReMissionTime < 5) {
                        rightReMissionTime++;
                        switch (rightPhases) {
                            case 1:
                                moveFloorFlag2 = true;
                                break;
                            case 2:
                                currentOutCount2 = currentOutCount2 - 1;
                                if(currentOutCount2 < 0 ){
                                    if(outGoods2[currentPackageCount2][6] == 0){
                                        if(outGoods2[currentPackageCount2][3] == 0){
                                            currentOutCount2 = 0;
                                        }else {
                                            currentOutCount2 = 1;
                                        }
                                    }
                                    else if(outGoods2[currentPackageCount2][6] != 0){
                                        currentOutCount2 = 2;
                                    }
                                }
                                pushGoodsFlag2 = true;
                                break;
                            case 3:
                                currentPackageCount2 = currentPackageCount2 - 1;
                                if(currentPackageCount2 < 0 ){
                                    currentPackageCount2 = SendThread.packageCount2 - 1;
                                }
                                outGoodsFlag2 = true;
                                break;
                            case 4:
                                homingFlag2 = true;
                                break;
                            default:break;
                        }
                    } else {
                        rightTimerErrorFlag = true;
                        Log.w("happy", "右柜通信故障");
                        Util.WriteFile("右柜通信故障");
                    }
                }
                if (rightTimeNumber >= 10000) {
                    rightTimeNumber = 0;
                }
            }
        };
            mTimer.schedule(timerTaskRight, 0, 200);

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


}
