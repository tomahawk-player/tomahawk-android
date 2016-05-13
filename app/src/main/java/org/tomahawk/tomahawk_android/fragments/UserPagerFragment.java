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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class UserPagerFragment extends PagerFragment {

    private User mUser;

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.UserPagerFragment}'s {@link
     * android.view.View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        getActivity().setTitle("");

        int initialPage = -1;
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.CONTAINER_FRAGMENT_PAGE)) {
                initialPage = getArguments().getInt(TomahawkFragment.CONTAINER_FRAGMENT_PAGE);
            }
            if (getArguments().containsKey(TomahawkFragment.USER) && !TextUtils
                    .isEmpty(getArguments().getString(TomahawkFragment.USER))) {
                mUser = User.getUserById(getArguments().getString(TomahawkFragment.USER));
                if (mUser == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else if (mUser.getName() == null) {
                    String requestId = InfoSystem.get().resolve(mUser);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
        }
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                if (user != null && user.getFollowings() == null) {
                    String requestId = InfoSystem.get().resolveFollowings(user);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            }
        });

        mFollowButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final HatchetAuthenticatorUtils authUtils =
                        (HatchetAuthenticatorUtils) AuthenticatorManager.get()
                                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                User.getSelf().done(new DoneCallback<User>() {
                    @Override
                    public void onDone(User user) {
                        if (user.getFollowings() != null
                                && user.getFollowings().containsKey(mUser)) {
                            String relationshipId = user.getFollowings().get(mUser);
                            InfoSystem.get().deleteRelationship(authUtils, relationshipId);
                            mShowFakeNotFollowing = true;
                            mShowFakeFollowing = false;
                        } else {
                            InfoSystem.get().sendRelationshipPostStruct(authUtils, mUser);
                            mShowFakeNotFollowing = false;
                            mShowFakeFollowing = true;
                        }
                    }
                });
                showContentHeader(mUser);
            }
        };

        showContentHeader(mUser);

        List<FragmentInfoList> fragmentInfoLists = new ArrayList<>();
        FragmentInfoList fragmentInfoList = new FragmentInfoList();
        FragmentInfo fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = SocialActionsFragment.class;
        fragmentInfo.mTitle = getString(R.string.activity);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putString(TomahawkFragment.USER, mUser.getCacheKey());
        fragmentInfo.mBundle
                .putInt(TomahawkFragment.SHOW_MODE, SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
        fragmentInfo.mIconResId = R.drawable.ic_action_activity;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        fragmentInfoList = new FragmentInfoList();
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = AlbumsFragment.class;
        fragmentInfo.mTitle = getString(R.string.drawer_title_collection);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putString(TomahawkFragment.USER, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_collection;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = PlaylistsFragment.class;
        fragmentInfo.mTitle = getString(R.string.drawer_title_playlists);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putString(TomahawkFragment.USER, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_playlist;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = PlaylistEntriesFragment.class;
        fragmentInfo.mTitle = getString(R.string.history);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putInt(TomahawkFragment.SHOW_MODE,
                PlaylistEntriesFragment.SHOW_MODE_PLAYBACKLOG);
        fragmentInfo.mBundle.putString(TomahawkFragment.USER, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_history;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = PlaylistEntriesFragment.class;
        fragmentInfo.mTitle = getString(R.string.drawer_title_lovedtracks);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putInt(TomahawkFragment.SHOW_MODE,
                PlaylistEntriesFragment.SHOW_MODE_LOVEDITEMS);
        fragmentInfo.mBundle.putString(TomahawkFragment.USER, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_favorites;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoList.setCurrent(
                PreferenceUtils.getInt(PreferenceUtils.USERPAGER_SELECTOR_POSITION));
        fragmentInfoLists.add(fragmentInfoList);

        fragmentInfoList = new FragmentInfoList();
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = UsersFragment.class;
        fragmentInfo.mTitle = getString(R.string.followers);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putInt(TomahawkFragment.SHOW_MODE,
                UsersFragment.SHOW_MODE_TYPE_FOLLOWERS);
        fragmentInfo.mBundle.putString(TomahawkFragment.USER, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_friend;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = UsersFragment.class;
        fragmentInfo.mTitle = getString(R.string.following);
        fragmentInfo.mBundle = getChildFragmentBundle();
        fragmentInfo.mBundle.putInt(TomahawkFragment.SHOW_MODE,
                UsersFragment.SHOW_MODE_TYPE_FOLLOWINGS);
        fragmentInfo.mBundle.putString(TomahawkFragment.USER, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_friend;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        setupPager(fragmentInfoLists, initialPage, PreferenceUtils.USERPAGER_SELECTOR_POSITION, 1);
    }

    @Override
    protected void onInfoSystemResultsReported(InfoRequestData infoRequestData) {
        InfoRequestData sentLoggedOp = InfoSystem.get()
                .getSentLoggedOpById(infoRequestData.getRequestId());
        if (sentLoggedOp != null
                && sentLoggedOp.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS
                && (sentLoggedOp.getHttpType() == InfoRequestData.HTTPTYPE_DELETE
                || sentLoggedOp.getHttpType() == InfoRequestData.HTTPTYPE_POST)) {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    String requestId = InfoSystem.get().resolveFollowings(user);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
            });
        }
        if (mCorrespondingRequestIds.contains(infoRequestData.getRequestId())) {
            if (infoRequestData.getType() == InfoRequestData.INFOREQUESTDATA_TYPE_USERS_FOLLOWS) {
                mShowFakeFollowing = false;
                mShowFakeNotFollowing = false;
            }
        }
        showContentHeader(mUser);
    }
}
