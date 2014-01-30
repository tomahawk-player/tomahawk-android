/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Christopher Reichert <creichert07@gmail.com>
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
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.GridView;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * More customizable implementation of {@link android.app.ListFragment}
 */
public class TomahawkListFragment extends Fragment {

    public static final String TOMAHAWK_LIST_SCROLL_POSITION
            = "org.tomahawk.tomahawk_android.tomahawk_list_scroll_position";

    protected TomahawkApp mTomahawkApp;

    protected TomahawkMainActivity mTomahawkMainActivity;

    private StickyListHeadersAdapter mTomahawkListAdapter;

    private TomahawkGridAdapter mTomahawkGridAdapter;

    private boolean mShowGridView;

    private StickyListHeadersListView mList;

    private GridView mGrid;

    private int mListScrollPosition = 0;

    private boolean restoreScrollPosition = true;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            (mShowGridView ? mGrid : mList).focusableViewAvailable((mShowGridView ? mGrid : mList));
        }
    };

    /**
     * Store the reference to the attached {@link android.app.Activity}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof TomahawkMainActivity) {
            mTomahawkMainActivity = (TomahawkMainActivity) activity;
        }
    }

    /**
     * Get a stored list scroll position, if present
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTomahawkApp = ((TomahawkApp) mTomahawkMainActivity.getApplication());
        if (getArguments() != null) {
            if (getArguments().containsKey(TOMAHAWK_LIST_SCROLL_POSITION)
                    && getArguments().getInt(TOMAHAWK_LIST_SCROLL_POSITION) > 0) {
                mListScrollPosition = getArguments().getInt(TOMAHAWK_LIST_SCROLL_POSITION);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tomahawklistfragment_layout, null, false);
    }

    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mGrid = null;
        super.onDestroyView();
    }

    /**
     * Get this {@link TomahawkListFragment}'s {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
     */
    public StickyListHeadersListView getListView() {
        ensureList();
        return mList;
    }

    /**
     * Get this {@link TomahawkListFragment}'s {@link GridView}
     */
    public GridView getGridView() {
        ensureList();
        return mGrid;
    }

    /**
     * Set mList/mGrid to the listview/gridview layout element and catch possible exceptions.
     */
    private void ensureList() {
        if (((mShowGridView) ? mGrid : mList) != null) {
            return;
        }
        View root = getView();
        LayoutInflater layoutInflater = mTomahawkMainActivity.getLayoutInflater();
        if (root == null) {
            throw new IllegalStateException("Content view not yet created");
        }
        if (!mShowGridView) {
            mList = (StickyListHeadersListView) layoutInflater
                    .inflate(R.layout.stickylistheaderslistview, null, false);
            View listViewContainer = root
                    .findViewById(R.id.fragmentLayout_listLayout_frameLayout);
            if (listViewContainer instanceof FrameLayout) {
                ((FrameLayout) listViewContainer).addView(mList);
            }
            if (mTomahawkListAdapter != null) {
                setListAdapter(mTomahawkListAdapter);
            }
        } else {
            mGrid = (GridView) layoutInflater.inflate(R.layout.gridview, null, false);
            View listViewContainer = root
                    .findViewById(R.id.fragmentLayout_gridLayout_frameLayout);
            if (listViewContainer instanceof FrameLayout) {
                ((FrameLayout) listViewContainer).addView(mGrid);
            }
            if (mTomahawkGridAdapter != null) {
                setGridAdapter(mTomahawkGridAdapter);
            }
        }
        mHandler.post(mRequestFocus);
    }

    /**
     * @return the current scrolling position of the list- or gridView
     */
    public int getListScrollPosition() {
        if (mShowGridView) {
            return getGridView().getFirstVisiblePosition();
        }
        return getListView().getFirstVisiblePosition();
    }

    /**
     * Get the {@link se.emilsjolander.stickylistheaders.StickyListHeadersAdapter} associated with
     * this activity's ListView.
     */
    public StickyListHeadersAdapter getListAdapter() {
        return mTomahawkListAdapter;
    }

    /**
     * Get the {@link org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter} associated with
     * this activity's GridView.
     */
    public TomahawkGridAdapter getGridAdapter() {
        return mTomahawkGridAdapter;
    }

    /**
     * Set the {@link org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter} associated with
     * this activity's ListView.
     */
    public void setListAdapter(StickyListHeadersAdapter adapter) {
        mTomahawkListAdapter = adapter;
        mShowGridView = false;
        StickyListHeadersListView listView = getListView();
        listView.setAdapter(adapter);
        if (restoreScrollPosition) {
            listView.setSelection(mListScrollPosition);
        }
    }

    /**
     * Set the {@link org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter} associated with
     * this activity's GridView.
     */
    public void setGridAdapter(TomahawkGridAdapter adapter) {
        mTomahawkGridAdapter = adapter;
        mShowGridView = true;
        GridView gridView = getGridView();
        gridView.setAdapter(adapter);
        if (restoreScrollPosition) {
            gridView.setSelection(mListScrollPosition);
        }
    }

    public boolean isShowGridView() {
        return mShowGridView;
    }

    public void setRestoreScrollPosition(boolean restoreScrollPosition) {
        this.restoreScrollPosition = restoreScrollPosition;
    }
}
