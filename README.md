tomahawk-android
================

Tomahawk's Android Music Player

Nightly
================
Nightly builds are available here:
http://download.tomahawk-player.org/nightly/android/

Setup
================
    - Open Eclipse and go to "File"->"Import"
    - Under Android/ select "Existing Android Code into Workspace."
    - Browse to your tomahawk-android checkout.
    - Two projects will appear in the import dialog. Import them both.
      One is the app and one is the unit tests.
    - Right click on "tomahawk-android-test" and select "Properties". Now 
      select "Java Build Path" and the tab "Projects". Click on "Add" and
      choose "tomahawk-android". Finish by clicking "OK".
      (The junit tests are not up to date. If they cause you any troubles,
      feel free to exclude them from the build)
    - tomahawk-android depends on the following jars in its "libs" folder:
        - acra-4.5.0.jar
        - stickylistheaders-77cf3c.jar
    - tomahawk-android requires the following library projects:
        - "ActionBarSherlock" git://github.com/mrmaffen/ActionBarSherlock.git
        - "SlidingMenu"       git://github.com/mrmaffen/SlidingMenu.git
    - Do the following steps for each library project:
        - "File" -> "Import" -> "Android" -> "Existing Android Code into Workspace"
        - Choose the "library" subfolder in the SlidingMenu project folder or
          the "actionbarsherlock" subfolder in the ActionBarSherlock folder as your "Root Directory".
        - Check "copy projects into workspace" and click "Finish".
        - Now add the just created library project to tomahawk-android by
          right-clicking your "tomahawk-android" project and selecting "Properties"
        - Select "Android" and add the library by clicking "Add...".
        - To finish the process, choose your  library project and click "OK".
    - Since libspotify is only available as a c-library, we have to use the NDK
      in order to connect the native c/c++ stuff with our Java/Android code:
        - The first step is to download the latest Android NDK here:
          http://developer.android.com/tools/sdk/ndk/index.html
        - Set everything up according to the official how-to
        - Now run the ndk-build script inside the tomahawk-android project folder.
          The desired outcome shows these two lines in its console output:
          - Install        : libspotify.so => libs/armeabi/libspotify.so
          - Install        : libspotifywrapper.so => libs/armeabi/libspotifywrapper.so
    - tomahawk-android should now compile successfully. If you have any further problems,
      feel free to join the #tomahawk.mobile irc channel on irc.freenode.org

    Notes:
        - There is a known issue when importing. The primary app name
          ends up being "org.tomahawk.tomahawk_android.TomahawkMainActivity".
          Right click on the project and go to "Refactor"->"Rename". Rename
          the project to "tomahawk-android" and this should fix any errors.
        - If you have troubles building ActionBarSherlock, confirm that you have android-14 installed
          in the sdk. This version is needed to build ActionBarSherlock, but you should use
          the latest version to build Tomahawk-Android.
        - If you have other build problems, confirm that your Java Compiler is set to v1.6. 
          ( in eclipse, go to tomahawk-android ( right click ) -> Properties -> Java Compiler -> 
          Compiler compliance level -> 1.6 )
        - Make sure that you don't tick the "Is Library" box in Properties->Android || Library in your
          tomahawk-android project. Only ActionBarSherlock is needed as a library project.
        - It is also good to add the sdk to your path.

Ready to contribute? Here's the to-do list :)
================
https://trello.com/board/tomahawk-android/500c1f61aa1ffaae1b027ba1

Code Style Guidelines for Contributors
================
In order to keep everything clean and cozy, please use the official android code style format preset:
    - https://github.com/android/platform_development/tree/master/ide

For a larger overview you could read the official android "Code Style Guidelines for Contributors":
    - http://source.android.com/source/code-style.html


Recommended reading
================
 - http://developer.android.com/training/basics/activity-lifecycle/index.html
 - http://developer.android.com/training/basics/supporting-devices/index.html
 - http://developer.android.com/training/basics/fragments/index.html

Recommended IDE
================
Since there are some glitches/bugs and performance issues with Eclipse, you should check out
IntelliJ IDEA (http://www.jetbrains.com/idea/), which is basically Eclipse done better.
Since IntelliJ IDEA 12 there's also great integration with the Android SDK.
Also the new Android Studio IDE (which is based on IntelliJ) is a great alternative to Eclipse.
http://developer.android.com/sdk/installing/studio.html

