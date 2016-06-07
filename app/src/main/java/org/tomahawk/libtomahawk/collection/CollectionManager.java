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

import org.jdeferred.AlwaysCallback;
import org.jdeferred.Deferred;
import org.jdeferred.DoneCallback;
import org.jdeferred.Promise;
import org.jdeferred.android.AndroidDeferredManager;
import org.jdeferred.multiple.MultipleResults;
import org.jdeferred.multiple.OneReject;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.QueryParams;
import org.tomahawk.libtomahawk.infosystem.Relationship;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ADeferredObject;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

public class CollectionManager {

    public static final String TAG = CollectionManager.class.getSimpleName();

    private static class Holder {

        private static final CollectionManager instance = new CollectionManager();

    }

    public static class AddedOrRemovedEvent {

        public Collection mCollection;

    }

    public static class UpdatedEvent {

        public Collection mCollection;

        public HashSet<String> mUpdatedItemIds;

    }

    private final ConcurrentHashMap<String, Collection> mCollections
            = new ConcurrentHashMap<>();

    private final HashSet<String> mCorrespondingRequestIds = new HashSet<>();

    private final HashSet<String> mResolvingHatchetIds = new HashSet<>();

    private final Set<String> mShowAsDeletedPlaylistMap =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final Set<String> mShowAsCreatedPlaylistMap =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private AndroidDeferredManager mDeferredManager = new AndroidDeferredManager();

    private CollectionManager() {
        EventBus.getDefault().register(this);

        addCollection(new UserCollection());
        addCollection(new HatchetCollection());

        fillUserWithStoredPlaylists();

        fetchPlaylists();
        fetchLovedItemsPlaylist();
        fetchStarredAlbums();
        fetchStarredArtists();
    }

    public static CollectionManager get() {
        return Holder.instance;
    }

    @SuppressWarnings("unused")
    public void onEventAsync(InfoSystem.OpLogIsEmptiedEvent event) {
        for (Integer requestType : event.mRequestTypes) {
            if (requestType
                    == InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS) {
                fetchStarredArtists();
                fetchStarredAlbums();
                fetchLovedItemsPlaylist();
            } else if (requestType
                    == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS
                    || requestType
                    == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
                fetchPlaylists();
            }
        }
    }

    @SuppressWarnings("unused")
    public void onEventAsync(final InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            mCorrespondingRequestIds.remove(event.mInfoRequestData.getRequestId());
            handleHatchetPlaylistResponse(event.mInfoRequestData);
        }
    }

    @SuppressWarnings("unused")
    public void onEventAsync(HatchetAuthenticatorUtils.UserLoginEvent event) {
        fetchPlaylists();
        fetchLovedItemsPlaylist();
        fetchStarredAlbums();
        fetchStarredArtists();
    }

    @SuppressWarnings("unused")
    public void onEventAsync(final DatabaseHelper.PlaylistsUpdatedEvent event) {
        fillUserWithStoredPlaylists();
    }

    /**
     * Fill the logged-in User object with the playlists we have stored in the database.
     */
    public Promise<Void, Throwable, Void> fillUserWithStoredPlaylists() {
        final ADeferredObject<Void, Throwable, Void> deferred = new ADeferredObject<>();
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(final User user) {
                TomahawkRunnable r = new TomahawkRunnable(
                        TomahawkRunnable.PRIORITY_IS_DATABASEACTION) {
                    @Override
                    public void run() {
                        user.setPlaylists(DatabaseHelper.get().getPlaylists());
                        Playlist favorites = DatabaseHelper.get().getLovedItemsPlaylist();
                        if (favorites != null) {
                            user.setFavorites(favorites);
                        }
                        deferred.resolve(null);
                    }
                };
                ThreadManager.get().execute(r);
            }
        });
        return deferred;
    }

    public void addCollection(Collection collection) {
        mCollections.put(collection.getId(), collection);
        AddedOrRemovedEvent event = new AddedOrRemovedEvent();
        event.mCollection = collection;
        EventBus.getDefault().post(event);
    }

    public void removeCollection(Collection collection) {
        mCollections.remove(collection.getId());
        AddedOrRemovedEvent event = new AddedOrRemovedEvent();
        event.mCollection = collection;
        EventBus.getDefault().post(event);
    }

    public Collection getCollection(String collectionId) {
        return mCollections.get(collectionId);
    }

    /**
     * Convenience method
     */
    public UserCollection getUserCollection() {
        return (UserCollection) mCollections.get(TomahawkApp.PLUGINNAME_USERCOLLECTION);
    }

    /**
     * Convenience method
     */
    public HatchetCollection getHatchetCollection() {
        return (HatchetCollection) mCollections.get(TomahawkApp.PLUGINNAME_HATCHET);
    }

    public java.util.Collection<Collection> getCollections() {
        return mCollections.values();
    }

    /**
     * Remove or add a lovedItem-query from the LovedItems-Playlist, depending on whether or not it
     * is already a lovedItem
     */
    public void setLovedItem(final Query query, boolean loved) {
        boolean isLoved = DatabaseHelper.get().isItemLoved(query);
        if (loved != isLoved) {
            toggleLovedItem(query);
        } else {
            Log.e(TAG, "Track " + query.getName() + " by " + query.getArtist().getName() + " on "
                    + query.getAlbum().getName() + " was already loved!");
        }
    }

    /**
     * Remove or add a lovedItem-query from the LovedItems-Playlist, depending on whether or not it
     * is already a lovedItem
     */
    public void toggleLovedItem(final Query query) {
        boolean doSweetSweetLovin = !DatabaseHelper.get().isItemLoved(query);
        Log.d(TAG, "Hatchet sync - " + (doSweetSweetLovin ? "loved" : "unloved") + " track "
                + query.getName() + " by " + query.getArtist().getName() + " on "
                + query.getAlbum().getName());
        DatabaseHelper.get().setLovedItem(query, doSweetSweetLovin);
        UpdatedEvent event = new UpdatedEvent();
        event.mUpdatedItemIds = new HashSet<>();
        event.mUpdatedItemIds.add(query.getCacheKey());
        EventBus.getDefault().post(event);
        final AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        if (doSweetSweetLovin) {
            InfoSystem.get().sendRelationshipPostStruct(hatchetAuthUtils, query);
        } else {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User result) {
                    Relationship relationship = result.getRelationship(query);
                    if (relationship == null) {
                        Log.e(TAG, "Can't unlove track, because there's no relationshipId"
                                + " associated with it.");
                        return;
                    }
                    InfoSystem.get().deleteRelationship(
                            hatchetAuthUtils, relationship.getCacheKey());
                }
            });
        }
    }

    public void toggleLovedItem(final Artist artist) {
        boolean doSweetSweetLovin = !getUserCollection().isLoved(artist);
        Log.d(TAG, "Hatchet sync - " + (doSweetSweetLovin ? "starred" : "unstarred") + " artist "
                + artist.getName());
        final AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        if (doSweetSweetLovin) {
            List<Artist> artists = new ArrayList<>();
            artists.add(artist);
            List<Long> lastModifieds = new ArrayList<>();
            lastModifieds.add(System.currentTimeMillis());
            getUserCollection().addLovedArtists(artists, lastModifieds);
            InfoSystem.get().sendRelationshipPostStruct(hatchetAuthUtils, artist);
        } else {
            getUserCollection().removeLoved(artist);
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User result) {
                    Relationship relationship = result.getRelationship(artist);
                    if (relationship == null) {
                        Log.e(TAG, "Can't unlove artist, because there's no relationship"
                                + " associated with it.");
                        return;
                    }
                    InfoSystem.get().deleteRelationship(
                            hatchetAuthUtils, relationship.getCacheKey());
                }
            });
        }
        UpdatedEvent event = new UpdatedEvent();
        event.mUpdatedItemIds = new HashSet<>();
        event.mUpdatedItemIds.add(artist.getCacheKey());
        EventBus.getDefault().post(event);
    }

    public void toggleLovedItem(final Album album) {
        boolean doSweetSweetLovin = !getUserCollection().isLoved(album);
        Log.d(TAG, "Hatchet sync - " + (doSweetSweetLovin ? "starred" : "unstarred") + " album "
                + album.getName() + " by " + album.getArtist().getName());
        final AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        if (doSweetSweetLovin) {
            List<Album> albums = new ArrayList<>();
            albums.add(album);
            List<Long> lastModifieds = new ArrayList<>();
            lastModifieds.add(System.currentTimeMillis());
            getUserCollection().addLovedAlbums(albums, lastModifieds);
            InfoSystem.get().sendRelationshipPostStruct(hatchetAuthUtils, album);
        } else {
            getUserCollection().removeLoved(album);
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User result) {
                    Relationship relationship = result.getRelationship(album);
                    if (relationship == null) {
                        Log.e(TAG, "Can't unlove album, because there's no relationship"
                                + " associated with it.");
                        return;
                    }
                    InfoSystem.get().deleteRelationship(
                            hatchetAuthUtils, relationship.getCacheKey());
                }
            });
        }
        UpdatedEvent event = new UpdatedEvent();
        event.mUpdatedItemIds = new HashSet<>();
        event.mUpdatedItemIds.add(album.getCacheKey());
        EventBus.getDefault().post(event);
    }

    /**
     * Fetch the lovedItems Playlist from the Hatchet API and store it in the local db, if the log
     * of pending operations is empty. Meaning if every love/unlove has already been delivered to
     * the API.
     */
    public void fetchLovedItemsPlaylist() {
        if (DatabaseHelper.get().getLoggedOpsCount() == 0) {
            Log.d(TAG, "Hatchet sync - fetching loved tracks");
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    String requestId = InfoSystem.get().resolveLovedItems(user);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            });
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching loved tracks");
            HatchetAuthenticatorUtils hatchetAuthUtils = (HatchetAuthenticatorUtils)
                    AuthenticatorManager.get().getAuthenticatorUtils(
                            TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.get().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the starred artists from the Hatchet API and store it in the local db, if the log of
     * pending operations is empty. Meaning if every love/unlove has already been delivered to the
     * API.
     */
    public void fetchStarredArtists() {
        if (DatabaseHelper.get().getLoggedOpsCount() == 0) {
            Log.d(TAG, "Hatchet sync - fetching starred artists");
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    String requestId = InfoSystem.get().resolveLovedArtists(user);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            });
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching starred artists");
            HatchetAuthenticatorUtils hatchetAuthUtils = (HatchetAuthenticatorUtils)
                    AuthenticatorManager.get().getAuthenticatorUtils(
                            TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.get().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the starred albums from the Hatchet API and store it in the local db, if the log of
     * pending operations is empty. Meaning if every love/unlove has already been delivered to the
     * API.
     */
    public void fetchStarredAlbums() {
        if (DatabaseHelper.get().getLoggedOpsCount() == 0) {
            Log.d(TAG, "Hatchet sync - fetching starred albums");
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    String requestId = InfoSystem.get().resolveLovedAlbums(user);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            });
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching starred albums");
            HatchetAuthenticatorUtils hatchetAuthUtils = (HatchetAuthenticatorUtils)
                    AuthenticatorManager.get().getAuthenticatorUtils(
                            TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.get().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the Playlists from the Hatchet API and store it in the local db.
     */
    public void fetchPlaylists() {
        if (DatabaseHelper.get().getLoggedOpsCount() == 0) {
            Log.d(TAG, "Hatchet sync - fetching playlists");
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    String requestId = InfoSystem.get().resolvePlaylists(user, true);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            });
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching playlists");
            HatchetAuthenticatorUtils hatchetAuthUtils = (HatchetAuthenticatorUtils)
                    AuthenticatorManager.get().getAuthenticatorUtils(
                            TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.get().sendLoggedOps(hatchetAuthUtils);
        }
    }

    /**
     * Fetch the Playlist entries from the Hatchet API and store them in the local db.
     */
    public void fetchHatchetPlaylistEntries(String playlistId) {
        String hatchetId = DatabaseHelper.get().getPlaylistHatchetId(playlistId);
        String name = DatabaseHelper.get().getPlaylistName(playlistId);
        if (DatabaseHelper.get().getLoggedOpsCount() == 0) {
            if (hatchetId != null) {
                if (!mResolvingHatchetIds.contains(hatchetId)) {
                    Log.d(TAG, "Hatchet sync - fetching entry list for playlist \"" + name
                            + "\", hatchetId: " + hatchetId);
                    mResolvingHatchetIds.add(hatchetId);
                    QueryParams params = new QueryParams();
                    params.playlist_local_id = playlistId;
                    params.playlist_id = hatchetId;
                    String requestid = InfoSystem.get().resolve(
                            InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES, params,
                            true);
                    mCorrespondingRequestIds.add(requestid);
                } else {
                    Log.d(TAG, "Hatchet sync - couldn't fetch entry list for playlist \""
                            + name + "\", because this playlist is already waiting for its entry "
                            + "list, hatchetId: " + hatchetId);
                }
            } else {
                Log.d(TAG, "Hatchet sync - couldn't fetch entry list for playlist \""
                        + name + "\" because hatchetId was null");
            }
        } else {
            Log.d(TAG, "Hatchet sync - sending logged ops before fetching entry list for playlist"
                    + " \"" + name + "\", hatchetId: " + hatchetId);
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.get().sendLoggedOps(hatchetAuthUtils);
        }
    }

    public void handleHatchetPlaylistResponse(InfoRequestData data) {
        if (data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS) {
            List<Playlist> storedLists = DatabaseHelper.get().getPlaylists();
            HashMap<String, Playlist> storedListsMap = new HashMap<>();
            for (Playlist storedList : storedLists) {
                if (storedListsMap.containsKey(storedList.getHatchetId())) {
                    Log.e(TAG, "Hatchet sync - playlist \"" + storedList.getName()
                            + "\" is duplicated ... deleting");
                    if (TextUtils.isEmpty(storedList.getCurrentRevision())) {
                        DatabaseHelper.get().deletePlaylist(storedList.getId());
                    } else {
                        Playlist otherStoredList = storedListsMap.get(storedList.getHatchetId());
                        DatabaseHelper.get().deletePlaylist(otherStoredList.getId());
                        storedListsMap.put(storedList.getHatchetId(), storedList);
                    }
                } else {
                    storedListsMap.put(storedList.getHatchetId(), storedList);
                }
            }
            List<User> results = data.getResultList(User.class);
            if (results == null || results.size() == 0) {
                Log.e(TAG, "Hatchet sync - something went wrong. Got no user object back :(");
                return;
            }
            User user = results.get(0);
            List<Playlist> fetchedLists = user.getPlaylists();
            Log.d(TAG, "Hatchet sync - playlist count in database: " + storedLists.size()
                    + ", playlist count on Hatchet: " + fetchedLists.size());
            for (final Playlist fetchedList : fetchedLists) {
                Playlist storedList = storedListsMap.remove(fetchedList.getHatchetId());
                if (storedList == null) {
                    if (mShowAsDeletedPlaylistMap.contains(fetchedList.getHatchetId())) {
                        Log.d(TAG, "Hatchet sync - " + fetchedList
                                + " didn't exist in database, but was marked as showAsDeleted so"
                                + " we don't store it.");
                    } else {
                        if (mShowAsCreatedPlaylistMap.contains(fetchedList.getHatchetId())) {
                            mShowAsCreatedPlaylistMap.remove(fetchedList.getHatchetId());
                            Log.d(TAG, "Hatchet sync - " + fetchedList
                                    + " is no longer marked as showAsCreated, since it seems to"
                                    + " have arrived on the server");
                        }
                        Log.d(TAG, "Hatchet sync - " + fetchedList + " didn't exist in database"
                                + " ... storing and fetching entries");
                        DatabaseHelper.get().storePlaylist(fetchedList, false);
                        fetchHatchetPlaylistEntries(fetchedList.getId());
                    }
                } else if (!storedList.getCurrentRevision()
                        .equals(fetchedList.getCurrentRevision())) {
                    Log.d(TAG, "Hatchet sync - revision differed for " + fetchedList
                            + " ... fetching entries");
                    fetchHatchetPlaylistEntries(storedList.getId());
                } else if (!storedList.getName().equals(fetchedList.getName())) {
                    Log.d(TAG, "Hatchet sync - title differed for stored " + storedList
                            + " and fetched " + fetchedList + " ... renaming");
                    DatabaseHelper.get().renamePlaylist(storedList, fetchedList.getName());
                }
            }
            for (Playlist storedList : storedListsMap.values()) {
                if (storedList.getHatchetId() == null
                        || !mShowAsCreatedPlaylistMap.contains(storedList.getHatchetId())) {
                    Log.d(TAG, "Hatchet sync - " + storedList
                            + " doesn't exist on Hatchet ... deleting");
                    DatabaseHelper.get().deletePlaylist(storedList.getId());
                } else {
                    Log.d(TAG, "Hatchet sync - " + storedList
                            + " doesn't exist on Hatchet, but we don't delete it since it's"
                            + " marked as showAsCreated");
                }
            }
        } else if (data.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
            if (data.getHttpType() == InfoRequestData.HTTPTYPE_GET) {
                List<Playlist> results = data.getResultList(Playlist.class);
                if (results != null && results.size() > 0) {
                    Playlist filledList = results.get(0);
                    if (filledList != null) {
                        Log.d(TAG, "Hatchet sync - received entry list for " + filledList);
                        DatabaseHelper.get().storePlaylist(filledList, false);
                        mResolvingHatchetIds.remove(filledList.getHatchetId());
                    }
                }
            } else if (data.getHttpType() == InfoRequestData.HTTPTYPE_POST) {
                String hatchetId = DatabaseHelper.get()
                        .getPlaylistHatchetId(data.getQueryParams().playlist_local_id);
                if (hatchetId != null) {
                    mShowAsCreatedPlaylistMap.add(hatchetId);
                    Log.d(TAG, "Hatchet sync - created playlist and marked as showAsCreated, id: "
                            + data.getQueryParams().playlist_local_id + ", hatchetId: "
                            + hatchetId);
                }
            }
        } else if (data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS) {
            List<User> results = data.getResultList(User.class);
            if (results == null || results.size() == 0) {
                Log.e(TAG, "Hatchet sync - something went wrong. Got no user object back :(");
                return;
            }
            User user = results.get(0);
            Playlist fetchedList = user.getFavorites();
            if (fetchedList != null) {
                HatchetAuthenticatorUtils hatchetAuthUtils =
                        (HatchetAuthenticatorUtils) AuthenticatorManager.get()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                String userName = hatchetAuthUtils.getUserName();
                fetchedList.setName(TomahawkApp.getContext()
                        .getString(R.string.users_favorites_suffix, userName));
                Log.d(TAG, "Hatchet sync - received list of loved tracks, count: "
                        + fetchedList.size());
                DatabaseHelper.get().storeLovedItemsPlaylist(fetchedList, true);
            }
        } else if (data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDALBUMS) {
            List<User> results = data.getResultList(User.class);
            if (results == null || results.size() == 0) {
                Log.e(TAG, "Hatchet sync - something went wrong. Got no user object back :(");
                return;
            }
            User user = results.get(0);
            List<Album> fetchedAlbums = user.getStarredAlbums();
            Log.d(TAG, "Hatchet sync - received list of starred albums, count: "
                    + fetchedAlbums.size());
            List<Long> lastModifieds = new ArrayList<>();
            for (Album album : fetchedAlbums) {
                Relationship relationship = user.getRelationship(album);
                if (relationship == null) {
                    Log.e(TAG, "Hatchet sync - couldn't find associated relationship for " + album);
                    lastModifieds.add(Long.MAX_VALUE);
                } else {
                    lastModifieds.add(relationship.getDate().getTime());
                }
            }
            getUserCollection().addLovedAlbums(fetchedAlbums, lastModifieds);
        } else if (data.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDARTISTS) {
            List<User> results = data.getResultList(User.class);
            if (results == null || results.size() == 0) {
                Log.e(TAG, "Hatchet sync - something went wrong. Got no user object back :(");
                return;
            }
            User user = results.get(0);
            List<Artist> fetchedArtists = user.getStarredArtists();
            Log.d(TAG, "Hatchet sync - received list of starred artists, count: "
                    + fetchedArtists.size());
            List<Long> lastModifieds = new ArrayList<>();
            for (Artist artist : fetchedArtists) {
                Relationship relationship = user.getRelationship(artist);
                if (relationship == null) {
                    Log.e(TAG,
                            "Hatchet sync - couldn't find associated relationship for " + artist);
                    lastModifieds.add(Long.MAX_VALUE);
                } else {
                    lastModifieds.add(relationship.getDate().getTime());
                }
            }
            getUserCollection().addLovedArtists(fetchedArtists, lastModifieds);
        }
    }

    public void deletePlaylist(String playlistId) {
        String playlistName = DatabaseHelper.get().getPlaylistName(playlistId);
        if (playlistName != null) {
            Log.d(TAG, "Hatchet sync - deleting playlist \"" + playlistName + "\", id: "
                    + playlistId);
            Playlist playlist = DatabaseHelper.get().getEmptyPlaylist(playlistId);
            if (playlist.getHatchetId() != null) {
                mShowAsDeletedPlaylistMap.add(playlist.getHatchetId());
            }
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.get().deletePlaylist(hatchetAuthUtils, playlistId);
            DatabaseHelper.get().deletePlaylist(playlistId);
        } else {
            Log.e(TAG, "Hatchet sync - couldn't delete playlist with id: " + playlistId);
        }
    }

    public void createPlaylist(Playlist playlist) {
        Log.d(TAG, "Hatchet sync - creating " + playlist);
        DatabaseHelper.get().storePlaylist(playlist, false);
        AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        InfoSystem.get().sendPlaylistPostStruct(
                hatchetAuthUtils, playlist.getId(), playlist.getName());
        InfoSystem.get().sendPlaylistEntriesPostStruct(hatchetAuthUtils,
                playlist.getId(), playlist.getEntries());
    }

    public void addPlaylistEntries(String playlistId, ArrayList<PlaylistEntry> entries) {
        String playlistName = DatabaseHelper.get().getPlaylistName(playlistId);
        if (playlistName != null) {
            Log.d(TAG, "Hatchet sync - adding " + entries.size() + " entries to \""
                    + playlistName + "\", id: " + playlistId);
            DatabaseHelper.get().addEntriesToPlaylist(playlistId, entries);
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.get().sendPlaylistEntriesPostStruct(hatchetAuthUtils, playlistId, entries);
        } else {
            Log.e(TAG, "Hatchet sync - couldn't add " + entries.size()
                    + " entries to playlist with id: " + playlistId);
        }
    }

    public void deletePlaylistEntry(String localPlaylistId, String entryId) {
        String playlistName = DatabaseHelper.get().getPlaylistName(localPlaylistId);
        if (playlistName != null) {
            Log.d(TAG, "Hatchet sync - deleting playlist entry in \"" + playlistName
                    + "\", localPlaylistId: " + localPlaylistId + ", entryId: " + entryId);
            DatabaseHelper.get().deleteEntryInPlaylist(localPlaylistId, entryId);
            AuthenticatorUtils hatchetAuthUtils = AuthenticatorManager.get()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            InfoSystem.get().deletePlaylistEntry(hatchetAuthUtils, localPlaylistId, entryId);
        } else {
            Log.e(TAG, "Hatchet sync - couldn't delete entry in playlist, localPlaylistId: "
                    + localPlaylistId + ", entryId: " + entryId);
        }
    }

    public Promise<List<Collection>, Throwable, Void> getAvailableCollections(Artist artist) {
        final List<Collection> collections = new ArrayList<>();
        List<Promise> promises = new ArrayList<>();
        for (final Collection collection : mCollections.values()) {
            if (!collection.getId().equals(TomahawkApp.PLUGINNAME_HATCHET)) {
                promises.add(collection.getArtistAlbums(artist));
                collections.add(collection);
            }
        }
        final Deferred<List<Collection>, Throwable, Void> deferred = new ADeferredObject<>();
        mDeferredManager.when(promises.toArray(new Promise[promises.size()])).always(
                new AlwaysCallback<MultipleResults, OneReject>() {
                    @Override
                    public void onAlways(Promise.State state, MultipleResults resolved,
                            OneReject rejected) {
                        List<Collection> availableCollections = new ArrayList<>();
                        for (int i = 0; i < resolved.size(); i++) {
                            CollectionCursor cursor
                                    = (CollectionCursor) resolved.get(i).getResult();
                            if (cursor != null) {
                                if (cursor.size() > 0) {
                                    availableCollections.add(collections.get(i));
                                }
                                cursor.close();
                            }
                        }
                        availableCollections.add(mCollections.get(TomahawkApp.PLUGINNAME_HATCHET));
                        deferred.resolve(availableCollections);
                    }
                });
        return deferred;
    }

    public Promise<List<Collection>, Throwable, Void> getAvailableCollections(Album album) {
        final List<Collection> collections = new ArrayList<>();
        List<Promise> promises = new ArrayList<>();
        for (final Collection collection : mCollections.values()) {
            if (!collection.getId().equals(TomahawkApp.PLUGINNAME_HATCHET)) {
                promises.add(collection.getAlbumTracks(album));
                collections.add(collection);
            }
        }
        final Deferred<List<Collection>, Throwable, Void> deferred = new ADeferredObject<>();
        mDeferredManager.when(promises.toArray(new Promise[promises.size()])).always(
                new AlwaysCallback<MultipleResults, OneReject>() {
                    @Override
                    public void onAlways(Promise.State state, MultipleResults resolved,
                            OneReject rejected) {
                        List<Collection> availableCollections = new ArrayList<>();
                        for (int i = 0; i < resolved.size(); i++) {
                            Playlist playlist = (Playlist) resolved.get(i).getResult();
                            if (playlist != null) {
                                if (playlist.size() > 0) {
                                    availableCollections.add(collections.get(i));
                                }
                            }
                        }
                        availableCollections.add(mCollections.get(TomahawkApp.PLUGINNAME_HATCHET));
                        deferred.resolve(availableCollections);
                    }
                });
        return deferred;
    }
}
