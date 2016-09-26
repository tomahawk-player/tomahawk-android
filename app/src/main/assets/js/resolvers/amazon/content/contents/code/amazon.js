/* Amazon Music resolver for Tomahawk.
 *
 * Written in 2015 by Creepy Guy In The Corner
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

var AmazonResolver = Tomahawk.extend( Tomahawk.Resolver, {
    apiVersion: 0.9,

    api_location : 'https://www.amazon.com/',

    logged_in: null, // null, = not yet tried, 0 = pending, 1 = success, 2 = failed

    settings: {
        cacheTime: 300,
        name: 'Amazon Prime Music',
        icon: '../images/icon.png',
        weight: 91,
        timeout: 8,
        user_agent: 'Mozilla/6.0 (X11; Ubuntu; Linux x86_64; rv:40.0) Gecko/20100101 Firefox/40.0'
    },

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
            },{
                name: "region",
                widget: "region",
                property: "currentIndex"
            }]
        };
    },

    /**
     * Defines this Resolver's config dialog UI.
     */
    configUi: [
        {
            id: "email",
            type: "textfield",
            label: "E-Mail"
        },
        {
            id: "password",
            type: "textfield",
            label: "Password",
            isPassword: true
        },
        {
            id: "region",
            type: "dropdown",
            label: "Region",
            items: [".com", ".de", ".co.uk"],
            defaultValue: 0
        }
    ],

    newConfigSaved: function(config) {
        var that = this;
        var changed =
            this._email !== config.email ||
            this._password !== config.password ||
            this._region != config.region;

        if (changed) {
            return this._get(this.api_location + "gp/dmusic/cloudplayer/forceSignOut").then(function(resp){
                that.init();
                amazonCollection.wipe({id: amazonCollection.settings.id}).then(function () {
                    window.localStorage.removeItem("amzn_collection_version_key");
                    that.init();
                });
            });
        }
    },

    testConfig: function (config) {
        var that = this;
        return that._get(that.api_location + "gp/dmusic/cloudplayer/forceSignOut").then(
            function () {
                return that._getLoginPromise(config, true).then(function (resp) {
                    var appConfigRe = /amznMusic.appConfig *?= *?({.*});/g;
                    if (appConfigRe.exec(resp) != null) {
                        return Tomahawk.ConfigTestResultType.Success;
                    } else {
                        return Tomahawk.ConfigTestResultType.InvalidCredentials;
                    }
                }, function (error) {
                    return "Internal error.";
                });
            });
    },

    _request: function(url, method, options, use_csrf_headers){
        if (typeof options === 'undefined')
            options = {};
        if (!options.hasOwnProperty('headers'))
            options.headers = {};
        if (use_csrf_headers) {
            options.headers['csrf-token'] = this._appConfig['CSRFTokenConfig']['csrf_token'];
            options.headers['csrf-rnd'] = this._appConfig['CSRFTokenConfig']['csrf_rnd'];
            options.headers['csrf-ts'] = this._appConfig['CSRFTokenConfig']['csrf_ts'];
        }

        options.headers['User-Agent'] = 'Mozilla/5.0 (X11; Linux x86_64; rv:48.0) Gecko/20100101 Firefox/48.0';
        options.headers['Accept-Language'] = 'en-US,en;q=0.5';

        if (method == 'POST')
            return Tomahawk.post( url, options);
        else
            return Tomahawk.get( url, options);
    },
    _post: function (url, options, use_csrf_headers) {
        return this._request(url, 'POST', options, use_csrf_headers);
    },
    _get: function (url, options, use_csrf_headers) {
        return this._request(url, 'GET', options, use_csrf_headers);
    },

    _domains : ['.com', '.de', '.co.uk'],

    init: function() {
        var config = this.getUserConfig();

        this._email = config.email;
        this._password = config.password;
        this._region = config.region || 0;

        if (!this._email || !this._password) {
            Tomahawk.PluginManager.unregisterPlugin("collection", amazonCollection);
            Tomahawk.log("Invalid configuration.");
            return;
        }


        this.api_location = 'https://www.amazon' + this._domains[this._region] + '/';
        var that = this;

        return this._get(this.api_location + "gp/dmusic/cloudplayer/forceSignOut").then(function(resp){
            return that._login(config);
        });
    },

    _convertTrack: function (entry) {
        if (entry.hasOwnProperty('metadata'))
            entry = entry.metadata;
        var track = {
            artist:     entry.artistName,
            album:      entry.albumName,
            track:      entry.title,
            title:      entry.title,

            albumpos:   entry.trackNum,
            discnumber: entry.discNum,

            duration:   entry.duration,

            checked:    true,
            size:       entry.size,
            bitrate:    entry.bitrate,
            type:       "track"
        };

        if(entry.albumReleaseDate)
        {
            track['releaseyear'] = entry.albumReleaseDate.split('-')[0];
            track['year'] = entry.albumReleaseDate.split('-')[0];
        }

        if (entry.purchased === 'true' || entry.uploaded === 'true')
            track.url = 'amzn://track/' + entry.duration + '/COID/' + entry.objectId;
        else
            track.url = 'amzn://track/' + entry.duration + '/ASIN/' + entry.asin;

        if (entry.hasOwnProperty('bitrate'))
            track.bitrate = entry.bitrate;

        track.hint = track.url;
        return track;
    },

    search: function (params) {
        if (!this.logged_in) {
            return this._defer(this.search, params, this);
        } else if (this.logged_in === 2) {
            throw new Error('Failed login, cannot search.');
        }

        var that = this;


        //Just a guess, not sure how to check if haz prime music
        if (that._appConfig['featureController']['primePlatformMS3'] == 1)
        {
            //I have no idea where this URL comes from yet
            //This is to search 'Prime Music'
            return that._post(that.api_location + "clientbuddy/compartments/32f93572142e8f7c/handlers/search", {
                data: {
                    "keywords" : params.query,
                    "marketplaceId" : that._appConfig['cirrus']['marketplaceId']
                },
                dataFormat: 'json'
            }, true).then( function (response) {
                return response.tracks.filter(function(track) {
                    return track.isPrime;
                }).map(that._convertTrack, that);
            });
        } else {
            return [];
        }
    },

    resolve: function (params) {
        var query = [ params.artist, params.album, params.track ].join(' ');

        return this.search({query:query});
    },

    _debugPrint: function (obj, spaces) {
        spaces = spaces || '';

        var str = '';
        for (var key in obj) {
            if (typeof obj[key] === "object") {
                var b = ["{", "}"];
                if (obj[key].constructor == Array) {
                    b = ["[", "]"];
                }
                str += spaces+key+": "+b[0]+"\n"+this._debugPrint(obj[key], spaces+'    ')+"\n"+spaces+b[1]+'\n';
            } else {
                str += spaces+key+": "+obj[key]+"\n";
            }
        }
        if (spaces != '') {
            return str;
        } else {
            str.split('\n').map(Tomahawk.log, Tomahawk);
        }
    },

    getStreamUrl: function(params) {
        return this._getStreamUrlPromise(params.url).then(function (streamUrl){
            return {url: streamUrl};
        }).catch(Tomahawk.log);
    },

    _parseUrn: function (urn) {
        //amzn://track/' + entry.duration + '/ASIN/' + entry.asin
        var match = urn.match( /^amzn:\/\/([a-z]+)\/([0-9]+)\/(ASIN|COID)\/(.+)$/ );
        if (!match) return null;

        return {
            type:    match[ 1 ],
            duraion: match[ 2 ],
            idType:  match[ 3 ],
            id:      match[ 4 ]
        };
    },

    _getStreamUrlPromise: function (urn) {
        if (!this.logged_in) {
            return this._defer(this.getStreamUrl, [urn], this);
        } else if (this.logged_in === 2) {
            throw new Error('Failed login, cannot getStreamUrl.');
        }

        var parsedUrn = this._parseUrn( urn );

        if (!parsedUrn || parsedUrn.type != 'track') {
            Tomahawk.log( "Failed to get stream. Couldn't parse '" + urn + "'" );
            return;
        }


        var _headers = {
            'X-Amz-Target' : 'com.amazon.digitalmusiclocator.DigitalMusicLocatorServiceExternal.getRestrictedStreamingURL',
            'Content-Encoding': 'amz-1.0'
        };

        var request = {
            "appMetadata": {
                "https": "true"
            },
            "bitRate": "HIGH",
            "clientMetadata": {
                "clientId": "WebCP"
            },
            "contentDuration": parsedUrn.duration,
            "contentId": {
                "identifier": parsedUrn.id,
                "identifierType": parsedUrn.idType
            },
            "customerId": this._appConfig['customerId'],
            "deviceToken": {
                "deviceId": this._appConfig['deviceId'],
                "deviceTypeId": this._appConfig['deviceType']
            }
        };


        return this._post(this.api_location + "dmls/", {
            data: request,
            dataFormat: 'json',
            headers: _headers
        }, true).then( function (response) {
            Tomahawk.log(JSON.stringify(response));
            if (! response.contentResponse.urlList)
                throw new Error( response.contentResponse.statusMessage );
            return response.contentResponse.urlList[0];
        });
    },

    _defer: function (callback, args, scope) {
        if (typeof this._loginPromise !== 'undefined' && 'then' in this._loginPromise) {
            args = args || [];
            scope = scope || this;
            Tomahawk.log('Deferring action with ' + args.length + ' arguments.');
            return this._loginPromise.then(function () {
                Tomahawk.log('Performing deferred action with ' + args.length + ' arguments.');
                callback.call(scope, args);
            });
        }
    },

    _getLoginPromise: function (config, isTestingConfig) {
        var that = this;
        var options = {
            isTestingConfig: isTestingConfig
        };
        return this._get(this.api_location + "cloudplayer", options).then(
            function (resp) {
                if (resp.indexOf('amznMusic.appConfig') !== -1 )
                {
                    //We are already logged in
                    return resp;
                }
                else
                {
                    var myRE = /input type=.*? name="([^"]+)" value="([^"]+)"/g;

                    var match = myRE.exec(resp);
                    var params = {};
                    while (match != null) {
                        params[match[1]] = match[2];
                        match = myRE.exec(resp);
                    }
                    params['email'] = config.email.trim();
                    params['password'] = config.password.trim();
                    params['create'] = '0';
                    var actionRe = /action="([^"]+)"/g ;
                    var url = actionRe.exec(resp)[1];
                    var tokenRE = /token...([A-F0-9]+)/g ;
                    var token = tokenRE.exec(resp)[1];
                    var options = {
                        data: params,
                        headers : { 'Referer' : 'https://www.amazon' + that._domains[that._region] + '/ap/signin?_encoding=UTF8&accountStatusPolicy=P1&openid.assoc_handle=usflex&openid.claimed_id=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.identity=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0%2Fidentifier_select&openid.mode=checkid_setup&openid.ns=http%3A%2F%2Fspecs.openid.net%2Fauth%2F2.0&openid.ns.pape=http%3A%2F%2Fspecs.openid.net%2Fextensions%2Fpape%2F1.0&openid.pape.max_auth_age=0&openid.return_to=https%3A%2F%2Fwww.amazon' + that._domains[that._region] + '%3A443%2Fgp%2Fredirect.html%3F_encoding%3DUTF8%26location%3Dhttps%253A%252F%252Fmusic.amazon' + that._domains[that._region] + '%253Fref_%253Ddm_wcp_sfso%26source%3Dstandards%26token%3D' + token + '%23&pageId=amzn_cpweb&showRmrMe=1' }
                    };
                    if (isTestingConfig) {
                        options.isTestingConfig = true;
                    }
                    return that._post(url, options);
                }
            },
            function (error) {
                Tomahawk.log(JSON.stringify(error));
                Tomahawk.log('Error getting login page');
                that.logged_in = 2;
            }
        );
    },

    _login: function (config) {
        // If a login is already in progress don't start another!
        if (this.logged_in === 0) return;
        this.logged_in = 0;

        var that = this;

        this._loginPromise = this._getLoginPromise(config, false).then(
            function (resp) {
                var appConfigRe = /amznMusic.appConfig *?= *?({.*});/g;
                that._appConfig = JSON.parse(appConfigRe.exec(resp)[1]);
                that.logged_in = 1;
                that.api_location = 'https://' + that._appConfig['serverName'] + '/';
                amazonCollection.settings['description'] = that._appConfig['customerName'];
                Tomahawk.PluginManager.registerPlugin("collection", amazonCollection);
                that._checkForLibraryUpdates().then(function(){
                    amazonCollection.addTracks({
                        id: amazonCollection.settings.id,
                        tracks: []
                    });
                }, function (error) {
                    Tomahawk.PluginManager.unregisterPlugin("collection", amazonCollection);
                    Tomahawk.log("Failed updating Library:" + error);
                });
                Tomahawk.log(that.settings.name + " successfully logged in.");
            },
            function (error) {
                Tomahawk.log(that.settings.name + " failed login: " + JSON.stringify(error));

                delete that._appConfig;

                that.logged_in = 2;
            }
        ).catch(function(error) {
            Tomahawk.log(that.settings.name + " failed login: " + JSON.stringify(error));
            that.logged_in = 2;
        });
        return this._loginPromise;
    },
    //Collection/Library
    //
    _checkForLibraryUpdates: function() {
        var that = this;
        //Check for library updates every 15 Minutes
        setTimeout(function(){that._checkForLibraryUpdates()}, 1000 * 60 * 15);
        var currentVersion = Tomahawk.localStorage.getItem("amzn_collection_version_key");
        var _query = {
            'caller' : 'checkServerChange',
            'Operation' : 'getGlobalLastUpdatedDate',
            'ContentType' : 'JSON',
            'customerInfo.customerId' : this._appConfig['customerId'],
            'customerInfo.deviceId' :   this._appConfig['deviceId'],
            'customerInfo.deviceType' : this._appConfig['deviceType']
        };
        return this._post(this.api_location + 'cirrus/', {
            data: _query
        }, true).then( function (response) {
            var serverVersion = response.getGlobalLastUpdatedDateResponse.getGlobalLastUpdatedDateResult.date.toString();
            if( currentVersion != serverVersion ) {
                Tomahawk.log('Server-side library updated, syncing');
                amazonCollection.wipe({id: amazonCollection.settings.id}).then(function () {
                    Tomahawk.localStorage.removeItem("amzn_collection_version_key");
                    that._getLibraryTracks().then(function(tracks) {
                        amazonCollection.addTracks({
                            id: amazonCollection.settings.id,
                            tracks: tracks
                        }).then(function () {
                            Tomahawk.log("Updated Library to version with date:" + serverVersion);
                            Tomahawk.localStorage.setItem("amzn_collection_version_key", serverVersion);
                        });
                    });
                });
            } else {
                Tomahawk.log('Library up-to-date');
            }
        });
    },

    _getLibraryTracks: function(previousResults, nextResultsToken) {
        var that = this;
        if (!previousResults)
            previousResults = [];
        if (!nextResultsToken)
            nextResultsToken = '';
        var _query = {
            'ContentType' : 'JSON',
            'Operation' : 'searchLibrary',
            'albumArtUrlsRedirects' : 'false',
            'caller' : 'getServerSongs',
            'countOnly': 'false',
            'customerInfo.customerId' : this._appConfig['customerId'],
            'customerInfo.deviceId' :   this._appConfig['deviceId'],
            'customerInfo.deviceType' : this._appConfig['deviceType'],
            'distinctOnly' : 'false',
            'maxResults' : '1000',
            'nextResultsToken' : nextResultsToken,
            'searchCriteria.member.1.attributeName' : 'keywords',
            'searchCriteria.member.1.attributeValue' : '',
            'searchCriteria.member.1.comparisonType' : 'LIKE',
            'searchCriteria.member.2.attributeName' : 'assetType',
            'searchCriteria.member.2.attributeValue' : 'AUDIO',
            'searchCriteria.member.2.comparisonType' : 'EQUALS',
            'searchCriteria.member.3.attributeName' : 'status',
            'searchCriteria.member.3.attributeValue' : 'AVAILABLE',
            'searchCriteria.member.3.comparisonType' : 'EQUALS',
            'searchReturnType' : 'TRACKS',
            'selectedColumns.member.1' : 'albumArtistName',
            'selectedColumns.member.10' : 'title',
            'selectedColumns.member.11' : 'status',
            'selectedColumns.member.12' : 'trackStatus',
            'selectedColumns.member.13' : 'extension',
            'selectedColumns.member.14' : 'asin',
            'selectedColumns.member.15' : 'primeStatus',
            'selectedColumns.member.2' : 'albumName',
            'selectedColumns.member.3' : 'artistName',
            'selectedColumns.member.4' : 'assetType',
            'selectedColumns.member.5' : 'duration',
            'selectedColumns.member.6' : 'objectId',
            'selectedColumns.member.7' : 'sortAlbumArtistName',
            'selectedColumns.member.8' : 'sortAlbumName',
            'selectedColumns.member.9' : 'sortArtistName',
            'sortCriteriaList' : '',
            'sortCriteriaList.member.1.sortColumn' : 'sortTitle',
            'sortCriteriaList.member.1.sortType' : 'ASC'
        };
        return that._post(this.api_location + 'cirrus/', {
            data: _query
        }, true).then(function(response) {
            previousResults = previousResults.concat(response.searchLibraryResponse.searchLibraryResult.searchReturnItemList.map(that._convertTrack));
            nextResultsToken = response.searchLibraryResponse.searchLibraryResult.nextResultsToken;
            if (null === nextResultsToken)
                return previousResults;
            return that._getLibraryTracks(previousResults, nextResultsToken);
        });
    }
});

var amazonCollection = Tomahawk.extend(Tomahawk.Collection, {
    resolver: AmazonResolver,
    settings: {
        id: "amazon",
        prettyname: "Amazon Music Library",
        iconfile: "contents/images/icon.png"
    }
});
Tomahawk.resolver.instance = AmazonResolver;
