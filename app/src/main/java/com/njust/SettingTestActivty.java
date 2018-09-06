package com.njust;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.njust.major.SCM.MotorControl;
import com.njust.major.setting.SettingTestThread;

import org.w3c.dom.Text;

import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;



public class SettingTestActivty extends AppCompatActivity implements View.OnClickListener {


    private SettingTestThread settingTestThread;
    private MotorControl motorControl;
    private boolean serialFlag = true; //true为左 false为右
    private SerialPort serialPort;
    private final SerialPort mSerialPort1 = new SerialPort(1, 38400, 8, 'n', 1);
    private final SerialPort mSerialPort2 = new SerialPort(4, 38400, 8, 'n', 1);
    private int counter = 1;
    private TextView mNowCounter;
    private byte zhen = 0;

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
        serialPort = mSerialPort1;
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
                if (tmpRow != 0 && tmpColumn != 0) {
                    motorControl.pushTestCommand(counter, zhen++, 1, tmpRow, tmpColumn);
                } else if (tmpRow != 0 && tmpColumn == 0) {
                    motorControl.pushTestCommand(counter, zhen++, 2, tmpRow, tmpColumn);
                } else if (tmpRow == 0 && tmpColumn != 0) {
                    motorControl.pushTestCommand(counter, zhen++, 3, tmpRow, tmpColumn);
                } else if (tmpRow == 0 && tmpColumn == 0) {
                    motorControl.pushTestCommand(counter, zhen++, 4, tmpRow, tmpColumn);
                }
                break;
            case R.id.open_get_door:
                motorControl.centerCommand(zhen++, 4);
                break;
            case R.id.close_get_door:
                motorControl.centerCommand(zhen++, 5);
                break;
            case R.id.open_out_door:
                motorControl.counterCommand(counter, zhen++, 12);
                break;
            case R.id.close_out_door:
                motorControl.counterCommand(counter, zhen++, 13);
                break;
            case R.id.open_fall_door:
                motorControl.centerCommand(zhen++, 6);
                break;
            case R.id.close_fall_door:
                motorControl.centerCommand(zhen++, 6);
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
                motorControl.counterCommand(counter,zhen++,10);
                break;
            case R.id.x_motor_fan:
                motorControl.counterCommand(counter, zhen++,11);
        }
    }


    public void changeCounter() {
        if (serialFlag){
            serialPort = mSerialPort2;
        }else {
            serialPort = mSerialPort1;
        }
        settingTestThread.setPort(serialPort);
        motorControl.changPort(serialPort);
        serialFlag = !serialFlag;
        counter = counter == 1 ? 2 : 1;
        if (counter == 1) {
            mNowCounter.setText(LEFTCOUNTER);
        } else if (counter == 2) {
            mNowCounter.setText(RIGHTCOUNTER);
        }
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerTask.cancel();
        timer.cancel();
    }

    @Override
    protected void onPause() {
        super.onPause();
        settingTestThread.sendflag = false;

    }

    @Override
    protected void onResume() {
        super.onResume();
        settingTestThread.sendflag = true;

    }
}
