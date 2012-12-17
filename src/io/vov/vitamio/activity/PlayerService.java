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

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.SurfaceHolder;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnErrorListener;
import io.vov.vitamio.MediaPlayer.OnHWRenderFailedListener;
import io.vov.vitamio.MediaPlayer.OnInfoListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;
import io.vov.vitamio.MediaPlayer.OnSeekCompleteListener;
import io.vov.vitamio.MediaPlayer.OnVideoSizeChangedListener;
import io.vov.vitamio.utils.VitamioInstaller;
import com.yixia.zi.utils.FileHelper;
import com.yixia.zi.utils.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class PlayerService extends Service implements OnBufferingUpdateListener, OnCompletionListener, OnPreparedListener, OnVideoSizeChangedListener, OnErrorListener, OnInfoListener, OnSeekCompleteListener {
	private MediaPlayer mPlayer;
	private VPlayerListener mListener;
	private Uri mUri;
	private Uri mOldUri;
	private float mSeekTo = -1f;
	private boolean mFromNotification;
	private String[] mSubPaths;
	private boolean mInitialized;
	private final IBinder mBinder = new LocalBinder();
	private TelephonyManager mTelephonyManager;
	private int mCurrentState;
	private SurfaceHolder mSurfaceHolder;
	public static final int VPLYAER_NOTIFICATION_ID = 1;

	public static final int STATE_PREPARED = -1;
	public static final int STATE_PLAYING = 0;
	public static final int STATE_NEED_RESUME = 1;
	public static final int STATE_STOPPED = 2;
	public static final int STATE_RINGING = 3;

	private int mLastAudioTrack = -1;
	private String mLastSubTrack;
	private int mLastSubTrackId = -1;
	private long mMediaId = -1l;

	public class LocalBinder extends Binder {
		public PlayerService getService() {
			return PlayerService.this;
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		mInitialized = false;
		mTelephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
		mTelephonyManager.listen(mPhoneListener, PhoneStateListener.LISTEN_CALL_STATE);
		if (VitamioInstaller.isNativeLibsInited(this)) {
			vplayerInit();
		} else {
			stopSelf();
		}
		Log.d("CREATE OK");
	}

	private void vplayerInit() {
		mPlayer = new MediaPlayer(PlayerService.this.getApplicationContext(), false);
		mPlayer.setOnHWRenderFailedListener(new OnHWRenderFailedListener() {
			@Override
			public void onFailed() {
				if (mListener != null)
					mListener.onHWRenderFailed();
			}
		});
		mPlayer.setOnBufferingUpdateListener(PlayerService.this);
		mPlayer.setOnCompletionListener(PlayerService.this);
		mPlayer.setOnPreparedListener(PlayerService.this);
		mPlayer.setOnVideoSizeChangedListener(PlayerService.this);
		mPlayer.setOnErrorListener(PlayerService.this);
		mPlayer.setOnInfoListener(PlayerService.this);
	}

	public void releaseContext() {
		if (mPlayer != null)
			mPlayer.release();
		mPlayer = null;
	}

	@Override
	public IBinder onBind(Intent intent) {
		Log.d("BIND OK : " + intent.getPackage());
		return mBinder;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		release(true);
		releaseContext();
	}
	
	public boolean isInitialized() {
		return mInitialized;
	}

	public boolean initialize(Uri filePath, String displayName, boolean saveUri, float startPos, VPlayerListener listener, int parentId) {
		if (mPlayer == null)
			vplayerInit();
		mListener = listener;
		mOldUri = mUri;
		mUri = filePath;
		mSeekTo = startPos;
		mMediaId = -1;
		mLastAudioTrack = -1;
		mLastSubTrackId = -1;
		mLastSubTrack = "";
		setMediaTrack();
		Log.i("%s ==> %s, %s, %s, %s", mOldUri, mUri, mInitialized, mPrepared, mVideoSizeKnown);
		mFromNotification = mInitialized && mUri != null && mUri.equals(mOldUri);
		mListener.onOpenStart();
		if (!mFromNotification)
			openVideo();
		else
			openSuccess();
		return mInitialized;
	}

	private void setMediaTrack() {
	}

	private void openVideo() {
		if (mUri == null || mPlayer == null)
			return;

		mPlayer.reset();
		mInitialized = false;
		mPrepared = false;
		mVideoSizeKnown = false;

		try {
			mPlayer.setScreenOnWhilePlaying(true);
			mPlayer.setDataSource(PlayerService.this, mUri);
			if (mLastAudioTrack != -1)
				mPlayer.setInitialAudioTrack(mLastAudioTrack);
			if (mLastSubTrackId != -1)
				mPlayer.setInitialSubTrack(mLastSubTrackId);
			if (mSurfaceHolder != null && mSurfaceHolder.getSurface() != null && mSurfaceHolder.getSurface().isValid())
				mPlayer.setDisplay(mSurfaceHolder);
			mPlayer.prepareAsync();
		} catch (IllegalArgumentException e) {
			Log.e("openVideo", e);
		} catch (IllegalStateException e) {
			Log.e("openVideo", e);
		} catch (IOException e) {
			Log.e("openVideo", e);
		}
	}

	public Uri getUri() {
		return mUri;
	}

	public long getMediaId() {
		return mMediaId;
	}

	public int getLastAudioTrack() {
		return mLastAudioTrack;
	}

	public String getLastSubTrack() {
		return mLastSubTrack;
	}

	public int getLastSubTrackId() {
		return mLastSubTrackId;
	}

	public void setVPlayerListener(VPlayerListener listener) {
		mListener = listener;
	}

	public void setState(int state) {
		mCurrentState = state;
	}

	public boolean needResume() {
		return mInitialized && (mCurrentState == STATE_NEED_RESUME || mCurrentState == STATE_PREPARED);
	}

	public boolean ringingState() {
		return mInitialized && mCurrentState == STATE_RINGING;
	}

	public void release() {
		release(true);
	}

	private void release(boolean all) {
		if (mPlayer != null) {
			if (mListener != null)
				mListener.onCloseStart();
			mPlayer.reset();
			mInitialized = false;
			mPrepared = false;
			mVideoSizeKnown = false;
			if (mListener != null)
				mListener.onCloseComplete();
		}
		if (all) {
			mListener = null;
			mUri = null;
		}
	}

	public void stop() {
		if (mInitialized) {
			mPlayer.pause();
		}
	}

	public void start() {
		if (mInitialized) {
			mPlayer.start();
			setState(STATE_PLAYING);
		}
	}

	public void setDisplay(SurfaceHolder surface) {
		mSurfaceHolder = surface;
		if (mPlayer != null)
			mPlayer.setDisplay(surface);
	}

	public void releaseSurface() {
		if (mInitialized)
			mPlayer.releaseDisplay();
	}

	public boolean isPlaying() {
		return (mInitialized && mPlayer.isPlaying());
	}

	public int getVideoWidth() {
		if (mInitialized)
			return mPlayer.getVideoWidth();
		return 0;
	}

	public int getVideoHeight() {
		if (mInitialized)
			return mPlayer.getVideoHeight();
		return 0;
	}

	public float getVideoAspectRatio() {
		if (mInitialized)
			return mPlayer.getVideoAspectRatio();
		return 0f;
	}

	public long getDuration() {
		if (mInitialized)
			return mPlayer.getDuration();
		return 0;
	}

	public long getCurrentPosition() {
		if (mInitialized)
			return mPlayer.getCurrentPosition();
		return 0;
	}

	public Bitmap getCurrentFrame() {
		if (mInitialized)
			return mPlayer.getCurrentFrame();
		return null;
	}

	public float getBufferProgress() {
		if (mInitialized)
			return mPlayer.getBufferProgress();
		return 0f;
	}

	public void seekTo(float percent) {
		if (mInitialized)
			mPlayer.seekTo((int) (percent * getDuration()));
	}

	public String getMetaEncoding() {
		if (mInitialized)
			return mPlayer.getMetaEncoding();
		return null;
	}

	public void setAudioTrack(int num) {
		if (mInitialized)
			mPlayer.setAudioTrack(num);
	}

	public int getAudioTrack() {
		if (mInitialized)
			return mPlayer.getAudioTrack();
		return 0;
	}

	protected boolean isBuffering() {
		return (mInitialized && mPlayer.isBuffering());
	}

	protected void setBuffer(int bufSize) {
		if (mInitialized)
			mPlayer.setBufferSize(bufSize);
	}

	protected void setVolume(float left, float right) {
		if (mInitialized) {
			if (left <= 0f)
				left = 0f;
			else if (left >= 1f)
				left = 1f;
			if (right <= 0f)
				right = 0f;
			else if (right >= 1f)
				right = 1f;
			mPlayer.setVolume(left, right);
		}
	}

	protected void setVideoQuality(int quality) {
		if (mInitialized)
			mPlayer.setVideoQuality(quality);
	}

	protected void setDeinterlace(boolean deinterlace) {
		if (mInitialized)
			mPlayer.setDeinterlace(deinterlace);
	}

	public int getSubLocation() {
		if (mInitialized)
			return mPlayer.getSubLocation();
		return -1;
	}

	public void setSubPath(String subPath) {
		if (mInitialized)
			mPlayer.setSubPath(subPath);
	}

	public static interface VPlayerListener {
		public void onHWRenderFailed();

		public void onVideoSizeChanged(int width, int height);

		public void onOpenStart();

		public void onOpenSuccess();

		public void onOpenFailed();

		public void onBufferStart();

		public void onBufferComplete();

		public void onDownloadRateChanged(int kbPerSec);

		public void onPlaybackComplete();

		public void onCloseStart();

		public void onCloseComplete();
	}

	private PhoneStateListener mPhoneListener = new PhoneStateListener() {
		@Override
		public void onCallStateChanged(int state, String incomingNumber) {
			switch (state) {
			case TelephonyManager.CALL_STATE_IDLE:
				break;
			case TelephonyManager.CALL_STATE_OFFHOOK:
			case TelephonyManager.CALL_STATE_RINGING:
				if (isPlaying()) {
					stop();
					setState(STATE_RINGING);
				}
				break;
			default:
				break;
			}
		}
	};

	private boolean mVideoSizeKnown = false;
	private boolean mPrepared = false;

	@Override
	public void onVideoSizeChanged(MediaPlayer arg0, int arg1, int arg2) {
		mVideoSizeKnown = true;
		if (mListener != null)
			mListener.onVideoSizeChanged(arg1, arg2);
	}

	@Override
	public void onPrepared(MediaPlayer arg0) {
		mPrepared = true;
		openSuccess();
	}

	private void openSuccess() {
		mInitialized = true;
		if (!mFromNotification && mSeekTo > 0 && mSeekTo < 1)
			seekTo(mSeekTo);
		mSeekTo = -1;
		mListener.onOpenSuccess();
		if (!mFromNotification) {
			if (mUri != null)
				mSubPaths = getSubFiles(mUri.getPath());
			if (mSubPaths != null)
				setSubPath(FileHelper.getCanonical(new File(mSubPaths[0])));
		}
	}

	@Override
	public void onCompletion(MediaPlayer arg0) {
		if (mListener != null) {
			mListener.onPlaybackComplete();
		} else {
			release(true);
		}
	}

	@Override
	public void onBufferingUpdate(MediaPlayer arg0, int arg1) {
	}

	@Override
	public void onSeekComplete(MediaPlayer arg0) {
	}

	@Override
	public boolean onInfo(MediaPlayer arg0, int arg1, int arg2) {
		switch (arg1) {
		case MediaPlayer.MEDIA_INFO_BUFFERING_START:
			if (mListener != null)
				mListener.onBufferStart();
			else
				mPlayer.pause();
			break;
		case MediaPlayer.MEDIA_INFO_BUFFERING_END:
			if (mListener != null)
				mListener.onBufferComplete();
			else
				mPlayer.start();
			break;
		case MediaPlayer.MEDIA_INFO_DOWNLOAD_RATE_CHANGED:
			if (mListener != null)
				mListener.onDownloadRateChanged(arg2);
			break;
		}
		return true;
	}

	@Override
	public boolean onError(MediaPlayer arg0, int arg1, int arg2) {
		mListener.onOpenFailed();
		return true;
	}

	private String[] getSubFiles(String videoPath) {
		ArrayList<String> files = new ArrayList<String>();
		for (String ext : MediaPlayer.SUB_TYPES) {
			File s = new File(videoPath.substring(0, videoPath.lastIndexOf('.') > 0 ? videoPath.lastIndexOf('.') : videoPath.length()) + ext);
			if (s.exists() && s.isFile() && s.canRead())
				files.add(s.getAbsolutePath());
		}

		if (files.isEmpty())
			return null;
		else
			return files.toArray(new String[files.size()]);
	}

}
