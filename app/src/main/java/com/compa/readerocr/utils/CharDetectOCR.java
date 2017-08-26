package com.compa.readerocr.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.os.Environment;

import com.googlecode.tesseract.android.TessBaseAPI;

/**
 * Detect text from image
 * 
 * @author vmtram
 *
 */
public class CharDetectOCR {
	static TessBaseAPI mTess;

	/**
	 * Initial the data
	 * @param assetMana
	 * @return
	 */
	public static boolean init(AssetManager assetMana) {
		mTess = new TessBaseAPI();
		String datapath = CommonUtils.APP_PATH;
		File dir = new File(datapath + "tessdata/");
		if (!dir.exists()) {
			dir.mkdirs();
			try {
				// Write training text data to external storage
				// Maybe take time for first install app
				InputStream inStream = assetMana.open("CSDL/eng.traineddata");
				FileOutputStream outStream = new FileOutputStream(datapath + "tessdata/eng.traineddata");
				byte[] buffer = new byte[1024];
				int readCount = 0;
				while ((readCount = inStream.read(buffer)) != -1) {
					outStream.write(buffer, 0, readCount);
				}
				outStream.flush();
				outStream.close();

			} catch (Exception e) {
				e.printStackTrace();
			}

		}
		mTess.init(datapath, "eng"); // English
		return true;
	}

	public String getOCRResult(Bitmap bitmap) {

		mTess.setImage(bitmap);
		String result = mTess.getUTF8Text();

		return result;
	}

	public void onDestroy() {
		if (mTess != null)
			mTess.end();
	}

}
