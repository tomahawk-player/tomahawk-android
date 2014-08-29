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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.TomahawkPagerAdapter;
import org.tomahawk.tomahawk_android.views.TomahawkScrollView;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;

import java.util.List;

public class PagerFragment extends ContentHeaderFragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.pagerfragment_layout, container, false);
    }

    protected void setupPager(List<String> fragmentClassNames, List<String> fragmentTitles,
            List<Bundle> fragmentBundles, int initialPage) {
        TomahawkPagerAdapter adapter = new TomahawkPagerAdapter(getChildFragmentManager(),
                fragmentClassNames, fragmentTitles, fragmentBundles, ((Object) this).getClass());
        final ViewPager fragmentPager = (ViewPager) getView().findViewById(R.id.fragmentpager);
        fragmentPager.setAdapter(adapter);
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
        scrollView.getViewTreeObserver().addOnScrollChangedListener(new ViewTreeObserver.OnScrollChangedListener() {
            @Override
            public void onScrollChanged() {

            }
        });
        if (initialPage >= 0) {
            fragmentPager.setCurrentItem(initialPage);
        }
    }

    @Override
    public void onPanelCollapsed() {
    }

    @Override
    public void onPanelExpanded() {
    }
}
