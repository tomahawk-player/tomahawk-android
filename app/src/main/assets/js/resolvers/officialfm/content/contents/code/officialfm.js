/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2011, lasconic <lasconic@gmail.com>
 *   Copyright 2011, Leo Franchi <lfranchi@kde.org>
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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

var OfficialfmResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'Official.fm',
        icon: 'officialfm-icon.png',
        weight: 70,
        timeout: 5
    },

    _baseUrl: "http://api.official.fm/tracks/search",

    _apiKey: "lcghXySUP3nmYYpOALbPUJ6g30V1Z5hl",

    _convertTracks: function (results, limit) {
        var tracks = [];
        limit = limit || 9999;
        for (var i = 0; i < results.length && i < limit; i++) {
            var result = results[i].track;
            if (result.streaming && result.streaming.http) {
                tracks.push({
                    artist: result.artist,
                    track: result.title,
                    source: this.settings.name,
                    url: result.streaming.http,
                    duration: result.duration
                });
            }
        }
        return tracks;
    },

    _searchRequest: function (query, limit) {
        var that = this;

        var settings = {
            data: {
                api_key: this._apiKey,
                fields: "streaming",
                api_version: "2.0",
                q: query
            }
        };
        return Tomahawk.get(this._baseUrl, settings).then(function (response) {
            return that._convertTracks(response.tracks, limit);
        });
    },

    resolve: function (params) {
        var artist = params.artist;
        var album = params.album;
        var track = params.track;

        return this._searchRequest(artist + " " + track, 20);
    },

    search: function (params) {
        var query = params.query;

        return this._searchRequest(query);
    }
});

Tomahawk.resolver.instance = OfficialfmResolver;
