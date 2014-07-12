package org.tomahawk.libtomahawk.utils;


import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TomahawkSearchQueryStringParser {

    public static final String ARTIST_REGEX = "@artist" + Pattern.quote("(") + "(.*)" + Pattern.quote(")");
    public static final Pattern ARTIST_PATTERN = Pattern.compile(ARTIST_REGEX);

    public static SearchQuery parse(String searchQueryString) {
        SearchQuery query = new SearchQuery(searchQueryString);

        Matcher artistMatcher = ARTIST_PATTERN.matcher(searchQueryString);
        if (artistMatcher.matches()) {
            query.setArtist(artistMatcher.group(1));
        }

        return query;
    }

    public static class SearchQuery {

        protected String completeQueryString;
        protected String artist;

        public SearchQuery(String completeQueryString) {
            this.completeQueryString = completeQueryString;
        }

        protected void setArtist(String artist) {
            this.artist = artist;
        }

        public String getCompleteQueryString() {
            return completeQueryString;
        }

        public String getArtist() {
            return artist;
        }

        @Override
        public String toString() {
            return this.completeQueryString;
        }

        /**
         * Removes all Tags (e.g. @artist(), etc...) from the searchquery
         * @return
         */
        public String getUntagedQueryString() {
            return this.completeQueryString.replaceAll(ARTIST_REGEX, "");
        }
    }
}
