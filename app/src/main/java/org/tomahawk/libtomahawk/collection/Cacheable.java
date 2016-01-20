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
package org.tomahawk.libtomahawk.collection;

import android.util.Log;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This interface represents an item that can provide a corresponding cache key.
 */
public abstract class Cacheable {

    private static final String TAG = Cacheable.class.getSimpleName();

    private static final Map<Class, Map<String, Cacheable>> sCaches = new ConcurrentHashMap<>();

    private String mCacheKey;

    protected Cacheable(Class clss, String cacheKey) {
        mCacheKey = cacheKey;

        getCache(clss).put(cacheKey, this);
    }

    protected static void put(Class clss, String cacheKey, Cacheable cacheable) {
        getCache(clss).put(cacheKey, cacheable);
    }

    public String getCacheKey() {
        return mCacheKey;
    }

    private static Map<String, Cacheable> getCache(Class clss) {
        Map<String, Cacheable> cache = sCaches.get(clss);
        if (cache == null) {
            cache = new ConcurrentHashMap<>();
            sCaches.put(clss, cache);
        }
        return cache;
    }

    protected static Cacheable get(Class clss, String cacheKey) {
        return getCache(clss).get(cacheKey);
    }

    protected static String getCacheKey(Object... objects) {
        String result = "";
        for (int i = 0; i < objects.length; i++) {
            Object o = objects[i];
            if (o != null) {
                if (i > 0) {
                    result += "\t\t";
                }
                if (o instanceof String) {
                    result += ((String) o);
                } else if (o instanceof Boolean) {
                    result += ((Boolean) o) ? "1" : "0";
                } else {
                    Log.e(TAG, "getCacheKey - given Object type is not supported!");
                }
            }
        }
        return result;
    }
}