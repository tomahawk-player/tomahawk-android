/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.resolver.models;

import java.util.List;

public class ScriptResolverConfigUiField {

    public static final String TYPE_TEXTVIEW = "textview";

    public static final String TYPE_TEXTFIELD = "textfield";

    public static final String TYPE_CHECKBOX = "checkbox";

    public static final String TYPE_DROPDOWN = "dropdown";

    public String id;

    public String type;

    public String text;

    public String label;

    public String defaultValue;

    public boolean isPassword;

    public List<String> items;

    public ScriptResolverConfigUiField() {
    }
}
