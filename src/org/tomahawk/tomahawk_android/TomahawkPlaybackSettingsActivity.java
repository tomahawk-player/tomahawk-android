package org.tomahawk.tomahawk_android;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.app.NavUtils;
import android.view.MenuItem;

public class TomahawkPlaybackSettingsActivity extends PreferenceActivity 
        implements OnPreferenceChangeListener {

    public static final String PLAYBACK_ON_HEADSET = "playbackonheadset";
    public static final String PREF_PLAYBACK_ON_HEADSET = "playback_on_headset";

    private CheckBoxPreference mPlaybackOnHeadsetPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setupActionBar();

		addPreferencesFromResource(R.xml.playback_settings);

        mPlaybackOnHeadsetPref = (CheckBoxPreference)findPreference(PLAYBACK_ON_HEADSET);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
        boolean playbackOnHeadsetInsert = prefs.getBoolean(PREF_PLAYBACK_ON_HEADSET, false);

        mPlaybackOnHeadsetPref.setChecked(playbackOnHeadsetInsert);
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference == mPlaybackOnHeadsetPref) {        
        	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
        	SharedPreferences.Editor editor = prefs.edit();

            editor.putBoolean(PREF_PLAYBACK_ON_HEADSET, mPlaybackOnHeadsetPref.isChecked());
            editor.commit();

            return true;
        }

        return false;
    }

	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		return false;
	}
}
