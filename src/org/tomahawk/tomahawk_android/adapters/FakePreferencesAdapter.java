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

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Since {@link android.preference.PreferenceFragment} is not supported with the official support
 * library, and also not within ActionBarSherlock, we have to create our own {@link
 * org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment} with our own {@link
 * FakePreferencesAdapter}
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

    /**
     * @return the total amount of all {@link FakePreferenceGroup}s this {@link
     *         FakePreferencesAdapter} displays
     */
    @Override
    public int getCount() {
        int countSum = 0;
        for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
            countSum += fakePreferenceGroup.getFakePreferences().size();
        }
        return countSum;
    }

    /**
     * Get the correct {@link org.tomahawk.tomahawk_android.utils.FakePreferenceGroup.FakePreference}
     * for the given position
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

    /**
     * Get the id of the item with the given position (the returned id is equal to the position)
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get the correct {@link View} for the given position. Recycle a convertView, if possible.
     *
     * @param position    The position for which to get the correct {@link View}
     * @param convertView The old {@link View}, which we might be able to recycle
     * @param parent      parental {@link ViewGroup}
     * @return the correct {@link View} for the given position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = null;
        Object item = getItem(position);

        if (item != null) {
            TomahawkBaseAdapter.ViewHolder viewHolder;
            // First we inflate the correct view and set the correct resource ids in the viewHolder.
            // Also we check if we can re-use the old convertView
            if (((FakePreferenceGroup.FakePreference) item).getType()
                    == FakePreferenceGroup.FAKEPREFERENCE_TYPE_PLAIN && ((convertView == null)
                    || ((TomahawkBaseAdapter.ViewHolder) convertView.getTag()).viewType
                    != R.id.fakepreferencesadapter_viewtype_plain)) {
                // In case the View should be drawn as a "FAKEPREFERENCE_TYPE_PLAIN" and no
                // convertView is given or the viewType has changed
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
                // In case the View should be drawn as a "FAKEPREFERENCE_TYPE_CHECKBOX" and no
                // convertView is given or the viewType has changed
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
                // In case the View should be drawn as a "FAKEPREFERENCE_TYPE_DIALOG" and no
                // convertView is given or the viewType has changed
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
                // In case the View should be drawn as a "FAKEPREFERENCE_TYPE_SPINNER" and no
                // convertView is given or the viewType has changed
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
                // Else we can simply re-use the old View referenced by convertView
                view = convertView;
                // set the viewHolder by getting the old viewHolder from the view's tag
                viewHolder = (TomahawkBaseAdapter.ViewHolder) view.getTag();
            }

            // After we've setup the correct view and viewHolder, we now can fill the View's
            // components with the correct data
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

        // Finally we can return the the correct view
        return view;
    }

    /**
     * This method is being called by the StickyListHeaders library. Get the correct header {@link
     * View} for the given position.
     *
     * @param position    The position for which to get the correct {@link View}
     * @param convertView The old {@link View}, which we might be able to recycle
     * @param parent      parental {@link ViewGroup}
     * @return the correct header {@link View} for the given position.
     */
    @Override
    public View getHeaderView(int position, View convertView, ViewGroup parent) {
        TomahawkBaseAdapter.ViewHolder viewHolder;

        // First we inflate the correct view and set the correct resource ids in the viewHolder.
        // We don't do this if convertView already contains a properly setup View, that we can re-use.
        if (convertView == null || ((TomahawkBaseAdapter.ViewHolder) convertView.getTag()).viewType
                != R.id.tomahawklistadapter_viewtype_header) {
            convertView = mLayoutInflater.inflate(mHeaderResourceHolder.resourceId, null);
            viewHolder = new TomahawkBaseAdapter.ViewHolder();
            viewHolder.viewType = R.id.tomahawklistadapter_viewtype_header;
            viewHolder.textFirstLine = (TextView) convertView
                    .findViewById(mHeaderResourceHolder.textViewId1);
            convertView.setTag(viewHolder);
        }

        // After we've setup the correct view and viewHolder, we now can set the text for
        // the previously inflated header view
        viewHolder = (TomahawkBaseAdapter.ViewHolder) convertView.getTag();
        int sizeSum = 0;
        for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
            sizeSum += fakePreferenceGroup.getFakePreferences().size();
            if (position < sizeSum) {
                viewHolder.textFirstLine.setText(fakePreferenceGroup.getHeader());
                break;
            }
        }

        // Finally we can return the the correct view
        return convertView;
    }

    /**
     * This method is being called by the StickyListHeaders library. Returns the same value for each
     * item that should be grouped under the same header.
     *
     * @param position the position of the item for which to get the header id
     * @return the same value for each item that should be grouped under the same header.
     */
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
