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
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import io.vov.vitamio.LibsChecker;
import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.MediaPlayer.OnTimedTextListener;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class VideoViewSubtitle extends Activity {

	private String path = "";
	private String subtitle_path = "";
	private VideoView mVideoView;
	private TextView mSubtitleView;
	private long mPosition = 0;
	private int mVideoLayout = 0;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		if (!LibsChecker.checkVitamioLibs(this))
			return;
		setContentView(R.layout.subtitle2);
		mVideoView = (VideoView) findViewById(R.id.surface_view);
		mSubtitleView = (TextView) findViewById(R.id.subtitle_view);

		if (path == "") {
			// Tell the user to provide a media file URL/path.
			Toast.makeText(VideoViewSubtitle.this, "Please edit VideoViewSubtitle Activity, and set path" + " variable to your media file URL/path", Toast.LENGTH_LONG).show();
			return;
		} else {
			/*
			 * Alternatively,for streaming media you can use
			 * mVideoView.setVideoURI(Uri.parse(URLstring));
			 */
			mVideoView.setVideoPath(path);

			mVideoView.setMediaController(new MediaController(this));
			mVideoView.requestFocus();

			mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
				@Override
				public void onPrepared(MediaPlayer mediaPlayer) {
					// optional need Vitamio 4.0
					mediaPlayer.setPlaybackSpeed(1.0f);
					mVideoView.addTimedTextSource(subtitle_path);
					mVideoView.setTimedTextShown(true);

				}
			});
			mVideoView.setOnTimedTextListener(new OnTimedTextListener() {

				@Override
				public void onTimedText(String text) {
					mSubtitleView.setText(text);
				}

				@Override
				public void onTimedTextUpdate(byte[] pixels, int width, int height) {

				}
			});
		}
	}

	@Override
	protected void onPause() {
		mPosition = mVideoView.getCurrentPosition();
		mVideoView.stopPlayback();
		super.onPause();
	}

	@Override
	protected void onResume() {
		if (mPosition > 0) {
			mVideoView.seekTo(mPosition);
			mPosition = 0;
		}
		super.onResume();
		mVideoView.start();
	}

	public void changeLayout(View view) {
		mVideoLayout++;
		if (mVideoLayout == 4) {
			mVideoLayout = 0;
		}
		switch (mVideoLayout) {
		case 0:
			mVideoLayout = VideoView.VIDEO_LAYOUT_ORIGIN;
			view.setBackgroundResource(R.drawable.mediacontroller_sreen_size_100);
			break;
		case 1:
			mVideoLayout = VideoView.VIDEO_LAYOUT_SCALE;
			view.setBackgroundResource(R.drawable.mediacontroller_screen_fit);
			break;
		case 2:
			mVideoLayout = VideoView.VIDEO_LAYOUT_STRETCH;
			view.setBackgroundResource(R.drawable.mediacontroller_screen_size);
			break;
		case 3:
			mVideoLayout = VideoView.VIDEO_LAYOUT_ZOOM;
			view.setBackgroundResource(R.drawable.mediacontroller_sreen_size_crop);

			break;
		}
		mVideoView.setVideoLayout(mVideoLayout, 0);
	}

}
