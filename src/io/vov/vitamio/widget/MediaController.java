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

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.AudioManager;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;

import io.vov.vitamio.R;
import io.vov.vitamio.utils.CommonGestures;
import io.vov.vitamio.utils.CommonGestures.TouchListener;

import com.yixia.zi.utils.FractionalTouchDelegate;
import com.yixia.zi.utils.Log;
import com.yixia.zi.utils.StringHelper;
import com.yixia.zi.utils.StringUtils;
import com.yixia.zi.utils.UIUtils;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class MediaController extends FrameLayout {
	private MediaPlayerControl mPlayer;
	private Activity mContext;
	private PopupWindow mWindow;
	private View mAnchor;
	private View mRoot;
	private ImageButton mLock;
	private ImageButton mScreenToggle;
	private ImageButton mSnapshot;
	private SeekBar mProgress;
	private TextView mEndTime, mCurrentTime;
	private long mDuration;
	private boolean mShowing;
	private boolean mScreenLocked = false;
	private boolean mDragging;
	private boolean mInstantSeeking = true;
	private static final int DEFAULT_TIME_OUT = 3000;
	private static final int DEFAULT_LONG_TIME_SHOW = 120000;
	private static final int DEFAULT_SEEKBAR_VALUE = 1000;
	private static final int TIME_TICK_INTERVAL = 1000;
	private ImageButton mPauseButton;

	private View mMediaController;
	private View mControlsLayout;
	private View mSystemInfoLayout;
	private View mControlsButtons;
	private View mMenu;
	private TextView mDateTime;
	private TextView mDownloadRate;
	private TextView mFileName;
	private TextView mBatteryLevel;

	private TextView mOperationInfo;
	private View mOperationVolLum;
	private ImageView mVolLumNum;
	private ImageView mVolLumBg;

	private AudioManager mAM;
	private int mMaxVolume;
	private float mBrightness = 0.01f;
	private int mVolume = 0;
	private Handler mHandler;

	private Animation mAnimSlideInTop;
	private Animation mAnimSlideInBottom;
	private Animation mAnimSlideOutTop;
	private Animation mAnimSlideOutBottom;

	private CommonGestures mGestures;
	private int mVideoMode;

	public MediaController(Context context) {
		super(context);
		mContext = (Activity) context;
		initFloatingWindow();
		initResources();
	}

	public MediaController(Context context, boolean locked) {
		this(context);
		mScreenLocked = locked;
		lock(mScreenLocked);
	}

	private void initFloatingWindow() {
		mWindow = new PopupWindow(mContext);
		mWindow.setFocusable(true);
		mWindow.setBackgroundDrawable(null);
		mWindow.setOutsideTouchable(true);
	}

	@TargetApi(11)
	public void setWindowLayoutType() {
		if (UIUtils.hasICS()) {
			try {
				mAnchor.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
				Method setWindowLayoutType = PopupWindow.class.getMethod("setWindowLayoutType", new Class[] { int.class });
				setWindowLayoutType.invoke(mWindow, WindowManager.LayoutParams.TYPE_APPLICATION_ATTACHED_DIALOG);
			} catch (Exception e) {
				Log.e("setWindowLayoutType", e);
			}
		}
	}

	@TargetApi(11)
	private void initResources() {
		mHandler = new MHandler(this);
		mAM = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);
		mMaxVolume = mAM.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		mGestures = new CommonGestures(mContext);
		mGestures.setTouchListener(mTouchListener, true);

		mAnimSlideOutBottom = AnimationUtils.loadAnimation(mContext, R.anim.slide_out_bottom);
		mAnimSlideOutTop = AnimationUtils.loadAnimation(mContext, R.anim.slide_out_top);
		mAnimSlideInBottom = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_bottom);
		mAnimSlideInTop = AnimationUtils.loadAnimation(mContext, R.anim.slide_in_top);
		mAnimSlideOutBottom.setAnimationListener(new AnimationListener() {
			@Override
			public void onAnimationStart(Animation animation) {
			}

			@Override
			public void onAnimationEnd(Animation animation) {
				mMediaController.setVisibility(View.GONE);
				showButtons(false);
				mHandler.removeMessages(MSG_HIDE_SYSTEM_UI);
				mHandler.sendEmptyMessageDelayed(MSG_HIDE_SYSTEM_UI, DEFAULT_TIME_OUT);
			}

			@Override
			public void onAnimationRepeat(Animation animation) {
			}
		});

		removeAllViews();

		mRoot = inflateLayout();
		mWindow.setContentView(mRoot);
		mWindow.setWidth(android.view.ViewGroup.LayoutParams.MATCH_PARENT);
		mWindow.setHeight(android.view.ViewGroup.LayoutParams.MATCH_PARENT);

		findViewItems(mRoot);
		showSystemUi(false);
		if (UIUtils.hasHoneycomb()) {
			mRoot.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
				public void onSystemUiVisibilityChange(int visibility) {
					if ((visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0) {
						mHandler.sendEmptyMessageDelayed(MSG_HIDE_SYSTEM_UI, DEFAULT_TIME_OUT);
					}
				}
			});
		}
	}

	private View inflateLayout() {
		return ((LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.mediacontroller, this);
	}

	private void findViewItems(View v) {
		mMediaController = v.findViewById(R.id.mediacontroller);

		mSystemInfoLayout = v.findViewById(R.id.info_panel);

		mEndTime = (TextView) v.findViewById(R.id.mediacontroller_time_total);
		mCurrentTime = (TextView) v.findViewById(R.id.mediacontroller_time_current);

		mMenu = v.findViewById(R.id.video_menu);
		mMenu.setOnClickListener(mMenuListener);
		FractionalTouchDelegate.setupDelegate(mSystemInfoLayout, mMenu, new RectF(1.0f, 1f, 1.2f, 1.2f));

		mFileName = (TextView) v.findViewById(R.id.mediacontroller_file_name);
		mDateTime = (TextView) v.findViewById(R.id.date_time);
		mDownloadRate = (TextView) v.findViewById(R.id.download_rate);
		mBatteryLevel = (TextView) v.findViewById(R.id.battery_level);

		mControlsLayout = v.findViewById(R.id.mediacontroller_controls);
		mControlsButtons = v.findViewById(R.id.mediacontroller_controls_buttons);

		mOperationInfo = (TextView) v.findViewById(R.id.operation_info);
		mOperationVolLum = v.findViewById(R.id.operation_volume_brightness);
		mVolLumBg = (ImageView) v.findViewById(R.id.operation_bg);
		mVolLumNum = (ImageView) v.findViewById(R.id.operation_percent);

		mLock = (ImageButton) v.findViewById(R.id.mediacontroller_lock);
		mLock.setOnClickListener(mLockClickListener);
		FractionalTouchDelegate.setupDelegate(mSystemInfoLayout, mLock, new RectF(1.0f, 1f, 1.2f, 1.2f));

		mScreenToggle = (ImageButton) v.findViewById(R.id.mediacontroller_screen_size);
		mScreenToggle.setOnClickListener(mScreenToggleListener);

		mSnapshot = (ImageButton) v.findViewById(R.id.mediacontroller_snapshot);
		mSnapshot.setOnClickListener(mSnapshotListener);

		mPauseButton = (ImageButton) v.findViewById(R.id.mediacontroller_play_pause);
		mPauseButton.setOnClickListener(mPauseListener);

		mProgress = (SeekBar) v.findViewById(R.id.mediacontroller_seekbar);
		mProgress.setOnSeekBarChangeListener(mSeekListener);
		mProgress.setMax(DEFAULT_SEEKBAR_VALUE);
	}

	public void setAnchorView(View view) {
		mAnchor = view;
		int[] location = new int[2];
		mAnchor.getLocationOnScreen(location);
		Rect anchorRect = new Rect(location[0], location[1], location[0] + mAnchor.getWidth(), location[1] + mAnchor.getHeight());
		setWindowLayoutType();
		mWindow.showAtLocation(mAnchor, Gravity.NO_GRAVITY, anchorRect.left, anchorRect.bottom);
	}

	public void release() {
		if (mWindow != null) {
			mWindow.dismiss();
			mWindow = null;
		}
	}

	private View.OnClickListener mMenuListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			mPlayer.showMenu();
		}
	};

	private void setOperationInfo(String info, long time) {
		mOperationInfo.setText(info);
		mOperationInfo.setVisibility(View.VISIBLE);
		mHandler.removeMessages(MSG_HIDE_OPERATION_INFO);
		mHandler.sendEmptyMessageDelayed(MSG_HIDE_OPERATION_INFO, time);
	}

	private void setBrightnessScale(float scale) {
		setGraphicOperationProgress(R.drawable.video_brightness_bg, scale);
	}

	private void setVolumeScale(float scale) {
		setGraphicOperationProgress(R.drawable.video_volumn_bg, scale);
	}

	private void setGraphicOperationProgress(int bgID, float scale) {
		mVolLumBg.setImageResource(bgID);
		mOperationInfo.setVisibility(View.GONE);
		mOperationVolLum.setVisibility(View.VISIBLE);
		ViewGroup.LayoutParams lp = mVolLumNum.getLayoutParams();
		lp.width = (int) (findViewById(R.id.operation_full).getLayoutParams().width * scale);
		mVolLumNum.setLayoutParams(lp);
	}

	public void setFileName(String name) {
		mFileName.setText(name);
	}

	public void setDownloadRate(String rate) {
		mDownloadRate.setVisibility(View.VISIBLE);
		mDownloadRate.setText(rate);
	}

	public void setBatteryLevel(String level) {
		mBatteryLevel.setVisibility(View.VISIBLE);
		mBatteryLevel.setText(level);
	}

	public void setMediaPlayer(MediaPlayerControl player) {
		mPlayer = player;
		updatePausePlay();
	}

	public void show() {
		show(DEFAULT_TIME_OUT);
	}

	public void show(int timeout) {
		if (timeout != 0) {
			mHandler.removeMessages(MSG_FADE_OUT);
			mHandler.sendMessageDelayed(mHandler.obtainMessage(MSG_FADE_OUT), timeout);
		}
		if (!mShowing) {
			showButtons(true);
			mHandler.removeMessages(MSG_HIDE_SYSTEM_UI);
			showSystemUi(true);

			mPauseButton.requestFocus();

			mControlsLayout.startAnimation(mAnimSlideInTop);
			mSystemInfoLayout.startAnimation(mAnimSlideInBottom);
			mMediaController.setVisibility(View.VISIBLE);

			updatePausePlay();
			mHandler.sendEmptyMessage(MSG_TIME_TICK);
			mHandler.sendEmptyMessage(MSG_SHOW_PROGRESS);

			mShowing = true;
		}
	}

	public void hide() {
		if (mShowing) {
			try {
				mHandler.removeMessages(MSG_TIME_TICK);
				mHandler.removeMessages(MSG_SHOW_PROGRESS);
				mControlsLayout.startAnimation(mAnimSlideOutTop);
				mSystemInfoLayout.startAnimation(mAnimSlideOutBottom);
			} catch (IllegalArgumentException ex) {
				Log.d("MediaController already removed");
			}
			mShowing = false;
		}
	}

	private void toggleVideoMode(boolean larger, boolean recycle) {
		if (larger) {
			if (mVideoMode < VideoView.VIDEO_LAYOUT_ZOOM)
				mVideoMode++;
			else if (recycle)
				mVideoMode = VideoView.VIDEO_LAYOUT_ORIGIN;
		} else {
			if (mVideoMode > VideoView.VIDEO_LAYOUT_ORIGIN)
				mVideoMode--;
			else if (recycle)
				mVideoMode = VideoView.VIDEO_LAYOUT_ZOOM;
		}

		switch (mVideoMode) {
		case VideoView.VIDEO_LAYOUT_ORIGIN:
			setOperationInfo(mContext.getString(R.string.video_original), 500);
			mScreenToggle.setImageResource(R.drawable.mediacontroller_sreen_size_100);
			break;
		case VideoView.VIDEO_LAYOUT_SCALE:
			setOperationInfo(mContext.getString(R.string.video_fit_screen), 500);
			mScreenToggle.setImageResource(R.drawable.mediacontroller_screen_fit);
			break;
		case VideoView.VIDEO_LAYOUT_STRETCH:
			setOperationInfo(mContext.getString(R.string.video_stretch), 500);
			mScreenToggle.setImageResource(R.drawable.mediacontroller_screen_size);
			break;
		case VideoView.VIDEO_LAYOUT_ZOOM:
			setOperationInfo(mContext.getString(R.string.video_crop), 500);
			mScreenToggle.setImageResource(R.drawable.mediacontroller_sreen_size_crop);
			break;
		}

		mPlayer.toggleVideoMode(mVideoMode);
	}

	private void lock(boolean toLock) {
		if (toLock) {
			mLock.setImageResource(R.drawable.mediacontroller_lock);
			mMenu.setVisibility(View.GONE);
			mControlsButtons.setVisibility(View.GONE);
			mProgress.setEnabled(false);
			if (mScreenLocked != toLock)
				setOperationInfo(mContext.getString(R.string.video_screen_locked), 1000);
		} else {
			mLock.setImageResource(R.drawable.mediacontroller_unlock);
			mMenu.setVisibility(View.VISIBLE);
			mControlsButtons.setVisibility(View.VISIBLE);
			mProgress.setEnabled(true);
			if (mScreenLocked != toLock)
				setOperationInfo(mContext.getString(R.string.video_screen_unlocked), 1000);
		}
		mScreenLocked = toLock;
		mGestures.setTouchListener(mTouchListener, !mScreenLocked);
	}

	public boolean isLocked() {
		return mScreenLocked;
	}

	private static final int MSG_FADE_OUT = 1;
	private static final int MSG_SHOW_PROGRESS = 2;
	private static final int MSG_HIDE_SYSTEM_UI = 3;
	private static final int MSG_TIME_TICK = 4;
	private static final int MSG_HIDE_OPERATION_INFO = 5;
	private static final int MSG_HIDE_OPERATION_VOLLUM = 6;

	private static class MHandler extends Handler {
		private WeakReference<MediaController> mc;

		public MHandler(MediaController mc) {
			this.mc = new WeakReference<MediaController>(mc);
		}

		@Override
		public void handleMessage(Message msg) {
			MediaController c = mc.get();
			if (c == null)
				return;

			switch (msg.what) {
			case MSG_FADE_OUT:
				c.hide();
				break;
			case MSG_SHOW_PROGRESS:
				long pos = c.setProgress();
				if (!c.mDragging && c.mShowing) {
					msg = obtainMessage(MSG_SHOW_PROGRESS);
					sendMessageDelayed(msg, 1000 - (pos % 1000));
					c.updatePausePlay();
				}
				break;
			case MSG_HIDE_SYSTEM_UI:
				if (!c.mShowing)
					c.showSystemUi(false);
				break;
			case MSG_TIME_TICK:
				c.mDateTime.setText(StringHelper.currentTimeString());
				sendEmptyMessageDelayed(MSG_TIME_TICK, TIME_TICK_INTERVAL);
				break;
			case MSG_HIDE_OPERATION_INFO:
				c.mOperationInfo.setVisibility(View.GONE);
				break;
			case MSG_HIDE_OPERATION_VOLLUM:
				c.mOperationVolLum.setVisibility(View.GONE);
				break;
			}
		}
	};

	private long setProgress() {
		if (mPlayer == null || mDragging)
			return 0;

		long position = mPlayer.getCurrentPosition();
		long duration = mPlayer.getDuration();
		if (duration > 0) {
			long pos = 1000L * position / duration;
			mProgress.setProgress((int) pos);
		}
		int percent = mPlayer.getBufferPercentage();
		mProgress.setSecondaryProgress(percent * 10);

		mDuration = duration;

		mEndTime.setText(StringUtils.generateTime(mDuration));
		mCurrentTime.setText(StringUtils.generateTime(position));

		return position;
	}

	@Override
	public boolean onTouchEvent(MotionEvent event) {
		mHandler.removeMessages(MSG_HIDE_SYSTEM_UI);
		mHandler.sendEmptyMessageDelayed(MSG_HIDE_SYSTEM_UI, DEFAULT_TIME_OUT);
		return mGestures.onTouchEvent(event) || super.onTouchEvent(event);
	}

	private TouchListener mTouchListener = new TouchListener() {
		@Override
		public void onGestureBegin() {
			mBrightness = mContext.getWindow().getAttributes().screenBrightness;
			mVolume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
			if (mBrightness <= 0.00f)
				mBrightness = 0.50f;
			if (mBrightness < 0.01f)
				mBrightness = 0.01f;
			if (mVolume < 0)
				mVolume = 0;
		}

		@Override
		public void onGestureEnd() {
			mOperationVolLum.setVisibility(View.GONE);
		}

		@Override
		public void onLeftSlide(float percent) {
			setBrightness(mBrightness + percent);
			setBrightnessScale(mContext.getWindow().getAttributes().screenBrightness);
		}

		@Override
		public void onRightSlide(float percent) {
			int v = (int) (percent * mMaxVolume) + mVolume;
			setVolume(v);
		}

		@Override
		public void onSingleTap() {
			if (mShowing)
				hide();
			else
				show();
			if (mPlayer.getBufferPercentage() >= 100)
				mPlayer.removeLoadingView();
		}

		@Override
		public void onDoubleTap() {
			toggleVideoMode(true, true);
		}

		@Override
		public void onLongPress() {
			doPauseResume();
		}

		@Override
		public void onScale(float scaleFactor, int state) {
			switch (state) {
			case CommonGestures.SCALE_STATE_BEGIN:
				mVideoMode = VideoView.VIDEO_LAYOUT_SCALE_ZOOM;
				mScreenToggle.setImageResource(R.drawable.mediacontroller_sreen_size_100);
				mPlayer.toggleVideoMode(mVideoMode);
				break;
			case CommonGestures.SCALE_STATE_SCALEING:
				float currentRatio = mPlayer.scale(scaleFactor);
				setOperationInfo((int) (currentRatio * 100) + "%", 500);
				break;
			case CommonGestures.SCALE_STATE_END:
				break;
			}
		}
	};

	private void setVolume(int v) {
		if (v > mMaxVolume)
			v = mMaxVolume;
		else if (v < 0)
			v = 0;
		mAM.setStreamVolume(AudioManager.STREAM_MUSIC, v, 0);
		setVolumeScale((float) v / mMaxVolume);
	}

	private void setBrightness(float f) {
		WindowManager.LayoutParams lp = mContext.getWindow().getAttributes();
		lp.screenBrightness = f;
		if (lp.screenBrightness > 1.0f)
			lp.screenBrightness = 1.0f;
		else if (lp.screenBrightness < 0.01f)
			lp.screenBrightness = 0.01f;
		mContext.getWindow().setAttributes(lp);
	}

	@Override
	public boolean onTrackballEvent(MotionEvent ev) {
		show(DEFAULT_TIME_OUT);
		return false;
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		int keyCode = event.getKeyCode();

		switch (keyCode) {
		case KeyEvent.KEYCODE_VOLUME_MUTE:
			return super.dispatchKeyEvent(event);
		case KeyEvent.KEYCODE_VOLUME_UP:
		case KeyEvent.KEYCODE_VOLUME_DOWN:
			mVolume = mAM.getStreamVolume(AudioManager.STREAM_MUSIC);
			int step = keyCode == KeyEvent.KEYCODE_VOLUME_UP ? 1 : -1;
			setVolume(mVolume + step);
			mHandler.removeMessages(MSG_HIDE_OPERATION_VOLLUM);
			mHandler.sendEmptyMessageDelayed(MSG_HIDE_OPERATION_VOLLUM, 500);
			return true;
		}

		if (isLocked()) {
			show();
			return true;
		}

		if (event.getRepeatCount() == 0 && (keyCode == KeyEvent.KEYCODE_HEADSETHOOK || keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE || keyCode == KeyEvent.KEYCODE_SPACE)) {
			doPauseResume();
			show(DEFAULT_TIME_OUT);
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_MEDIA_STOP) {
			if (mPlayer.isPlaying()) {
				mPlayer.pause();
				updatePausePlay();
			}
			return true;
		} else if (keyCode == KeyEvent.KEYCODE_BACK) {
			release();
			mPlayer.stop();
			return true;
		} else {
			show(DEFAULT_TIME_OUT);
		}
		return super.dispatchKeyEvent(event);
	}

	@TargetApi(11)
	private void showSystemUi(boolean visible) {
		if (UIUtils.hasHoneycomb()) {
			int flag = visible ? 0 : View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LOW_PROFILE;
			mRoot.setSystemUiVisibility(flag);
		}
	}

	private void showButtons(boolean showButtons) {
		Window window = mContext.getWindow();
		WindowManager.LayoutParams layoutParams = window.getAttributes();
		float val = showButtons ? -1 : 0;
		try {
			Field buttonBrightness = layoutParams.getClass().getField("buttonBrightness");
			buttonBrightness.set(layoutParams, val);
		} catch (Exception e) {
			Log.e("dimButtons", e);
		}
		window.setAttributes(layoutParams);
	}

	private void updatePausePlay() {
		if (mPlayer.isPlaying())
			mPauseButton.setImageResource(R.drawable.mediacontroller_pause);
		else
			mPauseButton.setImageResource(R.drawable.mediacontroller_play);
	}

	private void doPauseResume() {
		if (mPlayer.isPlaying())
			mPlayer.pause();
		else
			mPlayer.start();
		updatePausePlay();
	}

	private View.OnClickListener mPauseListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			if (mPlayer.isPlaying())
				show(DEFAULT_LONG_TIME_SHOW);
			else
				show();
			doPauseResume();
		}
	};

	private View.OnClickListener mLockClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			hide();
			lock(!mScreenLocked);
			show();
		}
	};

	private View.OnClickListener mScreenToggleListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			show(DEFAULT_TIME_OUT);
			toggleVideoMode(true, true);
		}
	};

	private View.OnClickListener mSnapshotListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			show(DEFAULT_TIME_OUT);
			mSnapshot.setEnabled(false);
			mPlayer.snapshot();
			mSnapshot.setEnabled(true);
		}
	};

	private OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		private boolean wasStopped = false;

		@Override
		public void onStartTrackingTouch(SeekBar bar) {
			mDragging = true;
			show(3600000);
			mHandler.removeMessages(MSG_SHOW_PROGRESS);
			wasStopped = !mPlayer.isPlaying();
			if (mInstantSeeking) {
				mAM.setStreamMute(AudioManager.STREAM_MUSIC, true);
				if (wasStopped) {
					mPlayer.start();
				}
			}
		}

		@Override
		public void onProgressChanged(SeekBar bar, int progress, boolean fromuser) {
			if (!fromuser)
				return;

			long newposition = (mDuration * progress) / 1000;
			String time = StringUtils.generateTime(newposition);
			if (mInstantSeeking)
				mPlayer.seekTo(newposition);
			setOperationInfo(time, 1500);
			mCurrentTime.setText(time);
		}

		@Override
		public void onStopTrackingTouch(SeekBar bar) {
			if (!mInstantSeeking) {
				mPlayer.seekTo((mDuration * bar.getProgress()) / 1000);
			} else if (wasStopped) {
				mPlayer.pause();
			}
			mOperationInfo.setVisibility(View.GONE);
			show(DEFAULT_TIME_OUT);
			mHandler.removeMessages(MSG_SHOW_PROGRESS);
			mAM.setStreamMute(AudioManager.STREAM_MUSIC, false);
			mDragging = false;
			mHandler.sendEmptyMessageDelayed(MSG_SHOW_PROGRESS, 1000);
		}
	};

	public interface MediaPlayerControl {
		void start();

		void pause();

		void stop();

		void seekTo(long pos);

		boolean isPlaying();

		long getDuration();

		long getCurrentPosition();

		int getBufferPercentage();

		void previous();

		void next();

		long goForward();

		long goBack();

		void toggleVideoMode(int mode);

		void showMenu();

		void removeLoadingView();

		float scale(float scale);

		void snapshot();
	}

}
