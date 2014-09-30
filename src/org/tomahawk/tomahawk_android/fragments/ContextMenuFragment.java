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
package org.tomahawk.tomahawk_android.fragments;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.dialogs.ChoosePlaylistDialog;
import org.tomahawk.tomahawk_android.utils.AdapterUtils;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.ShareUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.utils.BlurTransformation;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * A {@link DialogFragment} which emulates the appearance and behaviour of the standard context menu
 * dialog, so that it is fully customizable.
 */
public class ContextMenuFragment extends Fragment {

    //the {@link TomahawkListItem} this {@link FakeContextMenuDialog} is associated with
    private TomahawkListItem mTomahawkListItem;

    private Album mAlbum;

    private Artist mArtist;

    private Playlist mPlaylist;

    private boolean mFromPlaybackFragment;

    protected Collection mCollection;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.context_menu_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        boolean showDelete = false;
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ALBUM_KEY)) {
                mAlbum = Album.getAlbumByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ALBUM_KEY));
                if (mAlbum == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            } else if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY)) {
                mPlaylist = Playlist.getPlaylistById(getArguments()
                        .getString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY));
                if (mPlaylist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            } else if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ARTIST_KEY)) {
                mArtist = Artist.getArtistByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ARTIST_KEY));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
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
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            }
            if (getArguments().containsKey(CollectionManager.COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(CollectionManager.COLLECTION_ID));
            }
        }
        if (mTomahawkListItem instanceof SocialAction) {
            mTomahawkListItem = ((SocialAction) mTomahawkListItem).getTargetObject();
        }

        //Set blurred background image
        if (getView() != null) {
            final View rootView = getActivity().findViewById(R.id.sliding_layout);
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            rootView.setDrawingCacheEnabled(true);
                            rootView.buildDrawingCache();
                            Bitmap bm = rootView.getDrawingCache();
                            bm = Bitmap.createScaledBitmap(bm, bm.getWidth() / 4,
                                    bm.getHeight() / 4, true);
                            bm = BlurTransformation.staticTransform(bm, 25f);

                            ImageView bgImageView =
                                    (ImageView) getView().findViewById(R.id.background);
                            bgImageView.setImageBitmap(bm);

                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            } else {
                                rootView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                            }
                        }
                    });
        }

        //Set up textviews
        TextView artistTextView = (TextView) getView().findViewById(R.id.artist_name);
        artistTextView.setText(mTomahawkListItem.getArtist().getName());
        if (!(mTomahawkListItem instanceof Artist)) {
            TextView itemTextView = (TextView) getView().findViewById(R.id.item_name);
            itemTextView.setText(mTomahawkListItem.getName());
        }

        //Set up button click listeners
        if (!(mTomahawkListItem instanceof Artist)) {
            View addToCollectionButton = getView().findViewById(R.id.addtocollection_button);
            addToCollectionButton.setVisibility(View.VISIBLE);
            addToCollectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mTomahawkListItem instanceof Album) {
                        CollectionManager.getInstance().toggleLovedItem((Album) mTomahawkListItem);
                    } else {
                        Toast.makeText(getActivity(), "Not yet implemented for tracks",
                                Toast.LENGTH_SHORT).show();
                    }
                    getActivity().getSupportFragmentManager().popBackStack();
                }
            });
        }
        if (mTomahawkListItem instanceof Query) {
            View favoriteButton = getView().findViewById(R.id.favorite_button);
            favoriteButton.setVisibility(View.VISIBLE);
            favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
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
            });
        }
        View addToPlaylistButton = getView().findViewById(R.id.addtoplaylist_button);
        addToPlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                ArrayList<Query> queries;
                if (mTomahawkListItem instanceof Album) {
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
                args.putStringArrayList(TomahawkFragment.TOMAHAWK_QUERYARRAY_KEY, queryKeys);
                dialog.setArguments(args);
                dialog.show(getActivity().getSupportFragmentManager(), null);
            }
        });
        View shareButton = getView().findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                Intent shareIntent = ShareUtils
                        .generateShareIntent(mTomahawkListItem);
                if (shareIntent != null) {
                    startActivity(shareIntent);
                }
            }
        });
        View goToArtistButton = getView().findViewById(R.id.gotoartist_button);
        goToArtistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                        getActivity().getSupportFragmentManager(), ArtistPagerFragment.class,
                        mTomahawkListItem.getArtist().getCacheKey(),
                        TomahawkFragment.TOMAHAWK_ARTIST_KEY, mCollection);
            }
        });
    }

}
