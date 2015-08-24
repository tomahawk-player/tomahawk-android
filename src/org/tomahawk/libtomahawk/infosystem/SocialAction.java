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
package org.tomahawk.libtomahawk.infosystem;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Cacheable;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.resolver.Query;

import java.util.Date;

public class SocialAction extends Cacheable {

    private final String mId;

    private String mAction;

    private Album mAlbum;

    private Artist mArtist;

    private Date mDate;

    private Playlist mPlaylist;

    private User mTarget;

    private Query mQuery;

    private String mType;

    private User mUser;

    /**
     * Construct a new {@link SocialAction} with the given id
     */
    private SocialAction(String id) {
        super(SocialAction.class, id);

        mId = id;
    }

    /**
     * Returns the {@link SocialAction} with the given id. If none exists in our static {@link
     * java.util.concurrent.ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link SocialAction} with the given id
     */
    public static SocialAction get(String id) {
        Cacheable cacheable = get(SocialAction.class, id);
        return cacheable != null ? (SocialAction) cacheable : new SocialAction(id);
    }

    /**
     * Get a SocialAction by providing its id
     */
    public static SocialAction getByKey(String id) {
        return (SocialAction) get(SocialAction.class, id);
    }

    public String getName() {
        return null;
    }

    public Image getImage() {
        if (mQuery.getImage() != null) {
            return mQuery.getImage();
        } else if (mAlbum.getImage() != null) {
            return mAlbum.getImage();
        } else if (mArtist.getImage() != null) {
            return mArtist.getImage();
        } else {
            return mUser.getImage();
        }
    }

    public Object getTargetObject() {
        if (mTarget != null) {
            return mTarget;
        } else if (mArtist != null) {
            return mArtist;
        } else if (mAlbum != null) {
            return mAlbum;
        } else if (mQuery != null) {
            return mQuery;
        } else if (mPlaylist != null) {
            return mPlaylist;
        }
        return null;
    }

    public String getId() {
        return mId;
    }

    public String getAction() {
        return mAction;
    }

    public void setAction(String action) {
        mAction = action;
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

    public Date getDate() {
        return mDate;
    }

    public void setDate(Date date) {
        mDate = date;
    }

    public Playlist getPlaylist() {
        return mPlaylist;
    }

    public void setPlaylist(Playlist playlist) {
        mPlaylist = playlist;
    }

    public User getTarget() {
        return mTarget;
    }

    public void setTarget(User target) {
        mTarget = target;
    }

    public Query getQuery() {
        return mQuery;
    }

    public void setQuery(Query query) {
        mQuery = query;
    }

    public String getType() {
        return mType;
    }

    public void setType(String type) {
        mType = type;
    }

    public User getUser() {
        return mUser;
    }

    public void setUser(User user) {
        mUser = user;
    }
}
