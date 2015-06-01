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
 * This method is externalized from Tomahawk.asyncRequest, so that we can inject our own logic that
 * determines whether or not to do a request natively.
 *
 * @returns boolean indicating whether or not to do a request with the given parameters natively
 */
shouldDoNativeRequest = function (url, callback, extraHeaders, options) {
    return ((options && options.needCookieHeader)
        || (extraHeaders
            && (extraHeaders.hasOwnProperty("Referer")
            || extraHeaders.hasOwnProperty("referer")
            || extraHeaders.hasOwnProperty("Origin")
            || extraHeaders.hasOwnProperty("origin")
            || extraHeaders.hasOwnProperty("User-Agent"))
        )
    );
};