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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.libtomahawk.infosystem.InfoRequestData;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class UserPagerFragment extends PagerFragment {

    public static final String USERPAGER_SELECTOR_POSITION
            = "org.tomahawk.tomahawk_android.userpager_selector_position";

    private User mUser;

    private View.OnClickListener mFollowButtonListener = new View.OnClickListener() {

        @Override
        public void onClick(View v) {
            HatchetAuthenticatorUtils authUtils =
                    (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                            .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            if (authUtils.getLoggedInUser().getFollowings().containsKey(mUser)) {
                String relationshipId =
                        authUtils.getLoggedInUser().getFollowings().get(mUser);
                mCurrentRequestIds.add(InfoSystem.getInstance()
                        .deleteRelationship(authUtils, relationshipId));
                setShowFakeNotFollowing(true);
                setShowFakeFollowing(false);
            } else {
                mCurrentRequestIds.add(InfoSystem.getInstance()
                        .sendRelationshipPostStruct(authUtils, mUser));
                setShowFakeNotFollowing(false);
                setShowFakeFollowing(true);
            }
            showContentHeader(mUser, R.dimen.header_clear_space_nonscrollable_static_user,
                    mFollowButtonListener);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mStaticHeaderHeight = getResources()
                .getDimensionPixelSize(R.dimen.header_clear_space_nonscrollable_static_user);
    }

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
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_USER_ID) && !TextUtils
                    .isEmpty(getArguments().getString(TomahawkFragment.TOMAHAWK_USER_ID))) {
                mUser = User
                        .getUserById(getArguments().getString(TomahawkFragment.TOMAHAWK_USER_ID));
                if (mUser == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                } else {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolve(mUser));
                    HatchetAuthenticatorUtils hatchetAuthUtils =
                            (HatchetAuthenticatorUtils) AuthenticatorManager.getInstance()
                                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolveFollowings(
                            hatchetAuthUtils.getLoggedInUser()));
                }
            }
        }

        showContentHeader(mUser, R.dimen.header_clear_space_nonscrollable_static_user,
                mFollowButtonListener);

        List<FragmentInfoList> fragmentInfoLists = new ArrayList<FragmentInfoList>();
        FragmentInfoList fragmentInfoList = new FragmentInfoList();
        FragmentInfo fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = SocialActionsFragment.class;
        fragmentInfo.mTitle = getString(R.string.activity);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentInfo.mBundle
                .putInt(TomahawkFragment.SHOW_MODE, SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
        fragmentInfo.mIconResId = R.drawable.ic_action_activity;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        fragmentInfoList = new FragmentInfoList();
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = CollectionFragment.class;
        fragmentInfo.mTitle = getString(R.string.drawer_title_collection);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putBoolean(ContentHeaderFragment.DONT_SHOW_HEADER, true);
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_collection;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = PlaylistsFragment.class;
        fragmentInfo.mTitle = getString(R.string.drawer_title_playlists);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putBoolean(ContentHeaderFragment.DONT_SHOW_HEADER, true);
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_playlist;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = PlaylistEntriesFragment.class;
        fragmentInfo.mTitle = getString(R.string.history);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putBoolean(ContentHeaderFragment.DONT_SHOW_HEADER, true);
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY,
                mUser.getPlaybackLog().getCacheKey());
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_history;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = PlaylistEntriesFragment.class;
        fragmentInfo.mTitle = getString(R.string.drawer_title_lovedtracks);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putBoolean(ContentHeaderFragment.DONT_SHOW_HEADER, true);
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY,
                mUser.getFavorites().getCacheKey());
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_favorites;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        SharedPreferences preferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        fragmentInfoList.setCurrent(preferences.getInt(USERPAGER_SELECTOR_POSITION, 0));
        fragmentInfoLists.add(fragmentInfoList);

        fragmentInfoList = new FragmentInfoList();
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = UsersFragment.class;
        fragmentInfo.mTitle = getString(R.string.followers);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putInt(TomahawkFragment.SHOW_MODE,
                UsersFragment.SHOW_MODE_TYPE_FOLLOWERS);
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_friend;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfo = new FragmentInfo();
        fragmentInfo.mClass = UsersFragment.class;
        fragmentInfo.mTitle = getString(R.string.following);
        fragmentInfo.mBundle = new Bundle();
        fragmentInfo.mBundle.putInt(TomahawkFragment.SHOW_MODE,
                UsersFragment.SHOW_MODE_TYPE_FOLLOWINGS);
        fragmentInfo.mBundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentInfo.mIconResId = R.drawable.ic_action_friend;
        fragmentInfoList.addFragmentInfo(fragmentInfo);
        fragmentInfoLists.add(fragmentInfoList);

        setupPager(fragmentInfoLists, initialPage, USERPAGER_SELECTOR_POSITION);
    }

    @Override
    protected void onPipeLineResultsReported(String key) {
    }

    @Override
    protected void onInfoSystemResultsReported(String requestId) {
        showContentHeader(mUser, R.dimen.header_clear_space_nonscrollable_static_user,
                mFollowButtonListener);

        InfoRequestData infoRequestData = InfoSystem.getInstance().getSentLoggedOpById(requestId);
        if (infoRequestData != null && infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS
                && (infoRequestData.getHttpType() == InfoRequestData.HTTPTYPE_DELETE
                || infoRequestData.getHttpType() == InfoRequestData.HTTPTYPE_POST)) {
            HatchetAuthenticatorUtils authUtils
                    = (HatchetAuthenticatorUtils) AuthenticatorManager
                    .getInstance().getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
            mCurrentRequestIds.add(
                    InfoSystem.getInstance().resolveFollowings(authUtils.getLoggedInUser()));
        }
        infoRequestData = InfoSystem.getInstance().getInfoRequestById(requestId);
        if (infoRequestData != null && infoRequestData.getType()
                == InfoRequestData.INFOREQUESTDATA_TYPE_RELATIONSHIPS_USERS_FOLLOWINGS) {
            setShowFakeFollowing(false);
            setShowFakeNotFollowing(false);
        }
    }
}
