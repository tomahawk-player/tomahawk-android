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

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.authentication.SpotifyAuthenticatorUtils;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Resolver;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.utils.FakePreferenceGroup;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;

import java.util.List;

import se.emilsjolander.stickylistheaders.StickyListHeadersAdapter;

/**
 * Since {@link android.preference.PreferenceFragment} is not supported with the official support
 * library, and also not within ActionBarSherlock, we have to create our own {@link
 * org.tomahawk.tomahawk_android.fragments.FakePreferenceFragment} with our own {@link
 * FakePreferencesAdapter}
 */
public class FakePreferencesAdapter extends BaseAdapter implements StickyListHeadersAdapter {

    private Context mContext;

    private final LayoutInflater mLayoutInflater;

    private SharedPreferences mSharedPreferences;

    private List<FakePreferenceGroup> mFakePreferenceGroups;

    private class SpinnerListener implements AdapterView.OnItemSelectedListener {

        private String mKey;

        public SpinnerListener(String key) {
            mKey = key;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                int position, long id) {
            if (mSharedPreferences.getInt(mKey,
                    SpotifyAuthenticatorUtils.SPOTIFY_PREF_BITRATE_MODE_MEDIUM) != position) {
                SharedPreferences.Editor editor = mSharedPreferences.edit();
                editor.putInt(mKey, position);
                editor.commit();
                SpotifyAuthenticatorUtils authUtils
                        = (SpotifyAuthenticatorUtils) AuthenticatorManager
                        .getInstance().getAuthenticatorUtils(
                                AuthenticatorManager.AUTHENTICATOR_ID_SPOTIFY);
                authUtils.setBitrate(position);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    /**
     * Constructs a new {@link org.tomahawk.tomahawk_android.adapters.FakePreferencesAdapter}
     */
    public FakePreferencesAdapter(Context context, LayoutInflater layoutInflater,
            List<FakePreferenceGroup> fakePreferenceGroups) {
        mContext = context;
        mLayoutInflater = layoutInflater;
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mContext);
        mFakePreferenceGroups = fakePreferenceGroups;
    }

    @Override
    public void notifyDataSetChanged() {
        super.notifyDataSetChanged();
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
            if (viewHolder == null || viewHolder.getViewType() != viewType) {
                // If the viewHolder is null or the old viewType is different than the new one,
                // we need to inflate a new view and construct a new viewHolder,
                // which we set as the view's tag
                if (viewType == R.id.fakepreferencesadapter_viewtype_plain) {
                    view = mLayoutInflater.inflate(R.layout.fake_preferences_plain, parent, false);
                    viewHolder = new ViewHolder(view, viewType);
                    view.setTag(viewHolder);
                } else if (viewType == R.id.fakepreferencesadapter_viewtype_checkbox) {
                    view = mLayoutInflater
                            .inflate(R.layout.fake_preferences_checkbox, parent, false);
                    viewHolder = new ViewHolder(view, viewType);
                    view.setTag(viewHolder);
                } else if (viewType == R.id.fakepreferencesadapter_viewtype_auth ||
                        viewType == R.id.fakepreferencesadapter_viewtype_config) {
                    view = mLayoutInflater.inflate(R.layout.fake_preferences_auth, parent, false);
                    viewHolder = new ViewHolder(view, viewType);
                    view.setTag(viewHolder);
                } else if (viewType == R.id.fakepreferencesadapter_viewtype_spinner) {
                    view = mLayoutInflater
                            .inflate(R.layout.fake_preferences_spinner, parent, false);
                    viewHolder = new ViewHolder(view, viewType);
                    view.setTag(viewHolder);
                }
            } else {
                if (viewHolder.getImageView1() != null) {
                    viewHolder.getImageView1().setVisibility(View.GONE);
                }
                if (viewHolder.getImageView2() != null) {
                    viewHolder.getImageView2().setVisibility(View.GONE);
                }
            }

            // After we've set up the correct view and viewHolder, we now can fill the View's
            // components with the correct data
            if (viewHolder.getViewType() == R.id.fakepreferencesadapter_viewtype_checkbox) {
                boolean preferenceState = mSharedPreferences
                        .getBoolean(item.getStorageKey(), false);
                viewHolder.getCheckBox().setChecked(preferenceState);
            } else if (viewHolder.getViewType() == R.id.fakepreferencesadapter_viewtype_auth) {
                viewHolder.getImageView2().setVisibility(View.VISIBLE);
                AuthenticatorUtils authenticatorUtils =
                        AuthenticatorManager.getInstance().getAuthenticatorUtils(item.getKey());
                TomahawkUtils.loadDrawableIntoImageView(mContext, viewHolder.getImageView2(),
                        item.getDrawableResId(), !authenticatorUtils.isLoggedIn());
            } else if (viewHolder.getViewType() == R.id.fakepreferencesadapter_viewtype_config) {
                viewHolder.getImageView2().setVisibility(View.VISIBLE);
                Resolver resolver = PipeLine.getInstance().getResolver(item.getKey());
                TomahawkUtils.loadResolverIconIntoImageView(mContext, viewHolder.getImageView2(),
                        resolver, !resolver.isEnabled());
            } else if (viewHolder.getViewType() == R.id.fakepreferencesadapter_viewtype_spinner) {
                String key = item.getStorageKey();
                viewHolder.getSpinner().setSelection(mSharedPreferences
                        .getInt(key, SpotifyAuthenticatorUtils.SPOTIFY_PREF_BITRATE_MODE_MEDIUM));
                viewHolder.getSpinner().setOnItemSelectedListener(new SpinnerListener(key));
            }
            viewHolder.getTextView1().setText(item.getTitle());
            viewHolder.getTextView2().setText(item.getSummary());
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
        View view;
        ViewHolder viewHolder;
        if (convertView != null) {
            viewHolder = (ViewHolder) convertView.getTag();
            view = convertView;
        } else {
            view = mLayoutInflater.inflate(R.layout.fake_preferences_header, parent, false);
            viewHolder = new ViewHolder(view, R.id.fakepreferencesadapter_viewtype_header);
            view.setTag(viewHolder);
        }

        // After we've setup the correct view and viewHolder, we now can set the text for
        // the previously inflated header view
        int sizeSum = 0;
        for (FakePreferenceGroup fakePreferenceGroup : mFakePreferenceGroups) {
            sizeSum += fakePreferenceGroup.getFakePreferences().size();
            if (position < sizeSum) {
                viewHolder.getTextView1().setText(fakePreferenceGroup.getHeader());
                break;
            }
        }

        // Finally we can return the the correct view
        return view;
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

    private int getViewType(FakePreferenceGroup.FakePreference item) {
        if (item.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_CHECKBOX) {
            return R.id.fakepreferencesadapter_viewtype_checkbox;
        } else if (item.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_AUTH) {
            return R.id.fakepreferencesadapter_viewtype_auth;
        } else if (item.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_SPINNER) {
            return R.id.fakepreferencesadapter_viewtype_spinner;
        } else if (item.getType() == FakePreferenceGroup.FAKEPREFERENCE_TYPE_CONFIG) {
            return R.id.fakepreferencesadapter_viewtype_config;
        }
        return R.id.fakepreferencesadapter_viewtype_plain;
    }

}
