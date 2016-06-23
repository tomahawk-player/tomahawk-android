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

import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.StickyBaseAdapter;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;

import de.greenrobot.event.EventBus;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

/**
 * More customizable implementation of {@link android.app.ListFragment}
 */
public abstract class TomahawkListFragment extends ContentHeaderFragment implements
        AbsListView.OnScrollListener {

    private static final String TAG = TomahawkListFragment.class.getSimpleName();

    private StickyBaseAdapter mStickyBaseAdapter;

    private StickyListHeadersListView mList;

    private Parcelable mListState = null;

    private boolean restoreScrollPosition = true;

    private final Handler mHandler = new Handler();

    private final Runnable mRequestFocus = new Runnable() {
        public void run() {
            mList.focusableViewAvailable(mList);
        }
    };

    protected String mContainerFragmentClass;

    /**
     * Get a stored list scroll position, if present
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(TomahawkFragment.LIST_SCROLL_POSITION)) {
                mListState = savedInstanceState.getParcelable(
                        TomahawkFragment.LIST_SCROLL_POSITION);
            }
        }

        if (getArguments() != null) {
            mContainerFragmentClass =
                    getArguments().getString(TomahawkFragment.CONTAINER_FRAGMENT_CLASSNAME);
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
            if (mContainerFragmentPage > -1) {
                getListView().setTag(mContainerFragmentPage);
            }
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

    @Override
    public void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);

        out.putParcelable(TomahawkFragment.LIST_SCROLL_POSITION, mListState);
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount,
            int totalItemCount) {
        updateAnimation();
    }

    private void updateAnimation() {
        int playTime;
        if (getListView() != null && getListView().getFirstVisiblePosition() == 0
                && getListView().getListChildAt(0) != null) {
            FrameLayout.LayoutParams params =
                    (FrameLayout.LayoutParams) getListView().getLayoutParams();
            float delta = getListView().getListChildAt(0).getBottom() - getListView().getTop()
                    + params.topMargin;
            playTime = (int)
                    (10000f - delta / getListView().getListChildAt(0).getHeight() * 10000f);
        } else {
            playTime = 10000;
        }
        if (mContainerFragmentId >= 0) {
            AnimateEvent event = new AnimateEvent();
            event.mContainerFragmentId = mContainerFragmentId;
            event.mContainerFragmentPage = mContainerFragmentPage;
            event.mPlayTime = playTime;
            EventBus.getDefault().post(event);
        } else {
            animate(playTime);
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(RequestSyncEvent event) {
        if (mContainerFragmentId == event.mContainerFragmentId
                && mContainerFragmentPage == event.mPerformerFragmentPage
                && getListView() != null) {
            PerformSyncEvent performSyncEvent = new PerformSyncEvent();
            performSyncEvent.mContainerFragmentId = event.mContainerFragmentId;
            performSyncEvent.mContainerFragmentPage = event.mReceiverFragmentPage;
            performSyncEvent.mFirstVisiblePosition = getListView().getFirstVisiblePosition();
            performSyncEvent.mTop = getListView().getListChildAt(0).getTop();
            EventBus.getDefault().post(performSyncEvent);
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(PerformSyncEvent event) {
        if (mContainerFragmentId == event.mContainerFragmentId
                && mContainerFragmentPage == event.mContainerFragmentPage
                && getListView() != null) {
            if (event.mFirstVisiblePosition == 0) {
                getListView().setSelectionFromTop(0, event.mTop);
            } else if (getListView().getFirstVisiblePosition() == 0) {
                getListView().setSelection(1);
            }
        }
    }

    protected void showContentHeader(Object item) {
        if (mContainerFragmentClass == null) {
            super.showContentHeader(item);
        }
    }

    protected void setupAnimations() {
        if (mContainerFragmentClass == null) {
            super.setupAnimations();
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
        if (root == null) {
            Log.e(TAG, "Couldn't inflate listview! root is null");
            return;
        }
        if (layoutInflater == null) {
            Log.e(TAG, "Couldn't inflate listview! layoutInflater is null");
            return;
        }
        View view = ViewUtils.ensureInflation(root, R.id.listview_stub, R.id.listview);
        if (view instanceof StickyListHeadersListView) {
            mList = (StickyListHeadersListView) view;
        }
        if (mList == null) {
            Log.e(TAG, "Something went wrong, listview is null");
        }
        if (mStickyBaseAdapter != null) {
            setListAdapter(mStickyBaseAdapter);
        }
        mHandler.post(mRequestFocus);
    }

    /**
     * @return the current scrolling position of the list- or gridView
     */
    private Parcelable getListState() {
        if (getListView() != null) {
            return getListView().getWrappedList().onSaveInstanceState();
        }
        return null;
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
        if (getListView() != null) {
            getListView().setAdapter(adapter);
            if (restoreScrollPosition && mListState != null) {
                getListView().getWrappedList().onRestoreInstanceState(mListState);
            }
        }
    }

    public void setRestoreScrollPosition(boolean restoreScrollPosition) {
        this.restoreScrollPosition = restoreScrollPosition;
    }
}
