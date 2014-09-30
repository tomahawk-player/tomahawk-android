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
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkFragment} which shows a set of {@link Album}s inside its {@link
 * se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class AlbumsFragment extends TomahawkFragment {

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
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(View view, TomahawkListItem item) {
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
            FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                    TracksFragment.class, item.getCacheKey(),
                    TomahawkFragment.TOMAHAWK_ALBUM_KEY, mCollection);
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
        View rootView = getView();
        List<Segment> segments = new ArrayList<Segment>();
        if (mArtist != null) {
            if (mCollection != null) {
                ArrayList<TomahawkListItem> items = new ArrayList<TomahawkListItem>();
                items.addAll(mCollection.getArtistAlbums(mArtist, true));
                segments.add(new Segment(items));
            } else {
                ArrayList<TomahawkListItem> items = new ArrayList<TomahawkListItem>();
                items.addAll(AdapterUtils.getArtistAlbums(mArtist, null));
                segments.add(new Segment(R.string.segmentheader_topalbums, items,
                        R.integer.grid_column_count, R.dimen.padding_superlarge,
                        R.dimen.padding_superlarge));
                ArrayList<Query> topHits = AdapterUtils.getArtistTopHits(mArtist);
                items = new ArrayList<TomahawkListItem>();
                items.addAll(topHits);
                segments.add(new Segment(R.string.segmentheader_tophits, items));
                mShownQueries = topHits;
            }
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, this);
                tomahawkListAdapter.setShowResolvedBy(true);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments);
            }
        } else if (mShowMode == SHOW_MODE_STARREDALBUMS) {
            ArrayList<Album> albums = DatabaseHelper.getInstance().getStarredAlbums();
            for (Album album : albums) {
                mCurrentRequestIds.add(InfoSystem.getInstance().resolve(album));
            }
            ArrayList<TomahawkListItem> items = new ArrayList<TomahawkListItem>();
            items.addAll(albums);
            segments.add(new Segment(items));
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments);
            }
        } else if (mSearchAlbums != null) {
            ArrayList<TomahawkListItem> items = new ArrayList<TomahawkListItem>();
            items.addAll(mSearchAlbums);
            segments.add(new Segment(items));
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments);
            }
        } else {
            ArrayList<TomahawkListItem> items = new ArrayList<TomahawkListItem>();
            items.addAll(mCollection.getAlbums());
            segments.add(new Segment(items, R.integer.grid_column_count, R.dimen.padding_superlarge,
                    R.dimen.padding_superlarge));
            if (getListAdapter() == null) {
                TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                        layoutInflater, segments, this);
                setListAdapter(tomahawkListAdapter);
            } else {
                getListAdapter().setSegments(segments);
            }
        }

        updateShowPlaystate();
    }
}
