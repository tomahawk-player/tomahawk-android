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
package org.tomahawk.tomahawk_android;

import org.tomahawk.libtomahawk.TomahawkMenuAdapter;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import com.actionbarsherlock.app.SherlockListActivity;

/**
 * Fragment which represents the "LocalCollection" tabview.
 */
public class SettingsActivity extends SherlockListActivity implements OnItemClickListener {

    private TomahawkMenuAdapter mTomahawkMenuAdapter;

    /* 
     * (non-Javadoc)
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.settings_activity, null, false);
        setContentView(view);
    }

    /* 
     * (non-Javadoc)
     * @see android.app.Activity#onResume()
     */
    @Override
    public void onResume() {
        super.onResume();

        getListView().setOnItemClickListener(this);
        mTomahawkMenuAdapter = new TomahawkMenuAdapter(this,
                getResources().getStringArray(R.array.settings_menu_items), getResources().obtainTypedArray(
                        R.array.settings_menu_items_icons));
        setListAdapter(mTomahawkMenuAdapter);
    }

    /* (non-Javadoc)
     * @see android.widget.AdapterView.OnItemClickListener#onItemClick(android.widget.AdapterView, android.view.View, int, long)
     */
    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int idx, long arg3) {
        switch ((int) arg3) {
        case 0:
            startActivity(new Intent(this, TomahawkAccountAuthenticatorActivity.class));
            break;
        }
    }
}