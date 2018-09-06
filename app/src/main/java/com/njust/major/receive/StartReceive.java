package com.njust.major.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.njust.major.service.VMService;
import com.njust.major.util.Util;


public class StartReceive extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent arg1) {
        // 收到广播开启服务
        Log.w("happy","StartReceive接收到了");
        Util.WriteFile("StartReceive接收到了");
        Intent service = new Intent(context, VMService.class);
        try{
            context.stopService(service);

        }catch (Exception e){
            e.printStackTrace();
        }
        context.startService(service);

    }
}

