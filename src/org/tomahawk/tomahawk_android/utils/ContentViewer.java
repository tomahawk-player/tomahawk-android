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
package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.fragments.SearchableFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.UserCollectionFragment;
import org.tomahawk.tomahawk_android.fragments.UserPlaylistsFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;

/**
 * This class wraps all functionality that handles the switching of {@link Fragment}s, whenever the
 * user navigates to a new {@link Fragment}. It also implements a custom back stack for every hub,
 * so the user can always return to the previous {@link Fragment}s. There is one hub for every menu
 * entry in the navigation drawer.
 *
 *
 * Example state of the {@link ContentViewer}:
 *
 * Home (Hub #1): HomeFragment (not yet implemented) -> END OF BACKSTACK
 *
 * Search (Hub #2): SearchableFragment -> END OF BACKSTACK
 *
 * Collection (Hub #3): UserCollectionFragment -> AlbumsFragment -> END OF BACKSTACK
 *
 * Playlists (Hub #4): UserPlaylistsFragment -> TracksFragment -> END OF BACKSTACK
 *
 * Stations (Hub #5): StationsFragment (not yet implemented) -> END OF BACKSTACK
 *
 * Friends (Hub #6): FriendsFragment (not yet implemented) -> END OF BACKSTACK
 *
 * Settings (Hub #7): FakePreferenceFragment -> END OF BACKSTACK
 */
public class ContentViewer {

    public static final int HUB_ID_HOME = -1;

    public static final int HUB_ID_COLLECTION = 0;

    public static final int HUB_ID_PLAYLISTS = 1;

    public static final int HUB_ID_STATIONS = -2;

    public static final int HUB_ID_FRIENDS = -3;

    public static final int HUB_ID_SETTINGS = 2;

    public static final int HUB_ID_PLAYBACK = 100;

    private TomahawkMainActivity mTomahawkMainActivity;

    private FragmentManager mFragmentManager;

    private int mContentFrameId;

    private ArrayList<FragmentStateHolder> mBackstack = new ArrayList<FragmentStateHolder>();

    /**
     * A {@link FragmentStateHolder} represents and stores all information needed to construct a
     * {@link Fragment}.
     */
    public static final class FragmentStateHolder implements Serializable {

        //The Class variable stores the class of the fragment.
        public final Class clss;

        //tomahawkListItemKey is the id of the corresponding TomahawkListItem which is being passed to the actual
        //fragment instance.
        public String tomahawkListItemKey = "";

        //the type of the corresponding TomahawkListItem
        public String tomahawkListItemType = null;

        //whether or not the corresponding TomahawkListItem is local
        public boolean tomahawkListItemIsLocal = false;

        public String queryString = null;

        //the listScrollPosition which is being stored and restored when the fragment is popped or stashed.
        public int listScrollPosition = 0;

        public ArrayList<String> correspondingQueryIds;

        /**
         * Construct a {@link FragmentStateHolder} without providing a reference to a {@link
         * org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
         */
        public FragmentStateHolder(Class clss, ArrayList<String> correspondingQueryIds) {
            this.clss = clss;
            this.correspondingQueryIds = correspondingQueryIds;
        }

        /**
         * Construct a {@link FragmentStateHolder} while also providing a reference to a {@link
         * org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
         */
        public FragmentStateHolder(Class clss, ArrayList<String> correspondingQueryIds,
                String tomahawkListItemKey, String tomahawkListItemType,
                boolean tomahawkListItemIsLocal) {
            this.clss = clss;
            this.correspondingQueryIds = correspondingQueryIds;
            this.tomahawkListItemKey = tomahawkListItemKey;
            this.tomahawkListItemType = tomahawkListItemType;
            this.tomahawkListItemIsLocal = tomahawkListItemIsLocal;
        }
    }

    /**
     * Constructs a new {@link ContentViewer}
     */
    public ContentViewer(TomahawkMainActivity activity, FragmentManager fragmentManager,
            int contentFrameId) {
        mTomahawkMainActivity = activity;
        mFragmentManager = fragmentManager;
        mContentFrameId = contentFrameId;
    }

    /**
     * Replaces the {@link Fragment} in the hub with the given hub id and adds it to the backstack,
     * if isBackAction is false.
     *
     * @param fragmentStateHolder {@link FragmentStateHolder} contains all information of the to be
     *                            replaced {@link Fragment}
     * @param isBackAction        whether or not the replacement is part of an action going back in
     *                            the backstack
     */
    public void replace(FragmentStateHolder fragmentStateHolder, boolean isBackAction) {
        // Get fragmentsStack for the given (tabs)position
        FragmentStateHolder currentFragmentStateHolder = getCurrentFragmentStateHolder();
        // Replace the fragment using a transaction.
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        if (isBackAction) {
            mBackstack.remove(currentFragmentStateHolder);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
        } else {
            Fragment currentFragment = null;
            if (mFragmentManager != null && mFragmentManager.getFragments() != null) {
                currentFragment = mFragmentManager.getFragments().get(0);
            }
            if (currentFragmentStateHolder != null && currentFragment != null
                    && currentFragment instanceof TomahawkFragment) {
                currentFragmentStateHolder.listScrollPosition
                        = ((TomahawkFragment) currentFragment).getListScrollPosition();
            }
            mBackstack.add(fragmentStateHolder);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        }
        Bundle bundle = new Bundle();
        bundle.putString(fragmentStateHolder.tomahawkListItemType,
                fragmentStateHolder.tomahawkListItemKey);
        bundle.putBoolean(TomahawkFragment.TOMAHAWK_LIST_ITEM_IS_LOCAL,
                fragmentStateHolder.tomahawkListItemIsLocal);
        bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                fragmentStateHolder.listScrollPosition);
        bundle.putString(SearchableFragment.SEARCHABLEFRAGMENT_QUERY_STRING,
                fragmentStateHolder.queryString);
        ft.replace(mContentFrameId,
                Fragment.instantiate(mTomahawkMainActivity, fragmentStateHolder.clss.getName(),
                        bundle));
        ft.commit();
        mTomahawkMainActivity.updateViewVisibility();
    }

    /**
     * Replaces the {@link Fragment} in the hub with the given hub id and adds it to the backstack,
     * if isBackAction is false.
     *
     * @param clss                 The {@link Fragment}'s class to be used to construct a new {@link
     *                             FragmentStateHolder}
     * @param tomahawkListItemKey  the key of the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
     *                             corresponding to the {@link Fragment}
     * @param tomahawkListItemType {@link String} containing the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}'s
     *                             type
     * @param isBackAction         whether or not the replacement is part of an action going back in
     *                             the backstack
     */
    public void replace(Class clss, String tomahawkListItemKey,
            String tomahawkListItemType, boolean tomahawkListItemIsLocal,
            boolean isBackAction) {
        FragmentStateHolder fragmentStateHolder = new FragmentStateHolder(clss, null,
                tomahawkListItemKey, tomahawkListItemType,
                tomahawkListItemIsLocal);
        replace(fragmentStateHolder, isBackAction);
    }

    /**
     * Replaces the current {@link Fragment} with the previous {@link Fragment} stored in the
     * backStack. Does nothing and returns false if no previous {@link Fragment} exists.
     */
    public boolean back() {
        if (mBackstack.size() > 1) {
            FragmentStateHolder previousFragmentStateHolder = mBackstack.get(mBackstack.size() - 2);
            // Restore the remembered fragment and remove it from back fragments.
            this.replace(previousFragmentStateHolder, true);
            return true;
        }
        // Nothing to go back to.
        return false;
    }

    /**
     * Get the complete backstack
     *
     * @return the complete backstack
     */
    public ArrayList<FragmentStateHolder> getBackStack() {
        FragmentStateHolder currentFragmentStateHolder = getCurrentFragmentStateHolder();
        Fragment currentFragment = null;
        if (mFragmentManager != null && mFragmentManager.getFragments() != null) {
            currentFragment = mFragmentManager.getFragments().get(0);
        }
        if (currentFragment != null && currentFragment instanceof TomahawkFragment) {
            currentFragmentStateHolder.listScrollPosition = ((TomahawkFragment) currentFragment)
                    .getListScrollPosition();
            mBackstack.set(mBackstack.size() - 1, currentFragmentStateHolder);
        }
        return mBackstack;
    }

    public FragmentStateHolder getCurrentFragmentStateHolder() {
        if (mBackstack.size() > 0) {
            return mBackstack.get(mBackstack.size() - 1);
        }
        return null;
    }

    /**
     * Set the complete backstack
     *
     * @param backStack the new complete backstack
     */
    public void setBackStack(ArrayList<FragmentStateHolder> backStack) {
        mBackstack = backStack;
    }

    /**
     * Set the currently shown hub, by providing its id
     *
     * @param hubToShow the id of the hub which should be shown
     */
    public void showHub(int hubToShow) {
        Class clss = null;
        switch (hubToShow) {
            case HUB_ID_HOME:
            case HUB_ID_COLLECTION:
                clss = UserCollectionFragment.class;
                break;
            case HUB_ID_PLAYLISTS:
                clss = UserPlaylistsFragment.class;
                break;
            case HUB_ID_STATIONS:
            case HUB_ID_FRIENDS:
            case HUB_ID_SETTINGS:
                clss = FakePreferenceFragment.class;
                break;
            case HUB_ID_PLAYBACK:
                clss = PlaybackFragment.class;
                break;
        }
        FragmentStateHolder currentFragmentStateHolder = getCurrentFragmentStateHolder();
        if (clss != null
                || currentFragmentStateHolder == null
                || currentFragmentStateHolder.clss != null
                || !TextUtils.isEmpty(currentFragmentStateHolder.tomahawkListItemKey)
                || currentFragmentStateHolder.tomahawkListItemType != null) {
            FragmentStateHolder newFragmentStateHolder = new FragmentStateHolder(clss, null);
            replace(newFragmentStateHolder, false);
        }
    }
}
