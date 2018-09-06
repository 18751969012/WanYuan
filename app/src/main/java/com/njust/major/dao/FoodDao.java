package com.njust.major.dao;

import com.njust.major.bean.Foodstuff;

import java.util.List;

public interface FoodDao {

    public void add(Foodstuff foodstuff);

    public void delete(int counter, int position);

    public void update(Foodstuff foodstuff);

    public void updateState(int counter, int positionID, int state);//

    public void updateAllState(int counter, int state);//改变一整个边柜的货物状态

    public void updatePositionIDStock(int counter, int positionID);//指定货道库存减1，如果没有减完没有货了，则关闭本货道物品的state

    public Foodstuff query(int counter, int positionID);

    public List<Foodstuff> queryAll();

    public List<Foodstuff> queryByFoodID(int foodID);
}
