package com.compa.readerocr.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.os.Environment;
import android.util.Log;

/**
 * 
 * @author Sea
 *
 */
public class CommonUtils {
	public static String TAG = "COMPA";
	public static String EXAM_CFG = "exam.txt";
	public static String REGEX_QUESTION_ANSWER = ":|=";
	public static String REGEX_QUESTION = "\\.|,";
	public static String APP_PATH = Environment.getExternalStorageDirectory() + "/RecognizeTextOCR/";

	/**
	 * Clear temp image in folder APP_PATH
	 */
	public static void cleanFolder() {
		info("Create or empty folder");
		String datapath = APP_PATH;
		File tenpPath = new File(datapath);
		if (!tenpPath.exists()) {
			if (!tenpPath.mkdir()) {
				// Can not create path
			}
		} else {
			for (File child : tenpPath.listFiles()) {
				// Keep only config files
				if (!child.getName().contains(".txt")) {
					child.delete();
				}
			}
		}
	}

	public static void info(Object msg) {
		Log.i(TAG, msg.toString());
	}

}
