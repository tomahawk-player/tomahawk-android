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
import org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter;

import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
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

    private StickyListHeadersAdapter mTomahawkListAdapter;

    private TomahawkGridAdapter mTomahawkGridAdapter;

    private boolean mShowGridView;

    private StickyListHeadersListView mList;

    private GridView mGrid;

    private Parcelable mListState = null;

    private boolean restoreScrollPosition = true;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            (mShowGridView ? mGrid : mList).focusableViewAvailable((mShowGridView ? mGrid : mList));
        }
    };

    /**
     * Get a stored list scroll position, if present
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(TOMAHAWK_LIST_SCROLL_POSITION)) {
                mListState = savedInstanceState.getParcelable(
                        TOMAHAWK_LIST_SCROLL_POSITION);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.tomahawklistfragment_layout, container, false);
    }

    @Override
    public void onPause() {
        super.onPause();

        mListState = getListState();
    }

    @Override
    public void onDestroyView() {
        mHandler.removeCallbacks(mRequestFocus);
        mList = null;
        mGrid = null;
        super.onDestroyView();
    }

    /**
     * Save the {@link String} inside the search {@link android.widget.TextView}.
     */
    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        out.putParcelable(TOMAHAWK_LIST_SCROLL_POSITION, mListState);
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
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        if (root == null) {
            return;
        }
        if (!mShowGridView) {
            View container = root
                    .findViewById(R.id.fragmentLayout_listLayout_frameLayout);
            mList = (StickyListHeadersListView) layoutInflater
                    .inflate(R.layout.stickylistheaderslistview, (ViewGroup) container,
                            false);
            if (container instanceof FrameLayout) {
                ((FrameLayout) container).addView(mList);
            }
            if (mTomahawkListAdapter != null) {
                setListAdapter(mTomahawkListAdapter);
            }
        } else {
            View container = root
                    .findViewById(R.id.fragmentLayout_gridLayout_frameLayout);
            mGrid = (GridView) layoutInflater
                    .inflate(R.layout.gridview, (ViewGroup) container, false);
            if (container instanceof FrameLayout) {
                ((FrameLayout) container).addView(mGrid);
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
    public Parcelable getListState() {
        if (mShowGridView) {
            return getGridView().onSaveInstanceState();
        }
        return getListView().getWrappedList().onSaveInstanceState();
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
        getListView().setAdapter(adapter);
        if (restoreScrollPosition && mListState != null) {
            getListView().getWrappedList().onRestoreInstanceState(mListState);
        }
    }

    /**
     * Set the {@link org.tomahawk.tomahawk_android.adapters.TomahawkGridAdapter} associated with
     * this activity's GridView.
     */
    public void setGridAdapter(TomahawkGridAdapter adapter) {
        mTomahawkGridAdapter = adapter;
        mShowGridView = true;
        getGridView().setAdapter(adapter);
        if (restoreScrollPosition && mListState != null) {
            getGridView().onRestoreInstanceState(mListState);
        }
    }

    public boolean isShowGridView() {
        return mShowGridView;
    }

    public void setRestoreScrollPosition(boolean restoreScrollPosition) {
        this.restoreScrollPosition = restoreScrollPosition;
    }
}
