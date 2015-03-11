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
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.BlurTransformation;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.ShareUtils;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;
import org.tomahawk.tomahawk_android.views.PlaybackPanel;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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

    private TomahawkListItem mTomahawkListItem;

    private Collection mCollection;

    private boolean mFromPlaybackFragment;

    private final HashSet<String> mCorrespondingRequestIds = new HashSet<>();

    @SuppressWarnings("unused")
    public void onEventMainThread(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())
                && getView() != null) {
            ImageView albumImageView = (ImageView) getView().findViewById(R.id.album_imageview);
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), albumImageView,
                    mTomahawkListItem.getAlbum().getImage(), Image.getLargeImageSize(), true,
                    false);
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

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        activity.hideActionbar();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        activity.hideActionbar();

        unpackArgs();

        setupCloseButton(view);
        setupContextMenuItems(view);
        setupBlurredBackground(view);

        if (mFromPlaybackFragment) {
            setupPlaybackTextViews(view, activity.getPlaybackPanel());
            activity.getPlaybackPanel().showButtons();
        } else {
            setupTextViews(view);
            setupAlbumArt(view);
            activity.hidePlaybackPanel();
        }
    }

    @Override
    public void onStop() {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        activity.showActionBar(false);

        if (mFromPlaybackFragment) {
            activity.getPlaybackPanel().hideButtons();
        } else {
            activity.showPlaybackPanel(false);
        }

        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    private void unpackArgs() {
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.FROM_PLAYBACKFRAGMENT)) {
                mFromPlaybackFragment = getArguments()
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
                if (mTomahawkListItem instanceof SocialAction) {
                    mTomahawkListItem = ((SocialAction) mTomahawkListItem).getTargetObject();
                }
            }
            if (getArguments().containsKey(TomahawkFragment.COLLECTION_ID)) {
                mCollection = CollectionManager.getInstance()
                        .getCollection(getArguments().getString(TomahawkFragment.COLLECTION_ID));
            }
        }
    }

    private void setupBlurredBackground(final View view) {
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
                        (ImageView) view.findViewById(R.id.background);
                bgImageView.setImageBitmap(bm);

                if (mFromPlaybackFragment) {
                    FrameLayout.LayoutParams params =
                            (FrameLayout.LayoutParams) bgImageView.getLayoutParams();
                    params.bottomMargin = TomahawkApp.getContext().getResources()
                            .getDimensionPixelSize(R.dimen.playback_clear_space_bottom);
                    params.topMargin = TomahawkApp.getContext().getResources()
                            .getDimensionPixelSize(R.dimen.playback_panel_height);
                }
            }
        });
    }

    private void setupCloseButton(View view) {
        View closeButton = view.findViewById(R.id.close_button);
        closeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
                if (!activity.getSlidingUpPanelLayout().isPanelHidden()) {
                    AnimationUtils.fade(activity.getPlaybackPanel(),
                            AnimationUtils.DURATION_CONTEXTMENU, true);
                }
            }
        });
        TextView closeButtonText = (TextView) closeButton.findViewById(R.id.close_button_text);
        closeButtonText.setText(getString(R.string.button_close).toUpperCase());
    }

    private void setupContextMenuItems(View view) {
        final TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();

        // set up "Add to playlist" context menu item
        if (!(mTomahawkListItem instanceof Artist)) {
            View v = TomahawkUtils.ensureInflation(view, R.id.context_menu_addtoplaylist_stub,
                    R.id.context_menu_addtoplaylist);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(R.drawable.ic_action_playlist_light);
            textView.setText(R.string.context_menu_add_to_playlist);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    ArrayList<Query> queries;
                    if (mTomahawkListItem instanceof Album) {
                        Album album = (Album) mTomahawkListItem;
                        queries = CollectionUtils.getAlbumTracks(album, mCollection);
                    } else {
                        queries = mTomahawkListItem.getQueries();
                    }
                    ArrayList<String> queryKeys = new ArrayList<>();
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

        // set up "Add to collection" context menu item
        if (mTomahawkListItem instanceof Album || mTomahawkListItem instanceof Artist) {
            int drawableResId;
            int stringResId;
            if ((mTomahawkListItem instanceof Album
                    && DatabaseHelper.getInstance().isItemLoved((Album) mTomahawkListItem))
                    || (mTomahawkListItem instanceof Artist
                    && DatabaseHelper.getInstance().isItemLoved((Artist) mTomahawkListItem))) {
                drawableResId = R.drawable.ic_action_collection_underlined;
                stringResId = R.string.context_menu_removefromcollection;
            } else {
                drawableResId = R.drawable.ic_action_collection;
                stringResId = R.string.context_menu_addtocollection;
            }
            View v = TomahawkUtils.ensureInflation(view, R.id.context_menu_addtocollection_stub,
                    R.id.context_menu_addtocollection);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(drawableResId);
            textView.setText(stringResId);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (mTomahawkListItem instanceof Album) {
                        CollectionManager.getInstance().toggleLovedItem((Album) mTomahawkListItem);
                    } else {
                        CollectionManager.getInstance().toggleLovedItem((Artist) mTomahawkListItem);
                    }
                }
            });
        }

        // set up "Add to favorites" context menu item
        if (mTomahawkListItem instanceof Query || mTomahawkListItem instanceof PlaylistEntry) {
            final Query query;
            if (mTomahawkListItem instanceof Query) {
                query = (Query) mTomahawkListItem;
            } else {
                query = ((PlaylistEntry) mTomahawkListItem).getQuery();
            }
            int drawableResId;
            int stringResId;
            if (DatabaseHelper.getInstance().isItemLoved(query)) {
                drawableResId = R.drawable.ic_action_favorites_underlined;
                stringResId = R.string.context_menu_unlove;
            } else {
                drawableResId = R.drawable.ic_action_favorites;
                stringResId = R.string.context_menu_love;
            }
            View v = TomahawkUtils.ensureInflation(view, R.id.context_menu_favorite_stub,
                    R.id.context_menu_favorite);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(drawableResId);
            textView.setText(stringResId);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    CollectionManager.getInstance().toggleLovedItem(query);
                }
            });
        }

        // set up "Share" context menu item
        View v = TomahawkUtils
                .ensureInflation(view, R.id.context_menu_share_stub, R.id.context_menu_share);
        TextView textView = (TextView) v.findViewById(R.id.textview);
        ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.ic_action_share);
        textView.setText(R.string.context_menu_share);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                ShareUtils.sendShareIntent(activity, mTomahawkListItem);
            }
        });

        // set up "Remove" context menu item
        if (mTomahawkListItem instanceof PlaylistEntry || mTomahawkListItem instanceof Playlist) {
            final String playlistId = mTomahawkListItem instanceof Playlist
                    ? ((Playlist) mTomahawkListItem).getId()
                    : ((PlaylistEntry) mTomahawkListItem).getPlaylistId();
            if (!DatabaseHelper.LOVEDITEMS_PLAYLIST_ID.equals(playlistId)
                    && DatabaseHelper.getInstance().getEmptyPlaylist(playlistId) != null) {
                int stringResId;
                if (mTomahawkListItem instanceof PlaylistEntry) {
                    stringResId = R.string.context_menu_removefromplaylist;
                } else {
                    stringResId = R.string.context_menu_delete;
                }
                v = TomahawkUtils.ensureInflation(view, R.id.context_menu_remove_stub,
                        R.id.context_menu_remove);
                textView = (TextView) v.findViewById(R.id.textview);
                imageView = (ImageView) v.findViewById(R.id.imageview);
                imageView.setImageResource(R.drawable.ic_player_exit_light);
                textView.setText(stringResId);
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().getSupportFragmentManager().popBackStack();
                        if (mTomahawkListItem instanceof PlaylistEntry) {
                            CollectionManager.getInstance().deletePlaylistEntry(playlistId,
                                    ((PlaylistEntry) mTomahawkListItem).getId());
                        } else {
                            CollectionManager.getInstance().deletePlaylist(playlistId);
                        }
                    }
                });
            }
        }
    }

    private void setupTextViews(View view) {
        if (mTomahawkListItem instanceof Album) {
            View v = TomahawkUtils
                    .ensureInflation(view, R.id.album_name_button_stub, R.id.album_name_button);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            textView.setText(mTomahawkListItem.getName());
            v.setOnClickListener(constructAlbumNameClickListener());
        } else if (mTomahawkListItem instanceof Query
                || mTomahawkListItem instanceof PlaylistEntry
                || mTomahawkListItem instanceof Playlist) {
            View v = TomahawkUtils.ensureInflation(view, R.id.track_name_stub, R.id.track_name);
            TextView textView = (TextView) v;
            textView.setText(mTomahawkListItem.getName());
        }
        if (!(mTomahawkListItem instanceof Playlist)) {
            View v = TomahawkUtils
                    .ensureInflation(view, R.id.artist_name_button_stub, R.id.artist_name_button);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            textView.setText(mTomahawkListItem.getArtist().getName());
        }
    }

    private void setupPlaybackTextViews(View view, PlaybackPanel playbackPanel) {
        if (mTomahawkListItem instanceof Album
                || ((mTomahawkListItem instanceof Query
                || mTomahawkListItem instanceof PlaylistEntry)
                && !TextUtils.isEmpty(mTomahawkListItem.getAlbum().getName()))) {
            View v = TomahawkUtils
                    .ensureInflation(view, R.id.view_album_button_stub, R.id.view_album_button);
            TextView viewAlbumButtonText = (TextView) v.findViewById(R.id.textview);
            viewAlbumButtonText.setText(
                    TomahawkApp.getContext().getString(R.string.view_album).toUpperCase());
            v.setOnClickListener(constructAlbumNameClickListener());
        }
        if (!(mTomahawkListItem instanceof Playlist)) {
            View artistNameButton = playbackPanel.findViewById(R.id.artist_name_button);
            artistNameButton.setOnClickListener(constructArtistNameClickListener());
        }
    }

    private void setupAlbumArt(View view) {
        if (mTomahawkListItem instanceof Album
                || ((mTomahawkListItem instanceof Query
                || mTomahawkListItem instanceof PlaylistEntry)
                && !TextUtils.isEmpty(mTomahawkListItem.getAlbum().getName()))) {
            View v = TomahawkUtils.ensureInflation(view, R.id.context_menu_albumart_stub,
                    R.id.context_menu_albumart);

            // load albumart image
            ImageView albumImageView = (ImageView) v.findViewById(R.id.album_imageview);
            if (mTomahawkListItem.getAlbum().getImage() != null) {
                TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), albumImageView,
                        mTomahawkListItem.getAlbum().getImage(), Image.getLargeImageSize(), true,
                        false);
            } else {
                mCorrespondingRequestIds
                        .add(InfoSystem.getInstance().resolve(mTomahawkListItem.getAlbum()));
            }

            // set text on "view album"-button and set up click listener
            View viewAlbumButton = view.findViewById(R.id.view_album_button);
            TextView viewAlbumButtonText =
                    (TextView) viewAlbumButton.findViewById(R.id.textview);
            viewAlbumButtonText.setText(
                    TomahawkApp.getContext().getString(R.string.view_album).toUpperCase());
            viewAlbumButton.setOnClickListener(constructAlbumNameClickListener());
        }
    }

    private View.OnClickListener constructArtistNameClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                Bundle bundle = new Bundle();
                bundle.putString(TomahawkFragment.ARTIST,
                        mTomahawkListItem.getArtist().getCacheKey());
                if (mCollection != null) {
                    bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
                }
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC_PAGER);
                bundle.putLong(TomahawkFragment.CONTAINER_FRAGMENT_ID,
                        TomahawkMainActivity.getSessionUniqueId());
                FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                        ArtistPagerFragment.class, bundle);
            }
        };
    }

    private View.OnClickListener constructAlbumNameClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                Bundle bundle = new Bundle();
                bundle.putString(TomahawkFragment.ALBUM,
                        mTomahawkListItem.getAlbum().getCacheKey());
                if (mCollection != null) {
                    bundle.putString(TomahawkFragment.COLLECTION_ID, mCollection.getId());
                }
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_DYNAMIC);
                FragmentUtils.replace((TomahawkMainActivity) getActivity(),
                        TracksFragment.class, bundle);
            }
        };
    }
}
