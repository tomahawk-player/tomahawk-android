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
            {"United Arab Emirates": "ae"},
            {"Antigua and Barbuda": "ag"},
            {"Armenia": "am"},
            {"Argentina": "ar"},
            {"Austria": "at"},
            {"Australia": "au"},
            {"Azerbaijan": "az"},
            {"Bosnia and Herzegovina": "ba"},
            {"Barbados": "bb"},
            {"Bangladesh": "bd"},
            {"Belgium": "be"},
            {"Burkina Faso": "bf"},
            {"Bulgaria": "bg"},
            {"Bahrain": "bh"},
            {"Burundi": "bi"},
            {"Bermuda": "bm"},
            {"Brunei Darussalam": "bn"},
            {"Bolivia": "bo"},
            {"Brazil": "br"},
            {"Bahamas": "bs"},
            {"Botswana": "bw"},
            {"Belarus": "by"},
            {"Belize": "bz"},
            {"Canada": "ca"},
            {"Democratic Republic of the Congo": "cd"},
            {"Central African Republic": "cf"},
            {"Switzerland": "ch"},
            {"Côte d’Ivoire": "ci"},
            {"Chile": "cl"},
            {"Cameroon": "cm"},
            {"Colombia": "co"},
            {"Costa Rica": "cr"},
            {"Cuba": "cu"},
            {"Cape Verde": "cv"},
            {"Cyprus": "cy"},
            {"Czech": "cz"},
            {"Germany": "de"},
            {"Djibouti": "dj"},
            {"Denmark": "dk"},
            {"Dominica": "dm"},
            {"Dominican Republic": "do"},
            {"Ecuador": "ec"},
            {"Estonia": "ee"},
            {"Egypt": "eg"},
            {"Eritrea": "er"},
            {"Spain": "es"},
            {"Ethiopia": "et"},
            {"Finland": "fi"},
            {"Fiji": "fj"},
            {"Falkland Islands": "fk"},
            {"Faroe Islands": "fo"},
            {"France": "fr"},
            {"Gabon": "ga"},
            {"Grenada": "gd"},
            {"Georgia": "ge"},
            {"Greenland": "gl"},
            {"Gambia": "gm"},
            {"Guinea": "gn"},
            {"Equatorial Guinea": "gq"},
            {"Greece": "gr"},
            {"South Georgia and South Sandwich Islands": "gs"},
            {"Guatemala": "gt"},
            {"Guinea-Bissau": "gw"},
            {"Hong Kong": "hk"},
            {"Honduras": "hn"},
            {"Haiti": "ht"},
            {"Hungary": "hu"},
            {"Indonesia": "id"},
            {"Ireland": "ie"},
            {"Israel": "il"},
            {"Isle of Man": "im"},
            {"India": "in"},
            {"Iraq": "iq"},
            {"Iran": "ir"},
            {"Italy": "it"},
            {"Jordan": "jo"},
            {"Japan": "jp"},
            {"Kenya": "ke"},
            {"Kyrgyzstan": "kg"},
            {"Cambodia": "kh"},
            {"Kiribati": "ki"},
            {"Comoros": "km"},
            {"North Korea": "kp"},
            {"Cayman Islands": "ky"},
            {"Kazakhstan": "kz"},
            {"Lao People’s Democratic Republic": "la"},
            {"Lebanon": "lb"},
            {"Liechtenstein": "li"},
            {"Sri Lanka": "lk"},
            {"Lesotho": "ls"},
            {"Lithuania": "lt"},
            {"Luxembourg": "lu"},
            {"Latvia": "lv"},
            {"Libyan Jamahiriya": "ly"},
            {"Morocco": "ma"},
            {"Monaco": "mc"},
            {"Moldova": "md"},
            {"Montenegro": "me"},
            {"Myanmar": "mm"},
            {"Mongolia": "mn"},
            {"Macao": "mo"},
            {"Malta": "mt"},
            {"Mauritius": "mu"},
            {"Maldives": "mv"},
            {"Mexico": "mx"},
            {"Malaysia": "my"},
            {"Mozambique": "mz"},
            {"Namibia": "na"},
            {"New Caledonia": "nc"},
            {"Niger": "ne"},
            {"Nigeria": "ng"},
            {"Nicaragua": "ni"},
            {"Netherlands": "nl"},
            {"Norway": "no"},
            {"Nepal": "np"},
            {"Nauru": "nr"},
            {"New Zealand": "nz"},
            {"Oman": "om"},
            {"Panama": "pa"},
            {"Peru": "pe"},
            {"French Polynesia": "pf"},
            {"Papua New Guinea": "pg"},
            {"Philippines": "ph"},
            {"Poland": "pl"},
            {"Portugal": "pt"},
            {"Paraguay": "py"},
            {"Qatar": "qa"},
            {"Romania": "ro"},
            {"Serbia": "rs"},
            {"Russian Federation": "ru"},
            {"Rwanda": "rw"},
            {"Saudi Arabia": "sa"},
            {"Sudan": "sd"},
            {"Sweden": "se"},
            {"Singapore": "sg"},
            {"Saint Helena": "sh"},
            {"Slovenia": "si"},
            {"Slovakia": "sk"},
            {"Somalia": "so"},
            {"El Salvador": "sv"},
            {"Syrian Arab Republic": "sy"},
            {"Swaziland": "sz"},
            {"Togo": "tg"},
            {"Thailand": "th"},
            {"Tajikistan": "tj"},
            {"Timor-Leste": "tl"},
            {"Turkmenistan": "tm"},
            {"Tonga": "to"},
            {"Turkey": "tr"},
            {"Trinidad and Tobago": "tt"},
            {"Tuvalu": "tv"},
            {"Taiwan": "tw"},
            {"Ukraine": "ua"},
            {"Uganda": "ug"},
            {"Uzbekistan": "uz"},
            {"Vatican": "va"},
            {"Venezuela": "ve"},
            {"Viet Nam": "vn"},
            {"Vanuatu": "vu"},
            {"Samoa": "ws"},
            {"Serbia and Montenegro": "yu"},
            {"South Africa": "za"},
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