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
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.Segment;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.view.LayoutInflater;
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

        if (getArguments() != null) {
            if (getArguments().containsKey(SHOW_MODE)) {
                mShowMode = getArguments().getInt(SHOW_MODE);
                if (mShowMode == SHOW_MODE_TYPE_FOLLOWERS) {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolveFollowers(mUser));
                } else {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolveFollowings(mUser));
                }
            }
        }
        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(View view, TomahawkListItem item) {
        if (item instanceof User) {
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    getActivity().getSupportFragmentManager(), SocialActionsFragment.class,
                    ((User) item).getId(), TomahawkFragment.TOMAHAWK_USER_ID,
                    SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
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

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();

        List<TomahawkListItem> users = new ArrayList<TomahawkListItem>();
        if (mShowMode == SHOW_MODE_TYPE_FOLLOWERS) {
            activity.setTitle(mUser.getName() + " " + getString(R.string.users_followers_suffix));
            if (mUser.getFollowers() != null && mUser.getFollowers().size() > 0) {
                users.addAll(mUser.getFollowers().keySet());
            }
        } else {
            activity.setTitle(mUser.getName() + " " + getString(R.string.users_followings_suffix));
            if (mUser.getFollowings() != null && mUser.getFollowings().size() > 0) {
                users.addAll(mUser.getFollowings().keySet());
            }
        }
        Segment segment = new Segment(users);
        if (getListAdapter() == null) {
            TomahawkListAdapter tomahawkListAdapter = new TomahawkListAdapter(activity,
                    layoutInflater, segment, this);
            setListAdapter(tomahawkListAdapter);
        } else {
            ((TomahawkListAdapter) getListAdapter()).setSegments(segment);
        }
    }

    @Override
    public void onPanelCollapsed() {
        if (mShowMode == SHOW_MODE_TYPE_FOLLOWERS) {
            getActivity()
                    .setTitle(mUser.getName() + " " + getString(R.string.users_followers_suffix));
        } else {
            getActivity()
                    .setTitle(mUser.getName() + " " + getString(R.string.users_followings_suffix));
        }
    }

    @Override
    public void onPanelExpanded() {
    }
}
