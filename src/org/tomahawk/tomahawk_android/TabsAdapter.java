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

import java.util.ArrayList;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.app.SherlockFragmentActivity;

public class TabsAdapter extends FragmentPagerAdapter implements ActionBar.TabListener, ViewPager.OnPageChangeListener {

    private final ArrayList<ArrayList<Fragment>> mFragments = new ArrayList<ArrayList<Fragment>>();
    private ActionBar mActionBar;
    private ViewPager mViewPager;
    private FragmentManager mFragmentManager;

    /**
     * Constructs a new TabsAdapter
     * 
     * @param activity
     *            the activity context in which a new TabsAdapter is constructed
     * @param pager
     *            the ViewPager object to display the content of the tabs
     */
    public TabsAdapter(SherlockFragmentActivity activity, FragmentManager fragmentManager, ViewPager pager) {
        super(fragmentManager);
        mActionBar = activity.getSupportActionBar();
        mViewPager = pager;
        mViewPager.setAdapter(this);
        mViewPager.setOnPageChangeListener(this);
        mFragmentManager = fragmentManager;
    }

    /**
     * Add a tab to the ActionBar
     * 
     * @param tab
     * @param clss
     * @param args
     */
    public void addTab(ActionBar.Tab tab, Fragment fragment) {
        tab.setTabListener(this);
        mActionBar.addTab(tab);
        ArrayList<Fragment> fragmentsStack = new ArrayList<Fragment>();
        fragmentsStack.add(fragment);
        mFragments.add(fragmentsStack);
        notifyDataSetChanged();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.view.PagerAdapter#getCount()
     */
    @Override
    public int getCount() {
        return mFragments.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.FragmentPagerAdapter#getItem(int)
     */
    @Override
    public Fragment getItem(int position) {
        ArrayList<Fragment> fragmentsStack = mFragments.get(position);
        return fragmentsStack.get(fragmentsStack.size() - 1);
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.FragmentPagerAdapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        // Get currently active fragment.
        ArrayList<Fragment> fragmentsStack = mFragments.get(position);

        return fragmentsStack.size() - 1 + 100 * position;
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.view.PagerAdapter#getItemPosition(java.lang.Object)
     */
    @Override
    public int getItemPosition(Object object) {
        for (ArrayList<Fragment> fragmentsStack : mFragments) {
            for (Fragment fragment : fragmentsStack) {
                if (object instanceof Fragment && fragment.getClass() != object.getClass()) {
                    return POSITION_NONE;
                }
            }
        }
        return POSITION_UNCHANGED;
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
        mActionBar.setSelectedNavigationItem(position);
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
     * Replaces the view pager fragment at specified position.
     */
    public void replace(int position, Fragment newFragment, boolean isBackAction) {
        // Get currently active fragment.
        ArrayList<Fragment> fragmentsStack = mFragments.get(position);
        Fragment currentFragment = fragmentsStack.get(fragmentsStack.size() - 1);
        if (currentFragment == null) {
            return;
        }
        // Replace the fragment using a transaction.
        this.startUpdate(mViewPager);
        FragmentTransaction ft = mFragmentManager.beginTransaction();
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.attach(newFragment).remove(currentFragment).commit();
        if (isBackAction == true)
            fragmentsStack.remove(currentFragment);
        else
            fragmentsStack.add(newFragment);
        this.notifyDataSetChanged();
        this.finishUpdate(mViewPager);
    }

    /**
     * Replaces the view pager fragment at current position.
     */
    public void replace(Fragment newFragment, boolean isBackAction) {
        replace(mViewPager.getCurrentItem(), newFragment, isBackAction);
    }

    /**
     * Replaces the current fragment by fragment stored in back stack. Does nothing and returns
     * false if no fragment is back-stacked.
     */
    public boolean back() {
        int position = mViewPager.getCurrentItem();
        ArrayList<Fragment> fragmentsStack = mFragments.get(position);
        if (fragmentsStack.size() < 2) {
            // Nothing to go back.
            return false;
        }
        Fragment previousFragment = fragmentsStack.get(fragmentsStack.size() - 2);
        // Restore the remembered fragment and remove it from back fragments.
        this.replace(previousFragment, true);
        return true;
    }
}