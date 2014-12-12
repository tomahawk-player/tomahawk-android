/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2011,      Dominik Schmidt <domme@tomahawk-player.org>
 *   Copyright 2011-2012, Leo Franchi <lfranchi@kde.org>
 *   Copyright 2011,      Thierry Goeckel
 *   Copyright 2013,      Teo Mrnjavac <teo@kde.org>
 *   Copyright 2013-2014,  Uwe L. Korn <uwelk@xhochy.com>
 *   Copyright 2014,      Enno Gottschalk <mrmaffen@googlemail.com>
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
 */

// if run in phantomjs add fake Tomahawk environment
if ((typeof Tomahawk === "undefined") || (Tomahawk === null)) {
    var Tomahawk = {
        fakeEnv: function () {
            return true;
        },
        resolverData: function () {
            return {
                scriptPath: function () {
                    return "/home/tomahawk/resolver.js";
                }
            };
        },
        log: function (message) {
            console.log(message);
        }
    };
}

Tomahawk.apiVersion = "0.2.2";

/**
 * Compares versions strings
 * (version1 < version2) == -1
 * (version1 = version2) == 0
 * (version1 > version2) == 1
 */
Tomahawk.versionCompare = function (version1, version2) {
    var v1 = version1.split('.').map(function (item) { return parseInt(item); });
    var v2 = version2.split('.').map(function (item) { return parseInt(item); })
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
            if ( v1[i] === 0 ) {
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
};

/**
 * Check if this is at least specified tomahawk-api-version.
 */
Tomahawk.atLeastVersion = function (version) {
    return (Tomahawk.versionCompare(Tomahawk.apiVersion, version) >= 0)
};


Tomahawk.resolver = {
    scriptPath: Tomahawk.resolverData().scriptPath
};

Tomahawk.timestamp = function () {
    return Math.round(new Date() / 1000);
};

Tomahawk.dumpResult = function (result) {
    var results = result.results,
        i = 0;
    Tomahawk.log("Dumping " + results.length + " results for query " + result.qid + "...");
    for (i = 0; i < results.length; i++) {
        Tomahawk.log(results[i].artist + " - " + results[i].track + " | " + results[i].url);
    }

    Tomahawk.log("Done.");
};

// javascript part of Tomahawk-Object API
Tomahawk.extend = function (object, members) {
    var F = function () {};
    F.prototype = object;
    var newObject = new F();

    for (var key in members) {
        newObject[key] = members[key];
    }

    return newObject;
};


var TomahawkResolverCapability = {
    NullCapability: 0,
    Browsable:      1,
    PlaylistSync:   2,
    AccountFactory: 4,
    UrlLookup:      8
};

var TomahawkUrlType = {
    Any: 0,
    Playlist: 1,
    Track: 2,
    Album: 4,
    Artist: 8
};

var TomahawkConfigTestResultType = {
    Other: 0,
    Success: 1,
    Logout: 2,
    CommunicationError: 3,
    InvalidCredentials: 4,
    InvalidAccount: 5,
    PlayingElsewhere: 6,
    AccountExpired: 7
};


/**
 * Resolver BaseObject, inherit it to implement your own resolver.
 */
var TomahawkResolver = {
    init: function() {
    },
    scriptPath: function () {
        return Tomahawk.resolverData().scriptPath;
    },
    getConfigUi: function () {
        return {};
    },
    getUserConfig: function () {
        return JSON.parse(window.localStorage[this.scriptPath()] || "{}");
    },
    saveUserConfig: function () {
        var configJson = JSON.stringify(Tomahawk.resolverData().config);
        window.localStorage[ this.scriptPath() ] = configJson;
        this.newConfigSaved();
    },
    newConfigSaved: function () {
    },
    resolve: function (qid, artist, album, title) {
        return {
            qid: qid
        };
    },
    search: function (qid, searchString) {
        return this.resolve( qid, "", "", searchString );
    },
    artists: function (qid) {
        return {
            qid: qid
        };
    },
    albums: function (qid, artist) {
        return {
            qid: qid
        };
    },
    tracks: function (qid, artist, album) {
        return {
            qid: qid
        };
    },
    collection: function () {
        return {};
    }
};

/**** begin example implementation of a resolver ****/


// implement the resolver
/*
 *    var DemoResolver = Tomahawk.extend(TomahawkResolver,
 *    {
 *        getSettings: function()
 *        {
 *            return {
 *                name: "Demo Resolver",
 *                weigth: 95,
 *                timeout: 5,
 *                limit: 10
 };
 },
 resolve: function( qid, artist, album, track )
 {
 return {
 qid: qid,
 results: [
 {
 artist: "Mokele",
 album: "You Yourself are Me Myself and I am in Love",
 track: "Hiding In Your Insides (php)",
 source: "Mokele.co.uk",
 url: "http://play.mokele.co.uk/music/Hiding%20In%20Your%20Insides.mp3",
 bitrate: 160,
 duration: 248,
 size: 4971780,
 score: 1.0,
 extension: "mp3",
 mimetype: "audio/mpeg"
 }
 ]
 };
 }
 }
 );

 // register the resolver
 Tomahawk.resolver.instance = DemoResolver;*/

/**** end example implementation of a resolver ****/


// help functions

Tomahawk.valueForSubNode = function (node, tag) {
    if (node === undefined) {
        throw new Error("Tomahawk.valueForSubnode: node is undefined!");
    }

    var element = node.getElementsByTagName(tag)[0];
    if (element === undefined) {
        return undefined;
    }

    return element.textContent;
};

/**
 * Do a synchronous HTTP(S) request. For further options see
 * Tomahawk.asyncRequest
 */
Tomahawk.syncRequest = function (url, extraHeaders, options) {
    // unpack options
    var opt = options || {};
    var method = opt.method || 'GET';

    var xmlHttpRequest = new XMLHttpRequest();
    xmlHttpRequest.open(method, url, false, opt.username, opt.password);
    if (extraHeaders) {
        for (var headerName in extraHeaders) {
            xmlHttpRequest.setRequestHeader(headerName, extraHeaders[headerName]);
        }
    }
    xmlHttpRequest.send(null);
    if (xmlHttpRequest.status == 200) {
        return xmlHttpRequest.responseText;
    } else {
        Tomahawk.log("Failed to do GET request: to: " + url);
        Tomahawk.log("Status Code was: " + xmlHttpRequest.status);
        if (opt.hasOwnProperty('errorHandler')) {
            opt.errorHandler.call(window, xmlHttpRequest);
        }
    }
};

/**
 * Internal counter used to identify retrievedMetadata call back from native
 * code.
 */
Tomahawk.retrieveMetadataIdCounter = 0;
/**
 * Internal map used to map metadataIds to the respective JavaScript callbacks.
 */
Tomahawk.retrieveMetadataCallbacks = {};

/**
 * Retrieve metadata for a media stream.
 *
 * @param url String The URL which should be scanned for metadata.
 * @param mimetype String The mimetype of the stream, e.g. application/ogg
 * @param sizehint Size in bytes if not supplied possibly the whole file needs
 *          to be downloaded
 * @param options Object Map to specify various parameters related to the media
 *          URL. This includes:
 *          * headers: Object of HTTP(S) headers that should be set on doing the
 *                     request.
 *          * method: String HTTP verb to be used (default: GET)
 *          * username: Username when using authentication
 *          * password: Password when using authentication
 * @param callback Function(Object,String) This function is called on completeion.
 *          If an error occured, error is set to the corresponding message else
 *          null.
 */
Tomahawk.retrieveMetadata = function (url, mimetype, sizehint, options, callback) {
    var metadataId = Tomahawk.retrieveMetadataIdCounter;
    Tomahawk.retrieveMetadataIdCounter++;
    Tomahawk.retrieveMetadataCallbacks[metadataId] = callback;
    Tomahawk.nativeRetrieveMetadata(metadataId, url, mimetype, sizehint, options);
};

/**
 * Pass the natively retrieved metadata back to the JavaScript callback.
 *
 * Internal use only!
 */
Tomahawk.retrievedMetadata = function(metadataId, metadata, error) {
    // Check we have a matching callback stored.
    if (!Tomahawk.retrieveMetadataCallbacks.hasOwnProperty(metadataId)) {
        return;
    }

    // Call the real callback
    if (Tomahawk.retrieveMetadataCallbacks.hasOwnProperty(metadataId)) {
        Tomahawk.retrieveMetadataCallbacks[metadataId](metadata, error);
    }

    // Callback are only used once.
    delete Tomahawk.retrieveMetadataCallbacks[metadataId];
};

/**
 * Internal counter used to identify asyncRequest callback from native code.
 */
Tomahawk.asyncRequestIdCounter = 0;
/**
 * Internal map used to map asyncRequestIds to the respective javascript
 * callback functions.
 */
Tomahawk.asyncRequestCallbacks = {};

/**
 * Pass the natively retrived reply back to the javascript callback
 * and augment the fake XMLHttpRequest object.
 *
 * Internal use only!
 */
Tomahawk.nativeAsyncRequestDone = function (reqId, xhr) {
    // Check that we have a matching callback stored.
    if (!Tomahawk.asyncRequestCallbacks.hasOwnProperty(reqId)) {
        return;
    }

    if (xhr.readyState == 4 && xhr.status == 200) {
        // Call the real callback
        if (Tomahawk.asyncRequestCallbacks[reqId].callback) {
            Tomahawk.asyncRequestCallbacks[reqId].callback(xhr);
        }
    } else if (xmlHttpRequest.readyState === 4) {
        Tomahawk.log("Failed to do nativeAsyncRequest");
        Tomahawk.log("Status Code was: " + xhr.status);
        if (Tomahawk.asyncRequestCallbacks[reqId].errorHandler) {
            Tomahawk.asyncRequestCallbacks[reqId].errorHandler(xhr);
        }
    }

    // Callbacks are only used once.
    delete Tomahawk.asyncRequestCallbacks[reqId];
};

/**
 * Possible options:
 *  - method: The HTTP request method (default: GET)
 *  - username: The username for HTTP Basic Auth
 *  - password: The password for HTTP Basic Auth
 *  - errorHandler: callback called if the request was not completed
 *  - data: body data included in POST requests
 *  - needCookieHeader: boolean indicating whether or not the request needs to be able to get the
 *                      "Set-Cookie" response header
 */
Tomahawk.asyncRequest = function (url, callback, extraHeaders, options) {
    // unpack options
    var opt = options || {};
    var method = opt.method || 'GET';
    if (shouldDoNativeRequest(url, callback, extraHeaders, options)) {
        // Assign a request Id to the callback so we can use it when we are
        // returning from the native call.
        var reqId = Tomahawk.asyncRequestIdCounter;
        Tomahawk.asyncRequestIdCounter++;
        Tomahawk.asyncRequestCallbacks[reqId] = {
            callback: callback,
            errorHandler: opt.errorHandler
        };
        Tomahawk.nativeAsyncRequest(reqId, url, extraHeaders, options);
    } else {
        var xmlHttpRequest = new XMLHttpRequest();
        xmlHttpRequest.open(method, url, true, opt.username, opt.password);
        if (extraHeaders) {
            for (var headerName in extraHeaders) {
                xmlHttpRequest.setRequestHeader(headerName, extraHeaders[headerName]);
            }
        }
        xmlHttpRequest.onreadystatechange = function () {
            if (xmlHttpRequest.readyState == 4 && xmlHttpRequest.status == 200) {
                callback.call(window, xmlHttpRequest);
            } else if (xmlHttpRequest.readyState === 4) {
                Tomahawk.log("Failed to do " + method + " request: to: " + url);
                Tomahawk.log("Status Code was: " + xmlHttpRequest.status);
                if (opt.hasOwnProperty('errorHandler')) {
                    opt.errorHandler.call(window, xmlHttpRequest);
                }
            }
        };
        xmlHttpRequest.send(opt.data || null);
    }
};

/**
 * This method is externalized from Tomahawk.asyncRequest, so that other clients
 * (like tomahawk-android) can inject their own logic that determines whether or not to do a request
 * natively.
 *
 * @returns boolean indicating whether or not to do a request with the given parameters natively
 */
shouldDoNativeRequest = function (url, callback, extraHeaders, options) {
    return (extraHeaders && (extraHeaders.hasOwnProperty("Referer")
        || extraHeaders.hasOwnProperty("referer")));
};

Tomahawk.sha256 = Tomahawk.sha256 || function(message) {
  return CryptoJS.SHA256(message).toString(CryptoJS.enc.Hex);
};
Tomahawk.md5 = Tomahawk.md5 || function(message) {
  return CryptoJS.MD5(message).toString(CryptoJS.enc.Hex);
};
// Return a HMAC (md5) signature of the input text with the desired key
Tomahawk.hmac = function (key, message) {
    return CryptoJS.HmacMD5(message, key).toString(CryptoJS.enc.Hex);
};

// some aliases
Tomahawk.setTimeout = Tomahawk.setTimeout || window.setTimeout;
Tomahawk.setInterval = Tomahawk.setInterval || window.setInterval;
Tomahawk.base64Decode = window.atob;
Tomahawk.base64Encode = window.btoa;
