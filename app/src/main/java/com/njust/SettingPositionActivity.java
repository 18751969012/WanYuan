package com.njust;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.njust.major.bean.Position;
import com.njust.major.dao.PositionDao;
import com.njust.major.dao.impl.PositionDaoImpl;

import java.io.PipedOutputStream;
import java.util.List;



public class SettingPositionActivity extends AppCompatActivity implements View.OnClickListener {


    private int counter = 1;
    private static final String[] typeName = new String[]{"窄履带", "宽履带", "弹簧带"};
    private int[] types = new int[]{0, 0, 0, 0, 0, 0};
    Button floor1;
    Button floor2;
    Button floor3;
    Button floor4;
    Button floor5;
    Button floor6;
    Button changeCounter;
    Button saveAll;
    Button queryPosition;
    TextView queryMsg;
    private TextView mCounter;
    private final static String LEFTCOUNTER = "当前为左柜";
    private final static String RIGHTCOUNTER = "当前为右柜";
    public PositionDao pDao;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting_position);
        mCounter = (TextView) findViewById(R.id.position_now_counter);
        queryMsg = (TextView) findViewById(R.id.position_msg);
        changeCounter = (Button) findViewById(R.id.postion_change_counter);
        floor1 = (Button) findViewById(R.id.one_floor);
        floor2 = (Button) findViewById(R.id.two_floor);
        floor3 = (Button) findViewById(R.id.three_floor);
        floor4 = (Button) findViewById(R.id.four_floor);
        floor5 = (Button) findViewById(R.id.five_floor);
        floor6 = (Button) findViewById(R.id.six_floor);
        saveAll = (Button) findViewById(R.id.save_all);
        queryPosition = (Button) findViewById(R.id.query_position);
        changeCounter.setOnClickListener(this);
        floor1.setOnClickListener(this);
        floor2.setOnClickListener(this);
        floor3.setOnClickListener(this);
        floor4.setOnClickListener(this);
        floor5.setOnClickListener(this);
        floor6.setOnClickListener(this);
        saveAll.setOnClickListener(this);
        queryPosition.setOnClickListener(this);
        pDao = new PositionDaoImpl(getApplicationContext());
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.postion_change_counter:
                changeCounter();
                break;
            case R.id.one_floor:
                types[0] = (types[0] + 1) % 3;
                floor1.setText(typeName[types[0]]);
                break;
            case R.id.two_floor:
                types[1] = (types[1] + 1) % 3;
                floor2.setText(typeName[types[1]]);
                break;
            case R.id.three_floor:
                types[2] = (types[2] + 1) % 3;
                floor3.setText(typeName[types[2]]);

                break;
            case R.id.four_floor:
                types[3] = (types[3] + 1) % 3;
                floor4.setText(typeName[types[3]]);

                break;
            case R.id.five_floor:
                types[4] = (types[4] + 1) % 3;
                floor5.setText(typeName[types[4]]);

                break;
            case R.id.six_floor:
                types[5] = (types[5] + 1) % 3;
                floor6.setText(typeName[types[5]]);

                break;
            case R.id.save_all:
                saveAll();
                break;

            case R.id.query_position:
                queryPosition();
                break;
        }
    }
    public void changeCounter(){
        counter = counter == 1 ? 2 : 1;
        if (counter == 1) {
            mCounter.setText(LEFTCOUNTER);
        } else {
            mCounter.setText(RIGHTCOUNTER);
        }
    }
    @SuppressLint("ShowToast")
    public void saveAll(){
        List<Position> positions = pDao.queryPosition();
        for (int i = 0 ; i < 6; i++){
            if (types[i] == 0){
                for (Position position : positions){
                    if ((position.getPositionID() - 1) / 10 == i && position.getCounter() == counter){
                        position.setMotorType(3);
                        position.setState(1);
                        pDao.updatePosition(position);
                    }
                }
            }
            if (types[i] == 1){
                for (Position position: positions){
                    if ((position.getPositionID() - 1) / 10 == i && position.getCounter() == counter){
                        if (position.getPositionID() % 10 <= 5 && position.getPositionID() % 10 >= 1){
                            position.setMotorType(4);
                            pDao.updatePosition(position);
                        }else {
                            position.setState(0);
                            pDao.updatePosition(position);

                        }
                    }
                }
            }
            if (types[i] == 2){
                for (Position position: positions){
                    if ((position.getPositionID()-1) / 10 == i && position.getCounter() == counter){
                        if (position.getPositionID() %10 <= 8 && position.getPositionID() %10 >= 1){
                            position.setMotorType(1);
                            position.setState(1);
                            pDao.updatePosition(position);
                        }else {
                            position.setMotorType(2);
                            position.setState(1);
                            pDao.updatePosition(position);
                            position.setPositionID(position.getPositionID() + 1);
                            position.setState(1);
                            pDao.updatePosition(position);
                        }
                    }
                }
            }
        }

        Toast.makeText(getApplicationContext(),"保存成功",Toast.LENGTH_LONG);
    }

    public void queryPosition(){
        List<Position> positions = pDao.queryPosition();
        String positionMsg = "";
        for (Position position: positions){
            if (position.getCounter() == counter){
                int tmp1 = position.getPositionID();
                int tmp2 = position.getMotorType();
                int tmp3 = position.getState();
                String tmp = "positionID = " + tmp1 + " Motor type = " + tmp2 + " state = " + tmp3 + "\n";
                positionMsg += tmp;
            }

        }
        queryMsg.setText(positionMsg);
    }
}
