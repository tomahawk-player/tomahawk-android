/*
 *   Copyright 2014, Uwe L. Korn <uwelk@xhochy.com>
 *
 *   The MIT License (MIT)
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
 *
 *   THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *   IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 *   FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 *   COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 *   IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 *   CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

var utils = require("../../test/utils.js");

var username = process.argv[2];
var password = process.argv[3];
var artist = process.argv[4];
var track = process.argv[5];

var owner = {};

// We have the relevant credentials, so get a resolver instance.
utils.loadResolver('beatsmusic', owner, function (err) {
    if (err) {
        console.log("Error on creating a resolver instance:");
        console.log(err);
        process.exit(1);
    }

    owner.context.once('track-result', function (qid, result) {
        owner.context.getStreamUrl(2, result.url);
    });
    owner.context.once('stream-url', function (qid, url) {
        console.log(url);
    });
    owner.instance.resolve(1, artist, "", track);
}, {
    user: username,
    password: password
});
