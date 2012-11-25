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
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter.TomahawkListItem;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * Fragment which represents the "Tracks" tabview.
 */
public class TracksFragment extends TomahawkFragment implements OnItemClickListener {

    private Album mAlbum;

    public TracksFragment() {
        mAlbum = null;
    }

    public TracksFragment(Album album) {
        mAlbum = album;
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(this);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {

        Bundle bundle = new Bundle();
        if (mAlbum != null)
            bundle.putLong(PlaybackActivity.PLAYLIST_ALBUM_ID, mAlbum.getId());
        else
            bundle.putInt(PlaybackActivity.PLAYLIST_COLLECTION_ID, getCurrentCollection().getId());

        bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID, ((Track) getListAdapter().getItem(idx)).getId());

        Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);
        playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
        startActivity(playbackIntent);
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkListItem> tracks = new ArrayList<TomahawkListItem>();
        if (mAlbum != null)
            tracks.addAll(mAlbum.getTracks());
        else
            tracks.addAll(coll.getTracks());

        setListAdapter(new TomahawkListAdapter(getActivity(), R.layout.double_line_list_item,
                R.id.double_line_list_textview, R.id.double_line_list_textview2, tracks));
    }

    public Album getAlbum() {
        return mAlbum;
    }
}
