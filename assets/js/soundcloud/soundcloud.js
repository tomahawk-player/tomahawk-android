/*
 * (c) 2012 thierry göckel <thierry@strayrayday.lu>
 */
var SoundcloudResolver = Tomahawk.extend(TomahawkResolver, {
	
	getConfigUi: function () {
		var uiData = Tomahawk.readBase64("config.ui");
		return {
			"widget": uiData,
			fields: [{
				name: "includeCovers",
				widget: "covers",
				property: "checked"
			}, {
				name: "includeRemixes",
				widget: "remixes",
				property: "checked"
			}, {
				name: "includeLive",
				widget: "live",
				property: "checked"
			}],
			images: [{
				"soundcloud.png" : Tomahawk.readBase64("soundcloud.png")
			}]
		};
	},

	newConfigSaved: function () {
		var userConfig = this.getUserConfig();
		if ((userConfig.includeCovers != this.includeCovers) || (userConfig.includeRemixes != this.includeRemixes) || (userConfig.includeLive != this.includeLive)) {
			this.includeCovers = userConfig.includeCovers;
			this.includeRemixes = userConfig.includeRemixes;
			this.includeLive = userConfig.includeLive;
			this.saveUserConfig();
		}
	},
	
	settings: {
		name: 'SoundCloud',
		icon: 'soundcloud-icon.png',
		weight: 85,
		timeout: 15
	},
	
	init: function() {
		// Set userConfig here
		var userConfig = this.getUserConfig();
		if ( userConfig !== undefined ){
			this.includeCovers = userConfig.includeCovers;
			this.includeRemixes = userConfig.includeRemixes;
			this.includeLive = userConfig.includeLive;
		}
		else {
			this.includeCovers = false;
			this.includeRemixes = false;
			this.includeLive = false;
		}
	
	
		String.prototype.capitalize = function(){
			return this.replace( /(^|\s)([a-z])/g , function(m,p1,p2){ return p1+p2.toUpperCase(); } );
		};
	},	

	getTrack: function (trackTitle, origTitle) {
		if ((this.includeCovers === false || this.includeCovers === undefined) && trackTitle.search(/cover/i) !== -1 && origTitle.search(/cover/i) === -1){
			return null;
		}
		if ((this.includeRemixes === false || this.includeRemixes === undefined) && trackTitle.search(/(re)*mix/i) !== -1 && origTitle.search(/(re)*mix/i) === -1){
			return null;
		}
		if ((this.includeLive === false || this.includeLive === undefined) && trackTitle.search(/live/i) !== -1 && origTitle.search(/live/i) === -1){
			return null;
		}
		else {
			return trackTitle;
		}
	},

	resolve: function (qid, artist, album, title)
	{
		if (artist !== "") {
			query = encodeURIComponent(artist) + "+";
		}
		if (title !== "") {
			query += encodeURIComponent(title);
		}
		var apiQuery = "http://api.soundcloud.com/tracks.json?consumer_key=TiNg2DRYhBnp01DA3zNag&filter=streamable&q=" + query;
		var that = this;
		var empty = {
			results: [],
			qid: qid
		};
		Tomahawk.asyncRequest(apiQuery, function (xhr) {
			var resp = JSON.parse(xhr.responseText);
			if (resp.length !== 0){
				var results = [];
				for (i = 0; i < resp.length; i++) {
					// Need some more validation here
					// This doesnt help it seems, or it just throws the error anyhow, and skips?
					if (resp[i] === undefined){
						continue;
					}

					if (!resp[i].streamable){ // Check for streamable tracks only
						continue;
					}

					// Check whether the artist and title (if set) are in the returned title, discard otherwise
					// But also, the artist could be the username
					if (resp[i].title !== undefined && (resp[i].title.toLowerCase().indexOf(artist.toLowerCase()) === -1 || resp[i].title.toLowerCase().indexOf(title.toLowerCase()) === -1)) {
						continue;
					}
					var result = new Object();
					result.artist = artist;
					if (that.getTrack(resp[i].title, title)){
						result.track = title;
					}
					else {
						continue;
					}

					result.source = that.settings.name;
					result.mimetype = "audio/mpeg";
					result.bitrate = 128;
					result.duration = resp[i].duration / 1000;
					result.score = 0.85;
					result.year = resp[i].release_year;
					result.url = resp[i].stream_url + ".json?client_id=TiNg2DRYhBnp01DA3zNag";
					if (resp[i].permalink_url !== undefined) result.linkUrl = resp[i].permalink_url;
					results.push(result);
				}
				var return1 = {
					qid: qid,
					results: [results[0]]
				};
				Tomahawk.addTrackResults(return1);
			}
			else {
				Tomahawk.addTrackResults(empty);
			}
		});
	},
	
	search: function (qid, searchString)
	{
		var apiQuery = "http://api.soundcloud.com/tracks.json?consumer_key=TiNg2DRYhBnp01DA3zNag&filter=streamable&q=" + encodeURIComponent(searchString.replace('"', '').replace("'", ""));
		var that = this;
		var empty = {
			results: [],
			qid: qid
		};
		Tomahawk.asyncRequest(apiQuery, function (xhr) {
			var resp = JSON.parse(xhr.responseText);
			if (resp.length !== 0){
				var results = [];
				var stop = resp.length;
				for (i = 0; i < resp.length; i++) {
					if(resp[i] === undefined){
						stop = stop - 1;
						continue;
					}
					var result = new Object();

					if (that.getTrack(resp[i].title, "")){
						var track = resp[i].title;
						if (track.indexOf(" - ") !== -1 && track.slice(track.indexOf(" - ") + 3).trim() !== ""){
							result.track = track.slice(track.indexOf(" - ") + 3).trim();
							result.artist = track.slice(0, track.indexOf(" - ")).trim();
						}
						else if (track.indexOf(" -") !== -1 && track.slice(track.indexOf(" -") + 2).trim() !== ""){
							result.track = track.slice(track.indexOf(" -") + 2).trim();
							result.artist = track.slice(0, track.indexOf(" -")).trim();
						}
						else if (track.indexOf(": ") !== -1 && track.slice(track.indexOf(": ") + 2).trim() !== ""){
							result.track = track.slice(track.indexOf(": ") + 2).trim();
							result.artist = track.slice(0, track.indexOf(": ")).trim();
						}
						else if (track.indexOf("-") !== -1 && track.slice(track.indexOf("-") + 1).trim() !== ""){
							result.track = track.slice(track.indexOf("-") + 1).trim();
							result.artist = track.slice(0, track.indexOf("-")).trim();
						}
						else if (track.indexOf(":") !== -1 && track.slice(track.indexOf(":") + 1).trim() !== ""){
							result.track = track.slice(track.indexOf(":") + 1).trim();
							result.artist = track.slice(0, track.indexOf(":")).trim();
						}
						else if (track.indexOf("\u2014") !== -1 && track.slice(track.indexOf("\u2014") + 2).trim() !== ""){
							result.track = track.slice(track.indexOf("\u2014") + 2).trim();
							result.artist = track.slice(0, track.indexOf("\u2014")).trim();
						}
						else if (resp[i].title !== "" && resp[i].user.username !== ""){
							// Last resort, the artist is the username
							result.track = resp[i].title;
							result.artist = resp[i].user.username;
						}
						else {
							stop = stop - 1;
							continue;
						}
					}
					else {
						stop = stop - 1;
						continue;
					}

					result.source = that.settings.name;
					result.mimetype = "audio/mpeg";
					result.bitrate = 128;
					result.duration = resp[i].duration / 1000;
					result.score = 0.85;
					result.year = resp[i].release_year;
					result.url = resp[i].stream_url + ".json?client_id=TiNg2DRYhBnp01DA3zNag";
					if (resp[i].permalink_url !== undefined) result.linkUrl = resp[i].permalink_url;
					
					(function (i, result) {
						var artist = encodeURIComponent(result.artist.capitalize());
						var url = "http://developer.echonest.com/api/v4/artist/extract?api_key=JRIHWEP6GPOER2QQ6&format=json&results=1&sort=hotttnesss-desc&text=" + artist;
						var xhr = new XMLHttpRequest();
						xhr.open('GET', url, true);
						xhr.onreadystatechange = function() {
								if (xhr.readyState === 4){
									if (xhr.status === 200) {
										var response = JSON.parse(xhr.responseText).response;
										if (response && response.artists && response.artists.length > 0) {
											artist = response.artists[0].name;
											result.artist = artist;
											result.id = i;
											results.push(result);
											stop = stop - 1;
										}
										else {
											stop = stop - 1;
										}
										if (stop === 0) {
											function sortResults(a, b){
												return a.id - b.id;
											}
											results = results.sort(sortResults);
											for (var j = 0; j < results.length; j++){
												delete results[j].id;
											}
											var toReturn = {
												results: results,
												qid: qid
											};
											Tomahawk.addTrackResults(toReturn);
										}
									}
									else {
										Tomahawk.log("Failed to do GET request to: " + url);
										Tomahawk.log("Error: " + xhr.status + " " + xhr.statusText);
									}
								}
						};
						xhr.send(null);
					})(i, result);	
				}
				if (stop === 0){
					Tomahawk.addTrackResults(empty);
				}
			}
			else {
				Tomahawk.addTrackResults(empty);
			}
		});
	}
});

Tomahawk.resolver.instance = SoundcloudResolver;
