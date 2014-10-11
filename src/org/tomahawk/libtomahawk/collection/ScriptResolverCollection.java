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
package org.tomahawk.libtomahawk.collection;

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a Collection which contains tracks/albums/artists retrieved by a
 * ScriptResolver.
 */
public class ScriptResolverCollection extends Collection {

    private ScriptResolver mScriptResolver;

    public ScriptResolverCollection(ScriptResolver scriptResolver) {
        super(scriptResolver.getId(), scriptResolver.getCollectionName(), true);

        mScriptResolver = scriptResolver;
        initializeCollection();
    }

    /**
     * Initialize this {@link org.tomahawk.libtomahawk.collection.ScriptResolverCollection}.
     */
    protected void initializeCollection() {
        mScriptResolver.artists(getId());
    }

    public ScriptResolver getScriptResolver() {
        return mScriptResolver;
    }

    public void addAlbumTrackResults(Album album, List<Result> results) {
        ArrayList<Query> queries = new ArrayList<Query>();
        for (Result r : results) {
            r.setTrackScore(1f);
            Query query = Query.get(r, isLocal());
            query.addTrackResult(r);
            queries.add(query);
            addQuery(query, 0);
        }
        addAlbumTracks(album, queries);
        sendCollectionUpdatedBroadcast();
    }

    public void addArtistResults(List<Artist> artists) {
        for (Artist artist : artists) {
            if (!TextUtils.isEmpty(artist.getName())) {
                addArtist(artist);
            }
        }
    }

    public void addAlbumResults(List<Album> albums) {
        for (Album album : albums) {
            if (!TextUtils.isEmpty(album.getName())) {
                addAlbum(album);
                addArtistAlbum(album.getArtist(), album);
                sendCollectionUpdatedBroadcast();
            }
        }
    }

    @Override
    public ArrayList<Query> getAlbumTracks(Album album, boolean sorted) {
        if (mAlbumTracks.get(album) != null) {
            return super.getAlbumTracks(album, sorted);
        } else {
            mScriptResolver.tracks(getId(), album.getArtist().getName(), album.getName());
            return new ArrayList<Query>();
        }
    }

    @Override
    public ArrayList<Album> getArtistAlbums(Artist artist, boolean sorted) {
        if (mArtistAlbums.get(artist) != null) {
            return super.getArtistAlbums(artist, sorted);
        } else {
            mScriptResolver.albums(getId(), artist.getName());
            return new ArrayList<Album>();
        }
    }
}
