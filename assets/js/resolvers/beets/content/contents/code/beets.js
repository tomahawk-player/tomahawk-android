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

    defaultServer: "http://localhost:8337",

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
                name: "useAuth",
                widget: "useAuthCheckBox",
                property: "checked"
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

    newConfigSaved: function (newConfig) {
        Tomahawk.log("Invalidating cache");
        var that = this;
        beetsCollection.wipe({id: beetsCollection.settings.id}).then(function () {
            window.localStorage.removeItem("beets_trackCount");
            window.localStorage.removeItem("beets_albumCount");
            that.init();
        });
    },

    init: function () {
        var userConfig = this.getUserConfig();
        var that = this;
        this.server = userConfig.server || this.defaultServer;
        this.useAuth = userConfig.useAuth;
        if (this.useAuth) {
            this.username = userConfig.username;
            this.password = userConfig.password;
        } else {
            this.username = null;
            this.password = null;
        }

        var settings;
        if (this.username && this.password) {
            settings = {
                username: this.username,
                password: this.password
            };
        }
        Tomahawk.get(this.server + '/stats', settings).then(function (response) {
            var trackCount = parseInt(response.items);
            var albumCount = parseInt(response.albums);
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
                return Tomahawk.get(that.server + "/item", settings).then(function (response) {
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
                            url: that.server + '/item/' + item.id + '/file',
                            duration: Math.floor(item.length)
                        });
                    });
                    beetsCollection.wipe({id: beetsCollection.settings.id}).then(function () {
                        beetsCollection.addTracks({
                            id: beetsCollection.settings.id,
                            tracks: searchResults
                        }).then(function () {
                            window.localStorage["beets_trackCount"] = trackCount;
                            window.localStorage["beets_albumCount"] = albumCount;
                            Tomahawk.PluginManager.registerPlugin("collection", beetsCollection);
                        });
                    });
                });
            } else {
                Tomahawk.log("Track count is still " + trackCount
                    + ". Album count is still " + albumCount
                    + ". No collection update necessary.");
                Tomahawk.PluginManager.registerPlugin("collection", beetsCollection);
            }
        });
    },

    testConfig: function (config) {
        var settings;
        if (this.username && this.password) {
            settings = {
                username: this.username,
                password: this.password
            };
        }
        Tomahawk.get(this.server + "/stats", settings).then(function () {
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