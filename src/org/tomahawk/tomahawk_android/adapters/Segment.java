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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.content.res.Resources;
import android.widget.AdapterView;

import java.util.ArrayList;
import java.util.List;

public class Segment {

    private int mColumnCount = 1;

    private int mHorizontalPadding;

    private int mVerticalPadding;

    private int mHeaderLayoutId;

    private AdapterView.OnItemSelectedListener mSpinnerClickListener;

    private int mInitialPos;

    private final List<String> mHeaderStrings = new ArrayList<>();

    private List mListItems = new ArrayList<>();

    private boolean mShowAsQueued;

    private int mNumerationCorrection;

    private boolean mShowDuration;

    private boolean mShowNumeration;

    private boolean mHideArtistName;

    private int mLeftExtraPadding;

    public Segment(List listItems, int headerLayoutId) {
        this(listItems);
        mHeaderLayoutId = headerLayoutId;
    }

    public Segment(List listItems) {
        mListItems = listItems;
    }

    public Segment(int headerStringResId, List listItems) {
        this(TomahawkApp.getContext().getString(headerStringResId), listItems);
    }

    public Segment(String headerString, List listItems) {
        this(listItems);
        mHeaderStrings.add(headerString);
        mHeaderLayoutId = R.layout.single_line_list_header;
    }

    public Segment(String headerString, List listItems, int headerLayoutId) {
        this(listItems);
        mHeaderStrings.add(headerString);
        mHeaderLayoutId = headerLayoutId;
    }

    public Segment(int initialPos, List<Integer> headerStringResIds,
            AdapterView.OnItemSelectedListener spinnerClickListener,
            List listItems) {
        this(listItems);
        mInitialPos = initialPos;
        for (Integer resId : headerStringResIds) {
            mHeaderStrings.add(TomahawkApp.getContext().getString(resId));
        }
        mHeaderLayoutId = R.layout.dropdown_header;
        mSpinnerClickListener = spinnerClickListener;
    }

    public Segment(List listItems, int columnCountResId,
            int horizontalPaddingResId, int verticalPaddingResId, int headerLayoutId) {
        this(listItems, columnCountResId, horizontalPaddingResId, verticalPaddingResId);
        mHeaderLayoutId = headerLayoutId;
    }


    public Segment(List listItems, int columnCountResId,
            int horizontalPaddingResId, int verticalPaddingResId) {
        this(listItems);
        Resources resources = TomahawkApp.getContext().getResources();
        mHorizontalPadding = resources.getDimensionPixelSize(horizontalPaddingResId);
        mVerticalPadding = resources.getDimensionPixelSize(verticalPaddingResId);
        mColumnCount = resources.getInteger(columnCountResId);
    }

    public Segment(int headerStringResId, List listItems, int columnCountResId,
            int horizontalPaddingResId, int verticalPaddingResId) {
        this(TomahawkApp.getContext().getString(headerStringResId), listItems, columnCountResId,
                horizontalPaddingResId, verticalPaddingResId);
    }

    public Segment(String headerString, List listItems, int columnCountResId,
            int horizontalPaddingResId, int verticalPaddingResId) {
        this(listItems, columnCountResId, horizontalPaddingResId, verticalPaddingResId);
        mHeaderStrings.add(headerString);
        mHeaderLayoutId = R.layout.single_line_list_header;
    }

    public Segment(int initialPos, List<Integer> headerStringResIds,
            AdapterView.OnItemSelectedListener spinnerClickListener,
            List listItems, int columnCountResId, int horizontalPaddingResId,
            int verticalPaddingResId) {
        this(listItems, columnCountResId, horizontalPaddingResId, verticalPaddingResId);
        mInitialPos = initialPos;
        for (Integer resId : headerStringResIds) {
            mHeaderStrings.add(TomahawkApp.getContext().getString(resId));
        }
        mHeaderLayoutId = R.layout.dropdown_header;
        mSpinnerClickListener = spinnerClickListener;
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

    public int getHeaderLayoutId() {
        return mHeaderLayoutId;
    }

    public AdapterView.OnItemSelectedListener getSpinnerClickListener() {
        return mSpinnerClickListener;
    }

    public int size() {
        return mListItems.size();
    }

    public Object get(int location) {
        if (mColumnCount > 1) {
            List<Object> list = new ArrayList<>();
            for (int i = location * mColumnCount; i < location * mColumnCount + mColumnCount; i++) {
                Object item = null;
                if (i < mListItems.size()) {
                    item = mListItems.get(i);
                }
                list.add(item);
            }
            return list;
        } else {
            return mListItems.get(location);
        }
    }

    public Object getFirstSegmentItem() {
        Object result = get(0);
        if (result instanceof List) {
            return ((List) get(0)).get(0);
        } else {
            return result;
        }
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

    public boolean isShowDuration() {
        return mShowDuration;
    }

    public void setShowDuration(boolean showDuration) {
        mShowDuration = showDuration;
    }

    public boolean isShowNumeration() {
        return mShowNumeration;
    }

    public int getNumerationCorrection() {
        return mNumerationCorrection;
    }

    public void setShowNumeration(boolean showNumeration, int numerationCorrection) {
        mShowNumeration = showNumeration;
        mNumerationCorrection = numerationCorrection;
    }

    public boolean isHideArtistName() {
        return mHideArtistName;
    }

    public void setHideArtistName(boolean hideArtistName) {
        mHideArtistName = hideArtistName;
    }

    public int getLeftExtraPadding() {
        return mLeftExtraPadding;
    }

    public void setLeftExtraPadding(int leftExtraPadding) {
        mLeftExtraPadding = leftExtraPadding;
    }
}