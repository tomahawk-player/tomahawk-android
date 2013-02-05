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

import java.util.ArrayList;
import java.util.HashMap;

import android.text.TextUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Application;
import android.content.Intent;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 19.01.13
 */
public class PipeLine {
    public static final String PIPELINE_RESULTSREPORTED = "pipeline_resultsreported";
    public static final String PIPELINE_RESULTSREPORTED_QID = "pipeline_resultsreported_qid";

    private static final float MINSCORE = 0.3F;

    Application mApplication;

    private ArrayList<Resolver> mResolvers = new ArrayList<Resolver>();

    private ArrayList<Query> mPendingQueries = new ArrayList<Query>();
    private ArrayList<Query> mTemporaryQueries = new ArrayList<Query>();
    private HashMap<String, Query> mQids = new HashMap<String, Query>();

    public PipeLine(Application application) {
        mApplication = application;
        init();
    }

    /**
     * Initilialize the pipeline. Load every resolver.
     */
    private void init() {
        ScriptResolver scriptResolver = new ScriptResolver((TomahawkApp) mApplication, "js/jamendo/jamendo-resolver.js");
        addResolver(scriptResolver);
        scriptResolver = new ScriptResolver((TomahawkApp) mApplication, "js/official.fm/officialfm.js");
        addResolver(scriptResolver);
        scriptResolver = new ScriptResolver((TomahawkApp) mApplication, "js/exfm/exfm.js");
        addResolver(scriptResolver);
        scriptResolver = new ScriptResolver((TomahawkApp) mApplication, "js/soundcloud/soundcloud.js");
        addResolver(scriptResolver);
    }

    /**
     * Add a resolver to the internal list.
     * @param resolver
     */
    public void addResolver(Resolver resolver) {
        mResolvers.add(resolver);
    }

    /**
     * This will invoke every resolver to resolve the given fullTextQuery.
     * If there already is a Query with the same fullTextQuery, the old resultList will be reported.
     * @param fullTextQuery
     */
    public void resolve(String fullTextQuery) {
        if (fullTextQuery != null && !TextUtils.isEmpty(fullTextQuery)) {
            Query q = null;
            for (Query query : mQids.values())
                if (query.getFullTextQuery() == fullTextQuery)
                    q = query;
            if (q == null)
                q = new Query(((TomahawkApp) mApplication).getUniqueQueryId(), fullTextQuery);
            if (!mQids.containsKey(q.getQid())) {
                mQids.put(q.getQid(), q);
                for (Resolver resolver : mResolvers) {
                    resolver.resolve(q);
                }
            } else if (q.isSolved()) {
                sendReportResultsBroadcast(q.getQid());
            }
        }
    }

    /**
     * This will invoke every resolver to resolve the given Query.
     * @param q
     */
    public void resolve(Query q) {
        if (!mQids.containsKey(q.getQid())) {
            mQids.put(q.getQid(), q);
            for (Resolver resolver : mResolvers) {
                resolver.resolve(q);
            }
        } else if (q.isSolved()) {
            sendReportResultsBroadcast(q.getQid());
        }
    }

    /**
     * Send a broadcast containing the id of the resolved query.
     * @param qid
     */
    private void sendReportResultsBroadcast(String qid) {
        Intent reportIntent = new Intent(PIPELINE_RESULTSREPORTED);
        reportIntent.putExtra(PIPELINE_RESULTSREPORTED_QID, qid);
        mApplication.sendBroadcast(reportIntent);
    }

    /**
     * If the ScriptResolver has resolved the query, this method will be called.
     * This method will then calculate a score and assign it to every result. If the score is higher than MINSCORE
     * the result is added to the output resultList.
     * @param qid the query id
     * @param results the unfiltered ArrayList<Result>
     */
    public void reportResults(String qid, ArrayList<Result> results) {
        ArrayList<Result> cleanResults = new ArrayList<Result>();
        Query q = getQuery(qid);
        if (q != null) {
            for (Result r : results) {
                float score = 0F;
                if (r != null) {
                    score = q.howSimilar(r);
                    r.setScore(score);
                }
                if (q.isFullTextQuery() && score >= MINSCORE) {
                    cleanResults.add(r);
                }
            }
            mQids.get(qid).addResults(cleanResults);
            sendReportResultsBroadcast(qid);
        }
    }

    /**
     * @return true if one or more ScriptResolvers are currently resolving. False otherwise
     */
    public boolean isResolving() {
        boolean isResolving = false;
        for (Resolver resolver : mResolvers)
            isResolving = resolver.isResolving();
        return isResolving;
    }

    /**
     * Get the query with the given id
     * @param qid
     * @return
     */
    public Query getQuery(String qid) {
        return mQids.get(qid);
    }

}
