/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2011, Dominik Schmidt <domme@tomahawk-player.org>
 *   Copyright 2011, Leo Franchi <lfranchi@kde.org>
 *   Copyright 2013, Teo Mrnjavac <teo@kde.org>
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

var AmpacheResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    _ready: false,

    settings: {
        name: 'Ampache',
        icon: 'ampache-icon.png',
        weight: 85,
        timeout: 5,
        limit: 10
    },

    getConfigUi: function () {
        var uiData = Tomahawk.readBase64("config.ui");
        return {

            "widget": uiData,
            fields: [{
                name: "server",
                widget: "serverLineEdit",
                property: "text"
            }, {
                name: "username",
                widget: "usernameLineEdit",
                property: "text"
            }, {
                name: "password",
                widget: "passwordLineEdit",
                property: "text"
            }],
            images: [{
                "owncloud.png": Tomahawk.readBase64("owncloud.png")
            }, {
                "ampache.png": Tomahawk.readBase64("ampache.png")
            }]
        };
    },

    newConfigSaved: function (newConfig) {
        if ((newConfig.username != this.username) || (newConfig.password != this.password)
            || (newConfig.server != this.server)) {
            Tomahawk.log("Invalidating cache");
            var that = this;
            ampacheCollection.wipe({id: ampacheCollection.settings.id}).then(function () {
                window.localStorage.removeItem("ampache_last_cache_update");
                that.init();
            });
        }
    },

    init: function () {
        var that = this;

        this._ready = false;

        if (!this.element) {
            this.element = document.createElement('div');
        }

        // check resolver is properly configured
        var userConfig = this.getUserConfig();
        if (!userConfig.username || !userConfig.password || !userConfig.server) {
            Tomahawk.log("Ampache Resolver not properly configured!");
            return;
        }

        this._sanitizeConfig(userConfig);
        this.username = userConfig.username;
        this.password = userConfig.password;
        this.server = userConfig.server;

        return this._login(this.username, this.password, this.server).then(function () {
            if (that.auth) {
                that._ensureCollection();
            }
        });
    },

    _ensureCollection: function () {
        var that = this;

        return ampacheCollection.revision({
            id: ampacheCollection.settings.id
        }).then(function (result) {
            var lastCollectionUpdate = window.localStorage["ampache_last_collection_update"];
            if (lastCollectionUpdate && lastCollectionUpdate == result) {
                Tomahawk.log("Collection database has not been changed since last time.");
                var add;
                if (window.localStorage["ampache_last_cache_update"]) {
                    var date = new Date(parseInt(window.localStorage["ampache_last_cache_update"]));
                    add = date.toISOString();
                }
                return that._fetchAndStoreCollection(add);
            } else {
                Tomahawk.log("Collection database has been changed. Wiping and re-fetching...");
                return ampacheCollection.wipe({
                    id: ampacheCollection.settings.id
                }).then(function () {
                    return that._fetchAndStoreCollection();
                });
            }
        });
    },

    _fetchAndStoreCollection: function (add) {
        var that = this;

        if (!this._requestPromise) {
            Tomahawk.log("Checking if collection needs to be updated");
            var time = Date.now();

            var settings = {
                offset: 0,
                limit: 1000000 // EHRM.
            };
            if (add) {
                settings.add = add;
            }
            this._requestPromise = this._apiCall("songs", settings).then(function (xmlDoc) {
                var songs;
                var songElements = xmlDoc.getElementsByTagName("song")[0];
                if (songElements !== undefined && songElements.childNodes.length > 0) {
                    songs = xmlDoc.getElementsByTagName("song");
                }
                Tomahawk.PluginManager.registerPlugin("collection", ampacheCollection);
                if (songs && songs.length > 0) {
                    Tomahawk.log("Collection needs to be updated");

                    var tracks = that._parseSongResponse(xmlDoc);
                    ampacheCollection.addTracks({
                        id: ampacheCollection.settings.id,
                        tracks: tracks
                    }).then(function (newRevision) {
                        Tomahawk.log("Updated cache in " + (Date.now() - time) + "ms");
                        window.localStorage["ampache_last_cache_update"] = Date.now();
                        window.localStorage["ampache_last_collection_update"] = newRevision;
                    });
                } else {
                    Tomahawk.log("Collection doesn't need to be updated");

                    ampacheCollection.addTracks({
                        id: ampacheCollection.settings.id,
                        tracks: []
                    });
                }
            }, function (xhr) {
                Tomahawk.log("Tomahawk.get failed: " + xhr.status + " - "
                    + xhr.statusText + " - " + xhr.responseText);
            }).finally(function () {
                that._requestPromise = undefined;
            });
        }
        return this._requestPromise;
    },

    testConfig: function (config) {
        var that = this;

        this._sanitizeConfig(config);

        return this._login(config.username, config.password, config.server)
            .then(function (response) {
                if (!that.auth) {
                    Tomahawk.log("auth failed!");
                    var error = response.getElementsByTagName("error")[0];
                    if (typeof error != 'undefined' && error.getAttribute("code") == "403") {
                        return TomahawkConfigTestResultType.InvalidAccount;
                    } else {
                        return TomahawkConfigTestResultType.InvalidCredentials;
                    }
                } else {
                    return TomahawkConfigTestResultType.Success;
                }
            }, function () {
                return TomahawkConfigTestResultType.CommunicationError;
            });
    },

    _sanitizeConfig: function (config) {
        if (!config.server) {
            config.server = "http://localhost/ampache";
        } else {
            if (config.server.search(".*:\/\/") < 0) {
                // couldn't find a proper protocol, so we default to "http://"
                config.server = "http://" + config.server;
            }
            config.server = config.server.trim();
        }

        return config;
    },

    _handshake: function (username, password, server) {
        var time = Tomahawk.timestamp();
        var key, passphrase;
        if (typeof CryptoJS !== "undefined" && typeof CryptoJS.SHA256 == "function") {
            key = CryptoJS.SHA256(password).toString(CryptoJS.enc.Hex);
            passphrase = CryptoJS.SHA256(time + key).toString(CryptoJS.enc.Hex);
        } else {
            key = Tomahawk.sha256(password);
            passphrase = Tomahawk.sha256(time + key);
        }

        var params = {};
        params.user = username;
        params.timestamp = time;
        params.version = 350001;
        params.auth = passphrase;

        return this._apiCallBase(server, 'handshake',
            params).then(this._parseHandshakeResult);
    },

    _parseHandshakeResult: function (xmlDoc) {
        var roots = xmlDoc.getElementsByTagName("root");
        var auth = roots[0] === undefined ? false : Tomahawk.valueForSubNode(roots[0], "auth");
        if (!auth) {
            Tomahawk.log("INVALID HANDSHAKE RESPONSE!");
            return xmlDoc;
        }

        Tomahawk.log("New auth token: " + auth);
        var pingInterval = parseInt(roots[0] === undefined ? 0 : Tomahawk.valueForSubNode(roots[0],
                "session_length")) * 1000;
        var trackCount = roots[0] === undefined ? (-1) : Tomahawk.valueForSubNode(roots[0],
            "songs");

        return {
            auth: auth,
            trackCount: trackCount > -1 ? parseInt(trackCount) : trackCount,
            pingInterval: pingInterval
        };
    },

    _login: function (username, password, server) {
        var that = this;
        return this._handshake(username, password, server).then(function (result) {
            that.auth = result.auth;
            that.trackCount = result.trackCount;

            Tomahawk.log("Ampache Resolver properly initialised!");
            that._ready = true;

            // FIXME: the old timer should be cancelled ...
            if (result.pingInterval) {
                window.setInterval(that._ping, result.pingInterval - 60);
            }
            return result;
        });
    },

    _apiCallBase: function (serverUrl, action, params) {
        params = params || {};
        params.action = action;

        var options = {
            url: serverUrl.replace(/\/$/, "") + "/server/xml.server.php",
            data: params
        };

        return Tomahawk.get(options);
    },

    _apiCall: function (action, params) {
        if (!this.auth) {
            throw new Error("Not authed, can't do api call");
        }

        params = params || {};
        params.auth = this.auth;

        var that = this;
        return this._apiCallBase(this.server, action, params).then(function (xmlDoc) {
            var error = xmlDoc.getElementsByTagName("error")[0];
            if (typeof error != 'undefined' && error.getAttribute("code") == "401") //session expired
            {
                Tomahawk.log("Let's reauth for: " + action);
                return that._login(that.username, that.password, that.server).then(function () {
                    return that._apiCallBase(action, params);
                }, function () {
                    throw new Error("Could not renew session.");
                });
            }

            return xmlDoc;
        });
    },

    _ping: function () {
        this._apiCall('ping').then(function () {
            Tomahawk.log('Ping succeeded.');
        }, function () {
            Tomahawk.log('Ping failed.');
        });
    },

    _decodeEntity: function (str) {
        this.element.innerHTML = str;
        return this.element.textContent;
    },

    _parseSongResponse: function (xmlDoc) {
        var results = [];
        // check the response
        var songElements = xmlDoc.getElementsByTagName("song")[0];
        if (songElements !== undefined && songElements.childNodes.length > 0) {
            var songs = xmlDoc.getElementsByTagName("song");

            // walk through the results and store it in 'results'
            for (var i = 0; i < songs.length; i++) {
                var song = songs[i];

                results.push({
                    artist: this._decodeEntity(Tomahawk.valueForSubNode(song, "artist")),
                    album: this._decodeEntity(Tomahawk.valueForSubNode(song, "album")),
                    track: this._decodeEntity(Tomahawk.valueForSubNode(song, "title")),
                    albumpos: Tomahawk.valueForSubNode(song, "track"),
                    //result.year = 0;//valueForSubNode(song, "year");
                    source: this.settings.name,
                    url: Tomahawk.valueForSubNode(song, "url"),
                    //mimetype: valueForSubNode(song, "mime"), //FIXME what's up here? it was there before :\
                    //result.bitrate = valueForSubNode(song, "title");
                    size: Tomahawk.valueForSubNode(song, "size"),
                    duration: Tomahawk.valueForSubNode(song, "time"),
                    score: Tomahawk.valueForSubNode(song, "rating")
                });
            }
        }
        return results;
    },

    resolve: function (params) {
        var artist = params.artist;
        var album = params.album;
        var track = params.track;

        return this.search({query: artist + " " + track});
    },

    search: function (params) {
        var query = params.query;

        if (!this._ready) {
            return;
        }

        params = {
            filter: query,
            limit: this.settings.limit
        };

        var that = this;
        return this._apiCall("search_songs", params).then(function (xmlDoc) {
            return that._parseSongResponse(xmlDoc);
        });
    }

});

Tomahawk.resolver.instance = AmpacheResolver;

var ampacheCollection = Tomahawk.extend(Tomahawk.Collection, {
    settings: {
        id: "ampache",
        prettyname: "Ampache",
        description: AmpacheResolver.getUserConfig()
            ? AmpacheResolver._sanitizeConfig(AmpacheResolver.getUserConfig()).server
            .replace(/^http:\/\//, "")
            .replace(/^https:\/\//, "")
            .replace(/\/$/, "")
            .replace(/\/remote.php\/ampache/, "")
            : "",
        iconfile: "contents/images/icon.png",
        trackcount: AmpacheResolver.trackCount
    }
});

/*
 * TEST ENVIRONMENT
 */

/*TomahawkResolver.getUserConfig = function() {
 return {
 username: "domme",
 password: "foo",
 ampache: "http://owncloud.lo/ampache"
 //ampache: "http://owncloud.lo/apps/media"
 };
 };*/
//
// var resolver = Tomahawk.resolver.instance;
//
//
// // configure tests
// var search = {
//     filter: "I Fell"
// };
//
// var resolve = {
//     artist: "The Aquabats!",
//     title: "I Fell Asleep On My Arm"
// };
// // end configure
//
//
//
//
//tests
//resolver.init();
//
// // test search
// //Tomahawk.log("Search for: " + search.filter );
// var response1 = resolver.search( 1234, search.filter );
// //Tomahawk.dumpResult( response1 );
//
// // test resolve
// //Tomahawk.log("Resolve: " + resolve.artist + " - " + resolve.album + " - " + resolve.title );
// var response2 = resolver.resolve( 1235, resolve.artist, resolve.album, resolve.title );
// //Tomahawk.dumpResult( response2 );
// Tomahawk.log("test");
// n = 0;
// var items = resolver.getArtists( n ).results;
// for(var i=0;i<items.length;i++)
// {
//     artist = items[i];
//     Tomahawk.log("Artist: " + artist);
//     var albums = resolver.getAlbums( ++n, artist ).results;
//     for(var j=0;j<albums.length;j++)
//     {
//         var album = albums[j];
//         Tomahawk.dumpResult( resolver.getTracks( ++n, artist, album ) );
//     }
// }
//
// phantom.exit();
