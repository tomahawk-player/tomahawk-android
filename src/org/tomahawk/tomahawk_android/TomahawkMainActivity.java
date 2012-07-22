/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

/**
 * This class represents the main entry point for the app.
 */
public class TomahawkMainActivity extends ListActivity implements AdapterView.OnItemClickListener {

    private static final String TAG = TomahawkMainActivity.class.getName();

    // Retrieve their order from values/strings.xml
    private static final int BROWSE_ACTION = 0;
    private static final int MY_MUSIC_ACTION = 1;
    private static final int FRIENDS_ACTION = 2;

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setListAdapter(ArrayAdapter.createFromResource(getApplicationContext(),
                R.array.main_options_list, R.layout.main_list_item));

        getListView().setOnItemClickListener(this);

        TomahawkApp app = TomahawkApp.instance();
        app.setContext(getApplicationContext());
        app.initialize();
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.tomahawk_main_activity, menu);
        return true;
    }

    /**
     * React to clicks on the ListView.
     * 
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        switch (position) {

        case BROWSE_ACTION:
            Log.d(TAG, "Browse activity requested.");
            Intent browse = new Intent(getApplicationContext(), BrowseActivity.class);
            startActivity(browse);
            break;

        case MY_MUSIC_ACTION:
            Log.d(TAG, "My Music activity request.");
            Intent mymusic = new Intent(getApplicationContext(), MyMusicActivity.class);
            startActivity(mymusic);
            break;

        case FRIENDS_ACTION:
            Log.d(TAG, "Friends activity requested.");
            Intent friends = new Intent(getApplicationContext(), FriendsActivity.class);
            startActivity(friends);
            break;
        }
    }
}
