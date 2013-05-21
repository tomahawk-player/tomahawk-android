/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.CollectionActivity;
import org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;

import android.app.Activity;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CheckBox;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which represents the "UserCollection" tabview.
 */
public class FakePreferenceFragment extends TomahawkListFragment implements OnItemClickListener {

    private static final String TAG = FakePreferenceFragment.class.getName();

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY = "plugintoplay";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_APPVERSION = "appversion";

    protected CollectionActivity mCollectionActivity;

    private SharedPreferences mSharedPreferences;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());

        List<FakePreferenceGroup> preferenceGroups = new ArrayList<FakePreferenceGroup>();
        FakePreferenceGroup prefGroup = new FakePreferenceGroup(
                getString(R.string.fakepreference_playback_header));
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX,
                FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY,
                getString(R.string.fakepreference_plugintoplay_title_string),
                getString(R.string.fakepreference_plugintoplay_summary_string));
        preferenceGroups.add(prefGroup);
        prefGroup = new FakePreferenceGroup(getString(R.string.fakepreference_info_header));
        String versionName = "";
        try {
            PackageInfo packageInfo = mCollectionActivity.getPackageManager()
                    .getPackageInfo(mCollectionActivity.getPackageName(), 0);
            versionName = packageInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "onViewCreated: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN,
                FAKEPREFERENCEFRAGMENT_KEY_APPVERSION,
                getString(R.string.fakepreference_appversion_title_string), versionName);
        preferenceGroups.add(prefGroup);
        FakePreferencesAdapter fakePreferencesAdapter = new FakePreferencesAdapter(
                mCollectionActivity.getLayoutInflater(), preferenceGroups);
        setListAdapter(fakePreferencesAdapter);
        getListView().setOnItemClickListener(this);
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onAttach(android.app.Activity)
     */
    @Override
    public void onAttach(Activity activity) {
        if (activity instanceof CollectionActivity) {
            mCollectionActivity = (CollectionActivity) activity;
        }
        super.onAttach(activity);
    }

    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockListFragment#onDetach()
     */
    @Override
    public void onDetach() {
        mCollectionActivity = null;
        super.onDetach();
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        idx -= getListView().getHeaderViewsCount();
        if (idx >= 0) {
            if (((FakePreferenceGroup.FakePreference) getListAdapter().getItem(idx)).getType()
                    == FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX) {
                CheckBox checkBox = (CheckBox) arg1.findViewById(R.id.fake_preferences_checkbox);
                checkBox.toggle();
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putBoolean(
                        ((FakePreferenceGroup.FakePreference) getListAdapter().getItem(idx))
                                .getKey(), checkBox.isChecked());
                editor.commit();
            }
        }
    }
}
