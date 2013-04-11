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
package org.tomahawk.tomahawk_android;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

public class ContentViewer {

    private TomahawkTabsActivity mActivity;

    private FragmentManager mFragmentManager;

    private int mContentFrameId;

    private int mCurrentlyShownStack;

    private ConcurrentHashMap<Integer, ArrayList<FragmentStateHolder>> mMapOfStacks
            = new ConcurrentHashMap<Integer, ArrayList<FragmentStateHolder>>();

    /**
     * A FragmentStateHolder represents and stores all information needed to construct a Fragment.
     */
    static final class FragmentStateHolder implements Serializable {

        //The Class variable stores the class of the fragment.
        protected final Class clss;

        //The fragmentTag is unique inside the complete BackStack.
        protected final String fragmentTag;

        //tomahawkListItemId is the id of the corresponding TomahawkListItem which is being passed to the actual
        //fragment instance.
        protected long tomahawkListItemId = -1;

        //the type of the corresponding TomahawkListItem
        protected String tomahawkListItemType = null;

        protected String queryId = null;

        //the listScrollPosition which is being stored and restored when the fragment is popped or stashed.
        protected int listScrollPosition = 0;

        FragmentStateHolder(Class clss, String fragmentTag) {
            this.clss = clss;
            this.fragmentTag = fragmentTag;
        }

        FragmentStateHolder(Class clss, String fragmentTag, long tomahawkListItemId,
                String tomahawkListItemType) {
            this.clss = clss;
            this.fragmentTag = fragmentTag;
            this.tomahawkListItemId = tomahawkListItemId;
            this.tomahawkListItemType = tomahawkListItemType;
        }
    }

    /**
     * Constructs a new ContentViewer
     */
    public ContentViewer(TomahawkTabsActivity activity, FragmentManager fragmentManager,
            int contentFrameId) {
        mActivity = activity;
        mFragmentManager = fragmentManager;
        mContentFrameId = contentFrameId;
    }

    /**
     * Add the root of the backStack to the tab. Set the resource id for the fragmentContainer for
     * this tab.
     *
     * @param clss the class of the rootFragment to add
     */
    public void addRootToTab(int stackId, Class clss) {
        ArrayList<FragmentStateHolder> fragmentStateHolders = new ArrayList<FragmentStateHolder>();
        fragmentStateHolders
                .add(new FragmentStateHolder(clss, getFragmentTag(mMapOfStacks.size(), 0)));
        mMapOfStacks.put(stackId, fragmentStateHolders);
    }

    /**
     * Generate the fragmentTag to the given position and offset. Examples:    Position 0 for first
     * tab and offset 0 for the current item in the stack. Position 1 for the second tab and offset
     * -1 for the previous item in the stack.
     *
     * @param stackId the position of the viewpager
     * @param offset  offset which will be added to the position of the current top item in the
     *                backstack
     * @return the generated fragmentTag String
     */
    public String getFragmentTag(int stackId, int offset) {
        if (mMapOfStacks.size() - 1 < stackId) {
            return String.valueOf(offset + 1000 * stackId);
        }
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfStacks.get(stackId);
        return String.valueOf(fragmentsStack.size() - 1 + offset + 1000 * stackId);
    }

    /**
     * Replaces the view pager fragment at specified position.
     */
    public void replace(int stackId, FragmentStateHolder fragmentStateHolder,
            boolean isBackAction) {
        // Get fragmentsStack for the given (tabs)position
        ArrayList<FragmentStateHolder> fragmentStateHolders = mMapOfStacks.get(stackId);
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
            bundle.putLong(fragmentStateHolder.tomahawkListItemType,
                    fragmentStateHolder.tomahawkListItemId);
            bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                    fragmentStateHolder.listScrollPosition);
            bundle.putString(SearchableFragment.SEARCHABLEFRAGMENT_QUERY_ID,
                    fragmentStateHolder.queryId);
            ft.replace(mContentFrameId,
                    Fragment.instantiate(mActivity, fragmentStateHolder.clss.getName(), bundle),
                    fragmentStateHolder.fragmentTag);
            ft.commit();
        }
        mActivity.onBackStackChanged();
    }

    /**
     * Replaces the fragment at the given position.
     */
    public void replace(int stackId, Class clss, long tomahawkListItemId,
            String tomahawkListItemType, boolean isBackAction) {
        FragmentStateHolder fragmentStateHolder = new FragmentStateHolder(clss,
                getFragmentTag(stackId, 1), tomahawkListItemId, tomahawkListItemType);
        replace(stackId, fragmentStateHolder, isBackAction);
    }

    /**
     * Replaces the current fragment by the previous fragment stored in the backStack. Does nothing
     * and returns false if no fragment is back-stacked.
     */
    public boolean back(int stackId) {
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfStacks.get(stackId);
        if (fragmentsStack.size() > 1) {
            FragmentStateHolder previousFragmentStateHolder = fragmentsStack
                    .get(fragmentsStack.size() - 2);
            // Restore the remembered fragment and remove it from back fragments.
            this.replace(stackId, previousFragmentStateHolder, true);
            mActivity.onBackStackChanged();
            return true;
        }
        // Nothing to go back.
        return false;
    }

    /**
     * Pop the backstack at the given position until the Fragment with the given fragmentTag is on
     * top
     *
     * @param stackId     the position of the backstack which should be used
     * @param fragmentTag the fragmentTag which belongs to the Fragment that should be gone back to
     * @return true if the Fragment with the given fragmentTag is now on top. False if Fragment with
     *         given fragmentTag not found
     */
    public boolean backToFragment(int stackId, String fragmentTag, boolean withBundle) {
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfStacks.get(stackId);
        for (FragmentStateHolder fpb : fragmentsStack) {
            if (fpb.fragmentTag.equals(fragmentTag)) {
                if (fragmentsStack.size() > 1) {
                    while (fragmentsStack.size() > 0 && !(fragmentsStack
                            .get(fragmentsStack.size() - 1).fragmentTag.equals(fragmentTag))) {
                        fragmentsStack.remove(fragmentsStack.get(fragmentsStack.size() - 1));
                    }
                    FragmentTransaction ft = mFragmentManager.beginTransaction();
                    if (withBundle) {
                        Bundle bundle = new Bundle();
                        bundle.putLong(fpb.tomahawkListItemType, fpb.tomahawkListItemId);
                        bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                                fpb.listScrollPosition);
                        bundle.putString(SearchableFragment.SEARCHABLEFRAGMENT_QUERY_ID,
                                fpb.queryId);
                        ft.replace(mContentFrameId,
                                Fragment.instantiate(mActivity, fpb.clss.getName(), bundle),
                                fpb.fragmentTag);
                    } else {
                        ft.replace(mContentFrameId,
                                Fragment.instantiate(mActivity, fpb.clss.getName()),
                                fpb.fragmentTag);
                    }
                    ft.commit();
                    mActivity.onBackStackChanged();
                }
                return fragmentsStack.get(fragmentsStack.size() - 1).equals(fragmentTag);
            }
        }
        return false;
    }

    /**
     * Go back to the root of the backstack at the given position
     *
     * @param stackId the position of the backstack which should be used
     * @return true if the rootFragment is now on top. False otherwise.
     */
    public boolean backToRoot(int stackId, boolean withBundle) {
        return backToFragment(stackId, mMapOfStacks.get(stackId).get(0).fragmentTag, withBundle);
    }

    /**
     * Get the backstack at the given position
     *
     * @param stackId the position of the backstack which should be used
     * @return backstack at the given position
     */
    public ArrayList<FragmentStateHolder> getBackStackAtPosition(int stackId) {
        return mMapOfStacks.get(stackId);
    }

    /**
     * Get the complete backstack
     *
     * @return the complete backstack for every tab
     */
    public ConcurrentHashMap<Integer, ArrayList<FragmentStateHolder>> getBackStack() {
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfStacks.get(mCurrentlyShownStack);
        FragmentStateHolder currentFragmentStateHolder = fragmentsStack
                .get(fragmentsStack.size() - 1);
        Fragment currentFragment = mFragmentManager
                .findFragmentByTag(currentFragmentStateHolder.fragmentTag);
        if (currentFragment != null && currentFragment instanceof TomahawkFragment) {
            currentFragmentStateHolder.listScrollPosition = ((TomahawkFragment) currentFragment)
                    .getListScrollPosition();
            fragmentsStack.set(fragmentsStack.size() - 1, currentFragmentStateHolder);
        }
        return mMapOfStacks;
    }

    /**
     * Set the complete backstack
     *
     * @param fragmentStateHolders the new backstack
     */
    public void setBackStack(
            ConcurrentHashMap<Integer, ArrayList<FragmentStateHolder>> fragmentStateHolders) {
        mMapOfStacks = fragmentStateHolders;
    }

    /**
     * Get the fragment which currently is on top in the given tab
     */
    public Fragment getFragmentOnTop(int stackId) {
        ArrayList<FragmentStateHolder> fragmentsStack = mMapOfStacks.get(stackId);
        FragmentStateHolder currentFragmentStateHolder = fragmentsStack
                .get(fragmentsStack.size() - 1);
        return mFragmentManager.findFragmentByTag(currentFragmentStateHolder.fragmentTag);
    }

    public int getCurrentStackId() {
        return mCurrentlyShownStack;
    }

    public void setCurrentStackId(int stackToShow) {
        if (mCurrentlyShownStack != stackToShow) {
            mCurrentlyShownStack = stackToShow;
            ArrayList<FragmentStateHolder> stack = mMapOfStacks.get(stackToShow);
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            FragmentStateHolder fragmentStateHolder = stack.get(stack.size() - 1);
            Bundle bundle = new Bundle();
            bundle.putLong(fragmentStateHolder.tomahawkListItemType,
                    fragmentStateHolder.tomahawkListItemId);
            bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                    fragmentStateHolder.listScrollPosition);
            bundle.putString(SearchableFragment.SEARCHABLEFRAGMENT_QUERY_ID,
                    fragmentStateHolder.queryId);
            ft.replace(mContentFrameId,
                    Fragment.instantiate(mActivity, fragmentStateHolder.clss.getName(), bundle),
                    stack.get(stack.size() - 1).fragmentTag);
            ft.commit();
        }
        mActivity.onBackStackChanged();
    }
}
