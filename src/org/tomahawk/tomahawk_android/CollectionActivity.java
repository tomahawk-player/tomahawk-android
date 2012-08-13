/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Christopher Reichert <creichert07@gmail.com>
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

import org.tomahawk.libtomahawk.Collection;
import org.tomahawk.libtomahawk.SourceList;

import android.os.Bundle;
import android.support.v4.view.ViewPager;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;

public class CollectionActivity extends SherlockFragmentActivity {

    public static final String COLLECTION_ID_EXTRA = "collection_id";
    public static final int SEARCH_OPTION_ID = 0;

    private ViewPager mViewPager;
    private TabsAdapter mTabsAdapter;

    private Collection mCollection;

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreate(android.os.Bundle)
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.view_pager);
        setContentView(mViewPager);

        final ActionBar bar = getSupportActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.title_browse_fragment),
                ArtistFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.title_mymusic_fragment),
                AlbumFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.title_friends_fragment),
                TrackFragment.class, null);
        mTabsAdapter.addTab(bar.newTab().setText(R.string.title_player_fragment),
                PlayerFragment.class, null);
    }

    @Override
    public void onResume() {
        super.onResume();

        if (getIntent().hasExtra(COLLECTION_ID_EXTRA)) {
            SourceList sl = ((TomahawkApp) getApplication()).getSourceList();
            mCollection = sl.getCollectionFromId(getIntent().getIntExtra(COLLECTION_ID_EXTRA, 0));
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.app.Activity#onCreateOptionsMenu(android.view.Menu)
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        getSupportMenuInflater().inflate(R.menu.tomahawk_main_activity, menu);

        menu.add(0, SEARCH_OPTION_ID, 0, "Search")
                .setIcon(R.drawable.ic_action_search)
                .setActionView(R.layout.collapsible_edittext)
                .setShowAsAction(
                        MenuItem.SHOW_AS_ACTION_ALWAYS
                                | MenuItem.SHOW_AS_ACTION_COLLAPSE_ACTION_VIEW);

        return true;
    }

    /**
     * Returns this Activities current Collection.
     * 
     * @return the current Collection in this Activity.
     */
    public Collection getCollection() {
        return mCollection;
    }
}
