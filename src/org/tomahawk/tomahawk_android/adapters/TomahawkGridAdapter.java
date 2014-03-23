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
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * This class is used to populate a {@link android.widget.GridView}. As in the {@link
 * android.widget.GridView} used for displaying all {@link Album}s in the {@link
 * org.tomahawk.libtomahawk.collection.UserCollection}.
 */
public class TomahawkGridAdapter extends TomahawkBaseAdapter {

    private LayoutInflater mLayoutInflater;

    /**
     * Constructs a new {@link TomahawkGridAdapter}
     *
     * @param activity  reference to whatever {@link Activity}
     * @param listArray complete set of lists containing all content which the listview should be
     *                  populated with
     */
    public TomahawkGridAdapter(Activity activity, List<List<TomahawkListItem>> listArray) {
        mActivity = activity;
        mLayoutInflater = mActivity.getLayoutInflater();
        mListArray = listArray;
    }

    /**
     * Get the correct {@link View} for the given position.
     *
     * @param position    The position for which to get the correct {@link View}
     * @param convertView The old {@link View}, which we might be able to recycle
     * @param parent      parental {@link ViewGroup}
     * @return the correct {@link View} for the given position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        TomahawkListItem item = (TomahawkListItem) getItem(position);

        if (item != null) {
            ViewHolder viewHolder;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            } else {
                view = mLayoutInflater.inflate(R.layout.album_art_grid_item, null);
                viewHolder = new ViewHolder(view, R.id.tomahawklistadapter_viewtype_griditem);
                view.setTag(viewHolder);
            }

            if (viewHolder.getViewType() == R.id.tomahawklistadapter_viewtype_griditem) {
                viewHolder.getTextFirstLine().setText(item.getName());
                viewHolder.getTextSecondLine().setVisibility(View.VISIBLE);
                viewHolder.getTextSecondLine().setText(item.getArtist().getName());
                if (item instanceof Album || item instanceof Artist) {
                    TomahawkUtils.loadImageIntoImageView(mActivity, viewHolder.getImageView1(),
                            item.getImage(), Image.IMAGE_SIZE_SMALL);
                }
            }
        }
        return view;
    }

    /**
     * @return the count of every item to display
     */
    @Override
    public int getCount() {
        if ((mFiltered ? mFilteredListArray : mListArray) == null) {
            return 0;
        }
        int displayedListArrayItemsCount = 0;
        for (List<TomahawkListItem> list : (mFiltered ? mFilteredListArray : mListArray)) {
            displayedListArrayItemsCount += list.size();
        }
        return displayedListArrayItemsCount;
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        Object item = null;
        int offsetCounter = 0;
        for (int i = 0; i < (mFiltered ? mFilteredListArray : mListArray).size(); i++) {
            List<TomahawkListItem> list = (mFiltered ? mFilteredListArray : mListArray).get(i);
            if (position - offsetCounter < list.size()) {
                item = list.get(position - offsetCounter);
                break;
            }
            offsetCounter += list.size();
        }
        return item;
    }

    /**
     * Get the id of the item for the given position. (Id is equal to given position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }
}
