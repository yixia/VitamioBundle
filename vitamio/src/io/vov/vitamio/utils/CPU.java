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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import android.os.Build;
import android.text.TextUtils;

public class CPU {
	private static final Map<String, String> cpuinfo = new HashMap<String, String>();
	private static int cachedFeature = -1;
	private static String cachedFeatureString = null;
	public static final int FEATURE_ARM_V5TE =  1 << 0;
	public static final int FEATURE_ARM_V6   =  1 << 1;
	public static final int FEATURE_ARM_VFP  =  1 << 2;
	public static final int FEATURE_ARM_V7A  =  1 << 3;
	public static final int FEATURE_ARM_VFPV3 = 1 << 4;
	public static final int FEATURE_ARM_NEON =  1 << 5;
	public static final int FEATURE_X86      =  1 << 6;
	public static final int FEATURE_MIPS     =  1 << 7;
	
	public static String getFeatureString() {
		getFeature();
		return cachedFeatureString;
	}

	public static int getFeature() {
		if (cachedFeature > 0)
			return getCachedFeature();

		cachedFeature = FEATURE_ARM_V5TE;

		if (cpuinfo.isEmpty()) {
			BufferedReader bis = null;
			try {
				bis = new BufferedReader(new FileReader(new File("/proc/cpuinfo")));
				String line;
				String[] pairs;
				while ((line = bis.readLine()) != null) {
					if (!line.trim().equals("")) {
						pairs = line.split(":");
						if (pairs.length > 1)
							cpuinfo.put(pairs[0].trim(), pairs[1].trim());
					}
				}
			} catch (Exception e) {
				Log.e("getCPUFeature", e);
			} finally {
				try {
					if (bis != null)
						bis.close();
				} catch (IOException e) {
					Log.e("getCPUFeature", e);
				}
			}
		}

		if (!cpuinfo.isEmpty()) {
			for (String key : cpuinfo.keySet())
				Log.d("%s:%s", key, cpuinfo.get(key));

			boolean hasARMv6 = false;
			boolean hasARMv7 = false;

			String val = cpuinfo.get("CPU architecture");
			if (!TextUtils.isEmpty(val)) {
				try {
					int i = StringUtils.convertToInt(val);
					Log.d("CPU architecture: %s", i);
					if (i >= 7) {
						hasARMv6 = true;
						hasARMv7 = true;
					} else if (i >= 6) {
						hasARMv6 = true;
						hasARMv7 = false;
					}
				} catch (NumberFormatException ex) {
					Log.e("getCPUFeature", ex);
				}
				
				val = cpuinfo.get("Processor");
				if (val != null && (val.contains("(v7l)") || val.contains("ARMv7"))) {
					hasARMv6 = true;
					hasARMv7 = true;
				}
				if (val != null && (val.contains("(v6l)") || val.contains("ARMv6"))) {
					hasARMv6 = true;
					hasARMv7 = false;
				}

				if (hasARMv6)
					cachedFeature |= FEATURE_ARM_V6;
				if (hasARMv7)
					cachedFeature |= FEATURE_ARM_V7A;

				val = cpuinfo.get("Features");
				if (val != null) {
					if (val.contains("neon"))
						cachedFeature |= FEATURE_ARM_VFP | FEATURE_ARM_VFPV3 | FEATURE_ARM_NEON;
					else if (val.contains("vfpv3"))
						cachedFeature |= FEATURE_ARM_VFP | FEATURE_ARM_VFPV3;
					else if (val.contains("vfp"))
						cachedFeature |= FEATURE_ARM_VFP;
				}
			} else {
					String vendor_id = cpuinfo.get("vendor_id");
					String mips = cpuinfo.get("cpu model");
				 if (!TextUtils.isEmpty(vendor_id) && vendor_id.contains("GenuineIntel")) {
					 cachedFeature |= FEATURE_X86;
				 } else if (!TextUtils.isEmpty(mips) && mips.contains("MIPS")) {
					 cachedFeature |= FEATURE_MIPS;
				 }
			} 
		}

		return getCachedFeature();
	}

	private static int getCachedFeature() {
		if (cachedFeatureString == null) {
			StringBuffer sb = new StringBuffer();
			if ((cachedFeature & FEATURE_ARM_V5TE) > 0)
				sb.append("V5TE ");
			if ((cachedFeature & FEATURE_ARM_V6) > 0)
				sb.append("V6 ");
			if ((cachedFeature & FEATURE_ARM_VFP) > 0)
				sb.append("VFP ");
			if ((cachedFeature & FEATURE_ARM_V7A) > 0)
				sb.append("V7A ");
			if ((cachedFeature & FEATURE_ARM_VFPV3) > 0)
				sb.append("VFPV3 ");
			if ((cachedFeature & FEATURE_ARM_NEON) > 0)
				sb.append("NEON ");
			if ((cachedFeature & FEATURE_X86) > 0)
				sb.append("X86 ");
			if ((cachedFeature & FEATURE_MIPS) > 0)
				sb.append("MIPS ");
			cachedFeatureString = sb.toString();
		}
		Log.d("GET CPU FATURE: %s", cachedFeatureString);
		return cachedFeature;
	}

	public static boolean isDroidXDroid2() {
		return (Build.MODEL.trim().equalsIgnoreCase("DROIDX") || Build.MODEL.trim().equalsIgnoreCase("DROID2") || Build.FINGERPRINT.toLowerCase().contains("shadow") || Build.FINGERPRINT.toLowerCase().contains("droid2"));
	}
}
