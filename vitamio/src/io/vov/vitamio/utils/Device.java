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

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;

import java.util.Locale;

public class Device {
  public static String getLocale() {
    Locale locale = Locale.getDefault();
    if (locale != null) {
      String lo = locale.getLanguage();
      Log.i("getLocale " + lo);
      if (lo != null) {
        return lo.toLowerCase();
      }
    }
    return "en";
  }

  public static String getDeviceFeatures(Context ctx) {
    return getIdentifiers(ctx) + getSystemFeatures() + getScreenFeatures(ctx);
  }
  
  @SuppressLint("NewApi")
  public static String getIdentifiers(Context ctx) {
    StringBuilder sb = new StringBuilder();
    sb.append(getPair("serial", Build.SERIAL));
    sb.append(getPair("android_id", Settings.Secure.getString(ctx.getContentResolver(), Settings.Secure.ANDROID_ID)));
    TelephonyManager tel = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
    sb.append(getPair("sim_country_iso", tel.getSimCountryIso()));
    sb.append(getPair("network_operator_name", tel.getNetworkOperatorName()));
    sb.append(getPair("unique_id", Crypto.md5(sb.toString())));
    return sb.toString();
  }

  public static String getSystemFeatures() {
    StringBuilder sb = new StringBuilder();
    sb.append(getPair("android_release", Build.VERSION.RELEASE));
    sb.append(getPair("android_sdk_int", "" + Build.VERSION.SDK_INT));
    sb.append(getPair("device_cpu_abi", Build.CPU_ABI));
    sb.append(getPair("device_model", Build.MODEL));
    sb.append(getPair("device_manufacturer", Build.MANUFACTURER));
    sb.append(getPair("device_board", Build.BOARD));
    sb.append(getPair("device_fingerprint", Build.FINGERPRINT));
    sb.append(getPair("device_cpu_feature", CPU.getFeatureString()));
    return sb.toString();
  }

  public static String getScreenFeatures(Context ctx) {
    StringBuilder sb = new StringBuilder();
    DisplayMetrics disp = ctx.getResources().getDisplayMetrics();
    sb.append(getPair("screen_density", "" + disp.density));
    sb.append(getPair("screen_density_dpi", "" + disp.densityDpi));
    sb.append(getPair("screen_height_pixels", "" + disp.heightPixels));
    sb.append(getPair("screen_width_pixels", "" + disp.widthPixels));
    sb.append(getPair("screen_scaled_density", "" + disp.scaledDensity));
    sb.append(getPair("screen_xdpi", "" + disp.xdpi));
    sb.append(getPair("screen_ydpi", "" + disp.ydpi));
    return sb.toString();
  }

  private static String getPair(String key, String value) {
    key = key == null ? "" : key.trim();
    value = value == null ? "" : value.trim();
    return "&" + key + "=" + value;
  }
}
