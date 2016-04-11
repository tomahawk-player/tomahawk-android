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

import org.jdeferred.DoneCallback;
import org.jdeferred.FailCallback;
import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.AlphaComparable;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Cacheable;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.SocialActionsFragment;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class User extends Cacheable implements AlphaComparable {

    private static User mSelf = new User("self");

    static {
        mSelf.setName("Myself");
        mSelf.setIsOffline(true);
    }

    private static final String PLAYLIST_PLAYBACKLOG_ID = "_playbackLog";

    private static final String PLAYLIST_FAVORITES_ID = "_favorites";

    private static final String PLAYLIST_SOCIALACTIONS_ID = "_socialActions";

    private static final String PLAYLIST_FRIENDSFEED_ID = "_friendsfeed";

    private String mId;

    private String mName;

    private Image mImage;

    private String mAbout;

    private int mFollowCount = -1;

    private int mFollowersCount = -1;

    private Query mNowPlaying;

    private Date mNowPlayingTimeStamp;

    private int mTotalPlays;

    private final TreeMap<Date, List<SocialAction>> mSocialActions = new TreeMap<>();

    private final TreeMap<Date, List<SocialAction>> mFriendsFeed = new TreeMap<>();

    private Date mSocialActionsNextDate = new Date();

    private Date mFriendsFeedNextDate = new Date();

    private Playlist mSocialActionsPlaylist;

    private Playlist mFriendsFeedPlaylist;

    private Set<Date> mSocialActionsDoneConversions = new HashSet<>();

    private Set<Date> mFriendsFeedDoneConversions = new HashSet<>();

    private final Map<SocialAction, PlaylistEntry> mPlaylistEntryMap = new HashMap<>();

    private Playlist mPlaybackLog;

    private Playlist mFavorites;

    private TreeMap<User, String> mFollowings;

    private TreeMap<User, String> mFollowers;

    private List<Album> mStarredAlbums = new ArrayList<>();

    private List<Artist> mStarredArtists = new ArrayList<>();

    private List<Playlist> mPlaylists;

    private boolean mIsOffline;

    private Map<Object, Relationship> mRelationships = new ConcurrentHashMap<>();

    /**
     * Construct a new {@link User} with the given id
     */
    private User(String id) {
        super(User.class, id);

        mId = id;
        mPlaybackLog = Playlist.fromEmptyList(id + User.PLAYLIST_PLAYBACKLOG_ID, "");
        mFavorites = Playlist.fromEmptyList(id + User.PLAYLIST_FAVORITES_ID, "");
        mFavorites.setFilled(true);
        mSocialActionsPlaylist =
                Playlist.fromEmptyList(id + User.PLAYLIST_SOCIALACTIONS_ID, "");
        mFriendsFeedPlaylist = Playlist.fromEmptyList(id + User.PLAYLIST_FRIENDSFEED_ID, "");
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

    public static Promise<User, Throwable, Void> getSelf() {
        final ADeferredObject<User, Throwable, Void> deferred = new ADeferredObject<>();
        final HatchetAuthenticatorUtils authUtils = (HatchetAuthenticatorUtils) AuthenticatorManager
                .get().getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        authUtils.getUserId().done(new DoneCallback<String>() {
            @Override
            public void onDone(String result) {
                if (result != null && !mSelf.getId().equals(result)) {
                    mSelf.setName(authUtils.getUserName());
                    mSelf.setId(result);
                    mSelf.setIsOffline(false);
                }
                deferred.resolve(mSelf);
            }
        }).fail(new FailCallback<Throwable>() {
            @Override
            public void onFail(Throwable result) {
                deferred.resolve(mSelf);
            }
        });
        return deferred;
    }

    public void putRelationship(Object object, Relationship relationship) {
        mRelationships.put(object, relationship);
    }

    public Relationship getRelationship(Object object) {
        return mRelationships.get(object);
    }

    public boolean isOffline() {
        return mIsOffline;
    }

    public void setIsOffline(boolean isOffline) {
        mIsOffline = isOffline;
    }

    private void setId(String id) {
        mId = id;
        put(User.class, id, this);
    }

    /**
     * @return this object' name
     */
    public String getName() {
        return mName;
    }

    public void setName(final String name) {
        mName = name;
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User result) {
                String playbackLogName;
                String favoritesName;
                if (User.this == result) {
                    playbackLogName = TomahawkApp.getContext().getString(R.string.my_playbacklog);
                    favoritesName = TomahawkApp.getContext().getString(R.string.my_favorites);
                } else {
                    playbackLogName = TomahawkApp.getContext().getString(
                            R.string.users_playbacklog_suffix, name);
                    favoritesName = TomahawkApp.getContext().getString(
                            R.string.users_favorites_suffix, name);
                }
                mPlaybackLog.setName(playbackLogName);
                mFavorites.setName(favoritesName);
            }
        });
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

    public TreeMap<Date, List<SocialAction>> getSocialActions() {
        return mSocialActions;
    }

    public void setSocialActions(List<SocialAction> socialActions, Date date) {
        if (socialActions != null && socialActions.size() > 0) {
            mSocialActions.put(date, socialActions);
            SocialAction socialAction = socialActions.get(socialActions.size() - 1);
            if (socialAction != null) {
                if (socialAction.getDate().getTime() < date.getTime()) {
                    mSocialActionsNextDate = socialAction.getDate();
                }
            }
            fillPlaylist(mSocialActionsPlaylist, mSocialActions, mSocialActionsDoneConversions);
        }
    }

    public Date getSocialActionsNextDate() {
        return mSocialActionsNextDate;
    }

    public TreeMap<Date, List<SocialAction>> getFriendsFeed() {
        return mFriendsFeed;
    }

    public void setFriendsFeed(List<SocialAction> friendsFeed, Date date) {
        if (friendsFeed != null && friendsFeed.size() > 0) {
            mFriendsFeed.put(date, friendsFeed);
            SocialAction socialAction = friendsFeed.get(friendsFeed.size() - 1);
            if (socialAction != null) {
                if (socialAction.getDate().getTime() < date.getTime()) {
                    mFriendsFeedNextDate = socialAction.getDate();
                }
            }
            fillPlaylist(mFriendsFeedPlaylist, mFriendsFeed, mFriendsFeedDoneConversions);
        }
    }

    public Date getFriendsFeedNextDate() {
        return mFriendsFeedNextDate;
    }

    public Playlist getSocialActionsPlaylist() {
        return mSocialActionsPlaylist;
    }

    public Playlist getFriendsFeedPlaylist() {
        return mFriendsFeedPlaylist;
    }

    private void fillPlaylist(Playlist playlist, TreeMap<Date, List<SocialAction>> actions,
            Set<Date> doneConversions) {
        for (Date date : actions.keySet()) {
            if (!doneConversions.contains(date)) {
                doneConversions.add(date);
                List<List<SocialAction>> mergedActions =
                        SocialActionsFragment.mergeSocialActions(actions.get(date));
                for (List<SocialAction> actionsList : mergedActions) {
                    for (SocialAction action : actionsList) {
                        if (action.getTargetObject() instanceof Query) {
                            Query query = (Query) action.getTargetObject();
                            PlaylistEntry entry = playlist.addQuery(playlist.size(), query);
                            mPlaylistEntryMap.put(action, entry);
                        }
                    }
                }
            }
        }
    }

    public PlaylistEntry getPlaylistEntry(SocialAction item) {
        return mPlaylistEntryMap.get(item);
    }

    public Playlist getPlaybackLog() {
        return mPlaybackLog;
    }

    public void setPlaybackLog(Playlist playbackLog) {
        if (playbackLog != null) {
            mPlaybackLog = playbackLog;
        }
    }

    public Playlist getFavorites() {
        return mFavorites;
    }

    public void setFavorites(Playlist favorites) {
        if (favorites != null) {
            favorites.setUserId(mId);
            favorites.setFilled(true);
            mFavorites = favorites;
        }
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

    public List<Album> getStarredAlbums() {
        return mStarredAlbums;
    }

    public void setStarredAlbums(List<Album> starredAlbums) {
        mStarredAlbums = starredAlbums;
    }

    public List<Artist> getStarredArtists() {
        return mStarredArtists;
    }

    public void setStarredArtists(
            List<Artist> starredArtists) {
        mStarredArtists = starredArtists;
    }

    public List<Playlist> getPlaylists() {
        return mPlaylists;
    }

    public void setPlaylists(List<Playlist> playlists) {
        if (playlists != null) {
            for (Playlist playlist : playlists) {
                playlist.setUserId(mId);
            }
        }
        mPlaylists = playlists;
    }
}
