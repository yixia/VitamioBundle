package io.vov.vitamio.utils;

import android.graphics.Typeface;

import io.vov.vitamio.MediaPlayer;

public class VP {
	
	public static final String SNAP_SHOT_PATH = "/Player";
	public static final String SESSION_LAST_POSITION_SUFIX = ".last";
	
	// key
	public static final String SUB_SHADOW_COLOR = "vplayer_sub_shadow_color";
	public static final String SUB_POSITION = "vplayer_sub_position";
	public static final String SUB_SIZE = "vplayer_sub_size";
	public static final String SUB_SHADOW_RADIUS = "vplayer_sub_shadow_radius";
	public static final String SUB_ENABLED = "vplayer_sub_enabled";
	public static final String SUB_SHADOW_ENABLED = "vplayer_sub_shadow_enabled";
	public static final String SUB_TEXT_KEY = "sub_text";
	public static final String SUB_PIXELS_KEY = "sub_pixels";
	public static final String SUB_WIDTH_KEY = "sub_width";
	public static final String SUB_HEIGHT_KEY = "sub_height";
	
	// default value
	public static final int DEFAULT_BUF_SIZE = 8192;
	public static final int DEFAULT_VIDEO_QUALITY = MediaPlayer.VIDEOQUALITY_MEDIUM;
	public static final boolean DEFAULT_DEINTERLACE = false;
	public static final float DEFAULT_ASPECT_RATIO = 0f;
	public static final float DEFAULT_STEREO_VOLUME = 1.0f;
	public static final String DEFAULT_META_ENCODING = "auto";
	public static final String DEFAULT_SUB_ENCODING = "auto";
	public static final int DEFAULT_SUB_STYLE = Typeface.BOLD;
	public static final int DEFAULT_SUB_COLOR = 0xffffffff;
	public static final int DEFAULT_SUB_SHADOWCOLOR = 0xff000000;
	public static final float DEFAULT_SUB_SHADOWRADIUS = 2.0f;
	public static final float DEFAULT_SUB_SIZE = 18.0f;
	public static final float DEFAULT_SUB_POS = 10.0f;
	public static final int DEFAULT_TYPEFACE_INT = 0;
	public static final boolean DEFAULT_SUB_SHOWN = true;
	public static final boolean DEFAULT_SUB_SHADOW = true;
	public static final Typeface DEFAULT_TYPEFACE = Typeface.DEFAULT;
	
	
	public static Typeface getTypeface(int type) {
		switch (type) {
		case 0:
			return Typeface.DEFAULT;
		case 1:
			return Typeface.SANS_SERIF;
		case 2:
			return Typeface.SERIF;
		case 3:
			return Typeface.MONOSPACE;
		default:
			return DEFAULT_TYPEFACE;
		}
	}
}
