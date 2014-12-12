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

import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkPagerAdapter;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.FancyDropDown;
import org.tomahawk.tomahawk_android.views.PageIndicator;
import org.tomahawk.tomahawk_android.views.Selector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class PagerFragment extends ContentHeaderFragment {

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    protected ConcurrentSkipListSet<String> mCorrespondingQueryIds
            = new ConcurrentSkipListSet<String>();

    private PagerFragmentReceiver mPagerFragmentReceiver;

    public class FragmentInfoList {

        private List<FragmentInfo> mFragmentInfos;

        private int mCurrent = 0;

        public void addFragmentInfo(FragmentInfo fragmentInfo) {
            if (mFragmentInfos == null) {
                mFragmentInfos = new ArrayList<FragmentInfo>();
            }
            mFragmentInfos.add(fragmentInfo);
        }

        public List<FragmentInfo> getFragmentInfos() {
            return mFragmentInfos;
        }

        public FragmentInfo getCurrentFragmentInfo() {
            return mFragmentInfos.get(mCurrent);
        }

        public void setCurrent(int current) {
            mCurrent = current;
        }

        public int size() {
            return mFragmentInfos.size();
        }
    }

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
            } else if (TomahawkMainActivity.SLIDING_LAYOUT_SHOWN.equals(intent.getAction())) {
                onSlidingLayoutShown();
            } else if (TomahawkMainActivity.SLIDING_LAYOUT_HIDDEN.equals(intent.getAction())) {
                onSlidingLayoutHidden();
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
            intentFilter = new IntentFilter(TomahawkMainActivity.SLIDING_LAYOUT_SHOWN);
            getActivity().registerReceiver(mPagerFragmentReceiver, intentFilter);
            intentFilter = new IntentFilter(TomahawkMainActivity.SLIDING_LAYOUT_HIDDEN);
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
    }

    protected void setupPager(List<FragmentInfoList> fragmentInfoLists, int initialPage,
            String selectorPosStorageKey) {
        List<FragmentInfo> currentFragmentInfos = new ArrayList<FragmentInfo>();
        for (FragmentInfoList list : fragmentInfoLists) {
            currentFragmentInfos.add(list.getCurrentFragmentInfo());
        }
        TomahawkPagerAdapter adapter = new TomahawkPagerAdapter(getChildFragmentManager(),
                currentFragmentInfos, ((Object) this).getClass());
        final ViewPager fragmentPager = (ViewPager) getView().findViewById(R.id.fragmentpager);
        if (initialPage < 0) {
            initialPage = fragmentPager.getCurrentItem();
        }
        fragmentPager.setAdapter(adapter);

        LinearLayout pageIndicatorContainer =
                (LinearLayout) getView().findViewById(R.id.page_indicator_container);
        pageIndicatorContainer.setVisibility(View.VISIBLE);
        PageIndicator pageIndicator =
                (PageIndicator) pageIndicatorContainer.findViewById(R.id.page_indicator);
        pageIndicator.setup(fragmentPager, fragmentInfoLists,
                getActivity().findViewById(R.id.sliding_layout),
                (Selector) getView().findViewById(R.id.selector), selectorPosStorageKey);
        if (initialPage >= 0) {
            fragmentPager.setCurrentItem(initialPage);
        }
        if (((TomahawkMainActivity) getActivity()).getSlidingUpPanelLayout().isPanelHidden()) {
            onSlidingLayoutHidden();
        } else {
            onSlidingLayoutShown();
        }
    }

    protected abstract void onPipeLineResultsReported(String key);

    protected abstract void onInfoSystemResultsReported(String requestId);

    protected void showFancyDropDown(TomahawkListItem item, int initialSelection,
            List<FancyDropDown.DropDownItemInfo> dropDownItemInfos,
            FancyDropDown.DropDownListener dropDownListener) {
        super.showFancyDropDown(
                (FrameLayout) getView().findViewById(R.id.content_header_frame_pager),
                initialSelection, item.getName().toUpperCase(), dropDownItemInfos,
                dropDownListener);
    }

    protected void showContentHeader(Object item) {
        super.showContentHeader(
                (FrameLayout) getView().findViewById(R.id.content_header_image_frame_pager),
                (FrameLayout) getView().findViewById(R.id.content_header_frame_pager), item);
    }

    private void onSlidingLayoutShown() {
        if (getView() != null) {
            Selector selector = (Selector) getView().findViewById(R.id.selector);
            if (selector != null) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) selector
                        .getLayoutParams();
                params.setMargins(0, 0, 0,
                        ((TomahawkMainActivity) getActivity()).getSlidingUpPanelLayout()
                                .getPanelHeight() * -1);
                selector.setLayoutParams(params);
            }
        }
    }

    private void onSlidingLayoutHidden() {
        if (getView() != null) {
            Selector selector = (Selector) getView().findViewById(R.id.selector);
            if (selector != null) {
                FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) selector
                        .getLayoutParams();
                params.setMargins(0, 0, 0, 0);
                selector.setLayoutParams(params);
            }
        }
    }

    protected Bundle getChildFragmentBundle() {
        Bundle bundle = new Bundle();
        if (getArguments().containsKey(CollectionManager.COLLECTION_ID)) {
            bundle.putString(CollectionManager.COLLECTION_ID,
                    getArguments().getString(CollectionManager.COLLECTION_ID));
        }
        if (getArguments().containsKey(MODE)) {
            bundle.putInt(MODE, getArguments().getInt(MODE));
        }
        return bundle;
    }
}
