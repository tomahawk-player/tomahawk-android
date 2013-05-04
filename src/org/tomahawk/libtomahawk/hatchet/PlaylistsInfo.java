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
package org.tomahawk.libtomahawk.hatchet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import java.util.ArrayList;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.04.13
 */
public class PlaylistsInfo implements Info {

    private final static String TAG = PlaylistsInfo.class.getName();

    public static final String PLAYLISTINFO_KEY_PLAYLISTS = "Playlists";

    private ArrayList<PlaylistInfo> mPlaylists;

    @Override
    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(PLAYLISTINFO_KEY_PLAYLISTS)) {
                JSONArray rawRevisionInfos = rawInfo.getJSONArray(PLAYLISTINFO_KEY_PLAYLISTS);
                mPlaylists = new ArrayList<PlaylistInfo>();
                for (int i = 0; i < rawRevisionInfos.length(); i++) {
                    PlaylistInfo playlistInfo = new PlaylistInfo();
                    playlistInfo.parseInfo(rawRevisionInfos.getJSONObject(i));
                    mPlaylists.add(playlistInfo);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public ArrayList<PlaylistInfo> getPlaylists() {
        return mPlaylists;
    }
}
