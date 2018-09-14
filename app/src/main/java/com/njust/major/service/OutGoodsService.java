package com.njust.major.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.njust.major.thread.OutGoodsThread;
import com.njust.major.util.Util;

import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.OutGoodsThreadFlag;


public class OutGoodsService extends Service {
    private OutGoodsThread mOutGoodsThread;


    @Override
    public void onCreate(){
        super.onCreate();
        VMMainThreadFlag = false;
        OutGoodsThreadFlag = true;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
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
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
