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
package org.tomahawk.tomahawk_android.adapters;

import com.emilsjolander.components.stickylistheaders.StickyListHeadersAdapter;

import org.tomahawk.libtomahawk.resolver.spotify.LibSpotifyWrapper;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.services.TomahawkService;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.HashMap;
import java.util.List;

/**
 * @author Enno Gottschalk <mrmaffen@googlemail.com>
 */
public class FakePreferencesAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    protected final LayoutInflater mLayoutInflater;

    private SharedPreferences mSharedPreferences;

    private List<FakePreferenceGroup> mFakePreferenceGroups;

    private TomahawkBaseAdapter.ResourceHolder mHeaderResourceHolder;

    private TomahawkBaseAdapter.ResourceHolder mFakePreferencesCheckboxResourceHolder;

    private TomahawkBaseAdapter.ResourceHolder mFakePreferencesPlainResourceHolder;

    private TomahawkBaseAdapter.ResourceHolder mFakePreferencesSpinnerResourceHolder;

    private HashMap<String, ImageView> mProgressDrawables = new HashMap<String, ImageView>();

    /**
     * Constructs a new {@link org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter}
     */
    public FakePreferencesAdapter(LayoutInflater layoutInflater,
            List<FakePreferenceGroup> fakePreferenceGroups) {
        mLayoutInflater = layoutInflater;
        mSharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(TomahawkApp.getContext());
        mFakePreferenceGroups = fakePreferenceGroups;
        mHeaderResourceHolder = new TomahawkBaseAdapter.ResourceHolder();
        mHeaderResourceHolder.resourceId = R.layout.fake_preferences_header;
        mHeaderResourceHolder.textViewId1 = R.id.fake_preferences_header_textview;
        mFakePreferencesCheckboxResourceHolder = new TomahawkBaseAdapter.ResourceHolder();
        mFakePreferencesCheckboxResourceHolder.resourceId = R.layout.fake_preferences_checkbox;
        mFakePreferencesCheckboxResourceHolder.checkBoxId = R.id.fake_preferences_checkbox;
        mFakePreferencesCheckboxResourceHolder.textViewId1 = R.id.fake_preferences_textview;
        mFakePreferencesCheckboxResourceHolder.textViewId2 = R.id.fake_preferences_textview2;
        mFakePreferencesCheckboxResourceHolder.imageViewId = R.id.fake_preferences_progressdrawable;
        mFakePreferencesPlainResourceHolder = new TomahawkBaseAdapter.ResourceHolder();
        mFakePreferencesPlainResourceHolder.resourceId = R.layout.fake_preferences_plain;
        mFakePreferencesPlainResourceHolder.textViewId1 = R.id.fake_preferences_textview;
        mFakePreferencesPlainResourceHolder.textViewId2 = R.id.fake_preferences_textview2;
        mFakePreferencesSpinnerResourceHolder = new TomahawkBaseAdapter.ResourceHolder();
        mFakePreferencesSpinnerResourceHolder.resourceId = R.layout.fake_preferences_spinner;
        mFakePreferencesSpinnerResourceHolder.spinnerId = R.id.fake_preferences_spinner;
        mFakePreferencesSpinnerResourceHolder.textViewId1 = R.id.fake_preferences_textview;
        mFakePreferencesSpinnerResourceHolder.textViewId2 = R.id.fake_preferences_textview2;
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Adapter#getCount()
     */
    @Override
    public int getCount() {
        int countSum = 0;
        for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
            countSum += fakePreferenceGroup.getFakePreferences().size();
        }
        return countSum;
    }

    /*
     * (non-Javadoc)
     * @see android.widget.Adapter#getItem(int)
     */
    @Override
    public Object getItem(int position) {
        Object item = null;
        int offsetCounter = 0;
        for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
            if (position - offsetCounter < fakePreferenceGroup.getFakePreferences().size()) {
                item = fakePreferenceGroup.getFakePreferences().get(position - offsetCounter);
                break;
            }
            offsetCounter += fakePreferenceGroup.getFakePreferences().size();
        }
        return item;
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Adapter#getItemId(int)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /* 
     * (non-Javadoc)
     * @see android.widget.Adapter#getView(int, android.view.View, android.view.ViewGroup)
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        Object item = getItem(position);

        if (item != null) {
            TomahawkBaseAdapter.ViewHolder viewHolder;
            if (((FakePreferenceGroup.FakePreference) item).getType()
                    == FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN && ((convertView == null)
                    || ((TomahawkBaseAdapter.ViewHolder) convertView.getTag()).viewType
                    != R.id.fakepreferencesadapter_viewtype_plain)) {
                view = mLayoutInflater
                        .inflate(mFakePreferencesPlainResourceHolder.resourceId, null);
                viewHolder = new TomahawkBaseAdapter.ViewHolder();
                viewHolder.viewType = R.id.fakepreferencesadapter_viewtype_plain;
                viewHolder.textFirstLine = (TextView) view
                        .findViewById(mFakePreferencesPlainResourceHolder.textViewId1);
                viewHolder.textSecondLine = (TextView) view
                        .findViewById(mFakePreferencesPlainResourceHolder.textViewId2);
                view.setTag(viewHolder);
            } else if (((FakePreferenceGroup.FakePreference) item).getType()
                    == FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX && ((convertView == null)
                    || ((TomahawkBaseAdapter.ViewHolder) convertView.getTag()).viewType
                    != R.id.fakepreferencesadapter_viewtype_checkbox)) {
                view = mLayoutInflater
                        .inflate(mFakePreferencesCheckboxResourceHolder.resourceId, null);
                viewHolder = new TomahawkBaseAdapter.ViewHolder();
                viewHolder.viewType = R.id.fakepreferencesadapter_viewtype_checkbox;
                viewHolder.textFirstLine = (TextView) view
                        .findViewById(mFakePreferencesCheckboxResourceHolder.textViewId1);
                viewHolder.textSecondLine = (TextView) view
                        .findViewById(mFakePreferencesCheckboxResourceHolder.textViewId2);
                viewHolder.checkBox = (CheckBox) view
                        .findViewById(mFakePreferencesCheckboxResourceHolder.checkBoxId);
                view.setTag(viewHolder);
            } else if (((FakePreferenceGroup.FakePreference) item).getType()
                    == FakePreferenceGroup.FAKEPREFERENCE_TYPE_DIALOG && ((convertView == null)
                    || ((TomahawkBaseAdapter.ViewHolder) convertView.getTag()).viewType
                    != R.id.fakepreferencesadapter_viewtype_dialog)) {
                view = mLayoutInflater
                        .inflate(mFakePreferencesCheckboxResourceHolder.resourceId, null);
                viewHolder = new TomahawkBaseAdapter.ViewHolder();
                viewHolder.viewType = R.id.fakepreferencesadapter_viewtype_dialog;
                viewHolder.textFirstLine = (TextView) view
                        .findViewById(mFakePreferencesCheckboxResourceHolder.textViewId1);
                viewHolder.textSecondLine = (TextView) view
                        .findViewById(mFakePreferencesCheckboxResourceHolder.textViewId2);
                viewHolder.checkBox = (CheckBox) view
                        .findViewById(mFakePreferencesCheckboxResourceHolder.checkBoxId);
                viewHolder.imageViewRight = (ImageView) view
                        .findViewById(mFakePreferencesCheckboxResourceHolder.imageViewId);
                view.setTag(viewHolder);
            } else if (((FakePreferenceGroup.FakePreference) item).getType()
                    == FakePreferenceGroup.FAKEPREFERENCE_TYPE_SPINNER && ((convertView == null)
                    || ((TomahawkBaseAdapter.ViewHolder) convertView.getTag()).viewType
                    != R.id.fakepreferencesadapter_viewtype_spinner)) {
                view = mLayoutInflater
                        .inflate(mFakePreferencesSpinnerResourceHolder.resourceId, null);
                viewHolder = new TomahawkBaseAdapter.ViewHolder();
                viewHolder.viewType = R.id.fakepreferencesadapter_viewtype_spinner;
                viewHolder.textFirstLine = (TextView) view
                        .findViewById(mFakePreferencesSpinnerResourceHolder.textViewId1);
                viewHolder.textSecondLine = (TextView) view
                        .findViewById(mFakePreferencesSpinnerResourceHolder.textViewId2);
                viewHolder.spinner = (Spinner) view
                        .findViewById(mFakePreferencesSpinnerResourceHolder.spinnerId);
                view.setTag(viewHolder);
            } else {
                view = convertView;
                viewHolder = (TomahawkBaseAdapter.ViewHolder) view.getTag();
            }
            if (viewHolder.viewType == R.id.fakepreferencesadapter_viewtype_plain) {
                FakePreferenceGroup.FakePreference fakePreference
                        = (FakePreferenceGroup.FakePreference) item;
                viewHolder.textFirstLine.setText(fakePreference.getTitle());
                viewHolder.textSecondLine.setText(fakePreference.getSummary());
            } else if (viewHolder.viewType == R.id.fakepreferencesadapter_viewtype_checkbox) {
                FakePreferenceGroup.FakePreference fakePreference
                        = (FakePreferenceGroup.FakePreference) item;
                boolean preferenceState = mSharedPreferences
                        .getBoolean(fakePreference.getKey(), false);
                viewHolder.checkBox.setChecked(preferenceState);
                viewHolder.textFirstLine.setText(fakePreference.getTitle());
                viewHolder.textSecondLine.setText(fakePreference.getSummary());
            } else if (viewHolder.viewType == R.id.fakepreferencesadapter_viewtype_dialog) {
                FakePreferenceGroup.FakePreference fakePreference
                        = (FakePreferenceGroup.FakePreference) item;
                viewHolder.checkBox.setChecked(fakePreference.isCheckboxState());
                viewHolder.textFirstLine.setText(fakePreference.getTitle());
                viewHolder.textSecondLine.setText(fakePreference.getSummary());
                mProgressDrawables.put(fakePreference.getKey(), viewHolder.imageViewRight);
            } else if (viewHolder.viewType == R.id.fakepreferencesadapter_viewtype_spinner) {
                FakePreferenceGroup.FakePreference fakePreference
                        = (FakePreferenceGroup.FakePreference) item;
                final String key = fakePreference.getKey();
                viewHolder.spinner.setSelection(mSharedPreferences
                        .getInt(key, TomahawkService.SPOTIFY_PREF_BITRATE_MODE_MEDIUM));
                viewHolder.spinner
                        .setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view,
                                    int position, long id) {
                                SharedPreferences.Editor editor = mSharedPreferences.edit();
                                editor.putInt(key, position);
                                editor.commit();
                                LibSpotifyWrapper.setbitrate(position);
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {
                            }
                        });
                viewHolder.textFirstLine.setText(fakePreference.getTitle());
                viewHolder.textSecondLine.setText(fakePreference.getSummary());
            }
        }
        return view;
    }

    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        TomahawkBaseAdapter.ViewHolder viewHolder;
        if (convertView == null || ((TomahawkBaseAdapter.ViewHolder) convertView.getTag()).viewType
                != R.id.tomahawklistadapter_viewtype_header) {
            convertView = mLayoutInflater.inflate(mHeaderResourceHolder.resourceId, null);
            viewHolder = new TomahawkBaseAdapter.ViewHolder();
            viewHolder.viewType = R.id.tomahawklistadapter_viewtype_header;
            viewHolder.textFirstLine = (TextView) convertView
                    .findViewById(mHeaderResourceHolder.textViewId1);
            convertView.setTag(viewHolder);
        }
        viewHolder = (TomahawkBaseAdapter.ViewHolder) convertView.getTag();
        int sizeSum = 0;
        for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
            sizeSum += fakePreferenceGroup.getFakePreferences().size();
            if (position < sizeSum) {
                viewHolder.textFirstLine.setText(fakePreferenceGroup.getHeader());
                break;
            }
        }
        return convertView;
    }

    //remember that these have to be static, position=1 should always return the same Id that is.
    @Override
    public long getHeaderId(int position) {
        long result = 0;
        int sizeSum = 0;
        for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
            sizeSum += fakePreferenceGroup.getFakePreferences().size();
            if (position < sizeSum) {
                break;
            } else {
                result++;
            }
        }
        return result;
    }

}
