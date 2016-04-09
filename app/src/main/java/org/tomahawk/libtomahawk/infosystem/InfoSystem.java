/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaybackLogEntry;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaybackLogPostStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntries;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntriesPostStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistEntriesRequest;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistPostStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetPlaylistRequest;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetRelationshipPostStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.models.HatchetRelationshipStruct;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.GsonHelper;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.IdGenerator;

import android.text.TextUtils;
import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * The InfoSystem resolves metadata for artists and albums like album covers and artist images.
 */
public class InfoSystem {

    private static final String TAG = InfoSystem.class.getSimpleName();

    private static class Holder {

        private static final InfoSystem instance = new InfoSystem();

    }

    public static class OpLogIsEmptiedEvent {

        public HashSet<Integer> mRequestTypes;

        public HashSet<String> mPlaylistIds;
    }

    public class ResultsEvent {

        public boolean mSuccess;

        public InfoRequestData mInfoRequestData;
    }

    private final ArrayList<InfoPlugin> mInfoPlugins = new ArrayList<>();

    private final ConcurrentHashMap<String, InfoRequestData> mSentRequests
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Integer, InfoRequestData> mLoggedOpsMap
            = new ConcurrentHashMap<>();

    // We store "create playlists"-loggedOps separately, because we need to check whether or not all
    // "create playlists"-loggedOps have been pushed to Hatchet before sending the corresponding
    // playlist entries
    private final ConcurrentHashMap<Integer, InfoRequestData> mPlaylistsLoggedOpsMap
            = new ConcurrentHashMap<>();

    // LoggedOps waiting to be sent as soon as mPlaylistsLoggedOpsMap is empty
    private final ArrayList<InfoRequestData> mQueuedLoggedOps = new ArrayList<>();

    private Query mLastPlaybackLogEntry = null;

    private Query mNowPlaying = null;

    private InfoSystem() {
        mInfoPlugins.add(new HatchetInfoPlugin());
    }

    public static InfoSystem get() {
        return Holder.instance;
    }

    public void addInfoPlugin(InfoPlugin plugin) {
        mInfoPlugins.add(plugin);
    }

    public void removeInfoPlugin(InfoPlugin plugin) {
        mInfoPlugins.remove(plugin);
    }

    /**
     * HatchetSearch the added InfoPlugins with the given keyword
     *
     * @return the created InfoRequestData's requestId
     */
    public String resolve(String keyword) {
        if (!TextUtils.isEmpty(keyword)) {
            QueryParams params = new QueryParams();
            params.term = keyword;
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES, params);
        }
        return null;
    }

    /**
     * Fill up the given artist with metadata fetched from all added InfoPlugins
     *
     * @param artist the Artist to enrich with data from the InfoPlugins
     * @param full   true, if top-hits and albums should also be resolved
     * @return the created InfoRequestData's requestId
     */
    public String resolve(Artist artist, boolean full) {
        if (artist != null && !TextUtils.isEmpty(artist.getName())) {
            QueryParams params = new QueryParams();
            params.name = artist.getName();
            if (full) {
                return resolve(
                        InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITSANDALBUMS, params);
            } else {
                return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS, params);
            }
        }
        return null;
    }

    /**
     * Fill up the given artist with metadata fetched from all added InfoPlugins
     *
     * @param album the Album to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolve(Album album) {
        if (album != null && !TextUtils.isEmpty(album.getName())) {
            QueryParams params = new QueryParams();
            params.name = album.getName();
            params.artistname = album.getArtist().getName();
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS_TRACKS, params);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user the User to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolve(User user) {
        if (user != null && !user.isOffline() && !TextUtils.isEmpty(user.getId())) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<>();
            params.ids.add(user.getId());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS, params);
        }
        return null;
    }

    /**
     * Get random users
     *
     * @param count the number of users to get
     * @return the created InfoRequestData's requestId
     */
    public String getRandomUsers(int count) {
        QueryParams params = new QueryParams();
        params.random = String.valueOf(true);
        params.count = String.valueOf(count);
        return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS, params);
    }

    public String resolve(Playlist playlist) {
        if (playlist != null) {
            QueryParams params = new QueryParams();
            params.playlist_local_id = playlist.getId();
            params.playlist_id = playlist.getHatchetId();
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES, params);
        }
        return null;
    }

    /**
     * Fetch a logged-in user's id and store it
     *
     * @param username the username with which to get the corresponding id
     * @return the created InfoRequestData's requestId
     */
    public String resolveUserId(String username) {
        if (username != null) {
            QueryParams params = new QueryParams();
            params.name = username;
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS, params);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user       the User for which to get the socialActions
     * @param beforeDate the Date that specifies which socialActions to fetch
     * @return the created InfoRequestData's requestId
     */
    public String resolveSocialActions(User user, Date beforeDate) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.userid = user.getId();
            params.before_date = beforeDate;
            params.limit = String.valueOf(HatchetInfoPlugin.SOCIALACTIONS_LIMIT);
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS, params);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user       the User to enrich with data from the InfoPlugins
     * @param beforeDate the Date that specifies which socialActions to fetch
     * @return the created InfoRequestData's requestId
     */
    public String resolveFriendsFeed(User user, Date beforeDate) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.userid = user.getId();
            params.type = HatchetInfoPlugin.HATCHET_SOCIALACTION_PARAMTYPE_FRIENDSFEED;
            params.before_date = beforeDate;
            params.limit = String.valueOf(HatchetInfoPlugin.FRIENDSFEED_LIMIT);
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS, params);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user the User to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolvePlaybackLog(User user) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<>();
            params.ids.add(user.getId());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG, params);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user the User to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolveLovedItems(User user) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<>();
            params.ids.add(user.getId());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDITEMS, params, true);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user the User to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolveFollowings(User user) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<>();
            params.ids.add(user.getId());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FOLLOWS, params);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user the User to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolveFollowers(User user) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<>();
            params.ids.add(user.getId());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FOLLOWERS, params);
        }
        return null;
    }

    /**
     * Fetch the given user's list of loved albums
     *
     * @return the created InfoRequestData's requestId
     */
    public String resolveLovedAlbums(User user) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<>();
            params.ids.add(user.getId());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDALBUMS, params, true);
        }
        return null;
    }

    /**
     * Fetch the given user's list of loved artists
     *
     * @return the created InfoRequestData's requestId
     */
    public String resolveLovedArtists(User user) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<>();
            params.ids.add(user.getId());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_LOVEDARTISTS, params, true);
        }
        return null;
    }

    public String resolvePlaylists(User user, boolean isBackgroundRequest) {
        if (user != null && !user.isOffline()) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<>();
            params.ids.add(user.getId());
            String requestId = IdGenerator.getSessionUniqueStringId();
            InfoRequestData infoRequestData = new InfoRequestData(requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYLISTS, params,
                    isBackgroundRequest);
            resolve(infoRequestData);
            return infoRequestData.getRequestId();
        }
        return null;
    }

    /**
     * Build an InfoRequestData object with the given data and order results
     *
     * @param type   the type of the InfoRequestData object
     * @param params all parameters to be given to the InfoPlugin
     * @return the created InfoRequestData's requestId
     */
    public String resolve(int type, QueryParams params) {
        return resolve(type, params, false);
    }

    /**
     * Build an InfoRequestData object with the given data and order results
     *
     * @param type                the type of the InfoRequestData object
     * @param params              all parameters to be given to the InfoPlugin
     * @param isBackgroundRequest boolean indicating whether or not this request should be run with
     *                            the lowest priority (useful for sync operations)
     * @return the created InfoRequestData's requestId
     */
    public String resolve(int type, QueryParams params, boolean isBackgroundRequest) {
        String requestId = IdGenerator.getSessionUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId, type, params,
                isBackgroundRequest);
        resolve(infoRequestData);
        return infoRequestData.getRequestId();
    }

    /**
     * Order results for the given InfoRequestData object
     *
     * @param infoRequestData the InfoRequestData object to fetch results for
     */
    public void resolve(InfoRequestData infoRequestData) {
        for (InfoPlugin infoPlugin : mInfoPlugins) {
            infoPlugin.resolve(infoRequestData);
        }
    }

    public void sendPlaybackEntryPostStruct(AuthenticatorUtils authenticatorUtils) {
        if (mNowPlaying != null && mNowPlaying != mLastPlaybackLogEntry) {
            mLastPlaybackLogEntry = mNowPlaying;
            long timeStamp = System.currentTimeMillis();
            HatchetPlaybackLogEntry playbackLogEntry = new HatchetPlaybackLogEntry();
            playbackLogEntry.albumString = mLastPlaybackLogEntry.getAlbum().getName();
            playbackLogEntry.artistString = mLastPlaybackLogEntry.getArtist().getName();
            if (playbackLogEntry.artistString.isEmpty()) {
                playbackLogEntry.artistString = "Unknown Artist";
            }
            playbackLogEntry.trackString = mLastPlaybackLogEntry.getName();
            if (playbackLogEntry.trackString.isEmpty()) {
                playbackLogEntry.trackString = "Unknown Title";
            }
            playbackLogEntry.timestamp = new Date(timeStamp);
            HatchetPlaybackLogPostStruct playbackLogPostStruct = new HatchetPlaybackLogPostStruct();
            playbackLogPostStruct.playbackLogEntry = playbackLogEntry;

            String requestId = IdGenerator.getSessionUniqueStringId();
            String jsonString = GsonHelper.get().toJson(playbackLogPostStruct);
            InfoRequestData infoRequestData = new InfoRequestData(requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES, null,
                    InfoRequestData.HTTPTYPE_POST, jsonString);
            DatabaseHelper.get().addOpToInfoSystemOpLog(infoRequestData,
                    (int) (timeStamp / 1000));
            sendLoggedOps(authenticatorUtils);
        }
    }

    public void sendNowPlayingPostStruct(AuthenticatorUtils authenticatorUtils, Query query) {
        if (mNowPlaying != query) {
            sendPlaybackEntryPostStruct(authenticatorUtils);
            mNowPlaying = query;
            long timeStamp = System.currentTimeMillis();
            HatchetPlaybackLogEntry playbackLogEntry = new HatchetPlaybackLogEntry();
            playbackLogEntry.albumString = query.getAlbum().getName();
            playbackLogEntry.artistString = query.getArtist().getName();
            if (playbackLogEntry.artistString.isEmpty()) {
                playbackLogEntry.artistString = "Unknown Artist";
            }
            playbackLogEntry.trackString = query.getName();
            if (playbackLogEntry.trackString.isEmpty()) {
                playbackLogEntry.trackString = "Unknown Title";
            }
            playbackLogEntry.type = "nowplaying";
            playbackLogEntry.timestamp = new Date(timeStamp);
            HatchetPlaybackLogPostStruct playbackLogPostStruct = new HatchetPlaybackLogPostStruct();
            playbackLogPostStruct.playbackLogEntry = playbackLogEntry;

            String requestId = IdGenerator.getSessionUniqueStringId();
            String jsonString = GsonHelper.get().toJson(playbackLogPostStruct);
            InfoRequestData infoRequestData = new InfoRequestData(requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES, null,
                    InfoRequestData.HTTPTYPE_POST, jsonString);
            send(infoRequestData, authenticatorUtils);
        }
    }

    public InfoRequestData buildPlaylistPostStruct(String localId, String title) {
        HatchetPlaylistRequest request = new HatchetPlaylistRequest();
        request.title = title;
        HatchetPlaylistPostStruct struct = new HatchetPlaylistPostStruct();
        struct.playlist = request;

        String requestId = IdGenerator.getSessionUniqueStringId();
        String jsonString = GsonHelper.get().toJson(struct);
        QueryParams params = new QueryParams();
        params.playlist_local_id = localId;
        return new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS, params,
                InfoRequestData.HTTPTYPE_POST, jsonString);
    }

    public void sendPlaylistPostStruct(AuthenticatorUtils authenticatorUtils,
            String localId, String title) {
        long timeStamp = System.currentTimeMillis();
        InfoRequestData infoRequestData = buildPlaylistPostStruct(localId, title);
        DatabaseHelper.get().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000));
        sendLoggedOps(authenticatorUtils);
    }

    public InfoRequestData buildPlaylistEntriesPostStruct(String localPlaylistId,
            List<PlaylistEntry> entries) {
        HatchetPlaylistEntriesPostStruct struct = new HatchetPlaylistEntriesPostStruct();
        struct.playlistEntries = new ArrayList<>();
        for (PlaylistEntry entry : entries) {
            HatchetPlaylistEntriesRequest request = new HatchetPlaylistEntriesRequest();
            request.trackString = entry.getQuery().getName();
            request.artistString = entry.getArtist().getName();
            request.albumString = entry.getAlbum().getName();
            struct.playlistEntries.add(request);
        }

        String requestId = IdGenerator.getSessionUniqueStringId();
        String jsonString = GsonHelper.get().toJson(struct);
        QueryParams params = new QueryParams();
        params.playlist_local_id = localPlaylistId;
        return new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES, params,
                InfoRequestData.HTTPTYPE_POST, jsonString);
    }

    public void sendPlaylistEntriesPostStruct(AuthenticatorUtils authenticatorUtils,
            String localPlaylistId, List<PlaylistEntry> entries) {
        long timeStamp = System.currentTimeMillis();
        InfoRequestData infoRequestData =
                buildPlaylistEntriesPostStruct(localPlaylistId, entries);
        DatabaseHelper.get().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000));
        sendLoggedOps(authenticatorUtils);
    }

    public void deletePlaylist(AuthenticatorUtils authenticatorUtils, String localPlaylistId) {
        long timeStamp = System.currentTimeMillis();
        String requestId = IdGenerator.getLifetimeUniqueStringId();
        QueryParams params = new QueryParams();
        params.playlist_local_id = localPlaylistId;
        InfoRequestData infoRequestData = new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS, params,
                InfoRequestData.HTTPTYPE_DELETE, null);
        DatabaseHelper.get().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000));
        sendLoggedOps(authenticatorUtils);
    }

    public void deletePlaylistEntry(AuthenticatorUtils authenticatorUtils, String localPlaylistId,
            String entryId) {
        long timeStamp = System.currentTimeMillis();
        String requestId = IdGenerator.getLifetimeUniqueStringId();
        QueryParams params = new QueryParams();
        params.entry_id = entryId;
        params.playlist_local_id = localPlaylistId;
        InfoRequestData infoRequestData = new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES, params,
                InfoRequestData.HTTPTYPE_DELETE, null);
        DatabaseHelper.get().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000));
        sendLoggedOps(authenticatorUtils);
    }

    public void sendRelationshipPostStruct(AuthenticatorUtils authenticatorUtils, User user) {
        sendRelationshipPostStruct(authenticatorUtils, user.getId(), null, null, null);
    }

    public void sendRelationshipPostStruct(AuthenticatorUtils authenticatorUtils, Query query) {
        sendRelationshipPostStruct(authenticatorUtils, null, query.getName(),
                query.getArtist().getName(), null);
    }

    public void sendRelationshipPostStruct(AuthenticatorUtils authenticatorUtils, Artist artist) {
        sendRelationshipPostStruct(authenticatorUtils, null, null, artist.getName(), null);
    }

    public void sendRelationshipPostStruct(AuthenticatorUtils authenticatorUtils, Album album) {
        sendRelationshipPostStruct(authenticatorUtils, null, null, album.getArtist().getName(),
                album.getName());
    }

    public InfoRequestData buildRelationshipPostStruct(String user, String track, String artist,
            String album) {
        HatchetRelationshipStruct relationship = new HatchetRelationshipStruct();
        relationship.targetUser = user;
        relationship.targetTrackString = track;
        relationship.targetArtistString = artist;
        relationship.targetAlbumString = album;
        relationship.type = user != null ? HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TYPE_FOLLOW
                : HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TYPE_LOVE;
        HatchetRelationshipPostStruct struct = new HatchetRelationshipPostStruct();
        struct.relationShip = relationship;

        String requestId = IdGenerator.getSessionUniqueStringId();

        String jsonString = GsonHelper.get().toJson(struct);
        return new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS, null,
                InfoRequestData.HTTPTYPE_POST, jsonString);
    }

    public void sendRelationshipPostStruct(AuthenticatorUtils authenticatorUtils,
            String user, String track, String artist, String album) {
        long timeStamp = System.currentTimeMillis();
        InfoRequestData infoRequestData = buildRelationshipPostStruct(user, track, artist, album);
        DatabaseHelper.get().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000));
        sendLoggedOps(authenticatorUtils);
    }

    public void deleteRelationship(AuthenticatorUtils authenticatorUtils, String relationshipId) {
        long timeStamp = System.currentTimeMillis();
        String requestId = IdGenerator.getSessionUniqueStringId();
        QueryParams params = new QueryParams();
        params.relationship_id = relationshipId;
        InfoRequestData infoRequestData = new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS, params,
                InfoRequestData.HTTPTYPE_DELETE, null);
        DatabaseHelper.get().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000));
        sendLoggedOps(authenticatorUtils);
    }

    /**
     * Send the given InfoRequestData's data out to every service that can handle it
     *
     * @param infoRequestData    the InfoRequestData object to fetch results for
     * @param authenticatorUtils the AuthenticatorUtils object to fetch the appropriate access
     *                           tokens
     */
    private void send(InfoRequestData infoRequestData, AuthenticatorUtils authenticatorUtils) {
        mSentRequests.put(infoRequestData.getRequestId(), infoRequestData);
        for (InfoPlugin infoPlugin : mInfoPlugins) {
            infoPlugin.send(infoRequestData, authenticatorUtils);
        }
    }

    /**
     * Get the InfoRequestData with the given Id
     */
    public InfoRequestData getSentLoggedOpById(String requestId) {
        return mSentRequests.get(requestId);
    }

    /**
     * Method to enable InfoPlugins to report that the InfoRequestData objects with the given
     * requestIds have received their results
     */
    public void reportResults(InfoRequestData infoRequestData, boolean success) {
        ResultsEvent event = new ResultsEvent();
        event.mInfoRequestData = infoRequestData;
        event.mSuccess = success;
        EventBus.getDefault().post(event);
    }


    public synchronized void sendLoggedOps(AuthenticatorUtils authenticatorUtils) {
        List<InfoRequestData> loggedOps = DatabaseHelper.get().getLoggedOps();
        for (InfoRequestData loggedOp : loggedOps) {
            verifyLoggedOp(loggedOp);
        }
        loggedOps = DatabaseHelper.get().getLoggedOps();
        for (InfoRequestData loggedOp : loggedOps) {
            if (!mLoggedOpsMap.containsKey(loggedOp.getLoggedOpId())) {
                mLoggedOpsMap.put(loggedOp.getLoggedOpId(), loggedOp);
                if (loggedOp.getType()
                        == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES
                        || (loggedOp.getHttpType() == InfoRequestData.HTTPTYPE_DELETE
                        && loggedOp.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS)) {
                    mQueuedLoggedOps.add(loggedOp);
                } else {
                    if (loggedOp.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS) {
                        mPlaylistsLoggedOpsMap.put(loggedOp.getLoggedOpId(), loggedOp);
                    }
                    send(loggedOp, authenticatorUtils);
                }
            }
        }
        trySendingQueuedOps();
    }

    public synchronized void onLoggedOpsSent(ArrayList<String> doneRequestsIds, boolean discard) {
        List<InfoRequestData> loggedOps = new ArrayList<>();
        HashSet<Integer> requestTypes = new HashSet<>();
        HashSet<String> playlistIds = new HashSet<>();
        for (String doneRequestId : doneRequestsIds) {
            if (mSentRequests.containsKey(doneRequestId)) {
                InfoRequestData loggedOp = mSentRequests.get(doneRequestId);
                loggedOps.add(loggedOp);
                requestTypes.add(loggedOp.getType());
                if (loggedOp.getType()
                        == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
                    playlistIds.add(loggedOp.getQueryParams().playlist_local_id);
                } else if (loggedOp.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS) {
                    List<HatchetPlaylistEntries> results =
                            loggedOp.getResultList(HatchetPlaylistEntries.class);
                    if (results != null && results.size() > 0) {
                        HatchetPlaylistEntries entries = results.get(0);
                        if (entries != null && entries.playlists.size() > 0) {
                            playlistIds.add(entries.playlists.get(0).id);
                            DatabaseHelper.get().updatePlaylistHatchetId(
                                    loggedOp.getQueryParams().playlist_local_id,
                                    entries.playlists.get(0).id);
                        }
                    }
                }
                mLoggedOpsMap.remove(loggedOp.getLoggedOpId());
            }
        }
        if (discard) {
            for (InfoRequestData loggedOp : loggedOps) {
                mPlaylistsLoggedOpsMap.remove(loggedOp.getLoggedOpId());
            }
            trySendingQueuedOps();
            DatabaseHelper.get().removeOpsFromInfoSystemOpLog(loggedOps);
            if (DatabaseHelper.get().getLoggedOpsCount() == 0) {
                if (!requestTypes.isEmpty()) {
                    OpLogIsEmptiedEvent event = new OpLogIsEmptiedEvent();
                    event.mRequestTypes = requestTypes;
                    event.mPlaylistIds = playlistIds;
                    EventBus.getDefault().post(event);
                }
            }
        }
    }

    private synchronized void trySendingQueuedOps() {
        if (mPlaylistsLoggedOpsMap.isEmpty()) {
            while (!mQueuedLoggedOps.isEmpty()) {
                InfoRequestData queuedLoggedOp = mQueuedLoggedOps.remove(0);
                QueryParams params = queuedLoggedOp.getQueryParams();
                String hatchetId = DatabaseHelper.get()
                        .getPlaylistHatchetId(params.playlist_local_id);
                if (hatchetId != null) {
                    if (queuedLoggedOp.getType()
                            == InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS_PLAYLISTENTRIES) {
                        if (queuedLoggedOp.getHttpType() == InfoRequestData.HTTPTYPE_POST) {
                            // Now that we know the hatchetId, we can add it to the playlistEntry
                            // object we POST to Hatchet
                            int newHatchetId = Integer.valueOf(hatchetId);
                            JsonElement element = GsonHelper.get().fromJson(
                                    queuedLoggedOp.getJsonStringToSend(), JsonElement.class);
                            if (element.isJsonObject()) {
                                JsonObject object = (JsonObject) element;
                                JsonObject playlistEntry = object.getAsJsonObject("playlistEntry");
                                if (playlistEntry != null) {
                                    // old way of posting playlistEntries (one per request)
                                    playlistEntry.addProperty("playlist", newHatchetId);
                                } else {
                                    // new way of posting playlistEntries (all at once)
                                    object.addProperty("playlist", newHatchetId);
                                }
                            }
                            queuedLoggedOp.setJsonStringToSend(GsonHelper.get().toJson(element));
                        } else if (queuedLoggedOp.getHttpType()
                                == InfoRequestData.HTTPTYPE_DELETE) {
                            params.playlist_id = hatchetId;
                        }
                    } else {
                        params.playlist_id = hatchetId;
                    }
                    send(queuedLoggedOp, AuthenticatorManager.get().getAuthenticatorUtils(
                            TomahawkApp.PLUGINNAME_HATCHET));
                } else {
                    Log.e(TAG, "Hatchet sync - Couldn't send queued logged op, because the stored "
                            + "local playlist id was no longer valid");
                    discardLoggedOp(queuedLoggedOp);
                }
            }
        }
    }

    private void discardLoggedOp(InfoRequestData loggedOp) {
        mSentRequests.put(loggedOp.getRequestId(), loggedOp);
        ArrayList<String> doneRequestsIds = new ArrayList<>();
        doneRequestsIds.add(loggedOp.getRequestId());
        InfoSystem.get().onLoggedOpsSent(doneRequestsIds, true);
    }

    /**
     * Verify if the given loggedOp needs to be converted to a newer version. This is needed because
     * the Hatchet API changes.
     */
    private void verifyLoggedOp(InfoRequestData loggedOp) {
        InfoRequestData convertedLogOp = null;
        if (loggedOp.getType() == 1300) { // old v1 way of posting a socialAction
            JsonElement element =
                    GsonHelper.get().fromJson(loggedOp.getJsonStringToSend(), JsonElement.class);
            if (element instanceof JsonObject) {
                JsonObject socialAction = ((JsonObject) element).getAsJsonObject("socialAction");
                String action = getAsString(socialAction, "action");
                if (action != null && action.equals("true")) {
                    String trackString = getAsString(socialAction, "trackString");
                    String artistString = getAsString(socialAction, "artistString");
                    String albumString = getAsString(socialAction, "albumString");
                    convertedLogOp = buildRelationshipPostStruct(
                            null, trackString, artistString, albumString);
                } else {
                    // We have to discard the loggedOp since we don't have any way of getting the
                    // associated relationShipId. Therefore we are unable to delete this particular
                    // relationship.
                    DatabaseHelper.get().removeOpFromInfoSystemOpLog(loggedOp);
                }
            }
        } else if (loggedOp.getType() == 1001) {
            JsonElement element =
                    GsonHelper.get().fromJson(loggedOp.getJsonStringToSend(), JsonElement.class);
            if (element instanceof JsonObject) {
                JsonElement playlist = ((JsonObject) element).get("playlist");
                if (playlist instanceof JsonObject
                        && !((JsonObject) element).has("playlistEntry")) {
                    // It's definitely an old "create playlist"-struct
                    String title = getAsString((JsonObject) playlist, "title");
                    convertedLogOp = buildPlaylistPostStruct(
                            loggedOp.getQueryParams().playlist_local_id, title);
                }
            }
        } else if (loggedOp.getType() == 1002) {
            JsonElement element =
                    GsonHelper.get().fromJson(loggedOp.getJsonStringToSend(), JsonElement.class);
            if (element instanceof JsonObject) {
                JsonObject playlistEntry = ((JsonObject) element).getAsJsonObject("playlistEntry");
                if (playlistEntry != null) {
                    String trackString = getAsString(playlistEntry, "trackString");
                    String artistString = getAsString(playlistEntry, "artistString");
                    String albumString = getAsString(playlistEntry, "albumString");

                    Query query = Query.get(trackString, albumString, artistString, false, true);
                    PlaylistEntry entry = PlaylistEntry.get(
                            loggedOp.getQueryParams().playlist_local_id, query,
                            IdGenerator.getLifetimeUniqueStringId());
                    List<PlaylistEntry> entries = new ArrayList<>();
                    entries.add(entry);
                    convertedLogOp = buildPlaylistEntriesPostStruct(
                            loggedOp.getQueryParams().playlist_local_id, entries);
                }
            }
        }
        if (convertedLogOp != null) {
            DatabaseHelper.get().removeOpFromInfoSystemOpLog(loggedOp);
            DatabaseHelper.get().addOpToInfoSystemOpLog(convertedLogOp,
                    (int) System.currentTimeMillis() / 1000);
        }
    }

    private String getAsString(JsonObject object, String memberName) {
        JsonElement element = object.get(memberName);
        if (element != null && element.isJsonPrimitive()) {
            return element.getAsString();
        }
        return null;
    }
}
