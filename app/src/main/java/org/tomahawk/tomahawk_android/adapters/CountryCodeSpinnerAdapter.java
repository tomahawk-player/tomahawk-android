/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

public class CountryCodeSpinnerAdapter extends ArrayAdapter<String> {

    private List<String> mCodeNames;

    /**
     * The resource indicating what views to inflate to display the content of this array adapter in
     * a drop down widget.
     */
    private int mDropDownResource;

    public CountryCodeSpinnerAdapter(Context context, int resource,
            List<String> codes, List<String> codeNames) {
        super(context, resource, codes);

        mCodeNames = codeNames;
    }

    @Override
    public void setDropDownViewResource(@LayoutRes int resource) {
        this.mDropDownResource = resource;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View view;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(mDropDownResource, parent, false);
        } else {
            view = convertView;
        }

        //  If no custom field is assigned, assume the whole resource is a TextView
        TextView textView = (TextView) view;
        String item = mCodeNames.get(position);
        textView.setText(item);

        return view;
    }
}
