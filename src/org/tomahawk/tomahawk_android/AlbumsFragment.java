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

import java.util.ArrayList;
import java.util.List;

import org.tomahawk.libtomahawk.Album;
import org.tomahawk.libtomahawk.Artist;
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter.TomahawkListItem;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;

/**
 * Fragment which represents the "Album" tabview.
 */
public class AlbumsFragment extends TomahawkFragment implements OnItemClickListener, OnItemLongClickListener {

    private Artist mArtist;

    public AlbumsFragment() {
        mArtist = null;
    }

    public AlbumsFragment(Artist artist) {
        mArtist = artist;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        setShowAsGrid(true);
        getGridView().setOnItemClickListener(this);
        getGridView().setOnItemLongClickListener(this);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        mCollectionActivity.getTabsAdapter().replace(new TracksFragment((Album) getListAdapter().getItem(idx)), false);
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.internal.widget.IcsAdapterView.OnItemLongClickListener#onItemLongClick(com.actionbarsherlock.internal.widget.IcsAdapterView, android.view.View, int, long)
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        Bundle bundle = new Bundle();
        bundle.putLong(PlaybackActivity.PLAYLIST_ALBUM_ID, ((Album) getListAdapter().getItem(position)).getId());
        bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID,
                ((Album) getListAdapter().getItem(position)).getTracks().get(0).getId());

        Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);
        playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
        startActivity(playbackIntent);
        return true;
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkListItem> albums = new ArrayList<TomahawkListItem>();
        if (mArtist != null)
            albums.addAll(mArtist.getAlbums());
        else
            albums.addAll(coll.getAlbums());

        setListAdapter(new TomahawkListAdapter(getActivity(), R.layout.album_art_grid_item, R.id.album_art_grid_image,
                R.id.album_art_grid_textView1, R.id.album_art_grid_textView2, albums));
    }

    public Artist getArtist() {
        return mArtist;
    }

}
