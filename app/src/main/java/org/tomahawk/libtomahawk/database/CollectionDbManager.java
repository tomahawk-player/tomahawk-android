/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.database;

import org.tomahawk.tomahawk_android.TomahawkApp;

import java.util.HashMap;
import java.util.Map;

public class CollectionDbManager {

    public static final String TAG = CollectionDbManager.class.getSimpleName();

    private static class Holder {

        private static final CollectionDbManager instance = new CollectionDbManager();

    }

    private Map<String, CollectionDb> mCollectionDbs = new HashMap<>();

    private CollectionDbManager() {
    }

    public static CollectionDbManager get() {
        return Holder.instance;
    }

    public synchronized CollectionDb getCollectionDb(String collectionId) {
        CollectionDb db = mCollectionDbs.get(collectionId);
        if (db == null) {
            if (collectionId.equals(TomahawkApp.PLUGINNAME_USERCOLLECTION)) {
                db = new UserCollectionDb(TomahawkApp.getContext(), collectionId);
            } else {
                db = new CollectionDb(TomahawkApp.getContext(), collectionId);
            }
            mCollectionDbs.put(collectionId, db);
        }
        return db;
    }

}
