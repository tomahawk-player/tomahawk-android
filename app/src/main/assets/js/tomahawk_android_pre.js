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

Tomahawk.localStorage = {
    setItem: function (key, value) {
        Tomahawk.localStorageSetItem(key, value);
    },
    getItem: function (key) {
        return Tomahawk.localStorageGetItem(key);
    },
    removeItem: function (key) {
        Tomahawk.localStorageRemoveItem(key);
    }
};
