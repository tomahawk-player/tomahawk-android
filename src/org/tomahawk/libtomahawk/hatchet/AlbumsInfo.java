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
public class AlbumsInfo implements Info {

    private final static String TAG = AlbumsInfo.class.getName();

    public static final String ALBUMSINFO_KEY_ALBUMS = "Albums";

    private ArrayList<AlbumInfo> mAlbums;

    @Override
    public void parseInfo(JSONObject rawInfo) {
        try {
            if (!rawInfo.isNull(ALBUMSINFO_KEY_ALBUMS)) {
                JSONArray rawArtistChartItemInfos = rawInfo.getJSONArray(ALBUMSINFO_KEY_ALBUMS);
                mAlbums = new ArrayList<AlbumInfo>();
                for (int i = 0; i < rawArtistChartItemInfos.length(); i++) {
                    AlbumInfo albumInfo = new AlbumInfo();
                    albumInfo.parseInfo(rawArtistChartItemInfos.getJSONObject(i));
                    mAlbums.add(albumInfo);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "parseInfo: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public ArrayList<AlbumInfo> getAlbums() {
        return mAlbums;
    }

}
