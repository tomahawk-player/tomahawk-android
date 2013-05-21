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

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;

import android.app.Activity;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.Filter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
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
     * This interface represents an item displayed in our {@link org.tomahawk.libtomahawk.collection.Collection}
     * list.
     */
    public interface TomahawkListItem {

        /**
         * @return the corresponding name/title
         */
        public String getName();

        /**
         * @return the corresponding {@link org.tomahawk.libtomahawk.collection.Artist}
         */
        public Artist getArtist();

        /**
         * @return the corresponding {@link org.tomahawk.libtomahawk.collection.Album}
         */
        public Album getAlbum();
    }

    /**
     * This {@link ResourceHolder} holds the resources to an entry in the grid/listView
     */
    static class ResourceHolder {

        int resourceId;

        int imageViewId;

        int imageViewId2;

        int checkBoxId;

        int textViewId1;

        int textViewId2;

        int textViewId3;
    }

    /**
     * This {@link ViewHolder} holds the data to an entry in the grid/listView
     */
    static class ViewHolder {

        int viewType;

        ImageView imageViewLeft;

        ImageView imageViewRight;

        CheckBox checkBox;

        TextView textFirstLine;

        TextView textSecondLine;

        TextView textThirdLine;
    }

    /**
     * Add a list to the {@link TomahawkGridAdapter}.
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

    public void setListWithIndex(int index, ArrayList<TomahawkListItem> itemList) {
        if (hasListWithIndex(index)) {
            mListArray.set(index, itemList);
        }
        notifyDataSetChanged();
    }

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
     * @return the {@link Filter}, which allows to filter the items inside the custom {@link
     *         ListView} fed by {@link TomahawkBaseAdapter}
     */
    public Filter getFilter() {
        return new Filter() {
            @SuppressWarnings("unchecked")
            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {
                mFilteredListArray = (List<List<TomahawkListItem>>) results.values;
                TomahawkBaseAdapter.this.notifyDataSetChanged();
            }

            @Override
            protected FilterResults performFiltering(CharSequence constraint) {
                constraint = constraint.toString().toLowerCase();
                constraint = constraint.toString().trim();
                List<List<TomahawkListItem>> filteredResults = getFilteredResults(constraint);

                FilterResults results = new FilterResults();
                synchronized (this) {
                    results.values = filteredResults;
                }

                return results;
            }

            protected List<List<TomahawkListItem>> getFilteredResults(CharSequence constraint) {
                List<List<TomahawkListItem>> filteredResults
                        = new ArrayList<List<TomahawkListItem>>();
                if (constraint == null || constraint.toString().length() <= 1) {
                    return filteredResults;
                }

                for (int i = 0; i < mListArray.size(); i++) {
                    filteredResults.add(new ArrayList<TomahawkListItem>());
                    for (int j = 0; j < mListArray.get(i).size(); j++) {
                        TomahawkListItem item = mListArray.get(i).get(j);
                        if (item.getName().toLowerCase().contains(constraint)) {
                            filteredResults.get(i).add(item);
                        }
                    }
                }
                return filteredResults;
            }
        };
    }

    /**
     * @param filtered true if the list is being filtered, else false
     */
    public void setFiltered(boolean filtered) {
        this.mFiltered = filtered;
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
