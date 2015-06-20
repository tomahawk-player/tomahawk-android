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
import org.tomahawk.tomahawk_android.utils.FragmentInfo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;

import java.util.List;

public class TomahawkPagerAdapter extends FragmentStatePagerAdapter {

    private final Class mContainerFragmentClass;

    private final long mContainerFragmentId;

    private List<FragmentInfo> mFragmentInfos;

    public TomahawkPagerAdapter(FragmentManager fragmentManager, List<FragmentInfo> fragmentInfos,
            Class containerFragmentClass, long containerFragmentId) {
        super(fragmentManager);

        mFragmentInfos = fragmentInfos;
        mContainerFragmentClass = containerFragmentClass;
        mContainerFragmentId = containerFragmentId;
    }

    @Override
    public Fragment getItem(int position) {
        Bundle bundle = mFragmentInfos.get(position).mBundle;
        bundle.putString(TomahawkFragment.CONTAINER_FRAGMENT_CLASSNAME,
                mContainerFragmentClass.getName());
        bundle.putInt(TomahawkFragment.CONTAINER_FRAGMENT_PAGE, position);
        bundle.putLong(TomahawkFragment.CONTAINER_FRAGMENT_ID, mContainerFragmentId);
        return Fragment.instantiate(TomahawkApp.getContext(),
                mFragmentInfos.get(position).mClass.getName(), bundle);
    }

    @Override
    public int getCount() {
        return mFragmentInfos.size();
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return mFragmentInfos.get(position).mTitle;
    }

    public void changeFragment(int position, FragmentInfo fragmentInfo) {
        mFragmentInfos.remove(position);
        mFragmentInfos.add(position, fragmentInfo);
        notifyDataSetChanged();
    }

    public void changeFragments(List<FragmentInfo> fragmentInfos) {
        mFragmentInfos = fragmentInfos;
        notifyDataSetChanged();
    }
}
