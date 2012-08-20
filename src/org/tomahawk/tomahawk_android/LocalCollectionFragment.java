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

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;

import com.actionbarsherlock.app.SherlockListFragment;

/**
 * Fragment which represents the "LocalCollection" tabview.
 */
public class LocalCollectionFragment extends SherlockListFragment implements OnItemClickListener {

    private ArrayAdapter<String> mLocalCollectionAdapter;
    protected CollectionActivity mCollectionActivity;

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        getListView().setFastScrollEnabled(true);
        getListView().setOnItemClickListener(this);
        mLocalCollectionAdapter = new ArrayAdapter<String>(getActivity(), R.layout.single_line_list_menu, R.id.single_line_list_menu_textview, getResources().getStringArray(
                R.array.local_collection_menu_items));
        setListAdapter(mLocalCollectionAdapter);
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof CollectionActivity) {
            mCollectionActivity = (CollectionActivity) activity;
        }
        super.onAttach(activity);
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onDetach()
     */
    @Override
    public void onDetach() {
        mCollectionActivity = null;
        super.onDetach();
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        switch ((int) arg3) {
        case 0:
            mCollectionActivity.getTabsAdapter().replace(new TrackFragment(), false);
            break;
        case 1:
            mCollectionActivity.getTabsAdapter().replace(new AlbumFragment(), false);
            break;
        case 2:
            mCollectionActivity.getTabsAdapter().replace(new ArtistFragment(), false);
            break;
        case 3:
        case 4:
        }
    }
}