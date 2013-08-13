/*
 * Copyright (C) 2013 yixia.com
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
package io.vov.vitamio.demo;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import io.vov.vitamio.MediaMetadataRetriever;

public class MediaMetadataRetrieverDemo extends Activity {

	private String path = "";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		io.vov.vitamio.MediaMetadataRetriever retriever = new io.vov.vitamio.MediaMetadataRetriever(this);
		try {
			path = "";
			if (path == "") {
				// Tell the user to provide an audio file URL.
				Toast.makeText(MediaMetadataRetrieverDemo.this, "Please edit MediaMetadataRetrieverDemo Activity, " + "and set the path variable to your audio file path." + " Your audio file must be stored on sdcard.", Toast.LENGTH_LONG).show();
				return;
			}
			retriever.setDataSource(path);
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalStateException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		long durationMs = Long.parseLong(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
		String artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		String title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		
		setContentView(R.layout.media_metadata);
		TextView textView = (TextView)findViewById(R.id.textView);
		textView.setText(durationMs + "" + artist + title);
		
	}
}
