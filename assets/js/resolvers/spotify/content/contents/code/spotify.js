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

var SpotifyResolver = Tomahawk.extend(TomahawkResolver, {

    settings: {
        name: 'Spotify',
        icon: 'spotify.png',
        weight: 95,
        timeout: 15
    },

    clientId : "q3r9p989687p496no2s92p9r84s779qp",

    clientSecret : "789r9n607poo4s9no6998771s969o630",

    redirectUri: "tomahawkspotifyresolver://callback",

    storageKeyRefreshToken: "spotify_refresh_token",

    storageKeyAccessToken: "spotify_access_token",

    storageKeyAccessTokenExpires: "spotify_access_token_expires",

    newConfigSaved: function () {
    },

    /**
     * Get the access token. Refresh when it is expired.
     */
    getAccessToken: function () {
        var that = this;
        if (!this.getAccessTokenPromise){
            this.getAccessTokenPromise = new Promise(function (resolve, reject) {
                if (new Date().getTime() + 60000 > that.accessTokenExpires) {
                    Tomahawk.log("Access token is no longer valid. We need to get a new one.");
                    var refreshToken = Tomahawk.localStorage.getItem(that.storageKeyRefreshToken);
                    if (refreshToken) {
                        Tomahawk.log("Fetching new access token ...");
                        var settings = {
                            headers: {
                                "Authorization": "Basic "
                                    + Tomahawk.base64Encode(that.spell(that.clientId)
                                    + ":" + that.spell(that.clientSecret)),
                                "Content-Type": "application/x-www-form-urlencoded"
                            },
                            data: "grant_type=refresh_token&refresh_token="
                                + encodeURIComponent(refreshToken)
                        };
                        Tomahawk.post("https://accounts.spotify.com/api/token", settings).then(
                            function(responseText) {
                                var res = JSON.parse(responseText);
                                that.accessToken = res.access_token;
                                that.accessTokenExpires = new Date().getTime() + res.expires_in * 1000;
                                Tomahawk.localStorage.setItem(that.storageKeyAccessToken, that.accessToken);
                                Tomahawk.localStorage.setItem(that.storageKeyAccessTokenExpires,
                                    that.accessTokenExpires);
                                Tomahawk.log("Received new access token!");
                                resolve({
                                    accessToken: that.accessToken
                                });
                            },
                            function(xhr) {
                                reject({
                                    error: xhr.responseText
                                });
                                Tomahawk.log("Couldn't fetch new access token: " + xhr.responseText);
                            }
                        );
                    } else {
                        reject({
                            error: "Can't fetch new access token, because there's no stored refresh"
                                + " token. Are you logged in?"
                        });
                        Tomahawk.log("Can't fetch new access token, because there's no stored refresh "
                            + "token. Are you logged in?");
                    }
                } else {
                    resolve({
                        accessToken: that.accessToken
                    });
                }
            }.done(function() {
                delete this.getAccessTokenPromise;
            }));
        }
        return this.getAccessTokenPromise;
    },

    login: function(callback) {
        Tomahawk.log("Starting login");

        var authUrl = "https://accounts.spotify.com/authorize";
        authUrl += "?client_id=" + this.spell(this.clientId);
        authUrl += "&response_type=code";
        authUrl += "&redirect_uri=" + encodeURIComponent(this.redirectUri);
        authUrl += "&scope=playlist-read-private%20streaming%20user-read-private%20user-library-read";
        authUrl += "&show_dialog=true";

        Tomahawk.showWebView(authUrl);
    },

    logout: function() {
        Tomahawk.localStorage.removeItem(this.storageKeyRefreshToken);
        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Logout);
    },

    isLoggedIn: function() {
        var refreshToken = Tomahawk.localStorage.getItem(this.storageKeyRefreshToken);
        return refreshToken !== null && refreshToken.length > 0;
    },

    /**
     * This function is being called from the native side whenever it has received a redirect
     * callback. In other words, the WebView shown to the user can call the js side here.
     */
    onRedirectCallback: function(params) {
        var that = this;

        var error = this.getParameterByName(params.query, "error");
        if (error) {
            Tomahawk.log("Authorization failed: " + error);
            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other, error);
        } else {
            Tomahawk.log("Authorization successful, fetching new refresh token ...");
            var code = this.getParameterByName(params.query, "code");
            var data = "grant_type=authorization_code";
            data += "&code=" + encodeURIComponent(code);
            data += "&redirect_uri=" + encodeURIComponent(this.redirectUri);
            var headers = {
                "Authorization": "Basic " + Tomahawk.base64Encode(this.spell(this.clientId)
                    + ":" + this.spell(this.clientSecret)),
                "Content-Type": "application/x-www-form-urlencoded"
            };
            Tomahawk.asyncRequest("https://accounts.spotify.com/api/token", function (xhr) {
                var res = JSON.parse(xhr.responseText);
                that.accessToken = res.access_token;
                that.accessTokenExpires = new Date().getTime() + res.expires_in * 1000;
                Tomahawk.localStorage.setItem(that.storageKeyAccessToken, that.accessToken);
                Tomahawk.localStorage.setItem(that.storageKeyAccessTokenExpires,
                    that.accessTokenExpires);
                Tomahawk.localStorage.setItem(that.storageKeyRefreshToken, res.refresh_token);
                Tomahawk.reportAccessToken(that.accessToken);
                Tomahawk.log("Received new refresh token!");
                Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Success);
            }, headers, {
                method: "POST",
                data: data,
                errorHandler: function(xhr) {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                    Tomahawk.log("error: " + xhr.responseText);
                }
            });
        }
    },

    getParameterByName: function(url, name) {
        name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
        var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
            results = regex.exec(url);
        return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
    },

    spell: function(a){magic=function(b){return(b=(b)?b:this).split("").map(function(d){if(!d.match(/[A-Za-z]/)){return d}c=d.charCodeAt(0)>=96;k=(d.toLowerCase().charCodeAt(0)-96+12)%26+1;return String.fromCharCode(k+(c?96:64))}).join("")};return magic(a)},

    init: function(cb) {
        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);
        Tomahawk.addCustomUrlHandler("spotify", "getStreamUrl", true);
        Tomahawk.addCustomUrlHandler("tomahawkspotifyresolver", "onRedirectCallback", true);

        this.accessToken = Tomahawk.localStorage.getItem(this.storageKeyAccessToken);
        this.accessTokenExpires = Tomahawk.localStorage.getItem(this.storageKeyAccessTokenExpires);
    },

    getStreamUrl: function (qid, url) {
        var trackId = url.replace("spotify://track/", "");
        Tomahawk.reportStreamUrl(qid, trackId);
    },

    resolve: function (qid, artist, album, title) {
        var that = this;

        this.getAccessToken().then(function (result) {
            var searchUrl = "https://api.spotify.com/v1/search?market=from_token"
            searchUrl += "&type=track";
            searchUrl += "&q=artist:" + encodeURIComponent(artist);
            searchUrl += "+track:" + encodeURIComponent(title);
            if (album != "") {
                searchUrl += "+album:" + encodeURIComponent(album);
            };
            Tomahawk.asyncRequest(searchUrl, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                Tomahawk.addTrackResults({
                    qid: qid,
                    results: res.tracks.items.map(function (item) {
                        return {
                            artist: item.artists[0].name,
                            album: item.album.name,
                            duration: item.duration_ms / 1000,
                            source: that.settings.name,
                            track: item.name,
                            url: "spotify://track/" + item.id
                        };
                    })
                });
            }, {
                "Authorization": "Bearer " + result.accessToken
            });
        });
    },

	search: function (qid, searchString) {
        var that = this;

        this.getAccessToken().then(function (result) {
            var searchUrl = "https://api.spotify.com/v1/search?market=from_token"
            // TODO: Artists and Albums
            searchUrl += "&type=track";
            searchUrl += "&q=" + encodeURIComponent(searchString);
            Tomahawk.asyncRequest(searchUrl, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                Tomahawk.addTrackResults({
                    qid: qid,
                    results: res.tracks.items.map(function (item) {
                        return {
                            artist: item.artists[0].name,
                            album: item.album.name,
                            duration: item.duration_ms / 1000,
                            source: that.settings.name,
                            track: item.name,
                            url: "spotify://track/" + item.id
                        };
                    })
                });
            }, {
                "Authorization": "Bearer " + result.accessToken
            });
        });
	},

    canParseUrl: function (url, type) {
        switch (type) {
            case TomahawkUrlType.Album:
                return /spotify:album:([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/album\/([^\/\?]+)/.test(url);
            case TomahawkUrlType.Artist:
                return /spotify:artist:([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/artist\/([^\/\?]+)/.test(url);
            case TomahawkUrlType.Playlist:
                return /spotify:user:([^:]+):playlist:([^:]+)/.test(url)
                    || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/user\/([^\/]+)\/playlist\/([^\/\?]+)/.test(url);;
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

    lookupUrl: function (url) {
        Tomahawk.log("lookupUrl: '" + url + "'");
        var that = this;
        var match = url.match(/spotify:(album|artist|track):([^:]+)/);
        if (match == null) {
            match = url.match(/https?:\/\/(?:play|open)\.spotify\.[^\/]+\/(album|artist|track)\/([^\/\?]+)/);
        }
        var playlistmatch = url.match(/spotify:user:([^:]+):playlist:([^:]+)/);
        if (playlistmatch == null) {
            var playlistmatch = url.match(/https?:\/\/(?:play|open)\.spotify\.[^\/]+\/user\/([^\/]+)\/playlist\/([^\/\?]+)/);
        }
        if (match != null) {
            var query = 'https://ws.spotify.com/lookup/1/.json?uri=spotify:' + match[1] + ':' + match[2];
            Tomahawk.log("Found album/artist/track, calling " + query);
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                if (match[1] == "artist") {
                    var result = {
                        type: "artist",
                        name: res.artist.name
                    };
                    Tomahawk.addUrlResult(url, result);
                    Tomahawk.log("Reported found artist '" + result.name + "'");
                } else if (match[1] == "album") {
                    var result = {
                        type: "album",
                        name: res.album.name,
                        artist: res.album.artist
                    };
                    Tomahawk.addUrlResult(url, result);
                    Tomahawk.log("Reported found album '" + result.name + "' by '" + result.artist + "'");
                } else if (match[1] == "track") {
                    var result = {
                        type: "track",
                        title: res.track.name,
                        artist: res.track.artists.map(function (item) { return item.name; }).join(" & ")
                    };
                    Tomahawk.addUrlResult(url, result);
                    Tomahawk.log("Reported found track '" + result.title + "' by '" + result.artist + "'");
                }
            });
        } else if (playlistmatch != null) {
            var query = 'http://spotikea.tomahawk-player.org/browse/spotify:user:' + playlistmatch[1]
                        + ':playlist:' + playlistmatch[2];
            Tomahawk.log("Found playlist, calling url: '" + query + "'");
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                var result = {
                    type: "playlist",
                    title: res.playlist.name,
                    guid: "spotify-playlist-" + url,
                    info: "A playlist on Spotify.",
                    creator: res.playlist.creator,
                    url: url,
                    tracks: []
                };
                result.tracks = res.playlist.result.map(function (item) { return { type: "track", title: item.title, artist: item.artist }; });
                Tomahawk.addUrlResult(url, result);
                Tomahawk.log("Reported found playlist '" + result.title + "' containing " + result.tracks.length + " tracks");
            });
        }
    }
});

Tomahawk.resolver.instance = SpotifyResolver;

