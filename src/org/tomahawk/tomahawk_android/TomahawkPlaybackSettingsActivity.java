package org.tomahawk.tomahawk_android;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

public class TomahawkPlaybackSettingsActivity extends SherlockPreferenceActivity 
        implements OnPreferenceChangeListener {

    public static final String PLAYBACK_ON_HEADSET = "playbackonheadset";
    public static final String PREF_PLAYBACK_ON_HEADSET = "playback_on_headset";

    private CheckBoxPreference mPlaybackOnHeadsetPref;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

		addPreferencesFromResource(R.xml.playback_settings);

        mPlaybackOnHeadsetPref = (CheckBoxPreference)findPreference(PLAYBACK_ON_HEADSET);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(TomahawkApp.getContext());
        boolean playbackOnHeadsetInsert = prefs.getBoolean(PREF_PLAYBACK_ON_HEADSET, false);

        mPlaybackOnHeadsetPref.setChecked(playbackOnHeadsetInsert);
	}
	
    /* 
     * (non-Javadoc)
     * @see com.actionbarsherlock.app.SherlockFragmentActivity#onOptionsItemSelected(android.view.MenuItem)
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item != null) {
            if (item.getItemId() == android.R.id.home) {
                super.onBackPressed();
                return true;
            }
        }
        return false;
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
