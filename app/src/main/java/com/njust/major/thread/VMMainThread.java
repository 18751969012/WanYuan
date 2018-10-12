package com.njust.major.thread;

import android.content.Context;
import android.content.Intent;
import android.database.ContentObserver;
import android.os.Handler;
import android.os.SystemClock;
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
    private MotorControl motorControl;
    private MachineStateDao mDao;
    private MachineState machineState;
    private byte[] rec;
    private int[] state = new int[32];
    private int[] stateOld = new int[32];
    private int delay = 60;

    private int oldLeftDoor = 0;//0=关门1=开门
    private int oldRightDoor = 0;
    private int oldMidDoor = 1;






    public VMMainThread(Context context) {
        super();
        this.context = context;
        serialPort485 = new SerialPort(1, 38400, 8, 'n', 1);
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
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
                        }
                        Util.WriteFile("查询左柜机器状态反馈：" + str1);
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
                                state[5] = (int) rec[15];//门开关状态，0=关门，1=开门
                                state[6] = (int) rec[16];//湿度测量值，%RH
                                state[7] = (int) rec[17];//边柜门加热状态，0=关，1=开
                                state[8] = (int) rec[18];//照明灯状态，0=关，1=开
                                state[9] = (int) rec[19];//下货光栅状态，0=正常，1=故障
                                state[10] = (int) rec[20];//X轴出货光栅状态，0=正常，1=故障
                                state[11] = (int) rec[21];//出货门开关状态，0=关，1=开，2=半开半关
                                Util.WriteFile("更新左柜机器状态");
                            }
                        }
                    }
                }
                if (mQuery2Flag) {
                    motorControl.counterQuery(2, rimZNum2++, machineState.getRightTempState(), machineState.getRightSetTemp());
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length > 10) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
                        }
                        Util.WriteFile("查询右柜机器状态反馈：" + str1);
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
                                state[17] = (int) rec[15];//门开关状态，0=关门，1=开门
                                state[18] = (int) rec[16];//湿度测量值，%RH
                                state[19] = (int) rec[17];//边柜门加热状态，0=关，1=开
                                state[20] = (int) rec[18];//照明灯状态，0=关，1=开
                                state[21] = (int) rec[19];//下货光栅状态，0=正常，1=故障
                                state[22] = (int) rec[20];//X轴出货光栅状态，0=正常，1=故障
                                state[23] = (int) rec[21];//出货门开关状态，0=关，1=开，2=半开半关
                                Util.WriteFile("更新右柜机器状态");
                            }
                        }
                    }
                }
                if (mQuery0Flag) {
                    motorControl.centerQuery(midZNum++);
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length > 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
                        }
                        Util.WriteFile("查询中柜机器状态反馈：" + str1);
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
                                Util.WriteFile("更新中柜机器状态");
                            }
                        }
                    }
                }
                if (mUpdataDatabaseFlag) {
                    if(stateOld[5] != state[5] || stateOld[17] != state[17] || stateOld[26] != state[26] || stateOld[25] != state[25]){//门开关、中间门门锁状态
                        mDao.updateDoorState(state[5],state[17],state[26],state[25]);
                        stateOld[5] = state[5]; stateOld[17] = state[17]; stateOld[26] = state[26]; stateOld[25] = state[25];
                        Util.WriteFile("更新门开关、中间门门锁状态");
                    }
                    if(stateOld[0] != state[0] || stateOld[1] != state[1] || stateOld[2] != state[2] || stateOld[6] != state[6] ||
                            stateOld[12] != state[12] || stateOld[13] != state[13] || stateOld[14] != state[14] || stateOld[18] != state[18]){//压缩机温度、边柜温度、边柜顶部温度、湿度
                        mDao.updateMeasureTemp(state[0],state[1],state[2],state[6],state[12],state[13],state[14],state[18]);
                        stateOld[0] = state[0]; stateOld[1] = state[1]; stateOld[2] = state[2]; stateOld[6] = state[6];
                        stateOld[12] = state[12]; stateOld[13] = state[13]; stateOld[14] = state[14]; stateOld[18] = state[18];
                        Util.WriteFile("更新压缩机温度、边柜温度、边柜顶部温度、湿度");
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
                        Util.WriteFile("更新门加热");
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

                    if(oldMidDoor != machineState.getMidDoor()){//中柜门
                        if(machineState.getMidDoor() == 1){//开门
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
                        oldMidDoor = machineState.getMidDoor();
                    }
                }
                VMMainThreadRunning = false;
                SystemClock.sleep(1000);
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
