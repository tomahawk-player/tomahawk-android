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
import org.tomahawk.libtomahawk.collection.UserPlaylist;
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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows information provided
 * by a User object. Such as the image, feed and nowPlaying info of a user.
 */
public class SocialActionsFragment extends TomahawkFragment implements OnItemClickListener {

    public static final int SHOW_MODE_SOCIALACTIONS = 0;

    public static final int SHOW_MODE_DASHBOARD = 1;

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
     * Called every time an item inside the {@link se.emilsjolander.stickylistheaders.StickyListHeadersListView}
     * is clicked
     *
     * @param parent   The AdapterView where the click happened.
     * @param view     The view within the AdapterView that was clicked (this will be a view
     *                 provided by the adapter)
     * @param position The position of the view in the adapter.
     * @param id       The row id of the item that was clicked.
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (getListAdapter().getItem(position) instanceof SocialAction) {
            TomahawkListItem item = ((SocialAction) getListAdapter().getItem(position))
                    .getTargetObject();
            TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
            if (item instanceof Query && ((Query) item).isPlayable()) {
                ArrayList<Query> queries = new ArrayList<Query>();
                queries.addAll(mShownQueries);
                PlaybackService playbackService = activity.getPlaybackService();
                if (playbackService != null && shouldShowPlaystate() && mQueryPositions
                        .get(playbackService.getCurrentPlaylist().getCurrentQueryIndex())
                        == position) {
                    playbackService.playPause();
                } else {
                    UserPlaylist playlist = UserPlaylist
                            .fromQueryList(DatabaseHelper.CACHED_PLAYLIST_ID,
                                    DatabaseHelper.CACHED_PLAYLIST_NAME, queries,
                                    mQueryPositions
                                            .keyAt(mQueryPositions.indexOfValue(position))
                            );
                    if (playbackService != null) {
                        playbackService.setCurrentPlaylist(playlist);
                        playbackService.start();
                    }
                }
            } else if (item instanceof Album) {
                FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                        TracksFragment.class, item.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_ALBUM_KEY, false);
            } else if (item instanceof Artist) {
                FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                        AlbumsFragment.class, item.getCacheKey(),
                        TomahawkFragment.TOMAHAWK_ARTIST_KEY, false);
            } else if (item instanceof User) {
                FragmentUtils.replace(getActivity(), getActivity().getSupportFragmentManager(),
                        SocialActionsFragment.class, ((User) item).getId(),
                        TomahawkFragment.TOMAHAWK_USER_ID,
                        SocialActionsFragment.SHOW_MODE_SOCIALACTIONS);
            }
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

        Context context = getActivity();
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View rootView = getActivity().findViewById(android.R.id.content);
        if (mUser != null) {
            ArrayList<TomahawkListItem> socialActions;
            if (mShowMode == SHOW_MODE_DASHBOARD) {
                socialActions = new ArrayList<TomahawkListItem>(mUser.getFriendsFeed());
            } else {
                socialActions = new ArrayList<TomahawkListItem>(mUser.getSocialActions());
            }
            TomahawkListAdapter tomahawkListAdapter;
            getActivity().setTitle(mUser.getName());
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(context, layoutInflater,
                        socialActions);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true);
                if (mShowMode != SHOW_MODE_DASHBOARD) {
                    tomahawkListAdapter.showContentHeaderUser(
                            getActivity().getSupportFragmentManager(), rootView, mUser, mIsLocal);
                }
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListItems(socialActions);
                if (mShowMode != SHOW_MODE_DASHBOARD) {
                    ((TomahawkListAdapter) getListAdapter()).showContentHeaderUser(
                            getActivity().getSupportFragmentManager(), rootView, mUser, mIsLocal);
                }
            }

            mShownQueries.clear();
            int i = 0;
            if (getListAdapter() != null
                    && ((TomahawkListAdapter) getListAdapter()).isShowingContentHeader()) {
                i++;
            }
            mQueryPositions.clear();
            for (TomahawkListItem listItem : socialActions) {
                if (((SocialAction) listItem).getQuery() != null) {
                    mShownQueries.add(((SocialAction) listItem).getQuery());
                    mQueryPositions.put(mShownQueries.size() - 1, i);
                }
                i++;
            }

            getListView().setOnItemClickListener(this);

            updateShowPlaystate();
        }
    }
}
