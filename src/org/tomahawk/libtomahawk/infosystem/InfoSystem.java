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

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetNowPlaying;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetNowPlayingPostStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaybackLogEntry;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetPlaybackLogPostStruct;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetSocialAction;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetSocialActionPostStruct;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Intent;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The InfoSystem resolves metadata for artists and albums like album covers and artist images.
 */
public class InfoSystem {

    public static final String INFOSYSTEM_RESULTSREPORTED = "infosystem_resultsreported";

    public static final String INFOSYSTEM_OPLOGISEMPTIED = "infosystem_oplogisempty";

    public static final String INFOSYSTEM_RESULTSREPORTED_REQUESTID
            = "infosystem_resultsreported_requestid";

    public static final String PARAM_NAME = "name";

    public static final String PARAM_ARTIST_NAME = "artist_name";

    public static final String PARAM_TERM = "term";

    TomahawkApp mTomahawkApp;

    private ArrayList<InfoPlugin> mInfoPlugins = new ArrayList<InfoPlugin>();

    private ConcurrentHashMap<String, InfoRequestData> mRequests
            = new ConcurrentHashMap<String, InfoRequestData>();

    private ConcurrentHashMap<String, InfoRequestData> mSentLoggedOps
            = new ConcurrentHashMap<String, InfoRequestData>();

    /**
     * The below HashSets are used to mark the included objects as "already done before"
     */
    private HashSet<Artist> mArtistHashSet = new HashSet<Artist>();

    private HashSet<Artist> mArtistTopHitsHashSet = new HashSet<Artist>();

    private HashSet<Artist> mArtistAlbumsHashSet = new HashSet<Artist>();

    private HashSet<Album> mAlbumHashSet = new HashSet<Album>();

    private Query mLastPlaybackLogEntry = null;

    private Query mNowPlaying = null;

    public InfoSystem(TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;
    }

    public void addInfoPlugin(InfoPlugin infoPlugin) {
        mInfoPlugins.add(infoPlugin);
    }

    /**
     * HatchetSearch the added InfoPlugins with the given keyword
     *
     * @return the created InfoRequestData's requestId
     */
    public String resolve(String keyword) {
        Multimap<String, String> params = HashMultimap.create(1, 1);
        params.put(PARAM_TERM, keyword);
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
            if (!mArtistHashSet.contains(artist)) {
                mArtistHashSet.add(artist);
                Multimap<String, String> params = HashMultimap.create(1, 1);
                params.put(PARAM_NAME, artist.getName());
                String requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS, params,
                        artist);
                requestIds.add(requestId);
            }
            if (!mArtistTopHitsHashSet.contains(artist) && !justImage) {
                mArtistTopHitsHashSet.add(artist);
                Multimap<String, String> params = HashMultimap.create(1, 1);
                params.put(PARAM_NAME, artist.getName());
                String requestId = resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ARTISTS_TOPHITS,
                        params, artist);
                requestIds.add(requestId);
            }
            if (!mArtistAlbumsHashSet.contains(artist) && !justImage) {
                mArtistAlbumsHashSet.add(artist);
                Multimap<String, String> params = HashMultimap.create(1, 1);
                params.put(PARAM_NAME, artist.getName());
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
        if (album != null && !mAlbumHashSet.contains(album)) {
            mAlbumHashSet.add(album);
            Multimap<String, String> params = HashMultimap.create(2, 1);
            params.put(PARAM_NAME, album.getName());
            params.put(PARAM_ARTIST_NAME, album.getArtist().getName());
            return resolve(InfoRequestData.INFOREQUESTDATA_TYPE_ALBUMS, params, album);
        }
        return null;
    }

    /**
     * Build an InfoRequestData object with the given data and order results
     *
     * @param type   the type of the InfoRequestData object
     * @param params a MultiMap of all parameters to be given to the InfoPlugin
     * @return the created InfoRequestData's requestId
     */
    public String resolve(int type, Multimap<String, String> params) {
        String requestId = TomahawkApp.getSessionUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId, type, params);
        resolve(infoRequestData);
        return infoRequestData.getRequestId();
    }

    /**
     * Build an InfoRequestData object with the given data and order results
     *
     * @param type           the type of the InfoRequestData object
     * @param params         a MultiMap of all parameters to be given to the InfoPlugin
     * @param itemToBeFilled the item to automatically be filled after the InfoPlugin fetched the
     *                       results from its source
     * @return the created InfoRequestData's requestId
     */
    public String resolve(int type, Multimap<String, String> params,
            TomahawkListItem itemToBeFilled) {
        String requestId = TomahawkApp.getSessionUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId, type, params);
        resolve(infoRequestData, itemToBeFilled);
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

            String requestId = TomahawkApp.getSessionUniqueStringId();
            InfoRequestData infoRequestData = new InfoRequestData(requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES,
                    playbackLogPostStruct);
            mTomahawkApp.getUserPlaylistsDataSource()
                    .addOpToInfoSystemOpLog(infoRequestData, (int) (timeStamp / 1000));
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

            String requestId = TomahawkApp.getSessionUniqueStringId();
            InfoRequestData infoRequestData = new InfoRequestData(requestId,
                    InfoRequestData.INFOREQUESTDATA_TYPE_PLAYBACKLOGENTRIES_NOWPLAYING,
                    nowPlayingPostStruct);
            send(infoRequestData, authenticatorUtils);
        }
    }

    public void sendSocialActionPostStruct(AuthenticatorUtils authenticatorUtils, Query query,
            String type, boolean action) {
        long timeStamp = System.currentTimeMillis();
        HatchetSocialAction socialAction = new HatchetSocialAction();
        socialAction.type = type;
        socialAction.action = String.valueOf(action);
        socialAction.trackString = query.getName();
        socialAction.artistString = query.getArtist().getName();
        socialAction.timestamp = new Date(timeStamp);
        HatchetSocialActionPostStruct socialActionPostStruct = new HatchetSocialActionPostStruct();
        socialActionPostStruct.socialAction = socialAction;

        String requestId = TomahawkApp.getSessionUniqueStringId();
        InfoRequestData infoRequestData = new InfoRequestData(requestId,
                InfoRequestData.INFOREQUESTDATA_TYPE_SOCIALACTIONS, socialActionPostStruct);
        mTomahawkApp.getUserPlaylistsDataSource()
                .addOpToInfoSystemOpLog(infoRequestData, (int) (timeStamp / 1000));
        sendLoggedOps(authenticatorUtils);
    }

    public List<String> sendLoggedOps(AuthenticatorUtils authenticatorUtils) {
        List<String> requestIds = new ArrayList<String>();
        List<InfoRequestData> loggedOps = mTomahawkApp.getUserPlaylistsDataSource().getLoggedOps();
        for (InfoRequestData loggedOp : loggedOps) {
            requestIds.add(loggedOp.getRequestId());
            mSentLoggedOps.put(loggedOp.getRequestId(), loggedOp);
            send(loggedOp, authenticatorUtils);
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
        mRequests.put(infoRequestData.getRequestId(), infoRequestData);
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
     * Method to enable InfoPlugins to report that the InfoRequestData objects with the given
     * requestIds have received their results
     */
    public void reportResults(ArrayList<String> doneRequestsIds) {
        for (String doneRequestId : doneRequestsIds) {
            if (mSentLoggedOps.containsKey(doneRequestId)) {
                InfoRequestData loggedOp = mSentLoggedOps.get(doneRequestId);
                mTomahawkApp.getUserPlaylistsDataSource()
                        .removeOpFromInfoSystemOpLog(loggedOp.getOpLogId());
                if (mTomahawkApp.getUserPlaylistsDataSource().getLoggedOps().isEmpty()) {
                    sendOpLogIsEmptiedBroadcast();
                }
            }
            sendReportResultsBroadcast(doneRequestId);
        }
    }

    /**
     * Send a broadcast containing the id of the resolved inforequest.
     */
    private void sendReportResultsBroadcast(String requestId) {
        Intent reportIntent = new Intent(INFOSYSTEM_RESULTSREPORTED);
        reportIntent.putExtra(INFOSYSTEM_RESULTSREPORTED_REQUESTID, requestId);
        mTomahawkApp.sendBroadcast(reportIntent);
    }

    /**
     * Send a broadcast indicating that the operation log has been emptied
     */
    private void sendOpLogIsEmptiedBroadcast() {
        Intent reportIntent = new Intent(INFOSYSTEM_OPLOGISEMPTIED);
        mTomahawkApp.sendBroadcast(reportIntent);
    }
}
