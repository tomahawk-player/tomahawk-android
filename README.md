tomahawk-android
================

Tomahawk's Android Music Player

Nightly
================
Nightly builds are available here:
http://download.tomahawk-player.org/nightly/android/

setup
================

    - Open Eclipse and go to "File"->"Import"
    - Under Android/ select "Existing Android Code into Workspace."
    - Browse to your tomahawk-android checkout.
    - Two projects will appear in the import dialog. Import them both.
      One is the app and one is the unit tests.
    - Right click on "tomahawk-android-test" and select "Properties". Now 
      select "Java Build Path" and the tab "Projects". Click on "Add" and
      choose "tomahawk-android". Finish by clicking "OK".
    - tomahawk-android requires the third-party support library
      "ActionBarSherlock". Download and extract the library:
        - https://github.com/JakeWharton/ActionBarSherlock/zipball/4.1.0
    - Now add it as an "Android Project" to your workspace: 
        - "File"-> "Import" -> "Android" -> "Existing Android Code into Workspace"
        - Go into the folder you've extracted your downloaded zip-file to and
          choose the "library" folder as your "Root Directory".
        - Check "copy projects into workspace" and click "Finish".
    - Since the 4.1.0 release of ActionBarSherlock does include an outdated copy
      of the android support package v4, you'll need to update that manually by
      doing the following:
        - Make sure you have the latest version of the android support package v4 installed.
          You can update your support package with your Android SDK Manager.
        - Copy "/ANDROID_SDK_FOLDER/extras/android/support/v4/android-support-v4.jar"
          into the just created ActionBarSherlock project's "lib" folder.
          Confirm if asked to overwrite the existing "android-support-v4.jar".
    - Now add the just created library project to tomahawk-android by
      right-clicking your "tomahawk-android" project and selecting "Properties"
    - Select "Android" and add the library by clicking "Add...".
    - To finish the process, choose your ActionBarSherlock library project and
      click "OK".

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

Code Style Guidelines for Contributors
================

In order to keep everything clean and cozy, please use the official android code style format preset:
    - https://github.com/android/platform_development/tree/master/ide

For a larger overview you could read the official android "Code Style Guidelines for Contributors":
    - http://source.android.com/source/code-style.html


recommended reading
================
 - http://developer.android.com/training/basics/activity-lifecycle/index.html
 - http://developer.android.com/training/basics/supporting-devices/index.html
 - http://developer.android.com/training/basics/fragments/index.html

recommended IDE
================
Since there are some glitches/bugs and performance issues with Eclipse, you should check out
IntelliJ IDEA (http://www.jetbrains.com/idea/), which is basically Eclipse done better.
Since IntelliJ IDEA 12 there's also great integration with the android SDK.

