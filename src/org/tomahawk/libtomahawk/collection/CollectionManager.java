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
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a user's local {@link org.tomahawk.libtomahawk.collection.CollectionManager}
 * of all his {@link org.tomahawk.libtomahawk.collection.Track}s.
 */
public class CollectionManager {

    public static final String COLLECTION_UPDATED
            = "org.tomahawk.tomahawk_android.COLLECTION_UPDATED";

    public static final String COLLECTION_ID = "org.tomahawk.tomahawk_android.collection_id";

    private boolean mInitialized;

    private static CollectionManager instance;

    private ConcurrentHashMap<String, Collection> mCollections
            = new ConcurrentHashMap<String, Collection>();

    private ConcurrentHashMap<String, Playlist> mPlaylists
            = new ConcurrentHashMap<String, Playlist>();

    private HashSet<String> mCorrespondingRequestIds = new HashSet<String>();

    private HashMap<String, String> mRequestIdPlaylistMap = new HashMap<String, String>();

    private CollectionManagerReceiver mCollectionManagerReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class CollectionManagerReceiver extends BroadcastReceiver {

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
                                        ArrayList<Playlist> storedLists
                                                = DatabaseHelper.getInstance()
                                                .getHatchetPlaylists();
                                        HashMap<String, Playlist> storedListsMap
                                                = new HashMap<String, Playlist>();
                                        for (Playlist storedList : storedLists) {
                                            storedListsMap.put(storedList.getId(), storedList);
                                        }
                                        List<Playlist> fetchedLists = data
                                                .getConvertedResultMap()
                                                .get(HatchetInfoPlugin.HATCHET_PLAYLISTS);
                                        for (Playlist fetchedList : fetchedLists) {
                                            Playlist storedList = storedListsMap
                                                    .remove(fetchedList.getId());
                                            if (storedList == null) {
                                                DatabaseHelper.getInstance().storePlaylist(
                                                        fetchedList);
                                                fetchHatchetPlaylistEntries(fetchedList.getId());
                                            } else if (!storedList.getCurrentRevision()
                                                    .equals(fetchedList.getCurrentRevision())
                                                    || DatabaseHelper.getInstance()
                                                    .getPlaylistTrackCount(storedList.getId())
                                                    == 0) {
                                                fetchHatchetPlaylistEntries(storedList.getId());
                                            } else if (!storedList.getName()
                                                    .equals(fetchedList.getName())) {
                                                DatabaseHelper.getInstance().renamePlaylist(
                                                        storedList, fetchedList.getName());
                                            }
                                        }
                                        for (Playlist storedList : storedListsMap.values()) {
                                            DatabaseHelper.getInstance().deletePlaylist(
                                                    storedList.getId());
                                        }
                                    } else if (data.getType()
                                            == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES) {
                                        HatchetPlaylistEntries
                                                playlistEntries = (HatchetPlaylistEntries) data
                                                .getInfoResult();
                                        Playlist playlist = mPlaylists
                                                .get(mRequestIdPlaylistMap.get(requestId));
                                        if (playlist != null && playlistEntries != null) {
                                            playlist = InfoSystemUtils
                                                    .fillPlaylist(playlist,
                                                            playlistEntries);
                                            DatabaseHelper.getInstance()
                                                    .storePlaylist(playlist);
                                        }
                                    } else if (data.getType()
                                            == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
                                        List<Playlist> fetchedLists = data
                                                .getConvertedResultMap()
                                                .get(HatchetInfoPlugin.HATCHET_PLAYLISTS);
                                        if (fetchedLists.size() > 0) {
                                            DatabaseHelper.getInstance().storePlaylist(
                                                    fetchedLists.get(0));
                                        }
                                    } else if (data.getType()
                                            == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDALBUMS) {
                                        List<Album> fetchedAlbums = data.getConvertedResultMap()
                                                .get(HatchetInfoPlugin.HATCHET_ALBUMS);
                                        if (fetchedAlbums.size() > 0) {
                                            DatabaseHelper.getInstance()
                                                    .storeStarredAlbums(fetchedAlbums);
                                        }
                                    } else if (data.getType()
                                            == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDARTISTS) {
                                        List<Artist> fetchedArtists = data.getConvertedResultMap()
                                                .get(HatchetInfoPlugin.HATCHET_ARTISTS);
                                        if (fetchedArtists.size() > 0) {
                                            DatabaseHelper.getInstance()
                                                    .storeStarredArtists(fetchedArtists);
                                        }
                                    }
                                }
                            }
                    );
                }
            } else if (InfoSystem.INFOSYSTEM_OPLOGISEMPTIED.equals(intent.getAction())) {
                CollectionManager.this.fetchLovedItemsPlaylist();
                CollectionManager.this.fetchStarredAlbums();
                CollectionManager.this.fetchStarredArtists();
            } else if (DatabaseHelper.PLAYLISTSDATASOURCE_RESULTSREPORTED
                    .equals(intent.getAction())) {
                CollectionManager.this.updatePlaylists();
            }
        }
    }

    private CollectionManager() {
    }

    public static CollectionManager getInstance() {
        if (instance == null) {
            synchronized (CollectionManager.class) {
                if (instance == null) {
                    instance = new CollectionManager();
                }
            }
        }
        return instance;
    }

    public void ensureInit() {
        if (!mInitialized) {
            mInitialized = true;

            addCollection(new UserCollection());
            addCollection(new HatchetCollection());

            updateLovedItemsPlaylist();
            updatePlaylists();
            fetchHatchetPlaylists();
            fetchStarredAlbums();
            fetchStarredArtists();

            mCollectionManagerReceiver = new CollectionManagerReceiver();
            TomahawkApp.getContext().registerReceiver(mCollectionManagerReceiver,
                    new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED));
            TomahawkApp.getContext().registerReceiver(mCollectionManagerReceiver,
                    new IntentFilter(InfoSystem.INFOSYSTEM_OPLOGISEMPTIED));
            TomahawkApp.getContext().registerReceiver(mCollectionManagerReceiver,
                    new IntentFilter(DatabaseHelper.PLAYLISTSDATASOURCE_RESULTSREPORTED));
        }
    }

    public void addCollection(Collection collection) {
        mCollections.put(collection.getId(), collection);
    }

    public Collection getCollection(String collectionId) {
        return mCollections.get(collectionId);
    }

    public ArrayList<Collection> getCollections() {
        return new ArrayList<Collection>(mCollections.values());
    }

    /**
     * @return A {@link java.util.List} of all {@link Playlist}s
     * in this {@link org.tomahawk.libtomahawk.collection.CollectionManager}
     */
    public ArrayList<Playlist> getLocalPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<Playlist>();
        for (Playlist playlist : mPlaylists.values()) {
            if (!playlist.isHatchetPlaylist()) {
                playlists.add(playlist);
            }
        }
        Collections.sort(playlists,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return playlists;
    }

    /**
     * @return A {@link java.util.List} of all {@link Playlist}s
     * in this {@link org.tomahawk.libtomahawk.collection.CollectionManager}
     */
    public ArrayList<Playlist> getHatchetPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<Playlist>();
        for (Playlist playlist : mPlaylists.values()) {
            if (playlist.isHatchetPlaylist()) {
                playlists.add(playlist);
            }
        }
        Collections.sort(playlists,
                new TomahawkListItemComparator(TomahawkListItemComparator.COMPARE_ALPHA));
        return playlists;
    }

    /**
     * Store the PlaybackService's currentPlaylist
     */
    public void setCachedPlaylist(Playlist playlist) {
        DatabaseHelper.getInstance().storePlaylist(playlist);
    }

    /**
     * @return the previously cached {@link Playlist}
     */
    public Playlist getCachedPlaylist() {
        return DatabaseHelper.getInstance().getCachedPlaylist();
    }

    /**
     * Remove or add a lovedItem-query from the LovedItems-Playlist, depending on whether or not
     * it is already a lovedItem
     */
    public void toggleLovedItem(Query query) {
        boolean doSweetSweetLovin = !DatabaseHelper.getInstance().isItemLoved(query);
        DatabaseHelper.getInstance().setLovedItem(query, doSweetSweetLovin);
        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance().sendSocialActionPostStruct(hatchetAuthUtils, query,
                HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE, doSweetSweetLovin);
    }

    public void toggleLovedItem(Artist artist) {
        boolean doSweetSweetLovin = !DatabaseHelper.getInstance().isItemLoved(artist);
        DatabaseHelper.getInstance().setLovedItem(artist, doSweetSweetLovin);
        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance().sendSocialActionPostStruct(hatchetAuthUtils, artist,
                HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE, doSweetSweetLovin);
    }

    public void toggleLovedItem(Album album) {
        boolean doSweetSweetLovin = !DatabaseHelper.getInstance().isItemLoved(album);
        DatabaseHelper.getInstance().setLovedItem(album, doSweetSweetLovin);
        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance().sendSocialActionPostStruct(hatchetAuthUtils, album,
                HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE, doSweetSweetLovin);
    }

    /**
     * Update the loved items user-playlist and the contained queries.
     */
    private void updateLovedItemsPlaylist() {
        Playlist lovedItemsPlayList = DatabaseHelper.getInstance().getLovedItemsPlaylist();
        if (lovedItemsPlayList == null) {
            // If we don't yet have a Playlist to store loved items, we create and store an
            // empty Playlist here
            DatabaseHelper.getInstance().storePlaylist(
                    Playlist.fromQueryList(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID,
                            DatabaseHelper.LOVEDITEMS_PLAYLIST_NAME,
                            new ArrayList<Query>())
            );
        }
    }

    /**
     * Fetch the lovedItems Playlist from the Hatchet API and store it in the local db, if the
     * log of pending operations is empty. Meaning if every love/unlove has already been delivered
     * to the API.
     */
    public void fetchLovedItemsPlaylist() {
        if (DatabaseHelper.getInstance().getLoggedOps().isEmpty()) {
            mCorrespondingRequestIds.add(InfoSystem.getInstance().resolve(
                    InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS, null));
        } else {
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the starred artists from the Hatchet API and store it in the local db, if the log of
     * pending operations is empty. Meaning if every love/unlove has already been delivered to the
     * API.
     */
    public void fetchStarredArtists() {
        if (DatabaseHelper.getInstance().getLoggedOps().isEmpty()) {
            mCorrespondingRequestIds.add(InfoSystem.getInstance().resolveStarredArtists(null));
        } else {
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the starred albums from the Hatchet API and store it in the local db, if the log of
     * pending operations is empty. Meaning if every love/unlove has already been delivered to the
     * API.
     */
    public void fetchStarredAlbums() {
        if (DatabaseHelper.getInstance().getLoggedOps().isEmpty()) {
            mCorrespondingRequestIds.add(InfoSystem.getInstance().resolveStarredAlbums(null));
        } else {
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch all user {@link Playlist} from the app's
     * database via our helper class {@link org.tomahawk.libtomahawk.database.DatabaseHelper}
     */
    private void updatePlaylists() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DatabaseHelper.getInstance()) {
                    ArrayList<Playlist> localPlaylists = DatabaseHelper.getInstance()
                            .getLocalPlaylists();
                    for (Playlist playlist : localPlaylists) {
                        mPlaylists.put(playlist.getId(), playlist);
                    }
                    ArrayList<Playlist> hatchetPlaylists = DatabaseHelper.getInstance()
                            .getHatchetPlaylists();
                    for (Playlist playlist : hatchetPlaylists) {
                        mPlaylists.put(playlist.getId(), playlist);
                    }
                    for (Playlist playlist : mPlaylists.values()) {
                        if (!localPlaylists.contains(playlist) && !hatchetPlaylists
                                .contains(playlist)) {
                            mPlaylists.remove(playlist.getId());
                        }
                    }
                    TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_UPDATED));
                }
            }
        }).start();
    }

    /**
     * Fetch the Playlists from the Hatchet API and store it in the local db.
     */
    public void fetchHatchetPlaylists() {
        mCorrespondingRequestIds.add(InfoSystem.getInstance().resolve(
                InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS, null));
    }

    /**
     * Fetch the Playlist entries from the Hatchet API and store them in the local db.
     */
    public void fetchHatchetPlaylistEntries(String playlistId) {
        Multimap<String, String> params = HashMultimap.create(1, 1);
        params.put(HatchetInfoPlugin.HATCHET_PARAM_ID, playlistId);
        String requestid = InfoSystem.getInstance()
                .resolve(InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES, params);
        mCorrespondingRequestIds.add(requestid);
        mRequestIdPlaylistMap.put(requestid, playlistId);
    }
}
