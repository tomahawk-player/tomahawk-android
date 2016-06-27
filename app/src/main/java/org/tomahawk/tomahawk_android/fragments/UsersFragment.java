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

import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.os.Bundle;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows a set of {@link
 * org.tomahawk.libtomahawk.collection.Artist}s inside its {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
 */
public class UsersFragment extends TomahawkFragment {

    public static final int SHOW_MODE_TYPE_FOLLOWINGS = 0;

    public static final int SHOW_MODE_TYPE_FOLLOWERS = 1;

    @Override
    public void onResume() {
        super.onResume();

        if (mShowMode == SHOW_MODE_TYPE_FOLLOWERS) {
            String requestId = InfoSystem.get().resolveFollowers(mUser);
            if (requestId != null) {
                mCorrespondingRequestIds.add(requestId);
            }
        } else {
            String requestId = InfoSystem.get().resolveFollowings(mUser);
            if (requestId != null) {
                mCorrespondingRequestIds.add(requestId);
            }
        }
        if (mContainerFragmentClass == null) {
            getActivity().setTitle("");
        }
        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *  @param view the clicked view
     * @param item the Object which corresponds to the click
     * @param segment
     */
    @Override
    public void onItemClick(View view, Object item, Segment segment) {
        if (item instanceof User) {
            Bundle bundle = new Bundle();
            bundle.putInt(TomahawkFragment.SHOW_MODE,
                    SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
            bundle.putString(TomahawkFragment.USER, ((User) item).getId());
            bundle.putInt(CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC_USER);
            FragmentUtils.replace((TomahawkMainActivity) getActivity(), UserPagerFragment.class,
                    bundle);
        }
    }

    /**
     * Update this {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment}'s {@link
     * org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        List users = new ArrayList();
        if (mShowMode == SHOW_MODE_TYPE_FOLLOWERS) {
            if (mUser.getFollowers() != null) {
                users.addAll(mUser.getFollowers().keySet());
            }
        } else if (mUserArray != null) {
            users.addAll(mUserArray);
        } else if (mUser.getFollowings() != null) {
            users.addAll(mUser.getFollowings().keySet());
        }
        fillAdapter(new Segment.Builder(users).build());
    }
}
