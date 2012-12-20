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

package io.vov.vitamio.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.KeyguardManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import android.widget.Toast;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.R;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;
import com.yixia.zi.utils.BitmapHelper;
import com.yixia.zi.utils.FileHelper;
import com.yixia.zi.utils.FileUtils;
import com.yixia.zi.utils.IntentHelper;
import com.yixia.zi.utils.Log;
import com.yixia.zi.utils.Media;
import com.yixia.zi.utils.ToastHelper;
import com.yixia.zi.utils.UIUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicBoolean;

@SuppressLint("HandlerLeak")
public class VideoActivity extends Activity implements MediaController.MediaPlayerControl, VideoView.SurfaceCallback {

	private static final int RESULT_FAILED = -7;
	private static final int DEFAULT_BUF_SIZE = 0;
	private static final int DEFAULT_VIDEO_QUALITY = MediaPlayer.VIDEOQUALITY_MEDIUM;
	private static final boolean DEFAULT_DEINTERLACE = false;
	private static final float DEFAULT_ASPECT_RATIO = 0f;
	private static final float DEFAULT_STEREO_VOLUME = 1.0f;
	private static final String SNAP_SHOT_PATH = "/Player";

	private static final IntentFilter USER_PRESENT_FILTER = new IntentFilter(Intent.ACTION_USER_PRESENT);
	private static final IntentFilter SCREEN_FILTER = new IntentFilter(Intent.ACTION_SCREEN_ON);
	private static final IntentFilter HEADSET_FILTER = new IntentFilter(Intent.ACTION_HEADSET_PLUG);
	private static final IntentFilter BATTERY_FILTER = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);

	private boolean mCreated = false;
	private boolean mNeedLock;
	private String mDisplayName;
	private String mBatteryLevel;
	private boolean mFromStart;
	private int mLoopCount;
	private boolean mSaveUri;
	private int mParentId;
	private float mStartPos;
	private View mViewRoot;
	private VideoView mVideoView;
	private View mVideoLoadingLayout;
	private TextView mVideoLoadingText;
	private Uri mUri;
	private ScreenReceiver mScreenReceiver;
	private HeadsetPlugReceiver mHeadsetPlugReceiver;
	private UserPresentReceiver mUserPresentReceiver;
	private BatteryReceiver mBatteryReceiver;
	private boolean mReceiverRegistered = false;
	private boolean mHeadsetPlaying = false;
	private boolean mCloseComplete = false;

	private MediaController mMediaController;
	private PlayerService vPlayer;
	private ServiceConnection vPlayerServiceConnection;
	private Animation mLoadingAnimation;
	private View mLoadingProgressView;

	static {
		SCREEN_FILTER.addAction(Intent.ACTION_SCREEN_OFF);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;

		vPlayerServiceConnection = new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName name, IBinder service) {
				vPlayer = ((PlayerService.LocalBinder) service).getService();
				mServiceConnected = true;
				if (mSurfaceCreated)
					vPlayerHandler.sendEmptyMessage(OPEN_FILE);
			}

			@Override
			public void onServiceDisconnected(ComponentName name) {
				vPlayer = null;
				mServiceConnected = false;
			}
		};

		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		parseIntent(getIntent());
		loadView(R.layout.activity_video);
		manageReceivers();

		mCreated = true;
	}

	private void attachMediaController() {
		if (mMediaController != null) {
			mNeedLock = mMediaController.isLocked();
			mMediaController.release();
		}
		mMediaController = new MediaController(this, mNeedLock);
		mMediaController.setMediaPlayer(this);
		mMediaController.setAnchorView(mVideoView.getRootView());
		setFileName();
		setBatteryLevel();
	}

	@Override
	public void onStart() {
		super.onStart();
		if (!mCreated)
			return;
		bindService(new Intent(this, PlayerService.class), vPlayerServiceConnection, Context.BIND_AUTO_CREATE);
	}

	@Override
	public void onResume() {
		super.onResume();
		if (!mCreated)
			return;

		if (isInitialized()) {
			KeyguardManager keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
			if (!keyguardManager.inKeyguardRestrictedInputMode()) {
				startPlayer();
			}
		} else {
			if (mCloseComplete) {
				reOpen();
			}
		}
	}

	@Override
	public void onPause() {
		super.onPause();
		if (!mCreated)
			return;
		if (isInitialized()) {
			if (vPlayer != null && vPlayer.isPlaying()) {
				stopPlayer();
			}
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		if (!mCreated)
			return;
		if (isInitialized()) {
			vPlayer.releaseSurface();
		}
		if (mServiceConnected) {
			unbindService(vPlayerServiceConnection);
			mServiceConnected = false;
		}

	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (!mCreated)
			return;
		manageReceivers();
		if (isInitialized() && !vPlayer.isPlaying())
			release();
		if (mMediaController != null)
			mMediaController.release();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (isInitialized()) {
			setVideoLayout();
			attachMediaController();
		}

		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		super.onBackPressed();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// http://code.google.com/p/android/issues/detail?id=19917
		outState.putString("WORKAROUND_FOR_BUG_19917_KEY", "WORKAROUND_FOR_BUG_19917_VALUE");
		super.onSaveInstanceState(outState);
	}

	public void showMenu() {
		
	}

	private void loadView(int id) {
		setContentView(id);
		mViewRoot = findViewById(R.id.video_root);
		mVideoView = (VideoView) findViewById(R.id.video);
		mVideoView.initialize(this, this, false);
		mVideoLoadingText = (TextView) findViewById(R.id.video_loading_text);
		mVideoLoadingLayout = findViewById(R.id.video_loading);
		mLoadingProgressView = mVideoLoadingLayout.findViewById(R.id.video_loading_progress);

		mLoadingAnimation = AnimationUtils.loadAnimation(VideoActivity.this, R.anim.loading_rotate);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
	}

	private void parseIntent(Intent i) {

		Uri dat = IntentHelper.getIntentUri(i);
		if (dat == null)
			resultFinish(RESULT_FAILED);

		String datString = dat.toString();
		if (!datString.equals(dat.toString()))
			dat = Uri.parse(datString);

		mUri = dat;

		mNeedLock = i.getBooleanExtra("lockScreen", false);
		mDisplayName = i.getStringExtra("displayName");
		mFromStart = i.getBooleanExtra("fromStart", false);
		mSaveUri = i.getBooleanExtra("saveUri", true);
		mStartPos = i.getFloatExtra("startPosition", -1.0f);
		mLoopCount = i.getIntExtra("loopCount", 1);
		mParentId = i.getIntExtra("parentId", 0);
		Log.i("L: %b, N: %s, S: %b, P: %f, LP: %d", mNeedLock, mDisplayName, mFromStart, mStartPos, mLoopCount);
	}

	private void manageReceivers() {
		if (!mReceiverRegistered) {
			mScreenReceiver = new ScreenReceiver();
			registerReceiver(mScreenReceiver, SCREEN_FILTER);
			mUserPresentReceiver = new UserPresentReceiver();
			registerReceiver(mUserPresentReceiver, USER_PRESENT_FILTER);
			mBatteryReceiver = new BatteryReceiver();
			registerReceiver(mBatteryReceiver, BATTERY_FILTER);
			mHeadsetPlugReceiver = new HeadsetPlugReceiver();
			registerReceiver(mHeadsetPlugReceiver, HEADSET_FILTER);
			mReceiverRegistered = true;
		} else {
			try {
				if (mScreenReceiver != null)
					unregisterReceiver(mScreenReceiver);
				if (mUserPresentReceiver != null)
					unregisterReceiver(mUserPresentReceiver);
				if (mHeadsetPlugReceiver != null)
					unregisterReceiver(mHeadsetPlugReceiver);
				if (mBatteryReceiver != null)
					unregisterReceiver(mBatteryReceiver);
			} catch (IllegalArgumentException e) {
			}
			mReceiverRegistered = false;
		}
	}

	private void setFileName() {
		if (mUri != null) {
			String name = null;
			if (mUri.getScheme() == null || mUri.getScheme().equals("file"))
				name = FileUtils.getName(mUri.toString());
			else
				name = mUri.getLastPathSegment();
			if (name == null)
				name = "null";
			if (mDisplayName == null)
				mDisplayName = name;
			mMediaController.setFileName(mDisplayName);
		}
	}

	private void applyResult(int resultCode) {
		vPlayerHandler.removeMessages(BUFFER_PROGRESS);
		Intent i = new Intent();
		i.putExtra("filePath", mUri.toString());
		if (isInitialized()) {
			i.putExtra("position", (double) vPlayer.getCurrentPosition() / vPlayer.getDuration());
			i.putExtra("duration", vPlayer.getDuration());
		}
		switch (resultCode) {
		case RESULT_FAILED:
			ToastHelper.showToast(this, Toast.LENGTH_LONG, R.string.video_cannot_play);
			break;
		case RESULT_CANCELED:
		case RESULT_OK:
			break;
		}
		setResult(resultCode, i);
	}

	private void resultFinish(int resultCode) {
		applyResult(resultCode);
		if (UIUtils.hasICS() && resultCode != RESULT_FAILED) {
			android.os.Process.killProcess(android.os.Process.myPid());
		} else {
			finish();
		}
	}

	private void release() {
		if (vPlayer != null) {
			if (UIUtils.hasICS()) {
				android.os.Process.killProcess(android.os.Process.myPid());
			} else {
				vPlayer.release();
				vPlayer.releaseContext();
			}
		}
	}

	private void reOpen(Uri path, String name, boolean fromStart) {
		if (isInitialized()) {
			vPlayer.release();
			vPlayer.releaseContext();
		}
		Intent i = getIntent();
		i.putExtra("lockScreen", mMediaController.isLocked());
		i.putExtra("startPosition", 7.7f);
		i.putExtra("fromStart", fromStart);
		i.putExtra("displayName", name);
		i.setData(path);
		parseIntent(i);
		mUri = path;
		if (mViewRoot != null)
			mViewRoot.invalidate();
		mOpened.set(false);
		loadView(R.layout.activity_video);
	}

	public void reOpen() {
		reOpen(mUri, mDisplayName, false);
	}

	protected void startPlayer() {
		if (isInitialized() && mScreenReceiver.screenOn && !vPlayer.isBuffering()) {
			Log.i("VideoActivity#startPlayer");
			if (!vPlayer.isPlaying()) {
				vPlayer.start();
			}
		}
	}

	protected void stopPlayer() {
		if (isInitialized()) {
			vPlayer.stop();
		}
	}

	private void setBatteryLevel() {
		if (mMediaController != null)
			mMediaController.setBatteryLevel(mBatteryLevel);
	}

	private class BatteryReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			int level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0);
			int scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
			int percent = scale > 0 ? level * 100 / scale : 0;
			if (percent > 100)
				percent = 100;
			mBatteryLevel = String.valueOf(percent) + "%";
			setBatteryLevel();
		}
	}

	private class UserPresentReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (isRootActivity()) {
				startPlayer();
			}
		}
	}

	private boolean isRootActivity() {
		ActivityManager activity = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
		return activity.getRunningTasks(1).get(0).topActivity.flattenToString().endsWith("io.vov.vitamio.activity.VideoActivity");
	}

	public class HeadsetPlugReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent != null && intent.hasExtra("state")) {
				int state = intent.getIntExtra("state", -1);
				if (state == 0) {
					mHeadsetPlaying = isPlaying();
					stopPlayer();
				} else if (state == 1) {
					if (mHeadsetPlaying)
						startPlayer();
				}
			}
		};
	}

	private class ScreenReceiver extends BroadcastReceiver {
		private boolean screenOn = true;

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
				screenOn = false;
				stopPlayer();
			} else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
				screenOn = true;
			}
		}
	}

	private void loadVPlayerPrefs() {
		if (!isInitialized())
			return;
		vPlayer.setBuffer(VideoActivity.DEFAULT_BUF_SIZE);
		vPlayer.setVideoQuality(DEFAULT_VIDEO_QUALITY);
		vPlayer.setDeinterlace(DEFAULT_DEINTERLACE);
		vPlayer.setVolume(DEFAULT_STEREO_VOLUME, DEFAULT_STEREO_VOLUME);
		if (mVideoView != null && isInitialized())
			setVideoLayout();
	}

	private boolean isInitialized() {
		return (mCreated && vPlayer != null && vPlayer.isInitialized());
	}

	private AtomicBoolean mOpened = new AtomicBoolean(Boolean.FALSE);
	private boolean mSurfaceCreated = false;
	private boolean mServiceConnected = false;
	private Object mOpenLock = new Object();
	private static final int OPEN_FILE = 0;
	private static final int OPEN_START = 1;
	private static final int OPEN_SUCCESS = 2;
	private static final int OPEN_FAILED = 3;
	private static final int HW_FAILED = 4;
	private static final int LOAD_PREFS = 5;
	private static final int BUFFER_START = 11;
	private static final int BUFFER_PROGRESS = 12;
	private static final int BUFFER_COMPLETE = 13;
	private static final int CLOSE_START = 21;
	private static final int CLOSE_COMPLETE = 22;
	private Handler vPlayerHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case OPEN_FILE:
				synchronized (mOpenLock) {
					if (!mOpened.get() && vPlayer != null) {
						mOpened.set(true);
						vPlayer.setVPlayerListener(vPlayerListener);
						if (vPlayer.isInitialized())
							mUri = vPlayer.getUri();

						if (mVideoView != null)
							vPlayer.setDisplay(mVideoView.getHolder());
						if (mUri != null)
							vPlayer.initialize(mUri, mDisplayName, mSaveUri, getStartPosition(), vPlayerListener, mParentId);
					}
				}
				break;
			case OPEN_START:
				mVideoLoadingText.setText(R.string.video_layout_loading);
				setVideoLoadingLayoutVisibility(View.VISIBLE);
				break;
			case OPEN_SUCCESS:
				loadVPlayerPrefs();
				setVideoLoadingLayoutVisibility(View.GONE);
				setVideoLayout();
				vPlayer.start();
				attachMediaController();
				break;
			case OPEN_FAILED:
				resultFinish(RESULT_FAILED);
				break;
			case BUFFER_START:
				setVideoLoadingLayoutVisibility(View.VISIBLE);
				vPlayerHandler.sendEmptyMessageDelayed(BUFFER_PROGRESS, 1000);
				break;
			case BUFFER_PROGRESS:
				if (vPlayer.getBufferProgress() >= 100) {
					setVideoLoadingLayoutVisibility(View.GONE);
				} else {
					mVideoLoadingText.setText(getString(R.string.video_layout_buffering_progress, vPlayer.getBufferProgress()));
					vPlayerHandler.sendEmptyMessageDelayed(BUFFER_PROGRESS, 1000);
					stopPlayer();
				}
				break;
			case BUFFER_COMPLETE:
				setVideoLoadingLayoutVisibility(View.GONE);
				vPlayerHandler.removeMessages(BUFFER_PROGRESS);
				break;
			case CLOSE_START:
				mVideoLoadingText.setText(R.string.closing_file);
				setVideoLoadingLayoutVisibility(View.VISIBLE);
				break;
			case CLOSE_COMPLETE:
				mCloseComplete = true;
				break;
			case HW_FAILED:
				if (mVideoView != null) {
					mVideoView.setVisibility(View.GONE);
					mVideoView.setVisibility(View.VISIBLE);
					mVideoView.initialize(VideoActivity.this, VideoActivity.this, false);
				}
				break;
			case LOAD_PREFS:
				loadVPlayerPrefs();
				break;
			}
		}
	};

	private void setVideoLoadingLayoutVisibility(int visibility) {
		if (mVideoLoadingLayout != null && mLoadingProgressView != null) {
			if (visibility == View.VISIBLE)
				mLoadingProgressView.startAnimation(mLoadingAnimation);
			mVideoLoadingLayout.setVisibility(visibility);
		}
	}

	private PlayerService.VPlayerListener vPlayerListener = new PlayerService.VPlayerListener() {
		@Override
		public void onHWRenderFailed() {

		}

		@Override
		public void onOpenStart() {
			vPlayerHandler.sendEmptyMessage(OPEN_START);
		}

		@Override
		public void onOpenSuccess() {
			vPlayerHandler.sendEmptyMessage(OPEN_SUCCESS);
		}

		@Override
		public void onOpenFailed() {
			vPlayerHandler.sendEmptyMessage(OPEN_FAILED);
		}

		@Override
		public void onBufferStart() {
			vPlayerHandler.sendEmptyMessage(BUFFER_START);
			stopPlayer();
		}

		@Override
		public void onBufferComplete() {
			Log.i("VideoActivity#onBufferComplete " + vPlayer.needResume());
			vPlayerHandler.sendEmptyMessage(BUFFER_COMPLETE);
			if (vPlayer != null && !vPlayer.needResume())
				startPlayer();
		}

		@Override
		public void onPlaybackComplete() {
			if (mLoopCount == 0 || mLoopCount-- > 1) {
				vPlayer.start();
				vPlayer.seekTo(0);
			} else {
				resultFinish(RESULT_OK);
			}
		}

		@Override
		public void onCloseStart() {
			vPlayerHandler.sendEmptyMessage(CLOSE_START);
		}

		@Override
		public void onCloseComplete() {
			vPlayerHandler.sendEmptyMessage(CLOSE_COMPLETE);
		}

		@Override
		public void onVideoSizeChanged(int width, int height) {
			if (mVideoView != null) {
				setVideoLayout();
			}
		}

		@Override
		public void onDownloadRateChanged(int kbPerSec) {
			if (!Media.isNative(mUri.toString()) && mMediaController != null) {
				mMediaController.setDownloadRate(String.format("%dKB/s", kbPerSec));
			}
		}
	};

	private int mVideoMode = VideoView.VIDEO_LAYOUT_SCALE;

	public static final String SESSION_LAST_POSITION_SUFIX = ".last";

	private void setVideoLayout() {
		mVideoView.setVideoLayout(mVideoMode, DEFAULT_ASPECT_RATIO, vPlayer.getVideoWidth(), vPlayer.getVideoHeight(), vPlayer.getVideoAspectRatio());
	}

	private float getStartPosition() {
		return mStartPos;
	}

	@Override
	public int getBufferPercentage() {
		if (isInitialized())
			return (int) (vPlayer.getBufferProgress() * 100);
		return 0;
	}

	@Override
	public long getCurrentPosition() {
		if (isInitialized())
			return vPlayer.getCurrentPosition();
		return (long) (getStartPosition() * vPlayer.getDuration());
	}

	@Override
	public long getDuration() {
		if (isInitialized())
			return vPlayer.getDuration();
		return 0;
	}

	@Override
	public boolean isPlaying() {
		if (isInitialized())
			return vPlayer.isPlaying();
		return false;
	}

	@Override
	public void pause() {
		if (isInitialized())
			vPlayer.stop();
	}

	@Override
	public void seekTo(long arg0) {
		if (isInitialized())
			vPlayer.seekTo((float) ((double) arg0 / vPlayer.getDuration()));
	}

	@Override
	public void start() {
		if (isInitialized())
			vPlayer.start();
	}

	@Override
	public void previous() {
	}

	@Override
	public void next() {
	}

	private static final int VIDEO_MAXIMUM_HEIGHT = 2048;
	private static final int VIDEO_MAXIMUM_WIDTH = 2048;

	@Override
	public float scale(float scaleFactor) {
		float userRatio = DEFAULT_ASPECT_RATIO;
		int videoWidth = vPlayer.getVideoWidth();
		int videoHeight = vPlayer.getVideoHeight();
		float videoRatio = vPlayer.getVideoAspectRatio();
		float currentRatio = mVideoView.mVideoHeight / (float) videoHeight;

		currentRatio += (scaleFactor - 1);
		if (videoWidth * currentRatio >= VIDEO_MAXIMUM_WIDTH)
			currentRatio = VIDEO_MAXIMUM_WIDTH / (float) videoWidth;

		if (videoHeight * currentRatio >= VIDEO_MAXIMUM_HEIGHT)
			currentRatio = VIDEO_MAXIMUM_HEIGHT / (float) videoHeight;

		if (currentRatio < 0.5f)
			currentRatio = 0.5f;

		mVideoView.mVideoHeight = (int) (videoHeight * currentRatio);
		mVideoView.setVideoLayout(mVideoMode, userRatio, videoWidth, videoHeight, videoRatio);
		return currentRatio;
	}

	@SuppressLint("SimpleDateFormat")
	@Override
	public void snapshot() {
		if (!FileHelper.sdAvailable()) {
			ToastHelper.showToast(this, R.string.file_explorer_sdcard_not_available);
		} else {
			Uri imgUri = null;
			Bitmap bitmap = vPlayer.getCurrentFrame();
			if (bitmap != null) {
				File screenshotsDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + SNAP_SHOT_PATH);
				if (!screenshotsDirectory.exists()) {
					screenshotsDirectory.mkdirs();
				}

				File savePath = new File(screenshotsDirectory.getPath() + "/" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()) + ".jpg");
				if (BitmapHelper.saveBitmapToFile(bitmap, savePath.getPath())) {
					imgUri = Uri.fromFile(savePath);
				}
			}
			if (imgUri != null) {
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, imgUri));
				ToastHelper.showToast(this, Toast.LENGTH_LONG, getString(R.string.video_screenshot_save_in, imgUri.getPath()));
			} else {
				ToastHelper.showToast(this, R.string.video_screenshot_failed);
			}
		}
	}

	@Override
	public void toggleVideoMode(int mode) {
		mVideoMode = mode;
		setVideoLayout();
	}

	@Override
	public void stop() {
		onBackPressed();
	}

	@Override
	public long goForward() {
		return 0;
	}

	@Override
	public long goBack() {
		return 0;
	}

	@Override
	public void removeLoadingView() {
		mVideoLoadingLayout.setVisibility(View.GONE);
	}

	@Override
	public void onSurfaceCreated(SurfaceHolder holder) {
		Log.i("onSurfaceCreated");
		mSurfaceCreated = true;
		if (mServiceConnected)
			vPlayerHandler.sendEmptyMessage(OPEN_FILE);
		if (vPlayer != null)
			vPlayer.setDisplay(holder);
	}

	@Override
	public void onSurfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		if (vPlayer != null) {
			vPlayer.setDisplay(holder);
			setVideoLayout();
		}
	}

	@Override
	public void onSurfaceDestroyed(SurfaceHolder holder) {
		Log.i("onSurfaceDestroyed");
		if (vPlayer != null && vPlayer.isInitialized()) {
			if (vPlayer.isPlaying()) {
				vPlayer.stop();
				vPlayer.setState(PlayerService.STATE_NEED_RESUME);
			}
			vPlayer.releaseSurface();
			if (vPlayer.needResume())
				vPlayer.start();
		}
	}
}
