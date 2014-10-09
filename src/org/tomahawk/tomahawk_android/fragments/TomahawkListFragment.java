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
import org.tomahawk.tomahawk_android.adapters.StickyBaseAdapter;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * More customizable implementation of {@link android.app.ListFragment}
 */
public abstract class TomahawkListFragment extends ContentHeaderFragment implements
        AbsListView.OnScrollListener {

    public static final String TOMAHAWK_LIST_SCROLL_POSITION
            = "org.tomahawk.tomahawk_android.tomahawk_list_scroll_position";

    private StickyBaseAdapter mStickyBaseAdapter;

    private StickyListHeadersListView mList;

    private Parcelable mListState = null;

    private boolean restoreScrollPosition = true;

    final private Handler mHandler = new Handler();

    final private Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
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
    public void onResume() {
        super.onResume();

        if (getListView() != null) {
            getListView().setOnScrollListener(this);
            getListView().setAreHeadersSticky(getResources().getBoolean(R.bool.set_headers_sticky));
        }
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

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        if (firstVisibleItem == 0 && getListView().getListChildAt(0) != null) {
            float delta = getListView().getListChildAt(0).getBottom() - getListView().getTop();
            animateContentHeader(
                    (int) (10000f - delta / getListView().getListChildAt(0).getHeight() * 10000f));
        } else {
            animateContentHeader(10000);
        }
    }

    protected void showContentHeader(Object item, int headerHeightResid) {
        super.showContentHeader(
                (FrameLayout) getView().findViewById(R.id.content_header_image_frame),
                (FrameLayout) getView().findViewById(R.id.content_header_frame), item,
                true, headerHeightResid, null);

        //Add a spacer to the top of the listview
        FrameLayout listFrame = (FrameLayout) getView().findViewById(
                R.id.fragmentLayout_listLayout_frameLayout);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) listFrame.getLayoutParams();
        int offset = getResources().getDimensionPixelSize(headerHeightResid);
        params.setMargins(0, offset, 0, 0);
        listFrame.setLayoutParams(params);
    }

    protected void showContentHeader(int drawableResid) {
        super.showContentHeader(
                (FrameLayout) getView().findViewById(R.id.content_header_image_frame),
                (FrameLayout) getView().findViewById(R.id.content_header_frame), drawableResid,
                true, 0, null);

        //Add a spacer to the top of the listview
        FrameLayout listFrame = (FrameLayout) getView().findViewById(
                R.id.fragmentLayout_listLayout_frameLayout);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) listFrame.getLayoutParams();
        int offset = 0;
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            offset = TypedValue.complexToDimensionPixelSize(tv.data,
                    getResources().getDisplayMetrics());
        }
        params.setMargins(0, offset, 0, 0);
        listFrame.setLayoutParams(params);
    }

    protected void setActionBarOffset() {
        FrameLayout listFrame =
                (FrameLayout) getView().findViewById(R.id.fragmentLayout_listLayout_frameLayout);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) listFrame.getLayoutParams();
        int actionBarHeight = 0;
        TypedValue tv = new TypedValue();
        if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
            actionBarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                    getResources().getDisplayMetrics());
        }
        params.setMargins(0, actionBarHeight, 0, 0);
        listFrame.setLayoutParams(params);

        View actionBarBg = getView().findViewById(R.id.action_bar_background);
        if (actionBarBg != null) {
            actionBarBg.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Get this {@link TomahawkListFragment}'s {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
     */
    public StickyListHeadersListView getListView() {
        ensureList();
        return mList;
    }

    /**
     * Set mList/mGrid to the listview/gridview layout element and catch possible exceptions.
     */
    private void ensureList() {
        if (mList != null) {
            return;
        }
        View root = getView();
        LayoutInflater layoutInflater = (LayoutInflater)
                TomahawkApp.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        if (root == null || layoutInflater == null) {
            return;
        }
        View container = root
                .findViewById(R.id.fragmentLayout_listLayout_frameLayout);
        mList = (StickyListHeadersListView) layoutInflater
                .inflate(R.layout.stickylistheaderslistview, (ViewGroup) container,
                        false);
        if (container instanceof FrameLayout) {
            ((FrameLayout) container).addView(mList);
        }
        if (mStickyBaseAdapter != null) {
            setListAdapter(mStickyBaseAdapter);
        }
        mHandler.post(mRequestFocus);
    }

    /**
     * @return the current scrolling position of the list- or gridView
     */
    public Parcelable getListState() {
        return getListView().getWrappedList().onSaveInstanceState();
    }

    /**
     * Get the {@link BaseAdapter} associated with this activity's ListView.
     */
    public StickyBaseAdapter getListAdapter() {
        return mStickyBaseAdapter;
    }

    /**
     * Set the {@link BaseAdapter} associated with this activity's ListView.
     */
    public void setListAdapter(StickyBaseAdapter adapter) {
        mStickyBaseAdapter = adapter;
        getListView().setAdapter(adapter);
        if (restoreScrollPosition && mListState != null) {
            getListView().getWrappedList().onRestoreInstanceState(mListState);
        }
    }

    public void setRestoreScrollPosition(boolean restoreScrollPosition) {
        this.restoreScrollPosition = restoreScrollPosition;
    }
}
