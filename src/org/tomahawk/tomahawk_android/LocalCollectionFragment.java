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

import com.actionbarsherlock.app.SherlockListFragment;

import org.tomahawk.libtomahawk.TomahawkMenuAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

/**
 * Fragment which represents the "UserCollection" tabview.
 */
public class LocalCollectionFragment extends SherlockListFragment implements OnItemClickListener {

    private TomahawkMenuAdapter mLocalCollectionMenuAdapter;

    protected CollectionActivity mCollectionActivity;

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.menu_layout, null, false);
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
        mLocalCollectionMenuAdapter = new TomahawkMenuAdapter(getActivity(),
                getResources().getStringArray(R.array.local_collection_menu_items),
                getResources().obtainTypedArray(R.array.local_collection_menu_items_icons));
        setListAdapter(mLocalCollectionMenuAdapter);
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
                mCollectionActivity.getTabsAdapter()
                        .replace(TomahawkTabsActivity.TAB_ID_MYMUSIC, TracksFragment.class, -1,
                                null, false);
                break;
            case 1:
                mCollectionActivity.getTabsAdapter()
                        .replace(TomahawkTabsActivity.TAB_ID_MYMUSIC, AlbumsFragment.class, -1,
                                null, false);
                break;
            case 2:
                mCollectionActivity.getTabsAdapter()
                        .replace(TomahawkTabsActivity.TAB_ID_MYMUSIC, ArtistsFragment.class, -1,
                                null, false);
                break;
            case 3:
                mCollectionActivity.getTabsAdapter()
                        .replace(TomahawkTabsActivity.TAB_ID_MYMUSIC, PlaylistsFragment.class, -1,
                                null, false);
                break;
            case 4:
        }
    }
}
