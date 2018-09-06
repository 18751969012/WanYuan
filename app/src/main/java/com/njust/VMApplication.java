package com.njust;

import android.app.Application;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;

import com.njust.major.database.MyOpenHelper;

import java.io.File;

public class VMApplication extends Application {
	public static boolean VMMainThreadFlag = true;
	public static boolean SendThreadFlag = true;
	public static boolean ReceiveThreadFlag = true;
	public static int leftZhenNumber = 0;
	public static int rightZhenNumber = 0;
	public static int midZhenNumber = 0;
	public static byte leftRimRecZhenNumber = 0x50;
	public static byte leftGoodsRecZhenNumber = 0x50;
	public static byte rightRimRecZhenNumber = 0x50;
	public static byte rightGoodsRecZhenNumber = 0x50;
	public static byte midRecZhenNumber = 0x50;
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
		MyOpenHelper myOpenHelper = new MyOpenHelper(getApplicationContext());
		SQLiteDatabase database = myOpenHelper.getReadableDatabase();
		database.close();
	}

}
