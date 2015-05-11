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
package org.tomahawk.libtomahawk.resolver;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;

import java.util.ArrayList;

public class ScriptUtils {

    /**
     * Parses the given {@link JsonArray) into an {@link ArrayList} of {@link Result}s.
     *
     * @param resolver   the {@link Resolver} which will be set in the {@link Result}'s constructor
     * @param rawResults {@link JsonArray) containing the raw result information
     * @return a {@link ArrayList} of {@link Result}s containing the parsed data
     */
    public static ArrayList<Result> parseResultList(ScriptResolver resolver, JsonArray rawResults) {
        ArrayList<Result> resultList = new ArrayList<>();
        for (JsonElement rawResult : rawResults) {
            String url = getNodeChildAsText(rawResult, "url");
            String track = getNodeChildAsText(rawResult, "track");
            if (url != null && track != null) {
                String artist = getNodeChildAsText(rawResult, "artist");
                String album = getNodeChildAsText(rawResult, "album");
                String purchaseUrl = getNodeChildAsText(rawResult, "purchaseUrl");
                String linkUrl = getNodeChildAsText(rawResult, "linkUrl");
                int albumpos = getNodeChildAsInt(rawResult, "albumpos");
                int discnumber = getNodeChildAsInt(rawResult, "discnumber");
                int year = getNodeChildAsInt(rawResult, "year");
                int duration = getNodeChildAsInt(rawResult, "duration");
                int bitrate = getNodeChildAsInt(rawResult, "bitrate");
                int size = getNodeChildAsInt(rawResult, "size");

                Artist artistObj = Artist.get(artist);
                Album albumObj = Album.get(album, artistObj);
                Track trackObj = Track.get(track, albumObj, artistObj);
                trackObj.setAlbumPos(albumpos);
                trackObj.setDiscNumber(discnumber);
                trackObj.setYear(year);
                trackObj.setDuration(duration * 1000);

                Result result = Result.get(url, trackObj, resolver);
                result.setBitrate(bitrate);
                result.setSize(size);
                result.setPurchaseUrl(purchaseUrl);
                result.setLinkUrl(linkUrl);
                result.setArtist(artistObj);
                result.setAlbum(albumObj);
                result.setTrack(trackObj);

                resultList.add(result);
            }
        }
        return resultList;
    }

    public static String getNodeChildAsText(JsonElement node, String fieldName) {
        if (node instanceof JsonObject) {
            JsonElement n = ((JsonObject) node).get(fieldName);
            if (n != null && n.isJsonPrimitive()) {
                return n.getAsString();
            }
        }
        return null;
    }

    public static int getNodeChildAsInt(JsonElement node, String fieldName) {
        if (node instanceof JsonObject) {
            JsonElement n = ((JsonObject) node).get(fieldName);
            if (n != null && n.isJsonPrimitive()) {
                return n.getAsInt();
            }
        }
        return 0;
    }

}
