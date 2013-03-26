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

package io.vov.vitamio.activity;

import io.vov.vitamio.R;
import io.vov.vitamio.Vitamio;

import java.lang.ref.WeakReference;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.WindowManager;

public class InitActivity extends Activity {
	public static final String FROM_ME = "fromVitamioInitActivity";
	private ProgressDialog mPD;
	private UIHandler uiHandler;

	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		uiHandler = new UIHandler(this);

		new AsyncTask<Object, Object, Boolean>() {
			@Override
			protected void onPreExecute() {
				mPD = new ProgressDialog(InitActivity.this);
				mPD.setCancelable(false);
				mPD.setMessage(getString(R.string.vitamio_init_decoders));
				mPD.show();
			}

			@Override
			protected Boolean doInBackground(Object... params) {
				return Vitamio.initialize(InitActivity.this);
			}

			@Override
			protected void onPostExecute(Boolean inited) {
				if (inited) {
					uiHandler.sendEmptyMessage(0);
				}
			}

		}.execute();
	}

	private static class UIHandler extends Handler {
		private WeakReference<Context> mContext;

		public UIHandler(Context c) {
			mContext = new WeakReference<Context>(c);
		}

		public void handleMessage(Message msg) {
			InitActivity ctx = (InitActivity) mContext.get();
			switch (msg.what) {
			case 0:
				ctx.mPD.dismiss();
				Intent src = ctx.getIntent();
				Intent i = new Intent();
				i.setClassName(src.getStringExtra("package"), src.getStringExtra("className"));
				i.setData(src.getData());
				i.putExtras(src);
				i.putExtra(FROM_ME, true);
				ctx.startActivity(i);
				ctx.finish();
				break;
			}
		}
	};
}
