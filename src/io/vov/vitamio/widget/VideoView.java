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

package io.vov.vitamio.widget;

import com.yixia.zi.utils.DeviceUtils;
import com.yixia.zi.utils.Log;
import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.ViewGroup.LayoutParams;

public class VideoView extends SurfaceView {
	private Activity mActivity;
	private SurfaceHolder mSurfaceHolder;
	private int mSurfaceWidth, mSurfaceHeight;
	private int mVideoMode = VIDEO_LAYOUT_SCALE;
	public static final int VIDEO_LAYOUT_ORIGIN = 0;
	public static final int VIDEO_LAYOUT_SCALE = 1;
	public static final int VIDEO_LAYOUT_STRETCH = 2;
	public static final int VIDEO_LAYOUT_ZOOM = 3;
	public static final int VIDEO_LAYOUT_SCALE_ZOOM = 4;
	public int mVideoHeight;

	public VideoView(Context context, AttributeSet attrs) {
		super(context, attrs);
		getHolder().addCallback(mCallback);
	}

	@SuppressWarnings("deprecation")
	public void initialize(Activity activity, SurfaceCallback l, boolean push) {
		mActivity = activity;
		mListener = l;
		if (mSurfaceHolder == null)
			mSurfaceHolder = getHolder();

		// These methods is set to hw decoder or sw decoder, <= 2.3
		if (push)
			getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		else
			getHolder().setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
	}

	private void setSurfaceLayout(float userRatio, int videoWidth, int videoHeight, float videoAspectRatio) {
		LayoutParams lp = getLayoutParams();
		int windowWidth = DeviceUtils.getScreenWidth(mActivity);
		int windowHeight = DeviceUtils.getScreenHeight(mActivity);
		float windowRatio = windowWidth / (float) windowHeight;
		float videoRatio = userRatio <= 0.01f ? videoAspectRatio : userRatio;
		mSurfaceHeight = videoHeight;
		mSurfaceWidth = videoWidth;
		if (VIDEO_LAYOUT_ORIGIN == mVideoMode && mSurfaceWidth < windowWidth && mSurfaceHeight < windowHeight) {
			lp.width = (int) (mSurfaceHeight * videoRatio);
			lp.height = mSurfaceHeight;
		} else if (mVideoMode == VIDEO_LAYOUT_ZOOM) {
			lp.width = windowRatio > videoRatio ? windowWidth : (int) (videoRatio * windowHeight);
			lp.height = windowRatio < videoRatio ? windowHeight : (int) (windowWidth / videoRatio);
		} else if (mVideoMode == VIDEO_LAYOUT_SCALE_ZOOM && mVideoHeight > 0) {
			lp.width = (int) (mVideoHeight * videoRatio);
			lp.height = mVideoHeight;
		} else {
			boolean full = mVideoMode == VIDEO_LAYOUT_STRETCH;
			lp.width = (full || windowRatio < videoRatio) ? windowWidth : (int) (videoRatio * windowHeight);
			lp.height = (full || windowRatio > videoRatio) ? windowHeight : (int) (windowWidth / videoRatio);
		}
		mVideoHeight = lp.height;
		setLayoutParams(lp);
		getHolder().setFixedSize(mSurfaceWidth, mSurfaceHeight);
		Log.d("VIDEO: %dx%dx%f, Surface: %dx%d, LP: %dx%d, Window: %dx%dx%f", videoWidth, videoHeight, videoAspectRatio, mSurfaceWidth, mSurfaceHeight, lp.width, lp.height, windowWidth, windowHeight, windowRatio);
	}

	public void setVideoLayout(int layout, float userRatio, int videoWidth, int videoHeight, float videoRatio) {
		mVideoMode = layout;
		setSurfaceLayout(userRatio, videoWidth, videoHeight, videoRatio);
	}

	private SurfaceHolder.Callback mCallback = new SurfaceHolder.Callback() {
		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
			holder.setKeepScreenOn(true);
			if (mListener != null)
				mListener.onSurfaceChanged(holder, format, width, height);
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			mSurfaceHolder = holder;
			if (mListener != null)
				mListener.onSurfaceCreated(holder);
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			if (mListener != null)
				mListener.onSurfaceDestroyed(holder);
		}
	};

	private SurfaceCallback mListener;

	public interface SurfaceCallback {
		public void onSurfaceCreated(SurfaceHolder holder);

		public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height);

		public void onSurfaceDestroyed(SurfaceHolder holder);
	}
}
