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
package org.tomahawk.tomahawk_android.utils;

import org.tomahawk.libtomahawk.authentication.AuthenticatorManager;
import org.tomahawk.libtomahawk.authentication.AuthenticatorUtils;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.fragments.CollectionFragment;
import org.tomahawk.tomahawk_android.fragments.FavoritesFragment;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.fragments.SearchableFragment;
import org.tomahawk.tomahawk_android.fragments.SocialActionsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.UserPlaylistsFragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.util.HashMap;
import java.util.Map;

/**
 * This class wraps all functionality that handles the switching of {@link Fragment}s, whenever the
 * user navigates to a new {@link Fragment}. It also implements a custom back stack for every hub,
 * so the user can always return to the previous {@link Fragment}s. There is one hub for every menu
 * entry in the navigation drawer.
 */
public class FragmentUtils {

    public static final int HUB_ID_HOME = -1;

    public static final int HUB_ID_DASHBOARD = 0;

    public static final int HUB_ID_COLLECTION = 1;

    public static final int HUB_ID_LOVEDTRACKS = 2;

    public static final int HUB_ID_PLAYLISTS = 3;

    public static final int HUB_ID_PLAYBACK = 100;

    public static final String FRAGMENT_TAG = "the_ultimate_tag";

    public static void addRootFragment(Context context, FragmentManager fragmentManager) {
        Map<String, String> data = new HashMap<String, String>();
        data.put(HatchetInfoPlugin.HATCHET_ACCOUNTDATA_USER_ID, null);
        TomahawkUtils.getUserDataForAccount(context, data,
                AuthenticatorUtils.AUTHENTICATOR_NAME_HATCHET);
        String mUserId = data.get(HatchetInfoPlugin.HATCHET_ACCOUNTDATA_USER_ID);
        FragmentTransaction ft = fragmentManager.beginTransaction();
        if (mUserId != null) {
            String userName = AuthenticatorManager.getInstance()
                    .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET)
                    .getUserName();
            User loggedInUser = User.get(mUserId);
            loggedInUser.setName(userName);
            Bundle bundle = new Bundle();
            bundle.putString(TomahawkFragment.TOMAHAWK_USER_ID, mUserId);
            bundle.putInt(TomahawkFragment.SHOW_MODE, SocialActionsFragment.SHOW_MODE_DASHBOARD);
            ft.add(R.id.content_viewer_frame,
                    Fragment.instantiate(context, SocialActionsFragment.class.getName(), bundle),
                    FRAGMENT_TAG);
        } else {
            ft.add(R.id.content_viewer_frame,
                    Fragment.instantiate(context, CollectionFragment.class.getName(), null),
                    FRAGMENT_TAG);
        }
        ft.commit();
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(Context context, FragmentManager fragmentManager, Class clss,
            String tomahawkListItemKey, String tomahawkListItemType, Collection collection) {
        Bundle bundle = new Bundle();
        bundle.putString(tomahawkListItemType, tomahawkListItemKey);
        if (collection != null) {
            bundle.putString(CollectionManager.COLLECTION_ID, collection.getId());
        }
        replace(context, fragmentManager, clss, bundle);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(Context context, FragmentManager fragmentManager, Class clss,
            String tomahawkListItemKey, String tomahawkListItemType, int showMode) {
        Bundle bundle = new Bundle();
        bundle.putString(tomahawkListItemType, tomahawkListItemKey);
        bundle.putInt(TomahawkFragment.SHOW_MODE, showMode);
        replace(context, fragmentManager, clss, bundle);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(Context context, FragmentManager fragmentManager, Class clss,
            String queryString) {
        Bundle bundle = new Bundle();
        bundle.putString(SearchableFragment.SEARCHABLEFRAGMENT_QUERY_STRING, queryString);
        replace(context, fragmentManager, clss, bundle);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(Context context, FragmentManager fragmentManager, Class clss) {
        replace(context, fragmentManager, clss, (Bundle) null);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(Context context, FragmentManager fragmentManager, Class clss,
            Bundle bundle) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.content_viewer_frame, Fragment.instantiate(context, clss.getName(), bundle),
                FRAGMENT_TAG);
        ft.addToBackStack(FRAGMENT_TAG);
        ft.commit();
    }

    /**
     * Set the currently shown hub, by providing its id
     *
     * @param hubToShow the id of the hub which should be shown
     */
    public static void showHub(Context context, FragmentManager fragmentManager, int hubToShow) {
        showHub(context, fragmentManager, hubToShow, null);
    }

    /**
     * Set the currently shown hub, by providing its id
     *
     * @param hubToShow the id of the hub which should be shown
     */
    public static void showHub(Context context, FragmentManager fragmentManager, int hubToShow,
            User loggedInUser) {
        switch (hubToShow) {
            case HUB_ID_HOME:
                if (loggedInUser == null) {
                    return;
                }
                replace(context, fragmentManager, SocialActionsFragment.class,
                        loggedInUser.getId(),
                        TomahawkFragment.TOMAHAWK_USER_ID,
                        SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
                break;
            case HUB_ID_DASHBOARD:
                if (loggedInUser == null) {
                    return;
                }
                replace(context, fragmentManager, SocialActionsFragment.class,
                        loggedInUser.getId(),
                        TomahawkFragment.TOMAHAWK_USER_ID,
                        SocialActionsFragment.SHOW_MODE_DASHBOARD);
                break;
            case HUB_ID_COLLECTION:
                Bundle bundle = new Bundle();
                bundle.putString(CollectionManager.COLLECTION_ID,
                        TomahawkApp.PLUGINNAME_USERCOLLECTION);
                replace(context, fragmentManager, CollectionFragment.class, bundle);
                break;
            case HUB_ID_LOVEDTRACKS:
                replace(context, fragmentManager, FavoritesFragment.class);
                break;
            case HUB_ID_PLAYLISTS:
                replace(context, fragmentManager, UserPlaylistsFragment.class);
                break;
            case HUB_ID_PLAYBACK:
                replace(context, fragmentManager, PlaybackFragment.class);
                break;
        }
    }
}
