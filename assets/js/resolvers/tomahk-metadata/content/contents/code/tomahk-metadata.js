/*
 *   Copyright 2013, Uwe L. Korn <uwelk@xhochy.com>
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

var TomaHKMetadataResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    settings: {
        name: 'toma.hk Metadata',
        icon: 'tomahk-metadata.png',
        weight: 0, // We cannot resolve, so use minimum weight
        timeout: 15
    },

    canParseUrl: function (params) {
        var url = params.url;
        var type = params.type;

        switch (type) {
            case Tomahawk.UrlType.Album:
                return /https?:\/\/(www\.)?toma.hk\/album\//.test(url);
            case Tomahawk.UrlType.Artist:
                return /https?:\/\/(www\.)?toma.hk\/artist\//.test(url);
            case Tomahawk.UrlType.Playlist:
                return /https?:\/\/(www\.)?toma.hk\/p\//.test(url);
            default:
                return /https?:\/\/(www\.)?toma.hk\//.test(url);
        }
    },

    lookupUrl: function (params) {
        var url = params.url;

        var urlParts =
            url.split('/').filter(function (item) {
                return item.length != 0;
            }).map(function (s) {
                return decodeURIComponent(s.replace(/\+/g, '%20'));
            });
        if (/https?:\/\/(www\.)?toma.hk\/album\//.test(url)) {
            // We have to deal with an Album
            return {
                type: Tomahawk.UrlType.Album,
                artist: urlParts[urlParts.length - 2],
                album: urlParts[urlParts.length - 1]
            };
        } else if (/https?:\/\/(www\.)?toma.hk\/artist\//.test(url)) {
            // We have to deal with an Artist
            return {
                type: Tomahawk.UrlType.Artist,
                artist: urlParts[urlParts.length - 1]
            };
        } else if (/https?:\/\/(www\.)?toma.hk\/p\//.test(url)) {
            // We have a xspf playlist
            return {
                type: Tomahawk.UrlType.XspfPlaylist,
                url: url.replace('toma.hk/p/', 'toma.hk/xspf/')
            };
        } else if (/https?:\/\/(www\.)?toma\.hk.*(\?title=)[^&]*(&artist=)/.test(url)
            || /https?:\/\/(www\.)?toma\.hk.*(\?artist=)[^&]*(&title=)/.test(url)) {
            // We search for a track
            var artist = url.match(/(?:\?|&)artist=([^&]*)/)[1];
            var title = url.match(/(?:\?|&)title=([^&]*)/)[1];
            return {
                type: Tomahawk.UrlType.Track,
                artist: decodeURIComponent(artist.replace(/\+/g, '%20')),
                track: decodeURIComponent(title.replace(/\+/g, '%20'))
            };
        } else {
            // We most likely have a track
            var query = url.replace("http://toma.hk/", "http://toma.hk/api.php?id=");
            return Tomahawk.get(query).then(function (res) {
                if (res.artist.length > 0 && res.title.length > 0) {
                    return {
                        type: Tomahawk.UrlType.Track,
                        artist: res.artist,
                        track: res.title
                    };
                }
            });
        }
    }
});

Tomahawk.resolver.instance = TomaHKMetadataResolver;

