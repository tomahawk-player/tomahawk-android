/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.mediaplayers.TomahawkMediaPlayer;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ThreadManager {

    /*
     * Gets the number of available cores
     * (not always the same as the maximum number of cores)
     */
    private static final int NUMBER_OF_CORES = Runtime.getRuntime().availableProcessors();

    // Sets the amount of time an idle thread waits before terminating
    private static final int KEEP_ALIVE_TIME = 1;

    // Sets the Time Unit to seconds
    private static final TimeUnit KEEP_ALIVE_TIME_UNIT = TimeUnit.SECONDS;

    private static class Holder {

        private static final ThreadManager instance = new ThreadManager();

    }

    private final ThreadPoolExecutor mThreadPool;

    private final ConcurrentHashMap<TomahawkMediaPlayer, ThreadPoolExecutor> mPlaybackThreadPools
            = new ConcurrentHashMap<>();

    private final Map<Query, Collection<TomahawkRunnable>> mQueryRunnableMap;

    private ThreadManager() {
        mQueryRunnableMap = new ConcurrentHashMap<>();
        mThreadPool = new ThreadPoolExecutor(NUMBER_OF_CORES, NUMBER_OF_CORES,
                KEEP_ALIVE_TIME, KEEP_ALIVE_TIME_UNIT, new PriorityBlockingQueue<Runnable>());
    }

    public static ThreadManager get() {
        return Holder.instance;
    }

    public void execute(TomahawkRunnable r) {
        mThreadPool.execute(r);
    }

    public void execute(TomahawkRunnable r, Query query) {
        Collection<TomahawkRunnable> runnables = mQueryRunnableMap.get(query);
        if (runnables == null) {
            runnables = new HashSet<>();
        }
        runnables.add(r);
        mQueryRunnableMap.put(query, runnables);
        mThreadPool.execute(r);
    }

    public boolean stop(Query query) {
        boolean success = false;
        Collection<TomahawkRunnable> runnables = mQueryRunnableMap.remove(query);
        if (runnables != null) {
            for (TomahawkRunnable r : runnables) {
                mThreadPool.remove(r);
                success = true;
            }
        }
        return success;
    }

    public void executePlayback(TomahawkMediaPlayer mp, Runnable r) {
        ThreadPoolExecutor pool = mPlaybackThreadPools.get(mp);
        if (pool == null) {
            pool = new ThreadPoolExecutor(1, 1, KEEP_ALIVE_TIME,
                    KEEP_ALIVE_TIME_UNIT, new LinkedBlockingQueue<Runnable>());
            mPlaybackThreadPools.put(mp, pool);
        }
        pool.execute(r);
    }

    public boolean isActive() {
        for (ThreadPoolExecutor pool : mPlaybackThreadPools.values()) {
            if (pool.getActiveCount() > 0 || pool.getQueue().size() > 0) {
                return true;
            }
        }
        return mThreadPool.getActiveCount() > 0 || mThreadPool.getQueue().size() > 0;
    }
}
