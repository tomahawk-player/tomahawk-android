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
package org.tomahawk.libtomahawk;

import java.util.ArrayList;
import java.util.List;

import org.tomahawk.tomahawk_android.R;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 *
 */
public class TomahawkGridAdapter extends TomahawkBaseAdapter {

    private ResourceHolder mGridItemResourceHolder;

    /**
     * Constructs a new {@link TomahawkGridAdapter}
     * 
     * @param activity the activity, which uses the {@link TomahawkListAdapter}. Used to get the {@link LayoutInflater}
     * @param resourceGridItem the resource id for the view, that displays the album art image
     * @param imageViewResourceGridItem the resource id for the imageView inside resourceListItem that displays the
     * albumArt bitmap
     * @param textViewResourceGridItemId1 the resource id for the textView inside resourceListItem that displays the
     * first line of text
     * @param textViewResourceGridItemId2 the resource id for the textView inside resourceListItem that displays the
     * second line of text
     * @param list contains a list of TomahawkListItems.
     */
    public TomahawkGridAdapter(Activity activity, int resourceGridItem, int imageViewResourceGridItem,
            int textViewResourceGridItemId1, int textViewResourceGridItemId2, List<TomahawkListItem> list) {
        mActivity = activity;
        setGridItemResources(resourceGridItem, imageViewResourceGridItem, textViewResourceGridItemId1,
                textViewResourceGridItemId2);
        mListArray = new ArrayList<List<TomahawkListItem>>();
        mListArray.add(list);
    }

    public void setGridItemResources(int resourceGridItem, int imageViewResourcesGridItemId, int textViewResourceGridItemId1, int textViewResourceGridItemId2) {
        mGridItemResourceHolder = new ResourceHolder(resourceGridItem, imageViewResourcesGridItemId,
                textViewResourceGridItemId1, textViewResourceGridItemId2);
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
            if ((item instanceof TomahawkListItem && convertView == null)
                    || (item instanceof TomahawkListItem && ((ViewHolder) convertView.getTag()).viewType != R.id.tomahawklistadapter_viewtype_griditem)) {
                view = mActivity.getLayoutInflater().inflate(mGridItemResourceHolder.resourceId, null);
                viewHolder = new ViewHolder(R.id.tomahawklistadapter_viewtype_griditem,
                        (ImageView) view.findViewById(mGridItemResourceHolder.imageViewId),
                        (TextView) view.findViewById(mGridItemResourceHolder.textViewId1),
                        (TextView) view.findViewById(mGridItemResourceHolder.textViewId2));
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (ViewHolder) view.getTag();
            }
            if (viewHolder.viewType == R.id.tomahawklistadapter_viewtype_griditem) {
                viewHolder.textFirstLine.setText(((TomahawkListItem) item).getName());
                viewHolder.textSecondLine.setText(((TomahawkListItem) item).getArtist().getName());
                loadBitmap((TomahawkListItem) item, viewHolder.imageView);
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
        if ((mFiltered ? mFilteredListArray : mListArray) == null)
            return 0;
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
