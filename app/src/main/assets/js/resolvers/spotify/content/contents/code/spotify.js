/*
 *   Copyright 2014, Uwe L. Korn <uwelk@xhochy.com>
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   The MIT License (MIT)
 *
 *   Permission is hereby granted, free of charge, to any person obtaining a copy
 *   of this software and associated documentation files (the "Software"), to deal
 *   in the Software without restriction, including without limitation the rights
 *   to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *   copies of the Software, and to permit persons to whom the Software is
 *   furnished to do so, subject to the following conditions:
 *
 *   The above copyright notice and this permission notice shall be included in
 *   all copies or substantial portions of the Software.
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *   FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *   COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

var SpotifyResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'Spotify',
        icon: 'spotify.png',
        weight: 95,
        timeout: 15
    },

    clientId: "q3r9p989687p496no2s92p9r84s779qp",

    clientSecret: "789r9n607poo4s9no6998771s969o630",

    redirectUri: "tomahawkspotifyresolver://callback",

    storageKeyRefreshToken: "spotify_refresh_token",

    /**
     * Get the access token. Refresh when it is expired.
     */
    getAccessToken: function () {
        var that = this;
        if (!this.getAccessTokenPromise || new Date().getTime() + 60000 > that.accessTokenExpires) {
            Tomahawk.log("Access token is not valid. We need to get a new one.");
            this.getAccessTokenPromise = new RSVP.Promise(function (resolve, reject) {
                var refreshToken = Tomahawk.localStorage.getItem(that.storageKeyRefreshToken);
                if (!refreshToken) {
                    Tomahawk.log("Can't fetch new access token, because there's no stored refresh "
                        + "token. Are you logged in?");
                    reject("Can't fetch new access token, because there's no stored refresh"
                        + " token. Are you logged in?");
                }
                resolve(refreshToken);
            }).then(function (result) {
                Tomahawk.log("Fetching new access token ...");
                var settings = {
                    headers: {
                        "Authorization": "Basic "
                        + Tomahawk.base64Encode(that._spell(that.clientId)
                            + ":" + that._spell(that.clientSecret)),
                        "Content-Type": "application/x-www-form-urlencoded"
                    },
                    data: {
                        "grant_type": "refresh_token",
                        "refresh_token": result
                    }
                };
                return Tomahawk.post("https://accounts.spotify.com/api/token", settings)
                    .then(function (res) {
                        that.accessToken = res.access_token;
                        that.accessTokenExpires = new Date().getTime() + res.expires_in
                            * 1000;
                        Tomahawk.log("Received new access token!");
                        return {
                            accessToken: res.access_token
                        };
                    });
            });
        }
        return this.getAccessTokenPromise;
    },

    login: function () {
        Tomahawk.log("Starting login");

        var authUrl = "https://accounts.spotify.com/authorize";
        authUrl += "?client_id=" + this._spell(this.clientId);
        authUrl += "&response_type=code";
        authUrl += "&redirect_uri=" + encodeURIComponent(this.redirectUri);
        authUrl
            += "&scope=playlist-read-private%20streaming%20user-read-private%20user-library-read";
        authUrl += "&show_dialog=true";

        var that = this;

        var params = {
            url: authUrl
        };
        return Tomahawk.NativeScriptJobManager.invoke("showWebView", params).then(
            function (result) {
                var error = that._getParameterByName(result.url, "error");
                if (error) {
                    Tomahawk.log("Authorization failed: " + error);
                    return error;
                } else {
                    Tomahawk.log("Authorization successful, fetching new refresh token ...");
                    var settings = {
                        headers: {
                            "Authorization": "Basic "
                            + Tomahawk.base64Encode(that._spell(that.clientId)
                                + ":" + that._spell(that.clientSecret)),
                            "Content-Type": "application/x-www-form-urlencoded"
                        },
                        data: {
                            grant_type: "authorization_code",
                            code: encodeURIComponent(that._getParameterByName(result.url, "code")),
                            redirect_uri: encodeURIComponent(that.redirectUri)
                        }
                    };

                    return Tomahawk.post("https://accounts.spotify.com/api/token", settings)
                        .then(function (response) {
                            Tomahawk.localStorage.setItem(that.storageKeyRefreshToken,
                                response.refresh_token);
                            Tomahawk.log("Received new refresh token!");
                            return TomahawkConfigTestResultType.Success;
                        });
                }
            });
    },

    logout: function () {
        Tomahawk.localStorage.removeItem(this.storageKeyRefreshToken);
        return TomahawkConfigTestResultType.Logout;
    },

    isLoggedIn: function () {
        var refreshToken = Tomahawk.localStorage.getItem(this.storageKeyRefreshToken);
        return refreshToken !== null && refreshToken.length > 0;
    },

    /**
     * Returns the value of the query parameter with the given name from the given URL.
     */
    _getParameterByName: function (url, name) {
        name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
        var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
            results = regex.exec(url);
        return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
    },

    _spell: function (a) {
        magic = function (b) {
            return (b = (b) ? b : this).split("").map(function (d) {
                if (!d.match(/[A-Za-z]/)) {
                    return d
                }
                c = d.charCodeAt(0) >= 96;
                k = (d.toLowerCase().charCodeAt(0) - 96 + 12) % 26 + 1;
                return String.fromCharCode(k + (c ? 96 : 64))
            }).join("")
        };
        return magic(a)
    },

    init: function () {
        Tomahawk.PluginManager.registerPlugin("linkParser", this);
    },

    getStreamUrl: function (params) {
        var url = params.url;

        return {
            url: url.replace("spotify://track/", "")
        };
    },

    resolve: function (params) {
        var artist = params.artist;
        var album = params.album;
        var track = params.track;

        var q = "artist:\"" + artist + "\" track:\"" + track + "\"";
        if (album) {
            q += " album:\"" + album + "\"";
        }

        return this._search(q);
    },

    search: function (params) {
        var query = params.query;

        return this._search(query);
    },

    _search: function (query) {
        var that = this;

        return this.getAccessToken().then(function (result) {
            var url = "https://api.spotify.com/v1/search";
            var settings = {
                data: {
                    market: "from_token",
                    type: "track",
                    q: query
                },
                headers: {
                    Authorization: "Bearer " + result.accessToken
                }
            };
            return Tomahawk.get(url, settings).then(function (response) {
                return response.tracks.items.map(function (item) {
                    return {
                        artist: item.artists[0].name,
                        album: item.album.name,
                        duration: item.duration_ms / 1000,
                        source: that.settings.name,
                        track: item.name,
                        url: "spotify://track/" + item.id
                    };
                });
            });
        });
    },

    canParseUrl: function (params) {
        var url = params.url;
        var type = params.type;

        if (!url) {
            throw new Error("Provided url was empty or null!");
        }
        switch (type) {
            case TomahawkUrlType.Album:
                return /spotify:album:([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/album\/([^\/\?]+)/.test(url);
            case TomahawkUrlType.Artist:
                return /spotify:artist:([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/artist\/([^\/\?]+)/.test(url);
            case TomahawkUrlType.Playlist:
                return /spotify:user:([^:]+):playlist:([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/user\/([^\/]+)\/playlist\/([^\/\?]+)/.test(url);
            case TomahawkUrlType.Track:
                return /spotify:track:([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/track\/([^\/\?]+)/.test(url);
            // case TomahawkUrlType.Any:
            default:
                return /spotify:(album|artist|track):([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/(album|artist|track)\/([^\/\?]+)/.test(url)
                    || /spotify:user:([^:]+):playlist:([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/user\/([^\/]+)\/playlist\/([^\/\?]+)/.test(url);
        }
    },

    lookupUrl: function (params) {
        var url = params.url;
        Tomahawk.log("lookupUrl: " + url);

        var match = url.match(/spotify[/:]+(album|artist|track)[/:]+([^/:?]+)/);
        if (match == null) {
            match
                = url.match(/https?:\/\/(?:play|open)\.spotify\.[^\/]+\/(album|artist|track)\/([^\/\?]+)/);
        }
        var playlistmatch = url.match(/spotify[/:]+user[/:]+([^/:]+)[/:]+playlist[/:]+([^/:?]+)/);
        if (playlistmatch == null) {
            playlistmatch
                = url.match(/https?:\/\/(?:play|open)\.spotify\.[^\/]+\/user\/([^\/]+)\/playlist\/([^\/\?]+)/);
        }
        if (match != null) {
            var query = 'https://ws.spotify.com/lookup/1/.json?uri=spotify:' + match[1] + ':'
                + match[2];
            Tomahawk.log("Found album/artist/track, calling " + query);
            return Tomahawk.get(query).then(function (response) {
                if (match[1] == "artist") {
                    Tomahawk.log("Reported found artist '" + response.artist.name + "'");
                    return {
                        type: Tomahawk.UrlType.Artist,
                        artist: response.artist.name
                    };
                } else if (match[1] == "album") {
                    Tomahawk.log("Reported found album '" + response.album.name + "' by '"
                        + response.album.artist + "'");
                    return {
                        type: Tomahawk.UrlType.Album,
                        album: response.album.name,
                        artist: response.album.artist
                    };
                } else if (match[1] == "track") {
                    var artist = response.track.artists.map(function (item) {
                        return item.name;
                    }).join(" & ");
                    Tomahawk.log("Reported found track '" + response.track.name + "' by '" + artist
                        + "'");
                    return {
                        type: Tomahawk.UrlType.Track,
                        track: response.track.name,
                        artist: artist
                    };
                }
            });
        } else if (playlistmatch != null) {
            var query = 'http://spotikea.tomahawk-player.org/browse/spotify:user:'
                + playlistmatch[1] + ':playlist:' + playlistmatch[2];
            Tomahawk.log("Found playlist, calling url: '" + query + "'");
            return Tomahawk.get(query).then(function (res) {
                var tracks = res.playlist.result.map(function (item) {
                    return {
                        type: Tomahawk.UrlType.Track,
                        track: item.title,
                        artist: item.artist
                    };
                });
                Tomahawk.log("Reported found playlist '" + res.playlist.name + "' containing "
                    + tracks.length + " tracks");
                return {
                    type: Tomahawk.UrlType.Playlist,
                    title: res.playlist.name,
                    guid: "spotify-playlist-" + url,
                    info: "A playlist on Spotify.",
                    creator: res.playlist.creator,
                    linkUrl: url,
                    tracks: tracks
                };
            });
        }
    }
});

Tomahawk.resolver.instance = SpotifyResolver;

Tomahawk.PluginManager.registerPlugin('chartsProvider', {

    _baseUrl: "https://spotifycharts.com/api/",

    countryCodes: {
        defaultCode: "global",
        codes: [
            {"Global": "global"},
            {"United States": "us"},
            {"United Kingdom": "gb"},
            {"Andorra": "ad"},
            {"Argentina": "ar"},
            {"Australia": "au"},
            {"Austria": "at"},
            {"Belgium": "be"},
            {"Bolivia": "bo"},
            {"Brazil": "br"},
            {"Bulgaria": "bg"},
            {"Canada": "ca"},
            {"Chile": "cl"},
            {"Colombia": "co"},
            {"Costa Rica": "cr"},
            {"Cyprus": "cy"},
            {"Czech Republic": "cz"},
            {"Denmark": "dk"},
            {"Dominican Republic": "do"},
            {"Ecuador": "ec"},
            {"El Salvador": "sv"},
            {"Estonia": "ee"},
            {"Finland": "fi"},
            {"France": "fr"},
            {"Germany": "de"},
            {"Greece": "gr"},
            {"Guatemala": "gt"},
            {"Honduras": "hn"},
            {"Hong Kong": "hk"},
            {"Hungary": "hu"},
            {"Iceland": "is"},
            {"Ireland": "ie"},
            {"Italy": "it"},
            {"Latvia": "lv"},
            {"Lithuania": "lt"},
            {"Luxembourg": "lu"},
            {"Malaysia": "my"},
            {"Malta": "mt"},
            {"Mexico": "mx"},
            {"Netherlands": "nl"},
            {"New Zealand": "nz"},
            {"Nicaragua": "ni"},
            {"Norway": "no"},
            {"Panama": "pa"},
            {"Paraguay": "py"},
            {"Peru": "pe"},
            {"Philippines": "ph"},
            {"Poland": "pl"},
            {"Portugal": "pt"},
            {"Singapore": "sg"},
            {"Slovakia": "sk"},
            {"Spain": "es"},
            {"Sweden": "se"},
            {"Switzerland": "ch"},
            {"Taiwan": "tw"},
            {"Turkey": "tr"},
            {"Uruguay": "uy"}
        ]
    },

    types: [
        {"Top 200": "regional"},
        {"Viral 50": "viral"}
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
                country: params.countryCode,
                recurrence: "daily",
                date: "latest",
                type: params.type
            }
        };
        return Tomahawk.get(url, options).then(function (response) {
            var parsedResults = [];
            for (var i = 0; i < response.entries.items.length; i++) {
                var entry = response.entries.items[i];
                parsedResults.push({
                    track: entry.track.name,
                    artist: entry.track.artists[0].name,
                    album: ""
                });
            }
            return {
                contentType: Tomahawk.UrlType.Track,
                results: parsedResults
            };
        });
    }

});

