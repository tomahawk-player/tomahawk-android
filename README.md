tomahawk-android
================

Tomahawk's Android Music Player

setup
================

    - Open Eclipse and go to "File"->"Import"
    - Under Android/ select "Existing Android Code into Workspace."
    - Browse to your tomahawk-android checkout.
    - Two project will appear in the import dialog. Import them both.
      One is the app and one is the unit tests.

    Notes:
        - There is a known issue when importing. The primary app name
          ends up being "org.tomahawk.tomahawk_android.TomahawkMainActivity".
          Right click on the project and go to "Refactor"->"Rename". Rename
          the project to "tomahawk-android" and this should fix any errors.

required reading
================
 - http://developer.android.com/reference/android/os/Handler.html
 - http://developer.android.com/guide/practices/screens_support.html

