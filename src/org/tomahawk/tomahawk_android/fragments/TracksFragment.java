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
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CustomPlaylist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.hatchet.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.activities.PlaybackActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which represents the "Tracks" tabview.
 */
public class TracksFragment extends TomahawkFragment implements OnItemClickListener {

    boolean mShouldShowLoadingAnimation = false;

    private TrackFragmentReceiver mTrackFragmentReceiver;

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class TrackFragmentReceiver extends BroadcastReceiver {

        /*
         * (non-Javadoc)
         *
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(PipeLine.PIPELINE_RESULTSREPORTED_NON_FULLTEXTQUERY)) {
                String queryId = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QID);
                if (mCorrespondingQueryIds.containsKey(queryId)) {
                    ArrayList<Track> tracks = mPipeline.getQuery(queryId).getTrackResults();
                    if (tracks != null && tracks.size() > 0) {
                        Track track = mCorrespondingQueryIds.get(queryId);
                        if (track.getScore() < tracks.get(0).getScore()) {
                            Query.trackResultToTrack(tracks.get(0), track);
                            updateAdapter();
                        }
                    }
                }
            } else if (intent.getAction().equals(InfoSystem.INFOSYSTEM_RESULTSREPORTED)) {
                String requestId = intent
                        .getStringExtra(InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                if (mCurrentRequestIds.contains(requestId)) {
                }
            }
        }
    }

    @Override
    public void onCreate(Bundle inState) {
        super.onCreate(inState);

        if (mActivity.getCollection() != null && getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_ALBUM_ID)
                    && getArguments().getLong(TOMAHAWK_ALBUM_ID) >= 0) {
                mAlbum = mActivity.getCollection()
                        .getAlbumById(getArguments().getLong(TOMAHAWK_ALBUM_ID));
            } else if (getArguments().containsKey(TOMAHAWK_PLAYLIST_ID)
                    && getArguments().getLong(TOMAHAWK_PLAYLIST_ID) >= 0) {
                mCustomPlaylist = mActivity.getCollection()
                        .getCustomPlaylistById(getArguments().getLong(TOMAHAWK_PLAYLIST_ID));
            } else if (getArguments().containsKey(UserCollection.USERCOLLECTION_ALBUMCACHED)) {
                mAlbum = mActivity.getCollection().getCachedAlbum();
                resolveAlbum(mAlbum);
                mShouldShowLoadingAnimation = true;
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mTrackFragmentReceiver == null) {
            mTrackFragmentReceiver = new TrackFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);
            getActivity().registerReceiver(mTrackFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            getActivity().registerReceiver(mTrackFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED_NON_FULLTEXTQUERY);
            getActivity().registerReceiver(mTrackFragmentReceiver, intentFilter);
        }
        if (mShouldShowLoadingAnimation) {
            startLoadingAnimation();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mTrackFragmentReceiver != null) {
            getActivity().unregisterReceiver(mTrackFragmentReceiver);
            mTrackFragmentReceiver = null;
        }
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        idx -= getListView().getHeaderViewsCount();
        if (idx >= 0) {
            if (getListAdapter().getItem(idx) instanceof Track && ((Track) getListAdapter()
                    .getItem(idx)).isResolved()) {
                ArrayList<Track> tracks = new ArrayList<Track>();
                if (mAlbum != null) {
                    tracks = mAlbum.getTracks();
                } else if (mArtist != null) {
                    tracks = mArtist.getTracks();
                } else if (mCustomPlaylist != null) {
                    tracks = mCustomPlaylist.getTracks();
                } else {
                    tracks.addAll(mActivity.getCollection().getTracks());
                }
                CustomPlaylist playlist = CustomPlaylist.fromTrackList("Last used playlist", tracks,
                        (Track) getListAdapter().getItem(idx));
                playlist.setCurrentTrackIndex(idx);
                ((UserCollection) mActivity.getCollection()).setCachedPlaylist(playlist);
                Bundle bundle = new Bundle();
                bundle.putBoolean(UserCollection.USERCOLLECTION_PLAYLISTCACHED, true);
                bundle.putLong(PlaybackActivity.PLAYLIST_TRACK_ID,
                        ((Track) getListAdapter().getItem(idx)).getId());

                Intent playbackIntent = getIntent(mActivity, PlaybackActivity.class);
                playbackIntent.putExtra(PlaybackActivity.PLAYLIST_EXTRA, bundle);
                startActivity(playbackIntent);
            }
        }
    }

    /* (non-Javadoc)
     * @see org.tomahawk.tomahawk_android.TomahawkListFragment#onLoadFinished(android.support.v4.content.Loader, org.tomahawk.libtomahawk.Collection)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        super.onLoadFinished(loader, coll);
        updateAdapter();
    }

    public void updateAdapter() {
        List<TomahawkBaseAdapter.TomahawkListItem> items
                = new ArrayList<TomahawkBaseAdapter.TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        Collection coll = mActivity.getCollection();
        if (mAlbum != null) {
            items.addAll(mAlbum.getTracks());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(items);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mAlbum);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        } else if (mArtist != null) {
            items.addAll(mArtist.getTracks());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(items);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mArtist);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        } else if (mCustomPlaylist != null) {
            mCustomPlaylist = coll.getCustomPlaylistById(mCustomPlaylist.getId());
            items.addAll(mCustomPlaylist.getTracks());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(items);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                tomahawkListAdapter.setShowContentHeader(true, getListView(), mCustomPlaylist);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        } else {
            items.addAll(coll.getTracks());
            List<List<TomahawkBaseAdapter.TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkBaseAdapter.TomahawkListItem>>();
            listArray.add(items);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mActivity, listArray);
                getListView().setAreHeadersSticky(false);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
            }
        }

        getListView().setOnItemClickListener(this);
    }

    public Album getAlbum() {
        return mAlbum;
    }

    /**
     * Return the {@link Intent} defined by the given parameters
     *
     * @param context the context with which the intent will be created
     * @param cls     the class which contains the activity to launch
     * @return the created intent
     */
    private static Intent getIntent(Context context, Class<?> cls) {
        Intent intent = new Intent(context, cls);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        return intent;
    }

    private void resolveAlbum(Album album) {
        if (album != null && album.getTracks() != null) {
            for (Track track : album.getTracks()) {
                if (!track.isResolved()) {
                    String queryId = mPipeline.resolve(track.getName(), track.getAlbum().getName(),
                            track.getArtist().getName());
                    if (queryId != null) {
                        mCorrespondingQueryIds.put(queryId, track);
                        startLoadingAnimation();
                    }
                }
            }
        }
    }
}
