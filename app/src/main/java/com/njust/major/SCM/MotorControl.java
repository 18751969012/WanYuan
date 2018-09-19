package com.njust.major.SCM;

import android.content.Context;
import android.util.Log;

import com.njust.SerialPort;
import com.njust.major.bean.MachineState;
import com.njust.major.bean.Position;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.PositionDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.dao.impl.PositionDaoImpl;
import com.njust.major.dao.impl.TransactionDaoImpl;

import java.util.Arrays;


public class MotorControl {

    private MachineStateDao mDao;
    private PositionDao pDao;
    private SerialPort motorPort;
    private byte[] MotorControlTXBuf;


    public MotorControl(SerialPort serialPort485, Context context) {
        motorPort = serialPort485;
        mDao = new MachineStateDaoImpl(context);
        pDao = new PositionDaoImpl(context);
        new TransactionDaoImpl(context);
    }

    /**
     * 垂直移层_货道指令
     * 说明：边柜板驱动Y电机升降云台到指定层位
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * @param positionID 货物位置的ID号，可以查出对应的实际地址，使用其中的层号信息
     * */
    public void moveFloor(int counter ,int zhenNumber ,int positionID) {
        MachineState queryMachineState = mDao.queryMachineState();
        MotorControlTXBuf = new byte[10];
        Position p1 = pDao.queryPosition(positionID ,counter);
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x0A;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x01;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        if(counter == 1){
            MotorControlTXBuf[6] = (byte) ((queryMachineState.getLeftFlootPosition((p1.getPosition1()/16 - 1)) >> 8) & 0xFF);
            MotorControlTXBuf[7] = (byte) (queryMachineState.getLeftFlootPosition((p1.getPosition1()/16 - 1)) & 0xFF);
        }else{
            MotorControlTXBuf[6] = (byte) ((queryMachineState.getRightFlootPosition((p1.getPosition1()/16 - 1)) >> 8) & 0xFF);
            MotorControlTXBuf[7] = (byte) (queryMachineState.getRightFlootPosition((p1.getPosition1()/16 - 1)) & 0xFF);
        }
        MotorControlTXBuf[8] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i <9; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[9] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 10);
    }


    /**
     * 垂直移层_出货口指令
     * 说明：边柜板驱动Y电机升降云台到出货口
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * */
    public void moveFloorOut(int counter ,int zhenNumber) {
        MachineState queryMachineState = mDao.queryMachineState();
        MotorControlTXBuf = new byte[10];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x0A;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x01;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        if(counter == 1){
            MotorControlTXBuf[6] = (byte) ((queryMachineState.getLeftOutPosition() >> 8) & 0xFF);
            MotorControlTXBuf[7] = (byte) (queryMachineState.getLeftOutPosition() & 0xFF);
        }else{
            MotorControlTXBuf[6] = (byte) ((queryMachineState.getRightOutPosition() >> 8) & 0xFF);
            MotorControlTXBuf[7] = (byte) (queryMachineState.getRightOutPosition() & 0xFF);
        }
        MotorControlTXBuf[8] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i <9; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[9] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 10);
    }


    /**
     * 水平移位_错位指令
     * 说明：边柜板驱动X电机左右移动云台履带
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * @param orientation 边柜X轴电机移动方向 0=原位，1=正转，2=反转
     * @param moveTime 边柜X轴电机移动时间 单位：毫秒，orientation = 0时无意义
     * */
    public void moveHorizontal(int counter ,int zhenNumber ,int orientation ,int moveTime) {
        MotorControlTXBuf = new byte[11];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x0B;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x02;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) orientation;
        MotorControlTXBuf[7] = (byte) ((moveTime >> 8) & 0xFF);
        MotorControlTXBuf[8] = (byte) (moveTime & 0xFF);
        MotorControlTXBuf[9] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i <10; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[10] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 11);
    }

    /**
     * 水平移位_推入出货口指令
     * 说明：边柜板驱动X电机左右移动云台履带
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * */
    public void moveHorizontalOut(int counter ,int zhenNumber, int orientation ,int moveTime) {
        MotorControlTXBuf = new byte[11];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x0B;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x02;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) orientation;
        MotorControlTXBuf[7] = (byte) ((moveTime >> 8) & 0xFF);
        MotorControlTXBuf[8] = (byte) (moveTime & 0xFF);
        MotorControlTXBuf[9] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i <10; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[10] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 11);
    }


    /**
     * 推送货指令
     * 说明：货道板驱动P电机推送指定货道货物掉落云台
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * @param positionID 货物位置的ID号
     * */
    public void pushGoods(int counter ,int zhenNumber ,int positionID ) {
        MotorControlTXBuf = new byte[12];
        Position p1 = pDao.queryPosition(positionID, counter);
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x0C;
        MotorControlTXBuf[2] = (byte) (0x80+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x03;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) (p1.getPosition1() / 16);
        MotorControlTXBuf[7] = (byte) (p1.getPosition1() % 16);
        MotorControlTXBuf[8] = (byte) (p1.getPosition2() % 16);
        MotorControlTXBuf[9] = (byte) p1.getMotorType();
        MotorControlTXBuf[10] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[11] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 12);
        Log.w("happy", String.valueOf(p1.getPosition1()));
        Log.w("happy", String.valueOf(p1.getPosition2()));
    }


    /**
     * 开出货门指令
     * 说明：边柜板驱动Z电机开启出货门
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * */
    public void openOutGoodsDoor(int counter ,int zhenNumber) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x04;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x00;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 关出货门指令
     * 说明：边柜板驱动Z电机关闭出货门
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * */
    public void closeOutGoodsDoor(int counter ,int zhenNumber) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x05;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x00;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 云台归位指令
     * 说明：边柜板驱动Y电机降云台到零位
     * @param zhenNumber 本帧编号
     * */
    public void homing(int counter ,int zhenNumber ) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x06;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x00;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 开取货门指令
     * 说明：中柜板驱动M电机开启取货门同时开启取货仓照明灯
     * @param zhenNumber 本帧编号
     * */
    public void openGetGoodsDoor(int zhenNumber ) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) 0xE0;
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x07;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x00;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 关取货门指令
     * 说明：中柜板驱动M电机关闭取货门同时关闭取货仓照明灯
     * @param zhenNumber 本帧编号
     * */
    public void closeGetGoodsDoor(int zhenNumber ) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) 0xE0;
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x08;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x00;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 开落货门指令
     * 说明：中柜板驱动F电机开启落货门
     * @param zhenNumber 本帧编号
     * */
    public void openDropGoodsDoor(int zhenNumber ) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) 0xE0;
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x09;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x00;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 关落货门指令
     * 说明：中柜板驱动F电机关闭落货门
     * @param zhenNumber 本帧编号
     * */
    public void closeDropGoodsDoor(int zhenNumber ) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) 0xE0;
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x0A;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x00;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 边柜命令指令
     * 说明：用于下发立即执行的命令
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * @param code 命令代码1=开启照明灯，2=关闭照明灯，3=开启门加热，4=停止门加热，5=Y轴电机全速上升，6=Y轴电机全速下降，7=Y轴电机慢速上升，8=Y轴电机慢速下降
     *             9=Y轴电机停转，10=X轴电机正转一周，11=X轴电机反转一周
     * */
    public void counterCommand(int counter ,int zhenNumber, int code) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x0B;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) code;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 中柜命令指令
     * 说明：用于下发立即执行的命令
     * @param zhenNumber 本帧编号
     * @param code 1=开启照明灯，2=关闭照明灯，3=开启电磁锁
     * */
    public void centerCommand(int zhenNumber ,int code) {
        MotorControlTXBuf = new byte[10];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x0A;
        MotorControlTXBuf[2] = (byte) 0xE0;
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x0B;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x35;
        MotorControlTXBuf[7] = (byte) code;
        MotorControlTXBuf[8] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 9; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[9] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 10);
    }


    /**
     * 货道电机测试指令
     * 说明：用于测试货道电机，第n行电机依次动作（有开关动作一周，无开关动作固定时间）
     * @param counter 货柜号 1 = 左柜（主柜），2 = 右柜（辅柜）
     * @param zhenNumber 本帧编号
     * @param code 1=指定行（Y）、列（X）电机动作（弹簧电机动作一周，履带电机动作固定时间，动作完毕应答，主机无确认）
     *              2=指定行（Y）电机依次动作动作（一动作一应答，主机无确认）
     *              3=指定列（X）电机依次动作动作（一动作一应答，主机无确认）
     *              4=所有电机依次动作一周，（一动作一应答，主机无确认）
     * @param floor 驱动矩阵行坐标（1～10）
     * @param column 驱动矩阵列坐标（1～10）
     * */
    public void pushTestCommand(int counter ,int zhenNumber, int code, int floor, int column) {
        MotorControlTXBuf = new byte[12];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x0C;
        MotorControlTXBuf[2] = (byte) (0x80+counter-1);
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x0C;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) code;
        MotorControlTXBuf[7] = (byte) floor;
        MotorControlTXBuf[8] = (byte) column;
        MotorControlTXBuf[9] = (byte) 0;
        MotorControlTXBuf[10] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[11] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 12);
    }


    /**
     * 边柜查询指令
     * 说明：设定边柜的温度，并查询压缩机、机柜、机柜顶部温度，压缩机、机柜直流风扇状态，门开关状态，湿度测量值，门加热状态，照明灯状态
     * @param counter 货柜号 1=左柜（主柜），2 =右柜（辅柜）
     * @param zhenNumber 本帧编号
     * @param temperatureMode 温控模式：0x00=制冷,0x01=制热,0x02=常温
     * @param setTemperature 设定温度，温控门限，-20～70
     * */
    public void counterQuery(int counter ,int zhenNumber, int temperatureMode, int setTemperature) {
        MotorControlTXBuf = new byte[12];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x0C;
        MotorControlTXBuf[2] = (byte) (0xC0+(counter-1));
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x0D;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x65;
        MotorControlTXBuf[7] = (byte) temperatureMode;
        MotorControlTXBuf[8] = (byte) ((setTemperature >> 8) & 0xff);
        MotorControlTXBuf[9] = (byte) (setTemperature & 0xff);
        MotorControlTXBuf[10] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 11; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[11] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 12);
    }


    /**
     * 中柜查询指令
     * 说明：查询中柜门开关状态、照明灯状态、门锁状态、取货光栅状态、落货光栅状态、防夹手光栅状态、取货门开关状态、落货门开关状态
     * @param zhenNumber 本帧编号
     * */
    public void centerQuery(int zhenNumber ) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = (byte) 0xE0;
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x0D;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) 0x6D;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }

    /**
     * 查询指令
     * 说明：用于查询下位机指令执行状态
     * @param code 要查询状态的指令标识（0x01~0x0C）
     * @param address 查询指令发送的目的地址
     * @param zhenNumber 本帧编号
     * */
    public void query(byte code ,byte address,int zhenNumber) {
        MotorControlTXBuf = new byte[9];
        MotorControlTXBuf[0] = (byte) 0xE2;
        MotorControlTXBuf[1] = (byte) 0x09;
        MotorControlTXBuf[2] = address;
        MotorControlTXBuf[3] = (byte) 0x00;
        MotorControlTXBuf[4] = (byte) 0x0E;
        MotorControlTXBuf[5] = (byte) (zhenNumber & 0xFF);
        MotorControlTXBuf[6] = (byte) code;
        MotorControlTXBuf[7] = (byte) 0xF1;
        int sum = 0;
        for (int i = 0; i < 8; i++) {
            sum = sum + MotorControlTXBuf[i];
        }
        MotorControlTXBuf[8] = (byte)(sum & 0xFF);
        motorPort.sendData(MotorControlTXBuf, 9);
    }
}


































