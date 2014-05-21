/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.tomahawk_android.receiver;

public class RocketPlayerReceiver extends BuiltInMusicAppReceiver {

    public static final String ACTION_ANDROID_PLAYSTATECHANGED
            = "com.jrtstudio.AnotherMusicPlayer.playstatechanged";

    public static final String ACTION_ANDROID_STOP
            = "com.jrtstudio.AnotherMusicPlayer.playbackcomplete";

    public static final String ACTION_ANDROID_METACHANGED
            = "com.jrtstudio.AnotherMusicPlayer.metachanged";

    public static final String PACKAGE_NAME = "com.jrtstudio.AnotherMusicPlayer";

    public static final String NAME = "Rocket Player";

    public RocketPlayerReceiver() {
        super(ACTION_ANDROID_STOP, PACKAGE_NAME, NAME);
    }
}
