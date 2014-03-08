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
package org.tomahawk.tomahawk_android.dialogs;

import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.app.Activity;
import android.support.v4.app.DialogFragment;

/**
 * Base class for every other used DialogFragment
 */
public class TomahawkDialogFragment extends DialogFragment {

    protected TomahawkMainActivity mTomahawkMainActivity;

    protected TomahawkApp mTomahawkApp;

    protected UserCollection mUserCollection;

    /**
     * Store the reference to the attached {@link android.app.Activity}
     */
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        if (activity instanceof TomahawkMainActivity) {
            mTomahawkMainActivity = (TomahawkMainActivity) activity;
            mTomahawkApp = ((TomahawkApp) mTomahawkMainActivity.getApplication());
            mUserCollection = (UserCollection) mTomahawkApp.getSourceList().getLocalSource()
                    .getCollection();
        }
    }
}
