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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import java.util.List;

/**
 * This class is used to populate a {@link android.widget.GridView}. As in the {@link
 * android.widget.GridView} used for displaying all {@link Album}s in the {@link
 * org.tomahawk.libtomahawk.collection.UserCollection}.
 */
public class TomahawkGridAdapter extends BaseAdapter {

    private Context mContext;

    private LayoutInflater mLayoutInflater;

    private List<TomahawkListItem> mListItems;

    /**
     * Constructs a new {@link TomahawkGridAdapter}
     */
    public TomahawkGridAdapter(Context context, LayoutInflater layoutInflater,
            List<TomahawkListItem> listItems) {
        mContext = context;
        mLayoutInflater = layoutInflater;
        mListItems = listItems;
    }

    /**
     * Set the complete list of lists
     */
    public void setListArray(List<TomahawkListItem> listItems) {
        mListItems = listItems;
        notifyDataSetChanged();
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
                view = mLayoutInflater.inflate(R.layout.album_art_grid_item, parent, false);
                viewHolder = new ViewHolder(view, R.id.tomahawklistadapter_viewtype_griditem);
                view.setTag(viewHolder);
            }

            if (viewHolder.getViewType() == R.id.tomahawklistadapter_viewtype_griditem) {
                viewHolder.getTextView1().setText(item.getName());
                viewHolder.getTextView2().setVisibility(View.VISIBLE);
                viewHolder.getTextView2().setText(item.getArtist().getName());
                if (item instanceof Album || item instanceof Artist) {
                    TomahawkUtils.loadImageIntoImageView(mContext,
                            viewHolder.getImageView1(),
                            item.getImage(), Image.getSmallImageSize());
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
        return mListItems.size();
    }

    /**
     * @return item for the given position
     */
    @Override
    public Object getItem(int position) {
        return mListItems.get(position);
    }

    /**
     * Get the id of the item for the given position. (Id is equal to given position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }
}
