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

import org.tomahawk.libtomahawk.collection.CollectionCursor;
import org.tomahawk.libtomahawk.collection.Playlist;
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

    private CollectionCursor mCollectionCursor;

    private Playlist mPlaylist;

    private int mOffset;

    private boolean mShowAsQueued;

    private int mNumerationCorrection;

    private boolean mShowDuration;

    private boolean mShowNumeration;

    private boolean mHideArtistName;

    private int mLeftExtraPadding;

    public static class Builder {

        private Segment mSegment;

        public Builder(CollectionCursor collectionCursor) {
            mSegment = new Segment();
            mSegment.mCollectionCursor = collectionCursor;
        }

        public Builder(List listItems) {
            mSegment = new Segment();
            mSegment.mListItems = listItems;
        }

        public Builder(Playlist playlist) {
            mSegment = new Segment();
            mSegment.mPlaylist = playlist;
        }

        public Builder headerLayout(int headerLayoutId) {
            mSegment.mHeaderLayoutId = headerLayoutId;
            return this;
        }

        public Builder headerString(int headerStringResId) {
            mSegment.mHeaderStrings.add(TomahawkApp.getContext().getString(headerStringResId));
            return this;
        }

        public Builder headerString(String headerString) {
            mSegment.mHeaderStrings.add(headerString);
            return this;
        }

        public Builder headerStrings(List<Integer> headerStringResIds) {
            for (Integer resId : headerStringResIds) {
                mSegment.mHeaderStrings.add(TomahawkApp.getContext().getString(resId));
            }
            return this;
        }

        public Builder spinner(AdapterView.OnItemSelectedListener spinnerClickListener,
                int initialPos) {
            mSegment.mSpinnerClickListener = spinnerClickListener;
            mSegment.mInitialPos = initialPos;
            return this;
        }

        public Builder showAsGrid(int columnCountResId, int horizontalPaddingResId,
                int verticalPaddingResId) {
            Resources resources = TomahawkApp.getContext().getResources();
            mSegment.mColumnCount = resources.getInteger(columnCountResId);
            mSegment.mHorizontalPadding = resources.getDimensionPixelSize(horizontalPaddingResId);
            mSegment.mVerticalPadding = resources.getDimensionPixelSize(verticalPaddingResId);
            return this;
        }

        public Builder offset(int offset) {
            mSegment.mOffset = offset;
            return this;
        }

        public Segment build() {
            return mSegment;
        }

    }

    private Segment() {
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

    public int getCount() {
        if (mCollectionCursor != null) {
            return mCollectionCursor.size() - mOffset;
        } else if (mPlaylist != null) {
            return mPlaylist.size() - mOffset;
        } else {
            return mListItems.size() - mOffset;
        }
    }

    public int getRowCount() {
        return (int) Math.ceil((float) getCount() / mColumnCount);
    }

    public Object get(int location) {
        location = location + mOffset;
        if (mColumnCount > 1) {
            List<Object> list = new ArrayList<>();
            for (int i = location * mColumnCount; i < location * mColumnCount + mColumnCount; i++) {
                Object item = null;
                if (mCollectionCursor != null && i < mCollectionCursor.size()) {
                    item = mCollectionCursor.get(i);
                } else if (mPlaylist != null) {
                    item = mPlaylist.getEntryAtPos(i);
                } else if (i < mListItems.size()) {
                    item = mListItems.get(i);
                }
                list.add(item);
            }
            return list;
        } else {
            Object item;
            if (mCollectionCursor != null) {
                item = mCollectionCursor.get(location);
            } else if (mPlaylist != null) {
                item = mPlaylist.getEntryAtPos(location);
            } else {
                item = mListItems.get(location);
            }
            return item;
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

    public void close() {
        if (mCollectionCursor != null) {
            mCollectionCursor.close();
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