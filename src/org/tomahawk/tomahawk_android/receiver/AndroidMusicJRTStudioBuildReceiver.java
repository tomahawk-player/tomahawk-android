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
 * @author grodin
 * @see AbstractPlayStatusReceiver
 * @since 1.4.3
 */
public class AndroidMusicJRTStudioBuildReceiver extends BuiltInMusicAppReceiver {

    public static final String ACTION_ANDROID_PLAYSTATECHANGED
            = "com.jrtstudio.music.playstatechanged";

    public static final String ACTION_ANDROID_STOP = "com.jrtstudio.music.playbackcomplete";

    public static final String ACTION_ANDROID_METACHANGED = "com.jrtstudio.music.metachanged";

    public static final String PACKAGE_NAME = "com.jrtstudio.music";

    public static final String NAME = "Android Music Player (JRT Studio Build)";

    static final String GOOGLE_MUSIC_PACKAGE = "com.jrtstudio.music";

    public AndroidMusicJRTStudioBuildReceiver() {
        super(ACTION_ANDROID_STOP, PACKAGE_NAME, NAME);
    }
}
