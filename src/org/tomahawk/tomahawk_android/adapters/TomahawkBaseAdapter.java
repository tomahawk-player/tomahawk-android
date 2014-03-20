/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.app.Activity;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * This class implements the basic {@link BaseAdapter} functionality for our listview adapters. It
 * is being extended by {@link TomahawkGridAdapter} and {@link TomahawkListAdapter}.
 */
public abstract class TomahawkBaseAdapter extends BaseAdapter {

    private static final String TAG = TomahawkBaseAdapter.class.getName();

    protected Activity mActivity;

    protected boolean mFiltered = false;

    protected int mHighlightedItemPosition = -1;

    protected boolean mHighlightedItemIsPlaying = false;

    protected List<List<TomahawkListItem>> mListArray;

    protected List<List<TomahawkListItem>> mFilteredListArray;

    /**
     * Add a list to the {@link TomahawkBaseAdapter}.
     *
     * @param title the title of the list, which will be displayed as a header, if the list is not
     *              empty
     * @return the index of the just added list
     */
    public int addList(String title) {
        mListArray.add(new ArrayList<TomahawkListItem>());
        notifyDataSetChanged();
        return mListArray.size() - 1;
    }

    /**
     * Add an item to the list with the given index
     *
     * @param index the index which specifies which list the item should be added to
     * @param item  the item to add
     * @return true if successful, otherwise false
     */
    public boolean addItemToList(int index, TomahawkListItem item) {
        if (hasListWithIndex(index)) {
            mListArray.get(index).add(item);
            notifyDataSetChanged();
            return true;
        }
        return false;
    }

    /**
     * Set the list to "itemList" at position "index"
     */
    public void setListWithIndex(int index, ArrayList<TomahawkListItem> itemList) {
        if (hasListWithIndex(index)) {
            mListArray.set(index, itemList);
        }
        notifyDataSetChanged();
    }

    /**
     * Set the complete list of lists
     */
    public void setListArray(List<List<TomahawkListItem>> listArray) {
        mListArray = listArray;
        notifyDataSetChanged();
    }

    /**
     * test if the list with the given index exists
     *
     * @param index the index of the list
     * @return true if list exists, false otherwise
     */
    public boolean hasListWithIndex(int index) {
        return (mListArray.get(index) != null);
    }

    /**
     * Removes every element from every list there is
     */
    public void clearAllLists() {
        for (List<TomahawkListItem> aMListArray : mListArray) {
            aMListArray.clear();
        }
        notifyDataSetChanged();
    }

    /**
     * set the position of the item, which should be highlighted
     */
    public void setHighlightedItem(int position) {
        mHighlightedItemPosition = position;
        notifyDataSetChanged();
    }

    /**
     * set wether or not the highlighted item should show the play or the pause drawable
     */
    public void setHighlightedItemIsPlaying(boolean highlightedItemIsPlaying) {
        this.mHighlightedItemIsPlaying = highlightedItemIsPlaying;
        notifyDataSetChanged();
    }
}
