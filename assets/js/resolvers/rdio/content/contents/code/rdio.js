/*
 *   Copyright 2013,      Uwe L. Korn <uwelk@xhochy.com>
 *   Copyright 2014,      Enno Gottschalk <mrmaffen@googlemail.com>
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
 */

var RdioResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'rdio',
        icon: 'rdio.png',
        weight: 95,
        timeout: 15
    },

    clientId: "byjtfyv7k5o7mxjd7xkdfycpnv",

    clientSecret: "79es-w4OOB1aHlKST18rLj",

    redirectUri: "tomahawkrdioresolver://callback",

    storageKeyRefreshToken: "rdio_refresh_token",

    /**
     * Get the access token. Refresh when it is expired.
     */
    getAccessToken: function () {
        return this._getAccessToken(true);
    },

    /**
     * Get the access token. Refresh when it is expired.
     */
    _getAccessToken: function (userAuthRequired) {
        var that = this;
        if (!this.getAccessTokenPromise || new Date().getTime() + 60000 > that.accessTokenExpires) {
            Tomahawk.log("Access token is not valid. We need to get a new one.");
            this.getAccessTokenPromise = new RSVP.Promise(function (resolve, reject) {
                var refreshToken = Tomahawk.localStorage.getItem(that.storageKeyRefreshToken);
                if (!refreshToken && userAuthRequired) {
                    Tomahawk.log("Can't fetch new access token, because there's no stored refresh "
                        + "token. Are you logged in?");
                    reject("Can't fetch new access token, because there's no stored refresh"
                        + " token. Are you logged in?");
                }
                resolve(refreshToken);
            }).then(function (refreshToken) {
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
                            "refresh_token": refreshToken
                        }
                    };
                    if (refreshToken) {
                        settings.data = {
                            "grant_type": "refresh_token",
                            "refresh_token": refreshToken
                        };
                    } else {
                        settings.data = {
                            "grant_type": "client_credentials"
                        };
                    }
                    return Tomahawk.post("https://services.rdio.com/oauth2/token", settings)
                        .then(function (res) {
                            that.accessToken = res.access_token;
                            that.accessTokenExpires =
                                new Date().getTime() + res.expires_in * 1000;
                            Tomahawk.log("Received new access token!");
                            Tomahawk.log(res.access_token);
                            return {
                                accessToken: res.access_token,
                                accessTokenExpires: res.expires_in
                            };
                        });
                });
        }
        return this.getAccessTokenPromise;
    },

    login: function () {
        Tomahawk.log("Starting login");

        var authUrl = "https://www.rdio.com/oauth2/authorize";
        authUrl += "?client_id=" + this._spell(this.clientId);
        authUrl += "&response_type=code";
        authUrl += "&redirect_uri=" + encodeURIComponent(this.redirectUri);

        Tomahawk.showWebView(authUrl);
    },

    logout: function () {
        Tomahawk.localStorage.removeItem(this.storageKeyAccessToken);
        Tomahawk.localStorage.removeItem(this.storageKeyAccessTokenSecret);
        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Logout);
    },

    isLoggedIn: function () {
        var accessToken = Tomahawk.localStorage.getItem(this.storageKeyAccessToken);
        var accessTokenSecret = Tomahawk.localStorage.removeItem(this.storageKeyAccessTokenSecret);
        return accessToken !== null && accessToken.length > 0
            && accessTokenSecret !== null && accessTokenSecret.length > 0;
    },

    /**
     * This function is being called from the native side whenever it has received a redirect
     * callback. In other words, the WebView shown to the user can call the js side here.
     */
    onRedirectCallback: function (params) {
        var url = params.url;

        var error = this._getParameterByName(url, "error");
        if (error) {
            Tomahawk.log("Authorization failed: " + error);
            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other, error);
        } else {
            Tomahawk.log("Authorization successful, fetching new refresh token ...");
            var settings = {
                headers: {
                    "Authorization": "Basic " + Tomahawk.base64Encode(this._spell(this.clientId)
                        + ":" + this._spell(this.clientSecret)),
                    "Content-Type": "application/x-www-form-urlencoded"
                },
                data: {
                    grant_type: "authorization_code",
                    code: this._getParameterByName(url, "code"),
                    redirect_uri: this.redirectUri
                }
            };

            var that = this;
            Tomahawk.post("https://services.rdio.com/oauth2/token", settings)
                .then(function (response) {
                    Tomahawk.localStorage.setItem(that.storageKeyRefreshToken,
                        response.refresh_token);
                    Tomahawk.log("Received new refresh token!");
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Success);
                });
        }
    },

    /**
     * Returns the value of the query parameter with the given name from the given URL.
     */
    _getParameterByName: function (url, name) {
        name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
        var regex = new RegExp("[\\?&#]" + name + "=([^&#]*)"), results = regex.exec(url);
        return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
    },

    init: function () {
        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);
    },

    _spell: function (a) {
        var magic = function (b) {
            return (b ? b : this).split("").map(function (d) {
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

    resolve: function (params) {
        var artist = params.artist;
        var album = params.album;
        var track = params.track;

        var that = this;

        return this.getAccessToken().then(function (response) {
            var settings = {
                data: {
                    method: "search",
                    query: artist + " " + track,
                    types: "track"
                },
                headers: {
                    "Authorization": "Bearer " + response.accessToken
                }
            };
            return Tomahawk.post("https://services.rdio.com/api/1/", settings)
                .then(function (response) {
                    var result = response.result;
                    if (response.status == 'ok') {
                        var results = [];
                        for (var i = 0; i < result.results.length; i++) {
                            if (result.results[i].type == 't' && result.results[i].canStream) {
                                results.push({
                                    source: that.settings.name,
                                    artist: result.results[i].artist,
                                    track: result.results[i].name,
                                    duration: result.results[i].duration,
                                    url: "rdio://track/" + result.results[i].key,
                                    album: result.results[i].album,
                                    linkUrl: result.results[i].url
                                });
                            }
                        }
                        return results;
                    } else {
                        throw new Error('Error in resolve: ' + JSON.stringify(result));
                    }
                }
            );
        });
    },

    search: function (params) {
        var query = params.query;

        return this.resolve({track: query});
    },

    getStreamUrl: function (params) {
        var url = params.url;

        return {
            url: url.replace("rdio://track/", "")
        };
    },

    canParseUrl: function (params) {
        var url = params.url;
        var type = params.type;

        if (!url) {
            throw new Error("Provided url was empty or null!");
        }
        switch (type) {
            case TomahawkUrlType.Album:
                return /https?:\/\/(www\.)?rdio.com\/artist\/([^\/]*)\/album\/([^\/]*)\/?$/.test(url);
            case TomahawkUrlType.Artist:
                return /https?:\/\/(www\.)?rdio.com\/artist\/([^\/]*)\/?$/.test(url);
            case TomahawkUrlType.Playlist:
                return /https?:\/\/(www\.)?rdio.com\/people\/([^\/]*)\/playlists\/(\d+)\//.test(url);
            case TomahawkUrlType.Track:
                return /https?:\/\/(www\.)?rdio.com\/artist\/([^\/]*)\/album\/([^\/]*)\/track\/([^\/]*)\/?$/.test(url);
            default:
                return /https?:\/\/(www\.)?rdio.com\/([^\/]*\/|)/.test(url);
        }
    },

    lookupUrl: function (params) {
        var url = params.url;
        Tomahawk.log("lookupUrl: " + url);

        return this._getAccessToken(false).then(function (response) {
            var settings = {
                data: {
                    method: "getObjectFromUrl",
                    extras: "tracks",
                    url: url
                },
                headers: {
                    "Authorization": "Bearer " + response.accessToken
                }
            };
            return Tomahawk.post("https://services.rdio.com/api/1/", settings)
                .then(function (response) {
                    Tomahawk.log("lookupUrl result: " + JSON.stringify(response));
                    var result = response.result;
                    if (response.status == 'ok') {
                        if (result.type == 'p') {
                            var tracks = result.tracks.map(function (item) {
                                return {
                                    type: Tomahawk.UrlType.Track,
                                    track: item.name,
                                    artist: item.artist
                                };
                            });
                            return {
                                type: Tomahawk.UrlType.Playlist,
                                title: result.name,
                                guid: "rdio-playlist-" + result.key,
                                info: "A playlist by " + result.owner + " on rdio.",
                                creator: result.owner,
                                linkUrl: result.shortUrl,
                                tracks: tracks
                            };
                        } else if (result.type == 't') {
                            return {
                                type: Tomahawk.UrlType.Track,
                                track: result.name,
                                artist: result.artist
                            };
                        } else if (result.type == 'a') {
                            return {
                                type: Tomahawk.UrlType.Album,
                                album: result.name,
                                artist: result.artist
                            };
                        } else if (result.type == 'r') {
                            return {
                                type: Tomahawk.UrlType.Artist,
                                artist: result.name
                            };
                        }
                    } else {
                        throw new Error('Error in lookupUrl: ' + JSON.stringify(result));
                    }
                }
            );
        });
    }
});

Tomahawk.resolver.instance = RdioResolver;

