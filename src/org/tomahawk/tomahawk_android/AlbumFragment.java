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
import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.playlist.AlbumPlaylist;
import org.tomahawk.libtomahawk.playlist.Playlist;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

/**
 * Fragment which represents the "MyMusic" tabview.
 */
public class AlbumFragment extends TomahawkListFragment implements OnItemClickListener {

    private ArrayAdapter<Album> mAlbumAdapter;

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /* (non-Javadoc)
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        TomahawkApp app = (TomahawkApp) getActivity().getApplicationContext();
        Collection mycoll = app.getSourceList().getLocalSource().getCollection();
        mAlbumAdapter = new ArrayAdapter<Album>(getActivity(), R.layout.mymusic_list_item,
                R.id.mymusic_list_textview, mycoll.getAlbums());
        setListAdapter(mAlbumAdapter);

        getListView().setOnItemClickListener(this);
	}

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {

        TomahawkApp app = (TomahawkApp) getActivity().getApplicationContext();
        Collection mycoll = app.getSourceList().getLocalSource().getCollection();
        Intent playbackIntent = new Intent(getActivity(), PlaybackActivity.class);

        Playlist playlist = AlbumPlaylist.fromAlbum(mycoll.getAlbums().get(idx));
        playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, playlist);
        startActivity(playbackIntent);
    }

    @Override
    public void onCollectionUpdated() {
        mAlbumAdapter.notifyDataSetChanged();
    }

    @Override
    protected ArrayAdapter<?> getAdapter() {
        return mAlbumAdapter;
    }
}
