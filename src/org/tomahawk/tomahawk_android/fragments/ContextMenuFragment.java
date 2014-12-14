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
import org.tomahawk.libtomahawk.collection.CollectionUtils;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.dialogs.ChoosePlaylistDialog;
import org.tomahawk.tomahawk_android.utils.BlurTransformation;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.ShareUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;

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

    protected HashSet<String> mCurrentRequestIds = new HashSet<String>();

    private ContextMenuFragmentReceiver mContextMenuFragmentReceiver;

    public static interface Action {

        public void run();
    }

    private class ContextMenuFragmentReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (InfoSystem.INFOSYSTEM_RESULTSREPORTED.equals(intent.getAction())) {
                String requestId = intent.getStringExtra(
                        InfoSystem.INFOSYSTEM_RESULTSREPORTED_REQUESTID);
                if (mCurrentRequestIds.contains(requestId) && getView() != null) {
                    ImageView albumImageView =
                            (ImageView) getView().findViewById(R.id.album_imageview);
                    TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), albumImageView,
                            mTomahawkListItem.getAlbum().getImage(), Image.getLargeImageSize(),
                            true, false);
                }
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.context_menu_fragment, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize and register Receiver
        if (mContextMenuFragmentReceiver == null) {
            mContextMenuFragmentReceiver = new ContextMenuFragmentReceiver();
            IntentFilter intentFilter = new IntentFilter(InfoSystem.INFOSYSTEM_RESULTSREPORTED);
            getActivity().registerReceiver(mContextMenuFragmentReceiver, intentFilter);
        }

        boolean showDelete = false;
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ALBUM_KEY)) {
                mAlbum = Album.getAlbumByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ALBUM_KEY));
                if (mAlbum == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
            } else if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY)) {
                mPlaylist = Playlist.getPlaylistById(getArguments()
                        .getString(TomahawkFragment.TOMAHAWK_PLAYLIST_KEY));
                if (mPlaylist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
            } else if (getArguments().containsKey(TomahawkFragment.TOMAHAWK_ARTIST_KEY)) {
                mArtist = Artist.getArtistByKey(
                        getArguments().getString(TomahawkFragment.TOMAHAWK_ARTIST_KEY));
                if (mArtist == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
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
                    return;
                }
            }
            if (getArguments().containsKey(CollectionManager.COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(CollectionManager.COLLECTION_ID));
            }
        }
        if (mTomahawkListItem instanceof SocialAction) {
            mTomahawkListItem = ((SocialAction) mTomahawkListItem).getTargetObject();
        } else if (mTomahawkListItem instanceof PlaylistEntry) {
            mTomahawkListItem = ((PlaylistEntry) mTomahawkListItem).getQuery();
        }

        //Set blurred background image
        if (getView() != null) {
            final View rootView = getActivity().findViewById(R.id.sliding_layout);
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            Bitmap bm = Bitmap.createBitmap(rootView.getWidth(),
                                    rootView.getHeight(), Bitmap.Config.ARGB_8888);
                            Canvas canvas = new Canvas(bm);
                            rootView.draw(canvas);
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

        //Set up button click listeners
        View closeButton = getView().findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });
        TextView closeButtonText = (TextView) closeButton.findViewById(R.id.close_button_text);
        closeButtonText.setText(getString(R.string.button_close).toUpperCase());

        setupClickListeners((TomahawkMainActivity) getActivity(), getView(), mTomahawkListItem,
                mCollection, false, new Action() {
                    @Override
                    public void run() {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                });

        //Set up textviews
        if (mTomahawkListItem instanceof Album) {
            View albumNameButton = getView().findViewById(R.id.album_name_button);
            albumNameButton.setVisibility(View.VISIBLE);
            albumNameButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.TOMAHAWK_ALBUM_KEY,
                            mTomahawkListItem.getCacheKey());
                    bundle.putString(CollectionManager.COLLECTION_ID, mCollection.getId());
                    bundle.putInt(ContentHeaderFragment.MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                            TracksFragment.class, bundle);
                }
            });
            TextView artistTextView = (TextView) albumNameButton.findViewById(R.id.album_name);
            artistTextView.setText(mTomahawkListItem.getName());
        } else if (mTomahawkListItem instanceof Query) {
            TextView itemTextView = (TextView) getView().findViewById(R.id.track_name);
            itemTextView.setVisibility(View.VISIBLE);
            itemTextView.setText(mTomahawkListItem.getName());
        }
        TextView artistTextView = (TextView) getView().findViewById(R.id.artist_name);
        artistTextView.setText(mTomahawkListItem.getArtist().getName());

        //Set up album image and stuff
        if (mTomahawkListItem instanceof Album
                || (mTomahawkListItem instanceof Query
                && !TextUtils.isEmpty(mTomahawkListItem.getAlbum().getName()))) {
            View viewAlbumContainer = getView().findViewById(R.id.view_album_container);
            viewAlbumContainer.setVisibility(View.VISIBLE);
            ImageView albumImageView =
                    (ImageView) viewAlbumContainer.findViewById(R.id.album_imageview);
            if (mTomahawkListItem.getAlbum().getImage() != null) {
                TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), albumImageView,
                        mTomahawkListItem.getAlbum().getImage(), Image.getLargeImageSize(), true,
                        false);
            } else {
                mCurrentRequestIds
                        .add(InfoSystem.getInstance().resolve(mTomahawkListItem.getAlbum()));
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mContextMenuFragmentReceiver != null) {
            getActivity().unregisterReceiver(mContextMenuFragmentReceiver);
            mContextMenuFragmentReceiver = null;
        }
    }

    public static void setupClickListeners(final TomahawkMainActivity activity, View view,
            final TomahawkListItem item, final Collection collection,
            final boolean isPlaybackContextMenu, final Action actionOnDone) {
        if (item instanceof Album || item instanceof Artist) {
            View addToCollectionButton = view.findViewById(R.id.addtocollection_button);
            addToCollectionButton.setVisibility(View.VISIBLE);
            if ((item instanceof Album
                    && DatabaseHelper.getInstance().isItemLoved((Album) item))
                    || (item instanceof Artist
                    && DatabaseHelper.getInstance().isItemLoved((Artist) item))) {
                addToCollectionButton.findViewById(R.id.addtocollection_button_underline)
                        .setVisibility(View.VISIBLE);
                TextView textView = (TextView) addToCollectionButton
                        .findViewById(R.id.addtocollection_button_textview);
                textView.setText(R.string.context_menu_removefromcollection);
            }
            addToCollectionButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionOnDone.run();
                    if (item instanceof Album) {
                        CollectionManager.getInstance().toggleLovedItem((Album) item);
                    } else {
                        CollectionManager.getInstance().toggleLovedItem((Artist) item);
                    }
                }
            });
        }
        if (item instanceof Query) {
            View favoriteButton = view.findViewById(R.id.favorite_button);
            favoriteButton.setVisibility(View.VISIBLE);
            if (DatabaseHelper.getInstance().isItemLoved((Query) item)) {
                favoriteButton.findViewById(R.id.favorite_button_underline)
                        .setVisibility(View.VISIBLE);
                TextView textView = (TextView) favoriteButton
                        .findViewById(R.id.favorite_button_textview);
                textView.setText(R.string.context_menu_unlove);
            }
            favoriteButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionOnDone.run();
                    Query query = (Query) item;
                    CollectionManager.getInstance().toggleLovedItem(query);
                }
            });
        }
        View addToPlaylistButton = view.findViewById(R.id.addtoplaylist_button);
        addToPlaylistButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionOnDone.run();
                ArrayList<Query> queries;
                if (item instanceof Album) {
                    Album album = (Album) item;
                    queries = CollectionUtils.getAlbumTracks(album, collection);
                } else {
                    queries = item.getQueries();
                }
                ArrayList<String> queryKeys = new ArrayList<String>();
                for (Query query : queries) {
                    queryKeys.add(query.getCacheKey());
                }
                ChoosePlaylistDialog dialog = new ChoosePlaylistDialog();
                Bundle args = new Bundle();
                args.putStringArrayList(TomahawkFragment.TOMAHAWK_QUERYARRAY_KEY, queryKeys);
                dialog.setArguments(args);
                dialog.show(activity.getSupportFragmentManager(), null);
            }
        });
        View shareButton = view.findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionOnDone.run();
                Intent shareIntent = ShareUtils
                        .generateShareIntent(item);
                if (shareIntent != null) {
                    activity.startActivity(shareIntent);
                }
            }
        });
        if (item instanceof Album
                || (item instanceof Query && !TextUtils.isEmpty(item.getAlbum().getName()))) {
            View viewAlbumButton = view.findViewById(R.id.view_album_button);
            TextView viewAlbumButtonText =
                    (TextView) viewAlbumButton.findViewById(R.id.view_album_button_text);
            viewAlbumButtonText.setText(activity.getString(R.string.view_album).toUpperCase());
            viewAlbumButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionOnDone.run();
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.TOMAHAWK_ALBUM_KEY,
                            item.getAlbum().getCacheKey());
                    bundle.putString(CollectionManager.COLLECTION_ID, collection.getId());
                    bundle.putInt(ContentHeaderFragment.MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    FragmentUtils.replace(activity, TracksFragment.class, bundle);
                }
            });
        }
        View.OnClickListener artistNameButtonListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionOnDone.run();
                Bundle bundle = new Bundle();
                bundle.putString(TomahawkFragment.TOMAHAWK_ARTIST_KEY,
                        item.getArtist().getCacheKey());
                if (collection != null) {
                    bundle.putString(CollectionManager.COLLECTION_ID, collection.getId());
                }
                bundle.putInt(ContentHeaderFragment.MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
                bundle.putLong(ContentHeaderFragment.CONTAINER_FRAGMENT_ID,
                        TomahawkMainActivity.getSessionUniqueId());
                FragmentUtils.replace(activity, ArtistPagerFragment.class, bundle);
            }
        };
        View artistNameButton;
        if (isPlaybackContextMenu) {
            artistNameButton = activity.getPlaybackPanel().findViewById(R.id.artist_name_button);
        } else {
            artistNameButton = view.findViewById(R.id.artist_name_button);
        }
        if (artistNameButton != null) {
            artistNameButton.setOnClickListener(artistNameButtonListener);
        }
    }

}
