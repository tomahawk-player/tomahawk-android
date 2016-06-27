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
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionCursor;
import org.tomahawk.libtomahawk.collection.ScriptResolverCollection;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.IdGenerator;

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
            = "org.tomahawk.tomahawk_android.collection_artists_spinner_position_";

    @Override
    public void onResume() {
        super.onResume();

        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *  @param view the clicked view
     * @param item the Object which corresponds to the click
     * @param segment
     */
    @Override
    public void onItemClick(View view, final Object item, Segment segment) {
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
                            IdGenerator.getSessionUniqueId());
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
            Segment.Builder builder = new Segment.Builder(mArtistArray);
            if (mContainerFragmentClass != null
                    && mContainerFragmentClass.equals(ChartsPagerFragment.class.getName())) {
                builder.showAsGrid(R.integer.grid_column_count,
                        R.dimen.padding_superlarge,
                        R.dimen.padding_superlarge)
                        .showNumeration(true, 1);
            }
            Segment segment = builder.build();
            fillAdapter(segment);
        } else {
            mCollection.getArtists(getSortMode())
                    .done(new DoneCallback<CollectionCursor<Artist>>() {
                        @Override
                        public void onDone(final CollectionCursor<Artist> cursor) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String id = mCollection.getId();
                                    Segment segment = new Segment.Builder(cursor)
                                            .headerLayout(R.layout.dropdown_header)
                                            .headerStrings(constructDropdownItems())
                                            .spinner(constructDropdownListener(
                                                    COLLECTION_ARTISTS_SPINNER_POSITION + id),
                                                    getDropdownPos(
                                                            COLLECTION_ARTISTS_SPINNER_POSITION
                                                                    + id))
                                            .showAsGrid(R.integer.grid_column_count,
                                                    R.dimen.padding_superlarge,
                                                    R.dimen.padding_superlarge)
                                            .build();
                                    fillAdapter(segment);
                                }
                            }).start();
                        }
                    });
        }
    }

    private List<Integer> constructDropdownItems() {
        List<Integer> dropDownItems = new ArrayList<>();
        if (!(mCollection instanceof ScriptResolverCollection)) {
            dropDownItems.add(R.string.collection_dropdown_recently_added);
        }
        dropDownItems.add(R.string.collection_dropdown_alpha);
        return dropDownItems;
    }

    private int getSortMode() {
        String id = mCollection.getId();
        int pos = getDropdownPos(COLLECTION_ARTISTS_SPINNER_POSITION + id);
        if (!(mCollection instanceof ScriptResolverCollection)) {
            switch (pos) {
                case 0:
                    return Collection.SORT_LAST_MODIFIED;
                case 1:
                    return Collection.SORT_ALPHA;
                default:
                    return Collection.SORT_NOT;
            }
        } else {
            switch (pos) {
                case 0:
                    return Collection.SORT_ALPHA;
                default:
                    return Collection.SORT_NOT;
            }
        }
    }
}
