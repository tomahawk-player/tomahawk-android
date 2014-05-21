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
 * A BroadcastReceiver for intents sent by the HTC Hero Music Player.
 *
 * @author tgwizard
 * @see AbstractPlayStatusReceiver
 * @since 1.0.1
 */
public class HeroMusicReceiver extends BuiltInMusicAppReceiver {

    public static final String ACTION_HTC_PLAYSTATECHANGED = "com.htc.music.playstatechanged";

    public static final String ACTION_HTC_STOP = "com.htc.music.playbackcomplete";

    public static final String ACTION_HTC_METACHANGED = "com.htc.music.metachanged";

    public HeroMusicReceiver() {
        super(ACTION_HTC_STOP, "com.htc.music", "Hero Music Player");
    }
}
