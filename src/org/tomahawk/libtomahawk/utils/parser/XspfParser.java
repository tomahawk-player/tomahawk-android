/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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
package org.tomahawk.libtomahawk.utils.parser;

import com.google.common.base.Charsets;
import com.google.common.io.Files;

import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.resolver.Query;
import org.tomahawk.libtomahawk.utils.GsonXmlHelper;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;

import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public class XspfParser {

    private final static String TAG = XspfParser.class.getSimpleName();

    public static Playlist parse(Uri uri) {
        if (uri.getScheme().equals("file")) {
            return parse(new File(uri.getPath()));
        } else {
            return parse(uri.toString());
        }
    }

    public static Playlist parse(File file) {
        String xspfString = null;
        try {
            xspfString = Files.toString(file, Charsets.UTF_8);
        } catch (IOException e) {
            Log.e(TAG, "parse: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        return parseXspf(xspfString);
    }

    public static Playlist parse(String url) {
        String xspfString = null;
        TomahawkUtils.HttpResponse response = null;
        try {
            response = TomahawkUtils.httpRequest(null, url, null, null, null, null);
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException e) {
            Log.e(TAG, "parse: " + e.getClass() + ": " + e.getLocalizedMessage());
        }
        if (response != null) {
            xspfString = response.mResponseText;
        }
        return parseXspf(xspfString);
    }

    public static Playlist parseXspf(String xspfString) {
        if (xspfString != null) {
            XspfPlaylist xspfPlaylist =
                    GsonXmlHelper.get().fromXml(xspfString, XspfPlaylist.class);
            if (xspfPlaylist != null && xspfPlaylist.trackList != null) {
                ArrayList<Query> qs = new ArrayList<>();
                for (XspfPlaylistTrack track : xspfPlaylist.trackList) {
                    qs.add(Query.get(track.title, track.album, track.creator, false));
                }
                String title = xspfPlaylist.title == null ? "XSPF Playlist" : xspfPlaylist.title;
                Playlist pl = Playlist.fromQueryList(title, qs);
                pl.setFilled(true);
                return pl;
            }
        }
        Log.e(TAG, "parse: couldn't read xspf playlist");
        return null;
    }

}
