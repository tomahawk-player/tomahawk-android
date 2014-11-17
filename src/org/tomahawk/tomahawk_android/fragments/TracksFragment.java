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

import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.CollectionUtils;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.TomahawkListItemComparator;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.FancyDropDown;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Track}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class TracksFragment extends TomahawkFragment {

    public static final String COLLECTION_TRACKS_SPINNER_POSITION
            = "org.tomahawk.tomahawk_android.collection_tracks_spinner_position";

    private TracksFragmentReceiver mTracksFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class TracksFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (CollectionManager.COLLECTION_UPDATED.equals(intent.getAction())) {
                if (intent.getStringExtra(TOMAHAWK_ALBUM_KEY) != null) {
                    if (mAlbum != null
                            && intent.getStringExtra(TomahawkFragment.TOMAHAWK_ALBUM_KEY).equals(
                            mAlbum.getCacheKey())) {
                        showAlbumFancyDropDown();
                    }
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize and register Receiver
        if (mTracksFragmentReceiver == null) {
            mTracksFragmentReceiver = new TracksFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(CollectionManager.COLLECTION_UPDATED);
            getActivity().registerReceiver(mTracksFragmentReceiver, intentFilter);
        }

        if (mContainerFragmentClass == null) {
            getActivity().setTitle("");
        }
        updateAdapter();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mTracksFragmentReceiver != null) {
            getActivity().unregisterReceiver(mTracksFragmentReceiver);
            mTracksFragmentReceiver = null;
        }
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(View view, TomahawkListItem item) {
        if (item instanceof Query) {
            Query query = (Query) item;
            if (query.isPlayable()) {
                ArrayList<Query> queries = new ArrayList<Query>();
                TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
                if (mAlbum != null) {
                    queries = CollectionUtils.getAlbumTracks(mAlbum, mCollection);
                } else if (mArtist != null) {
                    queries = CollectionUtils.getArtistTracks(mArtist, mCollection);
                } else if (mQuery != null) {
                    queries.add(mQuery);
                } else if (mSearchSongs != null) {
                    queries = mSearchSongs;
                } else {
                    Collection userCollection = CollectionManager.getInstance()
                            .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION);
                    queries.addAll(userCollection.getQueries());
                }
                PlaybackService playbackService = activity.getPlaybackService();
                if (playbackService != null && playbackService.getCurrentQuery() == query) {
                    playbackService.playPause();
                } else {
                    Playlist playlist = Playlist.fromQueryList(DatabaseHelper.CACHED_PLAYLIST_NAME,
                            queries);
                    playlist.setId(DatabaseHelper.CACHED_PLAYLIST_ID);
                    if (playbackService != null) {
                        playbackService.setPlaylist(playlist, playlist.getEntryWithQuery(query));
                        Class clss = mContainerFragmentClass != null ? mContainerFragmentClass
                                : ((Object) this).getClass();
                        playbackService.setReturnFragment(clss, getArguments());
                        playbackService.start();
                    }
                }
            }
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter}
     * content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        mResolveQueriesHandler.removeCallbacksAndMessages(null);
        mResolveQueriesHandler.sendEmptyMessage(RESOLVE_QUERIES_REPORTER_MSG);
        ArrayList<TomahawkListItem> queries = new ArrayList<TomahawkListItem>();
        TomahawkListAdapter tomahawkListAdapter;
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        if (mAlbum != null) {
            queries.addAll(CollectionUtils.getAlbumTracks(mAlbum, mCollection));
            Segment segment;
            if (mCollection != null) {
                segment = new Segment(mCollection.getName() + " " + getString(R.string.tracks),
                        queries);
            } else {
                segment = new Segment(R.string.album_details, queries);
            }
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater, segment,
                        this);
                if (CollectionUtils.allFromOneArtist(queries)) {
                    tomahawkListAdapter.setHideArtistName(true);
                    tomahawkListAdapter.setShowDuration(true);
                }
                tomahawkListAdapter.setShowNumeration(true);
                tomahawkListAdapter.setShowContentHeaderSpacerResId(
                        R.dimen.header_clear_space_scrollable, getListView());
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segment, getListView());
            }
            showContentHeader(mAlbum, R.dimen.header_clear_space_nonscrollable);
            showAlbumFancyDropDown();
        } else if (mQuery != null) {
            queries.add(mQuery);
            Segment segment = new Segment(queries);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater, segment,
                        this);
                tomahawkListAdapter.setShowDuration(true);
                tomahawkListAdapter.setShowContentHeaderSpacerResId(
                        R.dimen.header_clear_space_scrollable, getListView());
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segment, getListView());
            }
            showContentHeader(mQuery, R.dimen.header_clear_space_nonscrollable);
            showFancyDropDown(mQuery);
        } else if (mSearchSongs != null) {
            queries.addAll(mSearchSongs);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter((TomahawkMainActivity) getActivity(),
                        layoutInflater, new Segment(queries), this);
                tomahawkListAdapter.setShowDuration(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(new Segment(queries), getListView());
            }
        } else {
            queries.addAll(CollectionManager.getInstance()
                    .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION).getQueries());
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
            List<Integer> dropDownItems = new ArrayList<Integer>();
            dropDownItems.add(R.string.collection_dropdown_recently_added);
            dropDownItems.add(R.string.collection_dropdown_alpha);
            dropDownItems.add(R.string.collection_dropdown_alpha_artists);
            AdapterView.OnItemSelectedListener spinnerClickListener
                    = new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position,
                        long id) {
                    SharedPreferences preferences =
                            PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
                    int initialPos = preferences.getInt(COLLECTION_TRACKS_SPINNER_POSITION, 0);
                    if (initialPos != position) {
                        preferences.edit().putInt(COLLECTION_TRACKS_SPINNER_POSITION, position)
                                .commit();
                        updateAdapter();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };
            int initialPos = preferences.getInt(COLLECTION_TRACKS_SPINNER_POSITION, 0);
            if (initialPos == 0) {
                Collection userColl = CollectionManager.getInstance().getCollection(
                        TomahawkApp.PLUGINNAME_USERCOLLECTION);
                Collections.sort(queries, new TomahawkListItemComparator(
                        TomahawkListItemComparator.COMPARE_RECENTLY_ADDED,
                        userColl.getAddedTimeStamps()));
            } else if (initialPos == 1) {
                Collections.sort(queries, new TomahawkListItemComparator(
                        TomahawkListItemComparator.COMPARE_ALPHA));
            } else if (initialPos == 2) {
                Collections.sort(queries, new TomahawkListItemComparator(
                        TomahawkListItemComparator.COMPARE_ARTIST_ALPHA));
            }
            List<Segment> segments = new ArrayList<Segment>();
            segments.add(new Segment(initialPos, dropDownItems, spinnerClickListener, queries));
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater, segments,
                        this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments, getListView());
            }
        }

        mShownQueries.clear();
        for (TomahawkListItem query : queries) {
            mShownQueries.add((Query) query);
        }

        updateShowPlaystate();
        forceAutoResolve();
    }

    private void showAlbumFancyDropDown() {
        if (mAlbum != null) {
            final List<Collection> collections =
                    CollectionManager.getInstance().getAvailableCollections(mAlbum);
            int initialSelection = 0;
            for (int i = 0; i < collections.size(); i++) {
                if (collections.get(i) == mCollection) {
                    initialSelection = i;
                    break;
                }
            }
            showFancyDropDown(mAlbum, initialSelection,
                    FancyDropDown.convertToDropDownItemInfo(collections),
                    new FancyDropDown.DropDownListener() {
                        @Override
                        public void onDropDownItemSelected(int position) {
                            mCollection = collections.get(position);
                            updateAdapter();
                        }

                        @Override
                        public void onCancel() {
                        }
                    });
        }
    }
}
