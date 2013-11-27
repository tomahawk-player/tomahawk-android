tomahawk-android
================
Tomahawk's Android Music Player

Nightly
================
Nightly builds are available here:
http://download.tomahawk-player.org/nightly/android/

Setup
================
    Prerequisites:
    - First of all you have to properly setup your Android SDK/NDK:
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

    Setup using other IDEs without Gradle:
    - Import tomahawk-android into the IDE of your choice
    - tomahawk-android depends on the following jar libraries:
        - acra-4.5.0.jar
        - stickylistheaders-77cf3c.jar
        - android.support.v4.jar
    - In addition tomahawk-android uses the library project "Android Support v7 appcompat library"
    - Make sure you setup the support libraries correctly
      (http://developer.android.com/tools/support-library/setup.html)
    - Add all dependencies to your tomahawk-android project
    - Since libspotify is only available as a c-library, we have to use the NDK
      in order to connect the native c/c++ stuff with our Java/Android code:
        - Run the ndk-build script inside the tomahawk-android project folder before every new build
          The desired outcome shows these two lines in its console output:
          - Install        : libspotify.so => libs/armeabi/libspotify.so
          - Install        : libspotifywrapper.so => libs/armeabi/libspotifywrapper.so
    - tomahawk-android should now compile successfully.

    If you have any further problems, feel free to join the #tomahawk.mobile irc channel on
    irc.freenode.org

Ready to contribute?
Here's the to-do list :)
================
https://trello.com/board/tomahawk-android/500c1f61aa1ffaae1b027ba1

Code Style Guidelines for Contributors
================
In order to keep everything clean and cozy, please use the official Android code style format preset
    - https://github.com/android/platform_development/tree/master/ide
    (use the IntelliJ preset, if you're using Android Studio)

For a larger overview you should read the official Android "Code Style Guidelines for Contributors"
    - http://source.android.com/source/code-style.html

Recommended reading
================
 - http://developer.android.com/training/basics/activity-lifecycle/index.html
 - http://developer.android.com/training/basics/supporting-devices/index.html
 - http://developer.android.com/training/basics/fragments/index.html

Recommended IDE
================
http://developer.android.com/sdk/installing/studio.html

Since there are some glitches/bugs and performance issues with Eclipse, the new Android Studio IDE
(which is based on IntelliJ) is a great alternative.

