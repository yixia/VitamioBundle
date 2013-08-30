/*
 * Copyright (C) 2006 The Android Open Source Project
 * Copyright (C) 2013 YIXIA.COM
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

package io.vov.vitamio;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.util.SparseArray;
import android.view.Surface;
import android.view.SurfaceHolder;

import io.vov.vitamio.utils.FileUtils;
import io.vov.vitamio.utils.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * MediaPlayer class can be used to control playback of audio/video files and
 * streams. An example on how to use the methods in this class can be found in
 * {@link io.vov.vitamio.widget.VideoView}. This class will function the same as
 * android.media.MediaPlayer in most cases. Please see <a
 * href="http://developer.android.com/guide/topics/media/index.html">Audio and
 * Video</a> for additional help using MediaPlayer.
 */
public class MediaPlayer {
  public static final int CACHE_TYPE_NOT_AVAILABLE = 1;
  public static final int CACHE_TYPE_UPDATE = 2;
  public static final int CACHE_TYPE_SPEED = 3;
  public static final int CACHE_INFO_NO_SPACE = 1;
  public static final int CACHE_INFO_STREAM_NOT_SUPPORT = 2;
  public static final int MEDIA_ERROR_UNKNOWN = 1;
  public static final int MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK = 200;
  /**
   * The video is too complex for the decoder: it can't decode frames fast
   * enough. Possibly only the audio plays fine at this stage.
   *
   * @see io.vov.vitamio.MediaPlayer.OnInfoListener
   */
  public static final int MEDIA_INFO_VIDEO_TRACK_LAGGING = 700;
  /**
   * MediaPlayer is temporarily pausing playback internally in order to buffer
   * more data.
   */
  public static final int MEDIA_INFO_BUFFERING_START = 701;
  /**
   * MediaPlayer is resuming playback after filling buffers.
   */
  public static final int MEDIA_INFO_BUFFERING_END = 702;
  /**
   * The media cannot be seeked (e.g live stream)
   *
   * @see io.vov.vitamio.MediaPlayer.OnInfoListener
   */
  public static final int MEDIA_INFO_NOT_SEEKABLE = 801;
  /**
   * The rate in KB/s of av_read_frame()
   *
   * @see io.vov.vitamio.MediaPlayer.OnInfoListener
   */
  public static final int MEDIA_INFO_DOWNLOAD_RATE_CHANGED = 901;
  public static final int VIDEOQUALITY_LOW = -16;
  public static final int VIDEOQUALITY_MEDIUM = 0;
  public static final int VIDEOQUALITY_HIGH = 16;

  public static final int VIDEOCHROMA_RGB565 = 0;
  public static final int VIDEOCHROMA_RGBA = 1;
  /**
   * The subtitle displayed is embeded in the movie
   */
  public static final int SUBTITLE_INTERNAL = 0;
  /**
   * The subtitle displayed is an external file
   */
  public static final int SUBTITLE_EXTERNAL = 1;
  /**
   * The external subtitle types which Vitamio supports.
   */
  public static final String[] SUB_TYPES = {".srt", ".ssa", ".smi", ".txt", ".sub", ".ass"};
  private static final int MEDIA_NOP = 0;
  private static final int MEDIA_PREPARED = 1;
  private static final int MEDIA_PLAYBACK_COMPLETE = 2;
  private static final int MEDIA_BUFFERING_UPDATE = 3;
  private static final int MEDIA_SEEK_COMPLETE = 4;
  private static final int MEDIA_SET_VIDEO_SIZE = 5;
  private static final int MEDIA_ERROR = 100;
  private static final int MEDIA_INFO = 200;
  private static final int MEDIA_CACHE = 300;
  private static final int MEDIA_HW_ERROR = 400;
  private static final int MEDIA_TIMED_TEXT = 1000;
  private static final int MEDIA_CACHING_UPDATE = 2000;
  private static final String MEDIA_CACHING_SEGMENTS = "caching_segment";
  private static final String MEDIA_CACHING_TYPE = "caching_type";
  private static final String MEDIA_CACHING_INFO = "caching_info";
  private static final String MEDIA_SUBTITLE_STRING = "sub_string";
  private static final String MEDIA_SUBTITLE_BYTES = "sub_bytes";
  private static final String MEDIA_SUBTITLE_TYPE = "sub_type";
  private static final int SUBTITLE_TEXT = 0;
  private static final int SUBTITLE_BITMAP = 1;
  private static AtomicBoolean NATIVE_OMX_LOADED = new AtomicBoolean(false);
  private Context mContext;
  private Surface mSurface;
  private SurfaceHolder mSurfaceHolder;
  private EventHandler mEventHandler;
  private PowerManager.WakeLock mWakeLock = null;
  private boolean mScreenOnWhilePlaying;
  private boolean mStayAwake;
  private Metadata mMeta;
  private AssetFileDescriptor mFD = null;
  private OnHWRenderFailedListener mOnHWRenderFailedListener;
  private OnPreparedListener mOnPreparedListener;
  private OnCompletionListener mOnCompletionListener;
  private OnBufferingUpdateListener mOnBufferingUpdateListener;
  private OnCachingUpdateListener mOnCachingUpdateListener;
  private OnSeekCompleteListener mOnSeekCompleteListener;
  private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
  private OnErrorListener mOnErrorListener;
  /**
   * Register a callback to be invoked when an info/warning is available.
   *
   * @param listener
   * the callback that will be run
   */
  private OnInfoListener mOnInfoListener;
  private OnTimedTextListener mOnTimedTextListener;
  private AudioTrack mAudioTrack;
  private int mAudioTrackBufferSize;
  private Surface mLocalSurface;
  private Bitmap mBitmap;
  private ByteBuffer mByteBuffer;

  /**
   * Default constructor. The same as Android's MediaPlayer().
   * <p>
   * When done with the MediaPlayer, you should call {@link #release()}, to free
   * the resources. If not released, too many MediaPlayer instances may result
   * in an exception.
   * </p>
   */
  public MediaPlayer(Context ctx) {
    this(ctx, false);
  }

  /**
   * Default constructor. The same as Android's MediaPlayer().
   * <p>
   * When done with the MediaPlayer, you should call {@link #release()}, to free
   * the resources. If not released, too many MediaPlayer instances may result
   * in an exception.
   * </p>
   *
   * @param preferHWDecoder MediaPlayer will try to use hardware accelerated decoder if true
   */
  public MediaPlayer(Context ctx, boolean preferHWDecoder) {
    mContext = ctx;

    String LIB_ROOT = Vitamio.getLibraryPath();
    if (preferHWDecoder) {
      if (!NATIVE_OMX_LOADED.get()) {
        if (Build.VERSION.SDK_INT > 17)
          loadOMX_native(LIB_ROOT + "libOMX.18.so");
        else if (Build.VERSION.SDK_INT > 13)
          loadOMX_native(LIB_ROOT + "libOMX.14.so");
        else if (Build.VERSION.SDK_INT > 10)
          loadOMX_native(LIB_ROOT + "libOMX.11.so");
        else
          loadOMX_native(LIB_ROOT + "libOMX.9.so");
        NATIVE_OMX_LOADED.set(true);
      }
    } else {
      unloadOMX_native();
      NATIVE_OMX_LOADED.set(false);
    }

    Looper looper;
    if ((looper = Looper.myLooper()) != null)
      mEventHandler = new EventHandler(this, looper);
    else if ((looper = Looper.getMainLooper()) != null)
      mEventHandler = new EventHandler(this, looper);
    else
      mEventHandler = null;

    native_init();
  }

  static {
    String LIB_ROOT = Vitamio.getLibraryPath();
    try {
      Log.i("LIB ROOT: %s", LIB_ROOT);
      System.load(LIB_ROOT + "libstlport_shared.so");
      System.load(LIB_ROOT + "libvplayer.so");
      loadFFmpeg_native(LIB_ROOT + "libffmpeg.so");
      boolean vvo_loaded = false;
      if (Build.VERSION.SDK_INT > 8)
        vvo_loaded = loadVVO_native(LIB_ROOT + "libvvo.9.so");
      else if (Build.VERSION.SDK_INT > 7)
        vvo_loaded = loadVVO_native(LIB_ROOT + "libvvo.8.so");
      else
        vvo_loaded = loadVVO_native(LIB_ROOT + "libvvo.7.so");
      if (!vvo_loaded) {
        vvo_loaded = loadVVO_native(LIB_ROOT + "libvvo.j.so");
        Log.d("FALLBACK TO VVO JNI " + vvo_loaded);
      }
      loadVAO_native(LIB_ROOT + "libvao.0.so");
    } catch (java.lang.UnsatisfiedLinkError e) {
      Log.e("Error loading libs", e);
    }
  }

  private static void postEventFromNative(Object mediaplayer_ref, int what, int arg1, int arg2, Object obj) {
    MediaPlayer mp = (MediaPlayer) (mediaplayer_ref);
    if (mp == null)
      return;

    if (mp.mEventHandler != null) {
      Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
      mp.mEventHandler.sendMessage(m);
    }
  }

  private static native boolean loadVAO_native(String vaoPath);

  private static native boolean loadVVO_native(String vvoPath);

  private static native boolean loadOMX_native(String omxPath);

  private static native void unloadOMX_native();

  private static native boolean loadFFmpeg_native(String ffmpegPath);

  private native void _setVideoSurface();

  /**
   * Sets the SurfaceHolder to use for displaying the video portion of the
   * media. This call is optional. Not calling it when playing back a video will
   * result in only the audio track being played.
   *
   * @param sh the SurfaceHolder to use for video display
   */
  public void setDisplay(SurfaceHolder sh) {
    if (sh == null) {
      releaseDisplay();
    } else {
      mSurfaceHolder = sh;
      mSurface = sh.getSurface();
      _setVideoSurface();
      updateSurfaceScreenOn();
    }
  }

  /**
   * Sets the Surface to use for displaying the video portion of the media. This
   * is similar to {@link #setDisplay(SurfaceHolder)}.
   *
   * @param surface the Surface to use for video display
   */
  public void setSurface(Surface surface) {
    if (surface == null) {
      releaseDisplay();
    } else {
      mSurfaceHolder = null;
      mSurface = surface;
      _setVideoSurface();
      updateSurfaceScreenOn();
    }
  }

  /**
   * Sets the data source (file-path or http/rtsp URL) to use.
   *
   * @param path the path of the file, or the http/rtsp URL of the stream you want
   *             to play
   * @throws IllegalStateException if it is called in an invalid state
   *                               <p/>
   *                               <p/>
   *                               When <code>path</code> refers to a local file, the file may
   *                               actually be opened by a process other than the calling
   *                               application. This implies that the pathname should be an absolute
   *                               path (as any other process runs with unspecified current working
   *                               directory), and that the pathname should reference a
   *                               world-readable file. As an alternative, the application could
   *                               first open the file for reading, and then use the file descriptor
   *                               form {@link #setDataSource(FileDescriptor)}.
   */
  public void setDataSource(String path) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
    _setDataSource(path, null, null);
  }

  /**
   * Sets the data source as a content Uri.
   *
   * @param context the Context to use when resolving the Uri
   * @param uri     the Content URI of the data you want to play
   * @throws IllegalStateException if it is called in an invalid state
   */
  public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
    setDataSource(context, uri, null);
  }

  public void setDataSource(Context context, Uri uri, Map<String, String> headers) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
    if (context == null || uri == null)
      throw new IllegalArgumentException();
    String scheme = uri.getScheme();
    if (scheme == null || scheme.equals("file")) {
      setDataSource(FileUtils.getPath(uri.toString()));
      return;
    }

    try {
      ContentResolver resolver = context.getContentResolver();
      mFD = resolver.openAssetFileDescriptor(uri, "r");
      if (mFD == null)
        return;
      setDataSource(mFD.getParcelFileDescriptor().getFileDescriptor());
      return;
    } catch (Exception e) {
      closeFD();
    }
    setDataSource(uri.toString(), headers);
  }
  
  /**
   * Sets the data source (file-path or http/rtsp URL) to use.
   *
   * @param path the path of the file, or the http/rtsp URL of the stream you want to play
   * @param headers the headers associated with the http request for the stream you want to play
   * @throws IllegalStateException if it is called in an invalid state
   */
  public void setDataSource(String path, Map<String, String> headers)
          throws IOException, IllegalArgumentException, SecurityException, IllegalStateException
  {
      String[] keys = null;
      String[] values = null;

      if (headers != null) {
          keys = new String[headers.size()];
          values = new String[headers.size()];

          int i = 0;
          for (Map.Entry<String, String> entry: headers.entrySet()) {
              keys[i] = entry.getKey();
              values[i] = entry.getValue();
              ++i;
          }
      }
      setDataSource(path, keys, values);
  }
  
  /**
   * Sets the data source (file-path or http/rtsp URL) to use.
   *
   * @param path the path of the file, or the http/rtsp URL of the stream you want to play
   * @param keys   AVOption key
   * @param values AVOption value
   * @throws IllegalStateException if it is called in an invalid state
   */
	public void setDataSource(String path, String[] keys, String[] values) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
		final Uri uri = Uri.parse(path);
		if ("file".equals(uri.getScheme())) {
			path = uri.getPath();
		}

		final File file = new File(path);
		if (file.exists()) {
			FileInputStream is = new FileInputStream(file);
			FileDescriptor fd = is.getFD();
			setDataSource(fd);
			is.close();
		} else {
			_setDataSource(path, keys, values);
		}
	}

  /**
   * Set the segments source url
   * @param segments the array path of the url e.g. Segmented video list
   * @param cacheDir e.g. getCacheDir().toString()
   */
  public void setDataSegments(String[] uris, String cacheDir) {
  	_setDataSegmentsSource(uris, cacheDir);
  }

  public void setOnHWRenderFailedListener(OnHWRenderFailedListener l) {
    mOnHWRenderFailedListener = l;
  }

  /**
   * Sets the data source (file-path or http/rtsp/mms URL) to use.
   *
   * @param path    the path of the file, or the http/rtsp/mms URL of the stream you
   *                want to play
   * @param keys   AVOption key
   * @param values AVOption value
   * @throws IllegalStateException if it is called in an invalid state
   */
  private native void _setDataSource(String path, String[] keys, String[] values) throws IOException, IllegalArgumentException, IllegalStateException;

  /**
   * Sets the data source (FileDescriptor) to use. It is the caller's
   * responsibility to close the file descriptor. It is safe to do so as soon as
   * this call returns.
   *
   * @param fd the FileDescriptor for the file you want to play
   * @throws IllegalStateException if it is called in an invalid state
   */
  public native void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException;
  
  /**
   * Set the segments source url
   * @param segments the array path of the url
   * @param cacheDir e.g. getCacheDir().toString()
   */
  private native void _setDataSegmentsSource(String[] segments, String cacheDir);

  /**
   * Prepares the player for playback, synchronously.
   * <p/>
   * After setting the datasource and the display surface, you need to either
   * call prepare() or prepareAsync(). For files, it is OK to call prepare(),
   * which blocks until MediaPlayer is ready for playback.
   *
   * @throws IllegalStateException if it is called in an invalid state
   */
  public native void prepare() throws IOException, IllegalStateException;

  /**
   * Prepares the player for playback, asynchronously.
   * <p/>
   * After setting the datasource and the display surface, you need to either
   * call prepare() or prepareAsync(). For streams, you should call
   * prepareAsync(), which returns immediately, rather than blocking until
   * enough data has been buffered.
   *
   * @throws IllegalStateException if it is called in an invalid state
   */
  public native void prepareAsync() throws IllegalStateException;

  /**
   * Starts or resumes playback. If playback had previously been paused,
   * playback will continue from where it was paused. If playback had been
   * stopped, or never started before, playback will start at the beginning.
   *
   * @throws IllegalStateException if it is called in an invalid state
   */
  public void start() throws IllegalStateException {
    stayAwake(true);
    _start();
  }

  private native void _start() throws IllegalStateException;

  /**
   * The same as {@link #pause()}
   *
   * @throws IllegalStateException if the internal player engine has not been initialized.
   */
  public void stop() throws IllegalStateException {
    stayAwake(false);
    _stop();
  }

  private native void _stop() throws IllegalStateException;

  /**
   * Pauses playback. Call start() to resume.
   *
   * @throws IllegalStateException if the internal player engine has not been initialized.
   */
  public void pause() throws IllegalStateException {
    stayAwake(false);
    _pause();
  }

  private native void _pause() throws IllegalStateException;

  /**
   * Set the low-level power management behavior for this MediaPlayer. This can
   * be used when the MediaPlayer is not playing through a SurfaceHolder set
   * with {@link #setDisplay(SurfaceHolder)} and thus can use the high-level
   * {@link #setScreenOnWhilePlaying(boolean)} feature.
   * <p/>
   * This function has the MediaPlayer access the low-level power manager
   * service to control the device's power usage while playing is occurring. The
   * parameter is a combination of {@link android.os.PowerManager} wake flags.
   * Use of this method requires {@link android.Manifest.permission#WAKE_LOCK}
   * permission. By default, no attempt is made to keep the device awake during
   * playback.
   *
   * @param context the Context to use
   * @param mode    the power/wake mode to set
   * @see android.os.PowerManager
   */
  @SuppressLint("Wakelock")
  public void setWakeMode(Context context, int mode) {
    boolean washeld = false;
    if (mWakeLock != null) {
      if (mWakeLock.isHeld()) {
        washeld = true;
        mWakeLock.release();
      }
      mWakeLock = null;
    }

    PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
    mWakeLock = pm.newWakeLock(mode | PowerManager.ON_AFTER_RELEASE, MediaPlayer.class.getName());
    mWakeLock.setReferenceCounted(false);
    if (washeld) {
      mWakeLock.acquire();
    }
  }

  /**
   * Control whether we should use the attached SurfaceHolder to keep the screen
   * on while video playback is occurring. This is the preferred method over
   * {@link #setWakeMode} where possible, since it doesn't require that the
   * application have permission for low-level wake lock access.
   *
   * @param screenOn Supply true to keep the screen on, false to allow it to turn off.
   */
  public void setScreenOnWhilePlaying(boolean screenOn) {
    if (mScreenOnWhilePlaying != screenOn) {
      mScreenOnWhilePlaying = screenOn;
      updateSurfaceScreenOn();
    }
  }

  @SuppressLint("Wakelock")
  private void stayAwake(boolean awake) {
    if (mWakeLock != null) {
      if (awake && !mWakeLock.isHeld()) {
        mWakeLock.acquire();
      } else if (!awake && mWakeLock.isHeld()) {
        mWakeLock.release();
      }
    }
    mStayAwake = awake;
    updateSurfaceScreenOn();
  }

  private void updateSurfaceScreenOn() {
    if (mSurfaceHolder != null)
      mSurfaceHolder.setKeepScreenOn(mScreenOnWhilePlaying && mStayAwake);
  }

  /**
   * Returns the width of the video.
   *
   * @return the width of the video, or 0 if there is no video, or the width has
   *         not been determined yet. The OnVideoSizeChangedListener can be
   *         registered via
   *         {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
   *         to provide a notification when the width is available.
   */
  public native int getVideoWidth();

  private native int getVideoWidth_a();

  /**
   * Returns the height of the video.
   *
   * @return the height of the video, or 0 if there is no video, or the height
   *         has not been determined yet. The OnVideoSizeChangedListener can be
   *         registered via
   *         {@link #setOnVideoSizeChangedListener(OnVideoSizeChangedListener)}
   *         to provide a notification when the height is available.
   */
  public native int getVideoHeight();

  private native int getVideoHeight_a();

  /**
   * Checks whether the MediaPlayer is playing.
   *
   * @return true if currently playing, false otherwise
   */
  public native boolean isPlaying();


	/**
   * Adaptive streaming support, default is false
   *
   * @param adaptive true if wanna adaptive steam
   *
   */
  public native void setAdaptiveStream(boolean adaptive);

  /**
   * Seeks to specified time position.
   *
   * @param msec the offset in milliseconds from the start to seek to
   * @throws IllegalStateException if the internal player engine has not been initialized
   */
  public native void seekTo(long msec) throws IllegalStateException;

  /**
   * Gets the current playback position.
   *
   * @return the current position in milliseconds
   */
  public native long getCurrentPosition();

  /**
   * Get the current video frame
   *
   * @return bitmap object
   */
  public native Bitmap getCurrentFrame();

  /**
   * Gets the duration of the file.
   *
   * @return the duration in milliseconds
   */
  public native long getDuration();

  /**
   * Gets the media metadata.
   *
   * @return The metadata, possibly empty. null if an error occurred.
   */
  public Metadata getMetadata() {
    if (mMeta == null) {
      mMeta = new Metadata();
      Map<byte[], byte[]> meta = new HashMap<byte[], byte[]>();

      if (!native_getMetadata(meta)) {
        return null;
      }

      if (!mMeta.parse(meta, getMetaEncoding())) {
        return null;
      }
    }
    return mMeta;
  }

  /**
   * Releases resources associated with this MediaPlayer object. It is
   * considered good practice to call this method when you're done using the
   * MediaPlayer.
   */
  public void release() {
    stayAwake(false);
    updateSurfaceScreenOn();
    mOnPreparedListener = null;
    mOnBufferingUpdateListener = null;
    mOnCompletionListener = null;
    mOnSeekCompleteListener = null;
    mOnErrorListener = null;
    mOnInfoListener = null;
    mOnVideoSizeChangedListener = null;
    mOnCachingUpdateListener = null;
    mOnHWRenderFailedListener = null;
    _release();
    closeFD();
  }

  private native void _release();

  /**
   * Resets the MediaPlayer to its uninitialized state. After calling this
   * method, you will have to initialize it again by setting the data source and
   * calling prepare().
   */
  public void reset() {
    stayAwake(false);
    _reset();
    mEventHandler.removeCallbacksAndMessages(null);
    closeFD();
  }

  private native void _reset();

  private void closeFD() {
    if (mFD != null) {
      try {
        mFD.close();
      } catch (IOException e) {
        Log.e("closeFD", e);
      }
      mFD = null;
    }
  }

  /**
   * Amplify audio
   *
   * @param ratio  e.g. 3.5
   */
  public native void setAudioAmplify(float ratio);

  public native void setVolume(float leftVolume, float rightVolume);

  private native final boolean native_getTrackInfo(SparseArray<byte[]> trackSparse);

  private native final boolean native_getMetadata(Map<byte[], byte[]> meta);

  private native final void native_init();

  private native final void native_finalize();

  /**
   * Returns an array of track information.
   *
   * @return Array of track info. The total number of tracks is the array
   *         length. Must be called again if an external timed text source has
   *         been added after any of the addTimedTextSource methods are called.
   */
  public TrackInfo[] getTrackInfo(String encoding) {
    SparseArray<byte[]> trackSparse = new SparseArray<byte[]>();
    if (!native_getTrackInfo(trackSparse)) {
      return null;
    }

    int size = trackSparse.size();
    TrackInfo[] trackInfos = new TrackInfo[size];
    for (int i = 0; i < size; i++) {
      TrackInfo trackInfo = new TrackInfo(trackSparse.keyAt(i), parseTrackInfo(trackSparse.valueAt(i), encoding));
      trackInfos[i] = trackInfo;
    }
    return trackInfos;
  }

  /**
   * Use default chartset {@link #getTrackInfo()} method.
   *
   * @return array of {@link TrackInfo}
   */
  public TrackInfo[] getTrackInfo() {
    return getTrackInfo(Charset.defaultCharset().name());
  }

  private SparseArray<String> parseTrackInfo(byte[] tracks, String encoding) {
    SparseArray<String> trackSparse = new SparseArray<String>();
    String trackString;
    int trackNum;
    try {
      trackString = new String(tracks, encoding);
    } catch (Exception e) {
      Log.e("getTrackMap exception");
      trackString = new String(tracks);
    }
    for (String s : trackString.split("!#!")) {
      try {
        if (s.contains("."))
          trackNum = Integer.parseInt(s.split("\\.")[0]);
        else
          trackNum = Integer.parseInt(s);
        trackSparse.put(trackNum, s);
      } catch (NumberFormatException e) {
      }
    }

    return trackSparse;
  }

  /**
   * @param mediaTrackType
   * @param trackInfo
   * @return {@link TrackInfo#getTrackInfoArray()}
   */
  public SparseArray<String> findTrackFromTrackInfo(int mediaTrackType, TrackInfo[] trackInfo) {
    for (int i = 0; i < trackInfo.length; i++) {
      if (trackInfo[i].getTrackType() == mediaTrackType) {
        return trackInfo[i].getTrackInfoArray();
      }
    }
    return null;
  }

  /**
   * Set the file-path of an external timed text.
   *
   * @param path must be a local file
   */
  public native void addTimedTextSource(String path);

  /**
   * Selects a track.
   * <p>
   * In any valid state, if it is called multiple times on the same type of
   * track (ie. Video, Audio, Timed Text), the most recent one will be chosen.
   * </p>
   * <p>
   * The first audio and video tracks are selected by default if available, even
   * though this method is not called. However, no timed text track will be
   * selected until this function is called.
   * </p>
   *
   * @param index the index of the track to be selected. The valid range of the
   *              index is 0..total number of track - 1. The total number of tracks
   *              as well as the type of each individual track can be found by
   *              calling {@link #getTrackInfo()} method.
   * @see io.vov.vitamio.MediaPlayer#getTrackInfo
   */
  public void selectTrack(int index) {
    selectOrDeselectTrack(index, true /* select */);
  }

  /**
   * Deselect a track.
   * <p>
   * Currently, the track must be a timed text track and no audio or video
   * tracks can be deselected.
   * </p>
   *
   * @param index the index of the track to be deselected. The valid range of the
   *              index is 0..total number of tracks - 1. The total number of tracks
   *              as well as the type of each individual track can be found by
   *              calling {@link #getTrackInfo()} method.
   * @see io.vov.vitamio.MediaPlayer#getTrackInfo
   */
  public void deselectTrack(int index) {
    selectOrDeselectTrack(index, false /* select */);
  }

  private native void selectOrDeselectTrack(int index, boolean select);

  @Override
  protected void finalize() {
    native_finalize();
  }

  /**
   * Register a callback to be invoked when the media source is ready for
   * playback.
   *
   * @param listener the callback that will be run
   */
  public void setOnPreparedListener(OnPreparedListener listener) {
    mOnPreparedListener = listener;
  }

  /**
   * Register a callback to be invoked when the end of a media source has been
   * reached during playback.
   *
   * @param listener the callback that will be run
   */
  public void setOnCompletionListener(OnCompletionListener listener) {
    mOnCompletionListener = listener;
  }

  /**
   * Register a callback to be invoked when the status of a network stream's
   * buffer has changed.
   *
   * @param listener the callback that will be run.
   */
  public void setOnBufferingUpdateListener(OnBufferingUpdateListener listener) {
    mOnBufferingUpdateListener = listener;
  }

  /**
   * Register a callback to be invoked when the segments cached on storage has
   * changed.
   *
   * @param listener the callback that will be run.
   */
  public void setOnCachingUpdateListener(OnCachingUpdateListener listener) {
    mOnCachingUpdateListener = listener;
  }

  private void updateCacheStatus(int type, int info, long[] segments) {
    if (mEventHandler != null) {
      Message m = mEventHandler.obtainMessage(MEDIA_CACHING_UPDATE);
      Bundle b = m.getData();
      b.putInt(MEDIA_CACHING_TYPE, type);
      b.putInt(MEDIA_CACHING_INFO, info);
      b.putLongArray(MEDIA_CACHING_SEGMENTS, segments);
      mEventHandler.sendMessage(m);
    }
  }

  /**
   * Register a callback to be invoked when a seek operation has been completed.
   *
   * @param listener the callback that will be run
   */
  public void setOnSeekCompleteListener(OnSeekCompleteListener listener) {
    mOnSeekCompleteListener = listener;
  }

  /**
   * Register a callback to be invoked when the video size is known or updated.
   *
   * @param listener the callback that will be run
   */
  public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener listener) {
    mOnVideoSizeChangedListener = listener;
  }

  /**
   * Register a callback to be invoked when an error has happened during an
   * asynchronous operation.
   *
   * @param listener the callback that will be run
   */
  public void setOnErrorListener(OnErrorListener listener) {
    mOnErrorListener = listener;
  }

  public void setOnInfoListener(OnInfoListener listener) {
    mOnInfoListener = listener;
  }

  /**
   * Register a callback to be invoked when a timed text need to display.
   *
   * @param listener the callback that will be run
   */
  public void setOnTimedTextListener(OnTimedTextListener listener) {
    mOnTimedTextListener = listener;
  }

  private void updateSub(int subType, byte[] bytes, String encoding, int width, int height) {
    if (mEventHandler != null) {
      Message m = mEventHandler.obtainMessage(MEDIA_TIMED_TEXT, width, height);
      Bundle b = m.getData();
      if (subType == SUBTITLE_TEXT) {
        b.putInt(MEDIA_SUBTITLE_TYPE, SUBTITLE_TEXT);
        if (encoding == null) {
          b.putString(MEDIA_SUBTITLE_STRING, new String(bytes));
        } else {
          try {
            b.putString(MEDIA_SUBTITLE_STRING, new String(bytes, encoding.trim()));
          } catch (UnsupportedEncodingException e) {
            Log.e("updateSub", e);
            b.putString(MEDIA_SUBTITLE_STRING, new String(bytes));
          }
        }
      } else if (subType == SUBTITLE_BITMAP) {
        b.putInt(MEDIA_SUBTITLE_TYPE, SUBTITLE_BITMAP);
        b.putByteArray(MEDIA_SUBTITLE_BYTES, bytes);
      }
      mEventHandler.sendMessage(m);
    }
  }

  protected native void _releaseVideoSurface();

  /**
   * Calling this result in only the audio track being played.
   */
  public void releaseDisplay() {
    _releaseVideoSurface();
    mSurfaceHolder = null;
    mSurface = null;
  }

  /**
   * Returns the aspect ratio of the video.
   *
   * @return the aspect ratio of the video, or 0 if there is no video, or the
   *         width and height is not available.
	 *         @see io.vov.vitamio.widget.VideoView#setVideoLayout(int, float)
   */
  public native float getVideoAspectRatio();

  /**
   * Set the quality when play video, if the video is too lag, you may try
   * VIDEOQUALITY_LOW, default is VIDEOQUALITY_LOW.
   *
   * @param quality <ul>
   *                <li>{@link #VIDEOQUALITY_HIGH}
   *                <li>{@link #VIDEOQUALITY_MEDIUM}
   *                <li>{@link #VIDEOQUALITY_LOW}
   *                </ul>
   */
  public native void setVideoQuality(int quality);

  /**
   * Set the Video Chroma quality when play video, default is VIDEOCHROMA_RGB565
   * You can set on after {@link #prepareAsync()}.
   * @param chroma <ul>
   *                <li>{@link #VIDEOCHROMA_RGB565}
   *                <li>{@link #VIDEOCHROMA_RGBA}
   *                </ul>
   */
  public native void setVideoChroma(int chroma);

  /**
   * Set if should deinterlace the video picture
   *
   * @param deinterlace
   */
  public native void setDeinterlace(boolean deinterlace);

  /**
   * The buffer to fill before playback, default is 1024KB
   *
   * @param bufSize buffer size in Byte
   */
  public native void setBufferSize(long bufSize);

  /**
   * Set video and audio playback speed
   *
   * @param speed e.g. 0.8 or 2.0, default to 1.0, range in [0.5-2]
   */
  public native void setPlaybackSpeed(float speed);

  /**
   * Checks whether the buffer is filled
   *
   * @return false if buffer is filled
   */
  public native boolean isBuffering();

  /**
   * @return the percent
   * @see io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener
   */
  public native int getBufferProgress();

  /**
   * Get the encoding if haven't set with {@link #setMetaEncoding(String)}
   *
   * @return the encoding
   */
  public native String getMetaEncoding();

  /**
   * Set the encoding MediaPlayer will use to determine the metadata
   *
   * @param encoding e.g. "UTF-8"
   */
  public native void setMetaEncoding(String encoding);

  /**
   * Get the audio track number in playback
   *
   * @return track number
   */
	public native int getAudioTrack();

	/**
   * Get the video track number in playback
	 *
	 * @return track number
	 */
	public native int getVideoTrack();

  /**
   * Tell the MediaPlayer whether to show timed text
   *
   * @param shown true if wanna show
   */
  public native void setTimedTextShown(boolean shown);

  /**
   * Set the encoding to display timed text.
   *
   * @param encoding MediaPlayer will detet it if null
   */
  public native void setTimedTextEncoding(String encoding);

  /**
   * @return <ul>
   *         <li>{@link #SUBTITLE_EXTERNAL}
   *         <li>{@link #SUBTITLE_INTERNAL}
   *         </ul>
   */
  public native int getTimedTextLocation();

  /**
   * You can get the file-path of the external subtitle in use.
   *
   * @return null if no external subtitle
   */
  public native String getTimedTextPath();

  /**
   * Get the subtitle track number in playback
   *
   * @return track number
   */
  public native int getTimedTextTrack();

  private int audioTrackInit(int sampleRateInHz, int channels) {
    audioTrackRelease();
    int channelConfig = channels >= 2 ? AudioFormat.CHANNEL_OUT_STEREO : AudioFormat.CHANNEL_OUT_MONO;
    try {
      mAudioTrackBufferSize = AudioTrack.getMinBufferSize(sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT);
      mAudioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, sampleRateInHz, channelConfig, AudioFormat.ENCODING_PCM_16BIT, mAudioTrackBufferSize, AudioTrack.MODE_STREAM);
    } catch (Exception e) {
      mAudioTrackBufferSize = 0;
      Log.e("audioTrackInit", e);
    }
    return mAudioTrackBufferSize;
  }

  private void audioTrackSetVolume(float leftVolume, float rightVolume) {
    if (mAudioTrack != null)
      mAudioTrack.setStereoVolume(leftVolume, rightVolume);
  }

  private void audioTrackWrite(byte[] audioData, int offsetInBytes, int sizeInBytes) {
    if (mAudioTrack != null) {
      audioTrackStart();
      int written;
      while (sizeInBytes > 0) {
        written = sizeInBytes > mAudioTrackBufferSize ? mAudioTrackBufferSize : sizeInBytes;
        mAudioTrack.write(audioData, offsetInBytes, written);
        sizeInBytes -= written;
        offsetInBytes += written;
      }
    }
  }

  private void audioTrackStart() {
    if (mAudioTrack != null && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED && mAudioTrack.getPlayState() != AudioTrack.PLAYSTATE_PLAYING)
      mAudioTrack.play();
  }

  private void audioTrackPause() {
    if (mAudioTrack != null && mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED)
      mAudioTrack.pause();
  }

  private void audioTrackRelease() {
    if (mAudioTrack != null) {
      if (mAudioTrack.getState() == AudioTrack.STATE_INITIALIZED)
        mAudioTrack.stop();
      mAudioTrack.release();
    }
    mAudioTrack = null;
  }

  private ByteBuffer surfaceInit() {
    synchronized (this) {
      mLocalSurface = mSurface;
      int w = getVideoWidth_a();
      int h = getVideoHeight_a();
      if (mLocalSurface != null && w != 0 && h != 0) {
        mBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
        mByteBuffer = ByteBuffer.allocateDirect(w * h * 2);
      } else {
        mBitmap = null;
        mByteBuffer = null;
      }
      return mByteBuffer;
    }
  }

  private void surfaceRender() {
    synchronized (this) {
      if (mLocalSurface == null || !mLocalSurface.isValid() || mBitmap == null || mByteBuffer == null)
        return;

      try {
        Canvas c = mLocalSurface.lockCanvas(null);
        mBitmap.copyPixelsFromBuffer(mByteBuffer);
        c.drawBitmap(mBitmap, 0, 0, null);
        mLocalSurface.unlockCanvasAndPost(c);
      } catch (Exception e) {
        Log.e("surfaceRender", e);
      }
    }
  }

  private void surfaceRelease() {
    synchronized (this) {
      mLocalSurface = null;
      mBitmap = null;
      mByteBuffer = null;
    }
  }

  public interface OnHWRenderFailedListener {
    public void onFailed();
  }

  public interface OnPreparedListener {
    /**
     * Called when the media file is ready for playback.
     *
     * @param mp the MediaPlayer that is ready for playback
     */
    void onPrepared(MediaPlayer mp);
  }

  public interface OnCompletionListener {
    /**
     * Called when the end of a media source is reached during playback.
     *
     * @param mp the MediaPlayer that reached the end of the file
     */
    void onCompletion(MediaPlayer mp);
  }

  public interface OnBufferingUpdateListener {
    /**
     * Called to update status in buffering a media stream. Buffering is storing
     * data in memory while caching on external storage.
     *
     * @param mp      the MediaPlayer the update pertains to
     * @param percent the percentage (0-100) of the buffer that has been filled thus
     *                far
     */
    void onBufferingUpdate(MediaPlayer mp, int percent);
  }

  public interface OnCachingUpdateListener {
    /**
     * Called to update status in caching a media stream. Caching is storing
     * data on external storage while buffering in memory.
     *
     * @param mp       the MediaPlayer the update pertains to
     * @param segments the cached segments in bytes, in format [s1begin, s1end,
     *                 s2begin, s2end], s1begin < s1end < s2begin < s2end. e.g. [124,
     *                 100423, 4321412, 214323433]
     */
    void onCachingUpdate(MediaPlayer mp, long[] segments);

    /**
     * Cache speed
     *
     * @param mp    the MediaPlayer the update pertains to
     * @param speed the cached speed size kb/s
     */
    void onCachingSpeed(MediaPlayer mp, int speed);

    /**
     * Cache not available
     *
     * @param mp   the MediaPlayer the update pertains to
     * @param info the not available info
     *             <ul>
     *             <li>{@link #CACHE_INFO_NO_SPACE}
     *             <li>{@link #CACHE_INFO_STREAM_NOT_SUPPORT}
     *             </ul>
     */
    void onCachingNotAvailable(MediaPlayer mp, int info);
  }

  public interface OnSeekCompleteListener {
    /**
     * Called to indicate the completion of a seek operation.
     *
     * @param mp the MediaPlayer that issued the seek operation
     */
    public void onSeekComplete(MediaPlayer mp);
  }

  public interface OnVideoSizeChangedListener {
    /**
     * Called to indicate the video size
     *
     * @param mp     the MediaPlayer associated with this callback
     * @param width  the width of the video
     * @param height the height of the video
     */
    public void onVideoSizeChanged(MediaPlayer mp, int width, int height);
  }

  public interface OnErrorListener {
    /**
     * Called to indicate an error.
     *
     * @param mp    the MediaPlayer the error pertains to
     * @param what  the type of error that has occurred:
     *              <ul>
     *              <li>{@link #MEDIA_ERROR_UNKNOWN}
     *              <li>
     *              {@link #MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK}
     *              </ul>
     * @param extra an extra code, specific to the error. Typically implementation
     *              dependant.
     * @return True if the method handled the error, false if it didn't.
     *         Returning false, or not having an OnErrorListener at all, will
     *         cause the OnCompletionListener to be called.
     */
    boolean onError(MediaPlayer mp, int what, int extra);
  }

  public interface OnInfoListener {
    /**
     * Called to indicate an info or a warning.
     *
     * @param mp    the MediaPlayer the info pertains to.
     * @param what  the type of info or warning.
     *              <ul>
     *              <li>{@link #MEDIA_INFO_VIDEO_TRACK_LAGGING}
     *              <li>{@link #MEDIA_INFO_BUFFERING_START}
     *              <li>{@link #MEDIA_INFO_BUFFERING_END}
     *              <li>{@link #MEDIA_INFO_NOT_SEEKABLE}
     *              <li>{@link #MEDIA_INFO_DOWNLOAD_RATE_CHANGED}
     *              </ul>
     * @param extra an extra code, specific to the info. Typically implementation
     *              dependant.
     * @return True if the method handled the info, false if it didn't.
     *         Returning false, or not having an OnErrorListener at all, will
     *         cause the info to be discarded.
     */
    boolean onInfo(MediaPlayer mp, int what, int extra);
  }

  public interface OnTimedTextListener {
    /**
     * Called to indicate that a text timed text need to display
     *
     * @param text the timedText to display
     */
    public void onTimedText(String text);

    /**
     * Called to indicate that an image timed text need to display
     *
     * @param pixels the pixels of the timed text image
     * @param width  the width of the timed text image
     * @param height the height of the timed text image
     */
    public void onTimedTextUpdate(byte[] pixels, int width, int height);
  }

  /**
   * Class for MediaPlayer to return each audio/video/subtitle track's metadata.
   *
   * @see io.vov.vitamio.MediaPlayer#getTrackInfo
   */
  static public class TrackInfo {
    public static final int MEDIA_TRACK_TYPE_UNKNOWN = 0;
    public static final int MEDIA_TRACK_TYPE_VIDEO = 1;
    public static final int MEDIA_TRACK_TYPE_AUDIO = 2;
    public static final int MEDIA_TRACK_TYPE_TIMEDTEXT = 3;
    final int mTrackType;
    final SparseArray<String> mTrackInfoArray;

    TrackInfo(int trackType, SparseArray<String> trackInfoArray) {
      mTrackType = trackType;
      mTrackInfoArray = trackInfoArray;
    }

    /**
     * Gets the track type.
     *
     * @return TrackType which indicates if the track is video, audio, timed
     *         text.
     */
    public int getTrackType() {
      return mTrackType;
    }

    /**
     * Gets the track info
     *
     * @return map trackIndex to string (e.g. "English", 3).
     */
    public SparseArray<String> getTrackInfoArray() {
      return mTrackInfoArray;
    }
  }

  @SuppressLint("HandlerLeak")
  private class EventHandler extends Handler {
    private MediaPlayer mMediaPlayer;
    private Bundle mData;

    public EventHandler(MediaPlayer mp, Looper looper) {
      super(looper);
      mMediaPlayer = mp;
    }

    @Override
    public void handleMessage(Message msg) {
      switch (msg.what) {
        case MEDIA_PREPARED:
          if (mOnPreparedListener != null)
            mOnPreparedListener.onPrepared(mMediaPlayer);
          return;
        case MEDIA_PLAYBACK_COMPLETE:
          if (mOnCompletionListener != null)
            mOnCompletionListener.onCompletion(mMediaPlayer);
          stayAwake(false);
          return;
        case MEDIA_BUFFERING_UPDATE:
          if (mOnBufferingUpdateListener != null)
            mOnBufferingUpdateListener.onBufferingUpdate(mMediaPlayer, msg.arg1);
          return;
        case MEDIA_SEEK_COMPLETE:
          if (isPlaying())
            stayAwake(true);
          if (mOnSeekCompleteListener != null)
            mOnSeekCompleteListener.onSeekComplete(mMediaPlayer);
          return;
        case MEDIA_SET_VIDEO_SIZE:
          if (mOnVideoSizeChangedListener != null)
            mOnVideoSizeChangedListener.onVideoSizeChanged(mMediaPlayer, msg.arg1, msg.arg2);
          return;
        case MEDIA_ERROR:
          Log.e("Error (%d, %d)", msg.arg1, msg.arg2);
          boolean error_was_handled = false;
          if (mOnErrorListener != null)
            error_was_handled = mOnErrorListener.onError(mMediaPlayer, msg.arg1, msg.arg2);
          if (mOnCompletionListener != null && !error_was_handled)
            mOnCompletionListener.onCompletion(mMediaPlayer);
          stayAwake(false);
          return;
        case MEDIA_INFO:
          Log.i("Info (%d, %d)", msg.arg1, msg.arg2);
          if (mOnInfoListener != null)
            mOnInfoListener.onInfo(mMediaPlayer, msg.arg1, msg.arg2);
          return;
        case MEDIA_CACHE:
          return;
        case MEDIA_TIMED_TEXT:
          mData = msg.getData();
          if (mData.getInt(MEDIA_SUBTITLE_TYPE) == SUBTITLE_TEXT) {
            Log.i("Subtitle : %s", mData.getString(MEDIA_SUBTITLE_STRING));
            if (mOnTimedTextListener != null)
              mOnTimedTextListener.onTimedText(mData.getString(MEDIA_SUBTITLE_STRING));
          } else if (mData.getInt(MEDIA_SUBTITLE_TYPE) == SUBTITLE_BITMAP) {
            Log.i("Subtitle : bitmap");
            if (mOnTimedTextListener != null)
              mOnTimedTextListener.onTimedTextUpdate(mData.getByteArray(MEDIA_SUBTITLE_BYTES), msg.arg1, msg.arg2);
          }
          return;
        case MEDIA_CACHING_UPDATE:
          if (mOnCachingUpdateListener != null) {
            int cacheType = msg.getData().getInt(MEDIA_CACHING_TYPE);
            if (cacheType == CACHE_TYPE_NOT_AVAILABLE) {
              mOnCachingUpdateListener.onCachingNotAvailable(mMediaPlayer, msg.getData().getInt(MEDIA_CACHING_INFO));
            } else if (cacheType == CACHE_TYPE_UPDATE) {
              mOnCachingUpdateListener.onCachingUpdate(mMediaPlayer, msg.getData().getLongArray(MEDIA_CACHING_SEGMENTS));
            } else if (cacheType == CACHE_TYPE_SPEED) {
              mOnCachingUpdateListener.onCachingSpeed(mMediaPlayer, msg.getData().getInt(MEDIA_CACHING_INFO));
            }
          }
          return;
        case MEDIA_NOP:
          return;
        case MEDIA_HW_ERROR:
        	if (mOnHWRenderFailedListener != null)
        		mOnHWRenderFailedListener.onFailed();
        	return;
        default:
          Log.e("Unknown message type " + msg.what);
          return;
      }
    }
  }
}
