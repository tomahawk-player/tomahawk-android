/*
 *   Copyright 2013,      Uwe L. Korn <uwelk@xhochy.com>
 *   Copyright 2014,      Enno Gottschalk <mrmaffen@googlemail.com>
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

var RdioMetadataResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'rdio',
        icon: 'rdio.png',
        weight: 95,
        timeout: 15
    },

	init: function() {
        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);
	},

    properEncode: function(str) {
        return encodeURIComponent(str)
            .replace(/\!/g, "%21")
            .replace(/\*/g, "%2A")
            .replace(/\'/g, "%27")
            .replace(/\(/g, "%28")
            .replace(/\)/g, "%29");
    },

    spell: function(a){magic=function(b){return(b=(b)?b:this).split("").map(function(d){if(!d.match(/[A-Za-z]/)){return d}c=d.charCodeAt(0)>=96;k=(d.toLowerCase().charCodeAt(0)-96+12)%26+1;return String.fromCharCode(k+(c?96:64))}).join("")};return magic(a)},

    resolve: function (qid, artist, album, title) {
        var that = this;
        var fetchUrl = 'http://api.rdio.com/1/'
        var query = 'method=search';
        query += '&oauth_consumer_key=' + this.spell("tdo7m2m8u6r7r76mpod9jqlc");
        var nonce = '';
        for (i = 0; i < 8; i++) nonce += parseInt(Math.random() * 10).toString();
        query += '&oauth_nonce=' + nonce;
        query += '&oauth_signature_method=' + encodeURIComponent('HMAC-SHA1');
        query += '&oauth_timestamp=' + Math.round((new Date()).getTime() / 1000);;
        query += '&oauth_version=1.0';
        query += '&query=' + this.properEncode(artist + " " + title) + '&types=Track';
        var toSign = 'POST&' + encodeURIComponent(fetchUrl) + '&' + encodeURIComponent(query);
        var signature = CryptoJS.HmacSHA1(toSign, this.spell("oKeSjHrS9d") + '&').toString(CryptoJS.enc.Base64);
        query += '&oauth_signature=' + encodeURIComponent(signature);
		Tomahawk.log("query: " + query);

        Tomahawk.asyncRequest(fetchUrl, function (xhr) {
            var res = JSON.parse(xhr.responseText);
            Tomahawk.log("Received result with status: " + res.status + ", message: " + res.message);
            if (res.status == 'ok' && res.result.results.length !== 0) {
                Tomahawk.log("Result was ok, resultCount: " + res.result.results.length);
                var results = [];
                for (i = 0; i < res.result.results.length; i++) {
                    if (res.result.results[i].type == 't' && res.result.results[i].canStream) {
                        var result = {
                            source: that.settings.name,
                            artist: res.result.results[i].artist,
                            track: res.result.results[i].name,
                            duration: res.result.results[i].duration,
                            url: "rdio://track/" + res.result.results[i].key,
                            album: res.result.results[i].album,
                            linkUrl: res.result.results[i].url
                        };
                        results.push(result);
                    }
                }
				Tomahawk.addTrackResults({
                    qid: qid,
                    results: [results[0]]
                });
            } else {
                var empty = {
                    results: [],
                    qid: qid
                };
				Tomahawk.addTrackResults(empty);
            }
        }, {"Content-type": "application/x-www-form-urlencoded"}, {
            method: 'post',
            data: query
        });
    },

	search: function (qid, searchString) {
        this.resolve(qid, "", "", searchString);
	},

    canParseUrl: function (url, type) {
        switch (type) {
        case TomahawkUrlType.Album:
            return /https?:\/\/(www\.)?rdio.com\/artist\/([^\/]*)\/album\/([^\/]*)\/?$/.test(url);
        case TomahawkUrlType.Artist:
            return /https?:\/\/(www\.)?rdio.com\/artist\/([^\/]*)\/?$/.test(url);
        case TomahawkUrlType.Playlist:
            return /https?:\/\/(www\.)?rdio.com\/people\/([^\/]*)\/playlists\/(\d+)\//.test(url);
        case TomahawkUrlType.Track:
            return /https?:\/\/(www\.)?rdio.com\/artist\/([^\/]*)\/album\/([^\/]*)\/track\/([^\/]*)\/?$/.test(url);
        // case TomahawkUrlType.Any:
        default:
            return /https?:\/\/(www\.)?rdio.com\/([^\/]*\/|)/.test(url);
        }
    },

    lookupUrl: function (url) {
        var that = this;
        var fetchUrl = 'http://api.rdio.com/1/'
        var query = 'extras=tracks&method=getObjectFromUrl';
        query += '&oauth_consumer_key=' + this.spell("tdo7m2m8u6r7r76mpod9jqlc");
        var nonce = '';
        for (i = 0; i < 8; i++) nonce += parseInt(Math.random() * 10).toString();
        query += '&oauth_nonce=' + nonce;
        query += '&oauth_signature_method=' + encodeURIComponent('HMAC-SHA1');
        query += '&oauth_timestamp=' + Math.round((new Date()).getTime() / 1000);;
        query += '&oauth_version=1.0';
        query += '&url=' + encodeURIComponent(url);
        var toSign = 'POST&' + encodeURIComponent(fetchUrl) + '&' + encodeURIComponent(query);
        var signature = CryptoJS.HmacSHA1(toSign, this.spell("oKeSjHrS9d") + '&').toString(CryptoJS.enc.Base64);
        query += '&oauth_signature=' + encodeURIComponent(signature);
        Tomahawk.asyncRequest(fetchUrl, function (xhr) {
            var res = JSON.parse(xhr.responseText);
            if (res.status == 'ok') {
                if (res.result.type == 'p') {
                    var result = {
                        type: "playlist",
                        title: res.result.name,
                        guid: "rdio-playlist-" + res.result.key,
                        info: "A playlist by " + res.result.owner + " on rdio.",
                        creator: res.result.owner,
                        url: res.result.shortUrl,
                        tracks: []
                    };
                    result.tracks = res.result.tracks.map(function (item) { return { type: "track", title: item.name, artist: item.artist }; });
                    Tomahawk.addUrlResult(url, result);
                } else if (res.result.type == 't') {
                    Tomahawk.addUrlResult(url, {
                        type: "track",
                        title: res.result.name,
                        artist: res.result.artist,
                    });
                } else if (res.result.type == 'a') {
                    Tomahawk.addUrlResult(url, {
                        type: "album",
                        name: res.result.name,
                        artist: res.result.artist,
                    });
                } else if (res.result.type == 'r') {
                    Tomahawk.addUrlResult(url, {
                        type: "artist",
                        name: res.result.name,
                    });
                }
            }
        }, {"Content-type": "application/x-www-form-urlencoded"}, {
            method: 'post',
            data: query
        });
    }
});

Tomahawk.resolver.instance = RdioMetadataResolver;

