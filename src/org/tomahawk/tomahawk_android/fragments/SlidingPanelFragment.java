/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v4.app.Fragment;

public abstract class SlidingPanelFragment extends Fragment {

    private SlidingPanelFragmentReceiver mSlidingPanelFragmentReceiver;

    /**
     * Handles incoming broadcasts.
     */
    private class SlidingPanelFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (TomahawkMainActivity.SLIDING_LAYOUT_EXPANDED.equals(intent.getAction())) {
                onPanelExpanded();
            } else if (TomahawkMainActivity.SLIDING_LAYOUT_COLLAPSED.equals(intent.getAction())) {
                onPanelCollapsed();
            }
        }

    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize and register Receiver
        if (mSlidingPanelFragmentReceiver == null) {
            mSlidingPanelFragmentReceiver = new SlidingPanelFragmentReceiver();
            IntentFilter intentFilter =
                    new IntentFilter(TomahawkMainActivity.SLIDING_LAYOUT_COLLAPSED);
            getActivity().registerReceiver(mSlidingPanelFragmentReceiver, intentFilter);
            intentFilter =
                    new IntentFilter(TomahawkMainActivity.SLIDING_LAYOUT_EXPANDED);
            getActivity().registerReceiver(mSlidingPanelFragmentReceiver, intentFilter);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mSlidingPanelFragmentReceiver != null) {
            getActivity().unregisterReceiver(mSlidingPanelFragmentReceiver);
            mSlidingPanelFragmentReceiver = null;
        }
    }

    public abstract void onPanelCollapsed();

    public abstract void onPanelExpanded();

}
