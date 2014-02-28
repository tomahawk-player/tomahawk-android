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

import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter;
import org.tomahawk.tomahawk_android.dialogs.LoginDialog;
import org.tomahawk.tomahawk_android.services.TomahawkService;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;

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
 * {@link TomahawkListFragment} which fakes the standard {@link android.preference.PreferenceFragment}
 * behaviour. We need to fake it, because the official support library doesn't provide a {@link
 * android.preference.PreferenceFragment} class
 */
public class FakePreferenceFragment extends TomahawkListFragment
        implements OnItemClickListener, TomahawkService.OnAuthenticatedListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = FakePreferenceFragment.class.getName();

    public static final String FAKEPREFERENCEFRAGMENT_KEY_HATCHETLOGGEDIN
            = "org.tomahawk.tomahawk_android.hatchetloggedin";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_SPOTIFYLOGGEDIN
            = "org.tomahawk.tomahawk_android.spotifyloggedin";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PREFBITRATE
            = "org.tomahawk.tomahawk_android.prefbitrate";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY
            = "org.tomahawk.tomahawk_android.plugintoplay";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_APPVERSION
            = "org.tomahawk.tomahawk_android.appversion";

    private SharedPreferences mSharedPreferences;

    private List<FakePreferenceGroup> mFakePreferenceGroups;

    private FakePreferenceFragmentReceiver mFakePreferenceFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class FakePreferenceFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TomahawkApp.TOMAHAWKSERVICE_READY.equals(intent.getAction())) {
                mTomahawkApp.getTomahawkService()
                        .setOnAuthenticatedListener(FakePreferenceFragment.this);
                updateLogInOutState();
            }
        }
    }

    /**
     * Called, when this {@link FakePreferenceFragment}'s {@link View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fetch our SharedPreferences from the PreferenceManager
        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        if (mTomahawkApp.getTomahawkService() != null) {
            mTomahawkApp.getTomahawkService().setOnAuthenticatedListener(this);
        }

        // Set up the set of FakePreferences to be shown in this Fragment
        mFakePreferenceGroups = new ArrayList<FakePreferenceGroup>();
        FakePreferenceGroup prefGroup = new FakePreferenceGroup(
                getString(R.string.fakepreference_accounts_header));
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_DIALOG,
                FAKEPREFERENCEFRAGMENT_KEY_SPOTIFYLOGGEDIN,
                getString(R.string.fakepreference_spotifylogin_title_string),
                getString(R.string.fakepreference_spotifylogin_summary_string));
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_DIALOG,
                FAKEPREFERENCEFRAGMENT_KEY_HATCHETLOGGEDIN,
                getString(R.string.fakepreference_hatchetlogin_title_string),
                getString(R.string.fakepreference_hatchetlogin_summary_string));
        mFakePreferenceGroups.add(prefGroup);
        prefGroup = new FakePreferenceGroup(getString(R.string.fakepreference_playback_header));
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX,
                FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY,
                getString(R.string.fakepreference_plugintoplay_title_string),
                getString(R.string.fakepreference_plugintoplay_summary_string));
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_SPINNER,
                FAKEPREFERENCEFRAGMENT_KEY_PREFBITRATE,
                getString(R.string.fakepreference_bitrate_title_string),
                getString(R.string.fakepreference_bitrate_summary_string));
        mFakePreferenceGroups.add(prefGroup);
        prefGroup = new FakePreferenceGroup(getString(R.string.fakepreference_info_header));
        String versionName = "";
        try {
            if (mTomahawkMainActivity.getPackageManager() != null) {
                PackageInfo packageInfo = mTomahawkMainActivity.getPackageManager()
                        .getPackageInfo(mTomahawkMainActivity.getPackageName(), 0);
                versionName = packageInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "onViewCreated: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        prefGroup.addFakePreference(FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN,
                FAKEPREFERENCEFRAGMENT_KEY_APPVERSION,
                getString(R.string.fakepreference_appversion_title_string), versionName);
        mFakePreferenceGroups.add(prefGroup);

        // Now we can push the complete set of FakePreferences into our FakePreferencesAdapter,
        // so that it can provide our ListView with the correct Views.
        FakePreferencesAdapter fakePreferencesAdapter = new FakePreferencesAdapter(
                mTomahawkMainActivity.getLayoutInflater(), mFakePreferenceGroups);
        setListAdapter(fakePreferencesAdapter);

        getListView().setOnItemClickListener(this);

        updateLogInOutState();
    }

    /**
     * Initialize and register {@link org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment.FakePreferenceFragmentReceiver}
     */
    @Override
    public void onResume() {
        super.onResume();

        mTomahawkMainActivity.setTitle(getString(R.string.fakepreferencefragment_title_string));

        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkService.AUTHENTICATOR_ID)) {
                int authenticatorId = Integer.valueOf(
                        getArguments().getString(TomahawkService.AUTHENTICATOR_ID));
                if (authenticatorId == TomahawkService.AUTHENTICATOR_ID_HATCHET
                        || authenticatorId == TomahawkService.AUTHENTICATOR_ID_SPOTIFY) {
                    new LoginDialog(mTomahawkApp.getTomahawkService(), authenticatorId)
                            .show(getFragmentManager(), null);
                }
            }
        }

        mFakePreferenceFragmentReceiver = new FakePreferenceFragmentReceiver();
        mTomahawkMainActivity.registerReceiver(mFakePreferenceFragmentReceiver,
                new IntentFilter(TomahawkApp.TOMAHAWKSERVICE_READY));
    }

    /**
     * Unregister {@link org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment.FakePreferenceFragmentReceiver}
     * and delete reference
     */
    @Override
    public void onPause() {
        super.onPause();

        mTomahawkMainActivity.unregisterReceiver(mFakePreferenceFragmentReceiver);
        mFakePreferenceFragmentReceiver = null;
    }

    /**
     * Called every time an item inside the {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
     * is clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view
     *                 provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            if (((FakePreferenceGroup.FakePreference) getListAdapter().getItem(position)).getType()
                    == FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX) {
                // if a FakePreference of type "FAKEPREFERENCE_TYPE_CHECKBOX" has been clicked,
                // we edit the associated SharedPreference and toggle its boolean value
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                boolean preferenceState = mSharedPreferences.getBoolean(
                        ((FakePreferenceGroup.FakePreference) getListAdapter().getItem(position))
                                .getKey(), false);
                editor.putBoolean(
                        ((FakePreferenceGroup.FakePreference) getListAdapter().getItem(position))
                                .getKey(), !preferenceState);
                editor.commit();
            } else if ((mTomahawkApp.getTomahawkService() != null)
                    && ((FakePreferenceGroup.FakePreference) getListAdapter().getItem(position)).
                    getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_DIALOG) {
                // if a FakePreference of type "FAKEPREFERENCE_TYPE_DIALOG" has been clicked,
                // we show a LoginDialog
                if (((FakePreferenceGroup.FakePreference) getListAdapter().getItem(position))
                        .getKey().equals(FAKEPREFERENCEFRAGMENT_KEY_SPOTIFYLOGGEDIN)) {
                    new LoginDialog(mTomahawkApp.getTomahawkService(),
                            TomahawkService.AUTHENTICATOR_ID_SPOTIFY)
                            .show(getFragmentManager(), null);
                } else if (((FakePreferenceGroup.FakePreference) getListAdapter().getItem(position))
                        .getKey().equals(FAKEPREFERENCEFRAGMENT_KEY_HATCHETLOGGEDIN)) {
                    new LoginDialog(mTomahawkApp.getTomahawkService(),
                            TomahawkService.AUTHENTICATOR_ID_HATCHET)
                            .show(getFragmentManager(), null);
                }
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        ((FakePreferencesAdapter) getListAdapter()).notifyDataSetChanged();
    }

    /**
     * Called everytime an account has been logged in or out, so that we can update the
     * corresponding checkbox state
     *
     * @param authenticatorId the id of the {@link org.tomahawk.libtomahawk.resolver.Resolver},
     *                        which account has been logged in/out
     * @param loggedIn        true, if logged in, otherwise false
     */
    @Override
    public void onLoggedInOut(int authenticatorId, boolean loggedIn) {
        for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
            FakePreferenceGroup.FakePreference fakePreference = null;
            if (authenticatorId == TomahawkService.AUTHENTICATOR_ID_SPOTIFY) {
                fakePreference = fakePreferenceGroup.getFakePreferenceByKey(
                        FAKEPREFERENCEFRAGMENT_KEY_SPOTIFYLOGGEDIN);
            } else if (authenticatorId == TomahawkService.AUTHENTICATOR_ID_HATCHET) {
                fakePreference = fakePreferenceGroup.getFakePreferenceByKey(
                        FAKEPREFERENCEFRAGMENT_KEY_HATCHETLOGGEDIN);
            }
            if (fakePreference != null) {
                fakePreference.setCheckboxState(loggedIn);
                break;
            }
        }
        mTomahawkMainActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ((FakePreferencesAdapter) getListAdapter()).notifyDataSetChanged();
            }
        });
    }

    public void updateLogInOutState() {
        // Initialize the state of the FakePreference's checkboxes
        if (mTomahawkApp.getTomahawkService() != null) {
            if (AuthenticatorUtils.isLoggedIn(mTomahawkMainActivity.getApplicationContext(),
                    TomahawkService.AUTHENTICATOR_NAME_SPOTIFY,
                    TomahawkService.AUTH_TOKEN_TYPE_SPOTIFY)) {
                onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_SPOTIFY, true);
            } else {
                onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_SPOTIFY, false);
            }
            if (AuthenticatorUtils.isLoggedIn(mTomahawkMainActivity.getApplicationContext(),
                    TomahawkService.AUTHENTICATOR_NAME_HATCHET,
                    TomahawkService.AUTH_TOKEN_TYPE_HATCHET)) {
                onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_HATCHET, true);
            } else {
                onLoggedInOut(TomahawkService.AUTHENTICATOR_ID_HATCHET, false);
            }
        }
    }
}
