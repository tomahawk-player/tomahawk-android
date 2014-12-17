/* Google Play Music resolver for Tomahawk.
 *
 * Written in 2013 by Sam Hanes <sam@maltera.com>
 * Extensive modifications in 2014 by Lalit Maganti
 * Further modifications in 2014 by Enno Gottschalk <mrmaffen@googlemail.com>
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

var util = {};
util.Base64 = {
    _map: "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_",
    stringify: CryptoJS.enc.Base64.stringify,
    parse: CryptoJS.enc.Base64.parse
};
util.salt = function(len) {
    return Array.apply(0, Array(len)).map(function() {
        return (function(charset){
            return charset.charAt(Math.floor(Math.random() * charset.length));
        }('abcdefghijklmnopqrstuvwxyz0123456789'));
    }).join('');
};

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
        if (this._email !== config.email || this._password !== config.password) {
            this.invalidateCache();
            this.init();
        }
    },

    invalidateCache: function() {
        Tomahawk.log("Invalidating cache");
        delete this.cachedRequest;
        if (Tomahawk.localStorage)
            Tomahawk.localStorage.removeItem(this.storageKey);
        Tomahawk.deleteFuzzyIndex();
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
                        if (response) {
                            that.trackCount = response.length;
                            Tomahawk.log("Reporting collection with " + that.trackCount
                                + " tracks");
                            Tomahawk.reportCapabilities(TomahawkResolverCapability.Browsable);
                        }
                    });
                    that._ready = true;
                });
            });
        });
    },

    _convertTrack: function (entry) {
        var realId;
        if (entry.id) {
            realId = entry.id;
        } else {
            realId = entry.nid;
        }

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
            url:        'gmusic://track/' + realId,
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

    _waitingCallbacks: [],

    _isRequesting: false,

    _callAllWaitingCallbacks: function () {
        while (this._waitingCallbacks.length > 0) {
            this._waitingCallbacks.splice(0, 1)[0](this.cachedRequest.response);
        }
        this._isRequesting = false;
    },

    storageKey: "gmusic_cached_request",

    _getData: function (callback) {
        var that = this;
        if (!that.cachedRequest) {
            var persistedRequest;
            if (Tomahawk.localStorage)
                persistedRequest = Tomahawk.localStorage.getItem(that.storageKey);
            if (persistedRequest) {
                that.cachedRequest = JSON.parse(persistedRequest);
                that._ensureFuzzyIndex();
            }
        }
        var url = that._baseURL
            + 'trackfeed?fields=nextPageToken,'
            + 'data/items(id,nid,artist,album,title,year,trackNumber,discNumber,estimatedSize,durationMillis)';
        if (that.cachedRequest) {
            url += '&updated-min=' + that.cachedRequest.time * 1000;
        }
        var time = Date.now();
        var results = [];
        that._waitingCallbacks.push(callback);
        if (!that._isRequesting) {
            if (!that.cachedRequest
                || that.cachedRequest.time + that.settings.cacheTime * 1000 > Date.now) {
                Tomahawk.log("Checking cache");
                that._isRequesting = true;
                that._paginatedRequest(results, url, function (results) {
                    if (results && results.length > 0) {
                        Tomahawk.log("Collection needs to be updated");
                        if (that.cachedRequest) {
                            results = that.cachedRequest.response.concat(results);
                        }
                        // Recreate fuzzy index
                        that.cachedRequest = {
                            response: results,
                            time: Date.now()
                        };
                        if (Tomahawk.localStorage)
                            Tomahawk.localStorage.setItem(that.storageKey, JSON.stringify(that.cachedRequest));
                        that._createFuzzyIndex(that.cachedRequest.response);
                        Tomahawk.log("Updated cache in " + (Date.now() - time) + "ms");
                    } else {
                        Tomahawk.log("Collection doesn't need to be updated");
                    }
                    that._callAllWaitingCallbacks();
                }, {method: 'POST'});
            } else {
                that._callAllWaitingCallbacks();
            }
        }
    },

    _ensureFuzzyIndex: function () {
        if (!Tomahawk.hasFuzzyIndex() && this.cachedRequest) {
            this._createFuzzyIndex(this.cachedRequest.response);
        }
    },

    _createFuzzyIndex: function (results) {
        var indexList = [];
        for (var idx = 0; idx < results.length; idx++) {
            var entry = results[ idx ];
            indexList.push({
                id: idx,
                artist: entry.artist,
                album: entry.album,
                track: entry.title
            });
        }
        Tomahawk.log("Creating fuzzy index, count: " + indexList.length);
        Tomahawk.createFuzzyIndex(indexList);
    },

    _paginatedRequest: function (results, url, callback, options) {
        var that = this;
        var extraHeaders = {
            'Content-Type': 'application/json',
            'Authorization': 'GoogleLogin auth=' + that._token
        };
        Tomahawk.asyncRequest(url, function (request) {
            var response = JSON.parse(request.responseText);
            if (response.data) {
                results = results.concat(response.data.items);
                Tomahawk.log("Received chunk of tracks, tracks total: " + results.length);
            }
            if (response.nextPageToken) {
                options = {
                    method: 'POST',
                    data: JSON.stringify({'start-token': response.nextPageToken})
                };
                that._paginatedRequest(results, url, callback, options);
            } else {
                callback(results);
            }
        }, extraHeaders, options);
    },

    _execSearchLocker: function (query, callback, max_results, results) {
        var that = this;
        var time = Date.now();
        this._getData(function (response) {
            if (response) {
                if (!results) {
                    results = { tracks: [], albums: [], artists: [] };
                }

                var resultIds = Tomahawk.searchFuzzyIndex(query);
                for (var idx = 0; idx < resultIds.length; idx++) {
                    var id = resultIds[idx][0];
                    var entry = response[id];
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
            Tomahawk.log("Locker: Searched with query '" + query + "' for " + (Date.now() - time)
                + "ms and found "+ results.tracks.length + " track results");
            callback.call( window, results );
        });
    },

    _execSearchAllAccess: function (query, callback, max_results, results) {
        if (!results) {
            results = { tracks: [], albums: [], artists: [] };
        }
        var that = this;
        var url =  this._baseURL + 'query?q=' + query;
        if (max_results)
            url += '&max-results=' + max_results;

        var time = Date.now();
        Tomahawk.asyncRequest(url, function (request) {
            var response = JSON.parse( request.responseText );

            // entries member is missing when there are no results
            if (!response.entries) {
                callback.call( window, results );
                return;
            }

            for (var idx = 0; idx < response.entries.length; idx++) {
                var entry = response.entries[ idx ];
                switch (entry.type) {
                    case '1':
                        var result = that._convertTrack( entry.track );
                        results.tracks.push( result );
                        break;
                    case '2':
                        var result = that._convertArtist( entry.artist );
                        if (!that.containsObject(result, results.artists)) {
                            results.artists.push( result );
                        }
                        break;
                    case '3':
                        var result = that._convertAlbum( entry.album );
                        if (!that.containsObject(result, results.albums)) {
                            results.albums.push( result );
                        }
                        break;
                }
            }
            Tomahawk.log("All Access: Searched with query '" + query + "' for "
                + (Date.now() - time) + "ms and found " + results.tracks.length + " track results");
            callback.call( window, results );
        }, {
            'Authorization': 'GoogleLogin auth=' + this._token
        }, {
            method: 'GET',
            errorHandler: function (request) {
                Tomahawk.log("Google Music search '" + query + "' failed:\n"
                        + request.status + " "
                        + request.statusText.trim() + "\n"
                        + request.responseText.trim()
                );
            }
        });
    },

    _execSearch: function (query, callback, max_results) {
        var that = this;
        var results = { tracks: [], albums: [], artists: [] };
        this._execSearchLocker( query, function (results) {
            if (that._allAccess) {
                that._execSearchAllAccess( query, function (results) {
                    callback.call( window, results );
                }, max_results, results);
            } else {
                callback.call( window, results );
            }
        }, max_results, results);
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
        }, 20);
    },

    _resolveAllAccess: function(qid, artist, album, title) {
        if (this._allAccess) {
            // Format the search as track-artists-album for now
            var query = artist;
            if (album) {
                query += ' - ' + album;
            }
            query += ' - ' + title;
            this._execSearchAllAccess(query, function (results) {
                if (results.tracks.length > 0) {
                    Tomahawk.addTrackResults({
                        'qid': qid,
                        'results': [
                            results.tracks[0]
                        ]
                    });
                } else {
                    // no matches, don't wait for the timeout
                    Tomahawk.addTrackResults({ 'qid': qid, 'results': [] });
                }
            }, 1);
        } else {
            Tomahawk.addTrackResults({ 'qid': qid, 'results': [] });
        }
    },

    resolve: function (qid, artist, album, title) {
        var that = this;
        if (!this._ready) return;

        // Ensure that the recent data was loaded
        this._getData(function (response) {
            var time = Date.now();
            var resultIds = Tomahawk.resolveFromFuzzyIndex(artist, album, title);
            var resolveTarget = "";
            if (resultIds.length > 0) {
                resolveTarget = "Locker";
                Tomahawk.addTrackResults({
                    'qid': qid,
                    'results': [
                        that._convertTrack(response[resultIds[0][0]])
                    ]
                });
            } else {
                resolveTarget = "All Access";
                that._resolveAllAccess(qid, artist, album, title);
            }
            Tomahawk.log(resolveTarget + ": Resolved track '" + artist + " - " + title + " - "
                + album + "' for " + (Date.now() - time) + "ms and found " + resultIds.length
                + " track results");
        });
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
        if (!this._ready) {
            Tomahawk.log("Failed to get stream for '" + urn + "', resolver wasn't ready");
            return;
        }
        urn = this._parseUrn( urn );
        if (!urn || 'track' != urn.type) {
            Tomahawk.log( "Failed to get stream. Couldn't parse '" + urn + "'" );
            return;
        }
        Tomahawk.log("Getting stream for '" + urn + "', track ID is '" + urn.id + "'");

        var salt = util.salt(13);
        var sig = CryptoJS.HmacSHA1(urn.id + salt, this._key).toString(util.Base64)
                .replace( /=+$/, '' )   // no padding
                .replace( /\+/g, '-' )  // URL-safe alphabet
                .replace( /\//g, '_' )  // URL-safe alphabet
            ;

        var url = 'https://android.clients.google.com/music/mplay'
                + '?net=wifi&pt=e&targetkbps=8310'
                + '&' + ('T' == urn.id[ 0 ] ? 'mjck' : 'songid')
                    + '=' + urn.id + '&slt=' + salt + '&sig=' + sig;

        Tomahawk.reportStreamUrl(qid, url, {
            'Content-type': 'application/x-www-form-urlencoded',
            'Authorization': 'GoogleLogin auth=' + this._token,
            'X-Device-ID'  : this._deviceId
        });
    },

    _loadSettings: function (callback, doConfigTest) {
        var that = this;
        Tomahawk.asyncRequest(that._webURL
                    + 'services/loadsettings?u=0&xt='
                    + encodeURIComponent( that._xt ),
            function (request) {
                var response = JSON.parse( request.responseText );
                if (!response.settings) {
                    Tomahawk.log( "settings request failed:\n"
                            + request.responseText.trim()
                        );
                    if (doConfigTest) {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                            "Wasn't able to get resolver settings");
                    }
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
                    Tomahawk.log(that.settings.name + " using device ID '" + that._deviceId
                        + "' from " + device.carrier + " " + device.manufacturer + " "
                        + device.model);

                    callback.call( window );
                } else {
                    if (doConfigTest) {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                                "No Android devices associated with Google account."
                                + " Please open the 'Play Music' App, log in and play a song");
                    }
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
                'Authorization': 'GoogleLogin auth=' + this._token
            }, {
                method: 'POST',
                errorHandler: function (request) {
                    if (doConfigTest) {
                        if (request.status == 403) {
                            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidAccount);
                        } else {
                            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                        }
                    }
                    Tomahawk.log(
                            "Settings request failed:\n"
                            + request.status + " "
                            + request.statusText.trim()
                    );
                }
            }
        );
    },

    _loadWebToken: function (callback, doConfigTest) {
        var that = this;
        Tomahawk.asyncRequest(that._webURL + 'listen',
            function (request) {
                var match = request.getResponseHeader('Set-Cookie')
                    .match(/^xt=([^;]+)(?:;|$)/m);
                if (match) {
                    that._xt = match[ 1 ];
                    callback.call(window);
                } else {
                    if (doConfigTest) {
                        Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                            "Wasn't able to get xt cookie");
                    }
                    Tomahawk.log("xt cookie missing");
                }
            }, {
                'Authorization': 'GoogleLogin auth=' + this._token
            }, {
                method: 'HEAD',
                needCookieHeader: true,
                errorHandler: function (request) {
                    if (doConfigTest) {
                        if (request.status == 403) {
                            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidAccount);
                        } else {
                            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                        }
                    }
                    Tomahawk.log("Request for xt cookie failed:"
                            + request.status + " "
                            + request.statusText.trim()
                    );
                }
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
     * @param doConfigTest boolean indicating whether or not to call the configTest callbacks
     */
    _login: function (callback, doConfigTest) {
        this._token = null;

        if (!this._loginCallbacks) {
            this._loginCallbacks = [];
        }
        if (callback) {
            this._loginCallbacks.push(callback);
        }
        if (doConfigTest){
            this.doConfigTest = true;
        }
        // if a login is already in progress just queue the callback
        if (this._loginLock) {
            return;
        }

        this._loginLock = true;

        var that = this;
        var name = this.settings.name;
        Tomahawk.asyncRequest('https://www.google.com/accounts/ClientLogin',
            function (request) {
                that._token = request.responseText
                    .match(/^Auth=(.*)$/m)[ 1 ];

                Tomahawk.log(name + " logged in successfully");

                for (var idx = 0; idx < that._loginCallbacks.length; idx++) {
                    that._loginCallbacks[ idx ].call(window);
                }
                that._loginCallbacks = null;
                that._loginLock = false;
            }, {
                'Content-type': 'application/x-www-form-urlencoded'
            }, {
                method: 'POST',
                data: "accountType=HOSTED_OR_GOOGLE&Email=" + that._email.trim()
                    + "&Passwd=" + that._password.trim() + "&service=sj&source=tomahawk-gmusic-"
                    + that._version,
                errorHandler: function (request) {
                    Tomahawk.log(name + " login failed:\n"
                        + request.status + " "
                        + request.statusText.trim() + "\n"
                        + request.responseText.trim());
                    for (var idx = 0; idx < that._loginCallbacks.length; idx++) {
                        that._loginCallbacks[ idx ].call(window);
                    }
                    if (that.doConfigTest) {
                        that.doConfigTest = false;
                        if (request.status == 403) {
                            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.InvalidCredentials);
                        } else if (request.status == 404) {
                            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.CommunicationError);
                        } else {
                            Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Other,
                                request.responseText.trim());
                        }
                    }
                    that._loginCallbacks = null;
                    that._loginLock = false;
                }
            }
        );
    },

    configTest: function () {
        var that = this;
        this._login(function () {
            that._loadWebToken(function () {
                that._loadSettings(function () {
                    Tomahawk.onConfigTestResult(TomahawkConfigTestResultType.Success);
                }, true);
            }, true);
        }, true);
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
            var names = response.map(function (item) {
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
            var names = response.filter(function (item) {
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
            var tracks = response.filter(function (item) {
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
