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

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
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
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.greenrobot.event.EventBus;

/**
 * A {@link DialogFragment} which emulates the appearance and behaviour of the standard context menu
 * dialog, so that it is fully customizable.
 */
public class ContextMenuFragment extends Fragment {

    private Album mAlbum;

    private Artist mArtist;

    private Playlist mPlaylist;

    private PlaylistEntry mPlaylistEntry;

    private Query mQuery;

    private Collection mCollection;

    private boolean mFromPlaybackFragment;

    private final HashSet<String> mCorrespondingRequestIds = new HashSet<>();

    @SuppressWarnings("unused")
    public void onEventMainThread(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())
                && getView() != null) {
            ImageView albumImageView = (ImageView) getView().findViewById(R.id.album_imageview);
            Album album;
            if (mAlbum != null) {
                album = mAlbum;
            } else if (mQuery != null) {
                album = mQuery.getAlbum();
            } else {
                album = mPlaylistEntry.getAlbum();
            }
            TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), albumImageView,
                    album.getImage(), Image.getLargeImageSize(), true, false);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        unpackArgs();
        int layoutResId = mFromPlaybackFragment ? R.layout.context_menu_fragment_playback
                : R.layout.context_menu_fragment;
        return inflater.inflate(layoutResId, container, false);
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
                String type = getArguments().getString(TomahawkFragment.TOMAHAWKLISTITEM_TYPE);
                String key = getArguments().getString(TomahawkFragment.TOMAHAWKLISTITEM);
                switch (type) {
                    case TomahawkFragment.ALBUM:
                        mAlbum = Album.getByKey(key);
                        break;
                    case TomahawkFragment.PLAYLIST:
                        mPlaylist = Playlist.getByKey(key);
                        break;
                    case TomahawkFragment.ARTIST:
                        mArtist = Artist.getByKey(key);
                        break;
                    case TomahawkFragment.QUERY:
                        mQuery = Query.getByKey(key);
                        break;
                    case TomahawkFragment.SOCIALACTION:
                        SocialAction socialAction = SocialAction.getByKey(key);
                        Object targetObject = socialAction.getTargetObject();
                        if (targetObject instanceof Artist) {
                            mArtist = (Artist) targetObject;
                        } else if (targetObject instanceof Album) {
                            mAlbum = (Album) targetObject;
                        } else if (targetObject instanceof Query) {
                            mQuery = (Query) targetObject;
                        } else if (targetObject instanceof Playlist) {
                            mPlaylist = (Playlist) targetObject;
                        }
                        break;
                    case TomahawkFragment.PLAYLISTENTRY:
                        mPlaylistEntry = PlaylistEntry.getByKey(key);
                        break;
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
        if (mAlbum != null || mQuery != null || mPlaylistEntry != null || mPlaylist != null) {
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
                    if (mAlbum != null) {
                        mCollection.getAlbumTracks(mAlbum, false)
                                .done(new DoneCallback<Set<Query>>() {
                                    @Override
                                    public void onDone(Set<Query> result) {
                                        showAddToPlaylist(activity, new ArrayList<>(result));
                                    }
                                });
                    } else if (mQuery != null) {
                        ArrayList<Query> queries = new ArrayList<>();
                        queries.add(mQuery);
                        showAddToPlaylist(activity, queries);
                    } else if (mPlaylistEntry != null) {
                        ArrayList<Query> queries = new ArrayList<>();
                        queries.add(mPlaylistEntry.getQuery());
                        showAddToPlaylist(activity, queries);
                    } else if (mPlaylist != null) {
                        showAddToPlaylist(activity, mPlaylist.getQueries());
                    }
                }
            });
        }

        // set up "Add to collection" context menu item
        if (mAlbum != null || mArtist != null) {
            int drawableResId;
            int stringResId;
            if ((mAlbum != null && DatabaseHelper.getInstance().isItemLoved(mAlbum))
                    || (mArtist != null && DatabaseHelper.getInstance().isItemLoved(mArtist))) {
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
                    if (mAlbum != null) {
                        CollectionManager.getInstance().toggleLovedItem(mAlbum);
                    } else {
                        CollectionManager.getInstance().toggleLovedItem(mArtist);
                    }
                }
            });
        }

        // set up "Add to favorites" context menu item
        if (mQuery != null || mPlaylistEntry != null) {
            final Query query = mQuery != null ? mQuery : mPlaylistEntry.getQuery();
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
                if (mAlbum != null) {
                    ShareUtils.sendShareIntent(activity, mAlbum);
                } else if (mArtist != null) {
                    ShareUtils.sendShareIntent(activity, mArtist);
                } else if (mQuery != null) {
                    ShareUtils.sendShareIntent(activity, mQuery);
                } else if (mPlaylistEntry != null) {
                    ShareUtils.sendShareIntent(activity, mPlaylistEntry.getQuery());
                } else if (mPlaylist != null) {
                    ShareUtils.sendShareIntent(activity, mPlaylist);
                }
            }
        });

        // set up "Remove" context menu item
        if (mPlaylist != null || mPlaylistEntry != null) {
            final String playlistId = mPlaylist != null ? mPlaylist.getId()
                    : mPlaylistEntry.getPlaylistId();
            if (!DatabaseHelper.LOVEDITEMS_PLAYLIST_ID.equals(playlistId)
                    && DatabaseHelper.getInstance().getEmptyPlaylist(playlistId) != null) {
                int stringResId;
                if (mPlaylistEntry != null) {
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
                        if (mPlaylistEntry != null) {
                            CollectionManager.getInstance().deletePlaylistEntry(playlistId,
                                    mPlaylistEntry.getId());
                        } else {
                            CollectionManager.getInstance().deletePlaylist(playlistId);
                        }
                    }
                });
            }
        }
    }

    private void showAddToPlaylist(TomahawkMainActivity activity, List<Query> queries) {
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

    private void setupTextViews(View view) {
        if (mAlbum != null) {
            View v = TomahawkUtils
                    .ensureInflation(view, R.id.album_name_button_stub, R.id.album_name_button);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            textView.setText(mAlbum.getName());
            v.setOnClickListener(constructAlbumNameClickListener(mAlbum.getCacheKey()));
        } else if (mQuery != null || mPlaylistEntry != null || mPlaylist != null) {
            View v = TomahawkUtils.ensureInflation(view, R.id.track_name_stub, R.id.track_name);
            TextView textView = (TextView) v;
            if (mQuery != null) {
                textView.setText(mQuery.getName());
            } else if (mPlaylistEntry != null) {
                textView.setText(mPlaylistEntry.getName());
            } else if (mPlaylist != null) {
                textView.setText(mPlaylist.getName());
            }
        }
        if (mAlbum != null || mQuery != null || mPlaylistEntry != null || mArtist != null) {
            View v = TomahawkUtils
                    .ensureInflation(view, R.id.artist_name_button_stub, R.id.artist_name_button);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            String cacheKey;
            if (mQuery != null) {
                textView.setText(mQuery.getArtist().getPrettyName());
                cacheKey = mQuery.getArtist().getCacheKey();
            } else if (mAlbum != null) {
                textView.setText(mAlbum.getArtist().getPrettyName());
                cacheKey = mAlbum.getArtist().getCacheKey();
            } else if (mPlaylistEntry != null) {
                textView.setText(mPlaylistEntry.getArtist().getPrettyName());
                cacheKey = mPlaylistEntry.getArtist().getCacheKey();
            } else {
                textView.setText(mArtist.getPrettyName());
                cacheKey = mArtist.getCacheKey();
            }
            v.setOnClickListener(constructArtistNameClickListener(cacheKey));
        }
    }

    private void setupPlaybackTextViews(View view, PlaybackPanel playbackPanel) {
        if (mAlbum != null
                || (mQuery != null
                && !TextUtils.isEmpty(mQuery.getAlbum().getName()))
                || (mPlaylistEntry != null
                && !TextUtils.isEmpty(mPlaylistEntry.getQuery().getAlbum().getName()))) {
            View v = TomahawkUtils
                    .ensureInflation(view, R.id.view_album_button_stub, R.id.view_album_button);
            TextView viewAlbumButtonText = (TextView) v.findViewById(R.id.textview);
            viewAlbumButtonText.setText(
                    TomahawkApp.getContext().getString(R.string.view_album).toUpperCase());
            String cacheKey;
            if (mAlbum != null) {
                cacheKey = mAlbum.getCacheKey();
            } else if (mQuery != null) {
                cacheKey = mQuery.getAlbum().getCacheKey();
            } else {
                cacheKey = mPlaylistEntry.getAlbum().getCacheKey();
            }
            v.setOnClickListener(constructAlbumNameClickListener(cacheKey));
        }
        if (mAlbum != null || mQuery != null || mPlaylistEntry != null || mArtist != null) {
            View artistNameButton = playbackPanel.findViewById(R.id.artist_name_button);
            String cacheKey;
            if (mAlbum != null) {
                cacheKey = mAlbum.getArtist().getCacheKey();
            } else if (mQuery != null) {
                cacheKey = mQuery.getArtist().getCacheKey();
            } else if (mPlaylistEntry != null) {
                cacheKey = mPlaylistEntry.getArtist().getCacheKey();
            } else {
                cacheKey = mArtist.getCacheKey();
            }
            artistNameButton.setOnClickListener(constructArtistNameClickListener(cacheKey));
        }
    }

    private void setupAlbumArt(View view) {
        if (mAlbum != null
                || (mQuery != null
                && !TextUtils.isEmpty(mQuery.getAlbum().getName()))
                || (mPlaylistEntry != null
                && !TextUtils.isEmpty(mPlaylistEntry.getQuery().getAlbum().getName()))) {
            View v = TomahawkUtils.ensureInflation(view, R.id.context_menu_albumart_stub,
                    R.id.context_menu_albumart);

            // load albumart image
            ImageView albumImageView = (ImageView) v.findViewById(R.id.album_imageview);
            Album album;
            String cacheKey;
            if (mAlbum != null) {
                album = mAlbum;
                cacheKey = mAlbum.getCacheKey();
            } else if (mQuery != null) {
                album = mQuery.getAlbum();
                cacheKey = mQuery.getAlbum().getCacheKey();
            } else {
                album = mPlaylistEntry.getAlbum();
                cacheKey = mPlaylistEntry.getAlbum().getCacheKey();
            }
            if (album.getImage() != null) {
                TomahawkUtils.loadImageIntoImageView(TomahawkApp.getContext(), albumImageView,
                        album.getImage(), Image.getLargeImageSize(), true, false);
            } else {
                String requestId = InfoSystem.getInstance().resolve(album);
                if (requestId != null) {
                    mCorrespondingRequestIds.add(requestId);
                }
            }

            // set text on "view album"-button and set up click listener
            View viewAlbumButton = view.findViewById(R.id.view_album_button);
            TextView viewAlbumButtonText =
                    (TextView) viewAlbumButton.findViewById(R.id.textview);
            viewAlbumButtonText.setText(
                    TomahawkApp.getContext().getString(R.string.view_album).toUpperCase());
            viewAlbumButton.setOnClickListener(constructAlbumNameClickListener(cacheKey));
        }
    }

    private View.OnClickListener constructArtistNameClickListener(final String cacheKey) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                Bundle bundle = new Bundle();
                bundle.putString(TomahawkFragment.ARTIST, cacheKey);
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

    private View.OnClickListener constructAlbumNameClickListener(final String cachekey) {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                Bundle bundle = new Bundle();
                bundle.putString(TomahawkFragment.ALBUM, cachekey);
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
