/*****************************************************************************
 * EqualizerFragment.java ****************************************************************************
 * Copyright © 2013 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if
 * not, write to the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA
 * 02110-1301, USA.
 *****************************************************************************/
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.mediaplayers.VLCMediaPlayer;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;
import org.tomahawk.tomahawk_android.views.EqualizerBar;
import org.videolan.libvlc.MediaPlayer;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.MainThread;
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

    private SwitchCompat mEnableButton;

    private Spinner mEqualizerPresets;

    private SeekBar mPreAmpSeekBar;

    private LinearLayout mBandsContainers;

    private MediaPlayer.Equalizer mEqualizer = null;

    private final OnItemSelectedListener mPresetListener = new OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
            mEqualizer = MediaPlayer.Equalizer.createFromPreset(pos);
            mPreAmpSeekBar.setProgress((int) mEqualizer.getPreAmp() + 20);
            for (int i = 0; i < MediaPlayer.Equalizer.getBandCount(); ++i) {
                EqualizerBar bar = (EqualizerBar) mBandsContainers.getChildAt(i);
                bar.setValue(mEqualizer.getAmp(i));
            }
            if (mEnableButton.isChecked()) {
                VLCMediaPlayer.getMediaPlayerInstance().setEqualizer(mEqualizer);
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

            mEqualizer.setPreAmp(progress - 20);
            if (mEnableButton.isChecked()) {
                VLCMediaPlayer.getMediaPlayerInstance().setEqualizer(mEqualizer);
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
            mEqualizer.setAmp(index, value);
            if (mEnableButton.isChecked()
                    && VLCMediaPlayer.getMediaPlayerInstance() != null) {
                VLCMediaPlayer.getMediaPlayerInstance().setEqualizer(mEqualizer);
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

        if (mEnableButton.isChecked()) {
            storeEqualizerSettings(mEqualizer, mEqualizerPresets.getSelectedItemPosition());
        } else {
            storeEqualizerSettings(null, 0);
        }
    }

    private void fillViews() {
        final Context context = getActivity();

        if (context == null) {
            return;
        }

        final String[] presets = getEqualizerPresets();

        mEqualizer = readEqualizerSettings();
        final boolean isEnabled = mEqualizer != null;
        if (mEqualizer == null) {
            mEqualizer = MediaPlayer.Equalizer.create();
        }

        // on/off
        mEnableButton.setChecked(isEnabled);
        mEnableButton.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (VLCMediaPlayer.getMediaPlayerInstance() != null) {
                    VLCMediaPlayer.getMediaPlayerInstance().setEqualizer(
                            isChecked ? mEqualizer : null);
                }
            }
        });

        // presets
        mEqualizerPresets.setAdapter(new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_spinner_dropdown_item, presets));

        // Set the default selection asynchronously to prevent a layout initialization bug.
        final int equalizer_preset_pref = PreferenceUtils.getInt(PreferenceUtils.EQUALIZER_PRESET);
        mEqualizerPresets.post(new Runnable() {
            @Override
            public void run() {
                mEqualizerPresets.setSelection(equalizer_preset_pref, false);
                mEqualizerPresets.setOnItemSelectedListener(mPresetListener);
            }
        });

        // preamp
        mPreAmpSeekBar.setMax(40);
        mPreAmpSeekBar.setProgress((int) mEqualizer.getPreAmp() + 20);
        mPreAmpSeekBar.setOnSeekBarChangeListener(mPreampListener);

        // bands
        for (int i = 0; i < MediaPlayer.Equalizer.getBandCount(); i++) {
            float band = MediaPlayer.Equalizer.getBandFrequency(i);

            EqualizerBar bar = new EqualizerBar(getActivity(), band);
            bar.setValue(mEqualizer.getAmp(i));
            bar.setListener(new BandListener(i));

            mBandsContainers.addView(bar);
            LinearLayout.LayoutParams params =
                    new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.MATCH_PARENT, 1);
            bar.setLayoutParams(params);
        }
    }

    private static String[] getEqualizerPresets() {
        final int count = MediaPlayer.Equalizer.getPresetCount();
        final String[] presets = new String[count];
        for (int i = 0; i < count; ++i) {
            presets[i] = MediaPlayer.Equalizer.getPresetName(i);
        }
        return presets;
    }

    @MainThread
    public static MediaPlayer.Equalizer readEqualizerSettings() {
        if (PreferenceUtils.getBoolean(PreferenceUtils.EQUALIZER_ENABLED)) {
            final float[] bands = PreferenceUtils.getFloatArray(PreferenceUtils.EQUALIZER_VALUES);
            final int bandCount = MediaPlayer.Equalizer.getBandCount();
            if (bands.length != bandCount + 1) {
                return null;
            }

            final MediaPlayer.Equalizer eq = MediaPlayer.Equalizer.create();
            eq.setPreAmp(bands[0]);
            for (int i = 0; i < bandCount; ++i) {
                eq.setAmp(i, bands[i + 1]);
            }
            return eq;
        } else {
            return null;
        }
    }

    public static void storeEqualizerSettings(MediaPlayer.Equalizer eq, int preset) {
        SharedPreferences.Editor editor = PreferenceUtils.edit();
        if (eq != null) {
            editor.putBoolean(PreferenceUtils.EQUALIZER_ENABLED, true);
            final int bandCount = MediaPlayer.Equalizer.getBandCount();
            final float[] bands = new float[bandCount + 1];
            bands[0] = eq.getPreAmp();
            for (int i = 0; i < bandCount; ++i) {
                bands[i + 1] = eq.getAmp(i);
            }
            PreferenceUtils.putFloatArray(editor, PreferenceUtils.EQUALIZER_VALUES, bands);
            editor.putInt(PreferenceUtils.EQUALIZER_PRESET, preset);
        } else {
            editor.putBoolean(PreferenceUtils.EQUALIZER_ENABLED, false);
        }
        editor.apply();
    }
}