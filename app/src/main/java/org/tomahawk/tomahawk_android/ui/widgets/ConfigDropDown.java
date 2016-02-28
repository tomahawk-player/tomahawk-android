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
package org.tomahawk.tomahawk_android.ui.widgets;

import android.content.Context;
import android.support.v7.widget.AppCompatSpinner;
import android.util.AttributeSet;

public class ConfigDropDown extends AppCompatSpinner implements ConfigFieldView {

    public String mConfigFieldId;

    public ConfigDropDown(Context context) {
        super(context);
    }

    public ConfigDropDown(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    public Object getValue() {
        return getSelectedItemPosition();
    }

    public String getConfigFieldId() {
        return mConfigFieldId;
    }
}
