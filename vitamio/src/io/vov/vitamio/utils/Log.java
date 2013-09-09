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

import io.vov.vitamio.BuildConfig;

import java.util.MissingFormatArgumentException;

public class Log {
	public static final String TAG = "Vitamio[Player]";

	public static void i(String msg, Object... args) {
		try {
			if (BuildConfig.DEBUG) 
				android.util.Log.i(TAG, String.format(msg, args));
		} catch (MissingFormatArgumentException e) {
			android.util.Log.e(TAG, "vitamio.Log", e);
			android.util.Log.i(TAG, msg);
		}
	}

	public static void d(String msg, Object... args) {
		try {
			if (BuildConfig.DEBUG) 
				android.util.Log.d(TAG, String.format(msg, args));
		} catch (MissingFormatArgumentException e) {
			android.util.Log.e(TAG, "vitamio.Log", e);
			android.util.Log.d(TAG, msg);
		}
	}

	public static void e(String msg, Object... args) {
		try {
				android.util.Log.e(TAG, String.format(msg, args));
		} catch (MissingFormatArgumentException e) {
			android.util.Log.e(TAG, "vitamio.Log", e);
			android.util.Log.e(TAG, msg);
		}
	}

	public static void e(String msg, Throwable t) {
		android.util.Log.e(TAG, msg, t);
	}
}
