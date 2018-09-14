package com.njust.major.bean;



public class Transaction {

    private int _id;
    private String orderNO;    //交易编号,即订单号
    private int complete;
    private int type;
    private String beginTime;
    private String endTime;
    private String positionIDs;
    private int error;//本次交易是否故障，0=成功，1=故障。

    public Transaction(int _id, String orderNO, int complete, int type, String beginTime, String endTime, String positionIDs, int error) {
        this._id = _id;
        this.orderNO = orderNO;
        this.complete = complete;
        this.type = type;
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.positionIDs = positionIDs;
        this.error = error;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public String getOrderNO() {
        return orderNO;
    }

    public void setOrderNO(String orderNO) {
        this.orderNO = orderNO;
    }

    public int getComplete() {
        return complete;
    }

    public void setComplete(int complete) {
        this.complete = complete;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getBeginTime() {
        return beginTime;
    }

    public void setBeginTime(String beginTime) {
        this.beginTime = beginTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getPositionIDs() {
        return positionIDs;
    }

    public void setPositionIDs(String positionIDs) {
        this.positionIDs = positionIDs;
    }

    public int getError() {
        return error;
    }

    public void setError(int error) {
        this.error = error;
    }

    @Override
    public String toString() {
        return "Transaction{" +
                "_id=" + _id +
                ", orderNO='" + orderNO + '\'' +
                ", complete=" + complete +
                ", type=" + type +
                ", beginTime='" + beginTime + '\'' +
                ", endTime='" + endTime + '\'' +
                ", positionIDs='" + positionIDs + '\'' +
                ", error=" + error +
                '}';
    }
}
