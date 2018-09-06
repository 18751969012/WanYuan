package com.njust.major.bean;


public class Position {

    private int positionID;
    private int counter;//1=左柜（主柜），2=右柜（辅柜）
    private int state;//是否能用1 = 正常，0 = 关闭
    private int motorType;//电机类型，1=单弹簧机，2=双弹簧机，3=窄履带机，4=宽弹簧机
    private byte position1;//行+列1
    private byte position2;//行+列2，如果没合并货道，则列1列2相同

    public Position(int positionID, int counter, int state, int motorType, byte position1, byte position2) {
        this.positionID = positionID;
        this.counter = counter;
        this.state = state;
        this.motorType = motorType;
        this.position1 = position1;
        this.position2 = position2;
    }

    public int getPositionID() {
        return positionID;
    }

    public void setPositionID(int positionID) {
        this.positionID = positionID;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }

    public int getState() {
        return state;
    }

    public void setState(int state) {
        this.state = state;
    }

    public int getMotorType() {
        return motorType;
    }

    public void setMotorType(int motorType) {
        this.motorType = motorType;
    }

    public byte getPosition1() {
        return position1;
    }

    public void setPosition1(byte position1) {
        this.position1 = position1;
    }

    public byte getPosition2() {
        return position2;
    }

    public void setPosition2(byte position2) {
        this.position2 = position2;
    }

    @Override
    public String toString() {
        return "Position{" +
                "positionID=" + positionID +
                ", counter=" + counter +
                ", state=" + state +
                ", motorType=" + motorType +
                ", position1=" + position1 +
                ", position2=" + position2 +
                '}';
    }
}
