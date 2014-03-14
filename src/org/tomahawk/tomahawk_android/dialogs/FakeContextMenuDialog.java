/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.dialogs;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.collection.UserPlaylist;
import org.tomahawk.libtomahawk.database.UserPlaylistsDataSource;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkContextMenuAdapter;
import org.tomahawk.tomahawk_android.fragments.AlbumsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.TracksFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.ContentViewer;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * A {@link DialogFragment} which emulates the appearance and behaviour of the standard context menu
 * dialog, so that it is fully customizable.
 */
public class FakeContextMenuDialog extends TomahawkDialogFragment {

    //array of {@link String} containing all menu entry texts
    private String[] mMenuItemTitles;

    //the {@link TomahawkListItem} this {@link FakeContextMenuDialog} is associated with
    private TomahawkListItem mTomahawkListItem;

    private Album mAlbum;

    private Artist mArtist;

    private UserPlaylist mUserPlaylist;

    private boolean mFromPlaybackFragment;

    private boolean mIsLocal;

    /**
     * Called when this {@link DialogFragment} is being created
     */
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ALBUM_KEY)) {
                mAlbum = Album.getAlbumByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ALBUM_KEY));
            } else if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_USERPLAYLIST_KEY)) {
                mUserPlaylist = UserPlaylist.getUserPlaylistById(getArguments()
                                .getString(TomahawkFragment.TOMAHAWK_USERPLAYLIST_KEY));
            } else if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ARTIST_KEY)) {
                mArtist = Artist.getArtistByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ARTIST_KEY));
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_LIST_ITEM_IS_LOCAL)) {
                mIsLocal = getArguments().getBoolean(TomahawkFragment.TOMAHAWK_LIST_ITEM_IS_LOCAL);
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_MENUITEMTITLESARRAY_KEY)) {
                mMenuItemTitles = getArguments()
                        .getStringArray(TomahawkFragment.TOMAHAWK_MENUITEMTITLESARRAY_KEY);
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_FROMPLAYBACKFRAGMENT)) {
                mFromPlaybackFragment = getArguments()
                        .getBoolean(TomahawkFragment.TOMAHAWK_FROMPLAYBACKFRAGMENT);
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_TYPE)
                    && getArguments().containsKey(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY)) {
                String type = getArguments()
                        .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_TYPE);
                if (TomahawkFragment.TOMAHAWK_ALBUM_KEY.equals(type)) {
                    mTomahawkListItem = Album.getAlbumByKey(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                } else if (TomahawkFragment.TOMAHAWK_USERPLAYLIST_KEY.equals(type)) {
                    mTomahawkListItem = UserPlaylist.getUserPlaylistById(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                } else if (TomahawkFragment.TOMAHAWK_ARTIST_KEY.equals(type)) {
                    mTomahawkListItem = Artist.getArtistByKey(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                } else if (TomahawkFragment.TOMAHAWK_QUERY_KEY.equals(type)) {
                    mTomahawkListItem = Query.getQueryByKey(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                }
            }
        }
        LayoutInflater layoutInflater = getActivity().getLayoutInflater();
        View view = layoutInflater.inflate(R.layout.fake_context_menu_dialog, null);
        ListView listView = (ListView) view.findViewById(R.id.fake_context_menu_dialog_listview);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                onFakeContextItemSelected(position);
                dismiss();
            }
        });
        listView.setAdapter(
                new TomahawkContextMenuAdapter(getActivity().getLayoutInflater(), mMenuItemTitles));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }


    /**
     * If the user clicks on a fakeContextItem, handle what should be done here
     */
    private void onFakeContextItemSelected(int position) {
        UserCollection userCollection = mTomahawkMainActivity.getUserCollection();
        ArrayList<Query> queries = new ArrayList<Query>();
        PlaybackService playbackService = mTomahawkMainActivity.getPlaybackService();
        String menuItemTitle = mMenuItemTitles[position];
        if (menuItemTitle.equals(mTomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_delete))) {
            if (mTomahawkListItem instanceof UserPlaylist) {
                ((TomahawkApp) mTomahawkMainActivity.getApplication()).getUserPlaylistsDataSource()
                        .deleteUserPlaylist(((UserPlaylist) mTomahawkListItem).getId());
            } else if (mTomahawkListItem instanceof Query && mUserPlaylist != null) {
                ((TomahawkApp) mTomahawkMainActivity.getApplication()).getUserPlaylistsDataSource()
                        .deleteQueryInUserPlaylist(mUserPlaylist.getId(),
                                (Query) mTomahawkListItem);
            } else if (playbackService != null && mFromPlaybackFragment
                    && mTomahawkListItem instanceof Query) {
                if (TomahawkUtils.getCacheKey(playbackService.getCurrentTrack())
                        .equals(TomahawkUtils.getCacheKey(mTomahawkListItem))) {
                    boolean wasPlaying = playbackService.isPlaying();
                    if (wasPlaying) {
                        playbackService.pause();
                    }
                    if (playbackService.getCurrentPlaylist().peekNextQuery() != null) {
                        playbackService.next();
                        if (wasPlaying) {
                            playbackService.start();
                        }
                    } else if (playbackService.getCurrentPlaylist().peekPreviousQuery() != null) {
                        playbackService.previous();
                        if (wasPlaying) {
                            playbackService.start();
                        }
                    }
                }
                playbackService.deleteQuery((Query) mTomahawkListItem);
            }
        } else if (menuItemTitle.equals(
                mTomahawkMainActivity.getResources().getString(R.string.fake_context_menu_play))) {
            if (mFromPlaybackFragment) {
                if (playbackService != null && mTomahawkListItem instanceof Query
                        && playbackService.getCurrentPlaylist().getCurrentQuery() != null) {
                    if (TomahawkUtils.getCacheKey(
                            playbackService.getCurrentPlaylist().getCurrentQuery())
                            .equals(TomahawkUtils.getCacheKey(mTomahawkListItem))) {
                        if (!playbackService.isPlaying()) {
                            playbackService.start();
                        }
                    } else {
                        playbackService.setCurrentQuery((Query) mTomahawkListItem);
                        playbackService.getCurrentPlaylist()
                                .setCurrentQuery((Query) mTomahawkListItem);
                        playbackService.start();
                    }
                }
            } else {
                UserPlaylist playlist;
                if (mTomahawkListItem instanceof Query) {
                    if (mAlbum != null) {
                        queries = mAlbum.getQueries(mIsLocal);
                    } else if (mArtist != null) {
                        queries = mArtist.getQueries(mIsLocal);
                    } else if (mUserPlaylist != null) {
                        queries = mUserPlaylist.getQueries();
                    } else {
                        queries.add((Query) mTomahawkListItem);
                    }
                    playlist = UserPlaylist
                            .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                    UserPlaylistsDataSource.CACHED_PLAYLIST_NAME, queries,
                                    (Query) mTomahawkListItem);
                } else if (mTomahawkListItem instanceof UserPlaylist) {
                    playlist = (UserPlaylist) mTomahawkListItem;
                } else {
                    playlist = UserPlaylist
                            .fromQueryList(UserPlaylistsDataSource.CACHED_PLAYLIST_ID,
                                    UserPlaylistsDataSource.CACHED_PLAYLIST_NAME,
                                    mTomahawkListItem.getQueries());
                }
                if (playbackService != null) {
                    playbackService.setCurrentPlaylist(playlist);
                    playbackService.start();
                }
                mTomahawkApp.getContentViewer().showHub(ContentViewer.HUB_ID_PLAYBACK);
            }
        } else if (menuItemTitle.equals(mTomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_playaftercurrenttrack))) {
            queries = mTomahawkListItem.getQueries(mIsLocal);
            if (playbackService != null) {
                if (playbackService.getCurrentPlaylist() != null) {
                    playbackService.addTracksToCurrentPlaylist(
                            playbackService.getCurrentPlaylist().getCurrentQueryIndex() + 1,
                            queries);
                } else {
                    playbackService.addQueriesToCurrentPlaylist(queries);
                }
            }
        } else if (menuItemTitle.equals(mTomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_appendtoplaybacklist))) {
            queries = mTomahawkListItem.getQueries(mIsLocal);
            if (playbackService != null) {
                playbackService.addQueriesToCurrentPlaylist(queries);
            }
        } else if (menuItemTitle.equals(mTomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_addtoplaylist))) {
            queries = mTomahawkListItem.getQueries(mIsLocal);
            ArrayList<String> queryKeys = new ArrayList<String>();
            for (Query query : queries) {
                queryKeys.add(TomahawkUtils.getCacheKey(query));
            }
            ChooseUserPlaylistDialog dialog = new ChooseUserPlaylistDialog();
            Bundle args = new Bundle();
            args.putStringArrayList(TomahawkFragment.TOMAHAWK_QUERYKEYSARRAY_KEY, queryKeys);
            dialog.setArguments(args);
            dialog.show(getFragmentManager(), null);
        } else if (menuItemTitle.equals(mTomahawkMainActivity.getResources()
                .getString(R.string.menu_item_go_to_album))) {
            Bundle bundle = new Bundle();
            String key = TomahawkUtils.getCacheKey(mTomahawkListItem.getAlbum());
            bundle.putString(TomahawkFragment.TOMAHAWK_ALBUM_KEY, key);
            mTomahawkApp.getContentViewer()
                    .replace(TracksFragment.class, key, TomahawkFragment.TOMAHAWK_ALBUM_KEY, false,
                            false);
        } else if (menuItemTitle.equals(mTomahawkMainActivity.getResources()
                .getString(R.string.menu_item_go_to_artist))) {
            Bundle bundle = new Bundle();
            String key = TomahawkUtils.getCacheKey(mTomahawkListItem.getArtist());
            bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY, key);
            mTomahawkApp.getContentViewer()
                    .replace(AlbumsFragment.class, key, TomahawkFragment.TOMAHAWK_ARTIST_KEY, false,
                            false);
        } else if (menuItemTitle.equals(mTomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_love_track))
                || menuItemTitle.equals(mTomahawkMainActivity.getResources()
                .getString(R.string.fake_context_menu_unlove_track))) {
            mUserCollection.toggleLovedItem((Query) mTomahawkListItem);
        }
    }

}
