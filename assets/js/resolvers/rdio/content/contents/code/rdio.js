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

    redirectUri: "tomahawkrdioresolver://callback",

    storageKeyAccessToken: "rdio_access_token",

    storageKeyAccessTokenSecret: "rdio_access_token_secret",

    consumer: [],

    requestToken: [],

    accessToken: [],

    getAccessToken: function () {
        this.accessToken[0] = Tomahawk.localStorage.getItem(this.storageKeyAccessToken);
        this.accessToken[1] = Tomahawk.localStorage.getItem(this.storageKeyAccessTokenSecret);
        if (this.accessToken[0] !== null && this.accessToken[0].length > 0
            && this.accessToken[1] !== null && this.accessToken[1].length > 0) {
            return {
                accessToken: this.accessToken[0],
                accessTokenSecret: this.accessToken[1]
            };
        } else {
            throw new Error("There's no accessToken or accessTokenSecret set.");
        }
    },

    login: function () {
        Tomahawk.log("Starting login");

        var that = this;
        var params = {
            oauth_callback: this.redirectUri
        };
        this._getSignedPostPromise("http://api.rdio.com/oauth/request_token", params).then(
            function (result) {
                that.requestToken[0] = that._getParameterByName("&" + result, "oauth_token");
                that.requestToken[1] = that._getParameterByName("&" + result, "oauth_token_secret");
                var login_url = decodeURIComponent(that._getParameterByName("&" + result,
                    "login_url"));
                login_url += "?oauth_token=" + that.requestToken[0];
                Tomahawk.showWebView(login_url);
            }, function (xhr) {
                Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                Tomahawk.log("error: " + xhr.responseText);
            }
        );
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

        var that = this;
        url = decodeURIComponent(url);
        var oauth_verifier = this._getParameterByName(url, "oauth_verifier");
        if (oauth_verifier == null || oauth_verifier.length === 0) {
            Tomahawk.log("Authorization failed: Permission request rejected by user.");
            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                "Authorization failed: Permission request rejected by user.");
        } else {
            Tomahawk.log("Authorization successful. Fetching access token.");
            var requestParams = {
                oauth_verifier: oauth_verifier
            };
            this._getSignedPostPromise("http://api.rdio.com/oauth/access_token", requestParams)
                .then(function (result) {
                    Tomahawk.log("Got the access token!");
                    that.accessToken[0] = that._getParameterByName("&" + result, "oauth_token");
                    that.accessToken[1] = that._getParameterByName("&" + result,
                        "oauth_token_secret");
                    Tomahawk.localStorage.setItem(that.storageKeyAccessToken, that.accessToken[0]);
                    Tomahawk.localStorage.setItem(that.storageKeyAccessTokenSecret,
                        that.accessToken[1]);
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Success);
                }, function (xhr) {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                    Tomahawk.log("error: " + xhr.responseText);
                }
            );
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
        this.consumer = [this._spell("tdo7m2m8u6r7r76mpod9jqlc"), this._spell("oKeSjHrS9d")];
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
        var requestParams = {
            method: "search",
            query: artist + " " + track,
            types: "Track"
        };
        return that._getSignedPostPromise("http://api.rdio.com/1/", requestParams, that.accessToken)
            .then(function (response) {
                var result = response.result;
                if (response.status == 'ok' && result.results.length !== 0) {
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
    },

    search: function (params) {
        var query = params.query;

        return this.resolve({track: query});
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

        var that = this;
        var requestParams = {
            method: "getObjectFromUrl",
            extras: "tracks",
            url: url
        };
        return that._getSignedPostPromise("http://api.rdio.com/1/", requestParams, that.accessToken)
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
    },

    _getSignedPostPromise: function (url, params, token) {
        params = params || [];
        token = token || this.requestToken;
        var settings = {
            data: params,
            headers: {
                "Authorization": this._getOAuthHeader(this.consumer, url, params, token)
            }
        };
        return Tomahawk.post(url, settings);
    },

    _getOAuthHeader: function (consumer, urlString, params, token, method, realm, timestamp,
        nonce) {
        params = params || [];
        method = (method || "POST").toUpperCase();

        // Coerce params to array of [key, value] pairs.
        if (!Array.isArray(params)) {
            var paramsArray = [];

            for (var key in params) {
                if (params.hasOwnProperty(key)) {
                    paramsArray.push([this._encodeOAuthComponent(key),
                        this._encodeOAuthComponent(params[key])]);
                }
            }

            params = paramsArray;
        }

        // Generate nonce and timestamp if they weren't provided
        if (typeof timestamp == "undefined" || timestamp == null) {
            timestamp = Math.round(new Date().getTime() / 1000).toString();
        }
        if (typeof nonce == "undefined" || nonce == null) {
            nonce = Math.round(Math.random() * 1000000).toString();
        }

        // Add OAuth params.
        params.push(["oauth_version", "1.0"]);
        params.push(["oauth_timestamp", this._encodeOAuthComponent(timestamp)]);
        params.push(["oauth_nonce", this._encodeOAuthComponent(nonce)]);
        params.push(["oauth_signature_method", "HMAC-SHA1"]);
        params.push(["oauth_consumer_key", this._encodeOAuthComponent(consumer[0])]);

        // Calculate the hmac key.
        var hmacKey = this._encodeOAuthComponent(consumer[1]) + "&";

        // If a token was provided, add it to the params and hmac key.
        if (typeof token != "undefined" && token != null && token.length > 0) {
            params.push(["oauth_token", this._encodeOAuthComponent(token[0])]);
            hmacKey += this._encodeOAuthComponent(token[1]);
        }

        // Sort lexicographically, first by key then by value.
        params.sort();

        // Calculate the OAuth signature.
        var paramsString = params.map(function (param) {
            return param[0] + "=" + param[1];
        }).join("&");

        var signatureBase = [
            method,
            this._encodeOAuthComponent(urlString),
            this._encodeOAuthComponent(paramsString)
        ].join("&");

        var oauthSignature = CryptoJS.HmacSHA1(signatureBase,
            hmacKey).toString(CryptoJS.enc.Base64);

        // Build the Authorization header.
        var headerParams = [];

        if (realm) {
            headerParams.push(["realm", this._encodeOAuthComponent(realm)]);
        }

        headerParams.push(["oauth_signature", this._encodeOAuthComponent(oauthSignature)]);

        // Restrict header params to oauth_* subset.
        var oauthParams = ["oauth_version", "oauth_timestamp", "oauth_nonce",
            "oauth_signature_method", "oauth_signature", "oauth_consumer_key",
            "oauth_token"];

        params.forEach(function (param) {
            if (oauthParams.indexOf(param[0]) != -1) {
                headerParams.push(param);
            }
        });
        headerParams.sort();

        return "OAuth " + headerParams.map(function (param) {
                return param[0] + '="' + param[1] + '"';
            }).join(", ");
    },

    _encodeOAuthComponent: function (url) {
        return encodeURIComponent(url)
            .replace(/!/g, "%21")
            .replace(/\*/g, "%2A")
            .replace(/'/g, "%27")
            .replace(/\(/g, "%28")
            .replace(/\)/g, "%29");
    }
});

Tomahawk.resolver.instance = RdioResolver;

