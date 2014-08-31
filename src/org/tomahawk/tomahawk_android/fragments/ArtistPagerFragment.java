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

import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ArtistPagerFragment extends PagerFragment {

    private Artist mArtist;

    private Collection mCollection;

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    private ArtistPagerFragmentReceiver mArtistPagerFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class ArtistPagerFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                String requestId = intent.getStringExtra(
                        InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                onInfoSystemResultsReported(requestId);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.ArtistPagerFragment}'s
     * {@link android.view.View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int initialPage = -1;
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.CONTAINER_FRAGMENT_PAGE)) {
                initialPage = getArguments().getInt(TomahawkFragment.CONTAINER_FRAGMENT_PAGE);
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ARTIST_KEY) && !TextUtils
                    .isEmpty(getArguments().getString(TomahawkFragment.TOMAHAWK_ARTIST_KEY))) {
                mArtist = Artist.getArtistByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ARTIST_KEY));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                } else {
                    ArrayList<String> requestIds = InfoSystem.getInstance().resolve(mArtist, false);
                    for (String requestId : requestIds) {
                        mCurrentRequestIds.add(requestId);
                    }
                }
            }
            if (getArguments().containsKey(CollectionManager.COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(CollectionManager.COLLECTION_ID));
            }
        }

        // Initialize and register Receiver
        if (mArtistPagerFragmentReceiver == null) {
            mArtistPagerFragmentReceiver = new ArtistPagerFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            activity.registerReceiver(mArtistPagerFragmentReceiver, intentFilter);
        }

        showContentHeader(mArtist, mCollection);

        List<String> fragmentClassNames = new ArrayList<String>();
        fragmentClassNames.add(AlbumsFragment.class.getName());
        fragmentClassNames.add(AlbumsFragment.class.getName());
        fragmentClassNames.add(AlbumsFragment.class.getName());
        List<String> fragmentTitles = new ArrayList<String>();
        fragmentTitles.add(getString(R.string.music));
        fragmentTitles.add(getString(R.string.biography));
        fragmentTitles.add(getString(R.string.similar));
        List<Bundle> fragmentBundles = new ArrayList<Bundle>();
        Bundle bundle = new Bundle();
        bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, mArtist.getCacheKey());
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, mArtist.getCacheKey());
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, mArtist.getCacheKey());
        fragmentBundles.add(bundle);
        setupPager(fragmentClassNames, fragmentTitles, fragmentBundles, initialPage);
    }

    protected void onInfoSystemResultsReported(String requestId) {
        if (mCurrentRequestIds.contains(requestId)) {
            showContentHeader(mArtist, mCollection);
        }
    }

    @Override
    public void onPanelCollapsed() {
    }

    @Override
    public void onPanelExpanded() {
    }
}
