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
package io.vov.vitamio.utils;

import android.content.Context;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;

import com.yixia.zi.utils.ContextUtils;
import com.yixia.zi.utils.IOUtils;
import com.yixia.zi.utils.Log;

public class VitamioInstaller {

	private static final String[] LIBS = { "libvplayer.so", "libvscanner.so", "libffmpeg.so", "libvao.0.so", "libvvo.0.so", "libvvo.9.so", "libvvo.j.so", "libOMX.9.so", "libOMX.11.so", "libOMX.14.so", "libOMX.16.so", "inited.lock" };


	/**
	 * Get the absolute path to Vitamio's library
	 * 
	 * @param ctx a Context
	 * @return the absolute path to the libffmpeg.so
	 * @throws VitamioNotCompatibleException
	 * @throws VitamioNotFoundException
	 */
	public static final String getLibraryPath(String packageName) {
		return "/data/data/" + packageName + "/libs/";
	}

	public static boolean isNativeLibsInited(Context context) {
		String packageName = context.getPackageName();
		File dir = new File(VitamioInstaller.getLibraryPath(packageName));
		Log.i("isNativeLibsInited: %s, %b, %b", dir.getAbsolutePath(), dir.exists(), dir.isDirectory());
		if (dir.exists() && dir.isDirectory()) {
			String[] libs = dir.list();
			if (libs != null) {
				Arrays.sort(libs);
				for (String L : LIBS) {
					if (Arrays.binarySearch(libs, L) < 0) {
						Log.e("Native libs %s not exists!", L);
						return false;
					}
				}
				File lock = new File(getLibraryPath(packageName) + "/inited.lock");
				FileReader fr = null;
				try {
					fr = new FileReader(lock);
					int appVersion = ContextUtils.getVersionCode(context);
					int libVersion = fr.read();
					Log.i("isNativeLibsInited, APP VERSION: %d, Vitamio Library version: %d", appVersion, libVersion);
					if (libVersion == appVersion)
						return true;
				} catch (IOException e) {
					Log.e("isNativeLibsInited", e);
				} finally {
					IOUtils.closeSilently(fr);
				}
			}
		}
		return false;
	}
}
