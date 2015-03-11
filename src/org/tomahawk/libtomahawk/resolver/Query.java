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
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.libtomahawk.utils.TomahawkUtils;
import org.tomahawk.tomahawk_android.utils.MediaPlayerInterface;
import org.tomahawk.tomahawk_android.utils.TomahawkListItem;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class represents a query which is passed to a resolver. It contains all the information
 * needed to enable the Resolver to resolve the results.
 */
public class Query implements TomahawkListItem {

    public static final String TAG = Query.class.getSimpleName();

    private static final ConcurrentHashMap<String, Query> sQueries
            = new ConcurrentHashMap<>();

    private static final HashSet<String> sBlacklistedResults = new HashSet<>();

    private String mCacheKey;

    private final ConcurrentHashMap<String, Result> mTrackResults
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Result> mAlbumResults
            = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Result> mArtistResults
            = new ConcurrentHashMap<>();

    private Track mBasicTrack;

    private boolean mPlayable = false;

    private boolean mSolved = false;

    private String mTopTrackResultKey = "";

    private String mResultHint = "";

    private String mFullTextQuery;

    private final boolean mIsFullTextQuery;

    private final boolean mIsOnlyLocal;

    private boolean mIsFetchedViaHatchet;

    /**
     * Constructs a new Query.
     *
     * @param fullTextQuery fulltext-query String to construct this Query with
     * @param onlyLocal     whether or not this query should be resolved locally
     */
    private Query(String fullTextQuery, boolean onlyLocal) {
        if (fullTextQuery == null) {
            mFullTextQuery = "";
        } else {
            mFullTextQuery = fullTextQuery;
        }
        mIsFullTextQuery = true;
        mIsOnlyLocal = onlyLocal;
        if (mCacheKey == null) {
            mCacheKey = TomahawkUtils.getCacheKey(this);
        }
    }

    /**
     * Constructs a new Query.
     *
     * @param trackName           track's name String
     * @param artistName          artist's name String
     * @param albumName           album's name String
     * @param resultHint          resultHint's name String
     * @param onlyLocal           whether or not this query should be resolved locally
     * @param isFetchedViaHatchet whether or not this query has been fetched via the Hatchet API
     */
    private Query(String trackName, String albumName, String artistName, String resultHint,
            boolean onlyLocal, boolean isFetchedViaHatchet) {
        Artist artist = Artist.get(artistName);
        Album album = Album.get(albumName, artist);
        mBasicTrack = Track.get(trackName, album, artist);
        if (resultHint != null) {
            mResultHint = resultHint;
        }
        mIsFullTextQuery = false;
        mIsOnlyLocal = onlyLocal;
        mIsFetchedViaHatchet = isFetchedViaHatchet;
        if (mCacheKey == null) {
            mCacheKey = TomahawkUtils.getCacheKey(this);
        }
        album.addQuery(this);
        artist.addQuery(this);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint
     */
    public static Query get(String fullTextQuery, boolean onlyLocal) {
        Query query = new Query(fullTextQuery, onlyLocal);
        return ensureCache(query);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint
     */
    public static Query get(String trackName, String albumName, String artistName,
            boolean onlyLocal) {
        Query query = new Query(trackName, albumName, artistName, null, onlyLocal, false);
        return ensureCache(query);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint
     */
    public static Query get(String trackName, String albumName, String artistName,
            boolean onlyLocal, boolean isFetchedViaHatchet) {
        Query query = new Query(trackName, albumName, artistName, null, onlyLocal,
                isFetchedViaHatchet);
        return ensureCache(query);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint
     */
    public static Query get(String trackName, String albumName, String artistName,
            String resultHint, boolean onlyLocal, boolean isFetchedViaHatchet) {
        Query query = new Query(trackName, albumName, artistName, resultHint, onlyLocal,
                isFetchedViaHatchet);
        return ensureCache(query);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint
     */
    public static Query get(Track track, boolean onlyLocal) {
        Query query = new Query(track.getName(), track.getAlbum().getName(),
                track.getArtist().getName(), null, onlyLocal, false);
        return ensureCache(query);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint
     */
    public static Query get(Result result, boolean onlyLocal) {
        Query query = new Query(result.getTrack().getName(),
                result.getTrack().getAlbum().getName(),
                result.getTrack().getArtist().getName(), null, onlyLocal, false);
        return ensureCache(query);
    }

    /**
     * If Query is already in our cache, return that. Otherwise add it to the cache.
     */
    private static Query ensureCache(Query query) {
        if (!sQueries.containsKey(query.getCacheKey())) {
            sQueries.put(query.getCacheKey(), query);
        }
        return sQueries.get(query.getCacheKey());
    }

    public MediaPlayerInterface getMediaPlayerInterface() {
        if (getPreferredTrackResult() != null) {
            return getPreferredTrackResult().getMediaPlayerInterface();
        } else {
            return null;
        }
    }

    public String getCacheKey() {
        return mCacheKey;
    }

    public Track getBasicTrack() {
        return mBasicTrack;
    }

    /**
     * Get the {@link Query} by providing its cache key
     */
    public static Query getQueryByKey(String key) {
        return sQueries.get(key);
    }

    public static HashSet<String> getBlacklistedResults() {
        return sBlacklistedResults;
    }

    /**
     * @return An ArrayList<Result> which contains all tracks in the resultList, sorted by score.
     * Given as Results.
     */
    public ArrayList<Result> getTrackResults() {
        ArrayList<Result> results = new ArrayList<>(mTrackResults.values());
        Collections.sort(results, new ResultComparator(ResultComparator.COMPARE_TRACK_SCORE));
        return results;
    }

    /**
     * @return An ArrayList<Query> which contains all tracks in the resultList, sorted by score.
     * Given as queries.
     */
    public ArrayList<Query> getTrackQueries() {
        HashMap<String, Query> queryMap = new HashMap<>();
        for (Result result : getTrackResults()) {
            if (!isOnlyLocal() || result.isLocal()) {
                Query query = Query.get(result, isOnlyLocal());
                query.addTrackResult(result);
                queryMap.put(query.getCacheKey(), query);
            }
        }
        ArrayList<Query> queries = new ArrayList<>(queryMap.values());
        Collections.sort(queries, new QueryComparator(QueryComparator.COMPARE_TRACK_SCORE));
        return queries;
    }

    public Result getPreferredTrackResult() {
        return mTrackResults.get(mTopTrackResultKey);
    }

    public Track getPreferredTrack() {
        if (getPreferredTrackResult() != null) {
            return getPreferredTrackResult().getTrack();
        }
        return mBasicTrack;
    }

    public void addTrackResult(Result result) {
        if (!sBlacklistedResults.contains(result.getCacheKey())) {
            mPlayable = true;
            if (result.getTrackScore() == 1f) {
                mSolved = true;
            }
            if (!mTrackResults.containsKey(result.getCacheKey())) {
                mTrackResults.put(result.getCacheKey(), result);
                if ((getPreferredTrackResult() == null
                        || mResultHint.equals(result.getCacheKey())
                        || getPreferredTrackResult().getTrackScore() < result.getTrackScore()
                        || (getPreferredTrackResult().getTrackScore() == result.getTrackScore()
                        && getPreferredTrackResult().getResolvedBy().getWeight()
                        < result.getResolvedBy().getWeight()))) {
                    mTopTrackResultKey = result.getCacheKey();
                }
            }
        }
    }

    public void blacklistTrackResult(Result result) {
        sBlacklistedResults.add(result.getCacheKey());
        for (Result r : getTrackResults()) {
            if (!result.getCacheKey().equals(r.getCacheKey())
                    && !sBlacklistedResults.contains(r.getCacheKey())) {
                mTopTrackResultKey = r.getCacheKey();
                break;
            } else {
                mTopTrackResultKey = "";
                mPlayable = false;
                mSolved = false;
            }
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
        ArrayList<Result> results = new ArrayList<>(mAlbumResults.values());
        Collections.sort(results, new ResultComparator(ResultComparator.COMPARE_ALBUM_SCORE));
        return results;
    }

    /**
     * @return A ArrayList<Album> which contains all albums in the resultList, sorted by score.
     */
    public ArrayList<Album> getAlbums() {
        ArrayList<Result> results = getAlbumResults();
        ArrayList<Album> albums = new ArrayList<>();
        for (Result result : results) {
            albums.add(result.getAlbum());
        }
        return albums;
    }

    public void addAlbumResult(Result result) {
        mAlbumResults.put(result.getAlbum().getCacheKey(), result);
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
        ArrayList<Result> results = new ArrayList<>(mArtistResults.values());
        Collections.sort(results, new ResultComparator(ResultComparator.COMPARE_ARTIST_SCORE));
        return results;
    }

    /**
     * @return the ArrayList containing all track results
     */
    public ArrayList<Artist> getArtists() {
        ArrayList<Result> results = getArtistResults();
        ArrayList<Artist> artists = new ArrayList<>();
        for (Result result : results) {
            artists.add(result.getArtist());
        }
        return artists;
    }


    public void addArtistResult(Result result) {
        mArtistResults.put(result.getArtist().getCacheKey(), result);
    }

    /**
     * Append an ArrayList<Result> to the track result list
     */
    public void addArtistResults(ArrayList<Result> results) {
        for (Result result : results) {
            addArtistResult(result);
        }
    }

    public String getResultHint() {
        return mResultHint;
    }

    public String getTopTrackResultKey() {
        return mTopTrackResultKey;
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

    public boolean isFetchedViaHatchet() {
        return mIsFetchedViaHatchet;
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
            artistName = cleanUpString(mBasicTrack.getArtist().getName(), false);
            albumName = cleanUpString(mBasicTrack.getAlbum().getName(), false);
            trackName = cleanUpString(mBasicTrack.getName(), false);
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
            ArrayList<String> resultSearchStrings = new ArrayList<>();
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
            if (TextUtils.isEmpty(mBasicTrack.getAlbum().getName())) {
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
        if (isFullTextQuery()) {
            return mFullTextQuery;
        }
        return getPreferredTrack().getName();
    }

    @Override
    public Artist getArtist() {
        return getPreferredTrack().getArtist();
    }

    @Override
    public Album getAlbum() {
        if (mIsFetchedViaHatchet) {
            return mBasicTrack.getAlbum();
        }
        return getPreferredTrack().getAlbum();
    }

    @Override
    public ArrayList<Query> getQueries() {
        ArrayList<Query> queries = new ArrayList<>();
        queries.add(this);
        return queries;
    }

    @Override
    public Image getImage() {
        if (getAlbum().getImage() != null && !TextUtils
                .isEmpty(getAlbum().getImage().getImagePath())) {
            return getAlbum().getImage();
        } else {
            return getArtist().getImage();
        }
    }

    public boolean hasArtistImage() {
        return (getAlbum().getImage() == null
                || TextUtils.isEmpty(getAlbum().getImage().getImagePath()))
                && getArtist().getImage() != null;
    }
}
