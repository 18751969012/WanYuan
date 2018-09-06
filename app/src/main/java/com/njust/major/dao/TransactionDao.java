package com.njust.major.dao;

import com.njust.major.bean.Transaction;

import java.util.List;


public interface TransactionDao {

    public void addTransaction(Transaction transaction);

    public void deleteTransaction();

    public void updateTransaction(Transaction transaction);

    public Transaction queryLastedTransaction();


    public List<Transaction> getTransaction(String bTime, String eTime);
}
