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

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.fragments.CollectionPagerFragment;
import org.tomahawk.tomahawk_android.fragments.ContentHeaderFragment;
import org.tomahawk.tomahawk_android.fragments.ContextMenuFragment;
import org.tomahawk.tomahawk_android.fragments.PlaybackFragment;
import org.tomahawk.tomahawk_android.fragments.SocialActionsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

/**
 * This class wraps all functionality that handles the switching of {@link Fragment}s, whenever the
 * user navigates to a new {@link Fragment}.
 */
public class FragmentUtils {

    private static final String TAG = FragmentUtils.class.getSimpleName();

    public static final String FRAGMENT_TAG = "the_ultimate_tag";

    public static final String ROOT_FRAGMENT_TAG = "root_fragment_tag";

    public static final String PLAYBACK_FRAGMENT_TAG = "playback_fragment_tag";

    /**
     * Add a root {@link Fragment} as the first {@link Fragment} the user is seeing after opening
     * the app.
     *
     * @param activity     {@link TomahawkMainActivity} needed as a context object
     * @param loggedInUser the currently logged-in user object. determines whether to show the feed
     *                     fragment or collection fragment
     */
    public static void addRootFragment(TomahawkMainActivity activity, User loggedInUser) {
        if (activity.getSupportFragmentManager().findFragmentByTag(ROOT_FRAGMENT_TAG) == null) {
            FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
            Fragment fragment;
            if (!loggedInUser.isOffline()) {
                Bundle bundle = new Bundle();
                bundle.putString(TomahawkFragment.USER, loggedInUser.getId());
                bundle.putInt(TomahawkFragment.SHOW_MODE,
                        SocialActionsFragment.SHOW_MODE_DASHBOARD);
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_ACTIONBAR_FILLED);
                fragment = Fragment.instantiate(activity, SocialActionsFragment.class.getName(),
                        bundle);
                Log.d(TAG, "Added " + SocialActionsFragment.class.getSimpleName()
                        + " as root fragment.");
            } else {
                Bundle bundle = new Bundle();
                bundle.putString(TomahawkFragment.COLLECTION_ID,
                        TomahawkApp.PLUGINNAME_USERCOLLECTION);
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC);
                fragment = Fragment.instantiate(activity, CollectionPagerFragment.class.getName(),
                        bundle);
                Log.d(TAG, "Added " + CollectionPagerFragment.class.getSimpleName()
                        + " as root fragment.");
            }
            ft.add(R.id.content_viewer_frame, fragment, ROOT_FRAGMENT_TAG);
            ft.commitAllowingStateLoss();
        }
    }

    /**
     * Add a {@link PlaybackFragment} to the container inside the {@link
     * com.sothree.slidinguppanel.SlidingUpPanelLayout}
     *
     * @param activity {@link TomahawkMainActivity} needed as a context object
     */
    public static void addPlaybackFragment(TomahawkMainActivity activity) {
        if (activity.getSupportFragmentManager().findFragmentByTag(PLAYBACK_FRAGMENT_TAG) == null) {
            Bundle bundle = new Bundle();
            bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                    ContentHeaderFragment.MODE_HEADER_PLAYBACK);
            replace(activity, PlaybackFragment.class, bundle, R.id.playback_fragment_frame,
                    PLAYBACK_FRAGMENT_TAG);
        }
    }

    /**
     * Replaces the current {@link Fragment} in the main fragment container.
     *
     * @param activity {@link TomahawkMainActivity} needed as a context object
     * @param clss     Class of the {@link Fragment} to instantiate
     * @param bundle   {@link Bundle} which contains arguments (can be null)
     */
    public static void replace(TomahawkMainActivity activity, Class clss, Bundle bundle) {
        replace(activity, clss, bundle, R.id.content_viewer_frame, FRAGMENT_TAG);
    }

    /**
     * Replaces the current {@link Fragment} in the container with the given id.
     *
     * @param activity       {@link TomahawkMainActivity} needed as a context object
     * @param clss           Class of the {@link Fragment} to instantiate
     * @param bundle         {@link Bundle} which contains arguments (can be null)
     * @param containerResId the resource id of the {@link android.view.ViewGroup} in which the
     *                       Fragment will be replaced
     */
    public static void replace(TomahawkMainActivity activity, Class clss, Bundle bundle,
            int containerResId) {
        replace(activity, clss, bundle, containerResId, FRAGMENT_TAG);
    }

    /**
     * Replaces the current {@link Fragment} in the container with the given id. Allows to specify
     * the {@link String} with which to tag the replaced {@link Fragment}.
     *
     * @param activity       {@link TomahawkMainActivity} needed as a context object
     * @param clss           Class of the {@link Fragment} to instantiate
     * @param bundle         {@link Bundle} which contains arguments (can be null)
     * @param containerResId the resource id of the {@link android.view.ViewGroup} in which the
     *                       Fragment will be replaced
     * @param tag            a {@link String} id to tag the replaced {@link Fragment} with
     */
    public static void replace(TomahawkMainActivity activity, Class clss, Bundle bundle,
            int containerResId, String tag) {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.replace(containerResId, Fragment.instantiate(activity, clss.getName(), bundle), tag);
        if (containerResId == R.id.content_viewer_frame) {
            ft.addToBackStack(tag);
        }
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commitAllowingStateLoss();
        activity.collapsePanel();
        Log.d(TAG, "Current fragment is now " + clss.getSimpleName() + ", Bundle: " + bundle);
    }

    /**
     * Add the given {@link Fragment} to the container with the given id.
     *
     * @param activity       {@link TomahawkMainActivity} needed as a context object
     * @param clss           Class of the {@link Fragment} to instantiate
     * @param bundle         {@link Bundle} which contains arguments (can be null)
     * @param containerResId the resource id of the {@link android.view.ViewGroup} to which the
     *                       Fragment will be added
     */
    public static void add(TomahawkMainActivity activity, Class clss, Bundle bundle,
            int containerResId) {
        FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
        ft.add(containerResId, Fragment.instantiate(activity, clss.getName(), bundle),
                FRAGMENT_TAG);
        ft.addToBackStack(FRAGMENT_TAG);
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commitAllowingStateLoss();
        Log.d(TAG, "Added fragment " + clss.getSimpleName() + ", Bundle: " + bundle);
    }

    /**
     * Show the context menu for the given item in the given context.
     *
     * @param activity     {@link TomahawkMainActivity} needed as a context object
     * @param item         The item for which to show the context menu
     * @param collectionId the id of the corresponding {@link org.tomahawk.libtomahawk.collection.Collection}
     *                     (this is being used to e.g. get an album's track list from a specific
     *                     collection)
     */
    public static boolean showContextMenu(TomahawkMainActivity activity, Object item,
            String collectionId, boolean isFromPlaybackFragment, boolean hideRemoveButton) {
        if (item == null
                || (item instanceof SocialAction
                && ((SocialAction) item).getTargetObject() instanceof User)
                || item instanceof User) {
            return false;
        }

        Bundle args = new Bundle();
        if (collectionId != null) {
            args.putString(TomahawkFragment.COLLECTION_ID, collectionId);
        }
        if (isFromPlaybackFragment) {
            args.putBoolean(TomahawkFragment.FROM_PLAYBACKFRAGMENT, true);
            args.putBoolean(TomahawkFragment.HIDE_REMOVE_BUTTON, true);
        } else if (hideRemoveButton) {
            args.putBoolean(TomahawkFragment.HIDE_REMOVE_BUTTON, true);
        }
        if (item instanceof Query) {
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM,
                    ((Query) item).getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.QUERY);
        } else if (item instanceof Album) {
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM,
                    ((Album) item).getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.ALBUM);
        } else if (item instanceof Artist) {
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM,
                    ((Artist) item).getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.ARTIST);
        } else if (item instanceof SocialAction) {
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM,
                    ((SocialAction) item).getId());
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.SOCIALACTION);
        } else if (item instanceof PlaylistEntry) {
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM,
                    ((PlaylistEntry) item).getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.PLAYLISTENTRY);
        } else if (item instanceof StationPlaylist) {
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM,
                    ((StationPlaylist) item).getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.STATION);
        } else if (item instanceof Playlist) {
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM,
                    ((Playlist) item).getCacheKey());
            args.putString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE,
                    TomahawkFragment.PLAYLIST);
        }
        add(activity, ContextMenuFragment.class, args, R.id.context_menu_frame);
        return true;
    }
}
