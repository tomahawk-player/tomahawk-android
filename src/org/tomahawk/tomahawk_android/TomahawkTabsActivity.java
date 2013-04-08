/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android;

import com.slidingmenu.lib.app.SlidingFragmentActivity;

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.audio.PlaybackService;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 14.01.13
 */
public abstract class TomahawkTabsActivity extends SlidingFragmentActivity {

    public static final int TAB_ID_HOME = 0;

    public static final int TAB_ID_SEARCH = 1;

    public static final int TAB_ID_COLLECTION = 2;

    public static final int TAB_ID_PLAYLISTS = 3;

    public static final int TAB_ID_STATIONS = 4;

    public static final int TAB_ID_FRIENDS = 5;

    public static final int TAB_ID_SETTINGS = 6;

    public abstract Collection getCollection();

    public abstract PlaybackService getPlaybackService();

    public abstract ContentViewer getContentViewer();
}
