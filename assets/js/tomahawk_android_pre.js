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

// This detour is needed because we can only send Strings and no JSON objects back to JAVA through
// the Javascript Interface. Therefore we receive the JSON object here, stringify it and send it
// back to Java, where we then parse it to a JSON object again.
Tomahawk.addTrackResults =
    function (results) {
        Tomahawk.addTrackResultsString(JSON.stringify(results));
    };

// This detour is needed because we can only send Strings and no JSON objects back to JAVA through
// the Javascript Interface. Therefore we receive the JSON object here, stringify it and send it
// back to Java, where we then parse it to a JSON object again.
Tomahawk.reportStreamUrl =
    function (qid, url, headers) {
        var stringifiedHeaders = null;
        if (headers) {
            stringifiedHeaders = JSON.stringify(headers);
        }
        Tomahawk.reportStreamUrlString(qid, url, stringifiedHeaders);
    };