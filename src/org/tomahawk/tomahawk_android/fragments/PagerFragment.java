/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
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

import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkPagerAdapter;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.views.PageIndicator;
import org.tomahawk.tomahawk_android.views.TomahawkScrollView;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class PagerFragment extends ContentHeaderFragment {

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    protected ConcurrentSkipListSet<String> mCorrespondingQueryIds
            = new ConcurrentSkipListSet<String>();

    private PagerFragmentReceiver mPagerFragmentReceiver;

    protected boolean mHasScrollableHeader;

    protected int mStaticHeaderHeight = -1;

    /**
     * Handles incoming broadcasts.
     */
    private class PagerFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                String requestId = intent.getStringExtra(
                        InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                if (mCurrentRequestIds.contains(requestId)) {
                    onInfoSystemResultsReported(requestId);
                }
            } else if (PipeLine.PIPELINE_RESULTSREPORTED.equals(intent.getAction())) {
                String queryKey = intent.getStringExtra(PipeLine.PIPELINE_RESULTSREPORTED_QUERYKEY);
                if (mCorrespondingQueryIds.contains(queryKey)) {
                    onPipeLineResultsReported(queryKey);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize and register Receiver
        if (mPagerFragmentReceiver == null) {
            mPagerFragmentReceiver = new PagerFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED);
            getActivity().registerReceiver(mPagerFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            getActivity().registerReceiver(mPagerFragmentReceiver, intentFilter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        for (String queryKey : mCorrespondingQueryIds) {
            if (ThreadManager.getInstance().stop(Query.getQueryByKey(queryKey))) {
                mCorrespondingQueryIds.remove(queryKey);
            }
        }

        if (mPagerFragmentReceiver != null) {
            getActivity().unregisterReceiver(mPagerFragmentReceiver);
            mPagerFragmentReceiver = null;
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pagerfragment_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Initialize and register Receiver
        if (mPagerFragmentReceiver == null) {
            mPagerFragmentReceiver = new PagerFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            getActivity().registerReceiver(mPagerFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(PipeLine.PIPELINE_RESULTSREPORTED);
            getActivity().registerReceiver(mPagerFragmentReceiver, intentFilter);
        }

        //Add a spacer to the top of the scrollView
        TomahawkScrollView scrollView =
                (TomahawkScrollView) view.findViewById(R.id.scrollview);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) scrollView.getLayoutParams();
        int offset;
        if (mHasScrollableHeader) {
            offset = getResources()
                    .getDimensionPixelSize(R.dimen.header_clear_space_nonscrollable_pager);
        } else {
            offset = mStaticHeaderHeight;
        }
        params.setMargins(0, offset, 0, 0);
        scrollView.setLayoutParams(params);
    }

    protected void setupPager(List<String> fragmentClassNames, List<String> fragmentTitles,
            List<Bundle> fragmentBundles, int initialPage) {
        TomahawkPagerAdapter adapter = new TomahawkPagerAdapter(getChildFragmentManager(),
                fragmentClassNames, fragmentTitles, fragmentBundles, ((Object) this).getClass());
        final ViewPager fragmentPager = (ViewPager) getView().findViewById(R.id.fragmentpager);
        fragmentPager.setAdapter(adapter);

        LinearLayout pageIndicatorContainer =
                (LinearLayout) getView().findViewById(R.id.page_indicator_container);
        FrameLayout.LayoutParams indicatorParams =
                (FrameLayout.LayoutParams) pageIndicatorContainer.getLayoutParams();
        int margin;
        if (mHasScrollableHeader) {
            margin = getResources()
                    .getDimensionPixelSize(R.dimen.header_clear_space_nonscrollable_pager)
                    + getResources().getDimensionPixelSize(R.dimen.header_clear_space_scrollable)
                    - getResources().getDimensionPixelSize(R.dimen.pager_indicator_height);
        } else {
            margin = mStaticHeaderHeight
                    - getResources().getDimensionPixelSize(R.dimen.pager_indicator_height);
        }
        indicatorParams.setMargins(0, margin, 0, 0);
        pageIndicatorContainer.setLayoutParams(indicatorParams);
        pageIndicatorContainer.setVisibility(View.VISIBLE);
        PageIndicator pageIndicator =
                (PageIndicator) pageIndicatorContainer.findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(fragmentPager);
        if (mHasScrollableHeader) {
            final TomahawkScrollView scrollView =
                    (TomahawkScrollView) getView().findViewById(R.id.scrollview);
            View pagerClearSpace = scrollView.findViewById(R.id.pager_clear_space);
            pagerClearSpace.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, getResources()
                    .getDimensionPixelSize(R.dimen.header_clear_space_scrollable)));
            scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            fragmentPager.setLayoutParams(new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    scrollView.getHeight()));
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                scrollView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                scrollView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                    });
            scrollView.getViewTreeObserver()
                    .addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
                        @Override
                        public void onScrollChanged() {
                            if (getView() != null) {
                                int offset = getResources().getDimensionPixelSize(
                                        R.dimen.header_clear_space_scrollable);
                                float delta = getView().findViewById(R.id.scrollview).getScrollY();
                                animateContentHeader((int) (delta / offset * 10000f));
                            }
                        }
                    });
        }
        if (initialPage >= 0) {
            fragmentPager.setCurrentItem(initialPage);
        }
    }

    protected abstract void onPipeLineResultsReported(String key);

    protected abstract void onInfoSystemResultsReported(String requestId);

    protected void showContentHeader(Object item, Collection collection, int headerHeightResId) {
        super.showContentHeader(
                (FrameLayout) getView().findViewById(R.id.content_header_image_frame_pager),
                (FrameLayout) getView().findViewById(R.id.content_header_frame_pager), item,
                collection, mHasScrollableHeader, headerHeightResId);
    }

    @Override
    public void onPanelCollapsed() {
    }

    @Override
    public void onPanelExpanded() {
    }
}
