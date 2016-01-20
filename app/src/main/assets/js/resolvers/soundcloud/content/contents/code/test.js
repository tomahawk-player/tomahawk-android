var buster = require("buster");
var nock = require("nock");
var utils = require("../../../../test/utils.js");

buster.testCase("soundcloud", {
    setUp: function (done) {
        utils.loadResolver('soundcloud', this, done);
    },

    "test capabilities": function () {
        buster.assert(this.context.hasCapability('urllookup'));
        buster.refute(this.context.hasCapability('playlistsync'));
        buster.refute(this.context.hasCapability('browsable'));
    },

    "test resolving": function (done) {
        this.context.on('track-result', function (qid, result) {
            buster.assert.equals("qid-001", qid);
            buster.assert.equals("Bloc Party", result.artist);
            buster.assert.equals("Ratchet", result.track);
            done();
        });
        // This is not the original reponse but one of the answer slightly adjusted
        nock("https://api.soundcloud.com")
            .get("/tracks.json?consumer_key=TiNg2DRYhBnp01DA3zNag&filter=streamable&q=Bloc%20Party+Ratchet")
            .reply(200, JSON.stringify([
                {
                    "kind": "track",
                    "id": 112482070,
                    "created_at": "2013/09/25 21:51:32 +0000",
                    "user_id": 932254,
                    "duration": 1249970,
                    "commentable": true,
                    "state": "finished",
                    "original_content_size": 49995285,
                    "sharing": "public",
                    "tag_list": "kele okereke bloc party tapes k7 records dj mix cd download",
                    "permalink": "20-min-snippet",
                    "streamable": true,
                    "embeddable_by": "all",
                    "downloadable": false,
                    "purchase_url": null,
                    "label_id": null,
                    "purchase_title": null,
                    "genre": "Dance",
                    "title": "Bloc Party - Ratchet",
                    "description": "<--truncated-->",
                    "label_name": "",
                    "release": "",
                    "track_type": "recording",
                    "key_signature": "",
                    "isrc": "",
                    "video_url": null,
                    "bpm": null,
                    "release_year": null,
                    "release_month": null,
                    "release_day": null,
                    "original_format": "mp3",
                    "license": "all-rights-reserved",
                    "uri": "https://api.soundcloud.com/tracks/112482070",
                    "user": {
                        "id": 932254,
                        "kind": "user",
                        "permalink": "keleokereke",
                        "username": "Kele Okereke",
                        "uri": "https://api.soundcloud.com/users/932254",
                        "permalink_url": "http://soundcloud.com/keleokereke",
                        "avatar_url": "https://i1.sndcdn.com/avatars-000056100835-hnk1ji-large.jpg?30a2558"
                    },
                    "permalink_url": "http://soundcloud.com/keleokereke/20-min-snippet",
                    "artwork_url": "https://i1.sndcdn.com/artworks-000059074336-nf414w-large.jpg?30a2558",
                    "waveform_url": "https://w1.sndcdn.com/nprcFygtaWnU_m.png",
                    "stream_url": "https://api.soundcloud.com/tracks/112482070/stream",
                    "playback_count": 38057,
                    "download_count": 0,
                    "favoritings_count": 679,
                    "comment_count": 39,
                    "attachments_uri": "https://api.soundcloud.com/tracks/112482070/attachments"
                }
            ]));
        this.instance.resolve("qid-001", "Bloc Party", "", "Ratchet");
    },

    "// test search": function () {
    },

    "// test url lookup": function () {
    }
});

