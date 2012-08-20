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

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 *
 */
public class AlbumArrayAdapter extends ArrayAdapter<Album> implements Filterable {

    private List<Album> mAllAlbumItemsArray;
    private List<Album> mFilteredAlbumItemsArray;
    private AlbumFilter mAlbumFilter;
    private LayoutInflater inflator;
    private int mTextViewResourceIdFirstLine;
    private int mTextViewResourceIdSecondLine;
    private int mResource;

    /**
     * Constructs a new AlbumArrayAdapter
     * 
     * @param context
     * @param resource
     * @param textViewResourceIdSecondLine
     * @param objects
     */
    public AlbumArrayAdapter(Activity activity, int resource, int textViewResourceIdFirstLine,
            int textViewResourceIdSecondLine, List<Album> objects) {
        super(activity, resource, textViewResourceIdFirstLine, objects);
        this.mTextViewResourceIdFirstLine = textViewResourceIdFirstLine;
        this.mTextViewResourceIdSecondLine = textViewResourceIdSecondLine;
        this.mResource = resource;
        this.mAllAlbumItemsArray = new ArrayList<Album>();
        mAllAlbumItemsArray.addAll(objects);
        this.mFilteredAlbumItemsArray = new ArrayList<Album>();
        mFilteredAlbumItemsArray.addAll(mAllAlbumItemsArray);
        inflator = activity.getLayoutInflater();
        getFilter();
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Filterable#getFilter()
     */
    @Override
    public Filter getFilter() {
        if (mAlbumFilter == null) {
            mAlbumFilter = new AlbumFilter();
        }
        return mAlbumFilter;
    }

    static class ViewHolder {
        protected TextView textFirstLine;
        protected TextView textSecondLine;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;

        view = inflator.inflate(mResource, null);
        Album album = mFilteredAlbumItemsArray.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            viewHolder.textFirstLine = (TextView) view.findViewById(mTextViewResourceIdFirstLine);
            viewHolder.textSecondLine = (TextView) view.findViewById(mTextViewResourceIdSecondLine);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = ((ViewHolder) view.getTag());
        }
        viewHolder.textFirstLine.setText(album.getName());
        viewHolder.textSecondLine.setText(album.getArtist().getName());

        return view;
    }

    private class AlbumFilter extends Filter {
        /* 
                * (non-Javadoc)
                * @see android.widget.Filter#performFiltering(java.lang.CharSequence)
                */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults result = new FilterResults();
            if (constraint != null && constraint.toString().length() > 0) {
                ArrayList<Album> filteredItems = new ArrayList<Album>();

                for (int i = 0, l = mAllAlbumItemsArray.size(); i < l; i++) {
                    Album album = mAllAlbumItemsArray.get(i);
                    if (album.getArtist().getName().contentEquals(constraint))
                        filteredItems.add(album);
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            } else {
                synchronized (this) {
                    result.values = mAllAlbumItemsArray;
                    result.count = mAllAlbumItemsArray.size();
                }
            }
            return result;
        }

        /* 
         * (non-Javadoc)
         * @see android.widget.Filter#publishResults(java.lang.CharSequence, android.widget.Filter.FilterResults)
         */
        @SuppressWarnings("unchecked")
        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            mFilteredAlbumItemsArray = (ArrayList<Album>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0, l = mFilteredAlbumItemsArray.size(); i < l; i++)
                add(mFilteredAlbumItemsArray.get(i));
            notifyDataSetInvalidated();
        }

    }
}
