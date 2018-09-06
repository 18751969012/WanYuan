package com.njust.major.bean;

public class Foodstuff {

    private int _id;
    private int foodID;    //商品编号
    private int positionID;   //商品位置
    private int stock;      //库存数量
    private int counter; //所在货柜
    private int state;//1==可以， 2==不可以
    private int price;

    public Foodstuff(int _id, int foodID, int positionID, int stock, int counter, int state, int price) {
        this._id = _id;
        this.foodID = foodID;
        this.positionID = positionID;
        this.stock = stock;
        this.counter = counter;
        this.state = state;
        this.price = price;
    }

    public int get_id() {
        return _id;
    }

    public void set_id(int _id) {
        this._id = _id;
    }

    public int getFoodID() {
        return foodID;
    }

    public void setFoodID(int foodID) {
        this.foodID = foodID;
    }

    public int getPositionID() {
        return positionID;
    }

    public void setPositionID(int positionID) {
        this.positionID = positionID;
    }

    public int getStock() {
        return stock;
    }

    public void setStock(int stock) {
        this.stock = stock;
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

    public int getPrice() {
        return price;
    }

    public void setPrice(int price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return "Foodstuff{" +
                "_id=" + _id +
                ", foodID=" + foodID +
                ", positionID=" + positionID +
                ", stock=" + stock +
                ", counter=" + counter +
                ", state=" + state +
                ", price=" + price +
                '}';
    }
}
