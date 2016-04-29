/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.infosystem.stations;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;

import android.support.v4.util.Pair;

import java.util.ArrayList;
import java.util.List;

public class ScriptPlaylistGeneratorSearchResult {

    public List<Pair<Artist, String>> mArtists = new ArrayList<>();

    public List<Album> mAlbums = new ArrayList<>();

    public List<Pair<Track, String>> mTracks = new ArrayList<>();

    public List<String> mGenres = new ArrayList<>();

    public List<String> mMoods = new ArrayList<>();

    public ScriptPlaylistGeneratorSearchResult() {
    }
}
