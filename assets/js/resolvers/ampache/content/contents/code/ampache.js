/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2011, Dominik Schmidt <domme@tomahawk-player.org>
 *   Copyright 2011, Leo Franchi <lfranchi@kde.org>
 *   Copyright 2013, Teo Mrnjavac <teo@kde.org>
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

var AmpacheResolver = Tomahawk.extend(TomahawkResolver, {
    ready: false,
    artists: {},
    albums: {},
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

    newConfigSaved: function () {
        var userConfig = this.getUserConfig();
        if ((userConfig.username != this.username) || (userConfig.password != this.password)
            || (userConfig.server != this.server)) {
            Tomahawk.log("Saving new Ampache credentials with username:" << userConfig.username);
            window.sessionStorage["ampacheAuth"] = "";
            this.init();
        }
    },

    prepareHandshake: function()
    {
        // prepare handshake arguments
        var time = Tomahawk.timestamp();
        if (typeof CryptoJS !== "undefined" && typeof CryptoJS.SHA256 == "function") {
            var key = CryptoJS.SHA256(this.password).toString(CryptoJS.enc.Hex);
            this.passphrase = CryptoJS.SHA256(time + key).toString(CryptoJS.enc.Hex);
        } else {
            var key = Tomahawk.sha256(this.password);
            this.passphrase = Tomahawk.sha256(time + key);
        }

        // do the handshake
        this.params = {
            timestamp: time,
            version: 350001,
            user: this.username
        }
    },

    applyHandshake: function(xmlDoc)
    {
        var roots = xmlDoc.getElementsByTagName("root");
        Tomahawk.log("Old auth token: " + this.auth);
        this.auth = roots[0] === undefined ? false : Tomahawk.valueForSubNode(roots[0], "auth");
        Tomahawk.log("New auth token: " + this.auth);
        var pingInterval = parseInt(roots[0] === undefined ? 0 : Tomahawk.valueForSubNode(roots[0], "session_length")) * 1000;
        var trackCount = roots[0] === undefined ? (-1) : Tomahawk.valueForSubNode(roots[0], "songs");
        if ( trackCount > -1 )
            this.trackCount = parseInt(trackCount);

        // all fine, set the resolver to ready state
        this.ready = true;
        window.sessionStorage["ampacheAuth"] = this.auth;

        // setup pingTimer
        if (pingInterval) window.setInterval(this.ping, pingInterval - 60);
    },

    configTest: function () {
        var that = this;
        this.prepareHandshake();
        Tomahawk.asyncRequest(this.generateUrl('handshake', this.passphrase, this.params),
            function (xhr) {
                // parse the result
                var domParser = new DOMParser();
                xmlDoc = domParser.parseFromString(xhr.responseText, "text/xml");
                that.applyHandshake(xmlDoc);

                if (!that.auth) {
                    Tomahawk.log("auth failed: " + xhr.responseText);
                    var error = xmlDoc.getElementsByTagName("error")[0];
                    if (typeof error != 'undefined' && error.getAttribute("code") == "403") {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidAccount);
                    } else {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidCredentials);
                    }
                } else {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Success);
                }
            }, {}, {
                errorHandler: function () {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                }
            });
    },

    init: function () {
        // check resolver is properly configured
        var userConfig = this.getUserConfig();
        if (!userConfig.username || !userConfig.password || !userConfig.server) {
            Tomahawk.log("Ampache Resolver not properly configured!");
            return;
        }

        // don't do anything if we already have a valid auth token
        if (window.sessionStorage["ampacheAuth"]) {
            Tomahawk.log("Ampache resolver not using auth token from sessionStorage");
            return window.sessionStorage["ampacheAuth"];
        }

        this.username = userConfig.username;
        this.password = userConfig.password;
        this.server = userConfig.server;
        if (!this.server) {
            this.server = "http://localhost/ampache";
        } else {
            if (this.server.search(".*:\/\/") < 0) {
                // couldn't find a proper protocol, so we default to "http://"
                this.server = "http://" + this.server;
            }
            this.server = this.server.trim();
        }

        this.prepareHandshake();

        try {
            var that = this;
            Tomahawk.asyncRequest(this.generateUrl('handshake', this.passphrase, this.params), function (xhr){
                Tomahawk.log(xhr.responseText);

                // parse the result
                var domParser = new DOMParser();
                xmlDoc = domParser.parseFromString(xhr.responseText, "text/xml");

                that.applyHandshake(xmlDoc);

                // inform the user if something went wrong
                if (!that.auth) {
                    Tomahawk.log("INVALID HANDSHAKE RESPONSE: " + xhr.responseText);
                }

                Tomahawk.log("Ampache Resolver properly initialised!");

                if ( typeof Tomahawk.reportCapabilities == 'function' )
                    Tomahawk.reportCapabilities( TomahawkResolverCapability.Browsable | TomahawkResolverCapability.AccountFactory );
            });
        } catch (e) {
            Tomahawk.log("Caught exception in Ampache resolver doing auth handshake request");
            return;
        }

        this.element = document.createElement('div');
    },

    generateUrl: function (action, auth, params) {
        var ampacheUrl = this.server.replace(/\/$/, "") + "/server/xml.server.php?";
        if (params === undefined) params = [];
        params['action'] = action;
        params['auth'] = auth;

        var first = true;
        for (param in params) {
            if (typeof (params[param]) == 'string') params[param] = params[param].trim();

            if (!first) {
                ampacheUrl += "&";
            } else {
                first = false;
            }
            ampacheUrl += encodeURIComponent(param) + "=" + encodeURIComponent(params[param]);
        }
        return ampacheUrl;
    },

    apiCallSync: function (action, auth, params) { //do not use this because it doesn't do re-auth
        var ampacheUrl = this.generateUrl(action, auth, params);

        return Tomahawk.syncRequest(ampacheUrl);
    },

    apiCall: function (action, auth, params, callback) {
        var ampacheUrl = this.generateUrl(action, auth, params);

        Tomahawk.log("Ampache API call: " + ampacheUrl );

        var that = this;
        Tomahawk.asyncRequest(ampacheUrl, function(xhr) {
            var result = xhr.responseText;
            Tomahawk.log(result);

            var domParser = new DOMParser();
            xmlDoc = domParser.parseFromString(result, "text/xml");

            var error = xmlDoc.getElementsByTagName("error")[0];

            if ( typeof error != 'undefined' &&
                 error.getAttribute("code") == "401" ) //session expired
            {
                Tomahawk.log("Let's reauth!");
                that.prepareHandshake();
                Tomahawk.asyncRequest(that.generateUrl('handshake',that.passphrase,that.params), function(xhr){
                    var hsResponse = xhr.responseText;
                    Tomahawk.log(hsResponse);
                    xmlDoc = domParser.parseFromString(hsResponse, "text/xml");
                    that.applyHandshake(xmlDoc);

                    //reauth done, let's retry the API call
                    ampacheUrl = that.generateUrl(action,that.auth,params);
                    Tomahawk.asyncRequest(ampacheUrl, function(xhr){
                        result = xhr.responseText;
                        Tomahawk.log(result);
                        xmlDoc = domParser.parseFromString(result, "text/xml");
                        callback(xmlDoc);
                    });
                });
            }
            else
                callback(xmlDoc)
        });
    },

    ping: function () {
        // this is called from window scope (setInterval), so we need to make methods and data accessible from there
        Tomahawk.log(AmpacheResolver.apiCall('ping', AmpacheResolver.auth, {}, function () {}));
    },

    decodeEntity : function(str)
    {
        this.element.innerHTML = str;
        return this.element.textContent;
    },

    parseSongResponse: function(xmlDoc) {
        var results = new Array();
        // check the repsonse
        var songElements = xmlDoc.getElementsByTagName("song")[0];
        if (songElements !== undefined && songElements.childNodes.length > 0) {
            var songs = xmlDoc.getElementsByTagName("song");

            // walk through the results and store it in 'results'
            for (var i = 0; i < songs.length; i++) {
                var song = songs[i];

                var result = {
                    artist: this.decodeEntity(Tomahawk.valueForSubNode(song, "artist")),
                    album: this.decodeEntity(Tomahawk.valueForSubNode(song, "album")),
                    track: this.decodeEntity(Tomahawk.valueForSubNode(song, "title")),
                    albumpos: Tomahawk.valueForSubNode(song, "track"),
                    //result.year = 0;//valueForSubNode(song, "year");
                    source: this.settings.name,
                    url: Tomahawk.valueForSubNode(song, "url"),
                    //mimetype: valueForSubNode(song, "mime"), //FIXME what's up here? it was there before :\
                    //result.bitrate = valueForSubNode(song, "title");
                    size: Tomahawk.valueForSubNode(song, "size"),
                    duration: Tomahawk.valueForSubNode(song, "time"),
                    score: Tomahawk.valueForSubNode(song, "rating")
                };

                results.push(result);
            }
        }
        return results;
    },

    parseSearchResponse: function (qid, xmlDoc) {
        var results = this.parseSongResponse(xmlDoc);

        // prepare the return
        var return1 = {
            qid: qid,
            results: results
        };

        Tomahawk.addTrackResults(return1);
        //Tomahawk.dumpResult( return1 );
    },

    resolve: function (qid, artist, album, title) {
        return this.search(qid, title);
    },

    search: function (qid, searchString) {
        if (!this.ready) return {
            qid: qid
        };

        var params = {
            filter: searchString,
            limit: this.settings.limit
        };

        var that = this;
        this.apiCall("search_songs", AmpacheResolver.auth, params, function (xmlDoc) {
            that.parseSearchResponse(qid, xmlDoc);
        });

        //Tomahawk.log( searchResult );
    },

    // ScriptCollection support starts here
    artists: function (qid) {
        var that = this;

        this.artistIds = {};
        this.apiCall("artists", AmpacheResolver.auth, [], function (xmlDoc) {
            var results = [];

            // check the repsonse
            var root = xmlDoc.getElementsByTagName("root")[0];
            if (root !== undefined && root.childNodes.length > 0) {
                var artists = xmlDoc.getElementsByTagName("artist");
                for (var i = 0; i < artists.length; i++) {
                    artistName = Tomahawk.valueForSubNode(artists[i], "name");
                    artistId = artists[i].getAttribute("id");

                    results.push(that.decodeEntity(artistName));
                    that.artistIds[artistName] = artistId;
                }
            }

            var return_artists = {
                qid: qid,
                artists: results
            };
            Tomahawk.log("Ampache artists about to return: " + JSON.stringify( return_artists ));
            Tomahawk.addArtistResults( return_artists );
        } );
    },

    albums: function (qid, artist) {
        var artistId = this.artistIds[artist];
        this.albumIdsForArtist = {};
        var that = this;

        var params = {
            filter: artistId
        };

        this.apiCall("artist_albums", AmpacheResolver.auth, params, function (xmlDoc) {
            var results = [];

            // check the repsonse
            var root = xmlDoc.getElementsByTagName("root")[0];
            if (root !== undefined && root.childNodes.length > 0) {
                var albums = xmlDoc.getElementsByTagName("album");
                for (var i = 0; i < albums.length; i++) {
                    albumName = Tomahawk.valueForSubNode(albums[i], "name");
                    albumId = albums[i].getAttribute("id");

                    results.push(that.decodeEntity(albumName));

                    artistObject = that.albumIdsForArtist[artist];
                    if (artistObject === undefined) artistObject = {};
                    artistObject[albumName] = albumId;
                    that.albumIdsForArtist[artist] = artistObject;
                }
            }

            var return_albums = {
                qid: qid,
                artist: artist,
                albums: results
            };
            Tomahawk.log("Ampache albums about to return: " + JSON.stringify( return_albums ));
            Tomahawk.addAlbumResults( return_albums );
        } );
    },

    tracks: function (qid, artist, album) {
        var artistObject = this.albumIdsForArtist[artist];
        var albumId = artistObject[album];
        var that = this;

        Tomahawk.log("AlbumId for " + artist + " - " + album + ": " + albumId);

        var params = {
            filter: albumId
        };

        this.apiCall("album_songs", AmpacheResolver.auth, params, function (xmlDoc) {
            var tracks_result = that.parseSongResponse(xmlDoc);
            tracks_result.sort( function(a,b) {
                if ( a.albumpos < b.albumpos )
                    return -1;
                else if ( a.albumpos > b.albumpos )
                    return 1;
                else
                    return 0;
            } );

            var return_tracks = {
                qid: qid,
                artist: artist,
                album: album,
                results: tracks_result
            };
            Tomahawk.log("Ampache tracks about to return: " + JSON.stringify( return_tracks ));
            Tomahawk.addAlbumTrackResults( return_tracks );
        } );
    },

    collection: function()
    {
        //strip http:// and trailing slash
        var desc = this.server.replace(/^http:\/\//,"")
                               .replace(/^https:\/\//,"")
                               .replace(/\/$/, "")
                               .replace(/\/remote.php\/ampache/, "");

        var return_object = {
            prettyname: "Ampache",
            description: desc,
            iconfile: "ampache-icon.png"
        };

        if ( typeof( this.trackCount ) !== 'undefined' )
            return_object["trackcount"] = this.trackCount;

        //stupid check if it's an ownCloud instance
        if (this.server.indexOf("/remote.php/ampache") !== -1)
        {
            return_object["prettyname"] = "ownCloud";
            return_object["iconfile"] = "owncloud-icon.png";
        }

        return return_object;
    }
});

Tomahawk.resolver.instance = AmpacheResolver;




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
var resolver = Tomahawk.resolver.instance;
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
