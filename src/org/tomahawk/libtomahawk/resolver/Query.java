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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import org.tomahawk.libtomahawk.*;

import android.text.TextUtils;
import android.util.Log;

/**
 * Author Enno Gottschalk <mrmaffen@googlemail.com>
 * Date: 18.01.13
 * 
 * This class represents a query which is passed to a resolver. It contains all the information needed to
 * enable the Resolver to resolve the results.
 */
public class Query {
    public static final String TAG = Query.class.getName();

    private HashMap<String, ArrayList<Result>> mTrackResults = new HashMap<String, ArrayList<Result>>();
    private HashMap<String, ArrayList<Result>> mAlbumResults = new HashMap<String, ArrayList<Result>>();
    private HashMap<String, ArrayList<Result>> mArtistResults = new HashMap<String, ArrayList<Result>>();

    private boolean mSolved;
    private String mQid;

    private String mFullTextQuery;
    private boolean mIsFullTextQuery;

    private String mTrackName = "";
    private String mAlbumName = "";
    private String mArtistName = "";

    /**
     * Constructs a new Query with the given QueryID. ID should be generated in TomahawkApp.
     * @param qid
     */
    public Query(final String qid) {
        mQid = qid;
    }

    /**
     * Constructs a new Query with the given QueryID and a fullTextQuery String.
     * ID should be generated in TomahawkApp.
     * @param qid
     * @param fullTextQuery
     */
    public Query(final String qid, final String fullTextQuery) {
        mFullTextQuery = fullTextQuery.replace("'", "\\'");
        mIsFullTextQuery = true;
        mQid = qid;
    }

    public Query(final String qid, final String trackName, final String albumName, final String artistName) {
        mTrackName = trackName.replace("'", "\\'");
        mAlbumName = albumName.replace("'", "\\'");
        mArtistName = artistName.replace("'", "\\'");
        mQid = qid;
        mIsFullTextQuery = false;
    }

    /**
     * @return A ArrayList<Track> which contains all tracks in the resultList, sorted by score.
     */
    public ArrayList<Track> getTrackResults() {
        ArrayList<Track> tracks = new ArrayList<Track>();
        for (ArrayList<Result> resultList : mTrackResults.values())
            if (!resultList.isEmpty())
                tracks.add(resultList.get(0).getTrack());
        Collections.sort(tracks, new TrackComparator(TrackComparator.COMPARE_SCORE));
        return tracks;
    }

    /**
     * Append an ArrayList<Result> to the track result list
     * @param results
     */
    public void addTrackResults(ArrayList<Result> results) {
        for (Result r : results) {
            boolean isDuplicate = true;
            String trackName = "";
            Track track = r.getTrack();
            if (track != null && track.getName() != null)
                trackName = cleanUpString(track.getName(), false);
            String artistName = "";
            Artist artist = r.getArtist();
            if (artist != null && artist.getName() != null)
                artistName = cleanUpString(artist.getName(), true);
            String albumName = "";
            Album album = r.getAlbum();
            if (album != null && album.getName() != null)
                albumName = cleanUpString(album.getName(), false);
            String key = trackName + "+" + artistName + "+" + albumName;
            ArrayList<Result> value = mTrackResults.get(key);
            if (value == null) {
                isDuplicate = false;
                value = new ArrayList<Result>();
            }
            value.add(r);
            mTrackResults.put(key, value);

            key = artistName;
            value = mArtistResults.get(key);
            if (value != null && !isDuplicate) {
                for (Result artistResult : value) {
                    artistResult.getArtist().addTrack(track);
                }
            }

            key = artistName + "+" + albumName;
            value = mAlbumResults.get(key);
            if (value != null && !isDuplicate) {
                for (Result albumResult : value) {
                    albumResult.getAlbum().addTrack(track);
                }
            }
        }
        mSolved = true;
    }

    /**
     * @return A ArrayList<Album> which contains all albums in the resultList, sorted by score.
     */
    public ArrayList<Album> getAlbumResults() {
        ArrayList<Album> albums = new ArrayList<Album>();
        for (ArrayList<Result> resultList : mAlbumResults.values())
            if (!resultList.isEmpty())
                albums.add(resultList.get(0).getAlbum());
        Collections.sort(albums, new AlbumComparator(AlbumComparator.COMPARE_SCORE));
        return albums;
    }

    /**
     * Append an ArrayList<Result> to the track result list
     * @param results
     */
    public void addAlbumResults(ArrayList<Result> results) {
        for (Result r : results) {
            boolean isDuplicate = true;
            String artistName = "";
            Artist artist = r.getArtist();
            if (artist != null && artist.getName() != null)
                artistName = cleanUpString(artist.getName(), true);
            String albumName = "";
            Album album = r.getAlbum();
            if (album != null && album.getName() != null)
                albumName = cleanUpString(album.getName(), false);
            String key = artistName + "+" + albumName;
            ArrayList<Result> value = mAlbumResults.get(key);
            if (value == null) {
                isDuplicate = false;
                value = new ArrayList<Result>();
            }
            value.add(r);
            mAlbumResults.put(key, value);

            key = artistName;
            value = mArtistResults.get(key);
            if (value != null && !isDuplicate) {
                for (Result artistResult : value) {
                    artistResult.getArtist().addAlbum(album);
                }
            }
        }
        mSolved = true;
    }

    /**
     * @return the ArrayList containing all track results
     */
    public ArrayList<Artist> getArtistResults() {
        ArrayList<Artist> artists = new ArrayList<Artist>();
        for (ArrayList<Result> resultList : mArtistResults.values())
            if (!resultList.isEmpty())
                artists.add(resultList.get(0).getArtist());
        Collections.sort(artists, new ArtistComparator(ArtistComparator.COMPARE_SCORE));
        return artists;
    }

    /**
     * Append an ArrayList<Result> to the track result list
     * @param results
     */
    public void addArtistResults(ArrayList<Result> results) {
        for (Result r : results) {
            String artistName = "";
            Artist artist = r.getArtist();
            if (artist != null && artist.getName() != null)
                artistName = cleanUpString(artist.getName(), true);
            String key = artistName;
            ArrayList<Result> value = mArtistResults.get(key);
            if (value == null) {
                value = new ArrayList<Result>();
            }
            value.add(r);
            mArtistResults.put(key, value);
        }
        mSolved = true;
    }

    public String getFullTextQuery() {
        return mFullTextQuery;
    }

    public boolean isFullTextQuery() {
        return mIsFullTextQuery;
    }

    public boolean isSolved() {
        return mSolved;
    }

    public String getQid() {
        return mQid;
    }

    /**
     * This method determines how similar the given result is to the search string.
     * @param r
     * @return
     */
    public float howSimilar(Result r, int searchType) {
        String resultArtistName = "";
        String resultAlbumName = "";
        String resultTrackName = "";
        if (r.getArtist().getName() != null)
            resultArtistName = cleanUpString(r.getArtist().getName(), true);
        if (r.getAlbum().getName() != null)
            resultAlbumName = cleanUpString(r.getAlbum().getName(), false);
        if (r.getTrack().getName() != null)
            resultTrackName = cleanUpString(r.getTrack().getName(), false);

        int distanceArtist = TomahawkUtils.getLevenshteinDistance(mArtistName, resultArtistName);
        int distanceAlbum = TomahawkUtils.getLevenshteinDistance(mAlbumName, resultAlbumName);
        int distanceTrack = TomahawkUtils.getLevenshteinDistance(mTrackName, resultTrackName);

        int maxLengthArtist = Math.max(mArtistName.length(), resultArtistName.length());
        int maxLengthAlbum = Math.max(mAlbumName.length(), resultAlbumName.length());
        int maxLengthTrack = Math.max(mTrackName.length(), resultTrackName.length());

        float distanceScoreArtist = (float) (maxLengthArtist - distanceArtist) / maxLengthArtist;
        float distanceScoreAlbum;
        if (maxLengthAlbum > 0)
            distanceScoreAlbum = (float) (maxLengthAlbum - distanceAlbum) / maxLengthAlbum;
        else
            distanceScoreAlbum = 0F;
        float distanceScoreTrack = (float) (maxLengthTrack - distanceTrack) / maxLengthTrack;

        if (isFullTextQuery()) {
            final String searchString = cleanUpString(getFullTextQuery(), false);
            ArrayList<String> resultSearchStrings = new ArrayList<String>();
            switch (searchType) {
            case PipeLine.PIPELINE_SEARCHTYPE_TRACKS:
                resultSearchStrings.add(cleanUpString(resultArtistName + " " + resultTrackName, false));
                resultSearchStrings.add(cleanUpString(resultTrackName, false));
                break;
            case PipeLine.PIPELINE_SEARCHTYPE_ARTISTS:
                resultSearchStrings.add(cleanUpString(resultArtistName, false));
                break;
            case PipeLine.PIPELINE_SEARCHTYPE_ALBUMS:
                if (!TextUtils.isEmpty(resultAlbumName)) {
                    resultSearchStrings.add(cleanUpString(resultArtistName + " " + resultAlbumName, false));
                    resultSearchStrings.add(cleanUpString(resultAlbumName, false));
                }
                break;
            }

            float maxResult = 0F;
            for (String resultSearchString : resultSearchStrings) {
                int distanceArtistTrack = TomahawkUtils.getLevenshteinDistance(searchString, resultSearchString);
                int maxLengthArtistTrack = Math.max(searchString.length(), resultSearchString.length());
                float distanceScoreArtistTrack = (float) (maxLengthArtistTrack - distanceArtistTrack)
                        / maxLengthArtistTrack;

                float result = Math.max(distanceScoreArtist, distanceScoreAlbum);
                result = Math.max(result, distanceScoreArtistTrack);
                result = Math.max(result, distanceScoreTrack);
                if (resultSearchString.contains(searchString))
                    result = Math.max(result, 0.9F);
                maxResult = Math.max(result, maxResult);
                Log.d(TAG, "cleanFullTextQueryWithArticle = " + searchString + ", resultArtistName= "
                        + resultArtistName + ", resultAlbumName= " + resultAlbumName + ", resultTrackName= "
                        + resultTrackName + ", score = " + Math.max(result, distanceScoreArtistTrack));
            }
            return maxResult;
        } else {
            if (TextUtils.isEmpty(mAlbumName))
                distanceScoreAlbum = 1F;

            float combined = (distanceScoreArtist * 4 + distanceScoreAlbum + distanceScoreTrack * 5) / 10;
            return combined;
        }
    }

    /**
     * Clean up the given String.
     * @param in
     * @param replaceArticle wether or not the prefix "the " should be removed
     * @return the clean String
     */
    public String cleanUpString(String in, boolean replaceArticle) {
        String out = in.toLowerCase().trim().replaceAll("[\\s]{2,}", " ");
        if (replaceArticle && out.startsWith("the "))
            out = out.substring(4);
        return out;
    }

    public String getTrackName() {
        return mTrackName;
    }

    public String getAlbumName() {
        return mAlbumName;
    }

    public String getArtistName() {
        return mArtistName;
    }
}
