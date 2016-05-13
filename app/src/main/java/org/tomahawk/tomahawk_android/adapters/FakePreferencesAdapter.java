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

import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;
import org.tomahawk.tomahawk_android.utils.PreferenceUtils;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Since {@link android.preference.PreferenceFragment} is not supported with the official support
 * library, and also not within ActionBarSherlock, we have to create our own Fragment with our own
 * {@link FakePreferencesAdapter}
 */
public class FakePreferencesAdapter extends StickyBaseAdapter {

    private final LayoutInflater mLayoutInflater;

    private final List<FakePreferenceGroup> mFakePreferenceGroups;

    private class SpinnerListener implements AdapterView.OnItemSelectedListener {

        private final String mKey;

        public SpinnerListener(String key) {
            mKey = key;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            if (PreferenceUtils.getInt(mKey) != position) {
                SharedPreferences.Editor editor = PreferenceUtils.edit();
                editor.putInt(mKey, position);
                editor.commit();
                //TODO actually set bitrate
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    /**
     * Constructs a new {@link org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter}
     */
    public FakePreferencesAdapter(LayoutInflater layoutInflater,
            List<FakePreferenceGroup> fakePreferenceGroups) {
        mLayoutInflater = layoutInflater;
        mFakePreferenceGroups = fakePreferenceGroups;
    }

    /**
     * @return the total amount of all {@link FakePreferenceGroup}s this {@link
     * FakePreferencesAdapter} displays
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
        FakePreferenceGroup.FakePreference item =
                (FakePreferenceGroup.FakePreference) getItem(position);

        if (item != null) {
            ViewHolder viewHolder = null;
            if (convertView != null) {
                viewHolder = (ViewHolder) convertView.getTag();
                view = convertView;
            }
            int viewType = getViewType(item);
            if (viewHolder == null || viewHolder.mLayoutId != viewType) {
                // If the viewHolder is null or the old viewType is different than the new one,
                // we need to inflate a new view and construct a new viewHolder,
                // which we set as the view's tag
                view = mLayoutInflater.inflate(viewType, parent, false);
                viewHolder = new ViewHolder(view, viewType);
                view.setTag(viewHolder);
            } else {
                ImageView imageView = (ImageView) viewHolder.findViewById(R.id.imageview1);
                if (imageView != null) {
                    imageView.setVisibility(View.GONE);
                }
            }

            // After we've set up the correct view and viewHolder, we now can fill the View's
            // components with the correct data
            if (viewHolder.mLayoutId == R.layout.fake_preferences_checkbox) {
                boolean preferenceState = PreferenceUtils.getBoolean(item.storageKey);
                CheckBox checkBox = (CheckBox) viewHolder.findViewById(R.id.checkbox1);
                checkBox.setChecked(preferenceState);
            } else if (viewHolder.mLayoutId == R.layout.fake_preferences_spinner) {
                ArrayList<CharSequence> list = new ArrayList<>();
                for (String headerString : TomahawkApp.getContext().getResources()
                        .getStringArray(R.array.fake_preferences_items_bitrate)) {
                    list.add(headerString.toUpperCase());
                }
                ArrayAdapter<CharSequence> adapter = new ArrayAdapter<>(
                        TomahawkApp.getContext(), R.layout.spinner_textview, list);
                adapter.setDropDownViewResource(R.layout.spinner_dropdown_textview);
                Spinner spinner = (Spinner) viewHolder.findViewById(R.id.spinner1);
                spinner.setAdapter(adapter);
                spinner.setSelection(PreferenceUtils.getInt(item.storageKey));
                spinner.setOnItemSelectedListener(new SpinnerListener(item.storageKey));
            }
            TextView textView1 = (TextView) viewHolder.findViewById(R.id.textview1);
            textView1.setText(item.title);
            TextView textView2 = (TextView) viewHolder.findViewById(R.id.textview2);
            textView2.setText(item.summary);
        }

        // Finally we can return the correct view
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
        return new View(TomahawkApp.getContext());
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
        return 0;
    }

    private int getViewType(FakePreferenceGroup.FakePreference item) {
        if (item.type == FakePreferenceGroup.TYPE_CHECKBOX) {
            return R.layout.fake_preferences_checkbox;
        } else if (item.type == FakePreferenceGroup.TYPE_SPINNER) {
            return R.layout.fake_preferences_spinner;
        }
        return R.layout.fake_preferences_plain;
    }

}
