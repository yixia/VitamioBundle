/*
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

import io.vov.vitamio.utils.CPU;
import io.vov.vitamio.utils.ContextUtils;
import io.vov.vitamio.utils.IOUtils;
import io.vov.vitamio.utils.Log;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Inspect this class before using any other Vitamio classes.
 * <p/>
 * Don't modify this class, or the full Vitamio library may be broken.
 */
public class Vitamio {
  private static final String[] LIBS_ARM_CODECS = {"libvvo.7.so", "libvvo.8.so", "libffmpeg.so", "libOMX.9.so", "libOMX.11.so", "libOMX.14.so", "libOMX.18.so"};
  private static final String[] LIBS_X86_CODECS = {"libffmpeg.so", "libOMX.9.so", "libOMX.14.so", "libOMX.18.so"};
  private static final String[] LIBS_MIPS_CODECS = {"libffmpeg.so", "libOMX.14.so"};
  private static final String[] LIBS_PLAYER = {"libvplayer.so"};
  private static final String[] LIBS_SCANNER = {"libvscanner.so"};
  private static final String[] LIBS_AV = {"libvao.0.so", "libvvo.0.so", "libvvo.9.so", "libvvo.j.so"};
  private static final String LIBS_LOCK = ".lock";
  private static final int VITAMIO_NOT_SUPPORTED = -1;
  private static final int VITAMIO_MIPS = 40;
  private static final int VITAMIO_X86 = 50;
  private static final int VITAMIO_ARMV6 = 60;
  private static final int VITAMIO_ARMV6_VFP = 61;
  private static final int VITAMIO_ARMV7_VFPV3 = 70;
  private static final int VITAMIO_ARMV7_NEON = 71;
  private static final int vitamioType;

  static {
    int cpu = CPU.getFeature();
    if ((cpu & CPU.FEATURE_ARM_NEON) > 0)
      vitamioType = VITAMIO_ARMV7_NEON;
    else if ((cpu & CPU.FEATURE_ARM_VFPV3) > 0 && (cpu & CPU.FEATURE_ARM_V7A) > 0)
      vitamioType = VITAMIO_ARMV7_VFPV3;
    else if ((cpu & CPU.FEATURE_ARM_VFP) > 0 && (cpu & CPU.FEATURE_ARM_V6) > 0)
      vitamioType = VITAMIO_ARMV6_VFP;
    else if ((cpu & CPU.FEATURE_ARM_V6) > 0)
    	vitamioType = VITAMIO_ARMV6;
    else if ((cpu & CPU.FEATURE_X86) > 0)
    	vitamioType = VITAMIO_X86;
    else if ((cpu & CPU.FEATURE_MIPS) > 0) 
    	vitamioType = VITAMIO_MIPS;
    else
    	vitamioType = VITAMIO_NOT_SUPPORTED;
  }

  private static String vitamioPackage;
  private static String vitamioLibraryPath;

  /**
   * Call this method before using any other Vitamio specific classes.
   * <p/>
   * This method will use {@link #isInitialized(Context)} to check if Vitamio is
   * initialized at this device, and initialize it if not initialized.
   *
   * @param ctx Android Context
   * @return true if the Vitamio initialized successfully.
   */
  public static boolean initialize(Context ctx) {
    return isInitialized(ctx) || extractLibs(ctx, R.raw.libarm);
  }

  /**
   * Same as {@link #initialize(Context)}
   *
   * @param ctx   Android Context
   * @param rawId R.raw.libarm
   * @return true if the Vitamio initialized successfully.
   */
  public static boolean initialize(Context ctx, int rawId) {
    return isInitialized(ctx) || extractLibs(ctx, rawId);
  }

  /**
   * Check if Vitamio is initialized at this device
   *
   * @param ctx Android Context
   * @return true if the Vitamio has been initialized.
   */
  public static boolean isInitialized(Context ctx) {
    vitamioPackage = ctx.getPackageName();
    vitamioLibraryPath = ContextUtils.getDataDir(ctx) + "libs/";
    File dir = new File(getLibraryPath());
    if (dir.exists() && dir.isDirectory()) {
      String[] libs = dir.list();
      if (libs != null) {
        Arrays.sort(libs);
        for (String L : getRequiredLibs()) {
          if (Arrays.binarySearch(libs, L) < 0) {
            Log.e("Native libs %s not exists!", L);
            return false;
          }
        }
        File lock = new File(getLibraryPath() + LIBS_LOCK);
        BufferedReader buffer = null; 
        try {
          buffer = new BufferedReader(new FileReader(lock));  
          int appVersion = ContextUtils.getVersionCode(ctx);
          int libVersion = Integer.valueOf(buffer.readLine());  
          Log.i("isNativeLibsInited, APP VERSION: %d, Vitamio Library version: %d", appVersion, libVersion);
          if (libVersion == appVersion)
            return true;
        } catch (IOException e) {
          Log.e("isNativeLibsInited", e);
        } catch (NumberFormatException e) {
        	Log.e("isNativeLibsInited", e);
        } finally {
          IOUtils.closeSilently(buffer);
        }
      }
    }
    return false;
  }

  public static String getVitamioPackage() {
    return vitamioPackage;
  }

  public static int getVitamioType() {
    return vitamioType;
  }

  public static final String getLibraryPath() {
    return vitamioLibraryPath;
  }

  private static final List<String> getRequiredLibs() {
    List<String> libs = new ArrayList<String>();
    String[][] vitamioLibs = null;
    switch (vitamioType) {
		case VITAMIO_ARMV6:
		case VITAMIO_ARMV6_VFP:
		case VITAMIO_ARMV7_VFPV3:
		case VITAMIO_ARMV7_NEON:
			vitamioLibs = new String[][]{LIBS_ARM_CODECS, LIBS_PLAYER, LIBS_SCANNER, LIBS_AV};
			break;
		case VITAMIO_X86:
			vitamioLibs = new String[][]{LIBS_X86_CODECS, LIBS_PLAYER, LIBS_SCANNER, LIBS_AV};
			break;
		case VITAMIO_MIPS:
			vitamioLibs = new String[][]{LIBS_MIPS_CODECS, LIBS_PLAYER, LIBS_SCANNER, LIBS_AV};
			break;
		default:
			break;
		}
    if (vitamioLibs == null)
    	return libs;
    for (String[] libArray : vitamioLibs) {
      for (String lib : libArray)
        libs.add(lib);
    }
    libs.add(LIBS_LOCK);
    return libs;
  }

  private static boolean extractLibs(Context ctx, int rawID) {
    long begin = System.currentTimeMillis();
    final int version = ContextUtils.getVersionCode(ctx);
    Log.d("loadLibs start " + version);
    File lock = new File(getLibraryPath() + LIBS_LOCK);
    if (lock.exists())
      lock.delete();
    String libPath = copyCompressedLib(ctx, rawID, "libarm.so");
    Log.d("copyCompressedLib time: " + (System.currentTimeMillis() - begin) / 1000.0);
    boolean inited = native_initializeLibs(libPath, getLibraryPath(), String.valueOf(Vitamio.getVitamioType()));
    new File(libPath).delete();
    FileWriter fw = null;
    try {
      lock.createNewFile();
      fw = new FileWriter(lock);
      fw.write(String.valueOf(version));
      return true;
    } catch (IOException e) {
      Log.e("Error creating lock file", e);
    } finally {
      Log.d("initializeNativeLibs: " + inited);
      Log.d("loadLibs time: " + (System.currentTimeMillis() - begin) / 1000.0);
      IOUtils.closeSilently(fw);
    }
    return false;
  }

  private static String copyCompressedLib(Context ctx, int rawID, String destName) {
    byte[] buffer = new byte[1024];
    InputStream is = null;
    BufferedInputStream bis = null;
    FileOutputStream fos = null;
    String destPath = null;

    try {
      try {
        String destDir = getLibraryPath();
        destPath = destDir + destName;
        File f = new File(destDir);
        if (f.exists() && !f.isDirectory())
          f.delete();
        if (!f.exists())
          f.mkdirs();
        f = new File(destPath);
        if (f.exists() && !f.isFile())
          f.delete();
        if (!f.exists())
          f.createNewFile();
      } catch (Exception fe) {
        Log.e("loadLib", fe);
      }

      is = ctx.getResources().openRawResource(rawID);
      bis = new BufferedInputStream(is);
      fos = new FileOutputStream(destPath);
      while (bis.read(buffer) != -1) {
        fos.write(buffer);
      }
    } catch (Exception e) {
      Log.e("loadLib", e);
      return null;
    } finally {
      IOUtils.closeSilently(fos);
      IOUtils.closeSilently(bis);
      IOUtils.closeSilently(is);
    }

    return destPath;
  }

  static {
    System.loadLibrary("vinit");
  }

  private native static boolean native_initializeLibs(String libPath, String destDir, String prefix);
}
