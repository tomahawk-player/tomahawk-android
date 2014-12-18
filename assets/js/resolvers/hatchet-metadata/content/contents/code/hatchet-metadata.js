/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */

var HatchetMetadataResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'Hatchet Metadata',
        icon: 'hatchet-metadata.png',
        weight: 0, // We cannot resolve, so use minimum weight
        timeout: 15
    },

    init: function () {
        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);
    },

    resolve: function (qid, artist, album, title) {
        Tomahawk.addTrackResults({ results: [], qid: qid });
    },

    search: function (qid, searchString) {
        Tomahawk.addTrackResults({ results: [], qid: qid });
    },

    canParseUrl: function (url, type) {
        switch (type) {
            case TomahawkUrlType.Album:
                return /^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n]+\/[^\/\n]+$/.test(url);
            case TomahawkUrlType.Artist:
                return /^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n][^\/\n_]+$/.test(url);
            case TomahawkUrlType.Track:
                return /^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n]+\/_\/[^\/\n]+$/.test(url);
            default:
                return false;
        }
    },

    lookupUrl: function (url) {
        Tomahawk.log("lookupUrl: "+url);
        var urlParts = url.split('/').filter(function (item) {
            return item.length != 0;
        }).map(decodeURIComponent);
        if (/^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n]+\/[^\/\n]+$/.test(url)) {
            Tomahawk.log("Found an album");
            // We have to deal with an Album
            Tomahawk.addUrlResult(url, {
                type: 'album',
                artist: urlParts[urlParts.length - 2],
                name: urlParts[urlParts.length - 1]
            });
        } else if (/^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n][^\/\n_]+$/.test(url)) {
            Tomahawk.log("Found an artist");
            // We have to deal with an Artist
            Tomahawk.addUrlResult(url, {
                type: 'artist',
                name: urlParts[urlParts.length - 1]
            });
        } else if (/^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n]+\/_\/[^\/\n]+$/.test(url)) {
            Tomahawk.log("Found a track");
            // We have to deal with a Track
            Tomahawk.addUrlResult(url, {
                type: "track",
                artist: urlParts[urlParts.length - 3],
                title: urlParts[urlParts.length - 1]
            });
        }
    }
});

Tomahawk.resolver.instance = HatchetMetadataResolver;
