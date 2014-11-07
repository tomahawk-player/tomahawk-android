/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Adrian Sampson <adrian@radbox.org>
 *   Copyright 2013, Uwe L. Korn <uwelk@xhochy.com>
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

// Map all the audio types supported by beets to extensions and MIME types.
var AUDIO_TYPES = {
    'MP3':      ['mp3',  'audio/mpeg'],
    'AAC':      ['m4a',  'audio/mp4'],
    'OGG':      ['ogg',  'audio/ogg'],
    'FLAC':     ['flac', 'audio/x-flac'],
    'APE':      ['ape',  'audio/ape'],
    'WavPack':  ['wv',   'audio/x-wavpack'],
    'MusePack': ['mpc',  'audio/x-musepack']
};

// Backward compability for Tomahawk<0.7.100/API<0.2.0 which did not support authed requests
if (Tomahawk.hasOwnProperty('apiVersion') && Tomahawk.atLeastVersion('0.2.0')) {
    var passwordRequest = function (url, username, password, cb, errorHandler) {
        Tomahawk.asyncRequest(url, cb, {}, {username: username, password: password, errorHandler: errorHandler});
    };
} else {
    var passwordRequest = function (url, username, password, cb, errorHandler) {
        var xmlHttpRequest = new XMLHttpRequest();
        xmlHttpRequest.open('GET', url, true, username, password);
        xmlHttpRequest.onreadystatechange = function() {
            if (xmlHttpRequest.readyState == 4 && xmlHttpRequest.status == 200) {
                cb(xmlHttpRequest);
            } else if (xmlHttpRequest.readyState == 4) {
                errorHandler(xmlHttpRequest);
            }
        };
        xmlHttpRequest.send(null);
    };
}

var BeetsResolver = Tomahawk.extend(TomahawkResolver, {
    trackCount: -1,
    settings: {
        name: 'beets',
        icon: 'beets-icon.png',
        weight: 95,
        timeout: 5
    },

    // Resolution.
    resolve: function (qid, artist, album, title) {
        if (album == '') {
            this.beetsQuery(qid, ['artist:' + artist, 'title:' + title]);
        } else {
            this.beetsQuery(qid, ['artist:' + artist, 'album:' + album, 'title:' + title]);
        }
    },

    search: function (qid, searchString) {
        this.beetsQuery(qid, searchString.split(' '));
    },

    baseUrl: function () {
        if (this.useTLS) {
            return 'https://' + this.host + ':' + this.port;
        } else {
            return 'http://' + this.host + ':' + this.port;
        }
    },

    beetsQuery: function (qid, queryParts) {
        var baseUrl = this.baseUrl(),
            url = this.baseUrl() + '/item/query/';
        queryParts.forEach(function (item) {
            url += encodeURIComponent(item);
            url += '/';
        });
        url = url.substring(0, url.length - 1);  // Remove last /.

        passwordRequest(url, this.username, this.password, function (xhr) {
            var resp = JSON.parse(xhr.responseText),
            items = resp.results,
            searchResults = [];
            items.forEach(function (item) {
                var type_info = AUDIO_TYPES[item.format];
                searchResults.push({
                    artist: item.artist,
                    album: item.album,
                    track: item.title,
                    albumpos: item.track,
                    source: "beets",
                    url: baseUrl + '/item/' + item.id + '/file',
                    bitrate: Math.floor(item.bitrate / 1024),
                    duration: Math.floor(item.length),
                    size: (item.size || 0),
                    score: 1.0,
                    extension: type_info[0],
                    mimetype: type_info[1],
                    year: item.year
                });
            });
            Tomahawk.addTrackResults({
                qid: qid,
                results: searchResults
            });
        });
    },

    // Configuration.
    getConfigUi: function () {
        var uiData = Tomahawk.readBase64("config.ui");
        return {
            "widget": uiData,
            "fields": [{
                name: "host",
                widget: "hostField",
                property: "text"
            }, {
                name: "port",
                widget: "portField",
                property: "text"
            }, {
                name: "useTLS",
                widget: "tlsCheckBox",
                property: "checked"
            },{
                name: "useAuth",
                widget: "useAuthCheckBox",
                property: "checked"
            }, {
                name: "username",
                widget: "usernameField",
                property: "text"
            }, {
                name: "password",
                widget: "passwordField",
                property: "text"
            }]
        };
    },
    newConfigSaved: function () {
        this.init();
    },
    init: function () {
        var userConfig = this.getUserConfig(),
            that = this;
        this.host = userConfig.host || 'localhost';
        this.port = parseInt(userConfig.port, 10);
        this.useTLS = userConfig.useTLS;
        this.useAuth = userConfig.useAuth;
        if (this.useAuth) {
            this.username = userConfig.username;
            this.password = userConfig.password;
        } else {
            this.username = null;
            this.password = null;
        }
        if (isNaN(this.port) || !this.port) {
            this.port = 8337;
        }
        // Invalidate trackCount
        // Check if /stats is available and we can get enough information for ScriptCollection support and track count
        // (this works for beets 1.2.1+)
        passwordRequest(this.baseUrl() + '/stats', this.username, this.password, function (xhr) {
            // Success
            that.trackCount = parseInt(JSON.parse(xhr.responseText).items);
            Tomahawk.reportCapabilities(TomahawkResolverCapability.Browsable);
        }, function (xhr) {
            // Failed
            that.trackCount = -1;
            // Check if /artist/ is available and we can get enough information for ScriptCollection support
            // (this is needed for beets 1.1.0-1.2.0)
            passwordRequest(that.baseUrl() + '/artist/', this.username, this.password, function () {
                // Success
                Tomahawk.reportCapabilities(TomahawkResolverCapability.Browsable);
            }, function () {
                // Failed
                Tomahawk.reportCapabilities(TomahawkResolverCapability.NullCapability);
            });
        });
    },

    configTest: function () {
        Tomahawk.asyncRequest(this.baseUrl(),
            function (xhr) {
                Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Success);
            }, {}, {
                method: 'HEAD',
                username: this.username,
                password: this.password,
                errorHandler: function (xhr) {
                    if (xhr.status == 403) {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidCredentials);
                    } else if (xhr.status == 404 || xhr.status == 0) {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                    } else {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                            xhr.responseText.trim());
                    }
                }
            }
        );
    },

    // Script Collection Support

    artists: function (qid) {
        var url = this.baseUrl() + '/artist/';
        passwordRequest(url, this.username, this.password, function (xhr) {
            var response = JSON.parse(xhr.responseText);
            Tomahawk.addArtistResults({
                qid: qid,
                artists: response.artist_names
            });
        });
    },

    albums: function (qid, artist) {
        var url = this.baseUrl() + '/album/query/albumartist:' + encodeURIComponent(artist);
        passwordRequest(url, this.username, this.password, function (xhr) {
            var response = JSON.parse(xhr.responseText),
            results = [];
            response.results.forEach(function (item) {
                results.push(item.album);
            });
            Tomahawk.addAlbumResults({
                qid: qid,
                artist: artist,
                albums: results
            });
        });
    },

    tracks: function (qid, artist, album) {
        var url = this.baseUrl() + '/item/query/' + encodeURIComponent('artist:' + artist) + '/' + encodeURIComponent('album:' + album);
        var baseUrl = this.baseUrl();
        passwordRequest(url, this.username, this.password, function (xhr) {
            var response = JSON.parse(xhr.responseText),
            searchResults = [];
            response.results.forEach(function (item) {
                var type_info = AUDIO_TYPES[item.format];
                searchResults.push({
                    artist: item.artist,
                    album: item.album,
                    track: item.title,
                    albumpos: item.track,
                    source: "beets",
                    url: baseUrl + '/item/' + item.id + '/file',
                    bitrate: Math.floor(item.bitrate / 1024),
                    duration: Math.floor(item.length),
                    size: (item.size || 0),
                    score: 1.0,
                    extension: type_info[0],
                    mimetype: type_info[1],
                    year: item.year
                });
            });
            searchResults.sort(function (a, b) {
                if (a.albumpos < b.albumpos) {
                    return -1;
                } else if (a.albumpos > b.albumpos) {
                    return 1;
                } else {
                    return 0;
                }
            });
            Tomahawk.addAlbumTrackResults({
                qid: qid,
                artist: artist,
                album: album,
                results: searchResults
            });
        });
    },

    collection: function () {
        if (this.trackCount > -1) {
            return {
                prettyname: "Beets",
                description: this.host,
                iconfile: 'beets-icon.png',
                trackcount: this.trackCount
            };
        } else {
            // We cannot get the trackcout for this collection
            return {
                prettyname: "Beets",
                description: this.host,
                iconfile: 'beets-icon.png'
            };
        }
    }
});

Tomahawk.resolver.instance = BeetsResolver;
