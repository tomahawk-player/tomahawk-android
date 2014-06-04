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
 * This function overrides the standard implementation in tomahawk.js in order to be able to process
 * the request on the java side and not directly in js. This way we can avoid the typical js
 * limitations.
 *
 * Possible options:
 *  - method: The HTTP request method (default: GET)
 *  - username: The username for HTTP Basic Auth
 *  - password: The password for HTTP Basic Auth
 *  - errorHandler: callback called if the request was not completed
 *  - data: body data included in POST requests
 *  - needCookieHeader: boolean to indicate whether or not the request should return the "Set-Cookie" header
 */
Tomahawk.asyncRequest = function (url, callback, extraHeaders, options) {
    if (options && options.needCookieHeader) {
        if (!Tomahawk.idCounter) {
            Tomahawk.idCounter = 0;
        }
        if (!Tomahawk.callbackMap) {
            Tomahawk.callbackMap = {};
        }
        var callbackId = -1;
        if (callback) {
            callbackId = Tomahawk.idCounter++;
            Tomahawk.callbackMap[callbackId] = callback;
        }
        var errorHandlerId = -1;
        if (options && options.errorHandler) {
            errorHandlerId = Tomahawk.idCounter++;
            Tomahawk.callbackMap[errorHandlerId] = options.errorHandler;
        }
        var extraHeadersString = null;
        if (extraHeaders) {
            extraHeadersString = JSON.stringify(extraHeaders);
        }
        var optionsString = null;
        if (options) {
            optionsString = JSON.stringify(options);
        }
        Tomahawk.javaAsyncRequest(url, callbackId, extraHeadersString, optionsString,
            errorHandlerId);
    } else {
        // unpack options
        var opt = options || {};
        var method = opt.method || 'GET';

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
            } else if (xmlHttpRequest.readyState == 4 && xmlHttpRequest.status == 302) {
                // You know that XMLHttpRequest always follows redirects?
                // Guess what: It does not always.
                //
                // Known:
                // * If you are redirect to a different domain in QtWebkit on MacOS,
                //   you will have to deal with 302.
                Tomahawk.asyncRequest(xmlHttpRequest.getResponseHeader('Location'),
                    callback, extraHeaders, options);
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

function FakeXHR(responseText, responseHeaders, status, statusText) {
    this.responseHeaders = JSON.parse(responseHeaders);
    this.responseText = responseText;
    this.status = status;
    this.statusText = statusText;
    this.getAllResponseHeaders = function () {
        return this.responseHeaders;
    };
    this.getResponseHeader = function (header) {
        return this.responseHeaders[header];
    };
}

Tomahawk.callback = function (callbackId, responseText, responseHeaders, status, statusText) {
    Tomahawk.callbackMap[callbackId](new FakeXHR(responseText, responseHeaders, status,
        statusText));
};