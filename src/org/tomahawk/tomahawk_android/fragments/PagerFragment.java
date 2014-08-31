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
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkPagerAdapter;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.PageIndicator;
import org.tomahawk.tomahawk_android.views.TomahawkScrollView;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import java.util.List;

public class PagerFragment extends ContentHeaderFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pagerfragment_layout, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        //Add a spacer to the top of the scrollView
        TomahawkScrollView scrollView =
                (TomahawkScrollView) view.findViewById(R.id.scrollview);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) scrollView.getLayoutParams();
        int offset = getResources().getDimensionPixelSize(R.dimen.header_clear_space);
        int indicatorHeight = getResources().getDimensionPixelSize(R.dimen.pager_indicator_height);
        params.setMargins(0, offset + indicatorHeight, 0, 0);
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
        FrameLayout.LayoutParams params =
                (FrameLayout.LayoutParams) pageIndicatorContainer.getLayoutParams();
        int margin = getResources().getDimensionPixelSize(R.dimen.header_height_large)
                + getResources().getDimensionPixelSize(R.dimen.header_clear_space);
        params.setMargins(0, margin, 0, 0);
        pageIndicatorContainer.setLayoutParams(params);
        pageIndicatorContainer.setVisibility(View.VISIBLE);
        PageIndicator pageIndicator =
                (PageIndicator) pageIndicatorContainer.findViewById(R.id.page_indicator);
        pageIndicator.setViewPager(fragmentPager);
        final TomahawkScrollView scrollView =
                (TomahawkScrollView) getView().findViewById(R.id.scrollview);
        scrollView.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        fragmentPager.setLayoutParams(new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT, scrollView.getHeight()));
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
                            int offset = getResources()
                                    .getDimensionPixelSize(R.dimen.header_height_large);
                            float delta = getView().findViewById(R.id.scrollview).getScrollY();
                            animateContentHeader((int) (delta / offset * 10000f));
                        }
                    }
                });
        if (initialPage >= 0) {
            fragmentPager.setCurrentItem(initialPage);
        }
    }

    protected void showContentHeader(TomahawkListItem item, Collection collection) {
        super.showContentHeader(
                (FrameLayout) getView().findViewById(R.id.content_header_image_frame_pager),
                (FrameLayout) getView().findViewById(R.id.content_header_frame_pager), item,
                collection);
    }

    @Override
    public void onPanelCollapsed() {
    }

    @Override
    public void onPanelExpanded() {
    }
}
