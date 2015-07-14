/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, mack-t <no_register_no_volatile@ymail.com>
 *   Copyright 2012, Peter Loron <peterl@standingwave.org>
 *   Copyright 2013, Teo Mrnjavac <teo@kde.org>
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

var SubsonicResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'Subsonic',
        icon: 'subsonic-icon.png',
        weight: 70,
        timeout: 8
    },

    _subsonicApiVersion: "1.8.0",

    getConfigUi: function () {
        var uiData = Tomahawk.readBase64("config.ui");
        return {
            "widget": uiData,
            fields: [{
                name: "subsonic_url",
                widget: "subsonic_url_edit",
                property: "text"
            }, {
                name: "user",
                widget: "user_edit",
                property: "text"
            }, {
                name: "password",
                widget: "password_edit",
                property: "text"
            }],
            images: [{
                "subsonic.png": Tomahawk.readBase64("subsonic.png")
            }]
        };
    },

    newConfigSaved: function (newConfig) {
        Tomahawk.log("newConfigSaved User: " + newConfig.user);

        if (this.user !== newConfig.user ||
            this.password !== newConfig.password ||
            this.subsonic_url !== newConfig.subsonic_url) {
            Tomahawk.log("Invalidating cache");
            var that = this;
            subsonicCollection.wipe({id: subsonicCollection.settings.id}).then(function () {
                window.localStorage.removeItem("subsonic_last_cache_update");
                that.init();
            });
        }
    },

    _hexEncode: function (string) {
        var hex_slice;
        var hex_string = "";
        var padding = ["", "0", "00"];
        for (var pos = 0; pos < string.length; hex_string += hex_slice) {
            hex_slice = string.charCodeAt(pos++).toString(16);
            hex_slice = hex_slice.length < 2 ? (padding[2 - hex_slice.length] + hex_slice)
                : hex_slice;
        }
        return "enc:" + hex_string;
    },

    init: function () {
        var userConfig = this.getUserConfig();
        if (!userConfig.user || !userConfig.password) {
            Tomahawk.log("Subsonic Resolver not properly configured!");
            return;
        }

        this.user = userConfig.user;
        this.user = this.user.trim();
        this.password = this._hexEncode(userConfig.password);
        this.subsonic_url = userConfig.subsonic_url || "";
        this.subsonic_url = this.subsonic_url.replace(/\/+$/, "");
        if (!this.subsonic_url) {
            this.subsonic_url = "http://localhost:4040";
        } else {
            if (this.subsonic_url.search(".*:\/\/") < 0) {
                // couldn't find a proper protocol, so we default to "http://"
                this.subsonic_url = "http://" + this.subsonic_url;
            }
            this.subsonic_url = this.subsonic_url.trim();
        }

        Tomahawk.log("Subsonic resolver initalized, got credentials from config. user: "
            + this.user + ", subsonic_url: " + this.subsonic_url);

        this._ensureCollection().then(function () {
            Tomahawk.PluginManager.registerPlugin("collection", subsonicCollection);
        });
    },

    _buildStreamUrl: function (id) {
        return this.subsonic_url + "/rest/stream.view" +
            "?u=" + this.user +
            "&p=" + this.password +
            "&v=" + this._subsonicApiVersion +
            "&c=tomahawk" +
            "&f=json" +
            "&id=" + id;
    },

    _convertTracks: function (results) {
        var tracks = [];
        for (var i = 0; results && i < results.length; i++) {
            var result = results[i];
            if (!result.isDir && !result.isVideo && result.type == "music") {
                tracks.push({
                    artist: result.artist,
                    album: result.album,
                    track: result.title,
                    albumpos: result.track,
                    source: this.settings.name,
                    size: result.size,
                    duration: result.duration,
                    bitrate: result.bitRate,
                    url: this._buildStreamUrl(result.id),
                    extension: result.suffix,
                    year: result.year
                });
            }
        }
        return tracks;
    },

    _ensureCollection: function () {
        var that = this;

        if (!this._requestPromise) {
            Tomahawk.log("Checking if collection needs to be updated");
            var time = Date.now();

            var url = this.subsonic_url + "/rest/getIndexes.view";
            var settings = {
                data: {
                    c: "tomahawk",
                    f: "json",
                    p: this.password,
                    u: this.user,
                    v: this._subsonicApiVersion
                }
            };
            if (window.localStorage["subsonic_last_cache_update"]) {
                settings.data.ifModifiedSince = window.localStorage["subsonic_last_cache_update"];
            }
            this._requestPromise = Tomahawk.get(url, settings).then(function (response) {
                if (response["subsonic-response"].indexes) {
                    Tomahawk.log("Collection needs to be updated");

                    var tracks = that._convertTracks(response["subsonic-response"].indexes.child);
                    subsonicCollection.wipe({
                        id: subsonicCollection.settings.id
                    }).then(function () {
                        subsonicCollection.addTracks({
                            id: subsonicCollection.settings.id,
                            tracks: tracks
                        }).then(function () {
                            Tomahawk.log("Updated cache in " + (Date.now() - time) + "ms");
                            window.localStorage["subsonic_last_cache_update"]
                                = response["subsonic-response"].indexes.lastModified;
                        });
                    });
                } else {
                    Tomahawk.log("Collection doesn't need to be updated");
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
        var url = config.subsonic_url + "/rest/ping.view";
        var settings = {
            data: {
                u: config.user,
                p: config.password,
                v: this._subsonicApiVersion,
                c: "tomahawk",
                f: "json"
            }
        };
        return Tomahawk.get(url, settings).then(function (response) {
                if (response && response["subsonic-response"]
                    && response["subsonic-response"].status) {
                    if (response["subsonic-response"].status === "ok") {
                        return Tomahawk.ConfigTestResultType.Success;
                    } else {
                        if (response["subsonic-response"].error) {
                            if (response["subsonic-response"].error.code === 40) {
                                return Tomahawk.ConfigTestResultType.InvalidCredentials;
                            } else if (response["subsonic-response"].error.code === 50) {
                                return Tomahawk.ConfigTestResultType.InvalidAccount;
                            } else if (response["subsonic-response"].error.message) {
                                return response["subsonic-response"].error.message;
                            }
                        } else {
                            return Tomahawk.ConfigTestResultType.CommunicationError;
                        }
                    }
                } else {
                    return Tomahawk.ConfigTestResultType.CommunicationError;
                }
            }, function (xhr) {
                if (xhr.status == 404 || xhr.status == 0) {
                    return Tomahawk.ConfigTestResultType.CommunicationError;
                } else {
                    return xhr.responseText.trim();
                }
            }
        );
    }

});

Tomahawk.resolver.instance = SubsonicResolver;

var subsonicCollection = Tomahawk.extend(Tomahawk.Collection, {
    settings: {
        id: "subsonic",
        prettyname: "Subsonic",
        description: SubsonicResolver.subsonic_url,
        iconfile: "contents/images/icon.png",
        trackcount: 0
    }
});