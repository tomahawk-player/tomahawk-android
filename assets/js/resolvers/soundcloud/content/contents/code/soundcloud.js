/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2012, Thierry Göckel <thierry@strayrayday.lu>
 *   Copyright 2013, Uwe L. Korn <uwelk@xhochy.com>
 *   Copyright 2015, Enno Gottschalk <mrmaffen@googlemail.com>
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

var SoundcloudResolver = Tomahawk.extend(Tomahawk.Resolver, {

    apiVersion: 0.9,

    soundcloudClientId: "TiNg2DRYhBnp01DA3zNag",

    echonestClientId: "JRIHWEP6GPOER2QQ6",

    baseUrl: "https://api.soundcloud.com/",

    settings: {
        name: 'SoundCloud',
        icon: 'soundcloud-icon.png',
        weight: 85,
        timeout: 15
    },

    getConfigUi: function () {
        var uiData = Tomahawk.readBase64("config.ui");
        return {
            "widget": uiData,
            fields: [
                {
                    name: "includeCovers",
                    widget: "covers",
                    property: "checked"
                },
                {
                    name: "includeRemixes",
                    widget: "remixes",
                    property: "checked"
                },
                {
                    name: "includeLive",
                    widget: "live",
                    property: "checked"
                }
            ],
            images: [
                {
                    "soundcloud.png": Tomahawk.readBase64("soundcloud.png")
                }
            ]
        };
    },

    newConfigSaved: function (newConfig) {
        this.includeCovers = newConfig.includeCovers;
        this.includeRemixes = newConfig.includeRemixes;
        this.includeLive = newConfig.includeLive;
    },

    /**
     * Initialize the Soundcloud resolver.
     */
    init: function () {
        // Set userConfig here
        var userConfig = this.getUserConfig();
        if (userConfig) {
            this.includeCovers = userConfig.includeCovers;
            this.includeRemixes = userConfig.includeRemixes;
            this.includeLive = userConfig.includeLive;
        } else {
            this.includeCovers = false;
            this.includeRemixes = false;
            this.includeLive = false;
        }

        Tomahawk.reportCapabilities(TomahawkResolverCapability.UrlLookup);
    },

    _isValidTrack: function (trackTitle, origTitle) {
        if (!this.includeCovers &&
            trackTitle.search(/cover/i) >= 0 &&
            origTitle.search(/cover/i) < 0) {
            return false;
        }
        if (!this.includeRemixes &&
            trackTitle.search(/mix/i) >= 0 &&
            origTitle.search(/mix/i) < 0) {
            return false;
        }
        if (!this.includeLive &&
            trackTitle.search(/live/i) >= 0 &&
            origTitle.search(/live/i) < 0) {
            return false;
        }
        return true;
    },

    resolve: function (params) {
        var artist = params.artist;
        var album = params.album;
        var track = params.track;

        var that = this;

        var url = this.baseUrl + "tracks.json";
        var settings = {
            data: {
                consumer_key: this.soundcloudClientId,
                filter: "streamable",
                limit: 20,
                q: [artist, track].join(" ")
            }
        };
        return Tomahawk.get(url, settings).then(function (response) {
            var results = [];
            for (var i = 0; i < response.length; i++) {
                // Check if the title-string contains the track name we are looking for. Also check
                // if the artist name can be found in either the title-string or the username. Last
                // but not least we make sure that we only include covers/remixes and live versions
                // if the user wants us to.
                if (!response[i] || !response[i].title
                    || (response[i].title.toLowerCase().indexOf(artist.toLowerCase()) < 0
                    && response[i].user.username.toLowerCase().indexOf(artist.toLowerCase()) < 0)
                    || response[i].title.toLowerCase().indexOf(track.toLowerCase()) < 0
                    || !that._isValidTrack(response[i].title, track)) {
                    continue;
                }

                var guessedMetaData = that._guessMetaData(response[i].title);
                var title = guessedMetaData ? guessedMetaData.track : response[i].title;

                var result = {
                    track: title,
                    artist: artist,
                    bitrate: 128,
                    mimetype: "audio/mpeg",
                    source: that.settings.name,
                    duration: response[i].duration / 1000,
                    year: response[i].release_year,
                    url: response[i].stream_url + ".json?client_id=" + that.soundcloudClientId
                };
                if (response[i].permalink_url) {
                    result.linkUrl = response[i].permalink_url;
                }
                results.push(result);
            }
            return results;
        });
    },

    _guessMetaData: function (title) {
        var matches = title.match(/\s*(.+?)\s*(?:\s[-\u2014]|\s["']|:)\s*["']?(.+?)["']?\s*$/);
        if (matches && matches.length > 2) {
            return {
                track: matches[2],
                artist: matches[1]
            };
        }
        matches = title.match(/\s*(.+?)\s*[-\u2014]+\s*(.+?)\s*$/);
        if (matches && matches.length > 2) {
            return {
                track: matches[2],
                artist: matches[1]
            };
        }
    },

    search: function (params) {
        var query = params.query;

        var that = this;

        var url = this.baseUrl + "tracks.json";
        var settings = {
            data: {
                consumer_key: this.soundcloudClientId,
                filter: "streamable",
                limit: 50,
                q: query.replace("'", "")
            }
        };
        return Tomahawk.get(url, settings).then(function (response) {
            var promises = [];
            var results = [];
            for (var i = 0; i < response.length; i++) {
                // Make sure that we only include covers/remixes and live versions if the user wants
                // us to.
                if (!response[i] || !response[i].title
                    || !that._isValidTrack(response[i].title, "")) {
                    continue;
                }

                var candidate = {
                    mimetype: "audio/mpeg",
                    bitrate: 128,
                    duration: response[i].duration / 1000,
                    year: response[i].release_year,
                    url: response[i].stream_url + ".json?client_id=" + that.soundcloudClientId
                };
                if (response[i].permalink_url) {
                    candidate.linkUrl = response[i].permalink_url;
                }

                var guessedMetaData = that._guessMetaData(response[i].title);
                if (guessedMetaData) {
                    candidate.track = guessedMetaData.track;
                    candidate.artist = guessedMetaData.artist;

                    // We guessed the track and artist name of the track. Now we need to make sure
                    // that they are not accidentally interchanged.
                    var url = "https://developer.echonest.com/api/v4/artist/extract";
                    var settingsArtist = {
                        data: {
                            api_key: that.echonestClientId,
                            format: "json",
                            results: 1,
                            bucket: ["hotttnesss", "familiarity"],
                            text: candidate.artist
                        }
                    };
                    var settingsTrack = {
                        data: {
                            api_key: that.echonestClientId,
                            format: "json",
                            results: 1,
                            bucket: ["hotttnesss", "familiarity"],
                            text: candidate.track
                        }
                    };
                    (function (candidate) {
                        promises.push(RSVP.all([
                            Tomahawk.get(url, settingsArtist),
                            Tomahawk.get(url, settingsTrack)
                        ]).then(function (responses) {
                            // We have the results from Echonest and can now determine whether the
                            // assumed track name is more likely to be the artist name. If that's
                            // the case we simply swap them and voila.
                            var scoreArtist = 0;
                            var scoreTrack = 0;
                            if (responses[0] && responses[0].response.artists
                                && responses[0].response.artists.length > 0) {
                                scoreArtist = responses[0].response.artists[0].hotttnesss
                                    + responses[0].response.artists[0].familiarity;
                            }
                            if (responses[1] && responses[1].response.artists
                                && responses[1].response.artists.length > 0) {
                                scoreTrack = responses[1].response.artists[0].hotttnesss
                                    + responses[1].response.artists[0].familiarity;
                            }
                            if (scoreTrack > scoreArtist) {
                                var track = candidate.track;
                                candidate.track = candidate.artist;
                                candidate.artist = track;
                            }
                            return candidate;
                        }));
                    })(candidate);
                } else if (response[i].user.username) {
                    // We weren't able to guess the artist and track name, so we assume the username
                    // as the artist name. No further check with Echonest needed since it's very
                    // unlikely that the username actually is the name of the track and not of the
                    // artist.
                    candidate.track = response[i].title;
                    candidate.artist = response[i].user.username;
                    results.push(candidate);
                }
            }
            return RSVP.allSettled(promises).then(function (responses) {
                for (var i = 0; i < responses.length; i++) {
                    if (responses[i].state == 'fulfilled') {
                        results.push(responses[i].value);
                    }
                }
                return results;
            });
        });
    },

    canParseUrl: function (params) {
        var url = params.url;
        var type = params.type;
        // Soundcloud only returns tracks and playlists
        switch (type) {
            case TomahawkUrlType.Album:
                return false;
            case TomahawkUrlType.Artist:
                return false;
            default:
                return (/https?:\/\/(www\.)?soundcloud.com\//).test(url);
        }
    },

    _convertTrack: function (track) {
        var result = {
            type: Tomahawk.UrlType.Track,
            track: track.title,
            artist: track.user.username
        };

        if (!(track.stream_url === null || typeof track.stream_url === "undefined")) {
            result.hint = track.stream_url + "?client_id=" + this.soundcloudClientId;
        }
        return result;
    },

    lookupUrl: function (params) {
        var url = params.url;

        var that = this;

        var queryUrl = this.baseUrl + "resolve.json";
        var settings = {
            data: {
                client_id: this.soundcloudClientId,
                url: url.replace(/\/likes$/, '')
            }
        };
        return Tomahawk.get(queryUrl, settings).then(function (response) {
            if (response.kind == "playlist") {
                var result = {
                    type: Tomahawk.UrlType.Playlist,
                    title: response.title,
                    guid: 'soundcloud-playlist-' + response.id.toString(),
                    info: response.description,
                    creator: response.user.username,
                    linkUrl: response.permalink_url,
                    tracks: []
                };
                response.tracks.forEach(function (item) {
                    result.tracks.push(that._convertTrack(item));
                });
                return result;
            } else if (response.kind == "track") {
                return that._convertTrack(response);
            } else if (response.kind == "user") {
                var url2 = response.uri;
                var prefix = 'soundcloud-';
                var title = response.full_name + "'s ";
                if (url.indexOf("/likes") === -1) {
                    url2 += "/tracks.json?client_id=" + that.soundcloudClientId;
                    prefix += 'user-';
                    title += "Tracks";
                } else {
                    url2 += "/favorites.json?client_id=" + that.soundcloudClientId;
                    prefix += 'favortites-';
                    title += "Favorites";
                }
                return Tomahawk.get(url2).then(function (response) {
                    var result = {
                        type: Tomahawk.UrlType.Playlist,
                        title: title,
                        guid: prefix + response.id.toString(),
                        info: title,
                        creator: response.username,
                        linkUrl: response.permalink_url,
                        tracks: []
                    };
                    response.forEach(function (item) {
                        result.tracks.push(that._convertTrack(item));
                    });
                    return result;
                });
            } else {
                Tomahawk.log("Could not parse SoundCloud URL: " + url);
                throw new Error("Could not parse SoundCloud URL: " + url);
            }
        });
    }
});

Tomahawk.resolver.instance = SoundcloudResolver;
