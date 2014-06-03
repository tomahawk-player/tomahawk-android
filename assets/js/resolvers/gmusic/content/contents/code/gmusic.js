/* Google Play Music resolver for Tomahawk.
 *
 * Written in 2013 by Sam Hanes <sam@maltera.com>
 * Extensive modifications in 2014 by Lalit Maganti
 *
 * To the extent possible under law, the author(s) have dedicated all
 * copyright and related and neighboring rights to this software to
 * the public domain worldwide. This software is distributed without
 * any warranty.
 *
 * You should have received a copy of the CC0 Public Domain Dedication
 * along with this software. If not, see:
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

var GMusicResolver = Tomahawk.extend( TomahawkResolver, {
    settings: {
        cacheTime: 300,
        name: 'Google Play Music',
        icon: '../images/icon.png',
        weight: 90,
        timeout: 8
    },

    _version:   '0.1',
    _baseURL:   'https://www.googleapis.com/sj/v1/',
    _webURL:    'https://play.google.com/music/',

    getConfigUi: function() {
        return {
            "widget": Tomahawk.readBase64( "config.ui" ),
            fields: [{
                name: "email",
                widget: "email_edit",
                property: "text"
            }, {
                name: "password",
                widget: "password_edit",
                property: "text"
            }],
            images: [{
                "play-logo.png":
                    Tomahawk.readBase64( "play-logo.png" )
            }]
        };
    },

    newConfigSaved: function() {
        var config = this.getUserConfig();
        if (this._email !== config.email
                || this._password !== config.password)
            this.init();
    },

    init: function() {
        var name = this.settings.name;
        var config = this.getUserConfig();
        this._email = config.email;
        this._password = config.password;

        if (!this._email || !this._password) {
            Tomahawk.reportCapabilities(TomahawkResolverCapability.NullCapability);
            Tomahawk.log( name + " resolver not configured." );
            return;
        }

        // load signing key
        {   var s1 = CryptoJS.enc.Base64.parse(
                    'VzeC4H4h+T2f0VI180nVX8x+Mb5HiTtGnKgH52Otj8ZCGDz9jRW'
                    + 'yHb6QXK0JskSiOgzQfwTY5xgLLSdUSreaLVMsVVWfxfa8Rw=='
                );
            var s2 = CryptoJS.enc.Base64.parse(
                    'ZAPnhUkYwQ6y5DdQxWThbvhJHN8msQ1rqJw0ggKdufQjelrKuiG'
                    + 'GJI30aswkgCWTDyHkTGK9ynlqTkJ5L4CiGGUabGeo8M6JTQ=='
                );

            for (var idx = 0; idx < s1.words.length; idx++)
                s1.words[ idx ] ^= s2.words[ idx ];
            this._key = s1;
        }

        Tomahawk.addCustomUrlHandler( 'gmusic', 'getStreamUrl', true );

        var that = this;
        this._login( function() {
            that._loadWebToken( function() {
                that._loadSettings( function() {
                    that._getData(function (response) {
                        that.trackCount = response.data.items.length;
                        Tomahawk.log("Reporting collection");
                        Tomahawk.reportCapabilities(TomahawkResolverCapability.Browsable);
                    });
                    that._ready = true;
                });
            });
        });
    },

    _convertTrack: function (entry) {
        return {
            artist:     entry.artist,
            album:      entry.album,
            track:      entry.title,
            year:       entry.year,

            albumpos:   entry.trackNumber,
            discnumber: entry.discNumber,

            size:       entry.estimatedSize,
            duration:   entry.durationMillis / 1000,

            source:     "Google Music",
            url:        'gmusic://track/' + entry.id,
            checked:    true
        };
    },

    _convertAlbum: function (entry) {
        return {
            artist:     entry.artist,
            album:      entry.album,
            year:       entry.year
        };
    },

    _convertArtist: function (entry) {
        return entry.artist;
    },

    _getData: function (callback) {
        if (this.hasOwnProperty('cachedRequest') && this.cachedRequest.time + this.settings.cacheTime > Date.now()) {
            callback(this.cachedRequest.response);
        } else {
            var that = this;
            var url =  this._baseURL + 'trackfeed';
            Tomahawk.asyncRequest(url, function (request) {
                that.cachedRequest = {
                    response: JSON.parse( request.responseText ),
                    time: Date.now()
                };
                callback(that.cachedRequest.response);
            }, {
                'Content-type': 'application/x-www-form-urlencoded',
                'Authorization': 'GoogleLogin auth=' + this._token,
            }, {
                method: 'POST'
            });
        }
    },

    _execSearch: function (query, callback, max_results) {

        var that = this;
        this._getData(function (response) {
            var results = { tracks: [], albums: [], artists: [] };
            for (var idx = 0; idx < response.data.items.length; idx++) {
                var entry = response.data.items[ idx ];
                var lowerQuery = query.toLowerCase();
                if (entry.artist.toLowerCase() === lowerQuery
                || entry.album.toLowerCase() === lowerQuery
                || entry.title.toLowerCase() === lowerQuery) {
                    var artist = that._convertArtist(entry);
                    var album = that._convertAlbum(entry);
                    if (!that.containsObject(artist, results.artists)) {
                        results.artists.push(artist);
                    }
                    if (!that.containsObject(album, results.albums)) {
                        results.albums.push(album);
                    }
                    results.tracks.push(that._convertTrack(entry));
                }
            }
            callback.call( window, results );
        });

    },

    search: function (qid, query) {
        if (!this._ready) return;

        this._execSearch( query, function (results) {
            Tomahawk.addTrackResults(
            { 'qid': qid, 'results': results.tracks } );
            Tomahawk.addAlbumResults(
                    { 'qid': qid, 'results': results.albums } );
            Tomahawk.addArtistResults(
                    { 'qid': qid, 'results': results.artists } );
        });
    },

    resolve: function (qid, artist, album, title) {
        if (!this._ready) return;
        var query = title;
        this._execSearch( query, function (results) {
            if (results.tracks.length > 0) {
                Tomahawk.addTrackResults({ 'qid': qid, 'results': [ results.tracks[0] ] } );
            } else {
                // no matches, don't wait for the timeout
                Tomahawk.addTrackResults({ 'qid': qid, 'results': [] });
            }
        }, 1 );
    },

    _parseUrn: function (urn) {
        var match = urn.match( /^gmusic:\/\/([a-z]+)\/(.+)$/ );
        if (!match) return null;

        return {
            type:   match[ 1 ],
            id:     match[ 2 ]
        };
    },

    getStreamUrl: function (qid, urn) {
        if (!this._ready) return;
        Tomahawk.log( "getting stream for '" + urn + "'" );

        urn = this._parseUrn( urn );
        if (!urn || 'track' != urn.type)
            return;

        Tomahawk.log( "track ID is '" + urn.id + "'" );

        // TODO - this is required for the All Access part of Google Music
        /*// generate 13-digit numeric salt
        var salt = '' + Math.floor( Math.random() * 10000000000000 );

        // generate SHA1 HMAC of track ID + salt
        // encoded with URL-safe base64
        var sig = CryptoJS.HmacSHA1( urn.id + salt, this._key )
                .toString( CryptoJS.enc.Base64 )
                .replace( /=+$/, '' )   // no padding
                .replace( /\+/g, '-' )  // URL-safe alphabet
                .replace( /\//g, '_' )  // URL-safe alphabet
            ;*/

        var url = 'https://android.clients.google.com/music/mplay'
                + '?net=wifi&pt=e&targetkbps=8310'
                + '&' + ('T' == urn.id[ 0 ] ? 'mjck' : 'songid')
                    + '=' + urn.id;

        Tomahawk.reportStreamUrl(qid, url, {
            'Content-type': 'application/x-www-form-urlencoded',
            'Authorization': 'GoogleLogin auth=' + this._token,
            'X-Device-ID'  : this._deviceId
        });
    },

    _loadSettings: function (callback) {
        var that = this;
        Tomahawk.asyncRequest(that._webURL
                    + 'services/loadsettings?u=0&xt='
                    + encodeURIComponent( that._xt ),
            function (request) {
                if (200 != request.status) {
                    Tomahawk.log(
                            "settings request failed:\n"
                            + request.status + " "
                            + request.statusText.trim()
                        );
                    return;
                }

                var response = JSON.parse( request.responseText );
                if (!response.settings) {
                    Tomahawk.log( "settings request failed:\n"
                            + request.responseText.trim()
                        );
                    return;
                }

                that._allAccess = response.settings.isSubscription;
                Tomahawk.log( "Google Play Music All Access is "
                        + (that._allAccess ? "enabled" : "disabled" )
                    );

                var device = null;
                var devices = response.settings.devices;
                for (var i = 0; i < devices.length; i++) {
                    var entry = devices[ i ];
                    if ('PHONE' == entry.type) {
                        device = entry;
                        break;
                    }
                }

                if (device) {
                    that._deviceId = device.id.slice( 2 );
                    Tomahawk.log(that._deviceId);

                    Tomahawk.log( that.settings.name
                            + " using device ID from "
                            + device.carrier + " "
                            + device.manufacturer + " "
                            + device.model
                        );

                    callback.call( window );
                } else {
                    Tomahawk.log( that.settings.name
                            + ": there aren't any Android devices"
                            + " associated with your Google account."
                            + " This resolver needs an Android device"
                            + " ID to function. Please open the Google"
                            + " Music application on an Android device"
                            + " and log in to your account."
                        );
                }
            }, {
                'Content-type': 'application/x-www-form-urlencoded',
                'Authorization': 'GoogleLogin auth=' + this._token,
                'Content-Type': 'application/json'
            }, {
                method: 'POST',
                body: JSON.stringify({ 'sessionId': '' })
            }
        );
    },

    _loadWebToken: function (callback) {
        var that = this;
        Tomahawk.asyncRequest(that._webURL + 'listen',
            function (request) {
                if (200 != request.status) {
                    Tomahawk.log( "request for xt cookie failed:"
                            + request.status + " "
                            + request.statusText.trim()
                        );
                    return;
                }

                var match = request.getResponseHeader( 'Set-Cookie' )
                                .match( /^xt=([^;]+)(?:;|$)/m );
                if (match) {
                    that._xt = match[ 1 ];
                    callback.call( window );
                } else {
                    Tomahawk.log( "xt cookie missing" );
                    return;
                }
            }, {
                'Authorization': 'GoogleLogin auth=' + this._token
            }, {
                method: 'HEAD',
                doNativeRequest: true
            }
        );
    },

    /** Called when the login process is completed.
     * @callback loginCB
     */

    /** Asynchronously authenticates with the SkyJam service.
     * Only one login attempt will run at a time. If a login request is
     * already pending the callback (if one is provided) will be queued
     * to run when it is complete.
     *
     * @param {loginCB} [callback] a function to be called on completion
     */
    _login: function (callback) {
        this._token = null;

        // if a login is already in progress just queue the callback
        if (this._loginLock) {
            this._loginCallbacks.push( callback );
            return;
        }

        this._loginLock = true;
        this._loginCallbacks = [ callback ];

        var that = this;
        var name = this.settings.name;
        Tomahawk.asyncRequest('https://www.google.com/accounts/ClientLogin',
            function (request) {
                if (200 == request.status) {
                    that._token = request.responseText
                            .match( /^Auth=(.*)$/m )[ 1 ];
                    that._loginLock = false;

                    Tomahawk.log( name + " logged in successfully" );

                    for (var idx = 0; idx < that._loginCallbacks.length; idx++) {
                        that._loginCallbacks[ idx ].call( window );
                    }
                    that._loginCallbacks = null;
                } else {
                    Tomahawk.log(name + " login failed:\n"
                            + request.status + " "
                            + request.statusText.trim() + "\n"
                            + request.responseText.trim());
                }
            }, {
                'Content-type': 'application/x-www-form-urlencoded'
            }, {
                method: 'POST',
                data: "accountType=HOSTED_OR_GOOGLE&Email=" + that._email.trim()
                    + "&Passwd=" + that._password.trim() + "&service=sj&source=tomahawk-gmusic-"
                    + that._version
            }
        );
    },

    containsObject: function (obj, list) {
        var i;
        for (i = 0; i < list.length; i++) {
            if (list[i] === obj) {
                return true;
            }
        }

        return false;
    },

    // Script Collection Support

    artists: function (qid) {
        this._getData(function (response) {
            var names = response.data.items.map(function (item) {
                return item.artist;
            });
            var unique_names = names.filter(function (item, pos) {
                return names.indexOf(item) == pos;
            });
            Tomahawk.addArtistResults({
                qid: qid,
                artists: unique_names
            });
        });
    },

    albums: function (qid, artist) {
        this._getData(function (response) {
            var names = response.data.items.filter(function (item) {
                return item.artist == artist;
            }).map(function (item) {
                return item.album;
            });
            var unique_names = names.filter(function (item, pos) {
                return names.indexOf(item) == pos;
            });
            Tomahawk.addAlbumResults({
                qid: qid,
                artist: artist,
                albums: unique_names
            });
        });
    },

    tracks: function (qid, artist, album) {
        var that = this;
        this._getData(function (response) {
            var tracks = response.data.items.filter(function (item) {
                return item.artist == artist && item.album == album;
            }).map(function (item) {
                return that._convertTrack(item);
            });
            Tomahawk.addAlbumTrackResults({
                qid: qid,
                artist: artist,
                album: album,
                results: tracks
            });
        });
    },

    collection: function () {
        return {
            prettyname: "Google Music",
            description: this._email,
            iconfile: '../images/icon.png',
            trackcount: this.trackCount
        };
    }
});

Tomahawk.resolver.instance = GMusicResolver;
