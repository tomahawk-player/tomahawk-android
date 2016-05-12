/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.listeners;

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkMenuAdapter;
import org.tomahawk.tomahawk_android.fragments.ChartsSelectorFragment;
import org.tomahawk.tomahawk_android.fragments.CollectionPagerFragment;
import org.tomahawk.tomahawk_android.fragments.ContentHeaderFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistEntriesFragment;
import org.tomahawk.tomahawk_android.fragments.PlaylistsFragment;
import org.tomahawk.tomahawk_android.fragments.PreferencePagerFragment;
import org.tomahawk.tomahawk_android.fragments.SocialActionsFragment;
import org.tomahawk.tomahawk_android.fragments.StationsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.UserPagerFragment;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.MenuDrawer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class MenuDrawerListener implements ListView.OnItemClickListener {

    private TomahawkMainActivity mActivity;

    private StickyListHeadersListView mDrawerList;

    private MenuDrawer mMenuDrawer;

    public MenuDrawerListener(TomahawkMainActivity activity, StickyListHeadersListView drawerList,
            MenuDrawer menuDrawer) {
        mActivity = activity;
        mDrawerList = drawerList;
        mMenuDrawer = menuDrawer;
    }

    /**
     * Called every time an item inside the {@link android.widget.ListView} is clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view
     *                 provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        TomahawkMenuAdapter.ResourceHolder holder =
                (TomahawkMenuAdapter.ResourceHolder) mDrawerList.getAdapter().getItem(position);
        final Bundle bundle = new Bundle();
        if (holder.collection != null) {
            bundle.putString(TomahawkFragment.COLLECTION_ID, holder.collection.getId());
            bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC);
            FragmentUtils.replace(mActivity, CollectionPagerFragment.class, bundle);
        } else if (holder.id.equals(MenuDrawer.HUB_ID_USERPAGE)) {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    bundle.putString(TomahawkFragment.USER, user.getId());
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_STATIC_USER);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            FragmentUtils.replace(mActivity, UserPagerFragment.class, bundle);
                        }
                    });
                }
            });
        } else if (holder.id.equals(MenuDrawer.HUB_ID_FEED)) {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    bundle.putString(TomahawkFragment.USER, user.getId());
                    bundle.putInt(TomahawkFragment.SHOW_MODE,
                            SocialActionsFragment.SHOW_MODE_DASHBOARD);
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_ACTIONBAR_FILLED);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            FragmentUtils.replace(mActivity, SocialActionsFragment.class, bundle);
                        }
                    });
                }
            });
        } else if (holder.id.equals(MenuDrawer.HUB_ID_CHARTS)) {
            FragmentUtils
                    .replace(mActivity, ChartsSelectorFragment.class, bundle);
        } else if (holder.id.equals(MenuDrawer.HUB_ID_COLLECTION)) {
            bundle.putString(TomahawkFragment.COLLECTION_ID,
                    TomahawkApp.PLUGINNAME_USERCOLLECTION);
            bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC);
            FragmentUtils.replace(mActivity, CollectionPagerFragment.class, bundle);
        } else if (holder.id.equals(MenuDrawer.HUB_ID_LOVEDTRACKS)) {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    bundle.putInt(TomahawkFragment.SHOW_MODE,
                            PlaylistEntriesFragment.SHOW_MODE_LOVEDITEMS);
                    bundle.putString(TomahawkFragment.USER, user.getId());
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            FragmentUtils.replace(mActivity, PlaylistEntriesFragment.class, bundle);
                        }
                    });
                }
            });
        } else if (holder.id.equals(MenuDrawer.HUB_ID_PLAYLISTS)) {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    bundle.putString(TomahawkFragment.USER, user.getId());
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_STATIC);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            FragmentUtils.replace(mActivity, PlaylistsFragment.class, bundle);
                        }
                    });
                }
            });
        } else if (holder.id.equals(MenuDrawer.HUB_ID_STATIONS)) {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    bundle.putString(TomahawkFragment.USER, user.getId());
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_STATIC);
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            FragmentUtils.replace(mActivity, StationsFragment.class, bundle);
                        }
                    });
                }
            });
        } else if (holder.id.equals(MenuDrawer.HUB_ID_SETTINGS)) {
            bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_STATIC_SMALL);
            FragmentUtils.replace(mActivity, PreferencePagerFragment.class, bundle);
        }
        if (mMenuDrawer != null) {
            mMenuDrawer.closeDrawer();
        }
    }

}
