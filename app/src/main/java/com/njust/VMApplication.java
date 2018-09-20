package com.njust;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.njust.major.database.MyOpenHelper;

import java.io.File;

public class VMApplication extends Application {
	public static boolean VMMainThreadFlag = true;
	public static boolean OutGoodsThreadFlag = false;
	public static int rimZNum1 = 5;
	public static int rimZNum2 = 5;
	public static int aisleZNum1 = 5;
	public static int aisleZNum2 = 5;
	public static int midZNum = 5;
	public static String current_transaction_order_number = "";

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
		MyCrashHandler myCrashHandler = MyCrashHandler.getInstance();
		myCrashHandler.init(getApplicationContext());
		MyOpenHelper myOpenHelper = new MyOpenHelper(getApplicationContext());
		SQLiteDatabase database = myOpenHelper.getReadableDatabase();
		database.close();
	}

}
