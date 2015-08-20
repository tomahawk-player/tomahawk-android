/*****************************************************************************
 * EqualizerFragment.java
 *****************************************************************************
 * Copyright © 2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.views.EqualizerBar;
import org.videolan.libvlc.LibVLC;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Spinner;

public class EqualizerFragment extends ContentHeaderFragment {

    public final static String TAG = EqualizerFragment.class.getSimpleName();

    public final static String EQUALIZER_VALUES_PREFERENCE_KEY = "equalizer_values";

    public final static String EQUALIZER_ENABLED_PREFERENCE_KEY = "equalizer_enabled";

    public final static String EQUALIZER_PRESET_PREFERENCE_KEY = "equalizer_preset";

    private SwitchCompat mEnableButton;

    private Spinner mEqualizerPresets;

    private SeekBar mPreAmpSeekBar;

    private LinearLayout mBandsContainers;

    LibVLC mLibVLC = null;

    float[] mEqualizerValues = null;

    private final OnItemSelectedListener mPresetListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            if (mLibVLC == null) {
                return;
            }
            float[] preset = mLibVLC.getPreset(pos);
            if (preset == null) {
                return;
            }

            mEqualizerValues = preset;
            mPreAmpSeekBar.setProgress((int) mEqualizerValues[0] + 20);
            for (int i = 0; i < mEqualizerValues.length - 1; ++i) {
                EqualizerBar bar = (EqualizerBar) mBandsContainers.getChildAt(i);
                bar.setValue(mEqualizerValues[i + 1]);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    };

    private final OnSeekBarChangeListener mPreampListener = new OnSeekBarChangeListener() {
        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (!fromUser) {
                return;
            }

            mEqualizerValues[0] = progress - 20;
            if (mLibVLC != null && mEnableButton.isChecked()) {
                mLibVLC.setEqualizer(mEqualizerValues);
            }
        }
    };

    private class BandListener implements EqualizerBar.OnEqualizerBarChangeListener {

        private final int index;

        public BandListener(int index) {
            this.index = index;
        }

        @Override
        public void onProgressChanged(float value) {
            mEqualizerValues[index] = value;
            if (mLibVLC != null && mEnableButton.isChecked()) {
                mLibVLC.setEqualizer(mEqualizerValues);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        getActivity().setTitle(getResources().getString(R.string.preferences_equalizer));

        return inflater.inflate(R.layout.equalizerfragment_layout, container, false);
    }

    /**
     * Called, when this {@link org.tomahawk.tomahawk_android.fragments.EqualizerFragment}'s {@link
     * android.view.View} has been created
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mEnableButton = (SwitchCompat) view.findViewById(R.id.equalizer_button);
        mEqualizerPresets = (Spinner) view.findViewById(R.id.equalizer_presets);
        mPreAmpSeekBar = (SeekBar) view.findViewById(R.id.equalizer_preamp);
        mBandsContainers = (LinearLayout) view.findViewById(R.id.equalizer_bands);
        setupNonScrollableSpacer(getView());
    }

    @Override
    public void onResume() {
        super.onResume();

        fillViews();
    }

    @Override
    public void onPause() {
        super.onPause();

        mEnableButton.setOnCheckedChangeListener(null);
        mEqualizerPresets.setOnItemSelectedListener(null);
        mPreAmpSeekBar.setOnSeekBarChangeListener(null);
        mBandsContainers.removeAllViews();

        SharedPreferences.Editor editor = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext()).edit();
        editor.putBoolean(EQUALIZER_ENABLED_PREFERENCE_KEY, mEnableButton.isChecked());
        TomahawkUtils.putFloatArray(editor, EQUALIZER_VALUES_PREFERENCE_KEY, mEqualizerValues);
        editor.putInt(EQUALIZER_PRESET_PREFERENCE_KEY, mEqualizerPresets.getSelectedItemPosition());
        editor.apply();
    }

    private void fillViews() {
        SharedPreferences preferences =
                PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
        mLibVLC = VLCMediaPlayer.get().getLibVlcInstance();
        float[] bands = mLibVLC.getBands();
        String[] presets = mLibVLC.getPresets();
        if (mEqualizerValues == null) {
            mEqualizerValues = TomahawkUtils
                    .getFloatArray(preferences, EQUALIZER_VALUES_PREFERENCE_KEY);
        }
        if (mEqualizerValues == null) {
            mEqualizerValues = new float[bands.length + 1];
        }

        // on/off
        mEnableButton.setChecked(mLibVLC.getEqualizer() != null);
        mEnableButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (mLibVLC == null) {
                    return;
                }
                mLibVLC.setEqualizer(isChecked ? mEqualizerValues : null);
            }
        });

        // presets
        mEqualizerPresets.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, presets));

        // Set the default selection asynchronously to prevent a layout initialization bug.
        final int equalizer_preset_pref = preferences.getInt(EQUALIZER_PRESET_PREFERENCE_KEY, 0);
        mEqualizerPresets.post(new Runnable() {
            @Override
            public void run() {
                mEqualizerPresets.setSelection(equalizer_preset_pref, false);
                mEqualizerPresets.setOnItemSelectedListener(mPresetListener);
            }
        });

        // mPreAmpSeekBar
        mPreAmpSeekBar.setMax(40);
        mPreAmpSeekBar.setProgress((int) mEqualizerValues[0] + 20);
        mPreAmpSeekBar.setOnSeekBarChangeListener(mPreampListener);

        // bands
        for (int i = 0; i < bands.length; i++) {
            float band = bands[i];

            EqualizerBar bar = new EqualizerBar(getActivity(), band);
            bar.setValue(mEqualizerValues[i + 1]);
            bar.setListener(new BandListener(i + 1));

            mBandsContainers.addView(bar);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.MATCH_PARENT, 1);
            bar.setLayoutParams(params);
        }
    }
}