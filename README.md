## tomahawk-android

Music is everywhere, now you don’t have to be. Tomahawk, the critically acclaimed multi-source music player, is now available on Android. Given the name of an artist, album or song Tomahawk will find the best available source and play it - whether that be from Spotify, Beats Music, Rdio, Deezer, GMusic, Grooveshark, Soundcloud, Official.fm, Jamendo, Beets, Ampache, Subsonic or your phone’s local storage.
Tomahawk for Android also syncs your history, your loved tracks, artists, albums and your playlists to/from the desktop version of Tomahawk via our new music community, Hatchet. On Hatchet you can hear your friends' favorite tracks and see what they're currently listening to.

## Beta and Nightly

Join this Google+ community to take part in our current beta test on Google Play:
https://plus.google.com/u/0/communities/107064391247577662665

Nightly builds are available here:
http://download.tomahawk-player.org/nightly/android/

## Development Setup

#### Prerequisites:

First of all you have to properly setup your Android SDK/NDK:

- Download and install the Android SDK http://developer.android.com/sdk/index.html
    - Make sure you have updated and installed the following in your Android SDK Manager:
        - "/Tools"
        - the newest Android SDK Platform folder (e.g. "/Android 4.4 (API 19)")
        - "/Extras/Android Support Repository" and "/Extras/Android Support Library"
- Download and install the Android NDK http://developer.android.com/tools/sdk/ndk/index.html
    - Make sure you setup your environment variables correctly:
        - $ANDROID_HOME should point to your Android SDK root
          (e.g. /home/maffen/android-sdk/)
        - $ANDROID_NDK_HOME should point to your Android NDK root
          (e.g. /home/maffen/android-ndk-r8e/)

Build it on the commandline with gradle:

- Simply run "./gradlew assembleDebug" for the debug build or "./gradlew assembleRelease" for
  the release build in your tomahawk-android checkout directory. The built apk will be put into
  "tomahawk-android/build/apk"

Setup using Android Studio and gradle (highly recommended):

- Open Android Studio and go to "File"->"Import Project"
- Browse to your tomahawk-android checkout and click "OK".
- Make sure that the radio-button "Use default gradle wrapper (recommended)" is selected.
- Click "next" and that's it :) tomahawk-android should compile right away

Setup using other IDEs without gradle:

- Import tomahawk-android into the IDE of your choice
- tomahawk-android depends on the several 3rd party libraries. You can look up a list of those in ./build.gradle under dependencies{...}
- Make sure you setup the support libraries correctly (http://developer.android.com/tools/support-library/setup.html)
- Add all dependencies to your tomahawk-android project
- Since libspotify is only available as a c-library, we have to use the NDK
  in order to connect the native c/c++ stuff with our Java/Android code:
    - Run the ndk-build script inside the tomahawk-android project folder before every new build
      The desired outcome shows these two lines in its console output:
      - Install        : libspotify.so => libs/armeabi/libspotify.so
      - Install        : libspotifywrapper.so => libs/armeabi/libspotifywrapper.so
- tomahawk-android should now compile successfully.

If you have any further problems, feel free to join the #tomahawk.mobile irc channel on irc.freenode.org

## Ready to contribute?

Here's a link to the bug/to-do tracker :) https://bugs.tomahawk-player.org/secure/RapidBoard.jspa?rapidView=2

## Code Style Guidelines for Contributors

In order to keep everything clean and cozy, please use the official Android code style format preset:
- https://github.com/android/platform_development/tree/master/ide
  (use the IntelliJ preset, if you're using Android Studio)

For a larger overview you should read the official Android "Code Style Guidelines for Contributors":
- http://source.android.com/source/code-style.html

## Recommended reading

- http://developer.android.com/training/basics/activity-lifecycle/index.html
- http://developer.android.com/training/basics/supporting-devices/index.html
- http://developer.android.com/training/basics/fragments/index.html

## Recommended IDE

http://developer.android.com/sdk/installing/studio.html

Since there are some glitches/bugs and performance issues with Eclipse, the new Android Studio IDE (which is based on IntelliJ) is a great alternative.
