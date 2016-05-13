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
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;

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

    public static final String PREFERENCE_ID_PREFBITRATE = "pref_bitrate";

    public static final String PREFERENCE_ID_PLUGINTOPLAY = "plugin_to_play";

    public static final String PREFERENCE_ID_SCROBBLEEVERYTHING = "scrobble_everything";

    public static final String PREFERENCE_ID_EQUALIZER = "mEqualizerValues";

    /**
     * Called, when this {@link PreferenceAdvancedFragment}'s {@link android.view.View} has been
     * created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        PreferenceManager.getDefaultSharedPreferences(getActivity())
                .registerOnSharedPreferenceChangeListener(this);

        // Set up the set of FakePreferences to be shown in this Fragment
        List<FakePreferenceGroup> fakePreferenceGroups = new ArrayList<>();
        FakePreferenceGroup prefGroup = new FakePreferenceGroup();

        FakePreferenceGroup.FakePreference pref = new FakePreferenceGroup.FakePreference();
        pref.type = FakePreferenceGroup.TYPE_PLAIN;
        pref.id = PREFERENCE_ID_EQUALIZER;
        pref.title = getString(R.string.preferences_equalizer);
        pref.summary = getString(R.string.preferences_equalizer_text);
        prefGroup.addFakePreference(pref);

        pref = new FakePreferenceGroup.FakePreference();
        pref.type = FakePreferenceGroup.TYPE_CHECKBOX;
        pref.id = PREFERENCE_ID_PLUGINTOPLAY;
        pref.storageKey = PreferenceUtils.PLUG_IN_TO_PLAY;
        pref.title = getString(R.string.preferences_plug_and_play);
        pref.summary = getString(R.string.preferences_plug_and_play_text);
        prefGroup.addFakePreference(pref);

        pref = new FakePreferenceGroup.FakePreference();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            pref.type = FakePreferenceGroup.TYPE_PLAIN;
        } else {
            pref.type = FakePreferenceGroup.TYPE_CHECKBOX;
        }
        pref.id = PREFERENCE_ID_SCROBBLEEVERYTHING;
        pref.storageKey = PreferenceUtils.SCROBBLE_EVERYTHING;
        pref.title = getString(R.string.preferences_playback_data);
        pref.summary = getString(R.string.preferences_playback_data_text,
                HatchetAuthenticatorUtils.HATCHET_PRETTY_NAME);
        prefGroup.addFakePreference(pref);

        pref = new FakePreferenceGroup.FakePreference();
        pref.type = FakePreferenceGroup.TYPE_SPINNER;
        pref.id = PREFERENCE_ID_PREFBITRATE;
        pref.storageKey = PreferenceUtils.PREF_BITRATE;
        pref.title = getString(R.string.preferences_audio_quality);
        pref.summary = getString(R.string.preferences_audio_quality_text);
        prefGroup.addFakePreference(pref);

        fakePreferenceGroups.add(prefGroup);

        // Now we can push the complete set of FakePreferences into our FakePreferencesAdapter,
        // so that it can provide our ListView with the correct Views.
        FakePreferencesAdapter fakePreferencesAdapter = new FakePreferencesAdapter(
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
        if (fakePreference.type == FakePreferenceGroup.TYPE_CHECKBOX) {
            // if a FakePreference of type "TYPE_CHECKBOX" has been clicked,
            // we edit the associated SharedPreference and toggle its boolean value
            boolean preferenceState = PreferenceUtils.getBoolean(fakePreference.storageKey);
            PreferenceUtils.edit()
                    .putBoolean(fakePreference.storageKey, !preferenceState)
                    .commit();
        } else if (fakePreference.type == FakePreferenceGroup.TYPE_PLAIN) {
            if (fakePreference.id.equals(PREFERENCE_ID_EQUALIZER)) {
                Bundle bundle = new Bundle();
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_ACTIONBAR_FILLED);
                FragmentUtils.replace((TomahawkMainActivity) getActivity(), EqualizerFragment.class,
                        bundle);
            } else if (fakePreference.id.equals(PREFERENCE_ID_SCROBBLEEVERYTHING)) {
                PreferenceUtils.askAccess(getActivity());
            }
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        getListAdapter().notifyDataSetChanged();
    }
}
