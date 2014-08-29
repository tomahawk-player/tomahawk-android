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

import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.tomahawk_android.R;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class FavoritesFragment extends PagerFragment {

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.FavoritesFragment}'s {@link
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
        }

        List<String> fragmentClassNames = new ArrayList<String>();
        fragmentClassNames.add(PlaylistEntriesFragment.class.getName());
        fragmentClassNames.add(AlbumsFragment.class.getName());
        fragmentClassNames.add(ArtistsFragment.class.getName());
        List<String> fragmentTitles = new ArrayList<String>();
        fragmentTitles.add(getString(R.string.loved_tracks));
        fragmentTitles.add(getString(R.string.starred_albums));
        fragmentTitles.add(getString(R.string.starred_artists));
        List<Bundle> fragmentBundles = new ArrayList<Bundle>();
        Bundle bundle = new Bundle();
        bundle.putString(PlaylistsFragment.TOMAHAWK_PLAYLIST_KEY,
                DatabaseHelper.LOVEDITEMS_PLAYLIST_ID);
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        bundle.putInt(TomahawkFragment.SHOW_MODE, AlbumsFragment.SHOW_MODE_STARREDALBUMS);
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        bundle.putInt(TomahawkFragment.SHOW_MODE, ArtistsFragment.SHOW_MODE_STARREDARTISTS);
        fragmentBundles.add(bundle);
        setupPager(fragmentClassNames, fragmentTitles, fragmentBundles, initialPage);
    }

    @Override
    public void onPanelCollapsed() {
    }

    @Override
    public void onPanelExpanded() {
    }
}
