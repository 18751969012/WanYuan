package com.njust;

import android.Manifest;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;

import com.njust.major.database.MyOpenHelper;
import com.njust.major.service.OutGoodsService;
import com.njust.major.thread.VMMainThread;
import com.njust.major.util.Util;

import java.io.File;

public class VMApplication extends Application {

	public static boolean VMMainThreadFlag = true;
	public static boolean VMMainThreadRunning = true;
	public static boolean mQuery0Flag = true; //查询中柜、左柜、右柜机器状态
	public static boolean mQuery1Flag = true;
	public static boolean mQuery2Flag = true;
	public static boolean mUpdataDatabaseFlag = true;//更新数据库
	public static boolean OutGoodsThreadFlag = false;
	public static int rimZNum1 = 5;
	public static int rimZNum2 = 5;
	public static int aisleZNum1 = 5;
	public static int aisleZNum2 = 5;
	public static int midZNum = 5;
	public static String current_transaction_order_number = "";

	public static int delayGetDoor = 500;

	public static String wuOrsihuodaoxian = "13579_1357";//"12345_2345"   "13579_1357"



	static {
		File destFolder = new File(Environment.getExternalStorageDirectory()
				.getAbsoluteFile() + "/VM");
		if (!destFolder.exists()) {
			destFolder.mkdir();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
//		if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
//			//没有权限则申请权限
//			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);
//		}
//		if (ContextCompat.checkSelfPermission(context,Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED){
//			//没有权限则申请权限
//			ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},1);
//		}
//		Uri packageURI = Uri.parse("package:" + getApplicationContext().getPackageName());
//		Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageURI);
//		startActivity(intent);

//		ActivityCompat.requestPermissions(, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
		MyCrashHandler myCrashHandler = MyCrashHandler.getInstance();
		myCrashHandler.init(getApplicationContext());
		MyOpenHelper myOpenHelper = new MyOpenHelper(getApplicationContext());
		SQLiteDatabase database = myOpenHelper.getReadableDatabase();
		database.close();
		delayGetDoor = getDelayGetDoor(getApplicationContext());
		wuOrsihuodaoxian = getWuOrsihuodaoxian(getApplicationContext());
	}

	/**
	 * 获得当前delayGetDoor值
	 */
	public static int getDelayGetDoor(Context context) {
		SharedPreferences sp = context.getSharedPreferences("binding_info", Context.MODE_PRIVATE);
		return sp.getInt("delayGetDoor", 500);
	}

	/**
	 * 设置当前delayGetDoor值
	 */
	public static void setDelayGetDoor(Context context, int delayGetDoor) {
		SharedPreferences sp = context.getSharedPreferences("binding_info", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putInt("delayGetDoor", delayGetDoor);
		editor.commit();
	}

	/**
	 * 获得当前wuOrsihuodaoxian值
	 */
	public static String getWuOrsihuodaoxian(Context context) {
		SharedPreferences sp = context.getSharedPreferences("binding_info", Context.MODE_PRIVATE);
		return sp.getString("wuOrsihuodaoxian", "13579_1357");
	}

	/**
	 * 设置当前wuOrsihuodaoxian值
	 */
	public static void setWuOrsihuodaoxian(Context context, String wuOrsihuodaoxian) {
		SharedPreferences sp = context.getSharedPreferences("binding_info", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("wuOrsihuodaoxian", wuOrsihuodaoxian);
		editor.commit();
	}
	/**
	 * 设置当前温控回差、当前压缩机最长连续工作时间、当前温度自定义是否开启
	 */
	public static void setTempUserDefined(Context context,String tempControlHuiCha, String compressorMaxWorktime, String tempUserDefined) {
		SharedPreferences sp = context.getSharedPreferences("binding_info", Context.MODE_PRIVATE);
		SharedPreferences.Editor editor = sp.edit();
		editor.putString("TempControlHuiCha", tempControlHuiCha);
		editor.putString("CompressorMaxWorktime", compressorMaxWorktime);
		editor.putString("TempUserDefined", tempUserDefined);
		editor.apply();
	}
	/**
	 * 获得当前温度自定义是否开启
	 * */
	public static String getTempUserDefined(Context context) {
		SharedPreferences sp = context.getSharedPreferences("binding_info", Context.MODE_PRIVATE);
		return sp.getString("TempUserDefined", "close");
	}
	/**
	 * 获得当前温控回差
	 */
	public static String getTempControlHuiCha(Context context) {
		SharedPreferences sp = context.getSharedPreferences("binding_info", Context.MODE_PRIVATE);
		return sp.getString("TempControlHuiCha", "");
	}
	/**
	 * 获得当前压缩机最长连续工作时间
	 * */
	public static String getCompressorMaxWorktime(Context context) {
		SharedPreferences sp = context.getSharedPreferences("binding_info", Context.MODE_PRIVATE);
		return sp.getString("CompressorMaxWorktime", "");
	}
}
