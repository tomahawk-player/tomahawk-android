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

import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetNowPlaying;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetNowPlayingPostStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaybackLogEntry;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaybackLogPostStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetSocialAction;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetSocialActionPostStruct;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Intent;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The InfoSystem resolves metadata for artists and albums like album covers and artist images.
 */
public class InfoSystem {

    private static InfoSystem instance;

    public static final String INFOSYSTEM_RESULTSREPORTED = "infosystem_resultsreported";

    public static final String INFOSYSTEM_REQUESTFAILED = "infosystem_requestfailed";

    public static final String INFOSYSTEM_OPLOGISEMPTIED = "infosystem_oplogisempty";

    public static final String INFOSYSTEM_RESULTSREPORTED_REQUESTID
            = "infosystem_resultsreported_requestid";

    public static final String PARAM_NAME = "name";

    public static final String PARAM_ARTIST_NAME = "artist_name";

    public static final String PARAM_TERM = "term";

    private boolean mInitialized;

    private ArrayList<InfoPlugin> mInfoPlugins = new ArrayList<InfoPlugin>();

    private ConcurrentHashMap<String, InfoRequestData> mRequests
            = new ConcurrentHashMap<String, InfoRequestData>();

    private ConcurrentHashMap<String, InfoRequestData> mSentRequests
            = new ConcurrentHashMap<String, InfoRequestData>();

    private ConcurrentHashMap<Integer, InfoRequestData> mLoggedOpsMap
            = new ConcurrentHashMap<Integer, InfoRequestData>();

    /**
     * The below HashSets are used to mark the included objects as "already done before"
     */
    private HashSet<Artist> mArtistHashSet = new HashSet<Artist>();

    private HashSet<Artist> mArtistTopHitsHashSet = new HashSet<Artist>();

    private HashSet<Artist> mArtistAlbumsHashSet = new HashSet<Artist>();

    private HashSet<Album> mAlbumHashSet = new HashSet<Album>();

    private Query mLastPlaybackLogEntry = null;

    private Query mNowPlaying = null;

    private InfoSystem() {
    }

    public static InfoSystem getInstance() {
        if (instance == null) {
            synchronized (InfoSystem.class) {
                if (instance == null) {
                    instance = new InfoSystem();
                }
            }
        }
        return instance;
    }

    public void ensureInit() {
        if (!mInitialized) {
            mInitialized = true;
            mInfoPlugins.add(new HatchetInfoPlugin());
        }
    }

    /**
     * HatchetSearch the added InfoPlugins with the given keyword
     *
     * @return the created InfoRequestData's requestId
     */
    public String resolve(String keyword) {
        QueryParams params = new QueryParams();
        params.term = keyword;
        return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_SEARCHES, params);
    }

    /**
     * Fill up the given artist with metadata fetched from all added InfoPlugins
     *
     * @param artist    the Artist to enrich with data from the InfoPlugins
     * @param justImage true, if only the artist image should be fetched
     * @return an ArrayList of Strings containing all created requestIds
     */
    public ArrayList<String> resolve(Artist artist, boolean justImage) {
        ArrayList<String> requestIds = new ArrayList<String>();
        if (artist != null) {
            QueryParams params = new QueryParams();
            params.name = artist.getName();
            if (!mArtistHashSet.contains(artist)) {
                mArtistHashSet.add(artist);
                String requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS, params,
                        artist);
                requestIds.add(requestId);
            }
            if (!mArtistTopHitsHashSet.contains(artist) && !justImage) {
                mArtistTopHitsHashSet.add(artist);
                String requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS,
                        params, artist);
                requestIds.add(requestId);
            }
            if (!mArtistAlbumsHashSet.contains(artist) && !justImage) {
                mArtistAlbumsHashSet.add(artist);
                String requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_ALBUMS,
                        params, artist);
                requestIds.add(requestId);
            }
        }
        return requestIds;
    }

    /**
     * Fill up the given artist with metadata fetched from all added InfoPlugins
     *
     * @param album the Album to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolve(Album album) {
        if (album != null && !TextUtils.isEmpty(album.getName())
                && !mAlbumHashSet.contains(album)) {
            mAlbumHashSet.add(album);
            QueryParams params = new QueryParams();
            params.name = album.getName();
            params.artistname = album.getArtist().getName();
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS, params, album);
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
        if (user != null) {
            QueryParams params = new QueryParams();
            params.ids = new ArrayList<String>();
            params.ids.add(user.getId());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS, params, user);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user the User to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolveSocialActions(User user) {
        if (user != null) {
            QueryParams params = new QueryParams();
            params.userid = user.getId();
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_SOCIALACTIONS,
                    params, user);
        }
        return null;
    }

    /**
     * Fill up the given user with metadata fetched from all added InfoPlugins
     *
     * @param user the User to enrich with data from the InfoPlugins
     * @return the created InfoRequestData's requestId
     */
    public String resolveFriendsFeed(User user) {
        if (user != null) {
            QueryParams params = new QueryParams();
            params.userid = user.getId();
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FRIENDSFEED,
                    params, user);
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
        if (user != null) {
            QueryParams params = new QueryParams();
            params.userid = user.getId();
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_USERS_PLAYBACKLOG,
                    params, user);
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
        if (user != null) {
            QueryParams params = new QueryParams();
            params.userid = user.getId();
            params.type = HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TYPE_FOLLOW;
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWINGS,
                    params, user);
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
        if (user != null) {
            QueryParams params = new QueryParams();
            params.targetuserid = user.getId();
            params.type = HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TYPE_FOLLOW;
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWERS,
                    params, user);
        }
        return null;
    }

    /**
     * Fetch the given user's list of starred albums
     *
     * @return the created InfoRequestData's requestId
     */
    public String resolveStarredAlbums(User user) {
        QueryParams params = new QueryParams();
        if (user != null) {
            params.userid = user.getId();
        }
        params.type = HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TYPE_LOVE;
        params.targettype = HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TARGETTYPE_ALBUM;
        return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDALBUMS,
                params, user);
    }

    /**
     * Fetch the given user's list of starred artists
     *
     * @return the created InfoRequestData's requestId
     */
    public String resolveStarredArtists(User user) {
        QueryParams params = new QueryParams();
        if (user != null) {
            params.userid = user.getId();
        }
        params.type = HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TYPE_LOVE;
        params.targettype = HatchetInfoPlugin.HATCHET_RELATIONSHIPS_TARGETTYPE_ARTIST;
        return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_STARREDARTISTS,
                params, user);
    }

    /**
     * Build an InfoRequestData object with the given data and order results
     *
     * @param type   the type of the InfoRequestData object
     * @param params all parameters to be given to the InfoPlugin
     * @return the created InfoRequestData's requestId
     */
    public String resolve(int type, QueryParams params) {
        String requestId = TomahawkMainActivity.getSessionUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId, type, params);
        resolve(infoRequestData);
        return infoRequestData.getRequestId();
    }

    /**
     * Build an InfoRequestData object with the given data and order results
     *
     * @param type           the type of the InfoRequestData object
     * @param params         all parameters to be given to the InfoPlugin
     * @param itemToBeFilled the item to automatically be filled after the InfoPlugin fetched the
     *                       results from its source
     * @return the created InfoRequestData's requestId
     */
    public String resolve(int type, QueryParams params,
            TomahawkListItem itemToBeFilled) {
        String requestId = TomahawkMainActivity.getSessionUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId, type, params);
        if (itemToBeFilled != null) {
            resolve(infoRequestData, itemToBeFilled);
        } else {
            resolve(infoRequestData);
        }
        return infoRequestData.getRequestId();
    }


    /**
     * Order results for the given InfoRequestData object
     *
     * @param infoRequestData the InfoRequestData object to fetch results for
     */
    public void resolve(InfoRequestData infoRequestData) {
        mRequests.put(infoRequestData.getRequestId(), infoRequestData);
        for (InfoPlugin infoPlugin : mInfoPlugins) {
            infoPlugin.resolve(infoRequestData);
        }
    }

    /**
     * Order results for the given InfoRequestData object
     *
     * @param infoRequestData the InfoRequestData object to fetch results for
     * @param itemToBeFilled  the item to automatically be filled after the InfoPlugin fetched the
     *                        results from its source
     */
    public void resolve(InfoRequestData infoRequestData,
            TomahawkListItem itemToBeFilled) {
        mRequests.put(infoRequestData.getRequestId(), infoRequestData);
        for (InfoPlugin infoPlugin : mInfoPlugins) {
            infoPlugin.resolve(infoRequestData, itemToBeFilled);
        }
    }

    public void sendPlaybackEntryPostStruct(AuthenticatorUtils authenticatorUtils) {
        if (mNowPlaying != null && mNowPlaying != mLastPlaybackLogEntry) {
            mLastPlaybackLogEntry = mNowPlaying;
            long timeStamp = System.currentTimeMillis();
            HatchetPlaybackLogEntry playbackLogEntry = new HatchetPlaybackLogEntry();
            playbackLogEntry.albumString = mLastPlaybackLogEntry.getAlbum().getName();
            playbackLogEntry.artistString = mLastPlaybackLogEntry.getArtist().getName();
            playbackLogEntry.trackString = mLastPlaybackLogEntry.getName();
            playbackLogEntry.timestamp = new Date(timeStamp);
            HatchetPlaybackLogPostStruct playbackLogPostStruct = new HatchetPlaybackLogPostStruct();
            playbackLogPostStruct.playbackLogEntry = playbackLogEntry;

            String requestId = TomahawkMainActivity.getSessionUniqueStringId();
            InfoRequestData infoRequestData = new InfoRequestData(requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES,
                    playbackLogPostStruct);
            DatabaseHelper.getInstance().addOpToInfoSystemOpLog(infoRequestData,
                    (int) (timeStamp / 1000), DatabaseHelper.REQUESTTYPE_POST);
            sendLoggedOps(authenticatorUtils);
        }
    }

    public void sendNowPlayingPostStruct(AuthenticatorUtils authenticatorUtils, Query query) {
        if (mNowPlaying != query) {
            sendPlaybackEntryPostStruct(authenticatorUtils);
            mNowPlaying = query;
            HatchetNowPlaying nowPlaying = new HatchetNowPlaying();
            nowPlaying.album = query.getAlbum().getName();
            nowPlaying.artist = query.getArtist().getName();
            nowPlaying.track = query.getName();
            HatchetNowPlayingPostStruct nowPlayingPostStruct = new HatchetNowPlayingPostStruct();
            nowPlayingPostStruct.nowPlaying = nowPlaying;

            String requestId = TomahawkMainActivity.getSessionUniqueStringId();
            InfoRequestData infoRequestData = new InfoRequestData(requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES_NOWPLAYING,
                    nowPlayingPostStruct);
            send(infoRequestData, authenticatorUtils);
        }
    }

    private void sendSocialActionPostStruct(AuthenticatorUtils authenticatorUtils,
            String trackString, String artistString, String albumString, String type,
            boolean action) {
        long timeStamp = System.currentTimeMillis();
        HatchetSocialAction socialAction = new HatchetSocialAction();
        socialAction.type = type;
        socialAction.action = String.valueOf(action);
        socialAction.trackString = trackString;
        socialAction.artistString = artistString;
        socialAction.albumString = albumString;
        socialAction.timestamp = new Date(timeStamp);
        HatchetSocialActionPostStruct socialActionPostStruct = new HatchetSocialActionPostStruct();
        socialActionPostStruct.socialAction = socialAction;

        String requestId = TomahawkMainActivity.getSessionUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS, socialActionPostStruct);
        DatabaseHelper.getInstance().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000), DatabaseHelper.REQUESTTYPE_POST);
        sendLoggedOps(authenticatorUtils);
    }

    public void sendSocialActionPostStruct(AuthenticatorUtils authenticatorUtils, Query query,
            String type, boolean action) {
        sendSocialActionPostStruct(authenticatorUtils, query.getName(), query.getArtist().getName(),
                null, type, action);
    }

    public void sendSocialActionPostStruct(AuthenticatorUtils authenticatorUtils, Artist artist,
            String type, boolean action) {
        sendSocialActionPostStruct(authenticatorUtils, null, artist.getName(), null, type, action);
    }

    public void sendSocialActionPostStruct(AuthenticatorUtils authenticatorUtils, Album album,
            String type, boolean action) {
        sendSocialActionPostStruct(authenticatorUtils, null, album.getArtist().getName(),
                album.getName(), type, action);
    }

    public void createPlaylist(AuthenticatorUtils authenticatorUtils, String playlistId) {
        long timeStamp = System.currentTimeMillis();
        String requestId = TomahawkMainActivity.getLifetimeUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS, playlistId);
        DatabaseHelper.getInstance().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000), DatabaseHelper.REQUESTTYPE_DELETE);
        sendLoggedOps(authenticatorUtils);
    }

    public void deletePlaylist(AuthenticatorUtils authenticatorUtils, String playlistId) {
        long timeStamp = System.currentTimeMillis();
        String requestId = TomahawkMainActivity.getLifetimeUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_PLAYLISTS, playlistId);
        DatabaseHelper.getInstance().addOpToInfoSystemOpLog(infoRequestData,
                (int) (timeStamp / 1000), DatabaseHelper.REQUESTTYPE_DELETE);
        sendLoggedOps(authenticatorUtils);
    }

    public void addQueryToPlaylist(AuthenticatorUtils authenticatorUtils, String playlistId,
            Query query) {

    }

    public void deleteQueryInPlaylist(AuthenticatorUtils authenticatorUtils, String playlistId,
            String entryId) {

    }

    public List<String> sendLoggedOps(AuthenticatorUtils authenticatorUtils) {
        List<String> requestIds = new ArrayList<String>();
        List<InfoRequestData> loggedOps = DatabaseHelper.getInstance().getLoggedOps();
        for (InfoRequestData loggedOp : loggedOps) {
            if (!mLoggedOpsMap.containsKey(loggedOp.getOpLogId())) {
                mLoggedOpsMap.put(loggedOp.getOpLogId(), loggedOp);
                requestIds.add(loggedOp.getRequestId());
                send(loggedOp, authenticatorUtils);
            }
        }
        return requestIds;
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
    public InfoRequestData getInfoRequestById(String requestId) {
        return mRequests.get(requestId);
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
    public void reportResults(ArrayList<String> doneRequestsIds) {
        for (String doneRequestId : doneRequestsIds) {
            sendReportResultsBroadcast(doneRequestId);
        }
    }

    public void requestFailed(ArrayList<String> doneRequestsIds) {
        for (String doneRequestId : doneRequestsIds) {
            sendRequestFailedBroadcast(doneRequestId);
        }
    }

    public void onLoggedOpsSent(ArrayList<String> doneRequestsIds, boolean success) {
        List<InfoRequestData> loggedOps = new ArrayList<InfoRequestData>();
        for (String doneRequestId : doneRequestsIds) {
            if (mSentRequests.containsKey(doneRequestId)) {
                InfoRequestData loggedOp = mSentRequests.get(doneRequestId);
                loggedOps.add(loggedOp);
                mLoggedOpsMap.remove(loggedOp.getOpLogId());
            }
        }
        if (success) {
            DatabaseHelper.getInstance().removeOpsFromInfoSystemOpLog(loggedOps);
            if (DatabaseHelper.getInstance().getLoggedOpsCount() == 0) {
                sendOpLogIsEmptiedBroadcast();
            }
        }
    }

    /**
     * Send a broadcast containing the id of the resolved inforequest.
     */
    private void sendReportResultsBroadcast(String requestId) {
        Intent reportIntent = new Intent(INFOSYSTEM_RESULTSREPORTED);
        reportIntent.putExtra(INFOSYSTEM_RESULTSREPORTED_REQUESTID, requestId);
        TomahawkApp.getContext().sendBroadcast(reportIntent);
    }

    /**
     * Send a broadcast indicating that the request failed
     */
    private void sendRequestFailedBroadcast(String requestId) {
        Intent reportIntent = new Intent(INFOSYSTEM_REQUESTFAILED);
        reportIntent.putExtra(INFOSYSTEM_RESULTSREPORTED_REQUESTID, requestId);
        TomahawkApp.getContext().sendBroadcast(reportIntent);
    }

    /**
     * Send a broadcast indicating that the operation log has been emptied
     */
    private void sendOpLogIsEmptiedBroadcast() {
        Intent reportIntent = new Intent(INFOSYSTEM_OPLOGISEMPTIED);
        TomahawkApp.getContext().sendBroadcast(reportIntent);
    }
}
