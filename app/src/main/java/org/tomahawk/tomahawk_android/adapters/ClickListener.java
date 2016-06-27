/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.tomahawk_android.listeners.MultiColumnClickListener;

import android.view.View;

public class ClickListener implements View.OnClickListener, View.OnLongClickListener {

    private final Object mItem;

    private final Segment mSegment;

    private final MultiColumnClickListener mListener;

    public ClickListener(Object item, Segment segment, MultiColumnClickListener listener) {
        mItem = item;
        mListener = listener;
        mSegment = segment;
    }

    @Override
    public void onClick(View view) {
        mListener.onItemClick(view, mItem, mSegment);
    }

    @Override
    public boolean onLongClick(View view) {
        return mListener.onItemLongClick(view, mItem, mSegment);
    }

    public Object getItem() {
        return mItem;
    }

    public MultiColumnClickListener getListener() {
        return mListener;
    }
}