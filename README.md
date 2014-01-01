Vitamio
===============

Vitamio is an open multimedia framework for Android and iOS, with full and real hardware accelerated decoder and renderer.

Upgrade from 3.0
----------------

You don't need to change anything once 4.x is finally released. But if you want to use it right now, warnnings below:

1. Vitamio support RGBA\_8888 and RGB\_565 surface to render video, use RGBA\_8888 as default. if you use the VideoView from Vitamio, you can use `setVideoChroma(MediaPlayer.VIDEOCHROMA_RGB565)` to render video. Otherwise, you must add `getHolder().setFormat(PixelFormat.RGBA_8888)` to your own VideoView.

2. The dist folder include arm_x86, arm_x86_mips, online_arm, you can select difference arch copy to Vitamio.

New features
------------

Only few important features are listed here, we have fix many bugs and may introduce some new bugs.

1. The latest FFmpeg 2.0 git version, which should fix most playback issues, or bring some issues.
2. Support most FFmpeg AVOptions, which enables custom HTTP headers support.
3. Support more hardwares, e.g. X86 or MIPS.
4. Improve streaming, especially support adaptive bitrate streaming, you need open manually.
5. OpenSSL included, so some SSL related protocols, such as https, tls, rtmps, rtmpts, are supported.
6. Playback speed control from 0.5x to 2.0x.
7. Audio amplify to 2x volume.
8. Improved subtitle support, including external bitmap subtitles.
9. Cache online video to local storage and can be reused until you delete the cache file.
10. More MediaPlayer API, e.g. `getMetadata`, `getVideoTrack`.
11. The full Java code is open to all developers, modify and contribute is welcome.
12. Support RGBA\_8888 rendering, spport switching RGB\_565 or RGBA\_8888 to video rendering.
13. Enhance the hardware decoding in Android 16+.
14. Support ARMV6 CPU, may have some bugs.

How to use
----------

please see [the website](https://github.com/yixia/VitamioBundle/wiki)

License
-------

Please see [License](http://www.vitamio.org/en/License)



