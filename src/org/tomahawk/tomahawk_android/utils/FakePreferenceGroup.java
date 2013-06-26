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
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 20.05.13
 */
public class FakePreferenceGroup {

    public static final int FAKEPREFERENCE_TYPE_DIALOG = 0;

    public static final int FAKEPREFERENCE_TYPE_CHECKBOX = 1;

    public static final int FAKEPREFERENCE_TYPE_PLAIN = 2;

    private ArrayList<FakePreference> mFakePreferences = new ArrayList<FakePreference>();

    private String mHeader;

    public static class FakePreference {

        private int type;

        private String key;

        private boolean isLoggedIn;

        private String title;

        private String summary;

        private FakePreference(int type, String key, boolean isLoggedIn, String title,
                String summary) {
            this.type = type;
            this.key = key;
            this.isLoggedIn = isLoggedIn;
            this.title = title;
            this.summary = summary;
        }

        public int getType() {
            return type;
        }

        public String getKey() {
            return key;
        }

        public boolean isLoggedIn() {
            return isLoggedIn;
        }

        public void setLoggedIn(boolean loggedIn) {
            isLoggedIn = loggedIn;
        }

        public String getTitle() {
            return title;
        }

        public String getSummary() {
            return summary;
        }
    }

    public FakePreferenceGroup(String header) {
        mHeader = header;
    }

    public void addFakePreference(int type, String key, String title, String summary) {
        mFakePreferences.add(new FakePreference(type, key, false, title, summary));
    }

    public ArrayList<FakePreference> getFakePreferences() {
        return mFakePreferences;
    }

    public FakePreference getFakePreferenceByKey(String key) {
        for (FakePreference fakePreference : mFakePreferences) {
            if (fakePreference.getKey() == key) {
                return fakePreference;
            }
        }
        return null;
    }

    public String getHeader() {
        return mHeader;
    }

}
