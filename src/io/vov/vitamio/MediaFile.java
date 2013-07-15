/*
 * Copyright (C) 2006 The Android Open Source Project
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

import java.util.HashMap;
import java.util.Iterator;


public class MediaFile {
	protected final static String sFileExtensions;

	public static final int FILE_TYPE_MP3 = 1;
	public static final int FILE_TYPE_M4A = 2;
	public static final int FILE_TYPE_WAV = 3;
	public static final int FILE_TYPE_AMR = 4;
	public static final int FILE_TYPE_AWB = 5;
	public static final int FILE_TYPE_WMA = 6;
	public static final int FILE_TYPE_OGG = 7;
	public static final int FILE_TYPE_AAC = 8;
	public static final int FILE_TYPE_MKA = 9;
	public static final int FILE_TYPE_MID = 10;
	public static final int FILE_TYPE_SMF = 11;
	public static final int FILE_TYPE_IMY = 12;
	public static final int FILE_TYPE_APE = 13;
	public static final int FILE_TYPE_FLAC = 14;
	private static final int FIRST_AUDIO_FILE_TYPE = FILE_TYPE_MP3;
	private static final int LAST_AUDIO_FILE_TYPE = FILE_TYPE_FLAC;

	public static final int FILE_TYPE_MP4 = 701;
	public static final int FILE_TYPE_M4V = 702;
	public static final int FILE_TYPE_3GPP = 703;
	public static final int FILE_TYPE_3GPP2 = 704;
	public static final int FILE_TYPE_WMV = 705;
	public static final int FILE_TYPE_ASF = 706;
	public static final int FILE_TYPE_MKV = 707;
	public static final int FILE_TYPE_MP2TS = 708;
	public static final int FILE_TYPE_FLV = 709;
	public static final int FILE_TYPE_MOV = 710;
	public static final int FILE_TYPE_RM = 711;
	public static final int FILE_TYPE_DVD = 712;
	public static final int FILE_TYPE_DIVX = 713;
	public static final int FILE_TYPE_OGV = 714;
	public static final int FILE_TYPE_VIVO = 715;
	public static final int FILE_TYPE_WTV = 716;
	public static final int FILE_TYPE_AVS = 717;
	public static final int FILE_TYPE_SWF = 718;
	public static final int FILE_TYPE_RAW = 719;
	private static final int FIRST_VIDEO_FILE_TYPE = FILE_TYPE_MP4;
	private static final int LAST_VIDEO_FILE_TYPE = FILE_TYPE_RAW;

	protected static class MediaFileType {
		int fileType;
		String mimeType;

		MediaFileType(int fileType, String mimeType) {
			this.fileType = fileType;
			this.mimeType = mimeType;
		}
	}

	private static HashMap<String, MediaFileType> sFileTypeMap = new HashMap<String, MediaFileType>();
	private static HashMap<String, Integer> sMimeTypeMap = new HashMap<String, Integer>();

	static void addFileType(String extension, int fileType, String mimeType) {
		sFileTypeMap.put(extension, new MediaFileType(fileType, mimeType));
		sMimeTypeMap.put(mimeType, Integer.valueOf(fileType));
	}

	static {
		// addFileType("MP3", FILE_TYPE_MP3, "audio/mpeg");
		// addFileType("M4A", FILE_TYPE_M4A, "audio/mp4");
		// addFileType("WAV", FILE_TYPE_WAV, "audio/x-wav");
		// addFileType("AMR", FILE_TYPE_AMR, "audio/amr");
		// addFileType("AWB", FILE_TYPE_AWB, "audio/amr-wb");
		// addFileType("WMA", FILE_TYPE_WMA, "audio/x-ms-wma");
		// addFileType("OGG", FILE_TYPE_OGG, "application/ogg");
		// addFileType("OGA", FILE_TYPE_OGG, "application/ogg");
		// addFileType("AAC", FILE_TYPE_AAC, "audio/aac");
		// addFileType("MKA", FILE_TYPE_MKA, "audio/x-matroska");
		// addFileType("MID", FILE_TYPE_MID, "audio/midi");
		// addFileType("MIDI", FILE_TYPE_MID, "audio/midi");
		// addFileType("XMF", FILE_TYPE_MID, "audio/midi");
		// addFileType("RTTTL", FILE_TYPE_MID, "audio/midi");
		// addFileType("SMF", FILE_TYPE_SMF, "audio/sp-midi");
		// addFileType("IMY", FILE_TYPE_IMY, "audio/imelody");
		// addFileType("RTX", FILE_TYPE_MID, "audio/midi");
		// addFileType("OTA", FILE_TYPE_MID, "audio/midi");
		// addFileType("APE", FILE_TYPE_APE, "audio/x-ape");
		// addFileType("FLAC", FILE_TYPE_FLAC, "audio/flac");

		addFileType("M1V", FILE_TYPE_MP4, "video/mpeg");
		addFileType("MP2", FILE_TYPE_MP4, "video/mpeg");
		addFileType("MPE", FILE_TYPE_MP4, "video/mpeg");
		addFileType("MPG", FILE_TYPE_MP4, "video/mpeg");
		addFileType("MPEG", FILE_TYPE_MP4, "video/mpeg");
		addFileType("MP4", FILE_TYPE_MP4, "video/mp4");
		addFileType("M4V", FILE_TYPE_M4V, "video/mp4");
		addFileType("3GP", FILE_TYPE_3GPP, "video/3gpp");
		addFileType("3GPP", FILE_TYPE_3GPP, "video/3gpp");
		addFileType("3G2", FILE_TYPE_3GPP2, "video/3gpp2");
		addFileType("3GPP2", FILE_TYPE_3GPP2, "video/3gpp2");
		addFileType("MKV", FILE_TYPE_MKV, "video/x-matroska");
		addFileType("WEBM", FILE_TYPE_MKV, "video/x-matroska");
		addFileType("MTS", FILE_TYPE_MP2TS, "video/mp2ts");
		addFileType("TS", FILE_TYPE_MP2TS, "video/mp2ts");
		addFileType("TP", FILE_TYPE_MP2TS, "video/mp2ts");
		addFileType("WMV", FILE_TYPE_WMV, "video/x-ms-wmv");
		addFileType("ASF", FILE_TYPE_ASF, "video/x-ms-asf");
		addFileType("ASX", FILE_TYPE_ASF, "video/x-ms-asf");
		addFileType("FLV", FILE_TYPE_FLV, "video/x-flv");
		addFileType("F4V", FILE_TYPE_FLV, "video/x-flv");
		addFileType("HLV", FILE_TYPE_FLV, "video/x-flv");
		addFileType("MOV", FILE_TYPE_MOV, "video/quicktime");
		addFileType("QT", FILE_TYPE_MOV, "video/quicktime");
		addFileType("RM", FILE_TYPE_RM, "video/x-pn-realvideo");
		addFileType("RMVB", FILE_TYPE_RM, "video/x-pn-realvideo");
		addFileType("VOB", FILE_TYPE_DVD, "video/dvd");
		addFileType("DAT", FILE_TYPE_DVD, "video/dvd");
		addFileType("AVI", FILE_TYPE_DIVX, "video/x-divx");
		addFileType("OGV", FILE_TYPE_OGV, "video/ogg");
		addFileType("OGG", FILE_TYPE_OGV, "video/ogg");
		addFileType("VIV", FILE_TYPE_VIVO, "video/vnd.vivo");
		addFileType("VIVO", FILE_TYPE_VIVO, "video/vnd.vivo");
		addFileType("WTV", FILE_TYPE_WTV, "video/wtv");
		addFileType("AVS", FILE_TYPE_AVS, "video/avs-video");
		addFileType("SWF", FILE_TYPE_SWF, "video/x-shockwave-flash");
		addFileType("YUV", FILE_TYPE_RAW, "video/x-raw-yuv");

		StringBuilder builder = new StringBuilder();
		Iterator<String> iterator = sFileTypeMap.keySet().iterator();

		while (iterator.hasNext()) {
			if (builder.length() > 0)
				builder.append(',');
			builder.append(iterator.next());
		}
		sFileExtensions = builder.toString();
	}

	public static boolean isAudioFileType(int fileType) {
		return (fileType >= FIRST_AUDIO_FILE_TYPE && fileType <= LAST_AUDIO_FILE_TYPE);
	}

	public static boolean isVideoFileType(int fileType) {
		return (fileType >= FIRST_VIDEO_FILE_TYPE && fileType <= LAST_VIDEO_FILE_TYPE);
	}

	public static MediaFileType getFileType(String path) {
		int lastDot = path.lastIndexOf(".");
		if (lastDot < 0)
			return null;
		return sFileTypeMap.get(path.substring(lastDot + 1).toUpperCase());
	}

	public static int getFileTypeForMimeType(String mimeType) {
		Integer value = sMimeTypeMap.get(mimeType);
		return (value == null ? 0 : value.intValue());
	}

}