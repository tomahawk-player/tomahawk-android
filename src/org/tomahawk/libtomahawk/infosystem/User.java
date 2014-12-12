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
import org.tomahawk.libtomahawk.collection.TomahawkListItemComparator;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class User implements TomahawkListItem {

    public static final String PLAYLIST_PLAYBACKLOG_ID = "_playbackLog";

    public static final String PLAYLIST_FAVORTIES_ID = "_favorites";

    private static ConcurrentHashMap<String, User> sUsers
            = new ConcurrentHashMap<String, User>();

    private String mId;

    private String mName;

    private Image mImage;

    private String mAbout;

    private int mFollowCount;

    private int mFollowersCount;

    private Query mNowPlaying;

    private Date mNowPlayingTimeStamp;

    private int mTotalPlays;

    private SparseArray<List<SocialAction>> mSocialActions = new SparseArray<List<SocialAction>>();

    private SparseArray<List<SocialAction>> mFriendsFeed = new SparseArray<List<SocialAction>>();

    private Playlist mPlaybackLog;

    private Playlist mFavorites;

    private Map<User, String> mFollowings = new HashMap<User, String>();

    private Map<User, String> mFollowers = new HashMap<User, String>();

    private List<Album> mStarredAlbums = new ArrayList<Album>();

    private List<Playlist> mPlaylists = new ArrayList<Playlist>();

    /**
     * Construct a new {@link User} with the given id
     */
    private User(String id) {
        mId = id;
        mPlaybackLog = Playlist.get(id + User.PLAYLIST_PLAYBACKLOG_ID, "", "");
        mFavorites = Playlist.get(id + User.PLAYLIST_FAVORTIES_ID, "", "");
    }

    /**
     * Returns the {@link User} with the given id. If none exists in our static {@link
     * ConcurrentHashMap} yet, construct and add it.
     *
     * @return {@link User} with the given id
     */
    public static User get(String id) {
        User user = new User(id);
        if (!sUsers.containsKey(user.getId())) {
            sUsers.put(user.getId(), user);
        }
        return sUsers.get(user.getId());
    }

    /**
     * @return A {@link java.util.List} of all {@link User}s
     */
    public static ArrayList<User> getUsers() {
        ArrayList<User> users = new ArrayList<User>(sUsers.values());
        Collections.sort(users,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return users;
    }

    @Override
    public String getCacheKey() {
        return mId;
    }

    /**
     * @return this object's name
     */
    @Override
    public String toString() {
        return mName;
    }

    /**
     * @return this object' name
     */
    @Override
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
        mPlaybackLog.setName(
                name + TomahawkApp.getContext().getString(R.string.users_playbacklog_suffix));
        mFavorites.setName(
                name + TomahawkApp.getContext().getString(R.string.users_favorites_suffix));
    }

    @Override
    public Artist getArtist() {
        return null;
    }

    @Override
    public Album getAlbum() {
        return null;
    }

    @Override
    public ArrayList<Query> getQueries() {
        return mPlaybackLog.getQueries();
    }

    @Override
    public Image getImage() {
        return mImage;
    }

    public void setImage(Image image) {
        mImage = image;
    }

    public String getId() {
        return mId;
    }

    public String getAbout() {
        return mAbout;
    }

    public void setAbout(String about) {
        mAbout = about;
    }

    public int getFollowCount() {
        return mFollowCount;
    }

    public void setFollowCount(int followCount) {
        mFollowCount = followCount;
    }

    public int getFollowersCount() {
        return mFollowersCount;
    }

    public void setFollowersCount(int followersCount) {
        mFollowersCount = followersCount;
    }

    public Query getNowPlaying() {
        return mNowPlaying;
    }

    public void setNowPlaying(Query nowPlaying) {
        mNowPlaying = nowPlaying;
    }

    public Date getNowPlayingTimeStamp() {
        return mNowPlayingTimeStamp;
    }

    public void setNowPlayingTimeStamp(Date nowPlayingTimeStamp) {
        mNowPlayingTimeStamp = nowPlayingTimeStamp;
    }

    public int getTotalPlays() {
        return mTotalPlays;
    }

    public void setTotalPlays(int totalPlays) {
        mTotalPlays = totalPlays;
    }

    public SparseArray<List<SocialAction>> getSocialActions() {
        return mSocialActions;
    }

    public void setSocialActions(ArrayList<SocialAction> socialActions, int pageNumber) {
        mSocialActions.put(pageNumber, socialActions);
    }

    public SparseArray<List<SocialAction>> getFriendsFeed() {
        return mFriendsFeed;
    }

    public void setFriendsFeed(ArrayList<SocialAction> friendsFeed, int pageNumber) {
        mFriendsFeed.put(pageNumber, friendsFeed);
    }

    public Playlist getPlaybackLog() {
        return mPlaybackLog;
    }

    public Playlist getFavorites() {
        return mFavorites;
    }

    public Map<User, String> getFollowings() {
        return mFollowings;
    }

    public void setFollowings(Map<User, String> followings) {
        mFollowings = followings;
    }

    public Map<User, String> getFollowers() {
        return mFollowers;
    }

    public void setFollowers(Map<User, String> followers) {
        mFollowers = followers;
    }

    public List<Album> getStarredAlbums() {
        return mStarredAlbums;
    }

    public void setStarredAlbums(List<Album> starredAlbums) {
        mStarredAlbums = starredAlbums;
    }

    public List<Playlist> getPlaylists() {
        return mPlaylists;
    }

    public void setPlaylists(List<Playlist> playlists) {
        mPlaylists = playlists;
    }
}
