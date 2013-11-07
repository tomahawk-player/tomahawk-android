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
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Fragment which represents the "UserCollection" tabview.
 */
public class LocalCollectionFragment extends TomahawkListFragment implements OnItemClickListener {

    protected TomahawkMainActivity mTomahawkMainActivity;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TomahawkMenuAdapter tomahawkMenuAdapter = new TomahawkMenuAdapter(getActivity(),
                getResources().getStringArray(R.array.local_collection_menu_items),
                getResources().obtainTypedArray(R.array.local_collection_menu_items_icons));
        setListAdapter(tomahawkMenuAdapter);
        getListView().setOnItemClickListener(this);
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
        switch ((int) arg3) {
            case 0:
                mTomahawkMainActivity.getContentViewer()
                        .replace(TomahawkMainActivity.TAB_ID_COLLECTION, TracksFragment.class, -1,
                                null, false);
                break;
            case 1:
                mTomahawkMainActivity.getContentViewer()
                        .replace(TomahawkMainActivity.TAB_ID_COLLECTION, AlbumsFragment.class, -1,
                                null, false);
                break;
            case 2:
                mTomahawkMainActivity.getContentViewer()
                        .replace(TomahawkMainActivity.TAB_ID_COLLECTION, ArtistsFragment.class, -1,
                                null, false);
                break;
        }
    }
}
