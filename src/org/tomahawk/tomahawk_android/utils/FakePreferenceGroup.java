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

    public static final int FAKEPREFERENCE_TYPE_AUTH = 0;

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

        // if this FakePreference's type is FAKEPREFERENCE_TYPE_CHECKBOX or FAKEPREFERENCE_TYPE_AUTH
        // this contains the current state of this preference
        private boolean isEnabled;

        private String title;

        // short summary text to describe this FakePreference to the user
        private String summary;

        // drawable to show in grey, if isEnabled is false, otherwise colored
        private int drawableResId;

        /**
         * Construct a {@link FakePreference}
         */
        private FakePreference(int type, String key, boolean isEnabled, String title,
                String summary, int drawableResId) {
            this.type = type;
            this.key = key;
            this.isEnabled = isEnabled;
            this.title = title;
            this.summary = summary;
            this.drawableResId = drawableResId;
        }

        /**
         * Construct a {@link FakePreference}
         */
        private FakePreference(int type, String key, boolean isEnabled, String title,
                String summary) {
            this.type = type;
            this.key = key;
            this.isEnabled = isEnabled;
            this.title = title;
            this.summary = summary;
        }

        public int getType() {
            return type;
        }

        public String getKey() {
            return key;
        }

        public boolean isEnabled() {
            return isEnabled;
        }

        public void setEnabled(boolean enabled) {
            this.isEnabled = enabled;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }

        public int getDrawableResId() {
            return drawableResId;
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
    public void addFakePreference(int type, String key, String title, String summary,
            int drawableResId) {
        mFakePreferences.add(new FakePreference(type, key, false, title, summary, drawableResId));
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
            if (fakePreference.getKey().equals(key)) {
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
