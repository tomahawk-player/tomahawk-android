/**
 *  This file is part of Simple Last.fm Scrobbler.
 *
 *  Simple Last.fm Scrobbler is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Simple Last.fm Scrobbler is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Simple Last.fm Scrobbler.  If not, see <http://www.gnu.org/licenses/>.
 *
 *  See http://code.google.com/p/a-simple-lastfm-scrobbler/ for the latest version.
 */

package org.tomahawk.tomahawk_android.receiver;

/**
 * A BroadcastReceiver for intents sent by the Winamp Music Player.
 *
 * @author tgwizard
 * @see BuiltInMusicAppReceiver
 * @since 1.3.2
 */
public class WinampMusicReceiver extends BuiltInMusicAppReceiver {

    static final String TAG = WinampMusicReceiver.class.getSimpleName();

    public static final String ACTION_WINAMP_START = "com.nullsoft.winamp.metachanged";

    public static final String ACTION_WINAMP_PAUSERESUME = "com.nullsoft.winamp.playstatechanged";

    // doesn't seem to work
    public static final String ACTION_WINAMP_STOP = "com.nullsoft.winamp.playbackcomplete";

    public WinampMusicReceiver() {
        super(ACTION_WINAMP_STOP, "com.nullsoft.winamp", "Winamp");
    }

}
