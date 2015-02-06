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

    private List<String> mHeaderStrings = new ArrayList<String>();

    private List<Object> mListItems = new ArrayList<Object>();

    private List<List<Object>> mGridItems = new ArrayList<List<Object>>();

    private int mSegmentSize = 0;

    private boolean mShowAsQueued;

    public Segment(List<Object> listItems) {
        mListItems = listItems;
        mSegmentSize = mListItems.size();
    }

    public Segment(int headerStringResId, List<Object> listItems) {
        this(TomahawkApp.getContext().getString(headerStringResId), listItems);
    }

    public Segment(String headerString, List<Object> listItems) {
        this(listItems);
        mHeaderStrings.add(headerString);
    }

    public Segment(int initialPos, List<Integer> headerStringResIds,
            AdapterView.OnItemSelectedListener spinnerClickListener,
            List<Object> listItems) {
        this(listItems);
        mInitialPos = initialPos;
        for (Integer resId : headerStringResIds) {
            mHeaderStrings.add(TomahawkApp.getContext().getString(resId));
        }
        mSpinnerClickListener = spinnerClickListener;
        mSpinnerSegment = true;
    }

    public Segment(List<Object> listItems, int columnCountResId,
            int horizontalPaddingResId, int verticalPaddingResId) {
        mShowAsGrid = true;
        Resources resources = TomahawkApp.getContext().getResources();
        mHorizontalPadding = resources.getDimensionPixelSize(horizontalPaddingResId);
        mVerticalPadding = resources.getDimensionPixelSize(verticalPaddingResId);
        int columnCount = resources.getInteger(columnCountResId);
        for (int i = 0; i < listItems.size(); i += columnCount) {
            List<Object> row = new ArrayList<Object>();
            for (int j = 0; j < columnCount; j++) {
                if (i + j < listItems.size()) {
                    row.add(listItems.get(i + j));
                } else {
                    row.add(null);
                }
            }
            mGridItems.add(row);
        }
        mSegmentSize += listItems.size();
    }

    public Segment(int headerStringResId, List<Object> listItems, int columnCountResId,
            int horizontalPaddingResId, int verticalPaddingResId) {
        this(TomahawkApp.getContext().getString(headerStringResId), listItems, columnCountResId,
                horizontalPaddingResId, verticalPaddingResId);
    }

    public Segment(String headerString, List<Object> listItems, int columnCountResId,
            int horizontalPaddingResId, int verticalPaddingResId) {
        this(listItems, columnCountResId, horizontalPaddingResId, verticalPaddingResId);
        mHeaderStrings.add(headerString);
    }

    public Segment(int initialPos, List<Integer> headerStringResIds,
            AdapterView.OnItemSelectedListener spinnerClickListener,
            List<Object> listItems, int columnCountResId, int horizontalPaddingResId,
            int verticalPaddingResId) {
        this(listItems, columnCountResId, horizontalPaddingResId, verticalPaddingResId);
        mInitialPos = initialPos;
        for (Integer resId : headerStringResIds) {
            mHeaderStrings.add(TomahawkApp.getContext().getString(resId));
        }
        mSpinnerClickListener = spinnerClickListener;
        mSpinnerSegment = true;
    }

    public int getInitialPos() {
        return mInitialPos;
    }

    public String getHeaderString() {
        if (mHeaderStrings.isEmpty()) {
            return null;
        }
        return mHeaderStrings.get(0);
    }

    public List<String> getHeaderStrings() {
        return mHeaderStrings;
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

    public int segmentSize() {
        return mSegmentSize;
    }

    public Object get(int location) {
        return mShowAsGrid ? mGridItems.get(location) : mListItems.get(location);
    }

    public Object getFirstSegmentItem() {
        return mShowAsGrid ? mGridItems.get(0).get(0) : mListItems.get(0);
    }

    public int getHorizontalPadding() {
        return mHorizontalPadding;
    }

    public int getVerticalPadding() {
        return mVerticalPadding;
    }

    public boolean isShowAsQueued() {
        return mShowAsQueued;
    }

    public void setShowAsQueued(boolean showAsQueued) {
        mShowAsQueued = showAsQueued;
    }
}