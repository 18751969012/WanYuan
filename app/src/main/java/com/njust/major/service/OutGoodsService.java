package com.njust.major.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.njust.major.thread.ReceiveThread;
import com.njust.major.thread.ReceiveThreadAssist;
import com.njust.major.thread.SendThread;
import com.njust.major.error.errorHandling;
import com.njust.major.util.Util;

import java.io.IOException;

import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.SendThreadFlag;
import static com.njust.VMApplication.ReceiveThreadFlag;


public class OutGoodsService extends Service {

    private ReceiveThread mReceiveThread;
    private ReceiveThreadAssist mReceiveThreadAssist;
    private SendThread mSendThread;


    @Override
    public void onCreate(){
        super.onCreate();
        Log.w("happy", "主线程停止，接收发送线程主标志位开启");
        Util.WriteFile("主线程停止，接收发送线程主标志位开启");
        VMMainThreadFlag = false;
        SendThreadFlag = true;
        ReceiveThreadFlag = true;
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        mReceiveThreadAssist = new ReceiveThreadAssist(getApplicationContext());
        mReceiveThread = new ReceiveThread(getApplicationContext(),mReceiveThreadAssist);
        mSendThread = new SendThread(getApplicationContext(), mReceiveThread, mReceiveThreadAssist);
        mReceiveThreadAssist.initReceiveThread();
        mReceiveThread.initReceiveThread();
        mSendThread.initSendThread();
        mReceiveThreadAssist.start();
        mReceiveThread.start();
        mSendThread.start();
        Log.w("happy", "接收发送线程开启");
        Util.WriteFile("接收发送线程开启");
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
