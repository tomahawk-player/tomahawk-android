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

import org.tomahawk.tomahawk_android.R;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Since we can't customize the appearance of the official context menu dialog, we have to {@link
 * org.tomahawk.tomahawk_android.dialogs.FakeContextMenuDialog} with this {@link
 * TomahawkContextMenuAdapter} to be used to populate it.
 */
public class TomahawkContextMenuAdapter extends BaseAdapter {

    protected final LayoutInflater mLayoutInflater;

    private List<String> mStringArray = new ArrayList<String>();

    /**
     * Constructs a new {@link TomahawkContextMenuAdapter}
     *
     * @param layoutInflater used to inflate the {@link View}s
     * @param stringArray    the array of {@link String}s containing the context menu entry texts
     */
    public TomahawkContextMenuAdapter(LayoutInflater layoutInflater, String[] stringArray) {
        mLayoutInflater = layoutInflater;
        Collections.addAll(mStringArray, stringArray);
    }

    /**
     * @return length of the array of {@link String}s containing the context menu entry texts
     */
    @Override
    public int getCount() {
        return mStringArray.size();
    }

    /**
     * @return {@link String} for the given position
     */
    @Override
    public Object getItem(int position) {
        return mStringArray.get(position);
    }

    /**
     * Get the id of the item for the given position. (Id is equal to given position)
     */
    @Override
    public long getItemId(int position) {
        return position;
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
        View view = mLayoutInflater.inflate(R.layout.single_line_list_context_menu, null);
        TextView textView = (TextView) view
                .findViewById(R.id.single_line_list_context_menu_textview);
        String string = mStringArray.get(position);
        if (string != null) {
            textView.setText(string);
        }
        return view;
    }

}
