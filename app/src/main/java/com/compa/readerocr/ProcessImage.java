package com.compa.readerocr;

import static com.compa.readerocr.utils.CommonUtils.APP_PATH;
import static com.compa.readerocr.utils.CommonUtils.TAG;
import static com.compa.readerocr.utils.CommonUtils.info;
import static org.opencv.highgui.Highgui.imwrite;
import static org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY;
import static org.opencv.imgproc.Imgproc.GaussianBlur;
import static org.opencv.imgproc.Imgproc.INTER_CUBIC;
import static org.opencv.imgproc.Imgproc.MORPH_RECT;
import static org.opencv.imgproc.Imgproc.THRESH_BINARY;
import static org.opencv.imgproc.Imgproc.cvtColor;
import static org.opencv.imgproc.Imgproc.dilate;
import static org.opencv.imgproc.Imgproc.erode;
import static org.opencv.imgproc.Imgproc.getPerspectiveTransform;
import static org.opencv.imgproc.Imgproc.getStructuringElement;
import static org.opencv.imgproc.Imgproc.resize;
import static org.opencv.imgproc.Imgproc.threshold;
import static org.opencv.imgproc.Imgproc.warpPerspective;

import java.util.ArrayList;
import java.util.List;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.utils.Converters;

import com.compa.readerocr.utils.CharDetectOCR;

import android.graphics.Bitmap;
import android.util.Log;

/**
 * 
 * @author Sea
 *
 */
public class ProcessImage {

	static {
		if (!OpenCVLoader.initDebug()) {
			Log.w(TAG, "Unable to load OpenCV");
		} else {
			info("OpenCV loaded");
		}

		// For OCR
		System.loadLibrary("gnustl_shared");
		System.loadLibrary("nonfree");
	}

	public static boolean DEBUG = false; // Debug mode
	public static String language = "eng";

	public int sourceWidth = 1366; // To scale to
	public static int thresholdMin = 85; // Threshold 80 to 105 is Ok
	private int thresholdMax = 255; // Always 255

	public String recognizeResult = "";

	// Write debug image
	private void writeImage(String name, Mat origin) {
		if (!DEBUG) {
			return;
		}
		String appPath = APP_PATH;
		info("Writing " + appPath + name + "...");
		imwrite(appPath + name, origin);
	}

	public boolean parseBitmap(Bitmap bitmap, int top, int bot, int right, int left) {
		try {
			Mat origin = new Mat();
			Utils.bitmapToMat(bitmap, origin);
			info("Mat size: " + origin.width() + ":" + origin.height());
			// Crop image to get the resion inside the rectangle only
			info("Crop T: " + top + " B: " + bot + " R: " + right + " L: " + left);
			if (top != 0 && bot != 0 && right != 0 && left != 0) {
				info("Cropping...");	
				origin = origin.submat(new Rect(right, top, left, bot));
				writeImage("crop.jpg", origin);
			}

			boolean result = recognizeImage(origin);
			origin.release();
			return result;
		} catch (Exception e) {
			Log.e(TAG, "Error parse image. Message: " + e.getMessage(), e);
		}
		return false;
	}

	/**
	 * 
	 * Parse image
	 * 
	 * @param origin
	 * @return
	 */
	private boolean recognizeImage(Mat origin) {
		info("recognizeImage");
		// Reset value
		recognizeResult = "";

		// Resize origin image
		Size imgSize = origin.size();
		resize(origin, origin, new Size(sourceWidth, imgSize.height * sourceWidth / imgSize.width), 1.0, 1.0,
				INTER_CUBIC);

		writeImage("resize.jpg", origin);

		// Convert the image to GRAY
		Mat originGray = new Mat();
		cvtColor(origin, originGray, COLOR_BGR2GRAY);

		// Process noisy, blur, and threshold to get black-white image
		originGray = processNoisy(originGray);

		writeImage("gray.jpg", originGray); // Black-White image

		recognizeResult = matToString(originGray);
		info("Done recognize");

		originGray.release();
		originGray = null;

		info("Result: " + recognizeResult);
		return true;
	}

	/**
	 * Crop sub mat by four points.
	 * 
	 * @param origin
	 * @param tl
	 * @param tr
	 * @param bl
	 * @param br
	 * @return
	 */
	private Mat cropSub(Mat origin, Point tl, Point tr, Point bl, Point br) {
		info("cropSub");

		int resultWidth = (int) (tr.x - tl.x);
		int bottomWidth = (int) (br.x - bl.x);
		if (bottomWidth > resultWidth)
			resultWidth = bottomWidth;

		int resultHeight = (int) (bl.y - tl.y);
		int bottomHeight = (int) (br.y - tr.y);
		if (bottomHeight > resultHeight)
			resultHeight = bottomHeight;

		List<Point> source = new ArrayList<Point>();
		source.add(tl);
		source.add(tr);
		source.add(bl);
		source.add(br);
		Mat startM = Converters.vector_Point2f_to_Mat(source);

		Point outTL = new Point(0, 0);
		Point outTR = new Point(resultWidth, 0);
		Point outBL = new Point(0, resultHeight);
		Point outBR = new Point(resultWidth, resultHeight);
		List<Point> dest = new ArrayList<Point>();
		dest.add(outTL);
		dest.add(outTR);
		dest.add(outBL);
		dest.add(outBR);
		Mat endM = Converters.vector_Point2f_to_Mat(dest);

		Mat subTrans = getPerspectiveTransform(startM, endM);
		Mat subMat = new Mat();
		warpPerspective(origin, subMat, subTrans, new Size(resultWidth, resultHeight));
		subTrans.release();
		return subMat;
	}

	/**
	 * Process noisy or blur image with simplest filters
	 * @param grayMat
	 * @return
	 */
	private Mat processNoisy(Mat grayMat) {
		Mat element1 = getStructuringElement(MORPH_RECT, new Size(2, 2), new Point(1, 1));
		Mat element2 = getStructuringElement(MORPH_RECT, new Size(2, 2), new Point(1, 1));
		dilate(grayMat, grayMat, element1);
		erode(grayMat, grayMat, element2);

		GaussianBlur(grayMat, grayMat, new Size(3, 3), 0);
		// The thresold value will be used here
		threshold(grayMat, grayMat, thresholdMin, thresholdMax, THRESH_BINARY);

		return grayMat;
	}

	/**
	 * Convert mat to string
	 * 
	 * @param source
	 * @return
	 */
	private String matToString(Mat source) {
		int newWidth = source.width()/2;
		resize(source, source, new Size(newWidth, (source.height() * newWidth) / source.width()));
		writeImage("text.jpg", source);
		CharDetectOCR ocrReader = new CharDetectOCR();
		String result = ocrReader.getOCRResult(toBitmap(source));
		//result = result.replace("O", "0"); // Replace O to 0 if have.
		return result;
	}

	/**
	 * Convert mat to bitmap
	 * 
	 * @param mat
	 * @return
	 */
	public static Bitmap toBitmap(Mat mat) {
		Bitmap bitmap = Bitmap.createBitmap(mat.width(), mat.height(), Bitmap.Config.ARGB_8888);
		Utils.matToBitmap(mat, bitmap);
		return bitmap;
	}

}