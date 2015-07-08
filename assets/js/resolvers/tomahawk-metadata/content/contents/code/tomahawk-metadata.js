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

var TomahawkMetadataResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'Tomahawk Metadata',
        icon: 'tomahawk-metadata.png',
        weight: 0, // We cannot resolve, so use minimum weight
        timeout: 15
    },

    canParseUrl: function (params) {
        var url = params.url;
        var type = params.type;

        switch (type) {
            case Tomahawk.UrlType.Album:
                return /^tomahawk:\/\/view\/album\/?\?$/.test(url);
            case Tomahawk.UrlType.Artist:
                return /^tomahawk:\/\/view\/artist\/?\?$/.test(url);
            case Tomahawk.UrlType.Track:
                return /^tomahawk:\/\/(queue\/add|play)\/track\/?\?$/.test(url);
            default:
                return false;
        }
    },

    lookupUrl: function (params) {
        var url = params.url;

        Tomahawk.log("lookupUrl: " + url);
        if (/^tomahawk:\/\/view\/album\/?\?$/.test(url)) {
            Tomahawk.log("Found an album");
            // We have to deal with an Album
            return {
                type: Tomahawk.UrlType.Album,
                artist: this._getQueryVariable(url, 'artist'),
                album: this._getQueryVariable(url, 'name')
            };
        } else if (/^tomahawk:\/\/view\/artist\/?\?$/.test(url)) {
            Tomahawk.log("Found an artist");
            // We have to deal with an Artist
            return {
                type: Tomahawk.UrlType.Artist,
                artist: this._getQueryVariable(url, 'name')
            };
        } else if (/^tomahawk:\/\/(queue\/add|play)\/track\/?\?$/.test(url)) {
            Tomahawk.log("Found a track");
            // We have to deal with a Track
            return {
                type: Tomahawk.UrlType.Track,
                artist: this._getQueryVariable(url, 'artist'),
                track: this._getQueryVariable(url, 'title')
            };
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

