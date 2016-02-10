## tomahawk-android

*Music is everywhere, now you don’t have to be!*

Tomahawk, the critically acclaimed multi-source music player, is now available on Android. Given the name of an artist, album or song Tomahawk will find the best available source and play it - whether that be from Spotify, Deezer, GMusic, Soundcloud, Tidal, Official.fm, Jamendo, Beets, Ampache, Subsonic or your phone’s local storage.
Tomahawk for Android also syncs your history, your loved tracks, artists, albums and your playlists to/from the desktop version of Tomahawk via our new music community, Hatchet. On Hatchet you can hear your friends' favorite tracks and see what they're currently listening to.

![Tomahawk Screenshot1](/screenshots/screenshot1.png) | ![Tomahawk Screenshot2](/screenshots/screenshot2.png) | ![Tomahawk Screenshot3](/screenshots/screenshot3.png)
------ | -----  | -----

## Beta and Nightly

Get the Beta version on Google Play:
https://play.google.com/store/apps/details?id=org.tomahawk.tomahawk_android

Nightly builds are available here:
http://download.tomahawk-player.org/nightly/android/?C=M;O=D

## Development Setup

First of all you have to properly setup your Android SDK/NDK:

- Download and install the Android SDK http://developer.android.com/sdk/index.html
    - Make sure you have updated and installed the following in your Android SDK Manager:
        - "/Tools"
        - the latest Android SDK Platform folder (e.g. "/Android 6.0 (API 23)")
        - "/Extras/Android Support Repository" and "/Extras/Android Support Library"    
        - "/Extras/Google Play Services" and "/Extras/Google Repository"

Build it on the commandline with gradle:

- Simply run "./gradlew assembleDebug" for the debug build or "./gradlew assembleRelease" for
  the release build in your tomahawk-android checkout directory. The built apk will be put into
  "tomahawk-android/build/outputs/apk"

Setup using Android Studio and gradle (highly recommended):

- Open Android Studio and go to "File"->"Import Project"
- Browse to your tomahawk-android checkout and click "OK".
- Make sure that the radio-button "Use default gradle wrapper (recommended)" is selected.
- Click "next" and that's it :) tomahawk-android should compile right away

Setup using other IDEs without gradle:

- Import tomahawk-android into the IDE of your choice
- tomahawk-android depends on several 3rd party libraries. You can look up a list of all dependencies in ./app/build.gradle under dependencies{...}
- Make sure you setup the support libraries correctly (http://developer.android.com/tools/support-library/setup.html)
- Add all dependencies to your tomahawk-android project
- tomahawk-android should now compile successfully.

If you have any further problems, feel free to join the #tomahawk.mobile irc channel on irc.freenode.org

## Ready to contribute?

Drop us an e-mail at welisten@tomahawk-player.org or join our IRC Channel #tomahawk.mobile on irc.freenode.org

## Code Style Guidelines for Contributors

In order to keep everything clean and cozy, please use the official Android code style format preset:
- https://github.com/android/platform_development/tree/master/ide
  (use the IntelliJ preset, if you're using Android Studio)

For a larger overview you should read the official Android "Code Style Guidelines for Contributors":
- http://source.android.com/source/code-style.html

## Plugin Apps Source Code

[Spotify Plugin App](https://github.com/tomahawk-player/tomahawk-android-spotify)     
[Deezer Plugin App](https://github.com/tomahawk-player/tomahawk-android-deezer)