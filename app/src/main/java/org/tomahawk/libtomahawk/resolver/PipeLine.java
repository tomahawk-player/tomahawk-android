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

import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.DbCollection;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.resolver.models.ScriptResolverUrlResult;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkRunnable;

import android.text.TextUtils;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

/**
 * The {@link PipeLine} is being used to provide all the resolving functionality. All {@link
 * Resolver}s are stored and invoked here. Callbacks which report the found {@link Result}s are also
 * included in this class.
 */
public class PipeLine {

    private final static String TAG = PipeLine.class.getSimpleName();

    public static final int URL_TYPE_PLAYLIST = 1;

    public static final int URL_TYPE_TRACK = 2;

    public static final int URL_TYPE_ALBUM = 3;

    public static final int URL_TYPE_ARTIST = 4;

    public static final int URL_TYPE_XSPFURL = 5;

    private static final float MINSCORE = 0.5f;

    private static final float FULLTEXT_MINSCORE = 0f;

    private static class Holder {

        private static final PipeLine instance = new PipeLine();

    }

    public static class ResultsEvent {

        public Query mQuery;
    }

    public static class UrlResultsEvent {

        public Resolver mResolver;

        public ScriptResolverUrlResult mResult;
    }

    public static class ResolversChangedEvent {

        public ScriptResolver mScriptResolver;

        public boolean mManuallyAdded;

    }

    private final Set<ScriptAccount> mScriptAccounts =
            Collections.newSetFromMap(new ConcurrentHashMap<ScriptAccount, Boolean>());

    private final Set<ScriptAccount> mManualScriptAccounts =
            Collections.newSetFromMap(new ConcurrentHashMap<ScriptAccount, Boolean>());

    private final Set<ScriptResolver> mResolvers =
            Collections.newSetFromMap(new ConcurrentHashMap<ScriptResolver, Boolean>());

    private final Set<String> mWaitingUrlLookups =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final Set<Query> mWaitingQueries =
            Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    private final Set<ScriptAccount> mLoadingPlugins =
            Collections.newSetFromMap(new ConcurrentHashMap<ScriptAccount, Boolean>());

    private PipeLine() {
        try {
            String[] plugins = TomahawkApp.getContext().getAssets().list("js/resolvers");
            for (String plugin : plugins) {
                String path = "/js/resolvers/" + plugin;
                ScriptAccount account = new ScriptAccount(path, false);
                mScriptAccounts.add(account);
                mLoadingPlugins.add(account);
            }
            String manualResolverDirPath = TomahawkApp.getContext().getFilesDir().getAbsolutePath()
                    + File.separator + "manualresolvers";
            File manualResolverDir = new File(manualResolverDirPath);
            plugins = manualResolverDir.list();
            if (plugins != null) {
                for (String plugin : plugins) {
                    if (!plugin.equals(".temp")) {
                        String pluginPath = manualResolverDirPath + File.separator + plugin;
                        File pluginFile = new File(pluginPath);
                        if (pluginFile.isDirectory()) {
                            ScriptAccount account = new ScriptAccount(pluginPath, true);
                            mScriptAccounts.add(account);
                            mLoadingPlugins.add(account);
                        }
                    }
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "PipeLine<init>: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
    }

    public static PipeLine get() {
        return Holder.instance;
    }

    public void onPluginLoaded(ScriptAccount account) {
        mLoadingPlugins.remove(account);
        if (mLoadingPlugins.isEmpty()) {
            Log.d(TAG, "All plugins loaded. Resolving "
                    + mWaitingQueries.size() + " waiting queries. Looking up "
                    + mWaitingUrlLookups.size() + " waiting URLs.");
            for (Query query : mWaitingQueries) {
                resolve(query);
            }
            mWaitingQueries.clear();
            for (String url : mWaitingUrlLookups) {
                lookupUrl(url);
            }
            mWaitingUrlLookups.clear();
        }
    }

    public void addScriptAccount(ScriptAccount scriptAccount) {
        mManualScriptAccounts.add(scriptAccount);
        mScriptAccounts.add(scriptAccount);
        mLoadingPlugins.add(scriptAccount);
    }

    public void addResolver(ScriptResolver resolver) {
        mResolvers.add(resolver);
        ResolversChangedEvent event = new ResolversChangedEvent();
        event.mScriptResolver = resolver;
        event.mManuallyAdded = mManualScriptAccounts.contains(resolver.getScriptAccount());
        EventBus.getDefault().post(event);
    }

    public void removeResolver(ScriptResolver resolver) {
        mResolvers.remove(resolver);
        EventBus.getDefault().post(new ResolversChangedEvent());
    }

    /**
     * Get the {@link ScriptResolver} with the given id, null if not found
     */
    public ScriptResolver getResolver(String id) {
        for (ScriptResolver resolver : mResolvers) {
            if (resolver.getId().equals(id)) {
                return resolver;
            }
        }
        return null;
    }

    /**
     * Get the ArrayList of all {@link org.tomahawk.libtomahawk.resolver.ScriptResolver}s
     */
    public ArrayList<ScriptResolver> getScriptResolvers() {
        ArrayList<ScriptResolver> scriptResolvers = new ArrayList<>();
        for (Resolver resolver : mResolvers) {
            if (resolver instanceof ScriptResolver) {
                scriptResolvers.add((ScriptResolver) resolver);
            }
        }
        return scriptResolvers;
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given fullTextQuery. If there already
     * is a {@link Query} with the same fullTextQuery, the old resultList will be reported.
     */
    public Query resolve(String fullTextQuery) {
        return resolve(fullTextQuery, false);
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given fullTextQuery. If there already
     * is a {@link Query} with the same fullTextQuery, the old resultList will be reported.
     */
    public Query resolve(String fullTextQuery, boolean forceOnlyLocal) {
        if (fullTextQuery != null && !TextUtils.isEmpty(fullTextQuery)) {
            Query q = Query.get(fullTextQuery, forceOnlyLocal);
            resolve(q, forceOnlyLocal);
            return q;
        }
        return null;
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given {@link Query}.
     */
    public Query resolve(Query q) {
        return resolve(q, false);
    }

    /**
     * This will invoke every {@link Resolver} to resolve the given {@link Query}.
     */
    public Query resolve(final Query q, final boolean forceOnlyLocal) {
        final TomahawkRunnable r = new TomahawkRunnable(TomahawkRunnable.PRIORITY_IS_RESOLVING) {
            @Override
            public void run() {
                if (!mLoadingPlugins.isEmpty()) {
                    mWaitingQueries.add(q);
                } else {
                    for (ScriptResolver resolver : mResolvers) {
                        if (shouldResolve(resolver, q, forceOnlyLocal)) {
                            resolver.resolve(q);
                        }
                    }
                    for (Collection collection : CollectionManager.get().getCollections()) {
                        if (!(collection instanceof UserCollection)
                                && shouldResolve(collection, q, forceOnlyLocal)) {
                            ((DbCollection) collection).resolve(q);
                        }
                    }
                }
                if (shouldResolve(CollectionManager.get().getUserCollection(), q, forceOnlyLocal)) {
                    CollectionManager.get().getUserCollection().resolve(q);
                }
            }
        };
        ThreadManager.get().execute(r, q);
        return q;
    }

    /**
     * Method to determine if a given Resolver should resolve the query or not
     */
    public boolean shouldResolve(Collection collection, Query q, boolean forceOnlyLocal) {
        return !forceOnlyLocal && !q.isOnlyLocal() && collection instanceof DbCollection
                || collection instanceof UserCollection;
    }

    /**
     * Method to determine if a given Resolver should resolve the query or not
     */
    public boolean shouldResolve(Resolver resolver, Query q, boolean forceOnlyLocal) {
        return !forceOnlyLocal && !q.isOnlyLocal() && resolver.isEnabled();
    }

    /**
     * Resolve the given ArrayList of {@link org.tomahawk.libtomahawk.resolver.Query}s and return a
     * HashSet containing all query ids
     */
    public HashSet<Query> resolve(Set<Query> queries) {
        return resolve(queries, false);
    }

    /**
     * Resolve the given ArrayList of {@link org.tomahawk.libtomahawk.resolver.Query}s and return a
     * HashSet containing all query keys
     */
    public HashSet<Query> resolve(Set<Query> queries, boolean forceOnlyLocal) {
        HashSet<Query> queryKeys = new HashSet<>();
        if (queries != null) {
            for (Query query : queries) {
                queryKeys.add(resolve(query, forceOnlyLocal));
            }
        }
        return queryKeys;
    }

    /**
     * If the {@link ScriptResolver} has resolved the {@link Query}, this method will be called.
     * This method will then calculate a score and assign it to every {@link Result}. If the score
     * is higher than MINSCORE the {@link Result} is added to the output resultList.
     *
     * @param query   the {@link Query} that results are being reported for
     * @param results the unfiltered {@link ArrayList} of {@link Result}s
     */
    public void reportResults(final Query query, final ArrayList<Result> results,
            final String resolverId) {
        int priority;
        if (TomahawkApp.PLUGINNAME_USERCOLLECTION.equals(resolverId)) {
            priority = TomahawkRunnable.PRIORITY_IS_REPORTING_LOCALSOURCE;
        } else if (TomahawkApp.PLUGINNAME_SPOTIFY.equals(resolverId)
                || TomahawkApp.PLUGINNAME_DEEZER.equals(resolverId)
                || TomahawkApp.PLUGINNAME_BEATSMUSIC.equals(resolverId)) {
            priority = TomahawkRunnable.PRIORITY_IS_REPORTING_SUBSCRIPTION;
        } else {
            priority = TomahawkRunnable.PRIORITY_IS_REPORTING;
        }
        ThreadManager.get().execute(
                new TomahawkRunnable(priority) {
                    @Override
                    public void run() {
                        if (query != null) {
                            boolean shouldReport = query.isFullTextQuery();
                            for (Result r : results) {
                                if (r != null) {
                                    float trackScore = query.howSimilar(r);
                                    float goalScore = query.isFullTextQuery()
                                            ? FULLTEXT_MINSCORE : MINSCORE;
                                    if (trackScore > goalScore) {
                                        Result before = query.getPreferredTrackResult();
                                        query.addTrackResult(r, trackScore);
                                        if (before != query.getPreferredTrackResult()) {
                                            shouldReport = true;
                                        }
                                    }
                                }
                            }
                            if (shouldReport) {
                                ResultsEvent event = new ResultsEvent();
                                event.mQuery = query;
                                EventBus.getDefault().post(event);
                            }
                        }
                    }
                }
        );
    }

    public void lookupUrl(final String url) {
        Log.d(TAG, "lookupUrl - looking up url: " + url);
        if (!mLoadingPlugins.isEmpty()) {
            mWaitingUrlLookups.add(url);
        } else {
            for (Resolver resolver : mResolvers) {
                if (resolver instanceof ScriptResolver) {
                    ScriptResolver scriptResolver = (ScriptResolver) resolver;
                    scriptResolver.lookupUrl(url);
                }
            }
        }
    }

}
