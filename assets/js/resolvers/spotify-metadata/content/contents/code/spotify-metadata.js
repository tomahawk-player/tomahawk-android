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
            return /spotify:album:([^:]+)/.test(url)
                || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/album\/([^\/\?]+)/.test(url);
        case TomahawkUrlType.Artist:
            return /spotify:artist:([^:]+)/.test(url)
                || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/artist\/([^\/\?]+)/.test(url);
        case TomahawkUrlType.Playlist:
            return /spotify:user:([^:]+):playlist:([^:]+)/.test(url)
                || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/user\/([^\/]+)\/playlist\/([^\/\?]+)/.test(url);;
        case TomahawkUrlType.Track:
            return /spotify:track:([^:]+)/.test(url)
                || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/track\/([^\/\?]+)/.test(url);
        // case TomahawkUrlType.Any:
        default:
            return /spotify:(album|artist|track):([^:]+)/.test(url)
                || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/(album|artist|track)\/([^\/\?]+)/.test(url)
                || /spotify:user:([^:]+):playlist:([^:]+)/.test(url)
                || /https?:\/\/(?:play|open)\.spotify\.[^\/]+\/user\/([^\/]+)\/playlist\/([^\/\?]+)/.test(url);
        }
    },

    lookupUrl: function (url) {
        Tomahawk.log("lookupUrl: '" + url + "'");
        var that = this;
        var match = url.match(/spotify:(album|artist|track):([^:]+)/);
        if (match == null) {
            match = url.match(/https?:\/\/(?:play|open)\.spotify\.[^\/]+\/(album|artist|track)\/([^\/\?]+)/);
        }
        var playlistmatch = url.match(/spotify:user:([^:]+):playlist:([^:]+)/);
        if (playlistmatch == null) {
            var playlistmatch = url.match(/https?:\/\/(?:play|open)\.spotify\.[^\/]+\/user\/([^\/]+)\/playlist\/([^\/\?]+)/);
        }
        if (match != null) {
            var query = 'https://ws.spotify.com/lookup/1/.json?uri=spotify:' + match[1] + ':' + match[2];
            Tomahawk.log("Found album/artist/track, calling " + query);
            Tomahawk.asyncRequest(query, function (xhr) {
                var res = JSON.parse(xhr.responseText);
                if (match[1] == "artist") {
                    var result = {
                        type: "artist",
                        name: res.artist.name
                    };
                    Tomahawk.addUrlResult(url, result);
                    Tomahawk.log("Reported found artist '" + result.name + "'");
                } else if (match[1] == "album") {
                    var result = {
                        type: "album",
                        name: res.album.name,
                        artist: res.album.artist
                    };
                    Tomahawk.addUrlResult(url, result);
                    Tomahawk.log("Reported found album '" + result.name + "' by '" + result.artist + "'");
                } else if (match[1] == "track") {
                    var result = {
                        type: "track",
                        title: res.track.name,
                        artist: res.track.artists.map(function (item) { return item.name; }).join(" & ")
                    };
                    Tomahawk.addUrlResult(url, result);
                    Tomahawk.log("Reported found track '" + result.title + "' by '" + result.artist + "'");
                }
            });
        } else if (playlistmatch != null) {
            var query = 'http://spotikea.tomahawk-player.org/browse/spotify:user:' + playlistmatch[1]
                        + ':playlist:' + playlistmatch[2];
            Tomahawk.log("Found playlist, calling url: '" + query + "'");
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
                Tomahawk.log("Reported found playlist '" + result.title + "' containing " + result.tracks.length + " tracks");
            });
        }
    }
});

Tomahawk.resolver.instance = SpotifyMetadataResolver;

