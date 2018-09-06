package com.njust.major.dao;

import com.njust.major.bean.Malfunction;

import java.util.List;


public interface MalfunctionDao {

    public void addMalfunction(Malfunction Malfunction);

    public void deleteMalfunction(String transactionID);

    public void deleteAllMalfunction();

    public void updateMalfunction(Malfunction Malfunction);

    public List<Malfunction> queryMalfunction();

    public Malfunction queryMalfunction(String TransactionID);
}
