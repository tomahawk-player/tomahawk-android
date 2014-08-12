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

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.HashSet;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows information provided
 * by a User object. Such as the image, feed and nowPlaying info of a user.
 */
public class SocialActionsFragment extends TomahawkFragment {

    public static final int SHOW_MODE_SOCIALACTIONS = 0;

    public static final int SHOW_MODE_DASHBOARD = 1;

    private HashSet<User> mResolvedUsers = new HashSet<User>();

    @Override
    public void onResume() {
        super.onResume();

        if (getArguments() != null) {
            if (getArguments().containsKey(SHOW_MODE)) {
                mShowMode = getArguments().getInt(SHOW_MODE);
                if (mShowMode == SHOW_MODE_DASHBOARD) {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolveFriendsFeed(mUser));
                } else {
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolveSocialActions(mUser));
                }
            }
        }
        updateAdapter();
    }

    /**
     * Called every time an item inside a ListView or GridView is clicked
     *
     * @param item the TomahawkListItem which corresponds to the click
     */
    @Override
    public void onItemClick(TomahawkListItem item) {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (item instanceof SocialAction) {
            SocialAction socialAction = (SocialAction) item;
            TomahawkListItem target = socialAction.getTargetObject();
            if (target instanceof Query && ((Query) target).isPlayable()) {
                Query query = (Query) target;
                ArrayList<Query> queries = new ArrayList<Query>();
                queries.addAll(mShownQueries);
                PlaybackService playbackService = activity.getPlaybackService();
                if (playbackService != null && playbackService.getCurrentQuery() == query) {
                    playbackService.playPause();
                } else {
                    Playlist playlist = Playlist.fromQueryList(DatabaseHelper.CACHED_PLAYLIST_NAME,
                            queries, query.getCacheKey());
                    if (playbackService != null) {
                        playbackService.setCurrentPlaylist(playlist);
                        playbackService.start();
                    }
                }
            } else if (target instanceof Album) {
                FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                        TracksFragment.class, target.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_ALBUM_KEY, mCollection);
            } else if (target instanceof Artist) {
                FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                        AlbumsFragment.class, target.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_ARTIST_KEY, mCollection);
            } else if (target instanceof User) {
                FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                        SocialActionsFragment.class, ((User) target).getId(),
                        TomahawkFragment.TOMAHAWK_USER_ID,
                        SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
            }
        } else if (item instanceof User) {
            FragmentUtils.replace(activity, getActivity().getSupportFragmentManager(),
                    SocialActionsFragment.class, ((User) item).getId(),
                    TomahawkFragment.TOMAHAWK_USER_ID,
                    SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        if (!mIsResumed) {
            return;
        }

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = getActivity().findViewById(android.R.id.content);
        if (mUser != null) {
            ArrayList<TomahawkListItem> socialActions;
            if (mShowMode == SHOW_MODE_DASHBOARD) {
                socialActions = new ArrayList<TomahawkListItem>(mUser.getFriendsFeed());
            } else {
                socialActions = new ArrayList<TomahawkListItem>(mUser.getSocialActions());
            }
            for (TomahawkListItem socialAction : socialActions) {
                User user = ((SocialAction) socialAction).getUser();
                if (!mResolvedUsers.contains(user) && user.getImage() == null) {
                    mResolvedUsers.add(user);
                    mCurrentRequestIds.add(InfoSystem.getInstance().resolve(user));
                }
            }
            TomahawkListAdapter tomahawkListAdapter;
            getActivity().setTitle(mUser.getName());
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(activity, layoutInflater,
                        socialActions, this);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                if (mShowMode != SHOW_MODE_DASHBOARD) {
                    tomahawkListAdapter.showContentHeaderUser(
                            getActivity().getSupportFragmentManager(), rootView, mUser,
                            mCollection);
                }
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(socialActions);
                if (mShowMode != SHOW_MODE_DASHBOARD) {
                    ((TomahawkListAdapter) getListAdapter()).showContentHeaderUser(
                            getActivity().getSupportFragmentManager(), rootView, mUser,
                            mCollection);
                }
            }

            mShownQueries.clear();
            for (TomahawkListItem listItem : socialActions) {
                if (((SocialAction) listItem).getQuery() != null) {
                    mShownQueries.add(((SocialAction) listItem).getQuery());
                }
            }

            updateShowPlaystate();
        }
    }

    @Override
    public void onPanelCollapsed() {
        if (mUser != null) {
            getActivity().setTitle(mUser.getName());
        }
    }

    @Override
    public void onPanelExpanded() {
    }
}
