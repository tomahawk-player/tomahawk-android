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
 * This {@link SlideMenuFragment} displays a simple list of menu entries. Used as the {@link
 * ListFragment}, which is being shown as the Navigation Drawer, when the user swipes from left to
 * right.
 */
public class SlideMenuFragment extends ListFragment implements AdapterView.OnItemClickListener {

    protected TomahawkMainActivity mTomahawkMainActivity;

    /**
     * Store the reference to the {@link Activity}, in which this Fragment has been created
     */
    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof TomahawkMainActivity) {
            mTomahawkMainActivity = (TomahawkMainActivity) activity;
        }
        super.onAttach(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.slide_menu_layout, null);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getListView().setOnItemClickListener(this);
        // Set up the TomahawkMenuAdapter. Give it its set of menu item texts and icons to display
        TomahawkMenuAdapter slideMenuAdapter = new TomahawkMenuAdapter(getActivity(),
                getResources().getStringArray(R.array.slide_menu_items),
                getResources().obtainTypedArray(R.array.slide_menu_items_icons));
        setListAdapter(slideMenuAdapter);
    }

    /**
     * Null the reference to this Fragment's {@link Activity}
     */
    @Override
    public void onDetach() {
        mTomahawkMainActivity = null;
        super.onDetach();
    }

    /**
     * Called every time an item inside the {@link android.widget.ListView} is clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view
     *                 provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        Handler h = new Handler();
        // Show the correct hub, and if needed, display the search editText inside the ActionBar
        switch ((int) id) {
            case TomahawkMainActivity.HUB_ID_SEARCH:
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentStackId(TomahawkMainActivity.HUB_ID_SEARCH);
                mTomahawkMainActivity.showSearchEditText();
                h.postDelayed(new Runnable() {
                    public void run() {
                        mTomahawkMainActivity.showContent();
                    }
                }, 50);
                break;
            case TomahawkMainActivity.HUB_ID_COLLECTION:
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentStackId(TomahawkMainActivity.HUB_ID_COLLECTION);
                mTomahawkMainActivity.hideSearchEditText();
                h.postDelayed(new Runnable() {
                    public void run() {
                        mTomahawkMainActivity.showContent();
                    }
                }, 50);
                break;
            case TomahawkMainActivity.HUB_ID_PLAYLISTS:
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentStackId(TomahawkMainActivity.HUB_ID_PLAYLISTS);
                mTomahawkMainActivity.hideSearchEditText();
                h.postDelayed(new Runnable() {
                    public void run() {
                        mTomahawkMainActivity.showContent();
                    }
                }, 50);
                break;
            case TomahawkMainActivity.HUB_ID_SETTINGS:
                mTomahawkMainActivity.getContentViewer()
                        .setCurrentStackId(TomahawkMainActivity.HUB_ID_SETTINGS);
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
