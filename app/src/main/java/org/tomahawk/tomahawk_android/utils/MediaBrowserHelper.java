/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.tomahawk_android.utils;

import org.jdeferred.DoneCallback;
import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Collection;
import org.tomahawk.libtomahawk.collection.CollectionCursor;
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.HatchetCollection;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.tomahawk_android.R;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaBrowserServiceCompat;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MediaBrowserHelper {

    private static final String TAG = MediaBrowserHelper.class.getSimpleName();

    public static final String MEDIA_ID_ROOT = "__ROOT__";

    public static final String MEDIA_ID_SHUFFLEDPLAY = "__SHUFFLEDPLAY__";

    public static final String MEDIA_ID_SHUFFLEDPLAY_TRACKS = "__SHUFFLEDPLAY_TRACKS__";

    public static final String MEDIA_ID_SHUFFLEDPLAY_FAVORITES = "__SHUFFLEDPLAY_FAVORITES__";

    public static final String MEDIA_ID_COLLECTION = "__COLLECTION__";

    public static final String MEDIA_ID_COLLECTION_ALBUMS = "__COLLECTION_ALBUMS__";

    public static final String MEDIA_ID_COLLECTION_ARTISTS = "__COLLECTION_ARTISTS__";

    public static final String MEDIA_ID_PLAYLISTS = "__PLAYLISTS__";

    public static final String MEDIA_ID_STATIONS = "__STATIONS__";

    private PackageValidator mPackageValidator;

    private Context mContext;

    public MediaBrowserHelper(Context context) {
        mContext = context;
        mPackageValidator = new PackageValidator(context);
    }

    @Nullable
    public MediaBrowserServiceCompat.BrowserRoot onGetRoot(@NonNull String clientPackageName,
            int clientUid,
            @Nullable Bundle rootHints) {
        Log.d(TAG, "OnGetRoot: clientPackageName=" + clientPackageName +
                "; clientUid=" + clientUid + " ; rootHints=" + rootHints);
        // To ensure you are not allowing any arbitrary app to browse your app's contents, you
        // need to check the origin:
        if (!mPackageValidator.isCallerAllowed(mContext, clientPackageName, clientUid)) {
            // If the request comes from an untrusted package, return null. No further calls will
            // be made to other media browsing methods.
            Log.e(TAG, "OnGetRoot: IGNORING request from untrusted package "
                    + clientPackageName);
            return null;
        }
        return new MediaBrowserServiceCompat.BrowserRoot(MEDIA_ID_ROOT, null);
    }

    public void onLoadChildren(@NonNull String parentId,
            @NonNull final MediaBrowserServiceCompat.Result<List<MediaBrowserCompat.MediaItem>> result) {
        Log.d(TAG, "OnLoadChildren: parentMediaId=" + parentId);

        result.detach();

        final List<MediaBrowserCompat.MediaItem> mediaItems = new ArrayList<>();

        if (MEDIA_ID_ROOT.equals(parentId)) {
            Log.d(TAG, "OnLoadChildren.ROOT");
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MEDIA_ID_SHUFFLEDPLAY)
                            .setTitle(mContext.getString(R.string.mediabrowser_shuffledplay))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MEDIA_ID_COLLECTION)
                            .setTitle(mContext.getString(R.string.drawer_title_collection))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MEDIA_ID_PLAYLISTS)
                            .setTitle(mContext.getString(R.string.drawer_title_playlists))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MEDIA_ID_STATIONS)
                            .setTitle(mContext.getString(R.string.drawer_title_stations))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
            result.sendResult(mediaItems);
        } else if (MEDIA_ID_SHUFFLEDPLAY.equals(parentId)) {
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MEDIA_ID_SHUFFLEDPLAY_FAVORITES)
                            .setTitle(mContext.getString(R.string.drawer_title_lovedtracks))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            ));
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MEDIA_ID_SHUFFLEDPLAY_TRACKS)
                            .setTitle(mContext.getString(R.string.tracks))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
            ));
            result.sendResult(mediaItems);
        } else if (MEDIA_ID_COLLECTION.equals(parentId)) {
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MEDIA_ID_COLLECTION_ALBUMS)
                            .setTitle(mContext.getString(R.string.albums))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
            mediaItems.add(new MediaBrowserCompat.MediaItem(
                    new MediaDescriptionCompat.Builder()
                            .setMediaId(MEDIA_ID_COLLECTION_ARTISTS)
                            .setTitle(mContext.getString(R.string.artists))
                            .build(), MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
            ));
            result.sendResult(mediaItems);
        } else if (MEDIA_ID_COLLECTION_ALBUMS.equals(parentId)) {
            CollectionManager.get().getUserCollection().getAlbums(
                    Collection.SORT_ALPHA).done(
                    new DoneCallback<CollectionCursor<Album>>() {
                        @Override
                        public void onDone(CollectionCursor<Album> albums) {
                            for (int i = 0; i < albums.size(); i++) {
                                Album album = albums.get(i);
                                String mediaId = buildMediaId(MEDIA_ID_COLLECTION_ALBUMS,
                                        album.getCacheKey());
                                mediaItems.add(new MediaBrowserCompat.MediaItem(
                                        new MediaDescriptionCompat.Builder()
                                                .setMediaId(mediaId)
                                                .setTitle(album.getPrettyName())
                                                .setSubtitle(album.getArtist().getPrettyName())
                                                .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                                ));
                            }
                            albums.close();
                            result.sendResult(mediaItems);
                        }
                    });
        } else if (MEDIA_ID_COLLECTION_ARTISTS.equals(parentId)) {
            CollectionManager.get().getUserCollection().getArtists(
                    Collection.SORT_ALPHA).done(
                    new DoneCallback<CollectionCursor<Artist>>() {
                        @Override
                        public void onDone(CollectionCursor<Artist> artists) {
                            for (int i = 0; i < artists.size(); i++) {
                                Artist artist = artists.get(i);
                                String mediaId = buildMediaId(MEDIA_ID_COLLECTION_ARTISTS,
                                        artist.getCacheKey());
                                mediaItems.add(new MediaBrowserCompat.MediaItem(
                                        new MediaDescriptionCompat.Builder()
                                                .setMediaId(mediaId)
                                                .setTitle(artist.getPrettyName())
                                                .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                                ));
                            }
                            artists.close();
                            result.sendResult(mediaItems);
                        }
                    });
        } else if (MEDIA_ID_PLAYLISTS.equals(parentId)) {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    for (int i = 0; i < user.getPlaylists().size(); i++) {
                        Playlist playlist = user.getPlaylists().get(i);
                        String topArtistsString = "";
                        String[] artists = playlist.getTopArtistNames();
                        if (artists != null) {
                            for (int j = 0; j < artists.length && j < 5; j++) {
                                topArtistsString += artists[j];
                                if (j != artists.length - 1) {
                                    topArtistsString += ", ";
                                }
                            }
                        }
                        String mediaId = buildMediaId(MEDIA_ID_PLAYLISTS, playlist.getCacheKey());
                        mediaItems.add(new MediaBrowserCompat.MediaItem(
                                new MediaDescriptionCompat.Builder()
                                        .setMediaId(mediaId)
                                        .setTitle(playlist.getName())
                                        .setSubtitle(topArtistsString)
                                        .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                        ));
                    }
                    result.sendResult(mediaItems);
                }
            });
        } else if (MEDIA_ID_STATIONS.equals(parentId)) {
            List<StationPlaylist> stations = DatabaseHelper.get().getStations();
            for (StationPlaylist station : stations) {
                String mediaId = buildMediaId(MEDIA_ID_STATIONS, station.getCacheKey());
                mediaItems.add(new MediaBrowserCompat.MediaItem(
                        new MediaDescriptionCompat.Builder()
                                .setMediaId(mediaId)
                                .setTitle(station.getName())
                                .build(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                ));
            }
            result.sendResult(mediaItems);
        }
    }

    private String buildMediaId(String leaf, String cacheKey) {
        return leaf + "♠" + cacheKey;
    }

    /**
     * Override to handle requests to play a specific mediaId that was provided by your app.
     */
    public void onPlayFromMediaId(final MediaSessionCompat mediaSession,
            final PlaybackManager playbackManager, final String mediaId, Bundle extras) {
        String[] parts;
        final MediaControllerCompat.TransportControls transportControls =
                mediaSession.getController().getTransportControls();
        if (MEDIA_ID_SHUFFLEDPLAY_FAVORITES.equals(mediaId)) {
            User.getSelf().done(new DoneCallback<User>() {
                @Override
                public void onDone(User user) {
                    playbackManager.setPlaylist(user.getFavorites());
                    playbackManager.setShuffleMode(PlaybackManager.SHUFFLED);
                    transportControls.play();
                }
            });
        } else if (MEDIA_ID_SHUFFLEDPLAY_TRACKS.equals(mediaId)) {
            CollectionManager.get().getUserCollection().getQueries(Collection.SORT_ALPHA).done(
                    new DoneCallback<Playlist>() {
                        @Override
                        public void onDone(Playlist collectionTracks) {
                            playbackManager.setPlaylist(collectionTracks);
                            playbackManager.setShuffleMode(PlaybackManager.SHUFFLED);
                            transportControls.play();
                        }
                    });
        } else if ((parts = mediaId.split("♠", 2)).length > 1) {
            String leaf = parts[0];
            String cacheKey = parts[1];
            if (MEDIA_ID_COLLECTION_ALBUMS.equals(leaf)) {
                final Album album = Album.getByKey(cacheKey);
                CollectionManager.get().getUserCollection().getAlbumTracks(album).done(
                        new DoneCallback<Playlist>() {
                            @Override
                            public void onDone(Playlist albumTracks) {
                                if (albumTracks != null) {
                                    playbackManager.setPlaylist(albumTracks);
                                    transportControls.play();
                                } else {
                                    HatchetCollection hatchetCollection
                                            = CollectionManager.get().getHatchetCollection();
                                    hatchetCollection.getAlbumTracks(album).done(
                                            new DoneCallback<Playlist>() {
                                                @Override
                                                public void onDone(Playlist albumTracks) {
                                                    if (albumTracks != null) {
                                                        playbackManager.setPlaylist(albumTracks);
                                                        mediaSession.getController()
                                                                .getTransportControls().play();
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            } else if (MEDIA_ID_COLLECTION_ARTISTS.equals(leaf)) {
                final Artist artist = Artist.getByKey(cacheKey);
                CollectionManager.get().getUserCollection().getArtistTracks(artist).done(
                        new DoneCallback<Playlist>() {
                            @Override
                            public void onDone(Playlist artistTracks) {
                                if (artistTracks != null) {
                                    playbackManager.setPlaylist(artistTracks);
                                    transportControls.play();
                                } else {
                                    HatchetCollection hatchetCollection
                                            = CollectionManager.get().getHatchetCollection();
                                    hatchetCollection.getArtistTopHits(artist).done(
                                            new DoneCallback<Playlist>() {
                                                @Override
                                                public void onDone(Playlist artistTopHits) {
                                                    if (artistTopHits != null) {
                                                        playbackManager.setPlaylist(artistTopHits);
                                                        transportControls.play();
                                                    }
                                                }
                                            });
                                }
                            }
                        });
            } else if (MEDIA_ID_PLAYLISTS.equals(leaf) || MEDIA_ID_STATIONS.equals(leaf)) {
                Playlist playlist = Playlist.getByKey(cacheKey);
                if (!(playlist instanceof StationPlaylist) && !playlist.isFilled()) {
                    playlist = DatabaseHelper.get().getPlaylist(playlist.getId());
                }
                playbackManager.setPlaylist(playlist);
                transportControls.play();
            }
        }
    }

}
