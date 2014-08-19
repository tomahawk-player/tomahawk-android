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
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.adapters.TomahawkContextMenuAdapter;
import org.tomahawk.tomahawk_android.fragments.AlbumsFragment;
import org.tomahawk.tomahawk_android.fragments.TomahawkFragment;
import org.tomahawk.tomahawk_android.fragments.TracksFragment;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.ShareUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
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
public class FakeContextMenuDialog extends DialogFragment {

    TomahawkContextMenuAdapter mMenuAdapter;

    //the {@link TomahawkListItem} this {@link FakeContextMenuDialog} is associated with
    private TomahawkListItem mTomahawkListItem;

    private Album mAlbum;

    private Artist mArtist;

    private Playlist mPlaylist;

    private boolean mFromPlaybackFragment;

    protected Collection mCollection;

    /**
     * Called when this {@link DialogFragment} is being created
     */
    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean showDelete = false;
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ALBUM_KEY)) {
                mAlbum = Album.getAlbumByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ALBUM_KEY));
                if (mAlbum == null) {
                    dismiss();
                }
            } else if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY)) {
                mPlaylist = Playlist.getPlaylistById(getArguments()
                        .getString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY));
                if (mPlaylist == null) {
                    dismiss();
                }
            } else if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ARTIST_KEY)) {
                mArtist = Artist.getArtistByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ARTIST_KEY));
                if (mArtist == null) {
                    dismiss();
                }
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_SHOWDELETE_KEY)) {
                showDelete = getArguments().getBoolean(TomahawkFragment.TOMAHAWK_SHOWDELETE_KEY);
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
                } else if (TomahawkFragment.TOMAHAWK_PLAYLIST_KEY.equals(type)) {
                    mTomahawkListItem = Playlist.getPlaylistById(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                } else if (TomahawkFragment.TOMAHAWK_ARTIST_KEY.equals(type)) {
                    mTomahawkListItem = Artist.getArtistByKey(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                } else if (TomahawkFragment.TOMAHAWK_QUERY_KEY.equals(type)) {
                    mTomahawkListItem = Query.getQueryByKey(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                } else if (TomahawkFragment.TOMAHAWK_SOCIALACTION_ID.equals(type)) {
                    mTomahawkListItem = SocialAction.getSocialActionById(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                } else if (TomahawkFragment.TOMAHAWK_PLAYLISTENTRY_ID.equals(type)) {
                    mTomahawkListItem = PlaylistEntry.getPlaylistEntryByKey(getArguments()
                            .getString(TomahawkFragment.TOMAHAWK_TOMAHAWKLISTITEM_KEY));
                }
                if (mTomahawkListItem == null) {
                    dismiss();
                }
            }
            if (getArguments().containsKey(CollectionManager.COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(CollectionManager.COLLECTION_ID));
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
        mMenuAdapter = new TomahawkContextMenuAdapter(getActivity(),
                getActivity().getLayoutInflater(),
                mTomahawkListItem, showDelete);
        listView.setAdapter(mMenuAdapter);
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view);
        return builder.create();
    }


    /**
     * If the user clicks on a fakeContextItem, handle what should be done here
     */
    private void onFakeContextItemSelected(int position) {
        ArrayList<Query> queries = new ArrayList<Query>();
        PlaybackService playbackService = ((TomahawkMainActivity) getActivity())
                .getPlaybackService();
        String menuItemTitle = (String) mMenuAdapter.getItem(position);
        if (mTomahawkListItem instanceof SocialAction) {
            mTomahawkListItem = ((SocialAction) mTomahawkListItem).getTargetObject();
        }
        if (menuItemTitle.equals(getString(R.string.fake_context_menu_delete))) {
            if (mTomahawkListItem instanceof Playlist) {
                CollectionManager.getInstance().deletePlaylist(
                        ((Playlist) mTomahawkListItem).getId());
            } else if (mTomahawkListItem instanceof PlaylistEntry && mPlaylist != null) {
                CollectionManager.getInstance().deletePlaylistEntry(mPlaylist.getId(),
                        ((PlaylistEntry) mTomahawkListItem).getId());
            } else if (playbackService != null && mFromPlaybackFragment
                    && mTomahawkListItem instanceof PlaylistEntry) {
                playbackService.deleteQueryInQueue((PlaylistEntry) mTomahawkListItem);
            }
        } else if (menuItemTitle.equals(getString(R.string.fake_context_menu_play))) {
            if (mFromPlaybackFragment) {
                if (playbackService != null && mTomahawkListItem instanceof PlaylistEntry) {
                    if (playbackService.getCurrentEntry() != null && playbackService
                            .getCurrentEntry().getCacheKey().equals(
                                    mTomahawkListItem.getCacheKey())) {
                        if (!playbackService.isPlaying()) {
                            playbackService.start();
                        }
                    } else {
                        playbackService.setCurrentEntry((PlaylistEntry) mTomahawkListItem);
                        playbackService.start();
                    }
                }
            } else {
                Playlist playlist;
                PlaylistEntry currentEntry;
                if (mTomahawkListItem instanceof Query) {
                    if (mAlbum != null) {
                        queries = AdapterUtils.getAlbumTracks(mAlbum, mCollection);
                    } else if (mArtist != null) {
                        queries = AdapterUtils.getArtistTracks(mArtist, mCollection);
                    } else if (mPlaylist != null) {
                        queries = mPlaylist.getQueries();
                    } else {
                        queries.add((Query) mTomahawkListItem);
                    }
                    playlist = Playlist.fromQueryList(DatabaseHelper.CACHED_PLAYLIST_NAME, queries);
                    currentEntry = playlist.getEntryWithQuery((Query) mTomahawkListItem);
                    playlist.setId(DatabaseHelper.CACHED_PLAYLIST_ID);
                } else if (mTomahawkListItem instanceof PlaylistEntry) {
                    ArrayList<PlaylistEntry> playlistEntries = new ArrayList<PlaylistEntry>();
                    if (mPlaylist != null) {
                        playlistEntries = mPlaylist.getEntries();
                    }
                    playlist = Playlist.fromEntriesList(DatabaseHelper.CACHED_PLAYLIST_NAME, null,
                            playlistEntries);
                    currentEntry = (PlaylistEntry) mTomahawkListItem;
                    playlist.setId(DatabaseHelper.CACHED_PLAYLIST_ID);
                } else if (mTomahawkListItem instanceof Playlist) {
                    playlist = (Playlist) mTomahawkListItem;
                    currentEntry = playlist.getFirstEntry();
                } else {
                    playlist = Playlist.fromQueryList(DatabaseHelper.CACHED_PLAYLIST_NAME,
                            mTomahawkListItem.getQueries());
                    currentEntry = playlist.getFirstEntry();
                    playlist.setId(DatabaseHelper.CACHED_PLAYLIST_ID);
                }
                if (playbackService != null) {
                    playbackService.setPlaylist(playlist, currentEntry);
                    playbackService.start();
                }
                FragmentUtils.showHub((TomahawkMainActivity) getActivity(),
                        getActivity().getSupportFragmentManager(), FragmentUtils.HUB_ID_PLAYBACK);
            }
        } else if (menuItemTitle
                .equals(getString(R.string.fake_context_menu_addtoqueue))) {
            if (mTomahawkListItem instanceof Artist) {
                Artist artist = (Artist) mTomahawkListItem;
                queries = AdapterUtils.getArtistTracks(artist, mCollection);
            } else if (mTomahawkListItem instanceof Album) {
                Album album = (Album) mTomahawkListItem;
                queries = AdapterUtils.getAlbumTracks(album, mCollection);
            } else {
                queries = mTomahawkListItem.getQueries();
            }
            if (playbackService != null) {
                playbackService.addQueriesToQueue(queries);
            }
        } else if (menuItemTitle.equals(getString(R.string.fake_context_menu_addtoplaylist))) {
            if (mTomahawkListItem instanceof Artist) {
                Artist artist = (Artist) mTomahawkListItem;
                queries = AdapterUtils.getArtistTracks(artist, mCollection);
            } else if (mTomahawkListItem instanceof Album) {
                Album album = (Album) mTomahawkListItem;
                queries = AdapterUtils.getAlbumTracks(album, mCollection);
            } else {
                queries = mTomahawkListItem.getQueries();
            }
            ArrayList<String> queryKeys = new ArrayList<String>();
            for (Query query : queries) {
                queryKeys.add(query.getCacheKey());
            }
            ChoosePlaylistDialog dialog = new ChoosePlaylistDialog();
            Bundle args = new Bundle();
            args.putStringArrayList(TomahawkFragment.TOMAHAWK_QUERYKEYSARRAY_KEY, queryKeys);
            dialog.setArguments(args);
            dialog.show(getActivity().getSupportFragmentManager(), null);
        } else if (menuItemTitle.equals(getString(R.string.menu_item_share))) {
            startActivity(ShareUtils.generateShareIntent(mTomahawkListItem));
        } else if (menuItemTitle.equals(getString(R.string.menu_item_go_to_album))) {
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    getActivity().getSupportFragmentManager(), TracksFragment.class,
                    mTomahawkListItem.getAlbum().getCacheKey(), TomahawkFragment.TOMAHAWK_ALBUM_KEY,
                    mCollection);
        } else if (menuItemTitle.equals(getActivity().getString(R.string.menu_item_go_to_artist))) {
            FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                    getActivity().getSupportFragmentManager(), AlbumsFragment.class,
                    mTomahawkListItem.getArtist().getCacheKey(),
                    TomahawkFragment.TOMAHAWK_ARTIST_KEY, mCollection);
        } else if (menuItemTitle.equals(getString(R.string.fake_context_menu_love_track))
                || menuItemTitle.equals(getString(R.string.fake_context_menu_unlove_track))) {
            Query query = null;
            if (mTomahawkListItem instanceof Query) {
                query = (Query) mTomahawkListItem;
            } else if (mTomahawkListItem instanceof PlaylistEntry) {
                query = ((PlaylistEntry) mTomahawkListItem).getQuery();
            }
            if (query != null) {
                CollectionManager.getInstance().toggleLovedItem(query);
            }
        }
    }

}
