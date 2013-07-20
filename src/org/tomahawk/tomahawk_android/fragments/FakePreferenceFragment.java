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
import org.tomahawk.tomahawk_android.dialogs.LoginDialog;
import org.tomahawk.tomahawk_android.services.TomahawkService;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;
import org.tomahawk.tomahawk_android.utils.OnLoggedInOutListener;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment which represents the "UserCollection" tabview.
 */
public class FakePreferenceFragment extends TomahawkListFragment
        implements OnItemClickListener, OnLoggedInOutListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = FakePreferenceFragment.class.getName();

    public static final String FAKEPREFERENCEFRAGMENT_KEY_SPOTIFYLOGGEDIN = "spotifyloggedin";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY = "plugintoplay";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_APPVERSION = "appversion";

    protected CollectionActivity mCollectionActivity;

    private SharedPreferences mSharedPreferences;

    private List<FakePreferenceGroup> mFakePreferenceGroups;

    private FakePreferenceBroadcastReceiver mFakePreferenceBroadcastReceiver;

    private class FakePreferenceBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            mCollectionActivity.getTomahawkService()
                    .setOnLoggedInOutListener(FakePreferenceFragment.this);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        if (mCollectionActivity.getTomahawkService() != null) {
            mCollectionActivity.getTomahawkService().setOnLoggedInOutListener(this);
        }

        mFakePreferenceGroups = new ArrayList<FakePreferenceGroup>();
        FakePreferenceGroup prefGroup = new FakePreferenceGroup(
                getString(R.string.fakepreference_accounts_header));
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_DIALOG,
                FAKEPREFERENCEFRAGMENT_KEY_SPOTIFYLOGGEDIN,
                getString(R.string.fakepreference_spotifylogin_title_string),
                getString(R.string.fakepreference_spotifylogin_summary_string));
        mFakePreferenceGroups.add(prefGroup);
        prefGroup = new FakePreferenceGroup(getString(R.string.fakepreference_playback_header));
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX,
                FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY,
                getString(R.string.fakepreference_plugintoplay_title_string),
                getString(R.string.fakepreference_plugintoplay_summary_string));
        mFakePreferenceGroups.add(prefGroup);
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
        mFakePreferenceGroups.add(prefGroup);
        FakePreferencesAdapter fakePreferencesAdapter = new FakePreferencesAdapter(
                mCollectionActivity.getLayoutInflater(), mFakePreferenceGroups);
        setListAdapter(fakePreferencesAdapter);
        getListView().setOnItemClickListener(this);

        if (mCollectionActivity.getTomahawkService() != null
                && mCollectionActivity.getTomahawkService().getSpotifyUserId() != null) {
            onLoggedInOut(TomahawkApp.RESOLVER_ID_SPOTIFY, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mFakePreferenceBroadcastReceiver = new FakePreferenceBroadcastReceiver();
        mCollectionActivity.registerReceiver(mFakePreferenceBroadcastReceiver, new IntentFilter(
                CollectionActivity.TOMAHAWKSERVICE_READY));
    }

    @Override
    public void onPause() {
        super.onPause();

        mCollectionActivity.unregisterReceiver(mFakePreferenceBroadcastReceiver);
        mFakePreferenceBroadcastReceiver = null;
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
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                boolean preferenceState = mSharedPreferences.getBoolean(
                        ((FakePreferenceGroup.FakePreference) getListAdapter().getItem(idx))
                                .getKey(), false);
                editor.putBoolean(
                        ((FakePreferenceGroup.FakePreference) getListAdapter().getItem(idx))
                                .getKey(), !preferenceState);
                editor.commit();
            } else if (
                    (mCollectionActivity.getTomahawkService() != null)
                            && ((FakePreferenceGroup.FakePreference) getListAdapter().getItem(idx)).
                            getType()
                            == FakePreferenceGroup.FAKEPREFERENCE_TYPE_DIALOG) {
                new LoginDialog(mCollectionActivity.getTomahawkService())
                        .show(getFragmentManager(), null);
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        getListAdapter().notifyDataSetChanged();
    }

    @Override
    public void onLoggedInOut(int resolverId, boolean loggedIn) {
        if (resolverId == TomahawkApp.RESOLVER_ID_SPOTIFY) {
            for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
                FakePreferenceGroup.FakePreference fakePreference = fakePreferenceGroup
                        .getFakePreferenceByKey(FAKEPREFERENCEFRAGMENT_KEY_SPOTIFYLOGGEDIN);
                if (fakePreference != null) {
                    fakePreference.setLoggedIn(loggedIn);
                    break;
                }
            }
        }
        getListAdapter().notifyDataSetChanged();
    }
}
