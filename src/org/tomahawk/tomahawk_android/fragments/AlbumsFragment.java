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
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.CollectionUtils;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.ScriptResolverCollection;
import org.tomahawk.libtomahawk.collection.TomahawkListItemComparator;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Album}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class AlbumsFragment extends TomahawkFragment {

    public static final String COLLECTION_ALBUMS_SPINNER_POSITION
            = "org.tomahawk.tomahawk_android.collection_albums_spinner_position";

    public static final int SHOW_MODE_STARREDALBUMS = 1;

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(SHOW_MODE)) {
                mShowMode = getArguments().getInt(SHOW_MODE);
            }
        }
        if (mContainerFragmentClass == null) {
            getActivity().setTitle("");
        }
        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the Object which corresponds to the click
     */
    @Override
    public void onItemClick(View view, Object item) {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (item instanceof Query) {
            Query query = ((Query) item);
            if (query.isPlayable()) {
                PlaybackService playbackService = activity.getPlaybackService();
                if (playbackService != null
                        && playbackService.getCurrentQuery() == query) {
                    playbackService.playPause();
                } else {
                    Playlist playlist = Playlist.fromQueryList(
                            TomahawkMainActivity.getLifetimeUniqueStringId(), mShownQueries);
                    if (playbackService != null) {
                        playbackService.setPlaylist(playlist, playlist.getEntryWithQuery(query));
                        Class clss = mContainerFragmentClass != null ? mContainerFragmentClass
                                : ((Object) this).getClass();
                        playbackService.setReturnFragment(clss, getArguments());
                        playbackService.start();
                    }
                }
            }
        } else if (item instanceof Album) {
            Bundle bundle = new Bundle();
            bundle.putString(TomahawkFragment.ALBUM, ((Album) item).getCacheKey());
            if (mCollection != null
                    && (mCollection instanceof ScriptResolverCollection
                    || mCollection.getAlbumTracks((Album) item, false).size() > 0)) {
                bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
            } else {
                bundle.putString(TomahawkFragment.COLLECTION_ID, TomahawkApp.PLUGINNAME_HATCHET);
            }
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_DYNAMIC);
            FragmentUtils
                    .replace((TomahawkMainActivity) getActivity(), TracksFragment.class, bundle);
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        List<Segment> segments = new ArrayList<>();
        ArrayList items = new ArrayList<>();
        if (mArtist != null) {
            if (mCollection != null
                    && !TomahawkApp.PLUGINNAME_HATCHET.equals(mCollection.getId())) {
                items.addAll(mCollection.getArtistAlbums(mArtist, true));
                Segment segment = new Segment(
                        mCollection.getName() + " " + getString(R.string.albums),
                        items, R.integer.grid_column_count, R.dimen.padding_superlarge,
                        R.dimen.padding_superlarge);
                segments.add(segment);
            } else {
                items.addAll(CollectionUtils.getArtistAlbums(mArtist, null));
                Segment segment = new Segment(R.string.top_albums, items,
                        R.integer.grid_column_count, R.dimen.padding_superlarge,
                        R.dimen.padding_superlarge);
                segments.add(segment);
                ArrayList<Query> topHits = CollectionUtils.getArtistTopHits(mArtist);
                items = new ArrayList<>();
                items.addAll(topHits);
                segment = new Segment(R.string.top_hits, items);
                segment.setShowNumeration(true, 1);
                segment.setHideArtistName(true);
                segment.setShowDuration(true);
                segments.add(segment);
                mShownQueries = topHits;
            }
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, getListView(), this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments, getListView());
            }
        } else if (mShowMode == SHOW_MODE_STARREDALBUMS) {
            ArrayList<Album> albums = DatabaseHelper.getInstance().getStarredAlbums();
            for (Album album : albums) {
                String requestId = InfoSystem.getInstance().resolve(album);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }
            items.addAll(albums);
            segments.add(new Segment(items));
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, getListView(), this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments, getListView());
            }
        } else if (mAlbumArray != null) {
            items.addAll(mAlbumArray);
            segments.add(new Segment(items));
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, getListView(), this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments, getListView());
            }
        } else {
            items.addAll(CollectionManager.getInstance()
                    .getCollection(TomahawkApp.PLUGINNAME_USERCOLLECTION).getAlbums());
            for (Album album : DatabaseHelper.getInstance().getStarredAlbums()) {
                if (!items.contains(album)) {
                    items.add(album);
                }
            }
            SharedPreferences preferences =
                    PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
            List<Integer> dropDownItems = new ArrayList<>();
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
                    int initialPos = preferences.getInt(COLLECTION_ALBUMS_SPINNER_POSITION, 0);
                    if (initialPos != position) {
                        preferences.edit().putInt(COLLECTION_ALBUMS_SPINNER_POSITION, position)
                                .commit();
                        updateAdapter();
                    }
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                }
            };
            int initialPos = preferences.getInt(COLLECTION_ALBUMS_SPINNER_POSITION, 0);
            if (initialPos == 0) {
                Collection userColl = CollectionManager.getInstance().getCollection(
                        TomahawkApp.PLUGINNAME_USERCOLLECTION);
                Collections.sort(items, new TomahawkListItemComparator(
                        TomahawkListItemComparator.COMPARE_RECENTLY_ADDED,
                        userColl.getAlbumAddedTimeStamps()));
            } else if (initialPos == 1) {
                Collections.sort(items, new TomahawkListItemComparator(
                        TomahawkListItemComparator.COMPARE_ALPHA));
            } else if (initialPos == 2) {
                Collections.sort(items, new TomahawkListItemComparator(
                        TomahawkListItemComparator.COMPARE_ARTIST_ALPHA));
            }
            segments.add(new Segment(initialPos, dropDownItems, spinnerClickListener, items,
                    R.integer.grid_column_count, R.dimen.padding_superlarge,
                    R.dimen.padding_superlarge));
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, getListView(), this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments, getListView());
            }
        }

        onUpdateAdapterFinished();
    }
}
