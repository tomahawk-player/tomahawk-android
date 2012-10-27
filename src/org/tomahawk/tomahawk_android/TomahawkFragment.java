/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk, Christopher Reichert <creichert07@gmail.com>
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

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.CollectionLoader;
import org.tomahawk.libtomahawk.TomahawkListAdapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.GridView;
import android.widget.ListAdapter;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class TomahawkFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Collection> {

    private static IntentFilter sCollectionUpdateIntentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    protected CollectionActivity mCollectionActivity;

    static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    TomahawkListAdapter mAdapter;
    private boolean mShowAsGrid = false;
    ListView mList;
    GridView mGrid;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            (mShowAsGrid ? mGrid : mList).focusableViewAvailable((mShowAsGrid ? mGrid : mList));
        }
    };

    /**
     * Handles incoming {@link Collection} updated broadcasts.
     */
    private class CollectionUpdateReceiver extends BroadcastReceiver {

        /*
         * (non-Javadoc)
         * 
         * @see
         * android.content.BroadcastReceiver#onReceive(android.content.Context,
         * android.content.Intent)
         */
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Collection.COLLECTION_UPDATED))
                onCollectionUpdated();
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.ListFragment#onCreateView(android.view.LayoutInflater, android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = new View(getActivity().getApplicationContext());
        view = inflater.inflate(R.layout.fragment_layout, null, false);
        return view;
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onViewCreated(android.view.View, android.os.Bundle)
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ensureList();
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onDestroyView()
     */
    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mGrid = null;
        super.onDestroyView();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        adaptColumnCount();

        getSherlockActivity().getSupportLoaderManager().destroyLoader(getId());
        getSherlockActivity().getSupportLoaderManager().initLoader(getId(), null, this);

        if (mCollectionUpdatedReceiver == null) {
            mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
            getActivity().registerReceiver(mCollectionUpdatedReceiver, sCollectionUpdateIntentFilter);
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.support.v4.app.Fragment#onPause()
     */
    @Override
    public void onPause() {
        super.onPause();

        if (mCollectionUpdatedReceiver != null) {
            getActivity().unregisterReceiver(mCollectionUpdatedReceiver);
            mCollectionUpdatedReceiver = null;
        }
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
        super.onDetach();

        mCollectionActivity = null;
    }

    /* 
     * (non-Javadoc)
     * @see android.support.v4.app.Fragment#onConfigurationChanged(android.content.res.Configuration)
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        adaptColumnCount();
    }

    /** Adjust the column count so it fits to the current screen configuration */
    public void adaptColumnCount() {
        if (getGridView() != null) {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE)
                getGridView().setNumColumns(4);
            else
                getGridView().setNumColumns(2);
        }
    }

    /**
     * Called when a Collection has been updated.
     */
    protected void onCollectionUpdated() {
        getSherlockActivity().getSupportLoaderManager().restartLoader(getId(), null, this);
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onCreateLoader(int,
     * android.os.Bundle)
     */
    @Override
    public Loader<Collection> onCreateLoader(int id, Bundle args) {
        return new CollectionLoader(getActivity(), getCurrentCollection());
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoadFinished(android
     * .support.v4.content.Loader, java.lang.Object)
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * android.support.v4.app.LoaderManager.LoaderCallbacks#onLoaderReset(android
     * .support.v4.content.Loader)
     */
    @Override
    public void onLoaderReset(Loader<Collection> loader) {
    }

    /**
     * set wether or not the data should be shown in a gridView
     * @param showAsGrid */
    public void setShowAsGrid(boolean showAsGrid) {
        mShowAsGrid = showAsGrid;
        if (mAdapter != null)
            mAdapter.setShowAsGrid(true);
    }

    /** 
     * set wether or not the data should be shown in a listView
     * @param showAsList */
    public void setShowAsList(boolean showAsList) {
        mShowAsGrid = !showAsList;
        if (mAdapter != null)
            mAdapter.setShowAsGrid(!showAsList);
    }

    /**
     * Get the activity's list view widget.
     */
    public ListView getListView() {
        ensureList();
        return mList;
    }

    /**
     * Get the activity's list view widget.
     */
    public GridView getGridView() {
        ensureList();
        return mGrid;
    }

    /**
     * Get the ListAdapter associated with this activity's ListView.
     */
    public ListAdapter getListAdapter() {
        return mAdapter;
    }

    private void ensureList() {
        if ((mShowAsGrid ? mGrid : mList) != null) {
            return;
        }
        View root = getView();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (root instanceof ListView) {
            mList = (ListView) root;
        } else if (root instanceof GridView) {
            mGrid = (GridView) root;
        } else {
            if (!mShowAsGrid) {
                View rawListView = root.findViewById(R.id.listview);
                if (!(rawListView instanceof ListView)) {
                    if (rawListView == null) {
                        throw new RuntimeException("Your content must have a ListView whose id attribute is "
                                + "'R.id.listview'");
                    }
                    throw new RuntimeException("Content has view with id attribute 'R.id.listview' "
                            + "that is not a ListView class");
                }
                mList = (ListView) rawListView;
            } else {
                View rawListView = root.findViewById(R.id.gridview);
                if (!(rawListView instanceof GridView)) {
                    if (rawListView == null) {
                        throw new RuntimeException("Your content must have a GridView whose id attribute is "
                                + "'R.id.gridview'");
                    }
                    throw new RuntimeException("Content has view with id attribute 'R.id.gridview' "
                            + "that is not a GridView class");
                }
                mGrid = (GridView) rawListView;
            }
        }
        if (mAdapter != null) {
            TomahawkListAdapter adapter = mAdapter;
            mAdapter = null;
            setListAdapter(adapter);
        }
        mHandler.post(mRequestFocus);
    }

    /**
     * Provide the cursor for the list view.
     */
    public void setListAdapter(TomahawkListAdapter adapter) {
        mAdapter = adapter;
        if ((mShowAsGrid ? mGrid : mList) != null) {
            if (mShowAsGrid)
                mGrid.setAdapter(adapter);
            else
                mList.setAdapter(adapter);
            mAdapter.setShowAsGrid(mShowAsGrid);
        }
    }

    /**
     * @return the current Collection
     */
    public Collection getCurrentCollection() {
        return ((CollectionActivity) getActivity()).getCollection();
    }
}
