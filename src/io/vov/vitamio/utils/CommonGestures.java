/*
 * Copyright (C) 2012 YIXIA.COM
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.vov.vitamio.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector.SimpleOnGestureListener;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import com.yixia.zi.utils.DeviceUtils;

public class CommonGestures {
	public static final int SCALE_STATE_BEGIN = 0;
	public static final int SCALE_STATE_SCALEING = 1;
	public static final int SCALE_STATE_END = 2;

	private boolean mGestureEnabled;

	private GestureDetectorCompat mDoubleTapGestureDetector;
	private GestureDetectorCompat mTapGestureDetector;
	private ScaleGestureDetector mScaleDetector;

	private Activity mContext;

	public CommonGestures(Activity ctx) {
		mContext = ctx;
		mDoubleTapGestureDetector = new GestureDetectorCompat(mContext, new DoubleTapGestureListener());
		mTapGestureDetector = new GestureDetectorCompat(mContext, new TapGestureListener());
		mScaleDetector = new ScaleGestureDetector(mContext, new ScaleDetectorListener());
	}

	public boolean onTouchEvent(MotionEvent event) {
		if (mListener == null)
			return false;

		if (mTapGestureDetector.onTouchEvent(event))
			return true;

		if (event.getPointerCount() > 1) {
			try {
				if (mScaleDetector != null && mScaleDetector.onTouchEvent(event))
					return true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		if (mDoubleTapGestureDetector.onTouchEvent(event))
			return true;

		switch (event.getAction() & MotionEvent.ACTION_MASK) {
		case MotionEvent.ACTION_UP:
			mListener.onGestureEnd();
			break;
		}

		return false;
	}

	private class TapGestureListener extends SimpleOnGestureListener {
		@Override
		public boolean onSingleTapConfirmed(MotionEvent event) {
			if (mListener != null)
				mListener.onSingleTap();
			return true;
		}

		@Override
		public void onLongPress(MotionEvent e) {
			if (mListener != null && mGestureEnabled)
				mListener.onLongPress();
		}
	}

	@SuppressLint("NewApi")
	private class ScaleDetectorListener implements ScaleGestureDetector.OnScaleGestureListener {
		@Override
		public boolean onScale(ScaleGestureDetector detector) {
			if (mListener != null && mGestureEnabled)
				mListener.onScale(detector.getScaleFactor(), SCALE_STATE_SCALEING);
			return true;
		}

		@Override
		public void onScaleEnd(ScaleGestureDetector detector) {
			if (mListener != null && mGestureEnabled)
				mListener.onScale(0F, SCALE_STATE_END);
		}

		@Override
		public boolean onScaleBegin(ScaleGestureDetector detector) {
			if (mListener != null && mGestureEnabled)
				mListener.onScale(0F, SCALE_STATE_BEGIN);
			return true;
		}
	}

	private class DoubleTapGestureListener extends SimpleOnGestureListener {
		private boolean mDown = false;

		@Override
		public boolean onDown(MotionEvent event) {
			mDown = true;
			return super.onDown(event);
		}

		@Override
		public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
			if (mListener != null && mGestureEnabled && e1 != null && e2 != null) {
				if (mDown) {
					mListener.onGestureBegin();
					mDown = false;
				}
				float mOldX = e1.getX(), mOldY = e1.getY();
				int windowWidth = DeviceUtils.getScreenWidth(mContext);
				int windowHeight = DeviceUtils.getScreenHeight(mContext);
				if (Math.abs(e2.getY(0) - mOldY) * 2 > Math.abs(e2.getX(0) - mOldX)) {
					if (mOldX > windowWidth * 4.0 / 5) {
						mListener.onRightSlide((mOldY - e2.getY(0)) / windowHeight);
					} else if (mOldX < windowWidth / 5.0) {
						mListener.onLeftSlide((mOldY - e2.getY(0)) / windowHeight);
					}
				}
			}
			return super.onScroll(e1, e2, distanceX, distanceY);
		}

		@Override
		public boolean onDoubleTap(MotionEvent event) {
			if (mListener != null && mGestureEnabled)
				mListener.onDoubleTap();
			return super.onDoubleTap(event);
		}
	}

	public void setTouchListener(TouchListener l, boolean enable) {
		mListener = l;
		mGestureEnabled = enable;
	}

	private TouchListener mListener;

	public interface TouchListener {
		public void onGestureBegin();

		public void onGestureEnd();

		public void onLeftSlide(float percent);

		public void onRightSlide(float percent);

		public void onSingleTap();

		public void onDoubleTap();

		public void onScale(float scaleFactor, int state);

		public void onLongPress();
	}
}
