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
import org.tomahawk.tomahawk_android.R;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 */
public class TomahawkGridAdapter extends TomahawkBaseAdapter {

    private ResourceHolder mGridItemResourceHolder;

    /**
     * Constructs a new {@link TomahawkGridAdapter}
     */
    public TomahawkGridAdapter(Activity activity, List<List<TomahawkListItem>> listArray) {
        mActivity = activity;
        mGridItemResourceHolder = new ResourceHolder();
        mGridItemResourceHolder.resourceId = R.layout.album_art_grid_item;
        mGridItemResourceHolder.imageViewId = R.id.album_art_grid_image;
        mGridItemResourceHolder.textViewId1 = R.id.album_art_grid_textView1;
        mGridItemResourceHolder.textViewId2 = R.id.album_art_grid_textView2;
        mListArray = listArray;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        Object item = getItem(position);

        if (item != null) {
            ViewHolder viewHolder;
            if ((item instanceof TomahawkListItem && convertView == null) || (
                    item instanceof TomahawkListItem && ((ViewHolder) convertView.getTag()).viewType
                            != R.id.tomahawklistadapter_viewtype_griditem)) {
                view = mActivity.getLayoutInflater()
                        .inflate(mGridItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder();
                viewHolder.viewType = R.id.tomahawklistadapter_viewtype_griditem;
                viewHolder.imageViewLeft = (ImageView) view
                        .findViewById(mGridItemResourceHolder.imageViewId);
                viewHolder.textFirstLine = (TextView) view
                        .findViewById(mGridItemResourceHolder.textViewId1);
                viewHolder.textSecondLine = (TextView) view
                        .findViewById(mGridItemResourceHolder.textViewId2);
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }
            if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_griditem
                    && item instanceof TomahawkListItem) {
                viewHolder.textFirstLine.setText(((TomahawkListItem) item).getName());
                viewHolder.textSecondLine.setText(((TomahawkListItem) item).getArtist().getName());
                if (item instanceof Album) {
                    ((Album) item).loadBitmap(mActivity, viewHolder.imageViewLeft);
                } else if (item instanceof Artist) {
                    //                    ((Artist) item).loadBitmap(mContext, viewHolder.imageViewLeft);
                }
            }
        }
        return view;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getCount()
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

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
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

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }
}
