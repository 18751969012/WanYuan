package com.njust.major.thread;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.njust.SerialPort;
import com.njust.major.SCM.MotorControl;
import com.njust.major.bean.MachineState;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.PositionDao;
import com.njust.major.dao.TransactionDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.dao.impl.PositionDaoImpl;
import com.njust.major.dao.impl.TransactionDaoImpl;
import com.njust.major.util.Util;

import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.mQuery0Flag;
import static com.njust.VMApplication.mQuery1Flag;
import static com.njust.VMApplication.mQuery2Flag;
import static com.njust.VMApplication.mUpdataDatabaseFlag;
import static com.njust.VMApplication.VMMainThreadRunning;
import static com.njust.VMApplication.current_transaction_order_number;
import static com.njust.VMApplication.rimZNum1;
import static com.njust.VMApplication.rimZNum2;
import static com.njust.VMApplication.aisleZNum1;
import static com.njust.VMApplication.aisleZNum2;
import static com.njust.VMApplication.midZNum;

public class VMMainThread extends Thread {
    private Context context;
    private SerialPort serialPort485;
    private SerialPort serialPort232;
    private MotorControl motorControl;
    private MachineStateDao mDao;
    private MachineState machineState;
    private byte[] rec;
    private int[] state = new int[42];
    private int[] stateOld = new int[42];
    private int delay = 180;

    private int oldLeftDoor = 1;//1=关门0=开门
    private int oldRightDoor = 1;
    private int oldMidDoor = 2;






    public VMMainThread(Context context) {
        super();
        this.context = context;
        serialPort485 = new SerialPort(1, 38400, 8, 'n', 1);
        serialPort232 = new SerialPort(4, 9600, 8, 'n', 1);
        motorControl = new MotorControl(serialPort485, context);
        mDao = new MachineStateDaoImpl(context);
        machineState = mDao.queryMachineState();
    }


    public void initMainThread() {

        VMMainThreadFlag = true;
        mQuery1Flag = true;
        mQuery2Flag = true;
        mQuery0Flag = true;
        mUpdataDatabaseFlag = true;
        machineState = mDao.queryMachineState();
        if(machineState.getMidLight() == 1){
            motorControl.centerCommand(midZNum++, 1);
            SystemClock.sleep(5);
        }else{
            motorControl.centerCommand(midZNum++, 2);
            SystemClock.sleep(5);
        }
        if(machineState.getLeftLight() == 1){
            motorControl.counterCommand(1, rimZNum1++, 1);
            SystemClock.sleep(5);
        }else{
            motorControl.counterCommand(1, rimZNum1++, 2);
            SystemClock.sleep(5);
        }
        if(machineState.getRightLight() == 1){
            motorControl.counterCommand(2, rimZNum1++, 1);
            SystemClock.sleep(5);
        }else{
            motorControl.counterCommand(2, rimZNum1++, 2);
            SystemClock.sleep(5);
        }
        if(machineState.getLeftDoorheat() == 1){
            motorControl.counterCommand(1, rimZNum1++, 3);
            SystemClock.sleep(5);
        }else{
            motorControl.counterCommand(1, rimZNum1++, 4);
            SystemClock.sleep(5);
        }
        if(machineState.getRightDoorheat() == 1){
            motorControl.counterCommand(2, rimZNum2++, 3);
            SystemClock.sleep(5);
        }else{
            motorControl.counterCommand(2, rimZNum2++, 4);
            SystemClock.sleep(5);
        }
        Util.WriteFile("初始化主线程,控制门加热、灯为上次保存到数据库的状态");
    }

    @Override
    public void run() {
        super.run();
        while (true) {
            if (VMMainThreadFlag) {
                VMMainThreadRunning = true;
                if (mQuery1Flag) {
                    machineState = mDao.queryMachineState();
                    motorControl.counterQuery(1, rimZNum1++, machineState.getLeftTempState(), machineState.getLeftSetTemp());
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length > 10) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询左柜机器状态反馈：" + str1);
                        if (rec[0] == (byte) 0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte) 0x0F && rec[rec.length - 2] == (byte) 0xF1 /*&& isVerify(rec)*/) {
                            if (rec[6] == (byte) 0x65 && rec[3] == (byte) 0xC0) {
                                if (rec[7] == (byte) 0xEF && rec[8] == (byte) 0xFF) {//压缩机温度，有符号数，0xEFFF=故障
                                    state[0] = 22222;
                                } else {
                                    state[0] = ((rec[7] << 8) | (rec[8] & 0xFF));
                                }
                                if (rec[9] == (byte) 0xEF && rec[10] == (byte) 0xFF) {//边柜温度，有符号数，0xEFFF=故障
                                    state[1] = 22222;
                                } else {
                                    state[1] = ((rec[9] << 8) | (rec[10] & 0xFF));
                                }
                                if (rec[11] == (byte) 0xEF && rec[12] == (byte) 0xFF) {//边柜顶部温度，有符号数，0xEFFF=故障
                                    state[2] = 22222;
                                } else {
                                    state[2] = ((rec[11] << 8) | (rec[12] & 0xFF));
                                }
                                state[3] = rec[13] == (byte) 0x11 ? 2 : (int) rec[13];//压缩机直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                state[4] = rec[14] == (byte) 0x11 ? 2 : (int) rec[14];//边柜直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                if(rec[16] == (byte) 0xFF){
                                    state[5] = 22222;
                                    state[6] = 22222;
                                }else{
                                    state[5] = (int) rec[15];//外部温度测量值，有符号数
                                    state[6] = (int) rec[16];//湿度测量值，%RH，0xFF=温湿度模块故障
                                }
                                state[7] = (int) rec[17];//边柜门加热状态，0=关，1=开
                                state[8] = (int) rec[18];//照明灯状态，0=关，1=开
                                state[9] = (int) rec[19];//下货光栅状态，0=正常，1=故障
                                state[10] = (int) rec[20];//X轴出货光栅状态，0=正常，1=故障
                                state[11] = (int) rec[21];//出货门开关状态，0=关，1=开，2=半开半关

                                state[32] = (int) rec[22];//温控交流总电源状态（0=断开，1=接通）
                                state[33] = (int) rec[23];//制冷压缩机工作状态（0=停止，1=运转）
                                state[34] = (int) rec[24];//压缩机风扇工作状态（0=停止，1=运转）
                                state[35] = (int) rec[25];//制热电热丝工作状态（0=断开，1=接通）
                                state[36] = (int) rec[26];//循环风风扇工作状态（0=停止，1=运转）
                            }
                        }
                    }

                    motorControl.query((byte) 0x01, (byte) 0xC0, aisleZNum1++);//query_immediately_Y_left
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询左柜Y轴上下止点开关状态反馈：" + str1);
                        if (rec[0] == (byte) 0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte) 0x0F && rec[rec.length - 2] == (byte) 0xF1 && rec[6] == (byte) 0x79 && rec[7] == (byte) 0x59 && rec[3] == (byte)0xC0) {
                            mDao.updateYState(1,(rec[8]&0x04)==0x00? 0:1,(rec[8]&0x08)==0x00? 0:1);
                        }
                    }

                    motorControl.query((byte) 0x03, (byte) 0x80, aisleZNum1++);//query_immediately_aisle_left
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询左柜下货光栅即时状态反馈：" + str1);
                        if (rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 && rec[6] == (byte)0x70 && rec[7] == (byte)0x50 && rec[16] == (byte)0x02 && rec[3] == (byte)0x80) {
                            mDao.updatePushGoodsRaster(1,(rec[8]&0x04)==0x00? 0:1);
                        }
                    }

                    motorControl.query((byte) 0x02, (byte) 0xC0, aisleZNum1++);//query_immediately_X_left
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询左柜出货光栅传感器即时状态反馈：" + str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 && rec[6] == (byte)0x78 && rec[7] == (byte)0x58 && rec[16] == (byte)0x02 && rec[3] == (byte)0xC0){
                            mDao.updateOutGoodsRaster(1,(rec[8]&0x04)==0x00? 0:1);
                        }
                    }

                    motorControl.query((byte) 0x05, (byte) 0xC0, aisleZNum1++);//query_immediately_outGoodsDoor_left
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询左柜出货门上下止点开关即时状态反馈：" + str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 && (rec[6] == (byte)0x6F || rec[6] == (byte)0x63) && rec[7] == (byte)0x5A && rec[16] == (byte)0x02 && rec[3] == (byte)0xC0){
                            mDao.updateOutGoodsDoorSwitch(1,(rec[8]&0x04)==0x00? 0:1,(rec[8]&0x08)==0x00? 0:1);
                        }
                    }
                }
                if (mQuery2Flag) {
                    motorControl.counterQuery(2, rimZNum2++, machineState.getRightTempState(), machineState.getRightSetTemp());
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length > 10) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询右柜机器状态反馈：" + str1);
                        if (rec[0] == (byte) 0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte) 0x0F && rec[rec.length - 2] == (byte) 0xF1 /*&& isVerify(rec)*/) {
                            if (rec[6] == (byte) 0x65 && rec[3] == (byte) 0xC1) {
                                if (rec[7] == (byte) 0xEF && rec[8] == (byte) 0xFF) {//压缩机温度，有符号数，0xEFFF=故障
                                    state[12] = 22222;
                                } else {
                                    state[12] = ((rec[7] << 8) | (rec[8] & 0xFF));
                                }
                                if (rec[9] == (byte) 0xEF && rec[10] == (byte) 0xFF) {//边柜温度，有符号数，0xEFFF=故障
                                    state[13] = 22222;
                                } else {
                                    state[13] = ((rec[9] << 8) | (rec[10] & 0xFF));
                                }
                                if (rec[11] == (byte) 0xEF && rec[12] == (byte) 0xFF) {//边柜顶部温度，有符号数，0xEFFF=故障
                                    state[14] = 22222;
                                } else {
                                    state[14] = ((rec[11] << 8) | (rec[12] & 0xFF));
                                }
                                state[15] = rec[13] == (byte) 0x11 ? 2 : (int) rec[13];//压缩机直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                state[16] = rec[14] == (byte) 0x11 ? 2 : (int) rec[14];//边柜直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                if(rec[16] == (byte) 0xFF){
                                    state[17] = 22222;
                                    state[18] = 22222;
                                }else{
                                    state[17] = (int) rec[15];//外部温度测量值，有符号数
                                    state[18] = (int) rec[16];//湿度测量值，%RH，0xFF=温湿度模块故障
                                }
                                state[19] = (int) rec[17];//边柜门加热状态，0=关，1=开
                                state[20] = (int) rec[18];//照明灯状态，0=关，1=开
                                state[21] = (int) rec[19];//下货光栅状态，0=正常，1=故障
                                state[22] = (int) rec[20];//X轴出货光栅状态，0=正常，1=故障
                                state[23] = (int) rec[21];//出货门开关状态，0=关，1=开，2=半开半关

                                state[37] = (int) rec[22];//温控交流总电源状态（0=断开，1=接通）
                                state[38] = (int) rec[23];//制冷压缩机工作状态（0=停止，1=运转）
                                state[39] = (int) rec[24];//压缩机风扇工作状态（0=停止，1=运转）
                                state[40] = (int) rec[25];//制热电热丝工作状态（0=断开，1=接通）
                                state[41] = (int) rec[26];//循环风风扇工作状态（0=停止，1=运转）
                            }
                        }
                    }

                    motorControl.query((byte) 0x01, (byte) 0xC1, aisleZNum2++);//query_immediately_Y_right
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询右柜Y轴上下止点开关状态反馈：" + str1);
                        if (rec[0] == (byte) 0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte) 0x0F && rec[rec.length - 2] == (byte) 0xF1 && rec[6] == (byte) 0x79 && rec[7] == (byte) 0x59 && rec[3] == (byte)0xC1) {
                            mDao.updateYState(2,(rec[8]&0x04)==0x00? 0:1,(rec[8]&0x08)==0x00? 0:1);
                        }
                    }

                    motorControl.query((byte) 0x03, (byte) 0x81, aisleZNum2++);//query_immediately_aisle_right
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询右柜下货光栅即时状态反馈：" + str1);
                        if (rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 && rec[6] == (byte)0x70 && rec[7] == (byte)0x50 && rec[16] == (byte)0x02 && rec[3] == (byte)0x81) {
                            mDao.updatePushGoodsRaster(2,(rec[8]&0x04)==0x00? 0:1);
                        }
                    }

                    motorControl.query((byte) 0x02, (byte) 0xC1, aisleZNum2++);//query_immediately_X_right
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询右柜出货光栅传感器即时状态反馈：" + str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 && rec[6] == (byte)0x78 && rec[7] == (byte)0x58 && rec[16] == (byte)0x02 && rec[3] == (byte)0xC1){
                            mDao.updateOutGoodsRaster(2,(rec[8]&0x04)==0x00? 0:1);
                        }
                    }

                    motorControl.query((byte) 0x05, (byte) 0xC1, aisleZNum2++);//query_immediately_outGoodsDoor_right
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询右柜出货门上下止点开关即时状态反馈：" + str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 && (rec[6] == (byte)0x6F || rec[6] == (byte)0x63) && rec[7] == (byte)0x5A && rec[16] == (byte)0x02 && rec[3] == (byte)0xC1){
                            mDao.updateOutGoodsDoorSwitch(2,(rec[8]&0x04)==0x00? 0:1,(rec[8]&0x08)==0x00? 0:1);
                        }
                    }
                }
                if (mQuery0Flag) {
                    motorControl.centerQuery(midZNum++);
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length > 5) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询中柜机器状态反馈：" + str1);
                        if (rec[0] == (byte) 0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte) 0x0F && rec[rec.length - 2] == (byte) 0xF1 /*&& isVerify(rec)*/) {
                            if (rec[6] == (byte) 0x6D && rec[3] == (byte) 0xE0) {
                                state[24] = (int) rec[7];//照明灯状态，0=关，1=开
                                state[25] = (int) rec[8];//门锁状态，0=上锁，1=开锁
                                state[26] = (int) rec[9];//门开关状态，0=关门，1=开门
                                state[27] = (int) rec[10];//取货光栅状态，0=正常，1=故障
                                state[28] = (int) rec[11];//落货光栅状态，0=正常，1=故障
                                state[29] = (int) rec[12];//防夹手光栅状态，0=正常，1=故障
                                state[30] = (int) rec[13];//取货门开关状态，0=关，1=开，2=半开半关
                                state[31] = (int) rec[14];//落货门开关状态，0=关，1=开，2=半开半关
                            }
                        }
                    }
                    motorControl.query((byte) 0x08, (byte) 0xE0, midZNum++);//query_immediately_getGoodsDoor
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length >= 8) {
//                        StringBuilder str1 = new StringBuilder();
//                        for (byte aRec : rec) {
//                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
//                        }
//                        Util.WriteFile("查询取货门上下止点开关即时状态反馈：" + str1);
                        if((rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 && rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D && rec[16] == (byte)0x02)||
                                (rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 && rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D && rec[16] == (byte)0x02)){
                            byte getGoodsDoorSwith = rec[8];
                            mDao.updateGetGoodsDoorSwitch((getGoodsDoorSwith&0x40)==0x00? 0:1,(getGoodsDoorSwith&0x80)==0x00? 0:1);}
                    }
                    byte[] TXBuf = new byte[12];
                    TXBuf[0] = (byte) 0x08;
                    TXBuf[1] = (byte) 0x03;
                    TXBuf[2] = (byte) 0x00;
                    TXBuf[3] = (byte) 0x00;
                    TXBuf[4] = (byte) 0x00;
                    TXBuf[5] = (byte) 0x02;
                    TXBuf[6] = (byte) 0xc4;
                    TXBuf[7] = (byte) 0x92;
                    serialPort232.sendData(TXBuf,TXBuf.length);
                    SystemClock.sleep(30);
                    byte[] rec_232 = serialPort232.receiveData();
                    if(rec_232 != null && rec_232.length > 7 && rec_232[0] == (byte)0x08 && rec_232[1] == (byte)0x03){
                        int electricQuantity = (rec_232[3] & 0xff) * 256 * 256 * 256 + (rec_232[4] & 0xff)* 256 * 256 + (rec_232[5] & 0xff)* 256 + (rec_232[6] & 0xff);
                        mDao.updateElectricQuantity(electricQuantity);
                    }
                }
                if (mUpdataDatabaseFlag) {
                    if(stateOld[26] != state[26] || stateOld[25] != state[25]){//中间门门开关、中间门门锁状态
                        mDao.updateDoorState(state[26],state[25]);
                        stateOld[26] = state[26]; stateOld[25] = state[25];
                        Util.WriteFile("更新门开关、中间门门锁状态");
                    }
                    if(stateOld[0] != state[0] || stateOld[1] != state[1] || stateOld[2] != state[2] || stateOld[5] != state[5] || stateOld[6] != state[6] ||
                            stateOld[12] != state[12] || stateOld[13] != state[13] || stateOld[14] != state[14] || stateOld[17] != state[17] || stateOld[18] != state[18]){//压缩机温度、边柜温度、边柜顶部温度、外部温度、湿度
                        mDao.updateMeasureTemp(state[0],state[1],state[2],state[5],state[6],state[12],state[13],state[14],state[17],state[18]);
                        stateOld[0] = state[0]; stateOld[1] = state[1]; stateOld[2] = state[2]; stateOld[5] = state[5]; stateOld[6] = state[6];
                        stateOld[12] = state[12]; stateOld[13] = state[13]; stateOld[14] = state[14]; stateOld[17] = state[17]; stateOld[18] = state[18];
//                        Util.WriteFile("更新压缩机温度、边柜温度、边柜顶部温度、外部温度、湿度");
                    }
                    if(stateOld[3] != state[3] || stateOld[4] != state[4] ||
                            stateOld[15] != state[15] || stateOld[16] != state[16]){//压缩机直流风扇状态和边柜直流风扇状态
                        mDao.updateDCfan(state[3],state[4],state[15],state[16]);
                        stateOld[3] = state[3]; stateOld[4] = state[4]; stateOld[15] = state[15]; stateOld[16] = state[16];
                        Util.WriteFile("更新压缩机直流风扇状态和边柜直流风扇状态");
                    }
                    if(stateOld[7] != state[7] || stateOld[19] != state[19]){//门加热
                        mDao.updateCounterDoorState(state[7],state[19]);
                        stateOld[7] = state[7]; stateOld[19] = state[19];
                        Util.WriteFile("更新门加热"+"left:"+stateOld[7]+"right:"+stateOld[19]);
                    }
                    if(stateOld[8] != state[8] || stateOld[20] != state[20] || stateOld[24] != state[24]){//灯状态有变化
                        mDao.updateLight(state[8],state[20],state[24]);
                        stateOld[8] = state[8]; stateOld[20] = state[20]; stateOld[24] = state[24];
                        Util.WriteFile("更新灯");
                    }
                    if(stateOld[9] != state[9] || stateOld[10] != state[10] || stateOld[21] != state[21] || stateOld[22] != state[22] ||
                            stateOld[27] != state[27] || stateOld[28] != state[28] || stateOld[29] != state[29]){//边柜下货光栅状态、X轴出货光栅状态，中柜取货光栅状态、落货光栅状态、防夹手光栅状态
                        mDao.updateRasterState(state[9],state[10],state[21],state[22],state[27],state[28],state[29]);
                        stateOld[9] = state[9]; stateOld[10] = state[10]; stateOld[21] = state[21]; stateOld[22] = state[22];
                        stateOld[27] = state[27]; stateOld[28] = state[28]; stateOld[29] = state[29];
                        Util.WriteFile("更新光栅状态");
                    }
                    if(stateOld[11] != state[11] || stateOld[23] != state[23] || stateOld[30] != state[30] || stateOld[31] != state[31]){//出货门、取货门、落货门开关状态
                        mDao.updateOtherDoorState(state[11],state[23],state[30],state[31]);
                        stateOld[11] = state[11]; stateOld[23] = state[23]; stateOld[30] = state[30];stateOld[31] = state[31];
                        Util.WriteFile("更新出货门、取货门、落货门开关状态");
                    }
                    if(stateOld[32] != state[32] || stateOld[33] != state[33] || stateOld[34] != state[34] || stateOld[35] != state[35] || stateOld[36] != state[36] ||
                            stateOld[37] != state[37] || stateOld[38] != state[38] || stateOld[39] != state[39] || stateOld[40] != state[40] || stateOld[41] != state[41]){//温控交流总电源状态、制冷压缩机工作状态、压缩机风扇工作状态、制热电热丝工作状态、循环风风扇工作状态
                        mDao.updateFan(state[32],state[33],state[34],state[35],state[36],state[37],state[38],state[39],state[40],state[41]);
                        stateOld[32] = state[32]; stateOld[33] = state[33]; stateOld[34] = state[34];stateOld[35] = state[35];stateOld[36] = state[36];
                        stateOld[37] = state[37]; stateOld[38] = state[38]; stateOld[39] = state[39];stateOld[40] = state[40];stateOld[41] = state[41];
                        Util.WriteFile("温控交流总电源状态、制冷压缩机工作状态、压缩机风扇工作状态、制热电热丝工作状态、循环风风扇工作状态");
                    }
                    if(oldMidDoor != state[26]){//中柜门
                        if(state[26] == 0){//开门
                            Intent intent = new Intent();
                            intent.setAction("njust_midDoor_status");
                            intent.putExtra("status", "open");
                            context.sendBroadcast(intent);
                            Util.WriteFile("开门");
                        }else{//关门
                            Intent intent = new Intent();
                            intent.setAction("njust_midDoor_status");
                            intent.putExtra("status", "close");
                            context.sendBroadcast(intent);
                            Util.WriteFile("关门");
                        }
                        oldMidDoor = state[26];
                    }
                }
                VMMainThreadRunning = false;
                SystemClock.sleep(50);
            }
            updateZhen();
        }
    }

    private void updateZhen(){
        if (rimZNum1 >= 256) {
            rimZNum1 = 0;
        }
        if (rimZNum2 >= 256) {
            rimZNum2 = 0;
        }
        if (aisleZNum1 >= 256) {
            aisleZNum1 = 0;
        }
        if (aisleZNum2 >= 256) {
            aisleZNum2 = 0;
        }
        if (midZNum >= 256) {
            midZNum = 0;
        }
    }
}
