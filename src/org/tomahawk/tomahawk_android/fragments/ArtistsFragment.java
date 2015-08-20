/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.fragments;

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.CollectionCursor;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Artist}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class ArtistsFragment extends TomahawkFragment {

    public static final String COLLECTION_ARTISTS_SPINNER_POSITION
            = "org.tomahawk.tomahawk_android.collection_artists_spinner_position";

    @Override
    public void onResume() {
        super.onResume();

        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, final Object item) {
        if (item instanceof Artist) {
            Artist artist = (Artist) item;
            mCollection.getArtistAlbums(artist).done(new DoneCallback<CollectionCursor<Album>>() {
                @Override
                public void onDone(CollectionCursor<Album> cursor) {
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.ARTIST,
                            ((Artist) item).getCacheKey());
                    if (cursor != null && cursor.size() > 0) {
                        bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
                    } else {
                        bundle.putString(TomahawkFragment.COLLECTION_ID,
                                TomahawkApp.PLUGINNAME_HATCHET);
                    }
                    if (cursor != null) {
                        cursor.close();
                    }
                    bundle.putInt(CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
                    bundle.putLong(CONTAINER_FRAGMENT_ID,
                            TomahawkMainActivity.getSessionUniqueId());
                    FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                            ArtistPagerFragment.class, bundle);
                }
            });
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter}
     * content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        if (mArtistArray != null) {
            fillAdapter(new Segment(mArtistArray));
        } else {
            final List<Artist> starredArtists;
            if (mCollection.getId().equals(TomahawkApp.PLUGINNAME_USERCOLLECTION)) {
                starredArtists = DatabaseHelper.get().getStarredArtists();
            } else {
                starredArtists = null;
            }
            mCollection.getArtists(getSortMode())
                    .done(new DoneCallback<CollectionCursor<Artist>>() {
                        @Override
                        public void onDone(final CollectionCursor<Artist> cursor) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    if (starredArtists != null) {
                                        cursor.mergeItems(getSortMode(), starredArtists);
                                    }
                                    fillAdapter(new Segment(
                                            getDropdownPos(COLLECTION_ARTISTS_SPINNER_POSITION),
                                            constructDropdownItems(),
                                            constructDropdownListener(
                                                    COLLECTION_ARTISTS_SPINNER_POSITION),
                                            cursor, R.integer.grid_column_count,
                                            R.dimen.padding_superlarge,
                                            R.dimen.padding_superlarge));
                                }
                            }).start();
                        }
                    });
        }
    }

    private List<Integer> constructDropdownItems() {
        List<Integer> dropDownItems = new ArrayList<>();
        dropDownItems.add(R.string.collection_dropdown_recently_added);
        dropDownItems.add(R.string.collection_dropdown_alpha);
        return dropDownItems;
    }

    private int getSortMode() {
        switch (getDropdownPos(COLLECTION_ARTISTS_SPINNER_POSITION)) {
            case 0:
                return org.tomahawk.libtomahawk.collection.Collection.SORT_LAST_MODIFIED;
            case 1:
                return org.tomahawk.libtomahawk.collection.Collection.SORT_ALPHA;
            default:
                return org.tomahawk.libtomahawk.collection.Collection.SORT_NOT;
        }
    }
}
