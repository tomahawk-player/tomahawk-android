/*
 *   Copyright 2014, Uwe L. Korn <uwelk@xhochy.com>
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

    authorization_endpoint: "https://accounts.spotify.com/authorize/",

    getConfigUi: function () {
        var uiData = Tomahawk.readBase64("config.ui");
        return {
            "widget": uiData,
            fields: [{
                name: "user",
                widget: "user_edit",
                property: "text"
            }, {
                name: "password",
                widget: "password_edit",
                property: "text"
            }],
            images: [{
                "spotify.png" : Tomahawk.readBase64("spotify.png")
            }]
        };
    },

    newConfigSaved: function () {
        var userConfig = this.getUserConfig();

        // FIXME
        if (this.user !== userConfig.user || this.password !== userConfig.password) {
            this.init();
        }
    },

    /**
     * Refresh the access token when it is expired.
     */
    refreshToken: function (callback) {
        var that = this;
        var data = "grant_type=refresh_token";
        data += "&refresh_token=" + encodeURIComponent(this.refresh_token);
        var headers = {
            "Authorization": "Basic " + Tomahawk.base64Encode(this.client_id + ":" + this.client_secret),
            "Content-Type": "application/x-www-form-urlencoded"
        };
        Tomahawk.asyncRequest("https://accounts.spotify.com/api/token", function (xhr) {
            var res = JSON.parse(xhr.responseText);
            if (res.hasOwnProperty("access_token")) {
                that.access_token = res.access_token;
                that.access_token_expires = new Date().getTime() + res.expires_in * 1000;
                callback()
            } else {
                callback(res);
            }
        }, headers, {
            method: "POST",
            data: data
        });
    },

    login: function(callback) {
        var that = this;
        var userConfig = this.getUserConfig();
        if (!userConfig.access_token || !userConfig.refresh_token || !userConfig.user || !userConfig.password) {
            Tomahawk.log("Spotify Resolver not properly configured!");
            this.loggedIn = false;
            if (callback) {
                callback("Spotify Resolver not properly configured!");
            }
            return;
        }

        this.access_token = userConfig.access_token;
        this.refresh_token = userConfig.refresh_token;
        this.user = userConfig.user;
        this.password = userConfig.password;
        // TODO: Maybe not from userConfig...
        // Also we do not want the secret in the resolver
        this.client_id = userConfig.client_id;
        this.client_secret = userConfig.client_secret;

        Tomahawk.addCustomUrlHandler("spotify", "getStreamUrl", true);

        if (this.hasOwnProperty("access_token") && this.hasOwnProperty("refresh_token")) {
            // Check if access_token is still valid.
            var headers = {
                "Authorization": "Bearer " + this.access_token
            };
            Tomahawk.asyncRequest("https://api.spotify.com/v1/me", function (xhr) {
                var res = JSON.parse(xhr.responseText);
                if (res.hasOwnProperty("error")) {
                    that.refreshToken(callback);
                } else {
                    callback();
                }
            }, headers);
        } else {
        // var code = "bla";
        // if (typeof code !== "undefined" && code !== null) {
        //     var headers = {
        //         "Content-Type": "application/x-www-form-urlencoded"
        //     };
        //     var data = "grant_type=authorization_code";
        //     data += "&code=" + encodeURIComponent(code);
        //     data += "&redirect_uri=" + encodeURIComponent("http://localhost:8080/spotify");
        //     data += "&client_id=" + this.client_id;
        //     data += "&client_secret=" + this.client_secret;

        //     Tomahawk.asyncRequest("https://accounts.spotify.com/api/token", function (xhr) {
        //         var res = JSON.parse(xhr.responseText);
        //         if (res.hasOwnProperty("error")) {
        //             var errorMsg = "Spotify auth falied: " + res.error;
        //             Tomahawk.log(errorMsg);
        //             if (callback) {
        //                 callback(errorMsg);
        //             }
        //         } else {
        //             Tomahawk.log(res);
        //         }
        //     }, headers, {
        //         method: "POST",
        //         data: data
        //     });
        // } else {
            // This URL does only work in the browser
            var authUrl = this.authorization_endpoint;
            authUrl += "?client_id=" + this.client_id;
            authUrl += "&response_type=code";
            authUrl += "&redirect_uri=" + encodeURIComponent("http://localhost:8080/spotify");
            authUrl += "&scope=playlist-read-private%20streaming%20user-read-private%20user-library-read";

            var errorMsg = "Auth CODE missing, please visit: " + authUrl;
            Tomahawk.log(errorMsg);
            if (callback) {
                callback(errorMsg);
            }
        }
    },

    spell: function(a){magic=function(b){return(b=(b)?b:this).split("").map(function(d){if(!d.match(/[A-Za-z]/)){return d}c=d.charCodeAt(0)>=96;k=(d.toLowerCase().charCodeAt(0)-96+12)%26+1;return String.fromCharCode(k+(c?96:64))}).join("")};return magic(a)},

    init: function(cb) {
        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);

        // re-login every 50 minutes
        setInterval((function(self) { return function() { self.login(); }; })(this), 1000*60*50);

        this.login(cb);
    },

    getStreamUrl: function (qid, url) {
        var trackId = url.replace("spotify://track/", "");
        Tomahawk.reportStreamUrl(qid, trackId);
    },

    resolve: function (qid, artist, album, title) {
        var that = this;
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
            "Authorization": "Bearer " + this.access_token
        });
    },

	search: function (qid, searchString) {
        var that = this;
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
            "Authorization": "Bearer " + this.access_token
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
    },

    setAccessToken : function(accessToken) {
        this.access_token = accessToken;
    }
});

Tomahawk.resolver.instance = SpotifyResolver;

