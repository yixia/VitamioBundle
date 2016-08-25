Vitamio
===============

Vitamio is an open multimedia framework for Android and iOS, with full and real hardware accelerated decoder and renderer.


New features
------------

Only a few important features are listed here. We have fixed many bugs while it may introduce some new bugs.

1. The latest FFmpeg 2.0 git version, which should fix most playback issues, but may bring some new issues.
2. Support most FFmpeg AVOptions, which enable custom HTTP headers support.
3. Support more hardwares, e.g. X86 or MIPS.
4. Improve streaming, especially support adaptive bitrate streaming. You need enable this feature manually.
5. OpenSSL is now included, so some SSL related protocols, such as https, tls, rtmps and rtmpts, are noew supported.
6. Playback speed control from 0.5x to 2.0x.
7. Audio amplify to 2x volume.
8. Improved subtitle support, including external bitmap subtitles.
9. Cache online video to local storage and can be reused until you delete the cache file.
10. More MediaPlayer APIs, e.g. `getMetadata`, `getVideoTrack`.
11. The full Java code is open to all developers, modification and contribution are welcome.
12. Support RGBA\_8888 rendering, spport switching RGB\_565 or RGBA\_8888 to video rendering.
13. Enhance the hardware decoding in Android 16+.
14. Support ARMV6 CPU, but it may have some bugs.

How to use
----------

Please see [the website](https://github.com/yixia/VitamioBundle/wiki)

License
-------

Please see [License](http://www.vitamio.org/en/License)


## Google+
Vitamio Developers Community on Google+ [http://goo.gl/fhCDTD](http://goo.gl/fhCDTD)
