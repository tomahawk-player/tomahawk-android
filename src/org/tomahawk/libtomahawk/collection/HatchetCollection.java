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

import org.jdeferred.Promise;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.BetterDeferredManager;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.widget.ImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class holds the metadata retrieved via Hatchet.
 */
public class HatchetCollection extends NativeCollection {

    private final ConcurrentHashMap<Artist, List<Query>> mArtistTopHits
            = new ConcurrentHashMap<>();

    public HatchetCollection() {
        super(TomahawkApp.PLUGINNAME_HATCHET, "", true);
    }

    public void addArtistTopHits(Artist artist, List<Query> topHits) {
        mArtistTopHits.put(artist, topHits);
    }

    /**
     * @return A {@link java.util.List} of all top hits {@link Track}s from the given Artist.
     */
    public Promise<List<Query>, Throwable, Void> getArtistTopHits(final Artist artist) {
        BetterDeferredManager dm = new BetterDeferredManager();
        return dm.when(new Callable<List<Query>>() {
            @Override
            public List<Query> call() throws Exception {
                List<Query> queries = new ArrayList<>();
                if (mArtistTopHits.get(artist) != null) {
                    queries.addAll(mArtistTopHits.get(artist));
                }
                return queries;
            }
        });
    }

    @Override
    public void loadIcon(ImageView imageView, boolean grayOut) {
        TomahawkUtils.loadDrawableIntoImageView(TomahawkApp.getContext(), imageView,
                R.drawable.ic_hatchet, grayOut);
    }
}
