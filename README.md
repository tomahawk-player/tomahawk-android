tomahawk-android
================

Tomahawk's Android Music Player

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
      "File"-> "Import" -> "Android" -> "Existing Android Code into Workspace"
    - Go into the folder you've extracted your downloaded zip-file to and
      choose the "library" folder as your "Root Directory".
    - Check "copy projects into workspace" and click "Finish".
    - Now add the just created library project to tomahawk-android by
      rightclicking your "tomahawk-android" project and selecting "Properties"
    - Select "Android" and add the library by clicking "Add...".
    - To finish the process, choose your ActionBarSherlock library project and
      click "OK".

    Notes:
        - There is a known issue when importing. The primary app name
          ends up being "org.tomahawk.tomahawk_android.TomahawkMainActivity".
          Right click on the project and go to "Refactor"->"Rename". Rename
          the project to "tomahawk-android" and this should fix any errors.
		- If you have troubles building ActionBarSherlock, confirm that you have android-14 installed
		  in the sdk. This version is needed to build ActionBarSherlock, but you should use latest version
		  to build Tomahawk-Android.
		- If you have other build problems, confirm that your Java Compiler is set to v1.6. 
		  ( in eclipse, go to tomahawk-android ( right click ) -> Properties -> Java Compiler -> 
		  Compiler compliance level -> 1.6 )
		- Make sure that you dont tick the Is Library box in Properties->Android || Library.
		- It is also good to add the sdk to your path. 

required reading
================
 - http://developer.android.com/reference/android/os/Handler.html
 - http://developer.android.com/guide/practices/screens_support.html

