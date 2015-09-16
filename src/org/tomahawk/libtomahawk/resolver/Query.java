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
import org.tomahawk.libtomahawk.utils.LevensteinDistance;
import org.tomahawk.tomahawk_android.activities.TomahawkMainActivity;
import org.tomahawk.tomahawk_android.mediaplayers.TomahawkMediaPlayer;

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

    public TomahawkMediaPlayer getMediaPlayerInterface() {
        if (getPreferredTrackResult() != null) {
            return getPreferredTrackResult().getMediaPlayerInterface();
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
        Playlist playlist = Playlist.fromQueryList(TomahawkMainActivity.getSessionUniqueStringId(),
                false, mFullTextQuery, "", queries);
        playlist.setFilled(true);
        return playlist;
    }

    public Result getPreferredTrackResult() {
        if (mTrackResults.size() > 0) {
            return mTrackResults.first();
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

        int distanceArtist = LevensteinDistance.getDistance(artistName, resultArtistName);
        int distanceAlbum = LevensteinDistance.getDistance(albumName, resultAlbumName);
        int distanceTrack = LevensteinDistance.getDistance(trackName, resultTrackName);

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
            resultSearchStrings
                    .add(cleanUpString(resultArtistName + " " + resultTrackName, false));
            resultSearchStrings.add(cleanUpString(resultTrackName, false));

            float maxResult = 0F;
            for (String resultSearchString : resultSearchStrings) {
                int distanceArtistTrack =
                        LevensteinDistance.getDistance(searchString, resultSearchString);
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
        return getName().length() > 0 ? getName() : "<unknown>";
    }

    public Artist getArtist() {
        return getPreferredTrack().getArtist();
    }

    public Album getAlbum() {
        if (mIsFetchedViaHatchet) {
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
}
