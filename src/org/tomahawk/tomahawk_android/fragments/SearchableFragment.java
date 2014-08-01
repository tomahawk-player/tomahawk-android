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
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.ArrayList;

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
        CheckBox onlineSourcesCheckBox = (CheckBox) getActivity()
                .findViewById(R.id.search_onlinesources_checkbox);
        onlineSourcesCheckBox.setOnCheckedChangeListener(this);

        // If we have restored a CurrentQueryString, start searching, so that we show the proper
        // results again
        if (mCurrentQueryString != null) {
            resolveFullTextQuery(mCurrentQueryString);
            getActivity().setTitle(mCurrentQueryString);
        }
        updateAdapter();
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
        Object item = getListAdapter().getItem(position);
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (item instanceof Query) {
            PlaybackService playbackService = activity.getPlaybackService();
            if (playbackService != null && shouldShowPlaystate() && mQueryPositions
                    .get(playbackService.getCurrentPlaylist().getCurrentQueryIndex())
                    == position) {
                playbackService.playPause();
            } else {
                Playlist playlist = Playlist.fromQueryList(
                        TomahawkMainActivity.getLifetimeUniqueStringId(), mCurrentQueryString,
                        mShownQueries,
                        mQueryPositions.keyAt(mQueryPositions.indexOfValue(position)));
                if (playbackService != null) {
                    playbackService.setCurrentPlaylist(playlist);
                    playbackService.start();
                }
            }
        } else if (item instanceof Album) {
            FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                    TracksFragment.class, ((Album) item).getCacheKey(),
                    TomahawkFragment.TOMAHAWK_ALBUM_KEY, mCollection);
        } else if (item instanceof Artist) {
            FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                    AlbumsFragment.class, ((Artist) item).getCacheKey(),
                    TomahawkFragment.TOMAHAWK_ARTIST_KEY, mCollection);
        } else if (item instanceof User) {
            FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                    SocialActionsFragment.class, ((User) item).getId(),
                    TomahawkFragment.TOMAHAWK_USER_ID,
                    SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
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
    public void getQueryResults(String queryKey) {
        Query query = Query.getQueryByKey(queryKey);
        mCurrentQueryString = query.getFullTextQuery();
        mShownQueries = query.getTrackQueries();
    }

    public void getInfoResults(String requestId) {
        InfoRequestData data = InfoSystem.getInstance().getInfoRequestById(requestId);
        mShownAlbums.clear();
        mShownArtists.clear();
        mShownUsers.clear();
        for (Object object : data.getResultListMap().get(Album.class)) {
            mShownAlbums.add((Album) object);
        }
        for (Object object : data.getResultListMap().get(Artist.class)) {
            mShownArtists.add((Artist) object);
        }
        for (Object object : data.getResultListMap().get(User.class)) {
            mShownUsers.add((User) object);
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

        Context context = getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        ArrayList<TomahawkListItem> listItems = new ArrayList<TomahawkListItem>();
        if (!mShownArtists.isEmpty()) {
            listItems.addAll(mShownArtists);
        }
        if (!mShownAlbums.isEmpty()) {
            listItems.addAll(mShownAlbums);
        }
        if (!mShownUsers.isEmpty()) {
            listItems.addAll(mShownUsers);
        }
        if (!mShownQueries.isEmpty()) {
            int precedingItemCount = mShownAlbums.size() + mShownArtists.size()
                    + mShownUsers.size();
            if (getListAdapter() != null
                    && ((TomahawkListAdapter) getListAdapter()).isShowingContentHeader()) {
                precedingItemCount++;
            }
            mQueryPositions.clear();
            for (int i = 0; i < mShownQueries.size(); i++) {
                mQueryPositions.put(i, i + precedingItemCount);
            }
            listItems.addAll(mShownQueries);
        }
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(context,
                    layoutInflater, listItems);
            tomahawkListAdapter.setShowCategoryHeaders(true);
            tomahawkListAdapter.setShowResolvedBy(true);
            setListAdapter(tomahawkListAdapter);
        } else {
            ((TomahawkListAdapter) getListAdapter()).setListItems(listItems);
        }
        getListView().setOnItemClickListener(this);

        updateShowPlaystate();
    }

    /**
     * Invoke the resolving process with the given fullTextQuery {@link String}
     */
    public void resolveFullTextQuery(String fullTextQuery) {
        ((TomahawkMainActivity) getActivity()).closeDrawer();
        mCurrentQueryString = fullTextQuery;
        CheckBox onlineSourcesCheckBox = (CheckBox) getActivity()
                .findViewById(R.id.search_onlinesources_checkbox);
        String queryId = PipeLine.getInstance().resolve(fullTextQuery,
                !onlineSourcesCheckBox.isChecked());
        if (onlineSourcesCheckBox.isChecked()) {
            mCurrentRequestIds.clear();
            String requestId = InfoSystem.getInstance().resolve(fullTextQuery);
            mCurrentRequestIds.add(requestId);
        } else {
            mShownArtists.clear();
            mShownAlbums.clear();
            mShownUsers.clear();
            updateAdapter();
        }
        if (queryId != null) {
            mCorrespondingQueryIds.clear();
            mCorrespondingQueryIds.add(queryId);
        }
    }

    @Override
    protected void onPipeLineResultsReported(ArrayList<String> queryKeys) {
        boolean needsUpdate = false;
        for (String key : queryKeys) {
            if (mCorrespondingQueryIds.contains(key)) {
                getQueryResults(key);
                needsUpdate = true;
            }
        }
        if (needsUpdate) {
            updateAdapter();
        }
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        if (mCurrentRequestIds.contains(requestId)) {
            getInfoResults(requestId);
            updateAdapter();
        }
    }
}
