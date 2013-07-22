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

import android.app.Activity;
import android.content.Intent;

/**
 * LibsChecker is a wrapper of {@link Vitamio}, it helps to initialize Vitamio
 * easily.
 * <p/>
 * <pre>
 * public void onCreate(Bundle b) {
 * 	super.onCreate(b);
 * 	if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
 * 		return;
 *
 * 	// Code using Vitamio should go below {@link LibsChecker#checkVitamioLibs}
 * }
 * </pre>
 */
public final class LibsChecker {
  public static final String FROM_ME = "fromVitamioInitActivity";

  public static final boolean checkVitamioLibs(Activity ctx) {
    if (!Vitamio.isInitialized(ctx) && !ctx.getIntent().getBooleanExtra(FROM_ME, false)) {
      Intent i = new Intent();
      i.setClassName(Vitamio.getVitamioPackage(), "io.vov.vitamio.activity.InitActivity");
      i.putExtras(ctx.getIntent());
      i.setData(ctx.getIntent().getData());
      i.putExtra("package", ctx.getPackageName());
      i.putExtra("className", ctx.getClass().getName());
      ctx.startActivity(i);
      ctx.finish();
      return false;
    }
    return true;
  }
}
