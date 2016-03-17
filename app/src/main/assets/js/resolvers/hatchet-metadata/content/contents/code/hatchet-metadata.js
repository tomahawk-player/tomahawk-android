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

var HatchetMetadataResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'Hatchet Metadata',
        icon: 'hatchet-metadata.png',
        weight: 0, // We cannot resolve, so use minimum weight
        timeout: 15
    },

    canParseUrl: function (params) {
        var url = params.url;
        var type = params.type;

        switch (type) {
            case Tomahawk.UrlType.Album:
                return /^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n]+\/[^\/\n]+$/.test(url);
            case Tomahawk.UrlType.Artist:
                return /^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n][^\/\n_]+$/.test(url);
            case Tomahawk.UrlType.Track:
                return /^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n]+\/_\/[^\/\n]+$/.test(url);
            case Tomahawk.UrlType.Playlist:
                return /^https?:\/\/(www\.)?hatchet\.is\/people\/[^\/\n]+\/playlists\/[^\/\n]+$/.test(url);
            default:
                return false;
        }
    },

    lookupUrl: function (params) {
        var url = params.url;

        Tomahawk.log("lookupUrl: " + url);
        var urlParts =
            url.split('/').filter(function (item) {
                return item.length != 0;
            }).map(function (s) {
                return decodeURIComponent(s.replace(/\+/g, '%20'));
            });
        if (/^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n]+\/[^\/\n]+$/.test(url)) {
            Tomahawk.log("Found an album");
            // We have to deal with an Album
            return {
                type: Tomahawk.UrlType.Album,
                artist: urlParts[urlParts.length - 2],
                album: urlParts[urlParts.length - 1]
            };
        } else if (/^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n][^\/\n_]+$/.test(url)) {
            Tomahawk.log("Found an artist");
            // We have to deal with an Artist
            return {
                type: Tomahawk.UrlType.Artist,
                artist: urlParts[urlParts.length - 1]
            };
        } else if (/^https?:\/\/(www\.)?hatchet\.is\/music\/[^\/\n]+\/[^\/\n]+\/[^\/\n]+$/.test(url)) {
            Tomahawk.log("Found a track");
            // We have to deal with a Track
            return {
                type: Tomahawk.UrlType.Track,
                artist: urlParts[urlParts.length - 3],
                track: urlParts[urlParts.length - 1]
            };
        } else if (/^https?:\/\/(www\.)?hatchet\.is\/people\/[^\/\n]+\/playlists\/[^\/\n]+$/.test(url)) {
            Tomahawk.log("Found a playlist");
            // We have to deal with a Playlist
            var match = url.match(/^https?:\/\/(?:www\.)?hatchet\.is\/people\/[^\/\n]+\/playlists\/([^\/\n]+)$/);
            var query = "https://api.hatchet.is/v2/playlists";
            var settings = {
                data: {
                    "ids[]": match[1]
                }
            };
            return Tomahawk.get(query, settings).then(function (res) {
                var query = "https://api.hatchet.is" + res.playlists[0].links.playlistEntries;
                return Tomahawk.get(query).then(function (res) {
                    var entriesMap = {};
                    res.playlistEntries.forEach(function (item) {
                        entriesMap[item.id] = item;
                    });
                    var artistsMap = {};
                    res.artists.forEach(function (item) {
                        artistsMap[item.id] = item;
                    });
                    var tracksMap = {};
                    res.tracks.forEach(function (item) {
                        tracksMap[item.id] = item;
                    });
                    var tracks = res.playlists[0].playlistEntries.map(function (item) {
                        var track = tracksMap[entriesMap[item].track];
                        return {
                            type: Tomahawk.UrlType.Track,
                            track: track.name,
                            artist: artistsMap[track.artist].name
                        };
                    });
                    Tomahawk.log("Reported found playlist '" + res.playlists[0].title
                        + "' containing " + tracks.length + " tracks");
                    return {
                        type: Tomahawk.UrlType.Playlist,
                        title: res.playlists[0].title,
                        guid: res.playlists[0].id,
                        info: "A playlist on Hatchet.",
                        creator: res.playlists[0].user,
                        linkUrl: url,
                        tracks: tracks
                    };
                });
            });
        }
    }
});

Tomahawk.resolver.instance = HatchetMetadataResolver;

Tomahawk.PluginManager.registerPlugin('chartsProvider', {

    _baseUrl: "https://api.hatchet.is/v2/charts",

    countryCodes: {
        defaultCode: "global",
        codes: [
            {"Global": "global"}
        ]
    },

    types: [
        {"Songs": "track"},
        {"Artists": "artist"},
        {"Albums": "album"}
    ],

    /**
     * Get the charts from the server specified by the given params map and parse them into the
     * correct result format.
     *
     * @param params A map containing all of the necessary parameters describing the charts which to
     *               get from the server.
     *
     *               Example:
     *               { countryCode: "us",                //country code from the countryCodes map
     *                 type: "regional" }                //type from the types map
     *
     * @returns A map consisting of the contentType and parsed results.
     *
     *          Example:
     *          { contentType: Tomahawk.UrlType.Track,
     *            results: [
     *              { track: "We will rock you",
     *                artist: "Queen",
     *                album: "Greatest Hits" },
     *              { track: "Bohemian rhapsody",
     *                artist: "Queen",
     *                album: "Greatest Hits" }
     *            ]
     *          }
     *
     */
    charts: function (params) {
        var url = this._baseUrl;
        var options = {
            data: {
                type: params.type
            }
        };
        return Tomahawk.get(url, options).then(function (response) {
            var chartItemsMaps = {};
            response.chartItems.forEach(function (item) {
                chartItemsMaps[item.id] = item;
            });
            var tracksMaps = {};
            if (response.tracks) {
                response.tracks.forEach(function (item) {
                    tracksMaps[item.id] = item;
                });
            }
            var artistsMaps = {};
            response.artists.forEach(function (item) {
                artistsMaps[item.id] = item;
            });
            var albumsMaps = {};
            if (response.albums) {
                response.albums.forEach(function (item) {
                    albumsMaps[item.id] = item;
                });
            }
            var parsedResults = [];
            for (var i = 0; i < response.chart[0].chartItems.length; i++) {
                var chartItemId = response.chart[0].chartItems[i];
                var chartItem = chartItemsMaps[chartItemId];
                if (params.type == "track") {
                    var track = tracksMaps[chartItem.track];
                    parsedResults.push({
                        track: track.name,
                        artist: artistsMaps[track.artist].name,
                        album: ""
                    });
                } else if (params.type == "artist") {
                    parsedResults.push({
                        artist: artistsMaps[chartItem.artist].name
                    });
                } else if (params.type == "album") {
                    var album = albumsMaps[chartItem.album];
                    parsedResults.push({
                        artist: artistsMaps[album.artist].name,
                        album: album.name
                    });
                }
            }
            var contentType;
            if (params.type == "track") {
                contentType = Tomahawk.UrlType.Track;
            } else if (params.type == "artist") {
                contentType = Tomahawk.UrlType.Artist;
            } else if (params.type == "album") {
                contentType = Tomahawk.UrlType.Album;
            }
            return {
                contentType: contentType,
                results: parsedResults
            };
        });
    }

});
