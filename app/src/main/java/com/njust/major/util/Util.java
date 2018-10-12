package com.njust.major.util;

import android.os.Environment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class Util {
	public static long count = 0;
	private static FileWriter fw = null;
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS", Locale.getDefault());
	private static SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
	static {
		File destFolder = new File(Environment.getExternalStorageDirectory()
				.getAbsoluteFile() + "/Njust/log");
		if (!destFolder.exists()) {
			destFolder.mkdirs();
		}
	}

	public static String ReadFile(String Path) {
		BufferedReader reader = null;
		StringBuilder laststr = new StringBuilder();
		try {
			FileInputStream fileInputStream = new FileInputStream(Path);
			InputStreamReader inputStreamReader = new InputStreamReader(
					fileInputStream, "UTF-8");
			reader = new BufferedReader(inputStreamReader);
			String tempString = null;
			while ((tempString = reader.readLine()) != null) {
				laststr.append(tempString);
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return laststr.toString();
	}

	public static void WriteFile(String content) {
		String t = sd.format(Calendar.getInstance().getTime());
		String path = Environment.getExternalStorageDirectory()
				.getAbsoluteFile()
				+ "/Njust/log/" + t + ".text";
		String time = sdf.format(Calendar.getInstance().getTime());
		String text = time + " " + content + "\r";
		try {
			fw = new FileWriter(path, true);
			fw.write(text, 0, text.length());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}
}
