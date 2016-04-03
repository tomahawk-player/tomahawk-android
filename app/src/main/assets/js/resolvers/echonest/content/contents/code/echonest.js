/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
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
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */

Tomahawk.PluginManager.registerPlugin('playlistGenerator', {

    _baseUrl: 'http://developer.echonest.com/api/v4/',

    _apiKey: 'WEVUJRC6TCBRE2DD6',

    /**
     * Searches the source for available playlist seeds like artists or songs.
     * The results from this function are later being used to create a station playlist.
     *
     * @param params Example: {  query: "Queen rock you"  }
     *
     * @returns Example: {   artists: [ { artist: 'Queen' },
      *                                 { artist: 'Queens' } ],
     *                       albums:  [ { artist: 'Queen', album: 'Greatest Hits' } ],
     *                       tracks:  [ { artist: 'Queen', track: 'We will rock you' } ],
     *                       genres:  [ { name: 'Rock' },
      *                                 { name: 'Alternative Rock' } ],
     *                       moods:   [ { name: 'Happy' } ]   }
     */
    search: function (params) {
        var endpoints = [
            ['artist/search', 'name'],
            ['song/search', 'combined'],
            ['genre/search', 'name']
        ];
        var promises = [];
        for (var i = 0; i < endpoints.length; i++) {
            var url = this._baseUrl + endpoints[i][0];
            var options = {
                data: {
                    api_key: this._spell(this._apiKey),
                    format: 'json',
                    results: 5
                }
            };
            options.data[endpoints[i][1]] = params.query;
            promises.push(Tomahawk.get(url, options));
        }

        var that = this;
        return RSVP.all(promises).then(function (results) {
            var artists = [];
            if (results[0].response.artists) {
                for (var i = 0; i < results[0].response.artists.length; i++) {
                    artists.push({
                        artist: results[0].response.artists[i].name
                    });
                }
            }
            var tracks = [];
            if (results[1].response.songs) {
                for (var i = 0; i < results[1].response.songs.length; i++) {
                    tracks.push({
                        artist: results[1].response.songs[i].artist_name,
                        track: results[1].response.songs[i].title,
                        album: '',
                        songId: results[1].response.songs[i].id
                    });
                }
            }
            var genres = [];
            if (results[2].response.genres) {
                for (var i = 0; i < results[2].response.genres.length; i++) {
                    genres.push({
                        name: results[2].response.genres[i].name
                    });
                }
            }

            return {
                artists: artists,
                albums: [],
                tracks: tracks,
                genres: genres,
                moods: []
            };
        });
    },

    createStation: function (params) {
        var url, options;
        if (params.artists) {
            url = this._baseUrl + 'playlist/dynamic/create';
            var artists = [];
            for (var i = 0; i < params.artists.length; i++) {
                artists.push(params.artists[i].artist);
            }
            options = {
                data: {
                    api_key: this._spell(this._apiKey),
                    format: 'json',
                    results: 1,
                    type: 'artist-radio',
                    artist: artists
                }
            };
        } else if (params.tracks) {
            url = this._baseUrl + 'playlist/dynamic/create';
            var song_ids = [];
            for (var i = 0; i < params.tracks.length; i++) {
                song_ids.push(params.tracks[i].songId);
            }
            options = {
                data: {
                    api_key: this._spell(this._apiKey),
                    format: 'json',
                    results: 1,
                    type: 'song-radio',
                    song_id: song_ids
                }
            };
        } else if (params.genres) {
            url = this._baseUrl + 'playlist/dynamic/create';
            var genres = [];
            for (var i = 0; i < params.genres.length; i++) {
                genres.push(params.genres[i].name);
            }
            options = {
                data: {
                    api_key: this._spell(this._apiKey),
                    format: 'json',
                    type: 'genre-radio',
                    genre: genres
                }
            };
        }
        return Tomahawk.get(url, options).then(function (result) {
            var results = [];
            if (result.response.songs) {
                for (var i = 0; i < result.response.songs.length; i++) {
                    results.push({
                        artist: result.response.songs[i].artist_name,
                        track: result.response.songs[i].title,
                        album: ''
                    });
                }
            }
            return {
                sessionId: result.response.session_id,
                results: results
            };
        });
    },

    fillStation: function (params) {
        var url = this._baseUrl + 'playlist/dynamic/next';
        var options = {
            data: {
                api_key: this._spell(this._apiKey),
                format: 'json',
                results: 5,
                session_id: params.sessionId
            },
            headers: {
                'Cache-Control': 'no-cache'
            }
        };
        return Tomahawk.get(url, options).then(function (result) {
            var results = [];
            if (result.response.songs) {
                for (var i = 0; i < result.response.songs.length; i++) {
                    results.push({
                        artist: result.response.songs[i].artist_name,
                        track: result.response.songs[i].title,
                        album: ''
                    });
                }
            }
            return {
                sessionId: params.sessionId,
                results: results
            };
        });
    },

    _spell: function (a) {
        var magic = function (b) {
            return (b = (b) ? b : this).split("").map(function (d) {
                if (!d.match(/[A-Za-z]/)) {
                    return d
                }
                var c = d.charCodeAt(0) >= 96;
                var k = (d.toLowerCase().charCodeAt(0) - 96 + 12) % 26 + 1;
                return String.fromCharCode(k + (c ? 96 : 64))
            }).join("")
        };
        return magic(a)
    }

});