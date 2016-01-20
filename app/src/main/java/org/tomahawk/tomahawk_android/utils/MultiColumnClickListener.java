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
package org.tomahawk.tomahawk_android.utils;

import android.view.View;

public interface MultiColumnClickListener {

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param view the clicked view
     * @param item the Object which corresponds to the click
     */
    void onItemClick(View view, Object item);

    /**
     * Called every time an item inside a ListView or GridView is long-clicked
     *
     * @param view the clicked view
     * @param item the Object which corresponds to the long-click
     */
    boolean onItemLongClick(View view, Object item);
}