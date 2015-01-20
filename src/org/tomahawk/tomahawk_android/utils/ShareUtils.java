/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.PlaylistEntry;
import org.tomahawk.libtomahawk.infosystem.SocialAction;
import org.tomahawk.libtomahawk.infosystem.User;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class ShareUtils {

    private final static String TAG = ShareUtils.class.getSimpleName();

    private static String sHatchetBaseUrl = "https://hatchet.is/";

    public static final String DEFAULT_SHARE_PREFIX = "#musthear";

    public static void sendShareIntent(Activity activity, TomahawkListItem item) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        if (item instanceof PlaylistEntry) {
            item = ((PlaylistEntry) item).getQuery();
        } else if (item instanceof SocialAction) {
            item = ((SocialAction) item).getTarget();
        }

        if (item instanceof Album) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, ShareUtils.generateShareMsg((Album) item));
            activity.startActivity(shareIntent);
        } else if (item instanceof Artist) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, ShareUtils.generateShareMsg((Artist) item));
            activity.startActivity(shareIntent);
        } else if (item instanceof Query) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, ShareUtils.generateShareMsg((Query) item));
            activity.startActivity(shareIntent);
        } else if (item instanceof Playlist) {
            shareIntent.putExtra(Intent.EXTRA_TEXT, ShareUtils.generateShareMsg((Playlist) item));
            activity.startActivity(shareIntent);
        }
    }

    public static String generateLink(Album album) {
        if (album != null) {
            String urlStr = sHatchetBaseUrl + "music/" + album.getArtist().getName() + "/"
                    + album.getName();
            try {
                URL url = new URL(urlStr);
                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                        url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                return uri.toURL().toString();
            } catch (MalformedURLException e) {
                Log.e(TAG, "generateLink: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (URISyntaxException e) {
                Log.e(TAG, "generateLink: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return null;
    }

    public static String generateShareMsg(Album album) {
        if (album != null) {
            return DEFAULT_SHARE_PREFIX
                    + " " + TomahawkApp.getContext().getString(R.string.album_by_artist,
                    "\"" + album.getName() + "\"", album.getArtist().getName())
                    + " - " + generateLink(album);
        }
        return null;
    }

    public static String generateLink(Artist artist) {
        if (artist != null) {
            String urlStr = sHatchetBaseUrl + "music/" + artist.getName();
            try {
                URL url = new URL(urlStr);
                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                        url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                return uri.toURL().toString();
            } catch (MalformedURLException e) {
                Log.e(TAG, "generateLink: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (URISyntaxException e) {
                Log.e(TAG, "generateLink: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return null;
    }

    public static String generateShareMsg(Artist artist) {
        if (artist != null) {
            return DEFAULT_SHARE_PREFIX + " " + artist.getName() + " - " + generateLink(artist);
        }
        return null;
    }

    public static String generateLink(Query query) {
        if (query != null) {
            String urlStr = sHatchetBaseUrl + "music/" + query.getArtist().getName() + "/_/"
                    + query.getName();
            try {
                URL url = new URL(urlStr);
                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                        url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                return uri.toURL().toString();
            } catch (MalformedURLException e) {
                Log.e(TAG, "generateLink: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (URISyntaxException e) {
                Log.e(TAG, "generateLink: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return null;
    }

    public static String generateShareMsg(Query query) {
        if (query != null) {
            return DEFAULT_SHARE_PREFIX
                    + " " + TomahawkApp.getContext().getString(R.string.album_by_artist,
                    "\"" + query.getName() + "\"", query.getArtist().getName())
                    + " - " + generateLink(query);
        }
        return null;
    }

    public static String generateLink(Playlist playlist, User user) {
        if (playlist != null) {
            String urlStr = sHatchetBaseUrl + "people/" + user.getName() + "/playlists/"
                    + playlist.getHatchetId();
            try {
                URL url = new URL(urlStr);
                URI uri = new URI(url.getProtocol(), url.getUserInfo(), url.getHost(),
                        url.getPort(), url.getPath(), url.getQuery(), url.getRef());
                return uri.toURL().toString();
            } catch (MalformedURLException e) {
                Log.e(TAG, "generateLink: " + e.getClass() + ": " + e.getLocalizedMessage());
            } catch (URISyntaxException e) {
                Log.e(TAG, "generateLink: " + e.getClass() + ": " + e.getLocalizedMessage());
            }
        }
        return null;
    }

    public static String generateShareMsg(Playlist playlist) {
        if (playlist != null && playlist.getUserId() != null) {
            User user = User.getUserById(playlist.getUserId());
            if (user != null && user.getName() != null) {
                return DEFAULT_SHARE_PREFIX + " " + user.getName()
                        + TomahawkApp.getContext().getString(R.string.users_playlist_suffix)
                        + ": \"" + playlist.getName() + "\"" + " - " + generateLink(playlist, user);
            }
        }
        return null;
    }
}
