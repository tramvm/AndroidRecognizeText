package com.compa.readerocr;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView.ScaleType;

import com.compa.readerocr.utils.CharDetectOCR;
import com.compa.readerocr.utils.CommonUtils;
import com.compa.readerocr.view.TouchImageView;

import static com.compa.readerocr.utils.CommonUtils.info;

/**
 * 
 * @author Sea
 *
 */
public class RecognizeTextActivity extends Activity {
	static int REQUEST_IMAGE_CAPTURE = 1;
	static ProcessImage processImg = new ProcessImage();

	Button btnStartCamera;
	Button btnExit;

	private String language;
	private TouchImageView image;
	private EditText recognizeResult;

	private int sourceW = 0;
	private int sourceH = 0;
	private String lastFileName = "";
	private boolean isRecognized = false;

	ProgressDialog progressBar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_reader);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		Bundle b = getIntent().getExtras();
		language = b.getString("language");
		ProcessImage.language = language;
		ProcessImage.thresholdMin =  Integer.parseInt(b.getString("threshold"));
		info("Language: "+ language + "   threshold: "+ ProcessImage.thresholdMin);

		btnStartCamera = (Button) findViewById(R.id.btnStartCamera);
		btnExit = (Button) findViewById(R.id.btnExit);

		btnStartCamera.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
					takePicture();
				}
				return false;
			}
		});

		btnExit.setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				if (arg1.getAction() == MotionEvent.ACTION_UP) {
					existApp();
				}
				return false;
			}
		});

		recognizeResult = (EditText) findViewById(R.id.recognize_result);
		image = (TouchImageView) findViewById(R.id.grid_img);
		image.setScaleType(ScaleType.CENTER_INSIDE);

		if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
			new InitTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} else {
			new InitTask().execute();
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.recognize, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void takePicture() {
		Intent takePicIntent = new Intent(RecognizeTextActivity.this, AndroidCamera.class);
		lastFileName = CommonUtils.APP_PATH + "capture" + System.currentTimeMillis() + ".jpg";
		takePicIntent.putExtra("output", lastFileName);
		info(lastFileName);
		startActivityForResult(takePicIntent, REQUEST_IMAGE_CAPTURE);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
			BitmapFactory.Options options = new BitmapFactory.Options();
			Bitmap imageBitmap = BitmapFactory.decodeFile(lastFileName, options);

			if (imageBitmap == null) {
				// Try again
				isRecognized = false;
				image.setImageBitmap(imageBitmap);
				hideProcessBar();
				dialogBox("Can not recognize sheet. Please try again", "Retry", "Exist", true);
				return;
			}
			final Bitmap finalImageBitmap = imageBitmap.getWidth() > imageBitmap.getHeight()
					? rotateBitmap(imageBitmap, 90) : imageBitmap;

			int top = data.getIntExtra("top", 0);
			int bot = data.getIntExtra("bot", 0);
			int right = data.getIntExtra("right", 0);
			int left = data.getIntExtra("left", 0);

			image.setImageBitmap(finalImageBitmap);
			displayResult(finalImageBitmap, top, bot, right, left);

		}
	}

	public void displayResult(Bitmap imageBitmap, int top, int bot, int right, int left) {
		info("Origin size: " + imageBitmap.getWidth() + ":" + imageBitmap.getHeight());
		// Parser
		recognizeResult.setText("");
		if (processImg.parseBitmap(imageBitmap, top, bot, right, left)) {
			// TODO: set result
			recognizeResult.setText(processImg.recognizeResult);
			// TODO: write result to image
			// image.setImageBitmap(toBitmap(processImg.drawAnswered(numberAnswer)));
			isRecognized = true;
			hideProcessBar();
		} else {
			// Try again
			isRecognized = false;
			image.setImageBitmap(imageBitmap);
			hideProcessBar();
			dialogBox("Can not recognize sheet. Please try again", "Retry", "Exist", true);
		}
	}

	public Bitmap rotateBitmap(Bitmap source, float angle) {
		Matrix matrix = new Matrix();
		matrix.postRotate(angle);
		return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
	}

	public void dialogBox(String message, String bt1, String bt2, final boolean flagContinue) {
		AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
		alertDialogBuilder.setMessage(message);
		alertDialogBuilder.setPositiveButton(bt1, new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				if (flagContinue) {
					takePicture();
				}
			}
		});

		if (bt2 != "") {
			alertDialogBuilder.setNegativeButton(bt2, new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface arg0, int arg1) {
					existApp();
					// return false;
				}
			});
		}

		AlertDialog alertDialog = alertDialogBuilder.create();
		alertDialog.show();
	}

	public void existApp() {
		CommonUtils.cleanFolder();
		this.finish();
	}

	public void showProgressBar(String title, String message) {
		progressBar = ProgressDialog.show(this, title, message, false, false);
	}

	public void hideProcessBar() {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				if (progressBar != null && progressBar.isShowing()) {
					progressBar.dismiss();
				}
			}
		});
	}

	private class InitTask extends AsyncTask<String, Void, String> {
		@Override
		protected String doInBackground(String... data) {
			try {
				CharDetectOCR.init(getAssets());
				return "";
			} catch (Exception e) {
				Log.e("COMPA", "Error init data OCR. Message: " + e.getMessage());
			}
			return "";
		}

		@Override
		protected void onPostExecute(String result) {

		}
	}

}
