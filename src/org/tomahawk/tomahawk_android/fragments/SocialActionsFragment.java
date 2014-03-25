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
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.adapters.TomahawkListAdapter;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link org.tomahawk.tomahawk_android.fragments.TomahawkFragment} which shows information provided
 * by a User object. Such as the image, feed and nowPlaying info of a user.
 */
public class SocialActionsFragment extends TomahawkFragment implements OnItemClickListener {

    @Override
    public void onResume() {
        super.onResume();

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
        position -= getListView().getHeaderViewsCount();
        if (position >= 0) {
            if (getListAdapter().getItem(position) instanceof SocialAction) {
                TomahawkListItem item = ((SocialAction) getListAdapter().getItem(position))
                        .getTargetObject();
                if (item instanceof Query && ((Query) item).isPlayable()) {
                    ArrayList<Query> queries = new ArrayList<Query>();
                    queries.addAll(mShownQueries);
                    PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
                    if (playbackService != null && shouldShowPlaystate() && mQueryPositions
                            .get(playbackService.getCurrentPlaylist().getCurrentQueryIndex())
                            == position) {
                        playbackService.playPause();
                    } else {
                        UserPlaylist playlist = UserPlaylist
                                .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                        UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                        ((Query) item));
                        if (playbackService != null) {
                            playbackService.setCurrentPlaylist(playlist);
                            playbackService.start();
                        }
                    }
                } else if (item instanceof Album) {
                    String key = TomahawkUtils.getCacheKey(item);
                    mTomahawkApp.getContentViewer()
                            .replace(TracksFragment.class, key, TOMAHAWK_ALBUM_KEY, false, false);
                } else if (item instanceof Artist) {
                    String key = TomahawkUtils.getCacheKey(item);
                    mTomahawkApp.getContentViewer()
                            .replace(AlbumsFragment.class, key, TOMAHAWK_ARTIST_KEY, false, false);
                } else if (item instanceof User) {
                    String key = ((User) item).getId();
                    mTomahawkApp.getContentViewer()
                            .replace(SocialActionsFragment.class, key, TOMAHAWK_USER_ID, false,
                                    false);
                }
            }
        }
    }

    /**
     * Update this {@link TomahawkFragment}'s {@link TomahawkListAdapter} content
     */
    @Override
    protected void updateAdapter() {
        ArrayList<TomahawkListItem> socialActions
                = new ArrayList<TomahawkListItem>(mUser.getSocialActions());
        TomahawkListAdapter tomahawkListAdapter;
        if (mUser != null) {
            mTomahawkMainActivity.setTitle(mUser.getName());
            List<List<TomahawkListItem>> listArray
                    = new ArrayList<List<TomahawkListItem>>();
            listArray.add(socialActions);
            if (getListAdapter() == null) {
                tomahawkListAdapter = new TomahawkListAdapter(mTomahawkMainActivity, listArray);
                tomahawkListAdapter.setShowResolvedBy(true);
                tomahawkListAdapter.setShowCategoryHeaders(true, false);
                tomahawkListAdapter.showContentHeader(getListView(), mUser, mIsLocal);
                setListAdapter(tomahawkListAdapter);
            } else {
                ((TomahawkListAdapter) getListAdapter()).setListArray(listArray);
                ((TomahawkListAdapter) getListAdapter()).updateContentHeader(mUser, mIsLocal);
            }
        }

        mShownQueries.clear();
        int i = 0;
        for (SocialAction socialAction : mUser.getSocialActions()) {
            if (socialAction.getQuery() != null) {
                mShownQueries.add(socialAction.getQuery());
                mQueryPositions.put(mShownQueries.size() - 1, i);
            }
            i++;
        }

        getListView().setOnItemClickListener(this);

        updateShowPlaystate();
    }
}
