package com.njust;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.njust.major.SCM.MotorControl;
import com.njust.major.bean.MachineState;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.setting.SettingReceiveThread;

import java.util.Arrays;



public class SettingActivity extends AppCompatActivity implements View.OnClickListener {


    private TextView mTheMSG;
    private TextView mNowCounter;
    private EditText mFloorNo;//层数
    private TextView mFloors;

    private int thisFloorPosition;
    private int outFloorPosition; //出口
    private int[] floorsPosition; //各层
    private MachineStateDao mDao;

    private EditText mInput;
    private TextView yMotor;

    private final static String LEFTCOUNTER = "当前为左柜";
    private final static String RIGHTCOUNTER = "当前为右柜";
    private SerialPort serialPort;
    private final SerialPort mSerialPort = new SerialPort(1, 38400, 8, 'n', 1);
    private MotorControl motorControl;
    private int counter = 1;
    private Context mContext;
    private byte zhen = 0;
    SettingReceiveThread settingReceiveThread;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        mDao = new MachineStateDaoImpl(getApplicationContext());

        mTheMSG = (TextView) findViewById(R.id.the_msg);
        mNowCounter = (TextView) findViewById(R.id.now_counter);

        mFloors = (TextView) findViewById(R.id.floor_msg);
        yMotor = (TextView) findViewById(R.id.y_motor_msg);
        mFloorNo = (EditText) findViewById(R.id.floor_no);
        mInput = (EditText) findViewById(R.id.manuel_floor_no);
        floorsPosition = new int[0];

        Button fastUp = (Button) findViewById(R.id.fast_up);
        Button slowUp = (Button) findViewById(R.id.slow_up);
        Button fastDown = (Button) findViewById(R.id.fast_down);
        Button slowDown = (Button) findViewById(R.id.slow_down);
        Button stop = (Button) findViewById(R.id.stop);
        Button confirmThis = (Button) findViewById(R.id.confirm_this);
        Button changeCounter = (Button) findViewById(R.id.change_counter);
        Button confirmFloorNo = (Button) findViewById(R.id.confirm_floor_no);
        Button deleteLast = (Button) findViewById(R.id.delete_last);
        Button deleteAll = (Button) findViewById(R.id.delete_all);
        Button confirmAll = (Button) findViewById(R.id.confirm_all);
        Button manuelConfirm = (Button) findViewById(R.id.manuel_confirm_floor_no);
        Button getInTestActivty = (Button) findViewById(R.id.test_activity_button);
        Button positionSettingButton = (Button) findViewById(R.id.position_activity_button);


        positionSettingButton.setOnClickListener(this);
        fastUp.setOnClickListener(this);
        slowUp.setOnClickListener(this);
        fastDown.setOnClickListener(this);
        slowDown.setOnClickListener(this);
        stop.setOnClickListener(this);
        confirmThis.setOnClickListener(this);
        changeCounter.setOnClickListener(this);
        confirmFloorNo.setOnClickListener(this);
        deleteLast.setOnClickListener(this);
        deleteAll.setOnClickListener(this);
        confirmAll.setOnClickListener(this);
        manuelConfirm.setOnClickListener(this);
        getInTestActivty.setOnClickListener(this);
        serialPort = mSerialPort;
        settingReceiveThread = new SettingReceiveThread(serialPort);
        motorControl = new MotorControl(serialPort, mContext);
        settingReceiveThread.sendFlag = true;
        settingReceiveThread.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        serialPort.close();
    }

    @Override
    protected void onPause() {
        super.onPause();
        settingReceiveThread.sendFlag = false;

    }

    @Override
    protected void onResume() {
        super.onResume();
        settingReceiveThread.sendFlag = true;

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fast_up:
                motorControl.counterCommand(counter, zhen++, 5);
                break;
            case R.id.slow_up:
                motorControl.counterCommand(counter, zhen++, 7);
                break;
            case R.id.fast_down:
                motorControl.counterCommand(counter, zhen++, 6);
                break;
            case R.id.slow_down:
                motorControl.counterCommand(counter, zhen++, 8);
                break;
            case R.id.stop:
                motorControl.counterCommand(counter, zhen++, 9);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                thisFloorPosition = getGeerPosition();
                mTheMSG.setText(thisFloorPosition+ " ");
                yMotor.setText(settingReceiveThread.Response);
                break;
            case R.id.confirm_this:
                confirmThis();
                break;
            case R.id.change_counter:
                changeCounter();
                break;
            case R.id.confirm_floor_no:
                confirmFloor();
                break;
            case R.id.delete_last:
                deleteLast();
                break;
            case R.id.delete_all:
                deleteAll();
                break;
            case R.id.confirm_all:
                confirmAll();
                break;
            case R.id.manuel_confirm_floor_no:
                thisFloorPosition = Integer.parseInt(mInput.getText().toString());
                confirmThis();
                break;
            case R.id.test_activity_button:

                Intent intent = new Intent(SettingActivity.this, SettingTestActivty.class);
                startActivity(intent);
                break;
            case R.id.position_activity_button:
                Intent intent1 = new Intent(SettingActivity.this, SettingPositionActivity.class);
                startActivity(intent1);
                break;
        }
    }

    public void changeCounter() {
        settingReceiveThread.setPort(serialPort);
        counter = counter == 1 ? 2 : 1;
        if (counter == 1) {
            mNowCounter.setText(LEFTCOUNTER);
        } else {
            mNowCounter.setText(RIGHTCOUNTER);
        }
        deleteAll();
    }

    public void confirmFloor() {
        deleteIt();
        int a = Integer.parseInt(mFloorNo.getText().toString());
        floorsPosition = new int[a];

        showMsg();
    }

    public void confirmThis() {
        if (floorsPosition[floorsPosition.length - 1] != 0) {
            outFloorPosition = thisFloorPosition;
        }
        for (int i = 0; i < floorsPosition.length; i++) {
            if (floorsPosition[i] == 0) {
                floorsPosition[i] = thisFloorPosition;
                break;
            }
        }
        showMsg();
    }

    public void confirmAll() {

        String tmp = "";
        for (int aFloorsPosition : floorsPosition) {
            tmp += aFloorsPosition + " ";
        }
        mDao.updateSetting(counter, floorsPosition.length, outFloorPosition,tmp);
    }

    public void deleteLast() {
        if (outFloorPosition != 0) {
            outFloorPosition = 0;
            showMsg();
            return;
        }
        if (floorsPosition[floorsPosition.length - 1] != 0) {
            floorsPosition[floorsPosition.length - 1] = 0;
            showMsg();
            return;
        }

        if (floorsPosition[0] == 0) return;
        for (int i = 1; i < floorsPosition.length; i++) {
            if (floorsPosition[i] == 0) {
                floorsPosition[i - 1] = 0;
                break;
            }
        }
        showMsg();
    }

    public void deleteAll() {
        floorsPosition = new int[0];
        outFloorPosition = 0;
        mFloorNo.setText("0");
        showMsg();
    }

    public void deleteIt() {
        floorsPosition = new int[0];
        outFloorPosition = 0;
    }

    public void showMsg() {
        String str = "";
        for (int i = 0; i < floorsPosition.length; i++) {
            str += (i + 1) + "层为： " + floorsPosition[i] + "\n";
        }
        str += "出口为： " + outFloorPosition;
        mFloors.setText(str);
    }

    public int getGeerPosition() {
        return settingReceiveThread.gearPosition;
    }
}
