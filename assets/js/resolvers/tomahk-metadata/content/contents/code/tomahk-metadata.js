/*
 *   Copyright 2013,      Uwe L. Korn <uwelk@xhochy.com>
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

var TomaHKMetadataResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'toma.hk Metadata',
        icon: 'tomahk-metadata.png',
        weight: 0, // We cannot resolve, so use minimum weight
        timeout: 15
    },

    init: function() {
        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);
    },


    resolve: function (qid, artist, album, title) {
        Tomahawk.addTrackResults({ results: [], qid: qid });
    },

    search: function (qid, searchString) {
        Tomahawk.addTrackResults({ results: [], qid: qid });
    },

    canParseUrl: function (url, type) {
        // Soundcloud only returns tracks and playlists
        switch (type) {
            case TomahawkUrlType.Album:
                return /https?:\/\/(www\.)?toma.hk\/album\//.test(url);
                    case TomahawkUrlType.Artist:
                return /https?:\/\/(www\.)?toma.hk\/artist\//.test(url);
                    case TomahawkUrlType.Playlist:
                return /https?:\/\/(www\.)?toma.hk\/p\//.test(url)
                    // case TomahawkUrlType.Track:
                // case TomahawkUrlType.Any:
                default:
                    return /https?:\/\/(www\.)?toma.hk\//.test(url);
        }
    },

    lookupUrl: function (url) {
        var that = this;
        var urlParts = url.split('/').filter(function (item) { return item.length != 0; }).map(decodeURIComponent);
        if (/https?:\/\/(www\.)?toma.hk\/album\//.test(url)) {
            // We have to deal with an Album
            Tomahawk.addUrlResult(url, {
                type: 'album',
                name: urlParts[urlParts.length - 1],
                artist: urlParts[urlParts.length - 2]
            });
        } else if (/https?:\/\/(www\.)?toma.hk\/artist\//.test(url)) {
            // We have to deal with an Artist
            Tomahawk.addUrlResult(url, {
                type: 'artist',
                name: urlParts[urlParts.length - 1]
            });
        } else if (/https?:\/\/(www\.)?toma.hk\/p\//.test(url)) {
            // We have a playlist
            Tomahawk.addUrlResult(url, {
                type: 'xspf-url',
                url: url.replace('toma.hk/p/', 'toma.hk/xspf/')
            });
        } else if (/https?:\/\/(www\.)?toma.hk\/\?(artist=)[^&]*(&title=)/.test(url)) {
            // We search for a track
            Tomahawk.addUrlResult(url, {
                type: "track",
                title: decodeURIComponent(url.match(/(\&title=)([^&]*)/)[2]),
                artist: decodeURIComponent(url.match(/(\?artist=)([^&]*)/)[2])
            })
        } else if (/https?:\/\/(www\.)?toma.hk\/\?(title=)[^&]*(&artist=)/.test(url)) {
            // We search for a track
            Tomahawk.addUrlResult(url, {
                type: "track",
                title: decodeURIComponent(url.match(/(\?title=)([^&]*)/)[2]),
                artist: decodeURIComponent(url.match(/(\&artist=)([^&]*)/)[2])
            })
        } else {
            // We most likely have a track
            var query = url.replace("http://toma.hk/", "http://toma.hk/api.php?id=");
                Tomahawk.asyncRequest(query, function (xhr) {
                    var res = JSON.parse(xhr.responseText);
                    if (res.artist.length > 0 && res.title.length > 0) {
                        Tomahawk.addUrlResult(url, {
                            type: "track",
                            title: res.title,
                            artist: res.artist
                        });
                    } else {
                        Tomahawk.addUrlResult(url, {});
                    }
                });
        }
    }
});

Tomahawk.resolver.instance = TomaHKMetadataResolver;

