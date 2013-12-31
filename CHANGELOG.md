Change Log
==========

version 4.2.0 *(2013-12-31)*
---------------------------

 * Support MIPS architecture.
 * Customize FFmpeg, You can build FFmpeg by yourself.
 * Improve hardware decoder, support more devices.
 * Support HTTP Live Streaming download while playing.
 * Support AES-128 encrypt for HTTP Live Streaming.
 * Improve seek operation.
 * Fixed many bugs.


version 4.1.1 *(2013-09-16)*
---------------------------

 * improve hls playback.
 * videoview support avoptions.
 * support videoview switching rgb\_565 or rgba\_8888 to video rendering.
 * fix other bug.

Version 4.1.0 *(2013-08-30)*
---------------------------

 * Support full version, online version.
 * Support x86 hardware.
 * Improve hardware decoder.
 * Improve adaptive bitrate streaming.
 * Support most FFmpeg AVOptions, e.g. support rtmp AVOptions.
 * Improve MediaPlayer API, e.g. support playback segment video list.

Version 4.0.1 *(2013-07-22)*
---------------------------

 * Refactor `VitamioBundle`, dont need `ZI`.
 * Fix `MediaMetadataRetriever` API bug.
 * Fix other bug.

Version 4.0.0 *(2013-07-15)*
----------------------------

 * The latest FFmpeg 2.0 git version, which should fix most playback issues, or bring some issues.
 * Support most FFmpeg AVOptions, which enables custom HTTP headers support.
 * Support more hardwares, e.g. X86 or MIPS.
 * Improve streaming, especially support adaptive bitrate streaming, you need open manually.
 * OpenSSL included, so some SSL related protocols, such as https, tls, rtmps, rtmpts, are supported.
 * Playback speed control from 0.5x to 2.0x.
 * Audio amplify to 2x volume.
 * Improved subtitle support, including external bitmap subtitles.
 * Cache online video to local storage and can be reused until you delete the cache file.
 * More MediaPlayer API, e.g. `getMetadata`, `getVideoTrack`.
 * The full Java code is open to all developers, modify and contribute is welcome.
 * Support RGBA\_8888 rendering, spport switching RGB\_565 or RGBA\_8888 to video rendering.
 * Enhance the hardware decoding in Android 16+.
 * Support ARMV6 CPU, may have some bugs.


Version 3.0.0 *(2012-10-23)*
----------------------------

 * Support ARMV6+.
 * Support package it into your application without the need to
   let users download a seperated Vitamio Plugin.
