package com.njust;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.njust.major.SCM.MotorControl;
import com.njust.major.bean.MachineState;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.util.Util;

import java.text.DecimalFormat;
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
import static com.njust.VMApplication.rimZNum2;
import static com.njust.VMApplication.delayGetDoor;
import static com.njust.VMApplication.wuOrsihuodaoxian;


public class SettingActivity extends AppCompatActivity implements View.OnClickListener {

    private Button left_fast_up,left_slow_up,left_fast_down,left_slow_down,left_stop,left_query_Y,left_move_Y;
    private Button right_fast_up,right_slow_up,right_fast_down,right_slow_down,right_stop,right_query_Y,right_move_Y;
    private EditText left_grid_position_ET,left_please_input_grid_position;
    private EditText right_grid_position_ET,right_please_input_grid_position;
    private TextView left_floot_position_1,left_floot_position_2,left_floot_position_3,left_floot_position_4,left_floot_position_5,left_floot_position_6,left_out_position;
    private TextView right_floot_position_1,right_floot_position_2,right_floot_position_3,right_floot_position_4,right_floot_position_5,right_floot_position_6,right_out_position;
    private int thisFloorPosition = -1;
    private TextView info_TV;

    private final SerialPort mSerialPort = new SerialPort(1, 38400, 8, 'n', 1);
    private SerialPort serialPort;
    private MotorControl motorControl;
    private int delay = 220;
    private String Response;
    private boolean stopFlag = false;
    private boolean threadFlag = false;
    private boolean queryStopSuccess = false;

    private boolean closeButton = false;


//    private Button delayButton;
//    private EditText delayEditText;
    private TextView temp_minus_TV,temp_setting,temp_add_TV;
    private EditText compressor_max_continuous_worktime;
    private Button user_defined;
    private DecimalFormat df   =   new DecimalFormat("#####0.0");
    private TextView ele;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);
        if (ContextCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
            //没有权限则申请权限
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);//获取存储权限
        }
        initView();
        initData();
        Button confirmAll = (Button) findViewById(R.id.confirm_all);
        confirmAll.setOnClickListener(this);
        Button getInTestActivty = (Button) findViewById(R.id.test_activity_button);
        getInTestActivty.setOnClickListener(this);
        Button returnBack = (Button) findViewById(R.id.return_back_button);
        returnBack.setOnClickListener(this);
    }

    private void initView(){
        left_fast_up =  findViewById(R.id.left_fast_up);
        left_fast_up.setOnClickListener(this);
        left_slow_up =  findViewById(R.id.left_slow_up);
        left_slow_up.setOnClickListener(this);
        left_fast_down =  findViewById(R.id.left_fast_down);
        left_fast_down.setOnClickListener(this);
        left_slow_down =  findViewById(R.id.left_slow_down);
        left_slow_down.setOnClickListener(this);
        left_stop =  findViewById(R.id.left_stop);
        left_stop.setOnClickListener(this);
        left_query_Y =  findViewById(R.id.left_query_Y);
        left_query_Y.setOnClickListener(this);
        left_move_Y =  findViewById(R.id.left_move_Y);
        left_move_Y.setOnClickListener(this);
        left_grid_position_ET =  findViewById(R.id.left_grid_position_ET);
        left_please_input_grid_position =  findViewById(R.id.left_please_input_grid_position);

        right_fast_up =  findViewById(R.id.right_fast_up);
        right_fast_up.setOnClickListener(this);
        right_slow_up =  findViewById(R.id.right_slow_up);
        right_slow_up.setOnClickListener(this);
        right_fast_down =  findViewById(R.id.right_fast_down);
        right_fast_down.setOnClickListener(this);
        right_slow_down =  findViewById(R.id.right_slow_down);
        right_slow_down.setOnClickListener(this);
        right_stop =  findViewById(R.id.right_stop);
        right_stop.setOnClickListener(this);
        right_query_Y =  findViewById(R.id.right_query_Y);
        right_query_Y.setOnClickListener(this);
        right_move_Y =  findViewById(R.id.right_move_Y);
        right_move_Y.setOnClickListener(this);
        right_grid_position_ET =  findViewById(R.id.right_grid_position_ET);
        right_please_input_grid_position =  findViewById(R.id.right_please_input_grid_position);

        info_TV =  findViewById(R.id.info_TV);

        left_floot_position_1 =  findViewById(R.id.left_floot_position_1);
        left_floot_position_1.setOnClickListener(this);
        left_floot_position_2 =  findViewById(R.id.left_floot_position_2);
        left_floot_position_2.setOnClickListener(this);
        left_floot_position_3 =  findViewById(R.id.left_floot_position_3);
        left_floot_position_3.setOnClickListener(this);
        left_floot_position_4 =  findViewById(R.id.left_floot_position_4);
        left_floot_position_4.setOnClickListener(this);
        left_floot_position_5 =  findViewById(R.id.left_floot_position_5);
        left_floot_position_5.setOnClickListener(this);
        left_floot_position_6 =  findViewById(R.id.left_floot_position_6);
        left_floot_position_6.setOnClickListener(this);
        left_out_position =  findViewById(R.id.left_out_position);
        left_out_position.setOnClickListener(this);

        right_floot_position_1 =  findViewById(R.id.right_floot_position_1);
        right_floot_position_1.setOnClickListener(this);
        right_floot_position_2 =  findViewById(R.id.right_floot_position_2);
        right_floot_position_2.setOnClickListener(this);
        right_floot_position_3 =  findViewById(R.id.right_floot_position_3);
        right_floot_position_3.setOnClickListener(this);
        right_floot_position_4 =  findViewById(R.id.right_floot_position_4);
        right_floot_position_4.setOnClickListener(this);
        right_floot_position_5 =  findViewById(R.id.right_floot_position_5);
        right_floot_position_5.setOnClickListener(this);
        right_floot_position_6 =  findViewById(R.id.right_floot_position_6);
        right_floot_position_6.setOnClickListener(this);
        right_out_position =  findViewById(R.id.right_out_position);
        right_out_position.setOnClickListener(this);


//        delayButton = (Button) findViewById(R.id.delayButton);
//        delayButton.setOnClickListener(this);
//        delayEditText = (EditText) findViewById(R.id.delay);

        RadioGroup rg = (RadioGroup)findViewById(R.id.rg);
        final RadioButton ten_line = (RadioButton) findViewById(R.id.ten_line);
        final RadioButton five_line = (RadioButton) findViewById(R.id.five_line);
        if(wuOrsihuodaoxian.equals("13579_1357")){
            ten_line.setChecked(true);
        }else{
            five_line.setChecked(true);
        }
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){
            @Override
            public void onCheckedChanged(RadioGroup rg, int checkedId) {
                // TODO Auto-generated method stub
                if(checkedId == ten_line.getId()){
                    wuOrsihuodaoxian = "13579_1357";
                    VMApplication.setWuOrsihuodaoxian(getApplicationContext(),"13579_1357");
                }else if(checkedId == five_line.getId()){
                    wuOrsihuodaoxian = "12345_2345";
                    VMApplication.setWuOrsihuodaoxian(getApplicationContext(),"12345_2345");
                }
            }
        });

        temp_minus_TV =   findViewById(R.id.temp_minus_TV);
        temp_minus_TV.setOnClickListener(this);
        temp_add_TV =   findViewById(R.id.temp_add_TV);
        temp_add_TV.setOnClickListener(this);
        temp_setting =  findViewById(R.id.temp_setting);
        if(!(VMApplication.getTempControlHuiCha(getApplicationContext()).equals(""))){
            temp_setting.setText(df.format((double)Integer.parseInt(VMApplication.getTempControlHuiCha(getApplicationContext()))/10));
        }else{
            temp_setting.setText("1.0");
        }
        compressor_max_continuous_worktime =   findViewById(R.id.compressor_max_continuous_worktime);
        if(!(VMApplication.getCompressorMaxWorktime(getApplicationContext()).equals(""))){
            compressor_max_continuous_worktime.setText(VMApplication.getCompressorMaxWorktime(getApplicationContext()));
        }else{
            compressor_max_continuous_worktime.setText("");
        }
        user_defined =   findViewById(R.id.user_defined);
        user_defined.setOnClickListener(this);
        if(VMApplication.getTempUserDefined(getApplicationContext()).equals("open")){
            user_defined.setText(getResources().getString(R.string.user_defined_open));
            user_defined.setTextColor(getResources().getColor(R.color.green_1));
        }else{
            user_defined.setText(getResources().getString(R.string.user_defined_close));
            user_defined.setTextColor(getResources().getColor(R.color.black));
        }

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
    private void initData(){
        MachineStateDao dao = new MachineStateDaoImpl(getApplicationContext());
        MachineState machineState = dao.queryMachineState();
        if(machineState.getLeftFlootPosition(0) == 0){
            left_floot_position_1.setText(getResources().getString(R.string.left_floot_position_1));
        }else{
            left_floot_position_1.setText(String.valueOf(machineState.getLeftFlootPosition(0)));
        }
        if(machineState.getLeftFlootPosition(1) == 0){
            left_floot_position_2.setText(getResources().getString(R.string.left_floot_position_2));
        }else{
            left_floot_position_2.setText(String.valueOf(machineState.getLeftFlootPosition(1)));
        }
        if(machineState.getLeftFlootPosition(2) == 0){
            left_floot_position_3.setText(getResources().getString(R.string.left_floot_position_3));
        }else{
            left_floot_position_3.setText(String.valueOf(machineState.getLeftFlootPosition(2)));
        }
        if(machineState.getLeftFlootPosition(3) == 0){
            left_floot_position_4.setText(getResources().getString(R.string.left_floot_position_4));
        }else{
            left_floot_position_4.setText(String.valueOf(machineState.getLeftFlootPosition(3)));
        }
        if(machineState.getLeftFlootPosition(4) == 0){
            left_floot_position_5.setText(getResources().getString(R.string.left_floot_position_5));
        }else{
            left_floot_position_5.setText(String.valueOf(machineState.getLeftFlootPosition(4)));
        }
        if(machineState.getLeftFlootPosition(5) == 0){
            left_floot_position_6.setText(getResources().getString(R.string.left_floot_position_6));
        }else{
            left_floot_position_6.setText(String.valueOf(machineState.getLeftFlootPosition(5)));
        }
        if(machineState.getRightFlootPosition(0) == 0){
            right_floot_position_1.setText(getResources().getString(R.string.right_floot_position_1));
        }else{
            right_floot_position_1.setText(String.valueOf(machineState.getRightFlootPosition(0)));
        }
        if(machineState.getRightFlootPosition(1) == 0){
            right_floot_position_2.setText(getResources().getString(R.string.right_floot_position_2));
        }else{
            right_floot_position_2.setText(String.valueOf(machineState.getRightFlootPosition(1)));
        }
        if(machineState.getRightFlootPosition(2) == 0){
            right_floot_position_3.setText(getResources().getString(R.string.right_floot_position_3));
        }else{
            right_floot_position_3.setText(String.valueOf(machineState.getRightFlootPosition(2)));
        }
        if(machineState.getRightFlootPosition(3) == 0){
            right_floot_position_4.setText(getResources().getString(R.string.right_floot_position_4));
        }else{
            right_floot_position_4.setText(String.valueOf(machineState.getRightFlootPosition(3)));
        }
        if(machineState.getRightFlootPosition(4) == 0){
            right_floot_position_5.setText(getResources().getString(R.string.right_floot_position_5));
        }else{
            right_floot_position_5.setText(String.valueOf(machineState.getRightFlootPosition(4)));
        }
        if(machineState.getRightFlootPosition(5) == 0){
            right_floot_position_6.setText(getResources().getString(R.string.right_floot_position_6));
        }else{
            right_floot_position_6.setText(String.valueOf(machineState.getRightFlootPosition(5)));
        }
        if(machineState.getLeftOutPosition() == 0){
            left_out_position.setText(getResources().getString(R.string.Null));
        }else{
            left_out_position.setText(String.valueOf(machineState.getLeftOutPosition()));
        }
        if(machineState.getRightOutPosition() == 0){
            right_out_position.setText(getResources().getString(R.string.Null));
        }else{
            right_out_position.setText(String.valueOf(machineState.getRightOutPosition()));
        }

        ele = findViewById(R.id.ele);
        ele.setText("用电量："+String.valueOf((double)machineState.getElectricQuantity()/100));
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
        if(!closeButton){
            switch (v.getId()) {
                case R.id.left_fast_up:
                    closeButton();
                    motorControl.counterCommand(1, rimZNum1++, 5);
                    openButton();
                    break;
                case R.id.left_slow_up:
                    closeButton();
                    motorControl.counterCommand(1, rimZNum1++, 7);
                    openButton();
                    break;
                case R.id.left_fast_down:
                    closeButton();
                    motorControl.counterCommand(1, rimZNum1++, 6);
                    openButton();
                    break;
                case R.id.left_slow_down:
                    closeButton();
                    motorControl.counterCommand(1, rimZNum1++, 8);
                    openButton();
                    break;
                case R.id.left_stop:
                    closeButton();
                    motorControl.counterCommand(1, rimZNum1++, 9);
                    openButton();
//                    boolean flag = true;
//                    int times = 0;
//                    while (flag){
//                        motorControl.counterCommand(1, rimZNum1++, 9);
//                        Log.w("happy", "发送串口");
//                        SystemClock.sleep(delay);
//                        byte[] rec = serialPort.receiveData();
//                        if (rec != null && rec.length >= 5) {
//                            StringBuilder str1 = new StringBuilder();
//                            for (byte aRec : rec) {
//                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
//                            }
//                            Log.w("happy", "发送反馈："+ str1);
//                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
//                                if(rec[6] == (byte)0x31 && rec[3] == (byte)(0xC0) && rec[7] == (byte)0x59){
//                                    if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
//                                        flag = false;
//                                        openButton();
//                                    }
//                                }
//                            }
//                        }
//                        times = times + 1;
//                        if(times == 5){
//                            flag = false;
//                            Response = "测试通信故障";
//                            Log.w("happy", "测试通信故障");
//                            openButton();
//                        }
//                    }
                    break;
                case R.id.left_query_Y:
                    closeButton();
                    motorControl.counterCommand(1, rimZNum1++, 9);
                    SystemClock.sleep(100);
                    queryStopSuccess = false;
                    threadFlag=true;
                    stopFlag=true;
                    queryStop(1);
                    while(!queryStopSuccess){
                        SystemClock.sleep(20);
                    }
                    left_grid_position_ET.setText(String.valueOf(thisFloorPosition));
                    info_TV.setText(Response);
                    threadFlag=false;
                    stopFlag=false;
                    openButton();
                    break;
                case R.id.left_move_Y:
                    String left_grid_position = left_please_input_grid_position.getText().toString().trim();
                    if(!left_grid_position.equals("")){
                        if(Integer.parseInt(left_grid_position) > 0 && Integer.parseInt(left_grid_position) < 1000){
                            closeButton();
                            motorControl.moveFloorByMapan(1, rimZNum1++, Integer.parseInt(left_grid_position));
                            SystemClock.sleep(delay);
                            openButton();
                        }else{
                            AlertDialog.Builder message = new AlertDialog.Builder(this);
                            message.setTitle("Error");
                            message.setMessage("请输入大于0，小于1000的数字");
                            message.setPositiveButton("OK", null);
                            message.show();
                            break;
                        }
                    }else{
                        AlertDialog.Builder message = new AlertDialog.Builder(this);
                        message.setTitle("Error");
                        message.setMessage("请输入数字");
                        message.setPositiveButton("OK", null);
                        message.show();
                        break;
                    }
                    break;
                case R.id.right_fast_up:
                    closeButton();
                    motorControl.counterCommand(2, rimZNum2++, 5);
                    openButton();
                    break;
                case R.id.right_slow_up:
                    closeButton();
                    motorControl.counterCommand(2, rimZNum2++, 7);
                    openButton();
                    break;
                case R.id.right_fast_down:
                    closeButton();
                    motorControl.counterCommand(2, rimZNum2++, 6);
                    openButton();
                    break;
                case R.id.right_slow_down:
                    closeButton();
                    motorControl.counterCommand(2, rimZNum2++, 8);
                    openButton();
                    break;
                case R.id.right_stop:
                    closeButton();
                    motorControl.counterCommand(2, rimZNum2++, 9);
                    openButton();
//                    boolean flag1 = true;
//                    int times1 = 0;
//                    while (flag1){
//                        motorControl.counterCommand(2, rimZNum2++, 9);
//                        Log.w("happy", "发送串口");
//                        SystemClock.sleep(delay);
//                        byte[] rec = serialPort.receiveData();
//                        if (rec != null && rec.length >= 5) {
//                            StringBuilder str1 = new StringBuilder();
//                            for (byte aRec : rec) {
//                                str1.append(Integer.toHexString(aRec&0xFF)).append(" ");
//                            }
//                            Log.w("happy", "发送反馈："+ str1);
//                            if(rec[0] == (byte)0xE2 && rec[1] == rec.length && rec[2] == 0x00 && rec[4] == (byte)0x0F && rec[rec.length-2] == (byte)0xF1 /*&& isVerify(rec)*/){
//                                if(rec[6] == (byte)0x31 && rec[3] == (byte)(0xC1) && rec[7] == (byte)0x59){
//                                    if(rec[18] == (byte)0x00 || rec[18] == (byte)0x01 || rec[18] == (byte)0x03){
//                                        flag1 = false;
//                                        openButton();
//                                    }
//                                }
//                            }
//                        }
//                        times1 = times1 + 1;
//                        if(times1 == 5){
//                            flag1 = false;
//                            Response = "测试通信故障";
//                            Log.w("happy", "测试通信故障");
//                            openButton();
//                        }
//                    }
                    break;
                case R.id.right_query_Y:
                    closeButton();
                    motorControl.counterCommand(2, rimZNum2++, 9);
                    SystemClock.sleep(100);
                    queryStopSuccess = false;
                    threadFlag=true;
                    stopFlag=true;
                    queryStop(2);
                    while(!queryStopSuccess){
                        SystemClock.sleep(20);
                    }
                    right_grid_position_ET.setText(String.valueOf(thisFloorPosition));
                    info_TV.setText(Response);
                    threadFlag=false;
                    stopFlag=false;
                    openButton();
                    break;
                case R.id.right_move_Y:
                    String right_grid_position = right_please_input_grid_position.getText().toString().trim();
                    if(!right_grid_position.equals("")){
                        if(Integer.parseInt(right_grid_position) > 0 && Integer.parseInt(right_grid_position) < 1000){
                            closeButton();
                            motorControl.moveFloorByMapan(2, rimZNum2++, Integer.parseInt(right_grid_position));
                            SystemClock.sleep(delay);
                            openButton();
                        }else{
                            AlertDialog.Builder message = new AlertDialog.Builder(this);
                            message.setTitle("Error");
                            message.setMessage("请输入大于0，小于1000的数字");
                            message.setPositiveButton("OK", null);
                            message.show();
                            break;
                        }
                    }else{
                        AlertDialog.Builder message = new AlertDialog.Builder(this);
                        message.setTitle("Error");
                        message.setMessage("请输入数字");
                        message.setPositiveButton("OK", null);
                        message.show();
                        break;
                    }
                    break;
                case R.id.left_floot_position_1:
                    if(left_floot_position_1.getText().toString().equals(getResources().getString(R.string.left_floot_position_1))){
                        left_floot_position_1.setText(left_grid_position_ET.getText().toString());
                    }else{
                        left_floot_position_1.setText(getResources().getString(R.string.left_floot_position_1));
                    }
                    break;
                case R.id.left_floot_position_2:
                    if(left_floot_position_2.getText().toString().equals(getResources().getString(R.string.left_floot_position_2))){
                        left_floot_position_2.setText(left_grid_position_ET.getText().toString());
                    }else{
                        left_floot_position_2.setText(getResources().getString(R.string.left_floot_position_2));
                    }
                    break;
                case R.id.left_floot_position_3:
                    if(left_floot_position_3.getText().toString().equals(getResources().getString(R.string.left_floot_position_3))){
                        left_floot_position_3.setText(left_grid_position_ET.getText().toString());
                    }else{
                        left_floot_position_3.setText(getResources().getString(R.string.left_floot_position_3));
                    }
                    break;
                case R.id.left_floot_position_4:
                    if(left_floot_position_4.getText().toString().equals(getResources().getString(R.string.left_floot_position_4))){
                        left_floot_position_4.setText(left_grid_position_ET.getText().toString());
                    }else{
                        left_floot_position_4.setText(getResources().getString(R.string.left_floot_position_4));
                    }
                    break;
                case R.id.left_floot_position_5:
                    if(left_floot_position_5.getText().toString().equals(getResources().getString(R.string.left_floot_position_5))){
                        left_floot_position_5.setText(left_grid_position_ET.getText().toString());
                    }else{
                        left_floot_position_5.setText(getResources().getString(R.string.left_floot_position_5));
                    }
                    break;
                case R.id.left_floot_position_6:
                    if(left_floot_position_6.getText().toString().equals(getResources().getString(R.string.left_floot_position_6))){
                        left_floot_position_6.setText(left_grid_position_ET.getText().toString());
                    }else{
                        left_floot_position_6.setText(getResources().getString(R.string.left_floot_position_6));
                    }
                    break;
                case R.id.left_out_position:
                    if(left_out_position.getText().toString().equals(getResources().getString(R.string.Null))){
                        left_out_position.setText(left_grid_position_ET.getText().toString());
                    }else{
                        left_out_position.setText(getResources().getString(R.string.Null));
                    }
                    break;
                case R.id.right_floot_position_1:
                    if(right_floot_position_1.getText().toString().equals(getResources().getString(R.string.right_floot_position_1))){
                        right_floot_position_1.setText(right_grid_position_ET.getText().toString());
                    }else{
                        right_floot_position_1.setText(getResources().getString(R.string.right_floot_position_1));
                    }
                    break;
                case R.id.right_floot_position_2:
                    if(right_floot_position_2.getText().toString().equals(getResources().getString(R.string.right_floot_position_2))){
                        right_floot_position_2.setText(right_grid_position_ET.getText().toString());
                    }else{
                        right_floot_position_2.setText(getResources().getString(R.string.right_floot_position_2));
                    }
                    break;
                case R.id.right_floot_position_3:
                    if(right_floot_position_3.getText().toString().equals(getResources().getString(R.string.right_floot_position_3))){
                        right_floot_position_3.setText(right_grid_position_ET.getText().toString());
                    }else{
                        right_floot_position_3.setText(getResources().getString(R.string.right_floot_position_3));
                    }
                    break;
                case R.id.right_floot_position_4:
                    if(right_floot_position_4.getText().toString().equals(getResources().getString(R.string.right_floot_position_4))){
                        right_floot_position_4.setText(right_grid_position_ET.getText().toString());
                    }else{
                        right_floot_position_4.setText(getResources().getString(R.string.right_floot_position_4));
                    }
                    break;
                case R.id.right_floot_position_5:
                    if(right_floot_position_5.getText().toString().equals(getResources().getString(R.string.right_floot_position_5))){
                        right_floot_position_5.setText(right_grid_position_ET.getText().toString());
                    }else{
                        right_floot_position_5.setText(getResources().getString(R.string.right_floot_position_5));
                    }
                    break;
                case R.id.right_floot_position_6:
                    if(right_floot_position_6.getText().toString().equals(getResources().getString(R.string.right_floot_position_6))){
                        right_floot_position_6.setText(right_grid_position_ET.getText().toString());
                    }else{
                        right_floot_position_6.setText(getResources().getString(R.string.right_floot_position_6));
                    }
                    break;
                case R.id.right_out_position:
                    if(right_out_position.getText().toString().equals(getResources().getString(R.string.Null))){
                        right_out_position.setText(right_grid_position_ET.getText().toString());
                    }else{
                        right_out_position.setText(getResources().getString(R.string.Null));
                    }
                    break;
                case R.id.confirm_all:
                    MachineStateDao dao = new MachineStateDaoImpl(getApplicationContext());
                    String left_floot_position = "";
                    String left_out = "";
                    int left_floot_no = 0;
                    if(left_floot_position_1.getText().toString().trim().equals(getResources().getString(R.string.left_floot_position_1))){
                        left_floot_position += "0 ";
                    }else{
                        left_floot_position += left_floot_position_1.getText().toString().trim();
                        left_floot_position += " ";
                        left_floot_no++;
                    }
                    if(left_floot_position_2.getText().toString().trim().equals(getResources().getString(R.string.left_floot_position_2))){
                        left_floot_position += "0 ";
                    }else{
                        left_floot_position += left_floot_position_2.getText().toString().trim();
                        left_floot_position += " ";
                        left_floot_no++;
                    }
                    if(left_floot_position_3.getText().toString().trim().equals(getResources().getString(R.string.left_floot_position_3))){
                        left_floot_position += "0 ";
                    }else{
                        left_floot_position += left_floot_position_3.getText().toString().trim();
                        left_floot_position += " ";
                        left_floot_no++;
                    }
                    if(left_floot_position_4.getText().toString().trim().equals(getResources().getString(R.string.left_floot_position_4))){
                        left_floot_position += "0 ";
                    }else{
                        left_floot_position += left_floot_position_4.getText().toString().trim();
                        left_floot_position += " ";
                        left_floot_no++;
                    }
                    if(left_floot_position_5.getText().toString().trim().equals(getResources().getString(R.string.left_floot_position_5))){
                        left_floot_position += "0 ";
                    }else{
                        left_floot_position += left_floot_position_5.getText().toString().trim();
                        left_floot_position += " ";
                        left_floot_no++;
                    }
                    if(left_floot_position_6.getText().toString().trim().equals(getResources().getString(R.string.left_floot_position_6))){
                        left_floot_position += "0";
                    }else{
                        left_floot_position += left_floot_position_6.getText().toString().trim();
                        left_floot_no++;
                    }
                    if(left_out_position.getText().toString().trim().equals(getResources().getString(R.string.Null))){
                        left_out = "0";
                    }else{
                        left_out = left_out_position.getText().toString().trim();
                    }
                    dao.updateSetting(1, left_floot_no, Integer.parseInt(left_out),left_floot_position);
                    String right_floot_position = "";
                    String right_out = "";
                    int right_floot_no = 0;
                    if(right_floot_position_1.getText().toString().trim().equals(getResources().getString(R.string.right_floot_position_1))){
                        right_floot_position += "0 ";
                    }else{
                        right_floot_position += right_floot_position_1.getText().toString().trim();
                        right_floot_position += " ";
                        right_floot_no++;
                    }
                    if(right_floot_position_2.getText().toString().trim().equals(getResources().getString(R.string.right_floot_position_2))){
                        right_floot_position += "0 ";
                    }else{
                        right_floot_position += right_floot_position_2.getText().toString().trim();
                        right_floot_position += " ";
                        right_floot_no++;
                    }
                    if(right_floot_position_3.getText().toString().trim().equals(getResources().getString(R.string.right_floot_position_3))){
                        right_floot_position += "0 ";
                    }else{
                        right_floot_position += right_floot_position_3.getText().toString().trim();
                        right_floot_position += " ";
                        right_floot_no++;
                    }
                    if(right_floot_position_4.getText().toString().trim().equals(getResources().getString(R.string.right_floot_position_4))){
                        right_floot_position += "0 ";
                    }else{
                        right_floot_position += right_floot_position_4.getText().toString().trim();
                        right_floot_position += " ";
                        right_floot_no++;
                    }
                    if(right_floot_position_5.getText().toString().trim().equals(getResources().getString(R.string.right_floot_position_5))){
                        right_floot_position += "0 ";
                    }else{
                        right_floot_position += right_floot_position_5.getText().toString().trim();
                        right_floot_position += " ";
                        right_floot_no++;
                    }
                    if(right_floot_position_6.getText().toString().trim().equals(getResources().getString(R.string.right_floot_position_6))){
                        right_floot_position += "0";
                    }else{
                        right_floot_position += right_floot_position_6.getText().toString().trim();
                        right_floot_no++;
                    }
                    if(right_out_position.getText().toString().trim().equals(getResources().getString(R.string.Null))){
                        right_out = "0";
                    }else{
                        right_out = right_out_position.getText().toString().trim();
                    }
                    dao.updateSetting(2, right_floot_no, Integer.parseInt(right_out),right_floot_position);
                    break;
                case R.id.test_activity_button:
                    Intent intent = new Intent(SettingActivity.this, SettingTestActivty.class);
                    startActivity(intent);
                    break;
                case R.id.return_back_button:
                    stopFlag = false;
                    this.finish();
                    hintKbTwo();
                    VMMainThreadFlag = true;
                    mQuery1Flag = true;
                    mQuery2Flag = true;
                    mQuery0Flag = true;
                    mUpdataDatabaseFlag = true;
                    Util.WriteFile("返回上位机，开启主线程");
                    break;
//                case R.id.delayButton:
//                    delayGetDoor = Integer.parseInt(delayEditText.getText().toString());
//                    VMApplication.setDelayGetDoor(getApplicationContext(),Integer.parseInt(delayEditText.getText().toString()));
//                    break;
                case R.id.temp_minus_TV:
                    if(Double.parseDouble(temp_setting.getText().toString().trim()) > 1.0 && Double.parseDouble(temp_setting.getText().toString().trim()) <= 5.0){
                        temp_setting.setText(df.format(Double.parseDouble(temp_setting.getText().toString().trim()) - 0.5));
                    }else{
                        temp_setting.setText("5.0");
                    }
                    break;
                case R.id.temp_add_TV:
                    if(Double.parseDouble(temp_setting.getText().toString().trim()) >= 1.0 && Double.parseDouble(temp_setting.getText().toString().trim()) < 5.0){
                        temp_setting.setText(df.format(Double.parseDouble(temp_setting.getText().toString().trim()) + 0.5));
                    }else{
                        temp_setting.setText("1.0");
                    }
                    break;
                case R.id.user_defined:
                    if(user_defined.getText().toString().equals(getResources().getString(R.string.user_defined_close))) {
                        int temp = (int) (Double.parseDouble(temp_setting.getText().toString().trim()) * 10);
                        if(!compressor_max_continuous_worktime.getText().toString().trim().equals("")){
                            int worktime = Integer.parseInt(compressor_max_continuous_worktime.getText().toString().trim());
                            if (temp >= 10 && temp <= 50 && worktime >=1 && worktime<=10) {
                                VMApplication.setTempUserDefined(getApplicationContext(),String.valueOf(temp),String.valueOf(worktime),"open");
                                user_defined.setText(getResources().getString(R.string.user_defined_open));
                                user_defined.setTextColor(getResources().getColor(R.color.green_1));
                            }
                        }
                    }else{
                        temp_setting.setText("1.0");
                        compressor_max_continuous_worktime.setText("");
                        VMApplication.setTempUserDefined(getApplicationContext(),"","","close");
                        user_defined.setText(getResources().getString(R.string.user_defined_close));
                        user_defined.setTextColor(getResources().getColor(R.color.black));
                    }
                    break;
            }
        }
    }
    private void closeButton() {
        closeButton = true;
    }
    private void openButton() {
        closeButton = false;
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

    public void queryStop(final int counter) {
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
                                            thisFloorPosition = (rec[16] & 0xff) * 256 + (rec[17] & 0xff);
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
