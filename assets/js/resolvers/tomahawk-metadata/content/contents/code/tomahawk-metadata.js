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

var TomahawkMetadataResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'Tomahawk Metadata',
        icon: 'tomahawk-metadata.png',
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
                return /^tomahawk:\/\/view\/album\/?\?$/.test(url);
            case TomahawkUrlType.Artist:
                return /^tomahawk:\/\/view\/artist\/?\?$/.test(url);
            case TomahawkUrlType.Track:
                return /^tomahawk:\/\/(queue\/add|play)\/track\/?\?$/.test(url);
            default:
                return false;
        }
    },

    lookupUrl: function (url) {
        Tomahawk.log("lookupUrl: " + url);
        if (/^tomahawk:\/\/view\/album\/?\?$/.test(url)) {
            Tomahawk.log("Found an album");
            // We have to deal with an Album
            Tomahawk.addUrlResult(url, {
                type: 'album',
                artist: this._getQueryVariable(url, 'artist'),
                name: this._getQueryVariable(url, 'name')
            });
        } else if (/^tomahawk:\/\/view\/artist\/?\?$/.test(url)) {
            Tomahawk.log("Found an artist");
            // We have to deal with an Artist
            Tomahawk.addUrlResult(url, {
                type: 'artist',
                name: this._getQueryVariable(url, 'name')
            });
        } else if (/^tomahawk:\/\/(queue\/add|play)\/track\/?\?$/.test(url)) {
            Tomahawk.log("Found a track");
            // We have to deal with a Track
            Tomahawk.addUrlResult(url, {
                type: "track",
                artist: this._getQueryVariable(url, 'artist'),
                title: this._getQueryVariable(url, 'title')
            });
        }
    },

    _getQueryVariable: function (url, variable) {
        var parts = url.split('?');
        var vars = parts[parts.length - 1].split('&');
        for (var i = 0; i < vars.length; i++) {
            var pair = vars[i].split('=');
            if (decodeURIComponent(pair[0]) == variable) {
                return decodeURIComponent(pair[1]);
            }
        }
    }
});

Tomahawk.resolver.instance = TomahawkMetadataResolver;

