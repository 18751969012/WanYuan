package com.njust;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.njust.major.database.MyOpenHelper;
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

	private VMMainThread mainThread;

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
		Util.WriteFile("VMApplication启动");
		MyCrashHandler myCrashHandler = MyCrashHandler.getInstance();
		myCrashHandler.init(getApplicationContext());
		MyOpenHelper myOpenHelper = new MyOpenHelper(getApplicationContext());
		SQLiteDatabase database = myOpenHelper.getReadableDatabase();
		database.close();
		mainThread = new VMMainThread(getApplicationContext());
		mainThread.initMainThread();
		mainThread.start();
		Util.WriteFile("主线程启动");
	}

}
