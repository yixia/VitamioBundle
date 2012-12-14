package io.vov.vitamio.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;

import me.abitno.utils.IntentHelper;

import io.vov.vitamio.MediaPlayer;
import io.vov.vitamio.R;
import io.vov.vitamio.widget.MediaController;
import io.vov.vitamio.widget.VideoView;

public class VideoActivity extends Activity {

	private VideoView mVideoView;
	private Uri mUri;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (!io.vov.vitamio.LibsChecker.checkVitamioLibs(this))
			return;
		super.onCreate(savedInstanceState);
		setVolumeControlStream(AudioManager.STREAM_MUSIC);
		setContentView(R.layout.activity_video);
		parseIntent(getIntent());
	}

	private void parseIntent(Intent intent) {
		mUri = IntentHelper.getIntentUri(intent);
		mVideoView = (VideoView) findViewById(R.id.video);
		mVideoView.setVideoURI(mUri);
		mVideoView.setVideoTitle(intent.getStringExtra("displayName"));
		mVideoView.setVideoQuality(MediaPlayer.VIDEOQUALITY_HIGH);
		mVideoView.setMediaController(new MediaController(this));
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (mVideoView != null)
			mVideoView.setVideoLayout(VideoView.VIDEO_LAYOUT_SCALE, 0);
		super.onConfigurationChanged(newConfig);
	}

	private void resultFinish() {
		this.finish();
	}
}
