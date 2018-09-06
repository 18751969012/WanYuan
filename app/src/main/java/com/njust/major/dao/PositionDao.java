package com.njust.major.dao;

import com.njust.major.bean.Position;

import java.util.List;



public interface PositionDao {

    public void addPosition(Position position);

    public void deletePosition(int PositionID ,int Counter);

    public void deleteAllPosition();

    public void updatePosition(Position position);

    public void updatePosition(int counter, int positionID, int state);

    public List<Position> queryPosition();

    public Position queryPosition(int PositionID, int Counter);
}
