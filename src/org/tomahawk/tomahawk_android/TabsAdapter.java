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

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import java.io.Serializable;
import java.util.ArrayList;

public class TabsAdapter extends PagerAdapter
        implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

    private TomahawkTabsActivity mActivity;

    private ActionBar mActionBar;

    private ViewPager mViewPager;

    private FragmentManager mFragmentManager;

    private ArrayList<TabHolder> mTabHolders = new ArrayList<TabHolder>();

    private boolean mHasRecentlyInstantiatedItems = false;

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
     * This class represents a complete Tab (page in the ViewPager). It consists of a backStack and
     * a resource id of the fragmentContainer which shows the top fragment object in the backStack.
     */
    public static final class TabHolder implements Serializable {

        private ArrayList<FragmentStateHolder> fragmentStateHolders
                = new ArrayList<FragmentStateHolder>();

        private int fragmentContainerId;
    }

    /**
     * Constructs a new TabsAdapter
     */
    public TabsAdapter(TomahawkTabsActivity activity, FragmentManager fragmentManager,
            ViewPager pager, boolean tabsFunctionality) {
        mActionBar = activity.getSupportActionBar();
        mActivity = activity;
        mViewPager = pager;
        mViewPager.setAdapter(this);
        if (tabsFunctionality) {
            mViewPager.setOnPageChangeListener(this);
        }
        mViewPager.setOffscreenPageLimit(2);
        mFragmentManager = fragmentManager;
    }

    @Override
    public int getItemPosition(Object object) {
        return POSITION_NONE;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.view.PagerAdapter#getCount()
     */
    @Override
    public int getCount() {
        return mTabHolders.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }

    @Override
    public Object instantiateItem(ViewGroup collection, int position) {
        TabHolder tabHolder = mTabHolders.get(position);
        FragmentStateHolder currentFSH = tabHolder.fragmentStateHolders
                .get(tabHolder.fragmentStateHolders.size() - 1);
        Fragment currentFragment = mFragmentManager.findFragmentByTag(currentFSH.fragmentTag);
        FrameLayout fragmentContainer = null;
        if (currentFragment != null && currentFragment.getView() != null
                && currentFragment.getView().getParent() != null) {
            fragmentContainer = (FrameLayout) currentFragment.getView().getParent();
        }
        if (fragmentContainer == null
                || fragmentContainer.getId() != tabHolder.fragmentContainerId) {
            fragmentContainer = new FrameLayout(mActivity);
            fragmentContainer.setId(tabHolder.fragmentContainerId);
        }
        collection.addView(fragmentContainer);
        mHasRecentlyInstantiatedItems = true;

        return fragmentContainer;
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.view.PagerAdapter#destroyItem(android.view.View,
     * int, java.lang.Object)
     */
    @Override
    public void destroyItem(ViewGroup collection, int position, Object object) {
        collection.removeView((View) object);
    }

    @Override
    public void finishUpdate(ViewGroup viewGroup) {
        super.finishUpdate(viewGroup);
        if (mHasRecentlyInstantiatedItems) {
            mFragmentManager.executePendingTransactions();
            for (TabHolder tabHolder : mTabHolders) {
                FragmentStateHolder currentFSH = tabHolder.fragmentStateHolders
                        .get(tabHolder.fragmentStateHolders.size() - 1);
                Fragment currentFragment = mFragmentManager
                        .findFragmentByTag(currentFSH.fragmentTag);
                if (currentFragment == null || currentFragment.getView() == null
                        || currentFragment.getView().getParent() == null) {
                    FragmentTransaction ft = mFragmentManager.beginTransaction();
                    Bundle bundle = new Bundle();
                    for (FragmentStateHolder fSH : tabHolder.fragmentStateHolders) {
                        Fragment fragment = mFragmentManager.findFragmentByTag(fSH.fragmentTag);
                        if (fragment != null) {
                            ft.remove(fragment);
                        }
                    }
                    bundle.putLong(currentFSH.tomahawkListItemType, currentFSH.tomahawkListItemId);
                    bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                            currentFSH.listScrollPosition);
                    ft.add(tabHolder.fragmentContainerId,
                            Fragment.instantiate(mActivity, currentFSH.clss.getName(), bundle),
                            currentFSH.fragmentTag);
                    ft.commit();
                }
            }
            mHasRecentlyInstantiatedItems = false;
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.view.ViewPager.OnPageChangeListener#onPageScrolled
     * (int, float, int)
     */
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected
     * (int)
     */
    @Override
    public void onPageSelected(int position) {
        //        mActionBar.setSelectedNavigationItem(position);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.view.ViewPager.OnPageChangeListener#
     * onPageScrollStateChanged(int)
     */
    @Override
    public void onPageScrollStateChanged(int state) {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.actionbarsherlock.app.ActionBar.TabListener#onTabSelected(com.
     * actionbarsherlock.app.ActionBar.Tab,
     * android.support.v4.app.FragmentTransaction)
     */
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.actionbarsherlock.app.ActionBar.TabListener#onTabUnselected(com.
     * actionbarsherlock.app.ActionBar.Tab,
     * android.support.v4.app.FragmentTransaction)
     */
    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    /*
     * (non-Javadoc)
     *
     * @see com.actionbarsherlock.app.ActionBar.TabListener#onTabReselected(com.
     * actionbarsherlock.app.ActionBar.Tab,
     * android.support.v4.app.FragmentTransaction)
     */
    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    /**
     * Add a tab to the ActionBar
     */
    public void addTab(ActionBar.Tab tab) {
        tab.setTabListener(this);
        mActionBar.addTab(tab);
    }

    /**
     * Add the root of the backStack to the tab. Set the resource id for the fragmentContainer for
     * this tab.
     *
     * @param clss the class of the rootFragment to add
     */
    public void addRootToTab(Class clss) {
        TabHolder tabHolder = new TabHolder();
        tabHolder.fragmentContainerId = mTabHolders.size() + 10000000;
        tabHolder.fragmentStateHolders
                .add(new FragmentStateHolder(clss, getFragmentTag(mTabHolders.size(), 0)));
        mTabHolders.add(tabHolder);
    }

    /**
     * Generate the fragmentTag to the given position and offset. Examples:    Position 0 for first
     * tab and offset 0 for the current item in the stack. Position 1 for the second tab and offset
     * -1 for the previous item in the stack.
     *
     * @param position the position of the viewpager
     * @param offset   offset which will be added to the position of the current top item in the
     *                 backstack
     * @return the generated fragmentTag String
     */
    public String getFragmentTag(int position, int offset) {
        if (mTabHolders.size() - 1 < position) {
            return String.valueOf(offset + 1000 * position);
        }
        ArrayList<FragmentStateHolder> fragmentsStack = mTabHolders
                .get(position).fragmentStateHolders;
        return String.valueOf(fragmentsStack.size() - 1 + offset + 1000 * position);
    }

    /**
     * @return get the current position of the viewpager
     */
    public int getCurrentPosition() {
        return mViewPager.getCurrentItem();
    }

    /**
     * Set the current position of the viewpager
     */
    public void setCurrentPosition(int position) {
        mViewPager.setCurrentItem(position, false);
        mActivity.getSlidingMenu().showContent();
    }

    /**
     * Replaces the view pager fragment at specified position.
     */
    public void replace(int position, FragmentStateHolder fragmentStateHolder,
            boolean isBackAction) {
        // Get fragmentsStack for the given (tabs)position
        TabHolder tabHolder = mTabHolders.get(position);
        FragmentStateHolder currentFragmentStateHolder = tabHolder.fragmentStateHolders
                .get(tabHolder.fragmentStateHolders.size() - 1);
        if (currentFragmentStateHolder != null) {
            // Replace the fragment using a transaction.
            FragmentTransaction ft = mFragmentManager.beginTransaction();
            if (isBackAction) {
                tabHolder.fragmentStateHolders.remove(currentFragmentStateHolder);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_CLOSE);
            } else {
                Fragment currentFragment = mFragmentManager
                        .findFragmentByTag(currentFragmentStateHolder.fragmentTag);
                if (currentFragment != null && currentFragment instanceof TomahawkFragment) {
                    currentFragmentStateHolder.listScrollPosition
                            = ((TomahawkFragment) currentFragment).getListScrollPosition();
                    tabHolder.fragmentStateHolders.set(tabHolder.fragmentStateHolders.size() - 1,
                            currentFragmentStateHolder);
                }
                tabHolder.fragmentStateHolders.add(fragmentStateHolder);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
            }
            Bundle bundle = new Bundle();
            bundle.putLong(fragmentStateHolder.tomahawkListItemType,
                    fragmentStateHolder.tomahawkListItemId);
            bundle.putInt(TomahawkFragment.TOMAHAWK_LIST_SCROLL_POSITION,
                    fragmentStateHolder.listScrollPosition);
            ft.replace(tabHolder.fragmentContainerId,
                    Fragment.instantiate(mActivity, fragmentStateHolder.clss.getName(), bundle),
                    fragmentStateHolder.fragmentTag);
            ft.commit();
        }
    }

    /**
     * Replaces the fragment at the current position.
     */
    public void replace(FragmentStateHolder fragmentStateHolder, boolean isBackAction) {
        replace(getCurrentPosition(), fragmentStateHolder, isBackAction);
    }

    /**
     * Replaces the fragment at the given position.
     */
    public void replace(int position, Class clss, long tomahawkListItemId,
            String tomahawkListItemType, boolean isBackAction) {
        replace(position,
                new FragmentStateHolder(clss, getFragmentTag(position, 1), tomahawkListItemId,
                        tomahawkListItemType), isBackAction);
    }

    /**
     * Replaces the current fragment by the previous fragment stored in the backStack. Does nothing
     * and returns false if no fragment is back-stacked.
     */
    public boolean back() {
        int position = mViewPager.getCurrentItem();
        ArrayList<FragmentStateHolder> fragmentsStack = mTabHolders
                .get(position).fragmentStateHolders;
        if (fragmentsStack.size() > 1) {
            FragmentStateHolder previousFragmentStateHolder = fragmentsStack
                    .get(fragmentsStack.size() - 2);
            // Restore the remembered fragment and remove it from back fragments.
            this.replace(previousFragmentStateHolder, true);
            return true;
        }
        // Nothing to go back.
        return false;
    }

    /**
     * Pop the backstack at the given position until the Fragment with the given fragmentTag is on
     * top
     *
     * @param position    the position of the backstack which should be used
     * @param fragmentTag the fragmentTag which belongs to the Fragment that should be gone back to
     * @return true if the Fragment with the given fragmentTag is now on top. False if Fragment with
     *         given fragmentTag not found
     */
    public boolean backToFragment(int position, String fragmentTag) {
        ArrayList<FragmentStateHolder> fragmentsStack = mTabHolders
                .get(position).fragmentStateHolders;
        for (FragmentStateHolder fpb : fragmentsStack) {
            if (fpb.fragmentTag.equals(fragmentTag)) {
                if (fragmentsStack.size() > 2 && !(fragmentsStack.get(fragmentsStack.size() - 1)
                        .fragmentTag.equals(fragmentTag))) {
                    while (!(fragmentsStack.get(fragmentsStack.size() - 2).fragmentTag
                            .equals(fragmentTag))) {
                        fragmentsStack.remove(fragmentsStack.get(fragmentsStack.size() - 2));
                    }
                    this.replace(fragmentsStack.get(fragmentsStack.size() - 2), true);
                    return fragmentsStack.get(fragmentsStack.size() - 2).equals(fragmentTag);
                }
                break;
            }
        }
        return false;
    }

    /**
     * Go back to the root of the backstack at the given position
     *
     * @param position the position of the backstack which should be used
     * @return true if the rootFragment is now on top. False otherwise.
     */
    public boolean backToRoot(int position) {
        ArrayList<FragmentStateHolder> fragmentsStack = mTabHolders
                .get(position).fragmentStateHolders;
        if (fragmentsStack.size() > 1) {
            while (fragmentsStack.size() > 2) {
                fragmentsStack.remove(fragmentsStack.get(fragmentsStack.size() - 2));
            }
            this.replace(fragmentsStack.get(fragmentsStack.size() - 2), true);
        }
        return fragmentsStack.size() == 1;
    }

    /**
     * Get the backstack at the given position
     *
     * @param position the position of the backstack which should be used
     * @return backstack at the given position
     */
    public ArrayList<FragmentStateHolder> getBackStackAtPosition(int position) {
        return mTabHolders.get(position).fragmentStateHolders;
    }

    /**
     * Get the complete backstack
     *
     * @return the complete backstack for every tab
     */
    public ArrayList<TabHolder> getBackStack() {
        ArrayList<FragmentStateHolder> fragmentsStack = mTabHolders
                .get(getCurrentPosition()).fragmentStateHolders;
        FragmentStateHolder currentFragmentStateHolder = fragmentsStack
                .get(fragmentsStack.size() - 1);
        Fragment currentFragment = mFragmentManager
                .findFragmentByTag(currentFragmentStateHolder.fragmentTag);
        if (currentFragment != null && currentFragment instanceof TomahawkFragment) {
            currentFragmentStateHolder.listScrollPosition = ((TomahawkFragment) currentFragment)
                    .getListScrollPosition();
            fragmentsStack.set(fragmentsStack.size() - 1, currentFragmentStateHolder);
        }
        return mTabHolders;
    }

    /**
     * Set the complete backstack
     *
     * @param tabHolders the new backstack
     */
    public void setBackStack(ArrayList<TabHolder> tabHolders) {
        mTabHolders = tabHolders;
        notifyDataSetChanged();
    }

    /**
     * Add a new Fragment to the backstack (should only be used when the TabsAdapter is not yet
     * fully instantiated, otherwise use the replace(...) methods)
     *
     * @param position           position of the backStack
     * @param clss               the class of the fragment that should be added
     * @param tomahawkListItemId the corresponding tomahawkListItem. If not needed this should be
     *                           -1
     */
    public void addFragmentToBackStack(int position, Class clss, long tomahawkListItemId,
            String tomahawkListItemType) {
        FragmentStateHolder fSH = new FragmentStateHolder(clss, getFragmentTag(position, 1),
                tomahawkListItemId, tomahawkListItemType);
        mTabHolders.get(position).fragmentStateHolders.add(fSH);
    }

    /**
     * Get the fragment which currently is on top in the given tab
     */
    public Fragment getFragmentOnTop(int position) {
        ArrayList<FragmentStateHolder> fragmentsStack = mTabHolders
                .get(position).fragmentStateHolders;
        FragmentStateHolder currentFragmentStateHolder = fragmentsStack
                .get(fragmentsStack.size() - 1);
        return mFragmentManager.findFragmentByTag(currentFragmentStateHolder.fragmentTag);
    }
}
