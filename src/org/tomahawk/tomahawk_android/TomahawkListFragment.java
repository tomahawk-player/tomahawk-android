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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.EditText;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

public abstract class TomahawkListFragment extends SherlockListFragment implements
        LoaderManager.LoaderCallbacks<Collection> {

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;
    private EditText mFilterText = null;

    private SearchWatcher mFilterTextWatcher;

    /**
     * Class which manages search functionality withing fragments
     */
    private class SearchWatcher implements TextWatcher {

        /**
         * Called when text is changed in the search bar.
         */
        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (getAdapter() != null)
                getAdapter().getFilter().filter(s);
        }

        @Override
        public void afterTextChanged(Editable s) {
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }
    }

    /**
     * Handles incoming Collection updated broadcasts.
     */
    private class CollectionUpdateReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Collection.COLLECTION_UPDATED))
                onCollectionUpdated();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        setRetainInstance(true);

        mFilterTextWatcher = new SearchWatcher();
    }

    /** Called when the activity for this Fragment is created. */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        getSherlockActivity().getSupportLoaderManager().destroyLoader(getId());
        getSherlockActivity().getSupportLoaderManager().initLoader(getId(), null, this);
    }

    /** Called when this Fragment is resumed. */
    @Override
    public void onResume() {
        super.onResume();

        mCollectionUpdatedReceiver = new CollectionUpdateReceiver();
        IntentFilter filter = new IntentFilter(Collection.COLLECTION_UPDATED);
        getActivity().registerReceiver(mCollectionUpdatedReceiver, filter);
    }

    /** Called when this Fragment is paused. */
    @Override
    public void onPause() {
        super.onPause();

        if (mCollectionUpdatedReceiver != null)
            getActivity().unregisterReceiver(mCollectionUpdatedReceiver);
    }

    /** Called when the options menu for this Fragment is created. */
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        MenuItem item = (MenuItem) menu.findItem(TomahawkMainActivity.SEARCH_OPTION_ID);
        mFilterText = (EditText) item.getActionView().findViewById(R.id.search_edittext);
        mFilterText.addTextChangedListener(mFilterTextWatcher);
    }

    /** Called when the options menu is destroyed. */
    @Override
    public void onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu();

        mFilterText.removeTextChangedListener(mFilterTextWatcher);
    }

    /**
     * Returns the Adapter for this TomahawkListFragment.
     * 
     * @return
     */
    protected abstract ArrayAdapter<?> getAdapter();

    /**
     * Called when a Collection has been updated.
     */
    protected void onCollectionUpdated() {
        getSherlockActivity().getSupportLoaderManager().restartLoader(getId(), null, this);
    }

    /**
     * Called when the CollectionLoader needs to be created.
     */
    @Override
    public Loader<Collection> onCreateLoader(int id, Bundle args) {

        /** For now we will just start with the local collection to test. */
        TomahawkApp app = (TomahawkApp) getActivity().getApplicationContext();
        return new CollectionLoader(getActivity(), app.getSourceList().getLocalSource()
                .getCollection());
    }

    /**
     * Called when the CollectionLoader has finished loading.
     */
    @Override
    public void onLoadFinished(Loader<Collection> loader, Collection coll) {
        setListShown(true);
    }

    /**
     * Called when the loader is reset.
     */
    @Override
    public void onLoaderReset(Loader<Collection> loader) {
    }
}
