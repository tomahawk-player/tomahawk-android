/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
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

// This detour is needed because we can only send Strings and no JSON objects from Java to JS
// through the Javascript Interface.

Tomahawk.resolverData =
    function () {
        return JSON.parse(Tomahawk.resolverDataString());
    };

Tomahawk.addTrackResults =
    function (results) {
        Tomahawk.addTrackResultsString(JSON.stringify(results));
    };

Tomahawk.addAlbumResults =
    function (results) {
        Tomahawk.addAlbumResultsString(JSON.stringify(results));
    };

Tomahawk.addArtistResults =
    function (results) {
        Tomahawk.addArtistResultsString(JSON.stringify(results));
    };

Tomahawk.addAlbumTrackResults =
    function (results) {
        Tomahawk.addAlbumTrackResultsString(JSON.stringify(results));
    };

Tomahawk.reportStreamUrl =
    function (qid, url, headers) {
        var stringifiedHeaders = null;
        if (headers) {
            stringifiedHeaders = JSON.stringify(headers);
        }
        Tomahawk.reportStreamUrlString(qid, url, stringifiedHeaders);
    };

Tomahawk.createFuzzyIndex =
    function (indexList) {
        if (indexList) {
            Tomahawk.createFuzzyIndexString(JSON.stringify(indexList));
        }
    };

Tomahawk.searchFuzzyIndex =
    function (query) {
        return JSON.parse(Tomahawk.searchFuzzyIndexString(query));
    };

Tomahawk.resolveFromFuzzyIndex =
    function (artist, album, title) {
        return JSON.parse(Tomahawk.resolveFromFuzzyIndexString(artist, album, title));
    };

Tomahawk.nativeAsyncRequest =
    function (reqId, url, extraHeaders, options) {
        Tomahawk.nativeAsyncRequestString(reqId, url, JSON.stringify(extraHeaders),
            JSON.stringify(options));
    };

function FakeXHR(responseText, responseHeaders, status, statusText) {
    this.responseHeaders = JSON.parse(responseHeaders);
    this.responseText = responseText;
    this.readyState = 4;
    this.status = status;
    this.statusText = statusText;
    this.getAllResponseHeaders = function () {
        return this.responseHeaders;
    };
    this.getResponseHeader = function (header) {
        return this.responseHeaders[header];
    };
}

Tomahawk.callback = function (reqId, responseText, responseHeaders, status, statusText) {
    Tomahawk.nativeAsyncRequestDone(reqId,
        new FakeXHR(responseText, responseHeaders, status, statusText));
};