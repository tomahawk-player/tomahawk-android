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
import org.tomahawk.libtomahawk.collection.AlphaComparable;
import org.tomahawk.libtomahawk.collection.Cacheable;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.util.SparseArray;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class User extends Cacheable implements AlphaComparable {

    public static final String PLAYLIST_PLAYBACKLOG_ID = "_playbackLog";

    public static final String PLAYLIST_FAVORTIES_ID = "_favorites";

    private final String mId;

    private String mName;

    private Image mImage;

    private String mAbout;

    private int mFollowCount;

    private int mFollowersCount;

    private Query mNowPlaying;

    private Date mNowPlayingTimeStamp;

    private int mTotalPlays;

    private final SparseArray<List<SocialAction>> mSocialActions = new SparseArray<>();

    private final SparseArray<List<SocialAction>> mFriendsFeed = new SparseArray<>();

    private final Playlist mPlaybackLog;

    private final Playlist mFavorites;

    private TreeMap<User, String> mFollowings;

    private TreeMap<User, String> mFollowers;

    private Set<Album> mStarredAlbums = new HashSet<>();

    private List<Playlist> mPlaylists;

    /**
     * Construct a new {@link User} with the given id
     */
    private User(String id) {
        super(User.class, id);

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
        Cacheable cacheable = get(User.class, id);
        return cacheable != null ? (User) cacheable : new User(id);
    }

    public static User getUserById(String id) {
        return (User) get(User.class, id);
    }

    /**
     * @return this object' name
     */
    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
        mPlaybackLog.setName(
                TomahawkApp.getContext().getString(R.string.users_playbacklog_suffix, name));
        mFavorites.setName(
                TomahawkApp.getContext().getString(R.string.users_favorites_suffix, name));
    }

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

    public TreeMap<User, String> getFollowings() {
        return mFollowings;
    }

    public void setFollowings(TreeMap<User, String> followings) {
        mFollowings = followings;
    }

    public TreeMap<User, String> getFollowers() {
        return mFollowers;
    }

    public void setFollowers(TreeMap<User, String> followers) {
        mFollowers = followers;
    }

    public Set<Album> getStarredAlbums() {
        return mStarredAlbums;
    }

    public void setStarredAlbums(Set<Album> starredAlbums) {
        mStarredAlbums = starredAlbums;
    }

    public List<Playlist> getPlaylists() {
        return mPlaylists;
    }

    public void setPlaylists(List<Playlist> playlists) {
        mPlaylists = playlists;
    }
}
