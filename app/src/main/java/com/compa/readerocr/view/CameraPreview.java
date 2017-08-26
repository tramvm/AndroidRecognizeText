package com.compa.readerocr.view;

import static com.compa.readerocr.utils.CommonUtils.info;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

import com.compa.readerocr.AndroidCamera;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.Face;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.ViewGroup.LayoutParams;

public class CameraPreview implements SurfaceHolder.Callback {

	Camera camera;
	CameraSurfaceView cameraSurfaceView;
	SurfaceHolder surfaceHolder;
	AndroidCamera mainActivity;
	boolean previewing = false;

	DrawingView drawingView;
	boolean enableTakePic = true;

	Face[] detectedFaces;
	final int previewWidth;
	final int previewHeight;

	int cameraWidth;
	int cameraHeight;
	Bitmap bitmap;
	int[] pixels;
	int imageFormat = ImageFormat.JPEG;
	// Size base OpenCv, not base camera view
	int boxW = 100;
	int boxH = 100;
	int startX = 0;
	int startY = 0;
	float marginLeftRight = 10;
	float marginTop = 40;
	String outputPath;

	// To Crop
	int TOP = 0;
	int LEFT = 0;
	int BOT = 0;
	int RIGHT = 0;

	float ratioX;
	float ratioImg;

	final int RESULT_SAVEIMAGE = 0;

	private ScheduledExecutorService myScheduledExecutorService;

	/** Called when the activity is first created. */

	public CameraPreview(AndroidCamera androidCamera, CameraSurfaceView cameraSurfaceView) {
		info("Create CameraPreview2");
		Display display = androidCamera.getWindowManager().getDefaultDisplay();
		Point size = new Point();
		display.getSize(size);
		previewWidth = size.x;
		previewHeight = size.y;
		info("Actual preview size: " + previewWidth + ":" + previewHeight);

		this.mainActivity = androidCamera;
		this.cameraSurfaceView = cameraSurfaceView;
		surfaceHolder = cameraSurfaceView.getHolder();
		surfaceHolder.addCallback(this);

		// surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		drawingView = new DrawingView(androidCamera);
		LayoutParams layoutParamsDrawing = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		mainActivity.addContentView(drawingView, layoutParamsDrawing);

		// controlInflater = LayoutInflater.from(getBaseContext());
		// View viewControl = controlInflater.inflate(R.layout.control, null);
		// LayoutParams layoutParamsControl = new
		// LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT);
		// this.addContentView(viewControl, layoutParamsControl);
	}

	public void takePicture(String outputPath) {
		this.outputPath = outputPath;
		camera.takePicture(shutterCallback, picCallback_RAW, picCallback_JPG);
	}

	public void touchFocus(final Rect tfocusRect) {

		enableTakePic = false;

		// Convert from View's width and height to +/- 1000
		final Rect targetFocusRect = new Rect(tfocusRect.left * 2000 / drawingView.getWidth() - 1000,
				tfocusRect.top * 2000 / drawingView.getHeight() - 1000,
				tfocusRect.right * 2000 / drawingView.getWidth() - 1000,
				tfocusRect.bottom * 2000 / drawingView.getHeight() - 1000);

		// info("Touch focus: T=" + targetFocusRect.top + " R=" +
		// targetFocusRect.right + " B=" + targetFocusRect.bottom
		// + " L=" + targetFocusRect.left + " C=" + targetFocusRect.centerX() +
		// ":" + targetFocusRect.centerY());

		final List<Camera.Area> focusList = new ArrayList<Camera.Area>();
		Camera.Area focusArea = new Camera.Area(targetFocusRect, 1000);
		focusList.add(focusArea);

		try {
			Parameters para = camera.getParameters();
			para.setFocusAreas(focusList);
			para.setMeteringAreas(focusList);
			camera.setParameters(para);

			camera.autoFocus(autoFocusCallback);
			// drawingView.setHaveTouch(true, tfocusRect);
			// drawingView.invalidate();
		} catch (Exception e) {
			// Log.e(TAG, "Error touch zoom.", e);
			info("Error touch zoom. Not important!");
		}

	}

	AutoFocusCallback autoFocusCallback = new AutoFocusCallback() {

		@Override
		public void onAutoFocus(boolean arg0, Camera arg1) {
			if (arg0) {
				enableTakePic = true;
				camera.cancelAutoFocus();
			}

			float focusDistances[] = new float[3];
			arg1.getParameters().getFocusDistances(focusDistances);
			info("Optimal Focus Distance(meters): " + focusDistances[Camera.Parameters.FOCUS_DISTANCE_OPTIMAL_INDEX]);

		}
	};

	ShutterCallback shutterCallback = new ShutterCallback() {

		@Override
		public void onShutter() {

		}
	};

	PictureCallback picCallback_RAW = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {

		}
	};

	PictureCallback picCallback_JPG = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] arg0, Camera arg1) {
			info(">>>> onPictureTaken");
			camera.stopPreview();
			// Bitmap bitmapPicture = BitmapFactory.decodeByteArray(arg0, 0,
			// arg0.length);
			Uri uriTarget = Uri.fromFile(new File(outputPath));

			OutputStream imageFileOS;
			try {
				imageFileOS = mainActivity.getContentResolver().openOutputStream(uriTarget);
				imageFileOS.write(arg0);
				imageFileOS.flush();
				imageFileOS.close();

				info("Image saved: " + outputPath);

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

			mainActivity.callProcessImage(outputPath, TOP, BOT, RIGHT, LEFT);
			// camera.startPreview();
		}
	};

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (previewing) {
			info(">>>>>>>>>>>>>>>>>. surfaceChanged in previewing");
			camera.stopPreview();
			previewing = false;
		}

		if (camera != null) {
			Camera.Parameters parameters = camera.getParameters();
			try {
				Camera.Size size = getBestPreviewSize(previewWidth, previewHeight, parameters);

				if (size != null) {
					info("Preview size: " + size.width + ":" + size.height);

					cameraWidth = size.width;
					cameraHeight = size.height;
					ratioX = ((float) cameraWidth) / cameraHeight;
					Camera.Size picSize = getBestPictureSize(parameters, ratioX);
					ratioImg = ((float) picSize.width) / previewWidth;
					info("Pic preview size: " + picSize.width + ":" + picSize.height);
					parameters.setPreviewSize(cameraWidth, cameraHeight);
					parameters.setPictureSize(picSize.width, picSize.height);
					parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

					parameters.setPictureFormat(ImageFormat.JPEG);
					camera.setParameters(parameters);
				}

				camera.setPreviewDisplay(surfaceHolder);
				camera.startPreview();
				previewing = true;

			} catch (IOException e) {
				Log.e("COMPA", "Have some error!", e);
			}

			try {
				drawingView.setListRect(createAreaSign(previewWidth, previewHeight));
				drawingView.invalidate();
			} catch (Exception e) {

			}

		}

		try {
			//initSurfaceView();
		} catch (Exception e) {
		}
	}

	private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.1;
		double targetRatio = (double) h / w;

		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		camera = Camera.open();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		info(">>>>>>>>>>>>>> surfaceDestroyed");
		camera.stopPreview();
		camera.release();
		camera = null;
		previewing = false;
	}

	private Camera.Size getBestPreviewSize(int width, int height, Camera.Parameters parameters) {

		Camera.Size result = null;
		if (true) {
			return parameters.getSupportedPreviewSizes().get(0);
		}
		for (Camera.Size size : parameters.getSupportedPreviewSizes()) {
			info("Size " + size.width + ":" + size.height);
			if (size.width <= width && size.height <= height) {
				if (result == null) {
					result = size;
				} else {
					int resultArea = result.width * result.height;
					int newArea = size.width * size.height;

					if (newArea > resultArea) {
						result = size;
					}
				}
			}
		}

		return (result);
	}

	private Camera.Size getBestPictureSize(Camera.Parameters parameters, float ratio) {
		Camera.Size result = null;

		for (Camera.Size size : parameters.getSupportedPictureSizes()) {
			info("Pic size: " + size.width + ":" + size.height);
			if (result == null) {
				result = size;
			} else if (((float) size.width) / size.height == ratio) {
				result = size;
				break;
			}
		}

		return (result);
	}

	private void initSurfaceView() {
		int surfaceH = previewHeight;
		int surfaceW = (int) (surfaceH * ratioX);// 4 / 3;
		LayoutParams prSurface = cameraSurfaceView.getLayoutParams();
		prSurface.width = surfaceW;
		prSurface.height = surfaceH;
		info("Surface size: " + prSurface.width + ":" + prSurface.height);
		cameraSurfaceView.setLayoutParams(prSurface);
		cameraSurfaceView.invalidate();

		startX = (int) cameraSurfaceView.getX();
		startY = (int) cameraSurfaceView.getY();
		info("Surface X: " + startX + "  Y:" + startY);

		int btnW = previewWidth - startX - prSurface.width;
		mainActivity.resizeBtnTakePic(btnW, btnW);
	}

	private List<Rect> createAreaSign(int width, int height) {
		List<Rect> lstRect = new ArrayList<Rect>();

		float avrg = height / 34.0F;

		boxH = boxW = 10;//(int) (5 * avrg);
		int halfBox = boxW / 2;
		marginLeftRight = avrg / 2;
		info("Avrg: " + avrg + "  boxH=boxW: " + boxH + "  marginLR: " + marginLeftRight);

		int beginTop = startX;
		int topLeftY = (int) (beginTop + marginTop + halfBox);

		int topLeftX = (int) (height - marginLeftRight - halfBox);

		int topRightY = (int) (beginTop + marginTop + halfBox);
		int topRightX = (int) (marginLeftRight + halfBox);

		int midLeftX = (int) (height - marginLeftRight - halfBox);
		int midLeftY = (int) (topLeftY + (13 * avrg));

		int botLeftX = (int) (height - marginLeftRight - halfBox);
		int botLeftY = (int) (beginTop + marginTop) + (int) (39 * avrg);

		int midRightX = topRightX;
		int midRightY = midLeftY;

		int botRightX = topRightX;
		int botRightY = botLeftY;

		// To crop
//		TOP = (int) ((topLeftY - halfBox) * ratioImg);
//		RIGHT = (int) ((topRightX - halfBox) * ratioImg);
//		BOT = (int) ((botLeftY + halfBox) * ratioImg);
//		LEFT = (int) ((topLeftX + halfBox) * ratioImg);

		// TL
		int pLeft = width/4;
		int pTop = (int)marginLeftRight;
		int pRight = pLeft*2;
		int pBot = height-(int)marginLeftRight;
		lstRect.add(new Rect(pLeft, pTop, pRight, pBot));
		
		TOP = (int) (pLeft * ratioImg);
		RIGHT = (int) (pTop * ratioImg);
		BOT = (int) (pRight * ratioImg) - TOP;
		LEFT = (int) (pBot * ratioImg) - RIGHT;

		// BL
		//lstRect.add(new Rect(botLeftY - halfBox, botLeftX - halfBox, botLeftY + halfBox, botLeftX + halfBox));

		// MR
		//lstRect.add(new Rect(midRightY - halfBox, midRightX - halfBox, midRightY + halfBox, midRightX + halfBox));

		// BR
		//lstRect.add(new Rect(botRightY - halfBox, botRightX - halfBox, botRightY + halfBox, botRightX + halfBox));

		return lstRect;
	}

	private class DrawingView extends View {

		Paint drawingPaint;
		List<Rect> lstRectArea;

		public DrawingView(Context context) {
			super(context);
			drawingPaint = new Paint();
			drawingPaint.setColor(Color.GREEN);
			drawingPaint.setStyle(Paint.Style.STROKE);
			drawingPaint.setStrokeWidth(5);
		}

		public void setListRect(List<Rect> lstRect) {
			this.lstRectArea = lstRect;
		}

		@Override
		protected void onDraw(Canvas canvas) {
			if (lstRectArea != null && lstRectArea.size() > 0) {
				for (Rect rect : lstRectArea) {
					canvas.drawRect(rect.left * 1.0F, rect.top * 1.0F, rect.right * 1.0F, rect.bottom * 1.0F,
							drawingPaint);
				}
			}
		}

	}
}