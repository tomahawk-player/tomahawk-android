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
import org.tomahawk.libtomahawk.collection.CollectionManager;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.StationPlaylist;
import org.tomahawk.libtomahawk.database.DatabaseHelper;
import org.tomahawk.libtomahawk.infosystem.InfoSystem;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGenerator;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorManager;
import org.tomahawk.libtomahawk.infosystem.stations.ScriptPlaylistGeneratorSearchResult;
import org.tomahawk.libtomahawk.resolver.PipeLine;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.resolver.ResultScoring;

import android.app.SearchManager;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.media.session.MediaControllerCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import de.greenrobot.event.EventBus;

public class MediaPlayIntentHandler {

    private Artist mResolvingArtist;

    private Album mResolvingAlbum;

    private String mWaitingGenre;

    private final Set<String> mCorrespondingRequestIds =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    private final Set<Query> mCorrespondingQueries =
            Collections.newSetFromMap(new ConcurrentHashMap<Query, Boolean>());

    private PlaybackManager mPlaybackManager;

    private MediaControllerCompat.TransportControls mTransportControls;

    @SuppressWarnings("unused")
    public void onEvent(ScriptPlaylistGeneratorManager.GeneratorAddedEvent event) {
        playGenre();
    }

    @SuppressWarnings("unused")
    public void onEvent(PipeLine.ResultsEvent event) {
        if (mCorrespondingQueries.contains(event.mQuery)) {
            Playlist playlist;
            if (event.mQuery.isFullTextQuery()) {
                playlist = event.mQuery.getResultPlaylist();
            } else {
                List<Query> queries = new ArrayList<>();
                queries.add(event.mQuery);
                playlist = Playlist.fromQueryList(event.mQuery.getCacheKey(), "", "", queries);
            }
            playPlaylist(playlist, false);
        }
    }

    @SuppressWarnings("unused")
    public void onEvent(InfoSystem.ResultsEvent event) {
        if (mCorrespondingRequestIds.contains(event.mInfoRequestData.getRequestId())) {
            if (mResolvingAlbum != null) {
                CollectionManager.get().getHatchetCollection().getAlbumTracks(mResolvingAlbum).done(
                        new DoneCallback<Playlist>() {
                            @Override
                            public void onDone(Playlist result) {
                                if (result != null && result.size() > 0) {
                                    playPlaylist(result, false);
                                }
                            }
                        }
                );
            } else if (mResolvingArtist != null) {
                CollectionManager.get().getHatchetCollection().getArtistTopHits(mResolvingArtist)
                        .done(new DoneCallback<Playlist>() {
                                  @Override
                                  public void onDone(Playlist result) {
                                      if (result != null && result.size() > 0) {
                                          playPlaylist(result, false);
                                      }
                                  }
                              }
                        );
            }
        }
    }

    public MediaPlayIntentHandler(MediaControllerCompat.TransportControls transportControls,
            PlaybackManager playbackManager) {
        mTransportControls = transportControls;
        mPlaybackManager = playbackManager;
        EventBus.getDefault().register(this);
    }

    public void mediaPlayFromSearch(Bundle extras) {
        mWaitingGenre = null;
        mCorrespondingQueries.clear();
        mCorrespondingRequestIds.clear();
        mResolvingAlbum = null;
        mResolvingArtist = null;

        String mediaFocus = extras.getString(MediaStore.EXTRA_MEDIA_FOCUS);
        String query = extras.getString(SearchManager.QUERY);

        // Some of these extras may not be available depending on the search mode
        String album = extras.getString(MediaStore.EXTRA_MEDIA_ALBUM);
        String artist = extras.getString(MediaStore.EXTRA_MEDIA_ARTIST);
        final String genre = extras.getString("android.intent.extra.genre");
        String playlist = extras.getString("android.intent.extra.playlist");
        String title = extras.getString(MediaStore.EXTRA_MEDIA_TITLE);
        // Determine the search mode and use the corresponding extras
        if (mediaFocus == null) {
            // 'Unstructured' search mode (backward compatible)
            Query q = Query.get(query, false);
            mCorrespondingQueries.clear();
            mCorrespondingQueries.add(PipeLine.get().resolve(q));
        } else if (mediaFocus.compareTo("vnd.android.cursor.item/*") == 0) {
            if (query != null && query.isEmpty()) {
                // 'Any' search mode
                CollectionManager.get().getUserCollection().getQueries(Collection.SORT_ALPHA).done(
                        new DoneCallback<Playlist>() {
                            @Override
                            public void onDone(Playlist collectionTracks) {
                                playPlaylist(collectionTracks, true);
                            }
                        });
            } else {
                // 'Unstructured' search mode
                Query q = Query.get(query, false);
                mCorrespondingQueries.add(PipeLine.get().resolve(q));
            }
        } else if (mediaFocus.compareTo(MediaStore.Audio.Genres.ENTRY_CONTENT_TYPE) == 0) {
            // 'Genre' search mode
            mWaitingGenre = genre;
            playGenre();
        } else if (mediaFocus.compareTo(MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE) == 0) {
            // 'Artist' search mode
            final Artist a = Artist.get(artist);
            CollectionManager.get().getUserCollection().getArtistTracks(a).done(
                    new DoneCallback<Playlist>() {
                        @Override
                        public void onDone(Playlist result) {
                            if (result != null && result.size() > 0) {
                                // There are some local tracks for the requested artist available
                                playPlaylist(result, false);
                            } else {
                                // Try fetching top-hits for the requested artist
                                mResolvingArtist = a;
                                mCorrespondingRequestIds.add(InfoSystem.get().resolve(a, true));
                            }
                        }
                    });
        } else if (mediaFocus.compareTo(MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE) == 0) {
            // 'Album' search mode
            final Album a = Album.get(album, Artist.get(artist));
            CollectionManager.get().getUserCollection().getAlbumTracks(a).done(
                    new DoneCallback<Playlist>() {
                        @Override
                        public void onDone(Playlist result) {
                            if (result != null && result.size() > 0) {
                                // There are some local tracks for the requested album available
                                playPlaylist(result, false);
                            } else {
                                // Try fetching top-hits for the requested album
                                mResolvingAlbum = a;
                                mCorrespondingRequestIds.add(InfoSystem.get().resolve(a));
                            }
                        }
                    });
        } else if (mediaFocus.compareTo("vnd.android.cursor.item/audio") == 0) {
            // 'Song' search mode
            Query q = Query.get(title, album, artist, false);
            mCorrespondingQueries.add(PipeLine.get().resolve(q));
        } else if (mediaFocus.compareTo(MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE) == 0) {
            // 'Playlist' search mode
            Playlist bestMatch = null;
            float bestScore = 0f;
            float minScore = 0.7f;
            for (Playlist pl : DatabaseHelper.get().getPlaylists()) {
                float score = ResultScoring.calculateScore(pl.getName(), playlist);
                if (score > minScore && score > bestScore) {
                    bestMatch = pl;
                    bestScore = score;
                }
            }
            if (bestMatch != null) {
                playPlaylist(bestMatch, false);
            }
        }
    }

    private void playPlaylist(Playlist playlist, boolean shuffled) {
        mPlaybackManager.setPlaylist(playlist);
        if (shuffled) {
            mPlaybackManager.setShuffleMode(PlaybackManager.SHUFFLED);
        }
        mTransportControls.play();
        EventBus.getDefault().unregister(this);
    }

    private void playGenre() {
        ScriptPlaylistGenerator generator =
                ScriptPlaylistGeneratorManager.get().getDefaultPlaylistGenerator();
        if (generator != null && mWaitingGenre != null) {
            final String genre = mWaitingGenre;
            mWaitingGenre = null;
            generator.search(genre).done(new DoneCallback<ScriptPlaylistGeneratorSearchResult>() {
                @Override
                public void onDone(ScriptPlaylistGeneratorSearchResult result) {
                    if (result.mGenres.size() > 0) {
                        String bestMatch = null;
                        float bestScore = 0f;
                        float minScore = 0.7f;
                        for (String genreResult : result.mGenres) {
                            float score = ResultScoring.calculateScore(genreResult,
                                    genre.toLowerCase());
                            if (score > minScore && score > bestScore) {
                                bestMatch = genreResult;
                                bestScore = score;
                            }
                        }
                        if (bestMatch != null) {
                            List<String> list = new ArrayList<>();
                            list.add(bestMatch);
                            StationPlaylist pl = StationPlaylist.get(null, null, list);
                            playPlaylist(pl, false);
                        }
                    }
                }
            });
        }
    }

}
