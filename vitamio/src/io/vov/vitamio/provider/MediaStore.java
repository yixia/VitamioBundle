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

package io.vov.vitamio.provider;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.BaseColumns;

import io.vov.vitamio.utils.Log;

import java.io.FileNotFoundException;
import java.io.IOException;

public final class MediaStore {
  public static final String AUTHORITY = "me.abitno.vplayer.mediaprovider";
  public static final String MEDIA_SCANNER_VOLUME = "volume";
  public static final String CONTENT_AUTHORITY_SLASH = "content://" + AUTHORITY + "/";
  public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY_SLASH);
  private static final String BASE_SQL_FIELDS = MediaColumns._ID + " INTEGER PRIMARY KEY," + //
      MediaColumns.DATA + " TEXT NOT NULL," + //
      MediaColumns.DIRECTORY + " TEXT NOT NULL," + //
      MediaColumns.DIRECTORY_NAME + " TEXT NOT NULL," + //
      MediaColumns.SIZE + " INTEGER," + //
      MediaColumns.DISPLAY_NAME + " TEXT," + //
      MediaColumns.TITLE + " TEXT," + //
      MediaColumns.TITLE_KEY + " TEXT," + //
      MediaColumns.DATE_ADDED + " INTEGER," + //
      MediaColumns.DATE_MODIFIED + " INTEGER," + //
      MediaColumns.MIME_TYPE + " TEXT," + //
      MediaColumns.AVAILABLE_SIZE + " INTEGER default 0," + //
      MediaColumns.PLAY_STATUS + " INTEGER ,";

  public static Uri getMediaScannerUri() {
    return Uri.parse(CONTENT_AUTHORITY_SLASH + "media_scanner");
  }

  public static Uri getVolumeUri() {
    return Uri.parse(CONTENT_AUTHORITY_SLASH + MEDIA_SCANNER_VOLUME);
  }

  public interface MediaColumns extends BaseColumns {
    public static final String DATA = "_data";
    public static final String DIRECTORY = "_directory";
    public static final String DIRECTORY_NAME = "_directory_name";
    public static final String SIZE = "_size";
    public static final String DISPLAY_NAME = "_display_name";
    public static final String TITLE = "title";
    public static final String TITLE_KEY = "title_key";
    public static final String DATE_ADDED = "date_added";
    public static final String DATE_MODIFIED = "date_modified";
    public static final String MIME_TYPE = "mime_type";
    public static final String AVAILABLE_SIZE = "available_size";
    public static final String PLAY_STATUS = "play_status";

  }

  public static final class Audio {
    public interface AudioColumns extends MediaColumns {
      public static final String DURATION = "duration";
      public static final String BOOKMARK = "bookmark";
      public static final String ARTIST = "artist";
      public static final String COMPOSER = "composer";
      public static final String ALBUM = "album";
      public static final String TRACK = "track";
      public static final String YEAR = "year";
    }

    public static final class Media implements AudioColumns {
      public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY_SLASH + "audios/media");
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/audio";
    }
  }

  public static final class Video {

    public interface VideoColumns extends MediaColumns {
      public static final String DURATION = "duration";
      public static final String ARTIST = "artist";
      public static final String ALBUM = "album";
      public static final String WIDTH = "width";
      public static final String HEIGHT = "height";
      public static final String DESCRIPTION = "description";
      public static final String LANGUAGE = "language";
      public static final String LATITUDE = "latitude";
      public static final String LONGITUDE = "longitude";
      public static final String DATE_TAKEN = "datetaken";
      public static final String BOOKMARK = "bookmark";
      public static final String MINI_THUMB_MAGIC = "mini_thumb_magic";
      public static final String HIDDEN = "hidden";
      public static final String SUBTRACK = "sub_track";
      public static final String AUDIO_TRACK = "audio_track";
    }

    public static final class Media implements VideoColumns {
      public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY_SLASH + "videos/media");
      public static final String CONTENT_TYPE = "vnd.android.cursor.dir/video";
      protected static final String TABLE_NAME = "videos";
      protected static final String SQL_FIELDS = BASE_SQL_FIELDS + //
          DURATION + " INTEGER," + //
          ARTIST + " TEXT," + //
          ALBUM + " TEXT," + //
          WIDTH + " INTEGER," + //
          HEIGHT + " INTEGER," + //
          DESCRIPTION + " TEXT," + //
          LANGUAGE + " TEXT," + //
          LATITUDE + " DOUBLE," + //
          LONGITUDE + " DOUBLE," + //
          DATE_TAKEN + " INTEGER," + //
          BOOKMARK + " INTEGER," + //
          MINI_THUMB_MAGIC + " INTEGER," + //
          HIDDEN + " INTEGER default 0," + // 1 hidden , 0 visible
          SUBTRACK + " TEXT," + //
          AUDIO_TRACK + " INTEGER";
      protected static final String SQL_TRIGGER_VIDEO_CLEANUP = "CREATE TRIGGER " + //
          "IF NOT EXISTS video_cleanup AFTER DELETE ON " + TABLE_NAME + " " + //
          "BEGIN " + //
          "SELECT _DELETE_FILE(old._data);" + //
          "SELECT _DELETE_FILE(old._data || '.ssi');" + // The cache index
          "END";
      protected static final String SQL_TRIGGER_VIDEO_UPDATE = "CREATE TRIGGER " + //
          "IF NOT EXISTS video_update AFTER UPDATE ON " + TABLE_NAME + " " + //
          "WHEN new._data <> old._data " + //
          "BEGIN " + //
          "SELECT _DELETE_FILE(old._data || '.ssi');" + //
          "END";
    }

    public static class Thumbnails implements BaseColumns {
      public static final int MINI_KIND = 1;
      public static final int MICRO_KIND = 3;
      public static final Uri CONTENT_URI = Uri.parse(CONTENT_AUTHORITY_SLASH + "videos/thumbnails");
      public static final String THUMBNAILS_DIRECTORY = "Android/data/me.abitno.vplayer.t/thumbnails";
      public static final String DATA = "_data";
      public static final String VIDEO_ID = "video_id";
      public static final String KIND = "kind";
      public static final String WIDTH = "width";
      public static final String HEIGHT = "height";
      protected static final String TABLE_NAME = "videothumbnails";
      protected static final String SQL_FIELDS = _ID + " INTEGER PRIMARY KEY," + //
          DATA + " TEXT," + //
          VIDEO_ID + " INTEGER," + //
          KIND + " INTEGER," + //
          WIDTH + " INTEGER," + //
          HEIGHT + " INTEGER";
      protected static final String SQL_INDEX_VIDEO_ID = "CREATE INDEX IF NOT EXISTS video_id_index on videothumbnails(video_id);";
      protected static final String SQL_TRIGGER_VIDEO_THUMBNAILS_CLEANUP = "CREATE TRIGGER " + //
          "IF NOT EXISTS videothumbnails_cleanup DELETE ON videothumbnails " + //
          "BEGIN " + //
          "SELECT _DELETE_FILE(old._data);" + //
          "END";

      public static void cancelThumbnailRequest(ContentResolver cr, long origId) {
        InternalThumbnails.cancelThumbnailRequest(cr, origId, CONTENT_URI, InternalThumbnails.DEFAULT_GROUP_ID);
      }

      public static Bitmap getThumbnail(Context ctx, ContentResolver cr, long origId, int kind, BitmapFactory.Options options) {
        return InternalThumbnails.getThumbnail(ctx, cr, origId, InternalThumbnails.DEFAULT_GROUP_ID, kind, options, CONTENT_URI);
      }

      public static Bitmap getThumbnail(Context ctx, ContentResolver cr, long origId, long groupId, int kind, BitmapFactory.Options options) {
        return InternalThumbnails.getThumbnail(ctx, cr, origId, groupId, kind, options, CONTENT_URI);
      }

      public static String getThumbnailPath(Context ctx, ContentResolver cr, long origId) {
        return InternalThumbnails.getThumbnailPath(ctx, cr, origId, CONTENT_URI);
      }

      public static void cancelThumbnailRequest(ContentResolver cr, long origId, long groupId) {
        InternalThumbnails.cancelThumbnailRequest(cr, origId, CONTENT_URI, groupId);
      }
    }
  }

  private static class InternalThumbnails implements BaseColumns {
    static final int DEFAULT_GROUP_ID = 0;
    private static final int MINI_KIND = 1;
    private static final int MICRO_KIND = 3;
    private static final String[] PROJECTION = new String[]{_ID, MediaColumns.DATA};
    private static final Object sThumbBufLock = new Object();
    private static byte[] sThumbBuf;

    private static Bitmap getMiniThumbFromFile(Cursor c, Uri baseUri, ContentResolver cr, BitmapFactory.Options options) {
      Bitmap bitmap = null;
      Uri thumbUri = null;
      try {
        long thumbId = c.getLong(0);
        thumbUri = ContentUris.withAppendedId(baseUri, thumbId);
        ParcelFileDescriptor pfdInput = cr.openFileDescriptor(thumbUri, "r");
        bitmap = BitmapFactory.decodeFileDescriptor(pfdInput.getFileDescriptor(), null, options);
        pfdInput.close();
      } catch (FileNotFoundException ex) {
        Log.e("getMiniThumbFromFile", ex);
      } catch (IOException ex) {
        Log.e("getMiniThumbFromFile", ex);
      } catch (OutOfMemoryError ex) {
        Log.e("getMiniThumbFromFile", ex);
      }
      return bitmap;
    }

    static void cancelThumbnailRequest(ContentResolver cr, long origId, Uri baseUri, long groupId) {
      Uri cancelUri = baseUri.buildUpon().appendQueryParameter("cancel", "1").appendQueryParameter("orig_id", String.valueOf(origId)).appendQueryParameter("group_id", String.valueOf(groupId)).build();
      Cursor c = null;
      try {
        c = cr.query(cancelUri, PROJECTION, null, null, null);
      } finally {
        if (c != null)
          c.close();
      }
    }

    static String getThumbnailPath(Context ctx, ContentResolver cr, long origId, Uri baseUri) {
      String column = "video_id=";
      String path = "";
      Cursor c = null;
      try {
        c = cr.query(baseUri, PROJECTION, column + origId, null, null);
        if (c != null && c.moveToFirst()) {
          path = c.getString(c.getColumnIndex(MediaColumns.DATA));
        }
      } finally {
        if (c != null)
          c.close();
      }
      return path;
    }

    static Bitmap getThumbnail(Context ctx, ContentResolver cr, long origId, long groupId, int kind, BitmapFactory.Options options, Uri baseUri) {
      Bitmap bitmap = null;
      MiniThumbFile thumbFile = MiniThumbFile.instance(baseUri);
      long magic = thumbFile.getMagic(origId);
      if (magic != 0) {
        if (kind == MICRO_KIND) {
          synchronized (sThumbBufLock) {
            if (sThumbBuf == null)
              sThumbBuf = new byte[MiniThumbFile.BYTES_PER_MINTHUMB];
            if (thumbFile.getMiniThumbFromFile(origId, sThumbBuf) != null) {
              bitmap = BitmapFactory.decodeByteArray(sThumbBuf, 0, sThumbBuf.length);
              if (bitmap == null)
                Log.d("couldn't decode byte array.");
            }
          }
          return bitmap;
        } else if (kind == MINI_KIND) {
          String column = "video_id=";
          Cursor c = null;
          try {
            c = cr.query(baseUri, PROJECTION, column + origId, null, null);
            if (c != null && c.moveToFirst()) {
              bitmap = getMiniThumbFromFile(c, baseUri, cr, options);
              if (bitmap != null)
                return bitmap;
            }
          } finally {
            if (c != null)
              c.close();
          }
        }
      }

      Cursor c = null;
      try {
        Uri blockingUri = baseUri.buildUpon().appendQueryParameter("blocking", "1").appendQueryParameter("orig_id", String.valueOf(origId)).appendQueryParameter("group_id", String.valueOf(groupId)).build();
        c = cr.query(blockingUri, PROJECTION, null, null, null);
        if (c == null)
          return null;

        if (kind == MICRO_KIND) {
          synchronized (sThumbBufLock) {
            if (sThumbBuf == null)
              sThumbBuf = new byte[MiniThumbFile.BYTES_PER_MINTHUMB];
            if (thumbFile.getMiniThumbFromFile(origId, sThumbBuf) != null) {
              bitmap = BitmapFactory.decodeByteArray(sThumbBuf, 0, sThumbBuf.length);
              if (bitmap == null)
                Log.d("couldn't decode byte array.");
            }
          }
        } else if (kind == MINI_KIND) {
          if (c.moveToFirst())
            bitmap = getMiniThumbFromFile(c, baseUri, cr, options);
        } else {
          throw new IllegalArgumentException("Unsupported kind: " + kind);
        }
      } catch (SQLiteException ex) {
        Log.e("getThumbnail", ex);
      } finally {
        if (c != null)
          c.close();
      }
      return bitmap;
    }
  }

}
