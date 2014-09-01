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
package org.tomahawk.tomahawk_android.adapters;

import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.res.Resources;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

public class Segment {

    private boolean mShowAsGrid;

    private int mHorizontalPadding;

    private int mVerticalPadding;

    private boolean mSpinnerSegment;

    private AdapterView.OnItemSelectedListener mSpinnerClickListener;

    private int mInitialPos;

    private List<Integer> mHeaderStringResId = new ArrayList<Integer>();

    private List<TomahawkListItem> mListItems = new ArrayList<TomahawkListItem>();

    private List<List<TomahawkListItem>> mGridItems = new ArrayList<List<TomahawkListItem>>();

    public Segment(List<TomahawkListItem> listItems) {
        mListItems = listItems;
    }

    public Segment(int headerStringResId, List<TomahawkListItem> listItems) {
        this(listItems);
        mHeaderStringResId.add(headerStringResId);
    }

    public Segment(int initialPos, List<Integer> headerStringResIds,
            AdapterView.OnItemSelectedListener spinnerClickListener,
            List<TomahawkListItem> listItems) {
        this(listItems);
        mInitialPos = initialPos;
        mHeaderStringResId.addAll(headerStringResIds);
        mSpinnerClickListener = spinnerClickListener;
        mSpinnerSegment = true;
    }

    public Segment(List<TomahawkListItem> listItems, int columnCount, int horizontalPaddingResId,
            int verticalPaddingResId) {
        mShowAsGrid = true;
        Resources resources = TomahawkApp.getContext().getResources();
        mHorizontalPadding = resources.getDimensionPixelSize(horizontalPaddingResId);
        mVerticalPadding = resources.getDimensionPixelSize(verticalPaddingResId);
        for (int i = 0; i < listItems.size(); i += columnCount) {
            List<TomahawkListItem> row = new ArrayList<TomahawkListItem>();
            for (int j = 0; j < columnCount; j++) {
                if (i + j < listItems.size()) {
                    row.add(listItems.get(i + j));
                } else {
                    row.add(null);
                }
            }
            mGridItems.add(row);
        }
    }

    public Segment(int headerStringResId, List<TomahawkListItem> listItems, int columnCount,
            int horizontalPaddingResId, int verticalPaddingResId) {
        this(listItems, columnCount, horizontalPaddingResId, verticalPaddingResId);
        mHeaderStringResId.add(headerStringResId);
    }

    public Segment(int initialPos, List<Integer> headerStringResIds,
            AdapterView.OnItemSelectedListener spinnerClickListener,
            List<TomahawkListItem> listItems, int columnCount, int horizontalPaddingResId,
            int verticalPaddingResId) {
        this(listItems, columnCount, horizontalPaddingResId, verticalPaddingResId);
        mInitialPos = initialPos;
        mHeaderStringResId.addAll(headerStringResIds);
        mSpinnerClickListener = spinnerClickListener;
        mSpinnerSegment = true;
    }

    public int getInitialPos() {
        return mInitialPos;
    }

    public int getHeaderStringResId() {
        if (mHeaderStringResId.isEmpty()) {
            return 0;
        }
        return mHeaderStringResId.get(0);
    }

    public List<Integer> getHeaderStringResIds() {
        return mHeaderStringResId;
    }

    public boolean isSpinnerSegment() {
        return mSpinnerSegment;
    }

    public AdapterView.OnItemSelectedListener getSpinnerClickListener() {
        return mSpinnerClickListener;
    }

    public int size() {
        return mShowAsGrid ? mGridItems.size() : mListItems.size();
    }

    public Object get(int location) {
        return mShowAsGrid ? mGridItems.get(location) : mListItems.get(location);
    }

    public int getHorizontalPadding() {
        return mHorizontalPadding;
    }

    public int getVerticalPadding() {
        return mVerticalPadding;
    }
}