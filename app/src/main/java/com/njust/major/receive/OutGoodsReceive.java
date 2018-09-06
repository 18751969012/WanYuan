package com.njust.major.receive;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import static com.njust.VMApplication.current_transaction_order_number;
import com.njust.major.service.OutGoodsService;
import com.njust.major.util.Util;

import java.util.Arrays;

public class OutGoodsReceive extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // 收到广播开启服务
        Log.w("happy","OutGoodsReceive接收到了");
        Util.WriteFile("OutGoodsReceive接收到了");
        current_transaction_order_number = intent.getStringExtra("transaction_order_number");
        Intent service = new Intent(context, OutGoodsService.class);
        try{
            context.stopService(service);
        }catch (Exception e){
            e.printStackTrace();
        }
        context.startService(service);
    }
}
