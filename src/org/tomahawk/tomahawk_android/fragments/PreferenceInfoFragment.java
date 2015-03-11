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

    public static final String FAKEPREFERENCEFRAGMENT_KEY_APPVERSION
            = "org.tomahawk.tomahawk_android.appversion";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_USERVOICE
            = "org.tomahawk.tomahawk_android.uservoice";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_SENDLOG
            = "org.tomahawk.tomahawk_android.sendlog";

    private SharedPreferences mSharedPreferences;

    private List<FakePreferenceGroup> mFakePreferenceGroups;

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.PreferenceInfoFragment}'s
     * {@link android.view.View} has been created
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
                getString(R.string.preferences_info));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN,
                FAKEPREFERENCEFRAGMENT_ID_USERVOICE,
                FAKEPREFERENCEFRAGMENT_KEY_USERVOICE,
                getString(R.string.preferences_app_uservoice),
                getString(R.string.preferences_app_uservoice_text)));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN,
                FAKEPREFERENCEFRAGMENT_ID_SENDLOG,
                FAKEPREFERENCEFRAGMENT_KEY_SENDLOG,
                getString(R.string.preferences_app_sendlog),
                getString(R.string.preferences_app_sendlog_text)));
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
                getString(R.string.preferences_app_version), versionName));
        mFakePreferenceGroups.add(prefGroup);

        // Now we can push the complete set of FakePreferences into our FakePreferencesAdapter,
        // so that it can provide our ListView with the correct Views.
        FakePreferencesAdapter fakePreferencesAdapter = new FakePreferencesAdapter(getActivity(),
                getActivity().getLayoutInflater(), mFakePreferenceGroups);
        setListAdapter(fakePreferencesAdapter);

        getListView().setOnItemClickListener(this);
        setupNonScrollableSpacer();
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
        if (fakePreference.getKey().equals(FAKEPREFERENCEFRAGMENT_ID_USERVOICE)) {
            UserVoice.launchUserVoice(getActivity());
        } else if (fakePreference.getKey().equals(FAKEPREFERENCEFRAGMENT_ID_SENDLOG)) {
            ConfigDialog dialog = new SendLogConfigDialog();
            dialog.show(getFragmentManager(), null);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        getListAdapter().notifyDataSetChanged();
    }
}
