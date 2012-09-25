/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.actionbarsherlock.app.SherlockListFragment;

public abstract class TomahawkListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Collection> {

    private static IntentFilter sCollectionUpdateIntentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    protected CollectionActivity mCollectionActivity;

    /**
     * Handles incoming Collection updated broadcasts.
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
     * 
     * @see android.support.v4.app.Fragment#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

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
     * @return the current Collection
     */
    public Collection getCurrentCollection() {
        return ((CollectionActivity) getActivity()).getCollection();
    }
}
