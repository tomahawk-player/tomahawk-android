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

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.audio.PlaylistDialog;
import org.tomahawk.libtomahawk.playlist.CustomPlaylist;
import org.tomahawk.libtomahawk.playlist.Playlist;

import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which represents the "Playlist" tabview.
 */
public class PlaylistsFragment extends TomahawkFragment implements OnItemClickListener {

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        idx -= mList.getHeaderViewsCount();
        if (idx >= 0) {
            if (getListAdapter().getItem(idx) instanceof Playlist) {
                Bundle bundle = new Bundle();
                bundle.putLong(TOMAHAWK_PLAYLIST_ID,
                        ((CustomPlaylist) getListAdapter().getItem(idx)).getId());
                if (mActivity instanceof CollectionActivity) {
                    mActivity.getContentViewer()
                            .replace(mCorrespondingStackId, TracksFragment.class,
                                    ((CustomPlaylist) getListAdapter().getItem(idx)).getId(),
                                    TOMAHAWK_PLAYLIST_ID, false);
                }
            } else {
                new PlaylistDialog().show(getFragmentManager(),
                        getString(R.string.playbackactivity_create_playlist_dialog_title));
            }
        }
    }

    /* 
     * (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkBaseAdapter.TomahawkListItem> playlists
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        playlists.addAll(coll.getCustomPlaylists());
        List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
        listArray.add(playlists);
        TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(getActivity(), listArray);
        tomahawkListAdapter.setShowAddButton(true, getListView(),
                getResources().getString(R.string.playbackactivity_create_playlist_dialog_title));
        setListAdapter(tomahawkListAdapter);

        getListView().setOnItemClickListener(this);
        getListView().setAreHeadersSticky(false);
    }
}
