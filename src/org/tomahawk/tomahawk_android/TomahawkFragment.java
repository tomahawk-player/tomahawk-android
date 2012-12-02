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

import java.util.ArrayList;

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.CollectionLoader;
import org.tomahawk.libtomahawk.TomahawkBaseAdapter;
import org.tomahawk.libtomahawk.TomahawkGridAdapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;

import com.actionbarsherlock.app.SherlockFragment;

public abstract class TomahawkFragment extends SherlockFragment implements LoaderManager.LoaderCallbacks<Collection> {

    private static IntentFilter sCollectionUpdateIntentFilter = new IntentFilter(Collection.COLLECTION_UPDATED);

    private CollectionUpdateReceiver mCollectionUpdatedReceiver;

    protected CollectionActivity mCollectionActivity;

    static final int INTERNAL_EMPTY_ID = 0x00ff0001;
    static final int INTERNAL_PROGRESS_CONTAINER_ID = 0x00ff0002;
    static final int INTERNAL_LIST_CONTAINER_ID = 0x00ff0003;

    TomahawkBaseAdapter mTomahawkBaseAdapter;
    ListView mList;
    GridView mGrid;

    private int mPosition;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            ((mTomahawkBaseAdapter instanceof TomahawkGridAdapter) ? mGrid : mList).focusableViewAvailable(((mTomahawkBaseAdapter instanceof TomahawkGridAdapter)
                    ? mGrid : mList));
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
        updateBreadCrumbNavigation();
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

        if (mTomahawkBaseAdapter instanceof TomahawkGridAdapter) {
            getGridView().setSelection(mPosition);
        } else {
            getListView().setSelection(mPosition);
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

        if (mTomahawkBaseAdapter instanceof TomahawkGridAdapter) {
            mPosition = getGridView().getFirstVisiblePosition();
        } else {
            mPosition = getListView().getFirstVisiblePosition();
        }

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

    public void updateBreadCrumbNavigation() {
        ArrayList<Fragment> backStack = mCollectionActivity.getTabsAdapter().getBackStack(0);
        LinearLayout navigationLayoutView = (LinearLayout) getActivity().findViewById(
                R.id.fragmentLayout_breadcrumbLayout_linearLayout);
        int validFragmentCount = 0;
        for (Fragment fragment : backStack) {
            if (fragment instanceof AlbumsFragment || fragment instanceof TracksFragment
                    || fragment instanceof ArtistsFragment)
                validFragmentCount++;
        }
        for (Fragment fragment : backStack) {
            LinearLayout breadcrumbItem = (LinearLayout) getActivity().getLayoutInflater().inflate(
                    R.layout.fragment_layout_breadcrumb_item, null);
            ImageView breadcrumbItemImageView = (ImageView) breadcrumbItem.findViewById(R.id.fragmentLayout_icon_imageButton);
            SquareHeightRelativeLayout breadcrumbItemImageViewLayout = (SquareHeightRelativeLayout) breadcrumbItem.findViewById(R.id.fragmentLayout_icon_squareHeightRelativeLayout);
            TextView breadcrumbItemTextView = (TextView) breadcrumbItem.findViewById(R.id.fragmentLayout_text_textView);
            if (fragment instanceof AlbumsFragment) {
                AlbumsFragment albumsFragment = (AlbumsFragment) fragment;
                if (albumsFragment.getArtist() != null) {
                    breadcrumbItemTextView.setText(albumsFragment.getArtist().getName());
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.GONE);
                } else {
                    if (validFragmentCount == 1)
                        breadcrumbItemTextView.setText(getString(R.string.albumsfragment_title_string));
                    else
                        breadcrumbItemTextView.setVisibility(TextView.GONE);
                    breadcrumbItemImageView.setBackground(getResources().getDrawable(R.drawable.ic_action_album));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                }
                breadcrumbItem.setOnClickListener(new BreadCrumbOnClickListener(fragment));
                navigationLayoutView.addView(breadcrumbItem);
            } else if (fragment instanceof ArtistsFragment) {
                if (validFragmentCount == 1)
                    breadcrumbItemTextView.setText(getString(R.string.artistsfragment_title_string));
                else
                    breadcrumbItemTextView.setVisibility(TextView.GONE);
                breadcrumbItemImageView.setBackground(getResources().getDrawable(R.drawable.ic_action_artist));
                breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                breadcrumbItem.setOnClickListener(new BreadCrumbOnClickListener(fragment));
                navigationLayoutView.addView(breadcrumbItem);
            } else if (fragment instanceof TracksFragment) {
                TracksFragment tracksFragment = (TracksFragment) fragment;
                if (tracksFragment.getAlbum() != null) {
                    breadcrumbItemTextView.setText(tracksFragment.getAlbum().getName());
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.GONE);
                } else {
                    if (validFragmentCount == 1)
                        breadcrumbItemTextView.setText(getString(R.string.tracksfragment_title_string));
                    else
                        breadcrumbItemTextView.setVisibility(TextView.GONE);
                    breadcrumbItemImageView.setBackground(getResources().getDrawable(R.drawable.ic_action_track));
                    breadcrumbItemImageViewLayout.setVisibility(SquareHeightRelativeLayout.VISIBLE);
                }
                breadcrumbItem.setOnClickListener(new BreadCrumbOnClickListener(fragment));
                navigationLayoutView.addView(breadcrumbItem);
            }
        }
    }

    public class BreadCrumbOnClickListener implements View.OnClickListener {
        Fragment mSavedFragment;

        public BreadCrumbOnClickListener(Fragment savedFragment) {
            mSavedFragment = savedFragment;
        }

        @Override
        public void onClick(View view) {
            mCollectionActivity.getTabsAdapter().backToFragment(0, mSavedFragment);
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
        return mTomahawkBaseAdapter;
    }

    private void ensureList() {
        if (((mTomahawkBaseAdapter instanceof TomahawkGridAdapter) ? mGrid : mList) != null) {
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
            if (!(mTomahawkBaseAdapter instanceof TomahawkGridAdapter)) {
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
        if (mTomahawkBaseAdapter != null) {
            TomahawkBaseAdapter adapter = mTomahawkBaseAdapter;
            mTomahawkBaseAdapter = null;
            setListAdapter(adapter);
        }
        mHandler.post(mRequestFocus);
    }

    /**
     * Provide the cursor for the list view.
     */
    public void setListAdapter(TomahawkBaseAdapter adapter) {
        mTomahawkBaseAdapter = adapter;
        if (((mTomahawkBaseAdapter instanceof TomahawkGridAdapter) ? mGrid : mList) != null) {
            if (mTomahawkBaseAdapter instanceof TomahawkGridAdapter)
                mGrid.setAdapter(adapter);
            else
                mList.setAdapter(adapter);
        }
    }

    /**
     * @return the current Collection
     */
    public Collection getCurrentCollection() {
        return ((CollectionActivity) getActivity()).getCollection();
    }
}
