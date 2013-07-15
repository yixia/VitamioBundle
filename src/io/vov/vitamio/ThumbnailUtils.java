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

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import io.vov.vitamio.provider.MediaStore.Video;

/**
 * ThumbnailUtils is a wrapper of MediaMetadataRetriever to retrive a thumbnail
 * of video file.
 * <p/>
 * <pre>
 * Bitmap thumb = ThumbnailUtils.createVideoThumbnail(this, videoPath, MINI_KIND);
 * </pre>
 */
public class ThumbnailUtils {
  private static final int OPTIONS_NONE = 0x0;
  private static final int OPTIONS_SCALE_UP = 0x1;
  public static final int OPTIONS_RECYCLE_INPUT = 0x2;
  public static final int TARGET_SIZE_MINI_THUMBNAIL_WIDTH = 426;
  public static final int TARGET_SIZE_MINI_THUMBNAIL_HEIGHT = 320;
  public static final int TARGET_SIZE_MICRO_THUMBNAIL_WIDTH = 212;
  public static final int TARGET_SIZE_MICRO_THUMBNAIL_HEIGHT = 160;
  

  public static Bitmap createVideoThumbnail(Context ctx, String filePath, int kind) {
    if (!Vitamio.isInitialized(ctx)) {
      return null;
    }
    Bitmap bitmap = null;
    MediaMetadataRetriever retriever = null;
    try {
      retriever = new MediaMetadataRetriever(ctx);
      retriever.setDataSource(filePath);
      bitmap = retriever.getFrameAtTime(-1);
    } catch (Exception ex) {
    } finally {
      try {
        retriever.release();
      } catch (RuntimeException ex) {
      }
    }

    if (bitmap != null) {
      if (kind == Video.Thumbnails.MICRO_KIND)
        bitmap = extractThumbnail(bitmap, TARGET_SIZE_MICRO_THUMBNAIL_WIDTH, TARGET_SIZE_MICRO_THUMBNAIL_HEIGHT, OPTIONS_RECYCLE_INPUT);
      else if (kind == Video.Thumbnails.MINI_KIND)
        bitmap = extractThumbnail(bitmap, TARGET_SIZE_MINI_THUMBNAIL_WIDTH, TARGET_SIZE_MINI_THUMBNAIL_HEIGHT, OPTIONS_RECYCLE_INPUT);
    }
    return bitmap;
  }

  public static Bitmap extractThumbnail(Bitmap source, int width, int height) {
    return extractThumbnail(source, width, height, OPTIONS_NONE);
  }

  public static Bitmap extractThumbnail(Bitmap source, int width, int height, int options) {
    if (source == null)
      return null;

    float scale;
    if (source.getWidth() < source.getHeight())
      scale = width / (float) source.getWidth();
    else
      scale = height / (float) source.getHeight();
    Matrix matrix = new Matrix();
    matrix.setScale(scale, scale);
    Bitmap thumbnail = transform(matrix, source, width, height, OPTIONS_SCALE_UP | options);
    return thumbnail;
  }

  private static Bitmap transform(Matrix scaler, Bitmap source, int targetWidth, int targetHeight, int options) {
    boolean scaleUp = (options & OPTIONS_SCALE_UP) != 0;
    boolean recycle = (options & OPTIONS_RECYCLE_INPUT) != 0;

    int deltaX = source.getWidth() - targetWidth;
    int deltaY = source.getHeight() - targetHeight;
    if (!scaleUp && (deltaX < 0 || deltaY < 0)) {
      Bitmap b2 = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888);
      Canvas c = new Canvas(b2);

      int deltaXHalf = Math.max(0, deltaX / 2);
      int deltaYHalf = Math.max(0, deltaY / 2);
      Rect src = new Rect(deltaXHalf, deltaYHalf, deltaXHalf + Math.min(targetWidth, source.getWidth()), deltaYHalf + Math.min(targetHeight, source.getHeight()));
      int dstX = (targetWidth - src.width()) / 2;
      int dstY = (targetHeight - src.height()) / 2;
      Rect dst = new Rect(dstX, dstY, targetWidth - dstX, targetHeight - dstY);
      c.drawBitmap(source, src, dst, null);
      if (recycle)
        source.recycle();
      return b2;
    }

    float bitmapWidthF = source.getWidth();
    float bitmapHeightF = source.getHeight();
    float bitmapAspect = bitmapWidthF / bitmapHeightF;
    float viewAspect = (float) targetWidth / targetHeight;

    float scale = bitmapAspect > viewAspect ? targetHeight / bitmapHeightF : targetWidth / bitmapWidthF;
    if (scale < .9F || scale > 1F)
      scaler.setScale(scale, scale);
    else
      scaler = null;

    Bitmap b1;
    if (scaler != null)
      b1 = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), scaler, true);
    else
      b1 = source;

    if (recycle && b1 != source)
      source.recycle();

    int dx1 = Math.max(0, b1.getWidth() - targetWidth);
    int dy1 = Math.max(0, b1.getHeight() - targetHeight);

    Bitmap b2 = Bitmap.createBitmap(b1, dx1 / 2, dy1 / 2, targetWidth, targetHeight);

    if (b2 != b1 && (recycle || b1 != source))
      b1.recycle();

    return b2;
  }

}