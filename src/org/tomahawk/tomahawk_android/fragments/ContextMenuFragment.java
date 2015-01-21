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
import org.tomahawk.tomahawk_android.utils.BlurTransformation;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.ShareUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;

import de.greenrobot.event.EventBus;

/**
 * A {@link DialogFragment} which emulates the appearance and behaviour of the standard context menu
 * dialog, so that it is fully customizable.
 */
public class ContextMenuFragment extends Fragment {

    //the {@link TomahawkListItem} this {@link FakeContextMenuDialog} is associated with
    private TomahawkListItem mTomahawkListItem;

    protected Collection mCollection;

    protected HashSet<String> mCorrespondingRequestIds = new HashSet<String>();

    public static interface Action {

        public void run();
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())
                && getView() != null) {
            ImageView albumImageView =
                    (ImageView) getView().findViewById(R.id.album_imageview);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), albumImageView,
                    mTomahawkListItem.getAlbum().getImage(), Image.getLargeImageSize(),
                    true, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        return inflater.inflate(R.layout.context_menu_fragment, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onResume() {
        super.onResume();

        Object contextItem = null;
        boolean showDelete = false;
        boolean fromPlaybackFragment = false;

        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.ALBUM)) {
                contextItem = Album.getAlbumByKey(
                        getArguments().getString(TomahawkFragment.ALBUM));
                if (contextItem == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
            } else if (getArguments().containsKey(TomahawkFragment.PLAYLIST)) {
                contextItem = Playlist.getPlaylistById(getArguments()
                        .getString(TomahawkFragment.PLAYLIST));
                if (contextItem == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
                String playlistId = getArguments().getString(TomahawkFragment.PLAYLIST);
                contextItem = DatabaseHelper.getInstance().getPlaylist(playlistId);
                if (contextItem == null) {
                    contextItem = Playlist.getPlaylistById(playlistId);
                    if (contextItem == null) {
                        getActivity().getSupportFragmentManager().popBackStack();
                        return;
                    }
                }
            } else if (getArguments().containsKey(TomahawkFragment.ARTIST)) {
                contextItem = Artist.getArtistByKey(
                        getArguments().getString(TomahawkFragment.ARTIST));
                if (contextItem == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
            }
            if (getArguments().containsKey(TomahawkFragment.SHOWDELETE)) {
                showDelete = getArguments().getBoolean(TomahawkFragment.SHOWDELETE);
            }
            if (getArguments().containsKey(TomahawkFragment.FROM_PLAYBACKFRAGMENT)) {
                fromPlaybackFragment = getArguments()
                        .getBoolean(TomahawkFragment.FROM_PLAYBACKFRAGMENT);
            }
            if (getArguments().containsKey(TomahawkFragment.TOMAHAWKLISTITEM_TYPE)
                    && getArguments().containsKey(TomahawkFragment.TOMAHAWKLISTITEM)) {
                String type = getArguments()
                        .getString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE);
                switch (type) {
                    case TomahawkFragment.ALBUM:
                        mTomahawkListItem = Album.getAlbumByKey(getArguments()
                                .getString(TomahawkFragment.TOMAHAWKLISTITEM));
                        break;
                    case TomahawkFragment.PLAYLIST:
                        mTomahawkListItem = Playlist.getPlaylistById(getArguments()
                                .getString(TomahawkFragment.TOMAHAWKLISTITEM));
                        break;
                    case TomahawkFragment.ARTIST:
                        mTomahawkListItem = Artist.getArtistByKey(getArguments()
                                .getString(TomahawkFragment.TOMAHAWKLISTITEM));
                        break;
                    case TomahawkFragment.QUERY:
                        mTomahawkListItem = Query.getQueryByKey(getArguments()
                                .getString(TomahawkFragment.TOMAHAWKLISTITEM));
                        break;
                    case TomahawkFragment.SOCIALACTION:
                        mTomahawkListItem = SocialAction.getSocialActionById(getArguments()
                                .getString(TomahawkFragment.TOMAHAWKLISTITEM));
                        break;
                    case TomahawkFragment.PLAYLISTENTRY:
                        mTomahawkListItem = PlaylistEntry.getPlaylistEntryByKey(getArguments()
                                .getString(TomahawkFragment.TOMAHAWKLISTITEM));
                        break;
                }
                if (mTomahawkListItem == null) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    return;
                }
            }
            if (getArguments().containsKey(TomahawkFragment.COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(TomahawkFragment.COLLECTION_ID));
            }
        }
        if (mTomahawkListItem instanceof SocialAction) {
            mTomahawkListItem = ((SocialAction) mTomahawkListItem).getTargetObject();
        }

        //Set blurred background image
        if (getView() != null) {
            final View rootView = getActivity().findViewById(R.id.sliding_layout);
            TomahawkUtils.afterViewGlobalLayout(new TomahawkUtils.ViewRunnable(rootView) {
                @Override
                public void run() {
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
                    bundle.putString(TomahawkFragment.ALBUM,
                            mTomahawkListItem.getCacheKey());
                    bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                            TracksFragment.class, bundle);
                }
            });
            TextView albumTextView = (TextView) albumNameButton.findViewById(R.id.album_name);
            albumTextView.setText(mTomahawkListItem.getName());
        } else if (mTomahawkListItem instanceof Query
                || mTomahawkListItem instanceof PlaylistEntry
                || mTomahawkListItem instanceof Playlist) {
            TextView itemTextView = (TextView) getView().findViewById(R.id.track_name);
            itemTextView.setVisibility(View.VISIBLE);
            itemTextView.setText(mTomahawkListItem.getName());
        }
        if (!(mTomahawkListItem instanceof Playlist)) {
            TextView artistTextView = (TextView) getView().findViewById(R.id.artist_name);
            artistTextView.setText(mTomahawkListItem.getArtist().getName());
        }

        //Set up album image and stuff
        if (mTomahawkListItem instanceof Album
                || ((mTomahawkListItem instanceof Query
                || mTomahawkListItem instanceof PlaylistEntry)
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
                mCorrespondingRequestIds
                        .add(InfoSystem.getInstance().resolve(mTomahawkListItem.getAlbum()));
            }
        }
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);

        super.onStop();
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
        if (item instanceof Query || item instanceof PlaylistEntry) {
            View favoriteButton = view.findViewById(R.id.favorite_button);
            favoriteButton.setVisibility(View.VISIBLE);
            final Query query;
            if (item instanceof Query) {
                query = (Query) item;
            } else {
                query = ((PlaylistEntry) item).getQuery();
            }
            if (DatabaseHelper.getInstance().isItemLoved(query)) {
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
                    CollectionManager.getInstance().toggleLovedItem(query);
                }
            });
        }
        if (!(item instanceof Artist)) {
            View addToPlaylistButton = view.findViewById(R.id.addtoplaylist_button);
            addToPlaylistButton.setVisibility(View.VISIBLE);
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
                    Bundle bundle = new Bundle();
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_STATIC);
                    bundle.putStringArrayList(TomahawkFragment.QUERYARRAY, queryKeys);
                    FragmentUtils.replace(activity, PlaylistsFragment.class, bundle);
                }
            });
        }
        View shareButton = view.findViewById(R.id.share_button);
        shareButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                actionOnDone.run();
                ShareUtils.sendShareIntent(activity, item);
            }
        });
        if (item instanceof PlaylistEntry || item instanceof Playlist) {
            final String playlistId = item instanceof Playlist
                    ? ((Playlist) item).getId()
                    : ((PlaylistEntry) item).getPlaylistId();
            if (!DatabaseHelper.LOVEDITEMS_PLAYLIST_ID.equals(playlistId)
                    && DatabaseHelper.getInstance().getEmptyPlaylist(playlistId) != null) {
                View removeButton = view.findViewById(R.id.remove_button);
                TextView removeButtonTextView =
                        (TextView) removeButton.findViewById(R.id.remove_button_textview);
                if (item instanceof PlaylistEntry) {
                    removeButtonTextView.setText(R.string.context_menu_removefromplaylist);
                } else {
                    removeButtonTextView.setText(R.string.context_menu_delete);
                }
                removeButton.setVisibility(View.VISIBLE);
                removeButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        actionOnDone.run();
                        if (item instanceof PlaylistEntry) {
                            CollectionManager.getInstance().deletePlaylistEntry(playlistId,
                                    ((PlaylistEntry) item).getId());
                        } else {
                            CollectionManager.getInstance().deletePlaylist(playlistId);
                        }
                    }
                });
            }
        }

        if (item instanceof Album
                || ((item instanceof Query || item instanceof PlaylistEntry)
                && !TextUtils.isEmpty(item.getAlbum().getName()))) {
            View viewAlbumButton = view.findViewById(R.id.view_album_button);
            TextView viewAlbumButtonText =
                    (TextView) viewAlbumButton.findViewById(R.id.view_album_button_text);
            viewAlbumButtonText.setText(activity.getString(R.string.view_album).toUpperCase());
            viewAlbumButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionOnDone.run();
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.ALBUM,
                            item.getAlbum().getCacheKey());
                    if (collection != null) {
                        bundle.putString(TomahawkFragment.COLLECTION_ID, collection.getId());
                    }
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                    FragmentUtils.replace(activity, TracksFragment.class, bundle);
                }
            });
        }
        if (!(item instanceof Playlist)) {
            View.OnClickListener artistNameButtonListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    actionOnDone.run();
                    Bundle bundle = new Bundle();
                    bundle.putString(TomahawkFragment.ARTIST,
                            item.getArtist().getCacheKey());
                    if (collection != null) {
                        bundle.putString(TomahawkFragment.COLLECTION_ID, collection.getId());
                    }
                    bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                            ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
                    bundle.putLong(TomahawkFragment.CONTAINER_FRAGMENT_ID,
                            TomahawkMainActivity.getSessionUniqueId());
                    FragmentUtils.replace(activity, ArtistPagerFragment.class, bundle);
                }
            };
            View artistNameButton;
            if (isPlaybackContextMenu) {
                artistNameButton = activity.getPlaybackPanel()
                        .findViewById(R.id.artist_name_button);
            } else {
                artistNameButton = view.findViewById(R.id.artist_name_button);
                artistNameButton.setVisibility(View.VISIBLE);
            }
            if (artistNameButton != null) {
                artistNameButton.setOnClickListener(artistNameButtonListener);
            }
        }
    }

}
