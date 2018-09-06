package com.njust.major.bean;

public class Malfunction {

    private int _id;
    private String transactionID;    //交易编号,即订单号
    private String errorTime;      //故障时间
    private int counter; //所在货柜1=左柜（主柜），2=右柜（辅柜）
    private String errorModule;      //故障组件
    private String errorDescription; //故障描述
    private int motorRealActionTime;//电机实际动作时间（毫秒）
    private int motorMaxElectricity;//电机最大电流（毫安）
    private int motorAverageElectricity;//电机平均电流（毫安）

    public Malfunction(int _id, String transactionID,String errorTime, int counter, String errorModule, String errorDescription, int motorRealActionTime, int motorMaxElectricity, int motorAverageElectricity) {
        this._id = _id;
        this.transactionID = transactionID;
        this.errorTime = errorTime;
        this.counter = counter;
        this.errorModule = errorModule;
        this.errorDescription = errorDescription;
        this.motorRealActionTime = motorRealActionTime;
        this.motorMaxElectricity = motorMaxElectricity;
        this.motorAverageElectricity = motorAverageElectricity;
    }

    public Malfunction() {
        super();
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getTransactionID() {
        return transactionID;
    }

    public void setTransactionID(String transactionID) {
        this.transactionID = transactionID;
    }

    public String getErrorTime() {
        return errorTime;
    }

    public void setErrorTime(String errorTime) {
        this.errorTime = errorTime;
    }

    public int getCounter() {
        return counter;
    }

    public void setCounter(int counter) {
        this.counter = counter;
    }
    public String getErrorModule() {
        return errorModule;
    }

    public void setErrorModule(String errorModule) {
        this.errorModule = errorModule;
    }

    public String getErrorDescription() {
        return errorDescription;
    }

    public void setErrorDescription(String errorDescription) {
        this.errorDescription = errorDescription;
    }

    public int getMotorRealActionTime() {
        return motorRealActionTime;
    }

    public void setMotorRealActionTime(int motorRealActionTime) {
        this.motorRealActionTime = motorRealActionTime;
    }

    public int getMotorMaxElectricity() {
        return motorMaxElectricity;
    }

    public void setMotorMaxElectricity(int motorMaxElectricity) {
        this.motorMaxElectricity = motorMaxElectricity;
    }

    public int getMotorAverageElectricity() {
        return motorAverageElectricity;
    }

    public void setMotorAverageElectricity(int motorAverageElectricity) {
        this.motorAverageElectricity = motorAverageElectricity;
    }

    @Override
    public String toString() {
        return "Malfunction{" +
                "_id=" + _id +
                ", transactionID=" + transactionID +
                ", errorModule='" + errorModule + '\'' +
                ", errorDescription='" + errorDescription + '\'' +
                ", motorRealActionTime=" + motorRealActionTime +
                ", motorMaxElectricity=" + motorMaxElectricity +
                ", motorAverageElectricity=" + motorAverageElectricity +
                '}';
    }
}
