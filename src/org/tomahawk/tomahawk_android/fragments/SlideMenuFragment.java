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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.activities.TomahawkTabsActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkMenuAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 06.04.13
 */
public class SlideMenuFragment extends ListFragment implements AdapterView.OnItemClickListener {

    protected TomahawkMainActivity mTomahawkMainActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.slide_menu_layout, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(this);
        TomahawkMenuAdapter slideMenuAdapter = new TomahawkMenuAdapter(getActivity(),
                getResources().getStringArray(R.array.slide_menu_items),
                getResources().obtainTypedArray(R.array.slide_menu_items_icons));
        setListAdapter(slideMenuAdapter);
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof TomahawkMainActivity) {
            mTomahawkMainActivity = (TomahawkMainActivity) activity;
        }
        super.onAttach(activity);
    }

    /*
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onDetach()
     */
    @Override
    public void onDetach() {
        mTomahawkMainActivity = null;
        super.onDetach();
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        Handler h = new Handler();
        switch ((int) arg3) {
            case TomahawkTabsActivity.TAB_ID_SEARCH:
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentStackId(TomahawkTabsActivity.TAB_ID_SEARCH);
                mTomahawkMainActivity.showSearchEditText();
                h.postDelayed(new Runnable() {
                    public void run() {
                        mTomahawkMainActivity.showContent();
                    }
                }, 50);
                break;
            case TomahawkTabsActivity.TAB_ID_COLLECTION:
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentStackId(TomahawkTabsActivity.TAB_ID_COLLECTION);
                mTomahawkMainActivity.hideSearchEditText();
                h.postDelayed(new Runnable() {
                    public void run() {
                        mTomahawkMainActivity.showContent();
                    }
                }, 50);
                break;
            case TomahawkTabsActivity.TAB_ID_PLAYLISTS:
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentStackId(TomahawkTabsActivity.TAB_ID_PLAYLISTS);
                mTomahawkMainActivity.hideSearchEditText();
                h.postDelayed(new Runnable() {
                    public void run() {
                        mTomahawkMainActivity.showContent();
                    }
                }, 50);
                break;
            case TomahawkTabsActivity.TAB_ID_SETTINGS:
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentStackId(TomahawkTabsActivity.TAB_ID_SETTINGS);
                mTomahawkMainActivity.hideSearchEditText();
                h.postDelayed(new Runnable() {
                    public void run() {
                        mTomahawkMainActivity.showContent();
                    }
                }, 50);
                break;
        }
    }
}
