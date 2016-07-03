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
import org.tomahawk.libtomahawk.collection.AlphaComparable;
import org.tomahawk.libtomahawk.collection.Artist;
import org.tomahawk.libtomahawk.collection.ArtistAlphaComparable;
import org.tomahawk.libtomahawk.collection.Cacheable;
import org.tomahawk.libtomahawk.collection.Image;
import org.tomahawk.libtomahawk.collection.Playlist;
import org.tomahawk.libtomahawk.collection.Track;
import org.tomahawk.tomahawk_android.R;
import org.tomahawk.tomahawk_android.TomahawkApp;
import org.tomahawk.tomahawk_android.utils.IdGenerator;

import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This class represents a query which is passed to a resolver. It contains all the information
 * needed to enable the Resolver to resolve the results.
 */
public class Query extends Cacheable implements AlphaComparable, ArtistAlphaComparable {

    public static final String TAG = Query.class.getSimpleName();

    private static final HashSet<String> sBlacklistedResults = new HashSet<>();

    private Track mBasicTrack;

    private String mResultHint;

    private String mFullTextQuery;

    private final boolean mIsFullTextQuery;

    private final boolean mIsOnlyLocal;

    private boolean mIsFetchedViaHatchet;

    private final ConcurrentSkipListSet<Result> mTrackResults
            = new ConcurrentSkipListSet<>(new ResultComparator());

    private final ConcurrentHashMap<Result, Float> mTrackResultScores
            = new ConcurrentHashMap<>();

    public class ResultComparator implements Comparator<Result> {

        /**
         * The actual comparison method
         *
         * @param r1 First {@link org.tomahawk.libtomahawk.resolver.Result} object
         * @param r2 Second {@link org.tomahawk.libtomahawk.resolver.Result} Object
         * @return int containing comparison score
         */
        public int compare(Result r1, Result r2) {
            if (mResultHint != null) {
                // We have a result hint. If the cacheKey matches we automatically put the matching
                // Result at the top of the sorted list.
                if (r1.getCacheKey().equals(mResultHint)) {
                    return -1;
                } else if (r2.getCacheKey().equals(mResultHint)) {
                    return 1;
                }
            }
            if (r1 == r2) {
                return 0;
            }
            Float score1 = mTrackResultScores.get(r1);
            Float score2 = mTrackResultScores.get(r2);
            int scoreResult = score2.compareTo(score1);
            if (scoreResult > 0) {
                return 1;
            } else if (scoreResult < 0) {
                return -1;
            } else {
                // We have two identical trackScores.
                // Now we take the Resolver's weight into account.
                Integer weight1 = r1.getResolvedBy().getWeight();
                Integer weight2 = r2.getResolvedBy().getWeight();
                int weightResult = weight2.compareTo(weight1);
                if (weightResult > 0) {
                    return 1;
                } else if (weightResult < 0) {
                    return -1;
                } else {
                    // We have two identical trackScores and Resolver weights.
                    Integer hashCode1 = r1.hashCode();
                    Integer hashCode2 = r2.hashCode();
                    int hashCodeResult = hashCode1.compareTo(hashCode2);
                    if (hashCodeResult > 0) {
                        return 1;
                    } else if (hashCodeResult < 0) {
                        return -1;
                    } else {
                        // should never happen
                        return 0;
                    }
                }
            }
        }
    }

    /**
     * Constructs a new Query.
     *
     * @param fullTextQuery fulltext-query String to construct this Query with
     * @param onlyLocal     whether or not this query should be resolved locally
     */
    private Query(String fullTextQuery, boolean onlyLocal) {
        super(Query.class, getCacheKey(fullTextQuery, onlyLocal));

        mFullTextQuery = fullTextQuery != null ? fullTextQuery : "";
        mIsFullTextQuery = true;
        mIsOnlyLocal = onlyLocal;
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
        super(Query.class, getCacheKey(trackName, albumName, artistName, resultHint, onlyLocal));

        Artist artist = Artist.get(artistName);
        Album album = Album.get(albumName, artist);
        mBasicTrack = Track.get(trackName, album, artist);
        mResultHint = resultHint != null ? resultHint : "";
        mIsFullTextQuery = false;
        mIsOnlyLocal = onlyLocal;
        mIsFetchedViaHatchet = isFetchedViaHatchet;
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint/onlyLocal
     */
    public static Query get(String fullTextQuery, boolean onlyLocal) {
        Cacheable cacheable = get(Query.class, getCacheKey(fullTextQuery, onlyLocal));
        return cacheable != null ? (Query) cacheable : new Query(fullTextQuery, onlyLocal);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint/onlyLocal
     */
    public static Query get(String trackName, String albumName, String artistName,
            String resultHint, boolean onlyLocal, boolean isFetchedViaHatchet) {
        Cacheable cacheable = get(Query.class,
                getCacheKey(trackName, albumName, artistName, resultHint, onlyLocal));
        return cacheable != null ? (Query) cacheable :
                new Query(trackName, albumName, artistName, resultHint, onlyLocal,
                        isFetchedViaHatchet);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint/onlyLocal
     */
    public static Query get(String trackName, String albumName, String artistName,
            boolean onlyLocal) {
        return get(trackName, albumName, artistName, null, onlyLocal, false);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint/onlyLocal
     */
    public static Query get(String trackName, String albumName, String artistName,
            boolean onlyLocal, boolean isFetchedViaHatchet) {
        return get(trackName, albumName, artistName, null, onlyLocal, isFetchedViaHatchet);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint
     */
    public static Query get(Track track, boolean onlyLocal) {
        return get(track.getName(), track.getAlbum().getName(), track.getArtist().getName(), null,
                onlyLocal, false);
    }

    /**
     * Static builder method which constructs a query or fetches it from the cache, resulting in
     * Queries being unique by trackname/artistname/albumname/resulthint
     */
    public static Query get(Result result, boolean onlyLocal) {
        return get(result.getTrack().getName(), result.getTrack().getAlbum().getName(),
                result.getTrack().getArtist().getName(), result.getCacheKey(), onlyLocal, false);
    }

    public static Query getByKey(String cacheKey) {
        return (Query) get(Query.class, cacheKey);
    }

    public Class getMediaPlayerClass() {
        if (getPreferredTrackResult() != null) {
            return getPreferredTrackResult().getMediaPlayerClass();
        } else {
            return null;
        }
    }

    public Track getBasicTrack() {
        return mBasicTrack;
    }

    public static HashSet<String> getBlacklistedResults() {
        return sBlacklistedResults;
    }

    /**
     * @return An ArrayList<Query> which contains all tracks in the resultList, sorted by score.
     * Given as queries.
     */
    public Playlist getResultPlaylist() {
        ArrayList<Query> queries = new ArrayList<>();
        for (Result result : mTrackResults) {
            if (!isOnlyLocal() || result.isLocal()) {
                Query query = Query.get(result, isOnlyLocal());
                query.addTrackResult(result, mTrackResultScores.get(result));
                queries.add(query);
            }
        }
        Playlist playlist = Playlist.fromQueryList(IdGenerator.getSessionUniqueStringId(),
                mFullTextQuery, "", queries);
        playlist.setFilled(true);
        return playlist;
    }

    public Result getPreferredTrackResult() {
        for (Result trackResult : mTrackResults) {
            if (trackResult.getResolvedBy().isEnabled()) {
                return trackResult;
            }
        }
        return null;
    }

    public Track getPreferredTrack() {
        Result result = getPreferredTrackResult();
        if (result != null) {
            return result.getTrack();
        }
        return mBasicTrack;
    }

    /**
     * Add a {@link Result} to this {@link Query}.
     *
     * @param result     The {@link Result} which should be added
     * @param trackScore the trackScore for the given {@link Result}
     */
    public void addTrackResult(Result result, float trackScore) {
        String cacheKey = result.getCacheKey();
        if (!sBlacklistedResults.contains(cacheKey)) {
            mTrackResultScores.put(result, trackScore);
            mTrackResults.add(result);
        }
    }

    public void blacklistTrackResult(Result result) {
        sBlacklistedResults.add(result.getCacheKey());
        if (result.getCacheKey().equals(mResultHint)) {
            mResultHint = null;
        }
        mTrackResults.remove(result);
        mTrackResultScores.remove(result);
    }

    public String getResultHint() {
        return mResultHint;
    }

    public String getTopTrackResultKey() {
        Result result = getPreferredTrackResult();
        if (result != null) {
            return getPreferredTrackResult().getCacheKey();
        }
        return null;
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
        return getPreferredTrackResult() != null;
    }

    public boolean isSolved() {
        Result result = getPreferredTrackResult();
        return result != null && result.getCacheKey().equals(mResultHint);
    }

    public boolean isFetchedViaHatchet() {
        return mIsFetchedViaHatchet;
    }

    /**
     * This method determines how similar the given result is to the search string.
     */
    public float howSimilar(Result r) {
        String resultArtistName = ResultScoring.cleanUpString(r.getArtist().getName(), false);
        String resultAlbumName = ResultScoring.cleanUpString(r.getAlbum().getName(), false);
        String resultTrackName = ResultScoring.cleanUpString(r.getTrack().getName(), false);
        if (isFullTextQuery()) {
            String fullTextQuery = ResultScoring.cleanUpString(mFullTextQuery, true);
            float maxResult = 0f;
            maxResult = Math.max(maxResult, ResultScoring.calculateScore(
                    resultTrackName + " " + resultAlbumName + " " + resultArtistName,
                    fullTextQuery));
            maxResult = Math.max(maxResult, ResultScoring.calculateScore(
                    resultTrackName + " " + resultArtistName + " " + resultAlbumName,
                    fullTextQuery));
            maxResult = Math.max(maxResult, ResultScoring.calculateScore(
                    resultArtistName + " " + resultTrackName + " " + resultAlbumName,
                    fullTextQuery));
            maxResult = Math.max(maxResult, ResultScoring.calculateScore(
                    resultArtistName + " " + resultAlbumName + " " + resultTrackName,
                    fullTextQuery));
            maxResult = Math.max(maxResult, ResultScoring.calculateScore(
                    resultAlbumName + " " + resultArtistName + " " + resultTrackName,
                    fullTextQuery));
            maxResult = Math.max(maxResult, ResultScoring.calculateScore(
                    resultAlbumName + " " + resultTrackName + " " + resultArtistName,
                    fullTextQuery));
            return maxResult;
        } else {
            String queryArtistName =
                    ResultScoring.cleanUpString(mBasicTrack.getArtist().getName(), false);
            float artistScore = ResultScoring.calculateScore(resultArtistName, queryArtistName);
            String queryTrackName =
                    ResultScoring.cleanUpString(mBasicTrack.getName(), false);
            float trackScore = ResultScoring.calculateScore(resultTrackName, queryTrackName);
            String queryAlbumName =
                    ResultScoring.cleanUpString(mBasicTrack.getAlbum().getName(), false);
            float albumScore;
            if (queryAlbumName.isEmpty()) {
                return (artistScore + trackScore) / 2;
            } else {
                albumScore = ResultScoring.calculateScore(resultAlbumName, queryAlbumName);
                return (artistScore * 3 + albumScore + trackScore * 4) / 8;
            }
        }
    }

    public String getName() {
        if (isFullTextQuery()) {
            return mFullTextQuery;
        }
        return getPreferredTrack().getName();
    }

    /**
     * @return the name that should be displayed
     */
    public String getPrettyName() {
        return getName().isEmpty() ?
                TomahawkApp.getContext().getResources().getString(R.string.unknown)
                : getName();
    }

    public Artist getArtist() {
        return getPreferredTrack().getArtist();
    }

    public Album getAlbum() {
        if (mIsFetchedViaHatchet && !mBasicTrack.getAlbum().getName().isEmpty()) {
            return mBasicTrack.getAlbum();
        }
        return getPreferredTrack().getAlbum();
    }

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

    public String toShortString() {
        String desc;
        if (mIsFullTextQuery) {
            desc = "fullTextQuery: '" + mFullTextQuery + "'";
        } else {
            desc = "basic: " + mBasicTrack.toShortString()
                    + " - preferred: " + getPreferredTrack().toShortString();
        }
        return desc + ", resultCount: " + mTrackResults.size();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "( " + toShortString() + " )@"
                + Integer.toHexString(hashCode());
    }
}
