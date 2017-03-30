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

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.collection.UserCollection;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.ImageUtils;
import org.tomahawk.libtomahawk.utils.ViewUtils;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.services.PlaybackService;
import org.tomahawk.tomahawk_android.utils.AnimationUtils;
import org.tomahawk.tomahawk_android.utils.BlurTransformation;
import org.tomahawk.tomahawk_android.utils.FragmentUtils;
import org.tomahawk.tomahawk_android.utils.IdGenerator;
import org.tomahawk.tomahawk_android.utils.PlaybackManager;
import org.tomahawk.tomahawk_android.utils.ShareUtils;
import org.tomahawk.tomahawk_android.views.PlaybackPanel;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import de.greenrobot.event.EventBus;

/**
 * A {@link DialogFragment} which emulates the appearance and behaviour of the standard context menu
 * dialog, so that it is fully customizable.
 */
public class ContextMenuFragment extends Fragment {

    private final static String TAG = ContextMenuFragment.class.getSimpleName();

    private Album mAlbum;

    private Artist mArtist;

    private Playlist mPlaylist;

    private StationPlaylist mStationPlaylist;

    private PlaylistEntry mPlaylistEntry;

    private Query mQuery;

    private Collection mCollection;

    private boolean mFromPlaybackFragment;

    private boolean mHideRemoveButton;

    private final HashSet<String> mCorrespondingRequestIds = new HashSet<>();

    @SuppressWarnings("unused")
    public void onEventMainThread(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            setupAlbumArt(getView());
            setupContextMenuItems(getView());
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        unpackArgs();
        resolveItems();
        int layoutResId = mFromPlaybackFragment ? R.layout.context_menu_fragment_playback
                : R.layout.context_menu_fragment;
        return inflater.inflate(layoutResId, container, false);
    }

    @Override
    public void onStart() {
        super.onStart();

        EventBus.getDefault().register(this);
    }

    @Override
    public void onViewCreated(final View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupCloseButton(view);
        setupContextMenuItems(view);
        setupBlurredBackground(view);

        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (mFromPlaybackFragment) {
            setupPlaybackTextViews(view, activity.getPlaybackPanel());
            activity.getPlaybackPanel().showButtons();
            activity.getPlaybackPanel().hideStationContainer();
        } else {
            setupTextViews(view);
            setupAlbumArt(view);
            activity.hidePlaybackPanel();
        }
    }

    @Override
    public void onStop() {
        TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();
        if (mFromPlaybackFragment) {
            activity.getPlaybackPanel().hideButtons();
            activity.getPlaybackPanel().showStationContainer();
        } else {
            activity.showPlaybackPanel(false);
        }

        EventBus.getDefault().unregister(this);

        super.onStop();
    }

    private void unpackArgs() {
        if (getArguments() != null) {
            if (getArguments().containsKey(TomahawkFragment.HIDE_REMOVE_BUTTON)) {
                mHideRemoveButton = getArguments()
                        .getBoolean(TomahawkFragment.HIDE_REMOVE_BUTTON);
            }
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
                    case TomahawkFragment.STATION:
                        mStationPlaylist = (StationPlaylist) Playlist.getByKey(key);
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
                String collectionId = getArguments().getString(TomahawkFragment.COLLECTION_ID);
                mCollection = CollectionManager.get().getCollection(collectionId);
            }
        }
    }

    private void resolveItems() {
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User result) {
                if (mCollection != null && mAlbum != null) {
                    String requestId = InfoSystem.get().resolve(mAlbum);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }
                if (mPlaylist != null && mPlaylist.getUserId() != null
                        && !mPlaylist.getUserId().equals(result.getId())) {
                    String requestId = InfoSystem.get().resolve(mPlaylist);
                    if (requestId != null) {
                        mCorrespondingRequestIds.add(requestId);
                    }
                }

            }
        });
    }

    private void setupBlurredBackground(final View view) {
        final View rootView = getActivity().findViewById(R.id.sliding_layout);
        ViewUtils.afterViewGlobalLayout(new ViewUtils.ViewRunnable(rootView) {
            @Override
            public void run() {
                Bitmap bm = Bitmap.createBitmap(rootView.getWidth(),
                        rootView.getHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bm);
                rootView.draw(canvas);
                bm = Bitmap.createScaledBitmap(bm, bm.getWidth() / 4,
                        bm.getHeight() / 4, true);
                bm = new BlurTransformation(getContext(), 25).transform(bm);

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
                if (activity.getSlidingUpPanelLayout().getPanelState()
                        != SlidingUpPanelLayout.PanelState.HIDDEN) {
                    AnimationUtils.fade(activity.getPlaybackPanel(),
                            AnimationUtils.DURATION_CONTEXTMENU, true);
                }
            }
        });
        TextView closeButtonText = (TextView) closeButton.findViewById(R.id.close_button_text);
        closeButtonText.setText(getString(R.string.button_close).toUpperCase());
    }

    private void setupContextMenuItems(final View view) {
        if (view == null) {
            return;
        }

        final TomahawkMainActivity activity = (TomahawkMainActivity) getActivity();

        // set up "Add to playlist" context menu item
        if (mAlbum != null) {
            mCollection.getAlbumTracks(mAlbum).done(new DoneCallback<Playlist>() {
                @Override
                public void onDone(Playlist result) {
                    List<Query> queries = null;
                    if (result != null && result.isFilled()) {
                        queries = new ArrayList<>();
                        for (PlaylistEntry entry : result.getEntries()) {
                            queries.add(entry.getQuery());
                        }
                    }
                    setupAddToPlaylistButton(view, queries);
                }
            });
        } else if (mPlaylist != null) {
            List<Query> queries = null;
            if (mPlaylist.isFilled()) {
                queries = new ArrayList<>();
                for (PlaylistEntry entry : mPlaylist.getEntries()) {
                    queries.add(entry.getQuery());
                }
            }
            setupAddToPlaylistButton(view, queries);
        } else if (mQuery != null || mPlaylistEntry != null) {
            Query q = mQuery;
            if (mPlaylistEntry != null) {
                q = mPlaylistEntry.getQuery();
            }
            ArrayList<Query> queries = new ArrayList<>();
            queries.add(q);
            setupAddToPlaylistButton(view, queries);
        }

        // set up "Play Next" context menu item
            if (mAlbum != null || mPlaylist != null || mPlaylistEntry != null
                || mQuery != null) {
            setupPlayNextButton(view, mAlbum, mPlaylist, mPlaylistEntry, mQuery);
        }
        
        // set up "Create station" context menu item
        if (mAlbum != null || mArtist != null || mPlaylist != null || mPlaylistEntry != null
                || mQuery != null) {
            setupCreateStationButton(view, mAlbum, mArtist, mPlaylist, mPlaylistEntry, mQuery);
        }

        // set up "Add to collection" context menu item
        if (mAlbum != null || mArtist != null) {
            int drawableResId;
            int stringResId;
            UserCollection userCollection = CollectionManager.get().getUserCollection();
            if ((mAlbum != null && userCollection.isLoved(mAlbum))
                    || (mArtist != null && userCollection.isLoved(mArtist))) {
                drawableResId = R.drawable.ic_action_collection_underlined;
                stringResId = R.string.context_menu_removefromcollection;
            } else {
                drawableResId = R.drawable.ic_action_collection;
                stringResId = R.string.context_menu_addtocollection;
            }
            View v = ViewUtils.ensureInflation(view, R.id.context_menu_addtocollection_stub,
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
                        CollectionManager.get().toggleLovedItem(mAlbum);
                    } else {
                        CollectionManager.get().toggleLovedItem(mArtist);
                    }
                }
            });
        }

        // set up "Add to favorites" context menu item
        if (mQuery != null || mPlaylistEntry != null) {
            final Query query = mQuery != null ? mQuery : mPlaylistEntry.getQuery();
            int drawableResId;
            int stringResId;
            if (DatabaseHelper.get().isItemLoved(query)) {
                drawableResId = R.drawable.ic_action_favorites_underlined;
                stringResId = R.string.context_menu_unlove;
            } else {
                drawableResId = R.drawable.ic_action_favorites;
                stringResId = R.string.context_menu_love;
            }
            View v = ViewUtils.ensureInflation(view, R.id.context_menu_favorite_stub,
                    R.id.context_menu_favorite);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(drawableResId);
            textView.setText(stringResId);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    CollectionManager.get().toggleLovedItem(query);
                }
            });
        }

        // set up "Share" context menu item
        if (mStationPlaylist == null) {
            View v = ViewUtils.ensureInflation(
                    view, R.id.context_menu_share_stub, R.id.context_menu_share);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(R.drawable.ic_action_share);
            textView.setText(R.string.context_menu_share);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    boolean error = false;
                    if (mAlbum != null) {
                        ShareUtils.sendShareIntent(activity, mAlbum);
                    } else if (mArtist != null) {
                        ShareUtils.sendShareIntent(activity, mArtist);
                    } else if (mQuery != null) {
                        ShareUtils.sendShareIntent(activity, mQuery);
                    } else if (mPlaylistEntry != null) {
                        ShareUtils.sendShareIntent(activity, mPlaylistEntry.getQuery());
                    } else if (mPlaylist != null) {
                        if (mPlaylist.getHatchetId() == null) {
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(TomahawkApp.getContext(),
                                            R.string.contest_menu_share_error, Toast.LENGTH_LONG)
                                            .show();
                                }
                            });
                            error = true;
                        } else {
                            ShareUtils.sendShareIntent(activity, mPlaylist);
                        }
                    }
                    if (!error) {
                        getActivity().getSupportFragmentManager().popBackStack();
                    }
                }
            });
        }

        // set up "Remove" context menu item
        if (!mHideRemoveButton && (mPlaylist != null || mPlaylistEntry != null
                || mStationPlaylist != null)) {
            int stringResId;
            if (mPlaylistEntry != null) {
                stringResId = R.string.context_menu_removefromplaylist;
            } else {
                stringResId = R.string.context_menu_delete;
            }
            View v = ViewUtils.ensureInflation(view, R.id.context_menu_remove_stub,
                    R.id.context_menu_remove);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(R.drawable.ic_navigation_close);
            textView.setText(stringResId);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (mStationPlaylist != null) {
                        DatabaseHelper.get().deleteStation(mStationPlaylist);
                    } else {
                        String localPlaylistId = mPlaylist != null ? mPlaylist.getId()
                                : mPlaylistEntry.getPlaylistId();
                        if (mPlaylistEntry != null) {
                            CollectionManager.get().deletePlaylistEntry(localPlaylistId,
                                    mPlaylistEntry.getId());
                        } else {
                            CollectionManager.get().deletePlaylist(localPlaylistId);
                        }
                    }
                }
            });
        }

        // set up "Add to queue" context menu item
        if (mAlbum != null || mQuery != null || mPlaylistEntry != null || mPlaylist != null) {
            int drawableResId = R.drawable.ic_action_queue;
            int stringResId = R.string.context_menu_add_to_queue;
            View v = ViewUtils.ensureInflation(view, R.id.context_menu_addtoqueue_stub,
                    R.id.context_menu_addtoqueue);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(drawableResId);
            textView.setText(stringResId);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (mAlbum != null) {
                        mCollection.getAlbumTracks(mAlbum).done(new DoneCallback<Playlist>() {
                            @Override
                            public void onDone(Playlist playlist) {
                                ArrayList<String> queryKeys = new ArrayList<>();
                                if (playlist != null) {
                                    for (PlaylistEntry entry : playlist.getEntries()) {
                                        queryKeys.add(entry.getQuery().getCacheKey());
                                    }
                                }
                                Bundle extras = new Bundle();
                                extras.putStringArrayList(TomahawkFragment.QUERYARRAY, queryKeys);
                                getActivity().getSupportMediaController()
                                        .getTransportControls().sendCustomAction(
                                        PlaybackService.ACTION_ADD_QUERIES_TO_QUEUE, extras);
                            }
                        });
                    } else if (mQuery != null) {
                        Bundle extras = new Bundle();
                        extras.putString(TomahawkFragment.QUERY, mQuery.getCacheKey());
                        getActivity().getSupportMediaController()
                                .getTransportControls().sendCustomAction(
                                PlaybackService.ACTION_ADD_QUERY_TO_QUEUE, extras);
                    } else if (mPlaylistEntry != null) {
                        Bundle extras = new Bundle();
                        extras.putString(TomahawkFragment.QUERY,
                                mPlaylistEntry.getQuery().getCacheKey());
                        getActivity().getSupportMediaController()
                                .getTransportControls().sendCustomAction(
                                PlaybackService.ACTION_ADD_QUERY_TO_QUEUE, extras);
                    } else if (mPlaylist != null) {
                        ArrayList<String> queryKeys = new ArrayList<>();
                        if (mPlaylist != null) {
                            for (PlaylistEntry entry : mPlaylist.getEntries()) {
                                queryKeys.add(entry.getQuery().getCacheKey());
                            }
                        }
                        Bundle extras = new Bundle();
                        extras.putStringArrayList(TomahawkFragment.QUERYARRAY, queryKeys);
                        getActivity().getSupportMediaController()
                                .getTransportControls().sendCustomAction(
                                PlaybackService.ACTION_ADD_QUERIES_TO_QUEUE, extras);
                    }
                }
            });
        }
    }

    private void setupPlayNextButton(View view, final Album album, final Playlist playlist, 
                                     final PlaylistEntry entry, final Query query) {

            int drawableResId = R.drawable.ic_action_queue;
            int stringResId = R.string.context_menu_play_next;
            View v = ViewUtils.ensureInflation(view, R.id.context_menu_playnext_stub,
                    R.id.context_menu_playnext);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(drawableResId);
            textView.setText(stringResId);
            v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    getActivity().getSupportFragmentManager().popBackStack();
                    if (mAlbum != null) {
                        mCollection.getAlbumTracks(mAlbum).done(new DoneCallback<Playlist>() {
                            @Override
                            public void onDone(Playlist playlist) {
                                ArrayList<String> queryKeys = new ArrayList<>();
                                if (playlist != null) {
                                    for (PlaylistEntry entry : playlist.getEntries()) {
                                        queryKeys.add(entry.getQuery().getCacheKey());
                                    }
                                }
                                Bundle extras = new Bundle();
                                extras.putStringArrayList(TomahawkFragment.QUERYARRAY, queryKeys);
                                extras.putInt(TomahawkFragment.QUEUE_POSITION,1);
                                getActivity().getSupportMediaController()
                                        .getTransportControls().sendCustomAction(
                                        PlaybackService.ACTION_INSERT_QUERIES_TO_QUEUE, extras);
                            }
                        });
                    } else if (mQuery != null) {
                        Bundle extras = new Bundle();
                        extras.putString(TomahawkFragment.QUERY, mQuery.getCacheKey());
                        extras.putInt(TomahawkFragment.QUEUE_POSITION,1);
                        getActivity().getSupportMediaController()
                                .getTransportControls().sendCustomAction(
                                PlaybackService.ACTION_INSERT_QUERY_TO_QUEUE, extras);
                    } else if (mPlaylistEntry != null) {
                        Bundle extras = new Bundle();
                        extras.putString(TomahawkFragment.QUERY,
                                mPlaylistEntry.getQuery().getCacheKey());
                        extras.putInt(TomahawkFragment.QUEUE_POSITION,1);
                        getActivity().getSupportMediaController()
                                .getTransportControls().sendCustomAction(
                                PlaybackService.ACTION_INSERT_QUERY_TO_QUEUE, extras);
                    } else if (mPlaylist != null) {
                        ArrayList<String> queryKeys = new ArrayList<>();
                        if (mPlaylist != null) {
                            for (PlaylistEntry entry : mPlaylist.getEntries()) {
                                queryKeys.add(entry.getQuery().getCacheKey());
                            }
                        }
                        Bundle extras = new Bundle();
                        extras.putStringArrayList(TomahawkFragment.QUERYARRAY, queryKeys);
                        extras.putInt(TomahawkFragment.QUEUE_POSITION,1);
                        getActivity().getSupportMediaController()
                                .getTransportControls().sendCustomAction(
                                PlaybackService.ACTION_INSERT_QUERIES_TO_QUEUE, extras);
                    }
                }
            });
    
    }
    
    private void setupCreateStationButton(View view, final Album album, final Artist artist,
            final Playlist playlist, final PlaylistEntry entry, final Query query) {
        View v = ViewUtils.ensureInflation(view, R.id.context_menu_createstation_stub,
                R.id.context_menu_createstation);
        TextView textView = (TextView) v.findViewById(R.id.textview);
        ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
        imageView.setImageResource(R.drawable.ic_action_station);
        textView.setText(R.string.context_menu_create_station);
        v.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().getSupportFragmentManager().popBackStack();
                if (getActivity().getSupportMediaController() != null) {
                    String playbackManagerId = getActivity().getSupportMediaController().getExtras()
                            .getString(PlaybackService.EXTRAS_KEY_PLAYBACKMANAGER);
                    PlaybackManager playbackManager = PlaybackManager.getByKey(playbackManagerId);
                    StationPlaylist stationPlaylist = null;
                    if (album != null) {
                        List<Pair<Artist, String>> artists = new ArrayList<>();
                        artists.add(new Pair<>(album.getArtist(), ""));
                        stationPlaylist = StationPlaylist.get(artists, null, null);
                    } else if (artist != null) {
                        List<Pair<Artist, String>> artists = new ArrayList<>();
                        artists.add(new Pair<>(artist, ""));
                        stationPlaylist = StationPlaylist.get(artists, null, null);
                    } else if (playlist != null) {
                        stationPlaylist = StationPlaylist.get(playlist);
                    } else if (entry != null || query != null) {
                        List<Pair<Track, String>> tracks = new ArrayList<>();
                        if (query != null) {
                            tracks.add(new Pair<>(query.getBasicTrack(), ""));
                        } else {
                            tracks.add(new Pair<>(entry.getQuery().getBasicTrack(), ""));
                        }
                        stationPlaylist = StationPlaylist.get(null, tracks, null);
                    }
                    if (stationPlaylist != null
                            && stationPlaylist != playbackManager.getPlaylist()) {
                        playbackManager.setPlaylist(stationPlaylist);
                        getActivity().getSupportMediaController().getTransportControls().play();
                    }
                }
            }
        });
    }

    /**
     * Initializes the "Add to playlist"-context-menu-button.
     *
     * @param view    this Fragment's root View
     * @param queries the List of {@link Query}s that should be added to one of the user's playlists
     *                once he taps the "add to playlist-context-menu-button. If this List is null
     *                the button will show up as disabled and greyed out. If this List is empty the
     *                button won't be displayed at all.
     */
    private void setupAddToPlaylistButton(View view, final List<Query> queries) {
        View v = ViewUtils.ensureInflation(view, R.id.context_menu_addtoplaylist_stub,
                R.id.context_menu_addtoplaylist);
        if (queries != null && queries.size() == 0) {
            v.setVisibility(View.GONE);
        } else {
            TextView textView = (TextView) v.findViewById(R.id.textview);
            ImageView imageView = (ImageView) v.findViewById(R.id.imageview);
            imageView.setImageResource(R.drawable.ic_action_playlist);
            textView.setText(R.string.context_menu_add_to_playlist);
            if (queries != null) {
                v.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        getActivity().getSupportFragmentManager().popBackStack();
                        showAddToPlaylist((TomahawkMainActivity) getActivity(), queries);
                    }
                });
                textView.setTextColor(getResources().getColor(R.color.primary_textcolor_inverted));
                ImageUtils.setTint(imageView.getDrawable(), R.color.primary_textcolor_inverted);
            } else {
                textView.setTextColor(getResources().getColor(R.color.disabled));
                ImageUtils.setTint(imageView.getDrawable(), R.color.disabled);
            }
        }
    }

    private void showAddToPlaylist(final TomahawkMainActivity activity, final List<Query> queries) {
        User.getSelf().done(new DoneCallback<User>() {
            @Override
            public void onDone(User user) {
                ArrayList<String> queryKeys = new ArrayList<>();
                for (Query query : queries) {
                    queryKeys.add(query.getCacheKey());
                }
                Bundle bundle = new Bundle();
                bundle.putString(TomahawkFragment.USER, user.getId());
                bundle.putInt(TomahawkFragment.CONTENT_HEADER_MODE,
                        ContentHeaderFragment.MODE_HEADER_STATIC);
                bundle.putStringArrayList(TomahawkFragment.QUERYARRAY, queryKeys);
                FragmentUtils.replace(activity, PlaylistsFragment.class, bundle);
            }
        });
    }

    private void setupTextViews(View view) {
        if (mAlbum != null) {
            View v = ViewUtils.ensureInflation(
                    view, R.id.album_name_button_stub, R.id.album_name_button);
            TextView textView = (TextView) v.findViewById(R.id.textview);
            textView.setText(mAlbum.getName());
            v.setOnClickListener(constructAlbumNameClickListener(mAlbum.getCacheKey()));
        } else if (mQuery != null || mPlaylistEntry != null || mPlaylist != null
                || mStationPlaylist != null) {
            View v = ViewUtils.ensureInflation(view, R.id.track_name_stub, R.id.track_name);
            TextView textView = (TextView) v;
            if (mQuery != null) {
                textView.setText(mQuery.getName());
            } else if (mPlaylistEntry != null) {
                textView.setText(mPlaylistEntry.getQuery().getPrettyName());
            } else if (mPlaylist != null) {
                textView.setText(mPlaylist.getName());
            } else if (mStationPlaylist != null) {
                textView.setText(mStationPlaylist.getName());
            }
        }
        if (mAlbum != null || mQuery != null || mPlaylistEntry != null || mArtist != null) {
            View v = ViewUtils.ensureInflation(
                    view, R.id.artist_name_button_stub, R.id.artist_name_button);
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
            View v = ViewUtils.ensureInflation(
                    view, R.id.view_album_button_stub, R.id.view_album_button);
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
        if (view != null && mAlbum != null
                || (mQuery != null && !TextUtils.isEmpty(mQuery.getAlbum().getName()))
                || (mPlaylistEntry != null
                && !TextUtils.isEmpty(mPlaylistEntry.getQuery().getAlbum().getName()))) {
            View v = ViewUtils.ensureInflation(view, R.id.context_menu_albumart_stub,
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
                ImageUtils.loadImageIntoImageView(TomahawkApp.getContext(), albumImageView,
                        album.getImage(), Image.getLargeImageSize(), true, false);
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
                        IdGenerator.getSessionUniqueId());
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
                        PlaylistEntriesFragment.class, bundle);
            }
        };
    }
}
