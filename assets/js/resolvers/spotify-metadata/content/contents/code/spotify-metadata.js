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

var SpotifyMetadataResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'Spotify Metadata',
        icon: 'spotify-metadata.png',
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
        switch (type) {
        case TomahawkUrlType.Album:
            return /spotify:album:/.test(url) || /https?:\/\/(play|open)\.spotify\.[^\/]+\/album\//.test(url);
        case TomahawkUrlType.Artist:
            return /spotify:artist:/.test(url) || /https?:\/\/(play|open)\.spotify\.[^\/]+\/artist\//.test(url);
        case TomahawkUrlType.Playlist:
            return /spotify:user:[0-9]*:playlist:/.test(url);
        case TomahawkUrlType.Track:
            return /spotify:track:/.test(url) || /https?:\/\/(play|open)\.spotify\.[^\/]+\/track\//.test(url);
        // case TomahawkUrlType.Any:
        default:
            return /spotify:(album|artist|track|user):/.test(url) || /https?:\/\/(play|open)\.spotify\.[^\/]+\/(album|artist|track)\//.test(url);
        }
    },

    lookupUrl: function (url) {
        var that = this;
        var match = url.match(/spotify:(album|artist|track):(.*)/);
        var playlistmatch = url.match(/spotify:user:[0-9]*:playlist:(.*)/);
        if (match == null) {
            match = url.match(/https?:\/\/(play|open)\.spotify\.[^\/]+\/(album|artist|track)\/([^\/\?]*)/);
            if (match != null) match.splice(1, 1);
        }
        if (match != null) {
            var query = 'https://ws.spotify.com/lookup/1/.json?uri=spotify:' + match[1] + ':' + match[2];
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                if (match[1] == "artist") {
                    Tomahawk.addUrlResult(url, {
                        type: "artist",
                        name: res.artist.name
                    });
                } else if (match[1] == "album") {
                    Tomahawk.addUrlResult(url, {
                        type: "album",
                        name: res.album.name,
                        artist: res.album.artist,
                    });
                } else if (match[1] == "track") {
                    Tomahawk.addUrlResult(url, {
                        type: "track",
                        title: res.track.name,
                        artist: res.track.artists.map(function (item) { return item.name; }).join(" & ")
                    });
                }
            });
        } else if (playlistmatch != null) {
            var query = 'http://spotikea.tomahawk-player.org/browse/' + url;
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                var result = {
                    type: "playlist",
                    title: res.playlist.name,
                    guid: "spotify-playlist-" + url,
                    info: "A playlist on Spotify.",
                    creator: res.playlist.creator,
                    url: url,
                    tracks: []
                };
                result.tracks = res.playlist.result.map(function (item) { return { type: "track", title: item.title, artist: item.artist }; });
                Tomahawk.addUrlResult(url, result);
            });
        }
    }
});

Tomahawk.resolver.instance = SpotifyMetadataResolver;

