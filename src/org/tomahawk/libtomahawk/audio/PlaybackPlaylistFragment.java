/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.audio;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.TomahawkListAdapter;
import org.tomahawk.libtomahawk.Track;
import org.tomahawk.libtomahawk.playlist.Playlist;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkFragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Fragment which represents the current Playlist played
 */
public class PlaybackPlaylistFragment extends TomahawkFragment implements OnItemClickListener {

    private PlaybackService mPlaybackService;
    private Playlist mPlaylist;
    TomahawkListAdapter mTomahawkListAdapter;

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        setBreadCrumbNavigationEnabled(false);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        if (getListAdapter().getItem(idx) instanceof Track) {
            if (mPlaylist.getPosition() == idx)
                mPlaybackService.playPause();
            else {
                try {
                    mPlaybackService.setCurrentTrack(mPlaylist.getTrackAtPos(idx));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.playbackplaylistfragment_layout, null, false);
    }

    /*
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (mPlaybackService != null && mPlaylist != null) {
            List<TomahawkBaseAdapter.TomahawkListItem> tracks = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
            tracks.addAll(mPlaylist.getTracks());
            mTomahawkListAdapter = new TomahawkListAdapter(getActivity(),
                    R.layout.double_line_list_item_with_playstate_image, R.id.double_line_list_imageview,
                    R.id.double_line_list_textview, R.id.double_line_list_textview2, tracks);
            mTomahawkListAdapter.setShowHighlightingAndPlaystate(true);
            mTomahawkListAdapter.setHighlightedItem(mPlaylist.getPosition());
            mTomahawkListAdapter.setHighlightedItemIsPlaying(mPlaybackService.isPlaying());
            setListAdapter(mTomahawkListAdapter);
            getListView().setOnItemClickListener(this);
        }
    }

    /**
     * Called when the track has Changed inside our PlaybackService
     */
    public void onTrackChanged() {
        if (mPlaylist != null) {
            mTomahawkListAdapter.setHighlightedItem(mPlaylist.getPosition());
            mTomahawkListAdapter.setHighlightedItemIsPlaying(mPlaybackService.isPlaying());
            mTomahawkListAdapter.notifyDataSetInvalidated();
        }
    }

    /**
     * Called when the playState (playing or paused) has Changed inside our PlaybackService
     */
    public void onPlaystateChanged() {
        if (mPlaylist != null) {
            mTomahawkListAdapter.setHighlightedItem(mPlaylist.getPosition());
            mTomahawkListAdapter.setHighlightedItemIsPlaying(mPlaybackService.isPlaying());
            mTomahawkListAdapter.notifyDataSetInvalidated();
        }
    }

    /**
     * Called when the playlist has Changed inside our PlaybackService
     */
    public void onPlaylistChanged() {
        if (mPlaylist != null) {
            List<TomahawkBaseAdapter.TomahawkListItem> tracks = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
            tracks.addAll(mPlaylist.getTracks());
            mTomahawkListAdapter = new TomahawkListAdapter(getActivity(),
                    R.layout.double_line_list_item_with_playstate_image, R.id.double_line_list_imageview,
                    R.id.double_line_list_textview, R.id.double_line_list_textview2, tracks);
            mTomahawkListAdapter.setShowHighlightingAndPlaystate(true);
            mTomahawkListAdapter.setHighlightedItem(mPlaylist.getPosition());
            mTomahawkListAdapter.setHighlightedItemIsPlaying(mPlaybackService.isPlaying());
            setListAdapter(mTomahawkListAdapter);
            getListView().setOnItemClickListener(this);
        }
    }

    public void setPlaybackService(PlaybackService playbackService) {
        if (playbackService != null && playbackService != mPlaybackService) {
            mPlaybackService = playbackService;
            mPlaylist = mPlaybackService.getCurrentPlaylist();
            onPlaylistChanged();
        }
    }
}
