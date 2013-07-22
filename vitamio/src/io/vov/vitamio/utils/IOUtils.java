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

import android.database.Cursor;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.Closeable;

public class IOUtils {
	
	private static final String TAG = "IOUtils";
	
	public static void closeSilently(Closeable c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (Throwable t) {
			Log.w(TAG, "fail to close", t);
		}
	}

	public static void closeSilently(ParcelFileDescriptor c) {
		if (c == null)
			return;
		try {
			c.close();
		} catch (Throwable t) {
			Log.w(TAG, "fail to close", t);
		}
	}
	
	public static void closeSilently(Cursor cursor) {
    try {
    	if (cursor != null) cursor.close();
     } catch (Throwable t) {
    	 Log.w(TAG, "fail to close", t);
     }
	 }
}
