package com.njust.major.thread;

import android.content.Context;
import android.database.ContentObserver;
import android.os.Handler;
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
    private SimpleDateFormat sdf;
    private byte[] RxBuff;
    private MotorControl motorControl;
    int[] state = new int[32];

    private boolean foodOutFlag = false;

    private boolean mQueryFlag = false; //是否查询机器状态
    private MachineStateDao mDao;
    private MachineState machineState;
    private MachineState tmpMachineState;
    private boolean changeStateFlag; //是否给下位机下发指令

    public VMMainThread(Context context) {
        super();
        this.context = context;
        serialPort485 = new SerialPort(1, 38400, 8, 'n', 1);
        motorControl = new MotorControl(serialPort485, context);
        PositionDao pDao = new PositionDaoImpl(context);
        TransactionDao tDao = new TransactionDaoImpl(context);
        mDao = new MachineStateDaoImpl(context);
        machineState = mDao.queryMachineState();
        tmpMachineState = machineState;
        changeStateFlag = false;
        sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
    }

    private class VMContentObserve extends ContentObserver {
        /**
         * Creates a content observer.
         *
         * @param handler The handler to run {@link #onChange} on, or null if none.
         */
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
                    motorControl.counterQuery(1,rimZNum1++, machineState.getLeftTempState(), machineState.getLeftSetTemp());
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        byte[] rec = serialPort485.receiveData();
                        if (rec != null) {
                            if (rec.length>10 && rec[6] == 0x62) {
                                state[0] = rec[7] * 256 + rec[8]; //压缩机温度，有符号数，0xEFFF=故障
                                state[1] = rec[9] * 256 + rec[10];//边柜温度，有符号数，0xEFFF=故障
                                state[2] = rec[11] * 256 + rec[12];//边柜顶部温度，有符号数，0xEFFF=故障
                                state[3] = rec[13];//压缩机直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                state[4] = rec[14];//边柜直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                state[5] = rec[15];//门开关状态，0=关门，1=开门
                                state[6] = rec[16];//湿度测量值，%RH
                                state[7] = rec[17];//边柜门加热状态，0=关，1=开
                                state[8] = rec[18];//照明灯状态，0=关，1=开
                                state[9] = rec[19];//下货光栅状态，0=正常，1=故障
                                state[10] = rec[20];//X轴出货光栅状态，0=正常，1=故障
                                state[11] = rec[21] * 256 + rec[22];//备用字节
                                break;
                            }
                        }

                    }
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    motorControl.counterQuery(2, rimZNum2++, machineState.getRightTempState(), machineState.getRightSetTemp());
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        byte[] rec = serialPort485.receiveData();
                        if (rec != null) {
                            if (rec.length>10 && rec[6] == 0x62) {
                                state[12] = rec[7] * 256 + rec[8];//压缩机温度，有符号数，0xEFFF=故障
                                state[13] = rec[9] * 256 + rec[10];//边柜温度，有符号数，0xEFFF=故障
                                state[14] = rec[11] * 256 + rec[12];//边柜顶部温度，有符号数，0xEFFF=故障
                                state[15] = rec[13];//压缩机直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                state[16] = rec[14];//边柜直流风扇状态，0x00=未启动，0x01=正常，0x11=异常
                                state[17] = rec[15];//门开关状态，0=关门，1=开门
                                state[18] = rec[16];//湿度测量值，%RH
                                state[19] = rec[17];//边柜门加热状态，0=关，1=开
                                state[20] = rec[18];//照明灯状态，0=关，1=开
                                state[21] = rec[19];//下货光栅状态，0=正常，1=故障
                                state[22] = rec[20];//X轴出货光栅状态，0=正常，1=故障
                                state[23] = rec[21] * 256 + rec[22];//备用字节
                                break;
                            }
                        }
                    }
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    motorControl.centerQuery(midZNum++);
                    for (int i = 0; i < 5; i++) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        byte[] rec = serialPort485.receiveData();
                        if (rec != null) {
                            if (rec.length>10 && rec[6] == 0x6D) {
                                state[26] = rec[7];//照明灯状态，0=关，1=开
                                state[27] = rec[8];//门锁状态，0=上锁，1=开锁
                                state[28] = rec[9];//取货光栅状态，0=正常，1=故障
                                state[29] = rec[10];//落货光栅状态，0=正常，1=故障
                                state[30] = rec[11];//防夹手光栅状态，0=正常，1=故障
                                state[31] = rec[12] * 256 + rec[13];//备用字节
                                break;
                            }
                        }
                    }
                    saveAndBroadcastmsg();

                }
                if (changeStateFlag) {
                    motorControl.counterCommand(1, rimZNum1++, machineState.getLeftLight() == 1 ? 1 : 2);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    motorControl.counterCommand(2, rimZNum2++, machineState.getRightLight() == 1 ? 1 : 2);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    motorControl.counterCommand(1, rimZNum1++, machineState.getLeftDoorheat() == 1 ? 3 : 4);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    motorControl.counterCommand(2, rimZNum2++, machineState.getRightDoorheat() == 1 ? 3 : 4);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    motorControl.centerCommand(midZNum++, machineState.getMidLight() == 1 ? 1 : 2);
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    motorControl.counterQuery(1, rimZNum1++, machineState.getLeftTempState(), machineState.getLeftSetTemp());
                    try {
                        Thread.sleep(150);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    motorControl.counterQuery(2, rimZNum2++, machineState.getRightTempState(), machineState.getRightSetTemp());
                    tmpMachineState = machineState;

                    changeStateFlag = false;
                    mQueryFlag = true;

                }

            }
            updateZhen();
        }
    }

    public void saveAndBroadcastmsg(){

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
