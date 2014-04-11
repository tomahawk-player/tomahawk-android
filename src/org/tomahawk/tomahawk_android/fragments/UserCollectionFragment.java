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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkPagerAdapter;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link TomahawkListFragment} which shows a simple listview menu to the user, so that he can
 * choose between a {@link TracksFragment}, an {@link AlbumsFragment} and an {@link
 * ArtistsFragment}, which display the {@link org.tomahawk.libtomahawk.collection.UserCollection}'s
 * content to the user.
 */
public class UserCollectionFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pagerfragment_layout, container, false);
    }

    /**
     * Called, when this {@link UserCollectionFragment}'s {@link View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle(getString(R.string.usercollectionfragment_title_string));

        List<String> fragmentClassNames = new ArrayList<String>();
        fragmentClassNames.add(TracksFragment.class.getName());
        fragmentClassNames.add(ArtistsFragment.class.getName());
        fragmentClassNames.add(AlbumsFragment.class.getName());
        List<String> fragmentTitles = new ArrayList<String>();
        fragmentTitles.add(getString(R.string.tracksfragment_title_string));
        fragmentTitles.add(getString(R.string.artistsfragment_title_string));
        fragmentTitles.add(getString(R.string.albumsfragment_title_string));
        TomahawkPagerAdapter adapter = new TomahawkPagerAdapter(getChildFragmentManager(),
                fragmentClassNames, fragmentTitles);
        ViewPager fragmentPager = (ViewPager) getActivity().findViewById(R.id.fragmentpager);
        fragmentPager.setAdapter(adapter);
    }
}
