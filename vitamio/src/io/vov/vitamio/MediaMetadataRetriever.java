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
 * 
 * <pre>
 * MediaMetadataRetriever mmr = new MediaMetadataRetriever(this);
 * mmr.setDataSource(this, mediaUri);
 * String title = mmr.extractMetadata(METADATA_KEY_TITLE);
 * Bitmap frame = mmr.getFrameAtTime(-1);
 * mmr.release();
 * </pre>
 */
public class MediaMetadataRetriever {
  private AssetFileDescriptor mFD = null;

  static {
    String LIB_ROOT = Vitamio.getLibraryPath();
    Log.i("LIB ROOT: %s", LIB_ROOT);
    System.load(LIB_ROOT + "libstlport_shared.so");
    System.load(LIB_ROOT + "libvscanner.so");
    loadFFmpeg_native(LIB_ROOT + "libffmpeg.so");
    native_init();
  }

  private int mNativeContext;

  public MediaMetadataRetriever(Context ctx) {
    native_setup();
  }

  private static native boolean loadFFmpeg_native(String ffmpegPath);

  public void setDataSource(Context context, Uri uri) throws IOException, IllegalArgumentException,
      SecurityException, IllegalStateException {
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

  public native void setDataSource(String path) throws IOException, IllegalArgumentException,
      IllegalStateException;

  public native void setDataSource(FileDescriptor fd) throws IOException, IllegalArgumentException,
      IllegalStateException;

  /**
   * Call this method after setDataSource(). This method retrieves the meta data
   * value associated with the keyCode.
   * 
   * The keyCode currently supported is listed below as METADATA_XXX constants.
   * With any other value, it returns a null pointer.
   * 
   * @param keyCode One of the constants listed below at the end of the class.
   * @return The meta data value associate with the given keyCode on success;
   * null on failure.
   */
  public native String extractMetadata(String keyCode) throws IllegalStateException;

  public native Bitmap getFrameAtTime(long timeUs) throws IllegalStateException;

  /**
   * Call this method after setDataSource(). This method finds the optional
   * graphic or album/cover art associated associated with the data source. If
   * there are more than one pictures, (any) one of them is returned.
   * 
   * @return null if no such graphic is found.
   */
  public native byte[] getEmbeddedPicture() throws IllegalStateException;

  private native void _release();

  private native void native_setup();

  private static native final void native_init();

  private native final void native_finalize();

  public void release() {
    _release();
    closeFD();
  }

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
   * The metadata key to retrieve the information about the album title of the
   * data source.
   */
  public static final String METADATA_KEY_ALBUM = "album";
  /**
   * The metadata key to retrieve the main creator of the set/album, if
   * different from artist. e.g. "Various Artists" for compilation albums.
   */
  public static final String METADATA_KEY_ALBUM_ARTIST = "album_artist";
  /**
   * The metadata key to retrieve the information about the artist of the data
   * source.
   */
  public static final String METADATA_KEY_ARTIST = "artist";

  /**
   * The metadata key to retrieve the any additional description of the file.
   */
  public static final String METADATA_KEY_COMMENT = "comment";
  /**
   * The metadata key to retrieve the information about the author of the data
   * source.
   */
  public static final String METADATA_KEY_AUTHOR = "author";
  /**
   * The metadata key to retrieve the information about the composer of the data
   * source.
   */
  public static final String METADATA_KEY_COMPOSER = "composer";
  /**
   * The metadata key to retrieve the name of copyright holder.
   */
  public static final String METADATA_KEY_COPYRIGHT = "copyright";
  /**
   * The metadata key to retrieve the date when the file was created, preferably
   * in ISO 8601.
   */
  public static final String METADATA_KEY_CREATION_TIME = "creation_time";
  /**
   * The metadata key to retrieve the date when the work was created, preferably
   * in ISO 8601.
   */
  public static final String METADATA_KEY_DATE = "date";
  /**
   * The metadata key to retrieve the number of a subset, e.g. disc in a
   * multi-disc collection.
   */
  public static final String METADATA_KEY_DISC = "disc";
  /**
   * The metadata key to retrieve the name/settings of the software/hardware
   * that produced the file.
   */
  public static final String METADATA_KEY_ENCODER = "encoder";
  /**
   * The metadata key to retrieve the person/group who created the file.
   */
  public static final String METADATA_KEY_ENCODED_BY = "encoded_by";
  /**
   * The metadata key to retrieve the original name of the file.
   */
  public static final String METADATA_KEY_FILENAME = "filename";
  /**
   * The metadata key to retrieve the content type or genre of the data source.
   */
  public static final String METADATA_KEY_GENRE = "genre";
  /**
   * The metadata key to retrieve the main language in which the work is
   * performed, preferably in ISO 639-2 format. Multiple languages can be
   * specified by separating them with commas.
   */
  public static final String METADATA_KEY_LANGUAGE = "language";
  /**
   * The metadata key to retrieve the artist who performed the work, if
   * different from artist. E.g for "Also sprach Zarathustra", artist would be
   * "Richard Strauss" and performer "London Philharmonic Orchestra".
   */
  public static final String METADATA_KEY_PERFORMER = "performer";
  /**
   * The metadata key to retrieve the name of the label/publisher.
   */
  public static final String METADATA_KEY_PUBLISHER = "publisher";
  /**
   * The metadata key to retrieve the name of the service in broadcasting
   * (channel name).
   */
  public static final String METADATA_KEY_SERVICE_NAME = "service_name";
  /**
   * The metadata key to retrieve the name of the service provider in
   * broadcasting.
   */
  public static final String METADATA_KEY_SERVICE_PROVIDER = "service_provider";
  /**
   * The metadata key to retrieve the data source title.
   */
  public static final String METADATA_KEY_TITLE = "title";
  /**
   * The metadata key to retrieve the number of this work in the set, can be in
   * form current/total.
   */
  public static final String METADATA_KEY_TRACK = "track";
  /**
   * The metadata key to retrieve the total bitrate of the bitrate variant that
   * the current stream is part of.
   */
  public static final String METADATA_KEY_VARIANT_BITRATE = "bitrate";
  /**
   * The metadata key to retrieve the playback duration of the data source.
   */
  public static final String METADATA_KEY_DURATION = "duration";
  /**
   * The metadata key to retrieve the audio codec of the work.
   */
  public static final String METADATA_KEY_AUDIO_CODEC = "audio_codec";
  /**
   * The metadata key to retrieve the video codec of the work.
   */
  public static final String METADATA_KEY_VIDEO_CODEC = "video_codec";
  /**
   * This key retrieves the video rotation angle in degrees, if available. The
   * video rotation angle may be 0, 90, 180, or 270 degrees.
   */
  public static final String METADATA_KEY_VIDEO_ROTATION = "rotate";
  /**
   * If the media contains video, this key retrieves its width.
   */
  public static final String METADATA_KEY_VIDEO_WIDTH = "width";
  /**
   * If the media contains video, this key retrieves its height.
   */
  public static final String METADATA_KEY_VIDEO_HEIGHT = "height";
  /**
   * The metadata key to retrieve the number of tracks, such as audio, video,
   * text, in the data source, such as a mp4 or 3gpp file.
   */
  public static final String METADATA_KEY_NUM_TRACKS = "num_tracks";
  /**
   * If this key exists the media contains audio content. if has audio, return
   * 1.
   */
  public static final String METADATA_KEY_HAS_AUDIO = "has_audio";
  /**
   * If this key exists the media contains video content. if has video, return
   * 1.
   */
  public static final String METADATA_KEY_HAS_VIDEO = "has_video";

}