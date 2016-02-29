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

var BeetsResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'beets',
        icon: 'beets-icon.png',
        weight: 95,
        timeout: 5
    },

    // Configuration.
    getConfigUi: function () {
        var uiData = Tomahawk.readBase64("config.ui");
        return {
            "widget": uiData,
            "fields": [{
                name: "server",
                widget: "serverField",
                property: "text"
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

    /**
     * Defines this Resolver's config dialog UI.
     */
    configUi: [
        {
            id: "server",
            type: "textfield",
            label: "Server URL",
            defaultValue: "http://localhost:8337/"
        },
        {
            id: "username",
            type: "textfield",
            label: "Username"
        },
        {
            id: "password",
            type: "textfield",
            label: "Password",
            isPassword: true
        }
    ],

    newConfigSaved: function (newConfig) {
        Tomahawk.log("Invalidating cache");
        var that = this;
        beetsCollection.wipe({id: beetsCollection.settings.id}).then(function () {
            window.localStorage.removeItem("beets_trackCount");
            window.localStorage.removeItem("beets_albumCount");
            that.init();
        });
    },

    _sanitizeConfig: function (config) {
        if (!config.server) {
            config.server = "http://localhost:8337/";
        } else {
            if (config.server.search("^.*:\/\/") < 0) {
                // couldn't find a proper protocol, so we default to "http://"
                config.server = "http://" + config.server;
            }

            // qtwebkit doesn't support toString() or href on URLs
            if(URL.prototype.hasOwnProperty('toString')) {
                var url = new URL(config.server);
                if (!url.port) {
                    url.port = 8337;
                }
                config.server = url.toString();
            }
        }

        return config;
    },

    init: function () {
        var config = this._sanitizeConfig(this.getUserConfig());
        this._server = config.server;
        this._username = config.username;
        this._password = config.password;

        this._ensureCollection();
    },

    _ensureCollection: function () {
        var that = this;

        return beetsCollection.revision({
            id: beetsCollection.settings.id
        }).then(function (result) {
            var lastCollectionUpdate = window.localStorage["beets_last_collection_update"];
            if (lastCollectionUpdate && lastCollectionUpdate == result) {
                Tomahawk.log("Collection database has not been changed since last time.");
                return that._fetchAndStoreCollection();
            } else {
                Tomahawk.log("Collection database has been changed. Wiping and re-fetching...");
                window.localStorage.removeItem("beets_trackCount");
                window.localStorage.removeItem("beets_albumCount");
                return beetsCollection.wipe({
                    id: beetsCollection.settings.id
                }).then(function () {
                    return that._fetchAndStoreCollection();
                });
            }
        });
    },

    _fetchAndStoreCollection: function () {
        var that = this;

        var settings;
        if (this._username && this._password) {
            settings = {
                username: this._username,
                password: this._password
            };
        }

        Tomahawk.get(this._server + 'stats', settings).then(function (response) {
            var trackCount = parseInt(response.items);
            var albumCount = parseInt(response.albums);
            Tomahawk.PluginManager.registerPlugin("collection", beetsCollection);
            if (window.localStorage["beets_trackCount"] != trackCount
                || window.localStorage["beets_albumCount"] != albumCount) {
                var msg = "";
                if (window.localStorage["beets_trackCount"] != trackCount) {
                    msg += "Track count has changed from " + window.localStorage["beets_trackCount"]
                        + " to " + trackCount + ". ";
                }
                if (window.localStorage["beets_albumCount"] != albumCount) {
                    msg += "Album count has changed from " + window.localStorage["beets_albumCount"]
                        + " to " + albumCount + ". ";
                }
                Tomahawk.log(msg + "Updating collection ...");
                return Tomahawk.get(that._server + "item", settings).then(function (response) {
                    var searchResults = [];
                    response.items.forEach(function (item) {
                        searchResults.push({
                            artist: item.artist,
                            artistDisambiguation: "",
                            albumArtist: item.artist,
                            albumArtistDisambiguation: "",
                            album: item.album,
                            track: item.title,
                            albumpos: item.track,
                            url: that._server + 'item/' + item.id + '/file',
                            duration: Math.floor(item.length)
                        });
                    });
                    beetsCollection.wipe({id: beetsCollection.settings.id}).then(function () {
                        beetsCollection.addTracks({
                            id: beetsCollection.settings.id,
                            tracks: searchResults
                        }).then(function (newRevision) {
                            window.localStorage["beets_trackCount"] = trackCount;
                            window.localStorage["beets_albumCount"] = albumCount;
                            window.localStorage["beets_last_collection_update"] = newRevision;
                        });
                    });
                });
            } else {
                Tomahawk.log("Track count is still " + trackCount
                    + ". Album count is still " + albumCount
                    + ". No collection update necessary.");
                beetsCollection.addTracks({
                    id: beetsCollection.settings.id,
                    tracks: []
                });
            }
        });

    },

    testConfig: function (config) {
        config = this._sanitizeConfig(config);

        var settings;
        if (config.username && config.password) {
            settings = {
                username: config.username,
                password: config.password
            };
        }

        return Tomahawk.get(config.server + "stats", settings).then(function () {
            return Tomahawk.ConfigTestResultType.Success;
        }, function (xhr) {
            if (xhr.status == 403) {
                return Tomahawk.ConfigTestResultType.InvalidCredentials;
            } else if (xhr.status == 404 || xhr.status == 0) {
                return Tomahawk.ConfigTestResultType.CommunicationError;
            } else {
                return xhr.responseText.trim();
            }
        });
    }
});

Tomahawk.resolver.instance = BeetsResolver;

var beetsCollection = Tomahawk.extend(Tomahawk.Collection, {
    settings: {
        id: "beets",
        prettyname: "Beets",
        description: BeetsResolver.server,
        iconfile: "contents/images/icon.png",
        trackcount: BeetsResolver.trackCount
    }
});