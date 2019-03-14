package com.njust;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.njust.major.SCM.MotorControl;
import com.njust.major.bean.Transaction;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.PositionDao;
import com.njust.major.dao.TransactionDao;
import com.njust.major.dao.impl.PositionDaoImpl;
import com.njust.major.dao.impl.TransactionDaoImpl;
import com.njust.major.util.Util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.VMMainThreadRunning;
import static com.njust.VMApplication.mQuery0Flag;
import static com.njust.VMApplication.mQuery1Flag;
import static com.njust.VMApplication.mQuery2Flag;
import static com.njust.VMApplication.mUpdataDatabaseFlag;
import static com.njust.VMApplication.midZNum;
import static com.njust.VMApplication.rimZNum1;
import static com.njust.VMApplication.rimZNum2;


public class SettingTestActivty extends AppCompatActivity implements View.OnClickListener {

    private PositionDao pDao;
    private TransactionDao tDao;
    private MotorControl motorControl;
    private SerialPort serialPort;
    private final SerialPort mSerialPort = new SerialPort(1, 38400, 8, 'n', 1);
    public int counter = 1;
    private TextView mNowCounter;
    private Button changeCounter;
    private Button openGetDoor;
    private Button closeGetDoor;
    private Button openOutDoor;
    private Button closeOutDoor;
//    private Button openFallDoor;
//    private Button closeFallDoor;
    private Button xMotorZhen;
    private Button xMotorFan;
    private Button mHoudao;

    private Button mHoudaoRow;
    private Button mHoudaoColumn;
    private Button mTest;

    private Button mHoudaoAll;

    private boolean mHoudaoRow_start = false;
    private boolean mHoudaoColumn_start = false;
    private boolean mHoudaoAll_start = false;
    private boolean mTest_start = false;

    Timer mTimer;
    TimerTask mTimerTask;
    TimerTask mTimerTask_row;
    TimerTask mTimerTask_column;
    TimerTask mTimerTask_all;

    private Button openSideLight;
    private Button closeSideLight;
    private Button openCenterLight;
    private Button closeCenterLight;
    private Button openDoorHeat;
    private Button closeDoorHeat;

    private byte zhen = 0;
    private int delay = 220;
    
    private TextView mQuHuoMotor;
//    private TextView mLuoHuoMotor;
    private TextView mChuHuoMotor;
    private TextView mHuoDaoMotor;
    private TextView mXMotor;

    private EditText mRow;
    private EditText mColumn;

    private EditText mPositionID;

    private final static String LEFTCOUNTER = "当前为左柜";
    private final static String RIGHTCOUNTER = "当前为右柜";

    private boolean sendflag = false;
    
    private String outFoodResponse;
    private String getFoodResponse;
    private String fallFoodResponse;
    private String foodRoadResponse;
    private String xResponse;
    
    private boolean openGetFoodFlag;
    private boolean closeGetFoodFlag;
    private boolean openFallFoodFlag;
    private boolean closeFallFoodFlag;

    private boolean foodRoadFlag1;
    private boolean openOutFoodFlag1;
    private boolean closeOutFoodFlag1;
    private boolean xFlag1;
    private boolean foodRoadFlag2;
    private boolean openOutFoodFlag2;
    private boolean closeOutFoodFlag2;
    private boolean xFlag2;

    public void onDataReceived() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mXMotor.setText(xResponse);
                mChuHuoMotor.setText(outFoodResponse);
                mQuHuoMotor.setText(getFoodResponse);
//                mLuoHuoMotor.setText(fallFoodResponse);
                mHuoDaoMotor.setText(foodRoadResponse);
            }
        });
    }



    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_test);

        tDao = new TransactionDaoImpl(getApplicationContext());
        pDao = new PositionDaoImpl(getApplicationContext());
        mNowCounter = (TextView) findViewById(R.id.test_now_counter);
        changeCounter = (Button) findViewById(R.id.test_change_counter);

        mRow = (EditText) findViewById(R.id.row);
        mColumn = (EditText) findViewById(R.id.column);
        mPositionID = (EditText) findViewById(R.id.positionID);


        changeCounter.setOnClickListener(this);
        openGetDoor = (Button) findViewById(R.id.open_get_door);
        closeGetDoor = (Button) findViewById(R.id.close_get_door);
        openOutDoor = (Button) findViewById(R.id.open_out_door);
        closeOutDoor = (Button) findViewById(R.id.close_out_door);
//        openFallDoor = (Button) findViewById(R.id.open_fall_door);
//        closeFallDoor = (Button) findViewById(R.id.close_fall_door);
//        Button openCenterLock = (Button) findViewById(R.id.open_center_lock);

        openSideLight = (Button) findViewById(R.id.open_side_light);
        closeSideLight = (Button) findViewById(R.id.close_side_light);
        openCenterLight = (Button) findViewById(R.id.open_center_light);
        closeCenterLight = (Button) findViewById(R.id.close_center_light);
        openDoorHeat = (Button) findViewById(R.id.open_door_heat);
        closeDoorHeat = (Button) findViewById(R.id.close_door_heat);

        xMotorZhen = (Button) findViewById(R.id.x_motor_zhen);
        xMotorFan = (Button) findViewById(R.id.x_motor_fan);
        mHoudao = (Button) findViewById(R.id.houdao_motor);
        mHoudaoRow = (Button) findViewById(R.id.houdao_motor_row);
        mHoudaoColumn = (Button) findViewById(R.id.houdao_motor_column);
        mHoudaoAll = (Button) findViewById(R.id.houdao_motor_all);
        mTest = (Button) findViewById(R.id.test);

        Button returnBack = (Button) findViewById(R.id.return_back_button);
        mHuoDaoMotor = (TextView) findViewById(R.id.huodao_msg);
        mChuHuoMotor = (TextView) findViewById(R.id.chuhuo_msg);
//        mLuoHuoMotor = (TextView) findViewById(R.id.luohuo_msg);
        mQuHuoMotor = (TextView) findViewById(R.id.quhuo_msg);
        mXMotor = (TextView) findViewById(R.id.x_motor_msg);
        xMotorZhen.setOnClickListener(this);
        xMotorFan.setOnClickListener(this);
        openGetDoor.setOnClickListener(this);
        closeGetDoor.setOnClickListener(this);
        openOutDoor.setOnClickListener(this);
        closeOutDoor.setOnClickListener(this);
//        openFallDoor.setOnClickListener(this);
//        closeFallDoor.setOnClickListener(this);
//        openCenterLock.setOnClickListener(this);
        openSideLight.setOnClickListener(this);
        closeSideLight.setOnClickListener(this);
        openCenterLight.setOnClickListener(this);
        closeCenterLight.setOnClickListener(this);
        openDoorHeat.setOnClickListener(this);
        closeDoorHeat.setOnClickListener(this);
        mHoudao.setOnClickListener(this);
        mHoudaoRow.setOnClickListener(this);
        mHoudaoColumn.setOnClickListener(this);
        mHoudaoAll.setOnClickListener(this);
        mTest.setOnClickListener(this);
        returnBack.setOnClickListener(this);
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

        runThread();
        sendflag = true;
        mTimer = new Timer();

        IntentFilter filter = new IntentFilter();//注册广播，用来接收模拟出货的完成信号
        filter.addAction("njust_outgoods_complete");
        filter.setPriority(Integer.MAX_VALUE);
        registerReceiver(myReceiver, filter);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.test_change_counter:
                changeCounter();
                break;
            case R.id.houdao_motor:
                String txt1 = mRow.getText().toString();
                Pattern p1 = Pattern.compile("[0-9]*");
                Matcher m1 = p1.matcher(txt1);
                String txt2 = mColumn.getText().toString();
                Pattern p2 = Pattern.compile("[0-9]*");
                Matcher m2 = p2.matcher(txt2);
                if(m1.matches() && m2.matches() && !mRow.getText().toString().equals("") && !mColumn.getText().toString().equals("")) {//输入的数字
                    if(Integer.parseInt(mRow.getText().toString()) > 0 && Integer.parseInt(mRow.getText().toString()) <= 10 &&
                            Integer.parseInt(mColumn.getText().toString()) > 0 && Integer.parseInt(mColumn.getText().toString()) <= 10){
                        int tmpRow = Integer.parseInt(mRow.getText().toString());
                        int tmpColumn = Integer.parseInt(mColumn.getText().toString());
                        closeButton();
                        houdao_motor(tmpRow,tmpColumn);
                    }else{
                        AlertDialog.Builder message = new AlertDialog.Builder(this);
                        message.setTitle("Error");
                        message.setMessage("行、列号请输入1-10之内的数字");
                        message.setPositiveButton("OK", null);
                        message.show();
                    }
                }else{
                    AlertDialog.Builder message = new AlertDialog.Builder(this);
                    message.setTitle("Error");
                    message.setMessage("行、列号请输入数字");
                    message.setPositiveButton("OK", null);
                    message.show();
                }
                break;
            case R.id.houdao_motor_row:
                if(!mHoudaoRow_start){
                    String txt3 = mRow.getText().toString();
                    Pattern p3 = Pattern.compile("[0-9]*");
                    Matcher m3 = p3.matcher(txt3);
                    if(m3.matches()&& !mRow.getText().toString().equals("")) {//输入的数字
                        if(Integer.parseInt(mRow.getText().toString()) > 0 && Integer.parseInt(mRow.getText().toString()) <= 10){
                            final int tmpRow = Integer.parseInt(mRow.getText().toString());
                            mHoudaoRow_start = true;
                            closeButton();
                            mHoudaoRow.setText("停止测试");
                            mHoudaoRow.setClickable(true);
                            if(mTimerTask_row !=null){
                                mTimerTask_row.cancel();
                            }
                            final int[] tmpColumn = {1};
                            mTimer.purge();
                            mTimerTask_row = new TimerTask() {
                                @Override
                                public void run() {
                                    boolean flag = true;
                                    int times = 0;
                                    while (flag){
                                        motorControl.pushTestCommand(counter, zhen++, 1,tmpRow, tmpColumn[0]);
                                        Util.WriteFile("按行测试:"+tmpRow+"行"+tmpColumn[0]+"列");
                                        SystemClock.sleep(delay);
                                        byte[] rec = serialPort.receiveData();
                                        if (rec != null && rec.length >= 5) {
                                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                                if(rec[6] == (byte)0x39 && rec[3] == (byte)(0x80+(counter-1)) && rec[7] == (byte)0x50){
                                                    if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                                                        flag = false;
                                                        times = 0;
                                                    }
                                                }
                                            }
                                        }
                                        times = times + 1;
                                        if(times == 3){
                                            flag = false;
                                            Util.WriteFile("测试货道通信故障，跳过本货道");
                                        }
                                    }
                                    tmpColumn[0]++;
                                    if(tmpColumn[0] == 11){
                                        mHoudaoRow_start = false;
                                        if(mTimerTask_row !=null){
                                            mTimerTask_row.cancel();
                                        }
                                        mTimer.purge();
                                        onSetButton(true,"row","按行测试");
                                        Util.WriteFile("按行测试完成");
                                    }
                                }
                            };
                            mTimer.schedule(mTimerTask_row,0,5000);
                        }else{
                            AlertDialog.Builder message = new AlertDialog.Builder(this);
                            message.setTitle("Error");
                            message.setMessage("行号请输入1-10之内的数字");
                            message.setPositiveButton("OK", null);
                            message.show();
                        }
                    }else{
                        AlertDialog.Builder message = new AlertDialog.Builder(this);
                        message.setTitle("Error");
                        message.setMessage("行号请输入数字");
                        message.setPositiveButton("OK", null);
                        message.show();
                    }
                    break;
                }else{
                    if(mTimerTask_row !=null){
                        mTimerTask_row.cancel();
                    }
                    mTimer.purge();
                    mHoudaoRow_start = false;
                    mHoudaoRow.setText("按行测试");
                    openButton();
                    Util.WriteFile("停止按行测试");
                    break;
                }
            case R.id.houdao_motor_column:
                if(!mHoudaoColumn_start){
                    String txt4 = mColumn.getText().toString();
                    Pattern p4 = Pattern.compile("[0-9]*");
                    Matcher m4 = p4.matcher(txt4);
                    if(m4.matches()&& !mColumn.getText().toString().equals("")) {//输入的数字
                        if(Integer.parseInt(mColumn.getText().toString()) > 0 && Integer.parseInt(mColumn.getText().toString()) <= 10){
                            final int tmpColumn = Integer.parseInt(mColumn.getText().toString());
                            mHoudaoColumn_start = true;
                            closeButton();
                            mHoudaoColumn.setText("停止测试");
                            mHoudaoColumn.setClickable(true);
                            if(mTimerTask_column !=null){
                                mTimerTask_column.cancel();
                            }
                            final int[] tmpRow = {1};
                            mTimer.purge();
                            mTimerTask_column = new TimerTask() {
                                @Override
                                public void run() {
                                    boolean flag = true;
                                    int times = 0;
                                    while (flag){
                                        motorControl.pushTestCommand(counter, zhen++, 1,tmpRow[0], tmpColumn);
                                        Util.WriteFile("按列测试:"+tmpRow[0]+"行"+tmpColumn+"列");
                                        SystemClock.sleep(delay);
                                        byte[] rec = serialPort.receiveData();
                                        if (rec != null && rec.length >= 5) {
                                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                                if(rec[6] == (byte)0x39 && rec[3] == (byte)(0x80+(counter-1)) && rec[7] == (byte)0x50){
                                                    if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                                                        flag = false;
                                                        times = 0;
                                                    }
                                                }
                                            }
                                        }
                                        times = times + 1;
                                        if(times == 3){
                                            flag = false;
                                            Util.WriteFile("测试货道通信故障，跳过本货道");
                                        }
                                    }
                                    tmpRow[0]++;
                                    if(tmpRow[0] == 11){
                                        mHoudaoColumn_start = false;
                                        if(mTimerTask_column !=null){
                                            mTimerTask_column.cancel();
                                        }
                                        mTimer.purge();
                                        onSetButton(true,"column","按列测试");
                                        Util.WriteFile("按列测试完成");
                                    }
                                }
                            };
                            mTimer.schedule(mTimerTask_column,0,5000);
                        }else{
                            AlertDialog.Builder message = new AlertDialog.Builder(this);
                            message.setTitle("Error");
                            message.setMessage("列号请输入1-10之内的数字");
                            message.setPositiveButton("OK", null);
                            message.show();
                        }
                    }else{
                        AlertDialog.Builder message = new AlertDialog.Builder(this);
                        message.setTitle("Error");
                        message.setMessage("列号请输入数字");
                        message.setPositiveButton("OK", null);
                        message.show();
                    }
                    break;
                }else{
                    if(mTimerTask_column !=null){
                        mTimerTask_column.cancel();
                    }
                    mTimer.purge();
                    mHoudaoColumn_start = false;
                    mHoudaoColumn.setText("按列测试");
                    openButton();
                    Util.WriteFile("停止按列测试");
                    break;
                }
            case R.id.houdao_motor_all:
                if(!mHoudaoAll_start){
                    mHoudaoAll_start = true;
                    closeButton();
                    mHoudaoAll.setText("停止测试");
                    mHoudaoAll.setClickable(true);
                    if(mTimerTask_all !=null){
                        mTimerTask_all.cancel();
                    }
                    final int[] tmpRow = {1};
                    final int[] tmpColumn = {1};
                    mTimer.purge();
                    mTimerTask_all = new TimerTask() {
                        @Override
                        public void run() {
                            boolean flag = true;
                            int times = 0;
                            while (flag){
                                motorControl.pushTestCommand(counter, zhen++, 1,tmpRow[0], tmpColumn[0]);
                                Util.WriteFile("一键测试:"+tmpRow[0]+"行"+tmpColumn[0]+"列");
                                SystemClock.sleep(delay);
                                byte[] rec = serialPort.receiveData();
                                if (rec != null && rec.length >= 5) {
                                    if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                        if(rec[6] == (byte)0x39 && rec[3] == (byte)(0x80+(counter-1)) && rec[7] == (byte)0x50){
                                            if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                                                flag = false;
                                                times = 0;
                                            }
                                        }
                                    }
                                }
                                times = times + 1;
                                if(times == 3){
                                    flag = false;
                                    Util.WriteFile("测试货道通信故障，跳过本货道");
                                }
                            }
                            tmpColumn[0]++;
                            if(tmpColumn[0] == 11){
                                tmpRow[0]++;
                                tmpColumn[0] = 1;
                                if(tmpRow[0] == 11){
                                    mHoudaoAll_start = false;
                                    if(mTimerTask_all !=null){
                                        mTimerTask_all.cancel();
                                    }
                                    mTimer.purge();
                                    onSetButton(true,"all","所有电机");
                                    Util.WriteFile("一键测试完成");
                                }
                            }
                        }
                    };
                    mTimer.schedule(mTimerTask_all,0,5000);
                    break;
                }else{
                    if(mTimerTask_all !=null){
                        mTimerTask_all.cancel();
                    }
                    mTimer.purge();
                    mHoudaoAll_start = false;
                    mHoudaoAll.setText("所有电机");
                    openButton();
                    Util.WriteFile("停止一键测试");
                    break;
                }
            case R.id.test:
                String positionID = mPositionID.getText().toString().trim();
                Pattern p = Pattern.compile("[0-9[ ]]*");
                Matcher m = p.matcher(positionID);
                ok:if (m.matches() && !mPositionID.getText().toString().equals("")) {
                    String[] str = positionID.split(" ");
                    int[] pIDs = new int[str.length];
                    for (int i = 0; i < str.length; i++) {
                        pIDs[i] = Integer.parseInt(str[i]);
                        if (pIDs[i] <= 0 || pIDs[i] > 200) {
                            AlertDialog.Builder message = new AlertDialog.Builder(this);
                            message.setTitle("Error");
                            message.setMessage("请输入1-200之间的数字");
                            message.setPositiveButton("OK", null);
                            message.show();
                            break ok;
                        } else if (pIDs[i] <= 100 && pDao.queryPosition(pIDs[i], 1).getState() == 0) {
                            AlertDialog.Builder message = new AlertDialog.Builder(this);
                            message.setTitle("Error");
                            message.setMessage("请按照配置输入可以正常使用的货道");
                            message.setPositiveButton("OK", null);
                            message.show();
                            break ok;
                        } else if (pIDs[i] > 100 && pDao.queryPosition(pIDs[i], 2).getState() == 0) {
                            AlertDialog.Builder message = new AlertDialog.Builder(this);
                            message.setTitle("Error");
                            message.setMessage("请按照配置输入可以正常使用的货道");
                            message.setPositiveButton("OK", null);
                            message.show();
                            break ok;
                        }
                    }
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());
                    String time = sdf.format(Calendar.getInstance().getTime());
                    Transaction queryLastedTransaction = tDao.queryLastedTransaction();
                    if (queryLastedTransaction != null) {
                        if (queryLastedTransaction.getComplete() != 0) {
                            Transaction transaction = new Transaction(0, "ordernoTest", 0, 9, time, " ", positionID, 0);
                            tDao.addTransaction(transaction);
                            SystemClock.sleep(30);
                            closeButton();
                            mTest_start = true;
                            mTest.setText("正在出货，请勿进行其他操作");
                            Intent intent = new Intent();
                            intent.setAction("Avm.START_OUTGOODS");
                            intent.putExtra("transaction_order_number", "ordernoTest");
                            getApplicationContext().sendBroadcast(intent);
                            Util.WriteFile("测试模拟出货：" + positionID);
                        } else {
                            AlertDialog.Builder message = new AlertDialog.Builder(this);
                            message.setTitle("Error");
                            message.setMessage("请等待出货完成");
                            message.setPositiveButton("OK", null);
                            message.show();
                            break;
                        }
                    } else {
                        Transaction transaction = new Transaction(0, "ordernoTest", 0, 9, time, " ", positionID, 0);
                        tDao.addTransaction(transaction);
                        SystemClock.sleep(30);
                        closeButton();
                        mTest_start = true;
                        mTest.setText("正在出货，请勿进行其他操作");
                        Intent intent = new Intent();
                        intent.setAction("Avm.START_OUTGOODS");
                        intent.putExtra("transaction_order_number", "ordernoTest");
                        getApplicationContext().sendBroadcast(intent);
                        Util.WriteFile("测试模拟出货：" + positionID);
                        break;
                    }
                } else {
                    AlertDialog.Builder message = new AlertDialog.Builder(this);
                    message.setTitle("Error");
                    message.setMessage("模拟出货请按格式正确输入货道号");
                    message.setPositiveButton("OK", null);
                    message.show();
                    break;
                }
                break;
            case R.id.open_get_door:
                closeButton();
                openGetGoodsDoor();
                break;
            case R.id.close_get_door:
                closeButton();
                closeGetGoodsDoor();
                break;
            case R.id.open_out_door:
                closeButton();
                openOutGoodsDoor();
                break;
            case R.id.close_out_door:
                closeButton();
                closeOutGoodsDoor();
                break;
//            case R.id.open_fall_door:
//                closeButton();
//                openDropGoodsDoor();
//                break;
//            case R.id.close_fall_door:
//                closeButton();
//                closeDropGoodsDoor();
//                break;
//            case R.id.open_center_lock:
//                motorControl.centerCommand(zhen++, 3);
//                break;
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
                closeButton();
                moveHorizontal(1);
                break;
            case R.id.x_motor_fan:
                closeButton();
                moveHorizontal(2);
                break;
            case R.id.return_back_button:
                this.finish();
                if(myReceiver != null){
                    unregisterReceiver(myReceiver);
                    myReceiver = null;
                }
                hintKbTwo();
                sendflag = false;
                if(mTimerTask_row !=null){
                    mTimerTask_row.cancel();
                }
                if(mTimerTask_column !=null){
                    mTimerTask_column.cancel();
                }
                if(mTimerTask_all !=null){
                    mTimerTask_all.cancel();
                }
                mTimer.purge();
                break;
        }
    }


    public void changeCounter() {
        counter = counter == 1 ? 2 : 1;
        if (counter == 1) {
            mNowCounter.setText(LEFTCOUNTER);
            Util.WriteFile("当前为左柜");
        } else if (counter == 2) {
            mNowCounter.setText(RIGHTCOUNTER);
            Util.WriteFile("当前为右柜");
        }
    }

    private void closeButton() {
        mHoudao.setClickable(false);
        mHoudaoRow.setClickable(false);
        mHoudaoColumn.setClickable(false);
        mHoudaoAll.setClickable(false);
        mTest.setClickable(false);
        openGetDoor.setClickable(false);
        closeGetDoor.setClickable(false);
        openOutDoor.setClickable(false);
        closeOutDoor.setClickable(false);
//        openFallDoor.setEnabled(false);
//        closeFallDoor.setEnabled(false);
        xMotorZhen.setClickable(false);
        xMotorFan.setClickable(false);
        openSideLight.setClickable(false);
        closeSideLight.setClickable(false);
        openCenterLight.setClickable(false);
        closeCenterLight.setClickable(false);
        openDoorHeat.setClickable(false);
        closeDoorHeat.setClickable(false);
    }
    private void openButton() {
        mHoudao.setClickable(true);
        mHoudaoRow.setClickable(true);
        mHoudaoColumn.setClickable(true);
        mHoudaoAll.setClickable(true);
        mTest.setClickable(true);
        openGetDoor.setClickable(true);
        closeGetDoor.setClickable(true);
        openOutDoor.setClickable(true);
        closeOutDoor.setClickable(true);
//        openFallDoor.setEnabled(true);
//        closeFallDoor.setEnabled(true);
        xMotorZhen.setClickable(true);
        xMotorFan.setClickable(true);
        openSideLight.setClickable(true);
        closeSideLight.setClickable(true);
        openCenterLight.setClickable(true);
        closeCenterLight.setClickable(true);
        openDoorHeat.setClickable(true);
        closeDoorHeat.setClickable(true);
    }
    protected void onSetButton(final boolean open, final String button,final String msg) {
        runOnUiThread(new Runnable() {
            public void run() {
                switch (button){
                    case "row":{
                        mHoudaoRow.setText(msg);
                        break;
                    }
                    case "column":{
                        mHoudaoColumn.setText(msg);
                        break;
                    }
                    case "all":{
                        mHoudaoAll.setText(msg);
                        break;
                    }
                    case "test_success":{
                        mTest.setText(msg);
                        break;
                    }
                    case "test_fail":{
                        mTest.setText(msg);
                        break;
                    }
                }
                if(open){
                    openButton();
                }else{
                    closeButton();
                }
            }
        });
    }

    private void houdao_motor(int tmpRow, int tmpColumn){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.pushTestCommand(counter, zhen, 1,tmpRow, tmpColumn);
            Log.w("happy", "发送串口");
            SystemClock.sleep(delay);
            byte[] rec = serialPort.receiveData();

            if (rec != null && rec.length >= 5) {
                StringBuilder str1 = new StringBuilder();
                for (byte aRec : rec) {
                    str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                }
                Log.w("happy", "485收到串口："+ str1);
                if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                    if(rec[6] == (byte)0x39 && rec[3] == (byte)(0x80+(counter-1)) && rec[7] == (byte)0x50){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            zhen++;
                            if(counter == 1){
                                foodRoadFlag1 = true;
                            }else{
                                foodRoadFlag2 = true;
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
                openButton();
            }
        }
    }
    private void openGetGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.openGetGoodsDoor(midZNum);
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
                    if(rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            openGetFoodFlag = true;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
                openButton();
            }
        }
    }
    private void closeGetGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.closeGetGoodsDoor(midZNum);
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
                    if(rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            closeGetFoodFlag = true;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
                openButton();
            }
        }
    }
    private void openOutGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.openOutGoodsDoor(counter,rimZNum1);
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
                    if(rec[6] == (byte)0x6F && rec[3] == (byte)(0xC0+(counter-1)) && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            if(counter == 1){
                                openOutFoodFlag1 = true;
                            }else{
                                openOutFoodFlag2 = true;
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
                openButton();
            }
        }
    }
    private void closeOutGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.closeOutGoodsDoor(counter,rimZNum1);
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
                    if(rec[6] == (byte)0x63 && rec[3] == (byte)(0xC0+(counter-1)) && rec[7] == (byte)0x5A){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            if(counter == 1){
                                closeOutFoodFlag1 = true;
                            }else{
                                closeOutFoodFlag2 = true;
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
                openButton();
            }
        }
    }
    private void openDropGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.openDropGoodsDoor(midZNum);
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
                    if(rec[6] == (byte)0x66 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            openFallFoodFlag = true;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
                openButton();
            }
        }
    }
    private void closeDropGoodsDoor(){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.closeDropGoodsDoor(midZNum);
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
                    if(rec[6] == (byte)0x6C && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            midZNum++;
                            closeFallFoodFlag = true;
                        }
                    }
                }
            }
            times = times + 1;
            if(times == 5){
                flag = false;
                Log.w("happy", "中柜板通信故障");
                Util.WriteFile("中柜板通信故障");
                openButton();
            }
        }
    }
    private void moveHorizontal(int orientation){
        boolean flag = true;
        int times = 0;
        while (flag){
            motorControl.moveHorizontal(counter,rimZNum1,orientation,3600);
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
                    if(rec[6] == (byte)0x78 && rec[3] == (byte)(0xC0+(counter-1)) && rec[7] == (byte)0x58){
                        if(rec[16] == (byte)0x00 || rec[16] == (byte)0x01 || rec[16] == (byte)0x03){
                            flag = false;
                            rimZNum1++;
                            if(counter == 1){
                                xFlag1 = true;
                            }else{
                                xFlag2 = true;
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
                openButton();
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        sendflag = false;
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }


    public void runThread() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (sendflag) {
                    while (foodRoadFlag1){
                        motorControl.query((byte)0x0C,(byte)0x80,rimZNum1);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x70 && rec[3] == (byte)0x80 && rec[7] == (byte)0x50){
                                    if(rec[16] == (byte)0x02){
                                        rimZNum1++;
                                        foodRoadFlag1 = false;
                                        foodRoadResponse = "";
                                        byte error1[];
                                        error1 = byteTo8Byte(rec[9]);
                                        if (error1[0] != (byte)0x00) {
                                            foodRoadResponse += "已执行动作 ";
                                        } else {
                                            foodRoadResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            foodRoadResponse += "货道电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            foodRoadResponse += "货道电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            foodRoadResponse += "货道超时（无货物输出的超时），";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            foodRoadResponse += "商品超时（货物遮挡光栅超时），";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            foodRoadResponse += "弹簧电机1反馈开关故障，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            foodRoadResponse += "弹簧电机2反馈开关故障，";
                                        }
                                        if(error1[7] == (byte)0x01){
                                            foodRoadResponse += "货道下货光栅故障，";
                                        }
                                        foodRoadResponse += "\r\n";
                                        foodRoadResponse += "货道电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        foodRoadResponse += "货道电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        foodRoadResponse += "货道电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(openGetFoodFlag){
                        motorControl.query((byte)0x07,(byte)0xE0,midZNum);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x64 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                                    if(rec[16] == (byte)0x02){
                                        midZNum++;
                                        openGetFoodFlag = false;
                                        getFoodResponse = "";
                                        byte error1[];
                                        byte error2[];
                                        error1 = byteTo8Byte(rec[9]);
                                        error2 = byteTo8Byte(rec[8]);
                                        if (error1[0] != (byte)0x00) {
                                            getFoodResponse += "已执行动作 ";
                                        } else {
                                            getFoodResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            getFoodResponse += "取货门电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            getFoodResponse += "取货门电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            getFoodResponse += "取货门上止点开关故障，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            getFoodResponse += "取货门下止点开关故障，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            getFoodResponse += "开、关取货门超时，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            getFoodResponse += "取货门半开、半关，";
                                        }
                                        if(error1[7] == (byte)0x01){
                                            getFoodResponse += "取货仓光栅检测无货物，";
                                        }
                                        if(error2[0] == (byte)0x01){
                                            getFoodResponse += "取货仓光栅检测有货物，";
                                        }
                                        if(error2[1] == (byte)0x01){
                                            getFoodResponse += "取货篮光栅故障，";
                                        }
                                        if(error2[2] == (byte)0x01){
                                            getFoodResponse += "防夹手光栅检测到遮挡物，";
                                        }
                                        if(error2[3] == (byte)0x01){
                                            getFoodResponse += "防夹手光栅故障，";
                                        }
                                        getFoodResponse += "\r\n";
                                        getFoodResponse += "取货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        getFoodResponse += "取货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        getFoodResponse += "取货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(closeGetFoodFlag){
                        motorControl.query((byte)0x08,(byte)0xE0,midZNum);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x75 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x4D){
                                    if(rec[16] == (byte)0x02){
                                        midZNum++;
                                        closeGetFoodFlag = false;
                                        getFoodResponse = "";
                                        byte error1[];
                                        byte error2[];
                                        error1 = byteTo8Byte(rec[9]);
                                        error2 = byteTo8Byte(rec[8]);
                                        if (error1[0] != (byte)0x00) {
                                            getFoodResponse += "已执行动作 ";
                                        } else {
                                            getFoodResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            getFoodResponse += "取货门电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            getFoodResponse += "取货门电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            getFoodResponse += "取货门上止点开关故障，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            getFoodResponse += "取货门下止点开关故障，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            getFoodResponse += "开、关取货门超时，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            getFoodResponse += "取货门半开、半关，";
                                        }
                                        if(error1[7] == (byte)0x01){
                                            getFoodResponse += "取货仓光栅检测无货物，";
                                        }
                                        if(error2[0] == (byte)0x01){
                                            getFoodResponse += "取货仓光栅检测有货物，";
                                        }
                                        if(error2[1] == (byte)0x01){
                                            getFoodResponse += "取货篮光栅故障，";
                                        }
                                        if(error2[2] == (byte)0x01){
                                            getFoodResponse += "防夹手光栅检测到遮挡物，";
                                        }
                                        if(error2[3] == (byte)0x01){
                                            getFoodResponse += "防夹手光栅故障，";
                                        }
                                        getFoodResponse += "\r\n";
                                        getFoodResponse += "取货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        getFoodResponse += "取货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        getFoodResponse += "取货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(openOutFoodFlag1){
                        motorControl.query((byte)0x04,(byte)0xC0,rimZNum1);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                                    if(rec[16] == (byte)0x02){
                                        rimZNum1++;
                                        openOutFoodFlag1 = false;
                                        outFoodResponse = "";
                                        byte error1[];
                                        error1 = byteTo8Byte(rec[9]);
                                        if (error1[0] != (byte)0x00) {
                                            outFoodResponse += "已执行动作 ";
                                        } else {
                                            outFoodResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            outFoodResponse += "出货门电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            outFoodResponse += "出货门电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            outFoodResponse += "出货门前止点开关故障，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            outFoodResponse += "出货门后止点开关故障，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            outFoodResponse += "开、关出货门超时，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            outFoodResponse += "出货门半开、半关，";
                                        }
                                        outFoodResponse += "\r\n";
                                        outFoodResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        outFoodResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        outFoodResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(closeOutFoodFlag1){
                        motorControl.query((byte)0x05,(byte)0xC0,rimZNum1);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x5A){
                                    if(rec[16] == (byte)0x02){
                                        rimZNum1++;
                                        closeOutFoodFlag1 = false;
                                        outFoodResponse = "";
                                        byte error1[];
                                        error1 = byteTo8Byte(rec[9]);
                                        if (error1[0] != (byte)0x00) {
                                            outFoodResponse += "已执行动作 ";
                                        } else {
                                            outFoodResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            outFoodResponse += "出货门电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            outFoodResponse += "出货门电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            outFoodResponse += "出货门前止点开关故障，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            outFoodResponse += "出货门后止点开关故障，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            outFoodResponse += "开、关出货门超时，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            outFoodResponse += "出货门半开、半关，";
                                        }
                                        outFoodResponse += "\r\n";
                                        outFoodResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        outFoodResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        outFoodResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(openFallFoodFlag){
                        motorControl.query((byte)0x09,(byte)0xE0,midZNum);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x66 && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
                                    if(rec[16] == (byte)0x02){
                                        midZNum++;
                                        openFallFoodFlag = false;
                                        fallFoodResponse = "";
                                        byte error1[];
                                        byte error2[];
                                        error1 = byteTo8Byte(rec[9]);
                                        error2 = byteTo8Byte(rec[8]);
                                        if (error1[0] != (byte)0x00) {
                                            fallFoodResponse += "已执行动作，";
                                        } else {
                                            fallFoodResponse += "未执行动作，";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            fallFoodResponse += "落货门电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            fallFoodResponse += "落货门电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            fallFoodResponse += "落货门上止点开关故障，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            fallFoodResponse += "落货门下止点开关故障，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            fallFoodResponse += "开、关落货门超时，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            fallFoodResponse += "落货门半开、半关，";
                                        }
                                        if(error1[7] == (byte)0x01){
                                            fallFoodResponse += "落货仓光栅检测无货物，";
                                        }
                                        if(error2[0] == (byte)0x01){
                                            fallFoodResponse += "落货仓光栅检测有货物，";
                                        }
                                        if(error2[1] == (byte)0x01){
                                            fallFoodResponse += "落货篮光栅故障，";
                                        }
                                        fallFoodResponse += "\r\n";
                                        fallFoodResponse += "落货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        fallFoodResponse += "落货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        fallFoodResponse += "落货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(closeFallFoodFlag){
                        motorControl.query((byte)0x0A,(byte)0xE0,midZNum);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x6C && rec[3] == (byte)0xE0 && rec[7] == (byte)0x46){
                                    if(rec[16] == (byte)0x02){
                                        midZNum++;
                                        closeFallFoodFlag = false;
                                        fallFoodResponse = "";
                                        byte error1[];
                                        byte error2[];
                                        error1 = byteTo8Byte(rec[9]);
                                        error2 = byteTo8Byte(rec[8]);
                                        if (error1[0] != (byte)0x00) {
                                            fallFoodResponse += "已执行动作，";
                                        } else {
                                            fallFoodResponse += "未执行动作，";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            fallFoodResponse += "落货门电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            fallFoodResponse += "落货门电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            fallFoodResponse += "落货门上止点开关故障，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            fallFoodResponse += "落货门下止点开关故障，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            fallFoodResponse += "开、关落货门超时，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            fallFoodResponse += "落货门半开、半关，";
                                        }
                                        if(error1[7] == (byte)0x01){
                                            fallFoodResponse += "落货仓光栅检测无货物，";
                                        }
                                        if(error2[0] == (byte)0x01){
                                            fallFoodResponse += "落货仓光栅检测有货物，";
                                        }
                                        if(error2[1] == (byte)0x01){
                                            fallFoodResponse += "落货篮光栅故障，";
                                        }
                                        fallFoodResponse += "\r\n";
                                        fallFoodResponse += "落货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        fallFoodResponse += "落货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        fallFoodResponse += "落货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(xFlag1){
                        motorControl.query((byte)0x02,(byte)0xC0,rimZNum1);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC0 && rec[7] == (byte)0x58){
                                    if(rec[16] == (byte)0x02){
                                        rimZNum1++;
                                        xFlag1 = false;
                                        xResponse = "";
                                        byte error1[];
                                        error1 = byteTo8Byte(rec[9]);
                                        if (error1[0] != (byte)0x00) {
                                            xResponse += "已执行动作 ";
                                        } else {
                                            xResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            xResponse += "X轴电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            xResponse += "X轴电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            xResponse += "X轴出货光栅未检测到货物，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            xResponse += "X轴出货光栅货物遮挡超时，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            xResponse += "X轴出货光栅故障，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            xResponse += "X轴电机超时，";
                                        }
                                        xResponse += "\r\n";
                                        xResponse += "X轴电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        xResponse += "X轴电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        xResponse += "X轴电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(foodRoadFlag2){
                        motorControl.query((byte)0x0C,(byte)0x81,rimZNum2);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x70 && rec[3] == (byte)0x81 && rec[7] == (byte)0x50){
                                    if(rec[16] == (byte)0x02){
                                        rimZNum2++;
                                        foodRoadFlag2 = false;
                                        foodRoadResponse = "";
                                        byte error1[];
                                        error1 = byteTo8Byte(rec[9]);
                                        if (error1[0] != (byte)0x00) {
                                            foodRoadResponse += "已执行动作 ";
                                        } else {
                                            foodRoadResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            foodRoadResponse += "货道电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            foodRoadResponse += "货道电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            foodRoadResponse += "货道超时（无货物输出的超时），";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            foodRoadResponse += "商品超时（货物遮挡光栅超时），";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            foodRoadResponse += "弹簧电机1反馈开关故障，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            foodRoadResponse += "弹簧电机2反馈开关故障，";
                                        }
                                        if(error1[7] == (byte)0x01){
                                            foodRoadResponse += "货道下货光栅故障，";
                                        }
                                        foodRoadResponse += "\r\n";
                                        foodRoadResponse += "货道电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        foodRoadResponse += "货道电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        foodRoadResponse += "货道电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(openOutFoodFlag2){
                        motorControl.query((byte)0x04,(byte)0xC1,rimZNum2);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x6F && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                                    if(rec[16] == (byte)0x02){
                                        rimZNum2++;
                                        openOutFoodFlag2 = false;
                                        outFoodResponse = "";
                                        byte error1[];
                                        error1 = byteTo8Byte(rec[9]);
                                        if (error1[0] != (byte)0x00) {
                                            outFoodResponse += "已执行动作 ";
                                        } else {
                                            outFoodResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            outFoodResponse += "出货门电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            outFoodResponse += "出货门电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            outFoodResponse += "出货门前止点开关故障，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            outFoodResponse += "出货门后止点开关故障，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            outFoodResponse += "开、关出货门超时，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            outFoodResponse += "出货门半开、半关，";
                                        }
                                        outFoodResponse += "\r\n";
                                        outFoodResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        outFoodResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        outFoodResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(closeOutFoodFlag2){
                        motorControl.query((byte)0x05,(byte)0xC1,rimZNum2);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x63 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x5A){
                                    if(rec[16] == (byte)0x02){
                                        rimZNum2++;
                                        closeOutFoodFlag2 = false;
                                        outFoodResponse = "";
                                        byte error1[];
                                        error1 = byteTo8Byte(rec[9]);
                                        if (error1[0] != (byte)0x00) {
                                            outFoodResponse += "已执行动作 ";
                                        } else {
                                            outFoodResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            outFoodResponse += "出货门电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            outFoodResponse += "出货门电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            outFoodResponse += "出货门前止点开关故障，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            outFoodResponse += "出货门后止点开关故障，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            outFoodResponse += "开、关出货门超时，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            outFoodResponse += "出货门半开、半关，";
                                        }
                                        outFoodResponse += "\r\n";
                                        outFoodResponse += "出货门电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        outFoodResponse += "出货门电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        outFoodResponse += "出货门电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
                    while(xFlag2){
                        motorControl.query((byte)0x02,(byte)0xC1,rimZNum2);
                        SystemClock.sleep(delay);
                        byte[] rec = serialPort.receiveData();
                        if (rec != null && rec.length >= 5) {
                            StringBuilder str1 = new StringBuilder();
                            for (byte aRec : rec) {
                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
                            }
                            Log.w("happy", "查询485收到串口："+ str1);
                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
                                if(rec[6] == (byte)0x78 && rec[3] == (byte)0xC1 && rec[7] == (byte)0x58){
                                    if(rec[16] == (byte)0x02){

                                        rimZNum2++;
                                        xFlag2 = false;
                                        xResponse = "";
                                        byte error1[];
                                        error1 = byteTo8Byte(rec[9]);
                                        if (error1[0] != (byte)0x00) {
                                            xResponse += "已执行动作 ";
                                        } else {
                                            xResponse += "未执行动作 ";
                                        }
                                        if(error1[1] == (byte)0x01){
                                            xResponse += "X轴电机过流，";
                                        }
                                        if(error1[2] == (byte)0x01){
                                            xResponse += "X轴电机断路，";
                                        }
                                        if(error1[3] == (byte)0x01){
                                            xResponse += "X轴出货光栅未检测到货物，";
                                        }
                                        if(error1[4] == (byte)0x01){
                                            xResponse += "X轴出货光栅货物遮挡超时，";
                                        }
                                        if(error1[5] == (byte)0x01){
                                            xResponse += "X轴出货光栅故障，";
                                        }
                                        if(error1[6] == (byte)0x01){
                                            xResponse += "X轴电机超时，";
                                        }
                                        xResponse += "\r\n";
                                        xResponse += "X轴电机实际动作时间（毫秒）:" + ((rec[10]&0xff) * 256 + (rec[11]&0xff)) + "\r\n";
                                        xResponse += "X轴电机最大电流（毫安）:" + ((rec[12]&0xff) * 256 + (rec[13]&0xff)) + "\r\n";
                                        xResponse += "X轴电机平均电流（毫安）:" + ((rec[14]&0xff) * 256 + (rec[15]&0xff)) + "\r\n";
                                        openButton();
                                        onDataReceived();
                                    }
                                }
                            }
                        }
                    }
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
    /**
     * 接收广播，处理模拟出货完毕
     * */
    private BroadcastReceiver myReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            String outgoods_status = intent.getStringExtra("outgoods_status");
            if(outgoods_status.equals("closeMidDoorSuccess")){
                VMMainThreadFlag = false;
                mQuery1Flag = false;
                mQuery2Flag = false;
                mQuery0Flag = false;
                mUpdataDatabaseFlag = false;
                SystemClock.sleep(20);
                while(VMMainThreadRunning){
                    SystemClock.sleep(20);
                }
                onSetButton(true,"test_success","模拟出货");
            }else if(outgoods_status.equals("fail")){
                String error_type = intent.getStringExtra("error_type");
                if(error_type.equals("stopAllCounter") || error_type.equals("stopOneCounter")){
                    VMMainThreadFlag = false;
                    mQuery1Flag = false;
                    mQuery2Flag = false;
                    mQuery0Flag = false;
                    mUpdataDatabaseFlag = false;
                    SystemClock.sleep(20);
                    while(VMMainThreadRunning){
                        SystemClock.sleep(20);
                    }
                    onSetButton(true,"test_fail","模拟出货");
                }
            }
        }

    };
    /**
     * 关闭软键盘
     * */
    private void hintKbTwo() {
        InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null && imm.isActive() && getCurrentFocus() != null) {
            if (getCurrentFocus().getWindowToken() != null) {
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
    /**
     * 重写dispatchTouchEvent，获取当前用户点击的位置，点击空白处缩小键盘
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            // 获得当前得到焦点的View，一般情况下就是EditText（特殊情况就是轨迹求或者实体案件会移动焦点）  
            View v = getCurrentFocus();

            if (isShouldHideInput(v, ev)) {
                hideSoftInput(v.getWindowToken());
            }
        }
        return super.dispatchTouchEvent(ev);
    }
    /**
     * 根据EditText所在坐标和用户点击的坐标相对比，来判断是否隐藏键盘，因为当用户点击EditText时没必要隐藏
     */
    private boolean isShouldHideInput(View v, MotionEvent event) {
        if (v != null && (v instanceof EditText)) {
            int[] l = { 0, 0 };
            v.getLocationInWindow(l);
            int left = l[0], top = l[1], bottom = top + v.getHeight(), right = left
                    + v.getWidth();
            return !(event.getX() > left && event.getX() < right && event.getY() > top && event.getY() < bottom);
        }// 如果焦点不是EditText则忽略，这个发生在视图刚绘制完，第一个焦点不在EditView上，和用户用轨迹球选择其他的焦点
        return false;
    }

    /**
     * 多种隐藏软件盘方法的其中一种
     */
    private void hideSoftInput(IBinder token) {
        if (token != null) {
            InputMethodManager im = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (im != null) {
                im.hideSoftInputFromWindow(token, InputMethodManager.HIDE_NOT_ALWAYS);
            }
        }
    }
}
