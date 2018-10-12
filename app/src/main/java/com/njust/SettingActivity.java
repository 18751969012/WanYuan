package com.njust;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.njust.major.SCM.MotorControl;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.util.Util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.njust.VMApplication.OutGoodsThreadFlag;
import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.VMMainThreadRunning;
import static com.njust.VMApplication.mQuery0Flag;
import static com.njust.VMApplication.mQuery1Flag;
import static com.njust.VMApplication.mQuery2Flag;
import static com.njust.VMApplication.mUpdataDatabaseFlag;
import static com.njust.VMApplication.midZNum;
import static com.njust.VMApplication.rimZNum1;


public class SettingActivity extends AppCompatActivity implements View.OnClickListener {


    private TextView mTheMSG;
    private TextView mNowCounter;
    private EditText mFloorNo;//层数
    private TextView mFloors;

    private int thisFloorPosition = -1;
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
    private byte zhen = 0;
    private int delay = 130;
    private int[] yResponse = new int[5];
    private String Response;
    private boolean stopFlag = false;
    private boolean threadFlag = false;
    private boolean queryStopSuccess = false;
    private Button fastUp;
    private Button slowUp;
    private Button fastDown;
    private Button slowDown;
    private Button stop;





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

        fastUp = (Button) findViewById(R.id.fast_up);
        slowUp = (Button) findViewById(R.id.slow_up);
        fastDown = (Button) findViewById(R.id.fast_down);
        slowDown = (Button) findViewById(R.id.slow_down);
        stop = (Button) findViewById(R.id.stop);
        Button confirmThis = (Button) findViewById(R.id.confirm_this);
        Button changeCounter = (Button) findViewById(R.id.change_counter);
        Button confirmFloorNo = (Button) findViewById(R.id.confirm_floor_no);
        Button deleteLast = (Button) findViewById(R.id.delete_last);
        Button deleteAll = (Button) findViewById(R.id.delete_all);
        Button confirmAll = (Button) findViewById(R.id.confirm_all);
        Button manuelConfirm = (Button) findViewById(R.id.manuel_confirm_floor_no);
        Button getInTestActivty = (Button) findViewById(R.id.test_activity_button);
        Button returnBack = (Button) findViewById(R.id.return_back_button);


        returnBack.setOnClickListener(this);
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
        VMMainThreadFlag = false;
        mQuery1Flag = false;
        mQuery2Flag = false;
        mQuery0Flag = false;
        mUpdataDatabaseFlag = false;
        SystemClock.sleep(20);
        while(VMMainThreadRunning){
            SystemClock.sleep(20);
        }
        serialPort = mSerialPort;
        motorControl = new MotorControl(serialPort, getApplicationContext());
        Util.WriteFile("进入设置界面");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        VMMainThreadFlag = true;
        mQuery1Flag = true;
        mQuery2Flag = true;
        mQuery0Flag = true;
        mUpdataDatabaseFlag = true;
        Util.WriteFile("返回上位机，开启主线程");
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fast_up:
                closeButton();
                motorControl.counterCommand(counter, zhen++, 5);
                openButton();
                break;
            case R.id.slow_up:
                closeButton();
                motorControl.counterCommand(counter, zhen++, 7);
                openButton();
                break;
            case R.id.fast_down:
                closeButton();
                motorControl.counterCommand(counter, zhen++, 6);
                openButton();
                break;
            case R.id.slow_down:
                closeButton();
                motorControl.counterCommand(counter, zhen++, 8);
                openButton();
                break;
            case R.id.stop:
                closeButton();
                queryStopSuccess = false;
                boolean flag = true;
                int times = 0;
                while (flag){
                    motorControl.counterCommand(counter, zhen++, 9);
                    Log.w("happy", "发送串口");
                    SystemClock.sleep(delay);
                    byte[] rec = serialPort.receiveData();
                    if (rec != null && rec.length >= 5) {
                        StringBuilder str1 = new StringBuilder();
                        for (byte aRec : rec) {
                            str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                        }
                        Log.w("happy", "发送反馈："+ str1);
                        if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                            if(rec[6] == (byte)0x31 && rec[3] == (byte)(0xC0+(counter-1)) && rec[7] == (byte)0x59){
                                if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
                                    flag = false;
                                    threadFlag=true;
                                    stopFlag=true;
                                    queryStop();
                                }
                            }
                        }
                    }
                    times = times + 1;
                    if(times == 5){
                        flag = false;
                        Response = "测试通信故障";
                        Log.w("happy", "测试通信故障");
                        openButton();
                    }
                }
                while (!queryStopSuccess){
                    SystemClock.sleep(10);
                }
                mTheMSG.setText(thisFloorPosition+"");
                yMotor.setText(Response);
                threadFlag=false;
                stopFlag=false;
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
                String txt = mInput.getText().toString();
                Pattern p = Pattern.compile("[0-9]*");
                Matcher m = p.matcher(txt);
                if(m.matches() && !mInput.getText().toString().equals("")){//输入的数字
                    if(Integer.parseInt(mInput.getText().toString()) > 0){
                        thisFloorPosition = Integer.parseInt(mInput.getText().toString());
                        confirmThis();
                    }else{
                        AlertDialog.Builder message = new AlertDialog.Builder(this);
                        message.setTitle("Error");
                        message.setMessage("请输入大于0的数字");
                        message.setPositiveButton("OK", null);
                        message.show();
                    }
                }else{
                    AlertDialog.Builder message = new AlertDialog.Builder(this);
                    message.setTitle("Error");
                    message.setMessage("请输入数字");
                    message.setPositiveButton("OK", null);
                    message.show();
                }
                break;
            case R.id.test_activity_button:
                Intent intent = new Intent(SettingActivity.this, SettingTestActivty.class);
                startActivity(intent);
                break;
            case R.id.return_back_button:
                stopFlag = false;
                this.finish();
                VMMainThreadFlag = true;
                mQuery1Flag = true;
                mQuery2Flag = true;
                mQuery0Flag = true;
                mUpdataDatabaseFlag = true;
                Util.WriteFile("返回上位机，开启主线程");
                break;
        }
    }
    public void changeCounter() {
        counter = counter == 1 ? 2 : 1;
        if (counter == 1) {
            mNowCounter.setText(LEFTCOUNTER);
        } else {
            mNowCounter.setText(RIGHTCOUNTER);
        }
        deleteAll();
    }
    private void closeButton() {
        fastUp.setClickable(false);
        slowUp.setClickable(false);
        fastDown.setClickable(false);
        slowDown.setClickable(false);
        stop.setClickable(false);
    }
    private void openButton() {
        fastUp.setClickable(true);
        slowUp.setClickable(true);
        fastDown.setClickable(true);
        slowDown.setClickable(true);
        stop.setClickable(true);
    }
    public void confirmFloor() {
        floorsPosition = new int[0];
        outFloorPosition = 0;
        if(mFloorNo.getText().toString().equals("1") || mFloorNo.getText().toString().equals("2") ||
                mFloorNo.getText().toString().equals("3") || mFloorNo.getText().toString().equals("4") ||
                mFloorNo.getText().toString().equals("5") || mFloorNo.getText().toString().equals("6")){
            int a = Integer.parseInt(mFloorNo.getText().toString());
            floorsPosition = new int[a];
            showMsg();
        }else{
            AlertDialog.Builder message = new AlertDialog.Builder(this);
            message.setTitle("Error");
            message.setMessage("请输入1-6之间的数字");
            message.setPositiveButton("OK", null);
            message.show();
        }
    }

    public void confirmThis() {
        if(floorsPosition.length > 0 && thisFloorPosition >= 0){
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
        }else if(floorsPosition.length <= 0){
            AlertDialog.Builder message = new AlertDialog.Builder(this);
            message.setTitle("Error");
            message.setMessage("请先确认层数");
            message.setPositiveButton("OK", null);
            message.show();
        }else{
            AlertDialog.Builder message = new AlertDialog.Builder(this);
            message.setTitle("Error");
            message.setMessage("请先获取当前格位(通过停止键获取)");
            message.setPositiveButton("OK", null);
            message.show();
        }

    }

    public void confirmAll() {
        if(floorsPosition.length > 0){
            String tmp = "";
            for (int aFloorsPosition : floorsPosition) {
                tmp += aFloorsPosition + " ";
            }
            mDao.updateSetting(counter, floorsPosition.length, outFloorPosition,tmp);
        }
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


    public void showMsg() {
        String str = "";
        for (int i = 0; i < floorsPosition.length; i++) {
            str += (i + 1) + "层为： " + floorsPosition[i] + "\n";
        }
        str += "出口为： " + outFloorPosition;
        mFloors.setText(str);
    }
    /**
     * 将byte转换为一个长度为8的byte数组，数组每个值代表bit
     * @param b 1个字节byte数据
     * */
    private static byte[] byteTo8Byte(byte b) {
        byte[] array = new byte[8];
        for (int i = 0; i <= 7; i++) {
            array[i] = (byte)(b & 1);
            b = (byte) (b >> 1);
        }
        return array;
    }

    public void queryStop() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while ( threadFlag ) {
                        if(stopFlag){
                            motorControl.query((byte) 0x0B, (byte) (0xC0 + (counter - 1)), midZNum);
                            SystemClock.sleep(delay);
                            byte[] rec = serialPort.receiveData();
                            if (rec != null && rec.length >= 5) {
                                StringBuilder str1 = new StringBuilder();
                                for (byte aRec : rec) {
                                    str1.append(Integer.toHexString(aRec & 0xFF)).append(" ");
                                }
                                Log.w("happy", "查询485收到串口：" + str1);
                                if (rec[0] == (byte) 0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte) 0x0F && rec[rec.length - 2] == (byte) 0xF1 /*&& isVerify(rec)*/) {
                                    if (rec[6] == (byte) 0x79 && rec[3] == (byte) (0xC0+(counter-1)) && rec[7] == (byte) 0x59) {
                                        if (rec[18] == (byte) 0x02) {
                                            stopFlag = false;
                                            yResponse[0] = (rec[8] & 0xff) * 256 + (rec[9] & 0xff);
                                            yResponse[1] = (rec[10] & 0xff) * 256 + (rec[11] & 0xff);
                                            yResponse[2] = (rec[12] & 0xff) * 256 + (rec[13] & 0xff);
                                            yResponse[3] = (rec[14] & 0xff) * 256 + (rec[15] & 0xff);
                                            yResponse[4] = (rec[16] & 0xff) * 256 + (rec[17] & 0xff);
                                            thisFloorPosition = yResponse[4];

                                            Log.i("happy", "进来了" + yResponse[4]);
                                            Response = "";
                                            byte error1[];
                                            error1 = byteTo8Byte(rec[9]);
                                            if (error1[0] != (byte) 0x00) {
                                                Response += "已执行动作 ";
                                            } else {
                                                Response += "未执行动作 ";
                                            }
                                            if (error1[1] == (byte) 0x01) {
                                                Response += "Y轴电机过流，";
                                            }
                                            if (error1[2] == (byte) 0x01) {
                                                Response += "Y轴电机断路，";
                                            }
                                            if (error1[3] == (byte) 0x01) {
                                                Response += "Y轴上止点开关故障，";
                                            }
                                            if (error1[4] == (byte) 0x01) {
                                                Response += "Y轴下止点开关故障，";
                                            }
                                            if (error1[5] == (byte) 0x01) {
                                                Response += "Y轴电机超时，";
                                            }
                                            if (error1[6] == (byte) 0x01) {
                                                Response += "Y轴码盘故障，";
                                            }
                                            if (error1[7] == (byte) 0x01) {
                                                Response += "Y轴出货门定位开关故障，";
                                            }
                                            Response += "\r\n";
                                            Response += "电机实际动作时间（毫秒）:" + ((rec[10] & 0xff) * 256 + (rec[11] & 0xff)) + "\r\n";
                                            Response += "电机最大电流（毫安）:" + ((rec[12] & 0xff) * 256 + (rec[13] & 0xff)) + "\r\n";
                                            Response += "电机平均电流（毫安）:" + ((rec[14] & 0xff) * 256 + (rec[15] & 0xff)) + "\r\n";
                                            queryStopSuccess = true;
                                            SystemClock.sleep(10);
                                            openButton();
                                        }
                                    }
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread thread, Throwable e) {
            }
        });
        thread.start();
    }
}
