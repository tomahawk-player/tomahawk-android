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

    public static final int FAKEPREFERENCE_TYPE_DIALOG = 0;

    public static final int FAKEPREFERENCE_TYPE_CHECKBOX = 1;

    public static final int FAKEPREFERENCE_TYPE_PLAIN = 2;

    public static final int FAKEPREFERENCE_TYPE_SPINNER = 3;

    private ArrayList<FakePreference> mFakePreferences = new ArrayList<FakePreference>();

    private String mHeader;

    /**
     * A {@link FakePreference} contains all information needed to provide the {@link
     * org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter} with the necessary values to
     * be displayed inside the {@link org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment}'s
     * {@link android.widget.ListView}
     */
    public static class FakePreference {

        // this FakePreference's type (Dialog, Checkbox, Plain or Spinner)
        private int type;

        // the key to identify this FakePreference
        private String key;

        // if this FakePreference's type is FAKEPREFERENCE_TYPE_CHECKBOX,
        // this contains the current state of the checkbox
        private boolean checkboxState;

        private String title;

        // short summary text to describe this FakePreference to the user
        private String summary;

        /**
         * Construct a {@link FakePreference}
         */
        private FakePreference(int type, String key, boolean checkboxState, String title,
                String summary) {
            this.type = type;
            this.key = key;
            this.checkboxState = checkboxState;
            this.title = title;
            this.summary = summary;
        }

        public int getType() {
            return type;
        }

        public String getKey() {
            return key;
        }

        public boolean isCheckboxState() {
            return checkboxState;
        }

        public void setCheckboxState(boolean checkboxState) {
            this.checkboxState = checkboxState;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }
    }

    /**
     * Construct a {@link FakePreferenceGroup}
     *
     * @param header the header to be shown to the user
     */
    public FakePreferenceGroup(String header) {
        mHeader = header;
    }

    /**
     * Add a {@link FakePreference} to this {@link FakePreferenceGroup}
     */
    public void addFakePreference(int type, String key, String title, String summary) {
        mFakePreferences.add(new FakePreference(type, key, false, title, summary));
    }

    /**
     * @return an {@link ArrayList} of all {@link FakePreference}s
     */
    public ArrayList<FakePreference> getFakePreferences() {
        return mFakePreferences;
    }

    /**
     * Get the {@link FakePreference} with the given key
     */
    public FakePreference getFakePreferenceByKey(String key) {
        for (FakePreference fakePreference : mFakePreferences) {
            if (fakePreference.getKey() == key) {
                return fakePreference;
            }
        }
        return null;
    }

    /**
     * @return the header to be shown to the user
     */
    public String getHeader() {
        return mHeader;
    }

}
