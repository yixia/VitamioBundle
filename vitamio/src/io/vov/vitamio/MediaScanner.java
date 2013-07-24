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

import android.content.ContentProviderClient;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.net.Uri;
import android.os.RemoteException;
import android.text.TextUtils;

import io.vov.vitamio.provider.MediaStore;
import io.vov.vitamio.provider.MediaStore.Video;
import io.vov.vitamio.utils.ContextUtils;
import io.vov.vitamio.utils.FileUtils;
import io.vov.vitamio.utils.Log;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;

public class MediaScanner {
  private static final String[] VIDEO_PROJECTION = new String[]{Video.Media._ID, Video.Media.DATA, Video.Media.DATE_MODIFIED,};
  private static final int ID_VIDEO_COLUMN_INDEX = 0;
  private static final int PATH_VIDEO_COLUMN_INDEX = 1;
  private static final int DATE_MODIFIED_VIDEO_COLUMN_INDEX = 2;
  private Context mContext;
  private ContentProviderClient mProvider;
  private boolean mCaseInsensitivePaths;
  private HashMap<String, FileCacheEntry> mFileCache;
  private MyMediaScannerClient mClient = new MyMediaScannerClient();

  public MediaScanner(Context ctx) {
    mContext = ctx;
    native_init(mClient);
  }

  private static native boolean loadFFmpeg_native(String ffmpegPath);

  private void initialize() {
    mCaseInsensitivePaths = true;
  }

  private void prescan(String filePath) throws RemoteException {
    mProvider = mContext.getContentResolver().acquireContentProviderClient(MediaStore.AUTHORITY);
    Cursor c = null;
    String where = null;
    String[] selectionArgs = null;

    if (mFileCache == null)
      mFileCache = new HashMap<String, FileCacheEntry>();
    else
      mFileCache.clear();

    try {
      if (filePath != null) {
        where = Video.Media.DATA + "=?";
        selectionArgs = new String[]{filePath};
      }

      c = mProvider.query(Video.Media.CONTENT_URI, VIDEO_PROJECTION, where, selectionArgs, null);
      if (c != null) {
        try {
          while (c.moveToNext()) {
            long rowId = c.getLong(ID_VIDEO_COLUMN_INDEX);
            String path = c.getString(PATH_VIDEO_COLUMN_INDEX);
            long lastModified = c.getLong(DATE_MODIFIED_VIDEO_COLUMN_INDEX);
            if (path.startsWith("/")) {
              File tempFile = new File(path);
              if (!TextUtils.isEmpty(filePath) && !tempFile.exists()) {
                mProvider.delete(Video.Media.CONTENT_URI, where, selectionArgs);
                return;
              }
              path = FileUtils.getCanonical(tempFile);
              String key = mCaseInsensitivePaths ? path.toLowerCase() : path;
              mFileCache.put(key, new FileCacheEntry(Video.Media.CONTENT_URI, rowId, path, lastModified));
            }
          }
        } finally {
          c.close();
          c = null;
        }
      }
    } finally {
      if (c != null) {
        c.close();
      }
    }
  }

  ;

  private void postscan(String[] directories) throws RemoteException {
    Iterator<FileCacheEntry> iterator = mFileCache.values().iterator();

    while (iterator.hasNext()) {
      FileCacheEntry entry = iterator.next();
      String path = entry.mPath;

      if (!entry.mSeenInFileSystem) {
        if (inScanDirectory(path, directories) && !new File(path).exists()) {
          mProvider.delete(ContentUris.withAppendedId(entry.mTableUri, entry.mRowId), null, null);
          iterator.remove();
        }
      }
    }

    mFileCache.clear();
    mFileCache = null;
    mProvider.release();
    mProvider = null;
  }

  private boolean inScanDirectory(String path, String[] directories) {
    for (int i = 0; i < directories.length; i++) {
      if (path.startsWith(directories[i]))
        return true;
    }
    return false;
  }

  public void scanDirectories(String[] directories) {
    try {
      long start = System.currentTimeMillis();
      prescan(null);
      long prescan = System.currentTimeMillis();

      for (int i = 0; i < directories.length; i++) {
        if (!TextUtils.isEmpty(directories[i])) {
          directories[i] = ContextUtils.fixLastSlash(directories[i]);
          processDirectory(directories[i], MediaFile.sFileExtensions);
        }
      }

      long scan = System.currentTimeMillis();
      postscan(directories);
      long end = System.currentTimeMillis();

      Log.d(" prescan time: %dms", prescan - start);
      Log.d("    scan time: %dms", scan - prescan);
      Log.d("postscan time: %dms", end - scan);
      Log.d("   total time: %dms", end - start);
    } catch (SQLException e) {
      Log.e("SQLException in MediaScanner.scan()", e);
    } catch (UnsupportedOperationException e) {
      Log.e("UnsupportedOperationException in MediaScanner.scan()", e);
    } catch (RemoteException e) {
      Log.e("RemoteException in MediaScanner.scan()", e);
    }
  }

  public Uri scanSingleFile(String path, String mimeType) {
    try {
      prescan(path);
      File file = new File(path);
      long lastModifiedSeconds = file.lastModified() / 1000;

      return mClient.doScanFile(path, lastModifiedSeconds, file.length(), true);
    } catch (RemoteException e) {
      Log.e("RemoteException in MediaScanner.scanFile()", e);
      return null;
    }
  }

  static {
    String LIB_ROOT = Vitamio.getLibraryPath();
    Log.i("LIB ROOT: %s", LIB_ROOT);
    System.load(LIB_ROOT + "libstlport_shared.so");
    System.load(LIB_ROOT + "libvscanner.so");
    loadFFmpeg_native(LIB_ROOT + "libffmpeg.so");
  }

  private native void processDirectory(String path, String extensions);

  private native boolean processFile(String path, String mimeType);

  private native final void native_init(MediaScannerClient client);

  public native void release();

  private native final void native_finalize();

  @Override
  protected void finalize() throws Throwable {
    try {
      native_finalize();
    } finally {
      super.finalize();
    }
  }

  private static class FileCacheEntry {
    Uri mTableUri;
    long mRowId;
    String mPath;
    long mLastModified;
    boolean mLastModifiedChanged;
    boolean mSeenInFileSystem;

    FileCacheEntry(Uri tableUri, long rowId, String path, long lastModified) {
      mTableUri = tableUri;
      mRowId = rowId;
      mPath = path;
      mLastModified = lastModified;
      mSeenInFileSystem = false;
      mLastModifiedChanged = false;
    }

    @Override
    public String toString() {
      return mPath;
    }
  }

  private class MyMediaScannerClient implements MediaScannerClient {
    private String mMimeType;
    private int mFileType;
    private String mPath;
    private long mLastModified;
    private long mFileSize;
    private String mTitle;
    private String mArtist;
    private String mAlbum;
    private String mLanguage;
    private long mDuration;
    private int mWidth;
    private int mHeight;

    public FileCacheEntry beginFile(String path, long lastModified, long fileSize) {
      int lastSlash = path.lastIndexOf('/');
      if (lastSlash >= 0 && lastSlash + 2 < path.length()) {
        if (path.regionMatches(lastSlash + 1, "._", 0, 2))
          return null;

        if (path.regionMatches(true, path.length() - 4, ".jpg", 0, 4)) {
          if (path.regionMatches(true, lastSlash + 1, "AlbumArt_{", 0, 10) || path.regionMatches(true, lastSlash + 1, "AlbumArt.", 0, 9)) {
            return null;
          }
          int length = path.length() - lastSlash - 1;
          if ((length == 17 && path.regionMatches(true, lastSlash + 1, "AlbumArtSmall", 0, 13)) || (length == 10 && path.regionMatches(true, lastSlash + 1, "Folder", 0, 6))) {
            return null;
          }
        }
      }

      MediaFile.MediaFileType mediaFileType = MediaFile.getFileType(path);
      if (mediaFileType != null) {
        mFileType = mediaFileType.fileType;
        mMimeType = mediaFileType.mimeType;
      }

      String key = FileUtils.getCanonical(new File(path));
      if (mCaseInsensitivePaths)
        key = path.toLowerCase();
      FileCacheEntry entry = mFileCache.get(key);
      if (entry == null) {
        entry = new FileCacheEntry(null, 0, path, 0);
        mFileCache.put(key, entry);
      }
      entry.mSeenInFileSystem = true;

      long delta = lastModified - entry.mLastModified;
      if (delta > 1 || delta < -1) {
        entry.mLastModified = lastModified;
        entry.mLastModifiedChanged = true;
      }

      mPath = path;
      mLastModified = lastModified;
      mFileSize = fileSize;
      mTitle = null;
      mDuration = 0;

      return entry;
    }

    public void scanFile(String path, long lastModified, long fileSize) {
      Log.i("scanFile: %s", path);
      doScanFile(path, lastModified, fileSize, false);
    }

    public Uri doScanFile(String path, long lastModified, long fileSize, boolean scanAlways) {
      Uri result = null;
      try {
        FileCacheEntry entry = beginFile(path, lastModified, fileSize);
        if (entry != null && (entry.mLastModifiedChanged || scanAlways)) {
          if (processFile(path, null)) {
            result = endFile(entry);
          } else {
            if (mCaseInsensitivePaths)
              mFileCache.remove(path.toLowerCase());
            else
              mFileCache.remove(path);
          }
        }
      } catch (RemoteException e) {
        Log.e("RemoteException in MediaScanner.scanFile()", e);
      }
      return result;
    }

    private int parseSubstring(String s, int start, int defaultValue) {
      int length = s.length();
      if (start == length)
        return defaultValue;

      char ch = s.charAt(start++);
      if (ch < '0' || ch > '9')
        return defaultValue;

      int result = ch - '0';
      while (start < length) {
        ch = s.charAt(start++);
        if (ch < '0' || ch > '9')
          return result;
        result = result * 10 + (ch - '0');
      }

      return result;
    }

    public void handleStringTag(String name, byte[] valueBytes, String valueEncoding) {
      String value;
      try {
        value = new String(valueBytes, valueEncoding);
      } catch (Exception e) {
        Log.e("handleStringTag", e);
        value = new String(valueBytes);
      }
      Log.i("%s : %s", name, value);

      if (name.equalsIgnoreCase("title")) {
        mTitle = value;
      } else if (name.equalsIgnoreCase("artist")) {
        mArtist = value.trim();
      } else if (name.equalsIgnoreCase("albumartist")) {
        if (TextUtils.isEmpty(mArtist))
          mArtist = value.trim();
      } else if (name.equalsIgnoreCase("album")) {
        mAlbum = value.trim();
      } else if (name.equalsIgnoreCase("language")) {
        mLanguage = value.trim();
      } else if (name.equalsIgnoreCase("duration")) {
        mDuration = parseSubstring(value, 0, 0);
      } else if (name.equalsIgnoreCase("width")) {
        mWidth = parseSubstring(value, 0, 0);
      } else if (name.equalsIgnoreCase("height")) {
        mHeight = parseSubstring(value, 0, 0);
      }
    }

    public void setMimeType(String mimeType) {
      Log.i("setMimeType: %s", mimeType);
      mMimeType = mimeType;
      mFileType = MediaFile.getFileTypeForMimeType(mimeType);
    }

    private ContentValues toValues() {
      ContentValues map = new ContentValues();

      map.put(MediaStore.MediaColumns.DATA, mPath);
      map.put(MediaStore.MediaColumns.DATE_MODIFIED, mLastModified);
      map.put(MediaStore.MediaColumns.SIZE, mFileSize);
      map.put(MediaStore.MediaColumns.MIME_TYPE, mMimeType);
      map.put(MediaStore.MediaColumns.TITLE, mTitle);

      if (MediaFile.isVideoFileType(mFileType)) {
        map.put(Video.Media.DURATION, mDuration);
        map.put(Video.Media.LANGUAGE, mLanguage);
        map.put(Video.Media.ALBUM, mAlbum);
        map.put(Video.Media.ARTIST, mArtist);
        map.put(Video.Media.WIDTH, mWidth);
        map.put(Video.Media.HEIGHT, mHeight);
      }

      return map;
    }

    private Uri endFile(FileCacheEntry entry) throws RemoteException {
      Uri tableUri;
      boolean isVideo = MediaFile.isVideoFileType(mFileType) && mWidth > 0 && mHeight > 0;
      if (isVideo) {
        tableUri = Video.Media.CONTENT_URI;
      } else {
        return null;
      }
      entry.mTableUri = tableUri;

      ContentValues values = toValues();
      String title = values.getAsString(MediaStore.MediaColumns.TITLE);
      if (TextUtils.isEmpty(title)) {
        title = values.getAsString(MediaStore.MediaColumns.DATA);
        int lastSlash = title.lastIndexOf('/');
        if (lastSlash >= 0) {
          lastSlash++;
          if (lastSlash < title.length())
            title = title.substring(lastSlash);
        }
        int lastDot = title.lastIndexOf('.');
        if (lastDot > 0)
          title = title.substring(0, lastDot);
        values.put(MediaStore.MediaColumns.TITLE, title);
      }

      long rowId = entry.mRowId;

      Uri result = null;
      if (rowId == 0) {
        result = mProvider.insert(tableUri, values);
        if (result != null) {
          rowId = ContentUris.parseId(result);
          entry.mRowId = rowId;
        }
      } else {
        result = ContentUris.withAppendedId(tableUri, rowId);
        mProvider.update(result, values, null, null);
      }

      return result;
    }

    public void addNoMediaFolder(String path) {
      ContentValues values = new ContentValues();
      values.put(MediaStore.MediaColumns.DATA, "");
      String[] pathSpec = new String[]{path + '%'};
      try {
        mProvider.update(Video.Media.CONTENT_URI, values, MediaStore.MediaColumns.DATA + " LIKE ?", pathSpec);
      } catch (RemoteException e) {
        throw new RuntimeException();
      }
    }
  }
}