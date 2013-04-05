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

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.TomahawkGridAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which represents the "Album" tabview.
 */
public class AlbumsFragment extends TomahawkFragment implements OnItemClickListener {

    private Artist mArtist;

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        if (mActivity.getCollection() != null && getArguments() != null && getArguments()
                .containsKey(TOMAHAWK_ARTIST_ID)
                && getArguments().getLong(TOMAHAWK_ARTIST_ID) > 0) {
            mArtist = mActivity.getCollection()
                    .getArtistById(getArguments().getLong(TOMAHAWK_ARTIST_ID));
        }
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        idx -= mList.getHeaderViewsCount();
        if (idx >= 0) {
            if (getListAdapter().getItem(idx) instanceof Album) {
                Bundle bundle = new Bundle();
                bundle.putLong(TOMAHAWK_ALBUM_ID, ((Album) getListAdapter().getItem(idx)).getId());
                if (mActivity instanceof CollectionActivity) {
                    mActivity.getTabsAdapter()
                            .replace(TomahawkTabsActivity.TAB_ID_MYMUSIC, TracksFragment.class,
                                    ((Album) getListAdapter().getItem(idx)).getId(),
                                    TOMAHAWK_ALBUM_ID, false);
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkBaseAdapter.TomahawkListItem> albums
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        if (mArtist != null) {
            albums.addAll(mArtist.getAlbums());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(albums);
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
            tomahawkListAdapter.setShowCategoryHeaders(true);
            tomahawkListAdapter.setShowContentHeader(true, mList, mArtist);
            setListAdapter(tomahawkListAdapter);
            getListView().setOnItemClickListener(this);
        } else {
            albums.addAll(coll.getAlbums());
            setListAdapter(new TomahawkGridAdapter(getActivity(), R.layout.album_art_grid_item,
                    R.id.album_art_grid_image, R.id.album_art_grid_textView1,
                    R.id.album_art_grid_textView2, albums));
            getGridView().setOnItemClickListener(this);
            adaptColumnCount();
        }
    }

    public Artist getArtist() {
        return mArtist;
    }

}
