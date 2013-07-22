/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;

import io.vov.vitamio.utils.FileUtils;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * MediaMetadataRetriever is used to get meta data from any media file
 * <p/>
 * <pre>
 * MediaMetadataRetriever mmr = new MediaMetadataRetriever(this);
 * mmr.setDataSource(this, mediaUri);
 * String title = mmr.extractMetadata(METADATA_KEY_TITLE);
 * Bitmap frame = mmr.getFrameAtTime(-1);
 * </pre>
 */
public class MediaMetadataRetriever {
  private Context mContext;
  private AssetFileDescriptor mFD = null;

  public MediaMetadataRetriever(Context ctx) {
    mContext = ctx;
    native_init();
  }

  private static native boolean loadFFmpeg_native(String ffmpegPath);

  public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException, SecurityException, IllegalStateException {
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
    Log.e("Couldn't open file on client side, trying server side %s", uri.toString());
    setDataSource(uri.toString());
    return;
  }

  public native void setDataSource(String path) throws IOException, IllegalArgumentException, IllegalStateException;

  public native void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException, IllegalStateException;
  
  /**
   * Call this method after setDataSource(). This method retrieves the 
   * meta data value associated with the keyCode.
   * 
   * The keyCode currently supported is listed below as METADATA_XXX
   * constants. With any other value, it returns a null pointer.
   * 
   * @param keyCode One of the constants listed below at the end of the class.
   * @return The meta data value associate with the given keyCode on success; 
   * null on failure.
   */
  public native String extractMetadata(String keyCode) throws IllegalStateException;

  public native Bitmap getFrameAtTime(long timeUs) throws IllegalStateException;

  private native void _release();

  public void release() {
    _release();
    closeFD();
  }

  static {
    String LIB_ROOT = Vitamio.getLibraryPath();
    Log.i("LIB ROOT: %s", LIB_ROOT);
    System.load(LIB_ROOT + "libstlport_shared.so");
    System.load(LIB_ROOT + "libvscanner.so");
    loadFFmpeg_native(LIB_ROOT + "libffmpeg.so");
  }

  private native final void native_init();

  private native final void native_finalize();

  @Override
  protected void finalize() throws Throwable {
    try {
      native_finalize();
    } finally {
      super.finalize();
    }
  }

  private void closeFD() {
    if (mFD != null) {
      try {
        mFD.close();
      } catch (IOException e) {
      }
      mFD = null;
    }
  }
  
  /*
   * Do not change these metadata key values without updating their
   * counterparts in c file
   */
  
  /**
   * The metadata key to retrieve the information about the album title
   * of the data source.
   */
  public static final String METADATA_KEY_ALBUM           = "album";
  /**
   * The metadata key to retrieve the information about the artist of
   * the data source.
   */
  public static final String METADATA_KEY_ARTIST          = "artist";
  /**
   * The metadata key to retrieve the information about the author of
   * the data source.
   */
  public static final String METADATA_KEY_AUTHOR          = "author";
  /**
   * The metadata key to retrieve the information about the composer of
   * the data source.
   */
  public static final String METADATA_KEY_COMPOSER        = "composer";
  /**
   * The metadata key to retrieve the content type or genre of the data
   * source.
   */
  public static final String METADATA_KEY_GENRE           = "genre";
  /**
   * The metadata key to retrieve the data source title.
   */
  public static final String METADATA_KEY_TITLE           = "title";
  /**
   * The metadata key to retrieve the playback duration of the data source.
   */
  public static final String METADATA_KEY_DURATION        = "duration";
  /**
   * If the media contains video, this key retrieves its width.
   */
  public static final String METADATA_KEY_VIDEO_WIDTH     = "width";
  /**
   * If the media contains video, this key retrieves its height.
   */
  public static final String METADATA_KEY_VIDEO_HEIGHT    = "height";
  
}