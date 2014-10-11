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
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.infosystem.hatchet.HatchetInfoPlugin;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.CollectionFragment;
import org.tomahawk.tomahawk_android.fragments.ContextMenuFragment;
import org.tomahawk.tomahawk_android.fragments.SearchPagerFragment;
import org.tomahawk.tomahawk_android.fragments.SocialActionsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.View;

import java.util.HashMap;
import java.util.Map;

/**
 * This class wraps all functionality that handles the switching of {@link Fragment}s, whenever the
 * user navigates to a new {@link Fragment}. It also implements a custom back stack for every hub,
 * so the user can always return to the previous {@link Fragment}s. There is one hub for every menu
 * entry in the navigation drawer.
 */
public class FragmentUtils {

    public static final String FRAGMENT_TAG = "the_ultimate_tag";

    public static void addRootFragment(TomahawkMainActivity activity,
            FragmentManager fragmentManager) {
        Map<String, String> data = new HashMap<String, String>();
        data.put(HatchetInfoPlugin.HATCHET_ACCOUNTDATA_USER_ID, null);
        AuthenticatorUtils utils = AuthenticatorManager.getInstance()
                .getAuthenticatorUtils(TomahawkApp.PLUGINNAME_HATCHET);
        TomahawkUtils.getUserDataForAccount(data, utils.getAccountName());
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
                    Fragment.instantiate(activity, SocialActionsFragment.class.getName(), bundle),
                    FRAGMENT_TAG);
        } else {
            ft.add(R.id.content_viewer_frame,
                    Fragment.instantiate(activity, CollectionFragment.class.getName()),
                    FRAGMENT_TAG);
        }
        ft.commit();
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(TomahawkMainActivity activity, FragmentManager fragmentManager,
            Class clss, String tomahawkListItemKey, String tomahawkListItemType,
            Collection collection) {
        Bundle bundle = new Bundle();
        bundle.putString(tomahawkListItemType, tomahawkListItemKey);
        if (collection != null) {
            bundle.putString(CollectionManager.COLLECTION_ID, collection.getId());
        }
        replace(activity, fragmentManager, clss, bundle);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(TomahawkMainActivity activity, FragmentManager fragmentManager,
            Class clss, String tomahawkListItemKey, String tomahawkListItemType, int showMode) {
        Bundle bundle = new Bundle();
        bundle.putString(tomahawkListItemType, tomahawkListItemKey);
        bundle.putInt(TomahawkFragment.SHOW_MODE, showMode);
        replace(activity, fragmentManager, clss, bundle);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(TomahawkMainActivity activity, FragmentManager fragmentManager,
            Class clss, String tomahawkListItemKey, String tomahawkListItemType) {
        Bundle bundle = new Bundle();
        bundle.putString(tomahawkListItemType, tomahawkListItemKey);
        replace(activity, fragmentManager, clss, bundle);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(TomahawkMainActivity activity, FragmentManager fragmentManager,
            Class clss, String queryString) {
        Bundle bundle = new Bundle();
        bundle.putString(SearchPagerFragment.SEARCHABLEFRAGMENT_QUERY_STRING, queryString);
        replace(activity, fragmentManager, clss, bundle);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(TomahawkMainActivity activity, FragmentManager fragmentManager,
            Class clss) {
        replace(activity, fragmentManager, clss, (Bundle) null);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(TomahawkMainActivity activity, FragmentManager fragmentManager,
            Class clss, Bundle bundle) {
        replace(activity, fragmentManager, clss, bundle, R.id.content_viewer_frame);
    }

    /**
     * Replaces the current {@link Fragment}
     */
    public static void replace(TomahawkMainActivity activity, FragmentManager fragmentManager,
            Class clss, Bundle bundle, int frameResId) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        View contextMenu = activity.findViewById(R.id.context_menu_framelayout);
        if (contextMenu != null) {
            fragmentManager.popBackStackImmediate();
        }
        ft.replace(frameResId,
                Fragment.instantiate(activity, clss.getName(), bundle),
                FRAGMENT_TAG);
        ft.addToBackStack(FRAGMENT_TAG);
        ft.commit();
        activity.collapsePanel();
    }

    /**
     * Add the given {@link Fragment}
     */
    public static void add(TomahawkMainActivity activity, FragmentManager fragmentManager,
            Class clss, Bundle bundle, boolean inPlaybackFragment) {
        FragmentTransaction ft = fragmentManager.beginTransaction();
        View contextMenu = activity.findViewById(R.id.context_menu_framelayout);
        if (contextMenu != null) {
            fragmentManager.popBackStackImmediate();
        }
        if (inPlaybackFragment) {
            ft.add(R.id.playback_fragment_frame,
                    Fragment.instantiate(activity, clss.getName(), bundle),
                    FRAGMENT_TAG);
        } else {
            ft.add(R.id.content_viewer_frame,
                    Fragment.instantiate(activity, clss.getName(), bundle),
                    FRAGMENT_TAG);
            activity.collapsePanel();
        }
        ft.addToBackStack(FRAGMENT_TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        ft.commit();
    }

    public static boolean showContextMenu(TomahawkMainActivity activity,
            FragmentManager fragmentManager, TomahawkListItem item, TomahawkListItem contextItem,
            boolean inPlaybackFragment) {
        if (item == null
                || (item instanceof SocialAction
                && (((SocialAction) item).getTargetObject() instanceof User
                || ((SocialAction) item).getTargetObject() instanceof Playlist))
                || item instanceof User
                || item instanceof Playlist) {
            return false;
        }

        Bundle args = new Bundle();
        if (contextItem instanceof Album) {
            args.putString(TomahawkFragment.TOMAHAWK_ALBUM_KEY, contextItem.getCacheKey());
        } else if (contextItem instanceof Playlist) {
            args.putString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY,
                    ((Playlist) contextItem).getId());
        } else if (contextItem instanceof Artist) {
            args.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, contextItem.getCacheKey());
        }
        if (item instanceof Query) {
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY, item.getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.TOMAHAWK_QUERY_KEY);
        } else if (item instanceof Album) {
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY, item.getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.TOMAHAWK_ALBUM_KEY);
        } else if (item instanceof Artist) {
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY, item.getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.TOMAHAWK_ARTIST_KEY);
        } else if (item instanceof SocialAction) {
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY,
                    ((SocialAction) item).getId());
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.TOMAHAWK_SOCIALACTION_ID);
        } else if (item instanceof PlaylistEntry) {
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY, item.getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.TOMAHAWK_PLAYLISTENTRY_ID);
        }
        FragmentUtils.add(activity, fragmentManager, ContextMenuFragment.class, args,
                inPlaybackFragment);
        return true;
    }
}
