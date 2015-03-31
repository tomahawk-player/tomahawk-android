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

var SubsonicResolver = Tomahawk.extend(TomahawkResolver, {

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
                "subsonic.png" : Tomahawk.readBase64("subsonic.png")
            }]
        };
    },

    newConfigSaved: function () {
        var userConfig = this.getUserConfig();
        Tomahawk.log("newConfigSaved User: " + userConfig.user);

        if (this.user !== userConfig.user ||
            this.password !== userConfig.password ||
            this.subsonic_url !== userConfig.subsonic_url)
        {
            this.init();
        }
    },

    settings:
    {
        name: 'Subsonic',
        icon: 'subsonic-icon.png',
        weight: 70,
        timeout: 8
    },

    encodePassword : function(password)
    {
        var hex_slice;
        var hex_string = "";
        var padding = [ "", "0", "00" ];
        for (pos = 0; pos < password.length; hex_string += hex_slice)
        {
            hex_slice = password.charCodeAt(pos++).toString(16);
            hex_slice = hex_slice.length < 2 ? (padding[2 - hex_slice.length] + hex_slice) : hex_slice;
        }
        return "enc:" + hex_string;
    },

    /**
     * Compares versions strings
     * (version1 < version2) == -1
     * (version1 = version2) == 0
     * (version1 > version2) == 1
     */
    versionCompare: function (version1, version2) {
        var v1 = version1.split('.').map(function (item) {
            return parseInt(item);
        });
        var v2 = version2.split('.').map(function (item) {
            return parseInt(item);
        });
        var length = Math.max(v1.length, v2.length);
        var i = 0;

        for (; i < length; i++) {
            if (typeof v1[i] == "undefined" || v1[i] === null) {
                if (typeof v2[i] == "undefined" || v2[i] === null) {
                    // v1 == v2
                    return 0;
                } else if (v2[i] === 0) {
                    continue;
                } else {
                    // v1 < v2
                    return -1;
                }
            } else if (typeof v2[i] == "undefined" || v2[i] === null) {
                if (v1[i] === 0) {
                    continue;
                } else {
                    // v1 > v2
                    return 1;
                }
            } else if (v2[i] > v1[i]) {
                // v1 < v2
                return -1;
            } else if (v2[i] < v1[i]) {
                // v1 > v2
                return 1;
            }
        }
        // v1 == v2
        return 0;
    },

    init: function()
    {
        var userConfig = this.getUserConfig();
        if (!userConfig.user || !userConfig.password) {
            Tomahawk.log("Subsonic Resolver not properly configured!");
            return;
        }

        this.user = userConfig.user;
        this.user = this.user.trim();
        this.password = this.encodePassword(userConfig.password);
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

        Tomahawk.log("Doing Subsonic resolver init, got credentials from config.  user: "
        + this.user + ", subsonic_url: " + this.subsonic_url);

        this.element = document.createElement('div');

        //let's ask the server which API version it actually supports.
        if (!this.user || !this.password || !this.subsonic_url)
            return;

        var that = this;
        this.subsonic_api = "1.8.0";
        var ping_url = this.buildBaseUrl("/rest/ping.view");
        Tomahawk.asyncRequest(ping_url, function(xhr) {
            var response = JSON.parse(xhr.responseText);
            if (!response || !response["subsonic-response"]
                || !response["subsonic-response"].version)
                return;

            var version = response["subsonic-response"].version;

            if (typeof Tomahawk.reportCapabilities == 'function') {
                // We need at least 1.8.0
                if (that.versionCompare(version, that.subsonic_api) >= 0) {
                    Tomahawk.reportCapabilities(TomahawkResolverCapability.Browsable
                        | TomahawkResolverCapability.AccountFactory);
                }
            }
        } );
    },

    configTest: function () {
        Tomahawk.asyncRequest(this.buildBaseUrl("/rest/ping.view"),
            function (xhr) {
                try {
                    var response = JSON.parse(xhr.responseText);
                } catch (e) {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                }
                if (response && response["subsonic-response"]
                    && response["subsonic-response"].status) {
                    if (response["subsonic-response"].status === "ok") {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Success);
                    } else {
                        if (response["subsonic-response"].error) {
                            if (response["subsonic-response"].error.code === 40) {
                                Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidCredentials);
                            } else if (response["subsonic-response"].error.code === 50) {
                                Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidAccount);
                            } else if (response["subsonic-response"].error.message) {
                                Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                                    response["subsonic-response"].error.message);
                            }
                        } else {
                            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                        }
                    }
                } else {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                }
            }, {}, {
                errorHandler: function (xhr) {
                    if (xhr.status == 404 || xhr.status == 0) {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                    } else {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                            xhr.responseText.trim());
                    }
                }
            }
        );
    },

    buildBaseUrl : function(subsonic_view)
    {
        return this.subsonic_url + subsonic_view +
                "?u=" + this.user +
                "&p=" + this.password +
                "&v=" + this.subsonic_api +
                "&c=tomahawk" +
                "&f=json";
    },

    parseSongFromAttributes : function(song_attributes)
    {
        return {
            artist:     song_attributes["artist"],
            album:      song_attributes["album"],
            track:      song_attributes["title"],
            albumpos:   song_attributes["track"],
            source:     this.settings.name,
            size:       song_attributes["size"],
            duration:   song_attributes["duration"],
            bitrate:    song_attributes["bitRate"],
            url:        this.buildBaseUrl("/rest/stream.view") + "&id=" + song_attributes["id"],
            extension:  song_attributes["suffix"],
            year:       song_attributes["year"]
        };
    },

    executeSearchQuery : function(qid, search_url, isResolve)
    {
        var results = [];
        var that = this; // needed so we can reference this from within the lambda

        // Important to recognize this async request is doing a get and the user / password is passed in the search url
        // TODO: should most likely just use the xhr object and doing basic authentication.
        Tomahawk.asyncRequest(search_url, function(xhr) {
            doc = JSON.parse(xhr.responseText);

            var searchResults;
            if (isResolve){
                searchResults = doc["subsonic-response"].searchResult.match;
            } else {
                searchResults = doc["subsonic-response"].searchResult3.song;
            }
            if (searchResults) {
                Tomahawk.log(searchResults.length + " results returned.");
                for (var i = 0; i < searchResults.length; i++) {
                    results.push(that.parseSongFromAttributes(searchResults[i]));
                }

                var return_songs = {
                    qid: qid,
                    results: results
                };

                Tomahawk.addTrackResults(return_songs);
            }
        });
    },

    executeArtistsQuery : function(qid, artists_url)
    {
        var results = [];
        artists_url += "&f=json"; //for large responses we surely want JSON

        // Important to recognize this async request is doing a get and the user / password is passed in the search url
        // TODO: should most likely just use the xhr object and doing basic authentication.
        Tomahawk.asyncRequest(artists_url, function(xhr) {
            var doc = JSON.parse(xhr.responseText);
            Tomahawk.log("subsonic artists query:" + artists_url);
            Tomahawk.log("subsonic artists response:" + xhr.responseText);
            if (!!doc["subsonic-response"].artists) { // No search results yields empty string in 1.9.0 at least.
                var artists = doc["subsonic-response"].artists.index;

                for (var i = 0; i < artists.length; i++)
                {
                    if ( artists[i].artist instanceof Array )
                    {
                        for (var j = 0; j < artists[i].artist.length; j++)
                        {
                            results.push( artists[i].artist[j].name)
                        }
                    }
                    else
                    {
                        results.push( artists[i].artist.name )
                    }
                }
            }
            var return_artists = {
               qid: qid,
               artists: results
            };

            Tomahawk.log("subsonic artists about to return: " + JSON.stringify( return_artists ) );
            Tomahawk.addArtistResults(return_artists);
        });
    },

    executeAlbumsQuery : function(qid, search_url, artist)
    {
        var results = [];

        // Important to recognize this async request is doing a get and the user / password is passed in the search url
        // TODO: should most likely just use the xhr object and doing basic authentication.
        Tomahawk.asyncRequest(search_url, function(xhr) {
            var doc = JSON.parse(xhr.responseText);
            Tomahawk.log("subsonic albums query:" + search_url);
            Tomahawk.log("subsonic albums response:" + xhr.responseText);
            var albums = doc["subsonic-response"].searchResult3.album;

            if (albums instanceof Array)
            {
                Tomahawk.log(albums.length + " albums returned.")
                for (var i = 0; i < albums.length; i++)
                {
                    if (albums[i].artist.toLowerCase() === artist.toLowerCase()) //search2 does partial matches
                    {
                        results.push(albums[i].name)
                    }
                }
            }
            else
            {
                if (albums.artist.toLowerCase() === artist.toLowerCase())
                {
                    results.push(albums.name);
                }
            }

            var return_albums = {
                qid: qid,
                artist: artist,
                albums: results
            };

            Tomahawk.log("subsonic albums about to return: " + JSON.stringify( return_albums ) );
            Tomahawk.addAlbumResults(return_albums);
        });
    },

    executeTracksQuery : function(qid, search_url, artist, album)
    {
        var results = [];
        var that = this;

        // Important to recognize this async request is doing a get and the user / password is passed in the search url
        // TODO: should most likely just use the xhr object and doing basic authentication.
        Tomahawk.asyncRequest(search_url, function(xhr) {
            var doc = JSON.parse(xhr.responseText);
            Tomahawk.log("subsonic tracks query:" + search_url);
            Tomahawk.log("subsonic tracks response:" + xhr.responseText);
            var tracks = doc["subsonic-response"].searchResult.match;

            if (tracks instanceof Array)
            {
                Tomahawk.log(tracks.length + " tracks returned.")
                for (var i = 0; i < tracks.length; i++ )
                {
                    Tomahawk.log("tracks[i].artist=" + tracks[i].artist);
                    Tomahawk.log("artist=          " + artist);
                    Tomahawk.log("tracks[i].album =" + tracks[i].album);
                    Tomahawk.log("album=           " + album);

                    if (tracks[i].artist && artist
                        && tracks[i].artist.toLowerCase() === artist.toLowerCase()
                        && tracks[i].album && album
                        && tracks[i].album.toLowerCase() === album.toLowerCase()) {
                        results.push(that.parseSongFromAttributes(tracks[i]));
                    }
                }
            } else if (tracks && tracks.artist && artist
                && tracks.artist.toLowerCase() === artist.toLowerCase()
                && tracks.album && album
                && tracks.album.toLowerCase() === album.toLowerCase()) {
                results.push(that.parseSongFromAttributes(tracks));
            }

            var return_tracks = {
                qid: qid,
                artist: artist,
                album: album,
                results: results
            };

            Tomahawk.log("subsonic tracks about to return: " + JSON.stringify( return_tracks ) );
            Tomahawk.addAlbumTrackResults(return_tracks);
        });
    },

    //! Please note i am using the deprecated search method in resolve
    //  The reason i am doing this is because it allows me to get a little more specific with the search
    //  since i have the artist, album and title i want to be as specific as possible
    //  NOTE: I do use the newer search3.view in the search method below and it will populate each result with the
    //  appropriate url.
    resolve: function(qid, artist, album, title)
    {
        if (!this.user || !this.password || !this.subsonic_url)
            return { qid: qid, results: [] };

        var search_url = this.buildBaseUrl("/rest/search.view") + "&artist=" + artist + "&album=" + album + "&title=" + title + "&count=1";
        this.executeSearchQuery(qid, search_url, true);
    },

    search: function( qid, searchString )
    {
        if (!this.user || !this.password || !this.subsonic_url)
            return { qid: qid, results: [] };

        var search_url = this.buildBaseUrl("/rest/search3.view") + "&songCount=100&query=\"" + encodeURIComponent(searchString) + "\"";
        this.executeSearchQuery(qid, search_url, false);
    },

    artists: function( qid )
    {
        if (!this.user || !this.password || !this.subsonic_url)
            return { qid: qid, artists: [] };

        var artists_url = this.buildBaseUrl("/rest/getArtists.view");
        this.executeArtistsQuery(qid, artists_url);
    },

    albums: function( qid, artist )
    {
        if (!this.user || !this.password || !this.subsonic_url)
            return { qid: qid, artist: artist, albums: [] };

        var search_url = this.buildBaseUrl("/rest/search3.view") + "&songCount=0&artistCount=0&albumCount=900" +
                "&query=\"" + encodeURIComponent(artist) + "\"";
        this.executeAlbumsQuery(qid, search_url, artist);
    },

    tracks: function( qid, artist, album )
    {
        if (!this.user || !this.password || !this.subsonic_url)
            return { qid: qid, artist: artist, album: album, tracks: [] };

        // See note for resolve() about the search method
        var search_url = this.buildBaseUrl("/rest/search.view") +
                "&artist=\"" + encodeURIComponent(artist) +
                "\"&album=\"" + encodeURIComponent(album) + "\"&count=200";

        this.executeTracksQuery(qid, search_url, artist, album);
    },

    collection: function()
    {
        var prettyname;
        var iconfile;
        //Icon and text specific for Runners-ID
        if (this.subsonic_url.indexOf("runners-id.com") !== -1 ||
            this.subsonic_url.indexOf("runners-id.org") !== -1) {
            prettyname = "Runners-ID";
            iconfile = "runnersid-icon.png";
        } else {
            prettyname = "Subsonic";
            iconfile = "subsonic-icon.png";
        }

        return {
            prettyname: prettyname,
            description: this.subsonic_url,
            iconfile: iconfile
        };
    }
});

Tomahawk.resolver.instance = SubsonicResolver;
