/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.InfoSystemUtils;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.QueryComparator;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.resolver.Result;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.ContentObserver;
import android.database.Cursor;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a user's local {@link UserCollection} of all his {@link Track}s.
 */
public class UserCollection {

    public static final String COLLECTION_UPDATED
            = "org.tomahawk.tomahawk_android.COLLECTION_UPDATED";

    private static UserCollection instance;

    public static final int Id = 0;

    private boolean mInitialized;

    private HandlerThread mCollectionUpdateHandlerThread;

    private Handler mHandler;

    private ConcurrentHashMap<Long, Album> mAlbums = new ConcurrentHashMap<Long, Album>();

    private ConcurrentHashMap<String, Query> mQueries = new ConcurrentHashMap<String, Query>();

    private ConcurrentHashMap<String, UserPlaylist> mUserPlaylists
            = new ConcurrentHashMap<String, UserPlaylist>();

    private Runnable mUpdateRunnable = new Runnable() {
        /* 
         * (non-Javadoc)
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
            update();
            mCollectionUpdateHandlerThread.getLooper().quit();
        }
    };

    /**
     * This class watches for changes in the Media db.
     */
    private final ContentObserver mLocalMediaObserver = new ContentObserver(null) {
        @Override
        public void onChange(boolean selfChange) {
            mCollectionUpdateHandlerThread.start();
            mHandler.post(mUpdateRunnable);
        }
    };

    private HashSet<String> mCorrespondingRequestIds = new HashSet<String>();

    private HashMap<String, String> mRequestIdPlaylistMap = new HashMap<String, String>();

    private UserCollectionReceiver mUserCollectionReceiver;

    /**
     * Handles incoming {@link UserCollection} updated broadcasts.
     */
    private class UserCollectionReceiver extends BroadcastReceiver {

        /*
         * (non-Javadoc)
         *
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                final String requestId = intent
                        .getStringExtra(InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                if (mCorrespondingRequestIds.contains(requestId)) {
                    mCorrespondingRequestIds.remove(requestId);
                    ThreadManager.getInstance().execute(
                            new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_DATABASEACTION) {
                                @Override
                                public void run() {
                                    InfoRequestData data = InfoSystem.getInstance()
                                            .removeInfoRequestById(requestId);
                                    if (data.getType()
                                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS) {
                                        ArrayList<UserPlaylist> storedLists
                                                = DatabaseHelper.getInstance()
                                                .getHatchetUserPlaylists();
                                        HashMap<String, UserPlaylist> storedListsMap
                                                = new HashMap<String, UserPlaylist>();
                                        for (UserPlaylist storedList : storedLists) {
                                            storedListsMap.put(storedList.getId(), storedList);
                                        }
                                        List<UserPlaylist> fetchedLists = data
                                                .getConvertedResultMap()
                                                .get(HatchetInfoPlugin.HATCHET_PLAYLISTS);
                                        for (UserPlaylist fetchedList : fetchedLists) {
                                            UserPlaylist storedList = storedListsMap
                                                    .remove(fetchedList.getId());
                                            if (storedList == null) {
                                                DatabaseHelper.getInstance().storeUserPlaylist(
                                                        fetchedList);
                                                fetchHatchetUserPlaylist(fetchedList.getId());
                                            } else if (!storedList.getCurrentRevision()
                                                    .equals(fetchedList.getCurrentRevision())
                                                    || storedList.getCount() == 0) {
                                                fetchHatchetUserPlaylist(storedList.getId());
                                            }
                                        }
                                        for (UserPlaylist storedList : storedListsMap.values()) {
                                            DatabaseHelper.getInstance().deleteUserPlaylist(
                                                    storedList.getId());
                                        }
                                    } else if (data.getType()
                                            == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES) {
                                        HatchetPlaylistEntries
                                                playlistEntries = (HatchetPlaylistEntries) data
                                                .getInfoResult();
                                        UserPlaylist playlist = mUserPlaylists
                                                .get(mRequestIdPlaylistMap.get(requestId));
                                        if (playlist != null && playlistEntries != null) {
                                            playlist = InfoSystemUtils
                                                    .fillUserPlaylist(playlist,
                                                            playlistEntries);
                                            DatabaseHelper.getInstance()
                                                    .storeUserPlaylist(playlist);
                                        }
                                    } else if (data.getType()
                                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
                                        List<UserPlaylist> fetchedLists = data
                                                .getConvertedResultMap()
                                                .get(HatchetInfoPlugin.HATCHET_PLAYLISTS);
                                        if (fetchedLists.size() > 0) {
                                            DatabaseHelper.getInstance().storeUserPlaylist(
                                                    fetchedLists.get(0));
                                        }
                                    }
                                }
                            }
                    );
                }
            } else if (InfoSystem.INFOSYSTEM_OPLOGISEMPTIED.equals(intent.getAction())) {
                UserCollection.this.fetchLovedItemsUserPlaylists();
            } else if (DatabaseHelper.USERPLAYLISTSDATASOURCE_RESULTSREPORTED
                    .equals(intent.getAction())) {
                UserCollection.this.updateUserPlaylists();
            }
        }
    }

    private UserCollection() {
    }

    public static UserCollection getInstance() {
        if (instance == null) {
            synchronized (UserCollection.class) {
                if (instance == null) {
                    instance = new UserCollection();
                }
            }
        }
        return instance;
    }

    public void ensureInit() {
        if (!mInitialized) {
            mInitialized = true;
            mUserCollectionReceiver = new UserCollectionReceiver();
            TomahawkApp.getContext().registerReceiver(mUserCollectionReceiver,
                    new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED));
            TomahawkApp.getContext().registerReceiver(mUserCollectionReceiver,
                    new IntentFilter(InfoSystem.INFOSYSTEM_OPLOGISEMPTIED));
            TomahawkApp.getContext().registerReceiver(mUserCollectionReceiver,
                    new IntentFilter(DatabaseHelper.USERPLAYLISTSDATASOURCE_RESULTSREPORTED));

            TomahawkApp.getContext().getContentResolver().registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, false, mLocalMediaObserver);

            mCollectionUpdateHandlerThread = new HandlerThread("CollectionUpdate",
                    android.os.Process.THREAD_PRIORITY_BACKGROUND);
            mCollectionUpdateHandlerThread.start();

            mHandler = new Handler(mCollectionUpdateHandlerThread.getLooper());
            mHandler.postDelayed(mUpdateRunnable, 300);
        }
    }

    /**
     * @return A {@link List} of all {@link UserPlaylist}s in this {@link UserCollection}
     */
    public ArrayList<UserPlaylist> getLocalUserPlaylists() {
        ArrayList<UserPlaylist> userPlaylists = new ArrayList<UserPlaylist>();
        for (UserPlaylist userPlaylist : mUserPlaylists.values()) {
            if (!userPlaylist.isHatchetPlaylist()) {
                userPlaylists.add(userPlaylist);
            }
        }
        Collections.sort(userPlaylists,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return userPlaylists;
    }

    /**
     * @return A {@link List} of all {@link UserPlaylist}s in this {@link UserCollection}
     */
    public ArrayList<UserPlaylist> getHatchetUserPlaylists() {
        ArrayList<UserPlaylist> userPlaylists = new ArrayList<UserPlaylist>();
        for (UserPlaylist userPlaylist : mUserPlaylists.values()) {
            if (userPlaylist.isHatchetPlaylist()) {
                userPlaylists.add(userPlaylist);
            }
        }
        Collections.sort(userPlaylists,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return userPlaylists;
    }

    /**
     * Store the PlaybackService's currentPlaylist
     */
    public void setCachedUserPlaylist(UserPlaylist userPlaylist) {
        DatabaseHelper.getInstance().storeUserPlaylist(userPlaylist);
    }

    /**
     * @return the previously cached {@link UserPlaylist}
     */
    public UserPlaylist getCachedUserPlaylist() {
        return DatabaseHelper.getInstance().getCachedUserPlaylist();
    }

    public boolean isQueryLoved(Query query) {
        return DatabaseHelper.getInstance().isItemLoved(query);
    }

    /**
     * Remove or add a lovedItem-query from the LovedItems-UserPlaylist, depending on whether or not
     * it is already a lovedItem
     */
    public void toggleLovedItem(Query query) {
        boolean doSweetSweetLovin = !isQueryLoved(query);
        DatabaseHelper.getInstance().setLovedItem(query, doSweetSweetLovin);
        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(AuthenticatorManager.AUTHENTICATOR_ID_HATCHET);
        InfoSystem.getInstance().sendSocialActionPostStruct(hatchetAuthUtils, query,
                HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE, doSweetSweetLovin);
    }

    /**
     * @return the previously stored {@link UserPlaylist} which stores all of the user's loved items
     */
    public UserPlaylist getLovedItemsUserPlaylist() {
        return DatabaseHelper.getInstance().getLovedItemsUserPlaylist();
    }

    /**
     * Update the loved items user-playlist and the contained queries.
     */
    private void updateLovedItemsUserPlaylist() {
        UserPlaylist lovedItemsPlayList = DatabaseHelper.getInstance().getLovedItemsUserPlaylist();
        if (lovedItemsPlayList == null) {
            // If we don't yet have a UserPlaylist to store loved items, we create and store an
            // empty UserPlaylist here
            DatabaseHelper.getInstance().storeUserPlaylist(
                    UserPlaylist.fromQueryList(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID,
                            DatabaseHelper.LOVEDITEMS_PLAYLIST_NAME,
                            new ArrayList<Query>())
            );
        }
    }

    /**
     * Fetch the lovedItems UserPlaylist from the Hatchet API and store it in the local db, if the
     * log of pending operations is empty. Meaning if every love/unlove has already been delivered
     * to the API.
     */
    public void fetchLovedItemsUserPlaylists() {
        if (DatabaseHelper.getInstance().getLoggedOps().isEmpty()) {
            mCorrespondingRequestIds.add(InfoSystem.getInstance().resolve(
                    InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS, null));
        } else {
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(AuthenticatorManager.AUTHENTICATOR_ID_HATCHET);
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * @return A {@link List} of all {@link Track}s in this {@link UserCollection}
     */
    public ArrayList<Query> getQueries() {
        return getQueries(true);
    }

    /**
     * @return A {@link List} of all {@link Track}s in this {@link UserCollection}
     */
    public ArrayList<Query> getQueries(boolean sorted) {
        ArrayList<Query> queries = new ArrayList<Query>(mQueries.values());
        if (sorted) {
            Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_ALPHA));
        }
        return queries;
    }

    /**
     * @return always true
     */
    public boolean isLocal() {
        return true;
    }

    /**
     * Initialize this {@link UserCollection}. Pull all local tracks from the {@link MediaStore} and
     * add them to our {@link UserCollection}
     */
    private void initializeCollection() {
        Resolver userCollectionResolver = PipeLine.getInstance().getResolver(
                PipeLine.RESOLVER_ID_USERCOLLECTION);
        if (userCollectionResolver == null) {
            return;
        }

        updateLovedItemsUserPlaylist();
        updateUserPlaylists();
        fetchHatchetUserPlaylists();

        String selection = MediaStore.Audio.Media.IS_MUSIC + " != 0";

        String[] projection = {MediaStore.Audio.Media._ID, MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.TRACK, MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST, MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM};

        ContentResolver resolver = TomahawkApp.getContext().getContentResolver();

        Cursor cursor = resolver
                .query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, null,
                        null);

        // Go through the complete set of data in the MediaStore
        while (cursor != null && cursor.moveToNext()) {
            Artist artist = Artist.get(cursor.getString(6));

            Album album = Album.get(cursor.getString(8), artist);
            String albumsel = MediaStore.Audio.Albums._ID + " == " + Long.toString(
                    cursor.getLong(7));
            String[] albumproj = {MediaStore.Audio.Albums.ALBUM_ART,
                    MediaStore.Audio.Albums.FIRST_YEAR, MediaStore.Audio.Albums.LAST_YEAR};
            Cursor albumcursor = resolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
                    albumproj, albumsel, null, null);
            if (albumcursor != null && albumcursor.moveToNext()) {
                if (!TextUtils.isEmpty(albumcursor.getString(0))) {
                    album.setImage(Image.get(albumcursor.getString(0), false));
                }
                album.setFirstYear(albumcursor.getString(1));
                album.setLastYear(albumcursor.getString(2));
            }
            if (albumcursor != null) {
                albumcursor.close();
            }
            if (!mAlbums.containsKey(cursor.getLong(7))) {
                mAlbums.put(cursor.getLong(7), album);
            } else {
                album = mAlbums.get(cursor.getLong(7));
            }

            Track track = Track.get(cursor.getString(2), album, artist);
            track.setDuration(cursor.getLong(3));
            track.setAlbumPos(cursor.getInt(4));

            Query query = Query.get(track.getName(), album.getName(), artist.getName(), true);
            Result result = new Result(cursor.getString(1), track, userCollectionResolver);
            result.setTrackScore(1f);
            query.addTrackResult(result);
            mQueries.put(query.getCacheKey(), query);

            artist.addQuery(query);
            artist.addAlbum(album, true);
            album.addQuery(query);
        }

        if (cursor != null) {
            cursor.close();
        }
    }

    /**
     * Fetch all user {@link UserPlaylist} from the app's database via our helper class {@link
     * org.tomahawk.libtomahawk.database.DatabaseHelper}
     */
    private void updateUserPlaylists() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DatabaseHelper.getInstance()) {
                    ArrayList<UserPlaylist> localPlaylists = DatabaseHelper.getInstance()
                            .getLocalUserPlaylists();
                    for (UserPlaylist userPlaylist : localPlaylists) {
                        mUserPlaylists.put(userPlaylist.getId(), userPlaylist);
                    }
                    ArrayList<UserPlaylist> hatchetPlaylists = DatabaseHelper.getInstance()
                            .getHatchetUserPlaylists();
                    for (UserPlaylist userPlaylist : hatchetPlaylists) {
                        mUserPlaylists.put(userPlaylist.getId(), userPlaylist);
                    }
                    for (UserPlaylist userPlaylist : mUserPlaylists.values()) {
                        if (!localPlaylists.contains(userPlaylist) && !hatchetPlaylists
                                .contains(userPlaylist)) {
                            mUserPlaylists.remove(userPlaylist.getId());
                        }
                    }
                    TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
                }
            }
        }).start();
    }

    /**
     * Fetch the UserPlaylists from the Hatchet API and store it in the local db.
     */
    public void fetchHatchetUserPlaylists() {
        mCorrespondingRequestIds.add(InfoSystem.getInstance().resolve(
                InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS, null));
    }

    /**
     * Fetch the UserPlaylist entries from the Hatchet API and store them in the local db.
     */
    public void fetchHatchetUserPlaylist(String userPlaylistId) {
        Multimap<String, String> params = HashMultimap.create(1, 1);
        params.put(HatchetInfoPlugin.HATCHET_PARAM_ID, userPlaylistId);
        String requestid = InfoSystem.getInstance()
                .resolve(InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES, params);
        mCorrespondingRequestIds.add(requestid);
        mRequestIdPlaylistMap.put(requestid, userPlaylistId);
    }

    /**
     * Reinitalize this {@link UserCollection} and send a broadcast letting everybody know.
     */
    public void update() {
        initializeCollection();

        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
    }

    /**
     * @return this {@link UserCollection}'s id
     */
    public int getId() {
        return Id;
    }
}
