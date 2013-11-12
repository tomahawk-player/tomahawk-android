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

import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Artist}s inside its {@link
 * org.tomahawk.tomahawk_android.views.TomahawkStickyListHeadersListView}
 */
public class ArtistsFragment extends TomahawkFragment implements OnItemClickListener {

    /**
     * Called every time an item inside the {@link org.tomahawk.tomahawk_android.views.TomahawkStickyListHeadersListView}
     * is clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view
     *                 provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            if (getListAdapter().getItem(position) instanceof Artist) {
                Bundle bundle = new Bundle();
                bundle.putLong(TOMAHAWK_ARTIST_ID,
                        ((Artist) getListAdapter().getItem(position)).getId());
                if (mTomahawkMainActivity instanceof TomahawkMainActivity) {
                    mTomahawkMainActivity.getContentViewer()
                            .replace(mCorrespondingHubId, AlbumsFragment.class,
                                    ((Artist) getListAdapter().getItem(position)).getId(),
                                    TOMAHAWK_ARTIST_ID, false);
                }
            }
        }
    }

    /**
     * Called whenever the {@link org.tomahawk.libtomahawk.collection.UserCollection} {@link Loader}
     * has finished
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkBaseAdapter.TomahawkListItem> artists
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>(coll.getArtists());
        List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
        listArray.add(artists);
        setListAdapter(new TomahawkListAdapter(getActivity(), listArray));

        getListView().setOnItemClickListener(this);
        getListView().setAreHeadersSticky(false);
    }
}
