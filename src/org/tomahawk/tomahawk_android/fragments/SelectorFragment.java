/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.utils.FragmentInfo;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.ThreadManager;
import org.tomahawk.tomahawk_android.views.Selector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentSkipListSet;

public abstract class SelectorFragment extends Fragment {

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    protected ConcurrentSkipListSet<String> mCorrespondingQueryIds
            = new ConcurrentSkipListSet<String>();

    private PagerFragmentReceiver mPagerFragmentReceiver;

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
        return inflater.inflate(R.layout.selectorfragment_layout, container, false);
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

    protected void setupSelector(final List<FragmentInfo> fragmentInfos, final int initialPage,
            final String selectorPosStorageKey) {
        if (getView() != null) {
            FragmentInfo initalItem = fragmentInfos.get(initialPage);
            final ImageView imageViewHeader = (ImageView) getView()
                    .findViewById(R.id.imageview_header);
            imageViewHeader.setImageResource(initalItem.mIconResId);
            final TextView textViewHeader = (TextView) getView().findViewById(R.id.textview_header);
            textViewHeader.setText(initalItem.mTitle);
            textViewHeader.setVisibility(View.GONE);
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    getFragmentManager(), initalItem.mClass,
                    initalItem.mBundle, R.id.content_frame);

            final View selectorHeader = getView().findViewById(R.id.selectorHeader);
            selectorHeader.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectorHeader.setClickable(false);

                    ImageView arrowTop =
                            (ImageView) getView().findViewById(R.id.arrow_top_header);
                    ImageView arrowBottom =
                            (ImageView) getView().findViewById(R.id.arrow_bottom_header);
                    arrowTop.setVisibility(View.GONE);
                    arrowBottom.setVisibility(View.GONE);
                    textViewHeader.setVisibility(View.VISIBLE);

                    Selector selector = (Selector) getView().findViewById(R.id.selector);
                    Selector.SelectorListener selectorListener = new Selector.SelectorListener() {
                        @Override
                        public void onSelectorItemSelected(int position) {
                            selectorHeader.setClickable(true);

                            ImageView arrowTop = (ImageView) getView()
                                    .findViewById(R.id.arrow_top_header);
                            ImageView arrowBottom = (ImageView) getView()
                                    .findViewById(R.id.arrow_bottom_header);
                            if (arrowTop != null) {
                                arrowTop.setVisibility(View.VISIBLE);
                            }
                            if (arrowBottom != null) {
                                arrowBottom.setVisibility(View.VISIBLE);
                            }

                            FragmentInfo selectedItem = fragmentInfos.get(position);
                            imageViewHeader.setImageResource(selectedItem.mIconResId);
                            textViewHeader.setText(selectedItem.mTitle);
                            textViewHeader.setVisibility(View.GONE);

                            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                                    getFragmentManager(), selectedItem.mClass,
                                    selectedItem.mBundle, R.id.content_frame);
                        }

                        @Override
                        public void onCancel() {
                        }
                    };
                    selector.setup(fragmentInfos, selectorListener,
                            getActivity().findViewById(R.id.sliding_layout), selectorPosStorageKey);
                    selector.showSelectorList();
                }
            });
        }
    }

    protected void onPipeLineResultsReported(String key) {

    }

    protected void onInfoSystemResultsReported(String requestId) {

    }
}
