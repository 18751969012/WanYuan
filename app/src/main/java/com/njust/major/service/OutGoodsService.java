package com.njust.major.service;

import android.app.AlarmManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.util.Log;

import com.njust.major.bean.Transaction;
import com.njust.major.dao.TransactionDao;
import com.njust.major.dao.impl.TransactionDaoImpl;
import com.njust.major.thread.OutGoodsThread;
import com.njust.major.util.Util;

import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.OutGoodsThreadFlag;
import static com.njust.VMApplication.VMMainThreadRunning;
import static com.njust.VMApplication.mQuery0Flag;
import static com.njust.VMApplication.mQuery1Flag;
import static com.njust.VMApplication.mQuery2Flag;
import static com.njust.VMApplication.mUpdataDatabaseFlag;


public class OutGoodsService extends Service {
    private OutGoodsThread mOutGoodsThread;

    @Override
    public void onCreate(){
        super.onCreate();
        if(!OutGoodsThreadFlag){
            TransactionDao transactionDao = new TransactionDaoImpl(getApplicationContext());
            Transaction queryLastedTransaction = transactionDao.queryLastedTransaction();
            if(queryLastedTransaction != null){
                if(queryLastedTransaction.getComplete() == 0){
                    startOutGoodsThread();
                }
            }else{
                startOutGoodsThread();
            }
        }
    }

    private void startOutGoodsThread(){
        VMMainThreadFlag = false;
        mQuery1Flag = false;
        mQuery2Flag = false;
        mQuery0Flag = false;
        mUpdataDatabaseFlag = false;
        OutGoodsThreadFlag = true;
        SystemClock.sleep(20);
        while(VMMainThreadRunning){
            SystemClock.sleep(20);
        }
        mOutGoodsThread = new OutGoodsThread(getApplicationContext());
        mOutGoodsThread.init();
        mOutGoodsThread.start();

        Log.w("happy", "出货主线程开启");
        Util.WriteFile("出货主线程开启");
    }
    @Override
    public void onDestroy() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
