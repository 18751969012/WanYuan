package com.njust;

import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.njust.major.SCM.MotorControl;
import com.njust.major.config.Constant;
import com.njust.major.setting.SettingTestThread;
import com.njust.major.util.Util;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

import static com.njust.VMApplication.midZNum;
import static com.njust.VMApplication.rimZNum1;


public class SettingTestActivty extends AppCompatActivity implements View.OnClickListener {


    private SettingTestThread settingTestThread;
    private MotorControl motorControl;
    private SerialPort serialPort;
    private final SerialPort mSerialPort = new SerialPort(1, 38400, 8, 'n', 1);
    public int counter = 1;
    private TextView mNowCounter;
    private byte zhen = 0;
    private int delay = 100;

    public String outFoodResponse;
    public String getFoodResponse;
    public String fallFoodResponse;
    public String foodRoadResponse;
    public String xResponse;

    private TextView mQuHuoMotor;
    private TextView mLuoHuoMotor;
    private TextView mChuHuoMotor;
    private TextView mHuoDaoMotor;
    private TextView mXMotor;

    private EditText mRow;
    private EditText mColumn;

    private final static String LEFTCOUNTER = "当前为左柜";
    private final static String RIGHTCOUNTER = "当前为右柜";
    Timer timer = new Timer();

    public void onDataReceived() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mXMotor.setText(settingTestThread.xResponse);
                mChuHuoMotor.setText(settingTestThread.outFoodResponse);
                mQuHuoMotor.setText(settingTestThread.getFoodResponse);
                mLuoHuoMotor.setText(settingTestThread.fallFoodResponse);
                mHuoDaoMotor.setText(settingTestThread.foodRoadResponse);
            }
        });
    }


    TimerTask timerTask = new TimerTask() {
        @Override
        public void run() {
            onDataReceived();
        }
    };


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_test);
        mNowCounter = (TextView) findViewById(R.id.test_now_counter);
        Button changeCounter = (Button) findViewById(R.id.test_change_counter);

        mRow = (EditText) findViewById(R.id.row);
        mColumn = (EditText) findViewById(R.id.column);


        changeCounter.setOnClickListener(this);
        Button openGetDoor = (Button) findViewById(R.id.open_get_door);
        Button closeGetDoor = (Button) findViewById(R.id.close_get_door);
        Button openOutDoor = (Button) findViewById(R.id.open_out_door);
        Button closeOutDoor = (Button) findViewById(R.id.close_out_door);
        Button openFallDoor = (Button) findViewById(R.id.open_fall_door);
        Button closeFallDoor = (Button) findViewById(R.id.close_fall_door);
        Button openCenterLock = (Button) findViewById(R.id.open_center_lock);
        Button openSideLight = (Button) findViewById(R.id.open_side_light);
        Button closeSideLight = (Button) findViewById(R.id.close_side_light);
        Button openCenterLight = (Button) findViewById(R.id.open_center_light);
        Button closeCenterLight = (Button) findViewById(R.id.close_center_light);
        Button openDoorHeat = (Button) findViewById(R.id.open_door_heat);
        Button closeDoorHeat = (Button) findViewById(R.id.close_door_heat);
        Button xMotorZhen = (Button) findViewById(R.id.x_motor_zhen);
        Button xMotorFan = (Button) findViewById(R.id.x_motor_fan);
        Button mHoudao = (Button) findViewById(R.id.houdao_motor);
        Button returnBack = (Button) findViewById(R.id.return_back_button);
        mHuoDaoMotor = (TextView) findViewById(R.id.huodao_msg);
        mChuHuoMotor = (TextView) findViewById(R.id.chuhuo_msg);
        mLuoHuoMotor = (TextView) findViewById(R.id.luohuo_msg);
        mQuHuoMotor = (TextView) findViewById(R.id.quhuo_msg);
        mXMotor = (TextView) findViewById(R.id.x_motor_msg);
        xMotorZhen.setOnClickListener(this);
        xMotorFan.setOnClickListener(this);
        openGetDoor.setOnClickListener(this);
        closeGetDoor.setOnClickListener(this);
        openOutDoor.setOnClickListener(this);
        closeOutDoor.setOnClickListener(this);
        openFallDoor.setOnClickListener(this);
        closeFallDoor.setOnClickListener(this);
        openCenterLock.setOnClickListener(this);
        openSideLight.setOnClickListener(this);
        closeSideLight.setOnClickListener(this);
        openCenterLight.setOnClickListener(this);
        closeCenterLight.setOnClickListener(this);
        openDoorHeat.setOnClickListener(this);
        closeDoorHeat.setOnClickListener(this);
        mHoudao.setOnClickListener(this);
        returnBack.setOnClickListener(this);
        serialPort = mSerialPort;
        motorControl = new MotorControl(serialPort, getApplicationContext());
        settingTestThread = new SettingTestThread();
        settingTestThread.init(serialPort);
        settingTestThread.start();
        timer.schedule(timerTask, 500, 500);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.test_change_counter:
                changeCounter();
                break;

            case R.id.houdao_motor:
                int tmpRow = Integer.parseInt(mRow.getText().toString());
                int tmpColumn = Integer.parseInt(mColumn.getText().toString());
                Log.i("happy", "货道测试");
                houdao_motor(tmpRow,tmpColumn);
                mHuoDaoMotor.setText(foodRoadResponse);
                break;
            case R.id.open_get_door:
                openGetGoodsDoor();
                break;
            case R.id.close_get_door:
                closeGetGoodsDoor();
                break;
            case R.id.open_out_door:
                openOutGoodsDoor();
                break;
            case R.id.close_out_door:
                closeOutGoodsDoor();
                break;
            case R.id.open_fall_door:
                openDropGoodsDoor();
                break;
            case R.id.close_fall_door:
                closeDropGoodsDoor();
                break;
            case R.id.open_center_lock:
                motorControl.centerCommand(zhen++, 3);
                break;
            case R.id.open_center_light:
                motorControl.centerCommand(zhen++, 1);
                break;
            case R.id.close_center_light:
                motorControl.centerCommand(zhen++, 2);
                break;
            case R.id.open_side_light:
                motorControl.counterCommand(counter, zhen++, 1);
                break;
            case R.id.close_side_light:
                motorControl.counterCommand(counter, zhen++, 2);
                break;
            case R.id.open_door_heat:
                motorControl.counterCommand(counter, zhen++, 3);
                break;
            case R.id.close_door_heat:
                motorControl.counterCommand(counter, zhen++, 4);
                break;
            case R.id.x_motor_zhen:
                moveHorizontal(1);
                break;
            case R.id.x_motor_fan:
                moveHorizontal(2);
                break;
            case R.id.return_back_button:
                this.finish();
                timerTask.cancel();
                timer.cancel();
                settingTestThread.sendflag = false;
                break;
        }
    }


    public void changeCounter() {
        settingTestThread.setPort(serialPort);
        counter = counter == 1 ? 2 : 1;
        if (counter == 1) {
            mNowCounter.setText(LEFTCOUNTER);
        } else if (counter == 2) {
            mNowCounter.setText(RIGHTCOUNTER);
        }
    }

    private void houdao_motor(int tmpRow, int tmpColumn){
        boolean flag = true;
        int times = 0;
        while (flag){
            if (tmpRow != 0 && tmpColumn != 0) {
                motorControl.pushTestCommand(counter, zhen++, 1, tmpRow, tmpColumn);
            } else if (tmpRow != 0 && tmpColumn == 0) {
                motorControl.pushTestCommand(counter, zhen++, 2, tmpRow, tmpColumn);
            } else if (tmpRow == 0 && tmpColumn != 0) {
                motorControl.pushTestCommand(counter, zhen++, 3, tmpRow, tmpColumn);
            } else if (tmpRow == 0 && tmpColumn == 0) {
                motorControl.pushTestCommand(counter, zhen++, 4, tmpRow, tmpColumn);
            }
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x39 && rec[3] == (byte)(0x80+(counter-1)) && rec[7] == (byte)0x50){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            if(counter == 1){
                                settingTestThread.foodRoadFlag1 = true;
                            }else{
                                settingTestThread.foodRoadFlag2 = true;
                            }
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "货道板通信故障");
                Util.WriteFile("货道板通信故障");
            }
        }
    }
    private void openGetGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.openGetGoodsDoor(midZNum);
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            settingTestThread.openGetFoodFlag = true;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "中柜板通信故障");
            }
        }
    }
    private void closeGetGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.closeGetGoodsDoor(midZNum);
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            settingTestThread.openGetFoodFlag = true;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
            }
        }
    }
    private void openOutGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.openOutGoodsDoor(counter,rimZNum1);
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x6F && rec[3] == (byte)(0xC0+(counter-1)) && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            if(counter == 1){
                                settingTestThread.openOutFoodFlag1 = true;
                            }else{
                                settingTestThread.openOutFoodFlag2 = true;
                            }
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "边柜板通信故障");
                Util.WriteFile("边柜板通信故障");
            }
        }
    }
    private void closeOutGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.closeOutGoodsDoor(counter,rimZNum1);
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x63 && rec[3] == (byte)(0xC0+(counter-1)) && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            if(counter == 1){
                                settingTestThread.closeOutFoodFlag1 = true;
                            }else{
                                settingTestThread.closeOutFoodFlag2 = true;
                            }
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "边柜板通信故障");
                Util.WriteFile("边柜板通信故障");
            }
        }
    }
    private void openDropGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.openDropGoodsDoor(midZNum);
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x66 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            settingTestThread.openFallFoodFlag = true;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
            }
        }
    }
    private void closeDropGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.closeDropGoodsDoor(midZNum);
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x6C && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            settingTestThread.closeFallFoodFlag = true;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
            }
        }
    }
    private void moveHorizontal(int orientation){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.moveHorizontal(counter,rimZNum1,orientation,3600);
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();
            if (rec != null && rec.length >= 5) {
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)(0xC0+(counter-1)) && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            if(counter == 1){
                                settingTestThread.xFlag1 = true;
                            }else{
                                settingTestThread.xFlag2 = true;
                            }
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "边柜板通信故障");
                Util.WriteFile("边柜板通信故障");
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerTask.cancel();
        timer.cancel();
        settingTestThread.sendflag = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }
}
