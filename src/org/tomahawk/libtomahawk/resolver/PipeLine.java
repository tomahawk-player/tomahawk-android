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
package org.tomahawk.libtomahawk.resolver;

import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.Intent;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link PipeLine} is being used to provide all the resolving functionality. All {@link
 * Resolver}s are stored and invoked here. Callbacks which report the found {@link Result}s are also
 * included in this class.
 */
public class PipeLine {

    public static final int PIPELINE_SEARCHTYPE_TRACKS = 0;

    public static final int PIPELINE_SEARCHTYPE_ARTISTS = 1;

    public static final int PIPELINE_SEARCHTYPE_ALBUMS = 2;

    public static final String PIPELINE_RESULTSREPORTED
            = "org.tomahawk.tomahawk_android.pipeline_resultsreported";

    public static final String PIPELINE_RESULTSREPORTED_QID
            = "org.tomahawk.tomahawk_android.pipeline_resultsreported_qid";

    private static final float MINSCORE = 0.5F;

    private TomahawkApp mTomahawkApp;

    private ArrayList<Resolver> mResolvers = new ArrayList<Resolver>();

    private ConcurrentHashMap<String, Query> mQids = new ConcurrentHashMap<String, Query>();

    private ConcurrentHashMap<String, Query> mWaitingQids = new ConcurrentHashMap<String, Query>();

    private boolean mAllResolversAdded;

    public PipeLine(TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;
    }

    /**
     * Add a {@link Resolver} to the internal list.
     */
    public void addResolver(Resolver resolver) {
        mResolvers.add(resolver);
    }

    /**
     * Get the {@link Resolver} with the given id, null if not found
     */
    public Resolver getResolver(int id) {
        for (Resolver resolver : mResolvers) {
            if (resolver.getId() == id) {
                return resolver;
            }
        }
        return null;
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given fullTextQuery. If there already
     * is a {@link Query} with the same fullTextQuery, the old resultList will be reported.
     */
    public String resolve(String fullTextQuery) {
        return resolve(fullTextQuery, false);
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given fullTextQuery. If there already
     * is a {@link Query} with the same fullTextQuery, the old resultList will be reported.
     */
    public String resolve(String fullTextQuery, boolean forceOnlyLocal) {
        if (fullTextQuery != null && !TextUtils.isEmpty(fullTextQuery)) {
            Query q = new Query(fullTextQuery, forceOnlyLocal);
            resolve(q);
            return q.getQid();
        }
        return null;
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given {@link
     * org.tomahawk.libtomahawk.collection.Track}/{@link org.tomahawk.libtomahawk.collection.Artist}/{@link
     * org.tomahawk.libtomahawk.collection.Album}. If there already is a {@link Query} with the same
     * {@link org.tomahawk.libtomahawk.collection.Track}/{@link org.tomahawk.libtomahawk.collection.Artist}/{@link
     * org.tomahawk.libtomahawk.collection.Album}, the old resultList will be reported.
     */
    public String resolve(String trackName, String albumName, String artistName) {
        if (trackName != null && !TextUtils.isEmpty(trackName)) {
            Query q = new Query(trackName, albumName, artistName, false);
            return resolve(q);
        }
        return null;
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given {@link Query}.
     */
    public String resolve(Query q) {
        return resolve(q, false);
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given {@link Query}.
     */
    public String resolve(Query q, boolean forceOnlyLocal) {
        if (!forceOnlyLocal && q.isSolved()) {
            sendResultsReportBroadcast(q.getQid());
        } else {
            if (!isEveryResolverReady()) {
                if (!mWaitingQids.containsKey(q.getQid())) {
                    mWaitingQids.put(q.getQid(), q);
                }
            } else {
                mQids.put(q.getQid(), q);
                for (Resolver resolver : mResolvers) {
                    if ((forceOnlyLocal && resolver instanceof DataBaseResolver)
                            || (!forceOnlyLocal && q.isOnlyLocal()
                            && resolver instanceof DataBaseResolver)
                            || (!forceOnlyLocal && !q.isOnlyLocal())) {
                        resolver.resolve(q);
                    }
                }
            }
        }
        return q.getQid();
    }

    /**
     * Resolve the given ArrayList of {@link org.tomahawk.libtomahawk.resolver.Query}s and return a
     * HashSet containing all query ids
     */
    public HashSet<String> resolve(ArrayList<Query> queries) {
        return resolve(queries, false);
    }

    /**
     * Resolve the given ArrayList of {@link org.tomahawk.libtomahawk.resolver.Query}s and return a
     * HashSet containing all query ids
     */
    public HashSet<String> resolve(ArrayList<Query> queries, boolean forceOnlyLocal) {
        HashSet<String> qids = new HashSet<String>();
        if (queries != null) {
            for (Query query : queries) {
                if (forceOnlyLocal || !query.isSolved()) {
                    qids.add(resolve(query, forceOnlyLocal));
                }
            }
        }
        return qids;
    }

    /**
     * Send a broadcast containing the id of the resolved {@link Query}.
     */
    private void sendResultsReportBroadcast(String qid) {
        Intent reportIntent = new Intent(PIPELINE_RESULTSREPORTED);
        reportIntent.putExtra(PIPELINE_RESULTSREPORTED_QID, qid);
        mTomahawkApp.sendBroadcast(reportIntent);
    }

    /**
     * If the {@link ScriptResolver} has resolved the {@link Query}, this method will be called.
     * This method will then calculate a score and assign it to every {@link Result}. If the score
     * is higher than MINSCORE the {@link Result} is added to the output resultList.
     *
     * @param qid     the {@link Query} id
     * @param results the unfiltered {@link ArrayList} of {@link Result}s
     */
    public void reportResults(String qid, ArrayList<Result> results) {
        ArrayList<Result> cleanTrackResults = new ArrayList<Result>();
        ArrayList<Result> cleanAlbumResults = new ArrayList<Result>();
        ArrayList<Result> cleanArtistResults = new ArrayList<Result>();
        Query q = getQuery(qid);
        if (q != null && results != null) {
            for (Result r : results) {
                if (r != null) {
                    r.setTrackScore(q.howSimilar(r, PIPELINE_SEARCHTYPE_TRACKS));
                    if (r.getTrackScore() >= MINSCORE && !cleanTrackResults.contains(r)) {
                        r.setType(Result.RESULT_TYPE_TRACK);
                        cleanTrackResults.add(r);
                    }
                    if (q.isFullTextQuery()) {
                        r.setAlbumScore(q.howSimilar(r, PIPELINE_SEARCHTYPE_ALBUMS));
                        if (r.getAlbumScore() >= MINSCORE && !cleanAlbumResults.contains(r)) {
                            r.setType(Result.RESULT_TYPE_ALBUM);
                            cleanAlbumResults.add(r);
                        }
                        r.setArtistScore(q.howSimilar(r, PIPELINE_SEARCHTYPE_ARTISTS));
                        if (r.getArtistScore() >= MINSCORE && !cleanArtistResults.contains(r)) {
                            r.setType(Result.RESULT_TYPE_ARTIST);
                            cleanArtistResults.add(r);
                        }
                    }
                }
            }
            mQids.get(qid).addArtistResults(cleanArtistResults);
            mQids.get(qid).addAlbumResults(cleanAlbumResults);
            mQids.get(qid).addTrackResults(cleanTrackResults);
            sendResultsReportBroadcast(q.getQid());
        }
    }

    /**
     * @return true if one or more {@link ScriptResolver}s are currently resolving. False otherwise
     */
    public boolean isResolving() {
        for (Resolver resolver : mResolvers) {
            if (resolver.isResolving() || !resolver.isReady()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the {@link Query} with the given id
     */
    public Query getQuery(String qid) {
        return mQids.get(qid);
    }

    /**
     * @return whether or not every Resolver in this PipeLine is ready to resolve queries
     */
    public boolean isEveryResolverReady() {
        if (!mAllResolversAdded) {
            return false;
        }
        for (Resolver r : mResolvers) {
            if (!r.isReady()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Callback method, which is being called by Resolvers as soon as they are ready
     */
    public void onResolverReady() {
        if (isEveryResolverReady()) {
            for (Query query : mWaitingQids.values()) {
                mWaitingQids.remove(query.getQid());
                resolve(query);
            }
        }
    }

    public void setAllResolversAdded(boolean allResolversAdded) {
        mAllResolversAdded = allResolversAdded;
    }

    public void onCollectionUpdated() {
        ArrayList<Query> queries = new ArrayList<Query>(mQids.values());
        resolve(queries, true);
    }
}
