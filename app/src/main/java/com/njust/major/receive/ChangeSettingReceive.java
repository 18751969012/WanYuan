package com.njust.major.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.util.Log;

import com.njust.SerialPort;
import com.njust.major.SCM.MotorControl;
import com.njust.major.bean.MachineState;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.util.Util;
import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.mQuery0Flag;
import static com.njust.VMApplication.mQuery1Flag;
import static com.njust.VMApplication.mQuery2Flag;
import static com.njust.VMApplication.mUpdataDatabaseFlag;
import static com.njust.VMApplication.VMMainThreadRunning;
import static com.njust.VMApplication.midZNum;
import static com.njust.VMApplication.rimZNum1;
import static com.njust.VMApplication.rimZNum2;


public class ChangeSettingReceive extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {
        // 收到广播开启服务
        Util.WriteFile("收到上位机开关灯、门加热、温度设定更新等命令");
        MachineStateDao dao = new MachineStateDaoImpl(context);
        VMMainThreadFlag = false;
        mQuery1Flag = false;
        mQuery2Flag = false;
        mQuery0Flag = false;
        mUpdataDatabaseFlag = false;
        SystemClock.sleep(20);
        while(VMMainThreadRunning){
            SystemClock.sleep(10);
        }
        SerialPort serialPort = new SerialPort(1, 38400, 8, 'n', 1);;
        MotorControl motorControl = new MotorControl(serialPort, context);

        String open_mid = arg1.getStringExtra("open_mid");
        String open_left = arg1.getStringExtra("open_left");
        String open_right = arg1.getStringExtra("open_right");
        String heat_left = arg1.getStringExtra("heat_left");
        String heat_right = arg1.getStringExtra("heat_right");
        String leftTempState = arg1.getStringExtra("leftTempState");
        String leftSetTemp = arg1.getStringExtra("leftSetTemp");
        String rightTempState = arg1.getStringExtra("rightTempState");
        String rightSetTemp = arg1.getStringExtra("rightSetTemp");
        Util.WriteFile("open_mid:"+open_mid+"open_left:"+open_left+"open_right:"+open_right+"heat_left:"+heat_left+"heat_right:"+heat_right+
                "leftTempState:"+leftTempState+"leftSetTemp:"+leftSetTemp+"rightTempState:"+rightTempState+"rightSetTemp:"+rightSetTemp);
        dao.updateLight(Integer.parseInt(open_left),Integer.parseInt(open_right),Integer.parseInt(open_mid));
        dao.updateCounterDoorState(Integer.parseInt(heat_left),Integer.parseInt(heat_right));
        dao.updateTemperature(Integer.parseInt(leftTempState),Integer.parseInt(leftSetTemp),Integer.parseInt(rightTempState),Integer.parseInt(rightSetTemp));
        if(open_mid.equals("1")){
            motorControl.centerCommand(midZNum++, 1);
            SystemClock.sleep(10);
        }else{
            motorControl.centerCommand(midZNum++, 2);
            SystemClock.sleep(10);
        }
        if(open_left.equals("1")){
            motorControl.counterCommand(1, rimZNum1++, 1);
            SystemClock.sleep(10);
        }else{
            motorControl.counterCommand(1, rimZNum1++, 2);
            SystemClock.sleep(10);
        }
        if(open_right.equals("1")){
            motorControl.counterCommand(2, rimZNum1++, 1);
            SystemClock.sleep(10);
        }else{
            motorControl.counterCommand(2, rimZNum1++, 2);
            SystemClock.sleep(10);
        }
        if(heat_left.equals("1")){
            motorControl.counterCommand(1, rimZNum1++, 3);
            SystemClock.sleep(10);
        }else{
            motorControl.counterCommand(1, rimZNum1++, 4);
            SystemClock.sleep(10);
        }
        if(heat_right.equals("1")){
            motorControl.counterCommand(2, rimZNum2++, 3);
            SystemClock.sleep(10);
        }else{
            motorControl.counterCommand(2, rimZNum2++, 4);
            SystemClock.sleep(10);
        }

        VMMainThreadFlag = true;
        mQuery1Flag = true;
        mQuery2Flag = true;
        mQuery0Flag = true;
        mUpdataDatabaseFlag = true;
    }
}

