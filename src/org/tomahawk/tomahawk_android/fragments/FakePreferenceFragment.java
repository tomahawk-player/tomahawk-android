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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.ScriptResolver;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter;
import org.tomahawk.tomahawk_android.dialogs.LoginDialog;
import org.tomahawk.tomahawk_android.dialogs.RedirectConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.ResolverConfigDialog;
import org.tomahawk.tomahawk_android.services.RemoteControllerService;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
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
        implements OnItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = FakePreferenceFragment.class.getSimpleName();

    public static final String FAKEPREFERENCEFRAGMENT_ID_PREFBITRATE = "pref_bitrate";

    public static final String FAKEPREFERENCEFRAGMENT_ID_PLUGINTOPLAY = "plugin_to_play";

    public static final String FAKEPREFERENCEFRAGMENT_ID_SCROBBLEEVERYTHING = "scrobble_everything";

    public static final String FAKEPREFERENCEFRAGMENT_ID_APPVERSION = "app_version";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PREFBITRATE
            = "org.tomahawk.tomahawk_android.prefbitrate";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING
            = "org.tomahawk.tomahawk_android.scrobbleeverything";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY
            = "org.tomahawk.tomahawk_android.plugintoplay";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_APPVERSION
            = "org.tomahawk.tomahawk_android.appversion";

    private SharedPreferences mSharedPreferences;

    private List<FakePreferenceGroup> mFakePreferenceGroups;

    private FakePreferenceFragmentReceiver mFakePreferenceFragmentReceiver;

    private class FakePreferenceFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (AuthenticatorManager.AUTHENTICATOR_LOGGED_IN.equals(intent.getAction())) {
                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        ((FakePreferencesAdapter) getListAdapter()).notifyDataSetChanged();
                    }
                });
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
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Set up the set of FakePreferences to be shown in this Fragment
        mFakePreferenceGroups = new ArrayList<FakePreferenceGroup>();
        FakePreferenceGroup prefGroup = new FakePreferenceGroup(
                getString(R.string.fakepreference_accounts_header));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_AUTH,
                AuthenticatorManager.AUTHENTICATOR_ID_HATCHET,
                getString(R.string.fakepreference_hatchetlogin_title_string),
                getString(R.string.fakepreference_hatchetlogin_summary_string),
                R.drawable.hatchet_icon));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_AUTH,
                AuthenticatorManager.AUTHENTICATOR_ID_SPOTIFY,
                getString(R.string.fakepreference_spotifylogin_title_string),
                getString(R.string.fakepreference_spotifylogin_summary_string),
                R.drawable.spotify_icon));
        for (ScriptResolver scriptResolver : PipeLine.getInstance().getScriptResolvers()) {
            prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                    FakePreferenceGroup.FAKEPREFERENCE_TYPE_CONFIG, scriptResolver.getId(),
                    scriptResolver.getCollectionName(), scriptResolver.getDescription()));
        }
        mFakePreferenceGroups.add(prefGroup);
        prefGroup = new FakePreferenceGroup(getString(R.string.fakepreference_playback_header));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX,
                FAKEPREFERENCEFRAGMENT_ID_PLUGINTOPLAY,
                FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY,
                getString(R.string.fakepreference_plugintoplay_title_string),
                getString(R.string.fakepreference_plugintoplay_summary_string)));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX,
                FAKEPREFERENCEFRAGMENT_ID_SCROBBLEEVERYTHING,
                FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING,
                getString(R.string.fakepreference_scrobble_title_string),
                getString(R.string.fakepreference_scrobble_summary_string)));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_SPINNER,
                FAKEPREFERENCEFRAGMENT_ID_PREFBITRATE,
                FAKEPREFERENCEFRAGMENT_KEY_PREFBITRATE,
                getString(R.string.fakepreference_bitrate_title_string),
                getString(R.string.fakepreference_bitrate_summary_string)));
        mFakePreferenceGroups.add(prefGroup);
        prefGroup = new FakePreferenceGroup(getString(R.string.fakepreference_info_header));
        String versionName = "";
        try {
            if (getActivity().getPackageManager() != null) {
                PackageInfo packageInfo = getActivity().getPackageManager()
                        .getPackageInfo(getActivity().getPackageName(), 0);
                versionName = packageInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "onViewCreated: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN,
                FAKEPREFERENCEFRAGMENT_ID_APPVERSION,
                FAKEPREFERENCEFRAGMENT_KEY_APPVERSION,
                getString(R.string.fakepreference_appversion_title_string), versionName));
        mFakePreferenceGroups.add(prefGroup);

        // Now we can push the complete set of FakePreferences into our FakePreferencesAdapter,
        // so that it can provide our ListView with the correct Views.
        FakePreferencesAdapter fakePreferencesAdapter = new FakePreferencesAdapter(getActivity(),
                getActivity().getLayoutInflater(), mFakePreferenceGroups);
        setListAdapter(fakePreferencesAdapter);

        getListView().setOnItemClickListener(this);
    }

    /**
     * Initialize
     */
    @Override
    public void onResume() {
        super.onResume();

        ((FakePreferencesAdapter) getListAdapter()).notifyDataSetChanged();

        getActivity().setTitle(getString(R.string.fakepreferencefragment_title_string));

        if (mFakePreferenceFragmentReceiver == null) {
            mFakePreferenceFragmentReceiver = new FakePreferenceFragmentReceiver();
        }

        // Register intents that the BroadcastReceiver should listen to
        getActivity().registerReceiver(mFakePreferenceFragmentReceiver,
                new IntentFilter(AuthenticatorManager.AUTHENTICATOR_LOGGED_IN));
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mFakePreferenceFragmentReceiver != null) {
            getActivity().unregisterReceiver(mFakePreferenceFragmentReceiver);
            mFakePreferenceFragmentReceiver = null;
        }
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
        FakePreferenceGroup.FakePreference fakePreference
                = (FakePreferenceGroup.FakePreference) getListAdapter().getItem(position);
        if (fakePreference.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX) {
            // if a FakePreference of type "FAKEPREFERENCE_TYPE_CHECKBOX" has been clicked,
            // we edit the associated SharedPreference and toggle its boolean value
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            boolean preferenceState = mSharedPreferences
                    .getBoolean(fakePreference.getStorageKey(), false);
            editor.putBoolean(fakePreference.getStorageKey(), !preferenceState);
            editor.commit();
            if (fakePreference.getKey() == FAKEPREFERENCEFRAGMENT_ID_SCROBBLEEVERYTHING
                    && !preferenceState && Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                RemoteControllerService.askAccess();
            }
        } else if (fakePreference.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_AUTH) {
            // if a FakePreference of type "FAKEPREFERENCE_TYPE_AUTH" has been clicked,
            // we show a LoginDialog
            LoginDialog dialog = new LoginDialog();
            Bundle args = new Bundle();
            args.putString(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY, fakePreference.getKey());
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        } else if (fakePreference.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_CONFIG) {
            DialogFragment dialog;
            if (PipeLine.PLUGINNAME_RDIO.equals(fakePreference.getKey())
                    || PipeLine.PLUGINNAME_DEEZER.equals(fakePreference.getKey())) {
                dialog = new RedirectConfigDialog();
            } else {
                dialog = new ResolverConfigDialog();
            }
            Bundle args = new Bundle();
            args.putString(TomahawkFragment.TOMAHAWK_PREFERENCEID_KEY, fakePreference.getKey());
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        ((FakePreferencesAdapter) getListAdapter()).notifyDataSetChanged();
    }
}
