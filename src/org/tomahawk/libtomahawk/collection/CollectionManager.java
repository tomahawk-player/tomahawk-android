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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.QueryParams;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

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

    public static final String TAG = CollectionManager.class.getSimpleName();

    public static final String COLLECTION_UPDATED
            = "org.tomahawk.tomahawk_android.COLLECTION_UPDATED";

    public static final String COLLECTION_ADDED
            = "org.tomahawk.tomahawk_android.COLLECTION_ADDED";

    public static final String PLAYLISTS_UPDATED
            = "org.tomahawk.tomahawk_android.PLAYLISTS_UPDATED";

    public static final String COLLECTION_ID = "org.tomahawk.tomahawk_android.collection_id";

    private static class Holder {
<<<<<<< HEAD

        private static final CollectionManager instance = new CollectionManager();

=======

        private static final CollectionManager instance = new CollectionManager();

>>>>>>> upstream/master
    }

    private ConcurrentHashMap<String, Collection> mCollections
            = new ConcurrentHashMap<String, Collection>();

    private ConcurrentHashMap<String, Playlist> mPlaylists
            = new ConcurrentHashMap<String, Playlist>();

    private HashSet<String> mCorrespondingRequestIds = new HashSet<String>();


    private CollectionManagerReceiver mCollectionManagerReceiver = new CollectionManagerReceiver();

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
                                    InfoRequestData infoRequestData =
                                            InfoSystem.getInstance().getInfoRequestById(requestId);
                                    handleHatchetPlaylistResponse(infoRequestData);
                                }
                            }
                    );
                }
            } else if (InfoSystem.INFOSYSTEM_OPLOGISEMPTIED.equals(intent.getAction())) {
                ArrayList<Integer> requestTypes = intent.getIntegerArrayListExtra(
                        InfoSystem.INFOSYSTEM_OPLOGISEMPTIED_REQUESTTYPES);
                for (Integer requestType : requestTypes) {
                    if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS) {
                        CollectionManager.this.fetchStarredArtists();
                        CollectionManager.this.fetchStarredAlbums();
                        CollectionManager.this.fetchLovedItemsPlaylist();
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES) {
                        HatchetAuthenticatorUtils hatchetAuthUtils =
                                (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                        .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                        InfoSystem.getInstance()
                                .resolvePlaybackLog(hatchetAuthUtils.getLoggedInUser());
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS) {
                        CollectionManager.this.fetchPlaylists();
                    } else if (requestType
                            == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
                        ArrayList<String> playlistIds = intent.getStringArrayListExtra(
                                InfoSystem.INFOSYSTEM_OPLOGISEMPTIED_IDS);
                        for (String playlistId : playlistIds) {
                            CollectionManager.this.fetchHatchetPlaylistEntries(
                                    Playlist.getPlaylistById(playlistId));
                        }
                    }
                }
            } else if (DatabaseHelper.PLAYLISTSDATASOURCE_RESULTSREPORTED
                    .equals(intent.getAction())) {
                CollectionManager.this.updatePlaylists();
            } else if (HatchetAuthenticatorUtils.STORED_USER_ID.equals(intent.getAction())) {
                HatchetAuthenticatorUtils hatchetAuthUtils =
                        (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                InfoSystem.getInstance().resolvePlaybackLog(hatchetAuthUtils.getLoggedInUser());
                fetchAll();
            }
        }
    }

    private CollectionManager() {
        addCollection(new UserCollection());
        addCollection(new HatchetCollection());

        ensureLovedItemsPlaylist();
        updatePlaylists();
<<<<<<< HEAD
        fetchPlaylists();
        fetchLovedItemsPlaylist();
        fetchStarredAlbums();
        fetchStarredArtists();
=======
        fetchAll();
>>>>>>> upstream/master

        TomahawkApp.getContext().registerReceiver(mCollectionManagerReceiver,
                new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED));
        TomahawkApp.getContext().registerReceiver(mCollectionManagerReceiver,
                new IntentFilter(InfoSystem.INFOSYSTEM_OPLOGISEMPTIED));
        TomahawkApp.getContext().registerReceiver(mCollectionManagerReceiver,
                new IntentFilter(DatabaseHelper.PLAYLISTSDATASOURCE_RESULTSREPORTED));
<<<<<<< HEAD
=======
    }

    public void fetchAll() {
        fetchPlaylists();
        fetchLovedItemsPlaylist();
        fetchStarredAlbums();
        fetchStarredArtists();
>>>>>>> upstream/master
    }

    public static CollectionManager getInstance() {
        return Holder.instance;
    }

    public void addCollection(Collection collection) {
        mCollections.put(collection.getId(), collection);
        TomahawkApp.getContext().sendBroadcast(new Intent(COLLECTION_ADDED));
    }

    public Collection getCollection(String collectionId) {
        return mCollections.get(collectionId);
    }

    public ArrayList<Collection> getCollections() {
        return new ArrayList<Collection>(mCollections.values());
    }

    /**
     * @return A {@link java.util.List} of all {@link Playlist}s in this {@link
     * org.tomahawk.libtomahawk.collection.CollectionManager}
     */
    public ArrayList<Playlist> getPlaylists() {
        ArrayList<Playlist> playlists = new ArrayList<Playlist>(mPlaylists.values());
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
     * Remove or add a lovedItem-query from the LovedItems-Playlist, depending on whether or not it
     * is already a lovedItem
     */
    public void toggleLovedItem(Query query) {
        boolean doSweetSweetLovin = !DatabaseHelper.getInstance().isItemLoved(query);
        Log.d(TAG, "Hatchet sync - " + (doSweetSweetLovin ? "loved" : "unloved") + " track "
                + query.getName() + " by " + query.getArtist().getName() + " on "
                + query.getAlbum().getName());
        DatabaseHelper.getInstance().setLovedItem(query, doSweetSweetLovin);
        sendCollectionUpdatedBroadcast(null, query);
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance().sendSocialActionPostStruct(hatchetAuthUtils, query,
                HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE, doSweetSweetLovin);
    }

    public void toggleLovedItem(Artist artist) {
        boolean doSweetSweetLovin = !DatabaseHelper.getInstance().isItemLoved(artist);
        Log.d(TAG, "Hatchet sync - " + (doSweetSweetLovin ? "starred" : "unstarred") + " artist "
                + artist.getName());
        DatabaseHelper.getInstance().setLovedItem(artist, doSweetSweetLovin);
        sendCollectionUpdatedBroadcast(null, artist);
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance().sendSocialActionPostStruct(hatchetAuthUtils, artist,
                HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE, doSweetSweetLovin);
    }

    public void toggleLovedItem(Album album) {
        boolean doSweetSweetLovin = !DatabaseHelper.getInstance().isItemLoved(album);
        Log.d(TAG, "Hatchet sync - " + (doSweetSweetLovin ? "starred" : "unstarred") + " album "
                + album.getName() + " by " + album.getArtist().getName());
        DatabaseHelper.getInstance().setLovedItem(album, doSweetSweetLovin);
        sendCollectionUpdatedBroadcast(null, album);
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance().sendSocialActionPostStruct(hatchetAuthUtils, album,
                HatchetInfoPlugin.HATCHET_SOCIALACTION_TYPE_LOVE, doSweetSweetLovin);
    }

    /**
     * Update the loved items user-playlist and the contained queries.
     */
    private void ensureLovedItemsPlaylist() {
        Playlist lovedItemsPlayList =
                DatabaseHelper.getInstance().getPlaylist(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID);
        if (lovedItemsPlayList == null) {
            // If we don't yet have a Playlist to store loved items, we create and store an
            // empty Playlist here
            Playlist playlist = Playlist.fromQueryList(DatabaseHelper.LOVEDITEMS_PLAYLIST_NAME,
                    new ArrayList<Query>());
            playlist.setId(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID);
            DatabaseHelper.getInstance().storePlaylist(playlist);
        }
    }

    /**
     * Fetch the lovedItems Playlist from the Hatchet API and store it in the local db, if the log
     * of pending operations is empty. Meaning if every love/unlove has already been delivered to
     * the API.
     */
    public void fetchLovedItemsPlaylist() {
        HatchetAuthenticatorUtils hatchetAuthUtils = (HatchetAuthenticatorUtils)
                AuthenticatorManager.getInstance().getAuthenticatorUtils(
                        TomahawkApp.PLUGINNAME_HATCHET);
        if (DatabaseHelper.getInstance().getLoggedOpsCount() == 0) {
            Log.d(TAG, "Hatchet sync - fetching loved tracks");
            mCorrespondingRequestIds.add(InfoSystem.getInstance().resolveFavorites(
                    hatchetAuthUtils.getLoggedInUser()));
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching loved tracks");
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the starred artists from the Hatchet API and store it in the local db, if the log of
     * pending operations is empty. Meaning if every love/unlove has already been delivered to the
     * API.
     */
    public void fetchStarredArtists() {
        HatchetAuthenticatorUtils hatchetAuthUtils = (HatchetAuthenticatorUtils)
                AuthenticatorManager.getInstance().getAuthenticatorUtils(
                        TomahawkApp.PLUGINNAME_HATCHET);
        if (DatabaseHelper.getInstance().getLoggedOpsCount() == 0) {
            Log.d(TAG, "Hatchet sync - fetching starred artists");
            mCorrespondingRequestIds.add(InfoSystem.getInstance().
                    resolveStarredArtists(hatchetAuthUtils.getLoggedInUser()));
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching starred artists");
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the starred albums from the Hatchet API and store it in the local db, if the log of
     * pending operations is empty. Meaning if every love/unlove has already been delivered to the
     * API.
     */
    public void fetchStarredAlbums() {
        HatchetAuthenticatorUtils hatchetAuthUtils = (HatchetAuthenticatorUtils)
                AuthenticatorManager.getInstance().getAuthenticatorUtils(
                        TomahawkApp.PLUGINNAME_HATCHET);
        if (DatabaseHelper.getInstance().getLoggedOpsCount() == 0) {
            Log.d(TAG, "Hatchet sync - fetching starred albums");
            mCorrespondingRequestIds.add(InfoSystem.getInstance()
                    .resolveStarredAlbums(hatchetAuthUtils.getLoggedInUser()));
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching starred albums");
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch all user {@link Playlist} from the app's database via our helper class {@link
     * org.tomahawk.libtomahawk.database.DatabaseHelper}
     */
    private void updatePlaylists() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DatabaseHelper.getInstance()) {
                    mPlaylists.clear();
                    ArrayList<Playlist> playlists = DatabaseHelper.getInstance().getPlaylists();
                    for (Playlist playlist : playlists) {
                        mPlaylists.put(playlist.getId(), playlist);
                    }
                    Log.d(TAG, "Hatchet sync - read playlists from database, count: "
                            + mPlaylists.size());
                    sendPlaylistsUpdatedBroadcast();
                }
            }
        }).start();
    }

    /**
     * Fetch the Playlists from the Hatchet API and store it in the local db.
     */
    public void fetchPlaylists() {
        HatchetAuthenticatorUtils hatchetAuthUtils = (HatchetAuthenticatorUtils)
                AuthenticatorManager.getInstance().getAuthenticatorUtils(
                        TomahawkApp.PLUGINNAME_HATCHET);
        if (DatabaseHelper.getInstance().getLoggedOpsCount() == 0) {
            Log.d(TAG, "Hatchet sync - fetching playlists");
            mCorrespondingRequestIds.add(InfoSystem.getInstance()
                    .resolvePlaylists(hatchetAuthUtils.getLoggedInUser()));
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching playlists");
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the Playlist entries from the Hatchet API and store them in the local db.
     */
    public void fetchHatchetPlaylistEntries(Playlist playlist) {
        if (DatabaseHelper.getInstance().getLoggedOpsCount() == 0) {
            if (playlist.getHatchetId() != null) {
                Log.d(TAG, "Hatchet sync - fetching entry list for playlist \"" + playlist.getName()
                        + "\", hatchetId: " + playlist.getHatchetId());
                QueryParams params = new QueryParams();
                params.playlist_id = playlist.getHatchetId();
                String requestid = InfoSystem.getInstance().resolve(
                        InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES, params, playlist);
                mCorrespondingRequestIds.add(requestid);
            } else {
                Log.d(TAG, "Hatchet sync - couldn't fetch entry list for playlist \""
                        + playlist.getName() + "\" because hatchetId was null");
            }
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching entry list for playlist"
                    + " \"" + playlist.getName() + "\", hatchetId: " + playlist.getHatchetId());
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.getInstance().sendLoggedOps(hatchetAuthUtils);
        }
    }

    public void handleHatchetPlaylistResponse(InfoRequestData data) {
        if (data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS) {
            ArrayList<Playlist> storedLists = DatabaseHelper.getInstance().getPlaylists();
            HashMap<String, Playlist> storedListsMap = new HashMap<String, Playlist>();
            for (Playlist storedList : storedLists) {
                if (storedListsMap.containsKey(storedList.getHatchetId())) {
                    Log.e(TAG, "Hatchet sync - playlist \"" + storedList.getName()
                            + "\" is duplicated ... deleting");
                    DatabaseHelper.getInstance().deletePlaylist(storedList.getId());
                } else {
                    storedListsMap.put(storedList.getHatchetId(), storedList);
                }
            }
            List<Playlist> fetchedLists = data.getResultList(Playlist.class);
            Log.d(TAG, "Hatchet sync - playlist count in database: " + storedLists.size()
                    + ", playlist count on Hatchet: " + fetchedLists.size());
            for (Playlist fetchedList : fetchedLists) {
                Playlist storedList = storedListsMap.remove(fetchedList.getHatchetId());
                if (storedList == null) {
                    Log.d(TAG, "Hatchet sync - playlist \"" + fetchedList.getName()
                            + "\" didn't exist in database ... storing");
                    DatabaseHelper.getInstance().storePlaylist(fetchedList);
                    fetchHatchetPlaylistEntries(fetchedList);
                } else if (!storedList.getCurrentRevision()
                        .equals(fetchedList.getCurrentRevision())) {
                    Log.d(TAG, "Hatchet sync - revision differed for playlist \""
                            + fetchedList.getName() + "\" ... fetching entries");
                    fetchHatchetPlaylistEntries(storedList);
                } else if (!storedList.getName().equals(fetchedList.getName())) {
                    Log.d(TAG, "Hatchet sync - title differed for playlist \""
                            + storedList.getName() + "\", new name: \"" + fetchedList.getName()
                            + "\" ... renaming");
                    DatabaseHelper.getInstance().renamePlaylist(storedList, fetchedList.getName());
                }
            }
            for (Playlist storedList : storedListsMap.values()) {
                Log.d(TAG, "Hatchet sync - playlist \"" + storedList.getName()
                        + "\" doesn't exist on Hatchet ... deleting");
                DatabaseHelper.getInstance().deletePlaylist(storedList.getId());
            }
        } else if (data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_ENTRIES) {
            Playlist filledList = data.getResult(Playlist.class);
            if (filledList != null) {
                Log.d(TAG, "Hatchet sync - received entry list for playlist \""
                        + filledList.getName() + "\", hatchetId: " + filledList.getHatchetId()
                        + ", count: " + filledList.getEntries().size());
                DatabaseHelper.getInstance().storePlaylist(filledList);
            }
        } else if (data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
            Playlist fetchedList = data.getResult(Playlist.class);
            if (fetchedList != null) {
                HatchetAuthenticatorUtils hatchetAuthUtils =
                        (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                String userName = hatchetAuthUtils.getUserName();
                fetchedList.setName(userName + TomahawkApp.getContext()
                        .getString(R.string.users_favorites_suffix));
                fetchedList.setId(DatabaseHelper.LOVEDITEMS_PLAYLIST_ID);
                Log.d(TAG, "Hatchet sync - received list of loved tracks, count: "
                        + fetchedList.getEntries().size());
                DatabaseHelper.getInstance().storePlaylist(fetchedList, true);
            }
        } else if (data.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDALBUMS) {
            List<Album> fetchedAlbums = data.getResultList(Album.class);
            Log.d(TAG, "Hatchet sync - received list of starred albums, count: "
                    + fetchedAlbums.size());
            DatabaseHelper.getInstance().storeStarredAlbums(fetchedAlbums);
        } else if (data.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDARTISTS) {
            List<Artist> fetchedArtists = data.getResultList(Artist.class);
            Log.d(TAG, "Hatchet sync - received list of starred artists, count: "
                    + fetchedArtists.size());
            DatabaseHelper.getInstance().storeStarredArtists(fetchedArtists);
        }
    }

    public void deletePlaylist(String playlistId) {
        if (Playlist.getPlaylistById(playlistId) != null) {
            Log.d(TAG, "Hatchet sync - deleting playlist \""
                    + Playlist.getPlaylistById(playlistId).getName() + "\", id: " + playlistId);
        } else {
            Log.e(TAG, "Hatchet sync - couldn't delete playlist with id: " + playlistId);
        }
        DatabaseHelper.getInstance().deletePlaylist(playlistId);
        sendCollectionUpdatedBroadcast(null, Playlist.getPlaylistById(playlistId));
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance().deletePlaylist(hatchetAuthUtils, playlistId);
    }

    public void createPlaylist(Playlist playlist) {
        Log.d(TAG, "Hatchet sync - creating playlist \"" + playlist.getName() + "\", id: "
                + playlist.getId() + " with " + playlist.getEntries().size() + " entries");
        DatabaseHelper.getInstance().storePlaylist(playlist);
        CollectionManager.sendCollectionUpdatedBroadcast(null, playlist);
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance()
                .sendPlaylistPostStruct(hatchetAuthUtils, playlist.getId(), playlist.getName());
        for (PlaylistEntry entry : playlist.getEntries()) {
            InfoSystem.getInstance().sendPlaylistEntriesPostStruct(hatchetAuthUtils,
                    playlist.getId(), entry.getName(), entry.getArtist().getName(),
                    entry.getAlbum().getName());
        }
    }

    public void addPlaylistEntries(String playlistId, ArrayList<PlaylistEntry> entries) {
        if (Playlist.getPlaylistById(playlistId) != null) {
            Log.d(TAG, "Hatchet sync - adding " + entries.size() + " entries to playlist \""
                    + Playlist.getPlaylistById(playlistId).getName() + "\", id: " + playlistId);
        } else {
            Log.e(TAG, "Hatchet sync - couldn't add entries to playlist with id: " + playlistId);
        }
        DatabaseHelper.getInstance().addEntriesToPlaylist(playlistId, entries);
        sendCollectionUpdatedBroadcast(null, Playlist.getPlaylistById(playlistId));
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        for (PlaylistEntry entry : entries) {
            InfoSystem.getInstance().sendPlaylistEntriesPostStruct(hatchetAuthUtils, playlistId,
                    entry.getName(), entry.getArtist().getName(), entry.getAlbum().getName());
        }
    }

    public void deletePlaylistEntry(String playlistId, String entryId) {
        if (Playlist.getPlaylistById(playlistId) != null) {
            Log.d(TAG, "Hatchet sync - deleting entry with id \"" + entryId + "\" in playlist \""
                    + Playlist.getPlaylistById(playlistId).getName() + "\", id: " + playlistId);
        } else {
            Log.e(TAG, "Hatchet sync - couldn't delete entry in playlist with id: " + playlistId);
        }
        DatabaseHelper.getInstance().deleteEntryInPlaylist(playlistId, entryId);
        sendCollectionUpdatedBroadcast(null, Playlist.getPlaylistById(playlistId));
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.getInstance().deletePlaylistEntry(hatchetAuthUtils, playlistId, entryId);
    }

    public List<Collection> getAvailableCollections(Album album) {
        List<Collection> collections = new ArrayList<Collection>();
        for (Collection collection : mCollections.values()) {
            if (TomahawkApp.PLUGINNAME_HATCHET.equals(collection.getId())
                    || collection.getAlbumTracks(album, false).size() > 0) {
                collections.add(collection);
            }
        }
        return collections;
    }

    public List<Collection> getAvailableCollections(Artist artist) {
        List<Collection> collections = new ArrayList<Collection>();
        for (Collection collection : mCollections.values()) {
            if (TomahawkApp.PLUGINNAME_HATCHET.equals(collection.getId())
                    || collection.getArtistAlbums(artist, false).size() > 0) {
                collections.add(collection);
            }
        }
        return collections;
    }

    public static void sendCollectionUpdatedBroadcast(Collection collection,
            TomahawkListItem updatedItem) {
        Intent intent = new Intent(CollectionManager.COLLECTION_UPDATED);
        if (collection != null) {
            intent.putExtra(CollectionManager.COLLECTION_ID, collection.getId());
        }
        String key = null;
        if (updatedItem instanceof Playlist) {
            key = TomahawkFragment.TOMAHAWK_PLAYLIST_KEY;
        } else if (updatedItem instanceof Album) {
            key = TomahawkFragment.TOMAHAWK_ALBUM_KEY;
        } else if (updatedItem instanceof Artist) {
            key = TomahawkFragment.TOMAHAWK_ARTIST_KEY;
        } else if (updatedItem instanceof Query) {
            key = TomahawkFragment.TOMAHAWK_QUERY_KEY;
        }
        if (key != null) {
            intent.putExtra(key, updatedItem.getCacheKey());
        }
        TomahawkApp.getContext().sendBroadcast(intent);
    }

    public static void sendPlaylistsUpdatedBroadcast() {
        TomahawkApp.getContext().sendBroadcast(new Intent(CollectionManager.PLAYLISTS_UPDATED));
    }
}
