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
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;

public class SocialAction implements TomahawkListItem {

    private static final ConcurrentHashMap<String, SocialAction> sSocialActions
            = new ConcurrentHashMap<>();

    private final String mId;

    private String mAction;

    private Album mAlbum;

    private Artist mArtist;

    private Date mDate;

    private Date mTimeStamp;

    private Playlist mPlaylist;

    private User mTarget;

    private Query mQuery;

    private String mType;

    private User mUser;

    /**
     * Construct a new {@link SocialAction} with the given id
     */
    private SocialAction(String id) {
        mId = id;
    }

    /**
     * Returns the {@link SocialAction} with the given id. If none exists in our static {@link
     * java.util.concurrent.ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link SocialAction} with the given id
     */
    public static SocialAction get(String id) {
        SocialAction socialAction = new SocialAction(id);
        if (!sSocialActions.containsKey(socialAction.getId())) {
            sSocialActions.put(socialAction.getId(), socialAction);
        }
        return sSocialActions.get(socialAction.getId());
    }

    /**
     * Get a SocialAction by providing its id
     */
    public static SocialAction getSocialActionById(String id) {
        return sSocialActions.get(id);
    }

    /**
     * @return A {@link java.util.List} of all {@link SocialAction}s
     */
    public static ArrayList<SocialAction> getSocialActions() {
        return new ArrayList<>(sSocialActions.values());
    }

    @Override
    public String getCacheKey() {
        return mId;
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public Artist getArtist() {
        return mArtist;
    }

    @Override
    public Album getAlbum() {
        return mAlbum;
    }

    @Override
    public ArrayList<Query> getQueries() {
        ArrayList<Query> queries = new ArrayList<>();
        queries.add(mQuery);
        return queries;
    }

    @Override
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

    public TomahawkListItem getTargetObject() {
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

    public void setAlbum(Album album) {
        mAlbum = album;
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

    public Date getTimeStamp() {
        return mTimeStamp;
    }

    public void setTimeStamp(Date timeStamp) {
        mTimeStamp = timeStamp;
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
