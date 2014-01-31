/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 * Copyright 2011, lasonic <lasconic@gmail.com>
 * Copyright 2013, Uwe L. Korn <uwelk@xhochy.com>
 *
 * Tomahawk is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Tomahawk is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */

var ExfmResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'Ex.fm',
        icon: 'exfm-icon.png',
        weight: 30,
        timeout: 5
    },

    init: function () {
        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);
    },

    cleanTitle: function (title, artist) {
        // If the title contains a newline character, strip them off and remove additional spacing
        var newTitle = "",
            stringArray = title.split("\n");
        title.split("\n").forEach(function (split) {
            newTitle += split.trim() + " ";
        });
        // Remove dash and quotation characters.
        newTitle = newTitle.replace("\u2013", "").replace("  ", " ").replace("\u201c", "").replace("\u201d", "");
        // If the artist is included in the song title, cut it
        if (newTitle.toLowerCase().indexOf(artist.toLowerCase() + " -") === 0) {
            newTitle = newTitle.slice(artist.length + 2).trim();
        } else if (newTitle.toLowerCase().indexOf(artist.toLowerCase() + "-") === 0) {
            newTitle = newTitle.slice(artist.length + 1).trim();
        } else if (newTitle.toLowerCase().indexOf(artist.toLowerCase()) === 0) {
            // FIXME: This might break results where the artist name is a substring of the song title.
            newTitle = newTitle.slice(artist.length).trim();
        }
        return newTitle;
    },

    resolve: function (qid, artist, album, title) {
        var that = this,
        // Build search query for ex.fm
            url = "https://ex.fm/api/v3/song/search/" + encodeURIComponent(title) + "?start=0&results=20&client_id=tomahawk";

        // send request and parse it into javascript
        Tomahawk.asyncRequest(url, function (xhr) {
            // parse json
            var response = JSON.parse(xhr.responseText),
                results = [];

            // check the response
            if (response.results > 0) {
                response.songs.forEach(function (song) {
                    if ((song.url.indexOf("http://api.soundcloud") === 0) || (song.url.indexOf("https://api.soundcloud") === 0)) { // unauthorised, use soundcloud resolver instead
                        return;
                    }

                    if (song.artist === null || song.title === null) {
                        // This track misses relevant information, so we are going to ignore it.
                        return;
                    }
                    var result = {},
                        dTitle = that.cleanTitle(song.title, song.artist),
                        dArtist = song.artist,
                        dAlbum = "";
                    if (song.album !== null) {
                        dAlbum = song.album;
                    }
                    if ((dTitle.toLowerCase().indexOf(title.toLowerCase()) !== -1 && dArtist.toLowerCase().indexOf(artist.toLowerCase()) !== -1) || (artist === "" && album === "")) {
                        result.artist = ((dArtist !== "") ? dArtist : artist);
                        result.album = ((dAlbum !== "") ? dAlbum : album);
                        result.track = ((dTitle !== "") ? dTitle : title);
                        result.source = that.settings.name;
                        result.url = song.url;
                        result.extension = "mp3";
                        result.score = 0.80;
                        results.push(result);
                    }
                });
            }

            Tomahawk.addTrackResults({qid: qid, results: results});
        });
    },

    canParseUrl: function (url, type) {
        // Ex.fm does not support artists and only lists the album of the week as an album
        switch (type) {
        case TomahawkUrlType.Album:
            return /https?:\/\/(www\.)?ex.fm\/explore\/album-of-the-week/.test(url);
        case TomahawkUrlType.Artist:
            return false;
        case TomahawkUrlType.Track:
            return /https?:\/\/(www\.)?ex.fm\/song\//.test(url)
        // case TomahawkUrlType.Playlist:
        // case TomahawkUrlType.Any:
        default:
            return /https?:\/\/(www\.)?ex.fm\//.test(url);
        }
    },

    track2Result: function (item) {
        var result = {
            type: "track",
            title: item.title,
            artist: item.artist,
            album: item.album
        };
        return result;
    },

    lookupUrl: function (url) {
        var urlParts = url.split('/').filter(function (item) { return item !== "" });
        var query = "https://ex.fm/api/v3";
        var that = this;
        if (urlParts.length === 3) {
            // Url matches a user, e.g. https://ex.fm/xhochy
            query +=  "/user/" + urlParts[2] + "/trending";
            query += "?client_id=tomahawk";
        } else if (/\/explore\/site-of-the-day/.test(url)) {
            // Site of the day
            query += "/sotd?results=1";
            query += "&client_id=tomahawk";
        } else if (urlParts[2] == 'explore' && urlParts[3] == 'album-of-the-week') {
            query += '/atow?results=1';
            query += "&client_id=tomahawk";
        } else if (urlParts[2] == 'explore' && urlParts[3] == 'mixtape-of-the-month') {
            query += '/motm?results=1';
            query += "&client_id=tomahawk";
        } else if (/\/song\//.test(url)) {
            // Just one simple song
            query += "/song/" + urlParts[3];
            query += "?client_id=tomahawk";
        } else if (/https?:\/\/(www\.)?ex.fm\/site\//.test(url)) {
            query += url.replace(/https?:\/\/(www\.)?ex.fm/, '');
            query += "?client_id=tomahawk";
        } else if (urlParts[2] == "search") {
            query += "/song/search/" + urlParts[3];
            query += "?client_id=tomahawk";
        } else {
            query += url.replace(/https?:\/\/(www\.)?ex.fm/, '');
            query += "?client_id=tomahawk";
        }
        Tomahawk.asyncRequest(query, function (xhr) {
            var res = JSON.parse(xhr.responseText);
            if (res.hasOwnProperty("song")) {
                // One single song
                return that.track2Result(res.song);
            } else if (res.hasOwnProperty("site") && res.site.hasOwnProperty("songs")) {
                // A site with songs
                var result = {
                    type: "playlist",
                    title: res.site.title,
                    guid: 'exfm-site-' + Tomahawk.sha256(query),
                    info: "ex.fm parse of : " + res.site.url,
                    creator: "exfm",
                    url: url,
                    tracks: []
                };
                res.site.songs.forEach(function (item) {
                    result.tracks.push(that.track2Result(item));
                });
                Tomahawk.addUrlResult(url, result);
            } else if (res.hasOwnProperty("songs")) {
                // A list of songs
                var result = {
                    type: "playlist",
                    title: "ex.fm" + url.replace(/https?:\/\/(www\.)?ex.fm/, ''),
                    guid: 'exfm-playlist-' + Tomahawk.sha256(query),
                    info: "A playlist imported from ex.fm: " + url,
                    creator: "exfm",
                    url: url,
                    tracks: []
                };
                res.songs.forEach(function (item) {
                    result.tracks.push(that.track2Result(item));
                });
                Tomahawk.addUrlResult(url, result);
            } else {
                Tomahawk.log("Could not parse ex.fm URL: " + url);
                Tomahawk.addUrlResult(url, {})
            };
        });
    },

    search: function (qid, searchString) {
        this.settings.strictMatch = false;
        this.resolve(qid, "", "", searchString);
    }
});

Tomahawk.resolver.instance = ExfmResolver;
