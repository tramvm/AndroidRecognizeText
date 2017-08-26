package com.compa.readerocr.view;

import com.compa.readerocr.AndroidCamera;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.SurfaceView;

public class CameraSurfaceView extends SurfaceView {

	public CameraSurfaceView(Context context) {
		super(context);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public CameraSurfaceView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.getSize(heightMeasureSpec));
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {

		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			float x = event.getX();
			float y = event.getY();
			float touchMajor = event.getTouchMajor();
			float touchMinor = event.getTouchMinor();

			Rect touchRect = new Rect((int) (x - touchMajor / 2), (int) (y - touchMinor / 2),
					(int) (x + touchMajor / 2), (int) (y + touchMinor / 2));

			((AndroidCamera) getContext()).camPreview.touchFocus(touchRect);
		}

		return true;
	}

}