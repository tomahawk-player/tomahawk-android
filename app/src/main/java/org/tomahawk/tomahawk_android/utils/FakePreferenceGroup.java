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
package org.tomahawk.tomahawk_android.utils;

import java.util.ArrayList;

/**
 * A group of several {@link FakePreference}s
 */
public class FakePreferenceGroup {

    public static final int TYPE_CHECKBOX = 1;

    public static final int TYPE_PLAIN = 2;

    public static final int TYPE_SPINNER = 3;

    private final ArrayList<FakePreference> mFakePreferences = new ArrayList<>();

    /**
     * A {@link FakePreference} contains all information needed to provide the {@link
     * org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter} with the necessary values to
     * be displayed inside the Fragment's {@link android.widget.ListView}
     */
    public static class FakePreference {

        // this FakePreference's type (Dialog, Checkbox, Plain or Spinner)
        public int type;

        // the key to identify this FakePreference
        public String id;

        // the key to store preferences with
        public String storageKey;

        public String title;

        // short summary text to describe this FakePreference to the user
        public String summary;
    }

    /**
     * Add a {@link FakePreference} to this {@link FakePreferenceGroup}
     */
    public void addFakePreference(FakePreference fakePreference) {
        mFakePreferences.add(fakePreference);
    }

    /**
     * @return an {@link ArrayList} of all {@link FakePreference}s
     */
    public ArrayList<FakePreference> getFakePreferences() {
        return mFakePreferences;
    }

}
