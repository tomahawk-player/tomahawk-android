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

import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;

import org.tomahawk.tomahawk_android.R;

import android.app.Activity;
import android.content.res.TypedArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 */
public class TomahawkMenuAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    protected final Activity mActivity;

    private List<String> mStringArray = new ArrayList<String>();

    private List<Integer> mIconArray = new ArrayList<Integer>();

    /**
     * Constructs a new {@link TomahawkMenuAdapter}
     */
    public TomahawkMenuAdapter(Activity activity, String[] stringArray, TypedArray iconArray) {
        mActivity = activity;
        Collections.addAll(mStringArray, stringArray);
        for (int i = 0; i < iconArray.length(); i++) {
            mIconArray.add(iconArray.getResourceId(i, 0));
        }
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount() {
        return mStringArray.size();
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        return mStringArray.get(position);
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = mActivity.getLayoutInflater().inflate(R.layout.single_line_list_menu, null);
        TextView textView = (TextView) view.findViewById(R.id.single_line_list_menu_textview);
        ImageView imageView = (ImageView) view.findViewById(R.id.icon_menu_imageview);
        String string = mStringArray.get(position);
        Integer icon = mIconArray.get(position);
        if (string != null) {
            textView.setText(string);
        }
        if (icon != null) {
            imageView.setBackgroundResource(icon);
        }
        return view;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        return new View(mActivity);
    }

    @Override
    public long getHeaderId(int position) {
        return 0;
    }
}
