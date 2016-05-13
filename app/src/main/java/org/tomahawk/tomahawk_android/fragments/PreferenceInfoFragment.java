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

import com.uservoice.uservoicesdk.UserVoice;

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter;
import org.tomahawk.tomahawk_android.dialogs.ConfigDialog;
import org.tomahawk.tomahawk_android.dialogs.SendLogConfigDialog;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkListFragment} which fakes the standard
 * {@link android.preference.PreferenceFragment} behaviour. We need to fake it, because the official
 * support library doesn't provide a {@link android.preference.PreferenceFragment} class
 */
public class PreferenceInfoFragment extends TomahawkListFragment
        implements OnItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = PreferenceInfoFragment.class.getSimpleName();

    public static final String FAKEPREFERENCEFRAGMENT_ID_APPVERSION = "app_version";

    public static final String FAKEPREFERENCEFRAGMENT_ID_USERVOICE = "uservoice";

    public static final String FAKEPREFERENCEFRAGMENT_ID_SENDLOG = "sendlog";

    public static final String FAKEPREFERENCEFRAGMENT_ID_PLAYSTORELINK = "playstore_link";

    public static final String FAKEPREFERENCEFRAGMENT_ID_WEBSITELINK = "website_link";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_APPVERSION
            = "org.tomahawk.tomahawk_android.appversion";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_USERVOICE
            = "org.tomahawk.tomahawk_android.uservoice";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_SENDLOG
            = "org.tomahawk.tomahawk_android.sendlog";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PLAYSTORELINK
            = "org.tomahawk.tomahawk_android.playstore_link";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_WEBSITELINK
            = "org.tomahawk.tomahawk_android.website_link";

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.PreferenceInfoFragment}'s
     * {@link android.view.View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fetch our SharedPreferences from the PreferenceManager
        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getActivity());
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Set up the set of FakePreferences to be shown in this Fragment
        List<FakePreferenceGroup> fakePreferenceGroups = new ArrayList<>();
        FakePreferenceGroup prefGroup = new FakePreferenceGroup(
                getString(R.string.preferences_info));

        FakePreferenceGroup.FakePreference pref = new FakePreferenceGroup.FakePreference();
        pref.type = FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN;
        pref.id = FAKEPREFERENCEFRAGMENT_ID_USERVOICE;
        pref.storageKey = FAKEPREFERENCEFRAGMENT_KEY_USERVOICE;
        pref.title = getString(R.string.preferences_app_uservoice);
        pref.summary = getString(R.string.preferences_app_uservoice_text);
        prefGroup.addFakePreference(pref);

        pref = new FakePreferenceGroup.FakePreference();
        pref.type = FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN;
        pref.id = FAKEPREFERENCEFRAGMENT_ID_PLAYSTORELINK;
        pref.storageKey = FAKEPREFERENCEFRAGMENT_KEY_PLAYSTORELINK;
        pref.title = getString(R.string.preferences_app_playstore_link);
        pref.summary = getString(R.string.preferences_app_playstore_link_text);
        prefGroup.addFakePreference(pref);

        pref = new FakePreferenceGroup.FakePreference();
        pref.type = FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN;
        pref.id = FAKEPREFERENCEFRAGMENT_ID_WEBSITELINK;
        pref.storageKey = FAKEPREFERENCEFRAGMENT_KEY_WEBSITELINK;
        pref.title = getString(R.string.preferences_app_website_link);
        pref.summary = getString(R.string.preferences_app_website_link_text);
        prefGroup.addFakePreference(pref);

        pref = new FakePreferenceGroup.FakePreference();
        pref.type = FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN;
        pref.id = FAKEPREFERENCEFRAGMENT_ID_SENDLOG;
        pref.storageKey = FAKEPREFERENCEFRAGMENT_KEY_SENDLOG;
        pref.title = getString(R.string.preferences_app_sendlog);
        pref.summary = getString(R.string.preferences_app_sendlog_text);
        prefGroup.addFakePreference(pref);

        pref = new FakePreferenceGroup.FakePreference();
        pref.type = FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN;
        pref.id = FAKEPREFERENCEFRAGMENT_ID_APPVERSION;
        pref.storageKey = FAKEPREFERENCEFRAGMENT_KEY_APPVERSION;
        pref.title = getString(R.string.preferences_app_version);
        pref.summary = "";
        try {
            if (getActivity().getPackageManager() != null) {
                PackageInfo packageInfo = getActivity().getPackageManager()
                        .getPackageInfo(getActivity().getPackageName(), 0);
                pref.summary = packageInfo.versionName;
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "onViewCreated: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        prefGroup.addFakePreference(pref);

        fakePreferenceGroups.add(prefGroup);

        // Now we can push the complete set of FakePreferences into our FakePreferencesAdapter,
        // so that it can provide our ListView with the correct Views.
        FakePreferencesAdapter fakePreferencesAdapter = new FakePreferencesAdapter(getActivity(),
                getActivity().getLayoutInflater(), fakePreferenceGroups);
        setListAdapter(fakePreferencesAdapter);

        getListView().setOnItemClickListener(this);
        setupNonScrollableSpacer(getListView());
    }

    /**
     * Initialize
     */
    @Override
    public void onResume() {
        super.onResume();

        getListAdapter().notifyDataSetChanged();
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
        if (fakePreference.id.equals(FAKEPREFERENCEFRAGMENT_ID_USERVOICE)) {
            UserVoice.launchUserVoice(getActivity());
        } else if (fakePreference.id.equals(FAKEPREFERENCEFRAGMENT_ID_SENDLOG)) {
            ConfigDialog dialog = new SendLogConfigDialog();
            dialog.show(getFragmentManager(), null);
        } else if (fakePreference.id.equals(FAKEPREFERENCEFRAGMENT_ID_PLAYSTORELINK)) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("market://details?id=org.tomahawk.tomahawk_android"));
            startActivity(i);
        } else if (fakePreference.id.equals(FAKEPREFERENCEFRAGMENT_ID_WEBSITELINK)) {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setData(Uri.parse("https://www.tomahawk-player.org/"));
            startActivity(i);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        getListAdapter().notifyDataSetChanged();
    }
}
