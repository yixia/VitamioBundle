Vitamio 4.0-pre
===============

Vitamio 4.0 for Android is still under development, this is an early preview, which we used in VPlayer 3.1.x for Android.


Upgrade from 3.0
----------------

You don't need to change anything once 4.0 is finally released. But if you want to use it right now, two warnnings below:

1. Only ARMv7 CPU.
2. RGBA\_8888 surface is required to render video. If you use the VideoView from Vitamio, nothing to do. Otherwise, you must add `getHolder().setFormat(PixelFormat.RGBA_8888)` to your own VideoView.


New features
------------

Only few important features are listed here, we have fix many bugs and may introduce some new bugs.

1. The latest FFmpeg git version, which should fix most playback issues, or bring some issues.
2. Support most FFmpeg AVOptions, which enables custom HTTP headers support.
3. Support more hardwares, e.g. X86 or MIPS.
4. Improve streaming, especially the HLS support.
5. OpenSSL included, so some SSL related protocols, such as https, tls, rtmps, rtmpts, are supported.
6. Playback speed control from 0.5x to 2.0x.
7. Audio amplify to 2x volume.
8. Improved subtitle support, including external bitmap subtitles.
9. Cache online video to local storage and can be reused until you delete the cache file.
10. More MediaPlayer API, e.g. `getMetadata`.
11. The full Java code is open to all developers, modify and contribute is welcome.
12. Support RGBA\_8888 rendering, and increase the color conversion accuracy and speed.
13. Enhance the hardware decoding in Android 16+.
