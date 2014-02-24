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
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * {@link TomahawkFragment} which offers both local and non-local search functionality to the user.
 */
public class SearchableFragment extends TomahawkFragment
        implements OnItemClickListener, CompoundButton.OnCheckedChangeListener {

    public static final String SEARCHABLEFRAGMENT_QUERY_STRING
            = "org.tomahawk.tomahawk_android.SEARCHABLEFRAGMENT_QUERY_ID";

    private String mCurrentQueryString;

    /**
     * Restore the {@link String} inside the search {@link TextView}. Either through the
     * savedInstanceState {@link Bundle} or through the a {@link Bundle} provided in the Arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState
                .containsKey(SEARCHABLEFRAGMENT_QUERY_STRING)
                && savedInstanceState.getString(SEARCHABLEFRAGMENT_QUERY_STRING) != null) {
            mCurrentQueryString = savedInstanceState.getString(SEARCHABLEFRAGMENT_QUERY_STRING);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getArguments() != null && getArguments().containsKey(SEARCHABLEFRAGMENT_QUERY_STRING)
                && getArguments().getString(SEARCHABLEFRAGMENT_QUERY_STRING) != null) {
            mCurrentQueryString = getArguments().getString(SEARCHABLEFRAGMENT_QUERY_STRING);
        }

        // Initialize our onlineSourcesCheckBox
        CheckBox onlineSourcesCheckBox = (CheckBox) mTomahawkMainActivity
                .findViewById(R.id.search_onlinesources_checkbox);
        onlineSourcesCheckBox.setOnCheckedChangeListener(this);

        // If we have restored a CurrentQueryString, start searching, so that we show the proper
        // results again
        if (mCurrentQueryString != null) {
            resolveFullTextQuery(mCurrentQueryString);
            mTomahawkMainActivity.setTitle(mCurrentQueryString);
        }
    }

    /**
     * Save the {@link String} inside the search {@link TextView}.
     */
    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString(SEARCHABLEFRAGMENT_QUERY_STRING, mCurrentQueryString);
        super.onSaveInstanceState(out);
    }

    /**
     * Called every time an item inside the {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
     * is clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view
     *                 provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            Object item = getListAdapter().getItem(position);
            if (item instanceof Query) {
                PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                if (playbackService != null && shouldShowPlaystate()
                        && playbackService.getCurrentPlaylist().getCurrentQueryIndex()
                        == position - mShownAlbums.size() - mShownArtists.size()) {
                    playbackService.playPause();
                } else {
                    UserPlaylist playlist = UserPlaylist.fromQueryList(
                            TomahawkApp.getLifetimeUniqueStringId(), mCurrentQueryString,
                            mShownQueries, ((Query) item));
                    if (playbackService != null) {
                        playbackService.setCurrentPlaylist(playlist);
                        playbackService.start();
                    }
                }
            } else if (item instanceof Album) {
                Bundle bundle = new Bundle();
                String key = TomahawkUtils.getCacheKey((Album) item);
                bundle.putString(TOMAHAWK_ALBUM_KEY, key);
                mTomahawkApp.getContentViewer()
                        .replace(TracksFragment.class, key, TOMAHAWK_ALBUM_KEY, false, false);
            } else if (item instanceof Artist) {
                Bundle bundle = new Bundle();
                String key = TomahawkUtils.getCacheKey((Artist) item);
                bundle.putString(TOMAHAWK_ARTIST_KEY, key);
                mTomahawkApp.getContentViewer()
                        .replace(AlbumsFragment.class, key, TOMAHAWK_ARTIST_KEY, false, false);
            }
        }
    }

    /**
     * Called, when the checkbox' state has been changed,
     */
    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        resolveFullTextQuery(mCurrentQueryString);
    }

    /**
     * Display all {@link org.tomahawk.libtomahawk.resolver.Result}s of the {@link
     * org.tomahawk.libtomahawk.resolver.Query} with the given key
     */
    public void showQueryResults(String queryKey) {
        Query query = mPipeline.getQuery(queryKey);
        mCurrentQueryString = query.getFullTextQuery();
        mShownQueries = query.getTrackQueries();
    }

    public void showInfoResults(String requestId) {
        Map<String, List> convertedResultMap = mInfoSystem.getInfoRequestById(requestId)
                .getConvertedResultMap();
        if (convertedResultMap != null) {
            mShownArtists = (ArrayList<Artist>) convertedResultMap
                    .get(HatchetInfoPlugin.HATCHET_ARTISTS);
            mShownAlbums = (ArrayList<Album>) convertedResultMap
                    .get(HatchetInfoPlugin.HATCHET_ALBUMS);
            updateAdapter();
        }
    }

    protected void updateAdapter() {
        List<List<TomahawkListItem>> listArray
                = new ArrayList<List<TomahawkListItem>>();
        if (mShownArtists != null) {
            ArrayList<TomahawkListItem> artistResultList
                    = new ArrayList<TomahawkListItem>();
            artistResultList.addAll(mShownArtists);
            listArray.add(artistResultList);
        }
        if (mShownAlbums != null) {
            ArrayList<TomahawkListItem> albumResultList
                    = new ArrayList<TomahawkListItem>();
            albumResultList.addAll(mShownAlbums);
            listArray.add(albumResultList);
        }
        if (mShownQueries != null) {
            ArrayList<TomahawkListItem> trackResultList
                    = new ArrayList<TomahawkListItem>();
            trackResultList.addAll(mShownQueries);
            listArray.add(trackResultList);
        }
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity,
                    listArray);
            tomahawkListAdapter.setShowCategoryHeaders(true, false);
            tomahawkListAdapter.setShowResolvedBy(true);
            setListAdapter(tomahawkListAdapter);
        } else {
            ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
        }
        getListView().setOnItemClickListener(this);
        updateShowPlaystate();
    }

    /**
     * Invoke the resolving process with the given fullTextQuery {@link String}
     */
    public void resolveFullTextQuery(String fullTextQuery) {
        mCurrentQueryString = fullTextQuery;
        CheckBox onlineSourcesCheckBox = (CheckBox) mTomahawkMainActivity
                .findViewById(R.id.search_onlinesources_checkbox);
        String queryId = mPipeline.resolve(fullTextQuery, !onlineSourcesCheckBox.isChecked());
        mCorrespondingQueryIds.clear();
        if (onlineSourcesCheckBox.isChecked()) {
            mCurrentRequestIds.clear();
            String requestId = mInfoSystem.resolve(fullTextQuery);
            mCurrentRequestIds.add(requestId);
        }
        if (queryId != null) {
            mCorrespondingQueryIds.add(queryId);
            mTomahawkMainActivity.startLoadingAnimation();
        }
    }

    @Override
    protected void onPipeLineResultsReported(ArrayList<String> queryKeys) {
        for (String key : queryKeys) {
            if (mCorrespondingQueryIds.contains(key)) {
                showQueryResults(key);
            }
        }
        updateAdapter();
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        if (mCurrentRequestIds.contains(requestId)) {
            showInfoResults(requestId);
        }
    }
}
