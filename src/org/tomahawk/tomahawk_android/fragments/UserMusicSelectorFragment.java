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

import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class UserMusicSelectorFragment extends SelectorFragment {

    private User mUser;

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.UserMusicSelectorFragment}'s
     * {@link android.view.View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        int initialPage = -1;
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.CONTAINER_FRAGMENT_PAGE)) {
                initialPage = getArguments().getInt(TomahawkFragment.CONTAINER_FRAGMENT_PAGE);
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_USER_ID) && !TextUtils
                    .isEmpty(getArguments().getString(TomahawkFragment.TOMAHAWK_USER_ID))) {
                mUser = User.get(getArguments().getString(TomahawkFragment.TOMAHAWK_USER_ID));
                if (mUser.getName() == null) {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolve(mUser));
                }
            }
        }

        List<FragmentInfo> fragmentInfos = new ArrayList<FragmentInfo>();
        FragmentInfo fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = UserCollectionFragment.class;
        fragmentInfo.mTitle = getString(R.string.drawer_title_collection);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putInt(ContentHeaderFragment.MODE,
                ContentHeaderFragment.MODE_HEADER_DYNAMIC);
        fragmentInfo.mIconResId = R.drawable.ic_action_collection;
        fragmentInfos.add(fragmentInfo);

        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = PlaylistEntriesFragment.class;
        fragmentInfo.mTitle = getString(R.string.drawer_title_playlists);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY,
                mUser.getPlaybackLog().getCacheKey());
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentInfo.mBundle.putInt(ContentHeaderFragment.MODE,
                ContentHeaderFragment.MODE_HEADER_DYNAMIC);
        fragmentInfo.mIconResId = R.drawable.ic_action_playlist;
        fragmentInfos.add(fragmentInfo);

        setupSelector(fragmentInfos, initialPage, null);
    }
}
