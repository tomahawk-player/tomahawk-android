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

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class UserPagerFragment extends PagerFragment {

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
                } else {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolve(mUser));
                }
            }
        }

        showContentHeader(mUser, R.dimen.header_clear_space_nonscrollable_static_user,
                mFollowButtonListener);

        List<String> fragmentClassNames = new ArrayList<String>();
        fragmentClassNames.add(SocialActionsFragment.class.getName());
        fragmentClassNames.add(PlaylistEntriesFragment.class.getName());
        fragmentClassNames.add(UsersFragment.class.getName());
        List<String> fragmentTitles = new ArrayList<String>();
        fragmentTitles.add(getString(R.string.activity));
        fragmentTitles.add(getString(R.string.music));
        fragmentTitles.add(getString(R.string.friends));
        List<Bundle> fragmentBundles = new ArrayList<Bundle>();
        Bundle bundle = new Bundle();
        bundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        bundle.putInt(TomahawkFragment.SHOW_MODE, SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        bundle.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY,
                mUser.getPlaybackLog().getCacheKey());
        bundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentBundles.add(bundle);
        bundle = new Bundle();
        bundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        bundle.putInt(TomahawkFragment.SHOW_MODE, UsersFragment.SHOW_MODE_TYPE_FOLLOWERS);
        bundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUser.getCacheKey());
        fragmentBundles.add(bundle);
        setupPager(fragmentClassNames, fragmentTitles, fragmentBundles, initialPage);
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

    @Override
    public void onPanelCollapsed() {
    }

    @Override
    public void onPanelExpanded() {
    }
}
