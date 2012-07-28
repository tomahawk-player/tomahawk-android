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
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.playlist.ArtistPlaylist;
import org.tomahawk.libtomahawk.playlist.Playlist;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

/**
 * Fragment which represents the "Browse" tabview.
 */
public class BrowseFragment extends ListFragment implements OnItemClickListener {

	/* (non-Javadoc)
	 * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
	 */
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

        TomahawkApp app = (TomahawkApp) getActivity().getApplicationContext();
        Collection mycoll = app.getSourceList().getLocalSource().getCollection();
        ArrayAdapter<Artist> adapter = new ArrayAdapter<Artist>(getActivity(),
                R.layout.mymusic_list_item, R.id.mymusic_list_textview, mycoll.getArtists());
        setListAdapter(adapter);

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

        Playlist playlist = ArtistPlaylist.fromArtist(mycoll.getArtists().get(idx));
        playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, playlist);
        startActivity(playbackIntent);
    }
}