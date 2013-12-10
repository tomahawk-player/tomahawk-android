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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.SearchableFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

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

    private TomahawkMainActivity mTomahawkMainActivity;

    private FragmentManager mFragmentManager;

    private int mContentFrameId;

    private int mCurrentlyShownHub;

    private ConcurrentHashMap<Integer, ArrayList<FragmentStateHolder>> mMapOfHubs
            = new ConcurrentHashMap<Integer, ArrayList<FragmentStateHolder>>();

    /**
     * A {@link FragmentStateHolder} represents and stores all information needed to construct a
     * {@link Fragment}.
     */
    public static final class FragmentStateHolder implements Serializable {

        //The Class variable stores the class of the fragment.
        public final Class clss;

        //The fragmentTag is unique inside the complete BackStack.
        public final String fragmentTag;

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

        public int correspondingHubId = -1;

        public ArrayList<String> correspondingQueryIds;

        /**
         * Construct a {@link FragmentStateHolder} without providing a reference to a {@link
         * org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
         */
        FragmentStateHolder(Class clss, String fragmentTag, int correspondingHubId,
                ArrayList<String> correspondingQueryIds) {
            this.clss = clss;
            this.fragmentTag = fragmentTag;
            this.correspondingHubId = correspondingHubId;
            this.correspondingQueryIds = correspondingQueryIds;
        }

        /**
         * Construct a {@link FragmentStateHolder} while also providing a reference to a {@link
         * org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
         */
        FragmentStateHolder(Class clss, String fragmentTag, int correspondingHubId,
                ArrayList<String> correspondingQueryIds, String tomahawkListItemKey,
                String tomahawkListItemType, boolean tomahawkListItemIsLocal) {
            this.clss = clss;
            this.fragmentTag = fragmentTag;
            this.correspondingHubId = correspondingHubId;
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
     * Add the root of the backStack to the hub with the given id. Set the resource id for the
     * fragmentContainer for this hub.
     *
     * @param hubId the id of the hub
     * @param clss  the class of the rootFragment to add
     */
    public void addRootToHub(int hubId, Class clss) {
        ArrayList<FragmentStateHolder> fragmentStateHolders = new ArrayList<FragmentStateHolder>();
        fragmentStateHolders
                .add(new FragmentStateHolder(clss, getFragmentTag(mMapOfHubs.size(), 0), hubId,
                        null));
        mMapOfHubs.put(hubId, fragmentStateHolders);
    }

    /**
     * Generate a fragmentTag to the given hub id and offset.
     *
     * Examples: Position 0 for first tab and offset 0 for the current item in the stack.
     *
     * Position 1 for the second tab and offset -1 for the previous item in the stack.
     *
     * @param hubId  the id of the hub for which to generate the fragmentTag
     * @param offset offset which will be added to the position of the current top item in the
     *               backstack
     * @return the generated fragmentTag String
     */
    public String getFragmentTag(int hubId, int offset) {
        if (mMapOfHubs.size() - 1 < hubId) {
            return String.valueOf(offset + 1000 * hubId);
        }
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfHubs.get(hubId);
        return String.valueOf(fragmentsStack.size() - 1 + offset + 1000 * hubId);
    }

    /**
     * Replaces the {@link Fragment} in the hub with the given hub id and adds it to the backstack,
     * if isBackAction is false.
     *
     * @param hubId               the id of the hub
     * @param fragmentStateHolder {@link FragmentStateHolder} contains all information of the to be
     *                            replaced {@link Fragment}
     * @param isBackAction        whether or not the replacement is part of an action going back in
     *                            the backstack
     */
    public void replace(int hubId, FragmentStateHolder fragmentStateHolder, boolean isBackAction) {
        // Get fragmentsStack for the given (tabs)position
        ArrayList<FragmentStateHolder> fragmentStateHolders = mMapOfHubs.get(hubId);
        FragmentStateHolder currentFragmentStateHolder = fragmentStateHolders
                .get(fragmentStateHolders.size() - 1);
        if (currentFragmentStateHolder != null) {
            // Replace the fragment using a transaction.
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            if (isBackAction) {
                fragmentStateHolders.remove(currentFragmentStateHolder);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            } else {
                Fragment currentFragment = mFragmentManager
                        .findFragmentByTag(currentFragmentStateHolder.fragmentTag);
                if (currentFragment != null && currentFragment instanceof TomahawkFragment) {
                    currentFragmentStateHolder.listScrollPosition
                            = ((TomahawkFragment) currentFragment).getListScrollPosition();
                }
                fragmentStateHolders.add(fragmentStateHolder);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            }
            Bundle bundle = new Bundle();
            bundle.putString(fragmentStateHolder.tomahawkListItemType,
                    fragmentStateHolder.tomahawkListItemKey);
            bundle.putBoolean(TomahawkFragment.TOMAHAWK_LIST_ITEM_IS_LOCAL,
                    fragmentStateHolder.tomahawkListItemIsLocal);
            bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                    fragmentStateHolder.listScrollPosition);
            bundle.putInt(TomahawkFragment.TOMAHAWK_HUB_ID, fragmentStateHolder.correspondingHubId);
            bundle.putString(SearchableFragment.SEARCHABLEFRAGMENT_QUERY_STRING,
                    fragmentStateHolder.queryString);
            ft.replace(mContentFrameId,
                    Fragment.instantiate(mTomahawkMainActivity, fragmentStateHolder.clss.getName(),
                            bundle), fragmentStateHolder.fragmentTag);
            ft.commit();
        }

        mTomahawkMainActivity.onBackStackChanged();
    }

    /**
     * Replaces the {@link Fragment} in the hub with the given hub id and adds it to the backstack,
     * if isBackAction is false.
     *
     * @param hubId                the id of the hub
     * @param clss                 The {@link Fragment}'s class to be used to construct a new {@link
     *                             FragmentStateHolder}
     * @param tomahawkListItemKey  the key of the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}
     *                             corresponding to the {@link Fragment}
     * @param tomahawkListItemType {@link String} containing the {@link org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter.TomahawkListItem}'s
     *                             type
     * @param isBackAction         whether or not the replacement is part of an action going back in
     *                             the backstack
     */
    public void replace(int hubId, Class clss, String tomahawkListItemKey,
            String tomahawkListItemType, boolean tomahawkListItemIsLocal,
            boolean isBackAction) {
        FragmentStateHolder fragmentStateHolder = new FragmentStateHolder(clss,
                getFragmentTag(hubId, 1), hubId, null, tomahawkListItemKey, tomahawkListItemType,
                tomahawkListItemIsLocal);
        replace(hubId, fragmentStateHolder, isBackAction);
    }

    /**
     * Replaces the current {@link Fragment} with the previous {@link Fragment} stored in the
     * backStack. Does nothing and returns false if no previous {@link Fragment} exists.
     *
     * @param hubId the id of the hub in which to go back
     */
    public boolean back(int hubId) {
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfHubs.get(hubId);
        if (fragmentsStack.size() > 1) {
            FragmentStateHolder previousFragmentStateHolder = fragmentsStack
                    .get(fragmentsStack.size() - 2);
            // Restore the remembered fragment and remove it from back fragments.
            this.replace(hubId, previousFragmentStateHolder, true);
            mTomahawkMainActivity.onBackStackChanged();
            return true;
        }
        // Nothing to go back to.
        return false;
    }

    /**
     * Pop the backstack of the hub with the given id until the {@link Fragment} with the given
     * fragmentTag is on top
     *
     * @param hubId       the id of the hub
     * @param fragmentTag the fragmentTag which belongs to the Fragment that should be gone back to
     * @return true if the Fragment with the given fragmentTag is now on top. False if Fragment with
     * given fragmentTag not found
     */
    public boolean backToFragment(int hubId, String fragmentTag, boolean withBundle) {
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfHubs.get(hubId);
        for (FragmentStateHolder fpb : fragmentsStack) {
            if (fpb.fragmentTag != null && fpb.fragmentTag.equals(fragmentTag)) {
                if (fragmentsStack.size() > 1) {
                    while (fragmentsStack.size() > 0 && !(fragmentsStack
                            .get(fragmentsStack.size() - 1).fragmentTag.equals(fragmentTag))) {
                        fragmentsStack.remove(fragmentsStack.get(fragmentsStack.size() - 1));
                    }
                    FragmentTransaction ft = mFragmentManager.beginTransaction();
                    if (withBundle) {
                        Bundle bundle = new Bundle();
                        bundle.putString(fpb.tomahawkListItemType, fpb.tomahawkListItemKey);
                        bundle.putBoolean(TomahawkFragment.TOMAHAWK_LIST_ITEM_IS_LOCAL,
                                fpb.tomahawkListItemIsLocal);
                        bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                                fpb.listScrollPosition);
                        bundle.putInt(TomahawkFragment.TOMAHAWK_HUB_ID, fpb.correspondingHubId);
                        bundle.putString(SearchableFragment.SEARCHABLEFRAGMENT_QUERY_STRING,
                                fpb.queryString);
                        ft.replace(mContentFrameId,
                                Fragment.instantiate(mTomahawkMainActivity, fpb.clss.getName(),
                                        bundle), fpb.fragmentTag);
                    } else {
                        Bundle bundle = new Bundle();
                        bundle.putInt(TomahawkFragment.TOMAHAWK_HUB_ID, fpb.correspondingHubId);
                        ft.replace(mContentFrameId,
                                Fragment.instantiate(mTomahawkMainActivity, fpb.clss.getName(),
                                        bundle), fpb.fragmentTag);
                    }
                    ft.commit();
                    mTomahawkMainActivity.onBackStackChanged();
                }
                return fragmentsStack.get(fragmentsStack.size() - 1).fragmentTag
                        .equals(fragmentTag);
            }
        }
        return false;
    }

    /**
     * Go back to the root of the backstack in the hub with the given id
     *
     * @param hubId the id of the hub
     * @return true if the rootFragment is now on top. False otherwise.
     */
    public boolean backToRoot(int hubId, boolean withBundle) {
        return backToFragment(hubId, mMapOfHubs.get(hubId).get(0).fragmentTag, withBundle);
    }

    /**
     * Get the backstack of the hub with the given id
     *
     * @param hubId the id of the hub
     * @return backstack backstack of the hub with the given id
     */
    public ArrayList<FragmentStateHolder> getBackStackAtPosition(int hubId) {
        return mMapOfHubs.get(hubId);
    }

    /**
     * Get the complete backstack
     *
     * @return the complete backstack for every hub
     */
    public ConcurrentHashMap<Integer, ArrayList<FragmentStateHolder>> getBackStack() {
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfHubs.get(mCurrentlyShownHub);
        FragmentStateHolder currentFragmentStateHolder = fragmentsStack
                .get(fragmentsStack.size() - 1);
        Fragment currentFragment = mFragmentManager
                .findFragmentByTag(currentFragmentStateHolder.fragmentTag);
        if (currentFragment != null && currentFragment instanceof TomahawkFragment) {
            currentFragmentStateHolder.listScrollPosition = ((TomahawkFragment) currentFragment)
                    .getListScrollPosition();
            fragmentsStack.set(fragmentsStack.size() - 1, currentFragmentStateHolder);
        }
        return mMapOfHubs;
    }

    /**
     * Set the complete backstack
     *
     * @param fragmentStateHolders the new complete set of all hub's backstacks
     */
    public void setBackStack(
            ConcurrentHashMap<Integer, ArrayList<FragmentStateHolder>> fragmentStateHolders) {
        mMapOfHubs = fragmentStateHolders;
    }

    /**
     * Get the fragment which currently is on top in the backstack of the hub with the given id
     *
     * @param hubId the id of the hub
     */
    public Fragment getFragmentOnTop(int hubId) {
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfHubs.get(hubId);
        FragmentStateHolder currentFragmentStateHolder = fragmentsStack
                .get(fragmentsStack.size() - 1);
        return mFragmentManager.findFragmentByTag(currentFragmentStateHolder.fragmentTag);
    }

    /**
     * @return the currently shown hub's id
     */
    public int getCurrentHubId() {
        return mCurrentlyShownHub;
    }

    /**
     * @return the currently shown hub's title resource id, 0 if an error occurred
     */
    private int getCurrentHubTitleResId() {
        switch (mCurrentlyShownHub) {
            case TomahawkMainActivity.HUB_ID_HOME:
                return R.string.hub_title_home;
            case TomahawkMainActivity.HUB_ID_SEARCH:
                return R.string.hub_title_search;
            case TomahawkMainActivity.HUB_ID_COLLECTION:
                return R.string.hub_title_collection;
            case TomahawkMainActivity.HUB_ID_PLAYLISTS:
                return R.string.hub_title_playlists;
            case TomahawkMainActivity.HUB_ID_STATIONS:
                return R.string.hub_title_stations;
            case TomahawkMainActivity.HUB_ID_FRIENDS:
                return R.string.hub_title_friends;
            case TomahawkMainActivity.HUB_ID_SETTINGS:
                return R.string.hub_title_settings;
            case TomahawkMainActivity.HUB_ID_PLAYBACK:
                return R.string.hub_title_playback;
        }
        return 0;
    }

    /**
     * Set the currently shown hub, by providing its id
     *
     * @param hubToShow the id of the hub which should be shown
     */
    public void setCurrentHubId(int hubToShow) {
        if (mCurrentlyShownHub != hubToShow) {
            mCurrentlyShownHub = hubToShow;
            mTomahawkMainActivity
                    .setTitle(mTomahawkMainActivity.getString(getCurrentHubTitleResId()));
            ArrayList<FragmentStateHolder> stack = mMapOfHubs.get(hubToShow);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            FragmentStateHolder fragmentStateHolder = stack.get(stack.size() - 1);
            Bundle bundle = new Bundle();
            bundle.putString(fragmentStateHolder.tomahawkListItemType,
                    fragmentStateHolder.tomahawkListItemKey);
            bundle.putBoolean(TomahawkFragment.TOMAHAWK_LIST_ITEM_IS_LOCAL,
                    fragmentStateHolder.tomahawkListItemIsLocal);
            bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                    fragmentStateHolder.listScrollPosition);
            bundle.putInt(TomahawkFragment.TOMAHAWK_HUB_ID, fragmentStateHolder.correspondingHubId);
            bundle.putString(SearchableFragment.SEARCHABLEFRAGMENT_QUERY_STRING,
                    fragmentStateHolder.queryString);
            ft.replace(mContentFrameId,
                    Fragment.instantiate(mTomahawkMainActivity, fragmentStateHolder.clss.getName(),
                            bundle), stack.get(stack.size() - 1).fragmentTag);
            ft.commit();
            mTomahawkMainActivity.onBackStackChanged();
            mTomahawkMainActivity.updateViewVisibility();
        }
    }
}
