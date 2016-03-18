/* === This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2016, Enno Gottschalk <mrmaffen@googlemail.com>
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

Tomahawk.PluginManager.registerPlugin('chartsProvider', {

    _baseUrl: "https://itunes.apple.com/",

    countryCodes: {
        defaultCode: "us",
        codes: [
            {"United States": "us"},
            {"United Kingdom": "gb"},
            {"Andorra": "ad"},
            {"Antigua and Barbuda": "ag"},
            {"Argentina": "ar"},
            {"Armenia": "am"},
            {"Australia": "au"},
            {"Austria": "at"},
            {"Azerbaijan": "az"},
            {"Bahamas": "bs"},
            {"Bahrain": "bh"},
            {"Bangladesh": "bd"},
            {"Barbados": "bb"},
            {"Belarus": "by"},
            {"Belgium": "be"},
            {"Belize": "bz"},
            {"Bermuda": "bm"},
            {"Bolivia": "bo"},
            {"Bosnia and Herzegovina": "ba"},
            {"Botswana": "bw"},
            {"Brazil": "br"},
            {"Brunei Darussalam": "bn"},
            {"Bulgaria": "bg"},
            {"Burkina Faso": "bf"},
            {"Burundi": "bi"},
            {"Cambodia": "kh"},
            {"Cameroon": "cm"},
            {"Canada": "ca"},
            {"Cape Verde": "cv"},
            {"Cayman Islands": "ky"},
            {"Central African Republic": "cf"},
            {"Chile": "cl"},
            {"Colombia": "co"},
            {"Comoros": "km"},
            {"Costa Rica": "cr"},
            {"Cuba": "cu"},
            {"Cyprus": "cy"},
            {"Czech": "cz"},
            {"Côte d’Ivoire": "ci"},
            {"Democratic Republic of the Congo": "cd"},
            {"Denmark": "dk"},
            {"Djibouti": "dj"},
            {"Dominica": "dm"},
            {"Dominican Republic": "do"},
            {"Ecuador": "ec"},
            {"Egypt": "eg"},
            {"El Salvador": "sv"},
            {"Equatorial Guinea": "gq"},
            {"Eritrea": "er"},
            {"Estonia": "ee"},
            {"Ethiopia": "et"},
            {"Falkland Islands": "fk"},
            {"Faroe Islands": "fo"},
            {"Fiji": "fj"},
            {"Finland": "fi"},
            {"France": "fr"},
            {"French Polynesia": "pf"},
            {"Gabon": "ga"},
            {"Gambia": "gm"},
            {"Georgia": "ge"},
            {"Germany": "de"},
            {"Greece": "gr"},
            {"Greenland": "gl"},
            {"Grenada": "gd"},
            {"Guatemala": "gt"},
            {"Guinea": "gn"},
            {"Guinea-Bissau": "gw"},
            {"Haiti": "ht"},
            {"Honduras": "hn"},
            {"Hong Kong": "hk"},
            {"Hungary": "hu"},
            {"India": "in"},
            {"Indonesia": "id"},
            {"Iran": "ir"},
            {"Iraq": "iq"},
            {"Ireland": "ie"},
            {"Isle of Man": "im"},
            {"Israel": "il"},
            {"Italy": "it"},
            {"Japan": "jp"},
            {"Jordan": "jo"},
            {"Kazakhstan": "kz"},
            {"Kenya": "ke"},
            {"Kiribati": "ki"},
            {"Kyrgyzstan": "kg"},
            {"Lao People’s Democratic Republic": "la"},
            {"Latvia": "lv"},
            {"Lebanon": "lb"},
            {"Lesotho": "ls"},
            {"Libyan Jamahiriya": "ly"},
            {"Liechtenstein": "li"},
            {"Lithuania": "lt"},
            {"Luxembourg": "lu"},
            {"Macao": "mo"},
            {"Malaysia": "my"},
            {"Maldives": "mv"},
            {"Malta": "mt"},
            {"Mauritius": "mu"},
            {"Mexico": "mx"},
            {"Moldova": "md"},
            {"Monaco": "mc"},
            {"Mongolia": "mn"},
            {"Montenegro": "me"},
            {"Morocco": "ma"},
            {"Mozambique": "mz"},
            {"Myanmar": "mm"},
            {"Namibia": "na"},
            {"Nauru": "nr"},
            {"Nepal": "np"},
            {"Netherlands": "nl"},
            {"New Caledonia": "nc"},
            {"New Zealand": "nz"},
            {"Nicaragua": "ni"},
            {"Niger": "ne"},
            {"Nigeria": "ng"},
            {"North Korea": "kp"},
            {"Norway": "no"},
            {"Oman": "om"},
            {"Panama": "pa"},
            {"Papua New Guinea": "pg"},
            {"Paraguay": "py"},
            {"Peru": "pe"},
            {"Philippines": "ph"},
            {"Poland": "pl"},
            {"Portugal": "pt"},
            {"Qatar": "qa"},
            {"Romania": "ro"},
            {"Russian Federation": "ru"},
            {"Rwanda": "rw"},
            {"Saint Helena": "sh"},
            {"Samoa": "ws"},
            {"Saudi Arabia": "sa"},
            {"Serbia and Montenegro": "yu"},
            {"Serbia": "rs"},
            {"Singapore": "sg"},
            {"Slovakia": "sk"},
            {"Slovenia": "si"},
            {"Somalia": "so"},
            {"South Africa": "za"},
            {"South Georgia and South Sandwich Islands": "gs"},
            {"Spain": "es"},
            {"Sri Lanka": "lk"},
            {"Sudan": "sd"},
            {"Swaziland": "sz"},
            {"Sweden": "se"},
            {"Switzerland": "ch"},
            {"Syrian Arab Republic": "sy"},
            {"Taiwan": "tw"},
            {"Tajikistan": "tj"},
            {"Thailand": "th"},
            {"Timor-Leste": "tl"},
            {"Togo": "tg"},
            {"Tonga": "to"},
            {"Trinidad and Tobago": "tt"},
            {"Turkey": "tr"},
            {"Turkmenistan": "tm"},
            {"Tuvalu": "tv"},
            {"Uganda": "ug"},
            {"Ukraine": "ua"},
            {"United Arab Emirates": "ae"},
            {"Uzbekistan": "uz"},
            {"Vanuatu": "vu"},
            {"Vatican": "va"},
            {"Venezuela": "ve"},
            {"Viet Nam": "vn"},
            {"Zambia": "zm"},
            {"Zimbabwe": "zw"}
        ]
    },

    types: [
        {"Songs": "topsongs"},
        {"Albums": "topalbums"}
    ],

    /**
     * Get the charts from the server specified by the given params map and parse them into the
     * correct result format.
     *
     * @param params A map containing all of the necessary parameters describing the charts which to
     *               get from the server.
     *
     *               Example:
     *               { countryCode: "us",                //country code from the countryCodes map
     *                 type: "topsongs" }                //type from the types map
     *
     * @returns A map consisting of the contentType and parsed results.
     *
     *          Example:
     *          { contentType: Tomahawk.UrlType.Track,
     *            results: [
     *              { track: "We will rock you",
     *                artist: "Queen",
     *                album: "Greatest Hits" },
     *              { track: "Bohemian rhapsody",
     *                artist: "Queen",
     *                album: "Greatest Hits" }
     *            ]
     *          }
     *
     */
    charts: function (params) {
        var url = this._baseUrl + params.countryCode + "/rss/" + params.type
            + "/limit=100/explicit=true/json";
        return Tomahawk.get(url).then(function (response) {
            var results = JSON.parse(response);
            var firstITunesContentType =
                results.feed.entry[0]["im:contentType"]["im:contentType"].attributes.term;
            var contentType;
            if (firstITunesContentType == "Track") {
                contentType = Tomahawk.UrlType.Track;
            } else if (firstITunesContentType == "Album") {
                contentType = Tomahawk.UrlType.Album;
            } else {
                throw new Error("Unsupported contentType!");
            }
            var parsedResults = [];
            for (var i = 0; i < results.feed.entry.length; i++) {
                var entry = results.feed.entry[i];
                var iTunesContentType = entry["im:contentType"]["im:contentType"].attributes.term;
                if (iTunesContentType != firstITunesContentType) {
                    throw new Error("Two different contentTypes in one chart!");
                }
                if (contentType == Tomahawk.UrlType.Track) {
                    parsedResults.push({
                        track: entry["im:name"].label,
                        artist: entry["im:artist"].label,
                        album: entry["im:collection"]["im:name"].label
                    });
                } else if (contentType == Tomahawk.UrlType.Album) {
                    parsedResults.push({
                        album: entry["im:name"].label,
                        artist: entry["im:artist"].label
                    });
                }
            }
            return {
                contentType: contentType,
                results: parsedResults
            };
        });
    }

});