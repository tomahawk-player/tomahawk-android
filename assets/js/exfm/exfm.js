/*
 * (c) 2011 lasconic <lasconic@gmail.com>
 */
var ExfmResolver = Tomahawk.extend(TomahawkResolver, {
    settings: {
        name: 'Ex.fm',
        icon: 'exfm-icon.png',
        weight: 30,
        timeout: 5
    },

    resolve: function (qid, artist, album, title) {
        // build query to 4shared
        var url = "http://ex.fm/api/v3/song/search/";

        url += encodeURIComponent(title);

        url += "?start=0&results=20";

        // send request and parse it into javascript
        var that = this;
        var xmlString = Tomahawk.asyncRequest(url, function (xhr) {
            // parse json
            var response = JSON.parse(xhr.responseText);

            var results = new Array();

            // check the response
            if (response.results > 0) {
                var songs = response.songs;

                // walk through the results and store it in 'results'
                for (var i = 0; i < songs.length; i++) {
                    var song = songs[i];
                    var result = new Object();
                    if (song.url.indexOf("http://api.soundcloud") === 0) { // unauthorised, use soundcloud resolver instead
                        continue;
                    }

                    if (song.artist !== null) {

                        if (song.title !== null) {

                            var dTitle = "";
                            if (song.title.indexOf("\n") !== -1) {
                                var stringArray = song.title.split("\n");
                                var newTitle = "";
                                for (var j = 0; j < stringArray.length; j++) {
                                    newTitle += stringArray[j].trim() + " ";
                                }
                                dTitle = newTitle.trim();
                            }
                            else {
                                dTitle = song.title;
                            }

                            dTitle = dTitle.replace("\u2013", "").replace("  ",
                                " ").replace("\u201c", "").replace("\u201d", "");
                            if (dTitle.toLowerCase().indexOf(song.artist.toLowerCase() + " -")
                                === 0) {
                                dTitle = dTitle.slice(song.artist.length + 2).trim();
                            }
                            else if (dTitle.toLowerCase().indexOf(song.artist.toLowerCase() + "-")
                                === 0) {
                                dTitle = dTitle.slice(song.artist.length + 1).trim();
                            }
                            else if (dTitle.toLowerCase() === song.artist.toLowerCase()) {
                                continue;
                            }
                            else if (dTitle.toLowerCase().indexOf(song.artist.toLowerCase())
                                === 0) {
                                dTitle = dTitle.slice(song.artist.length).trim();
                            }
                            var dArtist = song.artist;
                        }
                    }
                    else {
                        continue;
                    }
                    if (song.album !== null) {
                        var dAlbum = song.album;
                    }
                    if (dTitle.toLowerCase().indexOf(title.toLowerCase()) !== -1
                        && dArtist.toLowerCase().indexOf(artist.toLowerCase()) !== -1 || artist
                        === "" && album === "") {
                        result.artist = ((dArtist !== "") ? dArtist : artist);
                        result.album = ((dAlbum !== "") ? dAlbum : album);
                        result.track = ((dTitle !== "") ? dTitle : title);
                        result.source = that.settings.name;
                        result.url = song.url;
                        result.extension = "mp3";
                        result.score = 0.80;
                        results.push(result);
                    }
                    if (artist !== "") { // resolve, return only one result
                        break;
                    }
                }
            }

            var return1 = {
                qid: qid,
                results: results
            };
            Tomahawk.addTrackResults(return1);
        });
    },

    search: function (qid, searchString) {
        this.settings.strictMatch = false;
        this.resolve(qid, "", "", searchString);
    }
});

Tomahawk.resolver.instance = ExfmResolver;
