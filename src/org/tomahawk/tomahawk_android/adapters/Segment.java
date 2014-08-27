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

import java.util.ArrayList;
import java.util.List;

public class Segment {

    private boolean mShowAsGrid;

    private int mHorizontalPadding;

    private int mVerticalPadding;

    private int mHeaderStringResId = -1;

    private List<TomahawkListItem> mListItems = new ArrayList<TomahawkListItem>();

    private List<List<TomahawkListItem>> mGridItems = new ArrayList<List<TomahawkListItem>>();

    public Segment(List<TomahawkListItem> listItems) {
        mListItems = listItems;
    }

    public Segment(int headerStringResId, List<TomahawkListItem> listItems) {
        this(listItems);
        mHeaderStringResId = headerStringResId;
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
        mHeaderStringResId = headerStringResId;
    }

    public int getHeaderStringResId() {
        return mHeaderStringResId;
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