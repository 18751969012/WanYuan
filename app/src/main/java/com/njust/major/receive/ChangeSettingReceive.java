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
        Log.w("happy", "收到上位机开关灯、门加热、温度设定更新等命令");
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

        String state = arg1.getStringExtra("state");
        if(state.equals("light")){
            Util.WriteFile("灯");
            String counter = arg1.getStringExtra("counter");
            String open = arg1.getStringExtra("open");
            Util.WriteFile(""+counter+""+open);
            if(counter.equals("0")){
                if(open.equals("1")){
                    motorControl.centerCommand(midZNum++, 1);
                    SystemClock.sleep(10);
                }else if(open.equals("0")){
                    motorControl.centerCommand(midZNum++, 2);
                    SystemClock.sleep(10);
                }
            }else if(counter.equals("1")){
                if(open.equals("1")){
                    motorControl.counterCommand(1, rimZNum1++, 1);
                    SystemClock.sleep(10);
                }else if(open.equals("0")){
                    motorControl.counterCommand(1, rimZNum1++, 2);
                    SystemClock.sleep(10);
                }
            }else if(counter.equals("2")){
                if(open.equals("1")){
                    motorControl.counterCommand(2, rimZNum2++, 1);
                    SystemClock.sleep(10);
                }else if(open.equals("0")){
                    motorControl.counterCommand(2, rimZNum2++, 2);
                    SystemClock.sleep(10);
                }
            }
        }else if(state.equals("doorHeat")){
            Util.WriteFile("门加热");
            String counter = arg1.getStringExtra("counter");
            String heat = arg1.getStringExtra("heat");
            Util.WriteFile(""+counter+""+heat);
            if(counter.equals("1")){
                if(heat.equals("1")){
                    motorControl.counterCommand(1, rimZNum1++, 3);
                    SystemClock.sleep(10);
                }else if(heat.equals("0")){
                    motorControl.counterCommand(1, rimZNum1++, 4);
                    SystemClock.sleep(10);
                }
            }else if(counter.equals("2")){
                if(heat.equals("1")){
                    motorControl.counterCommand(2, rimZNum2++, 3);
                    SystemClock.sleep(10);
                }else if(heat.equals("0")){
                    motorControl.counterCommand(2, rimZNum2++, 4);
                    SystemClock.sleep(10);
                }
            }
        }else if(state.equals("temperature")){
            Util.WriteFile("温度");
            String leftTempState = arg1.getStringExtra("leftTempState");
            String leftSetTemp = arg1.getStringExtra("leftSetTemp");
            String rightTempState = arg1.getStringExtra("rightTempState");
            String rightSetTemp = arg1.getStringExtra("rightSetTemp");
            Util.WriteFile(""+leftTempState+""+leftSetTemp+""+rightTempState+""+rightSetTemp);
            dao.updateTemperature(Integer.parseInt(leftTempState),Integer.parseInt(leftSetTemp),Integer.parseInt(rightTempState),Integer.parseInt(rightSetTemp));
            SystemClock.sleep(10);
        }
        VMMainThreadFlag = true;
        mQuery1Flag = true;
        mQuery2Flag = true;
        mQuery0Flag = true;
        mUpdataDatabaseFlag = true;
    }
}

