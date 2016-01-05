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
package org.tomahawk.libtomahawk.infosystem;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Cacheable;
import org.tomahawk.libtomahawk.resolver.Query;

import java.util.Date;

public class Relationship extends Cacheable {

    private String mType;

    private User mUser;

    private Date mDate;

    private Query mQuery;

    private Album mAlbum;

    private Artist mArtist;

    private Relationship(String id, String type, User user, Date date) {
        super(Relationship.class, id);

        mType = type;
        mUser = user;
        mDate = date;
    }

    public static Relationship get(String id, String type, User user, Date date) {
        Cacheable cacheable = get(Relationship.class, id);
        return cacheable != null ? (Relationship) cacheable :
                new Relationship(id, type, user, date);
    }

    public String getType() {
        return mType;
    }

    public User getUser() {
        return mUser;
    }

    public Date getDate() {
        return mDate;
    }

    public Query getQuery() {
        return mQuery;
    }

    public void setQuery(Query query) {
        mQuery = query;
    }

    public Album getAlbum() {
        return mAlbum;
    }

    public void setAlbum(Album album) {
        mAlbum = album;
    }

    public Artist getArtist() {
        return mArtist;
    }

    public void setArtist(Artist artist) {
        mArtist = artist;
    }
}
