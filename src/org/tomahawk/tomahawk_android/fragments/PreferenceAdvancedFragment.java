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

import org.tomahawk.libtomahawk.authentication.HatchetAuthenticatorUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;

import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
public class PreferenceAdvancedFragment extends TomahawkListFragment
        implements OnItemClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = PreferenceAdvancedFragment.class.getSimpleName();

    public static final String FAKEPREFERENCEFRAGMENT_ID_PREFBITRATE = "pref_bitrate";

    public static final String FAKEPREFERENCEFRAGMENT_ID_PLUGINTOPLAY = "plugin_to_play";

    public static final String FAKEPREFERENCEFRAGMENT_ID_SCROBBLEEVERYTHING = "scrobble_everything";

    public static final String FAKEPREFERENCEFRAGMENT_ID_EQUALIZER = "mEqualizerValues";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PREFBITRATE
            = "org.tomahawk.tomahawk_android.prefbitrate";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING
            = "org.tomahawk.tomahawk_android.scrobbleeverything";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY
            = "org.tomahawk.tomahawk_android.plugintoplay";

    public static final String FAKEPREFERENCEFRAGMENT_KEY_EQUALIZER
            = "org.tomahawk.tomahawk_android.mEqualizerValues";

    private SharedPreferences mSharedPreferences;

    private List<FakePreferenceGroup> mFakePreferenceGroups;

    /**
     * Called, when this {@link PreferenceAdvancedFragment}'s {@link android.view.View} has been
     * created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Fetch our SharedPreferences from the PreferenceManager
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        // Set up the set of FakePreferences to be shown in this Fragment
        mFakePreferenceGroups = new ArrayList<>();
        FakePreferenceGroup prefGroup = new FakePreferenceGroup(
                getString(R.string.preferences_playback));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN,
                FAKEPREFERENCEFRAGMENT_ID_EQUALIZER,
                FAKEPREFERENCEFRAGMENT_KEY_EQUALIZER,
                getString(R.string.preferences_equalizer),
                getString(R.string.preferences_equalizer_text)));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX,
                FAKEPREFERENCEFRAGMENT_ID_PLUGINTOPLAY,
                FAKEPREFERENCEFRAGMENT_KEY_PLUGINTOPLAY,
                getString(R.string.preferences_plug_and_play),
                getString(R.string.preferences_plug_and_play_text)));
        int scrobblePrefType;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            scrobblePrefType = FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN;
        } else {
            scrobblePrefType = FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX;
        }
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                scrobblePrefType,
                FAKEPREFERENCEFRAGMENT_ID_SCROBBLEEVERYTHING,
                FAKEPREFERENCEFRAGMENT_KEY_SCROBBLEEVERYTHING,
                getString(R.string.preferences_playback_data),
                getString(R.string.preferences_playback_data_text,
                        HatchetAuthenticatorUtils.HATCHET_PRETTY_NAME)));
        prefGroup.addFakePreference(new FakePreferenceGroup.FakePreference(
                FakePreferenceGroup.FAKEPREFERENCE_TYPE_SPINNER,
                FAKEPREFERENCEFRAGMENT_ID_PREFBITRATE,
                FAKEPREFERENCEFRAGMENT_KEY_PREFBITRATE,
                getString(R.string.preferences_audio_quality),
                getString(R.string.preferences_audio_quality_text)));
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
        if (fakePreference.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX) {
            // if a FakePreference of type "FAKEPREFERENCE_TYPE_CHECKBOX" has been clicked,
            // we edit the associated SharedPreference and toggle its boolean value
            SharedPreferences.Editor editor = mSharedPreferences.edit();
            boolean preferenceState = mSharedPreferences
                    .getBoolean(fakePreference.getStorageKey(), false);
            editor.putBoolean(fakePreference.getStorageKey(), !preferenceState);
            editor.commit();
        } else if (fakePreference.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN) {
            String key = fakePreference.getKey();
            if (key.equals(FAKEPREFERENCEFRAGMENT_ID_EQUALIZER)) {
                Bundle bundle = new Bundle();
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_ACTIONBAR_FILLED);
                FragmentUtils.replace((TomahawkMainActivity) getActivity(), EqualizerFragment.class,
                        bundle);
            } else if (key.equals(FAKEPREFERENCEFRAGMENT_ID_SCROBBLEEVERYTHING)) {
                ((TomahawkMainActivity) getActivity()).askAccess();
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        getListAdapter().notifyDataSetChanged();
    }
}
