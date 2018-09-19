package com.njust.major.thread;

import android.content.Context;
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
    private int delay = 110;
    private int oldMidLight = 0;//0=关灯1=开灯
    private int oldLeftLight = 0;
    private int oldRightLight = 0;
    private int oldleftHeat = 0;//0=关1=开
    private int oldRightHeat = 0;
    private int oldLeftTempState = 0;//左柜温控模式：0=制冷1=制热2=常温
    private int oldLeftSetTemp = 0;//左柜温度  -20～70
    private int oldRightTempState = 0;//右柜温控模式：0=制冷1=制热2=常温
    private int oldRightSetTemp = 0;//右柜温度  -20～70
    private int oldLeftDoor = 0;//0=关门1=开门
    private int oldRightDoor = 0;
    private int oldMidDoor = 0;


    private boolean mQueryFlag = false; //是否查询机器状态
    private boolean changeStateFlag; //是否给下位机下发指令



    public VMMainThread(Context context) {
        super();
        this.context = context;
        serialPort485 = new SerialPort(1, 38400, 8, 'n', 1);
        motorControl = new MotorControl(serialPort485, context);
        mDao = new MachineStateDaoImpl(context);
        machineState = mDao.queryMachineState();
        changeStateFlag = false;
    }

    private class VMContentObserve extends ContentObserver {

        public VMContentObserve(Handler handler) {
            super(handler);
        }

        @Override
        public void onChange(boolean selfChange) {
            super.onChange(selfChange);
            Log.w("happy", "收到上位机开关灯、门加热、温度设定更新等命令");
            Util.WriteFile("收到上位机开关灯、门加热、温度设定更新等命令");
            machineState = mDao.queryMachineState();
            changeStateFlag = true;
            mQueryFlag = false;
        }
    }

    public void initMainThread() {


    }

    @Override
    public void run() {
        super.run();
        while (true) {
            if (VMMainThreadFlag) {
                if (mQueryFlag) {
                    machineState = mDao.queryMachineState();
                    motorControl.counterQuery(1,rimZNum1++, machineState.getLeftTempState(), machineState.getLeftSetTemp());
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length>10) {
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x65 && rec[3] == (byte)0xC0) {
                                if(rec[7] == (byte)0xEF && rec[8] == (byte)0xFF){//压缩机温度，有符号数，0xEFFF=故障
                                    state[0] = 22222;
                                }else{
                                    state[0] = (short) (rec[7] << 8 | rec[8]);
                                }
                                if(rec[9] == (byte)0xEF && rec[10] == (byte)0xFF){//边柜温度，有符号数，0xEFFF=故障
                                    state[1] = 22222;
                                }else{
                                    state[1] = (short) (rec[9] << 8 | rec[10]);
                                }
                                if(rec[11] == (byte)0xEF && rec[12] == (byte)0xFF){//边柜顶部温度，有符号数，0xEFFF=故障
                                    state[2] = 22222;
                                }else{
                                    state[2] = (short) (rec[11] << 8 | rec[12]);
                                }
                                state[3] = rec[13]==(byte)0x11? 2:(int)rec[13];//压缩机直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                state[4] = rec[14]==(byte)0x11? 2:(int)rec[14];//边柜直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                state[5] = (int)rec[15];//门开关状态，0=关门，1=开门
                                state[6] = (int)rec[16];//湿度测量值，%RH
                                state[7] = (int)rec[17];//边柜门加热状态，0=关，1=开
                                state[8] = (int)rec[18];//照明灯状态，0=关，1=开
                                state[9] = (int)rec[19];//下货光栅状态，0=正常，1=故障
                                state[10] = (int)rec[20];//X轴出货光栅状态，0=正常，1=故障
                                state[11] = (int)rec[21];//出货门开关状态，0=关，1=开，2=半开半关
                                machineState.setLeftCompressorTemp(state[0]);
                                machineState.setLeftCabinetTemp(state[1]);
                                machineState.setLeftCabinetTopTemp(state[2]);
                                machineState.setLeftCompressorDCfanState(state[3]);
                                machineState.setLeftCabinetDCfanState(state[4]);
                                machineState.setLeftDoor(state[5]);
                                machineState.setLeftHumidity(state[6]);
                                machineState.setLeftDoorheat(state[7]);
                                machineState.setLeftLight(state[8]);
                                machineState.setLeftPushGoodsRaster(state[9]);
                                machineState.setLeftOutGoodsRaster(state[10]);
                                machineState.setLeftOutGoodsDoor(state[11]);
                                mDao.updateMachineState(machineState);
                                break;
                            }
                        }
                    }

                    motorControl.counterQuery(2, rimZNum2++, machineState.getRightTempState(), machineState.getRightSetTemp());
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length>10) {
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x65 && rec[3] == (byte)0xC1) {
                                if (rec[7] == (byte) 0xEF && rec[8] == (byte) 0xFF) {//压缩机温度，有符号数，0xEFFF=故障
                                    state[12] = 22222;
                                } else {
                                    state[12] = (short) (rec[7] << 8 | rec[8]);
                                }
                                if (rec[9] == (byte) 0xEF && rec[10] == (byte) 0xFF) {//边柜温度，有符号数，0xEFFF=故障
                                    state[13] = 22222;
                                } else {
                                    state[13] = (short) (rec[9] << 8 | rec[10]);
                                }
                                if (rec[11] == (byte) 0xEF && rec[12] == (byte) 0xFF) {//边柜顶部温度，有符号数，0xEFFF=故障
                                    state[14] = 22222;
                                } else {
                                    state[14] = (short) (rec[11] << 8 | rec[12]);
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
                                machineState.setRightCompressorTemp(state[12]);
                                machineState.setRightCabinetTemp(state[13]);
                                machineState.setRightCabinetTopTemp(state[14]);
                                machineState.setRightCompressorDCfanState(state[15]);
                                machineState.setRightCabinetDCfanState(state[16]);
                                machineState.setRightDoor(state[17]);
                                machineState.setRightHumidity(state[18]);
                                machineState.setRightDoorheat(state[19]);
                                machineState.setRightLight(state[20]);
                                machineState.setRightPushGoodsRaster(state[21]);
                                machineState.setRightOutGoodsRaster(state[22]);
                                machineState.setRightOutGoodsDoor(state[23]);
                                mDao.updateMachineState(machineState);
                                break;
                            }
                        }
                    }

                    motorControl.centerQuery(midZNum++);
                    SystemClock.sleep(delay);
                    rec = serialPort485.receiveData();
                    if (rec != null && rec.length>10) {
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x6D && rec[3] == (byte)0xE0) {
                                state[24] = (int)rec[7];//照明灯状态，0=关，1=开
                                state[25] = (int)rec[8];//门锁状态，0=上锁，1=开锁
                                state[26] = (int)rec[9];//门开关状态，0=关门，1=开门
                                state[27] = (int)rec[10];//取货光栅状态，0=正常，1=故障
                                state[28] = (int)rec[11];//落货光栅状态，0=正常，1=故障
                                state[29] = (int)rec[12];//防夹手光栅状态，0=正常，1=故障
                                state[30] = (int)rec[13];//取货门开关状态，0=关，1=开，2=半开半关
                                state[31] = (int)rec[14];//落货门开关状态，0=关，1=开，2=半开半关
                                machineState.setMidLight(state[24]);
                                machineState.setMidDoorLock(state[25]);
                                machineState.setMidDoor(state[26]);
                                machineState.setMidGetGoodsRaster(state[27]);
                                machineState.setMidDropGoodsRaster(state[28]);
                                machineState.setMidAntiPinchHandRaster(state[29]);
                                machineState.setMidGetDoor(state[30]);
                                machineState.setMidDropDoor(state[31]);
                                mDao.updateMachineState(machineState);
                                break;
                            }
                        }
                    }

                    if(oldMidDoor != machineState.getMidDoor()){//中柜门
                        if(machineState.getMidDoor() == 1){//开门

                        }else{//关门

                        }
                        oldMidDoor = machineState.getMidDoor();
                    }

                }
                if (changeStateFlag) {
                    if(oldLeftLight != machineState.getLeftLight()){
                        motorControl.counterCommand(1, rimZNum1++, machineState.getLeftLight() == 1 ? 1 : 2);
                        oldLeftLight = machineState.getLeftLight();
                        SystemClock.sleep(delay);
                    }
                    if(oldRightLight != machineState.getRightLight()){
                        motorControl.counterCommand(2, rimZNum2++, machineState.getRightLight() == 1 ? 1 : 2);
                        oldRightLight = machineState.getRightLight();
                        SystemClock.sleep(delay);
                    }
                    if(oldMidLight != machineState.getMidLight()){
                        motorControl.centerCommand(midZNum++, machineState.getMidLight() == 1 ? 1 : 2);
                        oldMidLight = machineState.getMidLight();
                        SystemClock.sleep(delay);
                    }
                    if(oldleftHeat != machineState.getLeftDoorheat()){
                        motorControl.counterCommand(1, rimZNum1++, machineState.getLeftDoorheat() == 1 ? 3 : 4);
                        oldleftHeat = machineState.getLeftDoorheat();
                        SystemClock.sleep(delay);
                    }
                    if(oldRightHeat != machineState.getRightDoorheat()){
                        motorControl.counterCommand(2, rimZNum2++, machineState.getRightDoorheat() == 1 ? 3 : 4);
                        oldRightHeat = machineState.getRightDoorheat();
                        SystemClock.sleep(delay);
                    }
                    if(oldLeftTempState != machineState.getLeftTempState() || oldLeftSetTemp != machineState.getLeftSetTemp()){
                        motorControl.counterQuery(1, rimZNum1++, machineState.getLeftTempState(), machineState.getLeftSetTemp());
                        oldLeftTempState = machineState.getLeftTempState();
                        oldLeftSetTemp = machineState.getLeftSetTemp();
                        SystemClock.sleep(delay);
                    }
                    if(oldRightTempState != machineState.getRightTempState() || oldRightSetTemp != machineState.getRightSetTemp()){
                        motorControl.counterQuery(2, rimZNum2++, machineState.getRightTempState(), machineState.getRightSetTemp());
                        oldRightTempState = machineState.getRightTempState();
                        oldRightSetTemp = machineState.getRightSetTemp();
                        SystemClock.sleep(delay);
                    }
                    changeStateFlag = false;
                    mQueryFlag = true;
                }
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
