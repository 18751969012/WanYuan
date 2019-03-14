package com.njust.major.service;

import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

import com.njust.major.bean.MachineState;
import com.njust.major.bean.Transaction;
import com.njust.major.dao.MachineStateDao;
import com.njust.major.dao.TransactionDao;
import com.njust.major.dao.impl.MachineStateDaoImpl;
import com.njust.major.dao.impl.TransactionDaoImpl;
import com.njust.major.thread.VMMainThread;
import com.njust.major.util.Util;

import static com.njust.VMApplication.VMMainThreadFlag;
import static com.njust.VMApplication.mQuery0Flag;
import static com.njust.VMApplication.mQuery1Flag;
import static com.njust.VMApplication.mQuery2Flag;
import static com.njust.VMApplication.mUpdataDatabaseFlag;


public class VMService extends Service {

    private TransactionDao tDao;
    private VMMainThread mainThread;

    @Override
    public void onCreate(){
        super.onCreate();
        Log.w("happy", "VMService启动");
        Util.WriteFile("VMService启动");
        updateVersion();
        tDao = new TransactionDaoImpl(getApplicationContext());
        Transaction transaction = tDao.queryLastedTransaction();
        if(transaction != null) {
            if (transaction.getComplete() == 0) {
                transaction.setComplete(1);
                tDao.updateTransaction(transaction);
            }
        }
        VMMainThreadFlag = true;
        mQuery1Flag = true;
        mQuery2Flag = true;
        mQuery0Flag = true;
        mUpdataDatabaseFlag = true;
        mainThread = new VMMainThread(getApplicationContext());
        mainThread.initMainThread();
        mainThread.start();
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

    private String getVersionName() {
        String version = "";
        try {
            // 获取packagemanager的实例
            PackageManager packageManager = getPackageManager();
            // getPackageName()是你当前类的包名，0代表是获取版本信息
            PackageInfo packInfo;
            packInfo = packageManager.getPackageInfo(getPackageName(), 0);
            version = packInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return version;
    }

    private void updateVersion() {
        MachineStateDao mDao = new MachineStateDaoImpl(getApplicationContext());
        mDao.updateVersion(getVersionName());
    }
}
