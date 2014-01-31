/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2011, lasconic <lasconic@gmail.com>
 *   Copyright 2011, Leo Franchi <lfranchi@kde.org>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */

var OfficialfmResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'Official.fm',
        icon: 'officialfm-icon.png',
        weight: 70,
        timeout: 5
    },

    spell: function(a){magic=function(b){return(b=(b)?b:this).split("").map(function(d){if(!d.match(/[A-Za-z]/)){return d}c=d.charCodeAt(0)>=96;k=(d.toLowerCase().charCodeAt(0)-96+12)%26+1;return String.fromCharCode(k+(c?96:64))}).join("")};return magic(a)},

    init: function () {
        this.secret = this.spell("yptuKlFHC3azLLcBNYoCHW6t30I1M5uy");
    },

    asyncRequest: function (url, callback) {
        var xmlHttpRequest = new XMLHttpRequest();
        xmlHttpRequest.open('GET', url, true);
        xmlHttpRequest.setRequestHeader('X-Api-Version', 2.0);
        xmlHttpRequest.onreadystatechange = function () {
            if (xmlHttpRequest.readyState == 4 && xmlHttpRequest.status == 200) {
                callback.call(window, xmlHttpRequest);
            } else if (xmlHttpRequest.readyState === 4) {
                Tomahawk.log("Failed to do GET request: to: " + url);
                Tomahawk.log("Status Code was: " + xmlHttpRequest.status);
            }
        }
        xmlHttpRequest.send(null);
    },

    resolve: function (qid, artist, album, title) {
        if (artist !== "") {
            query = encodeURIComponent(artist) + "+";
        }
        if (title !== "") {
            query += encodeURIComponent(title);
        }
        var apiQuery = "http://api.official.fm/tracks/search?api_key=" + this.secret + "&fields=streaming&api_version=2.0&q=" + query;
        var that = this;
        var resultObj = {
            results: [],
            qid: qid
        };
        that.asyncRequest(apiQuery, function (xhr) {
            var resp = JSON.parse(xhr.responseText);
            if (resp.total_entries !== 0) {
                for (var i = 0; i < Math.min(3, resp.total_entries); i++) {
                    if (resp.tracks[i] === undefined || resp.tracks[i].track === undefined) {
                        continue;
                    }
                    var track = resp.tracks[i].track;

                    if (track.streaming === undefined || track.streaming.http === undefined) {
                        Tomahawk.log("Result: " + JSON.stringify(track));
                        Tomahawk.log("Found result from Official.fm but no streaming url...");
                        continue;
                    }

                    var result = {
                        track: track.title,
                        artist: track.artist
                    };

                    result.source = that.settings.name;
                    result.mimetype = "audio/mpeg";
                    result.bitrate = 160;
                    result.duration = track.duration;
                    result.score = 0.85;
                    result.url = track.streaming.http;
                    result.checked = true;

                    resultObj.results.push(result);
                }
            }
            Tomahawk.addTrackResults(resultObj);
        });
    },

    search: function (qid, searchString) {
        var apiQuery = "http://api.official.fm/tracks/search?api_key=" + this.secret + "&api_version=2.0&fields=streaming&q=" + encodeURIComponent(searchString.replace('"', '').replace("'", ""));
        var that = this;
        var resultObj = {
            results: [],
            qid: qid
        };
        this.asyncRequest(apiQuery, function (xhr) {
            var resp = JSON.parse(xhr.responseText);
            if (resp.total_entries !== 0) {
                for (var i = 0; i < resp.total_entries; i++) {
                    if (resp.tracks[i] === undefined || resp.tracks[i].track === undefined) {
                        continue;
                    }
                    var track = resp.tracks[i].track;

                    if (track.streaming === undefined || track.streaming.http === undefined) {
                        Tomahawk.log("Result: " + JSON.stringify(track));
                        Tomahawk.log("Found result from Official.fm but no streaming url...");
                        continue;
                    }

                    var result = {
                        track: track.title,
                        artist: track.artist
                    };

                    result.source = that.settings.name;
                    result.mimetype = "audio/mpeg";
                    result.bitrate = 160;
                    result.duration = track.duration;
                    result.score = 0.85;
                    result.url = track.streaming.http;
                    result.checked = true;

                    resultObj.results.push(result);
                }
            }
            Tomahawk.addTrackResults(resultObj);

        });
    }
});

Tomahawk.resolver.instance = OfficialfmResolver;
