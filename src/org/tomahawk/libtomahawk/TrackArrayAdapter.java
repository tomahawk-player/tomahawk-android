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
public class TrackArrayAdapter extends ArrayAdapter<Track> implements Filterable {

    private List<Track> mAllTrackItemsArray;
    private List<Track> mFilteredTrackItemsArray;
    private TrackFilter mTrackFilter;
    private LayoutInflater inflator;
    private int mTextViewResourceIdFirstLine;
    private int mTextViewResourceIdSecondLine;
    private int mResource;

    /**
     * Constructs a new TrackArrayAdapter
     * 
     * @param context
     * @param resource
     * @param textViewResourceIdSecondLine
     * @param objects
     */
    public TrackArrayAdapter(Activity activity, int resource, int textViewResourceIdFirstLine, int textViewResourceIdSecondLine, List<Track> objects) {
        super(activity, resource, textViewResourceIdFirstLine, objects);
        this.mTextViewResourceIdFirstLine=textViewResourceIdFirstLine;
        this.mTextViewResourceIdSecondLine=textViewResourceIdSecondLine;
        this.mResource=resource;
        this.mAllTrackItemsArray = new ArrayList<Track>();
        mAllTrackItemsArray.addAll(objects);
        this.mFilteredTrackItemsArray = new ArrayList<Track>();
        mFilteredTrackItemsArray.addAll(mAllTrackItemsArray);
        inflator = activity.getLayoutInflater();
        getFilter();
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Filterable#getFilter()
     */
    @Override
    public Filter getFilter() {
        if (mTrackFilter == null) {
            mTrackFilter = new TrackFilter();
        }
        return mTrackFilter;
    }

    static class ViewHolder {
        protected TextView textFirstLine;
        protected TextView textSecondLine;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        
        view = inflator.inflate(mResource, null);
        Track track = mFilteredTrackItemsArray.get(position);
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
        viewHolder.textFirstLine.setText(track.getTitle());
        viewHolder.textSecondLine.setText(track.getArtist().getName());

        return view;
    }

    private class TrackFilter extends Filter {
        /* 
                * (non-Javadoc)
                * @see android.widget.Filter#performFiltering(java.lang.CharSequence)
                */
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            FilterResults result = new FilterResults();
            if (constraint != null && constraint.toString().length() > 0) {
                ArrayList<Track> filteredItems = new ArrayList<Track>();

                for (int i = 0, l = mAllTrackItemsArray.size(); i < l; i++) {
                    Track track = mAllTrackItemsArray.get(i);
                    if (track.getAlbum().getName().contains(constraint))
                        filteredItems.add(track);
                }
                result.count = filteredItems.size();
                result.values = filteredItems;
            } else {
                synchronized (this) {
                    result.values = mAllTrackItemsArray;
                    result.count = mAllTrackItemsArray.size();
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
            mFilteredTrackItemsArray = (ArrayList<Track>) results.values;
            notifyDataSetChanged();
            clear();
            for (int i = 0, l = mFilteredTrackItemsArray.size(); i < l; i++)
                add(mFilteredTrackItemsArray.get(i));
            notifyDataSetInvalidated();
        }

    }
}
