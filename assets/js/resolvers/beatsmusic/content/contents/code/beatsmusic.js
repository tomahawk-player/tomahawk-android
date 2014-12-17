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

var BeatsMusicResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'Beats Music',
        icon: 'beatsmusic.png',
        weight: 95,
        timeout: 15
    },

    // Production
    endpoint: "https://partner.api.beatsmusic.com/v1",
    redirect_uri: "https://tomahawk-beatslogin.appspot.com/json",

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
                "beatsmusic-wide.png" : Tomahawk.readBase64("beatsmusic-wide.png")
            }]
        };
    },

    newConfigSaved: function () {
        var userConfig = this.getUserConfig();

        if (this.user !== userConfig.user || this.password !== userConfig.password) {
            this.init();
        }
    },


    login: function(callback, doConfigTest) {
        var userConfig = this.getUserConfig();
        if (!userConfig.user || !userConfig.password) {
            Tomahawk.log("Beats Music Resolver not properly configured!");
            this.loggedIn = false;
            if (callback) {
                callback("Beats Music Resolver not properly configured!");
            }
            if (doConfigTest) {
                Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidCredentials);
            }
            return;
        }

        this.user = userConfig.user;
        this.password = userConfig.password;

        var referer = "https://partner.api.beatsmusic.com/oauth2/authorize?response_type=token";
        referer += "&redirect_uri=" + encodeURIComponent(this.redirect_uri);
        referer += "&client_id=" + encodeURIComponent(this.app_token);

        var headers = {
            "Content-Type": "application/x-www-form-urlencoded",
            "Referer": referer
        };

        // Keep empty arguments!
        var data = "login=" + encodeURIComponent(this.user);
        data += "&password=" + encodeURIComponent(this.password);
        data += "&redirect_uri=" + encodeURIComponent(this.redirect_uri);
        data += "&response_type=token&scope=&state=&user_id=";
        data += "&client_id=" + encodeURIComponent(this.app_token);

        var that = this;
        Tomahawk.asyncRequest("https://partner.api.beatsmusic.com/api/o/oauth2/approval", function (xhr) {
            try {
                var res = JSON.parse(xhr.responseText);
                that.accessToken = res.access_token;
                that.loggedIn = true;
                if (callback) {
                    callback();
                }
                if (doConfigTest) {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Success);
                }
            } catch (e) {
                if (doConfigTest) {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidCredentials);
                }
            }
        }, headers, {
            method: "POST",
            data: data,
            errorHandler: function (xhr) {
                if (doConfigTest) {
                    if (xhr.status == 404) {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                    } else {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                            xhr.statusText.trim());
                    }
                }
            }
        });
    },

    configTest: function () {
        this.login(null, true);
    },

    spell: function(a){magic=function(b){return(b=(b)?b:this).split("").map(function(d){if(!d.match(/[A-Za-z]/)){return d}c=d.charCodeAt(0)>=96;k=(d.toLowerCase().charCodeAt(0)-96+12)%26+1;return String.fromCharCode(k+(c?96:64))}).join("")};return magic(a)},

    init: function(cb) {
        this.app_token = this.spell("s4fw8if4jfwxakawi7xud55c");

        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);

        Tomahawk.addCustomUrlHandler("beatsmusic", "getStreamUrl", true);

        // re-login every 50 minutes
        setInterval((function(self) { return function() { self.login(); }; })(this), 1000*60*50);

        this.login(cb);
    },

    apiRequest: function (path, queryArgs, cb) {
        var queryArray = ["client_id=" + this.app_token];
        for (key in queryArgs) {
            queryArray.push(key + "=" + queryArgs[key]);
        }
        var url = this.endpoint + "/api" + path;
        url += "?" + queryArray.join("&");
        Tomahawk.asyncRequest(url, function (xhr) {
            var res = JSON.parse(xhr.responseText);
            if (res.code == "OK") {
                cb(res, xhr);
            }
        });
    },

    resolve: function (qid, artist, album, title) {
        if (!this.loggedIn) return;

        // TODO: Add album to search
        var that = this;
        this.apiRequest("/search", {
            "type": "track",
            "filters": "streamable:true",
            "limit": "1",
            "q": encodeURIComponent(artist + " " + title)
        }, function (res) {
            if (res.info.count > 0) {
                // For the moment we just use the first result
                that.apiRequest("/tracks/" + res.data[0].id, {}, function (res2) {
                    Tomahawk.addTrackResults({
                        qid: qid,
                        results: [{
                            artist: res2.data.artist_display_name,
                            duration: res2.data.duration,
                            source: that.settings.name,
                            track: res2.data.title,
                            url: "beatsmusic://track/" + res.data[0].id
                        }]
                    });
                });
            } else {
                Tomahawk.addTrackResults({ results: [], qid: qid });
            }
        });
    },

    getStreamUrl: function (qid, url) {
        var trackId = url.replace("beatsmusic://track/", "");
        Tomahawk.asyncRequest(this.endpoint + "/api/tracks/" + trackId + "/audio?acquire=1&bitrate=highest&access_token=" + this.accessToken, function (xhr) {
            var res = JSON.parse(xhr.responseText);
            Tomahawk.reportStreamUrl(qid, res.data.location + "/?slist=" + res.data.resource);
        });
    },

	search: function (qid, searchString) {
        var that = this;
        // TODO: Search for albums and artists, too.
        Tomahawk.asyncRequest(this.endpoint +
            "/api/search?type=track&filters=streamable:true&limit=100&q=" +
            encodeURIComponent(searchString) + "&client_id=" + this.app_token, function (xhr) {
            var res = JSON.parse(xhr.responseText);
            if (res.code == "OK" && res.data.length > 0) {
                async.map(res.data, function (item, cb) {
                    var query = that.endpoint + "/api/tracks/" + item.id;
                    query += "?fields=artist_display_name&fields=duration";
                    query += "&fields=title&fields=id&client_id=";
                    query += that.app_token;
                    Tomahawk.asyncRequest(query, function (xhr2) {
                        var res2 = JSON.parse(xhr2.responseText);
                        Tomahawk.log(xhr2.responseText);
                        if (res2.code == "OK") {
                            var result = {
                                artist: res2.data.artist_display_name,
                                bitrate: 320,
                                duration: res2.data.duration,
                                source: that.settings.name,
                                track: res2.data.title,
                                url: "beatsmusic://track/" + res2.data.id
                            };
                            if (res2.data.refs.hasOwnProperty("album")) {
                                result.album = res2.data.refs.album.display;
                            }
                            cb(null, result);
                        } else {
                            Tomahawk.log("Failed to get track metadata: " +
                                JSON.stringify(res2));
                            // Just skip this item instead of raising an error,
                            // so that at least the successful results get
                            // through.
                            cb(null, null);
                        }
                    });
                    // cb(null, item);
                }, function (err, results) {
                    Tomahawk.addTrackResults({
                        results: results.filter(function (item) {
                            return item !== null;
                        }),
                        qid: qid
                    });
                });
            } else {
                Tomahawk.addTrackResults({
                    results: [],
                    qid: qid
                });
            }
        });
	},

    canParseUrl: function (url, type) {
        // We accept all beats.mu shortened urls as we need a HTTP request to get more information.
        if (/https?:\/\/beats.mu\//.test(url)) return true;

        switch (type) {
        case TomahawkUrlType.Album:
            return /https?:\/\/((on|listen)\.)?beatsmusic.com\/albums\/([^\/]*)\/?$/.test(url);
        case TomahawkUrlType.Artist:
            return /https?:\/\/((on|listen)\.)?beatsmusic.com\/artists\/([^\/]*)\/?$/.test(url);
        case TomahawkUrlType.Playlist:
            return this.loggedIn && /https?:\/\/((on|listen)\.)?beatsmusic.com\/playlists\/([^\/]*)\/?$/.test(url);
        case TomahawkUrlType.Track:
            return /https?:\/\/((on|listen)\.)?beatsmusic.com\/albums\/([^\/]*)\/tracks\//.test(url);
        // case TomahawkUrlType.Any:
        default:
            return /https?:\/\/((on|listen)\.)?beatsmusic.com\/([^\/]*\/|)/.test(url);
        }
    },

    lookupUrl: function (url) {
        // Todo: unshorten beats.mu

        if (/https?:\/\/((on|listen)\.)?beatsmusic.com\/albums\/([^\/]*)\/?$/.test(url)) {
            // Found an album URL
            var match = url.match(/https?:\/\/((on|listen)\.)?beatsmusic.com\/albums\/([^\/]*)\/?$/);
            var query = this.endpoint + "/api/albums/" + encodeURIComponent(match[3]) + "?client_id=" + this.app_token;
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                if (res.code == "OK") {
                    Tomahawk.addUrlResult(url, {
                        type: "album",
                        name: res.data.title,
                        artist: res.data.artist_display_name
                    });
                }
            });
        } else if (/https?:\/\/((on|listen)\.)?beatsmusic.com\/artists\/([^\/]*)\/?$/.test(url)) {
            var match = url.match(/https?:\/\/((on|listen)\.)?beatsmusic.com\/artists\/([^\/]*)\/?$/);
            var query = this.endpoint + "/api/artists/" + encodeURIComponent(match[3]) + "?client_id=" + this.app_token;
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                if (res.code == "OK") {
                    Tomahawk.addUrlResult(url, {
                        type: "artist",
                        name: res.data.name
                    });
                }
            });
        } else if (/https?:\/\/((on|listen)\.)?beatsmusic.com\/albums\/([^\/]*)\/tracks\//.test(url)) {
            var match = url.match(/https?:\/\/((on|listen)\.)?beatsmusic.com\/albums\/([^\/]*)\/tracks\/([^\/]*)/);
            var query = this.endpoint + "/api/tracks/" + encodeURIComponent(match[4]) + "?client_id=" + this.app_token;
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                if (res.code == "OK") {
                    Tomahawk.addUrlResult(url, {
                        type: "track",
                        title: res.data.title,
                        artist: res.data.artist_display_name
                    });
                }
            });
        } else if (/https?:\/\/((on|listen)\.)?beatsmusic.com\/playlists\/([^\/]*)\/?$/.test(url)) {
            var match = url.match(/https?:\/\/((on|listen)\.)?beatsmusic.com\/playlists\/([^\/]*)\/?$/);
            var query = this.endpoint + "/api/playlists/" + encodeURIComponent(match[3]) + "?access_token=" + this.accessToken;
            var that = this;
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                if (res.code == "OK") {
                    var result = {
                        type: "playlist",
                        title: res.data.name,
                        guid: "beatsmusic-playlist-" + encodeURIComponent(match[3]),
                        info: res.data.description + " (A playlist by " + res.data.refs.author.display + " on Beats Music)",
                        creator: res.data.refs.author.display,
                        url: url,
                        tracks: []
                    };
                    async.map(res.data.refs.tracks, function (item, cb) {
                        var query2 = that.endpoint + "/api/tracks/" + encodeURIComponent(item.id)  + "?client_id=" + that.app_token;
                        Tomahawk.asyncRequest(query2, function (xhr2) {
                            var res2 = JSON.parse(xhr2.responseText);
                            if (res2.code == "OK") {
                                cb(null, {
                                    type: "track",
                                    title: res2.data.title,
                                    artist: res2.data.artist_display_name
                                });
                            } else {
                                cb(res2.code, null);
                            }
                        });
                    }, function (err, mapresult) {
                        result.tracks = mapresult;
                        Tomahawk.addUrlResult(url, result);
                    });
                }
            });
        }
    }
});

Tomahawk.resolver.instance = BeatsMusicResolver;

