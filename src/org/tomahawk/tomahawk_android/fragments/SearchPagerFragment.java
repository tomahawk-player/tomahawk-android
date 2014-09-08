/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class SearchPagerFragment extends PagerFragment {

    public static final String SEARCHABLEFRAGMENT_QUERY_STRING
            = "org.tomahawk.tomahawk_android.SEARCHABLEFRAGMENT_QUERY_ID";

    private String mCurrentQueryString;

    private ArrayList<String> mAlbumIds = new ArrayList<String>();

    private ArrayList<String> mArtistIds = new ArrayList<String>();

    private ArrayList<String> mSongIds = new ArrayList<String>();

    private ArrayList<String> mUserIds = new ArrayList<String>();

    private Image mContentHeaderImage;

    private SearchFragmentReceiver mSearchFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class SearchFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                boolean noConnectivity =
                        intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, false);
                if (!noConnectivity) {
                    resolveFullTextQuery(mCurrentQueryString);
                }
            }
        }
    }

    /**
     * Restore the {@link String} inside the search {@link android.widget.TextView}. Either through
     * the savedInstanceState {@link Bundle} or through the a {@link Bundle} provided in the
     * Arguments.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState
                .containsKey(SEARCHABLEFRAGMENT_QUERY_STRING)
                && savedInstanceState.getString(SEARCHABLEFRAGMENT_QUERY_STRING) != null) {
            mCurrentQueryString = savedInstanceState.getString(SEARCHABLEFRAGMENT_QUERY_STRING);
        }
        mStaticHeaderHeight = getResources()
                .getDimensionPixelSize(R.dimen.header_clear_space_nonscrollable_static);
    }


    /**
     * Called, when this {@link SearchPagerFragment}'s {@link
     * android.view.View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int initialPage = -1;
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.CONTAINER_FRAGMENT_PAGE)) {
                initialPage = getArguments().getInt(TomahawkFragment.CONTAINER_FRAGMENT_PAGE);
            }
            if (getArguments().containsKey(SEARCHABLEFRAGMENT_QUERY_STRING)
                    && getArguments().getString(SEARCHABLEFRAGMENT_QUERY_STRING) != null) {
                mCurrentQueryString = getArguments().getString(SEARCHABLEFRAGMENT_QUERY_STRING);
            }
        }

        // If we have restored a CurrentQueryString, start searching, so that we show the proper
        // results again
        if (mCurrentQueryString != null) {
            resolveFullTextQuery(mCurrentQueryString);
            getActivity().setTitle(mCurrentQueryString);
        }

        showContentHeader(mContentHeaderImage, null,
                R.dimen.header_clear_space_nonscrollable_static);

        updatePager(initialPage);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize and register Receiver
        if (mSearchFragmentReceiver == null) {
            mSearchFragmentReceiver = new SearchFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
            getActivity().registerReceiver(mSearchFragmentReceiver, intentFilter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSearchFragmentReceiver != null) {
            getActivity().unregisterReceiver(mSearchFragmentReceiver);
            mSearchFragmentReceiver = null;
        }
    }

    /**
     * Save the {@link String} inside the search {@link android.widget.TextView}.
     */
    @Override
    public void onSaveInstanceState(Bundle out) {
        out.putString(SEARCHABLEFRAGMENT_QUERY_STRING, mCurrentQueryString);
        super.onSaveInstanceState(out);
    }

    private void updatePager(int initialPage) {
        List<String> fragmentClassNames = new ArrayList<String>();
        fragmentClassNames.add(ArtistsFragment.class.getName());
        fragmentClassNames.add(AlbumsFragment.class.getName());
        fragmentClassNames.add(TracksFragment.class.getName());
        fragmentClassNames.add(UsersFragment.class.getName());

        List<String> fragmentTitles = new ArrayList<String>();
        fragmentTitles.add(getString(R.string.artists));
        fragmentTitles.add(getString(R.string.albums));
        fragmentTitles.add(getString(R.string.songs));
        fragmentTitles.add(getString(R.string.users));

        List<Bundle> fragmentBundles = new ArrayList<Bundle>();
        Bundle bundle = new Bundle();
        if (mArtistIds != null) {
            bundle.putStringArrayList(TomahawkFragment.TOMAHAWK_ARTISTARRAY_KEY, mArtistIds);
        }
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        if (mAlbumIds != null) {
            bundle.putStringArrayList(TomahawkFragment.TOMAHAWK_ALBUMARRAY_KEY, mAlbumIds);
        }
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        if (mSongIds != null) {
            bundle.putStringArrayList(TomahawkFragment.TOMAHAWK_QUERYARRAY_KEY, mSongIds);
        }
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        if (mUserIds != null) {
            bundle.putStringArrayList(TomahawkFragment.TOMAHAWK_USERARRAY_ID, mUserIds);
        }
        fragmentBundles.add(bundle);

        setupPager(fragmentClassNames, fragmentTitles, fragmentBundles, initialPage);
    }

    /**
     * Invoke the resolving process with the given fullTextQuery {@link String}
     */
    public void resolveFullTextQuery(String fullTextQuery) {
        ((TomahawkMainActivity) getActivity()).closeDrawer();
        mSongIds.clear();
        mAlbumIds.clear();
        mArtistIds.clear();
        mUserIds.clear();
        mCurrentQueryString = fullTextQuery;
        mCurrentRequestIds.clear();
        mCurrentRequestIds.add(InfoSystem.getInstance().resolve(fullTextQuery));
        String queryId = PipeLine.getInstance().resolve(fullTextQuery, false);
        if (queryId != null) {
            mCorrespondingQueryIds.clear();
            mCorrespondingQueryIds.add(queryId);
        }
    }

    @Override
    protected void onPipeLineResultsReported(String key) {
        Query query = Query.getQueryByKey(key);
        if (query != null) {
            for (Query q : query.getTrackQueries()) {
                mSongIds.add(q.getCacheKey());
            }
        }
        updatePager(0);
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        InfoRequestData data = InfoSystem.getInstance().getInfoRequestById(requestId);
        for (Artist artist : data.getResultList(Artist.class)) {
            if (mContentHeaderImage == null && artist.getImage() != null) {
                mContentHeaderImage = artist.getImage();
                showContentHeader(mContentHeaderImage, null,
                        R.dimen.header_clear_space_nonscrollable_static);
            }
            mArtistIds.add(artist.getCacheKey());
        }
        for (Album album : data.getResultList(Album.class)) {
            if (mContentHeaderImage == null && album.getImage() != null) {
                mContentHeaderImage = album.getImage();
                showContentHeader(mContentHeaderImage, null,
                        R.dimen.header_clear_space_nonscrollable_static);
            }
            mAlbumIds.add(album.getCacheKey());
        }
        for (User user : data.getResultList(User.class)) {
            if (mContentHeaderImage == null && user.getImage() != null) {
                mContentHeaderImage = user.getImage();
                showContentHeader(mContentHeaderImage, null,
                        R.dimen.header_clear_space_nonscrollable_static);
            }
            mUserIds.add(user.getCacheKey());
        }
        updatePager(0);
    }

    @Override
    public void onPanelCollapsed() {
    }

    @Override
    public void onPanelExpanded() {
    }
}
