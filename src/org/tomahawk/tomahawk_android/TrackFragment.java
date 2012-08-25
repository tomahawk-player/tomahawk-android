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

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.TomahawkListArrayAdapter;
import org.tomahawk.libtomahawk.TomahawkListArrayAdapter.TomahawkListItem;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.playlist.AlbumPlaylist;
import org.tomahawk.libtomahawk.playlist.CollectionPlaylist;
import org.tomahawk.libtomahawk.playlist.Playlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Fragment which represents the "Tracks" tabview.
 */
public class TrackFragment extends TomahawkListFragment implements OnItemClickListener {

    TomahawkListArrayAdapter mTomahawkListArrayAdapter;

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setFastScrollEnabled(true);
        getListView().setOnItemClickListener(this);
        TextView textView = (TextView) getActivity().findViewById(R.id.fragmentLayout_backbutton_textView);
        textView.setText(getString(R.string.tracksfragment_title_string));
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        Playlist playlist;
        if (mFilterConstraint != null && mFilterConstraint.length() > 0)
            playlist = AlbumPlaylist.fromAlbum(mTomahawkListArrayAdapter.getItem(idx).getAlbum(), (Track) mTomahawkListArrayAdapter.getItem(idx));
        else
            playlist = CollectionPlaylist.fromCollection(getCurrentCollection(), (Track) mTomahawkListArrayAdapter.getItem(idx));

        Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);
        playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, playlist);
        startActivity(playbackIntent);
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#getAdapter()
     */
    @Override
    protected ArrayAdapter<?> getAdapter() {
        return mTomahawkListArrayAdapter;
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkListItem> items = new ArrayList<TomahawkListItem>(coll.getTracks());
        mTomahawkListArrayAdapter = new TomahawkListArrayAdapter(getActivity(), R.layout.double_line_list_item, R.id.double_line_list_textview, R.id.double_line_list_textview2, items, TomahawkListArrayAdapter.FILTER_BY_ALBUM);
        setListAdapter(mTomahawkListArrayAdapter);
        getAdapter().getFilter().filter(mFilterConstraint);
    }
}
