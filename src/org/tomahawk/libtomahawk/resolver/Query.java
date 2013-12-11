/* == This file is part of Tomahawk Player - <http://tomahawk-player.org> ===
 *
 *   Copyright 2013, Enno Gottschalk <mrmaffen@googlemail.com>
 *
 *   Tomahawk is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   Tomahawk is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with Tomahawk. If not, see <http://www.gnu.org/licenses/>.
 */
package org.tomahawk.libtomahawk.resolver;

import org.tomahawk.libtomahawk.collection.Album;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.adapters.TomahawkBaseAdapter;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com> Date: 18.01.13
 *
 * This class represents a query which is passed to a resolver. It contains all the information
 * needed to enable the Resolver to resolve the results.
 */
public class Query implements TomahawkBaseAdapter.TomahawkListItem {

    public static final String TAG = Query.class.getName();

    private ConcurrentHashMap<String, Result> mTrackResults
            = new ConcurrentHashMap<String, Result>();

    private ConcurrentHashMap<String, Result> mAlbumResults
            = new ConcurrentHashMap<String, Result>();

    private ConcurrentHashMap<String, Result> mArtistResults
            = new ConcurrentHashMap<String, Result>();

    private Track mTrack;

    private boolean mPlayable = false;

    private boolean mSolved = false;

    private int mTrackResultHint = 0;

    private String mQid;

    private String mFullTextQuery;

    private boolean mIsFullTextQuery;

    private boolean mIsOnlyLocal;

    /**
     * Constructs a new Query with the given QueryID. ID should be generated in TomahawkApp.
     */
    private Query() {
        mQid = TomahawkApp.getUniqueStringId();
    }

    /**
     * Constructs a new Query with the given QueryID and a fullTextQuery String. ID should be
     * generated in TomahawkApp.
     */
    public Query(String fullTextQuery, boolean onlyLocal) {
        this();
        mFullTextQuery = fullTextQuery.replace("'", "\\'");
        mIsFullTextQuery = true;
        mIsOnlyLocal = onlyLocal;
    }

    public Query(String trackName, String albumName,
            String artistName, boolean onlyLocal) {
        this();
        Artist artist = Artist.get(artistName.replace("'", "\\'"));
        Album album = Album.get(albumName.replace("'", "\\'"), artist);
        mTrack = Track.get(trackName.replace("'", "\\'"), album, artist);
        mIsFullTextQuery = false;
        mIsOnlyLocal = onlyLocal;
    }

    private Query(Track track, boolean onlyLocal) {
        this(track.getName(), track.getAlbum().getName(), track.getArtist().getName(), onlyLocal);
    }

    public Query(Result result, boolean onlyLocal) {
        this(result.getTrack().getName(), result.getTrack().getAlbum().getName(),
                result.getTrack().getArtist().getName(), onlyLocal);
        addTrackResult(result);
    }

    /**
     * @return An ArrayList<Result> which contains all tracks in the resultList, sorted by score.
     * Given as Results.
     */
    public ArrayList<Result> getTrackResults() {
        ArrayList<Result> results = new ArrayList<Result>(mTrackResults.values());
        Collections.sort(results, new ResultComparator(ResultComparator.COMPARE_TRACK_SCORE));
        return results;
    }

    /**
     * @return An ArrayList<Query> which contains all tracks in the resultList, sorted by score.
     * Given as queries.
     */
    public ArrayList<Query> getTrackQueries() {
        ArrayList<Query> queries = new ArrayList<Query>();
        for (Result result : getTrackResults()) {
            Query query = new Query(result, isOnlyLocal());
            query.addTrackResult(result);
            queries.add(query);
        }
        Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_TRACK_SCORE));
        return queries;
    }

    public Result getPreferredTrackResult() {
        Result result = null;
        if (mTrackResultHint >= 0 && mTrackResultHint < getTrackResults().size()) {
            result = getTrackResults().get(mTrackResultHint);
        }
        return result;
    }

    public Track getPreferredTrack() {
        if (getPreferredTrackResult() != null) {
            return getPreferredTrackResult().getTrack();
        }
        return mTrack;
    }

    public void addTrackResult(Result result) {
        mPlayable = true;
        if (result.getTrackScore() == 1f) {
            mSolved = true;
        }
        String key = TomahawkUtils.getCacheKey(result.getTrack());
        if (!mTrackResults.containsKey(key)) {
            mTrackResults.put(key, result);
        }
    }

    /**
     * Append an ArrayList<Result> to the track result list
     */
    public void addTrackResults(ArrayList<Result> results) {
        for (Result result : results) {
            addTrackResult(result);
        }
    }

    /**
     * @return An ArrayList<Result> which contains all albums in the resultList, sorted by score.
     * Given as Results.
     */
    public ArrayList<Result> getAlbumResults() {
        ArrayList<Result> results = new ArrayList<Result>(mAlbumResults.values());
        Collections.sort(results, new ResultComparator(ResultComparator.COMPARE_ALBUM_SCORE));
        return results;
    }

    /**
     * @return A ArrayList<Album> which contains all albums in the resultList, sorted by score.
     */
    public ArrayList<Album> getAlbums() {
        ArrayList<Result> results = getAlbumResults();
        ArrayList<Album> albums = new ArrayList<Album>();
        for (Result result : results) {
            albums.add(result.getAlbum());
        }
        return albums;
    }

    public void addAlbumResult(Result result) {
        String key = TomahawkUtils.getCacheKey(result.getAlbum());
        if (!mAlbumResults.containsKey(key)) {
            mAlbumResults.put(key, result);
        }
    }

    /**
     * Append an ArrayList<Result> to the track result list
     */
    public void addAlbumResults(ArrayList<Result> results) {
        for (Result result : results) {
            addAlbumResult(result);
        }
    }

    /**
     * @return An ArrayList<Result> which contains all artists in the resultList, sorted by score.
     * Given as Results.
     */
    public ArrayList<Result> getArtistResults() {
        ArrayList<Result> results = new ArrayList<Result>(mArtistResults.values());
        Collections.sort(results, new ResultComparator(ResultComparator.COMPARE_ARTIST_SCORE));
        return results;
    }

    /**
     * @return the ArrayList containing all track results
     */
    public ArrayList<Artist> getArtists() {
        ArrayList<Result> results = getArtistResults();
        ArrayList<Artist> artists = new ArrayList<Artist>();
        for (Result result : results) {
            artists.add(result.getArtist());
        }
        return artists;
    }


    public void addArtistResult(Result result) {
        String key = TomahawkUtils.getCacheKey(result.getArtist());
        if (!mArtistResults.containsKey(key)) {
            mArtistResults.put(key, result);
        }
    }

    /**
     * Append an ArrayList<Result> to the track result list
     */
    public void addArtistResults(ArrayList<Result> results) {
        for (Result result : results) {
            addArtistResult(result);
        }
    }

    public String getFullTextQuery() {
        return mFullTextQuery;
    }

    public boolean isFullTextQuery() {
        return mIsFullTextQuery;
    }

    public boolean isOnlyLocal() {
        return mIsOnlyLocal;
    }

    public boolean isPlayable() {
        return mPlayable;
    }

    public boolean isSolved() {
        return mSolved;
    }

    public String getQid() {
        return mQid;
    }

    /**
     * This method determines how similar the given result is to the search string.
     */
    public float howSimilar(Result r, int searchType) {
        String resultArtistName = "";
        String resultAlbumName = "";
        String resultTrackName = "";
        String artistName;
        String albumName;
        String trackName;
        if (isFullTextQuery()) {
            artistName = cleanUpString(mFullTextQuery, true);
            albumName = cleanUpString(mFullTextQuery, false);
            trackName = albumName;
        } else {
            artistName = cleanUpString(mTrack.getArtist().getName(), false);
            albumName = cleanUpString(mTrack.getAlbum().getName(), false);
            trackName = cleanUpString(mTrack.getName(), false);
        }
        if (r.getArtist().getName() != null) {
            resultArtistName = cleanUpString(r.getArtist().getName(), false);
        }
        if (r.getAlbum().getName() != null) {
            resultAlbumName = cleanUpString(r.getAlbum().getName(), false);
        }
        if (r.getTrack().getName() != null) {
            resultTrackName = cleanUpString(r.getTrack().getName(), false);
        }

        int distanceArtist = TomahawkUtils
                .getLevenshteinDistance(artistName, resultArtistName);
        int distanceAlbum = TomahawkUtils
                .getLevenshteinDistance(albumName, resultAlbumName);
        int distanceTrack = TomahawkUtils.getLevenshteinDistance(trackName, resultTrackName);

        int maxLengthArtist = Math
                .max(artistName.length(), resultArtistName.length());
        int maxLengthAlbum = Math
                .max(albumName.length(), resultAlbumName.length());
        int maxLengthTrack = Math.max(trackName.length(), resultTrackName.length());

        float distanceScoreArtist = (float) (maxLengthArtist - distanceArtist) / maxLengthArtist;
        float distanceScoreAlbum;
        if (maxLengthAlbum > 0) {
            distanceScoreAlbum = (float) (maxLengthAlbum - distanceAlbum) / maxLengthAlbum;
        } else {
            distanceScoreAlbum = 0F;
        }
        float distanceScoreTrack = (float) (maxLengthTrack - distanceTrack) / maxLengthTrack;

        if (isFullTextQuery()) {
            final String searchString = cleanUpString(getFullTextQuery(), false);
            ArrayList<String> resultSearchStrings = new ArrayList<String>();
            switch (searchType) {
                case PipeLine.PIPELINE_SEARCHTYPE_TRACKS:
                    resultSearchStrings
                            .add(cleanUpString(resultArtistName + " " + resultTrackName, false));
                    resultSearchStrings.add(cleanUpString(resultTrackName, false));
                    break;
                case PipeLine.PIPELINE_SEARCHTYPE_ARTISTS:
                    resultSearchStrings.add(cleanUpString(resultArtistName, false));
                    break;
                case PipeLine.PIPELINE_SEARCHTYPE_ALBUMS:
                    if (!TextUtils.isEmpty(resultAlbumName)) {
                        resultSearchStrings
                                .add(cleanUpString(resultArtistName + " " + resultAlbumName,
                                        false));
                        resultSearchStrings.add(cleanUpString(resultAlbumName, false));
                    }
                    break;
            }

            float maxResult = 0F;
            for (String resultSearchString : resultSearchStrings) {
                int distanceArtistTrack = TomahawkUtils
                        .getLevenshteinDistance(searchString, resultSearchString);
                int maxLengthArtistTrack = Math
                        .max(searchString.length(), resultSearchString.length());
                float distanceScoreArtistTrack =
                        (float) (maxLengthArtistTrack - distanceArtistTrack) / maxLengthArtistTrack;

                float result = Math.max(distanceScoreArtist, distanceScoreAlbum);
                result = Math.max(result, distanceScoreArtistTrack);
                result = Math.max(result, distanceScoreTrack);
                if (resultSearchString.contains(searchString)) {
                    result = Math.max(result, 0.9F);
                }
                maxResult = Math.max(result, maxResult);
            }
            return maxResult;
        } else {
            if (TextUtils.isEmpty(mTrack.getAlbum().getName())) {
                distanceScoreAlbum = 1F;
            }

            return (distanceScoreArtist * 4 + distanceScoreAlbum + distanceScoreTrack * 5) / 10;
        }
    }

    /**
     * Clean up the given String.
     *
     * @param replaceArticle wether or not the prefix "the " should be removed
     * @return the clean String
     */
    public String cleanUpString(String in, boolean replaceArticle) {
        String out = in.toLowerCase().trim().replaceAll("[\\s]{2,}", " ");
        if (replaceArticle && out.startsWith("the ")) {
            out = out.substring(4);
        }
        return out;
    }

    @Override
    public String getName() {
        return getPreferredTrack().getName();
    }

    @Override
    public Artist getArtist() {
        return getPreferredTrack().getArtist();
    }

    @Override
    public Album getAlbum() {
        return getPreferredTrack().getAlbum();
    }
}
