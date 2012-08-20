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
public class ArtistArrayAdapter extends ArrayAdapter<Artist> implements Filterable {

    private List<Artist> mAllArtistItemsArray;
    private List<Artist> mFilteredArtistItemsArray;
    private ArtistFilter mArtistFilter;
    private LayoutInflater inflator;
    private int mTextViewResourceId;
    private int mResource;

    /**
     * Constructs a new ArtistArrayAdapter
     * 
     * @param context
     * @param resource
     * @param textViewResourceId
     * @param objects
     */
    public ArtistArrayAdapter(Activity activity, int resource, int textViewResourceId, List<Artist> objects) {
        super(activity, resource, textViewResourceId, objects);
        this.mTextViewResourceId=textViewResourceId;
        this.mResource=resource;
        this.mAllArtistItemsArray = new ArrayList<Artist>();
        mAllArtistItemsArray.addAll(objects);
        this.mFilteredArtistItemsArray = new ArrayList<Artist>();
        mFilteredArtistItemsArray.addAll(mAllArtistItemsArray);
        inflator = activity.getLayoutInflater();
        getFilter();
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Filterable#getFilter()
     */
    @Override
    public Filter getFilter() {
        if (mArtistFilter == null) {
            mArtistFilter = new ArtistFilter();
        }
        return mArtistFilter;
    }

    static class ViewHolder {
        protected TextView text;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        
        view = inflator.inflate(mResource, null);
        Artist artist = mFilteredArtistItemsArray.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            viewHolder.text = (TextView) view.findViewById(mTextViewResourceId);
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = ((ViewHolder) view.getTag());
        }
        viewHolder.text.setText(artist.getName());

        return view;
    }

    private class ArtistFilter extends Filter {
        /* 
                * (non-Javadoc)
                * @see android.widget.Filter#performFiltering(java.lang.CharSequence)
                */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults result = new FilterResults();
            if (constraint != null && constraint.toString().length() > 0) {
                ArrayList<Artist> filteredItems = new ArrayList<Artist>();

                for (int i = 0, l = mAllArtistItemsArray.size(); i < l; i++) {
                    Artist artist = mAllArtistItemsArray.get(i);
                    if (artist.getName().contains(constraint))
                        filteredItems.add(artist);
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            } else {
                synchronized (this) {
                    result.values = mAllArtistItemsArray;
                    result.count = mAllArtistItemsArray.size();
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
            mFilteredArtistItemsArray = (ArrayList<Artist>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0, l = mFilteredArtistItemsArray.size(); i < l; i++)
                add(mFilteredArtistItemsArray.get(i));
            notifyDataSetInvalidated();
        }

    }
}
