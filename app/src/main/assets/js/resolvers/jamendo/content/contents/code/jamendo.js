/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2011, lasconic <lasconic@gmail.com>
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

var JamendoResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'Jamendo',
        icon: 'jamendo-icon.png',
        weight: 75,
        timeout: 5
    },

    _baseUrl: "https://api.jamendo.com/v3.0/tracks/",

    _clientId: "f52d7f12",

    _convertTracks: function (results) {
        var tracks = [];
        for (var i = 0; i < results.length; i++) {
            var result = results[i];
            tracks.push({
                artist: result.artist_name,
                album: result.album_name,
                track: result.name,
                source: this.settings.name,
                url: decodeURI(result.audio),
                duration: result.duration
            });
        }
        return tracks;
    },

    _searchRequest: function (query, limit) {
        var that = this;

        var settings = {
            data: {
                client_id: this._clientId,
                format: "json",
                limit: limit,
                search: query
            }
        };
        return Tomahawk.get(this._baseUrl, settings).then(function (response) {
            return that._convertTracks(response.results);
        });
    },

    resolve: function (params) {
        var artist = params.artist;
        var album = params.album;
        var track = params.track;

        return this._searchRequest(artist + " " + track, 5);
    },

    search: function (params) {
        var query = params.query;

        return this._searchRequest(query, 20);
    }
});

Tomahawk.resolver.instance = JamendoResolver;
