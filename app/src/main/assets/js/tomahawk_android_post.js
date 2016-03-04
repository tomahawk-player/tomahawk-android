/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2014, Enno Gottschalk <mrmaffen@googlemail.com>
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

/**
 * This method is externalized from Tomahawk.asyncRequest, so that other clients
 * (like tomahawk-android) can inject their own logic that determines whether or not to do a request
 * natively.
 *
 * @returns boolean indicating whether or not to do a request with the given parameters natively
 */
var shouldDoNativeRequest = function (options) {
    return ((options && options.needCookieHeader)
        || (options.headers
            && (options.headers.hasOwnProperty("Referer")
            || options.headers.hasOwnProperty("referer")
            || options.headers.hasOwnProperty("Origin")
            || options.headers.hasOwnProperty("origin")
            || options.headers.hasOwnProperty("User-Agent"))
        )
    );
};

Tomahawk.Collection.addTracks = function (params) {
    return Tomahawk.NativeScriptJobManager.invoke("collectionAddTracks", params);
};

Tomahawk.Collection.wipe = function (params) {
    return Tomahawk.NativeScriptJobManager.invoke("collectionWipe", params);
};

Tomahawk.Collection.revision = function (params) {
    return Tomahawk.NativeScriptJobManager.invoke("collectionRevision", params);
};

var encodeParamsToNativeFunctions = function (param) {
    return JSON.stringify(param);
};

Tomahawk.error = function (msg, obj) {
    var objString = "";
    if (obj) {
        objString = " - " + JSON.stringify(obj);
    }
    var error;
    if (obj instanceof Error) {
        error = obj;
    } else {
        error = new Error('');
    }
    var stack = error.stack;
    stack = stack.split('\n').map(function (line) {
        return line.trim();
    });
    stack = stack.splice(stack[0] == 'Error' ? 2 : 1).join("\n");
    console.error(msg + objString + "\n" + stack + "\n");
};