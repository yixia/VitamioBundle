/*
 * Copyright (C) 2013 yixia.com
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

package io.vov.vitamio.demo;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.widget.Toast;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnBufferingUpdateListener;
import io.vov.vitamio.MediaPlayer.OnCompletionListener;
import io.vov.vitamio.MediaPlayer.OnPreparedListener;

@SuppressLint("NewApi")
public class MediaPlayerDemo_setSurface extends Activity implements OnBufferingUpdateListener,
    OnCompletionListener, OnPreparedListener, TextureView.SurfaceTextureListener {

  private static final String TAG = "MediaPlayerDemo";
  private int mVideoWidth;
  private int mVideoHeight;
  private MediaPlayer mMediaPlayer;
  private TextureView mTextureView;
  private String path;
  private Surface surf;
  
  private boolean mIsVideoSizeKnown = false;
  private boolean mIsVideoReadyToBePlayed = false;

  /**
   * 
   * Called when the activity is first created.
   */
  @Override
  public void onCreate(Bundle icicle) {
    super.onCreate(icicle);
    if (!LibsChecker.checkVitamioLibs(this))
      return;
    setContentView(R.layout.mediaplayer_3);
    mTextureView = (TextureView) findViewById(R.id.surface);
    mTextureView.setSurfaceTextureListener(this);

  }

  @SuppressLint("NewApi")
  private void playVideo(SurfaceTexture surfaceTexture) {
    doCleanUp();
    try {

      path = "";
      if (path == "") {
        // Tell the user to provide a media file URL.
        Toast.makeText(
            MediaPlayerDemo_setSurface.this,
            "Please edit MediaPlayerDemo_setSurface Activity, "
                + "and set the path variable to your media file path."
                + " Your media file must be stored on sdcard.", Toast.LENGTH_LONG).show();
        return;
      }
      // Create a new media player and set the listeners
      mMediaPlayer = new MediaPlayer(this, true);
      mMediaPlayer.setDataSource(path);
      if (surf == null) {
          surf = new Surface (surfaceTexture);
      }
      mMediaPlayer.setSurface(surf);
      mMediaPlayer.prepareAsync();
      mMediaPlayer.setOnBufferingUpdateListener(this);
      mMediaPlayer.setOnCompletionListener(this);
      mMediaPlayer.setOnPreparedListener(this);
      setVolumeControlStream(AudioManager.STREAM_MUSIC);

    } catch (Exception e) {
      Log.e(TAG, "error: " + e.getMessage(), e);
    }
  }

  public void onBufferingUpdate(MediaPlayer arg0, int percent) {
    // Log.d(TAG, "onBufferingUpdate percent:" + percent);

  }

  public void onCompletion(MediaPlayer arg0) {
    Log.d(TAG, "onCompletion called");
  }

  public void onPrepared(MediaPlayer mediaplayer) {
    Log.d(TAG, "onPrepared called");
    mIsVideoReadyToBePlayed = true;
    if (mIsVideoReadyToBePlayed) {
      startVideoPlayback();
    }
  }

  @Override
  protected void onPause() {
    super.onPause();
    releaseMediaPlayer();
    doCleanUp();
  }

  @Override
  protected void onDestroy() {
    super.onDestroy();
    releaseMediaPlayer();
    doCleanUp();
  }

  private void releaseMediaPlayer() {
    if (mMediaPlayer != null) {
      mMediaPlayer.release();
      mMediaPlayer = null;
    }
  }

  private void doCleanUp() {
    mVideoWidth = 0;
    mVideoHeight = 0;
    mIsVideoReadyToBePlayed = false;
    mIsVideoSizeKnown = false;
  }

  private void startVideoPlayback() {
    Log.v(TAG, "startVideoPlayback");
    adjustAspectRatio(mMediaPlayer.getVideoWidth(), mMediaPlayer.getVideoHeight());
    mMediaPlayer.start();
  }

  /**
   * Sets the TextureView transform to preserve the aspect ratio of the video.
   */
  private void adjustAspectRatio(int videoWidth, int videoHeight) {
    int viewWidth = mTextureView.getWidth();
    int viewHeight = mTextureView.getHeight();
    double aspectRatio = (double) videoHeight / videoWidth;

    int newWidth, newHeight;
    if (viewHeight > (int) (viewWidth * aspectRatio)) {
      // limited by narrow width; restrict height
      newWidth = viewWidth;
      newHeight = (int) (viewWidth * aspectRatio);
    } else {
      // limited by short height; restrict width
      newWidth = (int) (viewHeight / aspectRatio);
      newHeight = viewHeight;
    }
    int xoff = (viewWidth - newWidth) / 2;
    int yoff = (viewHeight - newHeight) / 2;
    Log.v(TAG, "video=" + videoWidth + "x" + videoHeight + " view=" + viewWidth + "x" + viewHeight
        + " newView=" + newWidth + "x" + newHeight + " off=" + xoff + "," + yoff);

    Matrix txform = new Matrix();
    mTextureView.getTransform(txform);
    txform.setScale((float) newWidth / viewWidth, (float) newHeight / viewHeight);
    //txform.postRotate(10);          // just for fun
    txform.postTranslate(xoff, yoff);
    mTextureView.setTransform(txform);
  }

  @Override
  public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
    playVideo(surface);
  }

  @Override
  public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

  }

  @Override
  public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    return false;
  }

  @Override
  public void onSurfaceTextureUpdated(SurfaceTexture surface) {

  }

}
