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
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 19.01.13
 */
public class PipeLine {

    public static final int PIPELINE_SEARCHTYPE_TRACKS = 0;

    public static final int PIPELINE_SEARCHTYPE_ARTISTS = 1;

    public static final int PIPELINE_SEARCHTYPE_ALBUMS = 2;

    public static final String PIPELINE_RESULTSREPORTED_FULLTEXTQUERY
            = "pipeline_resultsreported_fulltextquery";

    public static final String PIPELINE_RESULTSREPORTED_NON_FULLTEXTQUERY
            = "pipeline_resultsreported_non_fulltextquery";

    public static final String PIPELINE_RESULTSREPORTED_QID = "pipeline_resultsreported_qid";

    private static final float MINSCORE = 0.5F;

    TomahawkApp mTomahawkApp;

    private ArrayList<Resolver> mResolvers = new ArrayList<Resolver>();

    private ConcurrentHashMap<String, Query> mQids = new ConcurrentHashMap<String, Query>();

    private ConcurrentHashMap<String, String> mQueryMap = new ConcurrentHashMap<String, String>();

    public PipeLine(TomahawkApp tomahawkApp) {
        mTomahawkApp = tomahawkApp;
    }

    /**
     * Add a resolver to the internal list.
     */
    public void addResolver(Resolver resolver) {
        mResolvers.add(resolver);
    }

    /**
     * Get the resolver with the given id, null if not found
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
     * This will invoke every resolver to resolve the given fullTextQuery. If there already is a
     * Query with the same fullTextQuery, the old resultList will be reported.
     */
    public String resolve(String fullTextQuery) {
        return resolve(fullTextQuery, false);
    }

    /**
     * This will invoke every resolver to resolve the given fullTextQuery. If there already is a
     * Query with the same fullTextQuery, the old resultList will be reported.
     */
    public String resolve(String fullTextQuery, boolean onlyLocal) {
        Query q = null;
        if (fullTextQuery != null && !TextUtils.isEmpty(fullTextQuery)) {
            if (mQueryMap.contains(Query.constructCacheKey(fullTextQuery)) &&
                    mQids.get(mQueryMap.get(Query.constructCacheKey(fullTextQuery))).isOnlyLocal()
                            == onlyLocal) {
                q = mQids.get(mQueryMap.get(Query.constructCacheKey(fullTextQuery)));
            }
            if (q == null) {
                q = new Query(mTomahawkApp.getUniqueQueryId(), fullTextQuery, onlyLocal);
            }
            resolve(q, onlyLocal);
        }
        return q == null ? null : q.getQid();
    }

    /**
     * This will invoke every resolver to resolve the given track/artist/album. If there already is
     * a Query with the same track/artist/album, the old resultList will be reported.
     */
    public String resolve(String trackName, String albumName, String artistName) {
        return resolve(trackName, albumName, artistName, false);
    }

    /**
     * This will invoke every resolver to resolve the given track/artist/album. If there already is
     * a Query with the same track/artist/album, the old resultList will be reported.
     */
    public String resolve(String trackName, String albumName, String artistName,
            boolean onlyLocal) {
        Query q = null;
        if (trackName != null && !TextUtils.isEmpty(trackName)) {
            if (mQueryMap.contains(Query.constructCacheKey(trackName, albumName, artistName)) &&
                    mQids.get(mQueryMap
                            .get(Query.constructCacheKey(trackName, albumName, artistName)))
                            .isOnlyLocal() == onlyLocal) {
                q = mQids.get(mQueryMap
                        .get(Query.constructCacheKey(trackName, albumName, artistName)));
            }
            if (q == null) {
                q = new Query(mTomahawkApp.getUniqueQueryId(), trackName, albumName, artistName,
                        onlyLocal);
            }
            resolve(q, onlyLocal);
        }
        return q.getQid();
    }

    /**
     * This will invoke every resolver to resolve the given Query.
     */
    public void resolve(Query q) {
        resolve(q, false);
    }

    /**
     * This will invoke every resolver to resolve the given Query.
     */
    public void resolve(Query q, boolean onlyLocal) {
        if (q.isSolved()) {
            if (q.isFullTextQuery()) {
                sendReportFulltextQueryResultsBroadcast(q.getQid());
            } else {
                sendReportNonFulltextQueryResultsBroadcast(q.getQid());
            }
        } else if (!mQids.containsKey(q.getQid())) {
            mQids.put(q.getQid(), q);
            mQueryMap.put(q.getCacheKey(), q.getQid());
            for (Resolver resolver : mResolvers) {
                if ((onlyLocal && resolver instanceof DataBaseResolver) || !onlyLocal) {
                    resolver.resolve(q);
                    q.incResolversTodoCount();
                }
            }
        } else {
            for (Resolver resolver : mResolvers) {
                if ((onlyLocal && resolver instanceof DataBaseResolver) || !onlyLocal) {
                    resolver.resolve(q);
                }
            }
        }
    }

    /**
     * Send a broadcast containing the id of the resolved query.
     */
    private void sendReportFulltextQueryResultsBroadcast(String qid) {
        Intent reportIntent = new Intent(PIPELINE_RESULTSREPORTED_FULLTEXTQUERY);
        reportIntent.putExtra(PIPELINE_RESULTSREPORTED_QID, qid);
        mTomahawkApp.sendBroadcast(reportIntent);
    }

    /**
     * Send a broadcast containing the id of the resolved query.
     */
    private void sendReportNonFulltextQueryResultsBroadcast(String qid) {
        Intent reportIntent = new Intent(PIPELINE_RESULTSREPORTED_NON_FULLTEXTQUERY);
        reportIntent.putExtra(PIPELINE_RESULTSREPORTED_QID, qid);
        mTomahawkApp.sendBroadcast(reportIntent);
    }

    /**
     * If the ScriptResolver has resolved the query, this method will be called. This method will
     * then calculate a score and assign it to every result. If the score is higher than MINSCORE
     * the result is added to the output resultList.
     *
     * @param qid     the query id
     * @param results the unfiltered ArrayList<Result>
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
                    if (r.getTrackScore() >= MINSCORE) {
                        cleanTrackResults.add(r);
                    }
                    r.setAlbumScore(q.howSimilar(r, PIPELINE_SEARCHTYPE_ALBUMS));
                    if (r.getAlbumScore() >= MINSCORE) {
                        cleanAlbumResults.add(r);
                    }
                    r.setArtistScore(q.howSimilar(r, PIPELINE_SEARCHTYPE_ARTISTS));
                    if (r.getArtistScore() >= MINSCORE) {
                        cleanArtistResults.add(r);
                    }
                }
            }
            mQids.get(qid).addArtistResults(cleanArtistResults);
            //            mQids.get(qid).addAlbumResults(cleanAlbumResults);
            mQids.get(qid).addTrackResults(cleanTrackResults);
            mQids.get(qid).incResolversDoneCount();
            if (q.isFullTextQuery()) {
                sendReportFulltextQueryResultsBroadcast(q.getQid());
            } else {
                sendReportNonFulltextQueryResultsBroadcast(q.getQid());
            }
        }
    }

    /**
     * @return true if one or more ScriptResolvers are currently resolving. False otherwise
     */
    public boolean isResolving() {
        for (Resolver resolver : mResolvers) {
            if (resolver.isResolving()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get the query with the given id
     */
    public Query getQuery(String qid) {
        return mQids.get(qid);
    }
}
