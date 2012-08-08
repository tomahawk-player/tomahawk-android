/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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
package org.tomahawk.libtomahawk;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;

/**
 * Represents a Loader for a Collection.
 * 
 * This class aids in refreshing and keeping the Collection views in sync with
 * their underlying Collection.
 */
public class CollectionLoader extends AsyncTaskLoader<Collection> {

    private Collection mCollection;

    /**
     * Consutructs a new CollectionLoader.
     * 
     * @param context
     */
    public CollectionLoader(Context context, Collection coll) {
        super(context);

        mCollection = coll;
    }

    /**
     * Load the Collection in the background.
     */
    @Override
    public Collection loadInBackground() {
        return mCollection;
    }

    /**
     * Called when the results of this Loader need to be delivered.
     */
    @Override
    public void deliverResult(Collection coll) {
        if (isStarted())
            super.deliverResult(coll);
    }

    /**
     * Called when this Loader needs to start loading.
     */
    @Override
    protected void onStartLoading() {
        deliverResult(mCollection);
    }
}
