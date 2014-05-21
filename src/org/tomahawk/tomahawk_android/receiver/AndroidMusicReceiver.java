/**
 * This file is part of Simple Last.fm Scrobbler.
 *
 *     http://code.google.com/p/a-simple-lastfm-scrobbler/
 *
 * Copyright 2011 Simple Last.fm Scrobbler Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tomahawk.tomahawk_android.receiver;

/**
 * A BroadcastReceiver for intents sent by the Android Music Player.
 *
 * @author tgwizard
 * @see AbstractPlayStatusReceiver
 * @since 1.0.1
 */
public class AndroidMusicReceiver extends BuiltInMusicAppReceiver {

    public static final String ACTION_ANDROID_PLAYSTATECHANGED
            = "com.android.music.playstatechanged";

    public static final String ACTION_ANDROID_STOP = "com.android.music.playbackcomplete";

    public static final String ACTION_ANDROID_METACHANGED = "com.android.music.metachanged";

    public static final String PACKAGE_NAME = "com.android.music";

    public static final String NAME = "Android Music Player";

    static final String GOOGLE_MUSIC_PACKAGE = "com.google.android.music";

    public AndroidMusicReceiver() {
        super(ACTION_ANDROID_STOP, PACKAGE_NAME, NAME);
    }
}
