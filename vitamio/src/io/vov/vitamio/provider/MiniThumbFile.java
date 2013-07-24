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

import android.net.Uri;
import android.os.Environment;

import io.vov.vitamio.provider.MediaStore.Video;
import io.vov.vitamio.utils.Log;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.Hashtable;

public class MiniThumbFile {
  protected static final int BYTES_PER_MINTHUMB = 10000;
  private static final int MINI_THUMB_DATA_FILE_VERSION = 7;
  private static final int HEADER_SIZE = 1 + 8 + 4;
  private static Hashtable<String, MiniThumbFile> sThumbFiles = new Hashtable<String, MiniThumbFile>();
  private Uri mUri;
  private RandomAccessFile mMiniThumbFile;
  private FileChannel mChannel;
  private ByteBuffer mBuffer;

  public MiniThumbFile(Uri uri) {
    mUri = uri;
    mBuffer = ByteBuffer.allocateDirect(BYTES_PER_MINTHUMB);
  }

  protected static synchronized void reset() {
    for (MiniThumbFile file : sThumbFiles.values())
      file.deactivate();
    sThumbFiles.clear();
  }

  protected static synchronized MiniThumbFile instance(Uri uri) {
    String type = uri.getPathSegments().get(0);
    MiniThumbFile file = sThumbFiles.get(type);
    if (file == null) {
      file = new MiniThumbFile(Uri.parse(MediaStore.CONTENT_AUTHORITY_SLASH + type + "/media"));
      sThumbFiles.put(type, file);
    }

    return file;
  }

  private String randomAccessFilePath(int version) {
    String directoryName = Environment.getExternalStorageDirectory().toString() + "/" + Video.Thumbnails.THUMBNAILS_DIRECTORY;
    return directoryName + "/.thumbdata" + version + "-" + mUri.hashCode();
  }

  private void removeOldFile() {
    String oldPath = randomAccessFilePath(MINI_THUMB_DATA_FILE_VERSION - 1);
    File oldFile = new File(oldPath);
    if (oldFile.exists()) {
      try {
        oldFile.delete();
      } catch (SecurityException ex) {
      }
    }
  }

  private RandomAccessFile miniThumbDataFile() {
    if (mMiniThumbFile == null) {
      removeOldFile();
      String path = randomAccessFilePath(MINI_THUMB_DATA_FILE_VERSION);
      File directory = new File(path).getParentFile();
      if (!directory.isDirectory()) {
        if (!directory.mkdirs())
          Log.e("Unable to create .thumbnails directory %s", directory.toString());
      }
      File f = new File(path);
      try {
        mMiniThumbFile = new RandomAccessFile(f, "rw");
      } catch (IOException ex) {
        try {
          mMiniThumbFile = new RandomAccessFile(f, "r");
        } catch (IOException ex2) {
        }
      }

      if (mMiniThumbFile != null)
        mChannel = mMiniThumbFile.getChannel();
    }
    return mMiniThumbFile;
  }

  protected synchronized void deactivate() {
    if (mMiniThumbFile != null) {
      try {
        mMiniThumbFile.close();
        mMiniThumbFile = null;
      } catch (IOException ex) {
      }
    }
  }

  protected synchronized long getMagic(long id) {
    RandomAccessFile r = miniThumbDataFile();
    if (r != null) {
      long pos = id * BYTES_PER_MINTHUMB;
      FileLock lock = null;
      try {
        mBuffer.clear();
        mBuffer.limit(1 + 8);

        lock = mChannel.lock(pos, 1 + 8, true);
        if (mChannel.read(mBuffer, pos) == 9) {
          mBuffer.position(0);
          if (mBuffer.get() == 1)
            return mBuffer.getLong();
        }
      } catch (IOException ex) {
        Log.e("Got exception checking file magic: ", ex);
      } catch (RuntimeException ex) {
        Log.e("Got exception when reading magic, id = %d, disk full or mount read-only? %s", id, ex.getClass().toString());
      } finally {
        try {
          if (lock != null)
            lock.release();
        } catch (IOException ex) {
        }
      }
    }
    return 0;
  }

  protected synchronized void saveMiniThumbToFile(byte[] data, long id, long magic) throws IOException {
    RandomAccessFile r = miniThumbDataFile();
    if (r == null)
      return;

    long pos = id * BYTES_PER_MINTHUMB;
    FileLock lock = null;
    try {
      if (data != null) {
        if (data.length > BYTES_PER_MINTHUMB - HEADER_SIZE)
          return;

        mBuffer.clear();
        mBuffer.put((byte) 1);
        mBuffer.putLong(magic);
        mBuffer.putInt(data.length);
        mBuffer.put(data);
        mBuffer.flip();

        lock = mChannel.lock(pos, BYTES_PER_MINTHUMB, false);
        mChannel.write(mBuffer, pos);
      }
    } catch (IOException ex) {
      Log.e("couldn't save mini thumbnail data for %d; %s", id, ex.getMessage());
      throw ex;
    } catch (RuntimeException ex) {
      Log.e("couldn't save mini thumbnail data for %d, disk full or mount read-only? %s", id, ex.getClass().toString());
    } finally {
      try {
        if (lock != null)
          lock.release();
      } catch (IOException ex) {
      }
    }
  }

  protected synchronized byte[] getMiniThumbFromFile(long id, byte[] data) {
    RandomAccessFile r = miniThumbDataFile();
    if (r == null)
      return null;

    long pos = id * BYTES_PER_MINTHUMB;
    FileLock lock = null;
    try {
      mBuffer.clear();
      lock = mChannel.lock(pos, BYTES_PER_MINTHUMB, true);
      int size = mChannel.read(mBuffer, pos);
      if (size > 1 + 8 + 4) {
        mBuffer.position(9);
        int length = mBuffer.getInt();

        if (size >= 1 + 8 + 4 + length && data.length >= length) {
          mBuffer.get(data, 0, length);
          return data;
        }
      }
    } catch (IOException ex) {
      Log.e("got exception when reading thumbnail id = %d, exception: %s", id, ex.getMessage());
    } catch (RuntimeException ex) {
      Log.e("Got exception when reading thumbnail, id = %d, disk full or mount read-only? %s", id, ex.getClass().toString());
    } finally {
      try {
        if (lock != null)
          lock.release();
      } catch (IOException ex) {
      }
    }
    return null;
  }
}