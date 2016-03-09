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

var DeezerResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'Deezer',
        icon: 'deezer.png',
        weight: 95,
        timeout: 15
    },

    appId: "138751",

    // Deezer requires the redirectUri to be in the domain that has been defined when
    // Tomahawk-Android has been registered on the Deezer Developer website
    redirectUri: "tomahawkdeezerresolver://hatchet.is",

    storageKeyAccessToken: "deezer_access_token",

    storageKeyAccessTokenExpires: "deezer_access_token_expires",

    getAccessToken: function () {
        var that = this;

        var accessToken = Tomahawk.localStorage.getItem(that.storageKeyAccessToken);
        var accessTokenExpires =
            Tomahawk.localStorage.getItem(that.storageKeyAccessTokenExpires);
        if (accessToken !== null && accessToken.length > 0 && accessTokenExpires !== null) {
            return {
                accessToken: accessToken,
                accessTokenExpires: accessTokenExpires
            };
        } else {
            throw  new Error("There's no accessToken set.");
        }
    },

    login: function () {
        Tomahawk.log("Starting login");

        var authUrl = "https://connect.deezer.com/oauth/auth.php";
        authUrl += "?app_id=" + this.appId;
        authUrl += "&redirect_uri=" + encodeURIComponent(this.redirectUri);
        authUrl += "&perms=offline_access";
        authUrl += "&response_type=token";

        var that = this;

        var params = {
            url: authUrl
        };
        return Tomahawk.NativeScriptJobManager.invoke("showWebView", params).then(
            function (result) {
                var error = that._getParameterByName(result.url, "error_reason");
                if (error) {
                    Tomahawk.log("Authorization failed: " + error);
                    return error;
                } else {
                    Tomahawk.log("Authorization successful, received new access token ...");
                    that.accessToken = that._getParameterByName(result.url, "access_token");
                    that.accessTokenExpires = that._getParameterByName(result.url, "expires");
                    Tomahawk.localStorage.setItem(that.storageKeyAccessToken, that.accessToken);
                    Tomahawk.localStorage.setItem(that.storageKeyAccessTokenExpires,
                        that.accessTokenExpires);
                    return TomahawkConfigTestResultType.Success;
                }
            });
    },

    logout: function () {
        Tomahawk.localStorage.removeItem(this.storageKeyAccessToken);
        return TomahawkConfigTestResultType.Logout;
    },

    isLoggedIn: function () {
        var accessToken = Tomahawk.localStorage.getItem(this.storageKeyAccessToken);
        return accessToken !== null && accessToken.length > 0;
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
        Tomahawk.PluginManager.registerPlugin("linkParser", this);

        this.accessToken = Tomahawk.localStorage.getItem(this.storageKeyAccessToken);
        this.accessTokenExpires = Tomahawk.localStorage.getItem(this.storageKeyAccessTokenExpires);
    },

    resolve: function (params) {
        var artist = params.artist;
        var album = params.album;
        var track = params.track;

        var that = this;

        var queryPart;
        if (artist) {
            queryPart = artist + " " + track;
        } else {
            queryPart = track;
        }
        var query = "http://api.deezer.com/search?q=" + encodeURIComponent(queryPart)
            + "&limit=100";
        return Tomahawk.get(query).then(function (response) {
            var results = [];
            for (var i = 0; i < response.data.length; i++) {
                var item = response.data[i];
                if (item.type == 'track' && item.readable) {
                    results.push({
                        source: that.settings.name,
                        artist: item.artist.name,
                        track: item.title,
                        duration: item.duration,
                        url: "deezer://track/" + item.id,
                        album: item.album.title,
                        linkUrl: item.link
                    });
                }
            }
            return results;
        });
    },

    search: function (params) {
        var query = params.query;

        return this.resolve({
            track: query
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
                return /https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)album\//.test(url);
            case TomahawkUrlType.Artist:
                return /https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)artist\//.test(url);
            case TomahawkUrlType.Playlist:
                return /https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)playlist\//.test(url);
            case TomahawkUrlType.Track:
                return /https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)track\//.test(url);
            // case TomahawkUrlType.Any:
            default:
                return /https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)/.test(url);
        }
    },

    lookupUrl: function (params) {
        var url = params.url;
        Tomahawk.log("lookupUrl: " + url);

        var urlParts = url.split('/').filter(function (item) {
            return item.length != 0;
        }).map(decodeURIComponent);

        if (/https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)artist\//.test(url)) {
            // We have to deal with an artist
            var query = 'https://api.deezer.com/2.0/artist/' + urlParts[urlParts.length - 1];
            return Tomahawk.get(query).then(function (response) {
                return {
                    type: Tomahawk.UrlType.Artist,
                    artist: response.name
                };
            });
        } else if (/https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)playlist\//.test(url)) {
            // We have to deal with a playlist.
            var query = 'https://api.deezer.com/2.0/playlist/' + urlParts[urlParts.length - 1];
            return Tomahawk.get(query).then(function (res) {
                var query2 = 'https://api.deezer.com/2.0/playlist/' + res.creator.id;
                return Tomahawk.get(query2).then(function (res2) {
                    return {
                        type: Tomahawk.UrlType.Playlist,
                        title: res.title,
                        guid: "deezer-playlist-" + res.id.toString(),
                        info: "A playlist by " + res2.name + " on Deezer.",
                        creator: res2.name,
                        linkUrl: res.link,
                        tracks: res.tracks.data.map(function (item) {
                            return {
                                type: Tomahawk.UrlType.Track,
                                track: item.title,
                                artist: item.artist.name
                            };
                        })
                    };
                });
            });
        } else if (/https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)track\//.test(url)) {
            // We have to deal with a track.
            var query = 'https://api.deezer.com/2.0/track/' + urlParts[urlParts.length - 1];
            return Tomahawk.get(query).then(function (res) {
                return {
                    type: Tomahawk.UrlType.Track,
                    track: res.title,
                    artist: res.artist.name
                };
            });
        } else if (/https?:\/\/(www\.)?deezer.com\/([^\/]*\/|)album\//.test(url)) {
            // We have to deal with an album.
            var query = 'https://api.deezer.com/2.0/album/' + urlParts[urlParts.length - 1];
            return Tomahawk.get(query).then(function (res) {
                return {
                    type: Tomahawk.UrlType.Album,
                    album: res.title,
                    artist: res.artist.name
                };
            });
        }
    }
});

Tomahawk.resolver.instance = DeezerResolver;

