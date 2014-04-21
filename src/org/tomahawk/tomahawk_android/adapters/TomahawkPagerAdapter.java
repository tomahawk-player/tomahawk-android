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
package org.tomahawk.tomahawk_android.adapters;

import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;

import java.util.List;

public class TomahawkPagerAdapter extends FragmentPagerAdapter {

    private List<String> mFragmentClassNames;

    private List<String> mFragmentTitles;

    private List<Bundle> mFragmentBundles;

    public TomahawkPagerAdapter(FragmentManager fragmentManager, List<String> fragmentClassNames,
            List<String> fragmentTitles, List<Bundle> fragmentBundles) {
        super(fragmentManager);

        mFragmentClassNames = fragmentClassNames;
        mFragmentTitles = fragmentTitles;
        mFragmentBundles = fragmentBundles;
    }

    public TomahawkPagerAdapter(FragmentManager fragmentManager, List<String> fragmentClassNames,
            List<String> fragmentTitles) {
        super(fragmentManager);

        mFragmentClassNames = fragmentClassNames;
        mFragmentTitles = fragmentTitles;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle;
        if (mFragmentBundles != null && mFragmentBundles.get(position) != null) {
            bundle = mFragmentBundles.get(position);
        } else {
            bundle = new Bundle();
            bundle.putBoolean(TomahawkFragment.TOMAHAWK_LIST_ITEM_IS_LOCAL, true);
        }
        return Fragment.instantiate(TomahawkApp.getContext(),
                mFragmentClassNames.get(position), bundle);
    }

    @Override
    public int getCount() {
        return mFragmentClassNames.size();
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentTitles.get(position);
    }
}
