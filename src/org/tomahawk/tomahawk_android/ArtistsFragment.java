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
package org.tomahawk.tomahawk_android;

import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which represents the "Artist" tabview.
 */
public class ArtistsFragment extends TomahawkFragment
        implements OnItemClickListener, OnItemLongClickListener {

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        idx -= mList.getHeaderViewsCount();
        if (idx >= 0) {
            if (getListAdapter().getItem(idx) instanceof Artist) {
                Bundle bundle = new Bundle();
                bundle.putLong(TOMAHAWK_ARTIST_ID,
                        ((Artist) getListAdapter().getItem(idx)).getId());
                if (mActivity instanceof CollectionActivity) {
                    ((CollectionActivity) mActivity).getTabsAdapter()
                            .replace(CollectionActivity.LOCAL_COLLECTION_TAB_POSITION,
                                    AlbumsFragment.class,
                                    ((Artist) getListAdapter().getItem(idx)).getId(),
                                    TOMAHAWK_ARTIST_ID, false);
                }
            }
        }
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.internal.widget.IcsAdapterView.OnItemLongClickListener#onItemLongClick(com.actionbarsherlock.internal.widget.IcsAdapterView, android.view.View, int, long)
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        position -= mList.getHeaderViewsCount();
        if (position >= 0) {
            Bundle bundle = new Bundle();
            bundle.putLong(PlaybackActivity.PLAYLIST_ARTIST_ID,
                    ((Artist) getListAdapter().getItem(position)).getId());

            Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);
            playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
            startActivity(playbackIntent);
        }
        return true;
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
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
        getListView().setOnItemLongClickListener(this);
    }
}
