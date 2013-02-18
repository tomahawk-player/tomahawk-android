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

import org.tomahawk.libtomahawk.*;
import org.tomahawk.libtomahawk.audio.PlaybackActivity;
import org.tomahawk.libtomahawk.playlist.CustomPlaylist;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Fragment which represents the "Tracks" tabview.
 */
public class TracksFragment extends TomahawkFragment implements OnItemClickListener {

    private Album mAlbum;
    private Artist mArtist;
    private CustomPlaylist mCustomPlaylist;

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);
        if (mActivity.getCollection() != null && getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_ALBUM_ID) && getArguments().getLong(TOMAHAWK_ALBUM_ID) > 0)
                mAlbum = mActivity.getCollection().getAlbumById(getArguments().getLong(TOMAHAWK_ALBUM_ID));
            else if (getArguments().containsKey(TOMAHAWK_PLAYLIST_ID)
                    && getArguments().getLong(TOMAHAWK_PLAYLIST_ID) > 0)
                mCustomPlaylist = null;//mActivity.getCollection().getPlaylistById(getArguments().getLong(TOMAHAWK_PLAYLIST_ID));+
            else if (getArguments().containsKey(SearchableFragment.SEARCHABLEFRAGMENT_ARTISTCACHED))
                mArtist = mActivity.getCollection().getCachedArtist();
            else if (getArguments().containsKey(SearchableFragment.SEARCHABLEFRAGMENT_ALBUMCACHED))
                mAlbum = mActivity.getCollection().getCachedAlbum();
        }

    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        if (getListAdapter().getItem(idx) instanceof Track) {
            ArrayList<Track> tracks = new ArrayList<Track>();
            if (mAlbum != null)
                tracks = mAlbum.getTracks();
            else if (mArtist != null)
                tracks = mArtist.getTracks();
            else
                tracks.addAll(mActivity.getCollection().getTracks());
            long playlistId = mActivity.getCollection().addPlaylist(
                    CustomPlaylist.fromTrackList("Last used playlist", tracks, (Track) getListAdapter().getItem(idx)));
            Bundle bundle = new Bundle();
            bundle.putLong(PlaybackActivity.PLAYLIST_PLAYLIST_ID, playlistId);
            bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID, ((Track) getListAdapter().getItem(idx)).getId());

            Intent playbackIntent = getIntent(mActivity, PlaybackActivity.class);
            playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
            startActivity(playbackIntent);
        }
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);

        List<TomahawkBaseAdapter.TomahawkListItem> tracks = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        if (mAlbum != null) {
            tracks.addAll(mAlbum.getTracks());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(tracks);
            tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
            tomahawkListAdapter.setShowCategoryHeaders(true);
            tomahawkListAdapter.setShowContentHeader(true, mList, mAlbum);
        } else if (mArtist != null) {
            tracks.addAll(mArtist.getTracks());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(tracks);
            tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
            tomahawkListAdapter.setShowResolvedBy(true);
            tomahawkListAdapter.setShowCategoryHeaders(true);
            tomahawkListAdapter.setShowContentHeader(true, mList, mArtist);
        } else {
            tracks.addAll(coll.getTracks());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(tracks);
            tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
        }

        setListAdapter(tomahawkListAdapter);
        getListView().setOnItemClickListener(this);
    }

    public Album getAlbum() {
        return mAlbum;
    }

    /**
     * Return the {@link Intent} defined by the given parameters
     *
     * @param context the context with which the intent will be created
     * @param cls the class which contains the activity to launch
     * @return the created intent
     */
    private static Intent getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }
}
